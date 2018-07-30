package com.android.server.wifi;

import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.InterfaceConfiguration;
import android.net.wifi.IApInterface;
import android.net.wifi.IClientInterface;
import android.net.wifi.IInterfaceEventCallback.Stub;
import android.net.wifi.IWificond;
import android.net.wifi.WifiConfiguration;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.wifi.util.ApConfigUtil;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

public class SoftApManager implements ActiveModeManager {
    private static final boolean DBG = true;
    private static final int DNSMASQ_POLLING_INTERVAL = 1000;
    private static final int DNSMASQ_POLLING_MAX_TIMES = 10;
    private static final String TAG = "SoftApManager";
    private static final String dhcpLocation = "/data/misc/dnsmasq.leases";
    private WifiConfiguration mApConfig;
    private final IApInterface mApInterface;
    private HashMap<String, Boolean> mConnectedDeviceMap = new HashMap();
    private Context mContext;
    private final String mCountryCode;
    private boolean mDualSapMode = false;
    private HashMap<String, Boolean> mL2ConnectedDeviceMap = new HashMap();
    private int mLastSoftApNotificationId = 0;
    private final Listener mListener;
    private final INetworkManagementService mNwService;
    private int mSoftApChannel = 0;
    private final SoftApStateMachine mStateMachine;
    private final WifiApConfigStore mWifiApConfigStore;
    private final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;
    private String message;
    private Builder softApNotificationBuilder;

    private static class DnsmasqThread extends Thread {
        boolean connect_status;
        private int mInterval;
        private int mMaxTimes;
        private final SoftApManager mSoftapmgr;
        String mac_address;

        public DnsmasqThread(SoftApManager softap, String mac_address, int interval, int maxTimes, boolean connect_status) {
            super("SoftAp");
            this.mSoftapmgr = softap;
            this.mInterval = interval;
            this.mMaxTimes = maxTimes;
        }

        public void run() {
            boolean result = false;
            while (this.mMaxTimes > 0) {
                try {
                    result = this.mSoftapmgr.readDeviceInfoFromDnsmasq(this.mac_address);
                    if (result) {
                        Log.d(SoftApManager.TAG, "Successfully poll device info for " + this.mac_address);
                        break;
                    } else {
                        this.mMaxTimes--;
                        Thread.sleep((long) this.mInterval);
                    }
                } catch (Exception ex) {
                    result = false;
                    Log.e(SoftApManager.TAG, "Pulling " + this.mac_address + "error" + ex);
                }
            }
            if (!result) {
                Log.d(SoftApManager.TAG, "Pulling timeout, suppose STA uses static ip " + this.mac_address);
            }
            if (this.connect_status) {
                this.mSoftapmgr.mL2ConnectedDeviceMap.get(this.mac_address);
                this.mSoftapmgr.mConnectedDeviceMap.put(this.mac_address, Boolean.valueOf(this.connect_status));
                this.mSoftapmgr.sendTetherConnectStateChangedBroadcast();
                return;
            }
            Log.d(SoftApManager.TAG, "Device " + this.mac_address + "already disconnected, ignoring");
        }
    }

    private static class InterfaceEventHandler extends Stub {
        private SoftApStateMachine mSoftApStateMachine;

        InterfaceEventHandler(SoftApStateMachine stateMachine) {
            this.mSoftApStateMachine = stateMachine;
        }

        public void OnClientTorndownEvent(IClientInterface networkInterface) {
        }

        public void OnClientInterfaceReady(IClientInterface networkInterface) {
        }

        public void OnApTorndownEvent(IApInterface networkInterface) {
        }

        public void OnApInterfaceReady(IApInterface networkInterface) {
        }

        public void OnSoftApClientEvent(byte[] mac_address, boolean connect_status) {
            int i = 1;
            StringBuilder sb = new StringBuilder(18);
            for (byte b : mac_address) {
                if (sb.length() > 0) {
                    sb.append(':');
                }
                sb.append(String.format("%02x", new Object[]{Byte.valueOf(b)}));
            }
            Log.d(SoftApManager.TAG, "Client mac_addr = " + sb.toString() + " status = " + connect_status);
            Message msg = Message.obtain();
            msg.obj = sb.toString();
            SoftApStateMachine softApStateMachine = this.mSoftApStateMachine;
            if (!connect_status) {
                i = 0;
            }
            softApStateMachine.sendMessage(4, i, 0, msg.obj);
        }
    }

    public interface Listener {
        void onStateChanged(int i, int i2);
    }

    private class SoftApStateMachine extends StateMachine {
        public static final int CMD_AP_INTERFACE_BINDER_DEATH = 2;
        public static final int CMD_INTERFACE_STATUS_CHANGED = 3;
        public static final int CMD_SOFTAP_CLIENT_CONNECT_STATUS_CHANGED = 4;
        public static final int CMD_START = 0;
        public static final int CMD_STOP = 1;
        private final StateMachineDeathRecipient mDeathRecipient = new StateMachineDeathRecipient(this, 2);
        private final State mIdleState = new IdleState();
        private InterfaceEventHandler mInterfaceEventHandler;
        private NetworkObserver mNetworkObserver;
        private final State mStartedState = new StartedState();
        private WifiInjector mWifiInjector;
        private IWificond mWificond;

        private class IdleState extends State {
            /* synthetic */ IdleState(SoftApStateMachine this$1, IdleState -this1) {
                this();
            }

            private IdleState() {
            }

            public void enter() {
                SoftApStateMachine.this.mDeathRecipient.unlinkToDeath();
                unregisterObserver();
                SoftApStateMachine.this.mWificond = SoftApStateMachine.this.mWifiInjector.makeWificond();
                if (SoftApStateMachine.this.mWificond == null) {
                    Log.w(SoftApManager.TAG, "Failed to get wificond binder handler");
                }
                try {
                    SoftApStateMachine.this.mWificond.RegisterCallback(SoftApStateMachine.this.mInterfaceEventHandler);
                } catch (RemoteException e) {
                }
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 0:
                        SoftApManager.this.updateApState(12, 0);
                        if (!SoftApStateMachine.this.mDeathRecipient.linkToDeath(SoftApManager.this.mApInterface.asBinder())) {
                            SoftApStateMachine.this.mDeathRecipient.unlinkToDeath();
                            SoftApManager.this.updateApState(14, 0);
                            SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, 0);
                            break;
                        }
                        try {
                            SoftApStateMachine.this.mNetworkObserver = new NetworkObserver(SoftApManager.this.mApInterface.getInterfaceName());
                            SoftApManager.this.mNwService.registerObserver(SoftApStateMachine.this.mNetworkObserver);
                            int result = SoftApManager.this.startSoftAp((WifiConfiguration) message.obj);
                            if (result == 0) {
                                SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mStartedState);
                                break;
                            }
                            int failureReason = 0;
                            if (result == 1) {
                                failureReason = 1;
                            }
                            SoftApStateMachine.this.mDeathRecipient.unlinkToDeath();
                            unregisterObserver();
                            SoftApManager.this.updateApState(14, failureReason);
                            SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, failureReason);
                            break;
                        } catch (RemoteException e) {
                            SoftApStateMachine.this.mDeathRecipient.unlinkToDeath();
                            unregisterObserver();
                            SoftApManager.this.updateApState(14, 0);
                            SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, 0);
                            break;
                        }
                }
                return true;
            }

            private void unregisterObserver() {
                if (SoftApStateMachine.this.mNetworkObserver != null) {
                    try {
                        SoftApManager.this.mNwService.unregisterObserver(SoftApStateMachine.this.mNetworkObserver);
                    } catch (RemoteException e) {
                    }
                    SoftApStateMachine.this.mNetworkObserver = null;
                }
            }
        }

        private class NetworkObserver extends BaseNetworkObserver {
            private final String mIfaceName;

            NetworkObserver(String ifaceName) {
                this.mIfaceName = ifaceName;
            }

            public void interfaceLinkStateChanged(String iface, boolean up) {
                if (this.mIfaceName.equals(iface)) {
                    int i;
                    SoftApStateMachine softApStateMachine = SoftApStateMachine.this;
                    if (up) {
                        i = 1;
                    } else {
                        i = 0;
                    }
                    softApStateMachine.sendMessage(3, i, 0, this);
                }
            }
        }

        private class StartedState extends State {
            private boolean mIfaceIsUp;

            /* synthetic */ StartedState(SoftApStateMachine this$1, StartedState -this1) {
                this();
            }

            private StartedState() {
            }

            private void onUpChanged(boolean isUp) {
                if (isUp != this.mIfaceIsUp) {
                    this.mIfaceIsUp = isUp;
                    if (isUp) {
                        Log.d(SoftApManager.TAG, "SoftAp is ready for use");
                        SoftApManager.this.updateApState(13, 0);
                        SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(true, 0);
                    }
                }
            }

            public void enter() {
                this.mIfaceIsUp = false;
                InterfaceConfiguration config = null;
                try {
                    config = SoftApManager.this.mNwService.getInterfaceConfig(SoftApManager.this.mApInterface.getInterfaceName());
                } catch (RemoteException e) {
                }
                if (config != null) {
                    onUpChanged(config.isActive());
                }
            }

            public boolean processMessage(Message message) {
                boolean z = false;
                switch (message.what) {
                    case 0:
                        break;
                    case 1:
                    case 2:
                        SoftApManager.this.updateApState(10, 0);
                        SoftApManager.this.stopSoftAp();
                        if (message.what == 2) {
                            SoftApManager.this.updateApState(14, 0);
                        } else {
                            SoftApManager.this.updateApState(11, 0);
                        }
                        SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                        SoftApManager.this.clearSoftApClientsNotification();
                        SoftApManager.this.mConnectedDeviceMap.clear();
                        SoftApManager.this.mL2ConnectedDeviceMap.clear();
                        try {
                            SoftApStateMachine.this.mWificond.UnregisterCallback(SoftApStateMachine.this.mInterfaceEventHandler);
                        } catch (RemoteException e) {
                        }
                        SoftApStateMachine.this.mInterfaceEventHandler = null;
                        break;
                    case 3:
                        if (message.obj == SoftApStateMachine.this.mNetworkObserver) {
                            onUpChanged(message.arg1 == 1);
                            break;
                        }
                        break;
                    case 4:
                        SoftApManager softApManager = SoftApManager.this;
                        String str = (String) message.obj;
                        if (message.arg1 == 1) {
                            z = true;
                        }
                        softApManager.interfaceMessageRecevied(str, z);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        SoftApStateMachine(Looper looper, WifiInjector wifiInjector) {
            super(SoftApManager.TAG, looper);
            this.mWifiInjector = wifiInjector;
            this.mInterfaceEventHandler = new InterfaceEventHandler(this);
            addState(this.mIdleState);
            addState(this.mStartedState);
            setInitialState(this.mIdleState);
            start();
        }
    }

    public SoftApManager(Looper looper, WifiNative wifiNative, String countryCode, Listener listener, IApInterface apInterface, INetworkManagementService nms, WifiApConfigStore wifiApConfigStore, WifiConfiguration config, WifiMetrics wifiMetrics, WifiInjector wifiInjector, Context context) {
        this.mStateMachine = new SoftApStateMachine(looper, wifiInjector);
        this.mWifiNative = wifiNative;
        this.mCountryCode = countryCode;
        this.mListener = listener;
        this.mApInterface = apInterface;
        this.mNwService = nms;
        this.mWifiApConfigStore = wifiApConfigStore;
        if (config == null) {
            this.mApConfig = this.mWifiApConfigStore.getApConfiguration();
        } else {
            this.mApConfig = config;
        }
        this.mWifiMetrics = wifiMetrics;
        this.mContext = context;
    }

    public void start() {
        this.mStateMachine.sendMessage(0, this.mApConfig);
    }

    public void stop() {
        this.mStateMachine.sendMessage(1);
    }

    private void updateApState(int state, int reason) {
        if (this.mListener != null) {
            this.mListener.onStateChanged(state, reason);
        }
    }

    public void setDualSapMode(boolean enable) {
        this.mDualSapMode = enable;
    }

    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) this.mContext.getSystemService("connectivity");
    }

    private void sendTetherConnectStateChangedBroadcast() {
        if (getConnectivityManager().isTetheringSupported()) {
            Intent broadcast = new Intent("codeaurora.net.conn.TETHER_CONNECT_STATE_CHANGED");
            broadcast.addFlags(603979776);
            this.mContext.sendStickyBroadcastAsUser(broadcast, UserHandle.ALL);
            showSoftApClientsNotification(17303494);
        }
    }

    private boolean readDeviceInfoFromDnsmasq(String mac_address) {
        IOException ex;
        Throwable th;
        boolean result = false;
        FileInputStream fstream = null;
        try {
            FileInputStream fstream2 = new FileInputStream(dhcpLocation);
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(fstream2)));
                while (true) {
                    String line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    String[] fields = line.split(" ");
                    if (fields.length > 3) {
                        String addr = fields[1];
                        String name = fields[3];
                        if (addr.equals(mac_address)) {
                            result = true;
                            break;
                        }
                    }
                }
                if (fstream2 != null) {
                    try {
                        fstream2.close();
                    } catch (IOException e) {
                    }
                }
                fstream = fstream2;
            } catch (IOException e2) {
                ex = e2;
                fstream = fstream2;
                try {
                    Log.e(TAG, "readDeviceNameFromDnsmasq: " + ex);
                    if (fstream != null) {
                        try {
                            fstream.close();
                        } catch (IOException e3) {
                        }
                    }
                    return result;
                } catch (Throwable th2) {
                    th = th2;
                    if (fstream != null) {
                        try {
                            fstream.close();
                        } catch (IOException e4) {
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                fstream = fstream2;
                if (fstream != null) {
                    try {
                        fstream.close();
                    } catch (IOException e42) {
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
            ex = e5;
            Log.e(TAG, "readDeviceNameFromDnsmasq: " + ex);
            if (fstream != null) {
                try {
                    fstream.close();
                } catch (IOException e32) {
                }
            }
            return result;
        }
        return result;
    }

    public void interfaceMessageRecevied(String mac_address, boolean connect_status) {
        if (this.mContext.getResources().getBoolean(17957022)) {
            if (connect_status) {
                try {
                    this.mL2ConnectedDeviceMap.put(mac_address, Boolean.valueOf(connect_status));
                    this.mConnectedDeviceMap.put(mac_address, Boolean.valueOf(connect_status));
                    sendTetherConnectStateChangedBroadcast();
                    if (readDeviceInfoFromDnsmasq(mac_address)) {
                        this.mConnectedDeviceMap.put(mac_address, Boolean.valueOf(connect_status));
                        sendTetherConnectStateChangedBroadcast();
                    } else {
                        Log.d(TAG, "Starting poll device info for " + mac_address);
                        new DnsmasqThread(this, mac_address, DNSMASQ_POLLING_INTERVAL, 10, connect_status).start();
                    }
                } catch (IllegalArgumentException ex) {
                    Log.e(TAG, "Device IllegalArgument: " + ex);
                }
            } else {
                this.mL2ConnectedDeviceMap.remove(mac_address);
                this.mConnectedDeviceMap.remove(mac_address);
                sendTetherConnectStateChangedBroadcast();
            }
        }
    }

    private void showSoftApClientsNotification(int icon) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager != null) {
            CharSequence message;
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
            intent.setFlags(1073741824);
            PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
            Resources r = Resources.getSystem();
            CharSequence title = r.getText(17040959);
            int size = this.mConnectedDeviceMap.size();
            if (size == 0) {
                message = r.getText(17040957);
            } else if (size == 1) {
                message = String.format(r.getText(17040958).toString(), new Object[]{Integer.valueOf(size)});
            } else {
                message = String.format(r.getText(17040956).toString(), new Object[]{Integer.valueOf(size)});
            }
            if (this.softApNotificationBuilder == null) {
                this.softApNotificationBuilder = new Builder(this.mContext, SystemNotificationChannels.ALERTS);
                this.softApNotificationBuilder.setWhen(0).setOngoing(true).setColor(this.mContext.getColor(17170763)).setVisibility(1).setCategory("status");
            }
            this.softApNotificationBuilder.setSmallIcon(icon).setContentTitle(title).setContentText(message).setContentIntent(pi).setPriority(-2);
            this.softApNotificationBuilder.setContentText(message);
            this.mLastSoftApNotificationId = icon + 10;
            notificationManager.notify(this.mLastSoftApNotificationId, this.softApNotificationBuilder.build());
        }
    }

    private void clearSoftApClientsNotification() {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager != null && this.mLastSoftApNotificationId != 0) {
            notificationManager.cancel(this.mLastSoftApNotificationId);
            this.mLastSoftApNotificationId = 0;
        }
    }

    public void setSapChannel(int channel) {
        this.mSoftApChannel = channel;
    }

    private boolean writeDualHostapdConfig(WifiConfiguration wifiConfig) {
        String[] dualApInterfaces = this.mWifiApConfigStore.getDualSapInterfaces();
        if (dualApInterfaces == null || dualApInterfaces.length != 2) {
            Log.e(TAG, " dualApInterfaces is not set or length is not 2");
            return false;
        }
        String authStr;
        String hexSsid = String.format("%x", new Object[]{new BigInteger(1, wifiConfig.SSID.getBytes(StandardCharsets.UTF_8))});
        switch (wifiConfig.getAuthType()) {
            case 1:
                authStr = "wpa-psk " + wifiConfig.preSharedKey;
                break;
            case 4:
                authStr = "wpa2-psk " + wifiConfig.preSharedKey;
                break;
            default:
                authStr = "open";
                break;
        }
        try {
            return this.mWifiNative.runQsapCmd(new StringBuilder().append("softap setsoftap dual2g ").append(dualApInterfaces[0]).append(" ").append(hexSsid).append(" visible 0 ").append(authStr).toString(), "") && this.mWifiNative.runQsapCmd("softap setsoftap dual5g " + dualApInterfaces[1] + " " + hexSsid + " visible 0 " + authStr, "") && this.mWifiNative.runQsapCmd("softap qccmd set dual2g hw_mode=", "g") && this.mWifiNative.runQsapCmd("softap qccmd set dual5g hw_mode=", "a") && this.mWifiNative.runQsapCmd("softap qccmd set dual2g bridge=", this.mApInterface.getInterfaceName()) && this.mWifiNative.runQsapCmd("softap qccmd set dual5g bridge=", this.mApInterface.getInterfaceName());
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in configuring softap for dual mode: " + e);
        }
    }

    private int startDualSoftAp(WifiConfiguration config) {
        WifiConfiguration localConfig = new WifiConfiguration(config);
        if (this.mCountryCode == null || this.mWifiNative.setCountryCodeHal(this.mCountryCode.toUpperCase(Locale.ROOT))) {
            try {
                if (writeDualHostapdConfig(localConfig)) {
                    boolean success = this.mApInterface.startHostapd(this.mDualSapMode);
                    this.mWifiNative.runQsapCmd("softap bridge up ", this.mApInterface.getInterfaceName());
                    if (!success) {
                        Log.e(TAG, "Failed to start hostapd.");
                        return 2;
                    }
                    Log.d(TAG, "Dual Soft AP is started");
                    return 0;
                }
                Log.e(TAG, "Failed to write dual hostapd configuration");
                return 2;
            } catch (RemoteException e) {
                Log.e(TAG, "Exception in starting dual soft AP: " + e);
            }
        } else {
            Log.e(TAG, "Failed to set country code, required for setting up soft ap in 5GHz");
            return 2;
        }
    }

    private int startSoftAp(WifiConfiguration config) {
        if (config == null || config.SSID == null) {
            Log.e(TAG, "Unable to start soft AP without valid configuration");
            return 2;
        } else if (this.mDualSapMode) {
            return startDualSoftAp(config);
        } else {
            WifiConfiguration localConfig = new WifiConfiguration(config);
            int result = ApConfigUtil.updateApChannelConfig(this.mWifiNative, this.mCountryCode, this.mWifiApConfigStore.getAllowed2GChannel(), localConfig);
            if (result != 0) {
                Log.e(TAG, "Failed to update AP band and channel");
                return result;
            } else if (this.mCountryCode == null || this.mWifiNative.setCountryCodeHal(this.mCountryCode.toUpperCase(Locale.ROOT)) || config.apBand != 1) {
                int encryptionType = getIApInterfaceEncryptionType(localConfig);
                if (localConfig.hiddenSSID) {
                    Log.d(TAG, "SoftAP is a hidden network");
                }
                try {
                    byte[] bytes;
                    if (!(localConfig.apBand == 1 || this.mSoftApChannel == 0)) {
                        localConfig.apBand = 0;
                        localConfig.apChannel = this.mSoftApChannel;
                    }
                    IApInterface iApInterface = this.mApInterface;
                    byte[] bytes2 = localConfig.SSID.getBytes(StandardCharsets.UTF_8);
                    boolean z = localConfig.hiddenSSID;
                    int i = localConfig.apChannel;
                    if (localConfig.preSharedKey != null) {
                        bytes = localConfig.preSharedKey.getBytes(StandardCharsets.UTF_8);
                    } else {
                        bytes = new byte[0];
                    }
                    if (iApInterface.writeHostapdConfig(bytes2, z, i, encryptionType, bytes)) {
                        if (!this.mApInterface.startHostapd(false)) {
                            Log.e(TAG, "Failed to start hostapd.");
                            return 2;
                        }
                        Log.d(TAG, "Soft AP is started");
                        return 0;
                    }
                    Log.e(TAG, "Failed to write hostapd configuration");
                    return 2;
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception in starting soft AP: " + e);
                }
            } else {
                Log.e(TAG, "Failed to set country code, required for setting up soft ap in 5GHz");
                return 2;
            }
        }
    }

    private static int getIApInterfaceEncryptionType(WifiConfiguration localConfig) {
        switch (localConfig.getAuthType()) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 4:
                return 2;
            default:
                return 0;
        }
    }

    private void stopSoftAp() {
        try {
            this.mApInterface.stopHostapd(this.mDualSapMode);
            Log.d(TAG, "Soft AP is stopped");
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in stopping soft AP: " + e);
        }
    }
}
