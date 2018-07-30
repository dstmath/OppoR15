package com.color.widget;

import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

class ColorKeyBoardView$2 extends AccessibilityDelegate {
    final /* synthetic */ ColorKeyBoardView this$0;

    ColorKeyBoardView$2(ColorKeyBoardView this$0) {
        this.this$0 = this$0;
    }

    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        if (ColorKeyBoardView.-get1(this.this$0) != null) {
            info.setContentDescription(ColorKeyBoardView.-get1(this.this$0));
        }
        info.setClassName(Button.class.getName());
    }
}
