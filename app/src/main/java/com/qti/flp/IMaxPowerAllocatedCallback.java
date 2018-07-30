package com.qti.flp;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMaxPowerAllocatedCallback extends IInterface {

    public static abstract class Stub extends Binder implements IMaxPowerAllocatedCallback {
        private static final String DESCRIPTOR = "com.qti.flp.IMaxPowerAllocatedCallback";
        static final int TRANSACTION_onMaxPowerAllocatedChanged = 1;

        private static class Proxy implements IMaxPowerAllocatedCallback {
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

            public void onMaxPowerAllocatedChanged(double power_mW) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeDouble(power_mW);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMaxPowerAllocatedCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IMaxPowerAllocatedCallback)) {
                return new Proxy(obj);
            }
            return (IMaxPowerAllocatedCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    onMaxPowerAllocatedChanged(data.readDouble());
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void onMaxPowerAllocatedChanged(double d) throws RemoteException;
}
