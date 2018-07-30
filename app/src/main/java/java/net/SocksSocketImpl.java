package java.net;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Types;
import sun.security.action.GetPropertyAction;

class SocksSocketImpl extends PlainSocketImpl implements SocksConsts {
    static final /* synthetic */ boolean -assertionsDisabled = (SocksSocketImpl.class.desiredAssertionStatus() ^ 1);
    private boolean applicationSetProxy;
    private InputStream cmdIn = null;
    private OutputStream cmdOut = null;
    private Socket cmdsock = null;
    private InetSocketAddress external_address;
    private String server = null;
    private int serverPort = SocksConsts.DEFAULT_PORT;
    private boolean useV4 = false;

    SocksSocketImpl() {
    }

    SocksSocketImpl(String server, int port) {
        this.server = server;
        if (port == -1) {
            port = SocksConsts.DEFAULT_PORT;
        }
        this.serverPort = port;
    }

    SocksSocketImpl(Proxy proxy) {
        SocketAddress a = proxy.address();
        if (a instanceof InetSocketAddress) {
            InetSocketAddress ad = (InetSocketAddress) a;
            this.server = ad.getHostString();
            this.serverPort = ad.getPort();
        }
    }

    void setV4() {
        this.useV4 = true;
    }

    private synchronized void privilegedConnect(final String host, final int port, final int timeout) throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    SocksSocketImpl.this.superConnectServer(host, port, timeout);
                    SocksSocketImpl.this.cmdIn = SocksSocketImpl.this.getInputStream();
                    SocksSocketImpl.this.cmdOut = SocksSocketImpl.this.getOutputStream();
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            throw ((IOException) pae.getException());
        }
    }

    private void superConnectServer(String host, int port, int timeout) throws IOException {
        super.connect(new InetSocketAddress(host, port), timeout);
    }

    private static int remainingMillis(long deadlineMillis) throws IOException {
        if (deadlineMillis == 0) {
            return 0;
        }
        long remaining = deadlineMillis - System.currentTimeMillis();
        if (remaining > 0) {
            return (int) remaining;
        }
        throw new SocketTimeoutException();
    }

    private int readSocksReply(InputStream in, byte[] data) throws IOException {
        return readSocksReply(in, data, 0);
    }

    private int readSocksReply(InputStream in, byte[] data, long deadlineMillis) throws IOException {
        int len = data.length;
        int received = 0;
        int attempts = 0;
        while (received < len && attempts < 3) {
            try {
                int count = ((SocketInputStream) in).read(data, received, len - received, remainingMillis(deadlineMillis));
                if (count < 0) {
                    throw new SocketException("Malformed reply from SOCKS server");
                }
                received += count;
                attempts++;
            } catch (SocketTimeoutException e) {
                throw new SocketTimeoutException("Connect timed out");
            }
        }
        return received;
    }

    private boolean authenticate(byte method, InputStream in, BufferedOutputStream out) throws IOException {
        return authenticate(method, in, out, 0);
    }

    private boolean authenticate(byte method, InputStream in, BufferedOutputStream out, long deadlineMillis) throws IOException {
        if (method == (byte) 0) {
            return true;
        }
        if (method != (byte) 2) {
            return false;
        }
        String userName;
        String str = null;
        final InetAddress addr = InetAddress.getByName(this.server);
        PasswordAuthentication pw = (PasswordAuthentication) AccessController.doPrivileged(new PrivilegedAction<PasswordAuthentication>() {
            public PasswordAuthentication run() {
                return Authenticator.requestPasswordAuthentication(SocksSocketImpl.this.server, addr, SocksSocketImpl.this.serverPort, "SOCKS5", "SOCKS authentication", null);
            }
        });
        if (pw != null) {
            userName = pw.getUserName();
            str = new String(pw.getPassword());
        } else {
            userName = (String) AccessController.doPrivileged(new GetPropertyAction("user.name"));
        }
        if (userName == null) {
            return false;
        }
        out.write(1);
        out.write(userName.length());
        try {
            out.write(userName.getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            if (!-assertionsDisabled) {
                throw new AssertionError();
            }
        }
        if (str != null) {
            out.write(str.length());
            try {
                out.write(str.getBytes("ISO-8859-1"));
            } catch (UnsupportedEncodingException e2) {
                if (!-assertionsDisabled) {
                    throw new AssertionError();
                }
            }
        }
        out.write(0);
        out.flush();
        byte[] data = new byte[2];
        if (readSocksReply(in, data, deadlineMillis) == 2 && data[1] == (byte) 0) {
            return true;
        }
        out.close();
        in.close();
        return false;
    }

    private void connectV4(InputStream in, OutputStream out, InetSocketAddress endpoint, long deadlineMillis) throws IOException {
        if (endpoint.getAddress() instanceof Inet4Address) {
            out.write(4);
            out.write(1);
            out.write((endpoint.getPort() >> 8) & 255);
            out.write((endpoint.getPort() >> 0) & 255);
            out.write(endpoint.getAddress().getAddress());
            try {
                out.write(getUserName().getBytes("ISO-8859-1"));
            } catch (UnsupportedEncodingException e) {
                if (!-assertionsDisabled) {
                    throw new AssertionError();
                }
            }
            out.write(0);
            out.flush();
            byte[] data = new byte[8];
            int n = readSocksReply(in, data, deadlineMillis);
            if (n != 8) {
                throw new SocketException("Reply from SOCKS server has bad length: " + n);
            } else if (data[0] == (byte) 0 || data[0] == (byte) 4) {
                SocketException ex = null;
                switch (data[1]) {
                    case (byte) 90:
                        this.external_address = endpoint;
                        break;
                    case Types.DATE /*91*/:
                        ex = new SocketException("SOCKS request rejected");
                        break;
                    case Types.TIME /*92*/:
                        ex = new SocketException("SOCKS server couldn't reach destination");
                        break;
                    case Types.TIMESTAMP /*93*/:
                        ex = new SocketException("SOCKS authentication failed");
                        break;
                    default:
                        ex = new SocketException("Reply from SOCKS server contains bad status");
                        break;
                }
                if (ex != null) {
                    in.close();
                    out.close();
                    throw ex;
                }
                return;
            } else {
                throw new SocketException("Reply from SOCKS server has bad version");
            }
        }
        throw new SocketException("SOCKS V4 requires IPv4 only addresses");
    }

    protected void connect(java.net.SocketAddress r26, int r27) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r17_2 'ex' java.net.SocketException) in PHI: PHI: (r17_1 'ex' java.net.SocketException) = (r17_0 'ex' java.net.SocketException), (r17_2 'ex' java.net.SocketException), (r17_0 'ex' java.net.SocketException), (r17_0 'ex' java.net.SocketException), (r17_0 'ex' java.net.SocketException), (r17_3 'ex' java.net.SocketException), (r17_4 'ex' java.net.SocketException), (r17_5 'ex' java.net.SocketException), (r17_6 'ex' java.net.SocketException), (r17_7 'ex' java.net.SocketException), (r17_8 'ex' java.net.SocketException), (r17_9 'ex' java.net.SocketException), (r17_10 'ex' java.net.SocketException) binds: {(r17_0 'ex' java.net.SocketException)=B:73:0x01db, (r17_2 'ex' java.net.SocketException)=B:79:0x01ed, (r17_0 'ex' java.net.SocketException)=B:85:0x021b, (r17_0 'ex' java.net.SocketException)=B:93:0x0252, (r17_0 'ex' java.net.SocketException)=B:101:0x0285, (r17_3 'ex' java.net.SocketException)=B:104:0x0290, (r17_4 'ex' java.net.SocketException)=B:105:0x029c, (r17_5 'ex' java.net.SocketException)=B:106:0x02a8, (r17_6 'ex' java.net.SocketException)=B:107:0x02b4, (r17_7 'ex' java.net.SocketException)=B:108:0x02c0, (r17_8 'ex' java.net.SocketException)=B:109:0x02cc, (r17_9 'ex' java.net.SocketException)=B:110:0x02d8, (r17_10 'ex' java.net.SocketException)=B:111:0x02e4}
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
        r25 = this;
        if (r27 != 0) goto L_0x001b;
    L_0x0002:
        r6 = 0;
    L_0x0004:
        r23 = java.lang.System.getSecurityManager();
        if (r26 == 0) goto L_0x0012;
    L_0x000a:
        r0 = r26;
        r2 = r0 instanceof java.net.InetSocketAddress;
        r2 = r2 ^ 1;
        if (r2 == 0) goto L_0x0033;
    L_0x0012:
        r2 = new java.lang.IllegalArgumentException;
        r8 = "Unsupported address type";
        r2.<init>(r8);
        throw r2;
    L_0x001b:
        r8 = java.lang.System.currentTimeMillis();
        r0 = r27;
        r10 = (long) r0;
        r18 = r8 + r10;
        r8 = 0;
        r2 = (r18 > r8 ? 1 : (r18 == r8 ? 0 : -1));
        if (r2 >= 0) goto L_0x0030;
    L_0x002a:
        r6 = 9223372036854775807; // 0x7fffffffffffffff float:NaN double:NaN;
        goto L_0x0004;
    L_0x0030:
        r6 = r18;
        goto L_0x0004;
    L_0x0033:
        r5 = r26;
        r5 = (java.net.InetSocketAddress) r5;
        if (r23 == 0) goto L_0x004c;
    L_0x0039:
        r2 = r5.isUnresolved();
        if (r2 == 0) goto L_0x005c;
    L_0x003f:
        r2 = r5.getHostName();
        r8 = r5.getPort();
        r0 = r23;
        r0.checkConnect(r2, r8);
    L_0x004c:
        r0 = r25;
        r2 = r0.server;
        if (r2 != 0) goto L_0x006e;
    L_0x0052:
        r2 = remainingMillis(r6);
        r0 = r25;
        super.connect(r5, r2);
        return;
    L_0x005c:
        r2 = r5.getAddress();
        r2 = r2.getHostAddress();
        r8 = r5.getPort();
        r0 = r23;
        r0.checkConnect(r2, r8);
        goto L_0x004c;
    L_0x006e:
        r0 = r25;	 Catch:{ IOException -> 0x00a4 }
        r2 = r0.server;	 Catch:{ IOException -> 0x00a4 }
        r0 = r25;	 Catch:{ IOException -> 0x00a4 }
        r8 = r0.serverPort;	 Catch:{ IOException -> 0x00a4 }
        r9 = remainingMillis(r6);	 Catch:{ IOException -> 0x00a4 }
        r0 = r25;	 Catch:{ IOException -> 0x00a4 }
        r0.privilegedConnect(r2, r8, r9);	 Catch:{ IOException -> 0x00a4 }
        r4 = new java.io.BufferedOutputStream;
        r0 = r25;
        r2 = r0.cmdOut;
        r8 = 512; // 0x200 float:7.175E-43 double:2.53E-321;
        r4.<init>(r2, r8);
        r0 = r25;
        r3 = r0.cmdIn;
        r0 = r25;
        r2 = r0.useV4;
        if (r2 == 0) goto L_0x00b5;
    L_0x0094:
        r2 = r5.isUnresolved();
        if (r2 == 0) goto L_0x00af;
    L_0x009a:
        r2 = new java.net.UnknownHostException;
        r8 = r5.toString();
        r2.<init>(r8);
        throw r2;
    L_0x00a4:
        r16 = move-exception;
        r2 = new java.net.SocketException;
        r8 = r16.getMessage();
        r2.<init>(r8);
        throw r2;
    L_0x00af:
        r2 = r25;
        r2.connectV4(r3, r4, r5, r6);
        return;
    L_0x00b5:
        r2 = 5;
        r4.write(r2);
        r2 = 2;
        r4.write(r2);
        r2 = 0;
        r4.write(r2);
        r2 = 2;
        r4.write(r2);
        r4.flush();
        r2 = 2;
        r15 = new byte[r2];
        r0 = r25;
        r21 = r0.readSocksReply(r3, r15, r6);
        r2 = 2;
        r0 = r21;
        if (r0 != r2) goto L_0x00dc;
    L_0x00d6:
        r2 = 0;
        r2 = r15[r2];
        r8 = 5;
        if (r2 == r8) goto L_0x00f2;
    L_0x00dc:
        r2 = r5.isUnresolved();
        if (r2 == 0) goto L_0x00ec;
    L_0x00e2:
        r2 = new java.net.UnknownHostException;
        r8 = r5.toString();
        r2.<init>(r8);
        throw r2;
    L_0x00ec:
        r2 = r25;
        r2.connectV4(r3, r4, r5, r6);
        return;
    L_0x00f2:
        r2 = 1;
        r2 = r15[r2];
        r8 = -1;
        if (r2 != r8) goto L_0x0101;
    L_0x00f8:
        r2 = new java.net.SocketException;
        r8 = "SOCKS : No acceptable methods";
        r2.<init>(r8);
        throw r2;
    L_0x0101:
        r2 = 1;
        r9 = r15[r2];
        r8 = r25;
        r10 = r3;
        r11 = r4;
        r12 = r6;
        r2 = r8.authenticate(r9, r10, r11, r12);
        if (r2 != 0) goto L_0x0118;
    L_0x010f:
        r2 = new java.net.SocketException;
        r8 = "SOCKS : authentication failed";
        r2.<init>(r8);
        throw r2;
    L_0x0118:
        r2 = 5;
        r4.write(r2);
        r2 = 1;
        r4.write(r2);
        r2 = 0;
        r4.write(r2);
        r2 = r5.isUnresolved();
        if (r2 == 0) goto L_0x0182;
    L_0x012a:
        r2 = 3;
        r4.write(r2);
        r2 = r5.getHostName();
        r2 = r2.length();
        r4.write(r2);
        r2 = r5.getHostName();	 Catch:{ UnsupportedEncodingException -> 0x0177 }
        r8 = "ISO-8859-1";	 Catch:{ UnsupportedEncodingException -> 0x0177 }
        r2 = r2.getBytes(r8);	 Catch:{ UnsupportedEncodingException -> 0x0177 }
        r4.write(r2);	 Catch:{ UnsupportedEncodingException -> 0x0177 }
    L_0x0147:
        r2 = r5.getPort();
        r2 = r2 >> 8;
        r2 = r2 & 255;
        r4.write(r2);
        r2 = r5.getPort();
        r2 = r2 >> 0;
        r2 = r2 & 255;
        r4.write(r2);
    L_0x015d:
        r4.flush();
        r2 = 4;
        r15 = new byte[r2];
        r0 = r25;
        r21 = r0.readSocksReply(r3, r15, r6);
        r2 = 4;
        r0 = r21;
        if (r0 == r2) goto L_0x01d6;
    L_0x016e:
        r2 = new java.net.SocketException;
        r8 = "Reply from SOCKS server has bad length";
        r2.<init>(r8);
        throw r2;
    L_0x0177:
        r24 = move-exception;
        r2 = -assertionsDisabled;
        if (r2 != 0) goto L_0x0147;
    L_0x017c:
        r2 = new java.lang.AssertionError;
        r2.<init>();
        throw r2;
    L_0x0182:
        r2 = r5.getAddress();
        r2 = r2 instanceof java.net.Inet6Address;
        if (r2 == 0) goto L_0x01b0;
    L_0x018a:
        r2 = 4;
        r4.write(r2);
        r2 = r5.getAddress();
        r2 = r2.getAddress();
        r4.write(r2);
        r2 = r5.getPort();
        r2 = r2 >> 8;
        r2 = r2 & 255;
        r4.write(r2);
        r2 = r5.getPort();
        r2 = r2 >> 0;
        r2 = r2 & 255;
        r4.write(r2);
        goto L_0x015d;
    L_0x01b0:
        r2 = 1;
        r4.write(r2);
        r2 = r5.getAddress();
        r2 = r2.getAddress();
        r4.write(r2);
        r2 = r5.getPort();
        r2 = r2 >> 8;
        r2 = r2 & 255;
        r4.write(r2);
        r2 = r5.getPort();
        r2 = r2 >> 0;
        r2 = r2 & 255;
        r4.write(r2);
        goto L_0x015d;
    L_0x01d6:
        r17 = 0;
        r2 = 1;
        r2 = r15[r2];
        switch(r2) {
            case 0: goto L_0x01e7;
            case 1: goto L_0x0290;
            case 2: goto L_0x029c;
            case 3: goto L_0x02a8;
            case 4: goto L_0x02b4;
            case 5: goto L_0x02c0;
            case 6: goto L_0x02cc;
            case 7: goto L_0x02d8;
            case 8: goto L_0x02e4;
            default: goto L_0x01de;
        };
    L_0x01de:
        if (r17 == 0) goto L_0x02f0;
    L_0x01e0:
        r3.close();
        r4.close();
        throw r17;
    L_0x01e7:
        r2 = 3;
        r2 = r15[r2];
        switch(r2) {
            case 1: goto L_0x01f8;
            case 2: goto L_0x01ed;
            case 3: goto L_0x0226;
            case 4: goto L_0x025d;
            default: goto L_0x01ed;
        };
    L_0x01ed:
        r17 = new java.net.SocketException;
        r2 = "Reply from SOCKS server contains wrong code";
        r0 = r17;
        r0.<init>(r2);
        goto L_0x01de;
    L_0x01f8:
        r2 = 4;
        r14 = new byte[r2];
        r0 = r25;
        r21 = r0.readSocksReply(r3, r14, r6);
        r2 = 4;
        r0 = r21;
        if (r0 == r2) goto L_0x020f;
    L_0x0206:
        r2 = new java.net.SocketException;
        r8 = "Reply from SOCKS server badly formatted";
        r2.<init>(r8);
        throw r2;
    L_0x020f:
        r2 = 2;
        r15 = new byte[r2];
        r0 = r25;
        r21 = r0.readSocksReply(r3, r15, r6);
        r2 = 2;
        r0 = r21;
        if (r0 == r2) goto L_0x01de;
    L_0x021d:
        r2 = new java.net.SocketException;
        r8 = "Reply from SOCKS server badly formatted";
        r2.<init>(r8);
        throw r2;
    L_0x0226:
        r2 = 1;
        r22 = r15[r2];
        r0 = r22;
        r0 = new byte[r0];
        r20 = r0;
        r0 = r25;
        r1 = r20;
        r21 = r0.readSocksReply(r3, r1, r6);
        r0 = r21;
        r1 = r22;
        if (r0 == r1) goto L_0x0246;
    L_0x023d:
        r2 = new java.net.SocketException;
        r8 = "Reply from SOCKS server badly formatted";
        r2.<init>(r8);
        throw r2;
    L_0x0246:
        r2 = 2;
        r15 = new byte[r2];
        r0 = r25;
        r21 = r0.readSocksReply(r3, r15, r6);
        r2 = 2;
        r0 = r21;
        if (r0 == r2) goto L_0x01de;
    L_0x0254:
        r2 = new java.net.SocketException;
        r8 = "Reply from SOCKS server badly formatted";
        r2.<init>(r8);
        throw r2;
    L_0x025d:
        r2 = 1;
        r22 = r15[r2];
        r0 = r22;
        r14 = new byte[r0];
        r0 = r25;
        r21 = r0.readSocksReply(r3, r14, r6);
        r0 = r21;
        r1 = r22;
        if (r0 == r1) goto L_0x0279;
    L_0x0270:
        r2 = new java.net.SocketException;
        r8 = "Reply from SOCKS server badly formatted";
        r2.<init>(r8);
        throw r2;
    L_0x0279:
        r2 = 2;
        r15 = new byte[r2];
        r0 = r25;
        r21 = r0.readSocksReply(r3, r15, r6);
        r2 = 2;
        r0 = r21;
        if (r0 == r2) goto L_0x01de;
    L_0x0287:
        r2 = new java.net.SocketException;
        r8 = "Reply from SOCKS server badly formatted";
        r2.<init>(r8);
        throw r2;
    L_0x0290:
        r17 = new java.net.SocketException;
        r2 = "SOCKS server general failure";
        r0 = r17;
        r0.<init>(r2);
        goto L_0x01de;
    L_0x029c:
        r17 = new java.net.SocketException;
        r2 = "SOCKS: Connection not allowed by ruleset";
        r0 = r17;
        r0.<init>(r2);
        goto L_0x01de;
    L_0x02a8:
        r17 = new java.net.SocketException;
        r2 = "SOCKS: Network unreachable";
        r0 = r17;
        r0.<init>(r2);
        goto L_0x01de;
    L_0x02b4:
        r17 = new java.net.SocketException;
        r2 = "SOCKS: Host unreachable";
        r0 = r17;
        r0.<init>(r2);
        goto L_0x01de;
    L_0x02c0:
        r17 = new java.net.SocketException;
        r2 = "SOCKS: Connection refused";
        r0 = r17;
        r0.<init>(r2);
        goto L_0x01de;
    L_0x02cc:
        r17 = new java.net.SocketException;
        r2 = "SOCKS: TTL expired";
        r0 = r17;
        r0.<init>(r2);
        goto L_0x01de;
    L_0x02d8:
        r17 = new java.net.SocketException;
        r2 = "SOCKS: Command not supported";
        r0 = r17;
        r0.<init>(r2);
        goto L_0x01de;
    L_0x02e4:
        r17 = new java.net.SocketException;
        r2 = "SOCKS: address type not supported";
        r0 = r17;
        r0.<init>(r2);
        goto L_0x01de;
    L_0x02f0:
        r0 = r25;
        r0.external_address = r5;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: java.net.SocksSocketImpl.connect(java.net.SocketAddress, int):void");
    }

    protected InetAddress getInetAddress() {
        if (this.external_address != null) {
            return this.external_address.getAddress();
        }
        return super.getInetAddress();
    }

    protected int getPort() {
        if (this.external_address != null) {
            return this.external_address.getPort();
        }
        return super.getPort();
    }

    protected int getLocalPort() {
        if (this.socket != null) {
            return super.getLocalPort();
        }
        if (this.external_address != null) {
            return this.external_address.getPort();
        }
        return super.getLocalPort();
    }

    protected void close() throws IOException {
        if (this.cmdsock != null) {
            this.cmdsock.close();
        }
        this.cmdsock = null;
        super.close();
    }

    private String getUserName() {
        String userName = "";
        if (!this.applicationSetProxy) {
            return (String) AccessController.doPrivileged(new GetPropertyAction("user.name"));
        }
        try {
            return System.getProperty("user.name");
        } catch (SecurityException e) {
            return userName;
        }
    }
}
