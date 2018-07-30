package com.mediatek.server.wifi;

import android.os.IHwBinder.DeathRecipient;
import java.util.ArrayList;
import vendor.oppo.hardware.wifi.hostapd.V1_0.HostapdStatus;
import vendor.oppo.hardware.wifi.hostapd.V1_0.IHostapd.getInterfaceCallback;
import vendor.oppo.hardware.wifi.hostapd.V1_0.IHostapd.listInterfacesCallback;
import vendor.oppo.hardware.wifi.hostapd.V1_0.IHostapdIface;
import vendor.oppo.hardware.wifi.hostapd.V1_0.IHostapdIface.startWpsCheckPinCallback;

final /* synthetic */ class -$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698 implements DeathRecipient {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.mediatek.server.wifi.-$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698$1 */
    final /* synthetic */ class AnonymousClass1 implements getInterfaceCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(HostapdStatus arg0, IHostapdIface arg1) {
            HostapdIfaceHal.lambda$-com_mediatek_server_wifi_HostapdIfaceHal_9659((Mutable) this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(HostapdStatus hostapdStatus, IHostapdIface iHostapdIface) {
            $m$0(hostapdStatus, iHostapdIface);
        }
    }

    /* renamed from: com.mediatek.server.wifi.-$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698$2 */
    final /* synthetic */ class AnonymousClass2 implements listInterfacesCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(HostapdStatus arg0, ArrayList arg1) {
            HostapdIfaceHal.lambda$-com_mediatek_server_wifi_HostapdIfaceHal_8681((ArrayList) this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass2(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(HostapdStatus hostapdStatus, ArrayList arrayList) {
            $m$0(hostapdStatus, arrayList);
        }
    }

    /* renamed from: com.mediatek.server.wifi.-$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698$3 */
    final /* synthetic */ class AnonymousClass3 implements startWpsCheckPinCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(HostapdStatus arg0, String arg1) {
            ((HostapdIfaceHal) this.-$f0).lambda$-com_mediatek_server_wifi_HostapdIfaceHal_14681((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass3(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(HostapdStatus hostapdStatus, String str) {
            $m$0(hostapdStatus, str);
        }
    }

    private final /* synthetic */ void $m$0(long arg0) {
        ((HostapdIfaceHal) this.-$f0).lambda$-com_mediatek_server_wifi_HostapdIfaceHal_2856(arg0);
    }

    private final /* synthetic */ void $m$1(long arg0) {
        ((HostapdIfaceHal) this.-$f0).lambda$-com_mediatek_server_wifi_HostapdIfaceHal_3244(arg0);
    }

    public /* synthetic */ -$Lambda$wCPAxIqdWjia8qF1SPdkfZrm698(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void serviceDied(long j) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(j);
                return;
            case (byte) 1:
                $m$1(j);
                return;
            default:
                throw new AssertionError();
        }
    }
}
