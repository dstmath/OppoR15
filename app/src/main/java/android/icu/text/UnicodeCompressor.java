package android.icu.text;

import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Normalizer2Impl.Hangul;

public final class UnicodeCompressor implements SCSU {
    private static boolean[] sSingleTagTable = new boolean[]{false, true, true, true, true, true, true, true, true, false, false, true, true, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    private static boolean[] sUnicodeTagTable = new boolean[]{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false};
    private int fCurrentWindow = 0;
    private int[] fIndexCount = new int[256];
    private int fMode = 0;
    private int[] fOffsets = new int[8];
    private int fTimeStamp = 0;
    private int[] fTimeStamps = new int[8];

    public UnicodeCompressor() {
        reset();
    }

    public static byte[] compress(String buffer) {
        return compress(buffer.toCharArray(), 0, buffer.length());
    }

    public static byte[] compress(char[] buffer, int start, int limit) {
        UnicodeCompressor comp = new UnicodeCompressor();
        int len = Math.max(4, ((limit - start) * 3) + 1);
        byte[] temp = new byte[len];
        int byteCount = comp.compress(buffer, start, limit, null, temp, 0, len);
        byte[] result = new byte[byteCount];
        System.arraycopy(temp, 0, result, 0, byteCount);
        return result;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int compress(char[] charBuffer, int charBufferStart, int charBufferLimit, int[] charsRead, byte[] byteBuffer, int byteBufferStart, int byteBufferLimit) {
        int bytePos = byteBufferStart;
        int ucPos = charBufferStart;
        if (byteBuffer.length < 4 || byteBufferLimit - byteBufferStart < 4) {
            throw new IllegalArgumentException("byteBuffer.length < 4");
        }
        while (ucPos < charBufferLimit && bytePos < byteBufferLimit) {
            switch (this.fMode) {
                case 0:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        }
        if (charsRead != null) {
            charsRead[0] = ucPos - charBufferStart;
        }
        return bytePos - byteBufferStart;
    }

    public void reset() {
        int i;
        this.fOffsets[0] = 128;
        this.fOffsets[1] = 192;
        this.fOffsets[2] = 1024;
        this.fOffsets[3] = 1536;
        this.fOffsets[4] = 2304;
        this.fOffsets[5] = 12352;
        this.fOffsets[6] = 12448;
        this.fOffsets[7] = Normalizer2Impl.JAMO_VT;
        for (i = 0; i < 8; i++) {
            this.fTimeStamps[i] = 0;
        }
        for (i = 0; i <= 255; i++) {
            this.fIndexCount[i] = 0;
        }
        this.fTimeStamp = 0;
        this.fCurrentWindow = 0;
        this.fMode = 0;
    }

    private static int makeIndex(int c) {
        if (c >= 192 && c < 320) {
            return 249;
        }
        if (c >= 592 && c < 720) {
            return 250;
        }
        if (c >= 880 && c < 1008) {
            return 251;
        }
        if (c >= 1328 && c < 1424) {
            return 252;
        }
        if (c >= 12352 && c < 12448) {
            return 253;
        }
        if (c >= 12448 && c < 12576) {
            return 254;
        }
        if (c >= 65376 && c < 65439) {
            return 255;
        }
        if (c >= 128 && c < Normalizer2Impl.COMP_1_TRAIL_LIMIT) {
            return (c / 128) & 255;
        }
        if (c < 57344 || c > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            return 0;
        }
        return ((c - Hangul.HANGUL_BASE) / 128) & 255;
    }

    private boolean inDynamicWindow(int c, int whichWindow) {
        if (c < this.fOffsets[whichWindow] || c >= this.fOffsets[whichWindow] + 128) {
            return false;
        }
        return true;
    }

    private static boolean inStaticWindow(int c, int whichWindow) {
        if (c < sOffsets[whichWindow] || c >= sOffsets[whichWindow] + 128) {
            return false;
        }
        return true;
    }

    private static boolean isCompressible(int c) {
        return c < Normalizer2Impl.COMP_1_TRAIL_LIMIT || c >= 57344;
    }

    private int findDynamicWindow(int c) {
        for (int i = 7; i >= 0; i--) {
            if (inDynamicWindow(c, i)) {
                int[] iArr = this.fTimeStamps;
                iArr[i] = iArr[i] + 1;
                return i;
            }
        }
        return -1;
    }

    private static int findStaticWindow(int c) {
        for (int i = 7; i >= 0; i--) {
            if (inStaticWindow(c, i)) {
                return i;
            }
        }
        return -1;
    }

    private int getLRDefinedWindow() {
        int leastRU = Integer.MAX_VALUE;
        int whichWindow = -1;
        for (int i = 7; i >= 0; i--) {
            if (this.fTimeStamps[i] < leastRU) {
                leastRU = this.fTimeStamps[i];
                whichWindow = i;
            }
        }
        return whichWindow;
    }
}
