package com.android.server.wifi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.os.SystemClock;
import android.provider.Settings.System;
import android.util.Log;
import java.util.ArrayList;

public class OppoHostapdPowerSave {
    private static final String ACTION_STOP_HOTSPOT = "com.android.server.WifiManager.action.STOP_HOTSPOT";
    private static final long HOTSPOT_DISABLE_MS = 300000;
    private static final String TAG = "OppoHostapdPowerSave";
    private AlarmManager mAlarmManager;
    private Context mContext;
    private int mDuration = 2;
    private PendingIntent mIntentStopHotspot;
    private int mWifiApState;
    private WifiController mWifiController;

    OppoHostapdPowerSave(Context conext, WifiController controller) {
        this.mContext = conext;
        this.mWifiController = controller;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        initializeHotspotExtra();
    }

    private void initializeHotspotExtra() {
        this.mIntentStopHotspot = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_STOP_HOTSPOT), 0);
        this.mDuration = System.getInt(this.mContext.getContentResolver(), "wifi_hotspot_auto_disable", 2);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_STOP_HOTSPOT);
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intentFilter.addAction("codeaurora.net.conn.TETHER_CONNECT_STATE_CHANGED");
        if (WifiInjector.getInstance().getOppoWifiSharingManager().isWifiSharingSupported()) {
            intentFilter.addAction("oppo.intent.action.wifi.WIFI_SHARING_STATE_CHANGED");
        }
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(OppoHostapdPowerSave.TAG, "Got receive , action:" + action);
                if (action.equals(OppoHostapdPowerSave.ACTION_STOP_HOTSPOT)) {
                    OppoWifiSharingManager sharingManager = WifiInjector.getInstance().getOppoWifiSharingManager();
                    if (sharingManager.isWifiSharingEnabledState()) {
                        sharingManager.setWifiClosedByUser(true);
                    }
                    OppoHostapdPowerSave.this.stopSoftAp();
                } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED") || action.equals("oppo.intent.action.wifi.WIFI_SHARING_STATE_CHANGED")) {
                    OppoHostapdPowerSave.this.mWifiApState = intent.getIntExtra("wifi_state", 11);
                    if (OppoHostapdPowerSave.this.mWifiApState == 13 || OppoHostapdPowerSave.this.mWifiApState == StatusCode.UNKNOWN_AUTHENTICATION_SERVER) {
                        OppoHostapdPowerSave.this.startPowerSaveAlarm();
                    } else if (OppoHostapdPowerSave.this.mWifiApState == 11 || OppoHostapdPowerSave.this.mWifiApState == 111) {
                        OppoHostapdPowerSave.this.canclePowerSaveAlarm(false);
                    } else {
                        Log.e(OppoHostapdPowerSave.TAG, "other state no need do someting , mWifiApState:" + OppoHostapdPowerSave.this.mWifiApState);
                    }
                } else if (!action.equals("codeaurora.net.conn.TETHER_CONNECT_STATE_CHANGED")) {
                } else {
                    if (OppoHostapdPowerSave.this.mWifiApState != 13 && OppoHostapdPowerSave.this.mWifiApState != StatusCode.UNKNOWN_AUTHENTICATION_SERVER) {
                        return;
                    }
                    if (OppoHostapdPowerSave.this.getSoftApConnectedNum() > 0) {
                        OppoHostapdPowerSave.this.canclePowerSaveAlarm(true);
                    } else {
                        OppoHostapdPowerSave.this.startPowerSaveAlarm();
                    }
                }
            }
        }, intentFilter);
    }

    private void canclePowerSaveAlarm(boolean needCheck) {
        if (this.mDuration == 0) {
            Log.e(TAG, "canclePowerSaveAlarm failed mDuration = " + this.mDuration);
        } else if (!needCheck) {
            Log.d(TAG, "canclePowerSaveAlarm direct ");
            this.mAlarmManager.cancel(this.mIntentStopHotspot);
        } else if (getSoftApConnectedNum() > 0) {
            this.mAlarmManager.cancel(this.mIntentStopHotspot);
            Log.d(TAG, "canclePowerSaveAlarm ");
        } else {
            Log.e(TAG, "canclePowerSaveAlarm failed hava ap connected num is = " + getSoftApConnectedNum());
        }
    }

    private void startPowerSaveAlarm() {
        if (this.mDuration != 0 && getSoftApConnectedNum() == 0) {
            Log.d(TAG, "startPowerSaveAlarm, mDuration:" + this.mDuration);
            this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + (((long) this.mDuration) * HOTSPOT_DISABLE_MS), this.mIntentStopHotspot);
        }
    }

    private int getSoftApConnectedNum() {
        return ((ArrayList) OppoHotspotClientInfo.getInstance(this.mContext).getConnectedStations()).size();
    }

    private void stopSoftAp() {
        Log.d(TAG, "stopSoftAp----ten min softap not use,so stop ");
        this.mWifiController.sendMessage(155658, 0, 0);
    }
}
