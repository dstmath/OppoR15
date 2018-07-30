package com.android.internal.telephony;

import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.AppStatus;
import android.hardware.radio.V1_0.Call;
import android.hardware.radio.V1_0.CallForwardInfo;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.Carrier;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.HardwareConfig;
import android.hardware.radio.V1_0.IccIoResult;
import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.OperatorInfo;
import android.hardware.radio.V1_0.RadioCapability;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SignalStrength;
import android.hardware.radio.V1_0.UusInfo;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.hardware.radio.V1_1.IRadioResponse.Stub;
import android.hardware.radio.V1_1.KeepaliveStatus;
import android.os.AsyncResult;
import android.os.Message;
import android.service.carrier.CarrierIdentifier;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import com.android.internal.telephony.gsm.OppoGsmServiceStateTracker;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.SpnOverride;
import java.util.ArrayList;
import java.util.Collections;

public class RadioResponse extends Stub {
    private static final int CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES = 31;
    private static final int CDMA_BSI_NO_OF_INTS_STRUCT = 3;
    RIL mRil;

    public RadioResponse(RIL ril) {
        this.mRil = ril;
    }

    static void sendMessageResponse(Message msg, Object ret) {
        if (msg != null) {
            AsyncResult.forMessage(msg, ret, null);
            msg.sendToTarget();
        }
    }

    public void acknowledgeRequest(int serial) {
        this.mRil.processRequestAck(serial);
    }

    public void getIccCardStatusResponse(RadioResponseInfo responseInfo, CardStatus cardStatus) {
        responseIccCardStatus(responseInfo, cardStatus);
    }

    public void supplyIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responsePinOrPukStatus(responseInfo, remainingAttempts);
    }

    public void supplyIccPukForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responsePinOrPukStatus(responseInfo, remainingAttempts);
    }

    public void supplyIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responsePinOrPukStatus(responseInfo, remainingAttempts);
    }

    public void supplyIccPuk2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responsePinOrPukStatus(responseInfo, remainingAttempts);
    }

    public void changeIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responsePinOrPukStatus(responseInfo, remainingAttempts);
    }

    public void changeIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responsePinOrPukStatus(responseInfo, remainingAttempts);
    }

    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo responseInfo, int retriesRemaining) {
        responseInts(responseInfo, retriesRemaining);
    }

    public void getCurrentCallsResponse(RadioResponseInfo responseInfo, ArrayList<Call> calls) {
        responseCurrentCalls(responseInfo, calls);
    }

    public void dialResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
        this.mRil.mKeylogHandler.removeMessages(2);
        if (responseInfo.error != 0) {
            this.mRil.mKeylogHandler.obtainMessage(2, 10, responseInfo.error).sendToTarget();
        }
    }

    public void getIMSIForAppResponse(RadioResponseInfo responseInfo, String imsi) {
        responseString(responseInfo, imsi);
    }

    public void hangupConnectionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
        if (System.currentTimeMillis() - this.mRil.mHangupTime > 11000) {
            this.mRil.mKeylogHandler.obtainMessage(2, 12, 0).sendToTarget();
        }
    }

    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void conferenceResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void rejectCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getLastCallFailCauseResponse(RadioResponseInfo responseInfo, LastCallFailCauseInfo fcInfo) {
        responseLastCallFailCauseInfo(responseInfo, fcInfo);
    }

    public void getSignalStrengthResponse(RadioResponseInfo responseInfo, SignalStrength sigStrength) {
        responseSignalStrength(responseInfo, sigStrength);
    }

    public void getVoiceRegistrationStateResponse(RadioResponseInfo responseInfo, VoiceRegStateResult voiceRegResponse) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, voiceRegResponse);
            }
            this.mRil.processResponseDone(rr, responseInfo, (Object) voiceRegResponse);
        }
    }

    public void getDataRegistrationStateResponse(RadioResponseInfo responseInfo, DataRegStateResult dataRegResponse) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, dataRegResponse);
            }
            this.mRil.processResponseDone(rr, responseInfo, (Object) dataRegResponse);
        }
    }

    public void getOperatorResponse(RadioResponseInfo responseInfo, String longName, String shortName, String numeric) {
        responseStrings(responseInfo, longName, shortName, numeric);
    }

    public void setRadioPowerResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    public void sendSMSExpectMoreResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    public void setupDataCallResponse(RadioResponseInfo responseInfo, SetupDataCallResult setupDataCallResult) {
        responseSetupDataCall(responseInfo, setupDataCallResult);
    }

    public void iccIOForAppResponse(RadioResponseInfo responseInfo, IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    public void sendUssdResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void cancelPendingUssdResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getClirResponse(RadioResponseInfo responseInfo, int n, int m) {
        responseInts(responseInfo, n, m);
    }

    public void setClirResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCallForwardStatusResponse(RadioResponseInfo responseInfo, ArrayList<CallForwardInfo> callForwardInfos) {
        responseCallForwardInfo(responseInfo, callForwardInfos);
    }

    public void setCallForwardResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCallWaitingResponse(RadioResponseInfo responseInfo, boolean enable, int serviceClass) {
        int i;
        int[] iArr = new int[2];
        if (enable) {
            i = 1;
        } else {
            i = 0;
        }
        iArr[0] = i;
        iArr[1] = serviceClass;
        responseInts(responseInfo, iArr);
    }

    public void setCallWaitingResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void acceptCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
        if (responseInfo.error != 0) {
            this.mRil.mKeylogHandler.obtainMessage(2, 40, responseInfo.error).sendToTarget();
        }
    }

    public void deactivateDataCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getFacilityLockForAppResponse(RadioResponseInfo responseInfo, int response) {
        responseInts(responseInfo, response);
    }

    public void setFacilityLockForAppResponse(RadioResponseInfo responseInfo, int retry) {
        responsePinOrPukStatus(responseInfo, retry);
    }

    public void setBarringPasswordResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getNetworkSelectionModeResponse(RadioResponseInfo responseInfo, boolean selection) {
        int i = 1;
        int[] iArr = new int[1];
        if (!selection) {
            i = 0;
        }
        iArr[0] = i;
        responseInts(responseInfo, iArr);
    }

    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setNetworkSelectionModeManualResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getAvailableNetworksResponse(RadioResponseInfo responseInfo, ArrayList<OperatorInfo> networkInfos) {
        oppoResponseOperatorInfos(responseInfo, networkInfos);
    }

    public void startNetworkScanResponse(RadioResponseInfo responseInfo) {
        responseScanStatus(responseInfo);
    }

    public void stopNetworkScanResponse(RadioResponseInfo responseInfo) {
        responseScanStatus(responseInfo);
    }

    public void startDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void stopDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getBasebandVersionResponse(RadioResponseInfo responseInfo, String version) {
        responseString(responseInfo, version);
    }

    public void separateConnectionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setMuteResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getMuteResponse(RadioResponseInfo responseInfo, boolean enable) {
        int i = 1;
        int[] iArr = new int[1];
        if (!enable) {
            i = 0;
        }
        iArr[0] = i;
        responseInts(responseInfo, iArr);
    }

    public void getClipResponse(RadioResponseInfo responseInfo, int status) {
        responseInts(responseInfo, status);
    }

    public void getDataCallListResponse(RadioResponseInfo responseInfo, ArrayList<SetupDataCallResult> dataCallResultList) {
        responseDataCallList(responseInfo, dataCallResultList);
    }

    public void sendOemRilRequestRawResponse(RadioResponseInfo responseInfo, ArrayList<Byte> arrayList) {
    }

    public void setSuppServiceNotificationsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void writeSmsToSimResponse(RadioResponseInfo responseInfo, int index) {
        responseInts(responseInfo, index);
    }

    public void deleteSmsOnSimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setBandModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getAvailableBandModesResponse(RadioResponseInfo responseInfo, ArrayList<Integer> bandModes) {
        responseIntArrayList(responseInfo, bandModes);
    }

    public void sendEnvelopeResponse(RadioResponseInfo responseInfo, String commandResponse) {
        responseString(responseInfo, commandResponse);
    }

    public void sendTerminalResponseToSimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void explicitCallTransferResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setPreferredNetworkTypeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getPreferredNetworkTypeResponse(RadioResponseInfo responseInfo, int nwType) {
        this.mRil.mPreferredNetworkType = nwType;
        responseInts(responseInfo, nwType);
    }

    public void getNeighboringCidsResponse(RadioResponseInfo responseInfo, ArrayList<NeighboringCell> cells) {
        responseCellList(responseInfo, cells);
    }

    public void setLocationUpdatesResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo, int type) {
        responseInts(responseInfo, type);
    }

    public void setTTYModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getTTYModeResponse(RadioResponseInfo responseInfo, int mode) {
        responseInts(responseInfo, mode);
    }

    public void setPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo, boolean enable) {
        int i = 1;
        int[] iArr = new int[1];
        if (!enable) {
            i = 0;
        }
        iArr[0] = i;
        responseInts(responseInfo, iArr);
    }

    public void sendCDMAFeatureCodeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendBurstDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendCdmaSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getGsmBroadcastConfigResponse(RadioResponseInfo responseInfo, ArrayList<GsmBroadcastSmsConfigInfo> configs) {
        responseGmsBroadcastConfig(responseInfo, configs);
    }

    public void setGsmBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setGsmBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo, ArrayList<CdmaBroadcastSmsConfigInfo> configs) {
        responseCdmaBroadcastConfig(responseInfo, configs);
    }

    public void setCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setCdmaBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCDMASubscriptionResponse(RadioResponseInfo responseInfo, String mdn, String hSid, String hNid, String min, String prl) {
        responseStrings(responseInfo, mdn, hSid, hNid, min, prl);
    }

    public void writeSmsToRuimResponse(RadioResponseInfo responseInfo, int index) {
        responseInts(responseInfo, index);
    }

    public void deleteSmsOnRuimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getDeviceIdentityResponse(RadioResponseInfo responseInfo, String imei, String imeisv, String esn, String meid) {
        responseStrings(responseInfo, imei, imeisv, esn, meid);
    }

    public void exitEmergencyCallbackModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getSmscAddressResponse(RadioResponseInfo responseInfo, String smsc) {
        responseString(responseInfo, smsc);
    }

    public void setSmscAddressResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void reportSmsMemoryStatusResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void reportStkServiceIsRunningResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo, int source) {
        responseInts(responseInfo, source);
    }

    public void requestIsimAuthenticationResponse(RadioResponseInfo responseInfo, String response) {
        responseString(responseInfo, response);
    }

    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendEnvelopeWithStatusResponse(RadioResponseInfo responseInfo, IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    public void getVoiceRadioTechnologyResponse(RadioResponseInfo responseInfo, int rat) {
        responseInts(responseInfo, rat);
    }

    public void getCellInfoListResponse(RadioResponseInfo responseInfo, ArrayList<CellInfo> cellInfo) {
        responseCellInfoList(responseInfo, cellInfo);
    }

    public void setCellInfoListRateResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setInitialAttachApnResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getImsRegistrationStateResponse(RadioResponseInfo responseInfo, boolean isRegistered, int ratFamily) {
        int i;
        int[] iArr = new int[2];
        if (isRegistered) {
            i = 1;
        } else {
            i = 0;
        }
        iArr[0] = i;
        iArr[1] = ratFamily;
        responseInts(responseInfo, iArr);
    }

    public void sendImsSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo responseInfo, IccIoResult result) {
        responseIccIo(responseInfo, result);
    }

    public void iccOpenLogicalChannelResponse(RadioResponseInfo responseInfo, int channelId, ArrayList<Byte> selectResponse) {
        ArrayList<Integer> arr = new ArrayList();
        arr.add(Integer.valueOf(channelId));
        for (int i = 0; i < selectResponse.size(); i++) {
            arr.add(Integer.valueOf(((Byte) selectResponse.get(i)).byteValue()));
        }
        responseIntArrayList(responseInfo, arr);
    }

    public void iccCloseLogicalChannelResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void iccTransmitApduLogicalChannelResponse(RadioResponseInfo responseInfo, IccIoResult result) {
        responseIccIo(responseInfo, result);
    }

    public void nvReadItemResponse(RadioResponseInfo responseInfo, String result) {
        responseString(responseInfo, result);
    }

    public void nvWriteItemResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void nvWriteCdmaPrlResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void nvResetConfigResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setUiccSubscriptionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setDataAllowedResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getHardwareConfigResponse(RadioResponseInfo responseInfo, ArrayList<HardwareConfig> config) {
        responseHardwareConfig(responseInfo, config);
    }

    public void requestIccSimAuthenticationResponse(RadioResponseInfo responseInfo, IccIoResult result) {
        responseICC_IOBase64(responseInfo, result);
    }

    public void setDataProfileResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void requestShutdownResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getRadioCapabilityResponse(RadioResponseInfo responseInfo, RadioCapability rc) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = RIL.convertHalRadioCapability(rc, this.mRil);
            if (responseInfo.error == 6 || responseInfo.error == 2) {
                ret = this.mRil.makeStaticRadioCapability();
                responseInfo.error = 0;
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void setRadioCapabilityResponse(RadioResponseInfo responseInfo, RadioCapability rc) {
        responseRadioCapability(responseInfo, rc);
    }

    public void startLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        responseLceStatus(responseInfo, statusInfo);
    }

    public void stopLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        responseLceStatus(responseInfo, statusInfo);
    }

    public void pullLceDataResponse(RadioResponseInfo responseInfo, LceDataInfo lceInfo) {
        responseLceData(responseInfo, lceInfo);
    }

    public void getModemActivityInfoResponse(RadioResponseInfo responseInfo, ActivityStatsInfo activityInfo) {
        responseActivityData(responseInfo, activityInfo);
    }

    public void setAllowedCarriersResponse(RadioResponseInfo responseInfo, int numAllowed) {
        responseInts(responseInfo, numAllowed);
    }

    public void getAllowedCarriersResponse(RadioResponseInfo responseInfo, boolean allAllowed, CarrierRestrictions carriers) {
        responseCarrierIdentifiers(responseInfo, allAllowed, carriers);
    }

    public void sendDeviceStateResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setCarrierInfoForImsiEncryptionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setIndicationFilterResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setSimCardPowerResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setSimCardPowerResponse_1_1(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void startKeepaliveResponse(RadioResponseInfo responseInfo, KeepaliveStatus keepaliveStatus) {
        throw new UnsupportedOperationException("startKeepaliveResponse not implemented");
    }

    public void stopKeepaliveResponse(RadioResponseInfo responseInfo) {
        throw new UnsupportedOperationException("stopKeepaliveResponse not implemented");
    }

    private void responseIccCardStatus(RadioResponseInfo responseInfo, CardStatus cardStatus) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object iccCardStatus = new IccCardStatus();
            iccCardStatus.setCardState(cardStatus.cardState);
            iccCardStatus.setUniversalPinState(cardStatus.universalPinState);
            iccCardStatus.mGsmUmtsSubscriptionAppIndex = cardStatus.gsmUmtsSubscriptionAppIndex;
            iccCardStatus.mCdmaSubscriptionAppIndex = cardStatus.cdmaSubscriptionAppIndex;
            iccCardStatus.mImsSubscriptionAppIndex = cardStatus.imsSubscriptionAppIndex;
            int numApplications = cardStatus.applications.size();
            if (numApplications > 8) {
                numApplications = 8;
            }
            iccCardStatus.mApplications = new IccCardApplicationStatus[numApplications];
            for (int i = 0; i < numApplications; i++) {
                AppStatus rilAppStatus = (AppStatus) cardStatus.applications.get(i);
                IccCardApplicationStatus appStatus = new IccCardApplicationStatus();
                appStatus.app_type = appStatus.AppTypeFromRILInt(rilAppStatus.appType);
                appStatus.app_state = appStatus.AppStateFromRILInt(rilAppStatus.appState);
                appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(rilAppStatus.persoSubstate);
                appStatus.aid = rilAppStatus.aidPtr;
                appStatus.app_label = rilAppStatus.appLabelPtr;
                appStatus.pin1_replaced = rilAppStatus.pin1Replaced;
                appStatus.pin1 = appStatus.PinStateFromRILInt(rilAppStatus.pin1);
                appStatus.pin2 = appStatus.PinStateFromRILInt(rilAppStatus.pin2);
                iccCardStatus.mApplications[i] = appStatus;
            }
            iccCardStatus.mSimHotswapState = cardStatus.simHotswapState;
            this.mRil.riljLog("responseIccCardStatus: simHotswapState: " + cardStatus.simHotswapState);
            this.mRil.riljLog("responseIccCardStatus: from HIDL: " + iccCardStatus);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, iccCardStatus);
            }
            this.mRil.processResponseDone(rr, responseInfo, iccCardStatus);
        }
    }

    private void responseInts(RadioResponseInfo responseInfo, int... var) {
        ArrayList<Integer> ints = new ArrayList();
        for (int valueOf : var) {
            ints.add(Integer.valueOf(valueOf));
        }
        responseIntArrayList(responseInfo, ints);
    }

    private void responseIntArrayList(RadioResponseInfo responseInfo, ArrayList<Integer> var) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new int[var.size()];
            for (int i = 0; i < var.size(); i++) {
                ret[i] = ((Integer) var.get(i)).intValue();
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responsePinOrPukStatus(RadioResponseInfo responseInfo, int remainingAttempts) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new Integer(remainingAttempts);
            this.mRil.riljLog("responsePinOrPukStatus : remainingAttempts = " + ret);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCurrentCalls(RadioResponseInfo responseInfo, ArrayList<Call> calls) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            int num = calls.size();
            Object dcCalls = new ArrayList(num);
            for (int i = 0; i < num; i++) {
                DriverCall dc = new DriverCall();
                dc.state = DriverCall.stateFromCLCC(((Call) calls.get(i)).state);
                dc.index = ((Call) calls.get(i)).index;
                dc.TOA = ((Call) calls.get(i)).toa;
                dc.isMpty = ((Call) calls.get(i)).isMpty;
                dc.isMT = ((Call) calls.get(i)).isMT;
                dc.als = ((Call) calls.get(i)).als;
                dc.isVoice = ((Call) calls.get(i)).isVoice;
                dc.isVoicePrivacy = ((Call) calls.get(i)).isVoicePrivacy;
                dc.number = ((Call) calls.get(i)).number;
                dc.numberPresentation = DriverCall.presentationFromCLIP(((Call) calls.get(i)).numberPresentation);
                dc.name = ((Call) calls.get(i)).name;
                dc.namePresentation = DriverCall.presentationFromCLIP(((Call) calls.get(i)).namePresentation);
                if (((Call) calls.get(i)).uusInfo.size() == 1) {
                    dc.uusInfo = new UUSInfo();
                    dc.uusInfo.setType(((UusInfo) ((Call) calls.get(i)).uusInfo.get(0)).uusType);
                    dc.uusInfo.setDcs(((UusInfo) ((Call) calls.get(i)).uusInfo.get(0)).uusDcs);
                    if (TextUtils.isEmpty(((UusInfo) ((Call) calls.get(i)).uusInfo.get(0)).uusData)) {
                        this.mRil.riljLog("responseCurrentCalls: uusInfo data is null or empty");
                    } else {
                        dc.uusInfo.setUserData(((UusInfo) ((Call) calls.get(i)).uusInfo.get(0)).uusData.getBytes());
                    }
                    this.mRil.riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d", new Object[]{Integer.valueOf(dc.uusInfo.getType()), Integer.valueOf(dc.uusInfo.getDcs()), Integer.valueOf(dc.uusInfo.getUserData().length)}));
                    this.mRil.riljLogv("Incoming UUS : data (hex): " + IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
                } else {
                    this.mRil.riljLogv("Incoming UUS : NOT present!");
                }
                dc.codec = ((Call) calls.get(i)).codec;
                dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
                dcCalls.add(dc);
                if (dc.isVoicePrivacy) {
                    this.mRil.mVoicePrivacyOnRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is enabled");
                } else {
                    this.mRil.mVoicePrivacyOffRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is disabled");
                }
            }
            Collections.sort(dcCalls);
            if (num == 0 && this.mRil.mTestingEmergencyCall.getAndSet(false) && this.mRil.mEmergencyCallbackModeRegistrant != null) {
                this.mRil.riljLog("responseCurrentCalls: call ended, testing emergency call, notify ECM Registrants");
                this.mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, dcCalls);
            }
            this.mRil.processResponseDone(rr, responseInfo, dcCalls);
        }
    }

    private void responseVoid(RadioResponseInfo responseInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, null);
            }
            this.mRil.processResponseDone(rr, responseInfo, null);
        }
    }

    private void responseString(RadioResponseInfo responseInfo, String str) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, str);
            }
            this.mRil.processResponseDone(rr, responseInfo, (Object) str);
        }
    }

    private void responseStrings(RadioResponseInfo responseInfo, String... str) {
        ArrayList<String> strings = new ArrayList();
        for (Object add : str) {
            strings.add(add);
        }
        responseStringArrayList(this.mRil, responseInfo, strings);
    }

    static void responseStringArrayList(RIL ril, RadioResponseInfo responseInfo, ArrayList<String> strings) {
        RILRequest rr = ril.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new String[strings.size()];
            for (int i = 0; i < strings.size(); i++) {
                ret[i] = (String) strings.get(i);
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            ril.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseLastCallFailCauseInfo(RadioResponseInfo responseInfo, LastCallFailCauseInfo fcInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new LastCallFailCause();
            ret.causeCode = fcInfo.causeCode;
            ret.vendorCause = fcInfo.vendorCause;
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSignalStrength(RadioResponseInfo responseInfo, SignalStrength sigStrength) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = RIL.convertHalSignalStrength(sigStrength);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSms(RadioResponseInfo responseInfo, SendSmsResult sms) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new SmsResponse(sms.messageRef, sms.ackPDU, sms.errorCode);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSetupDataCall(RadioResponseInfo responseInfo, SetupDataCallResult setupDataCallResult) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = RIL.convertDataCallResult(setupDataCallResult);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
                this.mRil.oppoResetSwitchDssState(ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseIccIo(RadioResponseInfo responseInfo, IccIoResult result) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new com.android.internal.telephony.uicc.IccIoResult(result.sw1, result.sw2, result.simResponse);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCallForwardInfo(RadioResponseInfo responseInfo, ArrayList<CallForwardInfo> callForwardInfos) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new CallForwardInfo[callForwardInfos.size()];
            for (int i = 0; i < callForwardInfos.size(); i++) {
                ret[i] = new CallForwardInfo();
                ret[i].status = ((CallForwardInfo) callForwardInfos.get(i)).status;
                ret[i].reason = ((CallForwardInfo) callForwardInfos.get(i)).reason;
                ret[i].serviceClass = ((CallForwardInfo) callForwardInfos.get(i)).serviceClass;
                ret[i].toa = ((CallForwardInfo) callForwardInfos.get(i)).toa;
                ret[i].number = ((CallForwardInfo) callForwardInfos.get(i)).number;
                ret[i].timeSeconds = ((CallForwardInfo) callForwardInfos.get(i)).timeSeconds;
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private static String convertOpertatorInfoToString(int status) {
        if (status == 0) {
            return "unknown";
        }
        if (status == 1) {
            return "available";
        }
        if (status == 2) {
            return "current";
        }
        if (status == 3) {
            return "forbidden";
        }
        return SpnOverride.MVNO_TYPE_NONE;
    }

    private void responseOperatorInfos(RadioResponseInfo responseInfo, ArrayList<OperatorInfo> networkInfos) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new ArrayList();
            for (int i = 0; i < networkInfos.size(); i++) {
                ret.add(new OperatorInfo(((OperatorInfo) networkInfos.get(i)).alphaLong, ((OperatorInfo) networkInfos.get(i)).alphaShort, ((OperatorInfo) networkInfos.get(i)).operatorNumeric, convertOpertatorInfoToString(((OperatorInfo) networkInfos.get(i)).status)));
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseScanStatus(RadioResponseInfo responseInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object nsr = null;
            if (responseInfo.error == 0) {
                nsr = new NetworkScanResult(1, 0, null);
                sendMessageResponse(rr.mResult, nsr);
            }
            this.mRil.processResponseDone(rr, responseInfo, nsr);
        }
    }

    private void oppoResponseOperatorInfos(RadioResponseInfo responseInfo, ArrayList<OperatorInfo> networkInfos) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new ArrayList();
            Phone phone = PhoneFactory.getPhone(this.mRil.mPhoneId.intValue());
            String[] pnnEonsLongName = new String[2];
            String[] pnnEonsShortName = new String[2];
            for (int i = 0; i < networkInfos.size(); i++) {
                this.mRil.riljLog("oppo responseOperatorInfos   networkInfos[" + i + "] ( " + networkInfos.get(i) + ")");
                String[] tmp = ((OperatorInfo) networkInfos.get(i)).operatorNumeric.split("\\+");
                String mNwType = "0";
                if (tmp.length > 1) {
                    mNwType = tmp[1];
                }
                String longName = ((OperatorInfo) networkInfos.get(i)).alphaLong;
                String shortName = ((OperatorInfo) networkInfos.get(i)).alphaShort;
                String mNumeric = tmp[0];
                this.mRil.riljLog("lookupOperatorNameFromNetwork in oppoResponseOperatorInfos(),updated longAlpha= " + longName + ",shortAlpha= " + shortName + ",numeric=" + mNumeric);
                if (OppoGsmServiceStateTracker.isMvnoPlmn(mNumeric)) {
                    pnnEonsLongName = phone != null ? phone.getPnnFromEons(mNumeric, true) : null;
                    pnnEonsShortName = phone != null ? phone.getPnnFromEons(mNumeric, false) : null;
                    if (!(pnnEonsLongName == null || (TextUtils.isEmpty(pnnEonsLongName[0]) ^ 1) == 0)) {
                        longName = pnnEonsLongName[0];
                        this.mRil.riljLog("lookupOperatorNameFromNetwork in oppoResponseOperatorInfos(),updated pnnEonsLongName[0]= " + pnnEonsLongName[0]);
                    }
                    if (!(pnnEonsShortName == null || (TextUtils.isEmpty(pnnEonsShortName[0]) ^ 1) == 0)) {
                        shortName = pnnEonsShortName[0];
                        this.mRil.riljLog("lookupOperatorNameFromNetwork in oppoResponseOperatorInfos(),updated ,pnnEonsShortName[0]= " + pnnEonsShortName[0]);
                    }
                }
                ret.add(new OperatorInfo(longName, shortName, mNumeric, convertOpertatorInfoToString(((OperatorInfo) networkInfos.get(i)).status), mNwType));
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseDataCallList(RadioResponseInfo responseInfo, ArrayList<SetupDataCallResult> dataCallResultList) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object dcResponseList = new ArrayList();
            for (SetupDataCallResult dcResult : dataCallResultList) {
                dcResponseList.add(RIL.convertDataCallResult(dcResult));
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, dcResponseList);
            }
            this.mRil.processResponseDone(rr, responseInfo, dcResponseList);
        }
    }

    private void responseCellList(RadioResponseInfo responseInfo, ArrayList<NeighboringCell> cells) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new ArrayList();
            int radioType = ((TelephonyManager) this.mRil.mContext.getSystemService("phone")).getDataNetworkType(SubscriptionManager.getSubId(this.mRil.mPhoneId.intValue())[0]);
            if (radioType != 0) {
                for (int i = 0; i < cells.size(); i++) {
                    ret.add(new NeighboringCellInfo(((NeighboringCell) cells.get(i)).rssi, ((NeighboringCell) cells.get(i)).cid, radioType));
                }
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseGmsBroadcastConfig(RadioResponseInfo responseInfo, ArrayList<GsmBroadcastSmsConfigInfo> configs) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new ArrayList();
            for (int i = 0; i < configs.size(); i++) {
                ret.add(new SmsBroadcastConfigInfo(((GsmBroadcastSmsConfigInfo) configs.get(i)).fromServiceId, ((GsmBroadcastSmsConfigInfo) configs.get(i)).toServiceId, ((GsmBroadcastSmsConfigInfo) configs.get(i)).fromCodeScheme, ((GsmBroadcastSmsConfigInfo) configs.get(i)).toCodeScheme, ((GsmBroadcastSmsConfigInfo) configs.get(i)).selected));
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCdmaBroadcastConfig(RadioResponseInfo responseInfo, ArrayList<CdmaBroadcastSmsConfigInfo> configs) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret;
            int numServiceCategories = configs.size();
            int i;
            if (numServiceCategories == 0) {
                ret = new int[94];
                ret[0] = 31;
                for (i = 1; i < 94; i += 3) {
                    ret[i + 0] = i / 3;
                    ret[i + 1] = 1;
                    ret[i + 2] = 0;
                }
            } else {
                ret = new int[((numServiceCategories * 3) + 1)];
                ret[0] = numServiceCategories;
                i = 1;
                int j = 0;
                while (j < configs.size()) {
                    int i2;
                    ret[i] = ((CdmaBroadcastSmsConfigInfo) configs.get(j)).serviceCategory;
                    ret[i + 1] = ((CdmaBroadcastSmsConfigInfo) configs.get(j)).language;
                    int i3 = i + 2;
                    if (((CdmaBroadcastSmsConfigInfo) configs.get(j)).selected) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    ret[i3] = i2;
                    j++;
                    i += 3;
                }
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCellInfoList(RadioResponseInfo responseInfo, ArrayList<CellInfo> cellInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = RIL.convertHalCellInfoList(cellInfo);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseActivityData(android.hardware.radio.V1_0.RadioResponseInfo r21, android.hardware.radio.V1_0.ActivityStatsInfo r22) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r3_3 'ret' android.telephony.ModemActivityInfo) in PHI: PHI: (r3_2 'ret' java.lang.Object) = (r3_1 'ret' java.lang.Object), (r3_3 'ret' android.telephony.ModemActivityInfo) binds: {(r3_1 'ret' java.lang.Object)=B:8:0x002d, (r3_3 'ret' android.telephony.ModemActivityInfo)=B:10:0x004e}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
	at java.lang.Iterable.forEach(Iterable.java:75)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r20 = this;
        r0 = r20;
        r4 = r0.mRil;
        r0 = r21;
        r19 = r4.processResponse(r0);
        if (r19 == 0) goto L_0x004d;
    L_0x000c:
        r3 = 0;
        r0 = r21;
        r4 = r0.error;
        if (r4 != 0) goto L_0x004e;
    L_0x0013:
        r0 = r22;
        r6 = r0.sleepModeTimeMs;
        r0 = r22;
        r7 = r0.idleModeTimeMs;
        r4 = 5;
        r8 = new int[r4];
        r2 = 0;
    L_0x001f:
        r4 = 5;
        if (r2 >= r4) goto L_0x002d;
    L_0x0022:
        r0 = r22;
        r4 = r0.txmModetimeMs;
        r4 = r4[r2];
        r8[r2] = r4;
        r2 = r2 + 1;
        goto L_0x001f;
    L_0x002d:
        r0 = r22;
        r9 = r0.rxModeTimeMs;
        r3 = new android.telephony.ModemActivityInfo;
        r4 = android.os.SystemClock.elapsedRealtime();
        r10 = 0;
        r3.<init>(r4, r6, r7, r8, r9, r10);
    L_0x003b:
        r0 = r19;
        r4 = r0.mResult;
        sendMessageResponse(r4, r3);
        r0 = r20;
        r4 = r0.mRil;
        r0 = r19;
        r1 = r21;
        r4.processResponseDone(r0, r1, r3);
    L_0x004d:
        return;
    L_0x004e:
        r3 = new android.telephony.ModemActivityInfo;
        r12 = 0;
        r4 = 5;
        r0 = new int[r4];
        r16 = r0;
        r14 = 0;
        r15 = 0;
        r17 = 0;
        r18 = 0;
        r11 = r3;
        r11.<init>(r12, r14, r15, r16, r17, r18);
        r4 = 0;
        r0 = r21;
        r0.error = r4;
        goto L_0x003b;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.RadioResponse.responseActivityData(android.hardware.radio.V1_0.RadioResponseInfo, android.hardware.radio.V1_0.ActivityStatsInfo):void");
    }

    private void responseHardwareConfig(RadioResponseInfo responseInfo, ArrayList<HardwareConfig> config) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = RIL.convertHalHwConfigList(config, this.mRil);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseICC_IOBase64(RadioResponseInfo responseInfo, IccIoResult result) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            byte[] bArr;
            int i = result.sw1;
            int i2 = result.sw2;
            if (result.simResponse.equals(SpnOverride.MVNO_TYPE_NONE)) {
                bArr = (byte[]) null;
            } else {
                bArr = Base64.decode(result.simResponse, 0);
            }
            Object ret = new com.android.internal.telephony.uicc.IccIoResult(i, i2, bArr);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseRadioCapability(RadioResponseInfo responseInfo, RadioCapability rc) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = RIL.convertHalRadioCapability(rc, this.mRil);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseLceStatus(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new ArrayList();
            ret.add(Integer.valueOf(statusInfo.lceStatus));
            ret.add(Integer.valueOf(Byte.toUnsignedInt(statusInfo.actualIntervalMs)));
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseLceData(RadioResponseInfo responseInfo, LceDataInfo lceInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = RIL.convertHalLceData(lceInfo, this.mRil);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCarrierIdentifiers(RadioResponseInfo responseInfo, boolean allAllowed, CarrierRestrictions carriers) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = new ArrayList();
            for (int i = 0; i < carriers.allowedCarriers.size(); i++) {
                String mcc = ((Carrier) carriers.allowedCarriers.get(i)).mcc;
                String mnc = ((Carrier) carriers.allowedCarriers.get(i)).mnc;
                String spn = null;
                String imsi = null;
                String gid1 = null;
                String gid2 = null;
                int matchType = ((Carrier) carriers.allowedCarriers.get(i)).matchType;
                String matchData = ((Carrier) carriers.allowedCarriers.get(i)).matchData;
                if (matchType == 1) {
                    spn = matchData;
                } else if (matchType == 2) {
                    imsi = matchData;
                } else if (matchType == 3) {
                    gid1 = matchData;
                } else if (matchType == 4) {
                    gid2 = matchData;
                }
                ret.add(new CarrierIdentifier(mcc, mnc, spn, imsi, gid1, gid2));
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }
}
