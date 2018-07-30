package com.android.server.engineer;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.engineer.IOppoEngineerManager.Stub;
import android.net.Uri;
import android.os.Binder;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.am.OppoAppStartupManager;
import com.android.server.content.SyncStorageEngine;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class OppoEngineerService extends SystemService {
    private static final String ATM_ATCI_AUTOSTART_MODE_PROPERTY = "persist.service.atci.autostart";
    private static final String ATM_ATCI_SERVICE_NAME = "com.mediatek.atci.service.AtciService";
    private static final String ATM_ATCI_SERVICE_PACKAGE = "com.mediatek.atci.service";
    private static final String ATM_ATCI_USERMODE_MODE_PROPERTY = "persist.service.atci.usermode";
    private static final String ATM_MODEM_MODE_NORMAL = "normal";
    private static final String ATM_MODEM_MODE_PROPERTY = "persist.atm.mdmode";
    private static final String BACK_COVER_COLOR_ID_REG_STRING = "[a-f0-9A-F]{8}";
    private static final String BATTERY_HW_STATUS_NODE = "/sys/class/power_supply/battery/short_c_hw_status";
    private static final String BROADCAST_ACTION_ROM_UPDATE_CONFIG_SUCCES = "oppo.intent.action.ROM_UPDATE_CONFIG_SUCCESS";
    private static final String BUILD_RELEASE_TYPE_PROPERTY = "ro.build.release_type";
    private static final String COLUMN_NAME_1 = "version";
    private static final String COLUMN_NAME_2 = "xml";
    private static final Uri CONTENT_URI_WHITE_LIST = Uri.parse("content://com.nearme.romupdate.provider.db/update_list");
    private static final String CRITICAL_LOG_CONFIG_FILTER_NAME = "criticallog_config";
    private static final String ENGINEERMODE_PACKAGE_NAME = "com.oppo.engineermode";
    private static final int HANDLE_ROM_UPDATE_MSG = 1000001;
    private static final String MASTER_CLEAR_CLASS_NAME = "com.oppo.engineermode.manualtest.MasterClear";
    private static final String META_TST_SERVICE = "meta_tst";
    private static final String MMI_SERVER_CLASS_NAME = "com.oppo.autotest.connector.AutoTestServer";
    private static final String MMI_SERVER_START_BROADCAST_ACTION = "oppo.intent.action.START_OPPO_AT_SERVER";
    private static final String MMI_SERVER_STOP_BROADCAST_ACTION = "oppo.intent.action.STOP_OPPO_AT_SERVER";
    private static final int NVRAM_FACTORY_NUMBER_INFO_LENGTH = 16;
    private static final int NVRAM_OPPO_PRODUCT_INFO_LENGTH = 128;
    private static final String OPPO_COMPONENT_SAFE_PERMISSION = "oppo.permission.OPPO_COMPONENT_SAFE";
    private static final int PARTION_PROTECT_NOTIFICAITON_ID = 10000;
    private static final String PCBA_PROPERTY = "gsm.serial";
    private static final String PCBA_UNKNOWN_DEFAULT = "UNKNOWN";
    private static final String POWER_OFF_CLASS_NAME = "com.oppo.engineermode.PowerOff";
    private static final String ROM_UPDATE_CONFIG_LIST = "ROM_UPDATE_CONFIG_LIST";
    private static final String SERIAL_NO_PROPERTY = "ro.serialno";
    private static final String TAG = "OppoEngineerService";
    private static final String USB_CHARGE_SWITCH_PROPERTY = "sys.usb.config.meta";
    private static final int WRITE_PROTECT_NEED_RESET_ISSUE_TYPE = 101;
    private static final String WRITE_PROTECT_RESET_CONFIG = "WriteProtectReset state=\"true\"";
    private static final int WRITE_PROTECT_RESET_DONE_ISSUE_TYPE = 102;
    private ActivityManagerInternal mActivityManagerInternal;
    private int mBatteryLevel;
    private BinderService mBinderService;
    private Light mBreathLight;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Slog.d(OppoEngineerService.TAG, "onReceive intent = " + intent.toString());
            String action = intent.getAction();
            if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                if (OppoEngineerUtils.isMtkPlatform() && (OppoEngineerService.ATM_MODEM_MODE_NORMAL.equals(SystemProperties.get(OppoEngineerService.ATM_MODEM_MODE_PROPERTY, OppoEngineerService.ATM_MODEM_MODE_NORMAL)) ^ 1) != 0) {
                    Slog.d(OppoEngineerService.TAG, "auto reset mdmode while shutdown");
                    SystemProperties.set(OppoEngineerService.ATM_MODEM_MODE_PROPERTY, OppoEngineerService.ATM_MODEM_MODE_NORMAL);
                }
            } else if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                OppoEngineerService.this.mBatteryLevel = intent.getIntExtra("level", 0);
            }
        }
    };
    private Light mButtonLight;
    private final Context mContext;
    private final OppoEngineerHandler mHandler;
    private LightsManager mLightsManager;
    private final Object mLock = new Object();
    private final ServiceThread mServiceThread;
    private BroadcastReceiver mUpdateBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Slog.d(OppoEngineerService.TAG, "onReceive intent = " + intent);
            if (intent != null) {
                ArrayList<String> tmp = intent.getStringArrayListExtra("ROM_UPDATE_CONFIG_LIST");
                if (tmp != null && tmp.contains("criticallog_config")) {
                    Slog.i(OppoEngineerService.TAG, "need reset wp state");
                    OppoEngineerService.this.mHandler.sendEmptyMessage(OppoEngineerService.HANDLE_ROM_UPDATE_MSG);
                }
            }
        }
    };

    private final class BinderService extends Stub {
        private static final String TORCH_SWITCH_FILE_PATH = "/proc/qcom_flash";

        /* synthetic */ BinderService(OppoEngineerService this$0, BinderService -this1) {
            this();
        }

        private BinderService() {
        }

        public void turnButtonLightOn(int brightness) {
            OppoEngineerService.this.mButtonLight.setBrightness(brightness);
        }

        public void turnButtonLightOff() {
            OppoEngineerService.this.mButtonLight.turnOff();
        }

        public void turnBreathLightOn(int color) {
            OppoEngineerService.this.mBreathLight.setColor(color);
        }

        public void turnBreathLightFlashOn(int color) {
            OppoEngineerService.this.mBreathLight.setFlashing(color, 1, OppoEngineerService.this.mContext.getResources().getInteger(17694768), OppoEngineerService.this.mContext.getResources().getInteger(17694767));
        }

        public void turnBreathLightOff() {
            OppoEngineerService.this.mBreathLight.turnOff();
        }

        public void setTorchState(String state) {
            try {
                FileUtils.stringToFile(TORCH_SWITCH_FILE_PATH, state);
            } catch (IOException e) {
                Slog.i(OppoEngineerService.TAG, "setTorchState state=" + state + " failed : " + e.getMessage());
            }
        }

        public int cameraHardwareCheck(int camera, int invalid) {
            return OppoEngineerUtils.invokeCameraHardwareCheck(camera, invalid);
        }

        public String getDownloadStatus() {
            return OppoEngineerService.this.getDownloadStatusInternal();
        }

        public boolean isSerialPortEnabled() {
            return OppoEngineerNative.native_getSerialPortState();
        }

        public boolean setSerialPortState(boolean enable) {
            return OppoEngineerNative.native_setSerialPortState(enable ? 1 : 0);
        }

        public byte[] getEmmcHealthInfo() {
            return OppoEngineerNative.native_getEmmcHealthInfo();
        }

        public boolean isPartionWriteProtectDisabled() {
            return OppoEngineerNative.native_getPartionWriteProtectState();
        }

        public boolean disablePartionWriteProtect(boolean disable) {
            return OppoEngineerNative.native_setPartionWriteProtectState(disable ? 1 : 0);
        }

        public String getBackCoverColorId() {
            return OppoEngineerService.this.getBackCoverColorIdInternal();
        }

        public boolean setBackCoverColorId(String colorId) {
            if (OppoEngineerService.this.isBackCoverColorIdValid(colorId)) {
                return OppoEngineerNative.native_setBackCoverColorId(colorId);
            }
            return false;
        }

        public boolean getRpmbState() {
            return OppoEngineerNative.native_get_rpmb_state();
        }

        public boolean getRpmbEnableState() {
            return OppoEngineerNative.native_get_rpmb_enable_state();
        }

        public String getCarrierVersion() {
            byte[] version = OppoEngineerNative.native_getCarrierVersion();
            if (version == null || version.length <= 0) {
                return null;
            }
            return new String(version, StandardCharsets.UTF_8);
        }

        public boolean setCarrierVersion(String version) {
            return OppoEngineerNative.native_setCarrierVersion(version);
        }

        public String getRegionNetlockStatus() {
            byte[] netLock = OppoEngineerNative.native_getRegionNetlockStatus();
            if (netLock == null || netLock.length <= 0) {
                return null;
            }
            return new String(netLock, StandardCharsets.UTF_8);
        }

        public boolean setRegionNetlock(String lock) {
            return OppoEngineerNative.native_setRegionNetlock(lock);
        }

        public String getTelcelSimlockStatus() {
            byte[] simLock = OppoEngineerNative.native_getTelcelSimlockStatus();
            if (simLock == null || simLock.length <= 0) {
                return null;
            }
            return new String(simLock, StandardCharsets.UTF_8);
        }

        public boolean setTelcelSimlock(String lock) {
            return OppoEngineerNative.native_setTelcelSimlock(lock);
        }

        public String getTelcelSimlockUnlockTimes() {
            byte[] unlockTimes = OppoEngineerNative.native_getTelcelSimlockUnlockTimes();
            if (unlockTimes == null || unlockTimes.length <= 0) {
                return null;
            }
            return new String(unlockTimes, StandardCharsets.UTF_8);
        }

        public boolean setTelcelSimlockUnlockTimes(String times) {
            return OppoEngineerNative.native_setTelcelSimlockUnlockTimes(times);
        }

        public String getSingleDoubleCardStatus() {
            byte[] cardStatus = OppoEngineerNative.native_getSingleDoubleCardStatus();
            if (cardStatus == null || cardStatus.length <= 0) {
                return null;
            }
            return new String(cardStatus, StandardCharsets.UTF_8);
        }

        public boolean setSingleDoubleCard(String state) {
            return OppoEngineerNative.native_setSingleDoubleCard(state);
        }

        public byte[] getBadBatteryConfig(int offset, int size) {
            return OppoEngineerNative.native_getBadBatteryConfig(offset, size);
        }

        public int setBatteryBatteryConfig(int offset, int size, byte[] data) {
            return OppoEngineerNative.native_setBatteryBatteryConfig(offset, size, data);
        }

        public byte[] getProductLineTestResult() {
            return OppoEngineerNative.native_getProductLineTestResult();
        }

        public boolean setProductLineTestResult(int position, int result) {
            return OppoEngineerNative.native_setProductLineTestResult(position, result);
        }

        public boolean resetProductLineTestResult() {
            return OppoEngineerNative.native_resetProductLineTestResult();
        }

        public byte[] getEngResultFromNvram() {
            return OppoEngineerNative.native_getEngResultFromNvram();
        }

        public boolean saveEngResultToNvram(byte[] result) {
            return OppoEngineerNative.native_saveEngResulToNvram(result);
        }

        public byte[] getEncryptImeiFromNvram() {
            return OppoEngineerNative.native_getEncryptImeiFromNvram();
        }

        public byte[] getCarrierVersionFromNvram() {
            return OppoEngineerNative.native_getCarrierVersionFromNvram();
        }

        public boolean saveCarrierVersionToNvram(byte[] version) {
            return OppoEngineerNative.native_saveCarrierVersionToNvram(version);
        }

        public byte[] getCalibrationStatusFromNvram() {
            return OppoEngineerNative.native_getCalibrationStatusFromNvram();
        }

        public String getSimOperatorSwitchStatus() {
            byte[] switchStatus = OppoEngineerNative.native_getSimOperatorSwitchStatus();
            if (switchStatus == null || switchStatus.length <= 0) {
                return null;
            }
            return new String(switchStatus, StandardCharsets.UTF_8);
        }

        public boolean setSimOperatorSwitch(String state) {
            return OppoEngineerNative.native_setSimOperatorSwitch(state);
        }

        public String getBootImgWaterMark() {
            byte[] info = OppoEngineerNative.native_getBootImgWaterMark();
            if (info == null || info.length <= 0) {
                return null;
            }
            return new String(info, StandardCharsets.UTF_8);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (Binder.getCallingUid() == 1000 || OppoEngineerService.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") == 0) {
                OppoEngineerService.this.dumpInternal(fd, pw, args);
            } else {
                pw.println("Permission Denial: can't dump engineer from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            }
        }
    }

    private class OppoEngineerHandler extends Handler {
        public OppoEngineerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case OppoEngineerService.HANDLE_ROM_UPDATE_MSG /*1000001*/:
                    String update = OppoEngineerService.this.getDataFromProvider();
                    if (!OppoEngineerUtils.isMtkPlatform() || update == null || !update.contains(OppoEngineerService.WRITE_PROTECT_RESET_CONFIG)) {
                        return;
                    }
                    if (OppoEngineerService.this.resetWriteProtectState()) {
                        Slog.i(OppoEngineerService.TAG, "reset wp state success");
                        if (SystemProperties.getBoolean(OppoEngineerService.USB_CHARGE_SWITCH_PROPERTY, false)) {
                            SystemProperties.set(OppoEngineerService.USB_CHARGE_SWITCH_PROPERTY, "false");
                        }
                        OppoEngineerUtils.writeLogToPartition(102, "WriteProtectResetDone", "ANDROID", "WriteProtectIssue", "WriteProtectResetDone");
                        OppoEngineerService.this.mContext.unregisterReceiver(OppoEngineerService.this.mUpdateBroadcastReceiver);
                        return;
                    }
                    Slog.i(OppoEngineerService.TAG, "reset wp state fail");
                    OppoEngineerService.this.writeWpIssueToCriticalLog();
                    return;
                default:
                    return;
            }
        }
    }

    private class Shell extends ShellCommand {
        /* synthetic */ Shell(OppoEngineerService this$0, Shell -this1) {
            this();
        }

        private Shell() {
        }

        public int onCommand(String cmd) {
            return OppoEngineerService.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
        }
    }

    public OppoEngineerService(Context context) {
        super(context);
        this.mContext = context;
        this.mServiceThread = new ServiceThread(TAG, -2, true);
        this.mServiceThread.start();
        this.mHandler = new OppoEngineerHandler(this.mServiceThread.getLooper());
        this.mLightsManager = (LightsManager) LocalServices.getService(LightsManager.class);
        this.mButtonLight = this.mLightsManager.getLight(2);
        this.mBreathLight = this.mLightsManager.getLight(4);
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
    }

    public void onStart() {
        Slog.i(TAG, "publishBinderService ENGINEER_SERVICE");
        this.mBinderService = new BinderService();
        publishBinderService("engineer", this.mBinderService);
    }

    public void onBootPhase(int phase) {
        if (phase == 1000) {
            Slog.i(TAG, "on PHASE_BOOT_COMPLETED");
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.ACTION_SHUTDOWN");
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
            if (OppoEngineerUtils.isMtkPlatform() && OppoEngineerNative.native_getPartionWriteProtectState()) {
                IntentFilter ruFilter = new IntentFilter();
                ruFilter.addAction("oppo.intent.action.ROM_UPDATE_CONFIG_SUCCESS");
                this.mContext.registerReceiver(this.mUpdateBroadcastReceiver, ruFilter);
                this.mHandler.post(new Runnable() {
                    public void run() {
                        OppoEngineerService.this.addPartionProtectNotification(OppoEngineerService.this.mContext);
                        if (SystemProperties.getBoolean(OppoEngineerService.BUILD_RELEASE_TYPE_PROPERTY, false) && OppoEngineerUtils.isSecrecyEncryptState(OppoEngineerService.this.mContext)) {
                            OppoEngineerService.this.writeWpIssueToCriticalLog();
                        }
                    }
                });
            }
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (OppoEngineerUtils.isMtkPlatform()) {
                        String serialFromProperty = SystemProperties.get(OppoEngineerService.SERIAL_NO_PROPERTY, "");
                        if (!(serialFromProperty == null || (serialFromProperty.isEmpty() ^ 1) == 0)) {
                            String str = null;
                            byte[] serial = OppoEngineerNative.native_getSerialNoFromNvram();
                            if (serial != null && serial.length > 0) {
                                str = new String(serial, StandardCharsets.UTF_8);
                            }
                            Slog.d(OppoEngineerService.TAG, "getSerialNoFromNvram serialFromNv=" + str);
                            if (str == null || (serialFromProperty.equals(str) ^ 1) != 0) {
                                Slog.d(OppoEngineerService.TAG, "update serial to nvram " + (OppoEngineerNative.native_saveSerialNoToNvram(serialFromProperty) ? SyncStorageEngine.MESG_SUCCESS : "fail"));
                            }
                        }
                        byte[] oppoProductinfo = OppoEngineerNative.native_getOppoProductInfoFromNvram();
                        if (oppoProductinfo == null || oppoProductinfo.length != 128) {
                            Slog.d(OppoEngineerService.TAG, "OPPO_PRODUCT_INFO empty");
                            return;
                        }
                        int numberLength = 0;
                        for (int i = 0; i < 16; i++) {
                            if (oppoProductinfo[i] == (byte) 0) {
                                numberLength = i;
                                break;
                            }
                            if (i == 15) {
                                numberLength = 16;
                            }
                        }
                        if (numberLength > 0) {
                            String factoryNumber = new String(oppoProductinfo, 0, numberLength);
                            Slog.i(OppoEngineerService.TAG, "updateOppoProductInfo factoryNumber:" + factoryNumber);
                            SystemProperties.set("oppo.eng.factory.no", factoryNumber);
                        }
                    }
                }
            });
        }
    }

    private String getBackCoverColorIdInternal() {
        byte[] color = OppoEngineerNative.native_getBackCoverColorId();
        if (color == null || color.length <= 0) {
            return null;
        }
        return new String(color, StandardCharsets.UTF_8);
    }

    private String getDownloadStatusInternal() {
        byte[] status = OppoEngineerNative.native_getDownloadStatus();
        if (status == null || status.length <= 0) {
            return null;
        }
        return new String(status, StandardCharsets.UTF_8);
    }

    private void writeWpIssueToCriticalLog() {
        Slog.i(TAG, "writeWpIssueToCriticalLog");
        String pcba = SystemProperties.get(PCBA_PROPERTY, PCBA_UNKNOWN_DEFAULT);
        StringBuilder criticalLogSb = new StringBuilder();
        criticalLogSb.append("[");
        criticalLogSb.append(pcba);
        criticalLogSb.append("]");
        criticalLogSb.append("[");
        criticalLogSb.append("WP OFF ISSUE");
        criticalLogSb.append("]");
        if (SystemProperties.getBoolean(USB_CHARGE_SWITCH_PROPERTY, false)) {
            criticalLogSb.append("[");
            criticalLogSb.append("USB Charge Disable");
            criticalLogSb.append("]");
        }
        if (!ATM_MODEM_MODE_NORMAL.equals(SystemProperties.get(ATM_MODEM_MODE_PROPERTY, ATM_MODEM_MODE_NORMAL))) {
            criticalLogSb.append("[");
            criticalLogSb.append("MODEM META MODE");
            criticalLogSb.append("]");
        }
        OppoEngineerUtils.writeLogToPartition(101, criticalLogSb.toString(), "ANDROID", "WriteProtectIssue", "WriteProtectNeedReset");
    }

    private boolean resetWriteProtectState() {
        if (!OppoEngineerNative.native_getPartionWriteProtectState()) {
            return true;
        }
        if (!OppoEngineerNative.native_setPartionWriteProtectState(0) || (OppoEngineerNative.native_getPartionWriteProtectState() ^ 1) == 0) {
            return false;
        }
        this.mHandler.post(new Runnable() {
            public void run() {
                if (!OppoEngineerService.ATM_MODEM_MODE_NORMAL.equals(SystemProperties.get(OppoEngineerService.ATM_MODEM_MODE_PROPERTY, OppoEngineerService.ATM_MODEM_MODE_NORMAL))) {
                    Slog.i(OppoEngineerService.TAG, "current md mode is not normal, reset it to normal");
                    SystemProperties.set(OppoEngineerService.ATM_MODEM_MODE_PROPERTY, OppoEngineerService.ATM_MODEM_MODE_NORMAL);
                }
                SystemProperties.set(OppoEngineerService.ATM_ATCI_USERMODE_MODE_PROPERTY, "0");
                SystemProperties.set(OppoEngineerService.ATM_ATCI_AUTOSTART_MODE_PROPERTY, "0");
                try {
                    Intent atciIntent = new Intent();
                    atciIntent.setComponent(new ComponentName(OppoEngineerService.ATM_ATCI_SERVICE_PACKAGE, OppoEngineerService.ATM_ATCI_SERVICE_NAME));
                    if (OppoEngineerService.this.mActivityManagerInternal.isSystemReady()) {
                        if (OppoEngineerService.this.mContext.stopServiceAsUser(atciIntent, UserHandle.CURRENT)) {
                            Slog.d(OppoEngineerService.TAG, "stop atci service done");
                        } else {
                            Slog.d(OppoEngineerService.TAG, "stop atci service fail");
                        }
                        if (!android.os.SystemService.isStopped(OppoEngineerService.META_TST_SERVICE)) {
                            android.os.SystemService.stop(OppoEngineerService.META_TST_SERVICE);
                        }
                        ((NotificationManager) OppoEngineerService.this.mContext.getSystemService("notification")).cancel(10000);
                        OppoEngineerService.this.clearWpIssue();
                    }
                    Slog.d(OppoEngineerService.TAG, "Ams not ready");
                    if (android.os.SystemService.isStopped(OppoEngineerService.META_TST_SERVICE)) {
                        android.os.SystemService.stop(OppoEngineerService.META_TST_SERVICE);
                    }
                    ((NotificationManager) OppoEngineerService.this.mContext.getSystemService("notification")).cancel(10000);
                    OppoEngineerService.this.clearWpIssue();
                } catch (Exception e) {
                    Slog.d(OppoEngineerService.TAG, "stop atci server exception caught : " + e.getMessage());
                }
            }
        });
        return true;
    }

    private void clearWpIssue() {
        OppoEngineerUtils.cleanItem(101);
        OppoEngineerUtils.cleanItem(1125);
        OppoEngineerUtils.syncCacheToEmmc();
    }

    private String getDataFromProvider() {
        Cursor cursor = null;
        String returnStr = null;
        String[] projection = new String[]{"version", COLUMN_NAME_2};
        try {
            if (this.mContext == null) {
                return null;
            }
            cursor = this.mContext.getContentResolver().query(CONTENT_URI_WHITE_LIST, projection, "filtername=\"criticallog_config\"", null, null);
            if (cursor != null && cursor.getCount() > 0) {
                int versioncolumnIndex = cursor.getColumnIndex("version");
                int xmlcolumnIndex = cursor.getColumnIndex(COLUMN_NAME_2);
                cursor.moveToNext();
                int configVersion = cursor.getInt(versioncolumnIndex);
                returnStr = cursor.getString(xmlcolumnIndex);
                Slog.d(TAG, "White List updated, version = " + configVersion);
            }
            if (cursor != null) {
                cursor.close();
            }
            return returnStr;
        } catch (Exception e) {
            Slog.w(TAG, "We can not get white list data from provider, because of " + e);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isBackCoverColorIdValid(String color) {
        if (color == null || color.isEmpty() || !Pattern.matches(BACK_COVER_COLOR_ID_REG_STRING, color)) {
            return false;
        }
        return true;
    }

    private int startActivityAsUserInternal(Intent intent, int userId) {
        try {
            return ActivityManagerNative.getDefault().startActivityAsUser(this.mContext.getIApplicationThread(), this.mContext.getBasePackageName(), intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), null, null, 0, 268435456, null, null, userId);
        } catch (RemoteException e) {
            return -96;
        } catch (Exception e2) {
            return -96;
        }
    }

    private int onShellCommand(Shell shell, String cmd) {
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        String target;
        String colorId;
        String position;
        int targetPosition;
        Intent serverIntent;
        ComponentName componentName;
        ComponentName ret;
        String argument;
        Intent intent;
        if (cmd.equals("-get")) {
            try {
                target = shell.getNextArgRequired();
                if (target.equals("v")) {
                    pw.println("OK:180112");
                } else if (target.equals("wp")) {
                    if (!OppoEngineerUtils.isMtkPlatform()) {
                        return shell.handleDefaultCommands(cmd);
                    }
                    if (OppoEngineerNative.native_getPartionWriteProtectState()) {
                        pw.println("OK:WP OFF");
                    } else {
                        pw.println("OK:WP ON");
                    }
                } else if (target.equals("bcc")) {
                    colorId = getBackCoverColorIdInternal();
                    if (colorId == null) {
                        pw.println("FAIL:Access Fail");
                    } else {
                        pw.println("OK:" + colorId);
                    }
                } else if (target.equals("bl")) {
                    pw.println("OK:" + this.mBatteryLevel);
                } else if (target.equals("ds")) {
                    String downloadStatus = getDownloadStatusInternal();
                    if (downloadStatus == null || !downloadStatus.contains("download over")) {
                        pw.println("FAIL:Download Not Finished");
                    } else {
                        pw.println("OK:Download Over");
                    }
                } else if (target.equals(OppoAppStartupManager.TYPE_BIND_SERVICE)) {
                    if (OppoEngineerUtils.readIntFromFile(BATTERY_HW_STATUS_NODE, 1) == 1) {
                        pw.println("OK:Battery Status Okay");
                    } else {
                        pw.println("FAIL:Battery DET Exception");
                    }
                } else if (target.equals("er")) {
                    position = shell.getNextArg();
                    byte[] result = OppoEngineerNative.native_getProductLineTestResult();
                    if (result == null || result.length != 128) {
                        pw.println("FAIL:Access Fail");
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        targetPosition = -1;
                        if (position != null) {
                            try {
                                targetPosition = Integer.valueOf(position).intValue();
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                targetPosition = -1;
                            }
                            if (targetPosition < 0 || targetPosition >= 128) {
                                targetPosition = -1;
                            }
                        }
                        String sResult;
                        if (targetPosition < 0) {
                            for (byte b : result) {
                                sResult = Integer.toHexString(b & 255);
                                if (sResult.length() < 2) {
                                    stringBuilder.append(0);
                                }
                                stringBuilder.append(sResult);
                            }
                        } else {
                            sResult = Integer.toHexString(result[targetPosition] & 255);
                            if (sResult.length() < 2) {
                                stringBuilder.append(0);
                            }
                            stringBuilder.append(sResult);
                        }
                        pw.println("OK:" + stringBuilder.toString());
                    }
                }
            } catch (IllegalArgumentException e2) {
                return shell.handleDefaultCommands(cmd);
            }
        } else if (cmd.equals("-set")) {
            try {
                target = shell.getNextArgRequired();
                String value;
                if (target.equals("bcc")) {
                    try {
                        value = shell.getNextArgRequired();
                        if (!isBackCoverColorIdValid(value)) {
                            pw.println("FAIL:Invalid Parameter");
                        } else if (OppoEngineerNative.native_setBackCoverColorId(value)) {
                            pw.println("OK");
                        } else {
                            pw.println("FAIL:IO Exception");
                        }
                    } catch (IllegalArgumentException e3) {
                        return shell.handleDefaultCommands(cmd);
                    }
                } else if (target.equals("er")) {
                    try {
                        int targetValue;
                        position = shell.getNextArgRequired();
                        value = shell.getNextArgRequired();
                        try {
                            targetPosition = Integer.valueOf(position).intValue();
                            targetValue = Integer.valueOf(value).intValue();
                        } catch (NumberFormatException e4) {
                            e4.printStackTrace();
                            targetPosition = -1;
                            targetValue = -1;
                        }
                        if (targetPosition < 0 || targetPosition >= 128 || targetValue < 0 || targetValue >= 128) {
                            pw.println("FAIL:Unknown Argument");
                        } else if (OppoEngineerNative.native_setProductLineTestResult(targetPosition, targetValue)) {
                            pw.println("OK:Set Done");
                        } else {
                            pw.println("FAIL:Access Fail");
                        }
                    } catch (IllegalArgumentException e5) {
                        return shell.handleDefaultCommands(cmd);
                    }
                }
            } catch (IllegalArgumentException e6) {
                return shell.handleDefaultCommands(cmd);
            }
        } else if (cmd.equals("-reset")) {
            try {
                target = shell.getNextArgRequired();
                if (target.equals("bcc")) {
                    if (OppoEngineerNative.native_resetBackCoverColorId()) {
                        pw.println("OK:Reset Ok");
                    } else {
                        pw.println("FAIL:Reset Fail");
                    }
                } else if (target.equals("wp")) {
                    if (!OppoEngineerUtils.isMtkPlatform()) {
                        return shell.handleDefaultCommands(cmd);
                    }
                    if (resetWriteProtectState()) {
                        pw.println("OK:enable wp success");
                    } else {
                        pw.println("FAIL:enable wp fail");
                    }
                } else if (target.equals("er")) {
                    if (OppoEngineerNative.native_resetProductLineTestResult()) {
                        pw.println("OK:Reset Done");
                    } else {
                        pw.println("FAIL:Access Fail");
                    }
                }
            } catch (IllegalArgumentException e7) {
                return shell.handleDefaultCommands(cmd);
            }
        } else if (cmd.equals("-exec")) {
            try {
                String operation = shell.getNextArgRequired();
                int result2;
                if (operation.equals("StartMMI")) {
                    try {
                        serverIntent = new Intent();
                        componentName = new ComponentName(ENGINEERMODE_PACKAGE_NAME, MMI_SERVER_CLASS_NAME);
                        serverIntent.setComponent(componentName);
                        if (this.mActivityManagerInternal.isSystemReady()) {
                            ret = this.mContext.startServiceAsUser(serverIntent, UserHandle.CURRENT);
                            if (ret != null && ret.equals(componentName)) {
                                SystemClock.sleep(200);
                                pw.println("OK:START MMI Server OK");
                                this.mContext.sendBroadcastAsUser(new Intent(MMI_SERVER_START_BROADCAST_ACTION), UserHandle.CURRENT);
                            }
                        } else {
                            pw.println("FAIL:System Not Ready");
                        }
                    } catch (Exception e8) {
                        Slog.d(TAG, "start mmi server exception caught : " + e8.getMessage());
                        pw.println("FAIL:" + e8.getMessage());
                    }
                } else if (operation.equals("StopMMI")) {
                    try {
                        serverIntent = new Intent();
                        serverIntent.setComponent(new ComponentName(ENGINEERMODE_PACKAGE_NAME, MMI_SERVER_CLASS_NAME));
                        if (this.mActivityManagerInternal.isSystemReady()) {
                            if (this.mContext.stopServiceAsUser(serverIntent, UserHandle.CURRENT)) {
                                pw.println("OK:STOP MMI Server OK");
                                this.mContext.sendBroadcastAsUser(new Intent(MMI_SERVER_STOP_BROADCAST_ACTION), UserHandle.CURRENT);
                            } else {
                                pw.println("FAIL:STOP MMI Server FAIL");
                            }
                        } else {
                            pw.println("FAIL:System Not Ready");
                        }
                    } catch (Exception e82) {
                        Slog.d(TAG, "stop mmi server exception caught : " + e82.getMessage());
                        pw.println("FAIL:" + e82.getMessage());
                    }
                } else if (operation.equals("MasterClear")) {
                    try {
                        argument = shell.getNextArgRequired();
                        if (!argument.equals(SystemProperties.get(PCBA_PROPERTY, PCBA_UNKNOWN_DEFAULT))) {
                            pw.println("FAIL:Permission Deined");
                        } else if (this.mActivityManagerInternal.isSystemReady()) {
                            intent = new Intent();
                            intent.setComponent(new ComponentName(ENGINEERMODE_PACKAGE_NAME, MASTER_CLEAR_CLASS_NAME));
                            intent.addFlags(268435456);
                            intent.putExtra("auto_start", "true");
                            intent.putExtra("extends", argument);
                            result2 = startActivityAsUserInternal(intent, 0);
                            if (ActivityManager.isStartResultSuccessful(result2)) {
                                pw.println("OK:Start MasterClear");
                            } else {
                                pw.println("FAIL:Error Code " + result2);
                            }
                        } else {
                            pw.println("FAIL:System Not Ready");
                        }
                    } catch (IllegalArgumentException e9) {
                        return shell.handleDefaultCommands(cmd);
                    }
                } else if (operation.equals("PowerOff")) {
                    try {
                        argument = shell.getNextArgRequired();
                        if (!argument.equals(SystemProperties.get(PCBA_PROPERTY, PCBA_UNKNOWN_DEFAULT))) {
                            pw.println("FAIL:Permission Deined");
                        } else if (this.mActivityManagerInternal.isSystemReady()) {
                            intent = new Intent();
                            intent.setComponent(new ComponentName(ENGINEERMODE_PACKAGE_NAME, MASTER_CLEAR_CLASS_NAME));
                            intent.addFlags(268435456);
                            intent.putExtra("auto_start", "true");
                            intent.putExtra("extends", argument);
                            result2 = startActivityAsUserInternal(intent, 0);
                            if (ActivityManager.isStartResultSuccessful(result2)) {
                                pw.println("OK:Start MasterClear");
                            } else {
                                pw.println("FAIL:Error Code " + result2);
                            }
                        } else {
                            pw.println("FAIL:System Not Ready");
                        }
                    } catch (IllegalArgumentException e10) {
                        return shell.handleDefaultCommands(cmd);
                    }
                }
            } catch (IllegalArgumentException e11) {
                return shell.handleDefaultCommands(cmd);
            }
        } else if (cmd.equals("-cb")) {
            String cbArgument = shell.getNextArg();
            if (cbArgument != null && cbArgument.equals("QUIT_NON_SIGNAL_TEST")) {
                Slog.i(TAG, "QUIT_NON_SIGNAL_TEST CB received");
                intent = new Intent("oppo.intent.action.QUIT_NON_SIGNAL_TEST");
                this.mHandler.post(new Runnable() {
                    public void run() {
                        if (OppoEngineerService.this.mActivityManagerInternal.isSystemReady()) {
                            OppoEngineerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
                        } else {
                            Slog.d(OppoEngineerService.TAG, "system not ready");
                        }
                    }
                });
            }
        } else if (cmd.equals("-wp")) {
            if (!OppoEngineerUtils.isMtkPlatform()) {
                return shell.handleDefaultCommands(cmd);
            }
            try {
                if (!shell.getNextArgRequired().equals("on")) {
                    pw.println("Unknown argument");
                } else if (resetWriteProtectState()) {
                    pw.println("OK:enable wp success");
                } else {
                    pw.println("FAIL:enable wp fail");
                }
            } catch (IllegalArgumentException e12) {
                if (OppoEngineerNative.native_getPartionWriteProtectState()) {
                    pw.println("WP OFF");
                } else {
                    pw.println("WP ON");
                }
            }
        } else if (cmd.equals("-bcc")) {
            try {
                argument = shell.getNextArgRequired();
                if (argument.equals("reset")) {
                    if (OppoEngineerNative.native_resetBackCoverColorId()) {
                        pw.println("OK:RESET OK");
                    } else {
                        pw.println("FAIL:RESET FAIL");
                    }
                } else if (argument.equals("set")) {
                    String color = shell.getNextArg();
                    if (color == null || !isBackCoverColorIdValid(color)) {
                        pw.println("FAIL:INVALID PARAMETER");
                    } else if (OppoEngineerNative.native_setBackCoverColorId(color)) {
                        pw.println("OK");
                    } else {
                        pw.println("FAIL:IO EXCEPTION");
                    }
                }
            } catch (IllegalArgumentException e13) {
                colorId = getBackCoverColorIdInternal();
                if (colorId == null) {
                    pw.println("FAIL:ACCESS FAIL");
                } else {
                    pw.println("OK:" + colorId);
                }
            }
        } else if (!cmd.equals("-mmi")) {
            return shell.handleDefaultCommands(cmd);
        } else {
            try {
                argument = shell.getNextArgRequired();
                if ("start".equals(argument)) {
                    serverIntent = new Intent();
                    componentName = new ComponentName(ENGINEERMODE_PACKAGE_NAME, MMI_SERVER_CLASS_NAME);
                    serverIntent.setComponent(componentName);
                    try {
                        ret = this.mContext.startServiceAsUser(serverIntent, UserHandle.CURRENT);
                        if (ret != null && ret.equals(componentName)) {
                            SystemClock.sleep(200);
                            pw.println("OK:START MMI Server OK");
                            this.mContext.sendBroadcastAsUser(new Intent(MMI_SERVER_START_BROADCAST_ACTION), UserHandle.CURRENT);
                        }
                    } catch (Exception e822) {
                        Slog.d(TAG, "start mmi server exception caught : " + e822.getMessage());
                    }
                    pw.println("FAIL:START MMI Server FAIL");
                } else if ("stop".equals(argument)) {
                    serverIntent = new Intent();
                    serverIntent.setComponent(new ComponentName(ENGINEERMODE_PACKAGE_NAME, MMI_SERVER_CLASS_NAME));
                    try {
                        if (this.mContext.stopServiceAsUser(serverIntent, UserHandle.CURRENT)) {
                            pw.println("OK:STOP MMI Server OK");
                            this.mContext.sendBroadcastAsUser(new Intent(MMI_SERVER_STOP_BROADCAST_ACTION), UserHandle.CURRENT);
                        }
                    } catch (Exception e8222) {
                        Slog.d(TAG, "stop mmi server exception caught : " + e8222.getMessage());
                    }
                    pw.println("FAIL:STOP MMI Server FAIL");
                }
            } catch (IllegalArgumentException e14) {
                return shell.handleDefaultCommands(cmd);
            }
        }
        return 0;
    }

    private void dumpInternal(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args != null && args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String str : args) {
                sb.append(str);
                sb.append("    ");
            }
            Slog.i(TAG, "dumpInternal args is : " + sb.toString().trim());
        }
        if (!OppoEngineerUtils.isSecrecyEncryptState(this.mContext)) {
            new Shell().exec(this.mBinderService, null, fd, null, args, null, new ResultReceiver(null));
        }
    }

    private void addPartionProtectNotification(Context context) {
        Slog.i(TAG, "addPartionProtectNotification");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        notificationManager.createNotificationChannel(new NotificationChannel(ENGINEERMODE_PACKAGE_NAME, this.mContext.getString(17041207), 3));
        Builder builder = new Builder(context);
        builder.setSmallIcon(17301651);
        builder.setAutoCancel(false);
        builder.setContentTitle(this.mContext.getString(17041207));
        builder.setContentText(this.mContext.getString(17041206));
        builder.setShowWhen(true);
        builder.setWhen(System.currentTimeMillis());
        builder.setChannelId(ENGINEERMODE_PACKAGE_NAME);
        Notification status = builder.build();
        status.flags = 34;
        status.priority = 2;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(ENGINEERMODE_PACKAGE_NAME, "com.oppo.engineermode.wireless.NormalModeWarningPage"));
        intent.addFlags(268435456);
        status.contentIntent = PendingIntent.getActivityAsUser(context, 0, intent, 0, null, UserHandle.CURRENT);
        notificationManager.notify(10000, status);
    }
}
