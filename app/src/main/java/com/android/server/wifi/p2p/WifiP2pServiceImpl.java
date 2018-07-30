package com.android.server.wifi.p2p;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.wifi.V1_0.IWifiP2pIface;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.ip.IpManager;
import android.net.ip.IpManager.Callback;
import android.net.ip.IpManager.ProvisioningConfiguration;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.IWifiP2pManager.Stub;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.WifiP2pGroupList.GroupDeleteListener;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.os.Binder;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.HalDeviceManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiMetrics;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.hotspot2.AnqpCache;
import com.android.server.wifi.util.WifiAsyncChannel;
import com.android.server.wifi.util.WifiHandler;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiP2pServiceImpl extends Stub {
    public static final int AUTO_ACCEPT_INVITATION_TIME_OUT = 143376;
    private static final int BASE = 143360;
    public static final int BLOCK_DISCOVERY = 143375;
    private static boolean DBG = false;
    public static final int DISABLED = 0;
    public static final int DISABLE_P2P_TIMED_OUT = 143366;
    private static final int DISABLE_P2P_WAIT_TIME_MS = 5000;
    public static final int DISCONNECT_WIFI_REQUEST = 143372;
    public static final int DISCONNECT_WIFI_RESPONSE = 143373;
    private static final int DISCOVER_TIMEOUT_S = 120;
    private static final int DROP_WIFI_USER_ACCEPT = 143364;
    private static final int DROP_WIFI_USER_REJECT = 143365;
    private static final String EMPTY_DEVICE_ADDRESS = "00:00:00:00:00:00";
    public static final int ENABLED = 1;
    private static final Boolean FORM_GROUP = Boolean.valueOf(false);
    public static final int GROUP_CREATING_TIMED_OUT = 143361;
    private static final int GROUP_CREATING_WAIT_TIME_MS = 120000;
    private static final int GROUP_IDLE_TIME_S = 10;
    private static final int IPM_DHCP_RESULTS = 143392;
    private static final int IPM_POST_DHCP_ACTION = 143391;
    private static final int IPM_PRE_DHCP_ACTION = 143390;
    private static final int IPM_PROVISIONING_FAILURE = 143394;
    private static final int IPM_PROVISIONING_SUCCESS = 143393;
    private static final Boolean JOIN_GROUP = Boolean.valueOf(true);
    private static final String NETWORKTYPE = "WIFI_P2P";
    private static final Boolean NO_RELOAD = Boolean.valueOf(false);
    static final int P2P_BLUETOOTH_COEXISTENCE_MODE_DISABLED = 1;
    static final int P2P_BLUETOOTH_COEXISTENCE_MODE_SENSE = 2;
    public static final int P2P_CONNECTION_CHANGED = 143371;
    public static final int P2P_FIND_SPECIAL_FREQ_TIME_OUT = 143377;
    private static final int PEER_CONNECTION_USER_ACCEPT = 143362;
    private static final int PEER_CONNECTION_USER_REJECT = 143363;
    private static final Boolean RELOAD = Boolean.valueOf(true);
    private static final String SERVER_ADDRESS = "192.168.49.1";
    public static final int SET_MIRACAST_MODE = 143374;
    private static final String TAG = "WifiP2pService";
    private static int sDisableP2pTimeoutIndex = 0;
    private static int sGroupCreatingTimeoutIndex = 0;
    private boolean isNfcTriggered = false;
    private boolean mAutonomousGroup;
    private ClientHandler mClientHandler;
    private HashMap<Messenger, ClientInfo> mClientInfoList = new HashMap();
    private Context mContext;
    private final Map<IBinder, DeathHandlerData> mDeathDataByBinder = new HashMap();
    private DhcpResults mDhcpResults;
    private boolean mDiscoveryBlocked;
    private boolean mDiscoveryPostponed = false;
    private boolean mDiscoveryStarted;
    private int mFastConTriggeredNum = 0;
    private int mFindTimes = 0;
    private boolean mFoundTargetDevice = false;
    private HalDeviceManager mHalDeviceManager;
    private IWifiP2pIface mIWifiP2pIface;
    private String mIntentToConnectPeer = "";
    private IpManager mIpManager;
    private boolean mIsInvite = false;
    private boolean mJoinExistingGroup;
    private Object mLock = new Object();
    private NetworkInfo mNetworkInfo;
    INetworkManagementService mNwService;
    private int mP2pConnectFreq = 0;
    private P2pStateMachine mP2pStateMachine;
    private final boolean mP2pSupported;
    private AsyncChannel mReplyChannel = new WifiAsyncChannel(TAG);
    private String mServiceDiscReqId;
    private byte mServiceTransactionId = (byte) 0;
    private boolean mTemporarilyDisconnectedWifi = false;
    private WifiP2pDevice mThisDevice = new WifiP2pDevice();
    private AsyncChannel mWifiChannel;
    private WifiInjector mWifiInjector;

    private class ClientHandler extends WifiHandler {
        ClientHandler(String tag, Looper looper) {
            super(tag, looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 139265:
                case 139268:
                case 139274:
                case 139280:
                case 139283:
                case 139285:
                case 139287:
                case 139292:
                case 139295:
                case 139298:
                case 139301:
                case 139304:
                case 139307:
                case 139310:
                case 139315:
                case 139318:
                case 139321:
                case 139323:
                case 139326:
                case 139329:
                case 139332:
                case 139335:
                case 139361:
                    break;
                case 139271:
                case 139277:
                    if (WifiP2pServiceImpl.this.mWifiInjector == null) {
                        WifiP2pServiceImpl.this.mWifiInjector = WifiInjector.getInstance();
                    }
                    WifiP2pServiceImpl.this.mWifiInjector.getOppoWifiSharingManager().stopWifiSharingTetheringIfNeed();
                    break;
                default:
                    Slog.d(WifiP2pServiceImpl.TAG, "ClientHandler.handleMessage ignoring msg=" + msg);
                    return;
            }
            WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(Message.obtain(msg));
        }
    }

    private class ClientInfo {
        private Messenger mMessenger;
        private SparseArray<WifiP2pServiceRequest> mReqList;
        private List<WifiP2pServiceInfo> mServList;

        /* synthetic */ ClientInfo(WifiP2pServiceImpl this$0, Messenger m, ClientInfo -this2) {
            this(m);
        }

        private ClientInfo(Messenger m) {
            this.mMessenger = m;
            this.mReqList = new SparseArray();
            this.mServList = new ArrayList();
        }
    }

    private class DeathHandlerData {
        DeathRecipient mDeathRecipient;
        Messenger mMessenger;

        DeathHandlerData(DeathRecipient dr, Messenger m) {
            this.mDeathRecipient = dr;
            this.mMessenger = m;
        }

        public String toString() {
            return "deathRecipient=" + this.mDeathRecipient + ", messenger=" + this.mMessenger;
        }
    }

    private class P2pStateMachine extends StateMachine {
        private DefaultState mDefaultState = new DefaultState();
        private FrequencyConflictState mFrequencyConflictState = new FrequencyConflictState();
        private WifiP2pGroup mGroup;
        private GroupCreatedState mGroupCreatedState = new GroupCreatedState();
        private GroupCreatingState mGroupCreatingState = new GroupCreatingState();
        private GroupNegotiationState mGroupNegotiationState = new GroupNegotiationState();
        private final WifiP2pGroupList mGroups = new WifiP2pGroupList(null, new GroupDeleteListener() {
            public void onDeleteGroup(int netId) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd("called onDeleteGroup() netId=" + netId);
                }
                P2pStateMachine.this.mWifiNative.removeP2pNetwork(netId);
                P2pStateMachine.this.mWifiNative.saveConfig();
                P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
            }
        });
        private InactiveState mInactiveState = new InactiveState();
        private boolean mIsBTCoexDisabled = false;
        private OngoingGroupRemovalState mOngoingGroupRemovalState = new OngoingGroupRemovalState();
        private P2pDisabledState mP2pDisabledState = new P2pDisabledState();
        private P2pDisablingState mP2pDisablingState = new P2pDisablingState();
        private P2pEnabledState mP2pEnabledState = new P2pEnabledState();
        private P2pEnablingState mP2pEnablingState = new P2pEnablingState();
        private P2pNotSupportedState mP2pNotSupportedState = new P2pNotSupportedState();
        private final WifiP2pDeviceList mPeers = new WifiP2pDeviceList();
        private final WifiP2pDeviceList mPeersLostDuringConnection = new WifiP2pDeviceList();
        private ProvisionDiscoveryState mProvisionDiscoveryState = new ProvisionDiscoveryState();
        private WifiP2pConfig mSavedPeerConfig = new WifiP2pConfig();
        private UserAuthorizingInviteRequestState mUserAuthorizingInviteRequestState = new UserAuthorizingInviteRequestState();
        private UserAuthorizingJoinState mUserAuthorizingJoinState = new UserAuthorizingJoinState();
        private UserAuthorizingNegotiationRequestState mUserAuthorizingNegotiationRequestState = new UserAuthorizingNegotiationRequestState();
        private WifiNative mWifNative = WifiInjector.getInstance().getWifiNative();
        private WifiInjector mWifiInjector;
        private WifiP2pMonitor mWifiMonitor = WifiInjector.getInstance().getWifiP2pMonitor();
        private WifiP2pNative mWifiNative = WifiInjector.getInstance().getWifiP2pNative();
        private final WifiP2pInfo mWifiP2pInfo = new WifiP2pInfo();

        class DefaultState extends State {
            DefaultState() {
            }

            public boolean processMessage(Message message) {
                Object obj = null;
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case 69632:
                        if (message.arg1 != 0) {
                            P2pStateMachine.this.loge("Full connection failure, error = " + message.arg1);
                            WifiP2pServiceImpl.this.mWifiChannel = null;
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                            break;
                        }
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("Full connection with WifiStateMachine established");
                        }
                        WifiP2pServiceImpl.this.mWifiChannel = (AsyncChannel) message.obj;
                        break;
                    case 69633:
                        new WifiAsyncChannel(WifiP2pServiceImpl.TAG).connect(WifiP2pServiceImpl.this.mContext, P2pStateMachine.this.getHandler(), message.replyTo);
                        break;
                    case 69636:
                        if (message.arg1 == 2) {
                            P2pStateMachine.this.loge("Send failed, client connection lost");
                        } else {
                            P2pStateMachine.this.loge("Client connection lost with reason: " + message.arg1);
                        }
                        WifiP2pServiceImpl.this.mWifiChannel = null;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        break;
                    case WifiStateMachine.CMD_ENABLE_P2P /*131203*/:
                    case 139329:
                    case 139332:
                    case 139335:
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT /*143361*/:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT /*143364*/:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT /*143365*/:
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT /*143366*/:
                    case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE /*143373*/:
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE /*143374*/:
                    case WifiP2pServiceImpl.IPM_PRE_DHCP_ACTION /*143390*/:
                    case WifiP2pServiceImpl.IPM_POST_DHCP_ACTION /*143391*/:
                    case WifiP2pServiceImpl.IPM_DHCP_RESULTS /*143392*/:
                    case WifiP2pServiceImpl.IPM_PROVISIONING_SUCCESS /*143393*/:
                    case WifiP2pServiceImpl.IPM_PROVISIONING_FAILURE /*143394*/:
                    case 147457:
                    case 147458:
                    case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT /*147477*/:
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT /*147478*/:
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT /*147484*/:
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT /*147486*/:
                    case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT /*147488*/:
                    case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT /*147493*/:
                    case WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT /*147494*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT /*147495*/:
                        break;
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ /*131204*/:
                        if (WifiP2pServiceImpl.this.mWifiChannel == null) {
                            P2pStateMachine.this.loge("Unexpected disable request when WifiChannel is null");
                            break;
                        }
                        WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
                        break;
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        break;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 2);
                        break;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 2);
                        break;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 2);
                        break;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 2);
                        break;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 2);
                        break;
                    case 139283:
                        P2pStateMachine.this.replyToMessage(message, 139284, (Object) P2pStateMachine.this.getPeers((Bundle) message.obj, message.sendingUid));
                        break;
                    case 139285:
                        P2pStateMachine.this.replyToMessage(message, 139286, (Object) new WifiP2pInfo(P2pStateMachine.this.mWifiP2pInfo));
                        break;
                    case 139287:
                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                        if (P2pStateMachine.this.mGroup != null) {
                            obj = new WifiP2pGroup(P2pStateMachine.this.mGroup);
                        }
                        p2pStateMachine.replyToMessage(message, 139288, obj);
                        break;
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 2);
                        break;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 2);
                        break;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 2);
                        break;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 2);
                        break;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 2);
                        break;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 2);
                        break;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 2);
                        break;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 2);
                        break;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139318, 2);
                        break;
                    case 139321:
                        P2pStateMachine.this.replyToMessage(message, 139322, (Object) new WifiP2pGroupList(P2pStateMachine.this.mGroups, null));
                        break;
                    case 139323:
                        if (!WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139324, 2);
                        break;
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 2);
                        break;
                    case 139339:
                    case 139340:
                        P2pStateMachine.this.replyToMessage(message, 139341, null);
                        break;
                    case 139342:
                    case 139343:
                        P2pStateMachine.this.replyToMessage(message, 139345, 2);
                        break;
                    case 139361:
                        P2pStateMachine.this.replyToMessage(message, 139361, 2);
                        break;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY /*143375*/:
                        WifiP2pServiceImpl.this.mDiscoveryBlocked = message.arg1 == 1;
                        WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            if (message.obj != null) {
                                try {
                                    message.obj.sendMessage(message.arg2);
                                    break;
                                } catch (Exception e) {
                                    P2pStateMachine.this.loge("unable to send BLOCK_DISCOVERY response: " + e);
                                    break;
                                }
                            }
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            break;
                        }
                        break;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                        if (message.obj != null) {
                            P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                            P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                            P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                        break;
                    default:
                        P2pStateMachine.this.loge("Unhandled message " + message);
                        return false;
                }
                return true;
            }
        }

        class FrequencyConflictState extends State {
            private AlertDialog mFrequencyConflictDialog;

            FrequencyConflictState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                notifyFrequencyConflict();
            }

            private void notifyFrequencyConflict() {
                P2pStateMachine.this.logd("Notify frequency conflict");
                Resources r = Resources.getSystem();
                AlertDialog dialog = new Builder(WifiP2pServiceImpl.this.mContext, 201523207).setTitle(r.getString(17041121, new Object[]{P2pStateMachine.this.getDeviceName(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)})).setMessage(r.getString(17041128)).setPositiveButton(r.getString(17041120), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT);
                    }
                }).setNegativeButton(r.getString(17039360), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface arg0) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.setCancelable(false);
                LayoutParams p = dialog.getWindow().getAttributes();
                p.ignoreHomeMenuKey = 1;
                dialog.getWindow().setAttributes(p);
                dialog.getWindow().setType(2003);
                LayoutParams attrs = dialog.getWindow().getAttributes();
                attrs.privateFlags = 16;
                dialog.getWindow().setAttributes(attrs);
                dialog.show();
                this.mFrequencyConflictDialog = dialog;
                TextView msg = (TextView) dialog.findViewById(16908299);
                if (msg != null) {
                    msg.setGravity(17);
                } else {
                    P2pStateMachine.this.loge("textview is null");
                }
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT /*143364*/:
                        if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                            WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 1);
                        } else {
                            P2pStateMachine.this.loge("DROP_WIFI_USER_ACCEPT message received when WifiChannel is null");
                        }
                        WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = true;
                        break;
                    case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT /*143365*/:
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE /*143373*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + "Wifi disconnected, retry p2p");
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                        break;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT /*147481*/:
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT /*147483*/:
                        P2pStateMachine.this.loge(getName() + "group sucess during freq conflict!");
                        break;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT /*147482*/:
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT /*147484*/:
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT /*147486*/:
                        break;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                        P2pStateMachine.this.loge(getName() + "group started after freq conflict, handle anyway");
                        P2pStateMachine.this.deferMessage(message);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
                if (this.mFrequencyConflictDialog != null) {
                    this.mFrequencyConflictDialog.dismiss();
                }
            }
        }

        class GroupCreatedState extends State {
            GroupCreatedState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                P2pStateMachine.this.mSavedPeerConfig.invalidate();
                WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                P2pStateMachine.this.updateThisDevice(0);
                if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                    P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS));
                }
                if (WifiP2pServiceImpl.this.mAutonomousGroup) {
                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                }
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                WifiP2pDevice device;
                String deviceAddress;
                switch (message.what) {
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ /*131204*/:
                        P2pStateMachine.this.sendMessage(139280);
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case 139271:
                        WifiP2pConfig config = message.obj;
                        if (!P2pStateMachine.this.isConfigInvalid(config)) {
                            P2pStateMachine.this.logd("Inviting device : " + config.deviceAddress);
                            P2pStateMachine.this.mSavedPeerConfig = config;
                            if (!P2pStateMachine.this.mWifiNative.p2pInvite(P2pStateMachine.this.mGroup, config.deviceAddress)) {
                                P2pStateMachine.this.replyToMessage(message, 139272, 0);
                                break;
                            }
                            P2pStateMachine.this.mPeers.updateStatus(config.deviceAddress, 1);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            P2pStateMachine.this.replyToMessage(message, 139273);
                            break;
                        }
                        P2pStateMachine.this.loge("Dropping connect request " + config);
                        P2pStateMachine.this.replyToMessage(message, 139272);
                        break;
                    case 139280:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " remove group");
                        }
                        P2pStateMachine.this.enableBTCoex();
                        if (!P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface())) {
                            P2pStateMachine.this.handleGroupRemoved();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            P2pStateMachine.this.replyToMessage(message, 139281, 0);
                            break;
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mOngoingGroupRemovalState);
                        P2pStateMachine.this.replyToMessage(message, 139282);
                        break;
                    case 139326:
                        WpsInfo wps = message.obj;
                        if (wps != null) {
                            int i;
                            boolean ret = true;
                            if (wps.setup == 0) {
                                ret = P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                            } else if (wps.pin == null) {
                                String pin = P2pStateMachine.this.mWifiNative.startWpsPinDisplay(P2pStateMachine.this.mGroup.getInterface(), null);
                                try {
                                    Integer.parseInt(pin);
                                    P2pStateMachine.this.notifyInvitationSent(pin, "any");
                                } catch (NumberFormatException e) {
                                    ret = false;
                                }
                            } else {
                                ret = P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), wps.pin);
                            }
                            P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                            if (ret) {
                                i = 139328;
                            } else {
                                i = 139327;
                            }
                            p2pStateMachine.replyToMessage(message, i);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139327);
                        break;
                    case WifiP2pServiceImpl.IPM_PRE_DHCP_ACTION /*143390*/:
                        P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), false);
                        WifiP2pServiceImpl.this.mIpManager.completedPreDhcpAction();
                        break;
                    case WifiP2pServiceImpl.IPM_POST_DHCP_ACTION /*143391*/:
                        P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), true);
                        P2pStateMachine.this.enableBTCoex();
                        break;
                    case WifiP2pServiceImpl.IPM_DHCP_RESULTS /*143392*/:
                        WifiP2pServiceImpl.this.mDhcpResults = (DhcpResults) message.obj;
                        break;
                    case WifiP2pServiceImpl.IPM_PROVISIONING_SUCCESS /*143393*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("mDhcpResults: " + WifiP2pServiceImpl.this.mDhcpResults);
                        }
                        if (WifiP2pServiceImpl.this.mDhcpResults.serverAddress == null) {
                            WifiP2pServiceImpl.this.mDhcpResults.setServerAddress(WifiP2pServiceImpl.SERVER_ADDRESS);
                        }
                        WifiP2pServiceImpl.this.setNfcTriggered(false);
                        P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.AUTO_ACCEPT_INVITATION_TIME_OUT);
                        if (WifiP2pServiceImpl.this.mDhcpResults != null) {
                            P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(WifiP2pServiceImpl.this.mDhcpResults.serverAddress);
                        }
                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                        try {
                            String ifname = P2pStateMachine.this.mGroup.getInterface();
                            if (WifiP2pServiceImpl.this.mDhcpResults != null) {
                                WifiP2pServiceImpl.this.mNwService.addInterfaceToLocalNetwork(ifname, WifiP2pServiceImpl.this.mDhcpResults.getRoutes(ifname));
                                break;
                            }
                        } catch (RemoteException e2) {
                            P2pStateMachine.this.loge("Failed to add iface to local network " + e2);
                            break;
                        }
                        break;
                    case WifiP2pServiceImpl.IPM_PROVISIONING_FAILURE /*143394*/:
                        P2pStateMachine.this.loge("IP provisioning failed");
                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        break;
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT /*147478*/:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            return false;
                        }
                        device = message.obj;
                        if (!P2pStateMachine.this.mGroup.contains(device)) {
                            return false;
                        }
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("Add device to lost list " + device);
                        }
                        P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                        return true;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                        P2pStateMachine.this.loge("Duplicate group creation event notice, ignore");
                        break;
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT /*147486*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " group removed");
                        }
                        P2pStateMachine.this.enableBTCoex();
                        P2pStateMachine.this.handleGroupRemoved();
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT /*147488*/:
                        P2pStatus status = message.obj;
                        if (status != P2pStatus.SUCCESS) {
                            P2pStateMachine.this.loge("Invitation result " + status);
                            if (status == P2pStatus.UNKNOWN_P2P_GROUP) {
                                int netId = P2pStateMachine.this.mGroup.getNetworkId();
                                if (netId >= 0) {
                                    if (WifiP2pServiceImpl.DBG) {
                                        P2pStateMachine.this.logd("Remove unknown client from the list");
                                    }
                                    P2pStateMachine.this.removeClientFromList(netId, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, false);
                                    P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                                    break;
                                }
                            }
                        }
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT /*147489*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT /*147491*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT /*147492*/:
                        WifiP2pProvDiscEvent provDisc = message.obj;
                        P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                        if (!(provDisc == null || provDisc.device == null)) {
                            P2pStateMachine.this.mSavedPeerConfig.deviceAddress = provDisc.device.deviceAddress;
                        }
                        if (message.what == 147491) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                        } else if (message.what == 147492) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                            P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc.pin;
                        } else {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingJoinState);
                        break;
                    case 147497:
                        if (message.obj != null) {
                            device = message.obj;
                            deviceAddress = device.deviceAddress;
                            if (deviceAddress == null) {
                                P2pStateMachine.this.loge("Disconnect on unknown device: " + device);
                                break;
                            }
                            P2pStateMachine.this.mPeers.updateStatus(deviceAddress, 3);
                            if (P2pStateMachine.this.mGroup.removeClient(deviceAddress)) {
                                if (WifiP2pServiceImpl.DBG) {
                                    P2pStateMachine.this.logd("Removed client " + deviceAddress);
                                }
                                if (WifiP2pServiceImpl.this.mAutonomousGroup || !P2pStateMachine.this.mGroup.isClientListEmpty()) {
                                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                } else {
                                    P2pStateMachine.this.logd("Client list empty, remove non-persistent p2p group");
                                    P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                }
                            } else {
                                if (WifiP2pServiceImpl.DBG) {
                                    P2pStateMachine.this.logd("Failed to remove client " + deviceAddress);
                                }
                                for (WifiP2pDevice c : P2pStateMachine.this.mGroup.getClientList()) {
                                    if (WifiP2pServiceImpl.DBG) {
                                        P2pStateMachine.this.logd("client " + c.deviceAddress);
                                    }
                                }
                            }
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd(getName() + " ap sta disconnected");
                                break;
                            }
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                        break;
                    case 147498:
                        if (message.obj != null) {
                            deviceAddress = message.obj.deviceAddress;
                            P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 0);
                            if (deviceAddress != null) {
                                if (P2pStateMachine.this.mPeers.get(deviceAddress) != null) {
                                    P2pStateMachine.this.mGroup.addClient(P2pStateMachine.this.mPeers.get(deviceAddress));
                                } else {
                                    P2pStateMachine.this.mGroup.addClient(deviceAddress);
                                }
                                P2pStateMachine.this.mPeers.updateStatus(deviceAddress, 0);
                                if (WifiP2pServiceImpl.DBG) {
                                    P2pStateMachine.this.logd(getName() + " ap sta connected");
                                }
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                WifiP2pServiceImpl.this.setNfcTriggered(false);
                                P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.AUTO_ACCEPT_INVITATION_TIME_OUT);
                            } else {
                                P2pStateMachine.this.loge("Connect on null device address, ignore");
                            }
                            P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
                P2pStateMachine.this.updateThisDevice(3);
                P2pStateMachine.this.resetWifiP2pInfo();
                WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, null);
                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
            }
        }

        class GroupCreatingState extends State {
            GroupCreatingState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT, WifiP2pServiceImpl.sGroupCreatingTimeoutIndex = WifiP2pServiceImpl.sGroupCreatingTimeoutIndex + 1, 0), 120000);
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        return true;
                    case 139274:
                        P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        P2pStateMachine.this.replyToMessage(message, 139276);
                        return true;
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT /*143361*/:
                        if (WifiP2pServiceImpl.sGroupCreatingTimeoutIndex != message.arg1) {
                            return true;
                        }
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("Group negotiation timed out");
                        }
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT /*147478*/:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            return true;
                        }
                        WifiP2pDevice device = message.obj;
                        if (P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(device.deviceAddress)) {
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd("Add device to lost list " + device);
                            }
                            P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                            return true;
                        }
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("mSavedPeerConfig " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress + "device " + device.deviceAddress);
                        }
                        return false;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT /*147481*/:
                        WifiP2pServiceImpl.this.mAutonomousGroup = false;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class GroupNegotiationState extends State {
            GroupNegotiationState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
            }

            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT /*147481*/:
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT /*147483*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " go success");
                            break;
                        }
                        break;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT /*147482*/:
                        if (message.obj == P2pStatus.NO_COMMON_CHANNEL) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                            break;
                        }
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT /*147484*/:
                        if (((P2pStatus) message.obj) == P2pStatus.NO_COMMON_CHANNEL) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                            break;
                        }
                        break;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                        if (message.obj != null) {
                            P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd(getName() + " group started");
                            }
                            if (P2pStateMachine.this.mGroup.isGroupOwner() && WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(P2pStateMachine.this.mGroup.getOwner().deviceAddress)) {
                                P2pStateMachine.this.mGroup.getOwner().deviceAddress = WifiP2pServiceImpl.this.mThisDevice.deviceAddress;
                            }
                            if (P2pStateMachine.this.mGroup.getNetworkId() == -2) {
                                P2pStateMachine.this.updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                                P2pStateMachine.this.mGroup.setNetworkId(P2pStateMachine.this.mGroups.getNetworkId(P2pStateMachine.this.mGroup.getOwner().deviceAddress, P2pStateMachine.this.mGroup.getNetworkName()));
                            }
                            if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                                if (!WifiP2pServiceImpl.this.mAutonomousGroup) {
                                    P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 10);
                                }
                                P2pStateMachine.this.startDhcpServer(P2pStateMachine.this.mGroup.getInterface());
                            } else {
                                P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 10);
                                WifiP2pServiceImpl.this.startIpManager(P2pStateMachine.this.mGroup.getInterface());
                                P2pStateMachine.this.mWifNative.setBluetoothCoexistenceMode(1);
                                P2pStateMachine.this.mIsBTCoexDisabled = true;
                                WifiP2pDevice groupOwner = P2pStateMachine.this.mGroup.getOwner();
                                WifiP2pDevice peer = P2pStateMachine.this.mPeers.get(groupOwner.deviceAddress);
                                if (peer != null) {
                                    groupOwner.updateSupplicantDetails(peer);
                                    P2pStateMachine.this.mPeers.updateStatus(groupOwner.deviceAddress, 0);
                                    P2pStateMachine.this.sendPeersChangedBroadcast();
                                } else {
                                    P2pStateMachine.this.logw("Unknown group owner " + groupOwner);
                                }
                            }
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT /*147486*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " go failure");
                        }
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT /*147488*/:
                        P2pStatus status = (P2pStatus) message.obj;
                        if (status != P2pStatus.SUCCESS) {
                            P2pStateMachine.this.loge("Invitation result " + status);
                            if (status != P2pStatus.UNKNOWN_P2P_GROUP) {
                                if (status != P2pStatus.INFORMATION_IS_CURRENTLY_UNAVAILABLE) {
                                    if (status != P2pStatus.NO_COMMON_CHANNEL) {
                                        P2pStateMachine.this.handleGroupCreationFailure();
                                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                                        break;
                                    }
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                                    break;
                                }
                                P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                break;
                            }
                            int netId = P2pStateMachine.this.mSavedPeerConfig.netId;
                            if (netId >= 0) {
                                if (WifiP2pServiceImpl.DBG) {
                                    P2pStateMachine.this.logd("Remove unknown client from the list");
                                }
                                P2pStateMachine.this.removeClientFromList(netId, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, true);
                            }
                            P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                            P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            break;
                        }
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        class InactiveState extends State {
            InactiveState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                WifiP2pServiceImpl.this.mIsInvite = false;
                P2pStateMachine.this.mSavedPeerConfig.invalidate();
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                WifiP2pConfig config;
                switch (message.what) {
                    case 139268:
                        if (!P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                            P2pStateMachine.this.replyToMessage(message, 139269, 0);
                            break;
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        WifiP2pServiceImpl.this.mServiceDiscReqId = null;
                        P2pStateMachine.this.replyToMessage(message, 139270);
                        break;
                    case 139271:
                        config = message.obj;
                        WifiP2pServiceImpl.this.mP2pConnectFreq = message.arg1;
                        if (WifiP2pServiceImpl.this.mP2pConnectFreq != 0) {
                            WifiP2pServiceImpl.this.mFindTimes = 0;
                            if (config != null) {
                                WifiP2pServiceImpl.this.mIntentToConnectPeer = config.deviceAddress;
                            }
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd(getName() + " sending connect on freq=" + WifiP2pServiceImpl.this.mP2pConnectFreq);
                            }
                        } else if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " sending connect");
                        }
                        if (!P2pStateMachine.this.isConfigInvalid(config)) {
                            WifiP2pServiceImpl.this.mAutonomousGroup = false;
                            P2pStateMachine.this.mWifiNative.p2pStopFind();
                            if (P2pStateMachine.this.reinvokePersistentGroup(config)) {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            } else {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mProvisionDiscoveryState);
                            }
                            P2pStateMachine.this.mSavedPeerConfig = config;
                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            P2pStateMachine.this.replyToMessage(message, 139273);
                            break;
                        }
                        P2pStateMachine.this.loge("Dropping connect requeset " + config);
                        P2pStateMachine.this.replyToMessage(message, 139272);
                        break;
                    case 139277:
                        boolean ret;
                        WifiP2pServiceImpl.this.mAutonomousGroup = true;
                        if (message.arg1 == -2) {
                            int netId = P2pStateMachine.this.mGroups.getNetworkId(WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                            if (netId != -1) {
                                ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(netId);
                            } else {
                                ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(true);
                            }
                        } else {
                            ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(false);
                        }
                        if (!ret) {
                            P2pStateMachine.this.replyToMessage(message, 139278, 0);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139279);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case 139329:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " start listen mode");
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        if (!P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                            P2pStateMachine.this.replyToMessage(message, 139330);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139331);
                        break;
                    case 139332:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " stop listen mode");
                        }
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(false, 0, 0)) {
                            P2pStateMachine.this.replyToMessage(message, 139334);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139333);
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        break;
                    case 139335:
                        if (message.obj != null) {
                            Bundle p2pChannels = message.obj;
                            int lc = p2pChannels.getInt("lc", 0);
                            int oc = p2pChannels.getInt("oc", 0);
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd(getName() + " set listen and operating channel");
                            }
                            if (!P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                                P2pStateMachine.this.replyToMessage(message, 139336);
                                break;
                            }
                            P2pStateMachine.this.replyToMessage(message, 139337);
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments(s)");
                        break;
                    case 139342:
                        String handoverSelect = null;
                        if (message.obj != null) {
                            handoverSelect = ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE");
                        }
                        if (handoverSelect != null && P2pStateMachine.this.mWifiNative.initiatorReportNfcHandover(handoverSelect)) {
                            P2pStateMachine.this.replyToMessage(message, 139344);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139345);
                        break;
                        break;
                    case 139343:
                        String handoverRequest = null;
                        if (message.obj != null) {
                            handoverRequest = ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE");
                        }
                        if (handoverRequest != null && P2pStateMachine.this.mWifiNative.responderReportNfcHandover(handoverRequest)) {
                            P2pStateMachine.this.replyToMessage(message, 139344);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139345);
                        break;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT /*147479*/:
                        config = (WifiP2pConfig) message.obj;
                        if (!P2pStateMachine.this.isConfigInvalid(config)) {
                            P2pStateMachine.this.mSavedPeerConfig = config;
                            WifiP2pServiceImpl.this.mAutonomousGroup = false;
                            WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                            break;
                        }
                        P2pStateMachine.this.loge("Dropping GO neg request " + config);
                        break;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                        if (message.obj != null) {
                            P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd(getName() + " group started");
                            }
                            if (P2pStateMachine.this.mGroup.isGroupOwner() && WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(P2pStateMachine.this.mGroup.getOwner().deviceAddress)) {
                                P2pStateMachine.this.mGroup.getOwner().deviceAddress = WifiP2pServiceImpl.this.mThisDevice.deviceAddress;
                            }
                            if (P2pStateMachine.this.mGroup.getNetworkId() != -2) {
                                P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                                P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                break;
                            }
                            WifiP2pServiceImpl.this.mAutonomousGroup = false;
                            P2pStateMachine.this.deferMessage(message);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                        break;
                    case WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT /*147487*/:
                        if (message.obj != null) {
                            WifiP2pGroup group = message.obj;
                            WifiP2pDevice owner = group.getOwner();
                            if (owner == null) {
                                int id = group.getNetworkId();
                                if (id >= 0) {
                                    String addr = P2pStateMachine.this.mGroups.getOwnerAddr(id);
                                    if (addr == null) {
                                        P2pStateMachine.this.loge("Ignored invitation from null owner");
                                        break;
                                    }
                                    group.setOwner(new WifiP2pDevice(addr));
                                    owner = group.getOwner();
                                } else {
                                    P2pStateMachine.this.loge("Ignored invitation from null owner");
                                    break;
                                }
                            }
                            config = new WifiP2pConfig();
                            config.deviceAddress = group.getOwner().deviceAddress;
                            if (!P2pStateMachine.this.isConfigInvalid(config)) {
                                P2pStateMachine.this.mSavedPeerConfig = config;
                                if (owner != null) {
                                    owner = P2pStateMachine.this.mPeers.get(owner.deviceAddress);
                                    if (owner != null) {
                                        if (owner.wpsPbcSupported()) {
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                        } else if (owner.wpsKeypadSupported()) {
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                                        } else if (owner.wpsDisplaySupported()) {
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                                        }
                                    }
                                }
                                WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                WifiP2pServiceImpl.this.mJoinExistingGroup = true;
                                WifiP2pServiceImpl.this.mIsInvite = true;
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingInviteRequestState);
                                break;
                            }
                            P2pStateMachine.this.loge("Dropping invitation request " + config);
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT /*147489*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT /*147491*/:
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT /*147492*/:
                        WifiP2pProvDiscEvent provDisc = message.obj;
                        WifiP2pDevice device = provDisc.device;
                        if (device != null) {
                            P2pStateMachine.this.notifyP2pProvDiscShowPinRequest(provDisc.pin, device.deviceAddress);
                            P2pStateMachine.this.mPeers.updateStatus(device.deviceAddress, 1);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            break;
                        }
                        Slog.d(WifiP2pServiceImpl.TAG, "Device entry is null");
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        class OngoingGroupRemovalState extends State {
            OngoingGroupRemovalState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139282);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class P2pDisabledState extends State {
            P2pDisabledState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P /*131203*/:
                        try {
                            WifiP2pServiceImpl.this.mNwService.setInterfaceUp(P2pStateMachine.this.mWifiNative.getInterfaceName());
                        } catch (RemoteException re) {
                            P2pStateMachine.this.loge("Unable to change interface settings: " + re);
                        } catch (IllegalStateException ie) {
                            P2pStateMachine.this.loge("Unable to change interface settings: " + ie);
                        }
                        P2pStateMachine.this.mWifiMonitor.startMonitoring(P2pStateMachine.this.mWifiNative.getInterfaceName());
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pEnablingState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class P2pDisablingState extends State {
            P2pDisablingState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT, WifiP2pServiceImpl.sDisableP2pTimeoutIndex = WifiP2pServiceImpl.sDisableP2pTimeoutIndex + 1, 0), 5000);
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P /*131203*/:
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ /*131204*/:
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT /*143366*/:
                        if (WifiP2pServiceImpl.sDisableP2pTimeoutIndex == message.arg1) {
                            P2pStateMachine.this.loge("P2p disable timed out");
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                            break;
                        }
                        break;
                    case 147458:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("p2p socket connection lost");
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
                if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                    WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
                } else {
                    P2pStateMachine.this.loge("P2pDisablingState exit(): WifiChannel is null");
                }
            }
        }

        class P2pEnabledState extends State {
            P2pEnabledState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                P2pStateMachine.this.sendP2pStateChangedBroadcast(true);
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(true);
                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                P2pStateMachine.this.initializeP2pSettings();
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                WifiP2pServiceInfo servInfo;
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P /*131203*/:
                        break;
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ /*131204*/:
                        if (P2pStateMachine.this.mPeers.clear()) {
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        if (P2pStateMachine.this.mGroups.clear()) {
                            P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
                        }
                        P2pStateMachine.this.mWifiMonitor.stopMonitoring(P2pStateMachine.this.mWifiNative.getInterfaceName());
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisablingState);
                        break;
                    case 139265:
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked && (WifiP2pServiceImpl.this.isNfcTriggered ^ 1) != 0) {
                            P2pStateMachine.this.replyToMessage(message, 139266, 2);
                            break;
                        }
                        boolean p2pFindSetDown;
                        P2pStateMachine.this.clearSupplicantServiceRequest();
                        P2pStateMachine.this.logd(getName() + ",DISCOVER_PEERS on freq=" + message.arg1);
                        boolean specifyFreq = false;
                        WifiP2pServiceImpl.this.mFoundTargetDevice = false;
                        if (message.arg1 == 0) {
                            p2pFindSetDown = P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S);
                        } else if (message.arg1 == -1) {
                            specifyFreq = true;
                            p2pFindSetDown = P2pStateMachine.this.mWifiNative.p2pFind(30, 5745);
                        } else {
                            specifyFreq = true;
                            p2pFindSetDown = P2pStateMachine.this.mWifiNative.p2pFind(30, message.arg1);
                        }
                        if (!p2pFindSetDown) {
                            P2pStateMachine.this.replyToMessage(message, 139266, 0);
                            break;
                        }
                        if (specifyFreq && (WifiP2pServiceImpl.this.mIntentToConnectPeer.isEmpty() ^ 1) != 0) {
                            WifiP2pServiceImpl wifiP2pServiceImpl = WifiP2pServiceImpl.this;
                            wifiP2pServiceImpl.mFindTimes = wifiP2pServiceImpl.mFindTimes + 1;
                            P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.P2P_FIND_SPECIAL_FREQ_TIME_OUT, message.arg1, 0), WifiMetrics.TIMEOUT_RSSI_DELTA_MILLIS);
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd("mIntentToConnectPeer=" + WifiP2pServiceImpl.this.mIntentToConnectPeer + " mFindTimes=" + WifiP2pServiceImpl.this.mFindTimes);
                            }
                        }
                        P2pStateMachine.this.replyToMessage(message, 139267);
                        P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(true);
                        break;
                    case 139268:
                        if (!P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                            P2pStateMachine.this.replyToMessage(message, 139269, 0);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139270);
                        break;
                    case 139292:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " add service");
                        }
                        servInfo = message.obj;
                        if (!P2pStateMachine.this.addLocalService(message.replyTo, servInfo)) {
                            P2pStateMachine.this.replyToMessage(message, 139293);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139294);
                        break;
                    case 139295:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " remove service");
                        }
                        servInfo = (WifiP2pServiceInfo) message.obj;
                        P2pStateMachine.this.removeLocalService(message.replyTo, servInfo);
                        P2pStateMachine.this.replyToMessage(message, 139297);
                        break;
                    case 139298:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " clear service");
                        }
                        P2pStateMachine.this.clearLocalServices(message.replyTo);
                        P2pStateMachine.this.replyToMessage(message, 139300);
                        break;
                    case 139301:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " add service request");
                        }
                        if (!P2pStateMachine.this.addServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj)) {
                            P2pStateMachine.this.replyToMessage(message, 139302);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139303);
                        break;
                    case 139304:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " remove service request");
                        }
                        P2pStateMachine.this.removeServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj);
                        P2pStateMachine.this.replyToMessage(message, 139306);
                        break;
                    case 139307:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " clear service request");
                        }
                        P2pStateMachine.this.clearServiceRequests(message.replyTo);
                        P2pStateMachine.this.replyToMessage(message, 139309);
                        break;
                    case 139310:
                        if (!WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd(getName() + " discover services");
                            }
                            if (P2pStateMachine.this.updateSupplicantServiceRequest()) {
                                if (!P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S)) {
                                    P2pStateMachine.this.replyToMessage(message, 139311, 0);
                                    break;
                                }
                                P2pStateMachine.this.replyToMessage(message, 139312);
                                break;
                            }
                            P2pStateMachine.this.replyToMessage(message, 139311, 3);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139311, 2);
                        break;
                    case 139315:
                        WifiP2pDevice d = message.obj;
                        if (d != null && P2pStateMachine.this.setAndPersistDeviceName(d.deviceName)) {
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd("set device name " + d.deviceName);
                            }
                            P2pStateMachine.this.replyToMessage(message, 139317);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139316, 0);
                        break;
                    case 139318:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " delete persistent group");
                        }
                        P2pStateMachine.this.mGroups.remove(message.arg1);
                        P2pStateMachine.this.replyToMessage(message, 139320);
                        break;
                    case 139323:
                        WifiP2pWfdInfo d2 = message.obj;
                        if (WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            if (d2 != null && P2pStateMachine.this.setWfdInfo(d2)) {
                                P2pStateMachine.this.replyToMessage(message, 139325);
                                break;
                            }
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139324, 0);
                        break;
                        break;
                    case 139329:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " start listen mode");
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        if (!P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                            P2pStateMachine.this.replyToMessage(message, 139330);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139331);
                        break;
                    case 139332:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " stop listen mode");
                        }
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(false, 0, 0)) {
                            P2pStateMachine.this.replyToMessage(message, 139334);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139333);
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        break;
                    case 139335:
                        Bundle p2pChannels = message.obj;
                        int lc = p2pChannels.getInt("lc", 0);
                        int oc = p2pChannels.getInt("oc", 0);
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " set listen and operating channel");
                        }
                        if (!P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                            P2pStateMachine.this.replyToMessage(message, 139336);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139337);
                        break;
                    case 139339:
                        Bundle requestBundle = new Bundle();
                        requestBundle.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverRequest());
                        P2pStateMachine.this.replyToMessage(message, 139341, (Object) requestBundle);
                        break;
                    case 139340:
                        Bundle selectBundle = new Bundle();
                        selectBundle.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverSelect());
                        P2pStateMachine.this.replyToMessage(message, 139341, (Object) selectBundle);
                        break;
                    case 139361:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " ADD_PERSISTENT_GROUP");
                        }
                        HashMap<String, String> hVariables = (HashMap) message.obj.getSerializable("variables");
                        if (hVariables == null) {
                            P2pStateMachine.this.replyToMessage(message, 139362, 0);
                            break;
                        }
                        WifiP2pGroup group = P2pStateMachine.this.addPersistentGroup(hVariables);
                        P2pStateMachine.this.replyToMessage(message, 139364, (Object) new WifiP2pGroup(group));
                        break;
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE /*143374*/:
                        P2pStateMachine.this.mWifiNative.setMiracastMode(message.arg1);
                        break;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY /*143375*/:
                        boolean blocked;
                        if (message.arg1 == 1) {
                            blocked = true;
                        } else {
                            blocked = false;
                        }
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked != blocked) {
                            WifiP2pServiceImpl.this.mDiscoveryBlocked = blocked;
                            if (blocked && WifiP2pServiceImpl.this.mDiscoveryStarted) {
                                P2pStateMachine.this.mWifiNative.p2pStopFind();
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = true;
                            }
                            if (!blocked && WifiP2pServiceImpl.this.mDiscoveryPostponed) {
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                                P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S);
                            }
                        }
                        if (blocked) {
                            if (message.obj != null) {
                                try {
                                    message.obj.sendMessage(message.arg2);
                                    break;
                                } catch (Exception e) {
                                    P2pStateMachine.this.loge("unable to send BLOCK_DISCOVERY response: " + e);
                                    break;
                                }
                            }
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            break;
                        }
                        break;
                    case WifiP2pServiceImpl.AUTO_ACCEPT_INVITATION_TIME_OUT /*143376*/:
                        if (WifiP2pServiceImpl.this.mFastConTriggeredNum == message.arg1) {
                            if (WifiP2pServiceImpl.DBG) {
                                P2pStateMachine.this.logd("FastConTrigger: " + message.arg1 + "/" + WifiP2pServiceImpl.this.mFastConTriggeredNum);
                            }
                            WifiP2pServiceImpl.this.setNfcTriggered(false);
                            break;
                        }
                        break;
                    case WifiP2pServiceImpl.P2P_FIND_SPECIAL_FREQ_TIME_OUT /*143377*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("P2P_FIND_SPECIAL_FREQ_TIME_OUT:" + WifiP2pServiceImpl.this.mFoundTargetDevice + ", mFindTimes=" + WifiP2pServiceImpl.this.mFindTimes);
                        }
                        if (!WifiP2pServiceImpl.this.mFoundTargetDevice && WifiP2pServiceImpl.this.mFindTimes < 5) {
                            P2pStateMachine.this.sendMessage(139265, message.arg1);
                            break;
                        }
                        WifiP2pServiceImpl.this.mFindTimes = 0;
                        WifiP2pServiceImpl.this.mFoundTargetDevice = false;
                        break;
                    case 147458:
                        P2pStateMachine.this.loge("Unexpected loss of p2p socket connection");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        break;
                    case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT /*147477*/:
                        if (message.obj != null) {
                            WifiP2pDevice device = message.obj;
                            if (!WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(device.deviceAddress)) {
                                if (WifiP2pServiceImpl.DBG) {
                                    P2pStateMachine.this.logd("Found a Device=" + device.deviceAddress + ", mIntentToConnectPeer=" + WifiP2pServiceImpl.this.mIntentToConnectPeer);
                                }
                                if (!WifiP2pServiceImpl.this.mIntentToConnectPeer.isEmpty() && WifiP2pServiceImpl.this.mIntentToConnectPeer.equalsIgnoreCase(device.deviceAddress)) {
                                    if (WifiP2pServiceImpl.DBG) {
                                        P2pStateMachine.this.logd("mFoundTargetDevice true!");
                                    }
                                    WifiP2pServiceImpl.this.mFoundTargetDevice = true;
                                    WifiP2pServiceImpl.this.mIntentToConnectPeer = "";
                                    P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_FIND_SPECIAL_FREQ_TIME_OUT);
                                }
                                P2pStateMachine.this.mPeers.updateSupplicantDetails(device);
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                break;
                            }
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                        break;
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT /*147478*/:
                        if (message.obj != null) {
                            if (P2pStateMachine.this.mPeers.remove(((WifiP2pDevice) message.obj).deviceAddress) != null) {
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                break;
                            }
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                        break;
                    case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT /*147493*/:
                        P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                        break;
                    case WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT /*147494*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd(getName() + " receive service response");
                        }
                        if (message.obj != null) {
                            for (WifiP2pServiceResponse resp : message.obj) {
                                resp.setSrcDevice(P2pStateMachine.this.mPeers.get(resp.getSrcDevice().deviceAddress));
                                P2pStateMachine.this.sendServiceResponse(resp);
                            }
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
                P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                P2pStateMachine.this.sendP2pStateChangedBroadcast(false);
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(false);
            }
        }

        class P2pEnablingState extends State {
            P2pEnablingState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P /*131203*/:
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ /*131204*/:
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case 147457:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("P2p socket connection successful");
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    case 147458:
                        P2pStateMachine.this.loge("P2p socket connection failed");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        class P2pNotSupportedState extends State {
            P2pNotSupportedState() {
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 1);
                        break;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 1);
                        break;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 1);
                        break;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 1);
                        break;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 1);
                        break;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 1);
                        break;
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 1);
                        break;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 1);
                        break;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 1);
                        break;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 1);
                        break;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 1);
                        break;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 1);
                        break;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 1);
                        break;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 1);
                        break;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139318, 1);
                        break;
                    case 139323:
                        if (!WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139324, 1);
                        break;
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 1);
                        break;
                    case 139329:
                        P2pStateMachine.this.replyToMessage(message, 139330, 1);
                        break;
                    case 139332:
                        P2pStateMachine.this.replyToMessage(message, 139333, 1);
                        break;
                    case 139361:
                        P2pStateMachine.this.replyToMessage(message, 139361, 1);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        class ProvisionDiscoveryState extends State {
            ProvisionDiscoveryState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                if (P2pStateMachine.this.mSavedPeerConfig.wps.setup != 0) {
                    P2pStateMachine.this.logd("Force wps=" + P2pStateMachine.this.mSavedPeerConfig.wps.setup + " to pbc ");
                    P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                }
                P2pStateMachine.this.mWifiNative.p2pProvisionDiscovery(P2pStateMachine.this.mSavedPeerConfig);
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                WifiP2pDevice device;
                switch (message.what) {
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_RSP_EVENT /*147490*/:
                        if (message.obj != null) {
                            device = message.obj.device;
                            if ((device == null || (device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress) ^ 1) == 0) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                                if (WifiP2pServiceImpl.DBG) {
                                    P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                }
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                break;
                            }
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT /*147491*/:
                        if (message.obj != null) {
                            device = message.obj.device;
                            if ((device == null || (device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress) ^ 1) == 0) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 2) {
                                if (WifiP2pServiceImpl.DBG) {
                                    P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                }
                                if (!TextUtils.isEmpty(P2pStateMachine.this.mSavedPeerConfig.wps.pin)) {
                                    P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                    break;
                                }
                                WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                                break;
                            }
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT /*147492*/:
                        if (message.obj != null) {
                            WifiP2pProvDiscEvent provDisc = message.obj;
                            device = provDisc.device;
                            if (device != null) {
                                if (device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 1) {
                                    if (WifiP2pServiceImpl.DBG) {
                                        P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                    }
                                    P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc.pin;
                                    P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                    P2pStateMachine.this.notifyInvitationSent(provDisc.pin, device.deviceAddress);
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                    break;
                                }
                            }
                            Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT /*147495*/:
                        P2pStateMachine.this.loge("provision discovery failed");
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        class UserAuthorizingInviteRequestState extends State {
            UserAuthorizingInviteRequestState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                P2pStateMachine.this.notifyInvitationReceived();
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        if (!P2pStateMachine.this.reinvokePersistentGroup(P2pStateMachine.this.mSavedPeerConfig)) {
                            P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        }
                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("User rejected invitation " + P2pStateMachine.this.mSavedPeerConfig);
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
            }
        }

        class UserAuthorizingJoinState extends State {
            UserAuthorizingJoinState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                P2pStateMachine.this.notifyInvitationReceived();
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                            P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                        } else {
                            P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), P2pStateMachine.this.mSavedPeerConfig.wps.pin);
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        break;
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("User rejected incoming request");
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT /*147489*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT /*147491*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT /*147492*/:
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
            }
        }

        class UserAuthorizingNegotiationRequestState extends State {
            UserAuthorizingNegotiationRequestState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logd(getName());
                }
                P2pStateMachine.this.notifyInvitationReceived();
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.DBG) {
                    P2pStateMachine.this.logStateAndMessage(message, this);
                }
                switch (message.what) {
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        P2pStateMachine.this.mWifiInjector.getOppoWifiSharingManager().stopWifiSharingTetheringIfNeed();
                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                        if (WifiP2pServiceImpl.DBG) {
                            P2pStateMachine.this.logd("User rejected negotiation " + P2pStateMachine.this.mSavedPeerConfig);
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
            }
        }

        private void logStateAndMessage(Message message, State state) {
            StringBuilder b = new StringBuilder();
            if (message != null) {
                b.append("{ what=");
                b.append(message.what);
                if (message.arg1 != 0) {
                    b.append(" arg1=");
                    b.append(message.arg1);
                }
                if (message.arg2 != 0) {
                    b.append(" arg2=");
                    b.append(message.arg2);
                }
                if (message.obj != null) {
                    b.append(" obj=");
                    b.append(message.obj.getClass().getSimpleName());
                }
                b.append(" }");
            }
            logd(" " + state.getClass().getSimpleName() + " " + b.toString());
        }

        P2pStateMachine(String name, Looper looper, boolean p2pSupported) {
            super(name, looper);
            addState(this.mDefaultState);
            addState(this.mP2pNotSupportedState, this.mDefaultState);
            addState(this.mP2pDisablingState, this.mDefaultState);
            addState(this.mP2pDisabledState, this.mDefaultState);
            addState(this.mP2pEnablingState, this.mDefaultState);
            addState(this.mP2pEnabledState, this.mDefaultState);
            addState(this.mInactiveState, this.mP2pEnabledState);
            addState(this.mGroupCreatingState, this.mP2pEnabledState);
            addState(this.mUserAuthorizingInviteRequestState, this.mGroupCreatingState);
            addState(this.mUserAuthorizingNegotiationRequestState, this.mGroupCreatingState);
            addState(this.mProvisionDiscoveryState, this.mGroupCreatingState);
            addState(this.mGroupNegotiationState, this.mGroupCreatingState);
            addState(this.mFrequencyConflictState, this.mGroupCreatingState);
            addState(this.mGroupCreatedState, this.mP2pEnabledState);
            addState(this.mUserAuthorizingJoinState, this.mGroupCreatedState);
            addState(this.mOngoingGroupRemovalState, this.mGroupCreatedState);
            if (p2pSupported) {
                setInitialState(this.mP2pDisabledState);
            } else {
                setInitialState(this.mP2pNotSupportedState);
            }
            setLogRecSize(50);
            setLogOnlyTransitions(true);
            String interfaceName = this.mWifiNative.getInterfaceName();
            this.mWifiMonitor.registerHandler(interfaceName, 147498, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, 147497, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_DEVICE_LOST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_FIND_STOPPED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_GROUP_STARTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_PROV_DISC_PBC_RSP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, 147457, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, 147458, getHandler());
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            super.dump(fd, pw, args);
            pw.println("mWifiP2pInfo " + this.mWifiP2pInfo);
            pw.println("mGroup " + this.mGroup);
            pw.println("mSavedPeerConfig " + this.mSavedPeerConfig);
            pw.println("mGroups" + this.mGroups);
            pw.println();
        }

        private void sendP2pStateChangedBroadcast(boolean enabled) {
            Intent intent = new Intent("android.net.wifi.p2p.STATE_CHANGED");
            intent.addFlags(67108864);
            if (enabled) {
                intent.putExtra("wifi_p2p_state", 2);
            } else {
                intent.putExtra("wifi_p2p_state", 1);
            }
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pDiscoveryChangedBroadcast(boolean started) {
            if (WifiP2pServiceImpl.this.mDiscoveryStarted != started) {
                int i;
                WifiP2pServiceImpl.this.mDiscoveryStarted = started;
                if (WifiP2pServiceImpl.DBG) {
                    logd("discovery change broadcast " + started);
                }
                Intent intent = new Intent("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
                intent.addFlags(67108864);
                String str = "discoveryState";
                if (started) {
                    i = 2;
                } else {
                    i = 1;
                }
                intent.putExtra(str, i);
                WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            }
        }

        private void sendThisDeviceChangedBroadcast() {
            Intent intent = new Intent("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
            intent.addFlags(67108864);
            intent.putExtra("wifiP2pDevice", new WifiP2pDevice(WifiP2pServiceImpl.this.mThisDevice));
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendPeersChangedBroadcast() {
            Intent intent = new Intent("android.net.wifi.p2p.PEERS_CHANGED");
            intent.putExtra("wifiP2pDeviceList", new WifiP2pDeviceList(this.mPeers));
            intent.addFlags(67108864);
            WifiP2pServiceImpl.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pConnectionChangedBroadcast() {
            if (WifiP2pServiceImpl.DBG) {
                logd("sending p2p connection changed broadcast");
            }
            Intent intent = new Intent("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("wifiP2pInfo", new WifiP2pInfo(this.mWifiP2pInfo));
            intent.putExtra("networkInfo", new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
            intent.putExtra("p2pGroupInfo", new WifiP2pGroup(this.mGroup));
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.P2P_CONNECTION_CHANGED, new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
            } else {
                loge("sendP2pConnectionChangedBroadcast(): WifiChannel is null");
            }
        }

        private void sendP2pPersistentGroupsChangedBroadcast() {
            if (WifiP2pServiceImpl.DBG) {
                logd("sending p2p persistent groups changed broadcast");
            }
            Intent intent = new Intent("android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED");
            intent.addFlags(67108864);
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void startDhcpServer(String intf) {
            try {
                InterfaceConfiguration ifcg = WifiP2pServiceImpl.this.mNwService.getInterfaceConfig(intf);
                ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS), 24));
                ifcg.setInterfaceUp();
                WifiP2pServiceImpl.this.mNwService.setInterfaceConfig(intf, ifcg);
                String[] tetheringDhcpRanges = ((ConnectivityManager) WifiP2pServiceImpl.this.mContext.getSystemService("connectivity")).getTetheredDhcpRanges();
                if (WifiP2pServiceImpl.this.mNwService.isTetheringStarted()) {
                    if (WifiP2pServiceImpl.DBG) {
                        logd("Stop existing tethering and restart it");
                    }
                    WifiP2pServiceImpl.this.mNwService.stopTethering();
                }
                WifiP2pServiceImpl.this.mNwService.tetherInterface(intf);
                WifiP2pServiceImpl.this.mNwService.startTethering(tetheringDhcpRanges);
                logd("Started Dhcp server on " + intf);
            } catch (Exception e) {
                loge("Error configuring interface " + intf + ", :" + e);
            }
        }

        private void stopDhcpServer(String intf) {
            try {
                WifiP2pServiceImpl.this.mNwService.untetherInterface(intf);
                for (String temp : WifiP2pServiceImpl.this.mNwService.listTetheredInterfaces()) {
                    logd("List all interfaces " + temp);
                    if (temp.compareTo(intf) != 0) {
                        logd("Found other tethering interfaces, so keep tethering alive");
                        return;
                    }
                }
                WifiP2pServiceImpl.this.mNwService.stopTethering();
                logd("Stopped Dhcp server");
            } catch (Exception e) {
                loge("Error stopping Dhcp server" + e);
            } finally {
                logd("Stopped Dhcp server");
            }
        }

        private void notifyP2pEnableFailure() {
            Resources r = Resources.getSystem();
            AlertDialog dialog = new Builder(WifiP2pServiceImpl.this.mContext, 201523207).setTitle(r.getString(17041126)).setPositiveButton(r.getString(17041133), null).create();
            dialog.setCancelable(false);
            LayoutParams p = dialog.getWindow().getAttributes();
            p.ignoreHomeMenuKey = 1;
            dialog.getWindow().setAttributes(p);
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setType(2003);
            LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
            TextView msg = (TextView) dialog.findViewById(16908299);
            if (msg != null) {
                msg.setGravity(17);
            } else {
                loge("textview is null");
            }
        }

        private void addRowToDialog(ViewGroup group, int stringId, String value) {
            Resources r = Resources.getSystem();
            View row = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367313, group, false);
            ((TextView) row.findViewById(16909067)).setText(r.getString(stringId));
            ((TextView) row.findViewById(16909444)).setText(value);
            group.addView(row);
        }

        private void notifyInvitationSent(String pin, String peerAddress) {
            Resources r = Resources.getSystem();
            View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367312, null);
            ViewGroup group = (ViewGroup) textEntryView.findViewById(16908960);
            addRowToDialog(group, 17041135, getDeviceName(peerAddress));
            addRowToDialog(group, 17041134, pin);
            AlertDialog dialog = new Builder(WifiP2pServiceImpl.this.mContext, 201523207).setTitle(r.getString(17041130)).setView(textEntryView).setPositiveButton(r.getString(17039370), null).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setType(2003);
            LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
        }

        private void notifyP2pProvDiscShowPinRequest(final String pin, final String peerAddress) {
            Resources r = Resources.getSystem();
            String tempDevAddress = peerAddress;
            String tempPin = pin;
            View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367312, null);
            ViewGroup group = (ViewGroup) textEntryView.findViewById(16908960);
            addRowToDialog(group, 17041135, getDeviceName(peerAddress));
            addRowToDialog(group, 17041134, pin);
            AlertDialog dialog = new Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(17041130)).setView(textEntryView).setPositiveButton(r.getString(17039430), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                    P2pStateMachine.this.mSavedPeerConfig.deviceAddress = peerAddress;
                    P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                    P2pStateMachine.this.mSavedPeerConfig.wps.pin = pin;
                    P2pStateMachine.this.mWifiNative.p2pConnect(P2pStateMachine.this.mSavedPeerConfig, WifiP2pServiceImpl.FORM_GROUP.booleanValue());
                }
            }).setCancelable(false).create();
            dialog.getWindow().setType(2003);
            LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
        }

        private void notifyInvitationReceived() {
            Resources r = Resources.getSystem();
            final WpsInfo wps = this.mSavedPeerConfig.wps;
            View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367312, null);
            ViewGroup group = (ViewGroup) textEntryView.findViewById(16908960);
            addRowToDialog(group, 17041129, getDeviceName(this.mSavedPeerConfig.deviceAddress));
            final EditText pin = (EditText) textEntryView.findViewById(16909463);
            if (WifiP2pServiceImpl.this.isNfcTriggered) {
                if (wps.setup == 2) {
                    this.mSavedPeerConfig.wps.pin = pin.getText().toString();
                }
                logd(getName() + "NFC mode accept invitation " + this.mSavedPeerConfig);
                sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                switch (wps.setup) {
                    case 1:
                        if (WifiP2pServiceImpl.DBG) {
                            logd("Shown pin section visible");
                        }
                        addRowToDialog(group, 17041134, wps.pin);
                        return;
                    case 2:
                        if (WifiP2pServiceImpl.DBG) {
                            logd("Enter pin section visible");
                        }
                        textEntryView.findViewById(16908846).setVisibility(0);
                        return;
                    default:
                        return;
                }
            }
            Builder builder = new Builder(WifiP2pServiceImpl.this.mContext);
            if (wps.setup == 2) {
                builder.setTitle(r.getString(17041131));
                builder.setView(textEntryView);
            } else {
                builder.setTitle(r.getString(17041132, new Object[]{getDeviceName(this.mSavedPeerConfig.deviceAddress)}));
            }
            AlertDialog dialog = builder.setPositiveButton(r.getString(17039430), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (wps.setup == 2) {
                        P2pStateMachine.this.mSavedPeerConfig.wps.pin = pin.getText().toString();
                    }
                    if (WifiP2pServiceImpl.DBG) {
                        P2pStateMachine.this.logd(P2pStateMachine.this.getName() + " accept invitation " + P2pStateMachine.this.mSavedPeerConfig);
                    }
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                }
            }).setNegativeButton(r.getString(17039360), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (WifiP2pServiceImpl.DBG) {
                        P2pStateMachine.this.logd(P2pStateMachine.this.getName() + " ignore connect");
                    }
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }).setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface arg0) {
                    if (WifiP2pServiceImpl.DBG) {
                        P2pStateMachine.this.logd(P2pStateMachine.this.getName() + " ignore connect");
                    }
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }).create();
            dialog.setCanceledOnTouchOutside(false);
            switch (wps.setup) {
                case 1:
                    if (WifiP2pServiceImpl.DBG) {
                        logd("Shown pin section visible");
                    }
                    addRowToDialog(group, 17041134, wps.pin);
                    break;
                case 2:
                    if (WifiP2pServiceImpl.DBG) {
                        logd("Enter pin section visible");
                    }
                    textEntryView.findViewById(16908846).setVisibility(0);
                    break;
            }
            if ((r.getConfiguration().uiMode & 5) == 5) {
                dialog.setOnKeyListener(new OnKeyListener() {
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode != 164) {
                            return false;
                        }
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                        dialog.dismiss();
                        return true;
                    }
                });
            }
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            LayoutParams p = dialog.getWindow().getAttributes();
            p.ignoreHomeMenuKey = 1;
            dialog.getWindow().setAttributes(p);
            dialog.getWindow().setType(2003);
            LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
        }

        private void updatePersistentNetworks(boolean reload) {
            if (reload) {
                this.mGroups.clear();
            }
            if (this.mWifiNative.p2pListNetworks(this.mGroups) || reload) {
                for (WifiP2pGroup group : this.mGroups.getGroupList()) {
                    if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(group.getOwner().deviceAddress)) {
                        group.setOwner(WifiP2pServiceImpl.this.mThisDevice);
                    }
                }
                this.mWifiNative.saveConfig();
                sendP2pPersistentGroupsChangedBroadcast();
            }
        }

        private boolean isConfigInvalid(WifiP2pConfig config) {
            if (config == null || TextUtils.isEmpty(config.deviceAddress) || this.mPeers.get(config.deviceAddress) == null) {
                return true;
            }
            return false;
        }

        private WifiP2pDevice fetchCurrentDeviceDetails(WifiP2pConfig config) {
            if (config == null) {
                return null;
            }
            this.mPeers.updateGroupCapability(config.deviceAddress, this.mWifiNative.getGroupCapability(config.deviceAddress));
            return this.mPeers.get(config.deviceAddress);
        }

        private void p2pConnectWithPinDisplay(WifiP2pConfig config) {
            if (config == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return;
            }
            WifiP2pDevice dev = fetchCurrentDeviceDetails(config);
            if (dev == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                return;
            }
            boolean join;
            String pin;
            if (WifiP2pServiceImpl.this.mIsInvite) {
                join = true;
            } else {
                join = dev.isGroupOwner();
            }
            if (!WifiP2pServiceImpl.this.isNfcTriggered || WifiP2pServiceImpl.this.mP2pConnectFreq == 0) {
                pin = this.mWifiNative.p2pConnect(config, join);
            } else {
                pin = this.mWifiNative.p2pConnect(config, join, WifiP2pServiceImpl.this.mP2pConnectFreq);
            }
            try {
                Integer.parseInt(pin);
                notifyInvitationSent(pin, config.deviceAddress);
            } catch (NumberFormatException e) {
            }
            WifiP2pServiceImpl.this.mIsInvite = false;
        }

        private boolean reinvokePersistentGroup(WifiP2pConfig config) {
            if (config == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return false;
            }
            WifiP2pDevice dev = fetchCurrentDeviceDetails(config);
            if (dev == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                return false;
            }
            int netId;
            boolean join = dev.isGroupOwner();
            String ssid = this.mWifiNative.p2pGetSsid(dev.deviceAddress);
            if (WifiP2pServiceImpl.DBG) {
                logd("target ssid is " + ssid + " join:" + join);
            }
            if (join && dev.isGroupLimit()) {
                if (WifiP2pServiceImpl.DBG) {
                    logd("target device reaches group limit.");
                }
                join = false;
            } else if (join) {
                netId = this.mGroups.getNetworkId(dev.deviceAddress, ssid);
                if (netId >= 0) {
                    return this.mWifiNative.p2pGroupAdd(netId);
                }
            }
            if (join || !dev.isDeviceLimit()) {
                if (!join && dev.isInvitationCapable()) {
                    netId = -2;
                    if (config.netId < 0) {
                        netId = this.mGroups.getNetworkId(dev.deviceAddress);
                    } else if (config.deviceAddress.equals(this.mGroups.getOwnerAddr(config.netId))) {
                        netId = config.netId;
                    }
                    if (netId < 0) {
                        netId = getNetworkIdFromClientList(dev.deviceAddress);
                    }
                    if (WifiP2pServiceImpl.DBG) {
                        logd("netId related with " + dev.deviceAddress + " = " + netId);
                    }
                    if (netId >= 0) {
                        if (this.mWifiNative.p2pReinvoke(netId, dev.deviceAddress)) {
                            config.netId = netId;
                            return true;
                        }
                        loge("p2pReinvoke() failed, update networks");
                        updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                        return false;
                    }
                }
                return false;
            }
            loge("target device reaches the device limit.");
            return false;
        }

        private int getNetworkIdFromClientList(String deviceAddress) {
            if (deviceAddress == null) {
                return -1;
            }
            for (WifiP2pGroup group : this.mGroups.getGroupList()) {
                int netId = group.getNetworkId();
                String[] p2pClientList = getClientList(netId);
                if (p2pClientList != null) {
                    for (String client : p2pClientList) {
                        if (deviceAddress.equalsIgnoreCase(client)) {
                            return netId;
                        }
                    }
                    continue;
                }
            }
            return -1;
        }

        private String[] getClientList(int netId) {
            String p2pClients = this.mWifiNative.getP2pClientList(netId);
            if (p2pClients == null) {
                return null;
            }
            return p2pClients.split(" ");
        }

        private boolean removeClientFromList(int netId, String addr, boolean isRemovable) {
            StringBuilder modifiedClientList = new StringBuilder();
            String[] currentClientList = getClientList(netId);
            boolean isClientRemoved = false;
            if (currentClientList != null) {
                for (String client : currentClientList) {
                    if (client.equalsIgnoreCase(addr)) {
                        isClientRemoved = true;
                    } else {
                        modifiedClientList.append(" ");
                        modifiedClientList.append(client);
                    }
                }
            }
            if (modifiedClientList.length() == 0 && isRemovable) {
                if (WifiP2pServiceImpl.DBG) {
                    logd("Remove unknown network");
                }
                this.mGroups.remove(netId);
                return true;
            } else if (!isClientRemoved) {
                return false;
            } else {
                if (WifiP2pServiceImpl.DBG) {
                    logd("Modified client list: " + modifiedClientList);
                }
                if (modifiedClientList.length() == 0) {
                    modifiedClientList.append("\"\"");
                }
                this.mWifiNative.setP2pClientList(netId, modifiedClientList.toString());
                this.mWifiNative.saveConfig();
                return true;
            }
        }

        private void setWifiP2pInfoOnGroupFormation(InetAddress serverInetAddress) {
            this.mWifiP2pInfo.groupFormed = true;
            this.mWifiP2pInfo.isGroupOwner = this.mGroup.isGroupOwner();
            this.mWifiP2pInfo.groupOwnerAddress = serverInetAddress;
        }

        private void resetWifiP2pInfo() {
            this.mWifiP2pInfo.groupFormed = false;
            this.mWifiP2pInfo.isGroupOwner = false;
            this.mWifiP2pInfo.groupOwnerAddress = null;
        }

        private String getDeviceName(String deviceAddress) {
            WifiP2pDevice d = this.mPeers.get(deviceAddress);
            if (d != null) {
                return d.deviceName;
            }
            return deviceAddress;
        }

        private String getPersistedDeviceName() {
            String deviceName = Global.getString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name");
            if (deviceName == null) {
                return SystemProperties.get("ro.product.model", "OPPO");
            }
            return deviceName;
        }

        private boolean setAndPersistDeviceName(String devName) {
            if (devName == null) {
                return false;
            }
            if (this.mWifiNative.setDeviceName(devName)) {
                WifiP2pServiceImpl.this.mThisDevice.deviceName = devName;
                this.mWifiNative.setP2pSsidPostfix("-" + WifiP2pServiceImpl.this.mThisDevice.deviceName);
                Global.putString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name", devName);
                sendThisDeviceChangedBroadcast();
                return true;
            }
            loge("Failed to set device name " + devName);
            return false;
        }

        private boolean setWfdInfo(WifiP2pWfdInfo wfdInfo) {
            boolean success;
            if (!wfdInfo.isWfdEnabled()) {
                success = this.mWifiNative.setWfdEnable(false);
            } else if (this.mWifiNative.setWfdEnable(true)) {
                success = this.mWifiNative.setWfdDeviceInfo(wfdInfo.getDeviceInfoHex());
            } else {
                success = false;
            }
            if (success) {
                WifiP2pServiceImpl.this.mThisDevice.wfdInfo = wfdInfo;
                sendThisDeviceChangedBroadcast();
                return true;
            }
            loge("Failed to set wfd properties");
            return false;
        }

        private void initializeP2pSettings() {
            if (this.mWifiInjector == null) {
                this.mWifiInjector = WifiInjector.getInstance();
            }
            int halFeatureSet = this.mWifiInjector.getWifiNative().getSupportedFeatureSet();
            if ((halFeatureSet & 2) == 2) {
                logd("5G Supported, need to set 5G channel reduce interference ;mHalFeatureSet= " + halFeatureSet);
                this.mWifiNative.setP2pChannelSetAt5G();
            } else {
                logd("5G is not Supporte no need to set special channel for; mHalFeatureSet= " + halFeatureSet);
            }
            WifiP2pServiceImpl.this.mThisDevice.deviceName = getPersistedDeviceName();
            this.mWifiNative.setP2pDeviceName(WifiP2pServiceImpl.this.mThisDevice.deviceName);
            this.mWifiNative.setP2pSsidPostfix("-" + WifiP2pServiceImpl.this.mThisDevice.deviceName);
            this.mWifiNative.setP2pDeviceType(WifiP2pServiceImpl.this.mThisDevice.primaryDeviceType);
            this.mWifiNative.setConfigMethods("virtual_push_button physical_display keypad");
            WifiP2pServiceImpl.this.mThisDevice.deviceAddress = this.mWifiNative.p2pGetDeviceAddress();
            updateThisDevice(3);
            if (WifiP2pServiceImpl.DBG) {
                logd("DeviceAddress: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
            }
            WifiP2pServiceImpl.this.mClientInfoList.clear();
            this.mWifiNative.p2pFlush();
            this.mWifiNative.p2pServiceFlush();
            WifiP2pServiceImpl.this.mServiceTransactionId = (byte) 0;
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
        }

        private void updateThisDevice(int status) {
            WifiP2pServiceImpl.this.mThisDevice.status = status;
            sendThisDeviceChangedBroadcast();
        }

        private void handleGroupCreationFailure() {
            resetWifiP2pInfo();
            WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(DetailedState.FAILED, null, null);
            sendP2pConnectionChangedBroadcast();
            boolean peersChanged = this.mPeers.remove(this.mPeersLostDuringConnection);
            if (!(TextUtils.isEmpty(this.mSavedPeerConfig.deviceAddress) || this.mPeers.remove(this.mSavedPeerConfig.deviceAddress) == null)) {
                peersChanged = true;
            }
            if (peersChanged) {
                sendPeersChangedBroadcast();
            }
            this.mPeersLostDuringConnection.clear();
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            sendMessage(139265);
        }

        private void handleGroupRemoved() {
            if (this.mGroup.isGroupOwner()) {
                stopDhcpServer(this.mGroup.getInterface());
            } else {
                if (WifiP2pServiceImpl.DBG) {
                    logd("stop IpManager");
                }
                WifiP2pServiceImpl.this.stopIpManager();
                try {
                    WifiP2pServiceImpl.this.mNwService.removeInterfaceFromLocalNetwork(this.mGroup.getInterface());
                } catch (RemoteException e) {
                    loge("Failed to remove iface from local network " + e);
                }
            }
            try {
                WifiP2pServiceImpl.this.mNwService.clearInterfaceAddresses(this.mGroup.getInterface());
            } catch (Exception e2) {
                loge("Failed to clear addresses " + e2);
            }
            this.mWifiNative.setP2pGroupIdle(this.mGroup.getInterface(), 0);
            boolean peersChanged = false;
            for (WifiP2pDevice d : this.mGroup.getClientList()) {
                if (this.mPeers.remove(d)) {
                    peersChanged = true;
                }
            }
            if (this.mPeers.remove(this.mGroup.getOwner())) {
                peersChanged = true;
            }
            if (this.mPeers.remove(this.mPeersLostDuringConnection)) {
                peersChanged = true;
            }
            if (peersChanged) {
                sendPeersChangedBroadcast();
            }
            this.mGroup = null;
            this.mPeersLostDuringConnection.clear();
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            if (WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi) {
                if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                    WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 0);
                } else {
                    loge("handleGroupRemoved(): WifiChannel is null");
                }
                WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = false;
            }
        }

        private void replyToMessage(Message msg, int what) {
            if (msg.replyTo != null) {
                Message dstMsg = obtainMessage(msg);
                dstMsg.what = what;
                WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
            }
        }

        private void replyToMessage(Message msg, int what, int arg1) {
            if (msg.replyTo != null) {
                Message dstMsg = obtainMessage(msg);
                dstMsg.what = what;
                dstMsg.arg1 = arg1;
                WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
            }
        }

        private void replyToMessage(Message msg, int what, Object obj) {
            if (msg.replyTo != null) {
                Message dstMsg = obtainMessage(msg);
                dstMsg.what = what;
                dstMsg.obj = obj;
                WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
            }
        }

        private Message obtainMessage(Message srcMsg) {
            Message msg = Message.obtain();
            msg.arg2 = srcMsg.arg2;
            return msg;
        }

        protected void logd(String s) {
            Slog.d(WifiP2pServiceImpl.TAG, s);
        }

        protected void loge(String s) {
            Slog.e(WifiP2pServiceImpl.TAG, s);
        }

        private boolean updateSupplicantServiceRequest() {
            clearSupplicantServiceRequest();
            StringBuffer sb = new StringBuffer();
            for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                for (int i = 0; i < c.mReqList.size(); i++) {
                    WifiP2pServiceRequest req = (WifiP2pServiceRequest) c.mReqList.valueAt(i);
                    if (req != null) {
                        sb.append(req.getSupplicantQuery());
                    }
                }
            }
            if (sb.length() == 0) {
                return false;
            }
            WifiP2pServiceImpl.this.mServiceDiscReqId = this.mWifiNative.p2pServDiscReq(WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS, sb.toString());
            if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                return false;
            }
            return true;
        }

        private void clearSupplicantServiceRequest() {
            if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                this.mWifiNative.p2pServDiscCancelReq(WifiP2pServiceImpl.this.mServiceDiscReqId);
                WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            }
        }

        private boolean addServiceRequest(Messenger m, WifiP2pServiceRequest req) {
            if (m == null || req == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return false;
            }
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(m, true);
            if (clientInfo == null) {
                return false;
            }
            WifiP2pServiceImpl wifiP2pServiceImpl = WifiP2pServiceImpl.this;
            wifiP2pServiceImpl.mServiceTransactionId = (byte) (wifiP2pServiceImpl.mServiceTransactionId + 1);
            if (WifiP2pServiceImpl.this.mServiceTransactionId == (byte) 0) {
                wifiP2pServiceImpl = WifiP2pServiceImpl.this;
                wifiP2pServiceImpl.mServiceTransactionId = (byte) (wifiP2pServiceImpl.mServiceTransactionId + 1);
            }
            req.setTransactionId(WifiP2pServiceImpl.this.mServiceTransactionId);
            clientInfo.mReqList.put(WifiP2pServiceImpl.this.mServiceTransactionId, req);
            if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                return true;
            }
            return updateSupplicantServiceRequest();
        }

        private void removeServiceRequest(Messenger m, WifiP2pServiceRequest req) {
            if (m == null || req == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
            }
            ClientInfo clientInfo = getClientInfo(m, false);
            if (clientInfo != null) {
                boolean removed = false;
                for (int i = 0; i < clientInfo.mReqList.size(); i++) {
                    if (req.equals(clientInfo.mReqList.valueAt(i))) {
                        removed = true;
                        clientInfo.mReqList.removeAt(i);
                        break;
                    }
                }
                if (removed) {
                    if (clientInfo.mReqList.size() == 0 && clientInfo.mServList.size() == 0) {
                        if (WifiP2pServiceImpl.DBG) {
                            logd("remove client information from framework");
                        }
                        WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                    }
                    if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                        updateSupplicantServiceRequest();
                    }
                }
            }
        }

        private void clearServiceRequests(Messenger m) {
            if (m == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return;
            }
            ClientInfo clientInfo = getClientInfo(m, false);
            if (clientInfo != null && clientInfo.mReqList.size() != 0) {
                clientInfo.mReqList.clear();
                if (clientInfo.mServList.size() == 0) {
                    if (WifiP2pServiceImpl.DBG) {
                        logd("remove channel information from framework");
                    }
                    WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                }
                if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                    updateSupplicantServiceRequest();
                }
            }
        }

        private boolean addLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
            if (m == null || servInfo == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                return false;
            }
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(m, true);
            if (clientInfo == null || !clientInfo.mServList.add(servInfo)) {
                return false;
            }
            if (this.mWifiNative.p2pServiceAdd(servInfo)) {
                return true;
            }
            clientInfo.mServList.remove(servInfo);
            return false;
        }

        private void removeLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
            if (m == null || servInfo == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                return;
            }
            ClientInfo clientInfo = getClientInfo(m, false);
            if (clientInfo != null) {
                this.mWifiNative.p2pServiceDel(servInfo);
                clientInfo.mServList.remove(servInfo);
                if (clientInfo.mReqList.size() == 0 && clientInfo.mServList.size() == 0) {
                    if (WifiP2pServiceImpl.DBG) {
                        logd("remove client information from framework");
                    }
                    WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                }
            }
        }

        private void clearLocalServices(Messenger m) {
            if (m == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return;
            }
            ClientInfo clientInfo = getClientInfo(m, false);
            if (clientInfo != null) {
                for (WifiP2pServiceInfo servInfo : clientInfo.mServList) {
                    this.mWifiNative.p2pServiceDel(servInfo);
                }
                clientInfo.mServList.clear();
                if (clientInfo.mReqList.size() == 0) {
                    if (WifiP2pServiceImpl.DBG) {
                        logd("remove client information from framework");
                    }
                    WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                }
            }
        }

        private void clearClientInfo(Messenger m) {
            clearLocalServices(m);
            clearServiceRequests(m);
        }

        private void sendServiceResponse(WifiP2pServiceResponse resp) {
            if (resp == null) {
                Log.e(WifiP2pServiceImpl.TAG, "sendServiceResponse with null response");
                return;
            }
            for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                if (((WifiP2pServiceRequest) c.mReqList.get(resp.getTransactionId())) != null) {
                    Message msg = Message.obtain();
                    msg.what = 139314;
                    msg.arg1 = 0;
                    msg.arg2 = 0;
                    msg.obj = resp;
                    if (c.mMessenger != null) {
                        try {
                            c.mMessenger.send(msg);
                        } catch (RemoteException e) {
                            if (WifiP2pServiceImpl.DBG) {
                                logd("detect dead channel");
                            }
                            clearClientInfo(c.mMessenger);
                            return;
                        }
                    }
                    continue;
                }
            }
        }

        private void clearClientDeadChannels() {
            ArrayList<Messenger> deadClients = new ArrayList();
            for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                Message msg = Message.obtain();
                msg.what = 139313;
                msg.arg1 = 0;
                msg.arg2 = 0;
                msg.obj = null;
                if (c.mMessenger != null) {
                    try {
                        c.mMessenger.send(msg);
                    } catch (RemoteException e) {
                        if (WifiP2pServiceImpl.DBG) {
                            logd("detect dead channel");
                        }
                        deadClients.add(c.mMessenger);
                    }
                }
            }
            for (Messenger m : deadClients) {
                clearClientInfo(m);
            }
        }

        private ClientInfo getClientInfo(Messenger m, boolean createIfNotExist) {
            ClientInfo clientInfo = (ClientInfo) WifiP2pServiceImpl.this.mClientInfoList.get(m);
            if (clientInfo != null || !createIfNotExist) {
                return clientInfo;
            }
            if (WifiP2pServiceImpl.DBG) {
                logd("add a new client");
            }
            clientInfo = new ClientInfo(WifiP2pServiceImpl.this, m, null);
            WifiP2pServiceImpl.this.mClientInfoList.put(m, clientInfo);
            return clientInfo;
        }

        private WifiP2pDeviceList getPeers(Bundle pkg, int uid) {
            String pkgName = pkg.getString("android.net.wifi.p2p.CALLING_PACKAGE");
            boolean scanPermission = false;
            if (this.mWifiInjector == null) {
                this.mWifiInjector = WifiInjector.getInstance();
            }
            try {
                scanPermission = this.mWifiInjector.getWifiPermissionsUtil().canAccessScanResults(pkgName, uid, 26);
            } catch (SecurityException e) {
                Log.e(WifiP2pServiceImpl.TAG, "Security Exception, cannot access peer list");
            }
            if (scanPermission) {
                return new WifiP2pDeviceList(this.mPeers);
            }
            return new WifiP2pDeviceList();
        }

        public boolean isP2pGroupNegotiationState() {
            if (getCurrentState() == this.mGroupNegotiationState || WifiP2pServiceImpl.this.mDiscoveryStarted) {
                return true;
            }
            return false;
        }

        private void enableBTCoex() {
            if (this.mIsBTCoexDisabled) {
                this.mWifNative.setBluetoothCoexistenceMode(2);
                this.mIsBTCoexDisabled = false;
            }
        }

        private WifiP2pGroup addPersistentGroup(HashMap<String, String> variables) {
            int netId = this.mWifiNative.addNetwork();
            if (WifiP2pServiceImpl.DBG) {
                logd("addPersistentGroup netId=" + netId);
            }
            for (String key : variables.keySet()) {
                if (WifiP2pServiceImpl.DBG) {
                    logd("addPersistentGroup variable=" + key + " : " + ((String) variables.get(key)));
                }
                this.mWifiNative.setNetworkVariable(netId, key, (String) variables.get(key));
            }
            updatePersistentNetworks(true);
            for (WifiP2pGroup group : this.mGroups.getGroupList()) {
                if (netId == group.getNetworkId()) {
                    return group;
                }
            }
            if (WifiP2pServiceImpl.DBG) {
                logd("addPersistentGroup failed.");
            }
            return null;
        }
    }

    public enum P2pStatus {
        SUCCESS,
        INFORMATION_IS_CURRENTLY_UNAVAILABLE,
        INCOMPATIBLE_PARAMETERS,
        LIMIT_REACHED,
        INVALID_PARAMETER,
        UNABLE_TO_ACCOMMODATE_REQUEST,
        PREVIOUS_PROTOCOL_ERROR,
        NO_COMMON_CHANNEL,
        UNKNOWN_P2P_GROUP,
        BOTH_GO_INTENT_15,
        INCOMPATIBLE_PROVISIONING_METHOD,
        REJECTED_BY_USER,
        UNKNOWN;

        public static P2pStatus valueOf(int error) {
            switch (error) {
                case 0:
                    return SUCCESS;
                case 1:
                    return INFORMATION_IS_CURRENTLY_UNAVAILABLE;
                case 2:
                    return INCOMPATIBLE_PARAMETERS;
                case 3:
                    return LIMIT_REACHED;
                case 4:
                    return INVALID_PARAMETER;
                case 5:
                    return UNABLE_TO_ACCOMMODATE_REQUEST;
                case 6:
                    return PREVIOUS_PROTOCOL_ERROR;
                case 7:
                    return NO_COMMON_CHANNEL;
                case 8:
                    return UNKNOWN_P2P_GROUP;
                case 9:
                    return BOTH_GO_INTENT_15;
                case 10:
                    return INCOMPATIBLE_PROVISIONING_METHOD;
                case 11:
                    return REJECTED_BY_USER;
                default:
                    return UNKNOWN;
            }
        }
    }

    public WifiP2pServiceImpl(Context context) {
        this.mContext = context;
        this.mNetworkInfo = new NetworkInfo(13, 0, NETWORKTYPE, "");
        this.mP2pSupported = this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
        this.mThisDevice.primaryDeviceType = this.mContext.getResources().getString(17039716);
        HandlerThread wifiP2pThread = new HandlerThread(TAG);
        wifiP2pThread.start();
        this.mClientHandler = new ClientHandler(TAG, wifiP2pThread.getLooper());
        this.mP2pStateMachine = new P2pStateMachine(TAG, wifiP2pThread.getLooper(), this.mP2pSupported);
        this.mP2pStateMachine.start();
    }

    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            DBG = true;
        } else {
            DBG = false;
        }
    }

    public void connectivityServiceReady() {
        this.mNwService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
    }

    private int checkConnectivityInternalPermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL");
    }

    private int checkLocationHardwarePermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.LOCATION_HARDWARE");
    }

    private void enforceConnectivityInternalOrLocationHardwarePermission() {
        if (checkConnectivityInternalPermission() != 0 && checkLocationHardwarePermission() != 0) {
            enforceConnectivityInternalPermission();
        }
    }

    private void stopIpManager() {
        if (this.mIpManager != null) {
            this.mIpManager.shutdown();
            this.mIpManager = null;
        }
        this.mDhcpResults = null;
    }

    private void startIpManager(String ifname) {
        ProvisioningConfiguration config;
        stopIpManager();
        this.mIpManager = new IpManager(this.mContext, ifname, new Callback() {
            public void onPreDhcpAction() {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_PRE_DHCP_ACTION);
            }

            public void onPostDhcpAction() {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_POST_DHCP_ACTION);
            }

            public void onNewDhcpResults(DhcpResults dhcpResults) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_DHCP_RESULTS, dhcpResults);
            }

            public void onProvisioningSuccess(LinkProperties newLp) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_PROVISIONING_SUCCESS);
            }

            public void onProvisioningFailure(LinkProperties newLp) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_PROVISIONING_FAILURE);
            }
        }, this.mNwService);
        IpManager ipManager;
        if (this.isNfcTriggered) {
            Slog.d(TAG, "startIpManager set static ip to 192.168.49.100");
            DhcpResults dhcpResults = new DhcpResults();
            dhcpResults.setIpAddress("192.168.49.100", 24);
            dhcpResults.setGateway(SERVER_ADDRESS);
            dhcpResults.addDns(SERVER_ADDRESS);
            ipManager = this.mIpManager;
            config = IpManager.buildProvisioningConfiguration().withoutIPv6().withoutIpReachabilityMonitor().withStaticConfiguration(dhcpResults).build();
        } else {
            Slog.d(TAG, "startIpManager aquire dynamic ip for client");
            ipManager = this.mIpManager;
            config = IpManager.buildProvisioningConfiguration().withoutIPv6().withoutIpReachabilityMonitor().withPreDhcpAction(WifiStateMachine.LAST_SELECTED_NETWORK_EXPIRATION_AGE_MILLIS).withProvisioningTimeoutMs(36000).build();
        }
        this.mIpManager.startProvisioning(config);
    }

    public Messenger getMessenger(IBinder binder) {
        Messenger messenger;
        enforceAccessPermission();
        enforceChangePermission();
        synchronized (this.mLock) {
            messenger = new Messenger(this.mClientHandler);
            if (DBG) {
                Log.d(TAG, "getMessenger: uid=" + getCallingUid() + ", binder=" + binder + ", messenger=" + messenger);
            }
            DeathRecipient dr = new com.android.server.wifi.p2p.-$Lambda$fn1kZ8DxUVXwaqX8IUvJlmRRm00.AnonymousClass1(this, binder);
            try {
                binder.linkToDeath(dr, 0);
                this.mDeathDataByBinder.put(binder, new DeathHandlerData(dr, messenger));
            } catch (RemoteException e) {
                Log.e(TAG, "Error on linkToDeath: e=" + e);
            }
            if (this.mIWifiP2pIface == null) {
                if (this.mHalDeviceManager == null) {
                    if (this.mWifiInjector == null) {
                        this.mWifiInjector = WifiInjector.getInstance();
                    }
                    this.mHalDeviceManager = this.mWifiInjector.getHalDeviceManager();
                }
                this.mIWifiP2pIface = this.mHalDeviceManager.createP2pIface(new -$Lambda$fn1kZ8DxUVXwaqX8IUvJlmRRm00(this), this.mP2pStateMachine.getHandler().getLooper());
            }
        }
        return messenger;
    }

    /* synthetic */ void lambda$-com_android_server_wifi_p2p_WifiP2pServiceImpl_24846(IBinder binder) {
        if (DBG) {
            Log.d(TAG, "binderDied: binder=" + binder);
        }
        close(binder);
    }

    /* synthetic */ void lambda$-com_android_server_wifi_p2p_WifiP2pServiceImpl_25678() {
        if (DBG) {
            Log.d(TAG, "IWifiP2pIface destroyedListener");
        }
        synchronized (this.mLock) {
            this.mIWifiP2pIface = null;
        }
    }

    public Messenger getP2pStateMachineMessenger() {
        enforceConnectivityInternalOrLocationHardwarePermission();
        enforceAccessPermission();
        enforceChangePermission();
        return new Messenger(this.mP2pStateMachine.getHandler());
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void close(IBinder binder) {
        enforceAccessPermission();
        enforceChangePermission();
        synchronized (this.mLock) {
            DeathHandlerData dhd = (DeathHandlerData) this.mDeathDataByBinder.get(binder);
            if (dhd == null) {
                Log.w(TAG, "close(): no death recipient for binder");
                return;
            }
            binder.unlinkToDeath(dhd.mDeathRecipient, 0);
            this.mDeathDataByBinder.remove(binder);
            if (dhd.mMessenger != null && this.mDeathDataByBinder.isEmpty()) {
                try {
                    dhd.mMessenger.send(this.mClientHandler.obtainMessage(139268));
                    dhd.mMessenger.send(this.mClientHandler.obtainMessage(139280));
                } catch (RemoteException e) {
                    Log.e(TAG, "close: Failed sending clean-up commands: e=" + e);
                }
                if (this.mIWifiP2pIface != null) {
                    this.mHalDeviceManager.removeIface(this.mIWifiP2pIface);
                    this.mIWifiP2pIface = null;
                }
            }
        }
    }

    public void setNfcTriggered(boolean enable) {
        if (DBG) {
            Slog.d(TAG, "set isNfcTriggered(" + this.isNfcTriggered + ") to " + enable);
        }
        if (enable) {
            long token = Binder.clearCallingIdentity();
            try {
                this.mWifiInjector.getOppoWifiSharingManager().stopWifiSharingTetheringIfNeed();
                P2pStateMachine p2pStateMachine = this.mP2pStateMachine;
                int i = this.mFastConTriggeredNum + 1;
                this.mFastConTriggeredNum = i;
                p2pStateMachine.sendMessageDelayed(AUTO_ACCEPT_INVITATION_TIME_OUT, i, AnqpCache.CACHE_SWEEP_INTERVAL_MILLISECONDS);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            this.mFastConTriggeredNum = 0;
        }
        this.isNfcTriggered = enable;
    }

    public void setMiracastMode(int mode) {
        enforceConnectivityInternalPermission();
        checkConfigureWifiDisplayPermission();
        this.mP2pStateMachine.sendMessage(SET_MIRACAST_MODE, mode);
    }

    public void checkConfigureWifiDisplayPermission() {
        if (!getWfdPermission(Binder.getCallingUid())) {
            throw new SecurityException("Wifi Display Permission denied for uid = " + Binder.getCallingUid());
        }
    }

    private boolean getWfdPermission(int uid) {
        if (this.mWifiInjector == null) {
            this.mWifiInjector = WifiInjector.getInstance();
        }
        return this.mWifiInjector.getWifiPermissionsWrapper().getUidPermission("android.permission.CONFIGURE_WIFI_DISPLAY", uid) != -1;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump WifiP2pService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        this.mP2pStateMachine.dump(fd, pw, args);
        pw.println("mAutonomousGroup " + this.mAutonomousGroup);
        pw.println("mJoinExistingGroup " + this.mJoinExistingGroup);
        pw.println("mDiscoveryStarted " + this.mDiscoveryStarted);
        pw.println("mNetworkInfo " + this.mNetworkInfo);
        pw.println("mTemporarilyDisconnectedWifi " + this.mTemporarilyDisconnectedWifi);
        pw.println("mServiceDiscReqId " + this.mServiceDiscReqId);
        pw.println("mDeathDataByBinder " + this.mDeathDataByBinder);
        pw.println();
        IpManager ipManager = this.mIpManager;
        if (ipManager != null) {
            pw.println("mIpManager:");
            ipManager.dump(fd, pw, args);
        }
    }

    public boolean isP2pGroupNegotiationState() {
        if (this.mP2pStateMachine != null) {
            return this.mP2pStateMachine.isP2pGroupNegotiationState();
        }
        return false;
    }
}
