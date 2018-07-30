package com.android.server.wm;

import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$MMmeixlGtfvS8ONgeh16gZdfDeA implements Predicate {
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ boolean $m$0(Object arg0) {
        return ((DetectBlack) this.-$f0).lambda$-com_android_server_wm_DetectBlack_1698((WindowSurfaceController) this.-$f1, (WindowState) arg0);
    }

    public /* synthetic */ -$Lambda$MMmeixlGtfvS8ONgeh16gZdfDeA(Object obj, Object obj2) {
        this.-$f0 = obj;
        this.-$f1 = obj2;
    }

    public final boolean test(Object obj) {
        return $m$0(obj);
    }
}
