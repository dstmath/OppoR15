package com.android.server.wm;

import android.graphics.Rect;
import android.os.IBinder;
import android.util.MutableBoolean;
import android.view.SurfaceControl;
import android.view.WindowManagerPolicy.WindowState;
import com.android.internal.util.ToBooleanFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI implements Screenshoter {
    public static final /* synthetic */ -$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI $INST$0 = new -$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI((byte) 0);
    public static final /* synthetic */ -$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI $INST$1 = new -$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI((byte) 1);
    private final /* synthetic */ byte $id;

    /* renamed from: com.android.server.wm.-$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI$1 */
    final /* synthetic */ class AnonymousClass1 implements Predicate {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_128425((WindowState) this.-$f1, (WindowState) this.-$f2, (WindowState) arg0);
        }

        public /* synthetic */ AnonymousClass1(Object obj, Object obj2, Object obj3) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
        }

        public final boolean test(Object obj) {
            return $m$0(obj);
        }
    }

    /* renamed from: com.android.server.wm.-$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI$2 */
    final /* synthetic */ class AnonymousClass2 implements Consumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ int -$f1;

        private final /* synthetic */ void $m$0(Object arg0) {
            ((WindowState) arg0).mWinAnimator.seamlesslyRotateWindow(this.-$f0, this.-$f1);
        }

        private final /* synthetic */ void $m$1(Object arg0) {
            DisplayContent.lambda$-com_android_server_wm_DisplayContent_135981(this.-$f0, this.-$f1, (WindowState) arg0);
        }

        public /* synthetic */ AnonymousClass2(byte b, int i, int i2) {
            this.$id = b;
            this.-$f0 = i;
            this.-$f1 = i2;
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

    /* renamed from: com.android.server.wm.-$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI$3 */
    final /* synthetic */ class AnonymousClass3 implements Predicate {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ int -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return ((DisplayContent) this.-$f2).lambda$-com_android_server_wm_DisplayContent_115879(this.-$f0, this.-$f1, (WindowState) arg0);
        }

        public /* synthetic */ AnonymousClass3(int i, int i2, Object obj) {
            this.-$f0 = i;
            this.-$f1 = i2;
            this.-$f2 = obj;
        }

        public final boolean test(Object obj) {
            return $m$0(obj);
        }
    }

    /* renamed from: com.android.server.wm.-$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI$4 */
    final /* synthetic */ class AnonymousClass4 implements Consumer {
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(Object arg0) {
            ((DisplayContent) this.-$f1).lambda$-com_android_server_wm_DisplayContent_64353(this.-$f0, (WindowState) arg0);
        }

        public /* synthetic */ AnonymousClass4(boolean z, Object obj) {
            this.-$f0 = z;
            this.-$f1 = obj;
        }

        public final void accept(Object obj) {
            $m$0(obj);
        }
    }

    /* renamed from: com.android.server.wm.-$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI$5 */
    final /* synthetic */ class AnonymousClass5 implements ToBooleanFunction {
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ boolean -$f1;
        private final /* synthetic */ int -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;
        private final /* synthetic */ Object -$f5;
        private final /* synthetic */ Object -$f6;
        private final /* synthetic */ Object -$f7;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return ((DisplayContent) this.-$f3).lambda$-com_android_server_wm_DisplayContent_150793(this.-$f2, this.-$f0, (IBinder) this.-$f4, (MutableBoolean) this.-$f5, this.-$f1, (Rect) this.-$f6, (Rect) this.-$f7, (WindowState) arg0);
        }

        public /* synthetic */ AnonymousClass5(boolean z, boolean z2, int i, Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
            this.-$f0 = z;
            this.-$f1 = z2;
            this.-$f2 = i;
            this.-$f3 = obj;
            this.-$f4 = obj2;
            this.-$f5 = obj3;
            this.-$f6 = obj4;
            this.-$f7 = obj5;
        }

        public final boolean apply(Object obj) {
            return $m$0(obj);
        }
    }

    private final /* synthetic */ Object $m$0(Rect arg0, int arg1, int arg2, int arg3, int arg4, boolean arg5, int arg6) {
        return SurfaceControl.screenshot(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    private final /* synthetic */ Object $m$1(Rect arg0, int arg1, int arg2, int arg3, int arg4, boolean arg5, int arg6) {
        return SurfaceControl.screenshotToBuffer(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    private /* synthetic */ -$Lambda$OzPvdnGprtQoLZLCvw2GU8IaGyI(byte b) {
        this.$id = b;
    }

    public final Object screenshot(Rect rect, int i, int i2, int i3, int i4, boolean z, int i5) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(rect, i, i2, i3, i4, z, i5);
            case (byte) 1:
                return $m$1(rect, i, i2, i3, i4, z, i5);
            default:
                throw new AssertionError();
        }
    }
}
