package com.android.server.wm;

import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0 implements Predicate {
    public static final /* synthetic */ -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0 $INST$0 = new -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0((byte) 0);
    public static final /* synthetic */ -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0 $INST$1 = new -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0((byte) 1);
    public static final /* synthetic */ -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0 $INST$2 = new -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0((byte) 2);
    public static final /* synthetic */ -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0 $INST$3 = new -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0((byte) 3);
    public static final /* synthetic */ -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0 $INST$4 = new -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0((byte) 4);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ boolean $m$0(Object arg0) {
        return NonAppWindowContainers.lambda$-com_android_server_wm_DisplayContent$NonAppWindowContainers_177360((WindowState) arg0);
    }

    private final /* synthetic */ boolean $m$1(Object arg0) {
        return DisplayContent.lambda$-com_android_server_wm_DisplayContent_109349((WindowState) arg0);
    }

    private final /* synthetic */ boolean $m$2(Object arg0) {
        return DisplayContent.lambda$-com_android_server_wm_DisplayContent_135795((WindowState) arg0);
    }

    private final /* synthetic */ boolean $m$3(Object arg0) {
        return ((WindowState) arg0).mSeamlesslyRotated;
    }

    private final /* synthetic */ boolean $m$4(Object arg0) {
        return OppoWindowManagerService.lambda$-com_android_server_wm_OppoWindowManagerService_23016((WindowState) arg0);
    }

    private /* synthetic */ -$Lambda$6vWlz1P88lMkMG4g2BvX9ZYCNN0(byte b) {
        this.$id = b;
    }

    public final boolean test(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj);
            case (byte) 1:
                return $m$1(obj);
            case (byte) 2:
                return $m$2(obj);
            case (byte) 3:
                return $m$3(obj);
            case (byte) 4:
                return $m$4(obj);
            default:
                throw new AssertionError();
        }
    }
}
