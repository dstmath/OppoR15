package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.UiccCarrierPrivilegeRules.TLV;
import com.google.android.mms.pdu.PduHeaders;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UiccPkcs15 extends Handler {
    private static final String CARRIER_RULE_AID = "FFFFFFFFFFFF";
    private static final boolean DBG = true;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 7;
    private static final int EVENT_LOAD_ACCF_DONE = 6;
    private static final int EVENT_LOAD_ACMF_DONE = 4;
    private static final int EVENT_LOAD_ACRF_DONE = 5;
    private static final int EVENT_LOAD_DODF_DONE = 3;
    private static final int EVENT_LOAD_ODF_DONE = 2;
    private static final int EVENT_SELECT_PKCS15_DONE = 1;
    private static final String ID_ACRF = "4300";
    private static final String LOG_TAG = "UiccPkcs15";
    private static final String TAG_ASN_OCTET_STRING = "04";
    private static final String TAG_ASN_SEQUENCE = "30";
    private static final String TAG_TARGET_AID = "A0";
    private int mChannelId = -1;
    private FileHandler mFh;
    private Message mLoadedCallback;
    private Pkcs15Selector mPkcs15Selector;
    private List<String> mRules = new ArrayList();
    private UiccCard mUiccCard;

    private class FileHandler extends Handler {
        protected static final int EVENT_READ_BINARY_DONE = 102;
        protected static final int EVENT_SELECT_FILE_DONE = 101;
        private Message mCallback;
        private String mFileId;
        private final String mPkcs15Path;

        public FileHandler(String pkcs15Path) {
            UiccPkcs15.log("Creating FileHandler, pkcs15Path: " + pkcs15Path);
            this.mPkcs15Path = pkcs15Path;
        }

        public boolean loadFile(String fileId, Message callBack) {
            UiccPkcs15.log("loadFile: " + fileId);
            if (fileId == null || callBack == null) {
                return false;
            }
            this.mFileId = fileId;
            this.mCallback = callBack;
            selectFile();
            return true;
        }

        private void selectFile() {
            if (UiccPkcs15.this.mChannelId >= 0) {
                UiccPkcs15.this.mUiccCard.iccTransmitApduLogicalChannel(UiccPkcs15.this.mChannelId, 0, PduHeaders.MM_FLAGS, 0, 4, 2, this.mFileId, obtainMessage(101));
                return;
            }
            UiccPkcs15.log("EF based");
        }

        private void readBinary() {
            if (UiccPkcs15.this.mChannelId >= 0) {
                UiccPkcs15.this.mUiccCard.iccTransmitApduLogicalChannel(UiccPkcs15.this.mChannelId, 0, PduHeaders.ADDITIONAL_HEADERS, 0, 0, 0, SpnOverride.MVNO_TYPE_NONE, obtainMessage(102));
                return;
            }
            UiccPkcs15.log("EF based");
        }

        public void handleMessage(Message msg) {
            Throwable th = null;
            UiccPkcs15.log("handleMessage: " + msg.what);
            AsyncResult ar = msg.obj;
            if (ar.exception != null || ar.result == null) {
                UiccPkcs15.log("Error: " + ar.exception);
                AsyncResult.forMessage(this.mCallback, null, ar.exception);
                this.mCallback.sendToTarget();
                return;
            }
            switch (msg.what) {
                case 101:
                    readBinary();
                    break;
                case 102:
                    IccIoResult response = ar.result;
                    String result = IccUtils.bytesToHexString(response.payload).toUpperCase(Locale.US);
                    UiccPkcs15.log("IccIoResult: " + response + " payload: " + result);
                    Message message = this.mCallback;
                    if (result == null) {
                        th = new IccException("Error: null response for " + this.mFileId);
                    }
                    AsyncResult.forMessage(message, result, th);
                    this.mCallback.sendToTarget();
                    break;
                default:
                    UiccPkcs15.log("Unknown event" + msg.what);
                    break;
            }
        }
    }

    private class Pkcs15Selector extends Handler {
        private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 201;
        private static final String PKCS15_AID = "A000000063504B43532D3135";
        private Message mCallback;

        public Pkcs15Selector(Message callBack) {
            this.mCallback = callBack;
            UiccPkcs15.this.mUiccCard.iccOpenLogicalChannel(PKCS15_AID, 4, obtainMessage(EVENT_OPEN_LOGICAL_CHANNEL_DONE));
        }

        public void handleMessage(Message msg) {
            UiccPkcs15.log("handleMessage: " + msg.what);
            switch (msg.what) {
                case EVENT_OPEN_LOGICAL_CHANNEL_DONE /*201*/:
                    AsyncResult ar = msg.obj;
                    if (ar.exception != null || ar.result == null) {
                        UiccPkcs15.log("error: " + ar.exception);
                        AsyncResult.forMessage(this.mCallback, null, ar.exception);
                    } else {
                        UiccPkcs15.this.mChannelId = ((int[]) ar.result)[0];
                        UiccPkcs15.log("mChannelId: " + UiccPkcs15.this.mChannelId);
                        AsyncResult.forMessage(this.mCallback, null, null);
                    }
                    this.mCallback.sendToTarget();
                    return;
                default:
                    UiccPkcs15.log("Unknown event" + msg.what);
                    return;
            }
        }
    }

    public UiccPkcs15(UiccCard uiccCard, Message loadedCallback) {
        log("Creating UiccPkcs15");
        this.mUiccCard = uiccCard;
        this.mLoadedCallback = loadedCallback;
        this.mPkcs15Selector = new Pkcs15Selector(obtainMessage(1));
    }

    public void handleMessage(Message msg) {
        log("handleMessage: " + msg.what);
        AsyncResult ar = msg.obj;
        switch (msg.what) {
            case 1:
                if (ar.exception == null) {
                    this.mFh = new FileHandler((String) ar.result);
                    if (!this.mFh.loadFile(ID_ACRF, obtainMessage(5))) {
                        cleanUp();
                        return;
                    }
                    return;
                }
                log("select pkcs15 failed: " + ar.exception);
                this.mLoadedCallback.sendToTarget();
                return;
            case 5:
                if (ar.exception != null || ar.result == null) {
                    cleanUp();
                    return;
                }
                if (!this.mFh.loadFile(parseAcrf((String) ar.result), obtainMessage(6))) {
                    cleanUp();
                    return;
                }
                return;
            case 6:
                if (ar.exception == null && ar.result != null) {
                    parseAccf((String) ar.result);
                }
                cleanUp();
                return;
            case 7:
                return;
            default:
                Rlog.e(LOG_TAG, "Unknown event " + msg.what);
                return;
        }
    }

    private void cleanUp() {
        log("cleanUp");
        if (this.mChannelId >= 0) {
            this.mUiccCard.iccCloseLogicalChannel(this.mChannelId, obtainMessage(7));
            this.mChannelId = -1;
        }
        this.mLoadedCallback.sendToTarget();
    }

    private String parseAcrf(String data) {
        String ret = null;
        String acRules = data;
        while (!acRules.isEmpty()) {
            TLV tlvRule = new TLV(TAG_ASN_SEQUENCE);
            try {
                acRules = tlvRule.parse(acRules, false);
                String ruleString = tlvRule.getValue();
                if (ruleString.startsWith(TAG_TARGET_AID)) {
                    TLV tlvTarget = new TLV(TAG_TARGET_AID);
                    TLV tlvAid = new TLV(TAG_ASN_OCTET_STRING);
                    TLV tlvAsnPath = new TLV(TAG_ASN_SEQUENCE);
                    TLV tlvPath = new TLV(TAG_ASN_OCTET_STRING);
                    ruleString = tlvTarget.parse(ruleString, false);
                    tlvAid.parse(tlvTarget.getValue(), true);
                    if (CARRIER_RULE_AID.equals(tlvAid.getValue())) {
                        tlvAsnPath.parse(ruleString, true);
                        tlvPath.parse(tlvAsnPath.getValue(), true);
                        ret = tlvPath.getValue();
                    }
                }
            } catch (RuntimeException ex) {
                log("Error: " + ex);
            }
        }
        return ret;
    }

    private void parseAccf(String data) {
        String acCondition = data;
        while (!acCondition.isEmpty()) {
            TLV tlvCondition = new TLV(TAG_ASN_SEQUENCE);
            TLV tlvCert = new TLV(TAG_ASN_OCTET_STRING);
            try {
                acCondition = tlvCondition.parse(acCondition, false);
                tlvCert.parse(tlvCondition.getValue(), true);
                if (!tlvCert.getValue().isEmpty()) {
                    this.mRules.add(tlvCert.getValue());
                }
            } catch (RuntimeException ex) {
                log("Error: " + ex);
                return;
            }
        }
    }

    public List<String> getRules() {
        return this.mRules;
    }

    private static void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mRules != null) {
            pw.println(" mRules:");
            for (String cert : this.mRules) {
                pw.println("  " + cert);
            }
        }
    }
}
