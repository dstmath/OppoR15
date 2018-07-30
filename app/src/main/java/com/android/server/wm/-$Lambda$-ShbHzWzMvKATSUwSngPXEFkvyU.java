package com.android.server.wm;

import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU implements Consumer {
    public static final /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU $INST$0 = new -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU((byte) 0);
    public static final /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU $INST$1 = new -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU((byte) 1);
    public static final /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU $INST$2 = new -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU((byte) 2);
    public static final /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU $INST$3 = new -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU((byte) 3);
    public static final /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU $INST$4 = new -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU((byte) 4);
    public static final /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU $INST$5 = new -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU((byte) 5);
    public static final /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU $INST$6 = new -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU((byte) 6);
    public static final /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU $INST$7 = new -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU((byte) 7);
    public static final /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU $INST$8 = new -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU((byte) 8);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ void $m$0(Object arg0) {
        DetectBlack.lambda$-com_android_server_wm_DetectBlack_4182((WindowState) arg0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        ((WindowState) arg0).mWinAnimator.prepareSurfaceLocked(true);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        ((WindowState) arg0).mWinAnimator.disableSurfaceTrace();
    }

    private final /* synthetic */ void $m$3(Object arg0) {
        DisplayContent.lambda$-com_android_server_wm_DisplayContent_159009((WindowState) arg0);
    }

    private final /* synthetic */ void $m$4(Object arg0) {
        ((WindowState) arg0).resetDragResizingChangeReported();
    }

    private final /* synthetic */ void $m$5(Object arg0) {
        RootWindowContainer.lambda$-com_android_server_wm_RootWindowContainer_7587((WindowState) arg0);
    }

    private final /* synthetic */ void $m$6(Object arg0) {
        ((WindowState) arg0).mWinAnimator.resetDrawState();
    }

    private final /* synthetic */ void $m$7(Object arg0) {
        WindowLayersController.lambda$-com_android_server_wm_WindowLayersController_6502((WindowState) arg0);
    }

    private final /* synthetic */ void $m$8(Object arg0) {
        WindowManagerService.lambda$-com_android_server_wm_WindowManagerService_417664((WindowState) arg0);
    }

    private /* synthetic */ -$Lambda$-ShbHzWzMvKATSUwSngPXEFkvyU(byte b) {
        this.$id = b;
    }

    public final void accept(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(obj);
                return;
            case (byte) 1:
                $m$1(obj);
                return;
            case (byte) 2:
                $m$2(obj);
                return;
            case (byte) 3:
                $m$3(obj);
                return;
            case (byte) 4:
                $m$4(obj);
                return;
            case (byte) 5:
                $m$5(obj);
                return;
            case (byte) 6:
                $m$6(obj);
                return;
            case (byte) 7:
                $m$7(obj);
                return;
            case (byte) 8:
                $m$8(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
