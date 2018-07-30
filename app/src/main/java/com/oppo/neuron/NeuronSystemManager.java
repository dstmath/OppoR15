package com.oppo.neuron;

import android.content.ContentValues;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.oppo.neuron.INeuronSystemService.Stub;

public final class NeuronSystemManager {
    public static final int APP_USAGE_IS_PRIOR = 1;
    public static final int APP_USAGE_NOT_PRIOR = 0;
    public static final int APP_USAGE_UNKNOWN = -1;
    public static final long DEFAULT_HOOK_PROP = 0;
    public static final int DEFAULT_PROP = 29;
    public static final int FLAG_PAUSE_TIME = 1;
    public static final int FLAG_RESUME_TIME = 2;
    public static final boolean LOG_ON = SystemProperties.getBoolean("persist.sys.ns_logon", false);
    public static final int NS_ALL_ACCESS_ALLOWED = 4;
    public static final int NS_APP_PRELOAD = 64;
    public static final int NS_APP_USAGE_TRACKER = 8;
    public static final int NS_EVENT_PUBLISH = 16;
    public static final long NS_HOOK_GPS_UPDATE = 1;
    public static final long NS_HOOK_SIGNAL_UPDATE = 2;
    public static final int NS_ON = 1;
    public static final int NS_UPLOAD_DB = 32;
    public static final int NS_WRITE_DB = 2;
    private static final String TAG = "NeuronSystem";
    private static NeuronSystemManager sNeuronSystemManager = null;
    public static final long sNsHookProp = SystemProperties.getLong("persist.sys.neuron_system.hook", 0);
    public static int sNsProp = SystemProperties.getInt("persist.sys.neuron_system", 29);
    private INeuronSystemService mService;

    private NeuronSystemManager() {
        this.mService = null;
        this.mService = Stub.asInterface(ServiceManager.getService("neuronsystem"));
    }

    public static NeuronSystemManager getInstance() {
        if (sNeuronSystemManager == null) {
            synchronized (NeuronSystemManager.class) {
                if (sNeuronSystemManager == null) {
                    sNeuronSystemManager = new NeuronSystemManager();
                }
            }
        }
        return sNeuronSystemManager;
    }

    public static boolean isEnable() {
        return (sNsProp & 1) != 0;
    }

    public static boolean isHookEnable(long sProperty) {
        return (sNsHookProp & sProperty) != 0;
    }

    public void publishEvent(int type, ContentValues contentValues) {
        if (this.mService != null) {
            try {
                this.mService.publishEvent(type, contentValues);
            } catch (Exception e) {
                Slog.d("NeuronSystem", "NeuronSystemManager publishEvent err: " + e);
            }
        }
    }

    public int isPriorApp(String packageName) {
        if (this.mService == null) {
            return -1;
        }
        try {
            return this.mService.isPriorApp(packageName);
        } catch (RemoteException e) {
            Slog.e("NeuronSystem", "Exception happend while query prior App", e);
            return -1;
        }
    }

    long[] getAppRecentUsedTime(String packageName, int flag) {
        if (this.mService == null) {
            return null;
        }
        try {
            return this.mService.getAppRecentUsedTime(packageName, flag);
        } catch (RemoteException e) {
            Slog.e("NeuronSystem", "Exception happend while get App recent used time", e);
            return null;
        }
    }
}
