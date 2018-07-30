package com.android.server;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.DropBoxManager.Entry;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.OppoManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.ColorOSTelephonyManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.os.IDropBoxManagerService;
import com.android.internal.os.IDropBoxManagerService.Stub;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.ObjectUtils;
import com.android.server.face.FaceDaemonWrapper;
import com.oppo.debug.ASSERT;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import libcore.io.IoUtils;

public final class DropBoxManagerService extends SystemService {
    private static final int DEFAULT_AGE_SECONDS = 604800;
    private static final int DEFAULT_MAX_FILES = 1000;
    private static final int DEFAULT_MAX_FILES_LOWRAM = 300;
    private static final int DEFAULT_QUOTA_KB = 20480;
    private static final int DEFAULT_QUOTA_PERCENT = 10;
    private static final int DEFAULT_RESERVE_PERCENT = 10;
    private static final int MSG_SEND_BROADCAST = 1;
    public static final String OPPO_CTA_USER_ECPERIENCE = "oppo_cta_user_experience";
    private static final boolean PROFILE_DUMP = false;
    private static final int QUOTA_RESCAN_MILLIS = 5000;
    private static final String TAG = "DropBoxManagerService";
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "Jianhua.Lin@Plf.SDK, 2017-08-22 : Add for EAP", property = OppoRomType.ROM)
    private static String mIMEI;
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "Jianhua.Lin@Plf.SDK, 2017-08-22 : Add for EAP", property = OppoRomType.ROM)
    private static String mOtaVersion;
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "Jianhua.Lin@Plf.SDK, 2017-08-22 : Add for EAP", property = OppoRomType.ROM)
    private static String mProcessName;
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "Jianhua.Lin@Plf.SDK, 2017-08-22 : Add for EAP", property = OppoRomType.ROM)
    private static String mTime;
    private FileList mAllFiles;
    private int mBlockSize;
    private volatile boolean mBooted;
    private int mCachedQuotaBlocks;
    private long mCachedQuotaUptimeMillis;
    private final ContentResolver mContentResolver;
    private final File mDropBoxDir;
    private ArrayMap<String, FileList> mFilesByTag;
    private final Handler mHandler;
    private int mMaxFiles;
    private final BroadcastReceiver mReceiver;
    private StatFs mStatFs;
    private final Stub mStub;

    static final class EntryFile implements Comparable<EntryFile> {
        public final int blocks;
        public final int flags;
        public final String tag;
        public final long timestampMillis;

        public final int compareTo(EntryFile o) {
            int comp = Long.compare(this.timestampMillis, o.timestampMillis);
            if (comp != 0) {
                return comp;
            }
            comp = ObjectUtils.compare(this.tag, o.tag);
            if (comp != 0) {
                return comp;
            }
            comp = Integer.compare(this.flags, o.flags);
            if (comp != 0) {
                return comp;
            }
            return Integer.compare(hashCode(), o.hashCode());
        }

        public EntryFile(File temp, File dir, String tag, long timestampMillis, int flags, int blockSize) throws IOException {
            if ((flags & 1) != 0) {
                throw new IllegalArgumentException();
            }
            this.tag = TextUtils.safeIntern(tag);
            this.timestampMillis = timestampMillis;
            this.flags = flags;
            File file = getFile(dir);
            if (temp.renameTo(file)) {
                this.blocks = (int) (((file.length() + ((long) blockSize)) - 1) / ((long) blockSize));
                return;
            }
            throw new IOException("Can't rename " + temp + " to " + file);
        }

        public EntryFile(File dir, String tag, long timestampMillis) throws IOException {
            this.tag = TextUtils.safeIntern(tag);
            this.timestampMillis = timestampMillis;
            this.flags = 1;
            this.blocks = 0;
            new FileOutputStream(getFile(dir)).close();
        }

        public EntryFile(File file, int blockSize) {
            boolean parseFailure = false;
            String name = file.getName();
            if (name.contains("@data_app_anr@") || name.contains("@system_app_anr@") || name.contains("@data_app_crash@") || name.contains("@system_app_crash@")) {
                long time;
                int flag = 0;
                if (name.endsWith(".gz")) {
                    flag = 4;
                    name = name.substring(0, name.length() - 3);
                }
                if (name.endsWith(".txt")) {
                    flag |= 2;
                    name = name.substring(0, name.length() - 4);
                }
                this.flags = flag;
                String[] value = name.split("@");
                if (value.length == 7) {
                    this.tag = TextUtils.safeIntern(Uri.decode(value[5]));
                } else {
                    this.tag = null;
                }
                try {
                    time = Long.valueOf(name.substring(name.lastIndexOf(95) + 1, name.length())).longValue();
                } catch (NumberFormatException e) {
                    time = 0;
                }
                this.timestampMillis = time;
                this.blocks = (int) (((file.length() + ((long) blockSize)) - 1) / ((long) blockSize));
                return;
            }
            int flags = 0;
            String tag = null;
            long millis = 0;
            int at = name.lastIndexOf(64);
            if (at < 0) {
                parseFailure = true;
            } else {
                tag = Uri.decode(name.substring(0, at));
                if (name.endsWith(".gz")) {
                    flags = 4;
                    name = name.substring(0, name.length() - 3);
                }
                if (name.endsWith(".lost")) {
                    flags |= 1;
                    name = name.substring(at + 1, name.length() - 5);
                } else if (name.endsWith(".txt")) {
                    flags |= 2;
                    name = name.substring(at + 1, name.length() - 4);
                } else if (name.endsWith(".dat")) {
                    name = name.substring(at + 1, name.length() - 4);
                } else {
                    parseFailure = true;
                }
                if (!parseFailure) {
                    try {
                        millis = Long.parseLong(name);
                    } catch (NumberFormatException e2) {
                        parseFailure = true;
                    }
                }
            }
            if (parseFailure) {
                Slog.wtf(DropBoxManagerService.TAG, "Invalid filename: " + file);
                if (!name.contains("critical_event")) {
                    file.delete();
                }
                this.tag = null;
                this.flags = 1;
                this.timestampMillis = 0;
                this.blocks = 0;
                return;
            }
            this.blocks = (int) (((file.length() + ((long) blockSize)) - 1) / ((long) blockSize));
            this.tag = TextUtils.safeIntern(tag);
            this.flags = flags;
            this.timestampMillis = millis;
        }

        public EntryFile(long millis) {
            this.tag = null;
            this.timestampMillis = millis;
            this.flags = 1;
            this.blocks = 0;
        }

        public boolean hasFile() {
            return this.tag != null;
        }

        private String getExtension() {
            if ((this.flags & 1) != 0) {
                return ".lost";
            }
            return ((this.flags & 2) != 0 ? ".txt" : ".dat") + ((this.flags & 4) != 0 ? ".gz" : "");
        }

        public String getFilename() {
            String fileName;
            if (this.tag.contains("app_anr") || this.tag.contains("app_crash")) {
                fileName = DropBoxManagerService.getSaveDate() + "@" + DropBoxManagerService.mOtaVersion + "@" + DropBoxManagerService.mProcessName + "@" + Uri.encode(this.tag) + "@" + DropBoxManagerService.mIMEI + LocationManagerService.OPPO_FAKE_LOCATION_SPLIT + DropBoxManagerService.mTime + LocationManagerService.OPPO_FAKE_LOCATION_SPLIT + this.timestampMillis + getExtension();
            } else {
                fileName = Uri.encode(this.tag) + "@" + this.timestampMillis + getExtension();
            }
            return hasFile() ? fileName : null;
        }

        public File getFile(File dir) {
            return hasFile() ? new File(dir, getFilename()) : null;
        }

        public void deleteFile(File dir) {
            if (hasFile()) {
                getFile(dir).delete();
            }
        }
    }

    private static final class FileList implements Comparable<FileList> {
        public int blocks;
        public final TreeSet<EntryFile> contents;

        /* synthetic */ FileList(FileList -this0) {
            this();
        }

        private FileList() {
            this.blocks = 0;
            this.contents = new TreeSet();
        }

        public final int compareTo(FileList o) {
            if (this.blocks != o.blocks) {
                return o.blocks - this.blocks;
            }
            if (this == o) {
                return 0;
            }
            if (hashCode() < o.hashCode()) {
                return -1;
            }
            if (hashCode() > o.hashCode()) {
                return 1;
            }
            return 0;
        }
    }

    public DropBoxManagerService(Context context) {
        this(context, new File("/data/system/dropbox"), FgThread.get().getLooper());
    }

    public DropBoxManagerService(Context context, File path, Looper looper) {
        super(context);
        this.mAllFiles = null;
        this.mFilesByTag = null;
        this.mStatFs = null;
        this.mBlockSize = 0;
        this.mCachedQuotaBlocks = 0;
        this.mCachedQuotaUptimeMillis = 0;
        this.mBooted = false;
        this.mMaxFiles = -1;
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                DropBoxManagerService.this.mCachedQuotaUptimeMillis = 0;
                new Thread() {
                    public void run() {
                        try {
                            DropBoxManagerService.this.init();
                            DropBoxManagerService.this.trimToFit();
                        } catch (IOException e) {
                            Slog.e(DropBoxManagerService.TAG, "Can't init", e);
                        }
                    }
                }.start();
            }
        };
        this.mStub = new Stub() {
            public void add(Entry entry) {
                DropBoxManagerService.this.add(entry);
            }

            public boolean isTagEnabled(String tag) {
                return DropBoxManagerService.this.isTagEnabled(tag);
            }

            public Entry getNextEntry(String tag, long millis) {
                return DropBoxManagerService.this.getNextEntry(tag, millis);
            }

            public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                DropBoxManagerService.this.dump(fd, pw, args);
            }
        };
        this.mDropBoxDir = path;
        this.mContentResolver = getContext().getContentResolver();
        this.mHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    DropBoxManagerService.this.getContext().sendBroadcastAsUser((Intent) msg.obj, UserHandle.SYSTEM, "android.permission.READ_LOGS");
                }
            }
        };
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Jianhua.Lin@Plf.SDK, 2017-08-25 : Modify for EAP", property = OppoRomType.ROM)
    public void onStart() {
        publishBinderService("dropbox", this.mStub);
        mOtaVersion = SystemProperties.get("ro.build.version.ota");
    }

    public void onBootPhase(int phase) {
        switch (phase) {
            case 500:
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
                getContext().registerReceiver(this.mReceiver, filter);
                this.mContentResolver.registerContentObserver(Global.CONTENT_URI, true, new ContentObserver(new Handler()) {
                    public void onChange(boolean selfChange) {
                        DropBoxManagerService.this.mReceiver.onReceive(DropBoxManagerService.this.getContext(), (Intent) null);
                    }
                });
                return;
            case 1000:
                this.mBooted = true;
                return;
            default:
                return;
        }
    }

    public IDropBoxManagerService getServiceStub() {
        return this.mStub;
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Jianhua.Lin@Plf.SDK, 2017-08-25 : Modify for EAP", property = OppoRomType.ROM)
    public void add(Entry entry) {
        IOException e;
        Throwable th;
        File temp = null;
        InputStream input = null;
        AutoCloseable output = null;
        String tag = entry.getTag();
        boolean assertEnable = SystemProperties.getBoolean("persist.sys.assert.panic", false);
        boolean agreeUserExperience = getNetworkAccess(getContext());
        if (tag.contains("app_anr") || tag.contains("app_crash")) {
            initProcessName(entry);
            initTimeValue(entry);
            initIMEI();
        }
        int flags = entry.getFlags();
        if ((flags & 1) != 0) {
            throw new IllegalArgumentException();
        }
        init();
        if (isTagEnabled(tag)) {
            long max;
            long lastTrim;
            byte[] buffer;
            int read;
            File file;
            FileOutputStream foutput;
            OutputStream bufferedOutputStream;
            if (agreeUserExperience) {
                if (tag.equals("SYSTEM_SERVER_GZ") || tag.equals("SYSTEM_SERVER_WATCHDOG")) {
                    String systemcrashFile = SystemProperties.get("persist.sys.send.file", "null");
                    if (!systemcrashFile.equals("null")) {
                        ArrayList<String> gzFiles = new ArrayList();
                        gzFiles.add(systemcrashFile);
                        Slog.d(TAG, "send feedback broadcast!,tag =" + tag);
                        sendFeedbackBroadcast(gzFiles, tag);
                        SystemProperties.set("persist.sys.send.file", "null");
                        IoUtils.closeQuietly(null);
                        IoUtils.closeQuietly(null);
                        entry.close();
                        return;
                    }
                }
            }
            try {
                if (tag.equals("SYSTEM_SERVER_GZ") || tag.equals("SYSTEM_SERVER_WATCHDOG") || tag.equals("SYSTEM_SERVER") || tag.equals("SYSTEM_TOMBSTONE_CRASH")) {
                    OppoManager.writeLogToPartition(OppoManager.TYPE_ANDROID_CRASH, "system_restart", "ANDROID", "crash", getContext().getResources().getString(17039480));
                } else if (tag.equals("SYSTEM_LAST_KMSG")) {
                    String header = entry.getText(32);
                    Slog.v(TAG, "SYSTEM_LAST_KMSG text = " + header);
                    if (header == null || !header.startsWith("unknown reboot")) {
                        OppoManager.writeLogToPartition(OppoManager.TYPE_PANIC, "kernel_panic", "KERNEL", "panic", getContext().getResources().getString(17040986));
                    } else {
                        OppoManager.writeLogToPartition(OppoManager.TYPE_ANDROID_UNKNOWN_REBOOT, "unknow reboot", "KERNEL", "panic", getContext().getResources().getString(17041003));
                    }
                }
                max = trimToFit();
                lastTrim = System.currentTimeMillis();
                buffer = new byte[this.mBlockSize];
                input = entry.getInputStream();
                read = 0;
                while (read < buffer.length) {
                    int n = input.read(buffer, read, buffer.length - read);
                    if (n <= 0) {
                        break;
                    }
                    read += n;
                }
                file = new File(this.mDropBoxDir, "drop" + Thread.currentThread().getId() + ".tmp");
                try {
                    int bufferSize = this.mBlockSize;
                    if (bufferSize > 4096) {
                        bufferSize = 4096;
                    }
                    if (bufferSize < 512) {
                        bufferSize = 512;
                    }
                    foutput = new FileOutputStream(file);
                    bufferedOutputStream = new BufferedOutputStream(foutput, bufferSize);
                } catch (IOException e2) {
                    e = e2;
                    temp = file;
                } catch (Throwable th2) {
                    th = th2;
                    temp = file;
                }
            } catch (IOException e3) {
                e = e3;
            }
            Object output2;
            try {
                if (read == buffer.length && (flags & 4) == 0) {
                    output = new GZIPOutputStream(bufferedOutputStream);
                    flags |= 4;
                } else {
                    output2 = bufferedOutputStream;
                }
                do {
                    output.write(buffer, 0, read);
                    long now = System.currentTimeMillis();
                    if (now - lastTrim > 30000) {
                        max = trimToFit();
                        lastTrim = now;
                    }
                    read = input.read(buffer);
                    if (read <= 0) {
                        FileUtils.sync(foutput);
                        output.close();
                        output = null;
                    } else {
                        output.flush();
                    }
                    if (file.length() > max) {
                        Slog.w(TAG, "Dropping: " + tag + " (" + file.length() + " > " + max + " bytes)");
                        file.delete();
                        temp = null;
                        break;
                    }
                } while (read > 0);
                temp = file;
                long time = createEntry(temp, tag, flags);
                temp = null;
                Intent dropboxIntent = new Intent("android.intent.action.DROPBOX_ENTRY_ADDED");
                dropboxIntent.putExtra("tag", tag);
                dropboxIntent.putExtra("time", time);
                if (!this.mBooted) {
                    dropboxIntent.addFlags(1073741824);
                }
                this.mHandler.sendMessage(this.mHandler.obtainMessage(1, dropboxIntent));
                File[] logFiles = new File("/data/system/dropbox").listFiles();
                int i = 0;
                while (logFiles != null && i < logFiles.length) {
                    String name = logFiles[i].getName();
                    if (name.endsWith(".gz")) {
                        name = name.substring(0, name.length() - 3);
                    }
                    if (name.endsWith(".lost")) {
                        name = name.substring(0, name.length() - 5);
                    } else if (name.endsWith(".txt")) {
                        name = name.substring(0, name.length() - 4);
                    } else if (name.endsWith(".dat")) {
                        name = name.substring(0, name.length() - 4);
                    }
                    if (name.contains(String.valueOf(time)) && name.contains(tag)) {
                        ArrayList<String> mFiles = new ArrayList();
                        mFiles.add("/data/system/dropbox/" + logFiles[i].getName());
                        Slog.d(TAG, "file :: /data/system/dropbox/" + logFiles[i].getName());
                        if (assertEnable) {
                            if (tag.equals("system_server_lowmem")) {
                                Slog.d(TAG, "the tag is  :: " + tag);
                            } else if ((tag.startsWith("system_server", 0) && (tag.startsWith("system_server_wtf", 0) ^ 1) != 0) || tag.equals("system_app_crash") || tag.equals("system_app_anr") || tag.equals("data_app_crash") || tag.equals("data_app_anr")) {
                                Slog.d(TAG, "assert append,the tag is  :: " + tag);
                                ASSERT.epitaph(logFiles[i], tag, flags, getContext());
                            }
                        }
                        if (agreeUserExperience) {
                            if (tag.startsWith("system_server", 0) && (tag.startsWith("system_server_wtf", 0) ^ 1) != 0 && (tag.startsWith("system_server_lowmem", 0) ^ 1) != 0) {
                                Slog.d(TAG, "save crash file!");
                                SystemProperties.set("persist.sys.panic.file", "/data/system/dropbox/" + logFiles[i].getName());
                            } else if (tag.equals("SYSTEM_SERVER") || tag.equals("SYSTEM_LAST_KMSG") || tag.equals("SYSTEM_TOMBSTONE_CRASH")) {
                                Slog.d(TAG, "send feedback broadcast!,tag =" + tag);
                                if (!getContext().getPackageManager().hasSystemFeature("oppo.cta.support")) {
                                    sendFeedbackBroadcast(mFiles, tag);
                                }
                            }
                        }
                    }
                    i++;
                }
                IoUtils.closeQuietly(output);
                IoUtils.closeQuietly(input);
                entry.close();
            } catch (IOException e4) {
                e = e4;
                output2 = bufferedOutputStream;
                temp = file;
                try {
                    Slog.e(TAG, "Can't write: " + tag, e);
                    IoUtils.closeQuietly(output);
                    IoUtils.closeQuietly(input);
                    entry.close();
                    if (temp != null) {
                        temp.delete();
                    }
                    return;
                } catch (Throwable th3) {
                    th = th3;
                    IoUtils.closeQuietly(output);
                    IoUtils.closeQuietly(input);
                    entry.close();
                    if (temp != null) {
                        temp.delete();
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                output2 = bufferedOutputStream;
                temp = file;
                IoUtils.closeQuietly(output);
                IoUtils.closeQuietly(input);
                entry.close();
                if (temp != null) {
                    temp.delete();
                }
                throw th;
            }
            return;
        }
        IoUtils.closeQuietly(null);
        IoUtils.closeQuietly(null);
        entry.close();
    }

    public boolean isTagEnabled(String tag) {
        try {
            boolean equals = "disabled".equals(Global.getString(this.mContentResolver, "dropbox:" + tag)) ^ 1;
            return equals;
        } finally {
            Binder.restoreCallingIdentity(Binder.clearCallingIdentity());
        }
    }

    public synchronized Entry getNextEntry(String tag, long millis) {
        if (getContext().checkCallingOrSelfPermission("android.permission.READ_LOGS") != 0) {
            throw new SecurityException("READ_LOGS permission required");
        }
        try {
            init();
            FileList list = tag == null ? this.mAllFiles : (FileList) this.mFilesByTag.get(tag);
            if (list == null) {
                return null;
            }
            for (EntryFile entry : list.contents.tailSet(new EntryFile(1 + millis))) {
                if (entry.tag != null) {
                    if ((entry.flags & 1) != 0) {
                        return new Entry(entry.tag, entry.timestampMillis);
                    }
                    File file = entry.getFile(this.mDropBoxDir);
                    try {
                        return new Entry(entry.tag, entry.timestampMillis, file, entry.flags);
                    } catch (IOException e) {
                        Slog.wtf(TAG, "Can't read: " + file, e);
                    }
                }
            }
            return null;
        } catch (IOException e2) {
            Slog.e(TAG, "Can't init", e2);
            return null;
        }
    }

    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Throwable e;
        Throwable th;
        if (DumpUtils.checkDumpAndUsageStatsPermission(getContext(), TAG, pw)) {
            try {
                init();
                StringBuilder out = new StringBuilder();
                boolean doPrint = false;
                boolean doFile = false;
                ArrayList<String> searchArgs = new ArrayList();
                int i = 0;
                while (args != null && i < args.length) {
                    if (args[i].equals("-p") || args[i].equals("--print")) {
                        doPrint = true;
                    } else if (args[i].equals("-f") || args[i].equals("--file")) {
                        doFile = true;
                    } else if (args[i].equals("-h") || args[i].equals("--help")) {
                        pw.println("Dropbox (dropbox) dump options:");
                        pw.println("  [-h|--help] [-p|--print] [-f|--file] [timestamp]");
                        pw.println("    -h|--help: print this help");
                        pw.println("    -p|--print: print full contents of each entry");
                        pw.println("    -f|--file: print path of each entry's file");
                        pw.println("  [timestamp] optionally filters to only those entries.");
                        return;
                    } else if (args[i].startsWith("-")) {
                        out.append("Unknown argument: ").append(args[i]).append("\n");
                    } else {
                        searchArgs.add(args[i]);
                    }
                    i++;
                }
                out.append("Drop box contents: ").append(this.mAllFiles.contents.size()).append(" entries\n");
                out.append("Max entries: ").append(this.mMaxFiles).append("\n");
                if (!searchArgs.isEmpty()) {
                    out.append("Searching for:");
                    for (String a : searchArgs) {
                        out.append(" ").append(a);
                    }
                    out.append("\n");
                }
                int numFound = 0;
                int numArgs = searchArgs.size();
                Time time = new Time();
                out.append("\n");
                for (EntryFile entry : this.mAllFiles.contents) {
                    time.set(entry.timestampMillis);
                    String date = time.format("%Y-%m-%d %H:%M:%S");
                    boolean match = true;
                    for (i = 0; i < numArgs && match; i++) {
                        String arg = (String) searchArgs.get(i);
                        match = !date.contains(arg) ? arg.equals(entry.tag) : true;
                    }
                    if (match) {
                        numFound++;
                        if (doPrint) {
                            out.append("========================================\n");
                        }
                        out.append(date).append(" ").append(entry.tag == null ? "(no tag)" : entry.tag);
                        File file = entry.getFile(this.mDropBoxDir);
                        if (file == null) {
                            out.append(" (no file)\n");
                        } else if ((entry.flags & 1) != 0) {
                            out.append(" (contents lost)\n");
                        } else {
                            out.append(" (");
                            if ((entry.flags & 4) != 0) {
                                out.append("compressed ");
                            }
                            out.append((entry.flags & 2) != 0 ? "text" : "data");
                            out.append(", ").append(file.length()).append(" bytes)\n");
                            if (doFile || (doPrint && (entry.flags & 2) == 0)) {
                                if (!doPrint) {
                                    out.append("    ");
                                }
                                out.append(file.getPath()).append("\n");
                            }
                            if ((entry.flags & 2) != 0 && (doPrint || (doFile ^ 1) != 0)) {
                                InputStreamReader isr = null;
                                Entry dbe;
                                try {
                                    dbe = new Entry(entry.tag, entry.timestampMillis, file, entry.flags);
                                    if (doPrint) {
                                        InputStreamReader inputStreamReader;
                                        try {
                                            inputStreamReader = new InputStreamReader(dbe.getInputStream());
                                        } catch (IOException e2) {
                                            e = e2;
                                            try {
                                                out.append("*** ").append(e.toString()).append("\n");
                                                Slog.e(TAG, "Can't read: " + file, e);
                                                if (dbe != null) {
                                                    dbe.close();
                                                }
                                                if (isr != null) {
                                                    try {
                                                        isr.close();
                                                    } catch (IOException e3) {
                                                    }
                                                }
                                                if (doPrint) {
                                                    continue;
                                                } else {
                                                    out.append("\n");
                                                }
                                            } catch (Throwable th2) {
                                                th = th2;
                                            }
                                        }
                                        try {
                                            char[] buf = new char[4096];
                                            boolean newline = false;
                                            while (true) {
                                                int n = inputStreamReader.read(buf);
                                                if (n <= 0) {
                                                    break;
                                                }
                                                out.append(buf, 0, n);
                                                newline = buf[n + -1] == 10;
                                                if (out.length() > 65536) {
                                                    pw.write(out.toString());
                                                    out.setLength(0);
                                                }
                                            }
                                            if (newline) {
                                                isr = inputStreamReader;
                                            } else {
                                                out.append("\n");
                                                isr = inputStreamReader;
                                            }
                                        } catch (IOException e4) {
                                            e = e4;
                                            isr = inputStreamReader;
                                            out.append("*** ").append(e.toString()).append("\n");
                                            Slog.e(TAG, "Can't read: " + file, e);
                                            if (dbe != null) {
                                                dbe.close();
                                            }
                                            if (isr != null) {
                                                try {
                                                    isr.close();
                                                } catch (IOException e32) {
                                                }
                                            }
                                            if (doPrint) {
                                                continue;
                                            } else {
                                                out.append("\n");
                                            }
                                        } catch (Throwable th3) {
                                            th = th3;
                                            isr = inputStreamReader;
                                            if (dbe != null) {
                                                dbe.close();
                                            }
                                            if (isr != null) {
                                                try {
                                                    isr.close();
                                                } catch (IOException e5) {
                                                }
                                            }
                                            throw th;
                                        }
                                    }
                                    String text = dbe.getText(70);
                                    out.append("    ");
                                    if (text == null) {
                                        out.append("[null]");
                                    } else {
                                        boolean truncated = text.length() == 70;
                                        out.append(text.trim().replace(10, '/'));
                                        if (truncated) {
                                            out.append(" ...");
                                        }
                                    }
                                    out.append("\n");
                                    if (dbe != null) {
                                        dbe.close();
                                    }
                                    if (isr != null) {
                                        try {
                                            isr.close();
                                        } catch (IOException e6) {
                                        }
                                    }
                                } catch (IOException e7) {
                                    e = e7;
                                    dbe = null;
                                    out.append("*** ").append(e.toString()).append("\n");
                                    Slog.e(TAG, "Can't read: " + file, e);
                                    if (dbe != null) {
                                        dbe.close();
                                    }
                                    if (isr != null) {
                                        try {
                                            isr.close();
                                        } catch (IOException e322) {
                                        }
                                    }
                                    if (doPrint) {
                                        continue;
                                    } else {
                                        out.append("\n");
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    dbe = null;
                                }
                            }
                            if (doPrint) {
                                out.append("\n");
                            } else {
                                continue;
                            }
                        }
                    }
                }
                if (numFound == 0) {
                    out.append("(No entries found.)\n");
                }
                if (args == null || args.length == 0) {
                    if (!doPrint) {
                        out.append("\n");
                    }
                    out.append("Usage: dumpsys dropbox [--print|--file] [YYYY-mm-dd] [HH:MM:SS] [tag]\n");
                }
                pw.write(out.toString());
            } catch (Throwable e8) {
                pw.println("Can't initialize: " + e8);
                Slog.e(TAG, "Can't init", e8);
            }
        }
    }

    private synchronized void init() throws IOException {
        if (this.mStatFs == null) {
            if (this.mDropBoxDir.isDirectory() || (this.mDropBoxDir.mkdirs() ^ 1) == 0) {
                try {
                    this.mStatFs = new StatFs(this.mDropBoxDir.getPath());
                    this.mBlockSize = this.mStatFs.getBlockSize();
                } catch (IllegalArgumentException e) {
                    throw new IOException("Can't statfs: " + this.mDropBoxDir);
                }
            }
            throw new IOException("Can't mkdir: " + this.mDropBoxDir);
        }
        if (this.mAllFiles == null) {
            File[] files = this.mDropBoxDir.listFiles();
            if (files == null) {
                throw new IOException("Can't list files: " + this.mDropBoxDir);
            }
            this.mAllFiles = new FileList();
            this.mFilesByTag = new ArrayMap();
            for (File file : files) {
                if (file.getName().endsWith(".tmp")) {
                    Slog.i(TAG, "Cleaning temp file: " + file);
                    file.delete();
                } else {
                    EntryFile entry = new EntryFile(file, this.mBlockSize);
                    if (entry.hasFile()) {
                        enrollEntry(entry);
                    }
                }
            }
        }
    }

    private synchronized void enrollEntry(EntryFile entry) {
        this.mAllFiles.contents.add(entry);
        FileList fileList = this.mAllFiles;
        fileList.blocks += entry.blocks;
        if (entry.hasFile() && entry.blocks > 0) {
            FileList tagFiles = (FileList) this.mFilesByTag.get(entry.tag);
            if (tagFiles == null) {
                tagFiles = new FileList();
                this.mFilesByTag.put(TextUtils.safeIntern(entry.tag), tagFiles);
            }
            tagFiles.contents.add(entry);
            tagFiles.blocks += entry.blocks;
        }
    }

    private synchronized long createEntry(File temp, String tag, int flags) throws IOException {
        long t;
        t = System.currentTimeMillis();
        SortedSet<EntryFile> tail = this.mAllFiles.contents.tailSet(new EntryFile(10000 + t));
        EntryFile[] future = null;
        if (!tail.isEmpty()) {
            future = (EntryFile[]) tail.toArray(new EntryFile[tail.size()]);
            tail.clear();
        }
        if (!this.mAllFiles.contents.isEmpty()) {
            t = Math.max(t, ((EntryFile) this.mAllFiles.contents.last()).timestampMillis + 1);
        }
        if (future != null) {
            int i = 0;
            int length = future.length;
            while (true) {
                int i2 = i;
                if (i2 >= length) {
                    break;
                }
                long t2;
                EntryFile late = future[i2];
                FileList fileList = this.mAllFiles;
                fileList.blocks -= late.blocks;
                FileList tagFiles = (FileList) this.mFilesByTag.get(late.tag);
                if (tagFiles != null && tagFiles.contents.remove(late)) {
                    tagFiles.blocks -= late.blocks;
                }
                if ((late.flags & 1) == 0) {
                    t2 = t + 1;
                    enrollEntry(new EntryFile(late.getFile(this.mDropBoxDir), this.mDropBoxDir, late.tag, t, late.flags, this.mBlockSize));
                } else {
                    t2 = t + 1;
                    enrollEntry(new EntryFile(this.mDropBoxDir, late.tag, t));
                }
                t = t2;
                i = i2 + 1;
            }
        }
        if (temp == null) {
            enrollEntry(new EntryFile(this.mDropBoxDir, tag, t));
        } else {
            enrollEntry(new EntryFile(temp, this.mDropBoxDir, tag, t, flags, this.mBlockSize));
        }
        return t;
    }

    private synchronized long trimToFit() throws IOException {
        EntryFile entry;
        FileList tag;
        FileList fileList;
        int ageSeconds = Global.getInt(this.mContentResolver, "dropbox_age_seconds", DEFAULT_AGE_SECONDS);
        this.mMaxFiles = Global.getInt(this.mContentResolver, "dropbox_max_files", ActivityManager.isLowRamDeviceStatic() ? 300 : 1000);
        long cutoffMillis = System.currentTimeMillis() - ((long) (ageSeconds * 1000));
        while (!this.mAllFiles.contents.isEmpty()) {
            entry = (EntryFile) this.mAllFiles.contents.first();
            if (entry.timestampMillis > cutoffMillis && this.mAllFiles.contents.size() < this.mMaxFiles) {
                break;
            }
            tag = (FileList) this.mFilesByTag.get(entry.tag);
            if (tag != null && tag.contents.remove(entry)) {
                tag.blocks -= entry.blocks;
            }
            if (this.mAllFiles.contents.remove(entry)) {
                fileList = this.mAllFiles;
                fileList.blocks -= entry.blocks;
            }
            entry.deleteFile(this.mDropBoxDir);
        }
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis > this.mCachedQuotaUptimeMillis + FaceDaemonWrapper.TIMEOUT_FACED_BINDERCALL_CHECK) {
            int quotaPercent = Global.getInt(this.mContentResolver, "dropbox_quota_percent", 10);
            int reservePercent = Global.getInt(this.mContentResolver, "dropbox_reserve_percent", 10);
            int quotaKb = Global.getInt(this.mContentResolver, "dropbox_quota_kb", DEFAULT_QUOTA_KB);
            if (!(this.mDropBoxDir.exists() || (this.mDropBoxDir.isDirectory() ^ 1) == 0)) {
                this.mDropBoxDir.mkdirs();
            }
            try {
                this.mStatFs.restat(this.mDropBoxDir.getPath());
                this.mCachedQuotaBlocks = Math.min((quotaKb * 1024) / this.mBlockSize, Math.max(0, ((this.mStatFs.getAvailableBlocks() - ((this.mStatFs.getBlockCount() * reservePercent) / 100)) * quotaPercent) / 100));
                this.mCachedQuotaUptimeMillis = uptimeMillis;
            } catch (IllegalArgumentException e) {
                throw new IOException("Can't restat: " + this.mDropBoxDir);
            }
        }
        if (this.mAllFiles.blocks > this.mCachedQuotaBlocks) {
            int unsqueezed = this.mAllFiles.blocks;
            int squeezed = 0;
            TreeSet<FileList> treeSet = new TreeSet(this.mFilesByTag.values());
            for (FileList tag2 : treeSet) {
                if (squeezed > 0 && tag2.blocks <= (this.mCachedQuotaBlocks - unsqueezed) / squeezed) {
                    break;
                }
                unsqueezed -= tag2.blocks;
                squeezed++;
            }
            int tagQuota = (this.mCachedQuotaBlocks - unsqueezed) / squeezed;
            for (FileList tag22 : treeSet) {
                if (this.mAllFiles.blocks < this.mCachedQuotaBlocks) {
                    break;
                }
                while (tag22.blocks > tagQuota && (tag22.contents.isEmpty() ^ 1) != 0) {
                    entry = (EntryFile) tag22.contents.first();
                    if (tag22.contents.remove(entry)) {
                        tag22.blocks -= entry.blocks;
                    }
                    if (this.mAllFiles.contents.remove(entry)) {
                        fileList = this.mAllFiles;
                        fileList.blocks -= entry.blocks;
                    }
                    try {
                        entry.deleteFile(this.mDropBoxDir);
                        enrollEntry(new EntryFile(this.mDropBoxDir, entry.tag, entry.timestampMillis));
                    } catch (IOException e2) {
                        Slog.e(TAG, "Can't write tombstone file", e2);
                    }
                }
            }
        }
        return (long) (this.mCachedQuotaBlocks * this.mBlockSize);
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "Jianhua.Lin@Plf.SDK, 2017-08-22 : Add for EAP", property = OppoRomType.ROM)
    private void initIMEI() {
        mIMEI = ColorOSTelephonyManager.getDefault(getContext()).colorGetImei(0);
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "Jianhua.Lin@Plf.SDK, 2017-08-22 : Add for EAP", property = OppoRomType.ROM)
    private void initProcessName(Entry entry) {
        try {
            String text = entry.getText(500);
            String value = text.substring(text.indexOf("Package: ") + 9, text.indexOf("Foreground:") - 1);
            mProcessName = value.substring(0, value.indexOf(" v"));
        } catch (Exception e) {
            Slog.d(TAG, "fail to init package name, " + e);
            mProcessName = Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "Jianhua.Lin@Plf.SDK, 2017-08-22 : Add for EAP", property = OppoRomType.ROM)
    private void initTimeValue(Entry entry) {
        try {
            String info = entry.getText(300);
            mTime = info.substring(info.indexOf("Time: ") + 6, info.indexOf("Flags:") - 1);
        } catch (Exception e) {
            Slog.d(TAG, "fail to init time value, " + e);
            mTime = "0";
        }
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "Jianhua.Lin@Plf.SDK, 2017-08-22 : Add for EAP", property = OppoRomType.ROM)
    private static String getSaveDate() {
        return new SimpleDateFormat("yyyy@MM@dd", Locale.US).format(new Date());
    }

    private void sendFeedbackBroadcast(ArrayList<String> files, String tag) {
        Object pakageName = null;
        try {
            pakageName = ActivityManagerNative.getDefault().getTopAppName().getPackageName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!"com.nearme.feedback".equals(pakageName)) {
            try {
                String dumpEnvironmentPath = "/cache/environment";
                String dumpEnvironmentZipFile = "/data/system/environment.zip";
                String dumpEnvironmentGzFile = "/data/system/dropbox/" + tag + "@" + System.currentTimeMillis() + ".dat.gz";
                String dumpEnvironmentGzFile_temp = "/data/system/dropbox/feedbacktempfile.dat.gz";
                String currentFile = (String) files.get(0);
                Slog.d(TAG, "prepare zip dumpEnvironmentGzFile!" + currentFile);
                File dumpDir = new File(dumpEnvironmentPath);
                if (!tag.equals("SYSTEM_LAST_KMSG") && dumpDir.exists() && dumpDir.isDirectory()) {
                    Slog.d(TAG, "start zip dumpEnvironmentGzFile!");
                    zipFolder(dumpEnvironmentPath, currentFile, dumpEnvironmentZipFile);
                    gzipFile(dumpEnvironmentZipFile, dumpEnvironmentGzFile_temp);
                    if (new File(dumpEnvironmentGzFile_temp).exists() && new File(dumpEnvironmentGzFile_temp).renameTo(new File(dumpEnvironmentGzFile))) {
                        Slog.d(TAG, "start send dumpEnvironmentGzFile!");
                        files.clear();
                        files.add(dumpEnvironmentGzFile);
                        new File(currentFile).delete();
                    }
                    new File(dumpEnvironmentZipFile).delete();
                    deleteFolder(dumpEnvironmentPath);
                    deleteFolder("/data/system/dropbox/extra_log");
                }
            } catch (Exception e2) {
                Slog.e(TAG, "dumpEnvironmentGzFile failed!");
                e2.printStackTrace();
            }
            Intent dropboxIntent = new Intent("com.nearme.feedback.feedback");
            dropboxIntent.addFlags(32);
            dropboxIntent.putStringArrayListExtra("filePath", files);
            dropboxIntent.putExtra("boot_reason", "system");
            getContext().sendBroadcast(dropboxIntent);
        }
    }

    public static boolean getNetworkAccess(Context ctx) {
        int val = -1;
        try {
            val = System.getInt(ctx.getContentResolver(), OPPO_CTA_USER_ECPERIENCE);
        } catch (SettingNotFoundException e) {
            Slog.e(TAG, "get oppo_cta_user_experience FAIL!!");
        }
        if (val == 1) {
            return true;
        }
        return false;
    }

    private static void zipFolder(String inputFolderPath, String currentFilePath, String outZipPath) {
        IOException ioe;
        FileOutputStream fileOutputStream;
        Throwable th;
        ZipOutputStream zos = null;
        FileInputStream fis = null;
        try {
            FileOutputStream fos = new FileOutputStream(outZipPath);
            try {
                ZipOutputStream zos2 = new ZipOutputStream(fos);
                try {
                    File srcFile = new File(inputFolderPath);
                    File[] files = srcFile.listFiles();
                    Slog.d(TAG, "Zip directory: " + srcFile.getName());
                    int i = 0;
                    while (true) {
                        FileInputStream fis2;
                        try {
                            fis2 = fis;
                            if (i < files.length + 1) {
                                byte[] buffer = new byte[1024];
                                if (i == files.length) {
                                    File currentFile = new File(currentFilePath);
                                    if (currentFile == null || (currentFile.canRead() ^ 1) != 0) {
                                        fis = fis2;
                                    } else {
                                        fis = new FileInputStream(currentFile);
                                        zos2.putNextEntry(new ZipEntry(currentFile.getName()));
                                        while (true) {
                                            int length = fis.read(buffer);
                                            if (length <= 0) {
                                                break;
                                            }
                                            zos2.write(buffer, 0, length);
                                        }
                                        zos2.closeEntry();
                                        fis.close();
                                        fis = null;
                                    }
                                } else if (files[i].canRead()) {
                                    fis = new FileInputStream(files[i]);
                                    zos2.putNextEntry(new ZipEntry(files[i].getName()));
                                    while (true) {
                                        int length2 = fis.read(buffer);
                                        if (length2 <= 0) {
                                            break;
                                        }
                                        zos2.write(buffer, 0, length2);
                                    }
                                    zos2.closeEntry();
                                    fis.close();
                                    fis = null;
                                } else {
                                    fis = fis2;
                                }
                                i++;
                            } else {
                                if (zos2 != null) {
                                    try {
                                        zos2.close();
                                    } catch (Exception e) {
                                        Slog.e(TAG, "zos.close() error " + e.getMessage());
                                    }
                                }
                                if (null != null) {
                                    fis2.close();
                                }
                                return;
                            }
                        } catch (IOException e2) {
                            ioe = e2;
                            fis = fis2;
                            zos = zos2;
                            fileOutputStream = fos;
                            try {
                                Slog.e(TAG, ioe.getMessage());
                                if (zos != null) {
                                    try {
                                        zos.close();
                                    } catch (Exception e3) {
                                        Slog.e(TAG, "zos.close() error " + e3.getMessage());
                                        return;
                                    }
                                }
                                if (fis == null) {
                                    fis.close();
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                if (zos != null) {
                                    try {
                                        zos.close();
                                    } catch (Exception e32) {
                                        Slog.e(TAG, "zos.close() error " + e32.getMessage());
                                        throw th;
                                    }
                                }
                                if (fis != null) {
                                    fis.close();
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            fis = fis2;
                            zos = zos2;
                            if (zos != null) {
                                try {
                                    zos.close();
                                } catch (Exception e322) {
                                    Slog.e(TAG, "zos.close() error " + e322.getMessage());
                                    throw th;
                                }
                            }
                            if (fis != null) {
                                fis.close();
                            }
                            throw th;
                        }
                    }
                } catch (IOException e4) {
                    ioe = e4;
                    zos = zos2;
                    fileOutputStream = fos;
                } catch (Throwable th4) {
                    th = th4;
                    zos = zos2;
                    fileOutputStream = fos;
                }
            } catch (IOException e5) {
                ioe = e5;
                Slog.e(TAG, ioe.getMessage());
                if (zos != null) {
                    try {
                        zos.close();
                    } catch (Exception e3222) {
                        Slog.e(TAG, "zos.close() error " + e3222.getMessage());
                        return;
                    }
                }
                if (fis == null) {
                    fis.close();
                }
            } catch (Throwable th5) {
                th = th5;
                fileOutputStream = fos;
                if (zos != null) {
                    try {
                        zos.close();
                    } catch (Exception e32222) {
                        Slog.e(TAG, "zos.close() error " + e32222.getMessage());
                        throw th;
                    }
                }
                if (fis != null) {
                    fis.close();
                }
                throw th;
            }
        } catch (IOException e6) {
            ioe = e6;
            Slog.e(TAG, ioe.getMessage());
            if (zos != null) {
                try {
                    zos.close();
                } catch (Exception e322222) {
                    Slog.e(TAG, "zos.close() error " + e322222.getMessage());
                    return;
                }
            }
            if (fis == null) {
                fis.close();
            }
        }
    }

    public void gzipFile(String source_filepath, String destinaton_zip_filepath) {
        byte[] buffer = new byte[1024];
        try {
            GZIPOutputStream gzipOuputStream = new GZIPOutputStream(new FileOutputStream(destinaton_zip_filepath));
            FileInputStream fileInput = new FileInputStream(source_filepath);
            while (true) {
                int bytes_read = fileInput.read(buffer);
                if (bytes_read > 0) {
                    gzipOuputStream.write(buffer, 0, bytes_read);
                } else {
                    fileInput.close();
                    gzipOuputStream.finish();
                    gzipOuputStream.close();
                    Slog.d(TAG, "The file was compressed successfully!");
                    return;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void deleteFolder(String path) {
        try {
            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                String[] fileList = file.list();
                for (int i = 0; i < fileList.length; i++) {
                    File temp;
                    if (path.endsWith(File.separator)) {
                        temp = new File(path + fileList[i]);
                    } else {
                        temp = new File(path + File.separator + fileList[i]);
                    }
                    if (temp.isFile()) {
                        temp.delete();
                    }
                }
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
