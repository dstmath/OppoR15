package android.net.lowpan;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Objects;

public class LowpanProvision implements Parcelable {
    public static final Creator<LowpanProvision> CREATOR = new Creator<LowpanProvision>() {
        public LowpanProvision createFromParcel(Parcel in) {
            Builder builder = new Builder();
            builder.setLowpanIdentity((LowpanIdentity) LowpanIdentity.CREATOR.createFromParcel(in));
            if (in.readBoolean()) {
                builder.setLowpanCredential((LowpanCredential) LowpanCredential.CREATOR.createFromParcel(in));
            }
            return builder.build();
        }

        public LowpanProvision[] newArray(int size) {
            return new LowpanProvision[size];
        }
    };
    private LowpanCredential mCredential;
    private LowpanIdentity mIdentity;

    public static class Builder {
        private final LowpanProvision provision = new LowpanProvision();

        public Builder setLowpanIdentity(LowpanIdentity identity) {
            this.provision.mIdentity = identity;
            return this;
        }

        public Builder setLowpanCredential(LowpanCredential credential) {
            this.provision.mCredential = credential;
            return this;
        }

        public LowpanProvision build() {
            return this.provision;
        }
    }

    /* synthetic */ LowpanProvision(LowpanProvision -this0) {
        this();
    }

    private LowpanProvision() {
        this.mIdentity = new LowpanIdentity();
        this.mCredential = null;
    }

    public LowpanIdentity getLowpanIdentity() {
        return this.mIdentity;
    }

    public LowpanCredential getLowpanCredential() {
        return this.mCredential;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("LowpanProvision { identity => ").append(this.mIdentity.toString());
        if (this.mCredential != null) {
            sb.append(", credential => ").append(this.mCredential.toString());
        }
        sb.append("}");
        return sb.toString();
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.mIdentity, this.mCredential});
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LowpanProvision)) {
            return false;
        }
        LowpanProvision rhs = (LowpanProvision) obj;
        if (this.mIdentity.equals(rhs.mIdentity) && Objects.equals(this.mCredential, rhs.mCredential)) {
            return true;
        }
        return false;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        this.mIdentity.writeToParcel(dest, flags);
        if (this.mCredential == null) {
            dest.writeBoolean(false);
            return;
        }
        dest.writeBoolean(true);
        this.mCredential.writeToParcel(dest, flags);
    }
}
