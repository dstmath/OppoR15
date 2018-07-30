package android.graphics.drawable;

import android.R;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable.ConstantState;
import android.util.AttributeSet;
import java.io.IOException;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RippleDrawable extends LayerDrawable {
    private static final int MASK_CONTENT = 1;
    private static final int MASK_EXPLICIT = 2;
    private static final int MASK_NONE = 0;
    private static final int MASK_UNKNOWN = -1;
    private static final int MAX_RIPPLES = 10;
    public static final int RADIUS_AUTO = -1;
    private RippleBackground mBackground;
    private boolean mBackgroundActive;
    private int mDensity;
    private final Rect mDirtyBounds;
    private final Rect mDrawingBounds;
    private RippleForeground[] mExitingRipples;
    private int mExitingRipplesCount;
    private boolean mForceSoftware;
    private boolean mHasPending;
    private boolean mHasValidMask;
    private final Rect mHotspotBounds;
    private Drawable mMask;
    private Bitmap mMaskBuffer;
    private Canvas mMaskCanvas;
    private PorterDuffColorFilter mMaskColorFilter;
    private Matrix mMaskMatrix;
    private BitmapShader mMaskShader;
    private boolean mOverrideBounds;
    private float mPendingX;
    private float mPendingY;
    private RippleForeground mRipple;
    private boolean mRippleActive;
    private Paint mRipplePaint;
    private RippleState mState;
    private final Rect mTempRect;

    static class RippleState extends LayerState {
        ColorStateList mColor = ColorStateList.valueOf(Color.MAGENTA);
        int mMaxRadius = -1;
        int[] mTouchThemeAttrs;

        public RippleState(LayerState orig, RippleDrawable owner, Resources res) {
            super(orig, owner, res);
            if (orig != null && (orig instanceof RippleState)) {
                RippleState origs = (RippleState) orig;
                this.mTouchThemeAttrs = origs.mTouchThemeAttrs;
                this.mColor = origs.mColor;
                this.mMaxRadius = origs.mMaxRadius;
                if (origs.mDensity != this.mDensity) {
                    applyDensityScaling(orig.mDensity, this.mDensity);
                }
            }
        }

        protected void onDensityChanged(int sourceDensity, int targetDensity) {
            super.onDensityChanged(sourceDensity, targetDensity);
            applyDensityScaling(sourceDensity, targetDensity);
        }

        private void applyDensityScaling(int sourceDensity, int targetDensity) {
            if (this.mMaxRadius != -1) {
                this.mMaxRadius = Drawable.scaleFromDensity(this.mMaxRadius, sourceDensity, targetDensity, true);
            }
        }

        public boolean canApplyTheme() {
            if (this.mTouchThemeAttrs != null || (this.mColor != null && this.mColor.canApplyTheme())) {
                return true;
            }
            return super.canApplyTheme();
        }

        public Drawable newDrawable() {
            return new RippleDrawable(this, null, null);
        }

        public Drawable newDrawable(Resources res) {
            return new RippleDrawable(this, res, null);
        }

        public int getChangingConfigurations() {
            return (this.mColor != null ? this.mColor.getChangingConfigurations() : 0) | super.getChangingConfigurations();
        }
    }

    /* synthetic */ RippleDrawable(RippleState state, Resources res, RippleDrawable -this2) {
        this(state, res);
    }

    RippleDrawable() {
        this(new RippleState(null, null, null), null);
    }

    public RippleDrawable(ColorStateList color, Drawable content, Drawable mask) {
        this(new RippleState(null, null, null), null);
        if (color == null) {
            throw new IllegalArgumentException("RippleDrawable requires a non-null color");
        }
        if (content != null) {
            addLayer(content, null, 0, 0, 0, 0, 0);
        }
        if (mask != null) {
            addLayer(mask, null, R.id.mask, 0, 0, 0, 0);
        }
        setColor(color);
        ensurePadding();
        refreshPadding();
        updateLocalState();
    }

    public void jumpToCurrentState() {
        super.jumpToCurrentState();
        if (this.mRipple != null) {
            this.mRipple.end();
        }
        if (this.mBackground != null) {
            this.mBackground.end();
        }
        cancelExitingRipples();
    }

    private void cancelExitingRipples() {
        int count = this.mExitingRipplesCount;
        RippleForeground[] ripples = this.mExitingRipples;
        for (int i = 0; i < count; i++) {
            ripples[i].end();
        }
        if (ripples != null) {
            Arrays.fill(ripples, 0, count, null);
        }
        this.mExitingRipplesCount = 0;
        invalidateSelf(false);
    }

    public int getOpacity() {
        return -3;
    }

    protected boolean onStateChange(int[] stateSet) {
        boolean z;
        boolean changed = super.onStateChange(stateSet);
        boolean enabled = false;
        boolean pressed = false;
        boolean focused = false;
        boolean hovered = false;
        for (int state : stateSet) {
            if (state == R.attr.state_enabled) {
                enabled = true;
            } else if (state == R.attr.state_focused) {
                focused = true;
            } else if (state == R.attr.state_pressed) {
                pressed = true;
            } else if (state == R.attr.state_hovered) {
                hovered = true;
            }
        }
        if (enabled) {
            z = pressed;
        } else {
            z = false;
        }
        setRippleActive(z);
        if (hovered || focused) {
            pressed = true;
        } else if (!enabled) {
            pressed = false;
        }
        if (focused) {
            hovered = true;
        }
        setBackgroundActive(pressed, hovered);
        return changed;
    }

    private void setRippleActive(boolean active) {
        if (this.mRippleActive != active) {
            this.mRippleActive = active;
            if (active) {
                tryRippleEnter();
            } else {
                tryRippleExit();
            }
        }
    }

    private void setBackgroundActive(boolean active, boolean focused) {
        if (this.mBackgroundActive != active) {
            this.mBackgroundActive = active;
            if (active) {
                tryBackgroundEnter(focused);
            } else {
                tryBackgroundExit();
            }
        }
    }

    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (!this.mOverrideBounds) {
            this.mHotspotBounds.set(bounds);
            onHotspotBoundsChanged();
        }
        if (this.mBackground != null) {
            this.mBackground.onBoundsChange();
        }
        if (this.mRipple != null) {
            this.mRipple.onBoundsChange();
        }
        invalidateSelf();
    }

    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (!visible) {
            clearHotspots();
        } else if (changed) {
            if (this.mRippleActive) {
                tryRippleEnter();
            }
            if (this.mBackgroundActive) {
                tryBackgroundEnter(false);
            }
            jumpToCurrentState();
        }
        return changed;
    }

    public boolean isProjected() {
        if (isBounded()) {
            return false;
        }
        int radius = this.mState.mMaxRadius;
        Rect drawableBounds = getBounds();
        Rect hotspotBounds = this.mHotspotBounds;
        if (radius == -1 || radius > hotspotBounds.width() / 2 || radius > hotspotBounds.height() / 2 || (!drawableBounds.equals(hotspotBounds) && !drawableBounds.contains(hotspotBounds))) {
            return true;
        }
        return false;
    }

    private boolean isBounded() {
        return getNumberOfLayers() > 0;
    }

    public boolean isStateful() {
        return true;
    }

    public boolean hasFocusStateSpecified() {
        return true;
    }

    public void setColor(ColorStateList color) {
        this.mState.mColor = color;
        invalidateSelf(false);
    }

    public void setRadius(int radius) {
        this.mState.mMaxRadius = radius;
        invalidateSelf(false);
    }

    public int getRadius() {
        return this.mState.mMaxRadius;
    }

    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
        TypedArray a = Drawable.obtainAttributes(r, theme, attrs, com.android.internal.R.styleable.RippleDrawable);
        setPaddingMode(1);
        super.inflate(r, parser, attrs, theme);
        updateStateFromTypedArray(a);
        verifyRequiredAttributes(a);
        a.recycle();
        updateLocalState();
    }

    public boolean setDrawableByLayerId(int id, Drawable drawable) {
        if (!super.setDrawableByLayerId(id, drawable)) {
            return false;
        }
        if (id == R.id.mask) {
            this.mMask = drawable;
            this.mHasValidMask = false;
        }
        return true;
    }

    public void setPaddingMode(int mode) {
        super.setPaddingMode(mode);
    }

    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException {
        RippleState state = this.mState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mTouchThemeAttrs = a.extractThemeAttrs();
        ColorStateList color = a.getColorStateList(0);
        if (color != null) {
            this.mState.mColor = color;
        }
        this.mState.mMaxRadius = a.getDimensionPixelSize(1, this.mState.mMaxRadius);
    }

    private void verifyRequiredAttributes(TypedArray a) throws XmlPullParserException {
        if (this.mState.mColor != null) {
            return;
        }
        if (this.mState.mTouchThemeAttrs == null || this.mState.mTouchThemeAttrs[0] == 0) {
            throw new XmlPullParserException(a.getPositionDescription() + ": <ripple> requires a valid color attribute");
        }
    }

    public void applyTheme(Theme t) {
        super.applyTheme(t);
        RippleState state = this.mState;
        if (state != null) {
            if (state.mTouchThemeAttrs != null) {
                TypedArray a = t.resolveAttributes(state.mTouchThemeAttrs, com.android.internal.R.styleable.RippleDrawable);
                try {
                    updateStateFromTypedArray(a);
                    verifyRequiredAttributes(a);
                } catch (XmlPullParserException e) {
                    Drawable.rethrowAsRuntimeException(e);
                } finally {
                    a.recycle();
                }
            }
            if (state.mColor != null && state.mColor.canApplyTheme()) {
                state.mColor = state.mColor.obtainForTheme(t);
            }
            updateLocalState();
        }
    }

    public boolean canApplyTheme() {
        return (this.mState == null || !this.mState.canApplyTheme()) ? super.canApplyTheme() : true;
    }

    public void setHotspot(float x, float y) {
        if (this.mRipple == null || this.mBackground == null) {
            this.mPendingX = x;
            this.mPendingY = y;
            this.mHasPending = true;
        }
        if (this.mRipple != null) {
            this.mRipple.move(x, y);
        }
    }

    private void tryBackgroundEnter(boolean focused) {
        if (this.mBackground == null) {
            this.mBackground = new RippleBackground(this, this.mHotspotBounds, isBounded(), this.mForceSoftware);
        }
        this.mBackground.setup((float) this.mState.mMaxRadius, this.mDensity);
        this.mBackground.enter(focused);
    }

    private void tryBackgroundExit() {
        if (this.mBackground != null) {
            this.mBackground.exit();
        }
    }

    private void tryRippleEnter() {
        if (this.mExitingRipplesCount < 10) {
            if (this.mRipple == null) {
                float x;
                float y;
                if (this.mHasPending) {
                    this.mHasPending = false;
                    x = this.mPendingX;
                    y = this.mPendingY;
                } else {
                    x = this.mHotspotBounds.exactCenterX();
                    y = this.mHotspotBounds.exactCenterY();
                }
                this.mRipple = new RippleForeground(this, this.mHotspotBounds, x, y, isBounded(), this.mForceSoftware);
            }
            this.mRipple.setup((float) this.mState.mMaxRadius, this.mDensity);
            this.mRipple.enter(false);
        }
    }

    private void tryRippleExit() {
        if (this.mRipple != null) {
            if (this.mExitingRipples == null) {
                this.mExitingRipples = new RippleForeground[10];
            }
            RippleForeground[] rippleForegroundArr = this.mExitingRipples;
            int i = this.mExitingRipplesCount;
            this.mExitingRipplesCount = i + 1;
            rippleForegroundArr[i] = this.mRipple;
            this.mRipple.exit();
            this.mRipple = null;
        }
    }

    private void clearHotspots() {
        if (this.mRipple != null) {
            this.mRipple.end();
            this.mRipple = null;
            this.mRippleActive = false;
        }
        if (this.mBackground != null) {
            this.mBackground.end();
            this.mBackground = null;
            this.mBackgroundActive = false;
        }
        cancelExitingRipples();
    }

    public void setHotspotBounds(int left, int top, int right, int bottom) {
        this.mOverrideBounds = true;
        this.mHotspotBounds.set(left, top, right, bottom);
        onHotspotBoundsChanged();
    }

    public void getHotspotBounds(Rect outRect) {
        outRect.set(this.mHotspotBounds);
    }

    private void onHotspotBoundsChanged() {
        int count = this.mExitingRipplesCount;
        RippleForeground[] ripples = this.mExitingRipples;
        for (int i = 0; i < count; i++) {
            ripples[i].onHotspotBoundsChanged();
        }
        if (this.mRipple != null) {
            this.mRipple.onHotspotBoundsChanged();
        }
        if (this.mBackground != null) {
            this.mBackground.onHotspotBoundsChanged();
        }
    }

    public void getOutline(Outline outline) {
        LayerState state = this.mLayerState;
        ChildDrawable[] children = state.mChildren;
        int N = state.mNumChildren;
        for (int i = 0; i < N; i++) {
            if (children[i].mId != R.id.mask) {
                children[i].mDrawable.getOutline(outline);
                if (!outline.isEmpty()) {
                    return;
                }
            }
        }
    }

    public void draw(Canvas canvas) {
        pruneRipples();
        Rect bounds = getDirtyBounds();
        int saveCount = canvas.save(2);
        canvas.clipRect(bounds);
        drawContent(canvas);
        drawBackgroundAndRipples(canvas);
        canvas.restoreToCount(saveCount);
    }

    public void invalidateSelf() {
        invalidateSelf(true);
    }

    void invalidateSelf(boolean invalidateMask) {
        super.invalidateSelf();
        if (invalidateMask) {
            this.mHasValidMask = false;
        }
    }

    private void pruneRipples() {
        RippleForeground[] ripples = this.mExitingRipples;
        int count = this.mExitingRipplesCount;
        int i = 0;
        int remaining = 0;
        while (i < count) {
            int remaining2;
            if (ripples[i].hasFinishedExit()) {
                remaining2 = remaining;
            } else {
                remaining2 = remaining + 1;
                ripples[remaining] = ripples[i];
            }
            i++;
            remaining = remaining2;
        }
        for (i = remaining; i < count; i++) {
            ripples[i] = null;
        }
        this.mExitingRipplesCount = remaining;
    }

    private void updateMaskShaderIfNeeded() {
        if (!this.mHasValidMask) {
            int maskType = getMaskType();
            if (maskType != -1) {
                this.mHasValidMask = true;
                Rect bounds = getBounds();
                if (maskType == 0 || bounds.isEmpty()) {
                    if (this.mMaskBuffer != null) {
                        this.mMaskBuffer.recycle();
                        this.mMaskBuffer = null;
                        this.mMaskShader = null;
                        this.mMaskCanvas = null;
                    }
                    this.mMaskMatrix = null;
                    this.mMaskColorFilter = null;
                    return;
                }
                if (this.mMaskBuffer != null && this.mMaskBuffer.getWidth() == bounds.width() && this.mMaskBuffer.getHeight() == bounds.height()) {
                    this.mMaskBuffer.eraseColor(0);
                } else {
                    if (this.mMaskBuffer != null) {
                        this.mMaskBuffer.recycle();
                    }
                    this.mMaskBuffer = Bitmap.createBitmap(bounds.width(), bounds.height(), Config.ALPHA_8);
                    this.mMaskShader = new BitmapShader(this.mMaskBuffer, TileMode.CLAMP, TileMode.CLAMP);
                    this.mMaskCanvas = new Canvas(this.mMaskBuffer);
                }
                if (this.mMaskMatrix == null) {
                    this.mMaskMatrix = new Matrix();
                } else {
                    this.mMaskMatrix.reset();
                }
                if (this.mMaskColorFilter == null) {
                    this.mMaskColorFilter = new PorterDuffColorFilter(0, Mode.SRC_IN);
                }
                int left = bounds.left;
                int top = bounds.top;
                this.mMaskCanvas.translate((float) (-left), (float) (-top));
                if (maskType == 2) {
                    drawMask(this.mMaskCanvas);
                } else if (maskType == 1) {
                    drawContent(this.mMaskCanvas);
                }
                this.mMaskCanvas.translate((float) left, (float) top);
            }
        }
    }

    private int getMaskType() {
        if (this.mRipple == null && this.mExitingRipplesCount <= 0 && (this.mBackground == null || (this.mBackground.isVisible() ^ 1) != 0)) {
            return -1;
        }
        if (this.mMask == null) {
            ChildDrawable[] array = this.mLayerState.mChildren;
            int count = this.mLayerState.mNumChildren;
            for (int i = 0; i < count; i++) {
                if (array[i].mDrawable.getOpacity() != -1) {
                    return 1;
                }
            }
            return 0;
        } else if (this.mMask.getOpacity() == -1) {
            return 0;
        } else {
            return 2;
        }
    }

    private void drawContent(Canvas canvas) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int count = this.mLayerState.mNumChildren;
        for (int i = 0; i < count; i++) {
            if (array[i].mId != R.id.mask) {
                array[i].mDrawable.draw(canvas);
            }
        }
    }

    private void drawBackgroundAndRipples(Canvas canvas) {
        RippleForeground active = this.mRipple;
        RippleBackground background = this.mBackground;
        int count = this.mExitingRipplesCount;
        if (active != null || count > 0 || (background != null && (background.isVisible() ^ 1) == 0)) {
            float x = this.mHotspotBounds.exactCenterX();
            float y = this.mHotspotBounds.exactCenterY();
            canvas.translate(x, y);
            updateMaskShaderIfNeeded();
            if (this.mMaskShader != null) {
                Rect bounds = getBounds();
                this.mMaskMatrix.setTranslate(((float) bounds.left) - x, ((float) bounds.top) - y);
                this.mMaskShader.setLocalMatrix(this.mMaskMatrix);
            }
            int color = this.mState.mColor.getColorForState(getState(), -16777216);
            int halfAlpha = (Color.alpha(color) / 2) << 24;
            Paint p = getRipplePaint();
            if (this.mMaskColorFilter != null) {
                this.mMaskColorFilter.setColor(color | -16777216);
                p.setColor(halfAlpha);
                p.setColorFilter(this.mMaskColorFilter);
                p.setShader(this.mMaskShader);
            } else {
                p.setColor((16777215 & color) | halfAlpha);
                p.setColorFilter(null);
                p.setShader(null);
            }
            if (background != null && background.isVisible()) {
                background.draw(canvas, p);
            }
            if (count > 0) {
                RippleForeground[] ripples = this.mExitingRipples;
                for (int i = 0; i < count; i++) {
                    ripples[i].draw(canvas, p);
                }
            }
            if (active != null) {
                active.draw(canvas, p);
            }
            canvas.translate(-x, -y);
        }
    }

    private void drawMask(Canvas canvas) {
        this.mMask.draw(canvas);
    }

    private Paint getRipplePaint() {
        if (this.mRipplePaint == null) {
            this.mRipplePaint = new Paint();
            this.mRipplePaint.setAntiAlias(true);
            this.mRipplePaint.setStyle(Style.FILL);
        }
        return this.mRipplePaint;
    }

    public Rect getDirtyBounds() {
        if (isBounded()) {
            return getBounds();
        }
        Rect drawingBounds = this.mDrawingBounds;
        Rect dirtyBounds = this.mDirtyBounds;
        dirtyBounds.set(drawingBounds);
        drawingBounds.setEmpty();
        int cX = (int) this.mHotspotBounds.exactCenterX();
        int cY = (int) this.mHotspotBounds.exactCenterY();
        Rect rippleBounds = this.mTempRect;
        RippleForeground[] activeRipples = this.mExitingRipples;
        int N = this.mExitingRipplesCount;
        for (int i = 0; i < N; i++) {
            activeRipples[i].getBounds(rippleBounds);
            rippleBounds.offset(cX, cY);
            drawingBounds.union(rippleBounds);
        }
        RippleBackground background = this.mBackground;
        if (background != null) {
            background.getBounds(rippleBounds);
            rippleBounds.offset(cX, cY);
            drawingBounds.union(rippleBounds);
        }
        dirtyBounds.union(drawingBounds);
        dirtyBounds.union(super.getDirtyBounds());
        return dirtyBounds;
    }

    public void setForceSoftware(boolean forceSoftware) {
        this.mForceSoftware = forceSoftware;
    }

    public ConstantState getConstantState() {
        return this.mState;
    }

    public Drawable mutate() {
        super.mutate();
        this.mState = (RippleState) this.mLayerState;
        this.mMask = findDrawableByLayerId(R.id.mask);
        return this;
    }

    RippleState createConstantState(LayerState state, Resources res) {
        return new RippleState(state, this, res);
    }

    private RippleDrawable(RippleState state, Resources res) {
        this.mTempRect = new Rect();
        this.mHotspotBounds = new Rect();
        this.mDrawingBounds = new Rect();
        this.mDirtyBounds = new Rect();
        this.mExitingRipplesCount = 0;
        this.mState = new RippleState(state, this, res);
        this.mLayerState = this.mState;
        this.mDensity = Drawable.resolveDensity(res, this.mState.mDensity);
        if (this.mState.mNumChildren > 0) {
            ensurePadding();
            refreshPadding();
        }
        updateLocalState();
    }

    private void updateLocalState() {
        this.mMask = findDrawableByLayerId(R.id.mask);
    }
}
