package com.oppo.debug;

import android.os.IOppoService.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.os.ProcessCpuTracker;

public class ProcessCpuTrackerRunnable implements Runnable {
    private static boolean DEBUG = SystemProperties.getBoolean("persist.sys.assert.panic", false);
    private static final String TAG = "ProcessCpuTracker";

    public void run() {
        if (DEBUG) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Stub.asInterface(ServiceManager.getService("OPPO")).iotop();
                    } catch (RemoteException e) {
                    }
                    ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(true);
                    processCpuTracker.init();
                    System.gc();
                    long now = SystemClock.uptimeMillis();
                    processCpuTracker.update();
                    try {
                        synchronized (processCpuTracker) {
                            processCpuTracker.wait(500);
                        }
                    } catch (InterruptedException e2) {
                    }
                    processCpuTracker.update();
                    Slog.v(ProcessCpuTrackerRunnable.TAG, processCpuTracker.printCurrentState(now));
                }
            }, "ProcessCpuTrackerThread").start();
        }
    }
}
