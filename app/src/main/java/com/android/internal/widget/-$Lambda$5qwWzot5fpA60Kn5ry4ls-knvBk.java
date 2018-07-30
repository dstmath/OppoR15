package com.android.internal.widget;

import com.android.internal.widget.ColorFloatingToolbarPopup.AnonymousClass10;
import com.android.internal.widget.FloatingToolbar.FloatingToolbarPopup.AnonymousClass13;
import com.android.internal.widget.LockPatternUtils.CheckCredentialProgressCallback;
import com.android.internal.widget.SwipeDismissLayout.AnonymousClass1;

final /* synthetic */ class -$Lambda$5qwWzot5fpA60Kn5ry4ls-knvBk implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((AnonymousClass10) this.-$f0).lambda$-com_android_internal_widget_ColorFloatingToolbarPopup$10_68393();
    }

    private final /* synthetic */ void $m$1() {
        ((AnonymousClass13) this.-$f0).lambda$-com_android_internal_widget_FloatingToolbar$FloatingToolbarPopup$13_69244();
    }

    private final /* synthetic */ void $m$2() {
        ((CheckCredentialProgressCallback) this.-$f0).onEarlyMatched();
    }

    private final /* synthetic */ void $m$3() {
        ((AnonymousClass1) this.-$f0).lambda$-com_android_internal_widget_SwipeDismissLayout$1_4841();
    }

    public /* synthetic */ -$Lambda$5qwWzot5fpA60Kn5ry4ls-knvBk(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void run() {
        switch (this.$id) {
            case (byte) 0:
                $m$0();
                return;
            case (byte) 1:
                $m$1();
                return;
            case (byte) 2:
                $m$2();
                return;
            case (byte) 3:
                $m$3();
                return;
            default:
                throw new AssertionError();
        }
    }
}
