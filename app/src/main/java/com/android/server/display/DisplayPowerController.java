package com.android.server.display;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManagerInternal.DisplayPowerCallbacks;
import android.hardware.display.DisplayManagerInternal.DisplayPowerRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.Settings.System;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import android.util.TimeUtils;
import android.view.Display;
import android.view.WindowManagerPolicy;
import android.view.WindowManagerPolicy.ScreenOffListener;
import android.view.WindowManagerPolicy.ScreenOnListener;
import com.android.internal.app.IBatteryStats;
import com.android.server.LocalServices;
import com.android.server.am.BatteryStatsService;
import com.android.server.biometrics.BiometricsManagerInternal;
import com.android.server.display.RampAnimator.Listener;
import com.android.server.lights.LightsService;
import com.android.server.oppo.ScreenOnCpuBoostHelper;
import com.android.server.power.PowerManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

final class DisplayPowerController implements Callbacks {
    static final /* synthetic */ boolean -assertionsDisabled = (DisplayPowerController.class.desiredAssertionStatus() ^ 1);
    private static final int ALWAYSON_SENSOR_RATE_US = 500000;
    public static int BRIGHTNESS_RAMP_RATE_FAST = 0;
    public static int BRIGHTNESS_RAMP_RATE_SCREENON = 0;
    public static int BRIGHTNESS_RAMP_RATE_SLOW = 0;
    private static final int COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 100;
    private static final int COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 250;
    static boolean DEBUG = false;
    static boolean DEBUG_PANIC = false;
    private static final boolean DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT = false;
    private static final long LCD_HIGH_BRIGHTNESS_STATE_DELAY = 2000;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 2;
    private static final int MSG_SCREEN_OFF_UNBLOCKED = 4;
    private static final int MSG_SCREEN_ON_BRIGHTNESS_BOOST = 5;
    private static final int MSG_SCREEN_ON_UNBLOCKED = 3;
    private static final int MSG_SCREEN_ON_UNBLOCKED_BY_BIOMETRICS = 6;
    private static final int MSG_UPDATE_HIGH_BRIGHTNESS_STATE = 1;
    private static final int MSG_UPDATE_POWER_STATE = 1;
    private static final long OLED_HIGH_BRIGHTNESS_STATE_DELAY = 5000;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_SENSOR_NEGATIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_SENSOR_POSITIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final int RAMP_STATE_SKIP_AUTOBRIGHT = 2;
    private static final int RAMP_STATE_SKIP_INITIAL = 1;
    private static final int RAMP_STATE_SKIP_NONE = 0;
    private static final int REPORTED_TO_POLICY_SCREEN_OFF = 0;
    private static final int REPORTED_TO_POLICY_SCREEN_ON = 2;
    private static final int REPORTED_TO_POLICY_SCREEN_TURNING_OFF = 3;
    private static final int REPORTED_TO_POLICY_SCREEN_TURNING_ON = 1;
    private static final int SCREEN_DIM_MINIMUM_REDUCTION = 10;
    private static final String SCREEN_OFF_BLOCKED_TRACE_NAME = "Screen off blocked";
    private static final String SCREEN_ON_BLOCKED_BY_BIOMETRICS_TRACE_NAME = "Screen on blocked by biometrics";
    private static final String SCREEN_ON_BLOCKED_TRACE_NAME = "Screen on blocked";
    private static final String TAG = "DisplayPowerController";
    private static final String TAG_BIOMETRICS = "Biometrics_DEBUG";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final boolean USE_COLOR_FADE_ON_ANIMATION = false;
    private static int mHighBrightnessMode = 0;
    private static float mLux = OppoBrightUtils.MIN_LUX_LIMITI;
    private static OppoBrightUtils mOppoBrightUtils;
    private static int mPreHighBrightnessMode = 0;
    public static boolean mQuickDarkToBright = false;
    public static boolean mScreenDimQuicklyDark = false;
    private static boolean mStartTimer = false;
    private final boolean mAllowAutoBrightnessWhileDozingConfig;
    private final AnimatorListener mAnimatorListener = new AnimatorListener() {
        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            DisplayPowerController.this.sendUpdatePowerState();
            if (DisplayPowerController.this.mBiometricsManager != null) {
                DisplayPowerController.this.mBiometricsManager.onGoToSleepFinish();
            }
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationCancel(Animator animation) {
        }
    };
    private boolean mAppliedAutoBrightness;
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private AutomaticBrightnessController mAutomaticBrightnessController;
    private final IBatteryStats mBatteryStats;
    private BiometricsManagerInternal mBiometricsManager;
    private final DisplayBlanker mBlanker;
    private final ArrayList<String> mBlockReasonList = new ArrayList();
    private boolean mBrightnessBucketsInDozeConfig;
    private int mBrightnessRampRateFast;
    private int mBrightnessRampRateSlow;
    private final DisplayPowerCallbacks mCallbacks;
    private final Runnable mCleanListener = new Runnable() {
        public void run() {
            DisplayPowerController.this.sendUpdatePowerState();
        }
    };
    private final boolean mColorFadeEnabled;
    private boolean mColorFadeFadesConfig;
    private ObjectAnimator mColorFadeOffAnimator;
    private ObjectAnimator mColorFadeOnAnimator;
    private final Context mContext;
    private boolean mDisplayBlanksAfterDozeConfig;
    private boolean mDisplayReadyLocked;
    private final int mDozeBrightnessConfig = 0;
    private boolean mDozing;
    private final DisplayControllerHandler mHandler;
    private int mInitialAutoBrightness;
    private Sensor mLightSensor;
    private boolean mLightSensorAlwaysOn = false;
    private final SensorEventListener mLightSensorAlwaysOnListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (DisplayPowerController.this.mLightSensorAlwaysOn && (DisplayPowerController.this.mScreenBrightnessRampAnimator.isAnimating() ^ 1) != 0) {
                float lLux = event.values[0];
                DisplayPowerController.mLux = lLux;
                if (OppoBrightUtils.mHighBrightnessModeSupport) {
                    DisplayPowerController.mOppoBrightUtils;
                    if (OppoBrightUtils.mCameraMode == 1 && lLux >= 10000.0f) {
                        int i = LightsService.mScreenBrightness;
                        DisplayPowerController.mOppoBrightUtils;
                        if (i == OppoBrightUtils.mMaxBrightness) {
                            DisplayPowerController.mHighBrightnessMode = 8;
                        }
                    }
                    DisplayPowerController.mHighBrightnessMode = 0;
                } else if (lLux >= 10000.0f) {
                    DisplayPowerController.mHighBrightnessMode = 1;
                } else {
                    DisplayPowerController.mHighBrightnessMode = 0;
                }
                if (DisplayPowerController.mHighBrightnessMode != DisplayPowerController.mPreHighBrightnessMode) {
                    DisplayPowerController.mPreHighBrightnessMode = DisplayPowerController.mHighBrightnessMode;
                    DisplayPowerController.this.stopTimer();
                    DisplayPowerController.this.startTimer();
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final Object mLock = new Object();
    private final Runnable mOnProximityNegativeRunnable = new Runnable() {
        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityNegative();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnProximityNegativeSuspendRunnable = new Runnable() {
        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityNegativeForceSuspend();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnProximityPositiveRunnable = new Runnable() {
        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityPositive();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnProximityPositiveSuspendRunnable = new Runnable() {
        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityPositiveForceSuspend();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnStateChangedRunnable = new Runnable() {
        public void run() {
            DisplayPowerController.this.mCallbacks.onStateChanged();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private boolean mPendingDisplayReadyBlocker = false;
    private int mPendingProximity = -1;
    private long mPendingProximityDebounceTime = -1;
    private boolean mPendingRequestChangedLocked;
    private DisplayPowerRequest mPendingRequestLocked;
    private boolean mPendingScreenOff;
    private ScreenOffUnblocker mPendingScreenOffUnblocker;
    private ScreenOnUnblocker mPendingScreenOnUnblocker;
    private ScreenOnUnblockerByBiometrics mPendingScreenOnUnblockerFromBiometrics;
    private boolean mPendingUpdatePowerStateLocked;
    private boolean mPendingWaitForNegativeProximityLocked;
    private final PowerManagerInternal mPowerManagerInternal;
    private DisplayPowerRequest mPowerRequest;
    private DisplayPowerState mPowerState;
    private int mProximity = -1;
    private boolean mProximityEventHandled = true;
    private Sensor mProximitySensor;
    private boolean mProximitySensorEnabled;
    private final SensorEventListener mProximitySensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (DisplayPowerController.this.mProximitySensorEnabled) {
                long time = SystemClock.uptimeMillis();
                float distance = event.values[0];
                boolean positive = distance >= OppoBrightUtils.MIN_LUX_LIMITI && distance < DisplayPowerController.this.mProximityThreshold;
                if (DisplayPowerController.DEBUG_PANIC) {
                    Slog.d(DisplayPowerController.TAG, "P-Sensor Changed: distance = " + distance + ", positive = " + positive);
                }
                DisplayPowerController.this.handleProximitySensorEvent(time, positive);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private float mProximityThreshold;
    private final Listener mRampAnimatorListener = new Listener() {
        public void onAnimationEnd() {
            DisplayPowerController.this.sendUpdatePowerState();
        }
    };
    private int mReportedScreenStateToPolicy;
    private int mScreenBrightnessDarkConfig;
    private int mScreenBrightnessDimConfig;
    private int mScreenBrightnessDozeConfig;
    private RampAnimator<DisplayPowerState> mScreenBrightnessRampAnimator;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private boolean mScreenOffBecauseOfProximity;
    private long mScreenOffBlockStartRealTime;
    private long mScreenOnBlockStartRealTime;
    private boolean mScreenOnBlockedByFace = false;
    private ScreenOnCpuBoostHelper mScreenOnCpuBoostHelper;
    private int mScreenState;
    private final SensorManager mSensorManager;
    private int mSkipRampState = 0;
    private final boolean mSkipScreenOnBrightnessRamp;
    private TimerTask mTask;
    private Handler mTimehandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what != 1) {
                return;
            }
            if (DisplayPowerController.this.mScreenBrightnessRampAnimator.isAnimating()) {
                DisplayPowerController.this.stopTimer();
                DisplayPowerController.this.startTimer();
                return;
            }
            if (DisplayPowerController.mHighBrightnessMode == DisplayPowerController.mPreHighBrightnessMode) {
                DisplayPowerController.mOppoBrightUtils.setHighBrightness(DisplayPowerController.mHighBrightnessMode);
            }
            DisplayPowerController.this.stopTimer();
        }
    };
    private Timer mTimer;
    private boolean mUnfinishedBusiness;
    private boolean mUseSoftwareAutoBrightnessConfig;
    private boolean mWaitingForNegativeProximity;
    private final WindowManagerPolicy mWindowManagerPolicy;
    private boolean useProximityForceSuspend = false;

    private final class DisplayControllerHandler extends Handler {
        public DisplayControllerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 2:
                    DisplayPowerController.this.debounceProximitySensor();
                    return;
                case 3:
                    if (DisplayPowerController.this.mPendingScreenOnUnblocker == msg.obj) {
                        DisplayPowerController.this.unblockScreenOn();
                        DisplayPowerController.this.updatePowerState();
                        return;
                    }
                    return;
                case 4:
                    if (DisplayPowerController.this.mPendingScreenOffUnblocker == msg.obj) {
                        DisplayPowerController.this.unblockScreenOff();
                        DisplayPowerController.this.updatePowerState();
                        return;
                    }
                    return;
                case 5:
                    DisplayPowerController.mOppoBrightUtils;
                    OppoBrightUtils.mBrightnessBoost = 4;
                    return;
                case 6:
                    if (DisplayPowerController.this.mPendingScreenOnUnblockerFromBiometrics == msg.obj) {
                        DisplayPowerController.this.unblockScreenOnByBiometrics("MSG_SCREEN_ON_UNBLOCKED_BY_BIOMETRICS");
                        DisplayPowerController.this.updatePowerState();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private final class ScreenOffUnblocker implements ScreenOffListener {
        /* synthetic */ ScreenOffUnblocker(DisplayPowerController this$0, ScreenOffUnblocker -this1) {
            this();
        }

        private ScreenOffUnblocker() {
        }

        public void onScreenOff() {
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(4, this);
            msg.setAsynchronous(true);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }

    private final class ScreenOnUnblocker implements ScreenOnListener {
        /* synthetic */ ScreenOnUnblocker(DisplayPowerController this$0, ScreenOnUnblocker -this1) {
            this();
        }

        private ScreenOnUnblocker() {
        }

        public void onScreenOn() {
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(3, this);
            msg.setAsynchronous(true);
            Slog.d(DisplayPowerController.TAG, "ScreenOnUnblocker, onScreenOn");
            if (DisplayPowerController.this.mScreenOnBlockedByFace) {
                DisplayPowerController.this.mHandler.sendMessageDelayed(msg, 1000);
            } else {
                DisplayPowerController.this.mHandler.sendMessage(msg);
            }
        }
    }

    private final class ScreenOnUnblockerByBiometrics implements ScreenOnListener {
        /* synthetic */ ScreenOnUnblockerByBiometrics(DisplayPowerController this$0, ScreenOnUnblockerByBiometrics -this1) {
            this();
        }

        private ScreenOnUnblockerByBiometrics() {
        }

        public void onScreenOn() {
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(6, this);
            msg.setAsynchronous(true);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }

    public DisplayPowerController(Context context, DisplayPowerCallbacks callbacks, Handler handler, SensorManager sensorManager, DisplayBlanker blanker) {
        this.mHandler = new DisplayControllerHandler(handler.getLooper());
        this.mCallbacks = callbacks;
        this.mBatteryStats = BatteryStatsService.getService();
        this.mSensorManager = sensorManager;
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mScreenOnCpuBoostHelper = new ScreenOnCpuBoostHelper();
        this.mBlanker = blanker;
        this.mContext = context;
        this.mScreenState = -1;
        mQuickDarkToBright = false;
        mOppoBrightUtils = OppoBrightUtils.getInstance();
        mOppoBrightUtils.isSpecialSensor();
        mOppoBrightUtils.configAutoBrightness();
        OppoBrightUtils oppoBrightUtils = mOppoBrightUtils;
        DEBUG = OppoBrightUtils.DEBUG;
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
        Resources resources = context.getResources();
        int screenBrightnessSettingMinimum = clampAbsoluteBrightness(resources.getInteger(17694851));
        this.mScreenBrightnessDozeConfig = clampAbsoluteBrightness(resources.getInteger(17694845));
        this.mScreenBrightnessDimConfig = clampAbsoluteBrightness(resources.getInteger(17694844));
        this.mScreenBrightnessDarkConfig = clampAbsoluteBrightness(resources.getInteger(17694843));
        if (this.mScreenBrightnessDarkConfig > this.mScreenBrightnessDimConfig) {
            Slog.w(TAG, "Expected config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") to be less than or equal to " + "config_screenBrightnessDim (" + this.mScreenBrightnessDimConfig + ").");
        }
        if (this.mScreenBrightnessDarkConfig > screenBrightnessSettingMinimum) {
            Slog.w(TAG, "Expected config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") to be less than or equal to " + "config_screenBrightnessSettingMinimum (" + screenBrightnessSettingMinimum + ").");
        }
        int screenBrightnessRangeMinimum = Math.min(Math.min(screenBrightnessSettingMinimum, this.mScreenBrightnessDimConfig), this.mScreenBrightnessDarkConfig);
        screenBrightnessRangeMinimum = mOppoBrightUtils.getMinimumScreenBrightnessSetting();
        this.mScreenBrightnessRangeMaximum = PowerManager.BRIGHTNESS_MULTIBITS_ON;
        this.mUseSoftwareAutoBrightnessConfig = resources.getBoolean(17956894);
        this.mAllowAutoBrightnessWhileDozingConfig = resources.getBoolean(17956872);
        this.mSkipScreenOnBrightnessRamp = resources.getBoolean(17957016);
        int lightSensorRate = resources.getInteger(17694734);
        int initialLightSensorRate = resources.getInteger(17694733);
        if (initialLightSensorRate == -1) {
            initialLightSensorRate = lightSensorRate;
        } else if (initialLightSensorRate > lightSensorRate) {
            Slog.w(TAG, "Expected config_autoBrightnessInitialLightSensorRate (" + initialLightSensorRate + ") to be less than or equal to " + "config_autoBrightnessLightSensorRate (" + lightSensorRate + ").");
        }
        long brighteningLightDebounce = (long) resources.getInteger(17694731);
        long darkeningLightDebounce = (long) resources.getInteger(17694732);
        if (OppoBrightUtils.mBrightnessBitsConfig == 3) {
            darkeningLightDebounce = 1000;
        }
        boolean autoBrightnessResetAmbientLuxAfterWarmUp = resources.getBoolean(17956890);
        int ambientLightHorizon = resources.getInteger(17694730);
        float autoBrightnessAdjustmentMaxGamma = resources.getFraction(18022400, 1, 1);
        HysteresisLevels hysteresisLevels = new HysteresisLevels(resources.getIntArray(17236003), resources.getIntArray(17236004), resources.getIntArray(17236005));
        if (this.mUseSoftwareAutoBrightnessConfig) {
            int[] lux = mOppoBrightUtils.readAutoBrightnessLuxConfig();
            int[] screenBrightness = mOppoBrightUtils.readAutoBrightnessConfig();
            int lightSensorWarmUpTimeConfig = resources.getInteger(17694798);
            float dozeScaleFactor = resources.getFraction(18022403, 1, 1);
            Spline screenAutoBrightnessSpline = createAutoBrightnessSpline(lux, screenBrightness);
            if (screenAutoBrightnessSpline == null) {
                Slog.e(TAG, "Error in config.xml.  config_autoBrightnessLcdBacklightValues (size " + screenBrightness.length + ") " + "must be monotic and have exactly one more entry than " + "config_autoBrightnessLevels (size " + lux.length + ") " + "which must be strictly increasing.  " + "Auto-brightness will be disabled.");
                this.mUseSoftwareAutoBrightnessConfig = false;
            } else {
                int bottom = clampAbsoluteBrightness(screenBrightness[0]);
                if (this.mScreenBrightnessDarkConfig > bottom) {
                    Slog.w(TAG, "config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") should be less than or equal to the first value of " + "config_autoBrightnessLcdBacklightValues (" + bottom + ").");
                }
                if (bottom < screenBrightnessRangeMinimum) {
                    screenBrightnessRangeMinimum = bottom;
                }
                this.mAutomaticBrightnessController = new AutomaticBrightnessController(this, handler.getLooper(), this.mContext, sensorManager, screenAutoBrightnessSpline, lightSensorWarmUpTimeConfig, screenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounce, darkeningLightDebounce, autoBrightnessResetAmbientLuxAfterWarmUp, ambientLightHorizon, autoBrightnessAdjustmentMaxGamma, hysteresisLevels);
            }
            this.mScreenBrightnessDozeConfig = screenBrightness[0];
            this.mScreenBrightnessDimConfig = screenBrightness[0];
            this.mScreenBrightnessDarkConfig = screenBrightness[0];
            DEBUG_PANIC = SystemProperties.getBoolean("persist.sys.assert.panic", false);
        }
        this.mScreenBrightnessRangeMinimum = screenBrightnessRangeMinimum;
        this.mColorFadeEnabled = ActivityManager.isLowRamDeviceStatic() ^ 1;
        this.mColorFadeFadesConfig = resources.getBoolean(17956887);
        this.mDisplayBlanksAfterDozeConfig = resources.getBoolean(17956928);
        this.mBrightnessBucketsInDozeConfig = true;
        this.mProximitySensor = this.mSensorManager.getDefaultSensor(8);
        if (this.mProximitySensor != null) {
            this.mProximityThreshold = Math.min(this.mProximitySensor.getMaximumRange(), 5.0f);
        }
        this.mBiometricsManager = (BiometricsManagerInternal) LocalServices.getService(BiometricsManagerInternal.class);
    }

    public boolean isProximitySensorAvailable() {
        return this.mProximitySensor != null;
    }

    public void setUseProximityForceSuspend(boolean enable) {
        if (!this.useProximityForceSuspend) {
            this.useProximityForceSuspend = enable;
        }
    }

    public boolean requestPowerState(DisplayPowerRequest request, boolean waitForNegativeProximity) {
        boolean z;
        if (DEBUG) {
            Slog.d(TAG, "requestPowerState: " + request + ", waitForNegativeProximity=" + waitForNegativeProximity);
        }
        synchronized (this.mLock) {
            boolean changed = false;
            if (waitForNegativeProximity) {
                if ((this.mPendingWaitForNegativeProximityLocked ^ 1) != 0) {
                    this.mPendingWaitForNegativeProximityLocked = true;
                    changed = true;
                }
            }
            if (this.mPendingRequestLocked == null) {
                this.mPendingRequestLocked = new DisplayPowerRequest(request);
                changed = true;
            } else if (!this.mPendingRequestLocked.equals(request)) {
                this.mPendingRequestLocked.copyFrom(request);
                changed = true;
            }
            if (changed) {
                this.mDisplayReadyLocked = false;
            }
            if (changed && (this.mPendingRequestChangedLocked ^ 1) != 0) {
                this.mPendingRequestChangedLocked = true;
                sendUpdatePowerStateLocked();
            }
            z = this.mDisplayReadyLocked;
        }
        return z;
    }

    private void sendUpdatePowerState() {
        synchronized (this.mLock) {
            sendUpdatePowerStateLocked();
        }
    }

    private void sendUpdatePowerStateLocked() {
        if (!this.mPendingUpdatePowerStateLocked) {
            this.mPendingUpdatePowerStateLocked = true;
            Message msg = this.mHandler.obtainMessage(1);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    private void initialize() {
        this.mPowerState = new DisplayPowerState(this.mBlanker, this.mColorFadeEnabled ? new ColorFade(0) : null);
        if (this.mColorFadeEnabled) {
            this.mColorFadeOnAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, new float[]{OppoBrightUtils.MIN_LUX_LIMITI, 1.0f});
            this.mColorFadeOnAnimator.setDuration(250);
            this.mColorFadeOnAnimator.addListener(this.mAnimatorListener);
            this.mColorFadeOffAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, new float[]{1.0f, OppoBrightUtils.MIN_LUX_LIMITI});
            this.mColorFadeOffAnimator.setDuration(100);
            this.mColorFadeOffAnimator.addListener(this.mAnimatorListener);
        }
        this.mScreenBrightnessRampAnimator = new RampAnimator(this.mPowerState, DisplayPowerState.SCREEN_BRIGHTNESS);
        this.mScreenBrightnessRampAnimator.setListener(this.mRampAnimatorListener);
        try {
            this.mBatteryStats.noteScreenState(this.mPowerState.getScreenState());
            this.mBatteryStats.noteScreenBrightness(this.mPowerState.getScreenBrightness());
        } catch (RemoteException e) {
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updatePowerState() {
        boolean mustInitialize = false;
        boolean autoBrightnessAdjustmentChanged = false;
        synchronized (this.mLock) {
            this.mPendingUpdatePowerStateLocked = false;
            if (this.mPendingRequestLocked == null) {
                return;
            }
            if (this.mPowerRequest == null) {
                this.mPowerRequest = new DisplayPowerRequest(this.mPendingRequestLocked);
                this.mWaitingForNegativeProximity = this.mPendingWaitForNegativeProximityLocked;
                this.mPendingWaitForNegativeProximityLocked = false;
                this.mPendingRequestChangedLocked = false;
                mustInitialize = true;
            } else if (this.mPendingRequestChangedLocked) {
                autoBrightnessAdjustmentChanged = this.mPowerRequest.screenAutoBrightnessAdjustment != this.mPendingRequestLocked.screenAutoBrightnessAdjustment;
                this.mPowerRequest.copyFrom(this.mPendingRequestLocked);
                this.mWaitingForNegativeProximity |= this.mPendingWaitForNegativeProximityLocked;
                this.mPendingWaitForNegativeProximityLocked = false;
                this.mPendingRequestChangedLocked = false;
                this.mDisplayReadyLocked = false;
            }
            boolean mustNotify = this.mDisplayReadyLocked ^ 1;
        }
    }

    public void updateBrightness() {
        sendUpdatePowerState();
    }

    public void blockScreenOnByBiometrics(String reason) {
        if (DEBUG_PANIC) {
            Slog.d(TAG_BIOMETRICS, "blockScreenOnByBiometrics, mPendingScreenOnUnblockerFromBiometrics = " + this.mPendingScreenOnUnblockerFromBiometrics);
        }
        if (this.mPendingScreenOnUnblockerFromBiometrics == null) {
            Trace.asyncTraceBegin(131072, SCREEN_ON_BLOCKED_BY_BIOMETRICS_TRACE_NAME, 0);
            this.mPendingScreenOnUnblockerFromBiometrics = new ScreenOnUnblockerByBiometrics();
        }
        this.mPendingDisplayReadyBlocker = true;
        this.mBlockReasonList.add(reason);
    }

    public void unblockScreenOnByBiometrics(String reason) {
        if (DEBUG_PANIC) {
            Slog.d(TAG_BIOMETRICS, "unblockScreen(reason = " + reason + ") , mPendingScreenOnUnblockerFromBiometrics = " + this.mPendingScreenOnUnblockerFromBiometrics);
        }
        if (this.mPendingScreenOnUnblockerFromBiometrics != null) {
            this.mPendingScreenOnUnblockerFromBiometrics = null;
            Trace.asyncTraceEnd(131072, SCREEN_ON_BLOCKED_BY_BIOMETRICS_TRACE_NAME, 0);
        }
        this.mPendingDisplayReadyBlocker = false;
        if (!PowerManagerService.UNBLOCK_REASON_GO_TO_SLEEP.equals(reason)) {
            sendUpdatePowerState();
        }
        this.mBlockReasonList.clear();
    }

    private void unblockDisplayReady() {
        if (this.mPowerManagerInternal != null) {
            if (this.mPowerManagerInternal.isStartGoToSleep()) {
                this.mPendingDisplayReadyBlocker = false;
            }
            if (DEBUG_PANIC) {
                Slog.d(TAG_BIOMETRICS, "unblockDisplayReady, mPendingDisplayReadyBlocker = " + this.mPendingDisplayReadyBlocker);
            }
        }
    }

    public boolean isBlockScreenOnByBiometrics() {
        return this.mPendingScreenOnUnblockerFromBiometrics != null;
    }

    public boolean isBlockDisplayByBiometrics() {
        return this.mPendingDisplayReadyBlocker;
    }

    public boolean hasBiometricsBlockedReason(String reason) {
        return this.mBlockReasonList.contains(reason);
    }

    public int getScreenState() {
        if (DEBUG_PANIC) {
            boolean z;
            String str = TAG_BIOMETRICS;
            StringBuilder append = new StringBuilder().append("ScreenState = ").append(this.mPowerState.getScreenState()).append(", fingerPrint block = ");
            if (this.mPendingScreenOnUnblockerFromBiometrics != null) {
                z = true;
            } else {
                z = false;
            }
            append = append.append(z).append(", keyguard block = ");
            if (this.mPendingScreenOnUnblocker != null) {
                z = true;
            } else {
                z = false;
            }
            Slog.d(str, append.append(z).toString());
        }
        return (this.mPowerState.getScreenState() == 2 && this.mPendingScreenOnUnblockerFromBiometrics == null && this.mPendingScreenOnUnblocker == null) ? 1 : 0;
    }

    public void updateScreenOnBlockedState(boolean isBlockedScreenOn) {
        this.mScreenOnBlockedByFace = isBlockedScreenOn;
        if (DEBUG_PANIC) {
            Slog.d(TAG, "updateScreenOnBlockedState, isBlockedScreenOn = " + isBlockedScreenOn);
        }
        if (!isBlockedScreenOn && this.mHandler.hasMessages(3)) {
            this.mHandler.removeMessages(3);
            Message msg = this.mHandler.obtainMessage(3, this.mPendingScreenOnUnblocker);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
            Slog.d(TAG, "MSG_SCREEN_ON_UNBLOCKED sended");
        }
    }

    private void blockScreenOn() {
        this.mHandler.removeMessages(3);
        if (this.mPendingScreenOnUnblocker == null) {
            Trace.asyncTraceBegin(131072, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOnUnblocker = new ScreenOnUnblocker();
            this.mScreenOnBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(TAG, "Blocking screen on until initial contents have been drawn.");
        }
    }

    private void unblockScreenOn() {
        this.mHandler.removeMessages(3);
        if (this.mPendingScreenOnUnblocker != null) {
            this.mPendingScreenOnUnblocker = null;
            Slog.i(TAG, "Unblocked screen on after " + (SystemClock.elapsedRealtime() - this.mScreenOnBlockStartRealTime) + " ms");
            Trace.asyncTraceEnd(131072, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
        }
    }

    private void blockScreenOff() {
        if (this.mPendingScreenOffUnblocker == null) {
            Trace.asyncTraceBegin(131072, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOffUnblocker = new ScreenOffUnblocker();
            this.mScreenOffBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(TAG, "Blocking screen off");
        }
    }

    private void unblockScreenOff() {
        if (this.mPendingScreenOffUnblocker != null) {
            this.mPendingScreenOffUnblocker = null;
            Slog.i(TAG, "Unblocked screen off after " + (SystemClock.elapsedRealtime() - this.mScreenOffBlockStartRealTime) + " ms");
            Trace.asyncTraceEnd(131072, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
        }
    }

    private boolean setScreenState(int state) {
        return setScreenState(state, false);
    }

    private boolean setScreenState(int state, boolean reportOnly) {
        OppoBrightUtils oppoBrightUtils;
        String str;
        StringBuilder append;
        OppoBrightUtils oppoBrightUtils2;
        boolean z = true;
        if (state == 2) {
            oppoBrightUtils = mOppoBrightUtils;
            if (OppoBrightUtils.mFirstSetScreenState) {
                if (this.mPowerRequest.useAutoBrightness) {
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mBrightnessBoost = 1;
                    this.mHandler.removeMessages(5);
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5), 4000);
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mDisplayStateOn = true;
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mFirstSetScreenState = false;
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mManualBrightnessBackup = (int) System.getFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", OppoBrightUtils.MIN_LUX_LIMITI, -2);
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mManualAmbientLuxBackup = System.getFloatForUser(this.mContext.getContentResolver(), "autobrightness_manul_ambient", OppoBrightUtils.MIN_LUX_LIMITI, -2);
                    if (DEBUG) {
                        str = TAG;
                        append = new StringBuilder().append(" mManulAtAmbientLux = ");
                        oppoBrightUtils2 = mOppoBrightUtils;
                        Slog.d(str, append.append(OppoBrightUtils.mManulAtAmbientLux).toString());
                    }
                } else {
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mFirstSetScreenState = false;
                }
                if (OppoBrightUtils.mOutdoorBrightnessSupport) {
                    setLightSensorAlwaysOn(true);
                }
            }
        }
        boolean isOff = state == 1 || state == 3 || state == 4;
        if (this.mPowerState.getScreenState() != state) {
            if (isOff && (this.mScreenOffBecauseOfProximity ^ 1) != 0) {
                if (this.mReportedScreenStateToPolicy == 2) {
                    setReportedScreenState(3);
                    blockScreenOff();
                    this.mWindowManagerPolicy.screenTurningOff(this.mPendingScreenOffUnblocker);
                    unblockScreenOff();
                } else if (this.mPendingScreenOffUnblocker != null) {
                    return false;
                }
            }
            if (state == 1) {
                oppoBrightUtils = mOppoBrightUtils;
                if (!OppoBrightUtils.mSaveBrightnessByShutdown) {
                    oppoBrightUtils = mOppoBrightUtils;
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mManualBrightnessBackup = OppoBrightUtils.mManualBrightness;
                    oppoBrightUtils = mOppoBrightUtils;
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mManualAmbientLuxBackup = OppoBrightUtils.mManulAtAmbientLux;
                    if (DEBUG) {
                        str = TAG;
                        append = new StringBuilder().append("Display.STATE_OFF mManualBrightness = ");
                        oppoBrightUtils2 = mOppoBrightUtils;
                        append = append.append(OppoBrightUtils.mManualBrightness).append(" mManulAtAmbientLux = ");
                        oppoBrightUtils2 = mOppoBrightUtils;
                        Slog.d(str, append.append(OppoBrightUtils.mManulAtAmbientLux).toString());
                    }
                    oppoBrightUtils2 = mOppoBrightUtils;
                    System.putFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", (float) OppoBrightUtils.mManualBrightnessBackup, -2);
                    oppoBrightUtils2 = mOppoBrightUtils;
                    System.putFloatForUser(this.mContext.getContentResolver(), "autobrightness_manul_ambient", OppoBrightUtils.mManulAtAmbientLux, -2);
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mManualBrightness = 0;
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mManulAtAmbientLux = OppoBrightUtils.MIN_LUX_LIMITI;
                }
                oppoBrightUtils = mOppoBrightUtils;
                OppoBrightUtils.mSaveBrightnessByShutdown = false;
                if (AutomaticBrightnessController.mProximityNear && (OppoBrightUtils.DEBUG_PRETEND_PROX_SENSOR_ABSENT ^ 1) != 0) {
                    this.mScreenBrightnessRampAnimator.updateCurrentToTarget();
                }
                if (OppoBrightUtils.mHighBrightnessModeSupport || OppoBrightUtils.mOutdoorBrightnessSupport) {
                    setLightSensorAlwaysOn(false);
                }
                this.mAutomaticBrightnessController.resetLightParamsScreenOff();
            } else if (state == 2) {
                if (this.mPowerRequest.useAutoBrightness) {
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mBrightnessBoost = 1;
                    this.mHandler.removeMessages(5);
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5), 4000);
                }
                oppoBrightUtils = mOppoBrightUtils;
                OppoBrightUtils.mDisplayStateOn = true;
                oppoBrightUtils = mOppoBrightUtils;
                OppoBrightUtils.mManualBrightnessBackup = (int) System.getFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", OppoBrightUtils.MIN_LUX_LIMITI, -2);
                oppoBrightUtils = mOppoBrightUtils;
                if (OppoBrightUtils.mManualBrightnessBackup != 0) {
                    oppoBrightUtils = mOppoBrightUtils;
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mManualBrightness = OppoBrightUtils.mManualBrightnessBackup;
                }
                if (DEBUG) {
                    str = TAG;
                    append = new StringBuilder().append("Display.STATE_ON mManualBrightness ");
                    oppoBrightUtils2 = mOppoBrightUtils;
                    Slog.d(str, append.append(OppoBrightUtils.mManualBrightness).toString());
                }
                if (OppoBrightUtils.mOutdoorBrightnessSupport) {
                    setLightSensorAlwaysOn(true);
                }
            }
            if (!reportOnly) {
                this.mPowerState.setScreenState(state);
                try {
                    if (DEBUG_PANIC) {
                        Slog.d(TAG, "mBatteryStats.noteScreenState +++");
                    }
                    this.mBatteryStats.noteScreenState(state);
                    if (DEBUG_PANIC) {
                        Slog.d(TAG, "mBatteryStats.noteScreenState ---");
                    }
                } catch (RemoteException e) {
                }
            }
        }
        if (isOff && this.mReportedScreenStateToPolicy != 0 && (this.mScreenOffBecauseOfProximity ^ 1) != 0) {
            setReportedScreenState(0);
            unblockScreenOn();
            unblockDisplayReady();
            this.mWindowManagerPolicy.screenTurnedOff();
        } else if (!isOff && this.mReportedScreenStateToPolicy == 3) {
            unblockScreenOff();
            this.mWindowManagerPolicy.screenTurnedOff();
            setReportedScreenState(0);
        }
        if (!isOff && this.mReportedScreenStateToPolicy == 0) {
            setReportedScreenState(1);
            if (this.mPowerState.getColorFadeLevel() == OppoBrightUtils.MIN_LUX_LIMITI) {
                blockScreenOn();
            } else {
                unblockScreenOn();
            }
            this.mWindowManagerPolicy.screenTurningOn(this.mPendingScreenOnUnblocker);
        }
        if (!(this.mPendingScreenOnUnblocker == null && this.mPendingScreenOnUnblockerFromBiometrics == null)) {
            z = false;
        }
        return z;
    }

    private void setReportedScreenState(int state) {
        Trace.traceCounter(131072, "ReportedScreenStateToPolicy", state);
        this.mReportedScreenStateToPolicy = state;
    }

    private int clampScreenBrightness(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void animateScreenBrightness(int target, int rate) {
        if (DEBUG) {
            Slog.d(TAG, "Animating brightness: target=" + target + ", rate=" + rate);
        }
        OppoBrightUtils oppoBrightUtils = mOppoBrightUtils;
        if (OppoBrightUtils.mSetBrihgtnessSlide) {
            System.putIntForBrightness(this.mContext.getContentResolver(), "screen_brightness", target, -2);
            oppoBrightUtils = mOppoBrightUtils;
            OppoBrightUtils.mSetBrihgtnessSlide = false;
        }
        if (this.mScreenBrightnessRampAnimator.animateTo(target, rate)) {
            Trace.traceCounter(131072, "TargetScreenBrightness", target);
            try {
                this.mBatteryStats.noteScreenBrightness(target);
            } catch (RemoteException e) {
            }
        }
    }

    private void animateScreenStateChange(int target, boolean performScreenOffTransition) {
        int i = 2;
        if (!this.mColorFadeEnabled || (!this.mColorFadeOnAnimator.isStarted() && !this.mColorFadeOffAnimator.isStarted())) {
            if (this.mDisplayBlanksAfterDozeConfig && Display.isDozeState(this.mPowerState.getScreenState()) && (Display.isDozeState(target) ^ 1) != 0) {
                boolean z;
                this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 0);
                if (this.mColorFadeOffAnimator != null) {
                    this.mColorFadeOffAnimator.end();
                }
                if (target != 1) {
                    z = true;
                } else {
                    z = false;
                }
                setScreenState(1, z);
            }
            if (this.mPendingScreenOff && target != 1) {
                setScreenState(1);
                this.mPendingScreenOff = false;
                this.mPowerState.dismissColorFadeResources();
            }
            if (target == 2) {
                if (setScreenState(2)) {
                    this.mPowerState.setAodStatus(false);
                    this.mPowerState.setColorFadeLevel(1.0f);
                    this.mPowerState.dismissColorFade();
                }
            } else if (target == 5) {
                if (!(this.mScreenBrightnessRampAnimator.isAnimating() && this.mPowerState.getScreenState() == 2) && setScreenState(5)) {
                    this.mPowerState.setAodStatus(false);
                    this.mPowerState.setColorFadeLevel(1.0f);
                    this.mPowerState.dismissColorFade();
                }
            } else if (target == 3) {
                if (!(this.mScreenBrightnessRampAnimator.isAnimating() && this.mPowerState.getScreenState() == 2) && setScreenState(3)) {
                    this.mPowerState.setAodStatus(true);
                }
            } else if (target != 4) {
                this.mPendingScreenOff = true;
                this.mPowerState.setAodStatus(false);
                if (!this.mColorFadeEnabled) {
                    this.mPowerState.setColorFadeLevel(OppoBrightUtils.MIN_LUX_LIMITI);
                }
                if (this.mPowerState.getColorFadeLevel() == OppoBrightUtils.MIN_LUX_LIMITI) {
                    setScreenState(1);
                    this.mPendingScreenOff = false;
                    this.mPowerState.dismissColorFadeResources();
                } else {
                    if (performScreenOffTransition) {
                        DisplayPowerState displayPowerState = this.mPowerState;
                        Context context = this.mContext;
                        if (!this.mColorFadeFadesConfig) {
                            i = 1;
                        }
                        if (displayPowerState.prepareColorFade(context, i) && this.mPowerState.getScreenState() != 1) {
                            this.mColorFadeOffAnimator.start();
                        }
                    }
                    this.mColorFadeOffAnimator.end();
                }
            } else if (!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() == 4) {
                if (this.mPowerState.getScreenState() != 4) {
                    if (setScreenState(3)) {
                        setScreenState(4);
                    } else {
                        return;
                    }
                }
                this.mPowerState.setAodStatus(true);
            }
        }
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mProximitySensorEnabled) {
                if (DEBUG_PANIC) {
                    Slog.d(TAG, "setProximitySensorEnabled : True");
                }
                this.mProximitySensorEnabled = true;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mHandler);
            }
        } else if (this.mProximitySensorEnabled) {
            if (DEBUG_PANIC) {
                Slog.d(TAG, "setProximitySensorEnabled : False");
            }
            this.mProximitySensorEnabled = false;
            this.useProximityForceSuspend = false;
            this.mProximity = -1;
            this.mPendingProximity = -1;
            this.mHandler.removeMessages(2);
            this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            clearPendingProximityDebounceTime();
        }
    }

    private void handleProximitySensorEvent(long time, boolean positive) {
        if (this.mProximitySensorEnabled && (this.mPendingProximity != 0 || (positive ^ 1) == 0)) {
            if (this.mPendingProximity != 1 || !positive) {
                this.mHandler.removeMessages(2);
                if (positive) {
                    this.mPendingProximity = 1;
                    setPendingProximityDebounceTime(time + 0);
                } else {
                    this.mPendingProximity = 0;
                    setPendingProximityDebounceTime(time + 0);
                }
                debounceProximitySensor();
            }
        }
    }

    private void debounceProximitySensor() {
        if (this.mProximitySensorEnabled && this.mPendingProximity != -1 && this.mPendingProximityDebounceTime >= 0) {
            if (this.mPendingProximityDebounceTime <= SystemClock.uptimeMillis()) {
                this.mProximity = this.mPendingProximity;
                this.mProximityEventHandled = false;
                this.mScreenOnCpuBoostHelper.acquireCpuBoost(500);
                updatePowerState();
                clearPendingProximityDebounceTime();
                return;
            }
            Message msg = this.mHandler.obtainMessage(2);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageAtTime(msg, this.mPendingProximityDebounceTime);
        }
    }

    private void clearPendingProximityDebounceTime() {
        if (this.mPendingProximityDebounceTime >= 0) {
            this.mPendingProximityDebounceTime = -1;
            this.mCallbacks.releaseSuspendBlocker();
        }
    }

    private void setPendingProximityDebounceTime(long debounceTime) {
        if (this.mPendingProximityDebounceTime < 0) {
            this.mCallbacks.acquireSuspendBlocker();
        }
        this.mPendingProximityDebounceTime = debounceTime;
    }

    private void sendOnStateChangedWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnStateChangedRunnable);
    }

    private void sendOnProximityPositiveSuspendWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityPositiveSuspendRunnable);
    }

    private void sendOnProximityNegativeSuspendWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityNegativeSuspendRunnable);
    }

    private void sendOnProximityPositiveWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityPositiveRunnable);
    }

    private void sendOnProximityNegativeWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityNegativeRunnable);
    }

    public void dump(final PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println();
            pw.println("Display Power Controller Locked State:");
            pw.println("  mDisplayReadyLocked=" + this.mDisplayReadyLocked);
            pw.println("  mPendingRequestLocked=" + this.mPendingRequestLocked);
            pw.println("  mPendingRequestChangedLocked=" + this.mPendingRequestChangedLocked);
            pw.println("  mPendingWaitForNegativeProximityLocked=" + this.mPendingWaitForNegativeProximityLocked);
            pw.println("  mPendingUpdatePowerStateLocked=" + this.mPendingUpdatePowerStateLocked);
        }
        pw.println();
        pw.println("Display Power Controller Configuration:");
        pw.println("  mScreenBrightnessDozeConfig=" + this.mScreenBrightnessDozeConfig);
        pw.println("  mScreenBrightnessDimConfig=" + this.mScreenBrightnessDimConfig);
        pw.println("  mScreenBrightnessDarkConfig=" + this.mScreenBrightnessDarkConfig);
        pw.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        pw.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        pw.println("  mUseSoftwareAutoBrightnessConfig=" + this.mUseSoftwareAutoBrightnessConfig);
        pw.println("  mAllowAutoBrightnessWhileDozingConfig=" + this.mAllowAutoBrightnessWhileDozingConfig);
        pw.println("  mColorFadeFadesConfig=" + this.mColorFadeFadesConfig);
        this.mHandler.runWithScissors(new Runnable() {
            public void run() {
                DisplayPowerController.this.dumpLocal(pw);
            }
        }, 1000);
    }

    private void dumpLocal(PrintWriter pw) {
        pw.println();
        pw.println("Display Power Controller Thread State:");
        pw.println("  mPowerRequest=" + this.mPowerRequest);
        pw.println("  mWaitingForNegativeProximity=" + this.mWaitingForNegativeProximity);
        pw.println("  mProximitySensor=" + this.mProximitySensor);
        pw.println("  mProximitySensorEnabled=" + this.mProximitySensorEnabled);
        pw.println("  mProximityThreshold=" + this.mProximityThreshold);
        pw.println("  mProximity=" + proximityToString(this.mProximity));
        pw.println("  mPendingProximity=" + proximityToString(this.mPendingProximity));
        pw.println("  mPendingProximityDebounceTime=" + TimeUtils.formatUptime(this.mPendingProximityDebounceTime));
        pw.println("  mScreenOffBecauseOfProximity=" + this.mScreenOffBecauseOfProximity);
        pw.println("  mAppliedAutoBrightness=" + this.mAppliedAutoBrightness);
        pw.println("  mAppliedDimming=" + this.mAppliedDimming);
        pw.println("  mAppliedLowPower=" + this.mAppliedLowPower);
        pw.println("  mPendingScreenOnUnblocker=" + this.mPendingScreenOnUnblocker);
        pw.println("  mPendingScreenOff=" + this.mPendingScreenOff);
        pw.println("  mReportedToPolicy=" + reportedToPolicyToString(this.mReportedScreenStateToPolicy));
        pw.println("  mScreenBrightnessRampAnimator.isAnimating()=" + this.mScreenBrightnessRampAnimator.isAnimating());
        if (this.mColorFadeOnAnimator != null) {
            pw.println("  mColorFadeOnAnimator.isStarted()=" + this.mColorFadeOnAnimator.isStarted());
        }
        if (this.mColorFadeOffAnimator != null) {
            pw.println("  mColorFadeOffAnimator.isStarted()=" + this.mColorFadeOffAnimator.isStarted());
        }
        if (this.mPowerState != null) {
            this.mPowerState.dump(pw);
        }
        if (this.mAutomaticBrightnessController != null) {
            this.mAutomaticBrightnessController.dump(pw);
        }
    }

    private static String proximityToString(int state) {
        switch (state) {
            case -1:
                return "Unknown";
            case 0:
                return "Negative";
            case 1:
                return "Positive";
            default:
                return Integer.toString(state);
        }
    }

    private static String reportedToPolicyToString(int state) {
        switch (state) {
            case 0:
                return "REPORTED_TO_POLICY_SCREEN_OFF";
            case 1:
                return "REPORTED_TO_POLICY_SCREEN_TURNING_ON";
            case 2:
                return "REPORTED_TO_POLICY_SCREEN_ON";
            default:
                return Integer.toString(state);
        }
    }

    private static Spline createAutoBrightnessSpline(int[] lux, int[] brightness) {
        if (lux == null || lux.length == 0 || brightness == null || brightness.length == 0) {
            Slog.e(TAG, "Could not create auto-brightness spline.");
            return null;
        }
        try {
            int n = brightness.length;
            float[] x = new float[n];
            float[] y = new float[n];
            y[0] = normalizeAbsoluteBrightness(brightness[0]);
            for (int i = 1; i < n; i++) {
                x[i] = (float) lux[i - 1];
                y[i] = normalizeAbsoluteBrightness(brightness[i]);
            }
            Spline spline = Spline.createSpline(x, y);
            if (DEBUG) {
                Slog.d(TAG, "Auto-brightness spline: " + spline);
                for (float v = 1.0f; v < ((float) lux[lux.length - 1]) * 1.25f; v *= 1.25f) {
                    Slog.d(TAG, String.format("  %7.1f: %7.1f", new Object[]{Float.valueOf(v), Float.valueOf(spline.interpolate(v))}));
                }
            }
            return spline;
        } catch (IllegalArgumentException ex) {
            Slog.e(TAG, "Could not create auto-brightness spline.", ex);
            return null;
        }
    }

    private static float normalizeAbsoluteBrightness(int value) {
        return ((float) clampAbsoluteBrightness(value)) / ((float) PowerManager.BRIGHTNESS_MULTIBITS_ON);
    }

    private static int clampAbsoluteBrightness(int value) {
        return MathUtils.constrain(value, 0, PowerManager.BRIGHTNESS_MULTIBITS_ON);
    }

    private void setLightSensorAlwaysOn(boolean enable) {
        if (enable) {
            if (!this.mLightSensorAlwaysOn) {
                this.mLightSensorAlwaysOn = true;
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        DisplayPowerController.this.mSensorManager.registerListener(DisplayPowerController.this.mLightSensorAlwaysOnListener, DisplayPowerController.this.mLightSensor, DisplayPowerController.ALWAYSON_SENSOR_RATE_US, DisplayPowerController.this.mHandler);
                    }
                }, "LightSensorAlwaysOnThread");
                thread.setPriority(10);
                thread.start();
            }
        } else if (this.mLightSensorAlwaysOn) {
            this.mLightSensorAlwaysOn = false;
            this.mSensorManager.unregisterListener(this.mLightSensorAlwaysOnListener);
            mHighBrightnessMode = 0;
            mPreHighBrightnessMode = 0;
            mLux = OppoBrightUtils.MIN_LUX_LIMITI;
            stopTimer();
            mOppoBrightUtils.setHighBrightness(mHighBrightnessMode);
        }
    }

    public void setOutdoorMode(boolean enable) {
        if (!OppoBrightUtils.mHighBrightnessModeSupport) {
            return;
        }
        if (enable) {
            setLightSensorAlwaysOn(true);
        } else {
            setLightSensorAlwaysOn(false);
        }
    }

    private void startTimer() {
        if (!mStartTimer) {
            synchronized (this) {
                mStartTimer = true;
                if (this.mTimer == null) {
                    this.mTimer = new Timer();
                }
                if (this.mTask == null) {
                    this.mTask = new TimerTask() {
                        public void run() {
                            Message msg = new Message();
                            msg.what = 1;
                            DisplayPowerController.this.mTimehandler.sendMessage(msg);
                        }
                    };
                }
                if (!(this.mTimer == null || this.mTask == null)) {
                    if (OppoBrightUtils.mHighBrightnessModeSupport) {
                        this.mTimer.schedule(this.mTask, 5000, 5000);
                    } else {
                        this.mTimer.schedule(this.mTask, LCD_HIGH_BRIGHTNESS_STATE_DELAY, LCD_HIGH_BRIGHTNESS_STATE_DELAY);
                    }
                }
            }
        }
    }

    private void stopTimer() {
        if (mStartTimer) {
            synchronized (this) {
                try {
                    mStartTimer = false;
                    if (this.mTimer != null) {
                        this.mTimer.cancel();
                        this.mTimer = null;
                    }
                    if (this.mTask != null) {
                        this.mTask.cancel();
                        this.mTask = null;
                    }
                } catch (NullPointerException e) {
                    Slog.i(TAG, "stopTimer null pointer", e);
                }
            }
            return;
        }
        return;
    }
}
