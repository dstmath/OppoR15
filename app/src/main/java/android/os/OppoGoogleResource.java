package android.os;

public final class OppoGoogleResource {
    private static final boolean DEBUG = true;
    public static final String DO_GR_INSTALL_TALKBACK = "DO_GR_INSTALL_TALKBACK";
    public static final String DO_GR_TALKBACK_SUCC = "DO_GR_TALKBACK_SUCC";
    public static final String GMAP_PNAME = "com.google.android.apps.maps";
    public static final String TAG = "OppoGoogleResource";
    public static final String TALKBACK_PNAME = "com.google.android.marvin.talkback";

    public static void doInstallGoogleApp(String baseCodePath, String appName, String pkgName, String action) {
        OppoManager.doGr(baseCodePath, appName, pkgName, action);
    }
}
