package com.android.server.wifi.util;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.net.wifi.WifiConfiguration;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserManager;
import android.text.TextUtils;
import com.android.server.WifiRomUpdateHelper;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiLog;
import com.android.server.wifi.WifiSettingsStore;

public class WifiPermissionsUtil {
    public static final String CONNECT_WIFIINFO_PACKAGE = "CONNECT_WIFIINFO_PACKAGE";
    private static final String DEFAULT_WIFIINFO_PACKAGE = "com.android.server.wifi";
    private static final String TAG = "WifiPermissionsUtil";
    private static final String[] mNLPPackages = new String[]{"com.baidu.map.location", "com.amap.android.location", "com.amap.android.ams"};
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private WifiLog mLog;
    private final NetworkScoreManager mNetworkScoreManager;
    private final WifiSettingsStore mSettingsStore;
    private final UserManager mUserManager;
    private final WifiPermissionsWrapper mWifiPermissionsWrapper;
    private WifiRomUpdateHelper mWifiRomUpdateHelper = null;

    public WifiPermissionsUtil(WifiPermissionsWrapper wifiPermissionsWrapper, Context context, WifiSettingsStore settingsStore, UserManager userManager, NetworkScoreManager networkScoreManager, WifiInjector wifiInjector) {
        this.mWifiPermissionsWrapper = wifiPermissionsWrapper;
        this.mContext = context;
        this.mUserManager = userManager;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mSettingsStore = settingsStore;
        this.mLog = wifiInjector.makeLog(TAG);
        this.mNetworkScoreManager = networkScoreManager;
        this.mFrameworkFacade = wifiInjector.getFrameworkFacade();
        this.mWifiRomUpdateHelper = new WifiRomUpdateHelper(this.mContext);
    }

    public boolean checkConfigOverridePermission(int uid) {
        boolean z = false;
        try {
            if (this.mWifiPermissionsWrapper.getOverrideWifiConfigPermission(uid) == 0) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            this.mLog.err("Error checking for permission: %").r(e.getMessage()).flush();
            return false;
        }
    }

    public boolean checkChangePermission(int uid) {
        boolean z = false;
        try {
            if (this.mWifiPermissionsWrapper.getChangeWifiConfigPermission(uid) == 0) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            this.mLog.err("Error checking for permission: %").r(e.getMessage()).flush();
            return false;
        }
    }

    public void enforceTetherChangePermission(Context context) {
        ConnectivityManager.enforceTetherChangePermission(context, context.getOpPackageName());
    }

    public void enforceLocationPermission(String pkgName, int uid) {
        if (!checkCallersLocationPermission(pkgName, uid)) {
            throw new SecurityException("UID " + uid + " does not have Location permission");
        }
    }

    public boolean checkCallersLocationPermission(String pkgName, int uid) {
        if (this.mWifiPermissionsWrapper.getUidPermission("android.permission.ACCESS_COARSE_LOCATION", uid) == 0 && checkAppOpAllowed(0, pkgName, uid)) {
            return true;
        }
        return false;
    }

    public boolean canAccessScanResults(String pkgName, int uid, int minVersion) throws SecurityException {
        boolean canCallingUidAccessLocation;
        boolean canAppPackageUseLocation;
        this.mAppOps.checkPackage(uid, pkgName);
        if (checkCallerHasPeersMacAddressPermission(uid)) {
            canCallingUidAccessLocation = true;
        } else {
            canCallingUidAccessLocation = isCallerActiveNwScorer(uid);
        }
        if (isLegacyForeground(pkgName, minVersion)) {
            canAppPackageUseLocation = true;
        } else if (isLocationModeEnabled(pkgName)) {
            canAppPackageUseLocation = checkCallersLocationPermission(pkgName, uid);
        } else {
            canAppPackageUseLocation = false;
        }
        int canAppPackageUseLocation2 = !canAppPackageUseLocation ? isNLP(pkgName) : 1;
        if (!canCallingUidAccessLocation && (canAppPackageUseLocation2 ^ 1) != 0) {
            this.mLog.tC("Denied: no location permission");
            return false;
        } else if (!isScanAllowedbyApps(pkgName, uid)) {
            this.mLog.tC("Denied: app wifi scan not allowed");
            return false;
        } else if (canAccessUserProfile(uid)) {
            return true;
        } else {
            this.mLog.tC("Denied: Profile not permitted");
            return false;
        }
    }

    public boolean canAccessFullConnectionInfo(WifiConfiguration currentConfig, String pkgName, int uid, int minVersion) throws SecurityException {
        boolean z = true;
        if (isPackageCanGetWifiinfo(pkgName)) {
            return true;
        }
        this.mAppOps.checkPackage(uid, pkgName);
        if (canAccessUserProfile(uid)) {
            if (!canAccessScanResults(pkgName, uid, minVersion)) {
                z = isUseOpenWifiPackageWithConnectionInfoAccess(currentConfig, pkgName);
            }
            return z;
        }
        this.mLog.tC("Denied: Profile not permitted");
        return false;
    }

    private boolean isUseOpenWifiPackageWithConnectionInfoAccess(WifiConfiguration currentConfig, String pkgName) {
        if (currentConfig == null) {
            this.mLog.tC("Denied: WifiConfiguration is NULL.");
            return false;
        } else if (!currentConfig.isOpenNetwork()) {
            this.mLog.tC("Denied: The current config is not for an open network.");
            return false;
        } else if (isUseOpenWifiPackage(pkgName)) {
            return true;
        } else {
            this.mLog.tC("Denied: caller is not the current USE_OPEN_WIFI_PACKAGE");
            return false;
        }
    }

    private boolean canAccessUserProfile(int uid) {
        if (isCurrentProfile(uid) || (checkInteractAcrossUsersFull(uid) ^ 1) == 0) {
            return true;
        }
        return false;
    }

    private boolean checkCallerHasPeersMacAddressPermission(int uid) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.PEERS_MAC_ADDRESS", uid) == 0;
    }

    private boolean isCallerActiveNwScorer(int uid) {
        return this.mNetworkScoreManager.isCallerActiveScorer(uid);
    }

    private boolean isUseOpenWifiPackage(String packageName) {
        boolean z = false;
        if (!TextUtils.isEmpty(packageName) && packageName.equals(this.mFrameworkFacade.getStringSetting(this.mContext, "use_open_wifi_package"))) {
            long token = Binder.clearCallingIdentity();
            try {
                NetworkScorerAppData appData = this.mNetworkScoreManager.getActiveScorer();
                if (appData != null) {
                    ComponentName enableUseOpenWifiActivity = appData.getEnableUseOpenWifiActivity();
                    if (enableUseOpenWifiActivity != null) {
                        z = packageName.equals(enableUseOpenWifiActivity.getPackageName());
                    }
                    return z;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return false;
    }

    private boolean isScanAllowedbyApps(String pkgName, int uid) {
        return checkAppOpAllowed(10, pkgName, uid);
    }

    private boolean checkInteractAcrossUsersFull(int uid) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.INTERACT_ACROSS_USERS_FULL", uid) == 0;
    }

    private boolean isCurrentProfile(int uid) {
        long token = Binder.clearCallingIdentity();
        try {
            int currentUser = this.mWifiPermissionsWrapper.getCurrentUser();
            int callingUserId = this.mWifiPermissionsWrapper.getCallingUserId(uid);
            if (callingUserId == currentUser) {
                return true;
            }
            if (callingUserId == 999) {
                Binder.restoreCallingIdentity(token);
                return true;
            }
            for (UserInfo user : this.mUserManager.getProfiles(currentUser)) {
                if (user.id == callingUserId) {
                    Binder.restoreCallingIdentity(token);
                    return true;
                }
            }
            Binder.restoreCallingIdentity(token);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isLegacyVersion(String pkgName, int minVersion) {
        try {
            if (this.mContext.getPackageManager().getApplicationInfo(pkgName, 0).targetSdkVersion < minVersion) {
                return true;
            }
        } catch (NameNotFoundException e) {
        }
        return false;
    }

    private boolean checkAppOpAllowed(int op, String pkgName, int uid) {
        return this.mAppOps.noteOp(op, uid, pkgName) == 0;
    }

    private boolean isLegacyForeground(String pkgName, int version) {
        return isLegacyVersion(pkgName, version) ? isForegroundApp(pkgName) : false;
    }

    private boolean isForegroundApp(String pkgName) {
        return pkgName.equals(this.mWifiPermissionsWrapper.getTopPkgName());
    }

    private boolean isLocationModeEnabled(String pkgName) {
        return this.mSettingsStore.getLocationModeSetting(this.mContext) != 0;
    }

    public boolean checkNetworkSettingsPermission(int uid) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.NETWORK_SETTINGS", uid) == 0;
    }

    private static boolean isNLP(String callingPackage) {
        for (String name : mNLPPackages) {
            if (name.equals(callingPackage)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPackageCanGetWifiinfo(String pkg) {
        if (getRomUpdateValue(CONNECT_WIFIINFO_PACKAGE, DEFAULT_WIFIINFO_PACKAGE).contains(pkg)) {
            return true;
        }
        return false;
    }

    public String getRomUpdateValue(String key, String defaultVal) {
        if (this.mWifiRomUpdateHelper != null) {
            return this.mWifiRomUpdateHelper.getValue(key, defaultVal);
        }
        return defaultVal;
    }
}
