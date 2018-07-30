package com.android.server.wifi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.OppoAssertTip;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.WifiRomUpdateHelper;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import oppo.util.OppoStatistics;

public class OppoSlienceReCovery {
    private static final String ACTION_DETECT_NETWORK_ACCESS = "com.android.server.WifiManager.action.DETECT_NET_ACCESS";
    private static final String ACTION_RECOVERY_WIFI = "com.android.server.WifiManager.action.RECOVERY_WIFI";
    private static boolean DEBUG = true;
    private static final int OPPO_SLIENCI_RECOVERY = 8;
    private static final String TAG = "OppoSlienceReCovery";
    private static final int TRAFFIC_RX_LOWEST = 80;
    private static final int TRAFFIC_TX_LOWEST = 50;
    private AlarmManager mAlarmManager;
    private OppoAssertTip mAssertProxy;
    private Context mContext;
    private PendingIntent mDetectIntent;
    private Excecuter mExcecuter;
    private Handler mHandler;
    private Boolean mNetWorkConnected = Boolean.valueOf(false);
    private PacketInfo mPktInfo;
    private PendingIntent mRecoveryIntent;
    private int mResetAlarmCount = 0;
    private boolean mScreenOn = true;
    private PendingIntent mStartIntent;
    private PendingIntent mStopIntent;
    private boolean mTestMode = false;
    private WifiInjector mWifiInjector;
    private WifiManager mWifiManager;
    private WifiNative mWifiNative;
    private WifiRomUpdateHelper mWifiRomUpdateHelper = null;

    class Excecuter {
        private static final int DAYS_THIRD = 3;
        private static final int DAYS_TODAY = 0;
        private static final int DAYS_TOMORROW = 1;

        Excecuter() {
        }

        void startRecoveryTrigger(int ways) {
            Log.d(OppoSlienceReCovery.TAG, "startRecoveryTrigger ways = " + ways + " mTestMode=" + OppoSlienceReCovery.this.mTestMode);
            if (OppoSlienceReCovery.this.mTestMode) {
                Calendar ca = Calendar.getInstance();
                OppoSlienceReCovery.this.mAlarmManager.set(0, caculateTimeIntoMillis(0, ca.get(11), ca.get(12) + 1), OppoSlienceReCovery.this.mRecoveryIntent);
                return;
            }
            if (ways == 3) {
                OppoSlienceReCovery.this.mAlarmManager.set(0, caculateTimeIntoMillis(3, getRandomHour(), getRandomMin()), OppoSlienceReCovery.this.mRecoveryIntent);
            } else {
                OppoSlienceReCovery.this.mAlarmManager.set(0, caculateTimeIntoMillis(1, getRandomHour(), getRandomMin()), OppoSlienceReCovery.this.mRecoveryIntent);
            }
        }

        void detectWifi() {
            OppoSlienceReCovery.this.mAlarmManager.set(0, caculateTimeIntoMillis(0, 0, 1), OppoSlienceReCovery.this.mRecoveryIntent);
        }

        void cancelTrigger() {
            Log.d(OppoSlienceReCovery.TAG, "cancelTrigger ");
            OppoSlienceReCovery.this.mAlarmManager.cancel(OppoSlienceReCovery.this.mRecoveryIntent);
        }

        private long caculateTimeIntoMillis(int days, int hours, int mins) {
            Calendar cal = Calendar.getInstance();
            if (days < 0) {
                cal.add(5, 1);
            } else {
                cal.add(5, days);
            }
            if (hours < 0 || hours > 6) {
                cal.set(11, 1);
            } else {
                cal.set(11, hours);
            }
            cal.set(12, mins);
            cal.set(13, 0);
            if (OppoSlienceReCovery.DEBUG) {
                Log.d(OppoSlienceReCovery.TAG, "caculateTimeIntoMillis: " + cal.getTime());
            }
            return cal.getTimeInMillis();
        }

        private int getRandomHour() {
            int ret = new Random().nextInt(4) + 1;
            if (OppoSlienceReCovery.DEBUG) {
                Log.d(OppoSlienceReCovery.TAG, "fool-proof, start=" + 1 + " end=" + 5 + " random=" + ret);
            }
            return ret;
        }

        private int getRandomMin() {
            int ret = new Random().nextInt(60) + 0;
            if (OppoSlienceReCovery.DEBUG) {
                Log.d(OppoSlienceReCovery.TAG, "fool-proof, start=" + 0 + " end=" + 60 + " random=" + ret);
            }
            return ret;
        }

        private void resetRestartAlarm() {
            long detectInterval = System.currentTimeMillis() + 3600000;
            if (OppoSlienceReCovery.DEBUG) {
                Log.d(OppoSlienceReCovery.TAG, "fool-proof,reset alarm count = " + OppoSlienceReCovery.this.mResetAlarmCount);
            }
            OppoSlienceReCovery oppoSlienceReCovery = OppoSlienceReCovery.this;
            int -get5 = oppoSlienceReCovery.mResetAlarmCount;
            oppoSlienceReCovery.mResetAlarmCount = -get5 + 1;
            if (-get5 >= 3) {
                if (OppoSlienceReCovery.DEBUG) {
                    Log.d(OppoSlienceReCovery.TAG, "fool-proof,reset alarm next night!");
                }
                OppoSlienceReCovery.this.mResetAlarmCount = 0;
                OppoSlienceReCovery.this.mAlarmManager.set(0, caculateTimeIntoMillis(1, getRandomHour(), getRandomMin()), OppoSlienceReCovery.this.mRecoveryIntent);
                return;
            }
            OppoSlienceReCovery.this.mAlarmManager.set(0, detectInterval, OppoSlienceReCovery.this.mRecoveryIntent);
        }
    }

    public class PacketInfo {
        public long rxPkts = 0;
        public long txPkts = 0;
    }

    OppoSlienceReCovery(Context c, WifiInjector injector, WifiNative wn) {
        this.mContext = c;
        this.mWifiNative = wn;
        this.mWifiInjector = injector;
        this.mPktInfo = new PacketInfo();
        this.mExcecuter = new Excecuter();
        this.mAssertProxy = OppoAssertTip.getInstance();
        this.mAlarmManager = (AlarmManager) c.getSystemService("alarm");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mRecoveryIntent = PendingIntent.getBroadcast(c, 0, new Intent(ACTION_RECOVERY_WIFI, null), 268435456);
        this.mDetectIntent = PendingIntent.getBroadcast(c, 0, new Intent(ACTION_DETECT_NETWORK_ACCESS, null), 268435456);
        this.mWifiRomUpdateHelper = new WifiRomUpdateHelper(this.mContext);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction(ACTION_RECOVERY_WIFI);
        filter.addAction("android.net.wifi.STATE_CHANGE");
        c.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(OppoSlienceReCovery.TAG, "action" + action);
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    OppoSlienceReCovery.this.mScreenOn = true;
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    OppoSlienceReCovery.this.mScreenOn = false;
                } else if (action.equals(OppoSlienceReCovery.ACTION_RECOVERY_WIFI)) {
                    OppoSlienceReCovery.this.recoveyWifi();
                } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    DetailedState state = ((NetworkInfo) intent.getParcelableExtra("networkInfo")).getDetailedState();
                    Log.d(OppoSlienceReCovery.TAG, "wifi connect state =" + state);
                    if (state == DetailedState.CONNECTED) {
                        OppoSlienceReCovery.this.mNetWorkConnected = Boolean.valueOf(true);
                    } else if (state == DetailedState.DISCONNECTED) {
                        OppoSlienceReCovery.this.mNetWorkConnected = Boolean.valueOf(false);
                    } else {
                        Log.d(OppoSlienceReCovery.TAG, "wifi connect other state");
                    }
                } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    int wifiState = intent.getIntExtra("wifi_state", 4);
                    Log.d(OppoSlienceReCovery.TAG, "wifi wifiState = " + wifiState);
                    if (wifiState == 3) {
                        OppoSlienceReCovery.this.enableFoolProof(true);
                    } else if (wifiState == 1) {
                        OppoSlienceReCovery.this.enableFoolProof(false);
                    } else {
                        Log.d(OppoSlienceReCovery.TAG, "wifi other state");
                    }
                }
            }
        }, filter);
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.d(OppoSlienceReCovery.TAG, "handleMessage msg.what = " + msg.what + " msg.obj=" + ((Boolean) msg.obj).booleanValue());
                if (msg.what == 8) {
                    OppoSlienceReCovery.this.netWorkRestart(((Boolean) msg.obj).booleanValue());
                }
            }
        };
    }

    public Integer getRomUpdateIntegerValue(String key, Integer defaultVal) {
        if (this.mWifiRomUpdateHelper != null) {
            return this.mWifiRomUpdateHelper.getIntegerValue(key, defaultVal);
        }
        return defaultVal;
    }

    void enableFoolProof(boolean enable) {
        this.mResetAlarmCount = 0;
        if (getRomUpdateIntegerValue("BASIC_FOOL_PROOF_ON", Integer.valueOf(1)).intValue() != 1) {
            Log.d(TAG, "foolProofOn != 1, don't restart!");
            return;
        }
        Log.d(TAG, "enableFoolProof enable = " + enable);
        if (enable) {
            Log.d(TAG, "enableFoolProof  start trigger ");
            this.mExcecuter.startRecoveryTrigger(3);
        } else {
            this.mExcecuter.cancelTrigger();
        }
    }

    boolean ifPermitRecovery() {
        if (checkTimeInMorning() && !this.mScreenOn && (this.mTestMode ^ 1) == 0) {
            return true;
        }
        return false;
    }

    void recoveyWifi() {
        if (!ifPermitRecovery()) {
            this.mExcecuter.startRecoveryTrigger(1);
        } else if (this.mNetWorkConnected.booleanValue()) {
            hasNetworkAccessing();
        } else {
            Log.e(TAG, "silence recovery ,wifi will recoery later... ... ");
            setStatistics("silence", "wifi_restart_in_silence");
            this.mWifiInjector.getSelfRecovery().trigger(0);
        }
    }

    private boolean checkTimeInMorning() {
        if (this.mTestMode) {
            return true;
        }
        int hour = Calendar.getInstance().get(11);
        return hour < 6 && hour > 0;
    }

    public void setStatistics(String mapValue, String eventId) {
        HashMap<String, String> map = new HashMap();
        map.put("mapKey-", mapValue);
        Log.d(TAG, "fool-proof, onCommon eventId = " + eventId);
        OppoStatistics.onCommon(this.mContext, "wifi_fool_proof", eventId, map, false);
    }

    public void reportFoolProofException() {
        if (this.mContext.getPackageManager().hasSystemFeature("oppo.cta.support")) {
            Log.d(TAG, "fool-proof, CTA version don't reportFoolProofException");
            return;
        }
        if (getLoggingLevel() == 0) {
            this.mWifiManager.enableVerboseLogging(1);
        }
        RuntimeException excp = new RuntimeException("Please send this log to Yuanliu.Tang of wifi team,thank you!");
        excp.fillInStackTrace();
        this.mAssertProxy.requestShowAssertMessage(Log.getStackTraceString(excp));
    }

    private void netWorkRestart(boolean enable) {
        if (enable && ifPermitRecovery()) {
            Log.d(TAG, "silence recovery network connect wifi wifi will recovery ");
            setStatistics("silence_connected", "wifi_restart_in_silence_connected");
            this.mWifiInjector.getSelfRecovery().trigger(0);
            return;
        }
        Log.d(TAG, "netWorkRestart later do---tomorrow");
        this.mExcecuter.resetRestartAlarm();
    }

    private void hasNetworkAccessing() {
        new Thread() {
            public void run() {
                PacketInfo pktInfo = new PacketInfo();
                boolean result = false;
                String interFace = OppoSlienceReCovery.this.mWifiNative.getInterfaceName();
                super.run();
                int i = 0;
                while (i < 60) {
                    if (OppoSlienceReCovery.this.mNetWorkConnected.booleanValue()) {
                        long txPkts = TrafficStats.getTxPackets(interFace);
                        long rxPkts = TrafficStats.getRxPackets(interFace);
                        if (OppoSlienceReCovery.DEBUG) {
                            Log.d(OppoSlienceReCovery.TAG, "hasNetworkAccessing count = " + i + " txPkts = " + txPkts + " rxPkts = " + rxPkts + " tcpTx = " + TrafficStats.getTcpTxPackets(interFace) + " tcpRx = " + TrafficStats.getTcpRxPackets(interFace));
                        }
                        if (i == 0) {
                            pktInfo.txPkts = txPkts;
                            pktInfo.rxPkts = rxPkts;
                            result = true;
                        } else if (rxPkts - pktInfo.rxPkts >= 80 || txPkts - pktInfo.txPkts >= 50) {
                            result = false;
                            break;
                        } else {
                            result = true;
                        }
                        try {
                            AnonymousClass3.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i++;
                    } else {
                        Log.e(OppoSlienceReCovery.TAG, "networkdisconnected return ");
                        return;
                    }
                }
                Message msg = Message.obtain();
                msg.what = 8;
                msg.obj = Boolean.valueOf(result);
                OppoSlienceReCovery.this.mHandler.sendMessage(msg);
            }
        }.start();
    }

    int getLoggingLevel() {
        return SystemProperties.getBoolean(WifiStateMachine.DEBUG_PROPERTY, false) ? 1 : 0;
    }
}
