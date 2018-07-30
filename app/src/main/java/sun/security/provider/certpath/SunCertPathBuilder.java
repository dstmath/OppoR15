package sun.security.provider.certpath;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathBuilderSpi;
import java.security.cert.CertPathChecker;
import java.security.cert.CertPathParameters;
import java.security.cert.CertSelector;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PolicyNode;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import sun.security.util.Debug;

public final class SunCertPathBuilder extends CertPathBuilderSpi {
    private static final Debug debug = Debug.getInstance("certpath");
    private BuilderParams buildParams;
    private CertificateFactory cf;
    private PublicKey finalPublicKey;
    private boolean pathCompleted = false;
    private PolicyNode policyTreeResult;
    private TrustAnchor trustAnchor;

    public SunCertPathBuilder() throws CertPathBuilderException {
        try {
            this.cf = CertificateFactory.getInstance("X.509");
        } catch (Throwable e) {
            throw new CertPathBuilderException(e);
        }
    }

    public CertPathChecker engineGetRevocationChecker() {
        return new RevocationChecker();
    }

    public CertPathBuilderResult engineBuild(CertPathParameters params) throws CertPathBuilderException, InvalidAlgorithmParameterException {
        if (debug != null) {
            debug.println("SunCertPathBuilder.engineBuild(" + params + ")");
        }
        this.buildParams = PKIX.checkBuilderParams(params);
        return build();
    }

    private PKIXCertPathBuilderResult build() throws CertPathBuilderException {
        List<List<Vertex>> adjList = new ArrayList();
        PKIXCertPathBuilderResult result = buildCertPath(false, adjList);
        if (result == null) {
            if (debug != null) {
                debug.println("SunCertPathBuilder.engineBuild: 2nd pass; try building again searching all certstores");
            }
            adjList.clear();
            result = buildCertPath(true, adjList);
            if (result == null) {
                throw new SunCertPathBuilderException("unable to find valid certification path to requested target", new AdjacencyList(adjList));
            }
        }
        return result;
    }

    private PKIXCertPathBuilderResult buildCertPath(boolean searchAllCertStores, List<List<Vertex>> adjList) throws CertPathBuilderException {
        this.pathCompleted = false;
        this.trustAnchor = null;
        this.finalPublicKey = null;
        this.policyTreeResult = null;
        List certPathList = new LinkedList();
        try {
            buildForward(adjList, certPathList, searchAllCertStores);
            try {
                if (!this.pathCompleted) {
                    return null;
                }
                if (debug != null) {
                    debug.println("SunCertPathBuilder.engineBuild() pathCompleted");
                }
                Collections.reverse(certPathList);
                return new SunCertPathBuilderResult(this.cf.generateCertPath(certPathList), this.trustAnchor, this.policyTreeResult, this.finalPublicKey, new AdjacencyList(adjList));
            } catch (CertificateException e) {
                if (debug != null) {
                    debug.println("SunCertPathBuilder.engineBuild() exception in wrap-up");
                    e.printStackTrace();
                }
                throw new SunCertPathBuilderException("unable to find valid certification path to requested target", e, new AdjacencyList(adjList));
            }
        } catch (Exception e2) {
            if (debug != null) {
                debug.println("SunCertPathBuilder.engineBuild() exception in build");
                e2.printStackTrace();
            }
            throw new SunCertPathBuilderException("unable to find valid certification path to requested target", e2, new AdjacencyList(adjList));
        }
    }

    private void buildForward(List<List<Vertex>> adjacencyList, LinkedList<X509Certificate> certPathList, boolean searchAllCertStores) throws GeneralSecurityException, IOException {
        if (debug != null) {
            debug.println("SunCertPathBuilder.buildForward()...");
        }
        ForwardState currentState = new ForwardState();
        currentState.initState(this.buildParams.certPathCheckers());
        adjacencyList.clear();
        adjacencyList.add(new LinkedList());
        depthFirstSearchForward(this.buildParams.targetSubject(), currentState, new ForwardBuilder(this.buildParams, searchAllCertStores), adjacencyList, certPathList);
    }

    private void depthFirstSearchForward(javax.security.auth.x500.X500Principal r45, sun.security.provider.certpath.ForwardState r46, sun.security.provider.certpath.ForwardBuilder r47, java.util.List<java.util.List<sun.security.provider.certpath.Vertex>> r48, java.util.LinkedList<java.security.cert.X509Certificate> r49) throws java.security.GeneralSecurityException, java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r20_1 'basicChecker' sun.security.provider.certpath.BasicChecker) in PHI: PHI: (r20_2 'basicChecker' sun.security.provider.certpath.BasicChecker) = (r20_0 'basicChecker' sun.security.provider.certpath.BasicChecker), (r20_1 'basicChecker' sun.security.provider.certpath.BasicChecker) binds: {(r20_0 'basicChecker' sun.security.provider.certpath.BasicChecker)=B:21:0x0128, (r20_1 'basicChecker' sun.security.provider.certpath.BasicChecker)=B:27:0x0161}
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
        r44 = this;
        r5 = debug;
        if (r5 == 0) goto L_0x0035;
    L_0x0004:
        r5 = debug;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "SunCertPathBuilder.depthFirstSearchForward(";
        r6 = r6.append(r7);
        r0 = r45;
        r6 = r6.append(r0);
        r7 = ", ";
        r6 = r6.append(r7);
        r7 = r46.toString();
        r6 = r6.append(r7);
        r7 = ")";
        r6 = r6.append(r7);
        r6 = r6.toString();
        r5.println(r6);
    L_0x0035:
        r0 = r44;
        r5 = r0.buildParams;
        r5 = r5.certStores();
        r0 = r47;
        r1 = r46;
        r22 = r0.getMatchingCerts(r1, r5);
        r0 = r22;
        r1 = r48;
        r43 = addVertices(r0, r1);
        r5 = debug;
        if (r5 == 0) goto L_0x006e;
    L_0x0051:
        r5 = debug;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "SunCertPathBuilder.depthFirstSearchForward(): certs.size=";
        r6 = r6.append(r7);
        r7 = r43.size();
        r6 = r6.append(r7);
        r6 = r6.toString();
        r5.println(r6);
    L_0x006e:
        r42 = r43.iterator();
    L_0x0072:
        r5 = r42.hasNext();
        if (r5 == 0) goto L_0x044d;
    L_0x0078:
        r41 = r42.next();
        r41 = (sun.security.provider.certpath.Vertex) r41;
        r36 = r46.clone();
        r36 = (sun.security.provider.certpath.ForwardState) r36;
        r21 = r41.getCertificate();
        r0 = r47;	 Catch:{ GeneralSecurityException -> 0x01cc }
        r1 = r21;	 Catch:{ GeneralSecurityException -> 0x01cc }
        r2 = r36;	 Catch:{ GeneralSecurityException -> 0x01cc }
        r3 = r49;	 Catch:{ GeneralSecurityException -> 0x01cc }
        r0.verifyCert(r1, r2, r3);	 Catch:{ GeneralSecurityException -> 0x01cc }
        r0 = r47;
        r1 = r21;
        r5 = r0.isPathCompleted(r1);
        if (r5 == 0) goto L_0x03fb;
    L_0x009d:
        r5 = debug;
        if (r5 == 0) goto L_0x00a9;
    L_0x00a1:
        r5 = debug;
        r6 = "SunCertPathBuilder.depthFirstSearchForward(): commencing final verification";
        r5.println(r6);
    L_0x00a9:
        r19 = new java.util.ArrayList;
        r0 = r19;
        r1 = r49;
        r0.<init>(r1);
        r0 = r47;
        r5 = r0.trustAnchor;
        r5 = r5.getTrustedCert();
        if (r5 != 0) goto L_0x00c4;
    L_0x00bc:
        r5 = 0;
        r0 = r19;
        r1 = r21;
        r0.add(r5, r1);
    L_0x00c4:
        r5 = "2.5.29.32.0";
        r9 = java.util.Collections.singleton(r5);
        r4 = new sun.security.provider.certpath.PolicyNodeImpl;
        r6 = "2.5.29.32.0";
        r5 = 0;
        r7 = 0;
        r8 = 0;
        r10 = 0;
        r4.<init>(r5, r6, r7, r8, r9, r10);
        r25 = new java.util.ArrayList;
        r25.<init>();
        r10 = new sun.security.provider.certpath.PolicyChecker;
        r0 = r44;
        r5 = r0.buildParams;
        r11 = r5.initialPolicies();
        r12 = r19.size();
        r0 = r44;
        r5 = r0.buildParams;
        r13 = r5.explicitPolicyRequired();
        r0 = r44;
        r5 = r0.buildParams;
        r14 = r5.policyMappingInhibited();
        r0 = r44;
        r5 = r0.buildParams;
        r15 = r5.anyPolicyInhibited();
        r0 = r44;
        r5 = r0.buildParams;
        r16 = r5.policyQualifiersRejected();
        r17 = r4;
        r10.<init>(r11, r12, r13, r14, r15, r16, r17);
        r0 = r25;
        r0.add(r10);
        r5 = new sun.security.provider.certpath.AlgorithmChecker;
        r0 = r47;
        r6 = r0.trustAnchor;
        r5.<init>(r6);
        r0 = r25;
        r0.add(r5);
        r20 = 0;
        r5 = r36.keyParamsNeeded();
        if (r5 == 0) goto L_0x0190;
    L_0x012a:
        r38 = r21.getPublicKey();
        r0 = r47;
        r5 = r0.trustAnchor;
        r5 = r5.getTrustedCert();
        if (r5 != 0) goto L_0x0161;
    L_0x0138:
        r0 = r47;
        r5 = r0.trustAnchor;
        r38 = r5.getCAPublicKey();
        r5 = debug;
        if (r5 == 0) goto L_0x0161;
    L_0x0144:
        r5 = debug;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "SunCertPathBuilder.depthFirstSearchForward using buildParams public key: ";
        r6 = r6.append(r7);
        r7 = r38.toString();
        r6 = r6.append(r7);
        r6 = r6.toString();
        r5.println(r6);
    L_0x0161:
        r18 = new java.security.cert.TrustAnchor;
        r5 = r21.getSubjectX500Principal();
        r6 = 0;
        r0 = r18;
        r1 = r38;
        r0.<init>(r5, r1, r6);
        r20 = new sun.security.provider.certpath.BasicChecker;
        r0 = r44;
        r5 = r0.buildParams;
        r5 = r5.date();
        r0 = r44;
        r6 = r0.buildParams;
        r6 = r6.sigProvider();
        r7 = 1;
        r0 = r20;
        r1 = r18;
        r0.<init>(r1, r5, r6, r7);
        r0 = r25;
        r1 = r20;
        r0.add(r1);
    L_0x0190:
        r0 = r44;
        r5 = r0.buildParams;
        r0 = r44;
        r6 = r0.cf;
        r0 = r19;
        r6 = r6.generateCertPath(r0);
        r5.setCertPath(r6);
        r37 = 0;
        r0 = r44;
        r5 = r0.buildParams;
        r28 = r5.certPathCheckers();
        r27 = r28.iterator();
    L_0x01af:
        r5 = r27.hasNext();
        if (r5 == 0) goto L_0x0210;
    L_0x01b5:
        r26 = r27.next();
        r26 = (java.security.cert.PKIXCertPathChecker) r26;
        r0 = r26;
        r5 = r0 instanceof java.security.cert.PKIXRevocationChecker;
        if (r5 == 0) goto L_0x01af;
    L_0x01c1:
        if (r37 == 0) goto L_0x01f8;
    L_0x01c3:
        r5 = new java.security.cert.CertPathValidatorException;
        r6 = "Only one PKIXRevocationChecker can be specified";
        r5.<init>(r6);
        throw r5;
    L_0x01cc:
        r34 = move-exception;
        r5 = debug;
        if (r5 == 0) goto L_0x01ef;
    L_0x01d1:
        r5 = debug;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "SunCertPathBuilder.depthFirstSearchForward(): validation failed: ";
        r6 = r6.append(r7);
        r0 = r34;
        r6 = r6.append(r0);
        r6 = r6.toString();
        r5.println(r6);
        r34.printStackTrace();
    L_0x01ef:
        r0 = r41;
        r1 = r34;
        r0.setThrowable(r1);
        goto L_0x0072;
    L_0x01f8:
        r37 = 1;
        r0 = r26;
        r5 = r0 instanceof sun.security.provider.certpath.RevocationChecker;
        if (r5 == 0) goto L_0x01af;
    L_0x0200:
        r26 = (sun.security.provider.certpath.RevocationChecker) r26;
        r0 = r47;
        r5 = r0.trustAnchor;
        r0 = r44;
        r6 = r0.buildParams;
        r0 = r26;
        r0.init(r5, r6);
        goto L_0x01af;
    L_0x0210:
        r0 = r44;
        r5 = r0.buildParams;
        r5 = r5.revocationEnabled();
        if (r5 == 0) goto L_0x0230;
    L_0x021a:
        r5 = r37 ^ 1;
        if (r5 == 0) goto L_0x0230;
    L_0x021e:
        r5 = new sun.security.provider.certpath.RevocationChecker;
        r0 = r47;
        r6 = r0.trustAnchor;
        r0 = r44;
        r7 = r0.buildParams;
        r5.<init>(r6, r7);
        r0 = r25;
        r0.add(r5);
    L_0x0230:
        r0 = r25;
        r1 = r28;
        r0.addAll(r1);
        r35 = 0;
    L_0x0239:
        r5 = r19.size();
        r0 = r35;
        if (r0 >= r5) goto L_0x039e;
    L_0x0241:
        r0 = r19;
        r1 = r35;
        r30 = r0.get(r1);
        r30 = (java.security.cert.X509Certificate) r30;
        r5 = debug;
        if (r5 == 0) goto L_0x026c;
    L_0x024f:
        r5 = debug;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "current subject = ";
        r6 = r6.append(r7);
        r7 = r30.getSubjectX500Principal();
        r6 = r6.append(r7);
        r6 = r6.toString();
        r5.println(r6);
    L_0x026c:
        r40 = r30.getCriticalExtensionOIDs();
        if (r40 != 0) goto L_0x0276;
    L_0x0272:
        r40 = java.util.Collections.emptySet();
    L_0x0276:
        r32 = r25.iterator();
    L_0x027a:
        r5 = r32.hasNext();
        if (r5 == 0) goto L_0x02f1;
    L_0x0280:
        r31 = r32.next();
        r31 = (java.security.cert.PKIXCertPathChecker) r31;
        r5 = r31.isForwardCheckingSupported();
        if (r5 != 0) goto L_0x027a;
    L_0x028c:
        if (r35 != 0) goto L_0x02a5;
    L_0x028e:
        r5 = 0;
        r0 = r31;
        r0.init(r5);
        r0 = r31;
        r5 = r0 instanceof sun.security.provider.certpath.AlgorithmChecker;
        if (r5 == 0) goto L_0x02a5;
    L_0x029a:
        r5 = r31;
        r5 = (sun.security.provider.certpath.AlgorithmChecker) r5;
        r0 = r47;
        r6 = r0.trustAnchor;
        r5.trySetTrustAnchor(r6);
    L_0x02a5:
        r0 = r31;	 Catch:{ CertPathValidatorException -> 0x02af }
        r1 = r30;	 Catch:{ CertPathValidatorException -> 0x02af }
        r2 = r40;	 Catch:{ CertPathValidatorException -> 0x02af }
        r0.check(r1, r2);	 Catch:{ CertPathValidatorException -> 0x02af }
        goto L_0x027a;
    L_0x02af:
        r29 = move-exception;
        r5 = debug;
        if (r5 == 0) goto L_0x02cf;
    L_0x02b4:
        r5 = debug;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "SunCertPathBuilder.depthFirstSearchForward(): final verification failed: ";
        r6 = r6.append(r7);
        r0 = r29;
        r6 = r6.append(r0);
        r6 = r6.toString();
        r5.println(r6);
    L_0x02cf:
        r0 = r44;
        r5 = r0.buildParams;
        r5 = r5.targetCertConstraints();
        r0 = r30;
        r5 = r5.match(r0);
        if (r5 == 0) goto L_0x02e8;
    L_0x02df:
        r5 = r29.getReason();
        r6 = java.security.cert.CertPathValidatorException.BasicReason.REVOKED;
        if (r5 != r6) goto L_0x02e8;
    L_0x02e7:
        throw r29;
    L_0x02e8:
        r0 = r41;
        r1 = r29;
        r0.setThrowable(r1);
        goto L_0x0072;
    L_0x02f1:
        r0 = r44;
        r5 = r0.buildParams;
        r5 = r5.certPathCheckers();
        r24 = r5.iterator();
    L_0x02fd:
        r5 = r24.hasNext();
        if (r5 == 0) goto L_0x031d;
    L_0x0303:
        r23 = r24.next();
        r23 = (java.security.cert.PKIXCertPathChecker) r23;
        r5 = r23.isForwardCheckingSupported();
        if (r5 == 0) goto L_0x02fd;
    L_0x030f:
        r39 = r23.getSupportedExtensions();
        if (r39 == 0) goto L_0x02fd;
    L_0x0315:
        r0 = r40;
        r1 = r39;
        r0.removeAll(r1);
        goto L_0x02fd;
    L_0x031d:
        r5 = r40.isEmpty();
        if (r5 != 0) goto L_0x039a;
    L_0x0323:
        r5 = sun.security.x509.PKIXExtensions.BasicConstraints_Id;
        r5 = r5.toString();
        r0 = r40;
        r0.remove(r5);
        r5 = sun.security.x509.PKIXExtensions.NameConstraints_Id;
        r5 = r5.toString();
        r0 = r40;
        r0.remove(r5);
        r5 = sun.security.x509.PKIXExtensions.CertificatePolicies_Id;
        r5 = r5.toString();
        r0 = r40;
        r0.remove(r5);
        r5 = sun.security.x509.PKIXExtensions.PolicyMappings_Id;
        r5 = r5.toString();
        r0 = r40;
        r0.remove(r5);
        r5 = sun.security.x509.PKIXExtensions.PolicyConstraints_Id;
        r5 = r5.toString();
        r0 = r40;
        r0.remove(r5);
        r5 = sun.security.x509.PKIXExtensions.InhibitAnyPolicy_Id;
        r5 = r5.toString();
        r0 = r40;
        r0.remove(r5);
        r5 = sun.security.x509.PKIXExtensions.SubjectAlternativeName_Id;
        r5 = r5.toString();
        r0 = r40;
        r0.remove(r5);
        r5 = sun.security.x509.PKIXExtensions.KeyUsage_Id;
        r5 = r5.toString();
        r0 = r40;
        r0.remove(r5);
        r5 = sun.security.x509.PKIXExtensions.ExtendedKeyUsage_Id;
        r5 = r5.toString();
        r0 = r40;
        r0.remove(r5);
        r5 = r40.isEmpty();
        if (r5 != 0) goto L_0x039a;
    L_0x038c:
        r11 = new java.security.cert.CertPathValidatorException;
        r12 = "unrecognized critical extension(s)";
        r16 = java.security.cert.PKIXReason.UNRECOGNIZED_CRIT_EXT;
        r13 = 0;
        r14 = 0;
        r15 = -1;
        r11.<init>(r12, r13, r14, r15, r16);
        throw r11;
    L_0x039a:
        r35 = r35 + 1;
        goto L_0x0239;
    L_0x039e:
        r5 = debug;
        if (r5 == 0) goto L_0x03aa;
    L_0x03a2:
        r5 = debug;
        r6 = "SunCertPathBuilder.depthFirstSearchForward(): final verification succeeded - path completed!";
        r5.println(r6);
    L_0x03aa:
        r5 = 1;
        r0 = r44;
        r0.pathCompleted = r5;
        r0 = r47;
        r5 = r0.trustAnchor;
        r5 = r5.getTrustedCert();
        if (r5 != 0) goto L_0x03c2;
    L_0x03b9:
        r0 = r47;
        r1 = r21;
        r2 = r49;
        r0.addCertToPath(r1, r2);
    L_0x03c2:
        r0 = r47;
        r5 = r0.trustAnchor;
        r0 = r44;
        r0.trustAnchor = r5;
        if (r20 == 0) goto L_0x03dd;
    L_0x03cc:
        r5 = r20.getPublicKey();
        r0 = r44;
        r0.finalPublicKey = r5;
    L_0x03d4:
        r5 = r10.getPolicyTree();
        r0 = r44;
        r0.policyTreeResult = r5;
        return;
    L_0x03dd:
        r5 = r49.isEmpty();
        if (r5 == 0) goto L_0x03f4;
    L_0x03e3:
        r0 = r47;
        r5 = r0.trustAnchor;
        r33 = r5.getTrustedCert();
    L_0x03eb:
        r5 = r33.getPublicKey();
        r0 = r44;
        r0.finalPublicKey = r5;
        goto L_0x03d4;
    L_0x03f4:
        r33 = r49.getLast();
        r33 = (java.security.cert.Certificate) r33;
        goto L_0x03eb;
    L_0x03fb:
        r0 = r47;
        r1 = r21;
        r2 = r49;
        r0.addCertToPath(r1, r2);
        r0 = r36;
        r1 = r21;
        r0.updateState(r1);
        r5 = new java.util.LinkedList;
        r5.<init>();
        r0 = r48;
        r0.add(r5);
        r5 = r48.size();
        r5 = r5 + -1;
        r0 = r41;
        r0.setIndex(r5);
        r12 = r21.getIssuerX500Principal();
        r11 = r44;
        r13 = r36;
        r14 = r47;
        r15 = r48;
        r16 = r49;
        r11.depthFirstSearchForward(r12, r13, r14, r15, r16);
        r0 = r44;
        r5 = r0.pathCompleted;
        if (r5 == 0) goto L_0x0438;
    L_0x0437:
        return;
    L_0x0438:
        r5 = debug;
        if (r5 == 0) goto L_0x0444;
    L_0x043c:
        r5 = debug;
        r6 = "SunCertPathBuilder.depthFirstSearchForward(): backtracking";
        r5.println(r6);
    L_0x0444:
        r0 = r47;
        r1 = r49;
        r0.removeFinalCertFromPath(r1);
        goto L_0x0072;
    L_0x044d:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: sun.security.provider.certpath.SunCertPathBuilder.depthFirstSearchForward(javax.security.auth.x500.X500Principal, sun.security.provider.certpath.ForwardState, sun.security.provider.certpath.ForwardBuilder, java.util.List, java.util.LinkedList):void");
    }

    private static List<Vertex> addVertices(Collection<X509Certificate> certs, List<List<Vertex>> adjList) {
        List<Vertex> l = (List) adjList.get(adjList.size() - 1);
        for (X509Certificate cert : certs) {
            l.add(new Vertex(cert));
        }
        return l;
    }

    private static boolean anchorIsTarget(TrustAnchor anchor, CertSelector sel) {
        X509Certificate anchorCert = anchor.getTrustedCert();
        if (anchorCert != null) {
            return sel.match(anchorCert);
        }
        return false;
    }
}
