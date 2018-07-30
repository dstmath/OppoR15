package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkStats;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.service.carrier.CarrierIdentifier;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellLocation;
import android.telephony.ClientRequestStats;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.VoLteServiceState;
import android.text.TextUtils;
import com.android.ims.ImsCall;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Call.SrvccState;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.dataconnection.DataConnectionReasons;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.gsm.NetworkInfoWithAcT;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.SpnOverride;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UsimServiceTable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.codeaurora.ims.QtiCallConstants;

public abstract class Phone extends Handler implements PhoneInternalInterface {
    private static final int ALREADY_IN_AUTO_SELECTION = 1;
    private static final String CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_non_roaming_list_";
    private static final String CDMA_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_roaming_list_";
    public static final String CF_ENABLED_VIDEO = "cf_enabled_key_video";
    public static final String CF_ID = "cf_id_key";
    public static final String CF_STATUS = "cf_status_key";
    public static final String CLIR_KEY = "clir_key";
    protected static final int CMD_OPPO_GET_BATTERY_COVER_STATUS = 12;
    protected static final int CMD_OPPO_REG_FREQ_HOP_IND = 22;
    protected static final int CMD_OPPO_SET_SAR_RF_STATE = 5;
    protected static final int CMD_OPPO_SET_SIM_CARD_TYPE = 14;
    protected static final int CMD_OPPO_SET_TUNER_LOGIC = 13;
    public static final String CS_FALLBACK = "cs_fallback";
    public static final String DATA_DISABLED_ON_BOOT_KEY = "disabled_on_boot_key";
    public static final String DATA_ROAMING_IS_USER_SETTING_KEY = "data_roaming_is_user_setting_key";
    protected static final boolean DBG = OemConstant.SWITCH_LOG;
    private static final int DEFAULT_REPORT_INTERVAL_MS = 200;
    private static final String DNS_SERVER_CHECK_DISABLED_KEY = "dns_server_check_disabled_key";
    protected static final int EVENT_CALL_RING = 14;
    private static final int EVENT_CALL_RING_CONTINUE = 15;
    protected static final int EVENT_CARRIER_CONFIG_CHANGED = 43;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 27;
    private static final int EVENT_CHECK_FOR_NETWORK_AUTOMATIC = 38;
    private static final int EVENT_CONFIG_LCE = 37;
    protected static final int EVENT_EMERGENCY_CALLBACK_MODE_ENTER = 25;
    protected static final int EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE = 26;
    protected static final int EVENT_GET_BASEBAND_VERSION_DONE = 6;
    protected static final int EVENT_GET_CALL_FORWARD_DONE = 13;
    protected static final int EVENT_GET_DEVICE_IDENTITY_DONE = 21;
    protected static final int EVENT_GET_IMEISV_DONE = 10;
    protected static final int EVENT_GET_IMEI_DONE = 9;
    protected static final int EVENT_GET_RADIO_CAPABILITY = 35;
    private static final int EVENT_GET_SIM_STATUS_DONE = 11;
    private static final int EVENT_ICC_CHANGED = 30;
    protected static final int EVENT_ICC_RECORD_EVENTS = 29;
    private static final int EVENT_INITIATE_SILENT_REDIAL = 32;
    protected static final int EVENT_LAST = 45;
    private static final int EVENT_MMI_DONE = 4;
    protected static final int EVENT_MODEM_RESET = 45;
    protected static final int EVENT_NV_READY = 23;
    protected static final int EVENT_RADIO_AVAILABLE = 1;
    private static final int EVENT_RADIO_NOT_AVAILABLE = 33;
    protected static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 8;
    protected static final int EVENT_RADIO_ON = 5;
    protected static final int EVENT_REGISTERED_TO_NETWORK = 19;
    protected static final int EVENT_REQUEST_VOICE_RADIO_TECH_DONE = 40;
    protected static final int EVENT_RIL_CONNECTED = 41;
    protected static final int EVENT_RUIM_RECORDS_LOADED = 22;
    protected static final int EVENT_SET_CALL_FORWARD_DONE = 12;
    protected static final int EVENT_SET_CLIR_COMPLETE = 18;
    private static final int EVENT_SET_ENHANCED_VP = 24;
    protected static final int EVENT_SET_NETWORK_AUTOMATIC = 28;
    private static final int EVENT_SET_NETWORK_AUTOMATIC_COMPLETE = 17;
    private static final int EVENT_SET_NETWORK_MANUAL_COMPLETE = 16;
    protected static final int EVENT_SET_ROAMING_PREFERENCE_DONE = 44;
    protected static final int EVENT_SET_VM_NUMBER_DONE = 20;
    protected static final int EVENT_SIM_RECORDS_LOADED = 3;
    private static final int EVENT_SRVCC_STATE_CHANGED = 31;
    protected static final int EVENT_SS = 36;
    protected static final int EVENT_SSN = 2;
    private static final int EVENT_UNSOL_OEM_HOOK_RAW = 34;
    protected static final int EVENT_UPDATE_PHONE_OBJECT = 42;
    protected static final int EVENT_USSD = 7;
    protected static final int EVENT_VOICE_RADIO_TECH_CHANGED = 39;
    public static final String EXTRA_KEY_ALERT_MESSAGE = "alertMessage";
    public static final String EXTRA_KEY_ALERT_SHOW = "alertShow";
    public static final String EXTRA_KEY_ALERT_TITLE = "alertTitle";
    public static final String EXTRA_KEY_NOTIFICATION_MESSAGE = "notificationMessage";
    private static final int GET_IMEI_INTERVAL = 3000;
    private static final String GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_non_roaming_list_";
    private static final String GSM_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_roaming_list_";
    private static final boolean LCE_PULL_MODE = true;
    private static final String LOG_TAG = "Phone";
    private static final int MAX_GET_IMEI_RETRY = 3;
    public static final String NETWORK_SELECTION_KEY = "network_selection_key";
    public static final String NETWORK_SELECTION_NAME_KEY = "network_selection_name_key";
    public static final String NETWORK_SELECTION_SHORT_KEY = "network_selection_short_key";
    protected static String[] OEM_IMEI = new String[2];
    private static final String OEM_ISAUTO_ANSWER = "oem_is_auto_answer";
    protected static String OEM_MEID = null;
    protected static final int OPPO_EVENT_GET_MDM_VERSION_DONE = 999;
    protected static final int OPPO_RF_SAR_STATE_CALL_APPROX = 2;
    protected static final int OPPO_RF_SAR_STATE_CALL_APPROX_BATTERY = 8;
    protected static final int OPPO_RF_SAR_STATE_CALL_APPROX_HEADSET = 6;
    protected static final int OPPO_RF_SAR_STATE_CALL_APPROX_USB = 4;
    protected static final int OPPO_RF_SAR_STATE_DEFAULT = 0;
    protected static final int OPPO_RF_SAR_STATE_NON_CALL_APPROX = 1;
    protected static final int OPPO_RF_SAR_STATE_NON_CALL_APPROX_BATTERY = 7;
    protected static final int OPPO_RF_SAR_STATE_NON_CALL_APPROX_HEADSET = 5;
    protected static final int OPPO_RF_SAR_STATE_NON_CALL_APPROX_USB = 3;
    public static final String PROPERTY_QCOM_FREQHOP = "persist.sys.qcom_freqhop";
    protected static final int SAR_RF_STATE_DEFAULT = 0;
    protected static final int SAR_RF_STATE_PROXIMITY_ACQUIRED = 1;
    protected static final int SAR_RF_STATE_PROXIMITY_ACQUIRED_BATTERY = 7;
    protected static final int SAR_RF_STATE_PROXIMITY_ACQUIRED_HEADSET = 5;
    protected static final int SAR_RF_STATE_PROXIMITY_ACQUIRED_USB = 3;
    protected static final int SAR_RF_STATE_PROXIMITY_RELEASE = 2;
    protected static final int SAR_RF_STATE_PROXIMITY_RELEASE_BATTERY = 8;
    protected static final int SAR_RF_STATE_PROXIMITY_RELEASE_HEADSET = 6;
    protected static final int SAR_RF_STATE_PROXIMITY_RELEASE_USB = 4;
    private static final String VM_COUNT = "vm_count_key";
    private static final String VM_ID = "vm_id_key";
    protected static final Object lockForRadioTechnologyChange = new Object();
    protected final int USSD_MAX_QUEUE;
    private final String mActionAttached;
    private final String mActionDetached;
    private final AppSmsManager mAppSmsManager;
    private int mCallRingContinueToken;
    private int mCallRingDelay;
    protected CarrierActionAgent mCarrierActionAgent;
    protected CarrierSignalAgent mCarrierSignalAgent;
    private int mCatchImeiFail;
    public CommandsInterface mCi;
    protected final Context mContext;
    public DcTracker mDcTracker;
    protected DeviceStateMonitor mDeviceStateMonitor;
    protected final RegistrantList mDisconnectRegistrants;
    private boolean mDnsCheckDisabled;
    private boolean mDoesRilSendMultipleCallRing;
    protected final RegistrantList mEmergencyCallToggledRegistrants;
    private final RegistrantList mHandoverRegistrants;
    protected final AtomicReference<IccRecords> mIccRecords;
    private BroadcastReceiver mImsIntentReceiver;
    private ImsManager mImsMgr;
    protected Phone mImsPhone;
    private boolean mImsServiceReady;
    private final RegistrantList mIncomingRingRegistrants;
    private boolean mIsPendingSRVCC;
    protected boolean mIsPhoneInEcmState;
    protected boolean mIsVideoCapable;
    private boolean mIsVoiceCapable;
    private int mLceStatus;
    private Looper mLooper;
    private String mMdmVersion;
    protected final RegistrantList mMmiCompleteRegistrants;
    protected final RegistrantList mMmiRegistrants;
    private String mName;
    private final RegistrantList mNewRingingConnectionRegistrants;
    protected PhoneNotifier mNotifier;
    private boolean mOppoNeedNotify;
    protected int mPhoneId;
    protected Registrant mPostDialHandler;
    private final RegistrantList mPreciseCallStateRegistrants;
    protected final AtomicReference<RadioCapability> mRadioCapability;
    protected final RegistrantList mRadioOffOrNotAvailableRegistrants;
    private final RegistrantList mServiceStateRegistrants;
    private SimActivationTracker mSimActivationTracker;
    protected final RegistrantList mSimRecordsLoadedRegistrants;
    protected SimulatedRadioControl mSimulatedRadioControl;
    public SmsStorageMonitor mSmsStorageMonitor;
    public SmsUsageMonitor mSmsUsageMonitor;
    private SrvccState mSrvccState;
    protected final RegistrantList mSuppServiceFailedRegistrants;
    protected TelephonyComponentFactory mTelephonyComponentFactory;
    private TelephonyTester mTelephonyTester;
    protected AtomicReference<UiccCardApplication> mUiccApplication;
    protected UiccController mUiccController;
    private boolean mUnitTestMode;
    protected final RegistrantList mUnknownConnectionRegistrants;
    private final RegistrantList mVideoCapabilityChangedRegistrants;
    protected int mVmCount;

    private static class NetworkSelectMessage {
        public Message message;
        public String operatorAlphaLong;
        public String operatorAlphaShort;
        public String operatorNumeric;

        /* synthetic */ NetworkSelectMessage(NetworkSelectMessage -this0) {
            this();
        }

        private NetworkSelectMessage() {
        }
    }

    public abstract int getPhoneType();

    public abstract State getState();

    protected abstract void onUpdateIccAvailability();

    public abstract void sendEmergencyCallStateChange(boolean z);

    public abstract void setBroadcastEmergencyCallStateChanges(boolean z);

    protected void handleExitEmergencyCallbackMode() {
    }

    public IccRecords getIccRecords() {
        return (IccRecords) this.mIccRecords.get();
    }

    public String getPhoneName() {
        return this.mName;
    }

    protected void setPhoneName(String name) {
        this.mName = name;
    }

    public String getNai() {
        return null;
    }

    public String getActionDetached() {
        return this.mActionDetached;
    }

    public String getActionAttached() {
        return this.mActionAttached;
    }

    public void setSystemProperty(String property, String value) {
        if (!getUnitTestMode()) {
            SystemProperties.set(property, value);
        }
    }

    public String getSystemProperty(String property, String defValue) {
        if (getUnitTestMode()) {
            return null;
        }
        return SystemProperties.get(property, defValue);
    }

    protected Phone(String name, PhoneNotifier notifier, Context context, CommandsInterface ci, boolean unitTestMode) {
        this(name, notifier, context, ci, unitTestMode, Integer.MAX_VALUE, TelephonyComponentFactory.getInstance());
    }

    protected Phone(String name, PhoneNotifier notifier, Context context, CommandsInterface ci, boolean unitTestMode, int phoneId, TelephonyComponentFactory telephonyComponentFactory) {
        this.USSD_MAX_QUEUE = 10;
        this.mImsIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Rlog.d(Phone.LOG_TAG, "mImsIntentReceiver: action " + intent.getAction());
                if (intent.hasExtra("android:phone_id")) {
                    int extraPhoneId = intent.getIntExtra("android:phone_id", -1);
                    Rlog.d(Phone.LOG_TAG, "mImsIntentReceiver: extraPhoneId = " + extraPhoneId);
                    if (extraPhoneId == -1 || extraPhoneId != Phone.this.getPhoneId()) {
                        return;
                    }
                }
                synchronized (Phone.lockForRadioTechnologyChange) {
                    if (intent.getAction().equals("com.android.ims.IMS_SERVICE_UP")) {
                        Phone.this.mImsServiceReady = true;
                        Phone.this.updateImsPhone();
                        if (Phone.this.mImsMgr != null) {
                            Phone.this.mImsMgr.updateImsServiceConfigForSlot(false);
                        }
                    } else if (intent.getAction().equals("com.android.ims.IMS_SERVICE_DOWN")) {
                        Phone.this.mImsServiceReady = false;
                        Phone.this.updateImsPhone();
                    } else if (intent.getAction().equals("com.android.intent.action.IMS_CONFIG_CHANGED")) {
                        ImsManager.onProvisionedValueChanged(context, intent.getIntExtra("item", -1), intent.getStringExtra("value"));
                    }
                }
            }
        };
        this.mVmCount = 0;
        this.mIsVoiceCapable = true;
        this.mIsPhoneInEcmState = false;
        this.mIsVideoCapable = false;
        this.mUiccController = null;
        this.mIccRecords = new AtomicReference();
        this.mUiccApplication = new AtomicReference();
        this.mImsServiceReady = false;
        this.mImsPhone = null;
        this.mRadioCapability = new AtomicReference();
        this.mLceStatus = -1;
        this.mPreciseCallStateRegistrants = new RegistrantList();
        this.mHandoverRegistrants = new RegistrantList();
        this.mNewRingingConnectionRegistrants = new RegistrantList();
        this.mIncomingRingRegistrants = new RegistrantList();
        this.mDisconnectRegistrants = new RegistrantList();
        this.mServiceStateRegistrants = new RegistrantList();
        this.mMmiCompleteRegistrants = new RegistrantList();
        this.mMmiRegistrants = new RegistrantList();
        this.mUnknownConnectionRegistrants = new RegistrantList();
        this.mSuppServiceFailedRegistrants = new RegistrantList();
        this.mRadioOffOrNotAvailableRegistrants = new RegistrantList();
        this.mSimRecordsLoadedRegistrants = new RegistrantList();
        this.mVideoCapabilityChangedRegistrants = new RegistrantList();
        this.mEmergencyCallToggledRegistrants = new RegistrantList();
        this.mImsMgr = null;
        this.mCatchImeiFail = 0;
        this.mOppoNeedNotify = false;
        this.mSrvccState = SrvccState.NONE;
        this.mIsPendingSRVCC = false;
        this.mPhoneId = phoneId;
        this.mName = name;
        this.mNotifier = notifier;
        this.mContext = context;
        this.mLooper = Looper.myLooper();
        this.mCi = ci;
        this.mActionDetached = getClass().getPackage().getName() + ".action_detached";
        this.mActionAttached = getClass().getPackage().getName() + ".action_attached";
        this.mAppSmsManager = telephonyComponentFactory.makeAppSmsManager(context);
        if (Build.IS_DEBUGGABLE) {
            this.mTelephonyTester = new TelephonyTester(this);
        }
        setUnitTestMode(unitTestMode);
        this.mDnsCheckDisabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DNS_SERVER_CHECK_DISABLED_KEY, false);
        this.mCi.setOnCallRing(this, 14, null);
        this.mIsVoiceCapable = this.mContext.getResources().getBoolean(17957059);
        this.mDoesRilSendMultipleCallRing = SystemProperties.getBoolean("ro.telephony.call_ring.multiple", true);
        Rlog.d(LOG_TAG, "mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing);
        this.mCallRingDelay = SystemProperties.getInt("ro.telephony.call_ring.delay", GET_IMEI_INTERVAL);
        Rlog.d(LOG_TAG, "mCallRingDelay=" + this.mCallRingDelay);
        if (getPhoneType() != 5) {
            Locale carrierLocale = getLocaleFromCarrierProperties(this.mContext);
            if (!(carrierLocale == null || (TextUtils.isEmpty(carrierLocale.getCountry()) ^ 1) == 0)) {
                String country = carrierLocale.getCountry();
                try {
                    Global.getInt(this.mContext.getContentResolver(), "wifi_country_code");
                } catch (SettingNotFoundException e) {
                    ((WifiManager) this.mContext.getSystemService("wifi")).setCountryCode(country, false);
                }
            }
            this.mTelephonyComponentFactory = telephonyComponentFactory;
            this.mSmsStorageMonitor = this.mTelephonyComponentFactory.makeSmsStorageMonitor(this);
            this.mSmsUsageMonitor = this.mTelephonyComponentFactory.makeSmsUsageMonitor(context);
            this.mUiccController = UiccController.getInstance();
            this.mUiccController.registerForIccChanged(this, 30, null);
            this.mSimActivationTracker = this.mTelephonyComponentFactory.makeSimActivationTracker(this);
            if (getPhoneType() != 3) {
                this.mCi.registerForSrvccStateChanged(this, 31, null);
            }
            this.mCi.setOnUnsolOemHookRaw(this, 34, null);
            this.mCi.startLceService(200, true, obtainMessage(37));
            this.mDeviceStateMonitor = this.mTelephonyComponentFactory.makeDeviceStateMonitor(this);
        }
    }

    public void startMonitoringImsService() {
        if (getPhoneType() != 3) {
            synchronized (lockForRadioTechnologyChange) {
                IntentFilter filter = new IntentFilter();
                this.mImsMgr = ImsManager.getInstance(this.mContext, getPhoneId());
                if (!(this.mImsMgr == null || (this.mImsMgr.isDynamicBinding() ^ 1) == 0)) {
                    filter.addAction("com.android.ims.IMS_SERVICE_UP");
                    filter.addAction("com.android.ims.IMS_SERVICE_DOWN");
                }
                filter.addAction("com.android.intent.action.IMS_CONFIG_CHANGED");
                this.mContext.registerReceiver(this.mImsIntentReceiver, filter);
                if (this.mImsMgr != null && (this.mImsMgr.isDynamicBinding() || this.mImsMgr.isServiceAvailable())) {
                    this.mImsServiceReady = true;
                    updateImsPhone();
                }
            }
        }
    }

    public boolean supportsConversionOfCdmaCallerIdMmiCodesWhileRoaming() {
        PersistableBundle b = ((CarrierConfigManager) getContext().getSystemService("carrier_config")).getConfig();
        if (b != null) {
            return b.getBoolean("convert_cdma_caller_id_mmi_codes_while_roaming_on_3gpp_bool", false);
        }
        return false;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 16:
            case 17:
                handleSetSelectNetwork((AsyncResult) msg.obj);
                return;
            default:
                AsyncResult ar;
                switch (msg.what) {
                    case 14:
                        Rlog.d(LOG_TAG, "Event EVENT_CALL_RING Received state=" + getState());
                        if (msg.obj.exception == null) {
                            State state = getState();
                            if (!this.mDoesRilSendMultipleCallRing && (state == State.RINGING || state == State.IDLE)) {
                                this.mCallRingContinueToken++;
                                sendIncomingCallRingNotification(this.mCallRingContinueToken);
                                break;
                            }
                            notifyIncomingRing();
                            break;
                        }
                        break;
                    case 15:
                        Rlog.d(LOG_TAG, "Event EVENT_CALL_RING_CONTINUE Received state=" + getState());
                        if (getState() == State.RINGING) {
                            sendIncomingCallRingNotification(msg.arg1);
                            break;
                        }
                        break;
                    case 30:
                        onUpdateIccAvailability();
                        break;
                    case 31:
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
                            Rlog.e(LOG_TAG, "Srvcc exception: " + ar.exception);
                            break;
                        } else {
                            handleSrvccStateChanged((int[]) ar.result);
                            break;
                        }
                    case 32:
                        Rlog.d(LOG_TAG, "Event EVENT_INITIATE_SILENT_REDIAL Received");
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception == null && ar.result != null) {
                            String dialString = ar.result;
                            if (!TextUtils.isEmpty(dialString)) {
                                try {
                                    dialInternal(dialString, null, 0, null);
                                    break;
                                } catch (CallStateException e) {
                                    Rlog.e(LOG_TAG, "silent redial failed: " + e);
                                    break;
                                }
                            }
                            return;
                        }
                    case 34:
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
                            Rlog.e(LOG_TAG, "OEM hook raw exception: " + ar.exception);
                            break;
                        }
                        this.mNotifier.notifyOemHookRawEventForSubscriber(getSubId(), ar.result);
                        break;
                    case 37:
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception == null) {
                            this.mLceStatus = ((Integer) ar.result.get(0)).intValue();
                            break;
                        } else {
                            Rlog.d(LOG_TAG, "config LCE service failed: " + ar.exception);
                            break;
                        }
                    case 38:
                        onCheckForNetworkSelectionModeAutomatic(msg);
                        break;
                    case 999:
                        Rlog.d(LOG_TAG, "Event OPPO_EVENT_GET_MDM_VERSION_DONE Received");
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception == null) {
                            if (ar.result != null) {
                                this.mMdmVersion = (String) ar.result;
                            } else {
                                this.mMdmVersion = null;
                            }
                            Rlog.d(LOG_TAG, "get modem version:" + this.mMdmVersion);
                            break;
                        }
                        Rlog.d(LOG_TAG, "get modem version failed: " + ar.exception);
                        break;
                    default:
                        Rlog.w(LOG_TAG, "unexpected event not handled:" + msg.what);
                        break;
                }
                return;
        }
    }

    public ArrayList<Connection> getHandoverConnection() {
        return null;
    }

    public void notifySrvccState(SrvccState state) {
    }

    public void registerForSilentRedial(Handler h, int what, Object obj) {
    }

    public void unregisterForSilentRedial(Handler h) {
    }

    private void handleSrvccStateChanged(int[] ret) {
        Rlog.d(LOG_TAG, "handleSrvccStateChanged");
        ArrayList<Connection> conn = null;
        Phone imsPhone = this.mImsPhone;
        SrvccState srvccState = SrvccState.NONE;
        if (!(ret == null || ret.length == 0)) {
            switch (ret[0]) {
                case 0:
                    srvccState = SrvccState.STARTED;
                    if (imsPhone == null) {
                        Rlog.d(LOG_TAG, "HANDOVER_STARTED: mImsPhone null");
                        break;
                    }
                    conn = imsPhone.getHandoverConnection();
                    migrateFrom(imsPhone);
                    break;
                case 1:
                    srvccState = SrvccState.COMPLETED;
                    if (imsPhone == null) {
                        Rlog.d(LOG_TAG, "HANDOVER_COMPLETED: mImsPhone null");
                        break;
                    } else {
                        imsPhone.notifySrvccState(srvccState);
                        break;
                    }
                case 2:
                case 3:
                    srvccState = SrvccState.FAILED;
                    break;
                default:
                    return;
            }
            getCallTracker().notifySrvccState(srvccState, conn);
            this.mSrvccState = srvccState;
            if (this.mIsPendingSRVCC && this.mSrvccState == SrvccState.COMPLETED) {
                getCallTracker().pollCallsAfterDelay();
            }
            this.mIsPendingSRVCC = false;
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    public void disableDnsCheck(boolean b) {
        this.mDnsCheckDisabled = b;
        Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putBoolean(DNS_SERVER_CHECK_DISABLED_KEY, b);
        editor.apply();
    }

    public boolean isDnsCheckDisabled() {
        return this.mDnsCheckDisabled;
    }

    public void registerForPreciseCallStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mPreciseCallStateRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForPreciseCallStateChanged(Handler h) {
        this.mPreciseCallStateRegistrants.remove(h);
    }

    protected void notifyPreciseCallStateChangedP() {
        this.mPreciseCallStateRegistrants.notifyRegistrants(new AsyncResult(null, this, null));
        this.mNotifier.notifyPreciseCallState(this);
    }

    public void registerForHandoverStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mHandoverRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForHandoverStateChanged(Handler h) {
        this.mHandoverRegistrants.remove(h);
    }

    public void notifyHandoverStateChanged(Connection cn) {
        this.mHandoverRegistrants.notifyRegistrants(new AsyncResult(null, cn, null));
    }

    protected void setIsInEmergencyCall() {
    }

    protected void migrateFrom(Phone from) {
        migrate(this.mHandoverRegistrants, from.mHandoverRegistrants);
        migrate(this.mPreciseCallStateRegistrants, from.mPreciseCallStateRegistrants);
        migrate(this.mNewRingingConnectionRegistrants, from.mNewRingingConnectionRegistrants);
        migrate(this.mIncomingRingRegistrants, from.mIncomingRingRegistrants);
        migrate(this.mDisconnectRegistrants, from.mDisconnectRegistrants);
        migrate(this.mServiceStateRegistrants, from.mServiceStateRegistrants);
        migrate(this.mMmiCompleteRegistrants, from.mMmiCompleteRegistrants);
        migrate(this.mMmiRegistrants, from.mMmiRegistrants);
        migrate(this.mUnknownConnectionRegistrants, from.mUnknownConnectionRegistrants);
        migrate(this.mSuppServiceFailedRegistrants, from.mSuppServiceFailedRegistrants);
        if (from.isInEmergencyCall()) {
            setIsInEmergencyCall();
        }
    }

    protected void migrate(RegistrantList to, RegistrantList from) {
        from.removeCleared();
        int n = from.size();
        for (int i = 0; i < n; i++) {
            Message msg = ((Registrant) from.get(i)).messageForRegistrant();
            if (msg == null) {
                Rlog.d(LOG_TAG, "msg is null");
            } else if (msg.obj != CallManager.getInstance().getRegistrantIdentifier()) {
                to.add((Registrant) from.get(i));
            }
        }
    }

    public void registerForUnknownConnection(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mUnknownConnectionRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForUnknownConnection(Handler h) {
        this.mUnknownConnectionRegistrants.remove(h);
    }

    public void registerForNewRingingConnection(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mNewRingingConnectionRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForNewRingingConnection(Handler h) {
        this.mNewRingingConnectionRegistrants.remove(h);
    }

    public void registerForVideoCapabilityChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mVideoCapabilityChangedRegistrants.addUnique(h, what, obj);
        notifyForVideoCapabilityChanged(this.mIsVideoCapable);
    }

    public void unregisterForVideoCapabilityChanged(Handler h) {
        this.mVideoCapabilityChangedRegistrants.remove(h);
    }

    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj) {
        this.mCi.registerForInCallVoicePrivacyOn(h, what, obj);
    }

    public void unregisterForInCallVoicePrivacyOn(Handler h) {
        this.mCi.unregisterForInCallVoicePrivacyOn(h);
    }

    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj) {
        this.mCi.registerForInCallVoicePrivacyOff(h, what, obj);
    }

    public void unregisterForInCallVoicePrivacyOff(Handler h) {
        this.mCi.unregisterForInCallVoicePrivacyOff(h);
    }

    public void registerForIncomingRing(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mIncomingRingRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForIncomingRing(Handler h) {
        this.mIncomingRingRegistrants.remove(h);
    }

    public void registerForDisconnect(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mDisconnectRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForDisconnect(Handler h) {
        this.mDisconnectRegistrants.remove(h);
    }

    public void registerForSuppServiceFailed(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mSuppServiceFailedRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSuppServiceFailed(Handler h) {
        this.mSuppServiceFailedRegistrants.remove(h);
    }

    public void registerForMmiInitiate(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mMmiRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForMmiInitiate(Handler h) {
        this.mMmiRegistrants.remove(h);
    }

    public void registerForMmiComplete(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mMmiCompleteRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForMmiComplete(Handler h) {
        checkCorrectThread(h);
        this.mMmiCompleteRegistrants.remove(h);
    }

    public void registerForSimRecordsLoaded(Handler h, int what, Object obj) {
    }

    public void unregisterForSimRecordsLoaded(Handler h) {
    }

    public void registerForTtyModeReceived(Handler h, int what, Object obj) {
    }

    public void unregisterForTtyModeReceived(Handler h) {
    }

    public void setNetworkSelectionModeAutomatic(Message response) {
        Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic, querying current mode");
        Message msg = obtainMessage(38);
        msg.obj = response;
        this.mCi.getNetworkSelectionMode(msg);
    }

    private void onCheckForNetworkSelectionModeAutomatic(Message fromRil) {
        AsyncResult ar = fromRil.obj;
        Message response = ar.userObj;
        boolean doAutomatic = true;
        if (ar.exception == null && ar.result != null) {
            try {
                if (ar.result[0] == 0) {
                    doAutomatic = false;
                }
            } catch (Exception e) {
            }
        }
        NetworkSelectMessage nsm = new NetworkSelectMessage();
        nsm.message = response;
        nsm.operatorNumeric = SpnOverride.MVNO_TYPE_NONE;
        nsm.operatorAlphaLong = SpnOverride.MVNO_TYPE_NONE;
        nsm.operatorAlphaShort = SpnOverride.MVNO_TYPE_NONE;
        if (doAutomatic) {
            this.mCi.setNetworkSelectionModeAutomatic(obtainMessage(17, nsm));
        } else {
            Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic - already auto, ignoring");
            if (nsm.message != null) {
                nsm.message.arg1 = 1;
            }
            ar.userObj = nsm;
            handleSetSelectNetwork(ar);
        }
        updateSavedNetworkOperator(nsm);
    }

    public void getNetworkSelectionMode(Message message) {
        this.mCi.getNetworkSelectionMode(message);
    }

    public List<ClientRequestStats> getClientRequestStats() {
        return this.mCi.getClientRequestStats();
    }

    public void selectNetworkManually(OperatorInfo network, boolean persistSelection, Message response) {
        NetworkSelectMessage nsm = new NetworkSelectMessage();
        nsm.message = response;
        nsm.operatorNumeric = network.getOperatorNumeric();
        nsm.operatorAlphaLong = network.getOperatorAlphaLong();
        nsm.operatorAlphaShort = network.getOperatorAlphaShort();
        Message msg = obtainMessage(16, nsm);
        if (TextUtils.isEmpty(network.getNetworktype())) {
            this.mCi.setNetworkSelectionModeManual(network.getOperatorNumeric(), msg);
        } else {
            this.mCi.setNetworkSelectionModeManual(network.getOperatorNumeric() + "+" + network.getNetworktype(), msg);
        }
        if (persistSelection) {
            updateSavedNetworkOperator(nsm);
        } else {
            clearSavedNetworkSelection();
        }
    }

    public void registerForEmergencyCallToggle(Handler h, int what, Object obj) {
        this.mEmergencyCallToggledRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForEmergencyCallToggle(Handler h) {
        this.mEmergencyCallToggledRegistrants.remove(h);
    }

    private void updateSavedNetworkOperator(NetworkSelectMessage nsm) {
        int subId = getSubId();
        if (SubscriptionController.getInstance().isActiveSubId(subId)) {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            editor.putString(NETWORK_SELECTION_KEY + subId, nsm.operatorNumeric);
            editor.putString(NETWORK_SELECTION_NAME_KEY + subId, nsm.operatorAlphaLong);
            editor.putString(NETWORK_SELECTION_SHORT_KEY + subId, nsm.operatorAlphaShort);
            if (!editor.commit()) {
                Rlog.e(LOG_TAG, "failed to commit network selection preference");
                return;
            }
            return;
        }
        Rlog.e(LOG_TAG, "Cannot update network selection preference due to invalid subId " + subId);
    }

    private void handleSetSelectNetwork(AsyncResult ar) {
        if (ar.userObj instanceof NetworkSelectMessage) {
            NetworkSelectMessage nsm = ar.userObj;
            if (nsm.message != null) {
                AsyncResult.forMessage(nsm.message, ar.result, ar.exception);
                nsm.message.sendToTarget();
            }
            return;
        }
        Rlog.e(LOG_TAG, "unexpected result from user object.");
    }

    private OperatorInfo getSavedNetworkSelection() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        return new OperatorInfo(sp.getString(NETWORK_SELECTION_NAME_KEY + getSubId(), SpnOverride.MVNO_TYPE_NONE), sp.getString(NETWORK_SELECTION_SHORT_KEY + getSubId(), SpnOverride.MVNO_TYPE_NONE), sp.getString(NETWORK_SELECTION_KEY + getSubId(), SpnOverride.MVNO_TYPE_NONE));
    }

    private void clearSavedNetworkSelection() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove(NETWORK_SELECTION_KEY + getSubId()).remove(NETWORK_SELECTION_NAME_KEY + getSubId()).remove(NETWORK_SELECTION_SHORT_KEY + getSubId()).commit();
    }

    protected void restoreSavedNetworkSelection(Message response) {
        OperatorInfo networkSelection = getSavedNetworkSelection();
        if (networkSelection == null || TextUtils.isEmpty(networkSelection.getOperatorNumeric())) {
            setNetworkSelectionModeAutomatic(response);
        } else {
            selectNetworkManually(networkSelection, true, response);
        }
    }

    public void saveClirSetting(int commandInterfaceCLIRMode) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putInt(CLIR_KEY + getPhoneId(), commandInterfaceCLIRMode);
        Rlog.i(LOG_TAG, "saveClirSetting: clir_key" + getPhoneId() + "=" + commandInterfaceCLIRMode);
        if (!editor.commit()) {
            Rlog.e(LOG_TAG, "Failed to commit CLIR preference");
        }
    }

    private void setUnitTestMode(boolean f) {
        this.mUnitTestMode = f;
    }

    public boolean getUnitTestMode() {
        return this.mUnitTestMode;
    }

    protected void notifyDisconnectP(Connection cn) {
        this.mDisconnectRegistrants.notifyRegistrants(new AsyncResult(null, cn, null));
    }

    public void registerForServiceStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mServiceStateRegistrants.add(h, what, obj);
    }

    public void unregisterForServiceStateChanged(Handler h) {
        this.mServiceStateRegistrants.remove(h);
    }

    public void registerForRingbackTone(Handler h, int what, Object obj) {
        this.mCi.registerForRingbackTone(h, what, obj);
    }

    public void unregisterForRingbackTone(Handler h) {
        this.mCi.unregisterForRingbackTone(h);
    }

    public void registerForOnHoldTone(Handler h, int what, Object obj) {
    }

    public void unregisterForOnHoldTone(Handler h) {
    }

    public void registerForResendIncallMute(Handler h, int what, Object obj) {
        this.mCi.registerForResendIncallMute(h, what, obj);
    }

    public void unregisterForResendIncallMute(Handler h) {
        this.mCi.unregisterForResendIncallMute(h);
    }

    public void setEchoSuppressionEnabled() {
    }

    protected void notifyServiceStateChangedP(ServiceState ss) {
        this.mServiceStateRegistrants.notifyRegistrants(new AsyncResult(null, ss, null));
        this.mNotifier.notifyServiceState(this);
    }

    public SimulatedRadioControl getSimulatedRadioControl() {
        return this.mSimulatedRadioControl;
    }

    private void checkCorrectThread(Handler h) {
        if (h.getLooper() != this.mLooper) {
            throw new RuntimeException("com.android.internal.telephony.Phone must be used from within one thread");
        }
    }

    private static Locale getLocaleFromCarrierProperties(Context ctx) {
        String carrier = SystemProperties.get("ro.carrier");
        if (carrier == null || carrier.length() == 0 || "unknown".equals(carrier)) {
            return null;
        }
        CharSequence[] carrierLocales = ctx.getResources().getTextArray(17235975);
        for (int i = 0; i < carrierLocales.length; i += 3) {
            if (carrier.equals(carrierLocales[i].toString())) {
                return Locale.forLanguageTag(carrierLocales[i + 1].toString().replace('_', '-'));
            }
        }
        return null;
    }

    public IccFileHandler getIccFileHandler() {
        IccFileHandler fh;
        UiccCardApplication uiccApplication = (UiccCardApplication) this.mUiccApplication.get();
        if (uiccApplication == null) {
            Rlog.d(LOG_TAG, "getIccFileHandler: uiccApplication == null, return null");
            fh = null;
        } else {
            fh = uiccApplication.getIccFileHandler();
        }
        Rlog.d(LOG_TAG, "getIccFileHandler: fh=" + fh);
        return fh;
    }

    public Handler getHandler() {
        return this;
    }

    public void updatePhoneObject(int voiceRadioTech) {
    }

    public ServiceStateTracker getServiceStateTracker() {
        return null;
    }

    public CallTracker getCallTracker() {
        return null;
    }

    public void setVoiceActivationState(int state) {
        this.mSimActivationTracker.setVoiceActivationState(state);
    }

    public void setDataActivationState(int state) {
        this.mSimActivationTracker.setDataActivationState(state);
    }

    public int getVoiceActivationState() {
        return this.mSimActivationTracker.getVoiceActivationState();
    }

    public int getDataActivationState() {
        return this.mSimActivationTracker.getDataActivationState();
    }

    public void updateVoiceMail() {
        Rlog.e(LOG_TAG, "updateVoiceMail() should be overridden");
    }

    public AppType getCurrentUiccAppType() {
        UiccCardApplication currentApp = (UiccCardApplication) this.mUiccApplication.get();
        if (currentApp != null) {
            return currentApp.getType();
        }
        return AppType.APPTYPE_UNKNOWN;
    }

    public IccCard getIccCard() {
        return null;
    }

    public String getIccSerialNumber() {
        IccRecords r = (IccRecords) this.mIccRecords.get();
        if (r != null) {
            return r.getIccId();
        }
        return null;
    }

    public String getFullIccSerialNumber() {
        IccRecords r = (IccRecords) this.mIccRecords.get();
        if (r != null) {
            return r.getFullIccId();
        }
        return null;
    }

    public boolean getIccRecordsLoaded() {
        IccRecords r = (IccRecords) this.mIccRecords.get();
        return r != null ? r.getRecordsLoaded() : false;
    }

    public List<CellInfo> getAllCellInfo(WorkSource workSource) {
        return privatizeCellInfoList(getServiceStateTracker().getAllCellInfo(workSource));
    }

    public CellLocation getCellLocation() {
        return getCellLocation(null);
    }

    private List<CellInfo> privatizeCellInfoList(List<CellInfo> cellInfoList) {
        if (cellInfoList == null) {
            return null;
        }
        if (Secure.getInt(getContext().getContentResolver(), "location_mode", 0) == 0) {
            ArrayList<CellInfo> privateCellInfoList = new ArrayList(cellInfoList.size());
            for (CellInfo c : cellInfoList) {
                if (c instanceof CellInfoCdma) {
                    CellInfoCdma cellInfoCdma = (CellInfoCdma) c;
                    CellIdentityCdma cellIdentity = cellInfoCdma.getCellIdentity();
                    CellIdentityCdma maskedCellIdentity = new CellIdentityCdma(cellIdentity.getNetworkId(), cellIdentity.getSystemId(), cellIdentity.getBasestationId(), Integer.MAX_VALUE, Integer.MAX_VALUE);
                    CellInfoCdma privateCellInfoCdma = new CellInfoCdma(cellInfoCdma);
                    privateCellInfoCdma.setCellIdentity(maskedCellIdentity);
                    privateCellInfoList.add(privateCellInfoCdma);
                } else {
                    privateCellInfoList.add(c);
                }
            }
            cellInfoList = privateCellInfoList;
        }
        return cellInfoList;
    }

    public void setCellInfoListRate(int rateInMillis, WorkSource workSource) {
        this.mCi.setCellInfoListRate(rateInMillis, null, workSource);
    }

    public boolean getMessageWaitingIndicator() {
        return this.mVmCount != 0;
    }

    private int getCallForwardingIndicatorFromSharedPref() {
        boolean z = true;
        int status = 0;
        int subId = getSubId();
        if (SubscriptionController.getInstance().isActiveSubId(subId)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            status = sp.getInt(CF_STATUS + subId, -1);
            Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: for subId " + subId + "= " + status);
            if (status == -1) {
                String subscriberId = sp.getString(CF_ID, null);
                if (subscriberId != null) {
                    if (subscriberId.equals(getSubscriberId())) {
                        status = sp.getInt(CF_STATUS, 0);
                        if (status != 1) {
                            z = false;
                        }
                        setCallForwardingIndicatorInSharedPref(z);
                        Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: " + status);
                    } else {
                        Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: returning DISABLED as status for matching subscriberId not found");
                    }
                    Editor editor = sp.edit();
                    editor.remove(CF_ID);
                    editor.remove(CF_STATUS);
                    editor.apply();
                }
            }
        } else {
            Rlog.e(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: invalid subId " + subId);
        }
        return status;
    }

    private void setCallForwardingIndicatorInSharedPref(boolean enable) {
        int status;
        if (enable) {
            status = 1;
        } else {
            status = 0;
        }
        int subId = getSubId();
        Rlog.i(LOG_TAG, "setCallForwardingIndicatorInSharedPref: Storing status = " + status + " in pref " + CF_STATUS + subId);
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        editor.putInt(CF_STATUS + subId, status);
        editor.apply();
    }

    public void setVoiceCallForwardingFlag(int line, boolean enable, String number) {
        setCallForwardingIndicatorInSharedPref(enable);
        IccRecords r = (IccRecords) this.mIccRecords.get();
        if (r != null) {
            r.setVoiceCallForwardingFlag(line, enable, number);
        }
    }

    protected void setVoiceCallForwardingFlag(IccRecords r, int line, boolean enable, String number) {
        setCallForwardingIndicatorInSharedPref(enable);
        r.setVoiceCallForwardingFlag(line, enable, number);
    }

    public boolean getCallForwardingIndicator() {
        if (getPhoneType() == 2) {
            Rlog.e(LOG_TAG, "getCallForwardingIndicator: not possible in CDMA");
            return false;
        }
        boolean videoCallForwardingPreference;
        IccRecords r = (IccRecords) this.mIccRecords.get();
        int callForwardingIndicator = -1;
        if (r != null) {
            callForwardingIndicator = r.getVoiceCallForwardingFlag();
        }
        if (callForwardingIndicator == -1) {
            callForwardingIndicator = getCallForwardingIndicatorFromSharedPref();
        }
        Rlog.v(LOG_TAG, "getCallForwardingIndicator: iccForwardingFlag=" + (r != null ? Integer.valueOf(r.getVoiceCallForwardingFlag()) : "null") + ", sharedPrefFlag=" + getCallForwardingIndicatorFromSharedPref());
        if (callForwardingIndicator != 1) {
            videoCallForwardingPreference = getVideoCallForwardingPreference();
        } else {
            videoCallForwardingPreference = true;
        }
        return videoCallForwardingPreference;
    }

    public void setVideoCallForwardingPreference(boolean enabled) {
        Rlog.d(LOG_TAG, "Set video call forwarding info to preferences enabled = " + enabled);
        Editor edit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        edit.putBoolean(CF_ENABLED_VIDEO + getSubId(), enabled);
        edit.commit();
    }

    public boolean getVideoCallForwardingPreference() {
        Rlog.d(LOG_TAG, "Get video call forwarding info from preferences");
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getBoolean(CF_ENABLED_VIDEO + getSubId(), false);
    }

    public CarrierSignalAgent getCarrierSignalAgent() {
        return this.mCarrierSignalAgent;
    }

    public CarrierActionAgent getCarrierActionAgent() {
        return this.mCarrierActionAgent;
    }

    public void queryCdmaRoamingPreference(Message response) {
        this.mCi.queryCdmaRoamingPreference(response);
    }

    public SignalStrength getSignalStrength() {
        ServiceStateTracker sst = getServiceStateTracker();
        if (sst == null) {
            return new SignalStrength();
        }
        return sst.getSignalStrength();
    }

    public boolean isConcurrentVoiceAndDataAllowed() {
        ServiceStateTracker sst = getServiceStateTracker();
        return sst == null ? false : sst.isConcurrentVoiceAndDataAllowed();
    }

    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        this.mCi.setCdmaRoamingPreference(cdmaRoamingType, response);
    }

    public void setCdmaSubscription(int cdmaSubscriptionType, Message response) {
        this.mCi.setCdmaSubscriptionSource(cdmaSubscriptionType, response);
    }

    public void setPreferredNetworkType(int networkType, Message response) {
        int modemRaf = getRadioAccessFamily();
        int rafFromType = RadioAccessFamily.getRafFromNetworkType(networkType);
        if (modemRaf == 1 || rafFromType == 1) {
            Rlog.d(LOG_TAG, "setPreferredNetworkType: Abort, unknown RAF: " + modemRaf + " " + rafFromType);
            if (response != null) {
                AsyncResult.forMessage(response, null, new CommandException(Error.GENERIC_FAILURE));
                response.sendToTarget();
            }
            return;
        }
        int filteredType = RadioAccessFamily.getNetworkTypeFromRaf(rafFromType & modemRaf);
        Rlog.d(LOG_TAG, "setPreferredNetworkType: networkType = " + networkType + " modemRaf = " + modemRaf + " rafFromType = " + rafFromType + " filteredType = " + filteredType);
        this.mCi.setPreferredNetworkType(filteredType, response);
    }

    public void getPreferredNetworkType(Message response) {
        this.mCi.getPreferredNetworkType(response);
    }

    public void getSmscAddress(Message result) {
        this.mCi.getSmscAddress(result);
    }

    public void setSmscAddress(String address, Message result) {
        this.mCi.setSmscAddress(address, result);
    }

    public void setTTYMode(int ttyMode, Message onComplete) {
        this.mCi.setTTYMode(ttyMode, onComplete);
    }

    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        Rlog.d(LOG_TAG, "unexpected setUiTTYMode method call");
    }

    public void queryTTYMode(Message onComplete) {
        this.mCi.queryTTYMode(onComplete);
    }

    public void enableEnhancedVoicePrivacy(boolean enable, Message onComplete) {
    }

    public void getEnhancedVoicePrivacy(Message onComplete) {
    }

    public void setBandMode(int bandMode, Message response) {
        this.mCi.setBandMode(bandMode, response);
    }

    public void queryAvailableBandMode(Message response) {
        this.mCi.queryAvailableBandMode(response);
    }

    @Deprecated
    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        this.mCi.invokeOemRilRequestRaw(data, response);
    }

    @Deprecated
    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        this.mCi.invokeOemRilRequestStrings(strings, response);
    }

    public void nvReadItem(int itemID, Message response) {
        this.mCi.nvReadItem(itemID, response);
    }

    public void nvWriteItem(int itemID, String itemValue, Message response) {
        this.mCi.nvWriteItem(itemID, itemValue, response);
    }

    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message response) {
        this.mCi.nvWriteCdmaPrl(preferredRoamingList, response);
    }

    public void nvResetConfig(int resetType, Message response) {
        this.mCi.nvResetConfig(resetType, response);
    }

    public void notifyDataActivity() {
        this.mNotifier.notifyDataActivity(this);
    }

    private void notifyMessageWaitingIndicator() {
        if (this.mIsVoiceCapable) {
            this.mNotifier.notifyMessageWaitingChanged(this);
        }
    }

    public void notifyDataConnection(String reason, String apnType, DataState state) {
        this.mNotifier.notifyDataConnection(this, reason, apnType, state);
    }

    public void notifyDataConnection(String reason, String apnType) {
        this.mNotifier.notifyDataConnection(this, reason, apnType, getDataConnectionState(apnType));
    }

    public void notifyDataConnection(String reason) {
        for (String apnType : getActiveApnTypes()) {
            this.mNotifier.notifyDataConnection(this, reason, apnType, getDataConnectionState(apnType));
        }
    }

    public void notifyOtaspChanged(int otaspMode) {
        this.mNotifier.notifyOtaspChanged(this, otaspMode);
    }

    public void notifyVoiceActivationStateChanged(int state) {
        this.mNotifier.notifyVoiceActivationStateChanged(this, state);
    }

    public void notifyDataActivationStateChanged(int state) {
        this.mNotifier.notifyDataActivationStateChanged(this, state);
    }

    public void notifySignalStrength() {
        this.mNotifier.notifySignalStrength(this);
    }

    public void notifyCellInfo(List<CellInfo> cellInfo) {
        this.mNotifier.notifyCellInfo(this, privatizeCellInfoList(cellInfo));
    }

    public void notifyVoLteServiceStateChanged(VoLteServiceState lteState) {
        this.mNotifier.notifyVoLteServiceStateChanged(this, lteState);
    }

    public boolean isInEmergencyCall() {
        return false;
    }

    protected static boolean getInEcmMode() {
        return SystemProperties.getBoolean("ril.cdma.inecmmode", false);
    }

    public boolean isInEcm() {
        return this.mIsPhoneInEcmState;
    }

    public void setIsInEcm(boolean isInEcm) {
        setSystemProperty("ril.cdma.inecmmode", String.valueOf(isInEcm));
        this.mIsPhoneInEcmState = isInEcm;
    }

    private static int getVideoState(Call call) {
        Connection conn = call.getEarliestConnection();
        if (conn != null) {
            return conn.getVideoState();
        }
        return 0;
    }

    private boolean isVideoCallOrConference(Call call) {
        boolean z = true;
        if (call.isMultiparty()) {
            return true;
        }
        if (!(call instanceof ImsPhoneCall)) {
            return false;
        }
        ImsCall imsCall = ((ImsPhoneCall) call).getImsCall();
        if (imsCall == null) {
            z = false;
        } else if (!imsCall.isVideoCall()) {
            z = imsCall.wasVideoCall();
        }
        return z;
    }

    public boolean isImsVideoCallOrConferencePresent() {
        boolean isPresent = false;
        if (this.mImsPhone != null) {
            if (isVideoCallOrConference(this.mImsPhone.getForegroundCall()) || isVideoCallOrConference(this.mImsPhone.getBackgroundCall())) {
                isPresent = true;
            } else {
                isPresent = isVideoCallOrConference(this.mImsPhone.getRingingCall());
            }
        }
        Rlog.d(LOG_TAG, "isImsVideoCallOrConferencePresent: " + isPresent);
        return isPresent;
    }

    public int getVoiceMessageCount() {
        return this.mVmCount;
    }

    public void setVoiceMessageCount(int countWaiting) {
        this.mVmCount = countWaiting;
        int subId = getSubId();
        if (SubscriptionController.getInstance().isActiveSubId(subId)) {
            Rlog.d(LOG_TAG, "setVoiceMessageCount: Storing Voice Mail Count = " + countWaiting + " for mVmCountKey = " + VM_COUNT + subId + " in preferences.");
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
            editor.putInt(VM_COUNT + subId, countWaiting);
            editor.apply();
        } else {
            Rlog.e(LOG_TAG, "setVoiceMessageCount in sharedPreference: invalid subId " + subId);
        }
        notifyMessageWaitingIndicator();
    }

    protected int getStoredVoiceMessageCount() {
        int countVoiceMessages = 0;
        int subId = getSubId();
        if (SubscriptionController.getInstance().isActiveSubId(subId)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            int countFromSP = sp.getInt(VM_COUNT + subId, -2);
            if (countFromSP != -2) {
                countVoiceMessages = countFromSP;
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: from preference for subId " + subId + "= " + countFromSP);
                return countVoiceMessages;
            }
            String subscriberId = sp.getString(VM_ID, null);
            if (subscriberId == null) {
                return 0;
            }
            String currentSubscriberId = getSubscriberId();
            if (currentSubscriberId == null || !currentSubscriberId.equals(subscriberId)) {
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: returning 0 as count for matching subscriberId not found");
            } else {
                countVoiceMessages = sp.getInt(VM_COUNT, 0);
                setVoiceMessageCount(countVoiceMessages);
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: from preference = " + countVoiceMessages);
            }
            Editor editor = sp.edit();
            editor.remove(VM_ID);
            editor.remove(VM_COUNT);
            editor.apply();
            return countVoiceMessages;
        }
        Rlog.e(LOG_TAG, "getStoredVoiceMessageCount: invalid subId " + subId);
        return 0;
    }

    public void sendDialerSpecialCode(String code) {
        if (!TextUtils.isEmpty(code)) {
            Intent intent = new Intent("android.provider.Telephony.SECRET_CODE", Uri.parse("android_secret_code://" + code));
            intent.addFlags(QtiCallConstants.CAPABILITY_SUPPORTS_DOWNGRADE_TO_VOICE_REMOTE);
            this.mContext.sendBroadcast(intent);
        }
    }

    public int getCdmaEriIconIndex() {
        return -1;
    }

    public int getCdmaEriIconMode() {
        return -1;
    }

    public String getCdmaEriText() {
        return "GSM nw, no ERI";
    }

    public String getCdmaMin() {
        return null;
    }

    public boolean isMinInfoReady() {
        return false;
    }

    public String getCdmaPrlVersion() {
        return null;
    }

    public void addParticipant(String dialString) throws CallStateException {
        throw new CallStateException("addParticipant :: No-Op base implementation. " + this);
    }

    public void addParticipant(String dialString, Message onComplete) throws CallStateException {
        throw new CallStateException("addParticipant :: No-Op base implementation. " + this);
    }

    public void sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
    }

    public void setOnPostDialCharacter(Handler h, int what, Object obj) {
        this.mPostDialHandler = new Registrant(h, what, obj);
    }

    public Registrant getPostDialHandler() {
        return this.mPostDialHandler;
    }

    public void exitEmergencyCallbackMode() {
    }

    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj) {
    }

    public void unregisterForCdmaOtaStatusChange(Handler h) {
    }

    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
    }

    public void unregisterForSubscriptionInfoReady(Handler h) {
    }

    public boolean needsOtaServiceProvisioning() {
        return false;
    }

    public boolean isOtaSpNumber(String dialStr) {
        return false;
    }

    public void registerForCallWaiting(Handler h, int what, Object obj) {
    }

    public void unregisterForCallWaiting(Handler h) {
    }

    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
    }

    public void unregisterForEcmTimerReset(Handler h) {
    }

    public void registerForSignalInfo(Handler h, int what, Object obj) {
        this.mCi.registerForSignalInfo(h, what, obj);
    }

    public void unregisterForSignalInfo(Handler h) {
        this.mCi.unregisterForSignalInfo(h);
    }

    public void registerForDisplayInfo(Handler h, int what, Object obj) {
        this.mCi.registerForDisplayInfo(h, what, obj);
    }

    public void unregisterForDisplayInfo(Handler h) {
        this.mCi.unregisterForDisplayInfo(h);
    }

    public void registerForNumberInfo(Handler h, int what, Object obj) {
        this.mCi.registerForNumberInfo(h, what, obj);
    }

    public void unregisterForNumberInfo(Handler h) {
        this.mCi.unregisterForNumberInfo(h);
    }

    public void registerForRedirectedNumberInfo(Handler h, int what, Object obj) {
        this.mCi.registerForRedirectedNumberInfo(h, what, obj);
    }

    public void unregisterForRedirectedNumberInfo(Handler h) {
        this.mCi.unregisterForRedirectedNumberInfo(h);
    }

    public void registerForLineControlInfo(Handler h, int what, Object obj) {
        this.mCi.registerForLineControlInfo(h, what, obj);
    }

    public void unregisterForLineControlInfo(Handler h) {
        this.mCi.unregisterForLineControlInfo(h);
    }

    public void registerFoT53ClirlInfo(Handler h, int what, Object obj) {
        this.mCi.registerFoT53ClirlInfo(h, what, obj);
    }

    public void unregisterForT53ClirInfo(Handler h) {
        this.mCi.unregisterForT53ClirInfo(h);
    }

    public void registerForT53AudioControlInfo(Handler h, int what, Object obj) {
        this.mCi.registerForT53AudioControlInfo(h, what, obj);
    }

    public void unregisterForT53AudioControlInfo(Handler h) {
        this.mCi.unregisterForT53AudioControlInfo(h);
    }

    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
    }

    public void unsetOnEcbModeExitResponse(Handler h) {
    }

    public void registerForRadioOffOrNotAvailable(Handler h, int what, Object obj) {
        this.mRadioOffOrNotAvailableRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForRadioOffOrNotAvailable(Handler h) {
        this.mRadioOffOrNotAvailableRegistrants.remove(h);
    }

    public String[] getActiveApnTypes() {
        if (this.mDcTracker == null) {
            return null;
        }
        return this.mDcTracker.getActiveApnTypes();
    }

    public boolean hasMatchedTetherApnSetting() {
        return this.mDcTracker.hasMatchedTetherApnSetting();
    }

    public String getActiveApnHost(String apnType) {
        return this.mDcTracker.getActiveApnString(apnType);
    }

    public LinkProperties getLinkProperties(String apnType) {
        return this.mDcTracker.getLinkProperties(apnType);
    }

    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        return this.mDcTracker.getNetworkCapabilities(apnType);
    }

    public boolean isDataAllowed() {
        return this.mDcTracker != null ? this.mDcTracker.isDataAllowed(null) : false;
    }

    public boolean isDataAllowed(DataConnectionReasons reasons) {
        return this.mDcTracker != null ? this.mDcTracker.isDataAllowed(reasons) : false;
    }

    public void carrierActionSetMeteredApnsEnabled(boolean enabled) {
        this.mCarrierActionAgent.carrierActionSetMeteredApnsEnabled(enabled);
    }

    public void carrierActionSetRadioEnabled(boolean enabled) {
        this.mCarrierActionAgent.carrierActionSetRadioEnabled(enabled);
    }

    public void carrierActionReportDefaultNetworkStatus(boolean report) {
        this.mCarrierActionAgent.carrierActionReportDefaultNetworkStatus(report);
    }

    public void notifyNewRingingConnectionP(Connection cn) {
        if (this.mIsVoiceCapable) {
            this.mNewRingingConnectionRegistrants.notifyRegistrants(new AsyncResult(null, cn, null));
        }
    }

    public void notifyUnknownConnectionP(Connection cn) {
        this.mUnknownConnectionRegistrants.notifyResult(cn);
    }

    public void notifyForVideoCapabilityChanged(boolean isVideoCallCapable) {
        this.mIsVideoCapable = isVideoCallCapable;
        this.mVideoCapabilityChangedRegistrants.notifyRegistrants(new AsyncResult(null, Boolean.valueOf(isVideoCallCapable), null));
    }

    private void notifyIncomingRing() {
        if (this.mIsVoiceCapable) {
            this.mIncomingRingRegistrants.notifyRegistrants(new AsyncResult(null, this, null));
        }
    }

    private void sendIncomingCallRingNotification(int token) {
        if (this.mIsVoiceCapable && (this.mDoesRilSendMultipleCallRing ^ 1) != 0 && token == this.mCallRingContinueToken) {
            Rlog.d(LOG_TAG, "Sending notifyIncomingRing");
            notifyIncomingRing();
            sendMessageDelayed(obtainMessage(15, token, 0), (long) this.mCallRingDelay);
            return;
        }
        Rlog.d(LOG_TAG, "Ignoring ring notification request, mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing + " token=" + token + " mCallRingContinueToken=" + this.mCallRingContinueToken + " mIsVoiceCapable=" + this.mIsVoiceCapable);
    }

    public boolean isCspPlmnEnabled() {
        return false;
    }

    public IsimRecords getIsimRecords() {
        Rlog.e(LOG_TAG, "getIsimRecords() is only supported on LTE devices");
        return null;
    }

    public String getMsisdn() {
        return null;
    }

    public DataState getDataConnectionState() {
        return getDataConnectionState("default");
    }

    public void notifyCallForwardingIndicator() {
    }

    public void notifyDataConnectionFailed(String reason, String apnType) {
        this.mNotifier.notifyDataConnectionFailed(this, reason, apnType);
    }

    public void notifyPreciseDataConnectionFailed(String reason, String apnType, String apn, String failCause) {
        this.mNotifier.notifyPreciseDataConnectionFailed(this, reason, apnType, apn, failCause);
    }

    public int getLteOnCdmaMode() {
        return this.mCi.getLteOnCdmaMode();
    }

    public void setVoiceMessageWaiting(int line, int countWaiting) {
        Rlog.e(LOG_TAG, "Error! This function should never be executed, inactive Phone.");
    }

    public UsimServiceTable getUsimServiceTable() {
        IccRecords r = (IccRecords) this.mIccRecords.get();
        if (r != null) {
            return r.getUsimServiceTable();
        }
        return null;
    }

    public UiccCard getUiccCard() {
        return this.mUiccController.getUiccCard(this.mPhoneId);
    }

    public String[] getPcscfAddress(String apnType) {
        return this.mDcTracker.getPcscfAddress(apnType);
    }

    public void setImsRegistrationState(boolean registered) {
    }

    public Phone getImsPhone() {
        return this.mImsPhone;
    }

    public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int keyType) {
        return null;
    }

    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo) {
    }

    public boolean isUtEnabled() {
        if (this.mImsPhone != null) {
            return this.mImsPhone.isUtEnabled();
        }
        return false;
    }

    public void dispose() {
    }

    private void updateImsPhone() {
        Rlog.d(LOG_TAG, "updateImsPhone mImsServiceReady=" + this.mImsServiceReady);
        if (this.mImsServiceReady && this.mImsPhone == null) {
            this.mImsPhone = PhoneFactory.makeImsPhone(this.mNotifier, this);
            CallManager.getInstance().registerPhone(this.mImsPhone);
            this.mImsPhone.registerForSilentRedial(this, 32, null);
        } else if (!this.mImsServiceReady && this.mImsPhone != null) {
            CallManager.getInstance().unregisterPhone(this.mImsPhone);
            this.mImsPhone.unregisterForSilentRedial(this);
            this.mImsPhone.dispose();
            this.mImsPhone = null;
        }
    }

    protected Connection dialInternal(String dialString, UUSInfo uusInfo, int videoState, Bundle intentExtras) throws CallStateException {
        return null;
    }

    public int getSubId() {
        return SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhoneId);
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public int getVoicePhoneServiceState() {
        Phone imsPhone = this.mImsPhone;
        if (imsPhone == null || imsPhone.getServiceState().getState() != 0) {
            return getServiceState().getState();
        }
        return 0;
    }

    public boolean setOperatorBrandOverride(String brand) {
        return false;
    }

    public boolean setRoamingOverride(List<String> gsmRoamingList, List<String> gsmNonRoamingList, List<String> cdmaRoamingList, List<String> cdmaNonRoamingList) {
        String iccId = getIccSerialNumber();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }
        setRoamingOverrideHelper(gsmRoamingList, GSM_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        setRoamingOverrideHelper(gsmNonRoamingList, GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        setRoamingOverrideHelper(cdmaRoamingList, CDMA_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        setRoamingOverrideHelper(cdmaNonRoamingList, CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        ServiceStateTracker tracker = getServiceStateTracker();
        if (tracker != null) {
            tracker.pollState();
        }
        return true;
    }

    private void setRoamingOverrideHelper(List<String> list, String prefix, String iccId) {
        Editor spEditor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        String key = prefix + iccId;
        if (list == null || list.isEmpty()) {
            spEditor.remove(key).commit();
        } else {
            spEditor.putStringSet(key, new HashSet(list)).commit();
        }
    }

    public boolean isMccMncMarkedAsRoaming(String mccMnc) {
        return getRoamingOverrideHelper(GSM_ROAMING_LIST_OVERRIDE_PREFIX, mccMnc);
    }

    public boolean isMccMncMarkedAsNonRoaming(String mccMnc) {
        return getRoamingOverrideHelper(GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX, mccMnc);
    }

    public boolean isSidMarkedAsRoaming(int SID) {
        return getRoamingOverrideHelper(CDMA_ROAMING_LIST_OVERRIDE_PREFIX, Integer.toString(SID));
    }

    public boolean isSidMarkedAsNonRoaming(int SID) {
        return getRoamingOverrideHelper(CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX, Integer.toString(SID));
    }

    public boolean isImsRegistered() {
        Phone imsPhone = this.mImsPhone;
        boolean isImsRegistered = false;
        if (imsPhone != null) {
            isImsRegistered = imsPhone.isImsRegistered();
        } else {
            ServiceStateTracker sst = getServiceStateTracker();
            if (sst != null) {
                isImsRegistered = sst.isImsRegistered();
                if (isImsRegistered && sst.getCombinedRegState() != 0) {
                    isImsRegistered = false;
                }
            }
        }
        boolean enabled = true;
        if (this.mImsMgr != null) {
            enabled = this.mImsMgr.isEnhanced4gLteModeSettingEnabledByUserForSlot();
        }
        Rlog.d(LOG_TAG, "ENHANCED_4G_MODE_ENABLED =" + enabled);
        if (!enabled && isImsRegistered) {
            isImsRegistered = false;
        }
        Rlog.d(LOG_TAG, "isImsRegistered =" + isImsRegistered + " phoneid:" + this.mPhoneId);
        return isImsRegistered;
    }

    public boolean isWifiCallingEnabled() {
        Phone imsPhone = this.mImsPhone;
        boolean isWifiCallingEnabled = false;
        if (imsPhone != null) {
            isWifiCallingEnabled = imsPhone.isWifiCallingEnabled();
        }
        Rlog.d(LOG_TAG, "isWifiCallingEnabled =" + isWifiCallingEnabled);
        return isWifiCallingEnabled;
    }

    public boolean isVolteEnabled() {
        Phone imsPhone = this.mImsPhone;
        boolean isVolteEnabled = false;
        if (imsPhone != null) {
            isVolteEnabled = imsPhone.isVolteEnabled();
        }
        Rlog.d(LOG_TAG, "isImsRegistered =" + isVolteEnabled);
        return isVolteEnabled;
    }

    private boolean getRoamingOverrideHelper(String prefix, String key) {
        String iccId = getIccSerialNumber();
        if (TextUtils.isEmpty(iccId) || TextUtils.isEmpty(key)) {
            return false;
        }
        Set<String> value = PreferenceManager.getDefaultSharedPreferences(this.mContext).getStringSet(prefix + iccId, null);
        if (value == null) {
            return false;
        }
        return value.contains(key);
    }

    public boolean isRadioAvailable() {
        return this.mCi.getRadioState().isAvailable();
    }

    public boolean isRadioOn() {
        return this.mCi.getRadioState().isOn();
    }

    public void shutdownRadio() {
        getServiceStateTracker().requestShutdown();
    }

    public boolean isShuttingDown() {
        return getServiceStateTracker().isDeviceShuttingDown();
    }

    public void setRadioCapability(RadioCapability rc, Message response) {
        this.mCi.setRadioCapability(rc, response);
    }

    public int getRadioAccessFamily() {
        RadioCapability rc = getRadioCapability();
        return rc == null ? 1 : rc.getRadioAccessFamily();
    }

    public String getModemUuId() {
        RadioCapability rc = getRadioCapability();
        return rc == null ? SpnOverride.MVNO_TYPE_NONE : rc.getLogicalModemUuid();
    }

    public RadioCapability getRadioCapability() {
        return (RadioCapability) this.mRadioCapability.get();
    }

    public void radioCapabilityUpdated(RadioCapability rc) {
        this.mRadioCapability.set(rc);
        if (SubscriptionManager.isValidSubscriptionId(getSubId())) {
            sendSubscriptionSettings(true);
        }
    }

    public void sendSubscriptionSettings(boolean restoreNetworkSelection) {
        setPreferredNetworkType(PhoneFactory.calculatePreferredNetworkType(this.mContext, getSubId()), null);
        if (restoreNetworkSelection) {
            restoreSavedNetworkSelection(null);
        }
    }

    protected void setPreferredNetworkTypeIfSimLoaded() {
        if (SubscriptionManager.from(this.mContext).isActiveSubId(getSubId())) {
            setPreferredNetworkType(PhoneFactory.calculatePreferredNetworkType(this.mContext, getSubId()), null);
        }
    }

    public void registerForRadioCapabilityChanged(Handler h, int what, Object obj) {
        this.mCi.registerForRadioCapabilityChanged(h, what, obj);
    }

    public void unregisterForRadioCapabilityChanged(Handler h) {
        this.mCi.unregisterForRadioCapabilityChanged(this);
    }

    public boolean isImsUseEnabled() {
        if (this.mImsMgr == null) {
            return false;
        }
        boolean imsUseEnabled;
        if (this.mImsMgr.isVolteEnabledByPlatformForSlot() && this.mImsMgr.isEnhanced4gLteModeSettingEnabledByUserForSlot()) {
            imsUseEnabled = true;
        } else if (this.mImsMgr.isWfcEnabledByPlatformForSlot() && this.mImsMgr.isWfcEnabledByUserForSlot()) {
            imsUseEnabled = this.mImsMgr.isNonTtyOrTtyOnVolteEnabledForSlot();
        } else {
            imsUseEnabled = false;
        }
        return imsUseEnabled;
    }

    public boolean isImsAvailable() {
        if (this.mImsPhone == null) {
            return false;
        }
        return this.mImsPhone.isImsAvailable();
    }

    public boolean isVideoEnabled() {
        Phone imsPhone = this.mImsPhone;
        if (imsPhone != null) {
            return imsPhone.isVideoEnabled();
        }
        return false;
    }

    public int getLceStatus() {
        return this.mLceStatus;
    }

    public void getModemActivityInfo(Message response) {
        this.mCi.getModemActivityInfo(response);
    }

    public void startLceAfterRadioIsAvailable() {
        this.mCi.startLceService(200, true, obtainMessage(37));
    }

    public void setAllowedCarriers(List<CarrierIdentifier> carriers, Message response) {
        this.mCi.setAllowedCarriers(carriers, response);
    }

    public void getAllowedCarriers(Message response) {
        this.mCi.getAllowedCarriers(response);
    }

    public Locale getLocaleFromSimAndCarrierPrefs() {
        IccRecords records = (IccRecords) this.mIccRecords.get();
        if (records == null || records.getSimLanguage() == null) {
            return getLocaleFromCarrierProperties(this.mContext);
        }
        return new Locale(records.getSimLanguage());
    }

    public void updateDataConnectionTracker() {
        this.mDcTracker.update();
    }

    public void setInternalDataEnabled(boolean enable, Message onCompleteMsg) {
        this.mDcTracker.setInternalDataEnabled(enable, onCompleteMsg);
    }

    public boolean updateCurrentCarrierInProvider() {
        return false;
    }

    public void registerForAllDataDisconnected(Handler h, int what, Object obj) {
        this.mDcTracker.registerForAllDataDisconnected(h, what, obj);
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        this.mDcTracker.unregisterForAllDataDisconnected(h);
    }

    public void registerForDataEnabledChanged(Handler h, int what, Object obj) {
        this.mDcTracker.registerForDataEnabledChanged(h, what, obj);
    }

    public void unregisterForDataEnabledChanged(Handler h) {
        this.mDcTracker.unregisterForDataEnabledChanged(h);
    }

    public IccSmsInterfaceManager getIccSmsInterfaceManager() {
        return null;
    }

    protected boolean isMatchGid(String gid) {
        String gid1 = getGroupIdLevel1();
        int gidLength = gid.length();
        if (TextUtils.isEmpty(gid1) || gid1.length() < gidLength || !gid1.substring(0, gidLength).equalsIgnoreCase(gid)) {
            return false;
        }
        return true;
    }

    public void checkWfcWifiOnlyModeBeforeDial() throws CallStateException {
        if ((this.mImsPhone == null || (isWifiCallingEnabled() ^ 1) != 0) && this.mImsMgr != null) {
            boolean wfcWiFiOnly = (this.mImsMgr.isWfcEnabledByPlatformForSlot() && this.mImsMgr.isWfcEnabledByUserForSlot()) ? this.mImsMgr.getWfcModeForSlot() == 0 : false;
            if (wfcWiFiOnly) {
                throw new CallStateException(1, "WFC Wi-Fi Only Mode: IMS not registered");
            }
        }
    }

    public void startRingbackTone() {
    }

    public void stopRingbackTone() {
    }

    public void callEndCleanupHandOverCallIfAny() {
    }

    public void cancelUSSD() {
    }

    public String getOperatorNumeric() {
        return SpnOverride.MVNO_TYPE_NONE;
    }

    public Phone getDefaultPhone() {
        return this;
    }

    public NetworkStats getVtDataUsage(boolean perUidStats) {
        if (this.mImsPhone == null) {
            return null;
        }
        return this.mImsPhone.getVtDataUsage(perUidStats);
    }

    public void setPolicyDataEnabled(boolean enabled) {
        this.mDcTracker.setPolicyDataEnabled(enabled);
    }

    public Uri[] getCurrentSubscriberUris() {
        return null;
    }

    public AppSmsManager getAppSmsManager() {
        return this.mAppSmsManager;
    }

    public void setSimPowerState(int state) {
        this.mCi.setSimCardPower(state, null);
    }

    public void getCallForwardingOption(int commandInterfaceCFReason, int commandInterfaceServiceClass, Message onComplete) {
    }

    public void setCallForwardingOption(int commandInterfaceCFReason, int commandInterfaceCFAction, String dialingNumber, int commandInterfaceServiceClass, int timerSeconds, Message onComplete) {
    }

    public SIMRecords getSIMRecords() {
        return null;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Phone: subId=" + getSubId());
        pw.println(" mPhoneId=" + this.mPhoneId);
        pw.println(" mCi=" + this.mCi);
        pw.println(" mDnsCheckDisabled=" + this.mDnsCheckDisabled);
        pw.println(" mDcTracker=" + this.mDcTracker);
        pw.println(" mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing);
        pw.println(" mCallRingContinueToken=" + this.mCallRingContinueToken);
        pw.println(" mCallRingDelay=" + this.mCallRingDelay);
        pw.println(" mIsVoiceCapable=" + this.mIsVoiceCapable);
        pw.println(" mIccRecords=" + this.mIccRecords.get());
        pw.println(" mUiccApplication=" + this.mUiccApplication.get());
        pw.println(" mSmsStorageMonitor=" + this.mSmsStorageMonitor);
        pw.println(" mSmsUsageMonitor=" + this.mSmsUsageMonitor);
        pw.flush();
        pw.println(" mLooper=" + this.mLooper);
        pw.println(" mContext=" + this.mContext);
        pw.println(" mNotifier=" + this.mNotifier);
        pw.println(" mSimulatedRadioControl=" + this.mSimulatedRadioControl);
        pw.println(" mUnitTestMode=" + this.mUnitTestMode);
        pw.println(" isDnsCheckDisabled()=" + isDnsCheckDisabled());
        pw.println(" getUnitTestMode()=" + getUnitTestMode());
        pw.println(" getState()=" + getState());
        pw.println(" getIccSerialNumber()=" + getIccSerialNumber());
        pw.println(" getIccRecordsLoaded()=" + getIccRecordsLoaded());
        pw.println(" getMessageWaitingIndicator()=" + getMessageWaitingIndicator());
        pw.println(" getCallForwardingIndicator()=" + getCallForwardingIndicator());
        pw.println(" isInEmergencyCall()=" + isInEmergencyCall());
        pw.flush();
        pw.println(" isInEcm()=" + isInEcm());
        pw.println(" getPhoneName()=" + getPhoneName());
        pw.println(" getPhoneType()=" + getPhoneType());
        pw.println(" getVoiceMessageCount()=" + getVoiceMessageCount());
        pw.println(" getActiveApnTypes()=" + getActiveApnTypes());
        pw.println(" needsOtaServiceProvisioning=" + needsOtaServiceProvisioning());
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        if (this.mImsPhone != null) {
            try {
                this.mImsPhone.dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mDcTracker != null) {
            try {
                this.mDcTracker.dump(fd, pw, args);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (getServiceStateTracker() != null) {
            try {
                getServiceStateTracker().dump(fd, pw, args);
            } catch (Exception e22) {
                e22.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mCarrierActionAgent != null) {
            try {
                this.mCarrierActionAgent.dump(fd, pw, args);
            } catch (Exception e222) {
                e222.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mCarrierSignalAgent != null) {
            try {
                this.mCarrierSignalAgent.dump(fd, pw, args);
            } catch (Exception e2222) {
                e2222.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (getCallTracker() != null) {
            try {
                getCallTracker().dump(fd, pw, args);
            } catch (Exception e22222) {
                e22222.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mSimActivationTracker != null) {
            try {
                this.mSimActivationTracker.dump(fd, pw, args);
            } catch (Exception e222222) {
                e222222.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mCi != null && (this.mCi instanceof RIL)) {
            try {
                ((RIL) this.mCi).dump(fd, pw, args);
            } catch (Exception e2222222) {
                e2222222.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
    }

    public void getBandMode(Message response) {
        this.mCi.getBandMode(response);
    }

    public void setEngineerModeNvProcessCmd(int processcmd) {
        Rlog.d(LOG_TAG, "setEngineerModeNvProcessCmd cmd = " + processcmd);
        OppoNvFactory opponvfacty = new OppoNvFactory(this.mCi);
        switch (processcmd) {
            case 1:
                opponvfacty.onDynamicNvAutoCheck();
                break;
            case 2:
                opponvfacty.onDynamicNvCheck();
                break;
            case 3:
                opponvfacty.onDynamicNvRestore();
                break;
            case 4:
                opponvfacty.onDynamicNvBackup();
                break;
            case 5:
                opponvfacty.onStaticNvAutoCheck();
                break;
            case 6:
                opponvfacty.onStaticNvCheck();
                break;
            case 7:
                opponvfacty.onStaticNvRestore();
                break;
            case 8:
                opponvfacty.onStaticNvBackup();
                break;
            case 9:
                opponvfacty.onLteNvChange();
                break;
            case 20:
                break;
            case 21:
                opponvfacty.cancelCracked();
                break;
            case 22:
                opponvfacty.goCracked();
                break;
            default:
                return;
        }
        opponvfacty.hasCracked();
    }

    public void changeBarringPassword(String facility, String oldPwd, String newPwd, Message onComplete) {
        this.mCi.changeBarringPassword(facility, oldPwd, newPwd, onComplete);
    }

    public void setFactoryModeModemGPIO(int status, int num, Message response) {
        this.mCi.setFactoryModeModemGPIO(status, num, response);
    }

    public void getPreferedOperatorList(Message onComplete) {
    }

    public void setPOLEntry(NetworkInfoWithAcT networkWithAct, Message onComplete) {
    }

    public void OppoGetRffeDevInfo(int rf_tech, Message response) {
        this.mCi.OppoGetRffeDevInfo(rf_tech, response);
    }

    protected String getOEMImei(String mImei) {
        String imei = OEM_IMEI[getPhoneId()];
        if (TextUtils.isEmpty(imei)) {
            return mImei;
        }
        return imei;
    }

    protected void getImeiAgainIfFailure() {
        if (this.mCatchImeiFail < 3) {
            int delays = (this.mCatchImeiFail * GET_IMEI_INTERVAL) + 1;
            sendMessageDelayed(obtainMessage(1), (long) delays);
            this.mCatchImeiFail++;
            Rlog.d(LOG_TAG, "GET_DEVICE_IDENTITY[" + this.mCatchImeiFail + "]:Fail," + delays + "s get again,");
        }
    }

    protected void saveImeiAndMeid(String mMeid, String mImei) {
        int phoneId = getPhoneId();
        if (!(TextUtils.isEmpty(mImei) || ("00000000".equals(mImei) ^ 1) == 0 || !TextUtils.isEmpty(OEM_IMEI[phoneId]))) {
            OEM_IMEI[phoneId] = mImei;
        }
        if (!TextUtils.isEmpty(mMeid) && mMeid.toUpperCase().startsWith("A") && TextUtils.isEmpty(OEM_MEID)) {
            OEM_MEID = mMeid;
        }
        if (DBG) {
            Rlog.d(LOG_TAG + phoneId + " " + this.mPhoneId, "saveImeiAndMeid00:" + mMeid + ";" + mImei + ";" + OEM_MEID + ";" + OEM_IMEI[phoneId]);
        }
    }

    public void setModemCrash(Message response) {
        this.mCi.setModemCrash(response);
    }

    public String getModemVersion() {
        this.mCi.OppoGetMdmBaseBand(obtainMessage(999));
        return this.mMdmVersion;
    }

    public void registerForLteCAState(Handler h, int what, Object obj) {
        this.mCi.registerForLteCAState(h, what, obj);
    }

    public void unregisterForLteCAState(Handler h) {
        this.mCi.unregisterForLteCAState(h);
    }

    public void OppoSetTddLte(int status, Message response) {
        this.mCi.OppoSetTddLte(status, response);
    }

    public void oppoSetFilterArfcn(int arfcn1, int arfcn2, Message result) {
        this.mCi.oppoSetFilterArfcn(arfcn1, arfcn2, result);
    }

    public void oppoLockGSMArfcn(int arfcn1, Message result) {
        this.mCi.oppoLockGSMArfcn(arfcn1, result);
    }

    public void oppoLockLteCell(int arfcn, int pci, Message result) {
        this.mCi.oppoLockLteCell(arfcn, pci, result);
    }

    public void oppoGetEsnChangeFlag(Message result) {
        this.mCi.oppoGetEsnChangeFlag(result);
    }

    public synchronized void oppoUpdatePplmnList(byte[] value, Message response) {
        this.mCi.oppoUpdatePplmnList(value, response);
    }

    public void oppoUpdateFakeBsWeight(int[] values, Message response) {
        this.mCi.oppoUpdateFakeBsWeight(values, response);
    }

    public void oppoUpdateVolteFr2(int flags, int rsrp_thresh, int fr2_rsrp, int rsrp_adj, Message response) {
        this.mCi.oppoUpdateVolteFr2(flags, rsrp_thresh, fr2_rsrp, rsrp_adj, response);
    }

    public void oppoCtlModemFeature(int[] values, Message response) {
        this.mCi.oppoCtlModemFeature(values, response);
    }

    public void oppoNoticeUpdateVolteFr(int flags, Message response) {
        this.mCi.oppoNoticeUpdateVolteFr(flags, response);
    }

    public boolean is_test_card() {
        if (this.mIccRecords == null) {
            return false;
        }
        IccRecords mRecords = (IccRecords) this.mIccRecords.get();
        if (mRecords != null) {
            return mRecords.is_test_card();
        }
        return false;
    }

    public String[] getLteCdmaImsi(int phoneid) {
        String[] ImsiList = new String[]{SpnOverride.MVNO_TYPE_NONE, SpnOverride.MVNO_TYPE_NONE};
        SIMRecords sIMRecords = null;
        RuimRecords ruimRecords = null;
        if (this.mUiccController == null) {
            Rlog.d(LOG_TAG, "getLteCdmaImsi mUiccController == null");
            return ImsiList;
        }
        UiccCardApplication newUiccApplication = this.mUiccController.getUiccCardApplication(phoneid, 1);
        UiccCardApplication newUiccApplication2 = this.mUiccController.getUiccCardApplication(phoneid, 2);
        if (newUiccApplication != null) {
            sIMRecords = (SIMRecords) newUiccApplication.getIccRecords();
        }
        if (newUiccApplication2 != null) {
            ruimRecords = (RuimRecords) newUiccApplication2.getIccRecords();
        }
        if (!(ruimRecords == null || ruimRecords.getIMSI() == null)) {
            ImsiList[0] = ruimRecords.getIMSI();
            if (DBG) {
                Rlog.d(LOG_TAG, "getLteCdmaImsi ruim imsi = " + ImsiList[0]);
            }
        }
        if (!(sIMRecords == null || sIMRecords.getIMSI() == null)) {
            ImsiList[1] = sIMRecords.getIMSI();
            if (DBG) {
                Rlog.d(LOG_TAG, "getLteCdmaImsi sim imsi = " + ImsiList[1]);
            }
        }
        if (DBG) {
            Rlog.d(LOG_TAG, "getLteCdmaImsi ImsiList = " + ImsiList);
        }
        return ImsiList;
    }

    public String colorGetIccCardType() {
        return null;
    }

    public boolean getOemAutoAnswer() {
        boolean defaultVal = OemConstant.isNwLabTest();
        boolean isAuto = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(OEM_ISAUTO_ANSWER, defaultVal ^ 1);
        Rlog.d("oem", "getOemAutoAnswer isAuto:" + isAuto + ", defaultVal " + defaultVal);
        return isAuto;
    }

    public void setOemAutoAnswer(boolean isAuto) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        editor.putBoolean(OEM_ISAUTO_ANSWER, isAuto);
        editor.apply();
    }

    public void oemMigrateFrom() {
        if (this.mImsPhone != null) {
            oemMigrate(this.mHandoverRegistrants, this.mImsPhone.mHandoverRegistrants);
            CallTracker ct = this.mImsPhone.getCallTracker();
            if (ct != null) {
                ct.setOemStates(State.IDLE);
            }
        }
    }

    public void oemMigrate(RegistrantList to, RegistrantList from) {
        from.removeCleared();
        int n = from.size();
        for (int i = 0; i < n; i++) {
            Registrant r = (Registrant) from.get(i);
            Message msg = r.messageForRegistrant();
            if (msg == null) {
                Rlog.d(LOG_TAG, "msg is null");
            } else if (msg.obj != CallManager.getInstance().getRegistrantIdentifier()) {
                boolean issame = false;
                int k = to.size();
                for (int j = 0; j < k; j++) {
                    if (((Registrant) to.get(j)) == r) {
                        issame = true;
                        break;
                    }
                }
                Rlog.d(LOG_TAG, "leon oemMigrate:" + issame);
                if (!issame) {
                    to.add(r);
                }
            }
        }
    }

    public String getOemSpn() {
        return getServiceStateTracker().getOemSpn();
    }

    public void oppoSetSarRfState(int state) {
    }

    public void oppoSetSarRfStateV2(int state) {
        int sar_rf_state;
        switch (state) {
            case 0:
                sar_rf_state = 1;
                break;
            case 1:
                sar_rf_state = 2;
                break;
            case 2:
                sar_rf_state = 1;
                break;
            case 3:
                sar_rf_state = 4;
                break;
            case 4:
                sar_rf_state = 3;
                break;
            case 5:
                sar_rf_state = 6;
                break;
            case 6:
                sar_rf_state = 5;
                break;
            case 7:
                sar_rf_state = 8;
                break;
            case 8:
                sar_rf_state = 7;
                break;
            default:
                sar_rf_state = 1;
                break;
        }
        this.mCi.setFactoryModeModemGPIO(5, sar_rf_state, null);
        Rlog.d(LOG_TAG, "oppoSetSarRfStateV2() state : " + state + " sar_rf_state : " + sar_rf_state);
    }

    public void oppoGetBatteryCoverStatus(Message response) {
        this.mCi.setFactoryModeModemGPIO(12, 0, response);
    }

    public void oppoSetTunerLogic(int set_num, Message response) {
        this.mCi.setFactoryModeModemGPIO(13, set_num, response);
        Rlog.d(LOG_TAG, "oppoSetTunerLogic() set_num : " + set_num);
    }

    public void oppoGetTxRxInfo(int sys_mode, Message response) {
        this.mCi.oppoGetTxRxInfo(sys_mode, response);
    }

    public void oppoRffeCmd(int[] rffe_params, Message response) {
        this.mCi.oppoRffeCmd(rffe_params, response);
    }

    public void oppoSetSimType(int sim_sype) {
        this.mCi.setFactoryModeModemGPIO(14, sim_sype, null);
        Rlog.d(LOG_TAG, "oppoSetSimType() sim_sype: " + sim_sype);
    }

    public void oppoGetRadioInfo(Message response) {
    }

    public void notifySuppService(SuppServiceNotification supp) {
    }

    public String[] getPnnFromEons(String plmn, boolean longName) {
        return null;
    }

    public String getMvnoPattern(String type) {
        return null;
    }

    public boolean getOppoNeedNotifyStatus() {
        Rlog.d(LOG_TAG, " mPhoneId:  " + this.mPhoneId + "  GetOppoNeedNotifyStatus:  " + this.mOppoNeedNotify);
        return this.mOppoNeedNotify;
    }

    public void setOppoNeedNotifyStatus(boolean status) {
        Rlog.d(LOG_TAG, " mPhoneId:  " + this.mPhoneId + "  SetOppoNeedNotifyStatus:  " + status);
        this.mOppoNeedNotify = status;
    }

    public boolean isSRVCC() {
        return this.mSrvccState == SrvccState.STARTED;
    }

    public void setPeningSRVCC(boolean bl) {
        this.mIsPendingSRVCC = bl;
    }

    public boolean hasHoRegistrants() {
        return this.mHandoverRegistrants.size() > 0;
    }

    public DeviceStateMonitor getDeviceStateMonitor() {
        return this.mDeviceStateMonitor;
    }

    public void startMobileDataHongbaoPolicy(int time1, int time2, String value1, String value2) {
    }

    public void updateFreqHopEnable(boolean enable) {
        Rlog.d(LOG_TAG, "updateFreqHopEnable: enable = " + enable);
        if (enable) {
            SystemProperties.set("persist.sys.qcom_freqhop", "1");
        } else {
            SystemProperties.set("persist.sys.qcom_freqhop", "0");
        }
        this.mDeviceStateMonitor.initFreqHopEnable();
    }

    public void regFreqHopInd(int enable) {
        this.mCi.setFactoryModeModemGPIO(22, enable, null);
        Rlog.d(LOG_TAG, "regFreqHopInd() :  " + enable);
    }
}
