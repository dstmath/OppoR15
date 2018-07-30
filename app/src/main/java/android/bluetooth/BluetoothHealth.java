package android.bluetooth;

import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetoothStateChangeCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothHealth implements BluetoothProfile {
    public static final int APP_CONFIG_REGISTRATION_FAILURE = 1;
    public static final int APP_CONFIG_REGISTRATION_SUCCESS = 0;
    public static final int APP_CONFIG_UNREGISTRATION_FAILURE = 3;
    public static final int APP_CONFIG_UNREGISTRATION_SUCCESS = 2;
    public static final int CHANNEL_TYPE_ANY = 12;
    public static final int CHANNEL_TYPE_RELIABLE = 10;
    public static final int CHANNEL_TYPE_STREAMING = 11;
    private static final boolean DBG = true;
    public static final int HEALTH_OPERATION_ERROR = 6001;
    public static final int HEALTH_OPERATION_GENERIC_FAILURE = 6003;
    public static final int HEALTH_OPERATION_INVALID_ARGS = 6002;
    public static final int HEALTH_OPERATION_NOT_ALLOWED = 6005;
    public static final int HEALTH_OPERATION_NOT_FOUND = 6004;
    public static final int HEALTH_OPERATION_SUCCESS = 6000;
    public static final int SINK_ROLE = 2;
    public static final int SOURCE_ROLE = 1;
    public static final int STATE_CHANNEL_CONNECTED = 2;
    public static final int STATE_CHANNEL_CONNECTING = 1;
    public static final int STATE_CHANNEL_DISCONNECTED = 0;
    public static final int STATE_CHANNEL_DISCONNECTING = 3;
    private static final String TAG = "BluetoothHealth";
    private static final boolean VDBG = false;
    BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        public void onBluetoothStateChange(boolean up) {
            Log.d(BluetoothHealth.TAG, "onBluetoothStateChange: up=" + up);
            ServiceConnection -get0;
            if (up) {
                -get0 = BluetoothHealth.this.mConnection;
                synchronized (-get0) {
                    try {
                        if (BluetoothHealth.this.mService == null) {
                            BluetoothHealth.this.doBind();
                        }
                    } catch (Exception re) {
                        Log.e(BluetoothHealth.TAG, "", re);
                    }
                }
            } else {
                -get0 = BluetoothHealth.this.mConnection;
                synchronized (-get0) {
                    try {
                        BluetoothHealth.this.mService = null;
                        BluetoothHealth.this.mContext.unbindService(BluetoothHealth.this.mConnection);
                    } catch (Exception re2) {
                        Log.e(BluetoothHealth.TAG, "", re2);
                    }
                }
            }
            return;
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(BluetoothHealth.TAG, "Proxy object connected");
            BluetoothHealth.this.mService = IBluetoothHealth.Stub.asInterface(Binder.allowBlocking(service));
            if (BluetoothHealth.this.mServiceListener != null) {
                BluetoothHealth.this.mServiceListener.onServiceConnected(3, BluetoothHealth.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(BluetoothHealth.TAG, "Proxy object disconnected");
            BluetoothHealth.this.mService = null;
            if (BluetoothHealth.this.mServiceListener != null) {
                BluetoothHealth.this.mServiceListener.onServiceDisconnected(3);
            }
        }
    };
    private Context mContext;
    private volatile IBluetoothHealth mService;
    private ServiceListener mServiceListener;

    private static class BluetoothHealthCallbackWrapper extends IBluetoothHealthCallback.Stub {
        private BluetoothHealthCallback mCallback;

        public BluetoothHealthCallbackWrapper(BluetoothHealthCallback callback) {
            this.mCallback = callback;
        }

        public void onHealthAppConfigurationStatusChange(BluetoothHealthAppConfiguration config, int status) {
            this.mCallback.onHealthAppConfigurationStatusChange(config, status);
        }

        public void onHealthChannelStateChange(BluetoothHealthAppConfiguration config, BluetoothDevice device, int prevState, int newState, ParcelFileDescriptor fd, int channelId) {
            this.mCallback.onHealthChannelStateChange(config, device, prevState, newState, fd, channelId);
        }
    }

    public boolean registerSinkAppConfiguration(String name, int dataType, BluetoothHealthCallback callback) {
        if (!isEnabled() || name == null) {
            return false;
        }
        return registerAppConfiguration(name, dataType, 2, 12, callback);
    }

    public boolean registerAppConfiguration(String name, int dataType, int role, int channelType, BluetoothHealthCallback callback) {
        boolean result = false;
        if (!isEnabled() || (checkAppParam(name, role, channelType, callback) ^ 1) != 0) {
            return result;
        }
        BluetoothHealthCallbackWrapper wrapper = new BluetoothHealthCallbackWrapper(callback);
        BluetoothHealthAppConfiguration config = new BluetoothHealthAppConfiguration(name, dataType, role, channelType);
        IBluetoothHealth service = this.mService;
        if (service != null) {
            try {
                result = service.registerAppConfiguration(config, wrapper);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return result;
    }

    public boolean unregisterAppConfiguration(BluetoothHealthAppConfiguration config) {
        boolean result = false;
        IBluetoothHealth service = this.mService;
        if (service == null || !isEnabled() || config == null) {
            Log.w(TAG, "Proxy not attached to service");
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
            return result;
        }
        try {
            return service.unregisterAppConfiguration(config);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
            return result;
        }
    }

    public boolean connectChannelToSource(BluetoothDevice device, BluetoothHealthAppConfiguration config) {
        IBluetoothHealth service = this.mService;
        if (service == null || !isEnabled() || !isValidDevice(device) || config == null) {
            Log.w(TAG, "Proxy not attached to service");
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        } else {
            try {
                return service.connectChannelToSource(device, config);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
        return false;
    }

    public boolean connectChannelToSink(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelType) {
        IBluetoothHealth service = this.mService;
        if (service == null || !isEnabled() || !isValidDevice(device) || config == null) {
            Log.w(TAG, "Proxy not attached to service");
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        } else {
            try {
                return service.connectChannelToSink(device, config, channelType);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
        return false;
    }

    public boolean disconnectChannel(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelId) {
        IBluetoothHealth service = this.mService;
        if (service == null || !isEnabled() || !isValidDevice(device) || config == null) {
            Log.w(TAG, "Proxy not attached to service");
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        } else {
            try {
                return service.disconnectChannel(device, config, channelId);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
        return false;
    }

    public ParcelFileDescriptor getMainChannelFd(BluetoothDevice device, BluetoothHealthAppConfiguration config) {
        IBluetoothHealth service = this.mService;
        if (service == null || !isEnabled() || !isValidDevice(device) || config == null) {
            Log.w(TAG, "Proxy not attached to service");
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        } else {
            try {
                return service.getMainChannelFd(device, config);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
        return null;
    }

    public int getConnectionState(BluetoothDevice device) {
        IBluetoothHealth service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getHealthDeviceConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return 0;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothHealth service = this.mService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        }
        try {
            return service.getConnectedHealthDevices();
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new ArrayList();
        }
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        IBluetoothHealth service = this.mService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        }
        try {
            return service.getHealthDevicesMatchingConnectionStates(states);
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new ArrayList();
        }
    }

    BluetoothHealth(Context context, ServiceListener l) {
        this.mContext = context;
        this.mServiceListener = l;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothHealth.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && (this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle()) ^ 1) == 0) {
            return true;
        }
        Log.e(TAG, "Could not bind to Bluetooth Health Service with " + intent);
        return false;
    }

    void close() {
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
        synchronized (this.mConnection) {
            if (this.mService != null) {
                try {
                    this.mService = null;
                    this.mContext.unbindService(this.mConnection);
                } catch (Exception re) {
                    Log.e(TAG, "", re);
                }
            }
        }
        this.mServiceListener = null;
        return;
    }

    private boolean isEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.getState() == 12) {
            return true;
        }
        log("Bluetooth is Not enabled");
        return false;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null ? BluetoothAdapter.checkBluetoothAddress(device.getAddress()) : false;
    }

    private boolean checkAppParam(String name, int role, int channelType, BluetoothHealthCallback callback) {
        if (name == null || ((role != 1 && role != 2) || ((channelType != 10 && channelType != 11 && channelType != 12) || callback == null))) {
            return false;
        }
        return (role == 1 && channelType == 12) ? false : true;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
