package com.android.internal.telephony.uicc;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.usage.UsageStatsManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class UiccCard {
    protected static final boolean DBG = true;
    private static final int EVENT_CARD_ADDED = 14;
    private static final int EVENT_CARD_REMOVED = 13;
    private static final int EVENT_CARRIER_PRIVILEGES_LOADED = 20;
    private static final int EVENT_CARRIER_PRIVILIGES_LOADED = 20;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 16;
    private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 15;
    private static final int EVENT_SIM_GET_ATR_DONE = 21;
    private static final int EVENT_SIM_IO_DONE = 19;
    private static final int EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE = 18;
    private static final int EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE = 17;
    public static final String EXTRA_ICC_CARD_ADDED = "com.android.internal.telephony.uicc.ICC_CARD_ADDED";
    protected static final String LOG_TAG = "UiccCard";
    private static final String OPERATOR_BRAND_OVERRIDE_PREFIX = "operator_branding_";
    private static final LocalLog mLocalLog = new LocalLog(100);
    private RegistrantList mAbsentRegistrants = new RegistrantList();
    private CardState mCardState;
    private RegistrantList mCarrierPrivilegeRegistrants = new RegistrantList();
    private UiccCarrierPrivilegeRules mCarrierPrivilegeRules;
    private CatService mCatService;
    private int mCdmaSubscriptionAppIndex;
    private CommandsInterface mCi;
    private Context mContext;
    private int mGsmUmtsSubscriptionAppIndex;
    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 13:
                    UiccCard.this.onIccSwap(false);
                    return;
                case 14:
                    UiccCard.this.onIccSwap(true);
                    return;
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 21:
                    AsyncResult ar = msg.obj;
                    if (ar.exception != null) {
                        UiccCard.this.loglocal("Exception: " + ar.exception);
                        UiccCard.this.log("Error in SIM access with exception" + ar.exception);
                    }
                    AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
                    ((Message) ar.userObj).sendToTarget();
                    return;
                case 20:
                    UiccCard.this.onCarrierPriviligesLoadedMessage();
                    return;
                default:
                    UiccCard.this.loge("Unknown Event " + msg.what);
                    return;
            }
        }
    };
    private int mImsSubscriptionAppIndex;
    private RadioState mLastRadioState = RadioState.RADIO_UNAVAILABLE;
    private final Object mLock = new Object();
    private final int mPhoneId;
    private UiccCardApplication[] mUiccApplications = new UiccCardApplication[8];
    private PinState mUniversalPinState;

    private class ClickListener implements OnClickListener {
        String pkgName;

        public ClickListener(String pkgName) {
            this.pkgName = pkgName;
        }

        public void onClick(DialogInterface dialog, int which) {
            synchronized (UiccCard.this.mLock) {
                if (which == -1) {
                    Intent market = new Intent("android.intent.action.VIEW");
                    market.setData(Uri.parse("market://details?id=" + this.pkgName));
                    market.addFlags(268435456);
                    UiccCard.this.mContext.startActivity(market);
                } else if (which == -2) {
                    UiccCard.this.log("Not now clicked for carrier app dialog.");
                }
            }
        }
    }

    public UiccCard(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId) {
        log("Creating");
        this.mCardState = ics.mCardState;
        this.mPhoneId = phoneId;
        update(c, ci, ics);
    }

    public void dispose() {
        synchronized (this.mLock) {
            log("Disposing card");
            if (this.mCatService != null) {
                this.mCatService.dispose();
            }
            for (UiccCardApplication app : this.mUiccApplications) {
                if (app != null) {
                    app.dispose();
                }
            }
            this.mCatService = null;
            this.mUiccApplications = null;
            this.mCarrierPrivilegeRules = null;
        }
    }

    public void update(Context c, CommandsInterface ci, IccCardStatus ics) {
        synchronized (this.mLock) {
            CardState oldState = this.mCardState;
            this.mCardState = ics.mCardState;
            this.mUniversalPinState = ics.mUniversalPinState;
            this.mGsmUmtsSubscriptionAppIndex = ics.mGsmUmtsSubscriptionAppIndex;
            this.mCdmaSubscriptionAppIndex = ics.mCdmaSubscriptionAppIndex;
            this.mImsSubscriptionAppIndex = ics.mImsSubscriptionAppIndex;
            this.mContext = c;
            this.mCi = ci;
            int airPlaneModeStatus = Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
            log("airPlaneModeStatus:" + airPlaneModeStatus);
            log(ics.mApplications.length + " applications");
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] == null) {
                    if (i < ics.mApplications.length) {
                        this.mUiccApplications[i] = new UiccCardApplication(this, ics.mApplications[i], this.mContext, this.mCi);
                    }
                } else if (i < ics.mApplications.length) {
                    this.mUiccApplications[i].update(ics.mApplications[i], this.mContext, this.mCi);
                } else if (System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0) {
                    this.mUiccApplications[i].dispose();
                    this.mUiccApplications[i] = null;
                }
            }
            createAndUpdateCatServiceLocked();
            log("Before privilege rules: " + this.mCarrierPrivilegeRules + " : " + this.mCardState);
            if (this.mCarrierPrivilegeRules == null && this.mCardState == CardState.CARDSTATE_PRESENT) {
                this.mCarrierPrivilegeRules = new UiccCarrierPrivilegeRules(this, this.mHandler.obtainMessage(20));
            } else if (!(this.mCarrierPrivilegeRules == null || this.mCardState == CardState.CARDSTATE_PRESENT)) {
                this.mCarrierPrivilegeRules = null;
            }
            sanitizeApplicationIndexesLocked();
            int subId = SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhoneId);
            SubscriptionManager.from(this.mContext);
            int subState = SubscriptionManager.getSubState(subId);
            log("subId:" + subId + ",subState:" + subState);
            RadioState radioState = this.mCi.getRadioState();
            log("update: radioState=" + radioState + " mLastRadioState=" + this.mLastRadioState);
            if ((radioState == RadioState.RADIO_ON && this.mLastRadioState == RadioState.RADIO_ON) || (airPlaneModeStatus == 1 && subState == 0)) {
                if (oldState != CardState.CARDSTATE_ABSENT && this.mCardState == CardState.CARDSTATE_ABSENT) {
                    log("update: notify card removed");
                    this.mAbsentRegistrants.notifyRegistrants();
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(13, null));
                } else if (oldState == CardState.CARDSTATE_ABSENT && this.mCardState != CardState.CARDSTATE_ABSENT) {
                    log("update: notify card added");
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(14, null));
                }
            }
            this.mLastRadioState = radioState;
        }
    }

    private void createAndUpdateCatServiceLocked() {
        if (this.mUiccApplications.length <= 0 || this.mUiccApplications[0] == null) {
            if (this.mCatService != null) {
                this.mCatService.dispose();
            }
            this.mCatService = null;
        } else if (this.mCatService == null) {
            this.mCatService = CatService.getInstance(this.mCi, this.mContext, this, this.mPhoneId);
        } else {
            this.mCatService.update(this.mCi, this.mContext, this);
        }
    }

    protected void finalize() {
        log("UiccCard finalized");
    }

    private void sanitizeApplicationIndexesLocked() {
        this.mGsmUmtsSubscriptionAppIndex = checkIndexLocked(this.mGsmUmtsSubscriptionAppIndex, AppType.APPTYPE_SIM, AppType.APPTYPE_USIM);
        this.mCdmaSubscriptionAppIndex = checkIndexLocked(this.mCdmaSubscriptionAppIndex, AppType.APPTYPE_RUIM, AppType.APPTYPE_CSIM);
        this.mImsSubscriptionAppIndex = checkIndexLocked(this.mImsSubscriptionAppIndex, AppType.APPTYPE_ISIM, null);
    }

    private int checkIndexLocked(int index, AppType expectedAppType, AppType altExpectedAppType) {
        if (this.mUiccApplications == null || index >= this.mUiccApplications.length) {
            loge("App index " + index + " is invalid since there are no applications");
            return -1;
        } else if (index < 0) {
            return -1;
        } else {
            if (this.mUiccApplications[index].getType() == expectedAppType || this.mUiccApplications[index].getType() == altExpectedAppType) {
                return index;
            }
            loge("App index " + index + " is invalid since it's not " + expectedAppType + " and not " + altExpectedAppType);
            return -1;
        }
    }

    public void registerForAbsent(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mAbsentRegistrants.add(r);
            if (this.mCardState == CardState.CARDSTATE_ABSENT) {
                r.notifyRegistrant();
            }
        }
    }

    public void unregisterForAbsent(Handler h) {
        synchronized (this.mLock) {
            this.mAbsentRegistrants.remove(h);
        }
    }

    public void registerForCarrierPrivilegeRulesLoaded(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mCarrierPrivilegeRegistrants.add(r);
            if (areCarrierPriviligeRulesLoaded()) {
                r.notifyRegistrant();
            }
        }
    }

    public void unregisterForCarrierPrivilegeRulesLoaded(Handler h) {
        synchronized (this.mLock) {
            this.mCarrierPrivilegeRegistrants.remove(h);
        }
    }

    private void onIccSwap(boolean isAdded) {
        if (this.mContext.getResources().getBoolean(17956977)) {
            log("onIccSwap: isHotSwapSupported is true, don't prompt for rebooting");
            return;
        }
        log("onIccSwap: isHotSwapSupported is false, prompt for rebooting");
        promptForRestart(isAdded);
    }

    private void promptForRestart(boolean isAdded) {
        synchronized (this.mLock) {
            String title;
            String message;
            String dialogComponent = this.mContext.getResources().getString(17039692);
            if (dialogComponent != null) {
                try {
                    this.mContext.startActivity(new Intent().setComponent(ComponentName.unflattenFromString(dialogComponent)).addFlags(268435456).putExtra(EXTRA_ICC_CARD_ADDED, isAdded));
                    return;
                } catch (ActivityNotFoundException e) {
                    loge("Unable to find ICC hotswap prompt for restart activity: " + e);
                }
            }
            OnClickListener listener = new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (UiccCard.this.mLock) {
                        if (which == -1) {
                            UiccCard.this.log("Reboot due to SIM swap");
                            ((PowerManager) UiccCard.this.mContext.getSystemService("power")).reboot("SIM is added.");
                        }
                    }
                }
            };
            Resources r = Resources.getSystem();
            if (isAdded) {
                title = r.getString(17040868);
            } else {
                title = r.getString(17040871);
            }
            if (isAdded) {
                message = r.getString(17040867);
            } else {
                message = r.getString(17040870);
            }
            AlertDialog dialog = new Builder(this.mContext).setTitle(title).setMessage(message).setPositiveButton(r.getString(17040872), listener).create();
            dialog.getWindow().setType(2003);
            dialog.show();
        }
    }

    private boolean isPackageInstalled(String pkgName) {
        try {
            this.mContext.getPackageManager().getPackageInfo(pkgName, 1);
            log(pkgName + " is installed.");
            return true;
        } catch (NameNotFoundException e) {
            log(pkgName + " is not installed.");
            return false;
        }
    }

    private void promptInstallCarrierApp(String pkgName) {
        OnClickListener listener = new ClickListener(pkgName);
        Resources r = Resources.getSystem();
        String message = r.getString(17039621);
        AlertDialog dialog = new Builder(this.mContext).setMessage(message).setNegativeButton(r.getString(17039622), listener).setPositiveButton(r.getString(17039620), listener).create();
        dialog.getWindow().setType(2003);
        dialog.show();
    }

    private void onCarrierPriviligesLoadedMessage() {
        UsageStatsManager usm = (UsageStatsManager) this.mContext.getSystemService("usagestats");
        if (usm != null) {
            usm.onCarrierPrivilegedAppsChanged();
        }
        synchronized (this.mLock) {
            this.mCarrierPrivilegeRegistrants.notifyRegistrants();
            String whitelistSetting = Global.getString(this.mContext.getContentResolver(), "carrier_app_whitelist");
            if (TextUtils.isEmpty(whitelistSetting)) {
                return;
            }
            HashSet<String> carrierAppSet = new HashSet(Arrays.asList(whitelistSetting.split("\\s*;\\s*")));
            if (carrierAppSet.isEmpty()) {
                return;
            }
            for (String pkgName : this.mCarrierPrivilegeRules.getPackageNames()) {
                if (!(TextUtils.isEmpty(pkgName) || !carrierAppSet.contains(pkgName) || (isPackageInstalled(pkgName) ^ 1) == 0)) {
                    promptInstallCarrierApp(pkgName);
                }
            }
        }
    }

    public boolean isApplicationOnIcc(AppType type) {
        synchronized (this.mLock) {
            int i = 0;
            while (i < this.mUiccApplications.length) {
                if (this.mUiccApplications[i] == null || this.mUiccApplications[i].getType() != type) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public CardState getCardState() {
        CardState cardState;
        synchronized (this.mLock) {
            cardState = this.mCardState;
        }
        return cardState;
    }

    public PinState getUniversalPinState() {
        PinState pinState;
        synchronized (this.mLock) {
            pinState = this.mUniversalPinState;
        }
        return pinState;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public UiccCardApplication getApplication(int family) {
        synchronized (this.mLock) {
            int index = 8;
            switch (family) {
                case 1:
                    index = this.mGsmUmtsSubscriptionAppIndex;
                case 2:
                    index = this.mCdmaSubscriptionAppIndex;
                    if (index >= 0) {
                        if (index < this.mUiccApplications.length) {
                            UiccCardApplication uiccCardApplication = this.mUiccApplications[index];
                            return uiccCardApplication;
                        }
                    }
                    return null;
                case 3:
                    index = this.mImsSubscriptionAppIndex;
                    if (index >= 0) {
                        if (index < this.mUiccApplications.length) {
                            UiccCardApplication uiccCardApplication2 = this.mUiccApplications[index];
                            return uiccCardApplication2;
                        }
                    }
                    return null;
            }
            if (index >= 0) {
                if (index < this.mUiccApplications.length) {
                    UiccCardApplication uiccCardApplication22 = this.mUiccApplications[index];
                    return uiccCardApplication22;
                }
            }
            return null;
        }
    }

    public UiccCardApplication getApplicationIndex(int index) {
        synchronized (this.mLock) {
            if (index >= 0) {
                if (index < this.mUiccApplications.length) {
                    UiccCardApplication uiccCardApplication = this.mUiccApplications[index];
                    return uiccCardApplication;
                }
            }
            return null;
        }
    }

    public UiccCardApplication getApplicationByType(int type) {
        synchronized (this.mLock) {
            int i = 0;
            while (i < this.mUiccApplications.length) {
                if (this.mUiccApplications[i] == null || this.mUiccApplications[i].getType().ordinal() != type) {
                    i++;
                } else {
                    UiccCardApplication uiccCardApplication = this.mUiccApplications[i];
                    return uiccCardApplication;
                }
            }
            return null;
        }
    }

    public boolean resetAppWithAid(String aid) {
        boolean changed;
        synchronized (this.mLock) {
            changed = false;
            int i = 0;
            while (i < this.mUiccApplications.length) {
                if (this.mUiccApplications[i] != null && (TextUtils.isEmpty(aid) || aid.equals(this.mUiccApplications[i].getAid()))) {
                    this.mUiccApplications[i].dispose();
                    this.mUiccApplications[i] = null;
                    changed = true;
                }
                i++;
            }
            if (TextUtils.isEmpty(aid)) {
                if (this.mCarrierPrivilegeRules != null) {
                    this.mCarrierPrivilegeRules = null;
                    changed = true;
                }
                if (this.mCatService != null) {
                    this.mCatService.dispose();
                    this.mCatService = null;
                    changed = true;
                }
            }
        }
        return changed;
    }

    public void iccOpenLogicalChannel(String AID, int p2, Message response) {
        loglocal("Open Logical Channel: " + AID + " , " + p2 + " by pid:" + Binder.getCallingPid() + " uid:" + Binder.getCallingUid());
        this.mCi.iccOpenLogicalChannel(AID, p2, this.mHandler.obtainMessage(15, response));
    }

    public void iccCloseLogicalChannel(int channel, Message response) {
        loglocal("Close Logical Channel: " + channel);
        this.mCi.iccCloseLogicalChannel(channel, this.mHandler.obtainMessage(16, response));
    }

    public void iccTransmitApduLogicalChannel(int channel, int cla, int command, int p1, int p2, int p3, String data, Message response) {
        try {
            this.mCi.iccTransmitApduLogicalChannel(channel, cla, command, p1, p2, p3, data, this.mHandler.obtainMessage(17, response));
        } catch (Exception ex) {
            ex.printStackTrace();
            Message msg = this.mHandler.obtainMessage(17);
            msg.obj = new AsyncResult(response, null, ex);
            this.mHandler.sendMessage(msg);
        }
    }

    public void iccTransmitApduBasicChannel(int cla, int command, int p1, int p2, int p3, String data, Message response) {
        this.mCi.iccTransmitApduBasicChannel(cla, command, p1, p2, p3, data, this.mHandler.obtainMessage(18, response));
    }

    public void iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3, String pathID, Message response) {
        this.mCi.iccIO(command, fileID, pathID, p1, p2, p3, null, null, this.mHandler.obtainMessage(19, response));
    }

    public void getAtr(Message response) {
        this.mCi.getAtr(this.mHandler.obtainMessage(21, response));
    }

    public void sendEnvelopeWithStatus(String contents, Message response) {
        this.mCi.sendEnvelopeWithStatus(contents, response);
    }

    public int getNumApplications() {
        int count = 0;
        for (UiccCardApplication a : this.mUiccApplications) {
            if (a != null) {
                count++;
            }
        }
        return count;
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public boolean areCarrierPriviligeRulesLoaded() {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules != null) {
            return carrierPrivilegeRules.areCarrierPriviligeRulesLoaded();
        }
        return true;
    }

    public boolean hasCarrierPrivilegeRules() {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        return carrierPrivilegeRules != null ? carrierPrivilegeRules.hasCarrierPrivilegeRules() : false;
    }

    public int getCarrierPrivilegeStatus(Signature signature, String packageName) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatus(signature, packageName);
    }

    public int getCarrierPrivilegeStatus(PackageManager packageManager, String packageName) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatus(packageManager, packageName);
    }

    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatus(packageInfo);
    }

    public int getCarrierPrivilegeStatusForCurrentTransaction(PackageManager packageManager) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatusForCurrentTransaction(packageManager);
    }

    public List<String> getCarrierPackageNamesForIntent(PackageManager packageManager, Intent intent) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return null;
        }
        return carrierPrivilegeRules.getCarrierPackageNamesForIntent(packageManager, intent);
    }

    private UiccCarrierPrivilegeRules getCarrierPrivilegeRules() {
        UiccCarrierPrivilegeRules uiccCarrierPrivilegeRules;
        synchronized (this.mLock) {
            uiccCarrierPrivilegeRules = this.mCarrierPrivilegeRules;
        }
        return uiccCarrierPrivilegeRules;
    }

    public boolean setOperatorBrandOverride(String brand) {
        log("setOperatorBrandOverride: " + brand);
        log("current iccId: " + getIccId());
        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }
        Editor spEditor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        String key = OPERATOR_BRAND_OVERRIDE_PREFIX + iccId;
        if (brand == null) {
            spEditor.remove(key).commit();
        } else {
            spEditor.putString(key, brand).commit();
        }
        return true;
    }

    public String getOperatorBrandOverride() {
        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return null;
        }
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(OPERATOR_BRAND_OVERRIDE_PREFIX + iccId, null);
    }

    public String getIccId() {
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null) {
                IccRecords ir = app.getIccRecords();
                if (!(ir == null || ir.getIccId() == null)) {
                    return ir.getIccId();
                }
            }
        }
        return null;
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    private void loglocal(String msg) {
        mLocalLog.log(msg);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int i;
        pw.println("UiccCard:");
        pw.println(" mCi=" + this.mCi);
        pw.println(" mLastRadioState=" + this.mLastRadioState);
        pw.println(" mCatService=" + this.mCatService);
        pw.println(" mAbsentRegistrants: size=" + this.mAbsentRegistrants.size());
        for (i = 0; i < this.mAbsentRegistrants.size(); i++) {
            pw.println("  mAbsentRegistrants[" + i + "]=" + ((Registrant) this.mAbsentRegistrants.get(i)).getHandler());
        }
        for (i = 0; i < this.mCarrierPrivilegeRegistrants.size(); i++) {
            pw.println("  mCarrierPrivilegeRegistrants[" + i + "]=" + ((Registrant) this.mCarrierPrivilegeRegistrants.get(i)).getHandler());
        }
        pw.println(" mCardState=" + this.mCardState);
        pw.println(" mUniversalPinState=" + this.mUniversalPinState);
        pw.println(" mGsmUmtsSubscriptionAppIndex=" + this.mGsmUmtsSubscriptionAppIndex);
        pw.println(" mCdmaSubscriptionAppIndex=" + this.mCdmaSubscriptionAppIndex);
        pw.println(" mImsSubscriptionAppIndex=" + this.mImsSubscriptionAppIndex);
        pw.println(" mImsSubscriptionAppIndex=" + this.mImsSubscriptionAppIndex);
        pw.println(" mUiccApplications: length=" + this.mUiccApplications.length);
        for (i = 0; i < this.mUiccApplications.length; i++) {
            if (this.mUiccApplications[i] == null) {
                pw.println("  mUiccApplications[" + i + "]=" + null);
            } else {
                pw.println("  mUiccApplications[" + i + "]=" + this.mUiccApplications[i].getType() + " " + this.mUiccApplications[i]);
            }
        }
        pw.println();
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null) {
                app.dump(fd, pw, args);
                pw.println();
            }
        }
        for (UiccCardApplication app2 : this.mUiccApplications) {
            if (app2 != null) {
                IccRecords ir = app2.getIccRecords();
                if (ir != null) {
                    ir.dump(fd, pw, args);
                    pw.println();
                }
            }
        }
        if (this.mCarrierPrivilegeRules == null) {
            pw.println(" mCarrierPrivilegeRules: null");
        } else {
            pw.println(" mCarrierPrivilegeRules: " + this.mCarrierPrivilegeRules);
            this.mCarrierPrivilegeRules.dump(fd, pw, args);
        }
        pw.println(" mCarrierPrivilegeRegistrants: size=" + this.mCarrierPrivilegeRegistrants.size());
        for (i = 0; i < this.mCarrierPrivilegeRegistrants.size(); i++) {
            pw.println("  mCarrierPrivilegeRegistrants[" + i + "]=" + ((Registrant) this.mCarrierPrivilegeRegistrants.get(i)).getHandler());
        }
        pw.flush();
        pw.println("mLocalLog:");
        mLocalLog.dump(fd, pw, args);
        pw.flush();
    }
}
