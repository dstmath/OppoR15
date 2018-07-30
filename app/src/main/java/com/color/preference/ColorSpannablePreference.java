package com.color.preference;

import android.content.Context;
import android.preference.Preference;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class ColorSpannablePreference extends Preference {
    private final int DELAY_TIME = 70;

    public ColorSpannablePreference(Context context) {
        super(context);
    }

    public ColorSpannablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorSpannablePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ColorSpannablePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        final TextView summaryView = (TextView) view.findViewById(16908304);
        if (summaryView != null) {
            summaryView.setHighlightColor(17170445);
            summaryView.setMovementMethod(LinkMovementMethod.getInstance());
            summaryView.setOnTouchListener(new OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getActionMasked();
                    int start = summaryView.getSelectionStart();
                    int end = summaryView.getSelectionEnd();
                    int position = summaryView.getOffsetForPosition(event.getX(), event.getY());
                    boolean forbiddenUpdate = start == end || position <= start || position >= end;
                    switch (action) {
                        case 0:
                            if (!forbiddenUpdate) {
                                summaryView.setPressed(true);
                                summaryView.invalidate();
                                break;
                            }
                            return false;
                        case 1:
                        case 3:
                            summaryView.setPressed(false);
                            summaryView.postInvalidateDelayed(70);
                            break;
                    }
                    return false;
                }
            });
        }
    }
}
