package com.android.server.wifi.scanner;

import android.net.wifi.ScanResult;
import java.util.function.IntFunction;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$ckIrrmbHBOVG4LZY2cRLHtMBPV4 implements IntFunction {
    public static final /* synthetic */ -$Lambda$ckIrrmbHBOVG4LZY2cRLHtMBPV4 $INST$0 = new -$Lambda$ckIrrmbHBOVG4LZY2cRLHtMBPV4();

    /* renamed from: com.android.server.wifi.scanner.-$Lambda$ckIrrmbHBOVG4LZY2cRLHtMBPV4$1 */
    final /* synthetic */ class AnonymousClass1 implements Predicate {
        private final /* synthetic */ long -$f0;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return DefaultState.lambda$-com_android_server_wifi_scanner_WifiScanningServiceImpl$WifiSingleScanStateMachine$DefaultState_31149(this.-$f0, (ScanResult) arg0);
        }

        public /* synthetic */ AnonymousClass1(long j) {
            this.-$f0 = j;
        }

        public final boolean test(Object obj) {
            return $m$0(obj);
        }
    }

    private final /* synthetic */ Object $m$0(int arg0) {
        return new ScanResult[arg0];
    }

    private /* synthetic */ -$Lambda$ckIrrmbHBOVG4LZY2cRLHtMBPV4() {
    }

    public final Object apply(int i) {
        return $m$0(i);
    }
}
