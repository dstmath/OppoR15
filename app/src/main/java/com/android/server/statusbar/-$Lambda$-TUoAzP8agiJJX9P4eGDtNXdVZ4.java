package com.android.server.statusbar;

import com.android.server.power.ShutdownThread;

final /* synthetic */ class -$Lambda$-TUoAzP8agiJJX9P4eGDtNXdVZ4 implements Runnable {
    public static final /* synthetic */ -$Lambda$-TUoAzP8agiJJX9P4eGDtNXdVZ4 $INST$0 = new -$Lambda$-TUoAzP8agiJJX9P4eGDtNXdVZ4();

    /* renamed from: com.android.server.statusbar.-$Lambda$-TUoAzP8agiJJX9P4eGDtNXdVZ4$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((StatusBarManagerService) this.-$f0).lambda$-com_android_server_statusbar_StatusBarManagerService_25681();
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.statusbar.-$Lambda$-TUoAzP8agiJJX9P4eGDtNXdVZ4$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ boolean -$f0;

        private final /* synthetic */ void $m$0() {
            StatusBarManagerService.lambda$-com_android_server_statusbar_StatusBarManagerService_27798(this.-$f0);
        }

        public /* synthetic */ AnonymousClass2(boolean z) {
            this.-$f0 = z;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0() {
        ShutdownThread.shutdown(StatusBarManagerService.getUiContext(), "userrequested", false);
    }

    private /* synthetic */ -$Lambda$-TUoAzP8agiJJX9P4eGDtNXdVZ4() {
    }

    public final void run() {
        $m$0();
    }
}
