package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import sun.misc.HexDumpEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class X509CertInfo implements CertAttrSet<String> {
    public static final String ALGORITHM_ID = "algorithmID";
    private static final int ATTR_ALGORITHM = 3;
    private static final int ATTR_EXTENSIONS = 10;
    private static final int ATTR_ISSUER = 4;
    private static final int ATTR_ISSUER_ID = 8;
    private static final int ATTR_KEY = 7;
    private static final int ATTR_SERIAL = 2;
    private static final int ATTR_SUBJECT = 6;
    private static final int ATTR_SUBJECT_ID = 9;
    private static final int ATTR_VALIDITY = 5;
    private static final int ATTR_VERSION = 1;
    public static final String DN_NAME = "dname";
    public static final String EXTENSIONS = "extensions";
    public static final String IDENT = "x509.info";
    public static final String ISSUER = "issuer";
    public static final String ISSUER_ID = "issuerID";
    public static final String KEY = "key";
    public static final String NAME = "info";
    public static final String SERIAL_NUMBER = "serialNumber";
    public static final String SUBJECT = "subject";
    public static final String SUBJECT_ID = "subjectID";
    public static final String VALIDITY = "validity";
    public static final String VERSION = "version";
    private static final Map<String, Integer> map = new HashMap();
    protected CertificateAlgorithmId algId = null;
    protected CertificateExtensions extensions = null;
    protected CertificateValidity interval = null;
    protected X500Name issuer = null;
    protected UniqueIdentity issuerUniqueId = null;
    protected CertificateX509Key pubKey = null;
    private byte[] rawCertInfo = null;
    protected CertificateSerialNumber serialNum = null;
    protected X500Name subject = null;
    protected UniqueIdentity subjectUniqueId = null;
    protected CertificateVersion version = new CertificateVersion();

    static {
        map.put("version", Integer.valueOf(1));
        map.put("serialNumber", Integer.valueOf(2));
        map.put("algorithmID", Integer.valueOf(3));
        map.put("issuer", Integer.valueOf(4));
        map.put("validity", Integer.valueOf(5));
        map.put("subject", Integer.valueOf(6));
        map.put("key", Integer.valueOf(7));
        map.put(ISSUER_ID, Integer.valueOf(8));
        map.put(SUBJECT_ID, Integer.valueOf(9));
        map.put("extensions", Integer.valueOf(10));
    }

    public X509CertInfo(byte[] cert) throws CertificateParsingException {
        try {
            parse(new DerValue(cert));
        } catch (Throwable e) {
            throw new CertificateParsingException(e);
        }
    }

    public X509CertInfo(DerValue derVal) throws CertificateParsingException {
        try {
            parse(derVal);
        } catch (Throwable e) {
            throw new CertificateParsingException(e);
        }
    }

    public void encode(OutputStream out) throws CertificateException, IOException {
        if (this.rawCertInfo == null) {
            DerOutputStream tmp = new DerOutputStream();
            emit(tmp);
            this.rawCertInfo = tmp.toByteArray();
        }
        out.write((byte[]) this.rawCertInfo.clone());
    }

    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement("version");
        elements.addElement("serialNumber");
        elements.addElement("algorithmID");
        elements.addElement("issuer");
        elements.addElement("validity");
        elements.addElement("subject");
        elements.addElement("key");
        elements.addElement(ISSUER_ID);
        elements.addElement(SUBJECT_ID);
        elements.addElement("extensions");
        return elements.elements();
    }

    public String getName() {
        return "info";
    }

    public byte[] getEncodedInfo() throws CertificateEncodingException {
        try {
            if (this.rawCertInfo == null) {
                DerOutputStream tmp = new DerOutputStream();
                emit(tmp);
                this.rawCertInfo = tmp.toByteArray();
            }
            return (byte[]) this.rawCertInfo.clone();
        } catch (IOException e) {
            throw new CertificateEncodingException(e.toString());
        } catch (CertificateException e2) {
            throw new CertificateEncodingException(e2.toString());
        }
    }

    public boolean equals(Object other) {
        if (other instanceof X509CertInfo) {
            return equals((X509CertInfo) other);
        }
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean equals(X509CertInfo other) {
        if (this == other) {
            return true;
        }
        if (this.rawCertInfo == null || other.rawCertInfo == null || this.rawCertInfo.length != other.rawCertInfo.length) {
            return false;
        }
        for (int i = 0; i < this.rawCertInfo.length; i++) {
            if (this.rawCertInfo[i] != other.rawCertInfo[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int retval = 0;
        for (int i = 1; i < this.rawCertInfo.length; i++) {
            retval += this.rawCertInfo[i] * i;
        }
        return retval;
    }

    public String toString() {
        if (this.subject == null || this.pubKey == null || this.interval == null || this.issuer == null || this.algId == null || this.serialNum == null) {
            throw new NullPointerException("X.509 cert is incomplete");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        sb.append("  ").append(this.version.toString()).append("\n");
        sb.append("  Subject: ").append(this.subject.toString()).append("\n");
        sb.append("  Signature Algorithm: ").append(this.algId.toString()).append("\n");
        sb.append("  Key:  ").append(this.pubKey.toString()).append("\n");
        sb.append("  ").append(this.interval.toString()).append("\n");
        sb.append("  Issuer: ").append(this.issuer.toString()).append("\n");
        sb.append("  ").append(this.serialNum.toString()).append("\n");
        if (this.issuerUniqueId != null) {
            sb.append("  Issuer Id:\n").append(this.issuerUniqueId.toString()).append("\n");
        }
        if (this.subjectUniqueId != null) {
            sb.append("  Subject Id:\n").append(this.subjectUniqueId.toString()).append("\n");
        }
        if (this.extensions != null) {
            int i;
            Extension[] exts = (Extension[]) this.extensions.getAllExtensions().toArray(new Extension[0]);
            sb.append("\nCertificate Extensions: ").append(exts.length);
            for (i = 0; i < exts.length; i++) {
                sb.append("\n[").append(i + 1).append("]: ");
                Extension ext = exts[i];
                try {
                    if (OIDMap.getClass(ext.getExtensionId()) == null) {
                        sb.append(ext.toString());
                        byte[] extValue = ext.getExtensionValue();
                        if (extValue != null) {
                            DerOutputStream out = new DerOutputStream();
                            out.putOctetString(extValue);
                            extValue = out.toByteArray();
                            sb.append("Extension unknown: DER encoded OCTET string =\n").append(new HexDumpEncoder().encodeBuffer(extValue)).append("\n");
                        }
                    } else {
                        sb.append(ext.toString());
                    }
                } catch (Exception e) {
                    sb.append(", Error parsing this extension");
                }
            }
            Map<String, Extension> invalid = this.extensions.getUnparseableExtensions();
            if (!invalid.isEmpty()) {
                sb.append("\nUnparseable certificate extensions: ").append(invalid.size());
                i = 1;
                for (Object ext2 : invalid.values()) {
                    int i2 = i + 1;
                    sb.append("\n[").append(i).append("]: ");
                    sb.append(ext2);
                    i = i2;
                }
            }
        }
        sb.append("\n]");
        return sb.toString();
    }

    public void set(String name, Object val) throws CertificateException, IOException {
        X509AttributeName attrName = new X509AttributeName(name);
        int attr = attributeMap(attrName.getPrefix());
        if (attr == 0) {
            throw new CertificateException("Attribute name not recognized: " + name);
        }
        this.rawCertInfo = null;
        String suffix = attrName.getSuffix();
        switch (attr) {
            case 1:
                if (suffix == null) {
                    setVersion(val);
                    return;
                } else {
                    this.version.set(suffix, val);
                    return;
                }
            case 2:
                if (suffix == null) {
                    setSerialNumber(val);
                    return;
                } else {
                    this.serialNum.set(suffix, val);
                    return;
                }
            case 3:
                if (suffix == null) {
                    setAlgorithmId(val);
                    return;
                } else {
                    this.algId.set(suffix, val);
                    return;
                }
            case 4:
                setIssuer(val);
                return;
            case 5:
                if (suffix == null) {
                    setValidity(val);
                    return;
                } else {
                    this.interval.set(suffix, val);
                    return;
                }
            case 6:
                setSubject(val);
                return;
            case 7:
                if (suffix == null) {
                    setKey(val);
                    return;
                } else {
                    this.pubKey.set(suffix, val);
                    return;
                }
            case 8:
                setIssuerUniqueId(val);
                return;
            case 9:
                setSubjectUniqueId(val);
                return;
            case 10:
                if (suffix == null) {
                    setExtensions(val);
                    return;
                }
                if (this.extensions == null) {
                    this.extensions = new CertificateExtensions();
                }
                this.extensions.set(suffix, val);
                return;
            default:
                return;
        }
    }

    public void delete(String name) throws CertificateException, IOException {
        X509AttributeName attrName = new X509AttributeName(name);
        int attr = attributeMap(attrName.getPrefix());
        if (attr == 0) {
            throw new CertificateException("Attribute name not recognized: " + name);
        }
        this.rawCertInfo = null;
        String suffix = attrName.getSuffix();
        switch (attr) {
            case 1:
                if (suffix == null) {
                    this.version = null;
                    return;
                } else {
                    this.version.delete(suffix);
                    return;
                }
            case 2:
                if (suffix == null) {
                    this.serialNum = null;
                    return;
                } else {
                    this.serialNum.delete(suffix);
                    return;
                }
            case 3:
                if (suffix == null) {
                    this.algId = null;
                    return;
                } else {
                    this.algId.delete(suffix);
                    return;
                }
            case 4:
                this.issuer = null;
                return;
            case 5:
                if (suffix == null) {
                    this.interval = null;
                    return;
                } else {
                    this.interval.delete(suffix);
                    return;
                }
            case 6:
                this.subject = null;
                return;
            case 7:
                if (suffix == null) {
                    this.pubKey = null;
                    return;
                } else {
                    this.pubKey.delete(suffix);
                    return;
                }
            case 8:
                this.issuerUniqueId = null;
                return;
            case 9:
                this.subjectUniqueId = null;
                return;
            case 10:
                if (suffix == null) {
                    this.extensions = null;
                    return;
                } else if (this.extensions != null) {
                    this.extensions.delete(suffix);
                    return;
                } else {
                    return;
                }
            default:
                return;
        }
    }

    public Object get(String name) throws CertificateException, IOException {
        X509AttributeName attrName = new X509AttributeName(name);
        int attr = attributeMap(attrName.getPrefix());
        if (attr == 0) {
            throw new CertificateParsingException("Attribute name not recognized: " + name);
        }
        String suffix = attrName.getSuffix();
        switch (attr) {
            case 1:
                if (suffix == null) {
                    return this.version;
                }
                return this.version.get(suffix);
            case 2:
                if (suffix == null) {
                    return this.serialNum;
                }
                return this.serialNum.get(suffix);
            case 3:
                if (suffix == null) {
                    return this.algId;
                }
                return this.algId.get(suffix);
            case 4:
                if (suffix == null) {
                    return this.issuer;
                }
                return getX500Name(suffix, true);
            case 5:
                if (suffix == null) {
                    return this.interval;
                }
                return this.interval.get(suffix);
            case 6:
                if (suffix == null) {
                    return this.subject;
                }
                return getX500Name(suffix, false);
            case 7:
                if (suffix == null) {
                    return this.pubKey;
                }
                return this.pubKey.get(suffix);
            case 8:
                return this.issuerUniqueId;
            case 9:
                return this.subjectUniqueId;
            case 10:
                if (suffix == null) {
                    return this.extensions;
                }
                if (this.extensions == null) {
                    return null;
                }
                return this.extensions.get(suffix);
            default:
                return null;
        }
    }

    private Object getX500Name(String name, boolean getIssuer) throws IOException {
        if (name.equalsIgnoreCase("dname")) {
            return getIssuer ? this.issuer : this.subject;
        } else if (name.equalsIgnoreCase("x500principal")) {
            Object asX500Principal;
            if (getIssuer) {
                asX500Principal = this.issuer.asX500Principal();
            } else {
                asX500Principal = this.subject.asX500Principal();
            }
            return asX500Principal;
        } else {
            throw new IOException("Attribute name not recognized.");
        }
    }

    private void parse(DerValue val) throws CertificateParsingException, IOException {
        if (val.tag != (byte) 48) {
            throw new CertificateParsingException("signed fields invalid");
        }
        this.rawCertInfo = val.toByteArray();
        DerInputStream in = val.data;
        DerValue tmp = in.getDerValue();
        if (tmp.isContextSpecific((byte) 0)) {
            this.version = new CertificateVersion(tmp);
            tmp = in.getDerValue();
        }
        this.serialNum = new CertificateSerialNumber(tmp);
        this.algId = new CertificateAlgorithmId(in);
        this.issuer = new X500Name(in);
        if (this.issuer.isEmpty()) {
            throw new CertificateParsingException("Empty issuer DN not allowed in X509Certificates");
        }
        this.interval = new CertificateValidity(in);
        this.subject = new X500Name(in);
        if (this.version.compare(0) == 0 && this.subject.isEmpty()) {
            throw new CertificateParsingException("Empty subject DN not allowed in v1 certificate");
        }
        this.pubKey = new CertificateX509Key(in);
        if (in.available() == 0) {
            return;
        }
        if (this.version.compare(0) == 0) {
            throw new CertificateParsingException("no more data allowed for version 1 certificate");
        }
        tmp = in.getDerValue();
        if (tmp.isContextSpecific((byte) 1)) {
            this.issuerUniqueId = new UniqueIdentity(tmp);
            if (in.available() != 0) {
                tmp = in.getDerValue();
            } else {
                return;
            }
        }
        if (tmp.isContextSpecific((byte) 2)) {
            this.subjectUniqueId = new UniqueIdentity(tmp);
            if (in.available() != 0) {
                tmp = in.getDerValue();
            } else {
                return;
            }
        }
        if (this.version.compare(2) != 0) {
            throw new CertificateParsingException("Extensions not allowed in v2 certificate");
        }
        if (tmp.isConstructed() && tmp.isContextSpecific((byte) 3)) {
            this.extensions = new CertificateExtensions(tmp.data);
        }
        verifyCert(this.subject, this.extensions);
    }

    private void verifyCert(X500Name subject, CertificateExtensions extensions) throws CertificateParsingException, IOException {
        if (!subject.isEmpty()) {
            return;
        }
        if (extensions == null) {
            throw new CertificateParsingException("X.509 Certificate is incomplete: subject field is empty, and certificate has no extensions");
        }
        try {
            SubjectAlternativeNameExtension subjectAltNameExt = (SubjectAlternativeNameExtension) extensions.get(SubjectAlternativeNameExtension.NAME);
            GeneralNames names = subjectAltNameExt.get(SubjectAlternativeNameExtension.SUBJECT_NAME);
            if (names == null || names.isEmpty()) {
                throw new CertificateParsingException("X.509 Certificate is incomplete: subject field is empty, and SubjectAlternativeName extension is empty");
            } else if (!subjectAltNameExt.isCritical()) {
                throw new CertificateParsingException("X.509 Certificate is incomplete: SubjectAlternativeName extension MUST be marked critical when subject field is empty");
            }
        } catch (IOException e) {
            throw new CertificateParsingException("X.509 Certificate is incomplete: subject field is empty, and SubjectAlternativeName extension is absent");
        }
    }

    private void emit(DerOutputStream out) throws CertificateException, IOException {
        DerOutputStream tmp = new DerOutputStream();
        this.version.encode(tmp);
        this.serialNum.encode(tmp);
        this.algId.encode(tmp);
        if (this.version.compare(0) == 0 && this.issuer.toString() == null) {
            throw new CertificateParsingException("Null issuer DN not allowed in v1 certificate");
        }
        this.issuer.encode(tmp);
        this.interval.encode(tmp);
        if (this.version.compare(0) == 0 && this.subject.toString() == null) {
            throw new CertificateParsingException("Null subject DN not allowed in v1 certificate");
        }
        this.subject.encode(tmp);
        this.pubKey.encode(tmp);
        if (this.issuerUniqueId != null) {
            this.issuerUniqueId.encode(tmp, DerValue.createTag(Byte.MIN_VALUE, false, (byte) 1));
        }
        if (this.subjectUniqueId != null) {
            this.subjectUniqueId.encode(tmp, DerValue.createTag(Byte.MIN_VALUE, false, (byte) 2));
        }
        if (this.extensions != null) {
            this.extensions.encode(tmp);
        }
        out.write((byte) 48, tmp);
    }

    private int attributeMap(String name) {
        Integer num = (Integer) map.get(name);
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    private void setVersion(Object val) throws CertificateException {
        if (val instanceof CertificateVersion) {
            this.version = (CertificateVersion) val;
            return;
        }
        throw new CertificateException("Version class type invalid.");
    }

    private void setSerialNumber(Object val) throws CertificateException {
        if (val instanceof CertificateSerialNumber) {
            this.serialNum = (CertificateSerialNumber) val;
            return;
        }
        throw new CertificateException("SerialNumber class type invalid.");
    }

    private void setAlgorithmId(Object val) throws CertificateException {
        if (val instanceof CertificateAlgorithmId) {
            this.algId = (CertificateAlgorithmId) val;
            return;
        }
        throw new CertificateException("AlgorithmId class type invalid.");
    }

    private void setIssuer(Object val) throws CertificateException {
        if (val instanceof X500Name) {
            this.issuer = (X500Name) val;
            return;
        }
        throw new CertificateException("Issuer class type invalid.");
    }

    private void setValidity(Object val) throws CertificateException {
        if (val instanceof CertificateValidity) {
            this.interval = (CertificateValidity) val;
            return;
        }
        throw new CertificateException("CertificateValidity class type invalid.");
    }

    private void setSubject(Object val) throws CertificateException {
        if (val instanceof X500Name) {
            this.subject = (X500Name) val;
            return;
        }
        throw new CertificateException("Subject class type invalid.");
    }

    private void setKey(Object val) throws CertificateException {
        if (val instanceof CertificateX509Key) {
            this.pubKey = (CertificateX509Key) val;
            return;
        }
        throw new CertificateException("Key class type invalid.");
    }

    private void setIssuerUniqueId(Object val) throws CertificateException {
        if (this.version.compare(1) < 0) {
            throw new CertificateException("Invalid version");
        } else if (val instanceof UniqueIdentity) {
            this.issuerUniqueId = (UniqueIdentity) val;
        } else {
            throw new CertificateException("IssuerUniqueId class type invalid.");
        }
    }

    private void setSubjectUniqueId(Object val) throws CertificateException {
        if (this.version.compare(1) < 0) {
            throw new CertificateException("Invalid version");
        } else if (val instanceof UniqueIdentity) {
            this.subjectUniqueId = (UniqueIdentity) val;
        } else {
            throw new CertificateException("SubjectUniqueId class type invalid.");
        }
    }

    private void setExtensions(Object val) throws CertificateException {
        if (this.version.compare(2) < 0) {
            throw new CertificateException("Invalid version");
        } else if (val instanceof CertificateExtensions) {
            this.extensions = (CertificateExtensions) val;
        } else {
            throw new CertificateException("Extensions class type invalid.");
        }
    }
}
