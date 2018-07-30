package com.mediatek.server.wifi;

import android.os.SystemProperties;

public class WifiApInjector {
    static WifiApInjector sWifiApInjector = null;
    private final HostapdIfaceHal mHostapdIfaceHal = new HostapdIfaceHal(this.mWifiApMonitor);
    private final WifiApMonitor mWifiApMonitor = new WifiApMonitor(this);
    private final WifiApNative mWifiApNative = new WifiApNative(SystemProperties.get("wifi.tethering.interface", "softap0"), this.mHostapdIfaceHal);

    public WifiApInjector() {
        sWifiApInjector = this;
    }

    public static WifiApInjector getInstance() {
        return sWifiApInjector;
    }

    public WifiApNative getWifiApNative() {
        return this.mWifiApNative;
    }

    public WifiApMonitor getWifiApMonitor() {
        return this.mWifiApMonitor;
    }
}
