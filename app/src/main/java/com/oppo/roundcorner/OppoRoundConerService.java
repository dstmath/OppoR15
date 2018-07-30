package com.oppo.roundcorner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.oppo.roundcorner.IOppoRoundConerService.Stub;

public class OppoRoundConerService extends Stub {
    private static final String ACTION_CONFIGURATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    public static final boolean DEBUG = true;
    public static final String TAG = "OPPORoundConer";
    private BroadcastReceiver mConfigureChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("OPPORoundConer", "onReceive, and action is: " + action);
            if (OppoRoundConerService.ACTION_CONFIGURATION_CHANGED.equals(action)) {
                int tempOrient = OppoRoundConerWindowManager.getWindowManager(OppoRoundConerService.this.mContext).getDefaultDisplay().getRotation();
                Log.d("OPPORoundConer", "tempOrient= " + tempOrient + " mOrientation= " + OppoRoundConerService.this.mOrientation);
                if (tempOrient != OppoRoundConerService.this.mOrientation) {
                    OppoRoundConerService.this.mOrientation = tempOrient;
                    OppoRoundConerWindowManager.setOrientation(OppoRoundConerService.this.mOrientation);
                    OppoRoundConerWindowManager.updateLayout(OppoRoundConerService.this.mContext);
                }
            }
        }
    };
    private Context mContext;
    private int mOrientation = 0;

    public OppoRoundConerService(Context context) {
        this.mContext = context;
        OppoRoundConerWindowManager.getScreenInfo(this.mContext);
    }

    public void init() {
        OppoRoundConerWindowManager.createPortraitWindow(this.mContext);
        OppoRoundConerWindowManager.createLandscapeWindow(this.mContext);
        OppoRoundConerWindowManager.updateLayout(this.mContext);
        registerUsbStateReceiver();
    }

    private void registerUsbStateReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CONFIGURATION_CHANGED);
        this.mContext.registerReceiver(this.mConfigureChangeReceiver, intentFilter);
    }
}
