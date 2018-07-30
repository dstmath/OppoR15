package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.text.StringPrepParseException;
import android.icu.text.UTF16;

public final class Punycode {
    private static final int BASE = 36;
    private static final int CAPITAL_A = 65;
    private static final int CAPITAL_Z = 90;
    private static final int DAMP = 700;
    private static final char DELIMITER = '-';
    private static final char HYPHEN = '-';
    private static final int INITIAL_BIAS = 72;
    private static final int INITIAL_N = 128;
    private static final int SKEW = 38;
    private static final int SMALL_A = 97;
    private static final int SMALL_Z = 122;
    private static final int TMAX = 26;
    private static final int TMIN = 1;
    private static final int ZERO = 48;
    static final int[] basicToDigit = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    private static int adaptBias(int delta, int length, boolean firstTime) {
        if (firstTime) {
            delta /= DAMP;
        } else {
            delta /= 2;
        }
        delta += delta / length;
        int count = 0;
        while (delta > 455) {
            delta /= 35;
            count += 36;
        }
        return ((delta * 36) / (delta + 38)) + count;
    }

    private static char asciiCaseMap(char b, boolean uppercase) {
        if (uppercase) {
            if ('a' > b || b > 'z') {
                return b;
            }
            return (char) (b - 32);
        } else if ('A' > b || b > 'Z') {
            return b;
        } else {
            return (char) (b + 32);
        }
    }

    private static char digitToBasic(int digit, boolean uppercase) {
        if (digit >= 26) {
            return (char) (digit + 22);
        }
        if (uppercase) {
            return (char) (digit + 65);
        }
        return (char) (digit + 97);
    }

    public static StringBuilder encode(CharSequence src, boolean[] caseFlags) throws StringPrepParseException {
        int srcLength = src.length();
        int[] cpBuffer = new int[srcLength];
        StringBuilder dest = new StringBuilder(srcLength);
        int srcCPCount = 0;
        int j = 0;
        while (true) {
            int srcCPCount2 = srcCPCount;
            int n;
            if (j < srcLength) {
                char c = src.charAt(j);
                if (isBasic(c)) {
                    srcCPCount = srcCPCount2 + 1;
                    cpBuffer[srcCPCount2] = 0;
                    if (caseFlags != null) {
                        c = asciiCaseMap(c, caseFlags[j]);
                    }
                    dest.append(c);
                } else {
                    int i = (caseFlags == null || !caseFlags[j]) ? 0 : 1;
                    n = i << 31;
                    if (UTF16.isSurrogate(c)) {
                        if (!UTF16.isLeadSurrogate(c) || j + 1 >= srcLength) {
                            break;
                        }
                        char c2 = src.charAt(j + 1);
                        if (!UTF16.isTrailSurrogate(c2)) {
                            break;
                        }
                        j++;
                        n |= UCharacter.getCodePoint(c, c2);
                    } else {
                        n |= c;
                    }
                    srcCPCount = srcCPCount2 + 1;
                    cpBuffer[srcCPCount2] = n;
                }
                j++;
            } else {
                int basicLength = dest.length();
                if (basicLength > 0) {
                    dest.append('-');
                }
                n = 128;
                int delta = 0;
                int bias = 72;
                int handledCPCount = basicLength;
                while (handledCPCount < srcCPCount2) {
                    int q;
                    int m = Integer.MAX_VALUE;
                    for (j = 0; j < srcCPCount2; j++) {
                        q = cpBuffer[j] & Integer.MAX_VALUE;
                        if (n <= q && q < m) {
                            m = q;
                        }
                    }
                    if (m - n > (Integer.MAX_VALUE - delta) / (handledCPCount + 1)) {
                        throw new IllegalStateException("Internal program error");
                    }
                    delta += (m - n) * (handledCPCount + 1);
                    n = m;
                    for (j = 0; j < srcCPCount2; j++) {
                        q = cpBuffer[j] & Integer.MAX_VALUE;
                        if (q < n) {
                            delta++;
                        } else if (q == n) {
                            q = delta;
                            int k = 36;
                            while (true) {
                                int t = k - bias;
                                if (t < 1) {
                                    t = 1;
                                } else if (k >= bias + 26) {
                                    t = 26;
                                }
                                if (q < t) {
                                    break;
                                }
                                dest.append(digitToBasic(((q - t) % (36 - t)) + t, false));
                                q = (q - t) / (36 - t);
                                k += 36;
                            }
                            dest.append(digitToBasic(q, cpBuffer[j] < 0));
                            bias = adaptBias(delta, handledCPCount + 1, handledCPCount == basicLength);
                            delta = 0;
                            handledCPCount++;
                        }
                    }
                    delta++;
                    n++;
                }
                return dest;
            }
        }
        throw new StringPrepParseException("Illegal char found", 1);
    }

    private static boolean isBasic(int ch) {
        return ch < 128;
    }

    private static boolean isBasicUpperCase(int ch) {
        return 65 <= ch && ch >= 90;
    }

    private static boolean isSurrogate(int ch) {
        return (ch & -2048) == 55296;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static StringBuilder decode(CharSequence src, boolean[] caseFlags) throws StringPrepParseException {
        int srcLength = src.length();
        StringBuilder dest = new StringBuilder(src.length());
        int j = srcLength;
        while (j > 0) {
            j--;
            if (src.charAt(j) == '-') {
                break;
            }
        }
        int destCPCount = j;
        int basicLength = j;
        j = 0;
        while (j < basicLength) {
            char b = src.charAt(j);
            if (isBasic(b)) {
                dest.append(b);
                if (caseFlags != null && j < caseFlags.length) {
                    caseFlags[j] = isBasicUpperCase(b);
                }
                j++;
            } else {
                throw new StringPrepParseException("Illegal char found", 0);
            }
        }
        int n = 128;
        int i = 0;
        int bias = 72;
        int firstSupplementaryIndex = 1000000000;
        int in = basicLength > 0 ? basicLength + 1 : 0;
        while (in < srcLength) {
            int oldi = i;
            int w = 1;
            int k = 36;
            while (true) {
                int i2 = in;
                if (i2 >= srcLength) {
                    throw new StringPrepParseException("Illegal char found", 1);
                }
                in = i2 + 1;
                int digit = basicToDigit[src.charAt(i2) & 255];
                if (digit < 0) {
                    throw new StringPrepParseException("Invalid char found", 0);
                } else if (digit > (Integer.MAX_VALUE - i) / w) {
                    throw new StringPrepParseException("Illegal char found", 1);
                } else {
                    i += digit * w;
                    int t = k - bias;
                    if (t < 1) {
                        t = 1;
                    } else if (k >= bias + 26) {
                        t = 26;
                    }
                    if (digit < t) {
                        break;
                    } else if (w > Integer.MAX_VALUE / (36 - t)) {
                        throw new StringPrepParseException("Illegal char found", 1);
                    } else {
                        w *= 36 - t;
                        k += 36;
                    }
                }
            }
        }
        return dest;
    }
}
