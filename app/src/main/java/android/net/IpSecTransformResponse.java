package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class IpSecTransformResponse implements Parcelable {
    public static final Creator<IpSecTransformResponse> CREATOR = new Creator<IpSecTransformResponse>() {
        public IpSecTransformResponse createFromParcel(Parcel in) {
            return new IpSecTransformResponse(in, null);
        }

        public IpSecTransformResponse[] newArray(int size) {
            return new IpSecTransformResponse[size];
        }
    };
    private static final String TAG = "IpSecTransformResponse";
    public final int resourceId;
    public final int status;

    /* synthetic */ IpSecTransformResponse(Parcel in, IpSecTransformResponse -this1) {
        this(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.status);
        out.writeInt(this.resourceId);
    }

    public IpSecTransformResponse(int inStatus) {
        if (inStatus == 0) {
            throw new IllegalArgumentException("Valid status implies other args must be provided");
        }
        this.status = inStatus;
        this.resourceId = 0;
    }

    public IpSecTransformResponse(int inStatus, int inResourceId) {
        this.status = inStatus;
        this.resourceId = inResourceId;
    }

    private IpSecTransformResponse(Parcel in) {
        this.status = in.readInt();
        this.resourceId = in.readInt();
    }
}
