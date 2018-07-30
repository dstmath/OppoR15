package com.oppo.neuron;

import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.util.ArrayList;

public class NeoServiceProxy implements INeoService {
    private static final boolean DBG = false;
    private static final String TAG = "NeoServiceProxy";
    private static NeoServiceProxy sInstance;
    private DeathRecipient mDeathRecipient = new DeathRecipient() {
        public void binderDied() {
            NeoServiceProxy.this.mRemote = null;
        }
    };
    private IBinder mRemote;

    public static synchronized NeoServiceProxy getInstance() {
        NeoServiceProxy neoServiceProxy;
        synchronized (NeoServiceProxy.class) {
            if (sInstance == null) {
                sInstance = new NeoServiceProxy();
            }
            neoServiceProxy = sInstance;
        }
        return neoServiceProxy;
    }

    private NeoServiceProxy() {
        connectToNeoService();
    }

    private IBinder connectToNeoService() {
        this.mRemote = ServiceManager.getService(INeoService.DESCRIPTOR);
        if (this.mRemote != null) {
            try {
                this.mRemote.linkToDeath(this.mDeathRecipient, 0);
            } catch (RemoteException e) {
                this.mRemote = null;
            }
        }
        return this.mRemote;
    }

    public int isPriorApp(int counter, int timeFactor, long accumulatedTime, long duration) throws RemoteException {
        if (this.mRemote == null && connectToNeoService() == null) {
            return -1;
        }
        int result = -1;
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        try {
            _data.writeInterfaceToken(INeoService.DESCRIPTOR);
            _data.writeInt(counter);
            _data.writeInt(timeFactor);
            _data.writeLong(accumulatedTime);
            _data.writeLong(duration);
            this.mRemote.transact(201, _data, _reply, 0);
            _reply.readException();
            result = _reply.readInt();
            return result;
        } finally {
            _reply.recycle();
            _data.recycle();
        }
    }

    public float appCleanPredict(boolean scrrenOn, String targeApp, String foregroundApp, boolean charged, float remainBattery, String wifiSSID, String wifiBSSID, ArrayList<String> otherAps) {
        if (this.mRemote == null && connectToNeoService() == null) {
            return -1.0f;
        }
        if (otherAps == null || otherAps.size() % 2 != 0) {
            return -1.0f;
        }
        float result = -1.0f;
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        float f;
        try {
            _data.writeInterfaceToken(INeoService.DESCRIPTOR);
            _data.writeString(targeApp);
            f = this.mRemote;
            f.transact(301, _data, _reply, 0);
            _reply.readException();
            result = _reply.readFloat();
            return result;
        } catch (RemoteException e) {
            f = -1.0f;
            return f;
        } finally {
            _reply.recycle();
            _data.recycle();
        }
    }

    public String[] appPreloadPredict() {
        if (this.mRemote == null && connectToNeoService() == null) {
            return null;
        }
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        try {
            _data.writeInterfaceToken(INeoService.DESCRIPTOR);
            this.mRemote.transact(302, _data, _reply, 0);
            int num = _reply.readInt();
            String[] apps = new String[num];
            for (int i = 0; i < num; i++) {
                apps[i] = _reply.readString();
            }
            _reply.readException();
            _reply.readInt();
            return apps;
        } catch (RemoteException e) {
            return null;
        } finally {
            _reply.recycle();
            _data.recycle();
        }
    }
}
