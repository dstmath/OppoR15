package com.android.server;

import android.content.Context;
import com.android.server.input.InputManagerService;
import com.android.server.media.MediaRouterService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.oppo.OppoService;
import com.android.server.oppo.OppoUsageService;
import com.android.server.wm.WindowManagerService;
import com.oppo.media.OppoMultimediaService;
import com.oppo.roundcorner.OppoRoundConerService;

final /* synthetic */ class -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo implements Runnable {
    public static final /* synthetic */ -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo $INST$0 = new -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo((byte) 0);
    public static final /* synthetic */ -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo $INST$1 = new -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo((byte) 1);
    public static final /* synthetic */ -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo $INST$2 = new -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo((byte) 2);
    public static final /* synthetic */ -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo $INST$3 = new -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo((byte) 3);
    private final /* synthetic */ byte $id;

    /* renamed from: com.android.server.-$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f10;
        private final /* synthetic */ Object -$f11;
        private final /* synthetic */ Object -$f12;
        private final /* synthetic */ Object -$f13;
        private final /* synthetic */ Object -$f14;
        private final /* synthetic */ Object -$f15;
        private final /* synthetic */ Object -$f16;
        private final /* synthetic */ Object -$f17;
        private final /* synthetic */ Object -$f18;
        private final /* synthetic */ Object -$f19;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;
        private final /* synthetic */ Object -$f5;
        private final /* synthetic */ Object -$f6;
        private final /* synthetic */ Object -$f7;
        private final /* synthetic */ Object -$f8;
        private final /* synthetic */ Object -$f9;

        private final /* synthetic */ void $m$0() {
            ((SystemServer) this.-$f0).lambda$-com_android_server_SystemServer_103579((Context) this.-$f1, (WindowManagerService) this.-$f2, (NetworkScoreService) this.-$f3, (NetworkManagementService) this.-$f4, (NetworkPolicyManagerService) this.-$f5, (NetworkStatsService) this.-$f6, (ConnectivityService) this.-$f7, (LocationManagerService) this.-$f8, (CountryDetectorService) this.-$f9, (NetworkTimeUpdateService) this.-$f10, (OppoMultimediaService) this.-$f11, (OppoRoundConerService) this.-$f12, (CommonTimeManagementService) this.-$f13, (InputManagerService) this.-$f14, (TelephonyRegistry) this.-$f15, (MediaRouterService) this.-$f16, (OppoUsageService) this.-$f17, (MmsServiceBroker) this.-$f18, (OppoService) this.-$f19);
        }

        public /* synthetic */ AnonymousClass1(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11, Object obj12, Object obj13, Object obj14, Object obj15, Object obj16, Object obj17, Object obj18, Object obj19, Object obj20) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
            this.-$f3 = obj4;
            this.-$f4 = obj5;
            this.-$f5 = obj6;
            this.-$f6 = obj7;
            this.-$f7 = obj8;
            this.-$f8 = obj9;
            this.-$f9 = obj10;
            this.-$f10 = obj11;
            this.-$f11 = obj12;
            this.-$f12 = obj13;
            this.-$f13 = obj14;
            this.-$f14 = obj15;
            this.-$f15 = obj16;
            this.-$f16 = obj17;
            this.-$f17 = obj18;
            this.-$f18 = obj19;
            this.-$f19 = obj20;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.-$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((SystemServer) this.-$f0).lambda$-com_android_server_SystemServer_104484();
        }

        public /* synthetic */ AnonymousClass2(Object obj) {
            this.-$f0 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0() {
        SystemConfig.getInstance();
    }

    private final /* synthetic */ void $m$1() {
        SystemServer.lambda$-com_android_server_SystemServer_37102();
    }

    private final /* synthetic */ void $m$2() {
        SystemServer.lambda$-com_android_server_SystemServer_42990();
    }

    private final /* synthetic */ void $m$3() {
        SystemServer.lambda$-com_android_server_SystemServer_47954();
    }

    private /* synthetic */ -$Lambda$T7cKu_OKm_Fk2kBNthmo_uUJTSo(byte b) {
        this.$id = b;
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
