package android.os;

import android.os.IOppoService.Stub;
import android.util.Log;

public final class OppoZoneInfoFileController {
    private static final boolean DEBUG = true;
    public static final String SERVICE_NAME = "OPPO";
    private static final String TAG = "OppoZoneInfoFileController";
    private static OppoZoneInfoFileController mInstance = null;
    private static IOppoService sService;

    private OppoZoneInfoFileController() {
        sService = Stub.asInterface(ServiceManager.getService("OPPO"));
    }

    public static OppoZoneInfoFileController getOppoZoneInfoFileController() {
        if (mInstance == null) {
            mInstance = new OppoZoneInfoFileController();
        }
        return mInstance;
    }

    public boolean copyFile(String destPath, String srcPath) {
        try {
            return sService.copyFile(destPath, srcPath);
        } catch (RemoteException e) {
            Log.e(TAG, "copyFile failed.", e);
            return false;
        }
    }

    public boolean deleteFile(String path) {
        try {
            return sService.deleteFile(path);
        } catch (RemoteException e) {
            Log.e(TAG, "deleteFile failed.", e);
            return false;
        }
    }
}
