package com.android.server.wifi.util;

import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import libcore.util.HexEncoding;

public class NativeUtil {
    public static final byte[] ANY_MAC_BYTES = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private static final String ANY_MAC_STR = "any";
    private static final int MAC_LENGTH = 6;
    private static final int MAC_OUI_LENGTH = 3;
    private static final int MAC_STR_LENGTH = 17;
    private static final String TAG = "NativeUtil";

    public static ArrayList<Byte> stringToByteArrayList(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null string");
        }
        ArrayList<Byte> byteArrayList = new ArrayList();
        for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
            byteArrayList.add(new Byte(b));
        }
        return byteArrayList;
    }

    public static String stringFromByteArrayList(ArrayList<Byte> byteArrayList) {
        if (byteArrayList == null) {
            throw new IllegalArgumentException("null byte array list");
        }
        byte[] byteArray = new byte[byteArrayList.size()];
        int i = 0;
        for (Byte b : byteArrayList) {
            byteArray[i] = b.byteValue();
            i++;
        }
        return new String(byteArray, StandardCharsets.UTF_8);
    }

    public static byte[] stringToByteArray(String str) {
        if (str != null) {
            return str.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("null string");
    }

    public static String stringFromByteArray(byte[] byteArray) {
        if (byteArray != null) {
            return new String(byteArray);
        }
        throw new IllegalArgumentException("null byte array");
    }

    public static byte[] macAddressToByteArray(String macStr) {
        if (TextUtils.isEmpty(macStr) || "any".equals(macStr)) {
            return ANY_MAC_BYTES;
        }
        String cleanMac = macStr.replace(":", "");
        if (cleanMac.length() == 12) {
            return HexEncoding.decode(cleanMac.toCharArray(), false);
        }
        throw new IllegalArgumentException("invalid mac string length: " + cleanMac);
    }

    public static String macAddressFromByteArray(byte[] macArray) {
        if (macArray == null) {
            throw new IllegalArgumentException("null mac bytes");
        } else if (macArray.length != 6) {
            throw new IllegalArgumentException("invalid macArray length: " + macArray.length);
        } else {
            StringBuilder sb = new StringBuilder(17);
            for (int i = 0; i < macArray.length; i++) {
                if (i != 0) {
                    sb.append(":");
                }
                sb.append(new String(HexEncoding.encode(macArray, i, 1)));
            }
            return sb.toString().toLowerCase();
        }
    }

    public static byte[] macAddressOuiToByteArray(String macStr) {
        if (macStr == null) {
            throw new IllegalArgumentException("null mac string");
        }
        String cleanMac = macStr.replace(":", "");
        if (cleanMac.length() == 6) {
            return HexEncoding.decode(cleanMac.toCharArray(), false);
        }
        throw new IllegalArgumentException("invalid mac oui string length: " + cleanMac);
    }

    public static Long macAddressToLong(byte[] macArray) {
        if (macArray == null) {
            throw new IllegalArgumentException("null mac bytes");
        } else if (macArray.length != 6) {
            throw new IllegalArgumentException("invalid macArray length: " + macArray.length);
        } else {
            try {
                return Long.valueOf(ByteBufferReader.readInteger(ByteBuffer.wrap(macArray), ByteOrder.BIG_ENDIAN, macArray.length));
            } catch (BufferUnderflowException e) {
                throw new IllegalArgumentException("invalid macArray");
            }
        }
    }

    public static String removeEnclosingQuotes(String quotedStr) {
        int length = quotedStr.length();
        if (length >= 2 && quotedStr.charAt(0) == '\"' && quotedStr.charAt(length - 1) == '\"') {
            return quotedStr.substring(1, length - 1);
        }
        return quotedStr;
    }

    public static String addEnclosingQuotes(String str) {
        return "\"" + str + "\"";
    }

    public static ArrayList<Byte> hexOrQuotedStringToBytes(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null string");
        }
        int length = str.length();
        if (length > 1 && str.charAt(0) == '\"' && str.charAt(length - 1) == '\"') {
            return stringToByteArrayList(str.substring(1, str.length() - 1));
        }
        return byteArrayToArrayList(hexStringToByteArray(str));
    }

    public static String bytesToHexOrQuotedString(ArrayList<Byte> bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("null ssid bytes");
        }
        byte[] byteArray = byteArrayFromArrayList(bytes);
        if (!bytes.contains(Byte.valueOf((byte) 0))) {
            try {
                return "\"" + StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(byteArray)).toString() + "\"";
            } catch (CharacterCodingException cce) {
                Log.e(TAG, "Cannot decode ssid! CharacterCodingException cce =  " + cce);
            }
        }
        return hexStringFromByteArray(byteArray);
    }

    public static ArrayList<Byte> decodeSsid(String ssidStr) {
        return hexOrQuotedStringToBytes(ssidStr);
    }

    public static String encodeSsid(ArrayList<Byte> ssidBytes) {
        return bytesToHexOrQuotedString(ssidBytes);
    }

    public static boolean isAllAscii(ArrayList<Byte> ssidBytes) {
        if (ssidBytes == null) {
            return false;
        }
        int size = ssidBytes.size();
        for (int i = 0; i < size; i++) {
            if (((Byte) ssidBytes.get(i)).byteValue() < (byte) 0) {
                return false;
            }
        }
        return true;
    }

    public static byte[] getSsidBytes(String ssid, String charsetName) {
        if (ssid == null) {
            return null;
        }
        byte[] ssidBytes = null;
        try {
            ssidBytes = removeEnclosingQuotes(ssid).getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
        }
        return ssidBytes;
    }

    public static String encodeSsidByCharset(byte[] ssidBytes, String name) {
        String ssid = null;
        try {
            return "\"" + Charset.forName(name).newDecoder().decode(ByteBuffer.wrap(ssidBytes)).toString() + "\"";
        } catch (UnsupportedCharsetException e) {
            return ssid;
        } catch (CharacterCodingException e2) {
            return ssid;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isUtf(byte[] ssidBytes) {
        return ssidBytes == null || ssidBytes.length == 0 || encodeSsidByCharset(ssidBytes, "UTF-8") != null;
    }

    public static ArrayList<Byte> byteArrayToArrayList(byte[] bytes) {
        ArrayList<Byte> byteList = new ArrayList();
        for (byte valueOf : bytes) {
            byteList.add(Byte.valueOf(valueOf));
        }
        return byteList;
    }

    public static byte[] byteArrayFromArrayList(ArrayList<Byte> bytes) {
        byte[] byteArray = new byte[bytes.size()];
        int i = 0;
        for (Byte b : bytes) {
            int i2 = i + 1;
            byteArray[i] = b.byteValue();
            i = i2;
        }
        return byteArray;
    }

    public static byte[] hexStringToByteArray(String hexStr) {
        if (hexStr != null) {
            return HexEncoding.decode(hexStr.toCharArray(), false);
        }
        throw new IllegalArgumentException("null hex string");
    }

    public static String hexStringFromByteArray(byte[] bytes) {
        if (bytes != null) {
            return new String(HexEncoding.encode(bytes)).toLowerCase();
        }
        throw new IllegalArgumentException("null hex bytes");
    }

    public static String wpsDevTypeStringFromByteArray(byte[] devType) {
        byte[] a = devType;
        int x = ((devType[0] & Constants.BYTE_MASK) << 8) | (devType[1] & Constants.BYTE_MASK);
        String y = new String(HexEncoding.encode(Arrays.copyOfRange(devType, 2, 6)));
        int z = ((devType[6] & Constants.BYTE_MASK) << 8) | (devType[7] & Constants.BYTE_MASK);
        return String.format("%d-%s-%d", new Object[]{Integer.valueOf(x), y, Integer.valueOf(z)});
    }
}
