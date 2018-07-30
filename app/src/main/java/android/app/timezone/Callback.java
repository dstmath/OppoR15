package android.app.timezone;

public abstract class Callback {
    public static final int ERROR_INSTALL_BAD_DISTRO_FORMAT_VERSION = 3;
    public static final int ERROR_INSTALL_BAD_DISTRO_STRUCTURE = 2;
    public static final int ERROR_INSTALL_RULES_TOO_OLD = 4;
    public static final int ERROR_INSTALL_VALIDATION_ERROR = 5;
    public static final int ERROR_UNKNOWN_FAILURE = 1;
    public static final int SUCCESS = 0;

    public abstract void onFinished(int i);
}
