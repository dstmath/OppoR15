package android.engineer;

import android.engineer.IOppoEngineerManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class OppoEngineerManager {
    private static final String ENGINEER_SERVICE_NAME = "engineer";
    private static final String TAG = "OppoEngineerManager";
    private static OppoEngineerManager sInstance = null;
    private static IOppoEngineerManager sService = null;

    private OppoEngineerManager() {
    }

    private static boolean init() {
        if (sService == null) {
            sService = Stub.asInterface(ServiceManager.getService("engineer"));
        }
        if (sService != null) {
            return true;
        }
        Log.e(TAG, "init fail can't find engineer service");
        return false;
    }

    public static void turnButtonLightOn(int brightness) {
        if (init()) {
            try {
                sService.turnButtonLightOn(brightness);
            } catch (RemoteException e) {
            }
        }
    }

    public static void turnButtonLightOff() {
        if (init()) {
            try {
                sService.turnButtonLightOff();
            } catch (RemoteException e) {
            }
        }
    }

    public static void turnBreathLightOn(int color) {
        if (init()) {
            try {
                sService.turnBreathLightOn(color);
            } catch (RemoteException e) {
            }
        }
    }

    public static void turnBreathLightFlashOn(int color) {
        if (init()) {
            try {
                sService.turnBreathLightFlashOn(color);
            } catch (RemoteException e) {
            }
        }
    }

    public static void turnBreathLightOff() {
        if (init()) {
            try {
                sService.turnBreathLightOff();
            } catch (RemoteException e) {
            }
        }
    }

    public static void setTorchState(String state) {
        if (init()) {
            try {
                sService.setTorchState(state);
            } catch (RemoteException e) {
            }
        }
    }

    public static int cameraHardwareCheck(int camera, int invalid) {
        if (!init()) {
            return invalid;
        }
        try {
            return sService.cameraHardwareCheck(camera, invalid);
        } catch (RemoteException e) {
            return invalid;
        }
    }

    public static String getDownloadStatus() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getDownloadStatus();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean isSerialPortEnabled() {
        if (!init()) {
            return false;
        }
        try {
            return sService.isSerialPortEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static boolean setSerialPortState(boolean enable) {
        if (!init()) {
            return false;
        }
        try {
            return sService.setSerialPortState(enable);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static byte[] getEmmcHealthInfo() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getEmmcHealthInfo();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean isPartionWriteProtectDisabled() {
        if (!init()) {
            return false;
        }
        try {
            return sService.isPartionWriteProtectDisabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static boolean disablePartionWriteProtect(boolean disable) {
        if (!init()) {
            return false;
        }
        try {
            return sService.disablePartionWriteProtect(disable);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static String getBackCoverColorId() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getBackCoverColorId();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean setBackCoverColorId(String colorId) {
        if (!init()) {
            return false;
        }
        try {
            return sService.setBackCoverColorId(colorId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static boolean getRpmbState() {
        if (!init()) {
            return false;
        }
        try {
            return sService.getRpmbState();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static boolean getRpmbEnableState() {
        if (!init()) {
            return false;
        }
        try {
            return sService.getRpmbEnableState();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static String getCarrierVersion() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getCarrierVersion();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean setCarrierVersion(String version) {
        if (!init()) {
            return false;
        }
        try {
            return sService.setCarrierVersion(version);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static String getRegionNetlockStatus() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getRegionNetlockStatus();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean setRegionNetlock(String lock) {
        if (!init()) {
            return false;
        }
        try {
            return sService.setRegionNetlock(lock);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static String getTelcelSimlockStatus() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getTelcelSimlockStatus();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean setTelcelSimlock(String lock) {
        if (!init()) {
            return false;
        }
        try {
            return sService.setTelcelSimlock(lock);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static String getTelcelSimlockUnlockTimes() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getTelcelSimlockUnlockTimes();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean setTelcelSimlockUnlockTimes(String times) {
        if (!init()) {
            return false;
        }
        try {
            return sService.setTelcelSimlockUnlockTimes(times);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static String getSingleDoubleCardStatus() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getSingleDoubleCardStatus();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean setSingleDoubleCard(String state) {
        if (!init()) {
            return false;
        }
        try {
            return sService.setSingleDoubleCard(state);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static byte[] getBadBatteryConfig(int offset, int size) {
        if (!init()) {
            return null;
        }
        try {
            return sService.getBadBatteryConfig(offset, size);
        } catch (RemoteException e) {
            return null;
        }
    }

    public static int setBatteryBatteryConfig(int offset, int size, byte[] data) {
        if (!init()) {
            return -1;
        }
        try {
            return sService.setBatteryBatteryConfig(offset, size, data);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static byte[] getProductLineTestResult() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getProductLineTestResult();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean setProductLineTestResult(int position, int result) {
        if (!init()) {
            return false;
        }
        try {
            return sService.setProductLineTestResult(position, result);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static boolean resetProductLineTestResult() {
        if (!init()) {
            return false;
        }
        try {
            return sService.resetProductLineTestResult();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static byte[] getEngResultFromNvram() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getEngResultFromNvram();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean saveEngResultToNvram(byte[] result) {
        if (!init()) {
            return false;
        }
        try {
            return sService.saveEngResultToNvram(result);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static byte[] getEncryptImeiFromNvram() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getEncryptImeiFromNvram();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static byte[] getCarrierVersionFromNvram() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getCarrierVersionFromNvram();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean saveCarrierVersionToNvram(byte[] version) {
        if (!init()) {
            return false;
        }
        try {
            return sService.saveCarrierVersionToNvram(version);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static byte[] getCalibrationStatusFromNvram() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getCalibrationStatusFromNvram();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static String getSimOperatorSwitchStatus() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getSimOperatorSwitchStatus();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean setSimOperatorSwitch(String state) {
        if (!init()) {
            return false;
        }
        try {
            return sService.setSimOperatorSwitch(state);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static String getBootImgWaterMark() {
        if (!init()) {
            return null;
        }
        try {
            return sService.getBootImgWaterMark();
        } catch (RemoteException e) {
            return null;
        }
    }
}
