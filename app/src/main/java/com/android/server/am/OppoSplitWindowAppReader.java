package com.android.server.am;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.os.FileObserver;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.util.ArraySet;
import android.util.Log;
import android.util.Xml;
import android.view.IOppoWindowManagerImpl;
import com.android.server.OppoBPMHelper;
import com.android.server.coloros.OppoListManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;

public final class OppoSplitWindowAppReader {
    private static final String ALLOW_PKG = "com.tencent.mm";
    private static boolean DEBUG = false;
    private static boolean DEBUG_DETAIL = SystemProperties.getBoolean("persist.sys.assert.panic", false);
    private static final String FORCE_PKG_NAME = "bp";
    private static final String LOCKED_APPS_FILE = "locked_apps.xml";
    private static final String PKG_ATTR = "attr";
    private static final String PKG_NAME = "p";
    private static final String SPLIT_WINDOW_APP_FILE_PATH = "/data/oppo/coloros/config/sys_wms_split_app.xml";
    private static final String SYSTEM_CONFIG_PATH = "/data/oppo/coloros/config";
    private static final String TAG = "OppoSplitWindowAppReader";
    private static final String XMLPATH = "/data/oppo/coloros/recenttask";
    private static boolean mHasUpdate = false;
    private static volatile OppoSplitWindowAppReader mIns = null;
    private ArrayList<String> mAllowPackageName = new ArrayList();
    private DataFileListener mDataFileListener;
    private ArrayList<String> mForcePackageName = new ArrayList();
    private Object mLock = new Object();

    private class DataFileListener extends FileObserver {
        public DataFileListener(String path) {
            super(path, 12);
        }

        public void onEvent(int event, String path) {
            if (OppoSplitWindowAppReader.DEBUG_DETAIL) {
                Log.i(OppoSplitWindowAppReader.TAG, "readAppList onEvent: " + event);
            }
            if (event == 8) {
                OppoSplitWindowAppReader.this.getSplitWindowApp();
            }
        }
    }

    private static class UserIdPkg {
        public String packageName;
        public int userId;

        public UserIdPkg(String packageName, int userId) {
            this.userId = userId;
            this.packageName = packageName;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (obj instanceof UserIdPkg) {
                UserIdPkg tmp = (UserIdPkg) obj;
                return this.userId == tmp.userId && this.packageName.equals(tmp.packageName);
            }
        }
    }

    public static OppoSplitWindowAppReader getInstance() {
        if (mIns == null) {
            synchronized (OppoSplitWindowAppReader.class) {
                if (mIns == null) {
                    mIns = new OppoSplitWindowAppReader();
                }
            }
        }
        return mIns;
    }

    private OppoSplitWindowAppReader() {
        initFile();
        this.mDataFileListener = new DataFileListener(SPLIT_WINDOW_APP_FILE_PATH);
        this.mDataFileListener.startWatching();
        getSplitWindowApp();
    }

    public void handleDynamicLog(boolean on) {
        DEBUG = on;
    }

    public boolean isInConfigList(String packageName) {
        boolean result = false;
        synchronized (this.mLock) {
            if (this.mAllowPackageName != null) {
                result = this.mAllowPackageName.contains(packageName);
            }
        }
        if (mHasUpdate) {
            return result;
        }
        return ALLOW_PKG.equals(packageName);
    }

    public boolean isInBlackList(String packageName) {
        boolean result = false;
        synchronized (this.mLock) {
            if (this.mForcePackageName != null) {
                result = this.mForcePackageName.contains(packageName);
            }
        }
        return result;
    }

    private void initFile() {
        File systemConfigPath = new File(SYSTEM_CONFIG_PATH);
        File splitAppFilePath = new File(SPLIT_WINDOW_APP_FILE_PATH);
        try {
            if (!systemConfigPath.exists()) {
                systemConfigPath.mkdirs();
            }
            if (!splitAppFilePath.exists()) {
                splitAppFilePath.createNewFile();
            }
        } catch (IOException e) {
            Log.e(TAG, "initFile failed!");
        }
    }

    private void getSplitWindowApp() {
        Throwable th;
        synchronized (this.mLock) {
            FileInputStream inputStream = null;
            try {
                File file = new File(SPLIT_WINDOW_APP_FILE_PATH);
                if (file.exists()) {
                    FileInputStream inputStream2 = new FileInputStream(file);
                    try {
                        this.mAllowPackageName.clear();
                        this.mForcePackageName.clear();
                        readAppListFromXML(inputStream2);
                        inputStream = inputStream2;
                    } catch (Exception e) {
                        inputStream = inputStream2;
                        try {
                            Log.e(TAG, "getSplitWindowApp() error!");
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e2) {
                                    Log.e(TAG, "getSplitWindowApp() close inputStream error!");
                                }
                            }
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e3) {
                                    Log.e(TAG, "getSplitWindowApp() close inputStream error!");
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        inputStream = inputStream2;
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e32) {
                                Log.e(TAG, "getSplitWindowApp() close inputStream error!");
                            }
                        }
                        throw th;
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "getSplitWindowApp() close inputStream error!");
                    }
                }
            } catch (Exception e5) {
                Log.e(TAG, "getSplitWindowApp() error!");
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e22) {
                        Log.e(TAG, "getSplitWindowApp() close inputStream error!");
                    }
                }
                return;
            }
        }
        return;
    }

    private void readAppListFromXML(FileInputStream stream) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, null);
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                String pName;
                if (eventType == 2 && "p".equals(parser.getName())) {
                    pName = parser.getAttributeValue(null, PKG_ATTR);
                    this.mAllowPackageName.add(pName);
                    mHasUpdate = true;
                    if (DEBUG) {
                        Log.i(TAG, "readAppList: " + pName);
                    }
                } else if (eventType == 2 && FORCE_PKG_NAME.equals(parser.getName())) {
                    pName = parser.getAttributeValue(null, PKG_ATTR);
                    this.mForcePackageName.add(pName);
                    mHasUpdate = true;
                    if (DEBUG) {
                        Log.i(TAG, "readForceAppList: " + pName);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "readAppListFromXML() error!");
        }
    }

    public static boolean isInBackStopFilterList(Context context, String pkg, ArraySet<Integer> pidsSet, int userId) {
        if (!OppoListManager.getInstance().getBackKeyKillSwitch()) {
            Log.i(TAG, "back_key switch off, skip " + pkg);
            return true;
        } else if (OppoListManager.getInstance().getRemoveTaskFilterPkgList(context).contains(pkg)) {
            Log.d(TAG, "back_key skip top app : " + pkg);
            return true;
        } else if (OppoListManager.getInstance().getKillRestartServicePkgList(context).contains(pkg)) {
            Log.d(TAG, "back_key skip top app : " + pkg);
            return true;
        } else if (OppoListManager.getInstance().getBackKeyFilterList().contains(pkg)) {
            Log.d(TAG, "back_key skip filter app : " + pkg);
            return true;
        } else if (isSystemApp(context, pkg)) {
            return true;
        } else {
            if (isDefaultInputMethod(context, pkg)) {
                Log.d(TAG, "back_key skip default inputmethod : " + pkg);
                return true;
            } else if (OppoAppStartupManager.getInstance().inCustomizeWhiteList(pkg)) {
                Log.d(TAG, "back_key skip custom whitelist : " + pkg);
                return true;
            } else if (OppoListManager.getInstance().getGlobalWhiteList(context).contains(pkg)) {
                Log.d(TAG, "back_key skip global whitelist : " + pkg);
                return true;
            } else if (OppoListManager.getInstance().isInstalledAppWidget(pkg)) {
                Log.d(TAG, "back_key skip appwidget list : " + pkg);
                return true;
            } else {
                String wallpaperPkg = OppoBPMHelper.getLivePackageForLiveWallPaper();
                if (wallpaperPkg != null && wallpaperPkg.equals(pkg)) {
                    Log.d(TAG, "back_key skip live wallpaper : " + pkg);
                    return true;
                } else if (isPkgWindowShowing(context, pkg)) {
                    Log.d(TAG, "back_key skip showing pkg : " + pkg);
                    return true;
                } else {
                    String[] audioPids = getActiveAudioPids(context);
                    if (!(audioPids == null || pidsSet == null)) {
                        for (String str : audioPids) {
                            int pid = -1;
                            try {
                                pid = Integer.parseInt(str);
                            } catch (NumberFormatException e) {
                            }
                            if (pidsSet.contains(Integer.valueOf(pid))) {
                                Log.d(TAG, "back_key skip audio pid : " + pkg + "/" + pid);
                                return true;
                            }
                        }
                    }
                    IPackageManager pm = AppGlobals.getPackageManager();
                    if (pm != null) {
                        boolean isCtsRunning = false;
                        try {
                            isCtsRunning = pm.isClosedSuperFirewall();
                        } catch (RemoteException e2) {
                        }
                        if (isCtsRunning) {
                            Log.d(TAG, "back_key skip all for full function mode");
                            return true;
                        }
                    }
                    ArrayList<UserIdPkg> tempList = getRecentLockUserList(context);
                    UserIdPkg uip = new UserIdPkg(pkg, userId);
                    if (tempList == null || !tempList.contains(uip)) {
                        return false;
                    }
                    return true;
                }
            }
        }
    }

    public static boolean isInTwoSecond() {
        long currentTime = System.currentTimeMillis();
        long sinceLastBackDown = currentTime - SystemProperties.getLong("debug.sys.oppo.keydowntime", 0);
        if (sinceLastBackDown < 0) {
            return false;
        }
        Log.i(TAG, "back_key sinceLastBackDown " + sinceLastBackDown);
        if (sinceLastBackDown < 2000) {
            return true;
        }
        long sinceLastBackUp = currentTime - SystemProperties.getLong("debug.sys.oppo.keyuptime", 0);
        if (sinceLastBackUp < 0) {
            return false;
        }
        Log.i(TAG, "back_key sinceLastBackUp " + sinceLastBackUp);
        if (sinceLastBackUp < 2000) {
            return true;
        }
        return false;
    }

    private static ArrayList<UserIdPkg> getRecentLockUserList(Context context) {
        Exception e;
        Throwable th;
        File file = new File(XMLPATH, LOCKED_APPS_FILE);
        FileInputStream stream = null;
        ArrayList<UserIdPkg> tempList = new ArrayList();
        try {
            FileInputStream stream2 = new FileInputStream(file);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream2, null);
                int type;
                do {
                    type = parser.next();
                    if (type == 2) {
                        if ("p".equals(parser.getName())) {
                            String pkg = parser.getAttributeValue(null, "att");
                            if (pkg != null) {
                                UserIdPkg userIdPkg = getUserIdPkgFromSpStr(pkg);
                                if (userIdPkg != null) {
                                    if (DEBUG) {
                                        Log.d(TAG, "lock list:  pkg=" + userIdPkg.packageName + ", userId=" + userIdPkg.userId);
                                    }
                                    tempList.add(userIdPkg);
                                }
                            }
                        }
                    }
                } while (type != 1);
                if (stream2 != null) {
                    try {
                        stream2.close();
                    } catch (Exception e2) {
                        Log.i(TAG, "Failed to close state FileInputStream " + e2);
                    }
                }
                stream = stream2;
            } catch (Exception e3) {
                e2 = e3;
                stream = stream2;
                try {
                    Log.i(TAG, "failed parsing " + e2);
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Exception e22) {
                            Log.i(TAG, "Failed to close state FileInputStream " + e22);
                        }
                    }
                    return tempList;
                } catch (Throwable th2) {
                    th = th2;
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Exception e222) {
                            Log.i(TAG, "Failed to close state FileInputStream " + e222);
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
                    } catch (Exception e2222) {
                        Log.i(TAG, "Failed to close state FileInputStream " + e2222);
                    }
                }
                throw th;
            }
        } catch (Exception e4) {
            e2222 = e4;
            Log.i(TAG, "failed parsing " + e2222);
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e22222) {
                    Log.i(TAG, "Failed to close state FileInputStream " + e22222);
                }
            }
            return tempList;
        }
        return tempList;
    }

    private static UserIdPkg getUserIdPkgFromSpStr(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        UserIdPkg userIdPkg = new UserIdPkg();
        if (str.contains("#")) {
            String[] splits = str.split("#");
            if (splits.length == 2) {
                userIdPkg.packageName = splits[0];
                int userId = 0;
                try {
                    userId = Integer.parseInt(splits[1]);
                } catch (NumberFormatException e) {
                }
                userIdPkg.userId = userId;
            } else {
                userIdPkg.userId = 0;
                userIdPkg.packageName = str;
            }
        } else {
            userIdPkg.userId = 0;
            userIdPkg.packageName = str;
        }
        return userIdPkg;
    }

    private static boolean isSystemApp(Context context, String packageName) {
        if (packageName != null) {
            try {
                ApplicationInfo info = context.getPackageManager().getPackageInfo(packageName, 0).applicationInfo;
                if (!(info == null || (info.flags & 1) == 0)) {
                    if (DEBUG) {
                        Log.d(TAG, "isSystemApp system app");
                    }
                    return true;
                }
            } catch (NameNotFoundException e) {
                if (DEBUG) {
                    Log.d(TAG, "isSystemApp NameNotFoundException, return false");
                }
                return false;
            }
        }
        return false;
    }

    public static boolean isDefaultInputMethod(Context context, String pkg) {
        String inputComponent = Secure.getString(context.getContentResolver(), "default_input_method");
        if (inputComponent == null || !inputComponent.contains(pkg)) {
            return false;
        }
        return true;
    }

    private static String[] getActiveAudioPids(Context context) {
        return getActiveAudioPids(((AudioManager) context.getSystemService("audio")).getParameters("get_pid"));
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String[] getActiveAudioPids(String pids) {
        if (pids == null || pids.length() == 0 || !pids.contains(":")) {
            return null;
        }
        return pids.split(":");
    }

    private static boolean isPkgWindowShowing(Context context, String pkg) {
        if (pkg != null) {
            try {
                int uid = context.getPackageManager().getPackageUid(pkg, 0);
                if (uid > 0) {
                    boolean shown = false;
                    try {
                        shown = new IOppoWindowManagerImpl().isWindowShownForUid(uid);
                    } catch (RemoteException e) {
                    }
                    return shown;
                }
            } catch (NameNotFoundException e2) {
                return false;
            }
        }
        return false;
    }
}
