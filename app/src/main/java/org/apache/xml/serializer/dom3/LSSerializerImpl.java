package org.apache.xml.serializer.dom3;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.xml.serializer.DOM3Serializer;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.SystemIDResolver;
import org.apache.xml.serializer.utils.Utils;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSSerializerFilter;

public final class LSSerializerImpl implements DOMConfiguration, LSSerializer {
    private static final int CANONICAL = 1;
    private static final int CDATA = 2;
    private static final int CHARNORMALIZE = 4;
    private static final int COMMENTS = 8;
    private static final String DEFAULT_END_OF_LINE;
    private static final int DISCARDDEFAULT = 32768;
    private static final int DTNORMALIZE = 16;
    private static final int ELEM_CONTENT_WHITESPACE = 32;
    private static final int ENTITIES = 64;
    private static final int IGNORE_CHAR_DENORMALIZE = 131072;
    private static final int INFOSET = 128;
    private static final int NAMESPACEDECLS = 512;
    private static final int NAMESPACES = 256;
    private static final int NORMALIZECHARS = 1024;
    private static final int PRETTY_PRINT = 65536;
    private static final int SCHEMAVALIDATE = 8192;
    private static final int SPLITCDATA = 2048;
    private static final int VALIDATE = 4096;
    private static final int WELLFORMED = 16384;
    private static final int XMLDECL = 262144;
    private Properties fDOMConfigProperties = null;
    private DOMErrorHandler fDOMErrorHandler = null;
    private DOM3Serializer fDOMSerializer = null;
    private String fEncoding;
    private String fEndOfLine = DEFAULT_END_OF_LINE;
    protected int fFeatures = 0;
    private String[] fRecognizedParameters = new String[]{DOMConstants.DOM_CANONICAL_FORM, DOMConstants.DOM_CDATA_SECTIONS, DOMConstants.DOM_CHECK_CHAR_NORMALIZATION, DOMConstants.DOM_COMMENTS, DOMConstants.DOM_DATATYPE_NORMALIZATION, DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE, DOMConstants.DOM_ENTITIES, DOMConstants.DOM_INFOSET, DOMConstants.DOM_NAMESPACES, DOMConstants.DOM_NAMESPACE_DECLARATIONS, DOMConstants.DOM_SPLIT_CDATA, DOMConstants.DOM_VALIDATE, DOMConstants.DOM_VALIDATE_IF_SCHEMA, DOMConstants.DOM_WELLFORMED, DOMConstants.DOM_DISCARD_DEFAULT_CONTENT, DOMConstants.DOM_FORMAT_PRETTY_PRINT, DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS, DOMConstants.DOM_XMLDECL, DOMConstants.DOM_ERROR_HANDLER};
    private LSSerializerFilter fSerializerFilter = null;
    private Node fVisitedNode = null;
    private Serializer fXMLSerializer = null;

    static class ThrowableMethods {
        private static Method fgThrowableInitCauseMethod;
        private static boolean fgThrowableMethodsAvailable;

        static {
            fgThrowableInitCauseMethod = null;
            fgThrowableMethodsAvailable = false;
            try {
                fgThrowableInitCauseMethod = Throwable.class.getMethod("initCause", new Class[]{Throwable.class});
                fgThrowableMethodsAvailable = true;
            } catch (Exception e) {
                fgThrowableInitCauseMethod = null;
                fgThrowableMethodsAvailable = false;
            }
        }

        private ThrowableMethods() {
        }
    }

    static {
        String lineSeparator = (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    return System.getProperty("line.separator");
                } catch (SecurityException e) {
                    return null;
                }
            }
        });
        if (lineSeparator == null || !(lineSeparator.equals("\r\n") || lineSeparator.equals("\r"))) {
            lineSeparator = "\n";
        }
        DEFAULT_END_OF_LINE = lineSeparator;
    }

    public LSSerializerImpl() {
        this.fFeatures |= 2;
        this.fFeatures |= 8;
        this.fFeatures |= 32;
        this.fFeatures |= 64;
        this.fFeatures |= 256;
        this.fFeatures |= 512;
        this.fFeatures |= 2048;
        this.fFeatures |= 16384;
        this.fFeatures |= 32768;
        this.fFeatures |= 262144;
        this.fDOMConfigProperties = new Properties();
        initializeSerializerProps();
        this.fXMLSerializer = SerializerFactory.getSerializer(OutputPropertiesFactory.getDefaultMethodProperties("xml"));
        this.fXMLSerializer.setOutputFormat(this.fDOMConfigProperties);
    }

    public void initializeSerializerProps() {
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}canonical-form", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}check-character-normalization", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}datatype-normalization", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_DEFAULT_TRUE);
        if ((this.fFeatures & 128) != 0) {
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_DEFAULT_FALSE);
            this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_DEFAULT_FALSE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_DEFAULT_FALSE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate-if-schema", DOMConstants.DOM3_DEFAULT_FALSE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}datatype-normalization", DOMConstants.DOM3_DEFAULT_FALSE);
        }
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}split-cdata-sections", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate-if-schema", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("indent", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, Integer.toString(3));
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}discard-default-content", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("omit-xml-declaration", "no");
    }

    public boolean canSetParameter(String name, Object value) {
        if (value instanceof Boolean) {
            if (name.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS) || name.equalsIgnoreCase(DOMConstants.DOM_COMMENTS) || name.equalsIgnoreCase(DOMConstants.DOM_ENTITIES) || name.equalsIgnoreCase(DOMConstants.DOM_INFOSET) || name.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE) || name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES) || name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS) || name.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA) || name.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED) || name.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT) || name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT) || name.equalsIgnoreCase(DOMConstants.DOM_XMLDECL)) {
                return true;
            }
            if (name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || name.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE)) {
                return ((Boolean) value).booleanValue() ^ 1;
            }
            if (name.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
                return ((Boolean) value).booleanValue();
            }
        } else if ((name.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER) && value == null) || (value instanceof DOMErrorHandler)) {
            return true;
        }
        return false;
    }

    public Object getParameter(String name) throws DOMException {
        if (name.equalsIgnoreCase(DOMConstants.DOM_COMMENTS)) {
            return (this.fFeatures & 8) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS)) {
            return (this.fFeatures & 2) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_ENTITIES)) {
            return (this.fFeatures & 64) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES)) {
            return (this.fFeatures & 256) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS)) {
            return (this.fFeatures & 512) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA)) {
            return (this.fFeatures & 2048) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED)) {
            return (this.fFeatures & 16384) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT)) {
            return (this.fFeatures & 32768) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT)) {
            return (this.fFeatures & 65536) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_XMLDECL)) {
            return (this.fFeatures & 262144) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
            return (this.fFeatures & 32) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT)) {
            return (this.fFeatures & 65536) != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
            return Boolean.TRUE;
        } else {
            if (name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || name.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA)) {
                return Boolean.FALSE;
            }
            if (name.equalsIgnoreCase(DOMConstants.DOM_INFOSET)) {
                if ((this.fFeatures & 64) != 0 || (this.fFeatures & 2) != 0 || (this.fFeatures & 32) == 0 || (this.fFeatures & 256) == 0 || (this.fFeatures & 512) == 0 || (this.fFeatures & 16384) == 0 || (this.fFeatures & 8) == 0) {
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER)) {
                return this.fDOMErrorHandler;
            } else {
                if (name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_LOCATION) || name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_TYPE)) {
                    return null;
                }
                throw new DOMException((short) 8, Utils.messages.createMessage("FEATURE_NOT_FOUND", new Object[]{name}));
            }
        }
    }

    public DOMStringList getParameterNames() {
        return new DOMStringListImpl(this.fRecognizedParameters);
    }

    public void setParameter(String name, Object value) throws DOMException {
        if (value instanceof Boolean) {
            boolean state = ((Boolean) value).booleanValue();
            int i;
            if (name.equalsIgnoreCase(DOMConstants.DOM_COMMENTS)) {
                this.fFeatures = state ? this.fFeatures | 8 : this.fFeatures & -9;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS)) {
                this.fFeatures = state ? this.fFeatures | 2 : this.fFeatures & -3;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_ENTITIES)) {
                this.fFeatures = state ? this.fFeatures | 64 : this.fFeatures & -65;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                }
                this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_EXPLICIT_FALSE);
                this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_EXPLICIT_FALSE);
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES)) {
                this.fFeatures = state ? this.fFeatures | 256 : this.fFeatures & -257;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS)) {
                if (state) {
                    i = this.fFeatures | 512;
                } else {
                    i = this.fFeatures & -513;
                }
                this.fFeatures = i;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA)) {
                this.fFeatures = state ? this.fFeatures | 2048 : this.fFeatures & -2049;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}split-cdata-sections", DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}split-cdata-sections", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED)) {
                this.fFeatures = state ? this.fFeatures | 16384 : this.fFeatures & -16385;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT)) {
                if (state) {
                    i = this.fFeatures | 32768;
                } else {
                    i = this.fFeatures & -32769;
                }
                this.fFeatures = i;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}discard-default-content", DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}discard-default-content", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT)) {
                this.fFeatures = state ? this.fFeatures | 65536 : this.fFeatures & -65537;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}format-pretty-print", DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}format-pretty-print", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_XMLDECL)) {
                this.fFeatures = state ? this.fFeatures | 262144 : this.fFeatures & -262145;
                if (state) {
                    this.fDOMConfigProperties.setProperty("omit-xml-declaration", "no");
                } else {
                    this.fDOMConfigProperties.setProperty("omit-xml-declaration", "yes");
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
                this.fFeatures = state ? this.fFeatures | 32 : this.fFeatures & -33;
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
                if (state) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}ignore-unknown-character-denormalizations", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                }
                throw new DOMException((short) 9, Utils.messages.createMessage("FEATURE_NOT_SUPPORTED", new Object[]{name}));
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE) || name.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION)) {
                if (state) {
                    throw new DOMException((short) 9, Utils.messages.createMessage("FEATURE_NOT_SUPPORTED", new Object[]{name}));
                } else if (name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM)) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}canonical-form", DOMConstants.DOM3_EXPLICIT_FALSE);
                } else if (name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA)) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate-if-schema", DOMConstants.DOM3_EXPLICIT_FALSE);
                } else if (name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE)) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate", DOMConstants.DOM3_EXPLICIT_FALSE);
                } else if (name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA)) {
                    this.fDOMConfigProperties.setProperty("check-character-normalizationcheck-character-normalization", DOMConstants.DOM3_EXPLICIT_FALSE);
                } else if (name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION)) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}datatype-normalization", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_INFOSET)) {
                if (state) {
                    this.fFeatures &= -65;
                    this.fFeatures &= -3;
                    this.fFeatures &= -8193;
                    this.fFeatures &= -17;
                    this.fFeatures |= 256;
                    this.fFeatures |= 512;
                    this.fFeatures |= 16384;
                    this.fFeatures |= 32;
                    this.fFeatures |= 8;
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_EXPLICIT_FALSE);
                    this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_EXPLICIT_FALSE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_EXPLICIT_FALSE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate-if-schema", DOMConstants.DOM3_EXPLICIT_FALSE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}datatype-normalization", DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER) || name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_LOCATION) || name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_TYPE)) {
                throw new DOMException((short) 17, Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[]{name}));
            } else {
                throw new DOMException((short) 8, Utils.messages.createMessage("FEATURE_NOT_FOUND", new Object[]{name}));
            }
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER)) {
            if (value == null || (value instanceof DOMErrorHandler)) {
                this.fDOMErrorHandler = (DOMErrorHandler) value;
                return;
            }
            throw new DOMException((short) 17, Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[]{name}));
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_LOCATION) || name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_TYPE)) {
            if (value == null) {
                return;
            }
            if (value instanceof String) {
                throw new DOMException((short) 9, Utils.messages.createMessage("FEATURE_NOT_SUPPORTED", new Object[]{name}));
            }
            throw new DOMException((short) 17, Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[]{name}));
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_COMMENTS) || name.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS) || name.equalsIgnoreCase(DOMConstants.DOM_ENTITIES) || name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES) || name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS) || name.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA) || name.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED) || name.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT) || name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT) || name.equalsIgnoreCase(DOMConstants.DOM_XMLDECL) || name.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE) || name.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS) || name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE) || name.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_INFOSET)) {
            throw new DOMException((short) 17, Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[]{name}));
        } else {
            throw new DOMException((short) 8, Utils.messages.createMessage("FEATURE_NOT_FOUND", new Object[]{name}));
        }
    }

    public DOMConfiguration getDomConfig() {
        return this;
    }

    public LSSerializerFilter getFilter() {
        return this.fSerializerFilter;
    }

    public String getNewLine() {
        return this.fEndOfLine;
    }

    public void setFilter(LSSerializerFilter filter) {
        this.fSerializerFilter = filter;
    }

    public void setNewLine(String newLine) {
        if (newLine == null) {
            newLine = DEFAULT_END_OF_LINE;
        }
        this.fEndOfLine = newLine;
    }

    public boolean write(org.w3c.dom.Node r27, org.w3c.dom.ls.LSOutput r28) throws org.w3c.dom.ls.LSException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r18_1 'urlOutStream' java.io.OutputStream) in PHI: PHI: (r18_2 'urlOutStream' java.io.OutputStream) = (r18_1 'urlOutStream' java.io.OutputStream), (r18_3 'urlOutStream' java.io.OutputStream) binds: {(r18_1 'urlOutStream' java.io.OutputStream)=B:63:0x0211, (r18_3 'urlOutStream' java.io.OutputStream)=B:81:0x02d4}
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
        r26 = this;
        if (r28 != 0) goto L_0x003a;
    L_0x0002:
        r21 = org.apache.xml.serializer.utils.Utils.messages;
        r22 = "no-output-specified";
        r23 = 0;
        r10 = r21.createMessage(r22, r23);
        r0 = r26;
        r0 = r0.fDOMErrorHandler;
        r21 = r0;
        if (r21 == 0) goto L_0x002e;
    L_0x0015:
        r0 = r26;
        r0 = r0.fDOMErrorHandler;
        r21 = r0;
        r22 = new org.apache.xml.serializer.dom3.DOMErrorImpl;
        r23 = "no-output-specified";
        r24 = 3;
        r0 = r22;
        r1 = r24;
        r2 = r23;
        r0.<init>(r1, r10, r2);
        r21.handleError(r22);
    L_0x002e:
        r21 = new org.w3c.dom.ls.LSException;
        r22 = 82;
        r0 = r21;
        r1 = r22;
        r0.<init>(r1, r10);
        throw r21;
    L_0x003a:
        if (r27 != 0) goto L_0x003f;
    L_0x003c:
        r21 = 0;
        return r21;
    L_0x003f:
        r0 = r26;
        r13 = r0.fXMLSerializer;
        r13.reset();
        r0 = r26;
        r0 = r0.fVisitedNode;
        r21 = r0;
        r0 = r27;
        r1 = r21;
        if (r0 == r1) goto L_0x0146;
    L_0x0052:
        r20 = r26.getXMLVersion(r27);
        r21 = r28.getEncoding();
        r0 = r21;
        r1 = r26;
        r1.fEncoding = r0;
        r0 = r26;
        r0 = r0.fEncoding;
        r21 = r0;
        if (r21 != 0) goto L_0x0086;
    L_0x0068:
        r21 = r26.getInputEncoding(r27);
        r0 = r21;
        r1 = r26;
        r1.fEncoding = r0;
        r0 = r26;
        r0 = r0.fEncoding;
        r21 = r0;
        if (r21 == 0) goto L_0x00ca;
    L_0x007a:
        r0 = r26;
        r0 = r0.fEncoding;
        r21 = r0;
    L_0x0080:
        r0 = r21;
        r1 = r26;
        r1.fEncoding = r0;
    L_0x0086:
        r0 = r26;
        r0 = r0.fEncoding;
        r21 = r0;
        r21 = org.apache.xml.serializer.Encodings.isRecognizedEncoding(r21);
        if (r21 != 0) goto L_0x00d9;
    L_0x0092:
        r21 = org.apache.xml.serializer.utils.Utils.messages;
        r22 = "unsupported-encoding";
        r23 = 0;
        r10 = r21.createMessage(r22, r23);
        r0 = r26;
        r0 = r0.fDOMErrorHandler;
        r21 = r0;
        if (r21 == 0) goto L_0x00be;
    L_0x00a5:
        r0 = r26;
        r0 = r0.fDOMErrorHandler;
        r21 = r0;
        r22 = new org.apache.xml.serializer.dom3.DOMErrorImpl;
        r23 = "unsupported-encoding";
        r24 = 3;
        r0 = r22;
        r1 = r24;
        r2 = r23;
        r0.<init>(r1, r10, r2);
        r21.handleError(r22);
    L_0x00be:
        r21 = new org.w3c.dom.ls.LSException;
        r22 = 82;
        r0 = r21;
        r1 = r22;
        r0.<init>(r1, r10);
        throw r21;
    L_0x00ca:
        r21 = r26.getXMLEncoding(r27);
        if (r21 != 0) goto L_0x00d4;
    L_0x00d0:
        r21 = "UTF-8";
        goto L_0x0080;
    L_0x00d4:
        r21 = r26.getXMLEncoding(r27);
        goto L_0x0080;
    L_0x00d9:
        r21 = r13.getOutputFormat();
        r22 = "version";
        r0 = r21;
        r1 = r22;
        r2 = r20;
        r0.setProperty(r1, r2);
        r0 = r26;
        r0 = r0.fDOMConfigProperties;
        r21 = r0;
        r22 = "{http://xml.apache.org/xerces-2j}xml-version";
        r0 = r21;
        r1 = r22;
        r2 = r20;
        r0.setProperty(r1, r2);
        r0 = r26;
        r0 = r0.fDOMConfigProperties;
        r21 = r0;
        r22 = "encoding";
        r0 = r26;
        r0 = r0.fEncoding;
        r23 = r0;
        r21.setProperty(r22, r23);
        r21 = r27.getNodeType();
        r22 = 9;
        r0 = r21;
        r1 = r22;
        if (r0 != r1) goto L_0x0125;
    L_0x0119:
        r21 = r27.getNodeType();
        r22 = 1;
        r0 = r21;
        r1 = r22;
        if (r0 == r1) goto L_0x01db;
    L_0x0125:
        r0 = r26;
        r0 = r0.fFeatures;
        r21 = r0;
        r22 = 262144; // 0x40000 float:3.67342E-40 double:1.295163E-318;
        r21 = r21 & r22;
        if (r21 == 0) goto L_0x0140;
    L_0x0131:
        r0 = r26;
        r0 = r0.fDOMConfigProperties;
        r21 = r0;
        r22 = "omit-xml-declaration";
        r23 = "default:no";
        r21.setProperty(r22, r23);
    L_0x0140:
        r0 = r27;
        r1 = r26;
        r1.fVisitedNode = r0;
    L_0x0146:
        r0 = r26;
        r0 = r0.fXMLSerializer;
        r21 = r0;
        r0 = r26;
        r0 = r0.fDOMConfigProperties;
        r22 = r0;
        r21.setOutputFormat(r22);
        r19 = r28.getCharacterStream();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r19 != 0) goto L_0x02e1;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x015b:
        r11 = r28.getByteStream();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r11 != 0) goto L_0x02da;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0161:
        r15 = r28.getSystemId();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r15 != 0) goto L_0x01e9;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0167:
        r21 = org.apache.xml.serializer.utils.Utils.messages;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r22 = "no-output-specified";	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r23 = 0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r10 = r21.createMessage(r22, r23);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fDOMErrorHandler;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r21 == 0) goto L_0x0193;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x017a:
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fDOMErrorHandler;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r22 = new org.apache.xml.serializer.dom3.DOMErrorImpl;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r23 = "no-output-specified";	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r24 = 3;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r22;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1 = r24;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r2 = r23;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0.<init>(r1, r10, r2);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21.handleError(r22);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0193:
        r21 = new org.w3c.dom.ls.LSException;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r22 = 82;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1 = r22;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0.<init>(r1, r10);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        throw r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x019f:
        r14 = move-exception;
        r21 = org.apache.xml.serializer.utils.Utils.messages;
        r22 = "unsupported-encoding";
        r23 = 0;
        r10 = r21.createMessage(r22, r23);
        r0 = r26;
        r0 = r0.fDOMErrorHandler;
        r21 = r0;
        if (r21 == 0) goto L_0x01cc;
    L_0x01b3:
        r0 = r26;
        r0 = r0.fDOMErrorHandler;
        r21 = r0;
        r22 = new org.apache.xml.serializer.dom3.DOMErrorImpl;
        r23 = "unsupported-encoding";
        r24 = 3;
        r0 = r22;
        r1 = r24;
        r2 = r23;
        r0.<init>(r1, r10, r2, r14);
        r21.handleError(r22);
    L_0x01cc:
        r21 = 82;
        r0 = r21;
        r21 = createLSException(r0, r14);
        r21 = r21.fillInStackTrace();
        r21 = (org.w3c.dom.ls.LSException) r21;
        throw r21;
    L_0x01db:
        r21 = r27.getNodeType();
        r22 = 6;
        r0 = r21;
        r1 = r22;
        if (r0 == r1) goto L_0x0140;
    L_0x01e7:
        goto L_0x0125;
    L_0x01e9:
        r4 = org.apache.xml.serializer.utils.SystemIDResolver.getAbsoluteURI(r15);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r16 = new java.net.URL;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r16;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0.<init>(r4);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r18 = 0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r12 = r16.getProtocol();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r7 = r16.getHost();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = "file";	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r12.equalsIgnoreCase(r0);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r21 == 0) goto L_0x0297;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0209:
        if (r7 == 0) goto L_0x0211;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x020b:
        r21 = r7.length();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r21 != 0) goto L_0x028c;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0211:
        r18 = new java.io.FileOutputStream;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r16.getPath();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = getPathWithoutEscapes(r21);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r18;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0.<init>(r1);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0222:
        r0 = r18;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r13.setOutputStream(r0);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0227:
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fDOMSerializer;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r21 != 0) goto L_0x023b;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x022f:
        r21 = r13.asDOM3Serializer();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = (org.apache.xml.serializer.DOM3Serializer) r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1.fDOMSerializer = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x023b:
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fDOMErrorHandler;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r21 == 0) goto L_0x0252;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0243:
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fDOMSerializer;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fDOMErrorHandler;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r22 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21.setErrorHandler(r22);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0252:
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fSerializerFilter;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r21 == 0) goto L_0x0269;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x025a:
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fDOMSerializer;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fSerializerFilter;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r22 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21.setNodeFilter(r22);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0269:
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fDOMSerializer;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fEndOfLine;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r22 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r22 = r22.toCharArray();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21.setNewLine(r22);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r26;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0.fDOMSerializer;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1 = r27;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0.serializeDOM3(r1);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = 1;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        return r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x028c:
        r21 = "localhost";	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r7.equals(r0);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r21 != 0) goto L_0x0211;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x0297:
        r17 = r16.openConnection();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = 0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r17;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0.setDoInput(r1);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = 1;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r17;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0.setDoOutput(r1);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = 0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r17;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0.setUseCaches(r1);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = 0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r17;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r1 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0.setAllowUserInteraction(r1);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r17;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r0 instanceof java.net.HttpURLConnection;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        if (r21 == 0) goto L_0x02d4;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x02c7:
        r0 = r17;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = (java.net.HttpURLConnection) r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r8 = r0;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r21 = "PUT";	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r0 = r21;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r8.setRequestMethod(r0);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x02d4:
        r18 = r17.getOutputStream();	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        goto L_0x0222;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
    L_0x02da:
        r13.setOutputStream(r11);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        goto L_0x0227;
    L_0x02df:
        r9 = move-exception;
        throw r9;
    L_0x02e1:
        r0 = r19;	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        r13.setWriter(r0);	 Catch:{ UnsupportedEncodingException -> 0x019f, LSException -> 0x02df, RuntimeException -> 0x02e8, Exception -> 0x02f8 }
        goto L_0x0227;
    L_0x02e8:
        r6 = move-exception;
        r21 = 82;
        r0 = r21;
        r21 = createLSException(r0, r6);
        r21 = r21.fillInStackTrace();
        r21 = (org.w3c.dom.ls.LSException) r21;
        throw r21;
    L_0x02f8:
        r5 = move-exception;
        r0 = r26;
        r0 = r0.fDOMErrorHandler;
        r21 = r0;
        if (r21 == 0) goto L_0x031f;
    L_0x0301:
        r0 = r26;
        r0 = r0.fDOMErrorHandler;
        r21 = r0;
        r22 = new org.apache.xml.serializer.dom3.DOMErrorImpl;
        r23 = r5.getMessage();
        r24 = 3;
        r25 = 0;
        r0 = r22;
        r1 = r24;
        r2 = r23;
        r3 = r25;
        r0.<init>(r1, r2, r3, r5);
        r21.handleError(r22);
    L_0x031f:
        r21 = 82;
        r0 = r21;
        r21 = createLSException(r0, r5);
        r21 = r21.fillInStackTrace();
        r21 = (org.w3c.dom.ls.LSException) r21;
        throw r21;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.apache.xml.serializer.dom3.LSSerializerImpl.write(org.w3c.dom.Node, org.w3c.dom.ls.LSOutput):boolean");
    }

    public String writeToString(Node nodeArg) throws DOMException, LSException {
        if (nodeArg == null) {
            return null;
        }
        Serializer serializer = this.fXMLSerializer;
        serializer.reset();
        if (nodeArg != this.fVisitedNode) {
            String xmlVersion = getXMLVersion(nodeArg);
            serializer.getOutputFormat().setProperty("version", xmlVersion);
            this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}xml-version", xmlVersion);
            this.fDOMConfigProperties.setProperty("encoding", "UTF-16");
            if (!((nodeArg.getNodeType() == (short) 9 && nodeArg.getNodeType() == (short) 1 && nodeArg.getNodeType() == (short) 6) || (this.fFeatures & 262144) == 0)) {
                this.fDOMConfigProperties.setProperty("omit-xml-declaration", DOMConstants.DOM3_DEFAULT_FALSE);
            }
            this.fVisitedNode = nodeArg;
        }
        this.fXMLSerializer.setOutputFormat(this.fDOMConfigProperties);
        StringWriter output = new StringWriter();
        try {
            serializer.setWriter(output);
            if (this.fDOMSerializer == null) {
                this.fDOMSerializer = (DOM3Serializer) serializer.asDOM3Serializer();
            }
            if (this.fDOMErrorHandler != null) {
                this.fDOMSerializer.setErrorHandler(this.fDOMErrorHandler);
            }
            if (this.fSerializerFilter != null) {
                this.fDOMSerializer.setNodeFilter(this.fSerializerFilter);
            }
            this.fDOMSerializer.setNewLine(this.fEndOfLine.toCharArray());
            this.fDOMSerializer.serializeDOM3(nodeArg);
            return output.toString();
        } catch (LSException lse) {
            throw lse;
        } catch (RuntimeException e) {
            throw ((LSException) createLSException((short) 82, e).fillInStackTrace());
        } catch (Exception e2) {
            if (this.fDOMErrorHandler != null) {
                this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, e2.getMessage(), null, e2));
            }
            throw ((LSException) createLSException((short) 82, e2).fillInStackTrace());
        }
    }

    public boolean writeToURI(Node nodeArg, String uri) throws LSException {
        if (nodeArg == null) {
            return false;
        }
        Serializer serializer = this.fXMLSerializer;
        serializer.reset();
        if (nodeArg != this.fVisitedNode) {
            String xmlVersion = getXMLVersion(nodeArg);
            this.fEncoding = getInputEncoding(nodeArg);
            if (this.fEncoding == null) {
                String xMLEncoding = this.fEncoding != null ? this.fEncoding : getXMLEncoding(nodeArg) == null ? "UTF-8" : getXMLEncoding(nodeArg);
                this.fEncoding = xMLEncoding;
            }
            serializer.getOutputFormat().setProperty("version", xmlVersion);
            this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}xml-version", xmlVersion);
            this.fDOMConfigProperties.setProperty("encoding", this.fEncoding);
            if (!((nodeArg.getNodeType() == (short) 9 && nodeArg.getNodeType() == (short) 1 && nodeArg.getNodeType() == (short) 6) || (this.fFeatures & 262144) == 0)) {
                this.fDOMConfigProperties.setProperty("omit-xml-declaration", DOMConstants.DOM3_DEFAULT_FALSE);
            }
            this.fVisitedNode = nodeArg;
        }
        this.fXMLSerializer.setOutputFormat(this.fDOMConfigProperties);
        if (uri == null) {
            try {
                String msg = Utils.messages.createMessage(MsgKey.ER_NO_OUTPUT_SPECIFIED, null);
                if (this.fDOMErrorHandler != null) {
                    this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, msg, MsgKey.ER_NO_OUTPUT_SPECIFIED));
                }
                throw new LSException((short) 82, msg);
            } catch (LSException lse) {
                throw lse;
            } catch (RuntimeException e) {
                throw ((LSException) createLSException((short) 82, e).fillInStackTrace());
            } catch (Exception e2) {
                if (this.fDOMErrorHandler != null) {
                    this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, e2.getMessage(), null, e2));
                }
                throw ((LSException) createLSException((short) 82, e2).fillInStackTrace());
            }
        }
        OutputStream urlOutStream;
        URL url = new URL(SystemIDResolver.getAbsoluteURI(uri));
        String protocol = url.getProtocol();
        String host = url.getHost();
        if (protocol.equalsIgnoreCase("file") && (host == null || host.length() == 0 || host.equals("localhost"))) {
            urlOutStream = new FileOutputStream(getPathWithoutEscapes(url.getPath()));
        } else {
            URLConnection urlCon = url.openConnection();
            urlCon.setDoInput(false);
            urlCon.setDoOutput(true);
            urlCon.setUseCaches(false);
            urlCon.setAllowUserInteraction(false);
            if (urlCon instanceof HttpURLConnection) {
                ((HttpURLConnection) urlCon).setRequestMethod("PUT");
            }
            urlOutStream = urlCon.getOutputStream();
        }
        serializer.setOutputStream(urlOutStream);
        if (this.fDOMSerializer == null) {
            this.fDOMSerializer = (DOM3Serializer) serializer.asDOM3Serializer();
        }
        if (this.fDOMErrorHandler != null) {
            this.fDOMSerializer.setErrorHandler(this.fDOMErrorHandler);
        }
        if (this.fSerializerFilter != null) {
            this.fDOMSerializer.setNodeFilter(this.fSerializerFilter);
        }
        this.fDOMSerializer.setNewLine(this.fEndOfLine.toCharArray());
        this.fDOMSerializer.serializeDOM3(nodeArg);
        return true;
    }

    protected String getXMLVersion(Node nodeArg) {
        if (nodeArg != null) {
            Document doc;
            if (nodeArg.getNodeType() == (short) 9) {
                doc = (Document) nodeArg;
            } else {
                doc = nodeArg.getOwnerDocument();
            }
            if (doc != null && doc.getImplementation().hasFeature("Core", "3.0")) {
                return doc.getXmlVersion();
            }
        }
        return SerializerConstants.XMLVERSION10;
    }

    protected String getXMLEncoding(Node nodeArg) {
        if (nodeArg != null) {
            Document doc;
            if (nodeArg.getNodeType() == (short) 9) {
                doc = (Document) nodeArg;
            } else {
                doc = nodeArg.getOwnerDocument();
            }
            if (doc != null && doc.getImplementation().hasFeature("Core", "3.0")) {
                return doc.getXmlEncoding();
            }
        }
        return "UTF-8";
    }

    protected String getInputEncoding(Node nodeArg) {
        if (nodeArg != null) {
            Document doc;
            if (nodeArg.getNodeType() == (short) 9) {
                doc = (Document) nodeArg;
            } else {
                doc = nodeArg.getOwnerDocument();
            }
            if (doc != null && doc.getImplementation().hasFeature("Core", "3.0")) {
                return doc.getInputEncoding();
            }
        }
        return null;
    }

    public DOMErrorHandler getErrorHandler() {
        return this.fDOMErrorHandler;
    }

    private static String getPathWithoutEscapes(String origPath) {
        if (origPath == null || origPath.length() == 0 || origPath.indexOf(37) == -1) {
            return origPath;
        }
        StringTokenizer tokenizer = new StringTokenizer(origPath, "%");
        StringBuffer result = new StringBuffer(origPath.length());
        int size = tokenizer.countTokens();
        result.append(tokenizer.nextToken());
        for (int i = 1; i < size; i++) {
            String token = tokenizer.nextToken();
            if (token.length() >= 2 && isHexDigit(token.charAt(0)) && isHexDigit(token.charAt(1))) {
                result.append((char) Integer.valueOf(token.substring(0, 2), 16).intValue());
                token = token.substring(2);
            }
            result.append(token);
        }
        return result.toString();
    }

    private static boolean isHexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return true;
        }
        if (c >= 'a' && c <= 'f') {
            return true;
        }
        if (c < 'A' || c > 'F') {
            return false;
        }
        return true;
    }

    private static LSException createLSException(short code, Throwable cause) {
        String str = null;
        if (cause != null) {
            str = cause.getMessage();
        }
        LSException lse = new LSException(code, str);
        if (cause != null && ThrowableMethods.fgThrowableMethodsAvailable) {
            try {
                ThrowableMethods.fgThrowableInitCauseMethod.invoke(lse, new Object[]{cause});
            } catch (Exception e) {
            }
        }
        return lse;
    }
}
