package android.net;

import android.content.Context;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OppoHttpClient {
    private static final long AVERAGE_RECEIVE_TIME = 832;
    private static final boolean DEBUG = true;
    private static final long GMT_BEIJING_OFFSET = 28800000;
    private static final String TAG = "OppoHttpClient";
    private static final long VALID_LAST_TIME_THRESHOLD = 1500;
    private static long mLastGotSuccessLocaltime = 0;
    private static final String oppoServerURL_RANDOM = "http://newds01.myoppo.com/autotime/dateandtime.xml?number=";
    private static final String oppoServerURL_RANDOM2 = "http://newds02.myoppo.com/autotime/dateandtime.xml?number=";
    private long mHttpTime;
    private long mHttpTimeReference;
    private long mRoundTripTime;

    public class DateTimeXmlParseHandler extends DefaultHandler {
        private String mDateString = "";
        private boolean mIsDateFlag = false;
        private boolean mIsTimeFlag = false;
        private boolean mIsTimeZoneFlag = false;
        private String mTimeString = "";
        private String mTimeZoneString = "";

        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            if (this.mIsTimeZoneFlag) {
                this.mTimeZoneString = new String(ch, start, length);
            } else if (this.mIsDateFlag) {
                this.mDateString = new String(ch, start, length);
            } else if (this.mIsTimeFlag) {
                this.mTimeString = new String(ch, start, length);
            }
        }

        public void endDocument() throws SAXException {
            super.endDocument();
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            if (localName.equals("TimeZone")) {
                this.mIsTimeZoneFlag = false;
            } else if (localName.equals("Date")) {
                this.mIsDateFlag = false;
            } else if (localName.equals("Time")) {
                this.mIsTimeFlag = false;
            }
        }

        public void startDocument() throws SAXException {
            super.startDocument();
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (localName.equals("TimeZone")) {
                this.mIsTimeZoneFlag = true;
            } else if (localName.equals("Date")) {
                this.mIsDateFlag = true;
            } else if (localName.equals("Time")) {
                this.mIsTimeFlag = true;
            }
        }

        public String getTimeZone() {
            return this.mTimeZoneString;
        }

        public String getDate() {
            return this.mDateString;
        }

        public String getTime() {
            return this.mTimeString;
        }
    }

    public boolean requestTime(Context context, int selServerUrl, int timeout) {
        return forceRefreshTimeFromOppoServer(context, selServerUrl, timeout);
    }

    private boolean forceRefreshTimeFromOppoServer(android.content.Context r51, int r52, int r53) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r21_1 'mInputStreamReader' java.io.InputStreamReader) in PHI: PHI: (r21_2 'mInputStreamReader' java.io.InputStreamReader) = (r21_0 'mInputStreamReader' java.io.InputStreamReader), (r21_1 'mInputStreamReader' java.io.InputStreamReader) binds: {(r21_0 'mInputStreamReader' java.io.InputStreamReader)=B:14:0x0120, (r21_1 'mInputStreamReader' java.io.InputStreamReader)=B:20:0x016e}
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
        r50 = this;
        r41 = 0;
        r45 = 0;
        r3 = "OppoHttpClient";
        r4 = "Enter forceRefreshTimeFromOppoServer run";
        android.util.Log.d(r3, r4);
        r34 = "http://newds01.myoppo.com/autotime/dateandtime.xml?number=";	 Catch:{ Exception -> 0x031b }
        if (r52 <= 0) goto L_0x0015;	 Catch:{ Exception -> 0x031b }
    L_0x0012:
        r34 = "http://newds02.myoppo.com/autotime/dateandtime.xml?number=";	 Catch:{ Exception -> 0x031b }
    L_0x0015:
        r3 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x031b }
        r3.<init>();	 Catch:{ Exception -> 0x031b }
        r0 = r34;	 Catch:{ Exception -> 0x031b }
        r3 = r3.append(r0);	 Catch:{ Exception -> 0x031b }
        r4 = java.lang.System.currentTimeMillis();	 Catch:{ Exception -> 0x031b }
        r3 = r3.append(r4);	 Catch:{ Exception -> 0x031b }
        r34 = r3.toString();	 Catch:{ Exception -> 0x031b }
        r46 = new java.net.URL;	 Catch:{ Exception -> 0x031b }
        r0 = r46;	 Catch:{ Exception -> 0x031b }
        r1 = r34;	 Catch:{ Exception -> 0x031b }
        r0.<init>(r1);	 Catch:{ Exception -> 0x031b }
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x02fb }
        r4.<init>();	 Catch:{ Exception -> 0x02fb }
        r5 = "Cur http request:";	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r0 = r34;	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r0);	 Catch:{ Exception -> 0x02fb }
        r4 = r4.toString();	 Catch:{ Exception -> 0x02fb }
        android.util.Log.i(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r12 = 0;	 Catch:{ Exception -> 0x02fb }
        r36 = android.net.Proxy.getDefaultHost();	 Catch:{ Exception -> 0x02fb }
        r37 = android.net.Proxy.getDefaultPort();	 Catch:{ Exception -> 0x02fb }
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x02fb }
        r4.<init>();	 Catch:{ Exception -> 0x02fb }
        r5 = "OppoServer proxyHost = ";	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r0 = r36;	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r0);	 Catch:{ Exception -> 0x02fb }
        r5 = " proxyPort = ";	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r0 = r37;	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r0);	 Catch:{ Exception -> 0x02fb }
        r4 = r4.toString();	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r3 = r50.getNetType(r51);	 Catch:{ Exception -> 0x02fb }
        if (r3 == 0) goto L_0x0147;	 Catch:{ Exception -> 0x02fb }
    L_0x0089:
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = "Get network type success!";	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r12 = r46.openConnection();	 Catch:{ Exception -> 0x02fb }
        r12 = (java.net.HttpURLConnection) r12;	 Catch:{ Exception -> 0x02fb }
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = "HttpURLConnection open openConnection success!";	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
    L_0x00a1:
        r3 = 1;	 Catch:{ Exception -> 0x02fb }
        r12.setDoInput(r3);	 Catch:{ Exception -> 0x02fb }
        r3 = 0;	 Catch:{ Exception -> 0x02fb }
        r12.setUseCaches(r3);	 Catch:{ Exception -> 0x02fb }
        if (r52 <= 0) goto L_0x00ad;	 Catch:{ Exception -> 0x02fb }
    L_0x00ab:
        r53 = r53 * 3;	 Catch:{ Exception -> 0x02fb }
    L_0x00ad:
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x02fb }
        r4.<init>();	 Catch:{ Exception -> 0x02fb }
        r5 = "timeout:";	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r0 = r53;	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r0);	 Catch:{ Exception -> 0x02fb }
        r4 = r4.toString();	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r0 = r53;	 Catch:{ Exception -> 0x02fb }
        r12.setConnectTimeout(r0);	 Catch:{ Exception -> 0x02fb }
        r0 = r53;	 Catch:{ Exception -> 0x02fb }
        r12.setReadTimeout(r0);	 Catch:{ Exception -> 0x02fb }
        r38 = android.os.SystemClock.elapsedRealtime();	 Catch:{ Exception -> 0x02fb }
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = "Strart to connect http server!";	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r12.connect();	 Catch:{ Exception -> 0x02fb }
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = "Connect http server success!";	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r21 = 0;	 Catch:{ Exception -> 0x02fb }
        r15 = 0;	 Catch:{ Exception -> 0x02fb }
        r20 = "";	 Catch:{ Exception -> 0x02fb }
        r16 = 0;	 Catch:{ Exception -> 0x02fb }
        r42 = 0;	 Catch:{ Exception -> 0x02fb }
        r4 = 0;	 Catch:{ Exception -> 0x02fb }
        r0 = r50;	 Catch:{ Exception -> 0x02fb }
        r0.mHttpTimeReference = r4;	 Catch:{ Exception -> 0x02fb }
        r40 = r12.getResponseCode();	 Catch:{ Exception -> 0x02fb }
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x02fb }
        r4.<init>();	 Catch:{ Exception -> 0x02fb }
        r5 = "Http responseCode:";	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r0 = r40;	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r0);	 Catch:{ Exception -> 0x02fb }
        r4 = r4.toString();	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r3 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;	 Catch:{ Exception -> 0x02fb }
        r0 = r40;	 Catch:{ Exception -> 0x02fb }
        if (r0 != r3) goto L_0x0177;	 Catch:{ Exception -> 0x02fb }
    L_0x0122:
        r16 = java.lang.System.currentTimeMillis();	 Catch:{ Exception -> 0x02fb }
        r21 = new java.io.InputStreamReader;	 Catch:{ Exception -> 0x02fb }
        r3 = r12.getInputStream();	 Catch:{ Exception -> 0x02fb }
        r4 = "utf-8";	 Catch:{ Exception -> 0x02fb }
        r0 = r21;	 Catch:{ Exception -> 0x02fb }
        r0.<init>(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r15 = new java.io.BufferedReader;	 Catch:{ Exception -> 0x02fb }
        r0 = r21;	 Catch:{ Exception -> 0x02fb }
        r15.<init>(r0);	 Catch:{ Exception -> 0x02fb }
        r14 = "";	 Catch:{ Exception -> 0x02fb }
    L_0x013e:
        r14 = r15.readLine();	 Catch:{ Exception -> 0x02fb }
        if (r14 == 0) goto L_0x016e;	 Catch:{ Exception -> 0x02fb }
    L_0x0144:
        r20 = r14;	 Catch:{ Exception -> 0x02fb }
        goto L_0x013e;	 Catch:{ Exception -> 0x02fb }
    L_0x0147:
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = "Use http proxy!";	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r35 = new java.net.Proxy;	 Catch:{ Exception -> 0x02fb }
        r3 = java.net.Proxy.Type.HTTP;	 Catch:{ Exception -> 0x02fb }
        r4 = new java.net.InetSocketAddress;	 Catch:{ Exception -> 0x02fb }
        r0 = r36;	 Catch:{ Exception -> 0x02fb }
        r1 = r37;	 Catch:{ Exception -> 0x02fb }
        r4.<init>(r0, r1);	 Catch:{ Exception -> 0x02fb }
        r0 = r35;	 Catch:{ Exception -> 0x02fb }
        r0.<init>(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r0 = r46;	 Catch:{ Exception -> 0x02fb }
        r1 = r35;	 Catch:{ Exception -> 0x02fb }
        r12 = r0.openConnection(r1);	 Catch:{ Exception -> 0x02fb }
        r12 = (java.net.HttpURLConnection) r12;	 Catch:{ Exception -> 0x02fb }
        goto L_0x00a1;	 Catch:{ Exception -> 0x02fb }
    L_0x016e:
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = "Read response data success!";	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
    L_0x0177:
        r42 = android.os.SystemClock.elapsedRealtime();	 Catch:{ Exception -> 0x02fb }
        r4 = android.os.SystemClock.elapsedRealtime();	 Catch:{ Exception -> 0x02fb }
        r0 = r50;	 Catch:{ Exception -> 0x02fb }
        r0.mHttpTimeReference = r4;	 Catch:{ Exception -> 0x02fb }
        r15.close();	 Catch:{ Exception -> 0x02fb }
        r21.close();	 Catch:{ Exception -> 0x02fb }
        r12.disconnect();	 Catch:{ Exception -> 0x02fb }
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = "Start to parser http response data!";	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r31 = javax.xml.parsers.SAXParserFactory.newInstance();	 Catch:{ Exception -> 0x02fb }
        r30 = r31.newSAXParser();	 Catch:{ Exception -> 0x02fb }
        r33 = r30.getXMLReader();	 Catch:{ Exception -> 0x02fb }
        r19 = new android.net.OppoHttpClient$DateTimeXmlParseHandler;	 Catch:{ Exception -> 0x02fb }
        r0 = r19;	 Catch:{ Exception -> 0x02fb }
        r1 = r50;	 Catch:{ Exception -> 0x02fb }
        r0.<init>();	 Catch:{ Exception -> 0x02fb }
        r0 = r33;	 Catch:{ Exception -> 0x02fb }
        r1 = r19;	 Catch:{ Exception -> 0x02fb }
        r0.setContentHandler(r1);	 Catch:{ Exception -> 0x02fb }
        r3 = new org.xml.sax.InputSource;	 Catch:{ Exception -> 0x02fb }
        r4 = new java.io.StringReader;	 Catch:{ Exception -> 0x02fb }
        r0 = r20;	 Catch:{ Exception -> 0x02fb }
        r4.<init>(r0);	 Catch:{ Exception -> 0x02fb }
        r3.<init>(r4);	 Catch:{ Exception -> 0x02fb }
        r0 = r33;	 Catch:{ Exception -> 0x02fb }
        r0.parse(r3);	 Catch:{ Exception -> 0x02fb }
        r18 = r19.getDate();	 Catch:{ Exception -> 0x02fb }
        r3 = "-";	 Catch:{ Exception -> 0x02fb }
        r0 = r18;	 Catch:{ Exception -> 0x02fb }
        r9 = r0.split(r3);	 Catch:{ Exception -> 0x02fb }
        r3 = 3;	 Catch:{ Exception -> 0x02fb }
        r0 = new int[r3];	 Catch:{ Exception -> 0x02fb }
        r26 = r0;	 Catch:{ Exception -> 0x02fb }
        r13 = 0;	 Catch:{ Exception -> 0x02fb }
    L_0x01d5:
        r3 = r9.length;	 Catch:{ Exception -> 0x02fb }
        if (r13 >= r3) goto L_0x01e3;	 Catch:{ Exception -> 0x02fb }
    L_0x01d8:
        r3 = r9[r13];	 Catch:{ Exception -> 0x02fb }
        r3 = java.lang.Integer.parseInt(r3);	 Catch:{ Exception -> 0x02fb }
        r26[r13] = r3;	 Catch:{ Exception -> 0x02fb }
        r13 = r13 + 1;	 Catch:{ Exception -> 0x02fb }
        goto L_0x01d5;	 Catch:{ Exception -> 0x02fb }
    L_0x01e3:
        r32 = r19.getTime();	 Catch:{ Exception -> 0x02fb }
        r3 = ":";	 Catch:{ Exception -> 0x02fb }
        r0 = r32;	 Catch:{ Exception -> 0x02fb }
        r44 = r0.split(r3);	 Catch:{ Exception -> 0x02fb }
        r3 = 3;	 Catch:{ Exception -> 0x02fb }
        r0 = new int[r3];	 Catch:{ Exception -> 0x02fb }
        r27 = r0;	 Catch:{ Exception -> 0x02fb }
        r13 = 0;	 Catch:{ Exception -> 0x02fb }
    L_0x01f6:
        r0 = r44;	 Catch:{ Exception -> 0x02fb }
        r3 = r0.length;	 Catch:{ Exception -> 0x02fb }
        if (r13 >= r3) goto L_0x0206;	 Catch:{ Exception -> 0x02fb }
    L_0x01fb:
        r3 = r44[r13];	 Catch:{ Exception -> 0x02fb }
        r3 = java.lang.Integer.parseInt(r3);	 Catch:{ Exception -> 0x02fb }
        r27[r13] = r3;	 Catch:{ Exception -> 0x02fb }
        r13 = r13 + 1;	 Catch:{ Exception -> 0x02fb }
        goto L_0x01f6;	 Catch:{ Exception -> 0x02fb }
    L_0x0206:
        r2 = new android.text.format.Time;	 Catch:{ Exception -> 0x02fb }
        r2.<init>();	 Catch:{ Exception -> 0x02fb }
        r3 = "OppoHttpClient";	 Catch:{ Exception -> 0x02fb }
        r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x02fb }
        r4.<init>();	 Catch:{ Exception -> 0x02fb }
        r5 = "Parser time success, hour= ";	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r5 = 0;	 Catch:{ Exception -> 0x02fb }
        r5 = r27[r5];	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r5 = " minute = ";	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r5 = 1;	 Catch:{ Exception -> 0x02fb }
        r5 = r27[r5];	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r5 = "seconds =";	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r5 = 2;	 Catch:{ Exception -> 0x02fb }
        r5 = r27[r5];	 Catch:{ Exception -> 0x02fb }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x02fb }
        r4 = r4.toString();	 Catch:{ Exception -> 0x02fb }
        android.util.Log.d(r3, r4);	 Catch:{ Exception -> 0x02fb }
        r3 = 2;	 Catch:{ Exception -> 0x02fb }
        r3 = r27[r3];	 Catch:{ Exception -> 0x02fb }
        r4 = 1;	 Catch:{ Exception -> 0x02fb }
        r4 = r27[r4];	 Catch:{ Exception -> 0x02fb }
        r5 = 0;	 Catch:{ Exception -> 0x02fb }
        r5 = r27[r5];	 Catch:{ Exception -> 0x02fb }
        r6 = 2;	 Catch:{ Exception -> 0x02fb }
        r6 = r26[r6];	 Catch:{ Exception -> 0x02fb }
        r7 = 1;	 Catch:{ Exception -> 0x02fb }
        r7 = r26[r7];	 Catch:{ Exception -> 0x02fb }
        r7 = r7 + -1;	 Catch:{ Exception -> 0x02fb }
        r8 = 0;	 Catch:{ Exception -> 0x02fb }
        r8 = r26[r8];	 Catch:{ Exception -> 0x02fb }
        r2.set(r3, r4, r5, r6, r7, r8);	 Catch:{ Exception -> 0x02fb }
        r3 = 1;	 Catch:{ Exception -> 0x02fb }
        r4 = r2.toMillis(r3);	 Catch:{ Exception -> 0x02fb }
        r6 = 28800000; // 0x1b77400 float:6.7390035E-38 double:1.42290906E-316;	 Catch:{ Exception -> 0x02fb }
        r24 = r4 - r6;	 Catch:{ Exception -> 0x02fb }
        r22 = java.lang.System.currentTimeMillis();	 Catch:{ Exception -> 0x02fb }
        r3 = java.util.TimeZone.getDefault();	 Catch:{ Exception -> 0x02fb }
        r3 = r3.getRawOffset();	 Catch:{ Exception -> 0x02fb }
        r4 = (long) r3;	 Catch:{ Exception -> 0x02fb }
        r4 = r4 + r24;	 Catch:{ Exception -> 0x02fb }
        r6 = r22 - r16;	 Catch:{ Exception -> 0x02fb }
        r4 = r4 + r6;	 Catch:{ Exception -> 0x02fb }
        r6 = 832; // 0x340 float:1.166E-42 double:4.11E-321;	 Catch:{ Exception -> 0x02fb }
        r28 = r4 + r6;	 Catch:{ Exception -> 0x02fb }
        r3 = java.util.TimeZone.getDefault();	 Catch:{ Exception -> 0x02fb }
        r0 = r28;	 Catch:{ Exception -> 0x02fb }
        r3 = r3.getOffset(r0);	 Catch:{ Exception -> 0x02fb }
        r4 = java.util.TimeZone.getDefault();	 Catch:{ Exception -> 0x02fb }
        r4 = r4.getRawOffset();	 Catch:{ Exception -> 0x02fb }
        r10 = r3 - r4;	 Catch:{ Exception -> 0x02fb }
        r4 = (long) r10;	 Catch:{ Exception -> 0x02fb }
        r4 = r4 + r28;	 Catch:{ Exception -> 0x02fb }
        r0 = r50;	 Catch:{ Exception -> 0x02fb }
        r0.mHttpTime = r4;	 Catch:{ Exception -> 0x02fb }
        r4 = r42 - r38;	 Catch:{ Exception -> 0x02fb }
        r0 = r50;	 Catch:{ Exception -> 0x02fb }
        r0.mRoundTripTime = r4;	 Catch:{ Exception -> 0x02fb }
        r41 = 1;	 Catch:{ Exception -> 0x02fb }
        r4 = android.net.OppoHttpClient.class;	 Catch:{ Exception -> 0x02fb }
        monitor-enter(r4);	 Catch:{ Exception -> 0x02fb }
        r6 = android.os.SystemClock.elapsedRealtime();	 Catch:{ all -> 0x02f8 }
        r48 = mLastGotSuccessLocaltime;	 Catch:{ all -> 0x02f8 }
        r6 = r6 - r48;	 Catch:{ all -> 0x02f8 }
        r48 = 1500; // 0x5dc float:2.102E-42 double:7.41E-321;	 Catch:{ all -> 0x02f8 }
        r3 = (r6 > r48 ? 1 : (r6 == r48 ? 0 : -1));	 Catch:{ all -> 0x02f8 }
        if (r3 <= 0) goto L_0x02e7;	 Catch:{ all -> 0x02f8 }
    L_0x02af:
        r3 = "persist.sys.lasttime";	 Catch:{ all -> 0x02f8 }
        r6 = 0;	 Catch:{ all -> 0x02f8 }
        r6 = android.os.SystemProperties.getLong(r3, r6);	 Catch:{ all -> 0x02f8 }
        r3 = (r6 > r24 ? 1 : (r6 == r24 ? 0 : -1));	 Catch:{ all -> 0x02f8 }
        if (r3 < 0) goto L_0x02e7;	 Catch:{ all -> 0x02f8 }
    L_0x02bc:
        r3 = "OppoHttpClient";	 Catch:{ all -> 0x02f8 }
        r5 = "Cached by carrieroperator or others, Need Ntp algin time!";	 Catch:{ all -> 0x02f8 }
        android.util.Log.d(r3, r5);	 Catch:{ all -> 0x02f8 }
        r3 = "OppoHttpClient";	 Catch:{ all -> 0x02f8 }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02f8 }
        r5.<init>();	 Catch:{ all -> 0x02f8 }
        r6 = "mGMTTime:";	 Catch:{ all -> 0x02f8 }
        r5 = r5.append(r6);	 Catch:{ all -> 0x02f8 }
        r0 = r24;	 Catch:{ all -> 0x02f8 }
        r5 = r5.append(r0);	 Catch:{ all -> 0x02f8 }
        r5 = r5.toString();	 Catch:{ all -> 0x02f8 }
        android.util.Log.d(r3, r5);	 Catch:{ all -> 0x02f8 }
        r41 = 0;
    L_0x02e3:
        monitor-exit(r4);	 Catch:{ Exception -> 0x02fb }
        r45 = r46;
    L_0x02e6:
        return r41;
    L_0x02e7:
        r3 = "persist.sys.lasttime";	 Catch:{ all -> 0x02f8 }
        r5 = java.lang.Long.toString(r24);	 Catch:{ all -> 0x02f8 }
        android.os.SystemProperties.set(r3, r5);	 Catch:{ all -> 0x02f8 }
        r6 = android.os.SystemClock.elapsedRealtime();	 Catch:{ all -> 0x02f8 }
        mLastGotSuccessLocaltime = r6;	 Catch:{ all -> 0x02f8 }
        goto L_0x02e3;
    L_0x02f8:
        r3 = move-exception;
        monitor-exit(r4);	 Catch:{ Exception -> 0x02fb }
        throw r3;	 Catch:{ Exception -> 0x02fb }
    L_0x02fb:
        r11 = move-exception;
        r45 = r46;
    L_0x02fe:
        r3 = "OppoHttpClient";
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "oppoServer exception: ";
        r4 = r4.append(r5);
        r4 = r4.append(r11);
        r4 = r4.toString();
        android.util.Log.e(r3, r4);
        r41 = 0;
        goto L_0x02e6;
    L_0x031b:
        r11 = move-exception;
        goto L_0x02fe;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.net.OppoHttpClient.forceRefreshTimeFromOppoServer(android.content.Context, int, int):boolean");
    }

    private boolean getNetType(Context context) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conn == null) {
            return false;
        }
        NetworkInfo info = conn.getActiveNetworkInfo();
        if (info == null) {
            return false;
        }
        String type = info.getTypeName();
        if (type.equalsIgnoreCase("WIFI")) {
            return true;
        }
        if (!type.equalsIgnoreCase("MOBILE") && !type.equalsIgnoreCase("GPRS")) {
            return true;
        }
        String apn = info.getExtraInfo();
        return apn == null || !apn.equalsIgnoreCase("cmwap");
    }

    public long getHttpTime() {
        return this.mHttpTime;
    }

    public long getHttpTimeReference() {
        return this.mHttpTimeReference;
    }

    public long getRoundTripTime() {
        return this.mRoundTripTime;
    }
}
