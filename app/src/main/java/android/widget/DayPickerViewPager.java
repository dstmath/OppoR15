package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import com.android.internal.widget.ViewPager;
import com.android.internal.widget.ViewPager.LayoutParams;
import java.util.ArrayList;
import java.util.function.Predicate;

class DayPickerViewPager extends ViewPager {
    private final ArrayList<View> mMatchParentChildren;

    public DayPickerViewPager(Context context) {
        this(context, null);
    }

    public DayPickerViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DayPickerViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DayPickerViewPager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMatchParentChildren = new ArrayList(1);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        View child;
        LayoutParams lp;
        populate();
        int count = getChildCount();
        boolean measureMatchParentChildren = MeasureSpec.getMode(widthMeasureSpec) == 1073741824 ? MeasureSpec.getMode(heightMeasureSpec) != 1073741824 : true;
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;
        for (i = 0; i < count; i++) {
            child = getChildAt(i);
            if (child.getVisibility() != 8) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                lp = (LayoutParams) child.getLayoutParams();
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                childState = View.combineMeasuredStates(childState, child.getMeasuredState());
                if (measureMatchParentChildren && (lp.width == -1 || lp.height == -1)) {
                    this.mMatchParentChildren.add(child);
                }
            }
        }
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight = Math.max(maxHeight + (getPaddingTop() + getPaddingBottom()), getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        Drawable drawable = getForeground();
        if (drawable != null) {
            maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
            maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
        }
        setMeasuredDimension(View.resolveSizeAndState(maxWidth, widthMeasureSpec, childState), View.resolveSizeAndState(maxHeight, heightMeasureSpec, childState << 16));
        count = this.mMatchParentChildren.size();
        if (count > 1) {
            for (i = 0; i < count; i++) {
                int childWidthMeasureSpec;
                int childHeightMeasureSpec;
                child = (View) this.mMatchParentChildren.get(i);
                lp = (LayoutParams) child.getLayoutParams();
                if (lp.width == -1) {
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight(), 1073741824);
                } else {
                    childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(), lp.width);
                }
                if (lp.height == -1) {
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec((getMeasuredHeight() - getPaddingTop()) - getPaddingBottom(), 1073741824);
                } else {
                    childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(), lp.height);
                }
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
        this.mMatchParentChildren.clear();
    }

    protected <T extends View> T findViewByPredicateTraversal(Predicate<View> predicate, View childToSkip) {
        if (predicate.test(this)) {
            return this;
        }
        View v;
        View current = ((DayPickerPagerAdapter) getAdapter()).getView(getCurrent());
        if (!(current == childToSkip || current == null)) {
            v = current.findViewByPredicate(predicate);
            if (v != null) {
                return v;
            }
        }
        int len = getChildCount();
        for (int i = 0; i < len; i++) {
            View child = getChildAt(i);
            if (!(child == childToSkip || child == current)) {
                v = child.findViewByPredicate(predicate);
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }
}
