package com.android.internal.telephony.dataconnection;

import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkAddress;
import android.net.LinkProperties.CompareResult;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.DctConstants.Activity;
import com.android.internal.telephony.OemConstant;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.DataConnection.UpdateLinkPropertyResult;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class DcController extends StateMachine {
    static final int DATA_CONNECTION_ACTIVE_PH_LINK_DORMANT = 1;
    static final int DATA_CONNECTION_ACTIVE_PH_LINK_INACTIVE = 0;
    static final int DATA_CONNECTION_ACTIVE_PH_LINK_UP = 2;
    static final int DATA_CONNECTION_ACTIVE_UNKNOWN = Integer.MAX_VALUE;
    private static final boolean DBG = true;
    private static final boolean VDBG = false;
    private HashMap<Integer, DataConnection> mDcListActiveByCid = new HashMap();
    ArrayList<DataConnection> mDcListAll = new ArrayList();
    private DcTesterDeactivateAll mDcTesterDeactivateAll;
    private DccDefaultState mDccDefaultState = new DccDefaultState();
    private DcTracker mDct;
    private volatile boolean mExecutingCarrierChange;
    private Phone mPhone;
    private PhoneStateListener mPhoneStateListener;
    TelephonyManager mTelephonyManager;

    private class DccDefaultState extends State {
        /* synthetic */ DccDefaultState(DcController this$0, DccDefaultState -this1) {
            this();
        }

        private DccDefaultState() {
        }

        public void enter() {
            DcController.this.mPhone.mCi.registerForRilConnected(DcController.this.getHandler(), 262149, null);
            DcController.this.mPhone.mCi.registerForDataCallListChanged(DcController.this.getHandler(), 262151, null);
            if (Build.IS_DEBUGGABLE) {
                DcController.this.mDcTesterDeactivateAll = new DcTesterDeactivateAll(DcController.this.mPhone, DcController.this, DcController.this.getHandler());
            }
        }

        public void exit() {
            if (DcController.this.mPhone != null) {
                DcController.this.mPhone.mCi.unregisterForRilConnected(DcController.this.getHandler());
                DcController.this.mPhone.mCi.unregisterForDataCallListChanged(DcController.this.getHandler());
            }
            if (DcController.this.mDcTesterDeactivateAll != null) {
                DcController.this.mDcTesterDeactivateAll.dispose();
            }
        }

        public boolean processMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case 262149:
                    ar = msg.obj;
                    if (ar.exception != null) {
                        DcController.this.log("DccDefaultState: Unexpected exception on EVENT_RIL_CONNECTED");
                        break;
                    }
                    DcController.this.log("DccDefaultState: msg.what=EVENT_RIL_CONNECTED mRilVersion=" + ar.result);
                    break;
                case 262151:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        onDataStateChanged((ArrayList) ar.result);
                    } else {
                        DcController.this.log("DccDefaultState: EVENT_DATA_STATE_CHANGED: exception; likely radio not available, ignore");
                    }
                    if (OemConstant.getWlanAssistantEnable(DcController.this.mPhone.getContext())) {
                        SubscriptionManager s = SubscriptionManager.from(DcController.this.mPhone.getContext());
                        boolean isDefaultDataPhone = DcController.this.mPhone.getSubId() == SubscriptionManager.getDefaultDataSubId();
                        if (isDefaultDataPhone) {
                            boolean myMeasureDataState;
                            DcController dcController;
                            StringBuilder append;
                            StringBuilder append2;
                            boolean haveVsimIgnoreUserDataSetting;
                            boolean isRomming = DcController.this.mPhone.getServiceState().getRoaming();
                            DcController.this.mDct;
                            if (DcTracker.mMeasureDataState) {
                                DcController.this.mDct;
                                if (!((DcTracker.mDelayMeasure ^ 1) == 0 || (isRomming ^ 1) == 0)) {
                                    myMeasureDataState = !DcController.this.mDct.getDataEnabled() ? DcController.this.mDct.haveVsimIgnoreUserDataSetting() : true;
                                    dcController = DcController.this;
                                    append = new StringBuilder().append("WLAN+ EVENT_DATA_STATE_CHANGED: mMeasureDataState=");
                                    DcController.this.mDct;
                                    append2 = append.append(DcTracker.mMeasureDataState).append(" Roaming=").append(isRomming).append(" DataEnabled=");
                                    if (DcController.this.mDct.getDataEnabled()) {
                                        haveVsimIgnoreUserDataSetting = DcController.this.mDct.haveVsimIgnoreUserDataSetting();
                                    } else {
                                        haveVsimIgnoreUserDataSetting = true;
                                    }
                                    dcController.log(append2.append(haveVsimIgnoreUserDataSetting).append(" isDefaultDataPhone").append(isDefaultDataPhone).toString());
                                    if (myMeasureDataState) {
                                        new Thread() {
                                            public void run() {
                                                ConnectivityManager connectivityManager = (ConnectivityManager) DcController.this.mPhone.getContext().getSystemService("connectivity");
                                                if (!connectivityManager.measureDataState(DcController.this.mPhone.getServiceStateTracker().getSignalLevel())) {
                                                    NetworkRequest request = connectivityManager.getCelluarNetworkRequest();
                                                    if (request != null) {
                                                        DcController.this.mDct;
                                                        if (DcTracker.mMeasureDCCallback != null) {
                                                            DcController dcController = DcController.this;
                                                            StringBuilder append = new StringBuilder().append("WLAN+ EVENT_DATA_STATE_CHANGED release DC befor request: mMeasureDataState=");
                                                            DcController.this.mDct;
                                                            dcController.log(append.append(DcTracker.mMeasureDataState).toString());
                                                            try {
                                                                DcController.this.mDct;
                                                                connectivityManager.unregisterNetworkCallback(DcTracker.mMeasureDCCallback);
                                                            } catch (IllegalArgumentException e) {
                                                                DcController.this.log("WLAN+ " + e.toString());
                                                            } catch (Exception e2) {
                                                                DcController.this.log("WLAN+ Exception:" + e2.toString());
                                                            }
                                                        }
                                                        DcController.this.mDct;
                                                        DcTracker.mMeasureDCCallback = new NetworkCallback();
                                                        DcController.this.mDct;
                                                        connectivityManager.requestNetwork(request, DcTracker.mMeasureDCCallback);
                                                        connectivityManager.measureDataState(DcController.this.mPhone.getServiceStateTracker().getSignalLevel());
                                                    }
                                                }
                                            }
                                        }.start();
                                        break;
                                    }
                                }
                            }
                            myMeasureDataState = false;
                            dcController = DcController.this;
                            append = new StringBuilder().append("WLAN+ EVENT_DATA_STATE_CHANGED: mMeasureDataState=");
                            DcController.this.mDct;
                            append2 = append.append(DcTracker.mMeasureDataState).append(" Roaming=").append(isRomming).append(" DataEnabled=");
                            if (DcController.this.mDct.getDataEnabled()) {
                                haveVsimIgnoreUserDataSetting = true;
                            } else {
                                haveVsimIgnoreUserDataSetting = DcController.this.mDct.haveVsimIgnoreUserDataSetting();
                            }
                            dcController.log(append2.append(haveVsimIgnoreUserDataSetting).append(" isDefaultDataPhone").append(isDefaultDataPhone).toString());
                            if (myMeasureDataState) {
                                /* anonymous class already generated */.start();
                                break;
                            }
                        }
                    }
                    break;
            }
            return true;
        }

        private void onDataStateChanged(ArrayList<DataCallResponse> dcsList) {
            DataConnection dc;
            DcController.this.lr("onDataStateChanged: dcsList=" + dcsList + " mDcListActiveByCid=" + DcController.this.mDcListActiveByCid);
            HashMap<Integer, DataCallResponse> dataCallResponseListByCid = new HashMap();
            for (DataCallResponse dcs : dcsList) {
                dataCallResponseListByCid.put(Integer.valueOf(dcs.cid), dcs);
            }
            ArrayList<DataConnection> dcsToRetry = new ArrayList();
            for (DataConnection dc2 : DcController.this.mDcListActiveByCid.values()) {
                if (dataCallResponseListByCid.get(Integer.valueOf(dc2.mCid)) == null) {
                    DcController.this.log("onDataStateChanged: add to retry dc=" + dc2);
                    dcsToRetry.add(dc2);
                }
            }
            DcController.this.log("onDataStateChanged: dcsToRetry=" + dcsToRetry);
            ArrayList<ApnContext> apnsToCleanup = new ArrayList();
            boolean isAnyDataCallDormant = false;
            boolean isAnyDataCallActive = false;
            for (DataCallResponse newState : dcsList) {
                dc2 = (DataConnection) DcController.this.mDcListActiveByCid.get(Integer.valueOf(newState.cid));
                if (dc2 == null) {
                    DcController.this.loge("onDataStateChanged: no associated DC yet, ignore");
                } else {
                    if (dc2.mApnContexts.size() == 0) {
                        DcController.this.loge("onDataStateChanged: no connected apns, ignore");
                    } else {
                        DcController.this.log("onDataStateChanged: Found ConnId=" + newState.cid + " newState=" + newState.toString());
                        if (newState.active != 0) {
                            UpdateLinkPropertyResult result = dc2.updateLinkProperty(newState);
                            if (result.oldLp.equals(result.newLp)) {
                                DcController.this.log("onDataStateChanged: no change");
                            } else if (!result.oldLp.isIdenticalInterfaceName(result.newLp)) {
                                apnsToCleanup.addAll(dc2.mApnContexts.keySet());
                                DcController.this.log("onDataStateChanged: interface change, cleanup apns=" + dc2.mApnContexts);
                            } else if (result.oldLp.isIdenticalDnses(result.newLp) && (result.oldLp.isIdenticalRoutes(result.newLp) ^ 1) == 0 && (result.oldLp.isIdenticalHttpProxy(result.newLp) ^ 1) == 0 && (result.oldLp.isIdenticalAddresses(result.newLp) ^ 1) == 0) {
                                DcController.this.log("onDataStateChanged: no changes");
                            } else {
                                CompareResult<LinkAddress> car = result.oldLp.compareAddresses(result.newLp);
                                DcController.this.log("onDataStateChanged: oldLp=" + result.oldLp + " newLp=" + result.newLp + " car=" + car);
                                boolean needToClean = false;
                                for (LinkAddress added : car.added) {
                                    for (LinkAddress removed : car.removed) {
                                        if (NetworkUtils.addressTypeMatches(removed.getAddress(), added.getAddress())) {
                                            needToClean = true;
                                            break;
                                        }
                                    }
                                }
                                if (needToClean) {
                                    DcController.this.log("onDataStateChanged: addr change, cleanup apns=" + dc2.mApnContexts + " oldLp=" + result.oldLp + " newLp=" + result.newLp);
                                    apnsToCleanup.addAll(dc2.mApnContexts.keySet());
                                } else {
                                    DcController.this.log("onDataStateChanged: simple change");
                                    for (ApnContext apnContext : dc2.mApnContexts.keySet()) {
                                        DcController.this.mPhone.notifyDataConnection("linkPropertiesChanged", apnContext.getApnType());
                                    }
                                }
                            }
                        } else if (DcController.this.mDct.isCleanupRequired.get()) {
                            apnsToCleanup.addAll(dc2.mApnContexts.keySet());
                            DcController.this.mDct.isCleanupRequired.set(false);
                        } else {
                            DcFailCause failCause = DcFailCause.fromInt(newState.status);
                            if (failCause.isRestartRadioFail(DcController.this.mPhone.getContext(), DcController.this.mPhone.getSubId())) {
                                DcController.this.log("onDataStateChanged: X restart radio, failCause=" + failCause);
                                DcController.this.mDct.sendRestartRadio();
                            } else if (DcController.this.mDct.isPermanentFailure(failCause)) {
                                DcController.this.log("onDataStateChanged: inactive, add to cleanup list. failCause=" + failCause);
                                apnsToCleanup.addAll(dc2.mApnContexts.keySet());
                            } else {
                                DcController.this.log("onDataStateChanged: inactive, add to retry list. failCause=" + failCause);
                                dcsToRetry.add(dc2);
                            }
                        }
                    }
                    if (newState.active == 2) {
                        isAnyDataCallActive = true;
                    }
                    if (newState.active == 1) {
                        isAnyDataCallDormant = true;
                    }
                }
            }
            if (!isAnyDataCallDormant || (isAnyDataCallActive ^ 1) == 0) {
                DcController.this.log("onDataStateChanged: Data Activity updated to NONE. isAnyDataCallActive = " + isAnyDataCallActive + " isAnyDataCallDormant = " + isAnyDataCallDormant);
                if (isAnyDataCallActive) {
                    DcController.this.mDct.sendStartNetStatPoll(Activity.NONE);
                }
            } else {
                DcController.this.log("onDataStateChanged: Data Activity updated to DORMANT. stopNetStatePoll");
                DcController.this.mDct.sendStopNetStatPoll(Activity.DORMANT);
            }
            DcController.this.lr("onDataStateChanged: dcsToRetry=" + dcsToRetry + " apnsToCleanup=" + apnsToCleanup);
            for (ApnContext apnContext2 : apnsToCleanup) {
                DcController.this.mDct.sendCleanUpConnection(true, apnContext2);
            }
            for (DataConnection dc22 : dcsToRetry) {
                DcController.this.log("onDataStateChanged: send EVENT_LOST_CONNECTION dc.mTag=" + dc22.mTag);
                dc22.sendMessage(262153, dc22.mTag);
            }
        }
    }

    private DcController(String name, Phone phone, DcTracker dct, Handler handler) {
        super(name, handler);
        setLogRecSize(300);
        log("E ctor");
        this.mPhone = phone;
        this.mDct = dct;
        addState(this.mDccDefaultState);
        setInitialState(this.mDccDefaultState);
        log("X ctor");
        this.mPhoneStateListener = new PhoneStateListener(handler.getLooper()) {
            public void onCarrierNetworkChange(boolean active) {
                DcController.this.mExecutingCarrierChange = active;
            }
        };
        this.mTelephonyManager = (TelephonyManager) phone.getContext().getSystemService("phone");
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 65536);
        }
    }

    public static DcController makeDcc(Phone phone, DcTracker dct, Handler handler) {
        DcController dcc = new DcController("Dcc", phone, dct, handler);
        dcc.start();
        return dcc;
    }

    void dispose() {
        log("dispose: call quiteNow()");
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        quitNow();
    }

    void addDc(DataConnection dc) {
        this.mDcListAll.add(dc);
    }

    void removeDc(DataConnection dc) {
        this.mDcListActiveByCid.remove(Integer.valueOf(dc.mCid));
        this.mDcListAll.remove(dc);
    }

    public void addActiveDcByCid(DataConnection dc) {
        if (dc.mCid < 0) {
            log("addActiveDcByCid dc.mCid < 0 dc=" + dc);
        }
        this.mDcListActiveByCid.put(Integer.valueOf(dc.mCid), dc);
    }

    public DataConnection getActiveDcByCid(int cid) {
        return (DataConnection) this.mDcListActiveByCid.get(Integer.valueOf(cid));
    }

    void removeActiveDcByCid(DataConnection dc) {
        if (((DataConnection) this.mDcListActiveByCid.remove(Integer.valueOf(dc.mCid))) == null) {
            log("removeActiveDcByCid removedDc=null dc=" + dc);
        }
    }

    boolean isExecutingCarrierChange() {
        return this.mExecutingCarrierChange;
    }

    private void lr(String s) {
        logAndAddLogRec(s);
    }

    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    protected String getWhatToString(int what) {
        String info = DataConnection.cmdToString(what);
        if (info == null) {
            return DcAsyncChannel.cmdToString(what);
        }
        return info;
    }

    public String toString() {
        return "mDcListAll=" + this.mDcListAll + " mDcListActiveByCid=" + this.mDcListActiveByCid;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println(" mPhone=" + this.mPhone);
        pw.println(" mDcListAll=" + this.mDcListAll);
        pw.println(" mDcListActiveByCid=" + this.mDcListActiveByCid);
    }
}
