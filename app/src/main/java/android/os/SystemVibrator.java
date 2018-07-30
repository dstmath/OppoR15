package android.os;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.IVibratorService.Stub;
import android.os.VibrationEffect.Prebaked;
import android.os.VibrationEffect.Waveform;
import android.util.Log;

public class SystemVibrator extends Vibrator {
    private static final String TAG = "Vibrator";
    private ActivityManager mActivityManager;
    private boolean mCameraAntiShake = false;
    private boolean mLogEnable = SystemProperties.getBoolean("persist.sys.assert.panic", false);
    private final IVibratorService mService = Stub.asInterface(ServiceManager.getService(Context.VIBRATOR_SERVICE));
    private final Binder mToken = new Binder();

    public SystemVibrator(Context context) {
        super(context);
        this.mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.mCameraAntiShake = context.getPackageManager().hasSystemFeature("oppo.camera.antishake.support");
    }

    public boolean hasVibrator() {
        if (this.mService == null) {
            Log.w(TAG, "Failed to vibrate; no vibrator service.");
            return false;
        }
        try {
            return this.mService.hasVibrator();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean hasAmplitudeControl() {
        if (this.mService == null) {
            Log.w(TAG, "Failed to check amplitude control; no vibrator service.");
            return false;
        }
        try {
            return this.mService.hasAmplitudeControl();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void vibrate(int uid, String opPkg, VibrationEffect effect, AudioAttributes attributes) {
        if (this.mService == null) {
            Log.w(TAG, "Failed to vibrate; no vibrator service.");
            return;
        }
        boolean shouldVib = false;
        if ((effect instanceof Waveform) && ((Waveform) effect).getRepeatIndex() >= 0) {
            shouldVib = true;
        }
        if ((effect instanceof Prebaked) && ((Prebaked) effect).getId() == 0) {
            shouldVib = true;
        }
        String cameraPkgName = SystemProperties.get("oppo.camera.packname", "default");
        if (!(!this.mCameraAntiShake || (shouldVib ^ 1) == 0 || (cameraPkgName.equals("default") ^ 1) == 0)) {
            ComponentName componentName = this.mActivityManager.getTopAppName();
            String topAppPkgName = componentName != null ? componentName.toString() : null;
            if (topAppPkgName != null ? topAppPkgName.contains(cameraPkgName) : false) {
                return;
            }
        }
        if (this.mLogEnable) {
            Log.i(TAG, "SystemVibrator vibrate is uid= " + uid + ",opPkg =" + opPkg + ",Binder.getCallingPid()=" + Binder.getCallingPid());
            if (uid > 10000) {
                Log.d(TAG, "vibrate here dumpStack:  Callers=" + Debug.getCallers(4));
            }
        }
        try {
            this.mService.vibrate(uid, opPkg, effect, usageForAttributes(attributes), this.mToken);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to vibrate.", e);
        }
    }

    private static int usageForAttributes(AudioAttributes attributes) {
        return attributes != null ? attributes.getUsage() : 0;
    }

    public void cancel() {
        if (this.mService != null) {
            try {
                this.mService.cancelVibrate(this.mToken);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to cancel vibration.", e);
            }
        }
    }
}
