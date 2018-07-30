package com.oppo.hypnus;

import android.app.AppGlobals;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class Hypnus {
    public static final String ACTIONINFO = "/sys/kernel/hypnus/action_info";
    public static final int ACTION_AGAINST_IDLE = 16;
    public static final int ACTION_ANIMATION = 11;
    public static final int ACTION_AUDIO_PLAYBACK = 2;
    public static final int ACTION_BURST_ANR = 19;
    public static final int ACTION_BURST_BM = 20;
    public static final int ACTION_BURST_GC = 17;
    public static final int ACTION_BURST_LM = 18;
    public static final int ACTION_DOWNLOAD = 3;
    public static final int ACTION_IDLE = 0;
    public static final int ACTION_INSTALLATION = 15;
    public static final int ACTION_IO = 12;
    public static final int ACTION_LAUNCH = 13;
    public static final int ACTION_NONE = 1;
    public static final int ACTION_PERFD = 99;
    public static final int ACTION_PREVIEW = 5;
    public static final int ACTION_PRE_LAUNCH = 10;
    public static final int ACTION_RESUME = 9;
    public static final int ACTION_SCROLLING_H = 8;
    public static final int ACTION_SCROLLING_V = 7;
    public static final int ACTION_SNAPSHOT = 14;
    public static final int ACTION_VIDEO_ENCODING = 6;
    public static final int ACTION_VIDEO_PLAYBACK = 4;
    public static final int BURST_TYPE_GC = 1;
    public static final int BURST_TYPE_LM = 2;
    public static final Boolean HYPNUS_STATICS_ON = Boolean.valueOf(SystemProperties.getBoolean("persist.sys.hypnus.statics", false));
    public static final String NOTIFICATIONINFO = "/sys/kernel/hypnus/notification_info";
    public static final String SCENEINFO = "/sys/kernel/hypnus/scene_info";
    public static final int SCENE_BENCHMARK = 6;
    public static final int SCENE_BOOT = 13;
    public static final int SCENE_BROWSER = 3;
    public static final int SCENE_CAMERA = 2;
    public static final int SCENE_EBOOK = 11;
    public static final int SCENE_GALLERY = 9;
    public static final int SCENE_HEAVY_GAME = 5;
    public static final int SCENE_IO = 14;
    public static final int SCENE_LAUNCHER = 12;
    public static final int SCENE_LIGHT_GAME = 4;
    public static final int SCENE_LISTVIEW = 8;
    public static final int SCENE_MUSIC = 1;
    public static final int SCENE_NAVIGATION = 10;
    public static final int SCENE_NORMAL = 0;
    public static final int SCENE_SUPERAPP = 15;
    public static final int SCENE_VIDEO = 7;
    private static final String TAG = "Hypnus";
    public static final int TIME_ANIMATION = 600;
    public static final int TIME_ANIMATION_100 = 100;
    public static final int TIME_ANIMATION_300 = 300;
    public static final int TIME_ANIMATION_500 = 500;
    public static final int TIME_BM = 50;
    public static final int TIME_BURST = 199;
    public static final int TIME_DEX2OAT = 20000;
    public static final int TIME_INSTALLATION = 30000;
    public static final int TIME_LAUNCH = 5000;
    public static final int TIME_MAX = 600000;
    public static final int TIME_PRE_LAUNCH = 150;
    public static final int TIME_SERVICE_DELAY = 100000;
    public static final String VERSION = "M08";
    public static final String VERSIONINFO = "/sys/kernel/hypnus/version";
    private static boolean mHypnusOK = false;
    private static String mName;
    private static int mPid;
    private static String mVersion;
    private static Hypnus sHypnus;
    public static volatile HashMap<String, Long> staticsCount = new HashMap();
    private boolean DEBUG = false;
    private int mCount = 0;
    private boolean mInIO = false;

    public Hypnus() {
        IOException e;
        FileNotFoundException e2;
        Throwable th;
        if (SystemProperties.getBoolean("persist.debug.hypnus", false)) {
            this.DEBUG = true;
        }
        FileInputStream in = null;
        try {
            FileInputStream in2 = new FileInputStream(new File(VERSIONINFO));
            try {
                byte[] b = new byte[3];
                in2.read(b);
                mVersion = new String(b);
                if (in2 != null) {
                    try {
                        in2.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                in = in2;
            } catch (FileNotFoundException e4) {
                e2 = e4;
                in = in2;
                e2.printStackTrace();
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e32) {
                        e32.printStackTrace();
                    }
                }
                if (mVersion == null) {
                    mHypnusOK = true;
                    if (this.DEBUG) {
                        Log.d(TAG, "Hypnus framework module initialized, version:" + mVersion);
                    }
                    if (!mVersion.equals(VERSION)) {
                        Log.i(TAG, "Framework: M08 module: " + mVersion);
                    }
                }
                Log.w(TAG, "Hypnus version is null, is the module there?");
                mHypnusOK = false;
                return;
            } catch (IOException e5) {
                e32 = e5;
                in = in2;
                try {
                    e32.printStackTrace();
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e322) {
                            e322.printStackTrace();
                        }
                    }
                    if (mVersion == null) {
                        Log.w(TAG, "Hypnus version is null, is the module there?");
                        mHypnusOK = false;
                        return;
                    }
                    mHypnusOK = true;
                    if (this.DEBUG) {
                        Log.d(TAG, "Hypnus framework module initialized, version:" + mVersion);
                    }
                    if (!mVersion.equals(VERSION)) {
                        Log.i(TAG, "Framework: M08 module: " + mVersion);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e3222) {
                            e3222.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                in = in2;
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e32222) {
                        e32222.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e6) {
            e2 = e6;
            e2.printStackTrace();
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e322222) {
                    e322222.printStackTrace();
                }
            }
            if (mVersion == null) {
                mHypnusOK = true;
                if (this.DEBUG) {
                    Log.d(TAG, "Hypnus framework module initialized, version:" + mVersion);
                }
                if (!mVersion.equals(VERSION)) {
                    Log.i(TAG, "Framework: M08 module: " + mVersion);
                }
            }
            Log.w(TAG, "Hypnus version is null, is the module there?");
            mHypnusOK = false;
            return;
        } catch (IOException e7) {
            e322222 = e7;
            e322222.printStackTrace();
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3222222) {
                    e3222222.printStackTrace();
                }
            }
            if (mVersion == null) {
                Log.w(TAG, "Hypnus version is null, is the module there?");
                mHypnusOK = false;
                return;
            }
            mHypnusOK = true;
            if (this.DEBUG) {
                Log.d(TAG, "Hypnus framework module initialized, version:" + mVersion);
            }
            if (!mVersion.equals(VERSION)) {
                Log.i(TAG, "Framework: M08 module: " + mVersion);
            }
        }
        if (mVersion == null) {
            Log.w(TAG, "Hypnus version is null, is the module there?");
            mHypnusOK = false;
            return;
        }
        mHypnusOK = true;
        if (this.DEBUG) {
            Log.d(TAG, "Hypnus framework module initialized, version:" + mVersion);
        }
        if (!mVersion.equals(VERSION)) {
            Log.i(TAG, "Framework: M08 module: " + mVersion);
        }
    }

    public void hypnusSetNotification(int msg_src, int msg_type) {
        hypnusSetNotification(msg_src, msg_type, 0, 0, 0, 0);
    }

    public void hypnusSetNotification(int msg_src, int msg_type, long msg_time, int pid, int v0, int v1) {
        Throwable th;
        if (msg_time == 0) {
            msg_time = System.nanoTime();
        }
        if (pid == 0) {
            pid = Process.myPid();
        }
        File mNotificationInfoFile = new File(NOTIFICATIONINFO);
        String info = String.format("%d %d %d %d %d %d", new Object[]{Integer.valueOf(msg_src), Long.valueOf(msg_time), Integer.valueOf(msg_type), Integer.valueOf(pid), Integer.valueOf(v0), Integer.valueOf(v1)});
        if (mNotificationInfoFile.canWrite()) {
            FileOutputStream out = null;
            try {
                FileOutputStream out2 = new FileOutputStream(mNotificationInfoFile);
                try {
                    out2.write(info.getBytes());
                    if (out2 != null) {
                        try {
                            out2.close();
                        } catch (IOException e) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                } catch (FileNotFoundException e2) {
                    out = out2;
                    if (this.DEBUG) {
                        Log.d(TAG, "FileNotFoundException");
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e3) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                    if (this.DEBUG) {
                        Log.d(TAG, "hypnusSetNotification:" + info);
                    }
                } catch (IOException e4) {
                    out = out2;
                    try {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e5) {
                                if (this.DEBUG) {
                                    Log.d(TAG, "IOException");
                                }
                            }
                        }
                        if (this.DEBUG) {
                            Log.d(TAG, "hypnusSetNotification:" + info);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e6) {
                                if (this.DEBUG) {
                                    Log.d(TAG, "IOException");
                                }
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    out = out2;
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e62) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e7) {
                if (this.DEBUG) {
                    Log.d(TAG, "FileNotFoundException");
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e32) {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                    }
                }
                if (this.DEBUG) {
                    Log.d(TAG, "hypnusSetNotification:" + info);
                }
            } catch (IOException e8) {
                if (this.DEBUG) {
                    Log.d(TAG, "IOException");
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e52) {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                    }
                }
                if (this.DEBUG) {
                    Log.d(TAG, "hypnusSetNotification:" + info);
                }
            }
        }
        if (this.DEBUG) {
            Log.d(TAG, "hypnusSetNotification:" + info);
        }
    }

    public void hypnusSetScene(int pid, String processName) {
        Throwable th;
        if (mPid == pid) {
            if (this.DEBUG) {
                Log.d(TAG, "Same PID ignore");
            }
            return;
        }
        mPid = pid;
        File mSceneInfoFile = new File(SCENEINFO);
        String info = String.format("%d %d ", new Object[]{Integer.valueOf(0), Integer.valueOf(pid)});
        mName = processName;
        info = info + mName;
        if (mSceneInfoFile.canWrite()) {
            FileOutputStream out = null;
            try {
                FileOutputStream out2 = new FileOutputStream(mSceneInfoFile);
                try {
                    out2.write(info.getBytes());
                    if (out2 != null) {
                        try {
                            out2.close();
                        } catch (IOException e) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                } catch (FileNotFoundException e2) {
                    out = out2;
                    if (this.DEBUG) {
                        Log.d(TAG, "FileNotFoundException");
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e3) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                    if (this.DEBUG) {
                        Log.d(TAG, "hypnusSetScene:" + info);
                    }
                } catch (IOException e4) {
                    out = out2;
                    try {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e5) {
                                if (this.DEBUG) {
                                    Log.d(TAG, "IOException");
                                }
                            }
                        }
                        if (this.DEBUG) {
                            Log.d(TAG, "hypnusSetScene:" + info);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e6) {
                                if (this.DEBUG) {
                                    Log.d(TAG, "IOException");
                                }
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    out = out2;
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e62) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e7) {
                if (this.DEBUG) {
                    Log.d(TAG, "FileNotFoundException");
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e32) {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                    }
                }
                if (this.DEBUG) {
                    Log.d(TAG, "hypnusSetScene:" + info);
                }
            } catch (IOException e8) {
                if (this.DEBUG) {
                    Log.d(TAG, "IOException");
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e52) {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                    }
                }
                if (this.DEBUG) {
                    Log.d(TAG, "hypnusSetScene:" + info);
                }
            }
        }
        if (this.DEBUG) {
            Log.d(TAG, "hypnusSetScene:" + info);
        }
    }

    public void hypnusSendBootComplete() {
        Throwable th;
        File mSceneInfoFile = new File(SCENEINFO);
        String info = String.format("%d %d 0", new Object[]{Integer.valueOf(13), Integer.valueOf(0)});
        if (mSceneInfoFile.canWrite()) {
            FileOutputStream out = null;
            try {
                FileOutputStream out2 = new FileOutputStream(mSceneInfoFile);
                try {
                    out2.write(info.getBytes());
                    if (out2 != null) {
                        try {
                            out2.close();
                        } catch (IOException e) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                } catch (FileNotFoundException e2) {
                    out = out2;
                    if (this.DEBUG) {
                        Log.d(TAG, "FileNotFoundException");
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e3) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                    if (this.DEBUG) {
                        Log.d(TAG, "hypnusSendBootComplete:" + info);
                    }
                } catch (IOException e4) {
                    out = out2;
                    try {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e5) {
                                if (this.DEBUG) {
                                    Log.d(TAG, "IOException");
                                }
                            }
                        }
                        if (this.DEBUG) {
                            Log.d(TAG, "hypnusSendBootComplete:" + info);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e6) {
                                if (this.DEBUG) {
                                    Log.d(TAG, "IOException");
                                }
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    out = out2;
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e62) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e7) {
                if (this.DEBUG) {
                    Log.d(TAG, "FileNotFoundException");
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e32) {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                    }
                }
                if (this.DEBUG) {
                    Log.d(TAG, "hypnusSendBootComplete:" + info);
                }
            } catch (IOException e8) {
                if (this.DEBUG) {
                    Log.d(TAG, "IOException");
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e52) {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                    }
                }
                if (this.DEBUG) {
                    Log.d(TAG, "hypnusSendBootComplete:" + info);
                }
            }
        }
        if (this.DEBUG) {
            Log.d(TAG, "hypnusSendBootComplete:" + info);
        }
    }

    public void hypnusSetAction(int action, int timeout) {
        if (HYPNUS_STATICS_ON.booleanValue()) {
            try {
                String pkgnameinfo = AppGlobals.getPackageManager().getNameForUid(Process.myUid());
                if (pkgnameinfo == null) {
                    pkgnameinfo = "nopackagename";
                }
                int splitIndex = pkgnameinfo.indexOf(58);
                if (splitIndex > 0) {
                    hypnusSetAction(action, timeout, pkgnameinfo.substring(0, splitIndex));
                    return;
                } else {
                    hypnusSetAction(action, timeout, pkgnameinfo);
                    return;
                }
            } catch (RemoteException e) {
                hypnusSetAction(action, timeout, "exception");
                e.printStackTrace();
                return;
            }
        }
        hypnusSetAction(action, timeout, null);
    }

    public void recordActionCount(int action, int timeout, String pkgname) {
        if (staticsCount.get(pkgname + "_" + action) != null) {
            staticsCount.put(pkgname + "_" + action, Long.valueOf(((Long) staticsCount.get(pkgname + "_" + action)).longValue() + 1));
        } else {
            staticsCount.put(pkgname + "_" + action, Long.valueOf(1));
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void hypnusSetAction(int action, int timeout, String pkgname) {
        if (timeout > 180000) {
            Log.e(TAG, "hypnusSetAction: timeout longer than 180s, preven it. timeout value: " + timeout);
            timeout = 180000;
        }
        synchronized (this) {
            if (action > 11 && pkgname != null) {
                recordActionCount(action, timeout, pkgname);
                if (this.DEBUG) {
                    Log.d(TAG, action + ":" + pkgname);
                }
            }
            if (99 == action) {
                if (mName == null) {
                    return;
                }
                if (mName.indexOf("filemanager") == -1 && mName.indexOf("backuprestore") == -1) {
                    if (mName.indexOf("android.process.media") == -1) {
                        if (this.mInIO) {
                            action = 12;
                            timeout = 0;
                            this.mInIO = false;
                            this.mCount = 0;
                            if (this.DEBUG) {
                                Log.d(TAG, "Handle ACTION_PERFD, name: " + mName + " : " + timeout);
                            }
                        } else {
                            return;
                        }
                    }
                }
                if (timeout != 0) {
                    this.mCount++;
                    if (this.mCount == 1) {
                        action = 12;
                        timeout = TIME_MAX;
                        this.mInIO = true;
                    } else {
                        return;
                    }
                }
                this.mCount--;
                if (this.mCount == 0) {
                    action = 12;
                    timeout = 0;
                    this.mInIO = false;
                } else if (this.mCount < 0) {
                    this.mCount = 0;
                }
                if (this.DEBUG) {
                    Log.d(TAG, "Handle ACTION_PERFD, name: " + mName + " : " + timeout);
                }
            }
        }
    }

    public void hypnusSetBurst(int tid, int type, int timeout) {
        int act;
        Throwable th;
        File mActionInfoFile = new File(ACTIONINFO);
        switch (type) {
            case 1:
                act = 17;
                if (tid <= 0) {
                    timeout = 0;
                    break;
                } else {
                    timeout = TIME_BURST;
                    break;
                }
            case 2:
                act = 18;
                break;
            default:
                Log.e(TAG, "hypnusSetBurst: Inavlid burst type:" + type);
                return;
        }
        if (mActionInfoFile.canWrite()) {
            FileOutputStream out = null;
            try {
                FileOutputStream out2 = new FileOutputStream(mActionInfoFile);
                try {
                    out2.write(String.format("%d %d %d", new Object[]{Integer.valueOf(act), Integer.valueOf(timeout), Integer.valueOf(tid)}).getBytes());
                    if (out2 != null) {
                        try {
                            out2.close();
                        } catch (IOException e) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                } catch (FileNotFoundException e2) {
                    out = out2;
                    if (this.DEBUG) {
                        Log.d(TAG, "FileNotFoundException");
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e3) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                    if (this.DEBUG) {
                        Log.d(TAG, "hypnusSetBurst tid:" + tid + " act:" + act + " timeout:" + timeout);
                    }
                } catch (IOException e4) {
                    out = out2;
                    try {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e5) {
                                if (this.DEBUG) {
                                    Log.d(TAG, "IOException");
                                }
                            }
                        }
                        if (this.DEBUG) {
                            Log.d(TAG, "hypnusSetBurst tid:" + tid + " act:" + act + " timeout:" + timeout);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e6) {
                                if (this.DEBUG) {
                                    Log.d(TAG, "IOException");
                                }
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    out = out2;
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e62) {
                            if (this.DEBUG) {
                                Log.d(TAG, "IOException");
                            }
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e7) {
                if (this.DEBUG) {
                    Log.d(TAG, "FileNotFoundException");
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e32) {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                    }
                }
                if (this.DEBUG) {
                    Log.d(TAG, "hypnusSetBurst tid:" + tid + " act:" + act + " timeout:" + timeout);
                }
            } catch (IOException e8) {
                if (this.DEBUG) {
                    Log.d(TAG, "IOException");
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e52) {
                        if (this.DEBUG) {
                            Log.d(TAG, "IOException");
                        }
                    }
                }
                if (this.DEBUG) {
                    Log.d(TAG, "hypnusSetBurst tid:" + tid + " act:" + act + " timeout:" + timeout);
                }
            }
        }
        if (this.DEBUG) {
            Log.d(TAG, "hypnusSetBurst tid:" + tid + " act:" + act + " timeout:" + timeout);
        }
    }

    public boolean isHypnusOK() {
        return mHypnusOK;
    }

    public static synchronized Hypnus getHypnus() {
        Hypnus hypnus;
        synchronized (Hypnus.class) {
            if (sHypnus == null) {
                sHypnus = new Hypnus();
            }
            if (sHypnus == null) {
                Log.e(TAG, "Hypnus is null");
            }
            hypnus = sHypnus;
        }
        return hypnus;
    }
}
