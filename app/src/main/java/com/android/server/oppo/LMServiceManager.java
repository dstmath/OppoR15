package com.android.server.oppo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.app.ILMServiceManager.Stub;
import com.android.server.LocationManagerService;
import com.oppo.hypnus.HypnusManager;
import com.oppo.luckymoney.LuckyMoneyHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LMServiceManager {
    private static final int ACTION_BURST_ANR = 19;
    private static boolean DEBUG = false;
    private static final String TAG = "LMServiceManager";
    private IBinder mBinder = new Stub() {
        public void enableBoost(int pid, int uid, int timeout, int code) {
            Message msg = LMServiceManager.this.mHandler.obtainMessage();
            msg.what = 0;
            msg.arg1 = timeout;
            msg.arg2 = pid;
            msg.sendToTarget();
        }

        public String getLuckyMoneyInfo(int type) {
            return LMServiceManager.this.mLMHelper.getLuckyMoneyInfo(type);
        }

        public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            if (LMServiceManager.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                writer.println("Permission Denial: can't dump PowerManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            if (LMServiceManager.DEBUG) {
                Slog.d(LMServiceManager.TAG, "dump, args=" + args);
            }
            if (args.length >= 1) {
                if ("debug".equals(args[0])) {
                    if (args.length == 2) {
                        LMServiceManager.DEBUG = LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON.equals(args[1]);
                    } else {
                        writer.println("Invalid argument! Get detail help as bellow:");
                    }
                }
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                LMServiceManager.this.dumpInternal(writer);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    };
    private Context mContext;
    private HypnusManager mHM = null;
    private LMServiceThreadHandler mHandler;
    private LuckyMoneyHelper mLMHelper = null;
    private final Object mLock = new Object();
    private long mMaintainTimeout = 0;
    private SparseArray<Object> mPidArray = new SparseArray(0);
    private PowerManager mPowerManager = null;
    private WifiManager mWifiManger = null;

    private class LMServiceThreadHandler extends Handler {
        private static final int MESSAGE_BOOST_DISABLE = 1;
        private static final int MESSAGE_BOOST_ENABLE_TIMEOUT = 0;
        private static final int MESSAGE_BOOST_MAINTAIN = 2;
        private static final int MESSAGE_SCREEN_OFF = 4;
        private static final int MESSAGE_SCREEN_ON = 3;
        private static final int MESSAGE_WIFI_CONECTED = 5;
        private static final int MESSAGE_WIFI_DISCONECTED = 6;
        private boolean mIsWifiConected = false;

        public LMServiceThreadHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case 0:
                        long timeout = (long) msg.arg1;
                        int pid = msg.arg2;
                        boolean isInteractive = LMServiceManager.this.mPowerManager.isInteractive();
                        if (LMServiceManager.DEBUG) {
                            Slog.d(LMServiceManager.TAG, "isInteractive: " + isInteractive);
                        }
                        synchronized (LMServiceManager.this.mLock) {
                            if (isInteractive) {
                                Object tmp;
                                Message timeoutMsg;
                                if (LMServiceManager.this.mPidArray.indexOfKey(pid) >= 0) {
                                    tmp = LMServiceManager.this.mPidArray.get(pid);
                                    LMServiceManager.this.mHandler.removeMessages(1, tmp);
                                    timeoutMsg = LMServiceManager.this.mHandler.obtainMessage();
                                    timeoutMsg.what = 1;
                                    timeoutMsg.arg1 = pid;
                                    timeoutMsg.obj = tmp;
                                    LMServiceManager.this.mHandler.sendMessageDelayed(timeoutMsg, timeout);
                                } else {
                                    tmp = new Object();
                                    LMServiceManager.this.mPidArray.put(pid, tmp);
                                    timeoutMsg = LMServiceManager.this.mHandler.obtainMessage();
                                    timeoutMsg.what = 1;
                                    timeoutMsg.arg1 = pid;
                                    timeoutMsg.obj = tmp;
                                    LMServiceManager.this.mHandler.sendMessageDelayed(timeoutMsg, timeout);
                                }
                                LMServiceManager.this.enableBoostLocked(pid, timeout);
                            }
                        }
                        LMServiceManager.this.mHandler.removeMessages(2);
                        int delayTimeout = LMServiceManager.this.mLMHelper.getDelayTimeout();
                        LMServiceManager.this.mMaintainTimeout = SystemClock.elapsedRealtime() + ((long) delayTimeout);
                        if (LMServiceManager.DEBUG) {
                            Slog.d(LMServiceManager.TAG, "wifi power save delayTimeout: " + delayTimeout);
                        }
                        if (delayTimeout > 0 && isInteractive && this.mIsWifiConected) {
                            LMServiceManager.this.enableMaintainLocked();
                            LMServiceManager.this.mHandler.sendEmptyMessageDelayed(2, (long) delayTimeout);
                            return;
                        }
                        return;
                    case 1:
                        synchronized (LMServiceManager.this.mLock) {
                            int iPid = msg.arg1;
                            LMServiceManager.this.mPidArray.delete(iPid);
                            LMServiceManager.this.disableBoostLocked(iPid);
                        }
                        return;
                    case 2:
                        LMServiceManager.this.disableMaintain();
                        return;
                    case 3:
                        LMServiceManager.this.mHandler.removeMessages(2);
                        int remainTimeout = (int) (LMServiceManager.this.mMaintainTimeout - SystemClock.elapsedRealtime());
                        if (LMServiceManager.DEBUG) {
                            Slog.d(LMServiceManager.TAG, "SCREEN_ON wifi power save remainTimeout: " + remainTimeout);
                        }
                        if (remainTimeout > 0 && this.mIsWifiConected) {
                            LMServiceManager.this.enableMaintainLocked();
                            LMServiceManager.this.mHandler.sendEmptyMessageDelayed(2, (long) remainTimeout);
                            return;
                        }
                        return;
                    case 4:
                        LMServiceManager.this.mHandler.removeMessages(2);
                        if (LMServiceManager.DEBUG) {
                            Slog.d(LMServiceManager.TAG, "SCREEN_OFF wifi power save");
                        }
                        LMServiceManager.this.disableMaintain();
                        return;
                    case 5:
                        if (LMServiceManager.DEBUG) {
                            Slog.d(LMServiceManager.TAG, "wifi CONNECTED.");
                        }
                        this.mIsWifiConected = true;
                        if (LMServiceManager.this.mPowerManager.isInteractive()) {
                            LMServiceManager.this.mHandler.sendEmptyMessage(3);
                            return;
                        } else {
                            LMServiceManager.this.mHandler.sendEmptyMessage(4);
                            return;
                        }
                    case 6:
                        if (LMServiceManager.DEBUG) {
                            Slog.d(LMServiceManager.TAG, "wifi DISCONNECTED.");
                        }
                        if (((int) (LMServiceManager.this.mMaintainTimeout - SystemClock.elapsedRealtime())) > 0 && this.mIsWifiConected) {
                            if (LMServiceManager.DEBUG) {
                                Slog.d(LMServiceManager.TAG, "During LM window, enable wifi power save.");
                            }
                            LMServiceManager.this.mHandler.removeMessages(2);
                            LMServiceManager.this.disableMaintain();
                        }
                        this.mIsWifiConected = false;
                        return;
                    default:
                        return;
                }
            } catch (NullPointerException e) {
                Slog.d(LMServiceManager.TAG, "Exception in LMServiceThreadHandler.handleMessage: " + e);
            }
            Slog.d(LMServiceManager.TAG, "Exception in LMServiceThreadHandler.handleMessage: " + e);
        }
    }

    private static native int nativeRaisePriorityDisable(int i);

    private static native int nativeRaisePriorityEnable(int i);

    public LMServiceManager(Context context, Handler mainHandler) {
        this.mContext = context;
        this.mHandler = new LMServiceThreadHandler(mainHandler.getLooper());
        this.mLMHelper = new LuckyMoneyHelper(this.mContext);
        this.mHM = new HypnusManager();
        this.mWifiManger = (WifiManager) this.mContext.getSystemService("wifi");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (((int) (LMServiceManager.this.mMaintainTimeout - SystemClock.elapsedRealtime())) <= 0) {
                    return;
                }
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    LMServiceManager.this.mHandler.sendEmptyMessage(3);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    LMServiceManager.this.mHandler.sendEmptyMessage(4);
                }
            }
        }, filter);
        filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
                    DetailedState state = ((NetworkInfo) intent.getParcelableExtra("networkInfo")).getDetailedState();
                    if (state == DetailedState.CONNECTED) {
                        LMServiceManager.this.mHandler.sendEmptyMessage(5);
                    } else if (state == DetailedState.DISCONNECTED) {
                        LMServiceManager.this.mHandler.sendEmptyMessage(6);
                    }
                }
            }
        }, filter);
        initService();
    }

    private void initService() {
        try {
            Slog.i(TAG, "Start Service");
            ServiceManager.addService("luckymoney", this.mBinder);
        } catch (Throwable e) {
            Slog.i(TAG, "Start Service failed", e);
        }
    }

    public void systemReady() {
        this.mLMHelper.initUpdateBroadcastReceiver();
    }

    public String dumpToString() {
        return this.mLMHelper.dumpToString();
    }

    private void dumpInternal(PrintWriter pw) {
        pw.println(dumpToString());
    }

    private void enableBoostLocked(int pid, long timeout) {
        nativeRaisePriorityEnable(pid);
        if (this.mHM != null) {
            this.mHM.hypnusSetAction(19, (int) timeout);
        }
    }

    private void disableBoostLocked(int pid) {
        nativeRaisePriorityDisable(pid);
    }

    private void enableMaintainLocked() {
        if (DEBUG) {
            Slog.d(TAG, "wifi power save: false");
        }
        if (this.mWifiManger != null) {
            this.mWifiManger.setPowerSavingMode(false);
        }
    }

    private void disableMaintain() {
        if (DEBUG) {
            Slog.d(TAG, "wifi power save: true");
        }
        if (this.mWifiManger != null) {
            this.mWifiManger.setPowerSavingMode(true);
        }
    }
}
