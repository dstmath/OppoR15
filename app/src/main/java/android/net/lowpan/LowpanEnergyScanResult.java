package android.net.lowpan;

public class LowpanEnergyScanResult {
    public static final int UNKNOWN = Integer.MAX_VALUE;
    private int mChannel = Integer.MAX_VALUE;
    private int mMaxRssi = Integer.MAX_VALUE;

    LowpanEnergyScanResult() {
    }

    public int getChannel() {
        return this.mChannel;
    }

    public int getMaxRssi() {
        return this.mMaxRssi;
    }

    void setChannel(int x) {
        this.mChannel = x;
    }

    void setMaxRssi(int x) {
        this.mMaxRssi = x;
    }

    public String toString() {
        return "LowpanEnergyScanResult(channel: " + this.mChannel + ", maxRssi:" + this.mMaxRssi + ")";
    }
}
