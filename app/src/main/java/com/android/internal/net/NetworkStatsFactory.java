package com.android.internal.net;

import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.util.ArrayMap;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ProcFileReader;
import com.android.server.NetworkManagementSocketTagger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ProtocolException;
import libcore.io.IoUtils;

public class NetworkStatsFactory {
    private static final String CLATD_INTERFACE_PREFIX = "v4-";
    private static final boolean DEBUG = false;
    private static final int IPV4V6_HEADER_DELTA = 20;
    private static final boolean SANITY_CHECK_NATIVE = false;
    private static final String TAG = "NetworkStatsFactory";
    private static final boolean USE_NATIVE_PARSING = true;
    @GuardedBy("sStackedIfaces")
    private static final ArrayMap<String, String> sStackedIfaces = new ArrayMap();
    private final File mStatsXtIfaceAll;
    private final File mStatsXtIfaceFmt;
    private final File mStatsXtUid;
    private final File mStatsXtUidWithPids;

    public static native int nativeReadNetworkStatsDetail(NetworkStats networkStats, String str, int i, String[] strArr, int i2);

    public static native int nativeReadNetworkStatsDetailWithPids(NetworkStats networkStats, String str, int i);

    public static void noteStackedIface(String stackedIface, String baseIface) {
        synchronized (sStackedIfaces) {
            if (baseIface != null) {
                sStackedIfaces.put(stackedIface, baseIface);
            } else {
                sStackedIfaces.remove(stackedIface);
            }
        }
    }

    public NetworkStatsFactory() {
        this(new File("/proc/"));
    }

    public NetworkStatsFactory(File procRoot) {
        this.mStatsXtIfaceAll = new File(procRoot, "net/xt_qtaguid/iface_stat_all");
        this.mStatsXtIfaceFmt = new File(procRoot, "net/xt_qtaguid/iface_stat_fmt");
        this.mStatsXtUid = new File(procRoot, "net/xt_qtaguid/stats");
        this.mStatsXtUidWithPids = new File(procRoot, "net/xt_qtaguid/stats_pid");
    }

    public NetworkStats readNetworkStatsSummaryDev() throws IOException {
        RuntimeException e;
        Throwable th;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        Entry entry = new Entry();
        AutoCloseable reader = null;
        try {
            ProcFileReader reader2 = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceAll));
            while (reader2.hasMoreData()) {
                try {
                    entry.iface = reader2.nextString();
                    entry.uid = -1;
                    entry.set = -1;
                    entry.tag = 0;
                    boolean active = reader2.nextInt() != 0;
                    entry.rxBytes = reader2.nextLong();
                    entry.rxPackets = reader2.nextLong();
                    entry.txBytes = reader2.nextLong();
                    entry.txPackets = reader2.nextLong();
                    if (active) {
                        entry.rxBytes += reader2.nextLong();
                        entry.rxPackets += reader2.nextLong();
                        entry.txBytes += reader2.nextLong();
                        entry.txPackets += reader2.nextLong();
                    }
                    stats.addValues(entry);
                    reader2.finishLine();
                } catch (NullPointerException e2) {
                    e = e2;
                    reader = reader2;
                    try {
                        throw new ProtocolException("problem parsing stats", e);
                    } catch (Throwable th2) {
                        th = th2;
                        IoUtils.closeQuietly(reader);
                        StrictMode.setThreadPolicy(savedPolicy);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    Object reader3 = reader2;
                    IoUtils.closeQuietly(reader);
                    StrictMode.setThreadPolicy(savedPolicy);
                    throw th;
                }
            }
            IoUtils.closeQuietly(reader2);
            StrictMode.setThreadPolicy(savedPolicy);
            return stats;
        } catch (NullPointerException e3) {
            e = e3;
            throw new ProtocolException("problem parsing stats", e);
        }
    }

    public NetworkStats readNetworkStatsSummaryXt() throws IOException {
        RuntimeException e;
        Throwable th;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        if (!this.mStatsXtIfaceFmt.exists()) {
            return null;
        }
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        Entry entry = new Entry();
        AutoCloseable reader = null;
        try {
            ProcFileReader reader2 = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceFmt));
            try {
                reader2.finishLine();
                while (reader2.hasMoreData()) {
                    entry.iface = reader2.nextString();
                    entry.uid = -1;
                    entry.set = -1;
                    entry.tag = 0;
                    entry.rxBytes = reader2.nextLong();
                    entry.rxPackets = reader2.nextLong();
                    entry.txBytes = reader2.nextLong();
                    entry.txPackets = reader2.nextLong();
                    stats.addValues(entry);
                    reader2.finishLine();
                }
                IoUtils.closeQuietly(reader2);
                StrictMode.setThreadPolicy(savedPolicy);
                return stats;
            } catch (NullPointerException e2) {
                e = e2;
                reader = reader2;
                try {
                    throw new ProtocolException("problem parsing stats", e);
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(reader);
                    StrictMode.setThreadPolicy(savedPolicy);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                Object reader3 = reader2;
                IoUtils.closeQuietly(reader);
                StrictMode.setThreadPolicy(savedPolicy);
                throw th;
            }
        } catch (NullPointerException e3) {
            e = e3;
            throw new ProtocolException("problem parsing stats", e);
        }
    }

    public NetworkStats readNetworkStatsDetail() throws IOException {
        return readNetworkStatsDetail(-1, null, -1, null);
    }

    public NetworkStats readNetworkStatsDetail(int limitUid, String[] limitIfaces, int limitTag, NetworkStats lastStats) throws IOException {
        ArrayMap<String, String> arrayMap;
        NetworkStats stats = readNetworkStatsDetailInternal(limitUid, limitIfaces, limitTag, lastStats);
        synchronized (sStackedIfaces) {
            arrayMap = new ArrayMap(sStackedIfaces);
        }
        NetworkStats adjustments = new NetworkStats(0, arrayMap.size());
        Entry entry = null;
        for (int i = 0; i < stats.size(); i++) {
            entry = stats.getValues(i, entry);
            if (entry.iface != null && (entry.iface.startsWith(CLATD_INTERFACE_PREFIX) ^ 1) == 0) {
                String baseIface = (String) arrayMap.get(entry.iface);
                if (baseIface != null) {
                    Entry adjust = new Entry(baseIface, 0, 0, 0, 0, 0, 0, 0, 0);
                    adjust.rxBytes -= entry.rxBytes + (entry.rxPackets * 20);
                    adjust.txBytes -= entry.txBytes + (entry.txPackets * 20);
                    adjust.rxPackets -= entry.rxPackets;
                    adjust.txPackets -= entry.txPackets;
                    adjustments.combineValues(adjust);
                    entry.rxBytes = entry.rxPackets * 20;
                    entry.txBytes = entry.txPackets * 20;
                    entry.rxPackets = 0;
                    entry.txPackets = 0;
                    stats.combineValues(entry);
                }
            }
        }
        stats.combineAllValues(adjustments);
        return stats;
    }

    private NetworkStats readNetworkStatsDetailInternal(int limitUid, String[] limitIfaces, int limitTag, NetworkStats lastStats) throws IOException {
        NetworkStats stats;
        if (lastStats != null) {
            stats = lastStats;
            lastStats.setElapsedRealtime(SystemClock.elapsedRealtime());
        } else {
            stats = new NetworkStats(SystemClock.elapsedRealtime(), -1);
        }
        if (nativeReadNetworkStatsDetail(stats, this.mStatsXtUid.getAbsolutePath(), limitUid, limitIfaces, limitTag) == 0) {
            return stats;
        }
        throw new IOException("Failed to parse network stats");
    }

    public NetworkStats readNetworkStatsDetailWithPids() throws IOException {
        return readNetworkStatsDetailWithPids(-1);
    }

    public NetworkStats readNetworkStatsDetailWithPids(int limitUid) throws IOException {
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 0);
        stats.setContainExtendDataFlag(true);
        if (nativeReadNetworkStatsDetailWithPids(stats, this.mStatsXtUidWithPids.getAbsolutePath(), limitUid) == 0) {
            return stats;
        }
        throw new IOException("readNetworkStatsDetailWithPids:Failed to parse network stats with pid");
    }

    public static NetworkStats javaReadNetworkStatsDetailWithPids(File detailPath, int limitUid) throws IOException {
        return null;
    }

    public static NetworkStats javaReadNetworkStatsDetail(File detailPath, int limitUid, String[] limitIfaces, int limitTag) throws IOException {
        RuntimeException e;
        Throwable th;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
        Entry entry = new Entry();
        int idx = 1;
        int lastIdx = 1;
        AutoCloseable reader = null;
        try {
            ProcFileReader reader2 = new ProcFileReader(new FileInputStream(detailPath));
            try {
                reader2.finishLine();
                while (reader2.hasMoreData()) {
                    idx = reader2.nextInt();
                    if (idx != lastIdx + 1) {
                        throw new ProtocolException("inconsistent idx=" + idx + " after lastIdx=" + lastIdx);
                    }
                    lastIdx = idx;
                    entry.iface = reader2.nextString();
                    entry.tag = NetworkManagementSocketTagger.kernelToTag(reader2.nextString());
                    entry.uid = reader2.nextInt();
                    entry.set = reader2.nextInt();
                    entry.rxBytes = reader2.nextLong();
                    entry.rxPackets = reader2.nextLong();
                    entry.txBytes = reader2.nextLong();
                    entry.txPackets = reader2.nextLong();
                    if ((limitIfaces == null || ArrayUtils.contains((Object[]) limitIfaces, entry.iface)) && ((limitUid == -1 || limitUid == entry.uid) && (limitTag == -1 || limitTag == entry.tag))) {
                        stats.addValues(entry);
                    }
                    reader2.finishLine();
                }
                IoUtils.closeQuietly(reader2);
                StrictMode.setThreadPolicy(savedPolicy);
                return stats;
            } catch (NullPointerException e2) {
                e = e2;
                reader = reader2;
                try {
                    throw new ProtocolException("problem parsing idx " + idx, e);
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(reader);
                    StrictMode.setThreadPolicy(savedPolicy);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                Object reader3 = reader2;
                IoUtils.closeQuietly(reader);
                StrictMode.setThreadPolicy(savedPolicy);
                throw th;
            }
        } catch (NullPointerException e3) {
            e = e3;
            throw new ProtocolException("problem parsing idx " + idx, e);
        }
    }

    public void assertEquals(NetworkStats expected, NetworkStats actual) {
        if (expected.size() != actual.size()) {
            throw new AssertionError("Expected size " + expected.size() + ", actual size " + actual.size());
        }
        Entry expectedRow = null;
        Entry actualRow = null;
        int i = 0;
        while (i < expected.size()) {
            expectedRow = expected.getValues(i, expectedRow);
            actualRow = actual.getValues(i, actualRow);
            if (expectedRow.equals(actualRow)) {
                i++;
            } else {
                throw new AssertionError("Expected row " + i + ": " + expectedRow + ", actual row " + actualRow);
            }
        }
    }
}
