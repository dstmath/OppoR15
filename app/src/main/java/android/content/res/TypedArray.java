package android.content.res;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.content.pm.ActivityInfo;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import com.android.internal.util.XmlUtils;
import dalvik.system.VMRuntime;
import java.util.Arrays;

public class TypedArray {
    private AssetManager mAssets;
    int[] mData;
    long mDataAddress;
    int[] mIndices;
    long mIndicesAddress;
    int mLength;
    private DisplayMetrics mMetrics;
    private boolean mRecycled;
    private final Resources mResources;
    Theme mTheme;
    TypedValue mValue = new TypedValue();
    Parser mXml;

    static TypedArray obtain(Resources res, int len) {
        TypedArray attrs = (TypedArray) res.mTypedArrayPool.acquire();
        if (attrs == null) {
            attrs = new TypedArray(res);
        }
        attrs.mRecycled = false;
        attrs.mAssets = res.getAssets();
        attrs.mMetrics = res.getDisplayMetrics();
        attrs.resize(len);
        return attrs;
    }

    private void resize(int len) {
        this.mLength = len;
        int dataLen = len * 6;
        int indicesLen = len + 1;
        VMRuntime runtime = VMRuntime.getRuntime();
        if (this.mDataAddress == 0 || this.mData.length < dataLen) {
            this.mData = (int[]) runtime.newNonMovableArray(Integer.TYPE, dataLen);
            this.mDataAddress = runtime.addressOf(this.mData);
            this.mIndices = (int[]) runtime.newNonMovableArray(Integer.TYPE, indicesLen);
            this.mIndicesAddress = runtime.addressOf(this.mIndices);
        }
    }

    public int length() {
        if (!this.mRecycled) {
            return this.mLength;
        }
        throw new RuntimeException("Cannot make calls to a recycled instance!");
    }

    public int getIndexCount() {
        if (!this.mRecycled) {
            return this.mIndices[0];
        }
        throw new RuntimeException("Cannot make calls to a recycled instance!");
    }

    public int getIndex(int at) {
        if (!this.mRecycled) {
            return this.mIndices[at + 1];
        }
        throw new RuntimeException("Cannot make calls to a recycled instance!");
    }

    public Resources getResources() {
        if (!this.mRecycled) {
            return this.mResources;
        }
        throw new RuntimeException("Cannot make calls to a recycled instance!");
    }

    public CharSequence getText(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int type = this.mData[index + 0];
        if (type == 0) {
            return null;
        }
        if (type == 3) {
            return loadStringValueAt(index);
        }
        TypedValue v = this.mValue;
        if (getValueAt(index, v)) {
            return v.coerceToString();
        }
        throw new RuntimeException("getText of bad type: 0x" + Integer.toHexString(type));
    }

    public String getString(int index) {
        String str = null;
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int type = this.mData[index + 0];
        if (type == 0) {
            return null;
        }
        if (type == 3) {
            return loadStringValueAt(index).toString();
        }
        TypedValue v = this.mValue;
        if (getValueAt(index, v)) {
            CharSequence cs = v.coerceToString();
            if (cs != null) {
                str = cs.toString();
            }
            return str;
        }
        throw new RuntimeException("getString of bad type: 0x" + Integer.toHexString(type));
    }

    public String getNonResourceString(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int[] data = this.mData;
        if (data[index + 0] != 3 || data[index + 2] >= 0) {
            return null;
        }
        return this.mXml.getPooledString(data[index + 1]).toString();
    }

    public String getNonConfigurationString(int index, int allowedChangingConfigs) {
        String str = null;
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (((~allowedChangingConfigs) & ActivityInfo.activityInfoConfigNativeToJava(data[index + 4])) != 0 || type == 0) {
            return null;
        }
        if (type == 3) {
            return loadStringValueAt(index).toString();
        }
        TypedValue v = this.mValue;
        if (getValueAt(index, v)) {
            CharSequence cs = v.coerceToString();
            if (cs != null) {
                str = cs.toString();
            }
            return str;
        }
        throw new RuntimeException("getNonConfigurationString of bad type: 0x" + Integer.toHexString(type));
    }

    public boolean getBoolean(int index, boolean defValue) {
        boolean z = false;
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return defValue;
        }
        if (type < 16 || type > 31) {
            TypedValue v = this.mValue;
            if (getValueAt(index, v)) {
                StrictMode.noteResourceMismatch(v);
                return XmlUtils.convertValueToBoolean(v.coerceToString(), defValue);
            }
            throw new RuntimeException("getBoolean of bad type: 0x" + Integer.toHexString(type));
        }
        if (data[index + 1] != 0) {
            z = true;
        }
        return z;
    }

    public int getInt(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return defValue;
        }
        if (type >= 16 && type <= 31) {
            return data[index + 1];
        }
        TypedValue v = this.mValue;
        if (getValueAt(index, v)) {
            StrictMode.noteResourceMismatch(v);
            return XmlUtils.convertValueToInt(v.coerceToString(), defValue);
        }
        throw new RuntimeException("getInt of bad type: 0x" + Integer.toHexString(type));
    }

    public float getFloat(int index, float defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 4) {
            return Float.intBitsToFloat(data[index + 1]);
        }
        if (type >= 16 && type <= 31) {
            return (float) data[index + 1];
        }
        TypedValue v = this.mValue;
        if (getValueAt(index, v)) {
            CharSequence str = v.coerceToString();
            if (str != null) {
                StrictMode.noteResourceMismatch(v);
                return Float.parseFloat(str.toString());
            }
        }
        throw new RuntimeException("getFloat of bad type: 0x" + Integer.toHexString(type));
    }

    public int getColor(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int attrIndex = index;
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return defValue;
        }
        if (type >= 16 && type <= 31) {
            return data[index + 1];
        }
        TypedValue value;
        if (type == 3) {
            value = this.mValue;
            if (getValueAt(index, value)) {
                return this.mResources.loadColorStateList(value, value.resourceId, this.mTheme).getDefaultColor();
            }
            return defValue;
        } else if (type == 2) {
            value = this.mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + attrIndex + ": " + value);
        } else {
            throw new UnsupportedOperationException("Can't convert value at index " + attrIndex + " to color: type=0x" + Integer.toHexString(type));
        }
    }

    public ComplexColor getComplexColor(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (!getValueAt(index * 6, value)) {
            return null;
        }
        if (value.type != 2) {
            return this.mResources.loadComplexColor(value, value.resourceId, this.mTheme);
        }
        throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
    }

    public ColorStateList getColorStateList(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (!getValueAt(index * 6, value)) {
            return null;
        }
        if (value.type != 2) {
            return this.mResources.loadColorStateList(value, value.resourceId, this.mTheme);
        }
        throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
    }

    public int getInteger(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int attrIndex = index;
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return defValue;
        }
        if (type >= 16 && type <= 31) {
            return data[index + 1];
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + attrIndex + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex + " to integer: type=0x" + Integer.toHexString(type));
    }

    public float getDimension(int index, float defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int attrIndex = index;
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 5) {
            return TypedValue.complexToDimension(data[index + 1], this.mMetrics);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + attrIndex + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex + " to dimension: type=0x" + Integer.toHexString(type));
    }

    public int getDimensionPixelOffset(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int attrIndex = index;
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 5) {
            return TypedValue.complexToDimensionPixelOffset(data[index + 1], this.mMetrics);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + attrIndex + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex + " to dimension: type=0x" + Integer.toHexString(type));
    }

    public int getDimensionPixelSize(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int attrIndex = index;
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 5) {
            return TypedValue.complexToDimensionPixelSize(data[index + 1], this.mMetrics);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + attrIndex + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex + " to dimension: type=0x" + Integer.toHexString(type));
    }

    public int getLayoutDimension(int index, String name) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int attrIndex = index;
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type >= 16 && type <= 31) {
            return data[index + 1];
        }
        if (type == 5) {
            return TypedValue.complexToDimensionPixelSize(data[index + 1], this.mMetrics);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + attrIndex + ": " + value);
        }
        throw new UnsupportedOperationException(getPositionDescription() + ": You must supply a " + name + " attribute.");
    }

    public int getLayoutDimension(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type >= 16 && type <= 31) {
            return data[index + 1];
        }
        if (type == 5) {
            return TypedValue.complexToDimensionPixelSize(data[index + 1], this.mMetrics);
        }
        return defValue;
    }

    public float getFraction(int index, int base, int pbase, float defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int attrIndex = index;
        index *= 6;
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 6) {
            return TypedValue.complexToFraction(data[index + 1], (float) base, (float) pbase);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + attrIndex + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex + " to fraction: type=0x" + Integer.toHexString(type));
    }

    public int getResourceId(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int[] data = this.mData;
        if (data[index + 0] != 0) {
            int resid = data[index + 3];
            if (resid != 0) {
                return resid;
            }
        }
        return defValue;
    }

    public int getThemeAttributeId(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int[] data = this.mData;
        if (data[index + 0] == 2) {
            return data[index + 1];
        }
        return defValue;
    }

    public Drawable getDrawable(int index) {
        return getDrawableForDensity(index, 0);
    }

    public Drawable getDrawableForDensity(int index, int density) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (!getValueAt(index * 6, value)) {
            return null;
        }
        if (value.type == 2) {
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
        }
        if (density > 0) {
            this.mResources.getValueForDensity(value.resourceId, density, value, true);
        }
        return this.mResources.loadDrawable(value, value.resourceId, density, this.mTheme);
    }

    public Typeface getFont(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (!getValueAt(index * 6, value)) {
            return null;
        }
        if (value.type != 2) {
            return this.mResources.getFont(value, value.resourceId);
        }
        throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
    }

    public CharSequence[] getTextArray(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (getValueAt(index * 6, value)) {
            return this.mResources.getTextArray(value.resourceId);
        }
        return null;
    }

    public boolean getValue(int index, TypedValue outValue) {
        if (!this.mRecycled) {
            return getValueAt(index * 6, outValue);
        }
        throw new RuntimeException("Cannot make calls to a recycled instance!");
    }

    public int getType(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mData[(index * 6) + 0];
    }

    public boolean hasValue(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        if (this.mData[(index * 6) + 0] != 0) {
            return true;
        }
        return false;
    }

    public boolean hasValueOrEmpty(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        index *= 6;
        int[] data = this.mData;
        if (data[index + 0] != 0 || data[index + 1] == 1) {
            return true;
        }
        return false;
    }

    public TypedValue peekValue(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (getValueAt(index * 6, value)) {
            return value;
        }
        return null;
    }

    public String getPositionDescription() {
        if (!this.mRecycled) {
            return this.mXml != null ? this.mXml.getPositionDescription() : "<internal>";
        } else {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
    }

    public void recycle() {
        if (this.mRecycled) {
            throw new RuntimeException(toString() + " recycled twice!");
        }
        this.mRecycled = true;
        this.mXml = null;
        this.mTheme = null;
        this.mAssets = null;
        this.mResources.mTypedArrayPool.release(this);
    }

    public int[] extractThemeAttrs() {
        return extractThemeAttrs(null);
    }

    public int[] extractThemeAttrs(int[] scrap) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int[] attrs = null;
        int[] data = this.mData;
        int N = length();
        for (int i = 0; i < N; i++) {
            int index = i * 6;
            if (data[index + 0] == 2) {
                data[index + 0] = 0;
                int attr = data[index + 1];
                if (attr != 0) {
                    if (attrs == null) {
                        if (scrap == null || scrap.length != N) {
                            attrs = new int[N];
                        } else {
                            attrs = scrap;
                            Arrays.fill(scrap, 0);
                        }
                    }
                    attrs[i] = attr;
                }
            }
        }
        return attrs;
    }

    public int getChangingConfigurations() {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int changingConfig = 0;
        int[] data = this.mData;
        int N = length();
        for (int i = 0; i < N; i++) {
            int index = i * 6;
            if (data[index + 0] != 0) {
                changingConfig |= ActivityInfo.activityInfoConfigNativeToJava(data[index + 4]);
            }
        }
        return changingConfig;
    }

    private boolean getValueAt(int index, TypedValue outValue) {
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return false;
        }
        outValue.type = type;
        outValue.data = data[index + 1];
        outValue.assetCookie = data[index + 2];
        outValue.resourceId = data[index + 3];
        outValue.changingConfigurations = ActivityInfo.activityInfoConfigNativeToJava(data[index + 4]);
        outValue.density = data[index + 5];
        outValue.string = type == 3 ? loadStringValueAt(index) : null;
        return true;
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Suying.You@Plf.SDK : Modify for rom theme the packagename of the third party, and decompress the zipFile of data/string ", property = OppoRomType.ROM)
    private CharSequence loadStringValueAt(int index) {
        int[] data = this.mData;
        int cookie = data[index + 2];
        if (cookie >= 0) {
            CharSequence themeCharSequence = null;
            if (this.mResources != null) {
                themeCharSequence = this.mResources.getThemeCharSequence(data[index + 3]);
            }
            if (themeCharSequence != null) {
                return themeCharSequence;
            }
            return this.mAssets.getPooledStringForCookie(cookie, data[index + 1]);
        } else if (this.mXml != null) {
            return this.mXml.getPooledString(data[index + 1]);
        } else {
            return null;
        }
    }

    protected TypedArray(Resources resources) {
        this.mResources = resources;
        this.mMetrics = this.mResources.getDisplayMetrics();
        this.mAssets = this.mResources.getAssets();
    }

    public String toString() {
        return Arrays.toString(this.mData);
    }
}
