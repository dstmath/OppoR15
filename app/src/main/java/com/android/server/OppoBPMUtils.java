package com.android.server;

import android.os.FileObserver;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.Xml;
import com.android.server.am.OppoCrashClearManager;
import com.android.server.am.OppoProcessManager;
import com.android.server.face.FaceDaemonWrapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class OppoBPMUtils {
    private static final String APP_WIDGET_PATH = "/data/oppo/coloros/bpm/appwidgets.xml";
    private static final String BLACK_APP_BRD_ACTION = "blackBrdAction";
    private static final String BLACK_APP_PATH = "/data/oppo/coloros/bpm/pure_background_app_blacklist.xml";
    private static final String BLACK_SYS_APP_PATH = "/data/oppo/coloros/bpm/bad_apps.xml";
    private static final String BPM_DIR = "/data/oppo/coloros/bpm";
    private static final String BPM_PATH = "/data/oppo/coloros/bpm/bpm.xml";
    private static final String BPM_STATUS_PATH = "/data/oppo/coloros/bpm/bpm_sts.xml";
    private static final String BRD_PATH = "/data/oppo/coloros/bpm/brd.xml";
    private static final String CPR_PATH = "/data/oppo/coloros/bpm/cpr.xml";
    private static final String CUSTOMIZE_APP_PATH = "/system/etc/oppo_customize_whitelist.xml";
    private static final String ELSA_SWITCH = "elsaSwitch";
    private static final String LOW_POWER_CONFIG_PATH = "/data/oppo/coloros/bpm/low_power_config.xml";
    private static final long MSG_DELAY_TIME = 200;
    private static final String PAYMODE_ENTER_TIME = "payModeEnterTime";
    private static final String PAYSAFE_SWITCH = "paySafeSwitch";
    private static final String PKG_PATH = "/data/oppo/coloros/bpm/pkg.xml";
    private static final String POWER_CONN_STATUS_PATH = "/data/oppo/coloros/bpm/power_connection_status.xml";
    private static final String RECENT_NUM = "recentNum";
    private static final String RECENT_STORE = "recentStore";
    private static final String RECORD_SWITCH = "recordSwitch";
    private static final String SCREEN_OFF_CHECK_TIME = "screenOffCheckTime";
    private static final String SCREEN_ON_CHECK_TIME = "screenOnCheckTime";
    private static final String SMART_LOW_POWER_PATH = "/data/oppo/coloros/bpm/pure_background_smart_low_power.xml";
    private static final String START_FROM_NOTITY_TIME = "startFromNotityTime";
    private static final String STRICTMODE_ENTER_TIME = "strictModeEnterTime";
    private static final String STRICTMODE_SWITCH = "strictModeSwitch";
    private static final String STRICTMODE_WHITE_PKG = "strictModeWhitePkg";
    private static final String SYS_PUREBKG_CONFIG_PATH = "/data/oppo/coloros/bpm/sys_purebkg_config.xml";
    private static final String TAG = "OppoProcessManager";
    private static OppoBPMUtils sOppoBPMUtils = null;
    private FileObserverPolicy mAppWidgetFileObserver = null;
    private List<String> mAppWidgetList = new ArrayList();
    private final Object mAppWidgetLock = new Object();
    private FileObserverPolicy mBPMConfigFileObserver = null;
    private List<String> mBlackAppBrdList = new ArrayList();
    private FileObserverPolicy mBlackAppFileObserver = null;
    private List<String> mBlackAppList = new ArrayList();
    private FileObserverPolicy mBlackSysAppFileObserver = null;
    private List<String> mBlackSysAppList = new ArrayList();
    private FileObserverPolicy mBpmFileObserver = null;
    private List<String> mBpmList = new ArrayList();
    private FileObserverPolicy mBrdFileObserver = null;
    private List<String> mBrdList = new ArrayList();
    private FileObserverPolicy mCprFileObserver = null;
    private List<String> mCprList = new ArrayList();
    private List<String> mCustomizeAppList = new ArrayList();
    private boolean mDebugDetail = OppoProcessManager.sDebugDetail;
    private List<String> mDisplayDeviceList = new ArrayList();
    private final Object mDisplayDeviceListLock = new Object();
    private boolean mElsaSwitch = true;
    private boolean mLowPower = false;
    private FileObserverPolicy mLowPowerFileObserver = null;
    private OppoProcessManager mOppoBpmManager = null;
    public long mPayModeEnterTime = FaceDaemonWrapper.TIMEOUT_FACED_BINDERCALL_CHECK;
    private boolean mPaySafeSwitch = false;
    private FileObserverPolicy mPkgFileObserver = null;
    private List<String> mPkgList = new ArrayList();
    private boolean mPowerConnStatus = false;
    private FileObserverPolicy mPowerConnStsFileObserver = null;
    private int mRecentNum = 3;
    private int mRecentStore = 9;
    private boolean mRecordSwitch = false;
    public long mScreenOffCheckTime = 60000;
    public long mScreenOnCheckTime = 60000;
    private boolean mSmartLowPower = false;
    private FileObserverPolicy mSmartLowPowerFileObserver = null;
    public long mStartFromNotityTime = 10000;
    public long mStrictModeEnterTime = 60000;
    private boolean mStrictModeSwitch = true;
    private List<String> mStrictWhitePkgList = new ArrayList();
    private final Object mStrictWhitePkgListLock = new Object();

    private class FileObserverPolicy extends FileObserver {
        private String mFocusPath;

        public FileObserverPolicy(String path) {
            super(path, 8);
            this.mFocusPath = path;
        }

        public void onEvent(int event, String path) {
            if (event == 8 && !this.mFocusPath.equals(OppoBPMUtils.BPM_STATUS_PATH)) {
                if (this.mFocusPath.equals(OppoBPMUtils.BPM_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(101, OppoBPMUtils.MSG_DELAY_TIME);
                } else if (this.mFocusPath.equals(OppoBPMUtils.PKG_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(102, 0);
                } else if (this.mFocusPath.equals(OppoBPMUtils.BRD_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(103, 0);
                } else if (this.mFocusPath.equals(OppoBPMUtils.CPR_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(112, 0);
                } else if (this.mFocusPath.equals(OppoBPMUtils.BLACK_SYS_APP_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(104, 0);
                } else if (this.mFocusPath.equals(OppoBPMUtils.APP_WIDGET_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(105, OppoBPMUtils.MSG_DELAY_TIME);
                } else if (this.mFocusPath.equals(OppoBPMUtils.BLACK_APP_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(106, 0);
                } else if (this.mFocusPath.equals(OppoBPMUtils.POWER_CONN_STATUS_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(107, OppoBPMUtils.MSG_DELAY_TIME);
                } else if (this.mFocusPath.equals(OppoBPMUtils.SMART_LOW_POWER_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(108, 0);
                } else if (this.mFocusPath.equals(OppoBPMUtils.LOW_POWER_CONFIG_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(109, 0);
                } else if (this.mFocusPath.equals(OppoBPMUtils.SYS_PUREBKG_CONFIG_PATH)) {
                    OppoBPMUtils.this.mOppoBpmManager.sendBpmEmptyMessage(110, 0);
                }
            }
        }
    }

    private OppoBPMUtils() {
        initDir();
        initData();
        initFileObserver();
    }

    public void init(OppoProcessManager oppoBpmManager) {
        this.mOppoBpmManager = oppoBpmManager;
    }

    public static OppoBPMUtils getInstance() {
        if (sOppoBPMUtils == null) {
            sOppoBPMUtils = new OppoBPMUtils();
        }
        return sOppoBPMUtils;
    }

    private void initDir() {
        try {
            File file = new File(BPM_DIR);
            if (!file.exists()) {
                file.mkdirs();
            }
            copyFile("/system/oppo/bpm_sts.xml", BPM_STATUS_PATH);
            copyFile("/system/oppo/bpm.xml", BPM_PATH);
            copyFile("/system/oppo/pkg.xml", PKG_PATH);
            copyFile("/system/oppo/brd.xml", BRD_PATH);
            copyFile("/system/oppo/cpr.xml", CPR_PATH);
            copyFile("/system/oppo/bad_apps.xml", BLACK_SYS_APP_PATH);
            copyFile("/system/oppo/appwidgets.xml", APP_WIDGET_PATH);
            copyFile("/system/oppo/sys_purebkg_config.xml", SYS_PUREBKG_CONFIG_PATH);
            confirmFileExist(BLACK_APP_PATH);
            confirmFileExist(POWER_CONN_STATUS_PATH);
            confirmFileExist(SMART_LOW_POWER_PATH);
            confirmFileExist(LOW_POWER_CONFIG_PATH);
            confirmFileExist(SYS_PUREBKG_CONFIG_PATH);
            changeMod();
        } catch (Exception e) {
            Slog.w("OppoProcessManager", "mkdir failed " + e);
        }
    }

    public void initData() {
        this.mBpmList = loadListFile(BPM_PATH);
        this.mPkgList = loadListFile(PKG_PATH);
        this.mBrdList = loadListFile(BRD_PATH);
        this.mCprList = loadListFile(CPR_PATH);
        this.mBlackSysAppList = loadListFile(BLACK_SYS_APP_PATH);
        this.mAppWidgetList = loadListFile(APP_WIDGET_PATH);
        this.mBlackAppList = loadListFile(BLACK_APP_PATH);
        this.mCustomizeAppList = loadListFile(CUSTOMIZE_APP_PATH);
        this.mPowerConnStatus = loadStatusFile(POWER_CONN_STATUS_PATH);
        this.mSmartLowPower = loadStatusFile(SMART_LOW_POWER_PATH);
        this.mLowPower = loadStatusFile(LOW_POWER_CONFIG_PATH);
        initDefaultStrictWhitePkgList();
        readBPMConfigFile();
    }

    private void initFileObserver() {
        this.mBpmFileObserver = new FileObserverPolicy(BPM_PATH);
        this.mBpmFileObserver.startWatching();
        this.mPkgFileObserver = new FileObserverPolicy(PKG_PATH);
        this.mPkgFileObserver.startWatching();
        this.mBrdFileObserver = new FileObserverPolicy(BRD_PATH);
        this.mBrdFileObserver.startWatching();
        this.mCprFileObserver = new FileObserverPolicy(CPR_PATH);
        this.mCprFileObserver.startWatching();
        this.mBlackSysAppFileObserver = new FileObserverPolicy(BLACK_SYS_APP_PATH);
        this.mBlackSysAppFileObserver.startWatching();
        this.mAppWidgetFileObserver = new FileObserverPolicy(APP_WIDGET_PATH);
        this.mAppWidgetFileObserver.startWatching();
        this.mBlackAppFileObserver = new FileObserverPolicy(BLACK_APP_PATH);
        this.mBlackAppFileObserver.startWatching();
        this.mPowerConnStsFileObserver = new FileObserverPolicy(POWER_CONN_STATUS_PATH);
        this.mPowerConnStsFileObserver.startWatching();
        this.mSmartLowPowerFileObserver = new FileObserverPolicy(SMART_LOW_POWER_PATH);
        this.mSmartLowPowerFileObserver.startWatching();
        this.mLowPowerFileObserver = new FileObserverPolicy(LOW_POWER_CONFIG_PATH);
        this.mLowPowerFileObserver.startWatching();
        this.mBPMConfigFileObserver = new FileObserverPolicy(SYS_PUREBKG_CONFIG_PATH);
        this.mBPMConfigFileObserver.startWatching();
    }

    private void changeMod() {
        try {
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/bpm_sts.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/bpm.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/pkg.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/brd.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/cpr.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/bad_apps.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/appwidgets.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/pure_background_app_blacklist.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/power_connection_status.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/pure_background_smart_low_power.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/low_power_config.xml");
            Runtime.getRuntime().exec("chmod 750 /data/oppo/coloros/bpm/sys_purebkg_config.xml");
        } catch (IOException e) {
            Slog.w("OppoProcessManager", " " + e);
        }
    }

    private void copyFile(String fromFile, String toFile) throws IOException {
        File targetFile = new File(toFile);
        if (!targetFile.exists()) {
            FileUtils.copyFile(new File(fromFile), targetFile);
        }
    }

    private void confirmFileExist(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public boolean getPowerConnStatus() {
        return this.mPowerConnStatus;
    }

    public boolean getSmartLowPower() {
        return this.mSmartLowPower;
    }

    public boolean getLowPower() {
        return this.mLowPower;
    }

    public boolean getRecordSwitch() {
        return this.mRecordSwitch;
    }

    public int getRecentTaskNum() {
        return this.mRecentNum;
    }

    public int getRecentTaskStore() {
        return this.mRecentStore;
    }

    public long getScreenOnCheckTime() {
        return this.mScreenOnCheckTime;
    }

    public long getScreenOffCheckTime() {
        return this.mScreenOffCheckTime;
    }

    public boolean getStrictModeSwitch() {
        return this.mStrictModeSwitch;
    }

    public long getStrictModeEnterTime() {
        return this.mStrictModeEnterTime;
    }

    public long getPayModeEnterTime() {
        return this.mPayModeEnterTime;
    }

    public boolean getPaySafeSwitch() {
        return this.mPaySafeSwitch;
    }

    public long getStartFromNotityTime() {
        return this.mStartFromNotityTime;
    }

    public List<String> getBpmList() {
        return this.mBpmList;
    }

    public List<String> getPkgList() {
        return this.mPkgList;
    }

    public List<String> getBrdList() {
        return this.mBrdList;
    }

    public List<String> getCprList() {
        return this.mCprList;
    }

    public List<String> getBlackSysAppList() {
        return this.mBlackSysAppList;
    }

    public List<String> getAppWidgetList() {
        return this.mAppWidgetList;
    }

    public List<String> getBlackAppList() {
        return this.mBlackAppList;
    }

    public List<String> getBlackAppBrdList() {
        return this.mBlackAppBrdList;
    }

    public List<String> getCustomizeAppList() {
        return this.mCustomizeAppList;
    }

    public List<String> getDisplayDeviceList() {
        return this.mDisplayDeviceList;
    }

    public boolean getElsaEnable() {
        return this.mElsaSwitch;
    }

    public List<String> getStrictWhitePkgList() {
        List<String> list;
        synchronized (this.mStrictWhitePkgListLock) {
            list = this.mStrictWhitePkgList;
        }
        return list;
    }

    public void reLoadStatusAndListFile(int updateMsg) {
        switch (updateMsg) {
            case 101:
                this.mBpmList = loadListFile(BPM_PATH);
                return;
            case 102:
                this.mPkgList = loadListFile(PKG_PATH);
                return;
            case 103:
                this.mBrdList = loadListFile(BRD_PATH);
                return;
            case 104:
                this.mBlackSysAppList = loadListFile(BLACK_SYS_APP_PATH);
                return;
            case 105:
                synchronized (this.mAppWidgetLock) {
                    this.mAppWidgetList = loadListFile(APP_WIDGET_PATH);
                }
                return;
            case 106:
                this.mBlackAppList = loadListFile(BLACK_APP_PATH);
                return;
            case 107:
                this.mPowerConnStatus = loadStatusFile(POWER_CONN_STATUS_PATH);
                return;
            case 108:
                this.mSmartLowPower = loadStatusFile(SMART_LOW_POWER_PATH);
                return;
            case 109:
                this.mLowPower = loadStatusFile(LOW_POWER_CONFIG_PATH);
                return;
            case 112:
                this.mCprList = loadListFile(CPR_PATH);
                return;
            default:
                return;
        }
    }

    public void reLoadBpmConfigFile() {
        readBPMConfigFile();
    }

    public void readBPMConfigFile() {
        if (this.mDebugDetail) {
            Slog.i("OppoProcessManager", "readBPMConfigFile start");
        }
        readConfigFromFileLocked(new File(SYS_PUREBKG_CONFIG_PATH));
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readConfigFromFileLocked(File file) {
        Exception e;
        Throwable th;
        if (this.mDebugDetail) {
            Slog.i("OppoProcessManager", "readConfigFromFileLocked start");
        }
        if (!this.mBlackAppBrdList.isEmpty()) {
            this.mBlackAppBrdList.clear();
        }
        List<String> strictWhitePkgList = new ArrayList();
        FileInputStream fileInputStream = null;
        try {
            FileInputStream stream = new FileInputStream(file);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, null);
                int type;
                do {
                    type = parser.next();
                    if (type == 2) {
                        String tagName = parser.getName();
                        if (this.mDebugDetail) {
                            Slog.i("OppoProcessManager", " readConfigFromFileLocked tagName=" + tagName);
                        }
                        if (RECENT_NUM.equals(tagName)) {
                            try {
                                this.mRecentNum = Integer.parseInt(parser.nextText());
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", "mRecentNum read: " + this.mRecentNum);
                                }
                            } catch (NumberFormatException e2) {
                                Slog.w("OppoProcessManager", "recentNum:Failed to translate the string to int" + e2);
                            }
                        } else if (RECENT_STORE.equals(tagName)) {
                            try {
                                this.mRecentStore = Integer.parseInt(parser.nextText());
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", "recentStore read: " + this.mRecentStore);
                                }
                            } catch (NumberFormatException e22) {
                                Slog.w("OppoProcessManager", "recentStore:Failed to translate the string to int" + e22);
                            }
                        } else if (BLACK_APP_BRD_ACTION.equals(tagName)) {
                            String action = parser.nextText();
                            if (!action.equals("")) {
                                this.mBlackAppBrdList.add(action);
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", " readConfigFromFileLocked brdaction = " + action);
                                }
                            }
                        } else if (RECORD_SWITCH.equals(tagName)) {
                            String isRecord = parser.nextText();
                            if (!isRecord.equals("")) {
                                this.mRecordSwitch = Boolean.parseBoolean(isRecord);
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", " readFromFileLocked isRecord = " + isRecord);
                                }
                            }
                        } else if (SCREEN_ON_CHECK_TIME.equals(tagName)) {
                            try {
                                this.mScreenOnCheckTime = Long.valueOf(parser.nextText()).longValue();
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", "screenOnCheckTime read: " + this.mScreenOnCheckTime);
                                }
                            } catch (NumberFormatException e222) {
                                Slog.w("OppoProcessManager", "screenOnCheckTime:Failed to translate the string to int" + e222);
                            }
                        } else if (SCREEN_OFF_CHECK_TIME.equals(tagName)) {
                            try {
                                this.mScreenOffCheckTime = Long.valueOf(parser.nextText()).longValue();
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", "screenOffCheckTime read: " + this.mScreenOffCheckTime);
                                }
                            } catch (NumberFormatException e2222) {
                                Slog.w("OppoProcessManager", "screenOffCheckTime:Failed to translate the string to int" + e2222);
                            }
                        } else if (STRICTMODE_SWITCH.equals(tagName)) {
                            String strictMode = parser.nextText();
                            if (!strictMode.equals("")) {
                                this.mStrictModeSwitch = Boolean.parseBoolean(strictMode);
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", " readFromFileLocked strictMode = " + strictMode);
                                }
                            }
                        } else if (STRICTMODE_ENTER_TIME.equals(tagName)) {
                            try {
                                this.mStrictModeEnterTime = Long.valueOf(parser.nextText()).longValue();
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", "strictModeEnterTime read: " + this.mStrictModeEnterTime);
                                }
                            } catch (NumberFormatException e22222) {
                                Slog.w("OppoProcessManager", "strictModeEnterTime:Failed to translate the string to int" + e22222);
                            }
                        } else if (PAYMODE_ENTER_TIME.equals(tagName)) {
                            try {
                                this.mPayModeEnterTime = Long.valueOf(parser.nextText()).longValue();
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", "payModeEnterTime read: " + this.mPayModeEnterTime);
                                }
                            } catch (NumberFormatException e222222) {
                                Slog.w("OppoProcessManager", "payModeEnterTime:Failed to translate the string to int" + e222222);
                            }
                        } else if (PAYSAFE_SWITCH.equals(tagName)) {
                            String paySafe = parser.nextText();
                            if (!paySafe.equals("")) {
                                this.mPaySafeSwitch = Boolean.parseBoolean(paySafe);
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", " readFromFileLocked paySafe = " + paySafe);
                                }
                            }
                        } else if (START_FROM_NOTITY_TIME.equals(tagName)) {
                            try {
                                this.mStartFromNotityTime = Long.valueOf(parser.nextText()).longValue();
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", "startFromNotityTime read: " + this.mStartFromNotityTime);
                                }
                            } catch (NumberFormatException e2222222) {
                                Slog.w("OppoProcessManager", "startFromNotityTime:Failed to translate the string to int" + e2222222);
                            }
                        } else if (STRICTMODE_WHITE_PKG.equals(tagName)) {
                            String strictWhitePkg = parser.nextText();
                            if (!strictWhitePkg.equals("")) {
                                strictWhitePkgList.add(strictWhitePkg);
                                if (this.mDebugDetail) {
                                    Slog.i("OppoProcessManager", " readFromFileLocked strictWhitePkg = " + strictWhitePkg);
                                }
                            }
                        } else if (ELSA_SWITCH.equals(tagName)) {
                            String elsaSwitch = parser.nextText();
                            if (!elsaSwitch.equals("")) {
                                this.mElsaSwitch = Boolean.valueOf(elsaSwitch).booleanValue();
                            }
                        }
                    }
                } while (type != 1);
                synchronized (this.mStrictWhitePkgListLock) {
                    if (!strictWhitePkgList.isEmpty()) {
                        this.mStrictWhitePkgList.clear();
                        this.mStrictWhitePkgList.addAll(strictWhitePkgList);
                    }
                }
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e3) {
                        Slog.e("OppoProcessManager", "Failed to close state FileInputStream " + e3);
                    }
                }
                fileInputStream = stream;
            } catch (Exception e4) {
                e = e4;
                fileInputStream = stream;
            } catch (Throwable th2) {
                th = th2;
                fileInputStream = stream;
            }
        } catch (Exception e5) {
            e = e5;
            try {
                Slog.e("OppoProcessManager", "failed parsing ", e);
                initDefaultStrictWhitePkgList();
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e32) {
                        Slog.e("OppoProcessManager", "Failed to close state FileInputStream " + e32);
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e322) {
                        Slog.e("OppoProcessManager", "Failed to close state FileInputStream " + e322);
                    }
                }
                throw th;
            }
        }
    }

    private boolean loadStatusFile(String path) {
        List<String> tempList = loadListFile(path);
        if (tempList == null || tempList.size() != 1) {
            return false;
        }
        return ((String) tempList.get(0)).equals("true");
    }

    private List<String> loadListFile(String path) {
        Exception e;
        Throwable th;
        ArrayList<String> emptyList = new ArrayList();
        File file = new File(path);
        if (file.exists()) {
            ArrayList<String> ret = new ArrayList();
            FileInputStream stream = null;
            boolean success = false;
            try {
                FileInputStream stream2 = new FileInputStream(file);
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(stream2, null);
                    int type;
                    do {
                        type = parser.next();
                        if (type == 2) {
                            if (OppoCrashClearManager.CRASH_CLEAR_NAME.equals(parser.getName())) {
                                String value = parser.getAttributeValue(null, "att");
                                if (value != null) {
                                    ret.add(value);
                                }
                            }
                        }
                    } while (type != 1);
                    success = true;
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                    stream = stream2;
                } catch (Exception e3) {
                    e = e3;
                    stream = stream2;
                    try {
                        Slog.w("OppoProcessManager", "failed parsing ", e);
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e22) {
                                e22.printStackTrace();
                            }
                        }
                        if (!success) {
                            return ret;
                        }
                        Slog.w("OppoProcessManager", path + " file failed parsing!");
                        return emptyList;
                    } catch (Throwable th2) {
                        th = th2;
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e222) {
                                e222.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    stream = stream2;
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e2222) {
                            e2222.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e4) {
                e = e4;
                Slog.w("OppoProcessManager", "failed parsing ", e);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e22222) {
                        e22222.printStackTrace();
                    }
                }
                if (!success) {
                    return ret;
                }
                Slog.w("OppoProcessManager", path + " file failed parsing!");
                return emptyList;
            }
            if (!success) {
                return ret;
            }
            Slog.w("OppoProcessManager", path + " file failed parsing!");
            return emptyList;
        }
        Slog.w("OppoProcessManager", path + " file don't exist!");
        return emptyList;
    }

    public boolean addPkgToAppWidgetList(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        synchronized (this.mAppWidgetLock) {
            this.mAppWidgetList.add(pkgName);
            saveAppWidgetLocked();
        }
        return true;
    }

    public boolean removePkgFromAppWidgetList(String pkgName) {
        boolean result = false;
        if (pkgName == null) {
            return false;
        }
        synchronized (this.mAppWidgetLock) {
            for (String pkg : this.mAppWidgetList) {
                if (pkgName.equals(pkg)) {
                    this.mAppWidgetList.remove(pkg);
                    saveAppWidgetLocked();
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public void addPkgToDisplayDeviceList(String pkgName) {
        if (pkgName != null) {
            synchronized (this.mDisplayDeviceListLock) {
                if (!this.mDisplayDeviceList.contains(pkgName)) {
                    this.mDisplayDeviceList.add(pkgName);
                }
            }
            this.mOppoBpmManager.sendBpmEmptyMessage(111, 0);
        }
    }

    public void removePkgFromDisplayDeviceList(String pkgName) {
        if (pkgName != null) {
            synchronized (this.mDisplayDeviceListLock) {
                this.mDisplayDeviceList.remove(pkgName);
            }
            this.mOppoBpmManager.sendBpmEmptyMessage(111, 0);
        }
    }

    private void saveAppWidgetLocked() {
        FileNotFoundException e;
        Throwable th;
        File file = new File(APP_WIDGET_PATH);
        FileOutputStream stream = null;
        if (this.mDebugDetail) {
            Slog.i("OppoProcessManager", "saveAppWidgetLocked mAppWidgetList.size is " + this.mAppWidgetList.size());
        }
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try {
                file.createNewFile();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        try {
            FileOutputStream stream2 = new FileOutputStream(file);
            try {
                writeAppWidgetToFileLocked(stream2);
                if (stream2 != null) {
                    try {
                        stream2.close();
                    } catch (IOException e22) {
                        if (this.mDebugDetail) {
                            Slog.e("OppoProcessManager", "Failed to close state FileInputStream " + e22);
                        }
                    }
                }
                stream = stream2;
            } catch (FileNotFoundException e3) {
                e = e3;
                stream = stream2;
                try {
                    e.printStackTrace();
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e222) {
                            if (this.mDebugDetail) {
                                Slog.e("OppoProcessManager", "Failed to close state FileInputStream " + e222);
                            }
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e2222) {
                            if (this.mDebugDetail) {
                                Slog.e("OppoProcessManager", "Failed to close state FileInputStream " + e2222);
                            }
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                stream = stream2;
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e22222) {
                        if (this.mDebugDetail) {
                            Slog.e("OppoProcessManager", "Failed to close state FileInputStream " + e22222);
                        }
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e4) {
            e = e4;
            e.printStackTrace();
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e222222) {
                    if (this.mDebugDetail) {
                        Slog.e("OppoProcessManager", "Failed to close state FileInputStream " + e222222);
                    }
                }
            }
        }
    }

    private boolean writeAppWidgetToFileLocked(FileOutputStream stream) {
        try {
            XmlSerializer out = Xml.newSerializer();
            out.setOutput(stream, "utf-8");
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, "gs");
            for (String pkg : this.mAppWidgetList) {
                if (pkg != null) {
                    out.startTag(null, OppoCrashClearManager.CRASH_CLEAR_NAME);
                    out.attribute(null, "att", pkg);
                    out.endTag(null, OppoCrashClearManager.CRASH_CLEAR_NAME);
                }
            }
            out.endTag(null, "gs");
            out.endDocument();
            return true;
        } catch (IOException e) {
            if (this.mDebugDetail) {
                Slog.e("OppoProcessManager", "Failed to write state: " + e);
            }
            return false;
        }
    }

    private void initDefaultStrictWhitePkgList() {
        synchronized (this.mStrictWhitePkgListLock) {
            this.mStrictWhitePkgList.clear();
            this.mStrictWhitePkgList.add("com.alibaba.android.rimet");
            this.mStrictWhitePkgList.add("com.tencent.mm");
            this.mStrictWhitePkgList.add("com.tencent.mobileqq");
            this.mStrictWhitePkgList.add("com.immomo.momo");
            this.mStrictWhitePkgList.add("jp.naver.line.android");
            this.mStrictWhitePkgList.add("com.coloros.gamespacesdk");
            this.mStrictWhitePkgList.add("com.zing.zalo");
            this.mStrictWhitePkgList.add("com.facebook.orca");
            this.mStrictWhitePkgList.add("com.facebook.katana");
            this.mStrictWhitePkgList.add("com.instagram.android");
            this.mStrictWhitePkgList.add("jp.naver.line.android");
            this.mStrictWhitePkgList.add("com.whatsapp");
            this.mStrictWhitePkgList.add("com.bbm");
            this.mStrictWhitePkgList.add("com.skype.raider");
            this.mStrictWhitePkgList.add("com.viber.voip");
            this.mStrictWhitePkgList.add("com.path");
            this.mStrictWhitePkgList.add("com.facebook.lite");
            this.mStrictWhitePkgList.add("com.truecaller");
            this.mStrictWhitePkgList.add("com.bsb.hike");
            this.mStrictWhitePkgList.add("com.snapchat.android");
            this.mStrictWhitePkgList.add("com.twitter.android");
            this.mStrictWhitePkgList.add("com.imo.android.imoim");
            this.mStrictWhitePkgList.add("com.google.android.gm");
        }
    }

    public boolean isForumVersion() {
        String ver = SystemProperties.get("ro.build.version.opporom");
        if (ver == null) {
            return false;
        }
        ver = ver.toLowerCase();
        if (ver.endsWith("alpha") || ver.endsWith("beta")) {
            return true;
        }
        return false;
    }
}
