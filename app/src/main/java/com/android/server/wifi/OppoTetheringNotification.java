package com.android.server.wifi;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;

public class OppoTetheringNotification {
    public static final int BLUETOOTH_ONLY_TETHERED = 1;
    private static boolean DBG = false;
    public static final String EXTRA_TETHER_STATE_STATUS = "ExtraTetherStateStatus";
    public static final int NO_INTERFACE_TETHERED = 0;
    private static final String TAG = "OppoTetheringNotification";
    public static final int USB_ONLY_TETHERED = 2;
    public static final int WIFI_ONLY_TETHERED = 4;
    private TetherNotificationReceiver mBroadcastReceiver;
    private Context mContext;
    private int mHotspotClientNums = 0;
    private int mWhatInterfaceTethering = 0;

    private class TetherNotificationReceiver extends BroadcastReceiver {
        /* synthetic */ TetherNotificationReceiver(OppoTetheringNotification this$0, TetherNotificationReceiver -this1) {
            this();
        }

        private TetherNotificationReceiver() {
        }

        public void onReceive(Context content, Intent intent) {
            OppoTetheringNotification.this.Logd(" onReceive: intent: " + intent);
            String action = intent.getAction();
            if (action.equals("android.net.conn.TETHER_STATE_CHANGED")) {
                OppoTetheringNotification.this.mWhatInterfaceTethering = intent.getIntExtra(OppoTetheringNotification.EXTRA_TETHER_STATE_STATUS, 0);
                OppoTetheringNotification.this.handleTetherStateChanged();
            } else if (action.equals("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED")) {
                OppoTetheringNotification.this.mHotspotClientNums = intent.getIntExtra("HotspotClientNum", 0);
                OppoTetheringNotification.this.handleHotspotClientChanged();
            } else if (action.equals("android.intent.action.LOCALE_CHANGED")) {
                OppoTetheringNotification.this.handleLocaleChanged();
            } else {
                Log.e(OppoTetheringNotification.TAG, " unknown action received: " + action);
            }
        }
    }

    public OppoTetheringNotification(Context context) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED");
        filter.addAction("android.net.conn.TETHER_STATE_CHANGED");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        this.mBroadcastReceiver = new TetherNotificationReceiver();
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    private void handleTetherStateChanged() {
        switch (this.mWhatInterfaceTethering) {
            case 0:
                cancelTetheredNotification();
                return;
            case 1:
                oppoShowTetheredNotification(201852112);
                return;
            case 2:
                oppoShowTetheredNotification(201852004);
                return;
            case 4:
                showSoftApOrWifiSharingNotification();
                return;
            default:
                oppoShowTetheredNotification(201852002);
                return;
        }
    }

    private void handleHotspotClientChanged() {
        if (this.mWhatInterfaceTethering == 4) {
            showSoftApOrWifiSharingNotification();
        }
    }

    private void showSoftApOrWifiSharingNotification() {
        if (WifiInjector.getInstance().getOppoWifiSharingManager().isWifiSharingTethering()) {
            oppoShowTetheredNotification(201852208);
        } else {
            oppoShowTetheredNotification(201852003);
        }
    }

    private void handleLocaleChanged() {
        if (this.mWhatInterfaceTethering != 0) {
            handleTetherStateChanged();
        }
    }

    private void oppoShowTetheredNotification(int notificationType) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager != null) {
            int largeIcon;
            CharSequence title;
            switch (notificationType) {
                case 201852003:
                    largeIcon = 201852114;
                    break;
                case 201852004:
                    largeIcon = 201852116;
                    break;
                case 201852112:
                    largeIcon = 201852115;
                    break;
                case 201852208:
                    largeIcon = 201852208;
                    break;
                default:
                    largeIcon = 201852117;
                    break;
            }
            Intent intent = new Intent();
            Resources r = Resources.getSystem();
            if (notificationType == 201852003) {
                intent.setAction("android.settings.OPPO_WIFI_AP_SETTINGS");
                if (this.mHotspotClientNums == 0) {
                    title = r.getText(201590070);
                } else {
                    title = this.mHotspotClientNums + r.getText(201590071).toString();
                }
            } else if (notificationType == 201852208) {
                intent.setAction("oppo.settings.WLAN_SHARE_SETTINGS");
                if (this.mHotspotClientNums == 0) {
                    title = r.getText(201590162);
                } else {
                    title = this.mHotspotClientNums + r.getText(201590163).toString();
                }
            } else {
                intent.setAction("android.settings.OPPO_TETHER_SETTINGS");
                title = r.getText(201590072);
            }
            intent.setFlags(1073741824);
            PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
            CharSequence message = r.getText(17040955);
            Notification mTeNotification = new Builder(this.mContext, SystemNotificationChannels.NETWORK_STATUS).setContentTitle(title).setContentText(message).setTicker(title).setContentIntent(pi).setSmallIcon(201852197).setLargeIcon(BitmapFactory.decodeResource(this.mContext.getResources(), largeIcon)).build();
            mTeNotification.flags = 2;
            mTeNotification.defaults &= -2;
            Logd("notify teteredNotification ticker: " + title);
            notificationManager.notify(0, mTeNotification);
        }
    }

    private void cancelTetheredNotification() {
        NotificationManager mNM = (NotificationManager) this.mContext.getSystemService("notification");
        if (mNM != null) {
            mNM.cancel(0);
        } else {
            Log.e(TAG, " failed to get NotificationManager!");
        }
        this.mHotspotClientNums = 0;
        this.mWhatInterfaceTethering = 0;
    }

    private void Logd(String whatToLog) {
        if (DBG) {
            Log.d(TAG, whatToLog);
        }
    }

    public void enableLogging(boolean enable) {
        DBG = enable;
    }
}
