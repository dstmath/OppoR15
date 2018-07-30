package com.android.internal.telephony;

import android.content.Context;
import android.net.LocalServerSocket;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.telephony.dataconnection.TelephonyNetworkFactory;
import com.android.internal.telephony.euicc.EuiccController;
import com.android.internal.telephony.ims.ImsResolver;
import com.android.internal.telephony.imsphone.ImsPhoneFactory;
import com.android.internal.telephony.oem.rus.RusInitProcess;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.sip.SipPhoneFactory;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.SpnOverride;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class PhoneFactory {
    static final boolean DBG = false;
    static final String LOG_TAG = "PhoneFactory";
    public static final int MAX_ACTIVE_PHONES = 1;
    static final int SOCKET_OPEN_MAX_RETRY = 3;
    static final int SOCKET_OPEN_RETRY_MILLIS = 2000;
    private static CommandsInterface sCommandsInterface = null;
    private static CommandsInterface[] sCommandsInterfaces = null;
    private static Context sContext;
    private static EuiccController sEuiccController;
    private static ImsResolver sImsResolver;
    private static IntentBroadcaster sIntentBroadcaster;
    private static final HashMap<String, LocalLog> sLocalLogs = new HashMap();
    static final Object sLockProxyPhones = new Object();
    private static boolean sMadeDefaults = false;
    private static NotificationChannelController sNotificationChannelController;
    private static Phone sPhone = null;
    private static PhoneNotifier sPhoneNotifier;
    private static PhoneSwitcher sPhoneSwitcher;
    private static Phone[] sPhones = null;
    private static ProxyController sProxyController;
    private static SubscriptionInfoUpdater sSubInfoRecordUpdater = null;
    private static SubscriptionMonitor sSubscriptionMonitor;
    private static TelephonyNetworkFactory[] sTelephonyNetworkFactories;
    private static UiccController sUiccController;

    public static void makeDefaultPhones(Context context) {
        makeDefaultPhone(context);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void makeDefaultPhone(Context context) {
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                sContext = context;
                TelephonyDevController.create();
                int retryCount = 0;
                while (true) {
                    boolean hasException = false;
                    retryCount++;
                    try {
                        LocalServerSocket localServerSocket = new LocalServerSocket("com.android.internal.telephony");
                    } catch (IOException e) {
                        hasException = true;
                    }
                    if (!hasException) {
                        break;
                    } else if (retryCount > 3) {
                        throw new RuntimeException("PhoneFactory probably already running");
                    } else {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e2) {
                        }
                    }
                }
            }
        }
        RusInitProcess.execute(context);
    }

    public static Phone getDefaultPhone() {
        Phone phone;
        synchronized (sLockProxyPhones) {
            if (sMadeDefaults) {
                phone = sPhone;
            } else {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
        }
        return phone;
    }

    public static Phone getPhone(int phoneId) {
        Phone phone;
        String dbgInfo = SpnOverride.MVNO_TYPE_NONE;
        synchronized (sLockProxyPhones) {
            if (sMadeDefaults) {
                if (phoneId == Integer.MAX_VALUE) {
                    phone = sPhone;
                } else {
                    if (phoneId >= 0) {
                        if (phoneId < TelephonyManager.getDefault().getPhoneCount()) {
                            phone = sPhones[phoneId];
                        }
                    }
                    phone = null;
                }
            } else {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
        }
        return phone;
    }

    public static Phone[] getPhones() {
        Phone[] phoneArr;
        synchronized (sLockProxyPhones) {
            if (sMadeDefaults) {
                phoneArr = sPhones;
            } else {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
        }
        return phoneArr;
    }

    public static ImsResolver getImsResolver() {
        return sImsResolver;
    }

    public static SipPhone makeSipPhone(String sipUri) {
        return SipPhoneFactory.makePhone(sipUri, sContext, sPhoneNotifier);
    }

    public static int calculatePreferredNetworkType(Context context, int phoneSubId) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(phoneSubId);
        int phoneIdNetworkType = RILConstants.PREFERRED_NETWORK_MODE;
        try {
            phoneIdNetworkType = TelephonyManager.getIntAtIndex(context.getContentResolver(), "preferred_network_mode", phoneId);
        } catch (SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Valuefor phoneID");
        }
        int networkType = phoneIdNetworkType;
        Rlog.d(LOG_TAG, "calculatePreferredNetworkType: phoneId = " + phoneId + " phoneIdNetworkType = " + phoneIdNetworkType);
        if (SubscriptionController.getInstance().isActiveSubId(phoneSubId)) {
            networkType = Global.getInt(context.getContentResolver(), "preferred_network_mode" + phoneSubId, phoneIdNetworkType);
        } else {
            Rlog.d(LOG_TAG, "calculatePreferredNetworkType: phoneSubId = " + phoneSubId + " is not a active SubId");
        }
        Rlog.d(LOG_TAG, "calculatePreferredNetworkType: phoneSubId = " + phoneSubId + " networkType = " + networkType);
        return networkType;
    }

    public static int getDefaultSubscription() {
        return SubscriptionController.getInstance().getDefaultSubId();
    }

    public static boolean isSMSPromptEnabled() {
        int value = 0;
        try {
            value = Global.getInt(sContext.getContentResolver(), "multi_sim_sms_prompt");
        } catch (SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Prompt Values");
        }
        boolean prompt = value != 0;
        Rlog.d(LOG_TAG, "SMS Prompt option:" + prompt);
        return prompt;
    }

    public static Phone makeImsPhone(PhoneNotifier phoneNotifier, Phone defaultPhone) {
        return ImsPhoneFactory.makePhone(sContext, phoneNotifier, defaultPhone);
    }

    public static void requestEmbeddedSubscriptionInfoListRefresh(Runnable callback) {
        sSubInfoRecordUpdater.requestEmbeddedSubscriptionInfoListRefresh(callback);
    }

    public static void addLocalLog(String key, int size) {
        synchronized (sLocalLogs) {
            if (sLocalLogs.containsKey(key)) {
                throw new IllegalArgumentException("key " + key + " already present");
            }
            sLocalLogs.put(key, new LocalLog(size));
        }
    }

    public static void localLog(String key, String log) {
        synchronized (sLocalLogs) {
            if (sLocalLogs.containsKey(key)) {
                ((LocalLog) sLocalLogs.get(key)).log(log);
            } else {
                throw new IllegalArgumentException("key " + key + " not found");
            }
        }
    }

    public static void dump(FileDescriptor fd, PrintWriter printwriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printwriter, "  ");
        pw.println("PhoneFactory:");
        pw.println(" sMadeDefaults=" + sMadeDefaults);
        sPhoneSwitcher.dump(fd, pw, args);
        pw.println();
        Phone[] phones = getPhones();
        for (int i = 0; i < phones.length; i++) {
            pw.increaseIndent();
            Phone phone = phones[i];
            try {
                phone.dump(fd, pw, args);
                pw.flush();
                pw.println("++++++++++++++++++++++++++++++++");
                sTelephonyNetworkFactories[i].dump(fd, pw, args);
                pw.flush();
                pw.println("++++++++++++++++++++++++++++++++");
                try {
                    ((IccCardProxy) phone.getIccCard()).dump(fd, pw, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pw.flush();
                pw.decreaseIndent();
                pw.println("++++++++++++++++++++++++++++++++");
            } catch (Exception e2) {
                pw.println("Telephony DebugService: Could not get Phone[" + i + "] e=" + e2);
            }
        }
        pw.println("SubscriptionMonitor:");
        pw.increaseIndent();
        try {
            sSubscriptionMonitor.dump(fd, pw, args);
        } catch (Exception e22) {
            e22.printStackTrace();
        }
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        pw.println("UiccController:");
        pw.increaseIndent();
        try {
            sUiccController.dump(fd, pw, args);
        } catch (Exception e222) {
            e222.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        if (sEuiccController != null) {
            pw.println("EuiccController:");
            pw.increaseIndent();
            try {
                sEuiccController.dump(fd, pw, args);
            } catch (Exception e2222) {
                e2222.printStackTrace();
            }
            pw.flush();
            pw.decreaseIndent();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        pw.println("SubscriptionController:");
        pw.increaseIndent();
        try {
            SubscriptionController.getInstance().dump(fd, pw, args);
        } catch (Exception e22222) {
            e22222.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        pw.println("SubInfoRecordUpdater:");
        pw.increaseIndent();
        try {
            sSubInfoRecordUpdater.dump(fd, pw, args);
        } catch (Exception e222222) {
            e222222.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        pw.println("LocalLogs:");
        pw.increaseIndent();
        synchronized (sLocalLogs) {
            for (String key : sLocalLogs.keySet()) {
                pw.println(key);
                pw.increaseIndent();
                ((LocalLog) sLocalLogs.get(key)).dump(fd, pw, args);
                pw.decreaseIndent();
            }
            pw.flush();
        }
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        pw.println("SharedPreferences:");
        pw.increaseIndent();
        try {
            if (sContext != null) {
                Map spValues = PreferenceManager.getDefaultSharedPreferences(sContext).getAll();
                for (Object key2 : spValues.keySet()) {
                    pw.println(key2 + " : " + spValues.get(key2));
                }
            }
        } catch (Exception e2222222) {
            e2222222.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();
    }
}
