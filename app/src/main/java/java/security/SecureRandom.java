package java.security;

import dalvik.system.VMRuntime;
import java.security.Provider.Service;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.security.jca.GetInstance;
import sun.security.jca.GetInstance.Instance;
import sun.security.jca.Providers;

public class SecureRandom extends Random {
    public static final int DEFAULT_SDK_TARGET_FOR_CRYPTO_PROVIDER_WORKAROUND = 23;
    private static int sdkTargetForCryptoProviderWorkaround = 23;
    private static volatile SecureRandom seedGenerator = null;
    static final long serialVersionUID = 4940670005562187L;
    private String algorithm;
    private long counter;
    private MessageDigest digest;
    private Provider provider;
    private byte[] randomBytes;
    private int randomBytesUsed;
    private SecureRandomSpi secureRandomSpi;
    private byte[] state;

    private static final class StrongPatternHolder {
        private static Pattern pattern = Pattern.compile("\\s*([\\S&&[^:,]]*)(\\:([\\S&&[^,]]*))?\\s*(\\,(.*))?");

        private StrongPatternHolder() {
        }
    }

    public SecureRandom() {
        super(0);
        this.provider = null;
        this.secureRandomSpi = null;
        this.digest = null;
        getDefaultPRNG(false, null);
    }

    public SecureRandom(byte[] seed) {
        super(0);
        this.provider = null;
        this.secureRandomSpi = null;
        this.digest = null;
        getDefaultPRNG(true, seed);
    }

    private void getDefaultPRNG(boolean setSeed, byte[] seed) {
        String prng = getPrngAlgorithm();
        if (prng == null) {
            throw new IllegalStateException("No SecureRandom implementation!");
        }
        try {
            SecureRandom random = getInstance(prng);
            this.secureRandomSpi = random.getSecureRandomSpi();
            this.provider = random.getProvider();
            if (setSeed) {
                this.secureRandomSpi.engineSetSeed(seed);
            }
            if (getClass() == SecureRandom.class) {
                this.algorithm = prng;
            }
        } catch (Throwable nsae) {
            throw new RuntimeException(nsae);
        }
    }

    protected SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider) {
        this(secureRandomSpi, provider, null);
    }

    private SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider, String algorithm) {
        super(0);
        this.provider = null;
        this.secureRandomSpi = null;
        this.digest = null;
        this.secureRandomSpi = secureRandomSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public static SecureRandom getInstance(String algorithm) throws NoSuchAlgorithmException {
        Instance instance = GetInstance.getInstance("SecureRandom", SecureRandomSpi.class, algorithm);
        return new SecureRandom((SecureRandomSpi) instance.impl, instance.provider, algorithm);
    }

    public static void setSdkTargetForCryptoProviderWorkaround(int sdkTargetVersion) {
        sdkTargetForCryptoProviderWorkaround = sdkTargetVersion;
    }

    public static int getSdkTargetForCryptoProviderWorkaround() {
        return sdkTargetForCryptoProviderWorkaround;
    }

    public static SecureRandom getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        try {
            Instance instance = GetInstance.getInstance("SecureRandom", SecureRandomSpi.class, algorithm, provider);
            return new SecureRandom((SecureRandomSpi) instance.impl, instance.provider, algorithm);
        } catch (NoSuchProviderException nspe) {
            if ("Crypto".equals(provider)) {
                System.logE(" ********** PLEASE READ ************ ");
                System.logE(" * ");
                System.logE(" * New versions of the Android SDK no longer support the Crypto provider.");
                System.logE(" * If your app was relying on setSeed() to derive keys from strings, you");
                System.logE(" * should switch to using SecretKeySpec to load raw key bytes directly OR");
                System.logE(" * use a real key derivation function (KDF). See advice here : ");
                System.logE(" * http://android-developers.blogspot.com/2016/06/security-crypto-provider-deprecated-in.html ");
                System.logE(" *********************************** ");
                if (VMRuntime.getRuntime().getTargetSdkVersion() <= sdkTargetForCryptoProviderWorkaround) {
                    System.logE(" Returning an instance of SecureRandom from the Crypto provider");
                    System.logE(" as a temporary measure so that the apps targeting earlier SDKs");
                    System.logE(" keep working. Please do not rely on the presence of the Crypto");
                    System.logE(" provider in the codebase, as our plan is to delete it");
                    System.logE(" completely in the future.");
                    return getInstanceFromCryptoProvider(algorithm);
                }
            }
            throw nspe;
        }
    }

    private static SecureRandom getInstanceFromCryptoProvider(String algorithm) throws NoSuchAlgorithmException {
        try {
            Instance instance = GetInstance.getInstance(((Provider) SecureRandom.class.getClassLoader().loadClass("org.apache.harmony.security.provider.crypto.CryptoProvider").newInstance()).getService("SecureRandom", algorithm), SecureRandomSpi.class);
            return new SecureRandom((SecureRandomSpi) instance.impl, instance.provider, algorithm);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static SecureRandom getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        Instance instance = GetInstance.getInstance("SecureRandom", SecureRandomSpi.class, algorithm, provider);
        return new SecureRandom((SecureRandomSpi) instance.impl, instance.provider, algorithm);
    }

    SecureRandomSpi getSecureRandomSpi() {
        return this.secureRandomSpi;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public String getAlgorithm() {
        return this.algorithm != null ? this.algorithm : "unknown";
    }

    public synchronized void setSeed(byte[] seed) {
        this.secureRandomSpi.engineSetSeed(seed);
    }

    public void setSeed(long seed) {
        if (seed != 0) {
            this.secureRandomSpi.engineSetSeed(longToByteArray(seed));
        }
    }

    public synchronized void nextBytes(byte[] bytes) {
        this.secureRandomSpi.engineNextBytes(bytes);
    }

    protected final int next(int numBits) {
        int numBytes = (numBits + 7) / 8;
        byte[] b = new byte[numBytes];
        int next = 0;
        nextBytes(b);
        for (int i = 0; i < numBytes; i++) {
            next = (next << 8) + (b[i] & 255);
        }
        return next >>> ((numBytes * 8) - numBits);
    }

    public static byte[] getSeed(int numBytes) {
        if (seedGenerator == null) {
            seedGenerator = new SecureRandom();
        }
        return seedGenerator.generateSeed(numBytes);
    }

    public byte[] generateSeed(int numBytes) {
        return this.secureRandomSpi.engineGenerateSeed(numBytes);
    }

    private static byte[] longToByteArray(long l) {
        byte[] retVal = new byte[8];
        for (int i = 0; i < 8; i++) {
            retVal[i] = (byte) ((int) l);
            l >>= 8;
        }
        return retVal;
    }

    private static String getPrngAlgorithm() {
        for (Provider p : Providers.getProviderList().providers()) {
            for (Service s : p.getServices()) {
                if (s.getType().equals("SecureRandom")) {
                    return s.getAlgorithm();
                }
            }
        }
        return null;
    }

    public static SecureRandom getInstanceStrong() throws NoSuchAlgorithmException {
        String property = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return Security.getProperty("securerandom.strongAlgorithms");
            }
        });
        if (property == null || property.length() == 0) {
            throw new NoSuchAlgorithmException("Null/empty securerandom.strongAlgorithms Security Property");
        }
        CharSequence remainder = property;
        while (remainder != null) {
            Matcher m = StrongPatternHolder.pattern.matcher(remainder);
            if (m.matches()) {
                String alg = m.group(1);
                String prov = m.group(3);
                if (prov != null) {
                    return getInstance(alg, prov);
                }
                try {
                    return getInstance(alg);
                } catch (NoSuchAlgorithmException e) {
                    remainder = m.group(5);
                }
            } else {
                remainder = null;
            }
        }
        throw new NoSuchAlgorithmException("No strong SecureRandom impls available: " + property);
    }
}
