package com.android.internal.telephony;

public class PhoneConstants {
    public static final String ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED = "android.intent.action.SUBSCRIPTION_PHONE_STATE";
    public static final int APN_ALREADY_ACTIVE = 0;
    public static final int APN_ALREADY_INACTIVE = 4;
    public static final int APN_REQUEST_FAILED = 3;
    public static final int APN_REQUEST_STARTED = 1;
    public static final String[] APN_TYPES = new String[]{APN_TYPE_DEFAULT, APN_TYPE_MMS, APN_TYPE_SUPL, APN_TYPE_DUN, APN_TYPE_HIPRI, APN_TYPE_FOTA, APN_TYPE_IMS, APN_TYPE_CBS, APN_TYPE_IA, APN_TYPE_EMERGENCY};
    public static final String APN_TYPE_ALL = "*";
    public static final String APN_TYPE_CBS = "cbs";
    public static final String APN_TYPE_DEFAULT = "default";
    public static final String APN_TYPE_DUN = "dun";
    public static final String APN_TYPE_EMERGENCY = "emergency";
    public static final String APN_TYPE_FOTA = "fota";
    public static final String APN_TYPE_HIPRI = "hipri";
    public static final String APN_TYPE_IA = "ia";
    public static final String APN_TYPE_IMS = "ims";
    public static final String APN_TYPE_MMS = "mms";
    public static final int APN_TYPE_NOT_AVAILABLE = 2;
    public static final String APN_TYPE_SUPL = "supl";
    public static final int APPTYPE_CSIM = 4;
    public static final int APPTYPE_ISIM = 5;
    public static final int APPTYPE_RUIM = 3;
    public static final int APPTYPE_SIM = 1;
    public static final int APPTYPE_UNKNOWN = 0;
    public static final int APPTYPE_USIM = 2;
    public static final int AUDIO_OUTPUT_DEFAULT = 0;
    public static final int AUDIO_OUTPUT_DISABLE_SPEAKER = 1;
    public static final int AUDIO_OUTPUT_ENABLE_SPEAKER = 0;
    public static final int AUTH_CONTEXT_EAP_AKA = 129;
    public static final int AUTH_CONTEXT_EAP_SIM = 128;
    public static final int AUTH_CONTEXT_UNDEFINED = -1;
    public static final int CAPABILITY_3G = 1;
    public static final int CELL_OFF_DUE_TO_AIRPLANE_MODE_FLAG = 2;
    public static final int CELL_OFF_FLAG = 0;
    public static final int CELL_ON_FLAG = 1;
    public static final String COLOR_INT_SUBID = "color_int_subId";
    public static final String DATA_APN_KEY = "apn";
    public static final String DATA_APN_TYPE_KEY = "apnType";
    public static final String DATA_FAILURE_CAUSE_KEY = "failCause";
    public static final String DATA_IFACE_NAME_KEY = "iface";
    public static final String DATA_LINK_PROPERTIES_KEY = "linkProperties";
    public static final String DATA_NETWORK_CAPABILITIES_KEY = "networkCapabilities";
    public static final String DATA_NETWORK_ROAMING_KEY = "networkRoaming";
    public static final String DATA_NETWORK_TYPE_KEY = "networkType";
    public static final int DEFAULT_CARD_INDEX = 0;
    public static final String FAILURE_REASON_KEY = "reason";
    public static final int LTE_ON_CDMA_FALSE = 0;
    public static final int LTE_ON_CDMA_TRUE = 1;
    public static final int LTE_ON_CDMA_UNKNOWN = -1;
    public static final int MAX_PHONE_COUNT_DUAL_SIM = 2;
    public static final int MAX_PHONE_COUNT_SINGLE_SIM = 1;
    public static final int MAX_PHONE_COUNT_TRI_SIM = 3;
    public static final String NETWORK_UNAVAILABLE_KEY = "networkUnvailable";
    public static final String PHONE_IN_ECM_STATE = "phoneinECMState";
    public static final String PHONE_IN_EMERGENCY_CALL = "phoneInEmergencyCall";
    public static final String PHONE_KEY = "phone";
    public static final String PHONE_NAME_KEY = "phoneName";
    public static final int PHONE_TYPE_CDMA = 2;
    public static final int PHONE_TYPE_CDMA_LTE = 6;
    public static final int PHONE_TYPE_GSM = 1;
    public static final int PHONE_TYPE_IMS = 5;
    public static final int PHONE_TYPE_NONE = 0;
    public static final int PHONE_TYPE_SIP = 3;
    public static final int PHONE_TYPE_THIRD_PARTY = 4;
    public static final int PIN_GENERAL_FAILURE = 2;
    public static final int PIN_PASSWORD_INCORRECT = 1;
    public static final int PIN_RESULT_SUCCESS = 0;
    public static final int PRESENTATION_ALLOWED = 1;
    public static final int PRESENTATION_PAYPHONE = 4;
    public static final int PRESENTATION_RESTRICTED = 2;
    public static final int PRESENTATION_UNKNOWN = 3;
    public static final String REASON_LINK_PROPERTIES_CHANGED = "linkPropertiesChanged";
    public static final int RIL_CARD_MAX_APPS = 8;
    public static final int SIM_ACTIVATION_TYPE_DATA = 1;
    public static final int SIM_ACTIVATION_TYPE_VOICE = 0;
    public static final int SIM_ID_1 = 0;
    public static final int SIM_ID_2 = 1;
    public static final int SIM_ID_3 = 2;
    public static final int SIM_ID_4 = 3;
    public static final String SLOT_KEY = "slot";
    public static final String STATE_CHANGE_REASON_KEY = "reason";
    public static final String STATE_KEY = "state";
    public static final int SUB1 = 0;
    public static final int SUB2 = 1;
    public static final int SUB3 = 2;
    public static final String SUBSCRIPTION_KEY = "subscription";
    public static final String SUB_SETTING = "subSettings";
    public static final int UNSET_MTU = 0;

    public enum CardUnavailableReason {
        REASON_CARD_REMOVED,
        REASON_RADIO_UNAVAILABLE,
        REASON_SIM_REFRESH_RESET
    }

    public enum DataState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
        SUSPENDED
    }

    public enum State {
        IDLE,
        RINGING,
        OFFHOOK
    }
}
