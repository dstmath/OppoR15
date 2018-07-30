package android.net.lowpan;

import android.icu.text.StringPrep;
import android.icu.text.StringPrepParseException;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import com.android.internal.util.HexDump;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class LowpanIdentity implements Parcelable {
    public static final Creator<LowpanIdentity> CREATOR = new Creator<LowpanIdentity>() {
        public LowpanIdentity createFromParcel(Parcel in) {
            Builder builder = new Builder();
            builder.setRawName(in.createByteArray());
            builder.setType(in.readString());
            builder.setXpanid(in.createByteArray());
            builder.setPanid(in.readInt());
            builder.setChannel(in.readInt());
            return builder.build();
        }

        public LowpanIdentity[] newArray(int size) {
            return new LowpanIdentity[size];
        }
    };
    private static final String TAG = LowpanIdentity.class.getSimpleName();
    public static final int UNSPECIFIED_CHANNEL = -1;
    public static final int UNSPECIFIED_PANID = -1;
    private int mChannel = -1;
    private boolean mIsNameValid = true;
    private String mName = "";
    private int mPanid = -1;
    private byte[] mRawName = new byte[0];
    private String mType = "";
    private byte[] mXpanid = new byte[0];

    public static class Builder {
        private static final StringPrep stringPrep = StringPrep.getInstance(8);
        final LowpanIdentity mIdentity = new LowpanIdentity();

        private static String escape(byte[] bytes) {
            StringBuffer sb = new StringBuffer();
            for (byte b : bytes) {
                if (b < (byte) 32 || b > (byte) 126) {
                    sb.append(String.format("\\0x%02x", new Object[]{Integer.valueOf(b & 255)}));
                } else {
                    sb.append((char) b);
                }
            }
            return sb.toString();
        }

        public Builder setLowpanIdentity(LowpanIdentity x) {
            Objects.requireNonNull(x);
            setRawName(x.getRawName());
            setXpanid(x.getXpanid());
            setPanid(x.getPanid());
            setChannel(x.getChannel());
            setType(x.getType());
            return this;
        }

        public Builder setName(String name) {
            Objects.requireNonNull(name);
            try {
                this.mIdentity.mName = stringPrep.prepare(name, 0);
                this.mIdentity.mRawName = this.mIdentity.mName.getBytes(StandardCharsets.UTF_8);
                this.mIdentity.mIsNameValid = true;
            } catch (StringPrepParseException x) {
                Log.w(LowpanIdentity.TAG, x.toString());
                setRawName(name.getBytes(StandardCharsets.UTF_8));
            }
            return this;
        }

        public Builder setRawName(byte[] name) {
            Objects.requireNonNull(name);
            this.mIdentity.mRawName = (byte[]) name.clone();
            this.mIdentity.mName = new String(name, StandardCharsets.UTF_8);
            try {
                this.mIdentity.mIsNameValid = Arrays.equals(stringPrep.prepare(this.mIdentity.mName, 0).getBytes(StandardCharsets.UTF_8), name);
            } catch (StringPrepParseException x) {
                Log.w(LowpanIdentity.TAG, x.toString());
                this.mIdentity.mIsNameValid = false;
            }
            if (!this.mIdentity.mIsNameValid) {
                this.mIdentity.mName = "«" + escape(name) + "»";
            }
            return this;
        }

        public Builder setXpanid(byte[] x) {
            byte[] bArr = null;
            LowpanIdentity lowpanIdentity = this.mIdentity;
            if (x != null) {
                bArr = (byte[]) x.clone();
            }
            lowpanIdentity.mXpanid = bArr;
            return this;
        }

        public Builder setPanid(int x) {
            this.mIdentity.mPanid = x;
            return this;
        }

        public Builder setType(String x) {
            this.mIdentity.mType = x;
            return this;
        }

        public Builder setChannel(int x) {
            this.mIdentity.mChannel = x;
            return this;
        }

        public LowpanIdentity build() {
            return this.mIdentity;
        }
    }

    LowpanIdentity() {
    }

    public String getName() {
        return this.mName;
    }

    public boolean isNameValid() {
        return this.mIsNameValid;
    }

    public byte[] getRawName() {
        return (byte[]) this.mRawName.clone();
    }

    public byte[] getXpanid() {
        return (byte[]) this.mXpanid.clone();
    }

    public int getPanid() {
        return this.mPanid;
    }

    public String getType() {
        return this.mType;
    }

    public int getChannel() {
        return this.mChannel;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Name:").append(getName());
        if (this.mType.length() > 0) {
            sb.append(", Type:").append(this.mType);
        }
        if (this.mXpanid.length > 0) {
            sb.append(", XPANID:").append(HexDump.toHexString(this.mXpanid));
        }
        if (this.mPanid != -1) {
            sb.append(", PANID:").append(String.format("0x%04X", new Object[]{Integer.valueOf(this.mPanid)}));
        }
        if (this.mChannel != -1) {
            sb.append(", Channel:").append(this.mChannel);
        }
        return sb.toString();
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof LowpanIdentity)) {
            return false;
        }
        LowpanIdentity rhs = (LowpanIdentity) obj;
        if (Arrays.equals(this.mRawName, rhs.mRawName) && Arrays.equals(this.mXpanid, rhs.mXpanid) && this.mType.equals(rhs.mType) && this.mPanid == rhs.mPanid && this.mChannel == rhs.mChannel) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(Arrays.hashCode(this.mRawName)), this.mType, Integer.valueOf(Arrays.hashCode(this.mXpanid)), Integer.valueOf(this.mPanid), Integer.valueOf(this.mChannel)});
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.mRawName);
        dest.writeString(this.mType);
        dest.writeByteArray(this.mXpanid);
        dest.writeInt(this.mPanid);
        dest.writeInt(this.mChannel);
    }
}
