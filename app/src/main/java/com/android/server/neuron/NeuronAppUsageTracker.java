package com.android.server.neuron;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Log;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usage.UnixCalendar;
import com.oppo.neuron.NeoServiceProxy;
import com.oppo.util.FixSizeFIFOList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

public class NeuronAppUsageTracker {
    private static final String ACTION_HEAD = "action: ";
    private static final int APP_QUEUE_MAX = 50;
    private static final String APP_USAGE_STAT = "/data/system/neuron_system/app_usage";
    private static final boolean DBG = SystemProperties.getBoolean("persist.sys.ns_logon", false);
    private static final int FOREGROUND_APP_EVENT = 2;
    private static final int INIT_APPTRACKER_EVENT = 1;
    private static final long ONE_WEEK_MS = 604800000;
    private static final String PKG_HEAD = "pkg: ";
    private static final int REFRESH_AND_SYNC_EVENT = 3;
    private static final long SAMPLING_DURATION = 21600000;
    private static final long SIX_HOUR_MS = 21600000;
    private static final String TAG = "NeuronAppUsageTracker";
    private static final long TRACKER_TOTAL_DURATION = 604800000;
    private FixSizeFIFOList<NeuronAppRecord> mAppUsageList = new FixSizeFIFOList(50);
    private HashMap<String, NeuronAppRecord> mAppUsageMap = new HashMap();
    private NSAppTrackerHandler mHandler;
    private NeuronAppRecord mLatestAppUsage;
    private String mLatestPgName;
    private NeuronSystemService mNsService;

    private static class AppDataNode {
        boolean changed = false;
        long count;
        long duration;
        long endTime;
        long maxBgTime = -1;
        long minBgTime = JobStatus.NO_LATEST_RUNTIME;
        long totalBgTime;
    }

    private class NSAppTrackerHandler extends Handler {
        public NSAppTrackerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    NeuronAppUsageTracker.this.readFromDisk();
                    NeuronAppUsageTracker.this.createNewNodeWhenBoot();
                    NeuronAppUsageTracker.this.mNsService.registerEventObserver(1, NeuronAppUsageTracker.this.mHandler, 2);
                    return;
                case 2:
                    NeuronAppUsageTracker.this.notifyForegroundApp(msg.obj.result.getAsString("pkgname"));
                    return;
                case 3:
                    NeuronAppUsageTracker.this.refreshDataList();
                    NeuronAppUsageTracker.this.syncToDisk();
                    NeuronAppUsageTracker.this.mHandler.sendMessageDelayed(NeuronAppUsageTracker.this.mHandler.obtainMessage(3), 21600000);
                    return;
                default:
                    return;
            }
        }
    }

    private static class NeuronAppRecord {
        private static final int APP_META_DATA_MAX = 30;
        private static final int KEEP_RECENT_SIZE = 3;
        private FixSizeFIFOList<AppDataNode> dataList = new FixSizeFIFOList(30);
        private FixSizeFIFOList<Long> lastestResumeTime = new FixSizeFIFOList(3);
        private FixSizeFIFOList<Long> latestPauseTime = new FixSizeFIFOList(3);
        private String packageName;
        private int totalCount;
        private long totalDuration;

        public NeuronAppRecord(String pgName) {
            this.packageName = pgName;
        }
    }

    public NeuronAppUsageTracker(HandlerThread thread, NeuronSystemService service) {
        this.mHandler = new NSAppTrackerHandler(thread.getLooper());
        this.mHandler.obtainMessage(1).sendToTarget();
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), 21600000);
        this.mNsService = service;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void notifyForegroundApp(String packageName) {
        if (packageName != null) {
            if (!packageName.equals(this.mLatestPgName)) {
                NeuronAppRecord app = (NeuronAppRecord) this.mAppUsageMap.get(packageName);
                if (app == null) {
                    app = new NeuronAppRecord(packageName);
                    app.dataList.pushToHead(new AppDataNode());
                    this.mAppUsageMap.put(packageName, app);
                }
                long currentTime = System.currentTimeMillis();
                app.lastestResumeTime.pushToHead(Long.valueOf(currentTime));
                AppDataNode latestAppData = (AppDataNode) app.dataList.getLatest();
                Long latestPause = (Long) app.latestPauseTime.getLatest();
                if (!(latestAppData == null || latestPause == null || latestPause.longValue() <= 0)) {
                    long bgTime = currentTime - latestPause.longValue();
                    if (bgTime < latestAppData.minBgTime) {
                        latestAppData.minBgTime = bgTime;
                    }
                    if (bgTime > latestAppData.maxBgTime) {
                        latestAppData.maxBgTime = bgTime;
                    }
                    latestAppData.totalBgTime += bgTime;
                }
                NeuronAppRecord removedApp = (NeuronAppRecord) this.mAppUsageList.pushToHead(app);
                if (removedApp != null) {
                    this.mAppUsageMap.remove(removedApp.packageName);
                }
                pauseLatestApp(currentTime);
                this.mLatestAppUsage = app;
                this.mLatestPgName = packageName;
            }
        }
    }

    private synchronized void pauseLatestApp(long currentTime) {
        if (this.mLatestAppUsage != null) {
            this.mLatestAppUsage.latestPauseTime.pushToHead(Long.valueOf(currentTime));
            AppDataNode latestAppData = (AppDataNode) this.mLatestAppUsage.dataList.getLatest();
            if (latestAppData != null) {
                latestAppData.count++;
                latestAppData.duration += currentTime - ((Long) this.mLatestAppUsage.lastestResumeTime.getLatest()).longValue();
                if (!latestAppData.changed) {
                    latestAppData.changed = true;
                }
            } else {
                Log.e(TAG, "pauseLatestApp something wrong!");
            }
        }
    }

    public synchronized boolean contains(String packageName) {
        if (this.mAppUsageMap.containsKey(packageName)) {
            return false;
        }
        return true;
    }

    private synchronized void clear() {
        this.mAppUsageList.clear();
        this.mAppUsageMap.clear();
    }

    public synchronized int isPriorApp(String pkgName) {
        if (pkgName == null) {
            return -1;
        }
        NeuronAppRecord appUsage = (NeuronAppRecord) this.mAppUsageMap.get(pkgName);
        if (appUsage == null) {
            return 0;
        }
        int recentIndex = this.mAppUsageList.getAll().indexOf(appUsage);
        if (recentIndex < 0) {
            return -1;
        }
        int totalCount = appUsage.totalCount;
        long totalDuration = appUsage.totalDuration;
        AppDataNode latestData = (AppDataNode) appUsage.dataList.getLatest();
        if (latestData != null) {
            totalCount = (int) (((long) totalCount) + latestData.count);
            totalDuration += latestData.duration;
        }
        try {
            return NeoServiceProxy.getInstance().isPriorApp(totalCount, recentIndex, totalDuration, UnixCalendar.WEEK_IN_MILLIS);
        } catch (Exception e) {
            Log.e(TAG, "exception happed while talking to neo", e);
            return -1;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized long[] getAppRecentUsedTime(String packageName, int flag) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        NeuronAppRecord appUsage = (NeuronAppRecord) this.mAppUsageMap.get(packageName);
        if (appUsage == null) {
            return null;
        }
        boolean needResume = (flag & 2) > 0;
        boolean needPause = (flag & 1) > 0;
        int resumeSize = needResume ? appUsage.lastestResumeTime.getSize() : 0;
        int pauseSize = needPause ? appUsage.latestPauseTime.getSize() : 0;
        int size = resumeSize + pauseSize;
        if (needResume && needPause && resumeSize > pauseSize) {
            size++;
        }
        if (size == 0) {
            return null;
        }
        int i;
        int i2;
        long[] result = new long[size];
        if (needResume) {
            i = 0;
            while (i < resumeSize) {
                if (needPause) {
                    i2 = i * 2;
                } else {
                    i2 = i;
                }
                try {
                    result[i2] = ((Long) appUsage.lastestResumeTime.get(i)).longValue();
                    i++;
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, "some thing wrong when get pkg:" + packageName + " record", e);
                    return null;
                }
            }
        }
        if (pauseSize > 0) {
            int shift = 0;
            if (needResume) {
                Long lastResume = (Long) appUsage.lastestResumeTime.getLatest();
                Long lastPause = (Long) appUsage.latestPauseTime.getLatest();
                if (!(lastResume == null || lastPause == null || lastResume.longValue() <= lastPause.longValue())) {
                    shift = 2;
                }
            }
            i = 0;
            while (i < pauseSize && (!needResume || ((i * 2) + 1) + shift < size)) {
                if (needResume) {
                    i2 = ((i * 2) + 1) + shift;
                } else {
                    i2 = i;
                }
                try {
                    result[i2] = ((Long) appUsage.latestPauseTime.get(i)).longValue();
                    i++;
                } catch (ArrayIndexOutOfBoundsException e2) {
                    Log.e(TAG, "some thing wrong when get pkg:" + packageName + " record", e2);
                    return null;
                }
            }
        }
    }

    synchronized void shutdown() {
        long currentTime = System.currentTimeMillis();
        pauseLatestApp(currentTime);
        for (NeuronAppRecord -get0 : this.mAppUsageList.getAll()) {
            AppDataNode head = (AppDataNode) -get0.dataList.getLatest();
            if (head != null && head.changed) {
                head.endTime = currentTime;
            }
        }
        syncToDisk();
    }

    private synchronized void syncToDisk() {
        Log.d(TAG, "syncToDisk");
        AtomicFile atomicFile = new AtomicFile(new File(APP_USAGE_STAT));
        long time = System.currentTimeMillis();
        StringBuffer stringBuffer = new StringBuffer();
        for (NeuronAppRecord appUsage : this.mAppUsageList.getAll()) {
            stringBuffer.append(PKG_HEAD + xorEncrypt(appUsage.packageName) + "\n");
            stringBuffer.append(ACTION_HEAD);
            for (AppDataNode appData : appUsage.dataList.getAll()) {
                if (appData.changed) {
                    stringBuffer.append(appData.endTime + " ");
                    stringBuffer.append(appData.duration + " ");
                    stringBuffer.append(appData.count + " ");
                    stringBuffer.append(appData.minBgTime + " ");
                    stringBuffer.append(appData.totalBgTime + " ");
                    stringBuffer.append(appData.maxBgTime + "|");
                }
            }
            stringBuffer.append("\n\n");
        }
        try {
            FileOutputStream stream = atomicFile.startWrite();
            try {
                stream.write(stringBuffer.toString().getBytes());
                atomicFile.finishWrite(stream);
            } catch (IOException e) {
                atomicFile.failWrite(stream);
            }
            Log.d(TAG, "syncToDisk done: " + (System.currentTimeMillis() - time) + "ms");
            time = System.currentTimeMillis();
        } catch (IOException e2) {
            e2.printStackTrace();
            return;
        }
        return;
    }

    private synchronized void readFromDisk() {
        Log.d(TAG, "readFromDisk");
        long time = System.currentTimeMillis();
        clear();
        int PKG_HEAD_LEN = PKG_HEAD.length();
        int ACTION_HEAD_LEN = ACTION_HEAD.length();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(APP_USAGE_STAT));
            NeuronAppRecord appUsage = null;
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    bufferedReader.close();
                    break;
                } else if (line.startsWith(PKG_HEAD)) {
                    String pkgName = xorEncrypt(line.substring(PKG_HEAD_LEN));
                    appUsage = new NeuronAppRecord(pkgName);
                    this.mAppUsageList.pushToTail(appUsage);
                    this.mAppUsageMap.put(pkgName, appUsage);
                } else if (line.startsWith(ACTION_HEAD) && appUsage != null) {
                    StringTokenizer token = new StringTokenizer(line.substring(ACTION_HEAD_LEN), " |");
                    if (token.countTokens() % 6 != 0) {
                        throw new Exception("File Format Error");
                    }
                    while (token.hasMoreTokens()) {
                        AppDataNode appData = new AppDataNode();
                        appData.endTime = Long.valueOf(token.nextToken()).longValue();
                        appData.duration = Long.valueOf(token.nextToken()).longValue();
                        appData.count = Long.valueOf(token.nextToken()).longValue();
                        appData.minBgTime = Long.valueOf(token.nextToken()).longValue();
                        appData.totalBgTime = Long.valueOf(token.nextToken()).longValue();
                        appData.maxBgTime = Long.valueOf(token.nextToken()).longValue();
                        appData.changed = true;
                        if (appData.endTime > 0) {
                            appUsage.dataList.pushToTail(appData);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while read file:", e);
            new File(APP_USAGE_STAT).delete();
            clear();
        }
        Log.d(TAG, (System.currentTimeMillis() - time) + "ms");
    }

    private synchronized void createNewNodeWhenBoot() {
        for (NeuronAppRecord appUsage : this.mAppUsageList.getAll()) {
            Iterator<AppDataNode> iter = appUsage.dataList.getAll().iterator();
            while (iter.hasNext()) {
                AppDataNode data = (AppDataNode) iter.next();
                appUsage.totalCount = (int) (((long) appUsage.totalCount) + data.count);
                appUsage.totalDuration = appUsage.totalDuration + data.duration;
            }
            AppDataNode removed = (AppDataNode) appUsage.dataList.pushToHead(new AppDataNode());
            if (removed != null) {
                appUsage.totalCount = (int) (((long) appUsage.totalCount) - removed.count);
                appUsage.totalDuration = appUsage.totalDuration - removed.duration;
            }
        }
    }

    private String xorEncrypt(String input) {
        if (input == null) {
            return null;
        }
        byte[] src = input.getBytes();
        for (int i = 0; i < src.length; i++) {
            src[i] = (byte) (src[i] ^ 3);
        }
        return new String(src);
    }

    private synchronized void refreshDataList() {
        long currentTime = System.currentTimeMillis();
        for (NeuronAppRecord appUsage : this.mAppUsageList.getAll()) {
            AppDataNode head = (AppDataNode) appUsage.dataList.getLatest();
            if (head != null) {
                appUsage.totalCount = (int) (((long) appUsage.totalCount) + head.count);
                appUsage.totalDuration = appUsage.totalDuration + head.duration;
                head.endTime = currentTime;
            }
            Iterator<AppDataNode> iter = appUsage.dataList.getAll().iterator();
            while (iter.hasNext()) {
                AppDataNode data = (AppDataNode) iter.next();
                if (data.endTime < currentTime - UnixCalendar.WEEK_IN_MILLIS) {
                    appUsage.totalCount = (int) (((long) appUsage.totalCount) - data.count);
                    appUsage.totalDuration = appUsage.totalDuration - data.duration;
                    iter.remove();
                }
            }
            if (head.changed) {
                AppDataNode removed = (AppDataNode) appUsage.dataList.pushToHead(new AppDataNode());
                if (removed != null) {
                    appUsage.totalCount = (int) (((long) appUsage.totalCount) - removed.count);
                    appUsage.totalDuration = appUsage.totalDuration - removed.duration;
                }
            } else {
                head.endTime = 0;
            }
        }
    }

    public void dump() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Dump:\n");
        for (NeuronAppRecord appUsage : this.mAppUsageList.getAll()) {
            stringBuffer.append(appUsage.packageName + ": ");
            stringBuffer.append(" lastResume:" + appUsage.lastestResumeTime.getLatest());
            stringBuffer.append(" latestPauseTime:" + appUsage.latestPauseTime.getLatest());
            stringBuffer.append(" totalCount:" + appUsage.totalCount);
            stringBuffer.append(" totalDuration:" + appUsage.totalDuration + "\n");
            for (AppDataNode appData : appUsage.dataList.getAll()) {
                stringBuffer.append(appData.endTime + " ");
                stringBuffer.append(appData.duration + " ");
                stringBuffer.append(appData.count + " ");
                stringBuffer.append(appData.minBgTime + " ");
                stringBuffer.append(appData.totalBgTime + " ");
                stringBuffer.append(appData.maxBgTime + "|");
            }
            stringBuffer.append("\n");
        }
        logd(stringBuffer.toString());
    }

    private void logd(String str) {
        if (DBG) {
            Log.d(TAG, str);
        }
    }
}
