package android.bluetooth;

import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetoothStateChangeCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothA2dpSink implements BluetoothProfile {
    public static final String ACTION_AUDIO_CONFIG_CHANGED = "android.bluetooth.a2dp-sink.profile.action.AUDIO_CONFIG_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYING_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final String EXTRA_AUDIO_CONFIG = "android.bluetooth.a2dp-sink.profile.extra.AUDIO_CONFIG";
    public static final int STATE_NOT_PLAYING = 11;
    public static final int STATE_PLAYING = 10;
    private static final String TAG = "BluetoothA2dpSink";
    private static final boolean VDBG = false;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        public void onBluetoothStateChange(boolean up) {
            Log.d(BluetoothA2dpSink.TAG, "onBluetoothStateChange: up=" + up);
            ServiceConnection -get0;
            if (up) {
                -get0 = BluetoothA2dpSink.this.mConnection;
                synchronized (-get0) {
                    try {
                        if (BluetoothA2dpSink.this.mService == null) {
                            BluetoothA2dpSink.this.doBind();
                        }
                    } catch (Exception re) {
                        Log.e(BluetoothA2dpSink.TAG, "", re);
                    }
                }
            } else {
                -get0 = BluetoothA2dpSink.this.mConnection;
                synchronized (-get0) {
                    try {
                        BluetoothA2dpSink.this.mService = null;
                        BluetoothA2dpSink.this.mContext.unbindService(BluetoothA2dpSink.this.mConnection);
                    } catch (Exception re2) {
                        Log.e(BluetoothA2dpSink.TAG, "", re2);
                    }
                }
            }
            return;
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(BluetoothA2dpSink.TAG, "Proxy object connected");
            BluetoothA2dpSink.this.mService = IBluetoothA2dpSink.Stub.asInterface(Binder.allowBlocking(service));
            if (BluetoothA2dpSink.this.mServiceListener != null) {
                BluetoothA2dpSink.this.mServiceListener.onServiceConnected(11, BluetoothA2dpSink.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(BluetoothA2dpSink.TAG, "Proxy object disconnected");
            BluetoothA2dpSink.this.mService = null;
            if (BluetoothA2dpSink.this.mServiceListener != null) {
                BluetoothA2dpSink.this.mServiceListener.onServiceDisconnected(11);
            }
        }
    };
    private Context mContext;
    private volatile IBluetoothA2dpSink mService;
    private ServiceListener mServiceListener;

    BluetoothA2dpSink(Context context, ServiceListener l) {
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
        Intent intent = new Intent(IBluetoothA2dpSink.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && (this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle()) ^ 1) == 0) {
            return true;
        }
        Log.e(TAG, "Could not bind to Bluetooth A2DP Service with " + intent);
        return false;
    }

    void close() {
        this.mServiceListener = null;
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
        return;
    }

    public void finalize() {
        close();
    }

    public boolean connect(BluetoothDevice device) {
        log("connect(" + device + ")");
        IBluetoothA2dpSink service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        log("disconnect(" + device + ")");
        IBluetoothA2dpSink service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothA2dpSink service = this.mService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        }
        try {
            return service.getConnectedDevices();
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new ArrayList();
        }
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        IBluetoothA2dpSink service = this.mService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        }
        try {
            return service.getDevicesMatchingConnectionStates(states);
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new ArrayList();
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        IBluetoothA2dpSink service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public BluetoothAudioConfig getAudioConfig(BluetoothDevice device) {
        IBluetoothA2dpSink service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getAudioConfig(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return null;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return null;
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
        log("setPriority(" + device + ", " + priority + ")");
        IBluetoothA2dpSink service = this.mService;
        if (service == null || !isEnabled() || !isValidDevice(device)) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } else if (priority != 0 && priority != 100) {
            return false;
        } else {
            try {
                return service.setPriority(device, priority);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
    }

    public int getPriority(BluetoothDevice device) {
        IBluetoothA2dpSink service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getPriority(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean isA2dpPlaying(BluetoothDevice device) {
        IBluetoothA2dpSink service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.isA2dpPlaying(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public static String stateToString(int state) {
        switch (state) {
            case 0:
                return "disconnected";
            case 1:
                return "connecting";
            case 2:
                return "connected";
            case 3:
                return "disconnecting";
            case 10:
                return "playing";
            case 11:
                return "not playing";
            default:
                return "<unknown state " + state + ">";
        }
    }

    private boolean isEnabled() {
        return this.mAdapter.getState() == 12;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null ? BluetoothAdapter.checkBluetoothAddress(device.getAddress()) : false;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
