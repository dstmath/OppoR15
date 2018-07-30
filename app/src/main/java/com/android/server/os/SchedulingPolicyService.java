package com.android.server.os;

import android.os.Binder;
import android.os.ISchedulingPolicyService.Stub;
import android.os.Process;
import android.util.Log;
import vendor.oppo.hardware.biometrics.fingerprint.V2_1.FingerprintAcquiredInfo;

public class SchedulingPolicyService extends Stub {
    private static final int PRIORITY_MAX = 3;
    private static final int PRIORITY_MIN = 1;
    private static final String TAG = "SchedulingPolicyService";

    public int requestPriority(int pid, int tid, int prio, boolean isForApp) {
        if (!isPermitted() || prio < 1 || prio > 3 || Process.getThreadGroupLeader(tid) != pid) {
            return -1;
        }
        if (Binder.getCallingUid() != FingerprintAcquiredInfo.ACQUIRED_ALREADY_ENROLLED) {
            try {
                Process.setThreadGroup(tid, !isForApp ? 4 : 6);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed setThreadGroup: " + e);
                return -1;
            }
        }
        try {
            Process.setThreadScheduler(tid, 1073741825, prio);
            return 0;
        } catch (RuntimeException e2) {
            Log.e(TAG, "Failed setThreadScheduler: " + e2);
            return -1;
        }
    }

    private boolean isPermitted() {
        if (Binder.getCallingPid() == Process.myPid()) {
            return true;
        }
        switch (Binder.getCallingUid()) {
            case FingerprintAcquiredInfo.ACQUIRED_ALREADY_ENROLLED /*1002*/:
            case 1041:
            case 1047:
                return true;
            default:
                return false;
        }
    }
}
