package sun.nio.ch;

import android.system.ErrnoException;
import android.system.OsConstants;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DirectByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import libcore.io.Libcore;
import sun.misc.Cleaner;
import sun.security.action.GetPropertyAction;

public class FileChannelImpl extends FileChannel {
    static final /* synthetic */ boolean -assertionsDisabled = (FileChannelImpl.class.desiredAssertionStatus() ^ 1);
    private static final long MAPPED_TRANSFER_SIZE = 8388608;
    private static final int MAP_PV = 2;
    private static final int MAP_RO = 0;
    private static final int MAP_RW = 1;
    private static final int TRANSFER_SIZE = 8192;
    private static final long allocationGranularity = initIDs();
    private static volatile boolean fileSupported = true;
    private static boolean isSharedFileLockTable;
    private static volatile boolean pipeSupported = true;
    private static volatile boolean propertyChecked;
    private static volatile boolean transferSupported = true;
    private final boolean append;
    public final FileDescriptor fd;
    private volatile FileLockTable fileLockTable;
    private final CloseGuard guard = CloseGuard.get();
    private final FileDispatcher nd;
    private final Object parent;
    private final String path;
    private final Object positionLock = new Object();
    private final boolean readable;
    private final NativeThreadSet threads = new NativeThreadSet(2);
    private final boolean writable;

    private static class SimpleFileLockTable extends FileLockTable {
        static final /* synthetic */ boolean -assertionsDisabled = (SimpleFileLockTable.class.desiredAssertionStatus() ^ 1);
        private final List<FileLock> lockList = new ArrayList(2);

        private void checkList(long position, long size) throws OverlappingFileLockException {
            if (-assertionsDisabled || Thread.holdsLock(this.lockList)) {
                for (FileLock fl : this.lockList) {
                    if (fl.overlaps(position, size)) {
                        throw new OverlappingFileLockException();
                    }
                }
                return;
            }
            throw new AssertionError();
        }

        public void add(FileLock fl) throws OverlappingFileLockException {
            synchronized (this.lockList) {
                checkList(fl.position(), fl.size());
                this.lockList.add(fl);
            }
        }

        public void remove(FileLock fl) {
            synchronized (this.lockList) {
                this.lockList.remove((Object) fl);
            }
        }

        public List<FileLock> removeAll() {
            List<FileLock> result;
            synchronized (this.lockList) {
                result = new ArrayList(this.lockList);
                this.lockList.clear();
            }
            return result;
        }

        public void replace(FileLock fl1, FileLock fl2) {
            synchronized (this.lockList) {
                this.lockList.remove((Object) fl1);
                this.lockList.add(fl2);
            }
        }
    }

    private static class Unmapper implements Runnable {
        static final /* synthetic */ boolean -assertionsDisabled = (Unmapper.class.desiredAssertionStatus() ^ 1);
        static volatile int count;
        private static final NativeDispatcher nd = new FileDispatcherImpl();
        static volatile long totalCapacity;
        static volatile long totalSize;
        private volatile long address;
        private final int cap;
        private final FileDescriptor fd;
        private final long size;

        /* synthetic */ Unmapper(long address, long size, int cap, FileDescriptor fd, Unmapper -this4) {
            this(address, size, cap, fd);
        }

        private Unmapper(long address, long size, int cap, FileDescriptor fd) {
            if (-assertionsDisabled || address != 0) {
                this.address = address;
                this.size = size;
                this.cap = cap;
                this.fd = fd;
                synchronized (Unmapper.class) {
                    count++;
                    totalSize += size;
                    totalCapacity += (long) cap;
                }
                return;
            }
            throw new AssertionError();
        }

        public void run() {
            if (this.address != 0) {
                FileChannelImpl.unmap0(this.address, this.size);
                this.address = 0;
                if (this.fd.valid()) {
                    try {
                        nd.close(this.fd);
                    } catch (IOException e) {
                    }
                }
                synchronized (Unmapper.class) {
                    count--;
                    totalSize -= this.size;
                    totalCapacity -= (long) this.cap;
                }
            }
        }
    }

    private static native long initIDs();

    private native long map0(int i, long j, long j2) throws IOException;

    private native long position0(FileDescriptor fileDescriptor, long j);

    private native long transferTo0(FileDescriptor fileDescriptor, long j, long j2, FileDescriptor fileDescriptor2);

    private static native int unmap0(long j, long j2);

    private FileChannelImpl(FileDescriptor fd, String path, boolean readable, boolean writable, boolean append, Object parent) {
        this.fd = fd;
        this.readable = readable;
        this.writable = writable;
        this.append = append;
        this.parent = parent;
        this.path = path;
        this.nd = new FileDispatcherImpl(append);
        if (fd != null && fd.valid()) {
            this.guard.open("close");
        }
    }

    public static FileChannel open(FileDescriptor fd, String path, boolean readable, boolean writable, Object parent) {
        return new FileChannelImpl(fd, path, readable, writable, -assertionsDisabled, parent);
    }

    public static FileChannel open(FileDescriptor fd, String path, boolean readable, boolean writable, boolean append, Object parent) {
        return new FileChannelImpl(fd, path, readable, writable, append, parent);
    }

    private void ensureOpen() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    protected void implCloseChannel() throws IOException {
        this.guard.close();
        if (this.fileLockTable != null) {
            for (FileLock fl : this.fileLockTable.removeAll()) {
                synchronized (fl) {
                    if (fl.isValid()) {
                        this.nd.release(this.fd, fl.position(), fl.size());
                        ((FileLockImpl) fl).invalidate();
                    }
                }
            }
        }
        this.threads.signalAndWait();
        if (this.parent != null) {
            ((Closeable) this.parent).close();
        } else {
            this.nd.close(this.fd);
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

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read(ByteBuffer dst) throws IOException {
        boolean z = true;
        ensureOpen();
        if (this.readable) {
            synchronized (this.positionLock) {
                int n = 0;
                int ti = -1;
                try {
                    begin();
                    ti = this.threads.add();
                    if (isOpen()) {
                        do {
                            n = IOUtil.read(this.fd, dst, -1, this.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        int normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        if (n <= 0) {
                            z = -assertionsDisabled;
                        }
                        end(z);
                        if (-assertionsDisabled || IOStatus.check(n)) {
                        } else {
                            throw new AssertionError();
                        }
                    }
                    this.threads.remove(ti);
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(0)) {
                    } else {
                        throw new AssertionError();
                    }
                } catch (Throwable th) {
                    this.threads.remove(ti);
                    if (n <= 0) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (!-assertionsDisabled && !IOStatus.check(n)) {
                        AssertionError assertionError = new AssertionError();
                    }
                }
            }
        } else {
            throw new NonReadableChannelException();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        boolean z = true;
        if (offset < 0 || length < 0 || offset > dsts.length - length) {
            throw new IndexOutOfBoundsException();
        }
        ensureOpen();
        if (this.readable) {
            synchronized (this.positionLock) {
                long n = 0;
                int ti = -1;
                try {
                    begin();
                    ti = this.threads.add();
                    if (isOpen()) {
                        do {
                            n = IOUtil.read(this.fd, dsts, offset, length, this.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        long normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        if (n <= 0) {
                            z = -assertionsDisabled;
                        }
                        end(z);
                        if (-assertionsDisabled || IOStatus.check(n)) {
                        } else {
                            throw new AssertionError();
                        }
                    }
                    this.threads.remove(ti);
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(0)) {
                    } else {
                        throw new AssertionError();
                    }
                } catch (Throwable th) {
                    this.threads.remove(ti);
                    if (n <= 0) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (!-assertionsDisabled && !IOStatus.check(n)) {
                        AssertionError assertionError = new AssertionError();
                    }
                }
            }
        } else {
            throw new NonReadableChannelException();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int write(ByteBuffer src) throws IOException {
        boolean z = true;
        ensureOpen();
        if (this.writable) {
            synchronized (this.positionLock) {
                int n = 0;
                int ti = -1;
                try {
                    begin();
                    ti = this.threads.add();
                    if (isOpen()) {
                        do {
                            n = IOUtil.write(this.fd, src, -1, this.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        int normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        if (n <= 0) {
                            z = -assertionsDisabled;
                        }
                        end(z);
                        if (-assertionsDisabled || IOStatus.check(n)) {
                        } else {
                            throw new AssertionError();
                        }
                    }
                    this.threads.remove(ti);
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(0)) {
                    } else {
                        throw new AssertionError();
                    }
                } catch (Throwable th) {
                    this.threads.remove(ti);
                    if (n <= 0) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (!-assertionsDisabled && !IOStatus.check(n)) {
                        AssertionError assertionError = new AssertionError();
                    }
                }
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        boolean z = true;
        if (offset < 0 || length < 0 || offset > srcs.length - length) {
            throw new IndexOutOfBoundsException();
        }
        ensureOpen();
        if (this.writable) {
            synchronized (this.positionLock) {
                long n = 0;
                int ti = -1;
                try {
                    begin();
                    ti = this.threads.add();
                    if (isOpen()) {
                        do {
                            n = IOUtil.write(this.fd, srcs, offset, length, this.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        long normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        if (n <= 0) {
                            z = -assertionsDisabled;
                        }
                        end(z);
                        if (-assertionsDisabled || IOStatus.check(n)) {
                        } else {
                            throw new AssertionError();
                        }
                    }
                    this.threads.remove(ti);
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(0)) {
                    } else {
                        throw new AssertionError();
                    }
                } catch (Throwable th) {
                    this.threads.remove(ti);
                    if (n <= 0) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (!-assertionsDisabled && !IOStatus.check(n)) {
                        AssertionError assertionError = new AssertionError();
                    }
                }
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long position() throws IOException {
        boolean z = true;
        ensureOpen();
        synchronized (this.positionLock) {
            long p = -1;
            int ti = -1;
            try {
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    if (this.append) {
                        BlockGuard.getThreadPolicy().onWriteToDisk();
                    }
                    do {
                        p = this.append ? this.nd.size(this.fd) : position0(this.fd, -1);
                        if (p != -3) {
                            break;
                        }
                    } while (isOpen());
                    long normalize = IOStatus.normalize(p);
                    this.threads.remove(ti);
                    if (p <= -1) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(p)) {
                    } else {
                        throw new AssertionError();
                    }
                }
                this.threads.remove(ti);
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(-1)) {
                    return 0;
                }
                throw new AssertionError();
            } catch (Throwable th) {
                this.threads.remove(ti);
                if (p <= -1) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(p)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public FileChannel position(long newPosition) throws IOException {
        boolean z = true;
        ensureOpen();
        if (newPosition < 0) {
            throw new IllegalArgumentException();
        }
        synchronized (this.positionLock) {
            int ti = -1;
            try {
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    long p;
                    BlockGuard.getThreadPolicy().onReadFromDisk();
                    do {
                        p = position0(this.fd, newPosition);
                        if (p == -3) {
                        }
                        break;
                    } while (isOpen());
                    this.threads.remove(ti);
                    if (p <= -1) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(p)) {
                    } else {
                        throw new AssertionError();
                    }
                }
                this.threads.remove(ti);
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(-1)) {
                } else {
                    throw new AssertionError();
                }
            } catch (Throwable th) {
                this.threads.remove(ti);
                if (-1 <= -1) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(-1)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long size() throws IOException {
        boolean z = true;
        ensureOpen();
        synchronized (this.positionLock) {
            long s = -1;
            int ti = -1;
            try {
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    do {
                        s = this.nd.size(this.fd);
                        if (s != -3) {
                            break;
                        }
                    } while (isOpen());
                    long normalize = IOStatus.normalize(s);
                    this.threads.remove(ti);
                    if (s <= -1) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(s)) {
                    } else {
                        throw new AssertionError();
                    }
                }
                this.threads.remove(ti);
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(-1)) {
                } else {
                    throw new AssertionError();
                }
            } catch (Throwable th) {
                this.threads.remove(ti);
                if (s <= -1) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(s)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public FileChannel truncate(long newSize) throws IOException {
        ensureOpen();
        if (newSize < 0) {
            throw new IllegalArgumentException("Negative size");
        } else if (this.writable) {
            synchronized (this.positionLock) {
                int rv = -1;
                int ti = -1;
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    long size;
                    do {
                        try {
                            size = this.nd.size(this.fd);
                            if (size != -3) {
                                break;
                            }
                        } catch (Throwable th) {
                            this.threads.remove(ti);
                            end(rv > -1 ? true : -assertionsDisabled);
                            if (!-assertionsDisabled && !IOStatus.check(rv)) {
                                AssertionError assertionError = new AssertionError();
                            }
                        }
                    } while (isOpen());
                    if (isOpen()) {
                        long p;
                        do {
                            p = position0(this.fd, -1);
                            if (p != -3) {
                                break;
                            }
                        } while (isOpen());
                        if (!isOpen()) {
                            this.threads.remove(ti);
                            end(-assertionsDisabled);
                            if (-assertionsDisabled || IOStatus.check(-1)) {
                                return null;
                            }
                            throw new AssertionError();
                        } else if (-assertionsDisabled || p >= 0) {
                            if (newSize < size) {
                                do {
                                    rv = this.nd.truncate(this.fd, newSize);
                                    if (rv != -3) {
                                        break;
                                    }
                                } while (isOpen());
                                if (!isOpen()) {
                                    this.threads.remove(ti);
                                    end(rv > -1 ? true : -assertionsDisabled);
                                    if (-assertionsDisabled || IOStatus.check(rv)) {
                                        return null;
                                    }
                                    throw new AssertionError();
                                }
                            }
                            if (p > newSize) {
                                p = newSize;
                            }
                            do {
                                rv = (int) position0(this.fd, p);
                                if (rv == -3) {
                                }
                                break;
                            } while (isOpen());
                            this.threads.remove(ti);
                            end(rv > -1 ? true : -assertionsDisabled);
                            if (-assertionsDisabled || IOStatus.check(rv)) {
                            } else {
                                throw new AssertionError();
                            }
                        } else {
                            throw new AssertionError();
                        }
                    }
                    this.threads.remove(ti);
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(-1)) {
                        return null;
                    }
                    throw new AssertionError();
                }
                this.threads.remove(ti);
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(-1)) {
                    return null;
                }
                throw new AssertionError();
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    public void force(boolean metaData) throws IOException {
        boolean z = true;
        ensureOpen();
        int rv = -1;
        int ti = -1;
        try {
            begin();
            ti = this.threads.add();
            if (isOpen()) {
                do {
                    rv = this.nd.force(this.fd, metaData);
                    if (rv != -3) {
                        break;
                    }
                } while (isOpen());
                this.threads.remove(ti);
                if (rv <= -1) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(rv)) {
                    throw new AssertionError();
                }
                return;
            }
            this.threads.remove(ti);
            end(-assertionsDisabled);
            if (!-assertionsDisabled && !IOStatus.check(-1)) {
                throw new AssertionError();
            }
        } catch (Throwable th) {
            this.threads.remove(ti);
            if (rv <= -1) {
                z = -assertionsDisabled;
            }
            end(z);
            if (!-assertionsDisabled && !IOStatus.check(rv)) {
                AssertionError assertionError = new AssertionError();
            }
        }
    }

    private long transferToDirectlyInternal(long position, int icount, WritableByteChannel target, FileDescriptor targetFD) throws IOException {
        if (-assertionsDisabled || !this.nd.transferToDirectlyNeedsPositionLock() || (Thread.holdsLock(this.positionLock) ^ 1) == 0) {
            long n = -1;
            int ti = -1;
            try {
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    BlockGuard.getThreadPolicy().onWriteToDisk();
                    do {
                        n = transferTo0(this.fd, position, (long) icount, targetFD);
                        if (n != -3) {
                            break;
                        }
                    } while (isOpen());
                    boolean z;
                    if (n == -6) {
                        if (target instanceof SinkChannelImpl) {
                            pipeSupported = -assertionsDisabled;
                        }
                        if (target instanceof FileChannelImpl) {
                            fileSupported = -assertionsDisabled;
                        }
                        this.threads.remove(ti);
                        if (n > -1) {
                            z = true;
                        } else {
                            z = -assertionsDisabled;
                        }
                        end(z);
                        return -6;
                    } else if (n == -4) {
                        transferSupported = -assertionsDisabled;
                        this.threads.remove(ti);
                        if (n > -1) {
                            z = true;
                        } else {
                            z = -assertionsDisabled;
                        }
                        end(z);
                        return -4;
                    } else {
                        long normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        end(n > -1 ? true : -assertionsDisabled);
                        return normalize;
                    }
                }
                this.threads.remove(ti);
                end(-assertionsDisabled);
                return -1;
            } catch (Throwable th) {
                this.threads.remove(ti);
                end(n > -1 ? true : -assertionsDisabled);
            }
        } else {
            throw new AssertionError();
        }
    }

    private long transferToDirectly(long position, int icount, WritableByteChannel target) throws IOException {
        if (!transferSupported) {
            return -4;
        }
        FileDescriptor targetFD = null;
        if (target instanceof FileChannelImpl) {
            if (!fileSupported) {
                return -6;
            }
            targetFD = ((FileChannelImpl) target).fd;
        } else if (target instanceof SelChImpl) {
            if ((target instanceof SinkChannelImpl) && (pipeSupported ^ 1) != 0) {
                return -6;
            }
            if (!this.nd.canTransferToDirectly((SelectableChannel) target)) {
                return -6;
            }
            targetFD = ((SelChImpl) target).getFD();
        }
        if (targetFD == null) {
            return -4;
        }
        if (IOUtil.fdVal(this.fd) == IOUtil.fdVal(targetFD)) {
            return -4;
        }
        if (!this.nd.transferToDirectlyNeedsPositionLock()) {
            return transferToDirectlyInternal(position, icount, target, targetFD);
        }
        long transferToDirectlyInternal;
        synchronized (this.positionLock) {
            long pos = position();
            try {
                transferToDirectlyInternal = transferToDirectlyInternal(position, icount, target, targetFD);
                position(pos);
            } catch (Throwable th) {
                position(pos);
            }
        }
        return transferToDirectlyInternal;
    }

    private long transferToTrustedChannel(long position, long count, WritableByteChannel target) throws IOException {
        boolean isSelChImpl = target instanceof SelChImpl;
        if (!(!(target instanceof FileChannelImpl) ? isSelChImpl : true)) {
            return -4;
        }
        long remaining = count;
        while (remaining > 0) {
            MappedByteBuffer dbb;
            try {
                dbb = map(MapMode.READ_ONLY, position, Math.min(remaining, (long) MAPPED_TRANSFER_SIZE));
                int n = target.write(dbb);
                if (-assertionsDisabled || n >= 0) {
                    remaining -= (long) n;
                    if (isSelChImpl) {
                        unmap(dbb);
                        break;
                    } else if (-assertionsDisabled || n > 0) {
                        position += (long) n;
                        unmap(dbb);
                    } else {
                        throw new AssertionError();
                    }
                }
                throw new AssertionError();
            } catch (ClosedByInterruptException e) {
                if (-assertionsDisabled || !target.isOpen()) {
                    try {
                        close();
                    } catch (Throwable suppressed) {
                        e.addSuppressed(suppressed);
                    }
                    throw e;
                }
                throw new AssertionError();
            } catch (IOException ioe) {
                if (remaining == count) {
                    throw ioe;
                }
            } catch (Throwable th) {
                unmap(dbb);
            }
        }
        return count - remaining;
    }

    private long transferToArbitraryChannel(long r16, int r18, java.nio.channels.WritableByteChannel r19) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: java.lang.NullPointerException
	at java.util.BitSet.or(BitSet.java:940)
	at jadx.core.utils.BlockUtils.getPathCross(BlockUtils.java:433)
	at jadx.core.dex.visitors.regions.IfMakerHelper.restructureIf(IfMakerHelper.java:80)
	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:625)
	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:118)
	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:80)
	at jadx.core.dex.visitors.regions.RegionMaker.processExcHandler(RegionMaker.java:991)
	at jadx.core.dex.visitors.regions.RegionMaker.processTryCatchBlocks(RegionMaker.java:926)
	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:52)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
	at java.lang.Iterable.forEach(Iterable.java:75)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r15 = this;
        r11 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;
        r0 = r18;
        r3 = java.lang.Math.min(r0, r11);
        r2 = sun.nio.ch.Util.getTemporaryDirectBuffer(r3);
        r8 = 0;
        r6 = r16;
        sun.nio.ch.Util.erase(r2);	 Catch:{ IOException -> 0x0045 }
    L_0x0013:
        r0 = r18;	 Catch:{ IOException -> 0x0045 }
        r12 = (long) r0;	 Catch:{ IOException -> 0x0045 }
        r11 = (r8 > r12 ? 1 : (r8 == r12 ? 0 : -1));	 Catch:{ IOException -> 0x0045 }
        if (r11 >= 0) goto L_0x002e;	 Catch:{ IOException -> 0x0045 }
    L_0x001a:
        r0 = r18;	 Catch:{ IOException -> 0x0045 }
        r12 = (long) r0;	 Catch:{ IOException -> 0x0045 }
        r12 = r12 - r8;	 Catch:{ IOException -> 0x0045 }
        r11 = (int) r12;	 Catch:{ IOException -> 0x0045 }
        r12 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;	 Catch:{ IOException -> 0x0045 }
        r11 = java.lang.Math.min(r11, r12);	 Catch:{ IOException -> 0x0045 }
        r2.limit(r11);	 Catch:{ IOException -> 0x0045 }
        r4 = r15.read(r2, r6);	 Catch:{ IOException -> 0x0045 }
        if (r4 > 0) goto L_0x0032;
    L_0x002e:
        sun.nio.ch.Util.releaseTemporaryDirectBuffer(r2);
        return r8;
    L_0x0032:
        r2.flip();	 Catch:{ IOException -> 0x0045 }
        r0 = r19;	 Catch:{ IOException -> 0x0045 }
        r5 = r0.write(r2);	 Catch:{ IOException -> 0x0045 }
        r12 = (long) r5;	 Catch:{ IOException -> 0x0045 }
        r8 = r8 + r12;	 Catch:{ IOException -> 0x0045 }
        if (r5 != r4) goto L_0x002e;	 Catch:{ IOException -> 0x0045 }
    L_0x003f:
        r12 = (long) r5;	 Catch:{ IOException -> 0x0045 }
        r6 = r6 + r12;	 Catch:{ IOException -> 0x0045 }
        r2.clear();	 Catch:{ IOException -> 0x0045 }
        goto L_0x0013;
    L_0x0045:
        r10 = move-exception;
        r12 = 0;
        r11 = (r8 > r12 ? 1 : (r8 == r12 ? 0 : -1));
        if (r11 <= 0) goto L_0x0050;
    L_0x004c:
        sun.nio.ch.Util.releaseTemporaryDirectBuffer(r2);
        return r8;
    L_0x0050:
        throw r10;	 Catch:{ all -> 0x0051 }
    L_0x0051:
        r11 = move-exception;
        sun.nio.ch.Util.releaseTemporaryDirectBuffer(r2);
        throw r11;
        */
        throw new UnsupportedOperationException("Method not decompiled: sun.nio.ch.FileChannelImpl.transferToArbitraryChannel(long, int, java.nio.channels.WritableByteChannel):long");
    }

    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        ensureOpen();
        if (!target.isOpen()) {
            throw new ClosedChannelException();
        } else if (!this.readable) {
            throw new NonReadableChannelException();
        } else if ((target instanceof FileChannelImpl) && (((FileChannelImpl) target).writable ^ 1) != 0) {
            throw new NonWritableChannelException();
        } else if (position < 0 || count < 0) {
            throw new IllegalArgumentException();
        } else {
            long sz = size();
            if (position > sz) {
                return 0;
            }
            int icount = (int) Math.min(count, 2147483647L);
            if (sz - position < ((long) icount)) {
                icount = (int) (sz - position);
            }
            long n = transferToDirectly(position, icount, target);
            if (n >= 0) {
                return n;
            }
            n = transferToTrustedChannel(position, (long) icount, target);
            if (n >= 0) {
                return n;
            }
            return transferToArbitraryChannel(position, icount, target);
        }
    }

    private long transferFromFileChannel(FileChannelImpl src, long position, long count) throws IOException {
        if (src.readable) {
            long nwritten;
            synchronized (src.positionLock) {
                long pos = src.position();
                long max = Math.min(count, src.size() - pos);
                long remaining = max;
                long p = pos;
                while (remaining > 0) {
                    FileChannelImpl fileChannelImpl = src;
                    MappedByteBuffer bb = fileChannelImpl.map(MapMode.READ_ONLY, p, Math.min(remaining, (long) MAPPED_TRANSFER_SIZE));
                    try {
                        long n = (long) write(bb, position);
                        if (-assertionsDisabled || n > 0) {
                            p += n;
                            position += n;
                            remaining -= n;
                            unmap(bb);
                        } else {
                            throw new AssertionError();
                        }
                    } catch (IOException ioe) {
                        if (remaining == max) {
                            throw ioe;
                        } else {
                            unmap(bb);
                        }
                    } catch (Throwable th) {
                        unmap(bb);
                    }
                }
                nwritten = max - remaining;
                src.position(pos + nwritten);
            }
            return nwritten;
        }
        throw new NonReadableChannelException();
    }

    private long transferFromArbitraryChannel(java.nio.channels.ReadableByteChannel r17, long r18, long r20) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: java.lang.NullPointerException
	at java.util.BitSet.or(BitSet.java:940)
	at jadx.core.utils.BlockUtils.getPathCross(BlockUtils.java:433)
	at jadx.core.dex.visitors.regions.IfMakerHelper.restructureIf(IfMakerHelper.java:80)
	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:625)
	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:118)
	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:80)
	at jadx.core.dex.visitors.regions.RegionMaker.processExcHandler(RegionMaker.java:991)
	at jadx.core.dex.visitors.regions.RegionMaker.processTryCatchBlocks(RegionMaker.java:926)
	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:52)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
	at java.lang.Iterable.forEach(Iterable.java:75)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r16 = this;
        r12 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;
        r0 = r20;
        r12 = java.lang.Math.min(r0, r12);
        r3 = (int) r12;
        r2 = sun.nio.ch.Util.getTemporaryDirectBuffer(r3);
        r8 = 0;
        r6 = r18;
        sun.nio.ch.Util.erase(r2);	 Catch:{ IOException -> 0x0043 }
    L_0x0014:
        r11 = (r8 > r20 ? 1 : (r8 == r20 ? 0 : -1));	 Catch:{ IOException -> 0x0043 }
        if (r11 >= 0) goto L_0x002c;	 Catch:{ IOException -> 0x0043 }
    L_0x0018:
        r12 = r20 - r8;	 Catch:{ IOException -> 0x0043 }
        r14 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;	 Catch:{ IOException -> 0x0043 }
        r12 = java.lang.Math.min(r12, r14);	 Catch:{ IOException -> 0x0043 }
        r11 = (int) r12;	 Catch:{ IOException -> 0x0043 }
        r2.limit(r11);	 Catch:{ IOException -> 0x0043 }
        r0 = r17;	 Catch:{ IOException -> 0x0043 }
        r4 = r0.read(r2);	 Catch:{ IOException -> 0x0043 }
        if (r4 > 0) goto L_0x0030;
    L_0x002c:
        sun.nio.ch.Util.releaseTemporaryDirectBuffer(r2);
        return r8;
    L_0x0030:
        r2.flip();	 Catch:{ IOException -> 0x0043 }
        r0 = r16;	 Catch:{ IOException -> 0x0043 }
        r5 = r0.write(r2, r6);	 Catch:{ IOException -> 0x0043 }
        r12 = (long) r5;	 Catch:{ IOException -> 0x0043 }
        r8 = r8 + r12;	 Catch:{ IOException -> 0x0043 }
        if (r5 != r4) goto L_0x002c;	 Catch:{ IOException -> 0x0043 }
    L_0x003d:
        r12 = (long) r5;	 Catch:{ IOException -> 0x0043 }
        r6 = r6 + r12;	 Catch:{ IOException -> 0x0043 }
        r2.clear();	 Catch:{ IOException -> 0x0043 }
        goto L_0x0014;
    L_0x0043:
        r10 = move-exception;
        r12 = 0;
        r11 = (r8 > r12 ? 1 : (r8 == r12 ? 0 : -1));
        if (r11 <= 0) goto L_0x004e;
    L_0x004a:
        sun.nio.ch.Util.releaseTemporaryDirectBuffer(r2);
        return r8;
    L_0x004e:
        throw r10;	 Catch:{ all -> 0x004f }
    L_0x004f:
        r11 = move-exception;
        sun.nio.ch.Util.releaseTemporaryDirectBuffer(r2);
        throw r11;
        */
        throw new UnsupportedOperationException("Method not decompiled: sun.nio.ch.FileChannelImpl.transferFromArbitraryChannel(java.nio.channels.ReadableByteChannel, long, long):long");
    }

    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        ensureOpen();
        if (!src.isOpen()) {
            throw new ClosedChannelException();
        } else if (!this.writable) {
            throw new NonWritableChannelException();
        } else if (position < 0 || count < 0) {
            throw new IllegalArgumentException();
        } else if (position > size()) {
            return 0;
        } else {
            if (src instanceof FileChannelImpl) {
                return transferFromFileChannel((FileChannelImpl) src, position, count);
            }
            return transferFromArbitraryChannel(src, position, count);
        }
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        if (dst == null) {
            throw new NullPointerException();
        } else if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (this.readable) {
            ensureOpen();
            if (!this.nd.needsPositionLock()) {
                return readInternal(dst, position);
            }
            int readInternal;
            synchronized (this.positionLock) {
                readInternal = readInternal(dst, position);
            }
            return readInternal;
        } else {
            throw new NonReadableChannelException();
        }
    }

    private int readInternal(ByteBuffer dst, long position) throws IOException {
        boolean z = true;
        if (-assertionsDisabled || !this.nd.needsPositionLock() || (Thread.holdsLock(this.positionLock) ^ 1) == 0) {
            int n = 0;
            int ti = -1;
            try {
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    do {
                        n = IOUtil.read(this.fd, dst, position, this.nd);
                        if (n != -3) {
                            break;
                        }
                    } while (isOpen());
                    int normalize = IOStatus.normalize(n);
                    this.threads.remove(ti);
                    if (n <= 0) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(n)) {
                        return normalize;
                    }
                    throw new AssertionError();
                }
                this.threads.remove(ti);
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(0)) {
                    return -1;
                }
                throw new AssertionError();
            } catch (Throwable th) {
                this.threads.remove(ti);
                if (n <= 0) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(n)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        } else {
            throw new AssertionError();
        }
    }

    public int write(ByteBuffer src, long position) throws IOException {
        if (src == null) {
            throw new NullPointerException();
        } else if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (this.writable) {
            ensureOpen();
            if (!this.nd.needsPositionLock()) {
                return writeInternal(src, position);
            }
            int writeInternal;
            synchronized (this.positionLock) {
                writeInternal = writeInternal(src, position);
            }
            return writeInternal;
        } else {
            throw new NonWritableChannelException();
        }
    }

    private int writeInternal(ByteBuffer src, long position) throws IOException {
        boolean z = true;
        if (-assertionsDisabled || !this.nd.needsPositionLock() || (Thread.holdsLock(this.positionLock) ^ 1) == 0) {
            int n = 0;
            int ti = -1;
            try {
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    do {
                        n = IOUtil.write(this.fd, src, position, this.nd);
                        if (n != -3) {
                            break;
                        }
                    } while (isOpen());
                    int normalize = IOStatus.normalize(n);
                    this.threads.remove(ti);
                    if (n <= 0) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(n)) {
                        return normalize;
                    }
                    throw new AssertionError();
                }
                this.threads.remove(ti);
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(0)) {
                    return -1;
                }
                throw new AssertionError();
            } catch (Throwable th) {
                this.threads.remove(ti);
                if (n <= 0) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(n)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        } else {
            throw new AssertionError();
        }
    }

    private static void unmap(MappedByteBuffer bb) {
        Cleaner cl = ((DirectBuffer) bb).cleaner();
        if (cl != null) {
            cl.clean();
        }
    }

    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        long mapSize;
        long mapPosition;
        ensureOpen();
        if (mode == null) {
            throw new NullPointerException("Mode is null");
        } else if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (size < 0) {
            throw new IllegalArgumentException("Negative size");
        } else if (position + size < 0) {
            throw new IllegalArgumentException("Position + size overflow");
        } else if (size > 2147483647L) {
            throw new IllegalArgumentException("Size exceeds Integer.MAX_VALUE");
        } else {
            int imode = -1;
            if (mode == MapMode.READ_ONLY) {
                imode = 0;
            } else if (mode == MapMode.READ_WRITE) {
                imode = 1;
            } else if (mode == MapMode.PRIVATE) {
                imode = 2;
            }
            if (!-assertionsDisabled && imode < 0) {
                throw new AssertionError();
            } else if (mode != MapMode.READ_ONLY && (this.writable ^ 1) != 0) {
                throw new NonWritableChannelException();
            } else if (this.readable) {
                long addr = -1;
                int ti = -1;
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    long filesize;
                    do {
                        try {
                            filesize = this.nd.size(this.fd);
                            if (filesize != -3) {
                                break;
                            }
                        } catch (IOException ioe) {
                            unmap0(addr, mapSize);
                            throw ioe;
                        } catch (Throwable y) {
                            throw new IOException("Map failed", y);
                        } catch (OutOfMemoryError e) {
                            System.gc();
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e2) {
                                Thread.currentThread().interrupt();
                            }
                            addr = map0(imode, mapPosition, mapSize);
                        } catch (Throwable th) {
                            this.threads.remove(ti);
                            end(IOStatus.checkAll(addr));
                        }
                    } while (isOpen());
                    if (isOpen()) {
                        if (filesize < position + size) {
                            do {
                                try {
                                    if (this.nd.truncate(this.fd, position + size) != -3) {
                                        break;
                                    }
                                } catch (IOException r) {
                                    if (OsConstants.S_ISREG(Libcore.os.fstat(this.fd).st_mode)) {
                                        throw r;
                                    }
                                } catch (ErrnoException e3) {
                                    e3.rethrowAsIOException();
                                }
                            } while (isOpen());
                            if (!isOpen()) {
                                this.threads.remove(ti);
                                end(IOStatus.checkAll(addr));
                                return null;
                            }
                        }
                        if (size == 0) {
                            addr = 0;
                            FileDescriptor dummy = new FileDescriptor();
                            boolean z = (!this.writable || imode == 0) ? true : -assertionsDisabled;
                            MappedByteBuffer directByteBuffer = new DirectByteBuffer(0, 0, dummy, null, z);
                            this.threads.remove(ti);
                            end(IOStatus.checkAll(0));
                            return directByteBuffer;
                        }
                        int pagePosition = (int) (position % allocationGranularity);
                        mapPosition = position - ((long) pagePosition);
                        mapSize = size + ((long) pagePosition);
                        BlockGuard.getThreadPolicy().onReadFromDisk();
                        addr = map0(imode, mapPosition, mapSize);
                        FileDescriptor mfd = this.nd.duplicateForMapping(this.fd);
                        if (!-assertionsDisabled && !IOStatus.checkAll(addr)) {
                            throw new AssertionError();
                        } else if (-assertionsDisabled || addr % allocationGranularity == 0) {
                            int isize = (int) size;
                            Unmapper um = new Unmapper(addr, mapSize, isize, mfd, null);
                            long j = addr + ((long) pagePosition);
                            boolean z2 = (!this.writable || imode == 0) ? true : -assertionsDisabled;
                            MappedByteBuffer directByteBuffer2 = new DirectByteBuffer(isize, j, mfd, um, z2);
                            this.threads.remove(ti);
                            end(IOStatus.checkAll(addr));
                            return directByteBuffer2;
                        } else {
                            throw new AssertionError();
                        }
                    }
                    this.threads.remove(ti);
                    end(IOStatus.checkAll(addr));
                    return null;
                }
                this.threads.remove(ti);
                end(IOStatus.checkAll(addr));
                return null;
            } else {
                throw new NonReadableChannelException();
            }
        }
    }

    private static boolean isSharedFileLockTable() {
        boolean z = true;
        if (!propertyChecked) {
            synchronized (FileChannelImpl.class) {
                if (!propertyChecked) {
                    String value = (String) AccessController.doPrivileged(new GetPropertyAction("sun.nio.ch.disableSystemWideOverlappingFileLockCheck"));
                    if (value != null) {
                        z = value.equals("false");
                    }
                    isSharedFileLockTable = z;
                    propertyChecked = true;
                }
            }
        }
        return isSharedFileLockTable;
    }

    private FileLockTable fileLockTable() throws IOException {
        if (this.fileLockTable == null) {
            synchronized (this) {
                if (this.fileLockTable == null) {
                    if (isSharedFileLockTable()) {
                        int ti = this.threads.add();
                        try {
                            ensureOpen();
                            this.fileLockTable = FileLockTable.newSharedFileLockTable(this, this.fd);
                            this.threads.remove(ti);
                        } catch (Throwable th) {
                            this.threads.remove(ti);
                        }
                    } else {
                        this.fileLockTable = new SimpleFileLockTable();
                    }
                }
            }
        }
        return this.fileLockTable;
    }

    public FileLock lock(long position, long size, boolean shared) throws IOException {
        ensureOpen();
        if (shared && (this.readable ^ 1) != 0) {
            throw new NonReadableChannelException();
        } else if (shared || (this.writable ^ 1) == 0) {
            FileLockImpl fli = new FileLockImpl((FileChannel) this, position, size, shared);
            FileLockTable flt = fileLockTable();
            flt.add(fli);
            boolean completed = -assertionsDisabled;
            int ti = -1;
            begin();
            ti = this.threads.add();
            if (isOpen()) {
                int n;
                while (true) {
                    try {
                        n = this.nd.lock(this.fd, true, position, size, shared);
                        if (n == 2) {
                            if (!isOpen()) {
                                break;
                            }
                        }
                        break;
                    } finally {
                        if (null == null) {
                            flt.remove(fli);
                        }
                        this.threads.remove(ti);
                        try {
                            end(-assertionsDisabled);
                        } catch (ClosedByInterruptException e) {
                            throw new FileLockInterruptionException();
                        }
                    }
                }
                if (isOpen()) {
                    if (n == 1) {
                        if (-assertionsDisabled || shared) {
                            FileLockImpl fli2 = new FileLockImpl((FileChannel) this, position, size, (boolean) -assertionsDisabled);
                            flt.replace(fli, fli2);
                            fli = fli2;
                        } else {
                            throw new AssertionError();
                        }
                    }
                    completed = true;
                }
                if (!completed) {
                    flt.remove(fli);
                }
                this.threads.remove(ti);
                try {
                    end(completed);
                    return fli;
                } catch (ClosedByInterruptException e2) {
                    throw new FileLockInterruptionException();
                }
            }
            try {
                end(-assertionsDisabled);
                return null;
            } catch (ClosedByInterruptException e3) {
                throw new FileLockInterruptionException();
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        ensureOpen();
        if (shared && (this.readable ^ 1) != 0) {
            throw new NonReadableChannelException();
        } else if (shared || (this.writable ^ 1) == 0) {
            FileLockImpl fli = new FileLockImpl((FileChannel) this, position, size, shared);
            FileLockTable flt = fileLockTable();
            flt.add(fli);
            int ti = this.threads.add();
            try {
                ensureOpen();
                int result = this.nd.lock(this.fd, -assertionsDisabled, position, size, shared);
                if (result == -1) {
                    flt.remove(fli);
                    this.threads.remove(ti);
                    return null;
                } else if (result != 1) {
                    this.threads.remove(ti);
                    return fli;
                } else if (-assertionsDisabled || shared) {
                    FileLockImpl fli2 = new FileLockImpl((FileChannel) this, position, size, (boolean) -assertionsDisabled);
                    flt.replace(fli, fli2);
                    this.threads.remove(ti);
                    return fli2;
                } else {
                    throw new AssertionError();
                }
            } catch (IOException e) {
                flt.remove(fli);
                throw e;
            } catch (Throwable th) {
                this.threads.remove(ti);
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    void release(FileLockImpl fli) throws IOException {
        int ti = this.threads.add();
        try {
            ensureOpen();
            this.nd.release(this.fd, fli.position(), fli.size());
            if (-assertionsDisabled || this.fileLockTable != null) {
                this.fileLockTable.remove(fli);
                return;
            }
            throw new AssertionError();
        } finally {
            this.threads.remove(ti);
        }
    }
}
