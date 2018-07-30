package android.app;

import android.app.trust.ITrustManager;
import android.app.trust.ITrustManager.Stub;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceManager.ServiceNotFoundException;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.service.persistentdata.IPersistentDataBlockService;
import android.util.Log;
import android.view.IOnKeyguardExitResult;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.widget.LockPatternUtils;
import java.util.List;

public class KeyguardManager {
    public static final String ACTION_CONFIRM_DEVICE_CREDENTIAL = "android.app.action.CONFIRM_DEVICE_CREDENTIAL";
    public static final String ACTION_CONFIRM_DEVICE_CREDENTIAL_WITH_USER = "android.app.action.CONFIRM_DEVICE_CREDENTIAL_WITH_USER";
    public static final String ACTION_CONFIRM_FRP_CREDENTIAL = "android.app.action.CONFIRM_FRP_CREDENTIAL";
    public static final String EXTRA_ALTERNATE_BUTTON_LABEL = "android.app.extra.ALTERNATE_BUTTON_LABEL";
    public static final String EXTRA_DESCRIPTION = "android.app.extra.DESCRIPTION";
    public static final String EXTRA_TITLE = "android.app.extra.TITLE";
    public static final int RESULT_ALTERNATE = 1;
    private static final String TAG = "KeyguardManager";
    private final IActivityManager mAm = ActivityManager.getService();
    private final Context mContext;
    private final ITrustManager mTrustManager = Stub.asInterface(ServiceManager.getServiceOrThrow(Context.TRUST_SERVICE));
    private final IWindowManager mWM = WindowManagerGlobal.getWindowManagerService();

    public static abstract class KeyguardDismissCallback {
        public void onDismissError() {
        }

        public void onDismissSucceeded() {
        }

        public void onDismissCancelled() {
        }
    }

    @Deprecated
    public class KeyguardLock {
        private final String mTag;
        private final IBinder mToken = new Binder();

        KeyguardLock(String tag) {
            this.mTag = tag;
        }

        public void disableKeyguard() {
            try {
                KeyguardManager.this.mWM.disableKeyguard(this.mToken, this.mTag);
            } catch (RemoteException e) {
            }
        }

        public void reenableKeyguard() {
            try {
                KeyguardManager.this.mWM.reenableKeyguard(this.mToken);
            } catch (RemoteException e) {
            }
        }
    }

    @Deprecated
    public interface OnKeyguardExitResult {
        void onKeyguardExitResult(boolean z);
    }

    public Intent createConfirmDeviceCredentialIntent(CharSequence title, CharSequence description) {
        if (!isDeviceSecure()) {
            return null;
        }
        Intent intent = new Intent(ACTION_CONFIRM_DEVICE_CREDENTIAL);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        intent.setPackage(getSettingsPackageForIntent(intent));
        return intent;
    }

    public Intent createConfirmDeviceCredentialIntent(CharSequence title, CharSequence description, int userId) {
        if (!isDeviceSecure(userId)) {
            return null;
        }
        Intent intent = new Intent(ACTION_CONFIRM_DEVICE_CREDENTIAL_WITH_USER);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        intent.putExtra(Intent.EXTRA_USER_ID, userId);
        intent.setPackage(getSettingsPackageForIntent(intent));
        return intent;
    }

    public Intent createConfirmFactoryResetCredentialIntent(CharSequence title, CharSequence description, CharSequence alternateButtonLabel) {
        if (!LockPatternUtils.frpCredentialEnabled(this.mContext)) {
            Log.w(TAG, "Factory reset credentials not supported.");
            return null;
        } else if (Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
            Log.e(TAG, "Factory reset credential cannot be verified after provisioning.");
            return null;
        } else {
            try {
                IPersistentDataBlockService pdb = IPersistentDataBlockService.Stub.asInterface(ServiceManager.getService(Context.PERSISTENT_DATA_BLOCK_SERVICE));
                if (pdb == null) {
                    Log.e(TAG, "No persistent data block service");
                    return null;
                } else if (pdb.hasFrpCredentialHandle()) {
                    Intent intent = new Intent(ACTION_CONFIRM_FRP_CREDENTIAL);
                    intent.putExtra(EXTRA_TITLE, title);
                    intent.putExtra(EXTRA_DESCRIPTION, description);
                    intent.putExtra(EXTRA_ALTERNATE_BUTTON_LABEL, alternateButtonLabel);
                    intent.setPackage(getSettingsPackageForIntent(intent));
                    return intent;
                } else {
                    Log.i(TAG, "The persistent data block does not have a factory reset credential.");
                    return null;
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    private String getSettingsPackageForIntent(Intent intent) {
        List<ResolveInfo> resolveInfos = this.mContext.getPackageManager().queryIntentActivities(intent, 1048576);
        if (resolveInfos.size() > 0) {
            return ((ResolveInfo) resolveInfos.get(0)).activityInfo.packageName;
        }
        return "com.android.settings";
    }

    KeyguardManager(Context context) throws ServiceNotFoundException {
        this.mContext = context;
    }

    @Deprecated
    public KeyguardLock newKeyguardLock(String tag) {
        return new KeyguardLock(tag);
    }

    public boolean isKeyguardLocked() {
        try {
            return this.mWM.isKeyguardLocked();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isKeyguardSecure() {
        try {
            return this.mWM.isKeyguardSecure();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean inKeyguardRestrictedInputMode() {
        try {
            return this.mWM.inKeyguardRestrictedInputMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isDeviceLocked() {
        return isDeviceLocked(UserHandle.myUserId());
    }

    public boolean isDeviceLocked(int userId) {
        try {
            return this.mTrustManager.isDeviceLocked(userId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isDeviceSecure() {
        return isDeviceSecure(UserHandle.myUserId());
    }

    public boolean isDeviceSecure(int userId) {
        try {
            return this.mTrustManager.isDeviceSecure(userId);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Deprecated
    public void dismissKeyguard(Activity activity, KeyguardDismissCallback callback, Handler handler) {
        requestDismissKeyguard(activity, callback);
    }

    public void requestDismissKeyguard(final Activity activity, final KeyguardDismissCallback callback) {
        try {
            this.mAm.dismissKeyguard(activity.getActivityToken(), new IKeyguardDismissCallback.Stub() {
                public void onDismissError() throws RemoteException {
                    if (callback != null && (activity.isDestroyed() ^ 1) != 0) {
                        Handler handler = activity.mHandler;
                        KeyguardDismissCallback keyguardDismissCallback = callback;
                        keyguardDismissCallback.getClass();
                        handler.post(new -$Lambda$aS31cHIhRx41653CMnd4gZqshIQ((byte) 5, keyguardDismissCallback));
                    }
                }

                public void onDismissSucceeded() throws RemoteException {
                    if (callback != null && (activity.isDestroyed() ^ 1) != 0) {
                        Handler handler = activity.mHandler;
                        KeyguardDismissCallback keyguardDismissCallback = callback;
                        keyguardDismissCallback.getClass();
                        handler.post(new -$Lambda$aS31cHIhRx41653CMnd4gZqshIQ((byte) 6, keyguardDismissCallback));
                    }
                }

                public void onDismissCancelled() throws RemoteException {
                    if (callback != null && (activity.isDestroyed() ^ 1) != 0) {
                        Handler handler = activity.mHandler;
                        KeyguardDismissCallback keyguardDismissCallback = callback;
                        keyguardDismissCallback.getClass();
                        handler.post(new -$Lambda$aS31cHIhRx41653CMnd4gZqshIQ((byte) 4, keyguardDismissCallback));
                    }
                }
            });
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to dismiss keyguard: " + e);
        }
    }

    @Deprecated
    public void exitKeyguardSecurely(final OnKeyguardExitResult callback) {
        try {
            this.mWM.exitKeyguardSecurely(new IOnKeyguardExitResult.Stub() {
                public void onKeyguardExitResult(boolean success) throws RemoteException {
                    if (callback != null) {
                        callback.onKeyguardExitResult(success);
                    }
                }
            });
        } catch (RemoteException e) {
        }
    }

    public boolean isKeyguardShown() {
        try {
            return this.mWM.isKeyguardShown();
        } catch (RemoteException e) {
            return false;
        }
    }
}
