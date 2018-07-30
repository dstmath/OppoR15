package sun.security.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import sun.security.x509.X509CertImpl;

public class AnchorCertificates {
    private static final String HASH = "SHA-256";
    private static HashSet<String> certs;
    private static final Debug debug = Debug.getInstance("certpath");

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                Throwable th;
                File f = new File(System.getProperty("java.home"), "lib/security/cacerts");
                try {
                    KeyStore cacerts = KeyStore.getInstance("JKS");
                    FileInputStream fis = null;
                    Throwable th2;
                    try {
                        FileInputStream fis2 = new FileInputStream(f);
                        try {
                            cacerts.load(fis2, null);
                            AnchorCertificates.certs = new HashSet();
                            Enumeration<String> list = cacerts.aliases();
                            while (list.hasMoreElements()) {
                                String alias = (String) list.nextElement();
                                if (alias.contains(" [jdk")) {
                                    AnchorCertificates.certs.add(X509CertImpl.getFingerprint(AnchorCertificates.HASH, (X509Certificate) cacerts.getCertificate(alias)));
                                }
                            }
                            if (fis2 != null) {
                                try {
                                    fis2.close();
                                } catch (Throwable th3) {
                                    th2 = th3;
                                }
                            }
                            th2 = null;
                            if (th2 != null) {
                                throw th2;
                            }
                            return null;
                        } catch (Throwable th4) {
                            th2 = th4;
                            fis = fis2;
                            th = null;
                            if (fis != null) {
                                try {
                                    fis.close();
                                } catch (Throwable th5) {
                                    if (th == null) {
                                        th = th5;
                                    } else if (th != th5) {
                                        th.addSuppressed(th5);
                                    }
                                }
                            }
                            if (th == null) {
                                throw th;
                            } else {
                                throw th2;
                            }
                        }
                    } catch (Throwable th6) {
                        th2 = th6;
                        th = null;
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (Throwable th52) {
                                if (th == null) {
                                    th = th52;
                                } else if (th != th52) {
                                    th.addSuppressed(th52);
                                }
                            }
                        }
                        if (th == null) {
                            throw th2;
                        }
                        throw th;
                    }
                } catch (Exception e) {
                    if (AnchorCertificates.debug != null) {
                        AnchorCertificates.debug.println("Error parsing cacerts");
                    }
                    e.printStackTrace();
                }
            }
        });
    }

    public static boolean contains(X509Certificate cert) {
        boolean result = certs.contains(X509CertImpl.getFingerprint(HASH, cert));
        if (result && debug != null) {
            debug.println("AnchorCertificate.contains: matched " + cert.getSubjectDN());
        }
        return result;
    }

    private AnchorCertificates() {
    }
}
