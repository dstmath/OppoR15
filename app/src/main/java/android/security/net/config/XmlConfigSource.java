package android.security.net.config;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.security.net.config.NetworkSecurityConfig.Builder;
import android.telephony.SubscriptionPlan;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Pair;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlConfigSource implements ConfigSource {
    private static final int CONFIG_BASE = 0;
    private static final int CONFIG_DEBUG = 2;
    private static final int CONFIG_DOMAIN = 1;
    private Context mContext;
    private final boolean mDebugBuild;
    private NetworkSecurityConfig mDefaultConfig;
    private Set<Pair<Domain, NetworkSecurityConfig>> mDomainMap;
    private boolean mInitialized;
    private final Object mLock;
    private final int mResourceId;
    private final int mTargetSandboxVesrsion;
    private final int mTargetSdkVersion;

    public static class ParserException extends Exception {
        public ParserException(XmlPullParser parser, String message, Throwable cause) {
            super(message + " at: " + parser.getPositionDescription(), cause);
        }

        public ParserException(XmlPullParser parser, String message) {
            this(parser, message, null);
        }
    }

    public XmlConfigSource(Context context, int resourceId) {
        this(context, resourceId, false);
    }

    public XmlConfigSource(Context context, int resourceId, boolean debugBuild) {
        this(context, resourceId, debugBuild, 10000);
    }

    public XmlConfigSource(Context context, int resourceId, boolean debugBuild, int targetSdkVersion) {
        this(context, resourceId, debugBuild, targetSdkVersion, 1);
    }

    public XmlConfigSource(Context context, int resourceId, boolean debugBuild, int targetSdkVersion, int targetSandboxVesrsion) {
        this.mLock = new Object();
        this.mResourceId = resourceId;
        this.mContext = context;
        this.mDebugBuild = debugBuild;
        this.mTargetSdkVersion = targetSdkVersion;
        this.mTargetSandboxVesrsion = targetSandboxVesrsion;
    }

    public Set<Pair<Domain, NetworkSecurityConfig>> getPerDomainConfigs() {
        ensureInitialized();
        return this.mDomainMap;
    }

    public NetworkSecurityConfig getDefaultConfig() {
        ensureInitialized();
        return this.mDefaultConfig;
    }

    private static final String getConfigString(int configType) {
        switch (configType) {
            case 0:
                return "base-config";
            case 1:
                return "domain-config";
            case 2:
                return "debug-overrides";
            default:
                throw new IllegalArgumentException("Unknown config type: " + configType);
        }
    }

    private void ensureInitialized() {
        XmlResourceParser xmlResourceParser;
        Throwable th;
        Throwable th2 = null;
        synchronized (this.mLock) {
            if (this.mInitialized) {
                return;
            }
            xmlResourceParser = null;
            try {
                xmlResourceParser = this.mContext.getResources().getXml(this.mResourceId);
                parseNetworkSecurityConfig(xmlResourceParser);
                this.mContext = null;
                this.mInitialized = true;
                if (xmlResourceParser != null) {
                    try {
                        xmlResourceParser.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 != null) {
                    try {
                        throw th2;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse XML configuration from " + this.mContext.getResources().getResourceEntryName(this.mResourceId), e);
                    }
                }
                return;
            } catch (Throwable th22) {
                Throwable th4 = th22;
                th22 = th;
                th = th4;
            }
        }
        if (xmlResourceParser != null) {
            try {
                xmlResourceParser.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    private Pin parsePin(XmlResourceParser parser) throws IOException, XmlPullParserException, ParserException {
        String digestAlgorithm = parser.getAttributeValue(null, "digest");
        if (!Pin.isSupportedDigestAlgorithm(digestAlgorithm)) {
            throw new ParserException(parser, "Unsupported pin digest algorithm: " + digestAlgorithm);
        } else if (parser.next() != 4) {
            throw new ParserException(parser, "Missing pin digest");
        } else {
            try {
                byte[] decodedDigest = Base64.decode(parser.getText().trim(), 0);
                int expectedLength = Pin.getDigestLength(digestAlgorithm);
                if (decodedDigest.length != expectedLength) {
                    throw new ParserException(parser, "digest length " + decodedDigest.length + " does not match expected length for " + digestAlgorithm + " of " + expectedLength);
                } else if (parser.next() == 3) {
                    return new Pin(digestAlgorithm, decodedDigest);
                } else {
                    throw new ParserException(parser, "pin contains additional elements");
                }
            } catch (IllegalArgumentException e) {
                throw new ParserException(parser, "Invalid pin digest", e);
            }
        }
    }

    private PinSet parsePinSet(XmlResourceParser parser) throws IOException, XmlPullParserException, ParserException {
        String expirationDate = parser.getAttributeValue(null, "expiration");
        long expirationTimestampMilis = SubscriptionPlan.BYTES_UNLIMITED;
        if (expirationDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                Date date = sdf.parse(expirationDate);
                if (date == null) {
                    throw new ParserException(parser, "Invalid expiration date in pin-set");
                }
                expirationTimestampMilis = date.getTime();
            } catch (ParseException e) {
                throw new ParserException(parser, "Invalid expiration date in pin-set", e);
            }
        }
        int outerDepth = parser.getDepth();
        Set<Pin> pins = new ArraySet();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals("pin")) {
                pins.add(parsePin(parser));
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
        return new PinSet(pins, expirationTimestampMilis);
    }

    private Domain parseDomain(XmlResourceParser parser, Set<String> seenDomains) throws IOException, XmlPullParserException, ParserException {
        boolean includeSubdomains = parser.getAttributeBooleanValue(null, "includeSubdomains", false);
        if (parser.next() != 4) {
            throw new ParserException(parser, "Domain name missing");
        }
        String domain = parser.getText().trim().toLowerCase(Locale.US);
        if (parser.next() != 3) {
            throw new ParserException(parser, "domain contains additional elements");
        } else if (seenDomains.add(domain)) {
            return new Domain(domain, includeSubdomains);
        } else {
            throw new ParserException(parser, domain + " has already been specified");
        }
    }

    private CertificatesEntryRef parseCertificatesEntry(XmlResourceParser parser, boolean defaultOverridePins) throws IOException, XmlPullParserException, ParserException {
        boolean overridePins = parser.getAttributeBooleanValue(null, "overridePins", defaultOverridePins);
        int sourceId = parser.getAttributeResourceValue(null, "src", -1);
        String sourceString = parser.getAttributeValue(null, "src");
        if (sourceString == null) {
            throw new ParserException(parser, "certificates element missing src attribute");
        }
        CertificateSource source;
        if (sourceId != -1) {
            source = new ResourceCertificateSource(sourceId, this.mContext);
        } else if ("system".equals(sourceString)) {
            source = SystemCertificateSource.getInstance();
        } else if ("user".equals(sourceString)) {
            source = UserCertificateSource.getInstance();
        } else {
            throw new ParserException(parser, "Unknown certificates src. Should be one of system|user|@resourceVal");
        }
        XmlUtils.skipCurrentTag(parser);
        return new CertificatesEntryRef(source, overridePins);
    }

    private Collection<CertificatesEntryRef> parseTrustAnchors(XmlResourceParser parser, boolean defaultOverridePins) throws IOException, XmlPullParserException, ParserException {
        int outerDepth = parser.getDepth();
        List<CertificatesEntryRef> anchors = new ArrayList();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals("certificates")) {
                anchors.add(parseCertificatesEntry(parser, defaultOverridePins));
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
        return anchors;
    }

    private List<Pair<Builder, Set<Domain>>> parseConfigEntry(XmlResourceParser parser, Set<String> seenDomains, Builder parentBuilder, int configType) throws IOException, XmlPullParserException, ParserException {
        List<Pair<Builder, Set<Domain>>> builders = new ArrayList();
        Builder builder = new Builder();
        builder.setParent(parentBuilder);
        Set<Domain> domains = new ArraySet();
        boolean seenPinSet = false;
        boolean seenTrustAnchors = false;
        boolean defaultOverridePins = configType == 2;
        String configName = parser.getName();
        int outerDepth = parser.getDepth();
        builders.add(new Pair(builder, domains));
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String name = parser.getAttributeName(i);
            if ("hstsEnforced".equals(name)) {
                builder.setHstsEnforced(parser.getAttributeBooleanValue(i, false));
            } else if ("cleartextTrafficPermitted".equals(name)) {
                builder.setCleartextTrafficPermitted(parser.getAttributeBooleanValue(i, true));
            }
        }
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            String tagName = parser.getName();
            if ("domain".equals(tagName)) {
                if (configType != 1) {
                    throw new ParserException(parser, "domain element not allowed in " + getConfigString(configType));
                }
                domains.add(parseDomain(parser, seenDomains));
            } else if ("trust-anchors".equals(tagName)) {
                if (seenTrustAnchors) {
                    throw new ParserException(parser, "Multiple trust-anchor elements not allowed");
                }
                builder.addCertificatesEntryRefs(parseTrustAnchors(parser, defaultOverridePins));
                seenTrustAnchors = true;
            } else if ("pin-set".equals(tagName)) {
                if (configType != 1) {
                    throw new ParserException(parser, "pin-set element not allowed in " + getConfigString(configType));
                } else if (seenPinSet) {
                    throw new ParserException(parser, "Multiple pin-set elements not allowed");
                } else {
                    builder.setPinSet(parsePinSet(parser));
                    seenPinSet = true;
                }
            } else if (!"domain-config".equals(tagName)) {
                XmlUtils.skipCurrentTag(parser);
            } else if (configType != 1) {
                throw new ParserException(parser, "Nested domain-config not allowed in " + getConfigString(configType));
            } else {
                builders.addAll(parseConfigEntry(parser, seenDomains, builder, configType));
            }
        }
        if (configType != 1 || !domains.isEmpty()) {
            return builders;
        }
        throw new ParserException(parser, "No domain elements in domain-config");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void addDebugAnchorsIfNeeded(Builder debugConfigBuilder, Builder builder) {
        if (debugConfigBuilder != null && (debugConfigBuilder.hasCertificatesEntryRefs() ^ 1) == 0 && builder.hasCertificatesEntryRefs()) {
            builder.addCertificatesEntryRefs(debugConfigBuilder.getCertificatesEntryRefs());
        }
    }

    private void parseNetworkSecurityConfig(XmlResourceParser parser) throws IOException, XmlPullParserException, ParserException {
        Set<String> seenDomains = new ArraySet();
        List<Pair<Builder, Set<Domain>>> builders = new ArrayList();
        Builder baseConfigBuilder = null;
        Builder debugConfigBuilder = null;
        boolean seenDebugOverrides = false;
        boolean seenBaseConfig = false;
        XmlUtils.beginDocument(parser, "network-security-config");
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if ("base-config".equals(parser.getName())) {
                if (seenBaseConfig) {
                    throw new ParserException(parser, "Only one base-config allowed");
                }
                seenBaseConfig = true;
                baseConfigBuilder = ((Pair) parseConfigEntry(parser, seenDomains, null, 0).get(0)).first;
            } else if ("domain-config".equals(parser.getName())) {
                builders.addAll(parseConfigEntry(parser, seenDomains, baseConfigBuilder, 1));
            } else if (!"debug-overrides".equals(parser.getName())) {
                XmlUtils.skipCurrentTag(parser);
            } else if (seenDebugOverrides) {
                throw new ParserException(parser, "Only one debug-overrides allowed");
            } else {
                if (this.mDebugBuild) {
                    debugConfigBuilder = (Builder) ((Pair) parseConfigEntry(parser, null, null, 2).get(0)).first;
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
                seenDebugOverrides = true;
            }
        }
        if (this.mDebugBuild && debugConfigBuilder == null) {
            debugConfigBuilder = parseDebugOverridesResource();
        }
        Builder platformDefaultBuilder = NetworkSecurityConfig.getDefaultBuilder(this.mTargetSdkVersion, this.mTargetSandboxVesrsion);
        addDebugAnchorsIfNeeded(debugConfigBuilder, platformDefaultBuilder);
        if (baseConfigBuilder != null) {
            baseConfigBuilder.setParent(platformDefaultBuilder);
            addDebugAnchorsIfNeeded(debugConfigBuilder, baseConfigBuilder);
        } else {
            baseConfigBuilder = platformDefaultBuilder;
        }
        Set<Pair<Domain, NetworkSecurityConfig>> configs = new ArraySet();
        for (Pair<Builder, Set<Domain>> entry : builders) {
            Builder builder = entry.first;
            Set<Domain> domains = entry.second;
            if (builder.getParent() == null) {
                builder.setParent(baseConfigBuilder);
            }
            addDebugAnchorsIfNeeded(debugConfigBuilder, builder);
            NetworkSecurityConfig config = builder.build();
            for (Domain domain : domains) {
                configs.add(new Pair(domain, config));
            }
        }
        this.mDefaultConfig = baseConfigBuilder.build();
        this.mDomainMap = configs;
    }

    private Builder parseDebugOverridesResource() throws IOException, XmlPullParserException, ParserException {
        XmlResourceParser parser;
        Throwable th;
        Throwable th2 = null;
        Resources resources = this.mContext.getResources();
        int resId = resources.getIdentifier(resources.getResourceEntryName(this.mResourceId) + "_debug", "xml", resources.getResourcePackageName(this.mResourceId));
        if (resId == 0) {
            return null;
        }
        Builder debugConfigBuilder = null;
        parser = null;
        try {
            parser = resources.getXml(resId);
            XmlUtils.beginDocument(parser, "network-security-config");
            int outerDepth = parser.getDepth();
            boolean seenDebugOverrides = false;
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if (!"debug-overrides".equals(parser.getName())) {
                    XmlUtils.skipCurrentTag(parser);
                } else if (seenDebugOverrides) {
                    throw new ParserException(parser, "Only one debug-overrides allowed");
                } else {
                    if (this.mDebugBuild) {
                        debugConfigBuilder = (Builder) ((Pair) parseConfigEntry(parser, null, null, 2).get(0)).first;
                    } else {
                        XmlUtils.skipCurrentTag(parser);
                    }
                    seenDebugOverrides = true;
                }
            }
            if (parser != null) {
                try {
                    parser.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 == null) {
                return debugConfigBuilder;
            }
            throw th2;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (parser != null) {
            try {
                parser.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }
}
