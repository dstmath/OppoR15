package com.oppo.luckymoney;

import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.app.ILMServiceManager;
import com.android.internal.app.ILMServiceManager.Stub;
import com.android.internal.telephony.ITelephony;

public class LMManager {
    public static final String LUCKY_MONEY_SERVICE = "luckymoney";
    private static final int MOBILE_POLICY_LAST_TIME = 300;
    private static final int PER_PING_TIME = 8;
    private static final String TAG = "LMManager";
    private static LMManager sLMManager = null;
    private static ILMServiceManager sService = null;
    private static ITelephony sTelManager = null;

    public LMManager() {
        init();
    }

    public static synchronized LMManager getLMManager() {
        LMManager lMManager;
        synchronized (LMManager.class) {
            if (sLMManager == null) {
                sLMManager = new LMManager();
            }
            lMManager = sLMManager;
        }
        return lMManager;
    }

    private void init() {
        if (sService == null) {
            sService = Stub.asInterface(ServiceManager.getService(LUCKY_MONEY_SERVICE));
        }
        if (sTelManager == null) {
            sTelManager = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    }

    public void enableBoost(int timeout, int code) {
        if (sService != null) {
            try {
                sService.enableBoost(Process.myPid(), Process.myUid(), timeout, code);
                if (sTelManager == null || !SystemProperties.get("sys.oppo.nw.hongbao", "0").equals("1")) {
                    sTelManager = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                    return;
                }
                Slog.d(TAG, "enableBoost");
                sTelManager.startMobileDataHongbaoPolicy(300, 8, null, null);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
                return;
            }
        }
        init();
    }

    public String getLuckyMoneyInfo(int type) {
        String tmp = null;
        if (sService != null) {
            try {
                return sService.getLuckyMoneyInfo(type);
            } catch (RemoteException e) {
                e.printStackTrace();
                return tmp;
            }
        }
        init();
        return tmp;
    }
}
