package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.asn1.cms.GCMParameters;
import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.BufferedBlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.OutputLengthException;
import com.android.org.bouncycastle.crypto.modes.AEADBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CBCBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CCMBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CFBBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CTSBlockCipher;
import com.android.org.bouncycastle.crypto.modes.GCMBlockCipher;
import com.android.org.bouncycastle.crypto.modes.OFBBlockCipher;
import com.android.org.bouncycastle.crypto.modes.SICBlockCipher;
import com.android.org.bouncycastle.crypto.paddings.BlockCipherPadding;
import com.android.org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import com.android.org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import com.android.org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import com.android.org.bouncycastle.crypto.paddings.TBCPadding;
import com.android.org.bouncycastle.crypto.paddings.X923Padding;
import com.android.org.bouncycastle.crypto.paddings.ZeroBytePadding;
import com.android.org.bouncycastle.crypto.params.AEADParameters;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.util.Strings;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;

public class BaseBlockCipher extends BaseWrapCipher implements PBE {
    private static final Class gcmSpecClass = lookup("javax.crypto.spec.GCMParameterSpec");
    private AEADParameters aeadParams;
    private Class[] availableSpecs;
    private BlockCipher baseEngine;
    private GenericBlockCipher cipher;
    private int digest;
    private BlockCipherProvider engineProvider;
    private boolean fixedIv;
    private int ivLength;
    private ParametersWithIV ivParam;
    private int keySizeInBits;
    private String modeName;
    private boolean padded;
    private String pbeAlgorithm;
    private PBEParameterSpec pbeSpec;
    private int scheme;

    private interface GenericBlockCipher {
        int doFinal(byte[] bArr, int i) throws IllegalStateException, BadPaddingException;

        String getAlgorithmName();

        int getOutputSize(int i);

        BlockCipher getUnderlyingCipher();

        int getUpdateOutputSize(int i);

        void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException;

        int processByte(byte b, byte[] bArr, int i) throws DataLengthException;

        int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException;

        void updateAAD(byte[] bArr, int i, int i2);

        boolean wrapOnNoPadding();
    }

    private static class AEADGenericBlockCipher implements GenericBlockCipher {
        private static final Constructor aeadBadTagConstructor;
        private AEADBlockCipher cipher;

        static {
            Class aeadBadTagClass = BaseBlockCipher.lookup("javax.crypto.AEADBadTagException");
            if (aeadBadTagClass != null) {
                aeadBadTagConstructor = findExceptionConstructor(aeadBadTagClass);
            } else {
                aeadBadTagConstructor = null;
            }
        }

        private static Constructor findExceptionConstructor(Class clazz) {
            try {
                return clazz.getConstructor(new Class[]{String.class});
            } catch (Exception e) {
                return null;
            }
        }

        AEADGenericBlockCipher(AEADBlockCipher cipher) {
            this.cipher = cipher;
        }

        public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
            this.cipher.init(forEncryption, params);
        }

        public String getAlgorithmName() {
            return this.cipher.getUnderlyingCipher().getAlgorithmName();
        }

        public boolean wrapOnNoPadding() {
            return false;
        }

        public BlockCipher getUnderlyingCipher() {
            return this.cipher.getUnderlyingCipher();
        }

        public int getOutputSize(int len) {
            return this.cipher.getOutputSize(len);
        }

        public int getUpdateOutputSize(int len) {
            return this.cipher.getUpdateOutputSize(len);
        }

        public void updateAAD(byte[] input, int offset, int length) {
            this.cipher.processAADBytes(input, offset, length);
        }

        public int processByte(byte in, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processByte(in, out, outOff);
        }

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processBytes(in, inOff, len, out, outOff);
        }

        public int doFinal(byte[] out, int outOff) throws IllegalStateException, BadPaddingException {
            try {
                return this.cipher.doFinal(out, outOff);
            } catch (InvalidCipherTextException e) {
                if (aeadBadTagConstructor != null) {
                    BadPaddingException aeadBadTag = null;
                    try {
                        aeadBadTag = (BadPaddingException) aeadBadTagConstructor.newInstance(new Object[]{e.getMessage()});
                    } catch (Exception e2) {
                    }
                    if (aeadBadTag != null) {
                        throw aeadBadTag;
                    }
                }
                throw new BadPaddingException(e.getMessage());
            }
        }
    }

    private static class BufferedGenericBlockCipher implements GenericBlockCipher {
        private BufferedBlockCipher cipher;

        BufferedGenericBlockCipher(BufferedBlockCipher cipher) {
            this.cipher = cipher;
        }

        BufferedGenericBlockCipher(BlockCipher cipher) {
            this.cipher = new PaddedBufferedBlockCipher(cipher);
        }

        BufferedGenericBlockCipher(BlockCipher cipher, BlockCipherPadding padding) {
            this.cipher = new PaddedBufferedBlockCipher(cipher, padding);
        }

        public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
            this.cipher.init(forEncryption, params);
        }

        public boolean wrapOnNoPadding() {
            return (this.cipher instanceof CTSBlockCipher) ^ 1;
        }

        public String getAlgorithmName() {
            return this.cipher.getUnderlyingCipher().getAlgorithmName();
        }

        public BlockCipher getUnderlyingCipher() {
            return this.cipher.getUnderlyingCipher();
        }

        public int getOutputSize(int len) {
            return this.cipher.getOutputSize(len);
        }

        public int getUpdateOutputSize(int len) {
            return this.cipher.getUpdateOutputSize(len);
        }

        public void updateAAD(byte[] input, int offset, int length) {
            throw new UnsupportedOperationException("AAD is not supported in the current mode.");
        }

        public int processByte(byte in, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processByte(in, out, outOff);
        }

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processBytes(in, inOff, len, out, outOff);
        }

        public int doFinal(byte[] out, int outOff) throws IllegalStateException, BadPaddingException {
            try {
                return this.cipher.doFinal(out, outOff);
            } catch (InvalidCipherTextException e) {
                throw new BadPaddingException(e.getMessage());
            }
        }
    }

    private static class InvalidKeyOrParametersException extends InvalidKeyException {
        private final Throwable cause;

        InvalidKeyOrParametersException(String msg, Throwable cause) {
            super(msg);
            this.cause = cause;
        }

        public Throwable getCause() {
            return this.cause;
        }
    }

    private static Class lookup(String className) {
        try {
            return BaseBlockCipher.class.getClassLoader().loadClass(className);
        } catch (Exception e) {
            return null;
        }
    }

    protected BaseBlockCipher(BlockCipher engine) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine;
        this.cipher = new BufferedGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(BlockCipher engine, int scheme, int digest, int keySizeInBits, int ivLength) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine;
        this.scheme = scheme;
        this.digest = digest;
        this.keySizeInBits = keySizeInBits;
        this.ivLength = ivLength;
        this.cipher = new BufferedGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(BlockCipherProvider provider) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = provider.get();
        this.engineProvider = provider;
        this.cipher = new BufferedGenericBlockCipher(provider.get());
    }

    protected BaseBlockCipher(AEADBlockCipher engine) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine.getUnderlyingCipher();
        this.ivLength = this.baseEngine.getBlockSize();
        this.cipher = new AEADGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(AEADBlockCipher engine, boolean fixedIv, int ivLength) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine.getUnderlyingCipher();
        this.fixedIv = fixedIv;
        this.ivLength = ivLength;
        this.cipher = new AEADGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(BlockCipher engine, int ivLength) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine;
        this.cipher = new BufferedGenericBlockCipher(engine);
        this.ivLength = ivLength / 8;
    }

    protected BaseBlockCipher(BufferedBlockCipher engine, int ivLength) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine.getUnderlyingCipher();
        this.cipher = new BufferedGenericBlockCipher(engine);
        this.ivLength = ivLength / 8;
    }

    protected int engineGetBlockSize() {
        return this.baseEngine.getBlockSize();
    }

    protected byte[] engineGetIV() {
        byte[] bArr = null;
        if (this.aeadParams != null) {
            return this.aeadParams.getNonce();
        }
        if (this.ivParam != null) {
            bArr = this.ivParam.getIV();
        }
        return bArr;
    }

    protected int engineGetKeySize(Key key) {
        return key.getEncoded().length * 8;
    }

    protected int engineGetOutputSize(int inputLen) {
        return this.cipher.getOutputSize(inputLen);
    }

    protected AlgorithmParameters engineGetParameters() {
        if (this.engineParams == null) {
            if (this.pbeSpec != null) {
                try {
                    this.engineParams = createParametersInstance(this.pbeAlgorithm);
                    this.engineParams.init(this.pbeSpec);
                } catch (Exception e) {
                    return null;
                }
            } else if (this.aeadParams != null) {
                try {
                    this.engineParams = createParametersInstance("GCM");
                    this.engineParams.init(new GCMParameters(this.aeadParams.getNonce(), this.aeadParams.getMacSize() / 8).getEncoded());
                } catch (Exception e2) {
                    throw new RuntimeException(e2.toString());
                }
            } else if (this.ivParam != null) {
                String name = this.cipher.getUnderlyingCipher().getAlgorithmName();
                if (name.indexOf(47) >= 0) {
                    name = name.substring(0, name.indexOf(47));
                }
                try {
                    this.engineParams = createParametersInstance(name);
                    this.engineParams.init(this.ivParam.getIV());
                } catch (Exception e22) {
                    throw new RuntimeException(e22.toString());
                }
            }
        }
        return this.engineParams;
    }

    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        this.modeName = Strings.toUpperCase(mode);
        if (this.modeName.equals("ECB")) {
            this.ivLength = 0;
            this.cipher = new BufferedGenericBlockCipher(this.baseEngine);
        } else if (this.modeName.equals("CBC")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new BufferedGenericBlockCipher(new CBCBlockCipher(this.baseEngine));
        } else if (this.modeName.startsWith("OFB")) {
            this.ivLength = this.baseEngine.getBlockSize();
            if (this.modeName.length() != 3) {
                this.cipher = new BufferedGenericBlockCipher(new OFBBlockCipher(this.baseEngine, Integer.parseInt(this.modeName.substring(3))));
                return;
            }
            this.cipher = new BufferedGenericBlockCipher(new OFBBlockCipher(this.baseEngine, this.baseEngine.getBlockSize() * 8));
        } else if (this.modeName.startsWith("CFB")) {
            this.ivLength = this.baseEngine.getBlockSize();
            if (this.modeName.length() != 3) {
                this.cipher = new BufferedGenericBlockCipher(new CFBBlockCipher(this.baseEngine, Integer.parseInt(this.modeName.substring(3))));
                return;
            }
            this.cipher = new BufferedGenericBlockCipher(new CFBBlockCipher(this.baseEngine, this.baseEngine.getBlockSize() * 8));
        } else if (this.modeName.startsWith("CTR")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.fixedIv = false;
            this.cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(new SICBlockCipher(this.baseEngine)));
        } else if (this.modeName.startsWith("CTS")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(new CBCBlockCipher(this.baseEngine)));
        } else if (this.modeName.startsWith("CCM")) {
            this.ivLength = 13;
            this.cipher = new AEADGenericBlockCipher(new CCMBlockCipher(this.baseEngine));
        } else if (this.modeName.startsWith("GCM")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new AEADGenericBlockCipher(new GCMBlockCipher(this.baseEngine));
        } else {
            throw new NoSuchAlgorithmException("can't support mode " + mode);
        }
    }

    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        String paddingName = Strings.toUpperCase(padding);
        if (paddingName.equals("NOPADDING")) {
            if (this.cipher.wrapOnNoPadding()) {
                this.cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(this.cipher.getUnderlyingCipher()));
            }
        } else if (paddingName.equals("WITHCTS")) {
            this.cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(this.cipher.getUnderlyingCipher()));
        } else {
            this.padded = true;
            if (isAEADModeName(this.modeName)) {
                throw new NoSuchPaddingException("Only NoPadding can be used with AEAD modes.");
            } else if (paddingName.equals("PKCS5PADDING") || paddingName.equals("PKCS7PADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher());
            } else if (paddingName.equals("ZEROBYTEPADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ZeroBytePadding());
            } else if (paddingName.equals("ISO10126PADDING") || paddingName.equals("ISO10126-2PADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO10126d2Padding());
            } else if (paddingName.equals("X9.23PADDING") || paddingName.equals("X923PADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new X923Padding());
            } else if (paddingName.equals("ISO7816-4PADDING") || paddingName.equals("ISO9797-1PADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO7816d4Padding());
            } else if (paddingName.equals("TBCPADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new TBCPadding());
            } else {
                throw new NoSuchPaddingException("Padding " + padding + " unknown.");
            }
        }
    }

    private boolean isBCPBEKeyWithoutIV(Key key) {
        return key instanceof BCPBEKey ? (((BCPBEKey) key).getParam() instanceof ParametersWithIV) ^ 1 : false;
    }

    protected void engineInit(int r32, java.security.Key r33, java.security.spec.AlgorithmParameterSpec r34, java.security.SecureRandom r35) throws java.security.InvalidKeyException, java.security.InvalidAlgorithmParameterException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r23_21 'param' com.android.org.bouncycastle.crypto.CipherParameters) in PHI: PHI: (r23_3 'param' com.android.org.bouncycastle.crypto.CipherParameters) = (r23_1 'param' com.android.org.bouncycastle.crypto.CipherParameters), (r23_1 'param' com.android.org.bouncycastle.crypto.CipherParameters), (r23_18 'param' com.android.org.bouncycastle.crypto.CipherParameters), (r23_18 'param' com.android.org.bouncycastle.crypto.CipherParameters), (r23_20 'param' com.android.org.bouncycastle.crypto.CipherParameters), (r23_20 'param' com.android.org.bouncycastle.crypto.CipherParameters), (r23_21 'param' com.android.org.bouncycastle.crypto.CipherParameters) binds: {(r23_1 'param' com.android.org.bouncycastle.crypto.CipherParameters)=B:48:0x00f7, (r23_1 'param' com.android.org.bouncycastle.crypto.CipherParameters)=B:49:0x00f9, (r23_18 'param' com.android.org.bouncycastle.crypto.CipherParameters)=B:73:0x0192, (r23_18 'param' com.android.org.bouncycastle.crypto.CipherParameters)=B:74:0x0194, (r23_20 'param' com.android.org.bouncycastle.crypto.CipherParameters)=B:95:0x0288, (r23_20 'param' com.android.org.bouncycastle.crypto.CipherParameters)=B:96:0x028a, (r23_21 'param' com.android.org.bouncycastle.crypto.CipherParameters)=B:107:0x02b8}
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
        r31 = this;
        r4 = 0;
        r0 = r31;
        r0.pbeSpec = r4;
        r4 = 0;
        r0 = r31;
        r0.pbeAlgorithm = r4;
        r4 = 0;
        r0 = r31;
        r0.engineParams = r4;
        r4 = 0;
        r0 = r31;
        r0.aeadParams = r4;
        r0 = r33;
        r4 = r0 instanceof javax.crypto.SecretKey;
        if (r4 != 0) goto L_0x0043;
    L_0x001a:
        r5 = new java.security.InvalidKeyException;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r6 = "Key for algorithm ";
        r6 = r4.append(r6);
        if (r33 == 0) goto L_0x0041;
    L_0x002a:
        r4 = r33.getAlgorithm();
    L_0x002e:
        r4 = r6.append(r4);
        r6 = " not suitable for symmetric enryption.";
        r4 = r4.append(r6);
        r4 = r4.toString();
        r5.<init>(r4);
        throw r5;
    L_0x0041:
        r4 = 0;
        goto L_0x002e;
    L_0x0043:
        if (r34 != 0) goto L_0x005f;
    L_0x0045:
        r0 = r31;
        r4 = r0.baseEngine;
        r4 = r4.getAlgorithmName();
        r5 = "RC5-64";
        r4 = r4.startsWith(r5);
        if (r4 == 0) goto L_0x005f;
    L_0x0056:
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = "RC5 requires an RC5ParametersSpec to be passed in.";
        r4.<init>(r5);
        throw r4;
    L_0x005f:
        r0 = r31;
        r4 = r0.scheme;
        r5 = 2;
        if (r4 == r5) goto L_0x006c;
    L_0x0066:
        r0 = r33;
        r4 = r0 instanceof com.android.org.bouncycastle.jcajce.PKCS12Key;
        if (r4 == 0) goto L_0x015e;
    L_0x006c:
        r0 = r31;
        r1 = r33;
        r4 = r0.isBCPBEKeyWithoutIV(r1);
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x015e;
    L_0x0078:
        r0 = r33;	 Catch:{ Exception -> 0x00ab }
        r0 = (javax.crypto.SecretKey) r0;	 Catch:{ Exception -> 0x00ab }
        r19 = r0;	 Catch:{ Exception -> 0x00ab }
        r0 = r34;
        r4 = r0 instanceof javax.crypto.spec.PBEParameterSpec;
        if (r4 == 0) goto L_0x008c;
    L_0x0084:
        r4 = r34;
        r4 = (javax.crypto.spec.PBEParameterSpec) r4;
        r0 = r31;
        r0.pbeSpec = r4;
    L_0x008c:
        r0 = r19;
        r4 = r0 instanceof javax.crypto.interfaces.PBEKey;
        if (r4 == 0) goto L_0x00c6;
    L_0x0092:
        r0 = r31;
        r4 = r0.pbeSpec;
        if (r4 != 0) goto L_0x00c6;
    L_0x0098:
        r25 = r19;
        r25 = (javax.crypto.interfaces.PBEKey) r25;
        r4 = r25.getSalt();
        if (r4 != 0) goto L_0x00b5;
    L_0x00a2:
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = "PBEKey requires parameters to specify salt";
        r4.<init>(r5);
        throw r4;
    L_0x00ab:
        r14 = move-exception;
        r4 = new java.security.InvalidKeyException;
        r5 = "PKCS12 requires a SecretKey/PBEKey";
        r4.<init>(r5);
        throw r4;
    L_0x00b5:
        r4 = new javax.crypto.spec.PBEParameterSpec;
        r5 = r25.getSalt();
        r6 = r25.getIterationCount();
        r4.<init>(r5, r6);
        r0 = r31;
        r0.pbeSpec = r4;
    L_0x00c6:
        r0 = r31;
        r4 = r0.pbeSpec;
        if (r4 != 0) goto L_0x00dd;
    L_0x00cc:
        r0 = r19;
        r4 = r0 instanceof javax.crypto.interfaces.PBEKey;
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x00dd;
    L_0x00d4:
        r4 = new java.security.InvalidKeyException;
        r5 = "Algorithm requires a PBE key";
        r4.<init>(r5);
        throw r4;
    L_0x00dd:
        r0 = r33;
        r4 = r0 instanceof com.android.org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey;
        if (r4 == 0) goto L_0x013a;
    L_0x00e3:
        r4 = r33;
        r4 = (com.android.org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey) r4;
        r26 = r4.getParam();
        r0 = r26;
        r4 = r0 instanceof com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        if (r4 == 0) goto L_0x0126;
    L_0x00f1:
        r23 = r26;
    L_0x00f3:
        r0 = r23;
        r4 = r0 instanceof com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        if (r4 == 0) goto L_0x0101;
    L_0x00f9:
        r4 = r23;
        r4 = (com.android.org.bouncycastle.crypto.params.ParametersWithIV) r4;
        r0 = r31;
        r0.ivParam = r4;
    L_0x0101:
        r0 = r34;
        r4 = r0 instanceof com.android.org.bouncycastle.jcajce.spec.AEADParameterSpec;
        if (r4 == 0) goto L_0x0391;
    L_0x0107:
        r0 = r31;
        r4 = r0.modeName;
        r0 = r31;
        r4 = r0.isAEADModeName(r4);
        if (r4 != 0) goto L_0x02c5;
    L_0x0113:
        r0 = r31;
        r4 = r0.cipher;
        r4 = r4 instanceof com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.AEADGenericBlockCipher;
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x02c5;
    L_0x011d:
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = "AEADParameterSpec can only be used with AEAD modes.";
        r4.<init>(r5);
        throw r4;
    L_0x0126:
        if (r26 != 0) goto L_0x0131;
    L_0x0128:
        r4 = new java.lang.IllegalStateException;
        r5 = "Unreachable code";
        r4.<init>(r5);
        throw r4;
    L_0x0131:
        r4 = new java.security.InvalidKeyException;
        r5 = "Algorithm requires a PBE key suitable for PKCS12";
        r4.<init>(r5);
        throw r4;
    L_0x013a:
        r3 = r19.getEncoded();
        r0 = r31;
        r5 = r0.digest;
        r0 = r31;
        r6 = r0.keySizeInBits;
        r0 = r31;
        r4 = r0.ivLength;
        r7 = r4 * 8;
        r0 = r31;
        r8 = r0.pbeSpec;
        r0 = r31;
        r4 = r0.cipher;
        r9 = r4.getAlgorithmName();
        r4 = 2;
        r23 = com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE.Util.makePBEParameters(r3, r4, r5, r6, r7, r8, r9);
        goto L_0x00f3;
    L_0x015e:
        r0 = r33;
        r4 = r0 instanceof com.android.org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey;
        if (r4 == 0) goto L_0x022f;
    L_0x0164:
        r18 = r33;
        r18 = (com.android.org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey) r18;
        r4 = r18.getOID();
        if (r4 == 0) goto L_0x019e;
    L_0x016e:
        r4 = r18.getOID();
        r4 = r4.getId();
        r0 = r31;
        r0.pbeAlgorithm = r4;
    L_0x017a:
        r4 = r18.getParam();
        if (r4 == 0) goto L_0x01a7;
    L_0x0180:
        r4 = r18.getParam();
        r0 = r31;
        r1 = r34;
        r23 = r0.adjustParameters(r1, r4);
        r3 = r18;
    L_0x018e:
        r0 = r23;
        r4 = r0 instanceof com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        if (r4 == 0) goto L_0x0101;
    L_0x0194:
        r4 = r23;
        r4 = (com.android.org.bouncycastle.crypto.params.ParametersWithIV) r4;
        r0 = r31;
        r0.ivParam = r4;
        goto L_0x0101;
    L_0x019e:
        r4 = r18.getAlgorithm();
        r0 = r31;
        r0.pbeAlgorithm = r4;
        goto L_0x017a;
    L_0x01a7:
        r0 = r34;
        r4 = r0 instanceof javax.crypto.spec.PBEParameterSpec;
        if (r4 == 0) goto L_0x0226;
    L_0x01ad:
        r4 = r34;
        r4 = (javax.crypto.spec.PBEParameterSpec) r4;
        r0 = r31;
        r0.pbeSpec = r4;
        r0 = r31;
        r4 = r0.pbeSpec;
        r4 = r4.getSalt();
        r4 = r4.length;
        if (r4 == 0) goto L_0x0223;
    L_0x01c0:
        r0 = r31;
        r4 = r0.pbeSpec;
        r4 = r4.getIterationCount();
        if (r4 <= 0) goto L_0x0223;
    L_0x01ca:
        r3 = new com.android.org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey;
        r4 = r18.getAlgorithm();
        r5 = r18.getOID();
        r6 = r18.getType();
        r7 = r18.getDigest();
        r8 = r18.getKeySize();
        r9 = r18.getIvSize();
        r10 = new javax.crypto.spec.PBEKeySpec;
        r11 = r18.getPassword();
        r0 = r31;
        r0 = r0.pbeSpec;
        r28 = r0;
        r28 = r28.getSalt();
        r0 = r31;
        r0 = r0.pbeSpec;
        r29 = r0;
        r29 = r29.getIterationCount();
        r30 = r18.getKeySize();
        r0 = r28;
        r1 = r29;
        r2 = r30;
        r10.<init>(r11, r0, r1, r2);
        r11 = 0;
        r3.<init>(r4, r5, r6, r7, r8, r9, r10, r11);
    L_0x020f:
        r0 = r31;
        r4 = r0.cipher;
        r4 = r4.getUnderlyingCipher();
        r4 = r4.getAlgorithmName();
        r0 = r34;
        r23 = com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE.Util.makePBEParameters(r3, r0, r4);
        goto L_0x018e;
    L_0x0223:
        r3 = r18;
        goto L_0x020f;
    L_0x0226:
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = "PBE requires PBE parameters to be set.";
        r4.<init>(r5);
        throw r4;
    L_0x022f:
        r0 = r33;
        r4 = r0 instanceof javax.crypto.interfaces.PBEKey;
        if (r4 == 0) goto L_0x0294;
    L_0x0235:
        r20 = r33;
        r20 = (javax.crypto.interfaces.PBEKey) r20;
        r4 = r34;
        r4 = (javax.crypto.spec.PBEParameterSpec) r4;
        r0 = r31;
        r0.pbeSpec = r4;
        r0 = r20;
        r4 = r0 instanceof com.android.org.bouncycastle.jcajce.PKCS12KeyWithParameters;
        if (r4 == 0) goto L_0x025e;
    L_0x0247:
        r0 = r31;
        r4 = r0.pbeSpec;
        if (r4 != 0) goto L_0x025e;
    L_0x024d:
        r4 = new javax.crypto.spec.PBEParameterSpec;
        r5 = r20.getSalt();
        r6 = r20.getIterationCount();
        r4.<init>(r5, r6);
        r0 = r31;
        r0.pbeSpec = r4;
    L_0x025e:
        r4 = r20.getEncoded();
        r0 = r31;
        r5 = r0.scheme;
        r0 = r31;
        r6 = r0.digest;
        r0 = r31;
        r7 = r0.keySizeInBits;
        r0 = r31;
        r8 = r0.ivLength;
        r8 = r8 * 8;
        r0 = r31;
        r9 = r0.pbeSpec;
        r0 = r31;
        r10 = r0.cipher;
        r10 = r10.getAlgorithmName();
        r23 = com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE.Util.makePBEParameters(r4, r5, r6, r7, r8, r9, r10);
        r0 = r23;
        r4 = r0 instanceof com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        if (r4 == 0) goto L_0x0101;
    L_0x028a:
        r4 = r23;
        r4 = (com.android.org.bouncycastle.crypto.params.ParametersWithIV) r4;
        r0 = r31;
        r0.ivParam = r4;
        goto L_0x0101;
    L_0x0294:
        r0 = r31;
        r4 = r0.scheme;
        if (r4 == 0) goto L_0x02a1;
    L_0x029a:
        r0 = r31;
        r4 = r0.scheme;
        r5 = 4;
        if (r4 != r5) goto L_0x02aa;
    L_0x02a1:
        r4 = new java.security.InvalidKeyException;
        r5 = "Algorithm requires a PBE key";
        r4.<init>(r5);
        throw r4;
    L_0x02aa:
        r0 = r31;
        r4 = r0.scheme;
        r5 = 1;
        if (r4 == r5) goto L_0x02a1;
    L_0x02b1:
        r0 = r31;
        r4 = r0.scheme;
        r5 = 5;
        if (r4 == r5) goto L_0x02a1;
    L_0x02b8:
        r23 = new com.android.org.bouncycastle.crypto.params.KeyParameter;
        r4 = r33.getEncoded();
        r0 = r23;
        r0.<init>(r4);
        goto L_0x0101;
    L_0x02c5:
        r13 = r34;
        r13 = (com.android.org.bouncycastle.jcajce.spec.AEADParameterSpec) r13;
        r0 = r23;
        r4 = r0 instanceof com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        if (r4 == 0) goto L_0x038b;
    L_0x02cf:
        r23 = (com.android.org.bouncycastle.crypto.params.ParametersWithIV) r23;
        r21 = r23.getParameters();
        r21 = (com.android.org.bouncycastle.crypto.params.KeyParameter) r21;
    L_0x02d7:
        r23 = new com.android.org.bouncycastle.crypto.params.AEADParameters;
        r4 = r13.getMacSizeInBits();
        r5 = r13.getNonce();
        r6 = r13.getAssociatedData();
        r0 = r23;
        r1 = r21;
        r0.<init>(r1, r4, r5, r6);
        r0 = r23;
        r1 = r31;
        r1.aeadParams = r0;
    L_0x02f2:
        r0 = r31;
        r4 = r0.ivLength;
        if (r4 == 0) goto L_0x04d4;
    L_0x02f8:
        r0 = r23;
        r4 = r0 instanceof com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x04d4;
    L_0x0300:
        r0 = r23;
        r4 = r0 instanceof com.android.org.bouncycastle.crypto.params.AEADParameters;
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x0571;
    L_0x0308:
        r17 = r35;
        if (r35 != 0) goto L_0x0311;
    L_0x030c:
        r17 = new java.security.SecureRandom;
        r17.<init>();
    L_0x0311:
        r4 = 1;
        r0 = r32;
        if (r0 == r4) goto L_0x031b;
    L_0x0316:
        r4 = 3;
        r0 = r32;
        if (r0 != r4) goto L_0x04d8;
    L_0x031b:
        r0 = r31;
        r4 = r0.ivLength;
        r0 = new byte[r4];
        r16 = r0;
        r0 = r31;
        r1 = r33;
        r4 = r0.isBCPBEKeyWithoutIV(r1);
        if (r4 != 0) goto L_0x0334;
    L_0x032d:
        r0 = r17;
        r1 = r16;
        r0.nextBytes(r1);
    L_0x0334:
        r24 = new com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        r0 = r24;
        r1 = r23;
        r2 = r16;
        r0.<init>(r1, r2);
        r4 = r24;
        r4 = (com.android.org.bouncycastle.crypto.params.ParametersWithIV) r4;
        r0 = r31;
        r0.ivParam = r4;
    L_0x0347:
        if (r35 == 0) goto L_0x0519;
    L_0x0349:
        r0 = r31;
        r4 = r0.padded;
        if (r4 == 0) goto L_0x056d;
    L_0x034f:
        r23 = new com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
        r0 = r23;
        r1 = r24;
        r2 = r35;
        r0.<init>(r1, r2);
    L_0x035a:
        switch(r32) {
            case 1: goto L_0x051d;
            case 2: goto L_0x0562;
            case 3: goto L_0x051d;
            case 4: goto L_0x0562;
            default: goto L_0x035d;
        };
    L_0x035d:
        r4 = new java.security.InvalidParameterException;	 Catch:{ Exception -> 0x0380 }
        r5 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0380 }
        r5.<init>();	 Catch:{ Exception -> 0x0380 }
        r6 = "unknown opmode ";	 Catch:{ Exception -> 0x0380 }
        r5 = r5.append(r6);	 Catch:{ Exception -> 0x0380 }
        r0 = r32;	 Catch:{ Exception -> 0x0380 }
        r5 = r5.append(r0);	 Catch:{ Exception -> 0x0380 }
        r6 = " passed";	 Catch:{ Exception -> 0x0380 }
        r5 = r5.append(r6);	 Catch:{ Exception -> 0x0380 }
        r5 = r5.toString();	 Catch:{ Exception -> 0x0380 }
        r4.<init>(r5);	 Catch:{ Exception -> 0x0380 }
        throw r4;	 Catch:{ Exception -> 0x0380 }
    L_0x0380:
        r14 = move-exception;
        r4 = new com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$InvalidKeyOrParametersException;
        r5 = r14.getMessage();
        r4.<init>(r5, r14);
        throw r4;
    L_0x038b:
        r21 = r23;
        r21 = (com.android.org.bouncycastle.crypto.params.KeyParameter) r21;
        goto L_0x02d7;
    L_0x0391:
        r0 = r34;
        r4 = r0 instanceof javax.crypto.spec.IvParameterSpec;
        if (r4 == 0) goto L_0x0430;
    L_0x0397:
        r0 = r31;
        r4 = r0.ivLength;
        if (r4 == 0) goto L_0x0414;
    L_0x039d:
        r22 = r34;
        r22 = (javax.crypto.spec.IvParameterSpec) r22;
        r4 = r22.getIV();
        r4 = r4.length;
        r0 = r31;
        r5 = r0.ivLength;
        if (r4 == r5) goto L_0x03e1;
    L_0x03ac:
        r0 = r31;
        r4 = r0.cipher;
        r4 = r4 instanceof com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.AEADGenericBlockCipher;
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x03e1;
    L_0x03b6:
        r0 = r31;
        r4 = r0.fixedIv;
        if (r4 == 0) goto L_0x03e1;
    L_0x03bc:
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "IV must be ";
        r5 = r5.append(r6);
        r0 = r31;
        r6 = r0.ivLength;
        r5 = r5.append(r6);
        r6 = " bytes long.";
        r5 = r5.append(r6);
        r5 = r5.toString();
        r4.<init>(r5);
        throw r4;
    L_0x03e1:
        r0 = r23;
        r4 = r0 instanceof com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        if (r4 == 0) goto L_0x0404;
    L_0x03e7:
        r24 = new com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        r23 = (com.android.org.bouncycastle.crypto.params.ParametersWithIV) r23;
        r4 = r23.getParameters();
        r5 = r22.getIV();
        r0 = r24;
        r0.<init>(r4, r5);
        r23 = r24;
    L_0x03fa:
        r4 = r23;
        r4 = (com.android.org.bouncycastle.crypto.params.ParametersWithIV) r4;
        r0 = r31;
        r0.ivParam = r4;
        goto L_0x02f2;
    L_0x0404:
        r24 = new com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        r4 = r22.getIV();
        r0 = r24;
        r1 = r23;
        r0.<init>(r1, r4);
        r23 = r24;
        goto L_0x03fa;
    L_0x0414:
        r0 = r31;
        r4 = r0.modeName;
        if (r4 == 0) goto L_0x02f2;
    L_0x041a:
        r0 = r31;
        r4 = r0.modeName;
        r5 = "ECB";
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x02f2;
    L_0x0427:
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = "ECB mode does not use an IV";
        r4.<init>(r5);
        throw r4;
    L_0x0430:
        r4 = gcmSpecClass;
        if (r4 == 0) goto L_0x04c1;
    L_0x0434:
        r4 = gcmSpecClass;
        r0 = r34;
        r4 = r4.isInstance(r0);
        if (r4 == 0) goto L_0x04c1;
    L_0x043e:
        r0 = r31;
        r4 = r0.modeName;
        r0 = r31;
        r4 = r0.isAEADModeName(r4);
        if (r4 != 0) goto L_0x045d;
    L_0x044a:
        r0 = r31;
        r4 = r0.cipher;
        r4 = r4 instanceof com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.AEADGenericBlockCipher;
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x045d;
    L_0x0454:
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = "GCMParameterSpec can only be used with AEAD modes.";
        r4.<init>(r5);
        throw r4;
    L_0x045d:
        r4 = gcmSpecClass;	 Catch:{ Exception -> 0x04b7 }
        r5 = "getTLen";	 Catch:{ Exception -> 0x04b7 }
        r6 = 0;	 Catch:{ Exception -> 0x04b7 }
        r6 = new java.lang.Class[r6];	 Catch:{ Exception -> 0x04b7 }
        r27 = r4.getDeclaredMethod(r5, r6);	 Catch:{ Exception -> 0x04b7 }
        r4 = gcmSpecClass;	 Catch:{ Exception -> 0x04b7 }
        r5 = "getIV";	 Catch:{ Exception -> 0x04b7 }
        r6 = 0;	 Catch:{ Exception -> 0x04b7 }
        r6 = new java.lang.Class[r6];	 Catch:{ Exception -> 0x04b7 }
        r15 = r4.getDeclaredMethod(r5, r6);	 Catch:{ Exception -> 0x04b7 }
        r0 = r23;	 Catch:{ Exception -> 0x04b7 }
        r4 = r0 instanceof com.android.org.bouncycastle.crypto.params.ParametersWithIV;	 Catch:{ Exception -> 0x04b7 }
        if (r4 == 0) goto L_0x04b0;	 Catch:{ Exception -> 0x04b7 }
    L_0x047b:
        r23 = (com.android.org.bouncycastle.crypto.params.ParametersWithIV) r23;	 Catch:{ Exception -> 0x04b7 }
        r21 = r23.getParameters();	 Catch:{ Exception -> 0x04b7 }
        r21 = (com.android.org.bouncycastle.crypto.params.KeyParameter) r21;	 Catch:{ Exception -> 0x04b7 }
    L_0x0483:
        r23 = new com.android.org.bouncycastle.crypto.params.AEADParameters;	 Catch:{ Exception -> 0x04b7 }
        r4 = 0;	 Catch:{ Exception -> 0x04b7 }
        r4 = new java.lang.Object[r4];	 Catch:{ Exception -> 0x04b7 }
        r0 = r27;	 Catch:{ Exception -> 0x04b7 }
        r1 = r34;	 Catch:{ Exception -> 0x04b7 }
        r4 = r0.invoke(r1, r4);	 Catch:{ Exception -> 0x04b7 }
        r4 = (java.lang.Integer) r4;	 Catch:{ Exception -> 0x04b7 }
        r5 = r4.intValue();	 Catch:{ Exception -> 0x04b7 }
        r4 = 0;	 Catch:{ Exception -> 0x04b7 }
        r4 = new java.lang.Object[r4];	 Catch:{ Exception -> 0x04b7 }
        r0 = r34;	 Catch:{ Exception -> 0x04b7 }
        r4 = r15.invoke(r0, r4);	 Catch:{ Exception -> 0x04b7 }
        r4 = (byte[]) r4;	 Catch:{ Exception -> 0x04b7 }
        r0 = r23;	 Catch:{ Exception -> 0x04b7 }
        r1 = r21;	 Catch:{ Exception -> 0x04b7 }
        r0.<init>(r1, r5, r4);	 Catch:{ Exception -> 0x04b7 }
        r0 = r23;	 Catch:{ Exception -> 0x04b7 }
        r1 = r31;	 Catch:{ Exception -> 0x04b7 }
        r1.aeadParams = r0;	 Catch:{ Exception -> 0x04b7 }
        goto L_0x02f2;	 Catch:{ Exception -> 0x04b7 }
    L_0x04b0:
        r0 = r23;	 Catch:{ Exception -> 0x04b7 }
        r0 = (com.android.org.bouncycastle.crypto.params.KeyParameter) r0;	 Catch:{ Exception -> 0x04b7 }
        r21 = r0;	 Catch:{ Exception -> 0x04b7 }
        goto L_0x0483;
    L_0x04b7:
        r14 = move-exception;
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = "Cannot process GCMParameterSpec.";
        r4.<init>(r5);
        throw r4;
    L_0x04c1:
        if (r34 == 0) goto L_0x02f2;
    L_0x04c3:
        r0 = r34;
        r4 = r0 instanceof javax.crypto.spec.PBEParameterSpec;
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x02f2;
    L_0x04cb:
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = "unknown parameter type.";
        r4.<init>(r5);
        throw r4;
    L_0x04d4:
        r24 = r23;
        goto L_0x0347;
    L_0x04d8:
        r0 = r31;
        r4 = r0.cipher;
        r4 = r4.getUnderlyingCipher();
        r4 = r4.getAlgorithmName();
        r5 = "PGPCFB";
        r4 = r4.indexOf(r5);
        if (r4 >= 0) goto L_0x0571;
    L_0x04ed:
        r0 = r31;
        r1 = r33;
        r4 = r0.isBCPBEKeyWithoutIV(r1);
        if (r4 != 0) goto L_0x0500;
    L_0x04f7:
        r4 = new java.security.InvalidAlgorithmParameterException;
        r5 = "no IV set when one expected";
        r4.<init>(r5);
        throw r4;
    L_0x0500:
        r24 = new com.android.org.bouncycastle.crypto.params.ParametersWithIV;
        r0 = r31;
        r4 = r0.ivLength;
        r4 = new byte[r4];
        r0 = r24;
        r1 = r23;
        r0.<init>(r1, r4);
        r4 = r24;
        r4 = (com.android.org.bouncycastle.crypto.params.ParametersWithIV) r4;
        r0 = r31;
        r0.ivParam = r4;
        goto L_0x0347;
    L_0x0519:
        r23 = r24;
        goto L_0x035a;
    L_0x051d:
        r0 = r31;	 Catch:{ Exception -> 0x0380 }
        r4 = r0.cipher;	 Catch:{ Exception -> 0x0380 }
        r5 = 1;	 Catch:{ Exception -> 0x0380 }
        r0 = r23;	 Catch:{ Exception -> 0x0380 }
        r4.init(r5, r0);	 Catch:{ Exception -> 0x0380 }
    L_0x0527:
        r0 = r31;	 Catch:{ Exception -> 0x0380 }
        r4 = r0.cipher;	 Catch:{ Exception -> 0x0380 }
        r4 = r4 instanceof com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.AEADGenericBlockCipher;	 Catch:{ Exception -> 0x0380 }
        if (r4 == 0) goto L_0x0561;	 Catch:{ Exception -> 0x0380 }
    L_0x052f:
        r0 = r31;	 Catch:{ Exception -> 0x0380 }
        r4 = r0.aeadParams;	 Catch:{ Exception -> 0x0380 }
        if (r4 != 0) goto L_0x0561;	 Catch:{ Exception -> 0x0380 }
    L_0x0535:
        r0 = r31;	 Catch:{ Exception -> 0x0380 }
        r4 = r0.cipher;	 Catch:{ Exception -> 0x0380 }
        r4 = (com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.AEADGenericBlockCipher) r4;	 Catch:{ Exception -> 0x0380 }
        r12 = r4.cipher;	 Catch:{ Exception -> 0x0380 }
        r5 = new com.android.org.bouncycastle.crypto.params.AEADParameters;	 Catch:{ Exception -> 0x0380 }
        r0 = r31;	 Catch:{ Exception -> 0x0380 }
        r4 = r0.ivParam;	 Catch:{ Exception -> 0x0380 }
        r4 = r4.getParameters();	 Catch:{ Exception -> 0x0380 }
        r4 = (com.android.org.bouncycastle.crypto.params.KeyParameter) r4;	 Catch:{ Exception -> 0x0380 }
        r6 = r12.getMac();	 Catch:{ Exception -> 0x0380 }
        r6 = r6.length;	 Catch:{ Exception -> 0x0380 }
        r6 = r6 * 8;	 Catch:{ Exception -> 0x0380 }
        r0 = r31;	 Catch:{ Exception -> 0x0380 }
        r7 = r0.ivParam;	 Catch:{ Exception -> 0x0380 }
        r7 = r7.getIV();	 Catch:{ Exception -> 0x0380 }
        r5.<init>(r4, r6, r7);	 Catch:{ Exception -> 0x0380 }
        r0 = r31;	 Catch:{ Exception -> 0x0380 }
        r0.aeadParams = r5;	 Catch:{ Exception -> 0x0380 }
    L_0x0561:
        return;	 Catch:{ Exception -> 0x0380 }
    L_0x0562:
        r0 = r31;	 Catch:{ Exception -> 0x0380 }
        r4 = r0.cipher;	 Catch:{ Exception -> 0x0380 }
        r5 = 0;	 Catch:{ Exception -> 0x0380 }
        r0 = r23;	 Catch:{ Exception -> 0x0380 }
        r4.init(r5, r0);	 Catch:{ Exception -> 0x0380 }
        goto L_0x0527;
    L_0x056d:
        r23 = r24;
        goto L_0x035a;
    L_0x0571:
        r24 = r23;
        goto L_0x0347;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.engineInit(int, java.security.Key, java.security.spec.AlgorithmParameterSpec, java.security.SecureRandom):void");
    }

    private CipherParameters adjustParameters(AlgorithmParameterSpec params, CipherParameters param) {
        if (param instanceof ParametersWithIV) {
            CipherParameters key = ((ParametersWithIV) param).getParameters();
            if (!(params instanceof IvParameterSpec)) {
                return param;
            }
            this.ivParam = new ParametersWithIV(key, ((IvParameterSpec) params).getIV());
            return this.ivParam;
        } else if (!(params instanceof IvParameterSpec)) {
            return param;
        } else {
            this.ivParam = new ParametersWithIV(param, ((IvParameterSpec) params).getIV());
            return this.ivParam;
        }
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec paramSpec = null;
        if (params != null) {
            for (int i = 0; i != this.availableSpecs.length; i++) {
                if (this.availableSpecs[i] != null) {
                    try {
                        paramSpec = params.getParameterSpec(this.availableSpecs[i]);
                        break;
                    } catch (Exception e) {
                    }
                }
            }
            if (paramSpec == null) {
                throw new InvalidAlgorithmParameterException("can't handle parameter " + params.toString());
            }
        }
        engineInit(opmode, key, paramSpec, random);
        this.engineParams = params;
    }

    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        try {
            engineInit(opmode, key, (AlgorithmParameterSpec) null, random);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    protected void engineUpdateAAD(byte[] input, int offset, int length) {
        this.cipher.updateAAD(input, offset, length);
    }

    protected void engineUpdateAAD(ByteBuffer bytebuffer) {
        engineUpdateAAD(bytebuffer.array(), bytebuffer.arrayOffset() + bytebuffer.position(), bytebuffer.limit() - bytebuffer.position());
    }

    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        int length = this.cipher.getUpdateOutputSize(inputLen);
        if (length > 0) {
            byte[] out = new byte[length];
            int len = this.cipher.processBytes(input, inputOffset, inputLen, out, 0);
            if (len == 0) {
                return null;
            }
            if (len == out.length) {
                return out;
            }
            byte[] tmp = new byte[len];
            System.arraycopy(out, 0, tmp, 0, len);
            return tmp;
        }
        this.cipher.processBytes(input, inputOffset, inputLen, null, 0);
        return null;
    }

    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        if (this.cipher.getUpdateOutputSize(inputLen) + outputOffset > output.length) {
            throw new ShortBufferException("output buffer too short for input.");
        }
        try {
            return this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
        } catch (DataLengthException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException {
        int len = 0;
        byte[] tmp = new byte[engineGetOutputSize(inputLen)];
        if (inputLen != 0) {
            len = this.cipher.processBytes(input, inputOffset, inputLen, tmp, 0);
        }
        try {
            len += this.cipher.doFinal(tmp, len);
            if (len == tmp.length) {
                return tmp;
            }
            byte[] out = new byte[len];
            System.arraycopy(tmp, 0, out, 0, len);
            return out;
        } catch (DataLengthException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        }
    }

    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {
        int len = 0;
        if (engineGetOutputSize(inputLen) + outputOffset > output.length) {
            throw new ShortBufferException("output buffer too short for input.");
        }
        if (inputLen != 0) {
            try {
                len = this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
            } catch (OutputLengthException e) {
                throw new IllegalBlockSizeException(e.getMessage());
            } catch (DataLengthException e2) {
                throw new IllegalBlockSizeException(e2.getMessage());
            }
        }
        return this.cipher.doFinal(output, outputOffset + len) + len;
    }

    private boolean isAEADModeName(String modeName) {
        return !"CCM".equals(modeName) ? "GCM".equals(modeName) : true;
    }
}
