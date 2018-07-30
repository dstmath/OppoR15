package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.uicc.UiccController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyController {
    private static final int EVENT_APPLY_RC_RESPONSE = 3;
    private static final int EVENT_FINISH_RC_RESPONSE = 4;
    private static final int EVENT_NOTIFICATION_RC_CHANGED = 1;
    private static final int EVENT_START_RC_RESPONSE = 2;
    private static final int EVENT_TIMEOUT = 5;
    static final String LOG_TAG = "ProxyController";
    private static final int SET_RC_STATUS_APPLYING = 3;
    private static final int SET_RC_STATUS_FAIL = 5;
    private static final int SET_RC_STATUS_IDLE = 0;
    private static final int SET_RC_STATUS_STARTED = 2;
    private static final int SET_RC_STATUS_STARTING = 1;
    private static final int SET_RC_STATUS_SUCCESS = 4;
    private static final int SET_RC_TIMEOUT_WAITING_MSEC = 45000;
    private static ProxyController sProxyController;
    private CommandsInterface[] mCi;
    private Context mContext;
    private String[] mCurrentLogicalModemIds;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            ProxyController.this.logd("handleMessage msg.what=" + msg.what);
            switch (msg.what) {
                case 1:
                    ProxyController.this.onNotificationRadioCapabilityChanged(msg);
                    return;
                case 2:
                    ProxyController.this.onStartRadioCapabilityResponse(msg);
                    return;
                case 3:
                    ProxyController.this.onApplyRadioCapabilityResponse(msg);
                    return;
                case 4:
                    ProxyController.this.onFinishRadioCapabilityResponse(msg);
                    return;
                case 5:
                    ProxyController.this.onTimeoutRadioCapability(msg);
                    return;
                default:
                    return;
            }
        }
    };
    private String[] mNewLogicalModemIds;
    private int[] mNewRadioAccessFamily;
    private int[] mOldRadioAccessFamily;
    private PhoneSubInfoController mPhoneSubInfoController;
    private PhoneSwitcher mPhoneSwitcher;
    private Phone[] mPhones;
    private int mRadioAccessFamilyStatusCounter;
    private int mRadioCapabilitySessionId;
    private int[] mSetRadioAccessFamilyStatus;
    private boolean mTransactionFailed = false;
    private UiccController mUiccController;
    private UiccPhoneBookController mUiccPhoneBookController;
    private UiccSmsController mUiccSmsController;
    private AtomicInteger mUniqueIdGenerator = new AtomicInteger(new Random().nextInt());
    WakeLock mWakeLock;

    public static ProxyController getInstance(Context context, Phone[] phone, UiccController uiccController, CommandsInterface[] ci, PhoneSwitcher ps) {
        if (sProxyController == null) {
            sProxyController = new ProxyController(context, phone, uiccController, ci, ps);
        }
        return sProxyController;
    }

    public static ProxyController getInstance() {
        return sProxyController;
    }

    private ProxyController(Context context, Phone[] phone, UiccController uiccController, CommandsInterface[] ci, PhoneSwitcher phoneSwitcher) {
        logd("Constructor - Enter");
        this.mContext = context;
        this.mPhones = phone;
        this.mUiccController = uiccController;
        this.mCi = ci;
        this.mPhoneSwitcher = phoneSwitcher;
        this.mUiccPhoneBookController = new UiccPhoneBookController(this.mPhones);
        this.mPhoneSubInfoController = new PhoneSubInfoController(this.mContext, this.mPhones);
        this.mUiccSmsController = new UiccSmsController();
        this.mSetRadioAccessFamilyStatus = new int[this.mPhones.length];
        this.mNewRadioAccessFamily = new int[this.mPhones.length];
        this.mOldRadioAccessFamily = new int[this.mPhones.length];
        this.mCurrentLogicalModemIds = new String[this.mPhones.length];
        this.mNewLogicalModemIds = new String[this.mPhones.length];
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
        clearTransaction();
        for (Phone registerForRadioCapabilityChanged : this.mPhones) {
            registerForRadioCapabilityChanged.registerForRadioCapabilityChanged(this.mHandler, 1, null);
        }
        logd("Constructor - Exit");
    }

    public void updateDataConnectionTracker(int sub) {
        this.mPhones[sub].updateDataConnectionTracker();
    }

    public void enableDataConnectivity(int sub) {
        this.mPhones[sub].setInternalDataEnabled(true, null);
    }

    public void disableDataConnectivity(int sub, Message dataCleanedUpMsg) {
        this.mPhones[sub].setInternalDataEnabled(false, dataCleanedUpMsg);
    }

    public void updateCurrentCarrierInProvider(int sub) {
        this.mPhones[sub].updateCurrentCarrierInProvider();
    }

    public void registerForAllDataDisconnected(int subId, Handler h, int what, Object obj) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            this.mPhones[phoneId].registerForAllDataDisconnected(h, what, obj);
        }
    }

    public void unregisterForAllDataDisconnected(int subId, Handler h) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            this.mPhones[phoneId].unregisterForAllDataDisconnected(h);
        }
    }

    public boolean isDataDisconnected(int subId) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            return true;
        }
        return this.mPhones[phoneId].mDcTracker.isDisconnected();
    }

    public int getRadioAccessFamily(int phoneId) {
        if (phoneId >= this.mPhones.length) {
            return 1;
        }
        return this.mPhones[phoneId].getRadioAccessFamily();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setRadioCapability(RadioAccessFamily[] rafs) {
        if (rafs.length != this.mPhones.length) {
            throw new RuntimeException("Length of input rafs must equal to total phone count");
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                if (this.mSetRadioAccessFamilyStatus[i] != 0) {
                    loge("setRadioCapability: Phone[" + i + "] is not idle. Rejecting request.");
                    return false;
                }
            }
        }
    }

    private boolean doSetRadioCapabilities(RadioAccessFamily[] rafs) {
        this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, this.mRadioCapabilitySessionId, 0), 45000);
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd("setRadioCapability: new request session id=" + this.mRadioCapabilitySessionId);
            resetRadioAccessFamilyStatusCounter();
            for (int i = 0; i < rafs.length; i++) {
                int phoneId = rafs[i].getPhoneId();
                logd("setRadioCapability: phoneId=" + phoneId + " status=STARTING");
                this.mSetRadioAccessFamilyStatus[phoneId] = 1;
                this.mOldRadioAccessFamily[phoneId] = this.mPhones[phoneId].getRadioAccessFamily();
                int requestedRaf = rafs[i].getRadioAccessFamily();
                this.mNewRadioAccessFamily[phoneId] = requestedRaf;
                this.mCurrentLogicalModemIds[phoneId] = this.mPhones[phoneId].getModemUuId();
                this.mNewLogicalModemIds[phoneId] = getLogicalModemIdFromRaf(requestedRaf);
                logd("setRadioCapability: mOldRadioAccessFamily[" + phoneId + "]=" + this.mOldRadioAccessFamily[phoneId]);
                logd("setRadioCapability: mNewRadioAccessFamily[" + phoneId + "]=" + this.mNewRadioAccessFamily[phoneId]);
                sendRadioCapabilityRequest(phoneId, this.mRadioCapabilitySessionId, 1, this.mOldRadioAccessFamily[phoneId], this.mCurrentLogicalModemIds[phoneId], 0, 2);
            }
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onStartRadioCapabilityResponse(Message msg) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            AsyncResult ar = msg.obj;
            if (TelephonyManager.getDefault().getPhoneCount() != 1 || ar.exception == null) {
                RadioCapability rc = ((AsyncResult) msg.obj).result;
                if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
                    logd("onStartRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
                    return;
                }
                this.mRadioAccessFamilyStatusCounter--;
                int id = rc.getPhoneId();
                if (((AsyncResult) msg.obj).exception != null) {
                    logd("onStartRadioCapabilityResponse: Error response session=" + rc.getSession());
                    logd("onStartRadioCapabilityResponse: phoneId=" + id + " status=FAIL");
                    this.mSetRadioAccessFamilyStatus[id] = 5;
                    this.mTransactionFailed = true;
                } else {
                    logd("onStartRadioCapabilityResponse: phoneId=" + id + " status=STARTED");
                    this.mSetRadioAccessFamilyStatus[id] = 2;
                }
                if (this.mRadioAccessFamilyStatusCounter == 0) {
                    HashSet<String> modemsInUse = new HashSet(this.mNewLogicalModemIds.length);
                    for (String modemId : this.mNewLogicalModemIds) {
                        if (!modemsInUse.add(modemId)) {
                            this.mTransactionFailed = true;
                            Log.wtf(LOG_TAG, "ERROR: sending down the same id for different phones");
                        }
                    }
                    logd("onStartRadioCapabilityResponse: success=" + (this.mTransactionFailed ^ 1));
                    if (this.mTransactionFailed) {
                        issueFinish(this.mRadioCapabilitySessionId);
                    } else {
                        resetRadioAccessFamilyStatusCounter();
                        for (int i = 0; i < this.mPhones.length; i++) {
                            sendRadioCapabilityRequest(i, this.mRadioCapabilitySessionId, 2, this.mNewRadioAccessFamily[i], this.mNewLogicalModemIds[i], 0, 3);
                            logd("onStartRadioCapabilityResponse: phoneId=" + i + " status=APPLYING");
                            this.mSetRadioAccessFamilyStatus[i] = 3;
                        }
                    }
                }
            } else {
                logd("onStartRadioCapabilityResponse got exception=" + ar.exception);
                this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
                this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED"));
                clearTransaction();
            }
        }
    }

    private void onApplyRadioCapabilityResponse(Message msg) {
        RadioCapability rc = ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            logd("onApplyRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
            return;
        }
        logd("onApplyRadioCapabilityResponse: rc=" + rc);
        if (((AsyncResult) msg.obj).exception != null) {
            synchronized (this.mSetRadioAccessFamilyStatus) {
                logd("onApplyRadioCapabilityResponse: Error response session=" + rc.getSession());
                int id = rc.getPhoneId();
                logd("onApplyRadioCapabilityResponse: phoneId=" + id + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[id] = 5;
                this.mTransactionFailed = true;
            }
        } else {
            logd("onApplyRadioCapabilityResponse: Valid start expecting notification rc=" + rc);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onNotificationRadioCapabilityChanged(Message msg) {
        RadioCapability rc = ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            logd("onNotificationRadioCapabilityChanged: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd("onNotificationRadioCapabilityChanged: rc=" + rc);
            if (rc.getSession() != this.mRadioCapabilitySessionId) {
                logd("onNotificationRadioCapabilityChanged: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
                return;
            }
            int id = rc.getPhoneId();
            if (((AsyncResult) msg.obj).exception != null || rc.getStatus() == 2) {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + id + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[id] = 5;
                this.mTransactionFailed = true;
            } else {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + id + " status=SUCCESS");
                this.mSetRadioAccessFamilyStatus[id] = 4;
                this.mPhoneSwitcher.resendDataAllowed(id);
                this.mPhones[id].radioCapabilityUpdated(rc);
            }
            this.mRadioAccessFamilyStatusCounter--;
            if (this.mRadioAccessFamilyStatusCounter == 0) {
                logd("onNotificationRadioCapabilityChanged: APPLY URC success=" + this.mTransactionFailed);
                issueFinish(this.mRadioCapabilitySessionId);
            }
        }
    }

    void onFinishRadioCapabilityResponse(Message msg) {
        RadioCapability rc = ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() == this.mRadioCapabilitySessionId) {
            synchronized (this.mSetRadioAccessFamilyStatus) {
                logd(" onFinishRadioCapabilityResponse mRadioAccessFamilyStatusCounter=" + this.mRadioAccessFamilyStatusCounter);
                this.mRadioAccessFamilyStatusCounter--;
                if (this.mRadioAccessFamilyStatusCounter == 0) {
                    completeRadioCapabilityTransaction();
                }
            }
            return;
        }
        logd("onFinishRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
    }

    private void onTimeoutRadioCapability(Message msg) {
        if (msg.arg1 != this.mRadioCapabilitySessionId) {
            logd("RadioCapability timeout: Ignore msg.arg1=" + msg.arg1 + "!= mRadioCapabilitySessionId=" + this.mRadioCapabilitySessionId);
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                logd("RadioCapability timeout: mSetRadioAccessFamilyStatus[" + i + "]=" + this.mSetRadioAccessFamilyStatus[i]);
            }
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            this.mRadioAccessFamilyStatusCounter = 0;
            this.mTransactionFailed = true;
            issueFinish(this.mRadioCapabilitySessionId);
        }
    }

    private void issueFinish(int sessionId) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                int i2;
                String str;
                int i3;
                logd("issueFinish: phoneId=" + i + " sessionId=" + sessionId + " mTransactionFailed=" + this.mTransactionFailed);
                this.mRadioAccessFamilyStatusCounter++;
                if (this.mTransactionFailed) {
                    i2 = this.mOldRadioAccessFamily[i];
                } else {
                    i2 = this.mNewRadioAccessFamily[i];
                }
                if (this.mTransactionFailed) {
                    str = this.mCurrentLogicalModemIds[i];
                } else {
                    str = this.mNewLogicalModemIds[i];
                }
                if (this.mTransactionFailed) {
                    i3 = 2;
                } else {
                    i3 = 1;
                }
                sendRadioCapabilityRequest(i, sessionId, 4, i2, str, i3, 4);
                if (this.mTransactionFailed) {
                    logd("issueFinish: phoneId: " + i + " status: FAIL");
                    this.mSetRadioAccessFamilyStatus[i] = 5;
                }
            }
        }
    }

    private void completeRadioCapabilityTransaction() {
        Intent intent;
        logd("onFinishRadioCapabilityResponse: success=" + (this.mTransactionFailed ^ 1));
        if (this.mTransactionFailed) {
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
            this.mTransactionFailed = false;
            RadioAccessFamily[] rafs = new RadioAccessFamily[this.mPhones.length];
            for (int phoneId = 0; phoneId < this.mPhones.length; phoneId++) {
                rafs[phoneId] = new RadioAccessFamily(phoneId, this.mOldRadioAccessFamily[phoneId]);
            }
            doSetRadioCapabilities(rafs);
        } else {
            ArrayList<RadioAccessFamily> phoneRAFList = new ArrayList();
            for (int i = 0; i < this.mPhones.length; i++) {
                int raf = this.mPhones[i].getRadioAccessFamily();
                logd("radioAccessFamily[" + i + "]=" + raf);
                phoneRAFList.add(new RadioAccessFamily(i, raf));
            }
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
            intent.putParcelableArrayListExtra("rafs", phoneRAFList);
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            clearTransaction();
        }
        this.mContext.sendBroadcast(intent, "android.permission.READ_PHONE_STATE");
    }

    private void clearTransaction() {
        logd("clearTransaction");
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                logd("clearTransaction: phoneId=" + i + " status=IDLE");
                this.mSetRadioAccessFamilyStatus[i] = 0;
                this.mOldRadioAccessFamily[i] = 0;
                this.mNewRadioAccessFamily[i] = 0;
                this.mTransactionFailed = false;
            }
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
    }

    private void resetRadioAccessFamilyStatusCounter() {
        this.mRadioAccessFamilyStatusCounter = this.mPhones.length;
    }

    private void sendRadioCapabilityRequest(int phoneId, int sessionId, int rcPhase, int radioFamily, String logicalModemId, int status, int eventId) {
        this.mPhones[phoneId].setRadioCapability(new RadioCapability(phoneId, sessionId, rcPhase, radioFamily, logicalModemId, status), this.mHandler.obtainMessage(eventId));
    }

    public int getMaxRafSupported() {
        int[] numRafSupported = new int[this.mPhones.length];
        int maxNumRafBit = 0;
        int maxRaf = 1;
        for (int len = 0; len < this.mPhones.length; len++) {
            numRafSupported[len] = Integer.bitCount(this.mPhones[len].getRadioAccessFamily());
            if (maxNumRafBit < numRafSupported[len]) {
                maxNumRafBit = numRafSupported[len];
                maxRaf = this.mPhones[len].getRadioAccessFamily();
            }
        }
        return maxRaf;
    }

    public int getMinRafSupported() {
        int[] numRafSupported = new int[this.mPhones.length];
        int minNumRafBit = 0;
        int minRaf = 1;
        int len = 0;
        while (len < this.mPhones.length) {
            numRafSupported[len] = Integer.bitCount(this.mPhones[len].getRadioAccessFamily());
            if (minNumRafBit == 0 || minNumRafBit > numRafSupported[len]) {
                minNumRafBit = numRafSupported[len];
                minRaf = this.mPhones[len].getRadioAccessFamily();
            }
            len++;
        }
        return minRaf;
    }

    private String getLogicalModemIdFromRaf(int raf) {
        for (int phoneId = 0; phoneId < this.mPhones.length; phoneId++) {
            if (this.mPhones[phoneId].getRadioAccessFamily() == raf) {
                return this.mPhones[phoneId].getModemUuId();
            }
        }
        return null;
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }

    private void loge(String string) {
        Rlog.e(LOG_TAG, string);
    }
}
