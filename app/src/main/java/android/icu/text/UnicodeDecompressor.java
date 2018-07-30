package android.icu.text;

import android.icu.impl.Normalizer2Impl;

public final class UnicodeDecompressor implements SCSU {
    private static final int BUFSIZE = 3;
    private byte[] fBuffer = new byte[3];
    private int fBufferLength = 0;
    private int fCurrentWindow = 0;
    private int fMode = 0;
    private int[] fOffsets = new int[8];

    public UnicodeDecompressor() {
        reset();
    }

    public static String decompress(byte[] buffer) {
        return new String(decompress(buffer, 0, buffer.length));
    }

    public static char[] decompress(byte[] buffer, int start, int limit) {
        UnicodeDecompressor comp = new UnicodeDecompressor();
        int len = Math.max(2, (limit - start) * 2);
        char[] temp = new char[len];
        int charCount = comp.decompress(buffer, start, limit, null, temp, 0, len);
        char[] result = new char[charCount];
        System.arraycopy(temp, 0, result, 0, charCount);
        return result;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int decompress(byte[] byteBuffer, int byteBufferStart, int byteBufferLimit, int[] bytesRead, char[] charBuffer, int charBufferStart, int charBufferLimit) {
        int bytePos = byteBufferStart;
        int ucPos = charBufferStart;
        if (charBuffer.length < 2 || charBufferLimit - charBufferStart < 2) {
            throw new IllegalArgumentException("charBuffer.length < 2");
        }
        if (this.fBufferLength > 0) {
            int newBytes = 0;
            if (this.fBufferLength != 3) {
                newBytes = this.fBuffer.length - this.fBufferLength;
                if (byteBufferLimit - byteBufferStart < newBytes) {
                    newBytes = byteBufferLimit - byteBufferStart;
                }
                System.arraycopy(byteBuffer, byteBufferStart, this.fBuffer, this.fBufferLength, newBytes);
            }
            this.fBufferLength = 0;
            ucPos = charBufferStart + decompress(this.fBuffer, 0, this.fBuffer.length, null, charBuffer, charBufferStart, charBufferLimit);
            bytePos = byteBufferStart + newBytes;
        }
        while (bytePos < byteBufferLimit && ucPos < charBufferLimit) {
            switch (this.fMode) {
                case 0:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        }
        if (bytesRead != null) {
            bytesRead[0] = bytePos - byteBufferStart;
        }
        return ucPos - charBufferStart;
    }

    public void reset() {
        this.fOffsets[0] = 128;
        this.fOffsets[1] = 192;
        this.fOffsets[2] = 1024;
        this.fOffsets[3] = 1536;
        this.fOffsets[4] = 2304;
        this.fOffsets[5] = 12352;
        this.fOffsets[6] = 12448;
        this.fOffsets[7] = Normalizer2Impl.JAMO_VT;
        this.fCurrentWindow = 0;
        this.fMode = 0;
        this.fBufferLength = 0;
    }
}
