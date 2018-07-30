package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork.getIdCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getAuthAlgCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getBssidCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapAltSubjectMatchCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapAnonymousIdentityCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapCACertCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapCAPathCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapClientCertCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapDomainSuffixMatchCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapEngineCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapEngineIDCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapIdentityCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapMethodCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapPasswordCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapPhase2MethodCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapPrivateKeyIdCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapSubjectMatchCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getGroupCipherCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getIdStrCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getKeyMgmtCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getPairwiseCipherCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getProtoCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getPskCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getPskPassphraseCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getRequirePmfCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getScanSsidCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getSsidCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getWepKeyCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getWepTxKeyIdxCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getWpsNfcConfigurationTokenCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.util.MutableBoolean;
import java.util.ArrayList;
import vendor.qti.hardware.wifi.supplicant.V1_0.ISupplicantVendorStaNetwork.getWapiCertSelCallback;
import vendor.qti.hardware.wifi.supplicant.V1_0.ISupplicantVendorStaNetwork.getWapiCertSelModeCallback;
import vendor.qti.hardware.wifi.supplicant.V1_0.ISupplicantVendorStaNetwork.getWapiPskCallback;
import vendor.qti.hardware.wifi.supplicant.V1_0.ISupplicantVendorStaNetwork.getWapiPskTypeCallback;

final /* synthetic */ class -$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE implements getIdCallback {
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$10 */
    final /* synthetic */ class AnonymousClass10 implements getEapEngineIDCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_106396((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass10(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$11 */
    final /* synthetic */ class AnonymousClass11 implements getEapIdentityCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ArrayList arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_95874((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass11(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
            $m$0(supplicantStatus, arrayList);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$12 */
    final /* synthetic */ class AnonymousClass12 implements getEapMethodCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_93870((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass12(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$13 */
    final /* synthetic */ class AnonymousClass13 implements getEapPasswordCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ArrayList arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_98424((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass13(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
            $m$0(supplicantStatus, arrayList);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$14 */
    final /* synthetic */ class AnonymousClass14 implements getEapPhase2MethodCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_94875((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass14(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$15 */
    final /* synthetic */ class AnonymousClass15 implements getEapPrivateKeyIdCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_102373((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass15(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$16 */
    final /* synthetic */ class AnonymousClass16 implements getEapSubjectMatchCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_103379((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass16(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$17 */
    final /* synthetic */ class AnonymousClass17 implements getGroupCipherCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_86868((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass17(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$18 */
    final /* synthetic */ class AnonymousClass18 implements getIdStrCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_108376((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass18(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$19 */
    final /* synthetic */ class AnonymousClass19 implements getKeyMgmtCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_83913((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass19(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$1 */
    final /* synthetic */ class AnonymousClass1 implements getAuthAlgCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_85863((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass1(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$20 */
    final /* synthetic */ class AnonymousClass20 implements getPairwiseCipherCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_87894((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass20(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$21 */
    final /* synthetic */ class AnonymousClass21 implements getProtoCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_84900((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass21(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$22 */
    final /* synthetic */ class AnonymousClass22 implements getPskCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, byte[] arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_89896((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass22(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
            $m$0(supplicantStatus, bArr);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$23 */
    final /* synthetic */ class AnonymousClass23 implements getPskPassphraseCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_88926((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass23(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$24 */
    final /* synthetic */ class AnonymousClass24 implements getRequirePmfCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, boolean arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_92876((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass24(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
            $m$0(supplicantStatus, z);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$25 */
    final /* synthetic */ class AnonymousClass25 implements getScanSsidCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, boolean arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_82927((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass25(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
            $m$0(supplicantStatus, z);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$26 */
    final /* synthetic */ class AnonymousClass26 implements getSsidCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ArrayList arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_80951((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass26(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
            $m$0(supplicantStatus, arrayList);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$27 */
    final /* synthetic */ class AnonymousClass27 implements getWepKeyCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ArrayList arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_90856((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass27(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
            $m$0(supplicantStatus, arrayList);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$28 */
    final /* synthetic */ class AnonymousClass28 implements getWepTxKeyIdxCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_91884((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass28(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$29 */
    final /* synthetic */ class AnonymousClass29 implements getWpsNfcConfigurationTokenCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ArrayList arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_121831((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass29(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
            $m$0(supplicantStatus, arrayList);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$2 */
    final /* synthetic */ class AnonymousClass2 implements getBssidCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, byte[] arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_81939((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass2(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
            $m$0(supplicantStatus, bArr);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$30 */
    final /* synthetic */ class AnonymousClass30 implements getWapiCertSelCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_79951((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass30(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$31 */
    final /* synthetic */ class AnonymousClass31 implements getWapiCertSelModeCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_78987((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass31(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$32 */
    final /* synthetic */ class AnonymousClass32 implements getWapiPskCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_77973((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass32(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$33 */
    final /* synthetic */ class AnonymousClass33 implements getWapiPskTypeCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_76974((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass33(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, int i) {
            $m$0(supplicantStatus, i);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$3 */
    final /* synthetic */ class AnonymousClass3 implements getEapAltSubjectMatchCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_104400((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass3(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$4 */
    final /* synthetic */ class AnonymousClass4 implements getEapAnonymousIdentityCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ArrayList arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_96912((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass4(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
            $m$0(supplicantStatus, arrayList);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$5 */
    final /* synthetic */ class AnonymousClass5 implements getEapCACertCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_99429((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass5(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$6 */
    final /* synthetic */ class AnonymousClass6 implements getEapCAPathCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_100391((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass6(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$7 */
    final /* synthetic */ class AnonymousClass7 implements getEapClientCertCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_101365((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass7(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$8 */
    final /* synthetic */ class AnonymousClass8 implements getEapDomainSuffixMatchCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_107389((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass8(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE$9 */
    final /* synthetic */ class AnonymousClass9 implements getEapEngineCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, boolean arg1) {
            ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_105397((MutableBoolean) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass9(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
            $m$0(supplicantStatus, z);
        }
    }

    private final /* synthetic */ void $m$0(SupplicantStatus arg0, int arg1) {
        ((SupplicantStaNetworkHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaNetworkHal_52606((MutableBoolean) this.-$f1, arg0, arg1);
    }

    public /* synthetic */ -$Lambda$red2_TYcnQD-fzDZf_XpXgVlYqE(Object obj, Object obj2) {
        this.-$f0 = obj;
        this.-$f1 = obj2;
    }

    public final void onValues(SupplicantStatus supplicantStatus, int i) {
        $m$0(supplicantStatus, i);
    }
}
