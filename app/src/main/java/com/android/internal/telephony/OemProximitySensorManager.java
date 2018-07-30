package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;

public class OemProximitySensorManager {
    static String TAG = "OemProximitySensor";
    static OemProximitySensorManager sInstance = null;
    ProximitySensorEventListener mProximitySensorListener;

    public interface IOemListener {
        boolean isTestCard();

        void onBatteryChange(boolean z);

        void onCallChange(PhoneState phoneState);

        void onDataChange(PhoneState phoneState);

        void onHeadsetChange(boolean z);

        void onSensorChange(SensorState sensorState);

        void onSreenChange(boolean z);

        void onSuscriptionChange();

        void onSwitchChange(SwitchState switchState);

        void onUsbChange(boolean z);
    }

    public enum PhoneState {
        IDLE,
        OFFHOOK
    }

    private static class ProximitySensorEventListener extends Handler implements SensorEventListener {
        protected static final int EVENT_OEM_DATA_DELAY = 297;
        protected static final int EVENT_OEM_SCREEN_CHANGED = 298;
        private static final float FAR_THRESHOLD = 5.0f;
        protected static final int TIMER_DATA_DELAY = 3000;
        private volatile boolean mIsPhoneListen = false;
        private volatile boolean mIsProximListen = false;
        private volatile boolean mIsTestCard = false;
        private volatile boolean mLastBatteryPresent;
        private PhoneState mLastCallState;
        private PhoneState mLastDataState;
        private volatile boolean mLastHeadsetPresent;
        private volatile boolean mLastOtgPresent;
        private volatile boolean mLastScreenOn;
        private SensorState mLastSensorState;
        private volatile SwitchState mLastSwitch;
        private volatile boolean mLastUsbPresent;
        private final IOemListener mListener;
        private final float mMaxValue;
        private OemReceiver mOemReceiver = null;
        private OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = null;
        private OtgListener mOtgListener = null;
        private PhoneStateListener[] mPhoneStateListeners = null;
        private final Sensor mProximitySensor;
        private final SensorManager mSensorManager;

        protected class OemPhoneStateListener extends PhoneStateListener {
            ProximitySensorEventListener mPssListener;

            public OemPhoneStateListener(ProximitySensorEventListener pssListener, int slotId) {
                super(PhoneFactory.getPhone(slotId).getSubId(), slotId);
                this.mPssListener = pssListener;
            }

            public void onDataActivity(int direction) {
                this.mPssListener.onDataActivity(direction);
            }

            public void onCallStateChanged(int value, String incomingNumber) {
                this.mPssListener.onCallStateChanged(value, incomingNumber);
            }
        }

        protected class OemReceiver extends BroadcastReceiver {
            private static final String ACTION_BATTERY_CHANGED = "oppo.intent.action.BATTERY_PLUGGED_CHANGED";
            private boolean mIsBattery = false;
            private boolean mIsUsb = false;
            ProximitySensorEventListener mProximityListener;

            public OemReceiver(ProximitySensorEventListener orienListener) {
                this.mProximityListener = orienListener;
            }

            public void onReceive(Context context, Intent intent) {
                boolean z = true;
                if (intent != null) {
                    String action = intent.getAction();
                    if (action != null) {
                        synchronized (this) {
                            boolean usbPresent = false;
                            if (action.equals("android.hardware.usb.action.USB_STATE")) {
                                if (OemConstant.SWITCH_LOG) {
                                    Rlog.d(OemProximitySensorManager.TAG, "ACTION_USB_STATE:" + intent.getBooleanExtra(PhoneInternalInterface.REASON_CONNECTED, false));
                                }
                                if (intent.getBooleanExtra(PhoneInternalInterface.REASON_CONNECTED, false)) {
                                    usbPresent = true;
                                }
                                if (this.mIsUsb == usbPresent) {
                                    return;
                                }
                                this.mIsUsb = usbPresent;
                            } else if (action.equals(ACTION_BATTERY_CHANGED)) {
                                if (OemConstant.SWITCH_LOG) {
                                    Rlog.d(OemProximitySensorManager.TAG, "ACTION_BATTERY_CHANGED:" + intent.getIntExtra("plugged", 0));
                                }
                                if (1 == intent.getIntExtra("plugged", 0)) {
                                    usbPresent = true;
                                }
                                if (this.mIsBattery == usbPresent) {
                                    return;
                                }
                                this.mIsBattery = usbPresent;
                                if (OemConstant.needSetSarForBattery()) {
                                    this.mProximityListener.onBatteryStateChanged(this.mIsBattery);
                                    return;
                                }
                            } else if (action.equals("android.intent.action.HEADSET_PLUG")) {
                                boolean headsetPresent = false;
                                if (intent != null && intent.getIntExtra("state", 0) == 1) {
                                    headsetPresent = true;
                                }
                                this.mProximityListener.onHeadsetStateChanged(headsetPresent);
                                return;
                            } else {
                                return;
                            }
                            ProximitySensorEventListener proximitySensorEventListener = this.mProximityListener;
                            if (!this.mIsBattery) {
                                z = this.mIsUsb;
                            }
                            proximitySensorEventListener.onUsbStateChanged(z);
                        }
                    }
                }
            }

            public void register(Context context) {
                IntentFilter filter = new IntentFilter();
                if (OemConstant.needSetSarForHeadSet()) {
                    filter.addAction("android.intent.action.HEADSET_PLUG");
                } else {
                    filter.addAction("android.hardware.usb.action.USB_STATE");
                }
                filter.addAction(ACTION_BATTERY_CHANGED);
                context.registerReceiver(this, filter);
            }

            public void unregister(Context context) {
                context.unregisterReceiver(this);
            }
        }

        protected class OemSubscriptionsChangedListener extends OnSubscriptionsChangedListener {
            ProximitySensorEventListener mPssListener;

            public OemSubscriptionsChangedListener(ProximitySensorEventListener pssListener) {
                this.mPssListener = pssListener;
            }

            public void onSubscriptionsChanged() {
                if (OemConstant.SWITCH_LOG) {
                    Rlog.d(OemProximitySensorManager.TAG, "onSubscriptionsChanged");
                }
                this.mPssListener.onSubscriptionsChanged();
            }
        }

        protected class OtgListener extends StorageEventListener {
            String mLastPath = null;
            ProximitySensorEventListener mPssListener;
            StorageManager mSm = null;

            public OtgListener(ProximitySensorEventListener pssListener, Context context) {
                this.mPssListener = pssListener;
                this.mSm = (StorageManager) context.getSystemService("storage");
            }

            public void register(Context context) {
                this.mSm.registerListener(this);
            }

            public void unregister(Context context) {
                this.mSm.unregisterListener(this);
            }

            public void onStorageStateChanged(String path, String oldState, String newState) {
                if (OemConstant.SWITCH_LOG) {
                    Rlog.i(OemProximitySensorManager.TAG, "path:" + path + ",old:" + oldState + ",new:" + newState);
                }
                if (path != null && newState != null && !newState.equals(oldState)) {
                    if (newState.equals("mounted")) {
                        boolean isFind = false;
                        for (VolumeInfo volume : this.mSm.getVolumes()) {
                            if (path.equals(volume.path)) {
                                isFind = true;
                                String diskId = volume.getDiskId();
                                if (diskId != null) {
                                    DiskInfo diskInfo = this.mSm.findDiskById(diskId);
                                    if (diskInfo != null) {
                                        if (OemConstant.SWITCH_LOG) {
                                            Rlog.i(OemProximitySensorManager.TAG, "isUsb:" + diskInfo.isUsb());
                                        }
                                        if (!diskInfo.isUsb()) {
                                            return;
                                        }
                                        if (!isFind) {
                                            if (OemConstant.SWITCH_LOG) {
                                                Rlog.i(OemProximitySensorManager.TAG, "onOtgStateChanged 0 true:");
                                            }
                                            this.mLastPath = path;
                                            this.mPssListener.onOtgStateChanged(true);
                                        }
                                    }
                                    return;
                                }
                                return;
                            }
                        }
                        if (!isFind) {
                            if (OemConstant.SWITCH_LOG) {
                                Rlog.i(OemProximitySensorManager.TAG, "onOtgStateChanged 0 true:");
                            }
                            this.mLastPath = path;
                            this.mPssListener.onOtgStateChanged(true);
                        }
                    } else if (newState.equals("unmounted") || path.equals(this.mLastPath)) {
                        if (OemConstant.SWITCH_LOG) {
                            Rlog.i(OemProximitySensorManager.TAG, "onOtgStateChanged 0 false:");
                        }
                        this.mLastPath = null;
                        this.mPssListener.onOtgStateChanged(false);
                    }
                }
            }
        }

        public ProximitySensorEventListener(SensorManager sensorManager, Sensor proximitySensor, IOemListener listener) {
            this.mSensorManager = sensorManager;
            this.mProximitySensor = proximitySensor;
            this.mMaxValue = proximitySensor.getMaximumRange();
            this.mListener = listener;
            this.mLastSensorState = SensorState.FAR;
            this.mLastDataState = PhoneState.IDLE;
            this.mLastCallState = PhoneState.IDLE;
            this.mLastScreenOn = true;
            this.mLastUsbPresent = false;
            this.mLastOtgPresent = false;
            this.mLastHeadsetPresent = false;
            this.mLastBatteryPresent = false;
            this.mLastSwitch = SwitchState.FAR;
            Context context = getContext();
            this.mOnSubscriptionsChangedListener = new OemSubscriptionsChangedListener(this);
            int numPhones = TelephonyManager.from(context).getPhoneCount();
            this.mPhoneStateListeners = new OemPhoneStateListener[numPhones];
            for (int i = 0; i < numPhones; i++) {
                this.mPhoneStateListeners[i] = new OemPhoneStateListener(this, i);
            }
            this.mOemReceiver = new OemReceiver(this);
            if (OemConstant.needSetSarForUSB()) {
                this.mOtgListener = new OtgListener(this, context);
            }
        }

        public static Context getContext() {
            return PhoneFactory.getPhone(0).getContext();
        }

        public void register() {
            registerSceeen();
            registerSubInfo();
            Context context = getContext();
            this.mOemReceiver.register(context);
            if (OemConstant.needSetSarForUSB()) {
                this.mOtgListener.register(context);
            }
        }

        public void unregister() {
            unregisterScreen();
            unregisterSubInfo();
            Context context = getContext();
            this.mOemReceiver.unregister(context);
            if (OemConstant.needSetSarForUSB()) {
                this.mOtgListener.unregister(context);
            }
        }

        public void registerSceeen() {
            PhoneFactory.getPhone(0).getDeviceStateMonitor().registerForOemScreenChanged(this, EVENT_OEM_SCREEN_CHANGED, null);
        }

        public void unregisterScreen() {
            PhoneFactory.getPhone(0).getDeviceStateMonitor().unregisterOemScreenChanged(this);
        }

        public void registerSubInfo() {
            SubscriptionManager.from(PhoneFactory.getPhone(0).getContext()).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        }

        public void unregisterSubInfo() {
            SubscriptionManager.from(PhoneFactory.getPhone(0).getContext()).removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        }

        public void registerProximi() {
            if (!this.mIsProximListen) {
                this.mIsProximListen = true;
                this.mSensorManager.registerListener(this, this.mProximitySensor, 2);
            }
        }

        public void unregisterProximi() {
            if (this.mIsProximListen) {
                this.mIsProximListen = false;
                this.mSensorManager.unregisterListener(this);
            }
        }

        public void registerPhone() {
            this.mIsPhoneListen = true;
            TelephonyManager tm = TelephonyManager.from(PhoneFactory.getPhone(0).getContext());
            int numPhones = tm.getPhoneCount();
            for (int i = 0; i < numPhones; i++) {
                this.mPhoneStateListeners[i].setSubscripitonId(PhoneFactory.getPhone(i).getSubId());
                tm.listen(this.mPhoneStateListeners[i], 160);
            }
        }

        public void unregisterPhone() {
            this.mIsPhoneListen = false;
            TelephonyManager tm = TelephonyManager.from(PhoneFactory.getPhone(0).getContext());
            int numPhones = tm.getPhoneCount();
            for (int i = 0; i < numPhones; i++) {
                tm.listen(this.mPhoneStateListeners[i], 0);
            }
        }

        public void onSensorChanged(SensorEvent event) {
            if (event.values != null && event.values.length != 0) {
                SensorState state = getStateFromValue(event.values[0]);
                if (OemConstant.SWITCH_LOG) {
                    Rlog.d(OemProximitySensorManager.TAG, "onSensorChanged:" + state);
                }
                synchronized (this) {
                    if (state == this.mLastSensorState) {
                        return;
                    }
                    this.mLastSensorState = state;
                    this.mListener.onSensorChange(state);
                    processSwitch();
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_OEM_DATA_DELAY /*297*/:
                    synchronized (this) {
                        if (this.mLastDataState != PhoneState.IDLE) {
                            if (!this.mIsProximListen) {
                                registerProximi();
                                break;
                            }
                        }
                        return;
                    }
                    break;
                case EVENT_OEM_SCREEN_CHANGED /*298*/:
                    AsyncResult arscreen = msg.obj;
                    if (arscreen == null) {
                        Rlog.w(OemProximitySensorManager.TAG, "EVENT_OEM_SCREEN_CHANGED error");
                        break;
                    }
                    boolean isOn = ((Boolean) arscreen.result).booleanValue();
                    if (OemConstant.SWITCH_LOG) {
                        Rlog.w(OemProximitySensorManager.TAG, "EVENT_OEM_SCREEN_CHANGED " + isOn);
                    }
                    onSreenChanged(isOn);
                    break;
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onDataActivity(int direction) {
            PhoneState state = getDataStateFromValue(direction);
            if (OemConstant.SWITCH_LOG) {
                Rlog.d(OemProximitySensorManager.TAG, "onDataActivity:" + state);
            }
            synchronized (this) {
                if (state == this.mLastDataState) {
                    return;
                }
                this.mLastDataState = state;
                if (this.mLastDataState == PhoneState.OFFHOOK) {
                    Message msg = Message.obtain();
                    msg.what = EVENT_OEM_DATA_DELAY;
                    sendMessageDelayed(msg, 3000);
                } else {
                    removeMessages(EVENT_OEM_DATA_DELAY);
                    if (this.mLastCallState == PhoneState.IDLE && this.mIsProximListen) {
                        unregisterProximi();
                    }
                }
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onCallStateChanged(int value, String incomingNumber) {
            PhoneState state = getCallStateFromValue(value);
            if (OemConstant.SWITCH_LOG) {
                Rlog.d(OemProximitySensorManager.TAG, "onCallStateChanged:" + state);
            }
            synchronized (this) {
                if (state == this.mLastCallState) {
                    return;
                }
                this.mLastCallState = state;
                if (state == PhoneState.OFFHOOK) {
                    if (!this.mIsProximListen) {
                        registerProximi();
                    }
                } else if (this.mLastDataState == PhoneState.IDLE && this.mIsProximListen) {
                    unregisterProximi();
                }
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onSreenChanged(boolean isOn) {
            if (OemConstant.SWITCH_LOG) {
                Rlog.d(OemProximitySensorManager.TAG, "onSreenChange:" + isOn);
            }
            synchronized (this) {
                if (this.mLastScreenOn == isOn) {
                    return;
                }
                this.mLastScreenOn = isOn;
                if (this.mLastScreenOn) {
                    if (this.mIsPhoneListen) {
                        unregisterPhone();
                    }
                    if (!this.mIsTestCard && this.mIsProximListen) {
                        unregisterProximi();
                    }
                } else if (!this.mIsPhoneListen) {
                    registerPhone();
                }
            }
        }

        public void onSubscriptionsChanged() {
            boolean lastTestCard = this.mListener.isTestCard();
            this.mListener.onSuscriptionChange();
            this.mIsTestCard = this.mListener.isTestCard();
            synchronized (this) {
                if (this.mIsTestCard) {
                    unregisterScreen();
                    unregisterPhone();
                    registerProximi();
                } else {
                    unregisterProximi();
                    if (lastTestCard && this.mLastSensorState == SensorState.NEAR) {
                        this.mLastSensorState = SensorState.FAR;
                        this.mListener.onSensorChange(SensorState.FAR);
                        processSwitch();
                    }
                    if (!this.mLastScreenOn) {
                        unregisterPhone();
                        registerPhone();
                    }
                }
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onUsbStateChanged(boolean usbPresent) {
            synchronized (this) {
                if (this.mLastUsbPresent == usbPresent) {
                    return;
                }
                this.mLastUsbPresent = usbPresent;
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onOtgStateChanged(boolean otgPresent) {
            synchronized (this) {
                if (this.mLastOtgPresent == otgPresent) {
                    return;
                }
                this.mLastOtgPresent = otgPresent;
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onHeadsetStateChanged(boolean headsetPresent) {
            synchronized (this) {
                if (this.mLastHeadsetPresent == headsetPresent) {
                    return;
                }
                this.mLastHeadsetPresent = headsetPresent;
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onBatteryStateChanged(boolean batteryPresent) {
            synchronized (this) {
                if (this.mLastBatteryPresent == batteryPresent) {
                    return;
                }
                this.mLastBatteryPresent = batteryPresent;
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void processSwitch() {
            this.mIsTestCard = this.mListener.isTestCard();
            synchronized (this) {
                SwitchState state;
                boolean isSensorSwitchOn = this.mLastSensorState == SensorState.NEAR ? !this.mIsTestCard ? (this.mIsTestCard || this.mLastScreenOn) ? false : this.mLastDataState == PhoneState.OFFHOOK || this.mLastCallState == PhoneState.OFFHOOK : true : false;
                if (OemConstant.needSetSarForHeadSet()) {
                    state = isSensorSwitchOn ? this.mLastHeadsetPresent ? SwitchState.NEAR_HEADSET : SwitchState.NEAR : this.mLastHeadsetPresent ? SwitchState.FAR_HEADSET : SwitchState.FAR;
                } else {
                    state = isSensorSwitchOn ? (this.mLastUsbPresent || this.mLastOtgPresent) ? SwitchState.NEAR_USB : SwitchState.NEAR : (this.mLastUsbPresent || this.mLastOtgPresent) ? SwitchState.FAR_USB : SwitchState.FAR;
                    if (OemConstant.needSetSarForBattery()) {
                        if (isSensorSwitchOn) {
                            if (this.mLastBatteryPresent) {
                                state = SwitchState.NEAR_BATTERY;
                            }
                        } else if (this.mLastBatteryPresent) {
                            state = SwitchState.FAR_BATTERY;
                        }
                    }
                }
                if (this.mLastSwitch == state) {
                    return;
                }
                this.mLastSwitch = state;
            }
        }

        private SensorState getStateFromValue(float value) {
            return (value > FAR_THRESHOLD || value == this.mMaxValue) ? SensorState.FAR : SensorState.NEAR;
        }

        private PhoneState getDataStateFromValue(int value) {
            return (value == 1 || value == 2 || value == 3) ? PhoneState.OFFHOOK : PhoneState.IDLE;
        }

        private PhoneState getCallStateFromValue(int value) {
            return value == 0 ? PhoneState.IDLE : PhoneState.OFFHOOK;
        }
    }

    public enum SensorState {
        FAR,
        NEAR
    }

    public enum SwitchState {
        DEFAULT,
        NEAR,
        FAR,
        NEAR_USB,
        FAR_USB,
        NEAR_HEADSET,
        FAR_HEADSET,
        NEAR_BATTERY,
        FAR_BATTERY
    }

    public OemProximitySensorManager(Context context) {
        this(context, new IOemListener() {
            boolean mIsTestCard = false;

            public void onSensorChange(SensorState state) {
                this.mIsTestCard = initTestCard();
            }

            public void onDataChange(PhoneState state) {
            }

            public void onCallChange(PhoneState state) {
            }

            public void onSreenChange(boolean isOn) {
            }

            public void onUsbChange(boolean usbPresent) {
                this.mIsTestCard = initTestCard();
            }

            public void onHeadsetChange(boolean headsetPresent) {
                this.mIsTestCard = initTestCard();
            }

            public void onBatteryChange(boolean batteryPresent) {
                this.mIsTestCard = initTestCard();
            }

            public void onSuscriptionChange() {
                this.mIsTestCard = initTestCard();
            }

            public void onSwitchChange(SwitchState state) {
                PhoneFactory.getDefaultPhone().oppoSetSarRfStateV2(state.ordinal());
            }

            public boolean isTestCard() {
                return this.mIsTestCard;
            }

            private synchronized boolean initTestCard() {
                for (Phone phone : PhoneFactory.getPhones()) {
                    if (phone.is_test_card()) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public OemProximitySensorManager(Context context, IOemListener listener) {
        this.mProximitySensorListener = null;
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        Sensor proximitySensor = sensorManager.getDefaultSensor(8);
        if (proximitySensor == null) {
            this.mProximitySensorListener = null;
        } else {
            this.mProximitySensorListener = new ProximitySensorEventListener(sensorManager, proximitySensor, listener);
            this.mProximitySensorListener.register();
        }
        Rlog.d(TAG, "OemProximitySensorManager: init and release first");
        PhoneFactory.getDefaultPhone().oppoSetSarRfStateV2(2);
    }

    public static synchronized OemProximitySensorManager getDefault(Context context) {
        OemProximitySensorManager oemProximitySensorManager;
        synchronized (OemProximitySensorManager.class) {
            if (sInstance == null) {
                sInstance = new OemProximitySensorManager(context);
            }
            oemProximitySensorManager = sInstance;
        }
        return oemProximitySensorManager;
    }

    public void register() {
        this.mProximitySensorListener.register();
    }

    public void unregister() {
        if (this.mProximitySensorListener != null) {
            this.mProximitySensorListener.unregister();
        }
    }
}
