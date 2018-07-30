package com.android.server.neuron;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocationManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.OppoAppStartupManager;
import com.oppo.neuron.NeoServiceProxy;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import oppo.util.OppoStatistics;

public class NsAppPreload {
    private static final int FREEZE_DELAY = 20000;
    private static final String TAG = "NeuronSystem";
    private static final boolean sDCSEnable = SystemProperties.getBoolean("persist.sys.ns_dcs", true);
    private final long MIN_MEMORY = 1048576;
    private Context mContext;
    Evaluation mEvaluation = new Evaluation();
    private Handler mHandler;
    long mLastPreloadTriggerTime = 0;
    private PackageManager mPackageManager;
    private PowerManager mPowerManager;
    private ActivityManagerService mService;
    private HandlerThread mThread;

    private static class AppPreloadConfig {
        private static final String CONFIG_FILE = "/system/etc/NsAppPreloadConfig";
        private static Set<String> mSupportedApps;

        private AppPreloadConfig() {
        }

        private static Set<String> getSupportApps() {
            Throwable th;
            if (mSupportedApps != null) {
                return mSupportedApps;
            }
            mSupportedApps = new TreeSet();
            BufferedReader br = null;
            try {
                BufferedReader br2 = new BufferedReader(new FileReader(CONFIG_FILE));
                while (true) {
                    try {
                        String line = br2.readLine();
                        if (line == null) {
                            break;
                        } else if (!("".equals(line) || line.startsWith("#"))) {
                            mSupportedApps.add(line);
                        }
                    } catch (Exception e) {
                        br = br2;
                        if (br != null) {
                            try {
                                br.close();
                            } catch (IOException e2) {
                            }
                        }
                        return mSupportedApps;
                    } catch (Throwable th2) {
                        th = th2;
                        br = br2;
                        if (br != null) {
                            try {
                                br.close();
                            } catch (IOException e3) {
                            }
                        }
                        throw th;
                    }
                }
                if (br2 != null) {
                    try {
                        br2.close();
                    } catch (IOException e4) {
                    }
                }
            } catch (Exception e5) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e22) {
                    }
                }
                return mSupportedApps;
            } catch (Throwable th3) {
                th = th3;
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e32) {
                    }
                }
                throw th;
            }
            return mSupportedApps;
        }
    }

    private static class Evaluation {
        private int mHitCount;
        private HashMap<String, String> mHitLog;
        private int mPreloadCount;
        private HashMap<String, String> mPreloadLog;

        /* synthetic */ Evaluation(Evaluation -this0) {
            this();
        }

        private Evaluation() {
            this.mPreloadCount = 0;
            this.mHitCount = 0;
            this.mPreloadLog = new HashMap();
            this.mHitLog = new HashMap();
        }
    }

    private class FreezeTask implements Runnable {
        private String mPackage;
        private int mUid;

        public FreezeTask(int uid, String pkg) {
            this.mUid = uid;
            this.mPackage = pkg;
        }

        public void run() {
            if (NsAppPreload.this.isAppInPreloadStack(this.mPackage)) {
                NsAppPreload.this.mService.mStackSupervisor.pauseStack(7, true);
            } else {
                Slog.d("NeuronSystem", "package is not in preload stack");
            }
        }
    }

    private class OppoDcsUploadReceiver extends BroadcastReceiver {
        private static final String ACTION_OPPO_DCS_PERIOD_UPLOAD = "android.intent.action.OPPO_DCS_PERIOD_UPLOAD";
        private static final String EVENT_ID_HIT = "app_preload_hit";
        private static final String EVENT_ID_PRELOAD = "app_preload";
        private static final String LOG_TAG = "neuronsystem";

        /* synthetic */ OppoDcsUploadReceiver(NsAppPreload this$0, OppoDcsUploadReceiver -this1) {
            this();
        }

        private OppoDcsUploadReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (ACTION_OPPO_DCS_PERIOD_UPLOAD.equals(intent.getAction())) {
                HashMap<String, String> preloadMap;
                HashMap<String, String> hitMap;
                synchronized (NsAppPreload.this.mEvaluation) {
                    preloadMap = (HashMap) NsAppPreload.this.mEvaluation.mPreloadLog.clone();
                    hitMap = (HashMap) NsAppPreload.this.mEvaluation.mHitLog.clone();
                    NsAppPreload.this.mEvaluation.mPreloadCount = 0;
                    NsAppPreload.this.mEvaluation.mHitCount = 0;
                    NsAppPreload.this.mEvaluation.mPreloadLog.clear();
                    NsAppPreload.this.mEvaluation.mHitLog.clear();
                }
                OppoStatistics.onCommon(NsAppPreload.this.mContext, LOG_TAG, EVENT_ID_PRELOAD, preloadMap, false);
                OppoStatistics.onCommon(NsAppPreload.this.mContext, LOG_TAG, EVENT_ID_HIT, hitMap, false);
                Slog.d("NeuronSystem", "OppoDcsUploadReceiver onReceive OppoStatistics");
            }
        }
    }

    private class PreloadTask implements Runnable {
        /* synthetic */ PreloadTask(NsAppPreload this$0, PreloadTask -this1) {
            this();
        }

        private PreloadTask() {
        }

        public void run() {
            if (NsAppPreload.this.isAppPreloadEnable()) {
                String[] apps = NeoServiceProxy.getInstance().appPreloadPredict();
                if (apps == null || apps.length == 0) {
                    Slog.d("NeuronSystem", "appPreloadPredict get empty result");
                    return;
                }
                for (String app : apps) {
                    if (AppPreloadConfig.getSupportApps().contains(app)) {
                        NsAppPreload.this.preload(app);
                    } else {
                        Slog.d("NeuronSystem", "NsAppPreload package:" + app + " do not support preload");
                    }
                }
                return;
            }
            Slog.d("NeuronSystem", "NsAppPreload is not enable");
        }
    }

    public NsAppPreload(Context context, ActivityManagerService ams, HandlerThread thread) {
        this.mContext = context;
        this.mService = ams;
        this.mThread = thread;
        this.mHandler = new Handler(thread.getLooper());
    }

    protected void systemReady() {
        Log.d("NeuronSystem", "NsAppPreload systemReady");
        this.mPackageManager = this.mContext.getPackageManager();
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        if (sDCSEnable) {
            this.mContext.registerReceiver(new OppoDcsUploadReceiver(), new IntentFilter("android.intent.action.OPPO_DCS_PERIOD_UPLOAD"));
        }
    }

    public void onEvent(int eventType, ContentValues contentValues) {
        if (eventType == 1) {
            long now = System.currentTimeMillis();
            if (now - this.mLastPreloadTriggerTime >= 20000) {
                this.mLastPreloadTriggerTime = now;
                autoPreload();
            }
        }
    }

    public void onPreloadedAppLuanch(String pkg) {
        if (sDCSEnable) {
            Slog.d("NeuronSystem", "App:" + pkg + " launch by user which preload");
            synchronized (this.mEvaluation) {
                Evaluation evaluation = this.mEvaluation;
                evaluation.mHitCount = evaluation.mHitCount + 1;
                this.mEvaluation.mHitLog.put(String.valueOf(System.currentTimeMillis()), pkg);
            }
        }
    }

    protected void autoPreload() {
        this.mHandler.postDelayed(new PreloadTask(), 1000);
    }

    private Intent getAppMainIntent(String pkg) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.addFlags(268435456);
        intent.setPackage(pkg);
        if (this.mPackageManager == null) {
            this.mPackageManager = this.mContext.getPackageManager();
        }
        List<ResolveInfo> infos = this.mPackageManager.queryIntentActivities(intent, 0);
        if (infos == null || infos.size() <= 0) {
            Slog.d("NeuronSystem", "getAppMainIntent: pkg" + pkg + " has no main activity");
            return null;
        }
        ResolveInfo info = (ResolveInfo) infos.get(0);
        intent.setClassName(pkg, info.activityInfo.name);
        Slog.d("NeuronSystem", "getAppMainIntent: pkg" + pkg + " classname " + info.activityInfo.name);
        return intent;
    }

    private boolean isAppPreloadEnable() {
        int nsProp = SystemProperties.getInt("persist.sys.neuron_system", 0);
        if ((nsProp & 1) == 0 || (nsProp & 64) == 0) {
            return false;
        }
        return true;
    }

    protected void preload(String pkgname) {
        if (!isAppPreloadEnable()) {
            Slog.d("NeuronSystem", "NsAppPreload is not enable");
        } else if (pkgname == null) {
            Slog.d("NeuronSystem", "NsAppPreload package is null ");
        } else if (getPreloadTaskNum() > 0) {
            Slog.d("NeuronSystem", "NsAppPreload only one app can be preload");
        } else if (isAppInPreloadStack(pkgname)) {
            Slog.d("NeuronSystem", "NsAppPreload package have been preload " + pkgname);
        } else if (isAppRunning(pkgname)) {
            Slog.d("NeuronSystem", "NsAppPreload package have been started " + pkgname);
        } else if (!isAvailMemory()) {
            Slog.d("NeuronSystem", "NsAppPreload have no availMem, please wait");
        } else if (isAvailCpu()) {
            Intent intent = getAppMainIntent(pkgname);
            if (intent != null) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchStackId(7);
                Slog.d("NeuronSystem", "NsAppPreload preload app " + pkgname);
                this.mContext.startActivity(intent, options.toBundle());
                try {
                    int uid = this.mPackageManager.getPackageUid(pkgname, 0);
                    if (uid >= 10000) {
                        SystemProperties.set("sys.nsaudio.disable" + uid, LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON);
                        Slog.d("NeuronSystem", "close audio of uid " + uid);
                        this.mHandler.postDelayed(new FreezeTask(uid, pkgname), 20000);
                        if (sDCSEnable) {
                            synchronized (this.mEvaluation) {
                                Evaluation evaluation = this.mEvaluation;
                                evaluation.mPreloadCount = evaluation.mPreloadCount + 1;
                                this.mEvaluation.mPreloadLog.put(String.valueOf(System.currentTimeMillis()), pkgname);
                            }
                        }
                    }
                } catch (NameNotFoundException e) {
                }
            }
        } else {
            Slog.d("NeuronSystem", "NsAppPreload have no availCpu, please wait");
        }
    }

    private boolean isAvailMemory() {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY);
        MemoryInfo mi = new MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem > 1048576;
    }

    private boolean isAvailCpu() {
        Throwable th;
        String line = "";
        BufferedReader bufferedReader = null;
        try {
            BufferedReader bufferedReader2 = new BufferedReader(new FileReader("/proc/stat"), 1024);
            try {
                line = bufferedReader2.readLine();
                if (bufferedReader2 != null) {
                    try {
                        bufferedReader2.close();
                    } catch (IOException e) {
                    }
                }
                String[] nums = line.split(" ");
                if (nums.length < 9 || ("cpu".equals(nums[0]) ^ 1) != 0) {
                    Slog.d("NeuronSystem", "/proc/stat line content error" + line);
                    return false;
                }
                try {
                    int user = Integer.parseInt(nums[2].trim());
                    int nice = Integer.parseInt(nums[3].trim());
                    int system = Integer.parseInt(nums[4].trim());
                    int idle = Integer.parseInt(nums[5].trim());
                    int iowait = Integer.parseInt(nums[6].trim());
                    float usage = (float) ((((((user + nice) + system) + idle) + iowait) + Integer.parseInt(nums[7].trim())) + Integer.parseInt(nums[8].trim()));
                    usage = (usage - ((float) idle)) / usage;
                    Slog.d("NeuronSystem", "cpu usage " + usage);
                    return usage < 0.5f;
                } catch (NumberFormatException e2) {
                    Slog.d("NeuronSystem", "in isAvailCpu Exception " + e2.toString());
                    return false;
                }
            } catch (Exception e3) {
                bufferedReader = bufferedReader2;
            } catch (Throwable th2) {
                th = th2;
                bufferedReader = bufferedReader2;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            try {
                Slog.d("NeuronSystem", "read file /proc/stat error");
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e6) {
                    }
                }
                return false;
            } catch (Throwable th3) {
                th = th3;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e42) {
                    }
                }
                throw th;
            }
        }
    }

    private int getPreloadTaskNum() {
        StackInfo stackInfo = this.mService.getStackInfo(7);
        if (stackInfo == null) {
            return 0;
        }
        return stackInfo.taskNames.length;
    }

    private boolean isAppInPreloadStack(String pkg) {
        StackInfo stackInfo = this.mService.getStackInfo(7);
        if (stackInfo == null) {
            return false;
        }
        for (String taskName : stackInfo.taskNames) {
            if (taskName != null && taskName.indexOf(pkg) >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isAppRunning(String targetPackage) {
        for (RunningAppProcessInfo info : this.mService.getRunningAppProcesses()) {
            for (String pkg : info.pkgList) {
                if (targetPackage.equals(pkg)) {
                    return true;
                }
            }
        }
        return false;
    }
}
