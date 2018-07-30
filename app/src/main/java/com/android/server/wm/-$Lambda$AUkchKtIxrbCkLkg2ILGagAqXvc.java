package com.android.server.wm;

import android.view.WindowManagerPolicy;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$AUkchKtIxrbCkLkg2ILGagAqXvc implements Consumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    /* renamed from: com.android.server.wm.-$Lambda$AUkchKtIxrbCkLkg2ILGagAqXvc$1 */
    final /* synthetic */ class AnonymousClass1 implements Consumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ boolean -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0(Object arg0) {
            DisplayContent.lambda$-com_android_server_wm_DisplayContent_131045((WindowManagerPolicy) this.-$f2, this.-$f0, this.-$f1, (WindowState) arg0);
        }

        private final /* synthetic */ void $m$1(Object arg0) {
            WindowManagerService.lambda$-com_android_server_wm_WindowManagerService_364928(this.-$f0, this.-$f1, (ArrayList) this.-$f2, (WindowState) arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, boolean z, boolean z2, Object obj) {
            this.$id = b;
            this.-$f0 = z;
            this.-$f1 = z2;
            this.-$f2 = obj;
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

    private final /* synthetic */ void $m$0(Object arg0) {
        DisplayContent.lambda$-com_android_server_wm_DisplayContent_130192((PrintWriter) this.-$f0, (String) this.-$f1, (int[]) this.-$f2, (WindowState) arg0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        ((WindowManagerService) this.-$f0).lambda$-com_android_server_wm_WindowManagerService_110876((String) this.-$f1, (Boolean) this.-$f2, (WindowState) arg0);
    }

    public /* synthetic */ -$Lambda$AUkchKtIxrbCkLkg2ILGagAqXvc(byte b, Object obj, Object obj2, Object obj3) {
        this.$id = b;
        this.-$f0 = obj;
        this.-$f1 = obj2;
        this.-$f2 = obj3;
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
