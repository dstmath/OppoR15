package com.android.server.am;

import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import java.util.HashMap;

public class OppoProcessManagerHelper {
    private static final int BROADCAST_TIMEOUT_LIST_LENGTH = 30;
    private static final int SUPEND_TIME_INTERVAL = 20000;
    private static final String TAG = "OppoProcessManager";
    private static ActivityManagerService sActivityManagerService = null;
    private static HashMap<String, Long> sBroadcastTimeOutMap = new HashMap();
    private static boolean sDebugDetail = OppoProcessManager.sDebugDetail;
    public static OppoProcessManager sOppoProcessManager = null;

    public static void init(ActivityManagerService ams) {
        OppoProcessManager.getInstance().init(ams);
        sOppoProcessManager = OppoProcessManager.getInstance();
        sActivityManagerService = ams;
    }

    static final void resumeProvider(ContentProviderRecord cpr) {
        if (sOppoProcessManager.isEnable() && cpr.proc != null) {
            sOppoProcessManager.resumeProcess(cpr.proc, 3);
        }
    }

    static final boolean checkProcessCanRestart(ProcessRecord app) {
        return sOppoProcessManager.checkProcessCanRestart(app);
    }

    static final void checkAppInLaunchingProviders(ProcessRecord app) {
        ContentProviderRecord cpr;
        Slog.i("OppoProcessManager", app + " died but not restart......");
        if (!app.pubProviders.isEmpty()) {
            for (ContentProviderRecord cpr2 : app.pubProviders.values()) {
                sActivityManagerService.removeDyingProviderLocked(app, cpr2, true);
                cpr2.provider = null;
                cpr2.proc = null;
            }
            app.pubProviders.clear();
        }
        int length = sActivityManagerService.mLaunchingProviders.size();
        for (int i = 0; i < length; i++) {
            cpr2 = (ContentProviderRecord) sActivityManagerService.mLaunchingProviders.get(i);
            if (cpr2.launchingApp == app) {
                sActivityManagerService.removeDyingProviderLocked(app, cpr2, true);
                length = sActivityManagerService.mLaunchingProviders.size();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static final void resumeProcessForService(ServiceRecord r, boolean fg, String why) {
        if (sOppoProcessManager.isEnable() && r != null && r.app != null && !sOppoProcessManager.checkWhiteProcessRecord(r.app)) {
            if (isInStrictMode()) {
                if (sOppoProcessManager.mDynamicDebug) {
                    Slog.i("OppoProcessManager", "resumeProcessForService uid = " + r.app.uid + "  r = " + r + "  fg = " + fg + "  why = " + why);
                    Slog.i("OppoProcessManager", Log.getStackTraceString(new Throwable()));
                }
                sOppoProcessManager.resumeProcess(r.app, 2);
            } else {
                sOppoProcessManager.resumeProcessByUID(r.app.uid, 2);
            }
        }
    }

    static final void resumeProcessForSystemCall(ProcessRecord proc) {
        if (proc != null && isFrozingByPid(proc.pid)) {
            setProcessResume(proc.pid);
            recordResumeLog(proc.pid, OppoProcessManager.RESUME_REASON_SYSTEM_CALL_STR);
        }
    }

    static final boolean checkProcessWhileTimeout(ProcessRecord proc) {
        if (proc == null) {
            return false;
        }
        if (proc != null && proc.uid < 10000) {
            return false;
        }
        Slog.i("OppoProcessManager", "checkProcessWhileTimeout !  " + proc.processName);
        if (isFrozingByPid(proc.pid)) {
            if (isInStrictMode()) {
                Slog.i("OppoProcessManager", "service timeout for suspend, kill it in bg!  " + proc.processName);
                proc.killedByAm = true;
                Process.killProcessQuiet(proc.pid);
                return true;
            }
            Slog.i("OppoProcessManager", "service is suspend, resume it in background!  " + proc.processName);
            setProcessResume(proc.pid);
            recordResumeLog(proc.processName, OppoProcessManager.RESUME_REASON_SERVICE_TIMEOUT_STR);
            return true;
        } else if (!sOppoProcessManager.isUidGroupHasSuspended(proc)) {
            return false;
        } else {
            if (isInStrictMode()) {
                Slog.i("OppoProcessManager", "service timeout for the same uid's proc suspend, kill it in bg!  " + proc.processName);
                proc.killedByAm = true;
                Process.killProcessQuiet(proc.pid);
                return true;
            }
            Slog.i("OppoProcessManager", "checkProcessWhileTimeout the same uid's proc has suspend!  " + proc.processName);
            sOppoProcessManager.resumeProcessByUID(proc.uid, 11);
            return true;
        }
    }

    static final boolean checkProcessWhileBroadcastTimeout(ProcessRecord proc) {
        Slog.i("OppoProcessManager", "checkProcessWhileBroadcastTimeout proc = " + proc);
        if (proc == null) {
            return false;
        }
        if (proc != null && proc.uid < 10000) {
            return false;
        }
        Slog.i("OppoProcessManager", "checkProcessWhileBroadcastTimeout !  " + proc.processName);
        long time = SystemClock.elapsedRealtime();
        String procName = proc.processName;
        if (isFrozingByPid(proc.pid)) {
            Slog.i("OppoProcessManager", "broadcast proc is suspend, resume it in background!  " + procName);
            if (sBroadcastTimeOutMap.containsKey(procName)) {
                sBroadcastTimeOutMap.remove(procName);
                sBroadcastTimeOutMap.put(procName, Long.valueOf(time));
            } else {
                if (sBroadcastTimeOutMap.size() > 30) {
                    sBroadcastTimeOutMap.clear();
                }
                sBroadcastTimeOutMap.put(procName, Long.valueOf(time));
            }
            if (!isInStrictMode()) {
                setProcessResume(proc.pid);
                recordResumeLog(procName, OppoProcessManager.RESUME_REASON_BROADCAST_TIMEOUT_STR);
            }
            return true;
        } else if (sBroadcastTimeOutMap.containsKey(procName) && Math.abs(time - ((Long) sBroadcastTimeOutMap.get(procName)).longValue()) < 20000) {
            Slog.i("OppoProcessManager", "checkProcessWhileBroadcastTimeout maybe proc is suspend!  " + proc.processName);
            return true;
        } else if (!sOppoProcessManager.isUidGroupHasSuspended(proc)) {
            return false;
        } else {
            Slog.i("OppoProcessManager", "checkProcessWhileBroadcastTimeout the same uid's proc has suspend!  " + proc.processName);
            if (!isInStrictMode()) {
                sOppoProcessManager.resumeProcessByUID(proc.uid, 10);
            }
            return true;
        }
    }

    static final void updateProcessState(ProcessRecord app) {
        if (sActivityManagerService.mSystemReady) {
            sOppoProcessManager.updateProcessState(app);
        }
    }

    static final boolean checkBroadcast(BroadcastQueue queue, ProcessRecord app, BroadcastRecord r) throws RemoteException {
        if (!sOppoProcessManager.skipBroadcast(app, r, r.ordered)) {
            return true;
        }
        if (sOppoProcessManager.mDynamicDebug) {
            Slog.i("OppoProcessManager", "BPM skip: receiving " + r.intent.toString() + " to " + app.processName + " (pid=" + app.pid + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")" + " is ordered " + r.ordered);
        }
        queue.skipCurrentReceiverLocked(app);
        return false;
    }

    static final boolean skipBroadcast(BroadcastFilter filter, BroadcastRecord r, boolean ordered) {
        if (!sOppoProcessManager.skipBroadcast(filter.receiverList.app, r, ordered)) {
            return false;
        }
        if (sOppoProcessManager.mDynamicDebug) {
            Slog.i("OppoProcessManager", "BPM Denial: receiving " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ")" + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")" + " is ordered " + r.ordered + "   ordered " + ordered);
        }
        return true;
    }

    public static final void recordResumeLog(int pid, String reason) {
        if (sOppoProcessManager.mRecordSwitch) {
            if (sDebugDetail) {
                Slog.i("OppoProcessManager", "bpmhandle: resume " + pid + " reason is " + reason);
            }
            sOppoProcessManager.sendBpmMessage(pid, 140, 10000, sOppoProcessManager.strToReasonCode(reason));
        }
    }

    public static final void recordResumeLog(String processName, String reason) {
        if (sDebugDetail) {
            Slog.i("OppoProcessManager", "bpmhandle: resume " + processName + " reason is " + reason);
        }
        sOppoProcessManager.increaseResumeInfo(processName, sOppoProcessManager.strToReasonCode(reason));
    }

    public static final void resumeForMedia(int uid) {
        if (sDebugDetail) {
            Slog.i("OppoProcessManager", "bpmhandle: resume " + uid + " reason is media");
        }
        sOppoProcessManager.resumeProcessByUID(uid, 7);
    }

    public static final boolean isInStrictMode() {
        return sOppoProcessManager.isStrictMode();
    }

    public static final boolean isDelayAppAlarm(int callerPid, int callerUid, int calledUid, String calledPkg) {
        if (sOppoProcessManager.mDynamicDebug) {
            Slog.i("OppoProcessManager", "isDelayAppAlarm callerPid = " + callerPid + "  callerUid = " + callerUid + "  calledUid = " + calledUid + "  calledPkg = " + calledPkg);
        }
        return sOppoProcessManager.isDelayAppAlarm(callerPid, callerUid, calledUid, calledPkg);
    }

    public static final boolean isDelayAppSync(int uid, String pkg) {
        if (sOppoProcessManager.mDebugSwitch) {
            Slog.i("OppoProcessManager", "isDelayAppSync  Uid = " + uid + "  Pkg = " + pkg);
        }
        return sOppoProcessManager.isDelayAppSync(uid, pkg);
    }

    public static final boolean isDelayAppJob(int uid, String pkg) {
        if (sOppoProcessManager.mDebugSwitch) {
            Slog.i("OppoProcessManager", "isDelayAppJob  Uid = " + uid + "  Pkg = " + pkg);
        }
        return sOppoProcessManager.isDelayAppJob(uid, pkg);
    }

    public static final void enterStrictMode() {
        sOppoProcessManager.enterStrictMode();
    }

    public static final void stopStrictMode() {
        sOppoProcessManager.stopStrictMode();
    }

    public static final void resumeProcessByUID(int uid, String reason) {
        sOppoProcessManager.resumeProcessByUID(uid, sOppoProcessManager.strToReasonCode(reason));
    }

    public static final void handleAppDied(ProcessRecord app) {
        sOppoProcessManager.handleAppDied(app);
    }

    public static final boolean isFrozingByPid(int pid) {
        return sOppoProcessManager.isFrozingByPid(pid);
    }

    public static final void setProcessFrozen(int pid, String processName) {
        sOppoProcessManager.setProcessFrozen(pid, processName);
    }

    public static final void setProcessFrozen(int pid, String processName, int freezeLevel) {
        sOppoProcessManager.setProcessFrozen(pid, processName, freezeLevel);
    }

    public static final void setProcessResume(int pid) {
        sOppoProcessManager.setProcessResume(pid);
    }

    public static final void setProcessResume(int pid, int timeout, int isTargetFreeze) {
        sOppoProcessManager.setProcessResume(pid, timeout, isTargetFreeze);
    }

    public static final void noteWindowStateChange(int uid, int pid, int windowId, int windowType, boolean isVisible) {
        sOppoProcessManager.noteWindowStateChange(uid, pid, windowId, windowType, isVisible);
    }
}
