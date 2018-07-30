package com.android.internal.telephony.dataconnection;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.StringNetworkSpecifier;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import android.util.LocalLog;
import com.android.internal.telephony.OemConstant;
import com.android.internal.telephony.PhoneSwitcher;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.SubscriptionMonitor;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class TelephonyNetworkFactory extends NetworkFactory {
    protected static final boolean DBG = true;
    private static final int EVENT_ACTIVE_PHONE_SWITCH = 1;
    private static final int EVENT_DEFAULT_SUBSCRIPTION_CHANGED = 3;
    private static final int EVENT_NETWORK_RELEASE = 5;
    private static final int EVENT_NETWORK_REQUEST = 4;
    private static final int EVENT_SUBSCRIPTION_CHANGED = 2;
    private static final boolean RELEASE = false;
    private static final boolean REQUEST = true;
    private static final int REQUEST_LOG_SIZE = 40;
    private static final int TELEPHONY_NETWORK_SCORE = 50;
    private boolean DBG_OEM = false;
    public final String LOG_TAG;
    private final DcTracker mDcTracker;
    private final HashMap<NetworkRequest, LocalLog> mDefaultRequests = new HashMap();
    private final Handler mInternalHandler;
    private boolean mIsActive;
    private boolean mIsDefault;
    private int mPhoneId;
    private final PhoneSwitcher mPhoneSwitcher;
    private final HashMap<NetworkRequest, LocalLog> mSpecificRequests = new HashMap();
    private final SubscriptionController mSubscriptionController;
    private int mSubscriptionId;
    private final SubscriptionMonitor mSubscriptionMonitor;

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (TelephonyNetworkFactory.this.DBG_OEM) {
                TelephonyNetworkFactory.this.log("handleMessage: " + msg.what);
            }
            switch (msg.what) {
                case 1:
                    TelephonyNetworkFactory.this.onActivePhoneSwitch();
                    return;
                case 2:
                    TelephonyNetworkFactory.this.onSubIdChange();
                    return;
                case 3:
                    TelephonyNetworkFactory.this.onDefaultChange();
                    return;
                case 4:
                    TelephonyNetworkFactory.this.onNeedNetworkFor(msg);
                    return;
                case 5:
                    TelephonyNetworkFactory.this.onReleaseNetworkFor(msg);
                    return;
                default:
                    return;
            }
        }
    }

    public TelephonyNetworkFactory(PhoneSwitcher phoneSwitcher, SubscriptionController subscriptionController, SubscriptionMonitor subscriptionMonitor, Looper looper, Context context, int phoneId, DcTracker dcTracker) {
        super(looper, context, "TelephonyNetworkFactory[" + phoneId + "]", null);
        this.mInternalHandler = new InternalHandler(looper);
        setCapabilityFilter(makeNetworkFilter(subscriptionController, phoneId));
        setScoreFilter(50);
        this.mPhoneSwitcher = phoneSwitcher;
        this.mSubscriptionController = subscriptionController;
        this.mSubscriptionMonitor = subscriptionMonitor;
        this.mPhoneId = phoneId;
        this.LOG_TAG = "TelephonyNetworkFactory[" + phoneId + "]";
        this.mDcTracker = dcTracker;
        this.mIsActive = false;
        this.mPhoneSwitcher.registerForActivePhoneSwitch(this.mPhoneId, this.mInternalHandler, 1, null);
        this.mSubscriptionId = -1;
        this.mSubscriptionMonitor.registerForSubscriptionChanged(this.mPhoneId, this.mInternalHandler, 2, null);
        this.mIsDefault = false;
        this.mSubscriptionMonitor.registerForDefaultDataSubscriptionChanged(this.mPhoneId, this.mInternalHandler, 3, null);
        register();
        this.DBG_OEM = OemConstant.SWITCH_LOG;
    }

    private NetworkCapabilities makeNetworkFilter(SubscriptionController subscriptionController, int phoneId) {
        return makeNetworkFilter(subscriptionController.getSubIdUsingPhoneId(phoneId));
    }

    private NetworkCapabilities makeNetworkFilter(int subscriptionId) {
        NetworkCapabilities nc = new NetworkCapabilities();
        nc.addTransportType(0);
        nc.addCapability(0);
        nc.addCapability(1);
        nc.addCapability(2);
        nc.addCapability(3);
        nc.addCapability(4);
        nc.addCapability(5);
        nc.addCapability(7);
        nc.addCapability(8);
        nc.addCapability(9);
        nc.addCapability(10);
        nc.addCapability(13);
        nc.addCapability(12);
        nc.setNetworkSpecifier(new StringNetworkSpecifier(String.valueOf(subscriptionId)));
        return nc;
    }

    private void applyRequests(HashMap<NetworkRequest, LocalLog> requestMap, boolean action, String logStr) {
        for (NetworkRequest networkRequest : requestMap.keySet()) {
            LocalLog localLog = (LocalLog) requestMap.get(networkRequest);
            localLog.log(logStr);
            if (action) {
                this.mDcTracker.requestNetwork(networkRequest, localLog);
            } else {
                this.mDcTracker.releaseNetwork(networkRequest, localLog);
            }
        }
    }

    private void onActivePhoneSwitch() {
        boolean z = true;
        this.mIsActive = this.mPhoneSwitcher.isPhoneActive(this.mPhoneId);
        String logString = "onActivePhoneSwitch(" + this.mIsActive + ", " + this.mIsDefault + ")";
        log(logString);
        int mCurrentDefaultSubId = this.mSubscriptionController.getDefaultDataSubId();
        int mCurrentSubId = this.mSubscriptionController.getSubIdUsingPhoneId(this.mPhoneId);
        log("onActivePhoneSwitch() mCurrentDefaultSubId:" + mCurrentDefaultSubId + " mCurrentSubId:" + mCurrentSubId);
        if (this.mIsDefault || mCurrentDefaultSubId == mCurrentSubId) {
            boolean z2;
            HashMap hashMap = this.mDefaultRequests;
            if (this.mIsActive) {
                z2 = true;
            } else {
                z2 = false;
            }
            applyRequests(hashMap, z2, logString);
        }
        HashMap hashMap2 = this.mSpecificRequests;
        if (!this.mIsActive) {
            z = false;
        }
        applyRequests(hashMap2, z, logString);
    }

    private void onSubIdChange() {
        int newSubscriptionId = this.mSubscriptionController.getSubIdUsingPhoneId(this.mPhoneId);
        if (this.mSubscriptionId != newSubscriptionId) {
            log("onSubIdChange " + this.mSubscriptionId + "->" + newSubscriptionId);
            this.mSubscriptionId = newSubscriptionId;
            setCapabilityFilter(makeNetworkFilter(this.mSubscriptionId));
        } else if (this.DBG_OEM) {
            log("else-onSubIdChange " + this.mSubscriptionId + "->" + newSubscriptionId);
        }
    }

    private void onDefaultChange() {
        boolean newIsDefault = this.mSubscriptionController.getDefaultDataSubId() == this.mSubscriptionId;
        if (newIsDefault != this.mIsDefault) {
            this.mIsDefault = newIsDefault;
            String logString = "onDefaultChange(" + this.mIsActive + "," + this.mIsDefault + ")";
            log(logString);
            if (this.mIsActive && !this.mIsDefault) {
                applyRequests(this.mDefaultRequests, false, logString);
            }
        } else if (this.DBG_OEM) {
            log("else-onDefaultChange(" + this.mIsActive + "," + this.mIsDefault + ")");
        }
    }

    public void needNetworkFor(NetworkRequest networkRequest, int score) {
        Message msg = this.mInternalHandler.obtainMessage(4);
        msg.obj = networkRequest;
        msg.sendToTarget();
    }

    private void onNeedNetworkFor(Message msg) {
        LocalLog localLog;
        NetworkRequest networkRequest = msg.obj;
        boolean isApplicable = false;
        if (this.DBG_OEM) {
            log("onNeedNetworkFor: Network Request");
        }
        if (networkRequest.networkCapabilities.getNetworkSpecifier() == null) {
            localLog = (LocalLog) this.mDefaultRequests.get(networkRequest);
            if (this.DBG_OEM) {
                log("localLog: " + localLog);
            }
            if (localLog == null) {
                localLog = new LocalLog(40);
                localLog.log("created for " + networkRequest);
                this.mDefaultRequests.put(networkRequest, localLog);
                int defaultDataSubId = this.mSubscriptionController.getDefaultDataSubId();
                if (this.DBG_OEM) {
                    log("onNeedNetworkFor: mIsDefault = " + this.mIsDefault);
                }
                isApplicable = this.mIsDefault || defaultDataSubId == this.mSubscriptionId;
            }
        } else {
            localLog = (LocalLog) this.mSpecificRequests.get(networkRequest);
            if (localLog == null) {
                localLog = new LocalLog(40);
                this.mSpecificRequests.put(networkRequest, localLog);
                isApplicable = true;
            }
        }
        String s;
        if (this.mIsActive && isApplicable) {
            s = "onNeedNetworkFor";
            localLog.log(s);
            log(s + " " + networkRequest);
            this.mDcTracker.requestNetwork(networkRequest, localLog);
            return;
        }
        s = "not acting - isApp=" + isApplicable + ", isAct=" + this.mIsActive;
        localLog.log(s);
        log(s + " " + networkRequest);
    }

    public void releaseNetworkFor(NetworkRequest networkRequest) {
        Message msg = this.mInternalHandler.obtainMessage(5);
        msg.obj = networkRequest;
        msg.sendToTarget();
    }

    private void onReleaseNetworkFor(Message msg) {
        LocalLog localLog;
        boolean isApplicable;
        NetworkRequest networkRequest = msg.obj;
        if (this.DBG_OEM) {
            log("onReleaseNetworkFor: Release Request");
        }
        if (networkRequest.networkCapabilities.getNetworkSpecifier() == null) {
            localLog = (LocalLog) this.mDefaultRequests.remove(networkRequest);
            isApplicable = localLog != null ? this.mIsDefault : false;
        } else {
            localLog = (LocalLog) this.mSpecificRequests.remove(networkRequest);
            isApplicable = localLog != null;
        }
        String s;
        if (this.mIsActive && isApplicable) {
            s = "onReleaseNetworkFor";
            localLog.log(s);
            log(s + " " + networkRequest);
            this.mDcTracker.releaseNetwork(networkRequest, localLog);
            return;
        }
        s = "not releasing - isApp=" + isApplicable + ", isAct=" + this.mIsActive;
        localLog.log(s);
        log(s + " " + networkRequest);
    }

    protected void log(String s) {
        Rlog.d(this.LOG_TAG, s);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println(this.LOG_TAG + " mSubId=" + this.mSubscriptionId + " mIsActive=" + this.mIsActive + " mIsDefault=" + this.mIsDefault);
        pw.println("Default Requests:");
        pw.increaseIndent();
        for (NetworkRequest nr : this.mDefaultRequests.keySet()) {
            pw.println(nr);
            pw.increaseIndent();
            ((LocalLog) this.mDefaultRequests.get(nr)).dump(fd, pw, args);
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }
}
