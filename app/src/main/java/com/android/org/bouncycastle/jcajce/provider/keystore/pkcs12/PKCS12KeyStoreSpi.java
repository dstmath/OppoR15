package com.android.org.bouncycastle.jcajce.provider.keystore.pkcs12;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1Set;
import com.android.org.bouncycastle.asn1.DERBMPString;
import com.android.org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import com.android.org.bouncycastle.asn1.ntt.NTTObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.AuthenticatedSafe;
import com.android.org.bouncycastle.asn1.pkcs.CertBag;
import com.android.org.bouncycastle.asn1.pkcs.ContentInfo;
import com.android.org.bouncycastle.asn1.pkcs.EncryptedData;
import com.android.org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import com.android.org.bouncycastle.asn1.pkcs.MacData;
import com.android.org.bouncycastle.asn1.pkcs.PBES2Parameters;
import com.android.org.bouncycastle.asn1.pkcs.PBKDF2Params;
import com.android.org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.Pfx;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.asn1.pkcs.SafeBag;
import com.android.org.bouncycastle.asn1.util.ASN1Dump;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import com.android.org.bouncycastle.asn1.x509.DigestInfo;
import com.android.org.bouncycastle.asn1.x509.Extension;
import com.android.org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.digests.AndroidDigestFactory;
import com.android.org.bouncycastle.jcajce.PKCS12Key;
import com.android.org.bouncycastle.jcajce.PKCS12StoreParameter;
import com.android.org.bouncycastle.jcajce.spec.PBKDF2KeySpec;
import com.android.org.bouncycastle.jcajce.util.BCJcaJceHelper;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.jce.interfaces.BCKeyStore;
import com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.jce.provider.JDKPKCS12StoreParameter;
import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Integers;
import com.android.org.bouncycastle.util.Strings;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class PKCS12KeyStoreSpi extends KeyStoreSpi implements PKCSObjectIdentifiers, X509ObjectIdentifiers, BCKeyStore {
    static final int CERTIFICATE = 1;
    static final int KEY = 2;
    static final int KEY_PRIVATE = 0;
    static final int KEY_PUBLIC = 1;
    static final int KEY_SECRET = 2;
    private static final int MIN_ITERATIONS = 1024;
    static final int NULL = 0;
    private static final int SALT_SIZE = 20;
    static final int SEALED = 4;
    static final int SECRET = 3;
    private static final DefaultSecretKeyProvider keySizeProvider = new DefaultSecretKeyProvider();
    private ASN1ObjectIdentifier certAlgorithm;
    private CertificateFactory certFact;
    private IgnoresCaseHashtable certs = new IgnoresCaseHashtable();
    private Hashtable chainCerts = new Hashtable();
    private final JcaJceHelper helper = new BCJcaJceHelper();
    private ASN1ObjectIdentifier keyAlgorithm;
    private Hashtable keyCerts = new Hashtable();
    private IgnoresCaseHashtable keys = new IgnoresCaseHashtable();
    private Hashtable localIds = new Hashtable();
    protected SecureRandom random = new SecureRandom();

    public static class BCPKCS12KeyStore extends PKCS12KeyStoreSpi {
        public BCPKCS12KeyStore() {
            super(new BouncyCastleProvider(), pbeWithSHAAnd3_KeyTripleDES_CBC, pbeWithSHAAnd40BitRC2_CBC);
        }
    }

    private class CertId {
        byte[] id;

        CertId(PublicKey key) {
            this.id = PKCS12KeyStoreSpi.this.createSubjectKeyId(key).getKeyIdentifier();
        }

        CertId(byte[] id) {
            this.id = id;
        }

        public int hashCode() {
            return Arrays.hashCode(this.id);
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof CertId)) {
                return false;
            }
            return Arrays.areEqual(this.id, ((CertId) o).id);
        }
    }

    private static class DefaultSecretKeyProvider {
        private final Map KEY_SIZES;

        DefaultSecretKeyProvider() {
            Map keySizes = new HashMap();
            keySizes.put(new ASN1ObjectIdentifier("1.2.840.113533.7.66.10"), Integers.valueOf(128));
            keySizes.put(PKCSObjectIdentifiers.des_EDE3_CBC, Integers.valueOf(192));
            keySizes.put(NISTObjectIdentifiers.id_aes128_CBC, Integers.valueOf(128));
            keySizes.put(NISTObjectIdentifiers.id_aes192_CBC, Integers.valueOf(192));
            keySizes.put(NISTObjectIdentifiers.id_aes256_CBC, Integers.valueOf(256));
            keySizes.put(NTTObjectIdentifiers.id_camellia128_cbc, Integers.valueOf(128));
            keySizes.put(NTTObjectIdentifiers.id_camellia192_cbc, Integers.valueOf(192));
            keySizes.put(NTTObjectIdentifiers.id_camellia256_cbc, Integers.valueOf(256));
            this.KEY_SIZES = Collections.unmodifiableMap(keySizes);
        }

        public int getKeySize(AlgorithmIdentifier algorithmIdentifier) {
            Integer keySize = (Integer) this.KEY_SIZES.get(algorithmIdentifier.getAlgorithm());
            if (keySize != null) {
                return keySize.intValue();
            }
            return -1;
        }
    }

    private static class IgnoresCaseHashtable {
        private Hashtable keys;
        private Hashtable orig;

        /* synthetic */ IgnoresCaseHashtable(IgnoresCaseHashtable -this0) {
            this();
        }

        private IgnoresCaseHashtable() {
            this.orig = new Hashtable();
            this.keys = new Hashtable();
        }

        public void put(String key, Object value) {
            Object lower = key == null ? null : Strings.toLowerCase(key);
            String k = (String) this.keys.get(lower);
            if (k != null) {
                this.orig.remove(k);
            }
            this.keys.put(lower, key);
            this.orig.put(key, value);
        }

        public Enumeration keys() {
            return this.orig.keys();
        }

        public Object remove(String alias) {
            String k = (String) this.keys.remove(alias == null ? null : Strings.toLowerCase(alias));
            if (k == null) {
                return null;
            }
            return this.orig.remove(k);
        }

        public Object get(String alias) {
            String k = (String) this.keys.get(alias == null ? null : Strings.toLowerCase(alias));
            if (k == null) {
                return null;
            }
            return this.orig.get(k);
        }

        public Enumeration elements() {
            return this.orig.elements();
        }
    }

    public PKCS12KeyStoreSpi(Provider provider, ASN1ObjectIdentifier keyAlgorithm, ASN1ObjectIdentifier certAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
        this.certAlgorithm = certAlgorithm;
        if (provider != null) {
            try {
                this.certFact = CertificateFactory.getInstance("X.509", provider);
                return;
            } catch (Exception e) {
                throw new IllegalArgumentException("can't create cert factory - " + e.toString());
            }
        }
        this.certFact = CertificateFactory.getInstance("X.509");
    }

    private SubjectKeyIdentifier createSubjectKeyId(PublicKey pubKey) {
        try {
            return new SubjectKeyIdentifier(getDigest(SubjectPublicKeyInfo.getInstance(pubKey.getEncoded())));
        } catch (Exception e) {
            throw new RuntimeException("error creating key");
        }
    }

    private static byte[] getDigest(SubjectPublicKeyInfo spki) {
        Digest digest = AndroidDigestFactory.getSHA1();
        byte[] resBuf = new byte[digest.getDigestSize()];
        byte[] bytes = spki.getPublicKeyData().getBytes();
        digest.update(bytes, 0, bytes.length);
        digest.doFinal(resBuf, 0);
        return resBuf;
    }

    public void setRandom(SecureRandom rand) {
        this.random = rand;
    }

    public Enumeration engineAliases() {
        Hashtable tab = new Hashtable();
        Enumeration e = this.certs.keys();
        while (e.hasMoreElements()) {
            tab.put(e.nextElement(), "cert");
        }
        e = this.keys.keys();
        while (e.hasMoreElements()) {
            String a = (String) e.nextElement();
            if (tab.get(a) == null) {
                tab.put(a, "key");
            }
        }
        return tab.keys();
    }

    public boolean engineContainsAlias(String alias) {
        return (this.certs.get(alias) == null && this.keys.get(alias) == null) ? false : true;
    }

    public void engineDeleteEntry(String alias) throws KeyStoreException {
        Key k = (Key) this.keys.remove(alias);
        Certificate c = (Certificate) this.certs.remove(alias);
        if (c != null) {
            this.chainCerts.remove(new CertId(c.getPublicKey()));
        }
        if (k != null) {
            String id = (String) this.localIds.remove(alias);
            if (id != null) {
                c = (Certificate) this.keyCerts.remove(id);
            }
            if (c != null) {
                this.chainCerts.remove(new CertId(c.getPublicKey()));
            }
        }
    }

    public Certificate engineGetCertificate(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException("null alias passed to getCertificate.");
        }
        Certificate c = (Certificate) this.certs.get(alias);
        if (c != null) {
            return c;
        }
        String id = (String) this.localIds.get(alias);
        if (id != null) {
            return (Certificate) this.keyCerts.get(id);
        }
        return (Certificate) this.keyCerts.get(alias);
    }

    public String engineGetCertificateAlias(Certificate cert) {
        String ta;
        Enumeration c = this.certs.elements();
        Enumeration k = this.certs.keys();
        while (c.hasMoreElements()) {
            ta = (String) k.nextElement();
            if (((Certificate) c.nextElement()).equals(cert)) {
                return ta;
            }
        }
        c = this.keyCerts.elements();
        k = this.keyCerts.keys();
        while (c.hasMoreElements()) {
            ta = (String) k.nextElement();
            if (((Certificate) c.nextElement()).equals(cert)) {
                return ta;
            }
        }
        return null;
    }

    public Certificate[] engineGetCertificateChain(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException("null alias passed to getCertificateChain.");
        } else if (!engineIsKeyEntry(alias)) {
            return null;
        } else {
            Certificate c = engineGetCertificate(alias);
            if (c == null) {
                return null;
            }
            Vector cs = new Vector();
            while (c != null) {
                X509Certificate x509c = (X509Certificate) c;
                Certificate certificate = null;
                byte[] bytes = x509c.getExtensionValue(Extension.authorityKeyIdentifier.getId());
                if (bytes != null) {
                    try {
                        AuthorityKeyIdentifier id = AuthorityKeyIdentifier.getInstance(new ASN1InputStream(((ASN1OctetString) new ASN1InputStream(bytes).readObject()).getOctets()).readObject());
                        if (id.getKeyIdentifier() != null) {
                            certificate = (Certificate) this.chainCerts.get(new CertId(id.getKeyIdentifier()));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e.toString());
                    }
                }
                if (certificate == null) {
                    Principal i = x509c.getIssuerDN();
                    if (!i.equals(x509c.getSubjectDN())) {
                        Enumeration e2 = this.chainCerts.keys();
                        while (e2.hasMoreElements()) {
                            Certificate crt = (X509Certificate) this.chainCerts.get(e2.nextElement());
                            if (crt.getSubjectDN().equals(i)) {
                                try {
                                    x509c.verify(crt.getPublicKey());
                                    certificate = crt;
                                    break;
                                } catch (Exception e3) {
                                }
                            }
                        }
                    }
                }
                if (cs.contains(c)) {
                    c = null;
                } else {
                    cs.addElement(c);
                    if (certificate != c) {
                        c = certificate;
                    } else {
                        c = null;
                    }
                }
            }
            Certificate[] certChain = new Certificate[cs.size()];
            for (int i2 = 0; i2 != certChain.length; i2++) {
                certChain[i2] = (Certificate) cs.elementAt(i2);
            }
            return certChain;
        }
    }

    public Date engineGetCreationDate(String alias) {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        } else if (this.keys.get(alias) == null && this.certs.get(alias) == null) {
            return null;
        } else {
            return new Date();
        }
    }

    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        if (alias != null) {
            return (Key) this.keys.get(alias);
        }
        throw new IllegalArgumentException("null alias passed to getKey.");
    }

    public boolean engineIsCertificateEntry(String alias) {
        return this.certs.get(alias) != null && this.keys.get(alias) == null;
    }

    public boolean engineIsKeyEntry(String alias) {
        return this.keys.get(alias) != null;
    }

    public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
        if (this.keys.get(alias) != null) {
            throw new KeyStoreException("There is a key entry with the name " + alias + ".");
        }
        this.certs.put(alias, cert);
        this.chainCerts.put(new CertId(cert.getPublicKey()), cert);
    }

    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
        throw new RuntimeException("operation not supported");
    }

    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
        if (!(key instanceof PrivateKey)) {
            throw new KeyStoreException("PKCS12 does not support non-PrivateKeys");
        } else if ((key instanceof PrivateKey) && chain == null) {
            throw new KeyStoreException("no certificate chain for private key");
        } else {
            if (this.keys.get(alias) != null) {
                engineDeleteEntry(alias);
            }
            this.keys.put(alias, key);
            if (chain != null) {
                this.certs.put(alias, chain[0]);
                for (int i = 0; i != chain.length; i++) {
                    this.chainCerts.put(new CertId(chain[i].getPublicKey()), chain[i]);
                }
            }
        }
    }

    public int engineSize() {
        Hashtable tab = new Hashtable();
        Enumeration e = this.certs.keys();
        while (e.hasMoreElements()) {
            tab.put(e.nextElement(), "cert");
        }
        e = this.keys.keys();
        while (e.hasMoreElements()) {
            String a = (String) e.nextElement();
            if (tab.get(a) == null) {
                tab.put(a, "key");
            }
        }
        return tab.size();
    }

    protected PrivateKey unwrapKey(AlgorithmIdentifier algId, byte[] data, char[] password, boolean wrongPKCS12Zero) throws IOException {
        ASN1ObjectIdentifier algorithm = algId.getAlgorithm();
        try {
            if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds)) {
                PKCS12PBEParams pbeParams = PKCS12PBEParams.getInstance(algId.getParameters());
                PBEParameterSpec defParams = new PBEParameterSpec(pbeParams.getIV(), pbeParams.getIterations().intValue());
                Cipher cipher = this.helper.createCipher(algorithm.getId());
                cipher.init(4, new PKCS12Key(password, wrongPKCS12Zero), defParams);
                return (PrivateKey) cipher.unwrap(data, "", 2);
            } else if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
                return (PrivateKey) createCipher(4, password, algId).unwrap(data, "", 2);
            } else {
                throw new IOException("exception unwrapping private key - cannot recognise: " + algorithm);
            }
        } catch (Exception e) {
            throw new IOException("exception unwrapping private key - " + e.toString());
        }
    }

    protected byte[] wrapKey(String algorithm, Key key, PKCS12PBEParams pbeParams, char[] password) throws IOException {
        PBEKeySpec pbeSpec = new PBEKeySpec(password);
        try {
            SecretKeyFactory keyFact = this.helper.createSecretKeyFactory(algorithm);
            PBEParameterSpec defParams = new PBEParameterSpec(pbeParams.getIV(), pbeParams.getIterations().intValue());
            Cipher cipher = this.helper.createCipher(algorithm);
            cipher.init(3, keyFact.generateSecret(pbeSpec), defParams);
            return cipher.wrap(key);
        } catch (Exception e) {
            throw new IOException("exception encrypting data - " + e.toString());
        }
    }

    protected byte[] cryptData(boolean forEncryption, AlgorithmIdentifier algId, char[] password, boolean wrongPKCS12Zero, byte[] data) throws IOException {
        ASN1ObjectIdentifier algorithm = algId.getAlgorithm();
        int mode = forEncryption ? 1 : 2;
        if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds)) {
            PKCS12PBEParams pbeParams = PKCS12PBEParams.getInstance(algId.getParameters());
            PBEKeySpec pbeSpec = new PBEKeySpec(password);
            try {
                PBEParameterSpec defParams = new PBEParameterSpec(pbeParams.getIV(), pbeParams.getIterations().intValue());
                PKCS12Key key = new PKCS12Key(password, wrongPKCS12Zero);
                Cipher cipher = this.helper.createCipher(algorithm.getId());
                cipher.init(mode, key, defParams);
                return cipher.doFinal(data);
            } catch (Exception e) {
                throw new IOException("exception decrypting data - " + e.toString());
            }
        } else if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
            try {
                return createCipher(mode, password, algId).doFinal(data);
            } catch (Exception e2) {
                throw new IOException("exception decrypting data - " + e2.toString());
            }
        } else {
            throw new IOException("unknown PBE algorithm: " + algorithm);
        }
    }

    private Cipher createCipher(int mode, char[] password, AlgorithmIdentifier algId) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchProviderException {
        SecretKey key;
        PBES2Parameters alg = PBES2Parameters.getInstance(algId.getParameters());
        PBKDF2Params func = PBKDF2Params.getInstance(alg.getKeyDerivationFunc().getParameters());
        AlgorithmIdentifier encScheme = AlgorithmIdentifier.getInstance(alg.getEncryptionScheme());
        SecretKeyFactory keyFact = this.helper.createSecretKeyFactory(alg.getKeyDerivationFunc().getAlgorithm().getId());
        if (func.isDefaultPrf()) {
            key = keyFact.generateSecret(new PBEKeySpec(password, func.getSalt(), func.getIterationCount().intValue(), keySizeProvider.getKeySize(encScheme)));
        } else {
            key = keyFact.generateSecret(new PBKDF2KeySpec(password, func.getSalt(), func.getIterationCount().intValue(), keySizeProvider.getKeySize(encScheme), func.getPrf()));
        }
        Cipher cipher = Cipher.getInstance(alg.getEncryptionScheme().getAlgorithm().getId());
        AlgorithmIdentifier encryptionAlg = AlgorithmIdentifier.getInstance(alg.getEncryptionScheme());
        ASN1Encodable encParams = alg.getEncryptionScheme().getParameters();
        if (encParams instanceof ASN1OctetString) {
            cipher.init(mode, key, new IvParameterSpec(ASN1OctetString.getInstance(encParams).getOctets()));
        }
        return cipher;
    }

    public void engineLoad(InputStream stream, char[] password) throws IOException {
        if (stream != null) {
            if (password == null) {
                throw new NullPointerException("No password supplied for PKCS#12 KeyStore.");
            }
            InputStream bufferedInputStream = new BufferedInputStream(stream);
            bufferedInputStream.mark(10);
            if (bufferedInputStream.read() != 48) {
                throw new IOException("stream does not represent a PKCS12 key store");
            }
            int i;
            SafeBag b;
            PKCS12BagAttributeCarrier bagAttr;
            String alias;
            ASN1OctetString localId;
            Enumeration e;
            ASN1Sequence sq;
            ASN1Set attrSet;
            ASN1Primitive attr;
            ASN1Encodable existing;
            String str;
            bufferedInputStream.reset();
            Pfx bag = Pfx.getInstance((ASN1Sequence) new ASN1InputStream(bufferedInputStream).readObject());
            ContentInfo info = bag.getAuthSafe();
            Vector chain = new Vector();
            boolean unmarkedKey = false;
            boolean wrongPKCS12Zero = false;
            if (bag.getMacData() != null) {
                MacData mData = bag.getMacData();
                DigestInfo dInfo = mData.getMac();
                AlgorithmIdentifier algId = dInfo.getAlgorithmId();
                byte[] salt = mData.getSalt();
                int itCount = mData.getIterationCount().intValue();
                byte[] data = ((ASN1OctetString) info.getContent()).getOctets();
                try {
                    byte[] res = calculatePbeMac(algId.getAlgorithm(), salt, itCount, password, false, data);
                    byte[] dig = dInfo.getDigest();
                    if (!Arrays.constantTimeAreEqual(res, dig)) {
                        if (password.length > 0) {
                            throw new IOException("PKCS12 key store mac invalid - wrong password or corrupted file.");
                        } else if (Arrays.constantTimeAreEqual(calculatePbeMac(algId.getAlgorithm(), salt, itCount, password, true, data), dig)) {
                            wrongPKCS12Zero = true;
                        } else {
                            throw new IOException("PKCS12 key store mac invalid - wrong password or corrupted file.");
                        }
                    }
                } catch (IOException e2) {
                    throw e2;
                } catch (Exception e3) {
                    throw new IOException("error constructing MAC: " + e3.toString());
                }
            }
            this.keys = new IgnoresCaseHashtable();
            this.localIds = new Hashtable();
            if (info.getContentType().equals(data)) {
                ContentInfo[] c = AuthenticatedSafe.getInstance(new ASN1InputStream(((ASN1OctetString) info.getContent()).getOctets()).readObject()).getContentInfo();
                for (i = 0; i != c.length; i++) {
                    ASN1Sequence seq;
                    int j;
                    EncryptedPrivateKeyInfo eIn;
                    PrivateKey privKey;
                    ASN1ObjectIdentifier aOid;
                    if (c[i].getContentType().equals(data)) {
                        seq = (ASN1Sequence) new ASN1InputStream(((ASN1OctetString) c[i].getContent()).getOctets()).readObject();
                        for (j = 0; j != seq.size(); j++) {
                            b = SafeBag.getInstance(seq.getObjectAt(j));
                            if (b.getBagId().equals(pkcs8ShroudedKeyBag)) {
                                eIn = EncryptedPrivateKeyInfo.getInstance(b.getBagValue());
                                privKey = unwrapKey(eIn.getEncryptionAlgorithm(), eIn.getEncryptedData(), password, wrongPKCS12Zero);
                                bagAttr = (PKCS12BagAttributeCarrier) privKey;
                                alias = null;
                                localId = null;
                                if (b.getBagAttributes() != null) {
                                    e = b.getBagAttributes().getObjects();
                                    while (e.hasMoreElements()) {
                                        sq = (ASN1Sequence) e.nextElement();
                                        aOid = (ASN1ObjectIdentifier) sq.getObjectAt(0);
                                        attrSet = (ASN1Set) sq.getObjectAt(1);
                                        attr = null;
                                        if (attrSet.size() > 0) {
                                            attr = (ASN1Primitive) attrSet.getObjectAt(0);
                                            existing = bagAttr.getBagAttribute(aOid);
                                            if (existing == null) {
                                                bagAttr.setBagAttribute(aOid, attr);
                                            } else if (!existing.toASN1Primitive().equals(attr)) {
                                                throw new IOException("attempt to add existing attribute with different value");
                                            }
                                        }
                                        if (aOid.equals(pkcs_9_at_friendlyName)) {
                                            alias = ((DERBMPString) attr).getString();
                                            this.keys.put(alias, privKey);
                                        } else {
                                            if (aOid.equals(pkcs_9_at_localKeyId)) {
                                                localId = (ASN1OctetString) attr;
                                            }
                                        }
                                    }
                                }
                                if (localId != null) {
                                    str = new String(Hex.encode(localId.getOctets()));
                                    if (alias == null) {
                                        this.keys.put(str, privKey);
                                    } else {
                                        this.localIds.put(alias, str);
                                    }
                                } else {
                                    unmarkedKey = true;
                                    this.keys.put("unmarked", privKey);
                                }
                            } else if (b.getBagId().equals(certBag)) {
                                chain.addElement(b);
                            } else {
                                System.out.println("extra in data " + b.getBagId());
                                System.out.println(ASN1Dump.dumpAsString(b));
                            }
                        }
                        continue;
                    } else if (c[i].getContentType().equals(encryptedData)) {
                        EncryptedData d = EncryptedData.getInstance(c[i].getContent());
                        seq = (ASN1Sequence) ASN1Primitive.fromByteArray(cryptData(false, d.getEncryptionAlgorithm(), password, wrongPKCS12Zero, d.getContent().getOctets()));
                        for (j = 0; j != seq.size(); j++) {
                            b = SafeBag.getInstance(seq.getObjectAt(j));
                            if (b.getBagId().equals(certBag)) {
                                chain.addElement(b);
                            } else if (b.getBagId().equals(pkcs8ShroudedKeyBag)) {
                                eIn = EncryptedPrivateKeyInfo.getInstance(b.getBagValue());
                                privKey = unwrapKey(eIn.getEncryptionAlgorithm(), eIn.getEncryptedData(), password, wrongPKCS12Zero);
                                bagAttr = (PKCS12BagAttributeCarrier) privKey;
                                alias = null;
                                localId = null;
                                e = b.getBagAttributes().getObjects();
                                while (e.hasMoreElements()) {
                                    sq = (ASN1Sequence) e.nextElement();
                                    aOid = (ASN1ObjectIdentifier) sq.getObjectAt(0);
                                    attrSet = (ASN1Set) sq.getObjectAt(1);
                                    attr = null;
                                    if (attrSet.size() > 0) {
                                        attr = (ASN1Primitive) attrSet.getObjectAt(0);
                                        existing = bagAttr.getBagAttribute(aOid);
                                        if (existing == null) {
                                            bagAttr.setBagAttribute(aOid, attr);
                                        } else if (!existing.toASN1Primitive().equals(attr)) {
                                            throw new IOException("attempt to add existing attribute with different value");
                                        }
                                    }
                                    if (aOid.equals(pkcs_9_at_friendlyName)) {
                                        alias = ((DERBMPString) attr).getString();
                                        this.keys.put(alias, privKey);
                                    } else {
                                        if (aOid.equals(pkcs_9_at_localKeyId)) {
                                            localId = (ASN1OctetString) attr;
                                        }
                                    }
                                }
                                str = new String(Hex.encode(localId.getOctets()));
                                if (alias == null) {
                                    this.keys.put(str, privKey);
                                } else {
                                    this.localIds.put(alias, str);
                                }
                            } else if (b.getBagId().equals(keyBag)) {
                                privKey = BouncyCastleProvider.getPrivateKey(PrivateKeyInfo.getInstance(b.getBagValue()));
                                bagAttr = (PKCS12BagAttributeCarrier) privKey;
                                alias = null;
                                localId = null;
                                e = b.getBagAttributes().getObjects();
                                while (e.hasMoreElements()) {
                                    sq = ASN1Sequence.getInstance(e.nextElement());
                                    aOid = ASN1ObjectIdentifier.getInstance(sq.getObjectAt(0));
                                    attrSet = ASN1Set.getInstance(sq.getObjectAt(1));
                                    if (attrSet.size() > 0) {
                                        attr = (ASN1Primitive) attrSet.getObjectAt(0);
                                        existing = bagAttr.getBagAttribute(aOid);
                                        if (existing == null) {
                                            bagAttr.setBagAttribute(aOid, attr);
                                        } else if (!existing.toASN1Primitive().equals(attr)) {
                                            throw new IOException("attempt to add existing attribute with different value");
                                        }
                                        if (aOid.equals(pkcs_9_at_friendlyName)) {
                                            alias = ((DERBMPString) attr).getString();
                                            this.keys.put(alias, privKey);
                                        } else {
                                            if (aOid.equals(pkcs_9_at_localKeyId)) {
                                                localId = (ASN1OctetString) attr;
                                            }
                                        }
                                    }
                                }
                                str = new String(Hex.encode(localId.getOctets()));
                                if (alias == null) {
                                    this.keys.put(str, privKey);
                                } else {
                                    this.localIds.put(alias, str);
                                }
                            } else {
                                System.out.println("extra in encryptedData " + b.getBagId());
                                System.out.println(ASN1Dump.dumpAsString(b));
                            }
                        }
                        continue;
                    } else {
                        System.out.println("extra " + c[i].getContentType().getId());
                        System.out.println("extra " + ASN1Dump.dumpAsString(c[i].getContent()));
                    }
                }
            }
            this.certs = new IgnoresCaseHashtable();
            this.chainCerts = new Hashtable();
            this.keyCerts = new Hashtable();
            i = 0;
            while (i != chain.size()) {
                b = (SafeBag) chain.elementAt(i);
                CertBag cb = CertBag.getInstance(b.getBagValue());
                if (cb.getCertId().equals(x509Certificate)) {
                    try {
                        Certificate cert = this.certFact.generateCertificate(new ByteArrayInputStream(((ASN1OctetString) cb.getCertValue()).getOctets()));
                        localId = null;
                        alias = null;
                        if (b.getBagAttributes() != null) {
                            e = b.getBagAttributes().getObjects();
                            while (e.hasMoreElements()) {
                                sq = ASN1Sequence.getInstance(e.nextElement());
                                ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(sq.getObjectAt(0));
                                attrSet = ASN1Set.getInstance(sq.getObjectAt(1));
                                if (attrSet.size() > 0) {
                                    attr = (ASN1Primitive) attrSet.getObjectAt(0);
                                    if (cert instanceof PKCS12BagAttributeCarrier) {
                                        bagAttr = (PKCS12BagAttributeCarrier) cert;
                                        existing = bagAttr.getBagAttribute(oid);
                                        if (existing == null) {
                                            bagAttr.setBagAttribute(oid, attr);
                                        } else if (!existing.toASN1Primitive().equals(attr)) {
                                            throw new IOException("attempt to add existing attribute with different value");
                                        }
                                    }
                                    if (oid.equals(pkcs_9_at_friendlyName)) {
                                        alias = ((DERBMPString) attr).getString();
                                    } else {
                                        if (oid.equals(pkcs_9_at_localKeyId)) {
                                            localId = (ASN1OctetString) attr;
                                        }
                                    }
                                }
                            }
                        }
                        this.chainCerts.put(new CertId(cert.getPublicKey()), cert);
                        if (!unmarkedKey) {
                            if (localId != null) {
                                this.keyCerts.put(new String(Hex.encode(localId.getOctets())), cert);
                            }
                            if (alias != null) {
                                this.certs.put(alias, cert);
                            }
                        } else if (this.keyCerts.isEmpty()) {
                            str = new String(Hex.encode(createSubjectKeyId(cert.getPublicKey()).getKeyIdentifier()));
                            this.keyCerts.put(str, cert);
                            this.keys.put(str, this.keys.remove("unmarked"));
                        }
                        i++;
                    } catch (Exception e32) {
                        throw new RuntimeException(e32.toString());
                    }
                }
                throw new RuntimeException("Unsupported certificate type: " + cb.getCertId());
            }
        }
    }

    public void engineStore(LoadStoreParameter param) throws IOException, NoSuchAlgorithmException, CertificateException {
        if (param == null) {
            throw new IllegalArgumentException("'param' arg cannot be null");
        }
        if (!(param instanceof PKCS12StoreParameter) ? param instanceof JDKPKCS12StoreParameter : true) {
            PKCS12StoreParameter bcParam;
            char[] password;
            if (param instanceof PKCS12StoreParameter) {
                bcParam = (PKCS12StoreParameter) param;
            } else {
                bcParam = new PKCS12StoreParameter(((JDKPKCS12StoreParameter) param).getOutputStream(), param.getProtectionParameter(), ((JDKPKCS12StoreParameter) param).isUseDEREncoding());
            }
            ProtectionParameter protParam = param.getProtectionParameter();
            if (protParam == null) {
                password = null;
            } else if (protParam instanceof PasswordProtection) {
                password = ((PasswordProtection) protParam).getPassword();
            } else {
                throw new IllegalArgumentException("No support for protection parameter of type " + protParam.getClass().getName());
            }
            doStore(bcParam.getOutputStream(), password, bcParam.isForDEREncoding());
            return;
        }
        throw new IllegalArgumentException("No support for 'param' of type " + param.getClass().getName());
    }

    public void engineStore(OutputStream stream, char[] password) throws IOException {
        doStore(stream, password, false);
    }

    private void doStore(java.io.OutputStream r67, char[] r68, boolean r69) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r18_0 'asn1Out' com.android.org.bouncycastle.asn1.DEROutputStream) in PHI: PHI: (r18_1 'asn1Out' com.android.org.bouncycastle.asn1.DEROutputStream) = (r18_0 'asn1Out' com.android.org.bouncycastle.asn1.DEROutputStream), (r18_5 'asn1Out' com.android.org.bouncycastle.asn1.DEROutputStream) binds: {(r18_0 'asn1Out' com.android.org.bouncycastle.asn1.DEROutputStream)=B:99:0x0595, (r18_5 'asn1Out' com.android.org.bouncycastle.asn1.DEROutputStream)=B:108:0x0618}
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
        r66 = this;
        if (r68 != 0) goto L_0x000b;
    L_0x0002:
        r4 = new java.lang.NullPointerException;
        r5 = "No password supplied for PKCS#12 KeyStore.";
        r4.<init>(r5);
        throw r4;
    L_0x000b:
        r51 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;
        r51.<init>();
        r0 = r66;
        r4 = r0.keys;
        r54 = r4.keys();
    L_0x0018:
        r4 = r54.hasMoreElements();
        if (r4 == 0) goto L_0x018d;
    L_0x001e:
        r4 = 20;
        r0 = new byte[r4];
        r49 = r0;
        r0 = r66;
        r4 = r0.random;
        r0 = r49;
        r4.nextBytes(r0);
        r57 = r54.nextElement();
        r57 = (java.lang.String) r57;
        r0 = r66;
        r4 = r0.keys;
        r0 = r57;
        r62 = r4.get(r0);
        r62 = (java.security.PrivateKey) r62;
        r48 = new com.android.org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
        r4 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        r0 = r48;
        r1 = r49;
        r0.<init>(r1, r4);
        r0 = r66;
        r4 = r0.keyAlgorithm;
        r4 = r4.getId();
        r0 = r66;
        r1 = r62;
        r2 = r48;
        r3 = r68;
        r45 = r0.wrapKey(r4, r1, r2, r3);
        r43 = new com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
        r0 = r66;
        r4 = r0.keyAlgorithm;
        r5 = r48.toASN1Primitive();
        r0 = r43;
        r0.<init>(r4, r5);
        r46 = new com.android.org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
        r0 = r46;
        r1 = r43;
        r2 = r45;
        r0.<init>(r1, r2);
        r19 = 0;
        r47 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;
        r47.<init>();
        r0 = r62;
        r4 = r0 instanceof com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
        if (r4 == 0) goto L_0x0111;
    L_0x0085:
        r22 = r62;
        r22 = (com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier) r22;
        r4 = pkcs_9_at_friendlyName;
        r0 = r22;
        r58 = r0.getBagAttribute(r4);
        r58 = (com.android.org.bouncycastle.asn1.DERBMPString) r58;
        if (r58 == 0) goto L_0x00a3;
    L_0x0095:
        r4 = r58.getString();
        r0 = r57;
        r4 = r4.equals(r0);
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x00b1;
    L_0x00a3:
        r4 = pkcs_9_at_friendlyName;
        r5 = new com.android.org.bouncycastle.asn1.DERBMPString;
        r0 = r57;
        r5.<init>(r0);
        r0 = r22;
        r0.setBagAttribute(r4, r5);
    L_0x00b1:
        r4 = pkcs_9_at_localKeyId;
        r0 = r22;
        r4 = r0.getBagAttribute(r4);
        if (r4 != 0) goto L_0x00d4;
    L_0x00bb:
        r0 = r66;
        r1 = r57;
        r34 = r0.engineGetCertificate(r1);
        r4 = pkcs_9_at_localKeyId;
        r5 = r34.getPublicKey();
        r0 = r66;
        r5 = r0.createSubjectKeyId(r5);
        r0 = r22;
        r0.setBagAttribute(r4, r5);
    L_0x00d4:
        r39 = r22.getBagAttributeKeys();
    L_0x00d8:
        r4 = r39.hasMoreElements();
        if (r4 == 0) goto L_0x0111;
    L_0x00de:
        r59 = r39.nextElement();
        r59 = (com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier) r59;
        r50 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;
        r50.<init>();
        r0 = r50;
        r1 = r59;
        r0.add(r1);
        r4 = new com.android.org.bouncycastle.asn1.DERSet;
        r0 = r22;
        r1 = r59;
        r5 = r0.getBagAttribute(r1);
        r4.<init>(r5);
        r0 = r50;
        r0.add(r4);
        r19 = 1;
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;
        r0 = r50;
        r4.<init>(r0);
        r0 = r47;
        r0.add(r4);
        goto L_0x00d8;
    L_0x0111:
        if (r19 != 0) goto L_0x0170;
    L_0x0113:
        r50 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;
        r50.<init>();
        r0 = r66;
        r1 = r57;
        r34 = r0.engineGetCertificate(r1);
        r4 = pkcs_9_at_localKeyId;
        r0 = r50;
        r0.add(r4);
        r4 = new com.android.org.bouncycastle.asn1.DERSet;
        r5 = r34.getPublicKey();
        r0 = r66;
        r5 = r0.createSubjectKeyId(r5);
        r4.<init>(r5);
        r0 = r50;
        r0.add(r4);
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;
        r0 = r50;
        r4.<init>(r0);
        r0 = r47;
        r0.add(r4);
        r50 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;
        r50.<init>();
        r4 = pkcs_9_at_friendlyName;
        r0 = r50;
        r0.add(r4);
        r4 = new com.android.org.bouncycastle.asn1.DERSet;
        r5 = new com.android.org.bouncycastle.asn1.DERBMPString;
        r0 = r57;
        r5.<init>(r0);
        r4.<init>(r5);
        r0 = r50;
        r0.add(r4);
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;
        r0 = r50;
        r4.<init>(r0);
        r0 = r47;
        r0.add(r4);
    L_0x0170:
        r44 = new com.android.org.bouncycastle.asn1.pkcs.SafeBag;
        r4 = pkcs8ShroudedKeyBag;
        r5 = r46.toASN1Primitive();
        r7 = new com.android.org.bouncycastle.asn1.DERSet;
        r0 = r47;
        r7.<init>(r0);
        r0 = r44;
        r0.<init>(r4, r5, r7);
        r0 = r51;
        r1 = r44;
        r0.add(r1);
        goto L_0x0018;
    L_0x018d:
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;
        r0 = r51;
        r4.<init>(r0);
        r5 = "DER";
        r52 = r4.getEncoded(r5);
        r53 = new com.android.org.bouncycastle.asn1.BEROctetString;
        r0 = r53;
        r1 = r52;
        r0.<init>(r1);
        r4 = 20;
        r0 = new byte[r4];
        r27 = r0;
        r0 = r66;
        r4 = r0.random;
        r0 = r27;
        r4.nextBytes(r0);
        r32 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;
        r32.<init>();
        r26 = new com.android.org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
        r4 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        r0 = r26;
        r1 = r27;
        r0.<init>(r1, r4);
        r6 = new com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
        r0 = r66;
        r4 = r0.certAlgorithm;
        r5 = r26.toASN1Primitive();
        r6.<init>(r4, r5);
        r36 = new java.util.Hashtable;
        r36.<init>();
        r0 = r66;
        r4 = r0.keys;
        r33 = r4.keys();
    L_0x01dd:
        r4 = r33.hasMoreElements();
        if (r4 == 0) goto L_0x0332;
    L_0x01e3:
        r57 = r33.nextElement();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r57 = (java.lang.String) r57;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r66;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r1 = r57;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r28 = r0.engineGetCertificate(r1);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r23 = 0;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r24 = new com.android.org.bouncycastle.asn1.pkcs.CertBag;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = x509Certificate;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5 = new com.android.org.bouncycastle.asn1.DEROctetString;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r7 = r28.getEncoded();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5.<init>(r7);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r24;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.<init>(r4, r5);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r40 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r40.<init>();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r28;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = r0 instanceof com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;	 Catch:{ CertificateEncodingException -> 0x0313 }
        if (r4 == 0) goto L_0x0296;	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x0210:
        r0 = r28;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = (com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier) r0;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r22 = r0;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = pkcs_9_at_friendlyName;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r22;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r58 = r0.getBagAttribute(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r58 = (com.android.org.bouncycastle.asn1.DERBMPString) r58;	 Catch:{ CertificateEncodingException -> 0x0313 }
        if (r58 == 0) goto L_0x0230;	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x0222:
        r4 = r58.getString();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r57;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = r4.equals(r0);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = r4 ^ 1;	 Catch:{ CertificateEncodingException -> 0x0313 }
        if (r4 == 0) goto L_0x023e;	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x0230:
        r4 = pkcs_9_at_friendlyName;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5 = new com.android.org.bouncycastle.asn1.DERBMPString;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r57;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r22;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.setBagAttribute(r4, r5);	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x023e:
        r4 = pkcs_9_at_localKeyId;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r22;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = r0.getBagAttribute(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
        if (r4 != 0) goto L_0x0259;	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x0248:
        r4 = pkcs_9_at_localKeyId;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5 = r28.getPublicKey();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r66;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5 = r0.createSubjectKeyId(r5);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r22;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.setBagAttribute(r4, r5);	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x0259:
        r39 = r22.getBagAttributeKeys();	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x025d:
        r4 = r39.hasMoreElements();	 Catch:{ CertificateEncodingException -> 0x0313 }
        if (r4 == 0) goto L_0x0296;	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x0263:
        r59 = r39.nextElement();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r59 = (com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier) r59;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r41 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r41.<init>();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r1 = r59;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r1);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = new com.android.org.bouncycastle.asn1.DERSet;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r22;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r1 = r59;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5 = r0.getBagAttribute(r1);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4.<init>(r5);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r40;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r23 = 1;	 Catch:{ CertificateEncodingException -> 0x0313 }
        goto L_0x025d;	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x0296:
        if (r23 != 0) goto L_0x02ed;	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x0298:
        r41 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r41.<init>();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = pkcs_9_at_localKeyId;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = new com.android.org.bouncycastle.asn1.DERSet;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5 = r28.getPublicKey();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r66;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5 = r0.createSubjectKeyId(r5);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4.<init>(r5);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r40;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r41 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r41.<init>();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = pkcs_9_at_friendlyName;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = new com.android.org.bouncycastle.asn1.DERSet;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5 = new com.android.org.bouncycastle.asn1.DERBMPString;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r57;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4.<init>(r5);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r40;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0313 }
    L_0x02ed:
        r64 = new com.android.org.bouncycastle.asn1.pkcs.SafeBag;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r4 = certBag;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r5 = r24.toASN1Primitive();	 Catch:{ CertificateEncodingException -> 0x0313 }
        r7 = new com.android.org.bouncycastle.asn1.DERSet;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r40;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r7.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r64;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.<init>(r4, r5, r7);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r32;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r1 = r64;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.add(r1);	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0 = r36;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r1 = r28;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r2 = r28;	 Catch:{ CertificateEncodingException -> 0x0313 }
        r0.put(r1, r2);	 Catch:{ CertificateEncodingException -> 0x0313 }
        goto L_0x01dd;
    L_0x0313:
        r38 = move-exception;
        r4 = new java.io.IOException;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r7 = "Error encoding certificate: ";
        r5 = r5.append(r7);
        r7 = r38.toString();
        r5 = r5.append(r7);
        r5 = r5.toString();
        r4.<init>(r5);
        throw r4;
    L_0x0332:
        r0 = r66;
        r4 = r0.certs;
        r33 = r4.keys();
    L_0x033a:
        r4 = r33.hasMoreElements();
        if (r4 == 0) goto L_0x0462;
    L_0x0340:
        r31 = r33.nextElement();	 Catch:{ CertificateEncodingException -> 0x0443 }
        r31 = (java.lang.String) r31;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r66;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = r0.certs;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r31;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r28 = r4.get(r0);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r28 = (java.security.cert.Certificate) r28;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r23 = 0;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r66;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = r0.keys;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r31;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = r4.get(r0);	 Catch:{ CertificateEncodingException -> 0x0443 }
        if (r4 != 0) goto L_0x033a;	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x0360:
        r24 = new com.android.org.bouncycastle.asn1.pkcs.CertBag;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = x509Certificate;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r5 = new com.android.org.bouncycastle.asn1.DEROctetString;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r7 = r28.getEncoded();	 Catch:{ CertificateEncodingException -> 0x0443 }
        r5.<init>(r7);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r24;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.<init>(r4, r5);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r40 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r40.<init>();	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r28;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = r0 instanceof com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;	 Catch:{ CertificateEncodingException -> 0x0443 }
        if (r4 == 0) goto L_0x03f2;	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x037d:
        r0 = r28;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = (com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier) r0;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r22 = r0;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = pkcs_9_at_friendlyName;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r22;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r58 = r0.getBagAttribute(r4);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r58 = (com.android.org.bouncycastle.asn1.DERBMPString) r58;	 Catch:{ CertificateEncodingException -> 0x0443 }
        if (r58 == 0) goto L_0x039d;	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x038f:
        r4 = r58.getString();	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r31;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = r4.equals(r0);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = r4 ^ 1;	 Catch:{ CertificateEncodingException -> 0x0443 }
        if (r4 == 0) goto L_0x03ab;	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x039d:
        r4 = pkcs_9_at_friendlyName;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r5 = new com.android.org.bouncycastle.asn1.DERBMPString;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r31;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r5.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r22;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.setBagAttribute(r4, r5);	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x03ab:
        r39 = r22.getBagAttributeKeys();	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x03af:
        r4 = r39.hasMoreElements();	 Catch:{ CertificateEncodingException -> 0x0443 }
        if (r4 == 0) goto L_0x03f2;	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x03b5:
        r59 = r39.nextElement();	 Catch:{ CertificateEncodingException -> 0x0443 }
        r59 = (com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier) r59;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_localKeyId;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r59;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = r0.equals(r4);	 Catch:{ CertificateEncodingException -> 0x0443 }
        if (r4 != 0) goto L_0x03af;	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x03c5:
        r41 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r41.<init>();	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r1 = r59;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.add(r1);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = new com.android.org.bouncycastle.asn1.DERSet;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r22;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r1 = r59;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r5 = r0.getBagAttribute(r1);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4.<init>(r5);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r40;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r23 = 1;	 Catch:{ CertificateEncodingException -> 0x0443 }
        goto L_0x03af;	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x03f2:
        if (r23 != 0) goto L_0x041d;	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x03f4:
        r41 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r41.<init>();	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = pkcs_9_at_friendlyName;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = new com.android.org.bouncycastle.asn1.DERSet;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r5 = new com.android.org.bouncycastle.asn1.DERBMPString;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r31;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r5.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4.<init>(r5);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r40;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0443 }
    L_0x041d:
        r64 = new com.android.org.bouncycastle.asn1.pkcs.SafeBag;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r4 = certBag;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r5 = r24.toASN1Primitive();	 Catch:{ CertificateEncodingException -> 0x0443 }
        r7 = new com.android.org.bouncycastle.asn1.DERSet;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r40;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r7.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r64;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.<init>(r4, r5, r7);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r32;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r1 = r64;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.add(r1);	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0 = r36;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r1 = r28;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r2 = r28;	 Catch:{ CertificateEncodingException -> 0x0443 }
        r0.put(r1, r2);	 Catch:{ CertificateEncodingException -> 0x0443 }
        goto L_0x033a;
    L_0x0443:
        r38 = move-exception;
        r4 = new java.io.IOException;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r7 = "Error encoding certificate: ";
        r5 = r5.append(r7);
        r7 = r38.toString();
        r5 = r5.append(r7);
        r5 = r5.toString();
        r4.<init>(r5);
        throw r4;
    L_0x0462:
        r65 = r66.getUsedCertificateSet();
        r0 = r66;
        r4 = r0.chainCerts;
        r33 = r4.keys();
    L_0x046e:
        r4 = r33.hasMoreElements();
        if (r4 == 0) goto L_0x053e;
    L_0x0474:
        r30 = r33.nextElement();	 Catch:{ CertificateEncodingException -> 0x0502 }
        r30 = (com.android.org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi.CertId) r30;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r66;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = r0.chainCerts;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r30;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r28 = r4.get(r0);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r28 = (java.security.cert.Certificate) r28;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r65;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r1 = r28;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = r0.contains(r1);	 Catch:{ CertificateEncodingException -> 0x0502 }
        if (r4 == 0) goto L_0x046e;	 Catch:{ CertificateEncodingException -> 0x0502 }
    L_0x0490:
        r0 = r36;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r1 = r28;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = r0.get(r1);	 Catch:{ CertificateEncodingException -> 0x0502 }
        if (r4 != 0) goto L_0x046e;	 Catch:{ CertificateEncodingException -> 0x0502 }
    L_0x049a:
        r24 = new com.android.org.bouncycastle.asn1.pkcs.CertBag;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = x509Certificate;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r5 = new com.android.org.bouncycastle.asn1.DEROctetString;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r7 = r28.getEncoded();	 Catch:{ CertificateEncodingException -> 0x0502 }
        r5.<init>(r7);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r24;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0.<init>(r4, r5);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r40 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r40.<init>();	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r28;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = r0 instanceof com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;	 Catch:{ CertificateEncodingException -> 0x0502 }
        if (r4 == 0) goto L_0x0521;	 Catch:{ CertificateEncodingException -> 0x0502 }
    L_0x04b7:
        r0 = r28;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = (com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier) r0;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r22 = r0;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r39 = r22.getBagAttributeKeys();	 Catch:{ CertificateEncodingException -> 0x0502 }
    L_0x04c1:
        r4 = r39.hasMoreElements();	 Catch:{ CertificateEncodingException -> 0x0502 }
        if (r4 == 0) goto L_0x0521;	 Catch:{ CertificateEncodingException -> 0x0502 }
    L_0x04c7:
        r59 = r39.nextElement();	 Catch:{ CertificateEncodingException -> 0x0502 }
        r59 = (com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier) r59;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_localKeyId;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r59;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = r0.equals(r4);	 Catch:{ CertificateEncodingException -> 0x0502 }
        if (r4 != 0) goto L_0x04c1;	 Catch:{ CertificateEncodingException -> 0x0502 }
    L_0x04d7:
        r41 = new com.android.org.bouncycastle.asn1.ASN1EncodableVector;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r41.<init>();	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r1 = r59;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0.add(r1);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = new com.android.org.bouncycastle.asn1.DERSet;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r22;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r1 = r59;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r5 = r0.getBagAttribute(r1);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4.<init>(r5);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r41;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r40;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0.add(r4);	 Catch:{ CertificateEncodingException -> 0x0502 }
        goto L_0x04c1;
    L_0x0502:
        r38 = move-exception;
        r4 = new java.io.IOException;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r7 = "Error encoding certificate: ";
        r5 = r5.append(r7);
        r7 = r38.toString();
        r5 = r5.append(r7);
        r5 = r5.toString();
        r4.<init>(r5);
        throw r4;
    L_0x0521:
        r64 = new com.android.org.bouncycastle.asn1.pkcs.SafeBag;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r4 = certBag;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r5 = r24.toASN1Primitive();	 Catch:{ CertificateEncodingException -> 0x0502 }
        r7 = new com.android.org.bouncycastle.asn1.DERSet;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r40;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r7.<init>(r0);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r64;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0.<init>(r4, r5, r7);	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0 = r32;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r1 = r64;	 Catch:{ CertificateEncodingException -> 0x0502 }
        r0.add(r1);	 Catch:{ CertificateEncodingException -> 0x0502 }
        goto L_0x046e;
    L_0x053e:
        r4 = new com.android.org.bouncycastle.asn1.DERSequence;
        r0 = r32;
        r4.<init>(r0);
        r5 = "DER";
        r9 = r4.getEncoded(r5);
        r5 = 1;
        r8 = 0;
        r4 = r66;
        r7 = r68;
        r29 = r4.cryptData(r5, r6, r7, r8, r9);
        r25 = new com.android.org.bouncycastle.asn1.pkcs.EncryptedData;
        r4 = data;
        r5 = new com.android.org.bouncycastle.asn1.BEROctetString;
        r0 = r29;
        r5.<init>(r0);
        r0 = r25;
        r0.<init>(r4, r6, r5);
        r4 = 2;
        r0 = new com.android.org.bouncycastle.asn1.pkcs.ContentInfo[r4];
        r42 = r0;
        r4 = new com.android.org.bouncycastle.asn1.pkcs.ContentInfo;
        r5 = data;
        r0 = r53;
        r4.<init>(r5, r0);
        r5 = 0;
        r42[r5] = r4;
        r4 = new com.android.org.bouncycastle.asn1.pkcs.ContentInfo;
        r5 = encryptedData;
        r7 = r25.toASN1Primitive();
        r4.<init>(r5, r7);
        r5 = 1;
        r42[r5] = r4;
        r20 = new com.android.org.bouncycastle.asn1.pkcs.AuthenticatedSafe;
        r0 = r20;
        r1 = r42;
        r0.<init>(r1);
        r21 = new java.io.ByteArrayOutputStream;
        r21.<init>();
        if (r69 == 0) goto L_0x0618;
    L_0x0595:
        r18 = new com.android.org.bouncycastle.asn1.DEROutputStream;
        r0 = r18;
        r1 = r21;
        r0.<init>(r1);
    L_0x059e:
        r0 = r18;
        r1 = r20;
        r0.writeObject(r1);
        r61 = r21.toByteArray();
        r56 = new com.android.org.bouncycastle.asn1.pkcs.ContentInfo;
        r4 = data;
        r5 = new com.android.org.bouncycastle.asn1.BEROctetString;
        r0 = r61;
        r5.<init>(r0);
        r0 = r56;
        r0.<init>(r4, r5);
        r4 = 20;
        r12 = new byte[r4];
        r13 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        r0 = r66;
        r4 = r0.random;
        r4.nextBytes(r12);
        r4 = r56.getContent();
        r4 = (com.android.org.bouncycastle.asn1.ASN1OctetString) r4;
        r16 = r4.getOctets();
        r11 = id_SHA1;	 Catch:{ Exception -> 0x0623 }
        r15 = 0;	 Catch:{ Exception -> 0x0623 }
        r10 = r66;	 Catch:{ Exception -> 0x0623 }
        r14 = r68;	 Catch:{ Exception -> 0x0623 }
        r63 = r10.calculatePbeMac(r11, r12, r13, r14, r15, r16);	 Catch:{ Exception -> 0x0623 }
        r17 = new com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;	 Catch:{ Exception -> 0x0623 }
        r4 = id_SHA1;	 Catch:{ Exception -> 0x0623 }
        r5 = com.android.org.bouncycastle.asn1.DERNull.INSTANCE;	 Catch:{ Exception -> 0x0623 }
        r0 = r17;	 Catch:{ Exception -> 0x0623 }
        r0.<init>(r4, r5);	 Catch:{ Exception -> 0x0623 }
        r35 = new com.android.org.bouncycastle.asn1.x509.DigestInfo;	 Catch:{ Exception -> 0x0623 }
        r0 = r35;	 Catch:{ Exception -> 0x0623 }
        r1 = r17;	 Catch:{ Exception -> 0x0623 }
        r2 = r63;	 Catch:{ Exception -> 0x0623 }
        r0.<init>(r1, r2);	 Catch:{ Exception -> 0x0623 }
        r55 = new com.android.org.bouncycastle.asn1.pkcs.MacData;	 Catch:{ Exception -> 0x0623 }
        r0 = r55;	 Catch:{ Exception -> 0x0623 }
        r1 = r35;	 Catch:{ Exception -> 0x0623 }
        r0.<init>(r1, r12, r13);	 Catch:{ Exception -> 0x0623 }
        r60 = new com.android.org.bouncycastle.asn1.pkcs.Pfx;
        r0 = r60;
        r1 = r56;
        r2 = r55;
        r0.<init>(r1, r2);
        if (r69 == 0) goto L_0x0642;
    L_0x0607:
        r18 = new com.android.org.bouncycastle.asn1.DEROutputStream;
        r0 = r18;
        r1 = r67;
        r0.<init>(r1);
    L_0x0610:
        r0 = r18;
        r1 = r60;
        r0.writeObject(r1);
        return;
    L_0x0618:
        r18 = new com.android.org.bouncycastle.asn1.BEROutputStream;
        r0 = r18;
        r1 = r21;
        r0.<init>(r1);
        goto L_0x059e;
    L_0x0623:
        r37 = move-exception;
        r4 = new java.io.IOException;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r7 = "error constructing MAC: ";
        r5 = r5.append(r7);
        r7 = r37.toString();
        r5 = r5.append(r7);
        r5 = r5.toString();
        r4.<init>(r5);
        throw r4;
    L_0x0642:
        r18 = new com.android.org.bouncycastle.asn1.BEROutputStream;
        r0 = r18;
        r1 = r67;
        r0.<init>(r1);
        goto L_0x0610;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi.doStore(java.io.OutputStream, char[], boolean):void");
    }

    private Set getUsedCertificateSet() {
        Set usedSet = new HashSet();
        Enumeration en = this.keys.keys();
        while (en.hasMoreElements()) {
            Certificate[] certs = engineGetCertificateChain((String) en.nextElement());
            for (int i = 0; i != certs.length; i++) {
                usedSet.add(certs[i]);
            }
        }
        en = this.certs.keys();
        while (en.hasMoreElements()) {
            usedSet.add(engineGetCertificate((String) en.nextElement()));
        }
        return usedSet;
    }

    private byte[] calculatePbeMac(ASN1ObjectIdentifier oid, byte[] salt, int itCount, char[] password, boolean wrongPkcs12Zero, byte[] data) throws Exception {
        PBEParameterSpec defParams = new PBEParameterSpec(salt, itCount);
        Mac mac = this.helper.createMac(oid.getId());
        mac.init(new PKCS12Key(password, wrongPkcs12Zero), defParams);
        mac.update(data);
        return mac.doFinal();
    }
}
