package com.android.internal.os;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.icu.impl.CacheValue;
import android.icu.impl.CacheValue.Strength;
import android.icu.text.DecimalFormatSymbols;
import android.icu.util.ULocale;
import android.opengl.EGL14;
import android.os.Build;
import android.os.Environment;
import android.os.IInstalld;
import android.os.IInstalld.Stub;
import android.os.Process;
import android.os.Seccomp;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.ZygoteProcess;
import android.os.storage.StorageManager;
import android.provider.SettingsStringUtil;
import android.security.keystore.AndroidKeyStoreProvider;
import android.service.notification.ZenModeConfig;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.Hyphenator;
import android.util.EventLog;
import android.util.Log;
import android.util.TimingsTraceLog;
import android.webkit.WebViewFactory;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.Preconditions;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import dalvik.system.ZygoteHooks;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Provider;
import java.security.Security;
import libcore.io.IoUtils;

public class ZygoteInit {
    private static final String ABI_LIST_ARG = "--abi-list=";
    private static final int LOG_BOOT_PROGRESS_PRELOAD_END = 3030;
    private static final int LOG_BOOT_PROGRESS_PRELOAD_START = 3020;
    private static final String PRELOADED_CLASSES = "/system/etc/preloaded-classes";
    private static final int PRELOAD_GC_THRESHOLD = 50000;
    public static final boolean PRELOAD_RESOURCES = true;
    private static final String PROPERTY_DISABLE_OPENGL_PRELOADING = "ro.zygote.disable_gl_preload";
    private static final String PROPERTY_GFX_DRIVER = "ro.gfx.driver.0";
    private static final String PROPERTY_RUNNING_IN_CONTAINER = "ro.boot.container";
    private static final int ROOT_GID = 0;
    private static final int ROOT_UID = 0;
    private static final String SOCKET_NAME_ARG = "--socket-name=";
    private static final int SPECIAL_TARGET_SDK_VERSION_OF_SYSTEM_SERVER = 963852741;
    private static final String TAG = "Zygote";
    private static final int UNPRIVILEGED_GID = 9999;
    private static final int UNPRIVILEGED_UID = 9999;
    private static Resources mResources;
    private static boolean sPreloadComplete;

    private static native void nativePreloadAppProcessHALs();

    private static final native void nativeZygoteInit();

    static void preload(TimingsTraceLog bootTimingsTraceLog) {
        Log.d(TAG, "begin preload");
        bootTimingsTraceLog.traceBegin("BeginIcuCachePinning");
        beginIcuCachePinning();
        bootTimingsTraceLog.traceEnd();
        bootTimingsTraceLog.traceBegin("PreloadClasses");
        preloadClasses();
        bootTimingsTraceLog.traceEnd();
        bootTimingsTraceLog.traceBegin("PreloadResources");
        preloadResources();
        bootTimingsTraceLog.traceEnd();
        Trace.traceBegin(16384, "PreloadAppProcessHALs");
        nativePreloadAppProcessHALs();
        Trace.traceEnd(16384);
        Trace.traceBegin(16384, "PreloadOpenGL");
        preloadOpenGL();
        Trace.traceEnd(16384);
        preloadSharedLibraries();
        preloadTextResources();
        WebViewFactory.prepareWebViewInZygote();
        endIcuCachePinning();
        warmUpJcaProviders();
        Log.d(TAG, "end preload");
        sPreloadComplete = true;
    }

    public static void lazyPreload() {
        Preconditions.checkState(sPreloadComplete ^ 1);
        Log.i(TAG, "Lazily preloading resources.");
        preload(new TimingsTraceLog("ZygoteInitTiming_lazy", 16384));
    }

    private static void beginIcuCachePinning() {
        int i = 0;
        Log.i(TAG, "Installing ICU cache reference pinning...");
        CacheValue.setStrength(Strength.STRONG);
        Log.i(TAG, "Preloading ICU data...");
        ULocale[] localesToPin = new ULocale[]{ULocale.ROOT, ULocale.US, ULocale.getDefault()};
        int length = localesToPin.length;
        while (i < length) {
            DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(localesToPin[i]);
            i++;
        }
    }

    private static void endIcuCachePinning() {
        CacheValue.setStrength(Strength.SOFT);
        Log.i(TAG, "Uninstalled ICU cache reference pinning...");
    }

    private static void preloadSharedLibraries() {
        Log.i(TAG, "Preloading shared libraries...");
        System.loadLibrary(ZenModeConfig.SYSTEM_AUTHORITY);
        System.loadLibrary("compiler_rt");
        System.loadLibrary("jnigraphics");
    }

    private static void preloadOpenGL() {
        String driverPackageName = SystemProperties.get(PROPERTY_GFX_DRIVER);
        if (!SystemProperties.getBoolean(PROPERTY_DISABLE_OPENGL_PRELOADING, false)) {
            if (driverPackageName == null || driverPackageName.isEmpty()) {
                EGL14.eglGetDisplay(0);
            }
        }
    }

    private static void preloadTextResources() {
        Hyphenator.init();
        TextView.preloadFontCache();
    }

    private static void warmUpJcaProviders() {
        long startTime = SystemClock.uptimeMillis();
        Trace.traceBegin(16384, "Starting installation of AndroidKeyStoreProvider");
        AndroidKeyStoreProvider.install();
        Log.i(TAG, "Installed AndroidKeyStoreProvider in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
        Trace.traceEnd(16384);
        startTime = SystemClock.uptimeMillis();
        Trace.traceBegin(16384, "Starting warm up of JCA providers");
        for (Provider p : Security.getProviders()) {
            p.warmUpServiceProvision();
        }
        Log.i(TAG, "Warmed up JCA providers in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
        Trace.traceEnd(16384);
    }

    private static void preloadClasses() {
        VMRuntime runtime = VMRuntime.getRuntime();
        try {
            InputStream is = new FileInputStream(PRELOADED_CLASSES);
            Log.i(TAG, "Preloading classes...");
            long startTime = SystemClock.uptimeMillis();
            int reuid = Os.getuid();
            int regid = Os.getgid();
            boolean droppedPriviliges = false;
            if (reuid == 0 && regid == 0) {
                try {
                    Os.setregid(0, 9999);
                    Os.setreuid(0, 9999);
                    droppedPriviliges = true;
                } catch (ErrnoException ex) {
                    throw new RuntimeException("Failed to drop root", ex);
                }
            }
            float defaultUtilization = runtime.getTargetHeapUtilization();
            runtime.setTargetHeapUtilization(0.8f);
            String line;
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is), 256);
                int count = 0;
                while (true) {
                    line = br.readLine();
                    if (line != null) {
                        line = line.trim();
                        if (!(line.startsWith("#") || line.equals(""))) {
                            Trace.traceBegin(16384, line);
                            Class.forName(line, true, null);
                            count++;
                            Trace.traceEnd(16384);
                        }
                    } else {
                        Log.i(TAG, "...preloaded " + count + " classes in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
                        IoUtils.closeQuietly(is);
                        runtime.setTargetHeapUtilization(defaultUtilization);
                        Trace.traceBegin(16384, "PreloadDexCaches");
                        runtime.preloadDexCaches();
                        Trace.traceEnd(16384);
                        if (droppedPriviliges) {
                            try {
                                Os.setreuid(0, 0);
                                Os.setregid(0, 0);
                            } catch (ErrnoException ex2) {
                                throw new RuntimeException("Failed to restore root", ex2);
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "Class not found for preloading: " + line);
            } catch (UnsatisfiedLinkError e2) {
                Log.w(TAG, "Problem preloading " + line + ": " + e2);
            } catch (IOException e3) {
                Log.e(TAG, "Error reading /system/etc/preloaded-classes.", e3);
                IoUtils.closeQuietly(is);
                runtime.setTargetHeapUtilization(defaultUtilization);
                Trace.traceBegin(16384, "PreloadDexCaches");
                runtime.preloadDexCaches();
                Trace.traceEnd(16384);
                if (droppedPriviliges) {
                    try {
                        Os.setreuid(0, 0);
                        Os.setregid(0, 0);
                    } catch (ErrnoException ex22) {
                        throw new RuntimeException("Failed to restore root", ex22);
                    }
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(is);
                runtime.setTargetHeapUtilization(defaultUtilization);
                Trace.traceBegin(16384, "PreloadDexCaches");
                runtime.preloadDexCaches();
                Trace.traceEnd(16384);
                if (droppedPriviliges) {
                    try {
                        Os.setreuid(0, 0);
                        Os.setregid(0, 0);
                    } catch (ErrnoException ex222) {
                        throw new RuntimeException("Failed to restore root", ex222);
                    }
                }
            }
        } catch (FileNotFoundException e4) {
            Log.e(TAG, "Couldn't find /system/etc/preloaded-classes.");
        }
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Jianjun.Dan@Plf.SDK : Modify for preload oppo resources", property = OppoRomType.ROM)
    private static void preloadResources() {
        VMRuntime runtime = VMRuntime.getRuntime();
        try {
            mResources = Resources.getSystem();
            mResources.startPreloading();
            Log.i(TAG, "Preloading resources...");
            preloadOppoResources(runtime);
            long startTime = SystemClock.uptimeMillis();
            TypedArray ar = mResources.obtainTypedArray(R.array.preloaded_drawables);
            int N = preloadDrawables(ar);
            ar.recycle();
            Log.i(TAG, "...preloaded " + N + " resources in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
            startTime = SystemClock.uptimeMillis();
            ar = mResources.obtainTypedArray(R.array.preloaded_color_state_lists);
            N = preloadColorStateLists(ar);
            ar.recycle();
            Log.i(TAG, "...preloaded " + N + " resources in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
            if (mResources.getBoolean(R.bool.config_freeformWindowManagement)) {
                startTime = SystemClock.uptimeMillis();
                ar = mResources.obtainTypedArray(R.array.preloaded_freeform_multi_window_drawables);
                N = preloadDrawables(ar);
                ar.recycle();
                Log.i(TAG, "...preloaded " + N + " resource in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
            }
            mResources.finishPreloading();
        } catch (RuntimeException e) {
            Log.w(TAG, "Failure preloading resources", e);
        }
    }

    private static int preloadColorStateLists(TypedArray ar) {
        int N = ar.length();
        int i = 0;
        while (i < N) {
            int id = ar.getResourceId(i, 0);
            if (id == 0 || mResources.getColorStateList(id, null) != null) {
                i++;
            } else {
                throw new IllegalArgumentException("Unable to find preloaded color resource #0x" + Integer.toHexString(id) + " (" + ar.getString(i) + ")");
            }
        }
        return N;
    }

    private static int preloadDrawables(TypedArray ar) {
        int N = ar.length();
        int i = 0;
        while (i < N) {
            int id = ar.getResourceId(i, 0);
            if (id == 0 || mResources.getDrawable(id, null) != null) {
                i++;
            } else {
                throw new IllegalArgumentException("Unable to find preloaded drawable resource #0x" + Integer.toHexString(id) + " (" + ar.getString(i) + ")");
            }
        }
        return N;
    }

    static void gcAndFinalize() {
        VMRuntime runtime = VMRuntime.getRuntime();
        System.gc();
        runtime.runFinalizationSync();
        System.gc();
    }

    private static Runnable handleSystemServerProcess(Arguments parsedArgs) {
        Os.umask(OsConstants.S_IRWXG | OsConstants.S_IRWXO);
        if (parsedArgs.niceName != null) {
            Process.setArgV0(parsedArgs.niceName);
        }
        String systemServerClasspath = Os.getenv("SYSTEMSERVERCLASSPATH");
        if (systemServerClasspath != null) {
            performSystemServerDexOpt(systemServerClasspath);
            if (SystemProperties.getBoolean("dalvik.vm.profilesystemserver", false) && (Build.IS_USERDEBUG || Build.IS_ENG)) {
                try {
                    File profile = new File(Environment.getDataProfilesDePackageDirectory(1000, "system_server"), "primary.prof");
                    profile.getParentFile().mkdirs();
                    profile.createNewFile();
                    VMRuntime.registerAppInfo(profile.getPath(), systemServerClasspath.split(SettingsStringUtil.DELIMITER));
                } catch (Exception e) {
                    Log.wtf(TAG, "Failed to set up system server profile", e);
                }
            }
        }
        if (parsedArgs.invokeWith != null) {
            String[] args = parsedArgs.remainingArgs;
            if (systemServerClasspath != null) {
                String[] amendedArgs = new String[(args.length + 2)];
                amendedArgs[0] = "-cp";
                amendedArgs[1] = systemServerClasspath;
                System.arraycopy(args, 0, amendedArgs, 2, args.length);
                args = amendedArgs;
            }
            WrapperInit.execApplication(parsedArgs.invokeWith, parsedArgs.niceName, parsedArgs.targetSdkVersion, VMRuntime.getCurrentInstructionSet(), null, args);
            throw new IllegalStateException("Unexpected return from WrapperInit.execApplication");
        }
        ClassLoader cl = null;
        if (systemServerClasspath != null) {
            cl = createPathClassLoader(systemServerClasspath, parsedArgs.targetSdkVersion);
            Thread.currentThread().setContextClassLoader(cl);
        }
        return zygoteInit(SPECIAL_TARGET_SDK_VERSION_OF_SYSTEM_SERVER, parsedArgs.remainingArgs, cl);
    }

    static ClassLoader createPathClassLoader(String classPath, int targetSdkVersion) {
        String libraryPath = System.getProperty("java.library.path");
        return ClassLoaderFactory.createClassLoader(classPath, libraryPath, libraryPath, ClassLoader.getSystemClassLoader(), targetSdkVersion, true, null);
    }

    private static void performSystemServerDexOpt(String classPath) {
        String[] classPathElements = classPath.split(SettingsStringUtil.DELIMITER);
        IInstalld installd = Stub.asInterface(ServiceManager.getService("installd"));
        String instructionSet = VMRuntime.getRuntime().vmInstructionSet();
        String classPathForElement = "";
        int i = 0;
        int length = classPathElements.length;
        while (true) {
            int i2 = i;
            if (i2 < length) {
                int dexoptNeeded;
                String classPathElement = classPathElements[i2];
                String systemServerFilter = SystemProperties.get("dalvik.vm.systemservercompilerfilter", "speed");
                try {
                    dexoptNeeded = DexFile.getDexOptNeeded(classPathElement, instructionSet, systemServerFilter, false, false);
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Missing classpath element for system server: " + classPathElement);
                } catch (Throwable e2) {
                    Log.w(TAG, "Error checking classpath element for system server: " + classPathElement, e2);
                    dexoptNeeded = 0;
                }
                if (dexoptNeeded != 0) {
                    String packageName = PhoneConstants.APN_TYPE_ALL;
                    String compilerFilter = systemServerFilter;
                    try {
                        installd.dexopt(classPathElement, 1000, PhoneConstants.APN_TYPE_ALL, instructionSet, dexoptNeeded, null, 0, systemServerFilter, StorageManager.UUID_PRIVATE_INTERNAL, getSystemServerClassLoaderContext(classPathForElement), null, false);
                    } catch (Throwable e3) {
                        Log.w(TAG, "Failed compiling classpath element for system server: " + classPathElement, e3);
                    }
                }
                classPathForElement = encodeSystemServerClassPath(classPathForElement, classPathElement);
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    private static String getSystemServerClassLoaderContext(String classPath) {
        return classPath == null ? "PCL[]" : "PCL[" + classPath + "]";
    }

    private static String encodeSystemServerClassPath(String classPath, String newElement) {
        if (classPath == null || classPath.isEmpty()) {
            return newElement;
        }
        return classPath + SettingsStringUtil.DELIMITER + newElement;
    }

    private static Runnable forkSystemServer(String abiList, String socketName, ZygoteServer zygoteServer) {
        IllegalArgumentException ex;
        long capabilities = posixCapabilitiesAsBits(OsConstants.CAP_IPC_LOCK, OsConstants.CAP_KILL, OsConstants.CAP_NET_ADMIN, OsConstants.CAP_NET_BIND_SERVICE, OsConstants.CAP_NET_BROADCAST, OsConstants.CAP_NET_RAW, OsConstants.CAP_SYS_MODULE, OsConstants.CAP_SYS_NICE, OsConstants.CAP_SYS_PTRACE, OsConstants.CAP_SYS_TIME, OsConstants.CAP_SYS_TTY_CONFIG, OsConstants.CAP_WAKE_ALARM);
        if (!SystemProperties.getBoolean(PROPERTY_RUNNING_IN_CONTAINER, false)) {
            capabilities |= posixCapabilitiesAsBits(OsConstants.CAP_BLOCK_SUSPEND);
        }
        try {
            Arguments parsedArgs = new Arguments(new String[]{"--setuid=1000", "--setgid=1000", "--setgroups=1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1018,1021,1023,1032,3001,3002,3003,3006,3007,3009,3010", "--capabilities=" + capabilities + "," + capabilities, "--nice-name=system_server", "--runtime-args", "com.android.server.SystemServer"});
            try {
                ZygoteConnection.applyDebuggerSystemProperty(parsedArgs);
                ZygoteConnection.applyInvokeWithSystemProperty(parsedArgs);
                int pid = Zygote.forkSystemServer(parsedArgs.uid, parsedArgs.gid, parsedArgs.gids, parsedArgs.debugFlags, null, parsedArgs.permittedCapabilities, parsedArgs.effectiveCapabilities);
                if (pid == 0) {
                    if (hasSecondZygote(abiList)) {
                        waitForSecondaryZygote(socketName);
                    }
                    zygoteServer.closeServerSocket();
                    return handleSystemServerProcess(parsedArgs);
                }
                if (pid < 0) {
                    Log.e(TAG, "fork system_server failed, reboot device");
                    if (SystemProperties.getBoolean("ro.build.release_type", false)) {
                        SystemProperties.set("persist.hungtask.oppo.kill", "system_server,start failure");
                        SystemProperties.set("sys.powerctl", "reboot,");
                    }
                }
                return null;
            } catch (IllegalArgumentException e) {
                ex = e;
            }
        } catch (IllegalArgumentException e2) {
            ex = e2;
            throw new RuntimeException(ex);
        }
    }

    private static long posixCapabilitiesAsBits(int... capabilities) {
        long result = 0;
        for (int capability : capabilities) {
            if (capability < 0 || capability > OsConstants.CAP_LAST_CAP) {
                throw new IllegalArgumentException(String.valueOf(capability));
            }
            result |= 1 << capability;
        }
        return result;
    }

    public static void main(String[] argv) {
        ZygoteServer zygoteServer = new ZygoteServer();
        ZygoteHooks.startZygoteNoThreadCreation();
        try {
            Os.setpgid(0, 0);
            try {
                if (!"1".equals(SystemProperties.get("sys.boot_completed"))) {
                    MetricsLogger.histogram(null, "boot_zygote_init", (int) SystemClock.elapsedRealtime());
                }
                TimingsTraceLog bootTimingsTraceLog = new TimingsTraceLog(Process.is64Bit() ? "Zygote64Timing" : "Zygote32Timing", 16384);
                bootTimingsTraceLog.traceBegin("ZygoteInit");
                RuntimeInit.enableDdms();
                boolean startSystemServer = false;
                String socketName = "zygote";
                String abiList = null;
                boolean enableLazyPreload = false;
                for (int i = 1; i < argv.length; i++) {
                    if ("start-system-server".equals(argv[i])) {
                        startSystemServer = true;
                    } else if ("--enable-lazy-preload".equals(argv[i])) {
                        enableLazyPreload = true;
                    } else if (argv[i].startsWith(ABI_LIST_ARG)) {
                        abiList = argv[i].substring(ABI_LIST_ARG.length());
                    } else if (argv[i].startsWith(SOCKET_NAME_ARG)) {
                        socketName = argv[i].substring(SOCKET_NAME_ARG.length());
                    } else {
                        throw new RuntimeException("Unknown command line argument: " + argv[i]);
                    }
                }
                if (abiList == null) {
                    throw new RuntimeException("No ABI list supplied.");
                }
                zygoteServer.registerServerSocket(socketName);
                if (enableLazyPreload) {
                    Zygote.resetNicePriority();
                } else {
                    bootTimingsTraceLog.traceBegin("ZygotePreload");
                    EventLog.writeEvent(LOG_BOOT_PROGRESS_PRELOAD_START, SystemClock.uptimeMillis());
                    preload(bootTimingsTraceLog);
                    EventLog.writeEvent(LOG_BOOT_PROGRESS_PRELOAD_END, SystemClock.uptimeMillis());
                    bootTimingsTraceLog.traceEnd();
                }
                bootTimingsTraceLog.traceBegin("PostZygoteInitGC");
                gcAndFinalize();
                bootTimingsTraceLog.traceEnd();
                bootTimingsTraceLog.traceEnd();
                Trace.setTracingEnabled(false, 0);
                Zygote.nativeUnmountStorageOnInit();
                Seccomp.setPolicy();
                ZygoteHooks.stopZygoteNoThreadCreation();
                if (startSystemServer) {
                    Runnable r = forkSystemServer(abiList, socketName, zygoteServer);
                    if (r != null) {
                        r.run();
                        zygoteServer.closeServerSocket();
                        return;
                    }
                }
                Log.i(TAG, "Accepting command socket connections");
                Runnable caller = zygoteServer.runSelectLoop(abiList);
                zygoteServer.closeServerSocket();
                if (caller != null) {
                    caller.run();
                }
            } catch (Throwable th) {
                zygoteServer.closeServerSocket();
            }
        } catch (ErrnoException ex) {
            throw new RuntimeException("Failed to setpgid(0,0)", ex);
        }
    }

    private static boolean hasSecondZygote(String abiList) {
        return SystemProperties.get("ro.product.cpu.abilist").equals(abiList) ^ 1;
    }

    private static void waitForSecondaryZygote(String socketName) {
        ZygoteProcess.waitForConnectionToZygote("zygote".equals(socketName) ? "zygote_secondary" : "zygote");
    }

    static boolean isPreloadComplete() {
        return sPreloadComplete;
    }

    private ZygoteInit() {
    }

    public static final Runnable zygoteInit(int targetSdkVersion, String[] argv, ClassLoader classLoader) {
        Trace.traceBegin(64, "ZygoteInit");
        RuntimeInit.redirectLogStreams();
        RuntimeInit.commonInit();
        nativeZygoteInit();
        return RuntimeInit.applicationInit(targetSdkVersion, argv, classLoader);
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "Jianjun.Dan@Plf.SDK : Add for preload resource", property = OppoRomType.ROM)
    private static void preloadOppoResources(VMRuntime runtime) {
        long startTime = SystemClock.uptimeMillis();
        TypedArray ar = mResources.obtainTypedArray(201786385);
        int N = preloadDrawables(ar);
        ar.recycle();
        Log.i(TAG, "...preloaded " + N + " oppo drawable resources in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
        startTime = SystemClock.uptimeMillis();
        ar = mResources.obtainTypedArray(201786386);
        N = preloadColorStateLists(ar);
        ar.recycle();
        Log.i(TAG, "...preloaded " + N + " oppo color resources in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
    }
}
