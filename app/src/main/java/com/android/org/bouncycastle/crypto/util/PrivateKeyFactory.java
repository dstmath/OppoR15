package com.android.org.bouncycastle.crypto.util;

import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import java.io.IOException;
import java.io.InputStream;

public class PrivateKeyFactory {
    public static AsymmetricKeyParameter createKey(byte[] privateKeyInfoData) throws IOException {
        return createKey(PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(privateKeyInfoData)));
    }

    public static AsymmetricKeyParameter createKey(InputStream inStr) throws IOException {
        return createKey(PrivateKeyInfo.getInstance(new ASN1InputStream(inStr).readObject()));
    }

    public static com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter createKey(com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo r24) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r19_1 com.android.org.bouncycastle.crypto.params.DSAParameters) in PHI: PHI: (r19_2 com.android.org.bouncycastle.crypto.params.DSAParameters) = (r19_0 com.android.org.bouncycastle.crypto.params.DSAParameters), (r19_1 com.android.org.bouncycastle.crypto.params.DSAParameters) binds: {(r19_0 com.android.org.bouncycastle.crypto.params.DSAParameters)=B:15:0x0097, (r19_1 com.android.org.bouncycastle.crypto.params.DSAParameters)=B:16:0x0099}
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
        r10 = r24.getPrivateKeyAlgorithm();
        r3 = r10.getAlgorithm();
        r4 = com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.rsaEncryption;
        r3 = r3.equals(r4);
        if (r3 == 0) goto L_0x003e;
    L_0x0010:
        r3 = r24.parsePrivateKey();
        r16 = com.android.org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(r3);
        r1 = new com.android.org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
        r2 = r16.getModulus();
        r3 = r16.getPublicExponent();
        r4 = r16.getPrivateExponent();
        r5 = r16.getPrime1();
        r6 = r16.getPrime2();
        r7 = r16.getExponent1();
        r8 = r16.getExponent2();
        r9 = r16.getCoefficient();
        r1.<init>(r2, r3, r4, r5, r6, r7, r8, r9);
        return r1;
    L_0x003e:
        r3 = r10.getAlgorithm();
        r4 = com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.dhKeyAgreement;
        r3 = r3.equals(r4);
        if (r3 == 0) goto L_0x007f;
    L_0x004a:
        r3 = r10.getParameters();
        r20 = com.android.org.bouncycastle.asn1.pkcs.DHParameter.getInstance(r3);
        r13 = r24.parsePrivateKey();
        r13 = (com.android.org.bouncycastle.asn1.ASN1Integer) r13;
        r18 = r20.getL();
        if (r18 != 0) goto L_0x007a;
    L_0x005e:
        r17 = 0;
    L_0x0060:
        r14 = new com.android.org.bouncycastle.crypto.params.DHParameters;
        r3 = r20.getP();
        r4 = r20.getG();
        r5 = 0;
        r0 = r17;
        r14.<init>(r3, r4, r5, r0);
        r3 = new com.android.org.bouncycastle.crypto.params.DHPrivateKeyParameters;
        r4 = r13.getValue();
        r3.<init>(r4, r14);
        return r3;
    L_0x007a:
        r17 = r18.intValue();
        goto L_0x0060;
    L_0x007f:
        r3 = r10.getAlgorithm();
        r4 = com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers.id_dsa;
        r3 = r3.equals(r4);
        if (r3 == 0) goto L_0x00c0;
    L_0x008b:
        r13 = r24.parsePrivateKey();
        r13 = (com.android.org.bouncycastle.asn1.ASN1Integer) r13;
        r12 = r10.getParameters();
        r19 = 0;
        if (r12 == 0) goto L_0x00b4;
    L_0x0099:
        r3 = r12.toASN1Primitive();
        r21 = com.android.org.bouncycastle.asn1.x509.DSAParameter.getInstance(r3);
        r19 = new com.android.org.bouncycastle.crypto.params.DSAParameters;
        r3 = r21.getP();
        r4 = r21.getQ();
        r5 = r21.getG();
        r0 = r19;
        r0.<init>(r3, r4, r5);
    L_0x00b4:
        r3 = new com.android.org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
        r4 = r13.getValue();
        r0 = r19;
        r3.<init>(r4, r0);
        return r3;
    L_0x00c0:
        r3 = r10.getAlgorithm();
        r4 = com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers.id_ecPublicKey;
        r3 = r3.equals(r4);
        if (r3 == 0) goto L_0x013d;
    L_0x00cc:
        r22 = new com.android.org.bouncycastle.asn1.x9.X962Parameters;
        r3 = r10.getParameters();
        r3 = (com.android.org.bouncycastle.asn1.ASN1Primitive) r3;
        r0 = r22;
        r0.<init>(r3);
        r3 = r22.isNamedCurve();
        if (r3 == 0) goto L_0x011a;
    L_0x00df:
        r2 = r22.getParameters();
        r2 = (com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier) r2;
        r23 = com.android.org.bouncycastle.crypto.ec.CustomNamedCurves.getByOID(r2);
        if (r23 != 0) goto L_0x00ef;
    L_0x00eb:
        r23 = com.android.org.bouncycastle.asn1.x9.ECNamedCurveTable.getByOID(r2);
    L_0x00ef:
        r1 = new com.android.org.bouncycastle.crypto.params.ECNamedDomainParameters;
        r3 = r23.getCurve();
        r4 = r23.getG();
        r5 = r23.getN();
        r6 = r23.getH();
        r7 = r23.getSeed();
        r1.<init>(r2, r3, r4, r5, r6, r7);
    L_0x0108:
        r3 = r24.parsePrivateKey();
        r15 = com.android.org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(r3);
        r11 = r15.getKey();
        r3 = new com.android.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
        r3.<init>(r11, r1);
        return r3;
    L_0x011a:
        r3 = r22.getParameters();
        r23 = com.android.org.bouncycastle.asn1.x9.X9ECParameters.getInstance(r3);
        r1 = new com.android.org.bouncycastle.crypto.params.ECDomainParameters;
        r4 = r23.getCurve();
        r5 = r23.getG();
        r6 = r23.getN();
        r7 = r23.getH();
        r8 = r23.getSeed();
        r3 = r1;
        r3.<init>(r4, r5, r6, r7, r8);
        goto L_0x0108;
    L_0x013d:
        r3 = new java.lang.RuntimeException;
        r4 = "algorithm identifier in key not recognised";
        r3.<init>(r4);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.crypto.util.PrivateKeyFactory.createKey(com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo):com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter");
    }
}
