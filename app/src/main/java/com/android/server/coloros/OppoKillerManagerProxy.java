package com.android.server.coloros;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

public class OppoKillerManagerProxy implements IOppoKillerManager {
    final String TAG = "OppoKillerManagerProxy";
    private IBinder mRemote;

    public OppoKillerManagerProxy(IBinder remote) {
        this.mRemote = remote;
    }

    public String getInterfaceDescriptor() {
        return IOppoKillerManager.DESCRIPTOR;
    }

    public IBinder asBinder() {
        return this.mRemote;
    }

    public int oppoKill(int uid, int pid, String packageName, int level, int flag) throws RemoteException {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        int result = 0;
        try {
            _data.writeInterfaceToken(IOppoKillerManager.DESCRIPTOR);
            _data.writeInt(uid);
            _data.writeInt(pid);
            _data.writeString(packageName);
            _data.writeInt(level);
            _data.writeInt(flag);
            this.mRemote.transact(101, _data, _reply, 0);
            _reply.readException();
            result = _reply.readInt();
            return result;
        } finally {
            _reply.recycle();
            _data.recycle();
        }
    }

    public int oppoFreeze(int uid, int pid, String packageName, int level, int flag) throws RemoteException {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        int result = 0;
        try {
            _data.writeInterfaceToken(IOppoKillerManager.DESCRIPTOR);
            _data.writeInt(uid);
            _data.writeInt(pid);
            _data.writeString(packageName);
            _data.writeInt(level);
            _data.writeInt(flag);
            this.mRemote.transact(102, _data, _reply, 0);
            _reply.readException();
            result = _reply.readInt();
            return result;
        } finally {
            _reply.recycle();
            _data.recycle();
        }
    }
}
