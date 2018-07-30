package com.android.server.oppo;

import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;

public class ElsaManager {
    static final int PROP_ENABLE_RESUME_MASK = 16;
    static final boolean enableResume;

    static {
        boolean z = false;
        if ((SystemProperties.getInt("persist.sys.elsa.enable", 16) & 16) > 0) {
            z = true;
        }
        enableResume = z;
    }

    public static void elsaResume(int id, int timeout, boolean isTargetFreeze, int flag) {
        RuntimeException here = new RuntimeException("here");
        here.fillInStackTrace();
        Slog.d("elsa", "Called: elsaResume", here);
        if (enableResume) {
            IBinder binder = ServiceManager.checkService(IElsaManager.DESCRIPTOR);
            if (binder != null) {
                try {
                    new ElsaManagerProxy(binder).elsaResume(id, timeout, isTargetFreeze ? 1 : 0, flag);
                } catch (Exception e) {
                }
            }
        }
    }

    public static boolean isFrozingByPid(int pid) {
        if (pid == 0) {
            return false;
        }
        boolean result = false;
        IBinder binder = ServiceManager.checkService(IElsaManager.DESCRIPTOR);
        if (binder == null) {
            return false;
        }
        try {
            result = new ElsaManagerProxy(binder).elsaGetPackageFreezing(pid, 1) > 0;
        } catch (Exception e) {
        }
        return result;
    }

    public static void elsaResumePid(int id) {
        if (enableResume) {
            elsaResume(id, 0, true, 1);
        }
    }
}
