package com.qualcomm.qti.imscmservice.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class IMSCM_CONN_MESSAGE {
    public int eMessageType;
    public int eProtocol;
    public int iMessageLen;
    public short iRemotePort;
    public String pCallId = new String();
    public String pMessage = new String();
    public String pOutboundProxy = new String();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != IMSCM_CONN_MESSAGE.class) {
            return false;
        }
        IMSCM_CONN_MESSAGE other = (IMSCM_CONN_MESSAGE) otherObject;
        return HidlSupport.deepEquals(this.pOutboundProxy, other.pOutboundProxy) && this.iRemotePort == other.iRemotePort && this.eProtocol == other.eProtocol && this.eMessageType == other.eMessageType && HidlSupport.deepEquals(this.pCallId, other.pCallId) && HidlSupport.deepEquals(this.pMessage, other.pMessage) && this.iMessageLen == other.iMessageLen;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.pOutboundProxy)), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.iRemotePort))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.eProtocol))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.eMessageType))), Integer.valueOf(HidlSupport.deepHashCode(this.pCallId)), Integer.valueOf(HidlSupport.deepHashCode(this.pMessage)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.iMessageLen)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".pOutboundProxy = ");
        builder.append(this.pOutboundProxy);
        builder.append(", .iRemotePort = ");
        builder.append(this.iRemotePort);
        builder.append(", .eProtocol = ");
        builder.append(IMSCM_SIP_PROTOCOL.toString(this.eProtocol));
        builder.append(", .eMessageType = ");
        builder.append(IMSCM_MESSAGE_TYPE.toString(this.eMessageType));
        builder.append(", .pCallId = ");
        builder.append(this.pCallId);
        builder.append(", .pMessage = ");
        builder.append(this.pMessage);
        builder.append(", .iMessageLen = ");
        builder.append(this.iMessageLen);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(72), 0);
    }

    public static final ArrayList<IMSCM_CONN_MESSAGE> readVectorFromParcel(HwParcel parcel) {
        ArrayList<IMSCM_CONN_MESSAGE> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 72), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            IMSCM_CONN_MESSAGE _hidl_vec_element = new IMSCM_CONN_MESSAGE();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 72));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.pOutboundProxy = _hidl_blob.getString(0 + _hidl_offset);
        parcel.readEmbeddedBuffer((long) (this.pOutboundProxy.getBytes().length + 1), _hidl_blob.handle(), 0 + (0 + _hidl_offset), false);
        this.iRemotePort = _hidl_blob.getInt16(16 + _hidl_offset);
        this.eProtocol = _hidl_blob.getInt32(20 + _hidl_offset);
        this.eMessageType = _hidl_blob.getInt32(24 + _hidl_offset);
        this.pCallId = _hidl_blob.getString(32 + _hidl_offset);
        parcel.readEmbeddedBuffer((long) (this.pCallId.getBytes().length + 1), _hidl_blob.handle(), 0 + (32 + _hidl_offset), false);
        this.pMessage = _hidl_blob.getString(48 + _hidl_offset);
        parcel.readEmbeddedBuffer((long) (this.pMessage.getBytes().length + 1), _hidl_blob.handle(), 0 + (48 + _hidl_offset), false);
        this.iMessageLen = _hidl_blob.getInt32(64 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(72);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<IMSCM_CONN_MESSAGE> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 72);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((IMSCM_CONN_MESSAGE) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 72));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putString(0 + _hidl_offset, this.pOutboundProxy);
        _hidl_blob.putInt16(16 + _hidl_offset, this.iRemotePort);
        _hidl_blob.putInt32(20 + _hidl_offset, this.eProtocol);
        _hidl_blob.putInt32(24 + _hidl_offset, this.eMessageType);
        _hidl_blob.putString(32 + _hidl_offset, this.pCallId);
        _hidl_blob.putString(48 + _hidl_offset, this.pMessage);
        _hidl_blob.putInt32(64 + _hidl_offset, this.iMessageLen);
    }
}
