package android.text;

import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.text.Layout.Alignment;
import android.text.Layout.Directions;
import android.text.StaticLayout.Builder;
import android.text.TextUtils.TruncateAt;
import android.text.style.ReplacementSpan;
import android.text.style.UpdateLayout;
import android.util.ArraySet;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.lang.ref.WeakReference;

public class DynamicLayout extends Layout {
    private static final int BLOCK_MINIMUM_CHARACTER_LENGTH = 400;
    private static final int COLUMNS_ELLIPSIZE = 6;
    private static final int COLUMNS_NORMAL = 4;
    private static final int DESCENT = 2;
    private static final int DIR = 0;
    private static final int DIR_SHIFT = 30;
    private static final int ELLIPSIS_COUNT = 5;
    private static final int ELLIPSIS_START = 4;
    private static final int ELLIPSIS_UNDEFINED = Integer.MIN_VALUE;
    private static final int HYPHEN = 3;
    private static final int HYPHEN_MASK = 255;
    public static final int INVALID_BLOCK_INDEX = -1;
    private static final int MAY_PROTRUDE_FROM_TOP_OR_BOTTOM = 3;
    private static final int MAY_PROTRUDE_FROM_TOP_OR_BOTTOM_MASK = 256;
    private static final int PRIORITY = 128;
    private static final int START = 0;
    private static final int START_MASK = 536870911;
    private static final int TAB = 0;
    private static final int TAB_MASK = 536870912;
    private static final int TOP = 1;
    private static Builder sBuilder = null;
    private static final Object[] sLock = new Object[0];
    private static StaticLayout sStaticLayout = null;
    private CharSequence mBase;
    private int[] mBlockEndLines;
    private int[] mBlockIndices;
    private ArraySet<Integer> mBlocksAlwaysNeedToBeRedrawn;
    private int mBottomPadding;
    private int mBreakStrategy;
    private CharSequence mDisplay;
    private boolean mEllipsize;
    private TruncateAt mEllipsizeAt;
    private int mEllipsizedWidth;
    private int mHyphenationFrequency;
    private boolean mIncludePad;
    private int mIndexFirstChangedBlock;
    private PackedIntVector mInts;
    private int mJustificationMode;
    private int mNumberOfBlocks;
    private PackedObjectVector<Directions> mObjects;
    private Rect mTempRect;
    private int mTopPadding;
    private ChangeWatcher mWatcher;

    private static class ChangeWatcher implements TextWatcher, SpanWatcher {
        private WeakReference<DynamicLayout> mLayout;

        public ChangeWatcher(DynamicLayout layout) {
            this.mLayout = new WeakReference(layout);
        }

        private void reflow(CharSequence s, int where, int before, int after) {
            DynamicLayout ml = (DynamicLayout) this.mLayout.get();
            if (ml != null) {
                ml.reflow(s, where, before, after);
            } else if (s instanceof Spannable) {
                ((Spannable) s).removeSpan(this);
            }
        }

        public void beforeTextChanged(CharSequence s, int where, int before, int after) {
        }

        public void onTextChanged(CharSequence s, int where, int before, int after) {
            reflow(s, where, before, after);
        }

        public void afterTextChanged(Editable s) {
        }

        public void onSpanAdded(Spannable s, Object o, int start, int end) {
            if (o instanceof UpdateLayout) {
                reflow(s, start, end - start, end - start);
            }
        }

        public void onSpanRemoved(Spannable s, Object o, int start, int end) {
            if (o instanceof UpdateLayout) {
                reflow(s, start, end - start, end - start);
            }
        }

        public void onSpanChanged(Spannable s, Object o, int start, int end, int nstart, int nend) {
            if (o instanceof UpdateLayout) {
                reflow(s, start, end - start, end - start);
                reflow(s, nstart, nend - nstart, nend - nstart);
            }
        }
    }

    public DynamicLayout(CharSequence base, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(base, base, paint, width, align, spacingmult, spacingadd, includepad);
    }

    public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(base, display, paint, width, align, spacingmult, spacingadd, includepad, null, 0);
    }

    public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd, boolean includepad, TruncateAt ellipsize, int ellipsizedWidth) {
        this(base, display, paint, width, align, TextDirectionHeuristics.FIRSTSTRONG_LTR, spacingmult, spacingadd, includepad, 0, 0, 0, ellipsize, ellipsizedWidth);
    }

    public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment align, TextDirectionHeuristic textDir, float spacingmult, float spacingadd, boolean includepad, int breakStrategy, int hyphenationFrequency, int justificationMode, TruncateAt ellipsize, int ellipsizedWidth) {
        CharSequence charSequence;
        int[] start;
        if (ellipsize == null) {
            charSequence = display;
        } else if (display instanceof Spanned) {
            charSequence = new SpannedEllipsizer(display);
        } else {
            charSequence = new Ellipsizer(display);
        }
        super(charSequence, paint, width, align, textDir, spacingmult, spacingadd);
        this.mTempRect = new Rect();
        this.mBase = base;
        this.mDisplay = display;
        if (ellipsize != null) {
            this.mInts = new PackedIntVector(6);
            this.mEllipsizedWidth = ellipsizedWidth;
            this.mEllipsizeAt = ellipsize;
        } else {
            this.mInts = new PackedIntVector(4);
            this.mEllipsizedWidth = width;
            this.mEllipsizeAt = null;
        }
        this.mObjects = new PackedObjectVector(1);
        this.mIncludePad = includepad;
        this.mBreakStrategy = breakStrategy;
        this.mJustificationMode = justificationMode;
        this.mHyphenationFrequency = hyphenationFrequency;
        if (ellipsize != null) {
            Ellipsizer e = (Ellipsizer) getText();
            e.mLayout = this;
            e.mWidth = ellipsizedWidth;
            e.mMethod = ellipsize;
            this.mEllipsize = true;
        }
        if (ellipsize != null) {
            start = new int[6];
            start[4] = Integer.MIN_VALUE;
        } else {
            start = new int[4];
        }
        Directions[] dirs = new Directions[]{DIRS_ALL_LEFT_TO_RIGHT};
        FontMetricsInt fm = paint.getFontMetricsInt();
        int asc = fm.ascent;
        int desc = fm.descent;
        start[0] = 1073741824;
        start[1] = 0;
        start[2] = desc;
        this.mInts.insertAt(0, start);
        start[1] = desc - asc;
        this.mInts.insertAt(1, start);
        this.mObjects.insertAt(0, dirs);
        reflow(base, 0, 0, base.length());
        if (base instanceof Spannable) {
            if (this.mWatcher == null) {
                this.mWatcher = new ChangeWatcher(this);
            }
            Spannable sp = (Spannable) base;
            ChangeWatcher[] spans = (ChangeWatcher[]) sp.getSpans(0, sp.length(), ChangeWatcher.class);
            for (Object removeSpan : spans) {
                sp.removeSpan(removeSpan);
            }
            sp.setSpan(this.mWatcher, 0, base.length(), 8388626);
        }
    }

    private void reflow(java.lang.CharSequence r38, int r39, int r40, int r41) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r25_1 'reflowed' android.text.StaticLayout) in PHI: PHI: (r25_2 'reflowed' android.text.StaticLayout) = (r25_0 'reflowed' android.text.StaticLayout), (r25_1 'reflowed' android.text.StaticLayout) binds: {(r25_0 'reflowed' android.text.StaticLayout)=B:36:0x00f8, (r25_1 'reflowed' android.text.StaticLayout)=B:37:0x00fa}
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
        r37 = this;
        r0 = r37;
        r0 = r0.mBase;
        r34 = r0;
        r0 = r38;
        r1 = r34;
        if (r0 == r1) goto L_0x000d;
    L_0x000c:
        return;
    L_0x000d:
        r0 = r37;
        r0 = r0.mDisplay;
        r31 = r0;
        r21 = r31.length();
        r34 = r39 + -1;
        r35 = 10;
        r0 = r31;
        r1 = r35;
        r2 = r34;
        r15 = android.text.TextUtils.lastIndexOf(r0, r1, r2);
        if (r15 >= 0) goto L_0x00a9;
    L_0x0027:
        r15 = 0;
    L_0x0028:
        r10 = r39 - r15;
        r40 = r40 + r10;
        r41 = r41 + r10;
        r39 = r39 - r10;
        r34 = r39 + r41;
        r35 = 10;
        r0 = r31;
        r1 = r35;
        r2 = r34;
        r22 = android.text.TextUtils.indexOf(r0, r1, r2);
        if (r22 >= 0) goto L_0x00ad;
    L_0x0040:
        r22 = r21;
    L_0x0042:
        r34 = r39 + r41;
        r8 = r22 - r34;
        r40 = r40 + r8;
        r41 = r41 + r8;
        r0 = r31;
        r0 = r0 instanceof android.text.Spanned;
        r34 = r0;
        if (r34 == 0) goto L_0x00b2;
    L_0x0052:
        r26 = r31;
        r26 = (android.text.Spanned) r26;
    L_0x0056:
        r5 = 0;
        r34 = r39 + r41;
        r35 = android.text.style.WrapTogetherSpan.class;
        r0 = r26;
        r1 = r39;
        r2 = r34;
        r3 = r35;
        r16 = r0.getSpans(r1, r2, r3);
        r18 = 0;
    L_0x0069:
        r0 = r16;
        r0 = r0.length;
        r34 = r0;
        r0 = r18;
        r1 = r34;
        if (r0 >= r1) goto L_0x00b0;
    L_0x0074:
        r34 = r16[r18];
        r0 = r26;
        r1 = r34;
        r27 = r0.getSpanStart(r1);
        r34 = r16[r18];
        r0 = r26;
        r1 = r34;
        r11 = r0.getSpanEnd(r1);
        r0 = r27;
        r1 = r39;
        if (r0 >= r1) goto L_0x0097;
    L_0x008e:
        r5 = 1;
        r10 = r39 - r27;
        r40 = r40 + r10;
        r41 = r41 + r10;
        r39 = r39 - r10;
    L_0x0097:
        r34 = r39 + r41;
        r0 = r34;
        if (r11 <= r0) goto L_0x00a6;
    L_0x009d:
        r5 = 1;
        r34 = r39 + r41;
        r10 = r11 - r34;
        r40 = r40 + r10;
        r41 = r41 + r10;
    L_0x00a6:
        r18 = r18 + 1;
        goto L_0x0069;
    L_0x00a9:
        r15 = r15 + 1;
        goto L_0x0028;
    L_0x00ad:
        r22 = r22 + 1;
        goto L_0x0042;
    L_0x00b0:
        if (r5 != 0) goto L_0x0056;
    L_0x00b2:
        r0 = r37;
        r1 = r39;
        r29 = r0.getLineForOffset(r1);
        r0 = r37;
        r1 = r29;
        r30 = r0.getLineTop(r1);
        r34 = r39 + r40;
        r0 = r37;
        r1 = r34;
        r13 = r0.getLineForOffset(r1);
        r34 = r39 + r41;
        r0 = r34;
        r1 = r21;
        if (r0 != r1) goto L_0x00d8;
    L_0x00d4:
        r13 = r37.getLineCount();
    L_0x00d8:
        r0 = r37;
        r14 = r0.getLineTop(r13);
        r34 = r37.getLineCount();
        r0 = r34;
        if (r13 != r0) goto L_0x0331;
    L_0x00e6:
        r20 = 1;
    L_0x00e8:
        r35 = sLock;
        monitor-enter(r35);
        r25 = sStaticLayout;	 Catch:{ all -> 0x0335 }
        r6 = sBuilder;	 Catch:{ all -> 0x0335 }
        r34 = 0;	 Catch:{ all -> 0x0335 }
        sStaticLayout = r34;	 Catch:{ all -> 0x0335 }
        r34 = 0;	 Catch:{ all -> 0x0335 }
        sBuilder = r34;	 Catch:{ all -> 0x0335 }
        monitor-exit(r35);
        if (r25 != 0) goto L_0x011d;
    L_0x00fa:
        r25 = new android.text.StaticLayout;
        r34 = 0;
        r0 = r25;
        r1 = r34;
        r0.<init>(r1);
        r34 = r39 + r41;
        r35 = r37.getPaint();
        r36 = r37.getWidth();
        r0 = r31;
        r1 = r39;
        r2 = r34;
        r3 = r35;
        r4 = r36;
        r6 = android.text.StaticLayout.Builder.obtain(r0, r1, r2, r3, r4);
    L_0x011d:
        r34 = r39 + r41;
        r0 = r31;
        r1 = r39;
        r2 = r34;
        r34 = r6.setText(r0, r1, r2);
        r35 = r37.getPaint();
        r34 = r34.setPaint(r35);
        r35 = r37.getWidth();
        r34 = r34.setWidth(r35);
        r35 = r37.getTextDirectionHeuristic();
        r34 = r34.setTextDirection(r35);
        r35 = r37.getSpacingAdd();
        r36 = r37.getSpacingMultiplier();
        r34 = r34.setLineSpacing(r35, r36);
        r0 = r37;
        r0 = r0.mEllipsizedWidth;
        r35 = r0;
        r34 = r34.setEllipsizedWidth(r35);
        r0 = r37;
        r0 = r0.mEllipsizeAt;
        r35 = r0;
        r34 = r34.setEllipsize(r35);
        r0 = r37;
        r0 = r0.mBreakStrategy;
        r35 = r0;
        r34 = r34.setBreakStrategy(r35);
        r0 = r37;
        r0 = r0.mHyphenationFrequency;
        r35 = r0;
        r34 = r34.setHyphenationFrequency(r35);
        r0 = r37;
        r0 = r0.mJustificationMode;
        r35 = r0;
        r34.setJustificationMode(r35);
        r34 = 0;
        r35 = 1;
        r0 = r25;
        r1 = r34;
        r2 = r35;
        r0.generate(r6, r1, r2);
        r23 = r25.getLineCount();
        r34 = r39 + r41;
        r0 = r34;
        r1 = r21;
        if (r0 == r1) goto L_0x01ab;
    L_0x0197:
        r34 = r23 + -1;
        r0 = r25;
        r1 = r34;
        r34 = r0.getLineStart(r1);
        r35 = r39 + r41;
        r0 = r34;
        r1 = r35;
        if (r0 != r1) goto L_0x01ab;
    L_0x01a9:
        r23 = r23 + -1;
    L_0x01ab:
        r0 = r37;
        r0 = r0.mInts;
        r34 = r0;
        r35 = r13 - r29;
        r0 = r34;
        r1 = r29;
        r2 = r35;
        r0.deleteAt(r1, r2);
        r0 = r37;
        r0 = r0.mObjects;
        r34 = r0;
        r35 = r13 - r29;
        r0 = r34;
        r1 = r29;
        r2 = r35;
        r0.deleteAt(r1, r2);
        r0 = r25;
        r1 = r23;
        r17 = r0.getLineTop(r1);
        r33 = 0;
        r7 = 0;
        r0 = r37;
        r0 = r0.mIncludePad;
        r34 = r0;
        if (r34 == 0) goto L_0x01ee;
    L_0x01e0:
        if (r29 != 0) goto L_0x01ee;
    L_0x01e2:
        r33 = r25.getTopPadding();
        r0 = r33;
        r1 = r37;
        r1.mTopPadding = r0;
        r17 = r17 - r33;
    L_0x01ee:
        r0 = r37;
        r0 = r0.mIncludePad;
        r34 = r0;
        if (r34 == 0) goto L_0x0202;
    L_0x01f6:
        if (r20 == 0) goto L_0x0202;
    L_0x01f8:
        r7 = r25.getBottomPadding();
        r0 = r37;
        r0.mBottomPadding = r7;
        r17 = r17 + r7;
    L_0x0202:
        r0 = r37;
        r0 = r0.mInts;
        r34 = r0;
        r35 = r41 - r40;
        r36 = 0;
        r0 = r34;
        r1 = r29;
        r2 = r36;
        r3 = r35;
        r0.adjustValuesBelow(r1, r2, r3);
        r0 = r37;
        r0 = r0.mInts;
        r34 = r0;
        r35 = r30 - r14;
        r35 = r35 + r17;
        r36 = 1;
        r0 = r34;
        r1 = r29;
        r2 = r36;
        r3 = r35;
        r0.adjustValuesBelow(r1, r2, r3);
        r0 = r37;
        r0 = r0.mEllipsize;
        r34 = r0;
        if (r34 == 0) goto L_0x0338;
    L_0x0236:
        r34 = 6;
        r0 = r34;
        r0 = new int[r0];
        r19 = r0;
        r34 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        r35 = 4;
        r19[r35] = r34;
    L_0x0244:
        r34 = 1;
        r0 = r34;
        r0 = new android.text.Layout.Directions[r0];
        r24 = r0;
        r18 = 0;
    L_0x024e:
        r0 = r18;
        r1 = r23;
        if (r0 >= r1) goto L_0x0355;
    L_0x0254:
        r0 = r25;
        r1 = r18;
        r28 = r0.getLineStart(r1);
        r34 = 0;
        r19[r34] = r28;
        r34 = 0;
        r35 = r19[r34];
        r0 = r25;
        r1 = r18;
        r36 = r0.getParagraphDirection(r1);
        r36 = r36 << 30;
        r35 = r35 | r36;
        r19[r34] = r35;
        r35 = 0;
        r36 = r19[r35];
        r0 = r25;
        r1 = r18;
        r34 = r0.getLineContainsTab(r1);
        if (r34 == 0) goto L_0x0342;
    L_0x0280:
        r34 = 536870912; // 0x20000000 float:1.0842022E-19 double:2.652494739E-315;
    L_0x0282:
        r34 = r34 | r36;
        r19[r35] = r34;
        r0 = r25;
        r1 = r18;
        r34 = r0.getLineTop(r1);
        r32 = r34 + r30;
        if (r18 <= 0) goto L_0x0294;
    L_0x0292:
        r32 = r32 - r33;
    L_0x0294:
        r34 = 1;
        r19[r34] = r32;
        r0 = r25;
        r1 = r18;
        r9 = r0.getLineDescent(r1);
        r34 = r23 + -1;
        r0 = r18;
        r1 = r34;
        if (r0 != r1) goto L_0x02a9;
    L_0x02a8:
        r9 = r9 + r7;
    L_0x02a9:
        r34 = 2;
        r19[r34] = r9;
        r0 = r25;
        r1 = r18;
        r34 = r0.getLineDirections(r1);
        r35 = 0;
        r24[r35] = r34;
        r34 = r23 + -1;
        r0 = r18;
        r1 = r34;
        if (r0 != r1) goto L_0x0346;
    L_0x02c1:
        r12 = r39 + r41;
    L_0x02c3:
        r0 = r25;
        r1 = r18;
        r34 = r0.getHyphen(r1);
        r0 = r34;
        r0 = r0 & 255;
        r34 = r0;
        r35 = 3;
        r19[r35] = r34;
        r35 = 3;
        r36 = r19[r35];
        r0 = r37;
        r1 = r31;
        r2 = r28;
        r34 = r0.contentMayProtrudeFromLineTopOrBottom(r1, r2, r12);
        if (r34 == 0) goto L_0x0352;
    L_0x02e5:
        r34 = 256; // 0x100 float:3.59E-43 double:1.265E-321;
    L_0x02e7:
        r34 = r34 | r36;
        r19[r35] = r34;
        r0 = r37;
        r0 = r0.mEllipsize;
        r34 = r0;
        if (r34 == 0) goto L_0x030b;
    L_0x02f3:
        r0 = r25;
        r1 = r18;
        r34 = r0.getEllipsisStart(r1);
        r35 = 4;
        r19[r35] = r34;
        r0 = r25;
        r1 = r18;
        r34 = r0.getEllipsisCount(r1);
        r35 = 5;
        r19[r35] = r34;
    L_0x030b:
        r0 = r37;
        r0 = r0.mInts;
        r34 = r0;
        r35 = r29 + r18;
        r0 = r34;
        r1 = r35;
        r2 = r19;
        r0.insertAt(r1, r2);
        r0 = r37;
        r0 = r0.mObjects;
        r34 = r0;
        r35 = r29 + r18;
        r0 = r34;
        r1 = r35;
        r2 = r24;
        r0.insertAt(r1, r2);
        r18 = r18 + 1;
        goto L_0x024e;
    L_0x0331:
        r20 = 0;
        goto L_0x00e8;
    L_0x0335:
        r34 = move-exception;
        monitor-exit(r35);
        throw r34;
    L_0x0338:
        r34 = 4;
        r0 = r34;
        r0 = new int[r0];
        r19 = r0;
        goto L_0x0244;
    L_0x0342:
        r34 = 0;
        goto L_0x0282;
    L_0x0346:
        r34 = r18 + 1;
        r0 = r25;
        r1 = r34;
        r12 = r0.getLineStart(r1);
        goto L_0x02c3;
    L_0x0352:
        r34 = 0;
        goto L_0x02e7;
    L_0x0355:
        r34 = r13 + -1;
        r0 = r37;
        r1 = r29;
        r2 = r34;
        r3 = r23;
        r0.updateBlocks(r1, r2, r3);
        r6.finish();
        r35 = sLock;
        monitor-enter(r35);
        sStaticLayout = r25;	 Catch:{ all -> 0x036e }
        sBuilder = r6;	 Catch:{ all -> 0x036e }
        monitor-exit(r35);
        return;
    L_0x036e:
        r34 = move-exception;
        monitor-exit(r35);
        throw r34;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.text.DynamicLayout.reflow(java.lang.CharSequence, int, int, int):void");
    }

    private boolean contentMayProtrudeFromLineTopOrBottom(CharSequence text, int start, int end) {
        if ((text instanceof Spanned) && ((ReplacementSpan[]) ((Spanned) text).getSpans(start, end, ReplacementSpan.class)).length > 0) {
            return true;
        }
        Paint paint = getPaint();
        paint.getTextBounds(text, start, end, this.mTempRect);
        FontMetricsInt fm = paint.getFontMetricsInt();
        boolean z = this.mTempRect.top < fm.top || this.mTempRect.bottom > fm.bottom;
        return z;
    }

    private void createBlocks() {
        int offset = 400;
        this.mNumberOfBlocks = 0;
        CharSequence text = this.mDisplay;
        while (true) {
            offset = TextUtils.indexOf(text, 10, offset);
            if (offset < 0) {
                break;
            }
            addBlockAtOffset(offset);
            offset += 400;
        }
        addBlockAtOffset(text.length());
        this.mBlockIndices = new int[this.mBlockEndLines.length];
        for (int i = 0; i < this.mBlockEndLines.length; i++) {
            this.mBlockIndices[i] = -1;
        }
    }

    public ArraySet<Integer> getBlocksAlwaysNeedToBeRedrawn() {
        return this.mBlocksAlwaysNeedToBeRedrawn;
    }

    private void updateAlwaysNeedsToBeRedrawn(int blockIndex) {
        int startLine = blockIndex == 0 ? 0 : this.mBlockEndLines[blockIndex - 1] + 1;
        int endLine = this.mBlockEndLines[blockIndex];
        for (int i = startLine; i <= endLine; i++) {
            if (getContentMayProtrudeFromTopOrBottom(i)) {
                if (this.mBlocksAlwaysNeedToBeRedrawn == null) {
                    this.mBlocksAlwaysNeedToBeRedrawn = new ArraySet();
                }
                this.mBlocksAlwaysNeedToBeRedrawn.add(Integer.valueOf(blockIndex));
                return;
            }
        }
        if (this.mBlocksAlwaysNeedToBeRedrawn != null) {
            this.mBlocksAlwaysNeedToBeRedrawn.remove(Integer.valueOf(blockIndex));
        }
    }

    private void addBlockAtOffset(int offset) {
        int line = getLineForOffset(offset);
        if (this.mBlockEndLines == null) {
            this.mBlockEndLines = ArrayUtils.newUnpaddedIntArray(1);
            this.mBlockEndLines[this.mNumberOfBlocks] = line;
            updateAlwaysNeedsToBeRedrawn(this.mNumberOfBlocks);
            this.mNumberOfBlocks++;
            return;
        }
        if (line > this.mBlockEndLines[this.mNumberOfBlocks - 1]) {
            this.mBlockEndLines = GrowingArrayUtils.append(this.mBlockEndLines, this.mNumberOfBlocks, line);
            updateAlwaysNeedsToBeRedrawn(this.mNumberOfBlocks);
            this.mNumberOfBlocks++;
        }
    }

    public void updateBlocks(int startLine, int endLine, int newLineCount) {
        if (this.mBlockEndLines == null) {
            createBlocks();
            return;
        }
        int i;
        int i2;
        int firstBlock = -1;
        int lastBlock = -1;
        for (i = 0; i < this.mNumberOfBlocks; i++) {
            if (this.mBlockEndLines[i] >= startLine) {
                firstBlock = i;
                break;
            }
        }
        for (i = firstBlock; i < this.mNumberOfBlocks; i++) {
            if (this.mBlockEndLines[i] >= endLine) {
                lastBlock = i;
                break;
            }
        }
        int lastBlockEndLine = this.mBlockEndLines[lastBlock];
        if (firstBlock == 0) {
            i2 = 0;
        } else {
            i2 = this.mBlockEndLines[firstBlock - 1] + 1;
        }
        boolean createBlockBefore = startLine > i2;
        boolean createBlock = newLineCount > 0;
        boolean createBlockAfter = endLine < this.mBlockEndLines[lastBlock];
        int numAddedBlocks = 0;
        if (createBlockBefore) {
            numAddedBlocks = 1;
        }
        if (createBlock) {
            numAddedBlocks++;
        }
        if (createBlockAfter) {
            numAddedBlocks++;
        }
        int numRemovedBlocks = (lastBlock - firstBlock) + 1;
        int newNumberOfBlocks = (this.mNumberOfBlocks + numAddedBlocks) - numRemovedBlocks;
        if (newNumberOfBlocks == 0) {
            this.mBlockEndLines[0] = 0;
            this.mBlockIndices[0] = -1;
            this.mNumberOfBlocks = 1;
            return;
        }
        int newFirstChangedBlock;
        if (newNumberOfBlocks > this.mBlockEndLines.length) {
            int[] blockEndLines = ArrayUtils.newUnpaddedIntArray(Math.max(this.mBlockEndLines.length * 2, newNumberOfBlocks));
            int[] blockIndices = new int[blockEndLines.length];
            System.arraycopy(this.mBlockEndLines, 0, blockEndLines, 0, firstBlock);
            System.arraycopy(this.mBlockIndices, 0, blockIndices, 0, firstBlock);
            System.arraycopy(this.mBlockEndLines, lastBlock + 1, blockEndLines, firstBlock + numAddedBlocks, (this.mNumberOfBlocks - lastBlock) - 1);
            System.arraycopy(this.mBlockIndices, lastBlock + 1, blockIndices, firstBlock + numAddedBlocks, (this.mNumberOfBlocks - lastBlock) - 1);
            this.mBlockEndLines = blockEndLines;
            this.mBlockIndices = blockIndices;
        } else if (numAddedBlocks + numRemovedBlocks != 0) {
            System.arraycopy(this.mBlockEndLines, lastBlock + 1, this.mBlockEndLines, firstBlock + numAddedBlocks, (this.mNumberOfBlocks - lastBlock) - 1);
            System.arraycopy(this.mBlockIndices, lastBlock + 1, this.mBlockIndices, firstBlock + numAddedBlocks, (this.mNumberOfBlocks - lastBlock) - 1);
        }
        if (!(numAddedBlocks + numRemovedBlocks == 0 || this.mBlocksAlwaysNeedToBeRedrawn == null)) {
            ArraySet<Integer> set = new ArraySet();
            for (i = 0; i < this.mBlocksAlwaysNeedToBeRedrawn.size(); i++) {
                Integer block = (Integer) this.mBlocksAlwaysNeedToBeRedrawn.valueAt(i);
                if (block.intValue() > firstBlock) {
                    block = Integer.valueOf(block.intValue() + (numAddedBlocks - numRemovedBlocks));
                }
                set.add(block);
            }
            this.mBlocksAlwaysNeedToBeRedrawn = set;
        }
        this.mNumberOfBlocks = newNumberOfBlocks;
        int deltaLines = newLineCount - ((endLine - startLine) + 1);
        if (deltaLines != 0) {
            newFirstChangedBlock = firstBlock + numAddedBlocks;
            for (i = newFirstChangedBlock; i < this.mNumberOfBlocks; i++) {
                int[] iArr = this.mBlockEndLines;
                iArr[i] = iArr[i] + deltaLines;
            }
        } else {
            newFirstChangedBlock = this.mNumberOfBlocks;
        }
        this.mIndexFirstChangedBlock = Math.min(this.mIndexFirstChangedBlock, newFirstChangedBlock);
        int blockIndex = firstBlock;
        if (createBlockBefore) {
            this.mBlockEndLines[blockIndex] = startLine - 1;
            updateAlwaysNeedsToBeRedrawn(blockIndex);
            this.mBlockIndices[blockIndex] = -1;
            blockIndex++;
        }
        if (createBlock) {
            this.mBlockEndLines[blockIndex] = (startLine + newLineCount) - 1;
            updateAlwaysNeedsToBeRedrawn(blockIndex);
            this.mBlockIndices[blockIndex] = -1;
            blockIndex++;
        }
        if (createBlockAfter) {
            this.mBlockEndLines[blockIndex] = lastBlockEndLine + deltaLines;
            updateAlwaysNeedsToBeRedrawn(blockIndex);
            this.mBlockIndices[blockIndex] = -1;
        }
    }

    public void setBlocksDataForTest(int[] blockEndLines, int[] blockIndices, int numberOfBlocks, int totalLines) {
        this.mBlockEndLines = new int[blockEndLines.length];
        this.mBlockIndices = new int[blockIndices.length];
        System.arraycopy(blockEndLines, 0, this.mBlockEndLines, 0, blockEndLines.length);
        System.arraycopy(blockIndices, 0, this.mBlockIndices, 0, blockIndices.length);
        this.mNumberOfBlocks = numberOfBlocks;
        while (this.mInts.size() < totalLines) {
            this.mInts.insertAt(this.mInts.size(), new int[4]);
        }
    }

    public int[] getBlockEndLines() {
        return this.mBlockEndLines;
    }

    public int[] getBlockIndices() {
        return this.mBlockIndices;
    }

    public int getBlockIndex(int index) {
        return this.mBlockIndices[index];
    }

    public void setBlockIndex(int index, int blockIndex) {
        this.mBlockIndices[index] = blockIndex;
    }

    public int getNumberOfBlocks() {
        return this.mNumberOfBlocks;
    }

    public int getIndexFirstChangedBlock() {
        return this.mIndexFirstChangedBlock;
    }

    public void setIndexFirstChangedBlock(int i) {
        this.mIndexFirstChangedBlock = i;
    }

    public int getLineCount() {
        return this.mInts.size() - 1;
    }

    public int getLineTop(int line) {
        return this.mInts.getValue(line, 1);
    }

    public int getLineDescent(int line) {
        return this.mInts.getValue(line, 2);
    }

    public int getLineStart(int line) {
        return this.mInts.getValue(line, 0) & START_MASK;
    }

    public boolean getLineContainsTab(int line) {
        return (this.mInts.getValue(line, 0) & 536870912) != 0;
    }

    public int getParagraphDirection(int line) {
        return this.mInts.getValue(line, 0) >> 30;
    }

    public final Directions getLineDirections(int line) {
        return (Directions) this.mObjects.getValue(line, 0);
    }

    public int getTopPadding() {
        return this.mTopPadding;
    }

    public int getBottomPadding() {
        return this.mBottomPadding;
    }

    public int getHyphen(int line) {
        return this.mInts.getValue(line, 3) & 255;
    }

    private boolean getContentMayProtrudeFromTopOrBottom(int line) {
        return (this.mInts.getValue(line, 3) & 256) != 0;
    }

    public int getEllipsizedWidth() {
        return this.mEllipsizedWidth;
    }

    public int getEllipsisStart(int line) {
        if (this.mEllipsizeAt == null) {
            return 0;
        }
        return this.mInts.getValue(line, 4);
    }

    public int getEllipsisCount(int line) {
        if (this.mEllipsizeAt == null) {
            return 0;
        }
        return this.mInts.getValue(line, 5);
    }
}
