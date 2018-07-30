package android.hardware.location;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import java.util.Arrays;

public class ContextHubInfo {
    public static final Creator<ContextHubInfo> CREATOR = new Creator<ContextHubInfo>() {
        public ContextHubInfo createFromParcel(Parcel in) {
            return new ContextHubInfo(in, null);
        }

        public ContextHubInfo[] newArray(int size) {
            return new ContextHubInfo[size];
        }
    };
    private int mId;
    private int mMaxPacketLengthBytes;
    private MemoryRegion[] mMemoryRegions;
    private String mName;
    private float mPeakMips;
    private float mPeakPowerDrawMw;
    private int mPlatformVersion;
    private float mSleepPowerDrawMw;
    private int mStaticSwVersion;
    private float mStoppedPowerDrawMw;
    private int[] mSupportedSensors;
    private String mToolchain;
    private int mToolchainVersion;
    private String mVendor;

    /* synthetic */ ContextHubInfo(Parcel in, ContextHubInfo -this1) {
        this(in);
    }

    public int getMaxPacketLengthBytes() {
        return this.mMaxPacketLengthBytes;
    }

    public void setMaxPacketLenBytes(int bytes) {
        this.mMaxPacketLengthBytes = bytes;
    }

    public int getId() {
        return this.mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public String getName() {
        return this.mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getVendor() {
        return this.mVendor;
    }

    public void setVendor(String vendor) {
        this.mVendor = vendor;
    }

    public String getToolchain() {
        return this.mToolchain;
    }

    public void setToolchain(String toolchain) {
        this.mToolchain = toolchain;
    }

    public int getPlatformVersion() {
        return this.mPlatformVersion;
    }

    public void setPlatformVersion(int platformVersion) {
        this.mPlatformVersion = platformVersion;
    }

    public int getStaticSwVersion() {
        return this.mStaticSwVersion;
    }

    public void setStaticSwVersion(int staticSwVersion) {
        this.mStaticSwVersion = staticSwVersion;
    }

    public int getToolchainVersion() {
        return this.mToolchainVersion;
    }

    public void setToolchainVersion(int toolchainVersion) {
        this.mToolchainVersion = toolchainVersion;
    }

    public float getPeakMips() {
        return this.mPeakMips;
    }

    public void setPeakMips(float peakMips) {
        this.mPeakMips = peakMips;
    }

    public float getStoppedPowerDrawMw() {
        return this.mStoppedPowerDrawMw;
    }

    public void setStoppedPowerDrawMw(float stoppedPowerDrawMw) {
        this.mStoppedPowerDrawMw = stoppedPowerDrawMw;
    }

    public float getSleepPowerDrawMw() {
        return this.mSleepPowerDrawMw;
    }

    public void setSleepPowerDrawMw(float sleepPowerDrawMw) {
        this.mSleepPowerDrawMw = sleepPowerDrawMw;
    }

    public float getPeakPowerDrawMw() {
        return this.mPeakPowerDrawMw;
    }

    public void setPeakPowerDrawMw(float peakPowerDrawMw) {
        this.mPeakPowerDrawMw = peakPowerDrawMw;
    }

    public int[] getSupportedSensors() {
        return Arrays.copyOf(this.mSupportedSensors, this.mSupportedSensors.length);
    }

    public MemoryRegion[] getMemoryRegions() {
        return (MemoryRegion[]) Arrays.copyOf(this.mMemoryRegions, this.mMemoryRegions.length);
    }

    public void setSupportedSensors(int[] supportedSensors) {
        this.mSupportedSensors = Arrays.copyOf(supportedSensors, supportedSensors.length);
    }

    public void setMemoryRegions(MemoryRegion[] memoryRegions) {
        this.mMemoryRegions = (MemoryRegion[]) Arrays.copyOf(memoryRegions, memoryRegions.length);
    }

    public String toString() {
        return ((((((((((("" + "Id : " + this.mId) + ", Name : " + this.mName) + "\n\tVendor : " + this.mVendor) + ", ToolChain : " + this.mToolchain) + "\n\tPlatformVersion : " + this.mPlatformVersion) + ", StaticSwVersion : " + this.mStaticSwVersion) + "\n\tPeakMips : " + this.mPeakMips) + ", StoppedPowerDraw : " + this.mStoppedPowerDrawMw + " mW") + ", PeakPowerDraw : " + this.mPeakPowerDrawMw + " mW") + ", MaxPacketLength : " + this.mMaxPacketLengthBytes + " Bytes") + "\n\tSupported sensors : " + Arrays.toString(this.mSupportedSensors)) + "\n\tMemory Regions : " + Arrays.toString(this.mMemoryRegions);
    }

    private ContextHubInfo(Parcel in) {
        this.mId = in.readInt();
        this.mName = in.readString();
        this.mVendor = in.readString();
        this.mToolchain = in.readString();
        this.mPlatformVersion = in.readInt();
        this.mToolchainVersion = in.readInt();
        this.mStaticSwVersion = in.readInt();
        this.mPeakMips = in.readFloat();
        this.mStoppedPowerDrawMw = in.readFloat();
        this.mSleepPowerDrawMw = in.readFloat();
        this.mPeakPowerDrawMw = in.readFloat();
        this.mMaxPacketLengthBytes = in.readInt();
        this.mSupportedSensors = new int[in.readInt()];
        in.readIntArray(this.mSupportedSensors);
        this.mMemoryRegions = (MemoryRegion[]) in.createTypedArray(MemoryRegion.CREATOR);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mId);
        out.writeString(this.mName);
        out.writeString(this.mVendor);
        out.writeString(this.mToolchain);
        out.writeInt(this.mPlatformVersion);
        out.writeInt(this.mToolchainVersion);
        out.writeInt(this.mStaticSwVersion);
        out.writeFloat(this.mPeakMips);
        out.writeFloat(this.mStoppedPowerDrawMw);
        out.writeFloat(this.mSleepPowerDrawMw);
        out.writeFloat(this.mPeakPowerDrawMw);
        out.writeInt(this.mMaxPacketLengthBytes);
        out.writeInt(this.mSupportedSensors.length);
        out.writeIntArray(this.mSupportedSensors);
        out.writeTypedArray(this.mMemoryRegions, flags);
    }
}
