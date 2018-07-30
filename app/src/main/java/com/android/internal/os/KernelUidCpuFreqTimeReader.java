package com.android.internal.os;

import android.os.SystemClock;
import android.util.Slog;
import android.util.SparseArray;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class KernelUidCpuFreqTimeReader {
    private static final boolean DEBUG = false;
    private static final String TAG = "KernelUidCpuFreqTimeReader";
    private static final int TOTAL_READ_ERROR_COUNT = 5;
    private static final String UID_TIMES_PROC_FILE = "/proc/uid_time_in_state";
    private long[] mCpuFreqs;
    private int mCpuFreqsCount;
    private long mLastTimeReadMs;
    private SparseArray<long[]> mLastUidCpuFreqTimeMs = new SparseArray();
    private long mNowTimeMs;
    private boolean mProcFileAvailable;
    private int mReadErrorCounter;

    public interface Callback {
        void onCpuFreqs(long[] jArr);

        void onUidCpuFreqTime(int i, long[] jArr);
    }

    public void readDelta(Callback callback) {
        IOException e;
        Throwable th;
        Throwable th2 = null;
        if (this.mProcFileAvailable || this.mReadErrorCounter < 5) {
            BufferedReader reader = null;
            try {
                BufferedReader reader2 = new BufferedReader(new FileReader(UID_TIMES_PROC_FILE));
                try {
                    this.mNowTimeMs = SystemClock.elapsedRealtime();
                    readDelta(reader2, callback);
                    this.mLastTimeReadMs = this.mNowTimeMs;
                    this.mProcFileAvailable = true;
                    if (reader2 != null) {
                        try {
                            reader2.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        try {
                            throw th2;
                        } catch (IOException e2) {
                            e = e2;
                            reader = reader2;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                    reader = reader2;
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Throwable th5) {
                            if (th2 == null) {
                                th2 = th5;
                            } else if (th2 != th5) {
                                th2.addSuppressed(th5);
                            }
                        }
                    }
                    if (th2 == null) {
                        try {
                            throw th2;
                        } catch (IOException e3) {
                            e = e3;
                            this.mReadErrorCounter++;
                            Slog.e(TAG, "Failed to read /proc/uid_time_in_state: " + e);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Throwable th52) {
                        if (th2 == null) {
                            th2 = th52;
                        } else if (th2 != th52) {
                            th2.addSuppressed(th52);
                        }
                    }
                }
                if (th2 == null) {
                    throw th;
                }
                try {
                    throw th2;
                } catch (IOException e32) {
                    e = e32;
                    this.mReadErrorCounter++;
                    Slog.e(TAG, "Failed to read /proc/uid_time_in_state: " + e);
                }
            }
        }
    }

    public void removeUid(int uid) {
        this.mLastUidCpuFreqTimeMs.delete(uid);
    }

    public void removeUidsInRange(int startUid, int endUid) {
        if (endUid >= startUid) {
            this.mLastUidCpuFreqTimeMs.put(startUid, null);
            this.mLastUidCpuFreqTimeMs.put(endUid, null);
            int firstIndex = this.mLastUidCpuFreqTimeMs.indexOfKey(startUid);
            this.mLastUidCpuFreqTimeMs.removeAtRange(firstIndex, (this.mLastUidCpuFreqTimeMs.indexOfKey(endUid) - firstIndex) + 1);
        }
    }

    public void readDelta(BufferedReader reader, Callback callback) throws IOException {
        String line = reader.readLine();
        if (line != null) {
            readCpuFreqs(line, callback);
            while (true) {
                line = reader.readLine();
                if (line != null) {
                    int index = line.indexOf(32);
                    readTimesForUid(Integer.parseInt(line.substring(0, index - 1), 10), line.substring(index + 1, line.length()), callback);
                } else {
                    return;
                }
            }
        }
    }

    private void readTimesForUid(int uid, String line, Callback callback) {
        long[] uidTimeMs = (long[]) this.mLastUidCpuFreqTimeMs.get(uid);
        if (uidTimeMs == null) {
            uidTimeMs = new long[this.mCpuFreqsCount];
            this.mLastUidCpuFreqTimeMs.put(uid, uidTimeMs);
        }
        String[] timesStr = line.split(" ");
        int size = timesStr.length;
        if (size != uidTimeMs.length) {
            Slog.e(TAG, "No. of readings don't match cpu freqs, readings: " + size + " cpuFreqsCount: " + uidTimeMs.length);
            return;
        }
        long[] deltaUidTimeMs = new long[size];
        long[] curUidTimeMs = new long[size];
        boolean notify = false;
        int i = 0;
        while (i < size) {
            long totalTimeMs = Long.parseLong(timesStr[i], 10) * 10;
            deltaUidTimeMs[i] = totalTimeMs - uidTimeMs[i];
            if (deltaUidTimeMs[i] >= 0 && totalTimeMs >= 0) {
                curUidTimeMs[i] = totalTimeMs;
                if (notify || deltaUidTimeMs[i] > 0) {
                    notify = true;
                } else {
                    notify = false;
                }
                i++;
            } else {
                return;
            }
        }
        if (notify) {
            System.arraycopy(curUidTimeMs, 0, uidTimeMs, 0, size);
            if (callback != null) {
                callback.onUidCpuFreqTime(uid, deltaUidTimeMs);
            }
        }
    }

    private void readCpuFreqs(String line, Callback callback) {
        if (this.mCpuFreqs == null) {
            String[] freqStr = line.split(" ");
            this.mCpuFreqsCount = freqStr.length - 1;
            this.mCpuFreqs = new long[this.mCpuFreqsCount];
            for (int i = 0; i < this.mCpuFreqsCount; i++) {
                this.mCpuFreqs[i] = Long.parseLong(freqStr[i + 1], 10);
            }
        }
        if (callback != null) {
            callback.onCpuFreqs(this.mCpuFreqs);
        }
    }
}
