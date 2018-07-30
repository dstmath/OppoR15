package com.android.server.wm;

import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.display.OppoBrightUtils;
import com.android.server.wm.-$Lambda$lpBUCbECLvWBIi8CcvaEY5AB7jM.AnonymousClass1;
import java.io.PrintWriter;
import java.util.ArrayList;

class WallpaperController {
    private static final String TAG = "WindowManager";
    private static final int WALLPAPER_DRAW_NORMAL = 0;
    private static final int WALLPAPER_DRAW_PENDING = 1;
    private static final long WALLPAPER_DRAW_PENDING_TIMEOUT_DURATION = 500;
    private static final int WALLPAPER_DRAW_TIMEOUT = 2;
    private static final long WALLPAPER_TIMEOUT = 150;
    private static final long WALLPAPER_TIMEOUT_RECOVERY = 10000;
    WindowState mDeferredHideWallpaper = null;
    private final FindWallpaperTargetResult mFindResults = new FindWallpaperTargetResult();
    private final ToBooleanFunction<WindowState> mFindWallpaperTargetFunction = new -$Lambda$lpBUCbECLvWBIi8CcvaEY5AB7jM((byte) 1, this);
    private int mLastWallpaperDisplayOffsetX = Integer.MIN_VALUE;
    private int mLastWallpaperDisplayOffsetY = Integer.MIN_VALUE;
    private long mLastWallpaperTimeoutTime;
    private float mLastWallpaperX = -1.0f;
    private float mLastWallpaperXStep = -1.0f;
    private float mLastWallpaperY = -1.0f;
    private float mLastWallpaperYStep = -1.0f;
    private WindowState mPrevWallpaperTarget = null;
    private WindowManagerService mService;
    private WindowState mWaitingOnWallpaper;
    private int mWallpaperAnimLayerAdjustment;
    private int mWallpaperDrawState = 0;
    private WindowState mWallpaperTarget = null;
    private final ArrayList<WallpaperWindowToken> mWallpaperTokens = new ArrayList();

    private static final class FindWallpaperTargetResult {
        boolean resetTopWallpaper;
        WindowState topWallpaper;
        boolean useTopWallpaperAsTarget;
        WindowState wallpaperTarget;

        /* synthetic */ FindWallpaperTargetResult(FindWallpaperTargetResult -this0) {
            this();
        }

        private FindWallpaperTargetResult() {
            this.topWallpaper = null;
            this.useTopWallpaperAsTarget = false;
            this.wallpaperTarget = null;
            this.resetTopWallpaper = false;
        }

        void setTopWallpaper(WindowState win) {
            this.topWallpaper = win;
        }

        void setWallpaperTarget(WindowState win) {
            this.wallpaperTarget = win;
        }

        void setUseTopWallpaperAsTarget(boolean topWallpaperAsTarget) {
            this.useTopWallpaperAsTarget = topWallpaperAsTarget;
        }

        void reset() {
            this.topWallpaper = null;
            this.wallpaperTarget = null;
            this.useTopWallpaperAsTarget = false;
            this.resetTopWallpaper = false;
        }
    }

    /* synthetic */ boolean lambda$-com_android_server_wm_WallpaperController_4687(WindowState w) {
        WindowAnimator winAnimator = this.mService.mAnimator;
        if (w.mAttrs.type == 2013) {
            if (this.mFindResults.topWallpaper == null || this.mFindResults.resetTopWallpaper) {
                this.mFindResults.setTopWallpaper(w);
                this.mFindResults.resetTopWallpaper = false;
            }
            return false;
        }
        this.mFindResults.resetTopWallpaper = true;
        if (w == winAnimator.mWindowDetachedWallpaper || w.mAppToken == null || !w.mAppToken.hidden || w.mAppToken.mAppAnimator.animation != null) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v(TAG, "Win " + w + ": isOnScreen=" + w.isOnScreen() + " mDrawState=" + w.mWinAnimator.mDrawState);
            }
            if (w.mWillReplaceWindow && this.mWallpaperTarget == null && (this.mFindResults.useTopWallpaperAsTarget ^ 1) != 0) {
                this.mFindResults.setUseTopWallpaperAsTarget(true);
            }
            boolean keyguardGoingAwayWithWallpaper = (w.mAppToken == null || !AppTransition.isKeyguardGoingAwayTransit(w.mAppToken.mAppAnimator.getTransit())) ? false : (w.mAppToken.mAppAnimator.getTransitFlags() & 4) != 0;
            boolean needsShowWhenLockedWallpaper = false;
            if ((w.mAttrs.flags & DumpState.DUMP_FROZEN) != 0 && this.mService.mPolicy.isKeyguardLocked() && this.mService.mPolicy.isKeyguardOccluded()) {
                needsShowWhenLockedWallpaper = isFullscreen(w.mAttrs) ? w.mAppToken != null ? w.mAppToken.fillsParent() ^ 1 : false : true;
            }
            if (keyguardGoingAwayWithWallpaper || needsShowWhenLockedWallpaper) {
                this.mFindResults.setUseTopWallpaperAsTarget(true);
            }
            if (((w.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) && w.isOnScreen() && (this.mWallpaperTarget == w || w.isDrawFinishedLw())) {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v(TAG, "Found wallpaper target: " + w);
                }
                this.mFindResults.setWallpaperTarget(w);
                if (w == this.mWallpaperTarget && w.mWinAnimator.isAnimationSet() && WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v(TAG, "Win " + w + ": token animating, looking behind.");
                }
                return true;
            }
            if (w == winAnimator.mWindowDetachedWallpaper) {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    Slog.v(TAG, "Found animating detached wallpaper target win: " + w);
                }
                this.mFindResults.setUseTopWallpaperAsTarget(true);
            }
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v(TAG, "Skipping hidden and not animating token: " + w);
        }
        return false;
    }

    public WallpaperController(WindowManagerService service) {
        this.mService = service;
    }

    WindowState getWallpaperTarget() {
        return this.mWallpaperTarget;
    }

    boolean isWallpaperTarget(WindowState win) {
        return win == this.mWallpaperTarget;
    }

    boolean isBelowWallpaperTarget(WindowState win) {
        return this.mWallpaperTarget != null && this.mWallpaperTarget.mLayer >= win.mBaseLayer;
    }

    boolean isWallpaperVisible() {
        return isWallpaperVisible(this.mWallpaperTarget);
    }

    void startWallpaperAnimation(Animation a) {
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            ((WallpaperWindowToken) this.mWallpaperTokens.get(curTokenNdx)).startAnimation(a);
        }
    }

    private boolean isWallpaperVisible(WindowState wallpaperTarget) {
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Object obj;
            String str = TAG;
            StringBuilder append = new StringBuilder().append("Wallpaper vis: target ").append(wallpaperTarget).append(", obscured=").append(wallpaperTarget != null ? Boolean.toString(wallpaperTarget.mObscured) : "??").append(" anim=");
            if (wallpaperTarget == null || wallpaperTarget.mAppToken == null) {
                obj = null;
            } else {
                obj = wallpaperTarget.mAppToken.mAppAnimator.animation;
            }
            Slog.v(str, append.append(obj).append(" prev=").append(this.mPrevWallpaperTarget).toString());
        }
        if (wallpaperTarget == null || (wallpaperTarget.mObscured && (wallpaperTarget.mAppToken == null || wallpaperTarget.mAppToken.mAppAnimator.animation == null))) {
            return this.mPrevWallpaperTarget != null;
        } else {
            return true;
        }
    }

    boolean isWallpaperTargetAnimating() {
        if (this.mWallpaperTarget == null || !this.mWallpaperTarget.mWinAnimator.isAnimationSet()) {
            return false;
        }
        return this.mWallpaperTarget.mWinAnimator.isDummyAnimation() ^ 1;
    }

    void updateWallpaperVisibility() {
        boolean visible = isWallpaperVisible(this.mWallpaperTarget);
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            ((WallpaperWindowToken) this.mWallpaperTokens.get(curTokenNdx)).updateWallpaperVisibility(visible);
        }
    }

    void hideDeferredWallpapersIfNeeded() {
        if (this.mDeferredHideWallpaper != null) {
            hideWallpapers(this.mDeferredHideWallpaper);
            this.mDeferredHideWallpaper = null;
        }
    }

    void hideWallpapers(WindowState winGoingAway) {
        if (this.mWallpaperTarget != null && (this.mWallpaperTarget != winGoingAway || this.mPrevWallpaperTarget != null)) {
            return;
        }
        if (this.mService.mAppTransition.isRunning()) {
            this.mDeferredHideWallpaper = winGoingAway;
            return;
        }
        boolean wasDeferred = this.mDeferredHideWallpaper == winGoingAway;
        for (int i = this.mWallpaperTokens.size() - 1; i >= 0; i--) {
            WallpaperWindowToken token = (WallpaperWindowToken) this.mWallpaperTokens.get(i);
            token.hideWallpaperToken(wasDeferred, "hideWallpapers");
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT && (token.hidden ^ 1) != 0) {
                Slog.d(TAG, "Hiding wallpaper " + token + " from " + winGoingAway + " target=" + this.mWallpaperTarget + " prev=" + this.mPrevWallpaperTarget + "\n" + Debug.getCallers(5, "  "));
            }
        }
    }

    boolean updateWallpaperOffset(WindowState wallpaperWin, int dw, int dh, boolean sync) {
        boolean rawChanged = false;
        float wpx = this.mLastWallpaperX >= OppoBrightUtils.MIN_LUX_LIMITI ? this.mLastWallpaperX : wallpaperWin.isRtl() ? 1.0f : OppoBrightUtils.MIN_LUX_LIMITI;
        float wpxs = this.mLastWallpaperXStep >= OppoBrightUtils.MIN_LUX_LIMITI ? this.mLastWallpaperXStep : -1.0f;
        int availw = (wallpaperWin.mFrame.right - wallpaperWin.mFrame.left) - dw;
        int offset = availw > 0 ? -((int) ((((float) availw) * wpx) + 0.5f)) : 0;
        if (this.mLastWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
            offset += this.mLastWallpaperDisplayOffsetX;
        }
        boolean changed = wallpaperWin.mXOffset != offset;
        if (changed) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v(TAG, "Update wallpaper " + wallpaperWin + " x: " + offset);
            }
            wallpaperWin.mXOffset = offset;
        }
        if (!(wallpaperWin.mWallpaperX == wpx && wallpaperWin.mWallpaperXStep == wpxs)) {
            wallpaperWin.mWallpaperX = wpx;
            wallpaperWin.mWallpaperXStep = wpxs;
            rawChanged = true;
        }
        float wpy = this.mLastWallpaperY >= OppoBrightUtils.MIN_LUX_LIMITI ? this.mLastWallpaperY : 0.5f;
        float wpys = this.mLastWallpaperYStep >= OppoBrightUtils.MIN_LUX_LIMITI ? this.mLastWallpaperYStep : -1.0f;
        int availh = (wallpaperWin.mFrame.bottom - wallpaperWin.mFrame.top) - dh;
        offset = availh > 0 ? -((int) ((((float) availh) * wpy) + 0.5f)) : 0;
        if (this.mLastWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
            offset += this.mLastWallpaperDisplayOffsetY;
        }
        if (wallpaperWin.mYOffset != offset) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v(TAG, "Update wallpaper " + wallpaperWin + " y: " + offset);
            }
            changed = true;
            wallpaperWin.mYOffset = offset;
        }
        if (!(wallpaperWin.mWallpaperY == wpy && wallpaperWin.mWallpaperYStep == wpys)) {
            wallpaperWin.mWallpaperY = wpy;
            wallpaperWin.mWallpaperYStep = wpys;
            rawChanged = true;
        }
        if (rawChanged && (wallpaperWin.mAttrs.privateFlags & 4) != 0) {
            try {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v(TAG, "Report new wp offset " + wallpaperWin + " x=" + wallpaperWin.mWallpaperX + " y=" + wallpaperWin.mWallpaperY);
                }
                if (sync) {
                    this.mWaitingOnWallpaper = wallpaperWin;
                }
                wallpaperWin.mClient.dispatchWallpaperOffsets(wallpaperWin.mWallpaperX, wallpaperWin.mWallpaperY, wallpaperWin.mWallpaperXStep, wallpaperWin.mWallpaperYStep, sync);
                if (sync && this.mWaitingOnWallpaper != null) {
                    long start = SystemClock.uptimeMillis();
                    if (this.mLastWallpaperTimeoutTime + 10000 < start) {
                        try {
                            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                                Slog.v(TAG, "Waiting for offset complete...");
                            }
                            this.mService.mWindowMap.wait(WALLPAPER_TIMEOUT);
                        } catch (InterruptedException e) {
                        }
                        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                            Slog.v(TAG, "Offset complete!");
                        }
                        if (WALLPAPER_TIMEOUT + start < SystemClock.uptimeMillis()) {
                            Slog.i(TAG, "Timeout waiting for wallpaper to offset: " + wallpaperWin);
                            this.mLastWallpaperTimeoutTime = start;
                        }
                    }
                    this.mWaitingOnWallpaper = null;
                }
            } catch (RemoteException e2) {
            }
        }
        return changed;
    }

    void setWindowWallpaperPosition(WindowState window, float x, float y, float xStep, float yStep) {
        if (window.mWallpaperX != x || window.mWallpaperY != y) {
            window.mWallpaperX = x;
            window.mWallpaperY = y;
            window.mWallpaperXStep = xStep;
            window.mWallpaperYStep = yStep;
            updateWallpaperOffsetLocked(window, true);
        }
    }

    void setWindowWallpaperDisplayOffset(WindowState window, int x, int y) {
        if (window.mWallpaperDisplayOffsetX != x || window.mWallpaperDisplayOffsetY != y) {
            window.mWallpaperDisplayOffsetX = x;
            window.mWallpaperDisplayOffsetY = y;
            updateWallpaperOffsetLocked(window, true);
        }
    }

    Bundle sendWindowWallpaperCommand(WindowState window, String action, int x, int y, int z, Bundle extras, boolean sync) {
        if (window == this.mWallpaperTarget || window == this.mPrevWallpaperTarget) {
            boolean doWait = sync;
            for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
                ((WallpaperWindowToken) this.mWallpaperTokens.get(curTokenNdx)).sendWindowWallpaperCommand(action, x, y, z, extras, sync);
            }
        }
        return null;
    }

    private void updateWallpaperOffsetLocked(WindowState changingTarget, boolean sync) {
        DisplayContent displayContent = changingTarget.getDisplayContent();
        if (displayContent != null) {
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            int dw = displayInfo.logicalWidth;
            int dh = displayInfo.logicalHeight;
            WindowState target = this.mWallpaperTarget;
            if (target != null) {
                if (target.mWallpaperX >= OppoBrightUtils.MIN_LUX_LIMITI) {
                    this.mLastWallpaperX = target.mWallpaperX;
                } else if (changingTarget.mWallpaperX >= OppoBrightUtils.MIN_LUX_LIMITI) {
                    this.mLastWallpaperX = changingTarget.mWallpaperX;
                }
                if (target.mWallpaperY >= OppoBrightUtils.MIN_LUX_LIMITI) {
                    this.mLastWallpaperY = target.mWallpaperY;
                } else if (changingTarget.mWallpaperY >= OppoBrightUtils.MIN_LUX_LIMITI) {
                    this.mLastWallpaperY = changingTarget.mWallpaperY;
                }
                if (target.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                    this.mLastWallpaperDisplayOffsetX = target.mWallpaperDisplayOffsetX;
                } else if (changingTarget.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                    this.mLastWallpaperDisplayOffsetX = changingTarget.mWallpaperDisplayOffsetX;
                }
                if (target.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                    this.mLastWallpaperDisplayOffsetY = target.mWallpaperDisplayOffsetY;
                } else if (changingTarget.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                    this.mLastWallpaperDisplayOffsetY = changingTarget.mWallpaperDisplayOffsetY;
                }
                if (target.mWallpaperXStep >= OppoBrightUtils.MIN_LUX_LIMITI) {
                    this.mLastWallpaperXStep = target.mWallpaperXStep;
                } else if (changingTarget.mWallpaperXStep >= OppoBrightUtils.MIN_LUX_LIMITI) {
                    this.mLastWallpaperXStep = changingTarget.mWallpaperXStep;
                }
                if (target.mWallpaperYStep >= OppoBrightUtils.MIN_LUX_LIMITI) {
                    this.mLastWallpaperYStep = target.mWallpaperYStep;
                } else if (changingTarget.mWallpaperYStep >= OppoBrightUtils.MIN_LUX_LIMITI) {
                    this.mLastWallpaperYStep = changingTarget.mWallpaperYStep;
                }
            }
            for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
                ((WallpaperWindowToken) this.mWallpaperTokens.get(curTokenNdx)).updateWallpaperOffset(dw, dh, sync);
            }
        }
    }

    void clearLastWallpaperTimeoutTime() {
        this.mLastWallpaperTimeoutTime = 0;
    }

    void wallpaperCommandComplete(IBinder window) {
        if (this.mWaitingOnWallpaper != null && this.mWaitingOnWallpaper.mClient.asBinder() == window) {
            this.mWaitingOnWallpaper = null;
            this.mService.mWindowMap.notifyAll();
        }
    }

    void wallpaperOffsetsComplete(IBinder window) {
        if (this.mWaitingOnWallpaper != null && this.mWaitingOnWallpaper.mClient.asBinder() == window) {
            this.mWaitingOnWallpaper = null;
            this.mService.mWindowMap.notifyAll();
        }
    }

    int getAnimLayerAdjustment() {
        return this.mWallpaperAnimLayerAdjustment;
    }

    private void findWallpaperTarget(DisplayContent dc) {
        this.mFindResults.reset();
        if (dc.isStackVisible(2)) {
            this.mFindResults.setUseTopWallpaperAsTarget(false);
        }
        dc.forAllWindows(this.mFindWallpaperTargetFunction, true);
        if (this.mFindResults.wallpaperTarget == null && this.mFindResults.useTopWallpaperAsTarget) {
            this.mFindResults.setWallpaperTarget(this.mFindResults.topWallpaper);
        }
    }

    private boolean isFullscreen(LayoutParams attrs) {
        if (attrs.x == 0 && attrs.y == 0 && attrs.width == -1 && attrs.height == -1) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateWallpaperWindowsTarget(DisplayContent dc, FindWallpaperTargetResult result) {
        WindowState wallpaperTarget = result.wallpaperTarget;
        if (this.mWallpaperTarget == wallpaperTarget || (this.mPrevWallpaperTarget != null && this.mPrevWallpaperTarget == wallpaperTarget)) {
            if (!(this.mPrevWallpaperTarget == null || this.mPrevWallpaperTarget.isAnimatingLw())) {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    Slog.v(TAG, "No longer animating wallpaper targets!");
                }
                this.mPrevWallpaperTarget = null;
                this.mWallpaperTarget = wallpaperTarget;
            }
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.v(TAG, "New wallpaper target: " + wallpaperTarget + " prevTarget: " + this.mWallpaperTarget);
        }
        this.mPrevWallpaperTarget = null;
        WindowState prevWallpaperTarget = this.mWallpaperTarget;
        this.mWallpaperTarget = wallpaperTarget;
        if (wallpaperTarget != null && prevWallpaperTarget != null) {
            boolean oldAnim = prevWallpaperTarget.isAnimatingLw();
            boolean foundAnim = wallpaperTarget.isAnimatingLw();
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                Slog.v(TAG, "New animation: " + foundAnim + " old animation: " + oldAnim);
            }
            if (foundAnim && (oldAnim ^ 1) == 0 && dc.getWindow(new AnonymousClass1((byte) 2, prevWallpaperTarget)) != null) {
                boolean newTargetHidden;
                boolean oldTargetHidden;
                if (wallpaperTarget.mAppToken != null) {
                    newTargetHidden = wallpaperTarget.mAppToken.hiddenRequested;
                } else {
                    newTargetHidden = false;
                }
                if (prevWallpaperTarget.mAppToken != null) {
                    oldTargetHidden = prevWallpaperTarget.mAppToken.hiddenRequested;
                } else {
                    oldTargetHidden = false;
                }
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    Slog.v(TAG, "Animating wallpapers: old: " + prevWallpaperTarget + " hidden=" + oldTargetHidden + " new: " + wallpaperTarget + " hidden=" + newTargetHidden);
                }
                this.mPrevWallpaperTarget = prevWallpaperTarget;
                if (newTargetHidden && (oldTargetHidden ^ 1) != 0) {
                    if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                        Slog.v(TAG, "Old wallpaper still the target.");
                    }
                    this.mWallpaperTarget = prevWallpaperTarget;
                } else if (newTargetHidden == oldTargetHidden && (this.mService.mOpeningApps.contains(wallpaperTarget.mAppToken) ^ 1) != 0 && (this.mService.mOpeningApps.contains(prevWallpaperTarget.mAppToken) || this.mService.mClosingApps.contains(prevWallpaperTarget.mAppToken))) {
                    this.mWallpaperTarget = prevWallpaperTarget;
                }
                result.setWallpaperTarget(wallpaperTarget);
            }
        }
    }

    static /* synthetic */ boolean lambda$-com_android_server_wm_WallpaperController_23032(WindowState prevWallpaperTarget, WindowState w) {
        return w == prevWallpaperTarget;
    }

    private void updateWallpaperTokens(boolean visible) {
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WallpaperWindowToken token = (WallpaperWindowToken) this.mWallpaperTokens.get(curTokenNdx);
            token.updateWallpaperWindows(visible, this.mWallpaperAnimLayerAdjustment);
            token.getDisplayContent().assignWindowLayers(false);
        }
    }

    void adjustWallpaperWindows(DisplayContent dc) {
        int i = 0;
        this.mService.mRoot.mWallpaperMayChange = false;
        findWallpaperTarget(dc);
        updateWallpaperWindowsTarget(dc, this.mFindResults);
        boolean visible = this.mWallpaperTarget != null ? isWallpaperVisible(this.mWallpaperTarget) : false;
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v(TAG, "Wallpaper visibility: " + visible);
        }
        if (visible) {
            if (this.mPrevWallpaperTarget == null && this.mWallpaperTarget.mAppToken != null) {
                i = this.mWallpaperTarget.mAppToken.getAnimLayerAdjustment();
            }
            this.mWallpaperAnimLayerAdjustment = i;
            if (this.mWallpaperTarget.mWallpaperX >= OppoBrightUtils.MIN_LUX_LIMITI) {
                this.mLastWallpaperX = this.mWallpaperTarget.mWallpaperX;
                this.mLastWallpaperXStep = this.mWallpaperTarget.mWallpaperXStep;
            }
            if (this.mWallpaperTarget.mWallpaperY >= OppoBrightUtils.MIN_LUX_LIMITI) {
                this.mLastWallpaperY = this.mWallpaperTarget.mWallpaperY;
                this.mLastWallpaperYStep = this.mWallpaperTarget.mWallpaperYStep;
            }
            if (this.mWallpaperTarget.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = this.mWallpaperTarget.mWallpaperDisplayOffsetX;
            }
            if (this.mWallpaperTarget.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = this.mWallpaperTarget.mWallpaperDisplayOffsetY;
            }
        }
        updateWallpaperTokens(visible);
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.d(TAG, "New wallpaper: target=" + this.mWallpaperTarget + " prev=" + this.mPrevWallpaperTarget);
        }
    }

    boolean processWallpaperDrawPendingTimeout() {
        if (this.mWallpaperDrawState != 1) {
            return false;
        }
        this.mWallpaperDrawState = 2;
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v(TAG, "*** WALLPAPER DRAW TIMEOUT");
        }
        return true;
    }

    boolean wallpaperTransitionReady() {
        boolean transitionReady = true;
        boolean wallpaperReady = true;
        for (int curTokenIndex = this.mWallpaperTokens.size() - 1; curTokenIndex >= 0; curTokenIndex--) {
            if (((WallpaperWindowToken) this.mWallpaperTokens.get(curTokenIndex)).hasVisibleNotDrawnWallpaper()) {
                wallpaperReady = false;
                if (this.mWallpaperDrawState != 2) {
                    transitionReady = false;
                }
                if (this.mWallpaperDrawState == 0) {
                    this.mWallpaperDrawState = 1;
                    this.mService.mH.removeMessages(39);
                    this.mService.mH.sendEmptyMessageDelayed(39, 500);
                }
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v(TAG, "Wallpaper should be visible but has not been drawn yet. mWallpaperDrawState=" + this.mWallpaperDrawState);
                }
                if (wallpaperReady) {
                    this.mWallpaperDrawState = 0;
                    this.mService.mH.removeMessages(39);
                }
                return transitionReady;
            }
        }
        if (wallpaperReady) {
            this.mWallpaperDrawState = 0;
            this.mService.mH.removeMessages(39);
        }
        return transitionReady;
    }

    void adjustWallpaperWindowsForAppTransitionIfNeeded(DisplayContent dc, ArraySet<AppWindowToken> openingApps) {
        boolean adjust = false;
        if ((dc.pendingLayoutChanges & 4) != 0) {
            adjust = true;
        } else {
            for (int i = openingApps.size() - 1; i >= 0; i--) {
                if (((AppWindowToken) openingApps.valueAt(i)).windowsCanBeWallpaperTarget()) {
                    adjust = true;
                    break;
                }
            }
        }
        if (adjust) {
            adjustWallpaperWindows(dc);
        }
    }

    void addWallpaperToken(WallpaperWindowToken token) {
        this.mWallpaperTokens.add(token);
    }

    void removeWallpaperToken(WallpaperWindowToken token) {
        this.mWallpaperTokens.remove(token);
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mWallpaperTarget=");
        pw.println(this.mWallpaperTarget);
        if (this.mPrevWallpaperTarget != null) {
            pw.print(prefix);
            pw.print("mPrevWallpaperTarget=");
            pw.println(this.mPrevWallpaperTarget);
        }
        pw.print(prefix);
        pw.print("mLastWallpaperX=");
        pw.print(this.mLastWallpaperX);
        pw.print(" mLastWallpaperY=");
        pw.println(this.mLastWallpaperY);
        if (!(this.mLastWallpaperDisplayOffsetX == Integer.MIN_VALUE && this.mLastWallpaperDisplayOffsetY == Integer.MIN_VALUE)) {
            pw.print(prefix);
            pw.print("mLastWallpaperDisplayOffsetX=");
            pw.print(this.mLastWallpaperDisplayOffsetX);
            pw.print(" mLastWallpaperDisplayOffsetY=");
            pw.println(this.mLastWallpaperDisplayOffsetY);
        }
        if (this.mWallpaperAnimLayerAdjustment != 0) {
            pw.println(prefix + "mWallpaperAnimLayerAdjustment=" + this.mWallpaperAnimLayerAdjustment);
        }
    }
}
