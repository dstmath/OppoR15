package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.R;
import com.android.internal.widget.CachingIconView;
import java.util.ArrayList;

@RemoteView
public class NotificationHeaderView extends ViewGroup {
    public static final int NO_COLOR = 1;
    private boolean mAcceptAllTouches;
    private View mAppName;
    private Drawable mBackground;
    private final int mChildMinWidth;
    private final int mContentEndMargin;
    private boolean mEntireHeaderClickable;
    private ImageView mExpandButton;
    private OnClickListener mExpandClickListener;
    private boolean mExpandOnlyOnButton;
    private boolean mExpanded;
    private int mHeaderBackgroundHeight;
    private View mHeaderText;
    private CachingIconView mIcon;
    private int mIconColor;
    private View mInfo;
    private int mOriginalNotificationColor;
    private View mProfileBadge;
    ViewOutlineProvider mProvider;
    private boolean mShowExpandButtonAtEnd;
    private boolean mShowWorkBadgeAtEnd;
    private HeaderTouchListener mTouchListener;

    public class HeaderTouchListener implements OnTouchListener {
        private float mDownX;
        private float mDownY;
        private Rect mExpandButtonRect;
        private final ArrayList<Rect> mTouchRects = new ArrayList();
        private int mTouchSlop;
        private boolean mTrackGesture;

        public void bindTouchRects() {
            this.mTouchRects.clear();
            addRectAroundView(NotificationHeaderView.this.mIcon);
            this.mExpandButtonRect = addRectAroundView(NotificationHeaderView.this.mExpandButton);
            addWidthRect();
            this.mTouchSlop = ViewConfiguration.get(NotificationHeaderView.this.getContext()).getScaledTouchSlop();
        }

        private void addWidthRect() {
            Rect r = new Rect();
            r.top = 0;
            r.bottom = (int) (NotificationHeaderView.this.getResources().getDisplayMetrics().density * 32.0f);
            r.left = 0;
            r.right = NotificationHeaderView.this.getWidth();
            this.mTouchRects.add(r);
        }

        private Rect addRectAroundView(View view) {
            Rect r = getRectAroundView(view);
            this.mTouchRects.add(r);
            return r;
        }

        private Rect getRectAroundView(View view) {
            float size = 48.0f * NotificationHeaderView.this.getResources().getDisplayMetrics().density;
            Rect r = new Rect();
            if (view.getVisibility() == 8) {
                view = NotificationHeaderView.this.getFirstChildNotGone();
                r.left = (int) (((float) view.getLeft()) - (size / 2.0f));
            } else {
                r.left = (int) ((((float) (view.getLeft() + view.getRight())) / 2.0f) - (size / 2.0f));
            }
            r.top = (int) ((((float) (view.getTop() + view.getBottom())) / 2.0f) - (size / 2.0f));
            r.bottom = (int) (((float) r.top) + size);
            r.right = (int) (((float) r.left) + size);
            return r;
        }

        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getActionMasked() & 255) {
                case 0:
                    this.mTrackGesture = false;
                    if (isInside(x, y)) {
                        this.mDownX = x;
                        this.mDownY = y;
                        this.mTrackGesture = true;
                        return true;
                    }
                    break;
                case 1:
                    if (this.mTrackGesture) {
                        NotificationHeaderView.this.mExpandButton.performClick();
                        break;
                    }
                    break;
                case 2:
                    if (this.mTrackGesture && (Math.abs(this.mDownX - x) > ((float) this.mTouchSlop) || Math.abs(this.mDownY - y) > ((float) this.mTouchSlop))) {
                        this.mTrackGesture = false;
                        break;
                    }
            }
            return this.mTrackGesture;
        }

        private boolean isInside(float x, float y) {
            if (NotificationHeaderView.this.mAcceptAllTouches) {
                return true;
            }
            if (NotificationHeaderView.this.mExpandOnlyOnButton) {
                return this.mExpandButtonRect.contains((int) x, (int) y);
            }
            for (int i = 0; i < this.mTouchRects.size(); i++) {
                if (((Rect) this.mTouchRects.get(i)).contains((int) x, (int) y)) {
                    return true;
                }
            }
            return false;
        }
    }

    public NotificationHeaderView(Context context) {
        this(context, null);
    }

    public NotificationHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationHeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTouchListener = new HeaderTouchListener();
        this.mProvider = new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                if (NotificationHeaderView.this.mBackground != null) {
                    outline.setRect(0, 0, NotificationHeaderView.this.getWidth(), NotificationHeaderView.this.getHeight());
                    outline.setAlpha(1.0f);
                }
            }
        };
        Resources res = getResources();
        this.mChildMinWidth = res.getDimensionPixelSize(R.dimen.notification_header_shrink_min_width);
        this.mContentEndMargin = res.getDimensionPixelSize(R.dimen.notification_content_margin_end);
        this.mHeaderBackgroundHeight = res.getDimensionPixelSize(R.dimen.notification_header_background_height);
        this.mEntireHeaderClickable = res.getBoolean(R.bool.config_notificationHeaderClickableForExpand);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAppName = findViewById(R.id.app_name_text);
        this.mHeaderText = findViewById(R.id.header_text);
        this.mExpandButton = (ImageView) findViewById(R.id.expand_button);
        this.mIcon = (CachingIconView) findViewById(R.id.icon);
        this.mProfileBadge = findViewById(R.id.profile_badge);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int givenWidth = MeasureSpec.getSize(widthMeasureSpec);
        int givenHeight = MeasureSpec.getSize(heightMeasureSpec);
        int wrapContentWidthSpec = MeasureSpec.makeMeasureSpec(givenWidth, Integer.MIN_VALUE);
        int wrapContentHeightSpec = MeasureSpec.makeMeasureSpec(givenHeight, Integer.MIN_VALUE);
        int totalWidth = getPaddingStart() + getPaddingEnd();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                child.measure(ViewGroup.getChildMeasureSpec(wrapContentWidthSpec, lp.leftMargin + lp.rightMargin, lp.width), ViewGroup.getChildMeasureSpec(wrapContentHeightSpec, lp.topMargin + lp.bottomMargin, lp.height));
                totalWidth += (lp.leftMargin + lp.rightMargin) + child.getMeasuredWidth();
            }
        }
        if (totalWidth > givenWidth) {
            int overFlow = totalWidth - givenWidth;
            int appWidth = this.mAppName.getMeasuredWidth();
            if (overFlow > 0 && this.mAppName.getVisibility() != 8 && appWidth > this.mChildMinWidth) {
                int newSize = appWidth - Math.min(appWidth - this.mChildMinWidth, overFlow);
                this.mAppName.measure(MeasureSpec.makeMeasureSpec(newSize, Integer.MIN_VALUE), wrapContentHeightSpec);
                overFlow -= appWidth - newSize;
            }
            if (overFlow > 0 && this.mHeaderText.getVisibility() != 8) {
                this.mHeaderText.measure(MeasureSpec.makeMeasureSpec(Math.max(0, this.mHeaderText.getMeasuredWidth() - overFlow), Integer.MIN_VALUE), wrapContentHeightSpec);
            }
        }
        setMeasuredDimension(givenWidth, givenHeight);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingStart();
        int end = getMeasuredWidth();
        int childCount = getChildCount();
        int ownHeight = (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                int childHeight = child.getMeasuredHeight();
                MarginLayoutParams params = (MarginLayoutParams) child.getLayoutParams();
                left += params.getMarginStart();
                int right = left + child.getMeasuredWidth();
                int top = (int) (((float) getPaddingTop()) + (((float) (ownHeight - childHeight)) / 2.0f));
                int bottom = top + childHeight;
                int layoutLeft = left;
                int layoutRight = right;
                if (child == this.mExpandButton && this.mShowExpandButtonAtEnd) {
                    layoutRight = end - this.mContentEndMargin;
                    layoutLeft = layoutRight - child.getMeasuredWidth();
                    end = layoutLeft;
                }
                if (child == this.mProfileBadge) {
                    int paddingEnd = getPaddingEnd();
                    if (this.mShowWorkBadgeAtEnd) {
                        paddingEnd = this.mContentEndMargin;
                    }
                    layoutRight = end - paddingEnd;
                    layoutLeft = layoutRight - child.getMeasuredWidth();
                    end = layoutLeft;
                }
                if (getLayoutDirection() == 1) {
                    int ltrLeft = layoutLeft;
                    layoutLeft = getWidth() - layoutRight;
                    layoutRight = getWidth() - ltrLeft;
                }
                child.layout(layoutLeft, top, layoutRight, bottom);
                left = right + params.getMarginEnd();
            }
        }
        updateTouchListener();
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    public void setHeaderBackgroundDrawable(Drawable drawable) {
        if (drawable != null) {
            setWillNotDraw(false);
            this.mBackground = drawable;
            this.mBackground.setCallback(this);
            setOutlineProvider(this.mProvider);
        } else {
            setWillNotDraw(true);
            this.mBackground = null;
            setOutlineProvider(null);
        }
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        if (this.mBackground != null) {
            this.mBackground.setBounds(0, 0, getWidth(), getHeight());
            this.mBackground.draw(canvas);
        }
    }

    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mBackground;
    }

    protected void drawableStateChanged() {
        if (this.mBackground != null && this.mBackground.isStateful()) {
            this.mBackground.setState(getDrawableState());
        }
    }

    private void updateTouchListener() {
        if (this.mExpandClickListener != null) {
            this.mTouchListener.bindTouchRects();
        }
    }

    public void setOnClickListener(OnClickListener l) {
        OnTouchListener onTouchListener = null;
        this.mExpandClickListener = l;
        if (this.mExpandClickListener != null) {
            onTouchListener = this.mTouchListener;
        }
        setOnTouchListener(onTouchListener);
        this.mExpandButton.setOnClickListener(this.mExpandClickListener);
        updateTouchListener();
    }

    @RemotableViewMethod
    public void setOriginalIconColor(int color) {
        this.mIconColor = color;
    }

    public int getOriginalIconColor() {
        return this.mIconColor;
    }

    @RemotableViewMethod
    public void setOriginalNotificationColor(int color) {
        this.mOriginalNotificationColor = color;
    }

    public int getOriginalNotificationColor() {
        return this.mOriginalNotificationColor;
    }

    @RemotableViewMethod
    public void setExpanded(boolean expanded) {
        this.mExpanded = expanded;
        updateExpandButton();
    }

    private void updateExpandButton() {
        int drawableId;
        int contentDescriptionId;
        if (this.mExpanded) {
            drawableId = R.drawable.ic_collapse_notification;
            contentDescriptionId = R.string.expand_button_content_description_expanded;
        } else {
            drawableId = R.drawable.ic_expand_notification;
            contentDescriptionId = R.string.expand_button_content_description_collapsed;
        }
        this.mExpandButton.setImageDrawable(getContext().getDrawable(drawableId));
        this.mExpandButton.setColorFilter(this.mOriginalNotificationColor);
        this.mExpandButton.setContentDescription(this.mContext.getText(contentDescriptionId));
    }

    public void setShowWorkBadgeAtEnd(boolean showWorkBadgeAtEnd) {
        if (showWorkBadgeAtEnd != this.mShowWorkBadgeAtEnd) {
            setClipToPadding(showWorkBadgeAtEnd ^ 1);
            this.mShowWorkBadgeAtEnd = showWorkBadgeAtEnd;
        }
    }

    public void setShowExpandButtonAtEnd(boolean showExpandButtonAtEnd) {
        if (showExpandButtonAtEnd != this.mShowExpandButtonAtEnd) {
            setClipToPadding(showExpandButtonAtEnd ^ 1);
            this.mShowExpandButtonAtEnd = showExpandButtonAtEnd;
        }
    }

    public View getWorkProfileIcon() {
        return this.mProfileBadge;
    }

    public CachingIconView getIcon() {
        return this.mIcon;
    }

    private View getFirstChildNotGone() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return this;
    }

    public ImageView getExpandButton() {
        return this.mExpandButton;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public boolean isInTouchRect(float x, float y) {
        if (this.mExpandClickListener == null) {
            return false;
        }
        return this.mTouchListener.isInside(x, y);
    }

    @RemotableViewMethod
    public void setAcceptAllTouches(boolean acceptAllTouches) {
        if (this.mEntireHeaderClickable) {
            acceptAllTouches = true;
        }
        this.mAcceptAllTouches = acceptAllTouches;
    }

    @RemotableViewMethod
    public void setExpandOnlyOnButton(boolean expandOnlyOnButton) {
        this.mExpandOnlyOnButton = expandOnlyOnButton;
    }
}
