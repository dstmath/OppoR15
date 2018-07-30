package com.android.server.display;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import android.util.TimeUtils;
import com.android.server.EventLogTags;
import com.android.server.face.FaceDaemonWrapper;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

class AutomaticBrightnessController {
    private static final int AMBIENT_LIGHT_LONG_HORIZON_MILLIS = 10000;
    private static final long AMBIENT_LIGHT_PREDICTION_TIME_MILLIS = 100;
    private static final int AMBIENT_LIGHT_SHORT_HORIZON_MILLIS = 2000;
    private static final int BRIGHTNESS_ADJUSTMENT_SAMPLE_DEBOUNCE_MILLIS = 10000;
    public static boolean DEBUG = false;
    private static final boolean DEBUG_PRETEND_LIGHT_SENSOR_ABSENT = false;
    private static final int MSG_BRIGHTNESS_ADJUSTMENT_SAMPLE = 2;
    private static final int MSG_UPDATE_AMBIENT_LUX = 1;
    private static final int MSG_UPDATE_BRIGHTNESS_AFTER_PROXIMITY = 5;
    private static final String TAG = "AutomaticBrightnessController";
    private static final boolean USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT = true;
    private static int mCurrent_Lbr_Level = 0;
    private static int mLast_Lbr_Level = 0;
    private static OppoBrightUtils mOppoBrightUtils;
    public static boolean mProximityNear = false;
    private float lux_beforemax = OppoBrightUtils.MIN_LUX_LIMITI;
    private float lux_beforemin = OppoBrightUtils.MIN_LUX_LIMITI;
    private float lux_max = OppoBrightUtils.MIN_LUX_LIMITI;
    private float lux_min = OppoBrightUtils.MIN_LUX_LIMITI;
    private final int mAmbientLightHorizon;
    private AmbientLightRingBuffer mAmbientLightRingBuffer;
    private float mAmbientLux = 320.0f;
    private boolean mAmbientLuxValid;
    public int mAutoRate = 0;
    private int mBacklightStatus = 0;
    private final long mBrighteningLightDebounceConfig;
    private float mBrighteningLuxThreshold;
    private float mBrightnessAdjustmentSampleOldAdjustment;
    private int mBrightnessAdjustmentSampleOldBrightness;
    private float mBrightnessAdjustmentSampleOldGamma;
    private float mBrightnessAdjustmentSampleOldLux;
    private boolean mBrightnessAdjustmentSamplePending;
    private final Callbacks mCallbacks;
    private Context mContext;
    private int mCurrentLightSensorRate;
    private final long mDarkeningLightDebounceConfig;
    private float mDarkeningLuxThreshold;
    private float mDeltaLux = OppoBrightUtils.MIN_LUX_LIMITI;
    private final float mDozeScaleFactor;
    private boolean mDozing;
    private final HysteresisLevels mDynamicHysteresis;
    private AutomaticBrightnessHandler mHandler;
    private AmbientLightRingBuffer mInitialHorizonAmbientLightRingBuffer;
    private final int mInitialLightSensorRate;
    private float mLastObservedLux;
    private long mLastObservedLuxTime;
    private float mLastScreenAutoBrightnessGamma = 1.0f;
    private final Sensor mLightSensor;
    private long mLightSensorEnableTime;
    private boolean mLightSensorEnabled;
    private final SensorEventListener mLightSensorListener = new SensorEventListener() {
        private long mPrevTime = 0;

        public void onSensorChanged(SensorEvent event) {
            if (AutomaticBrightnessController.this.mLightSensorEnabled) {
                long time = SystemClock.uptimeMillis();
                AutomaticBrightnessController.mOppoBrightUtils;
                if (OppoBrightUtils.mBrightnessBoost == 1) {
                    this.mPrevTime = 0;
                    AutomaticBrightnessController.mOppoBrightUtils;
                    OppoBrightUtils.mBrightnessBoost = 2;
                }
                if (event.values[0] <= OppoBrightUtils.MIN_LUX_LIMITI || event.values[0] >= 31000.0f || time - this.mPrevTime >= ((long) (AutomaticBrightnessController.this.mNormalLightSensorRate / 4))) {
                    this.mPrevTime = time;
                    float lux = event.values[0];
                    AutomaticBrightnessController.mOppoBrightUtils;
                    if (OppoBrightUtils.mBrightnessBoost == 2 || !AutomaticBrightnessController.mProximityNear || (OppoBrightUtils.DEBUG_PRETEND_PROX_SENSOR_ABSENT ^ 1) == 0 || lux >= AutomaticBrightnessController.this.lux_max || AutomaticBrightnessController.this.mBacklightStatus != 0) {
                        AutomaticBrightnessController.mOppoBrightUtils;
                        if (OppoBrightUtils.mSeedLbrModeSupport) {
                            if (lux >= 15000.0f) {
                                AutomaticBrightnessController.mOppoBrightUtils;
                                if (OppoBrightUtils.mCameraMode == 1) {
                                    AutomaticBrightnessController.mCurrent_Lbr_Level = (int) ((lux - 15000.0f) / 235.0f);
                                    if (AutomaticBrightnessController.mCurrent_Lbr_Level > 63) {
                                        AutomaticBrightnessController.mCurrent_Lbr_Level = 63;
                                    }
                                    if (AutomaticBrightnessController.mCurrent_Lbr_Level != AutomaticBrightnessController.mLast_Lbr_Level) {
                                        AutomaticBrightnessController.mLast_Lbr_Level = AutomaticBrightnessController.mCurrent_Lbr_Level;
                                        if (AutomaticBrightnessController.DEBUG) {
                                            Slog.d(AutomaticBrightnessController.TAG, "go to OppoBrightUtils SEED_LBR_NODE write " + AutomaticBrightnessController.mCurrent_Lbr_Level);
                                        }
                                        AutomaticBrightnessController.mOppoBrightUtils.writeSeedLbrNodeValue(AutomaticBrightnessController.mCurrent_Lbr_Level);
                                    }
                                }
                            }
                            AutomaticBrightnessController.mCurrent_Lbr_Level = 0;
                            if (AutomaticBrightnessController.mCurrent_Lbr_Level != AutomaticBrightnessController.mLast_Lbr_Level) {
                                AutomaticBrightnessController.mLast_Lbr_Level = AutomaticBrightnessController.mCurrent_Lbr_Level;
                                if (AutomaticBrightnessController.DEBUG) {
                                    Slog.d(AutomaticBrightnessController.TAG, "go to OppoBrightUtils SEED_LBR_NODE write " + AutomaticBrightnessController.mCurrent_Lbr_Level);
                                }
                                AutomaticBrightnessController.mOppoBrightUtils.writeSeedLbrNodeValue(AutomaticBrightnessController.mCurrent_Lbr_Level);
                            }
                        }
                        if (AutomaticBrightnessController.DEBUG) {
                            Slog.d(AutomaticBrightnessController.TAG, "PowerMS L-Sensor Changed:lux=" + lux + ",lux_min = " + AutomaticBrightnessController.this.lux_min + ",lux_max = " + AutomaticBrightnessController.this.lux_max);
                        }
                        if (!(lux != OppoBrightUtils.MIN_LUX_LIMITI || AutomaticBrightnessController.this.lux_min == OppoBrightUtils.MIN_LUX_LIMITI || (AutomaticBrightnessController.this.mbStartTimer ^ 1) == 0)) {
                            AutomaticBrightnessController.mOppoBrightUtils;
                            if (OppoBrightUtils.mBrightnessBoost != 2) {
                                AutomaticBrightnessController.this.mZeroStartTime = time;
                                AutomaticBrightnessController.this.mbStartTimer = true;
                                AutomaticBrightnessController.this.startZeroTimer();
                                if (AutomaticBrightnessController.DEBUG) {
                                    Slog.d(AutomaticBrightnessController.TAG, "onSensorChanged: first received lux = 0");
                                }
                                return;
                            }
                        }
                        if (AutomaticBrightnessController.this.mbStartTimer) {
                            if (lux != OppoBrightUtils.MIN_LUX_LIMITI) {
                                AutomaticBrightnessController.this.stopZeroTimer();
                                if (AutomaticBrightnessController.DEBUG) {
                                    Slog.d(AutomaticBrightnessController.TAG, "received 0lux at" + AutomaticBrightnessController.this.mZeroStartTime + "now received lux=" + lux);
                                }
                            } else {
                                if (AutomaticBrightnessController.DEBUG) {
                                    Slog.d(AutomaticBrightnessController.TAG, "it will not go here");
                                }
                                return;
                            }
                        }
                        if (AutomaticBrightnessController.DEBUG) {
                            Slog.d(AutomaticBrightnessController.TAG, "mBacklightStatus=" + AutomaticBrightnessController.this.mBacklightStatus + " marea=" + AutomaticBrightnessController.this.marea + " mareabefore=" + AutomaticBrightnessController.this.mareabefore);
                        }
                        if (AutomaticBrightnessController.this.mBacklightStatus != 0 || lux < AutomaticBrightnessController.this.lux_min || lux >= AutomaticBrightnessController.this.lux_max) {
                            if (AutomaticBrightnessController.this.mBacklightStatus == 1) {
                                if (lux >= AutomaticBrightnessController.this.lux_beforemin && lux < AutomaticBrightnessController.this.lux_beforemax) {
                                    AutomaticBrightnessController.this.mBacklightStatus = 2;
                                    AutomaticBrightnessController.this.handleLightSensorEvent(time, AutomaticBrightnessController.mOppoBrightUtils.findAmbientLux(AutomaticBrightnessController.this.mareabefore));
                                    AutomaticBrightnessController.this.mBacklightStatus = 0;
                                    AutomaticBrightnessController.this.lux_min = AutomaticBrightnessController.this.lux_beforemin;
                                    AutomaticBrightnessController.this.lux_max = AutomaticBrightnessController.this.lux_beforemax;
                                    AutomaticBrightnessController.this.marea = AutomaticBrightnessController.this.mareabefore;
                                    if (AutomaticBrightnessController.DEBUG) {
                                        Slog.d(AutomaticBrightnessController.TAG, "not stable to stable status and return.");
                                    }
                                    return;
                                } else if (lux >= AutomaticBrightnessController.this.lux_min && lux < AutomaticBrightnessController.this.lux_max) {
                                    if (AutomaticBrightnessController.DEBUG) {
                                        Slog.d(AutomaticBrightnessController.TAG, "not stable range and return.");
                                    }
                                    return;
                                }
                            }
                            AutomaticBrightnessController.mOppoBrightUtils;
                            if (OppoBrightUtils.mBrightnessBitsConfig == 1) {
                                AutomaticBrightnessController.this.mAutoRate = AutomaticBrightnessController.mOppoBrightUtils.caclurateRate(lux, AutomaticBrightnessController.this.mLastObservedLux);
                            } else {
                                AutomaticBrightnessController.this.mDeltaLux = lux - AutomaticBrightnessController.this.mLastObservedLux;
                            }
                            if (AutomaticBrightnessController.DEBUG) {
                                Slog.d(AutomaticBrightnessController.TAG, "lux=" + lux + " ,mLastObservedLux=" + AutomaticBrightnessController.this.mLastObservedLux + " ,mAutoRate=" + AutomaticBrightnessController.this.mAutoRate);
                            }
                            AutomaticBrightnessController.this.mLastObservedLux = lux;
                            int ii = 0;
                            while (true) {
                                AutomaticBrightnessController.mOppoBrightUtils;
                                if (ii >= OppoBrightUtils.BRIGHTNESS_STEPS) {
                                    break;
                                }
                                AutomaticBrightnessController.mOppoBrightUtils;
                                AutomaticBrightnessController automaticBrightnessController;
                                if (lux <= OppoBrightUtils.mAutoBrightnessLux[ii]) {
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    lux = OppoBrightUtils.mAutoBrightnessLux[ii];
                                    automaticBrightnessController = AutomaticBrightnessController.this;
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    automaticBrightnessController.lux_min = OppoBrightUtils.mAutoBrightnessLuxMinLimit[ii];
                                    automaticBrightnessController = AutomaticBrightnessController.this;
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    automaticBrightnessController.lux_max = OppoBrightUtils.mAutoBrightnessLuxMaxLimit[ii];
                                    AutomaticBrightnessController.this.marea = ii;
                                    break;
                                }
                                AutomaticBrightnessController.mOppoBrightUtils;
                                float[] fArr = OppoBrightUtils.mAutoBrightnessLux;
                                AutomaticBrightnessController.mOppoBrightUtils;
                                if (lux > fArr[OppoBrightUtils.BRIGHTNESS_STEPS - 1]) {
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    fArr = OppoBrightUtils.mAutoBrightnessLux;
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    lux = fArr[OppoBrightUtils.BRIGHTNESS_STEPS - 1];
                                    automaticBrightnessController = AutomaticBrightnessController.this;
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    float[] fArr2 = OppoBrightUtils.mAutoBrightnessLuxMinLimit;
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    automaticBrightnessController.lux_min = fArr2[OppoBrightUtils.BRIGHTNESS_STEPS - 1];
                                    automaticBrightnessController = AutomaticBrightnessController.this;
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    fArr2 = OppoBrightUtils.mAutoBrightnessLuxMaxLimit;
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    automaticBrightnessController.lux_max = fArr2[OppoBrightUtils.BRIGHTNESS_STEPS - 1];
                                    automaticBrightnessController = AutomaticBrightnessController.this;
                                    AutomaticBrightnessController.mOppoBrightUtils;
                                    automaticBrightnessController.marea = OppoBrightUtils.BRIGHTNESS_STEPS - 1;
                                    break;
                                }
                                ii++;
                            }
                            AutomaticBrightnessController.this.mBacklightStatus = 1;
                            AutomaticBrightnessController.this.handleLightSensorEvent(time, lux);
                        } else {
                            if (AutomaticBrightnessController.DEBUG) {
                                Slog.d(AutomaticBrightnessController.TAG, "in stable status and return.");
                            }
                            return;
                        }
                    }
                    if (AutomaticBrightnessController.DEBUG) {
                        Slog.d(AutomaticBrightnessController.TAG, "DEBUG_PRETEND_PROX_SENSOR_ABSENT=" + OppoBrightUtils.DEBUG_PRETEND_PROX_SENSOR_ABSENT);
                    }
                    return;
                }
                if (AutomaticBrightnessController.DEBUG) {
                    Slog.d(AutomaticBrightnessController.TAG, "Skip onSensorChanaged, pre time = " + this.mPrevTime + ", now = " + time);
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private int mLightSensorWarmUpTimeConfig;
    private boolean mManulBrightnessSlide = false;
    private final int mNormalLightSensorRate;
    private final Sensor mProximitySensor;
    private long mProximitySensorChangeTime = 0;
    private final SensorEventListener mProximitySensorListener = new SensorEventListener() {
        private boolean mPrevProximityNear = false;

        public void onSensorChanged(SensorEvent event) {
            long time = SystemClock.uptimeMillis();
            if (((double) event.values[0]) == 0.0d) {
                AutomaticBrightnessController.mProximityNear = true;
                if (AutomaticBrightnessController.DEBUG) {
                    Slog.d(AutomaticBrightnessController.TAG, "Proximity is near");
                }
            } else {
                if (AutomaticBrightnessController.mProximityNear) {
                    if (!OppoBrightUtils.DEBUG_PRETEND_PROX_SENSOR_ABSENT) {
                        AutomaticBrightnessController.mOppoBrightUtils;
                        if (!OppoBrightUtils.mManualSetAutoBrightness) {
                            AutomaticBrightnessController.this.mRecentLightSamples = 0;
                            AutomaticBrightnessController.this.mAmbientLightRingBuffer.clear();
                            AutomaticBrightnessController.this.mHandler.removeMessages(1);
                            AutomaticBrightnessController.this.lux_min = OppoBrightUtils.MIN_LUX_LIMITI;
                            AutomaticBrightnessController.this.lux_max = OppoBrightUtils.MIN_LUX_LIMITI;
                            AutomaticBrightnessController.this.mBacklightStatus = 2;
                        }
                    }
                    if (AutomaticBrightnessController.DEBUG) {
                        Slog.d(AutomaticBrightnessController.TAG, "Proximity is far");
                    }
                    AutomaticBrightnessController.this.mHandler.removeMessages(5);
                    AutomaticBrightnessController.this.mHandler.sendEmptyMessageDelayed(5, AutomaticBrightnessController.this.mDarkeningLightDebounceConfig);
                }
                AutomaticBrightnessController.mProximityNear = false;
            }
            if (this.mPrevProximityNear && (AutomaticBrightnessController.mProximityNear ^ 1) != 0) {
                AutomaticBrightnessController.this.mProximitySensorChangeTime = time;
            }
            this.mPrevProximityNear = AutomaticBrightnessController.mProximityNear;
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private int mRecentLightSamples;
    private final boolean mResetAmbientLuxAfterWarmUpConfig;
    public int mScreenAutoBrightness = -1;
    private float mScreenAutoBrightnessAdjustment = OppoBrightUtils.MIN_LUX_LIMITI;
    private float mScreenAutoBrightnessAdjustmentMaxGamma;
    private final Spline mScreenAutoBrightnessSpline;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private final SensorManager mSensorManager;
    private boolean mStartManual = false;
    private final int mWeightingIntercept;
    private long mZeroStartTime = 0;
    private TimerTask mZeroTask;
    private Timer mZeroTimer;
    private int marea = -1;
    private int mareabefore = -1;
    private boolean mbStartTimer = false;
    private Handler zeroHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                long time = SystemClock.uptimeMillis();
                AutomaticBrightnessController automaticBrightnessController = AutomaticBrightnessController.this;
                AutomaticBrightnessController.mOppoBrightUtils;
                automaticBrightnessController.lux_min = OppoBrightUtils.mAutoBrightnessLuxMinLimit[0];
                automaticBrightnessController = AutomaticBrightnessController.this;
                AutomaticBrightnessController.mOppoBrightUtils;
                automaticBrightnessController.lux_max = OppoBrightUtils.mAutoBrightnessLuxMaxLimit[0];
                AutomaticBrightnessController.this.mDeltaLux = AutomaticBrightnessController.this.mLastObservedLux;
                AutomaticBrightnessController.this.mAutoRate = AutomaticBrightnessController.mOppoBrightUtils.caclurateRate(OppoBrightUtils.MIN_LUX_LIMITI, AutomaticBrightnessController.this.mLastObservedLux);
                AutomaticBrightnessController.this.mLastObservedLux = OppoBrightUtils.MIN_LUX_LIMITI;
                AutomaticBrightnessController.this.handleLightSensorEvent(time, OppoBrightUtils.MIN_LUX_LIMITI);
            }
            if (AutomaticBrightnessController.this.mbStartTimer) {
                AutomaticBrightnessController.this.stopZeroTimer();
            }
        }
    };

    private static final class AmbientLightRingBuffer {
        private static final float BUFFER_SLACK = 1.5f;
        private int mCapacity;
        private int mCount;
        private int mEnd;
        private float[] mRingLux = new float[this.mCapacity];
        private long[] mRingTime = new long[this.mCapacity];
        private int mStart;

        public AmbientLightRingBuffer(long lightSensorRate, int ambientLightHorizon) {
            this.mCapacity = (int) Math.ceil((double) ((((float) ambientLightHorizon) * BUFFER_SLACK) / ((float) lightSensorRate)));
        }

        public float getLux(int index) {
            return this.mRingLux[offsetOf(index)];
        }

        public long getTime(int index) {
            return this.mRingTime[offsetOf(index)];
        }

        public void push(long time, float lux) {
            int next = this.mEnd;
            if (this.mCount == this.mCapacity) {
                int newSize = this.mCapacity * 2;
                float[] newRingLux = new float[newSize];
                long[] newRingTime = new long[newSize];
                int length = this.mCapacity - this.mStart;
                System.arraycopy(this.mRingLux, this.mStart, newRingLux, 0, length);
                System.arraycopy(this.mRingTime, this.mStart, newRingTime, 0, length);
                if (this.mStart != 0) {
                    System.arraycopy(this.mRingLux, 0, newRingLux, length, this.mStart);
                    System.arraycopy(this.mRingTime, 0, newRingTime, length, this.mStart);
                }
                this.mRingLux = newRingLux;
                this.mRingTime = newRingTime;
                next = this.mCapacity;
                this.mCapacity = newSize;
                this.mStart = 0;
            }
            this.mRingTime[next] = time;
            this.mRingLux[next] = lux;
            this.mEnd = next + 1;
            if (this.mEnd == this.mCapacity) {
                this.mEnd = 0;
            }
            this.mCount++;
        }

        public void prune(long horizon) {
            if (this.mCount != 0) {
                while (this.mCount > 1) {
                    int next = this.mStart + 1;
                    if (next >= this.mCapacity) {
                        next -= this.mCapacity;
                    }
                    if (this.mRingTime[next] > horizon) {
                        break;
                    }
                    this.mStart = next;
                    this.mCount--;
                }
                if (this.mRingTime[this.mStart] < horizon) {
                    this.mRingTime[this.mStart] = horizon;
                }
            }
        }

        public int size() {
            return this.mCount;
        }

        public void clear() {
            this.mStart = 0;
            this.mEnd = 0;
            this.mCount = 0;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append('[');
            for (int i = 0; i < this.mCount; i++) {
                long next = i + 1 < this.mCount ? getTime(i + 1) : SystemClock.uptimeMillis();
                if (i != 0) {
                    buf.append(", ");
                }
                buf.append(getLux(i));
                buf.append(" / ");
                buf.append(next - getTime(i));
                buf.append("ms");
            }
            buf.append(']');
            return buf.toString();
        }

        private int offsetOf(int index) {
            if (index >= this.mCount || index < 0) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            index += this.mStart;
            if (index >= this.mCapacity) {
                return index - this.mCapacity;
            }
            return index;
        }
    }

    private final class AutomaticBrightnessHandler extends Handler {
        public AutomaticBrightnessHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AutomaticBrightnessController.this.updateAmbientLux();
                    return;
                case 2:
                    AutomaticBrightnessController.this.collectBrightnessAdjustmentSample();
                    return;
                case 5:
                    AutomaticBrightnessController.this.mCallbacks.updateBrightness();
                    return;
                default:
                    return;
            }
        }
    }

    interface Callbacks {
        void updateBrightness();
    }

    private void startZeroTimer() {
        synchronized (this) {
            if (this.mZeroTimer == null) {
                this.mZeroTimer = new Timer();
            }
            if (this.mZeroTask == null) {
                this.mZeroTask = new TimerTask() {
                    public void run() {
                        Message msg = new Message();
                        msg.what = 1;
                        AutomaticBrightnessController.this.zeroHandler.sendMessage(msg);
                    }
                };
            }
            if (!(this.mZeroTimer == null || this.mZeroTask == null)) {
                this.mZeroTimer.schedule(this.mZeroTask, FaceDaemonWrapper.TIMEOUT_FACED_BINDERCALL_CHECK, this.mDarkeningLightDebounceConfig);
            }
        }
    }

    private void stopZeroTimer() {
        if (this.mbStartTimer) {
            synchronized (this) {
                try {
                    this.mbStartTimer = false;
                    if (this.mZeroTimer != null) {
                        this.mZeroTimer.cancel();
                        this.mZeroTimer = null;
                    }
                    if (this.mZeroTask != null) {
                        this.mZeroTask.cancel();
                        this.mZeroTask = null;
                    }
                } catch (NullPointerException e) {
                    Slog.i(TAG, "stopZeroTimer null pointer", e);
                }
            }
            return;
        }
        return;
    }

    public void resetLightParamsScreenOff() {
        this.mAmbientLuxValid = false;
    }

    public AutomaticBrightnessController(Callbacks callbacks, Looper looper, Context context, SensorManager sensorManager, Spline autoBrightnessSpline, int lightSensorWarmUpTime, int brightnessMin, int brightnessMax, float dozeScaleFactor, int lightSensorRate, int initialLightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, boolean resetAmbientLuxAfterWarmUpConfig, int ambientLightHorizon, float autoBrightnessAdjustmentMaxGamma, HysteresisLevels dynamicHysteresis) {
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mScreenAutoBrightnessSpline = autoBrightnessSpline;
        this.mScreenBrightnessRangeMinimum = brightnessMin;
        this.mScreenBrightnessRangeMaximum = brightnessMax;
        this.mLightSensorWarmUpTimeConfig = lightSensorWarmUpTime;
        this.mDozeScaleFactor = dozeScaleFactor;
        this.mNormalLightSensorRate = lightSensorRate;
        this.mInitialLightSensorRate = initialLightSensorRate;
        this.mCurrentLightSensorRate = -1;
        this.mBrighteningLightDebounceConfig = brighteningLightDebounceConfig;
        this.mDarkeningLightDebounceConfig = darkeningLightDebounceConfig;
        this.mResetAmbientLuxAfterWarmUpConfig = resetAmbientLuxAfterWarmUpConfig;
        this.mAmbientLightHorizon = ambientLightHorizon;
        this.mWeightingIntercept = ambientLightHorizon;
        this.mScreenAutoBrightnessAdjustmentMaxGamma = autoBrightnessAdjustmentMaxGamma;
        this.mDynamicHysteresis = dynamicHysteresis;
        this.mHandler = new AutomaticBrightnessHandler(looper);
        this.mAmbientLightRingBuffer = new AmbientLightRingBuffer((long) this.mNormalLightSensorRate, this.mAmbientLightHorizon);
        this.mInitialHorizonAmbientLightRingBuffer = new AmbientLightRingBuffer((long) this.mNormalLightSensorRate, this.mAmbientLightHorizon);
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
        mOppoBrightUtils = OppoBrightUtils.getInstance();
        mOppoBrightUtils.readFeatureProperty();
        OppoBrightUtils oppoBrightUtils = mOppoBrightUtils;
        DEBUG = OppoBrightUtils.DEBUG;
        this.mContext = context;
        this.mProximitySensor = this.mSensorManager.getDefaultSensor(8);
        this.mbStartTimer = false;
    }

    public int getAutomaticScreenBrightness() {
        if (this.mDozing) {
            return (int) (((float) this.mScreenAutoBrightness) * this.mDozeScaleFactor);
        }
        return this.mScreenAutoBrightness;
    }

    public void configure(boolean enable, float adjustment, boolean dozing, boolean userInitiatedChange) {
        boolean z;
        this.mDozing = dozing;
        if (enable) {
            z = dozing ^ 1;
        } else {
            z = false;
        }
        boolean changed = setLightSensorEnabled(z);
        if (enable && (dozing ^ 1) != 0 && userInitiatedChange) {
            prepareBrightnessAdjustmentSample();
        }
        if (changed | setScreenAutoBrightnessAdjustment(adjustment)) {
            updateAutoBrightness(false);
        }
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Automatic Brightness Controller Configuration:");
        pw.println("  mScreenAutoBrightnessSpline=" + this.mScreenAutoBrightnessSpline);
        pw.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        pw.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        pw.println("  mLightSensorWarmUpTimeConfig=" + this.mLightSensorWarmUpTimeConfig);
        pw.println("  mBrighteningLightDebounceConfig=" + this.mBrighteningLightDebounceConfig);
        pw.println("  mDarkeningLightDebounceConfig=" + this.mDarkeningLightDebounceConfig);
        pw.println("  mResetAmbientLuxAfterWarmUpConfig=" + this.mResetAmbientLuxAfterWarmUpConfig);
        pw.println();
        pw.println("Automatic Brightness Controller State:");
        pw.println("  mLightSensor=" + this.mLightSensor);
        pw.println("  mLightSensorEnabled=" + this.mLightSensorEnabled);
        pw.println("  mLightSensorEnableTime=" + TimeUtils.formatUptime(this.mLightSensorEnableTime));
        pw.println("  mAmbientLux=" + this.mAmbientLux);
        pw.println("  mAmbientLightHorizon=" + this.mAmbientLightHorizon);
        pw.println("  mBrighteningLuxThreshold=" + this.mBrighteningLuxThreshold);
        pw.println("  mDarkeningLuxThreshold=" + this.mDarkeningLuxThreshold);
        pw.println("  mLastObservedLux=" + this.mLastObservedLux);
        pw.println("  mLastObservedLuxTime=" + TimeUtils.formatUptime(this.mLastObservedLuxTime));
        pw.println("  mRecentLightSamples=" + this.mRecentLightSamples);
        pw.println("  mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer);
        pw.println("  mInitialHorizonAmbientLightRingBuffer=" + this.mInitialHorizonAmbientLightRingBuffer);
        pw.println("  mScreenAutoBrightness=" + this.mScreenAutoBrightness);
        pw.println("  mScreenAutoBrightnessAdjustment=" + this.mScreenAutoBrightnessAdjustment);
        pw.println("  mScreenAutoBrightnessAdjustmentMaxGamma=" + this.mScreenAutoBrightnessAdjustmentMaxGamma);
        pw.println("  mLastScreenAutoBrightnessGamma=" + this.mLastScreenAutoBrightnessGamma);
        pw.println("  mDozing=" + this.mDozing);
        DEBUG = OppoBrightUtils.DEBUG;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean setLightSensorEnabled(boolean enable) {
        DisplayPowerController.mScreenDimQuicklyDark = false;
        OppoBrightUtils oppoBrightUtils;
        if (enable) {
            if (!this.mLightSensorEnabled) {
                this.mLightSensorEnabled = true;
                this.mLightSensorEnableTime = SystemClock.uptimeMillis();
                this.mCurrentLightSensorRate = this.mInitialLightSensorRate;
                oppoBrightUtils = mOppoBrightUtils;
                OppoBrightUtils.mUseAutoBrightness = true;
                oppoBrightUtils = mOppoBrightUtils;
                if (OppoBrightUtils.mBrightnessBoost == 1) {
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            AutomaticBrightnessController.this.mSensorManager.registerListener(AutomaticBrightnessController.this.mLightSensorListener, AutomaticBrightnessController.this.mLightSensor, AutomaticBrightnessController.this.mCurrentLightSensorRate * 1000, AutomaticBrightnessController.this.mHandler);
                            AutomaticBrightnessController.this.mSensorManager.registerListener(AutomaticBrightnessController.this.mProximitySensorListener, AutomaticBrightnessController.this.mProximitySensor, AutomaticBrightnessController.this.mCurrentLightSensorRate * 1000, AutomaticBrightnessController.this.mHandler);
                        }
                    }, "LightSensorEnableThread");
                    thread.setPriority(10);
                    thread.start();
                } else {
                    this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, this.mCurrentLightSensorRate * 1000, this.mHandler);
                    this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, this.mCurrentLightSensorRate * 1000, this.mHandler);
                    oppoBrightUtils = mOppoBrightUtils;
                    if (OppoBrightUtils.mBrightnessBoost != 4) {
                        oppoBrightUtils = mOppoBrightUtils;
                        if (OppoBrightUtils.mBrightnessBoost != 0) {
                            oppoBrightUtils = mOppoBrightUtils;
                        }
                    }
                    oppoBrightUtils = mOppoBrightUtils;
                    OppoBrightUtils.mBrightnessBoost = 3;
                }
                this.lux_min = OppoBrightUtils.MIN_LUX_LIMITI;
                this.lux_max = OppoBrightUtils.MIN_LUX_LIMITI;
                return true;
            }
        } else if (this.mLightSensorEnabled) {
            this.mLightSensorEnabled = false;
            this.mAmbientLuxValid = this.mResetAmbientLuxAfterWarmUpConfig ^ 1;
            this.mRecentLightSamples = 0;
            this.mAmbientLightRingBuffer.clear();
            this.mInitialHorizonAmbientLightRingBuffer.clear();
            this.mCurrentLightSensorRate = -1;
            this.mHandler.removeMessages(1);
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
            this.mManulBrightnessSlide = false;
            oppoBrightUtils = mOppoBrightUtils;
            OppoBrightUtils.mManualBrightness = 0;
            this.mStartManual = false;
            oppoBrightUtils = mOppoBrightUtils;
            OppoBrightUtils.mUseAutoBrightness = false;
            oppoBrightUtils = mOppoBrightUtils;
            OppoBrightUtils.mManualSetAutoBrightness = false;
            mProximityNear = false;
            this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            if (this.mbStartTimer) {
                stopZeroTimer();
            }
            DisplayPowerController.mQuickDarkToBright = false;
            this.mBacklightStatus = 0;
            oppoBrightUtils = mOppoBrightUtils;
            if (OppoBrightUtils.mSeedLbrModeSupport && mCurrent_Lbr_Level > 0) {
                mCurrent_Lbr_Level = 0;
                mLast_Lbr_Level = 0;
                if (DEBUG) {
                    Slog.d(TAG, "go to OppoBrightUtils SEED_LBR_NODE write " + mCurrent_Lbr_Level);
                }
                mOppoBrightUtils.writeSeedLbrNodeValue(mCurrent_Lbr_Level);
            }
        }
        return false;
    }

    private void handleLightSensorEvent(long time, float lux) {
        this.mHandler.removeMessages(1);
        if (this.mAmbientLightRingBuffer.size() == 0) {
            adjustLightSensorRate(this.mNormalLightSensorRate);
        }
        applyLightSensorMeasurement(time, lux);
        updateAmbientLux(time);
    }

    private void applyLightSensorMeasurement(long time, float lux) {
        this.mRecentLightSamples++;
        if (time <= this.mLightSensorEnableTime + ((long) this.mAmbientLightHorizon)) {
            this.mInitialHorizonAmbientLightRingBuffer.push(time, lux);
        }
        this.mAmbientLightRingBuffer.prune(time - ((long) this.mAmbientLightHorizon));
        this.mAmbientLightRingBuffer.push(time, lux);
    }

    private void adjustLightSensorRate(int lightSensorRate) {
        if (lightSensorRate != this.mCurrentLightSensorRate) {
            if (DEBUG) {
                Slog.d(TAG, "adjustLightSensorRate: previousRate=" + this.mCurrentLightSensorRate + ", currentRate=" + lightSensorRate);
            }
            this.mCurrentLightSensorRate = lightSensorRate;
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
            this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, lightSensorRate * 1000, this.mHandler);
        }
    }

    private boolean setScreenAutoBrightnessAdjustment(float adjustment) {
        if (adjustment == this.mScreenAutoBrightnessAdjustment) {
            return false;
        }
        this.mScreenAutoBrightnessAdjustment = adjustment;
        if (!mOppoBrightUtils.isSpecialAdj(this.mScreenAutoBrightnessAdjustment) && this.mLightSensorEnabled && this.mAmbientLuxValid) {
            this.mManulBrightnessSlide = true;
            OppoBrightUtils oppoBrightUtils = mOppoBrightUtils;
            OppoBrightUtils.mManualBrightness = Math.round(adjustment);
        }
        return true;
    }

    private void setAmbientLux(float lux) {
        if (DEBUG) {
            Slog.d(TAG, "setAmbientLux(" + lux + ")");
        }
        this.mAmbientLux = lux;
        this.mBrighteningLuxThreshold = this.mDynamicHysteresis.getBrighteningThreshold(lux);
        this.mDarkeningLuxThreshold = this.mDynamicHysteresis.getDarkeningThreshold(lux);
    }

    private float calculateAmbientLux(long now, long horizon) {
        if (DEBUG) {
            Slog.d(TAG, "calculateAmbientLux(" + now + ", " + horizon + ")");
        }
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "calculateAmbientLux: No ambient light readings available");
            return -1.0f;
        }
        int endIndex = 0;
        long horizonStartTime = now - horizon;
        int i = 0;
        while (i < N - 1 && this.mAmbientLightRingBuffer.getTime(i + 1) <= horizonStartTime) {
            endIndex++;
            i++;
        }
        if (DEBUG) {
            Slog.d(TAG, "calculateAmbientLux: selected endIndex=" + endIndex + ", point=(" + this.mAmbientLightRingBuffer.getTime(endIndex) + ", " + this.mAmbientLightRingBuffer.getLux(endIndex) + ")");
        }
        float sum = OppoBrightUtils.MIN_LUX_LIMITI;
        float totalWeight = OppoBrightUtils.MIN_LUX_LIMITI;
        long endTime = AMBIENT_LIGHT_PREDICTION_TIME_MILLIS;
        for (i = N - 1; i >= endIndex; i--) {
            long eventTime = this.mAmbientLightRingBuffer.getTime(i);
            if (i == endIndex && eventTime < horizonStartTime) {
                eventTime = horizonStartTime;
            }
            long startTime = eventTime - now;
            if (startTime < 0) {
                startTime = 0;
            }
            float weight = calculateWeight(startTime, endTime);
            float lux = this.mAmbientLightRingBuffer.getLux(i);
            if (DEBUG) {
                Slog.d(TAG, "calculateAmbientLux: [" + startTime + ", " + endTime + "]: lux=" + lux + ", weight=" + weight);
            }
            totalWeight += weight;
            sum += this.mAmbientLightRingBuffer.getLux(i) * weight;
            endTime = startTime;
        }
        if (DEBUG) {
            Slog.d(TAG, "calculateAmbientLux: totalWeight=" + totalWeight + ", newAmbientLux=" + (sum / totalWeight));
        }
        return sum / totalWeight;
    }

    private float calculateWeight(long startDelta, long endDelta) {
        return weightIntegral(endDelta) - weightIntegral(startDelta);
    }

    private float weightIntegral(long x) {
        return ((float) x) * ((((float) x) * 0.5f) + ((float) this.mWeightingIntercept));
    }

    private long nextAmbientLightBrighteningTransition(long time) {
        long earliestValidTime = time;
        int i = this.mAmbientLightRingBuffer.size() - 1;
        while (i >= 0 && Math.round(this.mAmbientLightRingBuffer.getLux(i)) > Math.round(this.mBrighteningLuxThreshold)) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
            i--;
        }
        return this.mBrighteningLightDebounceConfig + earliestValidTime;
    }

    private long nextAmbientLightDarkeningTransition(long time) {
        long earliestValidTime = time;
        int i = this.mAmbientLightRingBuffer.size() - 1;
        while (i >= 0 && Math.round(this.mAmbientLightRingBuffer.getLux(i)) < Math.round(this.mDarkeningLuxThreshold)) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
            i--;
        }
        return this.mDarkeningLightDebounceConfig + earliestValidTime;
    }

    private void updateAmbientLux() {
        long time = SystemClock.uptimeMillis();
        this.mAmbientLightRingBuffer.prune(time - ((long) this.mAmbientLightHorizon));
        updateAmbientLux(time);
    }

    private void updateAmbientLux(long time) {
        if (!this.mAmbientLuxValid) {
            long timeWhenSensorWarmedUp = ((long) this.mLightSensorWarmUpTimeConfig) + this.mLightSensorEnableTime;
            if (time < timeWhenSensorWarmedUp) {
                if (DEBUG) {
                    Slog.d(TAG, "updateAmbientLux: Sensor not  ready yet: time=" + time + ", timeWhenSensorWarmedUp=" + timeWhenSensorWarmedUp);
                }
                this.mHandler.sendEmptyMessageAtTime(1, timeWhenSensorWarmedUp);
                return;
            }
            this.mBacklightStatus = 0;
            this.lux_beforemin = this.lux_min;
            this.lux_beforemax = this.lux_max;
            this.mareabefore = this.marea;
            setAmbientLux(calculateAmbientLux(time, 2000));
            this.mAmbientLuxValid = true;
            if (DEBUG) {
                Slog.d(TAG, "updateAmbientLux: Initializing: mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer + ", mAmbientLux=" + this.mAmbientLux);
            }
            updateAutoBrightness(true);
        }
        long nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
        long nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        float slowAmbientLux = calculateAmbientLux(time, 10000);
        float fastAmbientLux = calculateAmbientLux(time, 2000);
        float ambientLux = fastAmbientLux;
        if (this.mBacklightStatus == 2) {
            time = 0;
        }
        boolean screenOn = false;
        if (time - this.mLightSensorEnableTime <= 4000) {
            screenOn = true;
        }
        boolean pocketRinging = false;
        mOppoBrightUtils.setPocketRingingState(false);
        if (time - this.mProximitySensorChangeTime <= 4000 && mOppoBrightUtils.getPhoneState() == 1) {
            mOppoBrightUtils.setPocketRingingState(true);
            pocketRinging = true;
        }
        OppoBrightUtils oppoBrightUtils = mOppoBrightUtils;
        if (!OppoBrightUtils.mIsOledHighBrightness && ((Math.round(fastAmbientLux) >= Math.round(this.mBrighteningLuxThreshold) && this.mBacklightStatus == 1 && (nextBrightenTransition <= time || screenOn || pocketRinging)) || ((Math.round(fastAmbientLux) <= Math.round(this.mDarkeningLuxThreshold) && this.mBacklightStatus == 1 && nextDarkenTransition <= time) || (Math.round(fastAmbientLux) == 0 && this.mbStartTimer)))) {
            this.mBacklightStatus = 0;
            this.lux_beforemin = this.lux_min;
            this.lux_beforemax = this.lux_max;
            this.mareabefore = this.marea;
            if (DEBUG) {
                Slog.d(TAG, "ambientLux = " + Math.round(fastAmbientLux) + " LuxThreshold = " + Math.round(this.mBrighteningLuxThreshold));
            }
            setAmbientLux(fastAmbientLux);
            updateAutoBrightness(true);
            nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
            nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        }
        long nextTransitionTime = Math.min(nextDarkenTransition, nextBrightenTransition);
        if (nextTransitionTime <= time) {
            nextTransitionTime = time + ((long) this.mNormalLightSensorRate);
        }
        if (DEBUG) {
            Slog.d(TAG, "updateAmbientLux: Scheduling ambient lux update for " + nextTransitionTime + TimeUtils.formatUptime(nextTransitionTime));
        }
        this.mHandler.sendEmptyMessageAtTime(1, nextTransitionTime);
    }

    private void resetAutoBrightness(float ambientLux, float manulAtAmbientLux, int manulBrightness) {
        int lNowInterval = mOppoBrightUtils.resetAmbientLux(ambientLux);
        int i = manulBrightness;
        this.mScreenAutoBrightness = mOppoBrightUtils.calDragBrightness(clampScreenBrightness(Math.round(((float) PowerManager.BRIGHTNESS_MULTIBITS_ON) * this.mScreenAutoBrightnessSpline.interpolate(manulAtAmbientLux))), i, lNowInterval, mOppoBrightUtils.resetAmbientLux(manulAtAmbientLux), this.mScreenAutoBrightness);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateAutoBrightness(boolean sendUpdate) {
        if (this.mAmbientLuxValid || (this.mManulBrightnessSlide ^ 1) == 0) {
            float value = this.mScreenAutoBrightnessSpline.interpolate(this.mAmbientLux);
            String str;
            StringBuilder append;
            OppoBrightUtils oppoBrightUtils;
            OppoBrightUtils oppoBrightUtils2;
            if (this.mManulBrightnessSlide) {
                if (DEBUG) {
                    str = TAG;
                    append = new StringBuilder().append("mOppoBrightUtils.mManualBrightness = ");
                    oppoBrightUtils = mOppoBrightUtils;
                    Slog.d(str, append.append(OppoBrightUtils.mManualBrightness).append(" mAmbientLux = ").append(this.mAmbientLux).toString());
                }
                oppoBrightUtils2 = mOppoBrightUtils;
                OppoBrightUtils.mManulAtAmbientLux = this.mAmbientLux;
                this.mStartManual = true;
                this.mManulBrightnessSlide = false;
                oppoBrightUtils2 = mOppoBrightUtils;
                OppoBrightUtils.mManualSetAutoBrightness = true;
                oppoBrightUtils2 = mOppoBrightUtils;
                this.mScreenAutoBrightness = OppoBrightUtils.mManualBrightness;
                this.mCallbacks.updateBrightness();
            } else {
                if (sendUpdate) {
                    oppoBrightUtils2 = mOppoBrightUtils;
                    OppoBrightUtils.mManualSetAutoBrightness = false;
                }
                int newScreenAutoBrightness = clampScreenBrightness(Math.round(((float) PowerManager.BRIGHTNESS_MULTIBITS_ON) * value));
                oppoBrightUtils2 = mOppoBrightUtils;
                OppoBrightUtils oppoBrightUtils3 = mOppoBrightUtils;
                newScreenAutoBrightness = oppoBrightUtils2.findNightBrightness(OppoBrightUtils.mDisplayStateOn, newScreenAutoBrightness);
                if (this.mScreenAutoBrightness == newScreenAutoBrightness) {
                    oppoBrightUtils2 = mOppoBrightUtils;
                    if (OppoBrightUtils.mCameraMode != 1) {
                        oppoBrightUtils2 = mOppoBrightUtils;
                    }
                }
                if (DEBUG) {
                    str = TAG;
                    append = new StringBuilder().append("mScreenAutoBrightness = ").append(this.mScreenAutoBrightness).append(" newScreenAutoBrightness = ").append(newScreenAutoBrightness).append(" mOppoBrightUtils.mManualBrightness = ");
                    oppoBrightUtils = mOppoBrightUtils;
                    append = append.append(OppoBrightUtils.mManualBrightness).append(" mStartManual = ").append(this.mStartManual).append(" mOppoBrightUtils.mManualBrightnessBackup = ");
                    oppoBrightUtils = mOppoBrightUtils;
                    append = append.append(OppoBrightUtils.mManualBrightnessBackup).append(" mOppoBrightUtils.mDisplayStateOn = ");
                    oppoBrightUtils = mOppoBrightUtils;
                    append = append.append(OppoBrightUtils.mDisplayStateOn).append(" mBrightnessOverride = ");
                    oppoBrightUtils = mOppoBrightUtils;
                    Slog.d(str, append.append(OppoBrightUtils.mBrightnessOverride).toString());
                }
                int autoBrightness = this.mScreenAutoBrightness;
                this.mScreenAutoBrightness = newScreenAutoBrightness;
                this.mLastScreenAutoBrightnessGamma = 1.0f;
                oppoBrightUtils2 = mOppoBrightUtils;
                if (OppoBrightUtils.mDisplayStateOn) {
                    oppoBrightUtils2 = mOppoBrightUtils;
                    if (OppoBrightUtils.mManualBrightnessBackup != 0) {
                        this.mStartManual = true;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mManualBrightness = OppoBrightUtils.mManualBrightnessBackup;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mManulAtAmbientLux = OppoBrightUtils.mManualAmbientLuxBackup;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mManualBrightnessBackup = 0;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mManualAmbientLuxBackup = OppoBrightUtils.MIN_LUX_LIMITI;
                    }
                }
                oppoBrightUtils2 = mOppoBrightUtils;
                if (OppoBrightUtils.mBrightnessOverride == 0) {
                    oppoBrightUtils2 = mOppoBrightUtils;
                    if (OppoBrightUtils.mBrightnessOverrideAdj != 0 && this.mLightSensorEnabled) {
                        this.mStartManual = true;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mManualBrightness = OppoBrightUtils.mBrightnessOverrideAdj;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mManulAtAmbientLux = OppoBrightUtils.mBrightnessOverrideAmbientLux;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mBrightnessOverride = -1;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mBrightnessOverrideAdj = 0;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mBrightnessOverrideAmbientLux = OppoBrightUtils.MIN_LUX_LIMITI;
                    }
                }
                if (this.mStartManual && this.mLightSensorEnabled) {
                    float f = this.mAmbientLux;
                    oppoBrightUtils3 = mOppoBrightUtils;
                    if (f == OppoBrightUtils.mManulAtAmbientLux) {
                        oppoBrightUtils2 = mOppoBrightUtils;
                        this.mScreenAutoBrightness = OppoBrightUtils.mManualBrightness;
                    } else {
                        f = this.mAmbientLux;
                        oppoBrightUtils3 = mOppoBrightUtils;
                        float f2 = OppoBrightUtils.mManulAtAmbientLux;
                        oppoBrightUtils = mOppoBrightUtils;
                        resetAutoBrightness(f, f2, OppoBrightUtils.mManualBrightness);
                        f = this.mAmbientLux;
                        oppoBrightUtils3 = mOppoBrightUtils;
                        int i;
                        if (f > OppoBrightUtils.mManulAtAmbientLux) {
                            i = this.mScreenAutoBrightness;
                            oppoBrightUtils3 = mOppoBrightUtils;
                            this.mScreenAutoBrightness = Math.max(i, OppoBrightUtils.mManualBrightness);
                        } else {
                            f = this.mAmbientLux;
                            oppoBrightUtils3 = mOppoBrightUtils;
                            if (f < OppoBrightUtils.mManulAtAmbientLux) {
                                i = this.mScreenAutoBrightness;
                                oppoBrightUtils3 = mOppoBrightUtils;
                                this.mScreenAutoBrightness = Math.min(i, OppoBrightUtils.mManualBrightness);
                            }
                        }
                    }
                }
                oppoBrightUtils2 = mOppoBrightUtils;
                if (OppoBrightUtils.mCameraMode == 1) {
                    oppoBrightUtils2 = mOppoBrightUtils;
                    if (OppoBrightUtils.mCameraBacklight) {
                        this.mScreenAutoBrightness = mOppoBrightUtils.adjustCameraBrightness(this.mScreenAutoBrightness);
                    } else {
                        oppoBrightUtils2 = mOppoBrightUtils;
                        if (OppoBrightUtils.mGalleryBacklight) {
                            Slog.d(TAG, "There is no request in Gallery, do nothing");
                        }
                    }
                }
                oppoBrightUtils2 = mOppoBrightUtils;
                if (OppoBrightUtils.mBrightnessBitsConfig != 1) {
                    this.mAutoRate = mOppoBrightUtils.caclurateRateForIndex(this.mDeltaLux, autoBrightness, this.mScreenAutoBrightness);
                }
                if (sendUpdate) {
                    this.mCallbacks.updateBrightness();
                }
                oppoBrightUtils2 = mOppoBrightUtils;
                if (OppoBrightUtils.mDisplayStateOn && this.mLightSensorEnabled) {
                    oppoBrightUtils2 = mOppoBrightUtils;
                    if (OppoBrightUtils.mManualBrightnessBackup != 0) {
                        this.mStartManual = true;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mManualBrightness = OppoBrightUtils.mManualBrightnessBackup;
                        oppoBrightUtils2 = mOppoBrightUtils;
                        OppoBrightUtils.mManualBrightnessBackup = 0;
                    }
                }
                oppoBrightUtils2 = mOppoBrightUtils;
                OppoBrightUtils.mDisplayStateOn = false;
            }
        }
    }

    private int clampScreenBrightness(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void prepareBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mHandler.removeMessages(2);
        } else {
            this.mBrightnessAdjustmentSamplePending = true;
            this.mBrightnessAdjustmentSampleOldAdjustment = this.mScreenAutoBrightnessAdjustment;
            this.mBrightnessAdjustmentSampleOldLux = this.mAmbientLuxValid ? this.mAmbientLux : -1.0f;
            this.mBrightnessAdjustmentSampleOldBrightness = this.mScreenAutoBrightness;
            this.mBrightnessAdjustmentSampleOldGamma = this.mLastScreenAutoBrightnessGamma;
        }
        this.mHandler.sendEmptyMessageDelayed(2, 10000);
    }

    private void cancelBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = false;
            this.mHandler.removeMessages(2);
        }
    }

    private void collectBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = false;
            if (this.mAmbientLuxValid && this.mScreenAutoBrightness >= 0) {
                if (DEBUG) {
                    Slog.d(TAG, "Auto-brightness adjustment changed by user: adj=" + this.mScreenAutoBrightnessAdjustment + ", lux=" + this.mAmbientLux + ", brightness=" + this.mScreenAutoBrightness + ", gamma=" + this.mLastScreenAutoBrightnessGamma + ", ring=" + this.mAmbientLightRingBuffer);
                }
                EventLog.writeEvent(EventLogTags.AUTO_BRIGHTNESS_ADJ, new Object[]{Float.valueOf(this.mBrightnessAdjustmentSampleOldAdjustment), Float.valueOf(this.mBrightnessAdjustmentSampleOldLux), Integer.valueOf(this.mBrightnessAdjustmentSampleOldBrightness), Float.valueOf(this.mBrightnessAdjustmentSampleOldGamma), Float.valueOf(this.mScreenAutoBrightnessAdjustment), Float.valueOf(this.mAmbientLux), Integer.valueOf(this.mScreenAutoBrightness), Float.valueOf(this.mLastScreenAutoBrightnessGamma)});
            }
        }
    }
}
