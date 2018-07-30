package com.android.server.power;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.OppoActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageManager.Stub;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import android.view.IOppoWindowManagerImpl;
import com.android.server.am.OppoAppStartupManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/* compiled from: OppoWakeLockCheck */
class CommonUtil {
    private static final String ATAG = "OppoWakeLockCheck";
    private ActivityManager mActivityManager = null;
    private AudioManager mAudioManager = null;
    private final Context mContext;
    private IOppoWindowManagerImpl mIOppoWindowManagerImpl;
    private IPackageManager mPkm = null;

    public CommonUtil(Context context) {
        this.mContext = context;
        this.mIOppoWindowManagerImpl = new IOppoWindowManagerImpl();
    }

    public String[] getActiveAudioPids() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
            if (this.mAudioManager == null) {
                return null;
            }
        }
        return getActiveAudioPids(this.mAudioManager.getParameters("get_pid"));
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String[] getActiveAudioPids(String pids) {
        if (pids == null || pids.length() == 0 || !pids.contains(":")) {
            return null;
        }
        return pids.split(":");
    }

    public RunningAppProcessInfo getProcessForPid(String pid) {
        if (this.mActivityManager == null) {
            this.mActivityManager = (ActivityManager) this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY);
            if (this.mActivityManager == null) {
                return null;
            }
        }
        for (RunningAppProcessInfo processInfo : this.mActivityManager.getRunningAppProcesses()) {
            if (processInfo.pid == Integer.valueOf(pid).intValue()) {
                return processInfo;
            }
        }
        return null;
    }

    public String getPkgNameForUid(int uid) {
        String pkgName = null;
        try {
            return getIPackageManager().getNameForUid(uid);
        } catch (Exception e) {
            Slog.w("OppoWakeLockCheck", "getNameForUid exception");
            return pkgName;
        }
    }

    public String[] getPackagesForUid(int uid) {
        String[] packages = null;
        try {
            return getIPackageManager().getPackagesForUid(uid);
        } catch (Exception e) {
            Slog.w("OppoWakeLockCheck", "getPackagesForUid exception");
            return packages;
        }
    }

    public String getAppLabel(String packageName, int uid) {
        String str = "";
        PackageManager pm = this.mContext.getPackageManager();
        ApplicationInfo app = null;
        if (uid < 10000 || pm == null) {
            return str;
        }
        try {
            app = pm.getApplicationInfo(packageName, 128);
        } catch (NameNotFoundException e) {
        }
        if (app != null) {
            str = str + pm.getApplicationLabel(app).toString();
        } else {
            Slog.e("OppoWakeLockCheck", "error happened when  getApplicationInfo from " + packageName);
        }
        return str;
    }

    public String[] getPkgsForUid(int uid) {
        String[] pkgName = null;
        try {
            return getIPackageManager().getPackagesForUid(uid);
        } catch (Exception e) {
            Slog.w("OppoWakeLockCheck", "getPackagesForUid exception");
            return pkgName;
        }
    }

    public String getForegroundPackage() {
        ComponentName cn;
        try {
            cn = new OppoActivityManager().getTopActivityComponentName();
        } catch (Exception e) {
            Slog.w("OppoWakeLockCheck", "getTopActivityComponentName exception");
            cn = null;
        }
        if (cn != null) {
            return cn.getPackageName();
        }
        return null;
    }

    public List<String> getAllTopPkgName() {
        try {
            return ((ActivityManager) this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY)).getAllTopPkgName();
        } catch (Exception e) {
            Slog.w("OppoWakeLockCheck", "getAllTopPkgName exception");
            return null;
        }
    }

    public int getUidForPkgName(String pkgName) {
        try {
            return getIPackageManager().getPackageUid(pkgName, 8192, 0);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public String getTopAppName() {
        if (this.mActivityManager == null) {
            this.mActivityManager = (ActivityManager) this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY);
            if (this.mActivityManager == null) {
                return null;
            }
        }
        ComponentName cn = this.mActivityManager.getTopAppName();
        return cn != null ? cn.getPackageName() : "";
    }

    public String getSurfceLayers() {
        Throwable th;
        String layers = "";
        BufferedReader reader = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("dumpsys SurfaceFlinger --list");
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (true) {
                try {
                    String lineText = reader2.readLine();
                    if (lineText == null) {
                        break;
                    } else if (!(lineText.equals("FocusedStackFrame") || lineText.equals("DimLayer") || lineText.equals("TickerPanel") || lineText.equals("Magnification Overlay") || lineText.equals("AssertTip"))) {
                        layers = layers.concat("  " + lineText);
                    }
                } catch (IOException e) {
                    reader = reader2;
                    try {
                        Slog.e("OppoWakeLockCheck", "dumpsys SurfaceFlinger --list IOException");
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e2) {
                                Slog.e("OppoWakeLockCheck", "failed closing reader");
                            }
                        }
                        if (process != null) {
                            process.destroy();
                        }
                        return layers;
                    } catch (Throwable th2) {
                        th = th2;
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e3) {
                                Slog.e("OppoWakeLockCheck", "failed closing reader");
                            }
                        }
                        if (process != null) {
                            process.destroy();
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    reader = reader2;
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e32) {
                            Slog.e("OppoWakeLockCheck", "failed closing reader");
                        }
                    }
                    if (process != null) {
                        process.destroy();
                    }
                    throw th;
                }
            }
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e4) {
                    Slog.e("OppoWakeLockCheck", "failed closing reader");
                }
            }
            if (process != null) {
                process.destroy();
            }
            reader = reader2;
        } catch (IOException e5) {
            Slog.e("OppoWakeLockCheck", "dumpsys SurfaceFlinger --list IOException");
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e22) {
                    Slog.e("OppoWakeLockCheck", "failed closing reader");
                }
            }
            if (process != null) {
                process.destroy();
            }
            return layers;
        }
        return layers;
    }

    private IPackageManager getIPackageManager() {
        if (this.mPkm != null) {
            return this.mPkm;
        }
        this.mPkm = Stub.asInterface(ServiceManager.getService("package"));
        return this.mPkm;
    }

    public boolean isWindowShownForUid(int uid) {
        boolean shown = true;
        try {
            return this.mIOppoWindowManagerImpl.isWindowShownForUid(uid);
        } catch (RemoteException e) {
            return shown;
        }
    }
}
