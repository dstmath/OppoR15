package com.android.server.wifi.hotspot2.anqp;

import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class HSIconFileElement extends ANQPElement {
    public static final int STATUS_CODE_FILE_NOT_FOUND = 1;
    public static final int STATUS_CODE_SUCCESS = 0;
    public static final int STATUS_CODE_UNSPECIFIED_ERROR = 2;
    private static final String TAG = "HSIconFileElement";
    private final byte[] mIconData;
    private final String mIconType;
    private final int mStatusCode;

    public HSIconFileElement(int statusCode, String iconType, byte[] iconData) {
        super(ANQPElementType.HSIconFile);
        this.mStatusCode = statusCode;
        this.mIconType = iconType;
        this.mIconData = iconData;
    }

    public static HSIconFileElement parse(ByteBuffer payload) throws ProtocolException {
        int status = payload.get() & Constants.BYTE_MASK;
        if (status != 0) {
            Log.e(TAG, "Icon file download failed: " + status);
            return new HSIconFileElement(status, null, null);
        }
        String iconType = ByteBufferReader.readStringWithByteLength(payload, StandardCharsets.US_ASCII);
        byte[] iconData = new byte[(((int) ByteBufferReader.readInteger(payload, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK)];
        payload.get(iconData);
        return new HSIconFileElement(status, iconType, iconData);
    }

    public boolean equals(Object thatObject) {
        boolean z = false;
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof HSIconFileElement)) {
            return false;
        }
        HSIconFileElement that = (HSIconFileElement) thatObject;
        if (this.mStatusCode == that.mStatusCode && TextUtils.equals(this.mIconType, that.mIconType)) {
            z = Arrays.equals(this.mIconData, that.mIconData);
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.mStatusCode), this.mIconType, Integer.valueOf(Arrays.hashCode(this.mIconData))});
    }

    public String toString() {
        return "HSIconFileElement{mStatusCode=" + this.mStatusCode + "mIconType=" + this.mIconType + "}";
    }
}
