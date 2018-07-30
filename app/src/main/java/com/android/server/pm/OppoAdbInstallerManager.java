package com.android.server.pm;

import android.app.ActivityManagerNative;
import android.content.Intent;
import android.content.pm.IPackageInstallObserver2;
import android.os.FileObserver;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Xml;
import com.android.server.am.OppoCrashClearManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class OppoAdbInstallerManager {
    private static final String ADB_INSTALLER_STATUS_PATH = "/data/system/config/adb_installer_status.xml";
    public static boolean DEBUG_DETAIL = SystemProperties.getBoolean("persist.sys.assert.panic", false);
    private static final String SYSTEM_CONFIG_PATH = "/data/system/config";
    public static final String TAG = "OppoAdbInstallerManager";
    private static OppoAdbInstallerManager sOppoAdbInstallerManager = null;
    private FileObserverPolicy mAdbInstallerFileObserver = null;
    private PackageManagerService mPms = null;
    private boolean mSwitch = true;
    private boolean mSystemReady = false;
    public String mVersion = SystemProperties.get("ro.oppo.version", "CN");

    private class FileObserverPolicy extends FileObserver {
        private String mFocusPath;

        public FileObserverPolicy(String path) {
            super(path, 8);
            this.mFocusPath = path;
        }

        public void onEvent(int event, String path) {
            if (event == 8 && this.mFocusPath.equals(OppoAdbInstallerManager.ADB_INSTALLER_STATUS_PATH)) {
                Log.i("OppoAdbInstallerManager", "onEvent: mFocusPath = ADB_INSTALLER_STATUS_PATH");
                OppoAdbInstallerManager.this.readAdbInstallerFile();
            }
        }
    }

    public static final OppoAdbInstallerManager getInstance() {
        if (sOppoAdbInstallerManager == null) {
            sOppoAdbInstallerManager = new OppoAdbInstallerManager();
        }
        return sOppoAdbInstallerManager;
    }

    public void init(PackageManagerService pms) {
        this.mPms = pms;
        initFile();
        readAdbInstallerFile();
    }

    public void handForAdbSessionInstallerCancel(String packageName) {
        if (DEBUG_DETAIL) {
            Log.d("OppoAdbInstallerManager", "handForAdbSessionInstallerCancel!!!");
        }
        if (this.mPms == null) {
            Log.e("OppoAdbInstallerManager", "handForAdbSessionInstallerCancel mPms = null !");
            return;
        }
        synchronized (this.mPms.mOppoPackageInstallerList) {
            int i = 0;
            while (i < this.mPms.mOppoPackageInstallerList.size()) {
                if (((OppoAdbInstallerEntry) this.mPms.mOppoPackageInstallerList.get(i)).mPackageName.equals(packageName)) {
                    if (DEBUG_DETAIL) {
                        Log.d("OppoAdbInstallerManager", "handAdbInstallCancel packageName == " + packageName);
                    }
                    try {
                        if (((OppoAdbInstallerEntry) this.mPms.mOppoPackageInstallerList.get(i)).mObserver != null) {
                            ((OppoAdbInstallerEntry) this.mPms.mOppoPackageInstallerList.get(i)).mObserver.onPackageInstalled(packageName, -99, null, null);
                        }
                    } catch (RemoteException e) {
                    }
                    this.mPms.mOppoPackageInstallerList.remove(i);
                    i--;
                }
                i++;
            }
        }
    }

    public boolean handleForAdbSessionInstaller(String packageName, String apkPath, IPackageInstallObserver2 observer, int installFlags) {
        if (!this.mVersion.equals("CN")) {
            Log.d("OppoAdbInstallerManager", "the version isn't CN!");
            return false;
        } else if (this.mSwitch) {
            if (!this.mSystemReady) {
                this.mSystemReady = ActivityManagerNative.isSystemReady();
                if (!this.mSystemReady) {
                    Log.d("OppoAdbInstallerManager", "System is not ready!");
                    return false;
                }
            }
            if (this.mPms == null) {
                Log.d("OppoAdbInstallerManager", "handleForAdbSessionInstaller mPms = null !");
                return false;
            } else if (packageName == null || apkPath == null) {
                Log.d("OppoAdbInstallerManager", "handleForAdbSessionInstaller packageName or apkPath = null !");
                return false;
            } else {
                if (DEBUG_DETAIL) {
                    Log.d("OppoAdbInstallerManager", "installStage INSTALL_FROM_ADB !");
                }
                if (!new File(apkPath).exists() && !apkPath.startsWith("/storage") && !apkPath.startsWith("/sdcard")) {
                    Log.d("OppoAdbInstallerManager", apkPath + "  file is not exists!");
                    return false;
                } else if (ColorPackageManagerHelper.isCtsAppFileByPkgName(packageName)) {
                    if (DEBUG_DETAIL) {
                        Log.d("OppoAdbInstallerManager", "skip adb intercept for " + packageName);
                    }
                    return false;
                } else if (packageName.equals("com.android.cts.priv.ctsshim") || packageName.equals("com.android.cts.ctsshim")) {
                    if (DEBUG_DETAIL) {
                        Log.d("OppoAdbInstallerManager", "skip adb intercept for " + packageName);
                    }
                    return false;
                } else {
                    if (DEBUG_DETAIL) {
                        Log.d("OppoAdbInstallerManager", "call installer for " + packageName);
                    }
                    Intent intent = new Intent("oppo.intent.action.OPPO_INSTALL_FROM_ADB");
                    intent.addFlags(16777216);
                    intent.putExtra("apkPath", apkPath);
                    intent.putExtra("installFlags", installFlags);
                    this.mPms.mContext.sendBroadcast(intent);
                    OppoAdbInstallerEntry oaie = OppoAdbInstallerEntry.Builder(apkPath, observer, packageName);
                    synchronized (this.mPms.mOppoPackageInstallerList) {
                        this.mPms.mOppoPackageInstallerList.add(oaie);
                    }
                    return true;
                }
            }
        } else {
            Log.d("OppoAdbInstallerManager", "handleForAdbSessionInstaller mSwitch = false !");
            return false;
        }
    }

    public void handleForAdbSessionInstallerObserver(String packageName, int ret) {
        if (this.mPms == null) {
            Log.d("OppoAdbInstallerManager", "handleForAdbSessionInstallerObserver mPms = null !");
        } else if (packageName == null) {
            Log.d("OppoAdbInstallerManager", "handleForAdbSessionInstallerObserver packageName = null !");
        } else {
            if (DEBUG_DETAIL) {
                Log.d("OppoAdbInstallerManager", "handleForAdbInstallerObserver packageName = " + packageName);
            }
            synchronized (this.mPms.mOppoPackageInstallerList) {
                int i = 0;
                while (i < this.mPms.mOppoPackageInstallerList.size()) {
                    if (((OppoAdbInstallerEntry) this.mPms.mOppoPackageInstallerList.get(i)).mPackageName.equals(packageName)) {
                        try {
                            if (((OppoAdbInstallerEntry) this.mPms.mOppoPackageInstallerList.get(i)).mObserver != null) {
                                ((OppoAdbInstallerEntry) this.mPms.mOppoPackageInstallerList.get(i)).mObserver.onPackageInstalled(packageName, ret, null, null);
                            }
                        } catch (RemoteException e) {
                        }
                        this.mPms.mOppoPackageInstallerList.remove(i);
                        i--;
                    }
                    i++;
                }
            }
        }
    }

    private void initFile() {
        Log.i("OppoAdbInstallerManager", "initFile start");
        File systemConfigPath = new File(SYSTEM_CONFIG_PATH);
        File adbInstallerPath = new File(ADB_INSTALLER_STATUS_PATH);
        try {
            if (!systemConfigPath.exists()) {
                systemConfigPath.mkdirs();
            }
            if (!adbInstallerPath.exists()) {
                adbInstallerPath.createNewFile();
                saveAdbInstallerStatusFile(true);
            }
        } catch (IOException e) {
            Log.e("OppoAdbInstallerManager", "initFile failed!!!");
            e.printStackTrace();
        }
        this.mAdbInstallerFileObserver = new FileObserverPolicy(ADB_INSTALLER_STATUS_PATH);
        this.mAdbInstallerFileObserver.startWatching();
    }

    public void readAdbInstallerFile() {
        File adbInstallerStatusFile = new File(ADB_INSTALLER_STATUS_PATH);
        if (adbInstallerStatusFile.exists()) {
            this.mSwitch = readFromStatusFileLocked(adbInstallerStatusFile);
            return;
        }
        this.mSwitch = true;
        initFile();
    }

    private boolean readFromStatusFileLocked(File adbInstallerStatusFile) {
        IOException e;
        NullPointerException e2;
        NumberFormatException e3;
        XmlPullParserException e4;
        IndexOutOfBoundsException e5;
        Throwable th;
        Log.i("OppoAdbInstallerManager", "readFromStatusFileLocked!!!");
        FileInputStream stream = null;
        boolean z = true;
        try {
            FileInputStream stream2 = new FileInputStream(adbInstallerStatusFile);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream2, null);
                int type;
                do {
                    type = parser.next();
                    if (type == 2) {
                        if (OppoCrashClearManager.CRASH_CLEAR_NAME.equals(parser.getName())) {
                            String str = parser.getAttributeValue(null, "att");
                            if (str != null) {
                                Log.i("OppoAdbInstallerManager", "readFromStatusFileLocked  status == " + str);
                                z = Boolean.parseBoolean(str);
                            }
                        }
                    }
                } while (type != 1);
                if (stream2 != null) {
                    try {
                        stream2.close();
                    } catch (IOException e6) {
                        Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e6);
                    }
                }
                stream = stream2;
            } catch (NullPointerException e7) {
                e2 = e7;
                stream = stream2;
                Log.e("OppoAdbInstallerManager", "failed parsing ", e2);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e62) {
                        Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e62);
                    }
                }
                return z;
            } catch (NumberFormatException e8) {
                e3 = e8;
                stream = stream2;
                Log.e("OppoAdbInstallerManager", "failed parsing ", e3);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e622) {
                        Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e622);
                    }
                }
                return z;
            } catch (XmlPullParserException e9) {
                e4 = e9;
                stream = stream2;
                Log.e("OppoAdbInstallerManager", "failed parsing ", e4);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e6222) {
                        Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e6222);
                    }
                }
                return z;
            } catch (IOException e10) {
                e6222 = e10;
                stream = stream2;
                Log.e("OppoAdbInstallerManager", "failed IOException ", e6222);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e62222) {
                        Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e62222);
                    }
                }
                return z;
            } catch (IndexOutOfBoundsException e11) {
                e5 = e11;
                stream = stream2;
                try {
                    Log.e("OppoAdbInstallerManager", "failed parsing ", e5);
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e622222) {
                            Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e622222);
                        }
                    }
                    return z;
                } catch (Throwable th2) {
                    th = th2;
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e6222222) {
                            Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e6222222);
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
                    } catch (IOException e62222222) {
                        Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e62222222);
                    }
                }
                throw th;
            }
        } catch (NullPointerException e12) {
            e2 = e12;
            Log.e("OppoAdbInstallerManager", "failed parsing ", e2);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e622222222) {
                    Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e622222222);
                }
            }
            return z;
        } catch (NumberFormatException e13) {
            e3 = e13;
            Log.e("OppoAdbInstallerManager", "failed parsing ", e3);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e6222222222) {
                    Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e6222222222);
                }
            }
            return z;
        } catch (XmlPullParserException e14) {
            e4 = e14;
            Log.e("OppoAdbInstallerManager", "failed parsing ", e4);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e62222222222) {
                    Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e62222222222);
                }
            }
            return z;
        } catch (IOException e15) {
            e62222222222 = e15;
            Log.e("OppoAdbInstallerManager", "failed IOException ", e62222222222);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e622222222222) {
                    Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e622222222222);
                }
            }
            return z;
        } catch (IndexOutOfBoundsException e16) {
            e5 = e16;
            Log.e("OppoAdbInstallerManager", "failed parsing ", e5);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e6222222222222) {
                    Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e6222222222222);
                }
            }
            return z;
        }
        return z;
    }

    public void saveAdbInstallerStatusFile(boolean status) {
        if (DEBUG_DETAIL) {
            Log.i("OppoAdbInstallerManager", "saveAdbInstallerStatusFile start");
        }
        writeToStatusFileLocked(new File(ADB_INSTALLER_STATUS_PATH), status);
    }

    private void writeToStatusFileLocked(File adbInstallerStatusFile, boolean status) {
        IOException e;
        Throwable th;
        if (DEBUG_DETAIL) {
            Log.i("OppoAdbInstallerManager", "writeToStatusFileLocked!!!");
        }
        FileOutputStream stream = null;
        try {
            FileOutputStream stream2 = new FileOutputStream(adbInstallerStatusFile);
            try {
                XmlSerializer out = Xml.newSerializer();
                out.setOutput(stream2, "utf-8");
                out.startDocument(null, Boolean.valueOf(true));
                out.startTag(null, "gs");
                String str = String.valueOf(status);
                if (str != null) {
                    out.startTag(null, OppoCrashClearManager.CRASH_CLEAR_NAME);
                    out.attribute(null, "att", str);
                    out.endTag(null, OppoCrashClearManager.CRASH_CLEAR_NAME);
                }
                out.endTag(null, "gs");
                out.endDocument();
                if (stream2 != null) {
                    try {
                        stream2.close();
                    } catch (IOException e2) {
                        Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e2);
                    }
                }
                stream = stream2;
            } catch (IOException e3) {
                e2 = e3;
                stream = stream2;
                try {
                    Log.e("OppoAdbInstallerManager", "Failed to write IOException: " + e2);
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e22) {
                            Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e22);
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e222) {
                            Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e222);
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
                        Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e2222);
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
            e2222 = e4;
            Log.e("OppoAdbInstallerManager", "Failed to write IOException: " + e2222);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e22222) {
                    Log.e("OppoAdbInstallerManager", "Failed to close state FileInputStream " + e22222);
                }
            }
        }
    }
}
