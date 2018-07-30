package sun.security.provider.certpath;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.x509.AuthorityKeyIdentifierExtension;
import sun.security.x509.CRLDistributionPointsExtension;
import sun.security.x509.DistributionPoint;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.RDN;
import sun.security.x509.URIName;
import sun.security.x509.X500Name;
import sun.security.x509.X509CRLImpl;
import sun.security.x509.X509CertImpl;

public class DistributionPointFetcher {
    private static final boolean[] ALL_REASONS = new boolean[]{true, true, true, true, true, true, true, true, true};
    private static final Debug debug = Debug.getInstance("certpath");

    private DistributionPointFetcher() {
    }

    public static Collection<X509CRL> getCRLs(X509CRLSelector selector, boolean signFlag, PublicKey prevKey, String provider, List<CertStore> certStores, boolean[] reasonsMask, Set<TrustAnchor> trustAnchors, Date validity) throws CertStoreException {
        return getCRLs(selector, signFlag, prevKey, null, provider, certStores, reasonsMask, trustAnchors, validity);
    }

    public static Collection<X509CRL> getCRLs(X509CRLSelector selector, boolean signFlag, PublicKey prevKey, X509Certificate prevCert, String provider, List<CertStore> certStores, boolean[] reasonsMask, Set<TrustAnchor> trustAnchors, Date validity) throws CertStoreException {
        X509Certificate cert = selector.getCertificateChecking();
        if (cert == null) {
            return Collections.emptySet();
        }
        try {
            X509CertImpl certImpl = X509CertImpl.toImpl(cert);
            if (debug != null) {
                debug.println("DistributionPointFetcher.getCRLs: Checking CRLDPs for " + certImpl.getSubjectX500Principal());
            }
            CRLDistributionPointsExtension ext = certImpl.getCRLDistributionPointsExtension();
            if (ext == null) {
                if (debug != null) {
                    debug.println("No CRLDP ext");
                }
                return Collections.emptySet();
            }
            List<DistributionPoint> points = ext.get(CRLDistributionPointsExtension.POINTS);
            Set<X509CRL> results = new HashSet();
            for (DistributionPoint point : points) {
                if ((Arrays.equals(reasonsMask, ALL_REASONS) ^ 1) != 0) {
                    Set<X509CRL> set = results;
                    set.addAll(getCRLs(selector, certImpl, point, reasonsMask, signFlag, prevKey, prevCert, provider, certStores, trustAnchors, validity));
                }
            }
            if (debug != null) {
                debug.println("Returning " + results.size() + " CRLs");
            }
            return results;
        } catch (CertificateException e) {
            return Collections.emptySet();
        }
    }

    private static Collection<X509CRL> getCRLs(X509CRLSelector selector, X509CertImpl certImpl, DistributionPoint point, boolean[] reasonsMask, boolean signFlag, PublicKey prevKey, X509Certificate prevCert, String provider, List<CertStore> certStores, Set<TrustAnchor> trustAnchors, Date validity) throws CertStoreException {
        X509CRL crl;
        GeneralNames fullName = point.getFullName();
        if (fullName == null) {
            RDN relativeName = point.getRelativeName();
            if (relativeName == null) {
                return Collections.emptySet();
            }
            try {
                GeneralNames crlIssuers = point.getCRLIssuer();
                if (crlIssuers == null) {
                    fullName = getFullNames((X500Name) certImpl.getIssuerDN(), relativeName);
                } else if (crlIssuers.size() != 1) {
                    return Collections.emptySet();
                } else {
                    fullName = getFullNames((X500Name) crlIssuers.get(0).getName(), relativeName);
                }
            } catch (IOException e) {
                return Collections.emptySet();
            }
        }
        Collection<X509CRL> possibleCRLs = new ArrayList();
        CertStoreException savedCSE = null;
        Iterator<GeneralName> t = fullName.iterator();
        while (t.hasNext()) {
            try {
                GeneralName name = (GeneralName) t.next();
                if (name.getType() == 4) {
                    possibleCRLs.addAll(getCRLs((X500Name) name.getName(), certImpl.getIssuerX500Principal(), certStores));
                } else if (name.getType() == 6) {
                    crl = getCRL((URIName) name.getName());
                    if (crl != null) {
                        possibleCRLs.add(crl);
                    }
                }
            } catch (CertStoreException cse) {
                savedCSE = cse;
            }
        }
        if (!possibleCRLs.isEmpty() || savedCSE == null) {
            Collection<X509CRL> crls = new ArrayList(2);
            for (X509CRL crl2 : possibleCRLs) {
                try {
                    selector.setIssuerNames(null);
                    if (selector.match(crl2) && verifyCRL(certImpl, point, crl2, reasonsMask, signFlag, prevKey, prevCert, provider, trustAnchors, certStores, validity)) {
                        crls.add(crl2);
                    }
                } catch (Exception e2) {
                    if (debug != null) {
                        debug.println("Exception verifying CRL: " + e2.getMessage());
                        e2.printStackTrace();
                    }
                }
            }
            return crls;
        }
        throw savedCSE;
    }

    private static X509CRL getCRL(URIName name) throws CertStoreException {
        Object uri = name.getURI();
        if (debug != null) {
            debug.println("Trying to fetch CRL from DP " + uri);
        }
        try {
            Collection<? extends CRL> crls = URICertStore.getInstance(new URICertStoreParameters(uri)).getCRLs(null);
            if (crls.isEmpty()) {
                return null;
            }
            return (X509CRL) crls.iterator().next();
        } catch (GeneralSecurityException e) {
            if (debug != null) {
                debug.println("Can't create URICertStore: " + e.getMessage());
            }
            return null;
        }
    }

    private static Collection<X509CRL> getCRLs(X500Name name, X500Principal certIssuer, List<CertStore> certStores) throws CertStoreException {
        if (debug != null) {
            debug.println("Trying to fetch CRL from DP " + name);
        }
        X509CRLSelector xcs = new X509CRLSelector();
        xcs.addIssuer(name.asX500Principal());
        xcs.addIssuer(certIssuer);
        Collection<X509CRL> crls = new ArrayList();
        CertStoreException savedCSE = null;
        for (CertStore store : certStores) {
            try {
                for (CRL crl : store.getCRLs(xcs)) {
                    crls.add((X509CRL) crl);
                }
            } catch (Object cse) {
                if (debug != null) {
                    debug.println("Exception while retrieving CRLs: " + cse);
                    cse.printStackTrace();
                }
                savedCSE = new CertStoreTypeException(store.getType(), cse);
            }
        }
        if (!crls.isEmpty() || savedCSE == null) {
            return crls;
        }
        throw savedCSE;
    }

    static boolean verifyCRL(sun.security.x509.X509CertImpl r52, sun.security.x509.DistributionPoint r53, java.security.cert.X509CRL r54, boolean[] r55, boolean r56, java.security.PublicKey r57, java.security.cert.X509Certificate r58, java.lang.String r59, java.util.Set<java.security.cert.TrustAnchor> r60, java.util.List<java.security.cert.CertStore> r61, java.util.Date r62) throws java.security.cert.CRLException, java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r47_0 'temporary' java.security.cert.TrustAnchor) in PHI: PHI: (r47_1 'temporary' java.security.cert.TrustAnchor) = (r47_0 'temporary' java.security.cert.TrustAnchor), (r47_2 'temporary' java.security.cert.TrustAnchor) binds: {(r47_0 'temporary' java.security.cert.TrustAnchor)=B:203:0x0448, (r47_2 'temporary' java.security.cert.TrustAnchor)=B:225:0x04ef}
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
        r49 = debug;
        if (r49 == 0) goto L_0x0043;
    L_0x0004:
        r49 = debug;
        r50 = new java.lang.StringBuilder;
        r50.<init>();
        r51 = "DistributionPointFetcher.verifyCRL: checking revocation status for\n  SN: ";
        r50 = r50.append(r51);
        r51 = r52.getSerialNumber();
        r51 = sun.security.util.Debug.toHexString(r51);
        r50 = r50.append(r51);
        r51 = "\n  Subject: ";
        r50 = r50.append(r51);
        r51 = r52.getSubjectX500Principal();
        r50 = r50.append(r51);
        r51 = "\n  Issuer: ";
        r50 = r50.append(r51);
        r51 = r52.getIssuerX500Principal();
        r50 = r50.append(r51);
        r50 = r50.toString();
        r49.println(r50);
    L_0x0043:
        r28 = 0;
        r13 = sun.security.x509.X509CRLImpl.toImpl(r54);
        r23 = r13.getIssuingDistributionPointExtension();
        r9 = r52.getIssuerDN();
        r9 = (sun.security.x509.X500Name) r9;
        r14 = r13.getIssuerDN();
        r14 = (sun.security.x509.X500Name) r14;
        r38 = r53.getCRLIssuer();
        r37 = 0;
        if (r38 == 0) goto L_0x00c1;
    L_0x0061:
        if (r23 == 0) goto L_0x0078;
    L_0x0063:
        r49 = "indirect_crl";
        r0 = r23;
        r1 = r49;
        r49 = r0.get(r1);
        r49 = (java.lang.Boolean) r49;
        r50 = java.lang.Boolean.FALSE;
        r49 = r49.equals(r50);
        if (r49 == 0) goto L_0x007b;
    L_0x0078:
        r49 = 0;
        return r49;
    L_0x007b:
        r31 = 0;
        r46 = r38.iterator();
    L_0x0081:
        if (r31 != 0) goto L_0x00a2;
    L_0x0083:
        r49 = r46.hasNext();
        if (r49 == 0) goto L_0x00a2;
    L_0x0089:
        r49 = r46.next();
        r49 = (sun.security.x509.GeneralName) r49;
        r32 = r49.getName();
        r0 = r32;
        r49 = r14.equals(r0);
        if (r49 == 0) goto L_0x0081;
    L_0x009b:
        r37 = r32;
        r37 = (sun.security.x509.X500Name) r37;
        r31 = 1;
        goto L_0x0081;
    L_0x00a2:
        if (r31 != 0) goto L_0x00a7;
    L_0x00a4:
        r49 = 0;
        return r49;
    L_0x00a7:
        r0 = r52;
        r1 = r59;
        r49 = issues(r0, r13, r1);
        if (r49 == 0) goto L_0x00be;
    L_0x00b1:
        r57 = r52.getPublicKey();
    L_0x00b5:
        if (r28 != 0) goto L_0x0130;
    L_0x00b7:
        r49 = r56 ^ 1;
        if (r49 == 0) goto L_0x0130;
    L_0x00bb:
        r49 = 0;
        return r49;
    L_0x00be:
        r28 = 1;
        goto L_0x00b5;
    L_0x00c1:
        r49 = r14.equals(r9);
        if (r49 != 0) goto L_0x00fd;
    L_0x00c7:
        r49 = debug;
        if (r49 == 0) goto L_0x00fa;
    L_0x00cb:
        r49 = debug;
        r50 = new java.lang.StringBuilder;
        r50.<init>();
        r51 = "crl issuer does not equal cert issuer.\ncrl issuer: ";
        r50 = r50.append(r51);
        r0 = r50;
        r50 = r0.append(r14);
        r51 = "\n";
        r50 = r50.append(r51);
        r51 = "cert issuer: ";
        r50 = r50.append(r51);
        r0 = r50;
        r50 = r0.append(r9);
        r50 = r50.toString();
        r49.println(r50);
    L_0x00fa:
        r49 = 0;
        return r49;
    L_0x00fd:
        r8 = r52.getAuthKeyId();
        r12 = r13.getAuthKeyId();
        if (r8 == 0) goto L_0x0109;
    L_0x0107:
        if (r12 != 0) goto L_0x0118;
    L_0x0109:
        r0 = r52;
        r1 = r59;
        r49 = issues(r0, r13, r1);
        if (r49 == 0) goto L_0x00b5;
    L_0x0113:
        r57 = r52.getPublicKey();
        goto L_0x00b5;
    L_0x0118:
        r49 = r8.equals(r12);
        if (r49 != 0) goto L_0x00b5;
    L_0x011e:
        r0 = r52;
        r1 = r59;
        r49 = issues(r0, r13, r1);
        if (r49 == 0) goto L_0x012d;
    L_0x0128:
        r57 = r52.getPublicKey();
        goto L_0x00b5;
    L_0x012d:
        r28 = 1;
        goto L_0x00b5;
    L_0x0130:
        if (r23 == 0) goto L_0x0345;
    L_0x0132:
        r49 = "point";
        r0 = r23;
        r1 = r49;
        r26 = r0.get(r1);
        r26 = (sun.security.x509.DistributionPointName) r26;
        if (r26 == 0) goto L_0x02bb;
    L_0x0141:
        r25 = r26.getFullName();
        if (r25 != 0) goto L_0x0183;
    L_0x0147:
        r44 = r26.getRelativeName();
        if (r44 != 0) goto L_0x015c;
    L_0x014d:
        r49 = debug;
        if (r49 == 0) goto L_0x0159;
    L_0x0151:
        r49 = debug;
        r50 = "IDP must be relative or full DN";
        r49.println(r50);
    L_0x0159:
        r49 = 0;
        return r49;
    L_0x015c:
        r49 = debug;
        if (r49 == 0) goto L_0x017d;
    L_0x0160:
        r49 = debug;
        r50 = new java.lang.StringBuilder;
        r50.<init>();
        r51 = "IDP relativeName:";
        r50 = r50.append(r51);
        r0 = r50;
        r1 = r44;
        r50 = r0.append(r1);
        r50 = r50.toString();
        r49.println(r50);
    L_0x017d:
        r0 = r44;
        r25 = getFullNames(r14, r0);
    L_0x0183:
        r49 = r53.getFullName();
        if (r49 != 0) goto L_0x018f;
    L_0x0189:
        r49 = r53.getRelativeName();
        if (r49 == 0) goto L_0x0281;
    L_0x018f:
        r40 = r53.getFullName();
        if (r40 != 0) goto L_0x01f0;
    L_0x0195:
        r44 = r53.getRelativeName();
        if (r44 != 0) goto L_0x01aa;
    L_0x019b:
        r49 = debug;
        if (r49 == 0) goto L_0x01a7;
    L_0x019f:
        r49 = debug;
        r50 = "DP must be relative or full DN";
        r49.println(r50);
    L_0x01a7:
        r49 = 0;
        return r49;
    L_0x01aa:
        r49 = debug;
        if (r49 == 0) goto L_0x01cb;
    L_0x01ae:
        r49 = debug;
        r50 = new java.lang.StringBuilder;
        r50.<init>();
        r51 = "DP relativeName:";
        r50 = r50.append(r51);
        r0 = r50;
        r1 = r44;
        r50 = r0.append(r1);
        r50 = r50.toString();
        r49.println(r50);
    L_0x01cb:
        if (r28 == 0) goto L_0x0269;
    L_0x01cd:
        r49 = r38.size();
        r50 = 1;
        r0 = r49;
        r1 = r50;
        if (r0 == r1) goto L_0x01e8;
    L_0x01d9:
        r49 = debug;
        if (r49 == 0) goto L_0x01e5;
    L_0x01dd:
        r49 = debug;
        r50 = "must only be one CRL issuer when relative name present";
        r49.println(r50);
    L_0x01e5:
        r49 = 0;
        return r49;
    L_0x01e8:
        r0 = r37;
        r1 = r44;
        r40 = getFullNames(r0, r1);
    L_0x01f0:
        r31 = 0;
        r21 = r25.iterator();
    L_0x01f6:
        if (r31 != 0) goto L_0x0270;
    L_0x01f8:
        r49 = r21.hasNext();
        if (r49 == 0) goto L_0x0270;
    L_0x01fe:
        r49 = r21.next();
        r49 = (sun.security.x509.GeneralName) r49;
        r24 = r49.getName();
        r49 = debug;
        if (r49 == 0) goto L_0x0229;
    L_0x020c:
        r49 = debug;
        r50 = new java.lang.StringBuilder;
        r50.<init>();
        r51 = "idpName: ";
        r50 = r50.append(r51);
        r0 = r50;
        r1 = r24;
        r50 = r0.append(r1);
        r50 = r50.toString();
        r49.println(r50);
    L_0x0229:
        r35 = r40.iterator();
    L_0x022d:
        if (r31 != 0) goto L_0x01f6;
    L_0x022f:
        r49 = r35.hasNext();
        if (r49 == 0) goto L_0x01f6;
    L_0x0235:
        r49 = r35.next();
        r49 = (sun.security.x509.GeneralName) r49;
        r39 = r49.getName();
        r49 = debug;
        if (r49 == 0) goto L_0x0260;
    L_0x0243:
        r49 = debug;
        r50 = new java.lang.StringBuilder;
        r50.<init>();
        r51 = "pointName: ";
        r50 = r50.append(r51);
        r0 = r50;
        r1 = r39;
        r50 = r0.append(r1);
        r50 = r50.toString();
        r49.println(r50);
    L_0x0260:
        r0 = r24;
        r1 = r39;
        r31 = r0.equals(r1);
        goto L_0x022d;
    L_0x0269:
        r0 = r44;
        r40 = getFullNames(r9, r0);
        goto L_0x01f0;
    L_0x0270:
        if (r31 != 0) goto L_0x02bb;
    L_0x0272:
        r49 = debug;
        if (r49 == 0) goto L_0x027e;
    L_0x0276:
        r49 = debug;
        r50 = "IDP name does not match DP name";
        r49.println(r50);
    L_0x027e:
        r49 = 0;
        return r49;
    L_0x0281:
        r31 = 0;
        r46 = r38.iterator();
    L_0x0287:
        if (r31 != 0) goto L_0x02b6;
    L_0x0289:
        r49 = r46.hasNext();
        if (r49 == 0) goto L_0x02b6;
    L_0x028f:
        r49 = r46.next();
        r49 = (sun.security.x509.GeneralName) r49;
        r15 = r49.getName();
        r21 = r25.iterator();
    L_0x029d:
        if (r31 != 0) goto L_0x0287;
    L_0x029f:
        r49 = r21.hasNext();
        if (r49 == 0) goto L_0x0287;
    L_0x02a5:
        r49 = r21.next();
        r49 = (sun.security.x509.GeneralName) r49;
        r24 = r49.getName();
        r0 = r24;
        r31 = r15.equals(r0);
        goto L_0x029d;
    L_0x02b6:
        if (r31 != 0) goto L_0x02bb;
    L_0x02b8:
        r49 = 0;
        return r49;
    L_0x02bb:
        r49 = "only_user_certs";
        r0 = r23;
        r1 = r49;
        r6 = r0.get(r1);
        r6 = (java.lang.Boolean) r6;
        r49 = java.lang.Boolean.TRUE;
        r0 = r49;
        r49 = r6.equals(r0);
        if (r49 == 0) goto L_0x02ed;
    L_0x02d2:
        r49 = r52.getBasicConstraints();
        r50 = -1;
        r0 = r49;
        r1 = r50;
        if (r0 == r1) goto L_0x02ed;
    L_0x02de:
        r49 = debug;
        if (r49 == 0) goto L_0x02ea;
    L_0x02e2:
        r49 = debug;
        r50 = "cert must be a EE cert";
        r49.println(r50);
    L_0x02ea:
        r49 = 0;
        return r49;
    L_0x02ed:
        r49 = "only_ca_certs";
        r0 = r23;
        r1 = r49;
        r6 = r0.get(r1);
        r6 = (java.lang.Boolean) r6;
        r49 = java.lang.Boolean.TRUE;
        r0 = r49;
        r49 = r6.equals(r0);
        if (r49 == 0) goto L_0x031f;
    L_0x0304:
        r49 = r52.getBasicConstraints();
        r50 = -1;
        r0 = r49;
        r1 = r50;
        if (r0 != r1) goto L_0x031f;
    L_0x0310:
        r49 = debug;
        if (r49 == 0) goto L_0x031c;
    L_0x0314:
        r49 = debug;
        r50 = "cert must be a CA cert";
        r49.println(r50);
    L_0x031c:
        r49 = 0;
        return r49;
    L_0x031f:
        r49 = "only_attribute_certs";
        r0 = r23;
        r1 = r49;
        r6 = r0.get(r1);
        r6 = (java.lang.Boolean) r6;
        r49 = java.lang.Boolean.TRUE;
        r0 = r49;
        r49 = r6.equals(r0);
        if (r49 == 0) goto L_0x0345;
    L_0x0336:
        r49 = debug;
        if (r49 == 0) goto L_0x0342;
    L_0x033a:
        r49 = debug;
        r50 = "cert must not be an AA cert";
        r49.println(r50);
    L_0x0342:
        r49 = 0;
        return r49;
    L_0x0345:
        r49 = 9;
        r0 = r49;
        r0 = new boolean[r0];
        r29 = r0;
        r43 = 0;
        if (r23 == 0) goto L_0x035e;
    L_0x0351:
        r49 = "reasons";
        r0 = r23;
        r1 = r49;
        r43 = r0.get(r1);
        r43 = (sun.security.x509.ReasonFlags) r43;
    L_0x035e:
        r41 = r53.getReasonFlags();
        if (r43 == 0) goto L_0x03d5;
    L_0x0364:
        if (r41 == 0) goto L_0x039e;
    L_0x0366:
        r27 = r43.getFlags();
        r20 = 0;
    L_0x036c:
        r0 = r29;
        r0 = r0.length;
        r49 = r0;
        r0 = r20;
        r1 = r49;
        if (r0 >= r1) goto L_0x03a8;
    L_0x0377:
        r0 = r27;
        r0 = r0.length;
        r49 = r0;
        r0 = r20;
        r1 = r49;
        if (r0 >= r1) goto L_0x039b;
    L_0x0382:
        r49 = r27[r20];
        if (r49 == 0) goto L_0x039b;
    L_0x0386:
        r0 = r41;
        r0 = r0.length;
        r49 = r0;
        r0 = r20;
        r1 = r49;
        if (r0 >= r1) goto L_0x0398;
    L_0x0391:
        r49 = r41[r20];
    L_0x0393:
        r29[r20] = r49;
        r20 = r20 + 1;
        goto L_0x036c;
    L_0x0398:
        r49 = 0;
        goto L_0x0393;
    L_0x039b:
        r49 = 0;
        goto L_0x0393;
    L_0x039e:
        r49 = r43.getFlags();
        r29 = r49.clone();
        r29 = (boolean[]) r29;
    L_0x03a8:
        r34 = 0;
        r20 = 0;
    L_0x03ac:
        r0 = r29;
        r0 = r0.length;
        r49 = r0;
        r0 = r20;
        r1 = r49;
        if (r0 >= r1) goto L_0x03ef;
    L_0x03b7:
        r49 = r34 ^ 1;
        if (r49 == 0) goto L_0x03ef;
    L_0x03bb:
        r49 = r29[r20];
        if (r49 == 0) goto L_0x03d2;
    L_0x03bf:
        r0 = r55;
        r0 = r0.length;
        r49 = r0;
        r0 = r20;
        r1 = r49;
        if (r0 >= r1) goto L_0x03ec;
    L_0x03ca:
        r49 = r55[r20];
    L_0x03cc:
        r49 = r49 ^ 1;
        if (r49 == 0) goto L_0x03d2;
    L_0x03d0:
        r34 = 1;
    L_0x03d2:
        r20 = r20 + 1;
        goto L_0x03ac;
    L_0x03d5:
        if (r23 == 0) goto L_0x03d9;
    L_0x03d7:
        if (r43 != 0) goto L_0x03a8;
    L_0x03d9:
        if (r41 == 0) goto L_0x03e2;
    L_0x03db:
        r29 = r41.clone();
        r29 = (boolean[]) r29;
        goto L_0x03a8;
    L_0x03e2:
        r49 = 1;
        r0 = r29;
        r1 = r49;
        java.util.Arrays.fill(r0, r1);
        goto L_0x03a8;
    L_0x03ec:
        r49 = 0;
        goto L_0x03cc;
    L_0x03ef:
        if (r34 != 0) goto L_0x03f4;
    L_0x03f1:
        r49 = 0;
        return r49;
    L_0x03f4:
        if (r28 == 0) goto L_0x048f;
    L_0x03f6:
        r10 = new java.security.cert.X509CertSelector;
        r10.<init>();
        r49 = r14.asX500Principal();
        r0 = r49;
        r10.setSubject(r0);
        r49 = 7;
        r0 = r49;
        r0 = new boolean[r0];
        r16 = r0;
        r16 = {0, 0, 0, 0, 0, 0, 1};
        r0 = r16;
        r10.setKeyUsage(r0);
        r4 = r13.getAuthKeyIdExtension();
        if (r4 == 0) goto L_0x043b;
    L_0x041a:
        r30 = r4.getEncodedKeyIdentifier();
        if (r30 == 0) goto L_0x0425;
    L_0x0420:
        r0 = r30;
        r10.setSubjectKeyIdentifier(r0);
    L_0x0425:
        r49 = "serial_number";
        r0 = r49;
        r5 = r4.get(r0);
        r5 = (sun.security.x509.SerialNumber) r5;
        if (r5 == 0) goto L_0x043b;
    L_0x0432:
        r49 = r5.getNumber();
        r0 = r49;
        r10.setSerialNumber(r0);
    L_0x043b:
        r33 = new java.util.HashSet;
        r0 = r33;
        r1 = r60;
        r0.<init>(r1);
        if (r57 == 0) goto L_0x045c;
    L_0x0446:
        if (r58 == 0) goto L_0x04ef;
    L_0x0448:
        r47 = new java.security.cert.TrustAnchor;
        r49 = 0;
        r0 = r47;
        r1 = r58;
        r2 = r49;
        r0.<init>(r1, r2);
    L_0x0455:
        r0 = r33;
        r1 = r47;
        r0.add(r1);
    L_0x045c:
        r36 = 0;
        r36 = new java.security.cert.PKIXBuilderParameters;	 Catch:{ InvalidAlgorithmParameterException -> 0x0504 }
        r0 = r36;	 Catch:{ InvalidAlgorithmParameterException -> 0x0504 }
        r1 = r33;	 Catch:{ InvalidAlgorithmParameterException -> 0x0504 }
        r0.<init>(r1, r10);	 Catch:{ InvalidAlgorithmParameterException -> 0x0504 }
        r0 = r36;
        r1 = r61;
        r0.setCertStores(r1);
        r0 = r36;
        r1 = r59;
        r0.setSigProvider(r1);
        r0 = r36;
        r1 = r62;
        r0.setDate(r1);
        r49 = "PKIX";	 Catch:{ GeneralSecurityException -> 0x050f }
        r7 = java.security.cert.CertPathBuilder.getInstance(r49);	 Catch:{ GeneralSecurityException -> 0x050f }
        r0 = r36;	 Catch:{ GeneralSecurityException -> 0x050f }
        r45 = r7.build(r0);	 Catch:{ GeneralSecurityException -> 0x050f }
        r45 = (java.security.cert.PKIXCertPathBuilderResult) r45;	 Catch:{ GeneralSecurityException -> 0x050f }
        r57 = r45.getPublicKey();	 Catch:{ GeneralSecurityException -> 0x050f }
    L_0x048f:
        r0 = r57;	 Catch:{ CertPathValidatorException -> 0x051a }
        r1 = r54;	 Catch:{ CertPathValidatorException -> 0x051a }
        sun.security.provider.certpath.AlgorithmChecker.check(r0, r1);	 Catch:{ CertPathValidatorException -> 0x051a }
        r0 = r54;	 Catch:{ GeneralSecurityException -> 0x053d }
        r1 = r57;	 Catch:{ GeneralSecurityException -> 0x053d }
        r2 = r59;	 Catch:{ GeneralSecurityException -> 0x053d }
        r0.verify(r1, r2);	 Catch:{ GeneralSecurityException -> 0x053d }
        r48 = r54.getCriticalExtensionOIDs();
        if (r48 == 0) goto L_0x0550;
    L_0x04a5:
        r49 = sun.security.x509.PKIXExtensions.IssuingDistributionPoint_Id;
        r49 = r49.toString();
        r48.remove(r49);
        r49 = r48.isEmpty();
        if (r49 != 0) goto L_0x0550;
    L_0x04b4:
        r49 = debug;
        if (r49 == 0) goto L_0x054d;
    L_0x04b8:
        r49 = debug;
        r50 = new java.lang.StringBuilder;
        r50.<init>();
        r51 = "Unrecognized critical extension(s) in CRL: ";
        r50 = r50.append(r51);
        r0 = r50;
        r1 = r48;
        r50 = r0.append(r1);
        r50 = r50.toString();
        r49.println(r50);
        r19 = r48.iterator();
    L_0x04d9:
        r49 = r19.hasNext();
        if (r49 == 0) goto L_0x054d;
    L_0x04df:
        r18 = r19.next();
        r18 = (java.lang.String) r18;
        r49 = debug;
        r0 = r49;
        r1 = r18;
        r0.println(r1);
        goto L_0x04d9;
    L_0x04ef:
        r42 = r52.getIssuerX500Principal();
        r47 = new java.security.cert.TrustAnchor;
        r49 = 0;
        r0 = r47;
        r1 = r42;
        r2 = r57;
        r3 = r49;
        r0.<init>(r1, r2, r3);
        goto L_0x0455;
    L_0x0504:
        r22 = move-exception;
        r49 = new java.security.cert.CRLException;
        r0 = r49;
        r1 = r22;
        r0.<init>(r1);
        throw r49;
    L_0x050f:
        r17 = move-exception;
        r49 = new java.security.cert.CRLException;
        r0 = r49;
        r1 = r17;
        r0.<init>(r1);
        throw r49;
    L_0x051a:
        r11 = move-exception;
        r49 = debug;
        if (r49 == 0) goto L_0x053a;
    L_0x051f:
        r49 = debug;
        r50 = new java.lang.StringBuilder;
        r50.<init>();
        r51 = "CRL signature algorithm check failed: ";
        r50 = r50.append(r51);
        r0 = r50;
        r50 = r0.append(r11);
        r50 = r50.toString();
        r49.println(r50);
    L_0x053a:
        r49 = 0;
        return r49;
    L_0x053d:
        r17 = move-exception;
        r49 = debug;
        if (r49 == 0) goto L_0x054a;
    L_0x0542:
        r49 = debug;
        r50 = "CRL signature failed to verify";
        r49.println(r50);
    L_0x054a:
        r49 = 0;
        return r49;
    L_0x054d:
        r49 = 0;
        return r49;
    L_0x0550:
        r20 = 0;
    L_0x0552:
        r0 = r55;
        r0 = r0.length;
        r49 = r0;
        r0 = r20;
        r1 = r49;
        if (r0 >= r1) goto L_0x0579;
    L_0x055d:
        r49 = r55[r20];
        if (r49 != 0) goto L_0x0573;
    L_0x0561:
        r0 = r29;
        r0 = r0.length;
        r49 = r0;
        r0 = r20;
        r1 = r49;
        if (r0 >= r1) goto L_0x0576;
    L_0x056c:
        r49 = r29[r20];
    L_0x056e:
        r55[r20] = r49;
        r20 = r20 + 1;
        goto L_0x0552;
    L_0x0573:
        r49 = 1;
        goto L_0x056e;
    L_0x0576:
        r49 = 0;
        goto L_0x056e;
    L_0x0579:
        r49 = 1;
        return r49;
        */
        throw new UnsupportedOperationException("Method not decompiled: sun.security.provider.certpath.DistributionPointFetcher.verifyCRL(sun.security.x509.X509CertImpl, sun.security.x509.DistributionPoint, java.security.cert.X509CRL, boolean[], boolean, java.security.PublicKey, java.security.cert.X509Certificate, java.lang.String, java.util.Set, java.util.List, java.util.Date):boolean");
    }

    private static GeneralNames getFullNames(X500Name issuer, RDN rdn) throws IOException {
        List<RDN> rdns = new ArrayList(issuer.rdns());
        rdns.add(rdn);
        GeneralNameInterface fullName = new X500Name((RDN[]) rdns.toArray(new RDN[0]));
        GeneralNames fullNames = new GeneralNames();
        fullNames.add(new GeneralName(fullName));
        return fullNames;
    }

    private static boolean issues(X509CertImpl cert, X509CRLImpl crl, String provider) throws IOException {
        AdaptableX509CertSelector issuerSelector = new AdaptableX509CertSelector();
        boolean[] usages = cert.getKeyUsage();
        if (usages != null) {
            usages[6] = true;
            issuerSelector.setKeyUsage(usages);
        }
        issuerSelector.setSubject(crl.getIssuerX500Principal());
        AuthorityKeyIdentifierExtension crlAKID = crl.getAuthKeyIdExtension();
        issuerSelector.setSkiAndSerialNumber(crlAKID);
        boolean matched = issuerSelector.match(cert);
        if (!matched) {
            return matched;
        }
        if (crlAKID != null && cert.getAuthorityKeyIdentifierExtension() != null) {
            return matched;
        }
        try {
            crl.verify(cert.getPublicKey(), provider);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
