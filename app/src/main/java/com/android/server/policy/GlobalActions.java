package com.android.server.policy;

import android.content.Context;
import android.os.Handler;
import android.view.WindowManagerPolicy.WindowManagerFuncs;
import com.android.server.LocalServices;
import com.android.server.face.FaceDaemonWrapper;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.statusbar.StatusBarManagerInternal.GlobalActionsListener;

class GlobalActions implements GlobalActionsListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "GlobalActions";
    private final Context mContext;
    private boolean mDeviceProvisioned;
    private final Handler mHandler;
    private boolean mKeyguardShowing;
    private LegacyGlobalActions mLegacyGlobalActions;
    private final Runnable mShowTimeout = new Runnable() {
        public void run() {
            GlobalActions.this.ensureLegacyCreated();
            GlobalActions.this.mLegacyGlobalActions.showDialog(GlobalActions.this.mKeyguardShowing, GlobalActions.this.mDeviceProvisioned);
        }
    };
    private boolean mShowing;
    private boolean mStatusBarConnected;
    private final StatusBarManagerInternal mStatusBarInternal;
    private final WindowManagerFuncs mWindowManagerFuncs;

    public GlobalActions(Context context, WindowManagerFuncs windowManagerFuncs) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mStatusBarInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        if (this.mStatusBarInternal != null) {
            this.mStatusBarInternal.setGlobalActionsListener(this);
        }
    }

    private void ensureLegacyCreated() {
        if (this.mLegacyGlobalActions == null) {
            this.mLegacyGlobalActions = new LegacyGlobalActions(this.mContext, this.mWindowManagerFuncs, new -$Lambda$pV_TcBBXJOcgD8CpVRVZuDc_ff8((byte) 0, this));
        }
    }

    /* synthetic */ void -com_android_server_policy_GlobalActions-mthref-0() {
        onGlobalActionsDismissed();
    }

    public void showDialog(boolean keyguardShowing, boolean deviceProvisioned) {
        this.mKeyguardShowing = keyguardShowing;
        this.mDeviceProvisioned = deviceProvisioned;
        this.mShowing = true;
        if (this.mStatusBarConnected) {
            this.mStatusBarInternal.showGlobalActions();
            this.mHandler.postDelayed(this.mShowTimeout, FaceDaemonWrapper.TIMEOUT_FACED_BINDERCALL_CHECK);
            return;
        }
        ensureLegacyCreated();
        this.mLegacyGlobalActions.showDialog(this.mKeyguardShowing, this.mDeviceProvisioned);
    }

    public void onGlobalActionsShown() {
        this.mHandler.removeCallbacks(this.mShowTimeout);
    }

    public void onGlobalActionsDismissed() {
        this.mShowing = false;
    }

    public void onStatusBarConnectedChanged(boolean connected) {
        this.mStatusBarConnected = connected;
        if (this.mShowing && (this.mStatusBarConnected ^ 1) != 0) {
            ensureLegacyCreated();
            this.mLegacyGlobalActions.showDialog(this.mKeyguardShowing, this.mDeviceProvisioned);
        }
    }
}
