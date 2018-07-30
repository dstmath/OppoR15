package vendor.qti.hardware.wifi.supplicant.V1_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork.getIdCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork.getInterfaceNameCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork.getTypeCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getAuthAlgCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getBssidCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapAltSubjectMatchCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapAnonymousIdentityCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapCACertCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapCAPathCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapClientCertCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapDomainSuffixMatchCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapEngineCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapEngineIDCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapIdentityCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapMethodCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapPasswordCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapPhase2MethodCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapPrivateKeyIdCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapSubjectMatchCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getGroupCipherCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getIdStrCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getKeyMgmtCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getPairwiseCipherCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getProtoCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getPskCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getPskPassphraseCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getRequirePmfCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getScanSsidCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getSsidCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getWepKeyCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getWepTxKeyIdxCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getWpsNfcConfigurationTokenCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public interface ISupplicantVendorStaNetwork extends ISupplicantStaNetwork {
    public static final String kInterfaceName = "vendor.qti.hardware.wifi.supplicant@1.0::ISupplicantVendorStaNetwork";

    @FunctionalInterface
    public interface getWapiCertSelCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getWapiCertSelModeCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getWapiPskCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getWapiPskTypeCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    public static final class Proxy implements ISupplicantVendorStaNetwork {
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
                return "[class or subclass of vendor.qti.hardware.wifi.supplicant@1.0::ISupplicantVendorStaNetwork]@Proxy";
            }
        }

        public void getId(getIdCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getInterfaceName(getInterfaceNameCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
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
            _hidl_request.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus registerCallback(ISupplicantStaNetworkCallback callback) throws RemoteException {
            IHwBinder iHwBinder = null;
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            if (callback != null) {
                iHwBinder = callback.asBinder();
            }
            _hidl_request.writeStrongBinder(iHwBinder);
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

        public SupplicantStatus setSsid(ArrayList<Byte> ssid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt8Vector(ssid);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setBssid(byte[] bssid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, bssid[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setScanSsid(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeBool(enable);
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

        public SupplicantStatus setKeyMgmt(int keyMgmtMask) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(keyMgmtMask);
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

        public SupplicantStatus setProto(int protoMask) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(protoMask);
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

        public SupplicantStatus setAuthAlg(int authAlgMask) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(authAlgMask);
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

        public SupplicantStatus setGroupCipher(int groupCipherMask) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(groupCipherMask);
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

        public SupplicantStatus setPairwiseCipher(int pairwiseCipherMask) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(pairwiseCipherMask);
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

        public SupplicantStatus setPskPassphrase(String psk) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(psk);
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

        public SupplicantStatus setPsk(byte[] psk) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(32);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 32; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, psk[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
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

        public SupplicantStatus setWepKey(int keyIdx, ArrayList<Byte> wepKey) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(keyIdx);
            _hidl_request.writeInt8Vector(wepKey);
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

        public SupplicantStatus setWepTxKeyIdx(int keyIdx) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(keyIdx);
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

        public SupplicantStatus setRequirePmf(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeBool(enable);
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

        public SupplicantStatus setEapMethod(int method) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(method);
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

        public SupplicantStatus setEapPhase2Method(int method) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(method);
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

        public SupplicantStatus setEapIdentity(ArrayList<Byte> identity) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt8Vector(identity);
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

        public SupplicantStatus setEapAnonymousIdentity(ArrayList<Byte> identity) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt8Vector(identity);
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

        public SupplicantStatus setEapPassword(ArrayList<Byte> password) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt8Vector(password);
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

        public SupplicantStatus setEapCACert(String path) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(path);
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

        public SupplicantStatus setEapCAPath(String path) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(path);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(24, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setEapClientCert(String path) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(path);
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

        public SupplicantStatus setEapPrivateKeyId(String id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(id);
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

        public SupplicantStatus setEapSubjectMatch(String match) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(match);
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

        public SupplicantStatus setEapAltSubjectMatch(String match) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(match);
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

        public SupplicantStatus setEapEngine(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeBool(enable);
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

        public SupplicantStatus setEapEngineID(String id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(id);
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

        public SupplicantStatus setEapDomainSuffixMatch(String match) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(match);
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

        public SupplicantStatus setProactiveKeyCaching(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeBool(enable);
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

        public SupplicantStatus setIdStr(String idStr) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(idStr);
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

        public SupplicantStatus setUpdateIdentifier(int id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(id);
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

        public void getSsid(getSsidCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(35, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getBssid(getBssidCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(36, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                byte[] _hidl_out_bssid = new byte[6];
                HwBlob _hidl_blob = _hidl_reply.readBuffer(6);
                long _hidl_array_offset_0 = 0;
                for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                    _hidl_out_bssid[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                    _hidl_array_offset_0++;
                }
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_bssid);
            } finally {
                _hidl_reply.release();
            }
        }

        public void getScanSsid(getScanSsidCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(37, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readBool());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getKeyMgmt(getKeyMgmtCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(38, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getProto(getProtoCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
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

        public void getAuthAlg(getAuthAlgCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(40, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getGroupCipher(getGroupCipherCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(41, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getPairwiseCipher(getPairwiseCipherCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(42, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getPskPassphrase(getPskPassphraseCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(43, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getPsk(getPskCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(44, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                byte[] _hidl_out_psk = new byte[32];
                HwBlob _hidl_blob = _hidl_reply.readBuffer(32);
                long _hidl_array_offset_0 = 0;
                for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 32; _hidl_index_0_0++) {
                    _hidl_out_psk[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                    _hidl_array_offset_0++;
                }
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_psk);
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWepKey(int keyIdx, getWepKeyCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(keyIdx);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(45, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWepTxKeyIdx(getWepTxKeyIdxCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(46, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getRequirePmf(getRequirePmfCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(47, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readBool());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapMethod(getEapMethodCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(48, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapPhase2Method(getEapPhase2MethodCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(49, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapIdentity(getEapIdentityCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(50, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapAnonymousIdentity(getEapAnonymousIdentityCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(51, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapPassword(getEapPasswordCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(52, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapCACert(getEapCACertCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(53, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapCAPath(getEapCAPathCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(54, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapClientCert(getEapClientCertCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(55, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapPrivateKeyId(getEapPrivateKeyIdCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(56, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapSubjectMatch(getEapSubjectMatchCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(57, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapAltSubjectMatch(getEapAltSubjectMatchCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(58, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapEngine(getEapEngineCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(59, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readBool());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapEngineID(getEapEngineIDCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(60, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapDomainSuffixMatch(getEapDomainSuffixMatchCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(61, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getIdStr(getIdStrCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(62, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWpsNfcConfigurationToken(getWpsNfcConfigurationTokenCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(63, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus enable(boolean noConnect) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeBool(noConnect);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(64, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus disable() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(65, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus select() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(66, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus sendNetworkEapSimGsmAuthResponse(ArrayList<NetworkResponseEapSimGsmAuthParams> params) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            NetworkResponseEapSimGsmAuthParams.writeVectorToParcel(_hidl_request, params);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(67, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus sendNetworkEapSimGsmAuthFailure() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(68, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus sendNetworkEapSimUmtsAuthResponse(NetworkResponseEapSimUmtsAuthParams params) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            params.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(69, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus sendNetworkEapSimUmtsAutsResponse(byte[] auts) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(14);
            long _hidl_array_offset_0 = 0;
            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 14; _hidl_index_0_0++) {
                _hidl_blob.putInt8(_hidl_array_offset_0, auts[_hidl_index_0_0]);
                _hidl_array_offset_0++;
            }
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(70, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus sendNetworkEapSimUmtsAuthFailure() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(71, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus sendNetworkEapIdentityResponse(ArrayList<Byte> identity) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt8Vector(identity);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(72, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWapiPskType(int type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(type);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(73, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWapiPsk(String psk) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            _hidl_request.writeString(psk);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(74, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWapiCertSelMode(int mode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(mode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(75, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWapiCertSel(String name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            _hidl_request.writeString(name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(76, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWapiPskType(getWapiPskTypeCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(77, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWapiPsk(getWapiPskCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(78, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWapiCertSelMode(getWapiCertSelModeCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(79, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWapiCertSel(getWapiCertSelCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(80, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setEapErp(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(81, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setSimNumber(int sim_num) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantVendorStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(sim_num);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(82, _hidl_request, _hidl_reply, 0);
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

    public static abstract class Stub extends HwBinder implements ISupplicantVendorStaNetwork {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList(Arrays.asList(new String[]{ISupplicantVendorStaNetwork.kInterfaceName, ISupplicantStaNetwork.kInterfaceName, ISupplicantNetwork.kInterfaceName, "android.hidl.base@1.0::IBase"}));
        }

        public final String interfaceDescriptor() {
            return ISupplicantVendorStaNetwork.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList(Arrays.asList(new byte[][]{new byte[]{(byte) -75, (byte) 28, (byte) -117, (byte) -77, (byte) -14, (byte) -41, (byte) -62, (byte) -31, (byte) -54, (byte) 123, (byte) -85, (byte) 99, (byte) -27, (byte) -29, (byte) -16, (byte) 51, (byte) -6, (byte) -108, (byte) 20, (byte) -34, (byte) -37, (byte) -55, (byte) 46, (byte) 110, (byte) -16, (byte) 7, (byte) -55, (byte) 70, (byte) -118, (byte) 114, (byte) 59, (byte) -95}, new byte[]{(byte) -79, (byte) 46, (byte) -16, (byte) -67, (byte) -40, (byte) -92, (byte) -46, (byte) 71, (byte) -88, (byte) -90, (byte) -23, (byte) 96, (byte) -78, (byte) 39, (byte) -19, (byte) 50, (byte) 56, (byte) 63, (byte) 43, (byte) 2, (byte) 65, (byte) -11, (byte) 93, (byte) 103, (byte) -4, (byte) -22, (byte) 110, (byte) -1, (byte) 106, (byte) 103, (byte) 55, (byte) -6}, new byte[]{(byte) -51, (byte) -96, (byte) 16, (byte) 8, (byte) -64, (byte) 105, (byte) 34, (byte) -6, (byte) 55, (byte) -63, (byte) 33, (byte) 62, (byte) -101, (byte) -72, (byte) 49, (byte) -95, (byte) 9, (byte) -77, (byte) 23, (byte) 69, (byte) 50, Byte.MIN_VALUE, (byte) 86, (byte) 22, (byte) -5, (byte) 113, (byte) 97, (byte) -19, (byte) -60, (byte) 3, (byte) -122, (byte) 111}, new byte[]{(byte) -67, (byte) -38, (byte) -74, (byte) 24, (byte) 77, (byte) 122, (byte) 52, (byte) 109, (byte) -90, (byte) -96, (byte) 125, (byte) -64, (byte) -126, (byte) -116, (byte) -15, (byte) -102, (byte) 105, (byte) 111, (byte) 76, (byte) -86, (byte) 54, (byte) 17, (byte) -59, (byte) 31, (byte) 46, (byte) 20, (byte) 86, (byte) 90, (byte) 20, (byte) -76, (byte) 15, (byte) -39}}));
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
            if (ISupplicantVendorStaNetwork.kInterfaceName.equals(descriptor)) {
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
            switch (_hidl_code) {
                case 1:
                    _hidl_request.enforceInterface(ISupplicantNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getId(new getIdCallback() {
                        public void onValues(SupplicantStatus status, int id) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(id);
                            hwParcel.send();
                        }
                    });
                    return;
                case 2:
                    _hidl_request.enforceInterface(ISupplicantNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getInterfaceName(new getInterfaceNameCallback() {
                        public void onValues(SupplicantStatus status, String name) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(name);
                            hwParcel.send();
                        }
                    });
                    return;
                case 3:
                    _hidl_request.enforceInterface(ISupplicantNetwork.kInterfaceName);
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
                case 4:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = registerCallback(ISupplicantStaNetworkCallback.asInterface(_hidl_request.readStrongBinder()));
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setSsid(_hidl_request.readInt8Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    byte[] bssid = new byte[6];
                    _hidl_blob = _hidl_request.readBuffer(6);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                        bssid[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = setBssid(bssid);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setScanSsid(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setKeyMgmt(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setProto(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setAuthAlg(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 11:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setGroupCipher(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 12:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setPairwiseCipher(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 13:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setPskPassphrase(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 14:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    byte[] psk = new byte[32];
                    _hidl_blob = _hidl_request.readBuffer(32);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 32; _hidl_index_0_0++) {
                        psk[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = setPsk(psk);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 15:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setWepKey(_hidl_request.readInt32(), _hidl_request.readInt8Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 16:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setWepTxKeyIdx(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 17:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setRequirePmf(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 18:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapMethod(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 19:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapPhase2Method(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 20:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapIdentity(_hidl_request.readInt8Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ReasonCode.UNSUPPORTED_RSN_IE_VERSION /*21*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapAnonymousIdentity(_hidl_request.readInt8Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 22:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapPassword(_hidl_request.readInt8Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 23:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapCACert(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 24:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapCAPath(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 25:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapClientCert(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ReasonCode.TDLS_TEARDOWN_UNSPECIFIED /*26*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapPrivateKeyId(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 27:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapSubjectMatch(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 28:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapAltSubjectMatch(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 29:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapEngine(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 30:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapEngineID(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 31:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapDomainSuffixMatch(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 32:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setProactiveKeyCaching(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 33:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setIdStr(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 34:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = setUpdateIdentifier(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 35:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getSsid(new getSsidCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> ssid) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt8Vector(ssid);
                            hwParcel.send();
                        }
                    });
                    return;
                case ReasonCode.STA_LEAVING /*36*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getBssid(new getBssidCallback() {
                        public void onValues(SupplicantStatus status, byte[] bssid) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            HwBlob _hidl_blob = new HwBlob(6);
                            long _hidl_array_offset_0 = 0;
                            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 6; _hidl_index_0_0++) {
                                _hidl_blob.putInt8(_hidl_array_offset_0, bssid[_hidl_index_0_0]);
                                _hidl_array_offset_0++;
                            }
                            hwParcel.writeBuffer(_hidl_blob);
                            hwParcel.send();
                        }
                    });
                    return;
                case 37:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getScanSsid(new getScanSsidCallback() {
                        public void onValues(SupplicantStatus status, boolean enabled) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeBool(enabled);
                            hwParcel.send();
                        }
                    });
                    return;
                case 38:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getKeyMgmt(new getKeyMgmtCallback() {
                        public void onValues(SupplicantStatus status, int keyMgmtMask) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(keyMgmtMask);
                            hwParcel.send();
                        }
                    });
                    return;
                case 39:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getProto(new getProtoCallback() {
                        public void onValues(SupplicantStatus status, int protoMask) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(protoMask);
                            hwParcel.send();
                        }
                    });
                    return;
                case 40:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getAuthAlg(new getAuthAlgCallback() {
                        public void onValues(SupplicantStatus status, int authAlgMask) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(authAlgMask);
                            hwParcel.send();
                        }
                    });
                    return;
                case StatusCode.GROUP_CIPHER_NOT_VALID /*41*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getGroupCipher(new getGroupCipherCallback() {
                        public void onValues(SupplicantStatus status, int groupCipherMask) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(groupCipherMask);
                            hwParcel.send();
                        }
                    });
                    return;
                case 42:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getPairwiseCipher(new getPairwiseCipherCallback() {
                        public void onValues(SupplicantStatus status, int pairwiseCipherMask) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(pairwiseCipherMask);
                            hwParcel.send();
                        }
                    });
                    return;
                case StatusCode.AKMP_NOT_VALID /*43*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getPskPassphrase(new getPskPassphraseCallback() {
                        public void onValues(SupplicantStatus status, String psk) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(psk);
                            hwParcel.send();
                        }
                    });
                    return;
                case StatusCode.UNSUPPORTED_RSN_IE_VERSION /*44*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getPsk(new getPskCallback() {
                        public void onValues(SupplicantStatus status, byte[] psk) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            HwBlob _hidl_blob = new HwBlob(32);
                            long _hidl_array_offset_0 = 0;
                            for (int _hidl_index_0_0 = 0; _hidl_index_0_0 < 32; _hidl_index_0_0++) {
                                _hidl_blob.putInt8(_hidl_array_offset_0, psk[_hidl_index_0_0]);
                                _hidl_array_offset_0++;
                            }
                            hwParcel.writeBuffer(_hidl_blob);
                            hwParcel.send();
                        }
                    });
                    return;
                case 45:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getWepKey(_hidl_request.readInt32(), new getWepKeyCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> wepKey) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt8Vector(wepKey);
                            hwParcel.send();
                        }
                    });
                    return;
                case 46:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getWepTxKeyIdx(new getWepTxKeyIdxCallback() {
                        public void onValues(SupplicantStatus status, int keyIdx) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(keyIdx);
                            hwParcel.send();
                        }
                    });
                    return;
                case 47:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getRequirePmf(new getRequirePmfCallback() {
                        public void onValues(SupplicantStatus status, boolean enabled) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeBool(enabled);
                            hwParcel.send();
                        }
                    });
                    return;
                case 48:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapMethod(new getEapMethodCallback() {
                        public void onValues(SupplicantStatus status, int method) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(method);
                            hwParcel.send();
                        }
                    });
                    return;
                case 49:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapPhase2Method(new getEapPhase2MethodCallback() {
                        public void onValues(SupplicantStatus status, int method) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(method);
                            hwParcel.send();
                        }
                    });
                    return;
                case 50:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapIdentity(new getEapIdentityCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> identity) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt8Vector(identity);
                            hwParcel.send();
                        }
                    });
                    return;
                case 51:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapAnonymousIdentity(new getEapAnonymousIdentityCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> identity) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt8Vector(identity);
                            hwParcel.send();
                        }
                    });
                    return;
                case 52:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapPassword(new getEapPasswordCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> password) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt8Vector(password);
                            hwParcel.send();
                        }
                    });
                    return;
                case 53:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapCACert(new getEapCACertCallback() {
                        public void onValues(SupplicantStatus status, String path) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(path);
                            hwParcel.send();
                        }
                    });
                    return;
                case 54:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapCAPath(new getEapCAPathCallback() {
                        public void onValues(SupplicantStatus status, String path) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(path);
                            hwParcel.send();
                        }
                    });
                    return;
                case 55:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapClientCert(new getEapClientCertCallback() {
                        public void onValues(SupplicantStatus status, String path) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(path);
                            hwParcel.send();
                        }
                    });
                    return;
                case 56:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapPrivateKeyId(new getEapPrivateKeyIdCallback() {
                        public void onValues(SupplicantStatus status, String id) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(id);
                            hwParcel.send();
                        }
                    });
                    return;
                case 57:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapSubjectMatch(new getEapSubjectMatchCallback() {
                        public void onValues(SupplicantStatus status, String match) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(match);
                            hwParcel.send();
                        }
                    });
                    return;
                case 58:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapAltSubjectMatch(new getEapAltSubjectMatchCallback() {
                        public void onValues(SupplicantStatus status, String match) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(match);
                            hwParcel.send();
                        }
                    });
                    return;
                case 59:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapEngine(new getEapEngineCallback() {
                        public void onValues(SupplicantStatus status, boolean enabled) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeBool(enabled);
                            hwParcel.send();
                        }
                    });
                    return;
                case 60:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapEngineID(new getEapEngineIDCallback() {
                        public void onValues(SupplicantStatus status, String id) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(id);
                            hwParcel.send();
                        }
                    });
                    return;
                case 61:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getEapDomainSuffixMatch(new getEapDomainSuffixMatchCallback() {
                        public void onValues(SupplicantStatus status, String match) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(match);
                            hwParcel.send();
                        }
                    });
                    return;
                case 62:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getIdStr(new getIdStrCallback() {
                        public void onValues(SupplicantStatus status, String idStr) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(idStr);
                            hwParcel.send();
                        }
                    });
                    return;
                case 63:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getWpsNfcConfigurationToken(new getWpsNfcConfigurationTokenCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> token) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt8Vector(token);
                            hwParcel.send();
                        }
                    });
                    return;
                case 64:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = enable(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 65:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = disable();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ReasonCode.MESH_CHANNEL_SWITCH_UNSPECIFIED /*66*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = select();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.REQ_REFUSED_SSPN /*67*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = sendNetworkEapSimGsmAuthResponse(NetworkResponseEapSimGsmAuthParams.readVectorFromParcel(_hidl_request));
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.REQ_REFUSED_UNAUTH_ACCESS /*68*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = sendNetworkEapSimGsmAuthFailure();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 69:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    NetworkResponseEapSimUmtsAuthParams params = new NetworkResponseEapSimUmtsAuthParams();
                    params.readFromParcel(_hidl_request);
                    _hidl_out_status = sendNetworkEapSimUmtsAuthResponse(params);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 70:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    byte[] auts = new byte[14];
                    _hidl_blob = _hidl_request.readBuffer(14);
                    _hidl_array_offset_0 = 0;
                    for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 14; _hidl_index_0_0++) {
                        auts[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
                        _hidl_array_offset_0++;
                    }
                    _hidl_out_status = sendNetworkEapSimUmtsAutsResponse(auts);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 71:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = sendNetworkEapSimUmtsAuthFailure();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.INVALID_RSNIE /*72*/:
                    _hidl_request.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                    _hidl_out_status = sendNetworkEapIdentityResponse(_hidl_request.readInt8Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.U_APSD_COEX_NOT_SUPPORTED /*73*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    _hidl_out_status = setWapiPskType(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.U_APSD_COEX_MODE_NOT_SUPPORTED /*74*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    _hidl_out_status = setWapiPsk(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.BAD_INTERVAL_WITH_U_APSD_COEX /*75*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    _hidl_out_status = setWapiCertSelMode(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.ANTI_CLOGGING_TOKEN_REQ /*76*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    _hidl_out_status = setWapiCertSel(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.FINITE_CYCLIC_GROUP_NOT_SUPPORTED /*77*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getWapiPskType(new getWapiPskTypeCallback() {
                        public void onValues(SupplicantStatus status, int type) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(type);
                            hwParcel.send();
                        }
                    });
                    return;
                case StatusCode.CANNOT_FIND_ALT_TBTT /*78*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getWapiPsk(new getWapiPskCallback() {
                        public void onValues(SupplicantStatus status, String psk) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(psk);
                            hwParcel.send();
                        }
                    });
                    return;
                case StatusCode.TRANSMISSION_FAILURE /*79*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getWapiCertSelMode(new getWapiCertSelModeCallback() {
                        public void onValues(SupplicantStatus status, int mode) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeInt32(mode);
                            hwParcel.send();
                        }
                    });
                    return;
                case 80:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    hwParcel = _hidl_reply;
                    getWapiCertSel(new getWapiCertSelCallback() {
                        public void onValues(SupplicantStatus status, String name) {
                            hwParcel.writeStatus(0);
                            status.writeToParcel(hwParcel);
                            hwParcel.writeString(name);
                            hwParcel.send();
                        }
                    });
                    return;
                case StatusCode.TCLAS_RESOURCES_EXCHAUSTED /*81*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    _hidl_out_status = setEapErp(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case StatusCode.REJECTED_WITH_SUGGESTED_BSS_TRANSITION /*82*/:
                    _hidl_request.enforceInterface(ISupplicantVendorStaNetwork.kInterfaceName);
                    _hidl_out_status = setSimNumber(_hidl_request.readInt32());
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

    public static final class VendorAuthAlgMask {
        public static final int FILS_SK = 32;

        public static final String toString(int o) {
            if (o == 32) {
                return "FILS_SK";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList();
            int flipped = 0;
            if ((o & 32) == 32) {
                list.add("FILS_SK");
                flipped = 32;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class VendorKeyMgmtMask {
        public static final int FILS_SHA256 = 262144;
        public static final int FILS_SHA384 = 524288;
        public static final int WAPI_CERT = 8192;
        public static final int WAPI_PSK = 4096;

        public static final String toString(int o) {
            if (o == 4096) {
                return "WAPI_PSK";
            }
            if (o == 8192) {
                return "WAPI_CERT";
            }
            if (o == FILS_SHA256) {
                return "FILS_SHA256";
            }
            if (o == FILS_SHA384) {
                return "FILS_SHA384";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList();
            int flipped = 0;
            if ((o & 4096) == 4096) {
                list.add("WAPI_PSK");
                flipped = 4096;
            }
            if ((o & 8192) == 8192) {
                list.add("WAPI_CERT");
                flipped |= 8192;
            }
            if ((o & FILS_SHA256) == FILS_SHA256) {
                list.add("FILS_SHA256");
                flipped |= FILS_SHA256;
            }
            if ((o & FILS_SHA384) == FILS_SHA384) {
                list.add("FILS_SHA384");
                flipped |= FILS_SHA384;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class VendorProtoMask {
        public static final int WAPI = 4;

        public static final String toString(int o) {
            if (o == 4) {
                return "WAPI";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList();
            int flipped = 0;
            if ((o & 4) == 4) {
                list.add("WAPI");
                flipped = 4;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    IHwBinder asBinder();

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getWapiCertSel(getWapiCertSelCallback getwapicertselcallback) throws RemoteException;

    void getWapiCertSelMode(getWapiCertSelModeCallback getwapicertselmodecallback) throws RemoteException;

    void getWapiPsk(getWapiPskCallback getwapipskcallback) throws RemoteException;

    void getWapiPskType(getWapiPskTypeCallback getwapipsktypecallback) throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    SupplicantStatus setEapErp(boolean z) throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    SupplicantStatus setSimNumber(int i) throws RemoteException;

    SupplicantStatus setWapiCertSel(String str) throws RemoteException;

    SupplicantStatus setWapiCertSelMode(int i) throws RemoteException;

    SupplicantStatus setWapiPsk(String str) throws RemoteException;

    SupplicantStatus setWapiPskType(int i) throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantVendorStaNetwork asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISupplicantVendorStaNetwork)) {
            return (ISupplicantVendorStaNetwork) iface;
        }
        ISupplicantVendorStaNetwork proxy = new Proxy(binder);
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

    static ISupplicantVendorStaNetwork castFrom(IHwInterface iface) {
        return iface == null ? null : asInterface(iface.asBinder());
    }

    static ISupplicantVendorStaNetwork getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static ISupplicantVendorStaNetwork getService() throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, HalDeviceManager.HAL_INSTANCE_NAME));
    }
}
