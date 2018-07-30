package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionInfo;
import java.util.List;

public interface ISub extends IInterface {

    public static abstract class Stub extends Binder implements ISub {
        private static final String DESCRIPTOR = "com.android.internal.telephony.ISub";
        static final int TRANSACTION_addSubInfoRecord = 12;
        static final int TRANSACTION_clearDefaultsForInactiveSubIds = 29;
        static final int TRANSACTION_clearSubInfo = 21;
        static final int TRANSACTION_getAccessibleSubscriptionInfoList = 10;
        static final int TRANSACTION_getActiveSubIdList = 30;
        static final int TRANSACTION_getActiveSubInfoCount = 7;
        static final int TRANSACTION_getActiveSubInfoCountMax = 8;
        static final int TRANSACTION_getActiveSubInfoList = 35;
        static final int TRANSACTION_getActiveSubscriptionInfo = 3;
        static final int TRANSACTION_getActiveSubscriptionInfoForIccId = 4;
        static final int TRANSACTION_getActiveSubscriptionInfoForSimSlotIndex = 5;
        static final int TRANSACTION_getActiveSubscriptionInfoList = 6;
        static final int TRANSACTION_getAllSubInfoCount = 2;
        static final int TRANSACTION_getAllSubInfoList = 1;
        static final int TRANSACTION_getAvailableSubscriptionInfoList = 9;
        static final int TRANSACTION_getDefaultDataSubId = 23;
        static final int TRANSACTION_getDefaultSmsSubId = 27;
        static final int TRANSACTION_getDefaultSubId = 20;
        static final int TRANSACTION_getDefaultVoiceSubId = 25;
        static final int TRANSACTION_getOperatorNumericForData = 42;
        static final int TRANSACTION_getPhoneId = 22;
        static final int TRANSACTION_getSimStateForSlotIndex = 33;
        static final int TRANSACTION_getSlotIndex = 18;
        static final int TRANSACTION_getSoftSimCardSlotId = 41;
        static final int TRANSACTION_getSubId = 19;
        static final int TRANSACTION_getSubInfoForSubscriber = 37;
        static final int TRANSACTION_getSubInfoUsingSlotId = 36;
        static final int TRANSACTION_getSubscriptionProperty = 32;
        static final int TRANSACTION_isActiveSubId = 34;
        static final int TRANSACTION_isCTCCard = 39;
        static final int TRANSACTION_isHasSoftSimCard = 40;
        static final int TRANSACTION_requestEmbeddedSubscriptionInfoListRefresh = 11;
        static final int TRANSACTION_setDataRoaming = 17;
        static final int TRANSACTION_setDefaultApplication = 38;
        static final int TRANSACTION_setDefaultDataSubId = 24;
        static final int TRANSACTION_setDefaultSmsSubId = 28;
        static final int TRANSACTION_setDefaultVoiceSubId = 26;
        static final int TRANSACTION_setDisplayName = 14;
        static final int TRANSACTION_setDisplayNameUsingSrc = 15;
        static final int TRANSACTION_setDisplayNumber = 16;
        static final int TRANSACTION_setIconTint = 13;
        static final int TRANSACTION_setSubscriptionProperty = 31;

        private static class Proxy implements ISub {
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

            public List<SubscriptionInfo> getAllSubInfoList(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getAllSubInfoCount(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public SubscriptionInfo getActiveSubscriptionInfo(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    SubscriptionInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (SubscriptionInfo) SubscriptionInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public SubscriptionInfo getActiveSubscriptionInfoForIccId(String iccId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    SubscriptionInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iccId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (SubscriptionInfo) SubscriptionInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIndex, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    SubscriptionInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (SubscriptionInfo) SubscriptionInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<SubscriptionInfo> getActiveSubscriptionInfoList(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getActiveSubInfoCount(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getActiveSubInfoCountMax() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<SubscriptionInfo> getAvailableSubscriptionInfoList(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<SubscriptionInfo> getAccessibleSubscriptionInfoList(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void requestEmbeddedSubscriptionInfoListRefresh() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public int addSubInfoRecord(String iccId, int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iccId);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setIconTint(int tint, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(tint);
                    _data.writeInt(subId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setDisplayName(String displayName, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(displayName);
                    _data.writeInt(subId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setDisplayNameUsingSrc(String displayName, int subId, long nameSource) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(displayName);
                    _data.writeInt(subId);
                    _data.writeLong(nameSource);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setDisplayNumber(String number, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    _data.writeInt(subId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setDataRoaming(int roaming, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(roaming);
                    _data.writeInt(subId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSlotIndex(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] getSubId(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDefaultSubId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int clearSubInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPhoneId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDefaultDataSubId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDefaultDataSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDefaultVoiceSubId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDefaultVoiceSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDefaultSmsSubId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDefaultSmsSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearDefaultsForInactiveSubIds() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] getActiveSubIdList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSubscriptionProperty(int subId, String propKey, String propValue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(propKey);
                    _data.writeString(propValue);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getSubscriptionProperty(int subId, String propKey, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(propKey);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSimStateForSlotIndex(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isActiveSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
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

            public List<SubInfoRecord> getActiveSubInfoList(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    List<SubInfoRecord> _result = _reply.createTypedArrayList(SubInfoRecord.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<SubInfoRecord> getSubInfoUsingSlotId(int slotId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    List<SubInfoRecord> _result = _reply.createTypedArrayList(SubInfoRecord.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public SubInfoRecord getSubInfoForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    SubInfoRecord _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (SubInfoRecord) SubInfoRecord.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDefaultApplication(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isCTCCard(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
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

            public boolean isHasSoftSimCard() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, _data, _reply, 0);
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

            public int getSoftSimCardSlotId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getOperatorNumericForData(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    this.mRemote.transact(42, _data, _reply, 0);
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

        public static ISub asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ISub)) {
                return new Proxy(obj);
            }
            return (ISub) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            List<SubscriptionInfo> _result;
            int _result2;
            SubscriptionInfo _result3;
            int[] _result4;
            String _result5;
            boolean _result6;
            List<SubInfoRecord> _result7;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getAllSubInfoList(data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getAllSubInfoCount(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getActiveSubscriptionInfo(data.readInt(), data.readString());
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getActiveSubscriptionInfoForIccId(data.readString(), data.readString());
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getActiveSubscriptionInfoForSimSlotIndex(data.readInt(), data.readString());
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getActiveSubscriptionInfoList(data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getActiveSubInfoCount(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getActiveSubInfoCountMax();
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getAvailableSubscriptionInfoList(data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getAccessibleSubscriptionInfoList(data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    requestEmbeddedSubscriptionInfoListRefresh();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = addSubInfoRecord(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setIconTint(data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setDisplayName(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setDisplayNameUsingSrc(data.readString(), data.readInt(), data.readLong());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setDisplayNumber(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setDataRoaming(data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getSlotIndex(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getSubId(data.readInt());
                    reply.writeNoException();
                    reply.writeIntArray(_result4);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getDefaultSubId();
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = clearSubInfo();
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getPhoneId(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getDefaultDataSubId();
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    setDefaultDataSubId(data.readInt());
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getDefaultVoiceSubId();
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    setDefaultVoiceSubId(data.readInt());
                    reply.writeNoException();
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getDefaultSmsSubId();
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    setDefaultSmsSubId(data.readInt());
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    clearDefaultsForInactiveSubIds();
                    reply.writeNoException();
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getActiveSubIdList();
                    reply.writeNoException();
                    reply.writeIntArray(_result4);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    setSubscriptionProperty(data.readInt(), data.readString(), data.readString());
                    reply.writeNoException();
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    _result5 = getSubscriptionProperty(data.readInt(), data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeString(_result5);
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getSimStateForSlotIndex(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    _result6 = isActiveSubId(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result6 ? 1 : 0);
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    _result7 = getActiveSubInfoList(data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result7);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    _result7 = getSubInfoUsingSlotId(data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result7);
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    SubInfoRecord _result8 = getSubInfoForSubscriber(data.readInt(), data.readString());
                    reply.writeNoException();
                    if (_result8 != null) {
                        reply.writeInt(1);
                        _result8.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    setDefaultApplication(data.readString());
                    reply.writeNoException();
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    _result6 = isCTCCard(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result6 ? 1 : 0);
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    _result6 = isHasSoftSimCard();
                    reply.writeNoException();
                    reply.writeInt(_result6 ? 1 : 0);
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getSoftSimCardSlotId();
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 42:
                    data.enforceInterface(DESCRIPTOR);
                    _result5 = getOperatorNumericForData(data.readInt());
                    reply.writeNoException();
                    reply.writeString(_result5);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    int addSubInfoRecord(String str, int i) throws RemoteException;

    void clearDefaultsForInactiveSubIds() throws RemoteException;

    int clearSubInfo() throws RemoteException;

    List<SubscriptionInfo> getAccessibleSubscriptionInfoList(String str) throws RemoteException;

    int[] getActiveSubIdList() throws RemoteException;

    int getActiveSubInfoCount(String str) throws RemoteException;

    int getActiveSubInfoCountMax() throws RemoteException;

    List<SubInfoRecord> getActiveSubInfoList(String str) throws RemoteException;

    SubscriptionInfo getActiveSubscriptionInfo(int i, String str) throws RemoteException;

    SubscriptionInfo getActiveSubscriptionInfoForIccId(String str, String str2) throws RemoteException;

    SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int i, String str) throws RemoteException;

    List<SubscriptionInfo> getActiveSubscriptionInfoList(String str) throws RemoteException;

    int getAllSubInfoCount(String str) throws RemoteException;

    List<SubscriptionInfo> getAllSubInfoList(String str) throws RemoteException;

    List<SubscriptionInfo> getAvailableSubscriptionInfoList(String str) throws RemoteException;

    int getDefaultDataSubId() throws RemoteException;

    int getDefaultSmsSubId() throws RemoteException;

    int getDefaultSubId() throws RemoteException;

    int getDefaultVoiceSubId() throws RemoteException;

    String getOperatorNumericForData(int i) throws RemoteException;

    int getPhoneId(int i) throws RemoteException;

    int getSimStateForSlotIndex(int i) throws RemoteException;

    int getSlotIndex(int i) throws RemoteException;

    int getSoftSimCardSlotId() throws RemoteException;

    int[] getSubId(int i) throws RemoteException;

    SubInfoRecord getSubInfoForSubscriber(int i, String str) throws RemoteException;

    List<SubInfoRecord> getSubInfoUsingSlotId(int i, String str) throws RemoteException;

    String getSubscriptionProperty(int i, String str, String str2) throws RemoteException;

    boolean isActiveSubId(int i) throws RemoteException;

    boolean isCTCCard(int i) throws RemoteException;

    boolean isHasSoftSimCard() throws RemoteException;

    void requestEmbeddedSubscriptionInfoListRefresh() throws RemoteException;

    int setDataRoaming(int i, int i2) throws RemoteException;

    void setDefaultApplication(String str) throws RemoteException;

    void setDefaultDataSubId(int i) throws RemoteException;

    void setDefaultSmsSubId(int i) throws RemoteException;

    void setDefaultVoiceSubId(int i) throws RemoteException;

    int setDisplayName(String str, int i) throws RemoteException;

    int setDisplayNameUsingSrc(String str, int i, long j) throws RemoteException;

    int setDisplayNumber(String str, int i) throws RemoteException;

    int setIconTint(int i, int i2) throws RemoteException;

    void setSubscriptionProperty(int i, String str, String str2) throws RemoteException;
}
