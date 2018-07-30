package com.qualcomm.qti;

public class Performance {
    public static final int REQUEST_FAILED = -1;
    public static final int REQUEST_SUCCEEDED = 0;
    private static final String TAG = "Perf";
    private int handle = 0;

    private native int native_perf_hint(int i, String str, int i2, int i3);

    private native int native_perf_io_prefetch_start(int i, String str, String str2);

    private native int native_perf_io_prefetch_stop();

    private native int native_perf_lock_acq(int i, int i2, int[] iArr);

    private native int native_perf_lock_rel(int i);

    static {
        try {
            System.loadLibrary("qti_performance");
        } catch (UnsatisfiedLinkError e) {
        }
    }

    public int perfLockAcquire(int duration, int... list) {
        this.handle = native_perf_lock_acq(this.handle, duration, list);
        if (this.handle <= 0) {
            return -1;
        }
        return this.handle;
    }

    public int perfLockRelease() {
        int retValue = native_perf_lock_rel(this.handle);
        this.handle = 0;
        return retValue;
    }

    public int perfLockReleaseHandler(int _handle) {
        return native_perf_lock_rel(_handle);
    }

    public int perfHint(int hint, String userDataStr, int userData1, int userData2) {
        this.handle = native_perf_hint(hint, userDataStr, userData1, userData2);
        if (this.handle <= 0) {
            return -1;
        }
        return this.handle;
    }

    public int perfIOPrefetchStart(int PId, String Pkg_name, String Code_path) {
        return native_perf_io_prefetch_start(PId, Pkg_name, Code_path);
    }

    public int perfIOPrefetchStop() {
        return native_perf_io_prefetch_stop();
    }
}
