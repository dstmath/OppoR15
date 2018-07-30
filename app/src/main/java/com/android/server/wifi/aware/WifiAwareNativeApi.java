package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.IWifiNanIface;
import android.hardware.wifi.V1_0.NanBandSpecificConfig;
import android.hardware.wifi.V1_0.NanConfigRequest;
import android.hardware.wifi.V1_0.NanEnableRequest;
import android.hardware.wifi.V1_0.NanInitiateDataPathRequest;
import android.hardware.wifi.V1_0.NanPublishRequest;
import android.hardware.wifi.V1_0.NanRespondToDataPathIndicationRequest;
import android.hardware.wifi.V1_0.NanSubscribeRequest;
import android.hardware.wifi.V1_0.NanTransmitFollowupRequest;
import android.hardware.wifi.V1_0.WifiStatus;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.util.Log;
import com.android.server.wifi.aware.WifiAwareShellCommand.DelegatedShellCommand;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WifiAwareNativeApi implements DelegatedShellCommand {
    private static final boolean DBG = false;
    public static final String PARAM_DW_DEFAULT_24GHZ = "dw_default_24ghz";
    public static final int PARAM_DW_DEFAULT_24GHZ_DEFAULT = -1;
    public static final String PARAM_DW_DEFAULT_5GHZ = "dw_default_5ghz";
    public static final int PARAM_DW_DEFAULT_5GHZ_DEFAULT = -1;
    public static final String PARAM_DW_ON_IDLE_24GHZ = "dw_on_idle_24ghz";
    public static final int PARAM_DW_ON_IDLE_24GHZ_DEFAULT = -1;
    public static final String PARAM_DW_ON_IDLE_5GHZ = "dw_on_idle_5ghz";
    public static final int PARAM_DW_ON_IDLE_5GHZ_DEFAULT = -1;
    public static final String PARAM_DW_ON_INACTIVE_24GHZ = "dw_on_inactive_24ghz";
    public static final int PARAM_DW_ON_INACTIVE_24GHZ_DEFAULT = 4;
    public static final String PARAM_DW_ON_INACTIVE_5GHZ = "dw_on_inactive_5ghz";
    public static final int PARAM_DW_ON_INACTIVE_5GHZ_DEFAULT = 0;
    public static final String PARAM_MAC_RANDOM_INTERVAL_SEC = "mac_random_interval_sec";
    public static final int PARAM_MAC_RANDOM_INTERVAL_SEC_DEFAULT = 1800;
    private static final String SERVICE_NAME_FOR_OOB_DATA_PATH = "Wi-Fi Aware Data Path";
    private static final String TAG = "WifiAwareNativeApi";
    private static final boolean VDBG = false;
    private final WifiAwareNativeManager mHal;
    private Map<String, Integer> mSettableParameters = new HashMap();

    public WifiAwareNativeApi(WifiAwareNativeManager wifiAwareNativeManager) {
        this.mHal = wifiAwareNativeManager;
        onReset();
    }

    public int onCommand(ShellCommand parentShell) {
        PrintWriter pw = parentShell.getErrPrintWriter();
        String subCmd = parentShell.getNextArgRequired();
        String name;
        if (subCmd.equals("set")) {
            name = parentShell.getNextArgRequired();
            if (this.mSettableParameters.containsKey(name)) {
                String valueStr = parentShell.getNextArgRequired();
                try {
                    this.mSettableParameters.put(name, Integer.valueOf(Integer.valueOf(valueStr).intValue()));
                    return 0;
                } catch (NumberFormatException e) {
                    pw.println("Can't convert value to integer -- '" + valueStr + "'");
                    return -1;
                }
            }
            pw.println("Unknown parameter name -- '" + name + "'");
            return -1;
        } else if (subCmd.equals("get")) {
            name = parentShell.getNextArgRequired();
            if (this.mSettableParameters.containsKey(name)) {
                parentShell.getOutPrintWriter().println(((Integer) this.mSettableParameters.get(name)).intValue());
                return 0;
            }
            pw.println("Unknown parameter name -- '" + name + "'");
            return -1;
        } else {
            pw.println("Unknown 'wifiaware native_api <cmd>'");
            return -1;
        }
    }

    public void onReset() {
        this.mSettableParameters.put(PARAM_DW_DEFAULT_24GHZ, Integer.valueOf(-1));
        this.mSettableParameters.put(PARAM_DW_DEFAULT_5GHZ, Integer.valueOf(-1));
        this.mSettableParameters.put(PARAM_DW_ON_INACTIVE_24GHZ, Integer.valueOf(4));
        this.mSettableParameters.put(PARAM_DW_ON_INACTIVE_5GHZ, Integer.valueOf(0));
        this.mSettableParameters.put(PARAM_DW_ON_IDLE_24GHZ, Integer.valueOf(-1));
        this.mSettableParameters.put(PARAM_DW_ON_IDLE_5GHZ, Integer.valueOf(-1));
        this.mSettableParameters.put(PARAM_MAC_RANDOM_INTERVAL_SEC, Integer.valueOf(PARAM_MAC_RANDOM_INTERVAL_SEC_DEFAULT));
    }

    public void onHelp(String command, ShellCommand parentShell) {
        PrintWriter pw = parentShell.getOutPrintWriter();
        pw.println("  " + command);
        pw.println("    set <name> <value>: sets named parameter to value. Names: " + this.mSettableParameters.keySet());
        pw.println("    get <name>: gets named parameter value. Names: " + this.mSettableParameters.keySet());
    }

    public boolean getCapabilities(short transactionId) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "getCapabilities: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.getCapabilitiesRequest(transactionId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "getCapabilities: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "getCapabilities: exception: " + e);
            return false;
        }
    }

    public boolean enableAndConfigure(short transactionId, ConfigRequest configRequest, boolean notifyIdentityChange, boolean initialConfiguration, boolean isInteractive, boolean isIdle) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "enableAndConfigure: null interface");
            return false;
        }
        WifiStatus status;
        NanBandSpecificConfig config24;
        NanBandSpecificConfig config5;
        if (initialConfiguration) {
            try {
                NanEnableRequest req = new NanEnableRequest();
                req.operateInBand[0] = true;
                req.operateInBand[1] = configRequest.mSupport5gBand;
                req.hopCountMax = (byte) 2;
                req.configParams.masterPref = (byte) configRequest.mMasterPreference;
                req.configParams.disableDiscoveryAddressChangeIndication = notifyIdentityChange ^ 1;
                req.configParams.disableStartedClusterIndication = notifyIdentityChange ^ 1;
                req.configParams.disableJoinedClusterIndication = notifyIdentityChange ^ 1;
                req.configParams.includePublishServiceIdsInBeacon = true;
                req.configParams.numberOfPublishServiceIdsInBeacon = (byte) 0;
                req.configParams.includeSubscribeServiceIdsInBeacon = true;
                req.configParams.numberOfSubscribeServiceIdsInBeacon = (byte) 0;
                req.configParams.rssiWindowSize = (short) 8;
                req.configParams.macAddressRandomizationIntervalSec = ((Integer) this.mSettableParameters.get(PARAM_MAC_RANDOM_INTERVAL_SEC)).intValue();
                config24 = new NanBandSpecificConfig();
                config24.rssiClose = (byte) 60;
                config24.rssiMiddle = (byte) 70;
                config24.rssiCloseProximity = (byte) 60;
                config24.dwellTimeMs = (byte) -56;
                config24.scanPeriodSec = (short) 20;
                if (configRequest.mDiscoveryWindowInterval[0] == -1) {
                    config24.validDiscoveryWindowIntervalVal = false;
                } else {
                    config24.validDiscoveryWindowIntervalVal = true;
                    config24.discoveryWindowIntervalVal = (byte) configRequest.mDiscoveryWindowInterval[0];
                }
                req.configParams.bandSpecificConfig[0] = config24;
                config5 = new NanBandSpecificConfig();
                config5.rssiClose = (byte) 60;
                config5.rssiMiddle = (byte) 75;
                config5.rssiCloseProximity = (byte) 60;
                config5.dwellTimeMs = (byte) -56;
                config5.scanPeriodSec = (short) 20;
                if (configRequest.mDiscoveryWindowInterval[1] == -1) {
                    config5.validDiscoveryWindowIntervalVal = false;
                } else {
                    config5.validDiscoveryWindowIntervalVal = true;
                    config5.discoveryWindowIntervalVal = (byte) configRequest.mDiscoveryWindowInterval[1];
                }
                req.configParams.bandSpecificConfig[1] = config5;
                req.debugConfigs.validClusterIdVals = true;
                req.debugConfigs.clusterIdTopRangeVal = (short) configRequest.mClusterHigh;
                req.debugConfigs.clusterIdBottomRangeVal = (short) configRequest.mClusterLow;
                req.debugConfigs.validIntfAddrVal = false;
                req.debugConfigs.validOuiVal = false;
                req.debugConfigs.ouiVal = 0;
                req.debugConfigs.validRandomFactorForceVal = false;
                req.debugConfigs.randomFactorForceVal = (byte) 0;
                req.debugConfigs.validHopCountForceVal = false;
                req.debugConfigs.hopCountForceVal = (byte) 0;
                req.debugConfigs.validDiscoveryChannelVal = false;
                req.debugConfigs.discoveryChannelMhzVal[0] = 0;
                req.debugConfigs.discoveryChannelMhzVal[1] = 0;
                req.debugConfigs.validUseBeaconsInBandVal = false;
                req.debugConfigs.useBeaconsInBandVal[0] = true;
                req.debugConfigs.useBeaconsInBandVal[1] = true;
                req.debugConfigs.validUseSdfInBandVal = false;
                req.debugConfigs.useSdfInBandVal[0] = true;
                req.debugConfigs.useSdfInBandVal[1] = true;
                updateConfigForPowerSettings(req.configParams, isInteractive, isIdle);
                status = iface.enableRequest(transactionId, req);
            } catch (RemoteException e) {
                Log.e(TAG, "enableAndConfigure: exception: " + e);
                return false;
            }
        }
        NanConfigRequest req2 = new NanConfigRequest();
        req2.masterPref = (byte) configRequest.mMasterPreference;
        req2.disableDiscoveryAddressChangeIndication = notifyIdentityChange ^ 1;
        req2.disableStartedClusterIndication = notifyIdentityChange ^ 1;
        req2.disableJoinedClusterIndication = notifyIdentityChange ^ 1;
        req2.includePublishServiceIdsInBeacon = true;
        req2.numberOfPublishServiceIdsInBeacon = (byte) 0;
        req2.includeSubscribeServiceIdsInBeacon = true;
        req2.numberOfSubscribeServiceIdsInBeacon = (byte) 0;
        req2.rssiWindowSize = (short) 8;
        req2.macAddressRandomizationIntervalSec = ((Integer) this.mSettableParameters.get(PARAM_MAC_RANDOM_INTERVAL_SEC)).intValue();
        config24 = new NanBandSpecificConfig();
        config24.rssiClose = (byte) 60;
        config24.rssiMiddle = (byte) 70;
        config24.rssiCloseProximity = (byte) 60;
        config24.dwellTimeMs = (byte) -56;
        config24.scanPeriodSec = (short) 20;
        if (configRequest.mDiscoveryWindowInterval[0] == -1) {
            config24.validDiscoveryWindowIntervalVal = false;
        } else {
            config24.validDiscoveryWindowIntervalVal = true;
            config24.discoveryWindowIntervalVal = (byte) configRequest.mDiscoveryWindowInterval[0];
        }
        req2.bandSpecificConfig[0] = config24;
        config5 = new NanBandSpecificConfig();
        config5.rssiClose = (byte) 60;
        config5.rssiMiddle = (byte) 75;
        config5.rssiCloseProximity = (byte) 60;
        config5.dwellTimeMs = (byte) -56;
        config5.scanPeriodSec = (short) 20;
        if (configRequest.mDiscoveryWindowInterval[1] == -1) {
            config5.validDiscoveryWindowIntervalVal = false;
        } else {
            config5.validDiscoveryWindowIntervalVal = true;
            config5.discoveryWindowIntervalVal = (byte) configRequest.mDiscoveryWindowInterval[1];
        }
        req2.bandSpecificConfig[1] = config5;
        updateConfigForPowerSettings(req2, isInteractive, isIdle);
        status = iface.configRequest(transactionId, req2);
        if (status.code == 0) {
            return true;
        }
        Log.e(TAG, "enableAndConfigure: error: " + statusString(status));
        return false;
    }

    public boolean disable(short transactionId) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "disable: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.disableRequest(transactionId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "disable: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "disable: exception: " + e);
            return false;
        }
    }

    public boolean publish(short transactionId, byte publishId, PublishConfig publishConfig) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "publish: null interface");
            return false;
        }
        ArrayList arrayList;
        NanPublishRequest req = new NanPublishRequest();
        req.baseConfigs.sessionId = publishId;
        req.baseConfigs.ttlSec = (short) publishConfig.mTtlSec;
        req.baseConfigs.discoveryWindowPeriod = (short) 1;
        req.baseConfigs.discoveryCount = (byte) 0;
        convertNativeByteArrayToArrayList(publishConfig.mServiceName, req.baseConfigs.serviceName);
        req.baseConfigs.discoveryMatchIndicator = 2;
        convertNativeByteArrayToArrayList(publishConfig.mServiceSpecificInfo, req.baseConfigs.serviceSpecificInfo);
        byte[] bArr = publishConfig.mMatchFilter;
        if (publishConfig.mPublishType == 0) {
            arrayList = req.baseConfigs.txMatchFilter;
        } else {
            arrayList = req.baseConfigs.rxMatchFilter;
        }
        convertNativeByteArrayToArrayList(bArr, arrayList);
        req.baseConfigs.useRssiThreshold = false;
        req.baseConfigs.disableDiscoveryTerminationIndication = publishConfig.mEnableTerminateNotification ^ 1;
        req.baseConfigs.disableMatchExpirationIndication = true;
        req.baseConfigs.disableFollowupReceivedIndication = false;
        req.baseConfigs.securityConfig.securityType = 0;
        req.baseConfigs.rangingRequired = false;
        req.autoAcceptDataPathRequests = false;
        req.publishType = publishConfig.mPublishType;
        req.txType = 0;
        try {
            WifiStatus status = iface.startPublishRequest(transactionId, req);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "publish: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "publish: exception: " + e);
            return false;
        }
    }

    public boolean subscribe(short transactionId, byte subscribeId, SubscribeConfig subscribeConfig) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "subscribe: null interface");
            return false;
        }
        ArrayList arrayList;
        NanSubscribeRequest req = new NanSubscribeRequest();
        req.baseConfigs.sessionId = subscribeId;
        req.baseConfigs.ttlSec = (short) subscribeConfig.mTtlSec;
        req.baseConfigs.discoveryWindowPeriod = (short) 1;
        req.baseConfigs.discoveryCount = (byte) 0;
        convertNativeByteArrayToArrayList(subscribeConfig.mServiceName, req.baseConfigs.serviceName);
        req.baseConfigs.discoveryMatchIndicator = 0;
        convertNativeByteArrayToArrayList(subscribeConfig.mServiceSpecificInfo, req.baseConfigs.serviceSpecificInfo);
        byte[] bArr = subscribeConfig.mMatchFilter;
        if (subscribeConfig.mSubscribeType == 1) {
            arrayList = req.baseConfigs.txMatchFilter;
        } else {
            arrayList = req.baseConfigs.rxMatchFilter;
        }
        convertNativeByteArrayToArrayList(bArr, arrayList);
        req.baseConfigs.useRssiThreshold = false;
        req.baseConfigs.disableDiscoveryTerminationIndication = subscribeConfig.mEnableTerminateNotification ^ 1;
        req.baseConfigs.disableMatchExpirationIndication = true;
        req.baseConfigs.disableFollowupReceivedIndication = false;
        req.baseConfigs.securityConfig.securityType = 0;
        req.baseConfigs.rangingRequired = false;
        req.subscribeType = subscribeConfig.mSubscribeType;
        try {
            WifiStatus status = iface.startSubscribeRequest(transactionId, req);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "subscribe: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "subscribe: exception: " + e);
            return false;
        }
    }

    public boolean sendMessage(short transactionId, byte pubSubId, int requestorInstanceId, byte[] dest, byte[] message, int messageId) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "sendMessage: null interface");
            return false;
        }
        NanTransmitFollowupRequest req = new NanTransmitFollowupRequest();
        req.discoverySessionId = pubSubId;
        req.peerId = requestorInstanceId;
        copyArray(dest, req.addr);
        req.isHighPriority = false;
        req.shouldUseDiscoveryWindow = true;
        convertNativeByteArrayToArrayList(message, req.serviceSpecificInfo);
        req.disableFollowupResultIndication = false;
        try {
            WifiStatus status = iface.transmitFollowupRequest(transactionId, req);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "sendMessage: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessage: exception: " + e);
            return false;
        }
    }

    public boolean stopPublish(short transactionId, byte pubSubId) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "stopPublish: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.stopPublishRequest(transactionId, pubSubId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "stopPublish: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "stopPublish: exception: " + e);
            return false;
        }
    }

    public boolean stopSubscribe(short transactionId, byte pubSubId) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "stopSubscribe: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.stopSubscribeRequest(transactionId, pubSubId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "stopSubscribe: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "stopSubscribe: exception: " + e);
            return false;
        }
    }

    public boolean createAwareNetworkInterface(short transactionId, String interfaceName) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "createAwareNetworkInterface: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.createDataInterfaceRequest(transactionId, interfaceName);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "createAwareNetworkInterface: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "createAwareNetworkInterface: exception: " + e);
            return false;
        }
    }

    public boolean deleteAwareNetworkInterface(short transactionId, String interfaceName) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "deleteAwareNetworkInterface: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.deleteDataInterfaceRequest(transactionId, interfaceName);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "deleteAwareNetworkInterface: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "deleteAwareNetworkInterface: exception: " + e);
            return false;
        }
    }

    public boolean initiateDataPath(short transactionId, int peerId, int channelRequestType, int channel, byte[] peer, String interfaceName, byte[] pmk, String passphrase, boolean isOutOfBand, Capabilities capabilities) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "initiateDataPath: null interface");
            return false;
        } else if (capabilities == null) {
            Log.e(TAG, "initiateDataPath: null capabilities");
            return false;
        } else {
            NanInitiateDataPathRequest req = new NanInitiateDataPathRequest();
            req.peerId = peerId;
            copyArray(peer, req.peerDiscMacAddr);
            req.channelRequestType = channelRequestType;
            req.channel = channel;
            req.ifaceName = interfaceName;
            req.securityConfig.securityType = 0;
            if (!(pmk == null || pmk.length == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
                req.securityConfig.securityType = 1;
                copyArray(pmk, req.securityConfig.pmk);
            }
            if (!(passphrase == null || passphrase.length() == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
                req.securityConfig.securityType = 2;
                convertNativeByteArrayToArrayList(passphrase.getBytes(), req.securityConfig.passphrase);
            }
            if (req.securityConfig.securityType != 0 && isOutOfBand) {
                convertNativeByteArrayToArrayList(SERVICE_NAME_FOR_OOB_DATA_PATH.getBytes(StandardCharsets.UTF_8), req.serviceNameOutOfBand);
            }
            try {
                WifiStatus status = iface.initiateDataPathRequest(transactionId, req);
                if (status.code == 0) {
                    return true;
                }
                Log.e(TAG, "initiateDataPath: error: " + statusString(status));
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "initiateDataPath: exception: " + e);
                return false;
            }
        }
    }

    public boolean respondToDataPathRequest(short transactionId, boolean accept, int ndpId, String interfaceName, byte[] pmk, String passphrase, boolean isOutOfBand, Capabilities capabilities) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "respondToDataPathRequest: null interface");
            return false;
        } else if (capabilities == null) {
            Log.e(TAG, "initiateDataPath: null capabilities");
            return false;
        } else {
            NanRespondToDataPathIndicationRequest req = new NanRespondToDataPathIndicationRequest();
            req.acceptRequest = accept;
            req.ndpInstanceId = ndpId;
            req.ifaceName = interfaceName;
            req.securityConfig.securityType = 0;
            if (!(pmk == null || pmk.length == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
                req.securityConfig.securityType = 1;
                copyArray(pmk, req.securityConfig.pmk);
            }
            if (!(passphrase == null || passphrase.length() == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
                req.securityConfig.securityType = 2;
                convertNativeByteArrayToArrayList(passphrase.getBytes(), req.securityConfig.passphrase);
            }
            if (req.securityConfig.securityType != 0 && isOutOfBand) {
                convertNativeByteArrayToArrayList(SERVICE_NAME_FOR_OOB_DATA_PATH.getBytes(StandardCharsets.UTF_8), req.serviceNameOutOfBand);
            }
            try {
                WifiStatus status = iface.respondToDataPathIndicationRequest(transactionId, req);
                if (status.code == 0) {
                    return true;
                }
                Log.e(TAG, "respondToDataPathRequest: error: " + statusString(status));
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "respondToDataPathRequest: exception: " + e);
                return false;
            }
        }
    }

    public boolean endDataPath(short transactionId, int ndpId) {
        IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "endDataPath: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.terminateDataPathRequest(transactionId, ndpId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "endDataPath: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "endDataPath: exception: " + e);
            return false;
        }
    }

    private void updateConfigForPowerSettings(NanConfigRequest req, boolean isInteractive, boolean isIdle) {
        if (isIdle) {
            updateSingleConfigForPowerSettings(req.bandSpecificConfig[1], ((Integer) this.mSettableParameters.get(PARAM_DW_ON_IDLE_5GHZ)).intValue());
            updateSingleConfigForPowerSettings(req.bandSpecificConfig[0], ((Integer) this.mSettableParameters.get(PARAM_DW_ON_IDLE_24GHZ)).intValue());
        } else if (isInteractive) {
            updateSingleConfigForPowerSettings(req.bandSpecificConfig[1], ((Integer) this.mSettableParameters.get(PARAM_DW_DEFAULT_5GHZ)).intValue());
            updateSingleConfigForPowerSettings(req.bandSpecificConfig[0], ((Integer) this.mSettableParameters.get(PARAM_DW_DEFAULT_24GHZ)).intValue());
        } else {
            updateSingleConfigForPowerSettings(req.bandSpecificConfig[1], ((Integer) this.mSettableParameters.get(PARAM_DW_ON_INACTIVE_5GHZ)).intValue());
            updateSingleConfigForPowerSettings(req.bandSpecificConfig[0], ((Integer) this.mSettableParameters.get(PARAM_DW_ON_INACTIVE_24GHZ)).intValue());
        }
    }

    private void updateSingleConfigForPowerSettings(NanBandSpecificConfig cfg, int override) {
        if (override != -1) {
            cfg.validDiscoveryWindowIntervalVal = true;
            cfg.discoveryWindowIntervalVal = (byte) override;
        }
    }

    private int getStrongestCipherSuiteType(int supportedCipherSuites) {
        if ((supportedCipherSuites & 2) != 0) {
            return 2;
        }
        if ((supportedCipherSuites & 1) != 0) {
            return 1;
        }
        return 0;
    }

    private ArrayList<Byte> convertNativeByteArrayToArrayList(byte[] from, ArrayList<Byte> to) {
        if (from == null) {
            from = new byte[0];
        }
        if (to == null) {
            to = new ArrayList(from.length);
        } else {
            to.ensureCapacity(from.length);
        }
        for (byte valueOf : from) {
            to.add(Byte.valueOf(valueOf));
        }
        return to;
    }

    private void copyArray(byte[] from, byte[] to) {
        if (from == null || to == null || from.length != to.length) {
            Log.e(TAG, "copyArray error: from=" + from + ", to=" + to);
            return;
        }
        for (int i = 0; i < from.length; i++) {
            to[i] = from[i];
        }
    }

    private static String statusString(WifiStatus status) {
        if (status == null) {
            return "status=null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(status.code).append(" (").append(status.description).append(")");
        return sb.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareNativeApi:");
        pw.println("  mSettableParameters: " + this.mSettableParameters);
        this.mHal.dump(fd, pw, args);
    }
}
