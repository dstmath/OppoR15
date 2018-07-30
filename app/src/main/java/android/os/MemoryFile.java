package android.os;

import android.system.ErrnoException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class MemoryFile {
    private static String TAG = "MemoryFile";
    private boolean mAllowPurging = false;
    private ByteBuffer mMapping;
    private SharedMemory mSharedMemory;

    private class MemoryInputStream extends InputStream {
        private int mMark;
        private int mOffset;
        private byte[] mSingleByte;

        /* synthetic */ MemoryInputStream(MemoryFile this$0, MemoryInputStream -this1) {
            this();
        }

        private MemoryInputStream() {
            this.mMark = 0;
            this.mOffset = 0;
        }

        public int available() throws IOException {
            if (this.mOffset >= MemoryFile.this.mSharedMemory.getSize()) {
                return 0;
            }
            return MemoryFile.this.mSharedMemory.getSize() - this.mOffset;
        }

        public boolean markSupported() {
            return true;
        }

        public void mark(int readlimit) {
            this.mMark = this.mOffset;
        }

        public void reset() throws IOException {
            this.mOffset = this.mMark;
        }

        public int read() throws IOException {
            if (this.mSingleByte == null) {
                this.mSingleByte = new byte[1];
            }
            if (read(this.mSingleByte, 0, 1) != 1) {
                return -1;
            }
            return this.mSingleByte[0];
        }

        public int read(byte[] buffer, int offset, int count) throws IOException {
            if (offset < 0 || count < 0 || offset + count > buffer.length) {
                throw new IndexOutOfBoundsException();
            }
            count = Math.min(count, available());
            if (count < 1) {
                return -1;
            }
            int result = MemoryFile.this.readBytes(buffer, this.mOffset, offset, count);
            if (result > 0) {
                this.mOffset += result;
            }
            return result;
        }

        public long skip(long n) throws IOException {
            if (((long) this.mOffset) + n > ((long) MemoryFile.this.mSharedMemory.getSize())) {
                n = (long) (MemoryFile.this.mSharedMemory.getSize() - this.mOffset);
            }
            this.mOffset = (int) (((long) this.mOffset) + n);
            return n;
        }
    }

    private class MemoryOutputStream extends OutputStream {
        private int mOffset;
        private byte[] mSingleByte;

        /* synthetic */ MemoryOutputStream(MemoryFile this$0, MemoryOutputStream -this1) {
            this();
        }

        private MemoryOutputStream() {
            this.mOffset = 0;
        }

        public void write(byte[] buffer, int offset, int count) throws IOException {
            MemoryFile.this.writeBytes(buffer, offset, this.mOffset, count);
            this.mOffset += count;
        }

        public void write(int oneByte) throws IOException {
            if (this.mSingleByte == null) {
                this.mSingleByte = new byte[1];
            }
            this.mSingleByte[0] = (byte) oneByte;
            write(this.mSingleByte, 0, 1);
        }
    }

    private static native int native_get_size(FileDescriptor fileDescriptor) throws IOException;

    private static native boolean native_pin(FileDescriptor fileDescriptor, boolean z) throws IOException;

    public MemoryFile(String name, int length) throws IOException {
        try {
            this.mSharedMemory = SharedMemory.create(name, length);
            this.mMapping = this.mSharedMemory.mapReadWrite();
        } catch (ErrnoException ex) {
            ex.rethrowAsIOException();
        }
    }

    public void close() {
        deactivate();
        this.mSharedMemory.close();
    }

    void deactivate() {
        if (this.mMapping != null) {
            SharedMemory.unmap(this.mMapping);
            this.mMapping = null;
        }
    }

    private void checkActive() throws IOException {
        if (this.mMapping == null) {
            throw new IOException("MemoryFile has been deactivated");
        }
    }

    private void beginAccess() throws IOException {
        checkActive();
        if (this.mAllowPurging && native_pin(this.mSharedMemory.getFileDescriptor(), true)) {
            throw new IOException("MemoryFile has been purged");
        }
    }

    private void endAccess() throws IOException {
        if (this.mAllowPurging) {
            native_pin(this.mSharedMemory.getFileDescriptor(), false);
        }
    }

    public int length() {
        return this.mSharedMemory.getSize();
    }

    @Deprecated
    public boolean isPurgingAllowed() {
        return this.mAllowPurging;
    }

    @Deprecated
    public synchronized boolean allowPurging(boolean allowPurging) throws IOException {
        boolean oldValue;
        oldValue = this.mAllowPurging;
        if (oldValue != allowPurging) {
            native_pin(this.mSharedMemory.getFileDescriptor(), allowPurging ^ 1);
            this.mAllowPurging = allowPurging;
        }
        return oldValue;
    }

    public InputStream getInputStream() {
        return new MemoryInputStream();
    }

    public OutputStream getOutputStream() {
        return new MemoryOutputStream();
    }

    public int readBytes(byte[] buffer, int srcOffset, int destOffset, int count) throws IOException {
        beginAccess();
        try {
            this.mMapping.position(srcOffset);
            this.mMapping.get(buffer, destOffset, count);
            return count;
        } finally {
            endAccess();
        }
    }

    public void writeBytes(byte[] buffer, int srcOffset, int destOffset, int count) throws IOException {
        beginAccess();
        try {
            this.mMapping.position(destOffset);
            this.mMapping.put(buffer, srcOffset, count);
        } finally {
            endAccess();
        }
    }

    public FileDescriptor getFileDescriptor() throws IOException {
        return this.mSharedMemory.getFileDescriptor();
    }

    public static int getSize(FileDescriptor fd) throws IOException {
        return native_get_size(fd);
    }
}
