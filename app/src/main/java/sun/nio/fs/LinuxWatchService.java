package sun.nio.fs;

import com.sun.nio.file.SensitivityWatchEventModifier;
import dalvik.system.CloseGuard;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import sun.misc.Unsafe;

class LinuxWatchService extends AbstractWatchService {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private final Poller poller;

    private static class LinuxWatchKey extends AbstractWatchKey {
        private final int ifd;
        private volatile int wd;

        LinuxWatchKey(UnixPath dir, LinuxWatchService watcher, int ifd, int wd) {
            super(dir, watcher);
            this.ifd = ifd;
            this.wd = wd;
        }

        int descriptor() {
            return this.wd;
        }

        void invalidate(boolean remove) {
            if (remove) {
                try {
                    LinuxWatchService.inotifyRmWatch(this.ifd, this.wd);
                } catch (UnixException e) {
                }
            }
            this.wd = -1;
        }

        public boolean isValid() {
            return this.wd != -1;
        }

        public void cancel() {
            if (isValid()) {
                ((LinuxWatchService) watcher()).poller.cancel(this);
            }
        }
    }

    private static class Poller extends AbstractPoller {
        private static final int BUFFER_SIZE = 8192;
        private static final int IN_ATTRIB = 4;
        private static final int IN_CREATE = 256;
        private static final int IN_DELETE = 512;
        private static final int IN_IGNORED = 32768;
        private static final int IN_MODIFY = 2;
        private static final int IN_MOVED_FROM = 64;
        private static final int IN_MOVED_TO = 128;
        private static final int IN_Q_OVERFLOW = 16384;
        private static final int IN_UNMOUNT = 8192;
        private static final int OFFSETOF_LEN = offsets[3];
        private static final int OFFSETOF_MASK = offsets[1];
        private static final int OFFSETOF_NAME = offsets[4];
        private static final int OFFSETOF_WD = offsets[0];
        private static final int SIZEOF_INOTIFY_EVENT = LinuxWatchService.eventSize();
        private static final int[] offsets = LinuxWatchService.eventOffsets();
        private final long address;
        private final UnixFileSystem fs;
        private final CloseGuard guard = CloseGuard.get();
        private final int ifd;
        private final int[] socketpair;
        private final LinuxWatchService watcher;
        private final Map<Integer, LinuxWatchKey> wdToKey;

        Poller(UnixFileSystem fs, LinuxWatchService watcher, int ifd, int[] sp) {
            this.fs = fs;
            this.watcher = watcher;
            this.ifd = ifd;
            this.socketpair = sp;
            this.wdToKey = new HashMap();
            this.address = LinuxWatchService.unsafe.allocateMemory(8192);
            this.guard.open("close");
        }

        void wakeup() throws IOException {
            try {
                UnixNativeDispatcher.write(this.socketpair[1], this.address, 1);
            } catch (UnixException x) {
                throw new IOException(x.errorString());
            }
        }

        Object implRegister(Path obj, Set<? extends Kind<?>> events, Modifier... modifiers) {
            UnixPath dir = (UnixPath) obj;
            int mask = 0;
            for (Kind<?> event : events) {
                if (event == StandardWatchEventKinds.ENTRY_CREATE) {
                    mask |= 384;
                } else if (event == StandardWatchEventKinds.ENTRY_DELETE) {
                    mask |= 576;
                } else if (event == StandardWatchEventKinds.ENTRY_MODIFY) {
                    mask |= 6;
                }
            }
            if (modifiers.length > 0) {
                for (Modifier modifier : modifiers) {
                    if (modifier == null) {
                        return new NullPointerException();
                    }
                    if (!(modifier instanceof SensitivityWatchEventModifier)) {
                        return new UnsupportedOperationException("Modifier not supported");
                    }
                }
            }
            try {
                if (!UnixFileAttributes.get(dir, true).isDirectory()) {
                    return new NotDirectoryException(dir.getPathForExceptionMessage());
                }
                NativeBuffer buffer;
                try {
                    buffer = NativeBuffers.asNativeBuffer(dir.getByteArrayForSysCalls());
                    int wd = LinuxWatchService.inotifyAddWatch(this.ifd, buffer.address(), mask);
                    buffer.release();
                    LinuxWatchKey key = (LinuxWatchKey) this.wdToKey.get(Integer.valueOf(wd));
                    if (key == null) {
                        key = new LinuxWatchKey(dir, this.watcher, this.ifd, wd);
                        this.wdToKey.put(Integer.valueOf(wd), key);
                    }
                    return key;
                } catch (UnixException x) {
                    if (x.errno() == UnixConstants.ENOSPC) {
                        return new IOException("User limit of inotify watches reached");
                    }
                    return x.asIOException(dir);
                } catch (Throwable th) {
                    buffer.release();
                }
            } catch (UnixException x2) {
                return x2.asIOException(dir);
            }
        }

        void implCancelKey(WatchKey obj) {
            LinuxWatchKey key = (LinuxWatchKey) obj;
            if (key.isValid()) {
                this.wdToKey.remove(Integer.valueOf(key.descriptor()));
                key.invalidate(true);
            }
        }

        void implCloseAll() {
            this.guard.close();
            for (Entry<Integer, LinuxWatchKey> entry : this.wdToKey.entrySet()) {
                ((LinuxWatchKey) entry.getValue()).invalidate(true);
            }
            this.wdToKey.clear();
            LinuxWatchService.unsafe.freeMemory(this.address);
            UnixNativeDispatcher.close(this.socketpair[0]);
            UnixNativeDispatcher.close(this.socketpair[1]);
            UnixNativeDispatcher.close(this.ifd);
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

        public void run() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Regions count limit reached
	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:83)
	at jadx.core.dex.visitors.regions.RegionMaker.makeEndlessLoop(RegionMaker.java:330)
	at jadx.core.dex.visitors.regions.RegionMaker.processLoop(RegionMaker.java:167)
	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:101)
	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:80)
	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:49)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
            /*
            r26 = this;
        L_0x0000:
            r0 = r26;	 Catch:{ UnixException -> 0x006a }
            r0 = r0.ifd;	 Catch:{ UnixException -> 0x006a }
            r20 = r0;	 Catch:{ UnixException -> 0x006a }
            r0 = r26;	 Catch:{ UnixException -> 0x006a }
            r0 = r0.socketpair;	 Catch:{ UnixException -> 0x006a }
            r21 = r0;	 Catch:{ UnixException -> 0x006a }
            r22 = 0;	 Catch:{ UnixException -> 0x006a }
            r21 = r21[r22];	 Catch:{ UnixException -> 0x006a }
            r14 = sun.nio.fs.LinuxWatchService.poll(r20, r21);	 Catch:{ UnixException -> 0x006a }
            r0 = r26;	 Catch:{ UnixException -> 0x005c }
            r0 = r0.ifd;	 Catch:{ UnixException -> 0x005c }
            r20 = r0;	 Catch:{ UnixException -> 0x005c }
            r0 = r26;	 Catch:{ UnixException -> 0x005c }
            r0 = r0.address;	 Catch:{ UnixException -> 0x005c }
            r22 = r0;	 Catch:{ UnixException -> 0x005c }
            r21 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;	 Catch:{ UnixException -> 0x005c }
            r0 = r20;	 Catch:{ UnixException -> 0x005c }
            r1 = r22;	 Catch:{ UnixException -> 0x005c }
            r3 = r21;	 Catch:{ UnixException -> 0x005c }
            r6 = sun.nio.fs.UnixNativeDispatcher.read(r0, r1, r3);	 Catch:{ UnixException -> 0x005c }
        L_0x002c:
            r20 = 1;
            r0 = r20;
            if (r14 > r0) goto L_0x003a;
        L_0x0032:
            r20 = 1;
            r0 = r20;
            if (r14 != r0) goto L_0x007f;
        L_0x0038:
            if (r6 != 0) goto L_0x007f;
        L_0x003a:
            r0 = r26;	 Catch:{ UnixException -> 0x0071 }
            r0 = r0.socketpair;	 Catch:{ UnixException -> 0x0071 }
            r20 = r0;	 Catch:{ UnixException -> 0x0071 }
            r21 = 0;	 Catch:{ UnixException -> 0x0071 }
            r20 = r20[r21];	 Catch:{ UnixException -> 0x0071 }
            r0 = r26;	 Catch:{ UnixException -> 0x0071 }
            r0 = r0.address;	 Catch:{ UnixException -> 0x0071 }
            r22 = r0;	 Catch:{ UnixException -> 0x0071 }
            r21 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;	 Catch:{ UnixException -> 0x0071 }
            r0 = r20;	 Catch:{ UnixException -> 0x0071 }
            r1 = r22;	 Catch:{ UnixException -> 0x0071 }
            r3 = r21;	 Catch:{ UnixException -> 0x0071 }
            sun.nio.fs.UnixNativeDispatcher.read(r0, r1, r3);	 Catch:{ UnixException -> 0x0071 }
            r17 = r26.processRequests();	 Catch:{ UnixException -> 0x0071 }
            if (r17 == 0) goto L_0x007f;
        L_0x005b:
            return;
        L_0x005c:
            r19 = move-exception;
            r20 = r19.errno();	 Catch:{ UnixException -> 0x006a }
            r21 = sun.nio.fs.UnixConstants.EAGAIN;	 Catch:{ UnixException -> 0x006a }
            r0 = r20;	 Catch:{ UnixException -> 0x006a }
            r1 = r21;	 Catch:{ UnixException -> 0x006a }
            if (r0 == r1) goto L_0x006f;	 Catch:{ UnixException -> 0x006a }
        L_0x0069:
            throw r19;	 Catch:{ UnixException -> 0x006a }
        L_0x006a:
            r19 = move-exception;
            r19.printStackTrace();
            goto L_0x005b;
        L_0x006f:
            r6 = 0;
            goto L_0x002c;
        L_0x0071:
            r19 = move-exception;
            r20 = r19.errno();	 Catch:{ UnixException -> 0x006a }
            r21 = sun.nio.fs.UnixConstants.EAGAIN;	 Catch:{ UnixException -> 0x006a }
            r0 = r20;	 Catch:{ UnixException -> 0x006a }
            r1 = r21;	 Catch:{ UnixException -> 0x006a }
            if (r0 == r1) goto L_0x007f;	 Catch:{ UnixException -> 0x006a }
        L_0x007e:
            throw r19;	 Catch:{ UnixException -> 0x006a }
        L_0x007f:
            r16 = 0;	 Catch:{ UnixException -> 0x006a }
        L_0x0081:
            r0 = r16;	 Catch:{ UnixException -> 0x006a }
            if (r0 >= r6) goto L_0x0000;	 Catch:{ UnixException -> 0x006a }
        L_0x0085:
            r0 = r26;	 Catch:{ UnixException -> 0x006a }
            r0 = r0.address;	 Catch:{ UnixException -> 0x006a }
            r20 = r0;	 Catch:{ UnixException -> 0x006a }
            r0 = r16;	 Catch:{ UnixException -> 0x006a }
            r0 = (long) r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r0;	 Catch:{ UnixException -> 0x006a }
            r8 = r20 + r22;	 Catch:{ UnixException -> 0x006a }
            r20 = sun.nio.fs.LinuxWatchService.unsafe;	 Catch:{ UnixException -> 0x006a }
            r21 = OFFSETOF_WD;	 Catch:{ UnixException -> 0x006a }
            r0 = r21;	 Catch:{ UnixException -> 0x006a }
            r0 = (long) r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r22 + r8;	 Catch:{ UnixException -> 0x006a }
            r0 = r20;	 Catch:{ UnixException -> 0x006a }
            r1 = r22;	 Catch:{ UnixException -> 0x006a }
            r18 = r0.getInt(r1);	 Catch:{ UnixException -> 0x006a }
            r20 = sun.nio.fs.LinuxWatchService.unsafe;	 Catch:{ UnixException -> 0x006a }
            r21 = OFFSETOF_MASK;	 Catch:{ UnixException -> 0x006a }
            r0 = r21;	 Catch:{ UnixException -> 0x006a }
            r0 = (long) r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r22 + r8;	 Catch:{ UnixException -> 0x006a }
            r0 = r20;	 Catch:{ UnixException -> 0x006a }
            r1 = r22;	 Catch:{ UnixException -> 0x006a }
            r13 = r0.getInt(r1);	 Catch:{ UnixException -> 0x006a }
            r20 = sun.nio.fs.LinuxWatchService.unsafe;	 Catch:{ UnixException -> 0x006a }
            r21 = OFFSETOF_LEN;	 Catch:{ UnixException -> 0x006a }
            r0 = r21;	 Catch:{ UnixException -> 0x006a }
            r0 = (long) r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r22 + r8;	 Catch:{ UnixException -> 0x006a }
            r0 = r20;	 Catch:{ UnixException -> 0x006a }
            r1 = r22;	 Catch:{ UnixException -> 0x006a }
            r12 = r0.getInt(r1);	 Catch:{ UnixException -> 0x006a }
            r15 = 0;	 Catch:{ UnixException -> 0x006a }
            if (r12 <= 0) goto L_0x012b;	 Catch:{ UnixException -> 0x006a }
        L_0x00d4:
            r4 = r12;	 Catch:{ UnixException -> 0x006a }
        L_0x00d5:
            if (r4 <= 0) goto L_0x00f5;	 Catch:{ UnixException -> 0x006a }
        L_0x00d7:
            r20 = OFFSETOF_NAME;	 Catch:{ UnixException -> 0x006a }
            r0 = r20;	 Catch:{ UnixException -> 0x006a }
            r0 = (long) r0;	 Catch:{ UnixException -> 0x006a }
            r20 = r0;	 Catch:{ UnixException -> 0x006a }
            r20 = r20 + r8;	 Catch:{ UnixException -> 0x006a }
            r0 = (long) r4;	 Catch:{ UnixException -> 0x006a }
            r22 = r0;	 Catch:{ UnixException -> 0x006a }
            r20 = r20 + r22;	 Catch:{ UnixException -> 0x006a }
            r22 = 1;	 Catch:{ UnixException -> 0x006a }
            r10 = r20 - r22;	 Catch:{ UnixException -> 0x006a }
            r20 = sun.nio.fs.LinuxWatchService.unsafe;	 Catch:{ UnixException -> 0x006a }
            r0 = r20;	 Catch:{ UnixException -> 0x006a }
            r20 = r0.getByte(r10);	 Catch:{ UnixException -> 0x006a }
            if (r20 == 0) goto L_0x011b;	 Catch:{ UnixException -> 0x006a }
        L_0x00f5:
            if (r4 <= 0) goto L_0x012b;	 Catch:{ UnixException -> 0x006a }
        L_0x00f7:
            r5 = new byte[r4];	 Catch:{ UnixException -> 0x006a }
            r7 = 0;	 Catch:{ UnixException -> 0x006a }
        L_0x00fa:
            if (r7 >= r4) goto L_0x011e;	 Catch:{ UnixException -> 0x006a }
        L_0x00fc:
            r20 = sun.nio.fs.LinuxWatchService.unsafe;	 Catch:{ UnixException -> 0x006a }
            r21 = OFFSETOF_NAME;	 Catch:{ UnixException -> 0x006a }
            r0 = r21;	 Catch:{ UnixException -> 0x006a }
            r0 = (long) r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r22 + r8;	 Catch:{ UnixException -> 0x006a }
            r0 = (long) r7;	 Catch:{ UnixException -> 0x006a }
            r24 = r0;	 Catch:{ UnixException -> 0x006a }
            r22 = r22 + r24;	 Catch:{ UnixException -> 0x006a }
            r0 = r20;	 Catch:{ UnixException -> 0x006a }
            r1 = r22;	 Catch:{ UnixException -> 0x006a }
            r20 = r0.getByte(r1);	 Catch:{ UnixException -> 0x006a }
            r5[r7] = r20;	 Catch:{ UnixException -> 0x006a }
            r7 = r7 + 1;	 Catch:{ UnixException -> 0x006a }
            goto L_0x00fa;	 Catch:{ UnixException -> 0x006a }
        L_0x011b:
            r4 = r4 + -1;	 Catch:{ UnixException -> 0x006a }
            goto L_0x00d5;	 Catch:{ UnixException -> 0x006a }
        L_0x011e:
            r15 = new sun.nio.fs.UnixPath;	 Catch:{ UnixException -> 0x006a }
            r0 = r26;	 Catch:{ UnixException -> 0x006a }
            r0 = r0.fs;	 Catch:{ UnixException -> 0x006a }
            r20 = r0;	 Catch:{ UnixException -> 0x006a }
            r0 = r20;	 Catch:{ UnixException -> 0x006a }
            r15.<init>(r0, r5);	 Catch:{ UnixException -> 0x006a }
        L_0x012b:
            r0 = r26;	 Catch:{ UnixException -> 0x006a }
            r1 = r18;	 Catch:{ UnixException -> 0x006a }
            r0.processEvent(r1, r13, r15);	 Catch:{ UnixException -> 0x006a }
            r20 = SIZEOF_INOTIFY_EVENT;	 Catch:{ UnixException -> 0x006a }
            r20 = r20 + r12;
            r16 = r16 + r20;
            goto L_0x0081;
            */
            throw new UnsupportedOperationException("Method not decompiled: sun.nio.fs.LinuxWatchService.Poller.run():void");
        }

        private Kind<?> maskToEventKind(int mask) {
            if ((mask & 2) > 0) {
                return StandardWatchEventKinds.ENTRY_MODIFY;
            }
            if ((mask & 4) > 0) {
                return StandardWatchEventKinds.ENTRY_MODIFY;
            }
            if ((mask & 256) > 0) {
                return StandardWatchEventKinds.ENTRY_CREATE;
            }
            if ((mask & 128) > 0) {
                return StandardWatchEventKinds.ENTRY_CREATE;
            }
            if ((mask & 512) > 0) {
                return StandardWatchEventKinds.ENTRY_DELETE;
            }
            if ((mask & 64) > 0) {
                return StandardWatchEventKinds.ENTRY_DELETE;
            }
            return null;
        }

        private void processEvent(int wd, int mask, UnixPath name) {
            if ((mask & 16384) > 0) {
                for (Entry<Integer, LinuxWatchKey> entry : this.wdToKey.entrySet()) {
                    ((LinuxWatchKey) entry.getValue()).signalEvent(StandardWatchEventKinds.OVERFLOW, null);
                }
                return;
            }
            LinuxWatchKey key = (LinuxWatchKey) this.wdToKey.get(Integer.valueOf(wd));
            if (key != null) {
                if ((32768 & mask) > 0) {
                    this.wdToKey.remove(Integer.valueOf(wd));
                    key.invalidate(false);
                    key.signal();
                } else if (name != null) {
                    Kind<?> kind = maskToEventKind(mask);
                    if (kind != null) {
                        key.signalEvent(kind, name);
                    }
                }
            }
        }
    }

    private static native void configureBlocking(int i, boolean z) throws UnixException;

    private static native int[] eventOffsets();

    private static native int eventSize();

    private static native int inotifyAddWatch(int i, long j, int i2) throws UnixException;

    private static native int inotifyInit() throws UnixException;

    private static native void inotifyRmWatch(int i, int i2) throws UnixException;

    private static native int poll(int i, int i2) throws UnixException;

    private static native void socketpair(int[] iArr) throws UnixException;

    LinuxWatchService(UnixFileSystem fs) throws IOException {
        try {
            int ifd = inotifyInit();
            int[] sp = new int[2];
            try {
                configureBlocking(ifd, false);
                socketpair(sp);
                configureBlocking(sp[0], false);
                this.poller = new Poller(fs, this, ifd, sp);
                this.poller.start();
            } catch (UnixException x) {
                UnixNativeDispatcher.close(ifd);
                throw new IOException(x.errorString());
            }
        } catch (UnixException x2) {
            String msg;
            if (x2.errno() == UnixConstants.EMFILE) {
                msg = "User limit of inotify instances reached or too many open files";
            } else {
                msg = x2.errorString();
            }
            throw new IOException(msg);
        }
    }

    WatchKey register(Path dir, Kind<?>[] events, Modifier... modifiers) throws IOException {
        return this.poller.register(dir, events, modifiers);
    }

    void implClose() throws IOException {
        this.poller.close();
    }
}
