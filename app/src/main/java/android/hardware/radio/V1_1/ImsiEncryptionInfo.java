package android.hardware.radio.V1_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class ImsiEncryptionInfo {
    public final ArrayList<Byte> carrierKey = new ArrayList();
    public long expirationTime;
    public String keyIdentifier = new String();
    public String mcc = new String();
    public String mnc = new String();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != ImsiEncryptionInfo.class) {
            return false;
        }
        ImsiEncryptionInfo other = (ImsiEncryptionInfo) otherObject;
        return HidlSupport.deepEquals(this.mcc, other.mcc) && HidlSupport.deepEquals(this.mnc, other.mnc) && HidlSupport.deepEquals(this.carrierKey, other.carrierKey) && HidlSupport.deepEquals(this.keyIdentifier, other.keyIdentifier) && this.expirationTime == other.expirationTime;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.mcc)), Integer.valueOf(HidlSupport.deepHashCode(this.mnc)), Integer.valueOf(HidlSupport.deepHashCode(this.carrierKey)), Integer.valueOf(HidlSupport.deepHashCode(this.keyIdentifier)), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.expirationTime)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".mcc = ");
        builder.append(this.mcc);
        builder.append(", .mnc = ");
        builder.append(this.mnc);
        builder.append(", .carrierKey = ");
        builder.append(this.carrierKey);
        builder.append(", .keyIdentifier = ");
        builder.append(this.keyIdentifier);
        builder.append(", .expirationTime = ");
        builder.append(this.expirationTime);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(72), 0);
    }

    public static final ArrayList<ImsiEncryptionInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<ImsiEncryptionInfo> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 72), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ImsiEncryptionInfo _hidl_vec_element = new ImsiEncryptionInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 72));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.mcc = _hidl_blob.getString(0 + _hidl_offset);
        parcel.readEmbeddedBuffer((long) (this.mcc.getBytes().length + 1), _hidl_blob.handle(), (0 + _hidl_offset) + 0, false);
        this.mnc = _hidl_blob.getString(16 + _hidl_offset);
        parcel.readEmbeddedBuffer((long) (this.mnc.getBytes().length + 1), _hidl_blob.handle(), (16 + _hidl_offset) + 0, false);
        int _hidl_vec_size = _hidl_blob.getInt32((32 + _hidl_offset) + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 1), _hidl_blob.handle(), (32 + _hidl_offset) + 0, true);
        this.carrierKey.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.carrierKey.add(Byte.valueOf(childBlob.getInt8((long) (_hidl_index_0 * 1))));
        }
        this.keyIdentifier = _hidl_blob.getString(48 + _hidl_offset);
        parcel.readEmbeddedBuffer((long) (this.keyIdentifier.getBytes().length + 1), _hidl_blob.handle(), (48 + _hidl_offset) + 0, false);
        this.expirationTime = _hidl_blob.getInt64(64 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(72);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<ImsiEncryptionInfo> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 72);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((ImsiEncryptionInfo) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 72));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putString(_hidl_offset + 0, this.mcc);
        _hidl_blob.putString(16 + _hidl_offset, this.mnc);
        int _hidl_vec_size = this.carrierKey.size();
        _hidl_blob.putInt32((_hidl_offset + 32) + 8, _hidl_vec_size);
        _hidl_blob.putBool((_hidl_offset + 32) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 1);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt8((long) (_hidl_index_0 * 1), ((Byte) this.carrierKey.get(_hidl_index_0)).byteValue());
        }
        _hidl_blob.putBlob((_hidl_offset + 32) + 0, childBlob);
        _hidl_blob.putString(48 + _hidl_offset, this.keyIdentifier);
        _hidl_blob.putInt64(64 + _hidl_offset, this.expirationTime);
    }
}
