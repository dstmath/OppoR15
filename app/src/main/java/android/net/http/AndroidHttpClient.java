package android.net.http;

import android.content.ContentResolver;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AUTH;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.cookie.SM;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;

public final class AndroidHttpClient implements HttpClient {
    public static long DEFAULT_SYNC_MIN_GZIP_BYTES = 256;
    private static final int SOCKET_OPERATION_TIMEOUT = 60000;
    private static final String TAG = "AndroidHttpClient";
    private static final HttpRequestInterceptor sThreadCheckInterceptor = new HttpRequestInterceptor() {
        public void process(HttpRequest request, HttpContext context) {
            if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
                throw new RuntimeException("This thread forbids HTTP requests");
            }
        }
    };
    private static String[] textContentTypes = new String[]{"text/", "application/xml", "application/json"};
    private volatile LoggingConfiguration curlConfiguration;
    private final HttpClient delegate;
    private RuntimeException mLeakedException = new IllegalStateException("AndroidHttpClient created and never closed");

    private class CurlLogger implements HttpRequestInterceptor {
        /* synthetic */ CurlLogger(AndroidHttpClient this$0, CurlLogger -this1) {
            this();
        }

        private CurlLogger() {
        }

        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            LoggingConfiguration configuration = AndroidHttpClient.this.curlConfiguration;
            if (configuration != null && configuration.isLoggable() && (request instanceof HttpUriRequest)) {
                configuration.println(AndroidHttpClient.toCurl((HttpUriRequest) request, false));
            }
        }
    }

    private static class LoggingConfiguration {
        private final int level;
        private final String tag;

        /* synthetic */ LoggingConfiguration(String tag, int level, LoggingConfiguration -this2) {
            this(tag, level);
        }

        private LoggingConfiguration(String tag, int level) {
            this.tag = tag;
            this.level = level;
        }

        private boolean isLoggable() {
            return Log.isLoggable(this.tag, this.level);
        }

        private void println(String message) {
            Log.println(this.level, this.tag, message);
        }
    }

    public static android.net.http.AndroidHttpClient newInstance(java.lang.String r26, android.content.Context r27) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r19_2 'sessionCache' android.net.SSLSessionCache) in PHI: PHI: (r19_1 'sessionCache' android.net.SSLSessionCache) = (r19_0 'sessionCache' android.net.SSLSessionCache), (r19_2 'sessionCache' android.net.SSLSessionCache) binds: {(r19_0 'sessionCache' android.net.SSLSessionCache)=B:13:0x01c5, (r19_2 'sessionCache' android.net.SSLSessionCache)=B:16:0x0211}
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
        r22 = "AndroidHttpClient";	 Catch:{ Exception -> 0x018b }
        r23 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x018b }
        r23.<init>();	 Catch:{ Exception -> 0x018b }
        r24 = "AndroidHttpClient newInstance";	 Catch:{ Exception -> 0x018b }
        r23 = r23.append(r24);	 Catch:{ Exception -> 0x018b }
        r0 = r23;	 Catch:{ Exception -> 0x018b }
        r1 = r26;	 Catch:{ Exception -> 0x018b }
        r23 = r0.append(r1);	 Catch:{ Exception -> 0x018b }
        r23 = r23.toString();	 Catch:{ Exception -> 0x018b }
        android.util.Log.d(r22, r23);	 Catch:{ Exception -> 0x018b }
        r22 = "Android-Mms";	 Catch:{ Exception -> 0x018b }
        r0 = r26;	 Catch:{ Exception -> 0x018b }
        r1 = r22;	 Catch:{ Exception -> 0x018b }
        r22 = r0.contains(r1);	 Catch:{ Exception -> 0x018b }
        if (r22 == 0) goto L_0x0199;	 Catch:{ Exception -> 0x018b }
    L_0x002b:
        r22 = "android.os.SystemProperties";	 Catch:{ Exception -> 0x018b }
        r21 = java.lang.Class.forName(r22);	 Catch:{ Exception -> 0x018b }
        r22 = "getBoolean";	 Catch:{ Exception -> 0x018b }
        r23 = 2;	 Catch:{ Exception -> 0x018b }
        r0 = r23;	 Catch:{ Exception -> 0x018b }
        r0 = new java.lang.Class[r0];	 Catch:{ Exception -> 0x018b }
        r23 = r0;	 Catch:{ Exception -> 0x018b }
        r24 = java.lang.String.class;	 Catch:{ Exception -> 0x018b }
        r25 = 0;	 Catch:{ Exception -> 0x018b }
        r23[r25] = r24;	 Catch:{ Exception -> 0x018b }
        r24 = java.lang.Boolean.TYPE;	 Catch:{ Exception -> 0x018b }
        r25 = 1;	 Catch:{ Exception -> 0x018b }
        r23[r25] = r24;	 Catch:{ Exception -> 0x018b }
        r7 = r21.getDeclaredMethod(r22, r23);	 Catch:{ Exception -> 0x018b }
        r22 = 1;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r7.setAccessible(r0);	 Catch:{ Exception -> 0x018b }
        r15 = r21.newInstance();	 Catch:{ Exception -> 0x018b }
        r22 = 2;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r0 = new java.lang.Object[r0];	 Catch:{ Exception -> 0x018b }
        r22 = r0;	 Catch:{ Exception -> 0x018b }
        r23 = "persist.sys.permission.enable";	 Catch:{ Exception -> 0x018b }
        r24 = 0;	 Catch:{ Exception -> 0x018b }
        r22[r24] = r23;	 Catch:{ Exception -> 0x018b }
        r23 = 0;	 Catch:{ Exception -> 0x018b }
        r23 = java.lang.Boolean.valueOf(r23);	 Catch:{ Exception -> 0x018b }
        r24 = 1;	 Catch:{ Exception -> 0x018b }
        r22[r24] = r23;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r22 = r7.invoke(r15, r0);	 Catch:{ Exception -> 0x018b }
        r22 = (java.lang.Boolean) r22;	 Catch:{ Exception -> 0x018b }
        r14 = r22.booleanValue();	 Catch:{ Exception -> 0x018b }
        if (r14 == 0) goto L_0x0199;	 Catch:{ Exception -> 0x018b }
    L_0x007f:
        r22 = "android.os.ServiceManager";	 Catch:{ Exception -> 0x018b }
        r17 = java.lang.Class.forName(r22);	 Catch:{ Exception -> 0x018b }
        r22 = "getService";	 Catch:{ Exception -> 0x018b }
        r23 = 1;	 Catch:{ Exception -> 0x018b }
        r0 = r23;	 Catch:{ Exception -> 0x018b }
        r0 = new java.lang.Class[r0];	 Catch:{ Exception -> 0x018b }
        r23 = r0;	 Catch:{ Exception -> 0x018b }
        r24 = java.lang.String.class;	 Catch:{ Exception -> 0x018b }
        r25 = 0;	 Catch:{ Exception -> 0x018b }
        r23[r25] = r24;	 Catch:{ Exception -> 0x018b }
        r0 = r17;	 Catch:{ Exception -> 0x018b }
        r1 = r22;	 Catch:{ Exception -> 0x018b }
        r2 = r23;	 Catch:{ Exception -> 0x018b }
        r8 = r0.getDeclaredMethod(r1, r2);	 Catch:{ Exception -> 0x018b }
        r22 = 1;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r8.setAccessible(r0);	 Catch:{ Exception -> 0x018b }
        r18 = r21.newInstance();	 Catch:{ Exception -> 0x018b }
        r22 = 1;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r0 = new java.lang.Object[r0];	 Catch:{ Exception -> 0x018b }
        r22 = r0;	 Catch:{ Exception -> 0x018b }
        r23 = "permission";	 Catch:{ Exception -> 0x018b }
        r24 = 0;	 Catch:{ Exception -> 0x018b }
        r22[r24] = r23;	 Catch:{ Exception -> 0x018b }
        r0 = r18;	 Catch:{ Exception -> 0x018b }
        r1 = r22;	 Catch:{ Exception -> 0x018b }
        r11 = r8.invoke(r0, r1);	 Catch:{ Exception -> 0x018b }
        r11 = (android.os.IBinder) r11;	 Catch:{ Exception -> 0x018b }
        r22 = "android.os.IPermissionController$Stub";	 Catch:{ Exception -> 0x018b }
        r20 = java.lang.Class.forName(r22);	 Catch:{ Exception -> 0x018b }
        r22 = "asInterface";	 Catch:{ Exception -> 0x018b }
        r23 = 1;	 Catch:{ Exception -> 0x018b }
        r0 = r23;	 Catch:{ Exception -> 0x018b }
        r0 = new java.lang.Class[r0];	 Catch:{ Exception -> 0x018b }
        r23 = r0;	 Catch:{ Exception -> 0x018b }
        r24 = android.os.IBinder.class;	 Catch:{ Exception -> 0x018b }
        r25 = 0;	 Catch:{ Exception -> 0x018b }
        r23[r25] = r24;	 Catch:{ Exception -> 0x018b }
        r0 = r20;	 Catch:{ Exception -> 0x018b }
        r1 = r22;	 Catch:{ Exception -> 0x018b }
        r2 = r23;	 Catch:{ Exception -> 0x018b }
        r3 = r0.getDeclaredMethod(r1, r2);	 Catch:{ Exception -> 0x018b }
        r22 = 1;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r3.setAccessible(r0);	 Catch:{ Exception -> 0x018b }
        r22 = "android.os.IPermissionController";	 Catch:{ Exception -> 0x018b }
        r12 = java.lang.Class.forName(r22);	 Catch:{ Exception -> 0x018b }
        r22 = "checkPermission";	 Catch:{ Exception -> 0x018b }
        r23 = 3;	 Catch:{ Exception -> 0x018b }
        r0 = r23;	 Catch:{ Exception -> 0x018b }
        r0 = new java.lang.Class[r0];	 Catch:{ Exception -> 0x018b }
        r23 = r0;	 Catch:{ Exception -> 0x018b }
        r24 = java.lang.String.class;	 Catch:{ Exception -> 0x018b }
        r25 = 0;	 Catch:{ Exception -> 0x018b }
        r23[r25] = r24;	 Catch:{ Exception -> 0x018b }
        r24 = java.lang.Integer.TYPE;	 Catch:{ Exception -> 0x018b }
        r25 = 1;	 Catch:{ Exception -> 0x018b }
        r23[r25] = r24;	 Catch:{ Exception -> 0x018b }
        r24 = java.lang.Integer.TYPE;	 Catch:{ Exception -> 0x018b }
        r25 = 2;	 Catch:{ Exception -> 0x018b }
        r23[r25] = r24;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r1 = r23;	 Catch:{ Exception -> 0x018b }
        r4 = r12.getDeclaredMethod(r0, r1);	 Catch:{ Exception -> 0x018b }
        r22 = 1;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r4.setAccessible(r0);	 Catch:{ Exception -> 0x018b }
        r22 = 1;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r0 = new java.lang.Object[r0];	 Catch:{ Exception -> 0x018b }
        r22 = r0;	 Catch:{ Exception -> 0x018b }
        r23 = 0;	 Catch:{ Exception -> 0x018b }
        r22[r23] = r11;	 Catch:{ Exception -> 0x018b }
        r23 = 0;	 Catch:{ Exception -> 0x018b }
        r0 = r23;	 Catch:{ Exception -> 0x018b }
        r1 = r22;	 Catch:{ Exception -> 0x018b }
        r13 = r3.invoke(r0, r1);	 Catch:{ Exception -> 0x018b }
        r22 = 3;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r0 = new java.lang.Object[r0];	 Catch:{ Exception -> 0x018b }
        r22 = r0;	 Catch:{ Exception -> 0x018b }
        r23 = "android.permission.SEND_MMS";	 Catch:{ Exception -> 0x018b }
        r24 = 0;	 Catch:{ Exception -> 0x018b }
        r22[r24] = r23;	 Catch:{ Exception -> 0x018b }
        r23 = android.os.Process.myPid();	 Catch:{ Exception -> 0x018b }
        r23 = java.lang.Integer.valueOf(r23);	 Catch:{ Exception -> 0x018b }
        r24 = 1;	 Catch:{ Exception -> 0x018b }
        r22[r24] = r23;	 Catch:{ Exception -> 0x018b }
        r23 = android.os.Process.myUid();	 Catch:{ Exception -> 0x018b }
        r23 = java.lang.Integer.valueOf(r23);	 Catch:{ Exception -> 0x018b }
        r24 = 2;	 Catch:{ Exception -> 0x018b }
        r22[r24] = r23;	 Catch:{ Exception -> 0x018b }
        r0 = r22;	 Catch:{ Exception -> 0x018b }
        r5 = r4.invoke(r13, r0);	 Catch:{ Exception -> 0x018b }
        r5 = (java.lang.Boolean) r5;	 Catch:{ Exception -> 0x018b }
        r22 = "AndroidHttpClient";	 Catch:{ Exception -> 0x018b }
        r23 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x018b }
        r23.<init>();	 Catch:{ Exception -> 0x018b }
        r24 = "check result";	 Catch:{ Exception -> 0x018b }
        r23 = r23.append(r24);	 Catch:{ Exception -> 0x018b }
        r0 = r23;	 Catch:{ Exception -> 0x018b }
        r23 = r0.append(r5);	 Catch:{ Exception -> 0x018b }
        r23 = r23.toString();	 Catch:{ Exception -> 0x018b }
        android.util.Log.d(r22, r23);	 Catch:{ Exception -> 0x018b }
        r22 = r5.booleanValue();	 Catch:{ Exception -> 0x018b }
        if (r22 != 0) goto L_0x0199;
    L_0x0188:
        r22 = 0;
        return r22;
    L_0x018b:
        r6 = move-exception;
        r22 = "AndroidHttpClient";
        r23 = "Exception in AndroidHttpClient is ";
        r0 = r22;
        r1 = r23;
        android.util.Log.e(r0, r1, r6);
    L_0x0199:
        r10 = new org.apache.http.params.BasicHttpParams;
        r10.<init>();
        r22 = 0;
        r0 = r22;
        org.apache.http.params.HttpConnectionParams.setStaleCheckingEnabled(r10, r0);
        r22 = 60000; // 0xea60 float:8.4078E-41 double:2.9644E-319;
        r0 = r22;
        org.apache.http.params.HttpConnectionParams.setConnectionTimeout(r10, r0);
        r22 = 60000; // 0xea60 float:8.4078E-41 double:2.9644E-319;
        r0 = r22;
        org.apache.http.params.HttpConnectionParams.setSoTimeout(r10, r0);
        r22 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;
        r0 = r22;
        org.apache.http.params.HttpConnectionParams.setSocketBufferSize(r10, r0);
        r22 = 0;
        r0 = r22;
        org.apache.http.client.params.HttpClientParams.setRedirecting(r10, r0);
        if (r27 != 0) goto L_0x0211;
    L_0x01c5:
        r19 = 0;
    L_0x01c7:
        r0 = r26;
        org.apache.http.params.HttpProtocolParams.setUserAgent(r10, r0);
        r16 = new org.apache.http.conn.scheme.SchemeRegistry;
        r16.<init>();
        r22 = new org.apache.http.conn.scheme.Scheme;
        r23 = "http";
        r24 = org.apache.http.conn.scheme.PlainSocketFactory.getSocketFactory();
        r25 = 80;
        r22.<init>(r23, r24, r25);
        r0 = r16;
        r1 = r22;
        r0.register(r1);
        r22 = new org.apache.http.conn.scheme.Scheme;
        r23 = "https";
        r24 = 60000; // 0xea60 float:8.4078E-41 double:2.9644E-319;
        r0 = r24;
        r1 = r19;
        r24 = android.net.SSLCertificateSocketFactory.getHttpSocketFactory(r0, r1);
        r25 = 443; // 0x1bb float:6.21E-43 double:2.19E-321;
        r22.<init>(r23, r24, r25);
        r0 = r16;
        r1 = r22;
        r0.register(r1);
        r9 = new org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
        r0 = r16;
        r9.<init>(r10, r0);
        r22 = new android.net.http.AndroidHttpClient;
        r0 = r22;
        r0.<init>(r9, r10);
        return r22;
    L_0x0211:
        r19 = new android.net.SSLSessionCache;
        r0 = r19;
        r1 = r27;
        r0.<init>(r1);
        goto L_0x01c7;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.net.http.AndroidHttpClient.newInstance(java.lang.String, android.content.Context):android.net.http.AndroidHttpClient");
    }

    public static AndroidHttpClient newInstance(String userAgent) {
        return newInstance(userAgent, null);
    }

    private AndroidHttpClient(ClientConnectionManager ccm, HttpParams params) {
        this.delegate = new DefaultHttpClient(ccm, params) {
            protected BasicHttpProcessor createHttpProcessor() {
                BasicHttpProcessor processor = super.createHttpProcessor();
                processor.addRequestInterceptor(AndroidHttpClient.sThreadCheckInterceptor);
                processor.addRequestInterceptor(new CurlLogger(AndroidHttpClient.this, null));
                return processor;
            }

            protected HttpContext createHttpContext() {
                HttpContext context = new BasicHttpContext();
                context.setAttribute(ClientContext.AUTHSCHEME_REGISTRY, getAuthSchemes());
                context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, getCookieSpecs());
                context.setAttribute(ClientContext.CREDS_PROVIDER, getCredentialsProvider());
                return context;
            }
        };
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (this.mLeakedException != null) {
            Log.e(TAG, "Leak found", this.mLeakedException);
            this.mLeakedException = null;
        }
    }

    public static void modifyRequestToAcceptGzipResponse(HttpRequest request) {
        request.addHeader("Accept-Encoding", "gzip");
    }

    public static InputStream getUngzippedContent(HttpEntity entity) throws IOException {
        InputStream responseStream = entity.getContent();
        if (responseStream == null) {
            return responseStream;
        }
        Header header = entity.getContentEncoding();
        if (header == null) {
            return responseStream;
        }
        String contentEncoding = header.getValue();
        if (contentEncoding == null) {
            return responseStream;
        }
        if (contentEncoding.contains("gzip")) {
            responseStream = new GZIPInputStream(responseStream);
        }
        return responseStream;
    }

    public void close() {
        if (this.mLeakedException != null) {
            getConnectionManager().shutdown();
            this.mLeakedException = null;
        }
    }

    public HttpParams getParams() {
        return this.delegate.getParams();
    }

    public ClientConnectionManager getConnectionManager() {
        return this.delegate.getConnectionManager();
    }

    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return this.delegate.execute(request);
    }

    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
        return this.delegate.execute(request, context);
    }

    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
        return this.delegate.execute(target, request);
    }

    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        return this.delegate.execute(target, request, context);
    }

    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return this.delegate.execute(request, (ResponseHandler) responseHandler);
    }

    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return this.delegate.execute(request, (ResponseHandler) responseHandler, context);
    }

    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return this.delegate.execute(target, request, (ResponseHandler) responseHandler);
    }

    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return this.delegate.execute(target, request, responseHandler, context);
    }

    public static AbstractHttpEntity getCompressedEntity(byte[] data, ContentResolver resolver) throws IOException {
        if (((long) data.length) < getMinGzipSize(resolver)) {
            return new ByteArrayEntity(data);
        }
        ByteArrayOutputStream arr = new ByteArrayOutputStream();
        OutputStream zipper = new GZIPOutputStream(arr);
        zipper.write(data);
        zipper.close();
        AbstractHttpEntity entity = new ByteArrayEntity(arr.toByteArray());
        entity.setContentEncoding("gzip");
        return entity;
    }

    public static long getMinGzipSize(ContentResolver resolver) {
        return DEFAULT_SYNC_MIN_GZIP_BYTES;
    }

    public void enableCurlLogging(String name, int level) {
        if (name == null) {
            throw new NullPointerException("name");
        } else if (level < 2 || level > 7) {
            throw new IllegalArgumentException("Level is out of range [2..7]");
        } else {
            this.curlConfiguration = new LoggingConfiguration(name, level, null);
        }
    }

    public void disableCurlLogging() {
        this.curlConfiguration = null;
    }

    private static String toCurl(HttpUriRequest request, boolean logAuthToken) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("curl ");
        builder.append("-X ");
        builder.append(request.getMethod());
        builder.append(" ");
        for (Header header : request.getAllHeaders()) {
            if (logAuthToken || !(header.getName().equals(AUTH.WWW_AUTH_RESP) || header.getName().equals(SM.COOKIE))) {
                builder.append("--header \"");
                builder.append(header.toString().trim());
                builder.append("\" ");
            }
        }
        URI uri = request.getURI();
        if (request instanceof RequestWrapper) {
            HttpRequest original = ((RequestWrapper) request).getOriginal();
            if (original instanceof HttpUriRequest) {
                uri = ((HttpUriRequest) original).getURI();
            }
        }
        builder.append("\"");
        builder.append(uri);
        builder.append("\"");
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            if (entity != null && entity.isRepeatable()) {
                if (entity.getContentLength() < 1024) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    entity.writeTo(stream);
                    if (isBinaryContent(request)) {
                        builder.insert(0, "echo '" + Base64.encodeToString(stream.toByteArray(), 2) + "' | base64 -d > /tmp/$$.bin; ");
                        builder.append(" --data-binary @/tmp/$$.bin");
                    } else {
                        builder.append(" --data-ascii \"").append(stream.toString()).append("\"");
                    }
                } else {
                    builder.append(" [TOO MUCH DATA TO INCLUDE]");
                }
            }
        }
        return builder.toString();
    }

    private static boolean isBinaryContent(HttpUriRequest request) {
        Header[] headers = request.getHeaders(Headers.CONTENT_ENCODING);
        if (headers != null) {
            for (Header header : headers) {
                if ("gzip".equalsIgnoreCase(header.getValue())) {
                    return true;
                }
            }
        }
        headers = request.getHeaders(Headers.CONTENT_TYPE);
        if (headers != null) {
            for (Header header2 : headers) {
                for (String contentType : textContentTypes) {
                    if (header2.getValue().startsWith(contentType)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static long parseDate(String dateString) {
        return LegacyHttpDateTime.parse(dateString);
    }
}
