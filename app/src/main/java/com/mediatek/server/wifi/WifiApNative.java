package com.mediatek.server.wifi;

public class WifiApNative {
    private final HostapdIfaceHal mHostapdIfaceHal;
    private final String mInterfaceName;
    private final String mTAG;

    public WifiApNative(String interfaceName, HostapdIfaceHal apIfaceHal) {
        this.mTAG = "WifiApNative-" + interfaceName;
        this.mInterfaceName = interfaceName;
        this.mHostapdIfaceHal = apIfaceHal;
    }

    public String getInterfaceName() {
        return this.mInterfaceName;
    }

    public void enableVerboseLogging(int verbose) {
        boolean z = false;
        HostapdIfaceHal hostapdIfaceHal = this.mHostapdIfaceHal;
        if (verbose > 0) {
            z = true;
        }
        hostapdIfaceHal.enableVerboseLogging(z);
    }

    public boolean connectToHostapd() {
        if (this.mHostapdIfaceHal.isInitializationStarted() || (this.mHostapdIfaceHal.initialize() ^ 1) == 0) {
            return this.mHostapdIfaceHal.isInitializationComplete();
        }
        return false;
    }

    public void closeHostapdConnection() {
    }

    public boolean startApWpsPbcCommand() {
        return this.mHostapdIfaceHal.startWpsPbc();
    }

    public boolean startApWpsWithPinFromDeviceCommand(String pin) {
        return this.mHostapdIfaceHal.startWpsPinKeypad(pin);
    }

    public String startApWpsCheckPinCommand(String pin) {
        return this.mHostapdIfaceHal.startWpsCheckPin(pin);
    }

    public boolean blockClientCommand(String deviceAddress) {
        return this.mHostapdIfaceHal.blockClient(deviceAddress);
    }

    public boolean setMaxClientCommand(int maxNum) {
        return this.mHostapdIfaceHal.setMaxClient(maxNum);
    }

    public boolean unblockClientCommand(String deviceAddress) {
        return this.mHostapdIfaceHal.unblockClient(deviceAddress);
    }

    public boolean updateAllowedListCommand(String filePath) {
        return this.mHostapdIfaceHal.updateAllowedList(filePath);
    }

    public boolean setAllDevicesAllowedCommand(boolean enable) {
        return this.mHostapdIfaceHal.setAllDevicesAllowed(enable);
    }
}
