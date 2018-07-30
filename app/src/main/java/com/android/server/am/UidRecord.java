package com.android.server.am;

import android.app.ActivityManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;

public final class UidRecord {
    static final int CHANGE_ACTIVE = 4;
    static final int CHANGE_CACHED = 8;
    static final int CHANGE_GONE = 1;
    static final int CHANGE_IDLE = 2;
    static final int CHANGE_PROCSTATE = 0;
    static final int CHANGE_UNCACHED = 16;
    int curProcState;
    @GuardedBy("networkStateUpdate")
    long curProcStateSeq;
    boolean curWhitelist;
    boolean ephemeral;
    boolean foregroundServices;
    volatile boolean hasInternetPermission;
    boolean idle;
    long lastBackgroundTime;
    @GuardedBy("networkStateUpdate")
    long lastDispatchedProcStateSeq;
    @GuardedBy("networkStateUpdate")
    long lastNetworkUpdatedProcStateSeq;
    int lastReportedChange;
    final Object networkStateLock = new Object();
    int numProcs;
    ChangeItem pendingChange;
    boolean setIdle;
    int setProcState = 18;
    boolean setWhitelist;
    final int uid;
    volatile boolean waitingForNetwork;

    static final class ChangeItem {
        int change;
        boolean ephemeral;
        long procStateSeq;
        int processState;
        int uid;
        UidRecord uidRecord;

        ChangeItem() {
        }
    }

    public UidRecord(int _uid) {
        this.uid = _uid;
        this.idle = true;
        reset();
    }

    public void reset() {
        this.curProcState = 17;
        this.foregroundServices = false;
    }

    public void updateHasInternetPermission() {
        boolean z = false;
        if (ActivityManager.checkUidPermission(OppoPermissionConstants.PERMISSION_SEND_MMS_INTERNET, this.uid) == 0) {
            z = true;
        }
        this.hasInternetPermission = z;
    }

    public void updateLastDispatchedProcStateSeq(int changeToDispatch) {
        if ((changeToDispatch & 1) == 0) {
            this.lastDispatchedProcStateSeq = this.curProcStateSeq;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("UidRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        UserHandle.formatUid(sb, this.uid);
        sb.append(' ');
        sb.append(ProcessList.makeProcStateString(this.curProcState));
        if (this.ephemeral) {
            sb.append(" ephemeral");
        }
        if (this.foregroundServices) {
            sb.append(" fgServices");
        }
        if (this.curWhitelist) {
            sb.append(" whitelist");
        }
        if (this.lastBackgroundTime > 0) {
            sb.append(" bg:");
            TimeUtils.formatDuration(SystemClock.elapsedRealtime() - this.lastBackgroundTime, sb);
        }
        if (this.idle) {
            sb.append(" idle");
        }
        if (this.lastReportedChange != 0) {
            sb.append(" change:");
            boolean printed = false;
            if ((this.lastReportedChange & 1) != 0) {
                printed = true;
                sb.append("gone");
            }
            if ((this.lastReportedChange & 2) != 0) {
                if (printed) {
                    sb.append("|");
                }
                printed = true;
                sb.append("idle");
            }
            if ((this.lastReportedChange & 4) != 0) {
                if (printed) {
                    sb.append("|");
                }
                printed = true;
                sb.append("active");
            }
            if ((this.lastReportedChange & 8) != 0) {
                if (printed) {
                    sb.append("|");
                }
                printed = true;
                sb.append("cached");
            }
            if ((this.lastReportedChange & 16) != 0) {
                if (printed) {
                    sb.append("|");
                }
                sb.append("uncached");
            }
        }
        sb.append(" procs:");
        sb.append(this.numProcs);
        sb.append(" seq(");
        sb.append(this.curProcStateSeq);
        sb.append(",");
        sb.append(this.lastNetworkUpdatedProcStateSeq);
        sb.append(",");
        sb.append(this.lastDispatchedProcStateSeq);
        sb.append(")}");
        return sb.toString();
    }
}
