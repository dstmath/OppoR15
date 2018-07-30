package com.oppo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.ListView;
import oppo.R;

@Deprecated
public class OppoListView extends ListView {
    private static final boolean DBG = false;
    private static final boolean DBG_MOTION = false;
    private static final int ID_CHECKBOX = 201457665;
    private static final int INVALID_SCROLL_CHOICE_POSITION = -2;
    private static final String OPPO_WIDGET_ANIM_DISABLE = "oppo.widget.animation.disabled";
    private static final long SCROLL_CHOICE_SCROLL_DELAY = 50;
    public static final int SCROLL_HORIZONTAL_LTR = 1;
    public static final int SCROLL_HORIZONTAL_NULL = 0;
    public static final int SCROLL_HORIZONTAL_RTL = 2;
    private static final String TAG = "OppoListView";
    private AccelerateInterpolator mAccelerateInterpolator;
    private boolean mAnimationDisabled;
    private CheckBox mCheckBox;
    private int mCurPosition;
    private DecelerateInterpolator mDecelerateInterpolator;
    private Runnable mDelayedScroll;
    private int mDividerItemHeight;
    private boolean mFillDivider;
    private int mFirstPos;
    private boolean mFlag;
    private boolean mHasMoved;
    private boolean mIsNotDrawFirstLine;
    private int mItemBottom;
    private int mItemCount;
    private int mItemHeight;
    private boolean mItemToAppear;
    private int mItemTop;
    private int mLastPos;
    private int mLastPosition;
    private int mLeftOffset;
    private boolean mMultiChoice;
    private float mPointY;
    private float mPositionOffset;
    private int mReferPosition;
    private int mRightOffset;
    private int mScrollDirection;
    private ScrollMultiChoiceListener mScrollMultiChoiceListener;
    private boolean mSpringEnabled;
    private boolean mUpScroll;
    private View mViewPager;

    public interface ScrollMultiChoiceListener {
        void onItemTouch(int i, View view);
    }

    public OppoListView(Context context) {
        this(context, null);
    }

    public OppoListView(Context context, AttributeSet attrs) {
        this(context, attrs, 201393152);
    }

    public OppoListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mIsNotDrawFirstLine = false;
        this.mFlag = false;
        this.mMultiChoice = true;
        this.mUpScroll = true;
        this.mHasMoved = false;
        this.mCheckBox = null;
        this.mLastPosition = INVALID_SCROLL_CHOICE_POSITION;
        this.mAnimationDisabled = false;
        this.mDelayedScroll = new Runnable() {
            public void run() {
                if (OppoListView.this.mUpScroll) {
                    OppoListView.this.setSelection(OppoListView.this.mCurPosition - 1);
                    return;
                }
                OppoListView.this.setSelectionFromTop(OppoListView.this.mCurPosition + 1, ((OppoListView.this.getHeight() - OppoListView.this.getPaddingTop()) - OppoListView.this.getPaddingBottom()) - OppoListView.this.mItemHeight);
            }
        };
        this.mItemToAppear = true;
        this.mScrollDirection = 0;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OppoListView, defStyle, 0);
        setSpringEnabled(a.getBoolean(0, false));
        setFillDivider(a.getBoolean(1, false));
        int dividerItemHeight = a.getInt(2, 0);
        if (dividerItemHeight == 0) {
            dividerItemHeight = getResources().getDimensionPixelOffset(201655360);
        }
        setDividerItemHeight(dividerItemHeight);
        a.recycle();
        this.mLeftOffset = getResources().getDimensionPixelOffset(201655351);
        this.mRightOffset = getResources().getDimensionPixelOffset(201655352);
        this.mItemHeight = getResources().getDimensionPixelOffset(201655360);
        this.mAccelerateInterpolator = new AccelerateInterpolator();
        this.mDecelerateInterpolator = new DecelerateInterpolator();
        this.mAnimationDisabled = context.getPackageManager().hasSystemFeature(OPPO_WIDGET_ANIM_DISABLE);
    }

    public void setSpringEnabled(boolean springEnable) {
        this.mSpringEnabled = springEnable;
        if (this.mSpringEnabled) {
            setOverScrollMode(0);
        } else {
            setOverScrollMode(2);
        }
    }

    public boolean isSpringEnabled() {
        return getOverScrollMode() != 2;
    }

    public void setFillDivider(boolean fillDivider) {
        this.mFillDivider = fillDivider;
    }

    public boolean getFillDivider() {
        return this.mFillDivider;
    }

    public void setDividerItemHeight(int itemHeight) {
        this.mDividerItemHeight = itemHeight;
    }

    public int getDividerItemHeight() {
        return this.mDividerItemHeight;
    }

    public void setIsNotDrawFirstLine(boolean flag) {
        this.mIsNotDrawFirstLine = flag;
    }

    public boolean getIsNotDrawFirstLine() {
        return this.mIsNotDrawFirstLine;
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        boolean isFullScreen = false;
        int count = getChildCount();
        int scrollY = getScrollY();
        View lastchild = null;
        if (count > 0) {
            lastchild = getChildAt(count - 1);
        }
        if (lastchild != null && (lastchild.getBottom() >= getHeight() || lastchild.getBottom() + 1 >= getHeight())) {
            isFullScreen = true;
        }
        if (getFillDivider() && (isFullScreen ^ 1) != 0) {
            int dividerHeight = getDividerHeight();
            boolean drawDividers = dividerHeight > 0 ? getDivider() != null : false;
            int listBottom = (getBottom() - getTop()) - getListPaddingBottom();
            int fillBottom = 0;
            int dividerItemHeight = getDividerItemHeight();
            if (count > 0) {
                fillBottom = lastchild.getBottom();
            }
            Rect bounds = new Rect();
            bounds.left = getPaddingLeft();
            bounds.right = (getRight() - getLeft()) - getPaddingRight();
            if (this.mIsNotDrawFirstLine) {
                fillBottom = (fillBottom + dividerItemHeight) + dividerHeight;
            }
            while (dividerItemHeight > 0 && fillBottom < listBottom + scrollY && drawDividers) {
                bounds.top = fillBottom;
                bounds.bottom = fillBottom + dividerHeight;
                drawDivider(canvas, bounds, 0);
                fillBottom = bounds.bottom + dividerItemHeight;
            }
        }
    }

    public void drawDivider(Canvas canvas, Rect bounds, int childIndex) {
        Drawable divider = getDivider();
        divider.setBounds(bounds);
        divider.draw(canvas);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & 255) {
            case 0:
                if (this.mMultiChoice && isInScrollRange(ev)) {
                    return true;
                }
            case 1:
                this.mMultiChoice = true;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean isInScrollRange(MotionEvent ev) {
        int curPosition = pointToPosition((int) ev.getX(), (int) ev.getY());
        int xRaw = (int) ev.getRawX();
        int[] location = new int[2];
        try {
            this.mCheckBox = (CheckBox) getChildAt(curPosition - getFirstVisiblePosition()).findViewById(ID_CHECKBOX);
            this.mCheckBox.getLocationOnScreen(location);
            int mLeftBorder = location[0] - this.mLeftOffset;
            int mRightBorder = location[0] + this.mRightOffset;
            if (this.mCheckBox.getVisibility() == 0 && xRaw > mLeftBorder && xRaw < mRightBorder && curPosition > getHeaderViewsCount() - 1 && curPosition < getCount() - getFooterViewsCount()) {
                return true;
            }
            if (ev.getActionMasked() == 0) {
                this.mMultiChoice = false;
            }
            return false;
        } catch (Exception e) {
            if (ev.getActionMasked() == 0) {
                this.mMultiChoice = false;
            }
            return false;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mMultiChoice && isInScrollRange(ev)) {
            int n = (int) ev.getY();
            this.mCurPosition = pointToPosition((int) ev.getX(), n);
            switch (ev.getActionMasked()) {
                case 0:
                    this.mFlag = true;
                    break;
                case 1:
                    this.mLastPosition = INVALID_SCROLL_CHOICE_POSITION;
                    break;
                case 2:
                    break;
            }
        }
        switch (ev.getAction() & 255) {
            case 1:
                removeCallbacks(this.mDelayedScroll);
                break;
            case 3:
                break;
        }
        this.mHasMoved = false;
        this.mUpScroll = true;
        this.mLastPosition = INVALID_SCROLL_CHOICE_POSITION;
        this.mFlag = false;
        this.mMultiChoice = true;
        return super.onTouchEvent(ev);
    }

    public void setScrollMultiChoiceListener(ScrollMultiChoiceListener listener) {
        this.mScrollMultiChoiceListener = listener;
    }

    public void setScrollDirection(int direction) {
        if (this.mScrollDirection != direction) {
            this.mScrollDirection = direction;
            layoutChildren();
        }
        this.mItemCount = getChildCount();
        this.mFirstPos = getFirstVisiblePosition();
        this.mLastPos = getLastVisiblePosition();
        if (this.mItemCount != 0) {
            this.mItemTop = getChildAt(0).getTop();
            this.mItemBottom = getChildAt(this.mItemCount - 1).getBottom();
        }
    }

    public void setScrollDirection(int direction, View pager) {
        setScrollDirection(direction);
        this.mViewPager = pager;
        recomputePositionInParent(this);
    }

    private void recomputePositionInParent(View view) {
        this.mItemTop += view.getTop();
        this.mItemBottom += view.getTop();
        if (this.mViewPager != null) {
            ViewParent viewParent = view.getParent();
            if (viewParent instanceof View) {
                View parent = (View) viewParent;
                if (!parent.equals(this.mViewPager)) {
                    recomputePositionInParent(parent);
                }
            }
        }
    }

    public void updateHorizontalPosition(float y, float offset, boolean toAppear) {
        this.mItemToAppear = toAppear;
        updateHorizontalPosition(y, offset);
    }

    public void updateHorizontalPosition(float y, float offset) {
        if (this.mItemCount != 0 && !this.mAnimationDisabled) {
            if (this.mPointY != y) {
                this.mPointY = y;
                this.mReferPosition = pointToPosition(getWidth() / 2, (int) (this.mPointY - ((float) this.mItemTop)));
            }
            if (this.mReferPosition == -1) {
                if (this.mPointY < ((float) this.mItemTop)) {
                    this.mReferPosition = this.mFirstPos;
                }
                if (this.mPointY > ((float) this.mItemTop)) {
                    this.mReferPosition = this.mLastPos;
                }
            }
            this.mPositionOffset = offset;
            layoutChildren();
        }
    }

    protected int calChildrenLeftPosition(int position, int childrenLeft) {
        int childLeft = childrenLeft;
        int count;
        if (this.mPositionOffset > 0.0f) {
            count = Math.max(Math.abs(this.mReferPosition - this.mFirstPos), Math.abs(this.mReferPosition - this.mLastPos));
            if (this.mScrollDirection == 2) {
                if (this.mItemToAppear) {
                    return childrenLeft + ((int) (((this.mAccelerateInterpolator.getInterpolation(((float) Math.abs(this.mReferPosition - position)) / ((float) this.mItemCount)) * (1.0f - this.mPositionOffset)) * ((float) getWidth())) * 4.0f));
                }
                if (this.mPositionOffset > 0.6f) {
                    this.mPositionOffset = 0.6f;
                }
                return childrenLeft - ((int) (((this.mDecelerateInterpolator.getInterpolation(1.0f - (((float) Math.abs(this.mReferPosition - position)) / ((float) count))) * this.mPositionOffset) * ((float) getWidth())) / 3.0f));
            } else if (this.mScrollDirection != 1) {
                return childLeft;
            } else {
                if (this.mItemToAppear) {
                    return childrenLeft - ((int) (((this.mAccelerateInterpolator.getInterpolation(((float) Math.abs(this.mReferPosition - position)) / ((float) this.mItemCount)) * this.mPositionOffset) * ((float) getWidth())) * 4.0f));
                }
                if (this.mPositionOffset < 0.4f) {
                    this.mPositionOffset = 0.4f;
                }
                return childrenLeft + ((int) (((this.mDecelerateInterpolator.getInterpolation(1.0f - (((float) Math.abs(this.mReferPosition - position)) / ((float) count))) * (1.0f - this.mPositionOffset)) * ((float) getWidth())) / 3.0f));
            }
        } else if (this.mPositionOffset >= 0.0f) {
            return childLeft;
        } else {
            count = Math.max(Math.abs(this.mReferPosition - this.mFirstPos), Math.abs(this.mReferPosition - this.mLastPos));
            if (this.mScrollDirection == 2) {
                return childrenLeft - ((int) (((this.mDecelerateInterpolator.getInterpolation(1.0f - (((float) Math.abs(this.mReferPosition - position)) / ((float) count))) * Math.abs(this.mPositionOffset)) * ((float) getWidth())) / 6.0f));
            }
            if (this.mScrollDirection == 1) {
                return childrenLeft + ((int) (((this.mDecelerateInterpolator.getInterpolation(1.0f - (((float) Math.abs(this.mReferPosition - position)) / ((float) count))) * Math.abs(this.mPositionOffset)) * ((float) getWidth())) / 6.0f));
            }
            return childLeft;
        }
    }
}
