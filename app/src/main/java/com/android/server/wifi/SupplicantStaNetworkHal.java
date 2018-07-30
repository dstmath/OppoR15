package com.android.server.wifi;

import android.content.Context;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.KeyMgmtMask;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback.NetworkRequestEapSimGsmAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback.NetworkRequestEapSimUmtsAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback.Stub;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableBoolean;
import com.android.internal.util.ArrayUtils;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass1;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass10;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass11;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass12;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass13;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass14;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass15;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass16;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass17;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass18;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass19;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass2;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass20;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass21;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass22;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass23;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass24;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass25;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass26;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass27;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass28;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass29;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass3;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass30;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass31;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass32;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass33;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass4;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass5;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass6;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass7;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass8;
import com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE.AnonymousClass9;
import com.android.server.wifi.WifiBackupRestore.SupplicantBackupMigration;
import com.android.server.wifi.util.NativeUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.concurrent.ThreadSafe;
import org.json.JSONException;
import org.json.JSONObject;
import vendor.qti.hardware.wifi.supplicant.V1_0.ISupplicantVendorStaNetwork;
import vendor.qti.hardware.wifi.supplicant.V1_0.ISupplicantVendorStaNetwork.VendorKeyMgmtMask;

@ThreadSafe
public class SupplicantStaNetworkHal {
    private static final Pattern GSM_AUTH_RESPONSE_PARAMS_PATTERN = Pattern.compile(":([0-9a-fA-F]+):([0-9a-fA-F]+)");
    public static final String ID_STRING_KEY_CONFIG_KEY = "configKey";
    public static final String ID_STRING_KEY_CREATOR_UID = "creatorUid";
    public static final String ID_STRING_KEY_FQDN = "fqdn";
    private static final String TAG = "SupplicantStaNetworkHal";
    private static final Pattern UMTS_AUTH_RESPONSE_PARAMS_PATTERN = Pattern.compile("^:([0-9a-fA-F]+):([0-9a-fA-F]+):([0-9a-fA-F]+)$");
    private static final Pattern UMTS_AUTS_RESPONSE_PARAMS_PATTERN = Pattern.compile("^:([0-9a-fA-F]+)$");
    private int mAuthAlgMask;
    private byte[] mBssid;
    private String mEapAltSubjectMatch;
    private ArrayList<Byte> mEapAnonymousIdentity;
    private String mEapCACert;
    private String mEapCAPath;
    private String mEapClientCert;
    private String mEapDomainSuffixMatch;
    private boolean mEapEngine;
    private String mEapEngineID;
    private ArrayList<Byte> mEapIdentity;
    private int mEapMethod;
    private ArrayList<Byte> mEapPassword;
    private int mEapPhase2Method;
    private String mEapPrivateKeyId;
    private String mEapSubjectMatch;
    private int mGroupCipherMask;
    private ISupplicantStaNetwork mISupplicantStaNetwork;
    private ISupplicantStaNetworkCallback mISupplicantStaNetworkCallback;
    private ISupplicantVendorStaNetwork mISupplicantVendorStaNetwork;
    private String mIdStr;
    private final String mIfaceName;
    private int mKeyMgmtMask;
    private final Object mLock = new Object();
    private int mNetworkId;
    private int mPairwiseCipherMask;
    private int mProtoMask;
    private byte[] mPsk;
    private String mPskPassphrase;
    private boolean mRequirePmf;
    private boolean mScanSsid;
    private ArrayList<Byte> mSsid;
    private boolean mSystemSupportsFastBssTransition = false;
    private boolean mVerboseLoggingEnabled = false;
    private String mWapiCertSel;
    private int mWapiCertSelMode;
    private String mWapiPsk;
    private int mWapiPskType;
    private ArrayList<Byte> mWepKey;
    private int mWepTxKeyIdx;
    private final WifiMonitor mWifiMonitor;

    private static class Mutable<E> {
        public E value;

        Mutable() {
            this.value = null;
        }

        Mutable(E value) {
            this.value = value;
        }
    }

    private class SupplicantStaNetworkHalCallback extends Stub {
        private final int mFramewokNetworkId;
        private final String mSsid;

        SupplicantStaNetworkHalCallback(int framewokNetworkId, String ssid) {
            this.mFramewokNetworkId = framewokNetworkId;
            this.mSsid = ssid;
        }

        public void onNetworkEapSimGsmAuthRequest(NetworkRequestEapSimGsmAuthParams params) {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapSimGsmAuthRequest");
                String[] data = new String[params.rands.size()];
                int i = 0;
                Iterator rand$iterator = params.rands.iterator();
                while (true) {
                    int i2 = i;
                    if (rand$iterator.hasNext()) {
                        i = i2 + 1;
                        data[i2] = NativeUtil.hexStringFromByteArray((byte[]) rand$iterator.next());
                    } else {
                        SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkGsmAuthRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid, data);
                    }
                }
            }
        }

        public void onNetworkEapSimUmtsAuthRequest(NetworkRequestEapSimUmtsAuthParams params) {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapSimUmtsAuthRequest");
                String randHex = NativeUtil.hexStringFromByteArray(params.rand);
                String autnHex = NativeUtil.hexStringFromByteArray(params.autn);
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkUmtsAuthRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid, new String[]{randHex, autnHex});
            }
        }

        public void onNetworkEapIdentityRequest() {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapIdentityRequest");
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkIdentityRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid);
            }
        }
    }

    SupplicantStaNetworkHal(ISupplicantStaNetwork iSupplicantStaNetwork, ISupplicantVendorStaNetwork iSupplicantVendorStaNetwork, String ifaceName, Context context, WifiMonitor monitor) {
        this.mISupplicantStaNetwork = iSupplicantStaNetwork;
        this.mISupplicantVendorStaNetwork = iSupplicantVendorStaNetwork;
        this.mIfaceName = ifaceName;
        this.mWifiMonitor = monitor;
        this.mSystemSupportsFastBssTransition = context.getResources().getBoolean(17957068);
    }

    void enableVerboseLogging(boolean enable) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = enable;
        }
    }

    public boolean loadWifiConfiguration(WifiConfiguration config, Map<String, String> networkExtras) {
        synchronized (this.mLock) {
            if (config == null) {
                return false;
            }
            config.SSID = null;
            if (!getSsid() || (ArrayUtils.isEmpty(this.mSsid) ^ 1) == 0) {
                Log.e(TAG, "failed to read ssid");
                return false;
            }
            config.SSID = NativeUtil.hexStringFromByteArray(NativeUtil.byteArrayFromArrayList(this.mSsid));
            config.networkId = -1;
            if (getId()) {
                config.networkId = this.mNetworkId;
                config.getNetworkSelectionStatus().setNetworkSelectionBSSID(null);
                if (getBssid() && (ArrayUtils.isEmpty(this.mBssid) ^ 1) != 0) {
                    config.getNetworkSelectionStatus().setNetworkSelectionBSSID(NativeUtil.macAddressFromByteArray(this.mBssid));
                }
                config.hiddenSSID = false;
                if (getScanSsid()) {
                    config.hiddenSSID = this.mScanSsid;
                }
                config.requirePMF = false;
                if (getRequirePmf()) {
                    config.requirePMF = this.mRequirePmf;
                }
                config.wepTxKeyIndex = -1;
                if (getWepTxKeyIdx()) {
                    config.wepTxKeyIndex = this.mWepTxKeyIdx;
                }
                for (int i = 0; i < 4; i++) {
                    config.wepKeys[i] = null;
                    if (getWepKey(i) && (ArrayUtils.isEmpty(this.mWepKey) ^ 1) != 0) {
                        config.wepKeys[i] = NativeUtil.bytesToHexOrQuotedString(this.mWepKey);
                    }
                }
                config.preSharedKey = null;
                if (getPskPassphrase() && (TextUtils.isEmpty(this.mPskPassphrase) ^ 1) != 0) {
                    config.preSharedKey = NativeUtil.addEnclosingQuotes(this.mPskPassphrase);
                } else if (getPsk() && (ArrayUtils.isEmpty(this.mPsk) ^ 1) != 0) {
                    config.preSharedKey = NativeUtil.hexStringFromByteArray(this.mPsk);
                }
                if (getKeyMgmt()) {
                    config.allowedKeyManagement = removeFastTransitionFlags(supplicantToWifiConfigurationKeyMgmtMask(this.mKeyMgmtMask));
                }
                if (config.allowedKeyManagement.get(10)) {
                    if (getWapiPskType()) {
                        config.wapiPskType = this.mWapiPskType;
                    }
                    if (config.wapiPskType == 0) {
                        if (getWapiPsk()) {
                            config.wapiPsk = NativeUtil.addEnclosingQuotes(this.mWapiPsk);
                        }
                    } else if (getWapiPsk()) {
                        config.wapiPsk = this.mWapiPsk;
                    }
                } else if (config.allowedKeyManagement.get(11)) {
                    if (getWapiCertSelMode()) {
                        config.wapiCertSelMode = this.mWapiCertSelMode;
                    }
                    if (config.wapiCertSelMode == 1 && getWapiCertSel()) {
                        config.wapiCertSel = this.mWapiCertSel;
                    }
                }
                if (getProto()) {
                    config.allowedProtocols = supplicantToWifiConfigurationProtoMask(this.mProtoMask);
                }
                if (getAuthAlg()) {
                    config.allowedAuthAlgorithms = supplicantToWifiConfigurationAuthAlgMask(this.mAuthAlgMask);
                }
                if (getGroupCipher()) {
                    config.allowedGroupCiphers = supplicantToWifiConfigurationGroupCipherMask(this.mGroupCipherMask);
                }
                if (getPairwiseCipher()) {
                    config.allowedPairwiseCiphers = supplicantToWifiConfigurationPairwiseCipherMask(this.mPairwiseCipherMask);
                }
                if (!getIdStr() || (TextUtils.isEmpty(this.mIdStr) ^ 1) == 0) {
                    Log.w(TAG, "getIdStr failed or empty");
                } else {
                    networkExtras.putAll(parseNetworkExtra(this.mIdStr));
                }
                boolean loadWifiEnterpriseConfig = loadWifiEnterpriseConfig(config.SSID, config.enterpriseConfig);
                return loadWifiEnterpriseConfig;
            }
            Log.e(TAG, "getId failed");
            return false;
        }
    }

    public boolean saveWifiConfiguration(WifiConfiguration config) {
        synchronized (this.mLock) {
            if (config == null) {
                return false;
            }
            String bssidStr = config.getNetworkSelectionStatus().getNetworkSelectionBSSID();
            if (bssidStr != null && !setBssid(NativeUtil.macAddressToByteArray(bssidStr))) {
                Log.e(TAG, "failed to set BSSID: " + bssidStr);
                return false;
            } else if (config.SSID == null || setSsid(NativeUtil.decodeSsid(config.SSID))) {
                if (!(config.preSharedKey == null || (config.preSharedKey.equals("*") ^ 1) == 0)) {
                    if (config.preSharedKey.startsWith("\"")) {
                        if (!setPskPassphrase(NativeUtil.removeEnclosingQuotes(config.preSharedKey))) {
                            Log.e(TAG, "failed to set psk passphrase");
                            return false;
                        }
                    } else if (!setPsk(NativeUtil.hexStringToByteArray(config.preSharedKey))) {
                        Log.e(TAG, "failed to set psk");
                        return false;
                    }
                }
                boolean hasSetKey = false;
                if (config.wepKeys != null) {
                    int i = 0;
                    while (i < config.wepKeys.length) {
                        if (!(config.wepKeys[i] == null || (config.wepKeys[i].equals("") ^ 1) == 0)) {
                            if (setWepKey(i, NativeUtil.hexOrQuotedStringToBytes(config.wepKeys[i]))) {
                                hasSetKey = true;
                            } else {
                                Log.e(TAG, "failed to set wep_key " + i);
                                this.mWifiMonitor.broadcastSaveConfigFailed(this.mIfaceName, config.networkId);
                                return false;
                            }
                        }
                        i++;
                    }
                }
                if (hasSetKey) {
                    if (!setWepTxKeyIdx(config.wepTxKeyIndex)) {
                        Log.e(TAG, "failed to set wep_tx_keyidx: " + config.wepTxKeyIndex);
                        return false;
                    }
                }
                if (!setScanSsid(config.hiddenSSID)) {
                    Log.e(TAG, config.SSID + ": failed to set hiddenSSID: " + config.hiddenSSID);
                    return false;
                } else if (setRequirePmf(config.requirePMF)) {
                    if (config.allowedKeyManagement.cardinality() != 0) {
                        BitSet keyMgmtMask = addFastTransitionFlags(config.allowedKeyManagement);
                        if (!setKeyMgmt(wifiConfigurationToSupplicantKeyMgmtMask(keyMgmtMask))) {
                            Log.e(TAG, "failed to set Key Management");
                            return false;
                        } else if (keyMgmtMask.get(8) || keyMgmtMask.get(9)) {
                            config.enterpriseConfig.setFieldValue("eap_erp", "1");
                        }
                    }
                    if (config.allowedKeyManagement.get(10)) {
                        if (!setWapiPskType(config.wapiPskType)) {
                            Log.e(TAG, "failed to set wapiPskType");
                            return false;
                        } else if (config.wapiPsk != null) {
                            if (config.wapiPsk.startsWith("\"")) {
                                if (!setWapiPsk(NativeUtil.removeEnclosingQuotes(config.wapiPsk))) {
                                    Log.e(TAG, "failed to set wapiPsk with quotes");
                                    return false;
                                }
                            } else if (!setWapiPsk(config.wapiPsk)) {
                                Log.e(TAG, "failed to set wapiPsk");
                                return false;
                            }
                        }
                    } else if (config.allowedKeyManagement.get(11)) {
                        if (config.wapiCertSelMode != -1 && (setWapiCertSelMode(config.wapiCertSelMode) ^ 1) != 0) {
                            Log.e(TAG, "failed to set wapiCertSelMode");
                            return false;
                        } else if (config.wapiCertSelMode == 1 && !setWapiCertSel(config.wapiCertSel)) {
                            Log.e(TAG, "failed to set wapiCertSel");
                            return false;
                        }
                    }
                    if (config.allowedProtocols.cardinality() != 0 && (setProto(wifiConfigurationToSupplicantProtoMask(config.allowedProtocols)) ^ 1) != 0) {
                        Log.e(TAG, "failed to set Security Protocol");
                        return false;
                    } else if (config.allowedAuthAlgorithms.cardinality() != 0 && (setAuthAlg(wifiConfigurationToSupplicantAuthAlgMask(config.allowedAuthAlgorithms)) ^ 1) != 0) {
                        Log.e(TAG, "failed to set AuthAlgorithm");
                        return false;
                    } else if (config.allowedGroupCiphers.cardinality() != 0 && (setGroupCipher(wifiConfigurationToSupplicantGroupCipherMask(config.allowedGroupCiphers)) ^ 1) != 0) {
                        Log.e(TAG, "failed to set Group Cipher");
                        return false;
                    } else if (config.allowedPairwiseCiphers.cardinality() == 0 || (setPairwiseCipher(wifiConfigurationToSupplicantPairwiseCipherMask(config.allowedPairwiseCiphers)) ^ 1) == 0) {
                        Map<String, String> metadata = new HashMap();
                        if (config.isPasspoint()) {
                            metadata.put(ID_STRING_KEY_FQDN, config.FQDN);
                        }
                        metadata.put(ID_STRING_KEY_CONFIG_KEY, config.configKey());
                        metadata.put(ID_STRING_KEY_CREATOR_UID, Integer.toString(config.creatorUid));
                        if (!setIdStr(createNetworkExtra(metadata))) {
                            Log.e(TAG, "failed to set id string");
                            return false;
                        } else if (config.updateIdentifier != null && (setUpdateIdentifier(Integer.parseInt(config.updateIdentifier)) ^ 1) != 0) {
                            Log.e(TAG, "failed to set update identifier");
                            return false;
                        } else if (config.enterpriseConfig == null || config.enterpriseConfig.getEapMethod() == -1 || saveWifiEnterpriseConfig(config.SSID, config.enterpriseConfig)) {
                            this.mISupplicantStaNetworkCallback = new SupplicantStaNetworkHalCallback(config.networkId, config.SSID);
                            if (registerCallback(this.mISupplicantStaNetworkCallback)) {
                                return true;
                            }
                            Log.e(TAG, "Failed to register callback");
                            return false;
                        } else {
                            return false;
                        }
                    } else {
                        Log.e(TAG, "failed to set PairwiseCipher");
                        return false;
                    }
                } else {
                    Log.e(TAG, config.SSID + ": failed to set requirePMF: " + config.requirePMF);
                    return false;
                }
            } else {
                Log.e(TAG, "failed to set SSID: " + config.SSID);
                return false;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean loadWifiEnterpriseConfig(String ssid, WifiEnterpriseConfig eapConfig) {
        synchronized (this.mLock) {
            if (eapConfig == null) {
                return false;
            } else if (getEapMethod()) {
                eapConfig.setEapMethod(supplicantToWifiConfigurationEapMethod(this.mEapMethod));
                if (getEapPhase2Method()) {
                    eapConfig.setPhase2Method(supplicantToWifiConfigurationEapPhase2Method(this.mEapPhase2Method));
                    if (getEapIdentity() && (ArrayUtils.isEmpty(this.mEapIdentity) ^ 1) != 0) {
                        eapConfig.setFieldValue("identity", NativeUtil.stringFromByteArrayList(this.mEapIdentity));
                    }
                    if (getEapAnonymousIdentity() && (ArrayUtils.isEmpty(this.mEapAnonymousIdentity) ^ 1) != 0) {
                        eapConfig.setFieldValue("anonymous_identity", NativeUtil.stringFromByteArrayList(this.mEapAnonymousIdentity));
                    }
                    if (getEapPassword() && (ArrayUtils.isEmpty(this.mEapPassword) ^ 1) != 0) {
                        eapConfig.setFieldValue("password", NativeUtil.stringFromByteArrayList(this.mEapPassword));
                    }
                    if (getEapClientCert() && (TextUtils.isEmpty(this.mEapClientCert) ^ 1) != 0) {
                        eapConfig.setFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CLIENT_CERT, this.mEapClientCert);
                    }
                    if (getEapCACert() && (TextUtils.isEmpty(this.mEapCACert) ^ 1) != 0) {
                        eapConfig.setFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CA_CERT, this.mEapCACert);
                    }
                    if (getEapSubjectMatch() && (TextUtils.isEmpty(this.mEapSubjectMatch) ^ 1) != 0) {
                        eapConfig.setFieldValue("subject_match", this.mEapSubjectMatch);
                    }
                    if (getEapEngineID() && (TextUtils.isEmpty(this.mEapEngineID) ^ 1) != 0) {
                        eapConfig.setFieldValue("engine_id", this.mEapEngineID);
                    }
                    if (getEapEngine() && (TextUtils.isEmpty(this.mEapEngineID) ^ 1) != 0) {
                        String str;
                        String str2 = "engine";
                        if (this.mEapEngine) {
                            str = "1";
                        } else {
                            str = "0";
                        }
                        eapConfig.setFieldValue(str2, str);
                    }
                    if (getEapPrivateKeyId() && (TextUtils.isEmpty(this.mEapPrivateKeyId) ^ 1) != 0) {
                        eapConfig.setFieldValue("key_id", this.mEapPrivateKeyId);
                    }
                    if (getEapAltSubjectMatch() && (TextUtils.isEmpty(this.mEapAltSubjectMatch) ^ 1) != 0) {
                        eapConfig.setFieldValue("altsubject_match", this.mEapAltSubjectMatch);
                    }
                    if (getEapDomainSuffixMatch() && (TextUtils.isEmpty(this.mEapDomainSuffixMatch) ^ 1) != 0) {
                        eapConfig.setFieldValue("domain_suffix_match", this.mEapDomainSuffixMatch);
                    }
                    if (getEapCAPath() && (TextUtils.isEmpty(this.mEapCAPath) ^ 1) != 0) {
                        eapConfig.setFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CA_PATH, this.mEapCAPath);
                    }
                } else {
                    Log.e(TAG, "failed to get eap phase2 method");
                    return false;
                }
            } else {
                Log.e(TAG, "failed to get eap method. Assumimg not an enterprise network");
                return true;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean saveWifiEnterpriseConfig(String ssid, WifiEnterpriseConfig eapConfig) {
        synchronized (this.mLock) {
            if (eapConfig == null) {
                return false;
            } else if (!setEapMethod(wifiConfigurationToSupplicantEapMethod(eapConfig.getEapMethod()))) {
                Log.e(TAG, ssid + ": failed to set eap method: " + eapConfig.getEapMethod());
                return false;
            } else if (setEapPhase2Method(wifiConfigurationToSupplicantEapPhase2Method(eapConfig.getPhase2Method()))) {
                String eapParam = eapConfig.getFieldValue("identity");
                if (TextUtils.isEmpty(eapParam) || (setEapIdentity(NativeUtil.stringToByteArrayList(eapParam)) ^ 1) == 0) {
                    eapParam = eapConfig.getFieldValue("anonymous_identity");
                    if (TextUtils.isEmpty(eapParam) || (setEapAnonymousIdentity(NativeUtil.stringToByteArrayList(eapParam)) ^ 1) == 0) {
                        eapParam = eapConfig.getFieldValue("password");
                        if (TextUtils.isEmpty(eapParam) || (setEapPassword(NativeUtil.stringToByteArrayList(eapParam)) ^ 1) == 0) {
                            eapParam = eapConfig.getFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CLIENT_CERT);
                            if (TextUtils.isEmpty(eapParam) || (setEapClientCert(eapParam) ^ 1) == 0) {
                                eapParam = eapConfig.getFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CA_CERT);
                                if (TextUtils.isEmpty(eapParam) || (setEapCACert(eapParam) ^ 1) == 0) {
                                    eapParam = eapConfig.getFieldValue("subject_match");
                                    if (TextUtils.isEmpty(eapParam) || (setEapSubjectMatch(eapParam) ^ 1) == 0) {
                                        eapParam = eapConfig.getFieldValue("engine_id");
                                        if (TextUtils.isEmpty(eapParam) || (setEapEngineID(eapParam) ^ 1) == 0) {
                                            boolean z;
                                            eapParam = eapConfig.getFieldValue("engine");
                                            if (!TextUtils.isEmpty(eapParam)) {
                                                if (eapParam.equals("1")) {
                                                    z = true;
                                                } else {
                                                    z = false;
                                                }
                                                if ((setEapEngine(z) ^ 1) != 0) {
                                                    Log.e(TAG, ssid + ": failed to set eap engine: " + eapParam);
                                                    return false;
                                                }
                                            }
                                            eapParam = eapConfig.getFieldValue("key_id");
                                            if (TextUtils.isEmpty(eapParam) || (setEapPrivateKeyId(eapParam) ^ 1) == 0) {
                                                eapParam = eapConfig.getFieldValue("altsubject_match");
                                                if (TextUtils.isEmpty(eapParam) || (setEapAltSubjectMatch(eapParam) ^ 1) == 0) {
                                                    eapParam = eapConfig.getFieldValue("domain_suffix_match");
                                                    if (TextUtils.isEmpty(eapParam) || (setEapDomainSuffixMatch(eapParam) ^ 1) == 0) {
                                                        eapParam = eapConfig.getFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CA_PATH);
                                                        if (TextUtils.isEmpty(eapParam) || (setEapCAPath(eapParam) ^ 1) == 0) {
                                                            eapParam = eapConfig.getFieldValue("sim_num");
                                                            if (TextUtils.isEmpty(eapParam) || (setSimNumber(Integer.parseInt(eapParam)) ^ 1) == 0) {
                                                                eapParam = eapConfig.getFieldValue("proactive_key_caching");
                                                                if (!TextUtils.isEmpty(eapParam)) {
                                                                    if (eapParam.equals("1")) {
                                                                        z = true;
                                                                    } else {
                                                                        z = false;
                                                                    }
                                                                    if ((setEapProactiveKeyCaching(z) ^ 1) != 0) {
                                                                        Log.e(TAG, ssid + ": failed to set proactive key caching: " + eapParam);
                                                                        return false;
                                                                    }
                                                                }
                                                                eapParam = eapConfig.getFieldValue("eap_erp");
                                                                if (!TextUtils.isEmpty(eapParam) && eapParam.equals("1")) {
                                                                    if (!setEapErp(true)) {
                                                                        Log.e(TAG, ssid + ": failed to set eap erp");
                                                                        return false;
                                                                    } else if (!setAuthAlg(0)) {
                                                                        Log.e(TAG, ssid + ": failed to reset AuthAlgorithm");
                                                                        return false;
                                                                    }
                                                                }
                                                                eapParam = eapConfig.getFieldValue("sim_num");
                                                                if (TextUtils.isEmpty(eapParam) || (setSimNumber(Integer.parseInt(eapParam)) ^ 1) == 0) {
                                                                } else {
                                                                    Log.e(TAG, ssid + ": failed to set SIM number : " + eapParam);
                                                                    return false;
                                                                }
                                                            }
                                                            Log.e(TAG, ssid + ": failed to set SIM number : " + eapParam);
                                                            return false;
                                                        }
                                                        Log.e(TAG, ssid + ": failed to set eap ca path: " + eapParam);
                                                        return false;
                                                    }
                                                    Log.e(TAG, ssid + ": failed to set eap domain suffix match: " + eapParam);
                                                    return false;
                                                }
                                                Log.e(TAG, ssid + ": failed to set eap alt subject match: " + eapParam);
                                                return false;
                                            }
                                            Log.e(TAG, ssid + ": failed to set eap private key: " + eapParam);
                                            return false;
                                        }
                                        Log.e(TAG, ssid + ": failed to set eap engine id: " + eapParam);
                                        return false;
                                    }
                                    Log.e(TAG, ssid + ": failed to set eap subject match: " + eapParam);
                                    return false;
                                }
                                Log.e(TAG, ssid + ": failed to set eap ca cert: " + eapParam);
                                return false;
                            }
                            Log.e(TAG, ssid + ": failed to set eap client cert: " + eapParam);
                            return false;
                        }
                        Log.e(TAG, ssid + ": failed to set eap password");
                        return false;
                    }
                    Log.e(TAG, ssid + ": failed to set eap anonymous identity: " + eapParam);
                    return false;
                }
                Log.e(TAG, ssid + ": failed to set eap identity: " + eapParam);
                return false;
            } else {
                Log.e(TAG, ssid + ": failed to set eap phase 2 method: " + eapConfig.getPhase2Method());
                return false;
            }
        }
    }

    private static int wifiConfigurationToSupplicantKeyMgmtMask(BitSet keyMgmt) {
        int mask = 0;
        int bit = keyMgmt.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 4;
                    break;
                case 1:
                    mask |= 2;
                    break;
                case 2:
                    mask |= 1;
                    break;
                case 3:
                    mask |= 8;
                    break;
                case 5:
                    mask |= KeyMgmtMask.OSEN;
                    break;
                case 6:
                    mask |= 64;
                    break;
                case 7:
                    mask |= 32;
                    break;
                case 8:
                    mask |= VendorKeyMgmtMask.FILS_SHA256;
                    break;
                case 9:
                    mask |= VendorKeyMgmtMask.FILS_SHA384;
                    break;
                case 10:
                    mask |= 4096;
                    break;
                case 11:
                    mask |= 8192;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid protoMask bit in keyMgmt: " + bit);
            }
            bit = keyMgmt.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantProtoMask(BitSet protoMask) {
        int mask = 0;
        int bit = protoMask.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 1;
                    break;
                case 1:
                    mask |= 2;
                    break;
                case 2:
                    mask |= 8;
                    break;
                case 3:
                    mask |= 4;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid protoMask bit in wificonfig: " + bit);
            }
            bit = protoMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantAuthAlgMask(BitSet authAlgMask) {
        int mask = 0;
        int bit = authAlgMask.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 1;
                    break;
                case 1:
                    mask |= 2;
                    break;
                case 2:
                    mask |= 4;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid authAlgMask bit in wificonfig: " + bit);
            }
            bit = authAlgMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantGroupCipherMask(BitSet groupCipherMask) {
        int mask = 0;
        int bit = groupCipherMask.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 2;
                    break;
                case 1:
                    mask |= 4;
                    break;
                case 2:
                    mask |= 8;
                    break;
                case 3:
                    mask |= 16;
                    break;
                case 4:
                    mask |= 16384;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid GroupCipherMask bit in wificonfig: " + bit);
            }
            bit = groupCipherMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantPairwiseCipherMask(BitSet pairwiseCipherMask) {
        int mask = 0;
        int bit = pairwiseCipherMask.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 1;
                    break;
                case 1:
                    mask |= 8;
                    break;
                case 2:
                    mask |= 16;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid pairwiseCipherMask bit in wificonfig: " + bit);
            }
            bit = pairwiseCipherMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int supplicantToWifiConfigurationEapMethod(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap method value from supplicant: " + value);
                return -1;
        }
    }

    private static int supplicantToWifiConfigurationEapPhase2Method(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap phase2 method value from supplicant: " + value);
                return -1;
        }
    }

    private static int supplicantMaskValueToWifiConfigurationBitSet(int supplicantMask, int supplicantValue, BitSet bitset, int bitSetPosition) {
        bitset.set(bitSetPosition, (supplicantMask & supplicantValue) == supplicantValue);
        return supplicantMask & (~supplicantValue);
    }

    private static BitSet supplicantToWifiConfigurationKeyMgmtMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 4, bitset, 0), 2, bitset, 1), 1, bitset, 2), 8, bitset, 3), KeyMgmtMask.OSEN, bitset, 5), 64, bitset, 6), 32, bitset, 7), 4096, bitset, 10), 8192, bitset, 11);
        if (mask == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid key mgmt mask from supplicant: " + mask);
    }

    private static BitSet supplicantToWifiConfigurationProtoMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 1, bitset, 0), 2, bitset, 1), 8, bitset, 2), 4, bitset, 3);
        if (mask == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid proto mask from supplicant: " + mask);
    }

    private static BitSet supplicantToWifiConfigurationAuthAlgMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 1, bitset, 0), 2, bitset, 1), 4, bitset, 2);
        if (mask == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid auth alg mask from supplicant: " + mask);
    }

    private static BitSet supplicantToWifiConfigurationGroupCipherMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 2, bitset, 0), 4, bitset, 1), 8, bitset, 2), 16, bitset, 3), 16384, bitset, 4);
        if (mask == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid group cipher mask from supplicant: " + mask);
    }

    private static BitSet supplicantToWifiConfigurationPairwiseCipherMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 1, bitset, 0), 8, bitset, 1), 16, bitset, 2);
        if (mask == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid pairwise cipher mask from supplicant: " + mask);
    }

    private static int wifiConfigurationToSupplicantEapMethod(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap method value from WifiConfiguration: " + value);
                return -1;
        }
    }

    private static int wifiConfigurationToSupplicantEapPhase2Method(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap phase2 method value from WifiConfiguration: " + value);
                return -1;
        }
    }

    private boolean getId() {
        synchronized (this.mLock) {
            String methodStr = "getId";
            if (checkISupplicantStaNetworkAndLogFailure("getId")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getId(new -$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getId");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_52606(MutableBoolean statusOk, SupplicantStatus status, int idValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mNetworkId = idValue;
        } else {
            checkStatusAndLogFailure(status, "getId");
        }
    }

    private boolean registerCallback(ISupplicantStaNetworkCallback callback) {
        synchronized (this.mLock) {
            String methodStr = "registerCallback";
            if (checkISupplicantStaNetworkAndLogFailure("registerCallback")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.registerCallback(callback), "registerCallback");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "registerCallback");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setSsid(ArrayList<Byte> ssid) {
        synchronized (this.mLock) {
            String methodStr = "setSsid";
            if (checkISupplicantStaNetworkAndLogFailure("setSsid")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setSsid(ssid), "setSsid");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setSsid");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setBssid(String bssidStr) {
        boolean bssid;
        synchronized (this.mLock) {
            try {
                bssid = setBssid(NativeUtil.macAddressToByteArray(bssidStr));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssidStr, e);
                return false;
            }
        }
        return bssid;
    }

    private boolean setBssid(byte[] bssid) {
        synchronized (this.mLock) {
            String methodStr = "setBssid";
            if (checkISupplicantStaNetworkAndLogFailure("setBssid")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setBssid(bssid), "setBssid");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setBssid");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setScanSsid(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setScanSsid";
            if (checkISupplicantStaNetworkAndLogFailure("setScanSsid")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setScanSsid(enable), "setScanSsid");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setScanSsid");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setKeyMgmt(int keyMgmtMask) {
        synchronized (this.mLock) {
            String methodStr = "setKeyMgmt";
            if (checkISupplicantStaNetworkAndLogFailure("setKeyMgmt")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setKeyMgmt(keyMgmtMask), "setKeyMgmt");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setKeyMgmt");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setProto(int protoMask) {
        synchronized (this.mLock) {
            String methodStr = "setProto";
            if (checkISupplicantStaNetworkAndLogFailure("setProto")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setProto(protoMask), "setProto");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setProto");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setAuthAlg(int authAlgMask) {
        synchronized (this.mLock) {
            String methodStr = "setAuthAlg";
            if (checkISupplicantStaNetworkAndLogFailure("setAuthAlg")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setAuthAlg(authAlgMask), "setAuthAlg");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setAuthAlg");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setGroupCipher(int groupCipherMask) {
        synchronized (this.mLock) {
            String methodStr = "setGroupCipher";
            if (checkISupplicantStaNetworkAndLogFailure("setGroupCipher")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setGroupCipher(groupCipherMask), "setGroupCipher");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setGroupCipher");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setPairwiseCipher(int pairwiseCipherMask) {
        synchronized (this.mLock) {
            String methodStr = "setPairwiseCipher";
            if (checkISupplicantStaNetworkAndLogFailure("setPairwiseCipher")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPairwiseCipher(pairwiseCipherMask), "setPairwiseCipher");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setPairwiseCipher");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setPskPassphrase(String psk) {
        synchronized (this.mLock) {
            String methodStr = "setPskPassphrase";
            if (checkISupplicantStaNetworkAndLogFailure("setPskPassphrase")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPskPassphrase(psk), "setPskPassphrase");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setPskPassphrase");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setPsk(byte[] psk) {
        synchronized (this.mLock) {
            String methodStr = "setPsk";
            if (checkISupplicantStaNetworkAndLogFailure("setPsk")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPsk(psk), "setPsk");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setPsk");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWepKey(int keyIdx, ArrayList<Byte> wepKey) {
        synchronized (this.mLock) {
            String methodStr = "setWepKey";
            if (checkISupplicantStaNetworkAndLogFailure("setWepKey")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setWepKey(keyIdx, wepKey), "setWepKey");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWepKey");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWepTxKeyIdx(int keyIdx) {
        synchronized (this.mLock) {
            String methodStr = "setWepTxKeyIdx";
            if (checkISupplicantStaNetworkAndLogFailure("setWepTxKeyIdx")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setWepTxKeyIdx(keyIdx), "setWepTxKeyIdx");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWepTxKeyIdx");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setRequirePmf(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setRequirePmf";
            if (checkISupplicantStaNetworkAndLogFailure("setRequirePmf")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setRequirePmf(enable), "setRequirePmf");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setRequirePmf");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setUpdateIdentifier(int identifier) {
        synchronized (this.mLock) {
            String methodStr = "setUpdateIdentifier";
            if (checkISupplicantStaNetworkAndLogFailure("setUpdateIdentifier")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setUpdateIdentifier(identifier), "setUpdateIdentifier");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setUpdateIdentifier");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapMethod(int method) {
        synchronized (this.mLock) {
            String methodStr = "setEapMethod";
            if (checkISupplicantStaNetworkAndLogFailure("setEapMethod")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapMethod(method), "setEapMethod");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapMethod");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapPhase2Method(int method) {
        synchronized (this.mLock) {
            String methodStr = "setEapPhase2Method";
            if (checkISupplicantStaNetworkAndLogFailure("setEapPhase2Method")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPhase2Method(method), "setEapPhase2Method");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapPhase2Method");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapIdentity(ArrayList<Byte> identity) {
        synchronized (this.mLock) {
            String methodStr = "setEapIdentity";
            if (checkISupplicantStaNetworkAndLogFailure("setEapIdentity")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapIdentity(identity), "setEapIdentity");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapIdentity");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapAnonymousIdentity(ArrayList<Byte> identity) {
        synchronized (this.mLock) {
            String methodStr = "setEapAnonymousIdentity";
            if (checkISupplicantStaNetworkAndLogFailure("setEapAnonymousIdentity")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapAnonymousIdentity(identity), "setEapAnonymousIdentity");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapAnonymousIdentity");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapPassword(ArrayList<Byte> password) {
        synchronized (this.mLock) {
            String methodStr = "setEapPassword";
            if (checkISupplicantStaNetworkAndLogFailure("setEapPassword")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPassword(password), "setEapPassword");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapPassword");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapCACert(String path) {
        synchronized (this.mLock) {
            String methodStr = "setEapCACert";
            if (checkISupplicantStaNetworkAndLogFailure("setEapCACert")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapCACert(path), "setEapCACert");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapCACert");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapCAPath(String path) {
        synchronized (this.mLock) {
            String methodStr = "setEapCAPath";
            if (checkISupplicantStaNetworkAndLogFailure("setEapCAPath")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapCAPath(path), "setEapCAPath");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapCAPath");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapClientCert(String path) {
        synchronized (this.mLock) {
            String methodStr = "setEapClientCert";
            if (checkISupplicantStaNetworkAndLogFailure("setEapClientCert")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapClientCert(path), "setEapClientCert");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapClientCert");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapPrivateKeyId(String id) {
        synchronized (this.mLock) {
            String methodStr = "setEapPrivateKeyId";
            if (checkISupplicantStaNetworkAndLogFailure("setEapPrivateKeyId")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPrivateKeyId(id), "setEapPrivateKeyId");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapPrivateKeyId");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapSubjectMatch(String match) {
        synchronized (this.mLock) {
            String methodStr = "setEapSubjectMatch";
            if (checkISupplicantStaNetworkAndLogFailure("setEapSubjectMatch")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapSubjectMatch(match), "setEapSubjectMatch");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapSubjectMatch");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapAltSubjectMatch(String match) {
        synchronized (this.mLock) {
            String methodStr = "setEapAltSubjectMatch";
            if (checkISupplicantStaNetworkAndLogFailure("setEapAltSubjectMatch")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapAltSubjectMatch(match), "setEapAltSubjectMatch");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapAltSubjectMatch");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapEngine(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setEapEngine";
            if (checkISupplicantStaNetworkAndLogFailure("setEapEngine")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapEngine(enable), "setEapEngine");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapEngine");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapEngineID(String id) {
        synchronized (this.mLock) {
            String methodStr = "setEapEngineID";
            if (checkISupplicantStaNetworkAndLogFailure("setEapEngineID")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapEngineID(id), "setEapEngineID");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapEngineID");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapDomainSuffixMatch(String match) {
        synchronized (this.mLock) {
            String methodStr = "setEapDomainSuffixMatch";
            if (checkISupplicantStaNetworkAndLogFailure("setEapDomainSuffixMatch")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapDomainSuffixMatch(match), "setEapDomainSuffixMatch");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapDomainSuffixMatch");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapProactiveKeyCaching(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setEapProactiveKeyCaching";
            if (checkISupplicantStaNetworkAndLogFailure("setEapProactiveKeyCaching")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setProactiveKeyCaching(enable), "setEapProactiveKeyCaching");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapProactiveKeyCaching");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapErp(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setEapErp";
            if (checkISupplicantVendorStaNetworkAndLogFailure("setEapErp")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantVendorStaNetwork.setEapErp(enable), "setEapErp");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapErp");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setSimNumber(int SimNum) {
        synchronized (this.mLock) {
            String methodStr = "setSimNumber";
            if (checkISupplicantStaNetworkAndLogFailure("setSimNumber")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantVendorStaNetwork.setSimNumber(SimNum), "setSimNumber");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setSimNumber");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setIdStr(String idString) {
        synchronized (this.mLock) {
            String methodStr = "setIdStr";
            if (checkISupplicantStaNetworkAndLogFailure("setIdStr")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setIdStr(idString), "setIdStr");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setIdStr");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWapiPskType(int type) {
        synchronized (this.mLock) {
            String methodStr = "setWapiPskType";
            if (checkISupplicantStaNetworkAndLogFailure("setWapiPskType")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantVendorStaNetwork.setWapiPskType(type), "setWapiPskType");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWapiPskType");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWapiPsk(String psk) {
        synchronized (this.mLock) {
            String methodStr = "setWapiPsk";
            if (checkISupplicantStaNetworkAndLogFailure("setWapiPsk")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantVendorStaNetwork.setWapiPsk(psk), "setWapiPsk");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWapiPsk");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWapiCertSelMode(int mode) {
        synchronized (this.mLock) {
            String methodStr = "setWapiCertSelMode";
            if (checkISupplicantStaNetworkAndLogFailure("setWapiCertSelMode")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantVendorStaNetwork.setWapiCertSelMode(mode), "setWapiCertSelMode");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWapiCertSelMode");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWapiCertSel(String name) {
        synchronized (this.mLock) {
            String methodStr = "setWapiCertSel";
            if (checkISupplicantStaNetworkAndLogFailure("setWapiCertSel")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantVendorStaNetwork.setWapiCertSel(name), "setWapiCertSel");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWapiCertSel");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean getWapiPskType() {
        synchronized (this.mLock) {
            String methodStr = "getWapiPskType";
            if (checkISupplicantStaNetworkAndLogFailure("getWapiPskType")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantVendorStaNetwork.getWapiPskType(new AnonymousClass33(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getWapiPskType");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_76974(MutableBoolean statusOk, SupplicantStatus status, int wapiPskTypeValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mWapiPskType = wapiPskTypeValue;
        } else {
            checkStatusAndLogFailure(status, "getWapiPskType");
        }
    }

    private boolean getWapiPsk() {
        synchronized (this.mLock) {
            String methodStr = "getWapiPsk";
            if (checkISupplicantStaNetworkAndLogFailure("getWapiPsk")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantVendorStaNetwork.getWapiPsk(new AnonymousClass32(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getWapiPsk");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_77973(MutableBoolean statusOk, SupplicantStatus status, String wapiPskValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mWapiPsk = wapiPskValue;
        } else {
            checkStatusAndLogFailure(status, "getWapiPsk");
        }
    }

    private boolean getWapiCertSelMode() {
        synchronized (this.mLock) {
            String methodStr = "getWapiCertSelMode";
            if (checkISupplicantStaNetworkAndLogFailure("getWapiCertSelMode")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantVendorStaNetwork.getWapiCertSelMode(new AnonymousClass31(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getWapiCertSelMode");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_78987(MutableBoolean statusOk, SupplicantStatus status, int wapiCertSelModeValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mWapiCertSelMode = wapiCertSelModeValue;
        } else {
            checkStatusAndLogFailure(status, "getWapiCertSelMode");
        }
    }

    private boolean getWapiCertSel() {
        synchronized (this.mLock) {
            String methodStr = "getWapiCertSel";
            if (checkISupplicantStaNetworkAndLogFailure("getWapiCertSel")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantVendorStaNetwork.getWapiCertSel(new AnonymousClass30(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getWapiCertSel");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_79951(MutableBoolean statusOk, SupplicantStatus status, String wapiCertSelValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mWapiCertSel = wapiCertSelValue;
        } else {
            checkStatusAndLogFailure(status, "getWapiCertSel");
        }
    }

    private boolean getSsid() {
        synchronized (this.mLock) {
            String methodStr = "getSsid";
            if (checkISupplicantStaNetworkAndLogFailure("getSsid")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getSsid(new AnonymousClass26(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getSsid");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_80951(MutableBoolean statusOk, SupplicantStatus status, ArrayList ssidValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mSsid = ssidValue;
        } else {
            checkStatusAndLogFailure(status, "getSsid");
        }
    }

    private boolean getBssid() {
        synchronized (this.mLock) {
            String methodStr = "getBssid";
            if (checkISupplicantStaNetworkAndLogFailure("getBssid")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getBssid(new AnonymousClass2(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getBssid");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_81939(MutableBoolean statusOk, SupplicantStatus status, byte[] bssidValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mBssid = bssidValue;
        } else {
            checkStatusAndLogFailure(status, "getBssid");
        }
    }

    private boolean getScanSsid() {
        synchronized (this.mLock) {
            String methodStr = "getScanSsid";
            if (checkISupplicantStaNetworkAndLogFailure("getScanSsid")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getScanSsid(new AnonymousClass25(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getScanSsid");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_82927(MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mScanSsid = enabledValue;
        } else {
            checkStatusAndLogFailure(status, "getScanSsid");
        }
    }

    private boolean getKeyMgmt() {
        synchronized (this.mLock) {
            String methodStr = "getKeyMgmt";
            if (checkISupplicantStaNetworkAndLogFailure("getKeyMgmt")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getKeyMgmt(new AnonymousClass19(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getKeyMgmt");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_83913(MutableBoolean statusOk, SupplicantStatus status, int keyMgmtMaskValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mKeyMgmtMask = keyMgmtMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getKeyMgmt");
        }
    }

    private boolean getProto() {
        synchronized (this.mLock) {
            String methodStr = "getProto";
            if (checkISupplicantStaNetworkAndLogFailure("getProto")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getProto(new AnonymousClass21(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getProto");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_84900(MutableBoolean statusOk, SupplicantStatus status, int protoMaskValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mProtoMask = protoMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getProto");
        }
    }

    private boolean getAuthAlg() {
        synchronized (this.mLock) {
            String methodStr = "getAuthAlg";
            if (checkISupplicantStaNetworkAndLogFailure("getAuthAlg")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getAuthAlg(new AnonymousClass1(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getAuthAlg");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_85863(MutableBoolean statusOk, SupplicantStatus status, int authAlgMaskValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mAuthAlgMask = authAlgMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getAuthAlg");
        }
    }

    private boolean getGroupCipher() {
        synchronized (this.mLock) {
            String methodStr = "getGroupCipher";
            if (checkISupplicantStaNetworkAndLogFailure("getGroupCipher")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getGroupCipher(new AnonymousClass17(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getGroupCipher");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_86868(MutableBoolean statusOk, SupplicantStatus status, int groupCipherMaskValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mGroupCipherMask = groupCipherMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getGroupCipher");
        }
    }

    private boolean getPairwiseCipher() {
        synchronized (this.mLock) {
            String methodStr = "getPairwiseCipher";
            if (checkISupplicantStaNetworkAndLogFailure("getPairwiseCipher")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getPairwiseCipher(new AnonymousClass20(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getPairwiseCipher");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_87894(MutableBoolean statusOk, SupplicantStatus status, int pairwiseCipherMaskValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mPairwiseCipherMask = pairwiseCipherMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getPairwiseCipher");
        }
    }

    private boolean getPskPassphrase() {
        synchronized (this.mLock) {
            String methodStr = "getPskPassphrase";
            if (checkISupplicantStaNetworkAndLogFailure("getPskPassphrase")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getPskPassphrase(new AnonymousClass23(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getPskPassphrase");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_88926(MutableBoolean statusOk, SupplicantStatus status, String pskValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mPskPassphrase = pskValue;
        } else {
            checkStatusAndLogFailure(status, "getPskPassphrase");
        }
    }

    private boolean getPsk() {
        synchronized (this.mLock) {
            String methodStr = "getPsk";
            if (checkISupplicantStaNetworkAndLogFailure("getPsk")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getPsk(new AnonymousClass22(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getPsk");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_89896(MutableBoolean statusOk, SupplicantStatus status, byte[] pskValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mPsk = pskValue;
        } else {
            checkStatusAndLogFailure(status, "getPsk");
        }
    }

    private boolean getWepKey(int keyIdx) {
        synchronized (this.mLock) {
            String methodStr = "keyIdx";
            if (checkISupplicantStaNetworkAndLogFailure("keyIdx")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getWepKey(keyIdx, new AnonymousClass27(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "keyIdx");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_90856(MutableBoolean statusOk, SupplicantStatus status, ArrayList wepKeyValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mWepKey = wepKeyValue;
        } else {
            Log.e(TAG, "keyIdx,  failed: " + status.debugMessage);
        }
    }

    private boolean getWepTxKeyIdx() {
        synchronized (this.mLock) {
            String methodStr = "getWepTxKeyIdx";
            if (checkISupplicantStaNetworkAndLogFailure("getWepTxKeyIdx")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getWepTxKeyIdx(new AnonymousClass28(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getWepTxKeyIdx");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_91884(MutableBoolean statusOk, SupplicantStatus status, int keyIdxValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mWepTxKeyIdx = keyIdxValue;
        } else {
            checkStatusAndLogFailure(status, "getWepTxKeyIdx");
        }
    }

    private boolean getRequirePmf() {
        synchronized (this.mLock) {
            String methodStr = "getRequirePmf";
            if (checkISupplicantStaNetworkAndLogFailure("getRequirePmf")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getRequirePmf(new AnonymousClass24(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getRequirePmf");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_92876(MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mRequirePmf = enabledValue;
        } else {
            checkStatusAndLogFailure(status, "getRequirePmf");
        }
    }

    private boolean getEapMethod() {
        synchronized (this.mLock) {
            String methodStr = "getEapMethod";
            if (checkISupplicantStaNetworkAndLogFailure("getEapMethod")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapMethod(new AnonymousClass12(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapMethod");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_93870(MutableBoolean statusOk, SupplicantStatus status, int methodValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapMethod = methodValue;
        } else {
            checkStatusAndLogFailure(status, "getEapMethod");
        }
    }

    private boolean getEapPhase2Method() {
        synchronized (this.mLock) {
            String methodStr = "getEapPhase2Method";
            if (checkISupplicantStaNetworkAndLogFailure("getEapPhase2Method")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapPhase2Method(new AnonymousClass14(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapPhase2Method");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_94875(MutableBoolean statusOk, SupplicantStatus status, int methodValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapPhase2Method = methodValue;
        } else {
            checkStatusAndLogFailure(status, "getEapPhase2Method");
        }
    }

    private boolean getEapIdentity() {
        synchronized (this.mLock) {
            String methodStr = "getEapIdentity";
            if (checkISupplicantStaNetworkAndLogFailure("getEapIdentity")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapIdentity(new AnonymousClass11(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapIdentity");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_95874(MutableBoolean statusOk, SupplicantStatus status, ArrayList identityValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapIdentity = identityValue;
        } else {
            checkStatusAndLogFailure(status, "getEapIdentity");
        }
    }

    private boolean getEapAnonymousIdentity() {
        synchronized (this.mLock) {
            String methodStr = "getEapAnonymousIdentity";
            if (checkISupplicantStaNetworkAndLogFailure("getEapAnonymousIdentity")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapAnonymousIdentity(new AnonymousClass4(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapAnonymousIdentity");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_96912(MutableBoolean statusOk, SupplicantStatus status, ArrayList identityValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapAnonymousIdentity = identityValue;
        } else {
            checkStatusAndLogFailure(status, "getEapAnonymousIdentity");
        }
    }

    public String fetchEapAnonymousIdentity() {
        synchronized (this.mLock) {
            if (getEapAnonymousIdentity()) {
                String stringFromByteArrayList = NativeUtil.stringFromByteArrayList(this.mEapAnonymousIdentity);
                return stringFromByteArrayList;
            }
            return null;
        }
    }

    private boolean getEapPassword() {
        synchronized (this.mLock) {
            String methodStr = "getEapPassword";
            if (checkISupplicantStaNetworkAndLogFailure("getEapPassword")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapPassword(new AnonymousClass13(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapPassword");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_98424(MutableBoolean statusOk, SupplicantStatus status, ArrayList passwordValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapPassword = passwordValue;
        } else {
            checkStatusAndLogFailure(status, "getEapPassword");
        }
    }

    private boolean getEapCACert() {
        synchronized (this.mLock) {
            String methodStr = "getEapCACert";
            if (checkISupplicantStaNetworkAndLogFailure("getEapCACert")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapCACert(new AnonymousClass5(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapCACert");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_99429(MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapCACert = pathValue;
        } else {
            checkStatusAndLogFailure(status, "getEapCACert");
        }
    }

    private boolean getEapCAPath() {
        synchronized (this.mLock) {
            String methodStr = "getEapCAPath";
            if (checkISupplicantStaNetworkAndLogFailure("getEapCAPath")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapCAPath(new AnonymousClass6(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapCAPath");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_100391(MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapCAPath = pathValue;
        } else {
            checkStatusAndLogFailure(status, "getEapCAPath");
        }
    }

    private boolean getEapClientCert() {
        synchronized (this.mLock) {
            String methodStr = "getEapClientCert";
            if (checkISupplicantStaNetworkAndLogFailure("getEapClientCert")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapClientCert(new AnonymousClass7(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapClientCert");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_101365(MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapClientCert = pathValue;
        } else {
            checkStatusAndLogFailure(status, "getEapClientCert");
        }
    }

    private boolean getEapPrivateKeyId() {
        synchronized (this.mLock) {
            String methodStr = "getEapPrivateKeyId";
            if (checkISupplicantStaNetworkAndLogFailure("getEapPrivateKeyId")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapPrivateKeyId(new AnonymousClass15(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapPrivateKeyId");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_102373(MutableBoolean statusOk, SupplicantStatus status, String idValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapPrivateKeyId = idValue;
        } else {
            checkStatusAndLogFailure(status, "getEapPrivateKeyId");
        }
    }

    private boolean getEapSubjectMatch() {
        synchronized (this.mLock) {
            String methodStr = "getEapSubjectMatch";
            if (checkISupplicantStaNetworkAndLogFailure("getEapSubjectMatch")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapSubjectMatch(new AnonymousClass16(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapSubjectMatch");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_103379(MutableBoolean statusOk, SupplicantStatus status, String matchValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapSubjectMatch = matchValue;
        } else {
            checkStatusAndLogFailure(status, "getEapSubjectMatch");
        }
    }

    private boolean getEapAltSubjectMatch() {
        synchronized (this.mLock) {
            String methodStr = "getEapAltSubjectMatch";
            if (checkISupplicantStaNetworkAndLogFailure("getEapAltSubjectMatch")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapAltSubjectMatch(new AnonymousClass3(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapAltSubjectMatch");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_104400(MutableBoolean statusOk, SupplicantStatus status, String matchValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapAltSubjectMatch = matchValue;
        } else {
            checkStatusAndLogFailure(status, "getEapAltSubjectMatch");
        }
    }

    private boolean getEapEngine() {
        synchronized (this.mLock) {
            String methodStr = "getEapEngine";
            if (checkISupplicantStaNetworkAndLogFailure("getEapEngine")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapEngine(new AnonymousClass9(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapEngine");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_105397(MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapEngine = enabledValue;
        } else {
            checkStatusAndLogFailure(status, "getEapEngine");
        }
    }

    private boolean getEapEngineID() {
        synchronized (this.mLock) {
            String methodStr = "getEapEngineID";
            if (checkISupplicantStaNetworkAndLogFailure("getEapEngineID")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapEngineID(new AnonymousClass10(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapEngineID");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_106396(MutableBoolean statusOk, SupplicantStatus status, String idValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapEngineID = idValue;
        } else {
            checkStatusAndLogFailure(status, "getEapEngineID");
        }
    }

    private boolean getEapDomainSuffixMatch() {
        synchronized (this.mLock) {
            String methodStr = "getEapDomainSuffixMatch";
            if (checkISupplicantStaNetworkAndLogFailure("getEapDomainSuffixMatch")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapDomainSuffixMatch(new AnonymousClass8(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapDomainSuffixMatch");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_107389(MutableBoolean statusOk, SupplicantStatus status, String matchValue) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mEapDomainSuffixMatch = matchValue;
        } else {
            checkStatusAndLogFailure(status, "getEapDomainSuffixMatch");
        }
    }

    private boolean getIdStr() {
        synchronized (this.mLock) {
            String methodStr = "getIdStr";
            if (checkISupplicantStaNetworkAndLogFailure("getIdStr")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getIdStr(new AnonymousClass18(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getIdStr");
                    return false;
                }
            }
            return false;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_108376(MutableBoolean statusOk, SupplicantStatus status, String idString) {
        boolean z = false;
        if (status.code == 0) {
            z = true;
        }
        statusOk.value = z;
        if (statusOk.value) {
            this.mIdStr = idString;
        } else {
            checkStatusAndLogFailure(status, "getIdStr");
        }
    }

    private boolean enable(boolean noConnect) {
        synchronized (this.mLock) {
            String methodStr = "enable";
            if (checkISupplicantStaNetworkAndLogFailure("enable")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.enable(noConnect), "enable");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "enable");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean disable() {
        synchronized (this.mLock) {
            String methodStr = "disable";
            if (checkISupplicantStaNetworkAndLogFailure("disable")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.disable(), "disable");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "disable");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean select() {
        synchronized (this.mLock) {
            String methodStr = "select";
            if (checkISupplicantStaNetworkAndLogFailure("select")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.select(), "select");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "select");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimGsmAuthResponse(String paramsStr) {
        synchronized (this.mLock) {
            try {
                Matcher match = GSM_AUTH_RESPONSE_PARAMS_PATTERN.matcher(paramsStr);
                ArrayList params = new ArrayList();
                while (match.find()) {
                    if (match.groupCount() != 2) {
                        Log.e(TAG, "Malformed gsm auth response params: " + paramsStr);
                        return false;
                    }
                    NetworkResponseEapSimGsmAuthParams param = new NetworkResponseEapSimGsmAuthParams();
                    byte[] kc = NativeUtil.hexStringToByteArray(match.group(1));
                    if (kc == null || kc.length != param.kc.length) {
                        Log.e(TAG, "Invalid kc value: " + match.group(1));
                        return false;
                    }
                    byte[] sres = NativeUtil.hexStringToByteArray(match.group(2));
                    if (sres == null || sres.length != param.sres.length) {
                        Log.e(TAG, "Invalid sres value: " + match.group(2));
                        return false;
                    }
                    System.arraycopy(kc, 0, param.kc, 0, param.kc.length);
                    System.arraycopy(sres, 0, param.sres, 0, param.sres.length);
                    params.add(param);
                }
                if (params.size() > 3 || params.size() < 2) {
                    Log.e(TAG, "Malformed gsm auth response params: " + paramsStr);
                    return false;
                }
                boolean sendNetworkEapSimGsmAuthResponse = sendNetworkEapSimGsmAuthResponse(params);
                return sendNetworkEapSimGsmAuthResponse;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + paramsStr, e);
                return false;
            }
        }
    }

    private boolean sendNetworkEapSimGsmAuthResponse(ArrayList<NetworkResponseEapSimGsmAuthParams> params) {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimGsmAuthResponse";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimGsmAuthResponse")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimGsmAuthResponse(params), "sendNetworkEapSimGsmAuthResponse");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimGsmAuthResponse");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimGsmAuthFailure() {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimGsmAuthFailure";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimGsmAuthFailure")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimGsmAuthFailure(), "sendNetworkEapSimGsmAuthFailure");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimGsmAuthFailure");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimUmtsAuthResponse(String paramsStr) {
        synchronized (this.mLock) {
            try {
                Matcher match = UMTS_AUTH_RESPONSE_PARAMS_PATTERN.matcher(paramsStr);
                if (match.find() && match.groupCount() == 3) {
                    NetworkResponseEapSimUmtsAuthParams params = new NetworkResponseEapSimUmtsAuthParams();
                    byte[] ik = NativeUtil.hexStringToByteArray(match.group(1));
                    if (ik == null || ik.length != params.ik.length) {
                        Log.e(TAG, "Invalid ik value: " + match.group(1));
                        return false;
                    }
                    byte[] ck = NativeUtil.hexStringToByteArray(match.group(2));
                    if (ck == null || ck.length != params.ck.length) {
                        Log.e(TAG, "Invalid ck value: " + match.group(2));
                        return false;
                    }
                    byte[] res = NativeUtil.hexStringToByteArray(match.group(3));
                    if (res == null || res.length == 0) {
                        Log.e(TAG, "Invalid res value: " + match.group(3));
                        return false;
                    }
                    System.arraycopy(ik, 0, params.ik, 0, params.ik.length);
                    System.arraycopy(ck, 0, params.ck, 0, params.ck.length);
                    for (byte b : res) {
                        params.res.add(Byte.valueOf(b));
                    }
                    return sendNetworkEapSimUmtsAuthResponse(params);
                }
                Log.e(TAG, "Malformed umts auth response params: " + paramsStr);
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + paramsStr, e);
                return false;
            }
        }
    }

    private boolean sendNetworkEapSimUmtsAuthResponse(NetworkResponseEapSimUmtsAuthParams params) {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimUmtsAuthResponse";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAuthResponse")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAuthResponse(params), "sendNetworkEapSimUmtsAuthResponse");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimUmtsAuthResponse");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimUmtsAutsResponse(String paramsStr) {
        synchronized (this.mLock) {
            try {
                Matcher match = UMTS_AUTS_RESPONSE_PARAMS_PATTERN.matcher(paramsStr);
                if (match.find() && match.groupCount() == 1) {
                    byte[] auts = NativeUtil.hexStringToByteArray(match.group(1));
                    if (auts == null || auts.length != 14) {
                        Log.e(TAG, "Invalid auts value: " + match.group(1));
                        return false;
                    }
                    boolean sendNetworkEapSimUmtsAutsResponse = sendNetworkEapSimUmtsAutsResponse(auts);
                    return sendNetworkEapSimUmtsAutsResponse;
                }
                Log.e(TAG, "Malformed umts auts response params: " + paramsStr);
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + paramsStr, e);
                return false;
            }
        }
    }

    private boolean sendNetworkEapSimUmtsAutsResponse(byte[] auts) {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimUmtsAutsResponse";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAutsResponse")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAutsResponse(auts), "sendNetworkEapSimUmtsAutsResponse");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimUmtsAutsResponse");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimUmtsAuthFailure() {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimUmtsAuthFailure";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAuthFailure")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAuthFailure(), "sendNetworkEapSimUmtsAuthFailure");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimUmtsAuthFailure");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapIdentityResponse(String identityStr) {
        boolean sendNetworkEapIdentityResponse;
        synchronized (this.mLock) {
            try {
                sendNetworkEapIdentityResponse = sendNetworkEapIdentityResponse(NativeUtil.stringToByteArrayList(identityStr));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + identityStr, e);
                return false;
            }
        }
        return sendNetworkEapIdentityResponse;
    }

    private boolean sendNetworkEapIdentityResponse(ArrayList<Byte> identity) {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapIdentityResponse";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapIdentityResponse")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapIdentityResponse(identity), "sendNetworkEapIdentityResponse");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapIdentityResponse");
                    return false;
                }
            }
            return false;
        }
    }

    public String getWpsNfcConfigurationToken() {
        synchronized (this.mLock) {
            ArrayList<Byte> token = getWpsNfcConfigurationTokenInternal();
            if (token == null) {
                return null;
            }
            String hexStringFromByteArray = NativeUtil.hexStringFromByteArray(NativeUtil.byteArrayFromArrayList(token));
            return hexStringFromByteArray;
        }
    }

    private ArrayList<Byte> getWpsNfcConfigurationTokenInternal() {
        synchronized (this.mLock) {
            String methodStr = "getWpsNfcConfigurationToken";
            if (checkISupplicantStaNetworkAndLogFailure("getWpsNfcConfigurationToken")) {
                Mutable<ArrayList<Byte>> gotToken = new Mutable();
                try {
                    this.mISupplicantStaNetwork.getWpsNfcConfigurationToken(new AnonymousClass29(this, gotToken));
                } catch (RemoteException e) {
                    handleRemoteException(e, "getWpsNfcConfigurationToken");
                }
                ArrayList<Byte> arrayList = (ArrayList) gotToken.value;
                return arrayList;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaNetworkHal_121831(Mutable gotToken, SupplicantStatus status, ArrayList token) {
        if (checkStatusAndLogFailure(status, "getWpsNfcConfigurationToken")) {
            gotToken.value = token;
        }
    }

    private boolean checkStatusAndLogFailure(SupplicantStatus status, String methodStr) {
        synchronized (this.mLock) {
            if (status.code != 0) {
                Log.e(TAG, "ISupplicantStaNetwork." + methodStr + " failed: " + SupplicantStaIfaceHal.supplicantStatusCodeToString(status.code) + ", " + status.debugMessage);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaNetwork." + methodStr + " succeeded");
            }
            return true;
        }
    }

    private void logCallback(String methodStr) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaNetworkCallback." + methodStr + " received");
            }
        }
    }

    private boolean checkISupplicantStaNetworkAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            if (this.mISupplicantStaNetwork == null) {
                Log.e(TAG, "Can't call " + methodStr + ", ISupplicantStaNetwork is null");
                return false;
            }
            return true;
        }
    }

    private boolean checkISupplicantVendorStaNetworkAndLogFailure(String methodStr) {
        if (this.mISupplicantVendorStaNetwork != null) {
            return true;
        }
        Log.e(TAG, "Can't call " + methodStr + ", ISupplicantVendorStaNetwork is null");
        return false;
    }

    private void handleRemoteException(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            this.mISupplicantStaNetwork = null;
            Log.e(TAG, "ISupplicantStaNetwork." + methodStr + " failed with exception", e);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private BitSet addFastTransitionFlags(BitSet keyManagementFlags) {
        synchronized (this.mLock) {
            if (this.mSystemSupportsFastBssTransition) {
                BitSet modifiedFlags = (BitSet) keyManagementFlags.clone();
                if (keyManagementFlags.get(1)) {
                    modifiedFlags.set(6);
                }
                if (keyManagementFlags.get(2)) {
                    modifiedFlags.set(7);
                }
            } else {
                return keyManagementFlags;
            }
        }
    }

    private BitSet removeFastTransitionFlags(BitSet keyManagementFlags) {
        BitSet modifiedFlags;
        synchronized (this.mLock) {
            modifiedFlags = (BitSet) keyManagementFlags.clone();
            modifiedFlags.clear(6);
            modifiedFlags.clear(7);
        }
        return modifiedFlags;
    }

    public static String createNetworkExtra(Map<String, String> values) {
        try {
            return URLEncoder.encode(new JSONObject(values).toString(), "UTF-8");
        } catch (NullPointerException e) {
            Log.e(TAG, "Unable to serialize networkExtra: " + e.toString());
            return null;
        } catch (UnsupportedEncodingException e2) {
            Log.e(TAG, "Unable to serialize networkExtra: " + e2.toString());
            return null;
        }
    }

    public static Map<String, String> parseNetworkExtra(String encoded) {
        if (TextUtils.isEmpty(encoded)) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(URLDecoder.decode(encoded, "UTF-8"));
            Map<String, String> values = new HashMap();
            Iterator<?> it = json.keys();
            while (it.hasNext()) {
                String key = (String) it.next();
                Object value = json.get(key);
                if (value instanceof String) {
                    values.put(key, (String) value);
                }
            }
            return values;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to deserialize networkExtra: " + e.toString());
            return null;
        } catch (JSONException e2) {
            return null;
        }
    }
}
