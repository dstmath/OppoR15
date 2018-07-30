package com.mediatek.server.wifi;

import android.os.Handler;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WifiApMonitor {
    public static final int AP_STA_CONNECTED_EVENT = 147498;
    public static final int AP_STA_DISCONNECTED_EVENT = 147497;
    private static final int BASE = 147456;
    public static final int SUP_CONNECTION_EVENT = 147457;
    public static final int SUP_DISCONNECTION_EVENT = 147458;
    private static final String TAG = "WifiApMonitor";
    public static final int WPS_OVERLAP_EVENT = 147466;
    private boolean mConnected = false;
    private final Map<String, SparseArray<Set<Handler>>> mHandlerMap = new HashMap();
    private final Map<String, Boolean> mMonitoringMap = new HashMap();
    private boolean mVerboseLoggingEnabled = true;
    private final WifiApInjector mWifiApInjector;

    public WifiApMonitor(WifiApInjector wifiApInjector) {
        this.mWifiApInjector = wifiApInjector;
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    public synchronized void registerHandler(String iface, int what, Handler handler) {
        SparseArray<Set<Handler>> ifaceHandlers = (SparseArray) this.mHandlerMap.get(iface);
        if (ifaceHandlers == null) {
            ifaceHandlers = new SparseArray();
            this.mHandlerMap.put(iface, ifaceHandlers);
        }
        Set<Handler> ifaceWhatHandlers = (Set) ifaceHandlers.get(what);
        if (ifaceWhatHandlers == null) {
            ifaceWhatHandlers = new ArraySet();
            ifaceHandlers.put(what, ifaceWhatHandlers);
        }
        ifaceWhatHandlers.add(handler);
    }

    public synchronized void unregisterAllHandler() {
        this.mHandlerMap.clear();
    }

    private boolean isMonitoring(String iface) {
        Boolean val = (Boolean) this.mMonitoringMap.get(iface);
        if (val == null) {
            return false;
        }
        return val.booleanValue();
    }

    public void setMonitoring(String iface, boolean enabled) {
        this.mMonitoringMap.put(iface, Boolean.valueOf(enabled));
    }

    private void setMonitoringNone() {
        for (String iface : this.mMonitoringMap.keySet()) {
            setMonitoring(iface, false);
        }
    }

    private boolean ensureConnectedLocked() {
        if (this.mConnected) {
            return true;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "connecting to hostapd");
        }
        int connectTries = 0;
        while (true) {
            this.mConnected = this.mWifiApInjector.getWifiApNative().connectToHostapd();
            if (this.mConnected) {
                return true;
            }
            int connectTries2 = connectTries + 1;
            if (connectTries >= 5) {
                return false;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            connectTries = connectTries2;
        }
    }

    public synchronized void startMonitoring(String iface) {
        if (ensureConnectedLocked()) {
            setMonitoring(iface, true);
            broadcastSupplicantConnectionEvent(iface);
        } else {
            boolean originalMonitoring = isMonitoring(iface);
            setMonitoring(iface, true);
            broadcastSupplicantDisconnectionEvent(iface);
            setMonitoring(iface, originalMonitoring);
            Log.e(TAG, "startMonitoring(" + iface + ") failed!");
        }
    }

    public synchronized void stopMonitoring(String iface) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "stopMonitoring(" + iface + ")");
        }
        setMonitoring(iface, true);
        broadcastSupplicantDisconnectionEvent(iface);
        setMonitoring(iface, false);
    }

    public synchronized void stopAllMonitoring() {
        this.mConnected = false;
        setMonitoringNone();
    }

    private void sendMessage(String iface, int what) {
        sendMessage(iface, Message.obtain(null, what));
    }

    private void sendMessage(String iface, int what, Object obj) {
        sendMessage(iface, Message.obtain(null, what, obj));
    }

    private void sendMessage(String iface, int what, int arg1) {
        sendMessage(iface, Message.obtain(null, what, arg1, 0));
    }

    private void sendMessage(String iface, int what, int arg1, int arg2) {
        sendMessage(iface, Message.obtain(null, what, arg1, arg2));
    }

    private void sendMessage(String iface, int what, int arg1, int arg2, Object obj) {
        sendMessage(iface, Message.obtain(null, what, arg1, arg2, obj));
    }

    private void sendMessage(String iface, Message message) {
        SparseArray<Set<Handler>> ifaceHandlers = (SparseArray) this.mHandlerMap.get(iface);
        boolean firstHandler;
        if (iface == null || ifaceHandlers == null) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Sending to all monitors because there's no matching iface");
            }
            firstHandler = true;
            for (Entry<String, SparseArray<Set<Handler>>> entry : this.mHandlerMap.entrySet()) {
                if (isMonitoring((String) entry.getKey())) {
                    for (Handler handler : (Set) ((SparseArray) entry.getValue()).get(message.what)) {
                        if (firstHandler) {
                            firstHandler = false;
                            sendMessage(handler, message);
                        } else {
                            sendMessage(handler, Message.obtain(message));
                        }
                    }
                }
            }
        } else if (isMonitoring(iface)) {
            firstHandler = true;
            Set<Handler> ifaceWhatHandlers = (Set) ifaceHandlers.get(message.what);
            if (ifaceWhatHandlers != null) {
                for (Handler handler2 : ifaceWhatHandlers) {
                    if (firstHandler) {
                        firstHandler = false;
                        sendMessage(handler2, message);
                    } else {
                        sendMessage(handler2, Message.obtain(message));
                    }
                }
            }
        } else if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "Dropping event because (" + iface + ") is stopped");
        }
    }

    private void sendMessage(Handler handler, Message message) {
        if (handler != null) {
            message.setTarget(handler);
            message.sendToTarget();
        }
    }

    public void broadcastWpsOverlapEvent(String iface) {
        sendMessage(iface, 147466);
    }

    public void broadcastSupplicantConnectionEvent(String iface) {
        sendMessage(iface, 147457);
    }

    public void broadcastSupplicantDisconnectionEvent(String iface) {
        sendMessage(iface, 147458);
    }

    public void broadcastApStaConnected(String iface, String macAddress) {
        sendMessage(iface, 147498, (Object) macAddress);
    }

    public void broadcastApStaDisconnected(String iface, String macAddress) {
        sendMessage(iface, 147497, (Object) macAddress);
    }
}
