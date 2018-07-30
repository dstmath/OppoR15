package com.android.server;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothCallback;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothHeadset;
import android.bluetooth.IBluetoothManager.Stub;
import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothProfileServiceConnection;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.UserManagerInternal.UserRestrictionsListener;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.am.OppoFreeFormManagerService;
import com.android.server.am.OppoMultiAppManager;
import com.android.server.am.OppoProcessManager;
import com.android.server.pm.UserRestrictionsUtils;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.oppo.hypnus.Hypnus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class BluetoothManagerService extends Stub {
    private static final int ACTIVE_LOG_MAX_SIZE = 20;
    private static final int ADD_PROXY_DELAY_MS = 100;
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final int BLUETOOTH_OFF = 0;
    private static final int BLUETOOTH_ON_AIRPLANE = 2;
    private static final int BLUETOOTH_ON_BLUETOOTH = 1;
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    private static final String BLUETOOTH_PRIVILEGED_PERM = "android.permission.BLUETOOTH_PRIVILEGED";
    private static final int CRASH_LOG_MAX_SIZE = 100;
    private static final boolean DBG = true;
    private static final int ERROR_RESTART_TIME_MS = 3000;
    private static final int MAX_ERROR_RESTART_RETRIES = 6;
    private static final int MAX_SAVE_RETRIES = 3;
    private static final int MESSAGE_ADD_PROXY_DELAYED = 400;
    private static final int MESSAGE_BIND_PROFILE_SERVICE = 401;
    private static final int MESSAGE_BLUETOOTH_SERVICE_CONNECTED = 40;
    private static final int MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED = 41;
    private static final int MESSAGE_BLUETOOTH_STATE_CHANGE = 60;
    private static final int MESSAGE_DISABLE = 2;
    private static final int MESSAGE_ENABLE = 1;
    private static final int MESSAGE_GET_NAME_AND_ADDRESS = 200;
    private static final int MESSAGE_REGISTER_ADAPTER = 20;
    private static final int MESSAGE_REGISTER_STATE_CHANGE_CALLBACK = 30;
    private static final int MESSAGE_RESTART_BLUETOOTH_SERVICE = 42;
    private static final int MESSAGE_RESTORE_USER_SETTING = 500;
    private static final int MESSAGE_SAVE_NAME_AND_ADDRESS = 201;
    private static final int MESSAGE_TIMEOUT_BIND = 100;
    private static final int MESSAGE_TIMEOUT_UNBIND = 101;
    private static final int MESSAGE_UNREGISTER_ADAPTER = 21;
    private static final int MESSAGE_UNREGISTER_STATE_CHANGE_CALLBACK = 31;
    private static final int MESSAGE_USER_SWITCHED = 300;
    private static final int MESSAGE_USER_UNLOCKED = 301;
    private static final String PACKAGE_NAME_OSHARE = "com.coloros.oshare";
    private static final String REASON_AIRPLANE_MODE = "airplane mode";
    private static final String REASON_DISALLOWED = "disallowed by system";
    private static final String REASON_RESTARTED = "automatic restart";
    private static final String REASON_RESTORE_USER_SETTING = "restore user setting";
    private static final String REASON_SHARING_DISALLOWED = "sharing disallowed by system";
    private static final String REASON_START_CRASH = "turn-on crash";
    private static final String REASON_SYSTEM_BOOT = "system boot";
    private static final String REASON_UNEXPECTED = "unexpected crash";
    private static final String REASON_USER_SWITCH = "user switch";
    private static final int RESTORE_SETTING_TO_OFF = 0;
    private static final int RESTORE_SETTING_TO_ON = 1;
    private static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";
    private static final String SECURE_SETTINGS_BLUETOOTH_ADDR_VALID = "bluetooth_addr_valid";
    private static final String SECURE_SETTINGS_BLUETOOTH_NAME = "bluetooth_name";
    private static final int SERVICE_IBLUETOOTH = 1;
    private static final int SERVICE_IBLUETOOTHGATT = 2;
    private static final int SERVICE_RESTART_TIME_MS = 200;
    private static final String TAG = "BluetoothManagerService";
    private static final int TIMEOUT_BIND_MS = 3000;
    private static final int TIMEOUT_SAVE_MS = 500;
    private static final int USER_SWITCHED_TIME_MS = 200;
    private LinkedList<ActiveLog> mActiveLogs;
    private String mAddress;
    private final ContentObserver mAirplaneModeObserver = new ContentObserver(null) {
        public void onChange(boolean unused) {
            synchronized (this) {
                if (BluetoothManagerService.this.isBluetoothPersistedStateOn()) {
                    if (BluetoothManagerService.this.isAirplaneModeOn()) {
                        BluetoothManagerService.this.persistBluetoothSetting(2);
                    } else {
                        BluetoothManagerService.this.persistBluetoothSetting(1);
                    }
                }
                int st = 10;
                try {
                    BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                    if (BluetoothManagerService.this.mBluetooth != null) {
                        st = BluetoothManagerService.this.mBluetooth.getState();
                    }
                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    Slog.d(BluetoothManagerService.TAG, "Airplane Mode change - current state:  " + BluetoothAdapter.nameForState(st));
                    if (BluetoothManagerService.this.isAirplaneModeOn()) {
                        BluetoothManagerService.this.clearBleApps();
                        if (st == 15) {
                            try {
                                BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                                if (BluetoothManagerService.this.mBluetooth != null) {
                                    BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                                    BluetoothManagerService.this.mEnable = false;
                                    BluetoothManagerService.this.mEnableExternal = false;
                                }
                                BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                            } catch (RemoteException e) {
                                Slog.e(BluetoothManagerService.TAG, "Unable to call onBrEdrDown", e);
                                BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                            } catch (Throwable th) {
                                BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                                throw th;
                            }
                        } else if (st == 12) {
                            BluetoothManagerService.this.sendDisableMsg(BluetoothManagerService.REASON_AIRPLANE_MODE);
                        }
                    } else if (BluetoothManagerService.this.mEnableExternal && BluetoothManagerService.this.isBluetoothPersistedStateOn()) {
                        BluetoothManagerService.this.sendEnableMsg(BluetoothManagerService.this.mQuietEnableExternal, BluetoothManagerService.REASON_AIRPLANE_MODE);
                    }
                } catch (RemoteException e2) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to call getState", e2);
                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                } catch (Throwable th2) {
                    throw th2;
                }
            }
        }
    };
    private boolean mBinding;
    private Map<IBinder, ClientDeathRecipient> mBleApps = new ConcurrentHashMap();
    private IBluetooth mBluetooth;
    private IBinder mBluetoothBinder;
    private final IBluetoothCallback mBluetoothCallback = new IBluetoothCallback.Stub() {
        public void onBluetoothStateChange(int prevState, int newState) throws RemoteException {
            BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(60, prevState, newState));
        }
    };
    private IBluetoothGatt mBluetoothGatt;
    private final ReentrantReadWriteLock mBluetoothLock = new ReentrantReadWriteLock();
    private final RemoteCallbackList<IBluetoothManagerCallback> mCallbacks;
    private BluetoothServiceConnection mConnection = new BluetoothServiceConnection();
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private LinkedList<Long> mCrashTimestamps;
    private int mCrashes;
    private boolean mEnable;
    private boolean mEnableExternal;
    private int mErrorRecoveryRetryCounter;
    private final BluetoothHandler mHandler = new BluetoothHandler(IoThread.get().getLooper());
    private Hypnus mHyp = null;
    private String mName;
    private final boolean mPermissionReviewRequired;
    private final Map<Integer, ProfileServiceConnections> mProfileServices = new HashMap();
    private boolean mQuietEnable = false;
    private boolean mQuietEnableExternal;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED".equals(action)) {
                String newName = intent.getStringExtra("android.bluetooth.adapter.extra.LOCAL_NAME");
                Slog.d(BluetoothManagerService.TAG, "Bluetooth Adapter name changed to " + newName);
                if (newName != null) {
                    BluetoothManagerService.this.storeNameAndAddress(newName, null);
                }
            } else if ("android.bluetooth.adapter.action.BLUETOOTH_ADDRESS_CHANGED".equals(action)) {
                String newAddress = intent.getStringExtra("android.bluetooth.adapter.extra.BLUETOOTH_ADDRESS");
                if (newAddress != null) {
                    Slog.d(BluetoothManagerService.TAG, "Bluetooth Adapter address changed to " + newAddress);
                    BluetoothManagerService.this.storeNameAndAddress(null, newAddress);
                    return;
                }
                Slog.e(BluetoothManagerService.TAG, "No Bluetooth Adapter address parameter found");
            } else if ("android.os.action.SETTING_RESTORED".equals(action)) {
                if ("bluetooth_on".equals(intent.getStringExtra("setting_name"))) {
                    String prevValue = intent.getStringExtra("previous_value");
                    String newValue = intent.getStringExtra("new_value");
                    Slog.d(BluetoothManagerService.TAG, "ACTION_SETTING_RESTORED with BLUETOOTH_ON, prevValue=" + prevValue + ", newValue=" + newValue);
                    if (newValue != null && prevValue != null && (prevValue.equals(newValue) ^ 1) != 0) {
                        int i;
                        BluetoothHandler -get10 = BluetoothManagerService.this.mHandler;
                        if (newValue.equals("0")) {
                            i = 0;
                        } else {
                            i = 1;
                        }
                        BluetoothManagerService.this.mHandler.sendMessage(-get10.obtainMessage(500, i, 0));
                    }
                }
            }
        }
    };
    private int mState;
    private final RemoteCallbackList<IBluetoothStateChangeCallback> mStateChangeCallbacks;
    private final int mSystemUiUid;
    private boolean mUnbinding;
    private final UserRestrictionsListener mUserRestrictionsListener = new UserRestrictionsListener() {
        public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            if (UserRestrictionsUtils.restrictionsChanged(prevRestrictions, newRestrictions, "no_bluetooth_sharing")) {
                BluetoothManagerService.this.updateOppLauncherComponentState(userId, newRestrictions.getBoolean("no_bluetooth_sharing"));
            }
            if (userId == 0) {
                if (!UserRestrictionsUtils.restrictionsChanged(prevRestrictions, newRestrictions, "no_bluetooth")) {
                    return;
                }
                if (userId == 0 && newRestrictions.getBoolean("no_bluetooth")) {
                    BluetoothManagerService.this.updateOppLauncherComponentState(userId, true);
                    BluetoothManagerService.this.sendDisableMsg(BluetoothManagerService.REASON_DISALLOWED);
                    return;
                }
                BluetoothManagerService.this.updateOppLauncherComponentState(userId, newRestrictions.getBoolean("no_bluetooth_sharing"));
            }
        }
    };

    private class ActiveLog {
        private boolean mEnable;
        private String mPackageName;
        private long mTimestamp;

        public ActiveLog(String packageName, boolean enable, long timestamp) {
            this.mPackageName = packageName;
            this.mEnable = enable;
            this.mTimestamp = timestamp;
        }

        public long getTime() {
            return this.mTimestamp;
        }

        public String toString() {
            return BluetoothManagerService.this.timeToLog(this.mTimestamp) + (this.mEnable ? "  Enabled " : " Disabled ") + " by " + this.mPackageName;
        }
    }

    private class BluetoothHandler extends Handler {
        boolean mGetNameAddressOnly = false;

        public BluetoothHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            ProfileServiceConnections psc;
            switch (msg.what) {
                case 1:
                    int state;
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_ENABLE(" + msg.arg1 + "): mBluetooth = " + BluetoothManagerService.this.mBluetooth);
                    BluetoothManagerService.this.mHandler.removeMessages(42);
                    BluetoothManagerService.this.mEnable = true;
                    try {
                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            state = BluetoothManagerService.this.mBluetooth.getState();
                            if (state != 15) {
                                if (state == 14 || state == 11) {
                                    Slog.w(BluetoothManagerService.TAG, "BT is enabling, ignore new enable request.");
                                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                                    break;
                                }
                            }
                            Slog.w(BluetoothManagerService.TAG, "BT Enable in BLE_ON State, going to ON");
                            BluetoothManagerService.this.mBluetooth.onLeServiceUp();
                            BluetoothManagerService.this.persistBluetoothSetting(1);
                            break;
                        }
                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    } catch (RemoteException e) {
                        Slog.e(BluetoothManagerService.TAG, "", e);
                    } finally {
                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    }
                    BluetoothManagerService.this.mQuietEnable = msg.arg1 == 1;
                    if (BluetoothManagerService.this.mBluetooth != null) {
                        try {
                            state = BluetoothManagerService.this.mBluetooth.getState();
                            if (state == 13 || state == 16) {
                                BluetoothManagerService.this.waitForMonitoredOnOff(false, true);
                            }
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(42), 400);
                            break;
                        } catch (RemoteException e2) {
                            Slog.e(BluetoothManagerService.TAG, "getState()", e2);
                            break;
                        }
                    }
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                    break;
                case 2:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_DISABLE: mBluetooth = " + BluetoothManagerService.this.mBluetooth);
                    BluetoothManagerService.this.mHandler.removeMessages(42);
                    if (BluetoothManagerService.this.mEnable && BluetoothManagerService.this.mBluetooth != null) {
                        BluetoothManagerService.this.waitForMonitoredOnOff(true, false);
                        BluetoothManagerService.this.mEnable = false;
                        BluetoothManagerService.this.handleDisable();
                        BluetoothManagerService.this.waitForMonitoredOnOff(false, false);
                        break;
                    }
                    BluetoothManagerService.this.mEnable = false;
                    BluetoothManagerService.this.handleDisable();
                    break;
                case 20:
                    Object callback = msg.obj;
                    boolean added = BluetoothManagerService.this.mCallbacks.register(callback, Integer.valueOf(msg.arg1));
                    String str = BluetoothManagerService.TAG;
                    StringBuilder append = new StringBuilder().append("Added callback: ");
                    if (callback == null) {
                        callback = "null";
                    }
                    Slog.d(str, append.append(callback).append(":").append(added).toString());
                    break;
                case 21:
                    BluetoothManagerService.this.mCallbacks.unregister((IBluetoothManagerCallback) msg.obj);
                    break;
                case 30:
                    IBluetoothStateChangeCallback callback2 = msg.obj;
                    if (callback2 != null) {
                        BluetoothManagerService.this.mStateChangeCallbacks.register(callback2, Integer.valueOf(msg.arg1));
                        break;
                    }
                    break;
                case 31:
                    BluetoothManagerService.this.mStateChangeCallbacks.unregister((IBluetoothStateChangeCallback) msg.obj);
                    break;
                case 40:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_SERVICE_CONNECTED: " + msg.arg1);
                    IBinder service = msg.obj;
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (msg.arg1 != 2) {
                            BluetoothManagerService.this.mHandler.removeMessages(100);
                            BluetoothManagerService.this.mBinding = false;
                            BluetoothManagerService.this.mBluetoothBinder = service;
                            BluetoothManagerService.this.mBluetooth = IBluetooth.Stub.asInterface(Binder.allowBlocking(service));
                            if (!BluetoothManagerService.this.isNameAndAddressSet()) {
                                BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(200));
                                if (this.mGetNameAddressOnly) {
                                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                                    return;
                                }
                            }
                            BluetoothManagerService.this.mBluetooth.registerCallback(BluetoothManagerService.this.mBluetoothCallback);
                            BluetoothManagerService.this.sendBluetoothServiceUpCallback();
                            try {
                                if (BluetoothManagerService.this.mQuietEnable) {
                                    if (!BluetoothManagerService.this.mBluetooth.enableNoAutoConnect()) {
                                        Slog.e(BluetoothManagerService.TAG, "IBluetooth.enableNoAutoConnect() returned false");
                                    }
                                } else if (!BluetoothManagerService.this.mBluetooth.enable()) {
                                    Slog.e(BluetoothManagerService.TAG, "IBluetooth.enable() returned false");
                                }
                            } catch (RemoteException e22) {
                                Slog.e(BluetoothManagerService.TAG, "Unable to call enable()", e22);
                            }
                            BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                            if (!BluetoothManagerService.this.mEnable) {
                                BluetoothManagerService.this.waitForMonitoredOnOff(true, false);
                                BluetoothManagerService.this.handleDisable();
                                BluetoothManagerService.this.waitForMonitoredOnOff(false, false);
                                break;
                            }
                        }
                        BluetoothManagerService.this.mBluetoothGatt = IBluetoothGatt.Stub.asInterface(Binder.allowBlocking(service));
                        BluetoothManagerService.this.onBluetoothGattServiceUp();
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                        break;
                    } catch (Throwable re) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to register BluetoothCallback", re);
                    } catch (Throwable th) {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    }
                    break;
                case 41:
                    Slog.e(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED(" + msg.arg1 + ")");
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (msg.arg1 != 1) {
                            if (msg.arg1 != 2) {
                                Slog.e(BluetoothManagerService.TAG, "Unknown argument for service disconnect!");
                                BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                                break;
                            }
                            BluetoothManagerService.this.mBluetoothGatt = null;
                            BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                            break;
                        } else if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.mBluetooth = null;
                            BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                            BluetoothManagerService.this.addCrashLog();
                            BluetoothManagerService.this.addActiveLog(BluetoothManagerService.REASON_UNEXPECTED, false);
                            if (BluetoothManagerService.this.mEnable) {
                                BluetoothManagerService.this.mEnable = false;
                                BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(42), 200);
                            }
                            BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                            if (BluetoothManagerService.this.mState == 11 || BluetoothManagerService.this.mState == 12) {
                                BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                                BluetoothManagerService.this.mState = 13;
                            }
                            if (BluetoothManagerService.this.mState == 13) {
                                BluetoothManagerService.this.bluetoothStateChangeHandler(13, 10);
                            }
                            BluetoothManagerService.this.mHandler.removeMessages(60);
                            BluetoothManagerService.this.mState = 10;
                            break;
                        } else {
                            break;
                        }
                    } finally {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    }
                case 42:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_RESTART_BLUETOOTH_SERVICE");
                    BluetoothManagerService.this.mEnable = true;
                    BluetoothManagerService.this.addActiveLog(BluetoothManagerService.REASON_RESTARTED, true);
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                    break;
                case 60:
                    int prevState = msg.arg1;
                    int newState = msg.arg2;
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_STATE_CHANGE: " + BluetoothAdapter.nameForState(prevState) + " > " + BluetoothAdapter.nameForState(newState));
                    BluetoothManagerService.this.mState = newState;
                    BluetoothManagerService.this.bluetoothStateChangeHandler(prevState, newState);
                    if (prevState == 14 && newState == 10 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                        BluetoothManagerService.this.recoverBluetoothServiceFromError(false);
                    }
                    if (prevState == 11 && newState == 10 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                        BluetoothManagerService.this.persistBluetoothSetting(0);
                    }
                    if (prevState == 11 && newState == 15 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                        BluetoothManagerService.this.recoverBluetoothServiceFromError(true);
                    }
                    if (prevState == 16 && newState == 10 && BluetoothManagerService.this.mEnable) {
                        Slog.d(BluetoothManagerService.TAG, "Entering STATE_OFF but mEnabled is true; restarting.");
                        BluetoothManagerService.this.waitForMonitoredOnOff(false, true);
                        BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(42), 400);
                    }
                    if ((newState == 12 || newState == 15) && BluetoothManagerService.this.mErrorRecoveryRetryCounter != 0) {
                        Slog.w(BluetoothManagerService.TAG, "bluetooth is recovered from error");
                        BluetoothManagerService.this.mErrorRecoveryRetryCounter = 0;
                        break;
                    }
                case 100:
                    Slog.e(BluetoothManagerService.TAG, "MESSAGE_TIMEOUT_BIND");
                    BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                    BluetoothManagerService.this.mBinding = false;
                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    break;
                case 101:
                    Slog.e(BluetoothManagerService.TAG, "MESSAGE_TIMEOUT_UNBIND");
                    BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                    BluetoothManagerService.this.mUnbinding = false;
                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    break;
                case 200:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_GET_NAME_AND_ADDRESS");
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (BluetoothManagerService.this.mBluetooth == null && (BluetoothManagerService.this.mBinding ^ 1) != 0) {
                            Slog.d(BluetoothManagerService.TAG, "Binding to service to get name and address");
                            this.mGetNameAddressOnly = true;
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(100), 3000);
                            if (BluetoothManagerService.this.doBind(new Intent(IBluetooth.class.getName()), BluetoothManagerService.this.mConnection, 65, UserHandle.CURRENT)) {
                                BluetoothManagerService.this.mBinding = true;
                            } else {
                                BluetoothManagerService.this.mHandler.removeMessages(100);
                            }
                        } else if (BluetoothManagerService.this.mBluetooth != null) {
                            Message saveMsg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS);
                            saveMsg.arg1 = 0;
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.mHandler.sendMessage(saveMsg);
                            } else {
                                BluetoothManagerService.this.mHandler.sendMessageDelayed(saveMsg, 500);
                            }
                        }
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                        break;
                    } catch (Throwable th2) {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    }
                    break;
                case BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS /*201*/:
                    boolean unbind = false;
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_SAVE_NAME_AND_ADDRESS");
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (!(BluetoothManagerService.this.mEnable || BluetoothManagerService.this.mBluetooth == null)) {
                            BluetoothManagerService.this.mBluetooth.enable();
                        }
                    } catch (RemoteException e222) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to call enable()", e222);
                    } catch (Throwable th3) {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    }
                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    if (BluetoothManagerService.this.mBluetooth != null) {
                        BluetoothManagerService.this.waitForBleOn();
                    }
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            String str2 = null;
                            String address = null;
                            str2 = BluetoothManagerService.this.mBluetooth.getName();
                            address = BluetoothManagerService.this.mBluetooth.getAddress();
                            if (str2 != null && address != null) {
                                BluetoothManagerService.this.storeNameAndAddress(str2, address);
                                if (this.mGetNameAddressOnly) {
                                    unbind = true;
                                }
                            } else if (msg.arg1 < 3) {
                                Message retryMsg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS);
                                retryMsg.arg1 = msg.arg1 + 1;
                                Slog.d(BluetoothManagerService.TAG, "Retrying name/address remote retrieval and save.....Retry count =" + retryMsg.arg1);
                                BluetoothManagerService.this.mHandler.sendMessageDelayed(retryMsg, 500);
                            } else {
                                Slog.w(BluetoothManagerService.TAG, "Maximum name/address remoteretrieval retry exceeded");
                                if (this.mGetNameAddressOnly) {
                                    unbind = true;
                                }
                            }
                            if (!BluetoothManagerService.this.mEnable) {
                                try {
                                    BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                                } catch (RemoteException e2222) {
                                    Slog.e(BluetoothManagerService.TAG, "Unable to call disable()", e2222);
                                }
                            }
                        } else {
                            BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(200));
                        }
                    } catch (Throwable re2) {
                        Slog.e(BluetoothManagerService.TAG, "", re2);
                    } catch (Throwable th4) {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    }
                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    if (!(BluetoothManagerService.this.mEnable || BluetoothManagerService.this.mBluetooth == null)) {
                        BluetoothManagerService.this.waitForOnOff(false, true);
                    }
                    if (unbind) {
                        BluetoothManagerService.this.unbindAndFinish();
                    }
                    this.mGetNameAddressOnly = false;
                    break;
                case 300:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_USER_SWITCHED");
                    BluetoothManagerService.this.mHandler.removeMessages(300);
                    if (BluetoothManagerService.this.mBluetooth == null || !BluetoothManagerService.this.isEnabled()) {
                        if (BluetoothManagerService.this.mBinding || BluetoothManagerService.this.mBluetooth != null) {
                            Message userMsg = BluetoothManagerService.this.mHandler.obtainMessage(300);
                            userMsg.arg2 = msg.arg2 + 1;
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(userMsg, 200);
                            Slog.d(BluetoothManagerService.TAG, "Retry MESSAGE_USER_SWITCHED " + userMsg.arg2);
                            break;
                        }
                    }
                    try {
                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.mBluetooth.unregisterCallback(BluetoothManagerService.this.mBluetoothCallback);
                        }
                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    } catch (Throwable re22) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to unregister", re22);
                    } catch (Throwable th5) {
                        break;
                    }
                    if (BluetoothManagerService.this.mState == 13) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 10);
                        BluetoothManagerService.this.mState = 10;
                    }
                    if (BluetoothManagerService.this.mState == 10) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 11);
                        BluetoothManagerService.this.mState = 11;
                    }
                    BluetoothManagerService.this.waitForMonitoredOnOff(true, false);
                    if (BluetoothManagerService.this.mState == 11) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 12);
                    }
                    BluetoothManagerService.this.unbindAllBluetoothProfileServices();
                    BluetoothManagerService.this.addActiveLog(BluetoothManagerService.REASON_USER_SWITCH, false);
                    BluetoothManagerService.this.clearBleApps();
                    BluetoothManagerService.this.handleDisable();
                    BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                    boolean didDisableTimeout = BluetoothManagerService.this.waitForMonitoredOnOff(false, true) ^ 1;
                    BluetoothManagerService.this.bluetoothStateChangeHandler(13, 10);
                    BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                    if (!didDisableTimeout) {
                        try {
                            BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.mBluetooth = null;
                                BluetoothManagerService.this.mContext.unbindService(BluetoothManagerService.this.mConnection);
                            }
                            BluetoothManagerService.this.mBluetoothGatt = null;
                        } finally {
                            BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                        }
                    }
                    if (didDisableTimeout) {
                        SystemClock.sleep(3000);
                        BluetoothManagerService.this.mHandler.removeMessages(41);
                    } else {
                        SystemClock.sleep(100);
                    }
                    BluetoothManagerService.this.mHandler.removeMessages(60);
                    BluetoothManagerService.this.mState = 10;
                    BluetoothManagerService.this.addActiveLog(BluetoothManagerService.REASON_USER_SWITCH, true);
                    BluetoothManagerService.this.mEnable = true;
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                    break;
                    break;
                case BluetoothManagerService.MESSAGE_USER_UNLOCKED /*301*/:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_USER_UNLOCKED");
                    BluetoothManagerService.this.mHandler.removeMessages(300);
                    if (BluetoothManagerService.this.mEnable && (BluetoothManagerService.this.mBinding ^ 1) != 0 && BluetoothManagerService.this.mBluetooth == null) {
                        Slog.d(BluetoothManagerService.TAG, "Enabled but not bound; retrying after unlock");
                        BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                        break;
                    }
                case 400:
                    psc = (ProfileServiceConnections) BluetoothManagerService.this.mProfileServices.get(new Integer(msg.arg1));
                    if (psc != null) {
                        psc.addProxy(msg.obj);
                        break;
                    }
                    break;
                case 401:
                    psc = (ProfileServiceConnections) msg.obj;
                    removeMessages(401, msg.obj);
                    if (psc != null) {
                        psc.bindService();
                        break;
                    }
                    break;
                case 500:
                    try {
                        if (msg.arg1 != 0 || !BluetoothManagerService.this.mEnable) {
                            if (msg.arg1 == 1 && (BluetoothManagerService.this.mEnable ^ 1) != 0) {
                                Slog.d(BluetoothManagerService.TAG, "Restore Bluetooth state to enabled");
                                BluetoothManagerService.this.enable(BluetoothManagerService.REASON_RESTORE_USER_SETTING);
                                break;
                            }
                        }
                        Slog.d(BluetoothManagerService.TAG, "Restore Bluetooth state to disabled");
                        BluetoothManagerService.this.disable(BluetoothManagerService.REASON_RESTORE_USER_SETTING, true);
                        break;
                    } catch (RemoteException e22222) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to change Bluetooth On setting", e22222);
                        break;
                    }
                    break;
            }
            return;
            BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
        }
    }

    private class BluetoothServiceConnection implements ServiceConnection {
        /* synthetic */ BluetoothServiceConnection(BluetoothManagerService this$0, BluetoothServiceConnection -this1) {
            this();
        }

        private BluetoothServiceConnection() {
        }

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            String name = componentName.getClassName();
            Slog.d(BluetoothManagerService.TAG, "BluetoothServiceConnection: " + name);
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(40);
            if (name.equals("com.android.bluetooth.btservice.AdapterService")) {
                msg.arg1 = 1;
            } else if (name.equals("com.android.bluetooth.gatt.GattService")) {
                msg.arg1 = 2;
            } else {
                Slog.e(BluetoothManagerService.TAG, "Unknown service connected: " + name);
                return;
            }
            msg.obj = service;
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }

        public void onServiceDisconnected(ComponentName componentName) {
            String name = componentName.getClassName();
            Slog.d(BluetoothManagerService.TAG, "BluetoothServiceConnection, disconnected: " + name);
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(41);
            if (name.equals("com.android.bluetooth.btservice.AdapterService")) {
                msg.arg1 = 1;
            } else if (name.equals("com.android.bluetooth.gatt.GattService")) {
                msg.arg1 = 2;
            } else {
                Slog.e(BluetoothManagerService.TAG, "Unknown service disconnected: " + name);
                return;
            }
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }
    }

    class ClientDeathRecipient implements DeathRecipient {
        private String mPackageName;

        public ClientDeathRecipient(String packageName) {
            this.mPackageName = packageName;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void binderDied() {
            Slog.d(BluetoothManagerService.TAG, "Binder is dead - unregister " + this.mPackageName);
            if (!BluetoothManagerService.this.isBleAppPresent()) {
                Slog.d(BluetoothManagerService.TAG, "Disabling LE only mode after application crash");
                try {
                    BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                    if (BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mBluetooth.getState() == 15) {
                        BluetoothManagerService.this.mEnable = false;
                        BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                    }
                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                } catch (RemoteException e) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to call onBrEdrDown", e);
                } catch (Throwable th) {
                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                }
            }
        }

        public String getPackageName() {
            return this.mPackageName;
        }
    }

    private final class ProfileServiceConnections implements ServiceConnection, DeathRecipient {
        ComponentName mClassName = null;
        Intent mIntent;
        boolean mInvokingProxyCallbacks = false;
        final RemoteCallbackList<IBluetoothProfileServiceConnection> mProxies = new RemoteCallbackList();
        IBinder mService = null;

        ProfileServiceConnections(Intent intent) {
            this.mIntent = intent;
        }

        private boolean bindService() {
            if (this.mIntent != null && this.mService == null && BluetoothManagerService.this.doBind(this.mIntent, this, 0, UserHandle.CURRENT_OR_SELF)) {
                Message msg = BluetoothManagerService.this.mHandler.obtainMessage(401);
                msg.obj = this;
                BluetoothManagerService.this.mHandler.sendMessageDelayed(msg, 3000);
                return true;
            }
            Slog.w(BluetoothManagerService.TAG, "Unable to bind with intent: " + this.mIntent);
            return false;
        }

        private void addProxy(IBluetoothProfileServiceConnection proxy) {
            this.mProxies.register(proxy);
            if (this.mService != null) {
                try {
                    proxy.onServiceConnected(this.mClassName, this.mService);
                } catch (RemoteException e) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e);
                }
            } else if (!BluetoothManagerService.this.mHandler.hasMessages(401, this)) {
                Message msg = BluetoothManagerService.this.mHandler.obtainMessage(401);
                msg.obj = this;
                BluetoothManagerService.this.mHandler.sendMessage(msg);
            }
        }

        private void removeProxy(IBluetoothProfileServiceConnection proxy) {
            if (proxy == null) {
                Slog.w(BluetoothManagerService.TAG, "Trying to remove a null proxy");
            } else if (this.mProxies.unregister(proxy)) {
                try {
                    proxy.onServiceDisconnected(this.mClassName);
                } catch (RemoteException e) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to disconnect proxy", e);
                }
            }
        }

        private void removeAllProxies() {
            onServiceDisconnected(this.mClassName);
            this.mProxies.kill();
        }

        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothManagerService.this.mHandler.removeMessages(401, this);
            this.mService = service;
            this.mClassName = className;
            try {
                this.mService.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.e(BluetoothManagerService.TAG, "Unable to linkToDeath", e);
            }
            if (this.mInvokingProxyCallbacks) {
                Slog.e(BluetoothManagerService.TAG, "Proxy callbacks already in progress.");
                return;
            }
            this.mInvokingProxyCallbacks = true;
            int n = this.mProxies.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    ((IBluetoothProfileServiceConnection) this.mProxies.getBroadcastItem(i)).onServiceConnected(className, service);
                } catch (RemoteException e2) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e2);
                } catch (Throwable th) {
                    this.mProxies.finishBroadcast();
                    this.mInvokingProxyCallbacks = false;
                }
            }
            this.mProxies.finishBroadcast();
            this.mInvokingProxyCallbacks = false;
        }

        public void onServiceDisconnected(ComponentName className) {
            if (this.mService != null && this.mClassName != null) {
                try {
                    synchronized (this.mClassName) {
                        this.mService.unlinkToDeath(this, 0);
                        this.mService = null;
                        this.mClassName = null;
                    }
                    if (this.mInvokingProxyCallbacks) {
                        Slog.e(BluetoothManagerService.TAG, "Proxy callbacks already in progress.");
                        return;
                    }
                    this.mInvokingProxyCallbacks = true;
                    int n = this.mProxies.beginBroadcast();
                    for (int i = 0; i < n; i++) {
                        try {
                            ((IBluetoothProfileServiceConnection) this.mProxies.getBroadcastItem(i)).onServiceDisconnected(className);
                        } catch (RemoteException e) {
                            Slog.e(BluetoothManagerService.TAG, "Unable to disconnect from proxy", e);
                        } catch (Throwable th) {
                            this.mProxies.finishBroadcast();
                            this.mInvokingProxyCallbacks = false;
                        }
                    }
                    this.mProxies.finishBroadcast();
                    this.mInvokingProxyCallbacks = false;
                } catch (NullPointerException npe) {
                    Slog.e(BluetoothManagerService.TAG, "NullPointerException for synchronized(mClassName)", npe);
                } catch (NoSuchElementException nsee) {
                    Slog.e(BluetoothManagerService.TAG, "onServiceDisconnected(), Unable to unlinkToDeath ", nsee);
                }
            }
        }

        public void binderDied() {
            Slog.w(BluetoothManagerService.TAG, "Profile service for profile: " + this.mClassName + " died.");
            onServiceDisconnected(this.mClassName);
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(401);
            msg.obj = this;
            BluetoothManagerService.this.mHandler.sendMessageDelayed(msg, 3000);
        }
    }

    private CharSequence timeToLog(long timestamp) {
        return DateFormat.format("MM-dd HH:mm:ss", timestamp);
    }

    BluetoothManagerService(Context context) {
        this.mContext = context;
        this.mPermissionReviewRequired = context.getResources().getBoolean(17956992);
        this.mActiveLogs = new LinkedList();
        this.mCrashTimestamps = new LinkedList();
        this.mCrashes = 0;
        this.mBluetooth = null;
        this.mBluetoothBinder = null;
        this.mBluetoothGatt = null;
        this.mBinding = false;
        this.mUnbinding = false;
        this.mEnable = false;
        this.mState = 10;
        this.mQuietEnableExternal = false;
        this.mEnableExternal = false;
        this.mAddress = null;
        this.mName = null;
        this.mErrorRecoveryRetryCounter = 0;
        this.mContentResolver = context.getContentResolver();
        registerForBleScanModeChange();
        this.mCallbacks = new RemoteCallbackList();
        this.mStateChangeCallbacks = new RemoteCallbackList();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.BLUETOOTH_ADDRESS_CHANGED");
        filter.addAction("android.os.action.SETTING_RESTORED");
        filter.setPriority(1000);
        this.mContext.registerReceiver(this.mReceiver, filter);
        loadStoredNameAndAddress();
        if (isBluetoothPersistedStateOn()) {
            Slog.d(TAG, "Startup: Bluetooth persisted state is ON.");
            this.mEnableExternal = true;
        }
        String airplaneModeRadios = Global.getString(this.mContentResolver, "airplane_mode_radios");
        if (airplaneModeRadios == null || airplaneModeRadios.contains(OppoProcessManager.RESUME_REASON_BLUETOOTH_STR)) {
            this.mContentResolver.registerContentObserver(Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        }
        int systemUiUid = -1;
        try {
            systemUiUid = this.mContext.getPackageManager().getPackageUidAsUser(OppoFreeFormManagerService.FREEFORM_CALLER_PKG, DumpState.DUMP_DEXOPT, 0);
        } catch (NameNotFoundException e) {
            Slog.w(TAG, "Unable to resolve SystemUI's UID.", e);
        }
        this.mSystemUiUid = systemUiUid;
    }

    private final boolean isAirplaneModeOn() {
        return Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private final boolean isBluetoothPersistedStateOn() {
        int state = Global.getInt(this.mContentResolver, "bluetooth_on", -1);
        Slog.d(TAG, "Bluetooth persisted state: " + state);
        if (state != 0) {
            return true;
        }
        return false;
    }

    private final boolean isBluetoothPersistedStateOnBluetooth() {
        return Global.getInt(this.mContentResolver, "bluetooth_on", 1) == 1;
    }

    private void persistBluetoothSetting(int value) {
        Slog.d(TAG, "Persisting Bluetooth Setting: " + value);
        long callingIdentity = Binder.clearCallingIdentity();
        Global.putInt(this.mContext.getContentResolver(), "bluetooth_on", value);
        Binder.restoreCallingIdentity(callingIdentity);
    }

    private boolean isNameAndAddressSet() {
        return this.mName != null && this.mAddress != null && this.mName.length() > 0 && this.mAddress.length() > 0;
    }

    private void loadStoredNameAndAddress() {
        Slog.d(TAG, "Loading stored name and address");
        if (this.mContext.getResources().getBoolean(17956897) && Secure.getInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 0) == 0) {
            Slog.d(TAG, "invalid bluetooth name and address stored");
            return;
        }
        this.mName = Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME);
        this.mAddress = Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS);
        Slog.d(TAG, "Stored bluetooth Name=" + this.mName + ",Address=" + this.mAddress);
    }

    private void storeNameAndAddress(String name, String address) {
        if (name != null) {
            Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME, name);
            this.mName = name;
            Slog.d(TAG, "Stored Bluetooth name: " + Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME));
        }
        if (address != null) {
            Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS, address);
            this.mAddress = address;
            Slog.d(TAG, "Stored Bluetoothaddress: " + Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS));
        }
        if (name != null && address != null) {
            Secure.putInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 1);
        }
    }

    public IBluetooth registerAdapter(IBluetoothManagerCallback callback) {
        if (callback == null) {
            Slog.w(TAG, "Callback is null in registerAdapter");
            return null;
        }
        Message msg = this.mHandler.obtainMessage(20);
        msg.obj = callback;
        msg.arg1 = Binder.getCallingPid();
        this.mHandler.sendMessage(msg);
        return this.mBluetooth;
    }

    public void unregisterAdapter(IBluetoothManagerCallback callback) {
        if (callback == null) {
            Slog.w(TAG, "Callback is null in unregisterAdapter");
            return;
        }
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Message msg = this.mHandler.obtainMessage(21);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
    }

    public void registerStateChangeCallback(IBluetoothStateChangeCallback callback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (callback == null) {
            Slog.w(TAG, "registerStateChangeCallback: Callback is null!");
            return;
        }
        Message msg = this.mHandler.obtainMessage(30);
        msg.obj = callback;
        msg.arg1 = Binder.getCallingPid();
        this.mHandler.sendMessage(msg);
    }

    public void unregisterStateChangeCallback(IBluetoothStateChangeCallback callback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (callback == null) {
            Slog.w(TAG, "unregisterStateChangeCallback: Callback is null!");
            return;
        }
        Message msg = this.mHandler.obtainMessage(31);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
    }

    public boolean isEnabled() {
        if (Binder.getCallingUid() == 1000 || (checkIfCallerIsForegroundUser() ^ 1) == 0) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    boolean isEnabled = this.mBluetooth.isEnabled();
                    return isEnabled;
                }
                this.mBluetoothLock.readLock().unlock();
                return false;
            } catch (RemoteException e) {
                Slog.e(TAG, "isEnabled()", e);
            } finally {
                this.mBluetoothLock.readLock().unlock();
            }
        } else {
            Slog.w(TAG, "isEnabled(): not allowed for non-active and non system user");
            return false;
        }
    }

    public int getState() {
        if (Binder.getCallingUid() == 1000 || (checkIfCallerIsForegroundUser() ^ 1) == 0) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    int state = this.mBluetooth.getState();
                    return state;
                }
                this.mBluetoothLock.readLock().unlock();
                return 10;
            } catch (RemoteException e) {
                Slog.e(TAG, "getState()", e);
            } finally {
                this.mBluetoothLock.readLock().unlock();
            }
        } else {
            Slog.w(TAG, "getState(): report OFF for non-active and non system user");
            return 10;
        }
    }

    public boolean isBleScanAlwaysAvailable() {
        boolean z = false;
        if (isAirplaneModeOn() && (this.mEnable ^ 1) != 0) {
            return false;
        }
        try {
            if (Global.getInt(this.mContentResolver, "ble_scan_always_enabled") != 0) {
                z = true;
            }
            return z;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    private void registerForBleScanModeChange() {
        this.mContentResolver.registerContentObserver(Global.getUriFor("ble_scan_always_enabled"), false, new ContentObserver(null) {
            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onChange(boolean selfChange) {
                if (!BluetoothManagerService.this.isBleScanAlwaysAvailable()) {
                    BluetoothManagerService.this.disableBleScanMode();
                    BluetoothManagerService.this.clearBleApps();
                    try {
                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                        }
                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    } catch (RemoteException e) {
                        Slog.e(BluetoothManagerService.TAG, "error when disabling bluetooth", e);
                    } catch (Throwable th) {
                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    }
                }
            }
        });
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void disableBleScanMode() {
        try {
            this.mBluetoothLock.writeLock().lock();
            if (!(this.mBluetooth == null || this.mBluetooth.getState() == 12 || (isBluetoothPersistedStateOnBluetooth() ^ 1) == 0)) {
                Slog.d(TAG, "Reseting the mEnable flag for clean disable");
                this.mEnable = false;
            }
            this.mBluetoothLock.writeLock().unlock();
        } catch (RemoteException e) {
            Slog.e(TAG, "getState()", e);
        } catch (Throwable th) {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int updateBleAppCount(IBinder token, boolean enable, String packageName) {
        ClientDeathRecipient r = (ClientDeathRecipient) this.mBleApps.get(token);
        int st = 10;
        if (r == null && enable) {
            ClientDeathRecipient deathRec = new ClientDeathRecipient(packageName);
            try {
                token.linkToDeath(deathRec, 0);
                this.mBleApps.put(token, deathRec);
                Slog.d(TAG, "Registered for death of " + packageName);
            } catch (RemoteException e) {
                throw new IllegalArgumentException("BLE app (" + packageName + ") already dead!");
            }
        } else if (!(enable || r == null)) {
            try {
                token.unlinkToDeath(r, 0);
            } catch (NoSuchElementException nsee) {
                Slog.e(TAG, "updateBleAppCount(), Unable to unlinkToDeath", nsee);
            }
            this.mBleApps.remove(token);
            Slog.d(TAG, "Unregistered for death of " + packageName);
        }
        int appCount = this.mBleApps.size();
        Slog.d(TAG, appCount + " registered Ble Apps");
        if (appCount == 0 && this.mEnable) {
            disableBleScanMode();
        }
        if (appCount == 0) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    st = this.mBluetooth.getState();
                }
                if (!this.mEnableExternal || st == 15) {
                    Slog.d(TAG, "Move to BT state OFF");
                    sendBrEdrDownCallback();
                }
                this.mBluetoothLock.readLock().unlock();
            } catch (RemoteException e2) {
                Slog.e(TAG, "", e2);
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
            }
        }
        return appCount;
    }

    private void clearBleApps() {
        this.mBleApps.clear();
    }

    public boolean isBleAppPresent() {
        Slog.d(TAG, "isBleAppPresent() count: " + this.mBleApps.size());
        if (this.mBleApps.size() > 0) {
            return true;
        }
        return false;
    }

    private void onBluetoothGattServiceUp() {
        Slog.d(TAG, "BluetoothGatt Service is Up");
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth == null) {
                Slog.w(TAG, "onBluetoothServiceUp: mBluetooth is null!");
                return;
            }
            int st = this.mBluetooth.getState();
            if (st != 15) {
                Slog.v(TAG, "onBluetoothServiceUp: state isn't BLE_ON: " + BluetoothAdapter.nameForState(st));
                this.mBluetoothLock.readLock().unlock();
                return;
            }
            if (isBluetoothPersistedStateOnBluetooth() || (isBleAppPresent() ^ 1) != 0) {
                this.mBluetooth.onLeServiceUp();
                persistBluetoothSetting(1);
            }
            this.mBluetoothLock.readLock().unlock();
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call onServiceUp", e);
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void sendBrEdrDownCallback() {
        Slog.d(TAG, "Calling sendBrEdrDownCallback callbacks");
        if (this.mBluetooth == null) {
            Slog.w(TAG, "Bluetooth handle is null");
            return;
        }
        if (isBleAppPresent()) {
            try {
                this.mBluetoothGatt.unregAll();
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to disconnect all apps.", e);
            }
        } else {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    this.mBluetooth.onBrEdrDown();
                }
                this.mBluetoothLock.readLock().unlock();
            } catch (RemoteException e2) {
                Slog.e(TAG, "Call to onBrEdrDown() failed.", e2);
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
            }
        }
    }

    public boolean enableNoAutoConnect(String packageName) {
        if (isBluetoothDisallowed()) {
            Slog.d(TAG, "enableNoAutoConnect(): not enabling - bluetooth disallowed");
            return false;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        Slog.d(TAG, "enableNoAutoConnect():  mBluetooth =" + this.mBluetooth + " mBinding = " + this.mBinding);
        int callingAppId = UserHandle.getAppId(Binder.getCallingUid());
        String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        Slog.d(TAG, "callingApp = " + callingApp);
        if (callingAppId == UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE || (PACKAGE_NAME_OSHARE.equals(callingApp) ^ 1) == 0) {
            synchronized (this.mReceiver) {
                this.mQuietEnableExternal = true;
                this.mEnableExternal = true;
                sendEnableMsg(true, packageName);
            }
            return true;
        }
        throw new SecurityException("no permission to enable Bluetooth quietly");
    }

    private boolean isStrictOpEnable() {
        return SystemProperties.getBoolean("persist.sys.strict_op_enable", false);
    }

    public boolean enable(String packageName) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        boolean callerSystem = UserHandle.getAppId(callingUid) == 1000;
        if (isBluetoothDisallowed()) {
            Slog.d(TAG, "enable(): not enabling - bluetooth disallowed");
            return false;
        }
        if (!callerSystem) {
            if (checkIfCallerIsForegroundUser()) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
                if (!isEnabled() && this.mPermissionReviewRequired && startConsentUiIfNeeded(packageName, callingUid, "android.bluetooth.adapter.action.REQUEST_ENABLE")) {
                    return false;
                }
            }
            Slog.w(TAG, "enable(): not allowed for non-active and non system user");
            return false;
        }
        if (isStrictOpEnable()) {
            AppOpsManager mAppOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
            String packages = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (Binder.getCallingUid() >= 10000 && packages.indexOf("android.uid.systemui") != 0 && packages.indexOf("android.uid.system") != 0 && mAppOpsManager.noteOp(71, Binder.getCallingUid(), packages) == 1) {
                return false;
            }
        }
        Slog.d(TAG, "enable(" + packageName + "):  mBluetooth =" + this.mBluetooth + " mBinding = " + this.mBinding + " mState = " + BluetoothAdapter.nameForState(this.mState));
        if (this.mHyp == null) {
            this.mHyp = Hypnus.getHypnus();
        }
        if (this.mHyp != null) {
            this.mHyp.hypnusSetAction(12, 2500);
        }
        synchronized (this.mReceiver) {
            this.mQuietEnableExternal = false;
            this.mEnableExternal = true;
            sendEnableMsg(false, packageName);
        }
        Slog.d(TAG, "enable returning");
        return true;
    }

    public boolean disable(String packageName, boolean persist) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        if (!(UserHandle.getAppId(callingUid) == 1000)) {
            if (checkIfCallerIsForegroundUser()) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
                if (isEnabled() && this.mPermissionReviewRequired && startConsentUiIfNeeded(packageName, callingUid, "android.bluetooth.adapter.action.REQUEST_DISABLE")) {
                    return false;
                }
            }
            Slog.w(TAG, "disable(): not allowed for non-active and non system user");
            return false;
        }
        Slog.d(TAG, "disable(): mBluetooth = " + this.mBluetooth + " mBinding = " + this.mBinding);
        synchronized (this.mReceiver) {
            if (persist) {
                persistBluetoothSetting(0);
                this.mEnableExternal = false;
                sendDisableMsg(packageName);
            } else {
                synchronized (this) {
                    clearBleApps();
                }
                try {
                    this.mBluetoothLock.readLock().lock();
                    this.mEnableExternal = false;
                    if (this.mBluetooth != null) {
                        if (this.mBluetooth.getState() == 15) {
                            this.mEnable = false;
                            this.mBluetooth.onBrEdrDown();
                        } else {
                            sendDisableMsg(REASON_SYSTEM_BOOT);
                        }
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to initiate disable", e);
                } finally {
                    this.mBluetoothLock.readLock().unlock();
                }
            }
        }
        return true;
    }

    private boolean startConsentUiIfNeeded(String packageName, int callingUid, String intentAction) throws RemoteException {
        try {
            if (this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 268435456, UserHandle.getUserId(callingUid)).uid != callingUid) {
                throw new SecurityException("Package " + callingUid + " not in uid " + callingUid);
            }
            Intent intent = new Intent(intentAction);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", packageName);
            intent.setFlags(276824064);
            try {
                this.mContext.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "Intent to handle action " + intentAction + " missing");
                return false;
            }
        } catch (NameNotFoundException e2) {
            throw new RemoteException(e2.getMessage());
        }
    }

    public void unbindAndFinish() {
        Slog.d(TAG, "unbindAndFinish(): " + this.mBluetooth + " mBinding = " + this.mBinding + " mUnbinding = " + this.mUnbinding);
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mUnbinding) {
                this.mBluetoothLock.writeLock().unlock();
                return;
            }
            this.mUnbinding = true;
            this.mHandler.removeMessages(60);
            this.mHandler.removeMessages(401);
            if (this.mBluetooth != null) {
                this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
                this.mBluetoothBinder = null;
                this.mBluetooth = null;
                this.mContext.unbindService(this.mConnection);
                this.mUnbinding = false;
                this.mBinding = false;
            } else {
                this.mUnbinding = false;
            }
            this.mBluetoothGatt = null;
            this.mBluetoothLock.writeLock().unlock();
        } catch (RemoteException re) {
            Slog.e(TAG, "Unable to unregister BluetoothCallback", re);
        } catch (Throwable th) {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    public IBluetoothGatt getBluetoothGatt() {
        return this.mBluetoothGatt;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean bindBluetoothProfileService(int bluetoothProfile, IBluetoothProfileServiceConnection proxy) {
        if (this.mEnable) {
            synchronized (this.mProfileServices) {
                if (((ProfileServiceConnections) this.mProfileServices.get(new Integer(bluetoothProfile))) == null) {
                    Slog.d(TAG, "Creating new ProfileServiceConnections object for profile: " + bluetoothProfile);
                    if (bluetoothProfile != 1) {
                        return false;
                    }
                    ProfileServiceConnections psc = new ProfileServiceConnections(new Intent(IBluetoothHeadset.class.getName()));
                    if (psc.bindService()) {
                        this.mProfileServices.put(new Integer(bluetoothProfile), psc);
                    } else {
                        return false;
                    }
                }
            }
        }
        Slog.d(TAG, "Trying to bind to profile: " + bluetoothProfile + ", while Bluetooth was disabled");
        return false;
    }

    public void unbindBluetoothProfileService(int bluetoothProfile, IBluetoothProfileServiceConnection proxy) {
        synchronized (this.mProfileServices) {
            ProfileServiceConnections psc = (ProfileServiceConnections) this.mProfileServices.get(new Integer(bluetoothProfile));
            if (psc == null) {
                return;
            }
            psc.removeProxy(proxy);
        }
    }

    private void unbindAllBluetoothProfileServices() {
        synchronized (this.mProfileServices) {
            for (Integer i : this.mProfileServices.keySet()) {
                ProfileServiceConnections psc = (ProfileServiceConnections) this.mProfileServices.get(i);
                try {
                    this.mContext.unbindService(psc);
                } catch (IllegalArgumentException e) {
                    Slog.e(TAG, "Unable to unbind service with intent: " + psc.mIntent, e);
                }
                psc.removeAllProxies();
            }
            this.mProfileServices.clear();
        }
    }

    public void handleOnBootPhase() {
        Slog.d(TAG, "Bluetooth boot completed");
        ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).addUserRestrictionsListener(this.mUserRestrictionsListener);
        if (!isBluetoothDisallowed()) {
            if (this.mEnableExternal && isBluetoothPersistedStateOnBluetooth()) {
                Slog.d(TAG, "Auto-enabling Bluetooth.");
                sendEnableMsg(this.mQuietEnableExternal, REASON_SYSTEM_BOOT);
            } else if (!isNameAndAddressSet()) {
                Slog.d(TAG, "Getting adapter name and address");
                this.mHandler.sendMessage(this.mHandler.obtainMessage(200));
            }
        }
    }

    public void handleOnSwitchUser(int userHandle) {
        Slog.d(TAG, "User " + userHandle + " switched");
        this.mHandler.obtainMessage(300, userHandle, 0).sendToTarget();
    }

    public void handleOnUnlockUser(int userHandle) {
        Slog.d(TAG, "User " + userHandle + " unlocked");
        this.mHandler.obtainMessage(MESSAGE_USER_UNLOCKED, userHandle, 0).sendToTarget();
    }

    private void sendBluetoothStateCallback(boolean isUp) {
        int i;
        try {
            int n = this.mStateChangeCallbacks.beginBroadcast();
            Slog.d(TAG, "Broadcasting onBluetoothStateChange(" + isUp + ") to " + n + " receivers.");
            i = 0;
            while (i < n) {
                if (tryResumeProcess(((Integer) this.mStateChangeCallbacks.getBroadcastCookie(i)).intValue())) {
                    ((IBluetoothStateChangeCallback) this.mStateChangeCallbacks.getBroadcastItem(i)).onBluetoothStateChange(isUp);
                } else {
                    Slog.e(TAG, "sendBluetoothStateCallback() resume failed, don't call back!!! i = " + i);
                }
                i++;
            }
            this.mStateChangeCallbacks.finishBroadcast();
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call onBluetoothStateChange() on callback #" + i, e);
        } catch (SecurityException se) {
            Slog.e(TAG, "Unable to call onBluetoothStateChange() on callback #" + i, se);
        } catch (Throwable th) {
            this.mStateChangeCallbacks.finishBroadcast();
        }
    }

    private void sendBluetoothServiceUpCallback() {
        int i;
        try {
            int n = this.mCallbacks.beginBroadcast();
            Slog.d(TAG, "Broadcasting onBluetoothServiceUp() to " + n + " receivers.");
            i = 0;
            while (i < n) {
                if (tryResumeProcess(((Integer) this.mCallbacks.getBroadcastCookie(i)).intValue())) {
                    ((IBluetoothManagerCallback) this.mCallbacks.getBroadcastItem(i)).onBluetoothServiceUp(this.mBluetooth);
                } else {
                    Slog.e(TAG, "sendBluetoothServiceUpCallback() resume failed, don't call back!!! i = " + i);
                }
                i++;
            }
            this.mCallbacks.finishBroadcast();
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call onBluetoothServiceUp() on callback #" + i, e);
        } catch (SecurityException se) {
            Slog.e(TAG, "Unable to call onBluetoothServiceUp() on callback #" + i, se);
        } catch (Throwable th) {
            this.mCallbacks.finishBroadcast();
        }
    }

    private void sendBluetoothServiceDownCallback() {
        int i;
        try {
            int n = this.mCallbacks.beginBroadcast();
            Slog.d(TAG, "Broadcasting onBluetoothServiceDown() to " + n + " receivers.");
            i = 0;
            while (i < n) {
                if (tryResumeProcess(((Integer) this.mCallbacks.getBroadcastCookie(i)).intValue())) {
                    ((IBluetoothManagerCallback) this.mCallbacks.getBroadcastItem(i)).onBluetoothServiceDown();
                } else {
                    Slog.e(TAG, "sendBluetoothServiceDownCallback() resume failed, don't call back!!! i = " + i);
                }
                i++;
            }
            this.mCallbacks.finishBroadcast();
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call onBluetoothServiceDown() on callback #" + i, e);
        } catch (SecurityException se) {
            Slog.e(TAG, "Unable to call onBluetoothServiceDown() on callback #" + i, se);
        } catch (Throwable th) {
            this.mCallbacks.finishBroadcast();
        }
    }

    public String getAddress() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (Binder.getCallingUid() != 1000 && (checkIfCallerIsForegroundUser() ^ 1) != 0) {
            Slog.w(TAG, "getAddress(): not allowed for non-active and non system user");
            return null;
        } else if (this.mContext.checkCallingOrSelfPermission("android.permission.LOCAL_MAC_ADDRESS") != 0) {
            return "02:00:00:00:00:00";
        } else {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    String address = this.mBluetooth.getAddress();
                    return address;
                }
                this.mBluetoothLock.readLock().unlock();
                return this.mAddress;
            } catch (RemoteException e) {
                Slog.e(TAG, "getAddress(): Unable to retrieve address remotely. Returning cached address", e);
            } finally {
                this.mBluetoothLock.readLock().unlock();
            }
        }
    }

    public String getName() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (Binder.getCallingUid() == 1000 || (checkIfCallerIsForegroundUser() ^ 1) == 0) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    String name = this.mBluetooth.getName();
                    return name;
                }
                this.mBluetoothLock.readLock().unlock();
                return this.mName;
            } catch (RemoteException e) {
                Slog.e(TAG, "getName(): Unable to retrieve name remotely. Returning cached name", e);
            } finally {
                this.mBluetoothLock.readLock().unlock();
            }
        } else {
            Slog.w(TAG, "getName(): not allowed for non-active and non system user");
            return null;
        }
    }

    public boolean factoryReset() {
        if (!(UserHandle.getAppId(Binder.getCallingUid()) == 1000)) {
            if (checkIfCallerIsForegroundUser()) {
                this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED_PERM, "Need BLUETOOTH PRIVILEGED permission");
            } else {
                Slog.w(TAG, "factoryReset(): not allowed for non-active and non system user");
                return false;
            }
        }
        try {
            if (this.mBluetooth == null) {
                return true;
            }
            clearBleApps();
            return this.mBluetooth.factoryReset();
        } catch (RemoteException e) {
            Slog.e(TAG, "factoryReset(): Unable to do factoryReset.", e);
            return false;
        }
    }

    private void handleEnable(boolean quietMode) {
        this.mQuietEnable = quietMode;
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mBluetooth == null && (this.mBinding ^ 1) != 0) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100), 3000);
                if (doBind(new Intent(IBluetooth.class.getName()), this.mConnection, 65, UserHandle.CURRENT)) {
                    this.mBinding = true;
                } else {
                    this.mHandler.removeMessages(100);
                }
            } else if (this.mBluetooth != null) {
                if (this.mQuietEnable) {
                    if (!this.mBluetooth.enableNoAutoConnect()) {
                        Slog.e(TAG, "IBluetooth.enableNoAutoConnect() returned false");
                    }
                } else if (!this.mBluetooth.enable()) {
                    Slog.e(TAG, "IBluetooth.enable() returned false");
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call enable()", e);
        } catch (Throwable th) {
            this.mBluetoothLock.writeLock().unlock();
        }
        this.mBluetoothLock.writeLock().unlock();
    }

    boolean doBind(Intent intent, ServiceConnection conn, int flags, UserHandle user) {
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && (this.mContext.bindServiceAsUser(intent, conn, flags, user) ^ 1) == 0) {
            return true;
        }
        Slog.e(TAG, "Fail to bind to: " + intent);
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleDisable() {
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                Slog.d(TAG, "Sending off request.");
                if (!this.mBluetooth.disable()) {
                    Slog.e(TAG, "IBluetooth.disable() returned false");
                }
            }
            this.mBluetoothLock.readLock().unlock();
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call disable()", e);
        } catch (Throwable th) {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    private boolean checkIfCallerIsForegroundUser() {
        int callingUser = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        UserInfo ui = ((UserManager) this.mContext.getSystemService("user")).getProfileParent(callingUser);
        int parentUser = ui != null ? ui.id : -10000;
        int callingAppId = UserHandle.getAppId(callingUid);
        try {
            int foregroundUser = ActivityManager.getCurrentUser();
            boolean valid = (callingUser == foregroundUser || callingUser == OppoMultiAppManager.USER_ID || parentUser == foregroundUser || callingAppId == UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE) ? true : callingAppId == this.mSystemUiUid;
            if ((valid ^ 1) != 0) {
                Slog.d(TAG, "checkIfCallerIsForegroundUser: valid=" + valid + " callingUser=" + callingUser + " parentUser=" + parentUser + " foregroundUser=" + foregroundUser);
            }
            Binder.restoreCallingIdentity(callingIdentity);
            return valid;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private void sendBleStateChanged(int prevState, int newState) {
        Slog.d(TAG, "Sending BLE State Change: " + BluetoothAdapter.nameForState(prevState) + " > " + BluetoothAdapter.nameForState(newState));
        Intent intent = new Intent("android.bluetooth.adapter.action.BLE_STATE_CHANGED");
        intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", prevState);
        intent.putExtra("android.bluetooth.adapter.extra.STATE", newState);
        intent.addFlags(67108864);
        intent.addFlags(16777216);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
    }

    private void bluetoothStateChangeHandler(int prevState, int newState) {
        boolean isStandardBroadcast = true;
        if (prevState != newState) {
            if (newState == 15 || newState == 10) {
                boolean intermediate_off = prevState == 13 ? newState == 15 : false;
                if (newState == 10) {
                    Slog.d(TAG, "Bluetooth is complete send Service Down");
                    sendBluetoothServiceDownCallback();
                    unbindAndFinish();
                    sendBleStateChanged(prevState, newState);
                    isStandardBroadcast = false;
                } else if (!intermediate_off) {
                    Slog.d(TAG, "Bluetooth is in LE only mode");
                    if (this.mBluetoothGatt != null) {
                        Slog.d(TAG, "Calling BluetoothGattServiceUp");
                        onBluetoothGattServiceUp();
                    } else {
                        Slog.d(TAG, "Binding Bluetooth GATT service");
                        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
                            doBind(new Intent(IBluetoothGatt.class.getName()), this.mConnection, 65, UserHandle.CURRENT);
                        }
                    }
                    sendBleStateChanged(prevState, newState);
                    isStandardBroadcast = false;
                } else if (intermediate_off) {
                    Slog.d(TAG, "Intermediate off, back to LE only mode");
                    sendBleStateChanged(prevState, newState);
                    sendBluetoothStateCallback(false);
                    newState = 10;
                    sendBrEdrDownCallback();
                }
            } else if (newState == 12) {
                sendBluetoothStateCallback(newState == 12);
                sendBleStateChanged(prevState, newState);
            } else if (newState == 14 || newState == 16) {
                sendBleStateChanged(prevState, newState);
                isStandardBroadcast = false;
            } else if (newState == 11 || newState == 13) {
                sendBleStateChanged(prevState, newState);
            }
            if (isStandardBroadcast) {
                if (prevState == 15) {
                    prevState = 10;
                }
                Intent intent = new Intent("android.bluetooth.adapter.action.STATE_CHANGED");
                intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", prevState);
                intent.putExtra("android.bluetooth.adapter.extra.STATE", newState);
                intent.addFlags(67108864);
                intent.addFlags(16777216);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean waitForOnOff(boolean on, boolean off) {
        int i = 0;
        while (i < 16) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth == null) {
                    this.mBluetoothLock.readLock().unlock();
                    break;
                }
                if (on) {
                    if (this.mBluetooth.getState() == 12) {
                        this.mBluetoothLock.readLock().unlock();
                        return true;
                    }
                } else if (off) {
                    if (this.mBluetooth.getState() == 10) {
                        this.mBluetoothLock.readLock().unlock();
                        return true;
                    }
                } else if (this.mBluetooth.getState() != 12) {
                    this.mBluetoothLock.readLock().unlock();
                    return true;
                }
                this.mBluetoothLock.readLock().unlock();
                if (on || off) {
                    SystemClock.sleep(500);
                } else {
                    SystemClock.sleep(30);
                }
                i++;
            } catch (RemoteException e) {
                Slog.e(TAG, "getState()", e);
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
            }
        }
        Slog.e(TAG, "waitForOnOff time out");
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean waitForMonitoredOnOff(boolean on, boolean off) {
        int i = 0;
        while (i < 10) {
            synchronized (this.mConnection) {
                try {
                    if (this.mBluetooth != null) {
                        boolean ret;
                        if (on) {
                            if (this.mBluetooth.getState() == 12) {
                                return true;
                            } else if (this.mBluetooth.getState() == 15) {
                                bluetoothStateChangeHandler(14, 15);
                                ret = waitForOnOff(on, off);
                                return ret;
                            }
                        } else if (off) {
                            if (this.mBluetooth.getState() == 10) {
                                return true;
                            } else if (this.mBluetooth.getState() == 15) {
                                bluetoothStateChangeHandler(13, 15);
                                ret = waitForOnOff(on, off);
                                return ret;
                            }
                        } else if (this.mBluetooth.getState() != 12) {
                            return true;
                        }
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "getState()", e);
                }
            }
        }
        Slog.e(TAG, "waitForMonitoredOnOff time out");
        return false;
        Slog.e(TAG, "waitForMonitoredOnOff time out");
        return false;
    }

    private void sendDisableMsg(String packageName) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
        addActiveLog(packageName, false);
    }

    private void sendEnableMsg(boolean quietMode, String packageName) {
        int i;
        BluetoothHandler bluetoothHandler = this.mHandler;
        BluetoothHandler bluetoothHandler2 = this.mHandler;
        if (quietMode) {
            i = 1;
        } else {
            i = 0;
        }
        bluetoothHandler.sendMessage(bluetoothHandler2.obtainMessage(1, i, 0));
        addActiveLog(packageName, true);
    }

    private void addActiveLog(String packageName, boolean enable) {
        synchronized (this.mActiveLogs) {
            if (this.mActiveLogs.size() > 20) {
                this.mActiveLogs.remove();
            }
            this.mActiveLogs.add(new ActiveLog(packageName, enable, System.currentTimeMillis()));
        }
    }

    private void addCrashLog() {
        synchronized (this.mCrashTimestamps) {
            if (this.mCrashTimestamps.size() == 100) {
                this.mCrashTimestamps.removeFirst();
            }
            this.mCrashTimestamps.add(Long.valueOf(System.currentTimeMillis()));
            this.mCrashes++;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void recoverBluetoothServiceFromError(boolean clearBle) {
        Slog.e(TAG, "recoverBluetoothServiceFromError");
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
            }
            this.mBluetoothLock.readLock().unlock();
        } catch (RemoteException re) {
            Slog.e(TAG, "Unable to unregister", re);
        } catch (Throwable th) {
            this.mBluetoothLock.readLock().unlock();
        }
        waitForMonitoredOnOff(false, true);
        sendBluetoothServiceDownCallback();
        this.mHandler.removeMessages(60);
        this.mState = 10;
        if (clearBle) {
            clearBleApps();
        }
        this.mEnable = false;
        int i = this.mErrorRecoveryRetryCounter;
        this.mErrorRecoveryRetryCounter = i + 1;
        if (i < 6) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(42), 3000);
        }
    }

    private boolean isBluetoothDisallowed() {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            boolean hasUserRestriction = ((UserManager) this.mContext.getSystemService(UserManager.class)).hasUserRestriction("no_bluetooth", UserHandle.SYSTEM);
            return hasUserRestriction;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private boolean waitForBleOn() {
        int i = 0;
        while (i < 10) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth == null) {
                    this.mBluetoothLock.readLock().unlock();
                    break;
                } else if (this.mBluetooth.getState() == 15) {
                    this.mBluetoothLock.readLock().unlock();
                    return true;
                } else {
                    this.mBluetoothLock.readLock().unlock();
                    SystemClock.sleep(300);
                    i++;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "getState()", e);
                this.mBluetoothLock.readLock().unlock();
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
                throw th;
            }
        }
        Slog.e(TAG, "waitForBleOn time out");
        return false;
    }

    private void updateOppLauncherComponentState(int userId, boolean bluetoothSharingDisallowed) {
        int newState;
        ComponentName oppLauncherComponent = new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity");
        if (bluetoothSharingDisallowed) {
            newState = 2;
        } else {
            newState = 0;
        }
        try {
            AppGlobals.getPackageManager().setComponentEnabledSetting(oppLauncherComponent, newState, 1, userId);
        } catch (Exception e) {
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            String errorMsg = null;
            boolean protoOut = args.length > 0 ? args[0].startsWith("--proto") : false;
            if (!protoOut) {
                writer.println("Bluetooth Status");
                writer.println("  enabled: " + isEnabled());
                writer.println("  state: " + BluetoothAdapter.nameForState(this.mState));
                writer.println("  address: " + this.mAddress);
                writer.println("  name: " + this.mName);
                if (this.mEnable) {
                    long onDuration = System.currentTimeMillis() - ((ActiveLog) this.mActiveLogs.getLast()).getTime();
                    writer.println("  time since enabled: " + String.format("%02d:%02d:%02d.%03d", new Object[]{Integer.valueOf((int) (onDuration / 3600000)), Integer.valueOf((int) ((onDuration / 60000) % 60)), Integer.valueOf((int) ((onDuration / 1000) % 60)), Integer.valueOf((int) (onDuration % 1000))}) + "\n");
                }
                if (this.mActiveLogs.size() == 0) {
                    writer.println("Bluetooth never enabled!");
                } else {
                    writer.println("Enable log:");
                    for (ActiveLog log : this.mActiveLogs) {
                        writer.println("  " + log);
                    }
                }
                writer.println("Bluetooth crashed " + this.mCrashes + " time" + (this.mCrashes == 1 ? "" : "s"));
                if (this.mCrashes == 100) {
                    writer.println("(last 100)");
                }
                for (Long time : this.mCrashTimestamps) {
                    writer.println("  " + timeToLog(time.longValue()));
                }
                String bleAppString = "No BLE Apps registered.";
                if (this.mBleApps.size() == 1) {
                    bleAppString = "1 BLE App registered:";
                } else if (this.mBleApps.size() > 1) {
                    bleAppString = this.mBleApps.size() + " BLE Apps registered:";
                }
                writer.println("\n" + bleAppString);
                for (ClientDeathRecipient app : this.mBleApps.values()) {
                    writer.println("  " + app.getPackageName());
                }
                writer.println("");
                writer.flush();
                if (args.length == 0) {
                    args = new String[]{"--print"};
                }
            }
            if (this.mBluetoothBinder == null) {
                errorMsg = "Bluetooth Service not connected";
            } else {
                try {
                    this.mBluetoothBinder.dump(fd, args);
                } catch (RemoteException e) {
                    errorMsg = "RemoteException while dumping Bluetooth Service";
                }
            }
            if (errorMsg != null && !protoOut) {
                writer.println(errorMsg);
            }
        }
    }

    private boolean tryResumeProcess(int pid) {
        int count = 0;
        if (!OppoBPMHelper.isFrozingByPid(pid)) {
            return true;
        }
        do {
            int i = count;
            count = i + 1;
            if (i < 3) {
                OppoBPMHelper.setProcessResume(pid);
            } else {
                Slog.e(TAG, "tryResumeProcess() failed count = " + count + "; pid = " + pid);
                return false;
            }
        } while (OppoBPMHelper.isFrozingByPid(pid));
        Slog.d(TAG, "tryResumeProcess() count = " + count);
        OppoBPMHelper.recordResumeLog(pid, OppoProcessManager.RESUME_REASON_BLUETOOTH_STR);
        return true;
    }
}
