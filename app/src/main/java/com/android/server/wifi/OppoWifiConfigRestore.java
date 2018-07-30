package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.os.FileUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class OppoWifiConfigRestore {
    private static final boolean DEBUG = true;
    private static final byte[] EMPTY_DATA = new byte[0];
    private static final String FILE_WIFI_CONFIGSTORE = "/data/misc/wifi/WifiConfigStore.xml";
    private static final String FILE_WIFI_SUPPLICANT = "/data/misc/wifi/wpa_supplicant.conf";
    private static final String TAG = "OppoWifiConfigRestore";
    private static boolean isConfigHaveNetwork = false;
    static final ArrayList<String> supplicantFile = new ArrayList();

    static class Network {
        boolean mCertUsed = false;
        boolean mHasWepKey = false;
        boolean mIsHidden = false;
        String mKeyMgmt = "";
        String mPsk = "";
        final ArrayList<String> mRawLines = new ArrayList();
        String mSsid = "";
        String mWapiKeyType = "0";
        String mWapiPsk = "";
        String mWepkey = "";

        Network() {
        }

        public static Network readFromStream(BufferedReader in) {
            Network n = new Network();
            String line = "";
            while (in.ready()) {
                try {
                    line = in.readLine();
                    if (line == null || line.startsWith("}")) {
                        break;
                    }
                    n.rememberLine(line);
                } catch (IOException e) {
                    return null;
                }
            }
            return n;
        }

        void rememberLine(String line) {
            line = line.trim();
            if (!line.isEmpty()) {
                this.mRawLines.add(line);
                if (line.startsWith("ssid=")) {
                    this.mSsid = line;
                } else if (line.startsWith("key_mgmt=")) {
                    this.mKeyMgmt = line;
                } else if (line.startsWith("psk=")) {
                    this.mPsk = line.substring(line.indexOf(61) + 1, line.length());
                } else if (line.startsWith("wapi_psk=")) {
                    this.mWapiPsk = line.substring(line.indexOf(61) + 1, line.length());
                } else if (line.startsWith("wapi_key_type=")) {
                    this.mWapiKeyType = line.substring(line.indexOf(61) + 1);
                } else if (line.startsWith("scan_ssid=")) {
                    this.mIsHidden = true;
                } else if (line.startsWith("client_cert=")) {
                    this.mCertUsed = true;
                } else if (line.startsWith("ca_cert=")) {
                    this.mCertUsed = true;
                } else if (line.startsWith("ca_path=")) {
                    this.mCertUsed = true;
                } else if (line.startsWith("wep_")) {
                    this.mHasWepKey = true;
                    this.mWepkey = line;
                }
            }
        }

        public void write(Writer w) throws IOException {
            w.write("\nnetwork={\n");
            for (String line : this.mRawLines) {
                w.write("\t" + line + "\n");
            }
            w.write("}\n");
        }
    }

    static class NetworkXml {
        boolean mCertUsed = false;
        boolean mHasWepKey = false;
        String mKeyMgmt = "";
        String mPsk = "";
        final ArrayList<String> mRawLines = new ArrayList();
        String mSsid = "";

        NetworkXml() {
        }

        public static NetworkXml readFromStream(BufferedReader in) {
            NetworkXml n = new NetworkXml();
            String line = "";
            while (in.ready()) {
                try {
                    line = in.readLine();
                    if (line == null || line.startsWith("</Network>")) {
                        break;
                    }
                    n.rememberLine(line);
                } catch (IOException e) {
                    return null;
                }
            }
            return n;
        }

        void rememberLine(String line) {
            line = line.trim();
            if (!line.isEmpty()) {
                this.mRawLines.add(line);
                if (line.startsWith("<string name=\"SSID\">")) {
                    this.mSsid = line;
                } else if (line.startsWith("<string name=\"ConfigKey\">")) {
                    this.mKeyMgmt = line;
                } else if (line.startsWith("client_cert=")) {
                    this.mCertUsed = true;
                } else if (line.startsWith("ca_cert=")) {
                    this.mCertUsed = true;
                } else if (line.startsWith("ca_path=")) {
                    this.mCertUsed = true;
                } else if (line.startsWith("wep_")) {
                    this.mHasWepKey = true;
                }
            }
        }

        public void write(Writer w) throws IOException {
            w.write("<Network>\n");
            for (String line : this.mRawLines) {
                w.write(line + "\n");
            }
            w.write("</Network>\n");
        }
    }

    static class WifiNetworkSettings {
        final HashSet<Network> mKnownNetworks = new HashSet();
        final ArrayList<Network> mNetworks = new ArrayList(8);

        WifiNetworkSettings() {
        }

        public void readNetworks(BufferedReader in) {
            try {
                String line = "";
                while (in.ready()) {
                    line = in.readLine();
                    if (line != null && line.startsWith("network")) {
                        OppoWifiConfigRestore.isConfigHaveNetwork = true;
                        Network net = Network.readFromStream(in);
                        Log.v(OppoWifiConfigRestore.TAG, "Adding " + net.mSsid + " / " + net.mKeyMgmt);
                        this.mKnownNetworks.add(net);
                        this.mNetworks.add(net);
                    }
                }
            } catch (IOException e) {
            }
        }

        public void write(Writer w) throws IOException {
            for (Network net : this.mNetworks) {
                if (!net.mCertUsed) {
                    net.write(w);
                }
            }
        }
    }

    static class WifiNetworkSettingsXml {
        static final ArrayList<String> mRawXmlBegin = new ArrayList();
        static final ArrayList<String> mRawXmlEnd = new ArrayList();
        final HashSet<NetworkXml> mKnownNetworksXml = new HashSet();
        final ArrayList<NetworkXml> mNetworksXml = new ArrayList(8);

        WifiNetworkSettingsXml() {
        }

        static {
            mRawXmlBegin.add("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>");
            mRawXmlBegin.add("<WifiConfigStoreData>");
            mRawXmlBegin.add("<int name=\"Version\" value=\"1\" />");
            mRawXmlBegin.add("<NetworkList>");
            mRawXmlEnd.add("</NetworkList>");
            mRawXmlEnd.add("<PasspointConfigData>");
            mRawXmlEnd.add("<long name=\"ProviderIndex\" value=\"0\" />");
            mRawXmlEnd.add("</PasspointConfigData>");
            mRawXmlEnd.add("</WifiConfigStoreData>");
        }

        public void readNetworks(BufferedReader in) {
            try {
                String line = "";
                while (in.ready()) {
                    line = in.readLine();
                    if (line != null && line.startsWith("<Network>")) {
                        NetworkXml net = NetworkXml.readFromStream(in);
                        if (this.mKnownNetworksXml.contains(net)) {
                            Log.v(OppoWifiConfigRestore.TAG, "Dupe; skipped " + net.mSsid + " / " + net.mKeyMgmt);
                        } else {
                            Log.v(OppoWifiConfigRestore.TAG, "XML Adding " + net.mSsid + " / " + net.mKeyMgmt);
                            this.mKnownNetworksXml.add(net);
                            this.mNetworksXml.add(net);
                        }
                    }
                }
            } catch (IOException e) {
            }
        }

        public void changeNetworks(WifiNetworkSettings wns) {
            Log.v(OppoWifiConfigRestore.TAG, "ChangeNetworks: Begin!!!");
            for (Network net : wns.mNetworks) {
                if (!net.mKeyMgmt.contains("WPA-EAP")) {
                    Log.v(OppoWifiConfigRestore.TAG, "ChangeNetworks: net = " + net.mSsid);
                    NetworkXml tempXml = OppoWifiConfigRestore.changeNetworkToXml(net);
                    Log.v(OppoWifiConfigRestore.TAG, "ChangeNetworks: Adding " + tempXml.mSsid + " / " + tempXml.mKeyMgmt);
                    this.mKnownNetworksXml.add(tempXml);
                    this.mNetworksXml.add(tempXml);
                }
            }
        }

        public void write(Writer w) throws IOException {
            for (NetworkXml net : this.mNetworksXml) {
                if (!net.mCertUsed) {
                    net.write(w);
                }
            }
        }
    }

    static class WifiSsid implements Parcelable {
        public static final Creator<WifiSsid> CREATOR = new Creator<WifiSsid>() {
            public WifiSsid createFromParcel(Parcel in) {
                boolean z = false;
                WifiSsid ssid = new WifiSsid();
                int length = in.readInt();
                byte[] b = new byte[length];
                in.readByteArray(b);
                ssid.octets.write(b, 0, length);
                if (in.readInt() != 0) {
                    z = true;
                }
                ssid.mIsGbkEncoding = z;
                return ssid;
            }

            public WifiSsid[] newArray(int size) {
                return new WifiSsid[size];
            }
        };
        private static final int HEX_RADIX = 16;
        public static final String NONE = "<unknown ssid>";
        private static final String TAG = "WifiSsid";
        private boolean mIsGbkEncoding;
        public final ByteArrayOutputStream octets;

        /* synthetic */ WifiSsid(WifiSsid -this0) {
            this();
        }

        private WifiSsid() {
            this.octets = new ByteArrayOutputStream(32);
            this.mIsGbkEncoding = false;
        }

        public static WifiSsid createFromAsciiEncoded(String asciiEncoded) {
            WifiSsid a = new WifiSsid();
            a.convertToBytes(asciiEncoded);
            return a;
        }

        public static WifiSsid createFromHex(String hexStr) {
            WifiSsid a = new WifiSsid();
            if (hexStr == null) {
                return a;
            }
            if (hexStr.startsWith("0x") || hexStr.startsWith("0X")) {
                hexStr = hexStr.substring(2);
            }
            for (int i = 0; i < hexStr.length() - 1; i += 2) {
                int val;
                try {
                    val = Integer.parseInt(hexStr.substring(i, i + 2), 16);
                } catch (NumberFormatException e) {
                    val = 0;
                }
                a.octets.write(val);
            }
            a.checkAndSetIsGbkEncoding();
            return a;
        }

        private void convertToBytes(String asciiEncoded) {
            int i = 0;
            while (i < asciiEncoded.length()) {
                char c = asciiEncoded.charAt(i);
                switch (c) {
                    case StatusCode.REFUSED_EXTERNAL_REASON /*92*/:
                        i++;
                        int val;
                        switch (asciiEncoded.charAt(i)) {
                            case '\"':
                                this.octets.write(34);
                                i++;
                                break;
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                                val = asciiEncoded.charAt(i) - 48;
                                i++;
                                if (asciiEncoded.charAt(i) >= '0' && asciiEncoded.charAt(i) <= '7') {
                                    val = ((val * 8) + asciiEncoded.charAt(i)) - 48;
                                    i++;
                                }
                                if (asciiEncoded.charAt(i) >= '0' && asciiEncoded.charAt(i) <= '7') {
                                    val = ((val * 8) + asciiEncoded.charAt(i)) - 48;
                                    i++;
                                }
                                this.octets.write(val);
                                break;
                            case StatusCode.REFUSED_EXTERNAL_REASON /*92*/:
                                this.octets.write(92);
                                i++;
                                break;
                            case StatusCode.MAF_LIMIT_EXCEEDED /*101*/:
                                this.octets.write(27);
                                i++;
                                break;
                            case 'n':
                                this.octets.write(10);
                                i++;
                                break;
                            case 'r':
                                this.octets.write(13);
                                i++;
                                break;
                            case 't':
                                this.octets.write(9);
                                i++;
                                break;
                            case 'x':
                                i++;
                                if (i != asciiEncoded.length() && i + 2 <= asciiEncoded.length()) {
                                    try {
                                        val = Integer.parseInt(asciiEncoded.substring(i, i + 2), 16);
                                    } catch (NumberFormatException e) {
                                        val = -1;
                                    }
                                    if (val >= 0) {
                                        this.octets.write(val);
                                        i += 2;
                                        break;
                                    }
                                    val = Character.digit(asciiEncoded.charAt(i), 16);
                                    if (val < 0) {
                                        break;
                                    }
                                    this.octets.write(val);
                                    i++;
                                    break;
                                }
                                Log.e(TAG, "convertToBytes met StringIndexOutOfBoundsException!! asciiEncoded:" + asciiEncoded);
                                i++;
                                break;
                            default:
                                break;
                        }
                    default:
                        this.octets.write(c);
                        i++;
                        break;
                }
            }
            checkAndSetIsGbkEncoding();
        }

        public String toString() {
            byte[] ssidBytes = this.octets.toByteArray();
            if (this.octets.size() <= 0 || isArrayAllZeroes(ssidBytes)) {
                return "";
            }
            boolean DBG = SystemProperties.get("persist.wifi.gbk.debug").equals("1");
            boolean ssidGbkEncoding = SystemProperties.get("persist.wifi.gbk.encoding").equals("1");
            Charset charset = Charset.forName("UTF-8");
            if (ssidGbkEncoding || this.mIsGbkEncoding) {
                charset = Charset.forName("GB2312");
            }
            CharsetDecoder decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            CharBuffer out = CharBuffer.allocate(32);
            CoderResult result = decoder.decode(ByteBuffer.wrap(ssidBytes), out, true);
            out.flip();
            if (result.isError()) {
                return NONE;
            }
            if (DBG) {
                Log.d(TAG, "persist.wifi.gbk.encoding: " + ssidGbkEncoding + ", isGbk: " + this.mIsGbkEncoding + ", toString: " + out.toString());
            }
            return out.toString();
        }

        private boolean isArrayAllZeroes(byte[] ssidBytes) {
            for (byte b : ssidBytes) {
                if (b != (byte) 0) {
                    return false;
                }
            }
            return true;
        }

        public boolean isHidden() {
            return isArrayAllZeroes(this.octets.toByteArray());
        }

        public byte[] getOctets() {
            return this.octets.toByteArray();
        }

        public String getHexString() {
            String out = "0x";
            byte[] ssidbytes = getOctets();
            for (int i = 0; i < this.octets.size(); i++) {
                out = out + String.format(Locale.US, "%02x", new Object[]{Byte.valueOf(ssidbytes[i])});
            }
            return this.octets.size() > 0 ? out : null;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.octets.size());
            dest.writeByteArray(this.octets.toByteArray());
            dest.writeInt(this.mIsGbkEncoding ? 1 : 0);
        }

        private static boolean isGBK(byte[] byteArray, int ssidStartPos, int ssidEndPos) {
            boolean DBG = SystemProperties.get("persist.wifi.gbk.debug").equals("1");
            if (isNotUtf8(byteArray, ssidStartPos, ssidEndPos)) {
                if (DBG) {
                    Log.d(TAG, "is not utf8");
                }
                return true;
            }
            if (DBG) {
                Log.d(TAG, "is utf8 format");
            }
            return false;
        }

        private static boolean isNotUtf8(byte[] input, int ssidStartPos, int ssidEndPos) {
            int nBytes = 0;
            int lastWildcar = 0;
            int Utf_bit = 0;
            int Utf_char_H = 0;
            boolean isAllAscii = true;
            boolean isAllGBK = true;
            boolean isWildcardChar = false;
            int i = ssidStartPos;
            while (i < ssidEndPos && i < input.length) {
                byte chr = input[i];
                if (isASCII(chr)) {
                    isWildcardChar = false;
                } else {
                    isAllAscii = false;
                    isWildcardChar ^= 1;
                    if (isWildcardChar && i < input.length - 1 && !isGBKChar(chr, input[i + 1])) {
                        isAllGBK = false;
                    }
                }
                if (nBytes == 0) {
                    if ((chr & Constants.BYTE_MASK) >= 128) {
                        lastWildcar = i;
                        nBytes = getUtf8CharLen(chr);
                        if (nBytes == 0) {
                            return true;
                        }
                        if (nBytes == 6) {
                            Utf_bit = 5;
                        } else if (nBytes == 5) {
                            Utf_bit = 4;
                        } else if (nBytes == 4) {
                            Utf_bit = 3;
                        } else if (nBytes == 3) {
                            Utf_bit = 2;
                        } else if (nBytes == 2) {
                            Utf_bit = 2;
                        }
                        Utf_char_H = chr;
                        nBytes--;
                    } else {
                        continue;
                    }
                } else if ((chr & WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE) != 128) {
                    break;
                } else {
                    byte Utf_char_L;
                    if (nBytes == 5 && Utf_bit == 5) {
                        Utf_char_L = chr;
                        Utf_bit = 0;
                        if ((Utf_char_H & 1) == 0 && (chr & Constants.BYTE_MASK) < 132) {
                            return true;
                        }
                    } else if (nBytes == 4 && Utf_bit == 4) {
                        Utf_char_L = chr;
                        Utf_bit = 0;
                        if ((Utf_char_H & 3) == 0 && (chr & Constants.BYTE_MASK) < 136) {
                            return true;
                        }
                    } else if (nBytes == 3 && Utf_bit == 3) {
                        Utf_char_L = chr;
                        Utf_bit = 0;
                        if ((Utf_char_H & 7) == 0 && (chr & Constants.BYTE_MASK) < 144) {
                            return true;
                        }
                    } else if (nBytes == 2 && Utf_bit == 2) {
                        Utf_char_L = chr;
                        Utf_bit = 0;
                        if ((Utf_char_H & 15) == 0 && (chr & Constants.BYTE_MASK) < 160) {
                            return true;
                        }
                    } else if (nBytes == 1 && Utf_bit == 2) {
                        Utf_bit = 0;
                        if ((Utf_char_H & Constants.BYTE_MASK) < 194) {
                            return true;
                        }
                    }
                    nBytes--;
                }
                i++;
            }
            if (nBytes <= 0) {
                return false;
            }
            if (isAllAscii) {
                return false;
            }
            if (isAllGBK) {
                return true;
            }
            nBytes = getUtf8CharLen(input[lastWildcar]);
            int j = lastWildcar;
            while (j < lastWildcar + nBytes && j < input.length) {
                if (!isASCII(input[j])) {
                    input[j] = (byte) 32;
                }
                j++;
            }
            return false;
        }

        private static int getUtf8CharLen(byte firstByte) {
            int nBytes;
            if (firstByte >= (byte) -4 && firstByte <= (byte) -3) {
                nBytes = 6;
            } else if (firstByte >= (byte) -8) {
                nBytes = 5;
            } else if (firstByte >= (byte) -16) {
                nBytes = 4;
            } else if (firstByte >= (byte) -32) {
                nBytes = 3;
            } else if (firstByte < (byte) -64) {
                return 0;
            } else {
                nBytes = 2;
            }
            return nBytes;
        }

        private static boolean isASCII(byte b) {
            if ((b & 128) == 0) {
                return true;
            }
            return false;
        }

        private static boolean isGBKChar(byte head, byte tail) {
            int b0 = head & Constants.BYTE_MASK;
            int b1 = tail & Constants.BYTE_MASK;
            if ((b0 < 161 || b0 > 169 || b1 < 161 || b1 > 254) && ((b0 < 176 || b0 > 247 || b1 < 161 || b1 > 254) && ((b0 < 129 || b0 > 160 || b1 < 64 || b1 > 254) && ((b0 < 170 || b0 > 254 || b1 < 64 || b1 > 160 || b1 == 127) && ((b0 < 168 || b0 > 169 || b1 < 64 || b1 > 160 || b1 == 127) && ((b0 < 170 || b0 > 175 || b1 < 161 || b1 > 254 || b1 == 127) && ((b0 < 248 || b0 > 254 || b1 < 161 || b1 > 254) && (b0 < 161 || b0 > 167 || b1 < 64 || b1 > 160 || b1 == 127)))))))) {
                return false;
            }
            return true;
        }

        private void checkAndSetIsGbkEncoding() {
            byte[] ssidBytes = this.octets.toByteArray();
            this.mIsGbkEncoding = isGBK(ssidBytes, 0, ssidBytes.length);
        }

        public boolean isGBK() {
            return this.mIsGbkEncoding;
        }
    }

    static {
        supplicantFile.add("update_config=1");
        supplicantFile.add("eapol_version=1");
        supplicantFile.add("ap_scan=1");
        supplicantFile.add("fast_reauth=1");
        supplicantFile.add("pmf=1");
        supplicantFile.add("p2p_add_cli_chan=1");
    }

    private static String changeToXmlFormat(String normalString) {
        return normalString.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static NetworkXml changeNetworkToXml(Network net) {
        NetworkXml tempXml = new NetworkXml();
        String bareSsid = net.mSsid.substring(net.mSsid.indexOf(61) + 1, net.mSsid.length());
        String allowedKeyMgmt = "";
        String bareWepkey = "";
        String bareKeyMgmt = net.mKeyMgmt.substring(net.mKeyMgmt.indexOf(61) + 1, net.mKeyMgmt.length()).replace('-', '_');
        Log.d(TAG, "Raw bareSsid = " + bareSsid);
        if (bareSsid.startsWith("\"") && bareSsid.endsWith("\"")) {
            Log.d(TAG, "It is String, no need to convert: " + bareSsid);
        } else {
            bareSsid = "\"" + WifiSsid.createFromHex(bareSsid).toString() + "\"";
        }
        Log.d(TAG, "after convert, ssid = " + bareSsid);
        bareSsid = changeToXmlFormat(bareSsid);
        tempXml.mSsid = "<string name=\"SSID\">" + bareSsid + "</string>";
        if (net.mHasWepKey) {
            bareKeyMgmt = "WEP";
            bareWepkey = net.mWepkey.substring(net.mWepkey.indexOf(61) + 1, net.mWepkey.length());
        }
        tempXml.mKeyMgmt = "<string name=\"ConfigKey\">" + bareSsid + "-" + bareKeyMgmt + "</string>";
        if (net.mPsk == "" || (bareKeyMgmt.equals("WAPI_PSK") ^ 1) == 0) {
            tempXml.mPsk = "<null name=\"PreSharedKey\" />";
        } else {
            tempXml.mPsk = "<string name=\"PreSharedKey\">" + changeToXmlFormat(net.mPsk) + "</string>";
        }
        Log.v(TAG, "changeNetworkToXml: SSID = " + bareSsid + " / " + bareKeyMgmt);
        tempXml.mRawLines.add("<WifiConfiguration>");
        tempXml.mRawLines.add(tempXml.mKeyMgmt);
        tempXml.mRawLines.add(tempXml.mSsid);
        tempXml.mRawLines.add("<string name=\"BSSID\">any</string>");
        tempXml.mRawLines.add(tempXml.mPsk);
        if (net.mHasWepKey) {
            tempXml.mRawLines.add("<string-array name=\"WEPKeys\" num=\"4\">");
            tempXml.mRawLines.add("<item value=\"" + changeToXmlFormat(bareWepkey) + "\" />");
            tempXml.mRawLines.add("<item value=\"\" />");
            tempXml.mRawLines.add("<item value=\"\" />");
            tempXml.mRawLines.add("<item value=\"\" />");
            tempXml.mRawLines.add("</string-array>");
        } else {
            tempXml.mRawLines.add("<null name=\"WEPKeys\" />");
        }
        tempXml.mRawLines.add("<int name=\"WEPTxKeyIndex\" value=\"0\" />");
        if (bareKeyMgmt.equals("NONE")) {
            if (net.mIsHidden) {
                tempXml.mRawLines.add("<boolean name=\"HiddenSSID\" value=\"true\" />");
            } else {
                tempXml.mRawLines.add("<boolean name=\"HiddenSSID\" value=\"false\" />");
            }
            tempXml.mRawLines.add("<boolean name=\"RequirePMF\" value=\"false\" />");
            tempXml.mRawLines.add("<byte-array name=\"AllowedKeyMgmt\" num=\"1\">01</byte-array>");
            tempXml.mRawLines.add("<byte-array name=\"AllowedProtocols\" num=\"1\">03</byte-array>");
        } else if (bareKeyMgmt.equals("WAPI_PSK")) {
            tempXml.mRawLines.add("<boolean name=\"HiddenSSID\" value=\"true\" />");
            tempXml.mRawLines.add("<boolean name=\"RequirePMF\" value=\"false\" />");
            tempXml.mRawLines.add("<byte-array name=\"AllowedKeyMgmt\" num=\"2\">0004</byte-array>");
            tempXml.mRawLines.add("<byte-array name=\"AllowedProtocols\" num=\"1\">0b</byte-array>");
        } else if (bareKeyMgmt.equals("WAPI_CERT")) {
            tempXml.mRawLines.add("<boolean name=\"HiddenSSID\" value=\"true\" />");
            tempXml.mRawLines.add("<boolean name=\"RequirePMF\" value=\"false\" />");
            tempXml.mRawLines.add("<byte-array name=\"AllowedKeyMgmt\" num=\"2\">0008</byte-array>");
            tempXml.mRawLines.add("<byte-array name=\"AllowedProtocols\" num=\"1\">0b</byte-array>");
        } else if (bareKeyMgmt.equals("WEP")) {
            if (net.mIsHidden) {
                tempXml.mRawLines.add("<boolean name=\"HiddenSSID\" value=\"true\" />");
            } else {
                tempXml.mRawLines.add("<boolean name=\"HiddenSSID\" value=\"false\" />");
            }
            tempXml.mRawLines.add("<boolean name=\"RequirePMF\" value=\"false\" />");
            tempXml.mRawLines.add("<byte-array name=\"AllowedKeyMgmt\" num=\"1\">01</byte-array>");
            tempXml.mRawLines.add("<byte-array name=\"AllowedProtocols\" num=\"1\">0b</byte-array>");
        } else {
            if (net.mIsHidden) {
                tempXml.mRawLines.add("<boolean name=\"HiddenSSID\" value=\"true\" />");
            } else {
                tempXml.mRawLines.add("<boolean name=\"HiddenSSID\" value=\"false\" />");
            }
            tempXml.mRawLines.add("<boolean name=\"RequirePMF\" value=\"false\" />");
            tempXml.mRawLines.add("<byte-array name=\"AllowedKeyMgmt\" num=\"1\">02</byte-array>");
            tempXml.mRawLines.add("<byte-array name=\"AllowedProtocols\" num=\"1\">03</byte-array>");
        }
        tempXml.mRawLines.add("<byte-array name=\"AllowedAuthAlgos\" num=\"1\">01</byte-array>");
        tempXml.mRawLines.add("<byte-array name=\"AllowedGroupCiphers\" num=\"1\">0f</byte-array>");
        tempXml.mRawLines.add("<byte-array name=\"AllowedPairwiseCiphers\" num=\"1\">06</byte-array>");
        if (bareKeyMgmt.equals("WAPI_PSK")) {
            tempXml.mRawLines.add("<int name=\"WapiPskType\" value=\"" + net.mWapiKeyType + "\" />");
            tempXml.mRawLines.add("<string name=\"WapiPsk\">" + changeToXmlFormat(net.mWapiPsk != "" ? net.mWapiPsk : net.mPsk) + "</string>");
        } else if (bareKeyMgmt.equals("WAPI_CERT")) {
            tempXml.mRawLines.add("<int name=\"WapiCertSelMode\" value=\"1\" />");
            if (bareSsid.equals("WAPI_CERT")) {
                tempXml.mRawLines.add("<string name=\"WapiCertSel\">user</string>");
            } else {
                tempXml.mRawLines.add("<string name=\"WapiCertSel\">as</string>");
            }
        }
        tempXml.mRawLines.add("<boolean name=\"Shared\" value=\"true\" />");
        tempXml.mRawLines.add("<int name=\"Status\" value=\"2\" />");
        tempXml.mRawLines.add("<null name=\"FQDN\" />");
        tempXml.mRawLines.add("<null name=\"ProviderFriendlyName\" />");
        tempXml.mRawLines.add("<null name=\"LinkedNetworksList\" />");
        tempXml.mRawLines.add("<null name=\"DefaultGwMacAddress\" />");
        tempXml.mRawLines.add("<boolean name=\"ValidatedInternetAccess\" value=\"false\" />");
        tempXml.mRawLines.add("<boolean name=\"NoInternetAccessExpected\" value=\"false\" />");
        tempXml.mRawLines.add("<int name=\"UserApproved\" value=\"0\" />");
        tempXml.mRawLines.add("<boolean name=\"MeteredHint\" value=\"false\" />");
        tempXml.mRawLines.add("<int name=\"MeteredOverride\" value=\"0\" />");
        tempXml.mRawLines.add("<boolean name=\"UseExternalScores\" value=\"false\" />");
        tempXml.mRawLines.add("<int name=\"NumAssociation\" value=\"2\" />");
        tempXml.mRawLines.add("<int name=\"CreatorUid\" value=\"1000\" />");
        tempXml.mRawLines.add("<string name=\"CreatorName\">android.uid.system:1000</string>");
        tempXml.mRawLines.add("<string name=\"CreationTime\">time=01-01 00:36:00.280</string>");
        tempXml.mRawLines.add("<int name=\"LastUpdateUid\" value=\"1000\" />");
        tempXml.mRawLines.add("<string name=\"LastUpdateName\">android.uid.system:1000</string>");
        tempXml.mRawLines.add("<int name=\"LastConnectUid\" value=\"1000\" />");
        tempXml.mRawLines.add("<boolean name=\"IsLegacyPasspointConfig\" value=\"false\" />");
        tempXml.mRawLines.add("<long-array name=\"RoamingConsortiumOIs\" num=\"0\" />");
        tempXml.mRawLines.add("</WifiConfiguration>");
        tempXml.mRawLines.add("<NetworkStatus>");
        tempXml.mRawLines.add("<string name=\"SelectionStatus\">NETWORK_SELECTION_ENABLED</string>");
        tempXml.mRawLines.add("<string name=\"DisableReason\">NETWORK_SELECTION_ENABLE</string>");
        tempXml.mRawLines.add("<null name=\"ConnectChoice\" />");
        tempXml.mRawLines.add("<long name=\"ConnectChoiceTimeStamp\" value=\"-1\" />");
        tempXml.mRawLines.add("<boolean name=\"HasEverConnected\" value=\"true\" />");
        tempXml.mRawLines.add("</NetworkStatus>");
        tempXml.mRawLines.add("<IpConfiguration>");
        tempXml.mRawLines.add("<string name=\"IpAssignment\">DHCP</string>");
        tempXml.mRawLines.add("<string name=\"ProxySettings\">NONE</string>");
        tempXml.mRawLines.add("</IpConfiguration>");
        return tempXml;
    }

    private static void copyWifiConfigStoreTemplate(BufferedWriter bw, ArrayList<String> templateData) {
        try {
            for (String line : templateData) {
                bw.write(line + "\n");
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Couldn't copy WifiConfigStore Template file!");
            ioe.printStackTrace();
        }
    }

    private static byte[] getFileData(String filename) {
        IOException ioe;
        Throwable th;
        InputStream is = null;
        byte[] bArr;
        try {
            File file = new File(filename);
            InputStream is2 = new FileInputStream(file);
            try {
                Log.d(TAG, "getFileData: " + filename + ", length: " + file.length());
                byte[] bytes = new byte[((int) file.length())];
                int offset = 0;
                while (offset < bytes.length) {
                    int numRead = is2.read(bytes, offset, bytes.length - offset);
                    if (numRead < 0) {
                        break;
                    }
                    offset += numRead;
                }
                if (offset < bytes.length) {
                    Log.w(TAG, "Couldn't backup " + filename);
                    bArr = EMPTY_DATA;
                    if (is2 != null) {
                        try {
                            is2.close();
                        } catch (IOException e) {
                        }
                    }
                    return bArr;
                }
                if (is2 != null) {
                    try {
                        is2.close();
                    } catch (IOException e2) {
                    }
                }
                return bytes;
            } catch (IOException e3) {
                ioe = e3;
                is = is2;
                try {
                    Log.w(TAG, "Couldn't backup " + filename);
                    ioe.printStackTrace();
                    bArr = EMPTY_DATA;
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e4) {
                        }
                    }
                    return bArr;
                } catch (Throwable th2) {
                    th = th2;
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                is = is2;
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e52) {
                    }
                }
                throw th;
            }
        } catch (IOException e6) {
            ioe = e6;
            Log.w(TAG, "Couldn't backup " + filename);
            ioe.printStackTrace();
            bArr = EMPTY_DATA;
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e42) {
                }
            }
            return bArr;
        }
    }

    private void writeSupplicantFile() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_WIFI_SUPPLICANT));
            for (String line : supplicantFile) {
                bw.write(line + "\n");
            }
            bw.close();
        } catch (IOException ioe) {
            Log.e(TAG, "Couldn't copy WifiSupplicant Raw file!");
            ioe.printStackTrace();
        }
    }

    public boolean isConfigHaveNetwork() {
        try {
            WifiNetworkSettings supplicantImage = new WifiNetworkSettings();
            if (!new File(FILE_WIFI_SUPPLICANT).exists()) {
                return isConfigHaveNetwork;
            }
            BufferedReader in;
            if (new File(FILE_WIFI_CONFIGSTORE).exists()) {
                in = new BufferedReader(new FileReader(FILE_WIFI_CONFIGSTORE));
                try {
                    String line = "";
                    while (in.ready()) {
                        line = in.readLine();
                        if (line != null && line.startsWith("<Network>")) {
                            Log.d(TAG, "WifiConfigStore.xml have networks, do nothing!");
                            in.close();
                            return false;
                        }
                    }
                    in.close();
                } catch (IOException e) {
                    Log.d(TAG, "Read WifiConfigStore.xml IOException!");
                    in.close();
                }
            }
            in = new BufferedReader(new FileReader(FILE_WIFI_SUPPLICANT));
            supplicantImage.readNetworks(in);
            in.close();
            if (isConfigHaveNetwork) {
                Log.d(TAG, "wpa_supplicant.conf has networks: " + isConfigHaveNetwork);
                writeSupplicantFile();
                WifiNetworkSettingsXml wifiConfigStoreImage = new WifiNetworkSettingsXml();
                wifiConfigStoreImage.changeNetworks(supplicantImage);
                BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_WIFI_CONFIGSTORE));
                copyWifiConfigStoreTemplate(bw, WifiNetworkSettingsXml.mRawXmlBegin);
                wifiConfigStoreImage.write(bw);
                copyWifiConfigStoreTemplate(bw, WifiNetworkSettingsXml.mRawXmlEnd);
                bw.close();
                FileUtils.setPermissions(FILE_WIFI_CONFIGSTORE, 432, Process.myUid(), 1010);
                return true;
            }
            Log.d(TAG, "wpa_supplicant.conf does not have networks!");
            return false;
        } catch (IOException ioe) {
            Log.e(TAG, "RestoreWifiConfigStore: Couldn't restore Wifi Config Store.");
            ioe.printStackTrace();
            return isConfigHaveNetwork;
        }
    }
}
