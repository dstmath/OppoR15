package com.android.server.net;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import com.android.server.LocalServices;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class NetworkStatsAccess {

    @Retention(RetentionPolicy.SOURCE)
    public @interface Level {
        public static final int DEFAULT = 0;
        public static final int DEVICE = 3;
        public static final int DEVICESUMMARY = 2;
        public static final int USER = 1;
    }

    private NetworkStatsAccess() {
    }

    public static int checkAccessLevel(Context context, int callingUid, String callingPackage) {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        boolean hasCarrierPrivileges = tm != null ? tm.checkCarrierPrivilegesForPackage(callingPackage) == 1 : false;
        boolean isDeviceOwner = dpmi != null ? dpmi.isActiveAdminWithPolicy(callingUid, -2) : false;
        if (hasCarrierPrivileges || isDeviceOwner || UserHandle.getAppId(callingUid) == 1000) {
            return 3;
        }
        if (hasAppOpsPermission(context, callingUid, callingPackage) || context.checkCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY") == 0) {
            return 2;
        }
        return dpmi != null ? dpmi.isActiveAdminWithPolicy(callingUid, -1) : false ? 1 : 0;
    }

    public static boolean isAccessibleToUser(int uid, int callerUid, int accessLevel) {
        boolean z = true;
        switch (accessLevel) {
            case 1:
                if (!(uid == 1000 || uid == -4 || uid == -5 || UserHandle.getUserId(uid) == UserHandle.getUserId(callerUid))) {
                    z = false;
                }
                return z;
            case 2:
                if (!(uid == 1000 || uid == -4 || uid == -5 || uid == -1 || UserHandle.getUserId(uid) == UserHandle.getUserId(callerUid))) {
                    z = false;
                }
                return z;
            case 3:
                return true;
            default:
                if (uid != callerUid) {
                    z = false;
                }
                return z;
        }
    }

    private static boolean hasAppOpsPermission(Context context, int callingUid, String callingPackage) {
        boolean z = true;
        if (callingPackage == null) {
            return false;
        }
        int mode = ((AppOpsManager) context.getSystemService("appops")).checkOp(43, callingUid, callingPackage);
        if (mode == 3) {
            if (context.checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") != 0) {
                z = false;
            }
            return z;
        }
        if (mode != 0) {
            z = false;
        }
        return z;
    }
}
