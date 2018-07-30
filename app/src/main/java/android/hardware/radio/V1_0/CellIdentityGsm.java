package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CellIdentityGsm {
    public int arfcn;
    public byte bsic;
    public int cid;
    public int lac;
    public String mcc = new String();
    public String mnc = new String();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != CellIdentityGsm.class) {
            return false;
        }
        CellIdentityGsm other = (CellIdentityGsm) otherObject;
        return HidlSupport.deepEquals(this.mcc, other.mcc) && HidlSupport.deepEquals(this.mnc, other.mnc) && this.lac == other.lac && this.cid == other.cid && this.arfcn == other.arfcn && this.bsic == other.bsic;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.mcc)), Integer.valueOf(HidlSupport.deepHashCode(this.mnc)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.lac))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cid))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.arfcn))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.bsic)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".mcc = ");
        builder.append(this.mcc);
        builder.append(", .mnc = ");
        builder.append(this.mnc);
        builder.append(", .lac = ");
        builder.append(this.lac);
        builder.append(", .cid = ");
        builder.append(this.cid);
        builder.append(", .arfcn = ");
        builder.append(this.arfcn);
        builder.append(", .bsic = ");
        builder.append(this.bsic);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(48), 0);
    }

    public static final ArrayList<CellIdentityGsm> readVectorFromParcel(HwParcel parcel) {
        ArrayList<CellIdentityGsm> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 48), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            CellIdentityGsm _hidl_vec_element = new CellIdentityGsm();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 48));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.mcc = _hidl_blob.getString(0 + _hidl_offset);
        parcel.readEmbeddedBuffer((long) (this.mcc.getBytes().length + 1), _hidl_blob.handle(), 0 + (0 + _hidl_offset), false);
        this.mnc = _hidl_blob.getString(16 + _hidl_offset);
        parcel.readEmbeddedBuffer((long) (this.mnc.getBytes().length + 1), _hidl_blob.handle(), 0 + (16 + _hidl_offset), false);
        this.lac = _hidl_blob.getInt32(32 + _hidl_offset);
        this.cid = _hidl_blob.getInt32(36 + _hidl_offset);
        this.arfcn = _hidl_blob.getInt32(40 + _hidl_offset);
        this.bsic = _hidl_blob.getInt8(44 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(48);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<CellIdentityGsm> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 48);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CellIdentityGsm) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 48));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putString(0 + _hidl_offset, this.mcc);
        _hidl_blob.putString(16 + _hidl_offset, this.mnc);
        _hidl_blob.putInt32(32 + _hidl_offset, this.lac);
        _hidl_blob.putInt32(36 + _hidl_offset, this.cid);
        _hidl_blob.putInt32(40 + _hidl_offset, this.arfcn);
        _hidl_blob.putInt8(44 + _hidl_offset, this.bsic);
    }
}
