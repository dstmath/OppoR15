package org.codeaurora.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IExtTelephony extends IInterface {

    public static abstract class Stub extends Binder implements IExtTelephony {
        private static final String DESCRIPTOR = "org.codeaurora.internal.IExtTelephony";
        static final int TRANSACTION_activateUiccCard = 3;
        static final int TRANSACTION_deactivateUiccCard = 4;
        static final int TRANSACTION_getActiveSubscription = 19;
        static final int TRANSACTION_getCurrentPrimaryCardSlotId = 27;
        static final int TRANSACTION_getCurrentUiccCardProvisioningStatus = 1;
        static final int TRANSACTION_getPhoneIdForECall = 7;
        static final int TRANSACTION_getPrimaryCarrierSlotId = 22;
        static final int TRANSACTION_getPrimaryStackPhoneId = 10;
        static final int TRANSACTION_getSmscAddress = 25;
        static final int TRANSACTION_getUiccCardProvisioningUserPreference = 2;
        static final int TRANSACTION_isDeviceInSingleStandby = 15;
        static final int TRANSACTION_isDsdaEnabled = 20;
        static final int TRANSACTION_isEmergencyNumber = 11;
        static final int TRANSACTION_isFdnEnabled = 9;
        static final int TRANSACTION_isLocalEmergencyNumber = 12;
        static final int TRANSACTION_isPotentialEmergencyNumber = 13;
        static final int TRANSACTION_isPotentialLocalEmergencyNumber = 14;
        static final int TRANSACTION_isPrimaryCarrierSlotId = 23;
        static final int TRANSACTION_isSMSPromptEnabled = 5;
        static final int TRANSACTION_isVendorApkAvailable = 26;
        static final int TRANSACTION_setDsdaAdapter = 18;
        static final int TRANSACTION_setLocalCallHold = 16;
        static final int TRANSACTION_setPrimaryCardOnSlot = 8;
        static final int TRANSACTION_setSMSPromptEnabled = 6;
        static final int TRANSACTION_setSmscAddress = 24;
        static final int TRANSACTION_supplyIccDepersonalization = 21;
        static final int TRANSACTION_switchToActiveSub = 17;

        private static class Proxy implements IExtTelephony {
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

            public int getCurrentUiccCardProvisioningStatus(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(Stub.TRANSACTION_getCurrentUiccCardProvisioningStatus, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getUiccCardProvisioningUserPreference(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(Stub.TRANSACTION_getUiccCardProvisioningUserPreference, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int activateUiccCard(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(Stub.TRANSACTION_activateUiccCard, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int deactivateUiccCard(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(Stub.TRANSACTION_deactivateUiccCard, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSMSPromptEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isSMSPromptEnabled, _data, _reply, 0);
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

            public void setSMSPromptEnabled(boolean enabled) throws RemoteException {
                int i = 0;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (enabled) {
                        i = Stub.TRANSACTION_getCurrentUiccCardProvisioningStatus;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_setSMSPromptEnabled, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPhoneIdForECall() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getPhoneIdForECall, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPrimaryCardOnSlot(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(Stub.TRANSACTION_setPrimaryCardOnSlot, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isFdnEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isFdnEnabled, _data, _reply, 0);
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

            public int getPrimaryStackPhoneId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getPrimaryStackPhoneId, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isEmergencyNumber(String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    this.mRemote.transact(Stub.TRANSACTION_isEmergencyNumber, _data, _reply, 0);
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

            public boolean isLocalEmergencyNumber(String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    this.mRemote.transact(Stub.TRANSACTION_isLocalEmergencyNumber, _data, _reply, 0);
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

            public boolean isPotentialEmergencyNumber(String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    this.mRemote.transact(Stub.TRANSACTION_isPotentialEmergencyNumber, _data, _reply, 0);
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

            public boolean isPotentialLocalEmergencyNumber(String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    this.mRemote.transact(Stub.TRANSACTION_isPotentialLocalEmergencyNumber, _data, _reply, 0);
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

            public boolean isDeviceInSingleStandby() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isDeviceInSingleStandby, _data, _reply, 0);
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

            public boolean setLocalCallHold(int subId, boolean enable) throws RemoteException {
                int i = 0;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    if (enable) {
                        i = Stub.TRANSACTION_getCurrentUiccCardProvisioningStatus;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_setLocalCallHold, _data, _reply, 0);
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

            public void switchToActiveSub(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(Stub.TRANSACTION_switchToActiveSub, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDsdaAdapter(IDsda dsdaAdapter) throws RemoteException {
                IBinder iBinder = null;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (dsdaAdapter != null) {
                        iBinder = dsdaAdapter.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(Stub.TRANSACTION_setDsdaAdapter, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getActiveSubscription() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getActiveSubscription, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isDsdaEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isDsdaEnabled, _data, _reply, 0);
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

            public void supplyIccDepersonalization(String netpin, String type, IDepersoResCallback callback, int phoneId) throws RemoteException {
                IBinder iBinder = null;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(netpin);
                    _data.writeString(type);
                    if (callback != null) {
                        iBinder = callback.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeInt(phoneId);
                    this.mRemote.transact(Stub.TRANSACTION_supplyIccDepersonalization, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPrimaryCarrierSlotId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getPrimaryCarrierSlotId, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isPrimaryCarrierSlotId(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(Stub.TRANSACTION_isPrimaryCarrierSlotId, _data, _reply, 0);
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

            public boolean setSmscAddress(int slotId, String smsc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeString(smsc);
                    this.mRemote.transact(Stub.TRANSACTION_setSmscAddress, _data, _reply, 0);
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

            public String getSmscAddress(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(Stub.TRANSACTION_getSmscAddress, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isVendorApkAvailable(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(Stub.TRANSACTION_isVendorApkAvailable, _data, _reply, 0);
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

            public int getCurrentPrimaryCardSlotId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getCurrentPrimaryCardSlotId, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IExtTelephony asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IExtTelephony)) {
                return new Proxy(obj);
            }
            return (IExtTelephony) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int _result;
            boolean _result2;
            switch (code) {
                case TRANSACTION_getCurrentUiccCardProvisioningStatus /*1*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getCurrentUiccCardProvisioningStatus(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_getUiccCardProvisioningUserPreference /*2*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getUiccCardProvisioningUserPreference(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_activateUiccCard /*3*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = activateUiccCard(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_deactivateUiccCard /*4*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = deactivateUiccCard(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_isSMSPromptEnabled /*5*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isSMSPromptEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_setSMSPromptEnabled /*6*/:
                    data.enforceInterface(DESCRIPTOR);
                    setSMSPromptEnabled(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case TRANSACTION_getPhoneIdForECall /*7*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getPhoneIdForECall();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_setPrimaryCardOnSlot /*8*/:
                    data.enforceInterface(DESCRIPTOR);
                    setPrimaryCardOnSlot(data.readInt());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_isFdnEnabled /*9*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isFdnEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_getPrimaryStackPhoneId /*10*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getPrimaryStackPhoneId();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_isEmergencyNumber /*11*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isEmergencyNumber(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_isLocalEmergencyNumber /*12*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isLocalEmergencyNumber(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_isPotentialEmergencyNumber /*13*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isPotentialEmergencyNumber(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_isPotentialLocalEmergencyNumber /*14*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isPotentialLocalEmergencyNumber(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_isDeviceInSingleStandby /*15*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isDeviceInSingleStandby();
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_setLocalCallHold /*16*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setLocalCallHold(data.readInt(), data.readInt() != 0);
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_switchToActiveSub /*17*/:
                    data.enforceInterface(DESCRIPTOR);
                    switchToActiveSub(data.readInt());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_setDsdaAdapter /*18*/:
                    data.enforceInterface(DESCRIPTOR);
                    setDsdaAdapter(org.codeaurora.internal.IDsda.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case TRANSACTION_getActiveSubscription /*19*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getActiveSubscription();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_isDsdaEnabled /*20*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isDsdaEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_supplyIccDepersonalization /*21*/:
                    data.enforceInterface(DESCRIPTOR);
                    supplyIccDepersonalization(data.readString(), data.readString(), org.codeaurora.internal.IDepersoResCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_getPrimaryCarrierSlotId /*22*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getPrimaryCarrierSlotId();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_isPrimaryCarrierSlotId /*23*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isPrimaryCarrierSlotId(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_setSmscAddress /*24*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSmscAddress(data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_getSmscAddress /*25*/:
                    data.enforceInterface(DESCRIPTOR);
                    String _result3 = getSmscAddress(data.readInt());
                    reply.writeNoException();
                    reply.writeString(_result3);
                    return true;
                case TRANSACTION_isVendorApkAvailable /*26*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = isVendorApkAvailable(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? TRANSACTION_getCurrentUiccCardProvisioningStatus : 0);
                    return true;
                case TRANSACTION_getCurrentPrimaryCardSlotId /*27*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getCurrentPrimaryCardSlotId();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    int activateUiccCard(int i) throws RemoteException;

    int deactivateUiccCard(int i) throws RemoteException;

    int getActiveSubscription() throws RemoteException;

    int getCurrentPrimaryCardSlotId() throws RemoteException;

    int getCurrentUiccCardProvisioningStatus(int i) throws RemoteException;

    int getPhoneIdForECall() throws RemoteException;

    int getPrimaryCarrierSlotId() throws RemoteException;

    int getPrimaryStackPhoneId() throws RemoteException;

    String getSmscAddress(int i) throws RemoteException;

    int getUiccCardProvisioningUserPreference(int i) throws RemoteException;

    boolean isDeviceInSingleStandby() throws RemoteException;

    boolean isDsdaEnabled() throws RemoteException;

    boolean isEmergencyNumber(String str) throws RemoteException;

    boolean isFdnEnabled() throws RemoteException;

    boolean isLocalEmergencyNumber(String str) throws RemoteException;

    boolean isPotentialEmergencyNumber(String str) throws RemoteException;

    boolean isPotentialLocalEmergencyNumber(String str) throws RemoteException;

    boolean isPrimaryCarrierSlotId(int i) throws RemoteException;

    boolean isSMSPromptEnabled() throws RemoteException;

    boolean isVendorApkAvailable(String str) throws RemoteException;

    void setDsdaAdapter(IDsda iDsda) throws RemoteException;

    boolean setLocalCallHold(int i, boolean z) throws RemoteException;

    void setPrimaryCardOnSlot(int i) throws RemoteException;

    void setSMSPromptEnabled(boolean z) throws RemoteException;

    boolean setSmscAddress(int i, String str) throws RemoteException;

    void supplyIccDepersonalization(String str, String str2, IDepersoResCallback iDepersoResCallback, int i) throws RemoteException;

    void switchToActiveSub(int i) throws RemoteException;
}
