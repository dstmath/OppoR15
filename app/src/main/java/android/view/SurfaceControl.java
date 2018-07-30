package android.view;

import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.view.Display.HdrCapabilities;
import android.view.Surface.OutOfResourcesException;
import dalvik.system.CloseGuard;

public class SurfaceControl {
    public static final int BUILT_IN_DISPLAY_ID_HDMI = 1;
    public static final int BUILT_IN_DISPLAY_ID_MAIN = 0;
    public static final int CURSOR_WINDOW = 8192;
    private static final boolean DEBUG = true;
    private static boolean DEBUG_SFC = SystemProperties.getBoolean("persist.sys.assert.panic", false);
    public static final int FX_SURFACE_DIM = 131072;
    public static final int FX_SURFACE_MASK = 983040;
    public static final int FX_SURFACE_NORMAL = 0;
    public static final int HIDDEN = 4;
    public static final int NON_PREMULTIPLIED = 256;
    public static final int OPAQUE = 1024;
    private static final String PERSIST_KEY_METRICS = "persist.sys.oppo.displaymetrics";
    public static final int POWER_MODE_DOZE = 1;
    public static final int POWER_MODE_DOZE_SUSPEND = 3;
    public static final int POWER_MODE_NORMAL = 2;
    public static final int POWER_MODE_OFF = 0;
    public static final int PROTECTED_APP = 2048;
    public static final int SECURE = 128;
    private static String SPLIT_PROP = ",";
    private static final int SURFACE_HIDDEN = 1;
    private static final int SURFACE_OPAQUE = 2;
    private static final String TAG = "SurfaceControl";
    public static final int WINDOW_TYPE_DONT_SCREENSHOT = 441731;
    private final CloseGuard mCloseGuard;
    private final String mName;
    long mNativeObject;

    public static final class PhysicalDisplayInfo {
        public long appVsyncOffsetNanos;
        public float density;
        public int height;
        public long presentationDeadlineNanos;
        public float refreshRate;
        public boolean secure;
        public int width;
        public float xDpi;
        public float yDpi;

        public PhysicalDisplayInfo(PhysicalDisplayInfo other) {
            copyFrom(other);
        }

        public boolean equals(Object o) {
            return o instanceof PhysicalDisplayInfo ? equals((PhysicalDisplayInfo) o) : false;
        }

        public boolean equals(PhysicalDisplayInfo other) {
            return other != null && this.width == other.width && this.height == other.height && this.refreshRate == other.refreshRate && this.density == other.density && this.xDpi == other.xDpi && this.yDpi == other.yDpi && this.secure == other.secure && this.appVsyncOffsetNanos == other.appVsyncOffsetNanos && this.presentationDeadlineNanos == other.presentationDeadlineNanos;
        }

        public int hashCode() {
            return 0;
        }

        public void copyFrom(PhysicalDisplayInfo other) {
            this.width = other.width;
            this.height = other.height;
            this.refreshRate = other.refreshRate;
            this.density = other.density;
            this.xDpi = other.xDpi;
            this.yDpi = other.yDpi;
            this.secure = other.secure;
            this.appVsyncOffsetNanos = other.appVsyncOffsetNanos;
            this.presentationDeadlineNanos = other.presentationDeadlineNanos;
        }

        public String toString() {
            return "PhysicalDisplayInfo{" + this.width + " x " + this.height + ", " + this.refreshRate + " fps, " + "density " + this.density + ", " + this.xDpi + " x " + this.yDpi + " dpi, secure " + this.secure + ", appVsyncOffset " + this.appVsyncOffsetNanos + ", bufferDeadline " + this.presentationDeadlineNanos + "}";
        }
    }

    private static native boolean nativeClearAnimationFrameStats();

    private static native boolean nativeClearContentFrameStats(long j);

    private static native void nativeCloseTransaction(boolean z);

    private static native long nativeCreate(SurfaceSession surfaceSession, String str, int i, int i2, int i3, int i4, long j, int i5, int i6) throws OutOfResourcesException;

    private static native IBinder nativeCreateDisplay(String str, boolean z);

    private static native void nativeDeferTransactionUntil(long j, IBinder iBinder, long j2);

    private static native void nativeDeferTransactionUntilSurface(long j, long j2, long j3);

    private static native void nativeDestroy(long j);

    private static native void nativeDestroyDisplay(IBinder iBinder);

    private static native void nativeDisconnect(long j);

    private static native int nativeGetActiveColorMode(IBinder iBinder);

    private static native int nativeGetActiveConfig(IBinder iBinder);

    private static native boolean nativeGetAnimationFrameStats(WindowAnimationFrameStats windowAnimationFrameStats);

    private static native IBinder nativeGetBuiltInDisplay(int i);

    private static native boolean nativeGetContentFrameStats(long j, WindowContentFrameStats windowContentFrameStats);

    private static native int[] nativeGetDisplayColorModes(IBinder iBinder);

    private static native PhysicalDisplayInfo[] nativeGetDisplayConfigs(IBinder iBinder);

    private static native IBinder nativeGetHandle(long j);

    private static native HdrCapabilities nativeGetHdrCapabilities(IBinder iBinder);

    private static native boolean nativeGetTransformToDisplayInverse(long j);

    private static native void nativeOpenTransaction();

    private static native void nativeRelease(long j);

    private static native void nativeReparentChildren(long j, IBinder iBinder);

    private static native Bitmap nativeScreenshot(IBinder iBinder, Rect rect, int i, int i2, int i3, int i4, boolean z, boolean z2, int i5);

    private static native void nativeScreenshot(IBinder iBinder, Surface surface, Rect rect, int i, int i2, int i3, int i4, boolean z, boolean z2);

    private static native GraphicBuffer nativeScreenshotToBuffer(IBinder iBinder, Rect rect, int i, int i2, int i3, int i4, boolean z, boolean z2, int i5);

    private static native boolean nativeSetActiveColorMode(IBinder iBinder, int i);

    private static native boolean nativeSetActiveConfig(IBinder iBinder, int i);

    private static native void nativeSetAlpha(long j, float f);

    private static native void nativeSetAnimationTransaction();

    private static native void nativeSetDisplayLayerStack(IBinder iBinder, int i);

    private static native void nativeSetDisplayPowerMode(IBinder iBinder, int i);

    private static native void nativeSetDisplayProjection(IBinder iBinder, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9);

    private static native void nativeSetDisplaySize(IBinder iBinder, int i, int i2);

    private static native void nativeSetDisplaySurface(IBinder iBinder, long j);

    private static native void nativeSetFinalCrop(long j, int i, int i2, int i3, int i4);

    private static native void nativeSetFlags(long j, int i, int i2);

    private static native void nativeSetGeometryAppliesWithResize(long j);

    private static native void nativeSetLayer(long j, int i);

    private static native void nativeSetLayerStack(long j, int i);

    private static native void nativeSetMatrix(long j, float f, float f2, float f3, float f4);

    private static native void nativeSetOverrideScalingMode(long j, int i);

    private static native void nativeSetPosition(long j, float f, float f2);

    private static native void nativeSetRelativeLayer(long j, IBinder iBinder, int i);

    private static native void nativeSetSize(long j, int i, int i2);

    private static native void nativeSetTransparentRegionHint(long j, Region region);

    private static native void nativeSetWindowCrop(long j, int i, int i2, int i3, int i4);

    private static native void nativeSeverChildren(long j);

    public SurfaceControl(SurfaceSession session, String name, int w, int h, int format, int flags, int windowType, int ownerUid) throws OutOfResourcesException {
        this(session, name, w, h, format, flags, null, windowType, ownerUid);
    }

    public SurfaceControl(SurfaceSession session, String name, int w, int h, int format, int flags) throws OutOfResourcesException {
        this(session, name, w, h, format, flags, null, -1, Binder.getCallingUid());
    }

    public SurfaceControl(SurfaceSession session, String name, int w, int h, int format, int flags, SurfaceControl parent, int windowType, int ownerUid) throws OutOfResourcesException {
        this.mCloseGuard = CloseGuard.get();
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        } else if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        } else {
            if ((flags & 4) == 0) {
                Log.w(TAG, "Surfaces should always be created with the HIDDEN flag set to ensure that they are not made visible prematurely before all of the surface's properties have been configured.  Set the other properties and make the surface visible within a transaction.  New surface name: " + name, new Throwable());
            }
            this.mName = name;
            this.mNativeObject = nativeCreate(session, name, w, h, format, flags, parent != null ? parent.mNativeObject : 0, windowType, ownerUid);
            if (this.mNativeObject == 0) {
                throw new OutOfResourcesException("Couldn't allocate SurfaceControl native object");
            }
            this.mCloseGuard.open("release");
        }
    }

    public SurfaceControl(SurfaceControl other) {
        this.mCloseGuard = CloseGuard.get();
        this.mName = other.mName;
        this.mNativeObject = other.mNativeObject;
        other.mCloseGuard.close();
        other.mNativeObject = 0;
        this.mCloseGuard.open("release");
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            if (this.mNativeObject != 0) {
                nativeRelease(this.mNativeObject);
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public String toString() {
        return "Surface(name=" + this.mName + ")";
    }

    public void release() {
        if (this.mNativeObject != 0) {
            nativeRelease(this.mNativeObject);
            this.mNativeObject = 0;
        }
        this.mCloseGuard.close();
    }

    public void destroy() {
        if (DEBUG_SFC) {
            Slog.v(TAG, "SurfaceControl destroy: " + this);
        }
        if (this.mNativeObject != 0) {
            nativeDestroy(this.mNativeObject);
            this.mNativeObject = 0;
        }
        this.mCloseGuard.close();
    }

    public void disconnect() {
        if (this.mNativeObject != 0) {
            nativeDisconnect(this.mNativeObject);
        }
    }

    private void checkNotReleased() {
        if (this.mNativeObject == 0) {
            throw new NullPointerException("mNativeObject is null. Have you called release() already?");
        }
    }

    public static void openTransaction() {
        nativeOpenTransaction();
    }

    public static void closeTransaction() {
        nativeCloseTransaction(false);
    }

    public static void closeTransactionSync() {
        nativeCloseTransaction(true);
    }

    public void deferTransactionUntil(IBinder handle, long frame) {
        if (frame > 0) {
            nativeDeferTransactionUntil(this.mNativeObject, handle, frame);
        }
    }

    public void deferTransactionUntil(Surface barrier, long frame) {
        if (frame > 0) {
            nativeDeferTransactionUntilSurface(this.mNativeObject, barrier.mNativeObject, frame);
        }
    }

    public void reparentChildren(IBinder newParentHandle) {
        nativeReparentChildren(this.mNativeObject, newParentHandle);
    }

    public void detachChildren() {
        if (this.mName != null && this.mName.contains("com.oppo.camera")) {
            if (DEBUG_SFC) {
                Slog.d(TAG, "Not reparentChildren, return " + this);
            }
        } else if (this.mName == null || !this.mName.contains("com.android.wallpaper.livepicker")) {
            nativeSeverChildren(this.mNativeObject);
        } else {
            if (DEBUG_SFC) {
                Slog.d(TAG, "detachChildren, return " + this);
            }
        }
    }

    public void setOverrideScalingMode(int scalingMode) {
        checkNotReleased();
        nativeSetOverrideScalingMode(this.mNativeObject, scalingMode);
    }

    public IBinder getHandle() {
        return nativeGetHandle(this.mNativeObject);
    }

    public static void setAnimationTransaction() {
        nativeSetAnimationTransaction();
    }

    public void setLayer(int zorder) {
        checkNotReleased();
        nativeSetLayer(this.mNativeObject, zorder);
    }

    public void setRelativeLayer(IBinder relativeTo, int zorder) {
        checkNotReleased();
        nativeSetRelativeLayer(this.mNativeObject, relativeTo, zorder);
    }

    public void setPosition(float x, float y) {
        checkNotReleased();
        nativeSetPosition(this.mNativeObject, x, y);
    }

    public void setGeometryAppliesWithResize() {
        checkNotReleased();
        nativeSetGeometryAppliesWithResize(this.mNativeObject);
    }

    public void setSize(int w, int h) {
        checkNotReleased();
        nativeSetSize(this.mNativeObject, w, h);
    }

    public void hide() {
        if (DEBUG_SFC) {
            Slog.v(TAG, "SurfaceControl hide: " + this);
        }
        checkNotReleased();
        nativeSetFlags(this.mNativeObject, 1, 1);
    }

    public void show() {
        if (DEBUG_SFC) {
            Slog.v(TAG, "SurfaceControl show : " + this);
        }
        checkNotReleased();
        nativeSetFlags(this.mNativeObject, 0, 1);
    }

    public void setTransparentRegionHint(Region region) {
        checkNotReleased();
        nativeSetTransparentRegionHint(this.mNativeObject, region);
    }

    public boolean clearContentFrameStats() {
        checkNotReleased();
        return nativeClearContentFrameStats(this.mNativeObject);
    }

    public boolean getContentFrameStats(WindowContentFrameStats outStats) {
        checkNotReleased();
        return nativeGetContentFrameStats(this.mNativeObject, outStats);
    }

    public static boolean clearAnimationFrameStats() {
        return nativeClearAnimationFrameStats();
    }

    public static boolean getAnimationFrameStats(WindowAnimationFrameStats outStats) {
        return nativeGetAnimationFrameStats(outStats);
    }

    public void setAlpha(float alpha) {
        checkNotReleased();
        nativeSetAlpha(this.mNativeObject, alpha);
    }

    public void setMatrix(float dsdx, float dtdx, float dtdy, float dsdy) {
        checkNotReleased();
        nativeSetMatrix(this.mNativeObject, dsdx, dtdx, dtdy, dsdy);
    }

    public void setWindowCrop(Rect crop) {
        checkNotReleased();
        if (crop != null) {
            nativeSetWindowCrop(this.mNativeObject, crop.left, crop.top, crop.right, crop.bottom);
        } else {
            nativeSetWindowCrop(this.mNativeObject, 0, 0, 0, 0);
        }
    }

    public void setFinalCrop(Rect crop) {
        checkNotReleased();
        if (crop != null) {
            nativeSetFinalCrop(this.mNativeObject, crop.left, crop.top, crop.right, crop.bottom);
        } else {
            nativeSetFinalCrop(this.mNativeObject, 0, 0, 0, 0);
        }
    }

    public void setLayerStack(int layerStack) {
        checkNotReleased();
        nativeSetLayerStack(this.mNativeObject, layerStack);
    }

    public void setOpaque(boolean isOpaque) {
        checkNotReleased();
        if (isOpaque) {
            nativeSetFlags(this.mNativeObject, 2, 2);
        } else {
            nativeSetFlags(this.mNativeObject, 0, 2);
        }
    }

    public void setSecure(boolean isSecure) {
        checkNotReleased();
        if (isSecure) {
            nativeSetFlags(this.mNativeObject, 128, 128);
        } else {
            nativeSetFlags(this.mNativeObject, 0, 128);
        }
    }

    public static void setDisplayPowerMode(IBinder displayToken, int mode) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeSetDisplayPowerMode(displayToken, mode);
    }

    public static PhysicalDisplayInfo[] getDisplayConfigs(IBinder displayToken) {
        if (displayToken != null) {
            return nativeGetDisplayConfigs(displayToken);
        }
        throw new IllegalArgumentException("displayToken must not be null");
    }

    public static int getActiveConfig(IBinder displayToken) {
        if (displayToken != null) {
            return nativeGetActiveConfig(displayToken);
        }
        throw new IllegalArgumentException("displayToken must not be null");
    }

    public static boolean setActiveConfig(IBinder displayToken, int id) {
        if (displayToken != null) {
            return nativeSetActiveConfig(displayToken, id);
        }
        throw new IllegalArgumentException("displayToken must not be null");
    }

    public static int[] getDisplayColorModes(IBinder displayToken) {
        if (displayToken != null) {
            return nativeGetDisplayColorModes(displayToken);
        }
        throw new IllegalArgumentException("displayToken must not be null");
    }

    public static int getActiveColorMode(IBinder displayToken) {
        if (displayToken != null) {
            return nativeGetActiveColorMode(displayToken);
        }
        throw new IllegalArgumentException("displayToken must not be null");
    }

    public static boolean setActiveColorMode(IBinder displayToken, int colorMode) {
        if (displayToken != null) {
            return nativeSetActiveColorMode(displayToken, colorMode);
        }
        throw new IllegalArgumentException("displayToken must not be null");
    }

    public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        } else if (layerStackRect == null) {
            throw new IllegalArgumentException("layerStackRect must not be null");
        } else if (displayRect == null) {
            throw new IllegalArgumentException("displayRect must not be null");
        } else {
            nativeSetDisplayProjection(displayToken, orientation, layerStackRect.left, layerStackRect.top, layerStackRect.right, layerStackRect.bottom, displayRect.left, displayRect.top, displayRect.right, displayRect.bottom);
        }
    }

    public static void setDisplayLayerStack(IBinder displayToken, int layerStack) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeSetDisplayLayerStack(displayToken, layerStack);
    }

    public static void setDisplaySurface(IBinder displayToken, Surface surface) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        } else if (surface != null) {
            synchronized (surface.mLock) {
                nativeSetDisplaySurface(displayToken, surface.mNativeObject);
            }
        } else {
            nativeSetDisplaySurface(displayToken, 0);
        }
    }

    public static void setDisplaySize(IBinder displayToken, int width, int height) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        } else if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        } else {
            nativeSetDisplaySize(displayToken, width, height);
        }
    }

    public static HdrCapabilities getHdrCapabilities(IBinder displayToken) {
        if (displayToken != null) {
            return nativeGetHdrCapabilities(displayToken);
        }
        throw new IllegalArgumentException("displayToken must not be null");
    }

    public static IBinder createDisplay(String name, boolean secure) {
        if (name != null) {
            return nativeCreateDisplay(name, secure);
        }
        throw new IllegalArgumentException("name must not be null");
    }

    public static void destroyDisplay(IBinder displayToken) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeDestroyDisplay(displayToken);
    }

    public static IBinder getBuiltInDisplay(int builtInDisplayId) {
        return nativeGetBuiltInDisplay(builtInDisplayId);
    }

    public static void screenshot(IBinder display, Surface consumer, int width, int height, int minLayer, int maxLayer, boolean useIdentityTransform) {
        screenshot(display, consumer, new Rect(), width, height, minLayer, maxLayer, false, useIdentityTransform);
    }

    public static void screenshot(IBinder display, Surface consumer, int width, int height) {
        screenshot(display, consumer, new Rect(), width, height, 0, 0, true, false);
    }

    public static void screenshot(IBinder display, Surface consumer) {
        screenshot(display, consumer, new Rect(), 0, 0, 0, 0, true, false);
    }

    public static Bitmap screenshot(Rect sourceCrop, int width, int height, int minLayer, int maxLayer, boolean useIdentityTransform, int rotation) {
        IBinder displayToken = getBuiltInDisplay(0);
        if (OppoScreenDragUtil.isOffsetState()) {
            float scale = OppoScreenDragUtil.getScale();
            String[] strProps = SystemProperties.get(PERSIST_KEY_METRICS, "1080,1920").split(SPLIT_PROP);
            int w = Integer.parseInt(strProps[0]);
            int h = Integer.parseInt(strProps[1]);
            int sw = sourceCrop.width();
            int sh = sourceCrop.height();
            if (OppoScreenDragUtil.getOffsetX() > 0) {
                sourceCrop.left = (int) (((1.0f - scale) * ((float) w)) + (((float) sourceCrop.left) * scale));
                sourceCrop.top = (int) (((1.0f - scale) * ((float) h)) + (((float) sourceCrop.top) * scale));
                sourceCrop.right = sourceCrop.left + ((int) (((float) sw) * scale));
                sourceCrop.bottom = sourceCrop.top + ((int) (((float) sh) * scale));
            } else {
                sourceCrop.left = (int) (((float) sourceCrop.left) * scale);
                sourceCrop.top = (int) (((1.0f - scale) * ((float) h)) + (((float) sourceCrop.top) * scale));
                sourceCrop.right = sourceCrop.left + ((int) (((float) sw) * scale));
                sourceCrop.bottom = sourceCrop.top + ((int) (((float) sh) * scale));
            }
        }
        return nativeScreenshot(displayToken, sourceCrop, width, height, minLayer, maxLayer, false, useIdentityTransform, rotation);
    }

    public static GraphicBuffer screenshotToBuffer(Rect sourceCrop, int width, int height, int minLayer, int maxLayer, boolean useIdentityTransform, int rotation) {
        return nativeScreenshotToBuffer(getBuiltInDisplay(0), sourceCrop, width, height, minLayer, maxLayer, false, useIdentityTransform, rotation);
    }

    public static Bitmap screenshot(int width, int height) {
        Bitmap tmpBitmap = nativeScreenshot(getBuiltInDisplay(0), new Rect(), width, height, 0, 0, true, false, 0);
        if (tmpBitmap == null) {
            return null;
        }
        return isOppoScreenDragMode(tmpBitmap, width, height);
    }

    private static Bitmap isOppoScreenDragMode(Bitmap bitmap, int width, int height) {
        if (OppoScreenDragUtil.isOffsetState()) {
            Bitmap tmpBitmap = bitmap;
            float scale = OppoScreenDragUtil.getScale();
            if (OppoScreenDragUtil.getOffsetX() > 0) {
                bitmap = Bitmap.createBitmap(bitmap, width - ((int) (((float) width) * scale)), height - ((int) (((float) height) * scale)), (int) (((float) width) * scale), (int) (((float) height) * scale));
            } else {
                bitmap = Bitmap.createBitmap(bitmap, 0, height - ((int) (((float) height) * scale)), (int) (((float) width) * scale), (int) (((float) height) * scale));
            }
            tmpBitmap.recycle();
        }
        return bitmap;
    }

    private static void screenshot(IBinder display, Surface consumer, Rect sourceCrop, int width, int height, int minLayer, int maxLayer, boolean allLayers, boolean useIdentityTransform) {
        if (display == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        } else if (consumer == null) {
            throw new IllegalArgumentException("consumer must not be null");
        } else {
            nativeScreenshot(display, consumer, sourceCrop, width, height, minLayer, maxLayer, allLayers, useIdentityTransform);
        }
    }
}
