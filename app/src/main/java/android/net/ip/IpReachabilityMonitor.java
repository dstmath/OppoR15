package android.net.ip;

import android.content.Context;
import android.net.LinkProperties;
import android.net.LinkProperties.ProvisioningChange;
import android.net.RouteInfo;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.IpReachabilityEvent;
import android.net.netlink.NetlinkConstants;
import android.net.netlink.NetlinkErrorMessage;
import android.net.netlink.NetlinkMessage;
import android.net.netlink.NetlinkSocket;
import android.net.netlink.RtNetlinkNeighborMessage;
import android.net.netlink.StructNdMsg;
import android.net.util.MultinetworkPolicyTracker;
import android.net.util.SharedLog;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.NetlinkSocketAddress;
import android.system.OsConstants;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.io.InterruptedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class IpReachabilityMonitor {
    private static final boolean DBG = false;
    private static final String TAG = "IpReachabilityMonitor";
    private static final boolean VDBG = false;
    private static short sArpNudState = (short) 0;
    private final Callback mCallback;
    private final int mInterfaceIndex;
    private final String mInterfaceName;
    @GuardedBy("mLock")
    private Map<InetAddress, Short> mIpWatchList;
    @GuardedBy("mLock")
    private int mIpWatchListVersion;
    private volatile long mLastProbeTimeMs;
    @GuardedBy("mLock")
    private LinkProperties mLinkProperties;
    private final Object mLock;
    private final SharedLog mLog;
    private final IpConnectivityLog mMetricsLog;
    private final MultinetworkPolicyTracker mMultinetworkPolicyTracker;
    private final NetlinkSocketObserver mNetlinkSocketObserver;
    private final Thread mObserverThread;
    private volatile boolean mRunning;
    @GuardedBy("mLock")
    private boolean mStopped;
    private final WakeLock mWakeLock;

    public interface Callback {
        void notifyLost(InetAddress inetAddress, String str);
    }

    private final class NetlinkSocketObserver implements Runnable {
        private NetlinkSocket mSocket;

        /* synthetic */ NetlinkSocketObserver(IpReachabilityMonitor this$0, NetlinkSocketObserver -this1) {
            this();
        }

        private NetlinkSocketObserver() {
        }

        public void run() {
            IpReachabilityMonitor.this.mRunning = true;
            try {
                setupNetlinkSocket();
            } catch (Exception e) {
                Log.e(IpReachabilityMonitor.TAG, "Failed to suitably initialize a netlink socket", e);
                IpReachabilityMonitor.this.mRunning = false;
            }
            while (IpReachabilityMonitor.this.mRunning && (IpReachabilityMonitor.this.isMonitorStopped() ^ 1) != 0) {
                try {
                    ByteBuffer byteBuffer = recvKernelReply();
                    long whenMs = SystemClock.elapsedRealtime();
                    if (byteBuffer != null) {
                        parseNetlinkMessageBuffer(byteBuffer, whenMs);
                    }
                } catch (ErrnoException e2) {
                    if (IpReachabilityMonitor.this.mRunning) {
                        Log.w(IpReachabilityMonitor.TAG, "ErrnoException: ", e2);
                    }
                }
            }
            clearNetlinkSocket();
            IpReachabilityMonitor.this.mRunning = false;
        }

        private void clearNetlinkSocket() {
            if (this.mSocket != null) {
                this.mSocket.close();
            }
        }

        private void setupNetlinkSocket() throws ErrnoException, SocketException {
            clearNetlinkSocket();
            this.mSocket = new NetlinkSocket(OsConstants.NETLINK_ROUTE);
            this.mSocket.bind(new NetlinkSocketAddress(0, OsConstants.RTMGRP_NEIGH));
        }

        private ByteBuffer recvKernelReply() throws ErrnoException {
            try {
                return this.mSocket.recvMessage(0);
            } catch (InterruptedIOException e) {
            } catch (ErrnoException e2) {
                if (e2.errno != OsConstants.EAGAIN) {
                    throw e2;
                }
            }
            return null;
        }

        private void parseNetlinkMessageBuffer(ByteBuffer byteBuffer, long whenMs) {
            while (byteBuffer.remaining() > 0) {
                int position = byteBuffer.position();
                NetlinkMessage nlMsg = NetlinkMessage.parse(byteBuffer);
                if (nlMsg == null || nlMsg.getHeader() == null) {
                    byteBuffer.position(position);
                    Log.e(IpReachabilityMonitor.TAG, "unparsable netlink msg: " + NetlinkConstants.hexify(byteBuffer));
                    return;
                }
                int srcPortId = nlMsg.getHeader().nlmsg_pid;
                if (srcPortId != 0) {
                    Log.e(IpReachabilityMonitor.TAG, "non-kernel source portId: " + ((long) (srcPortId & -1)));
                    return;
                } else if (nlMsg instanceof NetlinkErrorMessage) {
                    Log.e(IpReachabilityMonitor.TAG, "netlink error: " + nlMsg);
                } else if (nlMsg instanceof RtNetlinkNeighborMessage) {
                    evaluateRtNetlinkNeighborMessage((RtNetlinkNeighborMessage) nlMsg, whenMs);
                }
            }
        }

        private void evaluateRtNetlinkNeighborMessage(RtNetlinkNeighborMessage neighMsg, long whenMs) {
            StructNdMsg ndMsg = neighMsg.getNdHeader();
            if (ndMsg != null && ndMsg.ndm_ifindex == IpReachabilityMonitor.this.mInterfaceIndex) {
                InetAddress destination = neighMsg.getDestination();
                if (IpReachabilityMonitor.this.isWatching(destination)) {
                    short msgType = neighMsg.getHeader().nlmsg_type;
                    short nudState = ndMsg.ndm_state;
                    String eventMsg = "NeighborEvent{elapsedMs=" + whenMs + ", " + destination.getHostAddress() + ", " + "[" + NetlinkConstants.hexify(neighMsg.getLinkLayerAddress()) + "], " + NetlinkConstants.stringForNlMsgType(msgType) + ", " + StructNdMsg.stringForNudState(nudState) + "}";
                    synchronized (IpReachabilityMonitor.this.mLock) {
                        if (IpReachabilityMonitor.this.mIpWatchList.containsKey(destination)) {
                            short value;
                            if (msgType == (short) 29) {
                                value = (short) 0;
                            } else {
                                value = nudState;
                            }
                            IpReachabilityMonitor.this.mIpWatchList.put(destination, Short.valueOf(value));
                        }
                    }
                    if (nudState == (short) 32) {
                        Log.w(IpReachabilityMonitor.TAG, "ALERT: " + eventMsg);
                        IpReachabilityMonitor.this.handleNeighborLost(eventMsg);
                    }
                    synchronized (IpReachabilityMonitor.this.mLock) {
                        if (IpReachabilityMonitor.this.mIpWatchList.containsKey(destination) && (destination instanceof Inet4Address)) {
                            IpReachabilityMonitor.sArpNudState = nudState;
                        }
                    }
                    if (IpReachabilityMonitor.this.mWakeLock.isHeld() && (nudState == (short) 32 || nudState == StructNdMsg.NUD_PERMANENT || nudState == (short) 2)) {
                        IpReachabilityMonitor.this.mWakeLock.release();
                    }
                }
            }
        }
    }

    private static int probeNeighbor(int ifIndex, InetAddress ip) {
        String msgSnippet = "probing ip=" + ip.getHostAddress() + "%" + ifIndex;
        byte[] msg = RtNetlinkNeighborMessage.newNewNeighborMessage(1, ip, (short) 16, ifIndex, null);
        if (sArpNudState == StructNdMsg.NUD_PERMANENT && (ip instanceof Inet4Address)) {
            return 0;
        }
        try {
            NetlinkSocket.sendOneShotKernelMessage(OsConstants.NETLINK_ROUTE, msg);
            return 0;
        } catch (ErrnoException e) {
            Log.e(TAG, "Error " + msgSnippet + ": " + e);
            return -e.errno;
        }
    }

    public IpReachabilityMonitor(Context context, String ifName, SharedLog log, Callback callback) {
        this(context, ifName, log, callback, null);
    }

    public IpReachabilityMonitor(Context context, String ifName, SharedLog log, Callback callback, MultinetworkPolicyTracker tracker) throws IllegalArgumentException {
        this.mLock = new Object();
        this.mMetricsLog = new IpConnectivityLog();
        this.mLinkProperties = new LinkProperties();
        this.mIpWatchList = new HashMap();
        this.mStopped = false;
        this.mInterfaceName = ifName;
        try {
            this.mInterfaceIndex = NetworkInterface.getByName(ifName).getIndex();
            this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "IpReachabilityMonitor." + this.mInterfaceName);
            this.mLog = log.forSubComponent(TAG);
            this.mCallback = callback;
            this.mMultinetworkPolicyTracker = tracker;
            this.mNetlinkSocketObserver = new NetlinkSocketObserver();
            this.mObserverThread = new Thread(this.mNetlinkSocketObserver);
            this.mObserverThread.start();
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid interface '" + ifName + "': ", e);
        }
    }

    public void stop() {
        this.mRunning = false;
        clearLinkProperties();
        this.mNetlinkSocketObserver.clearNetlinkSocket();
        synchronized (this.mLock) {
            this.mStopped = true;
        }
    }

    private String describeWatchList() {
        String delimiter = ", ";
        StringBuilder sb = new StringBuilder();
        synchronized (this.mLock) {
            sb.append("iface{").append(this.mInterfaceName).append("/").append(this.mInterfaceIndex).append("}, ");
            sb.append("v{").append(this.mIpWatchListVersion).append("}, ");
            sb.append("ntable=[");
            boolean firstTime = true;
            for (Entry<InetAddress, Short> entry : this.mIpWatchList.entrySet()) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    sb.append(", ");
                }
                sb.append(((InetAddress) entry.getKey()).getHostAddress()).append("/").append(StructNdMsg.stringForNudState(((Short) entry.getValue()).shortValue()));
            }
            sb.append("]");
        }
        return sb.toString();
    }

    private boolean isWatching(InetAddress ip) {
        boolean containsKey;
        synchronized (this.mLock) {
            containsKey = this.mRunning ? this.mIpWatchList.containsKey(ip) : false;
        }
        return containsKey;
    }

    private boolean isMonitorStopped() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mStopped;
        }
        return z;
    }

    private static boolean isOnLink(List<RouteInfo> routes, InetAddress ip) {
        for (RouteInfo route : routes) {
            if (!route.hasGateway() && route.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    private short getNeighborStateLocked(InetAddress ip) {
        if (this.mIpWatchList.containsKey(ip)) {
            return ((Short) this.mIpWatchList.get(ip)).shortValue();
        }
        return (short) 0;
    }

    public void updateLinkProperties(LinkProperties lp) {
        if (this.mInterfaceName.equals(lp.getInterfaceName())) {
            synchronized (this.mLock) {
                this.mLinkProperties = new LinkProperties(lp);
                Map<InetAddress, Short> newIpWatchList = new HashMap();
                List<RouteInfo> routes = this.mLinkProperties.getRoutes();
                for (RouteInfo route : routes) {
                    if (route.hasGateway()) {
                        InetAddress gw = route.getGateway();
                        if (isOnLink(routes, gw)) {
                            newIpWatchList.put(gw, Short.valueOf(getNeighborStateLocked(gw)));
                        }
                    }
                }
                for (InetAddress nameserver : lp.getDnsServers()) {
                    if (isOnLink(routes, nameserver)) {
                        newIpWatchList.put(nameserver, Short.valueOf(getNeighborStateLocked(nameserver)));
                    }
                }
                this.mIpWatchList = newIpWatchList;
                this.mIpWatchListVersion++;
            }
            return;
        }
        Log.wtf(TAG, "requested LinkProperties interface '" + lp.getInterfaceName() + "' does not match: " + this.mInterfaceName);
    }

    public void clearLinkProperties() {
        synchronized (this.mLock) {
            this.mLinkProperties.clear();
            this.mIpWatchList.clear();
            this.mIpWatchListVersion++;
        }
    }

    private void handleNeighborLost(String msg) {
        ProvisioningChange delta;
        InetAddress ip = null;
        synchronized (this.mLock) {
            LinkProperties whatIfLp = new LinkProperties(this.mLinkProperties);
            for (Entry<InetAddress, Short> entry : this.mIpWatchList.entrySet()) {
                if (((Short) entry.getValue()).shortValue() == (short) 32) {
                    ip = (InetAddress) entry.getKey();
                    for (RouteInfo route : this.mLinkProperties.getRoutes()) {
                        if (ip.equals(route.getGateway())) {
                            whatIfLp.removeRoute(route);
                        }
                    }
                    if (avoidingBadLinks() || ((ip instanceof Inet6Address) ^ 1) != 0) {
                        whatIfLp.removeDnsServer(ip);
                    }
                }
            }
            delta = LinkProperties.compareProvisioning(this.mLinkProperties, whatIfLp);
        }
        if (delta == ProvisioningChange.LOST_PROVISIONING) {
            String logMsg = "FAILURE: LOST_PROVISIONING, " + msg;
            Log.w(TAG, logMsg);
            if (this.mCallback != null) {
                this.mCallback.notifyLost(ip, logMsg);
            }
        }
        logNudFailed(delta);
    }

    private boolean avoidingBadLinks() {
        return this.mMultinetworkPolicyTracker != null ? this.mMultinetworkPolicyTracker.getAvoidBadWifi() : true;
    }

    public void probeAll() {
        List<InetAddress> ipProbeList;
        synchronized (this.mLock) {
            ipProbeList = new ArrayList(this.mIpWatchList.keySet());
        }
        if (!ipProbeList.isEmpty() && this.mRunning) {
            this.mWakeLock.acquire(getProbeWakeLockDuration());
        }
        for (InetAddress target : ipProbeList) {
            if (!this.mRunning) {
                break;
            }
            int returnValue = probeNeighbor(this.mInterfaceIndex, target);
            this.mLog.log(String.format("put neighbor %s into NUD_PROBE state (rval=%d)", new Object[]{target.getHostAddress(), Integer.valueOf(returnValue)}));
            logEvent(256, returnValue);
        }
        this.mLastProbeTimeMs = SystemClock.elapsedRealtime();
    }

    private static long getProbeWakeLockDuration() {
        return 6500;
    }

    private void logEvent(int probeType, int errorCode) {
        this.mMetricsLog.log(this.mInterfaceName, new IpReachabilityEvent(probeType | (errorCode & 255)));
    }

    private void logNudFailed(ProvisioningChange delta) {
        this.mMetricsLog.log(this.mInterfaceName, new IpReachabilityEvent(IpReachabilityEvent.nudFailureEventType(SystemClock.elapsedRealtime() - this.mLastProbeTimeMs < getProbeWakeLockDuration(), delta == ProvisioningChange.LOST_PROVISIONING)));
    }
}
