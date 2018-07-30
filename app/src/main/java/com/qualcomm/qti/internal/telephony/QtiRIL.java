package com.qualcomm.qti.internal.telephony;

import android.content.Context;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.os.AsyncResult;
import android.os.IHwBinder.DeathRecipient;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.Rlog;
import com.android.internal.telephony.RIL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import vendor.qti.hardware.radio.qtiradio.V1_0.IQtiRadio;
import vendor.qti.hardware.radio.qtiradio.V1_0.OPPO_rffe_data_type;

public final class QtiRIL extends RIL {
    static final String[] QTI_HIDL_SERVICE_NAME = new String[]{"slot1", "slot2", "slot3"};
    static final String TAG = "QTIRILJ";
    final QtiRadioProxyDeathRecipient mDeathRecipient;
    public Context mQtiContext;
    int mQtiPhoneId;
    private IQtiRadio mQtiRadio;
    QtiRadioIndication mQtiRadioIndication;
    final AtomicLong mQtiRadioProxyCookie;
    QtiRadioResponse mQtiRadioResponse;
    private final QtiRadioServiceNotification mServiceNotification;

    final class QtiRadioProxyDeathRecipient implements DeathRecipient {
        QtiRadioProxyDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            Rlog.d(QtiRIL.TAG, "serviceDied");
            QtiRIL.this.resetServiceAndRequestList();
        }
    }

    final class QtiRadioServiceNotification extends Stub {
        QtiRadioServiceNotification() {
        }

        public void onRegistration(String fqName, String name, boolean preexisting) {
            Rlog.d(QtiRIL.TAG, "QtiRadio interface service started " + fqName + " " + name + " preexisting =" + preexisting);
            if (!QtiRIL.this.isQtiRadioServiceConnected()) {
                QtiRIL.this.initQtiRadio();
            }
        }
    }

    private void resetServiceAndRequestList() {
        resetProxyAndRequestList();
        this.mQtiRadio = null;
        this.mQtiRadioResponse = null;
        this.mQtiRadioIndication = null;
        this.mQtiRadioProxyCookie.incrementAndGet();
    }

    private boolean isQtiRadioServiceConnected() {
        return this.mQtiRadio != null;
    }

    private void registerForQtiRadioServiceNotification() {
        try {
            if (!IServiceManager.getService().registerForNotifications(IQtiRadio.kInterfaceName, QTI_HIDL_SERVICE_NAME[this.mQtiPhoneId], this.mServiceNotification)) {
                Rlog.e(TAG, "Failed to register for service start notifications");
            }
        } catch (RemoteException ex) {
            Rlog.e(TAG, "Failed to register for service start notifications. Exception " + ex);
        }
    }

    private synchronized void initQtiRadio() {
        try {
            this.mQtiRadio = IQtiRadio.getService(QTI_HIDL_SERVICE_NAME[this.mQtiPhoneId]);
            if (this.mQtiRadio == null) {
                Rlog.e(TAG, "initQtiRadio: mQtiRadio is null. Return");
                return;
            }
            Rlog.d(TAG, "initQtiRadio: mQtiRadio" + this.mQtiRadio);
            this.mQtiRadio.linkToDeath(this.mDeathRecipient, this.mQtiRadioProxyCookie.incrementAndGet());
            this.mQtiRadioResponse = new QtiRadioResponse(this);
            this.mQtiRadioIndication = new QtiRadioIndication(this);
            this.mQtiRadio.setCallback(this.mQtiRadioResponse, this.mQtiRadioIndication);
        } catch (Exception ex) {
            Rlog.e(TAG, "initQtiRadio: Exception: " + ex);
            resetServiceAndRequestList();
        }
        return;
    }

    public QtiRIL(Context context, int preferredNetworkType, int cdmaSubscription) {
        this(context, preferredNetworkType, cdmaSubscription, null);
    }

    public QtiRIL(Context context, int preferredNetworkType, int cdmaSubscription, Integer instanceId) {
        super(context, preferredNetworkType, cdmaSubscription, instanceId);
        this.mQtiPhoneId = 0;
        this.mQtiRadioProxyCookie = new AtomicLong(0);
        this.mServiceNotification = new QtiRadioServiceNotification();
        this.mQtiPhoneId = instanceId.intValue();
        Rlog.d(TAG, "QtiRIL");
        this.mDeathRecipient = new QtiRadioProxyDeathRecipient();
        registerForQtiRadioServiceNotification();
        this.mQtiContext = context;
    }

    private String convertNullToEmptyString(String string) {
        return string != null ? string : "";
    }

    public void getAtr(Message result) {
        Rlog.d(TAG, "getAtr");
        try {
            this.mQtiRadio.getAtr(obtainRequestSerial(200, result, this.mRILDefaultWorkSource));
        } catch (Exception e) {
            Rlog.e(TAG, "getAtr: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void setFactoryModeNvProcess(int cmd, Message result) {
        Rlog.d(TAG, "setFactoryModeNvProcess");
        try {
            this.mQtiRadio.processFactoryModeNV(obtainRequestSerial(143, result, this.mRILDefaultWorkSource), (byte) cmd);
        } catch (Exception e) {
            Rlog.e(TAG, "getAtr: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void setFactoryModeModemGPIO(int status, int num, Message result) {
        Rlog.d(TAG, "setFactoryModeGPIO");
        try {
            this.mQtiRadio.setFactoryModeGPIO(obtainRequestSerial(144, result, this.mRILDefaultWorkSource), status, num);
        } catch (Exception e) {
            Rlog.e(TAG, "getAtr: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void reportNvRestore(Message result) {
    }

    public void OppoGetRffeDevInfo(int rf_tech, Message result) {
        Rlog.d(TAG, "OppoGetRffeDevInfo");
        try {
            this.mQtiRadio.getRffeDevInfo(obtainRequestSerial(147, result, this.mRILDefaultWorkSource), rf_tech);
        } catch (Exception e) {
            Rlog.e(TAG, "OppoGetRffeDevInfo: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void setModemCrash(Message result) {
        Rlog.d(TAG, "setModemCrash");
        try {
            this.mQtiRadio.setModemErrorFatal(obtainRequestSerial(153, result, this.mRILDefaultWorkSource));
        } catch (Exception e) {
            Rlog.e(TAG, "setModemCrash: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void OppoGetMdmBaseBand(Message result) {
        Rlog.d(TAG, "OppoGetMdmBaseBand");
        try {
            this.mQtiRadio.getMdmBaseBand(obtainRequestSerial(154, result, this.mRILDefaultWorkSource));
        } catch (Exception e) {
            Rlog.e(TAG, "OppoGetMdmBaseBand: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void setTddLTE(int status, Message result) {
        Rlog.d(TAG, "setTddLTE");
        try {
            this.mQtiRadio.setTddLTE(obtainRequestSerial(155, result, this.mRILDefaultWorkSource), status);
        } catch (Exception e) {
            Rlog.e(TAG, "setTddLTE: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoGetRadioInfo(Message result) {
        Rlog.d(TAG, "oppoGetRadioInfo");
        try {
            this.mQtiRadio.getRadioInfo(obtainRequestSerial(157, result, this.mRILDefaultWorkSource));
        } catch (Exception e) {
            Rlog.e(TAG, "oppoGetRadioInfo(: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoSetFilterArfcn(int arfcn1, int arfcn2, Message result) {
        Rlog.d(TAG, "oppoSetFilterArfcn");
        try {
            this.mQtiRadio.setFilterArfcn(obtainRequestSerial(158, result, this.mRILDefaultWorkSource), arfcn1, arfcn2);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoSetFilterArfcn: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoUpdatePplmnList(byte[] values, Message result) {
        Rlog.d(TAG, "oppoUpdatePplmnList");
        int serial = obtainRequestSerial(159, result, this.mRILDefaultWorkSource);
        Rlog.d(TAG, "update value length" + values.length + "\n");
        try {
            ArrayList<Byte> arrList = new ArrayList();
            for (byte valueOf : values) {
                arrList.add(Byte.valueOf(valueOf));
            }
            Rlog.d(TAG, "arrList length" + arrList.size());
            this.mQtiRadio.setPplmnList(serial, arrList);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoUpdatePplmnList: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoGetTxRxInfo(int sys_mode, Message result) {
        Rlog.d(TAG, "oppoGetTxRxInfo");
        try {
            this.mQtiRadio.getTxRxInfo(obtainRequestSerial(160, result, this.mRILDefaultWorkSource), sys_mode);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoGetTxRxInfo: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void OppoExpSetRegionForRilEcclist(Message result) {
        Rlog.d(TAG, "OppoExpSetRegionForRilEcclist");
        try {
            this.mQtiRadio.getRegionChangedForEccList(obtainRequestSerial(162, result, this.mRILDefaultWorkSource));
        } catch (Exception e) {
            Rlog.e(TAG, "OppoExpSetRegionForRilEcclist: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoUpdateFakeBsWeight(int[] values, Message result) {
        Rlog.d(TAG, "oppoUpdateFakeBsWeight");
        int serial = obtainRequestSerial(163, result, this.mRILDefaultWorkSource);
        ArrayList<Integer> arrList = new ArrayList();
        for (int valueOf : values) {
            arrList.add(Integer.valueOf(valueOf));
        }
        try {
            this.mQtiRadio.setFakesBsWeight(serial, arrList);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoUpdateFakeBsWeight: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoUpdateVolteFr2(int flags, int rsrp_thresh, int fr2_rsrp, int rsrp_adj, Message result) {
        Rlog.d(TAG, "oppoUpdateVolteFr2");
        try {
            this.mQtiRadio.setVolteFr2(obtainRequestSerial(164, result, this.mRILDefaultWorkSource), flags, rsrp_thresh, fr2_rsrp, rsrp_adj);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoUpdateVolteFr2: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoNoticeUpdateVolteFr(int flags, Message result) {
        Rlog.d(TAG, "oppoNoticeUpdateVolteFr");
        try {
            this.mQtiRadio.setVolteFr1(obtainRequestSerial(165, result, this.mRILDefaultWorkSource), flags);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoNoticeUpdateVolteFr: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoLockGSMArfcn(int arfcn1, Message result) {
        Rlog.d(TAG, "oppoLockGSMArfcn");
        try {
            this.mQtiRadio.lockGsmArfcn(obtainRequestSerial(166, result, this.mRILDefaultWorkSource), arfcn1);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoLockGSMArfcn: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoRffeCmd(int[] rffe_params, Message result) {
        Rlog.d(TAG, "oppoRffeCmd");
        int serial = obtainRequestSerial(167, result, this.mRILDefaultWorkSource);
        try {
            OPPO_rffe_data_type rffe_data = new OPPO_rffe_data_type();
            rffe_data.ext = (byte) rffe_params[0];
            rffe_data.readwrite = (byte) rffe_params[1];
            rffe_data.channel = (byte) rffe_params[2];
            rffe_data.slave = (byte) rffe_params[3];
            rffe_data.address = (short) rffe_params[4];
            rffe_data.data = (byte) rffe_params[5];
            rffe_data.halfspeed = (byte) rffe_params[6];
            this.mQtiRadio.getRffeCmd(serial, rffe_data);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoRffeCmd: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoLockLteCell(int arfcn, int pci, Message result) {
        Rlog.d(TAG, "oppoLockLteCell");
        try {
            this.mQtiRadio.lockLteCell(obtainRequestSerial(166, result, this.mRILDefaultWorkSource), arfcn, pci);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoLockLteCell: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void oppoCtlModemFeature(int[] values, Message result) {
        Rlog.d(TAG, "oppoCtlModemFeature");
        int serial = obtainRequestSerial(166, result, this.mRILDefaultWorkSource);
        ArrayList<Integer> arrList = new ArrayList();
        for (int valueOf : values) {
            arrList.add(Integer.valueOf(valueOf));
        }
        try {
            this.mQtiRadio.controlModemFeature(serial, arrList);
        } catch (Exception e) {
            Rlog.e(TAG, "oppoCtlModemFeature: Exception: " + e);
            resetServiceAndRequestList();
        }
    }

    public void notifyLteCARegistrants(int[] caInfo) {
        if (this.mLteCARegistrants != null) {
            this.mLteCARegistrants.notifyRegistrants(new AsyncResult(null, caInfo, null));
        }
        this.mLteCaInfo = caInfo;
        if (this.mLteCaInfo != null && this.mLteCaInfo.length == 21 && this.mLteCaInfo[10] == 0 && this.mLteCaInfo[17] == 0) {
            Rlog.d(TAG, "OPPO_DBG: notifyLteCARegistrants: deconfigured ");
            this.mLteCaInfo = null;
        }
    }

    void qtiProcessResponseDone(Object ret, RadioResponseInfo responseInfo, Object obj) {
        processResponseDone(ret, responseInfo, obj);
    }

    Message qtiGetMessageFromRequest(Object request) {
        return getMessageFromRequest(request);
    }

    Object qtiProcessResponse(RadioResponseInfo responseInfo) {
        return processResponse(responseInfo);
    }

    void qtiProcessResponseDone(Object ret, RadioResponseInfo responseInfo, String str) {
        processResponseDone(ret, responseInfo, str);
    }
}
