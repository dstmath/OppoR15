package com.android.server.location;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NavigationStatusMonitor {
    private static final int ACTION_AR_MOVING = 1;
    private static final int ACTION_AR_STILL = 0;
    private static final long DO_MONITOR_DELAY_TIME = 1000;
    private static final int MIN_DIS_NAV_DIST_NUM = 300;
    private static final int MIN_INDOOR_DIST_NUM = 60;
    private static final int MIN_INDOOR_RESET_NUM = 4;
    private static final float MIN_MOVING_ACC_VALUE = 10.98f;
    private static final int MIN_NAV_DIST_NUM = 20;
    private static final int MIN_NAV_RESET_NUM = 5;
    private static final int MIN_OUTDOOR_DIST_NUM = 10;
    private static final float MIN_OUTDOOR_SNR = 8.0f;
    private static final int MIN_OUTDOOR_SNR_NUM = 4;
    private static final int MIN_USED_SATELLITES_NUM = 4;
    private static final float MIN_USED_SATELLITES_SNR = 18.0f;
    private static final float MIN_VEHICLE_SPEED = 20.0f;
    private static final float MIN_WALK_SPEED = 2.0f;
    private static final int MSG_DOING_MONITOR = 103;
    private static final int MSG_START_MONITOR = 101;
    private static final int MSG_STOP_MONITOR = 102;
    public static final int NAVIGATION_STATUS_OFF = 2;
    public static final int NAVIGATION_STATUS_ON = 1;
    private static final int PRINT_LOG_FREQ = 10;
    private static final String TAG = "NavigationStatusMonitor";
    private static int sCurNavigationStatus = 1;
    private static boolean sIsNavNow = false;
    private boolean DEBUG = false;
    private Context mContext = null;
    private int mCurrArStatus = 0;
    private int mDisNavTimer = 0;
    private GpsStatus mGpsStatus = null;
    private Listener mGpsStatusListener = new Listener() {
        public void onGpsStatusChanged(int event) {
            NavigationStatusMonitor.this.mGpsStatus = NavigationStatusMonitor.this.mLocMgr.getGpsStatus(null);
        }
    };
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 101:
                    if (hasMessages(103)) {
                        removeMessages(103);
                    }
                    sendEmptyMessage(103);
                    return;
                case 102:
                    removeMessages(103);
                    return;
                case 103:
                    NavigationStatusMonitor.this.doMonitor();
                    sendEmptyMessageDelayed(103, 1000);
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mHasStart = false;
    private int mIndoorTimer = 0;
    private int mLastArStatus = 0;
    private LocationManager mLocMgr = null;
    private final Object mLock = new Object();
    private int mNavTimer = 0;
    private NavigationStatusListener mNavigationListener = null;
    private int mOutdoorTimer = 0;
    private GnssLocationProvider mProvider = null;
    private Sensor mSensor = null;
    SensorEventListener mSensorEventListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            synchronized (NavigationStatusMonitor.this.mLock) {
                if ((Math.abs(event.values[0]) + Math.abs(event.values[1])) + Math.abs(event.values[2]) > NavigationStatusMonitor.MIN_MOVING_ACC_VALUE) {
                    if (NavigationStatusMonitor.this.mCurrArStatus == 0) {
                        NavigationStatusMonitor.this.mLastArStatus = 0;
                    }
                    NavigationStatusMonitor.this.mCurrArStatus = 1;
                } else {
                    if (1 == NavigationStatusMonitor.this.mCurrArStatus) {
                        NavigationStatusMonitor.this.mLastArStatus = 1;
                    }
                    NavigationStatusMonitor.this.mCurrArStatus = 0;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private SensorManager mSensorManager = null;

    public interface NavigationStatusListener {
        void onNavigationStatusChanged(int i);
    }

    public NavigationStatusMonitor(Context context, GnssLocationProvider provider, NavigationStatusListener listener) {
        this.mContext = context;
        this.mProvider = provider;
        this.mNavigationListener = listener;
    }

    public void startMonitor() {
        this.mHasStart = true;
        resetStatus();
        startSensor();
        this.mHandler.sendEmptyMessage(101);
    }

    public void stopMonitor() {
        this.mHandler.sendEmptyMessage(102);
        this.mHasStart = false;
        tearDown();
        stopSensor();
    }

    public void resetStatus() {
        this.mIndoorTimer = 0;
        this.mOutdoorTimer = 0;
        this.mNavTimer = 0;
        this.mDisNavTimer = 0;
        sIsNavNow = false;
        sCurNavigationStatus = 1;
    }

    private void tearDown() {
        this.mIndoorTimer = 0;
        this.mOutdoorTimer = 0;
        this.mNavTimer = 0;
        this.mDisNavTimer = 0;
        sIsNavNow = false;
        sCurNavigationStatus = 2;
    }

    public static int getNavigateMode() {
        int navigationStatus = sCurNavigationStatus;
        if (sIsNavNow) {
            return 1;
        }
        return navigationStatus;
    }

    public void setDebug(boolean isDebug) {
        this.DEBUG = isDebug;
    }

    protected void init() {
        this.mLocMgr = (LocationManager) this.mContext.getSystemService("location");
        this.mLocMgr.addGpsStatusListener(this.mGpsStatusListener);
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensor = this.mSensorManager.getDefaultSensor(1);
        if (this.mSensor == null) {
            Log.e(TAG, "Init Sensor Error!!!");
        }
    }

    private void doMonitor() {
        Object obj;
        if (1 == sCurNavigationStatus) {
            float speed = this.mProvider.getSpeed();
            isNavigateMode(speed);
            obj = this.mLock;
            synchronized (obj) {
                if (isGnssDataCredible()) {
                    if ((speed > 2.0f) && this.mCurrArStatus == 0) {
                        this.mCurrArStatus = 1;
                        printLog("change to curr status " + this.mCurrArStatus);
                    }
                }
                if (1 == this.mCurrArStatus) {
                    this.mCurrArStatus = 1;
                    if (this.mLastArStatus == 0) {
                        this.mIndoorTimer = 0;
                        this.mLastArStatus = 1;
                    }
                }
            }
            if (isIndoorMode()) {
                printLog("change navigation status to NAVIGATION_STATUS_OFF <--");
                sCurNavigationStatus = 2;
                this.mNavigationListener.onNavigationStatusChanged(2);
                return;
            }
            return;
        } else if (2 == sCurNavigationStatus) {
            obj = this.mLock;
            synchronized (obj) {
                if (1 == this.mCurrArStatus) {
                    printLog("change navigation status to NAVIGATION_STATUS_ON -->");
                    sCurNavigationStatus = 1;
                    this.mNavigationListener.onNavigationStatusChanged(1);
                }
            }
        } else {
            return;
        }
    }

    public void doFlpSessionOn() {
        synchronized (this.mLock) {
            if (this.mHasStart && this.mCurrArStatus == 0 && 2 == sCurNavigationStatus) {
                printLog("FLP_SESSION_ON : change to NAVIGATION_STATUS_ON <--");
                resetStatus();
                this.mNavigationListener.onNavigationStatusChanged(1);
            }
        }
    }

    private boolean isGnssDataCredible() {
        if (this.mGpsStatus == null) {
            return false;
        }
        int usedNum = 0;
        for (GpsSatellite satellite : this.mGpsStatus.getSatellites()) {
            if (satellite.usedInFix() && MIN_USED_SATELLITES_SNR < satellite.getSnr()) {
                usedNum++;
                if (4 <= usedNum) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isIndoorStatus() {
        boolean isIndoorMode = true;
        if (this.mGpsStatus != null) {
            int outNum = 0;
            for (GpsSatellite satellite : this.mGpsStatus.getSatellites()) {
                if (MIN_OUTDOOR_SNR < satellite.getSnr()) {
                    outNum++;
                    if (4 <= outNum) {
                        isIndoorMode = false;
                    }
                }
            }
        }
        return isIndoorMode;
    }

    private boolean isIndoorMode() {
        boolean isIndoor = false;
        if (isIndoorStatus()) {
            this.mIndoorTimer++;
            if (4 <= this.mIndoorTimer) {
                this.mOutdoorTimer = 0;
                if (60 <= this.mIndoorTimer) {
                    isIndoor = true;
                }
            }
        } else {
            this.mOutdoorTimer++;
            if (4 <= this.mOutdoorTimer) {
                this.mIndoorTimer = 0;
                if (10 <= this.mOutdoorTimer) {
                    isIndoor = false;
                }
            }
        }
        if ((this.mIndoorTimer > 0 && this.mIndoorTimer % 10 == 0) || (this.mOutdoorTimer > 0 && this.mOutdoorTimer % 10 == 0)) {
            printLog("isIndoorMode  mIndoorTimer = " + this.mIndoorTimer + ", mOutdoorTimer = " + this.mOutdoorTimer);
        }
        return isIndoor;
    }

    private boolean isNavigateMode(float speed) {
        boolean isNavigate = sIsNavNow;
        if (isNavigateStatus(speed)) {
            this.mNavTimer++;
            if (5 <= this.mNavTimer) {
                this.mDisNavTimer = 0;
                if (!sIsNavNow && 20 <= this.mNavTimer) {
                    printLog("Change to Navigation status!!");
                    isNavigate = true;
                    sIsNavNow = true;
                }
            }
        } else {
            synchronized (this.mLock) {
                if (sIsNavNow && 1 == this.mCurrArStatus) {
                    if (this.mDisNavTimer != 0) {
                        printLog("Maybe in a tunnel!!");
                        this.mDisNavTimer = 0;
                    }
                } else {
                    this.mDisNavTimer++;
                    if (5 <= this.mDisNavTimer) {
                        this.mNavTimer = 0;
                    }
                    if (sIsNavNow && 300 <= this.mDisNavTimer) {
                        printLog("Change to disNavigation status!!");
                        isNavigate = false;
                        sIsNavNow = false;
                    }
                }
            }
        }
        if ((this.mNavTimer > 0 && this.mNavTimer % 10 == 0 && 20 >= this.mNavTimer) || (this.mDisNavTimer > 0 && this.mDisNavTimer % 10 == 0)) {
            printLog("isNavigateMode NavTime = " + this.mNavTimer + ", disNavTime = " + this.mDisNavTimer + ", mode = " + isNavigate);
        }
        return isNavigate;
    }

    private boolean isNavigateStatus(float speed) {
        return 20.0f < speed;
    }

    private void printLog(String log) {
        if (this.DEBUG) {
            Log.d(TAG, log);
        }
    }

    private void startSensor() {
        if (this.mSensor != null) {
            this.mSensorManager.registerListener(this.mSensorEventListener, this.mSensor, 2);
        }
    }

    private void stopSensor() {
        if (this.mSensor != null) {
            this.mSensorManager.unregisterListener(this.mSensorEventListener);
        }
    }
}
