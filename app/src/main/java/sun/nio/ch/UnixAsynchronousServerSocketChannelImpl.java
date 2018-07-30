package sun.nio.ch;

import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AcceptPendingException;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetBoundException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

class UnixAsynchronousServerSocketChannelImpl extends AsynchronousServerSocketChannelImpl implements PollableChannel {
    private static final NativeDispatcher nd = new SocketDispatcher();
    private AccessControlContext acceptAcc;
    private Object acceptAttachment;
    private PendingFuture<AsynchronousSocketChannel, Object> acceptFuture;
    private CompletionHandler<AsynchronousSocketChannel, Object> acceptHandler;
    private boolean acceptPending;
    private final AtomicBoolean accepting = new AtomicBoolean();
    private final int fdVal;
    private final CloseGuard guard = CloseGuard.get();
    private final Port port;
    private final Object updateLock = new Object();

    private native int accept0(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, InetSocketAddress[] inetSocketAddressArr) throws IOException;

    private static native void initIDs();

    static {
        initIDs();
    }

    private void enableAccept() {
        this.accepting.set(false);
    }

    UnixAsynchronousServerSocketChannelImpl(Port port) throws IOException {
        super(port);
        try {
            IOUtil.configureBlocking(this.fd, false);
            this.port = port;
            this.fdVal = IOUtil.fdVal(this.fd);
            port.register(this.fdVal, this);
            this.guard.open("close");
        } catch (IOException x) {
            nd.close(this.fd);
            throw x;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void implClose() throws IOException {
        this.guard.close();
        this.port.unregister(this.fdVal);
        nd.close(this.fd);
        synchronized (this.updateLock) {
            if (this.acceptPending) {
                this.acceptPending = false;
                CompletionHandler handler = this.acceptHandler;
                Object att = this.acceptAttachment;
                PendingFuture<AsynchronousSocketChannel, Object> future = this.acceptFuture;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    public AsynchronousChannelGroupImpl group() {
        return this.port;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onEvent(int events, boolean mayInvokeDirect) {
        synchronized (this.updateLock) {
            if (this.acceptPending) {
                this.acceptPending = false;
            } else {
                return;
            }
        }
        CompletionHandler<AsynchronousSocketChannel, Object> handler;
        Object att;
        PendingFuture<AsynchronousSocketChannel, Object> future;
        AsynchronousSocketChannel child;
        Throwable exc;
        handler = this.acceptHandler;
        att = this.acceptAttachment;
        future = this.acceptFuture;
        enableAccept();
        if (handler != null) {
            future.setResult(child, exc);
            if (child != null && future.isCancelled()) {
                try {
                    child.close();
                } catch (IOException e) {
                }
            }
        } else {
            Invoker.invoke(this, handler, att, child, exc);
        }
        child = null;
        if (exc == null) {
            try {
                child = finishAccept(newfd, isaa[0], this.acceptAcc);
            } catch (Throwable th) {
                Throwable x = th;
                if (!((x instanceof IOException) || ((x instanceof SecurityException) ^ 1) == 0)) {
                    x = new IOException(x);
                }
                exc = x;
            }
        }
        handler = this.acceptHandler;
        att = this.acceptAttachment;
        future = this.acceptFuture;
        enableAccept();
        if (handler != null) {
            Invoker.invoke(this, handler, att, child, exc);
        } else {
            future.setResult(child, exc);
            if (child != null && future.isCancelled()) {
                try {
                    child.close();
                } catch (IOException e2) {
                }
            }
        }
    }

    private AsynchronousSocketChannel finishAccept(FileDescriptor newfd, final InetSocketAddress remote, AccessControlContext acc) throws IOException, SecurityException {
        try {
            AsynchronousSocketChannel ch = new UnixAsynchronousSocketChannelImpl(this.port, newfd, remote);
            if (acc != null) {
                try {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        public Void run() {
                            SecurityManager sm = System.getSecurityManager();
                            if (sm != null) {
                                sm.checkAccept(remote.getAddress().getHostAddress(), remote.getPort());
                            }
                            return null;
                        }
                    }, acc);
                } catch (SecurityException x) {
                    ch.close();
                } catch (Throwable suppressed) {
                    x.addSuppressed(suppressed);
                }
            } else {
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    sm.checkAccept(remote.getAddress().getHostAddress(), remote.getPort());
                }
            }
            return ch;
            throw x;
        } catch (IOException x2) {
            nd.close(newfd);
            throw x2;
        }
    }

    Future<AsynchronousSocketChannel> implAccept(Object att, CompletionHandler<AsynchronousSocketChannel, Object> handler) {
        Throwable th;
        Throwable x;
        if (!isOpen()) {
            Throwable e = new ClosedChannelException();
            if (handler == null) {
                return CompletedFuture.withFailure(e);
            }
            Invoker.invoke(this, handler, att, null, e);
            return null;
        } else if (this.localAddress == null) {
            throw new NotYetBoundException();
        } else if (isAcceptKilled()) {
            throw new RuntimeException("Accept not allowed due cancellation");
        } else if (this.accepting.compareAndSet(false, true)) {
            FileDescriptor newfd = new FileDescriptor();
            InetSocketAddress[] isaa = new InetSocketAddress[1];
            Throwable exc = null;
            try {
                begin();
                if (accept(this.fd, newfd, isaa) == -2) {
                    Future<AsynchronousSocketChannel> future = null;
                    synchronized (this.updateLock) {
                        AccessControlContext accessControlContext;
                        if (handler == null) {
                            PendingFuture<AsynchronousSocketChannel, Object> result;
                            try {
                                this.acceptHandler = null;
                                result = new PendingFuture(this);
                            } catch (Throwable th2) {
                                th = th2;
                            }
                            try {
                                this.acceptFuture = result;
                                future = result;
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        }
                        this.acceptHandler = handler;
                        this.acceptAttachment = att;
                        if (System.getSecurityManager() == null) {
                            accessControlContext = null;
                        } else {
                            accessControlContext = AccessController.getContext();
                        }
                        this.acceptAcc = accessControlContext;
                        this.acceptPending = true;
                        this.port.startPoll(this.fdVal, Net.POLLIN);
                        end();
                        return future;
                    }
                }
                end();
                Object child = null;
                if (exc == null) {
                    try {
                        child = finishAccept(newfd, isaa[0], null);
                    } catch (Throwable x2) {
                        exc = x2;
                    }
                }
                enableAccept();
                if (handler == null) {
                    return CompletedFuture.withResult(child, exc);
                }
                Invoker.invokeIndirectly((AsynchronousChannel) this, (CompletionHandler) handler, att, child, exc);
                return null;
            } catch (Throwable th4) {
                end();
            }
        } else {
            throw new AcceptPendingException();
        }
    }

    private int accept(FileDescriptor ssfd, FileDescriptor newfd, InetSocketAddress[] isaa) throws IOException {
        return accept0(ssfd, newfd, isaa);
    }
}
