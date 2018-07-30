package com.android.server.engineer;

public class OppoEngineerNative {
    private static final String TAG = "OppoEngineerNative";

    public static native byte[] native_getBackCoverColorId();

    public static native byte[] native_getBadBatteryConfig(int i, int i2);

    public static native byte[] native_getBootImgWaterMark();

    public static native byte[] native_getCalibrationStatusFromNvram();

    public static native byte[] native_getCarrierVersion();

    public static native byte[] native_getCarrierVersionFromNvram();

    public static native byte[] native_getDownloadStatus();

    public static native byte[] native_getEmmcHealthInfo();

    public static native byte[] native_getEncryptImeiFromNvram();

    public static native byte[] native_getEngResultFromNvram();

    public static native byte[] native_getOppoProductInfoFromNvram();

    public static native boolean native_getPartionWriteProtectState();

    public static native byte[] native_getProductLineTestResult();

    public static native byte[] native_getRegionNetlockStatus();

    public static native byte[] native_getSerialNoFromNvram();

    public static native boolean native_getSerialPortState();

    public static native byte[] native_getSimOperatorSwitchStatus();

    public static native byte[] native_getSingleDoubleCardStatus();

    public static native byte[] native_getTelcelSimlockStatus();

    public static native byte[] native_getTelcelSimlockUnlockTimes();

    public static native boolean native_get_rpmb_enable_state();

    public static native boolean native_get_rpmb_state();

    public static native boolean native_resetBackCoverColorId();

    public static native boolean native_resetProductLineTestResult();

    public static native boolean native_saveCarrierVersionToNvram(byte[] bArr);

    public static native boolean native_saveEngResulToNvram(byte[] bArr);

    public static native boolean native_saveSerialNoToNvram(String str);

    public static native boolean native_setBackCoverColorId(String str);

    public static native int native_setBatteryBatteryConfig(int i, int i2, byte[] bArr);

    public static native boolean native_setCarrierVersion(String str);

    public static native boolean native_setPartionWriteProtectState(int i);

    public static native boolean native_setProductLineTestResult(int i, int i2);

    public static native boolean native_setRegionNetlock(String str);

    public static native boolean native_setSerialPortState(int i);

    public static native boolean native_setSimOperatorSwitch(String str);

    public static native boolean native_setSingleDoubleCard(String str);

    public static native boolean native_setTelcelSimlock(String str);

    public static native boolean native_setTelcelSimlockUnlockTimes(String str);

    private OppoEngineerNative() {
    }
}
