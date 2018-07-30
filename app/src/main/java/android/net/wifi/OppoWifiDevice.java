package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class OppoWifiDevice implements Parcelable {
    public static final int CONNECTED = 1;
    public static final Creator<OppoWifiDevice> CREATOR = new Creator<OppoWifiDevice>() {
        public OppoWifiDevice createFromParcel(Parcel in) {
            OppoWifiDevice device = new OppoWifiDevice();
            device.deviceAddress = in.readString();
            device.deviceName = in.readString();
            device.deviceState = in.readInt();
            return device;
        }

        public OppoWifiDevice[] newArray(int size) {
            return new OppoWifiDevice[size];
        }
    };
    public static final int DISCONNECTED = 0;
    public String deviceAddress = "";
    public String deviceName = "";
    public int deviceState = 0;

    public OppoWifiDevice(String deviceAddress, boolean isConnected) {
        if (isConnected) {
            this.deviceState = 1;
        } else if (isConnected) {
            this.deviceState = 0;
        }
        this.deviceAddress = deviceAddress;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null || ((obj instanceof OppoWifiDevice) ^ 1) != 0) {
            return false;
        }
        OppoWifiDevice other = (OppoWifiDevice) obj;
        if (this.deviceAddress != null) {
            return this.deviceAddress.equals(other.deviceAddress);
        }
        if (other.deviceAddress == null) {
            z = true;
        }
        return z;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.deviceAddress);
        dest.writeString(this.deviceName);
        dest.writeInt(this.deviceState);
    }
}
