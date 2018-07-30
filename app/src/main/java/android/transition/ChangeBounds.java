package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.RectEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.BrowserContract.Bookmarks;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;

public class ChangeBounds extends Transition {
    private static final Property<View, PointF> BOTTOM_RIGHT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "bottomRight") {
        public void set(View view, PointF bottomRight) {
            view.setLeftTopRightBottom(view.getLeft(), view.getTop(), Math.round(bottomRight.x), Math.round(bottomRight.y));
        }

        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<ViewBounds, PointF> BOTTOM_RIGHT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "bottomRight") {
        public void set(ViewBounds viewBounds, PointF bottomRight) {
            viewBounds.setBottomRight(bottomRight);
        }

        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static final Property<Drawable, PointF> DRAWABLE_ORIGIN_PROPERTY = new Property<Drawable, PointF>(PointF.class, "boundsOrigin") {
        private Rect mBounds = new Rect();

        public void set(Drawable object, PointF value) {
            object.copyBounds(this.mBounds);
            this.mBounds.offsetTo(Math.round(value.x), Math.round(value.y));
            object.setBounds(this.mBounds);
        }

        public PointF get(Drawable object) {
            object.copyBounds(this.mBounds);
            return new PointF((float) this.mBounds.left, (float) this.mBounds.top);
        }
    };
    private static final String LOG_TAG = "ChangeBounds";
    private static final Property<View, PointF> POSITION_PROPERTY = new Property<View, PointF>(PointF.class, Bookmarks.POSITION) {
        public void set(View view, PointF topLeft) {
            int left = Math.round(topLeft.x);
            int top = Math.round(topLeft.y);
            view.setLeftTopRightBottom(left, top, left + view.getWidth(), top + view.getHeight());
        }

        public PointF get(View view) {
            return null;
        }
    };
    private static final String PROPNAME_BOUNDS = "android:changeBounds:bounds";
    private static final String PROPNAME_CLIP = "android:changeBounds:clip";
    private static final String PROPNAME_PARENT = "android:changeBounds:parent";
    private static final String PROPNAME_WINDOW_X = "android:changeBounds:windowX";
    private static final String PROPNAME_WINDOW_Y = "android:changeBounds:windowY";
    private static final Property<View, PointF> TOP_LEFT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "topLeft") {
        public void set(View view, PointF topLeft) {
            view.setLeftTopRightBottom(Math.round(topLeft.x), Math.round(topLeft.y), view.getRight(), view.getBottom());
        }

        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<ViewBounds, PointF> TOP_LEFT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "topLeft") {
        public void set(ViewBounds viewBounds, PointF topLeft) {
            viewBounds.setTopLeft(topLeft);
        }

        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static RectEvaluator sRectEvaluator = new RectEvaluator();
    private static final String[] sTransitionProperties = new String[]{PROPNAME_BOUNDS, PROPNAME_CLIP, PROPNAME_PARENT, PROPNAME_WINDOW_X, PROPNAME_WINDOW_Y};
    boolean mReparent;
    boolean mResizeClip;
    int[] tempLocation;

    /* renamed from: android.transition.ChangeBounds$10 */
    class AnonymousClass10 extends AnimatorListenerAdapter {
        final /* synthetic */ BitmapDrawable val$drawable;
        final /* synthetic */ ViewGroup val$sceneRoot;
        final /* synthetic */ float val$transitionAlpha;
        final /* synthetic */ View val$view;

        AnonymousClass10(ViewGroup viewGroup, BitmapDrawable bitmapDrawable, View view, float f) {
            this.val$sceneRoot = viewGroup;
            this.val$drawable = bitmapDrawable;
            this.val$view = view;
            this.val$transitionAlpha = f;
        }

        public void onAnimationEnd(Animator animation) {
            this.val$sceneRoot.getOverlay().remove(this.val$drawable);
            this.val$view.setTransitionAlpha(this.val$transitionAlpha);
        }
    }

    /* renamed from: android.transition.ChangeBounds$8 */
    class AnonymousClass8 extends AnimatorListenerAdapter {
        private boolean mIsCanceled;
        final /* synthetic */ int val$endBottom;
        final /* synthetic */ int val$endLeft;
        final /* synthetic */ int val$endRight;
        final /* synthetic */ int val$endTop;
        final /* synthetic */ Rect val$finalClip;
        final /* synthetic */ View val$view;

        AnonymousClass8(View view, Rect rect, int i, int i2, int i3, int i4) {
            this.val$view = view;
            this.val$finalClip = rect;
            this.val$endLeft = i;
            this.val$endTop = i2;
            this.val$endRight = i3;
            this.val$endBottom = i4;
        }

        public void onAnimationCancel(Animator animation) {
            this.mIsCanceled = true;
        }

        public void onAnimationEnd(Animator animation) {
            if (!this.mIsCanceled) {
                this.val$view.setClipBounds(this.val$finalClip);
                this.val$view.setLeftTopRightBottom(this.val$endLeft, this.val$endTop, this.val$endRight, this.val$endBottom);
            }
        }
    }

    private static class ViewBounds {
        private int mBottom;
        private int mBottomRightCalls;
        private int mLeft;
        private int mRight;
        private int mTop;
        private int mTopLeftCalls;
        private View mView;

        public ViewBounds(View view) {
            this.mView = view;
        }

        public void setTopLeft(PointF topLeft) {
            this.mLeft = Math.round(topLeft.x);
            this.mTop = Math.round(topLeft.y);
            this.mTopLeftCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        public void setBottomRight(PointF bottomRight) {
            this.mRight = Math.round(bottomRight.x);
            this.mBottom = Math.round(bottomRight.y);
            this.mBottomRightCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        private void setLeftTopRightBottom() {
            this.mView.setLeftTopRightBottom(this.mLeft, this.mTop, this.mRight, this.mBottom);
            this.mTopLeftCalls = 0;
            this.mBottomRightCalls = 0;
        }
    }

    public ChangeBounds() {
        this.tempLocation = new int[2];
        this.mResizeClip = false;
        this.mReparent = false;
    }

    public ChangeBounds(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.tempLocation = new int[2];
        this.mResizeClip = false;
        this.mReparent = false;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChangeBounds);
        boolean resizeClip = a.getBoolean(0, false);
        a.recycle();
        setResizeClip(resizeClip);
    }

    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    public void setResizeClip(boolean resizeClip) {
        this.mResizeClip = resizeClip;
    }

    public boolean getResizeClip() {
        return this.mResizeClip;
    }

    @Deprecated
    public void setReparent(boolean reparent) {
        this.mReparent = reparent;
    }

    private void captureValues(TransitionValues values) {
        View view = values.view;
        if (view.isLaidOut() || view.getWidth() != 0 || view.getHeight() != 0) {
            values.values.put(PROPNAME_BOUNDS, new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
            values.values.put(PROPNAME_PARENT, values.view.getParent());
            if (this.mReparent) {
                values.view.getLocationInWindow(this.tempLocation);
                values.values.put(PROPNAME_WINDOW_X, Integer.valueOf(this.tempLocation[0]));
                values.values.put(PROPNAME_WINDOW_Y, Integer.valueOf(this.tempLocation[1]));
            }
            if (this.mResizeClip) {
                values.values.put(PROPNAME_CLIP, view.getClipBounds());
            }
        }
    }

    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private boolean parentMatches(View startParent, View endParent) {
        if (!this.mReparent) {
            return true;
        }
        TransitionValues endValues = getMatchedTransitionValues(startParent, true);
        return endValues == null ? startParent == endParent : endParent == endValues.view;
    }

    public android.animation.Animator createAnimator(android.view.ViewGroup r61, android.transition.TransitionValues r62, android.transition.TransitionValues r63) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r45_2 'startClip' android.graphics.Rect) in PHI: PHI: (r45_3 'startClip' android.graphics.Rect) = (r45_1 'startClip' android.graphics.Rect), (r45_2 'startClip' android.graphics.Rect) binds: {(r45_1 'startClip' android.graphics.Rect)=B:59:0x0218, (r45_2 'startClip' android.graphics.Rect)=B:60:0x021a}
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
        r60 = this;
        if (r62 == 0) goto L_0x0004;
    L_0x0002:
        if (r63 != 0) goto L_0x0006;
    L_0x0004:
        r4 = 0;
        return r4;
    L_0x0006:
        r0 = r62;
        r0 = r0.values;
        r49 = r0;
        r0 = r63;
        r0 = r0.values;
        r30 = r0;
        r4 = "android:changeBounds:parent";
        r0 = r49;
        r48 = r0.get(r4);
        r48 = (android.view.ViewGroup) r48;
        r4 = "android:changeBounds:parent";
        r0 = r30;
        r29 = r0.get(r4);
        r29 = (android.view.ViewGroup) r29;
        if (r48 == 0) goto L_0x002c;
    L_0x002a:
        if (r29 != 0) goto L_0x002e;
    L_0x002c:
        r4 = 0;
        return r4;
    L_0x002e:
        r0 = r63;
        r6 = r0.view;
        r0 = r60;
        r1 = r48;
        r2 = r29;
        r4 = r0.parentMatches(r1, r2);
        if (r4 == 0) goto L_0x0271;
    L_0x003e:
        r0 = r62;
        r4 = r0.values;
        r5 = "android:changeBounds:bounds";
        r44 = r4.get(r5);
        r44 = (android.graphics.Rect) r44;
        r0 = r63;
        r4 = r0.values;
        r5 = "android:changeBounds:bounds";
        r26 = r4.get(r5);
        r26 = (android.graphics.Rect) r26;
        r0 = r44;
        r0 = r0.left;
        r47 = r0;
        r0 = r26;
        r8 = r0.left;
        r0 = r44;
        r0 = r0.top;
        r51 = r0;
        r0 = r26;
        r9 = r0.top;
        r0 = r44;
        r0 = r0.right;
        r50 = r0;
        r0 = r26;
        r10 = r0.right;
        r0 = r44;
        r0 = r0.bottom;
        r43 = r0;
        r0 = r26;
        r11 = r0.bottom;
        r52 = r50 - r47;
        r46 = r43 - r51;
        r31 = r10 - r8;
        r28 = r11 - r9;
        r0 = r62;
        r4 = r0.values;
        r5 = "android:changeBounds:clip";
        r45 = r4.get(r5);
        r45 = (android.graphics.Rect) r45;
        r0 = r63;
        r4 = r0.values;
        r5 = "android:changeBounds:clip";
        r7 = r4.get(r5);
        r7 = (android.graphics.Rect) r7;
        r38 = 0;
        if (r52 == 0) goto L_0x0132;
    L_0x00a6:
        if (r46 == 0) goto L_0x0132;
    L_0x00a8:
        r0 = r47;
        if (r0 != r8) goto L_0x00b0;
    L_0x00ac:
        r0 = r51;
        if (r0 == r9) goto L_0x00b2;
    L_0x00b0:
        r38 = 1;
    L_0x00b2:
        r0 = r50;
        if (r0 != r10) goto L_0x00ba;
    L_0x00b6:
        r0 = r43;
        if (r0 == r11) goto L_0x00bc;
    L_0x00ba:
        r38 = r38 + 1;
    L_0x00bc:
        if (r45 == 0) goto L_0x00c8;
    L_0x00be:
        r0 = r45;
        r4 = r0.equals(r7);
        r4 = r4 ^ 1;
        if (r4 != 0) goto L_0x00cc;
    L_0x00c8:
        if (r45 != 0) goto L_0x00ce;
    L_0x00ca:
        if (r7 == 0) goto L_0x00ce;
    L_0x00cc:
        r38 = r38 + 1;
    L_0x00ce:
        if (r38 <= 0) goto L_0x0367;
    L_0x00d0:
        r0 = r60;
        r4 = r0.mResizeClip;
        if (r4 != 0) goto L_0x01d8;
    L_0x00d6:
        r0 = r47;
        r1 = r51;
        r2 = r50;
        r3 = r43;
        r6.setLeftTopRightBottom(r0, r1, r2, r3);
        r4 = 2;
        r0 = r38;
        if (r0 != r4) goto L_0x019a;
    L_0x00e6:
        r0 = r52;
        r1 = r31;
        if (r0 != r1) goto L_0x0138;
    L_0x00ec:
        r0 = r46;
        r1 = r28;
        if (r0 != r1) goto L_0x0138;
    L_0x00f2:
        r4 = r60.getPathMotion();
        r0 = r47;
        r5 = (float) r0;
        r0 = r51;
        r12 = (float) r0;
        r13 = (float) r8;
        r14 = (float) r9;
        r56 = r4.getPath(r5, r12, r13, r14);
        r4 = POSITION_PROPERTY;
        r5 = 0;
        r0 = r56;
        r18 = android.animation.ObjectAnimator.ofObject(r6, r4, r5, r0);
    L_0x010b:
        r4 = r6.getParent();
        r4 = r4 instanceof android.view.ViewGroup;
        if (r4 == 0) goto L_0x0131;
    L_0x0113:
        r40 = r6.getParent();
        r40 = (android.view.ViewGroup) r40;
        r4 = 1;
        r0 = r40;
        r0.suppressLayout(r4);
        r57 = new android.transition.ChangeBounds$9;
        r0 = r57;
        r1 = r60;
        r2 = r40;
        r0.<init>(r2);
        r0 = r60;
        r1 = r57;
        r0.addListener(r1);
    L_0x0131:
        return r18;
    L_0x0132:
        if (r31 == 0) goto L_0x00bc;
    L_0x0134:
        if (r28 == 0) goto L_0x00bc;
    L_0x0136:
        goto L_0x00a8;
    L_0x0138:
        r58 = new android.transition.ChangeBounds$ViewBounds;
        r0 = r58;
        r0.<init>(r6);
        r4 = r60.getPathMotion();
        r0 = r47;
        r5 = (float) r0;
        r0 = r51;
        r12 = (float) r0;
        r13 = (float) r8;
        r14 = (float) r9;
        r56 = r4.getPath(r5, r12, r13, r14);
        r4 = TOP_LEFT_PROPERTY;
        r5 = 0;
        r0 = r58;
        r1 = r56;
        r55 = android.animation.ObjectAnimator.ofObject(r0, r4, r5, r1);
        r4 = r60.getPathMotion();
        r0 = r50;
        r5 = (float) r0;
        r0 = r43;
        r12 = (float) r0;
        r13 = (float) r10;
        r14 = (float) r11;
        r23 = r4.getPath(r5, r12, r13, r14);
        r4 = BOTTOM_RIGHT_PROPERTY;
        r5 = 0;
        r0 = r58;
        r1 = r23;
        r22 = android.animation.ObjectAnimator.ofObject(r0, r4, r5, r1);
        r42 = new android.animation.AnimatorSet;
        r42.<init>();
        r4 = 2;
        r4 = new android.animation.Animator[r4];
        r5 = 0;
        r4[r5] = r55;
        r5 = 1;
        r4[r5] = r22;
        r0 = r42;
        r0.playTogether(r4);
        r18 = r42;
        r4 = new android.transition.ChangeBounds$7;
        r0 = r60;
        r1 = r58;
        r4.<init>(r1);
        r0 = r42;
        r0.addListener(r4);
        goto L_0x010b;
    L_0x019a:
        r0 = r47;
        if (r0 != r8) goto L_0x01a2;
    L_0x019e:
        r0 = r51;
        if (r0 == r9) goto L_0x01bd;
    L_0x01a2:
        r4 = r60.getPathMotion();
        r0 = r47;
        r5 = (float) r0;
        r0 = r51;
        r12 = (float) r0;
        r13 = (float) r8;
        r14 = (float) r9;
        r56 = r4.getPath(r5, r12, r13, r14);
        r4 = TOP_LEFT_ONLY_PROPERTY;
        r5 = 0;
        r0 = r56;
        r18 = android.animation.ObjectAnimator.ofObject(r6, r4, r5, r0);
        goto L_0x010b;
    L_0x01bd:
        r4 = r60.getPathMotion();
        r0 = r50;
        r5 = (float) r0;
        r0 = r43;
        r12 = (float) r0;
        r13 = (float) r10;
        r14 = (float) r11;
        r21 = r4.getPath(r5, r12, r13, r14);
        r4 = BOTTOM_RIGHT_ONLY_PROPERTY;
        r5 = 0;
        r0 = r21;
        r18 = android.animation.ObjectAnimator.ofObject(r6, r4, r5, r0);
        goto L_0x010b;
    L_0x01d8:
        r0 = r52;
        r1 = r31;
        r37 = java.lang.Math.max(r0, r1);
        r0 = r46;
        r1 = r28;
        r36 = java.lang.Math.max(r0, r1);
        r4 = r47 + r37;
        r5 = r51 + r36;
        r0 = r47;
        r1 = r51;
        r6.setLeftTopRightBottom(r0, r1, r4, r5);
        r41 = 0;
        r0 = r47;
        if (r0 != r8) goto L_0x01fd;
    L_0x01f9:
        r0 = r51;
        if (r0 == r9) goto L_0x0216;
    L_0x01fd:
        r4 = r60.getPathMotion();
        r0 = r47;
        r5 = (float) r0;
        r0 = r51;
        r12 = (float) r0;
        r13 = (float) r8;
        r14 = (float) r9;
        r56 = r4.getPath(r5, r12, r13, r14);
        r4 = POSITION_PROPERTY;
        r5 = 0;
        r0 = r56;
        r41 = android.animation.ObjectAnimator.ofObject(r6, r4, r5, r0);
    L_0x0216:
        r34 = r7;
        if (r45 != 0) goto L_0x0227;
    L_0x021a:
        r45 = new android.graphics.Rect;
        r4 = 0;
        r5 = 0;
        r0 = r45;
        r1 = r52;
        r2 = r46;
        r0.<init>(r4, r5, r1, r2);
    L_0x0227:
        if (r7 != 0) goto L_0x0369;
    L_0x0229:
        r27 = new android.graphics.Rect;
        r4 = 0;
        r5 = 0;
        r0 = r27;
        r1 = r31;
        r2 = r28;
        r0.<init>(r4, r5, r1, r2);
    L_0x0236:
        r25 = 0;
        r0 = r45;
        r1 = r27;
        r4 = r0.equals(r1);
        if (r4 != 0) goto L_0x0265;
    L_0x0242:
        r0 = r45;
        r6.setClipBounds(r0);
        r4 = "clipBounds";
        r5 = sRectEvaluator;
        r12 = 2;
        r12 = new java.lang.Object[r12];
        r13 = 0;
        r12[r13] = r45;
        r13 = 1;
        r12[r13] = r27;
        r25 = android.animation.ObjectAnimator.ofObject(r6, r4, r5, r12);
        r4 = new android.transition.ChangeBounds$8;
        r5 = r60;
        r4.<init>(r6, r7, r8, r9, r10, r11);
        r0 = r25;
        r0.addListener(r4);
    L_0x0265:
        r0 = r41;
        r1 = r25;
        r18 = android.transition.TransitionUtils.mergeAnimators(r0, r1);
        r7 = r27;
        goto L_0x010b;
    L_0x0271:
        r0 = r60;
        r4 = r0.tempLocation;
        r0 = r61;
        r0.getLocationInWindow(r4);
        r0 = r62;
        r4 = r0.values;
        r5 = "android:changeBounds:windowX";
        r4 = r4.get(r5);
        r4 = (java.lang.Integer) r4;
        r4 = r4.intValue();
        r0 = r60;
        r5 = r0.tempLocation;
        r12 = 0;
        r5 = r5[r12];
        r53 = r4 - r5;
        r0 = r62;
        r4 = r0.values;
        r5 = "android:changeBounds:windowY";
        r4 = r4.get(r5);
        r4 = (java.lang.Integer) r4;
        r4 = r4.intValue();
        r0 = r60;
        r5 = r0.tempLocation;
        r12 = 1;
        r5 = r5[r12];
        r54 = r4 - r5;
        r0 = r63;
        r4 = r0.values;
        r5 = "android:changeBounds:windowX";
        r4 = r4.get(r5);
        r4 = (java.lang.Integer) r4;
        r4 = r4.intValue();
        r0 = r60;
        r5 = r0.tempLocation;
        r12 = 0;
        r5 = r5[r12];
        r32 = r4 - r5;
        r0 = r63;
        r4 = r0.values;
        r5 = "android:changeBounds:windowY";
        r4 = r4.get(r5);
        r4 = (java.lang.Integer) r4;
        r4 = r4.intValue();
        r0 = r60;
        r5 = r0.tempLocation;
        r12 = 1;
        r5 = r5[r12];
        r33 = r4 - r5;
        r0 = r53;
        r1 = r32;
        if (r0 != r1) goto L_0x02ee;
    L_0x02e8:
        r0 = r54;
        r1 = r33;
        if (r0 == r1) goto L_0x0367;
    L_0x02ee:
        r59 = r6.getWidth();
        r35 = r6.getHeight();
        r4 = android.graphics.Bitmap.Config.ARGB_8888;
        r0 = r59;
        r1 = r35;
        r20 = android.graphics.Bitmap.createBitmap(r0, r1, r4);
        r24 = new android.graphics.Canvas;
        r0 = r24;
        r1 = r20;
        r0.<init>(r1);
        r0 = r24;
        r6.draw(r0);
        r15 = new android.graphics.drawable.BitmapDrawable;
        r0 = r20;
        r15.<init>(r0);
        r4 = r53 + r59;
        r5 = r54 + r35;
        r0 = r53;
        r1 = r54;
        r15.setBounds(r0, r1, r4, r5);
        r17 = r6.getTransitionAlpha();
        r4 = 0;
        r6.setTransitionAlpha(r4);
        r4 = r61.getOverlay();
        r4.add(r15);
        r4 = r60.getPathMotion();
        r0 = r53;
        r5 = (float) r0;
        r0 = r54;
        r12 = (float) r0;
        r0 = r32;
        r13 = (float) r0;
        r0 = r33;
        r14 = (float) r0;
        r56 = r4.getPath(r5, r12, r13, r14);
        r4 = DRAWABLE_ORIGIN_PROPERTY;
        r5 = 0;
        r0 = r56;
        r39 = android.animation.PropertyValuesHolder.ofObject(r4, r5, r0);
        r4 = 1;
        r4 = new android.animation.PropertyValuesHolder[r4];
        r5 = 0;
        r4[r5] = r39;
        r19 = android.animation.ObjectAnimator.ofPropertyValuesHolder(r15, r4);
        r12 = new android.transition.ChangeBounds$10;
        r13 = r60;
        r14 = r61;
        r16 = r6;
        r12.<init>(r14, r15, r16, r17);
        r0 = r19;
        r0.addListener(r12);
        return r19;
    L_0x0367:
        r4 = 0;
        return r4;
    L_0x0369:
        r27 = r7;
        goto L_0x0236;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.transition.ChangeBounds.createAnimator(android.view.ViewGroup, android.transition.TransitionValues, android.transition.TransitionValues):android.animation.Animator");
    }
}
