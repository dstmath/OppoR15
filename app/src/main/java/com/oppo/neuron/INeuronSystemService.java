package com.oppo.neuron;

import android.content.ContentValues;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INeuronSystemService extends IInterface {

    public static abstract class Stub extends Binder implements INeuronSystemService {
        private static final String DESCRIPTOR = "com.oppo.neuron.INeuronSystemService";
        static final int TRANSACTION_flushDbSync = 3;
        static final int TRANSACTION_getAppRecentUsedTime = 4;
        static final int TRANSACTION_getConnectionPids = 5;
        static final int TRANSACTION_isInDisplayDeviceList = 6;
        static final int TRANSACTION_isPriorApp = 2;
        static final int TRANSACTION_publishEvent = 1;

        private static class Proxy implements INeuronSystemService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void publishEvent(int type, ContentValues contentValues) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    if (contentValues != null) {
                        _data.writeInt(1);
                        contentValues.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int isPriorApp(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void flushDbSync() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long[] getAppRecentUsedTime(String packageName, int flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(flag);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    long[] _result = _reply.createLongArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] getConnectionPids(int[] srcPids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(srcPids);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isInDisplayDeviceList(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INeuronSystemService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INeuronSystemService)) {
                return new Proxy(obj);
            }
            return (INeuronSystemService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    ContentValues _arg1;
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg1 = (ContentValues) ContentValues.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    publishEvent(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _result = isPriorApp(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    flushDbSync();
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    long[] _result2 = getAppRecentUsedTime(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeLongArray(_result2);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int[] _result3 = getConnectionPids(data.createIntArray());
                    reply.writeNoException();
                    reply.writeIntArray(_result3);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result4 = isInDisplayDeviceList(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void flushDbSync() throws RemoteException;

    long[] getAppRecentUsedTime(String str, int i) throws RemoteException;

    int[] getConnectionPids(int[] iArr) throws RemoteException;

    boolean isInDisplayDeviceList(String str) throws RemoteException;

    int isPriorApp(String str) throws RemoteException;

    void publishEvent(int i, ContentValues contentValues) throws RemoteException;
}
