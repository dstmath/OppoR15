package com.android.server.ethernet;

import android.net.LinkProperties;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.util.concurrent.CountDownLatch;

final /* synthetic */ class -$Lambda$eRAo6Nl9jtYC3igbtEDMyHdAcyk implements Runnable {
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.ethernet.-$Lambda$eRAo6Nl9jtYC3igbtEDMyHdAcyk$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((com.android.server.ethernet.EthernetNetworkFactory.AnonymousClass2) this.-$f0).lambda$-com_android_server_ethernet_EthernetNetworkFactory$2_13202((LinkProperties) this.-$f1);
        }

        private final /* synthetic */ void $m$1() {
            ((com.android.server.ethernet.EthernetNetworkFactory.AnonymousClass2) this.-$f0).lambda$-com_android_server_ethernet_EthernetNetworkFactory$2_13016((LinkProperties) this.-$f1);
        }

        private final /* synthetic */ void $m$2() {
            ((com.android.server.ethernet.EthernetNetworkFactory.AnonymousClass2) this.-$f0).lambda$-com_android_server_ethernet_EthernetNetworkFactory$2_12831((LinkProperties) this.-$f1);
        }

        private final /* synthetic */ void $m$3() {
            ((InterfaceObserver) this.-$f0).lambda$-com_android_server_ethernet_EthernetNetworkFactory$InterfaceObserver_6624((String) this.-$f1);
        }

        private final /* synthetic */ void $m$4() {
            ((InterfaceObserver) this.-$f0).lambda$-com_android_server_ethernet_EthernetNetworkFactory$InterfaceObserver_6800((String) this.-$f1);
        }

        private final /* synthetic */ void $m$5() {
            EthernetNetworkFactory.lambda$-com_android_server_ethernet_EthernetNetworkFactory_18300((CountDownLatch) this.-$f0, (Runnable) this.-$f1);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
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
                case (byte) 4:
                    $m$4();
                    return;
                case (byte) 5:
                    $m$5();
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.server.ethernet.-$Lambda$eRAo6Nl9jtYC3igbtEDMyHdAcyk$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;

        private final /* synthetic */ void $m$0() {
            ((EthernetNetworkFactory) this.-$f0).lambda$-com_android_server_ethernet_EthernetNetworkFactory_18591((IndentingPrintWriter) this.-$f1, (FileDescriptor) this.-$f2, (String[]) this.-$f3);
        }

        public /* synthetic */ AnonymousClass2(Object obj, Object obj2, Object obj3, Object obj4) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
            this.-$f3 = obj4;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.ethernet.-$Lambda$eRAo6Nl9jtYC3igbtEDMyHdAcyk$3 */
    final /* synthetic */ class AnonymousClass3 implements Runnable {
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((InterfaceObserver) this.-$f1).lambda$-com_android_server_ethernet_EthernetNetworkFactory$InterfaceObserver_6445((String) this.-$f2, this.-$f0);
        }

        public /* synthetic */ AnonymousClass3(boolean z, Object obj, Object obj2) {
            this.-$f0 = z;
            this.-$f1 = obj;
            this.-$f2 = obj2;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0() {
        ((EthernetNetworkFactory) this.-$f0).lambda$-com_android_server_ethernet_EthernetNetworkFactory_15568();
    }

    public /* synthetic */ -$Lambda$eRAo6Nl9jtYC3igbtEDMyHdAcyk(Object obj) {
        this.-$f0 = obj;
    }

    public final void run() {
        $m$0();
    }
}
