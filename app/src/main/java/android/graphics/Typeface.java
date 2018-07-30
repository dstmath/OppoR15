package android.graphics;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.content.res.AssetManager;
import android.content.res.FontResourcesParser.FamilyResourceEntry;
import android.content.res.FontResourcesParser.FontFamilyFilesResourceEntry;
import android.content.res.FontResourcesParser.FontFileResourceEntry;
import android.content.res.FontResourcesParser.ProviderResourceEntry;
import android.graphics.fonts.FontVariationAxis;
import android.net.Uri;
import android.provider.FontRequest;
import android.provider.FontsContract;
import android.provider.FontsContract.FontInfo;
import android.text.FontConfig.Family;
import android.text.FontConfig.Font;
import android.util.Base64;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.LruCache;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import oppo.content.res.OppoFontUtils;

public class Typeface {
    public static final int BOLD = 1;
    public static final int BOLD_ITALIC = 3;
    public static final Typeface DEFAULT = create((String) null, 0);
    public static final Typeface DEFAULT_BOLD = create((String) null, 1);
    private static final int[] EMPTY_AXES = new int[0];
    static final String FONTS_CONFIG = "fonts.xml";
    private static final String[] FontsLikeDefault = new String[]{"sans-serif-light", "sans-serif-medium"};
    public static final int ITALIC = 2;
    public static final String LIGHT_PATH = "/system/fonts/Roboto-Light.ttf";
    public static final String MEDIUM_PATH = "/system/fonts/ColorOSUI-Medium.ttf";
    public static final String MEDIUM_PATH2 = "/system/fonts/NotoSansSC-Medium.otf";
    public static final Typeface MONOSPACE = create("monospace", 0);
    public static final int NORMAL = 0;
    public static final int RESOLVE_BY_FONT_TABLE = -1;
    public static final Typeface SANS_SERIF = create("sans-serif", 0);
    public static final Typeface SERIF = create("serif", 0);
    private static final int STYLE_ITALIC = 1;
    private static final int STYLE_NORMAL = 0;
    private static String TAG = "Typeface";
    public static final String THIN_PATH = "/system/fonts/Roboto-Thin.ttf";
    public static final String XLIGHT_PATH = "/system/fonts/ColorOSUI-XLight.ttf";
    public static final String XTHIN_PATH = "/system/fonts/ColorOSUI-XThin.ttf";
    static Typeface sDefaultTypeface;
    static Typeface[] sDefaults = new Typeface[]{DEFAULT, DEFAULT_BOLD, create((String) null, 2), create((String) null, 3)};
    @GuardedBy("sLock")
    private static final LruCache<String, Typeface> sDynamicTypefaceCache = new LruCache(16);
    static FontFamily[] sFallbackFonts;
    private static final Object sLock = new Object();
    static Map<String, Typeface> sSystemFontMap;
    private static final LongSparseArray<SparseArray<Typeface>> sTypefaceCache = new LongSparseArray(3);
    public boolean isLikeDefault;
    private int mStyle;
    private int[] mSupportedAxes;
    private int mWeight;
    public long native_instance;

    public static final class Builder {
        public static final int BOLD_WEIGHT = 700;
        public static final int NORMAL_WEIGHT = 400;
        private static final Object sLock = new Object();
        @GuardedBy("sLock")
        private static final LongSparseArray<SparseArray<Typeface>> sTypefaceCache = new LongSparseArray(3);
        private AssetManager mAssetManager;
        private FontVariationAxis[] mAxes;
        private String mFallbackFamilyName;
        private FileDescriptor mFd;
        private Map<Uri, ByteBuffer> mFontBuffers;
        private FontInfo[] mFonts;
        private int mItalic = -1;
        private String mPath;
        private int mTtcIndex;
        private int mWeight = -1;

        public Builder(File path) {
            this.mPath = path.getAbsolutePath();
        }

        public Builder(FileDescriptor fd) {
            this.mFd = fd;
        }

        public Builder(String path) {
            this.mPath = path;
        }

        public Builder(AssetManager assetManager, String path) {
            this.mAssetManager = (AssetManager) Preconditions.checkNotNull(assetManager);
            this.mPath = (String) Preconditions.checkStringNotEmpty(path);
        }

        public Builder(FontInfo[] fonts, Map<Uri, ByteBuffer> buffers) {
            this.mFonts = fonts;
            this.mFontBuffers = buffers;
        }

        public Builder setWeight(int weight) {
            this.mWeight = weight;
            return this;
        }

        public Builder setItalic(boolean italic) {
            this.mItalic = italic ? 1 : 0;
            return this;
        }

        public Builder setTtcIndex(int ttcIndex) {
            if (this.mFonts != null) {
                throw new IllegalArgumentException("TTC index can not be specified for FontResult source.");
            }
            this.mTtcIndex = ttcIndex;
            return this;
        }

        public Builder setFontVariationSettings(String variationSettings) {
            if (this.mFonts != null) {
                throw new IllegalArgumentException("Font variation settings can not be specified for FontResult source.");
            } else if (this.mAxes != null) {
                throw new IllegalStateException("Font variation settings are already set.");
            } else {
                this.mAxes = FontVariationAxis.fromFontVariationSettings(variationSettings);
                return this;
            }
        }

        public Builder setFontVariationSettings(FontVariationAxis[] axes) {
            if (this.mFonts != null) {
                throw new IllegalArgumentException("Font variation settings can not be specified for FontResult source.");
            } else if (this.mAxes != null) {
                throw new IllegalStateException("Font variation settings are already set.");
            } else {
                this.mAxes = axes;
                return this;
            }
        }

        public Builder setFallback(String familyName) {
            this.mFallbackFamilyName = familyName;
            return this;
        }

        private static String createAssetUid(AssetManager mgr, String path, int ttcIndex, FontVariationAxis[] axes, int weight, int italic) {
            SparseArray<String> pkgs = mgr.getAssignedPackageIdentifiers();
            StringBuilder builder = new StringBuilder();
            int size = pkgs.size();
            for (int i = 0; i < size; i++) {
                builder.append((String) pkgs.valueAt(i));
                builder.append("-");
            }
            builder.append(path);
            builder.append("-");
            builder.append(Integer.toString(ttcIndex));
            builder.append("-");
            builder.append(Integer.toString(weight));
            builder.append("-");
            builder.append(Integer.toString(italic));
            builder.append("-");
            if (axes != null) {
                for (FontVariationAxis axis : axes) {
                    builder.append(axis.getTag());
                    builder.append("-");
                    builder.append(Float.toString(axis.getStyleValue()));
                }
            }
            return builder.toString();
        }

        private Typeface resolveFallbackTypeface() {
            int i = 1;
            if (this.mFallbackFamilyName == null) {
                return null;
            }
            Typeface base = (Typeface) Typeface.sSystemFontMap.get(this.mFallbackFamilyName);
            if (base == null) {
                base = Typeface.sDefaultTypeface;
            }
            if (this.mWeight == -1 && this.mItalic == -1) {
                return base;
            }
            int weight = this.mWeight == -1 ? base.mWeight : this.mWeight;
            boolean italic = this.mItalic != -1 ? this.mItalic != 1 : (base.mStyle & 2) == 0;
            int i2 = weight << 1;
            if (!italic) {
                i = 0;
            }
            int key = i2 | i;
            synchronized (sLock) {
                Typeface typeface;
                SparseArray<Typeface> innerCache = (SparseArray) sTypefaceCache.get(base.native_instance);
                if (innerCache != null) {
                    typeface = (Typeface) innerCache.get(key);
                    if (typeface != null) {
                        return typeface;
                    }
                }
                typeface = new Typeface(Typeface.nativeCreateFromTypefaceWithExactStyle(base.native_instance, weight, italic), null);
                if (innerCache == null) {
                    innerCache = new SparseArray(4);
                    sTypefaceCache.put(base.native_instance, innerCache);
                }
                innerCache.put(key, typeface);
                return typeface;
            }
        }

        public Typeface build() {
            Throwable th;
            Throwable th2;
            FontFamily fontFamily;
            if (this.mFd != null) {
                Throwable th3 = null;
                FileInputStream fis = null;
                try {
                    FileInputStream fileInputStream = new FileInputStream(this.mFd);
                    try {
                        FileChannel channel = fileInputStream.getChannel();
                        ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
                        fontFamily = new FontFamily();
                        Typeface resolveFallbackTypeface;
                        if (!fontFamily.addFontFromBuffer(buffer, this.mTtcIndex, this.mAxes, this.mWeight, this.mItalic)) {
                            fontFamily.abortCreation();
                            resolveFallbackTypeface = resolveFallbackTypeface();
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (Throwable th4) {
                                    th3 = th4;
                                }
                            }
                            if (th3 == null) {
                                return resolveFallbackTypeface;
                            }
                            try {
                                throw th3;
                            } catch (IOException e) {
                                fis = fileInputStream;
                            }
                        } else if (fontFamily.freeze()) {
                            resolveFallbackTypeface = Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, this.mWeight, this.mItalic);
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (Throwable th5) {
                                    th3 = th5;
                                }
                            }
                            if (th3 == null) {
                                return resolveFallbackTypeface;
                            }
                            throw th3;
                        } else {
                            resolveFallbackTypeface = resolveFallbackTypeface();
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (Throwable th6) {
                                    th3 = th6;
                                }
                            }
                            if (th3 == null) {
                                return resolveFallbackTypeface;
                            }
                            throw th3;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        fis = fileInputStream;
                        th2 = null;
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (Throwable th8) {
                                if (th2 == null) {
                                    th2 = th8;
                                } else if (th2 != th8) {
                                    th2.addSuppressed(th8);
                                }
                            }
                        }
                        if (th2 == null) {
                            throw th;
                        }
                        try {
                            throw th2;
                        } catch (IOException e2) {
                            return resolveFallbackTypeface();
                        }
                    }
                } catch (Throwable th9) {
                    th = th9;
                    th2 = null;
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Throwable th82) {
                            if (th2 == null) {
                                th2 = th82;
                            } else if (th2 != th82) {
                                th2.addSuppressed(th82);
                            }
                        }
                    }
                    if (th2 == null) {
                        try {
                            throw th2;
                        } catch (IOException e22) {
                            return resolveFallbackTypeface();
                        }
                    }
                    throw th;
                }
            } else if (this.mAssetManager != null) {
                String key = createAssetUid(this.mAssetManager, this.mPath, this.mTtcIndex, this.mAxes, this.mWeight, this.mItalic);
                synchronized (sLock) {
                    Typeface typeface = (Typeface) Typeface.sDynamicTypefaceCache.get(key);
                    if (typeface != null) {
                        return typeface;
                    }
                    fontFamily = new FontFamily();
                    if (!fontFamily.addFontFromAssetManager(this.mAssetManager, this.mPath, this.mTtcIndex, true, this.mTtcIndex, this.mWeight, this.mItalic, this.mAxes)) {
                        fontFamily.abortCreation();
                        return resolveFallbackTypeface();
                    } else if (fontFamily.freeze()) {
                        typeface = Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, this.mWeight, this.mItalic);
                        Typeface.sDynamicTypefaceCache.put(key, typeface);
                        return typeface;
                    } else {
                        return resolveFallbackTypeface();
                    }
                }
            } else if (this.mPath != null) {
                fontFamily = new FontFamily();
                if (!fontFamily.addFont(this.mPath, this.mTtcIndex, this.mAxes, this.mWeight, this.mItalic)) {
                    fontFamily.abortCreation();
                    return resolveFallbackTypeface();
                } else if (!fontFamily.freeze()) {
                    return resolveFallbackTypeface();
                } else {
                    return Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, this.mWeight, this.mItalic);
                }
            } else if (this.mFonts != null) {
                fontFamily = new FontFamily();
                boolean atLeastOneFont = false;
                for (FontInfo font : this.mFonts) {
                    ByteBuffer fontBuffer = (ByteBuffer) this.mFontBuffers.get(font.getUri());
                    if (fontBuffer != null) {
                        if (fontFamily.addFontFromBuffer(fontBuffer, font.getTtcIndex(), font.getAxes(), font.getWeight(), font.isItalic() ? 1 : 0)) {
                            atLeastOneFont = true;
                        } else {
                            fontFamily.abortCreation();
                            return null;
                        }
                    }
                }
                if (atLeastOneFont) {
                    fontFamily.freeze();
                    return Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, this.mWeight, this.mItalic);
                }
                fontFamily.abortCreation();
                return null;
            } else {
                throw new IllegalArgumentException("No source was set.");
            }
        }
    }

    /* synthetic */ Typeface(long ni, Typeface -this1) {
        this(ni);
    }

    private static native long nativeCreateFromArray(long[] jArr, int i, int i2);

    private static native long nativeCreateFromTypeface(long j, int i);

    private static native long nativeCreateFromTypefaceWithExactStyle(long j, int i, boolean z);

    private static native long nativeCreateFromTypefaceWithVariation(long j, List<FontVariationAxis> list);

    private static native long nativeCreateWeightAlias(long j, int i);

    private static native int nativeGetStyle(long j);

    private static native int[] nativeGetSupportedAxes(long j);

    private static native int nativeGetWeight(long j);

    private static native void nativeSetDefault(long j);

    private static native void nativeUnref(long j);

    static {
        init();
    }

    private static void setDefault(Typeface t) {
        sDefaultTypeface = t;
        nativeSetDefault(t.native_instance);
    }

    public int getStyle() {
        return this.mStyle;
    }

    public final boolean isBold() {
        return (this.mStyle & 1) != 0;
    }

    public final boolean isItalic() {
        return (this.mStyle & 2) != 0;
    }

    public static Typeface createFromResources(AssetManager mgr, String path, int cookie) {
        if (sFallbackFonts != null) {
            synchronized (sDynamicTypefaceCache) {
                String key = Builder.createAssetUid(mgr, path, 0, null, -1, -1);
                Typeface typeface = (Typeface) sDynamicTypefaceCache.get(key);
                if (typeface != null) {
                    return typeface;
                }
                FontFamily fontFamily = new FontFamily();
                if (fontFamily.addFontFromAssetManager(mgr, path, cookie, false, 0, -1, -1, null)) {
                    if (fontFamily.freeze()) {
                        typeface = createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, -1, -1);
                        sDynamicTypefaceCache.put(key, typeface);
                        return typeface;
                    }
                    return null;
                }
            }
        }
        return null;
    }

    public static Typeface createFromResources(FamilyResourceEntry entry, AssetManager mgr, String path) {
        if (sFallbackFonts == null) {
            return null;
        }
        Typeface typeface;
        if (entry instanceof ProviderResourceEntry) {
            ProviderResourceEntry providerEntry = (ProviderResourceEntry) entry;
            List<List<String>> givenCerts = providerEntry.getCerts();
            List<List<byte[]>> certs = new ArrayList();
            if (givenCerts != null) {
                for (int i = 0; i < givenCerts.size(); i++) {
                    List<String> certSet = (List) givenCerts.get(i);
                    List<byte[]> byteArraySet = new ArrayList();
                    for (int j = 0; j < certSet.size(); j++) {
                        byteArraySet.add(Base64.decode((String) certSet.get(j), 0));
                    }
                    certs.add(byteArraySet);
                }
            }
            typeface = FontsContract.getFontSync(new FontRequest(providerEntry.getAuthority(), providerEntry.getPackage(), providerEntry.getQuery(), certs));
            if (typeface == null) {
                typeface = DEFAULT;
            }
            return typeface;
        }
        typeface = findFromCache(mgr, path);
        if (typeface != null) {
            return typeface;
        }
        FontFamilyFilesResourceEntry filesEntry = (FontFamilyFilesResourceEntry) entry;
        FontFamily fontFamily = new FontFamily();
        FontFileResourceEntry[] entries = filesEntry.getEntries();
        int i2 = 0;
        int length = entries.length;
        while (true) {
            int i3 = i2;
            if (i3 < length) {
                FontFileResourceEntry fontFile = entries[i3];
                if (!fontFamily.addFontFromAssetManager(mgr, fontFile.getFileName(), 0, false, 0, fontFile.getWeight(), fontFile.getItalic(), null)) {
                    return null;
                }
                i2 = i3 + 1;
            } else if (!fontFamily.freeze()) {
                return null;
            } else {
                typeface = createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, -1, -1);
                synchronized (sDynamicTypefaceCache) {
                    sDynamicTypefaceCache.put(Builder.createAssetUid(mgr, path, 0, null, -1, -1), typeface);
                }
                return typeface;
            }
        }
    }

    public static Typeface findFromCache(AssetManager mgr, String path) {
        synchronized (sDynamicTypefaceCache) {
            Typeface typeface = (Typeface) sDynamicTypefaceCache.get(Builder.createAssetUid(mgr, path, 0, null, -1, -1));
            if (typeface != null) {
                return typeface;
            }
            return null;
        }
    }

    public static Typeface create(String familyName, int style) {
        if (sSystemFontMap == null) {
            return null;
        }
        Typeface tf = create((Typeface) sSystemFontMap.get(familyName), style);
        int ix = 0;
        while (ix < FontsLikeDefault.length) {
            if (familyName != null && familyName.equals(FontsLikeDefault[ix])) {
                tf.isLikeDefault = true;
                break;
            }
            ix++;
        }
        return tf;
    }

    public static Typeface create(Typeface family, int style) {
        Typeface typeface;
        if (style < 0 || style > 3) {
            style = 0;
        }
        long ni = 0;
        if (family != null) {
            if (family.mStyle == style) {
                return family;
            }
            ni = family.native_instance;
        }
        SparseArray<Typeface> styles = (SparseArray) sTypefaceCache.get(ni);
        if (styles != null) {
            typeface = (Typeface) styles.get(style);
            if (typeface != null) {
                return typeface;
            }
        }
        typeface = new Typeface(nativeCreateFromTypeface(ni, style));
        if (styles == null) {
            styles = new SparseArray(4);
            sTypefaceCache.put(ni, styles);
        }
        styles.put(style, typeface);
        return typeface;
    }

    public static Typeface createFromTypefaceWithVariation(Typeface family, List<FontVariationAxis> axes) {
        return new Typeface(nativeCreateFromTypefaceWithVariation(family == null ? 0 : family.native_instance, axes));
    }

    public static Typeface defaultFromStyle(int style) {
        if (OppoFontUtils.isFlipFontUsed) {
            return OppoFontUtils.flipTypeface(sDefaults[style]);
        }
        return sDefaults[style];
    }

    public static Typeface createFromAsset(AssetManager mgr, String path) {
        if (path == null) {
            throw new NullPointerException();
        }
        if (sFallbackFonts != null) {
            synchronized (sLock) {
                Typeface typeface = new Builder(mgr, path).build();
                if (typeface != null) {
                    return typeface;
                }
                String key = Builder.createAssetUid(mgr, path, 0, null, -1, -1);
                typeface = (Typeface) sDynamicTypefaceCache.get(key);
                if (typeface != null) {
                    return typeface;
                }
                FontFamily fontFamily = new FontFamily();
                if (fontFamily.addFontFromAssetManager(mgr, path, 0, true, 0, -1, -1, null)) {
                    fontFamily.allowUnsupportedFont();
                    fontFamily.freeze();
                    typeface = createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, -1, -1);
                    sDynamicTypefaceCache.put(key, typeface);
                    return typeface;
                }
                fontFamily.abortCreation();
            }
        }
        throw new RuntimeException("Font asset not found " + path);
    }

    private static String createProviderUid(String authority, String query) {
        StringBuilder builder = new StringBuilder();
        builder.append("provider:");
        builder.append(authority);
        builder.append("-");
        builder.append(query);
        return builder.toString();
    }

    public static Typeface createFromFile(File path) {
        return createFromFile(path.getAbsolutePath());
    }

    public static Typeface createFromFile(String path) {
        if (path != null && (MEDIUM_PATH.equals(path) || MEDIUM_PATH2.equals(path))) {
            return DEFAULT_BOLD;
        }
        if (sFallbackFonts != null) {
            FontFamily fontFamily = new FontFamily();
            if (fontFamily.addFont(path, 0, null, -1, -1)) {
                fontFamily.allowUnsupportedFont();
                fontFamily.freeze();
                return createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, -1, -1);
            }
            fontFamily.abortCreation();
        }
        throw new RuntimeException("Font not found " + path);
    }

    private static Typeface createFromFamilies(FontFamily[] families) {
        long[] ptrArray = new long[families.length];
        for (int i = 0; i < families.length; i++) {
            ptrArray[i] = families[i].mNativePtr;
        }
        return new Typeface(nativeCreateFromArray(ptrArray, -1, -1));
    }

    private static Typeface createFromFamiliesWithDefault(FontFamily[] families, int weight, int italic) {
        int i;
        long[] ptrArray = new long[(families.length + sFallbackFonts.length)];
        for (i = 0; i < families.length; i++) {
            ptrArray[i] = families[i].mNativePtr;
        }
        for (i = 0; i < sFallbackFonts.length; i++) {
            ptrArray[families.length + i] = sFallbackFonts[i].mNativePtr;
        }
        return new Typeface(nativeCreateFromArray(ptrArray, weight, italic));
    }

    private Typeface(long ni) {
        this.mStyle = 0;
        this.mWeight = 0;
        this.isLikeDefault = false;
        if (ni == 0) {
            throw new RuntimeException("native typeface cannot be made");
        }
        this.native_instance = ni;
        this.mStyle = nativeGetStyle(ni);
        this.mWeight = nativeGetWeight(ni);
    }

    private static FontFamily makeFamilyFromParsed(Family family, Map<String, ByteBuffer> bufferForPath) {
        IOException e;
        Throwable th;
        Throwable th2;
        FontFamily fontFamily = new FontFamily(family.getLanguage(), family.getVariant());
        Font[] fonts = family.getFonts();
        int i = 0;
        int length = fonts.length;
        while (true) {
            int i2 = i;
            if (i2 < length) {
                Font font = fonts[i2];
                String fullPathName = "/system/fonts/" + font.getFontName();
                ByteBuffer fontBuffer = (ByteBuffer) bufferForPath.get(fullPathName);
                if (fontBuffer == null) {
                    Throwable th3 = null;
                    FileInputStream file = null;
                    try {
                        FileInputStream fileInputStream = new FileInputStream(fullPathName);
                        try {
                            FileChannel fileChannel = fileInputStream.getChannel();
                            fontBuffer = fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size());
                            bufferForPath.put(fullPathName, fontBuffer);
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (Throwable th4) {
                                    th3 = th4;
                                }
                            }
                            if (th3 != null) {
                                try {
                                    throw th3;
                                } catch (IOException e2) {
                                    e = e2;
                                    file = fileInputStream;
                                }
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            file = fileInputStream;
                            th2 = null;
                            if (file != null) {
                                try {
                                    file.close();
                                } catch (Throwable th6) {
                                    if (th2 == null) {
                                        th2 = th6;
                                    } else if (th2 != th6) {
                                        th2.addSuppressed(th6);
                                    }
                                }
                            }
                            if (th2 == null) {
                                throw th;
                            }
                            try {
                                throw th2;
                            } catch (IOException e3) {
                                e = e3;
                                Log.e(TAG, "Error mapping font file " + fullPathName, e);
                                i = i2 + 1;
                            }
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        th2 = null;
                        if (file != null) {
                            try {
                                file.close();
                            } catch (Throwable th62) {
                                if (th2 == null) {
                                    th2 = th62;
                                } else if (th2 != th62) {
                                    th2.addSuppressed(th62);
                                }
                            }
                        }
                        if (th2 == null) {
                            try {
                                throw th2;
                            } catch (IOException e32) {
                                e = e32;
                                Log.e(TAG, "Error mapping font file " + fullPathName, e);
                                i = i2 + 1;
                            }
                        } else {
                            throw th;
                        }
                    }
                }
                if (!fontFamily.addFontFromBuffer(fontBuffer, font.getTtcIndex(), font.getAxes(), font.getWeight(), font.isItalic() ? 1 : 0)) {
                    Log.e(TAG, "Error creating font " + fullPathName + "#" + font.getTtcIndex());
                }
                i = i2 + 1;
            } else if (fontFamily.freeze()) {
                return fontFamily;
            } else {
                Log.e(TAG, "Unable to load Family: " + family.getName() + ":" + family.getLanguage());
                return null;
            }
        }
    }

    @android.annotation.OppoHook(level = android.annotation.OppoHook.OppoHookType.CHANGE_ACCESS, note = "JiFeng.Tan@Plf.SDK, 2015-10-12 : [-private +public] Modify for webview flipfont (from Fei.Wang@Plf.GraphicTech)", property = android.annotation.OppoHook.OppoRomType.ROM)
    public static void init() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r20_1 'newFace' android.graphics.Typeface) in PHI: PHI: (r20_2 'newFace' android.graphics.Typeface) = (r20_1 'newFace' android.graphics.Typeface), (r20_0 'newFace' android.graphics.Typeface) binds: {(r20_1 'newFace' android.graphics.Typeface)=B:50:0x0104, (r20_0 'newFace' android.graphics.Typeface)=B:51:0x0104}
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
        r21 = getSystemFontConfigLocation();
        r7 = new java.io.File;
        r25 = "fonts.xml";
        r0 = r21;
        r1 = r25;
        r7.<init>(r0, r1);
        r18 = new java.io.FileInputStream;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r18;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0.<init>(r7);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r16 = android.graphics.FontListParser.parse(r18);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r6 = new java.util.HashMap;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r6.<init>();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r15 = new java.util.ArrayList;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r15.<init>();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r19 = 0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0027:
        r25 = r16.getFamilies();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r0.length;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = r0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r19;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r1 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        if (r0 >= r1) goto L_0x0050;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0036:
        r25 = r16.getFamilies();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r12 = r25[r19];	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        if (r19 == 0) goto L_0x0044;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x003e:
        r25 = r12.getName();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        if (r25 != 0) goto L_0x004d;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0044:
        r14 = makeFamilyFromParsed(r12, r6);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        if (r14 == 0) goto L_0x004d;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x004a:
        r15.add(r14);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x004d:
        r19 = r19 + 1;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        goto L_0x0027;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0050:
        r25 = r15.size();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = new android.graphics.FontFamily[r0];	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = r0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = r15.toArray(r0);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = (android.graphics.FontFamily[]) r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        sFallbackFonts = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = sFallbackFonts;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = createFromFamilies(r25);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        setDefault(r25);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r22 = new java.util.HashMap;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r22.<init>();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r19 = 0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0074:
        r25 = r16.getFamilies();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r0.length;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = r0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r19;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r1 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        if (r0 >= r1) goto L_0x00c0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0083:
        r25 = r16.getFamilies();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r12 = r25[r19];	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = r12.getName();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        if (r25 == 0) goto L_0x00a0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x008f:
        if (r19 != 0) goto L_0x00a3;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0091:
        r23 = sDefaultTypeface;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0093:
        r25 = r12.getName();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r22;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r1 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r2 = r23;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0.put(r1, r2);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x00a0:
        r19 = r19 + 1;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        goto L_0x0074;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x00a3:
        r17 = makeFamilyFromParsed(r12, r6);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        if (r17 == 0) goto L_0x00a0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x00a9:
        r25 = 1;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r13 = new android.graphics.FontFamily[r0];	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = 0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r13[r25] = r17;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = -1;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r26 = -1;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r1 = r26;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r23 = createFromFamiliesWithDefault(r13, r0, r1);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        goto L_0x0093;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x00c0:
        r26 = r16.getAliases();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = 0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r26;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r0.length;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r27 = r0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x00cb:
        r0 = r25;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r1 = r27;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        if (r0 >= r1) goto L_0x0114;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x00d1:
        r4 = r26[r25];	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r28 = r4.getToName();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r22;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r1 = r28;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r5 = r0.get(r1);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r5 = (android.graphics.Typeface) r5;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r20 = r5;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r24 = r4.getWeight();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r28 = 400; // 0x190 float:5.6E-43 double:1.976E-321;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r1 = r28;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        if (r0 == r1) goto L_0x0104;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x00ef:
        r20 = new android.graphics.Typeface;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r5.native_instance;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r28 = r0;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r28;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r2 = r24;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r28 = nativeCreateWeightAlias(r0, r2);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r20;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r1 = r28;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0.<init>(r1);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0104:
        r28 = r4.getName();	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0 = r22;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r1 = r28;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r2 = r20;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r0.put(r1, r2);	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        r25 = r25 + 1;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
        goto L_0x00cb;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0114:
        sSystemFontMap = r22;	 Catch:{ RuntimeException -> 0x017a, FileNotFoundException -> 0x0159, IOException -> 0x0138, XmlPullParserException -> 0x0117 }
    L_0x0116:
        return;
    L_0x0117:
        r11 = move-exception;
        r25 = TAG;
        r26 = new java.lang.StringBuilder;
        r26.<init>();
        r27 = "XML parse exception for ";
        r26 = r26.append(r27);
        r0 = r26;
        r26 = r0.append(r7);
        r26 = r26.toString();
        r0 = r25;
        r1 = r26;
        android.util.Log.e(r0, r1, r11);
        goto L_0x0116;
    L_0x0138:
        r9 = move-exception;
        r25 = TAG;
        r26 = new java.lang.StringBuilder;
        r26.<init>();
        r27 = "Error reading ";
        r26 = r26.append(r27);
        r0 = r26;
        r26 = r0.append(r7);
        r26 = r26.toString();
        r0 = r25;
        r1 = r26;
        android.util.Log.e(r0, r1, r9);
        goto L_0x0116;
    L_0x0159:
        r8 = move-exception;
        r25 = TAG;
        r26 = new java.lang.StringBuilder;
        r26.<init>();
        r27 = "Error opening ";
        r26 = r26.append(r27);
        r0 = r26;
        r26 = r0.append(r7);
        r26 = r26.toString();
        r0 = r25;
        r1 = r26;
        android.util.Log.e(r0, r1, r8);
        goto L_0x0116;
    L_0x017a:
        r10 = move-exception;
        r25 = TAG;
        r26 = "Didn't create default family (most likely, non-Minikin build)";
        r0 = r25;
        r1 = r26;
        android.util.Log.w(r0, r1, r10);
        goto L_0x0116;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.graphics.Typeface.init():void");
    }

    private static File getSystemFontConfigLocation() {
        return new File("/system/etc/");
    }

    protected void finalize() throws Throwable {
        try {
            nativeUnref(this.native_instance);
            this.native_instance = 0;
        } finally {
            super.finalize();
        }
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Typeface typeface = (Typeface) o;
        if (!(this.mStyle == typeface.mStyle && this.native_instance == typeface.native_instance)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return ((((int) (this.native_instance ^ (this.native_instance >>> 32))) + 527) * 31) + this.mStyle;
    }

    public boolean isSupportedAxes(int axis) {
        if (this.mSupportedAxes == null) {
            synchronized (this) {
                if (this.mSupportedAxes == null) {
                    this.mSupportedAxes = nativeGetSupportedAxes(this.native_instance);
                    if (this.mSupportedAxes == null) {
                        this.mSupportedAxes = EMPTY_AXES;
                    }
                }
            }
        }
        if (Arrays.binarySearch(this.mSupportedAxes, axis) >= 0) {
            return true;
        }
        return false;
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "JiFeng.Tan@Plf.SDK, 2016-01-06 : Aprestrain ColorOS Fonts (from Jianjun.Dan@Plf.GraphicTech)", property = OppoRomType.ROM)
    private static Typeface createOsFontFromFile(String path) {
        if (sFallbackFonts != null) {
            if (new FontFamily().addFont(path, 0, null, -1, -1)) {
                return createFromFamiliesWithDefault(new FontFamily[]{new FontFamily()}, -1, -1);
            }
        }
        throw new RuntimeException("Font not found " + path);
    }
}
