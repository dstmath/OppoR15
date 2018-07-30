package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.OemConstant;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.gsm.NetworkInfoWithAcT;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class IccRecords extends Handler implements IccConstants {
    public static final int CALL_FORWARDING_STATUS_DISABLED = 0;
    public static final int CALL_FORWARDING_STATUS_ENABLED = 1;
    public static final int CALL_FORWARDING_STATUS_UNKNOWN = -1;
    protected static final boolean DBG = true;
    public static final int DEFAULT_VOICE_MESSAGE_COUNT = -2;
    private static final int EVENT_AKA_AUTHENTICATE_DONE = 90;
    protected static final int EVENT_APP_READY = 1;
    public static final int EVENT_CFI = 1;
    public static final int EVENT_GET_ICC_RECORD_DONE = 100;
    protected static final int EVENT_GET_SMS_RECORD_SIZE_DONE = 28;
    public static final int EVENT_MWI = 0;
    public static final int EVENT_REFRESH = 31;
    public static final int EVENT_SPN = 2;
    protected static final int HANDLER_ACTION_BASE = 1238272;
    protected static final int HANDLER_ACTION_NONE = 1238272;
    protected static final int HANDLER_ACTION_SEND_RESPONSE = 1238273;
    public static final int SPN_RULE_SHOW_PLMN = 2;
    public static final int SPN_RULE_SHOW_SPN = 1;
    protected static final int UNINITIALIZED = -1;
    protected static final int UNKNOWN = 0;
    public static final int UNKNOWN_VOICE_MESSAGE_COUNT = -1;
    protected static final boolean VDBG = false;
    protected static AtomicInteger sNextRequestId = new AtomicInteger(1);
    private IccIoResult auth_rsp;
    protected AdnRecordCache mAdnCache;
    CarrierTestOverride mCarrierTestOverride;
    protected CommandsInterface mCi;
    protected Context mContext;
    protected AtomicBoolean mDestroyed = new AtomicBoolean(false);
    protected String[] mEhplmns;
    protected String mFakeGid1;
    protected String mFakeGid2;
    protected String mFakeImsi;
    private String mFakeSpn;
    protected IccFileHandler mFh;
    protected String[] mFplmns;
    protected String mFullIccId;
    protected String mGid1;
    protected String mGid2;
    protected PlmnActRecord[] mHplmnActRecords;
    protected String mIccId;
    protected String mImsi;
    protected RegistrantList mImsiReadyRegistrants = new RegistrantList();
    protected boolean mIsTestCard = false;
    protected boolean mIsVoiceMailFixed = false;
    private final Object mLock = new Object();
    protected int mMailboxIndex = 0;
    protected int mMncLength = -1;
    protected String mMsisdn = null;
    protected String mMsisdnTag = null;
    protected RegistrantList mNetworkSelectionModeAutomaticRegistrants = new RegistrantList();
    protected String mNewMsisdn = null;
    protected String mNewMsisdnTag = null;
    protected RegistrantList mNewSmsRegistrants = new RegistrantList();
    protected String mNewVoiceMailNum = null;
    protected String mNewVoiceMailTag = null;
    protected PlmnActRecord[] mOplmnActRecords;
    protected UiccCardApplication mParentApp;
    protected final HashMap<Integer, Message> mPendingResponses = new HashMap();
    protected PlmnActRecord[] mPlmnActRecords;
    protected String mPrefLang;
    protected RegistrantList mRecordsEventsRegistrants = new RegistrantList();
    protected RegistrantList mRecordsLoadedRegistrants = new RegistrantList();
    protected boolean mRecordsRequested = false;
    protected int mRecordsToLoad;
    protected int mSmsCountOnIcc = -1;
    protected String mSpn;
    protected TelephonyManager mTelephonyManager;
    protected String mVoiceMailNum = null;
    protected String mVoiceMailTag = null;

    public interface IccRecordLoaded {
        String getEfName();

        void onRecordLoaded(AsyncResult asyncResult);
    }

    public abstract int getDisplayRule(String str);

    public abstract int getVoiceMessageCount();

    protected abstract void handleFileUpdate(int i);

    protected abstract void log(String str);

    protected abstract void loge(String str);

    protected abstract void onAllRecordsLoaded();

    public abstract void onReady();

    protected abstract void onRecordLoaded();

    public abstract void onRefresh(boolean z, int[] iArr);

    public abstract void setVoiceMailNumber(String str, String str2, Message message);

    public abstract void setVoiceMessageWaiting(int i, int i2);

    public String toString() {
        String str;
        StringBuilder append;
        StringBuilder append2 = new StringBuilder().append("mDestroyed=").append(this.mDestroyed).append(" mContext=").append(this.mContext).append(" mCi=").append(this.mCi).append(" mFh=").append(this.mFh).append(" mParentApp=").append(this.mParentApp).append(" recordsLoadedRegistrants=").append(this.mRecordsLoadedRegistrants).append(" mImsiReadyRegistrants=").append(this.mImsiReadyRegistrants).append(" mRecordsEventsRegistrants=").append(this.mRecordsEventsRegistrants).append(" mNewSmsRegistrants=").append(this.mNewSmsRegistrants).append(" mNetworkSelectionModeAutomaticRegistrants=").append(this.mNetworkSelectionModeAutomaticRegistrants).append(" recordsToLoad=").append(this.mRecordsToLoad).append(" adnCache=").append(this.mAdnCache).append(" recordsRequested=").append(this.mRecordsRequested).append(" iccid=").append(SubscriptionInfo.givePrintableIccid(this.mFullIccId)).append(" msisdnTag=").append(this.mMsisdnTag).append(" voiceMailNum=").append(Rlog.pii(false, this.mVoiceMailNum)).append(" voiceMailTag=").append(this.mVoiceMailTag).append(" voiceMailNum=").append(Rlog.pii(false, this.mNewVoiceMailNum)).append(" newVoiceMailTag=").append(this.mNewVoiceMailTag).append(" isVoiceMailFixed=").append(this.mIsVoiceMailFixed).append(" mImsi=");
        if (this.mImsi != null) {
            str = this.mImsi.substring(0, 6) + Rlog.pii(false, this.mImsi.substring(6));
        } else {
            str = "null";
        }
        append2 = append2.append(str);
        if (this.mCarrierTestOverride.isInTestMode()) {
            append = new StringBuilder().append(" mFakeImsi=");
            if (this.mFakeImsi != null) {
                str = this.mFakeImsi;
            } else {
                str = "null";
            }
            str = append.append(str).toString();
        } else {
            str = SpnOverride.MVNO_TYPE_NONE;
        }
        append2 = append2.append(str).append(" mncLength=").append(this.mMncLength).append(" mailboxIndex=").append(this.mMailboxIndex).append(" spn=").append(this.mSpn);
        if (this.mCarrierTestOverride.isInTestMode()) {
            append = new StringBuilder().append(" mFakeSpn=");
            if (this.mFakeSpn != null) {
                str = this.mFakeSpn;
            } else {
                str = "null";
            }
            str = append.append(str).toString();
        } else {
            str = SpnOverride.MVNO_TYPE_NONE;
        }
        return append2.append(str).toString();
    }

    public IccRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        this.mContext = c;
        this.mCi = ci;
        this.mFh = app.getIccFileHandler();
        this.mParentApp = app;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mCarrierTestOverride = new CarrierTestOverride();
        if (this.mCarrierTestOverride.isInTestMode()) {
            this.mFakeImsi = this.mCarrierTestOverride.getFakeIMSI();
            log("load mFakeImsi: " + this.mFakeImsi);
            this.mFakeGid1 = this.mCarrierTestOverride.getFakeGid1();
            log("load mFakeGid1: " + this.mFakeGid1);
            this.mFakeGid2 = this.mCarrierTestOverride.getFakeGid2();
            log("load mFakeGid2: " + this.mFakeGid2);
            this.mFakeSpn = this.mCarrierTestOverride.getFakeSpn();
            log("load mFakeSpn: " + this.mFakeSpn);
        }
    }

    public void dispose() {
        this.mDestroyed.set(true);
        this.auth_rsp = null;
        synchronized (this.mLock) {
            this.mLock.notifyAll();
        }
        this.mCi.unregisterForIccRefresh(this);
        this.mParentApp = null;
        this.mFh = null;
        this.mCi = null;
        this.mContext = null;
        if (this.mAdnCache != null) {
            this.mAdnCache.reset();
        }
    }

    public AdnRecordCache getAdnCache() {
        return this.mAdnCache;
    }

    public int storePendingResponseMessage(Message msg) {
        int key = sNextRequestId.getAndIncrement();
        synchronized (this.mPendingResponses) {
            this.mPendingResponses.put(Integer.valueOf(key), msg);
        }
        return key;
    }

    public Message retrievePendingResponseMessage(Integer key) {
        Message message;
        synchronized (this.mPendingResponses) {
            message = (Message) this.mPendingResponses.remove(key);
        }
        return message;
    }

    public String getIccId() {
        return this.mIccId;
    }

    public String getFullIccId() {
        return this.mFullIccId;
    }

    public void registerForRecordsLoaded(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            Registrant r = new Registrant(h, what, obj);
            this.mRecordsLoadedRegistrants.add(r);
            if (this.mRecordsToLoad == 0 && this.mRecordsRequested) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void unregisterForRecordsLoaded(Handler h) {
        this.mRecordsLoadedRegistrants.remove(h);
    }

    public void registerForImsiReady(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            Registrant r = new Registrant(h, what, obj);
            this.mImsiReadyRegistrants.add(r);
            if (getIMSI() != null) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void unregisterForImsiReady(Handler h) {
        this.mImsiReadyRegistrants.remove(h);
    }

    public void registerForRecordsEvents(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mRecordsEventsRegistrants.add(r);
        r.notifyResult(Integer.valueOf(0));
    }

    public void unregisterForRecordsEvents(Handler h) {
        this.mRecordsEventsRegistrants.remove(h);
    }

    public void registerForNewSms(Handler h, int what, Object obj) {
        this.mNewSmsRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForNewSms(Handler h) {
        this.mNewSmsRegistrants.remove(h);
    }

    public void registerForNetworkSelectionModeAutomatic(Handler h, int what, Object obj) {
        this.mNetworkSelectionModeAutomaticRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForNetworkSelectionModeAutomatic(Handler h) {
        this.mNetworkSelectionModeAutomaticRegistrants.remove(h);
    }

    public String getIMSI() {
        if (!this.mCarrierTestOverride.isInTestMode() || this.mFakeImsi == null) {
            return this.mImsi;
        }
        return this.mFakeImsi;
    }

    public void setImsi(String imsi) {
        this.mImsi = imsi;
        this.mImsiReadyRegistrants.notifyRegistrants();
    }

    public String getNAI() {
        return null;
    }

    public String getMsisdnNumber() {
        return this.mMsisdn;
    }

    public String getGid1() {
        if (!this.mCarrierTestOverride.isInTestMode() || this.mFakeGid1 == null) {
            return this.mGid1;
        }
        return this.mFakeGid1;
    }

    public String getGid2() {
        if (!this.mCarrierTestOverride.isInTestMode() || this.mFakeGid2 == null) {
            return this.mGid2;
        }
        return this.mFakeGid2;
    }

    public void setMsisdnNumber(String alphaTag, String number, Message onComplete) {
        loge("setMsisdn() should not be invoked on base IccRecords");
        AsyncResult.forMessage(onComplete).exception = new IccIoResult(106, 130, (byte[]) null).getException();
        onComplete.sendToTarget();
    }

    public String getMsisdnAlphaTag() {
        return this.mMsisdnTag;
    }

    public String getVoiceMailNumber() {
        return this.mVoiceMailNum;
    }

    public String getServiceProviderName() {
        if (this.mCarrierTestOverride.isInTestMode() && this.mFakeSpn != null) {
            return this.mFakeSpn;
        }
        String providerName = this.mSpn;
        UiccCardApplication parentApp = this.mParentApp;
        if (parentApp != null) {
            UiccCard card = parentApp.getUiccCard();
            if (card != null) {
                String brandOverride = card.getOperatorBrandOverride();
                if (brandOverride != null) {
                    log("getServiceProviderName: override, providerName=" + providerName);
                    providerName = brandOverride;
                } else {
                    log("getServiceProviderName: no brandOverride, providerName=" + providerName);
                }
            } else {
                log("getServiceProviderName: card is null, providerName=" + providerName);
            }
        } else {
            log("getServiceProviderName: mParentApp is null, providerName=" + providerName);
        }
        return providerName;
    }

    protected void setServiceProviderName(String spn) {
        this.mSpn = spn;
    }

    public String getVoiceMailAlphaTag() {
        return this.mVoiceMailTag;
    }

    public boolean getRecordsLoaded() {
        if (this.mRecordsToLoad == 0 && this.mRecordsRequested) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        AsyncResult ar;
        switch (msg.what) {
            case 28:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    loge("Exception in EVENT_GET_SMS_RECORD_SIZE_DONE " + ar.exception);
                    return;
                }
                int[] recordSize = ar.result;
                try {
                    this.mSmsCountOnIcc = recordSize[2];
                    log("EVENT_GET_SMS_RECORD_SIZE_DONE Size " + recordSize[0] + " total " + recordSize[1] + " record " + recordSize[2]);
                    return;
                } catch (ArrayIndexOutOfBoundsException exc) {
                    loge("ArrayIndexOutOfBoundsException in EVENT_GET_SMS_RECORD_SIZE_DONE: " + exc.toString());
                    return;
                }
            case 31:
                ar = (AsyncResult) msg.obj;
                log("Card REFRESH occurred: ");
                if (ar.exception == null) {
                    handleRefresh((IccRefreshResponse) ar.result);
                    return;
                } else {
                    loge("Icc refresh Exception: " + ar.exception);
                    return;
                }
            case EVENT_AKA_AUTHENTICATE_DONE /*90*/:
                ar = (AsyncResult) msg.obj;
                this.auth_rsp = null;
                log("EVENT_AKA_AUTHENTICATE_DONE");
                if (ar.exception != null) {
                    loge("Exception ICC SIM AKA: " + ar.exception);
                } else {
                    try {
                        this.auth_rsp = (IccIoResult) ar.result;
                        log("ICC SIM AKA: auth_rsp = " + this.auth_rsp);
                    } catch (Exception e) {
                        loge("Failed to parse ICC SIM AKA contents: " + e);
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notifyAll();
                }
                return;
            case 100:
                try {
                    ar = msg.obj;
                    IccRecordLoaded recordLoaded = ar.userObj;
                    log(recordLoaded.getEfName() + " LOADED");
                    if (ar.exception != null) {
                        loge("Record Load Exception: " + ar.exception);
                    } else {
                        recordLoaded.onRecordLoaded(ar);
                    }
                    onRecordLoaded();
                    return;
                } catch (RuntimeException exc2) {
                    loge("Exception parsing SIM record: " + exc2);
                    return;
                } catch (Throwable th) {
                    onRecordLoaded();
                }
            default:
                super.handleMessage(msg);
                return;
        }
    }

    public String getSimLanguage() {
        return this.mPrefLang;
    }

    protected void setSimLanguage(byte[] efLi, byte[] efPl) {
        String[] locales = this.mContext.getAssets().getLocales();
        try {
            this.mPrefLang = findBestLanguage(efLi, locales);
        } catch (UnsupportedEncodingException e) {
            log("Unable to parse EF-LI: " + Arrays.toString(efLi));
        }
        if (this.mPrefLang == null) {
            try {
                this.mPrefLang = findBestLanguage(efPl, locales);
            } catch (UnsupportedEncodingException e2) {
                log("Unable to parse EF-PL: " + Arrays.toString(efLi));
            }
        }
    }

    protected static String findBestLanguage(byte[] languages, String[] locales) throws UnsupportedEncodingException {
        if (languages == null || locales == null) {
            return null;
        }
        for (int i = 0; i + 1 < languages.length; i += 2) {
            String lang = new String(languages, i, 2, "ISO-8859-1");
            int j = 0;
            while (j < locales.length) {
                if (locales[j] != null && locales[j].length() >= 2 && locales[j].substring(0, 2).equalsIgnoreCase(lang)) {
                    return lang;
                }
                j++;
            }
        }
        return null;
    }

    protected void handleRefresh(IccRefreshResponse refreshResponse) {
        if (refreshResponse == null) {
            log("handleRefresh received without input");
        } else if (refreshResponse.aid == null || (refreshResponse.aid.equals(this.mParentApp.getAid()) ^ 1) == 0) {
            switch (refreshResponse.refreshResult) {
                case 0:
                    log("handleRefresh with SIM_FILE_UPDATED");
                    handleFileUpdate(refreshResponse.efId);
                    break;
                default:
                    log("handleRefresh with unknown operation");
                    break;
            }
        }
    }

    public boolean isCspPlmnEnabled() {
        return false;
    }

    public String getOperatorNumeric() {
        return null;
    }

    public int getVoiceCallForwardingFlag() {
        return -1;
    }

    public void setVoiceCallForwardingFlag(int line, boolean enable, String number) {
    }

    public boolean isProvisioned() {
        return true;
    }

    public IsimRecords getIsimRecords() {
        return null;
    }

    public UsimServiceTable getUsimServiceTable() {
        return null;
    }

    protected void setSystemProperty(String key, String val) {
        TelephonyManager.getDefault();
        TelephonyManager.setTelephonyProperty(this.mParentApp.getPhoneId(), key, val);
        log("[key, value]=" + key + ", " + val);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getIccSimChallengeResponse(int authContext, String data) {
        log("getIccSimChallengeResponse:");
        try {
            synchronized (this.mLock) {
                CommandsInterface ci = this.mCi;
                UiccCardApplication parentApp = this.mParentApp;
                if (ci == null || parentApp == null) {
                    loge("getIccSimChallengeResponse: Fail, ci or parentApp is null");
                    return null;
                }
                ci.requestIccSimAuthentication(authContext, data, parentApp.getAid(), obtainMessage(EVENT_AKA_AUTHENTICATE_DONE));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    loge("getIccSimChallengeResponse: Fail, interrupted while trying to request Icc Sim Auth");
                    return null;
                }
            }
        } catch (Exception e2) {
            loge("getIccSimChallengeResponse: Fail while trying to request Icc Sim Auth");
            return null;
        }
    }

    public int getSmsCapacityOnIcc() {
        log("getSmsCapacityOnIcc: " + this.mSmsCountOnIcc);
        return this.mSmsCountOnIcc;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int i;
        pw.println("IccRecords: " + this);
        pw.println(" mDestroyed=" + this.mDestroyed);
        pw.println(" mCi=" + this.mCi);
        pw.println(" mFh=" + this.mFh);
        pw.println(" mParentApp=" + this.mParentApp);
        pw.println(" recordsLoadedRegistrants: size=" + this.mRecordsLoadedRegistrants.size());
        for (i = 0; i < this.mRecordsLoadedRegistrants.size(); i++) {
            pw.println("  recordsLoadedRegistrants[" + i + "]=" + ((Registrant) this.mRecordsLoadedRegistrants.get(i)).getHandler());
        }
        pw.println(" mImsiReadyRegistrants: size=" + this.mImsiReadyRegistrants.size());
        for (i = 0; i < this.mImsiReadyRegistrants.size(); i++) {
            pw.println("  mImsiReadyRegistrants[" + i + "]=" + ((Registrant) this.mImsiReadyRegistrants.get(i)).getHandler());
        }
        pw.println(" mRecordsEventsRegistrants: size=" + this.mRecordsEventsRegistrants.size());
        for (i = 0; i < this.mRecordsEventsRegistrants.size(); i++) {
            pw.println("  mRecordsEventsRegistrants[" + i + "]=" + ((Registrant) this.mRecordsEventsRegistrants.get(i)).getHandler());
        }
        pw.println(" mNewSmsRegistrants: size=" + this.mNewSmsRegistrants.size());
        for (i = 0; i < this.mNewSmsRegistrants.size(); i++) {
            pw.println("  mNewSmsRegistrants[" + i + "]=" + ((Registrant) this.mNewSmsRegistrants.get(i)).getHandler());
        }
        pw.println(" mNetworkSelectionModeAutomaticRegistrants: size=" + this.mNetworkSelectionModeAutomaticRegistrants.size());
        for (i = 0; i < this.mNetworkSelectionModeAutomaticRegistrants.size(); i++) {
            pw.println("  mNetworkSelectionModeAutomaticRegistrants[" + i + "]=" + ((Registrant) this.mNetworkSelectionModeAutomaticRegistrants.get(i)).getHandler());
        }
        pw.println(" mRecordsRequested=" + this.mRecordsRequested);
        pw.println(" mRecordsToLoad=" + this.mRecordsToLoad);
        pw.println(" mRdnCache=" + this.mAdnCache);
        pw.println(" iccid=" + SubscriptionInfo.givePrintableIccid(this.mFullIccId));
        pw.println(" mMsisdn=" + Rlog.pii(false, this.mMsisdn));
        pw.println(" mMsisdnTag=" + this.mMsisdnTag);
        pw.println(" mVoiceMailNum=" + Rlog.pii(false, this.mVoiceMailNum));
        pw.println(" mVoiceMailTag=" + this.mVoiceMailTag);
        pw.println(" mNewVoiceMailNum=" + Rlog.pii(false, this.mNewVoiceMailNum));
        pw.println(" mNewVoiceMailTag=" + this.mNewVoiceMailTag);
        pw.println(" mIsVoiceMailFixed=" + this.mIsVoiceMailFixed);
        pw.println(" mImsi=" + (this.mImsi != null ? this.mImsi.substring(0, 6) + Rlog.pii(false, this.mImsi.substring(6)) : "null"));
        if (this.mCarrierTestOverride.isInTestMode()) {
            pw.println(" mFakeImsi=" + (this.mFakeImsi != null ? this.mFakeImsi : "null"));
        }
        pw.println(" mMncLength=" + this.mMncLength);
        pw.println(" mMailboxIndex=" + this.mMailboxIndex);
        pw.println(" mSpn=" + this.mSpn);
        if (this.mCarrierTestOverride.isInTestMode()) {
            pw.println(" mFakeSpn=" + (this.mFakeSpn != null ? this.mFakeSpn : "null"));
        }
        pw.flush();
    }

    public boolean is_test_card() {
        return this.mIsTestCard || OemConstant.isTestCard(this.mImsi) || SystemProperties.getInt("persist.sys.oppo.ctlab", 0) == 1;
    }

    public void getPreferedOperatorList(Message onComplete, ServiceStateTracker msst) {
    }

    public void setPOLEntry(NetworkInfoWithAcT networkWithAct, Message onComplete) {
    }

    public boolean isInCnList(String spn) {
        int i = 0;
        if (TextUtils.isEmpty(spn)) {
            return false;
        }
        boolean isCnList = false;
        try {
            String[] plmn_list = this.mContext.getResources().getStringArray(this.mContext.getResources().getIdentifier("oppo_cn_operator_list", "array", "android"));
            int length = plmn_list.length;
            while (i < length) {
                if (spn.equalsIgnoreCase(plmn_list[i])) {
                    isCnList = true;
                    break;
                }
                i++;
            }
        } catch (Exception e) {
            log("len is in cnlist error" + e.getMessage());
        }
        return isCnList;
    }

    public boolean isInCmccList(String spn) {
        int i = 0;
        if (TextUtils.isEmpty(spn)) {
            return false;
        }
        boolean isCnList = false;
        try {
            String[] plmn_list = this.mContext.getResources().getStringArray(this.mContext.getResources().getIdentifier("oppo_cmcc_operator_list", "array", "android"));
            int length = plmn_list.length;
            while (i < length) {
                if (spn.equalsIgnoreCase(plmn_list[i])) {
                    isCnList = true;
                    break;
                }
                i++;
            }
        } catch (Exception e) {
            log("len is in cnlist error" + e.getMessage());
        }
        return isCnList;
    }

    public String oppoGeOperatorByPlmn(Context context, String operatorNumic) {
        if (operatorNumic != null) {
            try {
                return context.getString(context.getResources().getIdentifier("mmcmnc" + operatorNumic, "string", "oppo"));
            } catch (Exception e) {
            }
        }
        return operatorNumic;
    }

    protected void setOemSpnFromConfig(String carrier) {
        if (isInCnList(this.mSpn) || TextUtils.isEmpty(this.mSpn) || (this.mSpn != null && this.mSpn.startsWith("460"))) {
            String operator = SubscriptionController.getOemOperator(this.mContext, carrier);
            if (!TextUtils.isEmpty(operator)) {
                setServiceProviderName(operator);
            }
        }
        setSystemProperty("gsm.sim.operator.alpha", getServiceProviderName());
    }

    public String getSIMCPHSOns() {
        return null;
    }

    public String getSpNameInEfSpn() {
        return null;
    }

    public String isOperatorMvnoForImsi() {
        return null;
    }

    public String isOperatorMvnoForEfPnn() {
        return null;
    }
}
