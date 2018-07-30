package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.UserManager;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationMap {
    private int mCurrentUserId = 0;
    private final Map<Integer, WifiConfiguration> mPerID = new ConcurrentHashMap();
    private final Map<Integer, WifiConfiguration> mPerIDForCurrentUser = new ConcurrentHashMap();
    private final Map<ScanResultMatchInfo, WifiConfiguration> mScanResultMatchInfoMapForCurrentUser = new ConcurrentHashMap();
    private final UserManager mUserManager;

    ConfigurationMap(UserManager userManager) {
        this.mUserManager = userManager;
    }

    public WifiConfiguration put(WifiConfiguration config) {
        WifiConfiguration current = (WifiConfiguration) this.mPerID.put(Integer.valueOf(config.networkId), config);
        if (WifiConfigurationUtil.isVisibleToAnyProfile(config, this.mUserManager.getProfiles(this.mCurrentUserId))) {
            this.mPerIDForCurrentUser.put(Integer.valueOf(config.networkId), config);
            this.mScanResultMatchInfoMapForCurrentUser.put(ScanResultMatchInfo.fromWifiConfiguration(config), config);
        }
        return current;
    }

    public WifiConfiguration remove(int netID) {
        WifiConfiguration config = (WifiConfiguration) this.mPerID.remove(Integer.valueOf(netID));
        if (config == null) {
            return null;
        }
        this.mPerIDForCurrentUser.remove(Integer.valueOf(netID));
        Iterator<Entry<ScanResultMatchInfo, WifiConfiguration>> scanResultMatchInfoEntries = this.mScanResultMatchInfoMapForCurrentUser.entrySet().iterator();
        while (scanResultMatchInfoEntries.hasNext()) {
            if (((WifiConfiguration) ((Entry) scanResultMatchInfoEntries.next()).getValue()).networkId == netID) {
                scanResultMatchInfoEntries.remove();
                break;
            }
        }
        return config;
    }

    public void clear() {
        this.mPerID.clear();
        this.mPerIDForCurrentUser.clear();
        this.mScanResultMatchInfoMapForCurrentUser.clear();
    }

    public void setNewUser(int userId) {
        this.mCurrentUserId = userId;
    }

    public WifiConfiguration getForAllUsers(int netid) {
        return (WifiConfiguration) this.mPerID.get(Integer.valueOf(netid));
    }

    public WifiConfiguration getForCurrentUser(int netid) {
        return (WifiConfiguration) this.mPerIDForCurrentUser.get(Integer.valueOf(netid));
    }

    public int sizeForAllUsers() {
        return this.mPerID.size();
    }

    public int sizeForCurrentUser() {
        return this.mPerIDForCurrentUser.size();
    }

    public WifiConfiguration getByConfigKeyForCurrentUser(String key) {
        if (key == null) {
            return null;
        }
        for (WifiConfiguration config : this.mPerIDForCurrentUser.values()) {
            if (config.configKey().equals(key)) {
                return config;
            }
        }
        return null;
    }

    public WifiConfiguration getByScanResultForCurrentUser(ScanResult scanResult) {
        return (WifiConfiguration) this.mScanResultMatchInfoMapForCurrentUser.get(ScanResultMatchInfo.fromScanResult(scanResult));
    }

    public Collection<WifiConfiguration> valuesForAllUsers() {
        return this.mPerID.values();
    }

    public Collection<WifiConfiguration> valuesForCurrentUser() {
        return this.mPerIDForCurrentUser.values();
    }
}
