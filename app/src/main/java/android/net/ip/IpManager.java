package android.net.ip;

import android.content.Context;
import android.net.DhcpResults;
import android.net.INetd;
import android.net.InterfaceConfiguration;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.LinkProperties.ProvisioningChange;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.apf.ApfCapabilities;
import android.net.apf.ApfFilter;
import android.net.dhcp.DhcpClient;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.IpManagerEvent;
import android.net.util.MultinetworkPolicyTracker;
import android.net.util.NetdService;
import android.net.util.SharedLog;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.IState;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.net.NetlinkTracker;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import oppo.util.OppoStatistics;

public class IpManager extends StateMachine {
    private static final /* synthetic */ int[] -android-net-LinkProperties$ProvisioningChangeSwitchesValues = null;
    private static final String CLAT_PREFIX = "v4-";
    private static final int CMD_CONFIRM = 3;
    private static final int CMD_SET_MULTICAST_FILTER = 8;
    private static final int CMD_START = 2;
    private static final int CMD_STOP = 1;
    private static final int CMD_UPDATE_HTTP_PROXY = 7;
    private static final int CMD_UPDATE_TCP_BUFFER_SIZES = 6;
    private static final boolean DBG = false;
    public static final String DUMP_ARG = "ipmanager";
    public static final String DUMP_ARG_CONFIRM = "confirm";
    private static final int EVENT_DHCPACTION_TIMEOUT = 10;
    private static final int EVENT_NETLINK_LINKPROPERTIES_CHANGED = 5;
    private static final int EVENT_PRE_DHCP_ACTION_COMPLETE = 4;
    private static final int EVENT_PROVISIONING_TIMEOUT = 9;
    private static final int MAX_LOG_RECORDS = 500;
    private static final int MAX_PACKET_RECORDS = 100;
    private static final boolean NO_CALLBACKS = false;
    private static final boolean SEND_CALLBACKS = true;
    private static final boolean VDBG = false;
    private static final String WIFI_DIFFERENT_IP = "wifi_different_ip";
    private static final String WIFI_STATISTIC_KEY = "wifi_fool_proof";
    private static final Class[] sMessageClasses = new Class[]{IpManager.class, DhcpClient.class};
    private static final SparseArray<String> sWhatToString = MessageUtils.findMessageNames(sMessageClasses);
    private ApfFilter mApfFilter;
    protected final Callback mCallback;
    private final String mClatInterfaceName;
    private ProvisioningConfiguration mConfiguration;
    private final LocalLog mConnectivityPacketLog;
    private final Context mContext;
    private final WakeupMessage mDhcpActionTimeoutAlarm;
    private DhcpClient mDhcpClient;
    private DhcpResults mDhcpResults;
    private ProxyInfo mHttpProxy;
    private final String mInterfaceName;
    private IpReachabilityMonitor mIpReachabilityMonitor;
    private LinkProperties mLinkProperties;
    private final SharedLog mLog;
    private final IpConnectivityLog mMetricsLog;
    private final MessageHandlingLogger mMsgStateLogger;
    private boolean mMulticastFiltering;
    private final MultinetworkPolicyTracker mMultinetworkPolicyTracker;
    private final INetd mNetd;
    private final NetlinkTracker mNetlinkTracker;
    private NetworkInterface mNetworkInterface;
    private final INetworkManagementService mNwService;
    private final WakeupMessage mProvisioningTimeoutAlarm;
    private final State mRunningState;
    private long mStartTimeMillis;
    private final State mStartedState;
    private final State mStoppedState;
    private final State mStoppingState;
    private final String mTag;
    private String mTcpBufferSizes;

    public static class Callback {
        public void onPreDhcpAction() {
        }

        public void onPostDhcpAction() {
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
        }

        public void onProvisioningFailure(LinkProperties newLp) {
        }

        public void onLinkPropertiesChange(LinkProperties newLp) {
        }

        public void onReachabilityLost(String logMsg) {
        }

        public void onQuit() {
        }

        public void installPacketFilter(byte[] filter) {
        }

        public void setFallbackMulticastFilter(boolean enabled) {
        }

        public void setNeighborDiscoveryOffload(boolean enable) {
        }
    }

    public static class InitialConfiguration {
        public final Set<IpPrefix> directlyConnectedRoutes = new HashSet();
        public final Set<InetAddress> dnsServers = new HashSet();
        public Inet4Address gateway;
        public final Set<LinkAddress> ipAddresses = new HashSet();

        public static InitialConfiguration copy(InitialConfiguration config) {
            if (config == null) {
                return null;
            }
            InitialConfiguration configCopy = new InitialConfiguration();
            configCopy.ipAddresses.addAll(config.ipAddresses);
            configCopy.directlyConnectedRoutes.addAll(config.directlyConnectedRoutes);
            configCopy.dnsServers.addAll(config.dnsServers);
            return configCopy;
        }

        public String toString() {
            return String.format("InitialConfiguration(IPs: {%s}, prefixes: {%s}, DNS: {%s}, v4 gateway: %s)", new Object[]{IpManager.join(", ", this.ipAddresses), IpManager.join(", ", this.directlyConnectedRoutes), IpManager.join(", ", this.dnsServers), this.gateway});
        }

        public boolean isValid() {
            if (this.ipAddresses.isEmpty()) {
                return false;
            }
            for (LinkAddress addr : this.ipAddresses) {
                if (!IpManager.any(this.directlyConnectedRoutes, new android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass3((byte) 2, addr))) {
                    return false;
                }
            }
            for (InetAddress addr2 : this.dnsServers) {
                if (!IpManager.any(this.directlyConnectedRoutes, new android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass3((byte) 3, addr2))) {
                    return false;
                }
            }
            if (IpManager.any(this.ipAddresses, IpManager.not(android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass1.$INST$0))) {
                return false;
            }
            if ((IpManager.any(this.directlyConnectedRoutes, android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass1.$INST$1) && IpManager.all(this.ipAddresses, IpManager.not(android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass1.$INST$2))) || IpManager.any(this.directlyConnectedRoutes, IpManager.not(android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass1.$INST$3))) {
                return false;
            }
            Stream stream = this.ipAddresses.stream();
            Class cls = Inet4Address.class;
            cls.getClass();
            if (stream.filter(new android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass3((byte) 4, cls)).count() > 1) {
                return false;
            }
            return true;
        }

        public boolean isProvisionedBy(List<LinkAddress> addresses, List<RouteInfo> routes) {
            if (this.ipAddresses.isEmpty()) {
                return false;
            }
            for (LinkAddress addr : this.ipAddresses) {
                if (!IpManager.any(addresses, new android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass3((byte) 0, addr))) {
                    return false;
                }
            }
            if (routes != null) {
                for (IpPrefix prefix : this.directlyConnectedRoutes) {
                    if (!IpManager.any(routes, new android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass3((byte) 1, prefix))) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static boolean isDirectlyConnectedRoute(RouteInfo route, IpPrefix prefix) {
            return !route.hasGateway() ? prefix.equals(route.getDestination()) : false;
        }

        private static boolean isPrefixLengthCompliant(LinkAddress addr) {
            return !addr.isIPv4() ? isCompliantIPv6PrefixLength(addr.getPrefixLength()) : true;
        }

        private static boolean isPrefixLengthCompliant(IpPrefix prefix) {
            return !prefix.isIPv4() ? isCompliantIPv6PrefixLength(prefix.getPrefixLength()) : true;
        }

        private static boolean isCompliantIPv6PrefixLength(int prefixLength) {
            if (48 > prefixLength || prefixLength > 64) {
                return false;
            }
            return true;
        }

        private static boolean isIPv6DefaultRoute(IpPrefix prefix) {
            return prefix.getAddress().equals(Inet6Address.ANY);
        }

        private static boolean isIPv6GUA(LinkAddress addr) {
            return addr.isIPv6() ? addr.isGlobalPreferred() : false;
        }
    }

    private class LoggingCallbackWrapper extends Callback {
        private static final String PREFIX = "INVOKE ";
        private Callback mCallback;

        public LoggingCallbackWrapper(Callback callback) {
            this.mCallback = callback;
        }

        private void log(String msg) {
            IpManager.this.mLog.log(PREFIX + msg);
        }

        public void onPreDhcpAction() {
            this.mCallback.onPreDhcpAction();
            log("onPreDhcpAction()");
        }

        public void onPostDhcpAction() {
            this.mCallback.onPostDhcpAction();
            log("onPostDhcpAction()");
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
            this.mCallback.onNewDhcpResults(dhcpResults);
            log("onNewDhcpResults({" + dhcpResults + "})");
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
            this.mCallback.onProvisioningSuccess(newLp);
            log("onProvisioningSuccess({" + newLp + "})");
        }

        public void onProvisioningFailure(LinkProperties newLp) {
            this.mCallback.onProvisioningFailure(newLp);
            log("onProvisioningFailure({" + newLp + "})");
        }

        public void onLinkPropertiesChange(LinkProperties newLp) {
            this.mCallback.onLinkPropertiesChange(newLp);
            log("onLinkPropertiesChange({" + newLp + "})");
        }

        public void onReachabilityLost(String logMsg) {
            this.mCallback.onReachabilityLost(logMsg);
            log("onReachabilityLost(" + logMsg + ")");
        }

        public void onQuit() {
            this.mCallback.onQuit();
            log("onQuit()");
        }

        public void installPacketFilter(byte[] filter) {
            this.mCallback.installPacketFilter(filter);
            log("installPacketFilter(byte[" + filter.length + "])");
        }

        public void setFallbackMulticastFilter(boolean enabled) {
            this.mCallback.setFallbackMulticastFilter(enabled);
            log("setFallbackMulticastFilter(" + enabled + ")");
        }

        public void setNeighborDiscoveryOffload(boolean enable) {
            this.mCallback.setNeighborDiscoveryOffload(enable);
            log("setNeighborDiscoveryOffload(" + enable + ")");
        }
    }

    private static class MessageHandlingLogger {
        public String processedInState;
        public String receivedInState;

        /* synthetic */ MessageHandlingLogger(MessageHandlingLogger -this0) {
            this();
        }

        private MessageHandlingLogger() {
        }

        public void reset() {
            this.processedInState = null;
            this.receivedInState = null;
        }

        public void handled(State processedIn, IState receivedIn) {
            this.processedInState = processedIn.getClass().getSimpleName();
            this.receivedInState = receivedIn.getName();
        }

        public String toString() {
            return String.format("rcvd_in=%s, proc_in=%s", new Object[]{this.receivedInState, this.processedInState});
        }
    }

    public static class ProvisioningConfiguration {
        private static final int DEFAULT_TIMEOUT_MS = 36000;
        ApfCapabilities mApfCapabilities;
        public boolean mDiscoverSent;
        boolean mEnableIPv4 = true;
        boolean mEnableIPv6 = true;
        int mIPv6AddrGenMode = 2;
        InitialConfiguration mInitialConfig;
        int mProvisioningTimeoutMs = DEFAULT_TIMEOUT_MS;
        public boolean mRapidCommit;
        int mRequestedPreDhcpActionMs;
        StaticIpConfiguration mStaticIpConfig;
        boolean mUsingIpReachabilityMonitor = true;

        public static class Builder {
            private ProvisioningConfiguration mConfig = new ProvisioningConfiguration();

            public Builder withoutIPv4() {
                this.mConfig.mEnableIPv4 = false;
                return this;
            }

            public Builder withoutIPv6() {
                this.mConfig.mEnableIPv6 = false;
                return this;
            }

            public Builder withoutIpReachabilityMonitor() {
                this.mConfig.mUsingIpReachabilityMonitor = false;
                return this;
            }

            public Builder withPreDhcpAction() {
                this.mConfig.mRequestedPreDhcpActionMs = ProvisioningConfiguration.DEFAULT_TIMEOUT_MS;
                return this;
            }

            public Builder withPreDhcpAction(int dhcpActionTimeoutMs) {
                this.mConfig.mRequestedPreDhcpActionMs = dhcpActionTimeoutMs;
                return this;
            }

            public Builder withInitialConfiguration(InitialConfiguration initialConfig) {
                this.mConfig.mInitialConfig = initialConfig;
                return this;
            }

            public Builder withStaticConfiguration(StaticIpConfiguration staticConfig) {
                this.mConfig.mStaticIpConfig = staticConfig;
                return this;
            }

            public Builder withApfCapabilities(ApfCapabilities apfCapabilities) {
                this.mConfig.mApfCapabilities = apfCapabilities;
                return this;
            }

            public Builder withProvisioningTimeoutMs(int timeoutMs) {
                this.mConfig.mProvisioningTimeoutMs = timeoutMs;
                return this;
            }

            public Builder withIPv6AddrGenModeEUI64() {
                this.mConfig.mIPv6AddrGenMode = 0;
                return this;
            }

            public Builder withIPv6AddrGenModeStablePrivacy() {
                this.mConfig.mIPv6AddrGenMode = 2;
                return this;
            }

            public ProvisioningConfiguration build() {
                return new ProvisioningConfiguration(this.mConfig);
            }
        }

        public ProvisioningConfiguration(ProvisioningConfiguration other) {
            this.mEnableIPv4 = other.mEnableIPv4;
            this.mEnableIPv6 = other.mEnableIPv6;
            this.mUsingIpReachabilityMonitor = other.mUsingIpReachabilityMonitor;
            this.mRequestedPreDhcpActionMs = other.mRequestedPreDhcpActionMs;
            this.mInitialConfig = InitialConfiguration.copy(other.mInitialConfig);
            this.mStaticIpConfig = other.mStaticIpConfig;
            this.mApfCapabilities = other.mApfCapabilities;
            this.mProvisioningTimeoutMs = other.mProvisioningTimeoutMs;
            this.mRapidCommit = other.mRapidCommit;
            this.mDiscoverSent = other.mDiscoverSent;
        }

        public String toString() {
            return new StringJoiner(", ", getClass().getSimpleName() + "{", "}").add("mEnableIPv4: " + this.mEnableIPv4).add("mEnableIPv6: " + this.mEnableIPv6).add("mUsingIpReachabilityMonitor: " + this.mUsingIpReachabilityMonitor).add("mRequestedPreDhcpActionMs: " + this.mRequestedPreDhcpActionMs).add("mInitialConfig: " + this.mInitialConfig).add("mStaticIpConfig: " + this.mStaticIpConfig).add("mApfCapabilities: " + this.mApfCapabilities).add("mProvisioningTimeoutMs: " + this.mProvisioningTimeoutMs).add("mIPv6AddrGenMode: " + this.mIPv6AddrGenMode).toString();
        }

        public boolean isValid() {
            return this.mInitialConfig != null ? this.mInitialConfig.isValid() : true;
        }
    }

    class RunningState extends State {
        private boolean mDhcpActionInFlight;
        private ConnectivityPacketTracker mPacketTracker;

        RunningState() {
        }

        public void enter() {
            IpManager.this.mApfFilter = ApfFilter.maybeCreate(IpManager.this.mConfiguration.mApfCapabilities, IpManager.this.mNetworkInterface, IpManager.this.mCallback, IpManager.this.mMulticastFiltering, IpManager.this.mContext.getResources().getBoolean(17956889), IpManager.this.mContext);
            if (IpManager.this.mApfFilter == null) {
                IpManager.this.mCallback.setFallbackMulticastFilter(IpManager.this.mMulticastFiltering);
            }
            this.mPacketTracker = createPacketTracker();
            if (this.mPacketTracker != null) {
                this.mPacketTracker.start();
            }
            if (IpManager.this.mConfiguration.mEnableIPv6 && (IpManager.this.startIPv6() ^ 1) != 0) {
                IpManager.this.doImmediateProvisioningFailure(5);
                IpManager.this.transitionTo(IpManager.this.mStoppingState);
            } else if (!IpManager.this.mConfiguration.mEnableIPv4 || (IpManager.this.startIPv4() ^ 1) == 0) {
                InitialConfiguration config = IpManager.this.mConfiguration.mInitialConfig;
                if (config != null && (IpManager.this.applyInitialConfig(config) ^ 1) != 0) {
                    IpManager.this.doImmediateProvisioningFailure(7);
                    IpManager.this.transitionTo(IpManager.this.mStoppingState);
                } else if (IpManager.this.mConfiguration.mUsingIpReachabilityMonitor && (IpManager.this.startIpReachabilityMonitor() ^ 1) != 0) {
                    IpManager.this.doImmediateProvisioningFailure(6);
                    IpManager.this.transitionTo(IpManager.this.mStoppingState);
                }
            } else {
                IpManager.this.doImmediateProvisioningFailure(4);
                IpManager.this.transitionTo(IpManager.this.mStoppingState);
            }
        }

        public void exit() {
            stopDhcpAction();
            if (IpManager.this.mIpReachabilityMonitor != null) {
                IpManager.this.mIpReachabilityMonitor.stop();
                IpManager.this.mIpReachabilityMonitor = null;
            }
            if (IpManager.this.mDhcpClient != null) {
                IpManager.this.mDhcpClient.sendMessage(DhcpClient.CMD_STOP_DHCP);
                IpManager.this.mDhcpClient.doQuit();
            }
            if (this.mPacketTracker != null) {
                this.mPacketTracker.stop();
                this.mPacketTracker = null;
            }
            if (IpManager.this.mApfFilter != null) {
                IpManager.this.mApfFilter.shutdown();
                IpManager.this.mApfFilter = null;
            }
            IpManager.this.resetLinkProperties();
        }

        private ConnectivityPacketTracker createPacketTracker() {
            try {
                return new ConnectivityPacketTracker(IpManager.this.mNetworkInterface, IpManager.this.mConnectivityPacketLog);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private void ensureDhcpAction() {
            if (!this.mDhcpActionInFlight) {
                IpManager.this.mCallback.onPreDhcpAction();
                this.mDhcpActionInFlight = true;
                IpManager.this.mDhcpActionTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) IpManager.this.mConfiguration.mRequestedPreDhcpActionMs));
            }
        }

        private void stopDhcpAction() {
            IpManager.this.mDhcpActionTimeoutAlarm.cancel();
            if (this.mDhcpActionInFlight) {
                IpManager.this.mCallback.onPostDhcpAction();
                this.mDhcpActionInFlight = false;
            }
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    IpManager.this.transitionTo(IpManager.this.mStoppingState);
                    break;
                case 2:
                    if (IpManager.this.mDhcpClient != null) {
                        stopDhcpAction();
                        IpManager.this.mDhcpClient.sendMessage(DhcpClient.CMD_RENEW_AFTER_ROAMING);
                        break;
                    }
                    break;
                case 3:
                    if (IpManager.this.mIpReachabilityMonitor != null) {
                        IpManager.this.mIpReachabilityMonitor.probeAll();
                        break;
                    }
                    break;
                case 4:
                    if (IpManager.this.mDhcpClient != null) {
                        IpManager.this.mDhcpClient.sendMessage(DhcpClient.CMD_PRE_DHCP_ACTION_COMPLETE);
                        break;
                    }
                    break;
                case 5:
                    if (!IpManager.this.handleLinkPropertiesUpdate(true)) {
                        IpManager.this.transitionTo(IpManager.this.mStoppingState);
                        break;
                    }
                    break;
                case 6:
                    IpManager.this.mTcpBufferSizes = (String) msg.obj;
                    IpManager.this.handleLinkPropertiesUpdate(true);
                    break;
                case 7:
                    IpManager.this.mHttpProxy = (ProxyInfo) msg.obj;
                    IpManager.this.handleLinkPropertiesUpdate(true);
                    break;
                case 8:
                    IpManager.this.mMulticastFiltering = ((Boolean) msg.obj).booleanValue();
                    if (IpManager.this.mApfFilter == null) {
                        IpManager.this.mCallback.setFallbackMulticastFilter(IpManager.this.mMulticastFiltering);
                        break;
                    }
                    IpManager.this.mApfFilter.setMulticastFilter(IpManager.this.mMulticastFiltering);
                    break;
                case 10:
                    stopDhcpAction();
                    break;
                case DhcpClient.CMD_PRE_DHCP_ACTION /*196611*/:
                    if (IpManager.this.mConfiguration.mRequestedPreDhcpActionMs <= 0) {
                        IpManager.this.sendMessage(4);
                        break;
                    }
                    ensureDhcpAction();
                    break;
                case DhcpClient.CMD_POST_DHCP_ACTION /*196612*/:
                    stopDhcpAction();
                    switch (msg.arg1) {
                        case 1:
                            DhcpResults newDhcpResult = msg.obj;
                            if (IpManager.this.mDhcpResults != null && IpManager.this.mDhcpResults.ipAddress != null && (IpManager.this.mDhcpResults.ipAddress.equals(newDhcpResult.ipAddress) ^ 1) != 0) {
                                IpManager.this.setDifferentIPStatics(IpManager.this.mDhcpResults, newDhcpResult);
                                IpManager.this.handleIPv4Failure();
                                break;
                            }
                            IpManager.this.handleIPv4Success((DhcpResults) msg.obj);
                            break;
                            break;
                        case 2:
                            IpManager.this.handleIPv4Failure();
                            break;
                        default:
                            IpManager.this.logError("Unknown CMD_POST_DHCP_ACTION status: %s", Integer.valueOf(msg.arg1));
                            break;
                    }
                case DhcpClient.CMD_ON_QUIT /*196613*/:
                    IpManager.this.logError("Unexpected CMD_ON_QUIT.", new Object[0]);
                    IpManager.this.mDhcpClient = null;
                    break;
                case DhcpClient.CMD_CLEAR_LINKADDRESS /*196615*/:
                    IpManager.this.clearIPv4Address();
                    break;
                case DhcpClient.CMD_CONFIGURE_LINKADDRESS /*196616*/:
                    LinkAddress ipAddress = msg.obj;
                    InterfaceConfiguration orginIfcg = null;
                    try {
                        orginIfcg = IpManager.this.mNwService.getInterfaceConfig(IpManager.this.mInterfaceName);
                    } catch (RemoteException e) {
                        Log.d(IpManager.this.mTag, "fail to get interfaceConfig");
                    }
                    if (orginIfcg != null) {
                        LinkAddress orginalAddress = orginIfcg.getLinkAddress();
                        if (orginalAddress != null && orginalAddress.equals(ipAddress)) {
                            Log.d(IpManager.this.mTag, "configAddress  address " + ipAddress + " same as old one");
                            IpManager.this.mCallback.onProvisioningSuccess(IpManager.this.assembleLinkProperties());
                            IpManager.this.mDhcpClient.sendMessage(DhcpClient.EVENT_LINKADDRESS_CONFIGURED);
                            break;
                        }
                    }
                    if (!IpManager.this.setIPv4Address(ipAddress)) {
                        IpManager.this.logError("Failed to set IPv4 address.", new Object[0]);
                        IpManager.this.dispatchCallback(ProvisioningChange.LOST_PROVISIONING, new LinkProperties(IpManager.this.mLinkProperties));
                        IpManager.this.transitionTo(IpManager.this.mStoppingState);
                        break;
                    }
                    IpManager.this.mDhcpClient.sendMessage(DhcpClient.EVENT_LINKADDRESS_CONFIGURED);
                    break;
                default:
                    return false;
            }
            IpManager.this.mMsgStateLogger.handled(this, IpManager.this.getCurrentState());
            return true;
        }
    }

    class StartedState extends State {
        StartedState() {
        }

        public void enter() {
            IpManager.this.mStartTimeMillis = SystemClock.elapsedRealtime();
            if (IpManager.this.mConfiguration.mProvisioningTimeoutMs > 0) {
                IpManager.this.mProvisioningTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) IpManager.this.mConfiguration.mProvisioningTimeoutMs));
            }
            if (readyToProceed()) {
                IpManager.this.transitionTo(IpManager.this.mRunningState);
            } else {
                IpManager.this.stopAllIP();
            }
        }

        public void exit() {
            IpManager.this.mProvisioningTimeoutAlarm.cancel();
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    IpManager.this.transitionTo(IpManager.this.mStoppingState);
                    break;
                case 5:
                    IpManager.this.handleLinkPropertiesUpdate(false);
                    if (readyToProceed()) {
                        IpManager.this.transitionTo(IpManager.this.mRunningState);
                        break;
                    }
                    break;
                case 9:
                    IpManager.this.handleProvisioningFailure();
                    break;
                default:
                    IpManager.this.deferMessage(msg);
                    break;
            }
            IpManager.this.mMsgStateLogger.handled(this, IpManager.this.getCurrentState());
            return true;
        }

        boolean readyToProceed() {
            if (IpManager.this.mLinkProperties.hasIPv4Address()) {
                return false;
            }
            return IpManager.this.mLinkProperties.hasGlobalIPv6Address() ^ 1;
        }
    }

    class StoppedState extends State {
        StoppedState() {
        }

        public void enter() {
            IpManager.this.stopAllIP();
            IpManager.this.resetLinkProperties();
            if (IpManager.this.mStartTimeMillis > 0) {
                IpManager.this.recordMetric(3);
                IpManager.this.mStartTimeMillis = 0;
            }
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    break;
                case 2:
                    IpManager.this.mConfiguration = (ProvisioningConfiguration) msg.obj;
                    IpManager.this.transitionTo(IpManager.this.mStartedState);
                    break;
                case 5:
                    IpManager.this.handleLinkPropertiesUpdate(false);
                    break;
                case 6:
                    IpManager.this.mTcpBufferSizes = (String) msg.obj;
                    IpManager.this.handleLinkPropertiesUpdate(false);
                    break;
                case 7:
                    IpManager.this.mHttpProxy = (ProxyInfo) msg.obj;
                    IpManager.this.handleLinkPropertiesUpdate(false);
                    break;
                case 8:
                    IpManager.this.mMulticastFiltering = ((Boolean) msg.obj).booleanValue();
                    break;
                case DhcpClient.CMD_ON_QUIT /*196613*/:
                    IpManager.this.logError("Unexpected CMD_ON_QUIT (already stopped).", new Object[0]);
                    break;
                default:
                    return false;
            }
            IpManager.this.mMsgStateLogger.handled(this, IpManager.this.getCurrentState());
            return true;
        }
    }

    class StoppingState extends State {
        StoppingState() {
        }

        public void enter() {
            if (IpManager.this.mDhcpClient == null) {
                IpManager.this.transitionTo(IpManager.this.mStoppedState);
            }
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    break;
                case DhcpClient.CMD_ON_QUIT /*196613*/:
                    IpManager.this.mDhcpClient = null;
                    IpManager.this.transitionTo(IpManager.this.mStoppedState);
                    break;
                case DhcpClient.CMD_CLEAR_LINKADDRESS /*196615*/:
                    IpManager.this.clearIPv4Address();
                    break;
                default:
                    IpManager.this.deferMessage(msg);
                    break;
            }
            IpManager.this.mMsgStateLogger.handled(this, IpManager.this.getCurrentState());
            return true;
        }
    }

    public static class WaitForProvisioningCallback extends Callback {
        private LinkProperties mCallbackLinkProperties;

        public LinkProperties waitForProvisioning() {
            LinkProperties linkProperties;
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
                linkProperties = this.mCallbackLinkProperties;
            }
            return linkProperties;
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
            synchronized (this) {
                this.mCallbackLinkProperties = newLp;
                notify();
            }
        }

        public void onProvisioningFailure(LinkProperties newLp) {
            synchronized (this) {
                this.mCallbackLinkProperties = null;
                notify();
            }
        }
    }

    private static /* synthetic */ int[] -getandroid-net-LinkProperties$ProvisioningChangeSwitchesValues() {
        if (-android-net-LinkProperties$ProvisioningChangeSwitchesValues != null) {
            return -android-net-LinkProperties$ProvisioningChangeSwitchesValues;
        }
        int[] iArr = new int[ProvisioningChange.values().length];
        try {
            iArr[ProvisioningChange.GAINED_PROVISIONING.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ProvisioningChange.LOST_PROVISIONING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[ProvisioningChange.STILL_NOT_PROVISIONED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[ProvisioningChange.STILL_PROVISIONED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        -android-net-LinkProperties$ProvisioningChangeSwitchesValues = iArr;
        return iArr;
    }

    public IpManager(Context context, String ifName, Callback callback) {
        this(context, ifName, callback, Stub.asInterface(ServiceManager.getService("network_management")), NetdService.getInstance());
    }

    public IpManager(Context context, String ifName, Callback callback, INetworkManagementService nwService) {
        this(context, ifName, callback, nwService, NetdService.getInstance());
    }

    IpManager(Context context, String ifName, Callback callback, INetworkManagementService nwService, INetd netd) {
        super(IpManager.class.getSimpleName() + "." + ifName);
        this.mStoppedState = new StoppedState();
        this.mStoppingState = new StoppingState();
        this.mStartedState = new StartedState();
        this.mRunningState = new RunningState();
        this.mMetricsLog = new IpConnectivityLog();
        this.mTag = getName();
        this.mContext = context;
        this.mInterfaceName = ifName;
        this.mClatInterfaceName = CLAT_PREFIX + ifName;
        this.mCallback = new LoggingCallbackWrapper(callback);
        this.mNwService = nwService;
        this.mNetd = netd;
        this.mLog = new SharedLog(500, this.mTag);
        this.mConnectivityPacketLog = new LocalLog(100);
        this.mMsgStateLogger = new MessageHandlingLogger();
        this.mNetlinkTracker = new NetlinkTracker(this.mInterfaceName, new com.android.server.net.NetlinkTracker.Callback() {
            public void update() {
                IpManager.this.sendMessage(5);
            }
        }) {
            public void interfaceAdded(String iface) {
                super.interfaceAdded(iface);
                if (IpManager.this.mClatInterfaceName.equals(iface)) {
                    IpManager.this.mCallback.setNeighborDiscoveryOffload(false);
                } else if (!IpManager.this.mInterfaceName.equals(iface)) {
                    return;
                }
                logMsg("interfaceAdded(" + iface + ")");
            }

            public void interfaceRemoved(String iface) {
                super.interfaceRemoved(iface);
                if (IpManager.this.mClatInterfaceName.equals(iface)) {
                    IpManager.this.mCallback.setNeighborDiscoveryOffload(true);
                } else if (!IpManager.this.mInterfaceName.equals(iface)) {
                    return;
                }
                logMsg("interfaceRemoved(" + iface + ")");
            }

            private void logMsg(String msg) {
                Log.d(IpManager.this.mTag, msg);
                IpManager.this.getHandler().post(new -$Lambda$EIaFPv5OO8Upo9X60vMtrcUNUEQ((byte) 1, this, msg));
            }

            /* synthetic */ void lambda$-android_net_ip_IpManager$2_26954(String msg) {
                IpManager.this.mLog.log("OBSERVED " + msg);
            }
        };
        this.mLinkProperties = new LinkProperties();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
        this.mMultinetworkPolicyTracker = new MultinetworkPolicyTracker(this.mContext, getHandler(), new android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass2(this));
        this.mProvisioningTimeoutAlarm = new WakeupMessage(this.mContext, getHandler(), this.mTag + ".EVENT_PROVISIONING_TIMEOUT", 9);
        this.mDhcpActionTimeoutAlarm = new WakeupMessage(this.mContext, getHandler(), this.mTag + ".EVENT_DHCPACTION_TIMEOUT", 10);
        configureAndStartStateMachine();
        startStateMachineUpdaters();
    }

    /* synthetic */ void lambda$-android_net_ip_IpManager_27235() {
        this.mLog.log("OBSERVED AvoidBadWifi changed");
    }

    private void configureAndStartStateMachine() {
        addState(this.mStoppedState);
        addState(this.mStartedState);
        addState(this.mRunningState, this.mStartedState);
        addState(this.mStoppingState);
        setInitialState(this.mStoppedState);
        super.start();
    }

    private void startStateMachineUpdaters() {
        try {
            this.mNwService.registerObserver(this.mNetlinkTracker);
        } catch (RemoteException e) {
            logError("Couldn't register NetlinkTracker: %s", e);
        }
        this.mMultinetworkPolicyTracker.start();
    }

    protected void onQuitting() {
        this.mCallback.onQuit();
    }

    public void shutdown() {
        stop();
        this.mMultinetworkPolicyTracker.shutdown();
        quit();
    }

    public static Builder buildProvisioningConfiguration() {
        return new Builder();
    }

    public void startProvisioning(ProvisioningConfiguration req) {
        if (req.isValid()) {
            getNetworkInterface();
            this.mCallback.setNeighborDiscoveryOffload(true);
            sendMessage(2, new ProvisioningConfiguration(req));
            return;
        }
        doImmediateProvisioningFailure(7);
    }

    public void startProvisioning(StaticIpConfiguration staticIpConfig) {
        startProvisioning(buildProvisioningConfiguration().withStaticConfiguration(staticIpConfig).build());
    }

    public void startProvisioning() {
        startProvisioning(new ProvisioningConfiguration());
    }

    public void stop() {
        sendMessage(1);
    }

    public void confirmConfiguration() {
        sendMessage(3);
    }

    public void completedPreDhcpAction() {
        sendMessage(4);
    }

    public void setTcpBufferSizes(String tcpBufferSizes) {
        sendMessage(6, tcpBufferSizes);
    }

    public void setHttpProxy(ProxyInfo proxyInfo) {
        sendMessage(7, proxyInfo);
    }

    public void setMulticastFilter(boolean enabled) {
        sendMessage(8, Boolean.valueOf(enabled));
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (args == null || args.length <= 0 || !DUMP_ARG_CONFIRM.equals(args[0])) {
            ApfFilter apfFilter = this.mApfFilter;
            ProvisioningConfiguration provisioningConfig = this.mConfiguration;
            ApfCapabilities apfCapabilities = provisioningConfig != null ? provisioningConfig.mApfCapabilities : null;
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            pw.println(this.mTag + " APF dump:");
            pw.increaseIndent();
            if (apfFilter != null) {
                apfFilter.dump(pw);
            } else {
                pw.print("No active ApfFilter; ");
                if (provisioningConfig == null) {
                    pw.println("IpManager not yet started.");
                } else if (apfCapabilities == null || apfCapabilities.apfVersionSupported == 0) {
                    pw.println("Hardware does not support APF.");
                } else {
                    pw.println("ApfFilter not yet started, APF capabilities: " + apfCapabilities);
                }
            }
            pw.decreaseIndent();
            pw.println();
            pw.println(this.mTag + " current ProvisioningConfiguration:");
            pw.increaseIndent();
            pw.println(Objects.toString(provisioningConfig, "N/A"));
            pw.decreaseIndent();
            pw.println();
            pw.println(this.mTag + " StateMachine dump:");
            pw.increaseIndent();
            this.mLog.dump(fd, pw, args);
            pw.decreaseIndent();
            pw.println();
            pw.println(this.mTag + " connectivity packet log:");
            pw.println();
            pw.println("Debug with python and scapy via:");
            pw.println("shell$ python");
            pw.println(">>> from scapy import all as scapy");
            pw.println(">>> scapy.Ether(\"<paste_hex_string>\".decode(\"hex\")).show2()");
            pw.println();
            pw.increaseIndent();
            this.mConnectivityPacketLog.readOnlyLocalLog().dump(fd, pw, args);
            pw.decreaseIndent();
            return;
        }
        confirmConfiguration();
    }

    protected String getWhatToString(int what) {
        return (String) sWhatToString.get(what, "UNKNOWN: " + Integer.toString(what));
    }

    protected String getLogRecString(Message msg) {
        String str = "%s/%d %d %d %s [%s]";
        Object[] objArr = new Object[6];
        objArr[0] = this.mInterfaceName;
        objArr[1] = Integer.valueOf(this.mNetworkInterface == null ? -1 : this.mNetworkInterface.getIndex());
        objArr[2] = Integer.valueOf(msg.arg1);
        objArr[3] = Integer.valueOf(msg.arg2);
        objArr[4] = Objects.toString(msg.obj);
        objArr[5] = this.mMsgStateLogger;
        String logLine = String.format(str, objArr);
        this.mLog.log(getWhatToString(msg.what) + " " + logLine);
        this.mMsgStateLogger.reset();
        return logLine;
    }

    protected boolean recordLogRec(Message msg) {
        boolean shouldLog = msg.what != 5;
        if (!shouldLog) {
            this.mMsgStateLogger.reset();
        }
        return shouldLog;
    }

    private void logError(String fmt, Object... args) {
        String msg = "ERROR " + String.format(fmt, args);
        Log.e(this.mTag, msg);
        this.mLog.log(msg);
    }

    private void getNetworkInterface() {
        try {
            this.mNetworkInterface = NetworkInterface.getByName(this.mInterfaceName);
        } catch (Exception e) {
            logError("Failed to get interface object: %s", e);
        }
    }

    private void resetLinkProperties() {
        this.mNetlinkTracker.clearLinkProperties();
        this.mConfiguration = null;
        this.mDhcpResults = null;
        this.mTcpBufferSizes = "";
        this.mHttpProxy = null;
        this.mLinkProperties = new LinkProperties();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
    }

    private void recordMetric(int type) {
        if (this.mStartTimeMillis <= 0) {
            Log.wtf(this.mTag, "Start time undefined!");
        }
        this.mMetricsLog.log(this.mInterfaceName, new IpManagerEvent(type, SystemClock.elapsedRealtime() - this.mStartTimeMillis));
    }

    static boolean isProvisioned(LinkProperties lp, InitialConfiguration config) {
        if (lp.hasIPv4Address() || lp.isProvisioned()) {
            return true;
        }
        if (config == null) {
            return false;
        }
        return config.isProvisionedBy(lp.getLinkAddresses(), lp.getRoutes());
    }

    private ProvisioningChange compareProvisioning(LinkProperties oldLp, LinkProperties newLp) {
        ProvisioningChange delta;
        InitialConfiguration config = this.mConfiguration != null ? this.mConfiguration.mInitialConfig : null;
        boolean wasProvisioned = isProvisioned(oldLp, config);
        boolean isProvisioned = isProvisioned(newLp, config);
        if (!wasProvisioned && isProvisioned) {
            delta = ProvisioningChange.GAINED_PROVISIONING;
        } else if (wasProvisioned && isProvisioned) {
            delta = ProvisioningChange.STILL_PROVISIONED;
        } else if (wasProvisioned || (isProvisioned ^ 1) == 0) {
            delta = ProvisioningChange.LOST_PROVISIONING;
        } else {
            delta = ProvisioningChange.STILL_NOT_PROVISIONED;
        }
        int lostIPv6 = oldLp.isIPv6Provisioned() ? newLp.isIPv6Provisioned() ^ 1 : 0;
        int lostIPv4Address = oldLp.hasIPv4Address() ? newLp.hasIPv4Address() ^ 1 : 0;
        int lostIPv6Router;
        if (oldLp.hasIPv6DefaultRoute()) {
            lostIPv6Router = newLp.hasIPv6DefaultRoute() ^ 1;
        } else {
            lostIPv6Router = 0;
        }
        boolean ignoreIPv6ProvisioningLoss = this.mMultinetworkPolicyTracker.getAvoidBadWifi() ^ 1;
        if (!(lostIPv4Address == 0 && (lostIPv6 == 0 || (ignoreIPv6ProvisioningLoss ^ 1) == 0))) {
            delta = ProvisioningChange.LOST_PROVISIONING;
        }
        if (!oldLp.hasGlobalIPv6Address() || lostIPv6Router == 0 || (ignoreIPv6ProvisioningLoss ^ 1) == 0) {
            return delta;
        }
        return ProvisioningChange.LOST_PROVISIONING;
    }

    private void dispatchCallback(ProvisioningChange delta, LinkProperties newLp) {
        switch (-getandroid-net-LinkProperties$ProvisioningChangeSwitchesValues()[delta.ordinal()]) {
            case 1:
                recordMetric(1);
                this.mCallback.onProvisioningSuccess(newLp);
                return;
            case 2:
                recordMetric(2);
                this.mCallback.onProvisioningFailure(newLp);
                return;
            default:
                this.mCallback.onLinkPropertiesChange(newLp);
                return;
        }
    }

    private ProvisioningChange setLinkProperties(LinkProperties newLp) {
        if (this.mApfFilter != null) {
            this.mApfFilter.setLinkProperties(newLp);
        }
        if (this.mIpReachabilityMonitor != null) {
            this.mIpReachabilityMonitor.updateLinkProperties(newLp);
        }
        ProvisioningChange delta = compareProvisioning(this.mLinkProperties, newLp);
        this.mLinkProperties = new LinkProperties(newLp);
        if (delta == ProvisioningChange.GAINED_PROVISIONING) {
            this.mProvisioningTimeoutAlarm.cancel();
        }
        return delta;
    }

    private LinkProperties assembleLinkProperties() {
        LinkProperties newLp = new LinkProperties();
        newLp.setInterfaceName(this.mInterfaceName);
        LinkProperties netlinkLinkProperties = this.mNetlinkTracker.getLinkProperties();
        newLp.setLinkAddresses(netlinkLinkProperties.getLinkAddresses());
        for (RouteInfo route : netlinkLinkProperties.getRoutes()) {
            newLp.addRoute(route);
        }
        addAllReachableDnsServers(newLp, netlinkLinkProperties.getDnsServers());
        if (this.mDhcpResults != null) {
            for (RouteInfo route2 : this.mDhcpResults.getRoutes(this.mInterfaceName)) {
                newLp.addRoute(route2);
            }
            addAllReachableDnsServers(newLp, this.mDhcpResults.dnsServers);
            newLp.setDomains(this.mDhcpResults.domains);
            if (this.mDhcpResults.mtu != 0) {
                newLp.setMtu(this.mDhcpResults.mtu);
            }
        }
        if (!TextUtils.isEmpty(this.mTcpBufferSizes)) {
            newLp.setTcpBufferSizes(this.mTcpBufferSizes);
        }
        if (this.mHttpProxy != null) {
            newLp.setHttpProxy(this.mHttpProxy);
        }
        if (!(this.mConfiguration == null || this.mConfiguration.mInitialConfig == null)) {
            InitialConfiguration config = this.mConfiguration.mInitialConfig;
            if (config.isProvisionedBy(newLp.getLinkAddresses(), null)) {
                for (IpPrefix prefix : config.directlyConnectedRoutes) {
                    newLp.addRoute(new RouteInfo(prefix, null, this.mInterfaceName));
                }
            }
            addAllReachableDnsServers(newLp, config.dnsServers);
        }
        LinkProperties oldLp = this.mLinkProperties;
        return newLp;
    }

    private static void addAllReachableDnsServers(LinkProperties lp, Iterable<InetAddress> dnses) {
        for (InetAddress dns : dnses) {
            if (!dns.isAnyLocalAddress() && lp.isReachable(dns)) {
                lp.addDnsServer(dns);
            }
        }
    }

    private boolean handleLinkPropertiesUpdate(boolean sendCallbacks) {
        boolean z = true;
        LinkProperties newLp = assembleLinkProperties();
        if (Objects.equals(newLp, this.mLinkProperties)) {
            return true;
        }
        ProvisioningChange delta = setLinkProperties(newLp);
        if (sendCallbacks) {
            dispatchCallback(delta, newLp);
        }
        if (delta == ProvisioningChange.LOST_PROVISIONING) {
            z = false;
        }
        return z;
    }

    private boolean setIPv4Address(LinkAddress address) {
        InterfaceConfiguration ifcg = new InterfaceConfiguration();
        ifcg.setLinkAddress(address);
        try {
            this.mNwService.setInterfaceConfig(this.mInterfaceName, ifcg);
            return true;
        } catch (Exception e) {
            logError("IPv4 configuration failed: %s", e);
            return false;
        }
    }

    private void clearIPv4Address() {
        try {
            InterfaceConfiguration ifcg = new InterfaceConfiguration();
            ifcg.setLinkAddress(new LinkAddress("0.0.0.0/0"));
            this.mNwService.setInterfaceConfig(this.mInterfaceName, ifcg);
        } catch (Exception e) {
            logError("Failed to clear IPv4 address on interface %s: %s", this.mInterfaceName, e);
        }
    }

    private void handleIPv4Success(DhcpResults dhcpResults) {
        this.mDhcpResults = new DhcpResults(dhcpResults);
        LinkProperties newLp = assembleLinkProperties();
        ProvisioningChange delta = setLinkProperties(newLp);
        this.mCallback.onNewDhcpResults(dhcpResults);
        dispatchCallback(delta, newLp);
    }

    private void handleIPv4Failure() {
        clearIPv4Address();
        this.mDhcpResults = null;
        this.mCallback.onNewDhcpResults(null);
        handleProvisioningFailure();
    }

    private void handleProvisioningFailure() {
        LinkProperties newLp = assembleLinkProperties();
        ProvisioningChange delta = setLinkProperties(newLp);
        if (delta == ProvisioningChange.STILL_NOT_PROVISIONED) {
            delta = ProvisioningChange.LOST_PROVISIONING;
        }
        dispatchCallback(delta, newLp);
        if (delta == ProvisioningChange.LOST_PROVISIONING) {
            transitionTo(this.mStoppingState);
        }
    }

    private void doImmediateProvisioningFailure(int failureType) {
        logError("onProvisioningFailure(): %s", Integer.valueOf(failureType));
        recordMetric(failureType);
        this.mCallback.onProvisioningFailure(new LinkProperties(this.mLinkProperties));
    }

    private boolean startIPv4() {
        int i = 0;
        if (this.mConfiguration.mStaticIpConfig == null) {
            this.mDhcpClient = DhcpClient.makeDhcpClient(this.mContext, this, this.mInterfaceName);
            this.mDhcpClient.registerForPreDhcpNotification();
            if (this.mConfiguration.mRapidCommit || this.mConfiguration.mDiscoverSent) {
                int i2;
                DhcpClient dhcpClient = this.mDhcpClient;
                if (this.mConfiguration.mRapidCommit) {
                    i2 = 1;
                } else {
                    i2 = 0;
                }
                if (this.mConfiguration.mDiscoverSent) {
                    i = 1;
                }
                dhcpClient.sendMessage(DhcpClient.CMD_START_DHCP_RAPID_COMMIT, i2, i);
            } else {
                this.mDhcpClient.sendMessage(DhcpClient.CMD_START_DHCP);
            }
        } else if (!setIPv4Address(this.mConfiguration.mStaticIpConfig.ipAddress)) {
            return false;
        } else {
            handleIPv4Success(new DhcpResults(this.mConfiguration.mStaticIpConfig));
        }
        return true;
    }

    private void setIPv6AddrGenModeIfSupported() throws RemoteException {
        try {
            this.mNwService.setIPv6AddrGenMode(this.mInterfaceName, this.mConfiguration.mIPv6AddrGenMode);
        } catch (ServiceSpecificException e) {
            if (e.errorCode != OsConstants.EOPNOTSUPP) {
                logError("Unable to set IPv6 addrgen mode: %s", e);
            }
        }
    }

    private boolean startIPv6() {
        try {
            this.mNwService.setInterfaceIpv6PrivacyExtensions(this.mInterfaceName, true);
            setIPv6AddrGenModeIfSupported();
            this.mNwService.enableIpv6(this.mInterfaceName);
            return true;
        } catch (Exception e) {
            logError("Unable to change interface settings: %s", e);
            return false;
        }
    }

    private boolean applyInitialConfig(InitialConfiguration config) {
        if (this.mNetd == null) {
            logError("tried to add %s to %s but INetd was null", config, this.mInterfaceName);
            return false;
        }
        for (LinkAddress addr : findAll(config.ipAddresses, android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass1.$INST$4)) {
            try {
                this.mNetd.interfaceAddAddress(this.mInterfaceName, addr.getAddress().getHostAddress(), addr.getPrefixLength());
            } catch (Exception e) {
                logError("failed to add %s to %s: %s", addr, this.mInterfaceName, e);
                return false;
            }
        }
        return true;
    }

    private boolean startIpReachabilityMonitor() {
        try {
            this.mIpReachabilityMonitor = new IpReachabilityMonitor(this.mContext, this.mInterfaceName, this.mLog, new android.net.ip.IpReachabilityMonitor.Callback() {
                public void notifyLost(InetAddress ip, String logMsg) {
                    IpManager.this.mCallback.onReachabilityLost(logMsg);
                }
            }, this.mMultinetworkPolicyTracker);
        } catch (IllegalArgumentException iae) {
            logError("IpReachabilityMonitor failure: %s", iae);
            this.mIpReachabilityMonitor = null;
        }
        if (this.mIpReachabilityMonitor != null) {
            return true;
        }
        return false;
    }

    public ByteBuffer buildDiscoverWithRapidCommitPacket() {
        return this.mDhcpClient.buildDiscoverWithRapidCommitPacket();
    }

    private void stopAllIP() {
        try {
            this.mNwService.disableIpv6(this.mInterfaceName);
        } catch (Exception e) {
            logError("Failed to disable IPv6: %s", e);
        }
        try {
            this.mNwService.clearInterfaceAddresses(this.mInterfaceName);
        } catch (Exception e2) {
            logError("Failed to clear addresses: %s", e2);
        }
    }

    private void setDifferentIPStatics(DhcpResults oldDhcpResults, DhcpResults newDchpResults) {
        HashMap<String, String> map = new HashMap();
        if (oldDhcpResults.gateway != null) {
            map.put("oldGatway", oldDhcpResults.gateway.toString());
        }
        if (newDchpResults.gateway != null) {
            map.put("newGatway", newDchpResults.gateway.toString());
        }
        if (oldDhcpResults.ipAddress != null) {
            map.put("oldIp", oldDhcpResults.ipAddress.toString());
        }
        if (newDchpResults.ipAddress != null) {
            map.put("newIp", newDchpResults.ipAddress.toString());
        }
        OppoStatistics.onCommon(this.mContext, WIFI_STATISTIC_KEY, WIFI_DIFFERENT_IP, map, false);
    }

    static <T> boolean any(Iterable<T> coll, Predicate<T> fn) {
        for (T t : coll) {
            if (fn.test(t)) {
                return true;
            }
        }
        return false;
    }

    static <T> boolean all(Iterable<T> coll, Predicate<T> fn) {
        return any(coll, not(fn)) ^ 1;
    }

    static <T> Predicate<T> not(Predicate<T> fn) {
        return new android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.AnonymousClass3((byte) 5, fn);
    }

    static <T> String join(String delimiter, Collection<T> coll) {
        return (String) coll.stream().map(-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ.$INST$0).collect(Collectors.joining(delimiter));
    }

    static <T> T find(Iterable<T> coll, Predicate<T> fn) {
        for (T t : coll) {
            if (fn.test(t)) {
                return t;
            }
        }
        return null;
    }

    static <T> List<T> findAll(Collection<T> coll, Predicate<T> fn) {
        return (List) coll.stream().filter(fn).collect(Collectors.toList());
    }
}
