package com.android.server.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetdEventCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.metrics.ConnectStats;
import android.net.metrics.DnsEvent;
import android.net.metrics.INetdEventListener.Stub;
import android.net.metrics.WakeupEvent;
import android.net.metrics.WakeupStats;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.BitUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.TokenBucket;
import com.android.server.connectivity.metrics.nano.IpConnectivityLogClass.IpConnectivityEvent;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Function;

public class NetdEventListenerService extends Stub {
    private static final int CONNECT_LATENCY_BURST_LIMIT = 5000;
    private static final int CONNECT_LATENCY_FILL_RATE = 15000;
    private static final int CONNECT_LATENCY_MAXIMUM_RECORDS = 20000;
    private static final boolean DBG = false;
    private static final int INITIAL_DNS_BATCH_SIZE = 100;
    public static final String SERVICE_NAME = "netd_listener";
    private static final String TAG = NetdEventListenerService.class.getSimpleName();
    private static final boolean VDBG = false;
    static final int WAKEUP_EVENT_BUFFER_LENGTH = 1024;
    static final String WAKEUP_EVENT_IFACE_PREFIX = "iface:";
    private final ConnectivityManager mCm;
    @GuardedBy("this")
    private final SparseArray<ConnectStats> mConnectEvents;
    @GuardedBy("this")
    private final TokenBucket mConnectTb;
    @GuardedBy("this")
    private final SparseArray<DnsEvent> mDnsEvents;
    @GuardedBy("this")
    private INetdEventCallback mNetdEventCallback;
    @GuardedBy("this")
    private long mWakeupEventCursor;
    @GuardedBy("this")
    private final WakeupEvent[] mWakeupEvents;
    @GuardedBy("this")
    private final ArrayMap<String, WakeupStats> mWakeupStats;

    public synchronized boolean registerNetdEventCallback(INetdEventCallback callback) {
        this.mNetdEventCallback = callback;
        return true;
    }

    public synchronized boolean unregisterNetdEventCallback() {
        this.mNetdEventCallback = null;
        return true;
    }

    public NetdEventListenerService(Context context) {
        this((ConnectivityManager) context.getSystemService(ConnectivityManager.class));
    }

    public NetdEventListenerService(ConnectivityManager cm) {
        this.mDnsEvents = new SparseArray();
        this.mConnectEvents = new SparseArray();
        this.mWakeupStats = new ArrayMap();
        this.mWakeupEvents = new WakeupEvent[1024];
        this.mWakeupEventCursor = 0;
        this.mConnectTb = new TokenBucket(15000, 5000);
        this.mCm = cm;
    }

    public synchronized void onDnsEvent(int netId, int eventType, int returnCode, int latencyMs, String hostname, String[] ipAddresses, int ipAddressesCount, int uid) throws RemoteException {
        maybeVerboseLog("onDnsEvent(%d, %d, %d, %dms)", Integer.valueOf(netId), Integer.valueOf(eventType), Integer.valueOf(returnCode), Integer.valueOf(latencyMs));
        DnsEvent dnsEvent = (DnsEvent) this.mDnsEvents.get(netId);
        if (dnsEvent == null) {
            dnsEvent = makeDnsEvent(netId);
            this.mDnsEvents.put(netId, dnsEvent);
        }
        dnsEvent.addResult((byte) eventType, (byte) returnCode, latencyMs);
        if (this.mNetdEventCallback != null) {
            this.mNetdEventCallback.onDnsEvent(hostname, ipAddresses, ipAddressesCount, System.currentTimeMillis(), uid);
        }
    }

    public synchronized void onConnectEvent(int netId, int error, int latencyMs, String ipAddr, int port, int uid) throws RemoteException {
        maybeVerboseLog("onConnectEvent(%d, %d, %dms)", Integer.valueOf(netId), Integer.valueOf(error), Integer.valueOf(latencyMs));
        ConnectStats connectStats = (ConnectStats) this.mConnectEvents.get(netId);
        if (connectStats == null) {
            connectStats = makeConnectStats(netId);
            this.mConnectEvents.put(netId, connectStats);
        }
        connectStats.addEvent(error, latencyMs, ipAddr);
        if (this.mNetdEventCallback != null) {
            this.mNetdEventCallback.onConnectEvent(ipAddr, port, System.currentTimeMillis(), uid);
        }
    }

    public synchronized void onWakeupEvent(String prefix, int uid, int gid, long timestampNs) {
        long timestampMs;
        maybeVerboseLog("onWakeupEvent(%s, %d, %d, %sns)", prefix, Integer.valueOf(uid), Integer.valueOf(gid), Long.valueOf(timestampNs));
        String iface = prefix.replaceFirst(WAKEUP_EVENT_IFACE_PREFIX, "");
        if (timestampNs > 0) {
            timestampMs = timestampNs / 1000000;
        } else {
            timestampMs = System.currentTimeMillis();
        }
        addWakeupEvent(iface, timestampMs, uid);
    }

    @GuardedBy("this")
    private void addWakeupEvent(String iface, long timestampMs, int uid) {
        int index = wakeupEventIndex(this.mWakeupEventCursor);
        this.mWakeupEventCursor++;
        WakeupEvent event = new WakeupEvent();
        event.iface = iface;
        event.timestampMs = timestampMs;
        event.uid = uid;
        this.mWakeupEvents[index] = event;
        WakeupStats stats = (WakeupStats) this.mWakeupStats.get(iface);
        if (stats == null) {
            stats = new WakeupStats(iface);
            this.mWakeupStats.put(iface, stats);
        }
        stats.countEvent(event);
    }

    @GuardedBy("this")
    private WakeupEvent[] getWakeupEvents() {
        WakeupEvent[] out = new WakeupEvent[((int) Math.min(this.mWakeupEventCursor, (long) this.mWakeupEvents.length))];
        int outIdx = out.length - 1;
        long inCursor = this.mWakeupEventCursor - 1;
        while (outIdx >= 0) {
            int outIdx2 = outIdx - 1;
            long inCursor2 = inCursor - 1;
            out[outIdx] = this.mWakeupEvents[wakeupEventIndex(inCursor)];
            outIdx = outIdx2;
            inCursor = inCursor2;
        }
        return out;
    }

    private static int wakeupEventIndex(long cursor) {
        return (int) Math.abs(cursor % 1024);
    }

    public synchronized void flushStatistics(List<IpConnectivityEvent> events) {
        flushProtos(events, this.mConnectEvents, -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY.$INST$0);
        flushProtos(events, this.mDnsEvents, -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY.$INST$1);
        for (int i = 0; i < this.mWakeupStats.size(); i++) {
            events.add(IpConnectivityEventBuilder.toProto((WakeupStats) this.mWakeupStats.valueAt(i)));
        }
        this.mWakeupStats.clear();
    }

    public synchronized void dump(PrintWriter writer) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println(TAG + ":");
        pw.increaseIndent();
        list(pw);
        pw.decreaseIndent();
    }

    static /* synthetic */ Object lambda$-com_android_server_connectivity_NetdEventListenerService_8935(ConnectStats x) {
        return x;
    }

    public synchronized void list(PrintWriter pw) {
        listEvents(pw, this.mConnectEvents, -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY.$INST$2, "\n");
        listEvents(pw, this.mDnsEvents, -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY.$INST$3, "\n");
        for (int i = 0; i < this.mWakeupStats.size(); i++) {
            pw.println(this.mWakeupStats.valueAt(i));
        }
        for (WakeupEvent wakeup : getWakeupEvents()) {
            pw.println(wakeup);
        }
    }

    static /* synthetic */ Object lambda$-com_android_server_connectivity_NetdEventListenerService_8987(DnsEvent x) {
        return x;
    }

    public synchronized void listAsProtos(PrintWriter pw) {
        listEvents(pw, this.mConnectEvents, -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY.$INST$4, "");
        listEvents(pw, this.mDnsEvents, -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY.$INST$5, "");
        for (int i = 0; i < this.mWakeupStats.size(); i++) {
            pw.print(IpConnectivityEventBuilder.toProto((WakeupStats) this.mWakeupStats.valueAt(i)));
        }
    }

    private static <T> void flushProtos(List<IpConnectivityEvent> out, SparseArray<T> in, Function<T, IpConnectivityEvent> mapper) {
        for (int i = 0; i < in.size(); i++) {
            out.add((IpConnectivityEvent) mapper.apply(in.valueAt(i)));
        }
        in.clear();
    }

    private static <T> void listEvents(PrintWriter pw, SparseArray<T> events, Function<T, Object> mapper, String separator) {
        for (int i = 0; i < events.size(); i++) {
            pw.print(mapper.apply(events.valueAt(i)));
            pw.print(separator);
        }
    }

    private ConnectStats makeConnectStats(int netId) {
        return new ConnectStats(netId, getTransports(netId), this.mConnectTb, CONNECT_LATENCY_MAXIMUM_RECORDS);
    }

    private DnsEvent makeDnsEvent(int netId) {
        return new DnsEvent(netId, getTransports(netId), 100);
    }

    private long getTransports(int netId) {
        NetworkCapabilities nc = this.mCm.getNetworkCapabilities(new Network(netId));
        if (nc == null) {
            return 0;
        }
        return BitUtils.packBits(nc.getTransportTypes());
    }

    private static void maybeLog(String s, Object... args) {
    }

    private static void maybeVerboseLog(String s, Object... args) {
    }
}
