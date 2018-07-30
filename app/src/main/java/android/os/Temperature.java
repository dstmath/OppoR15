package android.os;

import android.os.Parcelable.Creator;

public class Temperature implements Parcelable {
    public static final Creator<Temperature> CREATOR = new Creator<Temperature>() {
        public Temperature createFromParcel(Parcel p) {
            return new Temperature(p, null);
        }

        public Temperature[] newArray(int size) {
            return new Temperature[size];
        }
    };
    private int mType;
    private float mValue;

    /* synthetic */ Temperature(Parcel p, Temperature -this1) {
        this(p);
    }

    public Temperature() {
        this.mType = Integer.MIN_VALUE;
        this.mValue = -3.4028235E38f;
    }

    public Temperature(float value, int type) {
        this.mValue = value;
        this.mType = type;
    }

    public float getValue() {
        return this.mValue;
    }

    public int getType() {
        return this.mType;
    }

    private Temperature(Parcel p) {
        readFromParcel(p);
    }

    public void readFromParcel(Parcel p) {
        this.mValue = p.readFloat();
        this.mType = p.readInt();
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeFloat(this.mValue);
        p.writeInt(this.mType);
    }

    public int describeContents() {
        return 0;
    }
}
