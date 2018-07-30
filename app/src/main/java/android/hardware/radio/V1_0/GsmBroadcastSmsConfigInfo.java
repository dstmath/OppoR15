package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class GsmBroadcastSmsConfigInfo {
    public int fromCodeScheme;
    public int fromServiceId;
    public boolean selected;
    public int toCodeScheme;
    public int toServiceId;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != GsmBroadcastSmsConfigInfo.class) {
            return false;
        }
        GsmBroadcastSmsConfigInfo other = (GsmBroadcastSmsConfigInfo) otherObject;
        return this.fromServiceId == other.fromServiceId && this.toServiceId == other.toServiceId && this.fromCodeScheme == other.fromCodeScheme && this.toCodeScheme == other.toCodeScheme && this.selected == other.selected;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.fromServiceId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.toServiceId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.fromCodeScheme))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.toCodeScheme))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.selected)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".fromServiceId = ");
        builder.append(this.fromServiceId);
        builder.append(", .toServiceId = ");
        builder.append(this.toServiceId);
        builder.append(", .fromCodeScheme = ");
        builder.append(this.fromCodeScheme);
        builder.append(", .toCodeScheme = ");
        builder.append(this.toCodeScheme);
        builder.append(", .selected = ");
        builder.append(this.selected);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(20), 0);
    }

    public static final ArrayList<GsmBroadcastSmsConfigInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<GsmBroadcastSmsConfigInfo> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 20), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            GsmBroadcastSmsConfigInfo _hidl_vec_element = new GsmBroadcastSmsConfigInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 20));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.fromServiceId = _hidl_blob.getInt32(0 + _hidl_offset);
        this.toServiceId = _hidl_blob.getInt32(4 + _hidl_offset);
        this.fromCodeScheme = _hidl_blob.getInt32(8 + _hidl_offset);
        this.toCodeScheme = _hidl_blob.getInt32(12 + _hidl_offset);
        this.selected = _hidl_blob.getBool(16 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(20);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<GsmBroadcastSmsConfigInfo> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 20);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((GsmBroadcastSmsConfigInfo) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 20));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(0 + _hidl_offset, this.fromServiceId);
        _hidl_blob.putInt32(4 + _hidl_offset, this.toServiceId);
        _hidl_blob.putInt32(8 + _hidl_offset, this.fromCodeScheme);
        _hidl_blob.putInt32(12 + _hidl_offset, this.toCodeScheme);
        _hidl_blob.putBool(16 + _hidl_offset, this.selected);
    }
}
