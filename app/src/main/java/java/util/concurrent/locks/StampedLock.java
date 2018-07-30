package java.util.concurrent.locks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

public class StampedLock implements Serializable {
    private static final long ABITS = 255;
    private static final int CANCELLED = 1;
    private static final int HEAD_SPINS;
    private static final long INTERRUPTED = 1;
    private static final int LG_READERS = 7;
    private static final int MAX_HEAD_SPINS;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final long ORIGIN = 256;
    private static final int OVERFLOW_YIELD_RATE = 7;
    private static final long PARKBLOCKER;
    private static final long RBITS = 127;
    private static final long RFULL = 126;
    private static final int RMODE = 0;
    private static final long RUNIT = 1;
    private static final long SBITS = -128;
    private static final int SPINS = (NCPU > 1 ? 64 : 0);
    private static final long STATE;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final int WAITING = -1;
    private static final long WBIT = 128;
    private static final long WCOWAIT;
    private static final long WHEAD;
    private static final int WMODE = 1;
    private static final long WNEXT;
    private static final long WSTATUS;
    private static final long WTAIL;
    private static final long serialVersionUID = -6001602636862214147L;
    transient ReadLockView readLockView;
    transient ReadWriteLockView readWriteLockView;
    private transient int readerOverflow;
    private volatile transient long state = ORIGIN;
    private volatile transient WNode whead;
    transient WriteLockView writeLockView;
    private volatile transient WNode wtail;

    final class ReadLockView implements Lock {
        ReadLockView() {
        }

        public void lock() {
            StampedLock.this.readLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            StampedLock.this.readLockInterruptibly();
        }

        public boolean tryLock() {
            return StampedLock.this.tryReadLock() != 0;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return StampedLock.this.tryReadLock(time, unit) != 0;
        }

        public void unlock() {
            StampedLock.this.unstampedUnlockRead();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock {
        ReadWriteLockView() {
        }

        public Lock readLock() {
            return StampedLock.this.asReadLock();
        }

        public Lock writeLock() {
            return StampedLock.this.asWriteLock();
        }
    }

    static final class WNode {
        volatile WNode cowait;
        final int mode;
        volatile WNode next;
        volatile WNode prev;
        volatile int status;
        volatile Thread thread;

        WNode(int m, WNode p) {
            this.mode = m;
            this.prev = p;
        }
    }

    final class WriteLockView implements Lock {
        WriteLockView() {
        }

        public void lock() {
            StampedLock.this.writeLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            StampedLock.this.writeLockInterruptibly();
        }

        public boolean tryLock() {
            return StampedLock.this.tryWriteLock() != 0;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return StampedLock.this.tryWriteLock(time, unit) != 0;
        }

        public void unlock() {
            StampedLock.this.unstampedUnlockWrite();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    static {
        int i;
        int i2 = 0;
        if (NCPU > 1) {
            i = 1024;
        } else {
            i = 0;
        }
        HEAD_SPINS = i;
        if (NCPU > 1) {
            i2 = 65536;
        }
        MAX_HEAD_SPINS = i2;
        try {
            STATE = U.objectFieldOffset(StampedLock.class.getDeclaredField("state"));
            WHEAD = U.objectFieldOffset(StampedLock.class.getDeclaredField("whead"));
            WTAIL = U.objectFieldOffset(StampedLock.class.getDeclaredField("wtail"));
            WSTATUS = U.objectFieldOffset(WNode.class.getDeclaredField("status"));
            WNEXT = U.objectFieldOffset(WNode.class.getDeclaredField("next"));
            WCOWAIT = U.objectFieldOffset(WNode.class.getDeclaredField("cowait"));
            PARKBLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    public long writeLock() {
        long s = this.state;
        if ((ABITS & s) == 0) {
            long next = s + WBIT;
            if (U.compareAndSwapLong(this, STATE, s, next)) {
                return next;
            }
        }
        return acquireWrite(false, 0);
    }

    public long tryWriteLock() {
        long s = this.state;
        if ((ABITS & s) == 0) {
            long next = s + WBIT;
            if (U.compareAndSwapLong(this, STATE, s, next)) {
                return next;
            }
        }
        return 0;
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long next = tryWriteLock();
            if (next != 0) {
                return next;
            }
            if (nanos <= 0) {
                return 0;
            }
            long deadline = System.nanoTime() + nanos;
            if (deadline == 0) {
                deadline = 1;
            }
            next = acquireWrite(true, deadline);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long writeLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long next = acquireWrite(true, 0);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long readLock() {
        long s = this.state;
        if (this.whead == this.wtail && (ABITS & s) < RFULL) {
            long next = s + 1;
            if (U.compareAndSwapLong(this, STATE, s, next)) {
                return next;
            }
        }
        return acquireRead(false, 0);
    }

    public long tryReadLock() {
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m == WBIT) {
                return 0;
            }
            long next;
            if (m < RFULL) {
                next = s + 1;
                if (U.compareAndSwapLong(this, STATE, s, next)) {
                    return next;
                }
            } else {
                next = tryIncReaderOverflow(s);
                if (next != 0) {
                    return next;
                }
            }
        }
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long next;
            long s = this.state;
            long m = s & ABITS;
            if (m != WBIT) {
                if (m < RFULL) {
                    next = s + 1;
                    if (U.compareAndSwapLong(this, STATE, s, next)) {
                        return next;
                    }
                }
                next = tryIncReaderOverflow(s);
                if (next != 0) {
                    return next;
                }
            }
            if (nanos <= 0) {
                return 0;
            }
            long deadline = System.nanoTime() + nanos;
            if (deadline == 0) {
                deadline = 1;
            }
            next = acquireRead(true, deadline);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long readLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long next = acquireRead(true, 0);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long tryOptimisticRead() {
        long s = this.state;
        return (WBIT & s) == 0 ? SBITS & s : 0;
    }

    public boolean validate(long stamp) {
        U.loadFence();
        return (stamp & SBITS) == (this.state & SBITS);
    }

    public void unlockWrite(long stamp) {
        if (this.state != stamp || (stamp & WBIT) == 0) {
            throw new IllegalMonitorStateException();
        }
        long j;
        Unsafe unsafe = U;
        long j2 = STATE;
        stamp += WBIT;
        if (stamp == 0) {
            j = ORIGIN;
        } else {
            j = stamp;
        }
        unsafe.putLongVolatile(this, j2, j);
        WNode h = this.whead;
        if (h != null && h.status != 0) {
            release(h);
        }
    }

    public void unlockRead(long stamp) {
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp) || (ABITS & stamp) == 0) {
                break;
            }
            long m = s & ABITS;
            if (m == 0 || m == WBIT) {
                break;
            } else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                    if (m == 1) {
                        WNode h = this.whead;
                        if (h != null && h.status != 0) {
                            release(h);
                            return;
                        }
                        return;
                    }
                    return;
                }
            } else if (tryDecReaderOverflow(s) != 0) {
                return;
            }
        }
        throw new IllegalMonitorStateException();
    }

    public void unlock(long stamp) {
        long a = stamp & ABITS;
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp)) {
                break;
            }
            long m = s & ABITS;
            if (m != 0) {
                WNode h;
                if (m != WBIT) {
                    if (a == 0 || a >= WBIT) {
                        break;
                    } else if (m < RFULL) {
                        if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                            if (m == 1) {
                                h = this.whead;
                                if (!(h == null || h.status == 0)) {
                                    release(h);
                                }
                            }
                            return;
                        }
                    } else if (tryDecReaderOverflow(s) != 0) {
                        return;
                    }
                } else if (a == m) {
                    Unsafe unsafe = U;
                    long j = STATE;
                    s += WBIT;
                    if (s == 0) {
                        s = ORIGIN;
                    }
                    unsafe.putLongVolatile(this, j, s);
                    h = this.whead;
                    if (!(h == null || h.status == 0)) {
                        release(h);
                    }
                    return;
                }
            } else {
                break;
            }
        }
        throw new IllegalMonitorStateException();
    }

    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS;
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp)) {
                break;
            }
            long m = s & ABITS;
            long next;
            if (m != 0) {
                if (m != WBIT) {
                    if (m != 1 || a == 0) {
                        break;
                    }
                    next = (s - 1) + WBIT;
                    if (U.compareAndSwapLong(this, STATE, s, next)) {
                        return next;
                    }
                } else if (a == m) {
                    return stamp;
                }
            } else if (a != 0) {
                break;
            } else {
                next = s + WBIT;
                if (U.compareAndSwapLong(this, STATE, s, next)) {
                    return next;
                }
            }
        }
        return 0;
    }

    public long tryConvertToReadLock(long stamp) {
        long a = stamp & ABITS;
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp)) {
                break;
            }
            long m = s & ABITS;
            long next;
            if (m == 0) {
                if (a != 0) {
                    break;
                } else if (m < RFULL) {
                    next = s + 1;
                    if (U.compareAndSwapLong(this, STATE, s, next)) {
                        return next;
                    }
                } else {
                    next = tryIncReaderOverflow(s);
                    if (next != 0) {
                        return next;
                    }
                }
            } else if (m == WBIT) {
                if (a == m) {
                    next = s + 129;
                    U.putLongVolatile(this, STATE, next);
                    WNode h = this.whead;
                    if (!(h == null || h.status == 0)) {
                        release(h);
                    }
                    return next;
                }
            } else if (a != 0 && a < WBIT) {
                return stamp;
            }
        }
        return 0;
    }

    public long tryConvertToOptimisticRead(long stamp) {
        long a = stamp & ABITS;
        U.loadFence();
        while (true) {
            long s = this.state;
            if ((SBITS & s) == (SBITS & stamp)) {
                long m = s & ABITS;
                if (m != 0) {
                    long next;
                    WNode h;
                    if (m != WBIT) {
                        if (a == 0 || a >= WBIT) {
                            break;
                        } else if (m < RFULL) {
                            next = s - 1;
                            if (U.compareAndSwapLong(this, STATE, s, next)) {
                                if (m == 1) {
                                    h = this.whead;
                                    if (!(h == null || h.status == 0)) {
                                        release(h);
                                    }
                                }
                                return SBITS & next;
                            }
                        } else {
                            next = tryDecReaderOverflow(s);
                            if (next != 0) {
                                return SBITS & next;
                            }
                        }
                    } else if (a == m) {
                        Unsafe unsafe = U;
                        long j = STATE;
                        s += WBIT;
                        if (s == 0) {
                            next = ORIGIN;
                        } else {
                            next = s;
                        }
                        unsafe.putLongVolatile(this, j, next);
                        h = this.whead;
                        if (!(h == null || h.status == 0)) {
                            release(h);
                        }
                        return next;
                    }
                } else if (a == 0) {
                    return s;
                }
            } else {
                break;
            }
        }
        return 0;
    }

    public boolean tryUnlockWrite() {
        long s = this.state;
        if ((s & WBIT) == 0) {
            return false;
        }
        long j;
        Unsafe unsafe = U;
        long j2 = STATE;
        s += WBIT;
        if (s == 0) {
            j = ORIGIN;
        } else {
            j = s;
        }
        unsafe.putLongVolatile(this, j2, j);
        WNode h = this.whead;
        if (!(h == null || h.status == 0)) {
            release(h);
        }
        return true;
    }

    public boolean tryUnlockRead() {
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m != 0 && m < WBIT) {
                if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                        if (m == 1) {
                            WNode h = this.whead;
                            if (!(h == null || h.status == 0)) {
                                release(h);
                            }
                        }
                        return true;
                    }
                } else if (tryDecReaderOverflow(s) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getReadLockCount(long s) {
        long readers = s & RBITS;
        if (readers >= RFULL) {
            readers = RFULL + ((long) this.readerOverflow);
        }
        return (int) readers;
    }

    public boolean isWriteLocked() {
        return (this.state & WBIT) != 0;
    }

    public boolean isReadLocked() {
        return (this.state & RBITS) != 0;
    }

    public int getReadLockCount() {
        return getReadLockCount(this.state);
    }

    public String toString() {
        String str;
        long s = this.state;
        StringBuilder append = new StringBuilder().append(super.toString());
        if ((ABITS & s) == 0) {
            str = "[Unlocked]";
        } else if ((WBIT & s) != 0) {
            str = "[Write-locked]";
        } else {
            str = "[Read-locks:" + getReadLockCount(s) + "]";
        }
        return append.append(str).toString();
    }

    public Lock asReadLock() {
        ReadLockView v = this.readLockView;
        if (v != null) {
            return v;
        }
        v = new ReadLockView();
        this.readLockView = v;
        return v;
    }

    public Lock asWriteLock() {
        WriteLockView v = this.writeLockView;
        if (v != null) {
            return v;
        }
        v = new WriteLockView();
        this.writeLockView = v;
        return v;
    }

    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v = this.readWriteLockView;
        if (v != null) {
            return v;
        }
        v = new ReadWriteLockView();
        this.readWriteLockView = v;
        return v;
    }

    final void unstampedUnlockWrite() {
        long s = this.state;
        if ((s & WBIT) == 0) {
            throw new IllegalMonitorStateException();
        }
        long j;
        Unsafe unsafe = U;
        long j2 = STATE;
        s += WBIT;
        if (s == 0) {
            j = ORIGIN;
        } else {
            j = s;
        }
        unsafe.putLongVolatile(this, j2, j);
        WNode h = this.whead;
        if (h != null && h.status != 0) {
            release(h);
        }
    }

    final void unstampedUnlockRead() {
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m != 0 && m < WBIT) {
                if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                        if (m == 1) {
                            WNode h = this.whead;
                            if (h != null && h.status != 0) {
                                release(h);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                } else if (tryDecReaderOverflow(s) != 0) {
                    return;
                }
            }
        }
        throw new IllegalMonitorStateException();
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        U.putLongVolatile(this, STATE, ORIGIN);
    }

    private long tryIncReaderOverflow(long s) {
        if ((ABITS & s) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                this.readerOverflow++;
                U.putLongVolatile(this, STATE, s);
                return s;
            }
        } else if ((LockSupport.nextSecondarySeed() & 7) == 0) {
            Thread.yield();
        }
        return 0;
    }

    private long tryDecReaderOverflow(long s) {
        if ((ABITS & s) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, RBITS | s)) {
                long next;
                int r = this.readerOverflow;
                if (r > 0) {
                    this.readerOverflow = r - 1;
                    next = s;
                } else {
                    next = s - 1;
                }
                U.putLongVolatile(this, STATE, next);
                return next;
            }
        } else if ((LockSupport.nextSecondarySeed() & 7) == 0) {
            Thread.yield();
        }
        return 0;
    }

    private void release(WNode h) {
        if (h != null) {
            U.compareAndSwapInt(h, WSTATUS, -1, 0);
            WNode q = h.next;
            if (q == null || q.status == 1) {
                WNode t = this.wtail;
                while (t != null && t != h) {
                    if (t.status <= 0) {
                        q = t;
                    }
                    t = t.prev;
                }
            }
            if (q != null) {
                Thread w = q.thread;
                if (w != null) {
                    U.unpark(w);
                }
            }
        }
    }

    private long acquireWrite(boolean r49, long r50) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r23_2 'node' java.lang.Object) in PHI: PHI: (r23_3 'node' java.lang.Object) = (r23_1 'node' java.lang.Object), (r23_1 'node' java.lang.Object), (r23_1 'node' java.lang.Object), (r23_1 'node' java.lang.Object), (r23_1 'node' java.lang.Object), (r23_1 'node' java.lang.Object), (r23_1 'node' java.lang.Object), (r23_2 'node' java.lang.Object), (r23_1 'node' java.lang.Object), (r23_1 'node' java.lang.Object) binds: {(r23_1 'node' java.lang.Object)=B:99:0x0004, (r23_1 'node' java.lang.Object)=B:100:0x0004, (r23_1 'node' java.lang.Object)=B:101:0x0004, (r23_1 'node' java.lang.Object)=B:103:0x0004, (r23_1 'node' java.lang.Object)=B:102:0x0004, (r23_1 'node' java.lang.Object)=B:105:0x0004, (r23_1 'node' java.lang.Object)=B:104:0x0004, (r23_2 'node' java.lang.Object)=B:106:0x0004, (r23_1 'node' java.lang.Object)=B:107:0x0004, (r23_1 'node' java.lang.Object)=B:108:0x0004}
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
        r48 = this;
        r23 = 0;
        r42 = -1;
    L_0x0004:
        r0 = r48;
        r8 = r0.state;
        r4 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r38 = r8 & r4;
        r4 = 0;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 != 0) goto L_0x0023;
    L_0x0012:
        r4 = U;
        r6 = STATE;
        r12 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r10 = r8 + r12;
        r5 = r48;
        r4 = r4.compareAndSwapLong(r5, r6, r8, r10);
        if (r4 == 0) goto L_0x0004;
    L_0x0022:
        return r10;
    L_0x0023:
        if (r42 >= 0) goto L_0x003b;
    L_0x0025:
        r4 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 != 0) goto L_0x0038;
    L_0x002b:
        r0 = r48;
        r4 = r0.wtail;
        r0 = r48;
        r5 = r0.whead;
        if (r4 != r5) goto L_0x0038;
    L_0x0035:
        r42 = SPINS;
        goto L_0x0004;
    L_0x0038:
        r42 = 0;
        goto L_0x0004;
    L_0x003b:
        if (r42 <= 0) goto L_0x0046;
    L_0x003d:
        r4 = java.util.concurrent.locks.LockSupport.nextSecondarySeed();
        if (r4 < 0) goto L_0x0004;
    L_0x0043:
        r42 = r42 + -1;
        goto L_0x0004;
    L_0x0046:
        r0 = r48;
        r0 = r0.wtail;
        r22 = r0;
        if (r22 != 0) goto L_0x006c;
    L_0x004e:
        r17 = new java.util.concurrent.locks.StampedLock$WNode;
        r4 = 1;
        r5 = 0;
        r0 = r17;
        r0.<init>(r4, r5);
        r12 = U;
        r14 = WHEAD;
        r16 = 0;
        r13 = r48;
        r4 = r12.compareAndSwapObject(r13, r14, r16, r17);
        if (r4 == 0) goto L_0x0004;
    L_0x0065:
        r0 = r17;
        r1 = r48;
        r1.wtail = r0;
        goto L_0x0004;
    L_0x006c:
        if (r23 != 0) goto L_0x0079;
    L_0x006e:
        r23 = new java.util.concurrent.locks.StampedLock$WNode;
        r4 = 1;
        r0 = r23;
        r1 = r22;
        r0.<init>(r4, r1);
        goto L_0x0004;
    L_0x0079:
        r0 = r23;
        r4 = r0.prev;
        r0 = r22;
        if (r4 == r0) goto L_0x0089;
    L_0x0081:
        r0 = r22;
        r1 = r23;
        r1.prev = r0;
        goto L_0x0004;
    L_0x0089:
        r18 = U;
        r20 = WTAIL;
        r19 = r48;
        r4 = r18.compareAndSwapObject(r19, r20, r22, r23);
        if (r4 == 0) goto L_0x0004;
    L_0x0095:
        r0 = r23;
        r1 = r22;
        r1.next = r0;
        r46 = 0;
        r42 = -1;
    L_0x009f:
        r0 = r48;
        r0 = r0.whead;
        r25 = r0;
        r0 = r25;
        r1 = r22;
        if (r0 != r1) goto L_0x0115;
    L_0x00ab:
        if (r42 >= 0) goto L_0x00e3;
    L_0x00ad:
        r42 = HEAD_SPINS;
    L_0x00af:
        r36 = r42;
    L_0x00b1:
        r0 = r48;
        r8 = r0.state;
        r4 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r4 = r4 & r8;
        r6 = 0;
        r4 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1));
        if (r4 != 0) goto L_0x00ec;
    L_0x00be:
        r4 = U;
        r6 = STATE;
        r12 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r10 = r8 + r12;
        r5 = r48;
        r4 = r4.compareAndSwapLong(r5, r6, r8, r10);
        if (r4 == 0) goto L_0x00b1;
    L_0x00ce:
        r0 = r23;
        r1 = r48;
        r1.whead = r0;
        r4 = 0;
        r0 = r23;
        r0.prev = r4;
        if (r46 == 0) goto L_0x00e2;
    L_0x00db:
        r4 = java.lang.Thread.currentThread();
        r4.interrupt();
    L_0x00e2:
        return r10;
    L_0x00e3:
        r4 = MAX_HEAD_SPINS;
        r0 = r42;
        if (r0 >= r4) goto L_0x00af;
    L_0x00e9:
        r42 = r42 << 1;
        goto L_0x00af;
    L_0x00ec:
        r4 = java.util.concurrent.locks.LockSupport.nextSecondarySeed();
        if (r4 < 0) goto L_0x00b1;
    L_0x00f2:
        r36 = r36 + -1;
        if (r36 > 0) goto L_0x00b1;
    L_0x00f6:
        r0 = r48;
        r4 = r0.whead;
        r0 = r25;
        if (r4 != r0) goto L_0x009f;
    L_0x00fe:
        r0 = r23;
        r0 = r0.prev;
        r37 = r0;
        r0 = r37;
        r1 = r22;
        if (r0 == r1) goto L_0x013f;
    L_0x010a:
        if (r37 == 0) goto L_0x009f;
    L_0x010c:
        r22 = r37;
        r0 = r23;
        r1 = r37;
        r1.next = r0;
        goto L_0x009f;
    L_0x0115:
        if (r25 == 0) goto L_0x00f6;
    L_0x0117:
        r0 = r25;
        r0 = r0.cowait;
        r28 = r0;
        if (r28 == 0) goto L_0x00f6;
    L_0x011f:
        r24 = U;
        r26 = WCOWAIT;
        r0 = r28;
        r0 = r0.cowait;
        r29 = r0;
        r4 = r24.compareAndSwapObject(r25, r26, r28, r29);
        if (r4 == 0) goto L_0x0117;
    L_0x012f:
        r0 = r28;
        r0 = r0.thread;
        r43 = r0;
        if (r43 == 0) goto L_0x0117;
    L_0x0137:
        r4 = U;
        r0 = r43;
        r4.unpark(r0);
        goto L_0x0117;
    L_0x013f:
        r0 = r22;
        r0 = r0.status;
        r41 = r0;
        if (r41 != 0) goto L_0x0156;
    L_0x0147:
        r30 = U;
        r32 = WSTATUS;
        r34 = 0;
        r35 = -1;
        r31 = r22;
        r30.compareAndSwapInt(r31, r32, r34, r35);
        goto L_0x009f;
    L_0x0156:
        r4 = 1;
        r0 = r41;
        if (r0 != r4) goto L_0x0171;
    L_0x015b:
        r0 = r22;
        r0 = r0.prev;
        r40 = r0;
        if (r40 == 0) goto L_0x009f;
    L_0x0163:
        r0 = r40;
        r1 = r23;
        r1.prev = r0;
        r0 = r23;
        r1 = r40;
        r1.next = r0;
        goto L_0x009f;
    L_0x0171:
        r4 = 0;
        r4 = (r50 > r4 ? 1 : (r50 == r4 ? 0 : -1));
        if (r4 != 0) goto L_0x01e2;
    L_0x0177:
        r44 = 0;
    L_0x0179:
        r47 = java.lang.Thread.currentThread();
        r4 = U;
        r6 = PARKBLOCKER;
        r0 = r47;
        r1 = r48;
        r4.putObject(r0, r6, r1);
        r0 = r47;
        r1 = r23;
        r1.thread = r0;
        r0 = r22;
        r4 = r0.status;
        if (r4 >= 0) goto L_0x01bf;
    L_0x0194:
        r0 = r22;
        r1 = r25;
        if (r0 != r1) goto L_0x01a7;
    L_0x019a:
        r0 = r48;
        r4 = r0.state;
        r6 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r4 = r4 & r6;
        r6 = 0;
        r4 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1));
        if (r4 == 0) goto L_0x01bf;
    L_0x01a7:
        r0 = r48;
        r4 = r0.whead;
        r0 = r25;
        if (r4 != r0) goto L_0x01bf;
    L_0x01af:
        r0 = r23;
        r4 = r0.prev;
        r0 = r22;
        if (r4 != r0) goto L_0x01bf;
    L_0x01b7:
        r4 = U;
        r5 = 0;
        r0 = r44;
        r4.park(r5, r0);
    L_0x01bf:
        r4 = 0;
        r0 = r23;
        r0.thread = r4;
        r4 = U;
        r6 = PARKBLOCKER;
        r5 = 0;
        r0 = r47;
        r4.putObject(r0, r6, r5);
        r4 = java.lang.Thread.interrupted();
        if (r4 == 0) goto L_0x009f;
    L_0x01d4:
        if (r49 == 0) goto L_0x01fa;
    L_0x01d6:
        r4 = 1;
        r0 = r48;
        r1 = r23;
        r2 = r23;
        r4 = r0.cancelWaiter(r1, r2, r4);
        return r4;
    L_0x01e2:
        r4 = java.lang.System.nanoTime();
        r44 = r50 - r4;
        r4 = 0;
        r4 = (r44 > r4 ? 1 : (r44 == r4 ? 0 : -1));
        if (r4 > 0) goto L_0x0179;
    L_0x01ee:
        r4 = 0;
        r0 = r48;
        r1 = r23;
        r2 = r23;
        r4 = r0.cancelWaiter(r1, r2, r4);
        return r4;
    L_0x01fa:
        r46 = 1;
        goto L_0x009f;
        */
        throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.locks.StampedLock.acquireWrite(boolean, long):long");
    }

    private long acquireRead(boolean r51, long r52) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r23_2 'node' java.lang.Object) in PHI: PHI: (r23_4 'node' java.lang.Object) = (r23_1 'node' java.lang.Object), (r23_1 'node' java.lang.Object), (r23_2 'node' java.lang.Object), (r23_1 'node' java.lang.Object), (r23_3 'node' java.lang.Object), (r23_1 'node' java.lang.Object) binds: {(r23_1 'node' java.lang.Object)=B:201:0x0006, (r23_1 'node' java.lang.Object)=B:206:0x0006, (r23_2 'node' java.lang.Object)=B:202:0x0006, (r23_1 'node' java.lang.Object)=B:204:0x0006, (r23_3 'node' java.lang.Object)=B:205:0x0006, (r23_1 'node' java.lang.Object)=B:203:0x0006}
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
        r48 = 0;
        r23 = 0;
        r44 = -1;
    L_0x0006:
        r0 = r50;
        r0 = r0.whead;
        r25 = r0;
        r0 = r50;
        r0 = r0.wtail;
        r22 = r0;
        r0 = r25;
        r1 = r22;
        if (r0 != r1) goto L_0x03f7;
    L_0x0018:
        r0 = r50;
        r8 = r0.state;
        r4 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r38 = r8 & r4;
        r4 = 126; // 0x7e float:1.77E-43 double:6.23E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 >= 0) goto L_0x0040;
    L_0x0026:
        r4 = U;
        r6 = STATE;
        r12 = 1;
        r10 = r8 + r12;
        r5 = r50;
        r4 = r4.compareAndSwapLong(r5, r6, r8, r10);
        if (r4 == 0) goto L_0x0052;
    L_0x0036:
        if (r48 == 0) goto L_0x003f;
    L_0x0038:
        r4 = java.lang.Thread.currentThread();
        r4.interrupt();
    L_0x003f:
        return r10;
    L_0x0040:
        r4 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 >= 0) goto L_0x0052;
    L_0x0046:
        r0 = r50;
        r10 = r0.tryIncReaderOverflow(r8);
        r4 = 0;
        r4 = (r10 > r4 ? 1 : (r10 == r4 ? 0 : -1));
        if (r4 != 0) goto L_0x0036;
    L_0x0052:
        r4 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 < 0) goto L_0x0018;
    L_0x0058:
        if (r44 <= 0) goto L_0x0063;
    L_0x005a:
        r4 = java.util.concurrent.locks.LockSupport.nextSecondarySeed();
        if (r4 < 0) goto L_0x0018;
    L_0x0060:
        r44 = r44 + -1;
        goto L_0x0018;
    L_0x0063:
        if (r44 != 0) goto L_0x00ac;
    L_0x0065:
        r0 = r50;
        r0 = r0.whead;
        r40 = r0;
        r0 = r50;
        r0 = r0.wtail;
        r41 = r0;
        r0 = r40;
        r1 = r25;
        if (r0 != r1) goto L_0x00a2;
    L_0x0077:
        r0 = r41;
        r1 = r22;
        if (r0 != r1) goto L_0x00a2;
    L_0x007d:
        r36 = r25;
    L_0x007f:
        if (r22 != 0) goto L_0x00b0;
    L_0x0081:
        r17 = new java.util.concurrent.locks.StampedLock$WNode;
        r4 = 1;
        r5 = 0;
        r0 = r17;
        r0.<init>(r4, r5);
        r12 = U;
        r14 = WHEAD;
        r16 = 0;
        r13 = r50;
        r4 = r12.compareAndSwapObject(r13, r14, r16, r17);
        if (r4 == 0) goto L_0x03f3;
    L_0x0098:
        r0 = r17;
        r1 = r50;
        r1.wtail = r0;
        r25 = r36;
        goto L_0x0006;
    L_0x00a2:
        r25 = r40;
        r22 = r41;
        r0 = r40;
        r1 = r41;
        if (r0 != r1) goto L_0x007d;
    L_0x00ac:
        r44 = SPINS;
        goto L_0x0018;
    L_0x00b0:
        if (r23 != 0) goto L_0x00c0;
    L_0x00b2:
        r23 = new java.util.concurrent.locks.StampedLock$WNode;
        r4 = 0;
        r0 = r23;
        r1 = r22;
        r0.<init>(r4, r1);
        r25 = r36;
        goto L_0x0006;
    L_0x00c0:
        r0 = r36;
        r1 = r22;
        if (r0 == r1) goto L_0x00cc;
    L_0x00c6:
        r0 = r22;
        r4 = r0.mode;
        if (r4 == 0) goto L_0x0159;
    L_0x00cc:
        r0 = r23;
        r4 = r0.prev;
        r0 = r22;
        if (r4 == r0) goto L_0x00de;
    L_0x00d4:
        r0 = r22;
        r1 = r23;
        r1.prev = r0;
    L_0x00da:
        r25 = r36;
        goto L_0x0006;
    L_0x00de:
        r18 = U;
        r20 = WTAIL;
        r19 = r50;
        r4 = r18.compareAndSwapObject(r19, r20, r22, r23);
        if (r4 == 0) goto L_0x00da;
    L_0x00ea:
        r0 = r23;
        r1 = r22;
        r1.next = r0;
        r44 = -1;
    L_0x00f2:
        r0 = r50;
        r0 = r0.whead;
        r25 = r0;
        r0 = r25;
        r1 = r22;
        if (r0 != r1) goto L_0x0306;
    L_0x00fe:
        if (r44 >= 0) goto L_0x02b0;
    L_0x0100:
        r44 = HEAD_SPINS;
    L_0x0102:
        r37 = r44;
    L_0x0104:
        r0 = r50;
        r8 = r0.state;
        r4 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r38 = r8 & r4;
        r4 = 126; // 0x7e float:1.77E-43 double:6.23E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 >= 0) goto L_0x02ba;
    L_0x0112:
        r4 = U;
        r6 = STATE;
        r12 = 1;
        r10 = r8 + r12;
        r5 = r50;
        r4 = r4.compareAndSwapLong(r5, r6, r8, r10);
        if (r4 == 0) goto L_0x02cc;
    L_0x0122:
        r0 = r23;
        r1 = r50;
        r1.whead = r0;
        r4 = 0;
        r0 = r23;
        r0.prev = r4;
    L_0x012d:
        r0 = r23;
        r0 = r0.cowait;
        r28 = r0;
        if (r28 == 0) goto L_0x02fc;
    L_0x0135:
        r30 = U;
        r32 = WCOWAIT;
        r0 = r28;
        r0 = r0.cowait;
        r35 = r0;
        r31 = r23;
        r34 = r28;
        r4 = r30.compareAndSwapObject(r31, r32, r34, r35);
        if (r4 == 0) goto L_0x012d;
    L_0x0149:
        r0 = r28;
        r0 = r0.thread;
        r45 = r0;
        if (r45 == 0) goto L_0x012d;
    L_0x0151:
        r4 = U;
        r0 = r45;
        r4.unpark(r0);
        goto L_0x012d;
    L_0x0159:
        r24 = U;
        r26 = WCOWAIT;
        r0 = r22;
        r0 = r0.cowait;
        r28 = r0;
        r0 = r28;
        r1 = r23;
        r1.cowait = r0;
        r25 = r22;
        r29 = r23;
        r4 = r24.compareAndSwapObject(r25, r26, r28, r29);
        if (r4 != 0) goto L_0x03ef;
    L_0x0173:
        r4 = 0;
        r0 = r23;
        r0.cowait = r4;
        r25 = r36;
        goto L_0x0006;
    L_0x017c:
        r48 = 1;
    L_0x017e:
        r0 = r50;
        r0 = r0.whead;
        r25 = r0;
        if (r25 == 0) goto L_0x01ad;
    L_0x0186:
        r0 = r25;
        r0 = r0.cowait;
        r28 = r0;
        if (r28 == 0) goto L_0x01ad;
    L_0x018e:
        r24 = U;
        r26 = WCOWAIT;
        r0 = r28;
        r0 = r0.cowait;
        r29 = r0;
        r4 = r24.compareAndSwapObject(r25, r26, r28, r29);
        if (r4 == 0) goto L_0x01ad;
    L_0x019e:
        r0 = r28;
        r0 = r0.thread;
        r45 = r0;
        if (r45 == 0) goto L_0x01ad;
    L_0x01a6:
        r4 = U;
        r0 = r45;
        r4.unpark(r0);
    L_0x01ad:
        r0 = r22;
        r0 = r0.prev;
        r42 = r0;
        r0 = r25;
        r1 = r42;
        if (r0 == r1) goto L_0x01bf;
    L_0x01b9:
        r0 = r25;
        r1 = r22;
        if (r0 != r1) goto L_0x01e7;
    L_0x01bf:
        r0 = r50;
        r8 = r0.state;
        r4 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r38 = r8 & r4;
        r4 = 126; // 0x7e float:1.77E-43 double:6.23E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 >= 0) goto L_0x0205;
    L_0x01cd:
        r4 = U;
        r6 = STATE;
        r12 = 1;
        r10 = r8 + r12;
        r5 = r50;
        r4 = r4.compareAndSwapLong(r5, r6, r8, r10);
        if (r4 == 0) goto L_0x0217;
    L_0x01dd:
        if (r48 == 0) goto L_0x01e6;
    L_0x01df:
        r4 = java.lang.Thread.currentThread();
        r4.interrupt();
    L_0x01e6:
        return r10;
    L_0x01e7:
        if (r42 == 0) goto L_0x01bf;
    L_0x01e9:
        r0 = r50;
        r4 = r0.whead;
        r0 = r25;
        if (r4 != r0) goto L_0x017e;
    L_0x01f1:
        r0 = r22;
        r4 = r0.prev;
        r0 = r42;
        if (r4 != r0) goto L_0x017e;
    L_0x01f9:
        if (r42 == 0) goto L_0x0201;
    L_0x01fb:
        r0 = r25;
        r1 = r22;
        if (r0 != r1) goto L_0x021e;
    L_0x0201:
        r23 = 0;
        goto L_0x0006;
    L_0x0205:
        r4 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 >= 0) goto L_0x0217;
    L_0x020b:
        r0 = r50;
        r10 = r0.tryIncReaderOverflow(r8);
        r4 = 0;
        r4 = (r10 > r4 ? 1 : (r10 == r4 ? 0 : -1));
        if (r4 != 0) goto L_0x01dd;
    L_0x0217:
        r4 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 >= 0) goto L_0x01e9;
    L_0x021d:
        goto L_0x01bf;
    L_0x021e:
        r0 = r22;
        r4 = r0.status;
        if (r4 > 0) goto L_0x0201;
    L_0x0224:
        r4 = 0;
        r4 = (r52 > r4 ? 1 : (r52 == r4 ? 0 : -1));
        if (r4 != 0) goto L_0x028f;
    L_0x022a:
        r46 = 0;
    L_0x022c:
        r49 = java.lang.Thread.currentThread();
        r4 = U;
        r6 = PARKBLOCKER;
        r0 = r49;
        r1 = r50;
        r4.putObject(r0, r6, r1);
        r0 = r49;
        r1 = r23;
        r1.thread = r0;
        r0 = r25;
        r1 = r42;
        if (r0 != r1) goto L_0x0254;
    L_0x0247:
        r0 = r50;
        r4 = r0.state;
        r6 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r4 = r4 & r6;
        r6 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r4 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1));
        if (r4 != 0) goto L_0x026c;
    L_0x0254:
        r0 = r50;
        r4 = r0.whead;
        r0 = r25;
        if (r4 != r0) goto L_0x026c;
    L_0x025c:
        r0 = r22;
        r4 = r0.prev;
        r0 = r42;
        if (r4 != r0) goto L_0x026c;
    L_0x0264:
        r4 = U;
        r5 = 0;
        r0 = r46;
        r4.park(r5, r0);
    L_0x026c:
        r4 = 0;
        r0 = r23;
        r0.thread = r4;
        r4 = U;
        r6 = PARKBLOCKER;
        r5 = 0;
        r0 = r49;
        r4.putObject(r0, r6, r5);
        r4 = java.lang.Thread.interrupted();
        if (r4 == 0) goto L_0x017e;
    L_0x0281:
        if (r51 == 0) goto L_0x017c;
    L_0x0283:
        r4 = 1;
        r0 = r50;
        r1 = r23;
        r2 = r22;
        r4 = r0.cancelWaiter(r1, r2, r4);
        return r4;
    L_0x028f:
        r4 = java.lang.System.nanoTime();
        r46 = r52 - r4;
        r4 = 0;
        r4 = (r46 > r4 ? 1 : (r46 == r4 ? 0 : -1));
        if (r4 > 0) goto L_0x022c;
    L_0x029b:
        if (r48 == 0) goto L_0x02a4;
    L_0x029d:
        r4 = java.lang.Thread.currentThread();
        r4.interrupt();
    L_0x02a4:
        r4 = 0;
        r0 = r50;
        r1 = r23;
        r2 = r22;
        r4 = r0.cancelWaiter(r1, r2, r4);
        return r4;
    L_0x02b0:
        r4 = MAX_HEAD_SPINS;
        r0 = r44;
        if (r0 >= r4) goto L_0x0102;
    L_0x02b6:
        r44 = r44 << 1;
        goto L_0x0102;
    L_0x02ba:
        r4 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 >= 0) goto L_0x02cc;
    L_0x02c0:
        r0 = r50;
        r10 = r0.tryIncReaderOverflow(r8);
        r4 = 0;
        r4 = (r10 > r4 ? 1 : (r10 == r4 ? 0 : -1));
        if (r4 != 0) goto L_0x0122;
    L_0x02cc:
        r4 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r4 = (r38 > r4 ? 1 : (r38 == r4 ? 0 : -1));
        if (r4 < 0) goto L_0x0104;
    L_0x02d2:
        r4 = java.util.concurrent.locks.LockSupport.nextSecondarySeed();
        if (r4 < 0) goto L_0x0104;
    L_0x02d8:
        r37 = r37 + -1;
        if (r37 > 0) goto L_0x0104;
    L_0x02dc:
        r0 = r50;
        r4 = r0.whead;
        r0 = r25;
        if (r4 != r0) goto L_0x00f2;
    L_0x02e4:
        r0 = r23;
        r0 = r0.prev;
        r41 = r0;
        r0 = r41;
        r1 = r22;
        if (r0 == r1) goto L_0x0330;
    L_0x02f0:
        if (r41 == 0) goto L_0x00f2;
    L_0x02f2:
        r22 = r41;
        r0 = r23;
        r1 = r41;
        r1.next = r0;
        goto L_0x00f2;
    L_0x02fc:
        if (r48 == 0) goto L_0x0305;
    L_0x02fe:
        r4 = java.lang.Thread.currentThread();
        r4.interrupt();
    L_0x0305:
        return r10;
    L_0x0306:
        if (r25 == 0) goto L_0x02dc;
    L_0x0308:
        r0 = r25;
        r0 = r0.cowait;
        r28 = r0;
        if (r28 == 0) goto L_0x02dc;
    L_0x0310:
        r24 = U;
        r26 = WCOWAIT;
        r0 = r28;
        r0 = r0.cowait;
        r29 = r0;
        r4 = r24.compareAndSwapObject(r25, r26, r28, r29);
        if (r4 == 0) goto L_0x0308;
    L_0x0320:
        r0 = r28;
        r0 = r0.thread;
        r45 = r0;
        if (r45 == 0) goto L_0x0308;
    L_0x0328:
        r4 = U;
        r0 = r45;
        r4.unpark(r0);
        goto L_0x0308;
    L_0x0330:
        r0 = r22;
        r0 = r0.status;
        r43 = r0;
        if (r43 != 0) goto L_0x0347;
    L_0x0338:
        r30 = U;
        r32 = WSTATUS;
        r34 = 0;
        r35 = -1;
        r31 = r22;
        r30.compareAndSwapInt(r31, r32, r34, r35);
        goto L_0x00f2;
    L_0x0347:
        r4 = 1;
        r0 = r43;
        if (r0 != r4) goto L_0x0362;
    L_0x034c:
        r0 = r22;
        r0 = r0.prev;
        r42 = r0;
        if (r42 == 0) goto L_0x00f2;
    L_0x0354:
        r0 = r42;
        r1 = r23;
        r1.prev = r0;
        r0 = r23;
        r1 = r42;
        r1.next = r0;
        goto L_0x00f2;
    L_0x0362:
        r4 = 0;
        r4 = (r52 > r4 ? 1 : (r52 == r4 ? 0 : -1));
        if (r4 != 0) goto L_0x03d3;
    L_0x0368:
        r46 = 0;
    L_0x036a:
        r49 = java.lang.Thread.currentThread();
        r4 = U;
        r6 = PARKBLOCKER;
        r0 = r49;
        r1 = r50;
        r4.putObject(r0, r6, r1);
        r0 = r49;
        r1 = r23;
        r1.thread = r0;
        r0 = r22;
        r4 = r0.status;
        if (r4 >= 0) goto L_0x03b0;
    L_0x0385:
        r0 = r22;
        r1 = r25;
        if (r0 != r1) goto L_0x0398;
    L_0x038b:
        r0 = r50;
        r4 = r0.state;
        r6 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r4 = r4 & r6;
        r6 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r4 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1));
        if (r4 != 0) goto L_0x03b0;
    L_0x0398:
        r0 = r50;
        r4 = r0.whead;
        r0 = r25;
        if (r4 != r0) goto L_0x03b0;
    L_0x03a0:
        r0 = r23;
        r4 = r0.prev;
        r0 = r22;
        if (r4 != r0) goto L_0x03b0;
    L_0x03a8:
        r4 = U;
        r5 = 0;
        r0 = r46;
        r4.park(r5, r0);
    L_0x03b0:
        r4 = 0;
        r0 = r23;
        r0.thread = r4;
        r4 = U;
        r6 = PARKBLOCKER;
        r5 = 0;
        r0 = r49;
        r4.putObject(r0, r6, r5);
        r4 = java.lang.Thread.interrupted();
        if (r4 == 0) goto L_0x00f2;
    L_0x03c5:
        if (r51 == 0) goto L_0x03eb;
    L_0x03c7:
        r4 = 1;
        r0 = r50;
        r1 = r23;
        r2 = r23;
        r4 = r0.cancelWaiter(r1, r2, r4);
        return r4;
    L_0x03d3:
        r4 = java.lang.System.nanoTime();
        r46 = r52 - r4;
        r4 = 0;
        r4 = (r46 > r4 ? 1 : (r46 == r4 ? 0 : -1));
        if (r4 > 0) goto L_0x036a;
    L_0x03df:
        r4 = 0;
        r0 = r50;
        r1 = r23;
        r2 = r23;
        r4 = r0.cancelWaiter(r1, r2, r4);
        return r4;
    L_0x03eb:
        r48 = 1;
        goto L_0x00f2;
    L_0x03ef:
        r25 = r36;
        goto L_0x017e;
    L_0x03f3:
        r25 = r36;
        goto L_0x0006;
    L_0x03f7:
        r36 = r25;
        goto L_0x007f;
        */
        throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.locks.StampedLock.acquireRead(boolean, long):long");
    }

    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        WNode q;
        WNode t;
        WNode h;
        if (node != null && group != null) {
            node.status = 1;
            WNode p = group;
            while (true) {
                q = p.cowait;
                if (q == null) {
                    break;
                } else if (q.status == 1) {
                    U.compareAndSwapObject(p, WCOWAIT, q, q.cowait);
                    p = group;
                } else {
                    p = q;
                }
            }
            if (group == node) {
                Thread w;
                for (WNode r = group.cowait; r != null; r = r.cowait) {
                    w = r.thread;
                    if (w != null) {
                        U.unpark(w);
                    }
                }
                WNode pred = node.prev;
                while (pred != null) {
                    WNode succ;
                    WNode succ2;
                    do {
                        succ = node.next;
                        if (succ != null && succ.status != 1) {
                            break;
                        }
                        q = null;
                        t = this.wtail;
                        while (t != null && t != node) {
                            if (t.status != 1) {
                                q = t;
                            }
                            t = t.prev;
                        }
                        if (succ == q) {
                            break;
                        }
                        succ2 = q;
                    } while (!U.compareAndSwapObject(node, WNEXT, succ, q));
                    succ = succ2;
                    if (succ == null && node == this.wtail) {
                        U.compareAndSwapObject(this, WTAIL, node, pred);
                    }
                    if (pred.next == node) {
                        U.compareAndSwapObject(pred, WNEXT, node, succ);
                    }
                    if (succ != null) {
                        w = succ.thread;
                        if (w != null) {
                            succ.thread = null;
                            U.unpark(w);
                        }
                    }
                    if (pred.status != 1) {
                        break;
                    }
                    WNode pp = pred.prev;
                    if (pp == null) {
                        break;
                    }
                    node.prev = pp;
                    U.compareAndSwapObject(pp, WNEXT, pred, succ);
                    pred = pp;
                }
            }
        }
        do {
            h = this.whead;
            if (h == null) {
                break;
            }
            q = h.next;
            if (q == null || q.status == 1) {
                t = this.wtail;
                while (t != null && t != h) {
                    if (t.status <= 0) {
                        q = t;
                    }
                    t = t.prev;
                }
            }
        } while (h != this.whead);
        if (q != null && h.status == 0) {
            long s = this.state;
            if ((ABITS & s) != WBIT && (s == 0 || q.mode == 0)) {
                release(h);
            }
        }
        return (interrupted || Thread.interrupted()) ? 1 : 0;
    }
}
