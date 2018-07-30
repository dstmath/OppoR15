package android.hardware.tetheroffload.control.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NatTimeoutUpdate {
    public final IPv4AddrPortPair dst = new IPv4AddrPortPair();
    public int proto;
    public final IPv4AddrPortPair src = new IPv4AddrPortPair();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != NatTimeoutUpdate.class) {
            return false;
        }
        NatTimeoutUpdate other = (NatTimeoutUpdate) otherObject;
        return HidlSupport.deepEquals(this.src, other.src) && HidlSupport.deepEquals(this.dst, other.dst) && this.proto == other.proto;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.src)), Integer.valueOf(HidlSupport.deepHashCode(this.dst)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.proto)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".src = ");
        builder.append(this.src);
        builder.append(", .dst = ");
        builder.append(this.dst);
        builder.append(", .proto = ");
        builder.append(NetworkProtocol.toString(this.proto));
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(56), 0);
    }

    public static final ArrayList<NatTimeoutUpdate> readVectorFromParcel(HwParcel parcel) {
        ArrayList<NatTimeoutUpdate> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 56), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            NatTimeoutUpdate _hidl_vec_element = new NatTimeoutUpdate();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 56));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.src.readEmbeddedFromParcel(parcel, _hidl_blob, 0 + _hidl_offset);
        this.dst.readEmbeddedFromParcel(parcel, _hidl_blob, 24 + _hidl_offset);
        this.proto = _hidl_blob.getInt32(48 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(56);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<NatTimeoutUpdate> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 56);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((NatTimeoutUpdate) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 56));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        this.src.writeEmbeddedToBlob(_hidl_blob, 0 + _hidl_offset);
        this.dst.writeEmbeddedToBlob(_hidl_blob, 24 + _hidl_offset);
        _hidl_blob.putInt32(48 + _hidl_offset, this.proto);
    }
}
