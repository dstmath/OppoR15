package com.google.android.mms.pdu;

import android.hardware.radio.V1_0.RadioError;
import android.util.Log;
import com.android.internal.telephony.uicc.SpnOverride;
import com.google.android.mms.ContentType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

public class PduParser {
    static final /* synthetic */ boolean -assertionsDisabled = (PduParser.class.desiredAssertionStatus() ^ 1);
    private static final boolean DEBUG = false;
    private static final int END_STRING_FLAG = 0;
    private static final int LENGTH_QUOTE = 31;
    private static final boolean LOCAL_LOGV = false;
    private static final String LOG_TAG = "PduParser";
    private static final int LONG_INTEGER_LENGTH_MAX = 8;
    private static final int QUOTE = 127;
    private static final int QUOTED_STRING_FLAG = 34;
    private static final int SHORT_INTEGER_MAX = 127;
    private static final int SHORT_LENGTH_MAX = 30;
    private static final int TEXT_MAX = 127;
    private static final int TEXT_MIN = 32;
    private static final int THE_FIRST_PART = 0;
    private static final int THE_LAST_PART = 1;
    private static final int TYPE_QUOTED_STRING = 1;
    private static final int TYPE_TEXT_STRING = 0;
    private static final int TYPE_TOKEN_STRING = 2;
    private static byte[] mStartParam = null;
    private static byte[] mTypeParam = null;
    private PduBody mBody = null;
    private PduHeaders mHeaders = null;
    private final boolean mParseContentDisposition;
    private ByteArrayInputStream mPduDataStream = null;

    public PduParser(byte[] pduDataStream, boolean parseContentDisposition) {
        this.mPduDataStream = new ByteArrayInputStream(pduDataStream);
        this.mParseContentDisposition = parseContentDisposition;
    }

    public GenericPdu parse() {
        if (this.mPduDataStream == null) {
            return null;
        }
        this.mHeaders = parseHeaders(this.mPduDataStream);
        if (this.mHeaders == null) {
            return null;
        }
        int messageType = this.mHeaders.getOctet(140);
        if (checkMandatoryHeader(this.mHeaders)) {
            if (128 == messageType || 132 == messageType) {
                this.mBody = parseParts(this.mPduDataStream);
                if (this.mBody == null) {
                    return null;
                }
            }
            switch (messageType) {
                case 128:
                    return new SendReq(this.mHeaders, this.mBody);
                case 129:
                    return new SendConf(this.mHeaders);
                case 130:
                    return new NotificationInd(this.mHeaders);
                case 131:
                    return new NotifyRespInd(this.mHeaders);
                case 132:
                    RetrieveConf retrieveConf = new RetrieveConf(this.mHeaders, this.mBody);
                    byte[] contentType = retrieveConf.getContentType();
                    if (contentType == null) {
                        return null;
                    }
                    String ctTypeStr = new String(contentType);
                    if (ctTypeStr.equals(ContentType.MULTIPART_MIXED) || ctTypeStr.equals(ContentType.MULTIPART_RELATED) || ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                        return retrieveConf;
                    }
                    if (!ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                        return null;
                    }
                    PduPart firstPart = this.mBody.getPart(0);
                    this.mBody.removeAll();
                    this.mBody.addPart(0, firstPart);
                    return retrieveConf;
                case 133:
                    return new AcknowledgeInd(this.mHeaders);
                case 134:
                    return new DeliveryInd(this.mHeaders);
                case 135:
                    return new ReadRecInd(this.mHeaders);
                case 136:
                    return new ReadOrigInd(this.mHeaders);
                default:
                    log("Parser doesn't support this message type in this version!");
                    return null;
            }
        }
        log("check mandatory headers failed!");
        return null;
    }

    protected com.google.android.mms.pdu.PduHeaders parseHeaders(java.io.ByteArrayInputStream r39) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r23_0 'str' java.lang.String) in PHI: PHI: (r23_2 'str' java.lang.String) = (r23_0 'str' java.lang.String), (r23_1 'str' java.lang.String) binds: {(r23_0 'str' java.lang.String)=B:74:0x01cb, (r23_1 'str' java.lang.String)=B:75:0x01cd}
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
        r38 = this;
        if (r39 != 0) goto L_0x0005;
    L_0x0002:
        r33 = 0;
        return r33;
    L_0x0005:
        r15 = 1;
        r14 = new com.google.android.mms.pdu.PduHeaders;
        r14.<init>();
    L_0x000b:
        if (r15 == 0) goto L_0x05a1;
    L_0x000d:
        r33 = r39.available();
        if (r33 <= 0) goto L_0x05a1;
    L_0x0013:
        r33 = 1;
        r0 = r39;
        r1 = r33;
        r0.mark(r1);
        r13 = extractByteValue(r39);
        r33 = 32;
        r0 = r33;
        if (r13 < r0) goto L_0x003a;
    L_0x0026:
        r33 = 127; // 0x7f float:1.78E-43 double:6.27E-322;
        r0 = r33;
        if (r13 > r0) goto L_0x003a;
    L_0x002c:
        r39.reset();
        r33 = 0;
        r0 = r39;
        r1 = r33;
        r5 = parseWapString(r0, r1);
        goto L_0x000b;
    L_0x003a:
        switch(r13) {
            case 129: goto L_0x01ad;
            case 130: goto L_0x01ad;
            case 131: goto L_0x0141;
            case 132: goto L_0x053f;
            case 133: goto L_0x00f1;
            case 134: goto L_0x009d;
            case 135: goto L_0x0219;
            case 136: goto L_0x0219;
            case 137: goto L_0x0277;
            case 138: goto L_0x031d;
            case 139: goto L_0x0141;
            case 140: goto L_0x0044;
            case 141: goto L_0x0400;
            case 142: goto L_0x00f1;
            case 143: goto L_0x009d;
            case 144: goto L_0x009d;
            case 145: goto L_0x009d;
            case 146: goto L_0x009d;
            case 147: goto L_0x017a;
            case 148: goto L_0x009d;
            case 149: goto L_0x009d;
            case 150: goto L_0x017a;
            case 151: goto L_0x01ad;
            case 152: goto L_0x0141;
            case 153: goto L_0x009d;
            case 154: goto L_0x017a;
            case 155: goto L_0x009d;
            case 156: goto L_0x009d;
            case 157: goto L_0x0219;
            case 158: goto L_0x0141;
            case 159: goto L_0x00f1;
            case 160: goto L_0x0458;
            case 161: goto L_0x04b2;
            case 162: goto L_0x009d;
            case 163: goto L_0x009d;
            case 164: goto L_0x0501;
            case 165: goto L_0x009d;
            case 166: goto L_0x017a;
            case 167: goto L_0x009d;
            case 168: goto L_0x003d;
            case 169: goto L_0x009d;
            case 170: goto L_0x050c;
            case 171: goto L_0x009d;
            case 172: goto L_0x050c;
            case 173: goto L_0x0119;
            case 174: goto L_0x003d;
            case 175: goto L_0x0119;
            case 176: goto L_0x003d;
            case 177: goto L_0x009d;
            case 178: goto L_0x0534;
            case 179: goto L_0x0119;
            case 180: goto L_0x009d;
            case 181: goto L_0x017a;
            case 182: goto L_0x017a;
            case 183: goto L_0x0141;
            case 184: goto L_0x0141;
            case 185: goto L_0x0141;
            case 186: goto L_0x009d;
            case 187: goto L_0x009d;
            case 188: goto L_0x009d;
            case 189: goto L_0x0141;
            case 190: goto L_0x0141;
            case 191: goto L_0x009d;
            default: goto L_0x003d;
        };
    L_0x003d:
        r33 = "Unknown header";
        log(r33);
        goto L_0x000b;
    L_0x0044:
        r19 = extractByteValue(r39);
        switch(r19) {
            case 137: goto L_0x007d;
            case 138: goto L_0x007d;
            case 139: goto L_0x007d;
            case 140: goto L_0x007d;
            case 141: goto L_0x007d;
            case 142: goto L_0x007d;
            case 143: goto L_0x007d;
            case 144: goto L_0x007d;
            case 145: goto L_0x007d;
            case 146: goto L_0x007d;
            case 147: goto L_0x007d;
            case 148: goto L_0x007d;
            case 149: goto L_0x007d;
            case 150: goto L_0x007d;
            case 151: goto L_0x007d;
            default: goto L_0x004b;
        };
    L_0x004b:
        r0 = r19;	 Catch:{ InvalidHeaderValueException -> 0x0051, RuntimeException -> 0x0080 }
        r14.setOctet(r0, r13);	 Catch:{ InvalidHeaderValueException -> 0x0051, RuntimeException -> 0x0080 }
        goto L_0x000b;
    L_0x0051:
        r7 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r34 = "Set invalid Octet value: ";
        r33 = r33.append(r34);
        r0 = r33;
        r1 = r19;
        r33 = r0.append(r1);
        r34 = " into the header filed: ";
        r33 = r33.append(r34);
        r0 = r33;
        r33 = r0.append(r13);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x007d:
        r33 = 0;
        return r33;
    L_0x0080:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Octet header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x009d:
        r27 = extractByteValue(r39);
        r0 = r27;	 Catch:{ InvalidHeaderValueException -> 0x00a8, RuntimeException -> 0x00d4 }
        r14.setOctet(r0, r13);	 Catch:{ InvalidHeaderValueException -> 0x00a8, RuntimeException -> 0x00d4 }
        goto L_0x000b;
    L_0x00a8:
        r7 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r34 = "Set invalid Octet value: ";
        r33 = r33.append(r34);
        r0 = r33;
        r1 = r27;
        r33 = r0.append(r1);
        r34 = " into the header filed: ";
        r33 = r33.append(r34);
        r0 = r33;
        r33 = r0.append(r13);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x00d4:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Octet header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x00f1:
        r28 = parseLongInteger(r39);	 Catch:{ RuntimeException -> 0x00fc }
        r0 = r28;	 Catch:{ RuntimeException -> 0x00fc }
        r14.setLongInteger(r0, r13);	 Catch:{ RuntimeException -> 0x00fc }
        goto L_0x000b;
    L_0x00fc:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Long-Integer header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0119:
        r28 = parseIntegerValue(r39);	 Catch:{ RuntimeException -> 0x0124 }
        r0 = r28;	 Catch:{ RuntimeException -> 0x0124 }
        r14.setLongInteger(r0, r13);	 Catch:{ RuntimeException -> 0x0124 }
        goto L_0x000b;
    L_0x0124:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Long-Integer header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0141:
        r33 = 0;
        r0 = r39;
        r1 = r33;
        r31 = parseWapString(r0, r1);
        if (r31 == 0) goto L_0x000b;
    L_0x014d:
        r0 = r31;	 Catch:{ NullPointerException -> 0x0154, RuntimeException -> 0x015d }
        r14.setTextString(r0, r13);	 Catch:{ NullPointerException -> 0x0154, RuntimeException -> 0x015d }
        goto L_0x000b;
    L_0x0154:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        goto L_0x000b;
    L_0x015d:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Text-String header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x017a:
        r30 = parseEncodedStringValue(r39);
        if (r30 == 0) goto L_0x000b;
    L_0x0180:
        r0 = r30;	 Catch:{ NullPointerException -> 0x0187, RuntimeException -> 0x0190 }
        r14.setEncodedStringValue(r0, r13);	 Catch:{ NullPointerException -> 0x0187, RuntimeException -> 0x0190 }
        goto L_0x000b;
    L_0x0187:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        goto L_0x000b;
    L_0x0190:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Encoded-String-Value header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x01ad:
        r30 = parseEncodedStringValue(r39);
        if (r30 == 0) goto L_0x000b;
    L_0x01b3:
        r4 = r30.getTextString();
        if (r4 == 0) goto L_0x01e2;
    L_0x01b9:
        r23 = new java.lang.String;
        r0 = r23;
        r0.<init>(r4);
        r33 = "/";
        r0 = r23;
        r1 = r33;
        r10 = r0.indexOf(r1);
        if (r10 <= 0) goto L_0x01d7;
    L_0x01cd:
        r33 = 0;
        r0 = r23;
        r1 = r33;
        r23 = r0.substring(r1, r10);
    L_0x01d7:
        r33 = r23.getBytes();	 Catch:{ NullPointerException -> 0x01f2 }
        r0 = r30;	 Catch:{ NullPointerException -> 0x01f2 }
        r1 = r33;	 Catch:{ NullPointerException -> 0x01f2 }
        r0.setTextString(r1);	 Catch:{ NullPointerException -> 0x01f2 }
    L_0x01e2:
        r0 = r30;	 Catch:{ NullPointerException -> 0x01e9, RuntimeException -> 0x01fc }
        r14.appendEncodedStringValue(r0, r13);	 Catch:{ NullPointerException -> 0x01e9, RuntimeException -> 0x01fc }
        goto L_0x000b;
    L_0x01e9:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        goto L_0x000b;
    L_0x01f2:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        r33 = 0;
        return r33;
    L_0x01fc:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Encoded-String-Value header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0219:
        parseValueLength(r39);
        r26 = extractByteValue(r39);
        r24 = parseLongInteger(r39);	 Catch:{ RuntimeException -> 0x025a }
        r33 = 129; // 0x81 float:1.81E-43 double:6.37E-322;
        r0 = r33;
        r1 = r26;
        if (r0 != r1) goto L_0x0236;
    L_0x022c:
        r34 = java.lang.System.currentTimeMillis();
        r36 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r34 = r34 / r36;
        r24 = r24 + r34;
    L_0x0236:
        r0 = r24;	 Catch:{ RuntimeException -> 0x023d }
        r14.setLongInteger(r0, r13);	 Catch:{ RuntimeException -> 0x023d }
        goto L_0x000b;
    L_0x023d:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Long-Integer header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x025a:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Long-Integer header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0277:
        r11 = 0;
        parseValueLength(r39);
        r12 = extractByteValue(r39);
        r33 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r0 = r33;
        if (r0 != r12) goto L_0x02d4;
    L_0x0285:
        r11 = parseEncodedStringValue(r39);
        if (r11 == 0) goto L_0x02b8;
    L_0x028b:
        r4 = r11.getTextString();
        if (r4 == 0) goto L_0x02b8;
    L_0x0291:
        r23 = new java.lang.String;
        r0 = r23;
        r0.<init>(r4);
        r33 = "/";
        r0 = r23;
        r1 = r33;
        r10 = r0.indexOf(r1);
        if (r10 <= 0) goto L_0x02af;
    L_0x02a5:
        r33 = 0;
        r0 = r23;
        r1 = r33;
        r23 = r0.substring(r1, r10);
    L_0x02af:
        r33 = r23.getBytes();	 Catch:{ NullPointerException -> 0x02ca }
        r0 = r33;	 Catch:{ NullPointerException -> 0x02ca }
        r11.setTextString(r0);	 Catch:{ NullPointerException -> 0x02ca }
    L_0x02b8:
        r33 = 137; // 0x89 float:1.92E-43 double:6.77E-322;
        r0 = r33;	 Catch:{ NullPointerException -> 0x02c1, RuntimeException -> 0x0300 }
        r14.setEncodedStringValue(r11, r0);	 Catch:{ NullPointerException -> 0x02c1, RuntimeException -> 0x0300 }
        goto L_0x000b;
    L_0x02c1:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        goto L_0x000b;
    L_0x02ca:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        r33 = 0;
        return r33;
    L_0x02d4:
        r11 = new com.google.android.mms.pdu.EncodedStringValue;	 Catch:{ NullPointerException -> 0x02e3 }
        r33 = "insert-address-token";	 Catch:{ NullPointerException -> 0x02e3 }
        r33 = r33.getBytes();	 Catch:{ NullPointerException -> 0x02e3 }
        r0 = r33;	 Catch:{ NullPointerException -> 0x02e3 }
        r11.<init>(r0);	 Catch:{ NullPointerException -> 0x02e3 }
        goto L_0x02b8;
    L_0x02e3:
        r8 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Encoded-String-Value header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0300:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Encoded-String-Value header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x031d:
        r33 = 1;
        r0 = r39;
        r1 = r33;
        r0.mark(r1);
        r17 = extractByteValue(r39);
        r33 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r0 = r17;
        r1 = r33;
        if (r0 < r1) goto L_0x03c0;
    L_0x0332:
        r33 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r0 = r33;
        r1 = r17;
        if (r0 != r1) goto L_0x0355;
    L_0x033a:
        r33 = "personal";	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r33 = r33.getBytes();	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r34 = 138; // 0x8a float:1.93E-43 double:6.8E-322;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r0 = r33;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r1 = r34;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r14.setTextString(r0, r1);	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        goto L_0x000b;
    L_0x034c:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        goto L_0x000b;
    L_0x0355:
        r33 = 129; // 0x81 float:1.81E-43 double:6.37E-322;
        r0 = r33;
        r1 = r17;
        if (r0 != r1) goto L_0x038c;
    L_0x035d:
        r33 = "advertisement";	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r33 = r33.getBytes();	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r34 = 138; // 0x8a float:1.93E-43 double:6.8E-322;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r0 = r33;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r1 = r34;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r14.setTextString(r0, r1);	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        goto L_0x000b;
    L_0x036f:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Text-String header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x038c:
        r33 = 130; // 0x82 float:1.82E-43 double:6.4E-322;
        r0 = r33;
        r1 = r17;
        if (r0 != r1) goto L_0x03a6;
    L_0x0394:
        r33 = "informational";	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r33 = r33.getBytes();	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r34 = 138; // 0x8a float:1.93E-43 double:6.8E-322;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r0 = r33;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r1 = r34;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r14.setTextString(r0, r1);	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        goto L_0x000b;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
    L_0x03a6:
        r33 = 131; // 0x83 float:1.84E-43 double:6.47E-322;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r0 = r33;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r1 = r17;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        if (r0 != r1) goto L_0x000b;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
    L_0x03ae:
        r33 = "auto";	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r33 = r33.getBytes();	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r34 = 138; // 0x8a float:1.93E-43 double:6.8E-322;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r0 = r33;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r1 = r34;	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        r14.setTextString(r0, r1);	 Catch:{ NullPointerException -> 0x034c, RuntimeException -> 0x036f }
        goto L_0x000b;
    L_0x03c0:
        r39.reset();
        r33 = 0;
        r0 = r39;
        r1 = r33;
        r18 = parseWapString(r0, r1);
        if (r18 == 0) goto L_0x000b;
    L_0x03cf:
        r33 = 138; // 0x8a float:1.93E-43 double:6.8E-322;
        r0 = r18;	 Catch:{ NullPointerException -> 0x03da, RuntimeException -> 0x03e3 }
        r1 = r33;	 Catch:{ NullPointerException -> 0x03da, RuntimeException -> 0x03e3 }
        r14.setTextString(r0, r1);	 Catch:{ NullPointerException -> 0x03da, RuntimeException -> 0x03e3 }
        goto L_0x000b;
    L_0x03da:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        goto L_0x000b;
    L_0x03e3:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Text-String header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0400:
        r32 = parseShortInteger(r39);
        r33 = 141; // 0x8d float:1.98E-43 double:6.97E-322;
        r0 = r32;	 Catch:{ InvalidHeaderValueException -> 0x040f, RuntimeException -> 0x043b }
        r1 = r33;	 Catch:{ InvalidHeaderValueException -> 0x040f, RuntimeException -> 0x043b }
        r14.setOctet(r0, r1);	 Catch:{ InvalidHeaderValueException -> 0x040f, RuntimeException -> 0x043b }
        goto L_0x000b;
    L_0x040f:
        r7 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r34 = "Set invalid Octet value: ";
        r33 = r33.append(r34);
        r0 = r33;
        r1 = r32;
        r33 = r0.append(r1);
        r34 = " into the header filed: ";
        r33 = r33.append(r34);
        r0 = r33;
        r33 = r0.append(r13);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x043b:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Octet header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0458:
        parseValueLength(r39);
        parseIntegerValue(r39);	 Catch:{ RuntimeException -> 0x0478 }
        r22 = parseEncodedStringValue(r39);
        if (r22 == 0) goto L_0x000b;
    L_0x0464:
        r33 = 160; // 0xa0 float:2.24E-43 double:7.9E-322;
        r0 = r22;	 Catch:{ NullPointerException -> 0x046f, RuntimeException -> 0x0495 }
        r1 = r33;	 Catch:{ NullPointerException -> 0x046f, RuntimeException -> 0x0495 }
        r14.setEncodedStringValue(r0, r1);	 Catch:{ NullPointerException -> 0x046f, RuntimeException -> 0x0495 }
        goto L_0x000b;
    L_0x046f:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        goto L_0x000b;
    L_0x0478:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = " is not Integer-Value";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0495:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Encoded-String-Value header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x04b2:
        parseValueLength(r39);
        parseIntegerValue(r39);	 Catch:{ RuntimeException -> 0x04e4 }
        r20 = parseLongInteger(r39);	 Catch:{ RuntimeException -> 0x04c7 }
        r33 = 161; // 0xa1 float:2.26E-43 double:7.95E-322;	 Catch:{ RuntimeException -> 0x04c7 }
        r0 = r20;	 Catch:{ RuntimeException -> 0x04c7 }
        r2 = r33;	 Catch:{ RuntimeException -> 0x04c7 }
        r14.setLongInteger(r0, r2);	 Catch:{ RuntimeException -> 0x04c7 }
        goto L_0x000b;
    L_0x04c7:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Long-Integer header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x04e4:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = " is not Integer-Value";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0501:
        parseValueLength(r39);
        extractByteValue(r39);
        parseEncodedStringValue(r39);
        goto L_0x000b;
    L_0x050c:
        parseValueLength(r39);
        extractByteValue(r39);
        parseIntegerValue(r39);	 Catch:{ RuntimeException -> 0x0517 }
        goto L_0x000b;
    L_0x0517:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = " is not Integer-Value";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0534:
        r33 = 0;
        r0 = r39;
        r1 = r33;
        parseContentType(r0, r1);
        goto L_0x000b;
    L_0x053f:
        r16 = new java.util.HashMap;
        r16.<init>();
        r0 = r39;
        r1 = r16;
        r6 = parseContentType(r0, r1);
        if (r6 == 0) goto L_0x0555;
    L_0x054e:
        r33 = 132; // 0x84 float:1.85E-43 double:6.5E-322;
        r0 = r33;	 Catch:{ NullPointerException -> 0x0599, RuntimeException -> 0x057c }
        r14.setTextString(r6, r0);	 Catch:{ NullPointerException -> 0x0599, RuntimeException -> 0x057c }
    L_0x0555:
        r33 = 153; // 0x99 float:2.14E-43 double:7.56E-322;
        r33 = java.lang.Integer.valueOf(r33);
        r0 = r16;
        r1 = r33;
        r33 = r0.get(r1);
        r33 = (byte[]) r33;
        mStartParam = r33;
        r33 = 131; // 0x83 float:1.84E-43 double:6.47E-322;
        r33 = java.lang.Integer.valueOf(r33);
        r0 = r16;
        r1 = r33;
        r33 = r0.get(r1);
        r33 = (byte[]) r33;
        mTypeParam = r33;
        r15 = 0;
        goto L_0x000b;
    L_0x057c:
        r9 = move-exception;
        r33 = new java.lang.StringBuilder;
        r33.<init>();
        r0 = r33;
        r33 = r0.append(r13);
        r34 = "is not Text-String header field!";
        r33 = r33.append(r34);
        r33 = r33.toString();
        log(r33);
        r33 = 0;
        return r33;
    L_0x0599:
        r8 = move-exception;
        r33 = "null pointer error!";
        log(r33);
        goto L_0x0555;
    L_0x05a1:
        return r14;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.mms.pdu.PduParser.parseHeaders(java.io.ByteArrayInputStream):com.google.android.mms.pdu.PduHeaders");
    }

    protected PduBody parseParts(ByteArrayInputStream pduDataStream) {
        if (pduDataStream == null) {
            return null;
        }
        int count = parseUnsignedInt(pduDataStream);
        PduBody body = new PduBody();
        for (int i = 0; i < count; i++) {
            int headerLength = parseUnsignedInt(pduDataStream);
            int dataLength = parseUnsignedInt(pduDataStream);
            PduPart part = new PduPart();
            int startPos = pduDataStream.available();
            if (startPos <= 0) {
                return null;
            }
            HashMap<Integer, Object> map = new HashMap();
            byte[] contentType = parseContentType(pduDataStream, map);
            if (contentType != null) {
                part.setContentType(contentType);
            } else {
                part.setContentType(PduContentTypes.contentTypes[0].getBytes());
            }
            byte[] name = (byte[]) map.get(Integer.valueOf(151));
            if (name != null) {
                part.setName(name);
            }
            Integer charset = (Integer) map.get(Integer.valueOf(129));
            if (charset != null) {
                part.setCharset(charset.intValue());
            }
            int partHeaderLen = headerLength - (startPos - pduDataStream.available());
            if (partHeaderLen > 0) {
                if (!parsePartHeaders(pduDataStream, part, partHeaderLen)) {
                    return null;
                }
            } else if (partHeaderLen < 0) {
                return null;
            }
            if (part.getContentLocation() == null && part.getName() == null && part.getFilename() == null && part.getContentId() == null) {
                part.setContentLocation(Long.toOctalString(System.currentTimeMillis()).getBytes());
            }
            if (dataLength > 0) {
                byte[] partData = new byte[dataLength];
                String str = new String(part.getContentType());
                pduDataStream.read(partData, 0, dataLength);
                if (str.equalsIgnoreCase(ContentType.MULTIPART_ALTERNATIVE)) {
                    part = parseParts(new ByteArrayInputStream(partData)).getPart(0);
                } else {
                    byte[] partDataEncoding = part.getContentTransferEncoding();
                    if (partDataEncoding != null) {
                        String encoding = new String(partDataEncoding);
                        if (encoding.equalsIgnoreCase(PduPart.P_BASE64)) {
                            partData = Base64.decodeBase64(partData);
                        } else if (encoding.equalsIgnoreCase(PduPart.P_QUOTED_PRINTABLE)) {
                            partData = QuotedPrintable.decodeQuotedPrintable(partData);
                        }
                    }
                    if (partData == null) {
                        log("Decode part data error!");
                        return null;
                    }
                    part.setData(partData);
                }
            }
            if (checkPartPosition(part) == 0) {
                body.addPart(0, part);
            } else {
                body.addPart(part);
            }
        }
        return body;
    }

    private static void log(String text) {
    }

    protected static int parseUnsignedInt(ByteArrayInputStream pduDataStream) {
        if (-assertionsDisabled || pduDataStream != null) {
            int result = 0;
            int temp = pduDataStream.read();
            if (temp == -1) {
                return temp;
            }
            while ((temp & 128) != 0) {
                result = (result << 7) | (temp & 127);
                temp = pduDataStream.read();
                if (temp == -1) {
                    return temp;
                }
            }
            return (result << 7) | (temp & 127);
        }
        throw new AssertionError();
    }

    protected static int parseValueLength(ByteArrayInputStream pduDataStream) {
        if (-assertionsDisabled || pduDataStream != null) {
            int temp = pduDataStream.read();
            if (-assertionsDisabled || -1 != temp) {
                int first = temp & 255;
                if (first <= 30) {
                    return first;
                }
                if (first == 31) {
                    return parseUnsignedInt(pduDataStream);
                }
                throw new RuntimeException("Value length > LENGTH_QUOTE!");
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    protected static EncodedStringValue parseEncodedStringValue(ByteArrayInputStream pduDataStream) {
        if (-assertionsDisabled || pduDataStream != null) {
            pduDataStream.mark(1);
            int charset = 0;
            int temp = pduDataStream.read();
            if (-assertionsDisabled || -1 != temp) {
                int first = temp & 255;
                if (first == 0) {
                    return new EncodedStringValue(SpnOverride.MVNO_TYPE_NONE);
                }
                EncodedStringValue returnValue;
                pduDataStream.reset();
                if (first < 32) {
                    parseValueLength(pduDataStream);
                    charset = parseShortInteger(pduDataStream);
                }
                byte[] textString = parseWapString(pduDataStream, 0);
                if (charset != 0) {
                    try {
                        returnValue = new EncodedStringValue(charset, textString);
                    } catch (Exception e) {
                        return null;
                    }
                }
                returnValue = new EncodedStringValue(textString);
                return returnValue;
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    protected static byte[] parseWapString(ByteArrayInputStream pduDataStream, int stringType) {
        if (-assertionsDisabled || pduDataStream != null) {
            pduDataStream.mark(1);
            int temp = pduDataStream.read();
            if (-assertionsDisabled || -1 != temp) {
                if (1 == stringType && 34 == temp) {
                    pduDataStream.mark(1);
                } else if (stringType == 0 && 127 == temp) {
                    pduDataStream.mark(1);
                } else {
                    pduDataStream.reset();
                }
                return getWapString(pduDataStream, stringType);
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    protected static boolean isTokenCharacter(int ch) {
        if (ch < 33 || ch > 126) {
            return false;
        }
        switch (ch) {
            case 34:
            case 40:
            case 41:
            case 44:
            case 47:
            case 58:
            case 59:
            case RadioError.NETWORK_NOT_READY /*60*/:
            case RadioError.NOT_PROVISIONED /*61*/:
            case RadioError.NO_SUBSCRIPTION /*62*/:
            case 63:
            case 64:
            case 91:
            case 92:
            case 93:
            case 123:
            case 125:
                return false;
            default:
                return true;
        }
    }

    protected static boolean isText(int ch) {
        if ((ch >= 32 && ch <= 126) || (ch >= 128 && ch <= 255)) {
            return true;
        }
        switch (ch) {
            case 9:
            case 10:
            case 13:
                return true;
            default:
                return false;
        }
    }

    protected static byte[] getWapString(ByteArrayInputStream pduDataStream, int stringType) {
        if (-assertionsDisabled || pduDataStream != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int temp = pduDataStream.read();
            if (-assertionsDisabled || -1 != temp) {
                while (-1 != temp && temp != 0) {
                    if (stringType == 2) {
                        if (isTokenCharacter(temp)) {
                            out.write(temp);
                        }
                    } else if (isText(temp)) {
                        out.write(temp);
                    }
                    temp = pduDataStream.read();
                    if (!-assertionsDisabled && -1 == temp) {
                        throw new AssertionError();
                    }
                }
                if (out.size() > 0) {
                    return out.toByteArray();
                }
                return null;
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    protected static int extractByteValue(ByteArrayInputStream pduDataStream) {
        if (-assertionsDisabled || pduDataStream != null) {
            int temp = pduDataStream.read();
            if (-assertionsDisabled || -1 != temp) {
                return temp & 255;
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    protected static int parseShortInteger(ByteArrayInputStream pduDataStream) {
        if (-assertionsDisabled || pduDataStream != null) {
            int temp = pduDataStream.read();
            if (-assertionsDisabled || -1 != temp) {
                return temp & 127;
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    protected static long parseLongInteger(ByteArrayInputStream pduDataStream) {
        if (-assertionsDisabled || pduDataStream != null) {
            int temp = pduDataStream.read();
            if (-assertionsDisabled || -1 != temp) {
                int count = temp & 255;
                if (count > 8) {
                    throw new RuntimeException("Octet count greater than 8 and I can't represent that!");
                }
                long result = 0;
                int i = 0;
                while (i < count) {
                    temp = pduDataStream.read();
                    if (-assertionsDisabled || -1 != temp) {
                        result = (result << 8) + ((long) (temp & 255));
                        i++;
                    } else {
                        throw new AssertionError();
                    }
                }
                return result;
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    protected static long parseIntegerValue(ByteArrayInputStream pduDataStream) {
        if (-assertionsDisabled || pduDataStream != null) {
            pduDataStream.mark(1);
            int temp = pduDataStream.read();
            if (-assertionsDisabled || -1 != temp) {
                pduDataStream.reset();
                if (temp > 127) {
                    return (long) parseShortInteger(pduDataStream);
                }
                return parseLongInteger(pduDataStream);
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    protected static int skipWapValue(ByteArrayInputStream pduDataStream, int length) {
        if (-assertionsDisabled || pduDataStream != null) {
            int readLen = pduDataStream.read(new byte[length], 0, length);
            if (readLen < length) {
                return -1;
            }
            return readLen;
        }
        throw new AssertionError();
    }

    protected static void parseContentTypeParams(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map, Integer length) {
        if (!-assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        } else if (-assertionsDisabled || length.intValue() > 0) {
            int startPos = pduDataStream.available();
            int lastLen = length.intValue();
            while (lastLen > 0) {
                int param = pduDataStream.read();
                if (-assertionsDisabled || -1 != param) {
                    lastLen--;
                    switch (param) {
                        case 129:
                            pduDataStream.mark(1);
                            int firstValue = extractByteValue(pduDataStream);
                            pduDataStream.reset();
                            if ((firstValue <= 32 || firstValue >= 127) && firstValue != 0) {
                                int charset = (int) parseIntegerValue(pduDataStream);
                                if (map != null) {
                                    map.put(Integer.valueOf(129), Integer.valueOf(charset));
                                }
                            } else {
                                byte[] charsetStr = parseWapString(pduDataStream, 0);
                                try {
                                    int charsetInt = CharacterSets.getMibEnumValue(new String(charsetStr));
                                    map.put(Integer.valueOf(129), Integer.valueOf(charsetInt));
                                } catch (UnsupportedEncodingException e) {
                                    Log.e(LOG_TAG, Arrays.toString(charsetStr), e);
                                    map.put(Integer.valueOf(129), Integer.valueOf(0));
                                }
                            }
                            lastLen = length.intValue() - (startPos - pduDataStream.available());
                            break;
                        case 131:
                        case 137:
                            pduDataStream.mark(1);
                            int first = extractByteValue(pduDataStream);
                            pduDataStream.reset();
                            if (first > 127) {
                                int index = parseShortInteger(pduDataStream);
                                if (index < PduContentTypes.contentTypes.length) {
                                    map.put(Integer.valueOf(131), PduContentTypes.contentTypes[index].getBytes());
                                }
                            } else {
                                Object type = parseWapString(pduDataStream, 0);
                                if (!(type == null || map == null)) {
                                    map.put(Integer.valueOf(131), type);
                                }
                            }
                            lastLen = length.intValue() - (startPos - pduDataStream.available());
                            break;
                        case 133:
                        case 151:
                            byte[] name = parseWapString(pduDataStream, 0);
                            if (!(name == null || map == null)) {
                                map.put(Integer.valueOf(151), name);
                            }
                            lastLen = length.intValue() - (startPos - pduDataStream.available());
                            break;
                        case 138:
                        case 153:
                            byte[] start = parseWapString(pduDataStream, 0);
                            if (!(start == null || map == null)) {
                                map.put(Integer.valueOf(153), start);
                            }
                            lastLen = length.intValue() - (startPos - pduDataStream.available());
                            break;
                        default:
                            if (-1 != skipWapValue(pduDataStream, lastLen)) {
                                lastLen = 0;
                                break;
                            } else {
                                Log.e(LOG_TAG, "Corrupt Content-Type");
                                break;
                            }
                    }
                }
                throw new AssertionError();
            }
            if (lastLen != 0) {
                Log.e(LOG_TAG, "Corrupt Content-Type");
            }
        } else {
            throw new AssertionError();
        }
    }

    protected static byte[] parseContentType(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map) {
        if (-assertionsDisabled || pduDataStream != null) {
            pduDataStream.mark(1);
            int temp = pduDataStream.read();
            if (-assertionsDisabled || -1 != temp) {
                byte[] contentType;
                pduDataStream.reset();
                int cur = temp & 255;
                if (cur < 32) {
                    int length = parseValueLength(pduDataStream);
                    int startPos = pduDataStream.available();
                    pduDataStream.mark(1);
                    temp = pduDataStream.read();
                    if (-assertionsDisabled || -1 != temp) {
                        pduDataStream.reset();
                        int first = temp & 255;
                        if (first >= 32 && first <= 127) {
                            contentType = parseWapString(pduDataStream, 0);
                        } else if (first > 127) {
                            int index = parseShortInteger(pduDataStream);
                            if (index < PduContentTypes.contentTypes.length) {
                                contentType = PduContentTypes.contentTypes[index].getBytes();
                            } else {
                                pduDataStream.reset();
                                contentType = parseWapString(pduDataStream, 0);
                            }
                        } else {
                            Log.e(LOG_TAG, "Corrupt content-type");
                            return PduContentTypes.contentTypes[0].getBytes();
                        }
                        int parameterLen = length - (startPos - pduDataStream.available());
                        if (parameterLen > 0) {
                            parseContentTypeParams(pduDataStream, map, Integer.valueOf(parameterLen));
                        }
                        if (parameterLen < 0) {
                            Log.e(LOG_TAG, "Corrupt MMS message");
                            return PduContentTypes.contentTypes[0].getBytes();
                        }
                    }
                    throw new AssertionError();
                } else if (cur <= 127) {
                    contentType = parseWapString(pduDataStream, 0);
                } else {
                    contentType = PduContentTypes.contentTypes[parseShortInteger(pduDataStream)].getBytes();
                }
                return contentType;
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    protected boolean parsePartHeaders(ByteArrayInputStream pduDataStream, PduPart part, int length) {
        if (!-assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        } else if (!-assertionsDisabled && part == null) {
            throw new AssertionError();
        } else if (-assertionsDisabled || length > 0) {
            int startPos = pduDataStream.available();
            int lastLen = length;
            while (lastLen > 0) {
                int header = pduDataStream.read();
                if (-assertionsDisabled || -1 != header) {
                    lastLen--;
                    if (header > 127) {
                        switch (header) {
                            case 142:
                                byte[] contentLocation = parseWapString(pduDataStream, 0);
                                if (contentLocation != null) {
                                    part.setContentLocation(contentLocation);
                                }
                                lastLen = length - (startPos - pduDataStream.available());
                                break;
                            case 174:
                            case PduPart.P_CONTENT_DISPOSITION /*197*/:
                                if (!this.mParseContentDisposition) {
                                    break;
                                }
                                int len = parseValueLength(pduDataStream);
                                pduDataStream.mark(1);
                                int thisStartPos = pduDataStream.available();
                                int value = pduDataStream.read();
                                if (value == 128) {
                                    part.setContentDisposition(PduPart.DISPOSITION_FROM_DATA);
                                } else if (value == 129) {
                                    part.setContentDisposition(PduPart.DISPOSITION_ATTACHMENT);
                                } else if (value == 130) {
                                    part.setContentDisposition(PduPart.DISPOSITION_INLINE);
                                } else {
                                    pduDataStream.reset();
                                    part.setContentDisposition(parseWapString(pduDataStream, 0));
                                }
                                if (thisStartPos - pduDataStream.available() < len) {
                                    if (pduDataStream.read() == 152) {
                                        part.setFilename(parseWapString(pduDataStream, 0));
                                    }
                                    int thisEndPos = pduDataStream.available();
                                    if (thisStartPos - thisEndPos < len) {
                                        int last = len - (thisStartPos - thisEndPos);
                                        pduDataStream.read(new byte[last], 0, last);
                                    }
                                }
                                lastLen = length - (startPos - pduDataStream.available());
                                break;
                            case 192:
                                byte[] contentId = parseWapString(pduDataStream, 1);
                                if (contentId != null) {
                                    part.setContentId(contentId);
                                }
                                lastLen = length - (startPos - pduDataStream.available());
                                break;
                            default:
                                if (-1 != skipWapValue(pduDataStream, lastLen)) {
                                    lastLen = 0;
                                    break;
                                }
                                Log.e(LOG_TAG, "Corrupt Part headers");
                                return false;
                        }
                    } else if (header >= 32 && header <= 127) {
                        byte[] tempHeader = parseWapString(pduDataStream, 0);
                        byte[] tempValue = parseWapString(pduDataStream, 0);
                        if (PduPart.CONTENT_TRANSFER_ENCODING.equalsIgnoreCase(new String(tempHeader))) {
                            part.setContentTransferEncoding(tempValue);
                        }
                        lastLen = length - (startPos - pduDataStream.available());
                    } else if (-1 == skipWapValue(pduDataStream, lastLen)) {
                        Log.e(LOG_TAG, "Corrupt Part headers");
                        return false;
                    } else {
                        lastLen = 0;
                    }
                } else {
                    throw new AssertionError();
                }
            }
            if (lastLen == 0) {
                return true;
            }
            Log.e(LOG_TAG, "Corrupt Part headers");
            return false;
        } else {
            throw new AssertionError();
        }
    }

    private static int checkPartPosition(PduPart part) {
        if (!-assertionsDisabled && part == null) {
            throw new AssertionError();
        } else if (mTypeParam == null && mStartParam == null) {
            return 1;
        } else {
            if (mStartParam != null) {
                byte[] contentId = part.getContentId();
                return (contentId == null || !Arrays.equals(mStartParam, contentId)) ? 1 : 0;
            } else {
                if (mTypeParam != null) {
                    byte[] contentType = part.getContentType();
                    return (contentType == null || !Arrays.equals(mTypeParam, contentType)) ? 1 : 0;
                }
            }
        }
    }

    protected static boolean checkMandatoryHeader(PduHeaders headers) {
        if (headers == null) {
            return false;
        }
        int messageType = headers.getOctet(140);
        if (headers.getOctet(141) == 0) {
            return false;
        }
        switch (messageType) {
            case 128:
                if (headers.getTextString(132) == null) {
                    return false;
                }
                if (headers.getEncodedStringValue(137) == null) {
                    return false;
                }
                if (headers.getTextString(152) == null) {
                    return false;
                }
                break;
            case 129:
                if (headers.getOctet(146) == 0) {
                    return false;
                }
                if (headers.getTextString(152) == null) {
                    return false;
                }
                break;
            case 130:
                if (headers.getTextString(131) == null) {
                    return false;
                }
                if (-1 == headers.getLongInteger(136)) {
                    return false;
                }
                if (headers.getTextString(138) == null) {
                    return false;
                }
                if (-1 == headers.getLongInteger(142)) {
                    return false;
                }
                if (headers.getTextString(152) == null) {
                    return false;
                }
                break;
            case 131:
                if (headers.getOctet(149) == 0) {
                    return false;
                }
                if (headers.getTextString(152) == null) {
                    return false;
                }
                break;
            case 132:
                if (headers.getTextString(132) == null) {
                    return false;
                }
                if (-1 == headers.getLongInteger(133)) {
                    return false;
                }
                break;
            case 133:
                if (headers.getTextString(152) == null) {
                    return false;
                }
                break;
            case 134:
                if (-1 == headers.getLongInteger(133)) {
                    return false;
                }
                if (headers.getTextString(139) == null) {
                    return false;
                }
                if (headers.getOctet(149) == 0) {
                    return false;
                }
                if (headers.getEncodedStringValues(151) == null) {
                    return false;
                }
                break;
            case 135:
                if (headers.getEncodedStringValue(137) == null) {
                    return false;
                }
                if (headers.getTextString(139) == null) {
                    return false;
                }
                if (headers.getOctet(155) == 0) {
                    return false;
                }
                if (headers.getEncodedStringValues(151) == null) {
                    return false;
                }
                break;
            case 136:
                if (-1 == headers.getLongInteger(133)) {
                    return false;
                }
                if (headers.getEncodedStringValue(137) == null) {
                    return false;
                }
                if (headers.getTextString(139) == null) {
                    return false;
                }
                if (headers.getOctet(155) == 0) {
                    return false;
                }
                if (headers.getEncodedStringValues(151) == null) {
                    return false;
                }
                break;
            default:
                return false;
        }
        return true;
    }
}
