package android.engineer;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IOppoEngineerManager extends IInterface {

    public static abstract class Stub extends Binder implements IOppoEngineerManager {
        private static final String DESCRIPTOR = "android.engineer.IOppoEngineerManager";
        static final int TRANSACTION_cameraHardwareCheck = 7;
        static final int TRANSACTION_disablePartionWriteProtect = 13;
        static final int TRANSACTION_getBackCoverColorId = 14;
        static final int TRANSACTION_getBadBatteryConfig = 28;
        static final int TRANSACTION_getBootImgWaterMark = 41;
        static final int TRANSACTION_getCalibrationStatusFromNvram = 38;
        static final int TRANSACTION_getCarrierVersion = 18;
        static final int TRANSACTION_getCarrierVersionFromNvram = 36;
        static final int TRANSACTION_getDownloadStatus = 8;
        static final int TRANSACTION_getEmmcHealthInfo = 11;
        static final int TRANSACTION_getEncryptImeiFromNvram = 35;
        static final int TRANSACTION_getEngResultFromNvram = 33;
        static final int TRANSACTION_getProductLineTestResult = 30;
        static final int TRANSACTION_getRegionNetlockStatus = 20;
        static final int TRANSACTION_getRpmbEnableState = 17;
        static final int TRANSACTION_getRpmbState = 16;
        static final int TRANSACTION_getSimOperatorSwitchStatus = 40;
        static final int TRANSACTION_getSingleDoubleCardStatus = 26;
        static final int TRANSACTION_getTelcelSimlockStatus = 22;
        static final int TRANSACTION_getTelcelSimlockUnlockTimes = 24;
        static final int TRANSACTION_isPartionWriteProtectDisabled = 12;
        static final int TRANSACTION_isSerialPortEnabled = 9;
        static final int TRANSACTION_resetProductLineTestResult = 32;
        static final int TRANSACTION_saveCarrierVersionToNvram = 37;
        static final int TRANSACTION_saveEngResultToNvram = 34;
        static final int TRANSACTION_setBackCoverColorId = 15;
        static final int TRANSACTION_setBatteryBatteryConfig = 29;
        static final int TRANSACTION_setCarrierVersion = 19;
        static final int TRANSACTION_setProductLineTestResult = 31;
        static final int TRANSACTION_setRegionNetlock = 21;
        static final int TRANSACTION_setSerialPortState = 10;
        static final int TRANSACTION_setSimOperatorSwitch = 39;
        static final int TRANSACTION_setSingleDoubleCard = 27;
        static final int TRANSACTION_setTelcelSimlock = 23;
        static final int TRANSACTION_setTelcelSimlockUnlockTimes = 25;
        static final int TRANSACTION_setTorchState = 6;
        static final int TRANSACTION_turnBreathLightFlashOn = 4;
        static final int TRANSACTION_turnBreathLightOff = 5;
        static final int TRANSACTION_turnBreathLightOn = 3;
        static final int TRANSACTION_turnButtonLightOff = 2;
        static final int TRANSACTION_turnButtonLightOn = 1;

        private static class Proxy implements IOppoEngineerManager {
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

            public void turnButtonLightOn(int brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(brightness);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void turnButtonLightOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void turnBreathLightOn(int brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(brightness);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void turnBreathLightFlashOn(int brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(brightness);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void turnBreathLightOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setTorchState(String state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(state);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int cameraHardwareCheck(int camera, int invalid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(camera);
                    _data.writeInt(invalid);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getDownloadStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSerialPortEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
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

            public boolean setSerialPortState(boolean enable) throws RemoteException {
                int i = 0;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (enable) {
                        i = 1;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(10, _data, _reply, 0);
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

            public byte[] getEmmcHealthInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isPartionWriteProtectDisabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
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

            public boolean disablePartionWriteProtect(boolean disable) throws RemoteException {
                int i = 0;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (disable) {
                        i = 1;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(13, _data, _reply, 0);
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

            public String getBackCoverColorId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setBackCoverColorId(String colorId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(colorId);
                    this.mRemote.transact(15, _data, _reply, 0);
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

            public boolean getRpmbState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
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

            public boolean getRpmbEnableState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
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

            public String getCarrierVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setCarrierVersion(String version) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(version);
                    this.mRemote.transact(19, _data, _reply, 0);
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

            public String getRegionNetlockStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setRegionNetlock(String lock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(lock);
                    this.mRemote.transact(21, _data, _reply, 0);
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

            public String getTelcelSimlockStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setTelcelSimlock(String lock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(lock);
                    this.mRemote.transact(23, _data, _reply, 0);
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

            public String getTelcelSimlockUnlockTimes() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setTelcelSimlockUnlockTimes(String times) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(times);
                    this.mRemote.transact(25, _data, _reply, 0);
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

            public String getSingleDoubleCardStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSingleDoubleCard(String state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(state);
                    this.mRemote.transact(27, _data, _reply, 0);
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

            public byte[] getBadBatteryConfig(int offset, int size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(offset);
                    _data.writeInt(size);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setBatteryBatteryConfig(int offset, int size, byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(offset);
                    _data.writeInt(size);
                    _data.writeByteArray(data);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] getProductLineTestResult() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setProductLineTestResult(int position, int result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(position);
                    _data.writeInt(result);
                    this.mRemote.transact(31, _data, _reply, 0);
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

            public boolean resetProductLineTestResult() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
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

            public byte[] getEngResultFromNvram() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean saveEngResultToNvram(byte[] result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(result);
                    this.mRemote.transact(34, _data, _reply, 0);
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

            public byte[] getEncryptImeiFromNvram() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] getCarrierVersionFromNvram() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean saveCarrierVersionToNvram(byte[] version) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(version);
                    this.mRemote.transact(37, _data, _reply, 0);
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

            public byte[] getCalibrationStatusFromNvram() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSimOperatorSwitch(String state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(state);
                    this.mRemote.transact(39, _data, _reply, 0);
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

            public String getSimOperatorSwitchStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getBootImgWaterMark() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
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

        public static IOppoEngineerManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IOppoEngineerManager)) {
                return new Proxy(obj);
            }
            return (IOppoEngineerManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int _result;
            String _result2;
            boolean _result3;
            byte[] _result4;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    turnButtonLightOn(data.readInt());
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    turnButtonLightOff();
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    turnBreathLightOn(data.readInt());
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    turnBreathLightFlashOn(data.readInt());
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    turnBreathLightOff();
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    setTorchState(data.readString());
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    _result = cameraHardwareCheck(data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getDownloadStatus();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = isSerialPortEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = setSerialPortState(data.readInt() != 0);
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getEmmcHealthInfo();
                    reply.writeNoException();
                    reply.writeByteArray(_result4);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = isPartionWriteProtectDisabled();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = disablePartionWriteProtect(data.readInt() != 0);
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getBackCoverColorId();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = setBackCoverColorId(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getRpmbState();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getRpmbEnableState();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getCarrierVersion();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = setCarrierVersion(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getRegionNetlockStatus();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = setRegionNetlock(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getTelcelSimlockStatus();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = setTelcelSimlock(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getTelcelSimlockUnlockTimes();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = setTelcelSimlockUnlockTimes(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getSingleDoubleCardStatus();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = setSingleDoubleCard(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getBadBatteryConfig(data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeByteArray(_result4);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    _result = setBatteryBatteryConfig(data.readInt(), data.readInt(), data.createByteArray());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getProductLineTestResult();
                    reply.writeNoException();
                    reply.writeByteArray(_result4);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = setProductLineTestResult(data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = resetProductLineTestResult();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getEngResultFromNvram();
                    reply.writeNoException();
                    reply.writeByteArray(_result4);
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = saveEngResultToNvram(data.createByteArray());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getEncryptImeiFromNvram();
                    reply.writeNoException();
                    reply.writeByteArray(_result4);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getCarrierVersionFromNvram();
                    reply.writeNoException();
                    reply.writeByteArray(_result4);
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = saveCarrierVersionToNvram(data.createByteArray());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getCalibrationStatusFromNvram();
                    reply.writeNoException();
                    reply.writeByteArray(_result4);
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = setSimOperatorSwitch(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getSimOperatorSwitchStatus();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getBootImgWaterMark();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case IBinder.INTERFACE_TRANSACTION /*1598968902*/:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    int cameraHardwareCheck(int i, int i2) throws RemoteException;

    boolean disablePartionWriteProtect(boolean z) throws RemoteException;

    String getBackCoverColorId() throws RemoteException;

    byte[] getBadBatteryConfig(int i, int i2) throws RemoteException;

    String getBootImgWaterMark() throws RemoteException;

    byte[] getCalibrationStatusFromNvram() throws RemoteException;

    String getCarrierVersion() throws RemoteException;

    byte[] getCarrierVersionFromNvram() throws RemoteException;

    String getDownloadStatus() throws RemoteException;

    byte[] getEmmcHealthInfo() throws RemoteException;

    byte[] getEncryptImeiFromNvram() throws RemoteException;

    byte[] getEngResultFromNvram() throws RemoteException;

    byte[] getProductLineTestResult() throws RemoteException;

    String getRegionNetlockStatus() throws RemoteException;

    boolean getRpmbEnableState() throws RemoteException;

    boolean getRpmbState() throws RemoteException;

    String getSimOperatorSwitchStatus() throws RemoteException;

    String getSingleDoubleCardStatus() throws RemoteException;

    String getTelcelSimlockStatus() throws RemoteException;

    String getTelcelSimlockUnlockTimes() throws RemoteException;

    boolean isPartionWriteProtectDisabled() throws RemoteException;

    boolean isSerialPortEnabled() throws RemoteException;

    boolean resetProductLineTestResult() throws RemoteException;

    boolean saveCarrierVersionToNvram(byte[] bArr) throws RemoteException;

    boolean saveEngResultToNvram(byte[] bArr) throws RemoteException;

    boolean setBackCoverColorId(String str) throws RemoteException;

    int setBatteryBatteryConfig(int i, int i2, byte[] bArr) throws RemoteException;

    boolean setCarrierVersion(String str) throws RemoteException;

    boolean setProductLineTestResult(int i, int i2) throws RemoteException;

    boolean setRegionNetlock(String str) throws RemoteException;

    boolean setSerialPortState(boolean z) throws RemoteException;

    boolean setSimOperatorSwitch(String str) throws RemoteException;

    boolean setSingleDoubleCard(String str) throws RemoteException;

    boolean setTelcelSimlock(String str) throws RemoteException;

    boolean setTelcelSimlockUnlockTimes(String str) throws RemoteException;

    void setTorchState(String str) throws RemoteException;

    void turnBreathLightFlashOn(int i) throws RemoteException;

    void turnBreathLightOff() throws RemoteException;

    void turnBreathLightOn(int i) throws RemoteException;

    void turnButtonLightOff() throws RemoteException;

    void turnButtonLightOn(int i) throws RemoteException;
}
