package com.android.server.storage;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ColorSystemUpdateDialog;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.DiskInfo;
import android.os.storage.IStorageManager;
import android.os.storage.IStorageManager.Stub;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings.Global;
import android.util.DebugUtils;
import android.util.Slog;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.EventLogTags;
import com.android.server.LocationManagerService;
import com.android.server.am.OppoProcessManager;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class OppoDeviceStorageMonitorService extends DeviceStorageMonitorService {
    private static final String ACTION_OPPO_SDCARD_STORAGE_LOW = "android.intent.action.OPPO_SDCARD_STORAGE_LOW";
    private static final String ACTION_OPPO_SDCARD_STORAGE_OK = "oppo.intent.action.OPPO_SDCARD_STORAGE_OK";
    private static final String ACTION_OPPO_STORAGE_MONITOR_DCS_UPLOADE = "android.intent.action.OPPO_STORAGE_MONITOR_DCS_UPLOADE";
    private static final String ACT_A_KEY_MOVE = "act_a_key_move";
    private static final String ACT_CLEAN_UP_FILE = "act_clean_up_file";
    private static final String ACT_POP_UP = "act_pop_up";
    private static final String ACT_UNINSTALL_APP = "act_uninstall_app";
    private static final boolean ALLOW_UPLOAD_DCS = true;
    private static final String CLEAR_SCAN_MODE = "DEEP_CLEAN";
    private static final long DEFAULT_LOG_DELTA_BYTES = 67108864;
    private static final long DELAY_FIRST_CHECK = 200;
    private static final long DELAY_SD_MOUNT_CHECK = 200;
    private static final long FORCE_DATA_LOW_BYTES = 157286400;
    private static final long GB_BYTES = 1073741824;
    private static final String ID_DATA_FULL = "dialog_data_full";
    private static final String ID_DATA_LOW = "dialog_data_low_no_AKeyMove";
    private static final String ID_DATA_LOW_WITH_AKEYMOVE = "dialog_data_low_with_AKeyMove";
    private static final String ID_DATA_SD_LOW_STATE = "data_sd_low";
    private static final String ID_SD_LOW = "dialog_sd_low";
    private static final String ID_SD_MOUNT_STATE = "sd_mounted";
    private static final long INTERVAL_DIALOG_DATA = 1800000;
    private static final int MAX_INTERVAL = 21600000;
    private static final long MB_BYTES = 1048576;
    private static final int MIN_INTERVAL = 60000;
    private static final int MODE_DEEP = 2;
    private static final long NUMBER_THREE = 3;
    private static final String OPPO_ACTION_DIALOG_DATA = "oppo.intent.action.DIALOG_DATA";
    private static final String OPPO_ACTION_DIALOG_SD = "oppo.intent.action.DIALOG_SD";
    private static final String OPPO_ACTION_FILE_CLEANUP = "com.oppo.cleandroid.ui.ClearMainActivity";
    private static final String OPPO_ACTION_ONE_KEY_MOVE = "com.oppo.filemanager.akeytomove.AKeyToMoveActivity";
    private static final String OPPO_ACTION_OPEN_FILEMANAGER = "oppo.intent.action.OPEN_FILEMANAGER";
    private static final String OPPO_ACTION_SHOW_LOW_STORAGE_ALERT = "com.oppo.showLowStorageAlert";
    private static final String OPPO_ACTION_TASK_TERMINATION = "oppo.intent.action.TASK_TERMINATION_FOR_LOW_STORAGE";
    private static final String OPPO_ACTION_TOMORROW_ZERO_OCLOCK = "oppo.intent.action.TOMORROW_ZERO_OCLOCK";
    private static final long OPPO_DEFAULT_CHECK_INTERVAL = 30000;
    private static final int OPPO_DEVICE_SD_UNMOUNT = 101;
    private static final int OPPO_MONITOR_INTERVAL = 30;
    private static final long OPPO_SD_NOT_ENOUGH_TRIM_MB = 52428800;
    private static final long OPPO_SHORT_CHECK_INTERVAL = 10000;
    private static final int OPPO_SHORT_INTERVAL = 10;
    private static final String TAG = "OppoDeviceStorageMonitorService";
    private static final long THRESHOLD_DATA_FULL = 52428800;
    private static final long THRESHOLD_DATA_LOW = 838860800;
    private static final long THRESHOLD_DELTA_DATA_LOW = 104857600;
    private static final long THRESHOLD_SD_SUFFICIENT = 1073741824;
    private static final long TIMESTAMP_BOOT_COMPLETE = 120000;
    private static AtomicBoolean sCriticalLowDataFlag = new AtomicBoolean();
    private static final boolean sLocalLOGV = false;
    private boolean mAllowDialogTaskFinishDataShow = true;
    private boolean mAllowDialogTaskFinishSdShow = true;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.LOCALE_CHANGED")) {
                if (OppoDeviceStorageMonitorService.this.mDataLowFlag && OppoDeviceStorageMonitorService.this.mNotificationDataShowed) {
                    OppoDeviceStorageMonitorService.this.oppoCancelNotification(23);
                    OppoDeviceStorageMonitorService.this.oppoNotificationData();
                }
                OppoDeviceStorageMonitorService.this.mLocaleChanged = true;
            } else if (action.equals(OppoDeviceStorageMonitorService.OPPO_ACTION_SHOW_LOW_STORAGE_ALERT)) {
                OppoDeviceStorageMonitorService.this.oppoAlertDialogData();
            } else if (action.equals("android.intent.action.DATE_CHANGED")) {
                String str = "";
                if (OppoDeviceStorageMonitorService.this.mDataLowFlag) {
                    str = str.concat(LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON);
                } else {
                    str = str.concat("0");
                }
                if (OppoDeviceStorageMonitorService.this.mSdLowFlag) {
                    str = str.concat(LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON);
                } else {
                    str = str.concat("0");
                }
                OppoDeviceStorageMonitorService.this.uploadDcsKvEvent(OppoDeviceStorageMonitorService.ID_DATA_SD_LOW_STATE, str, true);
                if (OppoDeviceStorageMonitorService.this.mIsSdMounted) {
                    OppoDeviceStorageMonitorService.this.uploadDcsKvEvent(OppoDeviceStorageMonitorService.ID_SD_MOUNT_STATE, LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON, true);
                } else {
                    OppoDeviceStorageMonitorService.this.uploadDcsKvEvent(OppoDeviceStorageMonitorService.ID_SD_MOUNT_STATE, "0", true);
                }
            } else if (action.equals(OppoDeviceStorageMonitorService.OPPO_ACTION_TASK_TERMINATION)) {
                String pkg = intent.getStringExtra("package");
                String space = intent.getStringExtra("space");
                if (pkg == null || space == null) {
                    Slog.d(OppoDeviceStorageMonitorService.TAG, "OPPO_ACTION_TASK_TERMINATION. pkg=" + pkg + ", space=" + space);
                } else if ("Phone".equals(space)) {
                    if (OppoDeviceStorageMonitorService.this.mListTaskTermData.contains(pkg)) {
                        Slog.d(OppoDeviceStorageMonitorService.TAG, "OPPO_ACTION_TASK_TERMINATION. Phone. pkg(" + pkg + ") has showed TaskTermData before.");
                        return;
                    }
                    long freeDataSpace = OppoDeviceStorageMonitorService.this.getDataFreeSpace();
                    if (freeDataSpace < 0 || freeDataSpace > OppoDeviceStorageMonitorService.this.mDataLowThreshold + OppoDeviceStorageMonitorService.THRESHOLD_DELTA_DATA_LOW) {
                        Slog.d(OppoDeviceStorageMonitorService.TAG, "OPPO_ACTION_TASK_TERMINATION. freeDataSpace=" + (freeDataSpace / OppoDeviceStorageMonitorService.MB_BYTES) + "MB. ignore.");
                    } else {
                        OppoDeviceStorageMonitorService.this.mListTaskTermData.add(pkg);
                        OppoDeviceStorageMonitorService.this.oppoAlertDialogTaskTerminationData();
                    }
                } else if ("sd".equals(space)) {
                    if (OppoDeviceStorageMonitorService.this.mListTaskTermSd.contains(pkg)) {
                        Slog.d(OppoDeviceStorageMonitorService.TAG, "OPPO_ACTION_TASK_TERMINATION. sd. pkg(" + pkg + ") has showed TaskTermSd before.");
                        return;
                    }
                    long freeSdSpace = OppoDeviceStorageMonitorService.this.getSdFreeSpace();
                    if (freeSdSpace < 0 || freeSdSpace > OppoDeviceStorageMonitorService.this.mSdStartTrimThreshold) {
                        Slog.d(OppoDeviceStorageMonitorService.TAG, "OPPO_ACTION_TASK_TERMINATION. freeSdSpace=" + (freeSdSpace / OppoDeviceStorageMonitorService.MB_BYTES) + "MB. ignore.");
                    } else {
                        OppoDeviceStorageMonitorService.this.mListTaskTermSd.add(pkg);
                        OppoDeviceStorageMonitorService.this.oppoAlertDialogTaskTerminationSd();
                    }
                }
            } else if (OppoDeviceStorageMonitorService.OPPO_ACTION_DIALOG_DATA.equals(action)) {
                if (OppoDeviceStorageMonitorService.this.mCntNotifyData >= 2) {
                    Slog.d(OppoDeviceStorageMonitorService.TAG, "OPPO_ACTION_DIALOG_DATA. CntNotifyData=" + OppoDeviceStorageMonitorService.this.mCntNotifyData);
                    return;
                }
                if (OppoDeviceStorageMonitorService.this.oppoAlertDialogData()) {
                    OppoDeviceStorageMonitorService oppoDeviceStorageMonitorService = OppoDeviceStorageMonitorService.this;
                    oppoDeviceStorageMonitorService.mCntNotifyData = oppoDeviceStorageMonitorService.mCntNotifyData + 1;
                }
                OppoDeviceStorageMonitorService.this.oppoNotificationData();
                if (OppoDeviceStorageMonitorService.this.mCntNotifyData < 2) {
                    OppoDeviceStorageMonitorService.this.scheduleAlarmDialogData(0);
                }
            } else if (OppoDeviceStorageMonitorService.OPPO_ACTION_DIALOG_SD.equals(action)) {
                if (!OppoDeviceStorageMonitorService.this.oppoAlertDialogSd()) {
                    OppoDeviceStorageMonitorService.this.scheduleAlarmDialogSd(1800000);
                }
            } else if (OppoDeviceStorageMonitorService.OPPO_ACTION_TOMORROW_ZERO_OCLOCK.equals(action)) {
                OppoDeviceStorageMonitorService.this.mCntNotifyData = 0;
                OppoDeviceStorageMonitorService.this.mListTaskTermData.clear();
                OppoDeviceStorageMonitorService.this.mListTaskTermSd.clear();
                if (OppoDeviceStorageMonitorService.this.mDataLowFlag) {
                    OppoDeviceStorageMonitorService.this.scheduleAlarmDialogData(0);
                }
                OppoDeviceStorageMonitorService.this.scheduleAlarmTomorrowZeroOclock();
            }
        }
    };
    private int mCntNotifyData;
    private Context mContext;
    private long mDataFree;
    private boolean mDataFullFlag = false;
    private long mDataFullThreshold = 52428800;
    private int mDataLevel = 0;
    private boolean mDataLowFlag = false;
    private long mDataLowThreshold = THRESHOLD_DATA_LOW;
    private AlertDialog mDialogData = null;
    private PendingIntent mDialogDataIntent;
    private ColorSystemUpdateDialog mDialogDataMultiKey = null;
    private AlertDialog mDialogSd = null;
    private PendingIntent mDialogSdIntent;
    private AlertDialog mDialogTaskFinishData = null;
    private AlertDialog mDialogTaskFinishSd = null;
    private StatFs mFileStatsData;
    private long mFreeExternalSd;
    private WorkerHandler mHandler;
    private Intent mIntentDcs = null;
    private Intent mIntentFileCleanUP;
    private Intent mIntentFileManager;
    private Intent mIntentOneKeyMove;
    private Intent mIntentPackageStorage;
    private boolean mIsSdMounted = false;
    private long mLastDataFree;
    private List<String> mListTaskTermData = new ArrayList();
    private List<String> mListTaskTermSd = new ArrayList();
    private boolean mLocaleChanged = false;
    private final Object mLock = new Object();
    private IStorageManager mMountService = null;
    private boolean mNotificationDataShowed = false;
    private boolean mSdLowFlag = false;
    private long mSdStartTrimThreshold;
    private Intent mSdStorageLowIntent;
    private Intent mSdStorageOkIntent;
    private boolean mSdSufficient = true;
    private final StorageEventListener mStorageListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            DiskInfo diskInfo = vol.getDisk();
            if (diskInfo != null && (diskInfo.isSd() ^ 1) == 0) {
                if (oldState != 2 && newState == 2) {
                    synchronized (OppoDeviceStorageMonitorService.this.mLock) {
                        OppoDeviceStorageMonitorService.this.mVolumeExternalSd = vol;
                        OppoDeviceStorageMonitorService.this.mHandler.removeMessages(101);
                        OppoDeviceStorageMonitorService.this.reScheduleCheck(200);
                    }
                    Slog.d(OppoDeviceStorageMonitorService.TAG, "onVolumeStateChanged: external TF card mounted. id=" + vol.getId() + ", path=" + vol.path + ", oldState=" + DebugUtils.valueToString(VolumeInfo.class, "STATE_", oldState) + ", newState=" + DebugUtils.valueToString(VolumeInfo.class, "STATE_", newState));
                } else if (newState != 2 && oldState == 2) {
                    synchronized (OppoDeviceStorageMonitorService.this.mLock) {
                        OppoDeviceStorageMonitorService.this.mVolumeExternalSd = null;
                        OppoDeviceStorageMonitorService.this.mHandler.removeMessages(101);
                        OppoDeviceStorageMonitorService.this.mHandler.sendEmptyMessage(101);
                    }
                    Slog.d(OppoDeviceStorageMonitorService.TAG, "onVolumeStateChanged: external TF card unmounted. id=" + vol.getId() + ", path=" + vol.path + ", oldState=" + DebugUtils.valueToString(VolumeInfo.class, "STATE_", oldState) + ", newState=" + DebugUtils.valueToString(VolumeInfo.class, "STATE_", newState));
                }
            }
        }
    };
    private PendingIntent mTomorrowIntent;
    private long mTotalData;
    private VolumeInfo mVolumeExternalSd = null;

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            synchronized (OppoDeviceStorageMonitorService.this.mLock) {
                if (msg.what == 101) {
                    OppoDeviceStorageMonitorService.this.oppoSdUnmounted();
                }
            }
        }
    }

    private void oppoSdUnmounted() {
        this.mIsSdMounted = false;
        Slog.d(TAG, "oppoSdUnmounted");
        if (this.mSdLowFlag) {
            this.mSdLowFlag = false;
            Slog.d(TAG, "oppoSdUnmounted: Cancelling notification");
            sdcancelNotification();
        }
        reactDataLowWarning();
    }

    private long getSdFreeSpace() {
        if (this.mVolumeExternalSd == null || this.mVolumeExternalSd.path == null) {
            return -1;
        }
        File path = this.mVolumeExternalSd.getPath();
        if (path == null || path.getTotalSpace() <= 0) {
            return -1;
        }
        long freeExternalSd = path.getFreeSpace();
        Slog.d(TAG, "getSdFreeSpace: freeExternalSd = " + (freeExternalSd / MB_BYTES) + "MB");
        return freeExternalSd;
    }

    private long getDataFreeSpace() {
        long freeDataSpace = -1;
        if (this.mFileStatsData == null) {
            Slog.d(TAG, "getDataFreeSpace: mFileStatsData is null!!!");
            return -1;
        }
        try {
            this.mFileStatsData.restat(Environment.getDataDirectory().getAbsolutePath());
            freeDataSpace = ((long) this.mFileStatsData.getAvailableBlocks()) * ((long) this.mFileStatsData.getBlockSize());
        } catch (IllegalArgumentException e) {
            Slog.d(TAG, "getDataFreeSpace: IllegalArgumentException.");
        }
        return freeDataSpace;
    }

    private void maybeFreeStorage() {
        StorageManager storage = (StorageManager) getContext().getSystemService(StorageManager.class);
        for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
            File file = vol.getPath();
            long fullBytes = storage.getStorageFullBytes(file);
            long lowBytes = storage.getStorageLowBytes(file);
            if (file.getUsableSpace() < (3 * lowBytes) / 2) {
                try {
                    ((PackageManagerService) ServiceManager.getService("package")).freeStorage(vol.getFsUuid(), 2 * lowBytes, 0);
                } catch (IOException e) {
                }
            }
        }
    }

    private void dataBecomeLow() {
        if (oppoAlertDialogData()) {
            this.mCntNotifyData = 1;
            scheduleAlarmDialogData(0);
        } else {
            this.mCntNotifyData = 0;
            scheduleAlarmDialogData(1800000);
        }
        oppoNotificationData();
        sendDataLowBroadcast();
    }

    private void dataBecomeNotLow() {
        oppoCancelNotification(23);
        sendDataNotLowBroadcast();
        if (this.mDialogData != null && this.mDialogData.isShowing()) {
            this.mDialogData.cancel();
        }
        if (this.mDialogDataMultiKey != null && this.mDialogDataMultiKey.isShowing()) {
            this.mDialogDataMultiKey.cancel();
        }
        this.mAllowDialogTaskFinishDataShow = true;
        this.mNotificationDataShowed = false;
        cancelAlarmDialogData();
    }

    private void oppoCheckData() {
        int dataLevel = this.mDataLevel;
        long dataFree = getDataFreeSpace();
        this.mDataFree = dataFree;
        if (this.mOppoForceLevle == 1) {
            dataFree = FORCE_DATA_LOW_BYTES;
            this.mDataLowFlag = false;
            Slog.d(TAG, "oppoCheckData: mOppoForceLevle is low!!!");
        } else if (this.mOppoForceLevle == 2) {
            dataFree = 0;
            this.mDataFullFlag = false;
            Slog.d(TAG, "oppoCheckData: mOppoForceLevle is full!!!");
        } else if (this.mOppoForceLevle == 0) {
            this.mDataLowFlag = true;
            this.mDataFullFlag = true;
            Slog.d(TAG, "oppoCheckData: mOppoForceLevle is normal!!!");
        }
        if (dataFree < this.mDataLowThreshold) {
            if (!this.mDataLowFlag) {
                Slog.i(TAG, "data become low. freeStorage=" + this.mDataFree + ", mOppoForceLevle=" + this.mOppoForceLevle);
                dataBecomeLow();
                this.mDataLowFlag = true;
            }
            dataLevel = 1;
        } else if (dataFree >= this.mDataLowThreshold + THRESHOLD_DELTA_DATA_LOW) {
            if (this.mDataLowFlag) {
                Slog.i(TAG, "data available. freeStorage=" + this.mDataFree + ", mOppoForceLevle=" + this.mOppoForceLevle);
                dataBecomeNotLow();
                this.mDataLowFlag = false;
            }
            dataLevel = 0;
        }
        if (dataFree < this.mDataFullThreshold) {
            if (!this.mDataFullFlag) {
                Slog.d(TAG, "data become full, freeStorage=" + this.mDataFree + ", mOppoForceLevle=" + this.mOppoForceLevle);
                sendDataFullBroadcast();
                this.mDataFullFlag = true;
            }
            dataLevel = 2;
        } else if (this.mDataFullFlag) {
            Slog.d(TAG, "data become not full, freeStorage=" + this.mDataFree + ", mOppoForceLevle=" + this.mOppoForceLevle);
            sendDataNotFullBroadcast();
            this.mDataFullFlag = false;
        }
        if (this.mDataFullFlag) {
            Slog.d(TAG, "Running on data full, freeStorage=" + this.mDataFree + ", mOppoForceLevle=" + this.mOppoForceLevle);
            sCriticalLowDataFlag.set(true);
            oppoAlertDialogData();
            oppoNotificationData();
        } else {
            sCriticalLowDataFlag.set(false);
        }
        if (Math.abs(this.mLastDataFree - this.mDataFree) > DEFAULT_LOG_DELTA_BYTES || this.mDataLevel != dataLevel) {
            EventLogTags.writeStorageState(StorageManager.UUID_PRIVATE_INTERNAL, this.mDataLevel, dataLevel, this.mDataFree, this.mTotalData);
            this.mLastDataFree = this.mDataFree;
        }
        this.mDataLevel = dataLevel;
    }

    private final void oppoCheckSD() {
        if (this.mVolumeExternalSd != null && this.mVolumeExternalSd.path != null) {
            File path = this.mVolumeExternalSd.getPath();
            if (path != null && path.getTotalSpace() > 0) {
                boolean sdStateChange = false;
                if (!this.mIsSdMounted) {
                    this.mIsSdMounted = true;
                    sdStateChange = true;
                    Slog.v(TAG, "mIsSdMounted set true!");
                }
                this.mFreeExternalSd = path.getFreeSpace();
                if (this.mFreeExternalSd < this.mSdStartTrimThreshold) {
                    if (!this.mSdLowFlag) {
                        Slog.i(TAG, "oppoCheckSD: Running low on SDCARD. Sending notification");
                        sdsendNotification();
                    }
                } else if (this.mSdLowFlag) {
                    Slog.i(TAG, "oppoCheckSD: SDCARD available. Cancelling notification");
                    sdcancelNotification();
                }
                boolean sdSufficientChanged = false;
                if (this.mFreeExternalSd >= 1073741824) {
                    if (!this.mSdSufficient) {
                        this.mSdSufficient = true;
                        sdSufficientChanged = true;
                        Slog.i(TAG, "oppoCheckSD: SDCARD Sufficient.");
                    }
                } else if (this.mSdSufficient) {
                    this.mSdSufficient = false;
                    sdSufficientChanged = true;
                    Slog.i(TAG, "oppoCheckSD: SDCARD not Sufficient.");
                }
                if (sdSufficientChanged || sdStateChange) {
                    reactDataLowWarning();
                }
            }
        }
    }

    private void reactDataLowWarning() {
        if (this.mDataLowFlag) {
            if (this.mNotificationDataShowed) {
                oppoCancelNotification(23);
                oppoNotificationData();
            }
            if (this.mDialogDataMultiKey != null && this.mDialogDataMultiKey.isShowing()) {
                this.mDialogDataMultiKey.cancel();
                oppoAlertDialogData();
            }
            if (this.mDialogData != null && this.mDialogData.isShowing()) {
                this.mDialogData.cancel();
                oppoAlertDialogData();
            }
        }
    }

    private void sendDataLowBroadcast() {
        int seq = this.mSeq.get();
        this.mContext.sendStickyBroadcastAsUser(new Intent("android.intent.action.DEVICE_STORAGE_LOW").addFlags(85983232).putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, seq), UserHandle.ALL);
        Slog.d(TAG, "sendDataLowBroadcast. seq=" + seq);
    }

    private void sendDataNotLowBroadcast() {
        int seq = this.mSeq.get();
        Intent lowIntent = new Intent("android.intent.action.DEVICE_STORAGE_LOW").addFlags(85983232).putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, seq);
        Intent notLowIntent = new Intent("android.intent.action.DEVICE_STORAGE_OK").addFlags(85983232).putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, seq);
        this.mContext.removeStickyBroadcastAsUser(lowIntent, UserHandle.ALL);
        this.mContext.sendBroadcastAsUser(notLowIntent, UserHandle.ALL);
        Slog.d(TAG, "sendDataNotLowBroadcast. seq=" + seq);
    }

    private void sendDataFullBroadcast() {
        this.mContext.sendStickyBroadcastAsUser(new Intent("android.intent.action.DEVICE_STORAGE_FULL").addFlags(67108864).putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSeq.get()), UserHandle.ALL);
        Slog.d(TAG, "sendDataFullBroadcast.");
    }

    private void sendDataNotFullBroadcast() {
        int seq = this.mSeq.get();
        Intent fullIntent = new Intent("android.intent.action.DEVICE_STORAGE_FULL").addFlags(67108864).putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, seq);
        Intent notFullIntent = new Intent("android.intent.action.DEVICE_STORAGE_NOT_FULL").addFlags(67108864).putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, seq);
        this.mContext.removeStickyBroadcastAsUser(fullIntent, UserHandle.ALL);
        this.mContext.sendBroadcastAsUser(notFullIntent, UserHandle.ALL);
        Slog.d(TAG, "sendDataNotFullBroadcast.");
    }

    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (1000 == phase) {
            Slog.d(TAG, "onBootPhase: PHASE_BOOT_COMPLETED");
            reScheduleCheck(200);
        }
    }

    public void onStart() {
        super.onStart();
        Slog.i(TAG, "onStart!!!");
        this.mContext = getContext();
        this.mHandler = new WorkerHandler(this.looperStorageMonitor);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction(OPPO_ACTION_SHOW_LOW_STORAGE_ALERT);
        filter.addAction("android.intent.action.DATE_CHANGED");
        filter.addAction(OPPO_ACTION_TASK_TERMINATION);
        filter.addAction(OPPO_ACTION_DIALOG_DATA);
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, this.mHandler);
        this.mIntentFileCleanUP = new Intent(OPPO_ACTION_FILE_CLEANUP);
        this.mIntentFileCleanUP.putExtra("enter_from", "StorageMonitor");
        this.mIntentFileCleanUP.putExtra(CLEAR_SCAN_MODE, 2);
        this.mIntentFileCleanUP.addFlags(335544320);
        this.mIntentFileManager = new Intent(OPPO_ACTION_OPEN_FILEMANAGER);
        this.mIntentFileManager.addFlags(335544320);
        this.mIntentOneKeyMove = new Intent(OPPO_ACTION_ONE_KEY_MOVE);
        this.mIntentOneKeyMove.addFlags(603979776);
        this.mIntentPackageStorage = new Intent("android.intent.action.MANAGE_PACKAGE_STORAGE");
        this.mIntentPackageStorage.addFlags(335544320);
        this.mSdStorageLowIntent = new Intent(ACTION_OPPO_SDCARD_STORAGE_LOW);
        this.mSdStorageLowIntent.addFlags(67108864);
        this.mSdStorageOkIntent = new Intent(ACTION_OPPO_SDCARD_STORAGE_OK);
        this.mSdStorageOkIntent.addFlags(67108864);
        this.mDialogDataIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(OPPO_ACTION_DIALOG_DATA), 0);
        this.mDialogSdIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(OPPO_ACTION_DIALOG_SD), 0);
        this.mTomorrowIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(OPPO_ACTION_TOMORROW_ZERO_OCLOCK), 0);
        this.mSdStartTrimThreshold = 52428800;
        this.mMountService = Stub.asInterface(ServiceManager.getService(OppoProcessManager.RESUME_REASON_MOUNT_STR));
        ((StorageManager) this.mContext.getSystemService(StorageManager.class)).registerListener(this.mStorageListener);
        this.mFileStatsData = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        this.mTotalData = ((long) this.mFileStatsData.getBlockCount()) * ((long) this.mFileStatsData.getBlockSize());
    }

    public OppoDeviceStorageMonitorService(Context context) {
        super(context);
    }

    private void scheduleAlarmDialogData(long delay) {
        long now = System.currentTimeMillis();
        long alarmTime = now + delay;
        if (delay <= 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(now);
            c.set(11, 0);
            c.set(12, 0);
            c.set(13, 0);
            c.set(14, 0);
            c.add(5, 1);
            int delta = MAX_INTERVAL;
            if (c.getTimeInMillis() - now < 21600000) {
                delta = (int) (c.getTimeInMillis() - now);
            }
            int randNum = new Random().nextInt(delta);
            if (randNum < 60000) {
                randNum = 60000;
            }
            alarmTime = now + ((long) randNum);
        }
        ((AlarmManager) this.mContext.getSystemService("alarm")).setExact(1, alarmTime, this.mDialogDataIntent);
        Calendar cTmp = Calendar.getInstance();
        cTmp.setTimeInMillis(alarmTime);
        Slog.d(TAG, "scheduleAlarmDialogData: alarmTime= " + cTmp.getTime());
    }

    private void cancelAlarmDialogData() {
        ((AlarmManager) this.mContext.getSystemService("alarm")).cancel(this.mDialogDataIntent);
    }

    private void scheduleAlarmDialogSd(long delay) {
        long alarmTime = System.currentTimeMillis() + delay;
        ((AlarmManager) this.mContext.getSystemService("alarm")).setExact(1, alarmTime, this.mDialogSdIntent);
        Calendar cTmp = Calendar.getInstance();
        cTmp.setTimeInMillis(alarmTime);
        Slog.d(TAG, "scheduleAlarmDialogSd: alarmTime= " + cTmp.getTime());
    }

    private void cancelAlarmDialogSd() {
        ((AlarmManager) this.mContext.getSystemService("alarm")).cancel(this.mDialogSdIntent);
    }

    private void scheduleAlarmTomorrowZeroOclock() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(11, 0);
        c.set(12, 0);
        c.set(13, 0);
        c.set(14, 0);
        c.add(5, 1);
        ((AlarmManager) this.mContext.getSystemService("alarm")).set(0, c.getTimeInMillis(), this.mTomorrowIntent);
    }

    private void oppoNotificationData() {
        if (!this.mNotificationDataShowed) {
            PendingIntent intent;
            CharSequence details;
            NotificationManager mNotificationMgr = (NotificationManager) this.mContext.getSystemService("notification");
            CharSequence title = this.mContext.getText(201590065);
            if (this.mIsSdMounted && this.mSdSufficient) {
                intent = PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(OPPO_ACTION_SHOW_LOW_STORAGE_ALERT), 0, UserHandle.CURRENT);
                details = this.mContext.getText(201590067);
            } else {
                intent = PendingIntent.getActivityAsUser(this.mContext, 0, this.mIntentFileCleanUP, 0, null, UserHandle.CURRENT);
                details = this.mContext.getText(201590067);
            }
            Notification notification = new Builder(this.mContext, SystemNotificationChannels.ALERTS).setContentTitle(title).setContentText(details).setSmallIcon(201852148).setContentIntent(intent).setVisibility(1).setCategory("sys").build();
            notification.flags |= 32;
            mNotificationMgr.notifyAsUser(null, 23, notification, UserHandle.ALL);
            this.mNotificationDataShowed = true;
            Slog.d(TAG, "oppoNotificationData: send notification.");
        }
    }

    private void oppoCancelNotification(int id) {
        ((NotificationManager) this.mContext.getSystemService("notification")).cancelAsUser(null, id, UserHandle.ALL);
        if (id == 23) {
            this.mNotificationDataShowed = false;
        }
        Slog.d(TAG, "oppoCancelNotification: id=" + id);
    }

    private void sdsendNotification() {
        if (!oppoAlertDialogSd()) {
            scheduleAlarmDialogSd(1800000);
        }
        this.mSdLowFlag = true;
        this.mContext.sendStickyBroadcastAsUser(this.mSdStorageLowIntent, UserHandle.ALL);
    }

    private void sdcancelNotification() {
        if (this.mDialogSd != null && this.mDialogSd.isShowing()) {
            this.mDialogSd.cancel();
        }
        this.mSdLowFlag = false;
        this.mContext.removeStickyBroadcastAsUser(this.mSdStorageLowIntent, UserHandle.ALL);
        this.mContext.sendBroadcastAsUser(this.mSdStorageOkIntent, UserHandle.ALL);
        this.mAllowDialogTaskFinishSdShow = true;
        cancelAlarmDialogSd();
    }

    private boolean oppoAlertDialogData() {
        if (this.mDialogTaskFinishData == null || !this.mDialogTaskFinishData.isShowing()) {
            if (this.mIsSdMounted && this.mSdSufficient) {
                if (this.mDialogData != null && this.mDialogData.isShowing()) {
                    this.mDialogData.cancel();
                    Slog.d(TAG, "oppoAlertDialogData: cancel old DialogData.");
                }
                if (this.mDialogDataMultiKey == null || !this.mDialogDataMultiKey.isShowing() || (this.mLocaleChanged ^ 1) == 0) {
                    this.mDialogDataMultiKey = new ColorSystemUpdateDialog.Builder(this.mContext).setTitle(201590065).setMessage(201590089).setItems(201786390, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    OppoDeviceStorageMonitorService.this.mContext.startActivityAsUser(OppoDeviceStorageMonitorService.this.mIntentFileCleanUP, UserHandle.CURRENT);
                                    OppoDeviceStorageMonitorService.this.uploadDcsKvEvent(OppoDeviceStorageMonitorService.ID_DATA_LOW_WITH_AKEYMOVE, OppoDeviceStorageMonitorService.ACT_CLEAN_UP_FILE, false);
                                    return;
                                case 1:
                                    OppoDeviceStorageMonitorService.this.mContext.startActivityAsUser(OppoDeviceStorageMonitorService.this.mIntentOneKeyMove, UserHandle.CURRENT);
                                    OppoDeviceStorageMonitorService.this.uploadDcsKvEvent(OppoDeviceStorageMonitorService.ID_DATA_LOW_WITH_AKEYMOVE, OppoDeviceStorageMonitorService.ACT_A_KEY_MOVE, false);
                                    return;
                                default:
                                    return;
                            }
                        }
                    }).setCancelable(false).create();
                    this.mDialogDataMultiKey.getWindow().setType(2003);
                    ignoreMenuHOmeKey(this.mDialogDataMultiKey.getWindow());
                    this.mDialogDataMultiKey.show();
                    uploadDcsKvEvent(ID_DATA_LOW_WITH_AKEYMOVE, ACT_POP_UP, false);
                    Slog.d(TAG, "oppoAlertDialogData: show DialogDataMultiKey.");
                } else {
                    Slog.d(TAG, "oppoAlertDialogData: DialogDataMultiKey is showing.");
                    return false;
                }
            }
            if (this.mDialogDataMultiKey != null && this.mDialogDataMultiKey.isShowing()) {
                this.mDialogDataMultiKey.cancel();
                Slog.d(TAG, "oppoAlertDialogData: cancel old DialogDataMultiKey.");
            }
            if (this.mDialogData == null || !this.mDialogData.isShowing() || (this.mLocaleChanged ^ 1) == 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
                builder.setTitle(201590065);
                builder.setMessage(201590066);
                builder.setPositiveButton(201590060, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        OppoDeviceStorageMonitorService.this.mContext.startActivityAsUser(OppoDeviceStorageMonitorService.this.mIntentFileCleanUP, UserHandle.CURRENT);
                        OppoDeviceStorageMonitorService.this.uploadDcsKvEvent(OppoDeviceStorageMonitorService.ID_DATA_LOW, OppoDeviceStorageMonitorService.ACT_CLEAN_UP_FILE, false);
                    }
                });
                builder.setNegativeButton(201590061, null);
                this.mDialogData = builder.create();
                this.mDialogData.getWindow().setType(2003);
                this.mDialogData.setCancelable(false);
                ignoreMenuHOmeKey(this.mDialogData.getWindow());
                this.mDialogData.show();
                uploadDcsKvEvent(ID_DATA_LOW, ACT_POP_UP, false);
                Slog.d(TAG, "oppoAlertDialogData: show DialogData.");
            } else {
                Slog.d(TAG, "oppoAlertDialogData: DialogData is showing.");
                return false;
            }
            this.mLocaleChanged = false;
            return true;
        }
        Slog.d(TAG, "oppoAlertDialogData: DialogTaskFinishdata is showing.");
        return false;
    }

    private boolean oppoAlertDialogSd() {
        if (this.mDialogTaskFinishSd == null || !this.mDialogTaskFinishSd.isShowing()) {
            if (this.mDialogSd != null && this.mDialogSd.isShowing()) {
                this.mDialogSd.cancel();
                Slog.i(TAG, "oppoAlertDialogSd: cacel old DialogSd");
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
            builder.setTitle(201590063);
            builder.setPositiveButton(201590059, new OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    OppoDeviceStorageMonitorService.this.mContext.startActivityAsUser(OppoDeviceStorageMonitorService.this.mIntentFileManager, UserHandle.CURRENT);
                    OppoDeviceStorageMonitorService.this.uploadDcsKvEvent(OppoDeviceStorageMonitorService.ID_SD_LOW, OppoDeviceStorageMonitorService.ACT_CLEAN_UP_FILE, false);
                }
            });
            builder.setNegativeButton(201590061, null);
            this.mDialogSd = builder.create();
            this.mDialogSd.getWindow().setType(2003);
            ignoreMenuHOmeKey(this.mDialogSd.getWindow());
            this.mDialogSd.setCancelable(false);
            this.mDialogSd.show();
            TextView msg = (TextView) this.mDialogSd.findViewById(16908299);
            if (msg != null) {
                msg.setGravity(17);
            }
            uploadDcsKvEvent(ID_SD_LOW, ACT_POP_UP, false);
            Slog.d(TAG, "oppoAlertDialogSd: show DialogSd.");
            return true;
        }
        Slog.d(TAG, "oppoAlertDialogSd: DialogTaskFinishSd is showing.");
        return false;
    }

    private void uploadDcsKvEvent(String id, String act, boolean force) {
        if (!sCriticalLowDataFlag.get() || force) {
            this.mIntentDcs = new Intent(ACTION_OPPO_STORAGE_MONITOR_DCS_UPLOADE);
            this.mIntentDcs.putExtra("eventId", id);
            this.mIntentDcs.putExtra("act", act);
            this.mIntentDcs.addFlags(67108864);
            this.mContext.sendBroadcastAsUser(this.mIntentDcs, UserHandle.ALL);
        }
    }

    private void dumpVolumeinfo() {
        try {
            for (VolumeInfo vol : this.mMountService.getVolumes(0)) {
                DiskInfo diskInfo = vol.getDisk();
                File path = vol.getPath();
                Slog.d(TAG, "id=" + vol.getId() + ", path=" + vol.path + ", internalPath=" + vol.internalPath + ", diskInfo=" + diskInfo + ", type=" + DebugUtils.valueToString(VolumeInfo.class, "TYPE_", vol.getType()) + ", state=" + DebugUtils.valueToString(VolumeInfo.class, "STATE_", vol.state));
                if (diskInfo != null) {
                    Slog.d(TAG, "isSd=" + diskInfo.isSd() + ", isUsb=" + diskInfo.isUsb());
                }
            }
        } catch (RemoteException e) {
        }
    }

    private void ignoreMenuHOmeKey(Window window) {
        if (window == null) {
            Slog.i(TAG, "ignoreMenuHOmeKey: window is null!");
            return;
        }
        LayoutParams p = window.getAttributes();
        p.ignoreHomeMenuKey = 1;
        window.setAttributes(p);
    }

    private void oppoAlertDialogTaskTerminationData() {
        if (this.mAllowDialogTaskFinishDataShow) {
            this.mAllowDialogTaskFinishDataShow = false;
            if (this.mDialogTaskFinishData == null || !this.mDialogTaskFinishData.isShowing()) {
                if (this.mDialogData != null && this.mDialogData.isShowing()) {
                    this.mDialogData.cancel();
                    Slog.d(TAG, "oppoAlertDialogTaskTerminationData: cancel DialogData.");
                }
                if (this.mDialogDataMultiKey != null && this.mDialogDataMultiKey.isShowing()) {
                    this.mDialogDataMultiKey.cancel();
                    Slog.d(TAG, "oppoAlertDialogTaskTerminationData: cancel mDialogDataMultiKey.");
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
                builder.setTitle(201590143);
                builder.setPositiveButton(201590059, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        OppoDeviceStorageMonitorService.this.mContext.startActivityAsUser(OppoDeviceStorageMonitorService.this.mIntentFileCleanUP, UserHandle.CURRENT);
                    }
                });
                builder.setNegativeButton(201590061, null);
                this.mDialogTaskFinishData = builder.create();
                this.mDialogTaskFinishData.getWindow().setType(2003);
                ignoreMenuHOmeKey(this.mDialogTaskFinishData.getWindow());
                this.mDialogTaskFinishData.setCancelable(false);
                this.mDialogTaskFinishData.show();
                TextView msg = (TextView) this.mDialogTaskFinishData.findViewById(16908299);
                if (msg != null) {
                    msg.setGravity(17);
                }
                Slog.d(TAG, "oppoAlertDialogTaskTerminationData: show...");
                return;
            }
            Slog.d(TAG, "oppoAlertDialogTaskTerminationData: is showing.");
            return;
        }
        Slog.d(TAG, "oppoAlertDialogTaskTerminationData: has showed before.");
    }

    private void oppoAlertDialogTaskTerminationSd() {
        if (this.mAllowDialogTaskFinishSdShow) {
            this.mAllowDialogTaskFinishSdShow = false;
            if (this.mDialogTaskFinishSd == null || !this.mDialogTaskFinishSd.isShowing()) {
                if (this.mDialogSd != null && this.mDialogSd.isShowing()) {
                    this.mDialogSd.cancel();
                    Slog.d(TAG, "oppoAlertDialogTaskTerminationSd: cacel DialogSd");
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
                builder.setTitle(201590144);
                builder.setPositiveButton(201590059, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        OppoDeviceStorageMonitorService.this.mContext.startActivityAsUser(OppoDeviceStorageMonitorService.this.mIntentFileManager, UserHandle.CURRENT);
                    }
                });
                builder.setNegativeButton(201590061, null);
                this.mDialogTaskFinishSd = builder.create();
                this.mDialogTaskFinishSd.getWindow().setType(2003);
                ignoreMenuHOmeKey(this.mDialogTaskFinishSd.getWindow());
                this.mDialogTaskFinishSd.setCancelable(false);
                this.mDialogTaskFinishSd.show();
                TextView msg = (TextView) this.mDialogTaskFinishSd.findViewById(16908299);
                if (msg != null) {
                    msg.setGravity(17);
                }
                Slog.d(TAG, "oppoAlertDialogTaskTerminationSd: show...");
                return;
            }
            Slog.d(TAG, "oppoAlertDialogTaskTerminationSd: is showing.");
            return;
        }
        Slog.d(TAG, "oppoAlertDialogTaskTerminationSd: has showed before.");
    }

    private final String formatBytesLocked(long bytes) {
        StringBuilder formatBuilder = new StringBuilder(32);
        Formatter formatter = new Formatter(formatBuilder);
        formatBuilder.setLength(0);
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < MB_BYTES) {
            formatter.format("%.2fKB", new Object[]{Double.valueOf(((double) bytes) / 1024.0d)});
            return formatBuilder.toString();
        } else if (bytes < 1073741824) {
            formatter.format("%.2fMB", new Object[]{Double.valueOf(((double) bytes) / 1048576.0d)});
            return formatBuilder.toString();
        } else {
            formatter.format("%.2fGB", new Object[]{Double.valueOf(((double) bytes) / 1.073741824E9d)});
            return formatBuilder.toString();
        }
    }

    private boolean isNormalBoot() {
        String cryptState = SystemProperties.get("vold.decrypt", "trigger_restart_framework");
        if ("trigger_restart_framework".equals(cryptState)) {
            return true;
        }
        Slog.d(TAG, "cryptState = " + cryptState);
        return false;
    }

    long getMemoryLowThresholdInternal() {
        return this.mDataLowThreshold;
    }

    void oppoCheckStorage() {
        synchronized (this.mLock) {
            if (!isNormalBoot()) {
            } else if (isDeviceProvisioned()) {
                oppoCheckSD();
                oppoCheckData();
            } else {
                Slog.d(TAG, "oppoCheckStorage: DEVICE_PROVISIONED is not set!!!!!!");
            }
        }
    }

    private boolean isDeviceProvisioned() {
        if (Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
            return true;
        }
        return false;
    }

    void oppoDumpImpl(PrintWriter pw) {
        pw.println("Current OppoDeviceStorageMonitor state:");
        pw.print("  mDataFree=");
        pw.print(formatBytesLocked(this.mDataFree));
        pw.print("  mTotalData=");
        pw.println(formatBytesLocked(this.mTotalData));
        pw.print("  mMemLowThreshold=");
        pw.print(formatBytesLocked(this.mDataLowThreshold));
        pw.print("  mMemFullThreshold=");
        pw.println(formatBytesLocked(this.mDataFullThreshold));
        pw.print("  mDataLowFlag=");
        pw.print(this.mDataLowFlag);
        pw.print("  mDataFullFlag=");
        pw.println(this.mDataFullFlag);
        pw.println();
        pw.print("  mFreeExternalSd=");
        pw.println(formatBytesLocked(this.mFreeExternalSd));
        pw.print("  mSdStartTrimThreshold=");
        pw.println(formatBytesLocked(this.mSdStartTrimThreshold));
        pw.print("  mSdLowFlag=");
        pw.println(this.mSdLowFlag);
    }
}
