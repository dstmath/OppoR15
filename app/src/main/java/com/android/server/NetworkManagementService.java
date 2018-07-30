package com.android.server;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetd;
import android.net.INetworkManagementEventObserver;
import android.net.ITetheringStatsProvider;
import android.net.InterfaceConfiguration;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.Network;
import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.util.NetdService;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Handler;
import android.os.INetworkActivityListener;
import android.os.INetworkManagementService.Stub;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.Preconditions;
import com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg.AnonymousClass3;
import com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg.AnonymousClass4;
import com.android.server.NativeDaemonConnector.Command;
import com.android.server.Watchdog.Monitor;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import com.google.android.collect.Maps;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

public class NetworkManagementService extends Stub implements Monitor {
    static final int DAEMON_MSG_MOBILE_CONN_REAL_TIME_INFO = 1;
    private static final boolean DBG = Log.isLoggable(TAG, 3);
    private static boolean DBGOPPO = false;
    public static final int DNS_RESOLVER_DEFAULT_MAX_SAMPLES = 64;
    public static final int DNS_RESOLVER_DEFAULT_MIN_SAMPLES = 8;
    public static final int DNS_RESOLVER_DEFAULT_SAMPLE_VALIDITY_SECONDS = 1800;
    public static final int DNS_RESOLVER_DEFAULT_SUCCESS_THRESHOLD_PERCENT = 25;
    public static final String LIMIT_GLOBAL_ALERT = "globalAlert";
    private static final int MAX_UID_RANGES_PER_COMMAND = 10;
    private static final String NETD_SERVICE_NAME = "netd";
    private static final String NETD_TAG = "NetdConnector";
    public static final String PERMISSION_NETWORK = "NETWORK";
    public static final String PERMISSION_SYSTEM = "SYSTEM";
    static final String SOFT_AP_COMMAND = "softap";
    static final String SOFT_AP_COMMAND_SUCCESS = "Ok";
    private static final String TAG = "NetworkManagement";
    @GuardedBy("mQuotaLock")
    private HashMap<String, Long> mActiveAlerts;
    private HashMap<String, IdleTimerParams> mActiveIdleTimers;
    @GuardedBy("mQuotaLock")
    private HashMap<String, Long> mActiveQuotas;
    private volatile boolean mBandwidthControlEnabled;
    private IBatteryStats mBatteryStats;
    private CountDownLatch mConnectedSignal;
    private final NativeDaemonConnector mConnector;
    private final Context mContext;
    private final Handler mDaemonHandler;
    @GuardedBy("mQuotaLock")
    private volatile boolean mDataSaverMode;
    private final Handler mFgHandler;
    @GuardedBy("mRulesLock")
    final SparseBooleanArray mFirewallChainStates;
    private volatile boolean mFirewallEnabled;
    private final Object mIdleTimerLock;
    private int mLastPowerStateFromRadio;
    private int mLastPowerStateFromWifi;
    private boolean mMobileActivityFromRadio;
    private INetd mNetdService;
    private boolean mNetworkActive;
    private final RemoteCallbackList<INetworkActivityListener> mNetworkActivityListeners;
    private final RemoteCallbackList<INetworkManagementEventObserver> mObservers;
    private final Object mQuotaLock;
    private final Object mRulesLock;
    private final NetworkStatsFactory mStatsFactory;
    private volatile boolean mStrictEnabled;
    @GuardedBy("mTetheringStatsProviders")
    private final HashMap<ITetheringStatsProvider, String> mTetheringStatsProviders;
    private final Thread mThread;
    @GuardedBy("mRulesLock")
    private SparseBooleanArray mUidAllowOnMetered;
    @GuardedBy("mQuotaLock")
    private SparseIntArray mUidCleartextPolicy;
    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallDozableRules;
    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallPowerSaveRules;
    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallRules;
    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallStandbyRules;
    @GuardedBy("mRulesLock")
    private SparseBooleanArray mUidRejectOnMetered;

    @FunctionalInterface
    private interface NetworkManagementEventCallback {
        void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) throws RemoteException;
    }

    private static class IdleTimerParams {
        public int networkCount = 1;
        public final int timeout;
        public final int type;

        IdleTimerParams(int timeout, int type) {
            this.timeout = timeout;
            this.type = type;
        }
    }

    class Injector {
        Injector() {
        }

        void setDataSaverMode(boolean dataSaverMode) {
            NetworkManagementService.this.mDataSaverMode = dataSaverMode;
        }

        void setFirewallChainState(int chain, boolean state) {
            NetworkManagementService.this.setFirewallChainState(chain, state);
        }

        void setFirewallRule(int chain, int uid, int rule) {
            synchronized (NetworkManagementService.this.mRulesLock) {
                NetworkManagementService.this.getUidFirewallRulesLR(chain).put(uid, rule);
            }
        }

        void setUidOnMeteredNetworkList(boolean blacklist, int uid, boolean enable) {
            synchronized (NetworkManagementService.this.mRulesLock) {
                if (blacklist) {
                    NetworkManagementService.this.mUidRejectOnMetered.put(uid, enable);
                } else {
                    NetworkManagementService.this.mUidAllowOnMetered.put(uid, enable);
                }
            }
        }

        void reset() {
            synchronized (NetworkManagementService.this.mRulesLock) {
                setDataSaverMode(false);
                for (int chain : new int[]{1, 2, 3}) {
                    setFirewallChainState(chain, false);
                    NetworkManagementService.this.getUidFirewallRulesLR(chain).clear();
                }
                NetworkManagementService.this.mUidAllowOnMetered.clear();
                NetworkManagementService.this.mUidRejectOnMetered.clear();
            }
        }
    }

    class LocalService extends NetworkManagementInternal {
        LocalService() {
        }

        public boolean isNetworkRestrictedForUid(int uid) {
            return NetworkManagementService.this.isNetworkRestrictedInternal(uid);
        }
    }

    private class NetdCallbackReceiver implements INativeDaemonConnectorCallbacks {
        /* synthetic */ NetdCallbackReceiver(NetworkManagementService this$0, NetdCallbackReceiver -this1) {
            this();
        }

        private NetdCallbackReceiver() {
        }

        public void onDaemonConnected() {
            Slog.i(NetworkManagementService.TAG, "onDaemonConnected()");
            if (NetworkManagementService.this.mConnectedSignal != null) {
                NetworkManagementService.this.mConnectedSignal.countDown();
                NetworkManagementService.this.mConnectedSignal = null;
                return;
            }
            NetworkManagementService.this.mFgHandler.post(new Runnable() {
                public void run() {
                    NetworkManagementService.this.connectNativeNetdService();
                    NetworkManagementService.this.prepareNativeDaemon();
                }
            });
        }

        public boolean onCheckHoldWakeLock(int code) {
            return code == NetdResponseCode.InterfaceClassActivity;
        }

        public boolean onEvent(int code, String raw, String[] cooked) {
            String errorMessage = String.format("Invalid event from daemon (%s)", new Object[]{raw});
            switch (code) {
                case 600:
                    if (cooked.length < 4 || (cooked[1].equals("Iface") ^ 1) != 0) {
                        throw new IllegalStateException(errorMessage);
                    } else if (cooked[2].equals("added")) {
                        NetworkManagementService.this.notifyInterfaceAdded(cooked[3]);
                        return true;
                    } else if (cooked[2].equals("removed")) {
                        NetworkManagementService.this.notifyInterfaceRemoved(cooked[3]);
                        return true;
                    } else if (cooked[2].equals("changed") && cooked.length == 5) {
                        NetworkManagementService.this.notifyInterfaceStatusChanged(cooked[3], cooked[4].equals("up"));
                        return true;
                    } else if (cooked[2].equals("linkstate") && cooked.length == 5) {
                        NetworkManagementService.this.notifyInterfaceLinkStateChanged(cooked[3], cooked[4].equals("up"));
                        return true;
                    } else {
                        throw new IllegalStateException(errorMessage);
                    }
                case NetdResponseCode.BandwidthControl /*601*/:
                    if (cooked.length < 5 || (cooked[1].equals("limit") ^ 1) != 0) {
                        throw new IllegalStateException(errorMessage);
                    } else if (cooked[2].equals("alert")) {
                        NetworkManagementService.this.notifyLimitReached(cooked[3], cooked[4]);
                        return true;
                    } else {
                        throw new IllegalStateException(errorMessage);
                    }
                case NetdResponseCode.InterfaceClassActivity /*613*/:
                    if (cooked.length < 4 || (cooked[1].equals("IfaceClass") ^ 1) != 0) {
                        throw new IllegalStateException(errorMessage);
                    }
                    int i;
                    long timestampNanos = 0;
                    int processUid = -1;
                    if (cooked.length >= 5) {
                        try {
                            timestampNanos = Long.parseLong(cooked[4]);
                            if (cooked.length == 6) {
                                processUid = Integer.parseInt(cooked[5]);
                            }
                        } catch (NumberFormatException e) {
                        }
                    } else {
                        timestampNanos = SystemClock.elapsedRealtimeNanos();
                    }
                    boolean isActive = cooked[2].equals("active");
                    NetworkManagementService networkManagementService = NetworkManagementService.this;
                    int parseInt = Integer.parseInt(cooked[3]);
                    if (isActive) {
                        i = 3;
                    } else {
                        i = 1;
                    }
                    networkManagementService.notifyInterfaceClassActivity(parseInt, i, timestampNanos, processUid, false);
                    return true;
                case NetdResponseCode.InterfaceAddressChange /*614*/:
                    if (cooked.length < 7 || (cooked[1].equals("Address") ^ 1) != 0) {
                        throw new IllegalStateException(errorMessage);
                    }
                    String iface = cooked[4];
                    try {
                        LinkAddress address = new LinkAddress(cooked[3], Integer.parseInt(cooked[5]), Integer.parseInt(cooked[6]));
                        if (cooked[2].equals("updated")) {
                            NetworkManagementService.this.notifyAddressUpdated(iface, address);
                        } else {
                            NetworkManagementService.this.notifyAddressRemoved(iface, address);
                        }
                        return true;
                    } catch (NumberFormatException e2) {
                        throw new IllegalStateException(errorMessage, e2);
                    } catch (IllegalArgumentException e3) {
                        throw new IllegalStateException(errorMessage, e3);
                    }
                case NetdResponseCode.InterfaceDnsServerInfo /*615*/:
                    if (cooked.length == 6 && cooked[1].equals("DnsInfo") && cooked[2].equals("servers")) {
                        try {
                            long lifetime = Long.parseLong(cooked[4]);
                            NetworkManagementService.this.notifyInterfaceDnsServerInfo(cooked[3], lifetime, cooked[5].split(","));
                        } catch (NumberFormatException e4) {
                            throw new IllegalStateException(errorMessage);
                        }
                    }
                    return true;
                case NetdResponseCode.RouteChange /*616*/:
                    if (!cooked[1].equals("Route") || cooked.length < 6) {
                        throw new IllegalStateException(errorMessage);
                    }
                    String via = null;
                    String dev = null;
                    boolean valid = true;
                    for (int i2 = 4; i2 + 1 < cooked.length && valid; i2 += 2) {
                        if (cooked[i2].equals("dev")) {
                            if (dev == null) {
                                dev = cooked[i2 + 1];
                            } else {
                                valid = false;
                            }
                        } else if (!cooked[i2].equals("via")) {
                            valid = false;
                        } else if (via == null) {
                            via = cooked[i2 + 1];
                        } else {
                            valid = false;
                        }
                    }
                    if (valid) {
                        InetAddress gateway = null;
                        if (via != null) {
                            try {
                                gateway = InetAddress.parseNumericAddress(via);
                            } catch (IllegalArgumentException e5) {
                            }
                        }
                        NetworkManagementService.this.notifyRouteChange(cooked[2], new RouteInfo(new IpPrefix(cooked[3]), gateway, dev));
                        return true;
                    }
                    throw new IllegalStateException(errorMessage);
                case NetdResponseCode.StrictCleartext /*617*/:
                    try {
                        ActivityManager.getService().notifyCleartextNetwork(Integer.parseInt(cooked[1]), HexDump.hexStringToByteArray(cooked[2]));
                        break;
                    } catch (RemoteException e6) {
                        break;
                    }
            }
            return false;
        }
    }

    static class NetdResponseCode {
        public static final int BandwidthControl = 601;
        public static final int ClatdStatusResult = 223;
        public static final int DnsProxyQueryResult = 222;
        public static final int InterfaceAddressChange = 614;
        public static final int InterfaceChange = 600;
        public static final int InterfaceClassActivity = 613;
        public static final int InterfaceDnsServerInfo = 615;
        public static final int InterfaceGetCfgResult = 213;
        public static final int InterfaceListResult = 110;
        public static final int InterfaceRxCounterResult = 216;
        public static final int InterfaceTxCounterResult = 217;
        public static final int IpFwdStatusResult = 211;
        public static final int QuotaCounterResult = 220;
        public static final int RouteChange = 616;
        public static final int SoftapStatusResult = 214;
        public static final int StrictCleartext = 617;
        public static final int TetherDnsFwdTgtListResult = 112;
        public static final int TetherInterfaceListResult = 111;
        public static final int TetherStatusResult = 210;
        public static final int TetheringStatsListResult = 114;
        public static final int TetheringStatsResult = 221;
        public static final int TtyListResult = 113;

        NetdResponseCode() {
        }
    }

    private class NetdTetheringStatsProvider extends ITetheringStatsProvider.Stub {
        /* synthetic */ NetdTetheringStatsProvider(NetworkManagementService this$0, NetdTetheringStatsProvider -this1) {
            this();
        }

        private NetdTetheringStatsProvider() {
        }

        public NetworkStats getTetherStats(int how) {
            if (how != 1) {
                return new NetworkStats(SystemClock.elapsedRealtime(), 0);
            }
            try {
                NativeDaemonEvent[] events = NetworkManagementService.this.mConnector.executeForList("bandwidth", "gettetherstats");
                NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 1);
                for (NativeDaemonEvent event : events) {
                    if (event.getCode() == 114) {
                        StringTokenizer tok = new StringTokenizer(event.getMessage());
                        try {
                            String ifaceIn = tok.nextToken();
                            String ifaceOut = tok.nextToken();
                            Entry entry = new Entry();
                            entry.iface = ifaceOut;
                            entry.uid = -5;
                            entry.set = 0;
                            entry.tag = 0;
                            entry.rxBytes = Long.parseLong(tok.nextToken());
                            entry.rxPackets = Long.parseLong(tok.nextToken());
                            entry.txBytes = Long.parseLong(tok.nextToken());
                            entry.txPackets = Long.parseLong(tok.nextToken());
                            stats.combineValues(entry);
                        } catch (NoSuchElementException e) {
                            throw new IllegalStateException("problem parsing tethering stats: " + event);
                        } catch (NumberFormatException e2) {
                            throw new IllegalStateException("problem parsing tethering stats: " + event);
                        }
                    }
                }
                return stats;
            } catch (NativeDaemonConnectorException e3) {
                throw e3.rethrowAsParcelableException();
            }
        }

        public void setInterfaceQuota(String iface, long quotaBytes) {
        }
    }

    private NetworkManagementService(Context context, String socket) {
        this.mConnectedSignal = new CountDownLatch(1);
        this.mObservers = new RemoteCallbackList();
        this.mStatsFactory = new NetworkStatsFactory();
        this.mTetheringStatsProviders = Maps.newHashMap();
        this.mQuotaLock = new Object();
        this.mRulesLock = new Object();
        this.mActiveQuotas = Maps.newHashMap();
        this.mActiveAlerts = Maps.newHashMap();
        this.mUidRejectOnMetered = new SparseBooleanArray();
        this.mUidAllowOnMetered = new SparseBooleanArray();
        this.mUidCleartextPolicy = new SparseIntArray();
        this.mUidFirewallRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mIdleTimerLock = new Object();
        this.mActiveIdleTimers = Maps.newHashMap();
        this.mMobileActivityFromRadio = false;
        this.mLastPowerStateFromRadio = 1;
        this.mLastPowerStateFromWifi = 1;
        this.mNetworkActivityListeners = new RemoteCallbackList();
        this.mContext = context;
        this.mFgHandler = new Handler(FgThread.get().getLooper());
        this.mConnector = new NativeDaemonConnector(new NetdCallbackReceiver(), socket, 10, NETD_TAG, 160, null, FgThread.get().getLooper());
        this.mThread = new Thread(this.mConnector, NETD_TAG);
        this.mDaemonHandler = new Handler(FgThread.get().getLooper());
        Watchdog.getInstance().addMonitor(this);
        LocalServices.addService(NetworkManagementInternal.class, new LocalService());
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.put(new NetdTetheringStatsProvider(), NETD_SERVICE_NAME);
        }
        try {
            DBGOPPO = "true".equalsIgnoreCase(SystemProperties.get("persist.sys.assert.panic", "false"));
        } catch (Exception e) {
            e.printStackTrace();
            DBGOPPO = false;
        }
    }

    NetworkManagementService() {
        this.mConnectedSignal = new CountDownLatch(1);
        this.mObservers = new RemoteCallbackList();
        this.mStatsFactory = new NetworkStatsFactory();
        this.mTetheringStatsProviders = Maps.newHashMap();
        this.mQuotaLock = new Object();
        this.mRulesLock = new Object();
        this.mActiveQuotas = Maps.newHashMap();
        this.mActiveAlerts = Maps.newHashMap();
        this.mUidRejectOnMetered = new SparseBooleanArray();
        this.mUidAllowOnMetered = new SparseBooleanArray();
        this.mUidCleartextPolicy = new SparseIntArray();
        this.mUidFirewallRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mIdleTimerLock = new Object();
        this.mActiveIdleTimers = Maps.newHashMap();
        this.mMobileActivityFromRadio = false;
        this.mLastPowerStateFromRadio = 1;
        this.mLastPowerStateFromWifi = 1;
        this.mNetworkActivityListeners = new RemoteCallbackList();
        this.mConnector = null;
        this.mContext = null;
        this.mDaemonHandler = null;
        this.mFgHandler = null;
        this.mThread = null;
    }

    static NetworkManagementService create(Context context, String socket) throws InterruptedException {
        NetworkManagementService service = new NetworkManagementService(context, socket);
        CountDownLatch connectedSignal = service.mConnectedSignal;
        if (DBG) {
            Slog.d(TAG, "Creating NetworkManagementService");
        }
        service.mThread.start();
        if (DBG) {
            Slog.d(TAG, "Awaiting socket connection");
        }
        connectedSignal.await();
        if (DBG) {
            Slog.d(TAG, "Connected");
        }
        if (DBG) {
            Slog.d(TAG, "Connecting native netd service");
        }
        service.connectNativeNetdService();
        if (DBG) {
            Slog.d(TAG, "Connected");
        }
        return service;
    }

    public static NetworkManagementService create(Context context) throws InterruptedException {
        return create(context, NETD_SERVICE_NAME);
    }

    public void systemReady() {
        if (DBG) {
            long start = System.currentTimeMillis();
            prepareNativeDaemon();
            Slog.d(TAG, "Prepared in " + (System.currentTimeMillis() - start) + "ms");
            return;
        }
        prepareNativeDaemon();
    }

    private IBatteryStats getBatteryStats() {
        synchronized (this) {
            IBatteryStats iBatteryStats;
            if (this.mBatteryStats != null) {
                iBatteryStats = this.mBatteryStats;
                return iBatteryStats;
            }
            this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
            iBatteryStats = this.mBatteryStats;
            return iBatteryStats;
        }
    }

    public void registerObserver(INetworkManagementEventObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mObservers.register(observer);
    }

    public void unregisterObserver(INetworkManagementEventObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mObservers.unregister(observer);
    }

    private void invokeForAllObservers(NetworkManagementEventCallback eventCallback) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                eventCallback.sendCallback((INetworkManagementEventObserver) this.mObservers.getBroadcastItem(i));
            } catch (RemoteException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceStatusChanged(String iface, boolean up) {
        invokeForAllObservers(new AnonymousClass3((byte) 1, up, iface));
    }

    private void notifyInterfaceLinkStateChanged(String iface, boolean up) {
        invokeForAllObservers(new AnonymousClass3((byte) 0, up, iface));
    }

    private void notifyInterfaceAdded(String iface) {
        invokeForAllObservers(new -$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg((byte) 0, iface));
    }

    private void notifyInterfaceRemoved(String iface) {
        this.mActiveAlerts.remove(iface);
        this.mActiveQuotas.remove(iface);
        invokeForAllObservers(new -$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg((byte) 1, iface));
    }

    private void notifyLimitReached(String limitName, String iface) {
        invokeForAllObservers(new com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg.AnonymousClass1((byte) 2, limitName, iface));
    }

    private void notifyInterfaceClassActivity(int type, int powerState, long tsNanos, int uid, boolean fromRadio) {
        boolean isMobile = ConnectivityManager.isNetworkTypeMobile(type);
        if (isMobile) {
            if (fromRadio) {
                this.mMobileActivityFromRadio = true;
            } else if (this.mMobileActivityFromRadio) {
                powerState = this.mLastPowerStateFromRadio;
            }
            if (this.mLastPowerStateFromRadio != powerState) {
                this.mLastPowerStateFromRadio = powerState;
                try {
                    getBatteryStats().noteMobileRadioPowerState(powerState, tsNanos, uid);
                } catch (RemoteException e) {
                }
            }
        }
        if (ConnectivityManager.isNetworkTypeWifi(type) && this.mLastPowerStateFromWifi != powerState) {
            this.mLastPowerStateFromWifi = powerState;
            try {
                getBatteryStats().noteWifiRadioPowerState(powerState, tsNanos, uid);
            } catch (RemoteException e2) {
            }
        }
        boolean isActive = powerState != 2 ? powerState == 3 : true;
        if (!(isMobile && !fromRadio && (this.mMobileActivityFromRadio ^ 1) == 0)) {
            invokeForAllObservers(new AnonymousClass4(isActive, type, tsNanos));
        }
        boolean report = false;
        synchronized (this.mIdleTimerLock) {
            if (this.mActiveIdleTimers.isEmpty()) {
                isActive = true;
            }
            if (this.mNetworkActive != isActive) {
                this.mNetworkActive = isActive;
                report = isActive;
            }
        }
        if (report) {
            reportNetworkActive();
        }
    }

    public void registerTetheringStatsProvider(ITetheringStatsProvider provider, String name) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        Preconditions.checkNotNull(provider);
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.put(provider, name);
        }
    }

    public void unregisterTetheringStatsProvider(ITetheringStatsProvider provider) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.remove(provider);
        }
    }

    public void tetherLimitReached(ITetheringStatsProvider provider) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        synchronized (this.mTetheringStatsProviders) {
            if (this.mTetheringStatsProviders.containsKey(provider)) {
                notifyLimitReached(LIMIT_GLOBAL_ALERT, null);
                return;
            }
        }
    }

    private void syncFirewallChainLocked(int chain, String name) {
        SparseIntArray rules;
        synchronized (this.mRulesLock) {
            SparseIntArray uidFirewallRules = getUidFirewallRulesLR(chain);
            rules = uidFirewallRules.clone();
            uidFirewallRules.clear();
        }
        if (rules.size() > 0) {
            if (DBG) {
                Slog.d(TAG, "Pushing " + rules.size() + " active firewall " + name + "UID rules");
            }
            for (int i = 0; i < rules.size(); i++) {
                setFirewallUidRuleLocked(chain, rules.keyAt(i), rules.valueAt(i));
            }
        }
    }

    private void connectNativeNetdService() {
        this.mNetdService = NetdService.get();
    }

    private void prepareNativeDaemon() {
        this.mBandwidthControlEnabled = false;
        synchronized (this.mQuotaLock) {
            int i;
            if (new File("/proc/net/xt_qtaguid/ctrl").exists()) {
                if (DBGOPPO) {
                    Slog.d(TAG, "enabling bandwidth control");
                }
                try {
                    this.mConnector.execute("bandwidth", "enable");
                    this.mBandwidthControlEnabled = true;
                } catch (NativeDaemonConnectorException e) {
                    Log.wtf(TAG, "problem enabling bandwidth controls", e);
                }
            } else if (DBGOPPO) {
                Slog.i(TAG, "not enabling bandwidth control");
            }
            SystemProperties.set("net.qtaguid_enabled", this.mBandwidthControlEnabled ? LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON : "0");
            try {
                this.mConnector.execute("strict", "enable");
                this.mStrictEnabled = true;
            } catch (NativeDaemonConnectorException e2) {
                Log.wtf(TAG, "Failed strict enable", e2);
            }
            setDataSaverModeEnabled(this.mDataSaverMode);
            int size = this.mActiveQuotas.size();
            if (size > 0) {
                if (DBGOPPO) {
                    Slog.d(TAG, "Pushing " + size + " active quota rules");
                }
                HashMap<String, Long> activeQuotas = this.mActiveQuotas;
                this.mActiveQuotas = Maps.newHashMap();
                for (Map.Entry<String, Long> entry : activeQuotas.entrySet()) {
                    setInterfaceQuota((String) entry.getKey(), ((Long) entry.getValue()).longValue());
                }
            }
            size = this.mActiveAlerts.size();
            if (size > 0) {
                if (DBGOPPO) {
                    Slog.d(TAG, "Pushing " + size + " active alert rules");
                }
                HashMap<String, Long> activeAlerts = this.mActiveAlerts;
                this.mActiveAlerts = Maps.newHashMap();
                for (Map.Entry<String, Long> entry2 : activeAlerts.entrySet()) {
                    setInterfaceAlert((String) entry2.getKey(), ((Long) entry2.getValue()).longValue());
                }
            }
            SparseBooleanArray uidRejectOnQuota = null;
            SparseBooleanArray uidAcceptOnQuota = null;
            synchronized (this.mRulesLock) {
                size = this.mUidRejectOnMetered.size();
                if (size > 0) {
                    if (DBGOPPO) {
                        Slog.d(TAG, "Pushing " + size + " UIDs to metered blacklist rules");
                    }
                    uidRejectOnQuota = this.mUidRejectOnMetered;
                    this.mUidRejectOnMetered = new SparseBooleanArray();
                }
                size = this.mUidAllowOnMetered.size();
                if (size > 0) {
                    if (DBG) {
                        Slog.d(TAG, "Pushing " + size + " UIDs to metered whitelist rules");
                    }
                    uidAcceptOnQuota = this.mUidAllowOnMetered;
                    this.mUidAllowOnMetered = new SparseBooleanArray();
                }
            }
            if (uidRejectOnQuota != null) {
                for (i = 0; i < uidRejectOnQuota.size(); i++) {
                    setUidMeteredNetworkBlacklist(uidRejectOnQuota.keyAt(i), uidRejectOnQuota.valueAt(i));
                }
            }
            if (uidAcceptOnQuota != null) {
                for (i = 0; i < uidAcceptOnQuota.size(); i++) {
                    setUidMeteredNetworkWhitelist(uidAcceptOnQuota.keyAt(i), uidAcceptOnQuota.valueAt(i));
                }
            }
            size = this.mUidCleartextPolicy.size();
            if (size > 0) {
                if (DBG) {
                    Slog.d(TAG, "Pushing " + size + " active UID cleartext policies");
                }
                SparseIntArray local = this.mUidCleartextPolicy;
                this.mUidCleartextPolicy = new SparseIntArray();
                for (i = 0; i < local.size(); i++) {
                    setUidCleartextNetworkPolicy(local.keyAt(i), local.valueAt(i));
                }
            }
            setFirewallEnabled(this.mFirewallEnabled);
            syncFirewallChainLocked(0, "");
            syncFirewallChainLocked(2, "standby ");
            syncFirewallChainLocked(1, "dozable ");
            syncFirewallChainLocked(3, "powersave ");
            for (int chain : new int[]{2, 1, 3}) {
                if (getFirewallChainState(chain)) {
                    setFirewallChainEnabled(chain, true);
                }
            }
        }
        if (this.mBandwidthControlEnabled) {
            try {
                getBatteryStats().noteNetworkStatsEnabled();
            } catch (RemoteException e3) {
            }
        }
    }

    private void notifyAddressUpdated(String iface, LinkAddress address) {
        invokeForAllObservers(new com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg.AnonymousClass1((byte) 1, iface, address));
    }

    private void notifyAddressRemoved(String iface, LinkAddress address) {
        invokeForAllObservers(new com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg.AnonymousClass1((byte) 0, iface, address));
    }

    private void notifyInterfaceDnsServerInfo(String iface, long lifetime, String[] addresses) {
        invokeForAllObservers(new com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg.AnonymousClass2(lifetime, iface, addresses));
    }

    private void notifyRouteChange(String action, RouteInfo route) {
        if (action.equals("updated")) {
            invokeForAllObservers(new -$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg((byte) 2, route));
        } else {
            invokeForAllObservers(new -$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg((byte) 3, route));
        }
    }

    public INetd getNetdService() throws RemoteException {
        CountDownLatch connectedSignal = this.mConnectedSignal;
        if (connectedSignal != null) {
            try {
                connectedSignal.await();
            } catch (InterruptedException e) {
            }
        }
        return this.mNetdService;
    }

    public String[] listInterfaces() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("interface", "list"), 110);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public InterfaceConfiguration getInterfaceConfig(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("interface", "getcfg", iface);
            event.checkCode(NetdResponseCode.InterfaceGetCfgResult);
            StringTokenizer st = new StringTokenizer(event.getMessage());
            InterfaceConfiguration cfg = new InterfaceConfiguration();
            cfg.setHardwareAddress(st.nextToken(" "));
            InetAddress addr = null;
            int prefixLength = 0;
            try {
                addr = NetworkUtils.numericToInetAddress(st.nextToken());
            } catch (IllegalArgumentException iae) {
                Slog.e(TAG, "Failed to parse ipaddr", iae);
            }
            try {
                prefixLength = Integer.parseInt(st.nextToken());
                cfg.setLinkAddress(new LinkAddress(addr, prefixLength));
                while (st.hasMoreTokens()) {
                    cfg.setFlag(st.nextToken());
                }
                return cfg;
            } catch (NoSuchElementException e) {
                throw new IllegalStateException("Invalid response from daemon: " + event);
            }
        } catch (NativeDaemonConnectorException e2) {
            throw e2.rethrowAsParcelableException();
        }
    }

    public void setInterfaceConfig(String iface, InterfaceConfiguration cfg) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        LinkAddress linkAddr = cfg.getLinkAddress();
        if (linkAddr == null || linkAddr.getAddress() == null) {
            throw new IllegalStateException("Null LinkAddress given");
        }
        Command cmd = new Command("interface", "setcfg", iface, linkAddr.getAddress().getHostAddress(), Integer.valueOf(linkAddr.getPrefixLength()));
        for (String flag : cfg.getFlags()) {
            cmd.appendArg(flag);
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setInterfaceDown(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        InterfaceConfiguration ifcg = getInterfaceConfig(iface);
        ifcg.setInterfaceDown();
        setInterfaceConfig(iface, ifcg);
    }

    public void setInterfaceUp(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        InterfaceConfiguration ifcg = getInterfaceConfig(iface);
        ifcg.setInterfaceUp();
        setInterfaceConfig(iface, ifcg);
    }

    public void setInterfaceIpv6PrivacyExtensions(String iface, boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            String str = "interface";
            Object[] objArr = new Object[3];
            objArr[0] = "ipv6privacyextensions";
            objArr[1] = iface;
            objArr[2] = enable ? "enable" : "disable";
            nativeDaemonConnector.execute(str, objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearInterfaceAddresses(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "clearaddrs", iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableIpv6(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "ipv6", iface, "enable");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIPv6AddrGenMode(String iface, int mode) throws ServiceSpecificException {
        try {
            this.mNetdService.setIPv6AddrGenMode(iface, mode);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public void disableIpv6(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "ipv6", iface, "disable");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setInterfaceIpv6NdOffload(String iface, boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            String str = "interface";
            Object[] objArr = new Object[3];
            objArr[0] = "ipv6ndoffload";
            objArr[1] = iface;
            objArr[2] = enable ? "enable" : "disable";
            nativeDaemonConnector.execute(str, objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addRoute(int netId, RouteInfo route) {
        modifyRoute("add", "" + netId, route);
    }

    public void removeRoute(int netId, RouteInfo route) {
        modifyRoute("remove", "" + netId, route);
    }

    private void modifyRoute(String action, String netId, RouteInfo route) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Command cmd = new Command("network", "route", action, netId);
        cmd.appendArg(route.getInterface());
        cmd.appendArg(route.getDestination().toString());
        switch (route.getType()) {
            case 1:
                if (route.hasGateway()) {
                    cmd.appendArg(route.getGateway().getHostAddress());
                    break;
                }
                break;
            case 7:
                cmd.appendArg("unreachable");
                break;
            case 9:
                cmd.appendArg("throw");
                break;
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Connector Failed to execute cmd ", e);
        }
    }

    private ArrayList<String> readRouteList(String filename) {
        Throwable th;
        FileInputStream fstream = null;
        ArrayList<String> list = new ArrayList();
        try {
            FileInputStream fstream2 = new FileInputStream(filename);
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(fstream2)));
                while (true) {
                    String s = br.readLine();
                    if (s != null && s.length() != 0) {
                        list.add(s);
                    } else if (fstream2 != null) {
                        try {
                            fstream2.close();
                        } catch (IOException e) {
                        }
                    }
                }
                if (fstream2 != null) {
                    try {
                        fstream2.close();
                    } catch (IOException e2) {
                    }
                }
            } catch (IOException e3) {
                fstream = fstream2;
                if (fstream != null) {
                    try {
                        fstream.close();
                    } catch (IOException e4) {
                    }
                }
                return list;
            } catch (Throwable th2) {
                th = th2;
                fstream = fstream2;
                if (fstream != null) {
                    try {
                        fstream.close();
                    } catch (IOException e5) {
                    }
                }
                throw th;
            }
        } catch (IOException e6) {
            if (fstream != null) {
                try {
                    fstream.close();
                } catch (IOException e42) {
                }
            }
            return list;
        } catch (Throwable th3) {
            th = th3;
            if (fstream != null) {
                try {
                    fstream.close();
                } catch (IOException e52) {
                }
            }
            throw th;
        }
        return list;
    }

    public void setMtu(String iface, int mtu) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("interface", "setmtu", iface, Integer.valueOf(mtu));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void shutdown() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SHUTDOWN", TAG);
        Slog.i(TAG, "Shutting down");
    }

    public boolean getIpForwardingEnabled() throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("ipfwd", "status");
            event.checkCode(211);
            return event.getMessage().endsWith("enabled");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIpForwardingEnabled(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            String str = "ipfwd";
            Object[] objArr = new Object[2];
            objArr[0] = enable ? "enable" : "disable";
            objArr[1] = ConnectivityService.TETHERING_ARG;
            nativeDaemonConnector.execute(str, objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void startTethering(String[] dhcpRange) {
        int i = 0;
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Command cmd = new Command("tether", "start");
        int length = dhcpRange.length;
        while (i < length) {
            cmd.appendArg(dhcpRange[i]);
            i++;
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopTethering() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "stop");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isTetheringStarted() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("tether", "status");
            event.checkCode(210);
            return event.getMessage().endsWith("started");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void tetherInterface(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "interface", "add", iface);
            List<RouteInfo> routes = new ArrayList();
            routes.add(new RouteInfo(getInterfaceConfig(iface).getLinkAddress(), null, iface));
            addInterfaceToLocalNetwork(iface, routes);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void untetherInterface(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "interface", "remove", iface);
            removeInterfaceFromLocalNetwork(iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        } catch (Throwable th) {
            removeInterfaceFromLocalNetwork(iface);
        }
    }

    public String[] listTetheredInterfaces() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("tether", "interface", "list"), 111);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setDnsForwarders(Network network, String[] dns) {
        int i = 0;
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        int netId = network != null ? network.netId : 0;
        Command cmd = new Command("tether", "dns", "set", Integer.valueOf(netId));
        int length = dns.length;
        while (i < length) {
            cmd.appendArg(NetworkUtils.numericToInetAddress(dns[i]).getHostAddress());
            i++;
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public String[] getDnsForwarders() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("tether", "dns", "list"), 112);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private List<InterfaceAddress> excludeLinkLocal(List<InterfaceAddress> addresses) {
        ArrayList<InterfaceAddress> filtered = new ArrayList(addresses.size());
        for (InterfaceAddress ia : addresses) {
            if (!ia.getAddress().isLinkLocalAddress()) {
                filtered.add(ia);
            }
        }
        return filtered;
    }

    private void modifyInterfaceForward(boolean add, String fromIface, String toIface) {
        String str = "ipfwd";
        Object[] objArr = new Object[3];
        objArr[0] = add ? "add" : "remove";
        objArr[1] = fromIface;
        objArr[2] = toIface;
        try {
            this.mConnector.execute(new Command(str, objArr));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void startInterfaceForwarding(String fromIface, String toIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        modifyInterfaceForward(true, fromIface, toIface);
    }

    public void stopInterfaceForwarding(String fromIface, String toIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        modifyInterfaceForward(false, fromIface, toIface);
    }

    private void modifyNat(String action, String internalInterface, String externalInterface) throws SocketException {
        Command cmd = new Command("nat", action, internalInterface, externalInterface);
        NetworkInterface internalNetworkInterface = NetworkInterface.getByName(internalInterface);
        if (internalNetworkInterface == null) {
            cmd.appendArg("0");
        } else {
            List<InterfaceAddress> interfaceAddresses = excludeLinkLocal(internalNetworkInterface.getInterfaceAddresses());
            cmd.appendArg(Integer.valueOf(interfaceAddresses.size()));
            for (InterfaceAddress ia : interfaceAddresses) {
                cmd.appendArg(NetworkUtils.getNetworkPart(ia.getAddress(), ia.getNetworkPrefixLength()).getHostAddress() + "/" + ia.getNetworkPrefixLength());
            }
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableNat(String internalInterface, String externalInterface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            modifyNat("enable", internalInterface, externalInterface);
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }

    public void disableNat(String internalInterface, String externalInterface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            modifyNat("disable", internalInterface, externalInterface);
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }

    public String[] listTtys() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("list_ttys", new Object[0]), 113);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void attachPppd(String tty, String localAddr, String remoteAddr, String dns1Addr, String dns2Addr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("pppd", "attach", tty, NetworkUtils.numericToInetAddress(localAddr).getHostAddress(), NetworkUtils.numericToInetAddress(remoteAddr).getHostAddress(), NetworkUtils.numericToInetAddress(dns1Addr).getHostAddress(), NetworkUtils.numericToInetAddress(dns2Addr).getHostAddress());
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void detachPppd(String tty) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("pppd", "detach", tty);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addIdleTimer(String iface, int timeout, final int type) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (DBG) {
            Slog.d(TAG, "Adding idletimer");
        }
        synchronized (this.mIdleTimerLock) {
            IdleTimerParams params = (IdleTimerParams) this.mActiveIdleTimers.get(iface);
            if (params != null) {
                params.networkCount++;
                return;
            }
            try {
                this.mConnector.execute("idletimer", "add", iface, Integer.toString(timeout), Integer.toString(type));
                this.mActiveIdleTimers.put(iface, new IdleTimerParams(timeout, type));
                if (ConnectivityManager.isNetworkTypeMobile(type)) {
                    this.mNetworkActive = false;
                }
                this.mDaemonHandler.post(new Runnable() {
                    public void run() {
                        NetworkManagementService.this.notifyInterfaceClassActivity(type, 3, SystemClock.elapsedRealtimeNanos(), -1, false);
                    }
                });
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeIdleTimer(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (DBG) {
            Slog.d(TAG, "Removing idletimer");
        }
        synchronized (this.mIdleTimerLock) {
            final IdleTimerParams params = (IdleTimerParams) this.mActiveIdleTimers.get(iface);
            if (params != null) {
                int i = params.networkCount - 1;
                params.networkCount = i;
                if (i <= 0) {
                    try {
                        this.mConnector.execute("idletimer", "remove", iface, Integer.toString(params.timeout), Integer.toString(params.type));
                        this.mActiveIdleTimers.remove(iface);
                        this.mDaemonHandler.post(new Runnable() {
                            public void run() {
                                NetworkManagementService.this.notifyInterfaceClassActivity(params.type, 1, SystemClock.elapsedRealtimeNanos(), -1, false);
                            }
                        });
                    } catch (NativeDaemonConnectorException e) {
                        throw e.rethrowAsParcelableException();
                    }
                }
            }
        }
    }

    public NetworkStats getNetworkStatsSummaryDev() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsSummaryDev();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public NetworkStats getNetworkStatsSummaryXt() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsSummaryXt();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public NetworkStats getNetworkStatsDetail() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsDetail(-1, null, -1, null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setInterfaceQuota(String iface, long quotaBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveQuotas.containsKey(iface)) {
                    throw new IllegalStateException("iface " + iface + " already has quota");
                }
                try {
                    this.mConnector.execute("bandwidth", "setiquota", iface, Long.valueOf(quotaBytes));
                    this.mActiveQuotas.put(iface, Long.valueOf(quotaBytes));
                    synchronized (this.mTetheringStatsProviders) {
                        for (ITetheringStatsProvider provider : this.mTetheringStatsProviders.keySet()) {
                            try {
                                provider.setInterfaceQuota(iface, quotaBytes);
                            } catch (RemoteException e) {
                                Log.e(TAG, "Problem setting tethering data limit on provider " + ((String) this.mTetheringStatsProviders.get(provider)) + ": " + e);
                            }
                        }
                    }
                } catch (NativeDaemonConnectorException e2) {
                    throw e2.rethrowAsParcelableException();
                }
            }
        }
    }

    public void removeInterfaceQuota(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveQuotas.containsKey(iface)) {
                    this.mActiveQuotas.remove(iface);
                    this.mActiveAlerts.remove(iface);
                    try {
                        this.mConnector.execute("bandwidth", "removeiquota", iface);
                        synchronized (this.mTetheringStatsProviders) {
                            for (ITetheringStatsProvider provider : this.mTetheringStatsProviders.keySet()) {
                                try {
                                    provider.setInterfaceQuota(iface, -1);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Problem removing tethering data limit on provider " + ((String) this.mTetheringStatsProviders.get(provider)) + ": " + e);
                                }
                            }
                        }
                        return;
                    } catch (NativeDaemonConnectorException e2) {
                        throw e2.rethrowAsParcelableException();
                    }
                }
            }
        }
    }

    public void setInterfaceAlert(String iface, long alertBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (!this.mBandwidthControlEnabled) {
            return;
        }
        if (this.mActiveQuotas.containsKey(iface)) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveAlerts.containsKey(iface)) {
                    throw new IllegalStateException("iface " + iface + " already has alert");
                }
                try {
                    this.mConnector.execute("bandwidth", "setinterfacealert", iface, Long.valueOf(alertBytes));
                    this.mActiveAlerts.put(iface, Long.valueOf(alertBytes));
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
            return;
        }
        throw new IllegalStateException("setting alert requires existing quota on iface");
    }

    public void removeInterfaceAlert(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveAlerts.containsKey(iface)) {
                    try {
                        this.mConnector.execute("bandwidth", "removeinterfacealert", iface);
                        this.mActiveAlerts.remove(iface);
                        return;
                    } catch (NativeDaemonConnectorException e) {
                        throw e.rethrowAsParcelableException();
                    }
                }
            }
        }
    }

    public void setGlobalAlert(long alertBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            try {
                this.mConnector.execute("bandwidth", "setglobalalert", Long.valueOf(alertBytes));
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    private void setUidOnMeteredNetworkList(int uid, boolean blacklist, boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            String chain = blacklist ? "naughtyapps" : "niceapps";
            String suffix = enable ? "add" : "remove";
            synchronized (this.mQuotaLock) {
                SparseBooleanArray quotaList;
                boolean oldEnable;
                synchronized (this.mRulesLock) {
                    quotaList = blacklist ? this.mUidRejectOnMetered : this.mUidAllowOnMetered;
                    oldEnable = quotaList.get(uid, false);
                }
                if (oldEnable == enable) {
                    return;
                }
                Trace.traceBegin(2097152, "inetd bandwidth");
                try {
                    this.mConnector.execute("bandwidth", suffix + chain, Integer.valueOf(uid));
                    synchronized (this.mRulesLock) {
                        if (enable) {
                            quotaList.put(uid, true);
                        } else {
                            quotaList.delete(uid);
                        }
                    }
                    Trace.traceEnd(2097152);
                } catch (NativeDaemonConnectorException e) {
                    try {
                        throw e.rethrowAsParcelableException();
                    } catch (Throwable th) {
                        Trace.traceEnd(2097152);
                    }
                }
            }
        }
    }

    public void setUidMeteredNetworkBlacklist(int uid, boolean enable) {
        setUidOnMeteredNetworkList(uid, true, enable);
    }

    public void setUidMeteredNetworkWhitelist(int uid, boolean enable) {
        setUidOnMeteredNetworkList(uid, false, enable);
    }

    public boolean setDataSaverModeEnabled(boolean enable) {
        if (DBG) {
            Log.d(TAG, "setDataSaverMode: " + enable);
        }
        synchronized (this.mQuotaLock) {
            if (this.mDataSaverMode == enable) {
                Log.w(TAG, "setDataSaverMode(): already " + this.mDataSaverMode);
                return true;
            }
            Trace.traceBegin(2097152, "bandwidthEnableDataSaver");
            try {
                boolean changed = this.mNetdService.bandwidthEnableDataSaver(enable);
                if (changed) {
                    this.mDataSaverMode = enable;
                } else {
                    Log.w(TAG, "setDataSaverMode(" + enable + "): netd command silently failed");
                }
                Trace.traceEnd(2097152);
                return changed;
            } catch (RemoteException e) {
                Log.w(TAG, "setDataSaverMode(" + enable + "): netd command failed", e);
                Trace.traceEnd(2097152);
                return false;
            } catch (Throwable th) {
                Trace.traceEnd(2097152);
            }
        }
    }

    public void setAllowOnlyVpnForUids(boolean add, UidRange[] uidRanges) throws ServiceSpecificException {
        try {
            this.mNetdService.networkRejectNonSecureVpn(add, uidRanges);
        } catch (ServiceSpecificException e) {
            Log.w(TAG, "setAllowOnlyVpnForUids(" + add + ", " + Arrays.toString(uidRanges) + ")" + ": netd command failed", e);
            throw e;
        } catch (RemoteException e2) {
            Log.w(TAG, "setAllowOnlyVpnForUids(" + add + ", " + Arrays.toString(uidRanges) + ")" + ": netd command failed", e2);
            throw e2.rethrowAsRuntimeException();
        }
    }

    private void applyUidCleartextNetworkPolicy(int uid, int policy) {
        String policyString;
        switch (policy) {
            case 0:
                policyString = "accept";
                break;
            case 1:
                policyString = "log";
                break;
            case 2:
                policyString = "reject";
                break;
            default:
                throw new IllegalArgumentException("Unknown policy " + policy);
        }
        try {
            this.mConnector.execute("strict", "set_uid_cleartext_policy", Integer.valueOf(uid), policyString);
            this.mUidCleartextPolicy.put(uid, policy);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setUidCleartextNetworkPolicy(int uid, int policy) {
        if (Binder.getCallingUid() != uid) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        }
        synchronized (this.mQuotaLock) {
            int oldPolicy = this.mUidCleartextPolicy.get(uid, 0);
            if (oldPolicy == policy) {
            } else if (this.mStrictEnabled) {
                if (!(oldPolicy == 0 || policy == 0)) {
                    applyUidCleartextNetworkPolicy(uid, 0);
                }
                applyUidCleartextNetworkPolicy(uid, policy);
            } else {
                this.mUidCleartextPolicy.put(uid, policy);
            }
        }
    }

    public boolean isBandwidthControlEnabled() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return this.mBandwidthControlEnabled;
    }

    public NetworkStats getNetworkStatsUidDetail(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsDetail(uid, null, -1, null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public NetworkStats getNetworkStatsTethering(int how) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 1);
        synchronized (this.mTetheringStatsProviders) {
            for (ITetheringStatsProvider provider : this.mTetheringStatsProviders.keySet()) {
                try {
                    stats.combineAllValues(provider.getTetherStats(how));
                } catch (RemoteException e) {
                    Log.e(TAG, "Problem reading tethering stats from " + ((String) this.mTetheringStatsProviders.get(provider)) + ": " + e);
                }
            }
        }
        return stats;
    }

    public void setDnsConfigurationForNetwork(int netId, String[] servers, String domains) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        ContentResolver resolver = this.mContext.getContentResolver();
        int sampleValidity = Global.getInt(resolver, "dns_resolver_sample_validity_seconds", 1800);
        if (sampleValidity < 0 || sampleValidity > NetworkConstants.ARP_HWTYPE_RESERVED_HI) {
            Slog.w(TAG, "Invalid sampleValidity=" + sampleValidity + ", using default=" + 1800);
            sampleValidity = 1800;
        }
        int successThreshold = Global.getInt(resolver, "dns_resolver_success_threshold_percent", 25);
        if (successThreshold < 0 || successThreshold > 100) {
            Slog.w(TAG, "Invalid successThreshold=" + successThreshold + ", using default=" + 25);
            successThreshold = 25;
        }
        int minSamples = Global.getInt(resolver, "dns_resolver_min_samples", 8);
        int maxSamples = Global.getInt(resolver, "dns_resolver_max_samples", 64);
        if (minSamples < 0 || minSamples > maxSamples || maxSamples > 64) {
            Slog.w(TAG, "Invalid sample count (min, max)=(" + minSamples + ", " + maxSamples + "), using default=(" + 8 + ", " + 64 + ")");
            minSamples = 8;
            maxSamples = 64;
        }
        try {
            this.mNetdService.setResolverConfiguration(netId, servers, domains == null ? new String[0] : domains.split(" "), new int[]{sampleValidity, successThreshold, minSamples, maxSamples});
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void addVpnUidRanges(int netId, UidRange[] ranges) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[13];
        argv[0] = SoundModelContract.KEY_USERS;
        argv[1] = "add";
        argv[2] = Integer.valueOf(netId);
        int argc = 3;
        for (int i = 0; i < ranges.length; i++) {
            int argc2 = argc + 1;
            argv[argc] = ranges[i].toString();
            if (i == ranges.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 3;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
            argc = argc2;
        }
    }

    public void removeVpnUidRanges(int netId, UidRange[] ranges) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[13];
        argv[0] = SoundModelContract.KEY_USERS;
        argv[1] = "remove";
        argv[2] = Integer.valueOf(netId);
        int argc = 3;
        for (int i = 0; i < ranges.length; i++) {
            int argc2 = argc + 1;
            argv[argc] = ranges[i].toString();
            if (i == ranges.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 3;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
            argc = argc2;
        }
    }

    public void setFirewallEnabled(boolean enabled) {
        enforceSystemUid();
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            String str = "firewall";
            Object[] objArr = new Object[2];
            objArr[0] = "enable";
            objArr[1] = enabled ? "whitelist" : "blacklist";
            nativeDaemonConnector.execute(str, objArr);
            this.mFirewallEnabled = enabled;
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isFirewallEnabled() {
        enforceSystemUid();
        return this.mFirewallEnabled;
    }

    public void setFirewallInterfaceRule(String iface, boolean allow) {
        enforceSystemUid();
        Preconditions.checkState(this.mFirewallEnabled);
        String rule = allow ? "allow" : "deny";
        try {
            this.mConnector.execute("firewall", "set_interface_rule", iface, rule);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private void closeSocketsForFirewallChainLocked(int chain, String chainName) {
        UidRange[] ranges;
        int[] exemptUids;
        int numUids = 0;
        SparseIntArray rules;
        int i;
        if (getFirewallType(chain) == 0) {
            ranges = new UidRange[]{new UidRange(10000, Integer.MAX_VALUE)};
            synchronized (this.mRulesLock) {
                rules = getUidFirewallRulesLR(chain);
                exemptUids = new int[rules.size()];
                for (i = 0; i < exemptUids.length; i++) {
                    if (rules.valueAt(i) == 1) {
                        exemptUids[numUids] = rules.keyAt(i);
                        numUids++;
                    }
                }
            }
            if (numUids != exemptUids.length) {
                exemptUids = Arrays.copyOf(exemptUids, numUids);
            }
        } else {
            synchronized (this.mRulesLock) {
                rules = getUidFirewallRulesLR(chain);
                ranges = new UidRange[rules.size()];
                for (i = 0; i < ranges.length; i++) {
                    if (rules.valueAt(i) == 2) {
                        int uid = rules.keyAt(i);
                        ranges[numUids] = new UidRange(uid, uid);
                        numUids++;
                    }
                }
            }
            if (numUids != ranges.length) {
                ranges = (UidRange[]) Arrays.copyOf(ranges, numUids);
            }
            exemptUids = new int[0];
        }
        try {
            this.mNetdService.socketDestroy(ranges, exemptUids);
        } catch (Exception e) {
            Slog.e(TAG, "Error closing sockets after enabling chain " + chainName + ": " + e);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setFirewallChainEnabled(int chain, boolean enable) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            synchronized (this.mRulesLock) {
                if (getFirewallChainState(chain) == enable) {
                    return;
                }
                setFirewallChainState(chain, enable);
            }
        }
    }

    private int getFirewallType(int chain) {
        int i = 0;
        switch (chain) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 0;
            default:
                if (!isFirewallEnabled()) {
                    i = 1;
                }
                return i;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setFirewallUidRules(int chain, int[] uids, int[] rules) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            synchronized (this.mRulesLock) {
                int index;
                int uid;
                SparseIntArray uidFirewallRules = getUidFirewallRulesLR(chain);
                SparseIntArray newRules = new SparseIntArray();
                for (index = uids.length - 1; index >= 0; index--) {
                    uid = uids[index];
                    int rule = rules[index];
                    updateFirewallUidRuleLocked(chain, uid, rule);
                    newRules.put(uid, rule);
                }
                SparseIntArray rulesToRemove = new SparseIntArray();
                for (index = uidFirewallRules.size() - 1; index >= 0; index--) {
                    uid = uidFirewallRules.keyAt(index);
                    if (newRules.indexOfKey(uid) < 0) {
                        rulesToRemove.put(uid, 0);
                    }
                }
                for (index = rulesToRemove.size() - 1; index >= 0; index--) {
                    updateFirewallUidRuleLocked(chain, rulesToRemove.keyAt(index), 0);
                }
            }
            switch (chain) {
                case 1:
                    this.mNetdService.firewallReplaceUidChain("fw_dozable", true, uids);
                    break;
                case 2:
                    this.mNetdService.firewallReplaceUidChain("fw_standby", false, uids);
                    break;
                case 3:
                    this.mNetdService.firewallReplaceUidChain("fw_powersave", true, uids);
                    break;
                default:
                    try {
                        Slog.d(TAG, "setFirewallUidRules() called on invalid chain: " + chain);
                        break;
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Error flushing firewall chain " + chain, e);
                        break;
                    }
            }
        }
        return;
        return;
    }

    public void setFirewallUidRule(int chain, int uid, int rule) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            setFirewallUidRuleLocked(chain, uid, rule);
        }
    }

    private void setFirewallUidRuleLocked(int chain, int uid, int rule) {
        if (updateFirewallUidRuleLocked(chain, uid, rule)) {
            try {
                this.mConnector.execute("firewall", "set_uid_rule", getFirewallChainName(chain), Integer.valueOf(uid), getFirewallRuleName(chain, rule));
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean updateFirewallUidRuleLocked(int chain, int uid, int rule) {
        synchronized (this.mRulesLock) {
            SparseIntArray uidFirewallRules = getUidFirewallRulesLR(chain);
            int oldUidFirewallRule = uidFirewallRules.get(uid, 0);
            if (DBG) {
                Slog.d(TAG, "oldRule = " + oldUidFirewallRule + ", newRule=" + rule + " for uid=" + uid + " on chain " + chain);
            }
            if (oldUidFirewallRule != rule) {
                String ruleName = getFirewallRuleName(chain, rule);
                String oldRuleName = getFirewallRuleName(chain, oldUidFirewallRule);
                if (rule == 0) {
                    uidFirewallRules.delete(uid);
                } else {
                    uidFirewallRules.put(uid, rule);
                }
                boolean equals = ruleName.equals(oldRuleName) ^ 1;
                return equals;
            } else if (DBG) {
                Slog.d(TAG, "!!!!! Skipping change");
            }
        }
    }

    private String getFirewallRuleName(int chain, int rule) {
        if (getFirewallType(chain) == 0) {
            if (rule == 1) {
                return "allow";
            }
            return "deny";
        } else if (rule == 2) {
            return "deny";
        } else {
            return "allow";
        }
    }

    private SparseIntArray getUidFirewallRulesLR(int chain) {
        switch (chain) {
            case 0:
                return this.mUidFirewallRules;
            case 1:
                return this.mUidFirewallDozableRules;
            case 2:
                return this.mUidFirewallStandbyRules;
            case 3:
                return this.mUidFirewallPowerSaveRules;
            default:
                throw new IllegalArgumentException("Unknown chain:" + chain);
        }
    }

    public String getFirewallChainName(int chain) {
        switch (chain) {
            case 0:
                return "none";
            case 1:
                return "dozable";
            case 2:
                return "standby";
            case 3:
                return "powersave";
            default:
                throw new IllegalArgumentException("Unknown chain:" + chain);
        }
    }

    private static void enforceSystemUid() {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only available to AID_SYSTEM");
        }
    }

    public void startClatd(String interfaceName) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("clatd", "start", interfaceName);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopClatd(String interfaceName) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("clatd", "stop", interfaceName);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isClatdStarted(String interfaceName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("clatd", "status", interfaceName);
            event.checkCode(NetdResponseCode.ClatdStatusResult);
            return event.getMessage().endsWith("started");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void registerNetworkActivityListener(INetworkActivityListener listener) {
        this.mNetworkActivityListeners.register(listener);
    }

    public void unregisterNetworkActivityListener(INetworkActivityListener listener) {
        this.mNetworkActivityListeners.unregister(listener);
    }

    public boolean isNetworkActive() {
        boolean isEmpty;
        synchronized (this.mNetworkActivityListeners) {
            isEmpty = !this.mNetworkActive ? this.mActiveIdleTimers.isEmpty() : true;
        }
        return isEmpty;
    }

    private void reportNetworkActive() {
        int length = this.mNetworkActivityListeners.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                ((INetworkActivityListener) this.mNetworkActivityListeners.getBroadcastItem(i)).onNetworkActive();
            } catch (RemoteException e) {
            } catch (Throwable th) {
                this.mNetworkActivityListeners.finishBroadcast();
            }
        }
        this.mNetworkActivityListeners.finishBroadcast();
    }

    public void monitor() {
        if (this.mConnector != null) {
            this.mConnector.monitor();
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("NetworkManagementService NativeDaemonConnector Log:");
            this.mConnector.dump(fd, pw, args);
            pw.println();
            pw.print("Bandwidth control enabled: ");
            pw.println(this.mBandwidthControlEnabled);
            pw.print("mMobileActivityFromRadio=");
            pw.print(this.mMobileActivityFromRadio);
            pw.print(" mLastPowerStateFromRadio=");
            pw.println(this.mLastPowerStateFromRadio);
            pw.print("mNetworkActive=");
            pw.println(this.mNetworkActive);
            synchronized (this.mQuotaLock) {
                pw.print("Active quota ifaces: ");
                pw.println(this.mActiveQuotas.toString());
                pw.print("Active alert ifaces: ");
                pw.println(this.mActiveAlerts.toString());
                pw.print("Data saver mode: ");
                pw.println(this.mDataSaverMode);
                synchronized (this.mRulesLock) {
                    dumpUidRuleOnQuotaLocked(pw, "blacklist", this.mUidRejectOnMetered);
                    dumpUidRuleOnQuotaLocked(pw, "whitelist", this.mUidAllowOnMetered);
                }
            }
            synchronized (this.mRulesLock) {
                dumpUidFirewallRule(pw, "", this.mUidFirewallRules);
                pw.print("UID firewall standby chain enabled: ");
                pw.println(getFirewallChainState(2));
                dumpUidFirewallRule(pw, "standby", this.mUidFirewallStandbyRules);
                pw.print("UID firewall dozable chain enabled: ");
                pw.println(getFirewallChainState(1));
                dumpUidFirewallRule(pw, "dozable", this.mUidFirewallDozableRules);
                pw.println("UID firewall powersave chain enabled: " + getFirewallChainState(3));
                dumpUidFirewallRule(pw, "powersave", this.mUidFirewallPowerSaveRules);
            }
            synchronized (this.mIdleTimerLock) {
                pw.println("Idle timers:");
                for (Map.Entry<String, IdleTimerParams> ent : this.mActiveIdleTimers.entrySet()) {
                    pw.print("  ");
                    pw.print((String) ent.getKey());
                    pw.println(":");
                    IdleTimerParams params = (IdleTimerParams) ent.getValue();
                    pw.print("    timeout=");
                    pw.print(params.timeout);
                    pw.print(" type=");
                    pw.print(params.type);
                    pw.print(" networkCount=");
                    pw.println(params.networkCount);
                }
            }
            pw.print("Firewall enabled: ");
            pw.println(this.mFirewallEnabled);
            pw.print("Netd service status: ");
            if (this.mNetdService == null) {
                pw.println("disconnected");
            } else {
                try {
                    pw.println(this.mNetdService.isAlive() ? "alive" : "dead");
                } catch (RemoteException e) {
                    pw.println("unreachable");
                }
            }
        }
    }

    private void dumpUidRuleOnQuotaLocked(PrintWriter pw, String name, SparseBooleanArray list) {
        pw.print("UID bandwith control ");
        pw.print(name);
        pw.print(" rule: [");
        int size = list.size();
        for (int i = 0; i < size; i++) {
            pw.print(list.keyAt(i));
            if (i < size - 1) {
                pw.print(",");
            }
        }
        pw.println("]");
    }

    private void dumpUidFirewallRule(PrintWriter pw, String name, SparseIntArray rules) {
        pw.print("UID firewall ");
        pw.print(name);
        pw.print(" rule: [");
        int size = rules.size();
        for (int i = 0; i < size; i++) {
            pw.print(rules.keyAt(i));
            pw.print(":");
            pw.print(rules.valueAt(i));
            if (i < size - 1) {
                pw.print(",");
            }
        }
        pw.println("]");
    }

    public void createPhysicalNetwork(int netId, String permission) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (permission != null) {
            try {
                this.mConnector.execute("network", "create", Integer.valueOf(netId), permission);
                return;
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
        this.mConnector.execute("network", "create", Integer.valueOf(netId));
    }

    public void createVirtualNetwork(int netId, boolean hasDNS, boolean secure) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            String str = "network";
            Object[] objArr = new Object[5];
            objArr[0] = "create";
            objArr[1] = Integer.valueOf(netId);
            objArr[2] = "vpn";
            objArr[3] = hasDNS ? LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON : "0";
            objArr[4] = secure ? LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON : "0";
            nativeDaemonConnector.execute(str, objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void removeNetwork(int netId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "destroy", Integer.valueOf(netId));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addInterfaceToNetwork(String iface, int netId) {
        modifyInterfaceInNetwork("add", "" + netId, iface);
    }

    public void removeInterfaceFromNetwork(String iface, int netId) {
        modifyInterfaceInNetwork("remove", "" + netId, iface);
    }

    private void modifyInterfaceInNetwork(String action, String netId, String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "interface", action, netId, iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addLegacyRouteForNetId(int netId, RouteInfo routeInfo, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Command cmd = new Command("network", "route", "legacy", Integer.valueOf(uid), "add", Integer.valueOf(netId));
        LinkAddress la = routeInfo.getDestinationLinkAddress();
        cmd.appendArg(routeInfo.getInterface());
        cmd.appendArg(la.getAddress().getHostAddress() + "/" + la.getPrefixLength());
        if (routeInfo.hasGateway()) {
            cmd.appendArg(routeInfo.getGateway().getHostAddress());
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setDefaultNetId(int netId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "default", "set", Integer.valueOf(netId));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public NetworkStats getNetworkStatsUidDetailWithPids(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsDetailWithPids(-1);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void clearDefaultNetId() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "default", "clear");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setNetworkPermission(int netId, String permission) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (permission != null) {
            try {
                this.mConnector.execute("network", "permission", "network", "set", permission, Integer.valueOf(netId));
                return;
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
        this.mConnector.execute("network", "permission", "network", "clear", Integer.valueOf(netId));
    }

    public void setPermission(String permission, int[] uids) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[14];
        argv[0] = "permission";
        argv[1] = "user";
        argv[2] = "set";
        argv[3] = permission;
        int argc = 4;
        for (int i = 0; i < uids.length; i++) {
            int argc2 = argc + 1;
            argv[argc] = Integer.valueOf(uids[i]);
            if (i == uids.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 4;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
            argc = argc2;
        }
    }

    public void clearPermission(int[] uids) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[13];
        argv[0] = "permission";
        argv[1] = "user";
        argv[2] = "clear";
        int argc = 3;
        for (int i = 0; i < uids.length; i++) {
            int argc2 = argc + 1;
            argv[argc] = Integer.valueOf(uids[i]);
            if (i == uids.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 3;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
            argc = argc2;
        }
    }

    public void allowProtect(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "protect", "allow", Integer.valueOf(uid));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void denyProtect(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "protect", "deny", Integer.valueOf(uid));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addInterfaceToLocalNetwork(String iface, List<RouteInfo> routes) {
        modifyInterfaceInNetwork("add", "local", iface);
        for (RouteInfo route : routes) {
            if (!route.isDefaultRoute()) {
                modifyRoute("add", "local", route);
            }
        }
    }

    public void removeInterfaceFromLocalNetwork(String iface) {
        modifyInterfaceInNetwork("remove", "local", iface);
    }

    public int removeRoutesFromLocalNetwork(List<RouteInfo> routes) {
        int failures = 0;
        for (RouteInfo route : routes) {
            try {
                modifyRoute("remove", "local", route);
            } catch (IllegalStateException e) {
                failures++;
            }
        }
        return failures;
    }

    public boolean isNetworkRestricted(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return isNetworkRestrictedInternal(uid);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isNetworkRestrictedInternal(int uid) {
        synchronized (this.mRulesLock) {
            if (getFirewallChainState(2) && this.mUidFirewallStandbyRules.get(uid) == 2) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + uid + " restricted because of app standby mode");
                }
            } else if (!getFirewallChainState(1) || this.mUidFirewallDozableRules.get(uid) == 1) {
                if (!getFirewallChainState(3) || this.mUidFirewallPowerSaveRules.get(uid) == 1) {
                    if (this.mUidRejectOnMetered.get(uid)) {
                        if (DBG) {
                            Slog.d(TAG, "Uid " + uid + " restricted because of no metered data" + " in the background");
                        }
                    } else if (!this.mDataSaverMode || (this.mUidAllowOnMetered.get(uid) ^ 1) == 0) {
                        return false;
                    } else if (DBG) {
                        Slog.d(TAG, "Uid " + uid + " restricted because of data saver mode");
                    }
                } else if (DBG) {
                    Slog.d(TAG, "Uid " + uid + " restricted because of power saver mode");
                }
            } else if (DBG) {
                Slog.d(TAG, "Uid " + uid + " restricted because of device idle mode");
            }
        }
    }

    private void setFirewallChainState(int chain, boolean state) {
        synchronized (this.mRulesLock) {
            this.mFirewallChainStates.put(chain, state);
        }
    }

    private boolean getFirewallChainState(int chain) {
        boolean z;
        synchronized (this.mRulesLock) {
            z = this.mFirewallChainStates.get(chain);
        }
        return z;
    }

    Injector getInjector() {
        return new Injector();
    }
}
