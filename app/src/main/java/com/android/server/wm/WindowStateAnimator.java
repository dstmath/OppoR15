package com.android.server.wm;

import android.app.ActivityManager.StackId;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.net.arp.OppoArpPeer;
import android.os.Debug;
import android.os.Trace;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.MagnificationSpec;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceControl;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerPolicy;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import com.android.server.display.OppoBrightUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;

class WindowStateAnimator {
    static final int COMMIT_DRAW_PENDING = 2;
    static boolean DEBUG_ANIM = WindowManagerDebugConfig.DEBUG_ANIM;
    static boolean DEBUG_LAYERS = WindowManagerDebugConfig.DEBUG_LAYERS;
    static boolean DEBUG_ORIENTATION = WindowManagerDebugConfig.DEBUG_ORIENTATION;
    static boolean DEBUG_STARTING_WINDOW = WindowManagerDebugConfig.DEBUG_STARTING_WINDOW;
    static boolean DEBUG_SURFACE_TRACE = WindowManagerDebugConfig.DEBUG_SURFACE_TRACE;
    static boolean DEBUG_VISIBILITY = false;
    static final int DRAW_PENDING = 1;
    static final int HAS_DRAWN = 4;
    static final int NO_SURFACE = 0;
    static final long PENDING_TRANSACTION_FINISH_WAIT_TIME = 100;
    static final int READY_TO_SHOW = 3;
    static boolean SHOW_LIGHT_TRANSACTIONS = WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS;
    static boolean SHOW_SURFACE_ALLOC = WindowManagerDebugConfig.SHOW_SURFACE_ALLOC;
    static boolean SHOW_TRANSACTIONS = WindowManagerDebugConfig.SHOW_TRANSACTIONS;
    static final int STACK_CLIP_AFTER_ANIM = 0;
    static final int STACK_CLIP_BEFORE_ANIM = 1;
    static final int STACK_CLIP_NONE = 2;
    static final String TAG = "WindowManager";
    static final int WINDOW_FREEZE_LAYER = 2000000;
    static boolean localLOGV = WindowManagerService.localLOGV;
    float mAlpha = OppoBrightUtils.MIN_LUX_LIMITI;
    private int mAnimDx;
    private int mAnimDy;
    int mAnimLayer;
    boolean mAnimateMove = false;
    boolean mAnimating;
    Animation mAnimation;
    boolean mAnimationIsEntrance;
    private boolean mAnimationStartDelayed;
    long mAnimationStartTime;
    final WindowAnimator mAnimator;
    AppWindowAnimator mAppAnimator;
    int mAttrType;
    Rect mClipRect = new Rect();
    final Context mContext;
    boolean mDestroyDeferredFlag = false;
    private boolean mDestroyPreservedSurfaceUponRedraw;
    int mDrawState;
    float mDsDx = 1.0f;
    float mDsDy = OppoBrightUtils.MIN_LUX_LIMITI;
    float mDtDx = OppoBrightUtils.MIN_LUX_LIMITI;
    float mDtDy = 1.0f;
    boolean mEnterAnimationPending;
    boolean mEnteringAnimation;
    float mExtraHScale = 1.0f;
    float mExtraVScale = 1.0f;
    boolean mForceScaleUntilResize;
    boolean mHasClipRect;
    boolean mHasLocalTransformation;
    boolean mHasTransformation;
    boolean mHaveMatrix;
    final boolean mIsWallpaper;
    float mLastAlpha = OppoBrightUtils.MIN_LUX_LIMITI;
    long mLastAnimationTime;
    Rect mLastClipRect = new Rect();
    private float mLastDsDx = 1.0f;
    private float mLastDsDy = OppoBrightUtils.MIN_LUX_LIMITI;
    private float mLastDtDx = OppoBrightUtils.MIN_LUX_LIMITI;
    private float mLastDtDy = 1.0f;
    Rect mLastFinalClipRect = new Rect();
    boolean mLastHidden;
    int mLastLayer;
    private final Rect mLastSystemDecorRect = new Rect();
    boolean mLocalAnimating;
    private final WindowStateAnimator mParentWinAnimator;
    private WindowSurfaceController mPendingDestroySurface;
    final WindowManagerPolicy mPolicy;
    boolean mReportSurfaceResized;
    final WindowManagerService mService;
    final Session mSession;
    float mShownAlpha = OppoBrightUtils.MIN_LUX_LIMITI;
    int mStackClip = 1;
    WindowSurfaceController mSurfaceController;
    boolean mSurfaceDestroyDeferred;
    int mSurfaceFormat;
    boolean mSurfaceResized;
    private final Rect mSystemDecorRect = new Rect();
    private Rect mTmpAnimatingBounds = new Rect();
    Rect mTmpClipRect = new Rect();
    Rect mTmpFinalClipRect = new Rect();
    private final Rect mTmpSize = new Rect();
    private Rect mTmpSourceBounds = new Rect();
    Rect mTmpStackBounds = new Rect();
    final Transformation mTransformation = new Transformation();
    private final WallpaperController mWallpaperControllerLocked;
    boolean mWasAnimating;
    final WindowState mWin;

    String drawStateToString() {
        switch (this.mDrawState) {
            case 0:
                return "NO_SURFACE";
            case 1:
                return "DRAW_PENDING";
            case 2:
                return "COMMIT_DRAW_PENDING";
            case 3:
                return "READY_TO_SHOW";
            case 4:
                return "HAS_DRAWN";
            default:
                return Integer.toString(this.mDrawState);
        }
    }

    WindowStateAnimator(WindowState win) {
        AppWindowAnimator appWindowAnimator = null;
        WindowManagerService service = win.mService;
        this.mService = service;
        this.mAnimator = service.mAnimator;
        this.mPolicy = service.mPolicy;
        this.mContext = service.mContext;
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent != null) {
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            this.mAnimDx = displayInfo.appWidth;
            this.mAnimDy = displayInfo.appHeight;
        } else {
            Slog.w(TAG, "WindowStateAnimator ctor: Display has been removed");
        }
        this.mWin = win;
        this.mParentWinAnimator = !win.isChildWindow() ? null : win.getParentWindow().mWinAnimator;
        if (win.mAppToken != null) {
            appWindowAnimator = win.mAppToken.mAppAnimator;
        }
        this.mAppAnimator = appWindowAnimator;
        this.mSession = win.mSession;
        this.mAttrType = win.mAttrs.type;
        this.mIsWallpaper = win.mIsWallpaper;
        this.mWallpaperControllerLocked = this.mService.mRoot.mWallpaperController;
    }

    public void setAnimation(Animation anim, long startTime, int stackClip) {
        int i = 0;
        if (localLOGV) {
            Slog.v(TAG, "Setting animation in " + this + ": " + anim);
        }
        String name = null;
        if (!(this.mAppAnimator == null || this.mAppAnimator.mAppToken == null || this.mAppAnimator.mAppToken.token == null)) {
            name = this.mAppAnimator.mAppToken.token.toString();
        }
        this.mAnimating = false;
        this.mLocalAnimating = false;
        this.mAnimation = anim;
        this.mAnimation.restrictDuration(10000);
        if (name == null || !name.contains("plugin.luckymoney.ui.LuckyMoney")) {
            this.mAnimation.scaleCurrentDuration(this.mService.getWindowAnimationScaleLocked());
        } else {
            this.mAnimation.scaleCurrentDuration(OppoBrightUtils.MIN_LUX_LIMITI);
            Slog.d(TAG, "Cancel Animation, name = " + name);
        }
        this.mTransformation.clear();
        Transformation transformation = this.mTransformation;
        if (!this.mLastHidden) {
            i = 1;
        }
        transformation.setAlpha((float) i);
        this.mHasLocalTransformation = true;
        this.mAnimationStartTime = startTime;
        this.mStackClip = stackClip;
    }

    public void setAnimation(Animation anim, int stackClip) {
        setAnimation(anim, -1, stackClip);
    }

    public void setAnimation(Animation anim) {
        setAnimation(anim, -1, 0);
    }

    public void clearAnimation() {
        if (this.mAnimation != null) {
            this.mAnimating = true;
            this.mLocalAnimating = false;
            this.mAnimation.cancel();
            this.mAnimation = null;
            this.mStackClip = 1;
        }
    }

    boolean isAnimationSet() {
        if (this.mAnimation == null && (this.mParentWinAnimator == null || this.mParentWinAnimator.mAnimation == null)) {
            return this.mAppAnimator != null ? this.mAppAnimator.isAnimating() : false;
        } else {
            return true;
        }
    }

    boolean isAnimationStarting() {
        return isAnimationSet() ? this.mAnimating ^ 1 : false;
    }

    boolean isDummyAnimation() {
        if (this.mAppAnimator == null || this.mAppAnimator.animation != AppWindowAnimator.sDummyAnimation) {
            return false;
        }
        return true;
    }

    boolean isWindowAnimationSet() {
        return this.mAnimation != null;
    }

    boolean isWaitingForOpening() {
        if (this.mService.mAppTransition.isTransitionSet() && isDummyAnimation()) {
            return this.mService.mOpeningApps.contains(this.mWin.mAppToken);
        }
        return false;
    }

    void cancelExitAnimationForNextAnimationLocked() {
        if (DEBUG_ANIM) {
            Slog.d(TAG, "cancelExitAnimationForNextAnimationLocked: " + this.mWin);
        }
        if (this.mAnimation != null) {
            this.mAnimation.cancel();
            this.mAnimation = null;
            this.mLocalAnimating = false;
            this.mWin.destroyOrSaveSurfaceUnchecked();
        } else if (this.mWin != null && this.mWin.getWindowTag() != null && this.mWin.getWindowTag().toString().contains("com.coloros.compass") && isAnimationSet()) {
            if (DEBUG_ANIM || WindowManagerService.DEBUG_WMS) {
                Slog.d(TAG, "Animation is set,destroy: " + this.mWin);
            }
            this.mWin.destroyOrSaveSurfaceUnchecked();
        }
    }

    private boolean stepAnimation(long currentTime) {
        if (this.mAnimation == null || (this.mLocalAnimating ^ 1) != 0) {
            return false;
        }
        currentTime = getAnimationFrameTime(this.mAnimation, currentTime);
        this.mTransformation.clear();
        boolean more = this.mAnimation.getTransformation(currentTime, this.mTransformation);
        if (this.mAnimationStartDelayed && this.mAnimationIsEntrance) {
            this.mTransformation.setAlpha(OppoBrightUtils.MIN_LUX_LIMITI);
        }
        return more;
    }

    boolean stepAnimationLocked(long currentTime) {
        this.mWasAnimating = this.mAnimating;
        DisplayContent displayContent = this.mWin.getDisplayContent();
        if (this.mWin.mToken.okToAnimate()) {
            if (this.mWin.isDrawnLw() && this.mAnimation != null) {
                this.mHasTransformation = true;
                this.mHasLocalTransformation = true;
                if (!this.mLocalAnimating) {
                    long j;
                    if (DEBUG_ANIM) {
                        Slog.v(TAG, "Starting animation in " + this + " @ " + currentTime + ": ww=" + this.mWin.mFrame.width() + " wh=" + this.mWin.mFrame.height() + " dx=" + this.mAnimDx + " dy=" + this.mAnimDy + " scale=" + this.mService.getWindowAnimationScaleLocked());
                    }
                    DisplayInfo displayInfo = displayContent.getDisplayInfo();
                    if (this.mAnimateMove) {
                        this.mAnimateMove = false;
                        this.mAnimation.initialize(this.mWin.mFrame.width(), this.mWin.mFrame.height(), this.mAnimDx, this.mAnimDy);
                    } else {
                        this.mAnimation.initialize(this.mWin.mFrame.width(), this.mWin.mFrame.height(), displayInfo.appWidth, displayInfo.appHeight);
                    }
                    this.mAnimDx = displayInfo.appWidth;
                    this.mAnimDy = displayInfo.appHeight;
                    Animation animation = this.mAnimation;
                    if (this.mAnimationStartTime != -1) {
                        j = this.mAnimationStartTime;
                    } else {
                        j = currentTime;
                    }
                    animation.setStartTime(j);
                    this.mLocalAnimating = true;
                    this.mAnimating = true;
                }
                if (this.mAnimation != null && this.mLocalAnimating) {
                    this.mLastAnimationTime = currentTime;
                    if (stepAnimation(currentTime)) {
                        return true;
                    }
                }
                if (DEBUG_ANIM) {
                    Slog.v(TAG, "Finished animation in " + this + " @ " + currentTime);
                }
            }
            this.mHasLocalTransformation = false;
            if ((!this.mLocalAnimating || this.mAnimationIsEntrance) && this.mAppAnimator != null && this.mAppAnimator.animation != null) {
                this.mAnimating = true;
                this.mHasTransformation = true;
                this.mTransformation.clear();
                return false;
            } else if (this.mHasTransformation) {
                this.mAnimating = true;
            } else if (isAnimationSet()) {
                this.mAnimating = true;
            }
        } else if (this.mAnimation != null) {
            this.mAnimating = true;
        }
        if (!this.mAnimating && (this.mLocalAnimating ^ 1) != 0) {
            return false;
        }
        if (DEBUG_ANIM) {
            boolean z;
            String str = TAG;
            StringBuilder append = new StringBuilder().append("Animation done in ").append(this).append(": exiting=").append(this.mWin.mAnimatingExit).append(", reportedVisible=");
            if (this.mWin.mAppToken != null) {
                z = this.mWin.mAppToken.reportedVisible;
            } else {
                z = false;
            }
            Slog.v(str, append.append(z).toString());
        }
        this.mAnimating = false;
        this.mLocalAnimating = false;
        if (this.mAnimation != null) {
            this.mAnimation.cancel();
            this.mAnimation = null;
        }
        if (this.mAnimator.mWindowDetachedWallpaper == this.mWin) {
            this.mAnimator.mWindowDetachedWallpaper = null;
        }
        this.mAnimLayer = this.mWin.getSpecialWindowAnimLayerAdjustment();
        if (DEBUG_LAYERS) {
            Slog.v(TAG, "Stepping win " + this + " anim layer: " + this.mAnimLayer);
        }
        this.mHasTransformation = false;
        this.mHasLocalTransformation = false;
        this.mStackClip = 1;
        this.mWin.checkPolicyVisibilityChange();
        this.mTransformation.clear();
        if (this.mAttrType == OppoArpPeer.ARP_FIRST_RESPONSE_TIMEOUT && this.mWin.mPolicyVisibility && displayContent != null) {
            displayContent.setLayoutNeeded();
        }
        this.mWin.onExitAnimationDone();
        int displayId = this.mWin.getDisplayId();
        this.mAnimator.setPendingLayoutChanges(displayId, 8);
        if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
            this.mService.mWindowPlacerLocked.debugLayoutRepeats("WindowStateAnimator", this.mAnimator.getPendingLayoutChanges(displayId));
        }
        if (this.mWin.mAppToken != null) {
            this.mWin.mAppToken.updateReportedVisibilityLocked();
        }
        return false;
    }

    void hide(String reason) {
        if (!this.mLastHidden) {
            this.mLastHidden = true;
            if (this.mSurfaceController != null) {
                this.mSurfaceController.hideInTransaction(reason);
            }
        }
    }

    boolean finishDrawingLocked() {
        boolean startingWindow = this.mWin.mAttrs.type == 3;
        if (DEBUG_STARTING_WINDOW && startingWindow) {
            Slog.v(TAG, "Finishing drawing window " + this.mWin + ": mDrawState=" + drawStateToString());
        }
        boolean layoutNeeded = this.mWin.clearAnimatingWithSavedSurface();
        if (this.mDrawState != 1) {
            return layoutNeeded;
        }
        if (DEBUG_SURFACE_TRACE || DEBUG_ANIM || SHOW_TRANSACTIONS || DEBUG_ORIENTATION || WindowManagerService.DEBUG_WMS) {
            Slog.v(TAG, "finishDrawingLocked: mDrawState=COMMIT_DRAW_PENDING " + this.mWin + " in " + this.mSurfaceController);
        }
        if (DEBUG_STARTING_WINDOW && startingWindow) {
            Slog.v(TAG, "Draw state now committed in " + this.mWin);
        }
        this.mDrawState = 2;
        return true;
    }

    boolean commitFinishDrawingLocked() {
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE && this.mWin.mAttrs.type == 3) {
            Slog.i(TAG, "commitFinishDrawingLocked: " + this.mWin + " cur mDrawState=" + drawStateToString());
        }
        if (this.mDrawState != 2 && this.mDrawState != 3) {
            return false;
        }
        if (DEBUG_SURFACE_TRACE || DEBUG_ANIM) {
            Slog.i(TAG, "commitFinishDrawingLocked: mDrawState=READY_TO_SHOW " + this.mSurfaceController);
        }
        this.mDrawState = 3;
        boolean result = false;
        AppWindowToken atoken = this.mWin.mAppToken;
        if (atoken == null || atoken.allDrawn || this.mWin.mAttrs.type == 3) {
            result = this.mWin.performShowLocked();
        }
        return result;
    }

    void preserveSurfaceLocked() {
        if (this.mDestroyPreservedSurfaceUponRedraw) {
            this.mSurfaceDestroyDeferred = false;
            destroySurfaceLocked();
            this.mSurfaceDestroyDeferred = true;
            return;
        }
        if (SHOW_TRANSACTIONS) {
            WindowManagerService.logSurface(this.mWin, "SET FREEZE LAYER", false);
        }
        if (this.mSurfaceController != null) {
            this.mSurfaceController.setLayer(this.mAnimLayer + 1);
        }
        this.mDestroyPreservedSurfaceUponRedraw = true;
        this.mSurfaceDestroyDeferred = true;
        destroySurfaceLocked();
    }

    void destroyPreservedSurfaceLocked() {
        if (this.mDestroyPreservedSurfaceUponRedraw) {
            if (!(this.mSurfaceController == null || this.mPendingDestroySurface == null || (this.mWin.mAppToken != null && this.mWin.mAppToken.isRelaunching()))) {
                SurfaceControl.openTransaction();
                this.mPendingDestroySurface.reparentChildrenInTransaction(this.mSurfaceController);
                SurfaceControl.closeTransaction();
            }
            destroyDeferredSurfaceLocked();
            this.mDestroyPreservedSurfaceUponRedraw = false;
        }
    }

    void markPreservedSurfaceForDestroy() {
        if (this.mDestroyPreservedSurfaceUponRedraw && (this.mService.mDestroyPreservedSurface.contains(this.mWin) ^ 1) != 0) {
            this.mService.mDestroyPreservedSurface.add(this.mWin);
        }
    }

    private int getLayerStack() {
        return this.mWin.getDisplayContent().getDisplay().getLayerStack();
    }

    void updateLayerStackInTransaction() {
        if (this.mSurfaceController != null) {
            this.mSurfaceController.setLayerStackInTransaction(getLayerStack());
        }
    }

    void resetDrawState() {
        this.mDrawState = 1;
        if (this.mWin.mAppToken != null) {
            if (this.mWin.mAppToken.mAppAnimator.animation == null) {
                this.mWin.mAppToken.clearAllDrawn();
            } else {
                this.mWin.mAppToken.deferClearAllDrawn = true;
            }
        }
    }

    WindowSurfaceController createSurfaceLocked(int windowType, int ownerUid) {
        WindowState w = this.mWin;
        if (w.restoreSavedSurface()) {
            if (DEBUG_ANIM) {
                Slog.i(TAG, "createSurface: " + this + ": called when we had a saved surface");
            }
            return this.mSurfaceController;
        } else if (this.mSurfaceController != null) {
            return this.mSurfaceController;
        } else {
            if ((this.mWin.mAttrs.privateFlags & DumpState.DUMP_DEXOPT) != 0) {
                windowType = 441731;
            }
            w.setHasSurface(false);
            if (DEBUG_ANIM || DEBUG_ORIENTATION) {
                Slog.i(TAG, "createSurface " + this + ": mDrawState=DRAW_PENDING");
            }
            resetDrawState();
            this.mService.makeWindowFreezingScreenIfNeededLocked(w);
            int flags = 4;
            LayoutParams attrs = w.mAttrs;
            if (this.mService.isSecureLocked(w)) {
                flags = 132;
            }
            this.mTmpSize.set(w.mFrame.left + w.mXOffset, w.mFrame.top + w.mYOffset, 0, 0);
            calculateSurfaceBounds(w, attrs);
            int width = this.mTmpSize.width();
            int height = this.mTmpSize.height();
            if (DEBUG_VISIBILITY) {
                Slog.v(TAG, "Creating surface in session " + this.mSession.mSurfaceSession + " window " + this + " w=" + width + " h=" + height + " x=" + this.mTmpSize.left + " y=" + this.mTmpSize.top + " format=" + attrs.format + " flags=" + flags);
            }
            this.mLastSystemDecorRect.set(0, 0, 0, 0);
            this.mHasClipRect = false;
            this.mClipRect.set(0, 0, 0, 0);
            this.mLastClipRect.set(0, 0, 0, 0);
            try {
                int format = (attrs.flags & 16777216) != 0 ? -3 : attrs.format;
                if (!PixelFormat.formatHasAlpha(attrs.format) && attrs.surfaceInsets.left == 0 && attrs.surfaceInsets.top == 0 && attrs.surfaceInsets.right == 0 && attrs.surfaceInsets.bottom == 0 && (w.isDragResizing() ^ 1) != 0) {
                    flags |= 1024;
                }
                this.mSurfaceController = new WindowSurfaceController(this.mSession.mSurfaceSession, attrs.getTitle().toString(), width, height, format, flags, this, windowType, ownerUid);
                this.mSurfaceFormat = format;
                w.setHasSurface(true);
                if (SHOW_TRANSACTIONS || SHOW_SURFACE_ALLOC) {
                    Slog.i(TAG, "  CREATE SURFACE " + this.mSurfaceController + " IN SESSION " + this.mSession.mSurfaceSession + ": pid=" + this.mSession.mPid + " format=" + attrs.format + " flags=0x" + Integer.toHexString(flags) + " / " + this);
                }
                if (WindowManagerService.localLOGV) {
                    Slog.v(TAG, "Got surface: " + this.mSurfaceController + ", set left=" + w.mFrame.left + " top=" + w.mFrame.top + ", animLayer=" + this.mAnimLayer);
                }
                if (SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i(TAG, ">>> OPEN TRANSACTION createSurfaceLocked");
                    WindowManagerService.logSurface(w, "CREATE pos=(" + w.mFrame.left + "," + w.mFrame.top + ") (" + width + "x" + height + "), layer=" + this.mAnimLayer + " HIDE", false);
                }
                this.mService.openSurfaceTransaction();
                try {
                    this.mSurfaceController.setPositionInTransaction((float) this.mTmpSize.left, (float) this.mTmpSize.top, false);
                    this.mSurfaceController.setLayerStackInTransaction(getLayerStack());
                    this.mSurfaceController.setLayer(this.mAnimLayer);
                    this.mLastHidden = true;
                    if (WindowManagerService.localLOGV) {
                        Slog.v(TAG, "Created surface " + this);
                    }
                    return this.mSurfaceController;
                } finally {
                    this.mService.closeSurfaceTransaction();
                }
            } catch (OutOfResourcesException e) {
                Slog.w(TAG, "OutOfResourcesException creating surface");
                this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", true);
                this.mDrawState = 0;
                return null;
            } catch (Exception e2) {
                Slog.e(TAG, "Exception creating surface", e2);
                this.mDrawState = 0;
                return null;
            }
        }
    }

    private void calculateSurfaceBounds(WindowState w, LayoutParams attrs) {
        if ((attrs.flags & 16384) != 0) {
            this.mTmpSize.right = this.mTmpSize.left + w.mRequestedWidth;
            this.mTmpSize.bottom = this.mTmpSize.top + w.mRequestedHeight;
        } else if (w.isDragResizing()) {
            if (w.getResizeMode() == 0) {
                this.mTmpSize.left = 0;
                this.mTmpSize.top = 0;
            }
            DisplayInfo displayInfo = w.getDisplayInfo();
            this.mTmpSize.right = this.mTmpSize.left + displayInfo.logicalWidth;
            this.mTmpSize.bottom = this.mTmpSize.top + displayInfo.logicalHeight;
        } else {
            this.mTmpSize.right = this.mTmpSize.left + w.mCompatFrame.width();
            this.mTmpSize.bottom = this.mTmpSize.top + w.mCompatFrame.height();
        }
        if (this.mTmpSize.width() < 1) {
            this.mTmpSize.right = this.mTmpSize.left + 1;
        }
        if (this.mTmpSize.height() < 1) {
            this.mTmpSize.bottom = this.mTmpSize.top + 1;
        }
        Rect rect = this.mTmpSize;
        rect.left -= attrs.surfaceInsets.left;
        rect = this.mTmpSize;
        rect.top -= attrs.surfaceInsets.top;
        rect = this.mTmpSize;
        rect.right += attrs.surfaceInsets.right;
        rect = this.mTmpSize;
        rect.bottom += attrs.surfaceInsets.bottom;
    }

    boolean hasSurface() {
        if (this.mWin.hasSavedSurface() || this.mSurfaceController == null) {
            return false;
        }
        return this.mSurfaceController.hasSurface();
    }

    void destroySurfaceLocked() {
        AppWindowToken wtoken = this.mWin.mAppToken;
        if (wtoken != null && this.mWin == wtoken.startingWindow) {
            wtoken.startingDisplayed = false;
        }
        this.mWin.clearHasSavedSurface();
        if (this.mSurfaceController != null) {
            if (!this.mDestroyPreservedSurfaceUponRedraw) {
                this.mWin.mHidden = true;
            }
            try {
                if (DEBUG_VISIBILITY) {
                    WindowManagerService.logWithStack(TAG, "Window " + this + " destroying surface " + this.mSurfaceController + ", session " + this.mSession);
                }
                if (this.mSurfaceDestroyDeferred) {
                    if (!(this.mSurfaceController == null || this.mPendingDestroySurface == this.mSurfaceController)) {
                        if (this.mPendingDestroySurface != null) {
                            if (SHOW_TRANSACTIONS || SHOW_SURFACE_ALLOC) {
                                WindowManagerService.logSurface(this.mWin, "DESTROY PENDING", true);
                            }
                            this.mPendingDestroySurface.destroyInTransaction();
                        }
                        this.mPendingDestroySurface = this.mSurfaceController;
                    }
                    if (this.mDestroyDeferredFlag && this.mWin.mAppToken != null && this.mWin.mAppToken.isClientHidden()) {
                        if (SHOW_TRANSACTIONS) {
                            Slog.i(TAG, "HideDeferSurface mWin:" + this.mWin);
                        }
                        hide("HideDeferSurface");
                    }
                } else {
                    if (SHOW_TRANSACTIONS || SHOW_SURFACE_ALLOC) {
                        WindowManagerService.logSurface(this.mWin, "DESTROY", true);
                    }
                    destroySurface();
                    if (!(this.mPendingDestroySurface == null || wtoken == null || ((!wtoken.mAppStopped && !wtoken.mSplitStoped) || !wtoken.hiddenRequested))) {
                        destroyPreservedSurfaceLocked();
                        Slog.v(TAG, "mPendingDestroySurface:" + this.mPendingDestroySurface + " this=" + this);
                    }
                }
                if (!this.mDestroyPreservedSurfaceUponRedraw) {
                    this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
                }
            } catch (RuntimeException e) {
                Slog.w(TAG, "Exception thrown when destroying Window " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e.toString());
            }
            this.mWin.setHasSurface(false);
            if (this.mSurfaceController != null) {
                this.mSurfaceController.setShown(false);
            }
            this.mSurfaceController = null;
            this.mDrawState = 0;
        }
    }

    void destroyDeferredSurfaceLocked() {
        try {
            if (this.mPendingDestroySurface != null) {
                if (SHOW_TRANSACTIONS || SHOW_SURFACE_ALLOC) {
                    WindowManagerService.logSurface(this.mWin, "DESTROY PENDING", true);
                }
                this.mPendingDestroySurface.destroyInTransaction();
                if (!this.mDestroyPreservedSurfaceUponRedraw) {
                    this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
                }
            }
        } catch (RuntimeException e) {
            Slog.w(TAG, "Exception thrown when destroying Window " + this + " surface " + this.mPendingDestroySurface + " session " + this.mSession + ": " + e.toString());
        }
        this.mSurfaceDestroyDeferred = false;
        this.mPendingDestroySurface = null;
    }

    void applyMagnificationSpec(MagnificationSpec spec, Matrix transform) {
        int surfaceInsetLeft = this.mWin.mAttrs.surfaceInsets.left;
        int surfaceInsetTop = this.mWin.mAttrs.surfaceInsets.top;
        if (spec != null && (spec.isNop() ^ 1) != 0) {
            float scale = spec.scale;
            transform.postScale(scale, scale);
            transform.postTranslate(spec.offsetX, spec.offsetY);
            transform.postTranslate(-((((float) surfaceInsetLeft) * scale) - ((float) surfaceInsetLeft)), -((((float) surfaceInsetTop) * scale) - ((float) surfaceInsetTop)));
        }
    }

    void computeShownFrameLocked() {
        boolean selfTransformation = this.mHasLocalTransformation;
        Transformation attachedTransformation = (this.mParentWinAnimator == null || !this.mParentWinAnimator.mHasLocalTransformation) ? null : this.mParentWinAnimator.mTransformation;
        Transformation appTransformation = (this.mAppAnimator == null || !this.mAppAnimator.hasTransformation) ? null : this.mAppAnimator.transformation;
        WindowState wallpaperTarget = this.mWallpaperControllerLocked.getWallpaperTarget();
        if (this.mIsWallpaper && wallpaperTarget != null && this.mService.mAnimateWallpaperWithTarget) {
            WindowStateAnimator wallpaperAnimator = wallpaperTarget.mWinAnimator;
            if (!(!wallpaperAnimator.mHasLocalTransformation || wallpaperAnimator.mAnimation == null || (wallpaperAnimator.mAnimation.getDetachWallpaper() ^ 1) == 0)) {
                attachedTransformation = wallpaperAnimator.mTransformation;
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER && attachedTransformation != null) {
                    Slog.v(TAG, "WP target attached xform: " + attachedTransformation);
                }
            }
            AppWindowAnimator wpAppAnimator = wallpaperTarget.mAppToken == null ? null : wallpaperTarget.mAppToken.mAppAnimator;
            if (!(wpAppAnimator == null || !wpAppAnimator.hasTransformation || wpAppAnimator.animation == null || (wpAppAnimator.animation.getDetachWallpaper() ^ 1) == 0)) {
                appTransformation = wpAppAnimator.transformation;
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER && appTransformation != null) {
                    Slog.v(TAG, "WP target app xform: " + appTransformation);
                }
            }
        }
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(this.mWin.getDisplayId());
        boolean screenAnimation = screenRotationAnimation != null ? screenRotationAnimation.isAnimating() : false;
        this.mHasClipRect = false;
        Rect frame;
        float[] tmpFloats;
        Matrix tmpMatrix;
        MagnificationSpec spec;
        if (selfTransformation || attachedTransformation != null || appTransformation != null || screenAnimation) {
            frame = this.mWin.mFrame;
            tmpFloats = this.mService.mTmpFloats;
            tmpMatrix = this.mWin.mTmpMatrix;
            if (screenAnimation && screenRotationAnimation.isRotating()) {
                float w = (float) frame.width();
                float h = (float) frame.height();
                if (w < 1.0f || h < 1.0f) {
                    tmpMatrix.reset();
                } else {
                    tmpMatrix.setScale((2.0f / w) + 1.0f, (2.0f / h) + 1.0f, w / 2.0f, h / 2.0f);
                }
            } else {
                tmpMatrix.reset();
            }
            tmpMatrix.postScale(this.mWin.mGlobalScale, this.mWin.mGlobalScale);
            if (selfTransformation) {
                tmpMatrix.postConcat(this.mTransformation.getMatrix());
            }
            tmpMatrix.postTranslate((float) (frame.left + this.mWin.mXOffset), (float) (frame.top + this.mWin.mYOffset));
            if (attachedTransformation != null) {
                tmpMatrix.postConcat(attachedTransformation.getMatrix());
            }
            if (appTransformation != null) {
                tmpMatrix.postConcat(appTransformation.getMatrix());
            }
            if (screenAnimation) {
                tmpMatrix.postConcat(screenRotationAnimation.getEnterTransformation().getMatrix());
            }
            spec = getMagnificationSpec();
            if (spec != null) {
                applyMagnificationSpec(spec, tmpMatrix);
            }
            this.mHaveMatrix = true;
            tmpMatrix.getValues(tmpFloats);
            this.mDsDx = tmpFloats[0];
            this.mDtDx = tmpFloats[3];
            this.mDtDy = tmpFloats[1];
            this.mDsDy = tmpFloats[4];
            float x = tmpFloats[2];
            float y = tmpFloats[5];
            this.mWin.mShownPosition.set(Math.round(x), Math.round(y));
            this.mShownAlpha = this.mAlpha;
            if (!(this.mService.mLimitedAlphaCompositing && PixelFormat.formatHasAlpha(this.mWin.mAttrs.format) && (!this.mWin.isIdentityMatrix(this.mDsDx, this.mDtDx, this.mDtDy, this.mDsDy) || x != ((float) frame.left) || y != ((float) frame.top)))) {
                if (selfTransformation) {
                    this.mShownAlpha *= this.mTransformation.getAlpha();
                }
                if (attachedTransformation != null) {
                    this.mShownAlpha *= attachedTransformation.getAlpha();
                }
                if (appTransformation != null) {
                    this.mShownAlpha *= appTransformation.getAlpha();
                    if (appTransformation.hasClipRect()) {
                        this.mClipRect.set(appTransformation.getClipRect());
                        this.mHasClipRect = true;
                        if (this.mWin.layoutInParentFrame()) {
                            this.mClipRect.offset(this.mWin.mContainingFrame.left - this.mWin.mFrame.left, this.mWin.mContainingFrame.top - this.mWin.mFrame.top);
                        }
                    }
                }
                if (screenAnimation) {
                    this.mShownAlpha *= screenRotationAnimation.getEnterTransformation().getAlpha();
                }
            }
            if ((DEBUG_SURFACE_TRACE || WindowManagerService.localLOGV) && (((double) this.mShownAlpha) == 1.0d || ((double) this.mShownAlpha) == 0.0d)) {
                Float valueOf;
                String str = TAG;
                StringBuilder append = new StringBuilder().append("computeShownFrameLocked: Animating ").append(this).append(" mAlpha=").append(this.mAlpha).append(" self=").append(selfTransformation ? Float.valueOf(this.mTransformation.getAlpha()) : "null").append(" attached=").append(attachedTransformation == null ? "null" : Float.valueOf(attachedTransformation.getAlpha())).append(" app=").append(appTransformation == null ? "null" : Float.valueOf(appTransformation.getAlpha())).append(" screen=");
                if (screenAnimation) {
                    valueOf = Float.valueOf(screenRotationAnimation.getEnterTransformation().getAlpha());
                } else {
                    valueOf = "null";
                }
                Slog.v(str, append.append(valueOf).toString());
            }
        } else if ((!this.mIsWallpaper || !this.mService.mRoot.mWallpaperActionPending) && !this.mWin.isDragResizeChanged()) {
            if (WindowManagerService.localLOGV) {
                Slog.v(TAG, "computeShownFrameLocked: " + this + " not attached, mAlpha=" + this.mAlpha);
            }
            spec = getMagnificationSpec();
            if (spec != null) {
                frame = this.mWin.mFrame;
                tmpFloats = this.mService.mTmpFloats;
                tmpMatrix = this.mWin.mTmpMatrix;
                tmpMatrix.setScale(this.mWin.mGlobalScale, this.mWin.mGlobalScale);
                tmpMatrix.postTranslate((float) (frame.left + this.mWin.mXOffset), (float) (frame.top + this.mWin.mYOffset));
                applyMagnificationSpec(spec, tmpMatrix);
                tmpMatrix.getValues(tmpFloats);
                this.mHaveMatrix = true;
                this.mDsDx = tmpFloats[0];
                this.mDtDx = tmpFloats[3];
                this.mDtDy = tmpFloats[1];
                this.mDsDy = tmpFloats[4];
                this.mWin.mShownPosition.set(Math.round(tmpFloats[2]), Math.round(tmpFloats[5]));
                this.mShownAlpha = this.mAlpha;
            } else {
                this.mWin.mShownPosition.set(this.mWin.mFrame.left, this.mWin.mFrame.top);
                if (!(this.mWin.mXOffset == 0 && this.mWin.mYOffset == 0)) {
                    this.mWin.mShownPosition.offset(this.mWin.mXOffset, this.mWin.mYOffset);
                }
                this.mShownAlpha = this.mAlpha;
                this.mHaveMatrix = false;
                this.mDsDx = this.mWin.mGlobalScale;
                this.mDtDx = OppoBrightUtils.MIN_LUX_LIMITI;
                this.mDtDy = OppoBrightUtils.MIN_LUX_LIMITI;
                this.mDsDy = this.mWin.mGlobalScale;
            }
        }
    }

    private MagnificationSpec getMagnificationSpec() {
        if (this.mService.mAccessibilityController == null || this.mWin.getDisplayId() != 0) {
            return null;
        }
        return this.mService.mAccessibilityController.getMagnificationSpecForWindowLocked(this.mWin);
    }

    private boolean useFinalClipRect() {
        if ((isAnimationSet() && resolveStackClip() == 0) || this.mDestroyPreservedSurfaceUponRedraw) {
            return true;
        }
        return this.mWin.inPinnedWorkspace();
    }

    private boolean calculateFinalCrop(Rect finalClipRect) {
        WindowState w = this.mWin;
        DisplayContent displayContent = w.getDisplayContent();
        finalClipRect.setEmpty();
        if (displayContent == null || !shouldCropToStackBounds() || (useFinalClipRect() ^ 1) != 0) {
            return false;
        }
        TaskStack stack = w.getTask().mStack;
        stack.getDimBounds(finalClipRect);
        if (StackId.tasksAreFloating(stack.mStackId)) {
            w.expandForSurfaceInsets(finalClipRect);
        }
        MagnificationSpec spec = getMagnificationSpec();
        if (!(spec == null || (spec.isNop() ^ 1) == 0)) {
            Matrix transform = this.mWin.mTmpMatrix;
            RectF finalCrop = this.mService.mTmpRectF;
            transform.reset();
            transform.postScale(spec.scale, spec.scale);
            transform.postTranslate(-spec.offsetX, -spec.offsetY);
            transform.mapRect(finalCrop);
            finalClipRect.top = (int) finalCrop.top;
            finalClipRect.left = (int) finalCrop.left;
            finalClipRect.right = finalClipRect.right;
            finalClipRect.bottom = finalClipRect.bottom;
        }
        return true;
    }

    private boolean calculateCrop(Rect clipRect) {
        WindowState w = this.mWin;
        DisplayContent displayContent = w.getDisplayContent();
        clipRect.setEmpty();
        if (displayContent == null || w.inPinnedWorkspace() || w.mAttrs.type == 2013) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d(TAG, "Updating crop win=" + w + " mLastCrop=" + this.mLastClipRect);
        }
        w.calculatePolicyCrop(this.mSystemDecorRect);
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d(TAG, "Applying decor to crop win=" + w + " mDecorFrame=" + w.mDecorFrame + " mSystemDecorRect=" + this.mSystemDecorRect);
        }
        Task task = w.getTask();
        boolean fullscreen = !w.fillsDisplay() ? task != null ? task.isFullscreen() : false : true;
        boolean isFreeformResizing = w.isDragResizing() && w.getResizeMode() == 0;
        Rect rect = (!this.mHasClipRect || (fullscreen ^ 1) == 0) ? this.mSystemDecorRect : this.mClipRect;
        clipRect.set(rect);
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d(TAG, "win=" + w + " Initial clip rect: " + clipRect + " mHasClipRect=" + this.mHasClipRect + " fullscreen=" + fullscreen);
        }
        if (isFreeformResizing && (w.isChildWindow() ^ 1) != 0) {
            clipRect.offset(w.mShownPosition.x, w.mShownPosition.y);
        }
        w.expandForSurfaceInsets(clipRect);
        if (this.mHasClipRect && fullscreen) {
            clipRect.intersect(this.mClipRect);
        }
        clipRect.offset(w.mAttrs.surfaceInsets.left, w.mAttrs.surfaceInsets.top);
        if (!useFinalClipRect()) {
            adjustCropToStackBounds(clipRect, isFreeformResizing);
        }
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d(TAG, "win=" + w + " Clip rect after stack adjustment=" + clipRect);
        }
        w.transformClipRectFromScreenToSurfaceSpace(clipRect);
        return true;
    }

    private void applyCrop(Rect clipRect, Rect finalClipRect, boolean recoveringMemory) {
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d(TAG, "applyCrop: win=" + this.mWin + " clipRect=" + clipRect + " finalClipRect=" + finalClipRect);
        }
        if (clipRect == null) {
            this.mSurfaceController.clearCropInTransaction(recoveringMemory);
        } else if (!clipRect.equals(this.mLastClipRect)) {
            this.mLastClipRect.set(clipRect);
            this.mSurfaceController.setCropInTransaction(clipRect, recoveringMemory);
        }
        if (finalClipRect == null) {
            finalClipRect = this.mService.mTmpRect;
            finalClipRect.setEmpty();
        }
        if (!finalClipRect.equals(this.mLastFinalClipRect)) {
            this.mLastFinalClipRect.set(finalClipRect);
            this.mSurfaceController.setFinalCropInTransaction(finalClipRect);
            if (this.mDestroyPreservedSurfaceUponRedraw && this.mPendingDestroySurface != null) {
                this.mPendingDestroySurface.setFinalCropInTransaction(finalClipRect);
            }
        }
    }

    private int resolveStackClip() {
        if (this.mAppAnimator == null || this.mAppAnimator.animation == null) {
            return this.mStackClip;
        }
        return this.mAppAnimator.getStackClip();
    }

    private boolean shouldCropToStackBounds() {
        WindowState w = this.mWin;
        DisplayContent displayContent = w.getDisplayContent();
        if (displayContent != null && (displayContent.isDefaultDisplay ^ 1) != 0) {
            return false;
        }
        Task task = w.getTask();
        if (task == null || (task.cropWindowsToStackBounds() ^ 1) != 0) {
            return false;
        }
        int stackClip = resolveStackClip();
        if (isAnimationSet() && stackClip == 2) {
            return false;
        }
        return true;
    }

    private void adjustCropToStackBounds(Rect clipRect, boolean isFreeformResizing) {
        WindowState w = this.mWin;
        if (shouldCropToStackBounds()) {
            int frameX;
            int frameY;
            TaskStack stack = w.getTask().mStack;
            stack.getDimBounds(this.mTmpStackBounds);
            Rect surfaceInsets = w.getAttrs().surfaceInsets;
            if (isFreeformResizing) {
                frameX = (int) this.mSurfaceController.getX();
            } else {
                frameX = (w.mFrame.left + this.mWin.mXOffset) - surfaceInsets.left;
            }
            if (isFreeformResizing) {
                frameY = (int) this.mSurfaceController.getY();
            } else {
                frameY = (w.mFrame.top + this.mWin.mYOffset) - surfaceInsets.top;
            }
            if (StackId.hasWindowShadow(stack.mStackId) && (StackId.isTaskResizeAllowed(stack.mStackId) ^ 1) != 0) {
                this.mTmpStackBounds.inset(-surfaceInsets.left, -surfaceInsets.top, -surfaceInsets.right, -surfaceInsets.bottom);
            }
            clipRect.left = Math.max(0, Math.max(this.mTmpStackBounds.left, clipRect.left + frameX) - frameX);
            clipRect.top = Math.max(0, Math.max(this.mTmpStackBounds.top, clipRect.top + frameY) - frameY);
            clipRect.right = Math.max(0, Math.min(this.mTmpStackBounds.right, clipRect.right + frameX) - frameX);
            clipRect.bottom = Math.max(0, Math.min(this.mTmpStackBounds.bottom, clipRect.bottom + frameY) - frameY);
        }
    }

    void setSurfaceBoundariesLocked(boolean recoveringMemory) {
        if (this.mSurfaceController != null) {
            WindowState w = this.mWin;
            LayoutParams attrs = this.mWin.getAttrs();
            Task task = w.getTask();
            if (!w.isResizedWhileNotDragResizing() || (w.isGoneForLayoutLw() ^ 1) == 0) {
                this.mTmpSize.set(w.mShownPosition.x, w.mShownPosition.y, 0, 0);
                calculateSurfaceBounds(w, attrs);
                this.mExtraHScale = 1.0f;
                this.mExtraVScale = 1.0f;
                boolean wasForceScaled = this.mForceScaleUntilResize;
                boolean wasSeamlesslyRotated = w.mSeamlesslyRotated;
                if (!w.mRelayoutCalled || w.mInRelayout || w.mSafecenterWin) {
                    this.mSurfaceResized = this.mSurfaceController.setSizeInTransaction(this.mTmpSize.width(), this.mTmpSize.height(), recoveringMemory);
                } else {
                    this.mSurfaceResized = false;
                }
                this.mForceScaleUntilResize = this.mForceScaleUntilResize ? this.mSurfaceResized ^ 1 : false;
                this.mService.markForSeamlessRotation(w, w.mSeamlesslyRotated ? this.mSurfaceResized ^ 1 : false);
                Rect clipRect = null;
                Rect finalClipRect = null;
                if (calculateCrop(this.mTmpClipRect)) {
                    clipRect = this.mTmpClipRect;
                }
                if (calculateFinalCrop(this.mTmpFinalClipRect)) {
                    finalClipRect = this.mTmpFinalClipRect;
                }
                float surfaceWidth = this.mSurfaceController.getWidth();
                float surfaceHeight = this.mSurfaceController.getHeight();
                if (isForceScaled()) {
                    float surfaceContentWidth = surfaceWidth - ((float) (attrs.surfaceInsets.left + attrs.surfaceInsets.right));
                    float surfaceContentHeight = surfaceHeight - ((float) (attrs.surfaceInsets.top + attrs.surfaceInsets.bottom));
                    if (!this.mForceScaleUntilResize) {
                        this.mSurfaceController.forceScaleableInTransaction(true);
                    }
                    int posX = this.mTmpSize.left;
                    int posY = this.mTmpSize.top;
                    task.mStack.getDimBounds(this.mTmpStackBounds);
                    boolean allowStretching = false;
                    task.mStack.getFinalAnimationSourceHintBounds(this.mTmpSourceBounds);
                    if (this.mTmpSourceBounds.isEmpty() && ((this.mWin.mLastRelayoutContentInsets.width() > 0 || this.mWin.mLastRelayoutContentInsets.height() > 0) && (task.mStack.lastAnimatingBoundsWasToFullscreen() ^ 1) != 0)) {
                        this.mTmpSourceBounds.set(task.mStack.mPreAnimationBounds);
                        this.mTmpSourceBounds.inset(this.mWin.mLastRelayoutContentInsets);
                        allowStretching = true;
                    }
                    if (this.mTmpSourceBounds.isEmpty()) {
                        this.mExtraHScale = ((float) this.mTmpStackBounds.width()) / surfaceContentWidth;
                        this.mExtraVScale = ((float) this.mTmpStackBounds.height()) / surfaceContentHeight;
                        clipRect = null;
                        finalClipRect = null;
                    } else {
                        task.mStack.getFinalAnimationBounds(this.mTmpAnimatingBounds);
                        float initialWidth = (float) this.mTmpSourceBounds.width();
                        float tw = (surfaceContentWidth - ((float) this.mTmpStackBounds.width())) / (surfaceContentWidth - ((float) this.mTmpAnimatingBounds.width()));
                        float th = tw;
                        this.mExtraHScale = (((((float) this.mTmpAnimatingBounds.width()) - initialWidth) * tw) + initialWidth) / initialWidth;
                        if (allowStretching) {
                            float initialHeight = (float) this.mTmpSourceBounds.height();
                            th = (surfaceContentHeight - ((float) this.mTmpStackBounds.height())) / (surfaceContentHeight - ((float) this.mTmpAnimatingBounds.height()));
                            this.mExtraVScale = (((((float) this.mTmpAnimatingBounds.height()) - initialHeight) * tw) + initialHeight) / initialHeight;
                        } else {
                            this.mExtraVScale = this.mExtraHScale;
                        }
                        posX -= (int) ((this.mExtraHScale * tw) * ((float) this.mTmpSourceBounds.left));
                        posY -= (int) ((this.mExtraVScale * th) * ((float) this.mTmpSourceBounds.top));
                        clipRect = null;
                        finalClipRect = this.mTmpStackBounds;
                    }
                    this.mSurfaceController.setPositionInTransaction((float) Math.floor((double) ((int) (((float) (posX - ((int) (((float) attrs.x) * (1.0f - this.mExtraHScale))))) + (((float) attrs.surfaceInsets.left) * (1.0f - this.mExtraHScale))))), (float) Math.floor((double) ((int) (((float) (posY - ((int) (((float) attrs.y) * (1.0f - this.mExtraVScale))))) + (((float) attrs.surfaceInsets.top) * (1.0f - this.mExtraVScale))))), recoveringMemory);
                    this.mForceScaleUntilResize = true;
                } else if (!w.mSeamlesslyRotated) {
                    this.mSurfaceController.setPositionInTransaction((float) this.mTmpSize.left, (float) this.mTmpSize.top, recoveringMemory);
                }
                if ((wasForceScaled && (this.mForceScaleUntilResize ^ 1) != 0) || (wasSeamlesslyRotated && (w.mSeamlesslyRotated ^ 1) != 0)) {
                    this.mSurfaceController.setGeometryAppliesWithResizeInTransaction(true);
                    this.mSurfaceController.forceScaleableInTransaction(false);
                }
                if (!w.mSeamlesslyRotated) {
                    applyCrop(clipRect, finalClipRect, recoveringMemory);
                    this.mSurfaceController.setMatrixInTransaction((this.mDsDx * w.mHScale) * this.mExtraHScale, (this.mDtDx * w.mVScale) * this.mExtraVScale, (this.mDtDy * w.mHScale) * this.mExtraHScale, (this.mDsDy * w.mVScale) * this.mExtraVScale, recoveringMemory);
                }
                if (this.mSurfaceResized) {
                    this.mReportSurfaceResized = true;
                    this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 4);
                    w.applyDimLayerIfNeeded();
                }
            }
        }
    }

    void getContainerRect(Rect rect) {
        Task task = this.mWin.getTask();
        if (task != null) {
            task.getDimBounds(rect);
            return;
        }
        rect.bottom = 0;
        rect.right = 0;
        rect.top = 0;
        rect.left = 0;
    }

    void prepareSurfaceLocked(boolean recoveringMemory) {
        WindowState w = this.mWin;
        if (!hasSurface()) {
            if (w.getOrientationChanging() && w.isGoneForLayoutLw()) {
                if (DEBUG_ORIENTATION) {
                    Slog.v(TAG, "Orientation change skips hidden " + w);
                }
                w.setOrientationChanging(false);
            }
        } else if (!isWaitingForOpening()) {
            WindowAnimator windowAnimator;
            boolean displayed = false;
            computeShownFrameLocked();
            setSurfaceBoundariesLocked(recoveringMemory);
            if (this.mIsWallpaper && (this.mWin.mWallpaperVisible ^ 1) != 0) {
                hide("prepareSurfaceLocked");
            } else if (this.mWin.mDestroying && this.mWin.isDragResizing() && (this.mWin.isDockStackVisible() ^ 1) != 0) {
                if (SHOW_TRANSACTIONS) {
                    Slog.v(TAG, "prepareSurfaceLocked change-hidden " + w);
                }
                if (this.mSurfaceController != null) {
                    this.mSurfaceController.hideInTransaction("prepareSurfaceLocked_splitScreen");
                }
            } else if (w.isParentWindowHidden() || (w.isOnScreen() ^ 1) != 0) {
                hide("prepareSurfaceLocked");
                this.mWallpaperControllerLocked.hideWallpapers(w);
                if (w.getOrientationChanging() && w.isGoneForLayoutLw()) {
                    w.setOrientationChanging(false);
                    if (DEBUG_ORIENTATION) {
                        Slog.v(TAG, "Orientation change skips hidden " + w);
                    }
                }
            } else if (this.mLastLayer == this.mAnimLayer && this.mLastAlpha == this.mShownAlpha && this.mLastDsDx == this.mDsDx && this.mLastDtDx == this.mDtDx && this.mLastDsDy == this.mDsDy && this.mLastDtDy == this.mDtDy && w.mLastHScale == w.mHScale && w.mLastVScale == w.mVScale && !this.mLastHidden) {
                if (DEBUG_ANIM && isAnimationSet()) {
                    Slog.v(TAG, "prepareSurface: No changes in animation for " + this);
                }
                displayed = true;
            } else {
                displayed = true;
                this.mLastAlpha = this.mShownAlpha;
                this.mLastLayer = this.mAnimLayer;
                this.mLastDsDx = this.mDsDx;
                this.mLastDtDx = this.mDtDx;
                this.mLastDsDy = this.mDsDy;
                this.mLastDtDy = this.mDtDy;
                w.mLastHScale = w.mHScale;
                w.mLastVScale = w.mVScale;
                if (SHOW_TRANSACTIONS) {
                    WindowManagerService.logSurface(w, "controller=" + this.mSurfaceController + "alpha=" + this.mShownAlpha + " layer=" + this.mAnimLayer + " matrix=[" + this.mDsDx + "*" + w.mHScale + "," + this.mDtDx + "*" + w.mVScale + "][" + this.mDtDy + "*" + w.mHScale + "," + this.mDsDy + "*" + w.mVScale + "]", false);
                }
                boolean prepared = this.mSurfaceController.prepareToShowInTransaction(this.mShownAlpha, (this.mDsDx * w.mHScale) * this.mExtraHScale, (this.mDtDx * w.mVScale) * this.mExtraVScale, (this.mDtDy * w.mHScale) * this.mExtraHScale, (this.mDsDy * w.mVScale) * this.mExtraVScale, recoveringMemory);
                this.mSurfaceController.setLayer(this.mAnimLayer);
                if (this.mService.mIsShutdown) {
                    Slog.v(TAG, "Shut down is true,not show win " + this.mWin);
                }
                if (prepared && this.mDrawState == 4 && (this.mService.mIsShutdown ^ 1) != 0) {
                    if (this.mLastHidden) {
                        if (showSurfaceRobustlyLocked()) {
                            markPreservedSurfaceForDestroy();
                            this.mAnimator.requestRemovalOfReplacedWindows(w);
                            this.mLastHidden = false;
                            if (this.mIsWallpaper) {
                                w.dispatchWallpaperVisibility(true);
                            }
                            this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 8);
                        } else {
                            w.setOrientationChanging(false);
                        }
                    }
                    if (this.mWin.mTurnOnScreen && (this.mWin.mAppToken == null || this.mWin.mAppToken.canTurnScreenOn())) {
                        if (DEBUG_VISIBILITY) {
                            Slog.v(TAG, "Show surface turning screen on: " + this.mWin);
                        }
                        this.mWin.mTurnOnScreen = false;
                        if (this.mWin.mAppToken != null) {
                            this.mWin.mAppToken.setCanTurnScreenOn(false);
                        }
                        if (!(this.mService.mPowerManager.isInteractive() && this.mService.mPowerManager.getScreenState() == 1)) {
                            windowAnimator = this.mAnimator;
                            windowAnimator.mBulkUpdateParams |= 16;
                        }
                    }
                }
                if (hasSurface()) {
                    w.mToken.hasVisible = true;
                }
            }
            if (w.getOrientationChanging()) {
                if (w.isDrawnLw()) {
                    w.setOrientationChanging(false);
                    if (DEBUG_ORIENTATION) {
                        Slog.v(TAG, "Orientation change complete in " + w);
                    }
                } else {
                    windowAnimator = this.mAnimator;
                    windowAnimator.mBulkUpdateParams &= -9;
                    this.mAnimator.mLastWindowFreezeSource = w;
                    if (DEBUG_ORIENTATION) {
                        Slog.v(TAG, "Orientation continue waiting for draw in " + w);
                    }
                }
            }
            if (displayed) {
                w.mToken.hasVisible = true;
            }
        }
    }

    void setTransparentRegionHintLocked(Region region) {
        if (this.mSurfaceController == null) {
            Slog.w(TAG, "setTransparentRegionHint: null mSurface after mHasSurface true");
        } else {
            this.mSurfaceController.setTransparentRegionHint(region);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void setWallpaperOffset(Point shownPosition) {
        LayoutParams attrs = this.mWin.getAttrs();
        int left = shownPosition.x - attrs.surfaceInsets.left;
        int top = shownPosition.y - attrs.surfaceInsets.top;
        try {
            if (SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, ">>> OPEN TRANSACTION setWallpaperOffset");
            }
            this.mService.openSurfaceTransaction();
            this.mSurfaceController.setPositionInTransaction((float) (this.mWin.mFrame.left + left), (float) (this.mWin.mFrame.top + top), false);
            applyCrop(null, null, false);
            this.mService.closeSurfaceTransaction();
            if (SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION setWallpaperOffset");
            }
        } catch (RuntimeException e) {
            Slog.w(TAG, "Error positioning surface of " + this.mWin + " pos=(" + left + "," + top + ")", e);
            this.mService.closeSurfaceTransaction();
            if (SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION setWallpaperOffset");
            }
        } catch (Throwable th) {
            this.mService.closeSurfaceTransaction();
            if (SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION setWallpaperOffset");
            }
        }
    }

    boolean tryChangeFormatInPlaceLocked() {
        if (this.mSurfaceController == null) {
            return false;
        }
        LayoutParams attrs = this.mWin.getAttrs();
        if (((attrs.flags & 16777216) != 0 ? -3 : attrs.format) != this.mSurfaceFormat) {
            return false;
        }
        setOpaqueLocked(PixelFormat.formatHasAlpha(attrs.format) ^ 1);
        return true;
    }

    void setOpaqueLocked(boolean isOpaque) {
        if (this.mSurfaceController != null) {
            this.mSurfaceController.setOpaque(isOpaque);
        }
    }

    void setSecureLocked(boolean isSecure) {
        if (this.mSurfaceController != null) {
            this.mSurfaceController.setSecure(isSecure);
        }
    }

    private boolean showSurfaceRobustlyLocked() {
        Task task = this.mWin.getTask();
        if (task != null && StackId.windowsAreScaleable(task.mStack.mStackId)) {
            this.mSurfaceController.forceScaleableInTransaction(true);
        }
        if (this.mSurfaceController.showRobustlyInTransaction()) {
            return true;
        }
        return false;
    }

    void applyEnterAnimationLocked() {
        if (!this.mWin.mSkipEnterAnimationForSeamlessReplacement) {
            int transit;
            if (this.mEnterAnimationPending) {
                this.mEnterAnimationPending = false;
                transit = 1;
            } else {
                transit = 3;
            }
            applyAnimationLocked(transit, true);
            if (this.mService.mAccessibilityController != null && this.mWin.getDisplayId() == 0) {
                this.mService.mAccessibilityController.onWindowTransitionLocked(this.mWin, transit);
            }
        }
    }

    boolean applyAnimationLocked(int transit, boolean isEntrance) {
        boolean z = true;
        if (this.mLocalAnimating && this.mAnimationIsEntrance == isEntrance) {
            return true;
        }
        Trace.traceBegin(32, "WSA#applyAnimationLocked");
        if (this.mWin.mToken.okToAnimate()) {
            int anim = this.mPolicy.selectAnimationLw(this.mWin, transit);
            int attr = -1;
            Animation a = null;
            if (anim == 0) {
                switch (transit) {
                    case 1:
                        attr = 0;
                        break;
                    case 2:
                        attr = 1;
                        break;
                    case 3:
                        attr = 2;
                        break;
                    case 4:
                        attr = 3;
                        break;
                }
                if (attr >= 0) {
                    a = this.mService.mAppTransition.loadAnimationAttr(this.mWin.mAttrs, attr);
                }
            } else if (anim != -1) {
                a = AnimationUtils.loadAnimation(this.mContext, anim);
            } else {
                a = null;
            }
            if (DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation: win=" + this + " anim=" + anim + " attr=0x" + Integer.toHexString(attr) + " a=" + a + " transit=" + transit + " isEntrance=" + isEntrance + " Callers " + Debug.getCallers(3));
            }
            if (a != null) {
                if (DEBUG_ANIM) {
                    WindowManagerService.logWithStack(TAG, "Loaded animation " + a + " for " + this);
                }
                setAnimation(a);
                this.mAnimationIsEntrance = isEntrance;
            }
        } else {
            clearAnimation();
        }
        Trace.traceEnd(32);
        if (this.mWin.mAttrs.type == 2011) {
            this.mWin.getDisplayContent().adjustForImeIfNeeded();
            if (isEntrance) {
                this.mWin.setDisplayLayoutNeeded();
                this.mService.mWindowPlacerLocked.requestTraversal();
            }
        }
        if (this.mAnimation == null) {
            z = false;
        }
        return z;
    }

    private void applyFadeoutDuringKeyguardExitAnimation() {
        long startTime = this.mAnimation.getStartTime();
        long duration = this.mAnimation.getDuration();
        long elapsed = this.mLastAnimationTime - startTime;
        long fadeDuration = duration - elapsed;
        if (fadeDuration > 0) {
            AnimationSet newAnimation = new AnimationSet(false);
            newAnimation.setDuration(duration);
            newAnimation.setStartTime(startTime);
            newAnimation.addAnimation(this.mAnimation);
            Animation fadeOut = AnimationUtils.loadAnimation(this.mContext, 17432593);
            fadeOut.setDuration(fadeDuration);
            fadeOut.setStartOffset(elapsed);
            newAnimation.addAnimation(fadeOut);
            newAnimation.initialize(this.mWin.mFrame.width(), this.mWin.mFrame.height(), this.mAnimDx, this.mAnimDy);
            this.mAnimation = newAnimation;
        }
    }

    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        if (this.mAnimating || this.mLocalAnimating || this.mAnimationIsEntrance || this.mAnimation != null) {
            pw.print(prefix);
            pw.print("mAnimating=");
            pw.print(this.mAnimating);
            pw.print(" mLocalAnimating=");
            pw.print(this.mLocalAnimating);
            pw.print(" mAnimationIsEntrance=");
            pw.print(this.mAnimationIsEntrance);
            pw.print(" mAnimation=");
            pw.print(this.mAnimation);
            pw.print(" mStackClip=");
            pw.println(this.mStackClip);
        }
        if (this.mHasTransformation || this.mHasLocalTransformation) {
            pw.print(prefix);
            pw.print("XForm: has=");
            pw.print(this.mHasTransformation);
            pw.print(" hasLocal=");
            pw.print(this.mHasLocalTransformation);
            pw.print(" ");
            this.mTransformation.printShortString(pw);
            pw.println();
        }
        if (this.mSurfaceController != null) {
            this.mSurfaceController.dump(pw, prefix, dumpAll);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mDrawState=");
            pw.print(drawStateToString());
            pw.print(prefix);
            pw.print(" mLastHidden=");
            pw.println(this.mLastHidden);
            pw.print(prefix);
            pw.print("mSystemDecorRect=");
            this.mSystemDecorRect.printShortString(pw);
            pw.print(" last=");
            this.mLastSystemDecorRect.printShortString(pw);
            pw.print(" mHasClipRect=");
            pw.print(this.mHasClipRect);
            pw.print(" mLastClipRect=");
            this.mLastClipRect.printShortString(pw);
            if (!this.mLastFinalClipRect.isEmpty()) {
                pw.print(" mLastFinalClipRect=");
                this.mLastFinalClipRect.printShortString(pw);
            }
            pw.println();
        }
        if (this.mPendingDestroySurface != null) {
            pw.print(prefix);
            pw.print("mPendingDestroySurface=");
            pw.println(this.mPendingDestroySurface);
        }
        if (this.mSurfaceResized || this.mSurfaceDestroyDeferred) {
            pw.print(prefix);
            pw.print("mSurfaceResized=");
            pw.print(this.mSurfaceResized);
            pw.print(" mSurfaceDestroyDeferred=");
            pw.println(this.mSurfaceDestroyDeferred);
        }
        if (!(this.mShownAlpha == 1.0f && this.mAlpha == 1.0f && this.mLastAlpha == 1.0f)) {
            pw.print(prefix);
            pw.print("mShownAlpha=");
            pw.print(this.mShownAlpha);
            pw.print(" mAlpha=");
            pw.print(this.mAlpha);
            pw.print(" mLastAlpha=");
            pw.println(this.mLastAlpha);
        }
        if (this.mHaveMatrix || this.mWin.mGlobalScale != 1.0f) {
            pw.print(prefix);
            pw.print("mGlobalScale=");
            pw.print(this.mWin.mGlobalScale);
            pw.print(" mDsDx=");
            pw.print(this.mDsDx);
            pw.print(" mDtDx=");
            pw.print(this.mDtDx);
            pw.print(" mDtDy=");
            pw.print(this.mDtDy);
            pw.print(" mDsDy=");
            pw.println(this.mDsDy);
        }
        if (this.mAnimationStartDelayed) {
            pw.print(prefix);
            pw.print("mAnimationStartDelayed=");
            pw.print(this.mAnimationStartDelayed);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("WindowStateAnimator{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.mWin.mAttrs.getTitle());
        sb.append('}');
        return sb.toString();
    }

    void reclaimSomeSurfaceMemory(String operation, boolean secure) {
        this.mService.mRoot.reclaimSomeSurfaceMemory(this, operation, secure);
    }

    boolean getShown() {
        if (this.mSurfaceController != null) {
            return this.mSurfaceController.getShown();
        }
        return false;
    }

    void destroySurface() {
        try {
            if (this.mSurfaceController != null) {
                this.mSurfaceController.destroyInTransaction();
            }
            this.mWin.setHasSurface(false);
            this.mSurfaceController = null;
        } catch (RuntimeException e) {
            Slog.w(TAG, "Exception thrown when destroying surface " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e);
            this.mWin.setHasSurface(false);
            this.mSurfaceController = null;
        } catch (Throwable th) {
            this.mWin.setHasSurface(false);
            this.mSurfaceController = null;
            this.mDrawState = 0;
        }
        this.mDrawState = 0;
    }

    void setMoveAnimation(int left, int top) {
        setAnimation(AnimationUtils.loadAnimation(this.mContext, 17432753));
        this.mAnimDx = this.mWin.mLastFrame.left - left;
        this.mAnimDy = this.mWin.mLastFrame.top - top;
        this.mAnimateMove = true;
    }

    void deferTransactionUntilParentFrame(long frameNumber) {
        if (this.mWin.isChildWindow()) {
            this.mSurfaceController.deferTransactionUntil(this.mWin.getParentWindow().mWinAnimator.mSurfaceController.getHandle(), frameNumber);
        }
    }

    private long getAnimationFrameTime(Animation animation, long currentTime) {
        if (!this.mAnimationStartDelayed) {
            return currentTime;
        }
        animation.setStartTime(currentTime);
        return 1 + currentTime;
    }

    void startDelayingAnimationStart() {
        this.mAnimationStartDelayed = true;
    }

    void endDelayingAnimationStart() {
        this.mAnimationStartDelayed = false;
    }

    void seamlesslyRotateWindow(int oldRotation, int newRotation) {
        WindowState w = this.mWin;
        if (w.isVisibleNow() && !w.mIsWallpaper) {
            Rect cropRect = this.mService.mTmpRect;
            Rect displayRect = this.mService.mTmpRect2;
            RectF frameRect = this.mService.mTmpRectF;
            Matrix transform = this.mService.mTmpTransform;
            float x = (float) w.mFrame.left;
            float y = (float) w.mFrame.top;
            float width = (float) w.mFrame.width();
            float height = (float) w.mFrame.height();
            this.mService.getDefaultDisplayContentLocked().getLogicalDisplayRect(displayRect);
            DisplayContent.createRotationMatrix(DisplayContent.deltaRotation(newRotation, oldRotation), x, y, (float) displayRect.width(), (float) displayRect.height(), transform);
            this.mService.markForSeamlessRotation(w, true);
            transform.getValues(this.mService.mTmpFloats);
            float DsDx = this.mService.mTmpFloats[0];
            float DtDx = this.mService.mTmpFloats[3];
            float DtDy = this.mService.mTmpFloats[1];
            float DsDy = this.mService.mTmpFloats[4];
            this.mSurfaceController.setPositionInTransaction(this.mService.mTmpFloats[2], this.mService.mTmpFloats[5], false);
            this.mSurfaceController.setMatrixInTransaction(w.mHScale * DsDx, w.mVScale * DtDx, w.mHScale * DtDy, w.mVScale * DsDy, false);
        }
    }

    void enableSurfaceTrace(FileDescriptor fd) {
        if (this.mSurfaceController != null) {
            this.mSurfaceController.installRemoteTrace(fd);
        }
    }

    void disableSurfaceTrace() {
        if (this.mSurfaceController != null) {
            try {
                this.mSurfaceController.removeRemoteTrace();
            } catch (ClassCastException e) {
                Slog.e(TAG, "Disable surface trace for " + this + " but its not enabled");
            }
        }
    }

    boolean isForceScaled() {
        Task task = this.mWin.getTask();
        if (task == null || !task.mStack.isForceScaled()) {
            return this.mForceScaleUntilResize;
        }
        return true;
    }

    void detachChildren() {
        if (this.mSurfaceController != null) {
            this.mSurfaceController.detachChildren();
        }
    }
}
