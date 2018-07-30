package com.android.server.wm;

import java.util.ArrayList;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$_eaSO6hSZOKSnr04PN_UCoMXgDU implements Consumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0(Object arg0) {
        ((ColorLongshotWindowCompatible) this.-$f1).lambda$-com_android_server_wm_ColorLongshotWindowCompatible_5973(this.-$f0, (WindowState) arg0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        OppoWindowManagerService.lambda$-com_android_server_wm_OppoWindowManagerService_15013(this.-$f0, (ArrayList) this.-$f1, (WindowState) arg0);
    }

    public /* synthetic */ -$Lambda$_eaSO6hSZOKSnr04PN_UCoMXgDU(byte b, int i, Object obj) {
        this.$id = b;
        this.-$f0 = i;
        this.-$f1 = obj;
    }

    public final void accept(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(obj);
                return;
            case (byte) 1:
                $m$1(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
