package sun.nio.ch;

import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jdk.net.ExtendedSocketOptions;
import sun.net.ExtendedOptionsImpl;
import sun.net.ResourceManager;

class DatagramChannelImpl extends DatagramChannel implements SelChImpl {
    static final /* synthetic */ boolean -assertionsDisabled = (DatagramChannelImpl.class.desiredAssertionStatus() ^ 1);
    private static final int ST_CONNECTED = 1;
    private static final int ST_KILLED = 2;
    private static final int ST_UNCONNECTED = 0;
    private static final int ST_UNINITIALIZED = -1;
    private static NativeDispatcher nd = new DatagramDispatcher();
    private InetAddress cachedSenderInetAddress;
    private int cachedSenderPort;
    private final ProtocolFamily family;
    final FileDescriptor fd;
    private final int fdVal;
    private final CloseGuard guard = CloseGuard.get();
    private boolean isReuseAddress;
    private InetSocketAddress localAddress;
    private final Object readLock = new Object();
    private volatile long readerThread = 0;
    private MembershipRegistry registry;
    private InetSocketAddress remoteAddress;
    private boolean reuseAddressEmulated;
    private SocketAddress sender;
    private DatagramSocket socket;
    private int state = -1;
    private final Object stateLock = new Object();
    private final Object writeLock = new Object();
    private volatile long writerThread = 0;

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private DefaultOptionsHolder() {
        }

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet(8);
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            set.add(StandardSocketOptions.SO_BROADCAST);
            set.add(StandardSocketOptions.IP_TOS);
            set.add(StandardSocketOptions.IP_MULTICAST_IF);
            set.add(StandardSocketOptions.IP_MULTICAST_TTL);
            set.add(StandardSocketOptions.IP_MULTICAST_LOOP);
            if (ExtendedOptionsImpl.flowSupported()) {
                set.add(ExtendedSocketOptions.SO_FLOW_SLA);
            }
            return Collections.unmodifiableSet(set);
        }
    }

    private static native void disconnect0(FileDescriptor fileDescriptor, boolean z) throws IOException;

    private static native void initIDs();

    private native int receive0(FileDescriptor fileDescriptor, long j, int i, boolean z) throws IOException;

    private native int send0(boolean z, FileDescriptor fileDescriptor, long j, int i, InetAddress inetAddress, int i2) throws IOException;

    static {
        initIDs();
    }

    public DatagramChannelImpl(SelectorProvider sp) throws IOException {
        super(sp);
        ResourceManager.beforeUdpCreate();
        try {
            this.family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
            this.fd = Net.socket(this.family, -assertionsDisabled);
            this.fdVal = IOUtil.fdVal(this.fd);
            this.state = 0;
            if (this.fd != null && this.fd.valid()) {
                this.guard.open("close");
            }
        } catch (IOException ioe) {
            ResourceManager.afterUdpClose();
            throw ioe;
        }
    }

    public DatagramChannelImpl(SelectorProvider sp, ProtocolFamily family) throws IOException {
        super(sp);
        if (family == StandardProtocolFamily.INET || family == StandardProtocolFamily.INET6) {
            if (family != StandardProtocolFamily.INET6 || Net.isIPv6Available()) {
                this.family = family;
                this.fd = Net.socket(family, -assertionsDisabled);
                this.fdVal = IOUtil.fdVal(this.fd);
                this.state = 0;
                if (this.fd != null && this.fd.valid()) {
                    this.guard.open("close");
                    return;
                }
                return;
            }
            throw new UnsupportedOperationException("IPv6 not available");
        } else if (family == null) {
            throw new NullPointerException("'family' is null");
        } else {
            throw new UnsupportedOperationException("Protocol family not supported");
        }
    }

    public DatagramChannelImpl(SelectorProvider sp, FileDescriptor fd) throws IOException {
        super(sp);
        this.family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
        this.fd = fd;
        this.fdVal = IOUtil.fdVal(fd);
        this.state = 0;
        this.localAddress = Net.localAddress(fd);
        if (fd != null && fd.valid()) {
            this.guard.open("close");
        }
    }

    public DatagramSocket socket() {
        DatagramSocket datagramSocket;
        synchronized (this.stateLock) {
            if (this.socket == null) {
                this.socket = DatagramSocketAdaptor.create(this);
            }
            datagramSocket = this.socket;
        }
        return datagramSocket;
    }

    public SocketAddress getLocalAddress() throws IOException {
        SocketAddress revealedLocalAddress;
        synchronized (this.stateLock) {
            if (isOpen()) {
                revealedLocalAddress = Net.getRevealedLocalAddress(this.localAddress);
            } else {
                throw new ClosedChannelException();
            }
        }
        return revealedLocalAddress;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        SocketAddress socketAddress;
        synchronized (this.stateLock) {
            if (isOpen()) {
                socketAddress = this.remoteAddress;
            } else {
                throw new ClosedChannelException();
            }
        }
        return socketAddress;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        } else if (supportedOptions().contains(name)) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (!(name == StandardSocketOptions.IP_TOS || name == StandardSocketOptions.IP_MULTICAST_TTL)) {
                    if (name != StandardSocketOptions.IP_MULTICAST_LOOP) {
                        if (name != StandardSocketOptions.IP_MULTICAST_IF) {
                            if (name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind() && this.localAddress != null) {
                                this.reuseAddressEmulated = true;
                                this.isReuseAddress = ((Boolean) value).booleanValue();
                            }
                            Net.setSocketOption(this.fd, Net.UNSPEC, name, value);
                            return this;
                        } else if (value == null) {
                            throw new IllegalArgumentException("Cannot set IP_MULTICAST_IF to 'null'");
                        } else {
                            NetworkInterface interf = (NetworkInterface) value;
                            if (this.family == StandardProtocolFamily.INET6) {
                                int index = interf.getIndex();
                                if (index == -1) {
                                    throw new IOException("Network interface cannot be identified");
                                }
                                Net.setInterface6(this.fd, index);
                            } else {
                                Inet4Address target = Net.anyInet4Address(interf);
                                if (target == null) {
                                    throw new IOException("Network interface not configured for IPv4");
                                }
                                Net.setInterface4(this.fd, Net.inet4AsInt(target));
                            }
                        }
                    }
                }
                Net.setSocketOption(this.fd, this.family, name, value);
                return this;
            }
        } else {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
    }

    public <T> T getOption(SocketOption<T> name) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        } else if (supportedOptions().contains(name)) {
            synchronized (this.stateLock) {
                T valueOf;
                ensureOpen();
                if (!(name == StandardSocketOptions.IP_TOS || name == StandardSocketOptions.IP_MULTICAST_TTL)) {
                    if (name != StandardSocketOptions.IP_MULTICAST_LOOP) {
                        if (name == StandardSocketOptions.IP_MULTICAST_IF) {
                            NetworkInterface ni;
                            if (this.family == StandardProtocolFamily.INET) {
                                int address = Net.getInterface4(this.fd);
                                if (address == 0) {
                                    return null;
                                }
                                ni = NetworkInterface.getByInetAddress(Net.inet4FromInt(address));
                                if (ni == null) {
                                    throw new IOException("Unable to map address to interface");
                                }
                                return ni;
                            }
                            int index = Net.getInterface6(this.fd);
                            if (index == 0) {
                                return null;
                            }
                            ni = NetworkInterface.getByIndex(index);
                            if (ni == null) {
                                throw new IOException("Unable to map index to interface");
                            }
                            return ni;
                        } else if (name == StandardSocketOptions.SO_REUSEADDR && this.reuseAddressEmulated) {
                            valueOf = Boolean.valueOf(this.isReuseAddress);
                            return valueOf;
                        } else {
                            valueOf = Net.getSocketOption(this.fd, Net.UNSPEC, name);
                            return valueOf;
                        }
                    }
                }
                valueOf = Net.getSocketOption(this.fd, this.family, name);
                return valueOf;
            }
        } else {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
    }

    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SocketAddress receive(ByteBuffer dst) throws IOException {
        boolean z = true;
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        } else if (dst == null) {
            throw new NullPointerException();
        } else if (this.localAddress == null) {
            return null;
        } else {
            synchronized (this.readLock) {
                ensureOpen();
                int n = 0;
                ByteBuffer byteBuffer = null;
                try {
                    begin();
                    if (isOpen()) {
                        SecurityManager security = System.getSecurityManager();
                        this.readerThread = NativeThread.current();
                        if (isConnected() || security == null) {
                            do {
                                n = receive(this.fd, dst);
                                if (n != -3) {
                                    break;
                                }
                            } while (isOpen());
                            if (n == -2) {
                                this.readerThread = 0;
                                if (n <= 0 && n != -2) {
                                    z = -assertionsDisabled;
                                }
                                end(z);
                                if (-assertionsDisabled || IOStatus.check(n)) {
                                } else {
                                    throw new AssertionError();
                                }
                            }
                        }
                        byteBuffer = Util.getTemporaryDirectBuffer(dst.remaining());
                        while (true) {
                            n = receive(this.fd, byteBuffer);
                            if (n != -3 || !isOpen()) {
                                if (n != -2) {
                                    InetSocketAddress isa = this.sender;
                                    security.checkAccept(isa.getAddress().getHostAddress(), isa.getPort());
                                    break;
                                }
                                break;
                            }
                        }
                        SocketAddress socketAddress = this.sender;
                        if (byteBuffer != null) {
                            Util.releaseTemporaryDirectBuffer(byteBuffer);
                        }
                        this.readerThread = 0;
                        if (n <= 0 && n != -2) {
                            z = -assertionsDisabled;
                        }
                        end(z);
                        if (-assertionsDisabled || IOStatus.check(n)) {
                        } else {
                            throw new AssertionError();
                        }
                    }
                    this.readerThread = 0;
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(0)) {
                    } else {
                        throw new AssertionError();
                    }
                } catch (SecurityException e) {
                    byteBuffer.clear();
                } catch (Throwable th) {
                    if (byteBuffer != null) {
                        Util.releaseTemporaryDirectBuffer(byteBuffer);
                    }
                    this.readerThread = 0;
                    if (n <= 0 && n != -2) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (!-assertionsDisabled && !IOStatus.check(n)) {
                        AssertionError assertionError = new AssertionError();
                    }
                }
            }
        }
    }

    private int receive(FileDescriptor fd, ByteBuffer dst) throws IOException {
        int pos = dst.position();
        int lim = dst.limit();
        if (-assertionsDisabled || pos <= lim) {
            int rem = pos <= lim ? lim - pos : 0;
            if ((dst instanceof DirectBuffer) && rem > 0) {
                return receiveIntoNativeBuffer(fd, dst, rem, pos);
            }
            int newSize = Math.max(rem, 1);
            ByteBuffer bb = Util.getTemporaryDirectBuffer(newSize);
            try {
                BlockGuard.getThreadPolicy().onNetwork();
                int n = receiveIntoNativeBuffer(fd, bb, newSize, 0);
                bb.flip();
                if (n > 0 && rem > 0) {
                    dst.put(bb);
                }
                Util.releaseTemporaryDirectBuffer(bb);
                return n;
            } catch (Throwable th) {
                Util.releaseTemporaryDirectBuffer(bb);
            }
        } else {
            throw new AssertionError();
        }
    }

    private int receiveIntoNativeBuffer(FileDescriptor fd, ByteBuffer bb, int rem, int pos) throws IOException {
        FileDescriptor fileDescriptor = fd;
        int i = rem;
        int n = receive0(fileDescriptor, ((long) pos) + ((DirectBuffer) bb).address(), i, isConnected());
        if (n > 0) {
            bb.position(pos + n);
        }
        return n;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        boolean z = true;
        if (src == null) {
            throw new NullPointerException();
        }
        synchronized (this.writeLock) {
            ensureOpen();
            InetSocketAddress isa = Net.checkAddress(target);
            InetAddress ia = isa.getAddress();
            if (ia == null) {
                throw new IOException("Target address not resolved");
            }
            synchronized (this.stateLock) {
                if (isConnected()) {
                    if (target.equals(this.remoteAddress)) {
                        int write = write(src);
                        return write;
                    }
                    throw new IllegalArgumentException("Connected address not equal to target address");
                } else if (target == null) {
                    throw new NullPointerException();
                } else {
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        if (ia.isMulticastAddress()) {
                            sm.checkMulticast(ia);
                        } else {
                            sm.checkConnect(ia.getHostAddress(), isa.getPort());
                        }
                    }
                }
            }
        }
    }

    private int send(FileDescriptor fd, ByteBuffer src, InetSocketAddress target) throws IOException {
        if (src instanceof DirectBuffer) {
            return sendFromNativeBuffer(fd, src, target);
        }
        int pos = src.position();
        int lim = src.limit();
        if (-assertionsDisabled || pos <= lim) {
            ByteBuffer bb = Util.getTemporaryDirectBuffer(pos <= lim ? lim - pos : 0);
            try {
                bb.put(src);
                bb.flip();
                src.position(pos);
                int n = sendFromNativeBuffer(fd, bb, target);
                if (n > 0) {
                    src.position(pos + n);
                }
                Util.releaseTemporaryDirectBuffer(bb);
                return n;
            } catch (Throwable th) {
                Util.releaseTemporaryDirectBuffer(bb);
            }
        } else {
            throw new AssertionError();
        }
    }

    private int sendFromNativeBuffer(FileDescriptor fd, ByteBuffer bb, InetSocketAddress target) throws IOException {
        int pos = bb.position();
        int lim = bb.limit();
        if (-assertionsDisabled || pos <= lim) {
            int written;
            int rem = pos <= lim ? lim - pos : 0;
            try {
                written = send0(this.family != StandardProtocolFamily.INET ? true : -assertionsDisabled, fd, ((DirectBuffer) bb).address() + ((long) pos), rem, target.getAddress(), target.getPort());
            } catch (PortUnreachableException pue) {
                if (isConnected()) {
                    throw pue;
                }
                written = rem;
            }
            if (written > 0) {
                bb.position(pos + written);
            }
            return written;
        }
        throw new AssertionError();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read(ByteBuffer buf) throws IOException {
        boolean z = true;
        if (buf == null) {
            throw new NullPointerException();
        }
        synchronized (this.readLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (isConnected()) {
                } else {
                    throw new NotYetConnectedException();
                }
            }
            try {
                begin();
                if (isOpen()) {
                    int n;
                    this.readerThread = NativeThread.current();
                    while (true) {
                        n = IOUtil.read(this.fd, buf, -1, nd);
                        if (n == -3) {
                            if (!isOpen()) {
                                break;
                            }
                        }
                        break;
                    }
                    int normalize = IOStatus.normalize(n);
                    this.readerThread = 0;
                    if (n <= 0 && n != -2) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(n)) {
                    } else {
                        throw new AssertionError();
                    }
                }
                this.readerThread = 0;
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(0)) {
                } else {
                    throw new AssertionError();
                }
            } finally {
                this.readerThread = 0;
                if (null <= null && 0 != -2) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(0)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > dsts.length - length) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.readLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (isConnected()) {
                } else {
                    throw new NotYetConnectedException();
                }
            }
            try {
                begin();
                long isOpen = isOpen();
                if (isOpen == null) {
                    this.readerThread = isOpen;
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(0)) {
                        return 0;
                    }
                    throw new AssertionError();
                }
                long n;
                this.readerThread = NativeThread.current();
                while (true) {
                    n = IOUtil.read(this.fd, dsts, offset, length, nd);
                    if (n == -3) {
                        if (!isOpen()) {
                            break;
                        }
                    }
                    break;
                }
                long normalize = IOStatus.normalize(n);
                this.readerThread = 0;
                boolean z = (n > 0 || n == -2) ? true : -assertionsDisabled;
                end(z);
                if (-assertionsDisabled || IOStatus.check(n)) {
                } else {
                    throw new AssertionError();
                }
            } finally {
                this.readerThread = 0;
                boolean z2 = (0 > 0 || 0 == -2) ? true : -assertionsDisabled;
                end(z2);
                if (!-assertionsDisabled && !IOStatus.check(0)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int write(ByteBuffer buf) throws IOException {
        boolean z = true;
        if (buf == null) {
            throw new NullPointerException();
        }
        synchronized (this.writeLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (isConnected()) {
                } else {
                    throw new NotYetConnectedException();
                }
            }
            try {
                begin();
                if (isOpen()) {
                    int n;
                    this.writerThread = NativeThread.current();
                    while (true) {
                        n = IOUtil.write(this.fd, buf, -1, nd);
                        if (n == -3) {
                            if (!isOpen()) {
                                break;
                            }
                        }
                        break;
                    }
                    int normalize = IOStatus.normalize(n);
                    this.writerThread = 0;
                    if (n <= 0 && n != -2) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(n)) {
                    } else {
                        throw new AssertionError();
                    }
                }
                this.writerThread = 0;
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(0)) {
                } else {
                    throw new AssertionError();
                }
            } finally {
                this.writerThread = 0;
                if (null <= null && 0 != -2) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(0)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > srcs.length - length) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.writeLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (isConnected()) {
                } else {
                    throw new NotYetConnectedException();
                }
            }
            try {
                begin();
                long isOpen = isOpen();
                if (isOpen == null) {
                    this.writerThread = isOpen;
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(0)) {
                        return 0;
                    }
                    throw new AssertionError();
                }
                long n;
                this.writerThread = NativeThread.current();
                while (true) {
                    n = IOUtil.write(this.fd, srcs, offset, length, nd);
                    if (n == -3) {
                        if (!isOpen()) {
                            break;
                        }
                    }
                    break;
                }
                long normalize = IOStatus.normalize(n);
                this.writerThread = 0;
                boolean z = (n > 0 || n == -2) ? true : -assertionsDisabled;
                end(z);
                if (-assertionsDisabled || IOStatus.check(n)) {
                } else {
                    throw new AssertionError();
                }
            } finally {
                this.writerThread = 0;
                boolean z2 = (0 > 0 || 0 == -2) ? true : -assertionsDisabled;
                end(z2);
                if (!-assertionsDisabled && !IOStatus.check(0)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        IOUtil.configureBlocking(this.fd, block);
    }

    public SocketAddress localAddress() {
        SocketAddress socketAddress;
        synchronized (this.stateLock) {
            socketAddress = this.localAddress;
        }
        return socketAddress;
    }

    public SocketAddress remoteAddress() {
        SocketAddress socketAddress;
        synchronized (this.stateLock) {
            socketAddress = this.remoteAddress;
        }
        return socketAddress;
    }

    public DatagramChannel bind(SocketAddress local) throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    ensureOpen();
                    if (this.localAddress != null) {
                        throw new AlreadyBoundException();
                    }
                    InetSocketAddress isa;
                    if (local == null) {
                        isa = this.family == StandardProtocolFamily.INET ? new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0) : new InetSocketAddress(0);
                    } else {
                        isa = Net.checkAddress(local);
                        if (this.family == StandardProtocolFamily.INET && !(isa.getAddress() instanceof Inet4Address)) {
                            throw new UnsupportedAddressTypeException();
                        }
                    }
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        sm.checkListen(isa.getPort());
                    }
                    Net.bind(this.family, this.fd, isa.getAddress(), isa.getPort());
                    this.localAddress = Net.localAddress(this.fd);
                }
            }
        }
        return this;
    }

    public boolean isConnected() {
        boolean z = true;
        synchronized (this.stateLock) {
            if (this.state != 1) {
                z = -assertionsDisabled;
            }
        }
        return z;
    }

    void ensureOpenAndUnconnected() throws IOException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            } else if (this.state != 0) {
                throw new IllegalStateException("Connect already invoked");
            }
        }
    }

    public DatagramChannel connect(SocketAddress sa) throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    ensureOpenAndUnconnected();
                    InetSocketAddress isa = Net.checkAddress(sa);
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
                    }
                    if (Net.connect(this.family, this.fd, isa.getAddress(), isa.getPort()) <= 0) {
                        throw new Error();
                    }
                    this.state = 1;
                    this.remoteAddress = isa;
                    this.sender = isa;
                    this.cachedSenderInetAddress = isa.getAddress();
                    this.cachedSenderPort = isa.getPort();
                    this.localAddress = Net.localAddress(this.fd);
                    synchronized (blockingLock()) {
                        try {
                            boolean blocking = isBlocking();
                            ByteBuffer tmpBuf = ByteBuffer.allocate(1);
                            if (blocking) {
                                configureBlocking(-assertionsDisabled);
                            }
                            do {
                                tmpBuf.clear();
                            } while (receive(tmpBuf) != null);
                            if (blocking) {
                                configureBlocking(true);
                            }
                        } catch (Throwable th) {
                            if (null != null) {
                                configureBlocking(true);
                            }
                        }
                    }
                }
            }
        }
        return this;
    }

    public DatagramChannel disconnect() throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    if (isConnected() && (isOpen() ^ 1) == 0) {
                        InetSocketAddress isa = this.remoteAddress;
                        SecurityManager sm = System.getSecurityManager();
                        if (sm != null) {
                            sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
                        }
                        disconnect0(this.fd, this.family == StandardProtocolFamily.INET6 ? true : -assertionsDisabled);
                        this.remoteAddress = null;
                        this.state = 0;
                        this.localAddress = Net.localAddress(this.fd);
                        return this;
                    }
                    return this;
                }
            }
        }
    }

    private java.nio.channels.MembershipKey innerJoin(java.net.InetAddress r25, java.net.NetworkInterface r26, java.net.InetAddress r27) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r3_2 'key' sun.nio.ch.MembershipKeyImpl) in PHI: PHI: (r3_1 'key' sun.nio.ch.MembershipKeyImpl) = (r3_0 'key' sun.nio.ch.MembershipKeyImpl), (r3_2 'key' sun.nio.ch.MembershipKeyImpl) binds: {(r3_0 'key' sun.nio.ch.MembershipKeyImpl)=B:73:0x0108, (r3_2 'key' sun.nio.ch.MembershipKeyImpl)=B:90:0x0157}
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
        r24 = this;
        r4 = r25.isMulticastAddress();
        if (r4 != 0) goto L_0x000f;
    L_0x0006:
        r4 = new java.lang.IllegalArgumentException;
        r5 = "Group not a multicast address";
        r4.<init>(r5);
        throw r4;
    L_0x000f:
        r0 = r25;
        r4 = r0 instanceof java.net.Inet4Address;
        if (r4 == 0) goto L_0x002e;
    L_0x0015:
        r0 = r24;
        r4 = r0.family;
        r5 = java.net.StandardProtocolFamily.INET6;
        if (r4 != r5) goto L_0x004e;
    L_0x001d:
        r4 = sun.nio.ch.Net.canIPv6SocketJoinIPv4Group();
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x004e;
    L_0x0025:
        r4 = new java.lang.IllegalArgumentException;
        r5 = "IPv6 socket cannot join IPv4 multicast group";
        r4.<init>(r5);
        throw r4;
    L_0x002e:
        r0 = r25;
        r4 = r0 instanceof java.net.Inet6Address;
        if (r4 == 0) goto L_0x0045;
    L_0x0034:
        r0 = r24;
        r4 = r0.family;
        r5 = java.net.StandardProtocolFamily.INET6;
        if (r4 == r5) goto L_0x004e;
    L_0x003c:
        r4 = new java.lang.IllegalArgumentException;
        r5 = "Only IPv6 sockets can join IPv6 multicast group";
        r4.<init>(r5);
        throw r4;
    L_0x0045:
        r4 = new java.lang.IllegalArgumentException;
        r5 = "Address type not supported";
        r4.<init>(r5);
        throw r4;
    L_0x004e:
        if (r27 == 0) goto L_0x0081;
    L_0x0050:
        r4 = r27.isAnyLocalAddress();
        if (r4 == 0) goto L_0x005f;
    L_0x0056:
        r4 = new java.lang.IllegalArgumentException;
        r5 = "Source address is a wildcard address";
        r4.<init>(r5);
        throw r4;
    L_0x005f:
        r4 = r27.isMulticastAddress();
        if (r4 == 0) goto L_0x006e;
    L_0x0065:
        r4 = new java.lang.IllegalArgumentException;
        r5 = "Source address is multicast address";
        r4.<init>(r5);
        throw r4;
    L_0x006e:
        r4 = r27.getClass();
        r5 = r25.getClass();
        if (r4 == r5) goto L_0x0081;
    L_0x0078:
        r4 = new java.lang.IllegalArgumentException;
        r5 = "Source address is different type to group";
        r4.<init>(r5);
        throw r4;
    L_0x0081:
        r21 = java.lang.System.getSecurityManager();
        if (r21 == 0) goto L_0x008e;
    L_0x0087:
        r0 = r21;
        r1 = r25;
        r0.checkMulticast(r1);
    L_0x008e:
        r0 = r24;
        r0 = r0.stateLock;
        r23 = r0;
        monitor-enter(r23);
        r4 = r24.isOpen();	 Catch:{ all -> 0x00a1 }
        if (r4 != 0) goto L_0x00a4;	 Catch:{ all -> 0x00a1 }
    L_0x009b:
        r4 = new java.nio.channels.ClosedChannelException;	 Catch:{ all -> 0x00a1 }
        r4.<init>();	 Catch:{ all -> 0x00a1 }
        throw r4;	 Catch:{ all -> 0x00a1 }
    L_0x00a1:
        r4 = move-exception;
        monitor-exit(r23);
        throw r4;
    L_0x00a4:
        r0 = r24;	 Catch:{ all -> 0x00a1 }
        r4 = r0.registry;	 Catch:{ all -> 0x00a1 }
        if (r4 != 0) goto L_0x00d7;	 Catch:{ all -> 0x00a1 }
    L_0x00aa:
        r4 = new sun.nio.ch.MembershipRegistry;	 Catch:{ all -> 0x00a1 }
        r4.<init>();	 Catch:{ all -> 0x00a1 }
        r0 = r24;	 Catch:{ all -> 0x00a1 }
        r0.registry = r4;	 Catch:{ all -> 0x00a1 }
    L_0x00b3:
        r0 = r24;	 Catch:{ all -> 0x00a1 }
        r4 = r0.family;	 Catch:{ all -> 0x00a1 }
        r5 = java.net.StandardProtocolFamily.INET6;	 Catch:{ all -> 0x00a1 }
        if (r4 != r5) goto L_0x011e;	 Catch:{ all -> 0x00a1 }
    L_0x00bb:
        r0 = r25;	 Catch:{ all -> 0x00a1 }
        r4 = r0 instanceof java.net.Inet6Address;	 Catch:{ all -> 0x00a1 }
        if (r4 != 0) goto L_0x00c7;	 Catch:{ all -> 0x00a1 }
    L_0x00c1:
        r4 = sun.nio.ch.Net.canJoin6WithIPv4Group();	 Catch:{ all -> 0x00a1 }
        if (r4 == 0) goto L_0x011e;	 Catch:{ all -> 0x00a1 }
    L_0x00c7:
        r9 = r26.getIndex();	 Catch:{ all -> 0x00a1 }
        r4 = -1;	 Catch:{ all -> 0x00a1 }
        if (r9 != r4) goto L_0x00e9;	 Catch:{ all -> 0x00a1 }
    L_0x00ce:
        r4 = new java.io.IOException;	 Catch:{ all -> 0x00a1 }
        r5 = "Network interface cannot be identified";	 Catch:{ all -> 0x00a1 }
        r4.<init>(r5);	 Catch:{ all -> 0x00a1 }
        throw r4;	 Catch:{ all -> 0x00a1 }
    L_0x00d7:
        r0 = r24;	 Catch:{ all -> 0x00a1 }
        r4 = r0.registry;	 Catch:{ all -> 0x00a1 }
        r0 = r25;	 Catch:{ all -> 0x00a1 }
        r1 = r26;	 Catch:{ all -> 0x00a1 }
        r2 = r27;	 Catch:{ all -> 0x00a1 }
        r19 = r4.checkMembership(r0, r1, r2);	 Catch:{ all -> 0x00a1 }
        if (r19 == 0) goto L_0x00b3;
    L_0x00e7:
        monitor-exit(r23);
        return r19;
    L_0x00e9:
        r8 = sun.nio.ch.Net.inet6AsByteArray(r25);	 Catch:{ all -> 0x00a1 }
        if (r27 != 0) goto L_0x0103;	 Catch:{ all -> 0x00a1 }
    L_0x00ef:
        r10 = 0;	 Catch:{ all -> 0x00a1 }
    L_0x00f0:
        r0 = r24;	 Catch:{ all -> 0x00a1 }
        r4 = r0.fd;	 Catch:{ all -> 0x00a1 }
        r20 = sun.nio.ch.Net.join6(r4, r8, r9, r10);	 Catch:{ all -> 0x00a1 }
        r4 = -2;	 Catch:{ all -> 0x00a1 }
        r0 = r20;	 Catch:{ all -> 0x00a1 }
        if (r0 != r4) goto L_0x0108;	 Catch:{ all -> 0x00a1 }
    L_0x00fd:
        r4 = new java.lang.UnsupportedOperationException;	 Catch:{ all -> 0x00a1 }
        r4.<init>();	 Catch:{ all -> 0x00a1 }
        throw r4;	 Catch:{ all -> 0x00a1 }
    L_0x0103:
        r10 = sun.nio.ch.Net.inet6AsByteArray(r27);	 Catch:{ all -> 0x00a1 }
        goto L_0x00f0;	 Catch:{ all -> 0x00a1 }
    L_0x0108:
        r3 = new sun.nio.ch.MembershipKeyImpl$Type6;	 Catch:{ all -> 0x00a1 }
        r4 = r24;	 Catch:{ all -> 0x00a1 }
        r5 = r25;	 Catch:{ all -> 0x00a1 }
        r6 = r26;	 Catch:{ all -> 0x00a1 }
        r7 = r27;	 Catch:{ all -> 0x00a1 }
        r3.<init>(r4, r5, r6, r7, r8, r9, r10);	 Catch:{ all -> 0x00a1 }
    L_0x0115:
        r0 = r24;	 Catch:{ all -> 0x00a1 }
        r4 = r0.registry;	 Catch:{ all -> 0x00a1 }
        r4.add(r3);	 Catch:{ all -> 0x00a1 }
        monitor-exit(r23);
        return r3;
    L_0x011e:
        r22 = sun.nio.ch.Net.anyInet4Address(r26);	 Catch:{ all -> 0x00a1 }
        if (r22 != 0) goto L_0x012d;	 Catch:{ all -> 0x00a1 }
    L_0x0124:
        r4 = new java.io.IOException;	 Catch:{ all -> 0x00a1 }
        r5 = "Network interface not configured for IPv4";	 Catch:{ all -> 0x00a1 }
        r4.<init>(r5);	 Catch:{ all -> 0x00a1 }
        throw r4;	 Catch:{ all -> 0x00a1 }
    L_0x012d:
        r16 = sun.nio.ch.Net.inet4AsInt(r25);	 Catch:{ all -> 0x00a1 }
        r17 = sun.nio.ch.Net.inet4AsInt(r22);	 Catch:{ all -> 0x00a1 }
        if (r27 != 0) goto L_0x0152;	 Catch:{ all -> 0x00a1 }
    L_0x0137:
        r18 = 0;	 Catch:{ all -> 0x00a1 }
    L_0x0139:
        r0 = r24;	 Catch:{ all -> 0x00a1 }
        r4 = r0.fd;	 Catch:{ all -> 0x00a1 }
        r0 = r16;	 Catch:{ all -> 0x00a1 }
        r1 = r17;	 Catch:{ all -> 0x00a1 }
        r2 = r18;	 Catch:{ all -> 0x00a1 }
        r20 = sun.nio.ch.Net.join4(r4, r0, r1, r2);	 Catch:{ all -> 0x00a1 }
        r4 = -2;	 Catch:{ all -> 0x00a1 }
        r0 = r20;	 Catch:{ all -> 0x00a1 }
        if (r0 != r4) goto L_0x0157;	 Catch:{ all -> 0x00a1 }
    L_0x014c:
        r4 = new java.lang.UnsupportedOperationException;	 Catch:{ all -> 0x00a1 }
        r4.<init>();	 Catch:{ all -> 0x00a1 }
        throw r4;	 Catch:{ all -> 0x00a1 }
    L_0x0152:
        r18 = sun.nio.ch.Net.inet4AsInt(r27);	 Catch:{ all -> 0x00a1 }
        goto L_0x0139;	 Catch:{ all -> 0x00a1 }
    L_0x0157:
        r3 = new sun.nio.ch.MembershipKeyImpl$Type4;	 Catch:{ all -> 0x00a1 }
        r11 = r3;	 Catch:{ all -> 0x00a1 }
        r12 = r24;	 Catch:{ all -> 0x00a1 }
        r13 = r25;	 Catch:{ all -> 0x00a1 }
        r14 = r26;	 Catch:{ all -> 0x00a1 }
        r15 = r27;	 Catch:{ all -> 0x00a1 }
        r11.<init>(r12, r13, r14, r15, r16, r17, r18);	 Catch:{ all -> 0x00a1 }
        goto L_0x0115;
        */
        throw new UnsupportedOperationException("Method not decompiled: sun.nio.ch.DatagramChannelImpl.innerJoin(java.net.InetAddress, java.net.NetworkInterface, java.net.InetAddress):java.nio.channels.MembershipKey");
    }

    public MembershipKey join(InetAddress group, NetworkInterface interf) throws IOException {
        return innerJoin(group, interf, null);
    }

    public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException {
        if (source != null) {
            return innerJoin(group, interf, source);
        }
        throw new NullPointerException("source address is null");
    }

    void drop(MembershipKeyImpl key) {
        if (-assertionsDisabled || key.channel() == this) {
            synchronized (this.stateLock) {
                if (key.isValid()) {
                    try {
                        if (key instanceof Type6) {
                            Type6 key6 = (Type6) key;
                            Net.drop6(this.fd, key6.groupAddress(), key6.index(), key6.source());
                        } else {
                            Type4 key4 = (Type4) key;
                            Net.drop4(this.fd, key4.groupAddress(), key4.interfaceAddress(), key4.source());
                        }
                        key.invalidate();
                        this.registry.remove(key);
                        return;
                    } catch (Object ioe) {
                        throw new AssertionError(ioe);
                    }
                }
                return;
            }
        }
        throw new AssertionError();
    }

    void block(MembershipKeyImpl key, InetAddress source) throws IOException {
        if (!-assertionsDisabled && key.channel() != this) {
            throw new AssertionError();
        } else if (-assertionsDisabled || key.sourceAddress() == null) {
            synchronized (this.stateLock) {
                if (!key.isValid()) {
                    throw new IllegalStateException("key is no longer valid");
                } else if (source.isAnyLocalAddress()) {
                    throw new IllegalArgumentException("Source address is a wildcard address");
                } else if (source.isMulticastAddress()) {
                    throw new IllegalArgumentException("Source address is multicast address");
                } else if (source.getClass() != key.group().getClass()) {
                    throw new IllegalArgumentException("Source address is different type to group");
                } else {
                    int n;
                    if (key instanceof Type6) {
                        Type6 key6 = (Type6) key;
                        n = Net.block6(this.fd, key6.groupAddress(), key6.index(), Net.inet6AsByteArray(source));
                    } else {
                        Type4 key4 = (Type4) key;
                        n = Net.block4(this.fd, key4.groupAddress(), key4.interfaceAddress(), Net.inet4AsInt(source));
                    }
                    if (n == -2) {
                        throw new UnsupportedOperationException();
                    }
                }
            }
        } else {
            throw new AssertionError();
        }
    }

    void unblock(MembershipKeyImpl key, InetAddress source) {
        if (!-assertionsDisabled && key.channel() != this) {
            throw new AssertionError();
        } else if (-assertionsDisabled || key.sourceAddress() == null) {
            synchronized (this.stateLock) {
                if (key.isValid()) {
                    try {
                        if (key instanceof Type6) {
                            Type6 key6 = (Type6) key;
                            Net.unblock6(this.fd, key6.groupAddress(), key6.index(), Net.inet6AsByteArray(source));
                        } else {
                            Type4 key4 = (Type4) key;
                            Net.unblock4(this.fd, key4.groupAddress(), key4.interfaceAddress(), Net.inet4AsInt(source));
                        }
                    } catch (Object ioe) {
                        throw new AssertionError(ioe);
                    }
                }
                throw new IllegalStateException("key is no longer valid");
            }
        } else {
            throw new AssertionError();
        }
    }

    protected void implCloseSelectableChannel() throws IOException {
        synchronized (this.stateLock) {
            this.guard.close();
            if (this.state != 2) {
                nd.preClose(this.fd);
            }
            ResourceManager.afterUdpClose();
            if (this.registry != null) {
                this.registry.invalidateAll();
            }
            long th = this.readerThread;
            if (th != 0) {
                NativeThread.signal(th);
            }
            th = this.writerThread;
            if (th != 0) {
                NativeThread.signal(th);
            }
            if (!isRegistered()) {
                kill();
            }
        }
    }

    public void kill() throws IOException {
        synchronized (this.stateLock) {
            if (this.state == 2) {
            } else if (this.state == -1) {
                this.state = 2;
            } else if (-assertionsDisabled || !(isOpen() || isRegistered())) {
                nd.close(this.fd);
                this.state = 2;
            } else {
                throw new AssertionError();
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            if (this.fd != null) {
                close();
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public boolean translateReadyOps(int ops, int initialOps, SelectionKeyImpl sk) {
        boolean z = true;
        int intOps = sk.nioInterestOps();
        int oldOps = sk.nioReadyOps();
        int newOps = initialOps;
        if ((Net.POLLNVAL & ops) != 0) {
            return -assertionsDisabled;
        }
        if (((Net.POLLERR | Net.POLLHUP) & ops) != 0) {
            newOps = intOps;
            sk.nioReadyOps(intOps);
            if (((~oldOps) & intOps) == 0) {
                z = -assertionsDisabled;
            }
            return z;
        }
        if (!((Net.POLLIN & ops) == 0 || (intOps & 1) == 0)) {
            newOps = initialOps | 1;
        }
        if (!((Net.POLLOUT & ops) == 0 || (intOps & 4) == 0)) {
            newOps |= 4;
        }
        sk.nioReadyOps(newOps);
        if (((~oldOps) & newOps) == 0) {
            z = -assertionsDisabled;
        }
        return z;
    }

    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl sk) {
        return translateReadyOps(ops, sk.nioReadyOps(), sk);
    }

    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl sk) {
        return translateReadyOps(ops, 0, sk);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int poll(int events, long timeout) throws IOException {
        boolean z = -assertionsDisabled;
        if (-assertionsDisabled || (Thread.holdsLock(blockingLock()) && !isBlocking())) {
            synchronized (this.readLock) {
                long isOpen;
                try {
                    begin();
                    synchronized (this.stateLock) {
                        isOpen = isOpen();
                        if (isOpen != null) {
                            this.readerThread = NativeThread.current();
                        }
                    }
                } finally {
                    isOpen = 0;
                    this.readerThread = 0;
                    end(-assertionsDisabled);
                }
            }
        }
        throw new AssertionError();
    }

    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        int newOps = 0;
        if ((ops & 1) != 0) {
            newOps = Net.POLLIN | 0;
        }
        if ((ops & 4) != 0) {
            newOps |= Net.POLLOUT;
        }
        if ((ops & 8) != 0) {
            newOps |= Net.POLLIN;
        }
        sk.selector.putEventOps(sk, newOps);
    }

    public FileDescriptor getFD() {
        return this.fd;
    }

    public int getFDVal() {
        return this.fdVal;
    }
}
