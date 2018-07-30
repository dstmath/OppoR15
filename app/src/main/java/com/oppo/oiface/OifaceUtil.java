package com.oppo.oiface;

import android.os.DeadObjectException;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.oppo.oiface.OifaceService.Stub;

public class OifaceUtil {
    private static int NET_STATUS = 1;
    public static final int OIFACE_ON = 0;
    private static final String TAG = "OifaceUtil";
    private static OifaceUtil mOifaceUtil = null;
    private static OifaceService mService = null;
    public static int sOifaceProp = SystemProperties.getInt("persist.sys.oiface.enable", 0);

    public enum NetType {
        OIFACE_NETWORK_DATA_ON_WLAN,
        OIFACE_NETWORK_DATA_OFF_WLAN,
        OIFACE_NETWORK_DATA_ON_WWLAN,
        OIFACE_NETWORK_DATA_OFF_WWLAN
    }

    private OifaceUtil() {
        mService = Stub.asInterface(ServiceManager.checkService("oiface"));
        if (mService != null) {
            currentNetwork(NET_STATUS);
        }
    }

    public static OifaceUtil getInstance() {
        if (mService == null) {
            synchronized (OifaceUtil.class) {
                if (mService == null) {
                    mOifaceUtil = new OifaceUtil();
                }
            }
        }
        return mOifaceUtil;
    }

    public void currentNetwork(int status) {
        if (mService != null) {
            try {
                mService.currentNetwork(status);
                NET_STATUS = status;
            } catch (DeadObjectException e) {
                Slog.i(TAG, "OifaceService currentNetwork err: " + e);
                mService = null;
            } catch (RemoteException e2) {
                Slog.e(TAG, "current network error" + e2);
            }
        }
    }

    public void currentPackage(String pkgName, int uid, int pid) {
        if (mService != null) {
            try {
                mService.currentPackage(pkgName, uid, pid);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "OifaceService currentPackage err: " + e);
                mService = null;
            } catch (RemoteException e2) {
                Slog.d(TAG, "current package error" + e2);
            }
        }
    }

    public static boolean isEnable() {
        return sOifaceProp != 0;
    }
}
