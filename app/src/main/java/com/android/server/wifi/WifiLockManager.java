package com.android.server.wifi;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class WifiLockManager {
    private static final String TAG = "WifiLockManager";
    private final IBatteryStats mBatteryStats;
    private final Context mContext;
    private int mFullHighPerfLocksAcquired;
    private int mFullHighPerfLocksReleased;
    private int mFullLocksAcquired;
    private int mFullLocksReleased;
    private int mScanLocksAcquired;
    private int mScanLocksReleased;
    private boolean mVerboseLoggingEnabled = false;
    private final List<WifiLock> mWifiLocks = new ArrayList();

    private class WifiLock implements DeathRecipient {
        IBinder mBinder;
        int mMode;
        String mTag;
        int mUid = Binder.getCallingUid();
        WorkSource mWorkSource;

        WifiLock(int lockMode, String tag, IBinder binder, WorkSource ws) {
            this.mTag = tag;
            this.mBinder = binder;
            this.mMode = lockMode;
            this.mWorkSource = ws;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        protected WorkSource getWorkSource() {
            return this.mWorkSource;
        }

        protected int getUid() {
            return this.mUid;
        }

        protected IBinder getBinder() {
            return this.mBinder;
        }

        public void binderDied() {
            WifiLockManager.this.releaseLock(this.mBinder);
        }

        public void unlinkDeathRecipient() {
            this.mBinder.unlinkToDeath(this, 0);
        }

        public String toString() {
            return "WifiLock{" + this.mTag + " type=" + this.mMode + " uid=" + this.mUid + "}";
        }
    }

    WifiLockManager(Context context, IBatteryStats batteryStats) {
        this.mContext = context;
        this.mBatteryStats = batteryStats;
    }

    public boolean acquireWifiLock(int lockMode, String tag, IBinder binder, WorkSource ws) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
        if (isValidLockMode(lockMode)) {
            if (ws == null || ws.size() == 0) {
                ws = new WorkSource(Binder.getCallingUid());
            } else {
                this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
            }
            return addLock(new WifiLock(lockMode, tag, binder, ws));
        }
        throw new IllegalArgumentException("lockMode =" + lockMode);
    }

    public boolean releaseWifiLock(IBinder binder) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
        return releaseLock(binder);
    }

    public synchronized int getStrongestLockMode() {
        if (this.mWifiLocks.isEmpty()) {
            return 0;
        }
        if (this.mFullHighPerfLocksAcquired > this.mFullHighPerfLocksReleased) {
            return 3;
        }
        if (this.mFullLocksAcquired > this.mFullLocksReleased) {
            return 1;
        }
        return 2;
    }

    public synchronized WorkSource createMergedWorkSource() {
        WorkSource mergedWS;
        mergedWS = new WorkSource();
        for (WifiLock lock : this.mWifiLocks) {
            mergedWS.add(lock.getWorkSource());
        }
        return mergedWS;
    }

    public synchronized void updateWifiLockWorkSource(IBinder binder, WorkSource ws) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
        WifiLock wl = findLockByBinder(binder);
        if (wl == null) {
            throw new IllegalArgumentException("Wifi lock not active");
        }
        WorkSource newWorkSource;
        if (ws == null || ws.size() == 0) {
            newWorkSource = new WorkSource(Binder.getCallingUid());
        } else {
            newWorkSource = new WorkSource(ws);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteFullWifiLockReleasedFromSource(wl.mWorkSource);
            wl.mWorkSource = newWorkSource;
            this.mBatteryStats.noteFullWifiLockAcquiredFromSource(wl.mWorkSource);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private static boolean isValidLockMode(int lockMode) {
        if (lockMode == 1 || lockMode == 2 || lockMode == 3) {
            return true;
        }
        return false;
    }

    private synchronized boolean addLock(WifiLock lock) {
        if (this.mVerboseLoggingEnabled) {
            Slog.d(TAG, "addLock: " + lock);
        }
        if (findLockByBinder(lock.getBinder()) != null) {
            if (this.mVerboseLoggingEnabled) {
                Slog.d(TAG, "attempted to add a lock when already holding one");
            }
            return false;
        }
        this.mWifiLocks.add(lock);
        boolean lockAdded = false;
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteFullWifiLockAcquiredFromSource(lock.mWorkSource);
            switch (lock.mMode) {
                case 1:
                    this.mFullLocksAcquired++;
                    break;
                case 2:
                    this.mScanLocksAcquired++;
                    break;
                case 3:
                    this.mFullHighPerfLocksAcquired++;
                    break;
            }
            lockAdded = true;
            Binder.restoreCallingIdentity(ident);
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(ident);
            return lockAdded;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private synchronized WifiLock removeLock(IBinder binder) {
        WifiLock lock;
        lock = findLockByBinder(binder);
        if (lock != null) {
            this.mWifiLocks.remove(lock);
            lock.unlinkDeathRecipient();
        }
        return lock;
    }

    private synchronized boolean releaseLock(IBinder binder) {
        WifiLock wifiLock = removeLock(binder);
        if (wifiLock == null) {
            return false;
        }
        if (this.mVerboseLoggingEnabled) {
            Slog.d(TAG, "releaseLock: " + wifiLock);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteFullWifiLockReleasedFromSource(wifiLock.mWorkSource);
            switch (wifiLock.mMode) {
                case 1:
                    this.mFullLocksReleased++;
                    break;
                case 2:
                    this.mScanLocksReleased++;
                    break;
                case 3:
                    this.mFullHighPerfLocksReleased++;
                    break;
            }
            Binder.restoreCallingIdentity(ident);
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(ident);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
        return true;
    }

    private synchronized WifiLock findLockByBinder(IBinder binder) {
        for (WifiLock lock : this.mWifiLocks) {
            if (lock.getBinder() == binder) {
                return lock;
            }
        }
        return null;
    }

    protected void dump(PrintWriter pw) {
        pw.println("Locks acquired: " + this.mFullLocksAcquired + " full, " + this.mFullHighPerfLocksAcquired + " full high perf, " + this.mScanLocksAcquired + " scan");
        pw.println("Locks released: " + this.mFullLocksReleased + " full, " + this.mFullHighPerfLocksReleased + " full high perf, " + this.mScanLocksReleased + " scan");
        pw.println();
        pw.println("Locks held:");
        for (WifiLock lock : this.mWifiLocks) {
            pw.print("    ");
            pw.println(lock);
        }
    }

    protected void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }
}
