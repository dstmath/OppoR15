package com.android.server.neuron;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.LocationManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.neuron.NsDbWriteRunnable.WriteCallback;
import com.android.server.neuron.publish.Publisher;
import com.oppo.neuron.INeuronSystemService.Stub;
import com.oppo.neuron.NeuronSystemManager;
import com.oppo.neuron.NsDbManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NeuronSystemService extends Stub {
    private static final String ACTION_COPYDB = "oppo.intent.action.copydb";
    private static final boolean CLEAR_AFTER_COPY = true;
    public static final String NS_DB = "/data/system/neuron_system/ns.db";
    public static final String NS_DIR = "/data/system/neuron_system/";
    public static final String NS_UPLOAD_PATH = "/data/system/neuron_system/upload/";
    public static final long ONE_DAYS_MS = 86400000;
    private static final int PUSH_INTO_QUEUE = 1;
    public static final String TAG = "NeuronSystem";
    static final StringBuilder bootPhase = new StringBuilder();
    static int mBootRecIndex = 0;
    private static ExecutorService mNsDbWriteService = Executors.newFixedThreadPool(1);
    private final boolean LOG_ON = SystemProperties.getBoolean("persist.sys.ns_logon", false);
    private final long MAX_DB_SIZE = SystemProperties.getLong("perist.sys.ns_maxdb", 8000000);
    private final long MAX_UPLOAD_DIRSIZE = SystemProperties.getLong("perist.sys.ns_maxuploaddir", 50000000);
    private final double NS_VERSION = 1.0d;
    private final long TERM_SHRINK_SIZE = SystemProperties.getLong("perist.sys.ns_termshrink", 6000000);
    private AlarmManager mAlarmManager;
    private ActivityManagerService mAms = null;
    private NsAppPreload mAppPreload = null;
    Context mContext;
    private SQLiteDatabase mDb;
    private Handler mHandlerThread;
    private NeuronAppUsageTracker mNsAppTracker;
    private NsDbWriteRunnable mNsEventWriter;
    private HandlerThread mNsHandlerThread;
    private NsReceiver mNsReceiver = null;
    private Map<Integer, RegistrantList> mObservers;
    private Publisher mPublisher = null;
    private boolean mSystemBooted = false;

    private class NsReceiver extends BroadcastReceiver {
        public NsReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(NeuronSystemService.ACTION_COPYDB);
            if (NeuronSystemService.this.mContext != null) {
                NeuronSystemService.this.mContext.registerReceiver(this, filter);
                Log.d("NeuronSystem", "NsReceiver registered");
            }
        }

        public void onReceive(Context context, Intent intent) {
            if (NeuronSystemService.ACTION_COPYDB.equals(intent.getAction())) {
                NeuronSystemService.this.scheduledDbAction();
            }
        }
    }

    public NeuronSystemService(Context context) {
        this.mContext = context;
        this.mObservers = new HashMap();
    }

    public void publish(ActivityManagerService ams) {
        ServiceManager.addService("neuronsystem", asBinder());
        this.mAms = ams;
        this.mNsHandlerThread = new HandlerThread("ns");
        this.mNsHandlerThread.start();
        if ((NeuronSystemManager.sNsProp & 8) > 0) {
            this.mNsAppTracker = new NeuronAppUsageTracker(this.mNsHandlerThread, this);
        }
        if (isPublishEnable()) {
            this.mPublisher = new Publisher(this.mContext, this.mNsHandlerThread);
        }
        this.mAppPreload = new NsAppPreload(this.mContext, this.mAms, this.mNsHandlerThread);
        initNsPushWorker();
        Log.d("NeuronSystem", "neuronsystem published");
    }

    public void registerEventObserver(int eventType, Handler h, int what) {
        synchronized (this.mObservers) {
            RegistrantList rl = (RegistrantList) this.mObservers.get(Integer.valueOf(eventType));
            if (rl == null) {
                rl = new RegistrantList();
                this.mObservers.put(Integer.valueOf(eventType), rl);
            }
            rl.add(new Registrant(h, what, null));
        }
    }

    public void unregisterEventObserver(int eventType, Handler h) {
        synchronized (this.mObservers) {
            RegistrantList rl = (RegistrantList) this.mObservers.get(Integer.valueOf(eventType));
            if (rl != null) {
                rl.remove(h);
            }
        }
    }

    private void notifyEventObserver(int eventType, Object result) {
        synchronized (this.mObservers) {
            RegistrantList rl = (RegistrantList) this.mObservers.get(Integer.valueOf(eventType));
            if (rl != null) {
                rl.notifyResult(result);
            }
        }
    }

    private boolean permissionCheck(int callingUid) {
        if (callingUid < 10000 || (NeuronSystemManager.sNsProp & 4) > 0) {
            return true;
        }
        throw new IllegalStateException("Access Denied");
    }

    private boolean isDbWriteEnable() {
        if ((NeuronSystemManager.sNsProp & 1) == 0 || (NeuronSystemManager.sNsProp & 2) == 0) {
            return false;
        }
        return true;
    }

    private boolean isPublishEnable() {
        if ((NeuronSystemManager.sNsProp & 1) == 0 || (NeuronSystemManager.sNsProp & 16) == 0) {
            return false;
        }
        return true;
    }

    private boolean isDbUploadEnable() {
        if ((NeuronSystemManager.sNsProp & 1) == 0 || (NeuronSystemManager.sNsProp & 32) == 0) {
            return false;
        }
        return true;
    }

    public int isPriorApp(String packageName) {
        permissionCheck(Binder.getCallingUid());
        if (this.mNsAppTracker == null) {
            return -1;
        }
        return this.mNsAppTracker.isPriorApp(packageName);
    }

    public long[] getAppRecentUsedTime(String packageName, int flag) {
        permissionCheck(Binder.getCallingUid());
        if (this.mNsAppTracker == null) {
            return null;
        }
        return this.mNsAppTracker.getAppRecentUsedTime(packageName, flag);
    }

    public void flushDbSync() {
        permissionCheck(Binder.getCallingUid());
        if (isDbWriteEnable()) {
            this.mNsEventWriter.pushIntoQueue(null, 0, true);
            this.mNsEventWriter.waitForQueueClear();
        }
    }

    public int[] getConnectionPids(int[] srcPids) {
        if (!(srcPids == null || this.mAms == null || srcPids.length <= 0)) {
            List<Integer> pidList = new ArrayList();
            for (int pid : srcPids) {
                pidList.add(Integer.valueOf(pid));
            }
            List<Integer> outPidList = this.mAms.getConnectionPids(pidList);
            if (!(outPidList == null || (outPidList.isEmpty() ^ 1) == 0)) {
                int[] outPids = new int[outPidList.size()];
                for (int i = 0; i < outPidList.size(); i++) {
                    outPids[i] = ((Integer) outPidList.get(i)).intValue();
                }
                return outPids;
            }
        }
        return null;
    }

    public boolean isInDisplayDeviceList(String pkgName) {
        return false;
    }

    private void initNsPushWorker() {
        this.mHandlerThread = new Handler(this.mNsHandlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        int eventType = msg.arg1;
                        NeuronSystemService.this.notifyEventObserver(eventType, msg.obj);
                        ContentValues contentValues = msg.obj;
                        boolean forceWrite = false;
                        if (msg.arg2 > 0) {
                            forceWrite = true;
                        }
                        if (NeuronSystemService.this.isDbWriteEnable()) {
                            NeuronSystemService.this.mNsEventWriter.pushIntoQueue(contentValues, msg.arg1, forceWrite);
                        }
                        if (NeuronSystemService.this.isPublishEnable() && NeuronSystemService.this.mPublisher != null) {
                            NeuronSystemService.this.mPublisher.publishEvent(eventType, contentValues);
                        }
                        if (NeuronSystemService.this.mAppPreload != null) {
                            NeuronSystemService.this.mAppPreload.onEvent(eventType, contentValues);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mNsEventWriter = new NsDbWriteRunnable(new SparseArray(10), new WriteCallback() {
            public boolean writeToDb(SparseArray<ArrayList<ContentValues>> queueList) {
                Log.d("NeuronSystem", "writeToDb");
                File dbFile = new File(NeuronSystemService.NS_DB);
                if ((NeuronSystemService.this.mDb == null || (dbFile.exists() ^ 1) != 0) && !NeuronSystemService.this.reinitNsDb(false)) {
                    return false;
                }
                if (NeuronSystemService.this.mDb == null) {
                    return false;
                }
                long time0 = System.currentTimeMillis();
                int daysBefore = 30;
                if (dbFile.exists() && dbFile.length() > NeuronSystemService.this.MAX_DB_SIZE) {
                    do {
                        Log.d("NeuronSystem", "remove data before " + daysBefore);
                        try {
                            NsDbManager.removeDataBefore(NeuronSystemService.this.mDb, ((long) daysBefore) * 86400000);
                        } catch (Exception e) {
                        }
                        daysBefore -= 3;
                        if (!dbFile.exists() || dbFile.length() <= NeuronSystemService.this.TERM_SHRINK_SIZE) {
                        }
                    } while (daysBefore >= 0);
                    if (dbFile.exists() && dbFile.length() > NeuronSystemService.this.MAX_DB_SIZE) {
                        Log.e("NeuronSystem", "Somet worst thing happned, remove db file");
                        NeuronSystemService.this.reinitNsDb(true);
                        return false;
                    }
                }
                try {
                    NeuronSystemService.this.mDb.beginTransaction();
                    for (int i = 0; i < queueList.size(); i++) {
                        int key = queueList.keyAt(i);
                        for (ContentValues contentValues : (ArrayList) queueList.get(key)) {
                            NsDbManager.insert(NeuronSystemService.this.mDb, key, contentValues);
                        }
                    }
                    NeuronSystemService.this.mDb.setTransactionSuccessful();
                    NeuronSystemService.this.mDb.endTransaction();
                    Log.d("NeuronSystem", (System.currentTimeMillis() - time0) + "ms");
                    return true;
                } catch (Exception e2) {
                    Log.e("NeuronSystem", e2.toString());
                    return false;
                }
            }
        });
        this.mNsEventWriter.setMaxWriteInterval(600000);
        this.mNsEventWriter.setQueueMax(SystemProperties.getInt("persist.sys.ns_queuemax", 50));
        mNsDbWriteService.execute(this.mNsEventWriter);
    }

    public static boolean writeFile(String filePath, String content, boolean append) {
        if (content == null) {
            return false;
        }
        boolean res = true;
        File file = new File(filePath);
        if (!file.exists()) {
            File dir = new File(file.getParent());
            if (!(dir.exists() || dir.mkdirs())) {
                return false;
            }
        }
        try {
            FileWriter mFileWriter = new FileWriter(file, append);
            mFileWriter.write(content);
            mFileWriter.close();
        } catch (IOException e) {
            res = false;
        }
        return res;
    }

    public static boolean copyFile(File srcFile, File dstFile) {
        try {
            if (!srcFile.exists()) {
                return false;
            }
            if (!dstFile.getParentFile().exists()) {
                dstFile.getParentFile().mkdirs();
            }
            InputStream inputStream = new FileInputStream(srcFile);
            if (dstFile.exists()) {
                dstFile.delete();
            }
            OutputStream outputStream = new FileOutputStream(dstFile);
            byte[] buf = new byte[4096];
            while (true) {
                int cnt = inputStream.read(buf);
                if (cnt >= 0) {
                    outputStream.write(buf, 0, cnt);
                } else {
                    outputStream.close();
                    inputStream.close();
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean removeFile(File file) {
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            for (File removeFile : childFiles) {
                removeFile(removeFile);
            }
        }
        return file.delete();
    }

    public static void recordBootPhase(String tag, long uptime) {
        int i = mBootRecIndex;
        mBootRecIndex = i + 1;
        if (i == 0) {
            bootPhase.append(tag).append(":").append(uptime);
        } else {
            bootPhase.append("\n").append(tag).append(":").append(uptime);
        }
    }

    public void writeBootPhaseToDb() {
        if (isDbWriteEnable()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("is_up", Integer.valueOf(1));
            contentValues.put("data", bootPhase.toString());
            contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
            this.mNsEventWriter.pushIntoQueue(contentValues, 4, false);
        }
    }

    public void scheduledDbAction() {
        mNsDbWriteService.execute(new Runnable() {
            public void run() {
                if (NeuronSystemService.getFileSize(new File(NeuronSystemService.NS_UPLOAD_PATH)) > NeuronSystemService.this.MAX_UPLOAD_DIRSIZE) {
                    Log.w("NeuronSystem", "upload dir exceed limit, remove it");
                    NeuronSystemService.removeFile(new File(NeuronSystemService.NS_UPLOAD_PATH));
                    return;
                }
                String filename = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".db";
                try {
                    if (NeuronSystemService.copyFile(new File(NeuronSystemService.NS_DB), new File(NeuronSystemService.NS_UPLOAD_PATH + filename))) {
                        Log.d("NeuronSystem", "copy: " + filename);
                        NeuronSystemService.this.reinitNsDb(true);
                    }
                } catch (Exception e) {
                    Log.d("NeuronSystem", e + "");
                }
            }
        });
    }

    public boolean reinitNsDb(boolean removeOld) {
        if (removeOld) {
            new File(NS_DB).delete();
        }
        this.mDb = NsDbManager.getDatabase(NS_DB);
        return NsDbManager.initTables(this.mDb);
    }

    public static long getFileSize(File target) {
        if (target == null || (target.exists() ^ 1) != 0) {
            return 0;
        }
        if (target.isFile()) {
            return target.length();
        }
        long size = 0;
        for (File file : target.listFiles()) {
            if (file.isFile()) {
                size += file.length();
            } else if (file.isDirectory()) {
                size = (size + file.length()) + getFileSize(file);
            }
        }
        return size;
    }

    public static long getUTC(int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(11, hourOfDay);
        calendar.set(12, minute);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar.getTimeInMillis();
    }

    public static long getNextUTC(int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(11, hourOfDay);
        calendar.set(12, minute);
        calendar.set(13, 0);
        calendar.set(14, 0);
        long time = calendar.getTimeInMillis();
        if (System.currentTimeMillis() <= time) {
            return time;
        }
        calendar.add(5, 1);
        return calendar.getTimeInMillis();
    }

    public void systemReady() {
        if (isDbUploadEnable()) {
            this.mNsReceiver = new NsReceiver();
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_COPYDB, null), 0);
            int copyHour = 16;
            int copyMinute = 0;
            long interval = 43200000;
            try {
                copyHour = SystemProperties.getInt("persist.sys.ns_copyhour", 16);
                copyMinute = SystemProperties.getInt("persist.sys.ns_copyminute", 0);
                interval = Long.valueOf(SystemProperties.getLong("persist.sys.ns_copyinterval", 43200000)).longValue();
            } catch (Exception e) {
            }
            this.mAlarmManager.setRepeating(0, getNextUTC(copyHour, copyMinute), interval, pendingIntent);
        }
        this.mAppPreload.systemReady();
        this.mSystemBooted = true;
    }

    public void preloadApp(String pkg) {
        permissionCheck(Binder.getCallingUid());
        if (this.mAppPreload != null) {
            this.mAppPreload.preload(pkg);
        }
    }

    public void shutdown() {
        if (this.mNsAppTracker != null) {
            this.mNsAppTracker.shutdown();
        }
        if (isDbWriteEnable() && this.mDb != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("is_up", Integer.valueOf(0));
            contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
            this.mNsEventWriter.pushIntoQueue(contentValues, 4, true);
        }
    }

    public void publishEvent(int eventType, ContentValues contentValues) {
        if (this.mNsEventWriter != null) {
            long time0 = System.currentTimeMillis();
            if (this.mHandlerThread != null) {
                Message msg = this.mHandlerThread.obtainMessage(1, contentValues);
                msg.arg1 = eventType;
                msg.arg2 = 0;
                msg.sendToTarget();
            }
            if (NeuronSystemManager.LOG_ON) {
                Log.d("NeuronSystem", contentValues.toString());
                long time = System.currentTimeMillis() - time0;
                if (time > 1) {
                    Log.w("NeuronSystem", "warning: push into queue" + eventType + " cost " + time + "ms");
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (Binder.getCallingUid() > 10000) {
            pw.println("Permission Denial: can't dump NeuronSystemService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        } else if (args.length == 0) {
            pw.println("no parameter");
        } else {
            String cmd = args[0];
            if (cmd.equals("load") && args.length > 1 && this.mAppPreload != null) {
                Slog.d("NeuronSystem", "dump mAppPreload.preload ");
                this.mAppPreload.preload(args[1]);
            } else if (cmd.equals("autoload") && this.mAppPreload != null) {
                this.mAppPreload.autoPreload();
            } else if (cmd.equals("properity") && args.length > 1) {
                String uid = args[1];
                SystemProperties.set("sys.nsaudio.disable" + uid, LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON);
                Slog.d("NeuronSystem", "close audio of uid " + uid);
            }
        }
    }
}
