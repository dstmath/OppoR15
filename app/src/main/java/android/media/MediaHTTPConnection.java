package android.media;

import android.media.IMediaHTTPConnection.Stub;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.os.IBinder;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy.Builder;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.Map;

public class MediaHTTPConnection extends Stub {
    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int HTTP_TEMP_REDIRECT = 307;
    private static final int MAX_REDIRECTS = 20;
    private static final int MEDIA_ERROR_HTTP_PROTOCOL_ERROR = -214741;
    private static final String TAG = "MediaHTTPConnection";
    private static final boolean VERBOSE = false;
    private boolean mAllowCrossDomainRedirect = true;
    private boolean mAllowCrossProtocolRedirect = true;
    private HttpURLConnection mConnection = null;
    private long mCurrentOffset = -1;
    private Map<String, String> mHeaders = null;
    private InputStream mInputStream = null;
    private long mNativeContext;
    private long mTotalSize = -1;
    private URL mURL = null;

    private final native void native_finalize();

    private final native IBinder native_getIMemory();

    private static final native void native_init();

    private final native int native_readAt(long j, int i);

    private final native void native_setup();

    public MediaHTTPConnection() {
        if (CookieHandler.getDefault() == null) {
            Log.w(TAG, "MediaHTTPConnection: Unexpected. No CookieHandler found.");
        }
        native_setup();
    }

    public IBinder connect(String uri, String headers) {
        try {
            disconnect();
            this.mAllowCrossDomainRedirect = true;
            this.mURL = new URL(uri);
            this.mHeaders = convertHeaderStringToMap(headers);
            return native_getIMemory();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private boolean parseBoolean(String val) {
        boolean z = true;
        try {
            if (Long.parseLong(val) == 0) {
                z = false;
            }
            return z;
        } catch (NumberFormatException e) {
            if (!"true".equalsIgnoreCase(val)) {
                z = "yes".equalsIgnoreCase(val);
            }
            return z;
        }
    }

    private boolean filterOutInternalHeaders(String key, String val) {
        if (!"android-allow-cross-domain-redirect".equalsIgnoreCase(key)) {
            return false;
        }
        this.mAllowCrossDomainRedirect = parseBoolean(val);
        this.mAllowCrossProtocolRedirect = this.mAllowCrossDomainRedirect;
        return true;
    }

    private Map<String, String> convertHeaderStringToMap(String headers) {
        HashMap<String, String> map = new HashMap();
        for (String pair : headers.split("\r\n")) {
            int colonPos = pair.indexOf(":");
            if (colonPos >= 0) {
                String key = pair.substring(0, colonPos);
                String val = pair.substring(colonPos + 1);
                if (!filterOutInternalHeaders(key, val)) {
                    map.put(key, val);
                }
            }
        }
        return map;
    }

    public void disconnect() {
        teardownConnection();
        this.mHeaders = null;
        this.mURL = null;
    }

    private void teardownConnection() {
        if (this.mConnection != null) {
            if (this.mInputStream != null) {
                try {
                    this.mInputStream.close();
                } catch (IOException e) {
                }
                this.mInputStream = null;
            }
            this.mConnection.disconnect();
            this.mConnection = null;
            this.mCurrentOffset = -1;
        }
    }

    private static final boolean isLocalHost(URL url) {
        if (url == null) {
            return false;
        }
        String host = url.getHost();
        if (host == null) {
            return false;
        }
        try {
            return host.equalsIgnoreCase(ProxyInfo.LOCAL_HOST) || NetworkUtils.numericToInetAddress(host).isLoopbackAddress();
        } catch (IllegalArgumentException e) {
        }
    }

    private void seekTo(long r24) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r18_2 'url' java.net.URL) in PHI: PHI: (r18_1 'url' java.net.URL) = (r18_0 'url' java.net.URL), (r18_2 'url' java.net.URL) binds: {(r18_0 'url' java.net.URL)=B:2:?, (r18_2 'url' java.net.URL)=B:94:0x000e}
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
        r23 = this;
        r23.teardownConnection();
        r13 = 0;
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mURL;	 Catch:{ IOException -> 0x0076 }
        r18 = r0;	 Catch:{ IOException -> 0x0076 }
        r12 = isLocalHost(r18);	 Catch:{ IOException -> 0x0076 }
    L_0x000e:
        if (r12 == 0) goto L_0x008b;	 Catch:{ IOException -> 0x0076 }
    L_0x0010:
        r19 = java.net.Proxy.NO_PROXY;	 Catch:{ IOException -> 0x0076 }
        r19 = r18.openConnection(r19);	 Catch:{ IOException -> 0x0076 }
        r19 = (java.net.HttpURLConnection) r19;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        r1 = r23;	 Catch:{ IOException -> 0x0076 }
        r1.mConnection = r0;	 Catch:{ IOException -> 0x0076 }
    L_0x001e:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r20 = 30000; // 0x7530 float:4.2039E-41 double:1.4822E-319;	 Catch:{ IOException -> 0x0076 }
        r19.setConnectTimeout(r20);	 Catch:{ IOException -> 0x0076 }
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mAllowCrossDomainRedirect;	 Catch:{ IOException -> 0x0076 }
        r20 = r0;	 Catch:{ IOException -> 0x0076 }
        r19.setInstanceFollowRedirects(r20);	 Catch:{ IOException -> 0x0076 }
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mHeaders;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        if (r19 == 0) goto L_0x0098;	 Catch:{ IOException -> 0x0076 }
    L_0x0040:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mHeaders;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r19 = r19.entrySet();	 Catch:{ IOException -> 0x0076 }
        r8 = r19.iterator();	 Catch:{ IOException -> 0x0076 }
    L_0x004e:
        r19 = r8.hasNext();	 Catch:{ IOException -> 0x0076 }
        if (r19 == 0) goto L_0x0098;	 Catch:{ IOException -> 0x0076 }
    L_0x0054:
        r7 = r8.next();	 Catch:{ IOException -> 0x0076 }
        r7 = (java.util.Map.Entry) r7;	 Catch:{ IOException -> 0x0076 }
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r21 = r0;	 Catch:{ IOException -> 0x0076 }
        r19 = r7.getKey();	 Catch:{ IOException -> 0x0076 }
        r19 = (java.lang.String) r19;	 Catch:{ IOException -> 0x0076 }
        r20 = r7.getValue();	 Catch:{ IOException -> 0x0076 }
        r20 = (java.lang.String) r20;	 Catch:{ IOException -> 0x0076 }
        r0 = r21;	 Catch:{ IOException -> 0x0076 }
        r1 = r19;	 Catch:{ IOException -> 0x0076 }
        r2 = r20;	 Catch:{ IOException -> 0x0076 }
        r0.setRequestProperty(r1, r2);	 Catch:{ IOException -> 0x0076 }
        goto L_0x004e;
    L_0x0076:
        r5 = move-exception;
        r20 = -1;
        r0 = r20;
        r2 = r23;
        r2.mTotalSize = r0;
        r23.teardownConnection();
        r20 = -1;
        r0 = r20;
        r2 = r23;
        r2.mCurrentOffset = r0;
        throw r5;
    L_0x008b:
        r19 = r18.openConnection();	 Catch:{ IOException -> 0x0076 }
        r19 = (java.net.HttpURLConnection) r19;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        r1 = r23;	 Catch:{ IOException -> 0x0076 }
        r1.mConnection = r0;	 Catch:{ IOException -> 0x0076 }
        goto L_0x001e;	 Catch:{ IOException -> 0x0076 }
    L_0x0098:
        r20 = 0;	 Catch:{ IOException -> 0x0076 }
        r19 = (r24 > r20 ? 1 : (r24 == r20 ? 0 : -1));	 Catch:{ IOException -> 0x0076 }
        if (r19 <= 0) goto L_0x00c9;	 Catch:{ IOException -> 0x0076 }
    L_0x009e:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r20 = "Range";	 Catch:{ IOException -> 0x0076 }
        r21 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0076 }
        r21.<init>();	 Catch:{ IOException -> 0x0076 }
        r22 = "bytes=";	 Catch:{ IOException -> 0x0076 }
        r21 = r21.append(r22);	 Catch:{ IOException -> 0x0076 }
        r0 = r21;	 Catch:{ IOException -> 0x0076 }
        r1 = r24;	 Catch:{ IOException -> 0x0076 }
        r21 = r0.append(r1);	 Catch:{ IOException -> 0x0076 }
        r22 = "-";	 Catch:{ IOException -> 0x0076 }
        r21 = r21.append(r22);	 Catch:{ IOException -> 0x0076 }
        r21 = r21.toString();	 Catch:{ IOException -> 0x0076 }
        r19.setRequestProperty(r20, r21);	 Catch:{ IOException -> 0x0076 }
    L_0x00c9:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r14 = r19.getResponseCode();	 Catch:{ IOException -> 0x0076 }
        r19 = 300; // 0x12c float:4.2E-43 double:1.48E-321;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r14 == r0) goto L_0x0154;	 Catch:{ IOException -> 0x0076 }
    L_0x00d9:
        r19 = 301; // 0x12d float:4.22E-43 double:1.487E-321;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r14 == r0) goto L_0x0154;	 Catch:{ IOException -> 0x0076 }
    L_0x00df:
        r19 = 302; // 0x12e float:4.23E-43 double:1.49E-321;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r14 == r0) goto L_0x0154;	 Catch:{ IOException -> 0x0076 }
    L_0x00e5:
        r19 = 303; // 0x12f float:4.25E-43 double:1.497E-321;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r14 == r0) goto L_0x0154;	 Catch:{ IOException -> 0x0076 }
    L_0x00eb:
        r19 = 307; // 0x133 float:4.3E-43 double:1.517E-321;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r14 == r0) goto L_0x0154;	 Catch:{ IOException -> 0x0076 }
    L_0x00f1:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mAllowCrossDomainRedirect;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        if (r19 == 0) goto L_0x0109;	 Catch:{ IOException -> 0x0076 }
    L_0x00f9:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r19 = r19.getURL();	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        r1 = r23;	 Catch:{ IOException -> 0x0076 }
        r1.mURL = r0;	 Catch:{ IOException -> 0x0076 }
    L_0x0109:
        r19 = 206; // 0xce float:2.89E-43 double:1.02E-321;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r14 != r0) goto L_0x0253;	 Catch:{ IOException -> 0x0076 }
    L_0x010f:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r20 = "Content-Range";	 Catch:{ IOException -> 0x0076 }
        r4 = r19.getHeaderField(r20);	 Catch:{ IOException -> 0x0076 }
        r20 = -1;	 Catch:{ IOException -> 0x0076 }
        r0 = r20;	 Catch:{ IOException -> 0x0076 }
        r2 = r23;	 Catch:{ IOException -> 0x0076 }
        r2.mTotalSize = r0;	 Catch:{ IOException -> 0x0076 }
        if (r4 == 0) goto L_0x0142;	 Catch:{ IOException -> 0x0076 }
    L_0x0126:
        r19 = 47;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        r9 = r4.lastIndexOf(r0);	 Catch:{ IOException -> 0x0076 }
        if (r9 < 0) goto L_0x0142;	 Catch:{ IOException -> 0x0076 }
    L_0x0130:
        r19 = r9 + 1;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        r17 = r4.substring(r0);	 Catch:{ IOException -> 0x0076 }
        r20 = java.lang.Long.parseLong(r17);	 Catch:{ NumberFormatException -> 0x0292 }
        r0 = r20;	 Catch:{ NumberFormatException -> 0x0292 }
        r2 = r23;	 Catch:{ NumberFormatException -> 0x0292 }
        r2.mTotalSize = r0;	 Catch:{ NumberFormatException -> 0x0292 }
    L_0x0142:
        r20 = 0;
        r19 = (r24 > r20 ? 1 : (r24 == r20 ? 0 : -1));
        if (r19 <= 0) goto L_0x0276;
    L_0x0148:
        r19 = 206; // 0xce float:2.89E-43 double:1.02E-321;
        r0 = r19;
        if (r14 == r0) goto L_0x0276;
    L_0x014e:
        r19 = new java.net.ProtocolException;	 Catch:{ IOException -> 0x0076 }
        r19.<init>();	 Catch:{ IOException -> 0x0076 }
        throw r19;	 Catch:{ IOException -> 0x0076 }
    L_0x0154:
        r13 = r13 + 1;	 Catch:{ IOException -> 0x0076 }
        r19 = 20;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r13 <= r0) goto L_0x0178;	 Catch:{ IOException -> 0x0076 }
    L_0x015c:
        r19 = new java.net.NoRouteToHostException;	 Catch:{ IOException -> 0x0076 }
        r20 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0076 }
        r20.<init>();	 Catch:{ IOException -> 0x0076 }
        r21 = "Too many redirects: ";	 Catch:{ IOException -> 0x0076 }
        r20 = r20.append(r21);	 Catch:{ IOException -> 0x0076 }
        r0 = r20;	 Catch:{ IOException -> 0x0076 }
        r20 = r0.append(r13);	 Catch:{ IOException -> 0x0076 }
        r20 = r20.toString();	 Catch:{ IOException -> 0x0076 }
        r19.<init>(r20);	 Catch:{ IOException -> 0x0076 }
        throw r19;	 Catch:{ IOException -> 0x0076 }
    L_0x0178:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r11 = r19.getRequestMethod();	 Catch:{ IOException -> 0x0076 }
        r19 = 307; // 0x133 float:4.3E-43 double:1.517E-321;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r14 != r0) goto L_0x01ab;	 Catch:{ IOException -> 0x0076 }
    L_0x0188:
        r19 = "GET";	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        r19 = r11.equals(r0);	 Catch:{ IOException -> 0x0076 }
        r19 = r19 ^ 1;	 Catch:{ IOException -> 0x0076 }
        if (r19 == 0) goto L_0x01ab;	 Catch:{ IOException -> 0x0076 }
    L_0x0195:
        r19 = "HEAD";	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        r19 = r11.equals(r0);	 Catch:{ IOException -> 0x0076 }
        r19 = r19 ^ 1;	 Catch:{ IOException -> 0x0076 }
        if (r19 == 0) goto L_0x01ab;	 Catch:{ IOException -> 0x0076 }
    L_0x01a2:
        r19 = new java.net.NoRouteToHostException;	 Catch:{ IOException -> 0x0076 }
        r20 = "Invalid redirect";	 Catch:{ IOException -> 0x0076 }
        r19.<init>(r20);	 Catch:{ IOException -> 0x0076 }
        throw r19;	 Catch:{ IOException -> 0x0076 }
    L_0x01ab:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r20 = "Location";	 Catch:{ IOException -> 0x0076 }
        r10 = r19.getHeaderField(r20);	 Catch:{ IOException -> 0x0076 }
        if (r10 != 0) goto L_0x01c3;	 Catch:{ IOException -> 0x0076 }
    L_0x01ba:
        r19 = new java.net.NoRouteToHostException;	 Catch:{ IOException -> 0x0076 }
        r20 = "Invalid redirect";	 Catch:{ IOException -> 0x0076 }
        r19.<init>(r20);	 Catch:{ IOException -> 0x0076 }
        throw r19;	 Catch:{ IOException -> 0x0076 }
    L_0x01c3:
        r18 = new java.net.URL;	 Catch:{ IOException -> 0x0076 }
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mURL;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r0 = r18;	 Catch:{ IOException -> 0x0076 }
        r1 = r19;	 Catch:{ IOException -> 0x0076 }
        r0.<init>(r1, r10);	 Catch:{ IOException -> 0x0076 }
        r19 = r18.getProtocol();	 Catch:{ IOException -> 0x0076 }
        r20 = "https";	 Catch:{ IOException -> 0x0076 }
        r19 = r19.equals(r20);	 Catch:{ IOException -> 0x0076 }
        if (r19 != 0) goto L_0x01f7;	 Catch:{ IOException -> 0x0076 }
    L_0x01df:
        r19 = r18.getProtocol();	 Catch:{ IOException -> 0x0076 }
        r20 = "http";	 Catch:{ IOException -> 0x0076 }
        r19 = r19.equals(r20);	 Catch:{ IOException -> 0x0076 }
        r19 = r19 ^ 1;	 Catch:{ IOException -> 0x0076 }
        if (r19 == 0) goto L_0x01f7;	 Catch:{ IOException -> 0x0076 }
    L_0x01ee:
        r19 = new java.net.NoRouteToHostException;	 Catch:{ IOException -> 0x0076 }
        r20 = "Unsupported protocol redirect";	 Catch:{ IOException -> 0x0076 }
        r19.<init>(r20);	 Catch:{ IOException -> 0x0076 }
        throw r19;	 Catch:{ IOException -> 0x0076 }
    L_0x01f7:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mURL;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r19 = r19.getProtocol();	 Catch:{ IOException -> 0x0076 }
        r20 = r18.getProtocol();	 Catch:{ IOException -> 0x0076 }
        r16 = r19.equals(r20);	 Catch:{ IOException -> 0x0076 }
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mAllowCrossProtocolRedirect;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        if (r19 != 0) goto L_0x021e;	 Catch:{ IOException -> 0x0076 }
    L_0x0211:
        r19 = r16 ^ 1;	 Catch:{ IOException -> 0x0076 }
        if (r19 == 0) goto L_0x021e;	 Catch:{ IOException -> 0x0076 }
    L_0x0215:
        r19 = new java.net.NoRouteToHostException;	 Catch:{ IOException -> 0x0076 }
        r20 = "Cross-protocol redirects are disallowed";	 Catch:{ IOException -> 0x0076 }
        r19.<init>(r20);	 Catch:{ IOException -> 0x0076 }
        throw r19;	 Catch:{ IOException -> 0x0076 }
    L_0x021e:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mURL;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r19 = r19.getHost();	 Catch:{ IOException -> 0x0076 }
        r20 = r18.getHost();	 Catch:{ IOException -> 0x0076 }
        r15 = r19.equals(r20);	 Catch:{ IOException -> 0x0076 }
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mAllowCrossDomainRedirect;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        if (r19 != 0) goto L_0x0245;	 Catch:{ IOException -> 0x0076 }
    L_0x0238:
        r19 = r15 ^ 1;	 Catch:{ IOException -> 0x0076 }
        if (r19 == 0) goto L_0x0245;	 Catch:{ IOException -> 0x0076 }
    L_0x023c:
        r19 = new java.net.NoRouteToHostException;	 Catch:{ IOException -> 0x0076 }
        r20 = "Cross-domain redirects are disallowed";	 Catch:{ IOException -> 0x0076 }
        r19.<init>(r20);	 Catch:{ IOException -> 0x0076 }
        throw r19;	 Catch:{ IOException -> 0x0076 }
    L_0x0245:
        r19 = 307; // 0x133 float:4.3E-43 double:1.517E-321;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r14 == r0) goto L_0x000e;	 Catch:{ IOException -> 0x0076 }
    L_0x024b:
        r0 = r18;	 Catch:{ IOException -> 0x0076 }
        r1 = r23;	 Catch:{ IOException -> 0x0076 }
        r1.mURL = r0;	 Catch:{ IOException -> 0x0076 }
        goto L_0x000e;	 Catch:{ IOException -> 0x0076 }
    L_0x0253:
        r19 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        if (r14 == r0) goto L_0x025f;	 Catch:{ IOException -> 0x0076 }
    L_0x0259:
        r19 = new java.io.IOException;	 Catch:{ IOException -> 0x0076 }
        r19.<init>();	 Catch:{ IOException -> 0x0076 }
        throw r19;	 Catch:{ IOException -> 0x0076 }
    L_0x025f:
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r19 = r0;	 Catch:{ IOException -> 0x0076 }
        r19 = r19.getContentLength();	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        r0 = (long) r0;	 Catch:{ IOException -> 0x0076 }
        r20 = r0;	 Catch:{ IOException -> 0x0076 }
        r0 = r20;	 Catch:{ IOException -> 0x0076 }
        r2 = r23;	 Catch:{ IOException -> 0x0076 }
        r2.mTotalSize = r0;	 Catch:{ IOException -> 0x0076 }
        goto L_0x0142;	 Catch:{ IOException -> 0x0076 }
    L_0x0276:
        r19 = new java.io.BufferedInputStream;	 Catch:{ IOException -> 0x0076 }
        r0 = r23;	 Catch:{ IOException -> 0x0076 }
        r0 = r0.mConnection;	 Catch:{ IOException -> 0x0076 }
        r20 = r0;	 Catch:{ IOException -> 0x0076 }
        r20 = r20.getInputStream();	 Catch:{ IOException -> 0x0076 }
        r19.<init>(r20);	 Catch:{ IOException -> 0x0076 }
        r0 = r19;	 Catch:{ IOException -> 0x0076 }
        r1 = r23;	 Catch:{ IOException -> 0x0076 }
        r1.mInputStream = r0;	 Catch:{ IOException -> 0x0076 }
        r0 = r24;	 Catch:{ IOException -> 0x0076 }
        r2 = r23;	 Catch:{ IOException -> 0x0076 }
        r2.mCurrentOffset = r0;	 Catch:{ IOException -> 0x0076 }
        return;
    L_0x0292:
        r6 = move-exception;
        goto L_0x0142;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.media.MediaHTTPConnection.seekTo(long):void");
    }

    public int readAt(long offset, int size) {
        return native_readAt(offset, size);
    }

    private int readAt(long offset, byte[] data, int size) {
        int ret = readAt_internal(offset, data, size, false);
        if (ret == MEDIA_ERROR_HTTP_PROTOCOL_ERROR) {
            Log.w(TAG, "readAt " + offset + " / " + size + " => " + "protocol error, retry");
            ret = readAt_internal(offset, data, size, true);
        }
        if (ret != MEDIA_ERROR_HTTP_PROTOCOL_ERROR) {
            return ret;
        }
        Log.w(TAG, "readAt " + offset + " / " + size + " => " + "error, convert error");
        return MediaPlayer.MEDIA_ERROR_UNSUPPORTED;
    }

    private int readAt_internal(long offset, byte[] data, int size, boolean forceSeek) {
        StrictMode.setThreadPolicy(new Builder().permitAll().build());
        try {
            if (offset != this.mCurrentOffset || forceSeek) {
                seekTo(offset);
            }
            int n = this.mInputStream.read(data, 0, size);
            if (n == -1) {
                n = 0;
            }
            this.mCurrentOffset += (long) n;
            return n;
        } catch (ProtocolException e) {
            Log.w(TAG, "readAt " + offset + " / " + size + " => " + e);
            return MEDIA_ERROR_HTTP_PROTOCOL_ERROR;
        } catch (NoRouteToHostException e2) {
            Log.w(TAG, "readAt " + offset + " / " + size + " => " + e2);
            return MediaPlayer.MEDIA_ERROR_UNSUPPORTED;
        } catch (UnknownServiceException e3) {
            Log.w(TAG, "readAt " + offset + " / " + size + " => " + e3);
            return MediaPlayer.MEDIA_ERROR_UNSUPPORTED;
        } catch (IOException e4) {
            return -1;
        } catch (Exception e5) {
            return -1;
        }
    }

    public long getSize() {
        if (this.mConnection == null) {
            try {
                seekTo(0);
            } catch (IOException e) {
                return -1;
            }
        }
        return this.mTotalSize;
    }

    public String getMIMEType() {
        if (this.mConnection == null) {
            try {
                seekTo(0);
            } catch (IOException e) {
                return "application/octet-stream";
            }
        }
        return this.mConnection.getContentType();
    }

    public String getUri() {
        return this.mURL.toString();
    }

    protected void finalize() {
        native_finalize();
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }
}
