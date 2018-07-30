package com.android.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.OppoManager;
import android.os.OppoUsageManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.format.Time;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.uicc.SpnOverride;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CallManager {
    private static final boolean DBG = true;
    private static final int EVENT_CALL_WAITING = 108;
    private static final int EVENT_CDMA_OTA_STATUS_CHANGE = 111;
    private static final int EVENT_DISCONNECT = 100;
    private static final int EVENT_DISPLAY_INFO = 109;
    private static final int EVENT_ECM_TIMER_RESET = 115;
    private static final int EVENT_INCOMING_RING = 104;
    private static final int EVENT_IN_CALL_VOICE_PRIVACY_OFF = 107;
    private static final int EVENT_IN_CALL_VOICE_PRIVACY_ON = 106;
    private static final int EVENT_MMI_COMPLETE = 114;
    private static final int EVENT_MMI_INITIATE = 113;
    private static final int EVENT_NEW_RINGING_CONNECTION = 102;
    private static final int EVENT_ONHOLD_TONE = 120;
    private static final int EVENT_POST_DIAL_CHARACTER = 119;
    private static final int EVENT_PRECISE_CALL_STATE_CHANGED = 101;
    private static final int EVENT_RESEND_INCALL_MUTE = 112;
    private static final int EVENT_RINGBACK_TONE = 105;
    private static final int EVENT_SERVICE_STATE_CHANGED = 118;
    private static final int EVENT_SIGNAL_INFO = 110;
    private static final int EVENT_SUBSCRIPTION_INFO_READY = 116;
    private static final int EVENT_SUPP_SERVICE_FAILED = 117;
    private static final int EVENT_TTY_MODE_RECEIVED = 122;
    private static final int EVENT_UNKNOWN_CONNECTION = 103;
    private static final CallManager INSTANCE = new CallManager();
    private static final String LOG_TAG = "CallManager";
    private static final boolean VDBG = false;
    private final ArrayList<Call> mBackgroundCalls = new ArrayList();
    protected final RegistrantList mCallWaitingRegistrants = new RegistrantList();
    protected final RegistrantList mCdmaOtaStatusChangeRegistrants = new RegistrantList();
    private Phone mDefaultPhone = null;
    protected final RegistrantList mDisconnectRegistrants = new RegistrantList();
    protected final RegistrantList mDisplayInfoRegistrants = new RegistrantList();
    protected final RegistrantList mEcmTimerResetRegistrants = new RegistrantList();
    private final ArrayList<Connection> mEmptyConnections = new ArrayList();
    private final ArrayList<Call> mForegroundCalls = new ArrayList();
    private final HashMap<Phone, CallManagerHandler> mHandlerMap = new HashMap();
    protected final RegistrantList mInCallVoicePrivacyOffRegistrants = new RegistrantList();
    protected final RegistrantList mInCallVoicePrivacyOnRegistrants = new RegistrantList();
    protected final RegistrantList mIncomingRingRegistrants = new RegistrantList();
    protected final RegistrantList mMmiCompleteRegistrants = new RegistrantList();
    protected final RegistrantList mMmiInitiateRegistrants = new RegistrantList();
    protected final RegistrantList mMmiRegistrants = new RegistrantList();
    protected final RegistrantList mNewRingingConnectionRegistrants = new RegistrantList();
    protected final RegistrantList mOnHoldToneRegistrants = new RegistrantList();
    OppoUsageManager mOppoUsageManager = OppoUsageManager.getOppoUsageManager();
    private final ArrayList<Phone> mPhones = new ArrayList();
    protected final RegistrantList mPostDialCharacterRegistrants = new RegistrantList();
    protected final RegistrantList mPreciseCallStateRegistrants = new RegistrantList();
    private Object mRegistrantidentifier = new Object();
    protected final RegistrantList mResendIncallMuteRegistrants = new RegistrantList();
    protected final RegistrantList mRingbackToneRegistrants = new RegistrantList();
    private final ArrayList<Call> mRingingCalls = new ArrayList();
    protected final RegistrantList mServiceStateChangedRegistrants = new RegistrantList();
    protected final RegistrantList mSignalInfoRegistrants = new RegistrantList();
    private boolean mSpeedUpAudioForMtCall = false;
    protected final RegistrantList mSubscriptionInfoReadyRegistrants = new RegistrantList();
    protected final RegistrantList mSuppServiceFailedRegistrants = new RegistrantList();
    protected final RegistrantList mTtyModeReceivedRegistrants = new RegistrantList();
    protected final RegistrantList mUnknownConnectionRegistrants = new RegistrantList();

    private class CallManagerHandler extends Handler {
        /* synthetic */ CallManagerHandler(CallManager this$0, CallManagerHandler -this1) {
            this();
        }

        private CallManagerHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    CallManager.this.mDisconnectRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    CallManager.this.writeCallRecord(msg.obj.result);
                    return;
                case 101:
                    CallManager.this.mPreciseCallStateRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 102:
                    Connection c = ((AsyncResult) msg.obj).result;
                    if (CallManager.this.getActiveFgCallState(c.getCall().getPhone().getSubId()).isDialing() || CallManager.this.hasMoreThanOneRingingCall()) {
                        try {
                            Rlog.d(CallManager.LOG_TAG, "silently drop incoming call: " + c.getCall());
                            c.getCall().hangup();
                            return;
                        } catch (CallStateException e) {
                            Rlog.w(CallManager.LOG_TAG, "new ringing connection", e);
                            return;
                        }
                    }
                    CallManager.this.mNewRingingConnectionRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case CallManager.EVENT_UNKNOWN_CONNECTION /*103*/:
                    CallManager.this.mUnknownConnectionRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case CallManager.EVENT_INCOMING_RING /*104*/:
                    if (!CallManager.this.hasActiveFgCall()) {
                        CallManager.this.mIncomingRingRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                        return;
                    }
                    return;
                case CallManager.EVENT_RINGBACK_TONE /*105*/:
                    CallManager.this.mRingbackToneRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 106:
                    CallManager.this.mInCallVoicePrivacyOnRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case CallManager.EVENT_IN_CALL_VOICE_PRIVACY_OFF /*107*/:
                    CallManager.this.mInCallVoicePrivacyOffRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case CallManager.EVENT_CALL_WAITING /*108*/:
                    CallManager.this.mCallWaitingRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case CallManager.EVENT_DISPLAY_INFO /*109*/:
                    CallManager.this.mDisplayInfoRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case CallManager.EVENT_SIGNAL_INFO /*110*/:
                    CallManager.this.mSignalInfoRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 111:
                    CallManager.this.mCdmaOtaStatusChangeRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 112:
                    CallManager.this.mResendIncallMuteRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 113:
                    CallManager.this.mMmiInitiateRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 114:
                    Rlog.d(CallManager.LOG_TAG, "CallManager: handleMessage (EVENT_MMI_COMPLETE)");
                    CallManager.this.mMmiCompleteRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 115:
                    CallManager.this.mEcmTimerResetRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 116:
                    CallManager.this.mSubscriptionInfoReadyRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 117:
                    CallManager.this.mSuppServiceFailedRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 118:
                    CallManager.this.mServiceStateChangedRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 119:
                    for (int i = 0; i < CallManager.this.mPostDialCharacterRegistrants.size(); i++) {
                        Message notifyMsg = ((Registrant) CallManager.this.mPostDialCharacterRegistrants.get(i)).messageForRegistrant();
                        notifyMsg.obj = msg.obj;
                        notifyMsg.arg1 = msg.arg1;
                        notifyMsg.sendToTarget();
                    }
                    return;
                case 120:
                    CallManager.this.mOnHoldToneRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                case 122:
                    CallManager.this.mTtyModeReceivedRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    private CallManager() {
    }

    public static CallManager getInstance() {
        return INSTANCE;
    }

    public List<Phone> getAllPhones() {
        return Collections.unmodifiableList(this.mPhones);
    }

    private Phone getPhone(int subId) {
        for (Phone phone : this.mPhones) {
            if (phone.getSubId() == subId && phone.getPhoneType() != 5) {
                return phone;
            }
        }
        return null;
    }

    public State getState() {
        State s = State.IDLE;
        for (Phone phone : this.mPhones) {
            if (phone.getState() == State.RINGING) {
                s = State.RINGING;
            } else if (phone.getState() == State.OFFHOOK && s == State.IDLE) {
                s = State.OFFHOOK;
            }
        }
        return s;
    }

    public State getState(int subId) {
        State s = State.IDLE;
        for (Phone phone : this.mPhones) {
            if (phone.getSubId() == subId) {
                if (phone.getState() == State.RINGING) {
                    s = State.RINGING;
                } else if (phone.getState() == State.OFFHOOK && s == State.IDLE) {
                    s = State.OFFHOOK;
                }
            }
        }
        return s;
    }

    public int getServiceState() {
        int resultState = 1;
        for (Phone phone : this.mPhones) {
            int serviceState = phone.getServiceState().getState();
            if (serviceState == 0) {
                return serviceState;
            }
            if (serviceState == 1) {
                if (resultState == 2 || resultState == 3) {
                    resultState = serviceState;
                }
            } else if (serviceState == 2 && resultState == 3) {
                resultState = serviceState;
            }
        }
        return resultState;
    }

    public int getServiceState(int subId) {
        int resultState = 1;
        for (Phone phone : this.mPhones) {
            if (phone.getSubId() == subId) {
                int serviceState = phone.getServiceState().getState();
                if (serviceState == 0) {
                    return serviceState;
                }
                if (serviceState == 1) {
                    if (resultState == 2 || resultState == 3) {
                        resultState = serviceState;
                    }
                } else if (serviceState == 2 && resultState == 3) {
                    resultState = serviceState;
                }
            }
        }
        return resultState;
    }

    public Phone getPhoneInCall() {
        if (!getFirstActiveRingingCall().isIdle()) {
            return getFirstActiveRingingCall().getPhone();
        }
        if (getActiveFgCall().isIdle()) {
            return getFirstActiveBgCall().getPhone();
        }
        return getActiveFgCall().getPhone();
    }

    public Phone getPhoneInCall(int subId) {
        if (!getFirstActiveRingingCall(subId).isIdle()) {
            return getFirstActiveRingingCall(subId).getPhone();
        }
        if (getActiveFgCall(subId).isIdle()) {
            return getFirstActiveBgCall(subId).getPhone();
        }
        return getActiveFgCall(subId).getPhone();
    }

    public boolean registerPhone(Phone phone) {
        if (phone == null || (this.mPhones.contains(phone) ^ 1) == 0) {
            return false;
        }
        Rlog.d(LOG_TAG, "registerPhone(" + phone.getPhoneName() + " " + phone + ")");
        if (this.mPhones.isEmpty()) {
            this.mDefaultPhone = phone;
        }
        this.mPhones.add(phone);
        this.mRingingCalls.add(phone.getRingingCall());
        this.mBackgroundCalls.add(phone.getBackgroundCall());
        this.mForegroundCalls.add(phone.getForegroundCall());
        registerForPhoneStates(phone);
        return true;
    }

    public void unregisterPhone(Phone phone) {
        if (phone != null && this.mPhones.contains(phone)) {
            Rlog.d(LOG_TAG, "unregisterPhone(" + phone.getPhoneName() + " " + phone + ")");
            Phone imsPhone = phone.getImsPhone();
            if (imsPhone != null) {
                unregisterPhone(imsPhone);
            }
            this.mPhones.remove(phone);
            this.mRingingCalls.remove(phone.getRingingCall());
            this.mBackgroundCalls.remove(phone.getBackgroundCall());
            this.mForegroundCalls.remove(phone.getForegroundCall());
            unregisterForPhoneStates(phone);
            if (phone != this.mDefaultPhone) {
                return;
            }
            if (this.mPhones.isEmpty()) {
                this.mDefaultPhone = null;
            } else {
                this.mDefaultPhone = (Phone) this.mPhones.get(0);
            }
        }
    }

    public Phone getDefaultPhone() {
        return this.mDefaultPhone;
    }

    public Phone getFgPhone() {
        return getActiveFgCall().getPhone();
    }

    public Phone getFgPhone(int subId) {
        return getActiveFgCall(subId).getPhone();
    }

    public Phone getBgPhone() {
        return getFirstActiveBgCall().getPhone();
    }

    public Phone getBgPhone(int subId) {
        return getFirstActiveBgCall(subId).getPhone();
    }

    public Phone getRingingPhone() {
        return getFirstActiveRingingCall().getPhone();
    }

    public Phone getRingingPhone(int subId) {
        return getFirstActiveRingingCall(subId).getPhone();
    }

    private Context getContext() {
        Phone defaultPhone = getDefaultPhone();
        if (defaultPhone == null) {
            return null;
        }
        return defaultPhone.getContext();
    }

    public Object getRegistrantIdentifier() {
        return this.mRegistrantidentifier;
    }

    private void registerForPhoneStates(Phone phone) {
        if (((CallManagerHandler) this.mHandlerMap.get(phone)) != null) {
            Rlog.d(LOG_TAG, "This phone has already been registered.");
            return;
        }
        CallManagerHandler handler = new CallManagerHandler();
        this.mHandlerMap.put(phone, handler);
        phone.registerForPreciseCallStateChanged(handler, 101, this.mRegistrantidentifier);
        phone.registerForDisconnect(handler, 100, this.mRegistrantidentifier);
        phone.registerForNewRingingConnection(handler, 102, this.mRegistrantidentifier);
        phone.registerForUnknownConnection(handler, EVENT_UNKNOWN_CONNECTION, this.mRegistrantidentifier);
        phone.registerForIncomingRing(handler, EVENT_INCOMING_RING, this.mRegistrantidentifier);
        phone.registerForRingbackTone(handler, EVENT_RINGBACK_TONE, this.mRegistrantidentifier);
        phone.registerForInCallVoicePrivacyOn(handler, 106, this.mRegistrantidentifier);
        phone.registerForInCallVoicePrivacyOff(handler, EVENT_IN_CALL_VOICE_PRIVACY_OFF, this.mRegistrantidentifier);
        phone.registerForDisplayInfo(handler, EVENT_DISPLAY_INFO, this.mRegistrantidentifier);
        phone.registerForSignalInfo(handler, EVENT_SIGNAL_INFO, this.mRegistrantidentifier);
        phone.registerForResendIncallMute(handler, 112, this.mRegistrantidentifier);
        phone.registerForMmiInitiate(handler, 113, this.mRegistrantidentifier);
        phone.registerForMmiComplete(handler, 114, this.mRegistrantidentifier);
        phone.registerForSuppServiceFailed(handler, 117, this.mRegistrantidentifier);
        phone.registerForServiceStateChanged(handler, 118, this.mRegistrantidentifier);
        phone.setOnPostDialCharacter(handler, 119, null);
        phone.registerForCdmaOtaStatusChange(handler, 111, null);
        phone.registerForSubscriptionInfoReady(handler, 116, null);
        phone.registerForCallWaiting(handler, EVENT_CALL_WAITING, null);
        phone.registerForEcmTimerReset(handler, 115, null);
        phone.registerForOnHoldTone(handler, 120, null);
        phone.registerForTtyModeReceived(handler, 122, null);
    }

    private void unregisterForPhoneStates(Phone phone) {
        CallManagerHandler handler = (CallManagerHandler) this.mHandlerMap.get(phone);
        if (handler == null) {
            Rlog.e(LOG_TAG, "Could not find Phone handler for unregistration");
            return;
        }
        this.mHandlerMap.remove(phone);
        phone.unregisterForPreciseCallStateChanged(handler);
        phone.unregisterForDisconnect(handler);
        phone.unregisterForNewRingingConnection(handler);
        phone.unregisterForUnknownConnection(handler);
        phone.unregisterForIncomingRing(handler);
        phone.unregisterForRingbackTone(handler);
        phone.unregisterForInCallVoicePrivacyOn(handler);
        phone.unregisterForInCallVoicePrivacyOff(handler);
        phone.unregisterForDisplayInfo(handler);
        phone.unregisterForSignalInfo(handler);
        phone.unregisterForResendIncallMute(handler);
        phone.unregisterForMmiInitiate(handler);
        phone.unregisterForMmiComplete(handler);
        phone.unregisterForSuppServiceFailed(handler);
        phone.unregisterForServiceStateChanged(handler);
        phone.unregisterForTtyModeReceived(handler);
        phone.setOnPostDialCharacter(null, 119, null);
        phone.unregisterForCdmaOtaStatusChange(handler);
        phone.unregisterForSubscriptionInfoReady(handler);
        phone.unregisterForCallWaiting(handler);
        phone.unregisterForEcmTimerReset(handler);
        phone.unregisterForOnHoldTone(handler);
        phone.unregisterForSuppServiceFailed(handler);
    }

    public void acceptCall(Call ringingCall) throws CallStateException {
        Phone ringingPhone = ringingCall.getPhone();
        if (hasActiveFgCall()) {
            Phone activePhone = getActiveFgCall().getPhone();
            boolean hasBgCall = activePhone.getBackgroundCall().isIdle() ^ 1;
            boolean sameChannel = activePhone == ringingPhone;
            if (sameChannel && hasBgCall) {
                getActiveFgCall().hangup();
            } else if (!sameChannel && (hasBgCall ^ 1) != 0) {
                activePhone.switchHoldingAndActive();
            } else if (!sameChannel && hasBgCall) {
                getActiveFgCall().hangup();
            }
        }
        ringingPhone.acceptCall(0);
    }

    public void rejectCall(Call ringingCall) throws CallStateException {
        ringingCall.getPhone().rejectCall();
    }

    public void switchHoldingAndActive(Call heldCall) throws CallStateException {
        Phone activePhone = null;
        Phone heldPhone = null;
        if (hasActiveFgCall()) {
            activePhone = getActiveFgCall().getPhone();
        }
        if (heldCall != null) {
            heldPhone = heldCall.getPhone();
        }
        if (activePhone != null) {
            activePhone.switchHoldingAndActive();
        }
        if (heldPhone != null && heldPhone != activePhone) {
            heldPhone.switchHoldingAndActive();
        }
    }

    public void hangupForegroundResumeBackground(Call heldCall) throws CallStateException {
        if (hasActiveFgCall()) {
            Phone foregroundPhone = getFgPhone();
            if (heldCall == null) {
                return;
            }
            if (foregroundPhone == heldCall.getPhone()) {
                getActiveFgCall().hangup();
                return;
            }
            getActiveFgCall().hangup();
            switchHoldingAndActive(heldCall);
        }
    }

    public boolean canConference(Call heldCall) {
        Phone activePhone = null;
        Phone heldPhone = null;
        if (hasActiveFgCall()) {
            activePhone = getActiveFgCall().getPhone();
        }
        if (heldCall != null) {
            heldPhone = heldCall.getPhone();
        }
        return heldPhone.getClass().equals(activePhone.getClass());
    }

    public boolean canConference(Call heldCall, int subId) {
        Phone activePhone = null;
        Phone heldPhone = null;
        if (hasActiveFgCall(subId)) {
            activePhone = getActiveFgCall(subId).getPhone();
        }
        if (heldCall != null) {
            heldPhone = heldCall.getPhone();
        }
        return heldPhone.getClass().equals(activePhone.getClass());
    }

    public void conference(Call heldCall) throws CallStateException {
        Phone fgPhone = getFgPhone(heldCall.getPhone().getSubId());
        if (fgPhone == null) {
            Rlog.d(LOG_TAG, "conference: fgPhone=null");
        } else if (fgPhone instanceof SipPhone) {
            ((SipPhone) fgPhone).conference(heldCall);
        } else if (canConference(heldCall)) {
            fgPhone.conference();
        } else {
            throw new CallStateException("Can't conference foreground and selected background call");
        }
    }

    public Connection dial(Phone phone, String dialString, int videoState) throws CallStateException {
        int subId = phone.getSubId();
        if (canDial(phone)) {
            if (hasActiveFgCall(subId)) {
                Phone activePhone = getActiveFgCall(subId).getPhone();
                boolean hasBgCall = activePhone.getBackgroundCall().isIdle() ^ 1;
                Rlog.d(LOG_TAG, "hasBgCall: " + hasBgCall + " sameChannel:" + (activePhone == phone));
                Phone imsPhone = phone.getImsPhone();
                if (activePhone != phone && (imsPhone == null || imsPhone != activePhone)) {
                    if (hasBgCall) {
                        Rlog.d(LOG_TAG, "Hangup");
                        getActiveFgCall(subId).hangup();
                    } else {
                        Rlog.d(LOG_TAG, "Switch");
                        activePhone.switchHoldingAndActive();
                    }
                }
            }
            return phone.dial(dialString, videoState);
        } else if (phone.handleInCallMmiCommands(PhoneNumberUtils.stripSeparators(dialString))) {
            return null;
        } else {
            throw new CallStateException("cannot dial in current state");
        }
    }

    public Connection dial(Phone phone, String dialString, UUSInfo uusInfo, int videoState) throws CallStateException {
        return phone.dial(dialString, uusInfo, videoState, null);
    }

    public void clearDisconnected() {
        for (Phone phone : this.mPhones) {
            phone.clearDisconnected();
        }
    }

    public void clearDisconnected(int subId) {
        for (Phone phone : this.mPhones) {
            if (phone.getSubId() == subId) {
                phone.clearDisconnected();
            }
        }
    }

    private boolean canDial(Phone phone) {
        int serviceState = phone.getServiceState().getState();
        int subId = phone.getSubId();
        boolean hasRingingCall = hasActiveRingingCall();
        Call.State fgCallState = getActiveFgCallState(subId);
        boolean result = (serviceState == 3 || (hasRingingCall ^ 1) == 0) ? false : (fgCallState == Call.State.ACTIVE || fgCallState == Call.State.IDLE || fgCallState == Call.State.DISCONNECTED) ? true : fgCallState == Call.State.ALERTING;
        if (!result) {
            Rlog.d(LOG_TAG, "canDial serviceState=" + serviceState + " hasRingingCall=" + hasRingingCall + " fgCallState=" + fgCallState);
        }
        return result;
    }

    public boolean canTransfer(Call heldCall) {
        Phone activePhone = null;
        Phone heldPhone = null;
        if (hasActiveFgCall()) {
            activePhone = getActiveFgCall().getPhone();
        }
        if (heldCall != null) {
            heldPhone = heldCall.getPhone();
        }
        return heldPhone == activePhone ? activePhone.canTransfer() : false;
    }

    public boolean canTransfer(Call heldCall, int subId) {
        Phone activePhone = null;
        Phone heldPhone = null;
        if (hasActiveFgCall(subId)) {
            activePhone = getActiveFgCall(subId).getPhone();
        }
        if (heldCall != null) {
            heldPhone = heldCall.getPhone();
        }
        return heldPhone == activePhone ? activePhone.canTransfer() : false;
    }

    public void explicitCallTransfer(Call heldCall) throws CallStateException {
        if (canTransfer(heldCall)) {
            heldCall.getPhone().explicitCallTransfer();
        }
    }

    public List<? extends MmiCode> getPendingMmiCodes(Phone phone) {
        Rlog.e(LOG_TAG, "getPendingMmiCodes not implemented");
        return null;
    }

    public boolean sendUssdResponse(Phone phone, String ussdMessge) {
        Rlog.e(LOG_TAG, "sendUssdResponse not implemented");
        return false;
    }

    public void setMute(boolean muted) {
        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().setMute(muted);
        }
    }

    public boolean getMute() {
        if (hasActiveFgCall()) {
            return getActiveFgCall().getPhone().getMute();
        }
        if (hasActiveBgCall()) {
            return getFirstActiveBgCall().getPhone().getMute();
        }
        return false;
    }

    public void setEchoSuppressionEnabled() {
        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().setEchoSuppressionEnabled();
        }
    }

    public boolean sendDtmf(char c) {
        if (!hasActiveFgCall()) {
            return false;
        }
        getActiveFgCall().getPhone().sendDtmf(c);
        return true;
    }

    public boolean startDtmf(char c) {
        if (!hasActiveFgCall()) {
            return false;
        }
        getActiveFgCall().getPhone().startDtmf(c);
        return true;
    }

    public void stopDtmf() {
        if (hasActiveFgCall()) {
            getFgPhone().stopDtmf();
        }
    }

    public boolean sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
        if (!hasActiveFgCall()) {
            return false;
        }
        getActiveFgCall().getPhone().sendBurstDtmf(dtmfString, on, off, onComplete);
        return true;
    }

    public void registerForDisconnect(Handler h, int what, Object obj) {
        this.mDisconnectRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForDisconnect(Handler h) {
        this.mDisconnectRegistrants.remove(h);
    }

    public void registerForPreciseCallStateChanged(Handler h, int what, Object obj) {
        this.mPreciseCallStateRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForPreciseCallStateChanged(Handler h) {
        this.mPreciseCallStateRegistrants.remove(h);
    }

    public void registerForUnknownConnection(Handler h, int what, Object obj) {
        this.mUnknownConnectionRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForUnknownConnection(Handler h) {
        this.mUnknownConnectionRegistrants.remove(h);
    }

    public void registerForNewRingingConnection(Handler h, int what, Object obj) {
        this.mNewRingingConnectionRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForNewRingingConnection(Handler h) {
        this.mNewRingingConnectionRegistrants.remove(h);
    }

    public void registerForIncomingRing(Handler h, int what, Object obj) {
        this.mIncomingRingRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForIncomingRing(Handler h) {
        this.mIncomingRingRegistrants.remove(h);
    }

    public void registerForRingbackTone(Handler h, int what, Object obj) {
        this.mRingbackToneRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForRingbackTone(Handler h) {
        this.mRingbackToneRegistrants.remove(h);
    }

    public void registerForOnHoldTone(Handler h, int what, Object obj) {
        this.mOnHoldToneRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForOnHoldTone(Handler h) {
        this.mOnHoldToneRegistrants.remove(h);
    }

    public void registerForResendIncallMute(Handler h, int what, Object obj) {
        this.mResendIncallMuteRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForResendIncallMute(Handler h) {
        this.mResendIncallMuteRegistrants.remove(h);
    }

    public void registerForMmiInitiate(Handler h, int what, Object obj) {
        this.mMmiInitiateRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForMmiInitiate(Handler h) {
        this.mMmiInitiateRegistrants.remove(h);
    }

    public void registerForMmiComplete(Handler h, int what, Object obj) {
        Rlog.d(LOG_TAG, "registerForMmiComplete");
        this.mMmiCompleteRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForMmiComplete(Handler h) {
        this.mMmiCompleteRegistrants.remove(h);
    }

    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
        this.mEcmTimerResetRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForEcmTimerReset(Handler h) {
        this.mEcmTimerResetRegistrants.remove(h);
    }

    public void registerForServiceStateChanged(Handler h, int what, Object obj) {
        this.mServiceStateChangedRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForServiceStateChanged(Handler h) {
        this.mServiceStateChangedRegistrants.remove(h);
    }

    public void registerForSuppServiceFailed(Handler h, int what, Object obj) {
        this.mSuppServiceFailedRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSuppServiceFailed(Handler h) {
        this.mSuppServiceFailedRegistrants.remove(h);
    }

    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj) {
        this.mInCallVoicePrivacyOnRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForInCallVoicePrivacyOn(Handler h) {
        this.mInCallVoicePrivacyOnRegistrants.remove(h);
    }

    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj) {
        this.mInCallVoicePrivacyOffRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForInCallVoicePrivacyOff(Handler h) {
        this.mInCallVoicePrivacyOffRegistrants.remove(h);
    }

    public void registerForCallWaiting(Handler h, int what, Object obj) {
        this.mCallWaitingRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForCallWaiting(Handler h) {
        this.mCallWaitingRegistrants.remove(h);
    }

    public void registerForSignalInfo(Handler h, int what, Object obj) {
        this.mSignalInfoRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSignalInfo(Handler h) {
        this.mSignalInfoRegistrants.remove(h);
    }

    public void registerForDisplayInfo(Handler h, int what, Object obj) {
        this.mDisplayInfoRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForDisplayInfo(Handler h) {
        this.mDisplayInfoRegistrants.remove(h);
    }

    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj) {
        this.mCdmaOtaStatusChangeRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForCdmaOtaStatusChange(Handler h) {
        this.mCdmaOtaStatusChangeRegistrants.remove(h);
    }

    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        this.mSubscriptionInfoReadyRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSubscriptionInfoReady(Handler h) {
        this.mSubscriptionInfoReadyRegistrants.remove(h);
    }

    public void registerForPostDialCharacter(Handler h, int what, Object obj) {
        this.mPostDialCharacterRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForPostDialCharacter(Handler h) {
        this.mPostDialCharacterRegistrants.remove(h);
    }

    public void registerForTtyModeReceived(Handler h, int what, Object obj) {
        this.mTtyModeReceivedRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForTtyModeReceived(Handler h) {
        this.mTtyModeReceivedRegistrants.remove(h);
    }

    public List<Call> getRingingCalls() {
        return Collections.unmodifiableList(this.mRingingCalls);
    }

    public List<Call> getForegroundCalls() {
        return Collections.unmodifiableList(this.mForegroundCalls);
    }

    public List<Call> getBackgroundCalls() {
        return Collections.unmodifiableList(this.mBackgroundCalls);
    }

    public boolean hasActiveFgCall() {
        return getFirstActiveCall(this.mForegroundCalls) != null;
    }

    public boolean hasActiveFgCall(int subId) {
        return getFirstActiveCall(this.mForegroundCalls, subId) != null;
    }

    public boolean hasActiveBgCall() {
        return getFirstActiveCall(this.mBackgroundCalls) != null;
    }

    public boolean hasActiveBgCall(int subId) {
        return getFirstActiveCall(this.mBackgroundCalls, subId) != null;
    }

    public boolean hasActiveRingingCall() {
        return getFirstActiveCall(this.mRingingCalls) != null;
    }

    public boolean hasActiveRingingCall(int subId) {
        return getFirstActiveCall(this.mRingingCalls, subId) != null;
    }

    public Call getActiveFgCall() {
        Call call = getFirstNonIdleCall(this.mForegroundCalls);
        if (call != null) {
            return call;
        }
        if (this.mDefaultPhone == null) {
            return null;
        }
        return this.mDefaultPhone.getForegroundCall();
    }

    public Call getActiveFgCall(int subId) {
        Call call = getFirstNonIdleCall(this.mForegroundCalls, subId);
        if (call != null) {
            return call;
        }
        Phone phone = getPhone(subId);
        if (phone == null) {
            return null;
        }
        return phone.getForegroundCall();
    }

    private Call getFirstNonIdleCall(List<Call> calls) {
        Call result = null;
        for (Call call : calls) {
            if (!call.isIdle()) {
                return call;
            }
            if (call.getState() != Call.State.IDLE && result == null) {
                result = call;
            }
        }
        return result;
    }

    private Call getFirstNonIdleCall(List<Call> calls, int subId) {
        Call result = null;
        for (Call call : calls) {
            if (call.getPhone().getSubId() == subId || (call.getPhone() instanceof SipPhone)) {
                if (!call.isIdle()) {
                    return call;
                }
                if (call.getState() != Call.State.IDLE && result == null) {
                    result = call;
                }
            }
        }
        return result;
    }

    public Call getFirstActiveBgCall() {
        Call call = getFirstNonIdleCall(this.mBackgroundCalls);
        if (call != null) {
            return call;
        }
        if (this.mDefaultPhone == null) {
            return null;
        }
        return this.mDefaultPhone.getBackgroundCall();
    }

    public Call getFirstActiveBgCall(int subId) {
        Phone phone = getPhone(subId);
        if (hasMoreThanOneHoldingCall(subId)) {
            return phone.getBackgroundCall();
        }
        Call call = getFirstNonIdleCall(this.mBackgroundCalls, subId);
        if (call == null) {
            if (phone == null) {
                call = null;
            } else {
                call = phone.getBackgroundCall();
            }
        }
        return call;
    }

    public Call getFirstActiveRingingCall() {
        Call call = getFirstNonIdleCall(this.mRingingCalls);
        if (call != null) {
            return call;
        }
        if (this.mDefaultPhone == null) {
            return null;
        }
        return this.mDefaultPhone.getRingingCall();
    }

    public Call getFirstActiveRingingCall(int subId) {
        Phone phone = getPhone(subId);
        Call call = getFirstNonIdleCall(this.mRingingCalls, subId);
        if (call != null) {
            return call;
        }
        if (phone == null) {
            return null;
        }
        return phone.getRingingCall();
    }

    public Call.State getActiveFgCallState() {
        Call fgCall = getActiveFgCall();
        if (fgCall != null) {
            return fgCall.getState();
        }
        return Call.State.IDLE;
    }

    public Call.State getActiveFgCallState(int subId) {
        Call fgCall = getActiveFgCall(subId);
        if (fgCall != null) {
            return fgCall.getState();
        }
        return Call.State.IDLE;
    }

    public List<Connection> getFgCallConnections() {
        Call fgCall = getActiveFgCall();
        if (fgCall != null) {
            return fgCall.getConnections();
        }
        return this.mEmptyConnections;
    }

    public List<Connection> getFgCallConnections(int subId) {
        Call fgCall = getActiveFgCall(subId);
        if (fgCall != null) {
            return fgCall.getConnections();
        }
        return this.mEmptyConnections;
    }

    public List<Connection> getBgCallConnections() {
        Call bgCall = getFirstActiveBgCall();
        if (bgCall != null) {
            return bgCall.getConnections();
        }
        return this.mEmptyConnections;
    }

    public List<Connection> getBgCallConnections(int subId) {
        Call bgCall = getFirstActiveBgCall(subId);
        if (bgCall != null) {
            return bgCall.getConnections();
        }
        return this.mEmptyConnections;
    }

    public Connection getFgCallLatestConnection() {
        Call fgCall = getActiveFgCall();
        if (fgCall != null) {
            return fgCall.getLatestConnection();
        }
        return null;
    }

    public Connection getFgCallLatestConnection(int subId) {
        Call fgCall = getActiveFgCall(subId);
        if (fgCall != null) {
            return fgCall.getLatestConnection();
        }
        return null;
    }

    public boolean hasDisconnectedFgCall() {
        return getFirstCallOfState(this.mForegroundCalls, Call.State.DISCONNECTED) != null;
    }

    public boolean hasDisconnectedFgCall(int subId) {
        return getFirstCallOfState(this.mForegroundCalls, Call.State.DISCONNECTED, subId) != null;
    }

    public boolean hasDisconnectedBgCall() {
        return getFirstCallOfState(this.mBackgroundCalls, Call.State.DISCONNECTED) != null;
    }

    public boolean hasDisconnectedBgCall(int subId) {
        return getFirstCallOfState(this.mBackgroundCalls, Call.State.DISCONNECTED, subId) != null;
    }

    private Call getFirstActiveCall(ArrayList<Call> calls) {
        for (Call call : calls) {
            if (!call.isIdle()) {
                return call;
            }
        }
        return null;
    }

    private Call getFirstActiveCall(ArrayList<Call> calls, int subId) {
        for (Call call : calls) {
            if (!call.isIdle() && (call.getPhone().getSubId() == subId || (call.getPhone() instanceof SipPhone))) {
                return call;
            }
        }
        return null;
    }

    private Call getFirstCallOfState(ArrayList<Call> calls, Call.State state) {
        for (Call call : calls) {
            if (call.getState() == state) {
                return call;
            }
        }
        return null;
    }

    private Call getFirstCallOfState(ArrayList<Call> calls, Call.State state, int subId) {
        for (Call call : calls) {
            if (call.getState() == state || call.getPhone().getSubId() == subId) {
                return call;
            }
            if (call.getPhone() instanceof SipPhone) {
                return call;
            }
        }
        return null;
    }

    private boolean hasMoreThanOneRingingCall() {
        int count = 0;
        for (Call call : this.mRingingCalls) {
            if (call.getState().isRinging()) {
                count++;
                if (count > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasMoreThanOneRingingCall(int subId) {
        int count = 0;
        for (Call call : this.mRingingCalls) {
            if (call.getState().isRinging() && (call.getPhone().getSubId() == subId || (call.getPhone() instanceof SipPhone))) {
                count++;
                if (count > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasMoreThanOneHoldingCall(int subId) {
        int count = 0;
        for (Call call : this.mBackgroundCalls) {
            if (call.getState() == Call.State.HOLDING && (call.getPhone().getSubId() == subId || (call.getPhone() instanceof SipPhone))) {
                count++;
                if (count > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public String toString() {
        Call call;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            b.append("CallManager {");
            b.append("\nstate = ").append(getState(i));
            call = getActiveFgCall(i);
            if (call != null) {
                b.append("\n- Foreground: ").append(getActiveFgCallState(i));
                b.append(" from ").append(call.getPhone());
                b.append("\n  Conn: ").append(getFgCallConnections(i));
            }
            call = getFirstActiveBgCall(i);
            if (call != null) {
                b.append("\n- Background: ").append(call.getState());
                b.append(" from ").append(call.getPhone());
                b.append("\n  Conn: ").append(getBgCallConnections(i));
            }
            call = getFirstActiveRingingCall(i);
            if (call != null) {
                b.append("\n- Ringing: ").append(call.getState());
                b.append(" from ").append(call.getPhone());
            }
        }
        for (Phone phone : getAllPhones()) {
            if (phone != null) {
                b.append("\nPhone: ").append(phone).append(", name = ").append(phone.getPhoneName()).append(", state = ").append(phone.getState());
                call = phone.getForegroundCall();
                if (call != null) {
                    b.append("\n- Foreground: ").append(call);
                }
                call = phone.getBackgroundCall();
                if (call != null) {
                    b.append(" Background: ").append(call);
                }
                call = phone.getRingingCall();
                if (call != null) {
                    b.append(" Ringing: ").append(call);
                }
            }
        }
        b.append("\n}");
        return b.toString();
    }

    private void writeCallRecord(Connection c) {
        String[] log_array;
        String address = c.getAddress();
        long callDuration = c.getDurationMillis() / 1000;
        boolean isIncoming = c.isIncoming();
        String createDate = getCurrentDateStr();
        if (callDuration <= 0 || callDuration >= 60) {
            callDuration /= 60;
        } else {
            callDuration = 1;
        }
        if (isIncoming) {
            try {
                this.mOppoUsageManager.accumulateInComingCallDuration((int) callDuration);
            } catch (Exception e) {
            }
        } else {
            this.mOppoUsageManager.accumulateDialOutDuration((int) callDuration);
        }
        int log_type = -1;
        String log_desc = SpnOverride.MVNO_TYPE_NONE;
        try {
            log_array = getContext().getString(getContext().getResources().getIdentifier("zz_oppo_critical_log_" + (c.getPreState() == Call.State.DIALING ? 10 : 21), "string", "android")).split(",");
            log_type = Integer.valueOf(log_array[0]).intValue();
            log_desc = log_array[1];
        } catch (Exception e2) {
        }
        String loc = null;
        if (!(c.getCall() == null || c.getCall().getPhone() == null)) {
            String mccMnc = SpnOverride.MVNO_TYPE_NONE;
            int phoneId = c.getCall().getPhone().getPhoneId();
            String prop = SystemProperties.get("gsm.operator.numeric", SpnOverride.MVNO_TYPE_NONE);
            if (prop != null && prop.length() > 0) {
                String[] values = prop.split(",");
                if (phoneId >= 0 && phoneId < values.length && values[phoneId] != null) {
                    mccMnc = values[phoneId];
                }
            }
            int mcc = 0;
            int mnc = 0;
            if (mccMnc != null) {
                try {
                    if (mccMnc.length() >= 3) {
                        mcc = Integer.parseInt(mccMnc.substring(0, 3));
                        mnc = Integer.parseInt(mccMnc.substring(3));
                    }
                } catch (Exception e3) {
                    Rlog.d(LOG_TAG, "couldn't parse mcc/mnc: " + mccMnc);
                }
            }
            if (mcc == 460) {
                if (mnc == 2 || mnc == 7 || mnc == 8) {
                    mnc = 0;
                }
                if (mnc == 6 || mnc == 9) {
                    mnc = 1;
                }
                if (mnc == 3) {
                    mnc = 11;
                }
            }
            loc = "MCC:" + mcc + ", MNC:" + mnc;
            CellLocation cell = c.getCall().getPhone().getCellLocation();
            if (cell instanceof GsmCellLocation) {
                loc = loc + ", LAC:" + ((GsmCellLocation) cell).getLac() + ", CID:" + ((GsmCellLocation) cell).getCid();
            } else if (cell instanceof CdmaCellLocation) {
                loc = loc + ", SID:" + ((CdmaCellLocation) cell).getSystemId() + ", NID:" + ((CdmaCellLocation) cell).getNetworkId() + ", BID:" + ((CdmaCellLocation) cell).getBaseStationId();
            }
            SignalStrength signal = c.getCall().getPhone().getSignalStrength();
            if (signal != null) {
                loc = loc + ", signalstrength:" + signal.getDbm() + ", signallevel:" + signal.getLevel();
            }
        }
        int cause = c.getDisconnectCause();
        boolean imscall = c instanceof ImsPhoneConnection;
        if ((cause == 36 && (imscall ^ 1) != 0) || cause == 5 || cause == 7 || cause == 18 || cause == 69 || cause == 27 || ((cause == 12 && imscall) || (cause == 9 && imscall))) {
            if (c.getPreState() == Call.State.DIALING) {
                OppoManager.writeLogToPartition(log_type, loc + ", mo drop cause:" + cause + ", imscall:" + imscall, "NETWORK", RIL.ISSUE_SYS_OEM_NW_DIAG_CAUSE_MO_DROP, log_desc);
                OppoModemLogManager.saveModemLogPostBack(getContext(), SpnOverride.MVNO_TYPE_NONE + log_type, loc + ", mo drop cause:" + cause + ", imscall:" + imscall);
            } else {
                OppoManager.writeLogToPartition(log_type, loc + ", call drop cause:" + cause + ", imscall:" + imscall, "NETWORK", RIL.ISSUE_SYS_OEM_NW_DIAG_CAUSE_CALL_DROP, log_desc);
            }
        }
        String addressInfo = dealWithAddress(address, isIncoming);
        if (addressInfo == null || addressInfo.length() <= 0) {
            if (isIncoming) {
                log_array = getContext().getString(getContext().getResources().getIdentifier("zz_oppo_critical_log_18", "string", "android")).split(",");
                OppoManager.writeLogToPartition(Integer.valueOf(log_array[0]).intValue(), loc + ", incoming call number unknown", "NETWORK", RIL.ISSUE_SYS_OEM_NW_CALL_NUMBER_UNKNOWN, log_array[1]);
            }
            return;
        }
        this.mOppoUsageManager.writePhoneCallHistoryRecord(addressInfo, createDate);
    }

    private String dealWithAddress(String addr, boolean isIncoming) {
        if (addr == null || addr.length() <= 0) {
            return null;
        }
        StringBuilder strBuilder = new StringBuilder();
        String prefix = isIncoming ? "in :" : "out:";
        int length = addr.length();
        if (length <= 6) {
            strBuilder.append(prefix).append(addr);
        } else {
            int remainCharNum = length - 4;
            if (remainCharNum <= 0) {
                strBuilder.append(prefix).append(addr);
            } else {
                int halfOfRemain = remainCharNum / 2;
                int firstPartNum = halfOfRemain;
                int lastPartNum = remainCharNum - halfOfRemain;
                strBuilder.append(prefix);
                if (halfOfRemain > 0) {
                    strBuilder.append(addr.substring(0, halfOfRemain));
                }
                for (int i = 0; i < 4; i++) {
                    strBuilder.append('*');
                }
                if (lastPartNum > 0 && lastPartNum < length) {
                    strBuilder.append(addr.substring(halfOfRemain + 4, length));
                }
            }
        }
        return strBuilder.toString();
    }

    private String getCurrentDateStr() {
        Time timeObj = new Time();
        timeObj.set(System.currentTimeMillis());
        return timeObj.format("%Y-%m-%d %H:%M:%S");
    }
}
