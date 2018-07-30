package com.android.internal.os;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.ApplicationErrorReport.ParcelableCrashInfo;
import android.ddm.DdmRegister;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.DeadObjectException;
import android.os.Debug;
import android.os.IBinder;
import android.os.OppoManager;
import android.os.Process;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.AndroidConfig;
import com.android.server.NetworkManagementSocketTagger;
import dalvik.system.VMRuntime;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.TimeZone;
import java.util.logging.LogManager;
import org.apache.harmony.luni.internal.util.TimezoneGetter;

public class RuntimeInit {
    static final boolean DEBUG = false;
    static final String TAG = "AndroidRuntime";
    private static volatile boolean hadCatched = false;
    private static boolean initialized;
    private static IBinder mApplicationObject;
    private static volatile boolean mCrashing = false;

    static class Arguments {
        String[] startArgs;
        String startClass;

        Arguments(String[] args) throws IllegalArgumentException {
            parseArgs(args);
        }

        private void parseArgs(String[] args) throws IllegalArgumentException {
            int curArg = 0;
            while (curArg < args.length) {
                String arg = args[curArg];
                if (!arg.equals("--")) {
                    if (!arg.startsWith("--")) {
                        break;
                    }
                    curArg++;
                } else {
                    curArg++;
                    break;
                }
            }
            if (curArg == args.length) {
                throw new IllegalArgumentException("Missing classname argument to RuntimeInit!");
            }
            int curArg2 = curArg + 1;
            this.startClass = args[curArg];
            this.startArgs = new String[(args.length - curArg2)];
            System.arraycopy(args, curArg2, this.startArgs, 0, this.startArgs.length);
        }
    }

    private static class KillApplicationHandler implements UncaughtExceptionHandler {
        /* synthetic */ KillApplicationHandler(KillApplicationHandler -this0) {
            this();
        }

        private KillApplicationHandler() {
        }

        public void uncaughtException(Thread t, Throwable e) {
            try {
                if (RuntimeInit.mCrashing) {
                    if (!RuntimeInit.hadCatched) {
                        Process.killProcess(Process.myPid());
                        System.exit(10);
                    }
                    return;
                }
                RuntimeInit.mCrashing = true;
                if (ActivityThread.currentActivityThread() != null) {
                    ActivityThread.currentActivityThread().stopProfiling();
                }
                if (!RuntimeInit.hadCatched) {
                    ActivityManager.getService().handleApplicationCrash(RuntimeInit.mApplicationObject, new ParcelableCrashInfo(e));
                }
                if (!RuntimeInit.hadCatched) {
                    Process.killProcess(Process.myPid());
                    System.exit(10);
                }
            } catch (Throwable th) {
            }
            if (!RuntimeInit.hadCatched) {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        }
    }

    private static class LoggingHandler implements UncaughtExceptionHandler {
        /* synthetic */ LoggingHandler(LoggingHandler -this0) {
            this();
        }

        private LoggingHandler() {
        }

        private void checkToInstallGr(String appPkgName) {
            if (OppoManager.DEBUG_GR) {
                Log.d(RuntimeInit.TAG, "Geloin: Our system not contains gsf, let's download.");
            }
            if (OppoManager.canShowDialog(appPkgName).booleanValue()) {
                if (OppoManager.DEBUG_GR) {
                    Log.d(RuntimeInit.TAG, "Geloin: Will leader when Request from " + appPkgName);
                }
                OppoManager.doGr(null, null, appPkgName, "DO_GR_DOWN_INSTALL");
                OppoManager.exit(appPkgName);
                return;
            }
            if (OppoManager.DEBUG_GR) {
                Log.d(RuntimeInit.TAG, "Geloin: Will not leader when Request from " + appPkgName);
            }
            OppoManager.exit(appPkgName);
        }

        private void checkToReinstall(String appPkgName) {
            if (OppoManager.DEBUG_GR) {
                Log.d(RuntimeInit.TAG, "Geloin: Has installed GSF, need reinstall.");
            }
            OppoManager.doGr(null, null, appPkgName, "DO_GR_REINSTALL");
            OppoManager.exit(appPkgName);
        }

        public void uncaughtException(Thread t, Throwable e) {
            if (!RuntimeInit.mCrashing) {
                if (RuntimeInit.mApplicationObject == null) {
                    RuntimeInit.Clog_e(RuntimeInit.TAG, "*** FATAL EXCEPTION IN SYSTEM PROCESS: " + t.getName(), e);
                } else {
                    StringBuilder message = new StringBuilder();
                    message.append("FATAL EXCEPTION: ").append(t.getName()).append("\n");
                    String processName = ActivityThread.currentProcessName();
                    if (processName != null) {
                        message.append("Process: ").append(processName).append(", ");
                        if (OppoManager.isInnerVersion.booleanValue()) {
                            String msg = e.getMessage();
                            if (msg != null) {
                                if (msg.contains("does not have package com.google.android.gsf") && (OppoManager.grExists().booleanValue() ^ 1) != 0) {
                                    RuntimeInit.hadCatched = true;
                                    checkToInstallGr(processName);
                                } else if (msg.contains("without permission com.google.android.c2dm.permission.RECEIVE") || msg.contains("requires com.google.android.providers.gsf.permission.READ_GSERVICES, or grantUriPermission()") || msg.contains("requires com.google.android.providers.gsf.permission.READ_GSERVICES or com.google.android.providers.gsf.permission.WRITE_GSERVICES") || msg.contains("No Activity found to handle Intent { act=android.intent.action.VIEW dat=market://search") || msg.contains("Failed to find provider com.google.android.gsf.gservices")) {
                                    if (OppoManager.grExists().booleanValue()) {
                                        RuntimeInit.hadCatched = true;
                                        checkToReinstall(processName);
                                    } else {
                                        RuntimeInit.hadCatched = true;
                                        checkToInstallGr(processName);
                                    }
                                }
                            }
                        }
                    }
                    message.append("PID: ").append(Process.myPid());
                    if (!RuntimeInit.hadCatched) {
                        RuntimeInit.Clog_e(RuntimeInit.TAG, message.toString(), e);
                    }
                }
            }
        }
    }

    static class MethodAndArgsCaller implements Runnable {
        private final String[] mArgs;
        private final Method mMethod;

        public MethodAndArgsCaller(Method method, String[] args) {
            this.mMethod = method;
            this.mArgs = args;
        }

        public void run() {
            try {
                this.mMethod.invoke(null, new Object[]{this.mArgs});
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex2) {
                Throwable cause = ex2.getCause();
                if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                } else if (cause instanceof Error) {
                    throw ((Error) cause);
                } else {
                    throw new RuntimeException(ex2);
                }
            }
        }
    }

    private static final native void nativeFinishInit();

    private static final native void nativeSetExitWithoutCleanup(boolean z);

    private static int Clog_e(String tag, String msg, Throwable tr) {
        return Log.printlns(4, 6, tag, msg, tr);
    }

    protected static final void commonInit() {
        Thread.setUncaughtExceptionPreHandler(new LoggingHandler());
        Thread.setDefaultUncaughtExceptionHandler(new KillApplicationHandler());
        TimezoneGetter.setInstance(new TimezoneGetter() {
            public String getId() {
                return SystemProperties.get("persist.sys.timezone");
            }
        });
        TimeZone.setDefault(null);
        LogManager.getLogManager().reset();
        AndroidConfig androidConfig = new AndroidConfig();
        System.setProperty("http.agent", getDefaultUserAgent());
        NetworkManagementSocketTagger.install();
        if (SystemProperties.get("ro.kernel.android.tracing").equals("1")) {
            Slog.i(TAG, "NOTE: emulator trace profiling enabled");
            Debug.enableEmulatorTraceOutput();
        }
        initialized = true;
    }

    private static String getDefaultUserAgent() {
        StringBuilder result = new StringBuilder(64);
        result.append("Dalvik/");
        result.append(System.getProperty("java.vm.version"));
        result.append(" (Linux; U; Android ");
        String version = VERSION.RELEASE;
        if (version.length() <= 0) {
            version = "1.0";
        }
        result.append(version);
        if ("REL".equals(VERSION.CODENAME)) {
            String model = Build.MODEL;
            if (model.length() > 0) {
                result.append("; ");
                result.append(model);
            }
        }
        String id = Build.ID;
        if (id.length() > 0) {
            result.append(" Build/");
            result.append(id);
        }
        result.append(")");
        return result.toString();
    }

    private static Runnable findStaticMain(String className, String[] argv, ClassLoader classLoader) {
        boolean z = false;
        try {
            try {
                Method m = Class.forName(className, true, classLoader).getMethod("main", new Class[]{String[].class});
                int modifiers = m.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    z = Modifier.isPublic(modifiers);
                }
                if (z) {
                    return new MethodAndArgsCaller(m, argv);
                }
                throw new RuntimeException("Main method is not public and static on " + className);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("Missing static main on " + className, ex);
            } catch (SecurityException ex2) {
                throw new RuntimeException("Problem getting static main on " + className, ex2);
            }
        } catch (ClassNotFoundException ex3) {
            throw new RuntimeException("Missing class when invoking static main " + className, ex3);
        }
    }

    public static final void main(String[] argv) {
        enableDdms();
        if (argv.length == 2 && argv[1].equals("application")) {
            redirectLogStreams();
        }
        commonInit();
        nativeFinishInit();
    }

    protected static Runnable applicationInit(int targetSdkVersion, String[] argv, ClassLoader classLoader) {
        nativeSetExitWithoutCleanup(true);
        VMRuntime.getRuntime().setTargetHeapUtilization(0.75f);
        VMRuntime.getRuntime().setTargetSdkVersion(targetSdkVersion);
        Arguments args = new Arguments(argv);
        Trace.traceEnd(64);
        return findStaticMain(args.startClass, args.startArgs, classLoader);
    }

    public static void redirectLogStreams() {
        System.out.close();
        System.setOut(new AndroidPrintStream(4, "System.out"));
        System.err.close();
        System.setErr(new AndroidPrintStream(5, "System.err"));
    }

    public static void wtf(String tag, Throwable t, boolean system) {
        try {
            if (ActivityManager.getService().handleApplicationWtf(mApplicationObject, tag, system, new ParcelableCrashInfo(t))) {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        } catch (Throwable t2) {
            if (!(t2 instanceof DeadObjectException)) {
                Slog.e(TAG, "Error reporting WTF", t2);
                Slog.e(TAG, "Original WTF:", t);
            }
        }
    }

    public static final void setApplicationObject(IBinder app) {
        mApplicationObject = app;
    }

    public static final IBinder getApplicationObject() {
        return mApplicationObject;
    }

    static final void enableDdms() {
        DdmRegister.registerHandlers();
    }
}
