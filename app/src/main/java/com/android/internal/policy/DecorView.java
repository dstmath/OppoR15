package com.android.internal.policy;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.ActivityManager.StackId;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ActionMode.Callback2;
import android.view.ContextThemeWrapper;
import android.view.DisplayListCanvas;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewOutlineProvider;
import android.view.ViewStub;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.Window;
import android.view.Window.WindowControllerCallback;
import android.view.WindowCallbacks;
import android.view.WindowInsets;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import com.android.internal.R;
import com.android.internal.policy.PhoneWindow.PhoneWindowMenuCallback;
import com.android.internal.view.FloatingActionMode;
import com.android.internal.view.RootViewSurfaceTaker;
import com.android.internal.view.StandaloneActionMode;
import com.android.internal.view.menu.ContextMenuBuilder;
import com.android.internal.view.menu.MenuHelper;
import com.android.internal.widget.ActionBarContextView;
import com.android.internal.widget.BackgroundFallback;
import com.android.internal.widget.DecorCaptionView;
import com.android.internal.widget.FloatingToolbar;
import com.oppo.debug.InputLog;
import java.util.List;

public class DecorView extends FrameLayout implements RootViewSurfaceTaker, WindowCallbacks {
    private static final boolean DEBUG_MEASURE = false;
    private static final int DECOR_SHADOW_FOCUSED_HEIGHT_IN_DIP = 20;
    private static final int DECOR_SHADOW_UNFOCUSED_HEIGHT_IN_DIP = 5;
    public static final ColorViewAttributes NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES = new ColorViewAttributes(2, 134217728, 80, 5, 3, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME, R.id.navigationBarBackground, 0, null);
    private static final ViewOutlineProvider PIP_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
            outline.setAlpha(1.0f);
        }
    };
    public static final ColorViewAttributes STATUS_BAR_COLOR_VIEW_ATTRIBUTES = new ColorViewAttributes(4, 67108864, 48, 3, 5, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME, R.id.statusBarBackground, 1024, null);
    private static final boolean SWEEP_OPEN_MENU = false;
    private static final String TAG = "DecorView";
    private View mAddView;
    private boolean mAllowUpdateElevation = false;
    private boolean mApplyFloatingHorizontalInsets = false;
    private boolean mApplyFloatingVerticalInsets = false;
    private float mAvailableWidth;
    private BackdropFrameRenderer mBackdropFrameRenderer = null;
    private final BackgroundFallback mBackgroundFallback = new BackgroundFallback();
    private final Rect mBackgroundPadding = new Rect();
    private final int mBarEnterExitDuration;
    private Drawable mCaptionBackgroundDrawable;
    private boolean mChanging;
    ViewGroup mContentRoot;
    DecorCaptionView mDecorCaptionView;
    int mDefaultOpacity = -1;
    private int mDownY;
    private final Rect mDrawingBounds = new Rect();
    private boolean mElevationAdjustedForStack = false;
    private boolean mElevationChange = false;
    private ObjectAnimator mFadeAnim;
    private final int mFeatureId;
    private ActionMode mFloatingActionMode;
    private View mFloatingActionModeOriginatingView;
    private final Rect mFloatingInsets = new Rect();
    private FloatingToolbar mFloatingToolbar;
    private OnPreDrawListener mFloatingToolbarPreDrawListener;
    final boolean mForceWindowDrawsStatusBarBackground;
    private final Rect mFrameOffsets = new Rect();
    private final Rect mFramePadding = new Rect();
    private boolean mHasCaption = false;
    private final Interpolator mHideInterpolator;
    private final Paint mHorizontalResizeShadowPaint = new Paint();
    private boolean mIsInPictureInPictureMode;
    private Callback mLastBackgroundDrawableCb = null;
    private int mLastBottomInset = 0;
    private boolean mLastHasBottomStableInset = false;
    private boolean mLastHasLeftStableInset = false;
    private boolean mLastHasRightStableInset = false;
    private boolean mLastHasTopStableInset = false;
    private int mLastLeftInset = 0;
    private ViewOutlineProvider mLastOutlineProvider;
    private int mLastRightInset = 0;
    private boolean mLastShouldAlwaysConsumeNavBar = false;
    private boolean mLastShowNavigationBarView = true;
    private int mLastTopInset = 0;
    private int mLastWindowFlags = 0;
    String mLogTag = TAG;
    private Drawable mMenuBackground;
    private final ColorViewState mNavigationColorViewState = new ColorViewState(NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES);
    private View mNavigationGuard;
    private Rect mOutsets = new Rect();
    ActionMode mPrimaryActionMode;
    private PopupWindow mPrimaryActionModePopup;
    private ActionBarContextView mPrimaryActionModeView;
    private int mResizeMode = -1;
    private final int mResizeShadowSize;
    private Drawable mResizingBackgroundDrawable;
    private int mRootScrollY = 0;
    private final int mSemiTransparentStatusBarColor;
    private final Interpolator mShowInterpolator;
    private Runnable mShowPrimaryActionModePopup;
    int mStackId;
    private final ColorViewState mStatusColorViewState = new ColorViewState(STATUS_BAR_COLOR_VIEW_ATTRIBUTES);
    private View mStatusGuard;
    private Rect mTempRect;
    private Drawable mUserCaptionBackgroundDrawable;
    private final Paint mVerticalResizeShadowPaint = new Paint();
    private boolean mWatchingForMenu;
    private PhoneWindow mWindow;
    private boolean mWindowResizeCallbacksAdded = false;

    private class ActionModeCallback2Wrapper extends Callback2 {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallback2Wrapper(ActionMode.Callback wrapped) {
            this.mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return this.mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            DecorView.this.requestFitSystemWindows();
            return this.mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return this.mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            boolean isPrimary;
            boolean isFloating;
            this.mWrapped.onDestroyActionMode(mode);
            if (DecorView.this.mContext.getApplicationInfo().targetSdkVersion >= 23) {
                isPrimary = mode == DecorView.this.mPrimaryActionMode;
                isFloating = mode == DecorView.this.mFloatingActionMode;
                if (!isPrimary && mode.getType() == 0) {
                    Log.e(DecorView.this.mLogTag, "Destroying unexpected ActionMode instance of TYPE_PRIMARY; " + mode + " was not the current primary action mode! Expected " + DecorView.this.mPrimaryActionMode);
                }
                if (!isFloating && mode.getType() == 1) {
                    Log.e(DecorView.this.mLogTag, "Destroying unexpected ActionMode instance of TYPE_FLOATING; " + mode + " was not the current floating action mode! Expected " + DecorView.this.mFloatingActionMode);
                }
            } else {
                isPrimary = mode.getType() == 0;
                isFloating = mode.getType() == 1;
            }
            if (isPrimary) {
                if (DecorView.this.mPrimaryActionModePopup != null) {
                    DecorView.this.removeCallbacks(DecorView.this.mShowPrimaryActionModePopup);
                }
                if (DecorView.this.mPrimaryActionModeView != null) {
                    DecorView.this.endOnGoingFadeAnimation();
                    final ActionBarContextView lastActionModeView = DecorView.this.mPrimaryActionModeView;
                    DecorView.this.mFadeAnim = ObjectAnimator.ofFloat(DecorView.this.mPrimaryActionModeView, View.ALPHA, new float[]{1.0f, 0.0f});
                    DecorView.this.mFadeAnim.addListener(new AnimatorListener() {
                        public void onAnimationStart(Animator animation) {
                        }

                        public void onAnimationEnd(Animator animation) {
                            if (lastActionModeView == DecorView.this.mPrimaryActionModeView) {
                                lastActionModeView.setVisibility(8);
                                if (DecorView.this.mPrimaryActionModePopup != null) {
                                    DecorView.this.mPrimaryActionModePopup.dismiss();
                                }
                                lastActionModeView.killMode();
                                DecorView.this.mFadeAnim = null;
                            }
                        }

                        public void onAnimationCancel(Animator animation) {
                        }

                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
                    DecorView.this.mFadeAnim.start();
                }
                DecorView.this.mPrimaryActionMode = null;
            } else if (isFloating) {
                DecorView.this.cleanupFloatingActionModeViews();
                DecorView.this.mFloatingActionMode = null;
            }
            if (!(DecorView.this.mWindow.getCallback() == null || (DecorView.this.mWindow.isDestroyed() ^ 1) == 0)) {
                try {
                    DecorView.this.mWindow.getCallback().onActionModeFinished(mode);
                } catch (AbstractMethodError e) {
                }
            }
            DecorView.this.requestFitSystemWindows();
        }

        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (this.mWrapped instanceof Callback2) {
                ((Callback2) this.mWrapped).onGetContentRect(mode, view, outRect);
            } else {
                super.onGetContentRect(mode, view, outRect);
            }
        }
    }

    public static class ColorViewAttributes {
        final int hideWindowFlag;
        final int horizontalGravity;
        final int id;
        final int seascapeGravity;
        final int systemUiHideFlag;
        final String transitionName;
        final int translucentFlag;
        final int verticalGravity;

        /* synthetic */ ColorViewAttributes(int systemUiHideFlag, int translucentFlag, int verticalGravity, int horizontalGravity, int seascapeGravity, String transitionName, int id, int hideWindowFlag, ColorViewAttributes -this8) {
            this(systemUiHideFlag, translucentFlag, verticalGravity, horizontalGravity, seascapeGravity, transitionName, id, hideWindowFlag);
        }

        private ColorViewAttributes(int systemUiHideFlag, int translucentFlag, int verticalGravity, int horizontalGravity, int seascapeGravity, String transitionName, int id, int hideWindowFlag) {
            this.id = id;
            this.systemUiHideFlag = systemUiHideFlag;
            this.translucentFlag = translucentFlag;
            this.verticalGravity = verticalGravity;
            this.horizontalGravity = horizontalGravity;
            this.seascapeGravity = seascapeGravity;
            this.transitionName = transitionName;
            this.hideWindowFlag = hideWindowFlag;
        }

        public boolean isPresent(int sysUiVis, int windowFlags, boolean force) {
            if ((this.systemUiHideFlag & sysUiVis) != 0 || (this.hideWindowFlag & windowFlags) != 0) {
                return false;
            }
            if ((Integer.MIN_VALUE & windowFlags) == 0) {
                return force;
            }
            return true;
        }

        public boolean isVisible(boolean present, int color, int windowFlags, boolean force) {
            if (!present || (-16777216 & color) == 0) {
                return false;
            }
            if ((this.translucentFlag & windowFlags) != 0) {
                return force;
            }
            return true;
        }

        public boolean isVisible(int sysUiVis, int color, int windowFlags, boolean force) {
            return isVisible(isPresent(sysUiVis, windowFlags, force), color, windowFlags, force);
        }
    }

    private static class ColorViewState {
        final ColorViewAttributes attributes;
        int color;
        boolean present = false;
        int targetVisibility = 4;
        View view = null;
        boolean visible;

        ColorViewState(ColorViewAttributes attributes) {
            this.attributes = attributes;
        }
    }

    DecorView(Context context, int featureId, PhoneWindow window, LayoutParams params) {
        boolean z = true;
        super(context);
        this.mFeatureId = featureId;
        this.mShowInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
        this.mHideInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_linear_in);
        this.mBarEnterExitDuration = context.getResources().getInteger(R.integer.dock_enter_exit_duration);
        if (!context.getResources().getBoolean(R.bool.config_forceWindowDrawsStatusBarBackground)) {
            z = false;
        } else if (context.getApplicationInfo().targetSdkVersion < 24) {
            z = false;
        }
        this.mForceWindowDrawsStatusBarBackground = z;
        this.mSemiTransparentStatusBarColor = context.getResources().getColor(R.color.system_bar_background_semi_transparent, null);
        updateAvailableWidth();
        setWindow(window);
        updateLogTag(params);
        this.mResizeShadowSize = context.getResources().getDimensionPixelSize(R.dimen.resize_shadow_size);
        initResizingPaints();
    }

    void setBackgroundFallback(int resId) {
        boolean hasFallback;
        Drawable drawable = null;
        BackgroundFallback backgroundFallback = this.mBackgroundFallback;
        if (resId != 0) {
            drawable = getContext().getDrawable(resId);
        }
        backgroundFallback.setDrawable(drawable);
        if (getBackground() == null) {
            hasFallback = this.mBackgroundFallback.hasFallback() ^ 1;
        } else {
            hasFallback = false;
        }
        setWillNotDraw(hasFallback);
    }

    public boolean gatherTransparentRegion(Region region) {
        return (gatherTransparentRegion(this.mStatusColorViewState, region) || gatherTransparentRegion(this.mNavigationColorViewState, region)) ? true : super.gatherTransparentRegion(region);
    }

    boolean gatherTransparentRegion(ColorViewState colorViewState, Region region) {
        if (colorViewState.view != null && colorViewState.visible && isResizing()) {
            return colorViewState.view.gatherTransparentRegion(region);
        }
        return false;
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        this.mBackgroundFallback.draw(isResizing() ? this : this.mContentRoot, this.mContentRoot, c, this.mWindow.mContentParent);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean onKeyDown;
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        boolean isDown = action == 0;
        if (InputLog.DEBUG) {
            InputLog.d(TAG, " dispatchKeyEvent, keyCode = " + keyCode + ", action = " + action + ", isDown = " + isDown);
        }
        if (InputLog.DEBUG) {
            InputLog.d(TAG, " event.getRepeatCount() = " + event.getRepeatCount() + ", isDestroyed() = " + this.mWindow.isDestroyed());
        }
        if (isDown && event.getRepeatCount() == 0) {
            if (this.mWindow.mPanelChordingKey > 0 && this.mWindow.mPanelChordingKey != keyCode && dispatchKeyShortcutEvent(event)) {
                return true;
            }
            if (this.mWindow.mPreparedPanel != null && this.mWindow.mPreparedPanel.isOpen && this.mWindow.performPanelShortcut(this.mWindow.mPreparedPanel, keyCode, event, 0)) {
                return true;
            }
        }
        if (!this.mWindow.isDestroyed()) {
            boolean handled;
            Window.Callback cb = this.mWindow.getCallback();
            if (cb == null || this.mFeatureId >= 0) {
                handled = super.dispatchKeyEvent(event);
            } else {
                handled = cb.dispatchKeyEvent(event);
            }
            if (InputLog.DEBUG) {
                InputLog.d(TAG, "dispatchKeyEvent = " + event + " cb = " + cb + ", mFeatureId = " + this.mFeatureId + ", handled = " + handled);
            }
            if (handled) {
                if (InputLog.isVolumeKey(keyCode)) {
                    Log.d(TAG, "Volume key is handled by app!");
                }
                return true;
            }
        }
        if (isDown) {
            onKeyDown = this.mWindow.onKeyDown(this.mFeatureId, event.getKeyCode(), event);
        } else {
            onKeyDown = this.mWindow.onKeyUp(this.mFeatureId, event.getKeyCode(), event);
        }
        return onKeyDown;
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent ev) {
        if (this.mWindow.mPreparedPanel == null || !this.mWindow.performPanelShortcut(this.mWindow.mPreparedPanel, ev.getKeyCode(), ev, 1)) {
            Window.Callback cb = this.mWindow.getCallback();
            boolean handled = (cb == null || (this.mWindow.isDestroyed() ^ 1) == 0 || this.mFeatureId >= 0) ? super.dispatchKeyShortcutEvent(ev) : cb.dispatchKeyShortcutEvent(ev);
            if (handled) {
                return true;
            }
            PanelFeatureState st = this.mWindow.getPanelState(0, false);
            if (st != null && this.mWindow.mPreparedPanel == null) {
                this.mWindow.preparePanel(st, ev);
                handled = this.mWindow.performPanelShortcut(st, ev.getKeyCode(), ev, 1);
                st.isPrepared = false;
                if (handled) {
                    return true;
                }
            }
            return false;
        }
        if (this.mWindow.mPreparedPanel != null) {
            this.mWindow.mPreparedPanel.isHandled = true;
        }
        return true;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        Window.Callback cb = this.mWindow.getCallback();
        return (cb == null || (this.mWindow.isDestroyed() ^ 1) == 0 || this.mFeatureId >= 0) ? super.dispatchTouchEvent(ev) : cb.dispatchTouchEvent(ev);
    }

    public boolean dispatchTrackballEvent(MotionEvent ev) {
        Window.Callback cb = this.mWindow.getCallback();
        return (cb == null || (this.mWindow.isDestroyed() ^ 1) == 0 || this.mFeatureId >= 0) ? super.dispatchTrackballEvent(ev) : cb.dispatchTrackballEvent(ev);
    }

    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        Window.Callback cb = this.mWindow.getCallback();
        return (cb == null || (this.mWindow.isDestroyed() ^ 1) == 0 || this.mFeatureId >= 0) ? super.dispatchGenericMotionEvent(ev) : cb.dispatchGenericMotionEvent(ev);
    }

    public boolean superDispatchKeyEvent(KeyEvent event) {
        if (InputLog.DEBUG) {
            InputLog.d(TAG, " superDispatchKeyEvent(), event = " + KeyEvent.keyCodeToString(event.getKeyCode()));
        }
        if (event.getKeyCode() == 4) {
            int action = event.getAction();
            if (this.mPrimaryActionMode != null) {
                if (action == 1) {
                    this.mPrimaryActionMode.finish();
                }
                if (InputLog.DEBUG) {
                    InputLog.d(TAG, " KeyEvent.KEYCODE_BACK consumed by " + this.mPrimaryActionMode);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
        return super.dispatchKeyShortcutEvent(event);
    }

    public boolean superDispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    public boolean superDispatchTrackballEvent(MotionEvent event) {
        return super.dispatchTrackballEvent(event);
    }

    public boolean superDispatchGenericMotionEvent(MotionEvent event) {
        return super.dispatchGenericMotionEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return onInterceptTouchEvent(event);
    }

    private boolean isOutOfInnerBounds(int x, int y) {
        return x < 0 || y < 0 || x > getWidth() || y > getHeight();
    }

    private boolean isOutOfBounds(int x, int y) {
        if (x < -5 || y < -5 || x > getWidth() + 5 || y > getHeight() + 5) {
            return true;
        }
        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (this.mHasCaption && isShowingCaption() && action == 0 && isOutOfInnerBounds((int) event.getX(), (int) event.getY())) {
            return true;
        }
        if (this.mFeatureId < 0 || action != 0 || !isOutOfBounds((int) event.getX(), (int) event.getY())) {
            return false;
        }
        this.mWindow.closePanel(this.mFeatureId);
        return true;
    }

    public void sendAccessibilityEvent(int eventType) {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if ((this.mFeatureId == 0 || this.mFeatureId == 6 || this.mFeatureId == 2 || this.mFeatureId == 5) && getChildCount() == 1) {
                getChildAt(0).sendAccessibilityEvent(eventType);
            } else {
                super.sendAccessibilityEvent(eventType);
            }
        }
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        Window.Callback cb = this.mWindow.getCallback();
        if (cb == null || (this.mWindow.isDestroyed() ^ 1) == 0 || !cb.dispatchPopulateAccessibilityEvent(event)) {
            return super.dispatchPopulateAccessibilityEventInternal(event);
        }
        return true;
    }

    protected boolean setFrame(int l, int t, int r, int b) {
        boolean changed = super.setFrame(l, t, r, b);
        if (changed) {
            Rect drawingBounds = this.mDrawingBounds;
            getDrawingRect(drawingBounds);
            Drawable fg = getForeground();
            if (fg != null) {
                Rect frameOffsets = this.mFrameOffsets;
                drawingBounds.left += frameOffsets.left;
                drawingBounds.top += frameOffsets.top;
                drawingBounds.right -= frameOffsets.right;
                drawingBounds.bottom -= frameOffsets.bottom;
                fg.setBounds(drawingBounds);
                Rect framePadding = this.mFramePadding;
                drawingBounds.left += framePadding.left - frameOffsets.left;
                drawingBounds.top += framePadding.top - frameOffsets.top;
                drawingBounds.right -= framePadding.right - frameOffsets.right;
                drawingBounds.bottom -= framePadding.bottom - frameOffsets.bottom;
            }
            Drawable bg = getBackground();
            if (bg != null) {
                bg.setBounds(drawingBounds);
            }
        }
        return changed;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode;
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        boolean isPortrait = getResources().getConfiguration().orientation == 1;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean fixedWidth = false;
        this.mApplyFloatingHorizontalInsets = false;
        if (widthMode == Integer.MIN_VALUE) {
            TypedValue tvw = isPortrait ? this.mWindow.mFixedWidthMinor : this.mWindow.mFixedWidthMajor;
            if (!(tvw == null || tvw.type == 0)) {
                int w;
                if (tvw.type == 5) {
                    w = (int) tvw.getDimension(metrics);
                } else if (tvw.type == 6) {
                    w = (int) tvw.getFraction((float) metrics.widthPixels, (float) metrics.widthPixels);
                } else {
                    w = 0;
                }
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                if (w > 0) {
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(w, widthSize), 1073741824);
                    fixedWidth = true;
                } else {
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec((widthSize - this.mFloatingInsets.left) - this.mFloatingInsets.right, Integer.MIN_VALUE);
                    this.mApplyFloatingHorizontalInsets = true;
                }
            }
        }
        this.mApplyFloatingVerticalInsets = false;
        if (heightMode == Integer.MIN_VALUE) {
            TypedValue tvh;
            if (isPortrait) {
                tvh = this.mWindow.mFixedHeightMajor;
            } else {
                tvh = this.mWindow.mFixedHeightMinor;
            }
            if (!(tvh == null || tvh.type == 0)) {
                int h;
                if (tvh.type == 5) {
                    h = (int) tvh.getDimension(metrics);
                } else if (tvh.type == 6) {
                    h = (int) tvh.getFraction((float) metrics.heightPixels, (float) metrics.heightPixels);
                } else {
                    h = 0;
                }
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);
                if (h > 0) {
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(h, heightSize), 1073741824);
                } else if ((this.mWindow.getAttributes().flags & 256) == 0) {
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec((heightSize - this.mFloatingInsets.top) - this.mFloatingInsets.bottom, Integer.MIN_VALUE);
                    this.mApplyFloatingVerticalInsets = true;
                }
            }
        }
        getOutsets(this.mOutsets);
        if (this.mOutsets.top > 0 || this.mOutsets.bottom > 0) {
            mode = MeasureSpec.getMode(heightMeasureSpec);
            if (mode != 0) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec((this.mOutsets.top + MeasureSpec.getSize(heightMeasureSpec)) + this.mOutsets.bottom, mode);
            }
        }
        if (this.mOutsets.left > 0 || this.mOutsets.right > 0) {
            mode = MeasureSpec.getMode(widthMeasureSpec);
            if (mode != 0) {
                widthMeasureSpec = MeasureSpec.makeMeasureSpec((this.mOutsets.left + MeasureSpec.getSize(widthMeasureSpec)) + this.mOutsets.right, mode);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        boolean measure = false;
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, 1073741824);
        if (!fixedWidth && widthMode == Integer.MIN_VALUE) {
            TypedValue tv = isPortrait ? this.mWindow.mMinWidthMinor : this.mWindow.mMinWidthMajor;
            if (tv.type != 0) {
                int min;
                if (tv.type == 5) {
                    min = (int) tv.getDimension(metrics);
                } else if (tv.type == 6) {
                    min = (int) tv.getFraction(this.mAvailableWidth, this.mAvailableWidth);
                } else {
                    min = 0;
                }
                if (width < min) {
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(min, 1073741824);
                    measure = true;
                }
                if (measure && min > metrics.widthPixels && isPortrait) {
                    Log.d(this.mLogTag, "don't need Adjust width from " + width + " to " + min + ",should to " + metrics.widthPixels + ",isPortrait=" + isPortrait + ",mAvailableWidth=" + this.mAvailableWidth + ",attrs=" + (this.mWindow != null ? this.mWindow.getAttributes() : null));
                    if (this.mAvailableWidth > ((float) (metrics.widthPixels + 1))) {
                        updateAvailableWidth();
                        if (width < metrics.widthPixels) {
                            widthMeasureSpec = MeasureSpec.makeMeasureSpec(metrics.widthPixels, 1073741824);
                        } else {
                            measure = false;
                        }
                    }
                }
            }
        }
        if (measure) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        getOutsets(this.mOutsets);
        if (this.mOutsets.left > 0) {
            offsetLeftAndRight(-this.mOutsets.left);
        }
        if (this.mOutsets.top > 0) {
            offsetTopAndBottom(-this.mOutsets.top);
        }
        if (this.mApplyFloatingVerticalInsets) {
            offsetTopAndBottom(this.mFloatingInsets.top);
        }
        if (this.mApplyFloatingHorizontalInsets) {
            offsetLeftAndRight(this.mFloatingInsets.left);
        }
        updateElevation();
        this.mAllowUpdateElevation = true;
        if (changed && this.mResizeMode == 1) {
            getViewRootImpl().requestInvalidateRootRenderNode();
        }
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mMenuBackground != null) {
            this.mMenuBackground.draw(canvas);
        }
    }

    public boolean showContextMenuForChild(View originalView) {
        return showContextMenuForChildInternal(originalView, Float.NaN, Float.NaN);
    }

    public boolean showContextMenuForChild(View originalView, float x, float y) {
        return showContextMenuForChildInternal(originalView, x, y);
    }

    private boolean showContextMenuForChildInternal(View originalView, float x, float y) {
        MenuHelper helper;
        if (this.mWindow.mContextMenuHelper != null) {
            this.mWindow.mContextMenuHelper.dismiss();
            this.mWindow.mContextMenuHelper = null;
        }
        PhoneWindowMenuCallback callback = this.mWindow.mContextMenuCallback;
        if (this.mWindow.mContextMenu == null) {
            this.mWindow.mContextMenu = new ContextMenuBuilder(getContext());
            this.mWindow.mContextMenu.setCallback(callback);
        } else {
            this.mWindow.mContextMenu.clearAll();
        }
        int isPopup = !Float.isNaN(x) ? Float.isNaN(y) ^ 1 : 0;
        if (isPopup != 0) {
            helper = this.mWindow.mContextMenu.showPopup(getContext(), originalView, x, y);
        } else {
            helper = this.mWindow.mContextMenu.showDialog(originalView, originalView.getWindowToken());
        }
        if (helper != null) {
            callback.setShowDialogForSubmenu(isPopup ^ 1);
            helper.setPresenterCallback(callback);
        }
        this.mWindow.mContextMenuHelper = helper;
        if (helper != null) {
            return true;
        }
        return false;
    }

    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
        return startActionModeForChild(originalView, callback, 0);
    }

    public ActionMode startActionModeForChild(View child, ActionMode.Callback callback, int type) {
        return startActionMode(child, callback, type);
    }

    public ActionMode startActionMode(ActionMode.Callback callback) {
        return startActionMode(callback, 0);
    }

    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        return startActionMode(this, callback, type);
    }

    private ActionMode startActionMode(View originatingView, ActionMode.Callback callback, int type) {
        Callback2 wrappedCallback = new ActionModeCallback2Wrapper(callback);
        ActionMode mode = null;
        if (!(this.mWindow.getCallback() == null || (this.mWindow.isDestroyed() ^ 1) == 0)) {
            try {
                mode = this.mWindow.getCallback().onWindowStartingActionMode(wrappedCallback, type);
            } catch (AbstractMethodError e) {
                if (type == 0) {
                    try {
                        mode = this.mWindow.getCallback().onWindowStartingActionMode(wrappedCallback);
                    } catch (AbstractMethodError e2) {
                    }
                }
            }
        }
        if (mode == null) {
            mode = createActionMode(type, wrappedCallback, originatingView);
            if (mode == null || !wrappedCallback.onCreateActionMode(mode, mode.getMenu())) {
                mode = null;
            } else {
                setHandledActionMode(mode);
            }
        } else if (mode.getType() == 0) {
            cleanupPrimaryActionMode();
            this.mPrimaryActionMode = mode;
        } else if (mode.getType() == 1) {
            if (this.mFloatingActionMode != null) {
                this.mFloatingActionMode.finish();
            }
            this.mFloatingActionMode = mode;
        }
        if (!(mode == null || this.mWindow.getCallback() == null || (this.mWindow.isDestroyed() ^ 1) == 0)) {
            try {
                this.mWindow.getCallback().onActionModeStarted(mode);
            } catch (AbstractMethodError e3) {
            }
        }
        return mode;
    }

    private void cleanupPrimaryActionMode() {
        if (this.mPrimaryActionMode != null) {
            this.mPrimaryActionMode.finish();
            this.mPrimaryActionMode = null;
        }
        if (this.mPrimaryActionModeView != null) {
            this.mPrimaryActionModeView.killMode();
        }
    }

    private void cleanupFloatingActionModeViews() {
        if (this.mFloatingToolbar != null) {
            this.mFloatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        if (this.mFloatingActionModeOriginatingView != null) {
            if (this.mFloatingToolbarPreDrawListener != null) {
                this.mFloatingActionModeOriginatingView.getViewTreeObserver().removeOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
                this.mFloatingToolbarPreDrawListener = null;
            }
            this.mFloatingActionModeOriginatingView = null;
        }
    }

    void startChanging() {
        this.mChanging = true;
    }

    void finishChanging() {
        this.mChanging = false;
        drawableChanged();
    }

    public void setWindowBackground(Drawable drawable) {
        boolean z = true;
        if (getBackground() != drawable) {
            setBackgroundDrawable(drawable);
            if (drawable != null) {
                if (!this.mWindow.isTranslucent()) {
                    z = this.mWindow.isShowingWallpaper();
                }
                this.mResizingBackgroundDrawable = enforceNonTranslucentBackground(drawable, z);
            } else {
                Context context = getContext();
                int i = this.mWindow.mBackgroundFallbackResource;
                if (!this.mWindow.isTranslucent()) {
                    z = this.mWindow.isShowingWallpaper();
                }
                this.mResizingBackgroundDrawable = getResizingBackgroundDrawable(context, 0, i, z);
            }
            if (this.mResizingBackgroundDrawable != null) {
                this.mResizingBackgroundDrawable.getPadding(this.mBackgroundPadding);
            } else {
                this.mBackgroundPadding.setEmpty();
            }
            drawableChanged();
        }
    }

    public void setWindowFrame(Drawable drawable) {
        if (getForeground() != drawable) {
            setForeground(drawable);
            if (drawable != null) {
                drawable.getPadding(this.mFramePadding);
            } else {
                this.mFramePadding.setEmpty();
            }
            drawableChanged();
        }
    }

    public void onWindowSystemUiVisibilityChanged(int visible) {
        updateColorViews(null, true);
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        LayoutParams attrs = this.mWindow.getAttributes();
        this.mFloatingInsets.setEmpty();
        if ((attrs.flags & 256) == 0) {
            if (attrs.height == -2) {
                this.mFloatingInsets.top = insets.getSystemWindowInsetTop();
                this.mFloatingInsets.bottom = insets.getSystemWindowInsetBottom();
                insets = insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
            }
            if (this.mWindow.getAttributes().width == -2) {
                this.mFloatingInsets.left = insets.getSystemWindowInsetTop();
                this.mFloatingInsets.right = insets.getSystemWindowInsetBottom();
                insets = insets.replaceSystemWindowInsets(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            }
        }
        this.mFrameOffsets.set(insets.getSystemWindowInsets());
        insets = updateNavigationGuard(updateStatusGuard(updateColorViews(insets, true)));
        if (getForeground() != null) {
            drawableChanged();
        }
        return insets;
    }

    public boolean isTransitionGroup() {
        return false;
    }

    public static int getColorViewTopInset(int stableTop, int systemTop) {
        return Math.min(stableTop, systemTop);
    }

    public static int getColorViewBottomInset(int stableBottom, int systemBottom) {
        return Math.min(stableBottom, systemBottom);
    }

    public static int getColorViewRightInset(int stableRight, int systemRight) {
        return Math.min(stableRight, systemRight);
    }

    public static int getColorViewLeftInset(int stableLeft, int systemLeft) {
        return Math.min(stableLeft, systemLeft);
    }

    public static boolean isNavBarToRightEdge(int bottomInset, int rightInset) {
        return bottomInset == 0 && rightInset > 0;
    }

    public static boolean isNavBarToLeftEdge(int bottomInset, int leftInset) {
        return bottomInset == 0 && leftInset > 0;
    }

    public static int getNavBarSize(int bottomInset, int rightInset, int leftInset) {
        if (isNavBarToRightEdge(bottomInset, rightInset)) {
            return rightInset;
        }
        return isNavBarToLeftEdge(bottomInset, leftInset) ? leftInset : bottomInset;
    }

    public static void getNavigationBarRect(int canvasWidth, int canvasHeight, Rect stableInsets, Rect contentInsets, Rect outRect) {
        int bottomInset = getColorViewBottomInset(stableInsets.bottom, contentInsets.bottom);
        int leftInset = getColorViewLeftInset(stableInsets.left, contentInsets.left);
        int rightInset = getColorViewLeftInset(stableInsets.right, contentInsets.right);
        int size = getNavBarSize(bottomInset, rightInset, leftInset);
        if (isNavBarToRightEdge(bottomInset, rightInset)) {
            outRect.set(canvasWidth - size, 0, canvasWidth, canvasHeight);
        } else if (isNavBarToLeftEdge(bottomInset, leftInset)) {
            outRect.set(0, 0, size, canvasHeight);
        } else {
            outRect.set(0, canvasHeight - size, canvasWidth, canvasHeight);
        }
    }

    WindowInsets updateColorViews(WindowInsets insets, boolean animate) {
        boolean consumingNavBar;
        LayoutParams attrs = this.mWindow.getAttributes();
        int sysUiVisibility = attrs.systemUiVisibility | getWindowSystemUiVisibility();
        if (!this.mWindow.mIsFloating) {
            boolean statusBarNeedsRightInset;
            boolean statusBarNeedsLeftInset;
            boolean z;
            boolean disallowAnimate = (isLaidOut() ^ 1) | (((this.mLastWindowFlags ^ attrs.flags) & Integer.MIN_VALUE) != 0 ? 1 : 0);
            this.mLastWindowFlags = attrs.flags;
            if (insets != null) {
                this.mLastTopInset = getColorViewTopInset(insets.getStableInsetTop(), insets.getSystemWindowInsetTop());
                this.mLastBottomInset = getColorViewBottomInset(insets.getStableInsetBottom(), insets.getSystemWindowInsetBottom());
                this.mLastRightInset = getColorViewRightInset(insets.getStableInsetRight(), insets.getSystemWindowInsetRight());
                this.mLastLeftInset = getColorViewRightInset(insets.getStableInsetLeft(), insets.getSystemWindowInsetLeft());
                boolean hasTopStableInset = insets.getStableInsetTop() != 0;
                disallowAnimate |= hasTopStableInset != this.mLastHasTopStableInset ? 1 : 0;
                this.mLastHasTopStableInset = hasTopStableInset;
                boolean hasBottomStableInset = insets.getStableInsetBottom() != 0;
                disallowAnimate |= hasBottomStableInset != this.mLastHasBottomStableInset ? 1 : 0;
                this.mLastHasBottomStableInset = hasBottomStableInset;
                boolean hasRightStableInset = insets.getStableInsetRight() != 0;
                disallowAnimate |= hasRightStableInset != this.mLastHasRightStableInset ? 1 : 0;
                this.mLastHasRightStableInset = hasRightStableInset;
                boolean hasLeftStableInset = insets.getStableInsetLeft() != 0;
                disallowAnimate |= hasLeftStableInset != this.mLastHasLeftStableInset ? 1 : 0;
                this.mLastHasLeftStableInset = hasLeftStableInset;
                this.mLastShouldAlwaysConsumeNavBar = insets.shouldAlwaysConsumeNavBar();
            }
            boolean navBarToRightEdge = isNavBarToRightEdge(this.mLastBottomInset, this.mLastRightInset);
            boolean navBarToLeftEdge = isNavBarToLeftEdge(this.mLastBottomInset, this.mLastLeftInset);
            int navBarSize = getNavBarSize(this.mLastBottomInset, this.mLastRightInset, this.mLastLeftInset);
            boolean forceDrawsStatusBarBackground = false;
            if ((attrs.navigationBarVisibility & 536870912) != 0) {
                forceDrawsStatusBarBackground = true;
            }
            updateColorViewInt(this.mNavigationColorViewState, sysUiVisibility, this.mWindow.mNavigationBarColor, this.mWindow.mNavigationBarDividerColor, navBarSize, !navBarToRightEdge ? navBarToLeftEdge : true, navBarToLeftEdge, 0, animate ? disallowAnimate ^ 1 : false, false);
            if (navBarToRightEdge) {
                statusBarNeedsRightInset = this.mNavigationColorViewState.present;
            } else {
                statusBarNeedsRightInset = false;
            }
            if (navBarToLeftEdge) {
                statusBarNeedsLeftInset = this.mNavigationColorViewState.present;
            } else {
                statusBarNeedsLeftInset = false;
            }
            int statusBarSideInset = statusBarNeedsRightInset ? this.mLastRightInset : statusBarNeedsLeftInset ? this.mLastLeftInset : 0;
            ColorViewState colorViewState = this.mStatusColorViewState;
            int calculateStatusBarColor = calculateStatusBarColor();
            int i = this.mLastTopInset;
            boolean z2 = animate ? disallowAnimate ^ 1 : false;
            if (this.mForceWindowDrawsStatusBarBackground) {
                z = true;
            } else {
                z = forceDrawsStatusBarBackground;
            }
            updateColorViewInt(colorViewState, sysUiVisibility, calculateStatusBarColor, 0, i, false, statusBarNeedsLeftInset, statusBarSideInset, z2, z);
        }
        if ((attrs.flags & Integer.MIN_VALUE) != 0 && (sysUiVisibility & 512) == 0 && (sysUiVisibility & 2) == 0) {
            consumingNavBar = true;
        } else {
            consumingNavBar = this.mLastShouldAlwaysConsumeNavBar;
        }
        boolean consumingStatusBar = ((sysUiVisibility & 1024) == 0 && (Integer.MIN_VALUE & sysUiVisibility) == 0 && (attrs.flags & 256) == 0 && (attrs.flags & 65536) == 0 && this.mForceWindowDrawsStatusBarBackground) ? this.mLastTopInset != 0 : false;
        int consumedTop = consumingStatusBar ? this.mLastTopInset : 0;
        int consumedRight = consumingNavBar ? this.mLastRightInset : 0;
        int consumedBottom = consumingNavBar ? this.mLastBottomInset : 0;
        int consumedLeft = consumingNavBar ? this.mLastLeftInset : 0;
        if (this.mContentRoot != null && (this.mContentRoot.getLayoutParams() instanceof MarginLayoutParams)) {
            ViewGroup.LayoutParams lp = (MarginLayoutParams) this.mContentRoot.getLayoutParams();
            if (!(lp.topMargin == consumedTop && lp.rightMargin == consumedRight && lp.bottomMargin == consumedBottom && lp.leftMargin == consumedLeft)) {
                lp.topMargin = consumedTop;
                lp.rightMargin = consumedRight;
                lp.bottomMargin = consumedBottom;
                lp.leftMargin = consumedLeft;
                this.mContentRoot.setLayoutParams(lp);
                if (insets == null) {
                    requestApplyInsets();
                }
            }
            if (insets != null) {
                insets = insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft() - consumedLeft, insets.getSystemWindowInsetTop() - consumedTop, insets.getSystemWindowInsetRight() - consumedRight, insets.getSystemWindowInsetBottom() - consumedBottom);
            }
        }
        if (insets != null) {
            return insets.consumeStableInsets();
        }
        return insets;
    }

    private int calculateStatusBarColor() {
        return calculateStatusBarColor(this.mWindow.getAttributes().flags, this.mSemiTransparentStatusBarColor, this.mWindow.mStatusBarColor);
    }

    public static int calculateStatusBarColor(int flags, int semiTransparentStatusBarColor, int statusBarColor) {
        if ((67108864 & flags) != 0) {
            return semiTransparentStatusBarColor;
        }
        if ((Integer.MIN_VALUE & flags) != 0) {
            return statusBarColor;
        }
        return -16777216;
    }

    private int getCurrentColor(ColorViewState state) {
        if (state.visible) {
            return state.color;
        }
        return 0;
    }

    private void updateColorViewInt(ColorViewState state, int sysUiVis, int color, int dividerColor, int size, boolean verticalBar, boolean seascape, int sideMargin, boolean animate, boolean force) {
        state.present = state.attributes.isPresent(sysUiVis, this.mWindow.getAttributes().flags, force);
        boolean show = state.attributes.isVisible(state.present, color, this.mWindow.getAttributes().flags, force);
        boolean showView = show && (isResizing() ^ 1) != 0 && size > 0;
        if (state == this.mNavigationColorViewState) {
            if (showView != this.mLastShowNavigationBarView) {
                this.mLastShowNavigationBarView = showView;
                this.mWindow.setDrawNavigatinBarBackground(showView);
            }
            if (PhoneWindow.DEBUG_OPPO_SYSTEMBAR) {
                Log.d(TAG, "updateColorViewInt sysUiVis:" + Integer.toHexString(sysUiVis) + " flags:" + Integer.toHexString(this.mWindow.getAttributes().flags) + " force:" + force + " color:" + Integer.toHexString(color) + " show:" + show + " showView:" + showView + " size:" + size + " isResizing():" + isResizing() + " animate:" + animate + " window@" + this.mWindow + " DecorView@" + this);
            }
        }
        boolean visibilityChanged = false;
        View view = state.view;
        int resolvedHeight = verticalBar ? -1 : size;
        int resolvedWidth = verticalBar ? size : -1;
        int resolvedGravity = verticalBar ? seascape ? state.attributes.seascapeGravity : state.attributes.horizontalGravity : state.attributes.verticalGravity;
        FrameLayout.LayoutParams lp;
        if (view != null) {
            int vis = showView ? 0 : 4;
            visibilityChanged = state.targetVisibility != vis;
            state.targetVisibility = vis;
            lp = (FrameLayout.LayoutParams) view.getLayoutParams();
            int rightMargin = seascape ? 0 : sideMargin;
            int leftMargin = seascape ? sideMargin : 0;
            if (!(lp.height == resolvedHeight && lp.width == resolvedWidth && lp.gravity == resolvedGravity && lp.rightMargin == rightMargin && lp.leftMargin == leftMargin)) {
                lp.height = resolvedHeight;
                lp.width = resolvedWidth;
                lp.gravity = resolvedGravity;
                lp.rightMargin = rightMargin;
                lp.leftMargin = leftMargin;
                view.setLayoutParams(lp);
            }
            if (showView) {
                setColor(view, color, dividerColor, verticalBar, seascape);
            }
        } else if (showView) {
            view = new View(this.mContext);
            state.view = view;
            setColor(view, color, dividerColor, verticalBar, seascape);
            view.setTransitionName(state.attributes.transitionName);
            view.setId(state.attributes.id);
            visibilityChanged = true;
            view.setVisibility(4);
            state.targetVisibility = 0;
            lp = new FrameLayout.LayoutParams(resolvedWidth, resolvedHeight, resolvedGravity);
            if (seascape) {
                lp.leftMargin = sideMargin;
            } else {
                lp.rightMargin = sideMargin;
            }
            addView(view, (ViewGroup.LayoutParams) lp);
            updateColorViewTranslations();
        }
        if (visibilityChanged) {
            view.animate().cancel();
            if (!animate || (isResizing() ^ 1) == 0) {
                view.setAlpha(1.0f);
                view.setVisibility(showView ? 0 : 4);
            } else if (showView) {
                if (view.getVisibility() != 0) {
                    view.setVisibility(0);
                    view.setAlpha(0.0f);
                }
                view.animate().alpha(1.0f).setInterpolator(this.mShowInterpolator).setDuration((long) this.mBarEnterExitDuration);
            } else {
                final ColorViewState colorViewState = state;
                view.animate().alpha(0.0f).setInterpolator(this.mHideInterpolator).setDuration((long) this.mBarEnterExitDuration).withEndAction(new Runnable() {
                    public void run() {
                        colorViewState.view.setAlpha(1.0f);
                        colorViewState.view.setVisibility(4);
                    }
                });
            }
        }
        state.visible = show;
        state.color = color;
    }

    private static void setColor(View v, int color, int dividerColor, boolean verticalBar, boolean seascape) {
        if (dividerColor != 0) {
            Pair<Boolean, Boolean> dir = (Pair) v.getTag();
            if (dir != null && ((Boolean) dir.first).booleanValue() == verticalBar && ((Boolean) dir.second).booleanValue() == seascape) {
                LayerDrawable d = (LayerDrawable) v.getBackground();
                ((ColorDrawable) ((InsetDrawable) d.getDrawable(1)).getDrawable()).setColor(color);
                ((ColorDrawable) d.getDrawable(0)).setColor(dividerColor);
                return;
            }
            int i;
            int i2;
            int i3;
            int size = Math.round(TypedValue.applyDimension(1, 1.0f, v.getContext().getResources().getDisplayMetrics()));
            Drawable colorDrawable = new ColorDrawable(color);
            if (!verticalBar || (seascape ^ 1) == 0) {
                i = 0;
            } else {
                i = size;
            }
            if (verticalBar) {
                i2 = 0;
            } else {
                i2 = size;
            }
            if (verticalBar && seascape) {
                i3 = size;
            } else {
                i3 = 0;
            }
            InsetDrawable d2 = new InsetDrawable(colorDrawable, i, i2, i3, 0);
            v.setBackground(new LayerDrawable(new Drawable[]{new ColorDrawable(dividerColor), d2}));
            v.setTag(new Pair(Boolean.valueOf(verticalBar), Boolean.valueOf(seascape)));
            return;
        }
        v.setTag(null);
        v.setBackgroundColor(color);
    }

    private void updateColorViewTranslations() {
        int rootScrollY = this.mRootScrollY;
        if (this.mStatusColorViewState.view != null) {
            int i;
            View view = this.mStatusColorViewState.view;
            if (rootScrollY > 0) {
                i = rootScrollY;
            } else {
                i = 0;
            }
            view.setTranslationY((float) i);
        }
        if (this.mNavigationColorViewState.view != null) {
            View view2 = this.mNavigationColorViewState.view;
            if (rootScrollY >= 0) {
                rootScrollY = 0;
            }
            view2.setTranslationY((float) rootScrollY);
        }
    }

    private WindowInsets updateStatusGuard(WindowInsets insets) {
        int i = 0;
        boolean showStatusGuard = false;
        if (this.mPrimaryActionModeView != null && (this.mPrimaryActionModeView.getLayoutParams() instanceof MarginLayoutParams)) {
            MarginLayoutParams mlp = (MarginLayoutParams) this.mPrimaryActionModeView.getLayoutParams();
            boolean mlpChanged = false;
            if (this.mPrimaryActionModeView.isShown()) {
                boolean z;
                if (this.mTempRect == null) {
                    this.mTempRect = new Rect();
                }
                Rect rect = this.mTempRect;
                this.mWindow.mContentParent.computeSystemWindowInsets(insets, rect);
                if (mlp.topMargin != (rect.top == 0 ? insets.getSystemWindowInsetTop() : 0)) {
                    mlpChanged = true;
                    mlp.topMargin = insets.getSystemWindowInsetTop();
                    if (this.mStatusGuard == null) {
                        this.mStatusGuard = new View(this.mContext);
                        this.mStatusGuard.setBackgroundColor(this.mContext.getColor(R.color.input_method_navigation_guard));
                        addView(this.mStatusGuard, indexOfChild(this.mStatusColorViewState.view), new FrameLayout.LayoutParams(-1, mlp.topMargin, 8388659));
                    } else {
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mStatusGuard.getLayoutParams();
                        if (lp.height != mlp.topMargin) {
                            lp.height = mlp.topMargin;
                            this.mStatusGuard.setLayoutParams(lp);
                        }
                    }
                }
                showStatusGuard = this.mStatusGuard != null;
                if ((this.mWindow.getLocalFeaturesPrivate() & 1024) == 0) {
                    z = showStatusGuard;
                } else {
                    z = false;
                }
                insets = insets.consumeSystemWindowInsets(false, z, false, false);
            } else if (mlp.topMargin != 0) {
                mlpChanged = true;
                mlp.topMargin = 0;
            }
            if (mlpChanged) {
                this.mPrimaryActionModeView.setLayoutParams(mlp);
            }
        }
        if (this.mStatusGuard != null) {
            View view = this.mStatusGuard;
            if (!showStatusGuard) {
                i = 8;
            }
            view.setVisibility(i);
        }
        return insets;
    }

    private WindowInsets updateNavigationGuard(WindowInsets insets) {
        if (this.mWindow.getAttributes().type != LayoutParams.TYPE_INPUT_METHOD || (this.mWindow.getAttributes().flags & 33554432) != 0) {
            return insets;
        }
        if (this.mWindow.mContentParent != null && (this.mWindow.mContentParent.getLayoutParams() instanceof MarginLayoutParams)) {
            MarginLayoutParams mlp = (MarginLayoutParams) this.mWindow.mContentParent.getLayoutParams();
            mlp.bottomMargin = insets.getSystemWindowInsetBottom();
            this.mWindow.mContentParent.setLayoutParams(mlp);
        }
        if (this.mNavigationGuard == null) {
            this.mNavigationGuard = new View(this.mContext);
            this.mNavigationGuard.setBackgroundColor(PhoneWindow.NAVIGATION_BAR_COLOR_GRAY);
            addView(this.mNavigationGuard, indexOfChild(this.mNavigationColorViewState.view), new FrameLayout.LayoutParams(-1, insets.getSystemWindowInsetBottom(), 8388691));
        } else {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mNavigationGuard.getLayoutParams();
            lp.height = insets.getSystemWindowInsetBottom();
            this.mNavigationGuard.setLayoutParams(lp);
        }
        updateNavigationGuardColor();
        return insets.consumeSystemWindowInsets(false, false, false, true);
    }

    void updateNavigationGuardColor() {
        int i = 0;
        if (this.mNavigationGuard != null) {
            View view = this.mNavigationGuard;
            if (this.mWindow.getNavigationBarColor() == 0) {
                i = 4;
            }
            view.setVisibility(i);
        }
    }

    public void updatePictureInPictureOutlineProvider(boolean isInPictureInPictureMode) {
        if (this.mIsInPictureInPictureMode != isInPictureInPictureMode) {
            if (isInPictureInPictureMode) {
                WindowControllerCallback callback = this.mWindow.getWindowControllerCallback();
                if (callback != null && callback.isTaskRoot()) {
                    super.setOutlineProvider(PIP_OUTLINE_PROVIDER);
                }
            } else if (getOutlineProvider() != this.mLastOutlineProvider) {
                setOutlineProvider(this.mLastOutlineProvider);
            }
            this.mIsInPictureInPictureMode = isInPictureInPictureMode;
        }
    }

    public void setOutlineProvider(ViewOutlineProvider provider) {
        super.setOutlineProvider(provider);
        this.mLastOutlineProvider = provider;
    }

    private void drawableChanged() {
        if (!this.mChanging) {
            setPadding(this.mFramePadding.left + this.mBackgroundPadding.left, this.mFramePadding.top + this.mBackgroundPadding.top, this.mFramePadding.right + this.mBackgroundPadding.right, this.mFramePadding.bottom + this.mBackgroundPadding.bottom);
            requestLayout();
            invalidate();
            int opacity = -1;
            if (StackId.hasWindowShadow(this.mStackId)) {
                opacity = -3;
            } else {
                Drawable bg = getBackground();
                Drawable fg = getForeground();
                if (bg != null) {
                    if (fg == null) {
                        opacity = bg.getOpacity();
                    } else if (this.mFramePadding.left > 0 || this.mFramePadding.top > 0 || this.mFramePadding.right > 0 || this.mFramePadding.bottom > 0) {
                        opacity = -3;
                    } else {
                        int fop = fg.getOpacity();
                        int bop = bg.getOpacity();
                        if (fop == -1 || bop == -1) {
                            opacity = -1;
                        } else if (fop == 0) {
                            opacity = bop;
                        } else if (bop == 0) {
                            opacity = fop;
                        } else {
                            opacity = Drawable.resolveOpacity(fop, bop);
                        }
                    }
                }
            }
            this.mDefaultOpacity = opacity;
            if (this.mFeatureId < 0) {
                this.mWindow.setDefaultWindowFormat(opacity);
            }
        }
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!(!this.mWindow.hasFeature(0) || (hasWindowFocus ^ 1) == 0 || this.mWindow.mPanelChordingKey == 0)) {
            this.mWindow.closePanel(0);
        }
        Window.Callback cb = this.mWindow.getCallback();
        if (!(cb == null || (this.mWindow.isDestroyed() ^ 1) == 0 || this.mFeatureId >= 0)) {
            cb.onWindowFocusChanged(hasWindowFocus);
        }
        if (this.mPrimaryActionMode != null) {
            this.mPrimaryActionMode.onWindowFocusChanged(hasWindowFocus);
        }
        if (this.mFloatingActionMode != null) {
            this.mFloatingActionMode.onWindowFocusChanged(hasWindowFocus);
        }
        updateElevation();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window.Callback cb = this.mWindow.getCallback();
        if (!(cb == null || (this.mWindow.isDestroyed() ^ 1) == 0 || this.mFeatureId >= 0)) {
            cb.onAttachedToWindow();
        }
        if (this.mFeatureId == -1) {
            this.mWindow.openPanelsAfterRestore();
        }
        if (!this.mWindowResizeCallbacksAdded) {
            getViewRootImpl().addWindowCallbacks(this);
            this.mWindowResizeCallbacksAdded = true;
        } else if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.onConfigurationChange();
        }
        this.mWindow.onViewRootImplSet(getViewRootImpl());
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Window.Callback cb = this.mWindow.getCallback();
        if (cb != null && this.mFeatureId < 0) {
            cb.onDetachedFromWindow();
        }
        if (this.mWindow.mDecorContentParent != null) {
            this.mWindow.mDecorContentParent.dismissPopups();
        }
        if (this.mPrimaryActionModePopup != null) {
            removeCallbacks(this.mShowPrimaryActionModePopup);
            if (this.mPrimaryActionModePopup.isShowing()) {
                this.mPrimaryActionModePopup.dismiss();
            }
            this.mPrimaryActionModePopup = null;
        }
        if (this.mFloatingToolbar != null) {
            this.mFloatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        PanelFeatureState st = this.mWindow.getPanelState(0, false);
        if (!(st == null || st.menu == null || this.mFeatureId >= 0)) {
            st.menu.close();
        }
        releaseThreadedRenderer();
        if (this.mWindowResizeCallbacksAdded) {
            getViewRootImpl().removeWindowCallbacks(this);
            this.mWindowResizeCallbacksAdded = false;
        }
    }

    public void onCloseSystemDialogs(String reason) {
        if (this.mFeatureId >= 0) {
            this.mWindow.closeAllPanels();
        }
    }

    public SurfaceHolder.Callback2 willYouTakeTheSurface() {
        return this.mFeatureId < 0 ? this.mWindow.mTakeSurfaceCallback : null;
    }

    public InputQueue.Callback willYouTakeTheInputQueue() {
        return this.mFeatureId < 0 ? this.mWindow.mTakeInputQueueCallback : null;
    }

    public void setSurfaceType(int type) {
        this.mWindow.setType(type);
    }

    public void setSurfaceFormat(int format) {
        this.mWindow.setFormat(format);
    }

    public void setSurfaceKeepScreenOn(boolean keepOn) {
        if (keepOn) {
            this.mWindow.addFlags(128);
        } else {
            this.mWindow.clearFlags(128);
        }
    }

    public void onRootViewScrollYChanged(int rootScrollY) {
        this.mRootScrollY = rootScrollY;
        updateColorViewTranslations();
    }

    private ActionMode createActionMode(int type, Callback2 callback, View originatingView) {
        switch (type) {
            case 1:
                return createFloatingActionMode(originatingView, callback);
            default:
                return createStandaloneActionMode(callback);
        }
    }

    private void setHandledActionMode(ActionMode mode) {
        if (mode.getType() == 0) {
            setHandledPrimaryActionMode(mode);
        } else if (mode.getType() == 1) {
            setHandledFloatingActionMode(mode);
        }
    }

    private ActionMode createStandaloneActionMode(ActionMode.Callback callback) {
        endOnGoingFadeAnimation();
        cleanupPrimaryActionMode();
        if (this.mPrimaryActionModeView == null || (this.mPrimaryActionModeView.isAttachedToWindow() ^ 1) != 0) {
            if (this.mWindow.isFloating()) {
                Context actionBarContext;
                TypedValue outValue = new TypedValue();
                Theme baseTheme = this.mContext.getTheme();
                baseTheme.resolveAttribute(R.attr.actionBarTheme, outValue, true);
                if (outValue.resourceId != 0) {
                    Theme actionBarTheme = this.mContext.getResources().newTheme();
                    actionBarTheme.setTo(baseTheme);
                    actionBarTheme.applyStyle(outValue.resourceId, true);
                    actionBarContext = new ContextThemeWrapper(this.mContext, 0);
                    actionBarContext.getTheme().setTo(actionBarTheme);
                } else {
                    actionBarContext = this.mContext;
                }
                this.mPrimaryActionModeView = new ActionBarContextView(actionBarContext);
                this.mPrimaryActionModePopup = new PopupWindow(actionBarContext, null, (int) R.attr.actionModePopupWindowStyle);
                this.mPrimaryActionModePopup.setWindowLayoutType(2);
                this.mPrimaryActionModePopup.setContentView(this.mPrimaryActionModeView);
                this.mPrimaryActionModePopup.setWidth(-1);
                actionBarContext.getTheme().resolveAttribute(R.attr.actionBarSize, outValue, true);
                this.mPrimaryActionModeView.setContentHeight(TypedValue.complexToDimensionPixelSize(outValue.data, actionBarContext.getResources().getDisplayMetrics()));
                this.mPrimaryActionModePopup.setHeight(-2);
                this.mShowPrimaryActionModePopup = new Runnable() {
                    public void run() {
                        DecorView.this.mPrimaryActionModePopup.showAtLocation(DecorView.this.mPrimaryActionModeView.getApplicationWindowToken(), 55, 0, 0);
                        DecorView.this.endOnGoingFadeAnimation();
                        if (DecorView.this.shouldAnimatePrimaryActionModeView()) {
                            DecorView.this.mFadeAnim = ObjectAnimator.ofFloat(DecorView.this.mPrimaryActionModeView, View.ALPHA, new float[]{0.0f, 1.0f});
                            DecorView.this.mFadeAnim.addListener(new AnimatorListenerAdapter() {
                                public void onAnimationStart(Animator animation) {
                                    DecorView.this.mPrimaryActionModeView.setVisibility(0);
                                }

                                public void onAnimationEnd(Animator animation) {
                                    DecorView.this.mPrimaryActionModeView.setAlpha(1.0f);
                                    DecorView.this.mFadeAnim = null;
                                }
                            });
                            DecorView.this.mFadeAnim.start();
                            return;
                        }
                        DecorView.this.mPrimaryActionModeView.setAlpha(1.0f);
                        DecorView.this.mPrimaryActionModeView.setVisibility(0);
                    }
                };
            } else {
                ViewStub stub = (ViewStub) findViewById(R.id.action_mode_bar_stub);
                if (stub != null) {
                    this.mPrimaryActionModeView = (ActionBarContextView) stub.inflate();
                    this.mPrimaryActionModePopup = null;
                }
            }
        }
        if (this.mPrimaryActionModeView == null) {
            return null;
        }
        boolean z;
        this.mPrimaryActionModeView.killMode();
        Context context = this.mPrimaryActionModeView.getContext();
        ActionBarContextView actionBarContextView = this.mPrimaryActionModeView;
        if (this.mPrimaryActionModePopup == null) {
            z = true;
        } else {
            z = false;
        }
        return new StandaloneActionMode(context, actionBarContextView, callback, z);
    }

    private void endOnGoingFadeAnimation() {
        if (this.mFadeAnim != null) {
            this.mFadeAnim.end();
        }
    }

    private void setHandledPrimaryActionMode(ActionMode mode) {
        endOnGoingFadeAnimation();
        this.mPrimaryActionMode = mode;
        this.mPrimaryActionMode.invalidate();
        this.mPrimaryActionModeView.initForMode(this.mPrimaryActionMode);
        if (this.mPrimaryActionModePopup != null) {
            post(this.mShowPrimaryActionModePopup);
        } else if (shouldAnimatePrimaryActionModeView()) {
            this.mFadeAnim = ObjectAnimator.ofFloat(this.mPrimaryActionModeView, View.ALPHA, new float[]{0.0f, 1.0f});
            this.mFadeAnim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    DecorView.this.mPrimaryActionModeView.setVisibility(0);
                }

                public void onAnimationEnd(Animator animation) {
                    DecorView.this.mPrimaryActionModeView.setAlpha(1.0f);
                    DecorView.this.mFadeAnim = null;
                }
            });
            this.mFadeAnim.start();
        } else {
            this.mPrimaryActionModeView.setAlpha(1.0f);
            this.mPrimaryActionModeView.setVisibility(0);
        }
        this.mPrimaryActionModeView.sendAccessibilityEvent(32);
    }

    boolean shouldAnimatePrimaryActionModeView() {
        return isLaidOut();
    }

    private ActionMode createFloatingActionMode(View originatingView, Callback2 callback) {
        if (this.mFloatingActionMode != null) {
            this.mFloatingActionMode.finish();
        }
        cleanupFloatingActionModeViews();
        this.mFloatingToolbar = new FloatingToolbar(this.mWindow);
        final FloatingActionMode mode = new FloatingActionMode(this.mContext, callback, originatingView, this.mFloatingToolbar);
        this.mFloatingActionModeOriginatingView = originatingView;
        this.mFloatingToolbarPreDrawListener = new OnPreDrawListener() {
            public boolean onPreDraw() {
                mode.updateViewLocationInWindow();
                return true;
            }
        };
        return mode;
    }

    private void setHandledFloatingActionMode(ActionMode mode) {
        this.mFloatingActionMode = mode;
        this.mFloatingActionMode.invalidate();
        this.mFloatingActionModeOriginatingView.getViewTreeObserver().addOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
    }

    void enableCaption(boolean attachedAndVisible) {
        if (this.mHasCaption != attachedAndVisible) {
            this.mHasCaption = attachedAndVisible;
            if (getForeground() != null) {
                drawableChanged();
            }
        }
    }

    void setWindow(PhoneWindow phoneWindow) {
        this.mWindow = phoneWindow;
        Context context = getContext();
        if (context instanceof DecorContext) {
            ((DecorContext) context).setPhoneWindow(this.mWindow);
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int workspaceId = getStackId();
        if (this.mStackId != workspaceId) {
            this.mStackId = workspaceId;
            if (this.mDecorCaptionView == null && StackId.hasWindowDecor(this.mStackId)) {
                this.mDecorCaptionView = createDecorCaptionView(this.mWindow.getLayoutInflater());
                if (this.mDecorCaptionView != null) {
                    if (this.mDecorCaptionView.getParent() == null) {
                        addView(this.mDecorCaptionView, 0, new ViewGroup.LayoutParams(-1, -1));
                    }
                    removeView(this.mContentRoot);
                    if (!(this.mContentRoot == null || this.mContentRoot.getParent() == null)) {
                        if (this.mContentRoot.getParent() instanceof ViewGroup) {
                            ((ViewGroup) this.mContentRoot.getParent()).removeView(this.mContentRoot);
                        }
                        this.mContentRoot.oppoAssignParent(null);
                    }
                    this.mDecorCaptionView.addView(this.mContentRoot, this.mContentRoot.getLayoutParams());
                }
            } else if (this.mDecorCaptionView != null) {
                this.mDecorCaptionView.onConfigurationChanged(StackId.hasWindowDecor(this.mStackId));
                enableCaption(StackId.hasWindowDecor(workspaceId));
            }
        }
        updateAvailableWidth();
        initializeElevation();
    }

    void onResourcesLoaded(LayoutInflater inflater, int layoutResource) {
        this.mStackId = getStackId();
        if (this.mBackdropFrameRenderer != null) {
            loadBackgroundDrawablesIfNeeded();
            this.mBackdropFrameRenderer.onResourcesLoaded(this, this.mResizingBackgroundDrawable, this.mCaptionBackgroundDrawable, this.mUserCaptionBackgroundDrawable, getCurrentColor(this.mStatusColorViewState), getCurrentColor(this.mNavigationColorViewState));
        }
        this.mDecorCaptionView = createDecorCaptionView(inflater);
        View root = inflater.inflate(layoutResource, null);
        if (this.mDecorCaptionView != null) {
            if (this.mDecorCaptionView.getParent() == null) {
                addView(this.mDecorCaptionView, new ViewGroup.LayoutParams(-1, -1));
            }
            this.mDecorCaptionView.addView(root, new MarginLayoutParams(-1, -1));
        } else {
            addView(root, 0, new ViewGroup.LayoutParams(-1, -1));
        }
        this.mContentRoot = (ViewGroup) root;
        initializeElevation();
    }

    private void loadBackgroundDrawablesIfNeeded() {
        if (this.mResizingBackgroundDrawable == null) {
            this.mResizingBackgroundDrawable = getResizingBackgroundDrawable(getContext(), this.mWindow.mBackgroundResource, this.mWindow.mBackgroundFallbackResource, !this.mWindow.isTranslucent() ? this.mWindow.isShowingWallpaper() : true);
            if (this.mResizingBackgroundDrawable == null) {
                Log.w(this.mLogTag, "Failed to find background drawable for PhoneWindow=" + this.mWindow);
            }
        }
        if (this.mCaptionBackgroundDrawable == null) {
            this.mCaptionBackgroundDrawable = getContext().getDrawable(R.drawable.decor_caption_title_focused);
        }
        if (this.mResizingBackgroundDrawable != null) {
            this.mLastBackgroundDrawableCb = this.mResizingBackgroundDrawable.getCallback();
            this.mResizingBackgroundDrawable.setCallback(null);
        }
    }

    private DecorCaptionView createDecorCaptionView(LayoutInflater inflater) {
        boolean z = true;
        DecorCaptionView decorCaptionView = null;
        for (int i = getChildCount() - 1; i >= 0 && decorCaptionView == null; i--) {
            View view = getChildAt(i);
            if (view instanceof DecorCaptionView) {
                decorCaptionView = (DecorCaptionView) view;
                removeViewAt(i);
            }
        }
        LayoutParams attrs = this.mWindow.getAttributes();
        boolean isApplication = (attrs.type == 1 || attrs.type == 2) ? true : attrs.type == 4;
        if (!this.mWindow.isFloating() && isApplication && StackId.hasWindowDecor(this.mStackId)) {
            if (decorCaptionView == null) {
                decorCaptionView = inflateDecorCaptionView(inflater);
            }
            decorCaptionView.setPhoneWindow(this.mWindow, true);
        } else {
            decorCaptionView = null;
        }
        if (decorCaptionView == null) {
            z = false;
        }
        enableCaption(z);
        return decorCaptionView;
    }

    private DecorCaptionView inflateDecorCaptionView(LayoutInflater inflater) {
        Context context = getContext();
        DecorCaptionView view = (DecorCaptionView) LayoutInflater.from(context).inflate((int) R.layout.decor_caption, null);
        setDecorCaptionShade(context, view);
        return view;
    }

    private void setDecorCaptionShade(Context context, DecorCaptionView view) {
        switch (this.mWindow.getDecorCaptionShade()) {
            case 1:
                setLightDecorCaptionShade(view);
                return;
            case 2:
                setDarkDecorCaptionShade(view);
                return;
            default:
                TypedValue value = new TypedValue();
                context.getTheme().resolveAttribute(R.attr.colorPrimary, value, true);
                if (((double) Color.luminance(value.data)) < 0.5d) {
                    setLightDecorCaptionShade(view);
                    return;
                } else {
                    setDarkDecorCaptionShade(view);
                    return;
                }
        }
    }

    void updateDecorCaptionShade() {
        if (this.mDecorCaptionView != null) {
            setDecorCaptionShade(getContext(), this.mDecorCaptionView);
        }
    }

    private void setLightDecorCaptionShade(DecorCaptionView view) {
        view.findViewById(R.id.maximize_window).setBackgroundResource(R.drawable.decor_maximize_button_light);
        view.findViewById(R.id.close_window).setBackgroundResource(R.drawable.decor_close_button_light);
    }

    private void setDarkDecorCaptionShade(DecorCaptionView view) {
        view.findViewById(R.id.maximize_window).setBackgroundResource(R.drawable.decor_maximize_button_dark);
        view.findViewById(R.id.close_window).setBackgroundResource(R.drawable.decor_close_button_dark);
    }

    public static Drawable getResizingBackgroundDrawable(Context context, int backgroundRes, int backgroundFallbackRes, boolean windowTranslucent) {
        if (backgroundRes != 0) {
            Drawable drawable = context.getDrawable(backgroundRes);
            if (drawable != null) {
                return enforceNonTranslucentBackground(drawable, windowTranslucent);
            }
        }
        if (backgroundFallbackRes != 0) {
            Drawable fallbackDrawable = context.getDrawable(backgroundFallbackRes);
            if (fallbackDrawable != null) {
                return enforceNonTranslucentBackground(fallbackDrawable, windowTranslucent);
            }
        }
        return new ColorDrawable(-16777216);
    }

    private static Drawable enforceNonTranslucentBackground(Drawable drawable, boolean windowTranslucent) {
        if (!windowTranslucent && (drawable instanceof ColorDrawable)) {
            ColorDrawable colorDrawable = (ColorDrawable) drawable;
            int color = colorDrawable.getColor();
            if (Color.alpha(color) != 255) {
                ColorDrawable copy = (ColorDrawable) colorDrawable.getConstantState().newDrawable().mutate();
                copy.setColor(Color.argb(255, Color.red(color), Color.green(color), Color.blue(color)));
                return copy;
            }
        }
        return drawable;
    }

    private int getStackId() {
        int workspaceId = -1;
        WindowControllerCallback callback = this.mWindow.getWindowControllerCallback();
        if (callback != null) {
            try {
                workspaceId = callback.getWindowStackId();
            } catch (RemoteException e) {
                Log.e(this.mLogTag, "Failed to get the workspace ID of a PhoneWindow.");
            }
        }
        if (workspaceId == -1) {
            return 1;
        }
        return workspaceId;
    }

    void clearContentView() {
        if (this.mDecorCaptionView != null) {
            this.mDecorCaptionView.removeContentView();
            return;
        }
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View v = getChildAt(i);
            if (!(v == this.mStatusColorViewState.view || v == this.mNavigationColorViewState.view || v == this.mStatusGuard || v == this.mNavigationGuard)) {
                removeViewAt(i);
            }
        }
    }

    public void onWindowSizeIsChanging(Rect newBounds, boolean fullscreen, Rect systemInsets, Rect stableInsets) {
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.setTargetRect(newBounds, fullscreen, systemInsets, stableInsets);
        }
    }

    public void onWindowDragResizeStart(Rect initialBounds, boolean fullscreen, Rect systemInsets, Rect stableInsets, int resizeMode) {
        if (this.mWindow.isDestroyed()) {
            releaseThreadedRenderer();
        } else if (this.mBackdropFrameRenderer == null) {
            ThreadedRenderer renderer = getThreadedRenderer();
            if (renderer != null) {
                loadBackgroundDrawablesIfNeeded();
                this.mBackdropFrameRenderer = new BackdropFrameRenderer(this, renderer, initialBounds, this.mResizingBackgroundDrawable, this.mCaptionBackgroundDrawable, this.mUserCaptionBackgroundDrawable, getCurrentColor(this.mStatusColorViewState), getCurrentColor(this.mNavigationColorViewState), fullscreen, systemInsets, stableInsets, resizeMode);
                updateElevation();
                updateColorViews(null, false);
            }
            this.mResizeMode = resizeMode;
            getViewRootImpl().requestInvalidateRootRenderNode();
        }
    }

    public void onWindowDragResizeEnd() {
        releaseThreadedRenderer();
        updateColorViews(null, false);
        this.mResizeMode = -1;
        getViewRootImpl().requestInvalidateRootRenderNode();
    }

    public boolean onContentDrawn(int offsetX, int offsetY, int sizeX, int sizeY) {
        if (this.mBackdropFrameRenderer == null) {
            return false;
        }
        return this.mBackdropFrameRenderer.onContentDrawn(offsetX, offsetY, sizeX, sizeY);
    }

    public void onRequestDraw(boolean reportNextDraw) {
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.onRequestDraw(reportNextDraw);
        } else if (reportNextDraw && isAttachedToWindow()) {
            getViewRootImpl().reportDrawFinish();
        }
    }

    public void onPostDraw(DisplayListCanvas canvas) {
        drawResizingShadowIfNeeded(canvas);
    }

    private void initResizingPaints() {
        int middleColor = (this.mContext.getResources().getColor(R.color.resize_shadow_start_color, null) + this.mContext.getResources().getColor(R.color.resize_shadow_end_color, null)) / 2;
        this.mHorizontalResizeShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, (float) this.mResizeShadowSize, new int[]{startColor, middleColor, endColor}, new float[]{0.0f, 0.3f, 1.0f}, TileMode.CLAMP));
        this.mVerticalResizeShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, (float) this.mResizeShadowSize, 0.0f, new int[]{startColor, middleColor, endColor}, new float[]{0.0f, 0.3f, 1.0f}, TileMode.CLAMP));
    }

    private void drawResizingShadowIfNeeded(DisplayListCanvas canvas) {
        if (this.mResizeMode == 1 && !this.mWindow.mIsFloating && !this.mWindow.isTranslucent() && !this.mWindow.isShowingWallpaper()) {
            canvas.save();
            canvas.translate(0.0f, (float) (getHeight() - this.mFrameOffsets.bottom));
            canvas.drawRect(0.0f, 0.0f, (float) getWidth(), (float) this.mResizeShadowSize, this.mHorizontalResizeShadowPaint);
            canvas.restore();
            canvas.save();
            canvas.translate((float) (getWidth() - this.mFrameOffsets.right), 0.0f);
            canvas.drawRect(0.0f, 0.0f, (float) this.mResizeShadowSize, (float) getHeight(), this.mVerticalResizeShadowPaint);
            canvas.restore();
        }
    }

    private void releaseThreadedRenderer() {
        if (!(this.mResizingBackgroundDrawable == null || this.mLastBackgroundDrawableCb == null)) {
            this.mResizingBackgroundDrawable.setCallback(this.mLastBackgroundDrawableCb);
            this.mLastBackgroundDrawableCb = null;
        }
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.releaseRenderer();
            this.mBackdropFrameRenderer = null;
            updateElevation();
        }
    }

    private boolean isResizing() {
        return this.mBackdropFrameRenderer != null;
    }

    private void initializeElevation() {
        this.mAllowUpdateElevation = false;
        updateElevation();
    }

    private void updateElevation() {
        float elevation = 0.0f;
        boolean wasAdjustedForStack = this.mElevationAdjustedForStack;
        this.mElevationChange = false;
        if (this.mStackId == 2 && (isResizing() ^ 1) != 0) {
            elevation = (float) (hasWindowFocus() ? 20 : 5);
            if (!this.mAllowUpdateElevation) {
                this.mElevationChange = true;
            }
            elevation = dipToPx(5.0f);
            this.mElevationAdjustedForStack = true;
        } else if (this.mStackId == 4) {
            elevation = dipToPx(5.0f);
            this.mElevationAdjustedForStack = true;
        } else {
            this.mElevationAdjustedForStack = false;
        }
        if (!wasAdjustedForStack && !this.mElevationAdjustedForStack) {
            return;
        }
        if (getElevation() != elevation || this.mElevationChange) {
            this.mWindow.setElevation(elevation);
        }
    }

    boolean isShowingCaption() {
        return this.mDecorCaptionView != null ? this.mDecorCaptionView.isCaptionShowing() : false;
    }

    int getCaptionHeight() {
        return isShowingCaption() ? this.mDecorCaptionView.getCaptionHeight() : 0;
    }

    private float dipToPx(float dip) {
        return TypedValue.applyDimension(1, dip, getResources().getDisplayMetrics());
    }

    void setUserCaptionBackgroundDrawable(Drawable drawable) {
        this.mUserCaptionBackgroundDrawable = drawable;
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.setUserCaptionBackgroundDrawable(drawable);
        }
    }

    private static String getTitleSuffix(LayoutParams params) {
        if (params == null) {
            return "";
        }
        String[] split = params.getTitle().toString().split("\\.");
        if (split.length > 0) {
            return split[split.length - 1];
        }
        return "";
    }

    void updateLogTag(LayoutParams params) {
        this.mLogTag = "DecorView[" + getTitleSuffix(params) + "]";
    }

    private void updateAvailableWidth() {
        Resources res = getResources();
        this.mAvailableWidth = TypedValue.applyDimension(1, (float) res.getConfiguration().screenWidthDp, res.getDisplayMetrics());
    }

    public void requestKeyboardShortcuts(List<KeyboardShortcutGroup> list, int deviceId) {
        PanelFeatureState st = this.mWindow.getPanelState(0, false);
        Menu menu = st != null ? st.menu : null;
        if (!this.mWindow.isDestroyed() && this.mWindow.getCallback() != null) {
            this.mWindow.getCallback().onProvideKeyboardShortcuts(list, menu, deviceId);
        }
    }

    public void dispatchPointerCaptureChanged(boolean hasCapture) {
        super.dispatchPointerCaptureChanged(hasCapture);
        if (!this.mWindow.isDestroyed() && this.mWindow.getCallback() != null) {
            this.mWindow.getCallback().onPointerCaptureChanged(hasCapture);
        }
    }

    public int getAccessibilityViewId() {
        return 2147483646;
    }

    public String toString() {
        return "DecorView@" + Integer.toHexString(hashCode()) + "[" + getTitleSuffix(this.mWindow.getAttributes()) + "]";
    }

    public void setWindowNavigationBarColor(int color) {
        this.mWindow.setWindowPreNavigationBarColor(color);
    }

    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        boolean isFullScreen = params.width == -1 ? params.height == -1 : false;
        if (PhoneWindow.DEBUG_OPPO_SYSTEMBAR) {
            Log.d(TAG, "addView child:" + child + " index:" + index + " mActivityName:" + this.mWindow.mActivityName + " params:" + params + " width:" + params.width + " height:" + params.height + " DecorView@" + this);
        }
        if (!isFullScreen) {
            return;
        }
        if ("c8.Mrg".equals(child.getClass().getName()) || "com.wdzj.app.module.base.activity.BaseWebViewActivity$FullscreenHolder".equals(child.getClass().getName()) || ("com.kpie.android.ui.VideoPlayNormalActivity".equals(this.mWindow.mActivityName) && "android.widget.FrameLayout".equals(child.getClass().getName()))) {
            this.mAddView = child;
            this.mWindow.mHasAddFullScreenView = true;
            this.mWindow.updateNavigationBarLightFlag();
        }
    }

    public void removeView(View view) {
        super.removeView(view);
        if (this.mAddView != null && this.mAddView == view) {
            if (PhoneWindow.DEBUG_OPPO_SYSTEMBAR) {
                Log.d(TAG, "removeView child:" + view + " DecorView@" + this);
            }
            this.mWindow.mHasAddFullScreenView = false;
            this.mWindow.updateNavigationBarLightFlag();
        }
    }
}
