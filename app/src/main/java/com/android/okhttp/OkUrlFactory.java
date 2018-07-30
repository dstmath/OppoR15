package com.android.okhttp;

import com.android.okhttp.internal.URLFilter;
import com.android.okhttp.internal.huc.HttpURLConnectionImpl;
import com.android.okhttp.internal.huc.HttpsURLConnectionImpl;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public final class OkUrlFactory implements URLStreamHandlerFactory, Cloneable {
    private static Object mObject = null;
    private static Class<?> managerClass = null;
    private static Method method_boostEnableTimeoutMs = null;
    private static Method method_getLuckyMoneyURL = null;
    private static String sLuckyMoneyUrl = "";
    private final OkHttpClient client;
    private URLFilter urlFilter;

    public OkUrlFactory(OkHttpClient client) {
        this.client = client;
    }

    public OkHttpClient client() {
        return this.client;
    }

    void setUrlFilter(URLFilter filter) {
        this.urlFilter = filter;
    }

    public OkUrlFactory clone() {
        return new OkUrlFactory(this.client.clone());
    }

    public HttpURLConnection open(URL url) {
        return open(url, this.client.getProxy());
    }

    HttpURLConnection open(URL url, Proxy proxy) {
        String protocol = url.getProtocol();
        OkHttpClient copy = this.client.copyWithDefaults();
        copy.setProxy(proxy);
        startBoost(url.toString());
        if (protocol.equals("http")) {
            return new HttpURLConnectionImpl(url, copy, this.urlFilter);
        }
        if (protocol.equals("https")) {
            return new HttpsURLConnectionImpl(url, copy, this.urlFilter);
        }
        throw new IllegalArgumentException("Unexpected protocol: " + protocol);
    }

    public URLStreamHandler createURLStreamHandler(final String protocol) {
        if (protocol.equals("http") || (protocol.equals("https") ^ 1) == 0) {
            return new URLStreamHandler() {
                protected URLConnection openConnection(URL url) {
                    return OkUrlFactory.this.open(url);
                }

                protected URLConnection openConnection(URL url, Proxy proxy) {
                    return OkUrlFactory.this.open(url, proxy);
                }

                protected int getDefaultPort() {
                    if (protocol.equals("http")) {
                        return 80;
                    }
                    if (protocol.equals("https")) {
                        return 443;
                    }
                    throw new AssertionError();
                }
            };
        }
        return null;
    }

    private void startBoost(String url) {
        if (sLuckyMoneyUrl != null && "".equals(sLuckyMoneyUrl)) {
            try {
                if (managerClass == null) {
                    managerClass = Class.forName("com.oppo.luckymoney.LMManager");
                }
                if (mObject == null) {
                    mObject = managerClass.newInstance();
                }
                if (method_getLuckyMoneyURL == null) {
                    method_getLuckyMoneyURL = managerClass.getMethod("getLuckyMoneyInfo", new Class[]{Integer.TYPE});
                }
                sLuckyMoneyUrl = (String) method_getLuckyMoneyURL.invoke(mObject, new Object[]{Integer.valueOf(0)});
                System.out.println("sLuckyMoneyUrl = " + sLuckyMoneyUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if ((sLuckyMoneyUrl != null && url.contains(sLuckyMoneyUrl)) || url.contains("hongbao/img/hb.png")) {
            try {
                if (managerClass == null) {
                    managerClass = Class.forName("com.oppo.luckymoney.LMManager");
                }
                if (mObject == null) {
                    mObject = managerClass.newInstance();
                }
                if (method_boostEnableTimeoutMs == null) {
                    method_boostEnableTimeoutMs = managerClass.getMethod("enableBoost", new Class[]{Integer.TYPE, Integer.TYPE});
                }
                method_boostEnableTimeoutMs.invoke(mObject, new Object[]{Integer.valueOf(1500), Integer.valueOf(2014)});
                System.out.println("Start boost from OkHttpClient");
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }
}
