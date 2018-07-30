package com.android.server.wifi.scanner;

import android.content.Context;
import android.net.wifi.WifiScanner.ScanData;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiNative.PnoEventHandler;
import com.android.server.wifi.WifiNative.PnoSettings;
import com.android.server.wifi.WifiNative.ScanCapabilities;
import com.android.server.wifi.WifiNative.ScanEventHandler;
import com.android.server.wifi.WifiNative.ScanSettings;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class HalWifiScannerImpl extends WifiScannerImpl implements Callback {
    private static final boolean DBG = false;
    private static final String TAG = "HalWifiScannerImpl";
    private final ChannelHelper mChannelHelper;
    private final WifiNative mWifiNative;
    private final WificondScannerImpl mWificondScannerDelegate;

    public HalWifiScannerImpl(Context context, WifiNative wifiNative, WifiMonitor wifiMonitor, Looper looper, Clock clock) {
        this.mWifiNative = wifiNative;
        this.mChannelHelper = new HalChannelHelper(wifiNative);
        this.mWificondScannerDelegate = new WificondScannerImpl(context, wifiNative, wifiMonitor, this.mChannelHelper, looper, clock);
    }

    public boolean handleMessage(Message msg) {
        Log.w(TAG, "Unknown message received: " + msg.what);
        return true;
    }

    public void cleanup() {
        this.mWificondScannerDelegate.cleanup();
    }

    public boolean getScanCapabilities(ScanCapabilities capabilities) {
        return this.mWifiNative.getBgScanCapabilities(capabilities);
    }

    public ChannelHelper getChannelHelper() {
        return this.mChannelHelper;
    }

    public boolean startSingleScan(ScanSettings settings, ScanEventHandler eventHandler) {
        return this.mWificondScannerDelegate.startSingleScan(settings, eventHandler);
    }

    public ScanData getLatestSingleScanResults() {
        return this.mWificondScannerDelegate.getLatestSingleScanResults();
    }

    public boolean startBatchedScan(ScanSettings settings, ScanEventHandler eventHandler) {
        if (settings != null && eventHandler != null) {
            return this.mWifiNative.startBgScan(settings, eventHandler);
        }
        Log.w(TAG, "Invalid arguments for startBatched: settings=" + settings + ",eventHandler=" + eventHandler);
        return false;
    }

    public void stopBatchedScan() {
        this.mWifiNative.stopBgScan();
    }

    public void pauseBatchedScan() {
        this.mWifiNative.pauseBgScan();
    }

    public void restartBatchedScan() {
        this.mWifiNative.restartBgScan();
    }

    public ScanData[] getLatestBatchedScanResults(boolean flush) {
        return this.mWifiNative.getBgScanResults();
    }

    public boolean setHwPnoList(PnoSettings settings, PnoEventHandler eventHandler) {
        return this.mWificondScannerDelegate.setHwPnoList(settings, eventHandler);
    }

    public boolean resetHwPnoList() {
        return this.mWificondScannerDelegate.resetHwPnoList();
    }

    public boolean isHwPnoSupported(boolean isConnectedPno) {
        return this.mWificondScannerDelegate.isHwPnoSupported(isConnectedPno);
    }

    public boolean shouldScheduleBackgroundScanForHwPno() {
        return this.mWificondScannerDelegate.shouldScheduleBackgroundScanForHwPno();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mWificondScannerDelegate.dump(fd, pw, args);
    }
}
