package org.apache.http.client.protocol;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequestInterceptor;

@Deprecated
public class RequestAddCookies implements HttpRequestInterceptor {
    private final Log log = LogFactory.getLog(getClass());

    public void process(org.apache.http.HttpRequest r27, org.apache.http.protocol.HttpContext r28) throws org.apache.http.HttpException, java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r20_2 'requestURI' java.net.URI) in PHI: PHI: (r20_1 'requestURI' java.net.URI) = (r20_0 'requestURI' java.net.URI), (r20_2 'requestURI' java.net.URI) binds: {(r20_0 'requestURI' java.net.URI)=B:27:0x00bb, (r20_2 'requestURI' java.net.URI)=B:41:?}
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
        if (r27 != 0) goto L_0x000b;
    L_0x0002:
        r23 = new java.lang.IllegalArgumentException;
        r24 = "HTTP request may not be null";
        r23.<init>(r24);
        throw r23;
    L_0x000b:
        if (r28 != 0) goto L_0x0016;
    L_0x000d:
        r23 = new java.lang.IllegalArgumentException;
        r24 = "HTTP context may not be null";
        r23.<init>(r24);
        throw r23;
    L_0x0016:
        r23 = "http.cookie-store";
        r0 = r28;
        r1 = r23;
        r8 = r0.getAttribute(r1);
        r8 = (org.apache.http.client.CookieStore) r8;
        if (r8 != 0) goto L_0x0032;
    L_0x0025:
        r0 = r26;
        r0 = r0.log;
        r23 = r0;
        r24 = "Cookie store not available in HTTP context";
        r23.info(r24);
        return;
    L_0x0032:
        r23 = "http.cookiespec-registry";
        r0 = r28;
        r1 = r23;
        r19 = r0.getAttribute(r1);
        r19 = (org.apache.http.cookie.CookieSpecRegistry) r19;
        if (r19 != 0) goto L_0x004e;
    L_0x0041:
        r0 = r26;
        r0 = r0.log;
        r23 = r0;
        r24 = "CookieSpec registry not available in HTTP context";
        r23.info(r24);
        return;
    L_0x004e:
        r23 = "http.target_host";
        r0 = r28;
        r1 = r23;
        r21 = r0.getAttribute(r1);
        r21 = (org.apache.http.HttpHost) r21;
        if (r21 != 0) goto L_0x0066;
    L_0x005d:
        r23 = new java.lang.IllegalStateException;
        r24 = "Target host not specified in HTTP context";
        r23.<init>(r24);
        throw r23;
    L_0x0066:
        r23 = "http.connection";
        r0 = r28;
        r1 = r23;
        r3 = r0.getAttribute(r1);
        r3 = (org.apache.http.conn.ManagedClientConnection) r3;
        if (r3 != 0) goto L_0x007e;
    L_0x0075:
        r23 = new java.lang.IllegalStateException;
        r24 = "Client connection not specified in HTTP context";
        r23.<init>(r24);
        throw r23;
    L_0x007e:
        r23 = r27.getParams();
        r17 = org.apache.http.client.params.HttpClientParams.getCookiePolicy(r23);
        r0 = r26;
        r0 = r0.log;
        r23 = r0;
        r23 = r23.isDebugEnabled();
        if (r23 == 0) goto L_0x00b3;
    L_0x0092:
        r0 = r26;
        r0 = r0.log;
        r23 = r0;
        r24 = new java.lang.StringBuilder;
        r24.<init>();
        r25 = "CookieSpec selected: ";
        r24 = r24.append(r25);
        r0 = r24;
        r1 = r17;
        r24 = r0.append(r1);
        r24 = r24.toString();
        r23.debug(r24);
    L_0x00b3:
        r0 = r27;
        r0 = r0 instanceof org.apache.http.client.methods.HttpUriRequest;
        r23 = r0;
        if (r23 == 0) goto L_0x0154;
    L_0x00bb:
        r23 = r27;
        r23 = (org.apache.http.client.methods.HttpUriRequest) r23;
        r20 = r23.getURI();
    L_0x00c3:
        r14 = r21.getHostName();
        r18 = r21.getPort();
        if (r18 >= 0) goto L_0x00d1;
    L_0x00cd:
        r18 = r3.getRemotePort();
    L_0x00d1:
        r6 = new org.apache.http.cookie.CookieOrigin;
        r23 = r20.getPath();
        r24 = r3.isSecure();
        r0 = r18;
        r1 = r23;
        r2 = r24;
        r6.<init>(r14, r0, r1, r2);
        r23 = r27.getParams();
        r0 = r19;
        r1 = r17;
        r2 = r23;
        r7 = r0.getCookieSpec(r1, r2);
        r9 = new java.util.ArrayList;
        r23 = r8.getCookies();
        r0 = r23;
        r9.<init>(r0);
        r15 = new java.util.ArrayList;
        r15.<init>();
        r5 = r9.iterator();
    L_0x0106:
        r23 = r5.hasNext();
        if (r23 == 0) goto L_0x018e;
    L_0x010c:
        r4 = r5.next();
        r4 = (org.apache.http.cookie.Cookie) r4;
        r23 = r7.match(r4, r6);
        if (r23 == 0) goto L_0x0106;
    L_0x0118:
        r0 = r26;
        r0 = r0.log;
        r23 = r0;
        r23 = r23.isDebugEnabled();
        if (r23 == 0) goto L_0x0150;
    L_0x0124:
        r0 = r26;
        r0 = r0.log;
        r23 = r0;
        r24 = new java.lang.StringBuilder;
        r24.<init>();
        r25 = "Cookie ";
        r24 = r24.append(r25);
        r0 = r24;
        r24 = r0.append(r4);
        r25 = " match ";
        r24 = r24.append(r25);
        r0 = r24;
        r24 = r0.append(r6);
        r24 = r24.toString();
        r23.debug(r24);
    L_0x0150:
        r15.add(r4);
        goto L_0x0106;
    L_0x0154:
        r20 = new java.net.URI;	 Catch:{ URISyntaxException -> 0x0167 }
        r23 = r27.getRequestLine();	 Catch:{ URISyntaxException -> 0x0167 }
        r23 = r23.getUri();	 Catch:{ URISyntaxException -> 0x0167 }
        r0 = r20;	 Catch:{ URISyntaxException -> 0x0167 }
        r1 = r23;	 Catch:{ URISyntaxException -> 0x0167 }
        r0.<init>(r1);	 Catch:{ URISyntaxException -> 0x0167 }
        goto L_0x00c3;
    L_0x0167:
        r10 = move-exception;
        r23 = new org.apache.http.ProtocolException;
        r24 = new java.lang.StringBuilder;
        r24.<init>();
        r25 = "Invalid request URI: ";
        r24 = r24.append(r25);
        r25 = r27.getRequestLine();
        r25 = r25.getUri();
        r24 = r24.append(r25);
        r24 = r24.toString();
        r0 = r23;
        r1 = r24;
        r0.<init>(r1, r10);
        throw r23;
    L_0x018e:
        r23 = r15.isEmpty();
        if (r23 != 0) goto L_0x01ae;
    L_0x0194:
        r13 = r7.formatCookies(r15);
        r12 = r13.iterator();
    L_0x019c:
        r23 = r12.hasNext();
        if (r23 == 0) goto L_0x01ae;
    L_0x01a2:
        r11 = r12.next();
        r11 = (org.apache.http.Header) r11;
        r0 = r27;
        r0.addHeader(r11);
        goto L_0x019c;
    L_0x01ae:
        r22 = r7.getVersion();
        if (r22 <= 0) goto L_0x01e0;
    L_0x01b4:
        r16 = 0;
        r5 = r15.iterator();
    L_0x01ba:
        r23 = r5.hasNext();
        if (r23 == 0) goto L_0x01d3;
    L_0x01c0:
        r4 = r5.next();
        r4 = (org.apache.http.cookie.Cookie) r4;
        r23 = r4.getVersion();
        r0 = r22;
        r1 = r23;
        if (r0 == r1) goto L_0x01ba;
    L_0x01d0:
        r16 = 1;
        goto L_0x01ba;
    L_0x01d3:
        if (r16 == 0) goto L_0x01e0;
    L_0x01d5:
        r11 = r7.getVersionHeader();
        if (r11 == 0) goto L_0x01e0;
    L_0x01db:
        r0 = r27;
        r0.addHeader(r11);
    L_0x01e0:
        r23 = "http.cookie-spec";
        r0 = r28;
        r1 = r23;
        r0.setAttribute(r1, r7);
        r23 = "http.cookie-origin";
        r0 = r28;
        r1 = r23;
        r0.setAttribute(r1, r6);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.apache.http.client.protocol.RequestAddCookies.process(org.apache.http.HttpRequest, org.apache.http.protocol.HttpContext):void");
    }
}
