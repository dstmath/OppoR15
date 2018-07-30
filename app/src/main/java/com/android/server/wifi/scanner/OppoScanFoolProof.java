package com.android.server.wifi.scanner;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.IWifiP2pManager.Stub;
import android.os.Handler;
import android.os.OppoAssertTip;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.server.WifiRomUpdateHelper;
import com.android.server.wifi.Clock;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import java.util.HashMap;
import oppo.util.OppoStatistics;

public class OppoScanFoolProof {
    private static final int DELAY = 5000;
    private static final String MAP_KEY = "mapKey-";
    private static final int SCAN_DELTA = 15000;
    private static final int SCAN_RESULT_TIMEOUT_DELTA = 20000;
    private static final int SCAN_RESULT_TIMEOUT_REPORT = 30000;
    public static final String SCAN_RESULT_TIMEOUT_TAG = "Fp Scan result timer";
    private static final int SECOND_DELTA = 180;
    private static final String TAG = "OppoScanFoolProof";
    protected final AlarmManager mAlarmManager;
    private OppoAssertTip mAssertProxy = null;
    private final BroadcastReceiver mBroadcastReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.e(OppoScanFoolProof.TAG, "intent is null");
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                Log.e(OppoScanFoolProof.TAG, "action is null");
                return;
            }
            if (action.equals("android.net.wifi.STATE_CHANGE")) {
                OppoScanFoolProof.this.mIsConnecting = OppoScanFoolProof.isConnectingState(((NetworkInfo) intent.getParcelableExtra("networkInfo")).getDetailedState());
            } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                if (intent.getIntExtra("wifi_state", 4) == 3) {
                    OppoScanFoolProof.this.resetSFState();
                }
            } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (intent.getBooleanExtra("state", false)) {
                    OppoScanFoolProof.this.logd("am 1.");
                    OppoScanFoolProof.this.resetSFState();
                }
            } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED") && intent.getIntExtra("wifi_state", 11) == 13) {
                OppoScanFoolProof.this.resetSFState();
            }
        }
    };
    private Clock mClock;
    private Context mContext;
    private boolean mDbg = false;
    private boolean mDisableRejectRestore = false;
    protected final Handler mEventHandler;
    private long mFirstRejectTime = 0;
    private Object mFpsLock = new Object();
    private boolean mIsConnecting = false;
    private int mIsWifiScanAlwaysOn = 1;
    private long mLastRejectTime = 0;
    private boolean mRestartForScanReject = false;
    OnAlarmListener mRestartWifiListener = new OnAlarmListener() {
        public void onAlarm() {
            synchronized (OppoScanFoolProof.this.mFpsLock) {
                OppoScanFoolProof.this.restartWifi();
            }
        }
    };
    private int mScanCmdSetDownCount = 0;
    private int mScanRejectCount = 0;
    OnAlarmListener mScanResultTimeoutListener = new OnAlarmListener() {
        public void onAlarm() {
            synchronized (OppoScanFoolProof.this.mFpsLock) {
                OppoScanFoolProof.this.scanResultTimeOut();
            }
        }
    };
    private int mVerboseLoggingLevel = 0;
    private WifiManager mWifiManager;
    private WifiP2pServiceImpl mWifiP2pServiceImpl;
    private WifiRomUpdateHelper mWifiRomUpdateHelper = null;

    private void resetSFState() {
        logd("resetSFState");
        this.mScanCmdSetDownCount = 0;
        this.mScanRejectCount = 0;
        this.mFirstRejectTime = 0;
        if (this.mAlarmManager != null) {
            this.mAlarmManager.cancel(this.mScanResultTimeoutListener);
        }
    }

    public OppoScanFoolProof(Handler handler, Clock clock, Context context) {
        int i;
        this.mContext = context;
        this.mWifiP2pServiceImpl = (WifiP2pServiceImpl) Stub.asInterface(ServiceManager.getService("wifip2p"));
        if (SystemProperties.getBoolean(WifiStateMachine.DEBUG_PROPERTY, false)) {
            i = 1;
        } else {
            i = 0;
        }
        this.mVerboseLoggingLevel = i;
        this.mEventHandler = handler;
        this.mClock = clock;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mWifiRomUpdateHelper = new WifiRomUpdateHelper(this.mContext);
        if (this.mWifiRomUpdateHelper != null) {
            this.mWifiRomUpdateHelper.enableVerboseLogging(getVerboseLoggingLevel());
        }
        this.mIsConnecting = false;
        this.mAssertProxy = OppoAssertTip.getInstance();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        this.mContext.registerReceiver(this.mBroadcastReciever, intentFilter, null, this.mEventHandler);
        enableVerboseLogging(this.mVerboseLoggingLevel);
    }

    int getVerboseLoggingLevel() {
        int i = 0;
        if (SystemProperties.getBoolean(WifiStateMachine.DEBUG_PROPERTY, false)) {
            i = 1;
        }
        this.mVerboseLoggingLevel = i;
        return this.mVerboseLoggingLevel;
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mDbg = true;
        } else {
            this.mDbg = false;
        }
    }

    public String getRomUpdateValue(String key, String defaultVal) {
        if (this.mWifiRomUpdateHelper != null) {
            return this.mWifiRomUpdateHelper.getValue(key, defaultVal);
        }
        return defaultVal;
    }

    public Integer getRomUpdateIntegerValue(String key, Integer defaultVal) {
        if (this.mWifiRomUpdateHelper != null) {
            return this.mWifiRomUpdateHelper.getIntegerValue(key, defaultVal);
        }
        return defaultVal;
    }

    public Double getRomUpdateFloatValue(String key, Double defaultVal) {
        if (this.mWifiRomUpdateHelper != null) {
            return this.mWifiRomUpdateHelper.getFloatValue(key, defaultVal);
        }
        return defaultVal;
    }

    public Long getRomUpdateLongValue(String key, Long defaultVal) {
        if (this.mWifiRomUpdateHelper != null) {
            return this.mWifiRomUpdateHelper.getLongValue(key, defaultVal);
        }
        return defaultVal;
    }

    public Boolean getRomUpdateBooleanValue(String key, Boolean defaultVal) {
        if (this.mWifiRomUpdateHelper != null) {
            return Boolean.valueOf(this.mWifiRomUpdateHelper.getBooleanValue(key, defaultVal.booleanValue()));
        }
        return defaultVal;
    }

    public void setStatistics(String mapValue, String eventId) {
        HashMap<String, String> map = new HashMap();
        map.put(MAP_KEY, mapValue);
        Log.d(TAG, " onCommon eventId = " + eventId);
        OppoStatistics.onCommon(this.mContext, "wifi_fool_proof", eventId, map, false);
    }

    public static boolean isConnectingState(DetailedState state) {
        if (state != DetailedState.CONNECTING && state != DetailedState.AUTHENTICATING && state != DetailedState.DISCONNECTING && state != DetailedState.OBTAINING_IPADDR && state != DetailedState.BLOCKED && state != DetailedState.CAPTIVE_PORTAL_CHECK && state != DetailedState.VERIFYING_POOR_LINK) {
            return false;
        }
        Log.d(TAG, "isConnectingState DetailedState is " + state);
        return true;
    }

    private boolean isHandshakeState() {
        return this.mIsConnecting;
    }

    public void scanResultGot() {
        logd("scanResultGot, count=" + this.mScanCmdSetDownCount);
        if (this.mScanCmdSetDownCount > 0) {
            this.mScanCmdSetDownCount = 0;
            this.mAlarmManager.cancel(this.mScanResultTimeoutListener);
        }
        this.mScanRejectCount = 0;
    }

    public void scanResultFail() {
        logd("scanResultFail");
        this.mAlarmManager.cancel(this.mScanResultTimeoutListener);
    }

    public void scanCmdSetSuccess() {
        logd("scanCmdSetSuccess count=" + this.mScanCmdSetDownCount);
        this.mScanCmdSetDownCount++;
        if (this.mScanCmdSetDownCount == 1) {
            this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + 30000, SCAN_RESULT_TIMEOUT_TAG, this.mScanResultTimeoutListener, this.mEventHandler);
        }
        this.mScanRejectCount = 0;
        this.mFirstRejectTime = 0;
        if (this.mRestartForScanReject) {
            Log.d(TAG, "has restarted for scan rejection");
            this.mDisableRejectRestore = false;
            this.mRestartForScanReject = false;
            setStatistics("reject", "wifi_scan_reject_restored");
        }
    }

    public void scanResultTimeOut() {
        if (this.mScanCmdSetDownCount < getRomUpdateIntegerValue("BASIC_SCAN_CMD_DOWN_COUNT", Integer.valueOf(6)).intValue() || (this.mWifiP2pServiceImpl.isP2pGroupNegotiationState() ^ 1) == 0) {
            this.mAlarmManager.set(2, (this.mClock.getElapsedSinceBootMillis() + 30000) - 20000, SCAN_RESULT_TIMEOUT_TAG, this.mScanResultTimeoutListener, this.mEventHandler);
            return;
        }
        setStatistics("unreport", "unreport_scan_results");
        Log.d(TAG, "Supplicant did not report scan results after " + this.mScanCmdSetDownCount + " SCAN cmds and 30+10*n secs");
        this.mScanCmdSetDownCount = 0;
        reportFoolProofException();
        sheduleRestartWifi();
    }

    public void scanCmdRejcet() {
        logd("scanCmdRejcet mDisableRejectRestore=" + this.mDisableRejectRestore);
        if (!this.mDisableRejectRestore) {
            long now = System.currentTimeMillis();
            if (this.mScanRejectCount == 0) {
                this.mScanRejectCount++;
                this.mFirstRejectTime = now;
                this.mLastRejectTime = now;
            }
            if (now - this.mLastRejectTime >= 15000 && (isHandshakeState() ^ 1) != 0) {
                this.mScanRejectCount++;
                this.mLastRejectTime = now;
                int secs = ((int) (now - this.mFirstRejectTime)) / 1000;
                int romUpdateSRC = getRomUpdateIntegerValue("BASIC_SCAN_REJECT_COUNT", Integer.valueOf(6)).intValue();
                logd("fool-proof,mScanRejectCount = " + this.mScanRejectCount + " reject last " + secs + "romUpdateSRC = " + romUpdateSRC);
                if (this.mScanRejectCount > romUpdateSRC && secs > SECOND_DELTA) {
                    Log.d(TAG, "fool-proof,Scan rejection >6 times and last >3 mins,maybe something bad with wpa or driver happened,restore wifi!");
                    if (this.mRestartForScanReject) {
                        this.mDisableRejectRestore = true;
                        setStatistics("reject", "wifi_scan_reject_restore_failed");
                    } else {
                        HashMap<String, String> map = new HashMap();
                        map.put("last_time", Integer.toString(secs));
                        map.put("reject_count", Integer.toString(this.mScanRejectCount));
                        OppoStatistics.onCommon(this.mContext, "wifi_fool_proof", "wifi_scan_reject_restore_going", map, false);
                        Log.d(TAG, " wifi_scan_reject_restore_going..");
                        reportFoolProofException();
                        sheduleRestartWifi();
                        this.mRestartForScanReject = true;
                    }
                    this.mScanRejectCount = 0;
                    this.mFirstRejectTime = 0;
                }
            }
        }
    }

    public void reportFoolProofException() {
        if (this.mContext.getPackageManager().hasSystemFeature("oppo.cta.support")) {
            Log.d(TAG, " CTA version don't reportFoolProofException");
            return;
        }
        if (getVerboseLoggingLevel() == 0) {
            enableVerboseLogging(1);
        }
        RuntimeException excp = new RuntimeException("Please send this log to Connectivity team ,thank you!");
        excp.fillInStackTrace();
        this.mAssertProxy.requestShowAssertMessage(Log.getStackTraceString(excp));
    }

    private void sheduleRestartWifi() {
        if (getRomUpdateIntegerValue("BASIC_FOOL_PROOF_ON", Integer.valueOf(1)).intValue() != 1) {
            Log.d(TAG, "foolProofOn != 1, don't restart!");
            return;
        }
        this.mIsWifiScanAlwaysOn = getPersistedScanAlwaysAvailable();
        setPersistedScanAlwaysAvailable(0);
        this.mWifiManager.setWifiEnabled(false);
        this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + 5000, "Scan Fool-Proof restart", this.mRestartWifiListener, this.mEventHandler);
    }

    private void restartWifi() {
        setPersistedScanAlwaysAvailable(this.mIsWifiScanAlwaysOn);
        this.mWifiManager.setWifiEnabled(true);
    }

    private int getPersistedScanAlwaysAvailable() {
        return Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0);
    }

    private void setPersistedScanAlwaysAvailable(int value) {
        Global.putInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", value);
    }

    private void logd(String msg) {
        if (this.mDbg) {
            Log.d(TAG, "" + msg);
        }
    }
}
