package org.apache.http.impl.client;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.BasicRouteDirector;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRouteDirector;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

@Deprecated
public class DefaultRequestDirector implements RequestDirector {
    private static Method cleartextTrafficPermittedMethod;
    private static Object networkSecurityPolicy;
    protected final ClientConnectionManager connManager;
    protected final HttpProcessor httpProcessor;
    protected final ConnectionKeepAliveStrategy keepAliveStrategy;
    private final Log log = LogFactory.getLog(getClass());
    protected ManagedClientConnection managedConn;
    private int maxRedirects;
    protected final HttpParams params;
    private final AuthenticationHandler proxyAuthHandler;
    private final AuthState proxyAuthState;
    private int redirectCount;
    protected final RedirectHandler redirectHandler;
    protected final HttpRequestExecutor requestExec;
    protected final HttpRequestRetryHandler retryHandler;
    protected final ConnectionReuseStrategy reuseStrategy;
    protected final HttpRoutePlanner routePlanner;
    private final AuthenticationHandler targetAuthHandler;
    private final AuthState targetAuthState;
    private final UserTokenHandler userTokenHandler;

    public DefaultRequestDirector(HttpRequestExecutor requestExec, ClientConnectionManager conman, ConnectionReuseStrategy reustrat, ConnectionKeepAliveStrategy kastrat, HttpRoutePlanner rouplan, HttpProcessor httpProcessor, HttpRequestRetryHandler retryHandler, RedirectHandler redirectHandler, AuthenticationHandler targetAuthHandler, AuthenticationHandler proxyAuthHandler, UserTokenHandler userTokenHandler, HttpParams params) {
        if (requestExec == null) {
            throw new IllegalArgumentException("Request executor may not be null.");
        } else if (conman == null) {
            throw new IllegalArgumentException("Client connection manager may not be null.");
        } else if (reustrat == null) {
            throw new IllegalArgumentException("Connection reuse strategy may not be null.");
        } else if (kastrat == null) {
            throw new IllegalArgumentException("Connection keep alive strategy may not be null.");
        } else if (rouplan == null) {
            throw new IllegalArgumentException("Route planner may not be null.");
        } else if (httpProcessor == null) {
            throw new IllegalArgumentException("HTTP protocol processor may not be null.");
        } else if (retryHandler == null) {
            throw new IllegalArgumentException("HTTP request retry handler may not be null.");
        } else if (redirectHandler == null) {
            throw new IllegalArgumentException("Redirect handler may not be null.");
        } else if (targetAuthHandler == null) {
            throw new IllegalArgumentException("Target authentication handler may not be null.");
        } else if (proxyAuthHandler == null) {
            throw new IllegalArgumentException("Proxy authentication handler may not be null.");
        } else if (userTokenHandler == null) {
            throw new IllegalArgumentException("User token handler may not be null.");
        } else if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        } else {
            this.requestExec = requestExec;
            this.connManager = conman;
            this.reuseStrategy = reustrat;
            this.keepAliveStrategy = kastrat;
            this.routePlanner = rouplan;
            this.httpProcessor = httpProcessor;
            this.retryHandler = retryHandler;
            this.redirectHandler = redirectHandler;
            this.targetAuthHandler = targetAuthHandler;
            this.proxyAuthHandler = proxyAuthHandler;
            this.userTokenHandler = userTokenHandler;
            this.params = params;
            this.managedConn = null;
            this.redirectCount = 0;
            this.maxRedirects = this.params.getIntParameter(ClientPNames.MAX_REDIRECTS, 100);
            this.targetAuthState = new AuthState();
            this.proxyAuthState = new AuthState();
        }
    }

    private RequestWrapper wrapRequest(HttpRequest request) throws ProtocolException {
        if (request instanceof HttpEntityEnclosingRequest) {
            return new EntityEnclosingRequestWrapper((HttpEntityEnclosingRequest) request);
        }
        return new RequestWrapper(request);
    }

    protected void rewriteRequestURI(RequestWrapper request, HttpRoute route) throws ProtocolException {
        try {
            URI uri = request.getURI();
            if (route.getProxyHost() == null || (route.isTunnelled() ^ 1) == 0) {
                if (uri.isAbsolute()) {
                    request.setURI(URIUtils.rewriteURI(uri, null));
                }
            } else if (!uri.isAbsolute()) {
                request.setURI(URIUtils.rewriteURI(uri, route.getTargetHost()));
            }
        } catch (URISyntaxException ex) {
            throw new ProtocolException("Invalid URI: " + request.getRequestLine().getUri(), ex);
        }
    }

    public org.apache.http.HttpResponse execute(org.apache.http.HttpHost r36, org.apache.http.HttpRequest r37, org.apache.http.protocol.HttpContext r38) throws org.apache.http.HttpException, java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r26_0 'roureq' org.apache.http.impl.client.RoutedRequest) in PHI: PHI: (r26_1 'roureq' org.apache.http.impl.client.RoutedRequest) = (r26_0 'roureq' org.apache.http.impl.client.RoutedRequest), (r26_2 'roureq' org.apache.http.impl.client.RoutedRequest) binds: {(r26_0 'roureq' org.apache.http.impl.client.RoutedRequest)=B:0:0x0000, (r26_2 'roureq' org.apache.http.impl.client.RoutedRequest)=B:120:0x003e}
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
        r35 = this;
        r19 = r37;
        r0 = r35;
        r1 = r37;
        r21 = r0.wrapRequest(r1);
        r0 = r35;
        r0 = r0.params;
        r32 = r0;
        r0 = r21;
        r1 = r32;
        r0.setParams(r1);
        r0 = r35;
        r1 = r36;
        r2 = r21;
        r3 = r38;
        r20 = r0.determineRoute(r1, r2, r3);
        r26 = new org.apache.http.impl.client.RoutedRequest;
        r0 = r26;
        r1 = r21;
        r2 = r20;
        r0.<init>(r1, r2);
        r0 = r35;
        r0 = r0.params;
        r32 = r0;
        r28 = org.apache.http.conn.params.ConnManagerParams.getTimeout(r32);
        r14 = 0;
        r25 = 0;
        r23 = 0;
        r5 = 0;
    L_0x003e:
        if (r5 != 0) goto L_0x0218;
    L_0x0040:
        r31 = r26.getRequest();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r27 = r26.getRoute();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = "http.user-token";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r30 = r0.getAttribute(r1);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 != 0) goto L_0x00c7;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x005b:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.connManager;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r27;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r30;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r4 = r0.requestConnection(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r37;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0 instanceof org.apache.http.client.methods.AbortableHttpRequest;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x007e;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0073:
        r0 = r37;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = (org.apache.http.client.methods.AbortableHttpRequest) r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setConnectionRequest(r4);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x007e:
        r32 = java.util.concurrent.TimeUnit.MILLISECONDS;	 Catch:{ InterruptedException -> 0x01cd }
        r0 = r28;	 Catch:{ InterruptedException -> 0x01cd }
        r2 = r32;	 Catch:{ InterruptedException -> 0x01cd }
        r32 = r4.getConnection(r0, r2);	 Catch:{ InterruptedException -> 0x01cd }
        r0 = r32;	 Catch:{ InterruptedException -> 0x01cd }
        r1 = r35;	 Catch:{ InterruptedException -> 0x01cd }
        r1.managedConn = r0;	 Catch:{ InterruptedException -> 0x01cd }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.params;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = org.apache.http.params.HttpConnectionParams.isStaleCheckingEnabled(r32);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x00c7;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x009a:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = "Stale connection check";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.debug(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32.isStale();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x00c7;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x00b2:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = "Stale connection detected";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.debug(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ IOException -> 0x046c, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ IOException -> 0x046c, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ IOException -> 0x046c, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32.close();	 Catch:{ IOException -> 0x046c, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
    L_0x00c7:
        r0 = r37;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0 instanceof org.apache.http.client.methods.AbortableHttpRequest;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x00de;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x00cf:
        r0 = r37;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = (org.apache.http.client.methods.AbortableHttpRequest) r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.setReleaseTrigger(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x00de:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32.isOpen();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 != 0) goto L_0x01e0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x00ea:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.params;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r27;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r3 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.open(r1, r2, r3);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0101:
        r0 = r35;	 Catch:{ TunnelRefusedException -> 0x01fa }
        r1 = r27;	 Catch:{ TunnelRefusedException -> 0x01fa }
        r2 = r38;	 Catch:{ TunnelRefusedException -> 0x01fa }
        r0.establishRoute(r1, r2);	 Catch:{ TunnelRefusedException -> 0x01fa }
        r31.resetHeaders();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r31;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r27;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.rewriteRequestURI(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r31.getParams();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = "http.virtual-host";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32.getParameter(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = (org.apache.http.HttpHost) r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r36 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r36 != 0) goto L_0x012d;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0129:
        r36 = r27.getTargetHost();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x012d:
        r22 = r27.getProxyHost();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = "http.target_host";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r36;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setAttribute(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = "http.proxy_host";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r22;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setAttribute(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = "http.connection";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setAttribute(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = "http.auth.target-scope";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.targetAuthState;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setAttribute(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = "http.auth.proxy-scope";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.proxyAuthState;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setAttribute(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.requestExec;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.httpProcessor;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r31;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r3 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.preProcess(r1, r2, r3);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = "http.request";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r31;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setAttribute(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r24 = 1;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x01a4:
        if (r24 == 0) goto L_0x0373;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x01a6:
        r14 = r14 + 1;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r31.incrementExecCount();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r31.getExecCount();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = 1;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r0 <= r1) goto L_0x022f;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x01b7:
        r32 = r31.isRepeatable();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32 ^ 1;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x022f;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x01bf:
        r32 = new org.apache.http.client.NonRepeatableRequestException;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = "Cannot retry request with a non-repeatable request entity";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.<init>(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        throw r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x01c8:
        r12 = move-exception;
        r35.abortConnection();
        throw r12;
    L_0x01cd:
        r17 = move-exception;
        r18 = new java.io.InterruptedIOException;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r18.<init>();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r18;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r17;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.initCause(r1);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        throw r18;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x01db:
        r10 = move-exception;
        r35.abortConnection();
        throw r10;
    L_0x01e0:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.params;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = org.apache.http.params.HttpConnectionParams.getSoTimeout(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.setSocketTimeout(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        goto L_0x0101;
    L_0x01f5:
        r11 = move-exception;
        r35.abortConnection();
        throw r11;
    L_0x01fa:
        r13 = move-exception;
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32.isDebugEnabled();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x0214;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0207:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r13.getMessage();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.debug(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0214:
        r23 = r13.getResponse();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0218:
        if (r23 == 0) goto L_0x0220;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x021a:
        r32 = r23.getEntity();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 != 0) goto L_0x0446;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0220:
        if (r25 == 0) goto L_0x022b;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0222:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.markReusable();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x022b:
        r35.releaseConnection();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x022e:
        return r23;
    L_0x022f:
        r0 = r35;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32 = r32.isDebugEnabled();	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x0261;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
    L_0x023b:
        r0 = r35;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33.<init>();	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r34 = "Attempt ";	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = r33.append(r34);	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r0 = r33;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = r0.append(r14);	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r34 = " to execute request";	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = r33.append(r34);	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = r33.toString();	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32.debug(r33);	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
    L_0x0261:
        r32 = r27.isSecure();	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        if (r32 != 0) goto L_0x0355;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
    L_0x0267:
        r32 = r27.getTargetHost();	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32 = r32.getHostName();	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32 = isCleartextTrafficPermitted(r32);	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32 = r32 ^ 1;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x0355;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
    L_0x0277:
        r32 = new java.io.IOException;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33.<init>();	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r34 = "Cleartext traffic not permitted: ";	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = r33.append(r34);	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r34 = r27.getTargetHost();	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = r33.append(r34);	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = r33.toString();	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32.<init>(r33);	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        throw r32;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
    L_0x0295:
        r10 = move-exception;
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = "Closing the connection.";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.debug(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.close();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.retryHandler;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0.retryRequest(r10, r14, r1);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x0371;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x02bb:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32.isInfoEnabled();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x02fb;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x02c7:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = new java.lang.StringBuilder;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33.<init>();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r34 = "I/O exception (";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r33.append(r34);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r34 = r10.getClass();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r34 = r34.getName();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r33.append(r34);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r34 = ") caught when processing request: ";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r33.append(r34);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r34 = r10.getMessage();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r33.append(r34);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r33.toString();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.info(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x02fb:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32.isDebugEnabled();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x0318;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0307:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r10.getMessage();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.debug(r1, r10);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0318:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = "Retrying request";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.info(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r27.getHopCount();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = 1;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r0 != r1) goto L_0x0372;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0330:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = "Reopening the direct connection.";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.debug(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.params;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r27;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r3 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.open(r1, r2, r3);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        goto L_0x01a4;
    L_0x0355:
        r0 = r35;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r0 = r0.requestExec;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r1 = r31;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r2 = r33;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r3 = r38;	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r23 = r0.execute(r1, r2, r3);	 Catch:{ IOException -> 0x0295, HttpException -> 0x01c8, RuntimeException -> 0x01f5 }
        r24 = 0;
        goto L_0x01a4;
    L_0x0371:
        throw r10;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0372:
        throw r10;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0373:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.params;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r23;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setParams(r1);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.requestExec;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.httpProcessor;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r23;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r3 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.postProcess(r1, r2, r3);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.reuseStrategy;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r23;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r25 = r0.keepAlive(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r25 == 0) goto L_0x03c8;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x03a9:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.keepAliveStrategy;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r23;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r6 = r0.getKeepAliveDuration(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = java.util.concurrent.TimeUnit.MILLISECONDS;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r33;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setIdleDuration(r6, r1);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x03c8:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r26;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r23;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r3 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r15 = r0.handleResponse(r1, r2, r3);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r15 != 0) goto L_0x0408;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x03d6:
        r5 = 1;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x03d7:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.userTokenHandler;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r30 = r0.getUserToken(r1);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = "http.user-token";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r38;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r2 = r30;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setAttribute(r1, r2);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 == 0) goto L_0x003e;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x03f9:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r30;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setState(r1);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        goto L_0x003e;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0408:
        if (r25 == 0) goto L_0x043c;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x040a:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.log;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = "Connection kept alive";	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.debug(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r8 = r23.getEntity();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r8 == 0) goto L_0x041f;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x041c:
        r8.consumeContent();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x041f:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.markReusable();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0428:
        r32 = r15.getRoute();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r33 = r26.getRoute();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32.equals(r33);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 != 0) goto L_0x0439;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0436:
        r35.releaseConnection();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0439:
        r26 = r15;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        goto L_0x03d7;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x043c:
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32.close();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        goto L_0x0428;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0446:
        r32 = r23.getEntity();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32.isStreaming();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r32 ^ 1;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        if (r32 != 0) goto L_0x0220;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
    L_0x0452:
        r8 = r23.getEntity();	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r9 = new org.apache.http.conn.BasicManagedEntity;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r35;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r0.managedConn;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r32 = r0;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r32;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r1 = r25;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r9.<init>(r8, r0, r1);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0 = r23;	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        r0.setEntity(r9);	 Catch:{ HttpException -> 0x01c8, IOException -> 0x01db, RuntimeException -> 0x01f5 }
        goto L_0x022e;
    L_0x046c:
        r16 = move-exception;
        goto L_0x00c7;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.apache.http.impl.client.DefaultRequestDirector.execute(org.apache.http.HttpHost, org.apache.http.HttpRequest, org.apache.http.protocol.HttpContext):org.apache.http.HttpResponse");
    }

    protected void releaseConnection() {
        try {
            this.managedConn.releaseConnection();
        } catch (IOException ignored) {
            this.log.debug("IOException releasing connection", ignored);
        }
        this.managedConn = null;
    }

    protected HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        if (target == null) {
            target = (HttpHost) request.getParams().getParameter(ClientPNames.DEFAULT_HOST);
        }
        if (target != null) {
            return this.routePlanner.determineRoute(target, request, context);
        }
        String scheme = null;
        String host = null;
        String path = null;
        if (request instanceof HttpUriRequest) {
            URI uri = ((HttpUriRequest) request).getURI();
            if (uri != null) {
                scheme = uri.getScheme();
                host = uri.getHost();
                path = uri.getPath();
            }
        }
        throw new IllegalStateException("Target host must not be null, or set in parameters. scheme=" + scheme + ", host=" + host + ", path=" + path);
    }

    protected void establishRoute(HttpRoute route, HttpContext context) throws HttpException, IOException {
        HttpRouteDirector rowdy = new BasicRouteDirector();
        int step;
        do {
            HttpRoute fact = this.managedConn.getRoute();
            step = rowdy.nextStep(route, fact);
            boolean secure;
            switch (step) {
                case -1:
                    throw new IllegalStateException("Unable to establish route.\nplanned = " + route + "\ncurrent = " + fact);
                case 0:
                    break;
                case 1:
                case 2:
                    this.managedConn.open(route, context, this.params);
                    continue;
                case 3:
                    secure = createTunnelToTarget(route, context);
                    this.log.debug("Tunnel to target created.");
                    this.managedConn.tunnelTarget(secure, this.params);
                    continue;
                case 4:
                    int hop = fact.getHopCount() - 1;
                    secure = createTunnelToProxy(route, hop, context);
                    this.log.debug("Tunnel to proxy created.");
                    this.managedConn.tunnelProxy(route.getHopTarget(hop), secure, this.params);
                    continue;
                case 5:
                    this.managedConn.layerProtocol(context, this.params);
                    continue;
                default:
                    throw new IllegalStateException("Unknown step indicator " + step + " from RouteDirector.");
            }
        } while (step > 0);
    }

    protected boolean createTunnelToTarget(HttpRoute route, HttpContext context) throws HttpException, IOException {
        HttpEntity entity;
        HttpHost proxy = route.getProxyHost();
        HttpHost target = route.getTargetHost();
        HttpResponse response = null;
        boolean done = false;
        while (!done) {
            done = true;
            if (!this.managedConn.isOpen()) {
                this.managedConn.open(route, context, this.params);
            }
            HttpRequest connect = createConnectRequest(route, context);
            String agent = HttpProtocolParams.getUserAgent(this.params);
            if (agent != null) {
                connect.addHeader(HTTP.USER_AGENT, agent);
            }
            connect.addHeader(HTTP.TARGET_HOST, target.toHostString());
            AuthScheme authScheme = this.proxyAuthState.getAuthScheme();
            AuthScope authScope = this.proxyAuthState.getAuthScope();
            Credentials creds = this.proxyAuthState.getCredentials();
            if (!(creds == null || (authScope == null && (authScheme.isConnectionBased() ^ 1) == 0))) {
                try {
                    connect.addHeader(authScheme.authenticate(creds, connect));
                } catch (AuthenticationException ex) {
                    if (this.log.isErrorEnabled()) {
                        this.log.error("Proxy authentication error: " + ex.getMessage());
                    }
                }
            }
            response = this.requestExec.execute(connect, this.managedConn, context);
            if (response.getStatusLine().getStatusCode() < 200) {
                throw new HttpException("Unexpected response to CONNECT request: " + response.getStatusLine());
            }
            CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
            if (credsProvider != null && HttpClientParams.isAuthenticating(this.params)) {
                if (this.proxyAuthHandler.isAuthenticationRequested(response, context)) {
                    this.log.debug("Proxy requested authentication");
                    try {
                        processChallenges(this.proxyAuthHandler.getChallenges(response, context), this.proxyAuthState, this.proxyAuthHandler, response, context);
                    } catch (AuthenticationException ex2) {
                        if (this.log.isWarnEnabled()) {
                            this.log.warn("Authentication error: " + ex2.getMessage());
                            break;
                        }
                    }
                    updateAuthState(this.proxyAuthState, proxy, credsProvider);
                    if (this.proxyAuthState.getCredentials() != null) {
                        done = false;
                        if (this.reuseStrategy.keepAlive(response, context)) {
                            this.log.debug("Connection kept alive");
                            entity = response.getEntity();
                            if (entity != null) {
                                entity.consumeContent();
                            }
                        } else {
                            this.managedConn.close();
                        }
                    }
                } else {
                    this.proxyAuthState.setAuthScope(null);
                }
            }
        }
        if (response.getStatusLine().getStatusCode() > 299) {
            entity = response.getEntity();
            if (entity != null) {
                response.setEntity(new BufferedHttpEntity(entity));
            }
            this.managedConn.close();
            throw new TunnelRefusedException("CONNECT refused by proxy: " + response.getStatusLine(), response);
        }
        this.managedConn.markReusable();
        return false;
    }

    protected boolean createTunnelToProxy(HttpRoute route, int hop, HttpContext context) throws HttpException, IOException {
        throw new UnsupportedOperationException("Proxy chains are not supported.");
    }

    protected HttpRequest createConnectRequest(HttpRoute route, HttpContext context) {
        HttpHost target = route.getTargetHost();
        String host = target.getHostName();
        int port = target.getPort();
        if (port < 0) {
            port = this.connManager.getSchemeRegistry().getScheme(target.getSchemeName()).getDefaultPort();
        }
        StringBuilder buffer = new StringBuilder(host.length() + 6);
        buffer.append(host);
        buffer.append(':');
        buffer.append(Integer.toString(port));
        return new BasicHttpRequest("CONNECT", buffer.toString(), HttpProtocolParams.getVersion(this.params));
    }

    protected RoutedRequest handleResponse(RoutedRequest roureq, HttpResponse response, HttpContext context) throws HttpException, IOException {
        HttpRoute route = roureq.getRoute();
        HttpHost proxy = route.getProxyHost();
        RequestWrapper request = roureq.getRequest();
        HttpParams params = request.getParams();
        if (!HttpClientParams.isRedirecting(params) || !this.redirectHandler.isRedirectRequested(response, context)) {
            CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
            if (credsProvider != null && HttpClientParams.isAuthenticating(params)) {
                if (this.targetAuthHandler.isAuthenticationRequested(response, context)) {
                    HttpHost target = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                    if (target == null) {
                        target = route.getTargetHost();
                    }
                    this.log.debug("Target requested authentication");
                    try {
                        processChallenges(this.targetAuthHandler.getChallenges(response, context), this.targetAuthState, this.targetAuthHandler, response, context);
                    } catch (AuthenticationException ex) {
                        if (this.log.isWarnEnabled()) {
                            this.log.warn("Authentication error: " + ex.getMessage());
                            return null;
                        }
                    }
                    updateAuthState(this.targetAuthState, target, credsProvider);
                    if (this.targetAuthState.getCredentials() != null) {
                        return roureq;
                    }
                    return null;
                }
                this.targetAuthState.setAuthScope(null);
                if (this.proxyAuthHandler.isAuthenticationRequested(response, context)) {
                    this.log.debug("Proxy requested authentication");
                    try {
                        processChallenges(this.proxyAuthHandler.getChallenges(response, context), this.proxyAuthState, this.proxyAuthHandler, response, context);
                    } catch (AuthenticationException ex2) {
                        if (this.log.isWarnEnabled()) {
                            this.log.warn("Authentication error: " + ex2.getMessage());
                            return null;
                        }
                    }
                    updateAuthState(this.proxyAuthState, proxy, credsProvider);
                    if (this.proxyAuthState.getCredentials() != null) {
                        return roureq;
                    }
                    return null;
                }
                this.proxyAuthState.setAuthScope(null);
            }
            return null;
        } else if (this.redirectCount >= this.maxRedirects) {
            throw new RedirectException("Maximum redirects (" + this.maxRedirects + ") exceeded");
        } else {
            this.redirectCount++;
            URI uri = this.redirectHandler.getLocationURI(response, context);
            HttpHost newTarget = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
            HttpGet httpGet = new HttpGet(uri);
            httpGet.setHeaders(request.getOriginal().getAllHeaders());
            RequestWrapper requestWrapper = new RequestWrapper(httpGet);
            requestWrapper.setParams(params);
            HttpRoute newRoute = determineRoute(newTarget, requestWrapper, context);
            RoutedRequest newRequest = new RoutedRequest(requestWrapper, newRoute);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Redirecting to '" + uri + "' via " + newRoute);
            }
            return newRequest;
        }
    }

    private void abortConnection() {
        ManagedClientConnection mcc = this.managedConn;
        if (mcc != null) {
            this.managedConn = null;
            try {
                mcc.abortConnection();
            } catch (IOException ex) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug(ex.getMessage(), ex);
                }
            }
            try {
                mcc.releaseConnection();
            } catch (IOException ignored) {
                this.log.debug("Error releasing connection", ignored);
            }
        }
    }

    private void processChallenges(Map<String, Header> challenges, AuthState authState, AuthenticationHandler authHandler, HttpResponse response, HttpContext context) throws MalformedChallengeException, AuthenticationException {
        AuthScheme authScheme = authState.getAuthScheme();
        if (authScheme == null) {
            authScheme = authHandler.selectScheme(challenges, response, context);
            authState.setAuthScheme(authScheme);
        }
        String id = authScheme.getSchemeName();
        Header challenge = (Header) challenges.get(id.toLowerCase(Locale.ENGLISH));
        if (challenge == null) {
            throw new AuthenticationException(id + " authorization challenge expected, but not found");
        }
        authScheme.processChallenge(challenge);
        this.log.debug("Authorization challenge processed");
    }

    private void updateAuthState(AuthState authState, HttpHost host, CredentialsProvider credsProvider) {
        if (authState.isValid()) {
            String hostname = host.getHostName();
            int port = host.getPort();
            if (port < 0) {
                port = this.connManager.getSchemeRegistry().getScheme(host).getDefaultPort();
            }
            AuthScheme authScheme = authState.getAuthScheme();
            AuthScope authScope = new AuthScope(hostname, port, authScheme.getRealm(), authScheme.getSchemeName());
            if (this.log.isDebugEnabled()) {
                this.log.debug("Authentication scope: " + authScope);
            }
            Credentials creds = authState.getCredentials();
            if (creds == null) {
                creds = credsProvider.getCredentials(authScope);
                if (this.log.isDebugEnabled()) {
                    if (creds != null) {
                        this.log.debug("Found credentials");
                    } else {
                        this.log.debug("Credentials not found");
                    }
                }
            } else if (authScheme.isComplete()) {
                this.log.debug("Authentication failed");
                creds = null;
            }
            authState.setAuthScope(authScope);
            authState.setCredentials(creds);
        }
    }

    private static boolean isCleartextTrafficPermitted(String hostname) {
        try {
            Object policy;
            Method method;
            synchronized (DefaultRequestDirector.class) {
                if (cleartextTrafficPermittedMethod == null) {
                    Class<?> cls = Class.forName("android.security.NetworkSecurityPolicy");
                    networkSecurityPolicy = cls.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
                    cleartextTrafficPermittedMethod = cls.getMethod("isCleartextTrafficPermitted", new Class[]{String.class});
                }
                policy = networkSecurityPolicy;
                method = cleartextTrafficPermittedMethod;
            }
            return ((Boolean) method.invoke(policy, new Object[]{hostname})).booleanValue();
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }
}
