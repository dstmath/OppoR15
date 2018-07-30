package com.android.org.bouncycastle.crypto.generators;

import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import com.android.org.bouncycastle.crypto.KeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import com.android.org.bouncycastle.math.Primes;
import java.math.BigInteger;

public class RSAKeyPairGenerator implements AsymmetricCipherKeyPairGenerator {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private int iterations;
    private RSAKeyGenerationParameters param;

    public void init(KeyGenerationParameters param) {
        this.param = (RSAKeyGenerationParameters) param;
        this.iterations = getNumberOfIterations(this.param.getStrength(), this.param.getCertainty());
    }

    public com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair generateKeyPair() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r25_2 'result' com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair) in PHI: PHI: (r25_3 'result' com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair) = (r25_2 'result' com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair), (r25_1 'result' com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair) binds: {(r25_2 'result' com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair)=B:26:0x003f, (r25_1 'result' com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair)=B:25:0x003f}
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
        r30 = this;
        r25 = 0;
        r15 = 0;
        r0 = r30;
        r4 = r0.param;
        r27 = r4.getStrength();
        r4 = r27 + 1;
        r22 = r4 / 2;
        r24 = r27 - r22;
        r4 = r27 / 2;
        r20 = r4 + -100;
        r4 = r27 / 3;
        r0 = r20;
        if (r0 >= r4) goto L_0x001d;
    L_0x001b:
        r20 = r27 / 3;
    L_0x001d:
        r19 = r27 >> 2;
        r28 = 2;
        r4 = java.math.BigInteger.valueOf(r28);
        r28 = r27 / 2;
        r0 = r28;
        r13 = r4.pow(r0);
        r4 = ONE;
        r28 = r27 + -1;
        r0 = r28;
        r26 = r4.shiftLeft(r0);
        r4 = ONE;
        r0 = r20;
        r18 = r4.shiftLeft(r0);
    L_0x003f:
        if (r15 != 0) goto L_0x00fb;
    L_0x0041:
        r0 = r30;
        r4 = r0.param;
        r6 = r4.getPublicExponent();
        r0 = r30;
        r1 = r22;
        r2 = r26;
        r8 = r0.chooseRandomPrime(r1, r6, r2);
    L_0x0053:
        r0 = r30;
        r1 = r24;
        r2 = r26;
        r9 = r0.chooseRandomPrime(r1, r6, r2);
        r4 = r9.subtract(r8);
        r14 = r4.abs();
        r4 = r14.bitLength();
        r0 = r20;
        if (r4 < r0) goto L_0x0053;
    L_0x006d:
        r0 = r18;
        r4 = r14.compareTo(r0);
        if (r4 <= 0) goto L_0x0053;
    L_0x0075:
        r5 = r8.multiply(r9);
        r4 = r5.bitLength();
        r0 = r27;
        if (r4 == r0) goto L_0x0086;
    L_0x0081:
        r8 = r8.max(r9);
        goto L_0x0053;
    L_0x0086:
        r4 = com.android.org.bouncycastle.math.ec.WNafUtil.getNafWeight(r5);
        r0 = r19;
        if (r4 >= r0) goto L_0x0099;
    L_0x008e:
        r0 = r30;
        r1 = r22;
        r2 = r26;
        r8 = r0.chooseRandomPrime(r1, r6, r2);
        goto L_0x0053;
    L_0x0099:
        r4 = r8.compareTo(r9);
        if (r4 >= 0) goto L_0x00a4;
    L_0x009f:
        r16 = r8;
        r8 = r9;
        r9 = r16;
    L_0x00a4:
        r4 = ONE;
        r21 = r8.subtract(r4);
        r4 = ONE;
        r23 = r9.subtract(r4);
        r0 = r21;
        r1 = r23;
        r16 = r0.gcd(r1);
        r0 = r21;
        r1 = r16;
        r4 = r0.divide(r1);
        r0 = r23;
        r17 = r4.multiply(r0);
        r0 = r17;
        r7 = r6.modInverse(r0);
        r4 = r7.compareTo(r13);
        if (r4 <= 0) goto L_0x003f;
    L_0x00d2:
        r15 = 1;
        r0 = r21;
        r10 = r7.remainder(r0);
        r0 = r23;
        r11 = r7.remainder(r0);
        r12 = r9.modInverse(r8);
        r25 = new com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
        r28 = new com.android.org.bouncycastle.crypto.params.RSAKeyParameters;
        r4 = 0;
        r0 = r28;
        r0.<init>(r4, r5, r6);
        r4 = new com.android.org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
        r4.<init>(r5, r6, r7, r8, r9, r10, r11, r12);
        r0 = r25;
        r1 = r28;
        r0.<init>(r1, r4);
        goto L_0x003f;
    L_0x00fb:
        return r25;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.crypto.generators.RSAKeyPairGenerator.generateKeyPair():com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair");
    }

    protected BigInteger chooseRandomPrime(int bitlength, BigInteger e, BigInteger sqrdBound) {
        for (int i = 0; i != bitlength * 5; i++) {
            BigInteger p = new BigInteger(bitlength, 1, this.param.getRandom());
            if (!p.mod(e).equals(ONE) && p.multiply(p).compareTo(sqrdBound) >= 0 && isProbablePrime(p) && e.gcd(p.subtract(ONE)).equals(ONE)) {
                return p;
            }
        }
        throw new IllegalStateException("unable to generate prime number for RSA key");
    }

    protected boolean isProbablePrime(BigInteger x) {
        return !Primes.hasAnySmallFactors(x) ? Primes.isMRProbablePrime(x, this.param.getRandom(), this.iterations) : false;
    }

    private static int getNumberOfIterations(int bits, int certainty) {
        int i = 5;
        int i2 = 4;
        if (bits >= 1536) {
            if (certainty <= 100) {
                i2 = 3;
            } else if (certainty > 128) {
                i2 = (((certainty - 128) + 1) / 2) + 4;
            }
            return i2;
        } else if (bits >= 1024) {
            if (certainty > 100) {
                if (certainty <= 112) {
                    i2 = 5;
                } else {
                    i2 = (((certainty - 112) + 1) / 2) + 5;
                }
            }
            return i2;
        } else if (bits >= 512) {
            if (certainty > 80) {
                if (certainty <= 100) {
                    i = 7;
                } else {
                    i = (((certainty - 100) + 1) / 2) + 7;
                }
            }
            return i;
        } else {
            if (certainty <= 80) {
                i2 = 40;
            } else {
                i2 = (((certainty - 80) + 1) / 2) + 40;
            }
            return i2;
        }
    }
}
