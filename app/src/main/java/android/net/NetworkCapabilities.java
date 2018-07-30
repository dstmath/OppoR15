package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.android.internal.util.BitUtils;
import com.android.internal.util.Preconditions;
import java.util.Objects;
import java.util.StringJoiner;

public final class NetworkCapabilities implements Parcelable {
    public static final Creator<NetworkCapabilities> CREATOR = new Creator<NetworkCapabilities>() {
        public NetworkCapabilities createFromParcel(Parcel in) {
            NetworkCapabilities netCap = new NetworkCapabilities();
            netCap.mNetworkCapabilities = in.readLong();
            netCap.mTransportTypes = in.readLong();
            netCap.mLinkUpBandwidthKbps = in.readInt();
            netCap.mLinkDownBandwidthKbps = in.readInt();
            netCap.mNetworkSpecifier = (NetworkSpecifier) in.readParcelable(null);
            netCap.mSignalStrength = in.readInt();
            return netCap;
        }

        public NetworkCapabilities[] newArray(int size) {
            return new NetworkCapabilities[size];
        }
    };
    private static final long DEFAULT_CAPABILITIES = 57344;
    private static final int MAX_NET_CAPABILITY = 18;
    public static final int MAX_TRANSPORT = 6;
    private static final int MIN_NET_CAPABILITY = 0;
    public static final int MIN_TRANSPORT = 0;
    private static final long MUTABLE_CAPABILITIES = 475136;
    public static final int NET_CAPABILITY_CAPTIVE_PORTAL = 17;
    public static final int NET_CAPABILITY_CBS = 5;
    public static final int NET_CAPABILITY_DUN = 2;
    public static final int NET_CAPABILITY_EIMS = 10;
    public static final int NET_CAPABILITY_FOREGROUND = 18;
    public static final int NET_CAPABILITY_FOTA = 3;
    public static final int NET_CAPABILITY_IA = 7;
    public static final int NET_CAPABILITY_IMS = 4;
    public static final int NET_CAPABILITY_INTERNET = 12;
    public static final int NET_CAPABILITY_MMS = 0;
    public static final int NET_CAPABILITY_NOT_METERED = 11;
    public static final int NET_CAPABILITY_NOT_RESTRICTED = 13;
    public static final int NET_CAPABILITY_NOT_VPN = 15;
    public static final int NET_CAPABILITY_RCS = 8;
    public static final int NET_CAPABILITY_SUPL = 1;
    public static final int NET_CAPABILITY_TRUSTED = 14;
    public static final int NET_CAPABILITY_VALIDATED = 16;
    public static final int NET_CAPABILITY_WIFI_P2P = 6;
    public static final int NET_CAPABILITY_XCAP = 9;
    private static final long NON_REQUESTABLE_CAPABILITIES = 458752;
    static final long RESTRICTED_CAPABILITIES = 1980;
    public static final int SIGNAL_STRENGTH_UNSPECIFIED = Integer.MIN_VALUE;
    private static final String TAG = "NetworkCapabilities";
    public static final int TRANSPORT_BLUETOOTH = 2;
    public static final int TRANSPORT_CELLULAR = 0;
    public static final int TRANSPORT_ETHERNET = 3;
    public static final int TRANSPORT_LOWPAN = 6;
    private static final String[] TRANSPORT_NAMES = new String[]{"CELLULAR", "WIFI", "BLUETOOTH", "ETHERNET", "VPN", "WIFI_AWARE", "LOWPAN"};
    public static final int TRANSPORT_VPN = 4;
    public static final int TRANSPORT_WIFI = 1;
    public static final int TRANSPORT_WIFI_AWARE = 5;
    static final long UNRESTRICTED_CAPABILITIES = 4163;
    private int mLinkDownBandwidthKbps;
    private int mLinkUpBandwidthKbps;
    private long mNetworkCapabilities;
    private NetworkSpecifier mNetworkSpecifier = null;
    private int mSignalStrength;
    private long mTransportTypes;

    public NetworkCapabilities() {
        clearAll();
        this.mNetworkCapabilities = DEFAULT_CAPABILITIES;
    }

    public NetworkCapabilities(NetworkCapabilities nc) {
        if (nc != null) {
            this.mNetworkCapabilities = nc.mNetworkCapabilities;
            this.mTransportTypes = nc.mTransportTypes;
            this.mLinkUpBandwidthKbps = nc.mLinkUpBandwidthKbps;
            this.mLinkDownBandwidthKbps = nc.mLinkDownBandwidthKbps;
            this.mNetworkSpecifier = nc.mNetworkSpecifier;
            this.mSignalStrength = nc.mSignalStrength;
        }
    }

    public void clearAll() {
        this.mTransportTypes = 0;
        this.mNetworkCapabilities = 0;
        this.mLinkDownBandwidthKbps = 0;
        this.mLinkUpBandwidthKbps = 0;
        this.mNetworkSpecifier = null;
        this.mSignalStrength = Integer.MIN_VALUE;
    }

    public NetworkCapabilities addCapability(int capability) {
        if (capability < 0 || capability > 18) {
            throw new IllegalArgumentException("NetworkCapability out of range");
        }
        this.mNetworkCapabilities |= (long) (1 << capability);
        return this;
    }

    public NetworkCapabilities removeCapability(int capability) {
        if (capability < 0 || capability > 18) {
            throw new IllegalArgumentException("NetworkCapability out of range");
        }
        this.mNetworkCapabilities &= (long) (~(1 << capability));
        return this;
    }

    public int[] getCapabilities() {
        return BitUtils.unpackBits(this.mNetworkCapabilities);
    }

    public boolean hasCapability(int capability) {
        boolean z = true;
        if (capability < 0 || capability > 18) {
            return false;
        }
        if ((this.mNetworkCapabilities & ((long) (1 << capability))) == 0) {
            z = false;
        }
        return z;
    }

    private void combineNetCapabilities(NetworkCapabilities nc) {
        this.mNetworkCapabilities |= nc.mNetworkCapabilities;
    }

    public String describeFirstNonRequestableCapability() {
        if (hasCapability(16)) {
            return "NET_CAPABILITY_VALIDATED";
        }
        if (hasCapability(17)) {
            return "NET_CAPABILITY_CAPTIVE_PORTAL";
        }
        if (hasCapability(18)) {
            return "NET_CAPABILITY_FOREGROUND";
        }
        if ((this.mNetworkCapabilities & NON_REQUESTABLE_CAPABILITIES) != 0) {
            return "unknown non-requestable capabilities " + Long.toHexString(this.mNetworkCapabilities);
        }
        if (this.mLinkUpBandwidthKbps != 0 || this.mLinkDownBandwidthKbps != 0) {
            return "link bandwidth";
        }
        if (hasSignalStrength()) {
            return "signalStrength";
        }
        return null;
    }

    private boolean satisfiedByNetCapabilities(NetworkCapabilities nc, boolean onlyImmutable) {
        long networkCapabilities = this.mNetworkCapabilities;
        if (onlyImmutable) {
            networkCapabilities &= -475137;
        }
        return (nc.mNetworkCapabilities & networkCapabilities) == networkCapabilities;
    }

    public boolean equalsNetCapabilities(NetworkCapabilities nc) {
        return nc.mNetworkCapabilities == this.mNetworkCapabilities;
    }

    private boolean equalsNetCapabilitiesRequestable(NetworkCapabilities that) {
        return (this.mNetworkCapabilities & -458753) == (that.mNetworkCapabilities & -458753);
    }

    public void maybeMarkCapabilitiesRestricted() {
        boolean hasUnrestrictedCapabilities = (this.mNetworkCapabilities & UNRESTRICTED_CAPABILITIES) != 0;
        if (((this.mNetworkCapabilities & RESTRICTED_CAPABILITIES) != 0) && (hasUnrestrictedCapabilities ^ 1) != 0) {
            removeCapability(13);
        }
    }

    public static boolean isValidTransport(int transportType) {
        return transportType >= 0 && transportType <= 6;
    }

    public NetworkCapabilities addTransportType(int transportType) {
        checkValidTransportType(transportType);
        this.mTransportTypes |= (long) (1 << transportType);
        setNetworkSpecifier(this.mNetworkSpecifier);
        return this;
    }

    public NetworkCapabilities removeTransportType(int transportType) {
        checkValidTransportType(transportType);
        this.mTransportTypes &= (long) (~(1 << transportType));
        setNetworkSpecifier(this.mNetworkSpecifier);
        return this;
    }

    public int[] getTransportTypes() {
        return BitUtils.unpackBits(this.mTransportTypes);
    }

    public boolean hasTransport(int transportType) {
        return isValidTransport(transportType) && (this.mTransportTypes & ((long) (1 << transportType))) != 0;
    }

    private void combineTransportTypes(NetworkCapabilities nc) {
        this.mTransportTypes |= nc.mTransportTypes;
    }

    private boolean satisfiedByTransportTypes(NetworkCapabilities nc) {
        if (this.mTransportTypes == 0 || (this.mTransportTypes & nc.mTransportTypes) != 0) {
            return true;
        }
        return false;
    }

    public boolean equalsTransportTypes(NetworkCapabilities nc) {
        return nc.mTransportTypes == this.mTransportTypes;
    }

    public void setLinkUpstreamBandwidthKbps(int upKbps) {
        this.mLinkUpBandwidthKbps = upKbps;
    }

    public int getLinkUpstreamBandwidthKbps() {
        return this.mLinkUpBandwidthKbps;
    }

    public void setLinkDownstreamBandwidthKbps(int downKbps) {
        this.mLinkDownBandwidthKbps = downKbps;
    }

    public int getLinkDownstreamBandwidthKbps() {
        return this.mLinkDownBandwidthKbps;
    }

    private void combineLinkBandwidths(NetworkCapabilities nc) {
        this.mLinkUpBandwidthKbps = Math.max(this.mLinkUpBandwidthKbps, nc.mLinkUpBandwidthKbps);
        this.mLinkDownBandwidthKbps = Math.max(this.mLinkDownBandwidthKbps, nc.mLinkDownBandwidthKbps);
    }

    private boolean satisfiedByLinkBandwidths(NetworkCapabilities nc) {
        if (this.mLinkUpBandwidthKbps > nc.mLinkUpBandwidthKbps || this.mLinkDownBandwidthKbps > nc.mLinkDownBandwidthKbps) {
            return false;
        }
        return true;
    }

    private boolean equalsLinkBandwidths(NetworkCapabilities nc) {
        if (this.mLinkUpBandwidthKbps == nc.mLinkUpBandwidthKbps && this.mLinkDownBandwidthKbps == nc.mLinkDownBandwidthKbps) {
            return true;
        }
        return false;
    }

    public NetworkCapabilities setNetworkSpecifier(NetworkSpecifier networkSpecifier) {
        if (networkSpecifier == null || Long.bitCount(this.mTransportTypes) == 1) {
            this.mNetworkSpecifier = networkSpecifier;
            return this;
        }
        throw new IllegalStateException("Must have a single transport specified to use setNetworkSpecifier");
    }

    public NetworkSpecifier getNetworkSpecifier() {
        return this.mNetworkSpecifier;
    }

    private void combineSpecifiers(NetworkCapabilities nc) {
        if (this.mNetworkSpecifier == null || (this.mNetworkSpecifier.equals(nc.mNetworkSpecifier) ^ 1) == 0) {
            setNetworkSpecifier(nc.mNetworkSpecifier);
            return;
        }
        throw new IllegalStateException("Can't combine two networkSpecifiers");
    }

    private boolean satisfiedBySpecifier(NetworkCapabilities nc) {
        if (this.mNetworkSpecifier == null || this.mNetworkSpecifier.satisfiedBy(nc.mNetworkSpecifier)) {
            return true;
        }
        return nc.mNetworkSpecifier instanceof MatchAllNetworkSpecifier;
    }

    private boolean equalsSpecifier(NetworkCapabilities nc) {
        return Objects.equals(this.mNetworkSpecifier, nc.mNetworkSpecifier);
    }

    public void setSignalStrength(int signalStrength) {
        this.mSignalStrength = signalStrength;
    }

    public boolean hasSignalStrength() {
        return this.mSignalStrength > Integer.MIN_VALUE;
    }

    public int getSignalStrength() {
        return this.mSignalStrength;
    }

    private void combineSignalStrength(NetworkCapabilities nc) {
        this.mSignalStrength = Math.max(this.mSignalStrength, nc.mSignalStrength);
    }

    private boolean satisfiedBySignalStrength(NetworkCapabilities nc) {
        return this.mSignalStrength <= nc.mSignalStrength;
    }

    private boolean equalsSignalStrength(NetworkCapabilities nc) {
        return this.mSignalStrength == nc.mSignalStrength;
    }

    public void combineCapabilities(NetworkCapabilities nc) {
        combineNetCapabilities(nc);
        combineTransportTypes(nc);
        combineLinkBandwidths(nc);
        combineSpecifiers(nc);
        combineSignalStrength(nc);
    }

    private boolean satisfiedByNetworkCapabilities(NetworkCapabilities nc, boolean onlyImmutable) {
        if (nc == null || !satisfiedByNetCapabilities(nc, onlyImmutable) || !satisfiedByTransportTypes(nc) || ((!onlyImmutable && !satisfiedByLinkBandwidths(nc)) || !satisfiedBySpecifier(nc))) {
            return false;
        }
        if (onlyImmutable) {
            return true;
        }
        return satisfiedBySignalStrength(nc);
    }

    public boolean satisfiedByNetworkCapabilities(NetworkCapabilities nc) {
        return satisfiedByNetworkCapabilities(nc, false);
    }

    public boolean satisfiedByImmutableNetworkCapabilities(NetworkCapabilities nc) {
        return satisfiedByNetworkCapabilities(nc, true);
    }

    public String describeImmutableDifferences(NetworkCapabilities that) {
        if (that == null) {
            return "other NetworkCapabilities was null";
        }
        String before;
        String after;
        StringJoiner joiner = new StringJoiner(", ");
        long oldImmutableCapabilities = this.mNetworkCapabilities & -477189;
        long newImmutableCapabilities = that.mNetworkCapabilities & -477189;
        if (oldImmutableCapabilities != newImmutableCapabilities) {
            before = capabilityNamesOf(BitUtils.unpackBits(oldImmutableCapabilities));
            after = capabilityNamesOf(BitUtils.unpackBits(newImmutableCapabilities));
            joiner.add(String.format("immutable capabilities changed: %s -> %s", new Object[]{before, after}));
        }
        if (!equalsSpecifier(that)) {
            NetworkSpecifier before2 = getNetworkSpecifier();
            NetworkSpecifier after2 = that.getNetworkSpecifier();
            joiner.add(String.format("specifier changed: %s -> %s", new Object[]{before2, after2}));
        }
        if (!equalsTransportTypes(that)) {
            before = transportNamesOf(getTransportTypes());
            after = transportNamesOf(that.getTransportTypes());
            joiner.add(String.format("transports changed: %s -> %s", new Object[]{before, after}));
        }
        return joiner.toString();
    }

    public boolean equalRequestableCapabilities(NetworkCapabilities nc) {
        boolean z = false;
        if (nc == null) {
            return false;
        }
        if (equalsNetCapabilitiesRequestable(nc) && equalsTransportTypes(nc)) {
            z = equalsSpecifier(nc);
        }
        return z;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null || !(obj instanceof NetworkCapabilities)) {
            return false;
        }
        NetworkCapabilities that = (NetworkCapabilities) obj;
        if (equalsNetCapabilities(that) && equalsTransportTypes(that) && equalsLinkBandwidths(that) && equalsSignalStrength(that)) {
            z = equalsSpecifier(that);
        }
        return z;
    }

    public int hashCode() {
        return ((((((((int) (this.mNetworkCapabilities & -1)) + (((int) (this.mNetworkCapabilities >> 32)) * 3)) + (((int) (this.mTransportTypes & -1)) * 5)) + (((int) (this.mTransportTypes >> 32)) * 7)) + (this.mLinkUpBandwidthKbps * 11)) + (this.mLinkDownBandwidthKbps * 13)) + (Objects.hashCode(this.mNetworkSpecifier) * 17)) + (this.mSignalStrength * 19);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mNetworkCapabilities);
        dest.writeLong(this.mTransportTypes);
        dest.writeInt(this.mLinkUpBandwidthKbps);
        dest.writeInt(this.mLinkDownBandwidthKbps);
        dest.writeParcelable((Parcelable) this.mNetworkSpecifier, flags);
        dest.writeInt(this.mSignalStrength);
    }

    public String toString() {
        String upBand;
        String dnBand;
        int[] types = getTransportTypes();
        String transports = types.length > 0 ? " Transports: " + transportNamesOf(types) : "";
        types = getCapabilities();
        String capabilities = types.length > 0 ? " Capabilities: " : "";
        int i = 0;
        while (i < types.length) {
            capabilities = capabilities + capabilityNameOf(types[i]);
            i++;
            if (i < types.length) {
                capabilities = capabilities + "&";
            }
        }
        if (this.mLinkUpBandwidthKbps > 0) {
            upBand = " LinkUpBandwidth>=" + this.mLinkUpBandwidthKbps + "Kbps";
        } else {
            upBand = "";
        }
        if (this.mLinkDownBandwidthKbps > 0) {
            dnBand = " LinkDnBandwidth>=" + this.mLinkDownBandwidthKbps + "Kbps";
        } else {
            dnBand = "";
        }
        return "[" + transports + capabilities + upBand + dnBand + (this.mNetworkSpecifier == null ? "" : " Specifier: <" + this.mNetworkSpecifier + ">") + (hasSignalStrength() ? " SignalStrength: " + this.mSignalStrength : "") + "]";
    }

    public static String capabilityNamesOf(int[] capabilities) {
        StringJoiner joiner = new StringJoiner("|");
        if (capabilities != null) {
            for (int c : capabilities) {
                joiner.add(capabilityNameOf(c));
            }
        }
        return joiner.toString();
    }

    public static String capabilityNameOf(int capability) {
        switch (capability) {
            case 0:
                return "MMS";
            case 1:
                return "SUPL";
            case 2:
                return "DUN";
            case 3:
                return "FOTA";
            case 4:
                return "IMS";
            case 5:
                return "CBS";
            case 6:
                return "WIFI_P2P";
            case 7:
                return "IA";
            case 8:
                return "RCS";
            case 9:
                return "XCAP";
            case 10:
                return "EIMS";
            case 11:
                return "NOT_METERED";
            case 12:
                return "INTERNET";
            case 13:
                return "NOT_RESTRICTED";
            case 14:
                return "TRUSTED";
            case 15:
                return "NOT_VPN";
            case 16:
                return "VALIDATED";
            case 17:
                return "CAPTIVE_PORTAL";
            case 18:
                return "FOREGROUND";
            default:
                return Integer.toString(capability);
        }
    }

    public static String transportNamesOf(int[] types) {
        StringJoiner joiner = new StringJoiner("|");
        if (types != null) {
            for (int t : types) {
                joiner.add(transportNameOf(t));
            }
        }
        return joiner.toString();
    }

    public static String transportNameOf(int transport) {
        if (isValidTransport(transport)) {
            return TRANSPORT_NAMES[transport];
        }
        return "UNKNOWN";
    }

    private static void checkValidTransportType(int transport) {
        Preconditions.checkArgument(isValidTransport(transport), "Invalid TransportType " + transport);
    }
}
