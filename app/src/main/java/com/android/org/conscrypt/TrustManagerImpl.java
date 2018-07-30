package com.android.org.conscrypt;

import com.android.org.conscrypt.ct.CTPolicy;
import com.android.org.conscrypt.ct.CTVerifier;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PKIXRevocationChecker.Option;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedTrustManager;

public final class TrustManagerImpl extends X509ExtendedTrustManager {
    private static final TrustAnchorComparator TRUST_ANCHOR_COMPARATOR = new TrustAnchorComparator();
    private final X509Certificate[] acceptedIssuers;
    private final CertBlacklist blacklist;
    private boolean ctEnabledOverride;
    private CTPolicy ctPolicy;
    private CTVerifier ctVerifier;
    private final Exception err;
    private final CertificateFactory factory;
    private final TrustedCertificateIndex intermediateIndex;
    private CertPinManager pinManager;
    private final KeyStore rootKeyStore;
    private final TrustedCertificateIndex trustedCertificateIndex;
    private final TrustedCertificateStore trustedCertificateStore;
    private final CertPathValidator validator;

    private static class ExtendedKeyUsagePKIXCertPathChecker extends PKIXCertPathChecker {
        private static final String EKU_OID = "2.5.29.37";
        private static final String EKU_anyExtendedKeyUsage = "2.5.29.37.0";
        private static final String EKU_clientAuth = "1.3.6.1.5.5.7.3.2";
        private static final String EKU_msSGC = "1.3.6.1.4.1.311.10.3.3";
        private static final String EKU_nsSGC = "2.16.840.1.113730.4.1";
        private static final String EKU_serverAuth = "1.3.6.1.5.5.7.3.1";
        private static final Set<String> SUPPORTED_EXTENSIONS = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[]{EKU_OID})));
        private final boolean clientAuth;
        private final X509Certificate leaf;

        /* synthetic */ ExtendedKeyUsagePKIXCertPathChecker(boolean clientAuth, X509Certificate leaf, ExtendedKeyUsagePKIXCertPathChecker -this2) {
            this(clientAuth, leaf);
        }

        private ExtendedKeyUsagePKIXCertPathChecker(boolean clientAuth, X509Certificate leaf) {
            this.clientAuth = clientAuth;
            this.leaf = leaf;
        }

        public void init(boolean forward) throws CertPathValidatorException {
        }

        public boolean isForwardCheckingSupported() {
            return true;
        }

        public Set<String> getSupportedExtensions() {
            return SUPPORTED_EXTENSIONS;
        }

        public void check(Certificate c, Collection<String> unresolvedCritExts) throws CertPathValidatorException {
            if (c == this.leaf) {
                try {
                    List<String> ekuOids = this.leaf.getExtendedKeyUsage();
                    if (ekuOids != null) {
                        boolean goodExtendedKeyUsage = false;
                        for (String ekuOid : ekuOids) {
                            if (ekuOid.equals(EKU_anyExtendedKeyUsage)) {
                                goodExtendedKeyUsage = true;
                                break;
                            } else if (this.clientAuth) {
                                if (ekuOid.equals(EKU_clientAuth)) {
                                    goodExtendedKeyUsage = true;
                                    break;
                                }
                            } else if (ekuOid.equals(EKU_serverAuth)) {
                                goodExtendedKeyUsage = true;
                                break;
                            } else if (ekuOid.equals(EKU_nsSGC)) {
                                goodExtendedKeyUsage = true;
                                break;
                            } else if (ekuOid.equals(EKU_msSGC)) {
                                goodExtendedKeyUsage = true;
                                break;
                            }
                        }
                        if (goodExtendedKeyUsage) {
                            unresolvedCritExts.remove(EKU_OID);
                            return;
                        }
                        throw new CertPathValidatorException("End-entity certificate does not have a valid extendedKeyUsage.");
                    }
                } catch (CertificateParsingException e) {
                    throw new CertPathValidatorException(e);
                }
            }
        }
    }

    private static class TrustAnchorComparator implements Comparator<TrustAnchor> {
        private static final CertificatePriorityComparator CERT_COMPARATOR = new CertificatePriorityComparator();

        /* synthetic */ TrustAnchorComparator(TrustAnchorComparator -this0) {
            this();
        }

        private TrustAnchorComparator() {
        }

        public int compare(TrustAnchor lhs, TrustAnchor rhs) {
            return CERT_COMPARATOR.compare(lhs.getTrustedCert(), rhs.getTrustedCert());
        }
    }

    public TrustManagerImpl(KeyStore keyStore) {
        this(keyStore, null);
    }

    public TrustManagerImpl(KeyStore keyStore, CertPinManager manager) {
        this(keyStore, manager, null);
    }

    public TrustManagerImpl(KeyStore keyStore, CertPinManager manager, TrustedCertificateStore certStore) {
        this(keyStore, manager, certStore, null);
    }

    public TrustManagerImpl(KeyStore keyStore, CertPinManager manager, TrustedCertificateStore certStore, CertBlacklist blacklist) {
        this(keyStore, manager, certStore, blacklist, null, null, null);
    }

    public TrustManagerImpl(java.security.KeyStore r15, com.android.org.conscrypt.CertPinManager r16, com.android.org.conscrypt.TrustedCertificateStore r17, com.android.org.conscrypt.CertBlacklist r18, com.android.org.conscrypt.ct.CTLogStore r19, com.android.org.conscrypt.ct.CTVerifier r20, com.android.org.conscrypt.ct.CTPolicy r21) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r21_1 'ctPolicy' com.android.org.conscrypt.ct.CTPolicy) in PHI: PHI: (r21_2 'ctPolicy' com.android.org.conscrypt.ct.CTPolicy) = (r21_0 'ctPolicy' com.android.org.conscrypt.ct.CTPolicy), (r21_1 'ctPolicy' com.android.org.conscrypt.ct.CTPolicy) binds: {(r21_0 'ctPolicy' com.android.org.conscrypt.ct.CTPolicy)=B:13:0x003e, (r21_1 'ctPolicy' com.android.org.conscrypt.ct.CTPolicy)=B:14:0x0040}
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
        r14 = this;
        r14.<init>();
        r11 = 0;
        r5 = 0;
        r6 = 0;
        r9 = 0;
        r7 = 0;
        r2 = 0;
        r4 = 0;
        r12 = "PKIX";	 Catch:{ Exception -> 0x008e }
        r11 = java.security.cert.CertPathValidator.getInstance(r12);	 Catch:{ Exception -> 0x008e }
        r12 = "X509";	 Catch:{ Exception -> 0x008e }
        r5 = java.security.cert.CertificateFactory.getInstance(r12);	 Catch:{ Exception -> 0x008e }
        r12 = "AndroidCAStore";	 Catch:{ Exception -> 0x008e }
        r13 = r15.getType();	 Catch:{ Exception -> 0x008e }
        r12 = r12.equals(r13);	 Catch:{ Exception -> 0x008e }
        if (r12 == 0) goto L_0x007c;	 Catch:{ Exception -> 0x008e }
    L_0x0025:
        r6 = r15;	 Catch:{ Exception -> 0x008e }
        if (r17 == 0) goto L_0x0075;	 Catch:{ Exception -> 0x008e }
    L_0x0028:
        r9 = r17;	 Catch:{ Exception -> 0x008e }
    L_0x002a:
        r2 = 0;	 Catch:{ Exception -> 0x008e }
        r8 = new com.android.org.conscrypt.TrustedCertificateIndex;	 Catch:{ Exception -> 0x008e }
        r8.<init>();	 Catch:{ Exception -> 0x008e }
        r7 = r8;
    L_0x0031:
        if (r18 != 0) goto L_0x0037;
    L_0x0033:
        r18 = com.android.org.conscrypt.CertBlacklist.getDefault();
    L_0x0037:
        if (r19 != 0) goto L_0x003e;
    L_0x0039:
        r19 = new com.android.org.conscrypt.ct.CTLogStoreImpl;
        r19.<init>();
    L_0x003e:
        if (r21 != 0) goto L_0x004a;
    L_0x0040:
        r21 = new com.android.org.conscrypt.ct.CTPolicyImpl;
        r12 = 2;
        r0 = r21;
        r1 = r19;
        r0.<init>(r1, r12);
    L_0x004a:
        r0 = r16;
        r14.pinManager = r0;
        r14.rootKeyStore = r6;
        r14.trustedCertificateStore = r9;
        r14.validator = r11;
        r14.factory = r5;
        r14.trustedCertificateIndex = r7;
        r12 = new com.android.org.conscrypt.TrustedCertificateIndex;
        r12.<init>();
        r14.intermediateIndex = r12;
        r14.acceptedIssuers = r2;
        r14.err = r4;
        r0 = r18;
        r14.blacklist = r0;
        r12 = new com.android.org.conscrypt.ct.CTVerifier;
        r0 = r19;
        r12.<init>(r0);
        r14.ctVerifier = r12;
        r0 = r21;
        r14.ctPolicy = r0;
        return;
    L_0x0075:
        r10 = new com.android.org.conscrypt.TrustedCertificateStore;	 Catch:{ Exception -> 0x008e }
        r10.<init>();	 Catch:{ Exception -> 0x008e }
        r9 = r10;	 Catch:{ Exception -> 0x008e }
        goto L_0x002a;	 Catch:{ Exception -> 0x008e }
    L_0x007c:
        r6 = 0;	 Catch:{ Exception -> 0x008e }
        r9 = r17;	 Catch:{ Exception -> 0x008e }
        r2 = acceptedIssuers(r15);	 Catch:{ Exception -> 0x008e }
        r8 = new com.android.org.conscrypt.TrustedCertificateIndex;	 Catch:{ Exception -> 0x008e }
        r12 = trustAnchors(r2);	 Catch:{ Exception -> 0x008e }
        r8.<init>(r12);	 Catch:{ Exception -> 0x008e }
        r7 = r8;
        goto L_0x0031;
    L_0x008e:
        r3 = move-exception;
        r4 = r3;
        goto L_0x0031;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.conscrypt.TrustManagerImpl.<init>(java.security.KeyStore, com.android.org.conscrypt.CertPinManager, com.android.org.conscrypt.TrustedCertificateStore, com.android.org.conscrypt.CertBlacklist, com.android.org.conscrypt.ct.CTLogStore, com.android.org.conscrypt.ct.CTVerifier, com.android.org.conscrypt.ct.CTPolicy):void");
    }

    private static X509Certificate[] acceptedIssuers(KeyStore ks) {
        try {
            List<X509Certificate> trusted = new ArrayList();
            Enumeration<String> en = ks.aliases();
            while (en.hasMoreElements()) {
                X509Certificate cert = (X509Certificate) ks.getCertificate((String) en.nextElement());
                if (cert != null) {
                    trusted.add(cert);
                }
            }
            return (X509Certificate[]) trusted.toArray(new X509Certificate[trusted.size()]);
        } catch (KeyStoreException e) {
            return new X509Certificate[0];
        }
    }

    private static Set<TrustAnchor> trustAnchors(X509Certificate[] certs) {
        Set<TrustAnchor> trustAnchors = new HashSet(certs.length);
        for (X509Certificate cert : certs) {
            trustAnchors.add(new TrustAnchor(cert, null));
        }
        return trustAnchors;
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkTrusted(chain, authType, null, null, true);
    }

    public List<X509Certificate> checkClientTrusted(X509Certificate[] chain, String authType, String hostname) throws CertificateException {
        return checkTrusted(chain, null, null, authType, hostname, true);
    }

    private static SSLSession getHandshakeSessionOrThrow(SSLSocket sslSocket) throws CertificateException {
        SSLSession session = sslSocket.getHandshakeSession();
        if (session != null) {
            return session;
        }
        throw new CertificateException("Not in handshake; no session available");
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        SSLSession session = null;
        SSLParameters parameters = null;
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            session = getHandshakeSessionOrThrow(sslSocket);
            parameters = sslSocket.getSSLParameters();
        }
        checkTrusted(chain, authType, session, parameters, true);
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        SSLSession session = engine.getHandshakeSession();
        if (session == null) {
            throw new CertificateException("Not in handshake; no session available");
        }
        checkTrusted(chain, authType, session, engine.getSSLParameters(), true);
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkTrusted(chain, authType, null, null, false);
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, String hostname) throws CertificateException {
        return checkTrusted(chain, null, null, authType, hostname, false);
    }

    public List<X509Certificate> getTrustedChainForServer(X509Certificate[] certs, String authType, Socket socket) throws CertificateException {
        SSLSession session = null;
        SSLParameters parameters = null;
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            session = getHandshakeSessionOrThrow(sslSocket);
            parameters = sslSocket.getSSLParameters();
        }
        return checkTrusted(certs, authType, session, parameters, false);
    }

    public List<X509Certificate> getTrustedChainForServer(X509Certificate[] certs, String authType, SSLEngine engine) throws CertificateException {
        SSLSession session = engine.getHandshakeSession();
        if (session == null) {
            throw new CertificateException("Not in handshake; no session available");
        }
        return checkTrusted(certs, authType, session, engine.getSSLParameters(), false);
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        getTrustedChainForServer(chain, authType, socket);
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        getTrustedChainForServer(chain, authType, engine);
    }

    public boolean isUserAddedCertificate(X509Certificate cert) {
        if (this.trustedCertificateStore == null) {
            return false;
        }
        return this.trustedCertificateStore.isUserAddedCertificate(cert);
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, SSLSession session) throws CertificateException {
        return checkTrusted(chain, authType, session, null, false);
    }

    public void handleTrustStorageUpdate() {
        if (this.acceptedIssuers == null) {
            this.trustedCertificateIndex.reset();
        } else {
            this.trustedCertificateIndex.reset(trustAnchors(this.acceptedIssuers));
        }
    }

    private List<X509Certificate> checkTrusted(X509Certificate[] certs, String authType, SSLSession session, SSLParameters parameters, boolean clientAuth) throws CertificateException {
        byte[] ocspData = null;
        byte[] tlsSctData = null;
        String hostname = null;
        if (session != null) {
            hostname = session.getPeerHost();
            ocspData = getOcspDataFromSession(session);
            tlsSctData = getTlsSctDataFromSession(session);
        }
        if (!(session == null || parameters == null)) {
            String identificationAlgorithm = parameters.getEndpointIdentificationAlgorithm();
            if (!(identificationAlgorithm == null || !"HTTPS".equals(identificationAlgorithm.toUpperCase(Locale.US)) || HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session))) {
                throw new CertificateException("No subjectAltNames on the certificate match");
            }
        }
        return checkTrusted(certs, ocspData, tlsSctData, authType, hostname, clientAuth);
    }

    private byte[] getOcspDataFromSession(SSLSession session) {
        List<byte[]> ocspResponses = null;
        if (session instanceof ActiveSession) {
            ocspResponses = ((ActiveSession) session).getStatusResponses();
        } else {
            try {
                Method m_getResponses = session.getClass().getDeclaredMethod("getStatusResponses", new Class[0]);
                m_getResponses.setAccessible(true);
                Object rawResponses = m_getResponses.invoke(session, new Object[0]);
                if (rawResponses instanceof List) {
                    ocspResponses = (List) rawResponses;
                }
            } catch (NoSuchMethodException e) {
            } catch (InvocationTargetException e2) {
                throw new RuntimeException(e2.getCause());
            }
        }
        if (ocspResponses == null || ocspResponses.isEmpty()) {
            return null;
        }
        return (byte[]) ocspResponses.get(0);
    }

    private byte[] getTlsSctDataFromSession(SSLSession session) {
        if (session instanceof ActiveSession) {
            return ((ActiveSession) session).getPeerSignedCertificateTimestamp();
        }
        byte[] data = null;
        try {
            Method m_getTlsSctData = session.getClass().getDeclaredMethod("getPeerSignedCertificateTimestamp", new Class[0]);
            m_getTlsSctData.setAccessible(true);
            Object rawData = m_getTlsSctData.invoke(session, new Object[0]);
            if (rawData instanceof byte[]) {
                data = (byte[]) rawData;
            }
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e2) {
            throw new RuntimeException(e2.getCause());
        }
        return data;
    }

    private List<X509Certificate> checkTrusted(X509Certificate[] certs, byte[] ocspData, byte[] tlsSctData, String authType, String host, boolean clientAuth) throws CertificateException {
        if (certs == null || certs.length == 0 || authType == null || authType.length() == 0) {
            throw new IllegalArgumentException("null or zero-length parameter");
        } else if (this.err != null) {
            throw new CertificateException(this.err);
        } else {
            Set<X509Certificate> used = new HashSet();
            ArrayList<X509Certificate> untrustedChain = new ArrayList();
            ArrayList<TrustAnchor> trustedChain = new ArrayList();
            X509Certificate leaf = certs[0];
            TrustAnchor leafAsAnchor = findTrustAnchorBySubjectAndPublicKey(leaf);
            if (leafAsAnchor != null) {
                trustedChain.add(leafAsAnchor);
                used.add(leafAsAnchor.getTrustedCert());
            } else {
                untrustedChain.add(leaf);
            }
            used.add(leaf);
            return checkTrustedRecursive(certs, ocspData, tlsSctData, host, clientAuth, untrustedChain, trustedChain, used);
        }
    }

    private java.util.List<java.security.cert.X509Certificate> checkTrustedRecursive(java.security.cert.X509Certificate[] r25, byte[] r26, byte[] r27, java.lang.String r28, boolean r29, java.util.ArrayList<java.security.cert.X509Certificate> r30, java.util.ArrayList<java.security.cert.TrustAnchor> r31, java.util.Set<java.security.cert.X509Certificate> r32) throws java.security.cert.CertificateException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r22_7 'lastException' java.security.cert.CertificateException) in PHI: PHI: (r22_5 'lastException' java.security.cert.CertificateException) = (r22_6 'lastException' java.security.cert.CertificateException), (r22_7 'lastException' java.security.cert.CertificateException), (r22_4 'lastException' java.security.cert.CertificateException), (r22_4 'lastException' java.security.cert.CertificateException) binds: {(r22_6 'lastException' java.security.cert.CertificateException)=B:68:0x00cb, (r22_7 'lastException' java.security.cert.CertificateException)=B:67:0x00cb, (r22_4 'lastException' java.security.cert.CertificateException)=B:66:0x00cb, (r22_4 'lastException' java.security.cert.CertificateException)=B:65:0x00cb}
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
        r24 = this;
        r22 = 0;
        r2 = r31.isEmpty();
        if (r2 == 0) goto L_0x003c;
    L_0x0008:
        r2 = r30.size();
        r2 = r2 + -1;
        r0 = r30;
        r15 = r0.get(r2);
        r15 = (java.security.cert.X509Certificate) r15;
    L_0x0016:
        r0 = r24;
        r0.checkBlacklist(r15);
        r2 = r15.getIssuerDN();
        r3 = r15.getSubjectDN();
        r2 = r2.equals(r3);
        if (r2 == 0) goto L_0x004f;
    L_0x0029:
        r2 = r24;
        r3 = r30;
        r4 = r31;
        r5 = r28;
        r6 = r29;
        r7 = r26;
        r8 = r27;
        r2 = r2.verifyChain(r3, r4, r5, r6, r7, r8);
        return r2;
    L_0x003c:
        r2 = r31.size();
        r2 = r2 + -1;
        r0 = r31;
        r2 = r0.get(r2);
        r2 = (java.security.cert.TrustAnchor) r2;
        r15 = r2.getTrustedCert();
        goto L_0x0016;
    L_0x004f:
        r0 = r24;
        r12 = r0.findAllTrustAnchorsByIssuerAndSignature(r15);
        r23 = 0;
        r2 = sortPotentialAnchors(r12);
        r10 = r2.iterator();
    L_0x005f:
        r2 = r10.hasNext();
        if (r2 == 0) goto L_0x009c;
    L_0x0065:
        r9 = r10.next();
        r9 = (java.security.cert.TrustAnchor) r9;
        r11 = r9.getTrustedCert();
        r0 = r32;
        r2 = r0.contains(r11);
        if (r2 != 0) goto L_0x005f;
    L_0x0077:
        r23 = 1;
        r0 = r32;
        r0.add(r11);
        r0 = r31;
        r0.add(r9);
        r2 = r24.checkTrustedRecursive(r25, r26, r27, r28, r29, r30, r31, r32);	 Catch:{ CertificateException -> 0x0088 }
        return r2;
    L_0x0088:
        r16 = move-exception;
        r22 = r16;
        r2 = r31.size();
        r2 = r2 + -1;
        r0 = r31;
        r0.remove(r2);
        r0 = r32;
        r0.remove(r11);
        goto L_0x005f;
    L_0x009c:
        r2 = r31.isEmpty();
        if (r2 != 0) goto L_0x00b8;
    L_0x00a2:
        if (r23 != 0) goto L_0x00b7;
    L_0x00a4:
        r2 = r24;
        r3 = r30;
        r4 = r31;
        r5 = r28;
        r6 = r29;
        r7 = r26;
        r8 = r27;
        r2 = r2.verifyChain(r3, r4, r5, r6, r7, r8);
        return r2;
    L_0x00b7:
        throw r22;
    L_0x00b8:
        r17 = 1;
    L_0x00ba:
        r0 = r25;
        r2 = r0.length;
        r0 = r17;
        if (r0 >= r2) goto L_0x0128;
    L_0x00c1:
        r13 = r25[r17];
        r0 = r32;
        r2 = r0.contains(r13);
        if (r2 == 0) goto L_0x00ce;
    L_0x00cb:
        r17 = r17 + 1;
        goto L_0x00ba;
    L_0x00ce:
        r2 = r15.getIssuerDN();
        r3 = r13.getSubjectDN();
        r2 = r2.equals(r3);
        if (r2 == 0) goto L_0x00cb;
    L_0x00dc:
        r13.checkValidity();	 Catch:{ CertificateException -> 0x00f1 }
        com.android.org.conscrypt.ChainStrengthAnalyzer.checkCert(r13);	 Catch:{ CertificateException -> 0x00f1 }
        r0 = r32;
        r0.add(r13);
        r0 = r30;
        r0.add(r13);
        r2 = r24.checkTrustedRecursive(r25, r26, r27, r28, r29, r30, r31, r32);	 Catch:{ CertificateException -> 0x0114 }
        return r2;
    L_0x00f1:
        r16 = move-exception;
        r22 = new java.security.cert.CertificateException;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Unacceptable certificate: ";
        r2 = r2.append(r3);
        r3 = r13.getSubjectX500Principal();
        r2 = r2.append(r3);
        r2 = r2.toString();
        r0 = r22;
        r1 = r16;
        r0.<init>(r2, r1);
        goto L_0x00cb;
    L_0x0114:
        r16 = move-exception;
        r22 = r16;
        r0 = r32;
        r0.remove(r13);
        r2 = r30.size();
        r2 = r2 + -1;
        r0 = r30;
        r0.remove(r2);
        goto L_0x00cb;
    L_0x0128:
        r0 = r24;
        r2 = r0.intermediateIndex;
        r20 = r2.findAllByIssuerAndSignature(r15);
        r2 = sortPotentialAnchors(r20);
        r19 = r2.iterator();
    L_0x0138:
        r2 = r19.hasNext();
        if (r2 == 0) goto L_0x017b;
    L_0x013e:
        r18 = r19.next();
        r18 = (java.security.cert.TrustAnchor) r18;
        r21 = r18.getTrustedCert();
        r0 = r32;
        r1 = r21;
        r2 = r0.contains(r1);
        if (r2 != 0) goto L_0x0138;
    L_0x0152:
        r0 = r32;
        r1 = r21;
        r0.add(r1);
        r0 = r30;
        r1 = r21;
        r0.add(r1);
        r2 = r24.checkTrustedRecursive(r25, r26, r27, r28, r29, r30, r31, r32);	 Catch:{ CertificateException -> 0x0165 }
        return r2;
    L_0x0165:
        r16 = move-exception;
        r22 = r16;
        r2 = r30.size();
        r2 = r2 + -1;
        r0 = r30;
        r0.remove(r2);
        r0 = r32;
        r1 = r21;
        r0.remove(r1);
        goto L_0x0138;
    L_0x017b:
        if (r22 == 0) goto L_0x017e;
    L_0x017d:
        throw r22;
    L_0x017e:
        r0 = r24;
        r2 = r0.factory;
        r0 = r30;
        r14 = r2.generateCertPath(r0);
        r2 = new java.security.cert.CertificateException;
        r3 = new java.security.cert.CertPathValidatorException;
        r4 = "Trust anchor for certification path not found.";
        r5 = 0;
        r6 = -1;
        r3.<init>(r4, r5, r14, r6);
        r2.<init>(r3);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.conscrypt.TrustManagerImpl.checkTrustedRecursive(java.security.cert.X509Certificate[], byte[], byte[], java.lang.String, boolean, java.util.ArrayList, java.util.ArrayList, java.util.Set):java.util.List<java.security.cert.X509Certificate>");
    }

    private List<X509Certificate> verifyChain(List<X509Certificate> untrustedChain, List<TrustAnchor> trustAnchorChain, String host, boolean clientAuth, byte[] ocspData, byte[] tlsSctData) throws CertificateException {
        CertPath certPath = this.factory.generateCertPath(untrustedChain);
        if (trustAnchorChain.isEmpty()) {
            throw new CertificateException(new CertPathValidatorException("Trust anchor for certification path not found.", null, certPath, -1));
        }
        List<X509Certificate> wholeChain = new ArrayList();
        wholeChain.addAll(untrustedChain);
        for (TrustAnchor anchor : trustAnchorChain) {
            wholeChain.add(anchor.getTrustedCert());
        }
        if (this.pinManager != null) {
            this.pinManager.checkChainPinning(host, wholeChain);
        }
        for (X509Certificate cert : wholeChain) {
            checkBlacklist(cert);
        }
        if (!clientAuth && (this.ctEnabledOverride || (host != null && Platform.isCTVerificationRequired(host)))) {
            checkCT(host, wholeChain, ocspData, tlsSctData);
        }
        if (untrustedChain.isEmpty()) {
            return wholeChain;
        }
        ChainStrengthAnalyzer.check((List) untrustedChain);
        try {
            Set<TrustAnchor> anchorSet = new HashSet();
            anchorSet.add((TrustAnchor) trustAnchorChain.get(0));
            PKIXParameters params = new PKIXParameters(anchorSet);
            params.setRevocationEnabled(false);
            X509Certificate endPointCert = (X509Certificate) untrustedChain.get(0);
            setOcspResponses(params, endPointCert, ocspData);
            params.addCertPathChecker(new ExtendedKeyUsagePKIXCertPathChecker(clientAuth, endPointCert, null));
            this.validator.validate(certPath, params);
            for (int i = 1; i < untrustedChain.size(); i++) {
                this.intermediateIndex.index((X509Certificate) untrustedChain.get(i));
            }
            return wholeChain;
        } catch (InvalidAlgorithmParameterException e) {
            throw new CertificateException("Chain validation failed", e);
        } catch (CertPathValidatorException e2) {
            throw new CertificateException("Chain validation failed", e2);
        }
    }

    private void checkBlacklist(X509Certificate cert) throws CertificateException {
        if (this.blacklist.isPublicKeyBlackListed(cert.getPublicKey())) {
            throw new CertificateException("Certificate blacklisted by public key: " + cert);
        }
    }

    private void checkCT(String host, List<X509Certificate> chain, byte[] ocspData, byte[] tlsData) throws CertificateException {
        if (!this.ctPolicy.doesResultConformToPolicy(this.ctVerifier.verifySignedCertificateTimestamps((List) chain, tlsData, ocspData), host, (X509Certificate[]) chain.toArray(new X509Certificate[chain.size()]))) {
            throw new CertificateException("Certificate chain does not conform to required transparency policy.");
        }
    }

    private void setOcspResponses(PKIXParameters params, X509Certificate cert, byte[] ocspData) {
        if (ocspData != null) {
            PKIXRevocationChecker revChecker = null;
            List<PKIXCertPathChecker> checkers = new ArrayList(params.getCertPathCheckers());
            for (PKIXCertPathChecker checker : checkers) {
                if (checker instanceof PKIXRevocationChecker) {
                    revChecker = (PKIXRevocationChecker) checker;
                    break;
                }
            }
            if (revChecker == null) {
                try {
                    revChecker = (PKIXRevocationChecker) this.validator.getRevocationChecker();
                    checkers.add(revChecker);
                    revChecker.setOptions(Collections.singleton(Option.ONLY_END_ENTITY));
                } catch (UnsupportedOperationException e) {
                    return;
                }
            }
            revChecker.setOcspResponses(Collections.singletonMap(cert, ocspData));
            params.setCertPathCheckers(checkers);
        }
    }

    private static Collection<TrustAnchor> sortPotentialAnchors(Set<TrustAnchor> anchors) {
        if (anchors.size() <= 1) {
            return anchors;
        }
        List<TrustAnchor> sortedAnchors = new ArrayList(anchors);
        Collections.sort(sortedAnchors, TRUST_ANCHOR_COMPARATOR);
        return sortedAnchors;
    }

    private Set<TrustAnchor> findAllTrustAnchorsByIssuerAndSignature(X509Certificate cert) {
        Set<TrustAnchor> indexedAnchors = this.trustedCertificateIndex.findAllByIssuerAndSignature(cert);
        if (!indexedAnchors.isEmpty() || this.trustedCertificateStore == null) {
            return indexedAnchors;
        }
        Set<X509Certificate> storeAnchors = this.trustedCertificateStore.findAllIssuers(cert);
        if (storeAnchors.isEmpty()) {
            return indexedAnchors;
        }
        Set<TrustAnchor> result = new HashSet(storeAnchors.size());
        for (X509Certificate storeCert : storeAnchors) {
            result.add(this.trustedCertificateIndex.index(storeCert));
        }
        return result;
    }

    private TrustAnchor findTrustAnchorBySubjectAndPublicKey(X509Certificate cert) {
        TrustAnchor trustAnchor = this.trustedCertificateIndex.findBySubjectAndPublicKey(cert);
        if (trustAnchor != null) {
            return trustAnchor;
        }
        if (this.trustedCertificateStore == null) {
            return null;
        }
        X509Certificate systemCert = this.trustedCertificateStore.getTrustAnchor(cert);
        if (systemCert != null) {
            return new TrustAnchor(systemCert, null);
        }
        return null;
    }

    public X509Certificate[] getAcceptedIssuers() {
        return this.acceptedIssuers != null ? (X509Certificate[]) this.acceptedIssuers.clone() : acceptedIssuers(this.rootKeyStore);
    }

    public void setCTEnabledOverride(boolean enabled) {
        this.ctEnabledOverride = enabled;
    }

    public void setCTVerifier(CTVerifier verifier) {
        this.ctVerifier = verifier;
    }

    public void setCTPolicy(CTPolicy policy) {
        this.ctPolicy = policy;
    }
}
