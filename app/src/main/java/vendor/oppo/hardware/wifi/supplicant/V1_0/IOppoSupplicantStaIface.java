package vendor.oppo.hardware.wifi.supplicant.V1_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.addNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getNameCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getTypeCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.listNetworksCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.addExtRadioWorkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.getMacAddressCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.startWpsPinDisplayCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hidl.base.V1_0.DebugInfo;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwBinder.DeathRecipient;
import android.os.IHwInterface;
import android.os.RemoteException;
import android.os.SystemProperties;
import com.android.server.wifi.HalDeviceManager;
import com.android.server.wifi.WifiLoggerHal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import vendor.qti.hardware.wifi.supplicant.V1_0.ISupplicantVendorStaIface;

public interface IOppoSupplicantStaIface extends ISupplicantVendorStaIface {
    public static final String kInterfaceName = "vendor.oppo.hardware.wifi.supplicant@1.0::IOppoSupplicantStaIface";

    public static final class Proxy implements IOppoSupplicantStaIface {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.oppo.hardware.wifi.supplicant@1.0::IOppoSupplicantStaIface]@Proxy";
            }
        }

        public void getName(getNameCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getType(getTypeCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void addNetwork(addNetworkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, ISupplicantNetwork.asInterface(_hidl_reply.readStrongBinder()));
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeNetwork(int id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getNetwork(int id, getNetworkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, ISupplicantNetwork.asInterface(_hidl_reply.readStrongBinder()));
            } finally {
                _hidl_reply.release();
            }
        }

        public void listNetworks(listNetworksCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsDeviceName(String name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsDeviceType(byte[] type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(8);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 8; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, type[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsManufacturer(String manufacturer) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(manufacturer);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsModelName(String modelName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(modelName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsModelNumber(String modelNumber) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(modelNumber);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsSerialNumber(String serialNumber) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(serialNumber);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsConfigMethods(short configMethods) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt16(configMethods);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus registerCallback(ISupplicantStaIfaceCallback callback) throws RemoteException {
            IHwBinder iHwBinder = null;
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            if (callback != null) {
                iHwBinder = callback.asBinder();
            }
            _hidl_request.writeStrongBinder(iHwBinder);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reassociate() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reconnect() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(16, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus disconnect() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(17, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setPowerSave(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(18, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus initiateTdlsDiscover(byte[] macAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, macAddress[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(19, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus initiateTdlsSetup(byte[] macAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, macAddress[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(20, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus initiateTdlsTeardown(byte[] macAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, macAddress[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(21, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus initiateAnqpQuery(byte[] macAddress, ArrayList<Short> infoElements, ArrayList<Integer> subTypes) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, macAddress[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt16Vector(infoElements);
            _hidl_request.writeInt32Vector(subTypes);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(22, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus initiateHs20IconQuery(byte[] macAddress, String fileName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, macAddress[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeString(fileName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(23, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getMacAddress(getMacAddressCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(24, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                byte[] _hidl_out_macAddr = new byte[6];
                HwBlob _hidl_blob = _hidl_reply.readBuffer(6);
                long _hidl_array_offset_0 = 0;
                for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                    _hidl_out_macAddr[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                    _hidl_array_offset_0++;
                }
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_macAddr);
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus startRxFilter() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(25, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus stopRxFilter() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(26, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus addRxFilter(byte type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt8(type);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(27, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeRxFilter(byte type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt8(type);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(28, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setBtCoexistenceMode(byte mode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt8(mode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(29, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setBtCoexistenceScanModeEnabled(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(30, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setSuspendModeEnabled(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(31, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setCountryCode(byte[] code) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(2);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 2; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, code[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(32, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus startWpsRegistrar(byte[] bssid, String pin) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, bssid[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeString(pin);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(33, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus startWpsPbc(byte[] bssid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, bssid[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(34, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus startWpsPinKeypad(String pin) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeString(pin);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(35, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void startWpsPinDisplay(byte[] bssid, startWpsPinDisplayCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, bssid[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(36, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus cancelWps() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(37, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setExternalSim(boolean useExternalSim) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(useExternalSim);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(38, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void addExtRadioWork(String name, int freqInMhz, int timeoutInSec, addExtRadioWorkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeString(name);
            _hidl_request.writeInt32(freqInMhz);
            _hidl_request.writeInt32(timeoutInSec);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(39, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeExtRadioWork(int id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(40, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus enableAutoReconnect(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(41, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus filsHlpFlushRequest() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(42, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus filsHlpAddRequest(byte[] dst_mac, ArrayList<Byte> pkt) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, dst_mac[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt8Vector(pkt);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(43, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus manuReassociate() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IOppoSupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(44, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken("android.hidl.base@1.0::IBase");
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256067662, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<String> _hidl_out_descriptors = _hidl_reply.readStringVector();
                return _hidl_out_descriptors;
            } finally {
                _hidl_reply.release();
            }
        }

        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken("android.hidl.base@1.0::IBase");
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256136003, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken("android.hidl.base@1.0::IBase");
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256398152, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<byte[]> _hidl_out_hashchain = new ArrayList();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16);
                int _hidl_vec_size = _hidl_blob.getInt32(8);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer((long) (_hidl_vec_size * 32), _hidl_blob.handle(), 0, true);
                _hidl_out_hashchain.clear();
                for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                    Object _hidl_vec_element = new byte[32];
                    long _hidl_array_offset_1 = (long) (_hidl_index_0 * 32);
                    for (int _hidl_index_1_0 = 0; _hidl_index_1_0 < 32; _hidl_index_1_0++) {
                        _hidl_vec_element[_hidl_index_1_0] = childBlob.getInt8(_hidl_array_offset_1);
                        _hidl_array_offset_1++;
                    }
                    _hidl_out_hashchain.add(_hidl_vec_element);
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        public void setHALInstrumentation() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken("android.hidl.base@1.0::IBase");
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256462420, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean linkToDeath(DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        public void ping() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken("android.hidl.base@1.0::IBase");
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256921159, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken("android.hidl.base@1.0::IBase");
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257049926, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                DebugInfo _hidl_out_info = new DebugInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        public void notifySyspropsChanged() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken("android.hidl.base@1.0::IBase");
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257120595, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean unlinkToDeath(DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    public static abstract class Stub extends HwBinder implements IOppoSupplicantStaIface {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList(Arrays.asList(new String[]{IOppoSupplicantStaIface.kInterfaceName, ISupplicantVendorStaIface.kInterfaceName, ISupplicantStaIface.kInterfaceName, ISupplicantIface.kInterfaceName, "android.hidl.base@1.0::IBase"}));
        }

        public final String interfaceDescriptor() {
            return IOppoSupplicantStaIface.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList(Arrays.asList(new byte[][]{new byte[]{(byte) -46, (byte) -116, (byte) -21, (byte) -13, (byte) -14, (byte) -47, (byte) 40, (byte) -30, (byte) -83, (byte) 20, (byte) -3, (byte) 11, (byte) -73, (byte) -56, (byte) 35, (byte) -110, (byte) -28, (byte) 71, (byte) -62, (byte) -77, (byte) 58, (byte) 4, (byte) -24, (byte) 18, (byte) 23, (byte) 55, (byte) -91, (byte) 112, (byte) -87, (byte) -6, (byte) 23, (byte) -55}, new byte[]{(byte) 68, (byte) -10, WifiLoggerHal.WIFI_ALERT_REASON_MAX, (byte) -119, (byte) -18, (byte) 125, (byte) -88, (byte) 9, (byte) -35, (byte) 11, (byte) 122, (byte) 112, (byte) -111, (byte) -89, (byte) 117, (byte) 99, (byte) -12, (byte) 4, (byte) 41, (byte) 24, (byte) -11, (byte) -89, (byte) 47, (byte) -65, (byte) 42, (byte) -33, (byte) 9, (byte) 19, (byte) -9, (byte) 57, (byte) -40, Byte.MAX_VALUE}, new byte[]{(byte) 119, (byte) 82, (byte) -31, (byte) -34, (byte) -109, (byte) -86, (byte) -11, (byte) -2, (byte) -45, (byte) 112, (byte) 17, (byte) -62, (byte) 25, (byte) -84, (byte) 36, (byte) 112, (byte) 105, (byte) -10, (byte) -81, (byte) 50, (byte) 11, (byte) 8, (byte) 16, (byte) -38, (byte) -87, (byte) -123, (byte) 16, (byte) 88, (byte) 74, (byte) 16, (byte) -25, (byte) -76}, new byte[]{(byte) 53, (byte) -70, (byte) 123, (byte) -51, (byte) -15, (byte) -113, (byte) 36, (byte) -88, (byte) 102, (byte) -89, (byte) -27, (byte) 66, (byte) -107, (byte) 72, (byte) -16, (byte) 103, (byte) 104, (byte) -69, (byte) 32, (byte) -94, (byte) 87, (byte) -9, (byte) 91, (byte) 16, (byte) -93, (byte) -105, (byte) -60, (byte) -40, (byte) 37, (byte) -17, (byte) -124, (byte) 56}, new byte[]{(byte) -67, (byte) -38, (byte) -74, (byte) 24, (byte) 77, (byte) 122, (byte) 52, (byte) 109, (byte) -90, (byte) -96, (byte) 125, (byte) -64, (byte) -126, (byte) -116, (byte) -15, (byte) -102, (byte) 105, (byte) 111, (byte) 76, (byte) -86, (byte) 54, (byte) 17, (byte) -59, (byte) 31, (byte) 46, (byte) 20, (byte) 86, (byte) 90, (byte) 20, (byte) -76, (byte) 15, (byte) -39}}));
        }

        public final void setHALInstrumentation() {
        }

        public final boolean linkToDeath(DeathRecipient recipient, long cookie) {
            return true;
        }

        public final void ping() {
        }

        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = -1;
            info.ptr = 0;
            info.arch = 0;
            return info;
        }

        public final void notifySyspropsChanged() {
            SystemProperties.reportSyspropChanged();
        }

        public final boolean unlinkToDeath(DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IOppoSupplicantStaIface.kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws RemoteException {
            registerService(serviceName);
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        public void onTransact(int _hidl_code, HwParcel _hidl_request, HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            final HwParcel hwParcel;
            SupplicantStatus _hidl_out_status;
            HwBlob _hidl_blob;
            long _hidl_array_offset_0;
            int _hidl_index_0_0;
            byte[] macAddress;
            byte[] bssid;
            switch (_hidl_code) {
                case 1:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getName(new getNameCallback() {
                        public void onValues(SupplicantStatus status, String name) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(name);
                            hwParcel.send();
                        }
                    });
                    return;
                case 2:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getType(new getTypeCallback() {
                        public void onValues(SupplicantStatus status, int type) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(type);
                            hwParcel.send();
                        }
                    });
                    return;
                case 3:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    hwParcel = _hidl_reply;
                    addNetwork(new addNetworkCallback() {
                        public void onValues(SupplicantStatus status, ISupplicantNetwork network) {
                            IHwBinder iHwBinder = null;
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            HwParcel hwParcel = hwParcel;
                            if (network != null) {
                                iHwBinder = network.asBinder();
                            }
                            hwParcel.writeStrongBinder(iHwBinder);
                            hwParcel.send();
                        }
                    });
                    return;
                case 4:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = removeNetwork(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getNetwork(_hidl_request.readInt32(), new getNetworkCallback() {
                        public void onValues(SupplicantStatus status, ISupplicantNetwork network) {
                            IHwBinder iHwBinder = null;
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            HwParcel hwParcel = hwParcel;
                            if (network != null) {
                                iHwBinder = network.asBinder();
                            }
                            hwParcel.writeStrongBinder(iHwBinder);
                            hwParcel.send();
                        }
                    });
                    return;
                case 6:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    hwParcel = _hidl_reply;
                    listNetworks(new listNetworksCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Integer> networkIds) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32Vector(networkIds);
                            hwParcel.send();
                        }
                    });
                    return;
                case 7:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsDeviceName(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    byte[] type = new byte[8];
                    _hidl_blob = _hidl_request.readBuffer(8);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 8; _hidl_index_0_0++) {
                        type[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = setWpsDeviceType(type);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsManufacturer(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsModelName(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 11:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsModelNumber(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 12:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsSerialNumber(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 13:
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsConfigMethods(_hidl_request.readInt16());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 14:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = registerCallback(ISupplicantStaIfaceCallback.asInterface(_hidl_request.readStrongBinder()));
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 15:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = reassociate();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 16:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = reconnect();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 17:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = disconnect();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 18:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = setPowerSave(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 19:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    macAddress = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        macAddress[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = initiateTdlsDiscover(macAddress);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 20:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    macAddress = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        macAddress[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = initiateTdlsSetup(macAddress);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ReasonCode.UNSUPPORTED_RSN_IE_VERSION /*21*/:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    macAddress = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        macAddress[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = initiateTdlsTeardown(macAddress);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 22:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    macAddress = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        macAddress[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = initiateAnqpQuery(macAddress, _hidl_request.readInt16Vector(), _hidl_request.readInt32Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 23:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    macAddress = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        macAddress[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = initiateHs20IconQuery(macAddress, _hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 24:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getMacAddress(new getMacAddressCallback() {
                        public void onValues(SupplicantStatus status, byte[] macAddr) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            HwBlob _hidl_blob = new HwBlob(6);
                            long _hidl_array_offset_0 = 0;
                            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                                _hidl_blob.putInt8(_hidl_array_offset_0, macAddr[_hidl_index_0_0]);
                                _hidl_array_offset_0++;
                            }
                            hwParcel.writeBuffer(_hidl_blob);
                            hwParcel.send();
                        }
                    });
                    return;
                case 25:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = startRxFilter();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ReasonCode.TDLS_TEARDOWN_UNSPECIFIED /*26*/:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = stopRxFilter();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 27:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = addRxFilter(_hidl_request.readInt8());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 28:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = removeRxFilter(_hidl_request.readInt8());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 29:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = setBtCoexistenceMode(_hidl_request.readInt8());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 30:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = setBtCoexistenceScanModeEnabled(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 31:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = setSuspendModeEnabled(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 32:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    byte[] code = new byte[2];
                    _hidl_blob = _hidl_request.readBuffer(2);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 2; _hidl_index_0_0++) {
                        code[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = setCountryCode(code);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 33:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    bssid = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        bssid[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = startWpsRegistrar(bssid, _hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 34:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    bssid = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        bssid[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = startWpsPbc(bssid);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 35:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = startWpsPinKeypad(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ReasonCode.STA_LEAVING /*36*/:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    bssid = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        bssid[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    hwParcel = _hidl_reply;
                    startWpsPinDisplay(bssid, new startWpsPinDisplayCallback() {
                        public void onValues(SupplicantStatus status, String generatedPin) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(generatedPin);
                            hwParcel.send();
                        }
                    });
                    return;
                case 37:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = cancelWps();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 38:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = setExternalSim(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 39:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    hwParcel = _hidl_reply;
                    addExtRadioWork(_hidl_request.readString(), _hidl_request.readInt32(), _hidl_request.readInt32(), new addExtRadioWorkCallback() {
                        public void onValues(SupplicantStatus status, int id) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(id);
                            hwParcel.send();
                        }
                    });
                    return;
                case 40:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = removeExtRadioWork(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.GROUP_CIPHER_NOT_VALID /*41*/:
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    _hidl_out_status = enableAutoReconnect(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 42:
                    _hidl_request.enforceInterface(ISupplicantVendorStaIface.kInterfaceName);
                    _hidl_out_status = filsHlpFlushRequest();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.AKMP_NOT_VALID /*43*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaIface.kInterfaceName);
                    byte[] dst_mac = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        dst_mac[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = filsHlpAddRequest(dst_mac, _hidl_request.readInt8Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.UNSUPPORTED_RSN_IE_VERSION /*44*/:
                    _hidl_request.enforceInterface(IOppoSupplicantStaIface.kInterfaceName);
                    _hidl_out_status = manuReassociate();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 256067662:
                    _hidl_request.enforceInterface("android.hidl.base@1.0::IBase");
                    ArrayList<String> _hidl_out_descriptors = interfaceChain();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStringVector(_hidl_out_descriptors);
                    _hidl_reply.send();
                    return;
                case 256131655:
                    _hidl_request.enforceInterface("android.hidl.base@1.0::IBase");
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 256136003:
                    _hidl_request.enforceInterface("android.hidl.base@1.0::IBase");
                    String _hidl_out_descriptor = interfaceDescriptor();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeString(_hidl_out_descriptor);
                    _hidl_reply.send();
                    return;
                case 256398152:
                    _hidl_request.enforceInterface("android.hidl.base@1.0::IBase");
                    ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                    _hidl_reply.writeStatus(0);
                    _hidl_blob = new HwBlob(16);
                    int _hidl_vec_size = _hidl_out_hashchain.size();
                    _hidl_blob.putInt32(8, _hidl_vec_size);
                    _hidl_blob.putBool(12, false);
                    HwBlob hwBlob = new HwBlob(_hidl_vec_size * 32);
                    for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                        long _hidl_array_offset_1 = (long) (_hidl_index_0 * 32);
                        for (int _hidl_index_1_0 = 0; _hidl_index_1_0 < 32; _hidl_index_1_0++) {
                            hwBlob.putInt8(_hidl_array_offset_1, ((byte[]) _hidl_out_hashchain.get(_hidl_index_0))[_hidl_index_1_0]);
                            _hidl_array_offset_1++;
                        }
                    }
                    _hidl_blob.putBlob(0, hwBlob);
                    _hidl_reply.writeBuffer(_hidl_blob);
                    _hidl_reply.send();
                    return;
                case 256462420:
                    _hidl_request.enforceInterface("android.hidl.base@1.0::IBase");
                    setHALInstrumentation();
                    return;
                case 257049926:
                    _hidl_request.enforceInterface("android.hidl.base@1.0::IBase");
                    DebugInfo _hidl_out_info = getDebugInfo();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_info.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 257120595:
                    _hidl_request.enforceInterface("android.hidl.base@1.0::IBase");
                    notifySyspropsChanged();
                    return;
                default:
                    return;
            }
        }
    }

    IHwBinder asBinder();

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(DeathRecipient deathRecipient, long j) throws RemoteException;

    SupplicantStatus manuReassociate() throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient) throws RemoteException;

    static IOppoSupplicantStaIface asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IOppoSupplicantStaIface)) {
            return (IOppoSupplicantStaIface) iface;
        }
        IOppoSupplicantStaIface proxy = new Proxy(binder);
        try {
            for (String descriptor : proxy.interfaceChain()) {
                if (descriptor.equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static IOppoSupplicantStaIface castFrom(IHwInterface iface) {
        return iface == null ? null : asInterface(iface.asBinder());
    }

    static IOppoSupplicantStaIface getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IOppoSupplicantStaIface getService() throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, HalDeviceManager.HAL_INSTANCE_NAME));
    }
}
