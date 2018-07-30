package com.android.server.neuron;

import android.content.ContentValues;
import android.os.SystemProperties;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;

public class NsDbWriteRunnable implements Runnable {
    public static final String TAG = "NeuronSystem";
    private final boolean LOG_ON = SystemProperties.getBoolean("persist.sys.ns_logon", false);
    private int mCounter;
    private int mMaxWriteInterval = 300000;
    private SparseArray<ArrayList<ContentValues>> mQueueList;
    private Object mQueueLock = new Object();
    private int mQueueMax = 20;
    WriteCallback mWriteCallback;
    private Object mWriteLock = new Object();

    public interface WriteCallback {
        boolean writeToDb(SparseArray<ArrayList<ContentValues>> sparseArray);
    }

    public NsDbWriteRunnable(SparseArray<ArrayList<ContentValues>> arrays, WriteCallback writeCallback) {
        this.mQueueList = arrays;
        this.mWriteCallback = writeCallback;
    }

    public void setQueueMax(int max) {
        if (max >= 1) {
            this.mQueueMax = max;
        }
    }

    public void setMaxWriteInterval(int max) {
        if (max >= 1) {
            this.mMaxWriteInterval = max;
        }
    }

    public void pushIntoQueue(ContentValues contentValues, int table, boolean forceWrite) {
        synchronized (this.mQueueLock) {
            long time0 = System.currentTimeMillis();
            if (!(this.mQueueList == null || contentValues == null)) {
                ArrayList<ContentValues> queue = (ArrayList) this.mQueueList.get(table);
                if (queue == null) {
                    queue = new ArrayList(10);
                    this.mQueueList.append(table, queue);
                }
                queue.add(contentValues);
                this.mCounter++;
            }
            if (this.LOG_ON) {
                long time = System.currentTimeMillis() - time0;
                if (time > 1) {
                    Log.w("NeuronSystem", "internal: cost " + time + "ms");
                }
            }
        }
        if (this.mCounter >= this.mQueueMax || forceWrite) {
            synchronized (this.mWriteLock) {
                this.mWriteLock.notifyAll();
            }
        }
    }

    public void run() {
        while (true) {
            try {
                synchronized (this.mWriteLock) {
                    this.mWriteLock.wait((long) this.mMaxWriteInterval);
                }
                if (this.mWriteCallback != null && this.mCounter > 0) {
                    SparseArray<ArrayList<ContentValues>> clonelist;
                    synchronized (this.mQueueLock) {
                        clonelist = cloneQueueList(this.mQueueList);
                    }
                    if (clonelist != null && (this.mWriteCallback.writeToDb(clonelist) || this.mCounter > 1000)) {
                        synchronized (this.mQueueLock) {
                            this.mQueueList.clear();
                            this.mCounter = 0;
                            this.mQueueLock.notifyAll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public void waitForQueueClear() {
        synchronized (this.mQueueLock) {
            try {
                if (this.mCounter == 0) {
                    return;
                }
                if (this.LOG_ON) {
                    Log.d("NeuronSystem", "waitForQueueClear start");
                }
                this.mQueueLock.wait(100);
                if (this.LOG_ON) {
                    Log.d("NeuronSystem", "waitForQueueClear end");
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public static <T> List<T> cloneList(List<T> list) {
        List<T> clone = new ArrayList(list.size());
        for (T item : list) {
            clone.add(item);
        }
        return clone;
    }

    public static SparseArray<ArrayList<ContentValues>> cloneQueueList(SparseArray<ArrayList<ContentValues>> queueList) {
        SparseArray<ArrayList<ContentValues>> queues = new SparseArray();
        for (int i = 0; i < queueList.size(); i++) {
            int key = queueList.keyAt(i);
            queues.append(key, (ArrayList) cloneList((ArrayList) queueList.get(key)));
        }
        return queues;
    }

    public static void logd(Object object) {
        Log.d("NeuronSystem", object + "");
    }
}
