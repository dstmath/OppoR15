package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.text.TextUtils.TruncateAt;
import android.text.method.MetaKeyKeyListener;
import android.text.style.AlignmentSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LeadingMarginSpan.LeadingMarginSpan2;
import android.text.style.LineBackgroundSpan;
import android.text.style.ParagraphStyle;
import android.text.style.ReplacementSpan;
import android.text.style.TabStopSpan;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.util.Arrays;

public abstract class Layout {
    private static final /* synthetic */ int[] -android-text-Layout$AlignmentSwitchesValues = null;
    public static final int BREAK_STRATEGY_BALANCED = 2;
    public static final int BREAK_STRATEGY_HIGH_QUALITY = 1;
    public static final int BREAK_STRATEGY_SIMPLE = 0;
    public static final Directions DIRS_ALL_LEFT_TO_RIGHT = new Directions(new int[]{0, RUN_LENGTH_MASK});
    public static final Directions DIRS_ALL_RIGHT_TO_LEFT = new Directions(new int[]{0, 134217727});
    public static final int DIR_LEFT_TO_RIGHT = 1;
    static final int DIR_REQUEST_DEFAULT_LTR = 2;
    static final int DIR_REQUEST_DEFAULT_RTL = -2;
    static final int DIR_REQUEST_LTR = 1;
    static final int DIR_REQUEST_RTL = -1;
    public static final int DIR_RIGHT_TO_LEFT = -1;
    public static final int HYPHENATION_FREQUENCY_FULL = 2;
    public static final int HYPHENATION_FREQUENCY_NONE = 0;
    public static final int HYPHENATION_FREQUENCY_NORMAL = 1;
    public static final int JUSTIFICATION_MODE_INTER_WORD = 1;
    public static final int JUSTIFICATION_MODE_NONE = 0;
    private static final ParagraphStyle[] NO_PARA_SPANS = ((ParagraphStyle[]) ArrayUtils.emptyArray(ParagraphStyle.class));
    static final int RUN_LENGTH_MASK = 67108863;
    static final int RUN_LEVEL_MASK = 63;
    static final int RUN_LEVEL_SHIFT = 26;
    static final int RUN_RTL_FLAG = 67108864;
    private static final int TAB_INCREMENT = 20;
    private static final Rect sTempRect = new Rect();
    private Alignment mAlignment;
    private int mJustificationMode;
    private SpanSet<LineBackgroundSpan> mLineBackgroundSpans;
    private TextPaint mPaint;
    private float mSpacingAdd;
    private float mSpacingMult;
    private boolean mSpannedText;
    private CharSequence mText;
    private TextDirectionHeuristic mTextDir;
    private int mWidth;

    public enum Alignment {
        ALIGN_NORMAL,
        ALIGN_OPPOSITE,
        ALIGN_CENTER,
        ALIGN_LEFT,
        ALIGN_RIGHT
    }

    public static class Directions {
        public int[] mDirections;

        public Directions(int[] dirs) {
            this.mDirections = dirs;
        }
    }

    static class Ellipsizer implements CharSequence, GetChars {
        Layout mLayout;
        TruncateAt mMethod;
        CharSequence mText;
        int mWidth;

        public Ellipsizer(CharSequence s) {
            this.mText = s;
        }

        public char charAt(int off) {
            char[] buf = TextUtils.obtain(1);
            getChars(off, off + 1, buf, 0);
            char ret = buf[0];
            TextUtils.recycle(buf);
            return ret;
        }

        public void getChars(int start, int end, char[] dest, int destoff) {
            int line1 = this.mLayout.getLineForOffset(start);
            int line2 = this.mLayout.getLineForOffset(end);
            TextUtils.getChars(this.mText, start, end, dest, destoff);
            for (int i = line1; i <= line2; i++) {
                this.mLayout.ellipsize(start, end, i, dest, destoff, this.mMethod);
            }
        }

        public int length() {
            return this.mText.length();
        }

        public CharSequence subSequence(int start, int end) {
            char[] s = new char[(end - start)];
            getChars(start, end, s, 0);
            return new String(s);
        }

        public String toString() {
            char[] s = new char[length()];
            getChars(0, length(), s, 0);
            return new String(s);
        }
    }

    static class SpannedEllipsizer extends Ellipsizer implements Spanned {
        private Spanned mSpanned;

        public SpannedEllipsizer(CharSequence display) {
            super(display);
            this.mSpanned = (Spanned) display;
        }

        public <T> T[] getSpans(int start, int end, Class<T> type) {
            return this.mSpanned.getSpans(start, end, type);
        }

        public int getSpanStart(Object tag) {
            return this.mSpanned.getSpanStart(tag);
        }

        public int getSpanEnd(Object tag) {
            return this.mSpanned.getSpanEnd(tag);
        }

        public int getSpanFlags(Object tag) {
            return this.mSpanned.getSpanFlags(tag);
        }

        public int nextSpanTransition(int start, int limit, Class type) {
            return this.mSpanned.nextSpanTransition(start, limit, type);
        }

        public CharSequence subSequence(int start, int end) {
            char[] s = new char[(end - start)];
            getChars(start, end, s, 0);
            SpannableString ss = new SpannableString(new String(s));
            TextUtils.copySpansFrom(this.mSpanned, start, end, Object.class, ss, 0);
            return ss;
        }
    }

    static class TabStops {
        private int mIncrement;
        private int mNumStops;
        private int[] mStops;

        TabStops(int increment, Object[] spans) {
            reset(increment, spans);
        }

        void reset(int increment, Object[] spans) {
            this.mIncrement = increment;
            int ns = 0;
            if (spans != null) {
                int[] stops = this.mStops;
                int length = spans.length;
                int i = 0;
                int ns2 = 0;
                while (i < length) {
                    Object o = spans[i];
                    if (o instanceof TabStopSpan) {
                        if (stops == null) {
                            stops = new int[10];
                        } else if (ns2 == stops.length) {
                            int[] nstops = new int[(ns2 * 2)];
                            for (int i2 = 0; i2 < ns2; i2++) {
                                nstops[i2] = stops[i2];
                            }
                            stops = nstops;
                        }
                        ns = ns2 + 1;
                        stops[ns2] = ((TabStopSpan) o).getTabStop();
                    } else {
                        ns = ns2;
                    }
                    i++;
                    ns2 = ns;
                }
                if (ns2 > 1) {
                    Arrays.sort(stops, 0, ns2);
                }
                if (stops != this.mStops) {
                    this.mStops = stops;
                    ns = ns2;
                } else {
                    ns = ns2;
                }
            }
            this.mNumStops = ns;
        }

        float nextTab(float h) {
            int ns = this.mNumStops;
            if (ns > 0) {
                int[] stops = this.mStops;
                for (int i = 0; i < ns; i++) {
                    int stop = stops[i];
                    if (((float) stop) > h) {
                        return (float) stop;
                    }
                }
            }
            return nextDefaultStop(h, this.mIncrement);
        }

        public static float nextDefaultStop(float h, int inc) {
            return (float) (((int) ((((float) inc) + h) / ((float) inc))) * inc);
        }
    }

    private static /* synthetic */ int[] -getandroid-text-Layout$AlignmentSwitchesValues() {
        if (-android-text-Layout$AlignmentSwitchesValues != null) {
            return -android-text-Layout$AlignmentSwitchesValues;
        }
        int[] iArr = new int[Alignment.values().length];
        try {
            iArr[Alignment.ALIGN_CENTER.ordinal()] = 3;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Alignment.ALIGN_LEFT.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Alignment.ALIGN_NORMAL.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Alignment.ALIGN_OPPOSITE.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Alignment.ALIGN_RIGHT.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        -android-text-Layout$AlignmentSwitchesValues = iArr;
        return iArr;
    }

    public abstract int getBottomPadding();

    public abstract int getEllipsisCount(int i);

    public abstract int getEllipsisStart(int i);

    public abstract boolean getLineContainsTab(int i);

    public abstract int getLineCount();

    public abstract int getLineDescent(int i);

    public abstract Directions getLineDirections(int i);

    public abstract int getLineStart(int i);

    public abstract int getLineTop(int i);

    public abstract int getParagraphDirection(int i);

    public abstract int getTopPadding();

    public static float getDesiredWidth(CharSequence source, TextPaint paint) {
        return getDesiredWidth(source, 0, source.length(), paint);
    }

    public static float getDesiredWidth(CharSequence source, int start, int end, TextPaint paint) {
        return getDesiredWidth(source, start, end, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR);
    }

    public static float getDesiredWidth(CharSequence source, int start, int end, TextPaint paint, TextDirectionHeuristic textDir) {
        float need = 0.0f;
        int i = start;
        while (i <= end) {
            int next = TextUtils.indexOf(source, 10, i, end);
            if (next < 0) {
                next = end;
            }
            float w = measurePara(paint, source, i, next, textDir);
            if (w > need) {
                need = w;
            }
            i = next + 1;
        }
        return need;
    }

    protected Layout(CharSequence text, TextPaint paint, int width, Alignment align, float spacingMult, float spacingAdd) {
        this(text, paint, width, align, TextDirectionHeuristics.FIRSTSTRONG_LTR, spacingMult, spacingAdd);
    }

    protected Layout(CharSequence text, TextPaint paint, int width, Alignment align, TextDirectionHeuristic textDir, float spacingMult, float spacingAdd) {
        this.mAlignment = Alignment.ALIGN_NORMAL;
        if (width < 0) {
            throw new IllegalArgumentException("Layout: " + width + " < 0");
        }
        if (paint != null) {
            paint.bgColor = 0;
            paint.baselineShift = 0;
        }
        this.mText = text;
        this.mPaint = paint;
        this.mWidth = width;
        this.mAlignment = align;
        this.mSpacingMult = spacingMult;
        this.mSpacingAdd = spacingAdd;
        this.mSpannedText = text instanceof Spanned;
        this.mTextDir = textDir;
    }

    protected void setJustificationMode(int justificationMode) {
        this.mJustificationMode = justificationMode;
    }

    void replaceWith(CharSequence text, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd) {
        if (width < 0) {
            throw new IllegalArgumentException("Layout: " + width + " < 0");
        }
        this.mText = text;
        this.mPaint = paint;
        this.mWidth = width;
        this.mAlignment = align;
        this.mSpacingMult = spacingmult;
        this.mSpacingAdd = spacingadd;
        this.mSpannedText = text instanceof Spanned;
    }

    public void draw(Canvas c) {
        draw(c, null, null, 0);
    }

    public void draw(Canvas canvas, Path highlight, Paint highlightPaint, int cursorOffsetVertical) {
        long lineRange = getLineRangeForDraw(canvas);
        int firstLine = TextUtils.unpackRangeStartFromLong(lineRange);
        int lastLine = TextUtils.unpackRangeEndFromLong(lineRange);
        if (lastLine >= 0) {
            drawBackground(canvas, highlight, highlightPaint, cursorOffsetVertical, firstLine, lastLine);
            drawText(canvas, firstLine, lastLine);
        }
    }

    private boolean isJustificationRequired(int lineNum) {
        boolean z = false;
        if (this.mJustificationMode == 0) {
            return false;
        }
        int lineEnd = getLineEnd(lineNum);
        if (lineEnd < this.mText.length() && this.mText.charAt(lineEnd - 1) != 10) {
            z = true;
        }
        return z;
    }

    private float getJustifyWidth(int lineNum) {
        int indentWidth;
        Alignment paraAlign = this.mAlignment;
        int left = 0;
        int right = this.mWidth;
        int dir = getParagraphDirection(lineNum);
        ParagraphStyle[] spans = NO_PARA_SPANS;
        if (this.mSpannedText) {
            int n;
            Spanned sp = this.mText;
            int start = getLineStart(lineNum);
            boolean isFirstParaLine = start == 0 || this.mText.charAt(start - 1) == 10;
            if (isFirstParaLine) {
                spans = (ParagraphStyle[]) getParagraphSpans(sp, start, sp.nextSpanTransition(start, this.mText.length(), ParagraphStyle.class), ParagraphStyle.class);
                for (n = spans.length - 1; n >= 0; n--) {
                    if (spans[n] instanceof AlignmentSpan) {
                        paraAlign = ((AlignmentSpan) spans[n]).getAlignment();
                        break;
                    }
                }
            }
            int length = spans.length;
            boolean useFirstLineMargin = isFirstParaLine;
            for (n = 0; n < length; n++) {
                if (spans[n] instanceof LeadingMarginSpan2) {
                    if (lineNum < getLineForOffset(sp.getSpanStart(spans[n])) + ((LeadingMarginSpan2) spans[n]).getLeadingMarginLineCount()) {
                        useFirstLineMargin = true;
                        break;
                    }
                }
            }
            for (n = 0; n < length; n++) {
                if (spans[n] instanceof LeadingMarginSpan) {
                    LeadingMarginSpan margin = spans[n];
                    if (dir == -1) {
                        right -= margin.getLeadingMargin(useFirstLineMargin);
                    } else {
                        left += margin.getLeadingMargin(useFirstLineMargin);
                    }
                }
            }
        }
        if (getLineContainsTab(lineNum)) {
            TabStops tabStops = new TabStops(20, spans);
        }
        Alignment align = paraAlign == Alignment.ALIGN_LEFT ? dir == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE : paraAlign == Alignment.ALIGN_RIGHT ? dir == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL : paraAlign;
        if (align == Alignment.ALIGN_NORMAL) {
            if (dir == 1) {
                indentWidth = getIndentAdjust(lineNum, Alignment.ALIGN_LEFT);
            } else {
                indentWidth = -getIndentAdjust(lineNum, Alignment.ALIGN_RIGHT);
            }
        } else if (align != Alignment.ALIGN_OPPOSITE) {
            indentWidth = getIndentAdjust(lineNum, Alignment.ALIGN_CENTER);
        } else if (dir == 1) {
            indentWidth = -getIndentAdjust(lineNum, Alignment.ALIGN_RIGHT);
        } else {
            indentWidth = getIndentAdjust(lineNum, Alignment.ALIGN_LEFT);
        }
        return (float) ((right - left) - indentWidth);
    }

    public void drawText(android.graphics.Canvas r50, int r51, int r52) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r27_3 'tabStops' android.text.Layout$TabStops) in PHI: PHI: (r27_4 'tabStops' android.text.Layout$TabStops) = (r27_3 'tabStops' android.text.Layout$TabStops), (r27_5 'tabStops' android.text.Layout$TabStops) binds: {(r27_3 'tabStops' android.text.Layout$TabStops)=B:47:0x0163, (r27_5 'tabStops' android.text.Layout$TabStops)=B:70:0x01dd}
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
        r49 = this;
        r0 = r49;
        r1 = r51;
        r37 = r0.getLineTop(r1);
        r0 = r49;
        r1 = r51;
        r38 = r0.getLineStart(r1);
        r41 = NO_PARA_SPANS;
        r40 = 0;
        r0 = r49;
        r5 = r0.mPaint;
        r0 = r49;
        r11 = r0.mText;
        r0 = r49;
        r0 = r0.mAlignment;
        r36 = r0;
        r27 = 0;
        r44 = 0;
        r46 = android.text.TextLine.obtain();
        r33 = r51;
        r43 = r27;
    L_0x002e:
        r0 = r33;
        r1 = r52;
        if (r0 > r1) goto L_0x028a;
    L_0x0034:
        r12 = r38;
        r4 = r33 + 1;
        r0 = r49;
        r38 = r0.getLineStart(r4);
        r0 = r49;
        r1 = r33;
        r31 = r0.isJustificationRequired(r1);
        r0 = r49;
        r1 = r33;
        r2 = r38;
        r13 = r0.getLineVisibleEnd(r1, r12, r2);
        r8 = r37;
        r4 = r33 + 1;
        r0 = r49;
        r10 = r0.getLineTop(r4);
        r37 = r10;
        r0 = r49;
        r1 = r33;
        r4 = r0.getLineDescent(r1);
        r9 = r10 - r4;
        r0 = r49;
        r1 = r33;
        r7 = r0.getParagraphDirection(r1);
        r18 = 0;
        r0 = r49;
        r6 = r0.mWidth;
        r0 = r49;
        r4 = r0.mSpannedText;
        if (r4 == 0) goto L_0x0153;
    L_0x007a:
        r39 = r11;
        r39 = (android.text.Spanned) r39;
        r45 = r11.length();
        if (r12 == 0) goto L_0x008e;
    L_0x0084:
        r4 = r12 + -1;
        r4 = r11.charAt(r4);
        r15 = 10;
        if (r4 != r15) goto L_0x0127;
    L_0x008e:
        r14 = 1;
    L_0x008f:
        r0 = r40;
        if (r12 < r0) goto L_0x00ce;
    L_0x0093:
        r0 = r33;
        r1 = r51;
        if (r0 == r1) goto L_0x009b;
    L_0x0099:
        if (r14 == 0) goto L_0x00ce;
    L_0x009b:
        r4 = android.text.style.ParagraphStyle.class;
        r0 = r39;
        r1 = r45;
        r40 = r0.nextSpanTransition(r12, r1, r4);
        r4 = android.text.style.ParagraphStyle.class;
        r0 = r39;
        r1 = r40;
        r41 = getParagraphSpans(r0, r12, r1, r4);
        r41 = (android.text.style.ParagraphStyle[]) r41;
        r0 = r49;
        r0 = r0.mAlignment;
        r36 = r0;
        r0 = r41;
        r4 = r0.length;
        r35 = r4 + -1;
    L_0x00bc:
        if (r35 < 0) goto L_0x00cc;
    L_0x00be:
        r4 = r41[r35];
        r4 = r4 instanceof android.text.style.AlignmentSpan;
        if (r4 == 0) goto L_0x012a;
    L_0x00c4:
        r4 = r41[r35];
        r4 = (android.text.style.AlignmentSpan) r4;
        r36 = r4.getAlignment();
    L_0x00cc:
        r44 = 0;
    L_0x00ce:
        r0 = r41;
        r0 = r0.length;
        r32 = r0;
        r47 = r14;
        r35 = 0;
    L_0x00d7:
        r0 = r35;
        r1 = r32;
        if (r0 >= r1) goto L_0x0101;
    L_0x00dd:
        r4 = r41[r35];
        r4 = r4 instanceof android.text.style.LeadingMarginSpan.LeadingMarginSpan2;
        if (r4 == 0) goto L_0x012d;
    L_0x00e3:
        r4 = r41[r35];
        r4 = (android.text.style.LeadingMarginSpan.LeadingMarginSpan2) r4;
        r29 = r4.getLeadingMarginLineCount();
        r4 = r41[r35];
        r0 = r39;
        r4 = r0.getSpanStart(r4);
        r0 = r49;
        r42 = r0.getLineForOffset(r4);
        r4 = r42 + r29;
        r0 = r33;
        if (r0 >= r4) goto L_0x012d;
    L_0x00ff:
        r47 = 1;
    L_0x0101:
        r35 = 0;
    L_0x0103:
        r0 = r35;
        r1 = r32;
        if (r0 >= r1) goto L_0x0153;
    L_0x0109:
        r4 = r41[r35];
        r4 = r4 instanceof android.text.style.LeadingMarginSpan;
        if (r4 == 0) goto L_0x0124;
    L_0x010f:
        r3 = r41[r35];
        r3 = (android.text.style.LeadingMarginSpan) r3;
        r4 = -1;
        if (r7 != r4) goto L_0x0130;
    L_0x0116:
        r4 = r50;
        r15 = r49;
        r3.drawLeadingMargin(r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15);
        r0 = r47;
        r4 = r3.getLeadingMargin(r0);
        r6 = r6 - r4;
    L_0x0124:
        r35 = r35 + 1;
        goto L_0x0103;
    L_0x0127:
        r14 = 0;
        goto L_0x008f;
    L_0x012a:
        r35 = r35 + -1;
        goto L_0x00bc;
    L_0x012d:
        r35 = r35 + 1;
        goto L_0x00d7;
    L_0x0130:
        r15 = r3;
        r16 = r50;
        r17 = r5;
        r19 = r7;
        r20 = r8;
        r21 = r9;
        r22 = r10;
        r23 = r11;
        r24 = r12;
        r25 = r13;
        r26 = r14;
        r27 = r49;
        r15.drawLeadingMargin(r16, r17, r18, r19, r20, r21, r22, r23, r24, r25, r26, r27);
        r0 = r47;
        r4 = r3.getLeadingMargin(r0);
        r18 = r18 + r4;
        goto L_0x0124;
    L_0x0153:
        r0 = r49;
        r1 = r33;
        r26 = r0.getLineContainsTab(r1);
        if (r26 == 0) goto L_0x01da;
    L_0x015d:
        r4 = r44 ^ 1;
        if (r4 == 0) goto L_0x028e;
    L_0x0161:
        if (r43 != 0) goto L_0x01dd;
    L_0x0163:
        r27 = new android.text.Layout$TabStops;
        r4 = 20;
        r0 = r27;
        r1 = r41;
        r0.<init>(r4, r1);
    L_0x016e:
        r44 = 1;
    L_0x0170:
        r28 = r36;
        r4 = android.text.Layout.Alignment.ALIGN_LEFT;
        r0 = r28;
        if (r0 != r4) goto L_0x01ec;
    L_0x0178:
        r4 = 1;
        if (r7 != r4) goto L_0x01e9;
    L_0x017b:
        r28 = android.text.Layout.Alignment.ALIGN_NORMAL;
    L_0x017d:
        r4 = android.text.Layout.Alignment.ALIGN_NORMAL;
        r0 = r28;
        if (r0 != r4) goto L_0x020b;
    L_0x0183:
        r4 = 1;
        if (r7 != r4) goto L_0x01fb;
    L_0x0186:
        r4 = android.text.Layout.Alignment.ALIGN_LEFT;
        r0 = r49;
        r1 = r33;
        r30 = r0.getIndentAdjust(r1, r4);
        r48 = r18 + r30;
    L_0x0192:
        r0 = r49;
        r1 = r33;
        r4 = r0.getHyphen(r1);
        r5.setHyphenEdit(r4);
        r0 = r49;
        r1 = r33;
        r25 = r0.getLineDirections(r1);
        r4 = DIRS_ALL_LEFT_TO_RIGHT;
        r0 = r25;
        if (r0 != r4) goto L_0x025b;
    L_0x01ab:
        r0 = r49;
        r4 = r0.mSpannedText;
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x025b;
    L_0x01b3:
        r4 = r26 ^ 1;
        if (r4 == 0) goto L_0x025b;
    L_0x01b7:
        r4 = r31 ^ 1;
        if (r4 == 0) goto L_0x025b;
    L_0x01bb:
        r0 = r48;
        r0 = (float) r0;
        r23 = r0;
        r0 = (float) r9;
        r24 = r0;
        r19 = r50;
        r20 = r11;
        r21 = r12;
        r22 = r13;
        r25 = r5;
        r19.drawText(r20, r21, r22, r23, r24, r25);
    L_0x01d0:
        r4 = 0;
        r5.setHyphenEdit(r4);
        r33 = r33 + 1;
        r43 = r27;
        goto L_0x002e;
    L_0x01da:
        r27 = r43;
        goto L_0x0170;
    L_0x01dd:
        r4 = 20;
        r0 = r43;
        r1 = r41;
        r0.reset(r4, r1);
        r27 = r43;
        goto L_0x016e;
    L_0x01e9:
        r28 = android.text.Layout.Alignment.ALIGN_OPPOSITE;
        goto L_0x017d;
    L_0x01ec:
        r4 = android.text.Layout.Alignment.ALIGN_RIGHT;
        r0 = r28;
        if (r0 != r4) goto L_0x017d;
    L_0x01f2:
        r4 = 1;
        if (r7 != r4) goto L_0x01f8;
    L_0x01f5:
        r28 = android.text.Layout.Alignment.ALIGN_OPPOSITE;
        goto L_0x017d;
    L_0x01f8:
        r28 = android.text.Layout.Alignment.ALIGN_NORMAL;
        goto L_0x017d;
    L_0x01fb:
        r4 = android.text.Layout.Alignment.ALIGN_RIGHT;
        r0 = r49;
        r1 = r33;
        r4 = r0.getIndentAdjust(r1, r4);
        r0 = -r4;
        r30 = r0;
        r48 = r6 - r30;
        goto L_0x0192;
    L_0x020b:
        r4 = 0;
        r0 = r49;
        r1 = r33;
        r2 = r27;
        r4 = r0.getLineExtent(r1, r2, r4);
        r0 = (int) r4;
        r34 = r0;
        r4 = android.text.Layout.Alignment.ALIGN_OPPOSITE;
        r0 = r28;
        if (r0 != r4) goto L_0x0245;
    L_0x021f:
        r4 = 1;
        if (r7 != r4) goto L_0x0235;
    L_0x0222:
        r4 = android.text.Layout.Alignment.ALIGN_RIGHT;
        r0 = r49;
        r1 = r33;
        r4 = r0.getIndentAdjust(r1, r4);
        r0 = -r4;
        r30 = r0;
        r4 = r6 - r34;
        r48 = r4 - r30;
        goto L_0x0192;
    L_0x0235:
        r4 = android.text.Layout.Alignment.ALIGN_LEFT;
        r0 = r49;
        r1 = r33;
        r30 = r0.getIndentAdjust(r1, r4);
        r4 = r18 - r34;
        r48 = r4 + r30;
        goto L_0x0192;
    L_0x0245:
        r4 = android.text.Layout.Alignment.ALIGN_CENTER;
        r0 = r49;
        r1 = r33;
        r30 = r0.getIndentAdjust(r1, r4);
        r34 = r34 & -2;
        r4 = r6 + r18;
        r4 = r4 - r34;
        r4 = r4 >> 1;
        r48 = r4 + r30;
        goto L_0x0192;
    L_0x025b:
        r19 = r46;
        r20 = r5;
        r21 = r11;
        r22 = r12;
        r23 = r13;
        r24 = r7;
        r19.set(r20, r21, r22, r23, r24, r25, r26, r27);
        if (r31 == 0) goto L_0x0276;
    L_0x026c:
        r4 = r6 - r18;
        r4 = r4 - r30;
        r4 = (float) r4;
        r0 = r46;
        r0.justify(r4);
    L_0x0276:
        r0 = r48;
        r0 = (float) r0;
        r21 = r0;
        r19 = r46;
        r20 = r50;
        r22 = r8;
        r23 = r9;
        r24 = r10;
        r19.draw(r20, r21, r22, r23, r24);
        goto L_0x01d0;
    L_0x028a:
        android.text.TextLine.recycle(r46);
        return;
    L_0x028e:
        r27 = r43;
        goto L_0x0170;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.text.Layout.drawText(android.graphics.Canvas, int, int):void");
    }

    public void drawBackground(Canvas canvas, Path highlight, Paint highlightPaint, int cursorOffsetVertical, int firstLine, int lastLine) {
        if (this.mSpannedText) {
            if (this.mLineBackgroundSpans == null) {
                this.mLineBackgroundSpans = new SpanSet(LineBackgroundSpan.class);
            }
            Spanned buffer = this.mText;
            int textLength = buffer.length();
            this.mLineBackgroundSpans.init(buffer, 0, textLength);
            if (this.mLineBackgroundSpans.numberOfSpans > 0) {
                int previousLineBottom = getLineTop(firstLine);
                int previousLineEnd = getLineStart(firstLine);
                ParagraphStyle[] spans = NO_PARA_SPANS;
                int spansLength = 0;
                TextPaint paint = this.mPaint;
                int spanEnd = 0;
                int width = this.mWidth;
                for (int i = firstLine; i <= lastLine; i++) {
                    int start = previousLineEnd;
                    int end = getLineStart(i + 1);
                    previousLineEnd = end;
                    int ltop = previousLineBottom;
                    int lbottom = getLineTop(i + 1);
                    previousLineBottom = lbottom;
                    int lbaseline = lbottom - getLineDescent(i);
                    if (start >= spanEnd) {
                        spanEnd = this.mLineBackgroundSpans.getNextTransition(start, textLength);
                        spansLength = 0;
                        if (start != end || start == 0) {
                            int j = 0;
                            while (j < this.mLineBackgroundSpans.numberOfSpans) {
                                if (this.mLineBackgroundSpans.spanStarts[j] < end && this.mLineBackgroundSpans.spanEnds[j] > start) {
                                    spans = (ParagraphStyle[]) GrowingArrayUtils.append((Object[]) spans, spansLength, ((LineBackgroundSpan[]) this.mLineBackgroundSpans.spans)[j]);
                                    spansLength++;
                                }
                                j++;
                            }
                        }
                    }
                    for (int n = 0; n < spansLength; n++) {
                        spans[n].drawBackground(canvas, paint, 0, width, ltop, lbaseline, lbottom, buffer, start, end, i);
                    }
                }
            }
            this.mLineBackgroundSpans.recycle();
        }
        if (highlight != null) {
            if (cursorOffsetVertical != 0) {
                canvas.translate(0.0f, (float) cursorOffsetVertical);
            }
            canvas.drawPath(highlight, highlightPaint);
            if (cursorOffsetVertical != 0) {
                canvas.translate(0.0f, (float) (-cursorOffsetVertical));
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getLineRangeForDraw(Canvas canvas) {
        synchronized (sTempRect) {
            if (canvas.getClipBounds(sTempRect)) {
                int dtop = sTempRect.top;
                int dbottom = sTempRect.bottom;
            } else {
                long packRangeInLong = TextUtils.packRangeInLong(0, -1);
                return packRangeInLong;
            }
        }
    }

    private int getLineStartPos(int line, int left, int right) {
        Alignment align = getParagraphAlignment(line);
        int dir = getParagraphDirection(line);
        if (align == Alignment.ALIGN_LEFT) {
            align = dir == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE;
        } else if (align == Alignment.ALIGN_RIGHT) {
            align = dir == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL;
        }
        if (align != Alignment.ALIGN_NORMAL) {
            TabStops tabStops = null;
            if (this.mSpannedText && getLineContainsTab(line)) {
                Spanned spanned = this.mText;
                int start = getLineStart(line);
                TabStopSpan[] tabSpans = (TabStopSpan[]) getParagraphSpans(spanned, start, spanned.nextSpanTransition(start, spanned.length(), TabStopSpan.class), TabStopSpan.class);
                if (tabSpans.length > 0) {
                    tabStops = new TabStops(20, tabSpans);
                }
            }
            int max = (int) getLineExtent(line, tabStops, false);
            if (align != Alignment.ALIGN_OPPOSITE) {
                return ((left + right) - (max & -2)) >> (getIndentAdjust(line, Alignment.ALIGN_CENTER) + 1);
            } else if (dir == 1) {
                return (right - max) + getIndentAdjust(line, Alignment.ALIGN_RIGHT);
            } else {
                return (left - max) + getIndentAdjust(line, Alignment.ALIGN_LEFT);
            }
        } else if (dir == 1) {
            return left + getIndentAdjust(line, Alignment.ALIGN_LEFT);
        } else {
            return right + getIndentAdjust(line, Alignment.ALIGN_RIGHT);
        }
    }

    public final CharSequence getText() {
        return this.mText;
    }

    public final TextPaint getPaint() {
        return this.mPaint;
    }

    public final int getWidth() {
        return this.mWidth;
    }

    public int getEllipsizedWidth() {
        return this.mWidth;
    }

    public final void increaseWidthTo(int wid) {
        if (wid < this.mWidth) {
            throw new RuntimeException("attempted to reduce Layout width");
        }
        this.mWidth = wid;
    }

    public int getHeight() {
        return getLineTop(getLineCount());
    }

    public int getHeight(boolean cap) {
        return getHeight();
    }

    public final Alignment getAlignment() {
        return this.mAlignment;
    }

    public final float getSpacingMultiplier() {
        return this.mSpacingMult;
    }

    public final float getSpacingAdd() {
        return this.mSpacingAdd;
    }

    public final TextDirectionHeuristic getTextDirectionHeuristic() {
        return this.mTextDir;
    }

    public int getLineBounds(int line, Rect bounds) {
        if (bounds != null) {
            bounds.left = 0;
            bounds.top = getLineTop(line);
            bounds.right = this.mWidth;
            bounds.bottom = getLineTop(line + 1);
        }
        return getLineBaseline(line);
    }

    public int getHyphen(int line) {
        return 0;
    }

    public int getIndentAdjust(int line, Alignment alignment) {
        return 0;
    }

    public boolean isLevelBoundary(int offset) {
        boolean z = true;
        int line = getLineForOffset(offset);
        Directions dirs = getLineDirections(line);
        if (dirs == DIRS_ALL_LEFT_TO_RIGHT || dirs == DIRS_ALL_RIGHT_TO_LEFT) {
            return false;
        }
        int[] runs = dirs.mDirections;
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        if (offset == lineStart || offset == lineEnd) {
            if (((runs[(offset == lineStart ? 0 : runs.length - 2) + 1] >>> 26) & 63) == (getParagraphDirection(line) == 1 ? 0 : 1)) {
                z = false;
            }
            return z;
        }
        offset -= lineStart;
        for (int i = 0; i < runs.length; i += 2) {
            if (offset == runs[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean isRtlCharAt(int offset) {
        boolean z = true;
        int line = getLineForOffset(offset);
        Directions dirs = getLineDirections(line);
        if (dirs == DIRS_ALL_LEFT_TO_RIGHT) {
            return false;
        }
        if (dirs == DIRS_ALL_RIGHT_TO_LEFT) {
            return true;
        }
        int[] runs = dirs.mDirections;
        int lineStart = getLineStart(line);
        int i = 0;
        while (i < runs.length) {
            int start = lineStart + runs[i];
            int limit = start + (runs[i + 1] & RUN_LENGTH_MASK);
            if (offset < start || offset >= limit) {
                i += 2;
            } else {
                if ((((runs[i + 1] >>> 26) & 63) & 1) == 0) {
                    z = false;
                }
                return z;
            }
        }
        return false;
    }

    public long getRunRange(int offset) {
        int line = getLineForOffset(offset);
        Directions dirs = getLineDirections(line);
        if (dirs == DIRS_ALL_LEFT_TO_RIGHT || dirs == DIRS_ALL_RIGHT_TO_LEFT) {
            return TextUtils.packRangeInLong(0, getLineEnd(line));
        }
        int[] runs = dirs.mDirections;
        int lineStart = getLineStart(line);
        for (int i = 0; i < runs.length; i += 2) {
            int start = lineStart + runs[i];
            int limit = start + (runs[i + 1] & RUN_LENGTH_MASK);
            if (offset >= start && offset < limit) {
                return TextUtils.packRangeInLong(start, limit);
            }
        }
        return TextUtils.packRangeInLong(0, getLineEnd(line));
    }

    private boolean primaryIsTrailingPrevious(int offset) {
        int levelBefore;
        boolean z = true;
        int line = getLineForOffset(offset);
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        int[] runs = getLineDirections(line).mDirections;
        int levelAt = -1;
        int i = 0;
        while (i < runs.length) {
            int start = lineStart + runs[i];
            int limit = start + (runs[i + 1] & RUN_LENGTH_MASK);
            if (limit > lineEnd) {
                limit = lineEnd;
            }
            if (offset < start || offset >= limit) {
                i += 2;
            } else if (offset > start) {
                return false;
            } else {
                levelAt = (runs[i + 1] >>> 26) & 63;
                if (levelAt == -1) {
                    levelAt = getParagraphDirection(line) == 1 ? 0 : 1;
                }
                levelBefore = -1;
                if (offset == lineStart) {
                    offset--;
                    for (i = 0; i < runs.length; i += 2) {
                        start = lineStart + runs[i];
                        limit = start + (runs[i + 1] & RUN_LENGTH_MASK);
                        if (limit > lineEnd) {
                            limit = lineEnd;
                        }
                        if (offset >= start && offset < limit) {
                            levelBefore = (runs[i + 1] >>> 26) & 63;
                            break;
                        }
                    }
                } else if (getParagraphDirection(line) == 1) {
                    levelBefore = 0;
                } else {
                    levelBefore = 1;
                }
                if (levelBefore >= levelAt) {
                    z = false;
                }
                return z;
            }
        }
        if (levelAt == -1) {
            levelAt = getParagraphDirection(line) == 1 ? 0 : 1;
        }
        levelBefore = -1;
        if (offset == lineStart) {
            offset--;
            for (i = 0; i < runs.length; i += 2) {
                start = lineStart + runs[i];
                limit = start + (runs[i + 1] & RUN_LENGTH_MASK);
                if (limit > lineEnd) {
                    limit = lineEnd;
                }
                if (offset >= start && offset < limit) {
                    levelBefore = (runs[i + 1] >>> 26) & 63;
                    break;
                }
            }
        } else if (getParagraphDirection(line) == 1) {
            levelBefore = 0;
        } else {
            levelBefore = 1;
        }
        if (levelBefore >= levelAt) {
            z = false;
        }
        return z;
    }

    public float getPrimaryHorizontal(int offset) {
        return getPrimaryHorizontal(offset, false);
    }

    public float getPrimaryHorizontal(int offset, boolean clamped) {
        return getHorizontal(offset, primaryIsTrailingPrevious(offset), clamped);
    }

    public float getSecondaryHorizontal(int offset) {
        return getSecondaryHorizontal(offset, false);
    }

    public float getSecondaryHorizontal(int offset, boolean clamped) {
        return getHorizontal(offset, primaryIsTrailingPrevious(offset) ^ 1, clamped);
    }

    private float getHorizontal(int offset, boolean primary) {
        return primary ? getPrimaryHorizontal(offset) : getSecondaryHorizontal(offset);
    }

    private float getHorizontal(int offset, boolean trailing, boolean clamped) {
        return getHorizontal(offset, trailing, getLineForOffset(offset), clamped);
    }

    private float getHorizontal(int offset, boolean trailing, int line, boolean clamped) {
        int start = getLineStart(line);
        int end = getLineEnd(line);
        int dir = getParagraphDirection(line);
        boolean hasTab = getLineContainsTab(line);
        Directions directions = getLineDirections(line);
        TabStops tabStops = null;
        if (hasTab && (this.mText instanceof Spanned)) {
            TabStopSpan[] tabs = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, start, end, TabStopSpan.class);
            if (tabs.length > 0) {
                tabStops = new TabStops(20, tabs);
            }
        }
        TextLine tl = TextLine.obtain();
        tl.set(this.mPaint, this.mText, start, end, dir, directions, hasTab, tabStops);
        float wid = tl.measure(offset - start, trailing, null);
        TextLine.recycle(tl);
        if (clamped && wid > ((float) this.mWidth)) {
            wid = (float) this.mWidth;
        }
        return ((float) getLineStartPos(line, getParagraphLeft(line), getParagraphRight(line))) + wid;
    }

    public float getLineLeft(int line) {
        int dir = getParagraphDirection(line);
        Alignment align = getParagraphAlignment(line);
        if (align == Alignment.ALIGN_LEFT) {
            return 0.0f;
        }
        if (align == Alignment.ALIGN_NORMAL) {
            if (dir == -1) {
                return ((float) getParagraphRight(line)) - getLineMax(line);
            }
            return 0.0f;
        } else if (align == Alignment.ALIGN_RIGHT) {
            return ((float) this.mWidth) - getLineMax(line);
        } else {
            if (align != Alignment.ALIGN_OPPOSITE) {
                int left = getParagraphLeft(line);
                return (float) ((((getParagraphRight(line) - left) - (((int) getLineMax(line)) & -2)) / 2) + left);
            } else if (dir == -1) {
                return 0.0f;
            } else {
                return ((float) this.mWidth) - getLineMax(line);
            }
        }
    }

    public float getLineRight(int line) {
        int dir = getParagraphDirection(line);
        Alignment align = getParagraphAlignment(line);
        if (align == Alignment.ALIGN_LEFT) {
            return ((float) getParagraphLeft(line)) + getLineMax(line);
        }
        if (align == Alignment.ALIGN_NORMAL) {
            if (dir == -1) {
                return (float) this.mWidth;
            }
            return ((float) getParagraphLeft(line)) + getLineMax(line);
        } else if (align == Alignment.ALIGN_RIGHT) {
            return (float) this.mWidth;
        } else {
            if (align != Alignment.ALIGN_OPPOSITE) {
                int left = getParagraphLeft(line);
                int right = getParagraphRight(line);
                return (float) (right - (((right - left) - (((int) getLineMax(line)) & -2)) / 2));
            } else if (dir == -1) {
                return getLineMax(line);
            } else {
                return (float) this.mWidth;
            }
        }
    }

    public float getLineMax(int line) {
        float margin = (float) getParagraphLeadingMargin(line);
        float signedExtent = getLineExtent(line, false);
        if (signedExtent < 0.0f) {
            signedExtent = -signedExtent;
        }
        return margin + signedExtent;
    }

    public float getLineWidth(int line) {
        float margin = (float) getParagraphLeadingMargin(line);
        float signedExtent = getLineExtent(line, true);
        if (signedExtent < 0.0f) {
            signedExtent = -signedExtent;
        }
        return margin + signedExtent;
    }

    private float getLineExtent(int line, boolean full) {
        int start = getLineStart(line);
        int end = full ? getLineEnd(line) : getLineVisibleEnd(line);
        boolean hasTabs = getLineContainsTab(line);
        TabStops tabStops = null;
        if (hasTabs && (this.mText instanceof Spanned)) {
            TabStopSpan[] tabs = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, start, end, TabStopSpan.class);
            if (tabs.length > 0) {
                tabStops = new TabStops(20, tabs);
            }
        }
        Directions directions = getLineDirections(line);
        if (directions == null) {
            return 0.0f;
        }
        int dir = getParagraphDirection(line);
        TextLine tl = TextLine.obtain();
        this.mPaint.setHyphenEdit(getHyphen(line));
        tl.set(this.mPaint, this.mText, start, end, dir, directions, hasTabs, tabStops);
        if (isJustificationRequired(line)) {
            tl.justify(getJustifyWidth(line));
        }
        float width = tl.metrics(null);
        this.mPaint.setHyphenEdit(0);
        TextLine.recycle(tl);
        return width;
    }

    private float getLineExtent(int line, TabStops tabStops, boolean full) {
        int start = getLineStart(line);
        int end = full ? getLineEnd(line) : getLineVisibleEnd(line);
        boolean hasTabs = getLineContainsTab(line);
        Directions directions = getLineDirections(line);
        int dir = getParagraphDirection(line);
        TextLine tl = TextLine.obtain();
        this.mPaint.setHyphenEdit(getHyphen(line));
        tl.set(this.mPaint, this.mText, start, end, dir, directions, hasTabs, tabStops);
        if (isJustificationRequired(line)) {
            tl.justify(getJustifyWidth(line));
        }
        float width = tl.metrics(null);
        this.mPaint.setHyphenEdit(0);
        TextLine.recycle(tl);
        return width;
    }

    public int getLineForVertical(int vertical) {
        int high = getLineCount();
        int low = -1;
        while (high - low > 1) {
            int guess = (high + low) / 2;
            if (getLineTop(guess) > vertical) {
                high = guess;
            } else {
                low = guess;
            }
        }
        if (low < 0) {
            return 0;
        }
        return low;
    }

    public int getLineForOffset(int offset) {
        int high = getLineCount();
        int low = -1;
        while (high - low > 1) {
            int guess = (high + low) / 2;
            if (getLineStart(guess) > offset) {
                high = guess;
            } else {
                low = guess;
            }
        }
        if (low < 0) {
            return 0;
        }
        return low;
    }

    public int getOffsetForHorizontal(int line, float horiz) {
        return getOffsetForHorizontal(line, horiz, true);
    }

    public int getOffsetForHorizontal(int line, float horiz, boolean primary) {
        int max;
        float dist;
        int lineEndOffset = getLineEnd(line);
        int lineStartOffset = getLineStart(line);
        Directions dirs = getLineDirections(line);
        TextLine tl = TextLine.obtain();
        tl.set(this.mPaint, this.mText, lineStartOffset, lineEndOffset, getParagraphDirection(line), dirs, false, null);
        if (line == getLineCount() - 1) {
            max = lineEndOffset;
        } else {
            max = tl.getOffsetToLeftRightOf(lineEndOffset - lineStartOffset, isRtlCharAt(lineEndOffset - 1) ^ 1) + lineStartOffset;
        }
        int best = lineStartOffset;
        float bestdist = Math.abs(getHorizontal(lineStartOffset, primary) - horiz);
        for (int i = 0; i < dirs.mDirections.length; i += 2) {
            int here = lineStartOffset + dirs.mDirections[i];
            int there = here + (dirs.mDirections[i + 1] & RUN_LENGTH_MASK);
            boolean isRtl = (dirs.mDirections[i + 1] & 67108864) != 0;
            int swap = isRtl ? -1 : 1;
            if (there > max) {
                there = max;
            }
            int high = (there - 1) + 1;
            int low = (here + 1) - 1;
            while (high - low > 1) {
                int guess = (high + low) / 2;
                if (getHorizontal(getOffsetAtStartOf(guess), primary) * ((float) swap) >= ((float) swap) * horiz) {
                    high = guess;
                } else {
                    low = guess;
                }
            }
            if (low < here + 1) {
                low = here + 1;
            }
            if (low < there) {
                int aft = tl.getOffsetToLeftRightOf(low - lineStartOffset, isRtl) + lineStartOffset;
                low = tl.getOffsetToLeftRightOf(aft - lineStartOffset, isRtl ^ 1) + lineStartOffset;
                if (low >= here && low < there) {
                    dist = Math.abs(getHorizontal(low, primary) - horiz);
                    if (aft < there) {
                        float other = Math.abs(getHorizontal(aft, primary) - horiz);
                        if (other < dist) {
                            dist = other;
                            low = aft;
                        }
                    }
                    if (dist < bestdist) {
                        bestdist = dist;
                        best = low;
                    }
                }
            }
            dist = Math.abs(getHorizontal(here, primary) - horiz);
            if (dist < bestdist) {
                bestdist = dist;
                best = here;
            }
        }
        dist = Math.abs(getHorizontal(max, primary) - horiz);
        if (dist <= bestdist) {
            bestdist = dist;
            best = max;
        }
        TextLine.recycle(tl);
        return best;
    }

    public final int getLineEnd(int line) {
        return getLineStart(line + 1);
    }

    public int getLineVisibleEnd(int line) {
        return getLineVisibleEnd(line, getLineStart(line), getLineStart(line + 1));
    }

    private int getLineVisibleEnd(int line, int start, int end) {
        CharSequence text = this.mText;
        if (line == getLineCount() - 1) {
            return end;
        }
        while (end > start) {
            char ch = text.charAt(end - 1);
            if (ch == 10) {
                return end - 1;
            }
            if (!TextLine.isLineEndSpace(ch)) {
                break;
            }
            end--;
        }
        return end;
    }

    public final int getLineBottom(int line) {
        return getLineTop(line + 1);
    }

    public final int getLineBaseline(int line) {
        return getLineTop(line + 1) - getLineDescent(line);
    }

    public final int getLineAscent(int line) {
        return getLineTop(line) - (getLineTop(line + 1) - getLineDescent(line));
    }

    public int getOffsetToLeftOf(int offset) {
        return getOffsetToLeftRightOf(offset, true);
    }

    public int getOffsetToRightOf(int offset) {
        return getOffsetToLeftRightOf(offset, false);
    }

    private int getOffsetToLeftRightOf(int caret, boolean toLeft) {
        boolean z;
        int line = getLineForOffset(caret);
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        int lineDir = getParagraphDirection(line);
        boolean lineChanged = false;
        if (lineDir == -1) {
            z = true;
        } else {
            z = false;
        }
        if (toLeft == z) {
            if (caret == lineEnd) {
                if (line >= getLineCount() - 1) {
                    return caret;
                }
                lineChanged = true;
                line++;
            }
        } else if (caret == lineStart) {
            if (line <= 0) {
                return caret;
            }
            lineChanged = true;
            line--;
        }
        if (lineChanged) {
            lineStart = getLineStart(line);
            lineEnd = getLineEnd(line);
            int newDir = getParagraphDirection(line);
            if (newDir != lineDir) {
                toLeft ^= 1;
                lineDir = newDir;
            }
        }
        Directions directions = getLineDirections(line);
        TextLine tl = TextLine.obtain();
        tl.set(this.mPaint, this.mText, lineStart, lineEnd, lineDir, directions, false, null);
        caret = lineStart + tl.getOffsetToLeftRightOf(caret - lineStart, toLeft);
        tl = TextLine.recycle(tl);
        return caret;
    }

    private int getOffsetAtStartOf(int offset) {
        if (offset == 0) {
            return 0;
        }
        CharSequence text = this.mText;
        char c = text.charAt(offset);
        if (c >= 56320 && c <= 57343) {
            char c1 = text.charAt(offset - 1);
            if (c1 >= 55296 && c1 <= 56319) {
                offset--;
            }
        }
        if (this.mSpannedText) {
            ReplacementSpan[] spans = (ReplacementSpan[]) ((Spanned) text).getSpans(offset, offset, ReplacementSpan.class);
            for (int i = 0; i < spans.length; i++) {
                int start = ((Spanned) text).getSpanStart(spans[i]);
                int end = ((Spanned) text).getSpanEnd(spans[i]);
                if (start < offset && end > offset) {
                    offset = start;
                }
            }
        }
        return offset;
    }

    public boolean shouldClampCursor(int line) {
        boolean z = true;
        switch (-getandroid-text-Layout$AlignmentSwitchesValues()[getParagraphAlignment(line).ordinal()]) {
            case 1:
                return true;
            case 2:
                if (getParagraphDirection(line) <= 0) {
                    z = false;
                }
                return z;
            default:
                return false;
        }
    }

    public void getCursorPath(int point, Path dest, CharSequence editingBuffer) {
        dest.reset();
        int line = getLineForOffset(point);
        int top = getLineTop(line);
        int bottom = getLineTop(line + 1);
        boolean clamped = shouldClampCursor(line);
        float h1 = getPrimaryHorizontal(point, clamped) - 0.5f;
        float h2 = isLevelBoundary(point) ? getSecondaryHorizontal(point, clamped) - 0.5f : h1;
        int caps = MetaKeyKeyListener.getMetaState(editingBuffer, 1) | MetaKeyKeyListener.getMetaState(editingBuffer, 2048);
        int fn = MetaKeyKeyListener.getMetaState(editingBuffer, 2);
        int dist = 0;
        if (!(caps == 0 && fn == 0)) {
            dist = (bottom - top) >> 2;
            if (fn != 0) {
                top += dist;
            }
            if (caps != 0) {
                bottom -= dist;
            }
        }
        if (h1 < 0.5f) {
            h1 = 0.5f;
        }
        if (h2 < 0.5f) {
            h2 = 0.5f;
        }
        if (Float.compare(h1, h2) == 0) {
            dest.moveTo(h1, (float) top);
            dest.lineTo(h1, (float) bottom);
        } else {
            dest.moveTo(h1, (float) top);
            dest.lineTo(h1, (float) ((top + bottom) >> 1));
            dest.moveTo(h2, (float) ((top + bottom) >> 1));
            dest.lineTo(h2, (float) bottom);
        }
        if (caps == 2) {
            dest.moveTo(h2, (float) bottom);
            dest.lineTo(h2 - ((float) dist), (float) (bottom + dist));
            dest.lineTo(h2, (float) bottom);
            dest.lineTo(((float) dist) + h2, (float) (bottom + dist));
        } else if (caps == 1) {
            dest.moveTo(h2, (float) bottom);
            dest.lineTo(h2 - ((float) dist), (float) (bottom + dist));
            dest.moveTo(h2 - ((float) dist), ((float) (bottom + dist)) - 0.5f);
            dest.lineTo(((float) dist) + h2, ((float) (bottom + dist)) - 0.5f);
            dest.moveTo(((float) dist) + h2, (float) (bottom + dist));
            dest.lineTo(h2, (float) bottom);
        }
        if (fn == 2) {
            dest.moveTo(h1, (float) top);
            dest.lineTo(h1 - ((float) dist), (float) (top - dist));
            dest.lineTo(h1, (float) top);
            dest.lineTo(((float) dist) + h1, (float) (top - dist));
        } else if (fn == 1) {
            dest.moveTo(h1, (float) top);
            dest.lineTo(h1 - ((float) dist), (float) (top - dist));
            dest.moveTo(h1 - ((float) dist), ((float) (top - dist)) + 0.5f);
            dest.lineTo(((float) dist) + h1, ((float) (top - dist)) + 0.5f);
            dest.moveTo(((float) dist) + h1, (float) (top - dist));
            dest.lineTo(h1, (float) top);
        }
    }

    private void addSelection(int line, int start, int end, int top, int bottom, Path dest) {
        int linestart = getLineStart(line);
        int lineend = getLineEnd(line);
        Directions dirs = getLineDirections(line);
        if (lineend > linestart && this.mText.charAt(lineend - 1) == 10) {
            lineend--;
        }
        for (int i = 0; i < dirs.mDirections.length; i += 2) {
            int here = linestart + dirs.mDirections[i];
            int there = here + (dirs.mDirections[i + 1] & RUN_LENGTH_MASK);
            if (there > lineend) {
                there = lineend;
            }
            if (start <= there && end >= here) {
                int st = Math.max(start, here);
                int en = Math.min(end, there);
                if (st != en) {
                    float h1 = getHorizontal(st, false, line, false);
                    float h2 = getHorizontal(en, true, line, false);
                    Path path = dest;
                    path.addRect(Math.min(h1, h2), (float) top, Math.max(h1, h2), (float) bottom, Direction.CW);
                }
            }
        }
    }

    public void getSelectionPath(int start, int end, Path dest) {
        dest.reset();
        if (start != end) {
            if (end < start) {
                int temp = end;
                end = start;
                start = temp;
            }
            int startline = getLineForOffset(start);
            int endline = getLineForOffset(end);
            int top = getLineTop(startline);
            int bottom = getLineBottom(endline);
            if (startline == endline) {
                addSelection(startline, start, end, top, bottom, dest);
            } else {
                float width = (float) this.mWidth;
                addSelection(startline, start, getLineEnd(startline), top, getLineBottom(startline), dest);
                if (getParagraphDirection(startline) == -1) {
                    dest.addRect(getLineLeft(startline), (float) top, 0.0f, (float) getLineBottom(startline), Direction.CW);
                } else {
                    dest.addRect(getLineRight(startline), (float) top, width, (float) getLineBottom(startline), Direction.CW);
                }
                for (int i = startline + 1; i < endline; i++) {
                    Path path = dest;
                    float f = width;
                    path.addRect(0.0f, (float) getLineTop(i), f, (float) getLineBottom(i), Direction.CW);
                }
                top = getLineTop(endline);
                bottom = getLineBottom(endline);
                addSelection(endline, getLineStart(endline), end, top, bottom, dest);
                if (getParagraphDirection(endline) == -1) {
                    dest.addRect(width, (float) top, getLineRight(endline), (float) bottom, Direction.CW);
                } else {
                    dest.addRect(0.0f, (float) top, getLineLeft(endline), (float) bottom, Direction.CW);
                }
            }
        }
    }

    public final Alignment getParagraphAlignment(int line) {
        Alignment align = this.mAlignment;
        if (!this.mSpannedText) {
            return align;
        }
        AlignmentSpan[] spans = (AlignmentSpan[]) getParagraphSpans(this.mText, getLineStart(line), getLineEnd(line), AlignmentSpan.class);
        int spanLength = spans.length;
        if (spanLength > 0) {
            return spans[spanLength - 1].getAlignment();
        }
        return align;
    }

    public final int getParagraphLeft(int line) {
        if (getParagraphDirection(line) == -1 || (this.mSpannedText ^ 1) != 0) {
            return 0;
        }
        return getParagraphLeadingMargin(line);
    }

    public final int getParagraphRight(int line) {
        int right = this.mWidth;
        if (getParagraphDirection(line) == 1 || (this.mSpannedText ^ 1) != 0) {
            return right;
        }
        return right - getParagraphLeadingMargin(line);
    }

    private int getParagraphLeadingMargin(int line) {
        if (!this.mSpannedText) {
            return 0;
        }
        Spanned spanned = this.mText;
        int lineStart = getLineStart(line);
        LeadingMarginSpan[] spans = (LeadingMarginSpan[]) getParagraphSpans(spanned, lineStart, spanned.nextSpanTransition(lineStart, getLineEnd(line), LeadingMarginSpan.class), LeadingMarginSpan.class);
        if (spans.length == 0) {
            return 0;
        }
        int i;
        int margin = 0;
        boolean isFirstParaLine = lineStart != 0 ? spanned.charAt(lineStart + -1) == 10 : true;
        boolean useFirstLineMargin = isFirstParaLine;
        for (i = 0; i < spans.length; i++) {
            if (spans[i] instanceof LeadingMarginSpan2) {
                int i2;
                if (line < getLineForOffset(spanned.getSpanStart(spans[i])) + ((LeadingMarginSpan2) spans[i]).getLeadingMarginLineCount()) {
                    i2 = 1;
                } else {
                    i2 = 0;
                }
                useFirstLineMargin |= i2;
            }
        }
        for (LeadingMarginSpan span : spans) {
            margin += span.getLeadingMargin(useFirstLineMargin);
        }
        return margin;
    }

    static float measurePara(TextPaint paint, CharSequence text, int start, int end, TextDirectionHeuristic textDir) {
        MeasuredText mt = MeasuredText.obtain();
        TextLine tl = TextLine.obtain();
        try {
            Directions directions;
            int dir;
            float abs;
            mt.setPara(text, start, end, textDir, null);
            if (mt.mEasy) {
                directions = DIRS_ALL_LEFT_TO_RIGHT;
                dir = 1;
            } else {
                directions = AndroidBidi.directions(mt.mDir, mt.mLevels, 0, mt.mChars, 0, mt.mLen);
                dir = mt.mDir;
            }
            char[] chars = mt.mChars;
            int len = mt.mLen;
            boolean hasTabs = false;
            TabStops tabStops = null;
            int margin = 0;
            if (text instanceof Spanned) {
                for (LeadingMarginSpan lms : (LeadingMarginSpan[]) getParagraphSpans((Spanned) text, start, end, LeadingMarginSpan.class)) {
                    margin += lms.getLeadingMargin(true);
                }
            }
            for (int i = 0; i < len; i++) {
                if (chars[i] == 9) {
                    hasTabs = true;
                    if (text instanceof Spanned) {
                        Spanned spanned = (Spanned) text;
                        TabStopSpan[] spans = (TabStopSpan[]) getParagraphSpans(spanned, start, spanned.nextSpanTransition(start, end, TabStopSpan.class), TabStopSpan.class);
                        if (spans.length > 0) {
                            tabStops = new TabStops(20, spans);
                        }
                    }
                    tl.set(paint, text, start, end, dir, directions, hasTabs, tabStops);
                    abs = ((float) margin) + Math.abs(tl.metrics(null));
                    return abs;
                }
            }
            tl.set(paint, text, start, end, dir, directions, hasTabs, tabStops);
            abs = ((float) margin) + Math.abs(tl.metrics(null));
            return abs;
        } finally {
            TextLine.recycle(tl);
            MeasuredText.recycle(mt);
        }
    }

    static float nextTab(CharSequence text, int start, int end, float h, Object[] tabs) {
        float nh = Float.MAX_VALUE;
        boolean alltabs = false;
        if (text instanceof Spanned) {
            if (tabs == null) {
                tabs = getParagraphSpans((Spanned) text, start, end, TabStopSpan.class);
                alltabs = true;
            }
            int i = 0;
            while (i < tabs.length) {
                if (alltabs || (tabs[i] instanceof TabStopSpan)) {
                    int where = ((TabStopSpan) tabs[i]).getTabStop();
                    if (((float) where) < nh && ((float) where) > h) {
                        nh = (float) where;
                    }
                }
                i++;
            }
            if (nh != Float.MAX_VALUE) {
                return nh;
            }
        }
        return (float) (((int) ((h + 20.0f) / 20.0f)) * 20);
    }

    protected final boolean isSpanned() {
        return this.mSpannedText;
    }

    static <T> T[] getParagraphSpans(Spanned text, int start, int end, Class<T> type) {
        if (start == end && start > 0) {
            return ArrayUtils.emptyArray(type);
        }
        if (text instanceof SpannableStringBuilder) {
            return ((SpannableStringBuilder) text).getSpans(start, end, type, false);
        }
        return text.getSpans(start, end, type);
    }

    private char getEllipsisChar(TruncateAt method) {
        if (method == TruncateAt.END_SMALL) {
            return TextUtils.ELLIPSIS_TWO_DOTS[0];
        }
        return TextUtils.ELLIPSIS_NORMAL[0];
    }

    private void ellipsize(int start, int end, int line, char[] dest, int destoff, TruncateAt method) {
        int ellipsisCount = getEllipsisCount(line);
        if (ellipsisCount != 0) {
            int ellipsisStart = getEllipsisStart(line);
            int linestart = getLineStart(line);
            for (int i = ellipsisStart; i < ellipsisStart + ellipsisCount; i++) {
                char c;
                if (i == ellipsisStart) {
                    c = getEllipsisChar(method);
                } else {
                    c = 65279;
                }
                int a = i + linestart;
                if (a >= start && a < end) {
                    dest[(destoff + a) - start] = c;
                }
            }
        }
    }
}
