package gov.nist.javax.sip;

import gov.nist.javax.sip.header.RetryAfter;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import gov.nist.javax.sip.stack.ServerRequestInterface;
import gov.nist.javax.sip.stack.ServerResponseInterface;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.SipException;
import javax.sip.header.Header;
import javax.sip.header.ServerHeader;
import javax.sip.message.Response;

class DialogFilter implements ServerRequestInterface, ServerResponseInterface {
    protected ListeningPointImpl listeningPoint;
    private SipStackImpl sipStack;
    protected SIPTransaction transactionChannel;

    public DialogFilter(SipStackImpl sipStack) {
        this.sipStack = sipStack;
    }

    private void sendRequestPendingResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        Response sipResponse = sipRequest.createResponse(Response.REQUEST_PENDING);
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader((Header) serverHeader);
        }
        try {
            RetryAfter retryAfter = new RetryAfter();
            retryAfter.setRetryAfter(1);
            sipResponse.setHeader((Header) retryAfter);
            if (sipRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(transaction);
            }
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendBadRequestResponse(SIPRequest sipRequest, SIPServerTransaction transaction, String reasonPhrase) {
        Response sipResponse = sipRequest.createResponse(Response.BAD_REQUEST);
        if (reasonPhrase != null) {
            sipResponse.setReasonPhrase(reasonPhrase);
        }
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader((Header) serverHeader);
        }
        try {
            if (sipRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(transaction);
            }
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendCallOrTransactionDoesNotExistResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        Response sipResponse = sipRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader((Header) serverHeader);
        }
        try {
            if (sipRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(transaction);
            }
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendLoopDetectedResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        Response sipResponse = sipRequest.createResponse(Response.LOOP_DETECTED);
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader((Header) serverHeader);
        }
        try {
            this.sipStack.addTransactionPendingAck(transaction);
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendServerInternalErrorResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Sending 500 response for out of sequence message");
        }
        Response sipResponse = sipRequest.createResponse(500);
        sipResponse.setReasonPhrase("Request out of order");
        if (MessageFactoryImpl.getDefaultServerHeader() != null) {
            sipResponse.setHeader((Header) MessageFactoryImpl.getDefaultServerHeader());
        }
        try {
            RetryAfter retryAfter = new RetryAfter();
            retryAfter.setRetryAfter(10);
            sipResponse.setHeader((Header) retryAfter);
            this.sipStack.addTransactionPendingAck(transaction);
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    public void processRequest(gov.nist.javax.sip.message.SIPRequest r47, gov.nist.javax.sip.stack.MessageChannel r48) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r32_0 'sipEvent' java.util.EventObject) in PHI: PHI: (r32_3 'sipEvent' java.util.EventObject) = (r32_0 'sipEvent' java.util.EventObject), (r32_1 'sipEvent' java.util.EventObject), (r32_2 'sipEvent' java.util.EventObject), (r32_4 'sipEvent' java.util.EventObject), (r32_5 'sipEvent' java.util.EventObject) binds: {(r32_0 'sipEvent' java.util.EventObject)=B:470:0x0c91, (r32_1 'sipEvent' java.util.EventObject)=B:484:0x0d04, (r32_2 'sipEvent' java.util.EventObject)=B:488:0x0d26, (r32_4 'sipEvent' java.util.EventObject)=B:492:0x0d43, (r32_5 'sipEvent' java.util.EventObject)=B:493:0x0d52}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r46 = this;
        r0 = r46;
        r0 = r0.sipStack;
        r41 = r0;
        r41 = r41.isLoggingEnabled();
        if (r41 == 0) goto L_0x006c;
    L_0x000c:
        r0 = r46;
        r0 = r0.sipStack;
        r41 = r0;
        r41 = r41.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "PROCESSING INCOMING REQUEST ";
        r42 = r42.append(r43);
        r0 = r42;
        r1 = r47;
        r42 = r0.append(r1);
        r43 = " transactionChannel = ";
        r42 = r42.append(r43);
        r0 = r46;
        r0 = r0.transactionChannel;
        r43 = r0;
        r42 = r42.append(r43);
        r43 = " listening point = ";
        r42 = r42.append(r43);
        r0 = r46;
        r0 = r0.listeningPoint;
        r43 = r0;
        r43 = r43.getIPAddress();
        r42 = r42.append(r43);
        r43 = ":";
        r42 = r42.append(r43);
        r0 = r46;
        r0 = r0.listeningPoint;
        r43 = r0;
        r43 = r43.getPort();
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x006c:
        r0 = r46;
        r0 = r0.listeningPoint;
        r41 = r0;
        if (r41 != 0) goto L_0x0091;
    L_0x0074:
        r0 = r46;
        r0 = r0.sipStack;
        r41 = r0;
        r41 = r41.isLoggingEnabled();
        if (r41 == 0) goto L_0x0090;
    L_0x0080:
        r0 = r46;
        r0 = r0.sipStack;
        r41 = r0;
        r41 = r41.getStackLogger();
        r42 = "Dropping message: No listening point registered!";
        r41.logDebug(r42);
    L_0x0090:
        return;
    L_0x0091:
        r0 = r46;
        r0 = r0.transactionChannel;
        r41 = r0;
        r36 = r41.getSIPStack();
        r36 = (gov.nist.javax.sip.SipStackImpl) r36;
        r0 = r46;
        r0 = r0.listeningPoint;
        r41 = r0;
        r34 = r41.getProvider();
        if (r34 != 0) goto L_0x00ba;
    L_0x00a9:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x00b9;
    L_0x00af:
        r41 = r36.getStackLogger();
        r42 = "No provider - dropping !!";
        r41.logDebug(r42);
    L_0x00b9:
        return;
    L_0x00ba:
        if (r36 != 0) goto L_0x00c2;
    L_0x00bc:
        r41 = "Egads! no sip stack!";
        gov.nist.core.InternalErrorHandler.handleException(r41);
    L_0x00c2:
        r0 = r46;
        r0 = r0.transactionChannel;
        r39 = r0;
        r39 = (gov.nist.javax.sip.stack.SIPServerTransaction) r39;
        if (r39 == 0) goto L_0x00f1;
    L_0x00cc:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x00f1;
    L_0x00d2:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "transaction state = ";
        r42 = r42.append(r43);
        r43 = r39.getState();
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x00f1:
        r41 = 1;
        r0 = r47;
        r1 = r41;
        r14 = r0.getDialogId(r1);
        r0 = r36;
        r11 = r0.getDialog(r14);
        if (r11 == 0) goto L_0x01a9;
    L_0x0103:
        r41 = r11.getSipProvider();
        r0 = r34;
        r1 = r41;
        if (r0 == r1) goto L_0x01a9;
    L_0x010d:
        r7 = r11.getMyContactHeader();
        if (r7 == 0) goto L_0x01a9;
    L_0x0113:
        r41 = r7.getAddress();
        r10 = r41.getURI();
        r10 = (gov.nist.javax.sip.address.SipUri) r10;
        r22 = r10.getHost();
        r8 = r10.getPort();
        r9 = r10.getTransportParam();
        if (r9 != 0) goto L_0x012e;
    L_0x012b:
        r9 = "udp";
    L_0x012e:
        r41 = -1;
        r0 = r41;
        if (r8 != r0) goto L_0x014c;
    L_0x0134:
        r41 = "udp";
        r0 = r41;
        r41 = r9.equals(r0);
        if (r41 != 0) goto L_0x014a;
    L_0x013f:
        r41 = "tcp";
        r0 = r41;
        r41 = r9.equals(r0);
        if (r41 == 0) goto L_0x01cf;
    L_0x014a:
        r8 = 5060; // 0x13c4 float:7.09E-42 double:2.5E-320;
    L_0x014c:
        if (r22 == 0) goto L_0x01a9;
    L_0x014e:
        r0 = r46;
        r0 = r0.listeningPoint;
        r41 = r0;
        r41 = r41.getIPAddress();
        r0 = r22;
        r1 = r41;
        r41 = r0.equals(r1);
        if (r41 == 0) goto L_0x0170;
    L_0x0162:
        r0 = r46;
        r0 = r0.listeningPoint;
        r41 = r0;
        r41 = r41.getPort();
        r0 = r41;
        if (r8 == r0) goto L_0x01a9;
    L_0x0170:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x01a8;
    L_0x0176:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "nulling dialog -- listening point mismatch!  ";
        r42 = r42.append(r43);
        r0 = r42;
        r42 = r0.append(r8);
        r43 = "  lp port = ";
        r42 = r42.append(r43);
        r0 = r46;
        r0 = r0.listeningPoint;
        r43 = r0;
        r43 = r43.getPort();
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x01a8:
        r11 = 0;
    L_0x01a9:
        r41 = r34.isAutomaticDialogSupportEnabled();
        if (r41 == 0) goto L_0x01d3;
    L_0x01af:
        r41 = r34.isDialogErrorsAutomaticallyHandled();
        if (r41 == 0) goto L_0x01d3;
    L_0x01b5:
        r41 = r47.getToTag();
        if (r41 != 0) goto L_0x01d3;
    L_0x01bb:
        r0 = r36;
        r1 = r47;
        r35 = r0.findMergedTransaction(r1);
        if (r35 == 0) goto L_0x01d3;
    L_0x01c5:
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r0.sendLoopDetectedResponse(r1, r2);
        return;
    L_0x01cf:
        r8 = 5061; // 0x13c5 float:7.092E-42 double:2.5005E-320;
        goto L_0x014c;
    L_0x01d3:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0213;
    L_0x01d9:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "dialogId = ";
        r42 = r42.append(r43);
        r0 = r42;
        r42 = r0.append(r14);
        r42 = r42.toString();
        r41.logDebug(r42);
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "dialog = ";
        r42 = r42.append(r43);
        r0 = r42;
        r42 = r0.append(r11);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x0213:
        r41 = "Route";
        r0 = r47;
        r1 = r41;
        r41 = r0.getHeader(r1);
        if (r41 == 0) goto L_0x029e;
    L_0x0220:
        r41 = r39.getDialog();
        if (r41 == 0) goto L_0x029e;
    L_0x0226:
        r31 = r47.getRouteHeaders();
        r30 = r31.getFirst();
        r30 = (gov.nist.javax.sip.header.Route) r30;
        r41 = r30.getAddress();
        r40 = r41.getURI();
        r40 = (gov.nist.javax.sip.address.SipUri) r40;
        r41 = r40.getHostPort();
        r41 = r41.hasPort();
        if (r41 == 0) goto L_0x02d1;
    L_0x0244:
        r41 = r40.getHostPort();
        r28 = r41.getPort();
    L_0x024c:
        r21 = r40.getHost();
        r0 = r46;
        r0 = r0.listeningPoint;
        r41 = r0;
        r41 = r41.getIPAddress();
        r0 = r21;
        r1 = r41;
        r41 = r0.equals(r1);
        if (r41 != 0) goto L_0x0278;
    L_0x0264:
        r0 = r46;
        r0 = r0.listeningPoint;
        r41 = r0;
        r41 = r41.getSentBy();
        r0 = r21;
        r1 = r41;
        r41 = r0.equalsIgnoreCase(r1);
        if (r41 == 0) goto L_0x029e;
    L_0x0278:
        r0 = r46;
        r0 = r0.listeningPoint;
        r41 = r0;
        r41 = r41.getPort();
        r0 = r28;
        r1 = r41;
        if (r0 != r1) goto L_0x029e;
    L_0x0288:
        r41 = r31.size();
        r42 = 1;
        r0 = r41;
        r1 = r42;
        if (r0 != r1) goto L_0x02ec;
    L_0x0294:
        r41 = "Route";
        r0 = r47;
        r1 = r41;
        r0.removeHeader(r1);
    L_0x029e:
        r41 = r47.getMethod();
        r42 = "REFER";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x035a;
    L_0x02ab:
        if (r11 == 0) goto L_0x035a;
    L_0x02ad:
        r41 = r34.isDialogErrorsAutomaticallyHandled();
        if (r41 == 0) goto L_0x035a;
    L_0x02b3:
        r41 = "Refer-To";
        r0 = r47;
        r1 = r41;
        r33 = r0.getHeader(r1);
        r33 = (javax.sip.header.ReferToHeader) r33;
        if (r33 != 0) goto L_0x02f0;
    L_0x02c2:
        r41 = "Refer-To header is missing";
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r3 = r41;
        r0.sendBadRequestResponse(r1, r2, r3);
        return;
    L_0x02d1:
        r0 = r46;
        r0 = r0.listeningPoint;
        r41 = r0;
        r41 = r41.getTransport();
        r42 = "TLS";
        r41 = r41.equalsIgnoreCase(r42);
        if (r41 == 0) goto L_0x02e8;
    L_0x02e4:
        r28 = 5061; // 0x13c5 float:7.092E-42 double:2.5005E-320;
        goto L_0x024c;
    L_0x02e8:
        r28 = 5060; // 0x13c4 float:7.09E-42 double:2.5E-320;
        goto L_0x024c;
    L_0x02ec:
        r31.removeFirst();
        goto L_0x029e;
    L_0x02f0:
        r24 = r11.getLastTransaction();
        if (r24 == 0) goto L_0x039e;
    L_0x02f6:
        r41 = r34.isDialogErrorsAutomaticallyHandled();
        if (r41 == 0) goto L_0x039e;
    L_0x02fc:
        r23 = r24.getRequest();
        r23 = (gov.nist.javax.sip.message.SIPRequest) r23;
        r0 = r24;
        r0 = r0 instanceof gov.nist.javax.sip.stack.SIPServerTransaction;
        r41 = r0;
        if (r41 == 0) goto L_0x0327;
    L_0x030a:
        r41 = r11.isAckSeen();
        if (r41 != 0) goto L_0x039e;
    L_0x0310:
        r41 = r23.getMethod();
        r42 = "INVITE";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x039e;
    L_0x031d:
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r0.sendRequestPendingResponse(r1, r2);
        return;
    L_0x0327:
        r0 = r24;
        r0 = r0 instanceof gov.nist.javax.sip.stack.SIPClientTransaction;
        r41 = r0;
        if (r41 == 0) goto L_0x039e;
    L_0x032f:
        r41 = r23.getCSeqHeader();
        r12 = r41.getSeqNumber();
        r25 = r23.getMethod();
        r41 = "INVITE";
        r0 = r25;
        r1 = r41;
        r41 = r0.equals(r1);
        if (r41 == 0) goto L_0x039e;
    L_0x0348:
        r41 = r11.isAckSent(r12);
        r41 = r41 ^ 1;
        if (r41 == 0) goto L_0x039e;
    L_0x0350:
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r0.sendRequestPendingResponse(r1, r2);
        return;
    L_0x035a:
        r41 = r47.getMethod();
        r42 = "UPDATE";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x0379;
    L_0x0367:
        r41 = r34.isAutomaticDialogSupportEnabled();
        if (r41 == 0) goto L_0x039e;
    L_0x036d:
        if (r11 != 0) goto L_0x039e;
    L_0x036f:
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r0.sendCallOrTransactionDoesNotExistResponse(r1, r2);
        return;
    L_0x0379:
        r41 = r47.getMethod();
        r42 = "ACK";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x0632;
    L_0x0386:
        if (r39 == 0) goto L_0x0488;
    L_0x0388:
        r41 = r39.isInviteTransaction();
        if (r41 == 0) goto L_0x0488;
    L_0x038e:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x039e;
    L_0x0394:
        r41 = r36.getStackLogger();
        r42 = "Processing ACK for INVITE Tx ";
        r41.logDebug(r42);
    L_0x039e:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x03d0;
    L_0x03a4:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "CHECK FOR OUT OF SEQ MESSAGE ";
        r42 = r42.append(r43);
        r0 = r42;
        r42 = r0.append(r11);
        r43 = " transaction ";
        r42 = r42.append(r43);
        r0 = r42;
        r1 = r39;
        r42 = r0.append(r1);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x03d0:
        if (r11 == 0) goto L_0x0afd;
    L_0x03d2:
        if (r39 == 0) goto L_0x0afd;
    L_0x03d4:
        r41 = r47.getMethod();
        r42 = "BYE";
        r41 = r41.equals(r42);
        r41 = r41 ^ 1;
        if (r41 == 0) goto L_0x0afd;
    L_0x03e3:
        r41 = r47.getMethod();
        r42 = "CANCEL";
        r41 = r41.equals(r42);
        r41 = r41 ^ 1;
        if (r41 == 0) goto L_0x0afd;
    L_0x03f2:
        r41 = r47.getMethod();
        r42 = "ACK";
        r41 = r41.equals(r42);
        r41 = r41 ^ 1;
        if (r41 == 0) goto L_0x0afd;
    L_0x0401:
        r41 = r47.getMethod();
        r42 = "PRACK";
        r41 = r41.equals(r42);
        r41 = r41 ^ 1;
        if (r41 == 0) goto L_0x0afd;
    L_0x0410:
        r0 = r47;
        r41 = r11.isRequestConsumable(r0);
        if (r41 != 0) goto L_0x0add;
    L_0x0418:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0450;
    L_0x041e:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "Dropping out of sequence message ";
        r42 = r42.append(r43);
        r44 = r11.getRemoteSeqNumber();
        r0 = r42;
        r1 = r44;
        r42 = r0.append(r1);
        r43 = " ";
        r42 = r42.append(r43);
        r43 = r47.getCSeq();
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x0450:
        r42 = r11.getRemoteSeqNumber();
        r41 = r47.getCSeq();
        r44 = r41.getSeqNumber();
        r41 = (r42 > r44 ? 1 : (r42 == r44 ? 0 : -1));
        if (r41 < 0) goto L_0x0487;
    L_0x0460:
        r41 = r34.isDialogErrorsAutomaticallyHandled();
        if (r41 == 0) goto L_0x0487;
    L_0x0466:
        r41 = r39.getState();
        r42 = javax.sip.TransactionState.TRYING;
        r0 = r41;
        r1 = r42;
        if (r0 == r1) goto L_0x047e;
    L_0x0472:
        r41 = r39.getState();
        r42 = javax.sip.TransactionState.PROCEEDING;
        r0 = r41;
        r1 = r42;
        if (r0 != r1) goto L_0x0487;
    L_0x047e:
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r0.sendServerInternalErrorResponse(r1, r2);
    L_0x0487:
        return;
    L_0x0488:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x04ab;
    L_0x048e:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "Processing ACK for dialog ";
        r42 = r42.append(r43);
        r0 = r42;
        r42 = r0.append(r11);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x04ab:
        if (r11 != 0) goto L_0x0530;
    L_0x04ad:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x04df;
    L_0x04b3:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "Dialog does not exist ";
        r42 = r42.append(r43);
        r43 = r47.getFirstLine();
        r42 = r42.append(r43);
        r43 = " isServerTransaction = ";
        r42 = r42.append(r43);
        r43 = 1;
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x04df:
        r0 = r36;
        r37 = r0.getRetransmissionAlertTransaction(r14);
        if (r37 == 0) goto L_0x04f0;
    L_0x04e7:
        r41 = r37.isRetransmissionAlertEnabled();
        if (r41 == 0) goto L_0x04f0;
    L_0x04ed:
        r37.disableRetransmissionAlerts();
    L_0x04f0:
        r0 = r36;
        r1 = r47;
        r6 = r0.findTransactionPendingAck(r1);
        if (r6 == 0) goto L_0x039e;
    L_0x04fa:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x050a;
    L_0x0500:
        r41 = r36.getStackLogger();
        r42 = "Found Tx pending ACK";
        r41.logDebug(r42);
    L_0x050a:
        r6.setAckSeen();	 Catch:{ Exception -> 0x0518 }
        r0 = r36;	 Catch:{ Exception -> 0x0518 }
        r0.removeTransaction(r6);	 Catch:{ Exception -> 0x0518 }
        r0 = r36;	 Catch:{ Exception -> 0x0518 }
        r0.removeTransactionPendingAck(r6);	 Catch:{ Exception -> 0x0518 }
    L_0x0517:
        return;
    L_0x0518:
        r19 = move-exception;
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0517;
    L_0x051f:
        r41 = r36.getStackLogger();
        r42 = "Problem terminating transaction";
        r0 = r41;
        r1 = r42;
        r2 = r19;
        r0.logError(r1, r2);
        goto L_0x0517;
    L_0x0530:
        r0 = r39;
        r41 = r11.handleAck(r0);
        if (r41 != 0) goto L_0x05e2;
    L_0x0538:
        r41 = r11.isSequnceNumberValidation();
        if (r41 != 0) goto L_0x0592;
    L_0x053e:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x057f;
    L_0x0544:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "Dialog exists with loose dialog validation ";
        r42 = r42.append(r43);
        r43 = r47.getFirstLine();
        r42 = r42.append(r43);
        r43 = " isServerTransaction = ";
        r42 = r42.append(r43);
        r43 = 1;
        r42 = r42.append(r43);
        r43 = " dialog = ";
        r42 = r42.append(r43);
        r43 = r11.getDialogId();
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x057f:
        r0 = r36;
        r37 = r0.getRetransmissionAlertTransaction(r14);
        if (r37 == 0) goto L_0x039e;
    L_0x0587:
        r41 = r37.isRetransmissionAlertEnabled();
        if (r41 == 0) goto L_0x039e;
    L_0x058d:
        r37.disableRetransmissionAlerts();
        goto L_0x039e;
    L_0x0592:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x05a2;
    L_0x0598:
        r41 = r36.getStackLogger();
        r42 = "Dropping ACK - cannot find a transaction or dialog";
        r41.logDebug(r42);
    L_0x05a2:
        r0 = r36;
        r1 = r47;
        r6 = r0.findTransactionPendingAck(r1);
        if (r6 == 0) goto L_0x05c9;
    L_0x05ac:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x05bc;
    L_0x05b2:
        r41 = r36.getStackLogger();
        r42 = "Found Tx pending ACK";
        r41.logDebug(r42);
    L_0x05bc:
        r6.setAckSeen();	 Catch:{ Exception -> 0x05ca }
        r0 = r36;	 Catch:{ Exception -> 0x05ca }
        r0.removeTransaction(r6);	 Catch:{ Exception -> 0x05ca }
        r0 = r36;	 Catch:{ Exception -> 0x05ca }
        r0.removeTransactionPendingAck(r6);	 Catch:{ Exception -> 0x05ca }
    L_0x05c9:
        return;
    L_0x05ca:
        r19 = move-exception;
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x05c9;
    L_0x05d1:
        r41 = r36.getStackLogger();
        r42 = "Problem terminating transaction";
        r0 = r41;
        r1 = r42;
        r2 = r19;
        r0.logError(r1, r2);
        goto L_0x05c9;
    L_0x05e2:
        r39.passToListener();
        r0 = r39;
        r11.addTransaction(r0);
        r0 = r47;
        r11.addRoute(r0);
        r0 = r39;
        r0.setDialog(r11, r14);
        r41 = r47.getMethod();
        r42 = "INVITE";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x0610;
    L_0x0601:
        r41 = r34.isDialogErrorsAutomaticallyHandled();
        if (r41 == 0) goto L_0x0610;
    L_0x0607:
        r0 = r36;
        r1 = r39;
        r2 = r47;
        r0.putInMergeTable(r1, r2);
    L_0x0610:
        r0 = r36;
        r0 = r0.deliverTerminatedEventForAck;
        r41 = r0;
        if (r41 == 0) goto L_0x0627;
    L_0x0618:
        r0 = r36;	 Catch:{ IOException -> 0x0624 }
        r1 = r39;	 Catch:{ IOException -> 0x0624 }
        r0.addTransaction(r1);	 Catch:{ IOException -> 0x0624 }
        r39.scheduleAckRemoval();	 Catch:{ IOException -> 0x0624 }
        goto L_0x039e;
    L_0x0624:
        r18 = move-exception;
        goto L_0x039e;
    L_0x0627:
        r41 = 1;
        r0 = r39;
        r1 = r41;
        r0.setMapped(r1);
        goto L_0x039e;
    L_0x0632:
        r41 = r47.getMethod();
        r42 = "PRACK";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x0731;
    L_0x063f:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0662;
    L_0x0645:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "Processing PRACK for dialog ";
        r42 = r42.append(r43);
        r0 = r42;
        r42 = r0.append(r11);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x0662:
        if (r11 != 0) goto L_0x06da;
    L_0x0664:
        r41 = r34.isAutomaticDialogSupportEnabled();
        if (r41 == 0) goto L_0x06da;
    L_0x066a:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x069c;
    L_0x0670:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "Dialog does not exist ";
        r42 = r42.append(r43);
        r43 = r47.getFirstLine();
        r42 = r42.append(r43);
        r43 = " isServerTransaction = ";
        r42 = r42.append(r43);
        r43 = 1;
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x069c:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x06ac;
    L_0x06a2:
        r41 = r36.getStackLogger();
        r42 = "Sending 481 for PRACK - automatic dialog support is enabled -- cant find dialog!";
        r41.logDebug(r42);
    L_0x06ac:
        r41 = 481; // 0x1e1 float:6.74E-43 double:2.376E-321;
        r0 = r47;
        r1 = r41;
        r26 = r0.createResponse(r1);
        r0 = r34;	 Catch:{ SipException -> 0x06ca }
        r1 = r26;	 Catch:{ SipException -> 0x06ca }
        r0.sendResponse(r1);	 Catch:{ SipException -> 0x06ca }
    L_0x06bd:
        if (r39 == 0) goto L_0x06c9;
    L_0x06bf:
        r0 = r36;
        r1 = r39;
        r0.removeTransaction(r1);
        r39.releaseSem();
    L_0x06c9:
        return;
    L_0x06ca:
        r15 = move-exception;
        r41 = r36.getStackLogger();
        r42 = "error sending response";
        r0 = r41;
        r1 = r42;
        r0.logError(r1, r15);
        goto L_0x06bd;
    L_0x06da:
        if (r11 == 0) goto L_0x071f;
    L_0x06dc:
        r0 = r47;
        r41 = r11.handlePrack(r0);
        if (r41 != 0) goto L_0x0701;
    L_0x06e4:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x06f4;
    L_0x06ea:
        r41 = r36.getStackLogger();
        r42 = "Dropping out of sequence PRACK ";
        r41.logDebug(r42);
    L_0x06f4:
        if (r39 == 0) goto L_0x0700;
    L_0x06f6:
        r0 = r36;
        r1 = r39;
        r0.removeTransaction(r1);
        r39.releaseSem();
    L_0x0700:
        return;
    L_0x0701:
        r0 = r36;	 Catch:{ Exception -> 0x0719 }
        r1 = r39;	 Catch:{ Exception -> 0x0719 }
        r0.addTransaction(r1);	 Catch:{ Exception -> 0x0719 }
        r0 = r39;	 Catch:{ Exception -> 0x0719 }
        r11.addTransaction(r0);	 Catch:{ Exception -> 0x0719 }
        r0 = r47;	 Catch:{ Exception -> 0x0719 }
        r11.addRoute(r0);	 Catch:{ Exception -> 0x0719 }
        r0 = r39;	 Catch:{ Exception -> 0x0719 }
        r0.setDialog(r11, r14);	 Catch:{ Exception -> 0x0719 }
        goto L_0x039e;
    L_0x0719:
        r19 = move-exception;
        gov.nist.core.InternalErrorHandler.handleException(r19);
        goto L_0x039e;
    L_0x071f:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x039e;
    L_0x0725:
        r41 = r36.getStackLogger();
        r42 = "Processing PRACK without a DIALOG -- this must be a proxy element";
        r41.logDebug(r42);
        goto L_0x039e;
    L_0x0731:
        r41 = r47.getMethod();
        r42 = "BYE";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x0867;
    L_0x073e:
        if (r11 == 0) goto L_0x07b9;
    L_0x0740:
        r0 = r47;
        r41 = r11.isRequestConsumable(r0);
        r41 = r41 ^ 1;
        if (r41 == 0) goto L_0x07b9;
    L_0x074a:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x078a;
    L_0x0750:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "Dropping out of sequence BYE ";
        r42 = r42.append(r43);
        r44 = r11.getRemoteSeqNumber();
        r0 = r42;
        r1 = r44;
        r42 = r0.append(r1);
        r43 = " ";
        r42 = r42.append(r43);
        r43 = r47.getCSeq();
        r44 = r43.getSeqNumber();
        r0 = r42;
        r1 = r44;
        r42 = r0.append(r1);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x078a:
        r42 = r11.getRemoteSeqNumber();
        r41 = r47.getCSeq();
        r44 = r41.getSeqNumber();
        r41 = (r42 > r44 ? 1 : (r42 == r44 ? 0 : -1));
        if (r41 < 0) goto L_0x07af;
    L_0x079a:
        r41 = r39.getState();
        r42 = javax.sip.TransactionState.TRYING;
        r0 = r41;
        r1 = r42;
        if (r0 != r1) goto L_0x07af;
    L_0x07a6:
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r0.sendServerInternalErrorResponse(r1, r2);
    L_0x07af:
        if (r39 == 0) goto L_0x07b8;
    L_0x07b1:
        r0 = r36;
        r1 = r39;
        r0.removeTransaction(r1);
    L_0x07b8:
        return;
    L_0x07b9:
        if (r11 != 0) goto L_0x080d;
    L_0x07bb:
        r41 = r34.isAutomaticDialogSupportEnabled();
        if (r41 == 0) goto L_0x080d;
    L_0x07c1:
        r41 = 481; // 0x1e1 float:6.74E-43 double:2.376E-321;
        r0 = r47;
        r1 = r41;
        r29 = r0.createResponse(r1);
        r41 = "Dialog Not Found";
        r0 = r29;
        r1 = r41;
        r0.setReasonPhrase(r1);
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x07e5;
    L_0x07db:
        r41 = r36.getStackLogger();
        r42 = "dropping request -- automatic dialog support enabled and dialog does not exist!";
        r41.logDebug(r42);
    L_0x07e5:
        r0 = r39;	 Catch:{ SipException -> 0x07fb }
        r1 = r29;	 Catch:{ SipException -> 0x07fb }
        r0.sendResponse(r1);	 Catch:{ SipException -> 0x07fb }
    L_0x07ec:
        if (r39 == 0) goto L_0x07fa;
    L_0x07ee:
        r0 = r36;
        r1 = r39;
        r0.removeTransaction(r1);
        r39.releaseSem();
        r39 = 0;
    L_0x07fa:
        return;
    L_0x07fb:
        r20 = move-exception;
        r41 = r36.getStackLogger();
        r42 = "Error in sending response";
        r0 = r41;
        r1 = r42;
        r2 = r20;
        r0.logError(r1, r2);
        goto L_0x07ec;
    L_0x080d:
        if (r39 == 0) goto L_0x082c;
    L_0x080f:
        if (r11 == 0) goto L_0x082c;
    L_0x0811:
        r41 = r11.getSipProvider();	 Catch:{ IOException -> 0x0862 }
        r0 = r34;	 Catch:{ IOException -> 0x0862 }
        r1 = r41;	 Catch:{ IOException -> 0x0862 }
        if (r0 != r1) goto L_0x082c;	 Catch:{ IOException -> 0x0862 }
    L_0x081b:
        r0 = r36;	 Catch:{ IOException -> 0x0862 }
        r1 = r39;	 Catch:{ IOException -> 0x0862 }
        r0.addTransaction(r1);	 Catch:{ IOException -> 0x0862 }
        r0 = r39;	 Catch:{ IOException -> 0x0862 }
        r11.addTransaction(r0);	 Catch:{ IOException -> 0x0862 }
        r0 = r39;	 Catch:{ IOException -> 0x0862 }
        r0.setDialog(r11, r14);	 Catch:{ IOException -> 0x0862 }
    L_0x082c:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x039e;
    L_0x0832:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "BYE Tx = ";
        r42 = r42.append(r43);
        r0 = r42;
        r1 = r39;
        r42 = r0.append(r1);
        r43 = " isMapped =";
        r42 = r42.append(r43);
        r43 = r39.isTransactionMapped();
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
        goto L_0x039e;
    L_0x0862:
        r18 = move-exception;
        gov.nist.core.InternalErrorHandler.handleException(r18);
        goto L_0x082c;
    L_0x0867:
        r41 = r47.getMethod();
        r42 = "CANCEL";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x09b8;
    L_0x0874:
        r41 = 1;
        r0 = r36;
        r1 = r47;
        r2 = r41;
        r37 = r0.findCancelTransaction(r1, r2);
        r37 = (gov.nist.javax.sip.stack.SIPServerTransaction) r37;
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x08c5;
    L_0x0888:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "Got a CANCEL, InviteServerTx = ";
        r42 = r42.append(r43);
        r0 = r42;
        r1 = r37;
        r42 = r0.append(r1);
        r43 = " cancel Server Tx ID = ";
        r42 = r42.append(r43);
        r0 = r42;
        r1 = r39;
        r42 = r0.append(r1);
        r43 = " isMapped = ";
        r42 = r42.append(r43);
        r43 = r39.isTransactionMapped();
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x08c5:
        r41 = r47.getMethod();
        r42 = "CANCEL";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x093e;
    L_0x08d2:
        if (r37 == 0) goto L_0x0919;
    L_0x08d4:
        r41 = r37.getState();
        r42 = gov.nist.javax.sip.stack.SIPTransaction.TERMINATED_STATE;
        r0 = r41;
        r1 = r42;
        if (r0 != r1) goto L_0x0919;
    L_0x08e0:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x08f0;
    L_0x08e6:
        r41 = r36.getStackLogger();
        r42 = "Too late to cancel Transaction";
        r41.logDebug(r42);
    L_0x08f0:
        r41 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;
        r0 = r47;	 Catch:{ Exception -> 0x0902 }
        r1 = r41;	 Catch:{ Exception -> 0x0902 }
        r41 = r0.createResponse(r1);	 Catch:{ Exception -> 0x0902 }
        r0 = r39;	 Catch:{ Exception -> 0x0902 }
        r1 = r41;	 Catch:{ Exception -> 0x0902 }
        r0.sendResponse(r1);	 Catch:{ Exception -> 0x0902 }
    L_0x0901:
        return;
    L_0x0902:
        r19 = move-exception;
        r41 = r19.getCause();
        if (r41 == 0) goto L_0x0901;
    L_0x0909:
        r41 = r19.getCause();
        r0 = r41;
        r0 = r0 instanceof java.io.IOException;
        r41 = r0;
        if (r41 == 0) goto L_0x0901;
    L_0x0915:
        r37.raiseIOExceptionEvent();
        goto L_0x0901;
    L_0x0919:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x093e;
    L_0x091f:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "Cancel transaction = ";
        r42 = r42.append(r43);
        r0 = r42;
        r1 = r37;
        r42 = r0.append(r1);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x093e:
        if (r39 == 0) goto L_0x097b;
    L_0x0940:
        if (r37 == 0) goto L_0x097b;
    L_0x0942:
        r41 = r37.getDialog();
        if (r41 == 0) goto L_0x097b;
    L_0x0948:
        r41 = r37.getDialog();
        r41 = (gov.nist.javax.sip.stack.SIPDialog) r41;
        r0 = r39;
        r1 = r41;
        r0.setDialog(r1, r14);
        r11 = r37.getDialog();
        r11 = (gov.nist.javax.sip.stack.SIPDialog) r11;
    L_0x095b:
        if (r37 == 0) goto L_0x039e;
    L_0x095d:
        if (r39 == 0) goto L_0x039e;
    L_0x095f:
        r0 = r36;	 Catch:{ Exception -> 0x0975 }
        r1 = r39;	 Catch:{ Exception -> 0x0975 }
        r0.addTransaction(r1);	 Catch:{ Exception -> 0x0975 }
        r39.setPassToListener();	 Catch:{ Exception -> 0x0975 }
        r0 = r39;	 Catch:{ Exception -> 0x0975 }
        r1 = r37;	 Catch:{ Exception -> 0x0975 }
        r0.setInviteTransaction(r1);	 Catch:{ Exception -> 0x0975 }
        r37.acquireSem();	 Catch:{ Exception -> 0x0975 }
        goto L_0x039e;
    L_0x0975:
        r19 = move-exception;
        gov.nist.core.InternalErrorHandler.handleException(r19);
        goto L_0x039e;
    L_0x097b:
        if (r37 != 0) goto L_0x095b;
    L_0x097d:
        r41 = r34.isAutomaticDialogSupportEnabled();
        if (r41 == 0) goto L_0x095b;
    L_0x0983:
        if (r39 == 0) goto L_0x095b;
    L_0x0985:
        r41 = 481; // 0x1e1 float:6.74E-43 double:2.376E-321;
        r0 = r47;
        r1 = r41;
        r29 = r0.createResponse(r1);
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x099f;
    L_0x0995:
        r41 = r36.getStackLogger();
        r42 = "dropping request -- automatic dialog support enabled and INVITE ST does not exist!";
        r41.logDebug(r42);
    L_0x099f:
        r0 = r34;	 Catch:{ SipException -> 0x09b3 }
        r1 = r29;	 Catch:{ SipException -> 0x09b3 }
        r0.sendResponse(r1);	 Catch:{ SipException -> 0x09b3 }
    L_0x09a6:
        if (r39 == 0) goto L_0x09b2;
    L_0x09a8:
        r0 = r36;
        r1 = r39;
        r0.removeTransaction(r1);
        r39.releaseSem();
    L_0x09b2:
        return;
    L_0x09b3:
        r20 = move-exception;
        gov.nist.core.InternalErrorHandler.handleException(r20);
        goto L_0x09a6;
    L_0x09b8:
        r41 = r47.getMethod();
        r42 = "INVITE";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x039e;
    L_0x09c5:
        if (r11 != 0) goto L_0x0a37;
    L_0x09c7:
        r24 = 0;
    L_0x09c9:
        if (r11 == 0) goto L_0x0a3c;
    L_0x09cb:
        if (r39 == 0) goto L_0x0a3c;
    L_0x09cd:
        if (r24 == 0) goto L_0x0a3c;
    L_0x09cf:
        r41 = r47.getCSeq();
        r42 = r41.getSeqNumber();
        r44 = r11.getRemoteSeqNumber();
        r41 = (r42 > r44 ? 1 : (r42 == r44 ? 0 : -1));
        if (r41 <= 0) goto L_0x0a3c;
    L_0x09df:
        r0 = r24;
        r0 = r0 instanceof gov.nist.javax.sip.stack.SIPServerTransaction;
        r41 = r0;
        if (r41 == 0) goto L_0x0a3c;
    L_0x09e7:
        r41 = r34.isDialogErrorsAutomaticallyHandled();
        if (r41 == 0) goto L_0x0a3c;
    L_0x09ed:
        r41 = r11.isSequnceNumberValidation();
        if (r41 == 0) goto L_0x0a3c;
    L_0x09f3:
        r41 = r24.isInviteTransaction();
        if (r41 == 0) goto L_0x0a3c;
    L_0x09f9:
        r41 = r24.getState();
        r42 = javax.sip.TransactionState.COMPLETED;
        r0 = r41;
        r1 = r42;
        if (r0 == r1) goto L_0x0a3c;
    L_0x0a05:
        r41 = r24.getState();
        r42 = javax.sip.TransactionState.TERMINATED;
        r0 = r41;
        r1 = r42;
        if (r0 == r1) goto L_0x0a3c;
    L_0x0a11:
        r41 = r24.getState();
        r42 = javax.sip.TransactionState.CONFIRMED;
        r0 = r41;
        r1 = r42;
        if (r0 == r1) goto L_0x0a3c;
    L_0x0a1d:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0a2d;
    L_0x0a23:
        r41 = r36.getStackLogger();
        r42 = "Sending 500 response for out of sequence message";
        r41.logDebug(r42);
    L_0x0a2d:
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r0.sendServerInternalErrorResponse(r1, r2);
        return;
    L_0x0a37:
        r24 = r11.getInviteTransaction();
        goto L_0x09c9;
    L_0x0a3c:
        if (r11 != 0) goto L_0x0a9e;
    L_0x0a3e:
        r24 = 0;
    L_0x0a40:
        if (r11 == 0) goto L_0x0aa3;
    L_0x0a42:
        r41 = r34.isDialogErrorsAutomaticallyHandled();
        if (r41 == 0) goto L_0x0aa3;
    L_0x0a48:
        if (r24 == 0) goto L_0x0aa3;
    L_0x0a4a:
        r41 = r24.isInviteTransaction();
        if (r41 == 0) goto L_0x0aa3;
    L_0x0a50:
        r0 = r24;
        r0 = r0 instanceof javax.sip.ClientTransaction;
        r41 = r0;
        if (r41 == 0) goto L_0x0aa3;
    L_0x0a58:
        r41 = r24.getLastResponse();
        if (r41 == 0) goto L_0x0aa3;
    L_0x0a5e:
        r41 = r24.getLastResponse();
        r41 = r41.getStatusCode();
        r42 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;
        r0 = r41;
        r1 = r42;
        if (r0 != r1) goto L_0x0aa3;
    L_0x0a6e:
        r41 = r24.getLastResponse();
        r41 = r41.getCSeq();
        r42 = r41.getSeqNumber();
        r0 = r42;
        r41 = r11.isAckSent(r0);
        r41 = r41 ^ 1;
        if (r41 == 0) goto L_0x0aa3;
    L_0x0a84:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0a94;
    L_0x0a8a:
        r41 = r36.getStackLogger();
        r42 = "Sending 491 response for client Dialog ACK not sent.";
        r41.logDebug(r42);
    L_0x0a94:
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r0.sendRequestPendingResponse(r1, r2);
        return;
    L_0x0a9e:
        r24 = r11.getLastTransaction();
        goto L_0x0a40;
    L_0x0aa3:
        if (r11 == 0) goto L_0x039e;
    L_0x0aa5:
        if (r24 == 0) goto L_0x039e;
    L_0x0aa7:
        r41 = r34.isDialogErrorsAutomaticallyHandled();
        if (r41 == 0) goto L_0x039e;
    L_0x0aad:
        r41 = r24.isInviteTransaction();
        if (r41 == 0) goto L_0x039e;
    L_0x0ab3:
        r0 = r24;
        r0 = r0 instanceof javax.sip.ServerTransaction;
        r41 = r0;
        if (r41 == 0) goto L_0x039e;
    L_0x0abb:
        r41 = r11.isAckSeen();
        r41 = r41 ^ 1;
        if (r41 == 0) goto L_0x039e;
    L_0x0ac3:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0ad3;
    L_0x0ac9:
        r41 = r36.getStackLogger();
        r42 = "Sending 491 response for server Dialog ACK not seen.";
        r41.logDebug(r42);
    L_0x0ad3:
        r0 = r46;
        r1 = r47;
        r2 = r39;
        r0.sendRequestPendingResponse(r1, r2);
        return;
    L_0x0add:
        r41 = r11.getSipProvider();	 Catch:{ IOException -> 0x0bac }
        r0 = r34;	 Catch:{ IOException -> 0x0bac }
        r1 = r41;	 Catch:{ IOException -> 0x0bac }
        if (r0 != r1) goto L_0x0afd;	 Catch:{ IOException -> 0x0bac }
    L_0x0ae7:
        r0 = r36;	 Catch:{ IOException -> 0x0bac }
        r1 = r39;	 Catch:{ IOException -> 0x0bac }
        r0.addTransaction(r1);	 Catch:{ IOException -> 0x0bac }
        r0 = r39;	 Catch:{ IOException -> 0x0bac }
        r11.addTransaction(r0);	 Catch:{ IOException -> 0x0bac }
        r0 = r47;	 Catch:{ IOException -> 0x0bac }
        r11.addRoute(r0);	 Catch:{ IOException -> 0x0bac }
        r0 = r39;	 Catch:{ IOException -> 0x0bac }
        r0.setDialog(r11, r14);	 Catch:{ IOException -> 0x0bac }
    L_0x0afd:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0b2a;
    L_0x0b03:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = r47.getMethod();
        r42 = r42.append(r43);
        r43 = " transaction.isMapped = ";
        r42 = r42.append(r43);
        r43 = r39.isTransactionMapped();
        r42 = r42.append(r43);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x0b2a:
        if (r11 != 0) goto L_0x0d3b;
    L_0x0b2c:
        r41 = r47.getMethod();
        r42 = "NOTIFY";
        r41 = r41.equals(r42);
        if (r41 == 0) goto L_0x0d3b;
    L_0x0b39:
        r0 = r46;
        r0 = r0.listeningPoint;
        r41 = r0;
        r0 = r36;
        r1 = r47;
        r2 = r41;
        r27 = r0.findSubscribeTransaction(r1, r2);
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0b6e;
    L_0x0b4f:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "PROCESSING NOTIFY  DIALOG == null ";
        r42 = r42.append(r43);
        r0 = r42;
        r1 = r27;
        r42 = r0.append(r1);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x0b6e:
        r41 = r34.isAutomaticDialogSupportEnabled();
        if (r41 == 0) goto L_0x0bca;
    L_0x0b74:
        if (r27 != 0) goto L_0x0bca;
    L_0x0b76:
        r0 = r36;
        r0 = r0.deliverUnsolicitedNotify;
        r41 = r0;
        r41 = r41 ^ 1;
        if (r41 == 0) goto L_0x0bca;
    L_0x0b80:
        r41 = r36.isLoggingEnabled();	 Catch:{ Exception -> 0x0bb8 }
        if (r41 == 0) goto L_0x0b90;	 Catch:{ Exception -> 0x0bb8 }
    L_0x0b86:
        r41 = r36.getStackLogger();	 Catch:{ Exception -> 0x0bb8 }
        r42 = "Could not find Subscription for Notify Tx.";	 Catch:{ Exception -> 0x0bb8 }
        r41.logDebug(r42);	 Catch:{ Exception -> 0x0bb8 }
    L_0x0b90:
        r41 = 481; // 0x1e1 float:6.74E-43 double:2.376E-321;	 Catch:{ Exception -> 0x0bb8 }
        r0 = r47;	 Catch:{ Exception -> 0x0bb8 }
        r1 = r41;	 Catch:{ Exception -> 0x0bb8 }
        r16 = r0.createResponse(r1);	 Catch:{ Exception -> 0x0bb8 }
        r41 = "Subscription does not exist";	 Catch:{ Exception -> 0x0bb8 }
        r0 = r16;	 Catch:{ Exception -> 0x0bb8 }
        r1 = r41;	 Catch:{ Exception -> 0x0bb8 }
        r0.setReasonPhrase(r1);	 Catch:{ Exception -> 0x0bb8 }
        r0 = r34;	 Catch:{ Exception -> 0x0bb8 }
        r1 = r16;	 Catch:{ Exception -> 0x0bb8 }
        r0.sendResponse(r1);	 Catch:{ Exception -> 0x0bb8 }
        return;
    L_0x0bac:
        r18 = move-exception;
        r39.raiseIOExceptionEvent();
        r0 = r36;
        r1 = r39;
        r0.removeTransaction(r1);
        return;
    L_0x0bb8:
        r19 = move-exception;
        r41 = r36.getStackLogger();
        r42 = "Exception while sending error response statelessly";
        r0 = r41;
        r1 = r42;
        r2 = r19;
        r0.logError(r1, r2);
        return;
    L_0x0bca:
        if (r27 == 0) goto L_0x0d16;
    L_0x0bcc:
        r0 = r39;
        r1 = r27;
        r0.setPendingSubscribe(r1);
        r38 = r27.getDefaultDialog();
        if (r38 == 0) goto L_0x0bdf;
    L_0x0bd9:
        r41 = r38.getDialogId();
        if (r41 != 0) goto L_0x0caa;
    L_0x0bdf:
        if (r38 == 0) goto L_0x0cfc;
    L_0x0be1:
        r41 = r38.getDialogId();
        if (r41 != 0) goto L_0x0cfc;
    L_0x0be7:
        r0 = r38;
        r0.setDialogId(r14);
    L_0x0bec:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0c11;
    L_0x0bf2:
        r41 = r36.getStackLogger();
        r42 = new java.lang.StringBuilder;
        r42.<init>();
        r43 = "PROCESSING NOTIFY Subscribe DIALOG ";
        r42 = r42.append(r43);
        r0 = r42;
        r1 = r38;
        r42 = r0.append(r1);
        r42 = r42.toString();
        r41.logDebug(r42);
    L_0x0c11:
        if (r38 != 0) goto L_0x0c42;
    L_0x0c13:
        r41 = r34.isAutomaticDialogSupportEnabled();
        if (r41 != 0) goto L_0x0c1f;
    L_0x0c19:
        r41 = r27.getDefaultDialog();
        if (r41 == 0) goto L_0x0c42;
    L_0x0c1f:
        r41 = "Event";
        r0 = r47;
        r1 = r41;
        r17 = r0.getHeader(r1);
        r17 = (gov.nist.javax.sip.header.Event) r17;
        r41 = r17.getEventType();
        r0 = r36;
        r1 = r41;
        r41 = r0.isEventForked(r1);
        if (r41 == 0) goto L_0x0c42;
    L_0x0c3a:
        r0 = r27;
        r1 = r39;
        r38 = gov.nist.javax.sip.stack.SIPDialog.createFromNOTIFY(r0, r1);
    L_0x0c42:
        if (r38 == 0) goto L_0x0c89;
    L_0x0c44:
        r0 = r39;
        r1 = r38;
        r0.setDialog(r1, r14);
        r41 = javax.sip.DialogState.CONFIRMED;
        r41 = r41.getValue();
        r0 = r38;
        r1 = r41;
        r0.setState(r1);
        r0 = r36;
        r1 = r38;
        r0.putDialog(r1);
        r0 = r27;
        r1 = r38;
        r0.setDialog(r1, r14);
        r41 = r39.isTransactionMapped();
        if (r41 != 0) goto L_0x0c89;
    L_0x0c6c:
        r0 = r46;
        r0 = r0.sipStack;
        r41 = r0;
        r0 = r41;
        r1 = r39;
        r0.mapTransaction(r1);
        r39.setPassToListener();
        r0 = r46;	 Catch:{ Exception -> 0x0d65 }
        r0 = r0.sipStack;	 Catch:{ Exception -> 0x0d65 }
        r41 = r0;	 Catch:{ Exception -> 0x0d65 }
        r0 = r41;	 Catch:{ Exception -> 0x0d65 }
        r1 = r39;	 Catch:{ Exception -> 0x0d65 }
        r0.addTransaction(r1);	 Catch:{ Exception -> 0x0d65 }
    L_0x0c89:
        if (r39 == 0) goto L_0x0d04;
    L_0x0c8b:
        r41 = r39.isTransactionMapped();
        if (r41 == 0) goto L_0x0d04;
    L_0x0c91:
        r32 = new javax.sip.RequestEvent;
        r0 = r32;
        r1 = r34;
        r2 = r39;
        r3 = r38;
        r4 = r47;
        r0.<init>(r1, r2, r3, r4);
    L_0x0ca0:
        r0 = r34;
        r1 = r32;
        r2 = r39;
        r0.handleEvent(r1, r2);
        return;
    L_0x0caa:
        r41 = r38.getDialogId();
        r0 = r41;
        r41 = r0.equals(r14);
        r41 = r41 ^ 1;
        if (r41 != 0) goto L_0x0bdf;
    L_0x0cb8:
        r0 = r39;
        r1 = r38;
        r0.setDialog(r1, r14);
        r11 = r38;
        r41 = r39.isTransactionMapped();
        if (r41 != 0) goto L_0x0ce4;
    L_0x0cc7:
        r0 = r46;
        r0 = r0.sipStack;
        r41 = r0;
        r0 = r41;
        r1 = r39;
        r0.mapTransaction(r1);
        r39.setPassToListener();
        r0 = r46;	 Catch:{ Exception -> 0x0d63 }
        r0 = r0.sipStack;	 Catch:{ Exception -> 0x0d63 }
        r41 = r0;	 Catch:{ Exception -> 0x0d63 }
        r0 = r41;	 Catch:{ Exception -> 0x0d63 }
        r1 = r39;	 Catch:{ Exception -> 0x0d63 }
        r0.addTransaction(r1);	 Catch:{ Exception -> 0x0d63 }
    L_0x0ce4:
        r0 = r36;
        r1 = r38;
        r0.putDialog(r1);
        if (r27 == 0) goto L_0x0c89;
    L_0x0ced:
        r0 = r38;
        r1 = r27;
        r0.addTransaction(r1);
        r0 = r27;
        r1 = r38;
        r0.setDialog(r1, r14);
        goto L_0x0c89;
    L_0x0cfc:
        r0 = r27;
        r38 = r0.getDialog(r14);
        goto L_0x0bec;
    L_0x0d04:
        r32 = new javax.sip.RequestEvent;
        r41 = 0;
        r0 = r32;
        r1 = r34;
        r2 = r41;
        r3 = r38;
        r4 = r47;
        r0.<init>(r1, r2, r3, r4);
        goto L_0x0ca0;
    L_0x0d16:
        r41 = r36.isLoggingEnabled();
        if (r41 == 0) goto L_0x0d26;
    L_0x0d1c:
        r41 = r36.getStackLogger();
        r42 = "could not find subscribe tx";
        r41.logDebug(r42);
    L_0x0d26:
        r32 = new javax.sip.RequestEvent;
        r41 = 0;
        r42 = 0;
        r0 = r32;
        r1 = r34;
        r2 = r41;
        r3 = r42;
        r4 = r47;
        r0.<init>(r1, r2, r3, r4);
        goto L_0x0ca0;
    L_0x0d3b:
        if (r39 == 0) goto L_0x0d52;
    L_0x0d3d:
        r41 = r39.isTransactionMapped();
        if (r41 == 0) goto L_0x0d52;
    L_0x0d43:
        r32 = new javax.sip.RequestEvent;
        r0 = r32;
        r1 = r34;
        r2 = r39;
        r3 = r47;
        r0.<init>(r1, r2, r11, r3);
        goto L_0x0ca0;
    L_0x0d52:
        r32 = new javax.sip.RequestEvent;
        r41 = 0;
        r0 = r32;
        r1 = r34;
        r2 = r41;
        r3 = r47;
        r0.<init>(r1, r2, r11, r3);
        goto L_0x0ca0;
    L_0x0d63:
        r19 = move-exception;
        goto L_0x0ce4;
    L_0x0d65:
        r19 = move-exception;
        goto L_0x0c89;
        */
        throw new UnsupportedOperationException("Method not decompiled: gov.nist.javax.sip.DialogFilter.processRequest(gov.nist.javax.sip.message.SIPRequest, gov.nist.javax.sip.stack.MessageChannel):void");
    }

    public void processResponse(SIPResponse response, MessageChannel incomingMessageChannel, SIPDialog dialog) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("PROCESSING INCOMING RESPONSE" + response.encodeMessage());
        }
        if (this.listeningPoint == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping message: No listening point registered!");
            }
        } else if (!this.sipStack.checkBranchId() || (Utils.getInstance().responseBelongsToUs(response) ^ 1) == 0) {
            SipProviderImpl sipProvider = this.listeningPoint.getProvider();
            if (sipProvider == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("Dropping message:  no provider");
                }
            } else if (sipProvider.getSipListener() == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("No listener -- dropping response!");
                }
            } else {
                SIPClientTransaction transaction = this.transactionChannel;
                SipStackImpl sipStackImpl = sipProvider.sipStack;
                if (this.sipStack.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Transaction = " + transaction);
                }
                if (transaction == null) {
                    if (dialog != null) {
                        if (response.getStatusCode() / 100 != 2) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Response is not a final response and dialog is found for response -- dropping response!");
                            }
                            return;
                        } else if (dialog.getState() == DialogState.TERMINATED) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Dialog is terminated -- dropping response!");
                            }
                            return;
                        } else {
                            boolean ackAlreadySent = false;
                            if (dialog.isAckSeen() && dialog.getLastAckSent() != null && dialog.getLastAckSent().getCSeq().getSeqNumber() == response.getCSeq().getSeqNumber()) {
                                ackAlreadySent = true;
                            }
                            if (ackAlreadySent && response.getCSeq().getMethod().equals(dialog.getMethod())) {
                                try {
                                    if (this.sipStack.isLoggingEnabled()) {
                                        this.sipStack.getStackLogger().logDebug("Retransmission of OK detected: Resending last ACK");
                                    }
                                    dialog.resendAck();
                                    return;
                                } catch (SipException ex) {
                                    this.sipStack.getStackLogger().logError("could not resend ack", ex);
                                }
                            }
                        }
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("could not find tx, handling statelessly Dialog =  " + dialog);
                    }
                    ResponseEventExt sipEvent = new ResponseEventExt(sipProvider, transaction, dialog, response);
                    if (response.getCSeqHeader().getMethod().equals("INVITE")) {
                        sipEvent.setOriginalTransaction(this.sipStack.getForkedTransaction(response.getTransactionId()));
                    }
                    sipProvider.handleEvent(sipEvent, transaction);
                    return;
                }
                ResponseEventExt responseEvent = new ResponseEventExt(sipProvider, transaction, dialog, response);
                if (response.getCSeqHeader().getMethod().equals("INVITE")) {
                    responseEvent.setOriginalTransaction(this.sipStack.getForkedTransaction(response.getTransactionId()));
                }
                if (!(dialog == null || response.getStatusCode() == 100)) {
                    dialog.setLastResponse(transaction, response);
                    transaction.setDialog(dialog, dialog.getDialogId());
                }
                sipProvider.handleEvent(responseEvent, transaction);
            }
        } else {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping response - topmost VIA header does not originate from this stack");
            }
        }
    }

    public String getProcessingInfo() {
        return null;
    }

    public void processResponse(SIPResponse sipResponse, MessageChannel incomingChannel) {
        String dialogID = sipResponse.getDialogId(false);
        Dialog sipDialog = this.sipStack.getDialog(dialogID);
        String method = sipResponse.getCSeq().getMethod();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("PROCESSING INCOMING RESPONSE: " + sipResponse.encodeMessage());
        }
        if (this.sipStack.checkBranchId() && (Utils.getInstance().responseBelongsToUs(sipResponse) ^ 1) != 0) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Detected stray response -- dropping");
            }
        } else if (this.listeningPoint == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping message: No listening point registered!");
            }
        } else {
            SipProviderImpl sipProvider = this.listeningPoint.getProvider();
            if (sipProvider == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dropping message:  no provider");
                }
            } else if (sipProvider.getSipListener() == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dropping message:  no sipListener registered!");
                }
            } else {
                SIPClientTransaction transaction = this.transactionChannel;
                if (sipDialog == null && transaction != null) {
                    sipDialog = transaction.getDialog(dialogID);
                    if (sipDialog != null && sipDialog.getState() == DialogState.TERMINATED) {
                        sipDialog = null;
                    }
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Transaction = " + transaction + " sipDialog = " + sipDialog);
                }
                if (this.transactionChannel != null) {
                    String originalFrom = ((SIPRequest) this.transactionChannel.getRequest()).getFromTag();
                    if (((originalFrom == null ? 1 : 0) ^ (sipResponse.getFrom().getTag() == null ? 1 : 0)) != 0) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                        }
                        return;
                    } else if (!(originalFrom == null || (originalFrom.equalsIgnoreCase(sipResponse.getFrom().getTag()) ^ 1) == 0)) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                        }
                        return;
                    }
                }
                SipStackImpl sipStackImpl = this.sipStack;
                if (!SIPTransactionStack.isDialogCreated(method) || sipResponse.getStatusCode() == 100 || sipResponse.getFrom().getTag() == null || sipResponse.getTo().getTag() == null || sipDialog != null) {
                    if (!(sipDialog == null || transaction != null || sipDialog.getState() == DialogState.TERMINATED)) {
                        if (sipResponse.getStatusCode() / 100 != 2) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("status code != 200 ; statusCode = " + sipResponse.getStatusCode());
                            }
                        } else if (sipDialog.getState() == DialogState.TERMINATED) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Dialog is terminated -- dropping response!");
                            }
                            if (sipResponse.getStatusCode() / 100 == 2 && sipResponse.getCSeq().getMethod().equals("INVITE")) {
                                try {
                                    sipDialog.sendAck(sipDialog.createAck(sipResponse.getCSeq().getSeqNumber()));
                                } catch (Exception ex) {
                                    this.sipStack.getStackLogger().logError("Error creating ack", ex);
                                }
                            }
                            return;
                        } else {
                            boolean ackAlreadySent = false;
                            if (sipDialog.isAckSeen() && sipDialog.getLastAckSent() != null && sipDialog.getLastAckSent().getCSeq().getSeqNumber() == sipResponse.getCSeq().getSeqNumber() && sipResponse.getDialogId(false).equals(sipDialog.getLastAckSent().getDialogId(false))) {
                                ackAlreadySent = true;
                            }
                            if (ackAlreadySent && sipResponse.getCSeq().getMethod().equals(sipDialog.getMethod())) {
                                try {
                                    if (this.sipStack.isLoggingEnabled()) {
                                        this.sipStack.getStackLogger().logDebug("resending ACK");
                                    }
                                    sipDialog.resendAck();
                                    return;
                                } catch (SipException e) {
                                }
                            }
                        }
                    }
                } else if (sipProvider.isAutomaticDialogSupportEnabled()) {
                    if (this.transactionChannel == null) {
                        sipDialog = this.sipStack.createDialog(sipProvider, sipResponse);
                    } else if (sipDialog == null) {
                        sipDialog = this.sipStack.createDialog((SIPClientTransaction) this.transactionChannel, sipResponse);
                        this.transactionChannel.setDialog(sipDialog, sipResponse.getDialogId(false));
                    }
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("sending response to TU for processing ");
                }
                if (!(sipDialog == null || sipResponse.getStatusCode() == 100 || sipResponse.getTo().getTag() == null)) {
                    sipDialog.setLastResponse(transaction, sipResponse);
                }
                ResponseEventExt responseEvent = new ResponseEventExt(sipProvider, transaction, sipDialog, sipResponse);
                if (sipResponse.getCSeq().getMethod().equals("INVITE")) {
                    responseEvent.setOriginalTransaction(this.sipStack.getForkedTransaction(sipResponse.getTransactionId()));
                }
                sipProvider.handleEvent(responseEvent, transaction);
            }
        }
    }
}
