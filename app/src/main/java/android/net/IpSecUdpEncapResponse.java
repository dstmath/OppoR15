package android.net;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.io.FileDescriptor;
import java.io.IOException;

public final class IpSecUdpEncapResponse implements Parcelable {
    public static final Creator<IpSecUdpEncapResponse> CREATOR = new Creator<IpSecUdpEncapResponse>() {
        public IpSecUdpEncapResponse createFromParcel(Parcel in) {
            return new IpSecUdpEncapResponse(in, null);
        }

        public IpSecUdpEncapResponse[] newArray(int size) {
            return new IpSecUdpEncapResponse[size];
        }
    };
    private static final String TAG = "IpSecUdpEncapResponse";
    public final ParcelFileDescriptor fileDescriptor;
    public final int port;
    public final int resourceId;
    public final int status;

    /* synthetic */ IpSecUdpEncapResponse(Parcel in, IpSecUdpEncapResponse -this1) {
        this(in);
    }

    public int describeContents() {
        return this.fileDescriptor != null ? 1 : 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.status);
        out.writeInt(this.resourceId);
        out.writeInt(this.port);
        out.writeParcelable(this.fileDescriptor, 1);
    }

    public IpSecUdpEncapResponse(int inStatus) {
        if (inStatus == 0) {
            throw new IllegalArgumentException("Valid status implies other args must be provided");
        }
        this.status = inStatus;
        this.resourceId = 0;
        this.port = -1;
        this.fileDescriptor = null;
    }

    public IpSecUdpEncapResponse(int inStatus, int inResourceId, int inPort, FileDescriptor inFd) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = null;
        if (inStatus == 0 && inFd == null) {
            throw new IllegalArgumentException("Valid status implies FD must be non-null");
        }
        this.status = inStatus;
        this.resourceId = inResourceId;
        this.port = inPort;
        if (this.status == 0) {
            parcelFileDescriptor = ParcelFileDescriptor.dup(inFd);
        }
        this.fileDescriptor = parcelFileDescriptor;
    }

    private IpSecUdpEncapResponse(Parcel in) {
        this.status = in.readInt();
        this.resourceId = in.readInt();
        this.port = in.readInt();
        this.fileDescriptor = (ParcelFileDescriptor) in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
    }
}
