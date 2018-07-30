package com.android.internal.widget;

import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.ViewTreeObserver.OnComputeInternalInsetsListener;

final /* synthetic */ class -$Lambda$Go7JgWogXNLsQcgF5KW1dT_OHSc implements OnComputeInternalInsetsListener {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0(InternalInsetsInfo arg0) {
        ((ColorFloatingToolbarPopup) this.-$f0).lambda$-com_android_internal_widget_ColorFloatingToolbarPopup_5455(arg0);
    }

    private final /* synthetic */ void $m$1(InternalInsetsInfo arg0) {
        ((FloatingToolbarPopup) this.-$f0).lambda$-com_android_internal_widget_FloatingToolbar$FloatingToolbarPopup_15267(arg0);
    }

    public /* synthetic */ -$Lambda$Go7JgWogXNLsQcgF5KW1dT_OHSc(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void onComputeInternalInsets(InternalInsetsInfo internalInsetsInfo) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(internalInsetsInfo);
                return;
            case (byte) 1:
                $m$1(internalInsetsInfo);
                return;
            default:
                throw new AssertionError();
        }
    }
}
