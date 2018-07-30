package org.apache.xalan.processor;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.utils.SystemIDResolver;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProcessorInclude extends XSLTElementProcessor {
    static final long serialVersionUID = -4570078731972673481L;
    private String m_href = null;

    public String getHref() {
        return this.m_href;
    }

    public void setHref(String baseIdent) {
        this.m_href = baseIdent;
    }

    protected int getStylesheetType() {
        return 2;
    }

    protected String getStylesheetInclErr() {
        return XSLTErrorResources.ER_STYLESHEET_INCLUDES_ITSELF;
    }

    public void startElement(StylesheetHandler handler, String uri, String localName, String rawName, Attributes attributes) throws SAXException {
        setPropertiesFromAttributes(handler, rawName, attributes, this);
        int savedStylesheetType;
        try {
            Source sourceFromURIResolver = getSourceFromUriResolver(handler);
            String hrefUrl = getBaseURIOfIncludedStylesheet(handler, sourceFromURIResolver);
            if (handler.importStackContains(hrefUrl)) {
                throw new SAXException(XSLMessages.createMessage(getStylesheetInclErr(), new Object[]{hrefUrl}));
            }
            handler.pushImportURL(hrefUrl);
            handler.pushImportSource(sourceFromURIResolver);
            savedStylesheetType = handler.getStylesheetType();
            handler.setStylesheetType(getStylesheetType());
            handler.pushNewNamespaceSupport();
            parse(handler, uri, localName, rawName, attributes);
            handler.setStylesheetType(savedStylesheetType);
            handler.popImportURL();
            handler.popImportSource();
            handler.popNamespaceSupport();
        } catch (TransformerException te) {
            handler.error(te.getMessage(), te);
        } catch (Throwable th) {
            handler.setStylesheetType(savedStylesheetType);
            handler.popImportURL();
            handler.popImportSource();
            handler.popNamespaceSupport();
        }
    }

    protected void parse(org.apache.xalan.processor.StylesheetHandler r29, java.lang.String r30, java.lang.String r31, java.lang.String r32, org.xml.sax.Attributes r33) throws org.xml.sax.SAXException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r18_2 'source' javax.xml.transform.Source) in PHI: PHI: (r18_5 'source' javax.xml.transform.Source) = (r18_2 'source' javax.xml.transform.Source), (r18_7 'source' javax.xml.transform.Source) binds: {(r18_2 'source' javax.xml.transform.Source)=B:20:?, (r18_7 'source' javax.xml.transform.Source)=B:69:0x0130}
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
        r28 = this;
        r14 = r29.getStylesheetProcessor();
        r22 = r14.getURIResolver();
        r18 = 0;
        if (r22 == 0) goto L_0x0134;
    L_0x000c:
        r18 = r29.peekSourceFromURIResolver();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        if (r18 == 0) goto L_0x004e;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x0012:
        r0 = r18;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = r0 instanceof javax.xml.transform.dom.DOMSource;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r24 = r0;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        if (r24 == 0) goto L_0x0134;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x001a:
        r0 = r18;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = (javax.xml.transform.dom.DOMSource) r0;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r24 = r0;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r13 = r24.getNode();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r20 = r29.peekImportURL();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        if (r20 == 0) goto L_0x0031;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x002a:
        r0 = r29;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r1 = r20;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0.pushBaseIndentifier(r1);	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x0031:
        r23 = new org.apache.xml.utils.TreeWalker;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r24 = new org.apache.xml.utils.DOM2Helper;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r24.<init>();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = r23;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r1 = r29;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r2 = r24;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r3 = r20;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0.<init>(r1, r2, r3);	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = r23;	 Catch:{ SAXException -> 0x00d0 }
        r0.traverse(r13);	 Catch:{ SAXException -> 0x00d0 }
        if (r20 == 0) goto L_0x004d;
    L_0x004a:
        r29.popBaseIndentifier();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x004d:
        return;
    L_0x004e:
        r19 = r18;
    L_0x0050:
        if (r19 != 0) goto L_0x0130;
    L_0x0052:
        r24 = r28.getHref();	 Catch:{ IOException -> 0x0121, TransformerException -> 0x0125 }
        r25 = r29.getBaseIdentifier();	 Catch:{ IOException -> 0x0121, TransformerException -> 0x0125 }
        r4 = org.apache.xml.utils.SystemIDResolver.getAbsoluteURI(r24, r25);	 Catch:{ IOException -> 0x0121, TransformerException -> 0x0125 }
        r18 = new javax.xml.transform.stream.StreamSource;	 Catch:{ IOException -> 0x0121, TransformerException -> 0x0125 }
        r0 = r18;	 Catch:{ IOException -> 0x0121, TransformerException -> 0x0125 }
        r0.<init>(r4);	 Catch:{ IOException -> 0x0121, TransformerException -> 0x0125 }
    L_0x0065:
        r0 = r28;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r1 = r29;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r2 = r18;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r18 = r0.processSource(r1, r2);	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r15 = 0;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = r18;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = r0 instanceof javax.xml.transform.sax.SAXSource;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r24 = r0;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        if (r24 == 0) goto L_0x0082;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x0078:
        r0 = r18;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = (javax.xml.transform.sax.SAXSource) r0;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r16 = r0;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r15 = r16.getXMLReader();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x0082:
        r10 = javax.xml.transform.sax.SAXSource.sourceToInputSource(r18);	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        if (r15 != 0) goto L_0x00b1;
    L_0x0088:
        r9 = javax.xml.parsers.SAXParserFactory.newInstance();	 Catch:{ ParserConfigurationException -> 0x0113, FactoryConfigurationError -> 0x00f9, NoSuchMethodError -> 0x0129, AbstractMethodError -> 0x012b }
        r24 = 1;	 Catch:{ ParserConfigurationException -> 0x0113, FactoryConfigurationError -> 0x00f9, NoSuchMethodError -> 0x0129, AbstractMethodError -> 0x012b }
        r0 = r24;	 Catch:{ ParserConfigurationException -> 0x0113, FactoryConfigurationError -> 0x00f9, NoSuchMethodError -> 0x0129, AbstractMethodError -> 0x012b }
        r9.setNamespaceAware(r0);	 Catch:{ ParserConfigurationException -> 0x0113, FactoryConfigurationError -> 0x00f9, NoSuchMethodError -> 0x0129, AbstractMethodError -> 0x012b }
        r24 = r29.getStylesheetProcessor();	 Catch:{ ParserConfigurationException -> 0x0113, FactoryConfigurationError -> 0x00f9, NoSuchMethodError -> 0x0129, AbstractMethodError -> 0x012b }
        r24 = r24.isSecureProcessing();	 Catch:{ ParserConfigurationException -> 0x0113, FactoryConfigurationError -> 0x00f9, NoSuchMethodError -> 0x0129, AbstractMethodError -> 0x012b }
        if (r24 == 0) goto L_0x00a9;
    L_0x009d:
        r24 = "http://javax.xml.XMLConstants/feature/secure-processing";	 Catch:{ SAXException -> 0x012d }
        r25 = 1;	 Catch:{ SAXException -> 0x012d }
        r0 = r24;	 Catch:{ SAXException -> 0x012d }
        r1 = r25;	 Catch:{ SAXException -> 0x012d }
        r9.setFeature(r0, r1);	 Catch:{ SAXException -> 0x012d }
    L_0x00a9:
        r12 = r9.newSAXParser();	 Catch:{ ParserConfigurationException -> 0x0113, FactoryConfigurationError -> 0x00f9, NoSuchMethodError -> 0x0129, AbstractMethodError -> 0x012b }
        r15 = r12.getXMLReader();	 Catch:{ ParserConfigurationException -> 0x0113, FactoryConfigurationError -> 0x00f9, NoSuchMethodError -> 0x0129, AbstractMethodError -> 0x012b }
    L_0x00b1:
        if (r15 != 0) goto L_0x00b7;
    L_0x00b3:
        r15 = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x00b7:
        if (r15 == 0) goto L_0x00cf;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x00b9:
        r0 = r29;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r15.setContentHandler(r0);	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r24 = r10.getSystemId();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = r29;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r1 = r24;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0.pushBaseIndentifier(r1);	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r15.parse(r10);	 Catch:{ all -> 0x011c }
        r29.popBaseIndentifier();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x00cf:
        return;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x00d0:
        r17 = move-exception;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r24 = new javax.xml.transform.TransformerException;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = r24;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r1 = r17;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0.<init>(r1);	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        throw r24;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x00db:
        r11 = move-exception;
    L_0x00dc:
        r24 = "ER_IOEXCEPTION";
        r25 = 1;
        r0 = r25;
        r0 = new java.lang.Object[r0];
        r25 = r0;
        r26 = r28.getHref();
        r27 = 0;
        r25[r27] = r26;
        r0 = r29;
        r1 = r24;
        r2 = r25;
        r0.error(r1, r2, r11);
        goto L_0x00cf;
    L_0x00f9:
        r7 = move-exception;
        r24 = new org.xml.sax.SAXException;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r25 = r7.toString();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r24.<init>(r25);	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        throw r24;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x0104:
        r21 = move-exception;
    L_0x0105:
        r24 = r21.getMessage();
        r0 = r29;
        r1 = r24;
        r2 = r21;
        r0.error(r1, r2);
        goto L_0x00cf;
    L_0x0113:
        r6 = move-exception;
        r24 = new org.xml.sax.SAXException;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0 = r24;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r0.<init>(r6);	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        throw r24;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x011c:
        r24 = move-exception;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        r29.popBaseIndentifier();	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
        throw r24;	 Catch:{ IOException -> 0x00db, TransformerException -> 0x0104 }
    L_0x0121:
        r11 = move-exception;
        r18 = r19;
        goto L_0x00dc;
    L_0x0125:
        r21 = move-exception;
        r18 = r19;
        goto L_0x0105;
    L_0x0129:
        r8 = move-exception;
        goto L_0x00b1;
    L_0x012b:
        r5 = move-exception;
        goto L_0x00b1;
    L_0x012d:
        r17 = move-exception;
        goto L_0x00a9;
    L_0x0130:
        r18 = r19;
        goto L_0x0065;
    L_0x0134:
        r19 = r18;
        goto L_0x0050;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.apache.xalan.processor.ProcessorInclude.parse(org.apache.xalan.processor.StylesheetHandler, java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes):void");
    }

    protected Source processSource(StylesheetHandler handler, Source source) {
        return source;
    }

    private Source getSourceFromUriResolver(StylesheetHandler handler) throws TransformerException {
        URIResolver uriresolver = handler.getStylesheetProcessor().getURIResolver();
        if (uriresolver != null) {
            return uriresolver.resolve(getHref(), handler.getBaseIdentifier());
        }
        return null;
    }

    private String getBaseURIOfIncludedStylesheet(StylesheetHandler handler, Source s) throws TransformerException {
        if (s != null) {
            String idFromUriResolverSource = s.getSystemId();
            if (idFromUriResolverSource != null) {
                return idFromUriResolverSource;
            }
        }
        return SystemIDResolver.getAbsoluteURI(getHref(), handler.getBaseIdentifier());
    }
}
