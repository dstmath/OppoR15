package com.android.org.bouncycastle.crypto.util;

import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import java.io.IOException;
import java.io.InputStream;

public class PublicKeyFactory {
    public static AsymmetricKeyParameter createKey(byte[] keyInfoData) throws IOException {
        return createKey(SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(keyInfoData)));
    }

    public static AsymmetricKeyParameter createKey(InputStream inStr) throws IOException {
        return createKey(SubjectPublicKeyInfo.getInstance(new ASN1InputStream(inStr).readObject()));
    }

    public static com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter createKey(com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo r36) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r27_1 com.android.org.bouncycastle.crypto.params.DSAParameters) in PHI: PHI: (r27_2 com.android.org.bouncycastle.crypto.params.DSAParameters) = (r27_0 com.android.org.bouncycastle.crypto.params.DSAParameters), (r27_1 com.android.org.bouncycastle.crypto.params.DSAParameters) binds: {(r27_0 com.android.org.bouncycastle.crypto.params.DSAParameters)=B:29:0x00fa, (r27_1 com.android.org.bouncycastle.crypto.params.DSAParameters)=B:30:0x00fc}
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
        r16 = r36.getAlgorithm();
        r2 = r16.getAlgorithm();
        r10 = com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.rsaEncryption;
        r2 = r2.equals(r10);
        if (r2 != 0) goto L_0x001c;
    L_0x0010:
        r2 = r16.getAlgorithm();
        r10 = com.android.org.bouncycastle.asn1.x509.X509ObjectIdentifiers.id_ea_rsa;
        r2 = r2.equals(r10);
        if (r2 == 0) goto L_0x0033;
    L_0x001c:
        r2 = r36.parsePublicKey();
        r32 = com.android.org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(r2);
        r2 = new com.android.org.bouncycastle.crypto.params.RSAKeyParameters;
        r10 = r32.getModulus();
        r11 = r32.getPublicExponent();
        r12 = 0;
        r2.<init>(r12, r10, r11);
        return r2;
    L_0x0033:
        r2 = r16.getAlgorithm();
        r10 = com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers.dhpublicnumber;
        r2 = r2.equals(r10);
        if (r2 == 0) goto L_0x0091;
    L_0x003f:
        r2 = r36.parsePublicKey();
        r22 = com.android.org.bouncycastle.asn1.x9.DHPublicKey.getInstance(r2);
        r35 = r22.getY();
        r2 = r16.getParameters();
        r20 = com.android.org.bouncycastle.asn1.x9.DomainParameters.getInstance(r2);
        r3 = r20.getP();
        r4 = r20.getG();
        r5 = r20.getQ();
        r6 = 0;
        r2 = r20.getJ();
        if (r2 == 0) goto L_0x006a;
    L_0x0066:
        r6 = r20.getJ();
    L_0x006a:
        r7 = 0;
        r23 = r20.getValidationParams();
        if (r23 == 0) goto L_0x0084;
    L_0x0071:
        r33 = r23.getSeed();
        r31 = r23.getPgenCounter();
        r7 = new com.android.org.bouncycastle.crypto.params.DHValidationParameters;
        r2 = r31.intValue();
        r0 = r33;
        r7.<init>(r0, r2);
    L_0x0084:
        r10 = new com.android.org.bouncycastle.crypto.params.DHPublicKeyParameters;
        r2 = new com.android.org.bouncycastle.crypto.params.DHParameters;
        r2.<init>(r3, r4, r5, r6, r7);
        r0 = r35;
        r10.<init>(r0, r2);
        return r10;
    L_0x0091:
        r2 = r16.getAlgorithm();
        r10 = com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.dhKeyAgreement;
        r2 = r2.equals(r10);
        if (r2 == 0) goto L_0x00d6;
    L_0x009d:
        r2 = r16.getParameters();
        r28 = com.android.org.bouncycastle.asn1.pkcs.DHParameter.getInstance(r2);
        r19 = r36.parsePublicKey();
        r19 = (com.android.org.bouncycastle.asn1.ASN1Integer) r19;
        r26 = r28.getL();
        if (r26 != 0) goto L_0x00d1;
    L_0x00b1:
        r25 = 0;
    L_0x00b3:
        r21 = new com.android.org.bouncycastle.crypto.params.DHParameters;
        r2 = r28.getP();
        r10 = r28.getG();
        r11 = 0;
        r0 = r21;
        r1 = r25;
        r0.<init>(r2, r10, r11, r1);
        r2 = new com.android.org.bouncycastle.crypto.params.DHPublicKeyParameters;
        r10 = r19.getValue();
        r0 = r21;
        r2.<init>(r10, r0);
        return r2;
    L_0x00d1:
        r25 = r26.intValue();
        goto L_0x00b3;
    L_0x00d6:
        r2 = r16.getAlgorithm();
        r10 = com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers.id_dsa;
        r2 = r2.equals(r10);
        if (r2 != 0) goto L_0x00ee;
    L_0x00e2:
        r2 = r16.getAlgorithm();
        r10 = com.android.org.bouncycastle.asn1.oiw.OIWObjectIdentifiers.dsaWithSHA1;
        r2 = r2.equals(r10);
        if (r2 == 0) goto L_0x0123;
    L_0x00ee:
        r19 = r36.parsePublicKey();
        r19 = (com.android.org.bouncycastle.asn1.ASN1Integer) r19;
        r17 = r16.getParameters();
        r27 = 0;
        if (r17 == 0) goto L_0x0117;
    L_0x00fc:
        r2 = r17.toASN1Primitive();
        r29 = com.android.org.bouncycastle.asn1.x509.DSAParameter.getInstance(r2);
        r27 = new com.android.org.bouncycastle.crypto.params.DSAParameters;
        r2 = r29.getP();
        r10 = r29.getQ();
        r11 = r29.getG();
        r0 = r27;
        r0.<init>(r2, r10, r11);
    L_0x0117:
        r2 = new com.android.org.bouncycastle.crypto.params.DSAPublicKeyParameters;
        r10 = r19.getValue();
        r0 = r27;
        r2.<init>(r10, r0);
        return r2;
    L_0x0123:
        r2 = r16.getAlgorithm();
        r10 = com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers.id_ecPublicKey;
        r2 = r2.equals(r10);
        if (r2 == 0) goto L_0x01af;
    L_0x012f:
        r2 = r16.getParameters();
        r30 = com.android.org.bouncycastle.asn1.x9.X962Parameters.getInstance(r2);
        r2 = r30.isNamedCurve();
        if (r2 == 0) goto L_0x018c;
    L_0x013d:
        r9 = r30.getParameters();
        r9 = (com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier) r9;
        r34 = com.android.org.bouncycastle.crypto.ec.CustomNamedCurves.getByOID(r9);
        if (r34 != 0) goto L_0x014d;
    L_0x0149:
        r34 = com.android.org.bouncycastle.asn1.x9.ECNamedCurveTable.getByOID(r9);
    L_0x014d:
        r8 = new com.android.org.bouncycastle.crypto.params.ECNamedDomainParameters;
        r10 = r34.getCurve();
        r11 = r34.getG();
        r12 = r34.getN();
        r13 = r34.getH();
        r14 = r34.getSeed();
        r8.<init>(r9, r10, r11, r12, r13, r14);
    L_0x0166:
        r24 = new com.android.org.bouncycastle.asn1.DEROctetString;
        r2 = r36.getPublicKeyData();
        r2 = r2.getBytes();
        r0 = r24;
        r0.<init>(r2);
        r18 = new com.android.org.bouncycastle.asn1.x9.X9ECPoint;
        r2 = r34.getCurve();
        r0 = r18;
        r1 = r24;
        r0.<init>(r2, r1);
        r2 = new com.android.org.bouncycastle.crypto.params.ECPublicKeyParameters;
        r10 = r18.getPoint();
        r2.<init>(r10, r8);
        return r2;
    L_0x018c:
        r2 = r30.getParameters();
        r34 = com.android.org.bouncycastle.asn1.x9.X9ECParameters.getInstance(r2);
        r8 = new com.android.org.bouncycastle.crypto.params.ECDomainParameters;
        r11 = r34.getCurve();
        r12 = r34.getG();
        r13 = r34.getN();
        r14 = r34.getH();
        r15 = r34.getSeed();
        r10 = r8;
        r10.<init>(r11, r12, r13, r14, r15);
        goto L_0x0166;
    L_0x01af:
        r2 = new java.lang.RuntimeException;
        r10 = "algorithm identifier in key not recognised";
        r2.<init>(r10);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.crypto.util.PublicKeyFactory.createKey(com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo):com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter");
    }
}
