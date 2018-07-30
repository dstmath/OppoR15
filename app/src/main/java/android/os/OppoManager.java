package android.os;

import android.app.ActivityThread;
import android.bluetooth.BluetoothInputDevice;
import android.content.Context;
import android.content.pm.IPackageDeleteObserver.Stub;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build.VERSION;
import android.telephony.ColorOSTelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public final class OppoManager {
    public static final int ANDROID_MSG_INPUTMETHOD_FAILD = 1004;
    public static final int ANDROID_MSG_INSTALL_FAILD = 1003;
    public static final int ANDROID_MSG_LAUNCHACTIVITY = 1002;
    public static final int ANDROID_MSG_SKIPFRAMES = 1001;
    public static final String ANDROID_PANIC_TAG = "SYSTEM_SERVER";
    public static final String ANDROID_PANIC_TAG_BEGIN = "<android-panic-begin>\n";
    public static final String ANDROID_PANIC_TAG_END = "<android-panic-end>\n";
    public static final String ANDROID_TAG = "ANDROID";
    public static final String CAMERA_TAG = "CAMERA";
    public static final String CONNECT_TAG = "CONNECTIVITY";
    private static int DATA_SIZE = 16;
    private static final boolean DEBUG = true;
    public static boolean DEBUG_GR = QE_ENABLE;
    public static final String DO_GR_CHECK_INTERNET = "DO_GR_CHECK_INTERNET";
    public static final String DO_GR_DOWN_INSTALL = "DO_GR_DOWN_INSTALL";
    public static final String DO_GR_EXIT = "DO_GR_EXIT";
    public static final String DO_GR_INSTALL_TALKBACK = "DO_GR_INSTALL_TALKBACK";
    public static final String DO_GR_REINSTALL = "DO_GR_REINSTALL";
    public static final String DO_GR_SHOW_EXCEPTION = "DO_GR_SHOW_EXCEPTION";
    public static final String DO_GR_SUCC = "DO_GR_SUCC";
    public static final String DO_GR_TALKBACK_SUCC = "DO_GR_TALKBACK_SUCC";
    public static final String ENGINEERINGMODE_TEST_BEGIN = "<engineeringmode-test-begin>\n";
    public static final String ENGINEERINGMODE_TEST_END = "<engineeringmode-test-end>\n";
    public static final String ENGINEERINGMODE_TEST_TAG = "ENGINEERINGMODE_TEST";
    public static final String EXCEPTION_TYPE_NETWORK = "NetworkError";
    public static final String EXCEPTION_TYPE_TALKBACK = "TalkbackError";
    public static final String GMAP_PNAME = "com.google.android.apps.maps";
    public static Integer GR_APK_NUMBER = Integer.valueOf(5);
    private static final int GR_BLACK_LIST = 679;
    private static final int GR_WHITE_LIST = 680;
    private static final int INIT_TRY_TIMES = 3;
    public static final String ISSUE_ANDROID_ADSP_CRASH = "adsp_crash";
    public static final String ISSUE_ANDROID_AVERAGE_CURRENT_EVENT = "average_current_event";
    public static final String ISSUE_ANDROID_CHARGER_PLUGIN_625 = "charger_plugin";
    public static final String ISSUE_ANDROID_CHARGER_PLUGOUT_626 = "charger_plugout";
    public static final String ISSUE_ANDROID_CRASH = "crash";
    public static final String ISSUE_ANDROID_FP_DIE = "fp_die";
    public static final String ISSUE_ANDROID_FP_HW_ERROR = "fp_hw_error";
    public static final String ISSUE_ANDROID_FP_RESET_BYHM = "fp_reset_byhm";
    public static final String ISSUE_ANDROID_INPUTMETHOD_FAIL = "inputmethod_fail";
    public static final String ISSUE_ANDROID_INSTALL_FAIL = "install_fail";
    public static final String ISSUE_ANDROID_LAUNCH_ACTIVITY = "launch_activity";
    public static final String ISSUE_ANDROID_MODEM_CRASH = "modem_crash";
    public static final String ISSUE_ANDROID_OTA_UPGRADE = "ota_upgrade";
    public static final String ISSUE_ANDROID_PM_50 = "scan_event";
    public static final String ISSUE_ANDROID_PM_51 = "wifi_discounnect_event";
    public static final String ISSUE_ANDROID_PM_52 = "key_exchange_event";
    public static final String ISSUE_ANDROID_PM_53 = "dhcp_relet_event";
    public static final String ISSUE_ANDROID_PM_54 = "data_call_count";
    public static final String ISSUE_ANDROID_PM_55 = "no_service_time";
    public static final String ISSUE_ANDROID_PM_56 = "reselect_per_min";
    public static final String ISSUE_ANDROID_PM_57 = "sms_send_count";
    public static final String ISSUE_ANDROID_PM_58 = "background_music";
    public static final String ISSUE_ANDROID_PM_59 = "background_download";
    public static final String ISSUE_ANDROID_PM_60 = "wifi_wakeup";
    public static final String ISSUE_ANDROID_PM_61 = "modem_wakeup";
    public static final String ISSUE_ANDROID_PM_62 = "alarm_wakeup";
    public static final String ISSUE_ANDROID_PM_63 = "base_subsystem";
    public static final String ISSUE_ANDROID_PM_64 = "power_other";
    public static final String ISSUE_ANDROID_REBOOT_FROM_BLOCKED = "reboot_from_blocked";
    public static final String ISSUE_ANDROID_SKIP_FRAMES = "skip_frames";
    public static final String ISSUE_ANDROID_SYSTEM_REBOOT_FROM_BLOCKED = "system_server_reboot_from_blocked";
    public static final String ISSUE_ANDROID_VENUS_CRASH = "venus_crash";
    public static final String ISSUE_ANDROID_WCN_CRASH = "wcn_crash";
    public static final String ISSUE_KERNEL_PANIC = "panic";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_AS_FAILED = "as_failed";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_AUTHENTICATION_REJECT = "authentication_reject";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_CARD_DROP_RX_BREAK = "card_drop_rx_break";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_CARD_DROP_TIME_OUT = "card_drop_time_out";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_DATA_NOT_ALLOWED = "data_no_allowed";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_DATA_NO_AVAILABLE_APN = "data_no_acailable_apn";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_DATA_SET_UP_DATA_ERROR = "data_set_up_data_error";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_GSM_T3126_EXPIRED = "gsm_t3126_expired";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_LTE_AS_FAILED = "lte_as_failed";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_LTE_REG_REJECT = "ltc_reg_reject";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_LTE_REG_WITHOUT_LTE = "lte_reg_without_lte";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_MCFG_ICCID_FAILED = "mcfg_iccid_failed";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_MO_DROP = "mo_drop";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_MT_CSFB = "mt_csfb";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_MT_PCH = "mt_pch";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_MT_RACH = "mt_rach";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_MT_REJECT = "mt_reject";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_MT_RLF = "mt_rlf";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_MT_RRC = "mt_rrc";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_REG_REJECT = "reg_rejetc";
    public static final String ISSUE_SYS_OEM_NW_DIAG_CAUSE_RF_MIPI_HW_FAILED = "rf_mipi_hw_failed";
    public static final String ISSUE_WIFI_CONNECTING_FAILURE = "wifi_connecting_failure";
    public static final String ISSUE_WIFI_LOAD_DRIVER_FAILURE = "wifi_load_driver_failure";
    public static final String ISSUE_WIFI_TURN_ON_OFF_FAILURE = "wifi_turn_on_off_failure";
    public static final String KERNEL_PANIC_TAG = "SYSTEM_LAST_KMSG";
    public static final String KERNEL_PANIC_TAG_BEGIN = "<kernel-panic-begin>\n";
    public static final String KERNEL_PANIC_TAG_END = "<kernel-panic-end>\n";
    public static final String KERNEL_TAG = "KERNEL";
    public static final String MULTIMEDIA_TAG = "MULTIMEDIA";
    public static final String NETWORK_TAG = "NETWORK";
    public static final String OPPO_ROAM_SUPPORT_PARAM_NAME = "canSupportOppoRoam";
    public static final String PARAM_APP_NAME = "appName";
    public static final String PARAM_BASE_CODE_PATH = "baseCodePath";
    public static final String PARAM_EXCEPTION_TYPE = "exceptionType";
    public static final String PARAM_PKG_NAME = "pkgName";
    public static final boolean QE_ENABLE = SystemProperties.getBoolean("persist.sys.assert.panic", false);
    public static Integer SEPERATE_SIZE = Integer.valueOf(242473);
    public static final String SERVICE_NAME = "OPPO";
    public static final String SHUTDOWN_TAG = "SYSTEM_SHUTDOWN";
    public static final String SHUTDOWN_TAG_BEGIN = "<shutdown-begin>\n";
    public static final String SHUTDOWN_TAG_END = "<shutdown-end>\n";
    public static final String SPMI_BEGIN = "<spmi-begin>\n";
    public static final String SPMI_END = "<spmi-end>\n";
    public static final String SPMI_TAG = "SPMI";
    public static final String TAG = "OppoManager";
    public static int TYEP_Android_VER = 2;
    public static int TYEP_BUILD_VER = 3;
    public static int TYEP_DEVICE = 4;
    public static int TYEP_PHONE_IMEI = 1;
    public static int TYPE_ANDROID_ADSP_CRASH = 44;
    public static int TYPE_ANDROID_AVERAGE_CURRENT_EVENT = 37;
    public static int TYPE_ANDROID_BACK_KEY = 33;
    public static int TYPE_ANDROID_CAMERA = 28;
    public static final int TYPE_ANDROID_CHARGER_PLUGIN_625 = 625;
    public static final int TYPE_ANDROID_CHARGER_PLUGOUT_626 = 626;
    public static int TYPE_ANDROID_CRASH = 22;
    public static int TYPE_ANDROID_FP_DIE = 47;
    public static int TYPE_ANDROID_FP_HW_ERROR = 49;
    public static int TYPE_ANDROID_FP_RESET_BYHM = 48;
    public static int TYPE_ANDROID_HOME_KEY = 31;
    public static int TYPE_ANDROID_INPUTMETHOD_FAIL = 43;
    public static int TYPE_ANDROID_INSTALL_FAILD = 40;
    public static int TYPE_ANDROID_LAUNCH_ACTIVITY = 39;
    public static int TYPE_ANDROID_MENU_KEY = 32;
    public static int TYPE_ANDROID_OTA_FAILD = 41;
    public static int TYPE_ANDROID_OTA_UPGRADE = 29;
    public static int TYPE_ANDROID_PM_EVENT_50 = 50;
    public static int TYPE_ANDROID_PM_EVENT_51 = 51;
    public static int TYPE_ANDROID_PM_EVENT_52 = 52;
    public static int TYPE_ANDROID_PM_EVENT_53 = 53;
    public static int TYPE_ANDROID_PM_EVENT_54 = 54;
    public static int TYPE_ANDROID_PM_EVENT_55 = 55;
    public static int TYPE_ANDROID_PM_EVENT_56 = 56;
    public static int TYPE_ANDROID_PM_EVENT_57 = 57;
    public static int TYPE_ANDROID_PM_EVENT_58 = 58;
    public static int TYPE_ANDROID_PM_EVENT_59 = 59;
    public static int TYPE_ANDROID_PM_EVENT_60 = 60;
    public static int TYPE_ANDROID_PM_EVENT_61 = 61;
    public static int TYPE_ANDROID_PM_EVENT_62 = 62;
    public static int TYPE_ANDROID_PM_EVENT_63 = 63;
    public static int TYPE_ANDROID_PM_EVENT_64 = 64;
    public static int TYPE_ANDROID_POWER_KEY = 36;
    public static int TYPE_ANDROID_SKIPFRAMES = 38;
    public static int TYPE_ANDROID_SPMI = 24;
    public static int TYPE_ANDROID_SYSTEM_REBOOT_FROM_BLOCKED = 26;
    public static int TYPE_ANDROID_UNKNOWN_REBOOT = 42;
    public static int TYPE_ANDROID_USB = 30;
    public static int TYPE_ANDROID_VENUS_CRASH = 45;
    public static int TYPE_ANDROID_VOLDOWN_KEY = 35;
    public static int TYPE_ANDROID_VOLUP_KEY = 34;
    public static int TYPE_ANDROID_WCN_CRASH = 46;
    public static int TYPE_BATTERY_CHARGE_HISTORY = 8;
    public static int TYPE_CRITICAL_DATA_SIZE = 512;
    public static int TYPE_HW_SHUTDOWN = 5;
    public static int TYPE_LOGSIZE = 1022;
    public static int TYPE_LOGVER = 0;
    public static int TYPE_MODERN = 23;
    public static int TYPE_OTA_FLAG = 6;
    public static int TYPE_PANIC = 600;
    public static int TYPE_REBOOT = 21;
    public static int TYPE_REBOOT_FROM_BLOCKED = 27;
    public static int TYPE_RESMON = 25;
    public static int TYPE_ROOT_FLAG = 7;
    public static int TYPE_SHUTDOWN = 20;
    public static final int TYPE_SYMBOL_VERSION_DISAGREE = 803;
    public static final int TYPE_WDI_EXCEPTION = 804;
    public static int TYPE_WIFI_CONNECT_FAILED = 800;
    public static final String USER_IN_CHINA = "isInChina";
    public static final String WHETHER_IN_CHINA_PARAM_NAME = "isInChina";
    public static Boolean canCreateDialog = Boolean.valueOf(true);
    public static Boolean canReinstall = Boolean.valueOf(true);
    private static List<String> cannotExit = Arrays.asList(new String[]{"android"});
    private static List<String> grBlackList = Arrays.asList(new String[]{"com.google.android.exoplayer.playbacktests", "com.google.android.packageinstaller", "com.google.android.apps.youtube.testsuite", "com.google.android.accounts.gts.unaffiliated", "android.largeapk.app", "com.android.compatibility.common.deviceinfo", "com.android.cts.priv.ctsshim", "com.android.gts.ssaidapp1", "com.android.gts.ssaidapp2", "com.android.preconditions.gts", OppoGoogleResource.TALKBACK_PNAME, "com.google.android.ar.svc"});
    public static List<String> grList = Arrays.asList(new String[]{"com.google.android.gms", "com.google.android.partnersetup", "com.google.android.gsf", "com.google.android.syncadapters.calendar", "com.google.android.syncadapters.contacts"});
    public static final Signature[] grSig = new Signature[]{new Signature("308204433082032ba003020102020900c2e08746644a308d300d06092a864886f70d01010405003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d3038303832313233313333345a170d3336303130373233313333345a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820120300d06092a864886f70d01010105000382010d00308201080282010100ab562e00d83ba208ae0a966f124e29da11f2ab56d08f58e2cca91303e9b754d372f640a71b1dcb130967624e4656a7776a92193db2e5bfb724a91e77188b0e6a47a43b33d9609b77183145ccdf7b2e586674c9e1565b1f4c6a5955bff251a63dabf9c55c27222252e875e4f8154a645f897168c0b1bfc612eabf785769bb34aa7984dc7e2ea2764cae8307d8c17154d7ee5f64a51a44a602c249054157dc02cd5f5c0e55fbef8519fbe327f0b1511692c5a06f19d18385f5c4dbc2d6b93f68cc2979c70e18ab93866b3bd5db8999552a0e3b4c99df58fb918bedc182ba35e003c1b4b10dd244a8ee24fffd333872ab5221985edab0fc0d0b145b6aa192858e79020103a381d93081d6301d0603551d0e04160414c77d8cc2211756259a7fd382df6be398e4d786a53081a60603551d2304819e30819b8014c77d8cc2211756259a7fd382df6be398e4d786a5a178a4763074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964820900c2e08746644a308d300c0603551d13040530030101ff300d06092a864886f70d010104050003820101006dd252ceef85302c360aaace939bcff2cca904bb5d7a1661f8ae46b2994204d0ff4a68c7ed1a531ec4595a623ce60763b167297a7ae35712c407f208f0cb109429124d7b106219c084ca3eb3f9ad5fb871ef92269a8be28bf16d44c8d9a08e6cb2f005bb3fe2cb96447e868e731076ad45b33f6009ea19c161e62641aa99271dfd5228c5c587875ddb7f452758d661f6cc0cccb7352e424cc4365c523532f7325137593c4ae341f4db41edda0d0b1071a7c440f0fe9ea01cb627ca674369d084bd2fd911ff06cdbf2cfa10dc0f893ae35762919048c7efc64c7144178342f70581c9de573af55b390dd7fdb9418631895d5f759f30112687ff621410c069308a")};
    public static Boolean isInnerVersion = Boolean.valueOf(true);
    public static Boolean isNoDialogInstalling = Boolean.valueOf(false);
    public static List<String> mGrApkPathList = Arrays.asList(new String[]{"/data/gr/138e8af41c2a62b4c06adf65577772419.gr", "/data/gr/290aa18407779e8f44cb57733d3b5ea23.gr", "/data/gr/3b64e23f2e4cdf5b109c52f30b37cdcb5.gr", "/data/gr/4f20989b475c563b80c11b18a5c02b457.gr", "/data/gr/5010a28878517c105a60f155f0c6f5c56.gr", "/data/gr/6f8acd492101e6b11f5eadcc188566ae1.gr"});
    public static List<String> queue = new ArrayList();
    private static IOppoService sService;
    public static Boolean willUseGrLeader = Boolean.valueOf(true);

    private static class PackageDeleteObserver extends Stub {
        /* synthetic */ PackageDeleteObserver(PackageDeleteObserver -this0) {
            this();
        }

        private PackageDeleteObserver() {
        }

        public void packageDeleted(String packageName, int returnCode) {
            if (returnCode == 1) {
                Log.d(OppoManager.TAG, "Geloin: we uninstalled " + packageName);
            }
        }
    }

    private static native int native_oppoManager_cleanItem(int i);

    private static native String native_oppoManager_readCriticalData(int i, int i2);

    private static native String native_oppoManager_readRawPartition(int i, int i2);

    private static native int native_oppoManager_syncCahceToEmmc();

    private static native String native_oppoManager_testFunc(int i, int i2);

    private static native int native_oppoManager_updateConfig();

    private static native int native_oppoManager_writeCriticalData(int i, String str);

    private static native int native_oppoManager_writeRawPartition(int i, String str, int i2);

    static {
        initGr();
    }

    public static final boolean init() {
        initGr();
        if (sService != null) {
            return true;
        }
        int times = 3;
        do {
            Log.w(TAG, "Try to OppoService Instance! times = " + times);
            sService = IOppoService.Stub.asInterface(ServiceManager.getService("OPPO"));
            if (sService != null) {
                return true;
            }
            times--;
        } while (times > 0);
        return false;
    }

    public static void stopLeader() {
        willUseGrLeader = Boolean.valueOf(false);
    }

    public static Boolean isNeedLeader(String pkgName) {
        if (!(pkgName == null || (pkgName.startsWith("com.google.android.xts") ^ 1) == 0)) {
            int endsWith = pkgName.startsWith("com.google.android") ? !pkgName.endsWith(".gts") ? pkgName.endsWith(".xts") : 1 : 0;
            if (!((endsWith ^ 1) == 0 || (grBlackList.contains(pkgName) ^ 1) == 0 || (pkgName.startsWith("com.google.android.gts") ^ 1) == 0 || ActivityThread.inCptWhiteList(GR_BLACK_LIST, pkgName) || (!pkgName.startsWith("com.google.android") && !pkgName.equals("com.android.chrome") && !ActivityThread.inCptWhiteList(GR_WHITE_LIST, pkgName)))) {
                return Boolean.valueOf(true);
            }
        }
        return Boolean.valueOf(false);
    }

    public static void uninstallGrs(Context mContext) {
        PackageManager pm = mContext.getPackageManager();
        List<String> grList = grList;
        for (int i = 0; i < grList.size(); i++) {
            String pkgName = (String) grList.get(i);
            if (!queue.contains(pkgName)) {
                queue.add(pkgName);
                pm.deletePackage(pkgName, new PackageDeleteObserver(), 2);
            }
        }
    }

    public static Boolean grExists() {
        if (!willUseGrLeader.booleanValue() || (isInnerVersion.booleanValue() ^ 1) != 0) {
            return Boolean.valueOf(true);
        }
        String dataPath = "/data/data/";
        for (String name : grList) {
            if (!new File(dataPath + name).exists()) {
                return Boolean.valueOf(false);
            }
        }
        return Boolean.valueOf(true);
    }

    public static Boolean canShowDialog(String pkgName) {
        if (canCreateDialog.booleanValue()) {
            return Boolean.valueOf(true);
        }
        if (DEBUG_GR) {
            Log.d(TAG, "Geloin: We are installing GR so not leader to install.");
        }
        return Boolean.valueOf(false);
    }

    public static void exit(String pkgName) {
        if (pkgName == null || !cannotExit.contains(pkgName)) {
            doGr(null, null, pkgName, DO_GR_EXIT);
        } else if (DEBUG_GR) {
            Log.d(TAG, "Geloin: Some application can't be killed.");
        }
    }

    public static void doGr(String baseCodePath, String appName, String pkgName, String action) {
        if (sService != null || (init() ^ 1) == 0) {
            try {
                sService.doGr(baseCodePath, appName, pkgName, action);
            } catch (RemoteException e) {
                if (DEBUG_GR) {
                    Log.e(TAG, "Geloin: doGr exception!");
                    e.printStackTrace();
                }
            }
            return;
        }
        if (DEBUG_GR) {
            Log.d(TAG, "Geloin: Didn't init Service for GR.");
        }
    }

    private static void initGr() {
        willUseGrLeader = Boolean.valueOf(SystemProperties.getBoolean("gr.use.leader", false));
        if (willUseGrLeader.booleanValue()) {
            GR_APK_NUMBER = Integer.valueOf(SystemProperties.getInt("gr.apk.number", 5));
        }
        if (SystemProperties.get("ro.oppo.version", "CN").equals("CN")) {
            isInnerVersion = Boolean.valueOf(true);
        } else {
            isInnerVersion = Boolean.valueOf(false);
        }
    }

    public static String readRawPartition(int offset, int size) {
        String res = null;
        try {
            return native_oppoManager_readRawPartition(offset, size);
        } catch (Exception e) {
            Log.e(TAG, "read Raw Partition exception!");
            e.printStackTrace();
            return res;
        }
    }

    public static int writeRawPartition(int type, String content, int isAddToDropbox) {
        int res = -1;
        try {
            return native_oppoManager_writeRawPartition(type, content, isAddToDropbox);
        } catch (Exception e) {
            Log.e(TAG, "write Raw Partition exception!");
            e.printStackTrace();
            return res;
        }
    }

    public static int readCriticalData(int type) {
        int res = 0;
        String dataString = readCriticalData(type, DATA_SIZE);
        if (dataString == null) {
            return 0;
        }
        dataString = dataString.trim();
        if (dataString == null || dataString.length() == 0) {
            return 0;
        }
        try {
            res = Integer.parseInt(dataString) + 0;
        } catch (Exception e) {
            Log.e(TAG, "read critical data failed!! e = " + e.toString());
            e.printStackTrace();
        }
        return res;
    }

    public static String readCriticalData(int id, int size) {
        String res = null;
        try {
            return native_oppoManager_readCriticalData(id, size);
        } catch (Exception e) {
            Log.e(TAG, "read Critical Data exception!\n");
            e.printStackTrace();
            return res;
        }
    }

    public static int writeCriticalData(int id, String content) {
        if (content != null) {
            try {
                if (content.length() > TYPE_CRITICAL_DATA_SIZE - 10) {
                    content = content.substring(0, TYPE_CRITICAL_DATA_SIZE - 10);
                }
            } catch (Exception e) {
                Log.e(TAG, "write Critical Data exception!\n");
                e.printStackTrace();
                return -1;
            }
        }
        return native_oppoManager_writeCriticalData(id, content);
    }

    public static String getTime() {
        String strTime = "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis())) + "\n";
    }

    public static String getIMEINums(Context context) {
        String imei = "";
        try {
            imei = ColorOSTelephonyManager.getDefault(context).colorGetImei(0);
        } catch (Exception e) {
            Log.e(TAG, "Exception: ", e);
        }
        if (imei == null) {
            imei = "null";
        }
        Log.i(TAG, "imei:" + imei);
        return imei;
    }

    public static String getVersionFOrAndroid() {
        if (TextUtils.isEmpty(VERSION.RELEASE)) {
            return "null";
        }
        return VERSION.RELEASE;
    }

    public static String getOppoRomVersion() {
        String ver = SystemProperties.get("ro.build.version.opporom");
        if (ver == null || ver.isEmpty()) {
            return "null";
        }
        return ver;
    }

    public static String getBuildVersion() {
        String ver = SystemProperties.get("ro.build.version.ota");
        if (ver == null || ver.isEmpty()) {
            return "null";
        }
        return ver;
    }

    public static void recordEventForLog(int event, String log) {
        if (sService != null || (init() ^ 1) == 0) {
            try {
                Log.v(TAG, "recordEventForLog event = " + event);
                sService.recordCriticalEvent(event, Process.myPid(), log);
            } catch (Exception e) {
                Log.v(TAG, "record exception e =" + e.toString());
            }
            return;
        }
        Log.d(TAG, "can not init the oppo service");
    }

    public static int writeLogToPartition(String logstring, String tagString, String issue) {
        Log.v(TAG, "this is the old api");
        return -1;
    }

    public static int writeLogToPartition(int type, String logstring, String tagString, String issue, int isOnlyAddToDropbox) {
        if (logstring == null) {
            return -1;
        }
        String tagbegin = "";
        String tagend = "";
        String time = "log-time: " + getTime();
        String buildTime = "log-buildTime: " + SystemProperties.get("ro.build.version.ota", "") + "\n";
        String colorOS = "log-colorOS: " + SystemProperties.get("ro.build.version.opporom", "") + "\n";
        String logType = String.format("LOGTYPE: %d\n", new Object[]{Integer.valueOf(type)});
        if (issue == null || issue.isEmpty()) {
            issue = tagString;
        }
        if (tagString.equals(ANDROID_TAG)) {
            tagbegin = "<android-" + issue + "-begin>\n";
            tagend = "\n<android-" + issue + "-end>\n";
        } else if (tagString.equals(MULTIMEDIA_TAG)) {
            tagbegin = "<multimedia-" + issue + "-begin>\n";
            tagend = "\n<multimedia-" + issue + "-end>\n";
        } else if (tagString.equals(NETWORK_TAG)) {
            tagbegin = "<network-" + issue + "-begin>\n";
            tagend = "\n<network-" + issue + "-end>\n";
        } else if (tagString.equals(KERNEL_TAG)) {
            tagbegin = "<kernel-" + issue + "-begin>\n";
            tagend = "\n<kernel-" + issue + "-end>\n";
        } else if (tagString.equals(CONNECT_TAG)) {
            tagbegin = "<connectivity-" + issue + "-begin>\n";
            tagend = "\n<connectivity-" + issue + "-end>\n";
        } else if (tagString.equals(CAMERA_TAG)) {
            tagbegin = "<camera-" + issue + "-begin>\n";
            tagend = "\n<camera-" + issue + "-end>\n";
        } else {
            Log.v(TAG, "the invalid tag");
            return -1;
        }
        return writeRawPartition(type, tagbegin + logType + time + buildTime + colorOS + logstring + tagend, isOnlyAddToDropbox);
    }

    public static int incrementCriticalData(int type, String desc) {
        return writeLogToPartition(type, null, null, null, desc);
    }

    public static int writeLogToPartition(int type, String logstring, String tagString, String issue, String desc) {
        int res;
        if (logstring == null) {
            res = 0;
        } else if (logstring.isEmpty()) {
            Log.v(TAG, "log is empty");
            res = 0;
        } else {
            res = writeLogToPartition(type, logstring, tagString, issue, -1);
        }
        int upRes = updateLogReference(type, desc, false);
        if (type > 19) {
            upRes = updateLogReference(type, desc, true);
        }
        if (upRes == -1 && res == -1) {
            return -3;
        }
        if (upRes != -1 || res == -1) {
            return (upRes == -1 || res != -1) ? 1 : -1;
        } else {
            return -2;
        }
    }

    private static int updateLogReference(int type, String desc, boolean isBackup) {
        String ref;
        int res;
        if (isBackup) {
            ref = readCriticalData(type + 1024, 256);
            Log.v(TAG, "updateLogReference read backup type=" + (type + 1024) + " ref=" + ref);
        } else {
            ref = readCriticalData(type, 256);
            Log.v(TAG, "updateLogReference read now type=" + type + " ref=" + ref);
        }
        if (ref == null || ref.isEmpty()) {
            ref = String.format("%d:%s:%d", new Object[]{Integer.valueOf(type), desc, Integer.valueOf(1)});
        } else {
            String[] refSplit = ref.split(":");
            if (refSplit == null || refSplit.length < 2) {
                Log.v(TAG, "update can not get any keyword");
                ref = String.format("%d:%s:%d", new Object[]{Integer.valueOf(type), desc, Integer.valueOf(1)});
            } else {
                try {
                    int count = Integer.parseInt(refSplit[2]);
                    if (desc.equals(refSplit[1])) {
                        ref = String.format("%d:%s:%d", new Object[]{Integer.valueOf(type), desc, Integer.valueOf(count + 1)});
                    } else {
                        ref = String.format("%d:%s:%d", new Object[]{Integer.valueOf(type), desc, Integer.valueOf(1)});
                    }
                } catch (Exception e) {
                    Log.v(TAG, "catch e = " + e.toString());
                    ref = String.format("%d:%s:%d", new Object[]{Integer.valueOf(type), desc, Integer.valueOf(1)});
                }
            }
        }
        if (isBackup) {
            res = writeCriticalData(type + 1024, ref);
        } else {
            res = writeCriticalData(type, ref);
        }
        Log.v(TAG, "updateLogReference res=" + res);
        return res;
    }

    public static boolean isEmmcLimit(int type) {
        try {
            String[] refSplit = readCriticalData(type + 1024, 256).split(":");
            if (refSplit == null || refSplit.length < 2) {
                Log.v(TAG, "the refs is not formative");
                return false;
            }
            try {
                if (Integer.parseInt(refSplit[2]) < BluetoothInputDevice.INPUT_DISCONNECT_FAILED_NOT_CONNECTED) {
                    return false;
                }
            } catch (Exception e) {
                Log.v(TAG, "catch e = " + e.toString());
            }
            Log.v(TAG, "limit to record type = " + type);
            return true;
        } catch (Exception e2) {
            Log.v(TAG, "isEmmcLimit exception e = " + e2.toString());
            return false;
        }
    }

    public static int cleanItem(int id) {
        return native_oppoManager_cleanItem(id);
    }

    public static int syncCacheToEmmc() {
        native_oppoManager_syncCahceToEmmc();
        return 0;
    }

    public static int updateConfig() {
        native_oppoManager_updateConfig();
        return 0;
    }

    public static int testFunc(int id, int size) {
        native_oppoManager_testFunc(id, size);
        return 0;
    }
}
