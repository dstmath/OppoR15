package com.android.server.wm;

import android.util.DisplayMetrics;
import com.android.server.wm.ColorLongshotWindowCompatible.WindowTraversalListener;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$bQ8IMZ_taBbscGhHwzSZOHc-GLw implements Consumer {
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;
    private final /* synthetic */ Object -$f3;
    private final /* synthetic */ Object -$f4;

    private final /* synthetic */ void $m$0(Object arg0) {
        ((ColorLongshotWindowCompatible) this.-$f1).lambda$-com_android_server_wm_ColorLongshotWindowCompatible_2048((WindowTraversalListener) this.-$f2, (DisplayMetrics) this.-$f3, (DisplayMetrics) this.-$f4, this.-$f0, (WindowState) arg0);
    }

    public /* synthetic */ -$Lambda$bQ8IMZ_taBbscGhHwzSZOHc-GLw(int i, Object obj, Object obj2, Object obj3, Object obj4) {
        this.-$f0 = i;
        this.-$f1 = obj;
        this.-$f2 = obj2;
        this.-$f3 = obj3;
        this.-$f4 = obj4;
    }

    public final void accept(Object obj) {
        $m$0(obj);
    }
}
