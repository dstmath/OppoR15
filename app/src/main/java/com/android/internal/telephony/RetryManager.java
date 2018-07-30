package com.android.internal.telephony;

import android.os.Build;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.SpnOverride;
import java.util.ArrayList;
import java.util.Random;

public class RetryManager {
    public static final boolean DBG = true;
    private static final long DEFAULT_APN_RETRY_AFTER_DISCONNECT_DELAY = 10000;
    private static final String DEFAULT_DATA_RETRY_CONFIG = "max_retries=3, 5000, 5000, 5000";
    private static final long DEFAULT_INTER_APN_DELAY = 3000;
    private static final long DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING = 3000;
    public static final String LOG_TAG = "RetryManager";
    private static final int MAX_SAME_APN_RETRY = 3;
    public static final long NO_RETRY = -1;
    public static final long NO_SUGGESTED_RETRY_DELAY = -2;
    private static final long OPPO_DEFAULT_INTER_APN_DELAY = 5000;
    private static final String OTHERS_APN_TYPE = "others";
    public static final boolean VDBG = false;
    private long mApnRetryAfterDisconnectDelay;
    private String mApnType;
    private String mConfig;
    private int mCurrentApnIndex = -1;
    private long mFailFastInterApnDelay;
    private long mInterApnDelay;
    private int mMaxRetryCount;
    private long mModemSuggestedDelay = -2;
    private Phone mPhone;
    private ArrayList<RetryRec> mRetryArray = new ArrayList();
    private int mRetryCount = 0;
    private boolean mRetryForever = false;
    private Random mRng = new Random();
    private int mSameApnRetryCount = 0;
    private ArrayList<ApnSetting> mWaitingApns = null;

    private static class RetryRec {
        int mDelayTime;
        int mRandomizationTime;

        RetryRec(int delayTime, int randomizationTime) {
            this.mDelayTime = delayTime;
            this.mRandomizationTime = randomizationTime;
        }
    }

    public RetryManager(Phone phone, String apnType) {
        this.mPhone = phone;
        this.mApnType = apnType;
    }

    private boolean configure(String configStr) {
        if (configStr.startsWith("\"") && configStr.endsWith("\"")) {
            configStr = configStr.substring(1, configStr.length() - 1);
        }
        reset();
        log("configure: '" + configStr + "'");
        this.mConfig = configStr;
        if (TextUtils.isEmpty(configStr)) {
            log("configure: cleared");
        } else {
            int defaultRandomization = 0;
            String[] strArray = configStr.split(",");
            for (int i = 0; i < strArray.length; i++) {
                String[] splitStr = strArray[i].split("=", 2);
                splitStr[0] = splitStr[0].trim();
                Pair<Boolean, Integer> value;
                if (splitStr.length > 1) {
                    splitStr[1] = splitStr[1].trim();
                    if (TextUtils.equals(splitStr[0], "default_randomization")) {
                        value = parseNonNegativeInt(splitStr[0], splitStr[1]);
                        if (!((Boolean) value.first).booleanValue()) {
                            return false;
                        }
                        defaultRandomization = ((Integer) value.second).intValue();
                    } else if (!TextUtils.equals(splitStr[0], "max_retries")) {
                        Rlog.e(LOG_TAG, "Unrecognized configuration name value pair: " + strArray[i]);
                        return false;
                    } else if (TextUtils.equals("infinite", splitStr[1])) {
                        this.mRetryForever = true;
                    } else {
                        value = parseNonNegativeInt(splitStr[0], splitStr[1]);
                        if (!((Boolean) value.first).booleanValue()) {
                            return false;
                        }
                        this.mMaxRetryCount = ((Integer) value.second).intValue();
                    }
                } else {
                    splitStr = strArray[i].split(":", 2);
                    splitStr[0] = splitStr[0].trim();
                    RetryRec rr = new RetryRec(0, 0);
                    value = parseNonNegativeInt("delayTime", splitStr[0]);
                    if (!((Boolean) value.first).booleanValue()) {
                        return false;
                    }
                    rr.mDelayTime = ((Integer) value.second).intValue();
                    if (splitStr.length > 1) {
                        splitStr[1] = splitStr[1].trim();
                        value = parseNonNegativeInt("randomizationTime", splitStr[1]);
                        if (!((Boolean) value.first).booleanValue()) {
                            return false;
                        }
                        rr.mRandomizationTime = ((Integer) value.second).intValue();
                    } else {
                        rr.mRandomizationTime = defaultRandomization;
                    }
                    this.mRetryArray.add(rr);
                }
            }
            if (this.mRetryArray.size() > this.mMaxRetryCount) {
                this.mMaxRetryCount = this.mRetryArray.size();
            }
        }
        return true;
    }

    private void configureRetry() {
        String configString = null;
        String str = null;
        try {
            if (Build.IS_DEBUGGABLE) {
                String config = SystemProperties.get("test.data_retry_config");
                if (!TextUtils.isEmpty(config)) {
                    configure(config);
                    return;
                }
            }
            PersistableBundle b = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId());
            this.mInterApnDelay = OPPO_DEFAULT_INTER_APN_DELAY;
            this.mFailFastInterApnDelay = b.getLong("carrier_data_call_apn_delay_faster_long", 3000);
            this.mApnRetryAfterDisconnectDelay = b.getLong("carrier_data_call_apn_retry_after_disconnect_long", DEFAULT_APN_RETRY_AFTER_DISCONNECT_DELAY);
            String[] allConfigStrings = b.getStringArray("carrier_data_call_retry_config_strings");
            if (allConfigStrings != null) {
                for (String s : allConfigStrings) {
                    if (!TextUtils.isEmpty(s)) {
                        String[] splitStr = s.split(":", 2);
                        if (splitStr.length == 2) {
                            String apnType = splitStr[0].trim();
                            if (apnType.equals(this.mApnType)) {
                                configString = splitStr[1];
                                break;
                            } else if (apnType.equals(OTHERS_APN_TYPE)) {
                                str = splitStr[1];
                            }
                        } else {
                            continue;
                        }
                    }
                }
            }
            if (configString == null) {
                if (str != null) {
                    configString = str;
                } else {
                    log("Invalid APN retry configuration!. Use the default one now.");
                    configString = DEFAULT_DATA_RETRY_CONFIG;
                }
            }
        } catch (NullPointerException e) {
            log("Failed to read configuration! Use the hardcoded default value.");
            this.mInterApnDelay = 3000;
            this.mFailFastInterApnDelay = 3000;
            configString = DEFAULT_DATA_RETRY_CONFIG;
        }
        configure(configString);
    }

    private int getRetryTimer() {
        int index;
        int retVal;
        if (this.mRetryCount < this.mRetryArray.size()) {
            index = this.mRetryCount;
        } else {
            index = this.mRetryArray.size() - 1;
        }
        if (index < 0 || index >= this.mRetryArray.size()) {
            retVal = 0;
        } else {
            retVal = ((RetryRec) this.mRetryArray.get(index)).mDelayTime + nextRandomizationTime(index);
        }
        log("getRetryTimer: " + retVal);
        return retVal;
    }

    private Pair<Boolean, Integer> parseNonNegativeInt(String name, String stringValue) {
        try {
            int value = Integer.parseInt(stringValue);
            return new Pair(Boolean.valueOf(validateNonNegativeInt(name, value)), Integer.valueOf(value));
        } catch (NumberFormatException e) {
            Rlog.e(LOG_TAG, name + " bad value: " + stringValue, e);
            return new Pair(Boolean.valueOf(false), Integer.valueOf(0));
        }
    }

    private boolean validateNonNegativeInt(String name, int value) {
        if (value >= 0) {
            return true;
        }
        Rlog.e(LOG_TAG, name + " bad value: is < 0");
        return false;
    }

    private int nextRandomizationTime(int index) {
        int randomTime = ((RetryRec) this.mRetryArray.get(index)).mRandomizationTime;
        if (randomTime == 0) {
            return 0;
        }
        return this.mRng.nextInt(randomTime);
    }

    public ApnSetting getNextApnSetting() {
        if (this.mWaitingApns == null || this.mWaitingApns.size() == 0) {
            log("Waiting APN list is null or empty.");
            return null;
        } else if (this.mModemSuggestedDelay == -2 || this.mSameApnRetryCount >= 3) {
            this.mSameApnRetryCount = 0;
            int index = this.mCurrentApnIndex;
            do {
                index++;
                if (index == this.mWaitingApns.size()) {
                    index = 0;
                }
                if (!((ApnSetting) this.mWaitingApns.get(index)).permanentFailed) {
                    this.mCurrentApnIndex = index;
                    return (ApnSetting) this.mWaitingApns.get(this.mCurrentApnIndex);
                }
            } while (index != this.mCurrentApnIndex);
            return null;
        } else {
            this.mSameApnRetryCount++;
            return (ApnSetting) this.mWaitingApns.get(this.mCurrentApnIndex);
        }
    }

    public long getDelayForNextApn(boolean failFastEnabled) {
        if (this.mWaitingApns == null || this.mWaitingApns.size() == 0) {
            log("Waiting APN list is null or empty.");
            return -1;
        } else if (this.mModemSuggestedDelay == -1) {
            log("Modem suggested not retrying.");
            return -1;
        } else if (this.mModemSuggestedDelay == -2 || this.mSameApnRetryCount >= 3) {
            int index = this.mCurrentApnIndex;
            do {
                index++;
                if (index >= this.mWaitingApns.size()) {
                    index = 0;
                }
                if (!((ApnSetting) this.mWaitingApns.get(index)).permanentFailed) {
                    long delay;
                    if (index > this.mCurrentApnIndex) {
                        delay = this.mInterApnDelay;
                    } else if (this.mRetryForever || this.mRetryCount + 1 <= this.mMaxRetryCount) {
                        delay = (long) getRetryTimer();
                        this.mRetryCount++;
                    } else {
                        log("Reached maximum retry count " + this.mMaxRetryCount + ".");
                        return -1;
                    }
                    if (failFastEnabled && delay > this.mFailFastInterApnDelay) {
                        delay = this.mFailFastInterApnDelay;
                    }
                    return delay;
                }
            } while (index != this.mCurrentApnIndex);
            log("All APNs have permanently failed.");
            return -1;
        } else {
            log("Modem suggested retry in " + this.mModemSuggestedDelay + " ms.");
            return this.mModemSuggestedDelay;
        }
    }

    public void markApnPermanentFailed(ApnSetting apn) {
        if (apn != null) {
            apn.permanentFailed = true;
        }
    }

    private void reset() {
        this.mMaxRetryCount = 0;
        this.mRetryCount = 0;
        this.mCurrentApnIndex = -1;
        this.mSameApnRetryCount = 0;
        this.mModemSuggestedDelay = -2;
        this.mRetryArray.clear();
    }

    public void setWaitingApns(ArrayList<ApnSetting> waitingApns) {
        if (waitingApns == null) {
            log("No waiting APNs provided");
            return;
        }
        this.mWaitingApns = waitingApns;
        configureRetry();
        for (ApnSetting apn : this.mWaitingApns) {
            apn.permanentFailed = false;
        }
        log("Setting " + this.mWaitingApns.size() + " waiting APNs.");
    }

    public ArrayList<ApnSetting> getWaitingApns() {
        return this.mWaitingApns;
    }

    public void setModemSuggestedDelay(long delay) {
        this.mModemSuggestedDelay = delay;
    }

    public long getRetryAfterDisconnectDelay() {
        return this.mApnRetryAfterDisconnectDelay;
    }

    public String toString() {
        if (this.mConfig == null) {
            return SpnOverride.MVNO_TYPE_NONE;
        }
        return "RetryManager: mApnType=" + this.mApnType + " mRetryCount=" + this.mRetryCount + " mMaxRetryCount=" + this.mMaxRetryCount + " mCurrentApnIndex=" + this.mCurrentApnIndex + " mSameApnRtryCount=" + this.mSameApnRetryCount + " mModemSuggestedDelay=" + this.mModemSuggestedDelay + " mRetryForever=" + this.mRetryForever + " mInterApnDelay=" + this.mInterApnDelay + " mApnRetryAfterDisconnectDelay=" + this.mApnRetryAfterDisconnectDelay + " mConfig={" + this.mConfig + "}";
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, "[" + this.mApnType + "] " + s);
    }
}
