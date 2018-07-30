package android.content.pm;

import android.Manifest.permission;
import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.ActivityInfo.WindowLayout;
import android.content.pm.PackageParserCacheHelper.ReadHelper;
import android.content.pm.PackageParserCacheHelper.WriteHelper;
import android.content.pm.split.DefaultSplitAssetLoader;
import android.content.pm.split.SplitAssetDependencyLoader;
import android.content.pm.split.SplitAssetLoader;
import android.content.pm.split.SplitDependencyLoader;
import android.content.pm.split.SplitDependencyLoader.IllegalDependencyException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.hardware.Camera.Parameters;
import android.media.midi.MidiDeviceInfo;
import android.net.wifi.WifiEnterpriseConfig;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.BoostFramework;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedValue;
import android.util.apk.ApkSignatureSchemeV2Verifier;
import android.util.apk.ApkSignatureSchemeV2Verifier.SignatureNotFoundException;
import android.util.jar.StrictJarFile;
import com.android.internal.R;
import com.android.internal.os.ClassLoaderFactory;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import oppo.util.OppoMultiLauncherUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PackageParser {
    private static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";
    private static final String ANDROID_RESOURCES = "http://schemas.android.com/apk/res/android";
    public static final int APK_SIGNING_UNKNOWN = 0;
    public static final int APK_SIGNING_V1 = 1;
    public static final int APK_SIGNING_V2 = 2;
    private static final Set<String> CHILD_PACKAGE_TAGS = new ArraySet();
    private static final String CTEMM_PEM_PUBLIC_KEY = "-----BEGIN CERTIFICATE-----\nMIIFrDCCA5SgAwIBAgICEAEwDQYJKoZIhvcNAQELBQAwczELMAkGA1UEBhMCQ04x\nCzAJBgNVBAgMAkJKMQswCQYDVQQKDAJDVDENMAsGA1UECwwEQ1RTSTEaMBgGA1UE\nAwwRQ1RDQS1JTlRFUk1FRElBVEUxHzAdBgkqhkiG9w0BCQEWEGN0Y2FAY3RzaS5j\nb20uY24wHhcNMTYwODIwMDgwNjU0WhcNMTcwODMwMDgwNjU0WjB6MQswCQYDVQQG\nEwJDTjELMAkGA1UECAwCQkoxCzAJBgNVBAcMAkJKMQswCQYDVQQKDAJDVDENMAsG\nA1UECwwEQ1RTSTEVMBMGA1UEAwwMQ1RTSS1FTU1DRVJUMR4wHAYJKoZIhvcNAQkB\nFg9zb2NAY3RzaS5jb20uY24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB\nAQC6WgQ2S/gqgY4ldZUeCb1immgAXo63ilseCy0Va5kBii2bHO1QAjrfZo8dd5KJ\nBb5SA3F6unitVqiJNwjX/ZhMvB3uWy/FGlp0TFlJAdQ/HM6ZY0XkbfeRH7v1C4+E\naxlzqCWlfbXWFSwaZXRRwuAlbHUdlrSmoanpNRaN3Z/W0nl4WWkRlrrMPIM1AuXT\nIr2pwgTadbVK9J9JXXeJ2RhZFV9GJXLM3RB9DDNsjHHnIxlbdaC17YVnNasZjmp/\nF4WGvt+21M/p4t1bnS/Kv3Jj30icuyD9zypuOoNvM4t3VScIv/iIzEDWiZq2sg/w\nr2QKoqjAFowTO8GVPWqBAxVJAgMBAAGjggFBMIIBPTAJBgNVHRMEAjAAMBEGCWCG\nSAGG+EIBAQQEAwIGQDAzBglghkgBhvhCAQ0EJhYkT3BlblNTTCBHZW5lcmF0ZWQg\nU2VydmVyIENlcnRpZmljYXRlMB0GA1UdDgQWBBStVpCoWbmPOf1VrpLVdoOLvXLq\nqzCBowYDVR0jBIGbMIGYgBSGbtcFWWd5ThOWroYYzhxe1CdAEaF8pHoweDELMAkG\nA1UEBhMCQ04xCzAJBgNVBAgMAkJKMQswCQYDVQQHDAJCSjELMAkGA1UECgwCQ1Qx\nDTALBgNVBAsMBENUU0kxEjAQBgNVBAMMCUNUQ0EtUk9PVDEfMB0GCSqGSIb3DQEJ\nARYQY3RjYUBjdHNpLmNvbS5jboICEAEwDgYDVR0PAQH/BAQDAgWgMBMGA1UdJQQM\nMAoGCCsGAQUFBwMBMA0GCSqGSIb3DQEBCwUAA4ICAQBBgfrc9WswQCV5plZ5FfPF\ncPXsoubLfkG6+8fmcjy+9h9LRCJGn/HgUz81Q+FYQt6MFU9I6j9rwSw4AxUO2++7\n2Env5OMP1AhhALnF8VJVANv1HbEwwwnuvKG1I1kDgWPLaBdhZRKYlKaH6nZJPkZL\n1UO8cHYNoLPbBaAV60W9P3A2DwClcBYa6jPYXzDdtjPk2UOh9hFr63KQ+hiXhnKF\nOXbyB5BVqgGjpxJU5peb/21QFY+wSCNvK3f7iV4DGhEB0ulL2rO1Ui/xdEDmo9V5\nTn9Ixd/AazHAIkhsgivQZ5wyiH7rExlcdxUzOuNc0FSHcPnHeGlhbdh/IxeyxCDc\nZcbl/JbL1a81s6AIygjf9kMVWIgPiCFuayybzOOrKgn7MRWsW71cV5QjphAPHlHN\nv5k+LM7g37pPsNTMYsi21Taj2YuzQf4TiRYle8M+eYdypq1gMU3xrvgx2BAaBG8B\nMIKaU2zrQoG09VKrDbxwjzC7dXDEOlnhO9x1E+rEyCP1vq9Wa7ZtuN9tjc0j23Jd\nv/9lMJQoH+MzKOxLqcdF2EDbKXklu3NLL6deSRJz+X+2yb8lUuSsmOrfXQp1VVEf\nxPfWG6fMPmpPU0tvflIHjZPXJyq88nDxAVxcYxEaOPSJp6duotOptKxq1aqQqH/9\nZNoAt2cRFf8WiFbjuqpQlA==\n-----END CERTIFICATE-----\n";
    private static final boolean DEBUG_BACKUP = false;
    private static final boolean DEBUG_JAR = false;
    private static final boolean DEBUG_PARSER = false;
    private static final float DEFAULT_PRE_O_MAX_ASPECT_RATIO = 2.2f;
    private static final boolean LOG_PARSE_TIMINGS = Build.IS_DEBUGGABLE;
    private static final int LOG_PARSE_TIMINGS_THRESHOLD_MS = 100;
    private static final boolean LOG_UNSAFE_BROADCASTS = false;
    private static final int MAX_PACKAGES_PER_APK = 5;
    private static final String METADATA_MAX_ASPECT_RATIO = "android.max_aspect";
    private static final String META_DATA_INSTANT_APPS = "instantapps.clients.allowed";
    private static final String MNT_EXPAND = "/mnt/expand/";
    private static final boolean MULTI_PACKAGE_APK_ENABLED;
    public static final NewPermissionInfo[] NEW_PERMISSIONS = new NewPermissionInfo[]{new NewPermissionInfo(permission.WRITE_EXTERNAL_STORAGE, 4, 0), new NewPermissionInfo(permission.READ_PHONE_STATE, 4, 0)};
    private static final int NUMBER_OF_CORES = (Runtime.getRuntime().availableProcessors() >= 4 ? 4 : Runtime.getRuntime().availableProcessors());
    public static final int PARSE_CHATTY = 2;
    public static final int PARSE_COLLECT_CERTIFICATES = 256;
    private static final int PARSE_DEFAULT_INSTALL_LOCATION = -1;
    private static final int PARSE_DEFAULT_TARGET_SANDBOX = 1;
    public static final int PARSE_ENFORCE_CODE = 1024;
    public static final int PARSE_EXTERNAL_STORAGE = 32;
    public static final int PARSE_FORCE_SDK = 4096;
    public static final int PARSE_FORWARD_LOCK = 16;
    public static final int PARSE_IGNORE_PROCESSES = 8;
    @Deprecated
    public static final int PARSE_IS_EPHEMERAL = 2048;
    public static final int PARSE_IS_PRIVILEGED = 128;
    public static final int PARSE_IS_SYSTEM = 1;
    public static final int PARSE_IS_SYSTEM_DIR = 64;
    public static final int PARSE_MUST_BE_APK = 4;
    public static final int PARSE_TRUSTED_OVERLAY = 512;
    private static final String PROPERTY_CHILD_PACKAGES_ENABLED = "persist.sys.child_packages_enabled";
    private static final int RECREATE_ON_CONFIG_CHANGES_MASK = 3;
    private static final boolean RIGID_PARSER = false;
    private static final Set<String> SAFE_BROADCASTS = new ArraySet();
    private static final String[] SDK_CODENAMES = VERSION.ACTIVE_CODENAMES;
    private static final int SDK_VERSION = VERSION.SDK_INT;
    public static final SplitPermissionInfo[] SPLIT_PERMISSIONS;
    private static final String TAG = "PackageParser";
    private static final String TAG_ADOPT_PERMISSIONS = "adopt-permissions";
    private static final String TAG_APPLICATION = "application";
    private static final String TAG_COMPATIBLE_SCREENS = "compatible-screens";
    private static final String TAG_EAT_COMMENT = "eat-comment";
    private static final String TAG_FEATURE_GROUP = "feature-group";
    private static final String TAG_INSTRUMENTATION = "instrumentation";
    private static final String TAG_KEY_SETS = "key-sets";
    private static final String TAG_MANIFEST = "manifest";
    private static final String TAG_ORIGINAL_PACKAGE = "original-package";
    private static final String TAG_OVERLAY = "overlay";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PACKAGE_VERIFIER = "package-verifier";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSION_GROUP = "permission-group";
    private static final String TAG_PERMISSION_TREE = "permission-tree";
    private static final String TAG_PROTECTED_BROADCAST = "protected-broadcast";
    private static final String TAG_RESTRICT_UPDATE = "restrict-update";
    private static final String TAG_SUPPORTS_INPUT = "supports-input";
    private static final String TAG_SUPPORT_SCREENS = "supports-screens";
    private static final String TAG_USES_CONFIGURATION = "uses-configuration";
    private static final String TAG_USES_FEATURE = "uses-feature";
    private static final String TAG_USES_GL_TEXTURE = "uses-gl-texture";
    private static final String TAG_USES_PERMISSION = "uses-permission";
    private static final String TAG_USES_PERMISSION_SDK_23 = "uses-permission-sdk-23";
    private static final String TAG_USES_PERMISSION_SDK_M = "uses-permission-sdk-m";
    private static final String TAG_USES_SDK = "uses-sdk";
    private static final String TAG_USES_SPLIT = "uses-split";
    private static AtomicReference<byte[]> sBuffer = new AtomicReference();
    public static final AtomicInteger sCachedPackageReadCount = new AtomicInteger();
    private static boolean sCompatibilityModeEnabled = true;
    private static boolean sIsPerfLockAcquired = false;
    private static BoostFramework sPerfBoost = null;
    private static final Comparator<String> sSplitNameComparator = new SplitNameComparator();
    @Deprecated
    private String mArchiveSourcePath;
    private File mCacheDir;
    private Callback mCallback;
    private DisplayMetrics mMetrics = new DisplayMetrics();
    private boolean mOnlyCoreApps;
    private int mParseError = 1;
    private ParsePackageItemArgs mParseInstrumentationArgs;
    private String[] mSeparateProcesses;

    public static abstract class Component<II extends IntentInfo> {
        public final String className;
        ComponentName componentName;
        String componentShortName;
        public final ArrayList<II> intents;
        public Bundle metaData;
        public Package owner;

        public Component(Package _owner) {
            this.owner = _owner;
            this.intents = null;
            this.className = null;
        }

        public Component(ParsePackageItemArgs args, PackageItemInfo outInfo) {
            this.owner = args.owner;
            this.intents = new ArrayList(0);
            if (PackageParser.parsePackageItemInfo(args.owner, outInfo, args.outError, args.tag, args.sa, true, args.nameRes, args.labelRes, args.iconRes, args.roundIconRes, args.logoRes, args.bannerRes)) {
                this.className = outInfo.name;
            } else {
                this.className = null;
            }
        }

        public Component(ParseComponentArgs args, ComponentInfo outInfo) {
            this((ParsePackageItemArgs) args, (PackageItemInfo) outInfo);
            if (args.outError[0] == null) {
                if (args.processRes != 0) {
                    CharSequence pname;
                    if (this.owner.applicationInfo.targetSdkVersion >= 8) {
                        pname = args.sa.getNonConfigurationString(args.processRes, 1024);
                    } else {
                        pname = args.sa.getNonResourceString(args.processRes);
                    }
                    outInfo.processName = PackageParser.buildProcessName(this.owner.applicationInfo.packageName, this.owner.applicationInfo.processName, pname, args.flags, args.sepProcesses, args.outError);
                }
                if (args.descriptionRes != 0) {
                    outInfo.descriptionRes = args.sa.getResourceId(args.descriptionRes, 0);
                }
                outInfo.enabled = args.sa.getBoolean(args.enabledRes, true);
            }
        }

        public Component(Component<II> clone) {
            this.owner = clone.owner;
            this.intents = clone.intents;
            this.className = clone.className;
            this.componentName = clone.componentName;
            this.componentShortName = clone.componentShortName;
        }

        public ComponentName getComponentName() {
            if (this.componentName != null) {
                return this.componentName;
            }
            if (this.className != null) {
                this.componentName = new ComponentName(this.owner.applicationInfo.packageName, this.className);
            }
            return this.componentName;
        }

        protected Component(Parcel in) {
            this.className = in.readString();
            this.metaData = in.readBundle();
            this.intents = createIntentsList(in);
            this.owner = null;
        }

        protected void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.className);
            dest.writeBundle(this.metaData);
            writeIntentsList(this.intents, dest, flags);
        }

        private static void writeIntentsList(ArrayList<? extends IntentInfo> list, Parcel out, int flags) {
            if (list == null) {
                out.writeInt(-1);
                return;
            }
            int N = list.size();
            out.writeInt(N);
            if (N > 0) {
                out.writeString(((IntentInfo) list.get(0)).getClass().getName());
                for (int i = 0; i < N; i++) {
                    ((IntentInfo) list.get(i)).writeIntentInfoToParcel(out, flags);
                }
            }
        }

        private static <T extends IntentInfo> ArrayList<T> createIntentsList(Parcel in) {
            int N = in.readInt();
            if (N == -1) {
                return null;
            }
            if (N == 0) {
                return new ArrayList(0);
            }
            String componentName = in.readString();
            try {
                Constructor<T> cons = Class.forName(componentName).getConstructor(new Class[]{Parcel.class});
                ArrayList<T> intentsList = new ArrayList(N);
                for (int i = 0; i < N; i++) {
                    intentsList.add((IntentInfo) cons.newInstance(new Object[]{in}));
                }
                return intentsList;
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Unable to construct intent list for: " + componentName);
            }
        }

        public void appendComponentShortName(StringBuilder sb) {
            ComponentName.appendShortString(sb, this.owner.applicationInfo.packageName, this.className);
        }

        public void printComponentShortName(PrintWriter pw) {
            ComponentName.printShortString(pw, this.owner.applicationInfo.packageName, this.className);
        }

        public void setPackageName(String packageName) {
            this.componentName = null;
            this.componentShortName = null;
        }
    }

    public static final class Activity extends Component<ActivityIntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Activity>() {
            public Activity createFromParcel(Parcel in) {
                return new Activity(in, null);
            }

            public Activity[] newArray(int size) {
                return new Activity[size];
            }
        };
        public final ActivityInfo info;
        private boolean mHasMaxAspectRatio;

        /* synthetic */ Activity(Parcel in, Activity -this1) {
            this(in);
        }

        private boolean hasMaxAspectRatio() {
            return this.mHasMaxAspectRatio;
        }

        public Activity(ParseComponentArgs args, ActivityInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        private void setMaxAspectRatio(float maxAspectRatio) {
            if (this.info.resizeMode != 2 && this.info.resizeMode != 1) {
                if (maxAspectRatio >= 1.0f || maxAspectRatio == 0.0f) {
                    this.info.maxAspectRatio = maxAspectRatio;
                    if (maxAspectRatio >= 2.1f && maxAspectRatio < PackageParser.DEFAULT_PRE_O_MAX_ASPECT_RATIO) {
                        this.info.maxAspectRatio = PackageParser.DEFAULT_PRE_O_MAX_ASPECT_RATIO;
                    }
                    this.mHasMaxAspectRatio = true;
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Activity{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags | 2);
            dest.writeBoolean(this.mHasMaxAspectRatio);
        }

        private Activity(Parcel in) {
            super(in);
            this.info = (ActivityInfo) in.readParcelable(Object.class.getClassLoader());
            this.mHasMaxAspectRatio = in.readBoolean();
            for (ActivityIntentInfo aii : this.intents) {
                aii.activity = this;
            }
            if (this.info.permission != null) {
                this.info.permission = this.info.permission.intern();
            }
        }
    }

    public static abstract class IntentInfo extends IntentFilter {
        public int banner;
        public boolean hasDefault;
        public int icon;
        public int labelRes;
        public int logo;
        public CharSequence nonLocalizedLabel;
        public int preferred;

        protected IntentInfo() {
        }

        protected IntentInfo(Parcel dest) {
            boolean z = true;
            super(dest);
            if (dest.readInt() != 1) {
                z = false;
            }
            this.hasDefault = z;
            this.labelRes = dest.readInt();
            this.nonLocalizedLabel = dest.readCharSequence();
            this.icon = dest.readInt();
            this.logo = dest.readInt();
            this.banner = dest.readInt();
            this.preferred = dest.readInt();
        }

        public void writeIntentInfoToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.hasDefault ? 1 : 0);
            dest.writeInt(this.labelRes);
            dest.writeCharSequence(this.nonLocalizedLabel);
            dest.writeInt(this.icon);
            dest.writeInt(this.logo);
            dest.writeInt(this.banner);
            dest.writeInt(this.preferred);
        }
    }

    public static final class ActivityIntentInfo extends IntentInfo {
        public Activity activity;

        public ActivityIntentInfo(Activity _activity) {
            this.activity = _activity;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ActivityIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.activity.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public ActivityIntentInfo(Parcel in) {
            super(in);
        }
    }

    public static class ApkLite {
        public final Certificate[][] certificates;
        public final String codePath;
        public final String configForSplit;
        public final boolean coreApp;
        public final boolean debuggable;
        public final boolean extractNativeLibs;
        public final int installLocation;
        public boolean isFeatureSplit;
        public final boolean isolatedSplits;
        public final boolean multiArch;
        public final String packageName;
        public final int revisionCode;
        public final Signature[] signatures;
        public final String splitName;
        public final boolean use32bitAbi;
        public final String usesSplitName;
        public final VerifierInfo[] verifiers;
        public final int versionCode;

        public ApkLite(String codePath, String packageName, String splitName, boolean isFeatureSplit, String configForSplit, String usesSplitName, int versionCode, int revisionCode, int installLocation, List<VerifierInfo> verifiers, Signature[] signatures, Certificate[][] certificates, boolean coreApp, boolean debuggable, boolean multiArch, boolean use32bitAbi, boolean extractNativeLibs, boolean isolatedSplits) {
            this.codePath = codePath;
            this.packageName = packageName;
            this.splitName = splitName;
            this.isFeatureSplit = isFeatureSplit;
            this.configForSplit = configForSplit;
            this.usesSplitName = usesSplitName;
            this.versionCode = versionCode;
            this.revisionCode = revisionCode;
            this.installLocation = installLocation;
            this.verifiers = (VerifierInfo[]) verifiers.toArray(new VerifierInfo[verifiers.size()]);
            this.signatures = signatures;
            this.certificates = certificates;
            this.coreApp = coreApp;
            this.debuggable = debuggable;
            this.multiArch = multiArch;
            this.use32bitAbi = use32bitAbi;
            this.extractNativeLibs = extractNativeLibs;
            this.isolatedSplits = isolatedSplits;
        }
    }

    private static class CachedComponentArgs {
        ParseComponentArgs mActivityAliasArgs;
        ParseComponentArgs mActivityArgs;
        ParseComponentArgs mProviderArgs;
        ParseComponentArgs mServiceArgs;

        /* synthetic */ CachedComponentArgs(CachedComponentArgs -this0) {
            this();
        }

        private CachedComponentArgs() {
        }
    }

    public interface Callback {
        String[] getOverlayApks(String str);

        String[] getOverlayPaths(String str, String str2);

        boolean hasFeature(String str);
    }

    public static final class CallbackImpl implements Callback {
        private final PackageManager mPm;

        public CallbackImpl(PackageManager pm) {
            this.mPm = pm;
        }

        public boolean hasFeature(String feature) {
            return this.mPm.hasSystemFeature(feature);
        }

        public String[] getOverlayPaths(String targetPackageName, String targetPath) {
            return null;
        }

        public String[] getOverlayApks(String targetPackageName) {
            return null;
        }
    }

    public static final class Instrumentation extends Component<IntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Instrumentation>() {
            public Instrumentation createFromParcel(Parcel in) {
                return new Instrumentation(in, null);
            }

            public Instrumentation[] newArray(int size) {
                return new Instrumentation[size];
            }
        };
        public final InstrumentationInfo info;

        /* synthetic */ Instrumentation(Parcel in, Instrumentation -this1) {
            this(in);
        }

        public Instrumentation(ParsePackageItemArgs args, InstrumentationInfo _info) {
            super(args, (PackageItemInfo) _info);
            this.info = _info;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Instrumentation{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags);
        }

        private Instrumentation(Parcel in) {
            super(in);
            this.info = (InstrumentationInfo) in.readParcelable(Object.class.getClassLoader());
            if (this.info.targetPackage != null) {
                this.info.targetPackage = this.info.targetPackage.intern();
            }
            if (this.info.targetProcesses != null) {
                this.info.targetProcesses = this.info.targetProcesses.intern();
            }
        }
    }

    private static class ManifestDigest {
        private static final String DIGEST_ALGORITHM = "SHA-256";
        private static final String TAG = "ManifestDigest";
        private final byte[] mDigest;

        ManifestDigest(byte[] digest) {
            this.mDigest = digest;
        }

        static ManifestDigest fromInputStream(InputStream fileIs) {
            if (fileIs == null) {
                return null;
            }
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                DigestInputStream dis = new DigestInputStream(new BufferedInputStream(fileIs), md);
                try {
                    byte[] readBuffer = new byte[8192];
                    while (true) {
                        if (dis.read(readBuffer, 0, readBuffer.length) == -1) {
                            break;
                        }
                    }
                    return new ManifestDigest(md.digest());
                } catch (IOException e) {
                    Slog.w(TAG, "Could not read manifest");
                    return null;
                } finally {
                    IoUtils.closeQuietly(dis);
                }
            } catch (NoSuchAlgorithmException e2) {
                throw new RuntimeException("SHA-256 must be available", e2);
            }
        }

        public boolean equals(Object o) {
            if (!(o instanceof ManifestDigest)) {
                return false;
            }
            ManifestDigest other = (ManifestDigest) o;
            return this != other ? Arrays.equals(this.mDigest, other.mDigest) : true;
        }

        public int hashCode() {
            return Arrays.hashCode(this.mDigest);
        }
    }

    public static class NewPermissionInfo {
        public final int fileVersion;
        public final String name;
        public final int sdkVersion;

        public NewPermissionInfo(String name, int sdkVersion, int fileVersion) {
            this.name = name;
            this.sdkVersion = sdkVersion;
            this.fileVersion = fileVersion;
        }
    }

    public static final class Package implements Parcelable {
        public static final Creator CREATOR = new Creator<Package>() {
            public Package createFromParcel(Parcel in) {
                return new Package(in);
            }

            public Package[] newArray(int size) {
                return new Package[size];
            }
        };
        public final ArrayList<Activity> activities;
        public ApplicationInfo applicationInfo;
        public String baseCodePath;
        public boolean baseHardwareAccelerated;
        public int baseRevisionCode;
        public ArrayList<Package> childPackages;
        public String codePath;
        public ArrayList<ConfigurationInfo> configPreferences;
        public boolean coreApp;
        public String cpuAbiOverride;
        public ArrayList<FeatureGroupInfo> featureGroups;
        public int installLocation;
        public final ArrayList<Instrumentation> instrumentation;
        public boolean isStub;
        public ArrayList<String> libraryNames;
        public ArrayList<String> mAdoptPermissions;
        public Bundle mAppMetaData;
        public Certificate[][] mCertificates;
        public Object mExtras;
        public boolean mIsStaticOverlay;
        public ArrayMap<String, ArraySet<PublicKey>> mKeySetMapping;
        public long[] mLastPackageUsageTimeInMills;
        public ArrayList<String> mOriginalPackages;
        public int mOverlayPriority;
        public String mOverlayTarget;
        public int mPreferredOrder;
        public String mRealPackage;
        public String mRequiredAccountType;
        public boolean mRequiredForAllUsers;
        public String mRestrictedAccountType;
        public String mSharedUserId;
        public int mSharedUserLabel;
        public Signature[] mSignatures;
        public ArraySet<PublicKey> mSigningKeys;
        public boolean mTrustedOverlay;
        public ArraySet<String> mUpgradeKeySets;
        public int mVersionCode;
        public String mVersionName;
        public String manifestPackageName;
        public String packageName;
        public Package parentPackage;
        public final ArrayList<PermissionGroup> permissionGroups;
        public final ArrayList<Permission> permissions;
        public ArrayList<ActivityIntentInfo> preferredActivityFilters;
        public ArrayList<String> protectedBroadcasts;
        public final ArrayList<Provider> providers;
        public final ArrayList<Activity> receivers;
        public ArrayList<FeatureInfo> reqFeatures;
        public final ArrayList<String> requestedPermissions;
        public byte[] restrictUpdateHash;
        public final ArrayList<Service> services;
        public String[] splitCodePaths;
        public int[] splitFlags;
        public String[] splitNames;
        public int[] splitPrivateFlags;
        public int[] splitRevisionCodes;
        public String staticSharedLibName;
        public int staticSharedLibVersion;
        public boolean use32bitAbi;
        public ArrayList<String> usesLibraries;
        public String[] usesLibraryFiles;
        public ArrayList<String> usesOptionalLibraries;
        public ArrayList<String> usesStaticLibraries;
        public String[][] usesStaticLibrariesCertDigests;
        public int[] usesStaticLibrariesVersions;
        public boolean visibleToInstantApps;
        public String volumeUuid;

        public Package(String packageName) {
            this.applicationInfo = new ApplicationInfo();
            this.permissions = new ArrayList(0);
            this.permissionGroups = new ArrayList(0);
            this.activities = new ArrayList(0);
            this.receivers = new ArrayList(0);
            this.providers = new ArrayList(0);
            this.services = new ArrayList(0);
            this.instrumentation = new ArrayList(0);
            this.requestedPermissions = new ArrayList();
            this.staticSharedLibName = null;
            this.staticSharedLibVersion = 0;
            this.libraryNames = null;
            this.usesLibraries = null;
            this.usesStaticLibraries = null;
            this.usesStaticLibrariesVersions = null;
            this.usesStaticLibrariesCertDigests = null;
            this.usesOptionalLibraries = null;
            this.usesLibraryFiles = null;
            this.preferredActivityFilters = null;
            this.mOriginalPackages = null;
            this.mRealPackage = null;
            this.mAdoptPermissions = null;
            this.mAppMetaData = null;
            this.mPreferredOrder = 0;
            this.mLastPackageUsageTimeInMills = new long[8];
            this.configPreferences = null;
            this.reqFeatures = null;
            this.featureGroups = null;
            this.packageName = packageName;
            this.manifestPackageName = packageName;
            this.applicationInfo.packageName = packageName;
            this.applicationInfo.uid = -1;
        }

        public void setApplicationVolumeUuid(String volumeUuid) {
            UUID storageUuid = StorageManager.convert(volumeUuid);
            this.applicationInfo.volumeUuid = volumeUuid;
            this.applicationInfo.storageUuid = storageUuid;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.volumeUuid = volumeUuid;
                    ((Package) this.childPackages.get(i)).applicationInfo.storageUuid = storageUuid;
                }
            }
        }

        public void setApplicationInfoCodePath(String codePath) {
            this.applicationInfo.setCodePath(codePath);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.setCodePath(codePath);
                }
            }
        }

        public void setApplicationInfoResourcePath(String resourcePath) {
            this.applicationInfo.setResourcePath(resourcePath);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.setResourcePath(resourcePath);
                }
            }
        }

        public void setApplicationInfoBaseResourcePath(String resourcePath) {
            this.applicationInfo.setBaseResourcePath(resourcePath);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.setBaseResourcePath(resourcePath);
                }
            }
        }

        public void setApplicationInfoBaseCodePath(String baseCodePath) {
            this.applicationInfo.setBaseCodePath(baseCodePath);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.setBaseCodePath(baseCodePath);
                }
            }
        }

        public List<String> getChildPackageNames() {
            if (this.childPackages == null) {
                return null;
            }
            int childCount = this.childPackages.size();
            List<String> childPackageNames = new ArrayList(childCount);
            for (int i = 0; i < childCount; i++) {
                childPackageNames.add(((Package) this.childPackages.get(i)).packageName);
            }
            return childPackageNames;
        }

        public boolean hasChildPackage(String packageName) {
            int childCount = this.childPackages != null ? this.childPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                if (((Package) this.childPackages.get(i)).packageName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        public void setApplicationInfoSplitCodePaths(String[] splitCodePaths) {
            this.applicationInfo.setSplitCodePaths(splitCodePaths);
        }

        public void setApplicationInfoSplitResourcePaths(String[] resroucePaths) {
            this.applicationInfo.setSplitResourcePaths(resroucePaths);
        }

        public void setSplitCodePaths(String[] codePaths) {
            this.splitCodePaths = codePaths;
        }

        public void setCodePath(String codePath) {
            this.codePath = codePath;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).codePath = codePath;
                }
            }
        }

        public void setBaseCodePath(String baseCodePath) {
            this.baseCodePath = baseCodePath;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).baseCodePath = baseCodePath;
                }
            }
        }

        public void setSignatures(Signature[] signatures) {
            this.mSignatures = signatures;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).mSignatures = signatures;
                }
            }
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).volumeUuid = volumeUuid;
                }
            }
        }

        public void setApplicationInfoFlags(int mask, int flags) {
            this.applicationInfo.flags = (this.applicationInfo.flags & (~mask)) | (mask & flags);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.flags = (this.applicationInfo.flags & (~mask)) | (mask & flags);
                }
            }
        }

        public void setUse32bitAbi(boolean use32bitAbi) {
            this.use32bitAbi = use32bitAbi;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).use32bitAbi = use32bitAbi;
                }
            }
        }

        public boolean isLibrary() {
            return this.staticSharedLibName == null ? ArrayUtils.isEmpty(this.libraryNames) ^ 1 : true;
        }

        public List<String> getAllCodePaths() {
            ArrayList<String> paths = new ArrayList();
            paths.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(paths, this.splitCodePaths);
            }
            return paths;
        }

        public List<String> getAllCodePathsExcludingResourceOnly() {
            ArrayList<String> paths = new ArrayList();
            if ((this.applicationInfo.flags & 4) != 0) {
                paths.add(this.baseCodePath);
            }
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                for (int i = 0; i < this.splitCodePaths.length; i++) {
                    if ((this.splitFlags[i] & 4) != 0) {
                        paths.add(this.splitCodePaths[i]);
                    }
                }
            }
            return paths;
        }

        public void setPackageName(String newName) {
            int i;
            this.packageName = newName;
            this.applicationInfo.packageName = newName;
            for (i = this.permissions.size() - 1; i >= 0; i--) {
                ((Permission) this.permissions.get(i)).setPackageName(newName);
            }
            for (i = this.permissionGroups.size() - 1; i >= 0; i--) {
                ((PermissionGroup) this.permissionGroups.get(i)).setPackageName(newName);
            }
            for (i = this.activities.size() - 1; i >= 0; i--) {
                ((Activity) this.activities.get(i)).setPackageName(newName);
            }
            for (i = this.receivers.size() - 1; i >= 0; i--) {
                ((Activity) this.receivers.get(i)).setPackageName(newName);
            }
            for (i = this.providers.size() - 1; i >= 0; i--) {
                ((Provider) this.providers.get(i)).setPackageName(newName);
            }
            for (i = this.services.size() - 1; i >= 0; i--) {
                ((Service) this.services.get(i)).setPackageName(newName);
            }
            for (i = this.instrumentation.size() - 1; i >= 0; i--) {
                ((Instrumentation) this.instrumentation.get(i)).setPackageName(newName);
            }
        }

        public boolean hasComponentClassName(String name) {
            int i;
            for (i = this.activities.size() - 1; i >= 0; i--) {
                if (name.equals(((Activity) this.activities.get(i)).className)) {
                    return true;
                }
            }
            for (i = this.receivers.size() - 1; i >= 0; i--) {
                if (name.equals(((Activity) this.receivers.get(i)).className)) {
                    return true;
                }
            }
            for (i = this.providers.size() - 1; i >= 0; i--) {
                if (name.equals(((Provider) this.providers.get(i)).className)) {
                    return true;
                }
            }
            for (i = this.services.size() - 1; i >= 0; i--) {
                if (name.equals(((Service) this.services.get(i)).className)) {
                    return true;
                }
            }
            for (i = this.instrumentation.size() - 1; i >= 0; i--) {
                if (name.equals(((Instrumentation) this.instrumentation.get(i)).className)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isForwardLocked() {
            return this.applicationInfo.isForwardLocked();
        }

        public boolean isSystemApp() {
            return this.applicationInfo.isSystemApp();
        }

        public boolean isPrivilegedApp() {
            return this.applicationInfo.isPrivilegedApp();
        }

        public boolean isUpdatedSystemApp() {
            return this.applicationInfo.isUpdatedSystemApp();
        }

        public boolean canHaveOatDir() {
            if ((!isSystemApp() || isUpdatedSystemApp()) && (isForwardLocked() ^ 1) != 0) {
                return this.applicationInfo.isExternalAsec() ^ 1;
            }
            return false;
        }

        public boolean isMatch(int flags) {
            if ((1048576 & flags) != 0) {
                return isSystemApp();
            }
            return true;
        }

        public long getLatestPackageUseTimeInMills() {
            long latestUse = 0;
            for (long use : this.mLastPackageUsageTimeInMills) {
                latestUse = Math.max(latestUse, use);
            }
            return latestUse;
        }

        public long getLatestForegroundPackageUseTimeInMills() {
            int i = 0;
            int[] foregroundReasons = new int[]{0, 2};
            long latestUse = 0;
            int length = foregroundReasons.length;
            while (i < length) {
                latestUse = Math.max(latestUse, this.mLastPackageUsageTimeInMills[foregroundReasons[i]]);
                i++;
            }
            return latestUse;
        }

        public String toString() {
            return "Package{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.packageName + "}";
        }

        public int describeContents() {
            return 0;
        }

        public Package(Parcel dest) {
            boolean z;
            boolean z2 = true;
            this.applicationInfo = new ApplicationInfo();
            this.permissions = new ArrayList(0);
            this.permissionGroups = new ArrayList(0);
            this.activities = new ArrayList(0);
            this.receivers = new ArrayList(0);
            this.providers = new ArrayList(0);
            this.services = new ArrayList(0);
            this.instrumentation = new ArrayList(0);
            this.requestedPermissions = new ArrayList();
            this.staticSharedLibName = null;
            this.staticSharedLibVersion = 0;
            this.libraryNames = null;
            this.usesLibraries = null;
            this.usesStaticLibraries = null;
            this.usesStaticLibrariesVersions = null;
            this.usesStaticLibrariesCertDigests = null;
            this.usesOptionalLibraries = null;
            this.usesLibraryFiles = null;
            this.preferredActivityFilters = null;
            this.mOriginalPackages = null;
            this.mRealPackage = null;
            this.mAdoptPermissions = null;
            this.mAppMetaData = null;
            this.mPreferredOrder = 0;
            this.mLastPackageUsageTimeInMills = new long[8];
            this.configPreferences = null;
            this.reqFeatures = null;
            this.featureGroups = null;
            ClassLoader boot = Object.class.getClassLoader();
            this.packageName = dest.readString().intern();
            this.manifestPackageName = dest.readString();
            this.splitNames = dest.readStringArray();
            this.volumeUuid = dest.readString();
            this.codePath = dest.readString();
            this.baseCodePath = dest.readString();
            this.splitCodePaths = dest.readStringArray();
            this.baseRevisionCode = dest.readInt();
            this.splitRevisionCodes = dest.createIntArray();
            this.splitFlags = dest.createIntArray();
            this.splitPrivateFlags = dest.createIntArray();
            if (dest.readInt() == 1) {
                z = true;
            } else {
                z = false;
            }
            this.baseHardwareAccelerated = z;
            this.applicationInfo = (ApplicationInfo) dest.readParcelable(boot);
            if (this.applicationInfo.permission != null) {
                this.applicationInfo.permission = this.applicationInfo.permission.intern();
            }
            dest.readParcelableList(this.permissions, boot);
            fixupOwner(this.permissions);
            dest.readParcelableList(this.permissionGroups, boot);
            fixupOwner(this.permissionGroups);
            dest.readParcelableList(this.activities, boot);
            fixupOwner(this.activities);
            dest.readParcelableList(this.receivers, boot);
            fixupOwner(this.receivers);
            dest.readParcelableList(this.providers, boot);
            fixupOwner(this.providers);
            dest.readParcelableList(this.services, boot);
            fixupOwner(this.services);
            dest.readParcelableList(this.instrumentation, boot);
            fixupOwner(this.instrumentation);
            dest.readStringList(this.requestedPermissions);
            internStringArrayList(this.requestedPermissions);
            this.protectedBroadcasts = dest.createStringArrayList();
            internStringArrayList(this.protectedBroadcasts);
            this.parentPackage = (Package) dest.readParcelable(boot);
            this.childPackages = new ArrayList();
            dest.readParcelableList(this.childPackages, boot);
            if (this.childPackages.size() == 0) {
                this.childPackages = null;
            }
            this.staticSharedLibName = dest.readString();
            if (this.staticSharedLibName != null) {
                this.staticSharedLibName = this.staticSharedLibName.intern();
            }
            this.staticSharedLibVersion = dest.readInt();
            this.libraryNames = dest.createStringArrayList();
            internStringArrayList(this.libraryNames);
            this.usesLibraries = dest.createStringArrayList();
            internStringArrayList(this.usesLibraries);
            this.usesOptionalLibraries = dest.createStringArrayList();
            internStringArrayList(this.usesOptionalLibraries);
            this.usesLibraryFiles = dest.readStringArray();
            int libCount = dest.readInt();
            if (libCount > 0) {
                this.usesStaticLibraries = new ArrayList(libCount);
                dest.readStringList(this.usesStaticLibraries);
                internStringArrayList(this.usesStaticLibraries);
                this.usesStaticLibrariesVersions = new int[libCount];
                dest.readIntArray(this.usesStaticLibrariesVersions);
                this.usesStaticLibrariesCertDigests = new String[libCount][];
                for (int i = 0; i < libCount; i++) {
                    this.usesStaticLibrariesCertDigests[i] = dest.createStringArray();
                }
            }
            this.preferredActivityFilters = new ArrayList();
            dest.readParcelableList(this.preferredActivityFilters, boot);
            if (this.preferredActivityFilters.size() == 0) {
                this.preferredActivityFilters = null;
            }
            this.mOriginalPackages = dest.createStringArrayList();
            this.mRealPackage = dest.readString();
            this.mAdoptPermissions = dest.createStringArrayList();
            this.mAppMetaData = dest.readBundle();
            this.mVersionCode = dest.readInt();
            this.mVersionName = dest.readString();
            if (this.mVersionName != null) {
                this.mVersionName = this.mVersionName.intern();
            }
            this.mSharedUserId = dest.readString();
            if (this.mSharedUserId != null) {
                this.mSharedUserId = this.mSharedUserId.intern();
            }
            this.mSharedUserLabel = dest.readInt();
            this.mSignatures = (Signature[]) dest.readParcelableArray(boot, Signature.class);
            this.mCertificates = (Certificate[][]) dest.readSerializable();
            this.mPreferredOrder = dest.readInt();
            this.configPreferences = new ArrayList();
            dest.readParcelableList(this.configPreferences, boot);
            if (this.configPreferences.size() == 0) {
                this.configPreferences = null;
            }
            this.reqFeatures = new ArrayList();
            dest.readParcelableList(this.reqFeatures, boot);
            if (this.reqFeatures.size() == 0) {
                this.reqFeatures = null;
            }
            this.featureGroups = new ArrayList();
            dest.readParcelableList(this.featureGroups, boot);
            if (this.featureGroups.size() == 0) {
                this.featureGroups = null;
            }
            this.installLocation = dest.readInt();
            this.coreApp = dest.readInt() == 1;
            if (dest.readInt() == 1) {
                z = true;
            } else {
                z = false;
            }
            this.mRequiredForAllUsers = z;
            this.mRestrictedAccountType = dest.readString();
            this.mRequiredAccountType = dest.readString();
            this.mOverlayTarget = dest.readString();
            this.mOverlayPriority = dest.readInt();
            if (dest.readInt() == 1) {
                z = true;
            } else {
                z = false;
            }
            this.mIsStaticOverlay = z;
            if (dest.readInt() == 1) {
                z = true;
            } else {
                z = false;
            }
            this.mTrustedOverlay = z;
            this.mSigningKeys = dest.readArraySet(boot);
            this.mUpgradeKeySets = dest.readArraySet(boot);
            this.mKeySetMapping = readKeySetMapping(dest);
            this.cpuAbiOverride = dest.readString();
            if (dest.readInt() == 1) {
                z = true;
            } else {
                z = false;
            }
            this.use32bitAbi = z;
            this.restrictUpdateHash = dest.createByteArray();
            if (dest.readInt() != 1) {
                z2 = false;
            }
            this.visibleToInstantApps = z2;
        }

        private static void internStringArrayList(List<String> list) {
            if (list != null) {
                int N = list.size();
                for (int i = 0; i < N; i++) {
                    list.set(i, ((String) list.get(i)).intern());
                }
            }
        }

        private void fixupOwner(List<? extends Component<?>> list) {
            if (list != null) {
                for (Component<?> c : list) {
                    c.owner = this;
                    if (c instanceof Activity) {
                        ((Activity) c).info.applicationInfo = this.applicationInfo;
                    } else if (c instanceof Service) {
                        ((Service) c).info.applicationInfo = this.applicationInfo;
                    } else if (c instanceof Provider) {
                        ((Provider) c).info.applicationInfo = this.applicationInfo;
                    }
                }
            }
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i;
            int i2 = 1;
            dest.writeString(this.packageName);
            dest.writeString(this.manifestPackageName);
            dest.writeStringArray(this.splitNames);
            dest.writeString(this.volumeUuid);
            dest.writeString(this.codePath);
            dest.writeString(this.baseCodePath);
            dest.writeStringArray(this.splitCodePaths);
            dest.writeInt(this.baseRevisionCode);
            dest.writeIntArray(this.splitRevisionCodes);
            dest.writeIntArray(this.splitFlags);
            dest.writeIntArray(this.splitPrivateFlags);
            dest.writeInt(this.baseHardwareAccelerated ? 1 : 0);
            dest.writeParcelable(this.applicationInfo, flags);
            dest.writeParcelableList(this.permissions, flags);
            dest.writeParcelableList(this.permissionGroups, flags);
            dest.writeParcelableList(this.activities, flags);
            dest.writeParcelableList(this.receivers, flags);
            dest.writeParcelableList(this.providers, flags);
            dest.writeParcelableList(this.services, flags);
            dest.writeParcelableList(this.instrumentation, flags);
            dest.writeStringList(this.requestedPermissions);
            dest.writeStringList(this.protectedBroadcasts);
            dest.writeParcelable(this.parentPackage, flags);
            dest.writeParcelableList(this.childPackages, flags);
            dest.writeString(this.staticSharedLibName);
            dest.writeInt(this.staticSharedLibVersion);
            dest.writeStringList(this.libraryNames);
            dest.writeStringList(this.usesLibraries);
            dest.writeStringList(this.usesOptionalLibraries);
            dest.writeStringArray(this.usesLibraryFiles);
            if (ArrayUtils.isEmpty(this.usesStaticLibraries)) {
                dest.writeInt(-1);
            } else {
                dest.writeInt(this.usesStaticLibraries.size());
                dest.writeStringList(this.usesStaticLibraries);
                dest.writeIntArray(this.usesStaticLibrariesVersions);
                for (String[] usesStaticLibrariesCertDigest : this.usesStaticLibrariesCertDigests) {
                    dest.writeStringArray(usesStaticLibrariesCertDigest);
                }
            }
            dest.writeParcelableList(this.preferredActivityFilters, flags);
            dest.writeStringList(this.mOriginalPackages);
            dest.writeString(this.mRealPackage);
            dest.writeStringList(this.mAdoptPermissions);
            dest.writeBundle(this.mAppMetaData);
            dest.writeInt(this.mVersionCode);
            dest.writeString(this.mVersionName);
            dest.writeString(this.mSharedUserId);
            dest.writeInt(this.mSharedUserLabel);
            dest.writeParcelableArray(this.mSignatures, flags);
            dest.writeSerializable(this.mCertificates);
            dest.writeInt(this.mPreferredOrder);
            dest.writeParcelableList(this.configPreferences, flags);
            dest.writeParcelableList(this.reqFeatures, flags);
            dest.writeParcelableList(this.featureGroups, flags);
            dest.writeInt(this.installLocation);
            if (this.coreApp) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeInt(i);
            if (this.mRequiredForAllUsers) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeInt(i);
            dest.writeString(this.mRestrictedAccountType);
            dest.writeString(this.mRequiredAccountType);
            dest.writeString(this.mOverlayTarget);
            dest.writeInt(this.mOverlayPriority);
            if (this.mIsStaticOverlay) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeInt(i);
            if (this.mTrustedOverlay) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeInt(i);
            dest.writeArraySet(this.mSigningKeys);
            dest.writeArraySet(this.mUpgradeKeySets);
            writeKeySetMapping(dest, this.mKeySetMapping);
            dest.writeString(this.cpuAbiOverride);
            if (this.use32bitAbi) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeInt(i);
            dest.writeByteArray(this.restrictUpdateHash);
            if (!this.visibleToInstantApps) {
                i2 = 0;
            }
            dest.writeInt(i2);
        }

        private static void writeKeySetMapping(Parcel dest, ArrayMap<String, ArraySet<PublicKey>> keySetMapping) {
            if (keySetMapping == null) {
                dest.writeInt(-1);
                return;
            }
            int N = keySetMapping.size();
            dest.writeInt(N);
            for (int i = 0; i < N; i++) {
                dest.writeString((String) keySetMapping.keyAt(i));
                ArraySet<PublicKey> keys = (ArraySet) keySetMapping.valueAt(i);
                if (keys == null) {
                    dest.writeInt(-1);
                } else {
                    int M = keys.size();
                    dest.writeInt(M);
                    for (int j = 0; j < M; j++) {
                        dest.writeSerializable((Serializable) keys.valueAt(j));
                    }
                }
            }
        }

        private static ArrayMap<String, ArraySet<PublicKey>> readKeySetMapping(Parcel in) {
            int N = in.readInt();
            if (N == -1) {
                return null;
            }
            ArrayMap<String, ArraySet<PublicKey>> keySetMapping = new ArrayMap();
            for (int i = 0; i < N; i++) {
                String key = in.readString();
                int M = in.readInt();
                if (M == -1) {
                    keySetMapping.put(key, null);
                } else {
                    ArraySet<PublicKey> keys = new ArraySet(M);
                    for (int j = 0; j < M; j++) {
                        keys.add((PublicKey) in.readSerializable());
                    }
                    keySetMapping.put(key, keys);
                }
            }
            return keySetMapping;
        }
    }

    public static class PackageLite {
        public final String baseCodePath;
        public final int baseRevisionCode;
        public final String codePath;
        public final String[] configForSplit;
        public final boolean coreApp;
        public final boolean debuggable;
        public final boolean extractNativeLibs;
        public final int installLocation;
        public final boolean[] isFeatureSplits;
        public final boolean isolatedSplits;
        public final boolean multiArch;
        public final String packageName;
        public final String[] splitCodePaths;
        public final String[] splitNames;
        public final int[] splitRevisionCodes;
        public final boolean use32bitAbi;
        public final String[] usesSplitNames;
        public final VerifierInfo[] verifiers;
        public final int versionCode;

        public PackageLite(String codePath, ApkLite baseApk, String[] splitNames, boolean[] isFeatureSplits, String[] usesSplitNames, String[] configForSplit, String[] splitCodePaths, int[] splitRevisionCodes) {
            this.packageName = baseApk.packageName;
            this.versionCode = baseApk.versionCode;
            this.installLocation = baseApk.installLocation;
            this.verifiers = baseApk.verifiers;
            this.splitNames = splitNames;
            this.isFeatureSplits = isFeatureSplits;
            this.usesSplitNames = usesSplitNames;
            this.configForSplit = configForSplit;
            this.codePath = codePath;
            this.baseCodePath = baseApk.codePath;
            this.splitCodePaths = splitCodePaths;
            this.baseRevisionCode = baseApk.revisionCode;
            this.splitRevisionCodes = splitRevisionCodes;
            this.coreApp = baseApk.coreApp;
            this.debuggable = baseApk.debuggable;
            this.multiArch = baseApk.multiArch;
            this.use32bitAbi = baseApk.use32bitAbi;
            this.extractNativeLibs = baseApk.extractNativeLibs;
            this.isolatedSplits = baseApk.isolatedSplits;
        }

        public List<String> getAllCodePaths() {
            ArrayList<String> paths = new ArrayList();
            paths.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(paths, this.splitCodePaths);
            }
            return paths;
        }
    }

    public static class PackageParserException extends Exception {
        public final int error;

        public PackageParserException(int error, String detailMessage) {
            super(detailMessage);
            this.error = error;
        }

        public PackageParserException(int error, String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
            this.error = error;
        }
    }

    static class ParsePackageItemArgs {
        final int bannerRes;
        final int iconRes;
        final int labelRes;
        final int logoRes;
        final int nameRes;
        final String[] outError;
        final Package owner;
        final int roundIconRes;
        TypedArray sa;
        String tag;

        ParsePackageItemArgs(Package _owner, String[] _outError, int _nameRes, int _labelRes, int _iconRes, int _roundIconRes, int _logoRes, int _bannerRes) {
            this.owner = _owner;
            this.outError = _outError;
            this.nameRes = _nameRes;
            this.labelRes = _labelRes;
            this.iconRes = _iconRes;
            this.logoRes = _logoRes;
            this.bannerRes = _bannerRes;
            this.roundIconRes = _roundIconRes;
        }
    }

    public static class ParseComponentArgs extends ParsePackageItemArgs {
        final int descriptionRes;
        final int enabledRes;
        int flags;
        final int processRes;
        final String[] sepProcesses;

        public ParseComponentArgs(Package _owner, String[] _outError, int _nameRes, int _labelRes, int _iconRes, int _roundIconRes, int _logoRes, int _bannerRes, String[] _sepProcesses, int _processRes, int _descriptionRes, int _enabledRes) {
            super(_owner, _outError, _nameRes, _labelRes, _iconRes, _roundIconRes, _logoRes, _bannerRes);
            this.sepProcesses = _sepProcesses;
            this.processRes = _processRes;
            this.descriptionRes = _descriptionRes;
            this.enabledRes = _enabledRes;
        }
    }

    public static final class Permission extends Component<IntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Permission>() {
            public Permission createFromParcel(Parcel in) {
                return new Permission(in, null);
            }

            public Permission[] newArray(int size) {
                return new Permission[size];
            }
        };
        public PermissionGroup group;
        public final PermissionInfo info;
        public boolean tree;

        /* synthetic */ Permission(Parcel in, Permission -this1) {
            this(in);
        }

        public Permission(Package _owner) {
            super(_owner);
            this.info = new PermissionInfo();
        }

        public Permission(Package _owner, PermissionInfo _info) {
            super(_owner);
            this.info = _info;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            return "Permission{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.info.name + "}";
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags);
            dest.writeInt(this.tree ? 1 : 0);
            dest.writeParcelable(this.group, flags);
        }

        private Permission(Parcel in) {
            super(in);
            ClassLoader boot = Object.class.getClassLoader();
            this.info = (PermissionInfo) in.readParcelable(boot);
            if (this.info.group != null) {
                this.info.group = this.info.group.intern();
            }
            this.tree = in.readInt() == 1;
            this.group = (PermissionGroup) in.readParcelable(boot);
        }
    }

    public static final class PermissionGroup extends Component<IntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<PermissionGroup>() {
            public PermissionGroup createFromParcel(Parcel in) {
                return new PermissionGroup(in, null);
            }

            public PermissionGroup[] newArray(int size) {
                return new PermissionGroup[size];
            }
        };
        public final PermissionGroupInfo info;

        /* synthetic */ PermissionGroup(Parcel in, PermissionGroup -this1) {
            this(in);
        }

        public PermissionGroup(Package _owner) {
            super(_owner);
            this.info = new PermissionGroupInfo();
        }

        public PermissionGroup(Package _owner, PermissionGroupInfo _info) {
            super(_owner);
            this.info = _info;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            return "PermissionGroup{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.info.name + "}";
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags);
        }

        private PermissionGroup(Parcel in) {
            super(in);
            this.info = (PermissionGroupInfo) in.readParcelable(Object.class.getClassLoader());
        }
    }

    public static final class Provider extends Component<ProviderIntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Provider>() {
            public Provider createFromParcel(Parcel in) {
                return new Provider(in, null);
            }

            public Provider[] newArray(int size) {
                return new Provider[size];
            }
        };
        public final ProviderInfo info;
        public boolean syncable;

        /* synthetic */ Provider(Parcel in, Provider -this1) {
            this(in);
        }

        public Provider(ParseComponentArgs args, ProviderInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
            this.syncable = false;
        }

        public Provider(Provider existingProvider) {
            super((Component) existingProvider);
            this.info = existingProvider.info;
            this.syncable = existingProvider.syncable;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Provider{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags | 2);
            dest.writeInt(this.syncable ? 1 : 0);
        }

        private Provider(Parcel in) {
            super(in);
            this.info = (ProviderInfo) in.readParcelable(Object.class.getClassLoader());
            this.syncable = in.readInt() == 1;
            for (ProviderIntentInfo aii : this.intents) {
                aii.provider = this;
            }
            if (this.info.readPermission != null) {
                this.info.readPermission = this.info.readPermission.intern();
            }
            if (this.info.writePermission != null) {
                this.info.writePermission = this.info.writePermission.intern();
            }
            if (this.info.authority != null) {
                this.info.authority = this.info.authority.intern();
            }
        }
    }

    public static final class ProviderIntentInfo extends IntentInfo {
        public Provider provider;

        public ProviderIntentInfo(Provider provider) {
            this.provider = provider;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ProviderIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.provider.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public ProviderIntentInfo(Parcel in) {
            super(in);
        }
    }

    public static final class Service extends Component<ServiceIntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Service>() {
            public Service createFromParcel(Parcel in) {
                return new Service(in, null);
            }

            public Service[] newArray(int size) {
                return new Service[size];
            }
        };
        public final ServiceInfo info;

        /* synthetic */ Service(Parcel in, Service -this1) {
            this(in);
        }

        public Service(ParseComponentArgs args, ServiceInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Service{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags | 2);
        }

        private Service(Parcel in) {
            super(in);
            this.info = (ServiceInfo) in.readParcelable(Object.class.getClassLoader());
            for (ServiceIntentInfo aii : this.intents) {
                aii.service = this;
            }
            if (this.info.permission != null) {
                this.info.permission = this.info.permission.intern();
            }
        }
    }

    public static final class ServiceIntentInfo extends IntentInfo {
        public Service service;

        public ServiceIntentInfo(Service _service) {
            this.service = _service;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ServiceIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.service.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public ServiceIntentInfo(Parcel in) {
            super(in);
        }
    }

    private static class SplitNameComparator implements Comparator<String> {
        /* synthetic */ SplitNameComparator(SplitNameComparator -this0) {
            this();
        }

        private SplitNameComparator() {
        }

        public int compare(String lhs, String rhs) {
            if (lhs == null) {
                return -1;
            }
            if (rhs == null) {
                return 1;
            }
            return lhs.compareTo(rhs);
        }
    }

    public static class SplitPermissionInfo {
        public final String[] newPerms;
        public final String rootPerm;
        public final int targetSdk;

        public SplitPermissionInfo(String rootPerm, String[] newPerms, int targetSdk) {
            this.rootPerm = rootPerm;
            this.newPerms = newPerms;
            this.targetSdk = targetSdk;
        }
    }

    static {
        boolean z;
        if (Build.IS_DEBUGGABLE) {
            z = SystemProperties.getBoolean(PROPERTY_CHILD_PACKAGES_ENABLED, false);
        } else {
            z = false;
        }
        MULTI_PACKAGE_APK_ENABLED = z;
        CHILD_PACKAGE_TAGS.add(TAG_APPLICATION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_PERMISSION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_PERMISSION_SDK_M);
        CHILD_PACKAGE_TAGS.add(TAG_USES_PERMISSION_SDK_23);
        CHILD_PACKAGE_TAGS.add(TAG_USES_CONFIGURATION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_FEATURE);
        CHILD_PACKAGE_TAGS.add(TAG_FEATURE_GROUP);
        CHILD_PACKAGE_TAGS.add(TAG_USES_SDK);
        CHILD_PACKAGE_TAGS.add(TAG_SUPPORT_SCREENS);
        CHILD_PACKAGE_TAGS.add(TAG_INSTRUMENTATION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_GL_TEXTURE);
        CHILD_PACKAGE_TAGS.add(TAG_COMPATIBLE_SCREENS);
        CHILD_PACKAGE_TAGS.add(TAG_SUPPORTS_INPUT);
        CHILD_PACKAGE_TAGS.add(TAG_EAT_COMMENT);
        SAFE_BROADCASTS.add(Intent.ACTION_BOOT_COMPLETED);
        r0 = new SplitPermissionInfo[3];
        r0[0] = new SplitPermissionInfo(permission.WRITE_EXTERNAL_STORAGE, new String[]{permission.READ_EXTERNAL_STORAGE}, 10001);
        r0[1] = new SplitPermissionInfo(permission.READ_CONTACTS, new String[]{permission.READ_CALL_LOG}, 16);
        r0[2] = new SplitPermissionInfo(permission.WRITE_CONTACTS, new String[]{permission.WRITE_CALL_LOG}, 16);
        SPLIT_PERMISSIONS = r0;
    }

    public PackageParser() {
        this.mMetrics.setToDefaults();
    }

    public void setSeparateProcesses(String[] procs) {
        this.mSeparateProcesses = procs;
    }

    public void setOnlyCoreApps(boolean onlyCoreApps) {
        this.mOnlyCoreApps = onlyCoreApps;
    }

    public void setDisplayMetrics(DisplayMetrics metrics) {
        this.mMetrics = metrics;
    }

    public void setCacheDir(File cacheDir) {
        this.mCacheDir = cacheDir;
    }

    public void setCallback(Callback cb) {
        this.mCallback = cb;
    }

    public static final boolean isApkFile(File file) {
        return isApkPath(file.getName());
    }

    public static boolean isApkPath(String path) {
        return path.endsWith(".apk");
    }

    public static PackageInfo generatePackageInfo(Package p, int[] gids, int flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state) {
        return generatePackageInfo(p, gids, flags, firstInstallTime, lastUpdateTime, grantedPermissions, state, UserHandle.getCallingUserId());
    }

    private static boolean checkUseInstalledOrHidden(int flags, PackageUserState state, ApplicationInfo appInfo) {
        if (state.isAvailable(flags)) {
            return true;
        }
        if (appInfo == null || !appInfo.isSystemApp()) {
            return false;
        }
        return (PackageManager.MATCH_KNOWN_PACKAGES & flags) != 0;
    }

    public static boolean isAvailable(PackageUserState state) {
        return checkUseInstalledOrHidden(0, state, null);
    }

    public static PackageInfo generatePackageInfo(Package p, int[] gids, int flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.applicationInfo) || (p.isMatch(flags) ^ 1) != 0) {
            return null;
        }
        int N;
        ActivityInfo[] res;
        int i;
        int num;
        Activity a;
        int num2;
        PackageInfo pi = new PackageInfo();
        pi.packageName = p.packageName;
        pi.splitNames = p.splitNames;
        pi.versionCode = p.mVersionCode;
        pi.baseRevisionCode = p.baseRevisionCode;
        pi.splitRevisionCodes = p.splitRevisionCodes;
        pi.versionName = p.mVersionName;
        pi.sharedUserId = p.mSharedUserId;
        pi.sharedUserLabel = p.mSharedUserLabel;
        pi.applicationInfo = generateApplicationInfo(p, flags, state, userId);
        pi.installLocation = p.installLocation;
        pi.isStub = p.isStub;
        pi.coreApp = p.coreApp;
        if (!(pi.applicationInfo == null || ((pi.applicationInfo.flags & 1) == 0 && (pi.applicationInfo.flags & 128) == 0))) {
            pi.requiredForAllUsers = p.mRequiredForAllUsers;
        }
        pi.restrictedAccountType = p.mRestrictedAccountType;
        pi.requiredAccountType = p.mRequiredAccountType;
        pi.overlayTarget = p.mOverlayTarget;
        pi.overlayPriority = p.mOverlayPriority;
        if (p.mIsStaticOverlay) {
            pi.overlayFlags |= 2;
        }
        if (p.mTrustedOverlay) {
            pi.overlayFlags |= 4;
        }
        pi.firstInstallTime = firstInstallTime;
        pi.lastUpdateTime = lastUpdateTime;
        if ((flags & 256) != 0) {
            pi.gids = gids;
        }
        if ((flags & 16384) != 0) {
            N = p.configPreferences != null ? p.configPreferences.size() : 0;
            if (N > 0) {
                pi.configPreferences = new ConfigurationInfo[N];
                p.configPreferences.toArray(pi.configPreferences);
            }
            N = p.reqFeatures != null ? p.reqFeatures.size() : 0;
            if (N > 0) {
                pi.reqFeatures = new FeatureInfo[N];
                p.reqFeatures.toArray(pi.reqFeatures);
            }
            N = p.featureGroups != null ? p.featureGroups.size() : 0;
            if (N > 0) {
                pi.featureGroups = new FeatureGroupInfo[N];
                p.featureGroups.toArray(pi.featureGroups);
            }
        }
        if ((flags & 1) != 0) {
            N = p.activities.size();
            if (N > 0) {
                res = new ActivityInfo[N];
                i = 0;
                num = 0;
                while (i < N) {
                    a = (Activity) p.activities.get(i);
                    if (state.isMatch(a.info, flags)) {
                        num2 = num + 1;
                        res[num] = generateActivityInfo(a, flags, state, userId);
                    } else {
                        num2 = num;
                    }
                    i++;
                    num = num2;
                }
                pi.activities = (ActivityInfo[]) ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & 2) != 0) {
            N = p.receivers.size();
            if (N > 0) {
                res = new ActivityInfo[N];
                i = 0;
                num = 0;
                while (i < N) {
                    a = (Activity) p.receivers.get(i);
                    if (state.isMatch(a.info, flags)) {
                        num2 = num + 1;
                        res[num] = generateActivityInfo(a, flags, state, userId);
                    } else {
                        num2 = num;
                    }
                    i++;
                    num = num2;
                }
                pi.receivers = (ActivityInfo[]) ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & 4) != 0) {
            N = p.services.size();
            if (N > 0) {
                ServiceInfo[] res2 = new ServiceInfo[N];
                i = 0;
                num = 0;
                while (i < N) {
                    Service s = (Service) p.services.get(i);
                    if (state.isMatch(s.info, flags)) {
                        num2 = num + 1;
                        res2[num] = generateServiceInfo(s, flags, state, userId);
                    } else {
                        num2 = num;
                    }
                    i++;
                    num = num2;
                }
                pi.services = (ServiceInfo[]) ArrayUtils.trimToSize(res2, num);
            }
        }
        if ((flags & 8) != 0) {
            N = p.providers.size();
            if (N > 0) {
                ProviderInfo[] res3 = new ProviderInfo[N];
                i = 0;
                num = 0;
                while (i < N) {
                    Provider pr = (Provider) p.providers.get(i);
                    if (state.isMatch(pr.info, flags)) {
                        num2 = num + 1;
                        res3[num] = generateProviderInfo(pr, flags, state, userId);
                    } else {
                        num2 = num;
                    }
                    i++;
                    num = num2;
                }
                pi.providers = (ProviderInfo[]) ArrayUtils.trimToSize(res3, num);
            }
        }
        if ((flags & 16) != 0) {
            N = p.instrumentation.size();
            if (N > 0) {
                pi.instrumentation = new InstrumentationInfo[N];
                for (i = 0; i < N; i++) {
                    pi.instrumentation[i] = generateInstrumentationInfo((Instrumentation) p.instrumentation.get(i), flags);
                }
            }
        }
        if ((flags & 4096) != 0) {
            N = p.permissions.size();
            if (N > 0) {
                pi.permissions = new PermissionInfo[N];
                for (i = 0; i < N; i++) {
                    pi.permissions[i] = generatePermissionInfo((Permission) p.permissions.get(i), flags);
                }
            }
            N = p.requestedPermissions.size();
            if (N > 0) {
                pi.requestedPermissions = new String[N];
                pi.requestedPermissionsFlags = new int[N];
                for (i = 0; i < N; i++) {
                    String perm = (String) p.requestedPermissions.get(i);
                    pi.requestedPermissions[i] = perm;
                    int[] iArr = pi.requestedPermissionsFlags;
                    iArr[i] = iArr[i] | 1;
                    if (grantedPermissions != null && grantedPermissions.contains(perm)) {
                        iArr = pi.requestedPermissionsFlags;
                        iArr[i] = iArr[i] | 2;
                    }
                }
            }
        }
        if ((flags & 64) != 0) {
            N = p.mSignatures != null ? p.mSignatures.length : 0;
            if (N > 0) {
                pi.signatures = new Signature[N];
                System.arraycopy(p.mSignatures, 0, pi.signatures, 0, N);
            }
        }
        return pi;
    }

    private static Certificate[][] loadCertificates(StrictJarFile jarFile, ZipEntry entry) throws PackageParserException {
        if (jarFile == null) {
            Log.e(TAG, "loadCertificates come from", new Throwable());
            return null;
        }
        AutoCloseable is = null;
        try {
            is = jarFile.getInputStream(entry);
            readFullyIgnoringContents(is);
            Certificate[][] certificateChains = jarFile.getCertificateChains(entry);
            IoUtils.closeQuietly(is);
            return certificateChains;
        } catch (Exception e) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed reading " + entry.getName() + " in " + jarFile, e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(is);
        }
    }

    public static PackageLite parsePackageLite(File packageFile, int flags) throws PackageParserException {
        if (packageFile.isDirectory()) {
            return parseClusterPackageLite(packageFile, flags);
        }
        return parseMonolithicPackageLite(packageFile, flags);
    }

    private static PackageLite parseMonolithicPackageLite(File packageFile, int flags) throws PackageParserException {
        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "parseApkLite");
        ApkLite baseApk = parseApkLite(packageFile, flags);
        String packagePath = packageFile.getAbsolutePath();
        Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
        return new PackageLite(packagePath, baseApk, null, null, null, null, null, null);
    }

    static PackageLite parseClusterPackageLite(File packageDir, int flags) throws PackageParserException {
        File[] files = packageDir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            throw new PackageParserException(-100, "No packages found in split");
        }
        String packageName = null;
        int versionCode = 0;
        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "parseApkLite");
        ArrayMap<String, ApkLite> apks = new ArrayMap();
        for (File file : files) {
            if (isApkFile(file)) {
                ApkLite lite = parseApkLite(file, flags);
                if (packageName == null) {
                    packageName = lite.packageName;
                    versionCode = lite.versionCode;
                } else {
                    if (packageName.equals(lite.packageName)) {
                        if (versionCode != lite.versionCode) {
                            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Inconsistent version " + lite.versionCode + " in " + file + "; expected " + versionCode);
                        }
                    }
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Inconsistent package " + lite.packageName + " in " + file + "; expected " + packageName);
                }
                if (apks.put(lite.splitName, lite) != null) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Split name " + lite.splitName + " defined more than once; most recent was " + file);
                }
            }
        }
        Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
        ApkLite baseApk = (ApkLite) apks.remove(null);
        if (baseApk == null) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Missing base APK in " + packageDir);
        }
        int size = apks.size();
        String[] splitNames = null;
        boolean[] isFeatureSplits = null;
        String[] usesSplitNames = null;
        String[] configForSplits = null;
        String[] splitCodePaths = null;
        int[] splitRevisionCodes = null;
        if (size > 0) {
            isFeatureSplits = new boolean[size];
            usesSplitNames = new String[size];
            configForSplits = new String[size];
            splitCodePaths = new String[size];
            splitRevisionCodes = new int[size];
            splitNames = (String[]) apks.keySet().toArray(new String[size]);
            Arrays.sort(splitNames, sSplitNameComparator);
            for (int i = 0; i < size; i++) {
                ApkLite apk = (ApkLite) apks.get(splitNames[i]);
                usesSplitNames[i] = apk.usesSplitName;
                isFeatureSplits[i] = apk.isFeatureSplit;
                configForSplits[i] = apk.configForSplit;
                splitCodePaths[i] = apk.codePath;
                splitRevisionCodes[i] = apk.revisionCode;
            }
        }
        return new PackageLite(packageDir.getAbsolutePath(), baseApk, splitNames, isFeatureSplits, usesSplitNames, configForSplits, splitCodePaths, splitRevisionCodes);
    }

    public Package parsePackage(File packageFile, int flags, boolean useCaches) throws PackageParserException {
        Package parsed = useCaches ? getCachedResult(packageFile, flags) : null;
        if (parsed != null) {
            if (packageFile != null) {
                Slog.i(TAG, "parsePackage use cache for " + packageFile.getName() + ", flag=" + flags);
            }
            return parsed;
        }
        long parseTime = LOG_PARSE_TIMINGS ? SystemClock.uptimeMillis() : 0;
        if (packageFile.isDirectory()) {
            parsed = parseClusterPackage(packageFile, flags);
        } else {
            parsed = parseMonolithicPackage(packageFile, flags);
        }
        long cacheTime = LOG_PARSE_TIMINGS ? SystemClock.uptimeMillis() : 0;
        cacheResult(packageFile, flags, parsed);
        if (LOG_PARSE_TIMINGS) {
            parseTime = cacheTime - parseTime;
            cacheTime = SystemClock.uptimeMillis() - cacheTime;
            if (parseTime + cacheTime > 100) {
                Slog.i(TAG, "Parse times for '" + packageFile + "': parse=" + parseTime + "ms, update_cache=" + cacheTime + " ms");
            }
        }
        return parsed;
    }

    public Package parsePackage(File packageFile, int flags) throws PackageParserException {
        return parsePackage(packageFile, flags, false);
    }

    private String getCacheKey(File packageFile, int flags) {
        StringBuilder sb = new StringBuilder(packageFile.getName());
        sb.append('-');
        sb.append(flags);
        return sb.toString();
    }

    protected Package fromCacheEntry(byte[] bytes) {
        return fromCacheEntryStatic(bytes);
    }

    public static Package fromCacheEntryStatic(byte[] bytes) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        new ReadHelper(p).startAndInstall();
        Package pkg = new Package(p);
        p.recycle();
        sCachedPackageReadCount.incrementAndGet();
        return pkg;
    }

    protected byte[] toCacheEntry(Package pkg) {
        return toCacheEntryStatic(pkg);
    }

    public static byte[] toCacheEntryStatic(Package pkg) {
        Parcel p = Parcel.obtain();
        WriteHelper helper = new WriteHelper(p);
        pkg.writeToParcel(p, 0);
        helper.finishAndUninstall();
        byte[] serialized = p.marshall();
        p.recycle();
        return serialized;
    }

    private static boolean isCacheUpToDate(File packageFile, File cacheFile) {
        boolean z = false;
        try {
            if (Os.stat(packageFile.getAbsolutePath()).st_mtime < Os.stat(cacheFile.getAbsolutePath()).st_mtime) {
                z = true;
            }
            return z;
        } catch (ErrnoException ee) {
            if (ee.errno != OsConstants.ENOENT) {
                Slog.w("Error while stating package cache : ", ee);
            }
            return false;
        }
    }

    private Package getCachedResult(File packageFile, int flags) {
        if (this.mCacheDir == null) {
            return null;
        }
        File cacheFile = new File(this.mCacheDir, getCacheKey(packageFile, flags));
        try {
            if (!isCacheUpToDate(packageFile, cacheFile)) {
                return null;
            }
            Package p = fromCacheEntry(IoUtils.readFileAsByteArray(cacheFile.getAbsolutePath()));
            if (this.mCallback != null) {
                String[] overlayApks = this.mCallback.getOverlayApks(p.packageName);
                if (overlayApks != null && overlayApks.length > 0) {
                    for (String overlayApk : overlayApks) {
                        if (!isCacheUpToDate(new File(overlayApk), cacheFile)) {
                            return null;
                        }
                    }
                }
            }
            return p;
        } catch (Throwable e) {
            Slog.w(TAG, "Error reading package cache: ", e);
            cacheFile.delete();
            return null;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void cacheResult(File packageFile, int flags, Package parsed) {
        File cacheFile;
        IOException ioe;
        Throwable th;
        Throwable th2 = null;
        if (this.mCacheDir != null) {
            try {
                cacheFile = new File(this.mCacheDir, getCacheKey(packageFile, flags));
                if (cacheFile.exists() && !cacheFile.delete()) {
                    Slog.e(TAG, "Unable to delete cache file: " + cacheFile);
                }
                byte[] cacheEntry = toCacheEntry(parsed);
                if (cacheEntry != null) {
                    FileOutputStream fos = null;
                    try {
                        FileOutputStream fos2 = new FileOutputStream(cacheFile);
                        try {
                            fos2.write(cacheEntry);
                            if (fos2 != null) {
                                try {
                                    fos2.close();
                                } catch (IOException e) {
                                    ioe = e;
                                    fos = fos2;
                                }
                            }
                            if (th2 != null) {
                                throw th2;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            fos = fos2;
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e2) {
                                    ioe = e2;
                                }
                            }
                            if (th2 == null) {
                                throw th2;
                            }
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e22) {
                                ioe = e22;
                            }
                        }
                        if (th2 == null) {
                            throw th;
                        } else {
                            throw th2;
                        }
                    }
                }
                return;
            } catch (Throwable e3) {
                Slog.w(TAG, "Error saving package cache.", e3);
            }
        } else {
            return;
        }
        Slog.w(TAG, "Error writing cache entry.", ioe);
        cacheFile.delete();
    }

    private Package parseClusterPackage(File packageDir, int flags) throws PackageParserException {
        PackageLite lite = parseClusterPackageLite(packageDir, 0);
        if (!this.mOnlyCoreApps || (lite.coreApp ^ 1) == 0) {
            SplitAssetLoader assetLoader;
            SparseArray splitDependencies = null;
            if (!lite.isolatedSplits || (ArrayUtils.isEmpty(lite.splitNames) ^ 1) == 0) {
                assetLoader = new DefaultSplitAssetLoader(lite, flags);
            } else {
                try {
                    splitDependencies = SplitDependencyLoader.createDependenciesFromPackage(lite);
                    assetLoader = new SplitAssetDependencyLoader(lite, splitDependencies, flags);
                } catch (IllegalDependencyException e) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, e.getMessage());
                }
            }
            try {
                AssetManager assets = assetLoader.getBaseAssetManager();
                File baseApk = new File(lite.baseCodePath);
                Package pkg = parseBaseApk(baseApk, assets, flags);
                if (pkg == null) {
                    throw new PackageParserException(-100, "Failed to parse base APK: " + baseApk);
                }
                if (!ArrayUtils.isEmpty(lite.splitNames)) {
                    int num = lite.splitNames.length;
                    pkg.splitNames = lite.splitNames;
                    pkg.splitCodePaths = lite.splitCodePaths;
                    pkg.splitRevisionCodes = lite.splitRevisionCodes;
                    pkg.splitFlags = new int[num];
                    pkg.splitPrivateFlags = new int[num];
                    pkg.applicationInfo.splitNames = pkg.splitNames;
                    pkg.applicationInfo.splitDependencies = splitDependencies;
                    pkg.applicationInfo.splitClassLoaderNames = new String[num];
                    for (int i = 0; i < num; i++) {
                        parseSplitApk(pkg, i, assetLoader.getSplitAssetManager(i), flags);
                    }
                }
                pkg.setCodePath(packageDir.getAbsolutePath());
                pkg.setUse32bitAbi(lite.use32bitAbi);
                return pkg;
            } finally {
                IoUtils.closeQuietly(assetLoader);
            }
        } else {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Not a coreApp: " + packageDir);
        }
    }

    @Deprecated
    public Package parseMonolithicPackage(File apkFile, int flags) throws PackageParserException {
        AssetManager assets = newConfiguredAssetManager();
        PackageLite lite = parseMonolithicPackageLite(apkFile, flags);
        if (!this.mOnlyCoreApps || lite.coreApp) {
            try {
                Package pkg = parseBaseApk(apkFile, assets, flags);
                pkg.setCodePath(apkFile.getAbsolutePath());
                pkg.setUse32bitAbi(lite.use32bitAbi);
                return pkg;
            } finally {
                IoUtils.closeQuietly(assets);
            }
        } else {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Not a coreApp: " + apkFile);
        }
    }

    private static int loadApkIntoAssetManager(AssetManager assets, String apkPath, int flags) throws PackageParserException {
        if ((flags & 4) == 0 || (isApkPath(apkPath) ^ 1) == 0) {
            int cookie = assets.addAssetPath(apkPath);
            if (cookie != 0) {
                return cookie;
            }
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Failed adding asset path: " + apkPath);
        }
        throw new PackageParserException(-100, "Invalid package file: " + apkPath);
    }

    private Package parseBaseApk(File apkFile, AssetManager assets, int flags) throws PackageParserException {
        PackageParserException e;
        Throwable th;
        Exception e2;
        String apkPath = apkFile.getAbsolutePath();
        String volumeUuid = null;
        if (apkPath.startsWith(MNT_EXPAND)) {
            volumeUuid = apkPath.substring(MNT_EXPAND.length(), apkPath.indexOf(47, MNT_EXPAND.length()));
        }
        this.mParseError = 1;
        this.mArchiveSourcePath = apkFile.getAbsolutePath();
        int cookie = loadApkIntoAssetManager(assets, apkPath, flags);
        XmlResourceParser parser = null;
        Resources res;
        try {
            res = new Resources(assets, this.mMetrics, null);
            try {
                parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
                String[] outError = new String[1];
                Package pkg = parseBaseApk(apkPath, res, parser, flags, outError);
                if (pkg == null) {
                    throw new PackageParserException(this.mParseError, apkPath + " (at " + parser.getPositionDescription() + "): " + outError[0]);
                }
                pkg.setVolumeUuid(volumeUuid);
                pkg.setApplicationVolumeUuid(volumeUuid);
                pkg.setBaseCodePath(apkPath);
                pkg.setSignatures(null);
                IoUtils.closeQuietly(parser);
                return pkg;
            } catch (PackageParserException e3) {
                e = e3;
                try {
                    throw e;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Exception e4) {
                e2 = e4;
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + apkPath, e2);
            }
        } catch (PackageParserException e5) {
            e = e5;
            res = null;
            throw e;
        } catch (Exception e6) {
            e2 = e6;
            res = null;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + apkPath, e2);
        } catch (Throwable th3) {
            th = th3;
            res = null;
            IoUtils.closeQuietly(parser);
            throw th;
        }
    }

    private void parseSplitApk(Package pkg, int splitIndex, AssetManager assets, int flags) throws PackageParserException {
        PackageParserException e;
        Throwable th;
        Throwable e2;
        String apkPath = pkg.splitCodePaths[splitIndex];
        this.mParseError = 1;
        this.mArchiveSourcePath = apkPath;
        int cookie = loadApkIntoAssetManager(assets, apkPath, flags);
        XmlResourceParser parser;
        try {
            Resources resources = new Resources(assets, this.mMetrics, null);
            assets.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, VERSION.RESOURCES_SDK_INT);
            parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
            try {
                String[] outError = new String[1];
                if (parseSplitApk(pkg, resources, parser, flags, splitIndex, outError) == null) {
                    throw new PackageParserException(this.mParseError, apkPath + " (at " + parser.getPositionDescription() + "): " + outError[0]);
                }
                IoUtils.closeQuietly(parser);
            } catch (PackageParserException e3) {
                e = e3;
                try {
                    throw e;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Exception e4) {
                e2 = e4;
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + apkPath, e2);
            }
        } catch (PackageParserException e5) {
            e = e5;
            parser = null;
            throw e;
        } catch (Exception e6) {
            e2 = e6;
            parser = null;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + apkPath, e2);
        } catch (Throwable th3) {
            th = th3;
            parser = null;
            IoUtils.closeQuietly(parser);
            throw th;
        }
    }

    private Package parseSplitApk(Package pkg, Resources res, XmlResourceParser parser, int flags, int splitIndex, String[] outError) throws XmlPullParserException, IOException, PackageParserException {
        XmlResourceParser attrs = parser;
        parsePackageSplitNames(parser, parser);
        this.mParseInstrumentationArgs = null;
        boolean foundApp = false;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                if (!foundApp) {
                    outError[0] = "<manifest> does not contain an <application>";
                    this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_EMPTY;
                }
            } else if (!(type == 3 || type == 4)) {
                if (!parser.getName().equals(TAG_APPLICATION)) {
                    Slog.w(TAG, "Unknown element under <manifest>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                } else if (foundApp) {
                    Slog.w(TAG, "<manifest> has more than one <application>");
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    foundApp = true;
                    if (!parseSplitApplication(pkg, res, parser, flags, splitIndex, outError)) {
                        return null;
                    }
                }
            }
        }
        if (foundApp) {
            outError[0] = "<manifest> does not contain an <application>";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_EMPTY;
        }
        return pkg;
    }

    public static int getApkSigningVersion(Package pkg) {
        try {
            if (ApkSignatureSchemeV2Verifier.hasSignature(pkg.baseCodePath)) {
                return 2;
            }
            return 1;
        } catch (IOException e) {
            return 0;
        }
    }

    public static void populateCertificates(Package pkg, Certificate[][] certificates) throws PackageParserException {
        pkg.mCertificates = null;
        pkg.mSignatures = null;
        pkg.mSigningKeys = null;
        pkg.mCertificates = certificates;
        try {
            int i;
            pkg.mSignatures = convertToSignatures(certificates);
            pkg.mSigningKeys = new ArraySet(certificates.length);
            for (Certificate[] signerCerts : certificates) {
                pkg.mSigningKeys.add(signerCerts[0].getPublicKey());
            }
            int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
            for (i = 0; i < childCount; i++) {
                Package childPkg = (Package) pkg.childPackages.get(i);
                childPkg.mCertificates = pkg.mCertificates;
                childPkg.mSignatures = pkg.mSignatures;
                childPkg.mSigningKeys = pkg.mSigningKeys;
            }
        } catch (CertificateEncodingException e) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + pkg.baseCodePath, e);
        }
    }

    public static void collectCertificates(Package pkg, int parseFlags) throws PackageParserException {
        collectCertificatesInternal(pkg, parseFlags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            Package childPkg = (Package) pkg.childPackages.get(i);
            childPkg.mCertificates = pkg.mCertificates;
            childPkg.mSignatures = pkg.mSignatures;
            childPkg.mSigningKeys = pkg.mSigningKeys;
        }
    }

    private static void collectCertificatesInternal(Package pkg, int parseFlags) throws PackageParserException {
        pkg.mCertificates = null;
        pkg.mSignatures = null;
        pkg.mSigningKeys = null;
        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "collectCertificates");
        try {
            collectCertificates(pkg, new File(pkg.baseCodePath), parseFlags);
            if (!ArrayUtils.isEmpty(pkg.splitCodePaths)) {
                for (String file : pkg.splitCodePaths) {
                    collectCertificates(pkg, new File(file), parseFlags);
                }
            }
            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
        } catch (Throwable th) {
            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
        }
    }

    private static void collectCertificates(Package pkg, File apkFile, int parseFlags) throws PackageParserException {
        Exception e;
        int i;
        Throwable th;
        String apkPath = apkFile.getAbsolutePath();
        boolean verified = false;
        Certificate[][] certificateArr = null;
        Signature[] signatureArr = null;
        try {
            Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "verifyV2");
            certificateArr = ApkSignatureSchemeV2Verifier.verify(apkPath);
            signatureArr = convertToSignatures(certificateArr);
            verified = true;
        } catch (SignatureNotFoundException e2) {
            if ((parseFlags & 2048) != 0) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No APK Signature Scheme v2 signature in ephemeral package " + apkPath, e2);
            } else if (pkg.applicationInfo.isStaticSharedLibrary()) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Static shared libs must use v2 signature scheme " + apkPath);
            }
        } catch (Exception e3) {
            if (pkg.applicationInfo.targetSdkVersion == 0 || pkg.applicationInfo.targetSdkVersion >= 24) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + apkPath + " using APK Signature Scheme v2", e3);
            }
            Slog.d(TAG, "INSTALLATION WARNNING!!!", new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + apkPath + " using APK Signature Scheme v2", e3));
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
        }
        if (verified) {
            if (pkg.mCertificates == null) {
                pkg.mCertificates = certificateArr;
                pkg.mSignatures = signatureArr;
                pkg.mSigningKeys = new ArraySet(certificateArr.length);
                for (Certificate[] signerCerts : certificateArr) {
                    pkg.mSigningKeys.add(signerCerts[0].getPublicKey());
                }
            } else if (!Signature.areExactMatch(pkg.mSignatures, signatureArr)) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES, apkPath + " has mismatched certificates");
            }
        }
        int objectNumber = verified ? 1 : NUMBER_OF_CORES;
        StrictJarFile jarFile = null;
        ArrayMap<String, StrictJarFile> strictJarFiles = new ArrayMap();
        StrictJarFile[] sJarFiles = new StrictJarFile[objectNumber];
        try {
            Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "strictJarFileCtor");
            boolean signatureSchemeRollbackProtectionsEnforced = (parseFlags & 64) == 0 ? pkg.applicationInfo.targetSdkVersion != 0 ? pkg.applicationInfo.targetSdkVersion >= 24 : true : false;
            StrictJarFile strictJarFile = new StrictJarFile(apkPath, verified ^ 1, signatureSchemeRollbackProtectionsEnforced);
            try {
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                ZipEntry manifestEntry = strictJarFile.findEntry(ANDROID_MANIFEST_FILENAME);
                if (manifestEntry == null) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Package " + apkPath + " has no manifest");
                } else if (verified) {
                    if (sIsPerfLockAcquired && sPerfBoost != null) {
                        sPerfBoost.perfLockRelease();
                        sIsPerfLockAcquired = false;
                        Log.d(TAG, "Perflock released for PackageInstall ");
                    }
                    strictJarFiles.clear();
                    if (!verified) {
                        for (i = 0; i < objectNumber; i++) {
                            closeQuietly(sJarFiles[i]);
                        }
                    }
                    closeQuietly(strictJarFile);
                } else {
                    ZipEntry entry;
                    Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "verifyV1");
                    List<ZipEntry> toVerify = new ArrayList();
                    toVerify.add(manifestEntry);
                    if ((parseFlags & 64) == 0) {
                        Iterator<ZipEntry> i2 = strictJarFile.iterator();
                        while (i2.hasNext()) {
                            entry = (ZipEntry) i2.next();
                            if (!entry.isDirectory()) {
                                String entryName = entry.getName();
                                if (!entryName.startsWith("META-INF/")) {
                                    if (!entryName.equals(ANDROID_MANIFEST_FILENAME)) {
                                        toVerify.add(entry);
                                    }
                                }
                            }
                        }
                    }
                    if (sPerfBoost == null) {
                        sPerfBoost = new BoostFramework();
                    }
                    if (!(sPerfBoost == null || (sIsPerfLockAcquired ^ 1) == 0)) {
                        sPerfBoost.perfHint(4232, null, Integer.MAX_VALUE, -1);
                        Log.d(TAG, "perflock acquired for PackageInstall ");
                        sIsPerfLockAcquired = true;
                    }
                    for (i = 0; i < objectNumber; i++) {
                        sJarFiles[i] = new StrictJarFile(apkPath, verified ^ 1, signatureSchemeRollbackProtectionsEnforced);
                    }
                    AnonymousClass1VerificationData vData = new Object() {
                        public Exception exception;
                        public int exceptionFlag;
                        public int index;
                        public Object objWaitAll;
                        public boolean wait;
                    };
                    vData.objWaitAll = new Object();
                    ThreadPoolExecutor verificationExecutor = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES, 1, TimeUnit.SECONDS, new LinkedBlockingQueue());
                    for (final ZipEntry entry2 : toVerify) {
                        final ArrayMap<String, StrictJarFile> arrayMap = strictJarFiles;
                        final StrictJarFile[] strictJarFileArr = sJarFiles;
                        final AnonymousClass1VerificationData anonymousClass1VerificationData = vData;
                        final String str = apkPath;
                        final Package packageR = pkg;
                        Runnable verifyTask = new Runnable() {
                            public void run() {
                                Object obj;
                                try {
                                    StrictJarFile tmp;
                                    long tid = Thread.currentThread().getId();
                                    synchronized (arrayMap) {
                                        if (arrayMap.get(Long.toString(tid)) == null) {
                                            ArrayMap arrayMap = arrayMap;
                                            String l = Long.toString(tid);
                                            StrictJarFile[] strictJarFileArr = strictJarFileArr;
                                            AnonymousClass1VerificationData anonymousClass1VerificationData = anonymousClass1VerificationData;
                                            int i = anonymousClass1VerificationData.index;
                                            anonymousClass1VerificationData.index = i + 1;
                                            arrayMap.put(l, strictJarFileArr[i]);
                                        }
                                        tmp = (StrictJarFile) arrayMap.get(Long.toString(tid));
                                    }
                                    Certificate[][] tempEntryCerts = PackageParser.loadCertificates(tmp, entry2);
                                    if (ArrayUtils.isEmpty(tempEntryCerts)) {
                                        Log.d(PackageParser.TAG, "entryCerts not got, retry");
                                        tempEntryCerts = PackageParser.loadCertificates(tmp, entry2);
                                        if (ArrayUtils.isEmpty(tempEntryCerts)) {
                                            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Package " + str + " has no certificates at entry " + entry2.getName());
                                        }
                                    }
                                    Certificate[][] entryCerts = tempEntryCerts;
                                    Signature[] entrySignatures = PackageParser.convertToSignatures(entryCerts);
                                    synchronized (packageR) {
                                        if (packageR.mCertificates == null) {
                                            packageR.mCertificates = entryCerts;
                                            packageR.mSignatures = entrySignatures;
                                            packageR.mSigningKeys = new ArraySet();
                                            for (Certificate[] certificateArr : entryCerts) {
                                                packageR.mSigningKeys.add(certificateArr[0].getPublicKey());
                                            }
                                        } else if (!Signature.areExactMatch(packageR.mSignatures, entrySignatures)) {
                                            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES, "Package " + str + " has mismatched certificates at entry " + entry2.getName());
                                        }
                                    }
                                } catch (GeneralSecurityException e) {
                                    obj = anonymousClass1VerificationData.objWaitAll;
                                    synchronized (obj) {
                                        anonymousClass1VerificationData.exceptionFlag = PackageManager.INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING;
                                        anonymousClass1VerificationData.exception = e;
                                        e.printStackTrace();
                                        Slog.w(PackageParser.TAG, "1,verifyV1 GeneralSecurityException " + anonymousClass1VerificationData.exceptionFlag);
                                    }
                                } catch (PackageParserException e2) {
                                    obj = anonymousClass1VerificationData.objWaitAll;
                                    synchronized (obj) {
                                        anonymousClass1VerificationData.exceptionFlag = PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION;
                                        anonymousClass1VerificationData.exception = e2;
                                        e2.printStackTrace();
                                        Slog.w(PackageParser.TAG, "2,verifyV1 PackageParserException " + anonymousClass1VerificationData.exceptionFlag);
                                    }
                                }
                            }
                        };
                        synchronized (vData.objWaitAll) {
                            if (vData.exceptionFlag == 0) {
                                verificationExecutor.execute(verifyTask);
                            }
                        }
                    }
                    vData.wait = true;
                    verificationExecutor.shutdown();
                    while (vData.wait && vData.exceptionFlag == 0) {
                        try {
                            vData.wait = verificationExecutor.awaitTermination(50, TimeUnit.MILLISECONDS) ^ 1;
                        } catch (InterruptedException e4) {
                            Slog.w(TAG, "VerifyV1 interrupted while awaiting all threads done...");
                        }
                    }
                    if (vData.wait) {
                        Slog.w(TAG, "verifyV1 Exception " + vData.exceptionFlag);
                        verificationExecutor.shutdownNow();
                    }
                    Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                    if (vData.exceptionFlag != 0) {
                        throw new PackageParserException(vData.exceptionFlag, "Failed to collect certificates from " + apkPath, vData.exception);
                    }
                    if (sIsPerfLockAcquired && sPerfBoost != null) {
                        sPerfBoost.perfLockRelease();
                        sIsPerfLockAcquired = false;
                        Log.d(TAG, "Perflock released for PackageInstall ");
                    }
                    strictJarFiles.clear();
                    if (!verified) {
                        for (i = 0; i < objectNumber; i++) {
                            closeQuietly(sJarFiles[i]);
                        }
                    }
                    closeQuietly(strictJarFile);
                }
            } catch (IOException e5) {
                e3 = e5;
                jarFile = strictJarFile;
                try {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "2,Failed to collect certificates from " + apkPath, e3);
                } catch (Throwable th2) {
                    th = th2;
                    if (sIsPerfLockAcquired && sPerfBoost != null) {
                        sPerfBoost.perfLockRelease();
                        sIsPerfLockAcquired = false;
                        Log.d(TAG, "Perflock released for PackageInstall ");
                    }
                    strictJarFiles.clear();
                    if (!verified) {
                        for (i = 0; i < objectNumber; i++) {
                            closeQuietly(sJarFiles[i]);
                        }
                    }
                    closeQuietly(jarFile);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                jarFile = strictJarFile;
                sPerfBoost.perfLockRelease();
                sIsPerfLockAcquired = false;
                Log.d(TAG, "Perflock released for PackageInstall ");
                strictJarFiles.clear();
                if (verified) {
                    for (i = 0; i < objectNumber; i++) {
                        closeQuietly(sJarFiles[i]);
                    }
                }
                closeQuietly(jarFile);
                throw th;
            }
        } catch (IOException e6) {
            e3 = e6;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "2,Failed to collect certificates from " + apkPath, e3);
        }
    }

    private static Signature[] convertToSignatures(Certificate[][] certs) throws CertificateEncodingException {
        Signature[] res = new Signature[certs.length];
        for (int i = 0; i < certs.length; i++) {
            res[i] = new Signature(certs[i]);
        }
        return res;
    }

    private static AssetManager newConfiguredAssetManager() {
        AssetManager assetManager = new AssetManager();
        assetManager.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, VERSION.RESOURCES_SDK_INT);
        return assetManager;
    }

    public static ApkLite parseApkLite(File apkFile, int flags) throws PackageParserException {
        String apkPath = apkFile.getAbsolutePath();
        AutoCloseable assets = null;
        AutoCloseable parser = null;
        try {
            assets = newConfiguredAssetManager();
            int cookie = assets.addAssetPath(apkPath);
            if (cookie == 0) {
                throw new PackageParserException(-100, "Failed to parse " + apkPath);
            }
            Signature[] signatures;
            Certificate[][] certificates;
            new DisplayMetrics().setToDefaults();
            parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
            if ((flags & 256) != 0) {
                Package tempPkg = new Package((String) null);
                Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "collectCertificates");
                collectCertificates(tempPkg, apkFile, flags);
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                signatures = tempPkg.mSignatures;
                certificates = tempPkg.mCertificates;
            } else {
                signatures = null;
                certificates = null;
            }
            AutoCloseable attrs = parser;
            ApkLite parseApkLite = parseApkLite(apkPath, parser, parser, signatures, certificates);
            IoUtils.closeQuietly(parser);
            IoUtils.closeQuietly(assets);
            return parseApkLite;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to parse " + apkPath, e);
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to parse " + apkPath, e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(parser);
            IoUtils.closeQuietly(assets);
        }
    }

    private static String validateName(String name, boolean requireSeparator, boolean requireFilename) {
        int N = name.length();
        boolean hasSep = false;
        boolean front = true;
        for (int i = 0; i < N; i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                front = false;
            } else if (front || ((c < '0' || c > '9') && c != '_')) {
                if (c != '.') {
                    return "bad character '" + c + "'";
                }
                hasSep = true;
                front = true;
            }
        }
        if (requireFilename && (FileUtils.isValidExtFilename(name) ^ 1) != 0) {
            return "Invalid filename";
        }
        String str = (hasSep || (requireSeparator ^ 1) != 0) ? null : "must have at least one '.' separator";
        return str;
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "JianJun.Dan@Plf.SDK : Modify for oppo package", property = OppoRomType.ROM)
    private static Pair<String, String> parsePackageSplitNames(XmlPullParser parser, AttributeSet attrs) throws IOException, XmlPullParserException, PackageParserException {
        int type;
        do {
            type = parser.next();
            if (type == 2) {
                break;
            }
        } while (type != 1);
        if (type != 2) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No start tag found");
        } else if (parser.getName().equals(TAG_MANIFEST)) {
            String error;
            String packageName = attrs.getAttributeValue(null, "package");
            if (!"android".equals(packageName)) {
                error = PackageParser.filterNameError(packageName, validateName(packageName, true, true));
                if (error != null) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, "Invalid manifest package: " + error);
                }
            }
            Object splitName = attrs.getAttributeValue(null, "split");
            if (splitName != null) {
                if (splitName.length() == 0) {
                    splitName = null;
                } else {
                    error = validateName(splitName, false, false);
                    if (error != null) {
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, "Invalid manifest split: " + error);
                    }
                }
            }
            String intern = packageName.intern();
            if (splitName != null) {
                splitName = splitName.intern();
            }
            return Pair.create(intern, splitName);
        } else {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No <manifest> tag");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static ApkLite parseApkLite(String codePath, XmlPullParser parser, AttributeSet attrs, Signature[] signatures, Certificate[][] certificates) throws IOException, XmlPullParserException, PackageParserException {
        int i;
        String attr;
        Pair<String, String> packageSplit = parsePackageSplitNames(parser, attrs);
        int installLocation = -1;
        int versionCode = 0;
        int revisionCode = 0;
        boolean coreApp = false;
        boolean debuggable = false;
        boolean multiArch = false;
        boolean use32bitAbi = false;
        boolean extractNativeLibs = true;
        boolean isolatedSplits = false;
        boolean isFeatureSplit = false;
        String configForSplit = null;
        String usesSplitName = null;
        for (i = 0; i < attrs.getAttributeCount(); i++) {
            attr = attrs.getAttributeName(i);
            if (attr.equals("installLocation")) {
                installLocation = attrs.getAttributeIntValue(i, -1);
            } else {
                if (attr.equals("versionCode")) {
                    versionCode = attrs.getAttributeIntValue(i, 0);
                } else {
                    if (attr.equals("revisionCode")) {
                        revisionCode = attrs.getAttributeIntValue(i, 0);
                    } else {
                        if (attr.equals("coreApp")) {
                            coreApp = attrs.getAttributeBooleanValue(i, false);
                        } else {
                            if (attr.equals("isolatedSplits")) {
                                isolatedSplits = attrs.getAttributeBooleanValue(i, false);
                            } else {
                                if (attr.equals("configForSplit")) {
                                    configForSplit = attrs.getAttributeValue(i);
                                } else {
                                    if (attr.equals("isFeatureSplit")) {
                                        isFeatureSplit = attrs.getAttributeBooleanValue(i, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        String packageName = attrs.getAttributeValue(null, "package");
        if (!(coreApp || packageName == null)) {
            if (!packageName.equals("com.google.android.inputmethod.latin")) {
                if (!packageName.equals("com.nuance.swype.oppo")) {
                    if (!packageName.equals("com.google.android.keep")) {
                        if (!packageName.equals("com.emoji.keyboard.touchpal")) {
                        }
                    }
                }
            }
            Slog.d(TAG, "Parser swype for enter password in ecryption mode");
            coreApp = true;
        }
        int searchDepth = parser.getDepth() + 1;
        List<VerifierInfo> verifiers = new ArrayList();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() < searchDepth)) {
            } else if (!(type == 3 || type == 4 || parser.getDepth() != searchDepth)) {
                if (TAG_PACKAGE_VERIFIER.equals(parser.getName())) {
                    VerifierInfo verifier = parseVerifier(attrs);
                    if (verifier != null) {
                        verifiers.add(verifier);
                    }
                } else if (TAG_APPLICATION.equals(parser.getName())) {
                    for (i = 0; i < attrs.getAttributeCount(); i++) {
                        attr = attrs.getAttributeName(i);
                        if ("debuggable".equals(attr)) {
                            debuggable = attrs.getAttributeBooleanValue(i, false);
                        }
                        if ("multiArch".equals(attr)) {
                            multiArch = attrs.getAttributeBooleanValue(i, false);
                        }
                        if ("use32bitAbi".equals(attr)) {
                            use32bitAbi = attrs.getAttributeBooleanValue(i, false);
                        }
                        if ("extractNativeLibs".equals(attr)) {
                            extractNativeLibs = attrs.getAttributeBooleanValue(i, true);
                        }
                    }
                } else if (!TAG_USES_SPLIT.equals(parser.getName())) {
                    continue;
                } else if (usesSplitName != null) {
                    Slog.w(TAG, "Only one <uses-split> permitted. Ignoring others.");
                } else {
                    usesSplitName = attrs.getAttributeValue(ANDROID_RESOURCES, MidiDeviceInfo.PROPERTY_NAME);
                    if (usesSplitName == null) {
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "<uses-split> tag requires 'android:name' attribute");
                    }
                }
            }
        }
        return new ApkLite(codePath, (String) packageSplit.first, (String) packageSplit.second, isFeatureSplit, configForSplit, usesSplitName, versionCode, revisionCode, installLocation, verifiers, signatures, certificates, coreApp, debuggable, multiArch, use32bitAbi, extractNativeLibs, isolatedSplits);
    }

    private boolean parseBaseApkChild(Package parentPkg, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        if (parentPkg.childPackages == null || parentPkg.childPackages.size() + 2 <= 5) {
            String childPackageName = parser.getAttributeValue(null, "package");
            String message;
            if (validateName(childPackageName, true, false) != null) {
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
                return false;
            } else if (childPackageName.equals(parentPkg.packageName)) {
                message = "Child package name cannot be equal to parent package name: " + parentPkg.packageName;
                Slog.w(TAG, message);
                outError[0] = message;
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            } else if (parentPkg.hasChildPackage(childPackageName)) {
                message = "Duplicate child package:" + childPackageName;
                Slog.w(TAG, message);
                outError[0] = message;
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            } else {
                Package childPkg = new Package(childPackageName);
                childPkg.mVersionCode = parentPkg.mVersionCode;
                childPkg.baseRevisionCode = parentPkg.baseRevisionCode;
                childPkg.mVersionName = parentPkg.mVersionName;
                childPkg.applicationInfo.targetSdkVersion = parentPkg.applicationInfo.targetSdkVersion;
                childPkg.applicationInfo.minSdkVersion = parentPkg.applicationInfo.minSdkVersion;
                childPkg = parseBaseApkCommon(childPkg, CHILD_PACKAGE_TAGS, res, parser, flags, outError);
                if (childPkg == null) {
                    return false;
                }
                if (parentPkg.childPackages == null) {
                    parentPkg.childPackages = new ArrayList();
                }
                parentPkg.childPackages.add(childPkg);
                childPkg.parentPackage = parentPkg;
                return true;
            }
        }
        outError[0] = "Maximum number of packages per APK is: 5";
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private Package parseBaseApk(String apkPath, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        try {
            Pair<String, String> packageSplit = parsePackageSplitNames(parser, parser);
            String pkgName = packageSplit.first;
            String splitName = packageSplit.second;
            if (TextUtils.isEmpty(splitName)) {
                if (this.mCallback != null) {
                    String[] overlayPaths = this.mCallback.getOverlayPaths(pkgName, apkPath);
                    if (overlayPaths != null && overlayPaths.length > 0) {
                        for (String overlayPath : overlayPaths) {
                            res.getAssets().addOverlayPath(overlayPath);
                        }
                    }
                }
                Package pkg = new Package(pkgName);
                TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifest);
                int integer = sa.getInteger(1, 0);
                pkg.applicationInfo.versionCode = integer;
                pkg.mVersionCode = integer;
                pkg.baseRevisionCode = sa.getInteger(5, 0);
                pkg.mVersionName = sa.getNonConfigurationString(2, 0);
                if (pkg.mVersionName != null) {
                    pkg.mVersionName = pkg.mVersionName.intern();
                }
                pkg.coreApp = parser.getAttributeBooleanValue(null, "coreApp", false);
                sa.recycle();
                return parseBaseApkCommon(pkg, null, res, parser, flags, outError);
            }
            outError[0] = "Expected base APK, but found split " + splitName;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return null;
        } catch (PackageParserException e) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return null;
        }
    }

    @android.annotation.OppoHook(level = android.annotation.OppoHook.OppoHookType.CHANGE_CODE, note = "JianJun.Dan@Plf.SDK : Modify for oppo package", property = android.annotation.OppoHook.OppoRomType.ROM)
    private android.content.pm.PackageParser.Package parseBaseApkCommon(android.content.pm.PackageParser.Package r57, java.util.Set<java.lang.String> r58, android.content.res.Resources r59, android.content.res.XmlResourceParser r60, int r61, java.lang.String[] r62) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r24_2 'implicitPerms' java.lang.StringBuilder) in PHI: PHI: (r24_3 'implicitPerms' java.lang.StringBuilder) = (r24_2 'implicitPerms' java.lang.StringBuilder), (r24_1 'implicitPerms' java.lang.StringBuilder) binds: {(r24_2 'implicitPerms' java.lang.StringBuilder)=B:267:0x0869, (r24_1 'implicitPerms' java.lang.StringBuilder)=B:270:0x089b}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
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
        r56 = this;
        r5 = 0;
        r0 = r56;
        r0.mParseInstrumentationArgs = r5;
        r18 = 0;
        r5 = com.android.internal.R.styleable.AndroidManifest;
        r0 = r59;
        r1 = r60;
        r42 = r0.obtainAttributes(r1, r5);
        r5 = 0;
        r6 = 0;
        r0 = r42;
        r44 = r0.getNonConfigurationString(r5, r6);
        if (r44 == 0) goto L_0x0098;
    L_0x001b:
        r5 = r44.length();
        if (r5 <= 0) goto L_0x0098;
    L_0x0021:
        r0 = r61;
        r5 = r0 & 2048;
        if (r5 == 0) goto L_0x0035;
    L_0x0027:
        r5 = "sharedUserId not allowed in ephemeral application";
        r6 = 0;
        r62[r6] = r5;
        r5 = -107; // 0xffffffffffffff95 float:NaN double:NaN;
        r0 = r56;
        r0.mParseError = r5;
        r5 = 0;
        return r5;
    L_0x0035:
        r0 = r57;
        r5 = r0.packageName;
        r6 = 1;
        r7 = 0;
        r0 = r44;
        r6 = validateName(r0, r6, r7);
        r34 = android.content.pm.ColorInjector.PackageParser.filterNameError(r5, r6);
        if (r34 == 0) goto L_0x0084;
    L_0x0047:
        r5 = "android";
        r0 = r57;
        r6 = r0.packageName;
        r5 = r5.equals(r6);
        r5 = r5 ^ 1;
        if (r5 == 0) goto L_0x0084;
    L_0x0056:
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "<manifest> specifies bad sharedUserId name \"";
        r5 = r5.append(r6);
        r0 = r44;
        r5 = r5.append(r0);
        r6 = "\": ";
        r5 = r5.append(r6);
        r0 = r34;
        r5 = r5.append(r0);
        r5 = r5.toString();
        r6 = 0;
        r62[r6] = r5;
        r5 = -107; // 0xffffffffffffff95 float:NaN double:NaN;
        r0 = r56;
        r0.mParseError = r5;
        r5 = 0;
        return r5;
    L_0x0084:
        r5 = r44.intern();
        r0 = r57;
        r0.mSharedUserId = r5;
        r5 = 3;
        r6 = 0;
        r0 = r42;
        r5 = r0.getResourceId(r5, r6);
        r0 = r57;
        r0.mSharedUserLabel = r5;
    L_0x0098:
        r5 = 4;
        r6 = -1;
        r0 = r42;
        r5 = r0.getInteger(r5, r6);
        r0 = r57;
        r0.installLocation = r5;
        r0 = r57;
        r5 = r0.applicationInfo;
        r0 = r57;
        r6 = r0.installLocation;
        r5.installLocation = r6;
        r5 = 7;
        r6 = 1;
        r0 = r42;
        r51 = r0.getInteger(r5, r6);
        r0 = r57;
        r5 = r0.applicationInfo;
        r0 = r51;
        r5.targetSandboxVersion = r0;
        r5 = r61 & 16;
        if (r5 == 0) goto L_0x00cc;
    L_0x00c2:
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = r5.privateFlags;
        r6 = r6 | 4;
        r5.privateFlags = r6;
    L_0x00cc:
        r5 = r61 & 32;
        if (r5 == 0) goto L_0x00db;
    L_0x00d0:
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = r5.flags;
        r7 = 262144; // 0x40000 float:3.67342E-40 double:1.295163E-318;
        r6 = r6 | r7;
        r5.flags = r6;
    L_0x00db:
        r5 = 6;
        r6 = 0;
        r0 = r42;
        r5 = r0.getBoolean(r5, r6);
        if (r5 == 0) goto L_0x00f1;
    L_0x00e5:
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = r5.privateFlags;
        r7 = 32768; // 0x8000 float:4.5918E-41 double:1.61895E-319;
        r6 = r6 | r7;
        r5.privateFlags = r6;
    L_0x00f1:
        r47 = 1;
        r46 = 1;
        r45 = 1;
        r48 = 1;
        r41 = 1;
        r13 = 1;
        r37 = r60.getDepth();
    L_0x0100:
        r54 = r60.next();
        r5 = 1;
        r0 = r54;
        if (r0 == r5) goto L_0x07ee;
    L_0x0109:
        r5 = 3;
        r0 = r54;
        if (r0 != r5) goto L_0x0116;
    L_0x010e:
        r5 = r60.getDepth();
        r0 = r37;
        if (r5 <= r0) goto L_0x07ee;
    L_0x0116:
        r5 = 3;
        r0 = r54;
        if (r0 == r5) goto L_0x0100;
    L_0x011b:
        r5 = 4;
        r0 = r54;
        if (r0 == r5) goto L_0x0100;
    L_0x0120:
        r49 = r60.getName();
        if (r58 == 0) goto L_0x0170;
    L_0x0126:
        r0 = r58;
        r1 = r49;
        r5 = r0.contains(r1);
        r5 = r5 ^ 1;
        if (r5 == 0) goto L_0x0170;
    L_0x0132:
        r5 = "PackageParser";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "Skipping unsupported element under <manifest>: ";
        r6 = r6.append(r7);
        r0 = r49;
        r6 = r6.append(r0);
        r7 = " at ";
        r6 = r6.append(r7);
        r0 = r56;
        r7 = r0.mArchiveSourcePath;
        r6 = r6.append(r7);
        r7 = " ";
        r6 = r6.append(r7);
        r7 = r60.getPositionDescription();
        r6 = r6.append(r7);
        r6 = r6.toString();
        android.util.Slog.w(r5, r6);
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x0170:
        r5 = "application";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x01a1;
    L_0x017b:
        if (r18 == 0) goto L_0x018b;
    L_0x017d:
        r5 = "PackageParser";
        r6 = "<manifest> has more than one <application>";
        android.util.Slog.w(r5, r6);
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x018b:
        r18 = 1;
        r5 = r56;
        r6 = r57;
        r7 = r59;
        r8 = r60;
        r9 = r61;
        r10 = r62;
        r5 = r5.parseBaseApplication(r6, r7, r8, r9, r10);
        if (r5 != 0) goto L_0x0100;
    L_0x019f:
        r5 = 0;
        return r5;
    L_0x01a1:
        r5 = "overlay";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x0274;
    L_0x01ac:
        r5 = com.android.internal.R.styleable.AndroidManifestResourceOverlay;
        r0 = r59;
        r1 = r60;
        r42 = r0.obtainAttributes(r1, r5);
        r5 = 1;
        r0 = r42;
        r5 = r0.getString(r5);
        r0 = r57;
        r0.mOverlayTarget = r5;
        r5 = 0;
        r6 = 0;
        r0 = r42;
        r5 = r0.getInt(r5, r6);
        r0 = r57;
        r0.mOverlayPriority = r5;
        r5 = 2;
        r6 = 0;
        r0 = r42;
        r5 = r0.getBoolean(r5, r6);
        r0 = r57;
        r0.mIsStaticOverlay = r5;
        r5 = 3;
        r0 = r42;
        r39 = r0.getString(r5);
        r5 = 4;
        r0 = r42;
        r40 = r0.getString(r5);
        r42.recycle();
        r0 = r57;
        r5 = r0.mOverlayTarget;
        if (r5 != 0) goto L_0x01fe;
    L_0x01f0:
        r5 = "<overlay> does not specify a target package";
        r6 = 0;
        r62[r6] = r5;
        r5 = -108; // 0xffffffffffffff94 float:NaN double:NaN;
        r0 = r56;
        r0.mParseError = r5;
        r5 = 0;
        return r5;
    L_0x01fe:
        r0 = r57;
        r5 = r0.mOverlayPriority;
        if (r5 < 0) goto L_0x020c;
    L_0x0204:
        r0 = r57;
        r5 = r0.mOverlayPriority;
        r6 = 9999; // 0x270f float:1.4012E-41 double:4.94E-320;
        if (r5 <= r6) goto L_0x021a;
    L_0x020c:
        r5 = "<overlay> priority must be between 0 and 9999";
        r6 = 0;
        r62[r6] = r5;
        r5 = -108; // 0xffffffffffffff94 float:NaN double:NaN;
        r0 = r56;
        r0.mParseError = r5;
        r5 = 0;
        return r5;
    L_0x021a:
        r0 = r56;
        r1 = r39;
        r2 = r40;
        r5 = r0.checkOverlayRequiredSystemProperty(r1, r2);
        if (r5 != 0) goto L_0x026f;
    L_0x0226:
        r5 = "PackageParser";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "Skipping target and overlay pair ";
        r6 = r6.append(r7);
        r0 = r57;
        r7 = r0.mOverlayTarget;
        r6 = r6.append(r7);
        r7 = " and ";
        r6 = r6.append(r7);
        r0 = r57;
        r7 = r0.baseCodePath;
        r6 = r6.append(r7);
        r7 = ": overlay ignored due to required system property: ";
        r6 = r6.append(r7);
        r0 = r39;
        r6 = r6.append(r0);
        r7 = " with value: ";
        r6 = r6.append(r7);
        r0 = r40;
        r6 = r6.append(r0);
        r6 = r6.toString();
        android.util.Slog.i(r5, r6);
        r5 = 0;
        return r5;
    L_0x026f:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x0274:
        r5 = "key-sets";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x0291;
    L_0x027f:
        r0 = r56;
        r1 = r57;
        r2 = r59;
        r3 = r60;
        r4 = r62;
        r5 = r0.parseKeySets(r1, r2, r3, r4);
        if (r5 != 0) goto L_0x0100;
    L_0x028f:
        r5 = 0;
        return r5;
    L_0x0291:
        r5 = "permission-group";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x02b0;
    L_0x029c:
        r5 = r56;
        r6 = r57;
        r7 = r61;
        r8 = r59;
        r9 = r60;
        r10 = r62;
        r5 = r5.parsePermissionGroup(r6, r7, r8, r9, r10);
        if (r5 != 0) goto L_0x0100;
    L_0x02ae:
        r5 = 0;
        return r5;
    L_0x02b0:
        r5 = "permission";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x02cd;
    L_0x02bb:
        r0 = r56;
        r1 = r57;
        r2 = r59;
        r3 = r60;
        r4 = r62;
        r5 = r0.parsePermission(r1, r2, r3, r4);
        if (r5 != 0) goto L_0x0100;
    L_0x02cb:
        r5 = 0;
        return r5;
    L_0x02cd:
        r5 = "permission-tree";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x02ea;
    L_0x02d8:
        r0 = r56;
        r1 = r57;
        r2 = r59;
        r3 = r60;
        r4 = r62;
        r5 = r0.parsePermissionTree(r1, r2, r3, r4);
        if (r5 != 0) goto L_0x0100;
    L_0x02e8:
        r5 = 0;
        return r5;
    L_0x02ea:
        r5 = "uses-permission";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x0305;
    L_0x02f5:
        r0 = r56;
        r1 = r57;
        r2 = r59;
        r3 = r60;
        r5 = r0.parseUsesPermission(r1, r2, r3);
        if (r5 != 0) goto L_0x0100;
    L_0x0303:
        r5 = 0;
        return r5;
    L_0x0305:
        r5 = "uses-permission-sdk-m";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 != 0) goto L_0x031b;
    L_0x0310:
        r5 = "uses-permission-sdk-23";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x032b;
    L_0x031b:
        r0 = r56;
        r1 = r57;
        r2 = r59;
        r3 = r60;
        r5 = r0.parseUsesPermission(r1, r2, r3);
        if (r5 != 0) goto L_0x0100;
    L_0x0329:
        r5 = 0;
        return r5;
    L_0x032b:
        r5 = "uses-configuration";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x0397;
    L_0x0336:
        r14 = new android.content.pm.ConfigurationInfo;
        r14.<init>();
        r5 = com.android.internal.R.styleable.AndroidManifestUsesConfiguration;
        r0 = r59;
        r1 = r60;
        r42 = r0.obtainAttributes(r1, r5);
        r5 = 0;
        r6 = 0;
        r0 = r42;
        r5 = r0.getInt(r5, r6);
        r14.reqTouchScreen = r5;
        r5 = 1;
        r6 = 0;
        r0 = r42;
        r5 = r0.getInt(r5, r6);
        r14.reqKeyboardType = r5;
        r5 = 2;
        r6 = 0;
        r0 = r42;
        r5 = r0.getBoolean(r5, r6);
        if (r5 == 0) goto L_0x0369;
    L_0x0363:
        r5 = r14.reqInputFeatures;
        r5 = r5 | 1;
        r14.reqInputFeatures = r5;
    L_0x0369:
        r5 = 3;
        r6 = 0;
        r0 = r42;
        r5 = r0.getInt(r5, r6);
        r14.reqNavigation = r5;
        r5 = 4;
        r6 = 0;
        r0 = r42;
        r5 = r0.getBoolean(r5, r6);
        if (r5 == 0) goto L_0x0383;
    L_0x037d:
        r5 = r14.reqInputFeatures;
        r5 = r5 | 2;
        r14.reqInputFeatures = r5;
    L_0x0383:
        r42.recycle();
        r0 = r57;
        r5 = r0.configPreferences;
        r5 = com.android.internal.util.ArrayUtils.add(r5, r14);
        r0 = r57;
        r0.configPreferences = r5;
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x0397:
        r5 = "uses-feature";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x03dc;
    L_0x03a2:
        r0 = r56;
        r1 = r59;
        r2 = r60;
        r17 = r0.parseUsesFeature(r1, r2);
        r0 = r57;
        r5 = r0.reqFeatures;
        r0 = r17;
        r5 = com.android.internal.util.ArrayUtils.add(r5, r0);
        r0 = r57;
        r0.reqFeatures = r5;
        r0 = r17;
        r5 = r0.name;
        if (r5 != 0) goto L_0x03d7;
    L_0x03c0:
        r14 = new android.content.pm.ConfigurationInfo;
        r14.<init>();
        r0 = r17;
        r5 = r0.reqGlEsVersion;
        r14.reqGlEsVersion = r5;
        r0 = r57;
        r5 = r0.configPreferences;
        r5 = com.android.internal.util.ArrayUtils.add(r5, r14);
        r0 = r57;
        r0.configPreferences = r5;
    L_0x03d7:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x03dc:
        r5 = "feature-group";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x04a2;
    L_0x03e7:
        r19 = new android.content.pm.FeatureGroupInfo;
        r19.<init>();
        r16 = 0;
        r26 = r60.getDepth();
    L_0x03f2:
        r54 = r60.next();
        r5 = 1;
        r0 = r54;
        if (r0 == r5) goto L_0x0476;
    L_0x03fb:
        r5 = 3;
        r0 = r54;
        if (r0 != r5) goto L_0x0408;
    L_0x0400:
        r5 = r60.getDepth();
        r0 = r26;
        if (r5 <= r0) goto L_0x0476;
    L_0x0408:
        r5 = 3;
        r0 = r54;
        if (r0 == r5) goto L_0x03f2;
    L_0x040d:
        r5 = 4;
        r0 = r54;
        if (r0 == r5) goto L_0x03f2;
    L_0x0412:
        r27 = r60.getName();
        r5 = "uses-feature";
        r0 = r27;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x043b;
    L_0x0421:
        r0 = r56;
        r1 = r59;
        r2 = r60;
        r15 = r0.parseUsesFeature(r1, r2);
        r5 = r15.flags;
        r5 = r5 | 1;
        r15.flags = r5;
        r0 = r16;
        r16 = com.android.internal.util.ArrayUtils.add(r0, r15);
    L_0x0437:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x03f2;
    L_0x043b:
        r5 = "PackageParser";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "Unknown element under <feature-group>: ";
        r6 = r6.append(r7);
        r0 = r27;
        r6 = r6.append(r0);
        r7 = " at ";
        r6 = r6.append(r7);
        r0 = r56;
        r7 = r0.mArchiveSourcePath;
        r6 = r6.append(r7);
        r7 = " ";
        r6 = r6.append(r7);
        r7 = r60.getPositionDescription();
        r6 = r6.append(r7);
        r6 = r6.toString();
        android.util.Slog.w(r5, r6);
        goto L_0x0437;
    L_0x0476:
        if (r16 == 0) goto L_0x0492;
    L_0x0478:
        r5 = r16.size();
        r5 = new android.content.pm.FeatureInfo[r5];
        r0 = r19;
        r0.features = r5;
        r0 = r19;
        r5 = r0.features;
        r0 = r16;
        r5 = r0.toArray(r5);
        r5 = (android.content.pm.FeatureInfo[]) r5;
        r0 = r19;
        r0.features = r5;
    L_0x0492:
        r0 = r57;
        r5 = r0.featureGroups;
        r0 = r19;
        r5 = com.android.internal.util.ArrayUtils.add(r5, r0);
        r0 = r57;
        r0.featureGroups = r5;
        goto L_0x0100;
    L_0x04a2:
        r5 = "uses-sdk";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x055d;
    L_0x04ad:
        r5 = SDK_VERSION;
        if (r5 <= 0) goto L_0x0558;
    L_0x04b1:
        r5 = com.android.internal.R.styleable.AndroidManifestUsesSdk;
        r0 = r59;
        r1 = r60;
        r42 = r0.obtainAttributes(r1, r5);
        r32 = 1;
        r30 = 0;
        r53 = 0;
        r50 = 0;
        r5 = 0;
        r0 = r42;
        r55 = r0.peekValue(r5);
        if (r55 == 0) goto L_0x04e3;
    L_0x04cc:
        r0 = r55;
        r5 = r0.type;
        r6 = 3;
        if (r5 != r6) goto L_0x0520;
    L_0x04d3:
        r0 = r55;
        r5 = r0.string;
        if (r5 == 0) goto L_0x0520;
    L_0x04d9:
        r0 = r55;
        r5 = r0.string;
        r30 = r5.toString();
        r50 = r30;
    L_0x04e3:
        r5 = 1;
        r0 = r42;
        r55 = r0.peekValue(r5);
        if (r55 == 0) goto L_0x0505;
    L_0x04ec:
        r0 = r55;
        r5 = r0.type;
        r6 = 3;
        if (r5 != r6) goto L_0x0529;
    L_0x04f3:
        r0 = r55;
        r5 = r0.string;
        if (r5 == 0) goto L_0x0529;
    L_0x04f9:
        r0 = r55;
        r5 = r0.string;
        r50 = r5.toString();
        if (r30 != 0) goto L_0x0505;
    L_0x0503:
        r30 = r50;
    L_0x0505:
        r42.recycle();
        r5 = SDK_VERSION;
        r6 = SDK_CODENAMES;
        r0 = r32;
        r1 = r30;
        r2 = r62;
        r31 = computeMinSdkVersion(r0, r1, r5, r6, r2);
        if (r31 >= 0) goto L_0x0530;
    L_0x0518:
        r5 = -12;
        r0 = r56;
        r0.mParseError = r5;
        r5 = 0;
        return r5;
    L_0x0520:
        r0 = r55;
        r0 = r0.data;
        r32 = r0;
        r53 = r32;
        goto L_0x04e3;
    L_0x0529:
        r0 = r55;
        r0 = r0.data;
        r53 = r0;
        goto L_0x0505;
    L_0x0530:
        r5 = SDK_VERSION;
        r6 = SDK_CODENAMES;
        r0 = r53;
        r1 = r50;
        r2 = r62;
        r52 = computeTargetSdkVersion(r0, r1, r5, r6, r2);
        if (r52 >= 0) goto L_0x0548;
    L_0x0540:
        r5 = -12;
        r0 = r56;
        r0.mParseError = r5;
        r5 = 0;
        return r5;
    L_0x0548:
        r0 = r57;
        r5 = r0.applicationInfo;
        r0 = r31;
        r5.minSdkVersion = r0;
        r0 = r57;
        r5 = r0.applicationInfo;
        r0 = r52;
        r5.targetSdkVersion = r0;
    L_0x0558:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x055d:
        r5 = "supports-screens";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x05d9;
    L_0x0568:
        r5 = com.android.internal.R.styleable.AndroidManifestSupportsScreens;
        r0 = r59;
        r1 = r60;
        r42 = r0.obtainAttributes(r1, r5);
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = 6;
        r7 = 0;
        r0 = r42;
        r6 = r0.getInteger(r6, r7);
        r5.requiresSmallestWidthDp = r6;
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = 7;
        r7 = 0;
        r0 = r42;
        r6 = r0.getInteger(r6, r7);
        r5.compatibleWidthLimitDp = r6;
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = 8;
        r7 = 0;
        r0 = r42;
        r6 = r0.getInteger(r6, r7);
        r5.largestWidthLimitDp = r6;
        r5 = 1;
        r0 = r42;
        r1 = r47;
        r47 = r0.getInteger(r5, r1);
        r5 = 2;
        r0 = r42;
        r1 = r46;
        r46 = r0.getInteger(r5, r1);
        r5 = 3;
        r0 = r42;
        r1 = r45;
        r45 = r0.getInteger(r5, r1);
        r5 = 5;
        r0 = r42;
        r1 = r48;
        r48 = r0.getInteger(r5, r1);
        r5 = 4;
        r0 = r42;
        r1 = r41;
        r41 = r0.getInteger(r5, r1);
        r5 = 0;
        r0 = r42;
        r13 = r0.getInteger(r5, r13);
        r42.recycle();
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x05d9:
        r5 = "protected-broadcast";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x0629;
    L_0x05e4:
        r5 = com.android.internal.R.styleable.AndroidManifestProtectedBroadcast;
        r0 = r59;
        r1 = r60;
        r42 = r0.obtainAttributes(r1, r5);
        r5 = 0;
        r0 = r42;
        r33 = r0.getNonResourceString(r5);
        r42.recycle();
        if (r33 == 0) goto L_0x0624;
    L_0x05fa:
        r5 = r61 & 1;
        if (r5 == 0) goto L_0x0624;
    L_0x05fe:
        r0 = r57;
        r5 = r0.protectedBroadcasts;
        if (r5 != 0) goto L_0x060d;
    L_0x0604:
        r5 = new java.util.ArrayList;
        r5.<init>();
        r0 = r57;
        r0.protectedBroadcasts = r5;
    L_0x060d:
        r0 = r57;
        r5 = r0.protectedBroadcasts;
        r0 = r33;
        r5 = r5.contains(r0);
        if (r5 != 0) goto L_0x0624;
    L_0x0619:
        r0 = r57;
        r5 = r0.protectedBroadcasts;
        r6 = r33.intern();
        r5.add(r6);
    L_0x0624:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x0629:
        r5 = "instrumentation";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x0646;
    L_0x0634:
        r0 = r56;
        r1 = r57;
        r2 = r59;
        r3 = r60;
        r4 = r62;
        r5 = r0.parseInstrumentation(r1, r2, r3, r4);
        if (r5 != 0) goto L_0x0100;
    L_0x0644:
        r5 = 0;
        return r5;
    L_0x0646:
        r5 = "original-package";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x0697;
    L_0x0651:
        r5 = com.android.internal.R.styleable.AndroidManifestOriginalPackage;
        r0 = r59;
        r1 = r60;
        r42 = r0.obtainAttributes(r1, r5);
        r5 = 0;
        r6 = 0;
        r0 = r42;
        r36 = r0.getNonConfigurationString(r5, r6);
        r0 = r57;
        r5 = r0.packageName;
        r0 = r36;
        r5 = r5.equals(r0);
        if (r5 != 0) goto L_0x068f;
    L_0x066f:
        r0 = r57;
        r5 = r0.mOriginalPackages;
        if (r5 != 0) goto L_0x0686;
    L_0x0675:
        r5 = new java.util.ArrayList;
        r5.<init>();
        r0 = r57;
        r0.mOriginalPackages = r5;
        r0 = r57;
        r5 = r0.packageName;
        r0 = r57;
        r0.mRealPackage = r5;
    L_0x0686:
        r0 = r57;
        r5 = r0.mOriginalPackages;
        r0 = r36;
        r5.add(r0);
    L_0x068f:
        r42.recycle();
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x0697:
        r5 = "adopt-permissions";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x06d6;
    L_0x06a2:
        r5 = com.android.internal.R.styleable.AndroidManifestOriginalPackage;
        r0 = r59;
        r1 = r60;
        r42 = r0.obtainAttributes(r1, r5);
        r5 = 0;
        r6 = 0;
        r0 = r42;
        r33 = r0.getNonConfigurationString(r5, r6);
        r42.recycle();
        if (r33 == 0) goto L_0x06d1;
    L_0x06b9:
        r0 = r57;
        r5 = r0.mAdoptPermissions;
        if (r5 != 0) goto L_0x06c8;
    L_0x06bf:
        r5 = new java.util.ArrayList;
        r5.<init>();
        r0 = r57;
        r0.mAdoptPermissions = r5;
    L_0x06c8:
        r0 = r57;
        r5 = r0.mAdoptPermissions;
        r0 = r33;
        r5.add(r0);
    L_0x06d1:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x06d6:
        r5 = "uses-gl-texture";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x06e6;
    L_0x06e1:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x06e6:
        r5 = "compatible-screens";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x06f6;
    L_0x06f1:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x06f6:
        r5 = "supports-input";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x0706;
    L_0x0701:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x0706:
        r5 = "eat-comment";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x0716;
    L_0x0711:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x0716:
        r5 = "package";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x073e;
    L_0x0721:
        r5 = MULTI_PACKAGE_APK_ENABLED;
        if (r5 != 0) goto L_0x072a;
    L_0x0725:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x072a:
        r5 = r56;
        r6 = r57;
        r7 = r59;
        r8 = r60;
        r9 = r61;
        r10 = r62;
        r5 = r5.parseBaseApkChild(r6, r7, r8, r9, r10);
        if (r5 != 0) goto L_0x0100;
    L_0x073c:
        r5 = 0;
        return r5;
    L_0x073e:
        r5 = "restrict-update";
        r0 = r49;
        r5 = r0.equals(r5);
        if (r5 == 0) goto L_0x07ad;
    L_0x0749:
        r5 = r61 & 64;
        if (r5 == 0) goto L_0x07a8;
    L_0x074d:
        r5 = com.android.internal.R.styleable.AndroidManifestRestrictUpdate;
        r0 = r59;
        r1 = r60;
        r42 = r0.obtainAttributes(r1, r5);
        r5 = 0;
        r6 = 0;
        r0 = r42;
        r20 = r0.getNonConfigurationString(r5, r6);
        r42.recycle();
        r5 = 0;
        r0 = r57;
        r0.restrictUpdateHash = r5;
        if (r20 == 0) goto L_0x07a8;
    L_0x0769:
        r22 = r20.length();
        r5 = r22 / 2;
        r0 = new byte[r5];
        r21 = r0;
        r23 = 0;
    L_0x0775:
        r0 = r23;
        r1 = r22;
        if (r0 >= r1) goto L_0x07a2;
    L_0x077b:
        r5 = r23 / 2;
        r0 = r20;
        r1 = r23;
        r6 = r0.charAt(r1);
        r7 = 16;
        r6 = java.lang.Character.digit(r6, r7);
        r6 = r6 << 4;
        r7 = r23 + 1;
        r0 = r20;
        r7 = r0.charAt(r7);
        r8 = 16;
        r7 = java.lang.Character.digit(r7, r8);
        r6 = r6 + r7;
        r6 = (byte) r6;
        r21[r5] = r6;
        r23 = r23 + 2;
        goto L_0x0775;
    L_0x07a2:
        r0 = r21;
        r1 = r57;
        r1.restrictUpdateHash = r0;
    L_0x07a8:
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x07ad:
        r5 = "PackageParser";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "Unknown element under <manifest>: ";
        r6 = r6.append(r7);
        r7 = r60.getName();
        r6 = r6.append(r7);
        r7 = " at ";
        r6 = r6.append(r7);
        r0 = r56;
        r7 = r0.mArchiveSourcePath;
        r6 = r6.append(r7);
        r7 = " ";
        r6 = r6.append(r7);
        r7 = r60.getPositionDescription();
        r6 = r6.append(r7);
        r6 = r6.toString();
        android.util.Slog.w(r5, r6);
        com.android.internal.util.XmlUtils.skipCurrentTag(r60);
        goto L_0x0100;
    L_0x07ee:
        if (r18 != 0) goto L_0x0806;
    L_0x07f0:
        r0 = r57;
        r5 = r0.instrumentation;
        r5 = r5.size();
        if (r5 != 0) goto L_0x0806;
    L_0x07fa:
        r5 = "<manifest> does not contain an <application> or <instrumentation>";
        r6 = 0;
        r62[r6] = r5;
        r5 = -109; // 0xffffffffffffff93 float:NaN double:NaN;
        r0 = r56;
        r0.mParseError = r5;
    L_0x0806:
        r5 = NEW_PERMISSIONS;
        r11 = r5.length;
        r24 = 0;
        r28 = 0;
    L_0x080d:
        r0 = r28;
        if (r0 >= r11) goto L_0x0821;
    L_0x0811:
        r5 = NEW_PERMISSIONS;
        r35 = r5[r28];
        r0 = r57;
        r5 = r0.applicationInfo;
        r5 = r5.targetSdkVersion;
        r0 = r35;
        r6 = r0.sdkVersion;
        if (r5 < r6) goto L_0x0859;
    L_0x0821:
        if (r24 == 0) goto L_0x082d;
    L_0x0823:
        r5 = "PackageParser";
        r6 = r24.toString();
        android.util.Slog.i(r5, r6);
    L_0x082d:
        r5 = SPLIT_PERMISSIONS;
        r12 = r5.length;
        r29 = 0;
    L_0x0832:
        r0 = r29;
        if (r0 >= r12) goto L_0x08cc;
    L_0x0836:
        r5 = SPLIT_PERMISSIONS;
        r43 = r5[r29];
        r0 = r57;
        r5 = r0.applicationInfo;
        r5 = r5.targetSdkVersion;
        r0 = r43;
        r6 = r0.targetSdk;
        if (r5 >= r6) goto L_0x0856;
    L_0x0846:
        r0 = r57;
        r5 = r0.requestedPermissions;
        r0 = r43;
        r6 = r0.rootPerm;
        r5 = r5.contains(r6);
        r5 = r5 ^ 1;
        if (r5 == 0) goto L_0x08a3;
    L_0x0856:
        r29 = r29 + 1;
        goto L_0x0832;
    L_0x0859:
        r0 = r57;
        r5 = r0.requestedPermissions;
        r0 = r35;
        r6 = r0.name;
        r5 = r5.contains(r6);
        if (r5 != 0) goto L_0x0897;
    L_0x0867:
        if (r24 != 0) goto L_0x089b;
    L_0x0869:
        r24 = new java.lang.StringBuilder;
        r5 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r0 = r24;
        r0.<init>(r5);
        r0 = r57;
        r5 = r0.packageName;
        r0 = r24;
        r0.append(r5);
        r5 = ": compat added ";
        r0 = r24;
        r0.append(r5);
    L_0x0883:
        r0 = r35;
        r5 = r0.name;
        r0 = r24;
        r0.append(r5);
        r0 = r57;
        r5 = r0.requestedPermissions;
        r0 = r35;
        r6 = r0.name;
        r5.add(r6);
    L_0x0897:
        r28 = r28 + 1;
        goto L_0x080d;
    L_0x089b:
        r5 = 32;
        r0 = r24;
        r0.append(r5);
        goto L_0x0883;
    L_0x08a3:
        r25 = 0;
    L_0x08a5:
        r0 = r43;
        r5 = r0.newPerms;
        r5 = r5.length;
        r0 = r25;
        if (r0 >= r5) goto L_0x0856;
    L_0x08ae:
        r0 = r43;
        r5 = r0.newPerms;
        r38 = r5[r25];
        r0 = r57;
        r5 = r0.requestedPermissions;
        r0 = r38;
        r5 = r5.contains(r0);
        if (r5 != 0) goto L_0x08c9;
    L_0x08c0:
        r0 = r57;
        r5 = r0.requestedPermissions;
        r0 = r38;
        r5.add(r0);
    L_0x08c9:
        r25 = r25 + 1;
        goto L_0x08a5;
    L_0x08cc:
        if (r47 < 0) goto L_0x08d9;
    L_0x08ce:
        if (r47 <= 0) goto L_0x08e3;
    L_0x08d0:
        r0 = r57;
        r5 = r0.applicationInfo;
        r5 = r5.targetSdkVersion;
        r6 = 4;
        if (r5 < r6) goto L_0x08e3;
    L_0x08d9:
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = r5.flags;
        r6 = r6 | 512;
        r5.flags = r6;
    L_0x08e3:
        if (r46 == 0) goto L_0x08ef;
    L_0x08e5:
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = r5.flags;
        r6 = r6 | 1024;
        r5.flags = r6;
    L_0x08ef:
        if (r45 < 0) goto L_0x08fc;
    L_0x08f1:
        if (r45 <= 0) goto L_0x0906;
    L_0x08f3:
        r0 = r57;
        r5 = r0.applicationInfo;
        r5 = r5.targetSdkVersion;
        r6 = 4;
        if (r5 < r6) goto L_0x0906;
    L_0x08fc:
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = r5.flags;
        r6 = r6 | 2048;
        r5.flags = r6;
    L_0x0906:
        if (r48 < 0) goto L_0x0914;
    L_0x0908:
        if (r48 <= 0) goto L_0x091f;
    L_0x090a:
        r0 = r57;
        r5 = r0.applicationInfo;
        r5 = r5.targetSdkVersion;
        r6 = 9;
        if (r5 < r6) goto L_0x091f;
    L_0x0914:
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = r5.flags;
        r7 = 524288; // 0x80000 float:7.34684E-40 double:2.590327E-318;
        r6 = r6 | r7;
        r5.flags = r6;
    L_0x091f:
        if (r41 < 0) goto L_0x092c;
    L_0x0921:
        if (r41 <= 0) goto L_0x0936;
    L_0x0923:
        r0 = r57;
        r5 = r0.applicationInfo;
        r5 = r5.targetSdkVersion;
        r6 = 4;
        if (r5 < r6) goto L_0x0936;
    L_0x092c:
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = r5.flags;
        r6 = r6 | 4096;
        r5.flags = r6;
    L_0x0936:
        if (r13 < 0) goto L_0x0943;
    L_0x0938:
        if (r13 <= 0) goto L_0x094d;
    L_0x093a:
        r0 = r57;
        r5 = r0.applicationInfo;
        r5 = r5.targetSdkVersion;
        r6 = 4;
        if (r5 < r6) goto L_0x094d;
    L_0x0943:
        r0 = r57;
        r5 = r0.applicationInfo;
        r6 = r5.flags;
        r6 = r6 | 8192;
        r5.flags = r6;
    L_0x094d:
        r0 = r57;
        r5 = r0.applicationInfo;
        r5 = r5.usesCompatibilityMode();
        if (r5 == 0) goto L_0x095a;
    L_0x0957:
        r56.adjustPackageToBeUnresizeableAndUnpipable(r57);
    L_0x095a:
        return r57;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.content.pm.PackageParser.parseBaseApkCommon(android.content.pm.PackageParser$Package, java.util.Set, android.content.res.Resources, android.content.res.XmlResourceParser, int, java.lang.String[]):android.content.pm.PackageParser$Package");
    }

    private boolean checkOverlayRequiredSystemProperty(String propName, String propValue) {
        boolean z = false;
        if (!TextUtils.isEmpty(propName) && !TextUtils.isEmpty(propValue)) {
            String currValue = SystemProperties.get(propName);
            if (currValue != null) {
                z = currValue.equals(propValue);
            }
            return z;
        } else if (TextUtils.isEmpty(propName) && (TextUtils.isEmpty(propValue) ^ 1) == 0) {
            return true;
        } else {
            Slog.w(TAG, "Disabling overlay - incomplete property :'" + propName + "=" + propValue + "' - require both requiredSystemPropertyName" + " AND requiredSystemPropertyValue to be specified.");
            return false;
        }
    }

    private void adjustPackageToBeUnresizeableAndUnpipable(Package pkg) {
        for (Activity a : pkg.activities) {
            a.info.resizeMode = 0;
            ActivityInfo activityInfo = a.info;
            activityInfo.flags &= -4194305;
        }
    }

    public static int computeTargetSdkVersion(int targetVers, String targetCode, int platformSdkVersion, String[] platformSdkCodenames, String[] outError) {
        if (targetCode == null) {
            return targetVers;
        }
        if (ArrayUtils.contains(platformSdkCodenames, targetCode)) {
            return 10000;
        }
        if (platformSdkCodenames.length > 0) {
            outError[0] = "Requires development platform " + targetCode + " (current platform is any of " + Arrays.toString(platformSdkCodenames) + ")";
        } else {
            outError[0] = "Requires development platform " + targetCode + " but this is a release platform.";
        }
        return -1;
    }

    public static int computeMinSdkVersion(int minVers, String minCode, int platformSdkVersion, String[] platformSdkCodenames, String[] outError) {
        if (minCode == null) {
            if (minVers <= platformSdkVersion) {
                return minVers;
            }
            outError[0] = "Requires newer sdk version #" + minVers + " (current version is #" + platformSdkVersion + ")";
            return -1;
        } else if (ArrayUtils.contains(platformSdkCodenames, minCode)) {
            return 10000;
        } else {
            if (platformSdkCodenames.length > 0) {
                outError[0] = "Requires development platform " + minCode + " (current platform is any of " + Arrays.toString(platformSdkCodenames) + ")";
            } else {
                outError[0] = "Requires development platform " + minCode + " but this is a release platform.";
            }
            return -1;
        }
    }

    private FeatureInfo parseUsesFeature(Resources res, AttributeSet attrs) {
        FeatureInfo fi = new FeatureInfo();
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestUsesFeature);
        fi.name = sa.getNonResourceString(0);
        fi.version = sa.getInt(3, 0);
        if (fi.name == null) {
            fi.reqGlEsVersion = sa.getInt(1, 0);
        }
        if (sa.getBoolean(2, true)) {
            fi.flags |= 1;
        }
        sa.recycle();
        return fi;
    }

    private boolean parseUsesStaticLibrary(Package pkg, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesStaticLibrary);
        String lname = sa.getNonResourceString(0);
        int version = sa.getInt(1, -1);
        String certSha256Digest = sa.getNonResourceString(2);
        sa.recycle();
        if (lname == null || version < 0 || certSha256Digest == null) {
            outError[0] = "Bad uses-static-library declaration name: " + lname + " version: " + version + " certDigest" + certSha256Digest;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            XmlUtils.skipCurrentTag(parser);
            return false;
        } else if (pkg.usesStaticLibraries == null || !pkg.usesStaticLibraries.contains(lname)) {
            lname = lname.intern();
            certSha256Digest = certSha256Digest.replace(":", "").toLowerCase();
            String[] additionalCertSha256Digests = EmptyArray.STRING;
            if (pkg.applicationInfo.targetSdkVersion > 26) {
                additionalCertSha256Digests = parseAdditionalCertificates(res, parser, outError);
                if (additionalCertSha256Digests == null) {
                    return false;
                }
            }
            XmlUtils.skipCurrentTag(parser);
            String[] certSha256Digests = new String[(additionalCertSha256Digests.length + 1)];
            certSha256Digests[0] = certSha256Digest;
            System.arraycopy(additionalCertSha256Digests, 0, certSha256Digests, 1, additionalCertSha256Digests.length);
            pkg.usesStaticLibraries = ArrayUtils.add(pkg.usesStaticLibraries, lname);
            pkg.usesStaticLibrariesVersions = ArrayUtils.appendInt(pkg.usesStaticLibrariesVersions, version, true);
            pkg.usesStaticLibrariesCertDigests = (String[][]) ArrayUtils.appendElement(String[].class, pkg.usesStaticLibrariesCertDigests, certSha256Digests, true);
            return true;
        } else {
            outError[0] = "Depending on multiple versions of static library " + lname;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            XmlUtils.skipCurrentTag(parser);
            return false;
        }
    }

    private String[] parseAdditionalCertificates(Resources resources, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        String[] certSha256Digests = EmptyArray.STRING;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return certSha256Digests;
            }
            if (!(type == 3 || type == 4)) {
                if (parser.getName().equals("additional-certificate")) {
                    TypedArray sa = resources.obtainAttributes(parser, R.styleable.AndroidManifestAdditionalCertificate);
                    String certSha256Digest = sa.getNonResourceString(0);
                    sa.recycle();
                    if (TextUtils.isEmpty(certSha256Digest)) {
                        outError[0] = "Bad additional-certificate declaration with empty certDigest:" + certSha256Digest;
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        XmlUtils.skipCurrentTag(parser);
                        sa.recycle();
                        return null;
                    }
                    certSha256Digests = (String[]) ArrayUtils.appendElement(String.class, certSha256Digests, certSha256Digest.replace(":", "").toLowerCase());
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        return certSha256Digests;
    }

    private boolean parseUsesPermission(Package pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesPermission);
        String name = sa.getNonResourceString(0);
        int maxSdkVersion = 0;
        TypedValue val = sa.peekValue(1);
        if (val != null && val.type >= 16 && val.type <= 31) {
            maxSdkVersion = val.data;
        }
        String requiredFeature = sa.getNonConfigurationString(2, 0);
        String requiredNotfeature = sa.getNonConfigurationString(3, 0);
        sa.recycle();
        XmlUtils.skipCurrentTag(parser);
        if (name == null) {
            return true;
        }
        if (maxSdkVersion != 0 && maxSdkVersion < VERSION.RESOURCES_SDK_INT) {
            return true;
        }
        if (requiredFeature != null && this.mCallback != null && (this.mCallback.hasFeature(requiredFeature) ^ 1) != 0) {
            return true;
        }
        if ((requiredNotfeature == null || this.mCallback == null || !this.mCallback.hasFeature(requiredNotfeature)) && pkg.requestedPermissions.indexOf(name) == -1) {
            pkg.requestedPermissions.add(name.intern());
        }
        return true;
    }

    private static String buildClassName(String pkg, CharSequence clsSeq, String[] outError) {
        if (clsSeq == null || clsSeq.length() <= 0) {
            outError[0] = "Empty class name in package " + pkg;
            return null;
        }
        String cls = clsSeq.toString();
        if (cls.charAt(0) == '.') {
            return pkg + cls;
        }
        if (cls.indexOf(46) >= 0) {
            return cls;
        }
        StringBuilder b = new StringBuilder(pkg);
        b.append('.');
        b.append(cls);
        return b.toString();
    }

    private static String buildCompoundName(String pkg, CharSequence procSeq, String type, String[] outError) {
        String proc = procSeq.toString();
        char c = proc.charAt(0);
        String nameError;
        if (pkg == null || c != ':') {
            nameError = validateName(proc, true, false);
            if (nameError == null || (StorageManager.UUID_SYSTEM.equals(proc) ^ 1) == 0) {
                return proc;
            }
            outError[0] = "Invalid " + type + " name " + proc + " in package " + pkg + ": " + nameError;
            return null;
        } else if (proc.length() < 2) {
            outError[0] = "Bad " + type + " name " + proc + " in package " + pkg + ": must be at least two characters";
            return null;
        } else {
            nameError = validateName(proc.substring(1), false, false);
            if (nameError == null) {
                return pkg + proc;
            }
            outError[0] = "Invalid " + type + " name " + proc + " in package " + pkg + ": " + nameError;
            return null;
        }
    }

    private static String buildProcessName(String pkg, String defProc, CharSequence procSeq, int flags, String[] separateProcesses, String[] outError) {
        if ((flags & 8) == 0 || (StorageManager.UUID_SYSTEM.equals(procSeq) ^ 1) == 0) {
            if (separateProcesses != null) {
                for (int i = separateProcesses.length - 1; i >= 0; i--) {
                    String sp = separateProcesses[i];
                    if (sp.equals(pkg) || sp.equals(defProc) || sp.equals(procSeq)) {
                        return pkg;
                    }
                }
            }
            if (procSeq == null || procSeq.length() <= 0) {
                return defProc;
            }
            return TextUtils.safeIntern(buildCompoundName(pkg, procSeq, "process", outError));
        }
        if (defProc == null) {
            defProc = pkg;
        }
        return defProc;
    }

    private static String buildTaskAffinityName(String pkg, String defProc, CharSequence procSeq, String[] outError) {
        if (procSeq == null) {
            return defProc;
        }
        if (procSeq.length() <= 0) {
            return null;
        }
        return buildCompoundName(pkg, procSeq, "taskAffinity", outError);
    }

    private boolean parseKeySets(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int currentKeySetDepth = -1;
        String currentKeySet = null;
        ArrayMap<String, PublicKey> publicKeys = new ArrayMap();
        ArraySet<String> upgradeKeySets = new ArraySet();
        ArrayMap<String, ArraySet<String>> definedKeySets = new ArrayMap();
        ArraySet<String> improperKeySets = new ArraySet();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
            } else if (type != 3) {
                String tagName = parser.getName();
                TypedArray sa;
                if (tagName.equals("key-set")) {
                    if (currentKeySet != null) {
                        outError[0] = "Improperly nested 'key-set' tag at " + parser.getPositionDescription();
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    sa = res.obtainAttributes(parser, R.styleable.AndroidManifestKeySet);
                    String keysetName = sa.getNonResourceString(0);
                    definedKeySets.put(keysetName, new ArraySet());
                    currentKeySet = keysetName;
                    currentKeySetDepth = parser.getDepth();
                    sa.recycle();
                } else if (tagName.equals("public-key")) {
                    if (currentKeySet == null) {
                        outError[0] = "Improperly nested 'key-set' tag at " + parser.getPositionDescription();
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    sa = res.obtainAttributes(parser, R.styleable.AndroidManifestPublicKey);
                    String publicKeyName = sa.getNonResourceString(0);
                    String encodedKey = sa.getNonResourceString(1);
                    if (encodedKey == null && publicKeys.get(publicKeyName) == null) {
                        outError[0] = "'public-key' " + publicKeyName + " must define a public-key value" + " on first use at " + parser.getPositionDescription();
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        sa.recycle();
                        return false;
                    }
                    if (encodedKey != null) {
                        PublicKey currentKey = parsePublicKey(encodedKey);
                        if (currentKey == null) {
                            Slog.w(TAG, "No recognized valid key in 'public-key' tag at " + parser.getPositionDescription() + " key-set " + currentKeySet + " will not be added to the package's defined key-sets.");
                            sa.recycle();
                            improperKeySets.add(currentKeySet);
                            XmlUtils.skipCurrentTag(parser);
                        } else if (publicKeys.get(publicKeyName) == null || ((PublicKey) publicKeys.get(publicKeyName)).equals(currentKey)) {
                            publicKeys.put(publicKeyName, currentKey);
                        } else {
                            outError[0] = "Value of 'public-key' " + publicKeyName + " conflicts with previously defined value at " + parser.getPositionDescription();
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            sa.recycle();
                            return false;
                        }
                    }
                    ((ArraySet) definedKeySets.get(currentKeySet)).add(publicKeyName);
                    sa.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("upgrade-key-set")) {
                    sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUpgradeKeySet);
                    upgradeKeySets.add(sa.getNonResourceString(0));
                    sa.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <key-sets>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            } else if (parser.getDepth() == currentKeySetDepth) {
                currentKeySet = null;
                currentKeySetDepth = -1;
            }
        }
        if (publicKeys.keySet().removeAll(definedKeySets.keySet())) {
            outError[0] = "Package" + owner.packageName + " AndroidManifext.xml " + "'key-set' and 'public-key' names must be distinct.";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        owner.mKeySetMapping = new ArrayMap();
        for (Entry<String, ArraySet<String>> e : definedKeySets.entrySet()) {
            String keySetName = (String) e.getKey();
            if (((ArraySet) e.getValue()).size() == 0) {
                Slog.w(TAG, "Package" + owner.packageName + " AndroidManifext.xml " + "'key-set' " + keySetName + " has no valid associated 'public-key'." + " Not including in package's defined key-sets.");
            } else if (improperKeySets.contains(keySetName)) {
                Slog.w(TAG, "Package" + owner.packageName + " AndroidManifext.xml " + "'key-set' " + keySetName + " contained improper 'public-key'" + " tags. Not including in package's defined key-sets.");
            } else {
                owner.mKeySetMapping.put(keySetName, new ArraySet());
                for (String s : (ArraySet) e.getValue()) {
                    ((ArraySet) owner.mKeySetMapping.get(keySetName)).add((PublicKey) publicKeys.get(s));
                }
            }
        }
        if (owner.mKeySetMapping.keySet().containsAll(upgradeKeySets)) {
            owner.mUpgradeKeySets = upgradeKeySets;
            return true;
        }
        outError[0] = "Package" + owner.packageName + " AndroidManifext.xml " + "does not define all 'upgrade-key-set's .";
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private boolean parsePermissionGroup(Package owner, int flags, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        Component perm = new PermissionGroup(owner);
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestPermissionGroup);
        if (parsePackageItemInfo(owner, perm.info, outError, "<permission-group>", sa, true, 2, 0, 1, 8, 5, 7)) {
            perm.info.descriptionRes = sa.getResourceId(4, 0);
            perm.info.flags = sa.getInt(6, 0);
            perm.info.priority = sa.getInt(3, 0);
            if (perm.info.priority > 0 && (flags & 1) == 0) {
                perm.info.priority = 0;
            }
            sa.recycle();
            if (parseAllMetaData(res, parser, "<permission-group>", perm, outError)) {
                owner.permissionGroups.add(perm);
                return true;
            }
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        sa.recycle();
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private boolean parsePermission(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestPermission);
        Component perm = new Permission(owner);
        if (parsePackageItemInfo(owner, perm.info, outError, "<permission>", sa, true, 2, 0, 1, 9, 6, 8)) {
            perm.info.group = sa.getNonResourceString(4);
            if (perm.info.group != null) {
                perm.info.group = perm.info.group.intern();
            }
            perm.info.descriptionRes = sa.getResourceId(5, 0);
            perm.info.protectionLevel = sa.getInt(3, 0);
            perm.info.flags = sa.getInt(7, 0);
            sa.recycle();
            if (perm.info.protectionLevel == -1) {
                outError[0] = "<permission> does not specify protectionLevel";
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
            perm.info.protectionLevel = PermissionInfo.fixProtectionLevel(perm.info.protectionLevel);
            if ((perm.info.protectionLevel & PermissionInfo.PROTECTION_MASK_FLAGS) == 0 || (perm.info.protectionLevel & 4096) != 0 || (perm.info.protectionLevel & 8192) != 0 || (perm.info.protectionLevel & 15) == 2) {
                if (parseAllMetaData(res, parser, "<permission>", perm, outError)) {
                    owner.permissions.add(perm);
                    return true;
                }
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
            outError[0] = "<permission>  protectionLevel specifies a non-instnat flag but is not based on signature type";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        sa.recycle();
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private boolean parsePermissionTree(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        Component perm = new Permission(owner);
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestPermissionTree);
        if (parsePackageItemInfo(owner, perm.info, outError, "<permission-tree>", sa, true, 2, 0, 1, 5, 3, 4)) {
            sa.recycle();
            int index = perm.info.name.indexOf(46);
            if (index > 0) {
                index = perm.info.name.indexOf(46, index + 1);
            }
            if (index < 0) {
                outError[0] = "<permission-tree> name has less than three segments: " + perm.info.name;
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
            perm.info.descriptionRes = 0;
            perm.info.protectionLevel = 0;
            perm.tree = true;
            if (parseAllMetaData(res, parser, "<permission-tree>", perm, outError)) {
                owner.permissions.add(perm);
                return true;
            }
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        sa.recycle();
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private Instrumentation parseInstrumentation(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestInstrumentation);
        if (this.mParseInstrumentationArgs == null) {
            this.mParseInstrumentationArgs = new ParsePackageItemArgs(owner, outError, 2, 0, 1, 8, 6, 7);
            this.mParseInstrumentationArgs.tag = "<instrumentation>";
        }
        this.mParseInstrumentationArgs.sa = sa;
        Instrumentation a = new Instrumentation(this.mParseInstrumentationArgs, new InstrumentationInfo());
        if (outError[0] != null) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        String str = sa.getNonResourceString(3);
        a.info.targetPackage = str != null ? str.intern() : null;
        str = sa.getNonResourceString(9);
        a.info.targetProcesses = str != null ? str.intern() : null;
        a.info.handleProfiling = sa.getBoolean(4, false);
        a.info.functionalTest = sa.getBoolean(5, false);
        sa.recycle();
        if (a.info.targetPackage == null) {
            outError[0] = "<instrumentation> does not specify targetPackage";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        if (parseAllMetaData(res, parser, "<instrumentation>", a, outError)) {
            owner.instrumentation.add(a);
            return a;
        }
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean parseBaseApplication(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        ApplicationInfo ai = owner.applicationInfo;
        String pkgName = owner.applicationInfo.packageName;
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestApplication);
        if (parsePackageItemInfo(owner, ai, outError, "<application>", sa, false, 3, 1, 2, 42, 22, 30)) {
            if (ai.name != null) {
                ai.className = ai.name;
            }
            String manageSpaceActivity = sa.getNonConfigurationString(4, 1024);
            if (manageSpaceActivity != null) {
                ai.manageSpaceActivityName = buildClassName(pkgName, manageSpaceActivity, outError);
            }
            if (sa.getBoolean(17, true)) {
                ai.flags |= 32768;
                String backupAgent = sa.getNonConfigurationString(16, 1024);
                if (backupAgent != null) {
                    ai.backupAgentName = buildClassName(pkgName, backupAgent, outError);
                    if (sa.getBoolean(18, true)) {
                        ai.flags |= 65536;
                    }
                    if (sa.getBoolean(21, false)) {
                        ai.flags |= 131072;
                    }
                    if (sa.getBoolean(32, false)) {
                        ai.flags |= 67108864;
                    }
                    if (sa.getBoolean(40, false)) {
                        ai.privateFlags |= 8192;
                    }
                }
                TypedValue v = sa.peekValue(35);
                if (v != null) {
                    int i = v.resourceId;
                    ai.fullBackupContent = i;
                    if (i == 0) {
                        ai.fullBackupContent = v.data == 0 ? -1 : 0;
                    }
                }
            }
            ai.theme = sa.getResourceId(0, 0);
            ai.descriptionRes = sa.getResourceId(13, 0);
            if ((flags & 1) != 0 && sa.getBoolean(8, false)) {
                String requiredFeature = sa.getNonResourceString(45);
                if (requiredFeature == null || this.mCallback.hasFeature(requiredFeature)) {
                    ai.flags |= 8;
                }
            }
            if (sa.getBoolean(27, false)) {
                owner.mRequiredForAllUsers = true;
            }
            String restrictedAccountType = sa.getString(28);
            if (restrictedAccountType != null && restrictedAccountType.length() > 0) {
                owner.mRestrictedAccountType = restrictedAccountType;
            }
            String requiredAccountType = sa.getString(29);
            if (requiredAccountType != null && requiredAccountType.length() > 0) {
                owner.mRequiredAccountType = requiredAccountType;
            }
            if (sa.getBoolean(10, false)) {
                ai.flags |= 2;
            }
            if (sa.getBoolean(20, false)) {
                ai.flags |= 16384;
            }
            owner.baseHardwareAccelerated = sa.getBoolean(23, owner.applicationInfo.targetSdkVersion >= 14);
            if (owner.baseHardwareAccelerated) {
                ai.flags |= 536870912;
            }
            if (sa.getBoolean(7, true)) {
                ai.flags |= 4;
            }
            if (sa.getBoolean(14, false)) {
                ai.flags |= 32;
            }
            if (sa.getBoolean(5, true)) {
                ai.flags |= 64;
            }
            if (owner.parentPackage == null && sa.getBoolean(15, false)) {
                ai.flags |= 256;
            }
            if (sa.getBoolean(24, false)) {
                ai.flags |= 1048576;
            }
            if (sa.getBoolean(36, true)) {
                ai.flags |= 134217728;
            }
            if (sa.getBoolean(26, false)) {
                ai.flags |= 4194304;
            }
            if (sa.getBoolean(33, false)) {
                ai.flags |= Integer.MIN_VALUE;
            }
            if (sa.getBoolean(34, true)) {
                ai.flags |= 268435456;
            }
            if (sa.getBoolean(38, false)) {
                ai.privateFlags |= 32;
            }
            if (sa.getBoolean(39, false)) {
                ai.privateFlags |= 64;
            }
            if (sa.hasValueOrEmpty(37)) {
                if (sa.getBoolean(37, true)) {
                    ai.privateFlags |= 1024;
                } else {
                    ai.privateFlags |= 2048;
                }
            } else if (owner.applicationInfo.targetSdkVersion >= 24) {
                ai.privateFlags |= 4096;
            }
            ai.maxAspectRatio = sa.getFloat(44, 0.0f);
            ai.networkSecurityConfigRes = sa.getResourceId(41, 0);
            ai.category = sa.getInt(43, -1);
            String str = sa.getNonConfigurationString(6, 0);
            String intern = (str == null || str.length() <= 0) ? null : str.intern();
            ai.permission = intern;
            if (owner.applicationInfo.targetSdkVersion >= 8) {
                str = sa.getNonConfigurationString(12, 1024);
            } else {
                str = sa.getNonResourceString(12);
            }
            ai.taskAffinity = buildTaskAffinityName(ai.packageName, ai.packageName, str, outError);
            if (outError[0] == null) {
                CharSequence pname;
                if (owner.applicationInfo.targetSdkVersion >= 8) {
                    pname = sa.getNonConfigurationString(11, 1024);
                } else {
                    pname = sa.getNonResourceString(11);
                }
                ai.processName = buildProcessName(ai.packageName, null, pname, flags, this.mSeparateProcesses, outError);
                ai.enabled = sa.getBoolean(9, true);
                if (sa.getBoolean(31, false)) {
                    ai.flags |= 33554432;
                }
            }
            ai.uiOptions = sa.getInt(25, 0);
            ai.classLoaderName = sa.getString(46);
            if (!(ai.classLoaderName == null || (ClassLoaderFactory.isValidClassLoaderName(ai.classLoaderName) ^ 1) == 0)) {
                outError[0] = "Invalid class loader name: " + ai.classLoaderName;
            }
            sa.recycle();
            if (outError[0] != null) {
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
            int innerDepth = parser.getDepth();
            CachedComponentArgs cachedComponentArgs = new CachedComponentArgs();
            while (true) {
                int type = parser.next();
                if (type == 1 || (type == 3 && parser.getDepth() <= innerDepth)) {
                    setMaxAspectRatio(owner);
                    PackageBackwardCompatibility.modifySharedLibraries(owner);
                } else if (!(type == 3 || type == 4)) {
                    String tagName = parser.getName();
                    Activity a;
                    if (tagName.equals(Context.ACTIVITY_SERVICE)) {
                        a = parseActivity(owner, res, parser, flags, outError, cachedComponentArgs, false, owner.baseHardwareAccelerated);
                        if (a == null) {
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return false;
                        }
                        owner.activities.add(a);
                    } else {
                        if (tagName.equals("receiver")) {
                            a = parseActivity(owner, res, parser, flags, outError, cachedComponentArgs, true, false);
                            if (a == null) {
                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                return false;
                            }
                            owner.receivers.add(a);
                        } else {
                            if (tagName.equals(Notification.CATEGORY_SERVICE)) {
                                Service s = parseService(owner, res, parser, flags, outError, cachedComponentArgs);
                                if (s == null) {
                                    this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                    return false;
                                }
                                owner.services.add(s);
                            } else {
                                if (tagName.equals("provider")) {
                                    Provider p = parseProvider(owner, res, parser, flags, outError, cachedComponentArgs);
                                    if (p == null) {
                                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                        return false;
                                    }
                                    owner.providers.add(p);
                                } else {
                                    if (tagName.equals("activity-alias")) {
                                        a = parseActivityAlias(owner, res, parser, flags, outError, cachedComponentArgs);
                                        if (a == null) {
                                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                            return false;
                                        }
                                        owner.activities.add(a);
                                    } else if (parser.getName().equals("meta-data")) {
                                        Bundle parseMetaData = parseMetaData(res, parser, owner.mAppMetaData, outError);
                                        owner.mAppMetaData = parseMetaData;
                                        if (parseMetaData == null) {
                                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                            return false;
                                        }
                                    } else {
                                        String lname;
                                        if (tagName.equals("static-library")) {
                                            sa = res.obtainAttributes(parser, R.styleable.AndroidManifestStaticLibrary);
                                            lname = sa.getNonResourceString(0);
                                            int version = sa.getInt(1, -1);
                                            sa.recycle();
                                            if (lname == null || version < 0) {
                                                outError[0] = "Bad static-library declaration name: " + lname + " version: " + version;
                                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                                XmlUtils.skipCurrentTag(parser);
                                            } else if (owner.mSharedUserId != null) {
                                                outError[0] = "sharedUserId not allowed in static shared library";
                                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID;
                                                XmlUtils.skipCurrentTag(parser);
                                                return false;
                                            } else if (owner.staticSharedLibName != null) {
                                                outError[0] = "Multiple static-shared libs for package " + pkgName;
                                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                                XmlUtils.skipCurrentTag(parser);
                                                return false;
                                            } else {
                                                owner.staticSharedLibName = lname.intern();
                                                owner.staticSharedLibVersion = version;
                                                ai.privateFlags |= 16384;
                                                XmlUtils.skipCurrentTag(parser);
                                            }
                                        } else {
                                            if (tagName.equals("library")) {
                                                sa = res.obtainAttributes(parser, R.styleable.AndroidManifestLibrary);
                                                lname = sa.getNonResourceString(0);
                                                sa.recycle();
                                                if (lname != null) {
                                                    lname = lname.intern();
                                                    if (!ArrayUtils.contains(owner.libraryNames, lname)) {
                                                        owner.libraryNames = ArrayUtils.add(owner.libraryNames, lname);
                                                    }
                                                }
                                                XmlUtils.skipCurrentTag(parser);
                                            } else {
                                                if (!tagName.equals("uses-static-library")) {
                                                    if (tagName.equals("uses-library")) {
                                                        sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesLibrary);
                                                        lname = sa.getNonResourceString(0);
                                                        boolean req = sa.getBoolean(1, true);
                                                        sa.recycle();
                                                        if (lname != null) {
                                                            lname = lname.intern();
                                                            if (req) {
                                                                owner.usesLibraries = ArrayUtils.add(owner.usesLibraries, lname);
                                                            } else {
                                                                owner.usesOptionalLibraries = ArrayUtils.add(owner.usesOptionalLibraries, lname);
                                                            }
                                                        }
                                                        XmlUtils.skipCurrentTag(parser);
                                                    } else {
                                                        if (tagName.equals("uses-package")) {
                                                            XmlUtils.skipCurrentTag(parser);
                                                        } else {
                                                            Slog.w(TAG, "Unknown element under <application>: " + tagName + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                                                            XmlUtils.skipCurrentTag(parser);
                                                        }
                                                    }
                                                } else if (!parseUsesStaticLibrary(owner, res, parser, outError)) {
                                                    return false;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            setMaxAspectRatio(owner);
            PackageBackwardCompatibility.modifySharedLibraries(owner);
            ApplicationInfo applicationInfo;
            if (hasDomainURLs(owner)) {
                applicationInfo = owner.applicationInfo;
                applicationInfo.privateFlags |= 16;
            } else {
                applicationInfo = owner.applicationInfo;
                applicationInfo.privateFlags &= -17;
            }
            return true;
        }
        sa.recycle();
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private static boolean hasDomainURLs(Package pkg) {
        if (pkg == null || pkg.activities == null) {
            return false;
        }
        ArrayList<Activity> activities = pkg.activities;
        int countActivities = activities.size();
        for (int n = 0; n < countActivities; n++) {
            ArrayList<ActivityIntentInfo> filters = ((Activity) activities.get(n)).intents;
            if (filters != null) {
                int countFilters = filters.size();
                for (int m = 0; m < countFilters; m++) {
                    ActivityIntentInfo aii = (ActivityIntentInfo) filters.get(m);
                    if (aii.hasAction("android.intent.action.VIEW") && aii.hasAction("android.intent.action.VIEW") && (aii.hasDataScheme(IntentFilter.SCHEME_HTTP) || aii.hasDataScheme(IntentFilter.SCHEME_HTTPS))) {
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    private boolean parseSplitApplication(Package owner, Resources res, XmlResourceParser parser, int flags, int splitIndex, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestApplication);
        if (sa.getBoolean(7, true)) {
            int[] iArr = owner.splitFlags;
            iArr[splitIndex] = iArr[splitIndex] | 4;
        }
        String classLoaderName = sa.getString(46);
        if (classLoaderName == null || ClassLoaderFactory.isValidClassLoaderName(classLoaderName)) {
            owner.applicationInfo.splitClassLoaderNames[splitIndex] = classLoaderName;
            int innerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type != 1 && (type != 3 || parser.getDepth() > innerDepth)) {
                    if (!(type == 3 || type == 4)) {
                        ComponentInfo parsedComponent = null;
                        CachedComponentArgs cachedArgs = new CachedComponentArgs();
                        String tagName = parser.getName();
                        Activity a;
                        if (tagName.equals(Context.ACTIVITY_SERVICE)) {
                            a = parseActivity(owner, res, parser, flags, outError, cachedArgs, false, owner.baseHardwareAccelerated);
                            if (a == null) {
                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                return false;
                            }
                            owner.activities.add(a);
                            parsedComponent = a.info;
                        } else {
                            if (tagName.equals("receiver")) {
                                a = parseActivity(owner, res, parser, flags, outError, cachedArgs, true, false);
                                if (a == null) {
                                    this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                    return false;
                                }
                                owner.receivers.add(a);
                                parsedComponent = a.info;
                            } else {
                                if (tagName.equals(Notification.CATEGORY_SERVICE)) {
                                    Service s = parseService(owner, res, parser, flags, outError, cachedArgs);
                                    if (s == null) {
                                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                        return false;
                                    }
                                    owner.services.add(s);
                                    parsedComponent = s.info;
                                } else {
                                    if (tagName.equals("provider")) {
                                        Provider p = parseProvider(owner, res, parser, flags, outError, cachedArgs);
                                        if (p == null) {
                                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                            return false;
                                        }
                                        owner.providers.add(p);
                                        parsedComponent = p.info;
                                    } else {
                                        if (tagName.equals("activity-alias")) {
                                            a = parseActivityAlias(owner, res, parser, flags, outError, cachedArgs);
                                            if (a == null) {
                                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                                return false;
                                            }
                                            owner.activities.add(a);
                                            parsedComponent = a.info;
                                        } else if (parser.getName().equals("meta-data")) {
                                            Bundle parseMetaData = parseMetaData(res, parser, owner.mAppMetaData, outError);
                                            owner.mAppMetaData = parseMetaData;
                                            if (parseMetaData == null) {
                                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                                return false;
                                            }
                                        } else {
                                            if (!tagName.equals("uses-static-library")) {
                                                if (tagName.equals("uses-library")) {
                                                    sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesLibrary);
                                                    String lname = sa.getNonResourceString(0);
                                                    boolean req = sa.getBoolean(1, true);
                                                    sa.recycle();
                                                    if (lname != null) {
                                                        lname = lname.intern();
                                                        if (req) {
                                                            owner.usesLibraries = ArrayUtils.add(owner.usesLibraries, lname);
                                                            owner.usesOptionalLibraries = ArrayUtils.remove(owner.usesOptionalLibraries, lname);
                                                        } else if (!ArrayUtils.contains(owner.usesLibraries, lname)) {
                                                            owner.usesOptionalLibraries = ArrayUtils.add(owner.usesOptionalLibraries, lname);
                                                        }
                                                    }
                                                    XmlUtils.skipCurrentTag(parser);
                                                } else {
                                                    if (tagName.equals("uses-package")) {
                                                        XmlUtils.skipCurrentTag(parser);
                                                    } else {
                                                        Slog.w(TAG, "Unknown element under <application>: " + tagName + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                                                        XmlUtils.skipCurrentTag(parser);
                                                    }
                                                }
                                            } else if (!parseUsesStaticLibrary(owner, res, parser, outError)) {
                                                return false;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (parsedComponent != null && parsedComponent.splitName == null) {
                            parsedComponent.splitName = owner.splitNames[splitIndex];
                        }
                    }
                }
            }
            return true;
        }
        outError[0] = "Invalid class loader name: " + classLoaderName;
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private static boolean parsePackageItemInfo(Package owner, PackageItemInfo outInfo, String[] outError, String tag, TypedArray sa, boolean nameRequired, int nameRes, int labelRes, int iconRes, int roundIconRes, int logoRes, int bannerRes) {
        if (sa == null) {
            outError[0] = tag + " does not contain any attributes";
            return false;
        }
        String name = sa.getNonConfigurationString(nameRes, 0);
        if (name != null) {
            outInfo.name = buildClassName(owner.applicationInfo.packageName, name, outError);
            if (outInfo.name == null) {
                return false;
            }
        } else if (nameRequired) {
            outError[0] = tag + " does not specify android:name";
            return false;
        }
        int roundIconVal = Resources.getSystem().getBoolean(17957052) ? sa.getResourceId(roundIconRes, 0) : 0;
        if (roundIconVal != 0) {
            outInfo.icon = roundIconVal;
            outInfo.nonLocalizedLabel = null;
        } else {
            int iconVal = sa.getResourceId(iconRes, 0);
            if (iconVal != 0) {
                outInfo.icon = iconVal;
                outInfo.nonLocalizedLabel = null;
            }
        }
        int logoVal = sa.getResourceId(logoRes, 0);
        if (logoVal != 0) {
            outInfo.logo = logoVal;
        }
        int bannerVal = sa.getResourceId(bannerRes, 0);
        if (bannerVal != 0) {
            outInfo.banner = bannerVal;
        }
        TypedValue v = sa.peekValue(labelRes);
        if (v != null) {
            int i = v.resourceId;
            outInfo.labelRes = i;
            if (i == 0) {
                outInfo.nonLocalizedLabel = v.coerceToString();
            }
        }
        outInfo.packageName = owner.packageName;
        return true;
    }

    private Activity parseActivity(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError, CachedComponentArgs cachedArgs, boolean receiver, boolean hardwareAccelerated) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestActivity);
        if (cachedArgs.mActivityArgs == null) {
            cachedArgs.mActivityArgs = new ParseComponentArgs(owner, outError, 3, 1, 2, 44, 23, 30, this.mSeparateProcesses, 7, 17, 5);
        }
        cachedArgs.mActivityArgs.tag = receiver ? "<receiver>" : "<activity>";
        cachedArgs.mActivityArgs.sa = sa;
        cachedArgs.mActivityArgs.flags = flags;
        Activity activity = new Activity(cachedArgs.mActivityArgs, new ActivityInfo());
        if (outError[0] != null) {
            sa.recycle();
            return null;
        }
        ActivityInfo activityInfo;
        boolean setExported = sa.hasValue(6);
        if (setExported) {
            activity.info.exported = sa.getBoolean(6, false);
        }
        activity.info.theme = sa.getResourceId(0, 0);
        activity.info.uiOptions = sa.getInt(26, activity.info.applicationInfo.uiOptions);
        String parentName = sa.getNonConfigurationString(27, 1024);
        if (parentName != null) {
            String parentClassName = buildClassName(activity.info.packageName, parentName, outError);
            if (outError[0] == null) {
                activity.info.parentActivityName = parentClassName;
            } else {
                Log.e(TAG, "Activity " + activity.info.name + " specified invalid parentActivityName " + parentName);
                outError[0] = null;
            }
        }
        String str = sa.getNonConfigurationString(4, 0);
        if (str == null) {
            activity.info.permission = owner.applicationInfo.permission;
        } else {
            activity.info.permission = str.length() > 0 ? str.toString().intern() : null;
        }
        activity.info.taskAffinity = buildTaskAffinityName(owner.applicationInfo.packageName, owner.applicationInfo.taskAffinity, sa.getNonConfigurationString(8, 1024), outError);
        activity.info.splitName = sa.getNonConfigurationString(48, 0);
        activity.info.flags = 0;
        if (sa.getBoolean(9, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 1;
        }
        if (sa.getBoolean(10, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 2;
        }
        if (sa.getBoolean(11, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 4;
        }
        if (sa.getBoolean(21, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 128;
        }
        if (sa.getBoolean(18, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 8;
        }
        if (sa.getBoolean(12, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 16;
        }
        if (sa.getBoolean(13, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 32;
        }
        if (sa.getBoolean(19, (owner.applicationInfo.flags & 32) != 0)) {
            activityInfo = activity.info;
            activityInfo.flags |= 64;
        }
        if (sa.getBoolean(22, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 256;
        }
        if (sa.getBoolean(29, false) || sa.getBoolean(39, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 1024;
        }
        if (sa.getBoolean(24, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 2048;
        }
        if (sa.getBoolean(54, false)) {
            activityInfo = activity.info;
            activityInfo.flags |= 536870912;
        }
        boolean z;
        if (receiver) {
            activity.info.launchMode = 0;
            activity.info.configChanges = 0;
            if (sa.getBoolean(28, false)) {
                activityInfo = activity.info;
                activityInfo.flags |= 1073741824;
                if (activity.info.exported && (flags & 128) == 0) {
                    Slog.w(TAG, "Activity exported request ignored due to singleUser: " + activity.className + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    activity.info.exported = false;
                    setExported = true;
                }
            }
            activityInfo = activity.info;
            z = sa.getBoolean(42, false);
            activity.info.directBootAware = z;
            activityInfo.encryptionAware = z;
        } else {
            if (sa.getBoolean(25, hardwareAccelerated)) {
                activityInfo = activity.info;
                activityInfo.flags |= 512;
            }
            activity.info.launchMode = sa.getInt(14, 0);
            activity.info.documentLaunchMode = sa.getInt(33, 0);
            activity.info.maxRecents = sa.getInt(34, ActivityManager.getDefaultAppRecentsLimitStatic());
            activity.info.configChanges = getActivityConfigChanges(sa.getInt(16, 0), sa.getInt(47, 0));
            activity.info.softInputMode = sa.getInt(20, 0);
            activity.info.persistableMode = sa.getInteger(32, 0);
            if (sa.getBoolean(31, false)) {
                activityInfo = activity.info;
                activityInfo.flags |= Integer.MIN_VALUE;
            }
            if (sa.getBoolean(35, false)) {
                activityInfo = activity.info;
                activityInfo.flags |= 8192;
            }
            if (sa.getBoolean(36, false)) {
                activityInfo = activity.info;
                activityInfo.flags |= 4096;
            }
            if (sa.getBoolean(37, false)) {
                activityInfo = activity.info;
                activityInfo.flags |= 16384;
            }
            activity.info.screenOrientation = sa.getInt(15, -1);
            setActivityResizeMode(activity.info, sa, owner);
            if (sa.getBoolean(41, false)) {
                activityInfo = activity.info;
                activityInfo.flags |= 4194304;
            }
            if (sa.getBoolean(53, false)) {
                activityInfo = activity.info;
                activityInfo.flags |= 262144;
            }
            if (sa.hasValue(50) && sa.getType(50) == 4) {
                activity.setMaxAspectRatio(sa.getFloat(50, 0.0f));
            }
            activity.info.lockTaskLaunchMode = sa.getInt(38, 0);
            activityInfo = activity.info;
            z = sa.getBoolean(42, false);
            activity.info.directBootAware = z;
            activityInfo.encryptionAware = z;
            activity.info.requestedVrComponent = sa.getString(43);
            activity.info.rotationAnimation = sa.getInt(46, -1);
            activity.info.colorMode = sa.getInt(49, 0);
            if (sa.getBoolean(51, false)) {
                activityInfo = activity.info;
                activityInfo.flags |= 8388608;
            }
            if (sa.getBoolean(52, false)) {
                activityInfo = activity.info;
                activityInfo.flags |= 16777216;
            }
        }
        if (activity.info.directBootAware) {
            ApplicationInfo applicationInfo = owner.applicationInfo;
            applicationInfo.privateFlags |= 256;
        }
        boolean visibleToEphemeral = sa.getBoolean(45, false);
        if (visibleToEphemeral) {
            activityInfo = activity.info;
            activityInfo.flags |= 1048576;
            owner.visibleToInstantApps = true;
        }
        sa.recycle();
        if (receiver && (owner.applicationInfo.privateFlags & 2) != 0 && activity.info.processName == owner.packageName) {
            outError[0] = "Heavy-weight applications can not have receivers in main process";
        }
        if (outError[0] != null) {
            return null;
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                if (!setExported) {
                    activity.info.exported = activity.intents.size() > 0;
                }
            } else if (!(type == 3 || type == 4)) {
                ActivityIntentInfo intent;
                int visibility;
                if (parser.getName().equals("intent-filter")) {
                    intent = new ActivityIntentInfo(activity);
                    if (!parseIntent(res, parser, true, true, intent, outError)) {
                        return null;
                    }
                    if (intent.countActions() == 0) {
                        Slog.w(TAG, "No actions in intent filter at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    } else {
                        activity.intents.add(intent);
                    }
                    if (visibleToEphemeral) {
                        visibility = 1;
                    } else if (receiver || !isImplicitlyExposedIntent(intent)) {
                        visibility = 0;
                    } else {
                        visibility = 2;
                    }
                    intent.setVisibilityToInstantApp(visibility);
                    if (intent.isVisibleToInstantApp()) {
                        activityInfo = activity.info;
                        activityInfo.flags |= 1048576;
                    }
                    if (intent.isImplicitlyVisibleToInstantApp()) {
                        activityInfo = activity.info;
                        activityInfo.flags |= 2097152;
                    }
                } else if (!receiver && parser.getName().equals("preferred")) {
                    intent = new ActivityIntentInfo(activity);
                    if (!parseIntent(res, parser, false, false, intent, outError)) {
                        return null;
                    }
                    if (intent.countActions() != 0) {
                        if (owner.preferredActivityFilters == null) {
                            owner.preferredActivityFilters = new ArrayList();
                        }
                        owner.preferredActivityFilters.add(intent);
                    }
                    if (visibleToEphemeral) {
                        visibility = 1;
                    } else if (receiver || !isImplicitlyExposedIntent(intent)) {
                        visibility = 0;
                    } else {
                        visibility = 2;
                    }
                    intent.setVisibilityToInstantApp(visibility);
                    if (intent.isVisibleToInstantApp()) {
                        activityInfo = activity.info;
                        activityInfo.flags |= 1048576;
                    }
                    if (intent.isImplicitlyVisibleToInstantApp()) {
                        activityInfo = activity.info;
                        activityInfo.flags |= 2097152;
                    }
                } else if (parser.getName().equals("meta-data")) {
                    Bundle parseMetaData = parseMetaData(res, parser, activity.metaData, outError);
                    activity.metaData = parseMetaData;
                    if (parseMetaData == null) {
                        return null;
                    }
                    if (!visibleToEphemeral && activity.metaData.getBoolean(META_DATA_INSTANT_APPS)) {
                        int i;
                        visibleToEphemeral = true;
                        activityInfo = activity.info;
                        activityInfo.flags |= 1048576;
                        activityInfo = activity.info;
                        activityInfo.flags &= -2097153;
                        owner.visibleToInstantApps = true;
                        for (i = activity.intents.size() - 1; i >= 0; i--) {
                            ((ActivityIntentInfo) activity.intents.get(i)).setVisibilityToInstantApp(1);
                        }
                        if (owner.preferredActivityFilters != null) {
                            for (i = owner.preferredActivityFilters.size() - 1; i >= 0; i--) {
                                ((ActivityIntentInfo) owner.preferredActivityFilters.get(i)).setVisibilityToInstantApp(1);
                            }
                        }
                    }
                } else if (receiver || !parser.getName().equals(TtmlUtils.TAG_LAYOUT)) {
                    if (receiver) {
                        Slog.w(TAG, "Unknown element under <receiver>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    } else {
                        Slog.w(TAG, "Unknown element under <activity>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    parseLayout(res, parser, activity);
                }
            }
        }
        if (setExported) {
            activity.info.exported = activity.intents.size() > 0;
        }
        return activity;
    }

    private void setActivityResizeMode(ActivityInfo aInfo, TypedArray sa, Package owner) {
        boolean appExplicitDefault = (owner.applicationInfo.privateFlags & 3072) != 0;
        if (sa.hasValue(40) || appExplicitDefault) {
            if (sa.getBoolean(40, (owner.applicationInfo.privateFlags & 1024) != 0)) {
                aInfo.resizeMode = 2;
            } else {
                aInfo.resizeMode = 0;
            }
        } else if ((owner.applicationInfo.privateFlags & 4096) != 0) {
            aInfo.resizeMode = 1;
        } else {
            if (aInfo.isFixedOrientationPortrait()) {
                aInfo.resizeMode = 6;
            } else if (aInfo.isFixedOrientationLandscape()) {
                aInfo.resizeMode = 5;
            } else if (aInfo.isFixedOrientation()) {
                aInfo.resizeMode = 7;
            } else {
                aInfo.resizeMode = 4;
            }
        }
    }

    private void setMaxAspectRatio(Package owner) {
        float maxAspectRatio = owner.applicationInfo.targetSdkVersion < 26 ? DEFAULT_PRE_O_MAX_ASPECT_RATIO : 0.0f;
        if (owner.applicationInfo.maxAspectRatio != 0.0f) {
            maxAspectRatio = owner.applicationInfo.maxAspectRatio;
        } else if (owner.mAppMetaData != null && owner.mAppMetaData.containsKey(METADATA_MAX_ASPECT_RATIO)) {
            maxAspectRatio = owner.mAppMetaData.getFloat(METADATA_MAX_ASPECT_RATIO, maxAspectRatio);
            owner.applicationInfo.maxAspectRatio = owner.mAppMetaData.getFloat(METADATA_MAX_ASPECT_RATIO, 0.0f);
        }
        for (Activity activity : owner.activities) {
            if (!activity.hasMaxAspectRatio()) {
                float activityAspectRatio;
                if (activity.metaData != null) {
                    activityAspectRatio = activity.metaData.getFloat(METADATA_MAX_ASPECT_RATIO, maxAspectRatio);
                } else {
                    activityAspectRatio = maxAspectRatio;
                }
                activity.setMaxAspectRatio(activityAspectRatio);
            }
        }
    }

    public static int getActivityConfigChanges(int configChanges, int recreateOnConfigChanges) {
        return ((~recreateOnConfigChanges) & 3) | configChanges;
    }

    private void parseLayout(Resources res, AttributeSet attrs, Activity a) {
        TypedArray sw = res.obtainAttributes(attrs, R.styleable.AndroidManifestLayout);
        int width = -1;
        float widthFraction = -1.0f;
        int height = -1;
        float heightFraction = -1.0f;
        int widthType = sw.getType(3);
        if (widthType == 6) {
            widthFraction = sw.getFraction(3, 1, 1, -1.0f);
        } else if (widthType == 5) {
            width = sw.getDimensionPixelSize(3, -1);
        }
        int heightType = sw.getType(4);
        if (heightType == 6) {
            heightFraction = sw.getFraction(4, 1, 1, -1.0f);
        } else if (heightType == 5) {
            height = sw.getDimensionPixelSize(4, -1);
        }
        int gravity = sw.getInt(0, 17);
        int minWidth = sw.getDimensionPixelSize(1, -1);
        int minHeight = sw.getDimensionPixelSize(2, -1);
        sw.recycle();
        a.info.windowLayout = new WindowLayout(width, widthFraction, height, heightFraction, gravity, minWidth, minHeight);
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "YaoJun.Luo@Plf.SDK : Modify for rom activity-alias can set theme", property = OppoRomType.ROM)
    private Activity parseActivityAlias(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError, CachedComponentArgs cachedArgs) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestActivityAlias);
        String targetActivity = sa.getNonConfigurationString(8, 1024);
        if (targetActivity == null) {
            outError[0] = "<activity-alias> does not specify android:targetActivity";
            sa.recycle();
            return null;
        }
        targetActivity = buildClassName(owner.applicationInfo.packageName, targetActivity, outError);
        if (targetActivity == null) {
            sa.recycle();
            return null;
        }
        if (cachedArgs.mActivityAliasArgs == null) {
            cachedArgs.mActivityAliasArgs = new ParseComponentArgs(owner, outError, 3, 1, 2, 12, 9, 11, this.mSeparateProcesses, 0, 7, 5);
            cachedArgs.mActivityAliasArgs.tag = "<activity-alias>";
        }
        cachedArgs.mActivityAliasArgs.sa = sa;
        cachedArgs.mActivityAliasArgs.flags = flags;
        Activity target = null;
        int NA = owner.activities.size();
        for (int i = 0; i < NA; i++) {
            Activity t = (Activity) owner.activities.get(i);
            if (targetActivity.equals(t.info.name)) {
                target = t;
                break;
            }
        }
        if (target == null) {
            outError[0] = "<activity-alias> target activity " + targetActivity + " not found in manifest";
            sa.recycle();
            return null;
        }
        ActivityInfo info = new ActivityInfo();
        info.targetActivity = targetActivity;
        info.configChanges = target.info.configChanges;
        info.flags = target.info.flags;
        info.icon = target.info.icon;
        info.logo = target.info.logo;
        info.banner = target.info.banner;
        info.labelRes = target.info.labelRes;
        info.nonLocalizedLabel = target.info.nonLocalizedLabel;
        info.launchMode = target.info.launchMode;
        info.lockTaskLaunchMode = target.info.lockTaskLaunchMode;
        info.processName = target.info.processName;
        if (info.descriptionRes == 0) {
            info.descriptionRes = target.info.descriptionRes;
        }
        info.screenOrientation = target.info.screenOrientation;
        info.taskAffinity = target.info.taskAffinity;
        info.theme = target.info.theme;
        info.softInputMode = target.info.softInputMode;
        info.uiOptions = target.info.uiOptions;
        info.parentActivityName = target.info.parentActivityName;
        info.maxRecents = target.info.maxRecents;
        info.windowLayout = target.info.windowLayout;
        info.resizeMode = target.info.resizeMode;
        info.maxAspectRatio = target.info.maxAspectRatio;
        boolean z = target.info.directBootAware;
        info.directBootAware = z;
        info.encryptionAware = z;
        Activity activity = new Activity(cachedArgs.mActivityAliasArgs, info);
        if (outError[0] != null) {
            sa.recycle();
            return null;
        }
        activity.info.theme = sa.getResourceId(0, target.info.theme);
        boolean setExported = sa.hasValue(6);
        if (setExported) {
            activity.info.exported = sa.getBoolean(6, false);
        }
        String str = sa.getNonConfigurationString(4, 0);
        if (str != null) {
            activity.info.permission = str.length() > 0 ? str.toString().intern() : null;
        }
        String parentName = sa.getNonConfigurationString(10, 1024);
        if (parentName != null) {
            String parentClassName = buildClassName(activity.info.packageName, parentName, outError);
            if (outError[0] == null) {
                activity.info.parentActivityName = parentClassName;
            } else {
                Log.e(TAG, "Activity alias " + activity.info.name + " specified invalid parentActivityName " + parentName);
                outError[0] = null;
            }
        }
        boolean visibleToEphemeral = (activity.info.flags & 1048576) != 0;
        sa.recycle();
        if (outError[0] != null) {
            return null;
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                if (!setExported) {
                    activity.info.exported = activity.intents.size() > 0;
                }
            } else if (!(type == 3 || type == 4)) {
                if (parser.getName().equals("intent-filter")) {
                    ActivityIntentInfo intent = new ActivityIntentInfo(activity);
                    if (!parseIntent(res, parser, true, true, intent, outError)) {
                        return null;
                    }
                    int visibility;
                    ActivityInfo activityInfo;
                    if (intent.countActions() != 0) {
                        activity.intents.add(intent);
                    }
                    if (visibleToEphemeral) {
                        visibility = 1;
                    } else if (isImplicitlyExposedIntent(intent)) {
                        visibility = 2;
                    } else {
                        visibility = 0;
                    }
                    intent.setVisibilityToInstantApp(visibility);
                    if (intent.isVisibleToInstantApp()) {
                        activityInfo = activity.info;
                        activityInfo.flags |= 1048576;
                    }
                    if (intent.isImplicitlyVisibleToInstantApp()) {
                        activityInfo = activity.info;
                        activityInfo.flags |= 2097152;
                    }
                } else if (parser.getName().equals("meta-data")) {
                    Bundle parseMetaData = parseMetaData(res, parser, activity.metaData, outError);
                    activity.metaData = parseMetaData;
                    if (parseMetaData == null) {
                        return null;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <activity-alias>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        if (setExported) {
            activity.info.exported = activity.intents.size() > 0;
        }
        return activity;
    }

    private Provider parseProvider(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError, CachedComponentArgs cachedArgs) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestProvider);
        if (cachedArgs.mProviderArgs == null) {
            cachedArgs.mProviderArgs = new ParseComponentArgs(owner, outError, 2, 0, 1, 19, 15, 17, this.mSeparateProcesses, 8, 14, 6);
            cachedArgs.mProviderArgs.tag = "<provider>";
        }
        cachedArgs.mProviderArgs.sa = sa;
        cachedArgs.mProviderArgs.flags = flags;
        Provider p = new Provider(cachedArgs.mProviderArgs, new ProviderInfo());
        if (outError[0] != null) {
            sa.recycle();
            return null;
        }
        ProviderInfo providerInfo;
        boolean providerExportedDefault = false;
        if (owner.applicationInfo.targetSdkVersion < 17) {
            providerExportedDefault = true;
        }
        p.info.exported = sa.getBoolean(7, providerExportedDefault);
        String cpname = sa.getNonConfigurationString(10, 0);
        p.info.isSyncable = sa.getBoolean(11, false);
        String permission = sa.getNonConfigurationString(3, 0);
        String str = sa.getNonConfigurationString(4, 0);
        if (str == null) {
            str = permission;
        }
        if (str == null) {
            p.info.readPermission = owner.applicationInfo.permission;
        } else {
            p.info.readPermission = str.length() > 0 ? str.toString().intern() : null;
        }
        str = sa.getNonConfigurationString(5, 0);
        if (str == null) {
            str = permission;
        }
        if (str == null) {
            p.info.writePermission = owner.applicationInfo.permission;
        } else {
            p.info.writePermission = str.length() > 0 ? str.toString().intern() : null;
        }
        p.info.grantUriPermissions = sa.getBoolean(13, false);
        p.info.multiprocess = sa.getBoolean(9, false);
        p.info.initOrder = sa.getInt(12, 0);
        p.info.splitName = sa.getNonConfigurationString(21, 0);
        p.info.flags = 0;
        if (sa.getBoolean(16, false)) {
            providerInfo = p.info;
            providerInfo.flags |= 1073741824;
            if (p.info.exported && (flags & 128) == 0) {
                Slog.w(TAG, "Provider exported request ignored due to singleUser: " + p.className + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                p.info.exported = false;
            }
        }
        providerInfo = p.info;
        boolean z = sa.getBoolean(18, false);
        p.info.directBootAware = z;
        providerInfo.encryptionAware = z;
        if (p.info.directBootAware) {
            ApplicationInfo applicationInfo = owner.applicationInfo;
            applicationInfo.privateFlags |= 256;
        }
        boolean visibleToEphemeral = sa.getBoolean(20, false);
        if (visibleToEphemeral) {
            providerInfo = p.info;
            providerInfo.flags |= 1048576;
            owner.visibleToInstantApps = true;
        }
        sa.recycle();
        if ((owner.applicationInfo.privateFlags & 2) != 0 && p.info.processName == owner.packageName) {
            outError[0] = "Heavy-weight applications can not have providers in main process";
            return null;
        } else if (cpname == null) {
            outError[0] = "<provider> does not include authorities attribute";
            return null;
        } else if (cpname.length() <= 0) {
            outError[0] = "<provider> has empty authorities attribute";
            return null;
        } else {
            p.info.authority = cpname.intern();
            if (parseProviderTags(res, parser, visibleToEphemeral, owner, p, outError)) {
                return p;
            }
            return null;
        }
    }

    private boolean parseProviderTags(android.content.res.Resources r27, android.content.res.XmlResourceParser r28, boolean r29, android.content.pm.PackageParser.Package r30, android.content.pm.PackageParser.Provider r31, java.lang.String[] r32) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r18_1 android.os.PatternMatcher) in PHI: PHI: (r18_2 android.os.PatternMatcher) = (r18_0 android.os.PatternMatcher), (r18_1 android.os.PatternMatcher) binds: {(r18_0 android.os.PatternMatcher)=B:35:0x00e7, (r18_1 android.os.PatternMatcher)=B:36:0x00e9}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
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
        r26 = this;
        r16 = r28.getDepth();
    L_0x0004:
        r24 = r28.next();
        r4 = 1;
        r0 = r24;
        if (r0 == r4) goto L_0x0356;
    L_0x000d:
        r4 = 3;
        r0 = r24;
        if (r0 != r4) goto L_0x001a;
    L_0x0012:
        r4 = r28.getDepth();
        r0 = r16;
        if (r4 <= r0) goto L_0x0356;
    L_0x001a:
        r4 = 3;
        r0 = r24;
        if (r0 == r4) goto L_0x0004;
    L_0x001f:
        r4 = 4;
        r0 = r24;
        if (r0 == r4) goto L_0x0004;
    L_0x0024:
        r4 = r28.getName();
        r5 = "intent-filter";
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x0063;
    L_0x0031:
        r9 = new android.content.pm.PackageParser$ProviderIntentInfo;
        r0 = r31;
        r9.<init>(r0);
        r7 = 1;
        r8 = 0;
        r4 = r26;
        r5 = r27;
        r6 = r28;
        r10 = r32;
        r4 = r4.parseIntent(r5, r6, r7, r8, r9, r10);
        if (r4 != 0) goto L_0x004a;
    L_0x0048:
        r4 = 0;
        return r4;
    L_0x004a:
        if (r29 == 0) goto L_0x005b;
    L_0x004c:
        r4 = 1;
        r9.setVisibilityToInstantApp(r4);
        r0 = r31;
        r4 = r0.info;
        r5 = r4.flags;
        r6 = 1048576; // 0x100000 float:1.469368E-39 double:5.180654E-318;
        r5 = r5 | r6;
        r4.flags = r5;
    L_0x005b:
        r0 = r31;
        r4 = r0.intents;
        r4.add(r9);
        goto L_0x0004;
    L_0x0063:
        r4 = r28.getName();
        r5 = "meta-data";
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x00c6;
    L_0x0070:
        r0 = r31;
        r4 = r0.metaData;
        r0 = r26;
        r1 = r27;
        r2 = r28;
        r3 = r32;
        r4 = r0.parseMetaData(r1, r2, r4, r3);
        r0 = r31;
        r0.metaData = r4;
        if (r4 != 0) goto L_0x0088;
    L_0x0086:
        r4 = 0;
        return r4;
    L_0x0088:
        if (r29 != 0) goto L_0x0004;
    L_0x008a:
        r0 = r31;
        r4 = r0.metaData;
        r5 = "instantapps.clients.allowed";
        r4 = r4.getBoolean(r5);
        if (r4 == 0) goto L_0x0004;
    L_0x0097:
        r29 = 1;
        r0 = r31;
        r4 = r0.info;
        r5 = r4.flags;
        r6 = 1048576; // 0x100000 float:1.469368E-39 double:5.180654E-318;
        r5 = r5 | r6;
        r4.flags = r5;
        r4 = 1;
        r0 = r30;
        r0.visibleToInstantApps = r4;
        r0 = r31;
        r4 = r0.intents;
        r4 = r4.size();
        r13 = r4 + -1;
    L_0x00b3:
        if (r13 < 0) goto L_0x0004;
    L_0x00b5:
        r0 = r31;
        r4 = r0.intents;
        r4 = r4.get(r13);
        r4 = (android.content.pm.PackageParser.ProviderIntentInfo) r4;
        r5 = 1;
        r4.setVisibilityToInstantApp(r5);
        r13 = r13 + -1;
        goto L_0x00b3;
    L_0x00c6:
        r4 = r28.getName();
        r5 = "grant-uri-permission";
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x01a6;
    L_0x00d3:
        r4 = com.android.internal.R.styleable.AndroidManifestGrantUriPermission;
        r0 = r27;
        r1 = r28;
        r22 = r0.obtainAttributes(r1, r4);
        r18 = 0;
        r4 = 0;
        r5 = 0;
        r0 = r22;
        r23 = r0.getNonConfigurationString(r4, r5);
        if (r23 == 0) goto L_0x00f3;
    L_0x00e9:
        r18 = new android.os.PatternMatcher;
        r4 = 0;
        r0 = r18;
        r1 = r23;
        r0.<init>(r1, r4);
    L_0x00f3:
        r4 = 1;
        r5 = 0;
        r0 = r22;
        r23 = r0.getNonConfigurationString(r4, r5);
        if (r23 == 0) goto L_0x0107;
    L_0x00fd:
        r18 = new android.os.PatternMatcher;
        r4 = 1;
        r0 = r18;
        r1 = r23;
        r0.<init>(r1, r4);
    L_0x0107:
        r4 = 2;
        r5 = 0;
        r0 = r22;
        r23 = r0.getNonConfigurationString(r4, r5);
        if (r23 == 0) goto L_0x011b;
    L_0x0111:
        r18 = new android.os.PatternMatcher;
        r4 = 2;
        r0 = r18;
        r1 = r23;
        r0.<init>(r1, r4);
    L_0x011b:
        r22.recycle();
        if (r18 == 0) goto L_0x0165;
    L_0x0120:
        r0 = r31;
        r4 = r0.info;
        r4 = r4.uriPermissionPatterns;
        if (r4 != 0) goto L_0x0146;
    L_0x0128:
        r0 = r31;
        r4 = r0.info;
        r5 = 1;
        r5 = new android.os.PatternMatcher[r5];
        r4.uriPermissionPatterns = r5;
        r0 = r31;
        r4 = r0.info;
        r4 = r4.uriPermissionPatterns;
        r5 = 0;
        r4[r5] = r18;
    L_0x013a:
        r0 = r31;
        r4 = r0.info;
        r5 = 1;
        r4.grantUriPermissions = r5;
        com.android.internal.util.XmlUtils.skipCurrentTag(r28);
        goto L_0x0004;
    L_0x0146:
        r0 = r31;
        r4 = r0.info;
        r4 = r4.uriPermissionPatterns;
        r11 = r4.length;
        r4 = r11 + 1;
        r15 = new android.os.PatternMatcher[r4];
        r0 = r31;
        r4 = r0.info;
        r4 = r4.uriPermissionPatterns;
        r5 = 0;
        r6 = 0;
        java.lang.System.arraycopy(r4, r5, r15, r6, r11);
        r15[r11] = r18;
        r0 = r31;
        r4 = r0.info;
        r4.uriPermissionPatterns = r15;
        goto L_0x013a;
    L_0x0165:
        r4 = "PackageParser";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "Unknown element under <path-permission>: ";
        r5 = r5.append(r6);
        r6 = r28.getName();
        r5 = r5.append(r6);
        r6 = " at ";
        r5 = r5.append(r6);
        r0 = r26;
        r6 = r0.mArchiveSourcePath;
        r5 = r5.append(r6);
        r6 = " ";
        r5 = r5.append(r6);
        r6 = r28.getPositionDescription();
        r5 = r5.append(r6);
        r5 = r5.toString();
        android.util.Slog.w(r4, r5);
        com.android.internal.util.XmlUtils.skipCurrentTag(r28);
        goto L_0x0004;
    L_0x01a6:
        r4 = r28.getName();
        r5 = "path-permission";
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x0315;
    L_0x01b3:
        r4 = com.android.internal.R.styleable.AndroidManifestPathPermission;
        r0 = r27;
        r1 = r28;
        r22 = r0.obtainAttributes(r1, r4);
        r17 = 0;
        r4 = 0;
        r5 = 0;
        r0 = r22;
        r20 = r0.getNonConfigurationString(r4, r5);
        r4 = 1;
        r5 = 0;
        r0 = r22;
        r21 = r0.getNonConfigurationString(r4, r5);
        if (r21 != 0) goto L_0x01d3;
    L_0x01d1:
        r21 = r20;
    L_0x01d3:
        r4 = 2;
        r5 = 0;
        r0 = r22;
        r25 = r0.getNonConfigurationString(r4, r5);
        if (r25 != 0) goto L_0x01df;
    L_0x01dd:
        r25 = r20;
    L_0x01df:
        r12 = 0;
        if (r21 == 0) goto L_0x01e7;
    L_0x01e2:
        r21 = r21.intern();
        r12 = 1;
    L_0x01e7:
        if (r25 == 0) goto L_0x01ee;
    L_0x01e9:
        r25 = r25.intern();
        r12 = 1;
    L_0x01ee:
        if (r12 != 0) goto L_0x0231;
    L_0x01f0:
        r4 = "PackageParser";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "No readPermission or writePermssion for <path-permission>: ";
        r5 = r5.append(r6);
        r6 = r28.getName();
        r5 = r5.append(r6);
        r6 = " at ";
        r5 = r5.append(r6);
        r0 = r26;
        r6 = r0.mArchiveSourcePath;
        r5 = r5.append(r6);
        r6 = " ";
        r5 = r5.append(r6);
        r6 = r28.getPositionDescription();
        r5 = r5.append(r6);
        r5 = r5.toString();
        android.util.Slog.w(r4, r5);
        com.android.internal.util.XmlUtils.skipCurrentTag(r28);
        goto L_0x0004;
    L_0x0231:
        r4 = 3;
        r5 = 0;
        r0 = r22;
        r19 = r0.getNonConfigurationString(r4, r5);
        if (r19 == 0) goto L_0x0249;
    L_0x023b:
        r17 = new android.content.pm.PathPermission;
        r4 = 0;
        r0 = r17;
        r1 = r19;
        r2 = r21;
        r3 = r25;
        r0.<init>(r1, r4, r2, r3);
    L_0x0249:
        r4 = 4;
        r5 = 0;
        r0 = r22;
        r19 = r0.getNonConfigurationString(r4, r5);
        if (r19 == 0) goto L_0x0261;
    L_0x0253:
        r17 = new android.content.pm.PathPermission;
        r4 = 1;
        r0 = r17;
        r1 = r19;
        r2 = r21;
        r3 = r25;
        r0.<init>(r1, r4, r2, r3);
    L_0x0261:
        r4 = 5;
        r5 = 0;
        r0 = r22;
        r19 = r0.getNonConfigurationString(r4, r5);
        if (r19 == 0) goto L_0x0279;
    L_0x026b:
        r17 = new android.content.pm.PathPermission;
        r4 = 2;
        r0 = r17;
        r1 = r19;
        r2 = r21;
        r3 = r25;
        r0.<init>(r1, r4, r2, r3);
    L_0x0279:
        r4 = 6;
        r5 = 0;
        r0 = r22;
        r19 = r0.getNonConfigurationString(r4, r5);
        if (r19 == 0) goto L_0x0291;
    L_0x0283:
        r17 = new android.content.pm.PathPermission;
        r4 = 3;
        r0 = r17;
        r1 = r19;
        r2 = r21;
        r3 = r25;
        r0.<init>(r1, r4, r2, r3);
    L_0x0291:
        r22.recycle();
        if (r17 == 0) goto L_0x02d4;
    L_0x0296:
        r0 = r31;
        r4 = r0.info;
        r4 = r4.pathPermissions;
        if (r4 != 0) goto L_0x02b5;
    L_0x029e:
        r0 = r31;
        r4 = r0.info;
        r5 = 1;
        r5 = new android.content.pm.PathPermission[r5];
        r4.pathPermissions = r5;
        r0 = r31;
        r4 = r0.info;
        r4 = r4.pathPermissions;
        r5 = 0;
        r4[r5] = r17;
    L_0x02b0:
        com.android.internal.util.XmlUtils.skipCurrentTag(r28);
        goto L_0x0004;
    L_0x02b5:
        r0 = r31;
        r4 = r0.info;
        r4 = r4.pathPermissions;
        r11 = r4.length;
        r4 = r11 + 1;
        r14 = new android.content.pm.PathPermission[r4];
        r0 = r31;
        r4 = r0.info;
        r4 = r4.pathPermissions;
        r5 = 0;
        r6 = 0;
        java.lang.System.arraycopy(r4, r5, r14, r6, r11);
        r14[r11] = r17;
        r0 = r31;
        r4 = r0.info;
        r4.pathPermissions = r14;
        goto L_0x02b0;
    L_0x02d4:
        r4 = "PackageParser";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "No path, pathPrefix, or pathPattern for <path-permission>: ";
        r5 = r5.append(r6);
        r6 = r28.getName();
        r5 = r5.append(r6);
        r6 = " at ";
        r5 = r5.append(r6);
        r0 = r26;
        r6 = r0.mArchiveSourcePath;
        r5 = r5.append(r6);
        r6 = " ";
        r5 = r5.append(r6);
        r6 = r28.getPositionDescription();
        r5 = r5.append(r6);
        r5 = r5.toString();
        android.util.Slog.w(r4, r5);
        com.android.internal.util.XmlUtils.skipCurrentTag(r28);
        goto L_0x0004;
    L_0x0315:
        r4 = "PackageParser";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "Unknown element under <provider>: ";
        r5 = r5.append(r6);
        r6 = r28.getName();
        r5 = r5.append(r6);
        r6 = " at ";
        r5 = r5.append(r6);
        r0 = r26;
        r6 = r0.mArchiveSourcePath;
        r5 = r5.append(r6);
        r6 = " ";
        r5 = r5.append(r6);
        r6 = r28.getPositionDescription();
        r5 = r5.append(r6);
        r5 = r5.toString();
        android.util.Slog.w(r4, r5);
        com.android.internal.util.XmlUtils.skipCurrentTag(r28);
        goto L_0x0004;
    L_0x0356:
        r4 = 1;
        return r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.content.pm.PackageParser.parseProviderTags(android.content.res.Resources, android.content.res.XmlResourceParser, boolean, android.content.pm.PackageParser$Package, android.content.pm.PackageParser$Provider, java.lang.String[]):boolean");
    }

    private Service parseService(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError, CachedComponentArgs cachedArgs) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestService);
        if (cachedArgs.mServiceArgs == null) {
            cachedArgs.mServiceArgs = new ParseComponentArgs(owner, outError, 2, 0, 1, 15, 8, 12, this.mSeparateProcesses, 6, 7, 4);
            cachedArgs.mServiceArgs.tag = "<service>";
        }
        cachedArgs.mServiceArgs.sa = sa;
        cachedArgs.mServiceArgs.flags = flags;
        Service service = new Service(cachedArgs.mServiceArgs, new ServiceInfo());
        if (outError[0] != null) {
            sa.recycle();
            return null;
        }
        ServiceInfo serviceInfo;
        boolean setExported = sa.hasValue(5);
        if (setExported) {
            service.info.exported = sa.getBoolean(5, false);
        }
        String str = sa.getNonConfigurationString(3, 0);
        if (str == null) {
            service.info.permission = owner.applicationInfo.permission;
        } else {
            service.info.permission = str.length() > 0 ? str.toString().intern() : null;
        }
        service.info.splitName = sa.getNonConfigurationString(17, 0);
        service.info.flags = 0;
        if (sa.getBoolean(9, false)) {
            serviceInfo = service.info;
            serviceInfo.flags |= 1;
        }
        if (sa.getBoolean(10, false)) {
            serviceInfo = service.info;
            serviceInfo.flags |= 2;
        }
        if (sa.getBoolean(14, false)) {
            serviceInfo = service.info;
            serviceInfo.flags |= 4;
        }
        if (sa.getBoolean(11, false)) {
            serviceInfo = service.info;
            serviceInfo.flags |= 1073741824;
            if (service.info.exported && (flags & 128) == 0) {
                Slog.w(TAG, "Service exported request ignored due to singleUser: " + service.className + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                service.info.exported = false;
                setExported = true;
            }
        }
        serviceInfo = service.info;
        boolean z = sa.getBoolean(13, false);
        service.info.directBootAware = z;
        serviceInfo.encryptionAware = z;
        if (service.info.directBootAware) {
            ApplicationInfo applicationInfo = owner.applicationInfo;
            applicationInfo.privateFlags |= 256;
        }
        boolean visibleToEphemeral = sa.getBoolean(16, false);
        if (visibleToEphemeral) {
            serviceInfo = service.info;
            serviceInfo.flags |= 1048576;
            owner.visibleToInstantApps = true;
        }
        sa.recycle();
        if ((owner.applicationInfo.privateFlags & 2) == 0 || service.info.processName != owner.packageName) {
            int outerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                    if (!setExported) {
                        service.info.exported = service.intents.size() > 0;
                    }
                } else if (!(type == 3 || type == 4)) {
                    if (parser.getName().equals("intent-filter")) {
                        ServiceIntentInfo intent = new ServiceIntentInfo(service);
                        if (!parseIntent(res, parser, true, false, intent, outError)) {
                            return null;
                        }
                        if (visibleToEphemeral) {
                            intent.setVisibilityToInstantApp(1);
                            serviceInfo = service.info;
                            serviceInfo.flags |= 1048576;
                        }
                        service.intents.add(intent);
                    } else if (parser.getName().equals("meta-data")) {
                        Bundle parseMetaData = parseMetaData(res, parser, service.metaData, outError);
                        service.metaData = parseMetaData;
                        if (parseMetaData == null) {
                            return null;
                        }
                        if (!visibleToEphemeral && service.metaData.getBoolean(META_DATA_INSTANT_APPS)) {
                            visibleToEphemeral = true;
                            serviceInfo = service.info;
                            serviceInfo.flags |= 1048576;
                            owner.visibleToInstantApps = true;
                            for (int i = service.intents.size() - 1; i >= 0; i--) {
                                ((ServiceIntentInfo) service.intents.get(i)).setVisibilityToInstantApp(1);
                            }
                        }
                    } else {
                        Slog.w(TAG, "Unknown element under <service>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
            if (setExported) {
                service.info.exported = service.intents.size() > 0;
            }
            return service;
        }
        outError[0] = "Heavy-weight applications can not have services in main process";
        return null;
    }

    private boolean isImplicitlyExposedIntent(IntentInfo intent) {
        if (intent.hasCategory(Intent.CATEGORY_BROWSABLE) || intent.hasAction(Intent.ACTION_SEND) || intent.hasAction(Intent.ACTION_SENDTO)) {
            return true;
        }
        return intent.hasAction(Intent.ACTION_SEND_MULTIPLE);
    }

    private boolean parseAllMetaData(Resources res, XmlResourceParser parser, String tag, Component<?> outInfo, String[] outError) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return true;
            }
            if (!(type == 3 || type == 4)) {
                if (parser.getName().equals("meta-data")) {
                    Bundle parseMetaData = parseMetaData(res, parser, outInfo.metaData, outError);
                    outInfo.metaData = parseMetaData;
                    if (parseMetaData == null) {
                        return false;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under " + tag + ": " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        return true;
    }

    private Bundle parseMetaData(Resources res, XmlResourceParser parser, Bundle data, String[] outError) throws XmlPullParserException, IOException {
        String str = null;
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestMetaData);
        if (data == null) {
            data = new Bundle();
        }
        String name = sa.getNonConfigurationString(0, 0);
        if (name == null) {
            outError[0] = "<meta-data> requires an android:name attribute";
            sa.recycle();
            return null;
        }
        name = name.intern();
        TypedValue v = sa.peekValue(2);
        if (v == null || v.resourceId == 0) {
            v = sa.peekValue(1);
            if (v == null) {
                outError[0] = "<meta-data> requires an android:value or android:resource attribute";
                data = null;
            } else if (v.type == 3) {
                CharSequence cs = v.coerceToString();
                if (cs != null) {
                    str = cs.toString();
                }
                data.putString(name, str);
            } else if (v.type == 18) {
                data.putBoolean(name, v.data != 0);
            } else if (v.type >= 16 && v.type <= 31) {
                data.putInt(name, v.data);
            } else if (v.type == 4) {
                data.putFloat(name, v.getFloat());
            } else {
                Slog.w(TAG, "<meta-data> only supports string, integer, float, color, boolean, and resource reference types: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
            }
        } else {
            data.putInt(name, v.resourceId);
        }
        sa.recycle();
        XmlUtils.skipCurrentTag(parser);
        return data;
    }

    private static VerifierInfo parseVerifier(AttributeSet attrs) {
        String packageName = null;
        String encodedPublicKey = null;
        int attrCount = attrs.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            switch (attrs.getAttributeNameResource(i)) {
                case android.R.attr.name /*16842755*/:
                    packageName = attrs.getAttributeValue(i);
                    break;
                case android.R.attr.publicKey /*16843686*/:
                    encodedPublicKey = attrs.getAttributeValue(i);
                    break;
                default:
                    break;
            }
        }
        if (packageName == null || packageName.length() == 0) {
            Slog.i(TAG, "verifier package name was null; skipping");
            return null;
        }
        PublicKey publicKey = parsePublicKey(encodedPublicKey);
        if (publicKey != null) {
            return new VerifierInfo(packageName, publicKey);
        }
        Slog.i(TAG, "Unable to parse verifier public key for " + packageName);
        return null;
    }

    public static final PublicKey parsePublicKey(String encodedPublicKey) {
        EncodedKeySpec keySpec;
        if (encodedPublicKey == null) {
            Slog.w(TAG, "Could not parse null public key");
            return null;
        }
        try {
            keySpec = new X509EncodedKeySpec(Base64.decode(encodedPublicKey, 0));
            try {
                return KeyFactory.getInstance("RSA").generatePublic(keySpec);
            } catch (NoSuchAlgorithmException e) {
                Slog.wtf(TAG, "Could not parse public key: RSA KeyFactory not included in build");
            } catch (InvalidKeySpecException e2) {
            }
        } catch (IllegalArgumentException e3) {
            Slog.w(TAG, "Could not parse verifier public key; invalid Base64");
            return null;
        }
        return null;
        try {
            return KeyFactory.getInstance("EC").generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e4) {
            Slog.wtf(TAG, "Could not parse public key: EC KeyFactory not included in build");
        } catch (InvalidKeySpecException e5) {
        }
        try {
            return KeyFactory.getInstance("DSA").generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e6) {
            Slog.wtf(TAG, "Could not parse public key: DSA KeyFactory not included in build");
        } catch (InvalidKeySpecException e7) {
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean parseIntent(Resources res, XmlResourceParser parser, boolean allowGlobs, boolean allowAutoVerify, IntentInfo outInfo, String[] outError) throws XmlPullParserException, IOException {
        int roundIconVal;
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestIntentFilter);
        outInfo.setPriority(sa.getInt(2, 0));
        TypedValue v = sa.peekValue(0);
        if (v != null) {
            int i = v.resourceId;
            outInfo.labelRes = i;
            if (i == 0) {
                outInfo.nonLocalizedLabel = v.coerceToString();
            }
        }
        if (Resources.getSystem().getBoolean(17957052)) {
            roundIconVal = sa.getResourceId(6, 0);
        } else {
            roundIconVal = 0;
        }
        if (roundIconVal != 0) {
            outInfo.icon = roundIconVal;
        } else {
            outInfo.icon = sa.getResourceId(1, 0);
        }
        outInfo.logo = sa.getResourceId(3, 0);
        outInfo.banner = sa.getResourceId(4, 0);
        if (allowAutoVerify) {
            outInfo.setAutoVerify(sa.getBoolean(5, false));
        }
        sa.recycle();
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                outInfo.hasDefault = outInfo.hasCategory(Intent.CATEGORY_DEFAULT);
            } else if (!(type == 3 || type == 4)) {
                String nodeName = parser.getName();
                String value;
                if (nodeName.equals(Parameters.SCENE_MODE_ACTION)) {
                    value = parser.getAttributeValue(ANDROID_RESOURCES, MidiDeviceInfo.PROPERTY_NAME);
                    if (value == null || value == "") {
                        outError[0] = "No value supplied for <android:name>";
                    } else {
                        XmlUtils.skipCurrentTag(parser);
                        outInfo.addAction(value);
                    }
                } else if (nodeName.equals(CardEmulation.EXTRA_CATEGORY)) {
                    value = parser.getAttributeValue(ANDROID_RESOURCES, MidiDeviceInfo.PROPERTY_NAME);
                    if (value == null || value == "") {
                        outError[0] = "No value supplied for <android:name>";
                    } else {
                        XmlUtils.skipCurrentTag(parser);
                        outInfo.addCategory(value);
                    }
                } else if (nodeName.equals("data")) {
                    sa = res.obtainAttributes(parser, R.styleable.AndroidManifestData);
                    String str = sa.getNonConfigurationString(0, 0);
                    if (str != null) {
                        try {
                            outInfo.addDataType(str);
                        } catch (MalformedMimeTypeException e) {
                            outError[0] = e.toString();
                            sa.recycle();
                            return false;
                        }
                    }
                    str = sa.getNonConfigurationString(1, 0);
                    if (str != null) {
                        outInfo.addDataScheme(str);
                    }
                    str = sa.getNonConfigurationString(7, 0);
                    if (str != null) {
                        outInfo.addDataSchemeSpecificPart(str, 0);
                    }
                    str = sa.getNonConfigurationString(8, 0);
                    if (str != null) {
                        outInfo.addDataSchemeSpecificPart(str, 1);
                    }
                    str = sa.getNonConfigurationString(9, 0);
                    if (str != null) {
                        if (allowGlobs) {
                            outInfo.addDataSchemeSpecificPart(str, 2);
                        } else {
                            outError[0] = "sspPattern not allowed here; ssp must be literal";
                            return false;
                        }
                    }
                    String host = sa.getNonConfigurationString(2, 0);
                    String port = sa.getNonConfigurationString(3, 0);
                    if (host != null) {
                        outInfo.addDataAuthority(host, port);
                    }
                    str = sa.getNonConfigurationString(4, 0);
                    if (str != null) {
                        outInfo.addDataPath(str, 0);
                    }
                    str = sa.getNonConfigurationString(5, 0);
                    if (str != null) {
                        outInfo.addDataPath(str, 1);
                    }
                    str = sa.getNonConfigurationString(6, 0);
                    if (str != null) {
                        if (allowGlobs) {
                            outInfo.addDataPath(str, 2);
                        } else {
                            outError[0] = "pathPattern not allowed here; path must be literal";
                            return false;
                        }
                    }
                    str = sa.getNonConfigurationString(10, 0);
                    if (str != null) {
                        if (allowGlobs) {
                            outInfo.addDataPath(str, 3);
                        } else {
                            outError[0] = "pathAdvancedPattern not allowed here; path must be literal";
                            return false;
                        }
                    }
                    sa.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        outError[0] = "No value supplied for <android:name>";
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean copyNeeded(int flags, Package p, PackageUserState state, Bundle metaData, int userId) {
        if (userId != 0) {
            return true;
        }
        if (state.enabled != 0) {
            if (p.applicationInfo.enabled != (state.enabled == 1)) {
                return true;
            }
        }
        if (state.suspended != ((p.applicationInfo.flags & 1073741824) != 0) || !state.installed || state.hidden || state.stopped || state.instantApp != p.applicationInfo.isInstantApp()) {
            return true;
        }
        if ((flags & 128) == 0 || (metaData == null && p.mAppMetaData == null)) {
            return (((flags & 1024) == 0 || p.usesLibraryFiles == null) && p.staticSharedLibName == null) ? false : true;
        } else {
            return true;
        }
    }

    public static ApplicationInfo generateApplicationInfo(Package p, int flags, PackageUserState state) {
        return generateApplicationInfo(p, flags, state, UserHandle.getCallingUserId());
    }

    private static void updateApplicationInfo(ApplicationInfo ai, int flags, PackageUserState state) {
        boolean z = true;
        if (!sCompatibilityModeEnabled) {
            ai.disableCompatibilityMode();
        }
        if (state.installed) {
            ai.flags |= 8388608;
        } else {
            ai.flags &= -8388609;
        }
        if (state.suspended) {
            ai.flags |= 1073741824;
        } else {
            ai.flags &= -1073741825;
        }
        if (state.instantApp) {
            ai.privateFlags |= 128;
        } else {
            ai.privateFlags &= -129;
        }
        if (state.virtualPreload) {
            ai.privateFlags |= 65536;
        } else {
            ai.privateFlags &= -65537;
        }
        if (state.hidden) {
            ai.privateFlags |= 1;
        } else {
            ai.privateFlags &= -2;
        }
        if (state.enabled == 1) {
            ai.enabled = true;
        } else if (state.enabled == 4) {
            if ((32768 & flags) == 0) {
                z = false;
            }
            ai.enabled = z;
        } else if (state.enabled == 2 || state.enabled == 3) {
            ai.enabled = false;
        }
        ai.enabledSetting = state.enabled;
        if (ai.category == -1) {
            ai.category = state.categoryHint;
        }
        if (ai.category == -1) {
            ai.category = FallbackCategoryProvider.getFallbackCategory(ai.packageName);
        }
        ai.seInfoUser = SELinuxUtil.assignSeinfoUser(state);
        ai.resourceDirs = state.overlayPaths;
    }

    public static ApplicationInfo generateApplicationInfo(Package p, int flags, PackageUserState state, int userId) {
        if (p == null || !checkUseInstalledOrHidden(flags, state, p.applicationInfo) || (p.isMatch(flags) ^ 1) != 0) {
            return null;
        }
        if (!(userId != 999 || p == null || p.applicationInfo == null || p.applicationInfo.packageName == null || OppoMultiLauncherUtil.getInstance().isMultiApp(p.applicationInfo.packageName))) {
            userId = 0;
            p.applicationInfo.uid = UserHandle.getUid(0, p.applicationInfo.uid);
        }
        if (copyNeeded(flags, p, state, null, userId) || ((32768 & flags) != 0 && state.enabled == 4)) {
            ApplicationInfo ai = new ApplicationInfo(p.applicationInfo);
            ai.initForUser(userId);
            if ((flags & 128) != 0) {
                ai.metaData = p.mAppMetaData;
            }
            if ((flags & 1024) != 0) {
                ai.sharedLibraryFiles = p.usesLibraryFiles;
            }
            if (state.stopped) {
                ai.flags |= 2097152;
            } else {
                ai.flags &= -2097153;
            }
            updateApplicationInfo(ai, flags, state);
            return ai;
        }
        updateApplicationInfo(p.applicationInfo, flags, state);
        return p.applicationInfo;
    }

    public static ApplicationInfo generateApplicationInfo(ApplicationInfo ai, int flags, PackageUserState state, int userId) {
        if (ai == null || !checkUseInstalledOrHidden(flags, state, ai)) {
            return null;
        }
        ApplicationInfo ai2 = new ApplicationInfo(ai);
        ai2.initForUser(userId);
        if (state.stopped) {
            ai2.flags |= 2097152;
        } else {
            ai2.flags &= -2097153;
        }
        updateApplicationInfo(ai2, flags, state);
        return ai2;
    }

    public static final PermissionInfo generatePermissionInfo(Permission p, int flags) {
        if (p == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return p.info;
        }
        PermissionInfo pi = new PermissionInfo(p.info);
        pi.metaData = p.metaData;
        return pi;
    }

    public static final PermissionGroupInfo generatePermissionGroupInfo(PermissionGroup pg, int flags) {
        if (pg == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return pg.info;
        }
        PermissionGroupInfo pgi = new PermissionGroupInfo(pg.info);
        pgi.metaData = pg.metaData;
        return pgi;
    }

    public static final ActivityInfo generateActivityInfo(Activity a, int flags, PackageUserState state, int userId) {
        if (a == null || !checkUseInstalledOrHidden(flags, state, a.owner.applicationInfo)) {
            return null;
        }
        if (copyNeeded(flags, a.owner, state, a.metaData, userId)) {
            ActivityInfo ai = new ActivityInfo(a.info);
            ai.metaData = a.metaData;
            ai.applicationInfo = generateApplicationInfo(a.owner, flags, state, userId);
            return ai;
        }
        updateApplicationInfo(a.info.applicationInfo, flags, state);
        return a.info;
    }

    public static final ActivityInfo generateActivityInfo(ActivityInfo ai, int flags, PackageUserState state, int userId) {
        if (ai == null || !checkUseInstalledOrHidden(flags, state, ai.applicationInfo)) {
            return null;
        }
        ActivityInfo ai2 = new ActivityInfo(ai);
        ai2.applicationInfo = generateApplicationInfo(ai2.applicationInfo, flags, state, userId);
        return ai2;
    }

    public static final ServiceInfo generateServiceInfo(Service s, int flags, PackageUserState state, int userId) {
        if (s == null || !checkUseInstalledOrHidden(flags, state, s.owner.applicationInfo)) {
            return null;
        }
        if (copyNeeded(flags, s.owner, state, s.metaData, userId)) {
            ServiceInfo si = new ServiceInfo(s.info);
            si.metaData = s.metaData;
            si.applicationInfo = generateApplicationInfo(s.owner, flags, state, userId);
            return si;
        }
        updateApplicationInfo(s.info.applicationInfo, flags, state);
        return s.info;
    }

    public static final ProviderInfo generateProviderInfo(Provider p, int flags, PackageUserState state, int userId) {
        if (p == null || !checkUseInstalledOrHidden(flags, state, p.owner.applicationInfo)) {
            return null;
        }
        if (copyNeeded(flags, p.owner, state, p.metaData, userId) || ((flags & 2048) == 0 && p.info.uriPermissionPatterns != null)) {
            ProviderInfo pi = new ProviderInfo(p.info);
            pi.metaData = p.metaData;
            if ((flags & 2048) == 0) {
                pi.uriPermissionPatterns = null;
            }
            pi.applicationInfo = generateApplicationInfo(p.owner, flags, state, userId);
            return pi;
        }
        updateApplicationInfo(p.info.applicationInfo, flags, state);
        return p.info;
    }

    public static final InstrumentationInfo generateInstrumentationInfo(Instrumentation i, int flags) {
        if (i == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return i.info;
        }
        InstrumentationInfo ii = new InstrumentationInfo(i.info);
        ii.metaData = i.metaData;
        return ii;
    }

    public static void setCompatibilityModeEnabled(boolean compatibilityModeEnabled) {
        sCompatibilityModeEnabled = compatibilityModeEnabled;
    }

    public static long readFullyIgnoringContents(InputStream in) throws IOException {
        byte[] buffer = (byte[]) sBuffer.getAndSet(null);
        if (buffer == null) {
            buffer = new byte[4096];
        }
        int count = 0;
        while (true) {
            int n = in.read(buffer, 0, buffer.length);
            if (n != -1) {
                count += n;
            } else {
                sBuffer.set(buffer);
                return (long) count;
            }
        }
    }

    public static void closeQuietly(StrictJarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (Exception e) {
            }
        }
    }

    public void verifyEMMApkIfNeed(Context context, Package pkg) throws PackageParserException {
        for (String usesPerm : pkg.requestedPermissions) {
            if ("com.chinatelecom.permission.security.EMM".equals(usesPerm)) {
                verifyEMMCert(context, pkg);
                return;
            }
        }
    }

    private byte[] inputStream2ByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[256];
        while (true) {
            int nRead = is.read(data, 0, data.length);
            if (nRead != -1) {
                buffer.write(data, 0, nRead);
            } else {
                buffer.flush();
                return buffer.toByteArray();
            }
        }
    }

    private void verifyEMMCert(Context context, Package pkg) throws PackageParserException {
        byte[] emmCertBytes = getEMMCert(pkg);
        if (emmCertBytes == null) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Failed to get META-INF/EMM.CER");
        }
        String emmCert = new String(emmCertBytes);
        int index = emmCert.indexOf("Signature:");
        if (index == -1) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Signature of EMM.CER is missing");
        }
        Properties props = new Properties();
        try {
            props.load(new ByteArrayInputStream(emmCertBytes));
        } catch (IOException e) {
        }
        String signature = props.getProperty("Signature");
        if (signature == null) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Signature of EMM.CER is missing");
        } else if (checkSignature(emmCert.substring(0, index), signature)) {
            String apkHash = props.getProperty("ApkHash");
            String deviceIDs = props.getProperty("DeviceIds");
            if ((apkHash != null && !"*".equals(apkHash.trim())) || (deviceIDs != null && !"*".equals(deviceIDs.trim()))) {
                ManifestDigest digest = getManifestDigest(pkg);
                if (digest == null) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Failed to get META-INF/MANIFEST.MF");
                }
                if (!(apkHash == null || ("*".equals(apkHash.trim()) ^ 1) == 0)) {
                    ManifestDigest apkHashDigest = new ManifestDigest(hexStringToByteArray(apkHash));
                    if (!apkHashDigest.equals(digest)) {
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Package has mismatched apk hash: expected " + apkHashDigest + ", got " + digest);
                    }
                }
                if (!validateDeviceIDs(context, props)) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "IMEI isn't in the device list");
                } else if (!validateFromTo(props)) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Validity of the certificate has expired");
                }
            } else if ("*".equals(apkHash) && "*".equals(deviceIDs)) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Both ApkHash and DeviceIDs are '*'");
            } else {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Both ApkHash and DeviceIDs are missing");
            }
        } else {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Signature of EMM.CER mismatches content");
        }
    }

    private boolean validateDeviceIDs(Context context, Properties props) {
        String deviceIdsProp = props.getProperty("DeviceIds");
        if (deviceIdsProp == null || deviceIdsProp.trim().equals("*")) {
            return true;
        }
        String[] deviceIDs = deviceIdsProp.split(",");
        String imei = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getImei(0);
        for (String id : deviceIDs) {
            if (id.equalsIgnoreCase("IMEI/" + imei)) {
                return true;
            }
        }
        return false;
    }

    private static boolean validateFromTo(Properties props) {
        Date now = new Date();
        Date to = null;
        Date from = null;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String validFromProp = props.getProperty("ValidFrom");
        if (validFromProp != null) {
            try {
                from = df.parse(validFromProp);
            } catch (ParseException e) {
            }
        }
        String validToProp = props.getProperty("ValidTo");
        if (validToProp != null) {
            try {
                to = df.parse(validToProp);
            } catch (ParseException e2) {
            }
        }
        if ((from == null || !now.before(from)) && (to == null || !now.after(to))) {
            return true;
        }
        return false;
    }

    private boolean checkSignature(String content, String signature) {
        try {
            PublicKey key = ((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(CTEMM_PEM_PUBLIC_KEY.getBytes()))).getPublicKey();
            byte[] decode = Base64.decode(signature, 0);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(key);
            sig.update(content.getBytes());
            return sig.verify(decode);
        } catch (CertificateException e) {
            return false;
        }
    }

    private byte[] getEMMCert(Package pkg) {
        byte[] emmCert = null;
        StrictJarFile jarFile;
        try {
            jarFile = new StrictJarFile(pkg.baseCodePath);
            ZipEntry je = jarFile.findEntry("META-INF/EMM.CER");
            if (je != null) {
                emmCert = inputStream2ByteArray(jarFile.getInputStream(je));
            }
            jarFile.close();
        } catch (IOException e) {
        } catch (Throwable th) {
            jarFile.close();
        }
        return emmCert;
    }

    private ManifestDigest getManifestDigest(Package pkg) throws PackageParserException {
        ManifestDigest digest = null;
        StrictJarFile jarFile;
        try {
            jarFile = new StrictJarFile(pkg.baseCodePath);
            ZipEntry je = jarFile.findEntry("META-INF/MANIFEST.MF");
            if (je != null) {
                digest = ManifestDigest.fromInputStream(jarFile.getInputStream(je));
            }
            jarFile.close();
        } catch (IOException e) {
        } catch (Throwable th) {
            jarFile.close();
        }
        return digest;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[(len / 2)];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static boolean validateFromToForPackage(File apkFile) {
        Throwable th;
        StrictJarFile jarFile = null;
        try {
            StrictJarFile jarFile2 = new StrictJarFile(apkFile.getAbsolutePath());
            try {
                ZipEntry je = jarFile2.findEntry("META-INF/EMM.CER");
                if (je != null) {
                    Properties props = new Properties();
                    props.load(jarFile2.getInputStream(je));
                    boolean validateFromTo = validateFromTo(props);
                    closeQuietly(jarFile2);
                    return validateFromTo;
                }
                closeQuietly(jarFile2);
                return false;
            } catch (IOException e) {
                jarFile = jarFile2;
                closeQuietly(jarFile);
                return false;
            } catch (Throwable th2) {
                th = th2;
                jarFile = jarFile2;
                closeQuietly(jarFile);
                throw th;
            }
        } catch (IOException e2) {
            closeQuietly(jarFile);
            return false;
        } catch (Throwable th3) {
            th = th3;
            closeQuietly(jarFile);
            throw th;
        }
    }
}
