package com.android.okhttp.internal.http;

import com.android.okhttp.CacheControl;
import com.android.okhttp.Headers;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class CacheStrategy {
    public final Response cacheResponse;
    public final Request networkRequest;

    public static class Factory {
        private int ageSeconds = -1;
        final Response cacheResponse;
        private String etag;
        private Date expires;
        private Date lastModified;
        private String lastModifiedString;
        final long nowMillis;
        private long receivedResponseMillis;
        final Request request;
        private long sentRequestMillis;
        private Date servedDate;
        private String servedDateString;

        public Factory(long nowMillis, Request request, Response cacheResponse) {
            this.nowMillis = nowMillis;
            this.request = request;
            this.cacheResponse = cacheResponse;
            if (cacheResponse != null) {
                Headers headers = cacheResponse.headers();
                int size = headers.size();
                for (int i = 0; i < size; i++) {
                    String fieldName = headers.name(i);
                    String value = headers.value(i);
                    if ("Date".equalsIgnoreCase(fieldName)) {
                        this.servedDate = HttpDate.parse(value);
                        this.servedDateString = value;
                    } else if ("Expires".equalsIgnoreCase(fieldName)) {
                        this.expires = HttpDate.parse(value);
                    } else if ("Last-Modified".equalsIgnoreCase(fieldName)) {
                        this.lastModified = HttpDate.parse(value);
                        this.lastModifiedString = value;
                    } else if ("ETag".equalsIgnoreCase(fieldName)) {
                        this.etag = value;
                    } else if ("Age".equalsIgnoreCase(fieldName)) {
                        this.ageSeconds = HeaderParser.parseSeconds(value, -1);
                    } else if (OkHeaders.SENT_MILLIS.equalsIgnoreCase(fieldName)) {
                        this.sentRequestMillis = Long.parseLong(value);
                    } else if (OkHeaders.RECEIVED_MILLIS.equalsIgnoreCase(fieldName)) {
                        this.receivedResponseMillis = Long.parseLong(value);
                    }
                }
            }
        }

        public CacheStrategy get() {
            CacheStrategy candidate = getCandidate();
            if (candidate.networkRequest == null || !this.request.cacheControl().onlyIfCached()) {
                return candidate;
            }
            return new CacheStrategy(null, null, null);
        }

        private com.android.okhttp.internal.http.CacheStrategy getCandidate() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r19_35 com.android.okhttp.internal.http.CacheStrategy) in PHI: PHI: (r19_36 com.android.okhttp.internal.http.CacheStrategy) = (r19_35 com.android.okhttp.internal.http.CacheStrategy), (r19_37 com.android.okhttp.internal.http.CacheStrategy) binds: {(r19_35 com.android.okhttp.internal.http.CacheStrategy)=B:50:0x018b, (r19_37 com.android.okhttp.internal.http.CacheStrategy)=B:58:0x01d1}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
	at java.lang.Iterable.forEach(Iterable.java:75)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
            /*
            r24 = this;
            r0 = r24;
            r0 = r0.cacheResponse;
            r19 = r0;
            if (r19 != 0) goto L_0x0018;
        L_0x0008:
            r19 = new com.android.okhttp.internal.http.CacheStrategy;
            r0 = r24;
            r0 = r0.request;
            r20 = r0;
            r21 = 0;
            r22 = 0;
            r19.<init>(r20, r21, r22);
            return r19;
        L_0x0018:
            r0 = r24;
            r0 = r0.request;
            r19 = r0;
            r19 = r19.isHttps();
            if (r19 == 0) goto L_0x0040;
        L_0x0024:
            r0 = r24;
            r0 = r0.cacheResponse;
            r19 = r0;
            r19 = r19.handshake();
            if (r19 != 0) goto L_0x0040;
        L_0x0030:
            r19 = new com.android.okhttp.internal.http.CacheStrategy;
            r0 = r24;
            r0 = r0.request;
            r20 = r0;
            r21 = 0;
            r22 = 0;
            r19.<init>(r20, r21, r22);
            return r19;
        L_0x0040:
            r0 = r24;
            r0 = r0.cacheResponse;
            r19 = r0;
            r0 = r24;
            r0 = r0.request;
            r20 = r0;
            r19 = com.android.okhttp.internal.http.CacheStrategy.isCacheable(r19, r20);
            if (r19 != 0) goto L_0x0062;
        L_0x0052:
            r19 = new com.android.okhttp.internal.http.CacheStrategy;
            r0 = r24;
            r0 = r0.request;
            r20 = r0;
            r21 = 0;
            r22 = 0;
            r19.<init>(r20, r21, r22);
            return r19;
        L_0x0062:
            r0 = r24;
            r0 = r0.request;
            r19 = r0;
            r9 = r19.cacheControl();
            r19 = r9.noCache();
            if (r19 != 0) goto L_0x007e;
        L_0x0072:
            r0 = r24;
            r0 = r0.request;
            r19 = r0;
            r19 = hasConditions(r19);
            if (r19 == 0) goto L_0x008e;
        L_0x007e:
            r19 = new com.android.okhttp.internal.http.CacheStrategy;
            r0 = r24;
            r0 = r0.request;
            r20 = r0;
            r21 = 0;
            r22 = 0;
            r19.<init>(r20, r21, r22);
            return r19;
        L_0x008e:
            r4 = r24.cacheResponseAge();
            r10 = r24.computeFreshnessLifetime();
            r19 = r9.maxAgeSeconds();
            r20 = -1;
            r0 = r19;
            r1 = r20;
            if (r0 == r1) goto L_0x00b7;
        L_0x00a2:
            r19 = java.util.concurrent.TimeUnit.SECONDS;
            r20 = r9.maxAgeSeconds();
            r0 = r20;
            r0 = (long) r0;
            r20 = r0;
            r20 = r19.toMillis(r20);
            r0 = r20;
            r10 = java.lang.Math.min(r10, r0);
        L_0x00b7:
            r14 = 0;
            r19 = r9.minFreshSeconds();
            r20 = -1;
            r0 = r19;
            r1 = r20;
            if (r0 == r1) goto L_0x00d4;
        L_0x00c5:
            r19 = java.util.concurrent.TimeUnit.SECONDS;
            r20 = r9.minFreshSeconds();
            r0 = r20;
            r0 = (long) r0;
            r20 = r0;
            r14 = r19.toMillis(r20);
        L_0x00d4:
            r12 = 0;
            r0 = r24;
            r0 = r0.cacheResponse;
            r19 = r0;
            r18 = r19.cacheControl();
            r19 = r18.mustRevalidate();
            if (r19 != 0) goto L_0x0101;
        L_0x00e6:
            r19 = r9.maxStaleSeconds();
            r20 = -1;
            r0 = r19;
            r1 = r20;
            if (r0 == r1) goto L_0x0101;
        L_0x00f2:
            r19 = java.util.concurrent.TimeUnit.SECONDS;
            r20 = r9.maxStaleSeconds();
            r0 = r20;
            r0 = (long) r0;
            r20 = r0;
            r12 = r19.toMillis(r20);
        L_0x0101:
            r19 = r18.noCache();
            if (r19 != 0) goto L_0x015f;
        L_0x0107:
            r20 = r4 + r14;
            r22 = r10 + r12;
            r19 = (r20 > r22 ? 1 : (r20 == r22 ? 0 : -1));
            if (r19 >= 0) goto L_0x015f;
        L_0x010f:
            r0 = r24;
            r0 = r0.cacheResponse;
            r19 = r0;
            r6 = r19.newBuilder();
            r20 = r4 + r14;
            r19 = (r20 > r10 ? 1 : (r20 == r10 ? 0 : -1));
            if (r19 < 0) goto L_0x012c;
        L_0x011f:
            r19 = "Warning";
            r20 = "110 HttpURLConnection \"Response is stale\"";
            r0 = r19;
            r1 = r20;
            r6.addHeader(r0, r1);
        L_0x012c:
            r16 = 86400000; // 0x5265c00 float:7.82218E-36 double:4.2687272E-316;
            r20 = 86400000; // 0x5265c00 float:7.82218E-36 double:4.2687272E-316;
            r19 = (r4 > r20 ? 1 : (r4 == r20 ? 0 : -1));
            if (r19 <= 0) goto L_0x0149;
        L_0x0136:
            r19 = r24.isFreshnessLifetimeHeuristic();
            if (r19 == 0) goto L_0x0149;
        L_0x013c:
            r19 = "Warning";
            r20 = "113 HttpURLConnection \"Heuristic expiration\"";
            r0 = r19;
            r1 = r20;
            r6.addHeader(r0, r1);
        L_0x0149:
            r19 = new com.android.okhttp.internal.http.CacheStrategy;
            r20 = r6.build();
            r21 = 0;
            r22 = 0;
            r0 = r19;
            r1 = r21;
            r2 = r20;
            r3 = r22;
            r0.<init>(r1, r2, r3);
            return r19;
        L_0x015f:
            r0 = r24;
            r0 = r0.request;
            r19 = r0;
            r8 = r19.newBuilder();
            r0 = r24;
            r0 = r0.etag;
            r19 = r0;
            if (r19 == 0) goto L_0x019f;
        L_0x0171:
            r19 = "If-None-Match";
            r0 = r24;
            r0 = r0.etag;
            r20 = r0;
            r0 = r19;
            r1 = r20;
            r8.header(r0, r1);
        L_0x0181:
            r7 = r8.build();
            r19 = hasConditions(r7);
            if (r19 == 0) goto L_0x01d1;
        L_0x018b:
            r19 = new com.android.okhttp.internal.http.CacheStrategy;
            r0 = r24;
            r0 = r0.cacheResponse;
            r20 = r0;
            r21 = 0;
            r0 = r19;
            r1 = r20;
            r2 = r21;
            r0.<init>(r7, r1, r2);
        L_0x019e:
            return r19;
        L_0x019f:
            r0 = r24;
            r0 = r0.lastModified;
            r19 = r0;
            if (r19 == 0) goto L_0x01b8;
        L_0x01a7:
            r19 = "If-Modified-Since";
            r0 = r24;
            r0 = r0.lastModifiedString;
            r20 = r0;
            r0 = r19;
            r1 = r20;
            r8.header(r0, r1);
            goto L_0x0181;
        L_0x01b8:
            r0 = r24;
            r0 = r0.servedDate;
            r19 = r0;
            if (r19 == 0) goto L_0x0181;
        L_0x01c0:
            r19 = "If-Modified-Since";
            r0 = r24;
            r0 = r0.servedDateString;
            r20 = r0;
            r0 = r19;
            r1 = r20;
            r8.header(r0, r1);
            goto L_0x0181;
        L_0x01d1:
            r19 = new com.android.okhttp.internal.http.CacheStrategy;
            r20 = 0;
            r21 = 0;
            r0 = r19;
            r1 = r20;
            r2 = r21;
            r0.<init>(r7, r1, r2);
            goto L_0x019e;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.okhttp.internal.http.CacheStrategy.Factory.getCandidate():com.android.okhttp.internal.http.CacheStrategy");
        }

        private long computeFreshnessLifetime() {
            long j = 0;
            CacheControl responseCaching = this.cacheResponse.cacheControl();
            if (responseCaching.maxAgeSeconds() != -1) {
                return TimeUnit.SECONDS.toMillis((long) responseCaching.maxAgeSeconds());
            }
            long servedMillis;
            long delta;
            if (this.expires != null) {
                if (this.servedDate != null) {
                    servedMillis = this.servedDate.getTime();
                } else {
                    servedMillis = this.receivedResponseMillis;
                }
                delta = this.expires.getTime() - servedMillis;
                if (delta <= 0) {
                    delta = 0;
                }
                return delta;
            } else if (this.lastModified == null || this.cacheResponse.request().httpUrl().query() != null) {
                return 0;
            } else {
                if (this.servedDate != null) {
                    servedMillis = this.servedDate.getTime();
                } else {
                    servedMillis = this.sentRequestMillis;
                }
                delta = servedMillis - this.lastModified.getTime();
                if (delta > 0) {
                    j = delta / 10;
                }
                return j;
            }
        }

        private long cacheResponseAge() {
            long apparentReceivedAge;
            long receivedAge;
            if (this.servedDate != null) {
                apparentReceivedAge = Math.max(0, this.receivedResponseMillis - this.servedDate.getTime());
            } else {
                apparentReceivedAge = 0;
            }
            if (this.ageSeconds != -1) {
                receivedAge = Math.max(apparentReceivedAge, TimeUnit.SECONDS.toMillis((long) this.ageSeconds));
            } else {
                receivedAge = apparentReceivedAge;
            }
            return (receivedAge + (this.receivedResponseMillis - this.sentRequestMillis)) + (this.nowMillis - this.receivedResponseMillis);
        }

        private boolean isFreshnessLifetimeHeuristic() {
            return this.cacheResponse.cacheControl().maxAgeSeconds() == -1 && this.expires == null;
        }

        private static boolean hasConditions(Request request) {
            return (request.header("If-Modified-Since") == null && request.header("If-None-Match") == null) ? false : true;
        }
    }

    /* synthetic */ CacheStrategy(Request networkRequest, Response cacheResponse, CacheStrategy -this2) {
        this(networkRequest, cacheResponse);
    }

    private CacheStrategy(Request networkRequest, Response cacheResponse) {
        this.networkRequest = networkRequest;
        this.cacheResponse = cacheResponse;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isCacheable(Response response, Request request) {
        boolean z = false;
        switch (response.code()) {
            case 200:
            case 203:
            case 204:
            case 300:
            case 301:
            case StatusLine.HTTP_PERM_REDIRECT /*308*/:
            case 404:
            case 405:
            case 410:
            case 414:
            case 501:
                break;
            case 302:
            case StatusLine.HTTP_TEMP_REDIRECT /*307*/:
                if (response.header("Expires") == null) {
                    if (response.cacheControl().maxAgeSeconds() == -1) {
                        if (!response.cacheControl().isPublic()) {
                            break;
                        }
                    }
                }
                break;
        }
    }
}
