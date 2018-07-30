package android.net.dhcp;

import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.metrics.DhcpErrorEvent;
import android.net.util.NetworkConstants;
import android.os.Build.VERSION;
import android.os.SystemProperties;
import android.system.OsConstants;
import android.text.TextUtils;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class DhcpPacket {
    protected static final byte CLIENT_ID_ETHER = (byte) 1;
    protected static final byte DHCP_BOOTREPLY = (byte) 2;
    protected static final byte DHCP_BOOTREQUEST = (byte) 1;
    protected static final byte DHCP_BROADCAST_ADDRESS = (byte) 28;
    static final short DHCP_CLIENT = (short) 68;
    protected static final byte DHCP_CLIENT_IDENTIFIER = (byte) 61;
    protected static final byte DHCP_DNS_SERVER = (byte) 6;
    protected static final byte DHCP_DOMAIN_NAME = (byte) 15;
    protected static final byte DHCP_HOST_NAME = (byte) 12;
    protected static final byte DHCP_LEASE_TIME = (byte) 51;
    private static final int DHCP_MAGIC_COOKIE = 1669485411;
    protected static final byte DHCP_MAX_MESSAGE_SIZE = (byte) 57;
    protected static final byte DHCP_MESSAGE = (byte) 56;
    protected static final byte DHCP_MESSAGE_TYPE = (byte) 53;
    protected static final byte DHCP_MESSAGE_TYPE_ACK = (byte) 5;
    protected static final byte DHCP_MESSAGE_TYPE_DECLINE = (byte) 4;
    protected static final byte DHCP_MESSAGE_TYPE_DISCOVER = (byte) 1;
    protected static final byte DHCP_MESSAGE_TYPE_INFORM = (byte) 8;
    protected static final byte DHCP_MESSAGE_TYPE_NAK = (byte) 6;
    protected static final byte DHCP_MESSAGE_TYPE_OFFER = (byte) 2;
    protected static final byte DHCP_MESSAGE_TYPE_REQUEST = (byte) 3;
    protected static final byte DHCP_MTU = (byte) 26;
    protected static final byte DHCP_OPTION_END = (byte) -1;
    protected static final byte DHCP_OPTION_PAD = (byte) 0;
    protected static final byte DHCP_OPTION_RAPID_COMMIT = (byte) 80;
    protected static final byte DHCP_PARAMETER_LIST = (byte) 55;
    protected static final byte DHCP_REBINDING_TIME = (byte) 59;
    protected static final byte DHCP_RENEWAL_TIME = (byte) 58;
    protected static final byte DHCP_REQUESTED_IP = (byte) 50;
    protected static final byte DHCP_ROUTER = (byte) 3;
    static final short DHCP_SERVER = (short) 67;
    protected static final byte DHCP_SERVER_IDENTIFIER = (byte) 54;
    protected static final byte DHCP_SUBNET_MASK = (byte) 1;
    protected static final byte DHCP_VENDOR_CLASS_ID = (byte) 60;
    protected static final byte DHCP_VENDOR_INFO = (byte) 43;
    public static final int ENCAP_BOOTP = 2;
    public static final int ENCAP_L2 = 0;
    public static final int ENCAP_L3 = 1;
    public static final byte[] ETHER_BROADCAST = new byte[]{(byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1};
    public static final int HWADDR_LEN = 16;
    public static final Inet4Address INADDR_ANY = ((Inet4Address) Inet4Address.ANY);
    public static final Inet4Address INADDR_BROADCAST = ((Inet4Address) Inet4Address.ALL);
    public static final int INFINITE_LEASE = -1;
    private static final short IP_FLAGS_OFFSET = (short) 16384;
    private static final byte IP_TOS_LOWDELAY = (byte) 16;
    private static final byte IP_TTL = (byte) 64;
    private static final byte IP_TYPE_UDP = (byte) 17;
    private static final byte IP_VERSION_HEADER_LEN = (byte) 69;
    protected static final int MAX_LENGTH = 1500;
    private static final int MAX_MTU = 1500;
    public static final int MAX_OPTION_LEN = 255;
    public static final int MINIMUM_LEASE = 60;
    private static final int MIN_MTU = 1280;
    public static final int MIN_PACKET_LENGTH_BOOTP = 236;
    public static final int MIN_PACKET_LENGTH_L2 = 278;
    public static final int MIN_PACKET_LENGTH_L3 = 264;
    protected static final String TAG = "DhcpPacket";
    static String testOverrideHostname = null;
    static String testOverrideVendorId = null;
    protected boolean mBroadcast;
    protected Inet4Address mBroadcastAddress;
    protected final Inet4Address mClientIp;
    protected final byte[] mClientMac;
    protected List<Inet4Address> mDnsServers;
    protected String mDomainName;
    protected List<Inet4Address> mGateways;
    protected String mHostName;
    protected Integer mLeaseTime;
    protected Short mMaxMessageSize;
    protected String mMessage;
    protected Short mMtu;
    private final Inet4Address mNextIp;
    protected boolean mRapidCommit;
    private final Inet4Address mRelayIp;
    protected Inet4Address mRequestedIp;
    protected byte[] mRequestedParams;
    protected final short mSecs;
    protected Inet4Address mServerIdentifier;
    protected Inet4Address mSubnetMask;
    protected Integer mT1;
    protected Integer mT2;
    protected final int mTransId;
    protected String mVendorId;
    protected String mVendorInfo;
    protected final Inet4Address mYourIp;

    public static class ParseException extends Exception {
        public final int errorCode;

        public ParseException(int errorCode, String msg, Object... args) {
            super(String.format(msg, args));
            this.errorCode = errorCode;
        }
    }

    public abstract ByteBuffer buildPacket(int i, short s, short s2);

    abstract void finishPacket(ByteBuffer byteBuffer);

    protected DhcpPacket(int transId, short secs, Inet4Address clientIp, Inet4Address yourIp, Inet4Address nextIp, Inet4Address relayIp, byte[] clientMac, boolean broadcast, boolean rapidCommit) {
        this.mTransId = transId;
        this.mSecs = secs;
        this.mClientIp = clientIp;
        this.mYourIp = yourIp;
        this.mNextIp = nextIp;
        this.mRelayIp = relayIp;
        this.mClientMac = clientMac;
        this.mBroadcast = broadcast;
        this.mRapidCommit = rapidCommit;
    }

    protected DhcpPacket(int transId, short secs, Inet4Address clientIp, Inet4Address yourIp, Inet4Address nextIp, Inet4Address relayIp, byte[] clientMac, boolean broadcast) {
        this(transId, secs, clientIp, yourIp, nextIp, relayIp, clientMac, broadcast, false);
    }

    public int getTransactionId() {
        return this.mTransId;
    }

    public byte[] getClientMac() {
        return this.mClientMac;
    }

    public byte[] getClientId() {
        byte[] clientId = new byte[(this.mClientMac.length + 1)];
        clientId[0] = (byte) 1;
        System.arraycopy(this.mClientMac, 0, clientId, 1, this.mClientMac.length);
        return clientId;
    }

    protected void fillInPacket(int encap, Inet4Address destIp, Inet4Address srcIp, short destUdp, short srcUdp, ByteBuffer buf, byte requestCode, boolean broadcast) {
        byte[] destIpArray = destIp.getAddress();
        byte[] srcIpArray = srcIp.getAddress();
        int ipHeaderOffset = 0;
        int ipLengthOffset = 0;
        int ipChecksumOffset = 0;
        int endIpHeader = 0;
        int udpHeaderOffset = 0;
        int udpLengthOffset = 0;
        int udpChecksumOffset = 0;
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        if (encap == 0) {
            buf.put(ETHER_BROADCAST);
            buf.put(this.mClientMac);
            buf.putShort((short) OsConstants.ETH_P_IP);
        }
        if (encap <= 1) {
            ipHeaderOffset = buf.position();
            buf.put(IP_VERSION_HEADER_LEN);
            buf.put((byte) 16);
            ipLengthOffset = buf.position();
            buf.putShort((short) 0);
            buf.putShort((short) 0);
            buf.putShort(IP_FLAGS_OFFSET);
            buf.put(IP_TTL);
            buf.put((byte) 17);
            ipChecksumOffset = buf.position();
            buf.putShort((short) 0);
            buf.put(srcIpArray);
            buf.put(destIpArray);
            endIpHeader = buf.position();
            udpHeaderOffset = buf.position();
            buf.putShort(srcUdp);
            buf.putShort(destUdp);
            udpLengthOffset = buf.position();
            buf.putShort((short) 0);
            udpChecksumOffset = buf.position();
            buf.putShort((short) 0);
        }
        buf.put(requestCode);
        buf.put((byte) 1);
        buf.put((byte) this.mClientMac.length);
        buf.put((byte) 0);
        buf.putInt(this.mTransId);
        buf.putShort(this.mSecs);
        if (broadcast) {
            buf.putShort(Short.MIN_VALUE);
        } else {
            buf.putShort((short) 0);
        }
        buf.put(this.mClientIp.getAddress());
        buf.put(this.mYourIp.getAddress());
        buf.put(this.mNextIp.getAddress());
        buf.put(this.mRelayIp.getAddress());
        buf.put(this.mClientMac);
        buf.position(((buf.position() + (16 - this.mClientMac.length)) + 64) + 128);
        buf.putInt(DHCP_MAGIC_COOKIE);
        finishPacket(buf);
        if ((buf.position() & 1) == 1) {
            buf.put((byte) 0);
        }
        if (encap <= 1) {
            short udpLen = (short) (buf.position() - udpHeaderOffset);
            buf.putShort(udpLengthOffset, udpLen);
            ByteBuffer byteBuffer = buf;
            byteBuffer = buf;
            byteBuffer.putShort(udpChecksumOffset, (short) checksum(byteBuffer, (((((intAbs(buf.getShort(ipChecksumOffset + 2)) + 0) + intAbs(buf.getShort(ipChecksumOffset + 4))) + intAbs(buf.getShort(ipChecksumOffset + 6))) + intAbs(buf.getShort(ipChecksumOffset + 8))) + 17) + udpLen, udpHeaderOffset, buf.position()));
            buf.putShort(ipLengthOffset, (short) (buf.position() - ipHeaderOffset));
            buf.putShort(ipChecksumOffset, (short) checksum(buf, 0, ipHeaderOffset, endIpHeader));
        }
    }

    private static int intAbs(short v) {
        return NetworkConstants.ARP_HWTYPE_RESERVED_HI & v;
    }

    private int checksum(ByteBuffer buf, int seed, int start, int end) {
        int sum = seed;
        int bufPosition = buf.position();
        buf.position(start);
        ShortBuffer shortBuf = buf.asShortBuffer();
        buf.position(bufPosition);
        short[] shortArray = new short[((end - start) / 2)];
        shortBuf.get(shortArray);
        for (short s : shortArray) {
            sum += intAbs(s);
        }
        start += shortArray.length * 2;
        if (end != start) {
            short b = (short) buf.get(start);
            if (b < (short) 0) {
                b = (short) (b + 256);
            }
            sum += b * 256;
        }
        sum = ((sum >> 16) & NetworkConstants.ARP_HWTYPE_RESERVED_HI) + (sum & NetworkConstants.ARP_HWTYPE_RESERVED_HI);
        return intAbs((short) (~((((sum >> 16) & NetworkConstants.ARP_HWTYPE_RESERVED_HI) + sum) & NetworkConstants.ARP_HWTYPE_RESERVED_HI)));
    }

    protected static void addTlv(ByteBuffer buf, byte type) {
        buf.put(type);
        buf.put((byte) 0);
    }

    protected static void addTlv(ByteBuffer buf, byte type, byte value) {
        buf.put(type);
        buf.put((byte) 1);
        buf.put(value);
    }

    protected static void addTlv(ByteBuffer buf, byte type, byte[] payload) {
        if (payload == null) {
            return;
        }
        if (payload.length > 255) {
            throw new IllegalArgumentException("DHCP option too long: " + payload.length + " vs. " + 255);
        }
        buf.put(type);
        buf.put((byte) payload.length);
        buf.put(payload);
    }

    protected static void addTlv(ByteBuffer buf, byte type, Inet4Address addr) {
        if (addr != null) {
            addTlv(buf, type, addr.getAddress());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, List<Inet4Address> addrs) {
        if (addrs != null && addrs.size() != 0) {
            int optionLen = addrs.size() * 4;
            if (optionLen > 255) {
                throw new IllegalArgumentException("DHCP option too long: " + optionLen + " vs. " + 255);
            }
            buf.put(type);
            buf.put((byte) optionLen);
            for (Inet4Address addr : addrs) {
                buf.put(addr.getAddress());
            }
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, Short value) {
        if (value != null) {
            buf.put(type);
            buf.put((byte) 2);
            buf.putShort(value.shortValue());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, Integer value) {
        if (value != null) {
            buf.put(type);
            buf.put((byte) 4);
            buf.putInt(value.intValue());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, String str) {
        try {
            addTlv(buf, type, str.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("String is not US-ASCII: " + str);
        }
    }

    protected static void addTlvEnd(ByteBuffer buf) {
        buf.put((byte) -1);
    }

    private String getVendorId() {
        if (testOverrideVendorId != null) {
            return testOverrideVendorId;
        }
        return "android-dhcp-" + VERSION.RELEASE;
    }

    private String getHostname() {
        if (testOverrideHostname != null) {
            return testOverrideHostname;
        }
        return SystemProperties.get("net.hostname");
    }

    protected void addCommonClientTlvs(ByteBuffer buf) {
        addTlv(buf, (byte) DHCP_MAX_MESSAGE_SIZE, Short.valueOf((short) 1500));
        addTlv(buf, (byte) DHCP_VENDOR_CLASS_ID, getVendorId());
        String hn = getHostname();
        if (!TextUtils.isEmpty(hn)) {
            addTlv(buf, (byte) 12, hn);
        }
    }

    public static String macToString(byte[] mac) {
        String macAddr = "";
        for (int i = 0; i < mac.length; i++) {
            String hexString = "0" + Integer.toHexString(mac[i]);
            macAddr = macAddr + hexString.substring(hexString.length() - 2);
            if (i != mac.length - 1) {
                macAddr = macAddr + ":";
            }
        }
        return macAddr;
    }

    public String toString() {
        return macToString(this.mClientMac);
    }

    private static Inet4Address readIpAddress(ByteBuffer packet) {
        byte[] ipAddr = new byte[4];
        packet.get(ipAddr);
        try {
            return (Inet4Address) Inet4Address.getByAddress(ipAddr);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static String readAsciiString(ByteBuffer buf, int byteCount, boolean nullOk) {
        byte[] bytes = new byte[byteCount];
        buf.get(bytes);
        int length = bytes.length;
        if (!nullOk) {
            length = 0;
            while (length < bytes.length && bytes[length] != (byte) 0) {
                length++;
            }
        }
        return new String(bytes, 0, length, StandardCharsets.US_ASCII);
    }

    private static boolean isPacketToOrFromClient(short udpSrcPort, short udpDstPort) {
        return udpSrcPort == (short) 68 || udpDstPort == (short) 68;
    }

    private static boolean isPacketServerToServer(short udpSrcPort, short udpDstPort) {
        return udpSrcPort == DHCP_SERVER && udpDstPort == DHCP_SERVER;
    }

    static android.net.dhcp.DhcpPacket decodeFullPacket(java.nio.ByteBuffer r72, int r73) throws android.net.dhcp.DhcpPacket.ParseException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r2_3 'newPacket' android.net.dhcp.DhcpPacket) in PHI: PHI: (r2_1 'newPacket' android.net.dhcp.DhcpPacket) = (r2_0 'newPacket' android.net.dhcp.DhcpPacket), (r2_2 'newPacket' android.net.dhcp.DhcpPacket), (r2_3 'newPacket' android.net.dhcp.DhcpPacket), (r2_4 'newPacket' android.net.dhcp.DhcpPacket), (r2_5 'newPacket' android.net.dhcp.DhcpPacket), (r2_6 'newPacket' android.net.dhcp.DhcpPacket), (r2_7 'newPacket' android.net.dhcp.DhcpPacket) binds: {(r2_0 'newPacket' android.net.dhcp.DhcpPacket)=B:114:0x0439, (r2_2 'newPacket' android.net.dhcp.DhcpPacket)=B:117:0x0483, (r2_3 'newPacket' android.net.dhcp.DhcpPacket)=B:118:0x0489, (r2_4 'newPacket' android.net.dhcp.DhcpPacket)=B:119:0x0495, (r2_5 'newPacket' android.net.dhcp.DhcpPacket)=B:120:0x04a2, (r2_6 'newPacket' android.net.dhcp.DhcpPacket)=B:121:0x04a8, (r2_7 'newPacket' android.net.dhcp.DhcpPacket)=B:122:0x04b5}
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
        r25 = new java.util.ArrayList;
        r25.<init>();
        r32 = new java.util.ArrayList;
        r32.<init>();
        r63 = 0;
        r57 = 0;
        r55 = 0;
        r70 = 0;
        r71 = 0;
        r31 = 0;
        r34 = 0;
        r26 = 0;
        r6 = 0;
        r40 = 0;
        r21 = 0;
        r62 = 0;
        r56 = 0;
        r54 = 0;
        r53 = 0;
        r18 = 0;
        r19 = 0;
        r24 = -1;
        r10 = java.nio.ByteOrder.BIG_ENDIAN;
        r0 = r72;
        r0.order(r10);
        if (r73 != 0) goto L_0x00a4;
    L_0x0036:
        r10 = r72.remaining();
        r11 = 278; // 0x116 float:3.9E-43 double:1.374E-321;
        if (r10 >= r11) goto L_0x0062;
    L_0x003e:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.L2_TOO_SHORT;
        r12 = "L2 packet too short, %d < %d";
        r13 = 2;
        r13 = new java.lang.Object[r13];
        r14 = r72.remaining();
        r14 = java.lang.Integer.valueOf(r14);
        r17 = 0;
        r13[r17] = r14;
        r14 = 278; // 0x116 float:3.9E-43 double:1.374E-321;
        r14 = java.lang.Integer.valueOf(r14);
        r17 = 1;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x0062:
        r10 = 6;
        r0 = new byte[r10];
        r50 = r0;
        r10 = 6;
        r0 = new byte[r10];
        r51 = r0;
        r0 = r72;
        r1 = r50;
        r0.get(r1);
        r0 = r72;
        r1 = r51;
        r0.get(r1);
        r52 = r72.getShort();
        r10 = android.system.OsConstants.ETH_P_IP;
        r0 = r52;
        if (r0 == r10) goto L_0x00a4;
    L_0x0084:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.L2_WRONG_ETH_TYPE;
        r12 = "Unexpected L2 type 0x%04x, expected 0x%04x";
        r13 = 2;
        r13 = new java.lang.Object[r13];
        r14 = java.lang.Short.valueOf(r52);
        r17 = 0;
        r13[r17] = r14;
        r14 = android.system.OsConstants.ETH_P_IP;
        r14 = java.lang.Integer.valueOf(r14);
        r17 = 1;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x00a4:
        r10 = 1;
        r0 = r73;
        if (r0 > r10) goto L_0x0194;
    L_0x00a9:
        r10 = r72.remaining();
        r11 = 264; // 0x108 float:3.7E-43 double:1.304E-321;
        if (r10 >= r11) goto L_0x00d5;
    L_0x00b1:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.L3_TOO_SHORT;
        r12 = "L3 packet too short, %d < %d";
        r13 = 2;
        r13 = new java.lang.Object[r13];
        r14 = r72.remaining();
        r14 = java.lang.Integer.valueOf(r14);
        r17 = 0;
        r13[r17] = r14;
        r14 = 264; // 0x108 float:3.7E-43 double:1.304E-321;
        r14 = java.lang.Integer.valueOf(r14);
        r17 = 1;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x00d5:
        r47 = r72.get();
        r0 = r47;
        r10 = r0 & 240;
        r48 = r10 >> 4;
        r10 = 4;
        r0 = r48;
        if (r0 == r10) goto L_0x00fa;
    L_0x00e4:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.L3_NOT_IPV4;
        r12 = "Invalid IP version %d";
        r13 = 1;
        r13 = new java.lang.Object[r13];
        r14 = java.lang.Integer.valueOf(r48);
        r17 = 0;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x00fa:
        r39 = r72.get();
        r46 = r72.getShort();
        r43 = r72.getShort();
        r41 = r72.get();
        r42 = r72.get();
        r45 = r72.get();
        r44 = r72.get();
        r38 = r72.getShort();
        r6 = readIpAddress(r72);
        r40 = readIpAddress(r72);
        r10 = 17;
        r0 = r44;
        if (r0 == r10) goto L_0x013e;
    L_0x0128:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.L4_NOT_UDP;
        r12 = "Protocol not UDP: %d";
        r13 = 1;
        r13 = new java.lang.Object[r13];
        r14 = java.lang.Byte.valueOf(r44);
        r17 = 0;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x013e:
        r10 = r47 & 15;
        r61 = r10 + -5;
        r36 = 0;
    L_0x0144:
        r0 = r36;
        r1 = r61;
        if (r0 >= r1) goto L_0x0150;
    L_0x014a:
        r72.getInt();
        r36 = r36 + 1;
        goto L_0x0144;
    L_0x0150:
        r69 = r72.getShort();
        r67 = r72.getShort();
        r68 = r72.getShort();
        r66 = r72.getShort();
        r0 = r69;
        r1 = r67;
        r10 = isPacketToOrFromClient(r0, r1);
        if (r10 != 0) goto L_0x0194;
    L_0x016a:
        r0 = r69;
        r1 = r67;
        r10 = isPacketServerToServer(r0, r1);
        r10 = r10 ^ 1;
        if (r10 == 0) goto L_0x0194;
    L_0x0176:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.L4_WRONG_PORT;
        r12 = "Unexpected UDP ports %d->%d";
        r13 = 2;
        r13 = new java.lang.Object[r13];
        r14 = java.lang.Short.valueOf(r69);
        r17 = 0;
        r13[r17] = r14;
        r14 = java.lang.Short.valueOf(r67);
        r17 = 1;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x0194:
        r10 = 2;
        r0 = r73;
        if (r0 > r10) goto L_0x01a1;
    L_0x0199:
        r10 = r72.remaining();
        r11 = 236; // 0xec float:3.31E-43 double:1.166E-321;
        if (r10 >= r11) goto L_0x01c5;
    L_0x01a1:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.BOOTP_TOO_SHORT;
        r12 = "Invalid type or BOOTP packet too short, %d < %d";
        r13 = 2;
        r13 = new java.lang.Object[r13];
        r14 = r72.remaining();
        r14 = java.lang.Integer.valueOf(r14);
        r17 = 0;
        r13[r17] = r14;
        r14 = 236; // 0xec float:3.31E-43 double:1.166E-321;
        r14 = java.lang.Integer.valueOf(r14);
        r17 = 1;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x01c5:
        r65 = r72.get();
        r35 = r72.get();
        r10 = r72.get();
        r0 = r10 & 255;
        r20 = r0;
        r33 = r72.get();
        r3 = r72.getInt();
        r4 = r72.getShort();
        r22 = r72.getShort();
        r10 = 32768; // 0x8000 float:4.5918E-41 double:1.61895E-319;
        r10 = r10 & r22;
        if (r10 == 0) goto L_0x025f;
    L_0x01ec:
        r5 = 1;
    L_0x01ed:
        r10 = 4;
        r0 = new byte[r10];
        r49 = r0;
        r0 = r72;	 Catch:{ UnknownHostException -> 0x0261 }
        r1 = r49;	 Catch:{ UnknownHostException -> 0x0261 }
        r0.get(r1);	 Catch:{ UnknownHostException -> 0x0261 }
        r7 = java.net.Inet4Address.getByAddress(r49);	 Catch:{ UnknownHostException -> 0x0261 }
        r7 = (java.net.Inet4Address) r7;	 Catch:{ UnknownHostException -> 0x0261 }
        r0 = r72;	 Catch:{ UnknownHostException -> 0x0261 }
        r1 = r49;	 Catch:{ UnknownHostException -> 0x0261 }
        r0.get(r1);	 Catch:{ UnknownHostException -> 0x0261 }
        r8 = java.net.Inet4Address.getByAddress(r49);	 Catch:{ UnknownHostException -> 0x0261 }
        r8 = (java.net.Inet4Address) r8;	 Catch:{ UnknownHostException -> 0x0261 }
        r0 = r72;	 Catch:{ UnknownHostException -> 0x0261 }
        r1 = r49;	 Catch:{ UnknownHostException -> 0x0261 }
        r0.get(r1);	 Catch:{ UnknownHostException -> 0x0261 }
        r15 = java.net.Inet4Address.getByAddress(r49);	 Catch:{ UnknownHostException -> 0x0261 }
        r15 = (java.net.Inet4Address) r15;	 Catch:{ UnknownHostException -> 0x0261 }
        r0 = r72;	 Catch:{ UnknownHostException -> 0x0261 }
        r1 = r49;	 Catch:{ UnknownHostException -> 0x0261 }
        r0.get(r1);	 Catch:{ UnknownHostException -> 0x0261 }
        r16 = java.net.Inet4Address.getByAddress(r49);	 Catch:{ UnknownHostException -> 0x0261 }
        r16 = (java.net.Inet4Address) r16;	 Catch:{ UnknownHostException -> 0x0261 }
        r10 = 16;
        r0 = r20;
        if (r0 <= r10) goto L_0x0231;
    L_0x022c:
        r10 = ETHER_BROADCAST;
        r0 = r10.length;
        r20 = r0;
    L_0x0231:
        r0 = r20;
        r9 = new byte[r0];
        r0 = r72;
        r0.get(r9);
        r10 = r72.position();
        r11 = 16 - r20;
        r10 = r10 + r11;
        r10 = r10 + 64;
        r10 = r10 + 128;
        r0 = r72;
        r0.position(r10);
        r10 = r72.remaining();
        r11 = 4;
        if (r10 >= r11) goto L_0x0278;
    L_0x0251:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.DHCP_NO_COOKIE;
        r12 = "not a DHCP message";
        r13 = 0;
        r13 = new java.lang.Object[r13];
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x025f:
        r5 = 0;
        goto L_0x01ed;
    L_0x0261:
        r29 = move-exception;
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.L3_INVALID_IP;
        r12 = "Invalid IPv4 address: %s";
        r13 = 1;
        r13 = new java.lang.Object[r13];
        r14 = java.util.Arrays.toString(r49);
        r17 = 0;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x0278:
        r23 = r72.getInt();
        r10 = 1669485411; // 0x63825363 float:4.808171E21 double:8.24835388E-315;
        r0 = r23;
        if (r0 == r10) goto L_0x02a4;
    L_0x0283:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.DHCP_BAD_MAGIC_COOKIE;
        r12 = "Bad magic cookie 0x%08x, should be 0x%08x";
        r13 = 2;
        r13 = new java.lang.Object[r13];
        r14 = java.lang.Integer.valueOf(r23);
        r17 = 0;
        r13[r17] = r14;
        r14 = 1669485411; // 0x63825363 float:4.808171E21 double:8.24835388E-315;
        r14 = java.lang.Integer.valueOf(r14);
        r17 = 1;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x02a4:
        r58 = 1;
    L_0x02a6:
        r10 = r72.position();
        r11 = r72.limit();
        if (r10 >= r11) goto L_0x0412;
    L_0x02b0:
        if (r58 == 0) goto L_0x0412;
    L_0x02b2:
        r60 = r72.get();
        r10 = -1;
        r0 = r60;
        if (r0 != r10) goto L_0x02be;
    L_0x02bb:
        r58 = 0;
        goto L_0x02a6;
    L_0x02be:
        if (r60 == 0) goto L_0x02a6;
    L_0x02c0:
        r10 = r72.get();	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r10 & 255;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r59 = r0;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = 0;	 Catch:{ BufferUnderflowException -> 0x0315 }
        switch(r60) {
            case 1: goto L_0x02de;
            case 3: goto L_0x032c;
            case 6: goto L_0x0340;
            case 12: goto L_0x0354;
            case 15: goto L_0x036c;
            case 26: goto L_0x0360;
            case 28: goto L_0x0379;
            case 43: goto L_0x0405;
            case 50: goto L_0x0381;
            case 51: goto L_0x0389;
            case 53: goto L_0x0395;
            case 54: goto L_0x039d;
            case 55: goto L_0x03a5;
            case 56: goto L_0x03b6;
            case 57: goto L_0x03c3;
            case 58: goto L_0x03cf;
            case 59: goto L_0x03db;
            case 60: goto L_0x03e7;
            case 61: goto L_0x03f4;
            default: goto L_0x02cd;
        };	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x02cd:
        r36 = 0;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x02cf:
        r0 = r36;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        if (r0 >= r1) goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x02d5:
        r30 = r30 + 1;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r64 = r72.get();	 Catch:{ BufferUnderflowException -> 0x0315 }
        r36 = r36 + 1;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02cf;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x02de:
        r57 = readIpAddress(r72);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = 4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x02e4:
        r0 = r30;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        if (r0 == r1) goto L_0x02a6;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x02ea:
        r10 = android.net.metrics.DhcpErrorEvent.DHCP_INVALID_OPTION_LENGTH;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r60;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r28 = android.net.metrics.DhcpErrorEvent.errorCodeWithOption(r10, r0);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = new android.net.dhcp.DhcpPacket$ParseException;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r11 = "Invalid length %d for option %d, expected %d";	 Catch:{ BufferUnderflowException -> 0x0315 }
        r12 = 3;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r12 = new java.lang.Object[r12];	 Catch:{ BufferUnderflowException -> 0x0315 }
        r13 = java.lang.Integer.valueOf(r59);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r14 = 0;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r12[r14] = r13;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r13 = java.lang.Byte.valueOf(r60);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r14 = 1;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r12[r14] = r13;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r13 = java.lang.Integer.valueOf(r30);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r14 = 2;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r12[r14] = r13;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r28;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10.<init>(r0, r11, r12);	 Catch:{ BufferUnderflowException -> 0x0315 }
        throw r10;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0315:
        r27 = move-exception;
        r10 = android.net.metrics.DhcpErrorEvent.BUFFER_UNDERFLOW;
        r0 = r60;
        r28 = android.net.metrics.DhcpErrorEvent.errorCodeWithOption(r10, r0);
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = "BufferUnderflowException";
        r12 = 0;
        r12 = new java.lang.Object[r12];
        r0 = r28;
        r10.<init>(r0, r11, r12);
        throw r10;
    L_0x032c:
        r30 = 0;
    L_0x032e:
        r0 = r30;
        r1 = r59;
        if (r0 >= r1) goto L_0x02e4;
    L_0x0334:
        r10 = readIpAddress(r72);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r32;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0.add(r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = r30 + 4;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x032e;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0340:
        r30 = 0;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0342:
        r0 = r30;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        if (r0 >= r1) goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0348:
        r10 = readIpAddress(r72);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r25;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0.add(r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = r30 + 4;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x0342;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0354:
        r30 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = 0;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r72;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r34 = readAsciiString(r0, r1, r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0360:
        r30 = 2;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = r72.getShort();	 Catch:{ BufferUnderflowException -> 0x0315 }
        r56 = java.lang.Short.valueOf(r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x036c:
        r30 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = 0;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r72;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r26 = readAsciiString(r0, r1, r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0379:
        r21 = readIpAddress(r72);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = 4;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0381:
        r62 = readIpAddress(r72);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = 4;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0389:
        r10 = r72.getInt();	 Catch:{ BufferUnderflowException -> 0x0315 }
        r53 = java.lang.Integer.valueOf(r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = 4;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0395:
        r24 = r72.get();	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = 1;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x039d:
        r63 = readIpAddress(r72);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = 4;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x03a5:
        r0 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = new byte[r0];	 Catch:{ BufferUnderflowException -> 0x0315 }
        r31 = r0;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r72;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r31;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0.get(r1);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x03b6:
        r30 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = 0;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r72;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r55 = readAsciiString(r0, r1, r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x03c3:
        r30 = 2;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = r72.getShort();	 Catch:{ BufferUnderflowException -> 0x0315 }
        r54 = java.lang.Short.valueOf(r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x03cf:
        r30 = 4;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = r72.getInt();	 Catch:{ BufferUnderflowException -> 0x0315 }
        r18 = java.lang.Integer.valueOf(r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x03db:
        r30 = 4;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = r72.getInt();	 Catch:{ BufferUnderflowException -> 0x0315 }
        r19 = java.lang.Integer.valueOf(r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x03e7:
        r30 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = 1;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r72;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r70 = readAsciiString(r0, r1, r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x03f4:
        r0 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = new byte[r0];	 Catch:{ BufferUnderflowException -> 0x0315 }
        r37 = r0;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r72;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r37;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0.get(r1);	 Catch:{ BufferUnderflowException -> 0x0315 }
        r30 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;	 Catch:{ BufferUnderflowException -> 0x0315 }
    L_0x0405:
        r30 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r10 = 1;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r0 = r72;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r1 = r59;	 Catch:{ BufferUnderflowException -> 0x0315 }
        r71 = readAsciiString(r0, r1, r10);	 Catch:{ BufferUnderflowException -> 0x0315 }
        goto L_0x02e4;
    L_0x0412:
        switch(r24) {
            case -1: goto L_0x042b;
            case 0: goto L_0x0415;
            case 1: goto L_0x0439;
            case 2: goto L_0x0483;
            case 3: goto L_0x0489;
            case 4: goto L_0x0495;
            case 5: goto L_0x04a2;
            case 6: goto L_0x04a8;
            case 7: goto L_0x0415;
            case 8: goto L_0x04b5;
            default: goto L_0x0415;
        };
    L_0x0415:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.DHCP_UNKNOWN_MSG_TYPE;
        r12 = "Unimplemented DHCP type %d";
        r13 = 1;
        r13 = new java.lang.Object[r13];
        r14 = java.lang.Byte.valueOf(r24);
        r17 = 0;
        r13[r17] = r14;
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x042b:
        r10 = new android.net.dhcp.DhcpPacket$ParseException;
        r11 = android.net.metrics.DhcpErrorEvent.DHCP_NO_MSG_TYPE;
        r12 = "No DHCP message type option";
        r13 = 0;
        r13 = new java.lang.Object[r13];
        r10.<init>(r11, r12, r13);
        throw r10;
    L_0x0439:
        r2 = new android.net.dhcp.DhcpDiscoverPacket;
        r2.<init>(r3, r4, r9, r5);
    L_0x043e:
        r0 = r21;
        r2.mBroadcastAddress = r0;
        r0 = r25;
        r2.mDnsServers = r0;
        r0 = r26;
        r2.mDomainName = r0;
        r0 = r32;
        r2.mGateways = r0;
        r0 = r34;
        r2.mHostName = r0;
        r0 = r53;
        r2.mLeaseTime = r0;
        r0 = r55;
        r2.mMessage = r0;
        r0 = r56;
        r2.mMtu = r0;
        r0 = r62;
        r2.mRequestedIp = r0;
        r0 = r31;
        r2.mRequestedParams = r0;
        r0 = r63;
        r2.mServerIdentifier = r0;
        r0 = r57;
        r2.mSubnetMask = r0;
        r0 = r54;
        r2.mMaxMessageSize = r0;
        r0 = r18;
        r2.mT1 = r0;
        r0 = r19;
        r2.mT2 = r0;
        r0 = r70;
        r2.mVendorId = r0;
        r0 = r71;
        r2.mVendorInfo = r0;
        return r2;
    L_0x0483:
        r2 = new android.net.dhcp.DhcpOfferPacket;
        r2.<init>(r3, r4, r5, r6, r7, r8, r9);
        goto L_0x043e;
    L_0x0489:
        r2 = new android.net.dhcp.DhcpRequestPacket;
        r10 = r2;
        r11 = r3;
        r12 = r4;
        r13 = r7;
        r14 = r9;
        r15 = r5;
        r10.<init>(r11, r12, r13, r14, r15);
        goto L_0x043e;
    L_0x0495:
        r2 = new android.net.dhcp.DhcpDeclinePacket;
        r10 = r2;
        r11 = r3;
        r12 = r4;
        r13 = r7;
        r14 = r8;
        r17 = r9;
        r10.<init>(r11, r12, r13, r14, r15, r16, r17);
        goto L_0x043e;
    L_0x04a2:
        r2 = new android.net.dhcp.DhcpAckPacket;
        r2.<init>(r3, r4, r5, r6, r7, r8, r9);
        goto L_0x043e;
    L_0x04a8:
        r2 = new android.net.dhcp.DhcpNakPacket;
        r10 = r2;
        r11 = r3;
        r12 = r4;
        r13 = r7;
        r14 = r8;
        r17 = r9;
        r10.<init>(r11, r12, r13, r14, r15, r16, r17);
        goto L_0x043e;
    L_0x04b5:
        r2 = new android.net.dhcp.DhcpInformPacket;
        r10 = r2;
        r11 = r3;
        r12 = r4;
        r13 = r7;
        r14 = r8;
        r17 = r9;
        r10.<init>(r11, r12, r13, r14, r15, r16, r17);
        goto L_0x043e;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.net.dhcp.DhcpPacket.decodeFullPacket(java.nio.ByteBuffer, int):android.net.dhcp.DhcpPacket");
    }

    public static DhcpPacket decodeFullPacket(byte[] packet, int length, int pktType) throws ParseException {
        try {
            return decodeFullPacket(ByteBuffer.wrap(packet, 0, length).order(ByteOrder.BIG_ENDIAN), pktType);
        } catch (ParseException e) {
            throw e;
        } catch (Exception e2) {
            throw new ParseException(DhcpErrorEvent.PARSING_ERROR, e2.getMessage(), new Object[0]);
        }
    }

    public DhcpResults toDhcpResults() {
        int prefixLength;
        Inet4Address ipAddress = this.mYourIp;
        if (ipAddress.equals(Inet4Address.ANY)) {
            ipAddress = this.mClientIp;
            if (ipAddress.equals(Inet4Address.ANY)) {
                return null;
            }
        }
        if (this.mSubnetMask != null) {
            try {
                prefixLength = NetworkUtils.netmaskToPrefixLength(this.mSubnetMask);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        prefixLength = NetworkUtils.getImplicitNetmask(ipAddress);
        DhcpResults results = new DhcpResults();
        try {
            int i;
            results.ipAddress = new LinkAddress(ipAddress, prefixLength);
            if (this.mGateways.size() > 0) {
                results.gateway = (InetAddress) this.mGateways.get(0);
            }
            results.dnsServers.addAll(this.mDnsServers);
            results.domains = this.mDomainName;
            results.serverAddress = this.mServerIdentifier;
            results.vendorInfo = this.mVendorInfo;
            results.leaseDuration = this.mLeaseTime != null ? this.mLeaseTime.intValue() : -1;
            if (this.mMtu == null || (short) 1280 > this.mMtu.shortValue() || this.mMtu.shortValue() > (short) 1500) {
                i = 0;
            } else {
                i = this.mMtu.shortValue();
            }
            results.mtu = i;
            return results;
        } catch (IllegalArgumentException e2) {
            return null;
        }
    }

    public long getLeaseTimeMillis() {
        if (this.mLeaseTime == null || this.mLeaseTime.intValue() == -1) {
            return 0;
        }
        if (this.mLeaseTime.intValue() < 0 || this.mLeaseTime.intValue() >= 60) {
            return (((long) this.mLeaseTime.intValue()) & 4294967295L) * 1000;
        }
        return 60000;
    }

    public static ByteBuffer buildDiscoverPacket(int encap, int transactionId, short secs, byte[] clientMac, boolean broadcast, byte[] expectedParams) {
        return buildDiscoverPacket(encap, transactionId, secs, clientMac, broadcast, expectedParams, false);
    }

    public static ByteBuffer buildDiscoverPacket(int encap, int transactionId, short secs, byte[] clientMac, boolean broadcast, byte[] expectedParams, boolean rapidCommit) {
        DhcpPacket pkt = new DhcpDiscoverPacket(transactionId, secs, clientMac, broadcast, rapidCommit);
        pkt.mRequestedParams = expectedParams;
        return pkt.buildPacket(encap, DHCP_SERVER, (short) 68);
    }

    public static ByteBuffer buildOfferPacket(int encap, int transactionId, boolean broadcast, Inet4Address serverIpAddr, Inet4Address clientIpAddr, byte[] mac, Integer timeout, Inet4Address netMask, Inet4Address bcAddr, List<Inet4Address> gateways, List<Inet4Address> dnsServers, Inet4Address dhcpServerIdentifier, String domainName) {
        DhcpPacket pkt = new DhcpOfferPacket(transactionId, (short) 0, broadcast, serverIpAddr, INADDR_ANY, clientIpAddr, mac);
        pkt.mGateways = gateways;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mSubnetMask = netMask;
        pkt.mBroadcastAddress = bcAddr;
        return pkt.buildPacket(encap, (short) 68, DHCP_SERVER);
    }

    public static ByteBuffer buildAckPacket(int encap, int transactionId, boolean broadcast, Inet4Address serverIpAddr, Inet4Address clientIpAddr, byte[] mac, Integer timeout, Inet4Address netMask, Inet4Address bcAddr, List<Inet4Address> gateways, List<Inet4Address> dnsServers, Inet4Address dhcpServerIdentifier, String domainName) {
        DhcpPacket pkt = new DhcpAckPacket(transactionId, (short) 0, broadcast, serverIpAddr, INADDR_ANY, clientIpAddr, mac);
        pkt.mGateways = gateways;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mSubnetMask = netMask;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mBroadcastAddress = bcAddr;
        return pkt.buildPacket(encap, (short) 68, DHCP_SERVER);
    }

    public static ByteBuffer buildNakPacket(int encap, int transactionId, Inet4Address serverIpAddr, Inet4Address clientIpAddr, byte[] mac) {
        DhcpPacket pkt = new DhcpNakPacket(transactionId, (short) 0, clientIpAddr, serverIpAddr, serverIpAddr, serverIpAddr, mac);
        pkt.mMessage = "requested address not available";
        pkt.mRequestedIp = clientIpAddr;
        return pkt.buildPacket(encap, (short) 68, DHCP_SERVER);
    }

    public static ByteBuffer buildRequestPacket(int encap, int transactionId, short secs, Inet4Address clientIp, boolean broadcast, byte[] clientMac, Inet4Address requestedIpAddress, Inet4Address serverIdentifier, byte[] requestedParams, String hostName) {
        DhcpPacket pkt = new DhcpRequestPacket(transactionId, secs, clientIp, clientMac, broadcast);
        pkt.mRequestedIp = requestedIpAddress;
        pkt.mServerIdentifier = serverIdentifier;
        pkt.mHostName = hostName;
        pkt.mRequestedParams = requestedParams;
        return pkt.buildPacket(encap, DHCP_SERVER, (short) 68);
    }
}
