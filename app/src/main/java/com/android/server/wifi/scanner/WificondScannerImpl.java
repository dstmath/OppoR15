package com.android.server.wifi.scanner;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner.ScanData;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiNative.BucketSettings;
import com.android.server.wifi.WifiNative.HiddenNetwork;
import com.android.server.wifi.WifiNative.PnoEventHandler;
import com.android.server.wifi.WifiNative.PnoNetwork;
import com.android.server.wifi.WifiNative.PnoSettings;
import com.android.server.wifi.WifiNative.ScanCapabilities;
import com.android.server.wifi.WifiNative.ScanEventHandler;
import com.android.server.wifi.WifiNative.ScanSettings;
import com.android.server.wifi.scanner.ChannelHelper.ChannelCollection;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WificondScannerImpl extends WifiScannerImpl implements Callback {
    public static final String BACKGROUND_PERIOD_ALARM_TAG = "WificondScannerImpl Background Scan Period";
    private static final boolean DBG = false;
    private static final int MAX_APS_PER_SCAN = 32;
    public static final int MAX_HIDDEN_NETWORK_IDS_PER_SCAN = 16;
    private static final int MAX_SCAN_BUCKETS = 16;
    private static final int SCAN_BUFFER_CAPACITY = 10;
    private static final long SCAN_TIMEOUT_MS = 15000;
    private static final String TAG = "WificondScannerImpl";
    public static final String TIMEOUT_ALARM_TAG = "WificondScannerImpl Scan Timeout";
    private final AlarmManager mAlarmManager;
    private ScanBuffer mBackgroundScanBuffer;
    private ScanEventHandler mBackgroundScanEventHandler;
    private boolean mBackgroundScanPaused;
    private boolean mBackgroundScanPeriodPending;
    private ScanSettings mBackgroundScanSettings;
    private final ChannelHelper mChannelHelper;
    private final Clock mClock;
    private final Context mContext;
    private final Handler mEventHandler;
    private final HwPnoDebouncer mHwPnoDebouncer;
    private final Listener mHwPnoDebouncerListener;
    private final boolean mHwPnoScanSupported;
    private LastScanSettings mLastScanSettings;
    private ScanData mLatestSingleScanResult;
    private ArrayList<ScanDetail> mNativeScanResults;
    private int mNextBackgroundScanId;
    private int mNextBackgroundScanPeriod;
    private ScanEventHandler mPendingBackgroundScanEventHandler;
    private ScanSettings mPendingBackgroundScanSettings;
    private ScanEventHandler mPendingSingleScanEventHandler;
    private ScanSettings mPendingSingleScanSettings;
    private PnoEventHandler mPnoEventHandler;
    private PnoSettings mPnoSettings;
    private OppoScanFoolProof mScanFoolProof;
    OnAlarmListener mScanPeriodListener;
    OnAlarmListener mScanTimeoutListener;
    private final Object mSettingsLock;
    private final WifiManager mWifiManager;
    private final WifiNative mWifiNative;

    public static class HwPnoDebouncer {
        private static final int MINIMUM_PNO_GAP_MS = 5000;
        public static final String PNO_DEBOUNCER_ALARM_TAG = "WificondScannerImplPno Monitor";
        private final OnAlarmListener mAlarmListener = new OnAlarmListener() {
            public void onAlarm() {
                if (!HwPnoDebouncer.this.mExpectedPnoState) {
                    HwPnoDebouncer.this.stopPnoScanInternal();
                } else if (!(HwPnoDebouncer.this.startPnoScanInternal() || HwPnoDebouncer.this.mListener == null)) {
                    HwPnoDebouncer.this.mListener.onPnoScanFailed();
                }
                HwPnoDebouncer.this.mWaitForTimer = false;
            }
        };
        private final AlarmManager mAlarmManager;
        private final Clock mClock;
        private boolean mCurrentPnoState = false;
        private final Handler mEventHandler;
        private boolean mExpectedPnoState = false;
        private long mLastPnoChangeTimeStamp = -1;
        private Listener mListener;
        private PnoSettings mPnoSettings;
        private boolean mWaitForTimer = false;
        private final WifiNative mWifiNative;

        public interface Listener {
            void onPnoScanFailed();
        }

        public HwPnoDebouncer(WifiNative wifiNative, AlarmManager alarmManager, Handler eventHandler, Clock clock) {
            this.mWifiNative = wifiNative;
            this.mAlarmManager = alarmManager;
            this.mEventHandler = eventHandler;
            this.mClock = clock;
        }

        private boolean startPnoScanInternal() {
            if (this.mCurrentPnoState) {
                return true;
            }
            if (this.mPnoSettings == null) {
                Log.e(WificondScannerImpl.TAG, "PNO state change to enable failed, no available Pno settings");
                return false;
            }
            this.mLastPnoChangeTimeStamp = this.mClock.getElapsedSinceBootMillis();
            Log.d(WificondScannerImpl.TAG, "Remove all networks from supplicant before starting PNO scan");
            this.mWifiNative.removeAllNetworks();
            if (this.mWifiNative.startPnoScan(this.mPnoSettings)) {
                Log.d(WificondScannerImpl.TAG, "Changed PNO state from " + this.mCurrentPnoState + " to enable");
                this.mCurrentPnoState = true;
                return true;
            }
            Log.e(WificondScannerImpl.TAG, "PNO state change to enable failed");
            this.mCurrentPnoState = false;
            return false;
        }

        private boolean stopPnoScanInternal() {
            if (!this.mCurrentPnoState) {
                return true;
            }
            this.mLastPnoChangeTimeStamp = this.mClock.getElapsedSinceBootMillis();
            if (this.mWifiNative.stopPnoScan()) {
                Log.d(WificondScannerImpl.TAG, "Changed PNO state from " + this.mCurrentPnoState + " to disable");
                this.mCurrentPnoState = false;
                return true;
            }
            Log.e(WificondScannerImpl.TAG, "PNO state change to disable failed");
            this.mCurrentPnoState = false;
            return false;
        }

        private boolean setPnoState(boolean enable) {
            this.mExpectedPnoState = enable;
            if (this.mWaitForTimer) {
                return true;
            }
            long timeDifference = this.mClock.getElapsedSinceBootMillis() - this.mLastPnoChangeTimeStamp;
            if (timeDifference < 5000) {
                long alarmTimeout = 5000 - timeDifference;
                Log.d(WificondScannerImpl.TAG, "Start PNO timer with delay " + alarmTimeout);
                this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + alarmTimeout, PNO_DEBOUNCER_ALARM_TAG, this.mAlarmListener, this.mEventHandler);
                this.mWaitForTimer = true;
                return true;
            } else if (enable) {
                return startPnoScanInternal();
            } else {
                return stopPnoScanInternal();
            }
        }

        public boolean startPnoScan(PnoSettings pnoSettings, Listener listener) {
            this.mListener = listener;
            this.mPnoSettings = pnoSettings;
            if (setPnoState(true)) {
                return true;
            }
            this.mListener = null;
            return false;
        }

        public void stopPnoScan() {
            setPnoState(false);
            this.mListener = null;
        }

        public void forceStopPnoScan() {
            if (this.mWaitForTimer) {
                this.mAlarmManager.cancel(this.mAlarmListener);
                this.mWaitForTimer = false;
            }
            stopPnoScanInternal();
        }
    }

    private static class LastScanSettings {
        public boolean backgroundScanActive = false;
        public boolean hwPnoScanActive = false;
        public int maxAps;
        public PnoNetwork[] pnoNetworkList;
        public PnoEventHandler pnoScanEventHandler;
        public int reportEvents;
        public int reportNumScansThreshold;
        public int reportPercentThreshold;
        public boolean reportSingleScanFullResults;
        public int scanId;
        public boolean singleScanActive = false;
        public ScanEventHandler singleScanEventHandler;
        public ChannelCollection singleScanFreqs;
        public long startTime;

        LastScanSettings(long startTime) {
            this.startTime = startTime;
        }

        public void setBackgroundScan(int scanId, int maxAps, int reportEvents, int reportNumScansThreshold, int reportPercentThreshold) {
            this.backgroundScanActive = true;
            this.scanId = scanId;
            this.maxAps = maxAps;
            this.reportEvents = reportEvents;
            this.reportNumScansThreshold = reportNumScansThreshold;
            this.reportPercentThreshold = reportPercentThreshold;
        }

        public void setSingleScan(boolean reportSingleScanFullResults, ChannelCollection singleScanFreqs, ScanEventHandler singleScanEventHandler) {
            this.singleScanActive = true;
            this.reportSingleScanFullResults = reportSingleScanFullResults;
            this.singleScanFreqs = singleScanFreqs;
            this.singleScanEventHandler = singleScanEventHandler;
        }

        public void setHwPnoScan(PnoNetwork[] pnoNetworkList, PnoEventHandler pnoScanEventHandler) {
            this.hwPnoScanActive = true;
            this.pnoNetworkList = pnoNetworkList;
            this.pnoScanEventHandler = pnoScanEventHandler;
        }
    }

    private static class ScanBuffer {
        private final ArrayDeque<ScanData> mBuffer = new ArrayDeque(this.mCapacity);
        private int mCapacity;

        ScanBuffer(int capacity) {
            this.mCapacity = capacity;
        }

        public int size() {
            return this.mBuffer.size();
        }

        public int capacity() {
            return this.mCapacity;
        }

        public boolean isFull() {
            return size() == this.mCapacity;
        }

        public void add(ScanData scanData) {
            if (isFull()) {
                this.mBuffer.pollFirst();
            }
            this.mBuffer.offerLast(scanData);
        }

        public void clear() {
            this.mBuffer.clear();
        }

        public ScanData[] get() {
            return (ScanData[]) this.mBuffer.toArray(new ScanData[this.mBuffer.size()]);
        }
    }

    public WificondScannerImpl(Context context, WifiNative wifiNative, WifiMonitor wifiMonitor, ChannelHelper channelHelper, Looper looper, Clock clock) {
        this.mSettingsLock = new Object();
        this.mPendingBackgroundScanSettings = null;
        this.mPendingBackgroundScanEventHandler = null;
        this.mPendingSingleScanSettings = null;
        this.mPendingSingleScanEventHandler = null;
        this.mBackgroundScanSettings = null;
        this.mBackgroundScanEventHandler = null;
        this.mNextBackgroundScanPeriod = 0;
        this.mNextBackgroundScanId = 0;
        this.mBackgroundScanPeriodPending = false;
        this.mBackgroundScanPaused = false;
        this.mBackgroundScanBuffer = new ScanBuffer(10);
        this.mLatestSingleScanResult = new ScanData(0, 0, new ScanResult[0]);
        this.mLastScanSettings = null;
        this.mPnoSettings = null;
        this.mHwPnoDebouncerListener = new Listener() {
            public void onPnoScanFailed() {
                Log.e(WificondScannerImpl.TAG, "Pno scan failure received");
                WificondScannerImpl.this.reportPnoScanFailure();
            }
        };
        this.mScanPeriodListener = new OnAlarmListener() {
            public void onAlarm() {
                synchronized (WificondScannerImpl.this.mSettingsLock) {
                    WificondScannerImpl.this.handleScanPeriod();
                }
            }
        };
        this.mScanTimeoutListener = new OnAlarmListener() {
            public void onAlarm() {
                synchronized (WificondScannerImpl.this.mSettingsLock) {
                    WificondScannerImpl.this.handleScanTimeout();
                }
            }
        };
        this.mContext = context;
        this.mWifiNative = wifiNative;
        this.mChannelHelper = channelHelper;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mEventHandler = new Handler(looper, this);
        this.mClock = clock;
        this.mHwPnoDebouncer = new HwPnoDebouncer(this.mWifiNative, this.mAlarmManager, this.mEventHandler, this.mClock);
        this.mHwPnoScanSupported = this.mContext.getResources().getBoolean(17957061);
        wifiMonitor.registerHandler(this.mWifiNative.getInterfaceName(), WifiMonitor.SCAN_FAILED_EVENT, this.mEventHandler);
        wifiMonitor.registerHandler(this.mWifiNative.getInterfaceName(), WifiMonitor.PNO_SCAN_RESULTS_EVENT, this.mEventHandler);
        wifiMonitor.registerHandler(this.mWifiNative.getInterfaceName(), WifiMonitor.SCAN_RESULTS_EVENT, this.mEventHandler);
        this.mScanFoolProof = new OppoScanFoolProof(this.mEventHandler, clock, context);
    }

    public WificondScannerImpl(Context context, WifiNative wifiNative, WifiMonitor wifiMonitor, Looper looper, Clock clock) {
        this(context, wifiNative, wifiMonitor, new NoBandChannelHelper(), looper, clock);
    }

    public void cleanup() {
        synchronized (this.mSettingsLock) {
            this.mPendingSingleScanSettings = null;
            this.mPendingSingleScanEventHandler = null;
            stopHwPnoScan();
            stopBatchedScan();
            this.mLastScanSettings = null;
        }
    }

    public boolean getScanCapabilities(ScanCapabilities capabilities) {
        capabilities.max_scan_cache_size = Integer.MAX_VALUE;
        capabilities.max_scan_buckets = 16;
        capabilities.max_ap_cache_per_scan = 32;
        capabilities.max_rssi_sample_size = 8;
        capabilities.max_scan_reporting_threshold = 10;
        return true;
    }

    public ChannelHelper getChannelHelper() {
        return this.mChannelHelper;
    }

    public boolean startSingleScan(ScanSettings settings, ScanEventHandler eventHandler) {
        if (eventHandler == null || settings == null) {
            Log.w(TAG, "Invalid arguments for startSingleScan: settings=" + settings + ",eventHandler=" + eventHandler);
            return false;
        } else if ((this.mPendingSingleScanSettings != null || (this.mLastScanSettings != null && this.mLastScanSettings.singleScanActive)) && SupplicantState.SCANNING == this.mWifiManager.getConnectionInfo().getSupplicantState()) {
            Log.w(TAG, "A single scan is already running");
            return false;
        } else {
            synchronized (this.mSettingsLock) {
                this.mPendingSingleScanSettings = settings;
                this.mPendingSingleScanEventHandler = eventHandler;
                processPendingScans();
            }
            return true;
        }
    }

    public ScanData getLatestSingleScanResults() {
        return this.mLatestSingleScanResult;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean startBatchedScan(ScanSettings settings, ScanEventHandler eventHandler) {
        if (settings == null || eventHandler == null) {
            Log.w(TAG, "Invalid arguments for startBatched: settings=" + settings + ",eventHandler=" + eventHandler);
            return false;
        } else if (settings.max_ap_per_scan < 0 || settings.max_ap_per_scan > 32 || settings.num_buckets < 0 || settings.num_buckets > 16 || settings.report_threshold_num_scans < 0 || settings.report_threshold_num_scans > 10 || settings.report_threshold_percent < 0 || settings.report_threshold_percent > 100 || settings.base_period_ms <= 0) {
            return false;
        } else {
            for (int i = 0; i < settings.num_buckets; i++) {
                if (settings.buckets[i].period_ms % settings.base_period_ms != 0) {
                    return false;
                }
            }
            synchronized (this.mSettingsLock) {
                stopBatchedScan();
                this.mPendingBackgroundScanSettings = settings;
                this.mPendingBackgroundScanEventHandler = eventHandler;
                handleScanPeriod();
            }
            return true;
        }
    }

    public void stopBatchedScan() {
        synchronized (this.mSettingsLock) {
            this.mBackgroundScanSettings = null;
            this.mBackgroundScanEventHandler = null;
            this.mPendingBackgroundScanSettings = null;
            this.mPendingBackgroundScanEventHandler = null;
            this.mBackgroundScanPaused = false;
            this.mBackgroundScanPeriodPending = false;
            unscheduleScansLocked();
        }
        processPendingScans();
    }

    public void pauseBatchedScan() {
        synchronized (this.mSettingsLock) {
            if (this.mPendingBackgroundScanSettings == null) {
                this.mPendingBackgroundScanSettings = this.mBackgroundScanSettings;
                this.mPendingBackgroundScanEventHandler = this.mBackgroundScanEventHandler;
            }
            this.mBackgroundScanSettings = null;
            this.mBackgroundScanEventHandler = null;
            this.mBackgroundScanPeriodPending = false;
            this.mBackgroundScanPaused = true;
            unscheduleScansLocked();
            ScanData[] results = getLatestBatchedScanResults(true);
            if (this.mPendingBackgroundScanEventHandler != null) {
                this.mPendingBackgroundScanEventHandler.onScanPaused(results);
            }
        }
        processPendingScans();
    }

    public void restartBatchedScan() {
        synchronized (this.mSettingsLock) {
            if (this.mPendingBackgroundScanEventHandler != null) {
                this.mPendingBackgroundScanEventHandler.onScanRestarted();
            }
            this.mBackgroundScanPaused = false;
            handleScanPeriod();
        }
    }

    private boolean isScanAllowed() {
        if (this.mWifiManager.isScanAlwaysAvailable()) {
            return true;
        }
        int wifiState = this.mWifiManager.getWifiState();
        return (wifiState == 0 || wifiState == 1) ? false : true;
    }

    private void unscheduleScansLocked() {
        this.mAlarmManager.cancel(this.mScanPeriodListener);
        if (this.mLastScanSettings != null) {
            this.mLastScanSettings.backgroundScanActive = false;
        }
    }

    private void handleScanPeriod() {
        synchronized (this.mSettingsLock) {
            this.mBackgroundScanPeriodPending = true;
            processPendingScans();
        }
    }

    private void handleScanTimeout() {
        Log.e(TAG, "Timed out waiting for scan result from wificond");
        reportScanFailure();
        processPendingScans();
    }

    private boolean isDifferentPnoScanSettings(LastScanSettings newScanSettings) {
        return this.mLastScanSettings != null ? Arrays.equals(newScanSettings.pnoNetworkList, this.mLastScanSettings.pnoNetworkList) ^ 1 : true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void processPendingScans() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings == null || (this.mLastScanSettings.hwPnoScanActive ^ 1) == 0) {
                ChannelCollection allFreqs = this.mChannelHelper.createChannelCollection();
                Set<String> hiddenNetworkSSIDSet = new HashSet();
                final LastScanSettings newScanSettings = new LastScanSettings(this.mClock.getElapsedSinceBootMillis());
                if (!this.mBackgroundScanPaused) {
                    if (this.mPendingBackgroundScanSettings != null) {
                        this.mBackgroundScanSettings = this.mPendingBackgroundScanSettings;
                        this.mBackgroundScanEventHandler = this.mPendingBackgroundScanEventHandler;
                        this.mNextBackgroundScanPeriod = 0;
                        this.mPendingBackgroundScanSettings = null;
                        this.mPendingBackgroundScanEventHandler = null;
                        this.mBackgroundScanPeriodPending = true;
                    }
                    if (this.mBackgroundScanPeriodPending && this.mBackgroundScanSettings != null) {
                        int reportEvents = 4;
                        for (int bucket_id = 0; bucket_id < this.mBackgroundScanSettings.num_buckets; bucket_id++) {
                            BucketSettings bucket = this.mBackgroundScanSettings.buckets[bucket_id];
                            if (this.mNextBackgroundScanPeriod % (bucket.period_ms / this.mBackgroundScanSettings.base_period_ms) == 0) {
                                if ((bucket.report_events & 1) != 0) {
                                    reportEvents |= 1;
                                }
                                if ((bucket.report_events & 2) != 0) {
                                    reportEvents |= 2;
                                }
                                if ((bucket.report_events & 4) == 0) {
                                    reportEvents &= -5;
                                }
                                allFreqs.addChannels(bucket);
                            }
                        }
                        if (!allFreqs.isEmpty()) {
                            int i = this.mNextBackgroundScanId;
                            this.mNextBackgroundScanId = i + 1;
                            newScanSettings.setBackgroundScan(i, this.mBackgroundScanSettings.max_ap_per_scan, reportEvents, this.mBackgroundScanSettings.report_threshold_num_scans, this.mBackgroundScanSettings.report_threshold_percent);
                        }
                        this.mNextBackgroundScanPeriod++;
                        this.mBackgroundScanPeriodPending = false;
                        this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + ((long) this.mBackgroundScanSettings.base_period_ms), BACKGROUND_PERIOD_ALARM_TAG, this.mScanPeriodListener, this.mEventHandler);
                    }
                }
                if (this.mPendingSingleScanSettings != null) {
                    int i2;
                    boolean reportFullResults = false;
                    ChannelCollection singleScanFreqs = this.mChannelHelper.createChannelCollection();
                    for (i2 = 0; i2 < this.mPendingSingleScanSettings.num_buckets; i2++) {
                        BucketSettings bucketSettings = this.mPendingSingleScanSettings.buckets[i2];
                        if ((bucketSettings.report_events & 2) != 0) {
                            reportFullResults = true;
                        }
                        singleScanFreqs.addChannels(bucketSettings);
                        allFreqs.addChannels(bucketSettings);
                    }
                    newScanSettings.setSingleScan(reportFullResults, singleScanFreqs, this.mPendingSingleScanEventHandler);
                    HiddenNetwork[] hiddenNetworks = this.mPendingSingleScanSettings.hiddenNetworks;
                    if (hiddenNetworks != null) {
                        int numHiddenNetworks = Math.min(hiddenNetworks.length, 16);
                        for (i2 = 0; i2 < numHiddenNetworks; i2++) {
                            hiddenNetworkSSIDSet.add(hiddenNetworks[i2].ssid);
                        }
                    }
                    this.mPendingSingleScanSettings = null;
                    this.mPendingSingleScanEventHandler = null;
                }
                if (newScanSettings.backgroundScanActive || newScanSettings.singleScanActive) {
                    boolean success = false;
                    if (allFreqs.isEmpty()) {
                        Log.e(TAG, "Failed to start scan because there is no available channel to scan for");
                    } else {
                        pauseHwPnoScan();
                        Set<Integer> freqs = allFreqs.getScanFreqs();
                        success = this.mWifiNative.scan(freqs, hiddenNetworkSSIDSet);
                        if (!success) {
                            Log.e(TAG, "Failed to start scan, freqs=" + freqs);
                        }
                    }
                    if (success) {
                        this.mScanFoolProof.scanCmdSetSuccess();
                        this.mLastScanSettings = newScanSettings;
                        this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + SCAN_TIMEOUT_MS, TIMEOUT_ALARM_TAG, this.mScanTimeoutListener, this.mEventHandler);
                    } else {
                        SupplicantState supplicantState = SupplicantState.DISCONNECTED;
                        WifiInfo wifiInfo = null;
                        if (this.mWifiManager != null) {
                            wifiInfo = this.mWifiManager.getConnectionInfo();
                        }
                        if (wifiInfo != null) {
                            supplicantState = wifiInfo.getSupplicantState();
                        }
                        if (!SupplicantState.isHandshakeState(supplicantState)) {
                            this.mScanFoolProof.scanCmdRejcet();
                        }
                        this.mEventHandler.post(new Runnable() {
                            public void run() {
                                if (newScanSettings.singleScanEventHandler != null) {
                                    newScanSettings.singleScanEventHandler.onScanStatus(3);
                                }
                            }
                        });
                    }
                } else if (isHwPnoScanRequired()) {
                    boolean status;
                    newScanSettings.setHwPnoScan(this.mPnoSettings.networkList, this.mPnoEventHandler);
                    if (isDifferentPnoScanSettings(newScanSettings)) {
                        status = restartHwPnoScan(this.mPnoSettings);
                    } else {
                        status = startHwPnoScan(this.mPnoSettings);
                    }
                    if (status) {
                        this.mLastScanSettings = newScanSettings;
                    } else {
                        Log.e(TAG, "Failed to start PNO scan");
                        this.mEventHandler.post(new Runnable() {
                            public void run() {
                                if (WificondScannerImpl.this.mPnoEventHandler != null) {
                                    WificondScannerImpl.this.mPnoEventHandler.onPnoScanFailed();
                                }
                                WificondScannerImpl.this.mPnoSettings = null;
                                WificondScannerImpl.this.mPnoEventHandler = null;
                            }
                        });
                    }
                }
            }
        }
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case WifiMonitor.SCAN_RESULTS_EVENT /*147461*/:
                this.mAlarmManager.cancel(this.mScanTimeoutListener);
                this.mScanFoolProof.scanResultGot();
                pollLatestScanData();
                processPendingScans();
                break;
            case WifiMonitor.SCAN_FAILED_EVENT /*147473*/:
                Log.w(TAG, "Scan failed");
                this.mAlarmManager.cancel(this.mScanTimeoutListener);
                this.mScanFoolProof.scanResultFail();
                reportScanFailure();
                processPendingScans();
                break;
            case WifiMonitor.PNO_SCAN_RESULTS_EVENT /*147474*/:
                pollLatestScanDataForPno();
                processPendingScans();
                break;
        }
        return true;
    }

    private void reportScanFailure() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings != null) {
                if (this.mLastScanSettings.singleScanEventHandler != null) {
                    this.mLastScanSettings.singleScanEventHandler.onScanStatus(3);
                }
                this.mLastScanSettings = null;
            }
        }
    }

    private void reportPnoScanFailure() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings != null && this.mLastScanSettings.hwPnoScanActive) {
                if (this.mLastScanSettings.pnoScanEventHandler != null) {
                    this.mLastScanSettings.pnoScanEventHandler.onPnoScanFailed();
                }
                this.mPnoSettings = null;
                this.mPnoEventHandler = null;
                this.mLastScanSettings = null;
            }
        }
    }

    private void pollLatestScanDataForPno() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings == null) {
                return;
            }
            this.mNativeScanResults = this.mWifiNative.getPnoScanResults();
            List<ScanResult> hwPnoScanResults = new ArrayList();
            int numFilteredScanResults = 0;
            for (int i = 0; i < this.mNativeScanResults.size(); i++) {
                ScanResult result = ((ScanDetail) this.mNativeScanResults.get(i)).getScanResult();
                if (result.timestamp / 1000 <= this.mLastScanSettings.startTime) {
                    numFilteredScanResults++;
                } else if (this.mLastScanSettings.hwPnoScanActive) {
                    hwPnoScanResults.add(result);
                }
            }
            if (numFilteredScanResults != 0) {
                Log.d(TAG, "Filtering out " + numFilteredScanResults + " pno scan results.");
            }
            if (this.mLastScanSettings.hwPnoScanActive && this.mLastScanSettings.pnoScanEventHandler != null) {
                this.mLastScanSettings.pnoScanEventHandler.onPnoNetworkFound((ScanResult[]) hwPnoScanResults.toArray(new ScanResult[hwPnoScanResults.size()]));
            }
            if (this.mLastScanSettings.singleScanActive && this.mLastScanSettings.singleScanEventHandler != null) {
                Log.w(TAG, "Polling pno scan result when single scan is active, reporting single scan failure");
                this.mLastScanSettings.singleScanEventHandler.onScanStatus(3);
            }
            this.mLastScanSettings = null;
        }
    }

    private static boolean isAllChannelsScanned(ChannelCollection channelCollection) {
        if (channelCollection.containsBand(1)) {
            return channelCollection.containsBand(2);
        }
        return false;
    }

    private void pollLatestScanData() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings == null) {
                return;
            }
            int i;
            if (this.mWifiManager.getWifiState() == 0) {
                Log.e(TAG, "Wifi in turning OFF state, dont get scanresults");
                this.mNativeScanResults = new ArrayList();
            } else {
                this.mNativeScanResults = this.mWifiNative.getScanResults();
            }
            List<ScanResult> singleScanResults = new ArrayList();
            List<ScanResult> backgroundScanResults = new ArrayList();
            for (i = 0; i < this.mNativeScanResults.size(); i++) {
                ScanResult result = ((ScanDetail) this.mNativeScanResults.get(i)).getScanResult();
                long timestamp_ms = result.timestamp / 1000;
                if (this.mLastScanSettings.backgroundScanActive) {
                    backgroundScanResults.add(result);
                }
                if (this.mLastScanSettings.singleScanActive && this.mLastScanSettings.singleScanFreqs.containsChannel(result.frequency)) {
                    singleScanResults.add(result);
                }
            }
            if (this.mLastScanSettings.backgroundScanActive) {
                if (!(this.mBackgroundScanEventHandler == null || (this.mLastScanSettings.reportEvents & 2) == 0)) {
                    for (ScanResult scanResult : backgroundScanResults) {
                        this.mBackgroundScanEventHandler.onFullScanResult(scanResult, 0);
                    }
                }
                Collections.sort(backgroundScanResults, SCAN_RESULT_SORT_COMPARATOR);
                ScanResult[] scanResultsArray = new ScanResult[Math.min(this.mLastScanSettings.maxAps, backgroundScanResults.size())];
                for (i = 0; i < scanResultsArray.length; i++) {
                    scanResultsArray[i] = (ScanResult) backgroundScanResults.get(i);
                }
                if ((this.mLastScanSettings.reportEvents & 4) == 0) {
                    this.mBackgroundScanBuffer.add(new ScanData(this.mLastScanSettings.scanId, 0, scanResultsArray));
                }
                if (!(this.mBackgroundScanEventHandler == null || ((this.mLastScanSettings.reportEvents & 2) == 0 && (this.mLastScanSettings.reportEvents & 1) == 0 && (this.mLastScanSettings.reportEvents != 0 || (this.mBackgroundScanBuffer.size() < (this.mBackgroundScanBuffer.capacity() * this.mLastScanSettings.reportPercentThreshold) / 100 && this.mBackgroundScanBuffer.size() < this.mLastScanSettings.reportNumScansThreshold))))) {
                    this.mBackgroundScanEventHandler.onScanStatus(0);
                }
            }
            if (this.mLastScanSettings.singleScanActive && this.mLastScanSettings.singleScanEventHandler != null) {
                if (this.mLastScanSettings.reportSingleScanFullResults) {
                    for (ScanResult scanResult2 : singleScanResults) {
                        this.mLastScanSettings.singleScanEventHandler.onFullScanResult(scanResult2, 0);
                    }
                }
                Collections.sort(singleScanResults, SCAN_RESULT_SORT_COMPARATOR);
                this.mLatestSingleScanResult = new ScanData(this.mLastScanSettings.scanId, 0, 0, isAllChannelsScanned(this.mLastScanSettings.singleScanFreqs), (ScanResult[]) singleScanResults.toArray(new ScanResult[singleScanResults.size()]));
                this.mLastScanSettings.singleScanEventHandler.onScanStatus(0);
            }
            this.mLastScanSettings = null;
        }
    }

    public ScanData[] getLatestBatchedScanResults(boolean flush) {
        ScanData[] results;
        synchronized (this.mSettingsLock) {
            results = this.mBackgroundScanBuffer.get();
            if (flush) {
                this.mBackgroundScanBuffer.clear();
            }
        }
        return results;
    }

    private boolean startHwPnoScan(PnoSettings pnoSettings) {
        return this.mHwPnoDebouncer.startPnoScan(pnoSettings, this.mHwPnoDebouncerListener);
    }

    private void stopHwPnoScan() {
        this.mHwPnoDebouncer.stopPnoScan();
    }

    private void pauseHwPnoScan() {
        this.mHwPnoDebouncer.forceStopPnoScan();
    }

    private boolean restartHwPnoScan(PnoSettings pnoSettings) {
        this.mHwPnoDebouncer.forceStopPnoScan();
        return this.mHwPnoDebouncer.startPnoScan(pnoSettings, this.mHwPnoDebouncerListener);
    }

    private boolean isHwPnoScanRequired(boolean isConnectedPno) {
        return (isConnectedPno ^ 1) & this.mHwPnoScanSupported;
    }

    private boolean isHwPnoScanRequired() {
        if (this.mPnoSettings == null) {
            return false;
        }
        return isHwPnoScanRequired(this.mPnoSettings.isConnected);
    }

    public boolean setHwPnoList(PnoSettings settings, PnoEventHandler eventHandler) {
        synchronized (this.mSettingsLock) {
            if (this.mPnoSettings != null) {
                Log.w(TAG, "Already running a PNO scan");
                return false;
            }
            this.mPnoEventHandler = eventHandler;
            this.mPnoSettings = settings;
            processPendingScans();
            return true;
        }
    }

    public boolean resetHwPnoList() {
        synchronized (this.mSettingsLock) {
            if (this.mPnoSettings == null) {
                Log.w(TAG, "No PNO scan running");
                return false;
            }
            this.mPnoEventHandler = null;
            this.mPnoSettings = null;
            stopHwPnoScan();
            return true;
        }
    }

    public boolean isHwPnoSupported(boolean isConnectedPno) {
        return isHwPnoScanRequired(isConnectedPno);
    }

    public boolean shouldScheduleBackgroundScanForHwPno() {
        return false;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mSettingsLock) {
            pw.println("Latest native scan results:");
            if (!(this.mNativeScanResults == null || this.mNativeScanResults.size() == 0)) {
                long nowMs = this.mClock.getElapsedSinceBootMillis();
                pw.println("    BSSID              Frequency  RSSI  Age(sec)   SSID                                 Flags");
                for (ScanDetail scanDetail : this.mNativeScanResults) {
                    ScanResult r = scanDetail.getScanResult();
                    long timeStampMs = r.timestamp / 1000;
                    String age;
                    if (timeStampMs <= 0) {
                        age = "___?___";
                    } else if (nowMs < timeStampMs) {
                        age = "  0.000";
                    } else if (timeStampMs < nowMs - 1000000) {
                        age = ">1000.0";
                    } else {
                        age = String.format("%3.3f", new Object[]{Double.valueOf(((double) (nowMs - timeStampMs)) / 1000.0d)});
                    }
                    String ssid = r.SSID == null ? "" : r.SSID;
                    r13 = new Object[6];
                    r13[4] = String.format("%1.32s", new Object[]{ssid});
                    r13[5] = r.capabilities;
                    pw.printf("  %17s  %9d  %5d   %7s    %-32s  %s\n", r13);
                }
            }
        }
    }
}
