package com.android.server.wifi;

import android.net.wifi.WifiConfiguration;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class OppoNetworkRecordHelper {
    private static final int FIRST_AMOUNT_TO_REMOVE = 60;
    public static final int MAX_NETWORK_RECORDS = 100;
    protected static final String NETWORK_RECORD_FILE = (Environment.getDataDirectory() + "/misc/wifi/networkRecord.txt");
    private static final int RECORD_LENGTH = 4;
    private static final int SECOND_AMOUNT_TO_REMOVE = 20;
    private static final String TAG = "OppoNetworkRecordHelper";
    private boolean mDebug = false;
    private HashMap<String, Long> mNetworkRecords = new HashMap();
    private WifiConfigManager mWifiConfigManager;

    class RecordConnectTimesComparator implements Comparator<WifiConfiguration> {
        RecordConnectTimesComparator() {
        }

        public int compare(WifiConfiguration c1, WifiConfiguration c2) {
            int t1 = c1.numAssociation;
            int t2 = c2.numAssociation;
            if (t1 != t2) {
                return t1 > t2 ? -1 : 1;
            } else {
                return 0;
            }
        }
    }

    class RecordLastConnectedTimeComparator implements Comparator<WifiConfiguration> {
        RecordLastConnectedTimeComparator() {
        }

        public int compare(WifiConfiguration c1, WifiConfiguration c2) {
            long t1 = c1.lastConnected;
            long t2 = c2.lastConnected;
            if (t1 != t2) {
                return t1 > t2 ? 1 : -1;
            } else {
                return 0;
            }
        }
    }

    OppoNetworkRecordHelper(WifiConfigManager wifiConfigManager) {
        this.mWifiConfigManager = wifiConfigManager;
    }

    protected void clearObsoleteNetworks() {
        List<WifiConfiguration> configuredNetworks = this.mWifiConfigManager.getSavedNetworks();
        if (configuredNetworks.size() > 80) {
            Log.d(TAG, "clearing Obsolete Networks");
            if (this.mDebug) {
                Log.d(TAG, " first sort the networks by lastConnected time.");
            }
            Collections.sort(configuredNetworks, new RecordLastConnectedTimeComparator());
            for (int i = 0; i < 80; i++) {
                configuredNetworks.remove(configuredNetworks.size() - 1);
            }
            if (this.mDebug) {
                Log.d(TAG, "" + configuredNetworks.size() + " networks will be removed!");
            }
            boolean removed = false;
            for (WifiConfiguration config : configuredNetworks) {
                if (config != null) {
                    removed = this.mWifiConfigManager.removeNetworkWithoutBroadcast(config.networkId);
                }
            }
            if (removed) {
                this.mWifiConfigManager.sendConfiguredNetworksChangedBroadcast();
                this.mWifiConfigManager.saveToStore(true);
                Log.d(TAG, " successfully clean redundant configurated networks!");
            } else {
                Log.e(TAG, " failed to remove networks!!");
            }
        }
    }

    public void dump(List<WifiConfiguration> networks) {
        Log.d(TAG, " dump need-to-remove networks:");
        for (WifiConfiguration config : networks) {
            Log.d(TAG, " " + config.SSID);
        }
    }

    public void enableVerboseLogging(int Verbose) {
        if (Verbose > 0) {
            this.mDebug = true;
        } else {
            this.mDebug = false;
        }
    }

    public void loadAllNetworkRecords() {
        IOException e;
        Throwable th;
        BufferedReader reader = null;
        synchronized (this.mNetworkRecords) {
            try {
                this.mNetworkRecords.clear();
                try {
                    BufferedReader reader2 = new BufferedReader(new FileReader(NETWORK_RECORD_FILE));
                    try {
                        for (String line = reader2.readLine(); line != null; line = reader2.readLine()) {
                            if (line != null) {
                                Log.d(TAG, "loadAllNetworkRecords line: " + line);
                            }
                            String[] details = line.split("\t");
                            if (details.length == 4) {
                                try {
                                    this.mNetworkRecords.put(details[1], Long.valueOf(Long.parseLong(details[2])));
                                } catch (Exception e2) {
                                    Log.e(TAG, "failed to parse time stamp: " + e2);
                                }
                            } else {
                                Log.e(TAG, "loadAllNetworkRecords invalid record;");
                            }
                        }
                        reader = reader2;
                    } catch (EOFException e3) {
                        reader = reader2;
                    } catch (FileNotFoundException e4) {
                        reader = reader2;
                    } catch (IOException e5) {
                        e = e5;
                        reader = reader2;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (EOFException e6) {
                    if (reader != null) {
                        try {
                            reader.close();
                            reader = null;
                        } catch (Exception e22) {
                            Log.e(TAG, "loadAllNetworkRecords: Error closing file:" + e22);
                        }
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e222) {
                            Log.e(TAG, "loadAllNetworkRecord: Error closing file" + e222);
                        }
                    }
                    Log.d(TAG, "After loadAllNetworkRecords number of records: " + this.mNetworkRecords.size());
                    return;
                } catch (FileNotFoundException e7) {
                    if (reader != null) {
                        reader.close();
                        reader = null;
                    }
                    Log.e(TAG, "loadAllNetworkRecords networkRecord.txt not found!");
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e2222) {
                            Log.e(TAG, "loadAllNetworkRecord: Error closing file" + e2222);
                        }
                    }
                    Log.d(TAG, "After loadAllNetworkRecords number of records: " + this.mNetworkRecords.size());
                    return;
                } catch (IOException e8) {
                    e = e8;
                    Log.e(TAG, "loadAllNetworkRecords: Error reading network records:" + e);
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e22222) {
                            Log.e(TAG, "loadAllNetworkRecord: Error closing file" + e22222);
                        }
                    }
                    Log.d(TAG, "After loadAllNetworkRecords number of records: " + this.mNetworkRecords.size());
                    return;
                }
            } catch (Exception e222222) {
                Log.e(TAG, "loadAllNetworkRecords: Error closing file:" + e222222);
            } catch (Throwable th3) {
                th = th3;
            }
        }
        throw th;
    }

    public long getLastConnectedTimeStamp(String networkSsid) {
        if (this.mNetworkRecords.get(networkSsid) == null) {
            return System.currentTimeMillis() - 432000000;
        }
        return ((Long) this.mNetworkRecords.get(networkSsid)).longValue();
    }

    public static boolean isNetworkRecordTxtPresent() {
        return new File(NETWORK_RECORD_FILE).exists();
    }

    public void deleteNetworkRecordTxt() {
        if (isNetworkRecordTxtPresent()) {
            if (!new File(NETWORK_RECORD_FILE).delete()) {
                Log.e(TAG, " failed to remove networkRecord.txt!");
            }
            Log.d(TAG, " networkRecord.txt removed!");
            this.mNetworkRecords = null;
        }
    }

    public void fillFieldIfNecessary(WifiConfiguration configuration) {
        long netRLastconnected = getLastConnectedTimeStamp(configuration.SSID);
        if (configuration.lastConnected == 0) {
            Log.d(TAG, " need to fill " + configuration.SSID);
            configuration.lastConnected = netRLastconnected;
        }
    }
}
