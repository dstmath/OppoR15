package com.android.server.wifi.aware;

import android.content.Context;
import android.util.Log;
import com.android.server.SystemService;
import com.android.server.wifi.HalDeviceManager;
import com.android.server.wifi.WifiInjector;

public final class WifiAwareService extends SystemService {
    private static final String TAG = "WifiAwareService";
    final WifiAwareServiceImpl mImpl;

    public WifiAwareService(Context context) {
        super(context);
        this.mImpl = new WifiAwareServiceImpl(context);
    }

    public void onStart() {
        Log.i(TAG, "Registering wifiaware");
        publishBinderService("wifiaware", this.mImpl);
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            WifiInjector wifiInjector = WifiInjector.getInstance();
            if (wifiInjector == null) {
                Log.e(TAG, "onBootPhase(PHASE_SYSTEM_SERVICES_READY): NULL injector!");
                return;
            }
            HalDeviceManager halDeviceManager = wifiInjector.getHalDeviceManager();
            WifiAwareStateManager wifiAwareStateManager = new WifiAwareStateManager();
            WifiAwareNativeCallback wifiAwareNativeCallback = new WifiAwareNativeCallback(wifiAwareStateManager);
            WifiAwareNativeManager wifiAwareNativeManager = new WifiAwareNativeManager(wifiAwareStateManager, halDeviceManager, wifiAwareNativeCallback);
            WifiAwareNativeApi wifiAwareNativeApi = new WifiAwareNativeApi(wifiAwareNativeManager);
            wifiAwareStateManager.setNative(wifiAwareNativeManager, wifiAwareNativeApi);
            WifiAwareShellCommand wifiAwareShellCommand = new WifiAwareShellCommand();
            wifiAwareShellCommand.register("native_api", wifiAwareNativeApi);
            wifiAwareShellCommand.register("native_cb", wifiAwareNativeCallback);
            wifiAwareShellCommand.register("state_mgr", wifiAwareStateManager);
            this.mImpl.start(wifiInjector.getWifiAwareHandlerThread(), wifiAwareStateManager, wifiAwareShellCommand, wifiInjector.getWifiMetrics().getWifiAwareMetrics(), wifiInjector.getWifiPermissionsWrapper());
        } else if (phase == 1000) {
            this.mImpl.startLate();
        }
    }
}
