package com.android.org.conscrypt;

import java.security.Principal;
import java.security.cert.Certificate;
import java.util.HashMap;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.net.ssl.SSLSessionContext;
import javax.security.cert.X509Certificate;

final class SSLNullSession implements SSLSession, Cloneable {
    static final String INVALID_CIPHER = "SSL_NULL_WITH_NULL_NULL";
    private long creationTime;
    private long lastAccessedTime;
    private final HashMap<String, Object> values;

    private static class DefaultHolder {
        static final SSLNullSession NULL_SESSION = new SSLNullSession();

        private DefaultHolder() {
        }
    }

    /* synthetic */ SSLNullSession(SSLNullSession -this0) {
        this();
    }

    static SSLSession getNullSession() {
        return DefaultHolder.NULL_SESSION;
    }

    static boolean isNullSession(SSLSession session) {
        return session == DefaultHolder.NULL_SESSION;
    }

    private SSLNullSession() {
        this.values = new HashMap();
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
    }

    public int getApplicationBufferSize() {
        return 16384;
    }

    public String getCipherSuite() {
        return INVALID_CIPHER;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public byte[] getId() {
        return EmptyArray.BYTE;
    }

    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    public Certificate[] getLocalCertificates() {
        return null;
    }

    public Principal getLocalPrincipal() {
        return null;
    }

    public int getPacketBufferSize() {
        return 16709;
    }

    public X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        throw new SSLPeerUnverifiedException("No peer certificate");
    }

    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        throw new SSLPeerUnverifiedException("No peer certificate");
    }

    public String getPeerHost() {
        return null;
    }

    public int getPeerPort() {
        return -1;
    }

    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        throw new SSLPeerUnverifiedException("No peer certificate");
    }

    public String getProtocol() {
        return "NONE";
    }

    public SSLSessionContext getSessionContext() {
        return null;
    }

    public Object getValue(String name) {
        if (name != null) {
            return this.values.get(name);
        }
        throw new IllegalArgumentException("name == null");
    }

    public String[] getValueNames() {
        return (String[]) this.values.keySet().toArray(new String[this.values.size()]);
    }

    public void invalidate() {
    }

    public boolean isValid() {
        return false;
    }

    public void putValue(String name, Object value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name == null || value == null");
        }
        Object old = this.values.put(name, value);
        if (value instanceof SSLSessionBindingListener) {
            ((SSLSessionBindingListener) value).valueBound(new SSLSessionBindingEvent(this, name));
        }
        if (old instanceof SSLSessionBindingListener) {
            ((SSLSessionBindingListener) old).valueUnbound(new SSLSessionBindingEvent(this, name));
        }
    }

    public void removeValue(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name == null");
        }
        SSLSessionBindingListener old = this.values.remove(name);
        if (old instanceof SSLSessionBindingListener) {
            old.valueUnbound(new SSLSessionBindingEvent(this, name));
        }
    }
}
