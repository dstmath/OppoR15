package com.mediatek.server.wifi;

import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.os.IHwBinder.DeathRecipient;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.util.NativeUtil;
import com.mediatek.server.wifi.-$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698.AnonymousClass2;
import com.mediatek.server.wifi.-$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698.AnonymousClass3;
import java.util.ArrayList;
import java.util.Iterator;
import javax.annotation.concurrent.ThreadSafe;
import vendor.oppo.hardware.wifi.hostapd.V1_0.HostapdStatus;
import vendor.oppo.hardware.wifi.hostapd.V1_0.IHostapd;
import vendor.oppo.hardware.wifi.hostapd.V1_0.IHostapdIface;
import vendor.oppo.hardware.wifi.hostapd.V1_0.IHostapdIfaceCallback;

@ThreadSafe
public class HostapdIfaceHal {
    private static final String TAG = "HostapdIfaceHal";
    private final DeathRecipient mHostapdDeathRecipient = new -$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698((byte) 1, this);
    private IHostapd mIHostapd;
    private IHostapdIface mIHostapdIface;
    private IHostapdIfaceCallback mIHostapdIfaceCallback;
    private IServiceManager mIServiceManager = null;
    private String mIfaceName;
    private final Object mLock = new Object();
    private final DeathRecipient mServiceManagerDeathRecipient = new -$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698((byte) 0, this);
    private final IServiceNotification mServiceNotificationCallback = new Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            synchronized (HostapdIfaceHal.this.mLock) {
                Log.d(HostapdIfaceHal.TAG, "IServiceNotification.onRegistration for: " + fqName + ", " + name + " preexisting=" + preexisting);
                if (HostapdIfaceHal.this.initHostapdService() && (HostapdIfaceHal.this.initHostapdIface() ^ 1) == 0) {
                    Log.i(HostapdIfaceHal.TAG, "Completed initialization of IHostapd interfaces.");
                } else {
                    Log.e(HostapdIfaceHal.TAG, "initalizing IHostapdIfaces failed.");
                    HostapdIfaceHal.this.hostapdServiceDiedHandler();
                }
            }
        }
    };
    private boolean mVerboseLoggingEnabled = true;
    private final WifiApMonitor mWifiApMonitor;

    private class HostapdIfaceHalCallback extends IHostapdIfaceCallback.Stub {
        /* synthetic */ HostapdIfaceHalCallback(HostapdIfaceHal this$0, HostapdIfaceHalCallback -this1) {
            this();
        }

        private HostapdIfaceHalCallback() {
        }

        public void onWpsEventPbcOverlap() {
            synchronized (HostapdIfaceHal.this.mLock) {
                HostapdIfaceHal.this.logCallback("onWpsEventPbcOverlap");
                HostapdIfaceHal.this.mWifiApMonitor.broadcastWpsOverlapEvent(HostapdIfaceHal.this.mIfaceName);
            }
        }

        public void onStaAuthorized(byte[] staAddress) {
            HostapdIfaceHal.logd("STA authorized on " + HostapdIfaceHal.this.mIfaceName);
            try {
                HostapdIfaceHal.this.mWifiApMonitor.broadcastApStaConnected(HostapdIfaceHal.this.mIfaceName, NativeUtil.macAddressFromByteArray(staAddress));
            } catch (Exception e) {
                Log.e(HostapdIfaceHal.TAG, "Could not decode MAC address.", e);
            }
        }

        public void onStaDeauthorized(byte[] staAddress) {
            HostapdIfaceHal.logd("STA deauthorized on " + HostapdIfaceHal.this.mIfaceName);
            try {
                HostapdIfaceHal.this.mWifiApMonitor.broadcastApStaDisconnected(HostapdIfaceHal.this.mIfaceName, NativeUtil.macAddressFromByteArray(staAddress));
            } catch (Exception e) {
                Log.e(HostapdIfaceHal.TAG, "Could not decode MAC address.", e);
            }
        }
    }

    private static class Mutable<E> {
        public E value;

        Mutable() {
            this.value = null;
        }

        Mutable(E value) {
            this.value = value;
        }
    }

    /* synthetic */ void lambda$-com_mediatek_server_wifi_HostapdIfaceHal_2856(long cookie) {
        synchronized (this.mLock) {
            Log.w(TAG, "IServiceManager died: cookie=" + cookie);
            hostapdServiceDiedHandler();
            this.mIServiceManager = null;
        }
    }

    /* synthetic */ void lambda$-com_mediatek_server_wifi_HostapdIfaceHal_3244(long cookie) {
        synchronized (this.mLock) {
            Log.w(TAG, "IHostapd/IHostapdStaIface died: cookie=" + cookie);
            hostapdServiceDiedHandler();
        }
    }

    public HostapdIfaceHal(WifiApMonitor monitor) {
        this.mWifiApMonitor = monitor;
        this.mIHostapdIfaceCallback = new HostapdIfaceHalCallback();
    }

    void enableVerboseLogging(boolean enable) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = enable;
        }
    }

    private boolean linkToServiceManagerDeath() {
        synchronized (this.mLock) {
            if (this.mIServiceManager == null) {
                return false;
            }
            try {
                if (this.mIServiceManager.linkToDeath(this.mServiceManagerDeathRecipient, 0)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on IServiceManager");
                hostapdServiceDiedHandler();
                this.mIServiceManager = null;
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "IServiceManager.linkToDeath exception", e);
                return false;
            }
        }
    }

    public boolean initialize() {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.i(TAG, "Registering IHostapd service ready callback.");
            }
            this.mIHostapd = null;
            this.mIHostapdIface = null;
            if (this.mIServiceManager != null) {
                return true;
            }
            try {
                this.mIServiceManager = getServiceManagerMockable();
                if (this.mIServiceManager == null) {
                    Log.e(TAG, "Failed to get HIDL Service Manager");
                    return false;
                } else if (linkToServiceManagerDeath()) {
                    Log.d(TAG, "registerForNotifications kInterfaceName=vendor.oppo.hardware.wifi.hostapd@1.0::IHostapd, callback=" + this.mServiceNotificationCallback);
                    if (!this.mIServiceManager.registerForNotifications(IHostapd.kInterfaceName, "", this.mServiceNotificationCallback)) {
                        Log.e(TAG, "Failed to register for notifications to vendor.oppo.hardware.wifi.hostapd@1.0::IHostapd");
                        this.mIServiceManager = null;
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while trying to register a listener for IHostapd service: " + e);
                hostapdServiceDiedHandler();
            }
        }
        return true;
    }

    private boolean linkToHostapdDeath() {
        synchronized (this.mLock) {
            if (this.mIHostapd == null) {
                return false;
            }
            try {
                if (this.mIHostapd.linkToDeath(this.mHostapdDeathRecipient, 0)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on IHostapd");
                hostapdServiceDiedHandler();
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "IHostapd.linkToDeath exception", e);
                return false;
            }
        }
    }

    private boolean initHostapdService() {
        synchronized (this.mLock) {
            try {
                this.mIHostapd = getHostapdMockable();
                if (this.mIHostapd == null) {
                    Log.e(TAG, "Got null IHostapd service. Stopping hostapd HIDL startup");
                    return false;
                } else if (linkToHostapdDeath()) {
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "IHostapd.getService exception: " + e);
                return false;
            }
        }
    }

    private boolean linkToHostapdIfaceDeath() {
        synchronized (this.mLock) {
            if (this.mIHostapdIface == null) {
                return false;
            }
            try {
                if (this.mIHostapdIface.linkToDeath(this.mHostapdDeathRecipient, 0)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on IHostapdIface");
                hostapdServiceDiedHandler();
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "IHostapdIface.linkToDeath exception", e);
                return false;
            }
        }
    }

    private boolean initHostapdIface() {
        synchronized (this.mLock) {
            ArrayList<String> hostapdIfaces = new ArrayList();
            try {
                this.mIHostapd.listInterfaces(new AnonymousClass2(hostapdIfaces));
                if (hostapdIfaces.size() == 0) {
                    Log.e(TAG, "Got zero HIDL hostapd ifaces. Stopping hostapd HIDL startup.");
                    return false;
                }
                Mutable<IHostapdIface> hostapdIface = new Mutable();
                Mutable<String> ifaceName = new Mutable();
                Iterator ifaceInfo$iterator = hostapdIfaces.iterator();
                if (ifaceInfo$iterator.hasNext()) {
                    String ifaceInfo = (String) ifaceInfo$iterator.next();
                    try {
                        this.mIHostapd.getInterface(ifaceInfo, new com.mediatek.server.wifi.-$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698.AnonymousClass1(hostapdIface));
                        ifaceName.value = ifaceInfo;
                    } catch (RemoteException e) {
                        Log.e(TAG, "IHostapd.getInterface exception: " + e);
                        return false;
                    }
                }
                if (hostapdIface.value == null) {
                    Log.e(TAG, "initHostapdIface got null iface");
                    return false;
                }
                this.mIHostapdIface = getApIfaceMockable((IHostapdIface) hostapdIface.value);
                this.mIfaceName = (String) ifaceName.value;
                if (!linkToHostapdIfaceDeath()) {
                    return false;
                } else if (registerCallback(this.mIHostapdIfaceCallback)) {
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e2) {
                Log.e(TAG, "IHostapd.listInterfaces exception: " + e2);
                return false;
            }
        }
    }

    static /* synthetic */ void lambda$-com_mediatek_server_wifi_HostapdIfaceHal_8681(ArrayList hostapdIfaces, HostapdStatus status, ArrayList ifaces) {
        if (status.code != 0) {
            Log.e(TAG, "Getting Hostapd Interfaces failed: " + status.code);
        } else {
            hostapdIfaces.addAll(ifaces);
        }
    }

    static /* synthetic */ void lambda$-com_mediatek_server_wifi_HostapdIfaceHal_9659(Mutable hostapdIface, HostapdStatus status, IHostapdIface iface) {
        if (status.code != 0) {
            Log.e(TAG, "Failed to get IHostapdIface " + status.code);
        } else {
            hostapdIface.value = iface;
        }
    }

    private void hostapdServiceDiedHandler() {
        synchronized (this.mLock) {
            this.mIHostapd = null;
            this.mIHostapdIface = null;
            this.mWifiApMonitor.broadcastSupplicantDisconnectionEvent(this.mIfaceName);
        }
    }

    public boolean isInitializationStarted() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIServiceManager != null;
        }
        return z;
    }

    public boolean isInitializationComplete() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIHostapdIface != null;
        }
        return z;
    }

    protected IServiceManager getServiceManagerMockable() throws RemoteException {
        IServiceManager service;
        synchronized (this.mLock) {
            service = IServiceManager.getService();
        }
        return service;
    }

    protected IHostapd getHostapdMockable() throws RemoteException {
        IHostapd service;
        synchronized (this.mLock) {
            service = IHostapd.getService();
        }
        return service;
    }

    protected IHostapdIface getApIfaceMockable(IHostapdIface iface) {
        IHostapdIface asInterface;
        synchronized (this.mLock) {
            asInterface = IHostapdIface.asInterface(iface.asBinder());
        }
        return asInterface;
    }

    private boolean registerCallback(IHostapdIfaceCallback callback) {
        synchronized (this.mLock) {
            String methodStr = "registerCallback";
            if (checkHostapdIfaceAndLogFailure("registerCallback")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapdIface.registerCallback(callback), "registerCallback");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "registerCallback");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean startWpsPbc() {
        synchronized (this.mLock) {
            String methodStr = "startWpsPbc";
            if (checkHostapdIfaceAndLogFailure("startWpsPbc")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapdIface.startWpsPbc(), "startWpsPbc");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "startWpsPbc");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean startWpsPinKeypad(String pin) {
        if (TextUtils.isEmpty(pin)) {
            return false;
        }
        synchronized (this.mLock) {
            String methodStr = "startWpsPinKeypad";
            if (checkHostapdIfaceAndLogFailure("startWpsPinKeypad")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapdIface.startWpsPinKeypad(pin), "startWpsPinKeypad");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "startWpsPinKeypad");
                    return false;
                }
            }
            return false;
        }
    }

    public String startWpsCheckPin(String pin) {
        if (TextUtils.isEmpty(pin)) {
            return null;
        }
        synchronized (this.mLock) {
            String methodStr = "startWpsCheckPin";
            if (checkHostapdIfaceAndLogFailure("startWpsCheckPin")) {
                Mutable<String> gotPin = new Mutable();
                try {
                    this.mIHostapdIface.startWpsCheckPin(pin, new AnonymousClass3(this, gotPin));
                } catch (RemoteException e) {
                    handleRemoteException(e, "startWpsCheckPin");
                }
                String str = (String) gotPin.value;
                return str;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_mediatek_server_wifi_HostapdIfaceHal_14681(Mutable gotPin, HostapdStatus status, String validPin) {
        if (checkStatusAndLogFailure(status, "startWpsCheckPin")) {
            gotPin.value = validPin;
        }
    }

    public boolean setMaxClient(int maxNum) {
        Log.i(TAG, "setMaxClient " + maxNum);
        synchronized (this.mLock) {
            String methodStr = "setMaxClient";
            if (checkHostapdIfaceAndLogFailure("setMaxClient")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapdIface.setMaxClient(maxNum), "setMaxClient");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setMaxClient");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean blockClient(String deviceAddress) {
        Log.i(TAG, "blockClient " + deviceAddress);
        if (TextUtils.isEmpty(deviceAddress)) {
            return false;
        }
        synchronized (this.mLock) {
            String methodStr = "blockClient";
            if (checkHostapdIfaceAndLogFailure("blockClient")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapdIface.blockClient(deviceAddress), "blockClient");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "blockClient");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean unblockClient(String deviceAddress) {
        if (TextUtils.isEmpty(deviceAddress)) {
            return false;
        }
        synchronized (this.mLock) {
            String methodStr = "unblockClient";
            if (checkHostapdIfaceAndLogFailure("unblockClient")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapdIface.unblockClient(deviceAddress), "unblockClient");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "unblockClient");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean updateAllowedList(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        synchronized (this.mLock) {
            String methodStr = "updateAllowedList";
            if (checkHostapdIfaceAndLogFailure("updateAllowedList")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapdIface.updateAllowedList(filePath), "updateAllowedList");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "updateAllowedList");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setAllDevicesAllowed(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setAllDevicesAllowed";
            if (checkHostapdIfaceAndLogFailure("setAllDevicesAllowed")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapdIface.setAllDevicesAllowed(enable), "setAllDevicesAllowed");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setAllDevicesAllowed");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean checkHostapdIfaceAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            if (this.mIHostapdIface == null) {
                Log.e(TAG, "Can't call " + methodStr + ", IHostapdIface is null");
                return false;
            }
            return true;
        }
    }

    private boolean checkStatusAndLogFailure(HostapdStatus status, String methodStr) {
        synchronized (this.mLock) {
            if (status.code != 0) {
                Log.e(TAG, "IHostapdIface." + methodStr + " failed: " + hostapdStatusCodeToString(status.code) + ", " + status.debugMessage);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "IHostapdIface." + methodStr + " succeeded");
            }
            return true;
        }
    }

    private void logCallback(String methodStr) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "IHostapdIfaceCallback." + methodStr + " received");
            }
        }
    }

    private void handleRemoteException(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            hostapdServiceDiedHandler();
            Log.e(TAG, "IHostapdIfaceIface." + methodStr + " failed with exception", e);
        }
    }

    public static String hostapdStatusCodeToString(int code) {
        switch (code) {
            case 0:
                return "SUCCESS";
            case 1:
                return "FAILURE_UNKNOWN";
            case 2:
                return "FAILURE_ARGS_INVALID";
            case 3:
                return "FAILURE_IFACE_INVALID";
            case 4:
                return "FAILURE_IFACE_UNKNOWN";
            case 5:
                return "FAILURE_IFACE_EXISTS";
            case 6:
                return "FAILURE_IFACE_DISABLED";
            case 7:
                return "FAILURE_IFACE_NOT_DISCONNECTED";
            case 8:
                return "NOT_SUPPORTED";
            default:
                return "??? UNKNOWN_CODE";
        }
    }

    private static void logd(String s) {
        Log.d(TAG, s);
    }

    private static void logi(String s) {
        Log.i(TAG, s);
    }

    private static void loge(String s) {
        Log.e(TAG, s);
    }
}
