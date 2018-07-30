package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.IRttManager.Stub;
import android.net.wifi.RttManager.ParcelableRttParams;
import android.net.wifi.RttManager.ParcelableRttResults;
import android.net.wifi.RttManager.ResponderConfig;
import android.net.wifi.RttManager.RttCapabilities;
import android.net.wifi.RttManager.RttClient;
import android.net.wifi.RttManager.RttParams;
import android.net.wifi.RttManager.RttResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.SystemService;
import com.android.server.wifi.WifiNative.RttEventHandler;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public final class RttService extends SystemService {
    public static final boolean DBG = true;
    private static final String TAG = "RttService";
    private final HandlerThread mHandlerThread = new HandlerThread("WifiRttService");
    RttServiceImpl mImpl;

    static class RttServiceImpl extends Stub {
        private static final int BASE = 160512;
        private static final int CMD_DRIVER_LOADED = 160512;
        private static final int CMD_DRIVER_UNLOADED = 160513;
        private static final int CMD_ISSUE_NEXT_REQUEST = 160514;
        private static final int CMD_RTT_RESPONSE = 160515;
        private static final int MAX_RESPONDER_DURATION_SECONDS = 600;
        private final SparseArray<IBinder> mBinderByKey = new SparseArray();
        private ClientHandler mClientHandler;
        @GuardedBy("mLock")
        private ArrayMap<Messenger, ClientInfo> mClients = new ArrayMap();
        private final Context mContext;
        private int mCurrentKey = 100;
        private RttEventHandler mEventHandler = new RttEventHandler() {
            public void onRttResults(RttResult[] result) {
                RttServiceImpl.this.mStateMachine.sendMessage(RttServiceImpl.CMD_RTT_RESPONSE, result);
            }
        };
        private final Object mLock = new Object();
        private final Looper mLooper;
        private Queue<RttRequest> mRequestQueue = new LinkedList();
        private RttStateMachine mStateMachine;
        private final WifiInjector mWifiInjector;
        private final WifiNative mWifiNative;

        private class ClientHandler extends Handler {
            ClientHandler(Looper looper) {
                super(looper);
            }

            public void handleMessage(Message msg) {
                Log.d(RttService.TAG, "ClientHandler got" + msg + " what = " + getDescription(msg.what));
                ClientInfo ci;
                switch (msg.what) {
                    case 69633:
                        AsyncChannel ac = new AsyncChannel();
                        ac.connected(RttServiceImpl.this.mContext, this, msg.replyTo);
                        synchronized (RttServiceImpl.this.mLock) {
                            RttServiceImpl.this.mClients.put(msg.replyTo, new ClientInfo(ac, msg.sendingUid, msg.obj != null ? ((RttClient) msg.obj).getPackageName() : null));
                        }
                        ac.replyToMessage(msg, 69634, 0);
                        return;
                    case 69636:
                        if (msg.arg1 == 2) {
                            Slog.e(RttService.TAG, "Send failed, client connection lost");
                        } else {
                            Slog.d(RttService.TAG, "Client connection lost with reason: " + msg.arg1);
                        }
                        Slog.d(RttService.TAG, "closing client " + msg.replyTo);
                        synchronized (RttServiceImpl.this.mLock) {
                            ci = (ClientInfo) RttServiceImpl.this.mClients.remove(msg.replyTo);
                            if (ci != null) {
                                ci.cleanup();
                            }
                        }
                        return;
                    case 160265:
                        int key = msg.arg1;
                        IBinder binder = (IBinder) RttServiceImpl.this.mBinderByKey.get(key);
                        if (binder == null) {
                            Slog.e(RttService.TAG, "Can't find binder registered with key=" + key + " - no " + "death listener!");
                            return;
                        }
                        try {
                            binder.linkToDeath(new RttDeathListener(binder, msg.replyTo), 0);
                        } catch (RemoteException e) {
                            Slog.e(RttService.TAG, "Can't link to death for binder on key=" + key);
                        }
                        return;
                    default:
                        synchronized (RttServiceImpl.this.mLock) {
                            ci = (ClientInfo) RttServiceImpl.this.mClients.get(msg.replyTo);
                        }
                        if (ci == null) {
                            Slog.e(RttService.TAG, "Could not find client info for message " + msg.replyTo);
                            RttServiceImpl.this.replyFailed(msg, -3, "Could not find listener");
                            return;
                        } else if (!RttServiceImpl.this.enforcePermissionCheck(msg)) {
                            RttServiceImpl.this.replyFailed(msg, -5, "Client doesn't have LOCATION_HARDWARE permission");
                            return;
                        } else if (RttServiceImpl.this.checkLocationPermission(ci)) {
                            for (int cmd : new int[]{160256, 160257, 160261, 160262}) {
                                if (cmd == msg.what) {
                                    RttServiceImpl.this.mStateMachine.sendMessage(Message.obtain(msg));
                                    return;
                                }
                            }
                            RttServiceImpl.this.replyFailed(msg, -4, "Invalid request");
                            return;
                        } else {
                            RttServiceImpl.this.replyFailed(msg, -5, "Client doesn't have ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission");
                            return;
                        }
                }
            }

            private String getDescription(int what) {
                switch (what) {
                    case 160261:
                        return "CMD_OP_ENABLE_RESPONDER";
                    case 160262:
                        return "CMD_OP_DISABLE_RESPONDER";
                    default:
                        return "CMD_UNKNOWN";
                }
            }
        }

        private class ClientInfo {
            private final AsyncChannel mChannel;
            private final String mPackageName;
            ArrayMap<Integer, RttRequest> mRequests = new ArrayMap();
            Set<Integer> mResponderRequests = new HashSet();
            private final int mUid;

            ClientInfo(AsyncChannel channel, int uid, String packageName) {
                this.mChannel = channel;
                this.mUid = uid;
                this.mPackageName = packageName;
            }

            void addResponderRequest(int key) {
                this.mResponderRequests.add(Integer.valueOf(key));
            }

            void removeResponderRequest(int key) {
                this.mResponderRequests.remove(Integer.valueOf(key));
            }

            boolean addRttRequest(int key, ParcelableRttParams parcelableParams) {
                if (parcelableParams == null) {
                    return false;
                }
                RttParams[] params = parcelableParams.mParams;
                RttRequest request = new RttRequest(RttServiceImpl.this, null);
                request.key = Integer.valueOf(key);
                request.ci = this;
                request.params = params;
                this.mRequests.put(Integer.valueOf(key), request);
                RttServiceImpl.this.mRequestQueue.add(request);
                return true;
            }

            void removeRttRequest(int key) {
                this.mRequests.remove(Integer.valueOf(key));
            }

            void reportResponderEnableSucceed(int key, ResponderConfig config) {
                this.mChannel.sendMessage(160263, 0, key, config);
            }

            void reportResponderEnableFailed(int key, int reason) {
                this.mChannel.sendMessage(160264, reason, key);
                removeResponderRequest(key);
            }

            void reportResult(RttRequest request, RttResult[] results) {
                this.mChannel.sendMessage(160259, 0, request.key.intValue(), new ParcelableRttResults(results));
                removeRttRequest(request.key.intValue());
            }

            void reportFailed(RttRequest request, int reason, String description) {
                reportFailed(request.key.intValue(), reason, description);
            }

            void reportFailed(int key, int reason, String description) {
                Bundle bundle = new Bundle();
                bundle.putString("android.net.wifi.RttManager.Description", description);
                this.mChannel.sendMessage(160258, key, reason, bundle);
                removeRttRequest(key);
            }

            void reportAborted(int key) {
                this.mChannel.sendMessage(160260, 0, key);
                cleanup();
            }

            void cleanup() {
                this.mRequests.clear();
                RttServiceImpl.this.mRequestQueue.clear();
                this.mResponderRequests.clear();
                RttServiceImpl.this.mStateMachine.sendMessage(160262);
            }

            public String toString() {
                return "ClientInfo [uid=" + this.mUid + ", channel=" + this.mChannel + "]";
            }
        }

        private class RttDeathListener implements DeathRecipient {
            private final IBinder mBinder;
            private final Messenger mReplyTo;

            RttDeathListener(IBinder binder, Messenger replyTo) {
                this.mBinder = binder;
                this.mReplyTo = replyTo;
            }

            public void binderDied() {
                Slog.d(RttService.TAG, "binder death for client mReplyTo=" + this.mReplyTo);
                synchronized (RttServiceImpl.this.mLock) {
                    ClientInfo ci = (ClientInfo) RttServiceImpl.this.mClients.remove(this.mReplyTo);
                    if (ci != null) {
                        ci.cleanup();
                    } else {
                        Slog.w(RttService.TAG, "ClientInfo not found for terminated app -- mReplyTo=" + this.mReplyTo);
                    }
                }
            }
        }

        private class RttRequest {
            ClientInfo ci;
            Integer key;
            RttParams[] params;

            /* synthetic */ RttRequest(RttServiceImpl this$1, RttRequest -this1) {
                this();
            }

            private RttRequest() {
            }

            public String toString() {
                String str = getClass().getName() + "@" + Integer.toHexString(hashCode());
                if (this.key != null) {
                    return str + " key: " + this.key;
                }
                return str + " key: " + " , null";
            }
        }

        class RttStateMachine extends StateMachine {
            DefaultState mDefaultState = new DefaultState();
            EnabledState mEnabledState = new EnabledState();
            InitiatorEnabledState mInitiatorEnabledState = new InitiatorEnabledState();
            ResponderConfig mResponderConfig;
            ResponderEnabledState mResponderEnabledState = new ResponderEnabledState();

            class DefaultState extends State {
                DefaultState() {
                }

                public boolean processMessage(Message msg) {
                    Log.d(RttService.TAG, "DefaultState got" + msg);
                    switch (msg.what) {
                        case 160256:
                            RttServiceImpl.this.replyFailed(msg, -2, "Try later");
                            break;
                        case 160257:
                            return true;
                        case 160261:
                            ClientInfo client;
                            synchronized (RttServiceImpl.this.mLock) {
                                client = (ClientInfo) RttServiceImpl.this.mClients.get(msg.replyTo);
                            }
                            if (client != null) {
                                client.reportResponderEnableFailed(msg.arg2, -2);
                                break;
                            }
                            Log.e(RttService.TAG, "client not connected yet!");
                            break;
                        case 160262:
                            return true;
                        case 160512:
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mEnabledState);
                            break;
                        case RttServiceImpl.CMD_ISSUE_NEXT_REQUEST /*160514*/:
                            RttStateMachine.this.deferMessage(msg);
                            break;
                        default:
                            return false;
                    }
                    return true;
                }
            }

            class EnabledState extends State {
                EnabledState() {
                }

                public void enter() {
                }

                public void exit() {
                }

                public boolean processMessage(Message msg) {
                    ClientInfo ci;
                    Log.d(RttService.TAG, "EnabledState got" + msg);
                    synchronized (RttServiceImpl.this.mLock) {
                        ci = (ClientInfo) RttServiceImpl.this.mClients.get(msg.replyTo);
                    }
                    switch (msg.what) {
                        case 160256:
                            ParcelableRttParams params = msg.obj;
                            if (params != null && params.mParams != null && params.mParams.length != 0) {
                                if (!ci.addRttRequest(msg.arg2, params)) {
                                    RttServiceImpl.this.replyFailed(msg, -4, "Unspecified");
                                    break;
                                }
                                RttStateMachine.this.sendMessage(RttServiceImpl.CMD_ISSUE_NEXT_REQUEST);
                                break;
                            }
                            RttServiceImpl.this.replyFailed(msg, -4, "No params");
                            break;
                        case 160257:
                            for (RttRequest request : RttServiceImpl.this.mRequestQueue) {
                                if (request.key.intValue() == msg.arg2) {
                                    Log.d(RttService.TAG, "Cancelling not-yet-scheduled RTT");
                                    RttServiceImpl.this.mRequestQueue.remove(request);
                                    request.ci.reportAborted(request.key.intValue());
                                    break;
                                }
                            }
                            break;
                        case 160261:
                            int key = msg.arg2;
                            RttStateMachine.this.mResponderConfig = RttServiceImpl.this.mWifiNative.enableRttResponder(RttServiceImpl.MAX_RESPONDER_DURATION_SECONDS);
                            Log.d(RttService.TAG, "mWifiNative.enableRttResponder called");
                            if (RttStateMachine.this.mResponderConfig == null) {
                                Log.e(RttService.TAG, "enable responder failed");
                                ci.reportResponderEnableFailed(key, -1);
                                break;
                            }
                            RttStateMachine.this.mResponderConfig.macAddress = RttServiceImpl.this.mWifiNative.getMacAddress();
                            ci.addResponderRequest(key);
                            ci.reportResponderEnableSucceed(key, RttStateMachine.this.mResponderConfig);
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mResponderEnabledState);
                            break;
                        case 160262:
                            break;
                        case RttServiceImpl.CMD_DRIVER_UNLOADED /*160513*/:
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mDefaultState);
                            break;
                        case RttServiceImpl.CMD_ISSUE_NEXT_REQUEST /*160514*/:
                            RttStateMachine.this.deferMessage(msg);
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mInitiatorEnabledState);
                            break;
                        default:
                            return false;
                    }
                    return true;
                }
            }

            class InitiatorEnabledState extends State {
                RttRequest mOutstandingRequest;

                InitiatorEnabledState() {
                }

                public boolean processMessage(Message msg) {
                    Log.d(RttService.TAG, "RequestPendingState got" + msg);
                    switch (msg.what) {
                        case 160257:
                            if (this.mOutstandingRequest != null && msg.arg2 == this.mOutstandingRequest.key.intValue()) {
                                Log.d(RttService.TAG, "Cancelling ongoing RTT of: " + msg.arg2);
                                RttServiceImpl.this.mWifiNative.cancelRtt(this.mOutstandingRequest.params);
                                this.mOutstandingRequest.ci.reportAborted(this.mOutstandingRequest.key.intValue());
                                this.mOutstandingRequest = null;
                                RttStateMachine.this.sendMessage(RttServiceImpl.CMD_ISSUE_NEXT_REQUEST);
                                break;
                            }
                            return false;
                        case RttServiceImpl.CMD_DRIVER_UNLOADED /*160513*/:
                            if (this.mOutstandingRequest != null) {
                                RttServiceImpl.this.mWifiNative.cancelRtt(this.mOutstandingRequest.params);
                                Log.d(RttService.TAG, "abort key: " + this.mOutstandingRequest.key);
                                this.mOutstandingRequest.ci.reportAborted(this.mOutstandingRequest.key.intValue());
                                this.mOutstandingRequest = null;
                            }
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mDefaultState);
                            break;
                        case RttServiceImpl.CMD_ISSUE_NEXT_REQUEST /*160514*/:
                            if (this.mOutstandingRequest != null) {
                                Log.d(RttService.TAG, "Current mOutstandingRequest.key is: " + this.mOutstandingRequest.key);
                                Log.d(RttService.TAG, "Ignoring CMD_ISSUE_NEXT_REQUEST");
                                break;
                            }
                            this.mOutstandingRequest = RttServiceImpl.this.issueNextRequest();
                            if (this.mOutstandingRequest == null) {
                                RttStateMachine.this.transitionTo(RttStateMachine.this.mEnabledState);
                            }
                            if (this.mOutstandingRequest == null) {
                                Log.d(RttService.TAG, "CMD_ISSUE_NEXT_REQUEST: mOutstandingRequest =null ");
                                break;
                            }
                            Log.d(RttService.TAG, "new mOutstandingRequest.key is: " + this.mOutstandingRequest.key);
                            break;
                        case RttServiceImpl.CMD_RTT_RESPONSE /*160515*/:
                            Log.d(RttService.TAG, "Received an RTT response from: " + msg.arg2);
                            if (RttServiceImpl.this.checkLocationPermission(this.mOutstandingRequest.ci)) {
                                this.mOutstandingRequest.ci.reportResult(this.mOutstandingRequest, (RttResult[]) msg.obj);
                            }
                            this.mOutstandingRequest = null;
                            RttStateMachine.this.sendMessage(RttServiceImpl.CMD_ISSUE_NEXT_REQUEST);
                            break;
                        default:
                            return false;
                    }
                    return true;
                }
            }

            class ResponderEnabledState extends State {
                ResponderEnabledState() {
                }

                public boolean processMessage(Message msg) {
                    ClientInfo ci;
                    Log.d(RttService.TAG, "ResponderEnabledState got " + msg);
                    synchronized (RttServiceImpl.this.mLock) {
                        ci = (ClientInfo) RttServiceImpl.this.mClients.get(msg.replyTo);
                    }
                    int key = msg.arg2;
                    switch (msg.what) {
                        case 160256:
                        case 160257:
                            RttServiceImpl.this.replyFailed(msg, -6, "Initiator not allowed when responder is turned on");
                            return true;
                        case 160261:
                            ci.addResponderRequest(key);
                            ci.reportResponderEnableSucceed(key, RttStateMachine.this.mResponderConfig);
                            return true;
                        case 160262:
                            if (ci != null) {
                                ci.removeResponderRequest(key);
                            }
                            if (!RttStateMachine.this.hasOutstandingReponderRequests()) {
                                if (!RttServiceImpl.this.mWifiNative.disableRttResponder()) {
                                    Log.e(RttService.TAG, "disable responder failed");
                                }
                                Log.d(RttService.TAG, "mWifiNative.disableRttResponder called");
                                RttStateMachine.this.transitionTo(RttStateMachine.this.mEnabledState);
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            }

            RttStateMachine(Looper looper) {
                super("RttStateMachine", looper);
                addState(this.mDefaultState);
                addState(this.mEnabledState);
                addState(this.mInitiatorEnabledState, this.mEnabledState);
                addState(this.mResponderEnabledState, this.mEnabledState);
                setInitialState(this.mDefaultState);
            }

            private boolean hasOutstandingReponderRequests() {
                synchronized (RttServiceImpl.this.mLock) {
                    for (ClientInfo client : RttServiceImpl.this.mClients.values()) {
                        if (!client.mResponderRequests.isEmpty()) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            String currentState() {
                IState state = getCurrentState();
                return state == null ? "null" : state.getName();
            }
        }

        public Messenger getMessenger(IBinder binder, int[] key) {
            if (!(key == null || key.length == 0)) {
                int keyToUse = this.mCurrentKey;
                this.mCurrentKey = keyToUse + 1;
                if (binder != null) {
                    try {
                        binder.linkToDeath(new -$Lambda$RPzhR64WIMgTSfYC8KVwFhmhzoc(keyToUse, this), 0);
                        this.mBinderByKey.put(keyToUse, binder);
                    } catch (RemoteException e) {
                        Slog.e(RttService.TAG, "getMessenger: can't link to death on binder: " + e);
                        return null;
                    }
                }
                key[0] = keyToUse;
            }
            return new Messenger(this.mClientHandler);
        }

        /* synthetic */ void lambda$-com_android_server_wifi_RttService$RttServiceImpl_1874(int keyToUse) {
            Slog.d(RttService.TAG, "Binder death on key=" + keyToUse);
            this.mBinderByKey.delete(keyToUse);
        }

        RttServiceImpl(Context context, Looper looper, WifiInjector wifiInjector) {
            this.mContext = context;
            this.mWifiNative = wifiInjector.getWifiNative();
            this.mLooper = looper;
            this.mWifiInjector = wifiInjector;
        }

        public void startService() {
            this.mClientHandler = new ClientHandler(this.mLooper);
            this.mStateMachine = new RttStateMachine(this.mLooper);
            this.mContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra("scan_enabled", 1);
                    Log.d(RttService.TAG, "SCAN_AVAILABLE : " + state);
                    if (state == 3) {
                        RttServiceImpl.this.mStateMachine.sendMessage(160512);
                    } else if (state == 1) {
                        RttServiceImpl.this.mStateMachine.sendMessage(RttServiceImpl.CMD_DRIVER_UNLOADED);
                    }
                }
            }, new IntentFilter("wifi_scan_available"));
            this.mStateMachine.start();
        }

        void replySucceeded(Message msg, Object obj) {
            if (msg.replyTo != null) {
                Message reply = Message.obtain();
                reply.what = 160259;
                reply.arg2 = msg.arg2;
                reply.obj = obj;
                try {
                    msg.replyTo.send(reply);
                } catch (RemoteException e) {
                }
            }
        }

        void replyFailed(Message msg, int reason, String description) {
            Message reply = Message.obtain();
            reply.what = 160258;
            reply.arg1 = reason;
            reply.arg2 = msg.arg2;
            Bundle bundle = new Bundle();
            bundle.putString("android.net.wifi.RttManager.Description", description);
            reply.obj = bundle;
            try {
                if (msg.replyTo != null) {
                    msg.replyTo.send(reply);
                }
            } catch (RemoteException e) {
            }
        }

        private boolean enforcePermissionCheck(Message msg) {
            try {
                this.mContext.enforcePermission("android.permission.LOCATION_HARDWARE", -1, msg.sendingUid, "LocationRTT");
                return true;
            } catch (SecurityException e) {
                Log.e(RttService.TAG, "UID: " + msg.sendingUid + " has no LOCATION_HARDWARE Permission");
                return false;
            }
        }

        private boolean checkLocationPermission(ClientInfo clientInfo) {
            return this.mWifiInjector.getWifiPermissionsUtil().checkCallersLocationPermission(clientInfo.mPackageName, clientInfo.mUid);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump RttService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission " + "android.permission.DUMP");
                return;
            }
            pw.println("current state: " + this.mStateMachine.currentState());
            pw.println("clients:");
            synchronized (this.mLock) {
                for (ClientInfo client : this.mClients.values()) {
                    pw.println("  " + client);
                }
            }
        }

        RttRequest issueNextRequest() {
            while (!this.mRequestQueue.isEmpty()) {
                RttRequest request = (RttRequest) this.mRequestQueue.remove();
                if (request != null) {
                    if (this.mWifiNative.requestRtt(request.params, this.mEventHandler)) {
                        Log.d(RttService.TAG, "Issued next RTT request with key: " + request.key);
                        return request;
                    }
                    Log.e(RttService.TAG, "Fail to issue key at native layer");
                    request.ci.reportFailed(request, -1, "Failed to start");
                }
            }
            Log.d(RttService.TAG, "No more requests left");
            return null;
        }

        public RttCapabilities getRttCapabilities() {
            this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access rtt capabilities");
            return this.mWifiNative.getRttCapabilities();
        }
    }

    public RttService(Context context) {
        super(context);
        this.mHandlerThread.start();
        Log.i(TAG, "Creating rttmanager");
    }

    public void onStart() {
        this.mImpl = new RttServiceImpl(getContext(), this.mHandlerThread.getLooper(), WifiInjector.getInstance());
        Log.i(TAG, "Starting rttmanager");
        publishBinderService("rttmanager", this.mImpl);
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            Log.i(TAG, "Registering rttmanager");
            if (this.mImpl == null) {
                this.mImpl = new RttServiceImpl(getContext(), this.mHandlerThread.getLooper(), WifiInjector.getInstance());
            }
            this.mImpl.startService();
        }
    }
}
