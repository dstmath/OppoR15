package com.android.internal.app;

import android.app.AppOpsManager.PackageOps;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IAppOpsService extends IInterface {

    public static abstract class Stub extends Binder implements IAppOpsService {
        private static final String DESCRIPTOR = "com.android.internal.app.IAppOpsService";
        static final int TRANSACTION_checkAudioOperation = 17;
        static final int TRANSACTION_checkOperation = 1;
        static final int TRANSACTION_checkPackage = 10;
        static final int TRANSACTION_finishOperation = 4;
        static final int TRANSACTION_getOpsForPackage = 12;
        static final int TRANSACTION_getPackagesForOps = 11;
        static final int TRANSACTION_getToken = 7;
        static final int TRANSACTION_getUidOps = 13;
        static final int TRANSACTION_isOperationActive = 22;
        static final int TRANSACTION_noteOperation = 2;
        static final int TRANSACTION_noteProxyOperation = 9;
        static final int TRANSACTION_permissionToOpCode = 8;
        static final int TRANSACTION_removeUser = 21;
        static final int TRANSACTION_resetAllModes = 16;
        static final int TRANSACTION_setAudioRestriction = 18;
        static final int TRANSACTION_setMode = 15;
        static final int TRANSACTION_setUidMode = 14;
        static final int TRANSACTION_setUserRestriction = 20;
        static final int TRANSACTION_setUserRestrictions = 19;
        static final int TRANSACTION_startOperation = 3;
        static final int TRANSACTION_startWatchingMode = 5;
        static final int TRANSACTION_stopWatchingMode = 6;

        private static class Proxy implements IAppOpsService {
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

            public int checkOperation(int code, int uid, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int noteOperation(int code, int uid, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeInt(uid);
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

            public int startOperation(IBinder token, int code, int uid, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(code);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void finishOperation(IBinder token, int code, int uid, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(code);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startWatchingMode(int op, String packageName, IAppOpsCallback callback) throws RemoteException {
                IBinder iBinder = null;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(op);
                    _data.writeString(packageName);
                    if (callback != null) {
                        iBinder = callback.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopWatchingMode(IAppOpsCallback callback) throws RemoteException {
                IBinder iBinder = null;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (callback != null) {
                        iBinder = callback.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getToken(IBinder clientToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(clientToken);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int permissionToOpCode(String permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permission);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int noteProxyOperation(int code, String proxyPackageName, int callingUid, String callingPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeString(proxyPackageName);
                    _data.writeInt(callingUid);
                    _data.writeString(callingPackageName);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int checkPackage(int uid, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<PackageOps> getPackagesForOps(int[] ops) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(ops);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    List<PackageOps> _result = _reply.createTypedArrayList(PackageOps.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<PackageOps> getOpsForPackage(int uid, String packageName, int[] ops) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    _data.writeIntArray(ops);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    List<PackageOps> _result = _reply.createTypedArrayList(PackageOps.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<PackageOps> getUidOps(int uid, int[] ops) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeIntArray(ops);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    List<PackageOps> _result = _reply.createTypedArrayList(PackageOps.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setUidMode(int code, int uid, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeInt(uid);
                    _data.writeInt(mode);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setMode(int code, int uid, String packageName, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    _data.writeInt(mode);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void resetAllModes(int reqUserId, String reqPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reqUserId);
                    _data.writeString(reqPackageName);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int checkAudioOperation(int code, int usage, int uid, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeInt(usage);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAudioRestriction(int code, int usage, int uid, int mode, String[] exceptionPackages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeInt(usage);
                    _data.writeInt(uid);
                    _data.writeInt(mode);
                    _data.writeStringArray(exceptionPackages);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setUserRestrictions(Bundle restrictions, IBinder token, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (restrictions != null) {
                        _data.writeInt(1);
                        restrictions.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(token);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setUserRestriction(int code, boolean restricted, IBinder token, int userHandle, String[] exceptionPackages) throws RemoteException {
                int i = 0;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    if (restricted) {
                        i = 1;
                    }
                    _data.writeInt(i);
                    _data.writeStrongBinder(token);
                    _data.writeInt(userHandle);
                    _data.writeStringArray(exceptionPackages);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isOperationActive(int code, int uid, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    this.mRemote.transact(22, _data, _reply, 0);
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

        public static IAppOpsService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IAppOpsService)) {
                return new Proxy(obj);
            }
            return (IAppOpsService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int _result;
            List<PackageOps> _result2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    _result = checkOperation(data.readInt(), data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    _result = noteOperation(data.readInt(), data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    _result = startOperation(data.readStrongBinder(), data.readInt(), data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    finishOperation(data.readStrongBinder(), data.readInt(), data.readInt(), data.readString());
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    startWatchingMode(data.readInt(), data.readString(), com.android.internal.app.IAppOpsCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    stopWatchingMode(com.android.internal.app.IAppOpsCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result3 = getToken(data.readStrongBinder());
                    reply.writeNoException();
                    reply.writeStrongBinder(_result3);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    _result = permissionToOpCode(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    _result = noteProxyOperation(data.readInt(), data.readString(), data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    _result = checkPackage(data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getPackagesForOps(data.createIntArray());
                    reply.writeNoException();
                    reply.writeTypedList(_result2);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getOpsForPackage(data.readInt(), data.readString(), data.createIntArray());
                    reply.writeNoException();
                    reply.writeTypedList(_result2);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getUidOps(data.readInt(), data.createIntArray());
                    reply.writeNoException();
                    reply.writeTypedList(_result2);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    setUidMode(data.readInt(), data.readInt(), data.readInt());
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    setMode(data.readInt(), data.readInt(), data.readString(), data.readInt());
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    resetAllModes(data.readInt(), data.readString());
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    _result = checkAudioOperation(data.readInt(), data.readInt(), data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    setAudioRestriction(data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.createStringArray());
                    reply.writeNoException();
                    return true;
                case 19:
                    Bundle _arg0;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    setUserRestrictions(_arg0, data.readStrongBinder(), data.readInt());
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    setUserRestriction(data.readInt(), data.readInt() != 0, data.readStrongBinder(), data.readInt(), data.createStringArray());
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    removeUser(data.readInt());
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result4 = isOperationActive(data.readInt(), data.readInt(), data.readString());
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

    int checkAudioOperation(int i, int i2, int i3, String str) throws RemoteException;

    int checkOperation(int i, int i2, String str) throws RemoteException;

    int checkPackage(int i, String str) throws RemoteException;

    void finishOperation(IBinder iBinder, int i, int i2, String str) throws RemoteException;

    List<PackageOps> getOpsForPackage(int i, String str, int[] iArr) throws RemoteException;

    List<PackageOps> getPackagesForOps(int[] iArr) throws RemoteException;

    IBinder getToken(IBinder iBinder) throws RemoteException;

    List<PackageOps> getUidOps(int i, int[] iArr) throws RemoteException;

    boolean isOperationActive(int i, int i2, String str) throws RemoteException;

    int noteOperation(int i, int i2, String str) throws RemoteException;

    int noteProxyOperation(int i, String str, int i2, String str2) throws RemoteException;

    int permissionToOpCode(String str) throws RemoteException;

    void removeUser(int i) throws RemoteException;

    void resetAllModes(int i, String str) throws RemoteException;

    void setAudioRestriction(int i, int i2, int i3, int i4, String[] strArr) throws RemoteException;

    void setMode(int i, int i2, String str, int i3) throws RemoteException;

    void setUidMode(int i, int i2, int i3) throws RemoteException;

    void setUserRestriction(int i, boolean z, IBinder iBinder, int i2, String[] strArr) throws RemoteException;

    void setUserRestrictions(Bundle bundle, IBinder iBinder, int i) throws RemoteException;

    int startOperation(IBinder iBinder, int i, int i2, String str) throws RemoteException;

    void startWatchingMode(int i, String str, IAppOpsCallback iAppOpsCallback) throws RemoteException;

    void stopWatchingMode(IAppOpsCallback iAppOpsCallback) throws RemoteException;
}
