package com.android.server.am;

import android.app.ActivityManager.ProcessErrorStateInfo;
import android.app.ActivityThread;
import android.app.ApplicationErrorReport;
import android.app.ApplicationErrorReport.AnrInfo;
import android.app.ApplicationErrorReport.CrashInfo;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.app.ProcessMap;
import com.android.internal.logging.MetricsLogger;
import com.android.server.LocationManagerService;
import com.android.server.RescueParty;
import com.android.server.Watchdog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

class AppErrors {
    public static final String[] LIGHTWEIGHT_NATIVE_STACKS_OF_INTEREST = new String[]{"/system/bin/mediaserver", "/system/bin/surfaceflinger", "/system/bin/audioserver", "media.codec"};
    private static final String TAG = "ActivityManager";
    private ArraySet<String> mAppsNotReportingCrashes;
    private final ProcessMap<BadProcessInfo> mBadProcesses = new ProcessMap();
    private final Context mContext;
    private ArrayList<String> mInterestAnrAppProcNames = null;
    private final ProcessMap<Long> mProcessCrashTimes = new ProcessMap();
    private final ProcessMap<Long> mProcessCrashTimesPersistent = new ProcessMap();
    private final ActivityManagerService mService;
    SimpleDateFormat mTraceDateFormat = new SimpleDateFormat("dd_MMM_HH_mm_ss.SSS");

    static final class BadProcessInfo {
        final String longMsg;
        final String shortMsg;
        final String stack;
        final long time;

        BadProcessInfo(long time, String shortMsg, String longMsg, String stack) {
            this.time = time;
            this.shortMsg = shortMsg;
            this.longMsg = longMsg;
            this.stack = stack;
        }
    }

    AppErrors(Context context, ActivityManagerService service) {
        context.assertRuntimeOverlayThemable();
        this.mService = service;
        this.mContext = context;
    }

    boolean dumpLocked(FileDescriptor fd, PrintWriter pw, boolean needSep, String dumpPackage) {
        boolean printed;
        int processCount;
        int ip;
        String pname;
        int uidCount;
        int i;
        int puid;
        ProcessRecord r;
        if (!this.mProcessCrashTimes.getMap().isEmpty()) {
            printed = false;
            long now = SystemClock.uptimeMillis();
            ArrayMap<String, SparseArray<Long>> pmap = this.mProcessCrashTimes.getMap();
            processCount = pmap.size();
            for (ip = 0; ip < processCount; ip++) {
                pname = (String) pmap.keyAt(ip);
                SparseArray<Long> uids = (SparseArray) pmap.valueAt(ip);
                uidCount = uids.size();
                for (i = 0; i < uidCount; i++) {
                    puid = uids.keyAt(i);
                    r = (ProcessRecord) this.mService.mProcessNames.get(pname, puid);
                    if (dumpPackage == null || (r != null && (r.pkgList.containsKey(dumpPackage) ^ 1) == 0)) {
                        if (!printed) {
                            if (needSep) {
                                pw.println();
                            }
                            needSep = true;
                            pw.println("  Time since processes crashed:");
                            printed = true;
                        }
                        pw.print("    Process ");
                        pw.print(pname);
                        pw.print(" uid ");
                        pw.print(puid);
                        pw.print(": last crashed ");
                        TimeUtils.formatDuration(now - ((Long) uids.valueAt(i)).longValue(), pw);
                        pw.println(" ago");
                    }
                }
            }
        }
        if (!this.mBadProcesses.getMap().isEmpty()) {
            printed = false;
            ArrayMap<String, SparseArray<BadProcessInfo>> pmap2 = this.mBadProcesses.getMap();
            processCount = pmap2.size();
            for (ip = 0; ip < processCount; ip++) {
                pname = (String) pmap2.keyAt(ip);
                SparseArray<BadProcessInfo> uids2 = (SparseArray) pmap2.valueAt(ip);
                uidCount = uids2.size();
                for (i = 0; i < uidCount; i++) {
                    puid = uids2.keyAt(i);
                    r = (ProcessRecord) this.mService.mProcessNames.get(pname, puid);
                    if (dumpPackage == null || (r != null && (r.pkgList.containsKey(dumpPackage) ^ 1) == 0)) {
                        if (!printed) {
                            if (needSep) {
                                pw.println();
                            }
                            needSep = true;
                            pw.println("  Bad processes:");
                            printed = true;
                        }
                        BadProcessInfo info = (BadProcessInfo) uids2.valueAt(i);
                        pw.print("    Bad process ");
                        pw.print(pname);
                        pw.print(" uid ");
                        pw.print(puid);
                        pw.print(": crashed at time ");
                        pw.println(info.time);
                        if (info.shortMsg != null) {
                            pw.print("      Short msg: ");
                            pw.println(info.shortMsg);
                        }
                        if (info.longMsg != null) {
                            pw.print("      Long msg: ");
                            pw.println(info.longMsg);
                        }
                        if (info.stack != null) {
                            pw.println("      Stack:");
                            int lastPos = 0;
                            for (int pos = 0; pos < info.stack.length(); pos++) {
                                if (info.stack.charAt(pos) == 10) {
                                    pw.print("        ");
                                    pw.write(info.stack, lastPos, pos - lastPos);
                                    pw.println();
                                    lastPos = pos + 1;
                                }
                            }
                            if (lastPos < info.stack.length()) {
                                pw.print("        ");
                                pw.write(info.stack, lastPos, info.stack.length() - lastPos);
                                pw.println();
                            }
                        }
                    }
                }
            }
        }
        return needSep;
    }

    boolean isBadProcessLocked(ApplicationInfo info) {
        return this.mBadProcesses.get(info.processName, info.uid) != null;
    }

    void clearBadProcessLocked(ApplicationInfo info) {
        this.mBadProcesses.remove(info.processName, info.uid);
    }

    void resetProcessCrashTimeLocked(ApplicationInfo info) {
        this.mProcessCrashTimes.remove(info.processName, info.uid);
    }

    void resetProcessCrashTimeLocked(boolean resetEntireUser, int appId, int userId) {
        ArrayMap<String, SparseArray<Long>> pmap = this.mProcessCrashTimes.getMap();
        for (int ip = pmap.size() - 1; ip >= 0; ip--) {
            SparseArray<Long> ba = (SparseArray) pmap.valueAt(ip);
            for (int i = ba.size() - 1; i >= 0; i--) {
                boolean remove = false;
                int entUid = ba.keyAt(i);
                if (resetEntireUser) {
                    if (UserHandle.getUserId(entUid) == userId) {
                        remove = true;
                    }
                } else if (userId == -1) {
                    if (UserHandle.getAppId(entUid) == appId) {
                        remove = true;
                    }
                } else if (entUid == UserHandle.getUid(userId, appId)) {
                    remove = true;
                }
                if (remove) {
                    ba.removeAt(i);
                }
            }
            if (ba.size() == 0) {
                pmap.removeAt(ip);
            }
        }
    }

    void loadAppsNotReportingCrashesFromConfigLocked(String appsNotReportingCrashesConfig) {
        if (appsNotReportingCrashesConfig != null) {
            String[] split = appsNotReportingCrashesConfig.split(",");
            if (split.length > 0) {
                this.mAppsNotReportingCrashes = new ArraySet();
                Collections.addAll(this.mAppsNotReportingCrashes, split);
            }
        }
    }

    void killAppAtUserRequestLocked(ProcessRecord app, Dialog fromDialog) {
        app.crashing = false;
        app.crashingReport = null;
        app.notResponding = false;
        app.notRespondingReport = null;
        if (app.anrDialog == fromDialog) {
            app.anrDialog = null;
        }
        if (app.waitDialog == fromDialog) {
            app.waitDialog = null;
        }
        if (app.pid > 0 && app.pid != ActivityManagerService.MY_PID) {
            handleAppCrashLocked(app, "user-terminated", null, null, null, null);
            app.kill("user request after error", true);
        }
    }

    void scheduleAppCrashLocked(int uid, int initialPid, String packageName, int userId, String message) {
        ProcessRecord proc = null;
        synchronized (this.mService.mPidsSelfLocked) {
            for (int i = 0; i < this.mService.mPidsSelfLocked.size(); i++) {
                ProcessRecord p = (ProcessRecord) this.mService.mPidsSelfLocked.valueAt(i);
                if (uid < 0 || p.uid == uid) {
                    if (p.pid == initialPid) {
                        proc = p;
                        break;
                    } else if (p.pkgList.containsKey(packageName) && (userId < 0 || p.userId == userId)) {
                        proc = p;
                    }
                }
            }
        }
        if (proc == null) {
            Slog.w(TAG, "crashApplication: nothing for uid=" + uid + " initialPid=" + initialPid + " packageName=" + packageName + " userId=" + userId);
        } else {
            proc.scheduleCrash(message);
        }
    }

    void crashApplication(ProcessRecord r, CrashInfo crashInfo) {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            crashApplicationInner(r, crashInfo, callingPid, callingUid);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void crashApplicationInner(ProcessRecord r, CrashInfo crashInfo, int callingPid, int callingUid) {
        long timeMillis = System.currentTimeMillis();
        String shortMsg = crashInfo.exceptionClassName;
        String longMsg = crashInfo.exceptionMessage;
        String stackTrace = crashInfo.stackTrace;
        if (shortMsg != null && longMsg != null) {
            longMsg = shortMsg + ": " + longMsg;
        } else if (shortMsg != null) {
            longMsg = shortMsg;
        }
        if (r != null && r.persistent) {
            RescueParty.notePersistentAppCrash(this.mContext, r.uid);
        }
        AppErrorResult result = new AppErrorResult();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (handleAppCrashInActivityController(r, crashInfo, shortMsg, longMsg, stackTrace, timeMillis, callingPid, callingUid)) {
                } else {
                    if (r != null) {
                        if (r.instr != null) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                    }
                    if (r != null) {
                        this.mService.mBatteryStatsService.noteProcessCrash(r.processName, r.uid);
                    }
                    Data data = new Data();
                    data.result = result;
                    data.proc = r;
                    if (r == null || (makeAppCrashingLocked(r, shortMsg, longMsg, stackTrace, data) ^ 1) != 0) {
                    } else {
                        Message msg = Message.obtain();
                        msg.what = 1;
                        TaskRecord task = data.task;
                        msg.obj = data;
                        this.mService.mUiHandler.sendMessage(msg);
                    }
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return;
        OppoAppErrorsStatistics.doErrorsStatistics(this.mContext, r, crashInfo);
    }

    private boolean handleAppCrashInActivityController(ProcessRecord r, CrashInfo crashInfo, String shortMsg, String longMsg, String stackTrace, long timeMillis, int callingPid, int callingUid) {
        if (this.mService.mController == null) {
            return false;
        }
        String name;
        if (r != null) {
            try {
                name = r.processName;
            } catch (RemoteException e) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
                if (this.mService.mOppoActivityControlerScheduler != null) {
                    this.mService.mOppoActivityControlerScheduler.exitRunningScheduler();
                    this.mService.mOppoActivityControlerScheduler = null;
                }
            }
        } else {
            name = null;
        }
        int pid = r != null ? r.pid : callingPid;
        int uid = r != null ? r.info.uid : callingUid;
        if (!this.mService.mController.appCrashed(name, pid, shortMsg, longMsg, timeMillis, crashInfo.stackTrace)) {
            if (LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON.equals(SystemProperties.get("ro.debuggable", "0")) && "Native crash".equals(crashInfo.exceptionClassName)) {
                Slog.w(TAG, "Skip killing native crashed app " + name + "(" + pid + ") during testing");
            } else {
                Slog.w(TAG, "Force-killing crashed app " + name + " at watcher's request");
                if (r == null) {
                    Process.killProcess(pid);
                    ActivityManagerService.killProcessGroup(uid, pid);
                } else if (!makeAppCrashingLocked(r, shortMsg, longMsg, stackTrace, null)) {
                    r.kill("crash", true);
                }
            }
            return true;
        }
        return false;
    }

    private boolean makeAppCrashingLocked(ProcessRecord app, String shortMsg, String longMsg, String stackTrace, Data data) {
        app.crashing = true;
        app.crashingReport = generateProcessError(app, 1, null, shortMsg, longMsg, stackTrace);
        startAppProblemLocked(app);
        app.stopFreezingAllLocked();
        return handleAppCrashLocked(app, "force-crash", shortMsg, longMsg, stackTrace, data);
    }

    void startAppProblemLocked(ProcessRecord app) {
        app.errorReportReceiver = null;
        for (int userId : this.mService.mUserController.getCurrentProfileIdsLocked()) {
            if (app.userId == userId) {
                app.errorReportReceiver = ApplicationErrorReport.getErrorReportReceiver(this.mContext, app.info.packageName, app.info.flags);
            }
        }
        this.mService.skipCurrentReceiverLocked(app);
    }

    private ProcessErrorStateInfo generateProcessError(ProcessRecord app, int condition, String activity, String shortMsg, String longMsg, String stackTrace) {
        ProcessErrorStateInfo report = new ProcessErrorStateInfo();
        report.condition = condition;
        report.processName = app.processName;
        report.pid = app.pid;
        report.uid = app.info.uid;
        report.tag = activity;
        report.shortMsg = shortMsg;
        report.longMsg = longMsg;
        report.stackTrace = stackTrace;
        return report;
    }

    Intent createAppErrorIntentLocked(ProcessRecord r, long timeMillis, CrashInfo crashInfo) {
        ApplicationErrorReport report = createAppErrorReportLocked(r, timeMillis, crashInfo);
        if (report == null) {
            return null;
        }
        Intent result = new Intent("android.intent.action.APP_ERROR");
        result.setComponent(r.errorReportReceiver);
        result.putExtra("android.intent.extra.BUG_REPORT", report);
        result.addFlags(268435456);
        return result;
    }

    private ApplicationErrorReport createAppErrorReportLocked(ProcessRecord r, long timeMillis, CrashInfo crashInfo) {
        boolean z = false;
        if (r.errorReportReceiver == null) {
            return null;
        }
        if (!r.crashing && (r.notResponding ^ 1) != 0 && (r.forceCrashReport ^ 1) != 0) {
            return null;
        }
        ApplicationErrorReport report = new ApplicationErrorReport();
        report.packageName = r.info.packageName;
        report.installerPackageName = r.errorReportReceiver.getPackageName();
        report.processName = r.processName;
        report.time = timeMillis;
        if ((r.info.flags & 1) != 0) {
            z = true;
        }
        report.systemApp = z;
        if (r.crashing || r.forceCrashReport) {
            report.type = 1;
            report.crashInfo = crashInfo;
        } else if (r.notResponding) {
            report.type = 2;
            report.anrInfo = new AnrInfo();
            report.anrInfo.activity = r.notRespondingReport.tag;
            report.anrInfo.cause = r.notRespondingReport.shortMsg;
            report.anrInfo.info = r.notRespondingReport.longMsg;
        }
        return report;
    }

    boolean handleAppCrashLocked(ProcessRecord app, String reason, String shortMsg, String longMsg, String stackTrace, Data data) {
        Long crashTime;
        long now = SystemClock.uptimeMillis();
        boolean showBackground = Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        boolean procIsBoundForeground = app.curProcState == 3;
        OppoCrashClearManager.getInstance().clearAppUserData(app);
        boolean tryAgain = false;
        Long crashTimePersistent;
        if (app.isolated) {
            crashTimePersistent = null;
            crashTime = null;
        } else {
            crashTime = (Long) this.mProcessCrashTimes.get(app.info.processName, app.uid);
            crashTimePersistent = (Long) this.mProcessCrashTimesPersistent.get(app.info.processName, app.uid);
        }
        for (int i = app.services.size() - 1; i >= 0; i--) {
            ServiceRecord sr = (ServiceRecord) app.services.valueAt(i);
            if (now > sr.restartTime + 60000) {
                sr.crashCount = 1;
            } else {
                sr.crashCount++;
            }
            if (((long) sr.crashCount) < this.mService.mConstants.BOUND_SERVICE_MAX_CRASH_RETRY && (sr.isForeground || procIsBoundForeground)) {
                tryAgain = true;
            }
        }
        if (crashTime == null || now >= crashTime.longValue() + 60000) {
            TaskRecord affectedTask = this.mService.mStackSupervisor.finishTopRunningActivityLocked(app, reason);
            if (data != null) {
                data.task = affectedTask;
            }
            if (!(data == null || crashTimePersistent == null || now >= crashTimePersistent.longValue() + 60000)) {
                data.repeating = true;
            }
        } else {
            Slog.w(TAG, "Process " + app.info.processName + " has crashed too many times: killing!");
            EventLog.writeEvent(EventLogTags.AM_PROCESS_CRASHED_TOO_MUCH, new Object[]{Integer.valueOf(app.userId), app.info.processName, Integer.valueOf(app.uid)});
            this.mService.mStackSupervisor.handleAppCrashLocked(app);
            if (!app.persistent) {
                EventLog.writeEvent(EventLogTags.AM_PROC_BAD, new Object[]{Integer.valueOf(app.userId), Integer.valueOf(app.uid), app.info.processName});
                if (!app.isolated) {
                    this.mBadProcesses.put(app.info.processName, app.uid, new BadProcessInfo(now, shortMsg, longMsg, stackTrace));
                    this.mProcessCrashTimes.remove(app.info.processName, app.uid);
                }
                app.bad = true;
                app.removed = true;
                this.mService.removeProcessLocked(app, false, tryAgain, "crash");
                this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                if (!showBackground) {
                    return false;
                }
            }
            this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
        if (data != null && tryAgain) {
            data.isRestartableForService = true;
        }
        ArrayList<ActivityRecord> activities = app.activities;
        if (app == this.mService.mHomeProcess && activities.size() > 0 && (this.mService.mHomeProcess.info.flags & 1) == 0) {
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (r.isHomeActivity()) {
                    Log.i(TAG, "Clearing package preferred activities from " + r.packageName);
                    try {
                        ActivityThread.getPackageManager().clearPackagePreferredActivities(r.packageName);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
        if (!app.isolated) {
            this.mProcessCrashTimes.put(app.info.processName, app.uid, Long.valueOf(now));
            this.mProcessCrashTimesPersistent.put(app.info.processName, app.uid, Long.valueOf(now));
        }
        if (app.crashHandler != null) {
            this.mService.mHandler.post(app.crashHandler);
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void handleShowAppErrorUi(Message msg) {
        Data data = msg.obj;
        boolean showBackground = Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        synchronized (this.mService) {
            ActivityManagerService.boostPriorityForLockedSection();
            ProcessRecord proc = data.proc;
            AppErrorResult res = data.result;
            if (proc == null || proc.crashDialog == null) {
                try {
                    int isBackground = UserHandle.getAppId(proc.uid) >= 10000 ? proc.pid != ActivityManagerService.MY_PID ? 1 : 0 : 0;
                    for (int userId : this.mService.mUserController.getCurrentProfileIdsLocked()) {
                        isBackground &= proc.userId != userId ? 1 : 0;
                    }
                    if (isBackground == 0 || (showBackground ^ 1) == 0) {
                        int crashSilenced;
                        if (this.mAppsNotReportingCrashes != null) {
                            crashSilenced = this.mAppsNotReportingCrashes.contains(proc.info.packageName);
                        } else {
                            crashSilenced = 0;
                        }
                        if ((this.mService.canShowErrorDialogs() || showBackground) && (crashSilenced ^ 1) != 0) {
                            if (SystemProperties.getBoolean("persist.sys.assert.panic", false)) {
                                proc.crashDialog = new AppErrorDialog(this.mContext, this.mService, data);
                            } else {
                                if (proc.crashDialog != null) {
                                    Slog.w(TAG, " Dismiss app error dialog : " + proc.processName);
                                    proc.crashDialog = null;
                                }
                                if (res != null) {
                                    res.set(0);
                                }
                            }
                        } else if (res != null) {
                            res.set(AppErrorDialog.CANT_SHOW);
                        }
                        data.proc.crashDialog = proc.crashDialog;
                    } else {
                        Slog.w(TAG, "Skipping crash dialog of " + proc + ": background");
                        if (res != null) {
                            res.set(AppErrorDialog.BACKGROUND_USER);
                        }
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            } else {
                Slog.e(TAG, "App already has crash dialog: " + proc);
                if (res != null) {
                    res.set(AppErrorDialog.ALREADY_SHOWING);
                }
            }
        }
    }

    void stopReportingCrashesLocked(ProcessRecord proc) {
        if (this.mAppsNotReportingCrashes == null) {
            this.mAppsNotReportingCrashes = new ArraySet();
        }
        this.mAppsNotReportingCrashes.add(proc.info.packageName);
    }

    static boolean isInterestingForBackgroundTraces(ProcessRecord app) {
        boolean z = true;
        if (app.pid == ActivityManagerService.MY_PID) {
            return true;
        }
        if (!app.isInterestingToUserLocked() && ((app.info == null || !OppoFreeFormManagerService.FREEFORM_CALLER_PKG.equals(app.info.packageName)) && !app.hasTopUi)) {
            z = app.hasOverlayUi;
        }
        return z;
    }

    final void appNotResponding(ProcessRecord app, ActivityRecord activity, ActivityRecord parent, boolean aboveSystem, String annotation) {
        ProcessRecord fApp = app;
        ActivityRecord fActivity = activity;
        ActivityRecord fParent = parent;
        boolean fAboveSystem = aboveSystem;
        final ProcessRecord processRecord = app;
        final ActivityRecord activityRecord = activity;
        final ActivityRecord activityRecord2 = parent;
        final boolean z = aboveSystem;
        final String str = annotation;
        new Thread() {
            public void run() {
                AppErrors.this.appNotRespondingInner(processRecord, activityRecord, activityRecord2, z, str);
            }
        }.start();
    }

    final void appNotRespondingInner(com.android.server.am.ProcessRecord r50, com.android.server.am.ActivityRecord r51, com.android.server.am.ActivityRecord r52, boolean r53, java.lang.String r54) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r28_0 'lastPids' android.util.SparseArray<java.lang.Boolean>) in PHI: PHI: (r28_2 'lastPids' android.util.SparseArray<java.lang.Boolean>) = (r28_0 'lastPids' android.util.SparseArray<java.lang.Boolean>), (r28_1 'lastPids' android.util.SparseArray<java.lang.Boolean>) binds: {(r28_0 'lastPids' android.util.SparseArray<java.lang.Boolean>)=B:165:0x0485, (r28_1 'lastPids' android.util.SparseArray<java.lang.Boolean>)=B:166:0x0487}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
	at java.lang.Iterable.forEach(Iterable.java:75)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r49 = this;
        r21 = new java.util.ArrayList;
        r4 = 5;
        r0 = r21;
        r0.<init>(r4);
        r28 = new android.util.SparseArray;
        r4 = 20;
        r0 = r28;
        r0.<init>(r4);
        r0 = r49;
        r4 = r0.mService;
        r4 = r4.mController;
        if (r4 == 0) goto L_0x0040;
    L_0x0019:
        r0 = r49;	 Catch:{ RemoteException -> 0x009c }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x009c }
        r4 = r4.mController;	 Catch:{ RemoteException -> 0x009c }
        r0 = r50;	 Catch:{ RemoteException -> 0x009c }
        r5 = r0.processName;	 Catch:{ RemoteException -> 0x009c }
        r0 = r50;	 Catch:{ RemoteException -> 0x009c }
        r6 = r0.pid;	 Catch:{ RemoteException -> 0x009c }
        r0 = r54;	 Catch:{ RemoteException -> 0x009c }
        r43 = r4.appEarlyNotResponding(r5, r6, r0);	 Catch:{ RemoteException -> 0x009c }
        if (r43 >= 0) goto L_0x0040;	 Catch:{ RemoteException -> 0x009c }
    L_0x002f:
        r0 = r50;	 Catch:{ RemoteException -> 0x009c }
        r4 = r0.pid;	 Catch:{ RemoteException -> 0x009c }
        r5 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ RemoteException -> 0x009c }
        if (r4 == r5) goto L_0x0040;	 Catch:{ RemoteException -> 0x009c }
    L_0x0037:
        r4 = "anr";	 Catch:{ RemoteException -> 0x009c }
        r5 = 1;	 Catch:{ RemoteException -> 0x009c }
        r0 = r50;	 Catch:{ RemoteException -> 0x009c }
        r0.kill(r4, r5);	 Catch:{ RemoteException -> 0x009c }
    L_0x0040:
        r14 = android.os.SystemClock.uptimeMillis();
        r0 = r49;
        r4 = r0.mService;
        r4.updateCpuStatsNow();
        r0 = r49;
        r4 = r0.mContext;
        r4 = r4.getContentResolver();
        r5 = "anr_show_background";
        r6 = 0;
        r4 = android.provider.Settings.Secure.getInt(r4, r5, r6);
        if (r4 == 0) goto L_0x00c6;
    L_0x005d:
        r45 = 1;
    L_0x005f:
        r0 = r49;
        r5 = r0.mService;
        monitor-enter(r5);
        com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();	 Catch:{ all -> 0x0464 }
        r0 = r49;	 Catch:{ all -> 0x0464 }
        r4 = r0.mService;	 Catch:{ all -> 0x0464 }
        r4 = r4.mShuttingDown;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x00c9;	 Catch:{ all -> 0x0464 }
    L_0x006f:
        r4 = TAG;	 Catch:{ all -> 0x0464 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0464 }
        r6.<init>();	 Catch:{ all -> 0x0464 }
        r7 = "During shutdown skipping ANR: ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r7 = " ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r54;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r6 = r6.toString();	 Catch:{ all -> 0x0464 }
        android.util.Slog.i(r4, r6);	 Catch:{ all -> 0x0464 }
        monitor-exit(r5);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return;
    L_0x009c:
        r19 = move-exception;
        r0 = r49;
        r4 = r0.mService;
        r5 = 0;
        r4.mController = r5;
        r4 = com.android.server.Watchdog.getInstance();
        r5 = 0;
        r4.setActivityController(r5);
        r0 = r49;
        r4 = r0.mService;
        r4 = r4.mOppoActivityControlerScheduler;
        if (r4 == 0) goto L_0x0040;
    L_0x00b4:
        r0 = r49;
        r4 = r0.mService;
        r4 = r4.mOppoActivityControlerScheduler;
        r4.exitRunningScheduler();
        r0 = r49;
        r4 = r0.mService;
        r5 = 0;
        r4.mOppoActivityControlerScheduler = r5;
        goto L_0x0040;
    L_0x00c6:
        r45 = 0;
        goto L_0x005f;
    L_0x00c9:
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r4 = r0.notResponding;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x00fc;	 Catch:{ all -> 0x0464 }
    L_0x00cf:
        r4 = TAG;	 Catch:{ all -> 0x0464 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0464 }
        r6.<init>();	 Catch:{ all -> 0x0464 }
        r7 = "Skipping duplicate ANR: ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r7 = " ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r54;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r6 = r6.toString();	 Catch:{ all -> 0x0464 }
        android.util.Slog.i(r4, r6);	 Catch:{ all -> 0x0464 }
        monitor-exit(r5);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return;
    L_0x00fc:
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r4 = r0.crashing;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x012f;	 Catch:{ all -> 0x0464 }
    L_0x0102:
        r4 = TAG;	 Catch:{ all -> 0x0464 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0464 }
        r6.<init>();	 Catch:{ all -> 0x0464 }
        r7 = "Crashing app skipping ANR: ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r7 = " ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r54;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r6 = r6.toString();	 Catch:{ all -> 0x0464 }
        android.util.Slog.i(r4, r6);	 Catch:{ all -> 0x0464 }
        monitor-exit(r5);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return;
    L_0x012f:
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r4 = r0.killedByAm;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x0162;	 Catch:{ all -> 0x0464 }
    L_0x0135:
        r4 = TAG;	 Catch:{ all -> 0x0464 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0464 }
        r6.<init>();	 Catch:{ all -> 0x0464 }
        r7 = "App already killed by AM skipping ANR: ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r7 = " ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r54;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r6 = r6.toString();	 Catch:{ all -> 0x0464 }
        android.util.Slog.i(r4, r6);	 Catch:{ all -> 0x0464 }
        monitor-exit(r5);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return;
    L_0x0162:
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r4 = r0.killed;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x0195;	 Catch:{ all -> 0x0464 }
    L_0x0168:
        r4 = TAG;	 Catch:{ all -> 0x0464 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0464 }
        r6.<init>();	 Catch:{ all -> 0x0464 }
        r7 = "Skipping died app ANR: ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r7 = " ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r54;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r6 = r6.toString();	 Catch:{ all -> 0x0464 }
        android.util.Slog.i(r4, r6);	 Catch:{ all -> 0x0464 }
        monitor-exit(r5);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return;
    L_0x0195:
        r4 = 1;
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r0.notResponding = r4;	 Catch:{ all -> 0x0464 }
        r4 = 5;	 Catch:{ all -> 0x0464 }
        r4 = new java.lang.Object[r4];	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r0.userId;	 Catch:{ all -> 0x0464 }
        r6 = java.lang.Integer.valueOf(r6);	 Catch:{ all -> 0x0464 }
        r7 = 0;	 Catch:{ all -> 0x0464 }
        r4[r7] = r6;	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r0.pid;	 Catch:{ all -> 0x0464 }
        r6 = java.lang.Integer.valueOf(r6);	 Catch:{ all -> 0x0464 }
        r7 = 1;	 Catch:{ all -> 0x0464 }
        r4[r7] = r6;	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r0.processName;	 Catch:{ all -> 0x0464 }
        r7 = 2;	 Catch:{ all -> 0x0464 }
        r4[r7] = r6;	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r0.info;	 Catch:{ all -> 0x0464 }
        r6 = r6.flags;	 Catch:{ all -> 0x0464 }
        r6 = java.lang.Integer.valueOf(r6);	 Catch:{ all -> 0x0464 }
        r7 = 3;	 Catch:{ all -> 0x0464 }
        r4[r7] = r6;	 Catch:{ all -> 0x0464 }
        r6 = 4;	 Catch:{ all -> 0x0464 }
        r4[r6] = r54;	 Catch:{ all -> 0x0464 }
        r6 = 30008; // 0x7538 float:4.205E-41 double:1.4826E-319;	 Catch:{ all -> 0x0464 }
        android.util.EventLog.writeEvent(r6, r4);	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r4 = r0.pid;	 Catch:{ all -> 0x0464 }
        r4 = java.lang.Integer.valueOf(r4);	 Catch:{ all -> 0x0464 }
        r0 = r21;	 Catch:{ all -> 0x0464 }
        r0.add(r4);	 Catch:{ all -> 0x0464 }
        r4 = "persist.sys.assert.panic";	 Catch:{ all -> 0x0464 }
        r6 = 0;	 Catch:{ all -> 0x0464 }
        r17 = android.os.SystemProperties.getBoolean(r4, r6);	 Catch:{ all -> 0x0464 }
        if (r45 != 0) goto L_0x0305;	 Catch:{ all -> 0x0464 }
    L_0x01e6:
        r4 = isInterestingForBackgroundTraces(r50);	 Catch:{ all -> 0x0464 }
        r27 = r4 ^ 1;	 Catch:{ all -> 0x0464 }
    L_0x01ec:
        if (r27 != 0) goto L_0x037c;	 Catch:{ all -> 0x0464 }
    L_0x01ee:
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r0 = r0.pid;	 Catch:{ all -> 0x0464 }
        r36 = r0;	 Catch:{ all -> 0x0464 }
        if (r52 == 0) goto L_0x020c;	 Catch:{ all -> 0x0464 }
    L_0x01f6:
        r0 = r52;	 Catch:{ all -> 0x0464 }
        r4 = r0.app;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x020c;	 Catch:{ all -> 0x0464 }
    L_0x01fc:
        r0 = r52;	 Catch:{ all -> 0x0464 }
        r4 = r0.app;	 Catch:{ all -> 0x0464 }
        r4 = r4.pid;	 Catch:{ all -> 0x0464 }
        if (r4 <= 0) goto L_0x020c;	 Catch:{ all -> 0x0464 }
    L_0x0204:
        r0 = r52;	 Catch:{ all -> 0x0464 }
        r4 = r0.app;	 Catch:{ all -> 0x0464 }
        r0 = r4.pid;	 Catch:{ all -> 0x0464 }
        r36 = r0;	 Catch:{ all -> 0x0464 }
    L_0x020c:
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r4 = r0.pid;	 Catch:{ all -> 0x0464 }
        r0 = r36;	 Catch:{ all -> 0x0464 }
        if (r0 == r4) goto L_0x021d;	 Catch:{ all -> 0x0464 }
    L_0x0214:
        r4 = java.lang.Integer.valueOf(r36);	 Catch:{ all -> 0x0464 }
        r0 = r21;	 Catch:{ all -> 0x0464 }
        r0.add(r4);	 Catch:{ all -> 0x0464 }
    L_0x021d:
        if (r17 == 0) goto L_0x0309;	 Catch:{ all -> 0x0464 }
    L_0x021f:
        r16 = 4;	 Catch:{ all -> 0x0464 }
    L_0x0221:
        r4 = "persist.sys.assert.stackdump";	 Catch:{ all -> 0x0464 }
        r0 = r16;	 Catch:{ all -> 0x0464 }
        r31 = android.os.SystemProperties.getInt(r4, r0);	 Catch:{ all -> 0x0464 }
        r39 = 0;	 Catch:{ all -> 0x0464 }
        r40 = 0;	 Catch:{ all -> 0x0464 }
        r4 = "persist.sys.assert.dumpsys";	 Catch:{ all -> 0x0464 }
        r0 = r17;	 Catch:{ all -> 0x0464 }
        r18 = android.os.SystemProperties.getBoolean(r4, r0);	 Catch:{ all -> 0x0464 }
        if (r18 == 0) goto L_0x0252;	 Catch:{ all -> 0x0464 }
    L_0x0239:
        r4 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x0464 }
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r6 = r0.pid;	 Catch:{ all -> 0x0464 }
        if (r4 == r6) goto L_0x0252;	 Catch:{ all -> 0x0464 }
    L_0x0241:
        r4 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x0464 }
        r0 = r36;	 Catch:{ all -> 0x0464 }
        if (r4 == r0) goto L_0x0252;	 Catch:{ all -> 0x0464 }
    L_0x0247:
        r4 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x0464 }
        r4 = java.lang.Integer.valueOf(r4);	 Catch:{ all -> 0x0464 }
        r0 = r21;	 Catch:{ all -> 0x0464 }
        r0.add(r4);	 Catch:{ all -> 0x0464 }
    L_0x0252:
        r0 = r49;	 Catch:{ all -> 0x0464 }
        r4 = r0.mInterestAnrAppProcNames;	 Catch:{ all -> 0x0464 }
        if (r4 != 0) goto L_0x0275;	 Catch:{ all -> 0x0464 }
    L_0x0258:
        r4 = new java.util.ArrayList;	 Catch:{ all -> 0x0464 }
        r4.<init>();	 Catch:{ all -> 0x0464 }
        r0 = r49;	 Catch:{ all -> 0x0464 }
        r0.mInterestAnrAppProcNames = r4;	 Catch:{ all -> 0x0464 }
        r0 = r49;	 Catch:{ all -> 0x0464 }
        r4 = r0.mInterestAnrAppProcNames;	 Catch:{ all -> 0x0464 }
        r6 = "android.process.media";	 Catch:{ all -> 0x0464 }
        r4.add(r6);	 Catch:{ all -> 0x0464 }
        r0 = r49;	 Catch:{ all -> 0x0464 }
        r4 = r0.mInterestAnrAppProcNames;	 Catch:{ all -> 0x0464 }
        r6 = "com.android.phone";	 Catch:{ all -> 0x0464 }
        r4.add(r6);	 Catch:{ all -> 0x0464 }
    L_0x0275:
        r0 = r49;	 Catch:{ all -> 0x0464 }
        r4 = r0.mService;	 Catch:{ all -> 0x0464 }
        r4 = r4.mLruProcesses;	 Catch:{ all -> 0x0464 }
        r4 = r4.size();	 Catch:{ all -> 0x0464 }
        r23 = r4 + -1;	 Catch:{ all -> 0x0464 }
    L_0x0281:
        if (r23 < 0) goto L_0x037c;	 Catch:{ all -> 0x0464 }
    L_0x0283:
        r0 = r49;	 Catch:{ all -> 0x0464 }
        r4 = r0.mService;	 Catch:{ all -> 0x0464 }
        r4 = r4.mLruProcesses;	 Catch:{ all -> 0x0464 }
        r0 = r23;	 Catch:{ all -> 0x0464 }
        r42 = r4.get(r0);	 Catch:{ all -> 0x0464 }
        r42 = (com.android.server.am.ProcessRecord) r42;	 Catch:{ all -> 0x0464 }
        if (r42 == 0) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x0293:
        r0 = r42;	 Catch:{ all -> 0x0464 }
        r4 = r0.thread;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x0299:
        r0 = r42;	 Catch:{ all -> 0x0464 }
        r0 = r0.pid;	 Catch:{ all -> 0x0464 }
        r37 = r0;	 Catch:{ all -> 0x0464 }
        if (r37 <= 0) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x02a1:
        r0 = r50;	 Catch:{ all -> 0x0464 }
        r4 = r0.pid;	 Catch:{ all -> 0x0464 }
        r0 = r37;	 Catch:{ all -> 0x0464 }
        if (r0 == r4) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x02a9:
        r0 = r37;	 Catch:{ all -> 0x0464 }
        r1 = r36;	 Catch:{ all -> 0x0464 }
        if (r0 == r1) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x02af:
        r4 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x0464 }
        r0 = r37;	 Catch:{ all -> 0x0464 }
        if (r0 == r4) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x02b5:
        r0 = r42;	 Catch:{ all -> 0x0464 }
        r4 = r0.processName;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x030d;	 Catch:{ all -> 0x0464 }
    L_0x02bb:
        r0 = r49;	 Catch:{ all -> 0x0464 }
        r4 = r0.mInterestAnrAppProcNames;	 Catch:{ all -> 0x0464 }
        r0 = r42;	 Catch:{ all -> 0x0464 }
        r6 = r0.processName;	 Catch:{ all -> 0x0464 }
        r26 = r4.contains(r6);	 Catch:{ all -> 0x0464 }
    L_0x02c7:
        r0 = r42;	 Catch:{ all -> 0x0464 }
        r4 = r0.persistent;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x0310;	 Catch:{ all -> 0x0464 }
    L_0x02cd:
        r0 = r39;	 Catch:{ all -> 0x0464 }
        r1 = r31;	 Catch:{ all -> 0x0464 }
        if (r0 < r1) goto L_0x02d5;	 Catch:{ all -> 0x0464 }
    L_0x02d3:
        if (r26 == 0) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x02d5:
        r4 = java.lang.Integer.valueOf(r37);	 Catch:{ all -> 0x0464 }
        r0 = r21;	 Catch:{ all -> 0x0464 }
        r0.add(r4);	 Catch:{ all -> 0x0464 }
        r4 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_ANR;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x02fd;	 Catch:{ all -> 0x0464 }
    L_0x02e2:
        r4 = TAG;	 Catch:{ all -> 0x0464 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0464 }
        r6.<init>();	 Catch:{ all -> 0x0464 }
        r7 = "Adding persistent proc: ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r42;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r6 = r6.toString();	 Catch:{ all -> 0x0464 }
        android.util.Slog.i(r4, r6);	 Catch:{ all -> 0x0464 }
    L_0x02fd:
        if (r26 != 0) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x02ff:
        r39 = r39 + 1;	 Catch:{ all -> 0x0464 }
    L_0x0301:
        r23 = r23 + -1;	 Catch:{ all -> 0x0464 }
        goto L_0x0281;	 Catch:{ all -> 0x0464 }
    L_0x0305:
        r27 = 0;	 Catch:{ all -> 0x0464 }
        goto L_0x01ec;	 Catch:{ all -> 0x0464 }
    L_0x0309:
        r16 = 2;	 Catch:{ all -> 0x0464 }
        goto L_0x0221;	 Catch:{ all -> 0x0464 }
    L_0x030d:
        r26 = 0;	 Catch:{ all -> 0x0464 }
        goto L_0x02c7;	 Catch:{ all -> 0x0464 }
    L_0x0310:
        r0 = r42;	 Catch:{ all -> 0x0464 }
        r4 = r0.treatLikeActivity;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x0347;	 Catch:{ all -> 0x0464 }
    L_0x0316:
        r0 = r40;	 Catch:{ all -> 0x0464 }
        r1 = r31;	 Catch:{ all -> 0x0464 }
        if (r0 >= r1) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x031c:
        r4 = java.lang.Integer.valueOf(r37);	 Catch:{ all -> 0x0464 }
        r0 = r21;	 Catch:{ all -> 0x0464 }
        r0.add(r4);	 Catch:{ all -> 0x0464 }
        r4 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_ANR;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x0344;	 Catch:{ all -> 0x0464 }
    L_0x0329:
        r4 = TAG;	 Catch:{ all -> 0x0464 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0464 }
        r6.<init>();	 Catch:{ all -> 0x0464 }
        r7 = "Adding likely IME: ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r42;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r6 = r6.toString();	 Catch:{ all -> 0x0464 }
        android.util.Slog.i(r4, r6);	 Catch:{ all -> 0x0464 }
    L_0x0344:
        r40 = r40 + 1;	 Catch:{ all -> 0x0464 }
        goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x0347:
        r0 = r40;	 Catch:{ all -> 0x0464 }
        r1 = r31;	 Catch:{ all -> 0x0464 }
        if (r0 < r1) goto L_0x034f;	 Catch:{ all -> 0x0464 }
    L_0x034d:
        if (r26 == 0) goto L_0x0301;	 Catch:{ all -> 0x0464 }
    L_0x034f:
        r4 = java.lang.Boolean.TRUE;	 Catch:{ all -> 0x0464 }
        r0 = r28;	 Catch:{ all -> 0x0464 }
        r1 = r37;	 Catch:{ all -> 0x0464 }
        r0.put(r1, r4);	 Catch:{ all -> 0x0464 }
        r4 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_ANR;	 Catch:{ all -> 0x0464 }
        if (r4 == 0) goto L_0x0377;	 Catch:{ all -> 0x0464 }
    L_0x035c:
        r4 = TAG;	 Catch:{ all -> 0x0464 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0464 }
        r6.<init>();	 Catch:{ all -> 0x0464 }
        r7 = "Adding ANR proc: ";	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r7);	 Catch:{ all -> 0x0464 }
        r0 = r42;	 Catch:{ all -> 0x0464 }
        r6 = r6.append(r0);	 Catch:{ all -> 0x0464 }
        r6 = r6.toString();	 Catch:{ all -> 0x0464 }
        android.util.Slog.i(r4, r6);	 Catch:{ all -> 0x0464 }
    L_0x0377:
        if (r26 != 0) goto L_0x0301;
    L_0x0379:
        r40 = r40 + 1;
        goto L_0x0301;
    L_0x037c:
        monitor-exit(r5);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        r25 = new java.lang.StringBuilder;
        r25.<init>();
        r4 = 0;
        r0 = r25;
        r0.setLength(r4);
        r4 = "ANR in ";
        r0 = r25;
        r4 = r0.append(r4);
        r0 = r50;
        r5 = r0.processName;
        r4.append(r5);
        if (r51 == 0) goto L_0x03ba;
    L_0x039d:
        r0 = r51;
        r4 = r0.shortComponentName;
        if (r4 == 0) goto L_0x03ba;
    L_0x03a3:
        r4 = " (";
        r0 = r25;
        r4 = r0.append(r4);
        r0 = r51;
        r5 = r0.shortComponentName;
        r4 = r4.append(r5);
        r5 = ")";
        r4.append(r5);
    L_0x03ba:
        r4 = "\n";
        r0 = r25;
        r0.append(r4);
        r4 = "PID: ";
        r0 = r25;
        r4 = r0.append(r4);
        r0 = r50;
        r5 = r0.pid;
        r4 = r4.append(r5);
        r5 = "\n";
        r4.append(r5);
        if (r54 == 0) goto L_0x03f0;
    L_0x03db:
        r4 = "Reason: ";
        r0 = r25;
        r4 = r0.append(r4);
        r0 = r54;
        r4 = r4.append(r0);
        r5 = "\n";
        r4.append(r5);
    L_0x03f0:
        if (r52 == 0) goto L_0x040f;
    L_0x03f2:
        r0 = r52;
        r1 = r51;
        if (r0 == r1) goto L_0x040f;
    L_0x03f8:
        r4 = "Parent: ";
        r0 = r25;
        r4 = r0.append(r4);
        r0 = r52;
        r5 = r0.shortComponentName;
        r4 = r4.append(r5);
        r5 = "\n";
        r4.append(r5);
    L_0x040f:
        r41 = new com.android.internal.os.ProcessCpuTracker;
        r4 = 1;
        r0 = r41;
        r0.<init>(r4);
        r34 = 0;
        if (r27 == 0) goto L_0x046d;
    L_0x041b:
        r23 = 0;
    L_0x041d:
        r4 = com.android.server.Watchdog.NATIVE_STACKS_OF_INTEREST;
        r4 = r4.length;
        r0 = r23;
        if (r0 >= r4) goto L_0x043e;
    L_0x0424:
        r4 = com.android.server.Watchdog.NATIVE_STACKS_OF_INTEREST;
        r4 = r4[r23];
        r0 = r50;
        r5 = r0.processName;
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x046a;
    L_0x0432:
        r4 = 1;
        r0 = new java.lang.String[r4];
        r34 = r0;
        r0 = r50;
        r4 = r0.processName;
        r5 = 0;
        r34[r5] = r4;
    L_0x043e:
        if (r34 != 0) goto L_0x047d;
    L_0x0440:
        r38 = 0;
    L_0x0442:
        r33 = 0;
        if (r38 == 0) goto L_0x0482;
    L_0x0446:
        r33 = new java.util.ArrayList;
        r0 = r38;
        r4 = r0.length;
        r0 = r33;
        r0.<init>(r4);
        r4 = 0;
        r0 = r38;
        r5 = r0.length;
    L_0x0454:
        if (r4 >= r5) goto L_0x0482;
    L_0x0456:
        r23 = r38[r4];
        r6 = java.lang.Integer.valueOf(r23);
        r0 = r33;
        r0.add(r6);
        r4 = r4 + 1;
        goto L_0x0454;
    L_0x0464:
        r4 = move-exception;
        monitor-exit(r5);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        throw r4;
    L_0x046a:
        r23 = r23 + 1;
        goto L_0x041d;
    L_0x046d:
        r4 = "persist.sys.assert.nativestack";
        r5 = 0;
        r24 = android.os.SystemProperties.getBoolean(r4, r5);
        if (r24 == 0) goto L_0x047a;
    L_0x0477:
        r34 = com.android.server.Watchdog.NATIVE_STACKS_OF_INTEREST;
        goto L_0x043e;
    L_0x047a:
        r34 = LIGHTWEIGHT_NATIVE_STACKS_OF_INTEREST;
        goto L_0x043e;
    L_0x047d:
        r38 = android.os.Process.getPidsForCommands(r34);
        goto L_0x0442;
    L_0x0482:
        if (r27 == 0) goto L_0x0571;
    L_0x0484:
        r4 = 0;
    L_0x0485:
        if (r27 == 0) goto L_0x0489;
    L_0x0487:
        r28 = 0;
    L_0x0489:
        r5 = 1;
        r0 = r21;
        r1 = r28;
        r2 = r33;
        r12 = com.android.server.am.ActivityManagerService.dumpStackTraces(r5, r0, r4, r1, r2);
        r11 = 0;
        r0 = r49;
        r4 = r0.mService;
        r4.updateCpuStatsNow();
        r0 = r49;
        r4 = r0.mService;
        r5 = r4.mProcessCpuTracker;
        monitor-enter(r5);
        r0 = r49;	 Catch:{ all -> 0x0575 }
        r4 = r0.mService;	 Catch:{ all -> 0x0575 }
        r4 = r4.mProcessCpuTracker;	 Catch:{ all -> 0x0575 }
        r11 = r4.printCurrentState(r14);	 Catch:{ all -> 0x0575 }
        monitor-exit(r5);
        r4 = r41.printCurrentLoad();
        r0 = r25;
        r0.append(r4);
        r0 = r25;
        r0.append(r11);
        r0 = r41;
        r4 = r0.printCurrentState(r14);
        r0 = r25;
        r0.append(r4);
        r4 = TAG;
        r5 = r25.toString();
        android.util.Slog.e(r4, r5);
        if (r12 != 0) goto L_0x04da;
    L_0x04d2:
        r0 = r50;
        r4 = r0.pid;
        r5 = 3;
        android.os.Process.sendSignal(r4, r5);
    L_0x04da:
        if (r12 == 0) goto L_0x0526;
    L_0x04dc:
        r44 = new java.text.SimpleDateFormat;
        r4 = "yyyy-MM-dd-HH-mm-ss-SSS";
        r0 = r44;
        r0.<init>(r4);
        r4 = new java.util.Date;
        r4.<init>();
        r0 = r44;
        r22 = r0.format(r4);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "traces_";
        r4 = r4.append(r5);
        r0 = r50;
        r5 = r0.pid;
        r4 = r4.append(r5);
        r5 = "_";
        r4 = r4.append(r5);
        r0 = r22;
        r4 = r4.append(r0);
        r5 = ".txt";
        r4 = r4.append(r5);
        r47 = r4.toString();
        r4 = r12.getPath();
        r0 = r47;
        com.oppo.debug.ASSERT.copyAnr(r4, r0);
    L_0x0526:
        r0 = r49;
        r4 = r0.mService;
        r5 = "anr";
        r0 = r50;
        r7 = r0.processName;
        r13 = 0;
        r6 = r50;
        r8 = r51;
        r9 = r52;
        r10 = r54;
        r4.addErrorToDropBox(r5, r6, r7, r8, r9, r10, r11, r12, r13);
        r0 = r49;
        r4 = r0.mService;
        r4 = r4.mController;
        if (r4 == 0) goto L_0x05b8;
    L_0x0545:
        r0 = r49;	 Catch:{ RemoteException -> 0x0590 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0590 }
        r4 = r4.mController;	 Catch:{ RemoteException -> 0x0590 }
        r0 = r50;	 Catch:{ RemoteException -> 0x0590 }
        r5 = r0.processName;	 Catch:{ RemoteException -> 0x0590 }
        r0 = r50;	 Catch:{ RemoteException -> 0x0590 }
        r6 = r0.pid;	 Catch:{ RemoteException -> 0x0590 }
        r7 = r25.toString();	 Catch:{ RemoteException -> 0x0590 }
        r43 = r4.appNotResponding(r5, r6, r7);	 Catch:{ RemoteException -> 0x0590 }
        if (r43 == 0) goto L_0x05b8;	 Catch:{ RemoteException -> 0x0590 }
    L_0x055d:
        if (r43 >= 0) goto L_0x0578;	 Catch:{ RemoteException -> 0x0590 }
    L_0x055f:
        r0 = r50;	 Catch:{ RemoteException -> 0x0590 }
        r4 = r0.pid;	 Catch:{ RemoteException -> 0x0590 }
        r5 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ RemoteException -> 0x0590 }
        if (r4 == r5) goto L_0x0578;	 Catch:{ RemoteException -> 0x0590 }
    L_0x0567:
        r4 = "anr";	 Catch:{ RemoteException -> 0x0590 }
        r5 = 1;	 Catch:{ RemoteException -> 0x0590 }
        r0 = r50;	 Catch:{ RemoteException -> 0x0590 }
        r0.kill(r4, r5);	 Catch:{ RemoteException -> 0x0590 }
    L_0x0570:
        return;
    L_0x0571:
        r4 = r41;
        goto L_0x0485;
    L_0x0575:
        r4 = move-exception;
        monitor-exit(r5);
        throw r4;
    L_0x0578:
        r0 = r49;	 Catch:{ RemoteException -> 0x0590 }
        r5 = r0.mService;	 Catch:{ RemoteException -> 0x0590 }
        monitor-enter(r5);	 Catch:{ RemoteException -> 0x0590 }
        com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();	 Catch:{ all -> 0x05e1 }
        r0 = r49;	 Catch:{ all -> 0x05e1 }
        r4 = r0.mService;	 Catch:{ all -> 0x05e1 }
        r4 = r4.mServices;	 Catch:{ all -> 0x05e1 }
        r0 = r50;	 Catch:{ all -> 0x05e1 }
        r4.scheduleServiceTimeoutLocked(r0);	 Catch:{ all -> 0x05e1 }
        monitor-exit(r5);	 Catch:{ RemoteException -> 0x0590 }
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();	 Catch:{ RemoteException -> 0x0590 }
        goto L_0x0570;
    L_0x0590:
        r19 = move-exception;
        r0 = r49;
        r4 = r0.mService;
        r5 = 0;
        r4.mController = r5;
        r4 = com.android.server.Watchdog.getInstance();
        r5 = 0;
        r4.setActivityController(r5);
        r0 = r49;
        r4 = r0.mService;
        r4 = r4.mOppoActivityControlerScheduler;
        if (r4 == 0) goto L_0x05b8;
    L_0x05a8:
        r0 = r49;
        r4 = r0.mService;
        r4 = r4.mOppoActivityControlerScheduler;
        r4.exitRunningScheduler();
        r0 = r49;
        r4 = r0.mService;
        r5 = 0;
        r4.mOppoActivityControlerScheduler = r5;
    L_0x05b8:
        r0 = r49;
        r6 = r0.mService;
        monitor-enter(r6);
        com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();	 Catch:{ all -> 0x0707 }
        r0 = r49;	 Catch:{ all -> 0x0707 }
        r4 = r0.mService;	 Catch:{ all -> 0x0707 }
        r4 = r4.mBatteryStatsService;	 Catch:{ all -> 0x0707 }
        r0 = r50;	 Catch:{ all -> 0x0707 }
        r5 = r0.processName;	 Catch:{ all -> 0x0707 }
        r0 = r50;	 Catch:{ all -> 0x0707 }
        r7 = r0.uid;	 Catch:{ all -> 0x0707 }
        r4.noteProcessAnr(r5, r7);	 Catch:{ all -> 0x0707 }
        if (r27 == 0) goto L_0x05e7;	 Catch:{ all -> 0x0707 }
    L_0x05d3:
        r4 = "bg anr";	 Catch:{ all -> 0x0707 }
        r5 = 1;	 Catch:{ all -> 0x0707 }
        r0 = r50;	 Catch:{ all -> 0x0707 }
        r0.kill(r4, r5);	 Catch:{ all -> 0x0707 }
        monitor-exit(r6);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return;
    L_0x05e1:
        r4 = move-exception;
        monitor-exit(r5);	 Catch:{ RemoteException -> 0x0590 }
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();	 Catch:{ RemoteException -> 0x0590 }
        throw r4;	 Catch:{ RemoteException -> 0x0590 }
    L_0x05e7:
        if (r51 == 0) goto L_0x06dd;
    L_0x05e9:
        r0 = r51;	 Catch:{ all -> 0x0707 }
        r4 = r0.shortComponentName;	 Catch:{ all -> 0x0707 }
        r5 = r4;	 Catch:{ all -> 0x0707 }
    L_0x05ee:
        if (r54 == 0) goto L_0x06e1;	 Catch:{ all -> 0x0707 }
    L_0x05f0:
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0707 }
        r4.<init>();	 Catch:{ all -> 0x0707 }
        r7 = "ANR ";	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r7);	 Catch:{ all -> 0x0707 }
        r0 = r54;	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r0);	 Catch:{ all -> 0x0707 }
        r4 = r4.toString();	 Catch:{ all -> 0x0707 }
    L_0x0606:
        r7 = r25.toString();	 Catch:{ all -> 0x0707 }
        r0 = r49;	 Catch:{ all -> 0x0707 }
        r1 = r50;	 Catch:{ all -> 0x0707 }
        r0.makeAppNotRespondingLocked(r1, r5, r4, r7);	 Catch:{ all -> 0x0707 }
        r4 = "persist.sys.enableTraceRename";	 Catch:{ all -> 0x0707 }
        r5 = 0;	 Catch:{ all -> 0x0707 }
        r20 = android.os.SystemProperties.getBoolean(r4, r5);	 Catch:{ all -> 0x0707 }
        if (r20 == 0) goto L_0x069c;	 Catch:{ all -> 0x0707 }
    L_0x061b:
        r4 = "dalvik.vm.stack-trace-file";	 Catch:{ all -> 0x0707 }
        r5 = 0;	 Catch:{ all -> 0x0707 }
        r48 = android.os.SystemProperties.get(r4, r5);	 Catch:{ all -> 0x0707 }
        if (r48 == 0) goto L_0x069c;	 Catch:{ all -> 0x0707 }
    L_0x0625:
        r4 = r48.length();	 Catch:{ all -> 0x0707 }
        if (r4 == 0) goto L_0x069c;	 Catch:{ all -> 0x0707 }
    L_0x062b:
        r46 = new java.io.File;	 Catch:{ all -> 0x0707 }
        r0 = r46;	 Catch:{ all -> 0x0707 }
        r1 = r48;	 Catch:{ all -> 0x0707 }
        r0.<init>(r1);	 Catch:{ all -> 0x0707 }
        r4 = ".";	 Catch:{ all -> 0x0707 }
        r0 = r48;	 Catch:{ all -> 0x0707 }
        r29 = r0.lastIndexOf(r4);	 Catch:{ all -> 0x0707 }
        r4 = -1;	 Catch:{ all -> 0x0707 }
        r0 = r29;	 Catch:{ all -> 0x0707 }
        if (r4 == r0) goto L_0x06e6;	 Catch:{ all -> 0x0707 }
    L_0x0642:
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0707 }
        r4.<init>();	 Catch:{ all -> 0x0707 }
        r5 = 0;	 Catch:{ all -> 0x0707 }
        r0 = r48;	 Catch:{ all -> 0x0707 }
        r1 = r29;	 Catch:{ all -> 0x0707 }
        r5 = r0.substring(r5, r1);	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r5);	 Catch:{ all -> 0x0707 }
        r5 = "_";	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r5);	 Catch:{ all -> 0x0707 }
        r0 = r50;	 Catch:{ all -> 0x0707 }
        r5 = r0.processName;	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r5);	 Catch:{ all -> 0x0707 }
        r5 = "_";	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r5);	 Catch:{ all -> 0x0707 }
        r0 = r49;	 Catch:{ all -> 0x0707 }
        r5 = r0.mTraceDateFormat;	 Catch:{ all -> 0x0707 }
        r7 = new java.util.Date;	 Catch:{ all -> 0x0707 }
        r7.<init>();	 Catch:{ all -> 0x0707 }
        r5 = r5.format(r7);	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r5);	 Catch:{ all -> 0x0707 }
        r0 = r48;	 Catch:{ all -> 0x0707 }
        r1 = r29;	 Catch:{ all -> 0x0707 }
        r5 = r0.substring(r1);	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r5);	 Catch:{ all -> 0x0707 }
        r35 = r4.toString();	 Catch:{ all -> 0x0707 }
    L_0x068b:
        r4 = new java.io.File;	 Catch:{ all -> 0x0707 }
        r0 = r35;	 Catch:{ all -> 0x0707 }
        r4.<init>(r0);	 Catch:{ all -> 0x0707 }
        r0 = r46;	 Catch:{ all -> 0x0707 }
        r0.renameTo(r4);	 Catch:{ all -> 0x0707 }
        r4 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;	 Catch:{ all -> 0x0707 }
        android.os.SystemClock.sleep(r4);	 Catch:{ all -> 0x0707 }
    L_0x069c:
        r32 = android.os.Message.obtain();	 Catch:{ all -> 0x0707 }
        r30 = new java.util.HashMap;	 Catch:{ all -> 0x0707 }
        r30.<init>();	 Catch:{ all -> 0x0707 }
        r4 = 2;	 Catch:{ all -> 0x0707 }
        r0 = r32;	 Catch:{ all -> 0x0707 }
        r0.what = r4;	 Catch:{ all -> 0x0707 }
        r0 = r30;	 Catch:{ all -> 0x0707 }
        r1 = r32;	 Catch:{ all -> 0x0707 }
        r1.obj = r0;	 Catch:{ all -> 0x0707 }
        if (r53 == 0) goto L_0x0705;	 Catch:{ all -> 0x0707 }
    L_0x06b2:
        r4 = 1;	 Catch:{ all -> 0x0707 }
    L_0x06b3:
        r0 = r32;	 Catch:{ all -> 0x0707 }
        r0.arg1 = r4;	 Catch:{ all -> 0x0707 }
        r4 = "app";	 Catch:{ all -> 0x0707 }
        r0 = r30;	 Catch:{ all -> 0x0707 }
        r1 = r50;	 Catch:{ all -> 0x0707 }
        r0.put(r4, r1);	 Catch:{ all -> 0x0707 }
        if (r51 == 0) goto L_0x06cd;	 Catch:{ all -> 0x0707 }
    L_0x06c3:
        r4 = "activity";	 Catch:{ all -> 0x0707 }
        r0 = r30;	 Catch:{ all -> 0x0707 }
        r1 = r51;	 Catch:{ all -> 0x0707 }
        r0.put(r4, r1);	 Catch:{ all -> 0x0707 }
    L_0x06cd:
        r0 = r49;	 Catch:{ all -> 0x0707 }
        r4 = r0.mService;	 Catch:{ all -> 0x0707 }
        r4 = r4.mUiHandler;	 Catch:{ all -> 0x0707 }
        r0 = r32;	 Catch:{ all -> 0x0707 }
        r4.sendMessage(r0);	 Catch:{ all -> 0x0707 }
        monitor-exit(r6);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return;
    L_0x06dd:
        r4 = 0;
        r5 = r4;
        goto L_0x05ee;
    L_0x06e1:
        r4 = "ANR";	 Catch:{ all -> 0x0707 }
        goto L_0x0606;	 Catch:{ all -> 0x0707 }
    L_0x06e6:
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0707 }
        r4.<init>();	 Catch:{ all -> 0x0707 }
        r0 = r48;	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r0);	 Catch:{ all -> 0x0707 }
        r5 = "_";	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r5);	 Catch:{ all -> 0x0707 }
        r0 = r50;	 Catch:{ all -> 0x0707 }
        r5 = r0.processName;	 Catch:{ all -> 0x0707 }
        r4 = r4.append(r5);	 Catch:{ all -> 0x0707 }
        r35 = r4.toString();	 Catch:{ all -> 0x0707 }
        goto L_0x068b;
    L_0x0705:
        r4 = 0;
        goto L_0x06b3;
    L_0x0707:
        r4 = move-exception;
        monitor-exit(r6);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.AppErrors.appNotRespondingInner(com.android.server.am.ProcessRecord, com.android.server.am.ActivityRecord, com.android.server.am.ActivityRecord, boolean, java.lang.String):void");
    }

    private void makeAppNotRespondingLocked(ProcessRecord app, String activity, String shortMsg, String longMsg) {
        app.notResponding = true;
        app.notRespondingReport = generateProcessError(app, 2, activity, shortMsg, longMsg, null);
        startAppProblemLocked(app);
        app.stopFreezingAllLocked();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void handleShowAnrUi(Message msg) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                HashMap<String, Object> data = msg.obj;
                ProcessRecord proc = (ProcessRecord) data.get("app");
                if (proc == null || proc.anrDialog == null) {
                    Intent intent = new Intent("android.intent.action.ANR");
                    if (!this.mService.mProcessesReady) {
                        intent.addFlags(1342177280);
                    }
                    this.mService.broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, 0);
                    boolean showBackground = Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
                    if (!this.mService.canShowErrorDialogs() && !showBackground) {
                        MetricsLogger.action(this.mContext, 317, -1);
                        this.mService.killAppAtUsersRequest(proc, null);
                    } else if (SystemProperties.getBoolean("persist.sys.assert.panic", false)) {
                        proc.anrDialog = new AppNotRespondingDialog(this.mService, this.mContext, proc, (ActivityRecord) data.get(OppoAppStartupManager.TYPE_ACTIVITY), msg.arg1 != 0);
                    } else {
                        if (proc.anrDialog != null) {
                            Slog.w(TAG, " Dismiss app ANR dialog : " + proc.processName);
                            proc.anrDialog = null;
                        }
                        this.mService.killAppAtUsersRequest(proc, null);
                    }
                    Dialog d = proc.anrDialog;
                } else {
                    Slog.e(TAG, "App already has anr dialog: " + proc);
                    MetricsLogger.action(this.mContext, 317, -2);
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
