package com.android.server.wifi.hotspot2;

import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType;
import com.android.server.wifi.hotspot2.anqp.RawByteElement;
import com.android.server.wifi.util.InformationElementUtil.ExtendedCapabilities;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class NetworkDetail {
    private static final boolean DBG = false;
    private static final String TAG = "NetworkDetail:";
    private final Map<ANQPElementType, ANQPElement> mANQPElements;
    private final int mAnqpDomainID;
    private final int mAnqpOICount;
    private final Ant mAnt;
    private final long mBSSID;
    private final int mCapacity;
    private final int mCenterfreq0;
    private final int mCenterfreq1;
    private final int mChannelUtilization;
    private final int mChannelWidth;
    private int mDtimInterval;
    private final ExtendedCapabilities mExtendedCapabilities;
    private final long mHESSID;
    private final HSRelease mHSRelease;
    private final boolean mInternet;
    private final boolean mIsHiddenSsid;
    private final int mMaxRate;
    private final int mPrimaryFreq;
    private final long[] mRoamingConsortiums;
    private final String mSSID;
    private final int mStationCount;
    private final int mWifiMode;

    public enum Ant {
        Private,
        PrivateWithGuest,
        ChargeablePublic,
        FreePublic,
        Personal,
        EmergencyOnly,
        Resvd6,
        Resvd7,
        Resvd8,
        Resvd9,
        Resvd10,
        Resvd11,
        Resvd12,
        Resvd13,
        TestOrExperimental,
        Wildcard
    }

    public enum HSRelease {
        R1,
        R2,
        Unknown
    }

    public NetworkDetail(java.lang.String r33, android.net.wifi.ScanResult.InformationElement[] r34, java.util.List<java.lang.String> r35, int r36) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r21_3 'ssid' java.lang.String) in PHI: PHI: (r21_4 'ssid' java.lang.String) = (r21_2 'ssid' java.lang.String), (r21_3 'ssid' java.lang.String) binds: {(r21_2 'ssid' java.lang.String)=B:34:0x0105, (r21_3 'ssid' java.lang.String)=B:42:0x011c}
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
        r32 = this;
        r32.<init>();
        r27 = -1;
        r0 = r27;
        r1 = r32;
        r1.mDtimInterval = r0;
        if (r34 != 0) goto L_0x0016;
    L_0x000d:
        r27 = new java.lang.IllegalArgumentException;
        r28 = "Null information elements";
        r27.<init>(r28);
        throw r27;
    L_0x0016:
        r28 = com.android.server.wifi.hotspot2.Utils.parseMac(r33);
        r0 = r28;
        r2 = r32;
        r2.mBSSID = r0;
        r21 = 0;
        r17 = 0;
        r22 = 0;
        r4 = new com.android.server.wifi.util.InformationElementUtil$BssLoad;
        r4.<init>();
        r16 = new com.android.server.wifi.util.InformationElementUtil$Interworking;
        r16.<init>();
        r20 = new com.android.server.wifi.util.InformationElementUtil$RoamingConsortium;
        r20.<init>();
        r26 = new com.android.server.wifi.util.InformationElementUtil$Vsa;
        r26.<init>();
        r13 = new com.android.server.wifi.util.InformationElementUtil$HtOperation;
        r13.<init>();
        r25 = new com.android.server.wifi.util.InformationElementUtil$VhtOperation;
        r25.<init>();
        r11 = new com.android.server.wifi.util.InformationElementUtil$ExtendedCapabilities;
        r11.<init>();
        r24 = new com.android.server.wifi.util.InformationElementUtil$TrafficIndicationMap;
        r24.<init>();
        r23 = new com.android.server.wifi.util.InformationElementUtil$SupportedRates;
        r23.<init>();
        r12 = new com.android.server.wifi.util.InformationElementUtil$SupportedRates;
        r12.<init>();
        r10 = 0;
        r15 = new java.util.ArrayList;
        r15.<init>();
        r27 = 0;
        r0 = r34;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0 = r0.length;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r28 = r0;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x0065:
        r0 = r27;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r1 = r28;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        if (r0 >= r1) goto L_0x00ef;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x006b:
        r14 = r34[r27];	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0 = r14.id;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r29 = r0;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r29 = java.lang.Integer.valueOf(r29);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0 = r29;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r15.add(r0);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0 = r14.id;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r29 = r0;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        switch(r29) {
            case 0: goto L_0x0084;
            case 1: goto L_0x00e4;
            case 5: goto L_0x00de;
            case 11: goto L_0x0089;
            case 50: goto L_0x00ea;
            case 61: goto L_0x00be;
            case 107: goto L_0x00c8;
            case 111: goto L_0x00ce;
            case 127: goto L_0x00da;
            case 192: goto L_0x00c2;
            case 221: goto L_0x00d4;
            default: goto L_0x0081;
        };	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x0081:
        r27 = r27 + 1;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0065;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x0084:
        r0 = r14.bytes;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r22 = r0;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x0089:
        r4.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;
    L_0x008d:
        r9 = move-exception;
        r27 = r32.getClass();
        r27 = com.android.server.wifi.hotspot2.Utils.hs2LogTag(r27);
        r28 = new java.lang.StringBuilder;
        r28.<init>();
        r29 = "Caught ";
        r28 = r28.append(r29);
        r0 = r28;
        r28 = r0.append(r9);
        r28 = r28.toString();
        android.util.Log.d(r27, r28);
        if (r22 != 0) goto L_0x00ee;
    L_0x00b1:
        r27 = new java.lang.IllegalArgumentException;
        r28 = "Malformed IE string (no SSID)";
        r0 = r27;
        r1 = r28;
        r0.<init>(r1, r9);
        throw r27;
    L_0x00be:
        r13.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x00c2:
        r0 = r25;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x00c8:
        r0 = r16;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x00ce:
        r0 = r20;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x00d4:
        r0 = r26;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x00da:
        r11.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x00de:
        r0 = r24;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x00e4:
        r0 = r23;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        r0.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
    L_0x00ea:
        r12.from(r14);	 Catch:{ IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d, IllegalArgumentException -> 0x008d }
        goto L_0x0081;
    L_0x00ee:
        r10 = r9;
    L_0x00ef:
        if (r22 == 0) goto L_0x013e;
    L_0x00f1:
        r27 = java.nio.charset.StandardCharsets.UTF_8;
        r8 = r27.newDecoder();
        r27 = java.nio.ByteBuffer.wrap(r22);	 Catch:{ CharacterCodingException -> 0x0118 }
        r0 = r27;	 Catch:{ CharacterCodingException -> 0x0118 }
        r7 = r8.decode(r0);	 Catch:{ CharacterCodingException -> 0x0118 }
        r21 = r7.toString();	 Catch:{ CharacterCodingException -> 0x0118 }
    L_0x0105:
        if (r21 != 0) goto L_0x0129;
    L_0x0107:
        r27 = r11.isStrictUtf8();
        if (r27 == 0) goto L_0x011c;
    L_0x010d:
        if (r10 == 0) goto L_0x011c;
    L_0x010f:
        r27 = new java.lang.IllegalArgumentException;
        r28 = "Failed to decode SSID in dubious IE string";
        r27.<init>(r28);
        throw r27;
    L_0x0118:
        r6 = move-exception;
        r21 = 0;
        goto L_0x0105;
    L_0x011c:
        r21 = new java.lang.String;
        r27 = java.nio.charset.StandardCharsets.ISO_8859_1;
        r0 = r21;
        r1 = r22;
        r2 = r27;
        r0.<init>(r1, r2);
    L_0x0129:
        r17 = 1;
        r27 = 0;
        r0 = r22;
        r0 = r0.length;
        r28 = r0;
    L_0x0132:
        r0 = r27;
        r1 = r28;
        if (r0 >= r1) goto L_0x013e;
    L_0x0138:
        r5 = r22[r27];
        if (r5 == 0) goto L_0x0289;
    L_0x013c:
        r17 = 0;
    L_0x013e:
        r0 = r21;
        r1 = r32;
        r1.mSSID = r0;
        r0 = r16;
        r0 = r0.hessid;
        r28 = r0;
        r0 = r28;
        r2 = r32;
        r2.mHESSID = r0;
        r0 = r17;
        r1 = r32;
        r1.mIsHiddenSsid = r0;
        r0 = r4.stationCount;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mStationCount = r0;
        r0 = r4.channelUtilization;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mChannelUtilization = r0;
        r0 = r4.capacity;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mCapacity = r0;
        r0 = r16;
        r0 = r0.ant;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mAnt = r0;
        r0 = r16;
        r0 = r0.internet;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mInternet = r0;
        r0 = r26;
        r0 = r0.hsRelease;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mHSRelease = r0;
        r0 = r26;
        r0 = r0.anqpDomainID;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mAnqpDomainID = r0;
        r0 = r20;
        r0 = r0.anqpOICount;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mAnqpOICount = r0;
        r0 = r20;
        r0 = r0.roamingConsortiums;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mRoamingConsortiums = r0;
        r0 = r32;
        r0.mExtendedCapabilities = r11;
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mANQPElements = r0;
        r0 = r36;
        r1 = r32;
        r1.mPrimaryFreq = r0;
        r27 = r25.isValid();
        if (r27 == 0) goto L_0x028d;
    L_0x01d4:
        r27 = r25.getChannelWidth();
        r0 = r27;
        r1 = r32;
        r1.mChannelWidth = r0;
        r27 = r25.getCenterFreq0();
        r0 = r27;
        r1 = r32;
        r1.mCenterfreq0 = r0;
        r27 = r25.getCenterFreq1();
        r0 = r27;
        r1 = r32;
        r1.mCenterfreq1 = r0;
    L_0x01f2:
        r27 = r24.isValid();
        if (r27 == 0) goto L_0x0204;
    L_0x01f8:
        r0 = r24;
        r0 = r0.mDtimPeriod;
        r27 = r0;
        r0 = r27;
        r1 = r32;
        r1.mDtimInterval = r0;
    L_0x0204:
        r18 = 0;
        r19 = 0;
        r27 = r12.isValid();
        if (r27 == 0) goto L_0x0226;
    L_0x020e:
        r0 = r12.mRates;
        r27 = r0;
        r0 = r12.mRates;
        r28 = r0;
        r28 = r28.size();
        r28 = r28 + -1;
        r27 = r27.get(r28);
        r27 = (java.lang.Integer) r27;
        r19 = r27.intValue();
    L_0x0226:
        r27 = r23.isValid();
        if (r27 == 0) goto L_0x02b3;
    L_0x022c:
        r0 = r23;
        r0 = r0.mRates;
        r27 = r0;
        r0 = r23;
        r0 = r0.mRates;
        r28 = r0;
        r28 = r28.size();
        r28 = r28 + -1;
        r27 = r27.get(r28);
        r27 = (java.lang.Integer) r27;
        r18 = r27.intValue();
        r0 = r18;
        r1 = r19;
        if (r0 <= r1) goto L_0x0250;
    L_0x024e:
        r19 = r18;
    L_0x0250:
        r0 = r19;
        r1 = r32;
        r1.mMaxRate = r0;
        r0 = r32;
        r0 = r0.mPrimaryFreq;
        r27 = r0;
        r0 = r32;
        r0 = r0.mMaxRate;
        r28 = r0;
        r29 = r25.isValid();
        r30 = 61;
        r30 = java.lang.Integer.valueOf(r30);
        r0 = r30;
        r30 = r15.contains(r0);
        r31 = 42;
        r31 = java.lang.Integer.valueOf(r31);
        r0 = r31;
        r31 = r15.contains(r0);
        r27 = com.android.server.wifi.util.InformationElementUtil.WifiMode.determineMode(r27, r28, r29, r30, r31);
        r0 = r27;
        r1 = r32;
        r1.mWifiMode = r0;
    L_0x0288:
        return;
    L_0x0289:
        r27 = r27 + 1;
        goto L_0x0132;
    L_0x028d:
        r27 = r13.getChannelWidth();
        r0 = r27;
        r1 = r32;
        r1.mChannelWidth = r0;
        r0 = r32;
        r0 = r0.mPrimaryFreq;
        r27 = r0;
        r0 = r27;
        r27 = r13.getCenterFreq0(r0);
        r0 = r27;
        r1 = r32;
        r1.mCenterfreq0 = r0;
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mCenterfreq1 = r0;
        goto L_0x01f2;
    L_0x02b3:
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mWifiMode = r0;
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mMaxRate = r0;
        goto L_0x0288;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.hotspot2.NetworkDetail.<init>(java.lang.String, android.net.wifi.ScanResult$InformationElement[], java.util.List, int):void");
    }

    private static ByteBuffer getAndAdvancePayload(ByteBuffer data, int plLength) {
        ByteBuffer payload = data.duplicate().order(data.order());
        payload.limit(payload.position() + plLength);
        data.position(data.position() + plLength);
        return payload;
    }

    private NetworkDetail(NetworkDetail base, Map<ANQPElementType, ANQPElement> anqpElements) {
        this.mDtimInterval = -1;
        this.mSSID = base.mSSID;
        this.mIsHiddenSsid = base.mIsHiddenSsid;
        this.mBSSID = base.mBSSID;
        this.mHESSID = base.mHESSID;
        this.mStationCount = base.mStationCount;
        this.mChannelUtilization = base.mChannelUtilization;
        this.mCapacity = base.mCapacity;
        this.mAnt = base.mAnt;
        this.mInternet = base.mInternet;
        this.mHSRelease = base.mHSRelease;
        this.mAnqpDomainID = base.mAnqpDomainID;
        this.mAnqpOICount = base.mAnqpOICount;
        this.mRoamingConsortiums = base.mRoamingConsortiums;
        this.mExtendedCapabilities = new ExtendedCapabilities(base.mExtendedCapabilities);
        this.mANQPElements = anqpElements;
        this.mChannelWidth = base.mChannelWidth;
        this.mPrimaryFreq = base.mPrimaryFreq;
        this.mCenterfreq0 = base.mCenterfreq0;
        this.mCenterfreq1 = base.mCenterfreq1;
        this.mDtimInterval = base.mDtimInterval;
        this.mWifiMode = base.mWifiMode;
        this.mMaxRate = base.mMaxRate;
    }

    public NetworkDetail complete(Map<ANQPElementType, ANQPElement> anqpElements) {
        return new NetworkDetail(this, anqpElements);
    }

    public boolean queriable(List<ANQPElementType> queryElements) {
        if (this.mAnt == null) {
            return false;
        }
        if (Constants.hasBaseANQPElements(queryElements)) {
            return true;
        }
        return Constants.hasR2Elements(queryElements) && this.mHSRelease == HSRelease.R2;
    }

    public boolean has80211uInfo() {
        return (this.mAnt == null && this.mRoamingConsortiums == null && this.mHSRelease == null) ? false : true;
    }

    public boolean hasInterworking() {
        return this.mAnt != null;
    }

    public String getSSID() {
        return this.mSSID;
    }

    public String getTrimmedSSID() {
        if (this.mSSID != null) {
            for (int n = 0; n < this.mSSID.length(); n++) {
                if (this.mSSID.charAt(n) != 0) {
                    return this.mSSID;
                }
            }
        }
        return "";
    }

    public long getHESSID() {
        return this.mHESSID;
    }

    public long getBSSID() {
        return this.mBSSID;
    }

    public int getStationCount() {
        return this.mStationCount;
    }

    public int getChannelUtilization() {
        return this.mChannelUtilization;
    }

    public int getCapacity() {
        return this.mCapacity;
    }

    public boolean isInterworking() {
        return this.mAnt != null;
    }

    public Ant getAnt() {
        return this.mAnt;
    }

    public boolean isInternet() {
        return this.mInternet;
    }

    public HSRelease getHSRelease() {
        return this.mHSRelease;
    }

    public int getAnqpDomainID() {
        return this.mAnqpDomainID;
    }

    public byte[] getOsuProviders() {
        byte[] bArr = null;
        if (this.mANQPElements == null) {
            return null;
        }
        ANQPElement osuProviders = (ANQPElement) this.mANQPElements.get(ANQPElementType.HSOSUProviders);
        if (osuProviders != null) {
            bArr = ((RawByteElement) osuProviders).getPayload();
        }
        return bArr;
    }

    public int getAnqpOICount() {
        return this.mAnqpOICount;
    }

    public long[] getRoamingConsortiums() {
        return this.mRoamingConsortiums;
    }

    public Map<ANQPElementType, ANQPElement> getANQPElements() {
        return this.mANQPElements;
    }

    public int getChannelWidth() {
        return this.mChannelWidth;
    }

    public int getCenterfreq0() {
        return this.mCenterfreq0;
    }

    public int getCenterfreq1() {
        return this.mCenterfreq1;
    }

    public int getWifiMode() {
        return this.mWifiMode;
    }

    public int getDtimInterval() {
        return this.mDtimInterval;
    }

    public boolean is80211McResponderSupport() {
        return this.mExtendedCapabilities.is80211McRTTResponder();
    }

    public boolean isSSID_UTF8() {
        return this.mExtendedCapabilities.isStrictUtf8();
    }

    public boolean equals(Object thatObject) {
        boolean z = true;
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        NetworkDetail that = (NetworkDetail) thatObject;
        if (!(getSSID().equals(that.getSSID()) && getBSSID() == that.getBSSID())) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (((this.mSSID.hashCode() * 31) + ((int) (this.mBSSID >>> 32))) * 31) + ((int) this.mBSSID);
    }

    public String toString() {
        return String.format("NetworkInfo{SSID='%s', HESSID=%x, BSSID=%x, StationCount=%d, ChannelUtilization=%d, Capacity=%d, Ant=%s, Internet=%s, HSRelease=%s, AnqpDomainID=%d, AnqpOICount=%d, RoamingConsortiums=%s}", new Object[]{this.mSSID, Long.valueOf(this.mHESSID), Long.valueOf(this.mBSSID), Integer.valueOf(this.mStationCount), Integer.valueOf(this.mChannelUtilization), Integer.valueOf(this.mCapacity), this.mAnt, Boolean.valueOf(this.mInternet), this.mHSRelease, Integer.valueOf(this.mAnqpDomainID), Integer.valueOf(this.mAnqpOICount), Utils.roamingConsortiumsToString(this.mRoamingConsortiums)});
    }

    public String toKeyString() {
        if (this.mHESSID != 0) {
            return String.format("'%s':%012x (%012x)", new Object[]{this.mSSID, Long.valueOf(this.mBSSID), Long.valueOf(this.mHESSID)});
        }
        return String.format("'%s':%012x", new Object[]{this.mSSID, Long.valueOf(this.mBSSID)});
    }

    public String getBSSIDString() {
        return toMACString(this.mBSSID);
    }

    public boolean isBeaconFrame() {
        return this.mDtimInterval > 0;
    }

    public boolean isHiddenBeaconFrame() {
        return isBeaconFrame() ? this.mIsHiddenSsid : false;
    }

    public static String toMACString(long mac) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int n = 5; n >= 0; n--) {
            if (first) {
                first = false;
            } else {
                sb.append(':');
            }
            sb.append(String.format("%02x", new Object[]{Long.valueOf((mac >>> (n * 8)) & 255)}));
        }
        return sb.toString();
    }
}
