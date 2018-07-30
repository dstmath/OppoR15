package com.color.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OppoBezierInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import com.android.internal.widget.ColorViewExplorerByTouchHelper;
import com.android.internal.widget.ColorViewExplorerByTouchHelper.ColorViewTalkBalkInteraction;
import com.color.util.ColorAccessibilityUtil;
import com.color.util.ColorChangeTextUtil;
import com.color.util.ColorContextUtil;
import oppo.R;

public class ColorGlobalActionView extends View {
    private static final Interpolator AUTODOWNINTER = new PathInterpolator(0.507f, 0.04f, 0.889f, 0.78f);
    private static final int AUTODOWNREBOUNCE = 150;
    private static final Interpolator AUTODOWNREBOUNCEINTER = new OppoBezierInterpolator(-0.10000000149011612d, 1.475000023841858d, 0.6740000247955322d, 0.8700000047683716d, false);
    private static final int AUTODOWNTIME = 475;
    private static final int AUTODOWN_FLAG = 1;
    private static final int AUTODOWN_REBOUNCE_FLAG = 2;
    private static final float BG_ALPHA = 0.9f;
    private static final int CANCEL_EXIT_FLAG = 3;
    private static final float CRITICAL_NUMBER = 0.8f;
    private static final float DEGREE_360 = 360.0f;
    private static final float DEGREE_90 = 90.0f;
    private static final Interpolator EXITAINM = new LinearInterpolator();
    private static final int EXITTIME = 250;
    private static final int LARGEARGANGLE = 13;
    private static final float LARGESTARTANGLE = 193.0f;
    private static final float LARGESWEEPANGLE = 154.0f;
    private static final int OPAQUE = 255;
    private static final Interpolator RESTOREPOSITION = new PathInterpolator(0.121f, 0.82f, 0.71f, 0.944f);
    private static final Interpolator RESTOREREBOUNCE = new DecelerateInterpolator();
    private static final int RESTORE_FIRST_REBOUCE = 5;
    private static final int RESTORE_INIT_FLAG = 4;
    private static final int RESTORE_SECOND_REBOUCE = 6;
    private static final Interpolator SHUTDOWNINTER = new PathInterpolator(0.121f, 0.82f, 0.71f, 0.944f);
    private static final int SHUTDOWNY = 200;
    private static final int SHUT_DOWN_UP_FLAG = 7;
    private static final int SMALLARGANGLE = 32;
    private static final int STATICARCTIME = 500;
    private static final int STATICBGTIME = 100;
    private static final int STATICLINETIME = 300;
    private static final int STATICSMALLARC = 350;
    private static final int STATIC_ARC_FLAG = 10;
    private static final int STATIC_BG_ALPHA = 8;
    private static final int STATIC_LINE_FLAG = 9;
    private static final String TAG = "ColorGlobalActionView";
    private static final int TEXTAPHA = 255;
    private String mAccessContent;
    private int mAutoDownReUp;
    private int mAutoRectHight;
    private int mAutonDownReDown;
    private float mBgAlpha;
    private ColorDrawable mBgColor;
    private float mCancelAutoEnd;
    private Drawable mCancelBg;
    private int mCancelBgAlpha;
    private int mCancelBgBottom;
    private float mCancelBgDimen;
    private int mCancelBgEnd;
    private int mCancelBgHeight;
    private int mCancelBgLeft;
    private int mCancelBgRight;
    private int mCancelBgStart;
    private int mCancelBgTop;
    private Drawable mCancelNormalBg;
    private TextPaint mCancelPaint;
    private Drawable mCancelPressBg;
    private String mCancelText;
    private int mCancelTextAlpha;
    private int mCancelTextColor;
    private float mCancelTextDimen;
    private int mCancelTextEnd;
    private int mCancelTextHeight;
    private int mCancelTextSize;
    private int mCancelTextStart;
    private int mCancelTextY;
    private float mCancleBgColorAlpha;
    private int mCircleX;
    private int mCircleY;
    private ColorViewTalkBalkInteraction mColorViewTalkBalkInteraction;
    private int mContentHeight;
    private int mContentWidth;
    private Drawable mDynamicBg;
    private Drawable mEmergencyBg;
    private int mEmergencyBgBottom;
    private int mEmergencyBgHeight;
    private int mEmergencyBgLeft;
    private int mEmergencyBgRight;
    private int mEmergencyBgTop;
    private Drawable mEmergencyNormalBg;
    private Drawable mEmergencyPressBg;
    private String mEmergencyText;
    private int mEmergencyTextY;
    private boolean mFirstBgAlpha;
    private boolean mFirstRebounceAnim;
    private ValueAnimator mFirstRebound;
    private boolean mIsClickCancelBg;
    private boolean mIsClickEmergencyBg;
    private boolean mIsExitAnim;
    private boolean mIsOrientationPortrait;
    private boolean mIsShutDown;
    private boolean mIsTouchShutBg;
    private int mLargeArcEndColor;
    private Paint mLargeArcPaint;
    private int mLargeArcRadius;
    private RectF mLargeArcRect;
    private int mLargeArcStartColor;
    private int mLineEndColor;
    private Paint mLinePaint;
    private final Path mLinePath;
    private int mLineStartColor;
    private OnCancelListener mOnCancelListener;
    private OnEmergencyListener mOnEmergencyListener;
    private int mPaintWidth;
    private int mRectBgEndColor;
    private int mRectBgStartColor;
    private ValueAnimator mRestoreYAnim;
    private ValueAnimator mSecondRebound;
    private float mShutDownBgAutoEnd;
    private float mShutDownBgDimen;
    private int mShutDownBgEnd;
    private int mShutDownBgStart;
    private int mShutDownHeight;
    private OnShutDownListener mShutDownLister;
    private TextPaint mShutDownPaint;
    private String mShutDownText;
    private int mShutDownTextColor;
    private float mShutDownTextDimen;
    private int mShutDownTextEnd;
    private int mShutDownTextHeight;
    private int mShutDownTextSize;
    private int mShutDownTextStart;
    private float mSmallArcAngle;
    private int mSmallArcEndColor;
    private Paint mSmallArcPaint;
    private int mSmallArcRadius;
    private RectF mSmallArcRect;
    private int mSmallArcStartColor;
    private float mSmallArcSweepAngle;
    private int mStartDrawableAlpha;
    private boolean mStartStaticAlpha;
    private int mStartTextAlpha;
    private boolean mStateRestore;
    private float mStaticArcAlpha;
    private Drawable mStaticBg;
    private float mStaticBgAlpha;
    private int mStaticBgBottom;
    private int mStaticBgLeft;
    private int mStaticBgRight;
    private int mStaticBgTop;
    private float mStaticLineAlpha;
    private float mStaticLineStartY;
    private float mStaticMoveY;
    private float mTouchDownY;
    private ColorViewExplorerByTouchHelper mTouchHelper;
    private float mTouchMoveY;
    private float mTouchUpY;
    private Paint mViewRectBgPaint;

    private class AnimUpdateListener implements AnimatorUpdateListener {
        private int mFlag = 0;

        public AnimUpdateListener(int flag) {
            this.mFlag = flag;
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            float value = ((Float) animation.getAnimatedValue()).floatValue();
            if (this.mFlag == 1) {
                ColorGlobalActionView.this.mShutDownTextDimen = ((float) ColorGlobalActionView.this.mShutDownTextStart) + (((float) (ColorGlobalActionView.this.mShutDownTextEnd - ColorGlobalActionView.this.mShutDownTextStart)) * value);
                ColorGlobalActionView.this.mShutDownBgDimen = ((float) ColorGlobalActionView.this.mShutDownBgStart) + (((float) (ColorGlobalActionView.this.mShutDownBgEnd - ColorGlobalActionView.this.mShutDownBgStart)) * value);
                ColorGlobalActionView.this.mCancelBgDimen = ((1.0f - value) * ((float) (ColorGlobalActionView.this.mCancelBgHeight + ColorGlobalActionView.this.mCancelBgStart))) + (((float) ((ColorGlobalActionView.this.mCancelBgEnd + ColorGlobalActionView.this.mCancelTextHeight) + ColorGlobalActionView.this.mCancelBgHeight)) * value);
                ColorGlobalActionView.this.mCancelTextDimen = ((1.0f - value) * ((float) ColorGlobalActionView.this.mCancelTextStart)) + (((float) (ColorGlobalActionView.this.mCancelTextEnd + ColorGlobalActionView.this.mCancelTextHeight)) * value);
                ColorGlobalActionView.this.mStartDrawableAlpha = (int) (((1.0f - value) * 0.0f) + (value * 255.0f));
                ColorGlobalActionView.this.mStartTextAlpha = (int) (((1.0f - value) * 0.0f) + (value * 255.0f));
            }
            if (this.mFlag == 2) {
                ColorGlobalActionView.this.mShutDownBgDimen = ((1.0f - value) * (ColorGlobalActionView.this.mShutDownBgAutoEnd - ((float) ColorGlobalActionView.this.mAutoDownReUp))) + ((ColorGlobalActionView.this.mShutDownBgAutoEnd - ((float) ColorGlobalActionView.this.mAutonDownReDown)) * value);
                ColorGlobalActionView.this.mCancelBgDimen = ((1.0f - value) * (ColorGlobalActionView.this.mCancelAutoEnd - ((float) ColorGlobalActionView.this.mAutoDownReUp))) + ((ColorGlobalActionView.this.mCancelAutoEnd - ((float) ColorGlobalActionView.this.mAutonDownReDown)) * value);
            }
            if (this.mFlag == 3) {
                ColorGlobalActionView.this.mCancelBgAlpha = (int) (((1.0f - value) * 255.0f) + (0.0f * value));
                ColorGlobalActionView.this.mCancelTextAlpha = (int) (((1.0f - value) * 255.0f) + (0.0f * value));
                ColorGlobalActionView.this.mCancleBgColorAlpha = (float) ((int) ((((1.0f - value) * 255.0f) * ColorGlobalActionView.BG_ALPHA) + (0.0f * value)));
            }
            if (this.mFlag == 4) {
                ColorGlobalActionView.this.mTouchMoveY = ((1.0f - value) * ColorGlobalActionView.this.mTouchUpY) + (ColorGlobalActionView.this.mTouchDownY * value);
                ColorGlobalActionView.this.mStaticMoveY = ((1.0f - value) * ColorGlobalActionView.this.mTouchUpY) + ((ColorGlobalActionView.this.mTouchDownY - ((float) ColorGlobalActionView.this.mAutonDownReDown)) * value);
            }
            if (this.mFlag == 5) {
                ColorGlobalActionView.this.mStaticMoveY = ((1.0f - value) * (ColorGlobalActionView.this.mTouchDownY - ((float) ColorGlobalActionView.this.mAutonDownReDown))) + (((ColorGlobalActionView.this.mTouchDownY - ((float) ColorGlobalActionView.this.mAutonDownReDown)) + ((float) ColorGlobalActionView.this.mAutoDownReUp)) * value);
            }
            if (this.mFlag == 6) {
                ColorGlobalActionView.this.mStaticMoveY = ((1.0f - value) * ((ColorGlobalActionView.this.mTouchDownY - ((float) ColorGlobalActionView.this.mAutonDownReDown)) + ((float) ColorGlobalActionView.this.mAutoDownReUp))) + (ColorGlobalActionView.this.mTouchDownY * value);
            }
            if (this.mFlag == 7) {
                ColorGlobalActionView.this.mStaticMoveY = ((1.0f - value) * ColorGlobalActionView.this.mTouchUpY) + (((((float) ColorGlobalActionView.this.mShutDownHeight) * ColorGlobalActionView.CRITICAL_NUMBER) + ColorGlobalActionView.this.mTouchDownY) * value);
            }
            if (this.mFlag == 8) {
                ColorGlobalActionView.this.mStaticBgAlpha = (float) ((int) (((1.0f - value) * 255.0f) + (0.0f * value)));
            }
            if (this.mFlag == 9) {
                ColorGlobalActionView.this.mStaticLineAlpha = (float) ((int) (((1.0f - value) * 255.0f) + (0.0f * value)));
                ColorGlobalActionView.this.mStaticLineStartY = (float) ((int) (((1.0f - value) * ((float) ColorGlobalActionView.this.mSmallArcRadius)) + (0.0f * value)));
            }
            if (this.mFlag == ColorGlobalActionView.STATIC_ARC_FLAG) {
                ColorGlobalActionView.this.mStaticArcAlpha = (float) ((int) (((1.0f - value) * 255.0f) + (0.0f * value)));
                ColorGlobalActionView.this.mSmallArcAngle = (ColorGlobalActionView.DEGREE_360 * value) + ((1.0f - value) * 0.0f);
                ColorGlobalActionView.this.mSmallArcSweepAngle = (296.0f * value) + ((1.0f - value) * 0.0f);
            }
            ColorGlobalActionView.this.invalidate();
        }
    }

    public interface OnCancelListener {
        void onCancel();
    }

    public interface OnEmergencyListener {
        void onEmergency();
    }

    public interface OnShutDownListener {
        void onShutDown();
    }

    public ColorGlobalActionView(Context context) {
        this(context, null);
    }

    public ColorGlobalActionView(Context context, AttributeSet attrs) {
        this(context, attrs, 201393291);
    }

    public ColorGlobalActionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLinePath = new Path();
        this.mStaticBg = null;
        this.mCancelBg = null;
        this.mCancelNormalBg = null;
        this.mCancelPressBg = null;
        this.mDynamicBg = null;
        this.mEmergencyTextY = 0;
        this.mIsClickEmergencyBg = false;
        this.mEmergencyBgHeight = 0;
        this.mEmergencyBgLeft = 0;
        this.mEmergencyBgRight = 0;
        this.mEmergencyBgTop = 0;
        this.mEmergencyBgBottom = 0;
        this.mShutDownTextColor = 0;
        this.mCancelTextColor = 0;
        this.mBgColor = null;
        this.mShutDownTextSize = 0;
        this.mCancelTextSize = 0;
        this.mShutDownText = null;
        this.mCancelText = null;
        this.mContentWidth = 0;
        this.mContentHeight = 0;
        this.mShutDownHeight = 0;
        this.mShutDownPaint = null;
        this.mCancelPaint = null;
        this.mSmallArcPaint = null;
        this.mViewRectBgPaint = null;
        this.mLargeArcPaint = null;
        this.mSmallArcStartColor = 0;
        this.mSmallArcEndColor = 0;
        this.mLargeArcStartColor = 0;
        this.mLargeArcEndColor = 0;
        this.mLinePaint = null;
        this.mLineStartColor = 0;
        this.mLineEndColor = 0;
        this.mPaintWidth = 0;
        this.mCircleY = 0;
        this.mCancelTextHeight = 0;
        this.mSmallArcRadius = 0;
        this.mShutDownTextStart = 0;
        this.mShutDownTextEnd = 0;
        this.mShutDownTextDimen = 0.0f;
        this.mShutDownBgStart = 0;
        this.mShutDownBgEnd = 0;
        this.mShutDownBgDimen = 0.0f;
        this.mCancelTextStart = 0;
        this.mCancelTextEnd = 0;
        this.mCancelTextDimen = 0.0f;
        this.mCancelBgStart = 0;
        this.mCancelBgEnd = 0;
        this.mCancelBgDimen = 0.0f;
        this.mCancelTextY = 0;
        this.mAutoRectHight = 0;
        this.mCancelBgLeft = 0;
        this.mCancelBgRight = 0;
        this.mAutoDownReUp = 0;
        this.mAutonDownReDown = 0;
        this.mCancelBgTop = 0;
        this.mCancelBgBottom = 0;
        this.mSmallArcRect = new RectF();
        this.mLargeArcRect = new RectF();
        this.mCancelBgHeight = 0;
        this.mFirstRebounceAnim = false;
        this.mIsClickCancelBg = false;
        this.mIsTouchShutBg = false;
        this.mIsExitAnim = false;
        this.mShutDownBgAutoEnd = 0.0f;
        this.mCancelAutoEnd = 0.0f;
        this.mCancelTextAlpha = 0;
        this.mCancelBgAlpha = 0;
        this.mStaticBgLeft = 0;
        this.mStaticBgRight = 0;
        this.mStaticBgBottom = 0;
        this.mStaticBgTop = 0;
        this.mShutDownTextHeight = 0;
        this.mLargeArcRadius = 0;
        this.mTouchDownY = 0.0f;
        this.mTouchMoveY = 0.0f;
        this.mStaticMoveY = 0.0f;
        this.mTouchUpY = 0.0f;
        this.mStateRestore = false;
        this.mFirstBgAlpha = true;
        this.mIsShutDown = false;
        this.mStartStaticAlpha = false;
        this.mStaticBgAlpha = 0.0f;
        this.mStaticLineAlpha = 0.0f;
        this.mStaticArcAlpha = 0.0f;
        this.mBgAlpha = 0.0f;
        this.mStaticLineStartY = 0.0f;
        this.mSmallArcAngle = 0.0f;
        this.mSmallArcSweepAngle = 0.0f;
        this.mRectBgStartColor = 0;
        this.mRectBgEndColor = 0;
        this.mStartDrawableAlpha = 0;
        this.mStartTextAlpha = 0;
        this.mIsOrientationPortrait = true;
        this.mCircleX = 0;
        this.mSecondRebound = null;
        this.mFirstRebound = null;
        this.mRestoreYAnim = null;
        this.mCancleBgColorAlpha = 229.5f;
        this.mOnCancelListener = null;
        this.mShutDownLister = null;
        this.mOnEmergencyListener = null;
        this.mAccessContent = null;
        this.mColorViewTalkBalkInteraction = new ColorViewTalkBalkInteraction() {
            public void getItemBounds(int position, Rect rect) {
                if (position == 0) {
                    rect.set(ColorGlobalActionView.this.mStaticBgLeft, ColorGlobalActionView.this.mStaticBgTop, ColorGlobalActionView.this.mStaticBgRight, ColorGlobalActionView.this.mStaticBgBottom);
                } else if (position == 1) {
                    rect.set(ColorGlobalActionView.this.mCancelBgLeft, ColorGlobalActionView.this.mCancelBgTop, ColorGlobalActionView.this.mCancelBgRight, ColorGlobalActionView.this.mCancelBgBottom);
                }
            }

            public void performAction(int virtualViewId, int actiontype, boolean resolvePara) {
                if (virtualViewId == 0) {
                    if (ColorGlobalActionView.this.mShutDownLister != null) {
                        ColorGlobalActionView.this.mShutDownLister.onShutDown();
                    }
                } else if (virtualViewId == 1) {
                    ColorGlobalActionView.this.setQuitView();
                } else if (ColorGlobalActionView.this.mShutDownLister != null) {
                    ColorGlobalActionView.this.mShutDownLister.onShutDown();
                }
            }

            public int getCurrentPosition() {
                return -2;
            }

            public int getItemCounts() {
                return 2;
            }

            public int getVirtualViewAt(float x, float y) {
                if (x >= 0.0f && x <= ((float) ColorGlobalActionView.this.mContentWidth) && y >= 0.0f && y <= ((float) (ColorGlobalActionView.this.mStaticBgBottom * 2)) && ColorGlobalActionView.this.mFirstRebounceAnim) {
                    return 0;
                }
                if (x < ((float) ColorGlobalActionView.this.mCancelBgLeft) || x > ((float) ColorGlobalActionView.this.mCancelBgRight) || y < ((float) ColorGlobalActionView.this.mCancelBgTop) || y > ((float) ColorGlobalActionView.this.mCancelBgBottom) || !ColorGlobalActionView.this.mFirstRebounceAnim) {
                    return -1;
                }
                return 1;
            }

            public CharSequence getItemDescription(int virtualViewId) {
                if (virtualViewId == 0) {
                    return ColorGlobalActionView.this.mAccessContent;
                }
                if (virtualViewId == 1) {
                    return ColorGlobalActionView.this.mCancelText;
                }
                return getClass().getSimpleName();
            }

            public CharSequence getClassName() {
                return Button.class.getName();
            }

            public int getDisablePosition() {
                return -2;
            }
        };
        this.mIsOrientationPortrait = isOrientationPortrait(context);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorGlobalActionView, defStyle, 0);
        this.mStaticBg = a.getDrawable(0);
        this.mCancelNormalBg = a.getDrawable(1);
        this.mCancelPressBg = a.getDrawable(5);
        this.mCancelTextSize = a.getDimensionPixelSize(2, 0);
        this.mShutDownText = a.getString(4);
        this.mCancelText = a.getString(3);
        this.mEmergencyNormalBg = a.getDrawable(6);
        this.mEmergencyPressBg = a.getDrawable(7);
        this.mEmergencyText = a.getString(8);
        a.recycle();
        if (this.mIsOrientationPortrait) {
            this.mDynamicBg = context.getDrawable(201852178);
        } else {
            this.mDynamicBg = context.getDrawable(201852186);
        }
        this.mShutDownTextColor = context.getColor(201720899);
        this.mCancelTextColor = context.getColor(201720900);
        this.mBgColor = new ColorDrawable(ColorContextUtil.getAttrColor(context, 201392717));
        this.mRectBgStartColor = ColorContextUtil.getAttrColor(context, 201392717);
        this.mRectBgEndColor = context.getColor(201720890);
        this.mShutDownTextSize = getResources().getDimensionPixelSize(201655524);
        float fontScale = getResources().getConfiguration().fontScale;
        this.mShutDownTextSize = (int) ColorChangeTextUtil.getSuitableFontSize((float) this.mShutDownTextSize, fontScale, 2);
        this.mCancelTextSize = (int) ColorChangeTextUtil.getSuitableFontSize((float) this.mCancelTextSize, fontScale, 2);
        this.mSmallArcStartColor = context.getColor(201720891);
        this.mSmallArcEndColor = context.getColor(201720892);
        this.mLineStartColor = context.getColor(201720893);
        this.mLineEndColor = context.getColor(201720894);
        this.mLargeArcStartColor = context.getColor(201720895);
        this.mLargeArcEndColor = context.getColor(201720896);
        if (this.mCancelNormalBg != null) {
            this.mCancelBgHeight = this.mCancelNormalBg.getIntrinsicHeight();
            this.mCancelBg = this.mCancelNormalBg;
            this.mCancelBg.setAlpha(255);
        }
        if (this.mEmergencyNormalBg != null) {
            this.mEmergencyBgHeight = this.mEmergencyNormalBg.getIntrinsicHeight();
            this.mEmergencyBg = this.mEmergencyNormalBg;
            this.mEmergencyBg.setAlpha(255);
        }
        animationData();
        initPaint();
        this.mTouchHelper = new ColorViewExplorerByTouchHelper(this);
        this.mTouchHelper.setColorViewTalkBalkInteraction(this.mColorViewTalkBalkInteraction);
        setAccessibilityDelegate(this.mTouchHelper);
        setImportantForAccessibility(1);
        this.mAccessContent = context.getString(201590175);
    }

    private boolean isOrientationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == 1;
    }

    private void initPaint() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService("window")).getDefaultDisplay().getRealMetrics(displayMetrics);
        this.mContentWidth = displayMetrics.widthPixels;
        this.mContentHeight = displayMetrics.heightPixels;
        this.mShutDownPaint = new TextPaint(1);
        this.mShutDownPaint.setAntiAlias(true);
        this.mShutDownPaint.setTextSize((float) this.mShutDownTextSize);
        this.mShutDownPaint.setColor(this.mShutDownTextColor);
        this.mCancelPaint = new TextPaint(1);
        this.mCancelPaint.setAntiAlias(true);
        this.mCancelPaint.setTextSize((float) this.mCancelTextSize);
        this.mCancelPaint.setColor(this.mCancelTextColor);
        this.mSmallArcPaint = new Paint();
        this.mSmallArcPaint.setStyle(Style.STROKE);
        this.mSmallArcPaint.setStrokeWidth((float) this.mPaintWidth);
        this.mSmallArcPaint.setAntiAlias(true);
        this.mSmallArcPaint.setDither(true);
        this.mSmallArcPaint.setStrokeJoin(Join.ROUND);
        this.mSmallArcPaint.setStrokeCap(Cap.ROUND);
        this.mLargeArcPaint = new Paint();
        this.mLargeArcPaint.setStyle(Style.STROKE);
        this.mLargeArcPaint.setStrokeWidth((float) this.mPaintWidth);
        this.mLargeArcPaint.setAntiAlias(true);
        this.mLargeArcPaint.setDither(true);
        this.mLargeArcPaint.setStrokeJoin(Join.ROUND);
        this.mLargeArcPaint.setStrokeCap(Cap.ROUND);
        this.mLinePaint = new Paint();
        this.mLinePaint.setStyle(Style.STROKE);
        this.mLinePaint.setStrokeWidth((float) this.mPaintWidth);
        this.mLinePaint.setAntiAlias(true);
        this.mLinePaint.setDither(true);
        this.mLinePaint.setStrokeJoin(Join.ROUND);
        this.mLinePaint.setStrokeCap(Cap.ROUND);
        this.mLinePaint.setColor(this.mLineStartColor);
        this.mStaticBg.setAlpha(255);
    }

    private void animationData() {
        this.mShutDownTextStart = getResources().getDimensionPixelSize(201655531);
        this.mShutDownTextEnd = getResources().getDimensionPixelSize(201655532);
        this.mShutDownBgStart = getResources().getDimensionPixelSize(201655533);
        this.mShutDownBgEnd = getResources().getDimensionPixelSize(201655534);
        this.mCancelTextStart = getResources().getDimensionPixelSize(201655562);
        this.mCancelTextEnd = getResources().getDimensionPixelSize(201655535);
        this.mCancelBgStart = getResources().getDimensionPixelSize(201655536);
        this.mCancelBgEnd = getResources().getDimensionPixelSize(201655537);
        this.mPaintWidth = getResources().getDimensionPixelSize(201655525);
        this.mSmallArcRadius = getResources().getDimensionPixelSize(201655526);
        this.mAutoDownReUp = getResources().getDimensionPixelSize(201655527);
        this.mAutonDownReDown = getResources().getDimensionPixelSize(201655528);
        this.mAutoRectHight = getResources().getDimensionPixelSize(201655529);
        this.mLargeArcRadius = getResources().getDimensionPixelSize(201655530);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(this.mContentWidth, this.mContentHeight);
    }

    protected void onDraw(android.graphics.Canvas r51) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r4_3 'lineShader' android.graphics.Shader) in PHI: PHI: (r4_2 'lineShader' android.graphics.Shader) = (r4_1 'lineShader' android.graphics.Shader), (r4_3 'lineShader' android.graphics.Shader) binds: {(r4_1 'lineShader' android.graphics.Shader)=B:69:0x0220, (r4_3 'lineShader' android.graphics.Shader)=B:146:0x05a9}
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
        r44 = 0;
        r41 = 0;
        r40 = 0;
        r20 = 0;
        r23 = 0;
        r36 = 0;
        r37 = 0;
        r38 = 0;
        r35 = 0;
        r26 = 0;
        r27 = 0;
        r28 = 0;
        r25 = 0;
        r34 = 0;
        r31 = 0;
        r29 = 0;
        r30 = 0;
        r32 = 0;
        r6 = 0;
        r39 = 0;
        r15 = 0;
        r16 = 0;
        r0 = r50;
        r0 = r0.mStaticBg;
        r22 = r0;
        r19 = 0;
        r4 = 0;
        r5 = "ColorGlobalActionView";
        r7 = "ColorGlobalActionView onDraw";
        android.util.Log.i(r5, r7);
        r0 = r50;
        r5 = r0.mTouchMoveY;
        r0 = r50;
        r7 = r0.mTouchDownY;
        r5 = (r5 > r7 ? 1 : (r5 == r7 ? 0 : -1));
        if (r5 > 0) goto L_0x0050;
    L_0x0048:
        r0 = r50;
        r5 = r0.mTouchDownY;
        r0 = r50;
        r0.mTouchMoveY = r5;
    L_0x0050:
        r0 = r50;
        r5 = r0.mShutDownHeight;
        if (r5 != 0) goto L_0x04a5;
    L_0x0056:
        r5 = 1130725376; // 0x43658000 float:229.5 double:5.58652563E-315;
        r0 = r50;
        r0.mBgAlpha = r5;
    L_0x005d:
        r0 = r50;
        r5 = r0.mIsExitAnim;
        if (r5 == 0) goto L_0x04df;
    L_0x0063:
        r0 = r50;
        r5 = r0.mBgColor;
        r0 = r50;
        r7 = r0.mCancleBgColorAlpha;
        r7 = (int) r7;
        r5.setAlpha(r7);
    L_0x006f:
        r0 = r50;
        r5 = r0.mBgColor;
        r0 = r50;
        r0.setBackgroundDrawable(r5);
        r0 = r50;
        r5 = r0.mShutDownText;
        if (r5 == 0) goto L_0x00ff;
    L_0x007e:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        r5 = r5 ^ 1;
        if (r5 == 0) goto L_0x00ff;
    L_0x0086:
        r0 = r50;
        r5 = r0.mShutDownPaint;
        r24 = r5.getFontMetricsInt();
        r0 = r50;
        r5 = r0.mShutDownPaint;
        r0 = r50;
        r7 = r0.mShutDownText;
        r5 = r5.measureText(r7);
        r0 = (int) r5;
        r33 = r0;
        r0 = r24;
        r5 = r0.bottom;
        r0 = r24;
        r7 = r0.top;
        r5 = r5 - r7;
        r0 = r50;
        r0.mShutDownTextHeight = r5;
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x04ed;
    L_0x00b0:
        r0 = r50;
        r5 = r0.mContentWidth;
        r5 = r5 - r33;
        r5 = r5 / 2;
        r0 = (float) r5;
        r44 = r0;
        r0 = r50;
        r5 = r0.mShutDownTextDimen;
        r0 = r24;
        r7 = r0.top;
        r7 = (float) r7;
        r34 = r5 - r7;
    L_0x00c6:
        r0 = r50;
        r5 = r0.mIsExitAnim;
        if (r5 == 0) goto L_0x00d7;
    L_0x00cc:
        r0 = r50;
        r5 = r0.mShutDownPaint;
        r0 = r50;
        r7 = r0.mCancelTextAlpha;
        r5.setAlpha(r7);
    L_0x00d7:
        r0 = r50;
        r5 = r0.mFirstBgAlpha;
        if (r5 == 0) goto L_0x00e8;
    L_0x00dd:
        r0 = r50;
        r5 = r0.mShutDownPaint;
        r0 = r50;
        r7 = r0.mStartTextAlpha;
        r5.setAlpha(r7);
    L_0x00e8:
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x0506;
    L_0x00ee:
        r0 = r50;
        r5 = r0.mShutDownText;
        r0 = r50;
        r7 = r0.mShutDownPaint;
        r0 = r51;
        r1 = r44;
        r2 = r34;
        r0.drawText(r5, r1, r2, r7);
    L_0x00ff:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x052a;
    L_0x0105:
        r0 = r50;
        r5 = r0.mStateRestore;
        r5 = r5 ^ 1;
        if (r5 == 0) goto L_0x052a;
    L_0x010d:
        r0 = r50;
        r5 = r0.mIsShutDown;
        r5 = r5 ^ 1;
        if (r5 == 0) goto L_0x052a;
    L_0x0115:
        r0 = r50;
        r0 = r0.mDynamicBg;
        r22 = r0;
    L_0x011b:
        if (r22 == 0) goto L_0x01d0;
    L_0x011d:
        r0 = r50;
        r5 = r0.mStaticBg;
        r41 = r5.getIntrinsicWidth();
        r0 = r50;
        r5 = r0.mStaticBg;
        r40 = r5.getIntrinsicHeight();
        r0 = r50;
        r5 = r0.mStateRestore;
        if (r5 != 0) goto L_0x0147;
    L_0x0133:
        r0 = r50;
        r5 = r0.mStaticMoveY;
        r0 = r50;
        r7 = r0.mTouchDownY;
        r5 = (r5 > r7 ? 1 : (r5 == r7 ? 0 : -1));
        if (r5 > 0) goto L_0x0147;
    L_0x013f:
        r0 = r50;
        r5 = r0.mTouchDownY;
        r0 = r50;
        r0.mStaticMoveY = r5;
    L_0x0147:
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x054a;
    L_0x014d:
        r0 = r50;
        r5 = r0.mContentWidth;
        r5 = r5 - r41;
        r5 = r5 / 2;
        r0 = r50;
        r0.mStaticBgLeft = r5;
        r0 = r50;
        r5 = r0.mShutDownBgDimen;
        r0 = r50;
        r7 = r0.mStaticMoveY;
        r0 = r50;
        r9 = r0.mTouchDownY;
        r7 = r7 - r9;
        r5 = r5 + r7;
        r0 = r50;
        r7 = r0.mShutDownTextHeight;
        r7 = (float) r7;
        r5 = r5 + r7;
        r5 = (int) r5;
        r0 = r50;
        r0.mStaticBgTop = r5;
    L_0x0172:
        r0 = r50;
        r5 = r0.mStaticBgLeft;
        r5 = r5 + r41;
        r0 = r50;
        r0.mStaticBgRight = r5;
        r0 = r50;
        r5 = r0.mStaticBgTop;
        r5 = r5 + r40;
        r0 = r50;
        r0.mStaticBgBottom = r5;
        r0 = r50;
        r5 = r0.mStaticBgLeft;
        r0 = r50;
        r7 = r0.mStaticBgTop;
        r0 = r50;
        r9 = r0.mStaticBgRight;
        r0 = r50;
        r10 = r0.mStaticBgBottom;
        r0 = r22;
        r0.setBounds(r5, r7, r9, r10);
        r0 = r50;
        r5 = r0.mFirstBgAlpha;
        if (r5 == 0) goto L_0x01aa;
    L_0x01a1:
        r0 = r50;
        r5 = r0.mStartDrawableAlpha;
        r0 = r22;
        r0.setAlpha(r5);
    L_0x01aa:
        r0 = r50;
        r5 = r0.mStartStaticAlpha;
        if (r5 == 0) goto L_0x01ba;
    L_0x01b0:
        r0 = r50;
        r5 = r0.mStaticBgAlpha;
        r5 = (int) r5;
        r0 = r22;
        r0.setAlpha(r5);
    L_0x01ba:
        r0 = r50;
        r5 = r0.mIsExitAnim;
        if (r5 == 0) goto L_0x01c9;
    L_0x01c0:
        r0 = r50;
        r5 = r0.mCancelBgAlpha;
        r0 = r22;
        r0.setAlpha(r5);
    L_0x01c9:
        r0 = r22;
        r1 = r51;
        r0.draw(r1);
    L_0x01d0:
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x0571;
    L_0x01d6:
        r0 = r50;
        r5 = r0.mContentWidth;
        r5 = r5 / 2;
        r0 = r50;
        r0.mCircleX = r5;
        r0 = r50;
        r5 = r0.mStaticBgBottom;
        r7 = r40 / 2;
        r5 = r5 - r7;
        r0 = r50;
        r0.mCircleY = r5;
    L_0x01eb:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x0209;
    L_0x01f1:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x01fd;
    L_0x01f7:
        r0 = r50;
        r5 = r0.mStateRestore;
        if (r5 != 0) goto L_0x0209;
    L_0x01fd:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x0588;
    L_0x0203:
        r0 = r50;
        r5 = r0.mIsShutDown;
        if (r5 == 0) goto L_0x0843;
    L_0x0209:
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x0598;
    L_0x020f:
        r0 = r50;
        r5 = r0.mStartStaticAlpha;
        if (r5 == 0) goto L_0x058c;
    L_0x0215:
        r0 = r50;
        r5 = r0.mCircleY;
        r5 = (float) r5;
        r0 = r50;
        r7 = r0.mStaticLineStartY;
        r6 = r5 - r7;
    L_0x0220:
        r0 = r50;
        r5 = r0.mLinePath;
        r5.rewind();
        r0 = r50;
        r5 = r0.mLinePath;
        r0 = r50;
        r7 = r0.mCircleX;
        r7 = (float) r7;
        r5.moveTo(r7, r6);
        r0 = r50;
        r5 = r0.mLinePath;
        r0 = r50;
        r7 = r0.mCircleX;
        r7 = (float) r7;
        r0 = r50;
        r9 = r0.mCircleY;
        r9 = (float) r9;
        r5.lineTo(r7, r9);
        r4 = new android.graphics.LinearGradient;
        r0 = r50;
        r5 = r0.mCircleX;
        r5 = (float) r5;
        r0 = r50;
        r7 = r0.mCircleX;
        r7 = (float) r7;
        r0 = r50;
        r9 = r0.mCircleY;
        r8 = (float) r9;
        r0 = r50;
        r9 = r0.mLineStartColor;
        r0 = r50;
        r10 = r0.mLineEndColor;
        r11 = android.graphics.Shader.TileMode.CLAMP;
        r4.<init>(r5, r6, r7, r8, r9, r10, r11);
        r8 = r39;
    L_0x0264:
        r0 = r50;
        r5 = r0.mStartStaticAlpha;
        if (r5 == 0) goto L_0x0276;
    L_0x026a:
        r0 = r50;
        r5 = r0.mLinePaint;
        r0 = r50;
        r7 = r0.mStaticLineAlpha;
        r7 = (int) r7;
        r5.setAlpha(r7);
    L_0x0276:
        r0 = r50;
        r5 = r0.mIsExitAnim;
        if (r5 == 0) goto L_0x0287;
    L_0x027c:
        r0 = r50;
        r5 = r0.mLinePaint;
        r0 = r50;
        r7 = r0.mCancelBgAlpha;
        r5.setAlpha(r7);
    L_0x0287:
        r0 = r50;
        r5 = r0.mFirstBgAlpha;
        if (r5 == 0) goto L_0x0298;
    L_0x028d:
        r0 = r50;
        r5 = r0.mLinePaint;
        r0 = r50;
        r7 = r0.mStartDrawableAlpha;
        r5.setAlpha(r7);
    L_0x0298:
        r0 = r50;
        r5 = r0.mLinePaint;
        r5.setShader(r4);
        r0 = r50;
        r5 = r0.mLinePath;
        r0 = r50;
        r7 = r0.mLinePaint;
        r0 = r51;
        r0.drawPath(r5, r7);
    L_0x02ac:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x05f9;
    L_0x02b2:
        r0 = r50;
        r5 = r0.mStateRestore;
        r5 = r5 ^ 1;
        if (r5 == 0) goto L_0x083d;
    L_0x02ba:
        r0 = r50;
        r5 = r0.mCircleX;
        r0 = r50;
        r7 = r0.mLargeArcRadius;
        r26 = r5 - r7;
        r0 = r50;
        r5 = r0.mCircleX;
        r0 = r50;
        r7 = r0.mLargeArcRadius;
        r27 = r5 + r7;
        r0 = r50;
        r5 = r0.mCircleY;
        r0 = r50;
        r7 = r0.mLargeArcRadius;
        r28 = r5 - r7;
        r0 = r50;
        r5 = r0.mCircleY;
        r0 = r50;
        r7 = r0.mLargeArcRadius;
        r25 = r5 + r7;
        r0 = r50;
        r5 = r0.mLargeArcRect;
        r0 = r26;
        r7 = (float) r0;
        r0 = r28;
        r9 = (float) r0;
        r0 = r27;
        r10 = (float) r0;
        r0 = r25;
        r13 = (float) r0;
        r5.set(r7, r9, r10, r13);
        r42 = new android.graphics.SweepGradient;
        r0 = r50;
        r5 = r0.mCircleX;
        r5 = (float) r5;
        r0 = r50;
        r7 = r0.mCircleY;
        r7 = (float) r7;
        r9 = 3;
        r9 = new int[r9];
        r0 = r50;
        r10 = r0.mLargeArcStartColor;
        r13 = 0;
        r9[r13] = r10;
        r0 = r50;
        r10 = r0.mLargeArcEndColor;
        r13 = 1;
        r9[r13] = r10;
        r0 = r50;
        r10 = r0.mLargeArcStartColor;
        r13 = 2;
        r9[r13] = r10;
        r10 = 0;
        r0 = r42;
        r0.<init>(r5, r7, r9, r10);
        r0 = r50;
        r5 = r0.mLargeArcPaint;
        r0 = r42;
        r5.setShader(r0);
        r0 = r50;
        r5 = r0.mTouchMoveY;
        r0 = r50;
        r7 = r0.mTouchDownY;
        r5 = r5 - r7;
        r7 = 1125777408; // 0x431a0000 float:154.0 double:5.56207942E-315;
        r5 = r5 * r7;
        r0 = r50;
        r7 = r0.mShutDownHeight;
        r7 = (float) r7;
        r9 = 1061997773; // 0x3f4ccccd float:0.8 double:5.246966156E-315;
        r7 = r7 * r9;
        r32 = r5 / r7;
        r0 = r50;
        r5 = r0.mTouchMoveY;
        r0 = r50;
        r7 = r0.mTouchDownY;
        r5 = r5 - r7;
        r0 = r50;
        r7 = r0.mShutDownHeight;
        r7 = (float) r7;
        r9 = 1061997773; // 0x3f4ccccd float:0.8 double:5.246966156E-315;
        r7 = r7 * r9;
        r5 = (r5 > r7 ? 1 : (r5 == r7 ? 0 : -1));
        if (r5 < 0) goto L_0x0389;
    L_0x0355:
        r0 = r50;
        r5 = r0.mTouchMoveY;
        r0 = r50;
        r7 = r0.mTouchDownY;
        r5 = r5 - r7;
        r0 = r50;
        r7 = r0.mShutDownHeight;
        r7 = (float) r7;
        r9 = 1061997773; // 0x3f4ccccd float:0.8 double:5.246966156E-315;
        r7 = r7 * r9;
        r5 = r5 - r7;
        r0 = r50;
        r7 = r0.mShutDownHeight;
        r7 = (float) r7;
        r9 = 1045220556; // 0x3e4ccccc float:0.19999999 double:5.16407569E-315;
        r7 = r7 * r9;
        r5 = r5 / r7;
        r7 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r5 = r7 - r5;
        r7 = 1132396544; // 0x437f0000 float:255.0 double:5.5947823E-315;
        r5 = r5 * r7;
        r0 = (int) r5;
        r43 = r0;
        if (r43 > 0) goto L_0x0380;
    L_0x037e:
        r43 = 0;
    L_0x0380:
        r0 = r50;
        r5 = r0.mLargeArcPaint;
        r0 = r43;
        r5.setAlpha(r0);
    L_0x0389:
        r5 = 1125777408; // 0x431a0000 float:154.0 double:5.56207942E-315;
        r5 = (r32 > r5 ? 1 : (r32 == r5 ? 0 : -1));
        if (r5 < 0) goto L_0x0391;
    L_0x038f:
        r32 = 1125777408; // 0x431a0000 float:154.0 double:5.56207942E-315;
    L_0x0391:
        r5 = 0;
        r5 = (r32 > r5 ? 1 : (r32 == r5 ? 0 : -1));
        if (r5 > 0) goto L_0x0398;
    L_0x0396:
        r32 = 0;
    L_0x0398:
        r5 = 1125777408; // 0x431a0000 float:154.0 double:5.56207942E-315;
        r12 = r5 - r32;
        r5 = 1128333312; // 0x43410000 float:193.0 double:5.574707265E-315;
        r11 = r5 + r32;
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x05ff;
    L_0x03a6:
        r0 = r50;
        r10 = r0.mLargeArcRect;
        r0 = r50;
        r14 = r0.mLargeArcPaint;
        r13 = 0;
        r9 = r51;
        r9.drawArc(r10, r11, r12, r13, r14);
    L_0x03b4:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x03d2;
    L_0x03ba:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x03c6;
    L_0x03c0:
        r0 = r50;
        r5 = r0.mStateRestore;
        if (r5 != 0) goto L_0x03d2;
    L_0x03c6:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x049b;
    L_0x03cc:
        r0 = r50;
        r5 = r0.mIsShutDown;
        if (r5 == 0) goto L_0x049b;
    L_0x03d2:
        r0 = r50;
        r5 = r0.mCircleX;
        r0 = r50;
        r7 = r0.mSmallArcRadius;
        r36 = r5 - r7;
        r0 = r50;
        r5 = r0.mCircleX;
        r0 = r50;
        r7 = r0.mSmallArcRadius;
        r37 = r5 + r7;
        r0 = r50;
        r5 = r0.mCircleY;
        r0 = r50;
        r7 = r0.mSmallArcRadius;
        r38 = r5 - r7;
        r0 = r50;
        r5 = r0.mCircleY;
        r0 = r50;
        r7 = r0.mSmallArcRadius;
        r35 = r5 + r7;
        r0 = r50;
        r5 = r0.mSmallArcRect;
        r0 = r36;
        r7 = (float) r0;
        r0 = r38;
        r9 = (float) r0;
        r0 = r37;
        r10 = (float) r0;
        r0 = r35;
        r13 = (float) r0;
        r5.set(r7, r9, r10, r13);
        r42 = new android.graphics.SweepGradient;
        r0 = r50;
        r5 = r0.mCircleX;
        r5 = (float) r5;
        r0 = r50;
        r7 = r0.mCircleY;
        r7 = (float) r7;
        r9 = 3;
        r9 = new int[r9];
        r0 = r50;
        r10 = r0.mSmallArcStartColor;
        r13 = 0;
        r9[r13] = r10;
        r0 = r50;
        r10 = r0.mSmallArcEndColor;
        r13 = 1;
        r9[r13] = r10;
        r0 = r50;
        r10 = r0.mSmallArcStartColor;
        r13 = 2;
        r9[r13] = r10;
        r10 = 0;
        r0 = r42;
        r0.<init>(r5, r7, r9, r10);
        r0 = r50;
        r5 = r0.mStartStaticAlpha;
        if (r5 == 0) goto L_0x0626;
    L_0x043d:
        r0 = r50;
        r5 = r0.mSmallArcPaint;
        r0 = r50;
        r7 = r0.mStaticArcAlpha;
        r7 = (int) r7;
        r5.setAlpha(r7);
        r0 = r50;
        r5 = r0.mSmallArcAngle;
        r7 = 1133969408; // 0x43970000 float:302.0 double:5.60255328E-315;
        r15 = r7 + r5;
        r0 = r50;
        r5 = r0.mSmallArcSweepAngle;
        r7 = 1133772800; // 0x43940000 float:296.0 double:5.601581907E-315;
        r16 = r7 - r5;
    L_0x0459:
        r0 = r50;
        r5 = r0.mFirstBgAlpha;
        if (r5 == 0) goto L_0x046a;
    L_0x045f:
        r0 = r50;
        r5 = r0.mSmallArcPaint;
        r0 = r50;
        r7 = r0.mStartDrawableAlpha;
        r5.setAlpha(r7);
    L_0x046a:
        r0 = r50;
        r5 = r0.mIsExitAnim;
        if (r5 == 0) goto L_0x047b;
    L_0x0470:
        r0 = r50;
        r5 = r0.mSmallArcPaint;
        r0 = r50;
        r7 = r0.mCancelBgAlpha;
        r5.setAlpha(r7);
    L_0x047b:
        r0 = r50;
        r5 = r0.mSmallArcPaint;
        r0 = r42;
        r5.setShader(r0);
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x062c;
    L_0x048a:
        r0 = r50;
        r14 = r0.mSmallArcRect;
        r0 = r50;
        r0 = r0.mSmallArcPaint;
        r18 = r0;
        r17 = 0;
        r13 = r51;
        r13.drawArc(r14, r15, r16, r17, r18);
    L_0x049b:
        r5 = isIndiaRegion();
        if (r5 == 0) goto L_0x0656;
    L_0x04a1:
        r50.drawIndiaCancelBg(r51);
    L_0x04a4:
        return;
    L_0x04a5:
        r0 = r50;
        r5 = r0.mTouchMoveY;
        r0 = r50;
        r7 = r0.mTouchDownY;
        r5 = r5 - r7;
        r0 = r50;
        r7 = r0.mShutDownHeight;
        r7 = (float) r7;
        r9 = 1061997773; // 0x3f4ccccd float:0.8 double:5.246966156E-315;
        r7 = r7 * r9;
        r45 = r5 / r7;
        r5 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r5 = r5 - r45;
        r7 = 1132396544; // 0x437f0000 float:255.0 double:5.5947823E-315;
        r5 = r5 * r7;
        r7 = 1063675494; // 0x3f666666 float:0.9 double:5.2552552E-315;
        r5 = r5 * r7;
        r7 = 1132396544; // 0x437f0000 float:255.0 double:5.5947823E-315;
        r7 = r7 * r45;
        r5 = r5 + r7;
        r0 = r50;
        r0.mBgAlpha = r5;
        r0 = r50;
        r5 = r0.mBgAlpha;
        r7 = 1132396544; // 0x437f0000 float:255.0 double:5.5947823E-315;
        r5 = (r5 > r7 ? 1 : (r5 == r7 ? 0 : -1));
        if (r5 < 0) goto L_0x005d;
    L_0x04d7:
        r5 = 1132396544; // 0x437f0000 float:255.0 double:5.5947823E-315;
        r0 = r50;
        r0.mBgAlpha = r5;
        goto L_0x005d;
    L_0x04df:
        r0 = r50;
        r5 = r0.mBgColor;
        r0 = r50;
        r7 = r0.mBgAlpha;
        r7 = (int) r7;
        r5.setAlpha(r7);
        goto L_0x006f;
    L_0x04ed:
        r0 = r50;
        r5 = r0.mShutDownTextDimen;
        r0 = r24;
        r7 = r0.top;
        r7 = (float) r7;
        r44 = r5 - r7;
        r0 = r50;
        r5 = r0.mContentHeight;
        r5 = r5 / 2;
        r7 = r33 / 2;
        r5 = r5 + r7;
        r0 = (float) r5;
        r34 = r0;
        goto L_0x00c6;
    L_0x0506:
        r51.save();
        r5 = -1028390912; // 0xffffffffc2b40000 float:-90.0 double:NaN;
        r0 = r51;
        r1 = r44;
        r2 = r34;
        r0.rotate(r5, r1, r2);
        r0 = r50;
        r5 = r0.mShutDownText;
        r0 = r50;
        r7 = r0.mShutDownPaint;
        r0 = r51;
        r1 = r44;
        r2 = r34;
        r0.drawText(r5, r1, r2, r7);
        r51.restore();
        goto L_0x00ff;
    L_0x052a:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x0536;
    L_0x0530:
        r0 = r50;
        r5 = r0.mStateRestore;
        if (r5 != 0) goto L_0x0542;
    L_0x0536:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x011b;
    L_0x053c:
        r0 = r50;
        r5 = r0.mIsShutDown;
        if (r5 == 0) goto L_0x011b;
    L_0x0542:
        r0 = r50;
        r0 = r0.mStaticBg;
        r22 = r0;
        goto L_0x011b;
    L_0x054a:
        r0 = r50;
        r5 = r0.mShutDownBgDimen;
        r0 = r50;
        r7 = r0.mStaticMoveY;
        r0 = r50;
        r9 = r0.mTouchDownY;
        r7 = r7 - r9;
        r5 = r5 + r7;
        r0 = r50;
        r7 = r0.mShutDownTextHeight;
        r7 = (float) r7;
        r5 = r5 + r7;
        r5 = (int) r5;
        r0 = r50;
        r0.mStaticBgLeft = r5;
        r0 = r50;
        r5 = r0.mContentHeight;
        r5 = r5 - r41;
        r5 = r5 / 2;
        r0 = r50;
        r0.mStaticBgTop = r5;
        goto L_0x0172;
    L_0x0571:
        r0 = r50;
        r5 = r0.mStaticBgRight;
        r7 = r40 / 2;
        r5 = r5 - r7;
        r0 = r50;
        r0.mCircleX = r5;
        r0 = r50;
        r5 = r0.mContentHeight;
        r5 = r5 / 2;
        r0 = r50;
        r0.mCircleY = r5;
        goto L_0x01eb;
    L_0x0588:
        r8 = r39;
        goto L_0x02ac;
    L_0x058c:
        r0 = r50;
        r5 = r0.mCircleY;
        r0 = r50;
        r7 = r0.mSmallArcRadius;
        r5 = r5 - r7;
        r6 = (float) r5;
        goto L_0x0220;
    L_0x0598:
        r0 = r50;
        r5 = r0.mStartStaticAlpha;
        if (r5 == 0) goto L_0x05ee;
    L_0x059e:
        r0 = r50;
        r5 = r0.mCircleX;
        r5 = (float) r5;
        r0 = r50;
        r7 = r0.mStaticLineStartY;
        r8 = r5 - r7;
    L_0x05a9:
        r0 = r50;
        r5 = r0.mLinePath;
        r5.rewind();
        r0 = r50;
        r5 = r0.mLinePath;
        r0 = r50;
        r7 = r0.mCircleY;
        r7 = (float) r7;
        r5.moveTo(r8, r7);
        r0 = r50;
        r5 = r0.mLinePath;
        r0 = r50;
        r7 = r0.mCircleX;
        r7 = (float) r7;
        r0 = r50;
        r9 = r0.mCircleY;
        r9 = (float) r9;
        r5.lineTo(r7, r9);
        r4 = new android.graphics.LinearGradient;
        r0 = r50;
        r5 = r0.mCircleY;
        r9 = (float) r5;
        r0 = r50;
        r5 = r0.mCircleX;
        r10 = (float) r5;
        r0 = r50;
        r5 = r0.mCircleY;
        r11 = (float) r5;
        r0 = r50;
        r12 = r0.mLineStartColor;
        r0 = r50;
        r13 = r0.mLineEndColor;
        r14 = android.graphics.Shader.TileMode.CLAMP;
        r7 = r4;
        r7.<init>(r8, r9, r10, r11, r12, r13, r14);
        goto L_0x0264;
    L_0x05ee:
        r0 = r50;
        r5 = r0.mCircleX;
        r0 = r50;
        r7 = r0.mSmallArcRadius;
        r5 = r5 - r7;
        r8 = (float) r5;
        goto L_0x05a9;
    L_0x05f9:
        r12 = r30;
        r11 = r29;
        goto L_0x03b4;
    L_0x05ff:
        r51.save();
        r5 = -1028390912; // 0xffffffffc2b40000 float:-90.0 double:NaN;
        r0 = r50;
        r7 = r0.mCircleX;
        r7 = (float) r7;
        r0 = r50;
        r9 = r0.mCircleY;
        r9 = (float) r9;
        r0 = r51;
        r0.rotate(r5, r7, r9);
        r0 = r50;
        r10 = r0.mLargeArcRect;
        r0 = r50;
        r14 = r0.mLargeArcPaint;
        r13 = 0;
        r9 = r51;
        r9.drawArc(r10, r11, r12, r13, r14);
        r51.restore();
        goto L_0x03b4;
    L_0x0626:
        r15 = 1133969408; // 0x43970000 float:302.0 double:5.60255328E-315;
        r16 = 1133772800; // 0x43940000 float:296.0 double:5.601581907E-315;
        goto L_0x0459;
    L_0x062c:
        r51.save();
        r5 = -1028390912; // 0xffffffffc2b40000 float:-90.0 double:NaN;
        r0 = r50;
        r7 = r0.mCircleX;
        r7 = (float) r7;
        r0 = r50;
        r9 = r0.mCircleY;
        r9 = (float) r9;
        r0 = r51;
        r0.rotate(r5, r7, r9);
        r0 = r50;
        r14 = r0.mSmallArcRect;
        r0 = r50;
        r0 = r0.mSmallArcPaint;
        r18 = r0;
        r17 = 0;
        r13 = r51;
        r13.drawArc(r14, r15, r16, r17, r18);
        r51.restore();
        goto L_0x049b;
    L_0x0656:
        r0 = r50;
        r5 = r0.mCancelBg;
        if (r5 == 0) goto L_0x0713;
    L_0x065c:
        r0 = r50;
        r5 = r0.mCancelBg;
        r20 = r5.getIntrinsicWidth();
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x07d5;
    L_0x066a:
        r0 = r50;
        r5 = r0.mContentWidth;
        r5 = r5 - r20;
        r5 = r5 / 2;
        r0 = r50;
        r0.mCancelBgLeft = r5;
        r0 = r50;
        r5 = r0.mContentHeight;
        r5 = (float) r5;
        r0 = r50;
        r7 = r0.mCancelBgDimen;
        r5 = r5 - r7;
        r5 = (int) r5;
        r0 = r50;
        r0.mCancelBgTop = r5;
    L_0x0685:
        r0 = r50;
        r5 = r0.mCancelBgLeft;
        r5 = r5 + r20;
        r0 = r50;
        r0.mCancelBgRight = r5;
        r0 = r50;
        r5 = r0.mCancelBgHeight;
        r0 = r50;
        r7 = r0.mCancelBgTop;
        r5 = r5 + r7;
        r0 = r50;
        r0.mCancelBgBottom = r5;
        r0 = r50;
        r5 = r0.mCancelBg;
        r0 = r50;
        r7 = r0.mCancelBgLeft;
        r0 = r50;
        r9 = r0.mCancelBgTop;
        r0 = r50;
        r10 = r0.mCancelBgRight;
        r0 = r50;
        r13 = r0.mCancelBgBottom;
        r5.setBounds(r7, r9, r10, r13);
        r0 = r50;
        r5 = r0.mTouchMoveY;
        r0 = r50;
        r7 = r0.mTouchDownY;
        r5 = r5 - r7;
        r0 = r50;
        r7 = r0.mShutDownHeight;
        r0 = (double) r7;
        r46 = r0;
        r48 = 4602678819172646912; // 0x3fe0000000000000 float:0.0 double:0.5;
        r46 = r46 * r48;
        r0 = r46;
        r7 = (float) r0;
        r5 = r5 / r7;
        r7 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r5 = r7 - r5;
        r7 = 1132396544; // 0x437f0000 float:255.0 double:5.5947823E-315;
        r5 = r5 * r7;
        r0 = (int) r5;
        r43 = r0;
        if (r43 > 0) goto L_0x06d9;
    L_0x06d7:
        r43 = 0;
    L_0x06d9:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x06e8;
    L_0x06df:
        r0 = r50;
        r5 = r0.mCancelBg;
        r0 = r43;
        r5.setAlpha(r0);
    L_0x06e8:
        r0 = r50;
        r5 = r0.mIsExitAnim;
        if (r5 == 0) goto L_0x06f9;
    L_0x06ee:
        r0 = r50;
        r5 = r0.mCancelBg;
        r0 = r50;
        r7 = r0.mCancelBgAlpha;
        r5.setAlpha(r7);
    L_0x06f9:
        r0 = r50;
        r5 = r0.mFirstBgAlpha;
        if (r5 == 0) goto L_0x070a;
    L_0x06ff:
        r0 = r50;
        r5 = r0.mCancelBg;
        r0 = r50;
        r7 = r0.mStartDrawableAlpha;
        r5.setAlpha(r7);
    L_0x070a:
        r0 = r50;
        r5 = r0.mCancelBg;
        r0 = r51;
        r5.draw(r0);
    L_0x0713:
        r0 = r50;
        r5 = r0.mCancelText;
        if (r5 == 0) goto L_0x04a4;
    L_0x0719:
        r0 = r50;
        r5 = r0.mCancelPaint;
        r24 = r5.getFontMetricsInt();
        r0 = r50;
        r5 = r0.mCancelPaint;
        r0 = r50;
        r7 = r0.mCancelText;
        r5 = r5.measureText(r7);
        r0 = (int) r5;
        r21 = r0;
        r0 = r24;
        r5 = r0.bottom;
        r0 = r24;
        r7 = r0.top;
        r5 = r5 - r7;
        r0 = r50;
        r0.mCancelTextHeight = r5;
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x07f2;
    L_0x0743:
        r0 = r50;
        r5 = r0.mContentWidth;
        r5 = r5 - r21;
        r5 = r5 / 2;
        r0 = (float) r5;
        r44 = r0;
        r0 = r50;
        r5 = r0.mContentHeight;
        r0 = r24;
        r7 = r0.top;
        r5 = r5 - r7;
        r5 = (float) r5;
        r0 = r50;
        r7 = r0.mCancelTextDimen;
        r5 = r5 - r7;
        r5 = (int) r5;
        r0 = r50;
        r0.mCancelTextY = r5;
    L_0x0762:
        r0 = r50;
        r5 = r0.mTouchMoveY;
        r0 = r50;
        r7 = r0.mTouchDownY;
        r5 = r5 - r7;
        r0 = r50;
        r7 = r0.mShutDownHeight;
        r0 = (double) r7;
        r46 = r0;
        r48 = 4602678819172646912; // 0x3fe0000000000000 float:0.0 double:0.5;
        r46 = r46 * r48;
        r0 = r46;
        r7 = (float) r0;
        r5 = r5 / r7;
        r7 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r5 = r7 - r5;
        r7 = 1132396544; // 0x437f0000 float:255.0 double:5.5947823E-315;
        r5 = r5 * r7;
        r0 = (int) r5;
        r43 = r0;
        if (r43 > 0) goto L_0x0788;
    L_0x0786:
        r43 = 0;
    L_0x0788:
        r0 = r50;
        r5 = r0.mFirstBgAlpha;
        if (r5 == 0) goto L_0x0799;
    L_0x078e:
        r0 = r50;
        r5 = r0.mShutDownPaint;
        r0 = r50;
        r7 = r0.mStartTextAlpha;
        r5.setAlpha(r7);
    L_0x0799:
        r0 = r50;
        r5 = r0.mIsTouchShutBg;
        if (r5 == 0) goto L_0x07a8;
    L_0x079f:
        r0 = r50;
        r5 = r0.mCancelPaint;
        r0 = r43;
        r5.setAlpha(r0);
    L_0x07a8:
        r0 = r50;
        r5 = r0.mIsExitAnim;
        if (r5 == 0) goto L_0x07b9;
    L_0x07ae:
        r0 = r50;
        r5 = r0.mCancelPaint;
        r0 = r50;
        r7 = r0.mCancelTextAlpha;
        r5.setAlpha(r7);
    L_0x07b9:
        r0 = r50;
        r5 = r0.mIsOrientationPortrait;
        if (r5 == 0) goto L_0x0813;
    L_0x07bf:
        r0 = r50;
        r5 = r0.mCancelText;
        r0 = r50;
        r7 = r0.mCancelTextY;
        r7 = (float) r7;
        r0 = r50;
        r9 = r0.mCancelPaint;
        r0 = r51;
        r1 = r44;
        r0.drawText(r5, r1, r7, r9);
        goto L_0x04a4;
    L_0x07d5:
        r0 = r50;
        r5 = r0.mContentWidth;
        r5 = (float) r5;
        r0 = r50;
        r7 = r0.mCancelBgDimen;
        r5 = r5 - r7;
        r5 = (int) r5;
        r0 = r50;
        r0.mCancelBgLeft = r5;
        r0 = r50;
        r5 = r0.mContentHeight;
        r5 = r5 - r20;
        r5 = r5 / 2;
        r0 = r50;
        r0.mCancelBgTop = r5;
        goto L_0x0685;
    L_0x07f2:
        r0 = r50;
        r5 = r0.mContentWidth;
        r0 = r24;
        r7 = r0.top;
        r5 = r5 - r7;
        r5 = (float) r5;
        r0 = r50;
        r7 = r0.mCancelTextDimen;
        r5 = r5 - r7;
        r5 = (int) r5;
        r0 = (float) r5;
        r44 = r0;
        r0 = r50;
        r5 = r0.mContentHeight;
        r5 = r5 + r21;
        r5 = r5 / 2;
        r0 = r50;
        r0.mCancelTextY = r5;
        goto L_0x0762;
    L_0x0813:
        r51.save();
        r5 = -1028390912; // 0xffffffffc2b40000 float:-90.0 double:NaN;
        r0 = r50;
        r7 = r0.mCancelTextY;
        r7 = (float) r7;
        r0 = r51;
        r1 = r44;
        r0.rotate(r5, r1, r7);
        r0 = r50;
        r5 = r0.mCancelText;
        r0 = r50;
        r7 = r0.mCancelTextY;
        r7 = (float) r7;
        r0 = r50;
        r9 = r0.mCancelPaint;
        r0 = r51;
        r1 = r44;
        r0.drawText(r5, r1, r7, r9);
        r51.restore();
        goto L_0x04a4;
    L_0x083d:
        r12 = r30;
        r11 = r29;
        goto L_0x03b4;
    L_0x0843:
        r8 = r39;
        goto L_0x02ac;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.color.widget.ColorGlobalActionView.onDraw(android.graphics.Canvas):void");
    }

    private void startAutoDownReboundAnim() {
        ValueAnimator rebounceAnim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        rebounceAnim.addUpdateListener(new AnimUpdateListener(2));
        rebounceAnim.setDuration(150);
        rebounceAnim.setInterpolator(AUTODOWNREBOUNCEINTER);
        rebounceAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                ColorGlobalActionView.this.mFirstRebounceAnim = false;
            }

            public void onAnimationEnd(Animator animation) {
                ColorGlobalActionView.this.mFirstRebounceAnim = true;
                ColorGlobalActionView.this.mFirstBgAlpha = false;
                ColorGlobalActionView.this.mTouchHelper.sendEventForVirtualView(0, 8);
            }
        });
        rebounceAnim.start();
    }

    private boolean detectionIsClickCancelBg(float x, float y) {
        if (x < ((float) this.mCancelBgLeft) || x > ((float) this.mCancelBgRight) || y < ((float) this.mCancelBgTop) || y > ((float) this.mCancelBgBottom) || !this.mFirstRebounceAnim) {
            return false;
        }
        return true;
    }

    private boolean detectionIsTouchShutDownBg(float x, float y) {
        if (x < ((float) this.mStaticBgLeft) || x > ((float) this.mStaticBgRight) || y < ((float) this.mStaticBgTop) || y > ((float) this.mStaticBgBottom) || !this.mFirstRebounceAnim) {
            return false;
        }
        return true;
    }

    private boolean detectionAccessTouch(float x, float y) {
        if (x < 0.0f || x > ((float) this.mContentWidth) || y < 0.0f || y > ((float) (this.mStaticBgBottom * 2)) || !this.mFirstRebounceAnim) {
            return false;
        }
        return true;
    }

    private void startExitAnim() {
        ValueAnimator exitAlpha = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        exitAlpha.addUpdateListener(new AnimUpdateListener(3));
        exitAlpha.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ColorGlobalActionView.this.setQuitView();
            }
        });
        exitAlpha.setDuration(250);
        exitAlpha.setInterpolator(EXITAINM);
        exitAlpha.start();
    }

    private void setQuitView() {
        if (this.mOnCancelListener != null) {
            this.mOnCancelListener.onCancel();
        }
        this.mTouchHelper.sendEventForVirtualView(1, 1);
    }

    private void startReturnInitialPosition() {
        double time = (double) (((this.mTouchUpY - this.mTouchDownY) / (((float) this.mShutDownHeight) * CRITICAL_NUMBER)) * 500.0f);
        this.mRestoreYAnim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mRestoreYAnim.addUpdateListener(new AnimUpdateListener(4));
        this.mRestoreYAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ColorGlobalActionView.this.startRestoreFirstRebound();
            }
        });
        this.mRestoreYAnim.setDuration((long) time);
        this.mRestoreYAnim.setInterpolator(RESTOREPOSITION);
        this.mRestoreYAnim.start();
    }

    private void startCancelToInitialPosition() {
        this.mRestoreYAnim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mRestoreYAnim.addUpdateListener(new AnimUpdateListener(4));
        this.mRestoreYAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ColorGlobalActionView.this.startRestoreFirstRebound();
            }
        });
        this.mRestoreYAnim.setDuration(100);
        this.mRestoreYAnim.setInterpolator(RESTOREPOSITION);
        this.mRestoreYAnim.start();
    }

    private void startRestoreFirstRebound() {
        this.mFirstRebound = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mFirstRebound.addUpdateListener(new AnimUpdateListener(5));
        this.mFirstRebound.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ColorGlobalActionView.this.startRestoreSecondRebound();
            }
        });
        this.mFirstRebound.setDuration(150);
        this.mFirstRebound.setInterpolator(RESTOREREBOUNCE);
        this.mFirstRebound.start();
    }

    private void startRestoreSecondRebound() {
        this.mSecondRebound = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mSecondRebound.addUpdateListener(new AnimUpdateListener(6));
        this.mSecondRebound.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ColorGlobalActionView.this.mTouchMoveY = 0.0f;
                ColorGlobalActionView.this.mTouchDownY = 0.0f;
                ColorGlobalActionView.this.mStaticMoveY = 0.0f;
                ColorGlobalActionView.this.mStateRestore = false;
                ColorGlobalActionView.this.mIsTouchShutBg = false;
            }
        });
        this.mSecondRebound.setDuration(150);
        this.mSecondRebound.setInterpolator(RESTOREREBOUNCE);
        this.mSecondRebound.start();
    }

    private void startShutDownYAnim() {
        double time = (((double) ((this.mTouchUpY - this.mTouchDownY) - (((float) this.mShutDownHeight) * CRITICAL_NUMBER))) / (((double) this.mShutDownHeight) * 0.25d)) * 200.0d;
        ValueAnimator shutDownYAnim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        shutDownYAnim.addUpdateListener(new AnimUpdateListener(7));
        shutDownYAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ColorGlobalActionView.this.startStaticBgAlphaAnim();
            }
        });
        shutDownYAnim.setDuration((long) time);
        shutDownYAnim.setInterpolator(SHUTDOWNINTER);
        shutDownYAnim.start();
    }

    private void startStaticBgAlphaAnim() {
        ValueAnimator staticBgAlphaAnim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        staticBgAlphaAnim.addUpdateListener(new AnimUpdateListener(8));
        staticBgAlphaAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                ColorGlobalActionView.this.mStartStaticAlpha = true;
                ColorGlobalActionView.this.startStaticLineAnim();
                ColorGlobalActionView.this.startStaticArcAnim();
            }
        });
        staticBgAlphaAnim.setDuration(100);
        staticBgAlphaAnim.setInterpolator(EXITAINM);
        staticBgAlphaAnim.start();
    }

    private void startStaticLineAnim() {
        ValueAnimator staticLineAlphaAnim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        staticLineAlphaAnim.addUpdateListener(new AnimUpdateListener(9));
        staticLineAlphaAnim.setDuration(300);
        staticLineAlphaAnim.setInterpolator(EXITAINM);
        staticLineAlphaAnim.start();
    }

    private void startStaticArcAnim() {
        ValueAnimator staticArcAlphaAnim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        staticArcAlphaAnim.addUpdateListener(new AnimUpdateListener(STATIC_ARC_FLAG));
        staticArcAlphaAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (ColorGlobalActionView.this.mShutDownLister != null) {
                    ColorGlobalActionView.this.mShutDownLister.onShutDown();
                }
            }
        });
        staticArcAlphaAnim.setDuration(350);
        staticArcAlphaAnim.setInterpolator(EXITAINM);
        staticArcAlphaAnim.start();
    }

    private boolean isMultiPointerEvent(MotionEvent event) {
        if (event.getPointerId(event.getActionIndex()) > 0) {
            return true;
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case 0:
                this.mIsClickCancelBg = detectionIsClickCancelBg(x, y);
                if (ColorAccessibilityUtil.isTalkbackEnabled(getContext()) && AccessibilityManager.getInstance(getContext()).isTouchExplorationEnabled()) {
                    this.mIsTouchShutBg = detectionAccessTouch(x, y);
                } else {
                    this.mIsTouchShutBg = detectionIsTouchShutDownBg(x, y);
                }
                if (this.mSecondRebound != null && this.mSecondRebound.isRunning()) {
                    this.mSecondRebound.end();
                    this.mIsTouchShutBg = false;
                }
                if (this.mFirstRebound != null && this.mFirstRebound.isRunning()) {
                    this.mFirstRebound.end();
                    this.mIsTouchShutBg = false;
                }
                if (this.mRestoreYAnim != null && this.mRestoreYAnim.isRunning()) {
                    this.mRestoreYAnim.end();
                    this.mIsTouchShutBg = false;
                }
                if (this.mIsOrientationPortrait) {
                    this.mShutDownHeight = this.mCancelBgTop - (this.mStaticBgTop + (this.mStaticBg.getIntrinsicHeight() / 2));
                } else {
                    this.mShutDownHeight = this.mCancelBgLeft - (this.mStaticBgLeft + (this.mStaticBg.getIntrinsicHeight() / 2));
                }
                if (this.mIsTouchShutBg && this.mIsOrientationPortrait) {
                    this.mTouchDownY = event.getY();
                } else if (this.mIsTouchShutBg && (this.mIsOrientationPortrait ^ 1) != 0) {
                    this.mTouchDownY = event.getX();
                }
                if (this.mIsClickCancelBg) {
                    this.mCancelBg = this.mCancelPressBg;
                }
                this.mIsClickEmergencyBg = detectionIsClickEmergencyBg(x, y);
                if (this.mIsClickEmergencyBg) {
                    this.mEmergencyBg = this.mEmergencyPressBg;
                }
                invalidate();
                break;
            case 1:
                if (this.mIsTouchShutBg) {
                    this.mTouchUpY = this.mTouchMoveY;
                    if (this.mTouchUpY - this.mTouchDownY < ((float) this.mShutDownHeight) * CRITICAL_NUMBER && this.mTouchUpY > this.mTouchDownY) {
                        this.mStateRestore = true;
                        startReturnInitialPosition();
                    }
                    if (this.mTouchUpY - this.mTouchDownY >= ((float) this.mShutDownHeight) * CRITICAL_NUMBER && this.mTouchUpY > this.mTouchDownY) {
                        if (this.mTouchUpY - this.mTouchDownY > ((float) this.mShutDownHeight) && this.mTouchUpY > this.mTouchDownY) {
                            this.mTouchUpY = this.mTouchDownY + ((float) this.mShutDownHeight);
                        }
                        this.mIsShutDown = true;
                        setEnabled(false);
                        startShutDownYAnim();
                    }
                    if (this.mTouchUpY <= this.mTouchDownY) {
                        this.mTouchUpY = this.mTouchDownY;
                        this.mStateRestore = false;
                        this.mIsTouchShutBg = false;
                    }
                    invalidate();
                }
                if (this.mIsClickCancelBg) {
                    this.mCancelBg = this.mCancelNormalBg;
                    this.mIsExitAnim = this.mIsClickCancelBg;
                    startExitAnim();
                    invalidate();
                }
                if (this.mIsClickEmergencyBg) {
                    this.mEmergencyBg = this.mEmergencyNormalBg;
                    this.mIsExitAnim = this.mIsClickEmergencyBg;
                    startEmergencyExitAnim();
                    invalidate();
                    break;
                }
                break;
            case 2:
                if (this.mIsTouchShutBg) {
                    float dimen;
                    if (this.mIsOrientationPortrait) {
                        this.mTouchMoveY = event.getY();
                        if (((double) (this.mTouchMoveY - this.mTouchDownY)) > ((double) this.mShutDownHeight) * 0.8d) {
                            dimen = (float) ((((double) this.mTouchMoveY) - (((double) this.mShutDownHeight) * 0.8d)) - ((double) (this.mCancelBgTop - this.mShutDownHeight)));
                            this.mTouchMoveY = (float) ((((double) (this.mCancelBgTop - this.mShutDownHeight)) + (((double) this.mShutDownHeight) * 0.8d)) + ((double) ((float) (((((double) this.mShutDownHeight) * 0.4d) * ((double) dimen)) / ((((double) this.mShutDownHeight) * 0.4d) + ((double) dimen))))));
                        }
                    } else {
                        this.mTouchMoveY = event.getX();
                        if (((double) (this.mTouchMoveY - this.mTouchDownY)) > ((double) this.mShutDownHeight) * 0.8d) {
                            dimen = (float) ((((double) this.mTouchMoveY) - (((double) this.mShutDownHeight) * 0.8d)) - ((double) (this.mCancelBgLeft - this.mShutDownHeight)));
                            this.mTouchMoveY = (float) ((((double) (this.mCancelBgLeft - this.mShutDownHeight)) + (((double) this.mShutDownHeight) * 0.8d)) + ((double) ((float) (((((double) this.mShutDownHeight) * 0.4d) * ((double) dimen)) / ((((double) this.mShutDownHeight) * 0.4d) + ((double) dimen))))));
                        }
                    }
                    if (this.mTouchMoveY - this.mTouchDownY > ((float) this.mShutDownHeight) && this.mTouchMoveY > this.mTouchDownY) {
                        this.mTouchMoveY = this.mTouchDownY + ((float) this.mShutDownHeight);
                    }
                    this.mStaticMoveY = this.mTouchMoveY;
                    invalidate();
                    break;
                }
                break;
            case 3:
                Log.i(TAG, "ColorGlobalActionView ACTION_CANCEL mTouchDownY = " + this.mTouchDownY + ", mTouchUpY = " + this.mTouchUpY);
                if (2 < event.getPointerCount()) {
                    this.mTouchUpY = this.mTouchDownY;
                    this.mIsTouchShutBg = false;
                    this.mStateRestore = true;
                    startCancelToInitialPosition();
                }
                this.mIsClickCancelBg = false;
                this.mIsClickEmergencyBg = false;
                invalidate();
                break;
        }
        return true;
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mSecondRebound = null;
        this.mFirstRebound = null;
        this.mRestoreYAnim = null;
        this.mStartStaticAlpha = false;
    }

    public void startAutoDownAnim() {
        setEnabled(true);
        ValueAnimator autoDownAnim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        autoDownAnim.addUpdateListener(new AnimUpdateListener(1));
        autoDownAnim.setDuration(475);
        autoDownAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ColorGlobalActionView.this.mShutDownBgAutoEnd = ColorGlobalActionView.this.mShutDownBgDimen;
                ColorGlobalActionView.this.mCancelAutoEnd = ColorGlobalActionView.this.mCancelBgDimen;
                ColorGlobalActionView.this.startAutoDownReboundAnim();
            }
        });
        autoDownAnim.setInterpolator(AUTODOWNINTER);
        autoDownAnim.start();
    }

    public void setOnCancelListener(OnCancelListener cancel) {
        this.mOnCancelListener = cancel;
    }

    public void setOnShutDownListener(OnShutDownListener shutDown) {
        this.mShutDownLister = shutDown;
    }

    private boolean detectionIsClickEmergencyBg(float x, float y) {
        if (x < ((float) this.mEmergencyBgLeft) || x > ((float) this.mEmergencyBgRight) || y < ((float) this.mEmergencyBgTop) || y > ((float) this.mEmergencyBgBottom) || !this.mFirstRebounceAnim) {
            return false;
        }
        return true;
    }

    private void startEmergencyExitAnim() {
        ValueAnimator exitAlpha = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        exitAlpha.addUpdateListener(new AnimUpdateListener(3));
        exitAlpha.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (ColorGlobalActionView.this.mOnEmergencyListener != null) {
                    ColorGlobalActionView.this.mOnEmergencyListener.onEmergency();
                }
            }
        });
        exitAlpha.setDuration(250);
        exitAlpha.setInterpolator(EXITAINM);
        exitAlpha.start();
    }

    public void setOnEmergencyListener(OnEmergencyListener emergency) {
        this.mOnEmergencyListener = emergency;
    }

    public static boolean isIndiaRegion() {
        if ("IN".equalsIgnoreCase(SystemProperties.get("persist.sys.oppo.region", "CN"))) {
            return true;
        }
        return false;
    }

    public void drawIndiaCancelBg(Canvas canvas) {
        int temp;
        if (!(this.mCancelBg == null || this.mEmergencyBg == null)) {
            int cancelBgWidth = this.mCancelBg.getIntrinsicWidth();
            if (this.mIsOrientationPortrait) {
                this.mCancelBgLeft = (this.mContentWidth / 2) + (((this.mContentWidth / 2) - cancelBgWidth) / 2);
                this.mEmergencyBgLeft = ((this.mContentWidth / 2) - cancelBgWidth) / 2;
                this.mEmergencyBgTop = (int) (((float) this.mContentHeight) - this.mCancelBgDimen);
                this.mCancelBgTop = this.mEmergencyBgTop;
            } else {
                this.mEmergencyBgLeft = (int) (((float) this.mContentWidth) - this.mCancelBgDimen);
                this.mCancelBgLeft = this.mEmergencyBgLeft;
                this.mCancelBgTop = ((this.mContentHeight / 2) - cancelBgWidth) / 2;
                this.mEmergencyBgTop = (this.mContentHeight / 2) + (((this.mContentHeight / 2) - cancelBgWidth) / 2);
            }
            this.mCancelBgRight = this.mCancelBgLeft + cancelBgWidth;
            this.mCancelBgBottom = this.mCancelBgHeight + this.mCancelBgTop;
            this.mCancelBg.setBounds(this.mCancelBgLeft, this.mCancelBgTop, this.mCancelBgRight, this.mCancelBgBottom);
            this.mEmergencyBgRight = this.mEmergencyBgLeft + cancelBgWidth;
            this.mEmergencyBgBottom = this.mCancelBgHeight + this.mEmergencyBgTop;
            this.mEmergencyBg.setBounds(this.mEmergencyBgLeft, this.mEmergencyBgTop, this.mEmergencyBgRight, this.mEmergencyBgBottom);
            temp = (int) ((1.0f - ((this.mTouchMoveY - this.mTouchDownY) / ((float) (((double) this.mShutDownHeight) * 0.5d)))) * 255.0f);
            if (temp <= 0) {
                temp = 0;
            }
            if (this.mIsTouchShutBg) {
                this.mCancelBg.setAlpha(temp);
                this.mEmergencyBg.setAlpha(temp);
            }
            if (this.mIsExitAnim) {
                this.mCancelBg.setAlpha(this.mCancelBgAlpha);
                this.mEmergencyBg.setAlpha(this.mCancelBgAlpha);
            }
            if (this.mFirstBgAlpha) {
                this.mCancelBg.setAlpha(this.mStartDrawableAlpha);
                this.mEmergencyBg.setAlpha(this.mStartDrawableAlpha);
            }
            this.mCancelBg.draw(canvas);
            this.mEmergencyBg.draw(canvas);
        }
        if (this.mCancelText != null && this.mEmergencyText != null) {
            float cancelTextX;
            float emergencyTextX;
            FontMetricsInt fmi = this.mCancelPaint.getFontMetricsInt();
            int cancelTextWidth = (int) this.mCancelPaint.measureText(this.mCancelText);
            int emergencyTextWidth = (int) this.mCancelPaint.measureText(this.mEmergencyText);
            this.mCancelTextHeight = fmi.bottom - fmi.top;
            if (this.mIsOrientationPortrait) {
                cancelTextX = (float) ((this.mContentWidth / 2) + (((this.mContentWidth / 2) - cancelTextWidth) / 2));
                emergencyTextX = (float) (((this.mContentWidth / 2) - emergencyTextWidth) / 2);
                this.mEmergencyTextY = (int) (((float) (this.mContentHeight - fmi.top)) - this.mCancelTextDimen);
                this.mCancelTextY = this.mEmergencyTextY;
            } else {
                emergencyTextX = (float) ((int) (((float) (this.mContentWidth - fmi.top)) - this.mCancelTextDimen));
                cancelTextX = emergencyTextX;
                this.mCancelTextY = ((this.mContentHeight / 2) + cancelTextWidth) / 2;
                this.mEmergencyTextY = (this.mContentHeight / 2) + (((this.mContentHeight / 2) + emergencyTextWidth) / 2);
            }
            temp = (int) ((1.0f - ((this.mTouchMoveY - this.mTouchDownY) / ((float) (((double) this.mShutDownHeight) * 0.5d)))) * 255.0f);
            if (temp <= 0) {
                temp = 0;
            }
            if (this.mFirstBgAlpha) {
                this.mShutDownPaint.setAlpha(this.mStartTextAlpha);
            }
            if (this.mIsTouchShutBg) {
                this.mCancelPaint.setAlpha(temp);
            }
            if (this.mIsExitAnim) {
                this.mCancelPaint.setAlpha(this.mCancelTextAlpha);
            }
            if (this.mIsOrientationPortrait) {
                canvas.drawText(this.mCancelText, cancelTextX, (float) this.mCancelTextY, this.mCancelPaint);
                canvas.drawText(this.mEmergencyText, emergencyTextX, (float) this.mEmergencyTextY, this.mCancelPaint);
                return;
            }
            canvas.save();
            canvas.rotate(-90.0f, cancelTextX, (float) this.mCancelTextY);
            canvas.drawText(this.mCancelText, cancelTextX, (float) this.mCancelTextY, this.mCancelPaint);
            canvas.restore();
            canvas.save();
            canvas.rotate(-90.0f, emergencyTextX, (float) this.mEmergencyTextY);
            canvas.drawText(this.mEmergencyText, emergencyTextX, (float) this.mEmergencyTextY, this.mCancelPaint);
            canvas.restore();
        }
    }

    public void clearAccessibilityFocus() {
        if (this.mTouchHelper != null) {
            this.mTouchHelper.clearFocusedVirtualView();
        }
    }

    protected boolean dispatchHoverEvent(MotionEvent event) {
        if (this.mTouchHelper == null || !this.mTouchHelper.dispatchHoverEvent(event)) {
            return super.dispatchHoverEvent(event);
        }
        return true;
    }
}
