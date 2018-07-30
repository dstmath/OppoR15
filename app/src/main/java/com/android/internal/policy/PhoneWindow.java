package com.android.internal.policy;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.Spanned;
import android.text.TextUtils;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AndroidRuntimeException;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.IRotationWatcher.Stub;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputQueue;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.KeyEvent.DispatcherState;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.SurfaceHolder.Callback2;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewManager;
import android.view.ViewRootImpl;
import android.view.ViewRootImpl.ActivityConfigCallback;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.view.menu.ContextMenuBuilder;
import com.android.internal.view.menu.IconMenuPresenter;
import com.android.internal.view.menu.ListMenuPresenter;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuBuilder.Callback;
import com.android.internal.view.menu.MenuDialogHelper;
import com.android.internal.view.menu.MenuHelper;
import com.android.internal.view.menu.MenuPresenter;
import com.android.internal.view.menu.MenuView;
import com.android.internal.widget.DecorContentParent;
import com.android.internal.widget.SwipeDismissLayout;
import com.android.internal.widget.SwipeDismissLayout.OnDismissedListener;
import com.android.internal.widget.SwipeDismissLayout.OnSwipeProgressChangedListener;
import com.oppo.debug.InputLog;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PhoneWindow extends Window implements Callback {
    private static final String ACTION_BAR_TAG = "android:ActionBar";
    private static final int CUSTOM_TITLE_COMPATIBLE_FEATURES = 13505;
    private static final boolean DEBUG = false;
    public static boolean DEBUG_OPPO_SYSTEMBAR = false;
    private static final int DEFAULT_BACKGROUND_FADE_DURATION_MS = 300;
    static final int FLAG_RESOURCE_SET_ICON = 1;
    static final int FLAG_RESOURCE_SET_ICON_FALLBACK = 4;
    static final int FLAG_RESOURCE_SET_LOGO = 2;
    private static final String FOCUSED_ID_TAG = "android:focusedViewId";
    private static final int NAVIGATION_BAR_COLOR_BLACK = -16777216;
    public static final int NAVIGATION_BAR_COLOR_GRAY = -263173;
    private static final String PANELS_TAG = "android:Panels";
    private static final String TAG = "PhoneWindow";
    private static final Transition USE_DEFAULT_TRANSITION = new TransitionSet();
    private static final String VIEWS_TAG = "android:views";
    static final RotationWatcher sRotationWatcher = new RotationWatcher();
    private ActionMenuPresenterCallback mActionMenuPresenterCallback;
    private ActivityConfigCallback mActivityConfigCallback;
    String mActivityName;
    private int mAdapationStatusBarColor;
    private Boolean mAllowEnterTransitionOverlap;
    private Boolean mAllowReturnTransitionOverlap;
    private boolean mAlwaysReadCloseOnTouchAttr;
    private AudioManager mAudioManager;
    private Drawable mBackgroundDrawable;
    private long mBackgroundFadeDurationMillis;
    int mBackgroundFallbackResource;
    int mBackgroundResource;
    private ProgressBar mCircularProgressBar;
    private boolean mClipToOutline;
    private boolean mClosingActionMenu;
    ViewGroup mContentParent;
    private boolean mContentParentExplicitlySet;
    private Scene mContentScene;
    ContextMenuBuilder mContextMenu;
    final PhoneWindowMenuCallback mContextMenuCallback;
    MenuHelper mContextMenuHelper;
    private DecorView mDecor;
    private int mDecorCaptionShade;
    DecorContentParent mDecorContentParent;
    private DrawableFeatureState[] mDrawables;
    boolean mDrawsNavBarBackground;
    private float mElevation;
    private Transition mEnterTransition;
    private Transition mExitTransition;
    TypedValue mFixedHeightMajor;
    TypedValue mFixedHeightMinor;
    TypedValue mFixedWidthMajor;
    TypedValue mFixedWidthMinor;
    private boolean mForceDecorInstall;
    boolean mForceWindowDrawsNavBarBackground;
    private boolean mForcedNavigationBarColor;
    private boolean mForcedStatusBarColor;
    private int mFrameResource;
    boolean mHasAddFullScreenView;
    boolean mHasPaletteColor;
    private ProgressBar mHorizontalProgressBar;
    int mIconRes;
    private int mInvalidatePanelMenuFeatures;
    private boolean mInvalidatePanelMenuPosted;
    private final Runnable mInvalidatePanelMenuRunnable;
    boolean mIsFloating;
    private boolean mIsStartingWindow;
    private boolean mIsTranslucent;
    private KeyguardManager mKeyguardManager;
    private LayoutInflater mLayoutInflater;
    private ImageView mLeftIconView;
    private boolean mLoadElevation;
    int mLogoRes;
    private MediaController mMediaController;
    final TypedValue mMinWidthMajor;
    final TypedValue mMinWidthMinor;
    boolean mNavColorIsAppCustom;
    int mNavigationBarColor;
    int mNavigationBarDividerColor;
    boolean mNeedPaletteColor;
    int mPanelChordingKey;
    private PanelMenuPresenterCallback mPanelMenuPresenterCallback;
    private PanelFeatureState[] mPanels;
    PanelFeatureState mPreparedPanel;
    private Transition mReenterTransition;
    int mResourcesSetFlags;
    private Transition mReturnTransition;
    private ImageView mRightIconView;
    private boolean mRunningCts;
    private Transition mSharedElementEnterTransition;
    private Transition mSharedElementExitTransition;
    private Transition mSharedElementReenterTransition;
    private Transition mSharedElementReturnTransition;
    private Boolean mSharedElementsUseOverlay;
    int mStatusBarColor;
    private boolean mSupportsPictureInPicture;
    InputQueue.Callback mTakeInputQueueCallback;
    Callback2 mTakeSurfaceCallback;
    private int mTextColor;
    private int mTheme;
    private CharSequence mTitle;
    private int mTitleColor;
    private TextView mTitleView;
    private TransitionManager mTransitionManager;
    private int mUiOptions;
    private boolean mUseDecorContext;
    private int mVolumeControlStreamType;

    private final class ActionMenuPresenterCallback implements MenuPresenter.Callback {
        /* synthetic */ ActionMenuPresenterCallback(PhoneWindow this$0, ActionMenuPresenterCallback -this1) {
            this();
        }

        private ActionMenuPresenterCallback() {
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            Window.Callback cb = PhoneWindow.this.getCallback();
            if (cb == null) {
                return false;
            }
            cb.onMenuOpened(8, subMenu);
            return true;
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            PhoneWindow.this.checkCloseActionMenu(menu);
        }
    }

    private static final class DrawableFeatureState {
        int alpha = 255;
        Drawable child;
        Drawable cur;
        int curAlpha = 255;
        Drawable def;
        final int featureId;
        Drawable local;
        int resid;
        Uri uri;

        DrawableFeatureState(int _featureId) {
            this.featureId = _featureId;
        }
    }

    static final class PanelFeatureState {
        int background;
        View createdPanelView;
        DecorView decorView;
        int featureId;
        Bundle frozenActionViewState;
        Bundle frozenMenuState;
        int fullBackground;
        int gravity;
        IconMenuPresenter iconMenuPresenter;
        boolean isCompact;
        boolean isHandled;
        boolean isInExpandedMode;
        boolean isOpen;
        boolean isPrepared;
        ListMenuPresenter listMenuPresenter;
        int listPresenterTheme;
        MenuBuilder menu;
        public boolean qwertyMode;
        boolean refreshDecorView = false;
        boolean refreshMenuContent;
        View shownPanelView;
        boolean wasLastExpanded;
        boolean wasLastOpen;
        int windowAnimations;
        int x;
        int y;

        private static class SavedState implements Parcelable {
            public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
                public SavedState createFromParcel(Parcel in) {
                    return SavedState.readFromParcel(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
            int featureId;
            boolean isInExpandedMode;
            boolean isOpen;
            Bundle menuState;

            /* synthetic */ SavedState(SavedState -this0) {
                this();
            }

            private SavedState() {
            }

            public int describeContents() {
                return 0;
            }

            public void writeToParcel(Parcel dest, int flags) {
                int i = 1;
                dest.writeInt(this.featureId);
                dest.writeInt(this.isOpen ? 1 : 0);
                if (!this.isInExpandedMode) {
                    i = 0;
                }
                dest.writeInt(i);
                if (this.isOpen) {
                    dest.writeBundle(this.menuState);
                }
            }

            private static SavedState readFromParcel(Parcel source) {
                boolean z = true;
                SavedState savedState = new SavedState();
                savedState.featureId = source.readInt();
                savedState.isOpen = source.readInt() == 1;
                if (source.readInt() != 1) {
                    z = false;
                }
                savedState.isInExpandedMode = z;
                if (savedState.isOpen) {
                    savedState.menuState = source.readBundle();
                }
                return savedState;
            }
        }

        PanelFeatureState(int featureId) {
            this.featureId = featureId;
        }

        public boolean isInListMode() {
            return !this.isInExpandedMode ? this.isCompact : true;
        }

        public boolean hasPanelItems() {
            boolean z = true;
            if (this.shownPanelView == null) {
                return false;
            }
            if (this.createdPanelView != null) {
                return true;
            }
            if (this.isCompact || this.isInExpandedMode) {
                return this.listMenuPresenter.getAdapter().getCount() > 0;
            }
            if (((ViewGroup) this.shownPanelView).getChildCount() <= 0) {
                z = false;
            }
            return z;
        }

        public void clearMenuPresenters() {
            if (this.menu != null) {
                this.menu.removeMenuPresenter(this.iconMenuPresenter);
                this.menu.removeMenuPresenter(this.listMenuPresenter);
            }
            this.iconMenuPresenter = null;
            this.listMenuPresenter = null;
        }

        void setStyle(Context context) {
            TypedArray a = context.obtainStyledAttributes(R.styleable.Theme);
            this.background = a.getResourceId(46, 0);
            this.fullBackground = a.getResourceId(47, 0);
            this.windowAnimations = a.getResourceId(93, 0);
            this.isCompact = a.getBoolean(306, false);
            this.listPresenterTheme = a.getResourceId(307, R.style.Theme_ExpandedMenu);
            a.recycle();
        }

        void setMenu(MenuBuilder menu) {
            if (menu != this.menu) {
                if (this.menu != null) {
                    this.menu.removeMenuPresenter(this.iconMenuPresenter);
                    this.menu.removeMenuPresenter(this.listMenuPresenter);
                }
                this.menu = menu;
                if (menu != null) {
                    if (this.iconMenuPresenter != null) {
                        menu.addMenuPresenter(this.iconMenuPresenter);
                    }
                    if (this.listMenuPresenter != null) {
                        menu.addMenuPresenter(this.listMenuPresenter);
                    }
                }
            }
        }

        MenuView getListMenuView(Context context, MenuPresenter.Callback cb) {
            if (this.menu == null) {
                return null;
            }
            if (!this.isCompact) {
                getIconMenuView(context, cb);
            }
            if (this.listMenuPresenter == null) {
                this.listMenuPresenter = new ListMenuPresenter((int) R.layout.list_menu_item_layout, this.listPresenterTheme);
                this.listMenuPresenter.setCallback(cb);
                this.listMenuPresenter.setId(R.id.list_menu_presenter);
                this.menu.addMenuPresenter(this.listMenuPresenter);
            }
            if (this.iconMenuPresenter != null) {
                this.listMenuPresenter.setItemIndexOffset(this.iconMenuPresenter.getNumActualItemsShown());
            }
            return this.listMenuPresenter.getMenuView(this.decorView);
        }

        MenuView getIconMenuView(Context context, MenuPresenter.Callback cb) {
            if (this.menu == null) {
                return null;
            }
            if (this.iconMenuPresenter == null) {
                this.iconMenuPresenter = new IconMenuPresenter(context);
                this.iconMenuPresenter.setCallback(cb);
                this.iconMenuPresenter.setId(R.id.icon_menu_presenter);
                this.menu.addMenuPresenter(this.iconMenuPresenter);
            }
            return this.iconMenuPresenter.getMenuView(this.decorView);
        }

        Parcelable onSaveInstanceState() {
            SavedState savedState = new SavedState();
            savedState.featureId = this.featureId;
            savedState.isOpen = this.isOpen;
            savedState.isInExpandedMode = this.isInExpandedMode;
            if (this.menu != null) {
                savedState.menuState = new Bundle();
                this.menu.savePresenterStates(savedState.menuState);
            }
            return savedState;
        }

        void onRestoreInstanceState(Parcelable state) {
            SavedState savedState = (SavedState) state;
            this.featureId = savedState.featureId;
            this.wasLastOpen = savedState.isOpen;
            this.wasLastExpanded = savedState.isInExpandedMode;
            this.frozenMenuState = savedState.menuState;
            this.createdPanelView = null;
            this.shownPanelView = null;
            this.decorView = null;
        }

        void applyFrozenState() {
            if (this.menu != null && this.frozenMenuState != null) {
                this.menu.restorePresenterStates(this.frozenMenuState);
                this.frozenMenuState = null;
            }
        }
    }

    private class PanelMenuPresenterCallback implements MenuPresenter.Callback {
        /* synthetic */ PanelMenuPresenterCallback(PhoneWindow this$0, PanelMenuPresenterCallback -this1) {
            this();
        }

        private PanelMenuPresenterCallback() {
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            MenuBuilder parentMenu = menu.getRootMenu();
            boolean isSubMenu = parentMenu != menu;
            PhoneWindow phoneWindow = PhoneWindow.this;
            if (isSubMenu) {
                menu = parentMenu;
            }
            PanelFeatureState panel = phoneWindow.findMenuPanel(menu);
            if (panel == null) {
                return;
            }
            if (isSubMenu) {
                PhoneWindow.this.callOnPanelClosed(panel.featureId, panel, parentMenu);
                PhoneWindow.this.closePanel(panel, true);
                return;
            }
            PhoneWindow.this.closePanel(panel, allMenusAreClosing);
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            if (subMenu == null && PhoneWindow.this.hasFeature(8)) {
                Window.Callback cb = PhoneWindow.this.getCallback();
                if (!(cb == null || (PhoneWindow.this.isDestroyed() ^ 1) == 0)) {
                    cb.onMenuOpened(8, subMenu);
                }
            }
            return true;
        }
    }

    public static final class PhoneWindowMenuCallback implements Callback, MenuPresenter.Callback {
        private static final int FEATURE_ID = 6;
        private boolean mShowDialogForSubmenu;
        private MenuDialogHelper mSubMenuHelper;
        private final PhoneWindow mWindow;

        public PhoneWindowMenuCallback(PhoneWindow window) {
            this.mWindow = window;
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            if (menu.getRootMenu() != menu) {
                onCloseSubMenu(menu);
            }
            if (allMenusAreClosing) {
                Window.Callback callback = this.mWindow.getCallback();
                if (!(callback == null || (this.mWindow.isDestroyed() ^ 1) == 0)) {
                    callback.onPanelClosed(6, menu);
                }
                if (menu == this.mWindow.mContextMenu) {
                    this.mWindow.dismissContextMenu();
                }
                if (this.mSubMenuHelper != null) {
                    this.mSubMenuHelper.dismiss();
                    this.mSubMenuHelper = null;
                }
            }
        }

        private void onCloseSubMenu(MenuBuilder menu) {
            Window.Callback callback = this.mWindow.getCallback();
            if (callback != null && (this.mWindow.isDestroyed() ^ 1) != 0) {
                callback.onPanelClosed(6, menu.getRootMenu());
            }
        }

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            Window.Callback callback = this.mWindow.getCallback();
            if (callback == null || (this.mWindow.isDestroyed() ^ 1) == 0) {
                return false;
            }
            return callback.onMenuItemSelected(6, item);
        }

        public void onMenuModeChange(MenuBuilder menu) {
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            if (subMenu == null) {
                return false;
            }
            subMenu.setCallback(this);
            if (!this.mShowDialogForSubmenu) {
                return false;
            }
            this.mSubMenuHelper = new MenuDialogHelper(subMenu);
            this.mSubMenuHelper.show(null);
            return true;
        }

        public void setShowDialogForSubmenu(boolean enabled) {
            this.mShowDialogForSubmenu = enabled;
        }
    }

    static class RotationWatcher extends Stub {
        private Handler mHandler;
        private boolean mIsWatching;
        private final Runnable mRotationChanged = new Runnable() {
            public void run() {
                RotationWatcher.this.dispatchRotationChanged();
            }
        };
        private final ArrayList<WeakReference<PhoneWindow>> mWindows = new ArrayList();

        RotationWatcher() {
        }

        public void onRotationChanged(int rotation) throws RemoteException {
            this.mHandler.post(this.mRotationChanged);
        }

        public void addWindow(PhoneWindow phoneWindow) {
            synchronized (this.mWindows) {
                if (!this.mIsWatching) {
                    try {
                        WindowManagerHolder.sWindowManager.watchRotation(this, phoneWindow.getContext().getDisplay().getDisplayId());
                        this.mHandler = new Handler();
                        this.mIsWatching = true;
                    } catch (RemoteException ex) {
                        Log.e(PhoneWindow.TAG, "Couldn't start watching for device rotation", ex);
                    }
                }
                this.mWindows.add(new WeakReference(phoneWindow));
            }
            return;
        }

        public void removeWindow(PhoneWindow phoneWindow) {
            synchronized (this.mWindows) {
                int i = 0;
                while (i < this.mWindows.size()) {
                    PhoneWindow win = (PhoneWindow) ((WeakReference) this.mWindows.get(i)).get();
                    if (win == null || win == phoneWindow) {
                        this.mWindows.remove(i);
                    } else {
                        i++;
                    }
                }
            }
        }

        void dispatchRotationChanged() {
            synchronized (this.mWindows) {
                int i = 0;
                while (i < this.mWindows.size()) {
                    PhoneWindow win = (PhoneWindow) ((WeakReference) this.mWindows.get(i)).get();
                    if (win != null) {
                        win.onOptionsPanelRotationChanged();
                        i++;
                    } else {
                        this.mWindows.remove(i);
                    }
                }
            }
        }
    }

    static class WindowManagerHolder {
        static final IWindowManager sWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        WindowManagerHolder() {
        }
    }

    public PhoneWindow(Context context) {
        super(context);
        this.mContextMenuCallback = new PhoneWindowMenuCallback(this);
        this.mMinWidthMajor = new TypedValue();
        this.mMinWidthMinor = new TypedValue();
        this.mForceDecorInstall = false;
        this.mContentParentExplicitlySet = false;
        this.mBackgroundResource = 0;
        this.mBackgroundFallbackResource = 0;
        this.mLoadElevation = true;
        this.mFrameResource = 0;
        this.mTextColor = 0;
        this.mStatusBarColor = 0;
        this.mNavigationBarColor = 0;
        this.mNavigationBarDividerColor = 0;
        this.mForcedStatusBarColor = false;
        this.mForcedNavigationBarColor = false;
        this.mTitle = null;
        this.mTitleColor = 0;
        this.mAlwaysReadCloseOnTouchAttr = false;
        this.mVolumeControlStreamType = Integer.MIN_VALUE;
        this.mUiOptions = 0;
        this.mInvalidatePanelMenuRunnable = new Runnable() {
            public void run() {
                for (int i = 0; i <= 13; i++) {
                    if ((PhoneWindow.this.mInvalidatePanelMenuFeatures & (1 << i)) != 0) {
                        PhoneWindow.this.doInvalidatePanelMenu(i);
                    }
                }
                PhoneWindow.this.mInvalidatePanelMenuPosted = false;
                PhoneWindow.this.mInvalidatePanelMenuFeatures = 0;
            }
        };
        this.mEnterTransition = null;
        this.mReturnTransition = USE_DEFAULT_TRANSITION;
        this.mExitTransition = null;
        this.mReenterTransition = USE_DEFAULT_TRANSITION;
        this.mSharedElementEnterTransition = null;
        this.mSharedElementReturnTransition = USE_DEFAULT_TRANSITION;
        this.mSharedElementExitTransition = null;
        this.mSharedElementReenterTransition = USE_DEFAULT_TRANSITION;
        this.mBackgroundFadeDurationMillis = -1;
        this.mTheme = -1;
        this.mDecorCaptionShade = 0;
        this.mUseDecorContext = false;
        this.mForceWindowDrawsNavBarBackground = false;
        this.mActivityName = "";
        this.mDrawsNavBarBackground = true;
        this.mHasAddFullScreenView = false;
        this.mHasPaletteColor = false;
        this.mNeedPaletteColor = false;
        this.mNavColorIsAppCustom = false;
        this.mAdapationStatusBarColor = 0;
        this.mRunningCts = false;
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    public PhoneWindow(Context context, Window preservedWindow, ActivityConfigCallback activityConfigCallback) {
        boolean z;
        this(context);
        this.mUseDecorContext = true;
        if (preservedWindow != null) {
            this.mDecor = (DecorView) preservedWindow.getDecorView();
            this.mElevation = preservedWindow.getElevation();
            this.mLoadElevation = false;
            this.mForceDecorInstall = true;
            getAttributes().token = preservedWindow.getAttributes().token;
        }
        if (Global.getInt(context.getContentResolver(), Global.DEVELOPMENT_FORCE_RESIZABLE_ACTIVITIES, 0) != 0) {
            z = true;
        } else {
            z = context.getPackageManager().hasSystemFeature("android.software.picture_in_picture");
        }
        this.mSupportsPictureInPicture = z;
        this.mActivityConfigCallback = activityConfigCallback;
        this.mRunningCts = context.getPackageManager().isClosedSuperFirewall();
    }

    public final void setContainer(Window container) {
        super.setContainer(container);
    }

    public void setWindowManager(WindowManager wm, IBinder appToken, String appName, boolean hardwareAccelerated) {
        super.setWindowManager(wm, appToken, appName, hardwareAccelerated);
        this.mActivityName = getContext().getClass().getName();
        this.mAdapationStatusBarColor = getStatusBarColorFromAdaptation();
        this.mNeedPaletteColor = needPaletteColorForNavBar();
    }

    public boolean requestFeature(int featureId) {
        if (this.mContentParentExplicitlySet) {
            throw new AndroidRuntimeException("requestFeature() must be called before adding content");
        }
        int features = getFeatures();
        int newFeatures = features | (1 << featureId);
        if ((newFeatures & 128) != 0 && (newFeatures & -13506) != 0) {
            throw new AndroidRuntimeException("You cannot combine custom titles with other title features");
        } else if ((features & 2) != 0 && featureId == 8) {
            return false;
        } else {
            if ((features & 256) != 0 && featureId == 1) {
                removeFeature(8);
            }
            if ((features & 256) != 0 && featureId == 11) {
                throw new AndroidRuntimeException("You cannot combine swipe dismissal and the action bar.");
            } else if ((features & 2048) != 0 && featureId == 8) {
                throw new AndroidRuntimeException("You cannot combine swipe dismissal and the action bar.");
            } else if (featureId != 5 || !getContext().getPackageManager().hasSystemFeature("android.hardware.type.watch")) {
                return super.requestFeature(featureId);
            } else {
                throw new AndroidRuntimeException("You cannot use indeterminate progress on a watch.");
            }
        }
    }

    public void setUiOptions(int uiOptions) {
        this.mUiOptions = uiOptions;
    }

    public void setUiOptions(int uiOptions, int mask) {
        this.mUiOptions = (this.mUiOptions & (~mask)) | (uiOptions & mask);
    }

    public TransitionManager getTransitionManager() {
        return this.mTransitionManager;
    }

    public void setTransitionManager(TransitionManager tm) {
        this.mTransitionManager = tm;
    }

    public Scene getContentScene() {
        return this.mContentScene;
    }

    public void setContentView(int layoutResID) {
        if (this.mContentParent == null) {
            installDecor();
        } else if (!hasFeature(12)) {
            this.mContentParent.removeAllViews();
        }
        if (hasFeature(12)) {
            transitionTo(Scene.getSceneForLayout(this.mContentParent, layoutResID, getContext()));
        } else {
            this.mLayoutInflater.inflate(layoutResID, this.mContentParent);
        }
        this.mContentParent.requestApplyInsets();
        Window.Callback cb = getCallback();
        if (!(cb == null || (isDestroyed() ^ 1) == 0)) {
            cb.onContentChanged();
        }
        this.mContentParentExplicitlySet = true;
    }

    public void setContentView(View view) {
        setContentView(view, new LayoutParams(-1, -1));
    }

    public void setContentView(View view, LayoutParams params) {
        if (this.mContentParent == null) {
            installDecor();
        } else if (!hasFeature(12)) {
            this.mContentParent.removeAllViews();
        }
        if (hasFeature(12)) {
            view.setLayoutParams(params);
            transitionTo(new Scene(this.mContentParent, view));
        } else {
            this.mContentParent.addView(view, params);
        }
        this.mContentParent.requestApplyInsets();
        Window.Callback cb = getCallback();
        if (!(cb == null || (isDestroyed() ^ 1) == 0)) {
            cb.onContentChanged();
        }
        this.mContentParentExplicitlySet = true;
    }

    public void addContentView(View view, LayoutParams params) {
        if (this.mContentParent == null) {
            installDecor();
        }
        if (hasFeature(12)) {
            Log.v(TAG, "addContentView does not support content transitions");
        }
        this.mContentParent.addView(view, params);
        this.mContentParent.requestApplyInsets();
        Window.Callback cb = getCallback();
        if (cb != null && (isDestroyed() ^ 1) != 0) {
            cb.onContentChanged();
        }
    }

    public void clearContentView() {
        if (this.mDecor != null) {
            this.mDecor.clearContentView();
        }
    }

    private void transitionTo(Scene scene) {
        if (this.mContentScene == null) {
            scene.enter();
        } else {
            this.mTransitionManager.transitionTo(scene);
        }
        this.mContentScene = scene;
    }

    public View getCurrentFocus() {
        return this.mDecor != null ? this.mDecor.findFocus() : null;
    }

    public void takeSurface(Callback2 callback) {
        this.mTakeSurfaceCallback = callback;
    }

    public void takeInputQueue(InputQueue.Callback callback) {
        this.mTakeInputQueueCallback = callback;
    }

    public boolean isFloating() {
        return this.mIsFloating;
    }

    public boolean isTranslucent() {
        return this.mIsTranslucent;
    }

    boolean isShowingWallpaper() {
        return (getAttributes().flags & 1048576) != 0;
    }

    public LayoutInflater getLayoutInflater() {
        return this.mLayoutInflater;
    }

    public void setTitle(CharSequence title) {
        setTitle(title, true);
    }

    public void setTitle(CharSequence title, boolean updateAccessibilityTitle) {
        if (this.mTitleView != null) {
            this.mTitleView.setText(title);
        } else if (this.mDecorContentParent != null) {
            this.mDecorContentParent.setWindowTitle(title);
        }
        this.mTitle = title;
        if (updateAccessibilityTitle) {
            WindowManager.LayoutParams params = getAttributes();
            if (!TextUtils.equals(title, params.accessibilityTitle)) {
                params.accessibilityTitle = TextUtils.stringOrSpannedString(title);
                if (this.mDecor != null) {
                    ViewRootImpl vr = this.mDecor.getViewRootImpl();
                    if (vr != null) {
                        vr.onWindowTitleChanged();
                    }
                }
                dispatchWindowAttributesChanged(getAttributes());
            }
        }
    }

    @Deprecated
    public void setTitleColor(int textColor) {
        if (this.mTitleView != null) {
            this.mTitleView.setTextColor(textColor);
        }
        this.mTitleColor = textColor;
    }

    public final boolean preparePanel(PanelFeatureState st, KeyEvent event) {
        if (isDestroyed()) {
            return false;
        }
        if (st.isPrepared) {
            return true;
        }
        if (!(this.mPreparedPanel == null || this.mPreparedPanel == st)) {
            closePanel(this.mPreparedPanel, false);
        }
        Window.Callback cb = getCallback();
        if (cb != null) {
            st.createdPanelView = cb.onCreatePanelView(st.featureId);
        }
        boolean isActionBarMenu = st.featureId == 0 || st.featureId == 8;
        if (isActionBarMenu && this.mDecorContentParent != null) {
            this.mDecorContentParent.setMenuPrepared();
        }
        if (st.createdPanelView == null) {
            if (st.menu == null || st.refreshMenuContent) {
                if (st.menu == null && (!initializePanelMenu(st) || st.menu == null)) {
                    return false;
                }
                if (isActionBarMenu && this.mDecorContentParent != null) {
                    if (this.mActionMenuPresenterCallback == null) {
                        this.mActionMenuPresenterCallback = new ActionMenuPresenterCallback();
                    }
                    this.mDecorContentParent.setMenu(st.menu, this.mActionMenuPresenterCallback);
                }
                st.menu.stopDispatchingItemsChanged();
                if (cb == null || (cb.onCreatePanelMenu(st.featureId, st.menu) ^ 1) != 0) {
                    st.setMenu(null);
                    if (isActionBarMenu && this.mDecorContentParent != null) {
                        this.mDecorContentParent.setMenu(null, this.mActionMenuPresenterCallback);
                    }
                    return false;
                }
                st.refreshMenuContent = false;
            }
            st.menu.stopDispatchingItemsChanged();
            if (st.frozenActionViewState != null) {
                st.menu.restoreActionViewStates(st.frozenActionViewState);
                st.frozenActionViewState = null;
            }
            if (cb.onPreparePanel(st.featureId, st.createdPanelView, st.menu)) {
                boolean z;
                if (KeyCharacterMap.load(event != null ? event.getDeviceId() : -1).getKeyboardType() != 1) {
                    z = true;
                } else {
                    z = false;
                }
                st.qwertyMode = z;
                st.menu.setQwertyMode(st.qwertyMode);
                st.menu.startDispatchingItemsChanged();
            } else {
                if (isActionBarMenu && this.mDecorContentParent != null) {
                    this.mDecorContentParent.setMenu(null, this.mActionMenuPresenterCallback);
                }
                st.menu.startDispatchingItemsChanged();
                return false;
            }
        }
        st.isPrepared = true;
        st.isHandled = false;
        this.mPreparedPanel = st;
        return true;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mDecorContentParent == null) {
            PanelFeatureState st = getPanelState(0, false);
            if (st != null && st.menu != null) {
                if (st.isOpen) {
                    Bundle state = new Bundle();
                    if (st.iconMenuPresenter != null) {
                        st.iconMenuPresenter.saveHierarchyState(state);
                    }
                    if (st.listMenuPresenter != null) {
                        st.listMenuPresenter.saveHierarchyState(state);
                    }
                    clearMenuViews(st);
                    reopenMenu(false);
                    if (st.iconMenuPresenter != null) {
                        st.iconMenuPresenter.restoreHierarchyState(state);
                    }
                    if (st.listMenuPresenter != null) {
                        st.listMenuPresenter.restoreHierarchyState(state);
                        return;
                    }
                    return;
                }
                clearMenuViews(st);
            }
        }
    }

    public void onMultiWindowModeChanged() {
        if (this.mDecor != null) {
            this.mDecor.onConfigurationChanged(getContext().getResources().getConfiguration());
        }
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (this.mDecor != null) {
            this.mDecor.updatePictureInPictureOutlineProvider(isInPictureInPictureMode);
        }
    }

    public void reportActivityRelaunched() {
        if (this.mDecor != null && this.mDecor.getViewRootImpl() != null) {
            this.mDecor.getViewRootImpl().reportActivityRelaunched();
        }
    }

    private static void clearMenuViews(PanelFeatureState st) {
        st.createdPanelView = null;
        st.refreshDecorView = true;
        st.clearMenuPresenters();
    }

    public final void openPanel(int featureId, KeyEvent event) {
        if (featureId != 0 || this.mDecorContentParent == null || !this.mDecorContentParent.canShowOverflowMenu() || (ViewConfiguration.get(getContext()).hasPermanentMenuKey() ^ 1) == 0) {
            openPanel(getPanelState(featureId, true), event);
        } else {
            this.mDecorContentParent.showOverflowMenu();
        }
    }

    private void openPanel(com.android.internal.policy.PhoneWindow.PanelFeatureState r20, android.view.KeyEvent r21) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r16_2 'lp' android.view.ViewGroup$LayoutParams) in PHI: PHI: (r16_3 'lp' android.view.ViewGroup$LayoutParams) = (r16_1 'lp' android.view.ViewGroup$LayoutParams), (r16_2 'lp' android.view.ViewGroup$LayoutParams) binds: {(r16_1 'lp' android.view.ViewGroup$LayoutParams)=B:52:0x00b6, (r16_2 'lp' android.view.ViewGroup$LayoutParams)=B:53:0x00b8}
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
        r19 = this;
        r0 = r20;
        r4 = r0.isOpen;
        if (r4 != 0) goto L_0x000c;
    L_0x0006:
        r4 = r19.isDestroyed();
        if (r4 == 0) goto L_0x000d;
    L_0x000c:
        return;
    L_0x000d:
        r0 = r20;
        r4 = r0.featureId;
        if (r4 != 0) goto L_0x003b;
    L_0x0013:
        r13 = r19.getContext();
        r4 = r13.getResources();
        r12 = r4.getConfiguration();
        r4 = r12.screenLayout;
        r4 = r4 & 15;
        r5 = 4;
        if (r4 != r5) goto L_0x0037;
    L_0x0026:
        r15 = 1;
    L_0x0027:
        r4 = r13.getApplicationInfo();
        r4 = r4.targetSdkVersion;
        r5 = 11;
        if (r4 < r5) goto L_0x0039;
    L_0x0031:
        r14 = 1;
    L_0x0032:
        if (r15 == 0) goto L_0x003b;
    L_0x0034:
        if (r14 == 0) goto L_0x003b;
    L_0x0036:
        return;
    L_0x0037:
        r15 = 0;
        goto L_0x0027;
    L_0x0039:
        r14 = 0;
        goto L_0x0032;
    L_0x003b:
        r11 = r19.getCallback();
        if (r11 == 0) goto L_0x005a;
    L_0x0041:
        r0 = r20;
        r4 = r0.featureId;
        r0 = r20;
        r5 = r0.menu;
        r4 = r11.onMenuOpened(r4, r5);
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x005a;
    L_0x0051:
        r4 = 1;
        r0 = r19;
        r1 = r20;
        r0.closePanel(r1, r4);
        return;
    L_0x005a:
        r18 = r19.getWindowManager();
        if (r18 != 0) goto L_0x0061;
    L_0x0060:
        return;
    L_0x0061:
        r4 = r19.preparePanel(r20, r21);
        if (r4 != 0) goto L_0x0068;
    L_0x0067:
        return;
    L_0x0068:
        r3 = -2;
        r0 = r20;
        r4 = r0.decorView;
        if (r4 == 0) goto L_0x0075;
    L_0x006f:
        r0 = r20;
        r4 = r0.refreshDecorView;
        if (r4 == 0) goto L_0x0160;
    L_0x0075:
        r0 = r20;
        r4 = r0.decorView;
        if (r4 != 0) goto L_0x0088;
    L_0x007b:
        r4 = r19.initializePanelDecor(r20);
        if (r4 == 0) goto L_0x0087;
    L_0x0081:
        r0 = r20;
        r4 = r0.decorView;
        if (r4 != 0) goto L_0x009f;
    L_0x0087:
        return;
    L_0x0088:
        r0 = r20;
        r4 = r0.refreshDecorView;
        if (r4 == 0) goto L_0x009f;
    L_0x008e:
        r0 = r20;
        r4 = r0.decorView;
        r4 = r4.getChildCount();
        if (r4 <= 0) goto L_0x009f;
    L_0x0098:
        r0 = r20;
        r4 = r0.decorView;
        r4.removeAllViews();
    L_0x009f:
        r4 = r19.initializePanelContent(r20);
        if (r4 == 0) goto L_0x00ad;
    L_0x00a5:
        r4 = r20.hasPanelItems();
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x00ae;
    L_0x00ad:
        return;
    L_0x00ae:
        r0 = r20;
        r4 = r0.shownPanelView;
        r16 = r4.getLayoutParams();
        if (r16 != 0) goto L_0x00c1;
    L_0x00b8:
        r16 = new android.view.ViewGroup$LayoutParams;
        r4 = -2;
        r5 = -2;
        r0 = r16;
        r0.<init>(r4, r5);
    L_0x00c1:
        r0 = r16;
        r4 = r0.width;
        r5 = -1;
        if (r4 != r5) goto L_0x015a;
    L_0x00c8:
        r0 = r20;
        r10 = r0.fullBackground;
        r3 = -1;
    L_0x00cd:
        r0 = r20;
        r4 = r0.decorView;
        r5 = r19.getContext();
        r5 = r5.getDrawable(r10);
        r4.setWindowBackground(r5);
        r0 = r20;
        r4 = r0.shownPanelView;
        r17 = r4.getParent();
        if (r17 == 0) goto L_0x00f7;
    L_0x00e6:
        r0 = r17;
        r4 = r0 instanceof android.view.ViewGroup;
        if (r4 == 0) goto L_0x00f7;
    L_0x00ec:
        r17 = (android.view.ViewGroup) r17;
        r0 = r20;
        r4 = r0.shownPanelView;
        r0 = r17;
        r0.removeView(r4);
    L_0x00f7:
        r0 = r20;
        r4 = r0.decorView;
        r0 = r20;
        r5 = r0.shownPanelView;
        r0 = r16;
        r4.addView(r5, r0);
        r0 = r20;
        r4 = r0.shownPanelView;
        r4 = r4.hasFocus();
        if (r4 != 0) goto L_0x0115;
    L_0x010e:
        r0 = r20;
        r4 = r0.shownPanelView;
        r4.requestFocus();
    L_0x0115:
        r4 = 0;
        r0 = r20;
        r0.isHandled = r4;
        r2 = new android.view.WindowManager$LayoutParams;
        r0 = r20;
        r5 = r0.x;
        r0 = r20;
        r6 = r0.y;
        r0 = r20;
        r4 = r0.decorView;
        r9 = r4.mDefaultOpacity;
        r4 = -2;
        r7 = 1003; // 0x3eb float:1.406E-42 double:4.955E-321;
        r8 = 8519680; // 0x820000 float:1.1938615E-38 double:4.209281E-317;
        r2.<init>(r3, r4, r5, r6, r7, r8, r9);
        r0 = r20;
        r4 = r0.isCompact;
        if (r4 == 0) goto L_0x0181;
    L_0x0138:
        r4 = r19.getOptionsPanelGravity();
        r2.gravity = r4;
        r4 = sRotationWatcher;
        r0 = r19;
        r4.addWindow(r0);
    L_0x0145:
        r0 = r20;
        r4 = r0.windowAnimations;
        r2.windowAnimations = r4;
        r0 = r20;
        r4 = r0.decorView;
        r0 = r18;
        r0.addView(r4, r2);
        r4 = 1;
        r0 = r20;
        r0.isOpen = r4;
        return;
    L_0x015a:
        r0 = r20;
        r10 = r0.background;
        goto L_0x00cd;
    L_0x0160:
        r4 = r20.isInListMode();
        if (r4 != 0) goto L_0x0168;
    L_0x0166:
        r3 = -1;
        goto L_0x0115;
    L_0x0168:
        r0 = r20;
        r4 = r0.createdPanelView;
        if (r4 == 0) goto L_0x0115;
    L_0x016e:
        r0 = r20;
        r4 = r0.createdPanelView;
        r16 = r4.getLayoutParams();
        if (r16 == 0) goto L_0x0115;
    L_0x0178:
        r0 = r16;
        r4 = r0.width;
        r5 = -1;
        if (r4 != r5) goto L_0x0115;
    L_0x017f:
        r3 = -1;
        goto L_0x0115;
    L_0x0181:
        r0 = r20;
        r4 = r0.gravity;
        r2.gravity = r4;
        goto L_0x0145;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.policy.PhoneWindow.openPanel(com.android.internal.policy.PhoneWindow$PanelFeatureState, android.view.KeyEvent):void");
    }

    public final void closePanel(int featureId) {
        if (featureId == 0 && this.mDecorContentParent != null && this.mDecorContentParent.canShowOverflowMenu() && (ViewConfiguration.get(getContext()).hasPermanentMenuKey() ^ 1) != 0) {
            this.mDecorContentParent.hideOverflowMenu();
        } else if (featureId == 6) {
            closeContextMenu();
        } else {
            closePanel(getPanelState(featureId, true), true);
        }
    }

    public final void closePanel(PanelFeatureState st, boolean doCallback) {
        if (doCallback && st.featureId == 0 && this.mDecorContentParent != null && this.mDecorContentParent.isOverflowMenuShowing()) {
            checkCloseActionMenu(st.menu);
            return;
        }
        ViewManager wm = getWindowManager();
        if (wm != null && st.isOpen) {
            if (st.decorView != null) {
                wm.removeView(st.decorView);
                if (st.isCompact) {
                    sRotationWatcher.removeWindow(this);
                }
            }
            if (doCallback) {
                callOnPanelClosed(st.featureId, st, null);
            }
        }
        st.isPrepared = false;
        st.isHandled = false;
        st.isOpen = false;
        st.shownPanelView = null;
        if (st.isInExpandedMode) {
            st.refreshDecorView = true;
            st.isInExpandedMode = false;
        }
        if (this.mPreparedPanel == st) {
            this.mPreparedPanel = null;
            this.mPanelChordingKey = 0;
        }
    }

    void checkCloseActionMenu(Menu menu) {
        if (!this.mClosingActionMenu) {
            this.mClosingActionMenu = true;
            this.mDecorContentParent.dismissPopups();
            Window.Callback cb = getCallback();
            if (!(cb == null || (isDestroyed() ^ 1) == 0)) {
                cb.onPanelClosed(8, menu);
            }
            this.mClosingActionMenu = false;
        }
    }

    public final void togglePanel(int featureId, KeyEvent event) {
        PanelFeatureState st = getPanelState(featureId, true);
        if (st.isOpen) {
            closePanel(st, true);
        } else {
            openPanel(st, event);
        }
    }

    public void invalidatePanelMenu(int featureId) {
        this.mInvalidatePanelMenuFeatures |= 1 << featureId;
        if (!this.mInvalidatePanelMenuPosted && this.mDecor != null) {
            this.mDecor.postOnAnimation(this.mInvalidatePanelMenuRunnable);
            this.mInvalidatePanelMenuPosted = true;
        }
    }

    void doPendingInvalidatePanelMenu() {
        if (this.mInvalidatePanelMenuPosted) {
            this.mDecor.removeCallbacks(this.mInvalidatePanelMenuRunnable);
            this.mInvalidatePanelMenuRunnable.run();
        }
    }

    void doInvalidatePanelMenu(int featureId) {
        PanelFeatureState st = getPanelState(featureId, false);
        if (st != null) {
            if (st.menu != null) {
                Bundle savedActionViewStates = new Bundle();
                st.menu.saveActionViewStates(savedActionViewStates);
                if (savedActionViewStates.size() > 0) {
                    st.frozenActionViewState = savedActionViewStates;
                }
                st.menu.stopDispatchingItemsChanged();
                st.menu.clear();
            }
            st.refreshMenuContent = true;
            st.refreshDecorView = true;
            if ((featureId == 8 || featureId == 0) && this.mDecorContentParent != null) {
                st = getPanelState(0, false);
                if (st != null) {
                    st.isPrepared = false;
                    preparePanel(st, null);
                }
            }
        }
    }

    public final boolean onKeyDownPanel(int featureId, KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getRepeatCount() == 0) {
            this.mPanelChordingKey = keyCode;
            PanelFeatureState st = getPanelState(featureId, false);
            if (!(st == null || (st.isOpen ^ 1) == 0)) {
                return preparePanel(st, event);
            }
        }
        return false;
    }

    public final void onKeyUpPanel(int featureId, KeyEvent event) {
        if (this.mPanelChordingKey != 0) {
            this.mPanelChordingKey = 0;
            PanelFeatureState st = getPanelState(featureId, false);
            if (!event.isCanceled() && ((this.mDecor == null || this.mDecor.mPrimaryActionMode == null) && st != null)) {
                boolean playSoundEffect = false;
                if (featureId != 0 || this.mDecorContentParent == null || !this.mDecorContentParent.canShowOverflowMenu() || (ViewConfiguration.get(getContext()).hasPermanentMenuKey() ^ 1) == 0) {
                    if (st.isOpen || st.isHandled) {
                        playSoundEffect = st.isOpen;
                        closePanel(st, true);
                    } else if (st.isPrepared) {
                        boolean show = true;
                        if (st.refreshMenuContent) {
                            st.isPrepared = false;
                            show = preparePanel(st, event);
                        }
                        if (show) {
                            EventLog.writeEvent(50001, 0);
                            openPanel(st, event);
                            playSoundEffect = true;
                        }
                    }
                } else if (this.mDecorContentParent.isOverflowMenuShowing()) {
                    playSoundEffect = this.mDecorContentParent.hideOverflowMenu();
                } else if (!isDestroyed() && preparePanel(st, event)) {
                    playSoundEffect = this.mDecorContentParent.showOverflowMenu();
                }
                if (playSoundEffect) {
                    AudioManager audioManager = (AudioManager) getContext().getSystemService("audio");
                    if (audioManager != null) {
                        audioManager.playSoundEffect(0);
                    } else {
                        Log.w(TAG, "Couldn't get audio manager");
                    }
                }
            }
        }
    }

    public final void closeAllPanels() {
        if (getWindowManager() != null) {
            PanelFeatureState[] panels = this.mPanels;
            int N = panels != null ? panels.length : 0;
            for (int i = 0; i < N; i++) {
                PanelFeatureState panel = panels[i];
                if (panel != null) {
                    closePanel(panel, true);
                }
            }
            closeContextMenu();
        }
    }

    private synchronized void closeContextMenu() {
        if (this.mContextMenu != null) {
            this.mContextMenu.close();
            dismissContextMenu();
        }
    }

    private synchronized void dismissContextMenu() {
        this.mContextMenu = null;
        if (this.mContextMenuHelper != null) {
            this.mContextMenuHelper.dismiss();
            this.mContextMenuHelper = null;
        }
    }

    public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) {
        return performPanelShortcut(getPanelState(featureId, false), keyCode, event, flags);
    }

    boolean performPanelShortcut(PanelFeatureState st, int keyCode, KeyEvent event, int flags) {
        if (event.isSystem() || st == null) {
            return false;
        }
        boolean handled = false;
        if ((st.isPrepared || preparePanel(st, event)) && st.menu != null) {
            handled = st.menu.performShortcut(keyCode, event, flags);
        }
        if (handled) {
            st.isHandled = true;
            if ((flags & 1) == 0 && this.mDecorContentParent == null) {
                closePanel(st, true);
            }
        }
        return handled;
    }

    public boolean performPanelIdentifierAction(int featureId, int id, int flags) {
        PanelFeatureState st = getPanelState(featureId, true);
        if (!preparePanel(st, new KeyEvent(0, 82)) || st.menu == null) {
            return false;
        }
        boolean res = st.menu.performIdentifierAction(id, flags);
        if (this.mDecorContentParent == null) {
            closePanel(st, true);
        }
        return res;
    }

    public PanelFeatureState findMenuPanel(Menu menu) {
        PanelFeatureState[] panels = this.mPanels;
        int N = panels != null ? panels.length : 0;
        for (int i = 0; i < N; i++) {
            PanelFeatureState panel = panels[i];
            if (panel != null && panel.menu == menu) {
                return panel;
            }
        }
        return null;
    }

    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        Window.Callback cb = getCallback();
        if (!(cb == null || (isDestroyed() ^ 1) == 0)) {
            PanelFeatureState panel = findMenuPanel(menu.getRootMenu());
            if (panel != null) {
                return cb.onMenuItemSelected(panel.featureId, item);
            }
        }
        return false;
    }

    public void onMenuModeChange(MenuBuilder menu) {
        reopenMenu(true);
    }

    private void reopenMenu(boolean toggleMenuMode) {
        PanelFeatureState st;
        if (this.mDecorContentParent == null || !this.mDecorContentParent.canShowOverflowMenu() || (ViewConfiguration.get(getContext()).hasPermanentMenuKey() && !this.mDecorContentParent.isOverflowMenuShowPending())) {
            st = getPanelState(0, false);
            if (st != null) {
                boolean newExpandedMode = toggleMenuMode ? st.isInExpandedMode ^ 1 : st.isInExpandedMode;
                st.refreshDecorView = true;
                closePanel(st, false);
                st.isInExpandedMode = newExpandedMode;
                openPanel(st, null);
                return;
            }
            return;
        }
        Window.Callback cb = getCallback();
        if (this.mDecorContentParent.isOverflowMenuShowing() && (toggleMenuMode ^ 1) == 0) {
            this.mDecorContentParent.hideOverflowMenu();
            st = getPanelState(0, false);
            if (!(st == null || cb == null || (isDestroyed() ^ 1) == 0)) {
                cb.onPanelClosed(8, st.menu);
            }
        } else if (!(cb == null || (isDestroyed() ^ 1) == 0)) {
            if (this.mInvalidatePanelMenuPosted && (this.mInvalidatePanelMenuFeatures & 1) != 0) {
                this.mDecor.removeCallbacks(this.mInvalidatePanelMenuRunnable);
                this.mInvalidatePanelMenuRunnable.run();
            }
            st = getPanelState(0, false);
            if (!(st == null || st.menu == null || (st.refreshMenuContent ^ 1) == 0 || !cb.onPreparePanel(0, st.createdPanelView, st.menu))) {
                cb.onMenuOpened(8, st.menu);
                this.mDecorContentParent.showOverflowMenu();
            }
        }
    }

    protected boolean initializePanelMenu(PanelFeatureState st) {
        Context context = getContext();
        if ((st.featureId == 0 || st.featureId == 8) && this.mDecorContentParent != null) {
            TypedValue outValue = new TypedValue();
            Theme baseTheme = context.getTheme();
            baseTheme.resolveAttribute(R.attr.actionBarTheme, outValue, true);
            Theme widgetTheme = null;
            if (outValue.resourceId != 0) {
                widgetTheme = context.getResources().newTheme();
                widgetTheme.setTo(baseTheme);
                widgetTheme.applyStyle(outValue.resourceId, true);
                widgetTheme.resolveAttribute(R.attr.actionBarWidgetTheme, outValue, true);
            } else {
                baseTheme.resolveAttribute(R.attr.actionBarWidgetTheme, outValue, true);
            }
            if (outValue.resourceId != 0) {
                if (widgetTheme == null) {
                    widgetTheme = context.getResources().newTheme();
                    widgetTheme.setTo(baseTheme);
                }
                widgetTheme.applyStyle(outValue.resourceId, true);
            }
            if (widgetTheme != null) {
                Context context2 = new ContextThemeWrapper(context, 0);
                context2.getTheme().setTo(widgetTheme);
                context = context2;
            }
        }
        MenuBuilder menu = new MenuBuilder(context);
        menu.setCallback(this);
        st.setMenu(menu);
        return true;
    }

    protected boolean initializePanelDecor(PanelFeatureState st) {
        st.decorView = generateDecor(st.featureId);
        st.gravity = 81;
        st.setStyle(getContext());
        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.Window, 0, st.listPresenterTheme);
        float elevation = a.getDimension(38, 0.0f);
        if (elevation != 0.0f) {
            st.decorView.setElevation(elevation);
        }
        a.recycle();
        return true;
    }

    private int getOptionsPanelGravity() {
        try {
            return WindowManagerHolder.sWindowManager.getPreferredOptionsPanelGravity();
        } catch (RemoteException ex) {
            Log.e(TAG, "Couldn't getOptionsPanelGravity; using default", ex);
            return 81;
        }
    }

    void onOptionsPanelRotationChanged() {
        PanelFeatureState st = getPanelState(0, false);
        if (st != null) {
            LayoutParams lp = st.decorView != null ? (WindowManager.LayoutParams) st.decorView.getLayoutParams() : null;
            if (lp != null) {
                lp.gravity = getOptionsPanelGravity();
                ViewManager wm = getWindowManager();
                if (wm != null) {
                    wm.updateViewLayout(st.decorView, lp);
                }
            }
        }
    }

    protected boolean initializePanelContent(PanelFeatureState st) {
        if (st.createdPanelView != null) {
            st.shownPanelView = st.createdPanelView;
            return true;
        } else if (st.menu == null) {
            return false;
        } else {
            MenuView menuView;
            if (this.mPanelMenuPresenterCallback == null) {
                this.mPanelMenuPresenterCallback = new PanelMenuPresenterCallback();
            }
            if (st.isInListMode()) {
                menuView = st.getListMenuView(getContext(), this.mPanelMenuPresenterCallback);
            } else {
                menuView = st.getIconMenuView(getContext(), this.mPanelMenuPresenterCallback);
            }
            st.shownPanelView = (View) menuView;
            if (st.shownPanelView == null) {
                return false;
            }
            int defaultAnimations = menuView.getWindowAnimations();
            if (defaultAnimations != 0) {
                st.windowAnimations = defaultAnimations;
            }
            return true;
        }
    }

    public boolean performContextMenuIdentifierAction(int id, int flags) {
        return this.mContextMenu != null ? this.mContextMenu.performIdentifierAction(id, flags) : false;
    }

    public final void setElevation(float elevation) {
        this.mElevation = elevation;
        WindowManager.LayoutParams attrs = getAttributes();
        if (this.mDecor != null) {
            this.mDecor.setElevation(elevation);
            attrs.setSurfaceInsets(this.mDecor, true, false);
        }
        dispatchWindowAttributesChanged(attrs);
    }

    public float getElevation() {
        return this.mElevation;
    }

    public final void setClipToOutline(boolean clipToOutline) {
        this.mClipToOutline = clipToOutline;
        if (this.mDecor != null) {
            this.mDecor.setClipToOutline(clipToOutline);
        }
    }

    public final void setBackgroundDrawable(Drawable drawable) {
        int i = 0;
        if (drawable != this.mBackgroundDrawable || this.mBackgroundResource != 0) {
            this.mBackgroundResource = 0;
            this.mBackgroundDrawable = drawable;
            if (this.mDecor != null) {
                this.mDecor.setWindowBackground(drawable);
            }
            if (this.mBackgroundFallbackResource != 0) {
                DecorView decorView = this.mDecor;
                if (drawable == null) {
                    i = this.mBackgroundFallbackResource;
                }
                decorView.setBackgroundFallback(i);
            }
        }
    }

    public final void setFeatureDrawableResource(int featureId, int resId) {
        if (resId != 0) {
            DrawableFeatureState st = getDrawableState(featureId, true);
            if (st.resid != resId) {
                st.resid = resId;
                st.uri = null;
                st.local = getContext().getDrawable(resId);
                updateDrawable(featureId, st, false);
                return;
            }
            return;
        }
        setFeatureDrawable(featureId, null);
    }

    public final void setFeatureDrawableUri(int featureId, Uri uri) {
        if (uri != null) {
            DrawableFeatureState st = getDrawableState(featureId, true);
            if (st.uri == null || (st.uri.equals(uri) ^ 1) != 0) {
                st.resid = 0;
                st.uri = uri;
                st.local = loadImageURI(uri);
                updateDrawable(featureId, st, false);
                return;
            }
            return;
        }
        setFeatureDrawable(featureId, null);
    }

    public final void setFeatureDrawable(int featureId, Drawable drawable) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        st.resid = 0;
        st.uri = null;
        if (st.local != drawable) {
            st.local = drawable;
            updateDrawable(featureId, st, false);
        }
    }

    public void setFeatureDrawableAlpha(int featureId, int alpha) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        if (st.alpha != alpha) {
            st.alpha = alpha;
            updateDrawable(featureId, st, false);
        }
    }

    protected final void setFeatureDefaultDrawable(int featureId, Drawable drawable) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        if (st.def != drawable) {
            st.def = drawable;
            updateDrawable(featureId, st, false);
        }
    }

    public final void setFeatureInt(int featureId, int value) {
        updateInt(featureId, value, false);
    }

    protected final void updateDrawable(int featureId, boolean fromActive) {
        DrawableFeatureState st = getDrawableState(featureId, false);
        if (st != null) {
            updateDrawable(featureId, st, fromActive);
        }
    }

    protected void onDrawableChanged(int featureId, Drawable drawable, int alpha) {
        ImageView view;
        if (featureId == 3) {
            view = getLeftIconView();
        } else if (featureId == 4) {
            view = getRightIconView();
        } else {
            return;
        }
        if (drawable != null) {
            drawable.setAlpha(alpha);
            view.setImageDrawable(drawable);
            view.setVisibility(0);
        } else {
            view.setVisibility(8);
        }
    }

    protected void onIntChanged(int featureId, int value) {
        if (featureId == 2 || featureId == 5) {
            updateProgressBars(value);
        } else if (featureId == 7) {
            ViewGroup titleContainer = (FrameLayout) findViewById(R.id.title_container);
            if (titleContainer != null) {
                this.mLayoutInflater.inflate(value, titleContainer);
            }
        }
    }

    private void updateProgressBars(int value) {
        ProgressBar circularProgressBar = getCircularProgressBar(true);
        ProgressBar horizontalProgressBar = getHorizontalProgressBar(true);
        int features = getLocalFeatures();
        if (value == -1) {
            if ((features & 4) != 0) {
                if (horizontalProgressBar != null) {
                    int visibility = (horizontalProgressBar.isIndeterminate() || horizontalProgressBar.getProgress() < 10000) ? 0 : 4;
                    horizontalProgressBar.setVisibility(visibility);
                } else {
                    Log.e(TAG, "Horizontal progress bar not located in current window decor");
                }
            }
            if ((features & 32) == 0) {
                return;
            }
            if (circularProgressBar != null) {
                circularProgressBar.setVisibility(0);
            } else {
                Log.e(TAG, "Circular progress bar not located in current window decor");
            }
        } else if (value == -2) {
            if ((features & 4) != 0) {
                if (horizontalProgressBar != null) {
                    horizontalProgressBar.setVisibility(8);
                } else {
                    Log.e(TAG, "Horizontal progress bar not located in current window decor");
                }
            }
            if ((features & 32) == 0) {
                return;
            }
            if (circularProgressBar != null) {
                circularProgressBar.setVisibility(8);
            } else {
                Log.e(TAG, "Circular progress bar not located in current window decor");
            }
        } else if (value == -3) {
            if (horizontalProgressBar != null) {
                horizontalProgressBar.setIndeterminate(true);
            } else {
                Log.e(TAG, "Horizontal progress bar not located in current window decor");
            }
        } else if (value == -4) {
            if (horizontalProgressBar != null) {
                horizontalProgressBar.setIndeterminate(false);
            } else {
                Log.e(TAG, "Horizontal progress bar not located in current window decor");
            }
        } else if (value >= 0 && value <= 10000) {
            if (horizontalProgressBar != null) {
                horizontalProgressBar.setProgress(value + 0);
            } else {
                Log.e(TAG, "Horizontal progress bar not located in current window decor");
            }
            if (value < 10000) {
                showProgressBars(horizontalProgressBar, circularProgressBar);
            } else {
                hideProgressBars(horizontalProgressBar, circularProgressBar);
            }
        } else if (20000 <= value && value <= 30000) {
            if (horizontalProgressBar != null) {
                horizontalProgressBar.setSecondaryProgress(value - 20000);
            } else {
                Log.e(TAG, "Horizontal progress bar not located in current window decor");
            }
            showProgressBars(horizontalProgressBar, circularProgressBar);
        }
    }

    private void showProgressBars(ProgressBar horizontalProgressBar, ProgressBar spinnyProgressBar) {
        int features = getLocalFeatures();
        if (!((features & 32) == 0 || spinnyProgressBar == null || spinnyProgressBar.getVisibility() != 4)) {
            spinnyProgressBar.setVisibility(0);
        }
        if ((features & 4) != 0 && horizontalProgressBar != null && horizontalProgressBar.getProgress() < 10000) {
            horizontalProgressBar.setVisibility(0);
        }
    }

    private void hideProgressBars(ProgressBar horizontalProgressBar, ProgressBar spinnyProgressBar) {
        int features = getLocalFeatures();
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        anim.setDuration(1000);
        if (!((features & 32) == 0 || spinnyProgressBar == null || spinnyProgressBar.getVisibility() != 0)) {
            spinnyProgressBar.startAnimation(anim);
            spinnyProgressBar.setVisibility(4);
        }
        if ((features & 4) != 0 && horizontalProgressBar != null && horizontalProgressBar.getVisibility() == 0) {
            horizontalProgressBar.startAnimation(anim);
            horizontalProgressBar.setVisibility(4);
        }
    }

    public void setIcon(int resId) {
        this.mIconRes = resId;
        this.mResourcesSetFlags |= 1;
        this.mResourcesSetFlags &= -5;
        if (this.mDecorContentParent != null) {
            this.mDecorContentParent.setIcon(resId);
        }
    }

    public void setDefaultIcon(int resId) {
        if ((this.mResourcesSetFlags & 1) == 0) {
            this.mIconRes = resId;
            if (!(this.mDecorContentParent == null || (this.mDecorContentParent.hasIcon() && (this.mResourcesSetFlags & 4) == 0))) {
                if (resId != 0) {
                    this.mDecorContentParent.setIcon(resId);
                    this.mResourcesSetFlags &= -5;
                } else {
                    this.mDecorContentParent.setIcon(getContext().getPackageManager().getDefaultActivityIcon());
                    this.mResourcesSetFlags |= 4;
                }
            }
        }
    }

    public void setLogo(int resId) {
        this.mLogoRes = resId;
        this.mResourcesSetFlags |= 2;
        if (this.mDecorContentParent != null) {
            this.mDecorContentParent.setLogo(resId);
        }
    }

    public void setDefaultLogo(int resId) {
        if ((this.mResourcesSetFlags & 2) == 0) {
            this.mLogoRes = resId;
            if (!(this.mDecorContentParent == null || (this.mDecorContentParent.hasLogo() ^ 1) == 0)) {
                this.mDecorContentParent.setLogo(resId);
            }
        }
    }

    public void setLocalFocus(boolean hasFocus, boolean inTouchMode) {
        getViewRootImpl().windowFocusChanged(hasFocus, inTouchMode);
    }

    public void injectInputEvent(InputEvent event) {
        getViewRootImpl().dispatchInputEvent(event);
    }

    private ViewRootImpl getViewRootImpl() {
        if (this.mDecor != null) {
            ViewRootImpl viewRootImpl = this.mDecor.getViewRootImpl();
            if (viewRootImpl != null) {
                return viewRootImpl;
            }
        }
        throw new IllegalStateException("view not added");
    }

    public void takeKeyEvents(boolean get) {
        this.mDecor.setFocusable(get);
    }

    public boolean superDispatchKeyEvent(KeyEvent event) {
        return this.mDecor.superDispatchKeyEvent(event);
    }

    public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
        return this.mDecor.superDispatchKeyShortcutEvent(event);
    }

    public boolean superDispatchTouchEvent(MotionEvent event) {
        return this.mDecor.superDispatchTouchEvent(event);
    }

    public boolean superDispatchTrackballEvent(MotionEvent event) {
        return this.mDecor.superDispatchTrackballEvent(event);
    }

    public boolean superDispatchGenericMotionEvent(MotionEvent event) {
        return this.mDecor.superDispatchGenericMotionEvent(event);
    }

    protected boolean onKeyDown(int featureId, int keyCode, KeyEvent event) {
        int i = 0;
        DispatcherState dispatcher = this.mDecor != null ? this.mDecor.getKeyDispatcherState() : null;
        switch (keyCode) {
            case 4:
                if (event.getRepeatCount() <= 0 && featureId >= 0) {
                    if (dispatcher != null) {
                        dispatcher.startTracking(event, this);
                    }
                    return true;
                }
            case 24:
            case 25:
            case 164:
                if (this.mMediaController != null) {
                    int direction = 0;
                    switch (keyCode) {
                        case 24:
                            direction = 1;
                            break;
                        case 25:
                            direction = -1;
                            break;
                        case 164:
                            direction = 101;
                            break;
                    }
                    if (InputLog.DEBUG) {
                        boolean z;
                        String str = TAG;
                        StringBuilder append = new StringBuilder().append("KEYCODE_VOLUME is handled by system, mMediaController = ");
                        if (this.mMediaController != null) {
                            z = true;
                        }
                        Log.d(str, append.append(z).append(", ").append(event).toString());
                    }
                    this.mMediaController.adjustVolume(direction, 1);
                } else {
                    MediaSessionLegacyHelper.getHelper(getContext()).sendVolumeKeyEvent(event, this.mVolumeControlStreamType, false);
                }
                return true;
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
                return this.mMediaController != null && this.mMediaController.dispatchMediaButtonEvent(event);
            case 82:
                if (featureId >= 0) {
                    i = featureId;
                }
                onKeyDownPanel(i, event);
                return true;
        }
        return false;
    }

    private KeyguardManager getKeyguardManager() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) getContext().getSystemService("keyguard");
        }
        return this.mKeyguardManager;
    }

    AudioManager getAudioManager() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
        }
        return this.mAudioManager;
    }

    protected boolean onKeyUp(int featureId, int keyCode, KeyEvent event) {
        int i = 0;
        DispatcherState dispatcher = this.mDecor != null ? this.mDecor.getKeyDispatcherState() : null;
        if (dispatcher != null) {
            dispatcher.handleUpEvent(event);
        }
        switch (keyCode) {
            case 4:
                if (featureId >= 0 && event.isTracking() && (event.isCanceled() ^ 1) != 0) {
                    if (featureId == 0) {
                        PanelFeatureState st = getPanelState(featureId, false);
                        if (st != null && st.isInExpandedMode) {
                            reopenMenu(true);
                            return true;
                        }
                    }
                    closePanel(featureId);
                    return true;
                }
            case 24:
            case 25:
                if (InputLog.DEBUG) {
                    Log.d(TAG, "KEYCODE_VOLUME is handled by system, mMediaController = " + (this.mMediaController != null) + ", " + event);
                }
                if (this.mMediaController != null) {
                    this.mMediaController.adjustVolume(0, 4116);
                } else {
                    MediaSessionLegacyHelper.getHelper(getContext()).sendVolumeKeyEvent(event, this.mVolumeControlStreamType, false);
                }
                return true;
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
                return this.mMediaController != null && this.mMediaController.dispatchMediaButtonEvent(event);
            case 82:
                if (featureId >= 0) {
                    i = featureId;
                }
                onKeyUpPanel(i, event);
                return true;
            case 84:
                if (!getKeyguardManager().inKeyguardRestrictedInputMode()) {
                    if (event.isTracking() && (event.isCanceled() ^ 1) != 0) {
                        launchDefaultSearch(event);
                    }
                    return true;
                }
                break;
            case 164:
                MediaSessionLegacyHelper.getHelper(getContext()).sendVolumeKeyEvent(event, Integer.MIN_VALUE, false);
                if (InputLog.DEBUG) {
                    InputLog.d(TAG, " getAudioManager.handleKeyUp() in PhoneWindow ");
                }
                return true;
            case 171:
                if (this.mSupportsPictureInPicture && (event.isCanceled() ^ 1) != 0) {
                    getWindowControllerCallback().enterPictureInPictureModeIfPossible();
                }
                return true;
        }
        return false;
    }

    protected void onActive() {
    }

    public final View getDecorView() {
        if (this.mDecor == null || this.mForceDecorInstall) {
            installDecor();
        }
        return this.mDecor;
    }

    public final View peekDecorView() {
        return this.mDecor;
    }

    void onViewRootImplSet(ViewRootImpl viewRoot) {
        viewRoot.setActivityConfigCallback(this.mActivityConfigCallback);
    }

    public Bundle saveHierarchyState() {
        Bundle outState = new Bundle();
        if (this.mContentParent == null) {
            return outState;
        }
        SparseArray<Parcelable> states = new SparseArray();
        this.mContentParent.saveHierarchyState(states);
        outState.putSparseParcelableArray(VIEWS_TAG, states);
        View focusedView = this.mContentParent.findFocus();
        if (!(focusedView == null || focusedView.getId() == -1)) {
            outState.putInt(FOCUSED_ID_TAG, focusedView.getId());
        }
        SparseArray<Parcelable> panelStates = new SparseArray();
        savePanelState(panelStates);
        if (panelStates.size() > 0) {
            outState.putSparseParcelableArray(PANELS_TAG, panelStates);
        }
        if (this.mDecorContentParent != null) {
            SparseArray<Parcelable> actionBarStates = new SparseArray();
            this.mDecorContentParent.saveToolbarHierarchyState(actionBarStates);
            outState.putSparseParcelableArray(ACTION_BAR_TAG, actionBarStates);
        }
        return outState;
    }

    public void restoreHierarchyState(Bundle savedInstanceState) {
        if (this.mContentParent != null) {
            SparseArray<Parcelable> savedStates = savedInstanceState.getSparseParcelableArray(VIEWS_TAG);
            if (savedStates != null) {
                this.mContentParent.restoreHierarchyState(savedStates);
            }
            int focusedViewId = savedInstanceState.getInt(FOCUSED_ID_TAG, -1);
            if (focusedViewId != -1) {
                View needsFocus = this.mContentParent.findViewById(focusedViewId);
                if (needsFocus != null) {
                    needsFocus.requestFocus();
                } else {
                    Log.w(TAG, "Previously focused view reported id " + focusedViewId + " during save, but can't be found during restore.");
                }
            }
            SparseArray<Parcelable> panelStates = savedInstanceState.getSparseParcelableArray(PANELS_TAG);
            if (panelStates != null) {
                restorePanelState(panelStates);
            }
            if (this.mDecorContentParent != null) {
                SparseArray<Parcelable> actionBarStates = savedInstanceState.getSparseParcelableArray(ACTION_BAR_TAG);
                if (actionBarStates != null) {
                    doPendingInvalidatePanelMenu();
                    this.mDecorContentParent.restoreToolbarHierarchyState(actionBarStates);
                } else {
                    Log.w(TAG, "Missing saved instance states for action bar views! State will not be restored.");
                }
            }
        }
    }

    private void savePanelState(SparseArray<Parcelable> icicles) {
        PanelFeatureState[] panels = this.mPanels;
        if (panels != null) {
            for (int curFeatureId = panels.length - 1; curFeatureId >= 0; curFeatureId--) {
                if (panels[curFeatureId] != null) {
                    icicles.put(curFeatureId, panels[curFeatureId].onSaveInstanceState());
                }
            }
        }
    }

    private void restorePanelState(SparseArray<Parcelable> icicles) {
        for (int i = icicles.size() - 1; i >= 0; i--) {
            int curFeatureId = icicles.keyAt(i);
            PanelFeatureState st = getPanelState(curFeatureId, false);
            if (st != null) {
                st.onRestoreInstanceState((Parcelable) icicles.get(curFeatureId));
                invalidatePanelMenu(curFeatureId);
            }
        }
    }

    void openPanelsAfterRestore() {
        PanelFeatureState[] panels = this.mPanels;
        if (panels != null) {
            for (int i = panels.length - 1; i >= 0; i--) {
                PanelFeatureState st = panels[i];
                if (st != null) {
                    st.applyFrozenState();
                    if (!st.isOpen && st.wasLastOpen) {
                        st.isInExpandedMode = st.wasLastExpanded;
                        openPanel(st, null);
                    }
                }
            }
        }
    }

    protected DecorView generateDecor(int featureId) {
        Context context;
        if (this.mUseDecorContext) {
            Context applicationContext = getContext().getApplicationContext();
            if (applicationContext == null) {
                context = getContext();
            } else {
                context = new DecorContext(applicationContext, getContext().getResources());
                if (this.mTheme != -1) {
                    context.setTheme(this.mTheme);
                }
            }
        } else {
            context = getContext();
        }
        return new DecorView(context, featureId, this, getAttributes());
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "XiaoKang.Feng@Plf.SDK : Modify for OppoStyle ActionBar", property = OppoRomType.ROM)
    protected ViewGroup generateLayout(DecorView decor) {
        String pkg;
        int layoutResource;
        TypedArray a = getWindowStyle();
        this.mIsFloating = a.getBoolean(4, false);
        int flagsToUpdate = 65792 & (~getForcedWindowFlags());
        if (this.mIsFloating) {
            setLayout(-2, -2);
            setFlags(0, flagsToUpdate);
        } else {
            setFlags(65792, flagsToUpdate);
        }
        if (a.getBoolean(3, false)) {
            requestFeature(1);
        } else if (a.getBoolean(15, false)) {
            requestFeature(8);
        }
        if (a.getBoolean(17, false)) {
            requestFeature(9);
        }
        if (a.getBoolean(16, false)) {
            requestFeature(10);
        }
        if (a.getBoolean(25, false)) {
            requestFeature(11);
        }
        if (a.getBoolean(9, false)) {
            setFlags(1024, (~getForcedWindowFlags()) & 1024);
        }
        if (a.getBoolean(23, false)) {
            setFlags(67108864, (~getForcedWindowFlags()) & 67108864);
        }
        if (a.getBoolean(24, false)) {
            setFlags(134217728, (~getForcedWindowFlags()) & 134217728);
        }
        if (a.getBoolean(22, false)) {
            setFlags(33554432, (~getForcedWindowFlags()) & 33554432);
        }
        if (a.getBoolean(14, false)) {
            setFlags(1048576, (~getForcedWindowFlags()) & 1048576);
        }
        if (a.getBoolean(18, getContext().getApplicationInfo().targetSdkVersion >= 11)) {
            setFlags(8388608, (~getForcedWindowFlags()) & 8388608);
        }
        a.getValue(19, this.mMinWidthMajor);
        a.getValue(20, this.mMinWidthMinor);
        if (a.hasValue(54)) {
            if (this.mFixedWidthMajor == null) {
                this.mFixedWidthMajor = new TypedValue();
            }
            a.getValue(54, this.mFixedWidthMajor);
        }
        if (a.hasValue(55)) {
            if (this.mFixedWidthMinor == null) {
                this.mFixedWidthMinor = new TypedValue();
            }
            a.getValue(55, this.mFixedWidthMinor);
        }
        if (a.hasValue(52)) {
            if (this.mFixedHeightMajor == null) {
                this.mFixedHeightMajor = new TypedValue();
            }
            a.getValue(52, this.mFixedHeightMajor);
        }
        if (a.hasValue(53)) {
            if (this.mFixedHeightMinor == null) {
                this.mFixedHeightMinor = new TypedValue();
            }
            a.getValue(53, this.mFixedHeightMinor);
        }
        if (a.getBoolean(26, false)) {
            requestFeature(12);
        }
        if (a.getBoolean(45, false)) {
            requestFeature(13);
        }
        this.mIsTranslucent = a.getBoolean(5, false);
        Context context = getContext();
        int targetSdk = context.getApplicationInfo().targetSdkVersion;
        boolean targetPreHoneycomb = targetSdk < 11;
        boolean targetPreIcs = targetSdk < 14;
        boolean targetPreL = targetSdk < 21;
        boolean targetHcNeedsOptions = context.getResources().getBoolean(R.bool.target_honeycomb_needs_options_menu);
        boolean noActionBar = hasFeature(8) ? hasFeature(1) : true;
        if (targetPreHoneycomb || (targetPreIcs && targetHcNeedsOptions && noActionBar)) {
            setNeedsMenuKey(1);
        } else {
            setNeedsMenuKey(2);
        }
        if (!this.mForcedStatusBarColor) {
            pkg = context.getPackageName();
            if (this.mAdapationStatusBarColor != 0) {
                this.mStatusBarColor = this.mAdapationStatusBarColor;
                if (DEBUG_OPPO_SYSTEMBAR) {
                    Log.d(TAG, "adapter status bar color mStatusBarColor:" + Integer.toHexString(this.mStatusBarColor) + ", mAdapationStatusBarColor:" + Integer.toHexString(this.mAdapationStatusBarColor) + ", window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
                }
                WindowManager.LayoutParams attr = getAttributes();
                attr.navigationBarVisibility |= 134217728;
                if (isColorDark(this.mStatusBarColor)) {
                    if (DEBUG_OPPO_SYSTEMBAR) {
                        Log.d(TAG, "set status bar color icon dark, window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
                    }
                    attr.navigationBarVisibility |= 67108864;
                } else {
                    if (DEBUG_OPPO_SYSTEMBAR) {
                        Log.d(TAG, "set status bar color icon light, window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
                    }
                    attr.navigationBarVisibility &= -67108865;
                }
            } else {
                this.mStatusBarColor = a.getColor(35, -16777216);
            }
        }
        if (!this.mForcedNavigationBarColor) {
            pkg = context.getPackageName();
            int navigationBarColor = getNavBarColorFromAdaptation();
            if (navigationBarColor != 0) {
                this.mNavigationBarColor = navigationBarColor;
            } else if ("com.google.android.setupwizard".equals(pkg) || "com.google.android.gms".equals(pkg)) {
                this.mNavigationBarColor = NAVIGATION_BAR_COLOR_GRAY;
            } else {
                this.mNavigationBarColor = a.getColor(36, NAVIGATION_BAR_COLOR_GRAY);
                if (this.mNavigationBarColor != -263173) {
                    this.mNavColorIsAppCustom = true;
                }
            }
            this.mNavigationBarDividerColor = a.getColor(50, 0);
            if (this.mNavigationBarDividerColor != 0) {
                this.mNavColorIsAppCustom = true;
            }
            if (DEBUG_OPPO_SYSTEMBAR) {
                Log.d(TAG, "generate navigation bar color mNavigationBarColor:" + Integer.toHexString(this.mNavigationBarColor) + ", mNavigationBarDividerColor:" + Integer.toHexString(this.mNavigationBarDividerColor) + ", window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + ", navigationBarColor:" + Integer.toHexString(navigationBarColor) + " @" + this);
            }
        }
        WindowManager.LayoutParams params = getAttributes();
        if (!this.mIsFloating) {
            if (!targetPreL && a.getBoolean(34, false)) {
                setFlags(Integer.MIN_VALUE, (~getForcedWindowFlags()) & Integer.MIN_VALUE);
            }
            if (this.mDecor.mForceWindowDrawsStatusBarBackground) {
                params.privateFlags |= 131072;
            }
        }
        if (a.getBoolean(46, false)) {
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() | 8192);
        }
        if (a.getBoolean(49, false)) {
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() | 16);
        }
        if ((this.mAlwaysReadCloseOnTouchAttr || getContext().getApplicationInfo().targetSdkVersion >= 11) && a.getBoolean(21, false)) {
            setCloseOnTouchOutsideIfNotSet(true);
        }
        if (!hasSoftInputMode()) {
            params.softInputMode = a.getInt(13, params.softInputMode);
        }
        if (a.getBoolean(11, this.mIsFloating)) {
            if ((getForcedWindowFlags() & 2) == 0) {
                params.flags |= 2;
            }
            if (!haveDimAmount()) {
                params.dimAmount = a.getFloat(0, 0.5f);
            }
        }
        if (params.windowAnimations == 0) {
            params.windowAnimations = a.getResourceId(8, 0);
        }
        if (getContainer() == null) {
            if (this.mBackgroundDrawable == null) {
                if (this.mBackgroundResource == 0) {
                    this.mBackgroundResource = a.getResourceId(1, 0);
                }
                if (this.mFrameResource == 0) {
                    this.mFrameResource = a.getResourceId(2, 0);
                }
                this.mBackgroundFallbackResource = a.getResourceId(47, 0);
            }
            if (this.mLoadElevation) {
                this.mElevation = a.getDimension(38, 0.0f);
            }
            this.mClipToOutline = a.getBoolean(39, false);
            this.mTextColor = a.getColor(7, 0);
        }
        int features = getLocalFeatures();
        TypedValue res;
        if ((features & 2048) != 0) {
            layoutResource = R.layout.screen_swipe_dismiss;
            setCloseOnSwipeEnabled(true);
        } else if ((features & 24) != 0) {
            if (this.mIsFloating) {
                res = new TypedValue();
                getContext().getTheme().resolveAttribute(R.attr.dialogTitleIconsDecorLayout, res, true);
                layoutResource = res.resourceId;
            } else {
                layoutResource = R.layout.screen_title_icons;
            }
            removeFeature(8);
        } else if ((features & 36) != 0 && (features & 256) == 0) {
            layoutResource = R.layout.screen_progress;
        } else if ((features & 128) != 0) {
            if (this.mIsFloating) {
                res = new TypedValue();
                getContext().getTheme().resolveAttribute(R.attr.dialogCustomTitleDecorLayout, res, true);
                layoutResource = res.resourceId;
            } else {
                layoutResource = R.layout.screen_custom_title;
            }
            removeFeature(8);
        } else if ((features & 2) == 0) {
            if (this.mIsFloating) {
                res = new TypedValue();
                getContext().getTheme().resolveAttribute(R.attr.dialogTitleDecorLayout, res, true);
                layoutResource = res.resourceId;
            } else if ((features & 256) != 0) {
                layoutResource = PhoneWindow.overlayStartingLayout(this, a.getResourceId(51, R.layout.screen_action_bar));
            } else {
                layoutResource = R.layout.screen_title;
            }
        } else if ((features & 1024) != 0) {
            layoutResource = R.layout.screen_simple_overlay_action_mode;
        } else {
            layoutResource = R.layout.screen_simple;
        }
        this.mDecor.startChanging();
        this.mDecor.onResourcesLoaded(this.mLayoutInflater, layoutResource);
        ViewGroup contentParent = (ViewGroup) findViewById(16908290);
        if (contentParent == null) {
            throw new RuntimeException("Window couldn't find content container view");
        }
        if ((features & 32) != 0) {
            ProgressBar progress = getCircularProgressBar(false);
            if (progress != null) {
                progress.setIndeterminate(true);
            }
        }
        if ((features & 2048) != 0) {
            registerSwipeCallbacks(contentParent);
        }
        if (getContainer() == null) {
            Drawable background;
            Drawable frame;
            if (this.mBackgroundResource != 0) {
                background = getContext().getDrawable(this.mBackgroundResource);
            } else {
                background = this.mBackgroundDrawable;
            }
            this.mDecor.setWindowBackground(background);
            if (this.mFrameResource != 0) {
                frame = getContext().getDrawable(this.mFrameResource);
            } else {
                frame = null;
            }
            this.mDecor.setWindowFrame(frame);
            this.mDecor.setElevation(this.mElevation);
            this.mDecor.setClipToOutline(this.mClipToOutline);
            if (this.mTitle != null) {
                setTitle(this.mTitle);
            }
            if (this.mTitleColor == 0) {
                this.mTitleColor = this.mTextColor;
            }
            setTitleColor(this.mTitleColor);
        }
        if (!this.mForcedNavigationBarColor) {
            if (this.mIsStartingWindow) {
                Drawable windowBackground = this.mDecor.getBackground();
                if (DEBUG_OPPO_SYSTEMBAR) {
                    Log.d(TAG, "starting window background " + windowBackground + ", mNavigationBarColor:" + Integer.toHexString(this.mNavigationBarColor) + ", pkg" + context.getPackageName() + " @" + this);
                }
                if ((windowBackground instanceof ColorDrawable) || "com.coloros.weather".equals(context.getPackageName())) {
                    this.mNavigationBarColor = 0;
                    setFlags(Integer.MIN_VALUE, (~getForcedWindowFlags()) & Integer.MIN_VALUE);
                } else if ((params.flags & Integer.MIN_VALUE) == 0) {
                    params.navigationBarVisibility |= 536870912;
                }
            }
            updateNavigationBarLightFlag();
        }
        this.mDecor.finishChanging();
        return contentParent;
    }

    public void alwaysReadCloseOnTouchAttr() {
        this.mAlwaysReadCloseOnTouchAttr = true;
    }

    private void installDecor() {
        this.mForceDecorInstall = false;
        if (this.mDecor == null) {
            this.mDecor = generateDecor(-1);
            this.mDecor.setDescendantFocusability(262144);
            this.mDecor.setIsRootNamespace(true);
            if (!(this.mInvalidatePanelMenuPosted || this.mInvalidatePanelMenuFeatures == 0)) {
                this.mDecor.postOnAnimation(this.mInvalidatePanelMenuRunnable);
            }
        } else {
            this.mDecor.setWindow(this);
        }
        if (this.mContentParent == null) {
            this.mContentParent = generateLayout(this.mDecor);
            this.mDecor.makeOptionalFitsSystemWindows();
            DecorContentParent decorContentParent = (DecorContentParent) this.mDecor.findViewById(R.id.decor_content_parent);
            if (decorContentParent != null) {
                this.mDecorContentParent = decorContentParent;
                this.mDecorContentParent.setWindowCallback(getCallback());
                if (this.mDecorContentParent.getTitle() == null) {
                    this.mDecorContentParent.setWindowTitle(this.mTitle);
                }
                int localFeatures = getLocalFeatures();
                for (int i = 0; i < 13; i++) {
                    if (((1 << i) & localFeatures) != 0) {
                        this.mDecorContentParent.initFeature(i);
                    }
                }
                this.mDecorContentParent.setUiOptions(this.mUiOptions);
                if ((this.mResourcesSetFlags & 1) != 0 || (this.mIconRes != 0 && (this.mDecorContentParent.hasIcon() ^ 1) != 0)) {
                    this.mDecorContentParent.setIcon(this.mIconRes);
                } else if ((this.mResourcesSetFlags & 1) == 0 && this.mIconRes == 0 && (this.mDecorContentParent.hasIcon() ^ 1) != 0) {
                    this.mDecorContentParent.setIcon(getContext().getPackageManager().getDefaultActivityIcon());
                    this.mResourcesSetFlags |= 4;
                }
                if (!((this.mResourcesSetFlags & 2) == 0 && (this.mLogoRes == 0 || (this.mDecorContentParent.hasLogo() ^ 1) == 0))) {
                    this.mDecorContentParent.setLogo(this.mLogoRes);
                }
                PanelFeatureState st = getPanelState(0, false);
                if (!isDestroyed() && ((st == null || st.menu == null) && (this.mIsStartingWindow ^ 1) != 0)) {
                    invalidatePanelMenu(8);
                }
            } else {
                this.mTitleView = (TextView) findViewById(R.id.title);
                if (this.mTitleView != null) {
                    if ((getLocalFeatures() & 2) != 0) {
                        View titleContainer = findViewById(R.id.title_container);
                        if (titleContainer != null) {
                            titleContainer.setVisibility(8);
                        } else {
                            this.mTitleView.setVisibility(8);
                        }
                        this.mContentParent.setForeground(null);
                    } else {
                        this.mTitleView.setText(this.mTitle);
                    }
                }
            }
            if (this.mDecor.getBackground() == null && this.mBackgroundFallbackResource != 0) {
                this.mDecor.setBackgroundFallback(this.mBackgroundFallbackResource);
            }
            if (hasFeature(13)) {
                if (this.mTransitionManager == null) {
                    int transitionRes = getWindowStyle().getResourceId(27, 0);
                    if (transitionRes != 0) {
                        this.mTransitionManager = TransitionInflater.from(getContext()).inflateTransitionManager(transitionRes, this.mContentParent);
                    } else {
                        this.mTransitionManager = new TransitionManager();
                    }
                }
                this.mEnterTransition = getTransition(this.mEnterTransition, null, 28);
                this.mReturnTransition = getTransition(this.mReturnTransition, USE_DEFAULT_TRANSITION, 40);
                this.mExitTransition = getTransition(this.mExitTransition, null, 29);
                this.mReenterTransition = getTransition(this.mReenterTransition, USE_DEFAULT_TRANSITION, 41);
                this.mSharedElementEnterTransition = getTransition(this.mSharedElementEnterTransition, null, 30);
                this.mSharedElementReturnTransition = getTransition(this.mSharedElementReturnTransition, USE_DEFAULT_TRANSITION, 42);
                this.mSharedElementExitTransition = getTransition(this.mSharedElementExitTransition, null, 31);
                this.mSharedElementReenterTransition = getTransition(this.mSharedElementReenterTransition, USE_DEFAULT_TRANSITION, 43);
                if (this.mAllowEnterTransitionOverlap == null) {
                    this.mAllowEnterTransitionOverlap = Boolean.valueOf(getWindowStyle().getBoolean(33, true));
                }
                if (this.mAllowReturnTransitionOverlap == null) {
                    this.mAllowReturnTransitionOverlap = Boolean.valueOf(getWindowStyle().getBoolean(32, true));
                }
                if (this.mBackgroundFadeDurationMillis < 0) {
                    this.mBackgroundFadeDurationMillis = (long) getWindowStyle().getInteger(37, 300);
                }
                if (this.mSharedElementsUseOverlay == null) {
                    this.mSharedElementsUseOverlay = Boolean.valueOf(getWindowStyle().getBoolean(44, true));
                }
            }
        }
    }

    private Transition getTransition(Transition currentValue, Transition defaultValue, int id) {
        if (currentValue != defaultValue) {
            return currentValue;
        }
        int transitionId = getWindowStyle().getResourceId(id, -1);
        Transition transition = defaultValue;
        if (!(transitionId == -1 || transitionId == R.transition.no_transition)) {
            transition = TransitionInflater.from(getContext()).inflateTransition(transitionId);
            if ((transition instanceof TransitionSet) && ((TransitionSet) transition).getTransitionCount() == 0) {
                transition = null;
            }
        }
        return transition;
    }

    private Drawable loadImageURI(Uri uri) {
        try {
            return Drawable.createFromStream(getContext().getContentResolver().openInputStream(uri), null);
        } catch (Exception e) {
            Log.w(TAG, "Unable to open content: " + uri);
            return null;
        }
    }

    private DrawableFeatureState getDrawableState(int featureId, boolean required) {
        if ((getFeatures() & (1 << featureId)) != 0) {
            DrawableFeatureState[] ar = this.mDrawables;
            if (ar == null || ar.length <= featureId) {
                DrawableFeatureState[] nar = new DrawableFeatureState[(featureId + 1)];
                if (ar != null) {
                    System.arraycopy(ar, 0, nar, 0, ar.length);
                }
                ar = nar;
                this.mDrawables = nar;
            }
            DrawableFeatureState st = ar[featureId];
            if (st == null) {
                st = new DrawableFeatureState(featureId);
                ar[featureId] = st;
            }
            return st;
        } else if (!required) {
            return null;
        } else {
            throw new RuntimeException("The feature has not been requested");
        }
    }

    PanelFeatureState getPanelState(int featureId, boolean required) {
        return getPanelState(featureId, required, null);
    }

    private PanelFeatureState getPanelState(int featureId, boolean required, PanelFeatureState convertPanelState) {
        if ((getFeatures() & (1 << featureId)) != 0) {
            PanelFeatureState[] ar = this.mPanels;
            if (ar == null || ar.length <= featureId) {
                PanelFeatureState[] nar = new PanelFeatureState[(featureId + 1)];
                if (ar != null) {
                    System.arraycopy(ar, 0, nar, 0, ar.length);
                }
                ar = nar;
                this.mPanels = nar;
            }
            PanelFeatureState st = ar[featureId];
            if (st == null) {
                if (convertPanelState != null) {
                    st = convertPanelState;
                } else {
                    st = new PanelFeatureState(featureId);
                }
                ar[featureId] = st;
            }
            return st;
        } else if (!required) {
            return null;
        } else {
            throw new RuntimeException("The feature has not been requested");
        }
    }

    public final void setChildDrawable(int featureId, Drawable drawable) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        st.child = drawable;
        updateDrawable(featureId, st, false);
    }

    public final void setChildInt(int featureId, int value) {
        updateInt(featureId, value, false);
    }

    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        PanelFeatureState st = getPanelState(0, false);
        if (st == null || st.menu == null) {
            return false;
        }
        return st.menu.isShortcutKey(keyCode, event);
    }

    private void updateDrawable(int featureId, DrawableFeatureState st, boolean fromResume) {
        if (this.mContentParent != null) {
            int featureMask = 1 << featureId;
            if ((getFeatures() & featureMask) != 0 || (fromResume ^ 1) == 0) {
                Drawable drawable = null;
                if (st != null) {
                    drawable = st.child;
                    if (drawable == null) {
                        drawable = st.local;
                    }
                    if (drawable == null) {
                        drawable = st.def;
                    }
                }
                if ((getLocalFeatures() & featureMask) == 0) {
                    if (getContainer() != null && (isActive() || fromResume)) {
                        getContainer().setChildDrawable(featureId, drawable);
                    }
                } else if (!(st == null || (st.cur == drawable && st.curAlpha == st.alpha))) {
                    st.cur = drawable;
                    st.curAlpha = st.alpha;
                    onDrawableChanged(featureId, drawable, st.alpha);
                }
            }
        }
    }

    private void updateInt(int featureId, int value, boolean fromResume) {
        if (this.mContentParent != null) {
            int featureMask = 1 << featureId;
            if ((getFeatures() & featureMask) != 0 || (fromResume ^ 1) == 0) {
                if ((getLocalFeatures() & featureMask) != 0) {
                    onIntChanged(featureId, value);
                } else if (getContainer() != null) {
                    getContainer().setChildInt(featureId, value);
                }
            }
        }
    }

    private ImageView getLeftIconView() {
        if (this.mLeftIconView != null) {
            return this.mLeftIconView;
        }
        if (this.mContentParent == null) {
            installDecor();
        }
        ImageView imageView = (ImageView) findViewById(R.id.left_icon);
        this.mLeftIconView = imageView;
        return imageView;
    }

    protected void dispatchWindowAttributesChanged(WindowManager.LayoutParams attrs) {
        super.dispatchWindowAttributesChanged(attrs);
        if (this.mDecor != null) {
            this.mDecor.updateColorViews(null, true);
        }
    }

    private ProgressBar getCircularProgressBar(boolean shouldInstallDecor) {
        if (this.mCircularProgressBar != null) {
            return this.mCircularProgressBar;
        }
        if (this.mContentParent == null && shouldInstallDecor) {
            installDecor();
        }
        this.mCircularProgressBar = (ProgressBar) findViewById(R.id.progress_circular);
        if (this.mCircularProgressBar != null) {
            this.mCircularProgressBar.setVisibility(4);
        }
        return this.mCircularProgressBar;
    }

    private ProgressBar getHorizontalProgressBar(boolean shouldInstallDecor) {
        if (this.mHorizontalProgressBar != null) {
            return this.mHorizontalProgressBar;
        }
        if (this.mContentParent == null && shouldInstallDecor) {
            installDecor();
        }
        this.mHorizontalProgressBar = (ProgressBar) findViewById(R.id.progress_horizontal);
        if (this.mHorizontalProgressBar != null) {
            this.mHorizontalProgressBar.setVisibility(4);
        }
        return this.mHorizontalProgressBar;
    }

    private ImageView getRightIconView() {
        if (this.mRightIconView != null) {
            return this.mRightIconView;
        }
        if (this.mContentParent == null) {
            installDecor();
        }
        ImageView imageView = (ImageView) findViewById(R.id.right_icon);
        this.mRightIconView = imageView;
        return imageView;
    }

    private void registerSwipeCallbacks(ViewGroup contentParent) {
        if (contentParent instanceof SwipeDismissLayout) {
            SwipeDismissLayout swipeDismiss = (SwipeDismissLayout) contentParent;
            swipeDismiss.setOnDismissedListener(new OnDismissedListener() {
                public void onDismissed(SwipeDismissLayout layout) {
                    PhoneWindow.this.dispatchOnWindowSwipeDismissed();
                    PhoneWindow.this.dispatchOnWindowDismissed(false, true);
                }
            });
            swipeDismiss.setOnSwipeProgressChangedListener(new OnSwipeProgressChangedListener() {
                public void onSwipeProgressChanged(SwipeDismissLayout layout, float alpha, float translate) {
                    int flags;
                    WindowManager.LayoutParams newParams = PhoneWindow.this.getAttributes();
                    newParams.x = (int) translate;
                    newParams.alpha = alpha;
                    PhoneWindow.this.setAttributes(newParams);
                    if (newParams.x == 0) {
                        flags = 1024;
                    } else {
                        flags = 512;
                    }
                    PhoneWindow.this.setFlags(flags, View.SYSTEM_UI_LAYOUT_FLAGS);
                }

                public void onSwipeCancelled(SwipeDismissLayout layout) {
                    WindowManager.LayoutParams newParams = PhoneWindow.this.getAttributes();
                    if (newParams.x != 0 || newParams.alpha != 1.0f) {
                        newParams.x = 0;
                        newParams.alpha = 1.0f;
                        PhoneWindow.this.setAttributes(newParams);
                        PhoneWindow.this.setFlags(1024, View.SYSTEM_UI_LAYOUT_FLAGS);
                    }
                }
            });
            return;
        }
        Log.w(TAG, "contentParent is not a SwipeDismissLayout: " + contentParent);
    }

    public void setCloseOnSwipeEnabled(boolean closeOnSwipeEnabled) {
        if (hasFeature(11) && (this.mContentParent instanceof SwipeDismissLayout)) {
            ((SwipeDismissLayout) this.mContentParent).setDismissable(closeOnSwipeEnabled);
        }
        super.setCloseOnSwipeEnabled(closeOnSwipeEnabled);
    }

    private void callOnPanelClosed(int featureId, PanelFeatureState panel, Menu menu) {
        Window.Callback cb = getCallback();
        if (cb != null) {
            if (menu == null) {
                if (panel == null && featureId >= 0 && featureId < this.mPanels.length) {
                    panel = this.mPanels[featureId];
                }
                if (panel != null) {
                    menu = panel.menu;
                }
            }
            if ((panel == null || (panel.isOpen ^ 1) == 0) && !isDestroyed()) {
                cb.onPanelClosed(featureId, menu);
            }
        }
    }

    private boolean isTvUserSetupComplete() {
        int i = 0;
        boolean isTvSetupComplete = Secure.getInt(getContext().getContentResolver(), Secure.USER_SETUP_COMPLETE, 0) != 0;
        if (Secure.getInt(getContext().getContentResolver(), Secure.TV_USER_SETUP_COMPLETE, 0) != 0) {
            i = 1;
        }
        return isTvSetupComplete & i;
    }

    private boolean launchDefaultSearch(KeyEvent event) {
        if (getContext().getPackageManager().hasSystemFeature("android.software.leanback") && (isTvUserSetupComplete() ^ 1) != 0) {
            return false;
        }
        boolean result;
        Window.Callback cb = getCallback();
        if (cb == null || isDestroyed()) {
            result = false;
        } else {
            sendCloseSystemWindows("search");
            int deviceId = event.getDeviceId();
            SearchEvent searchEvent = null;
            if (deviceId != 0) {
                searchEvent = new SearchEvent(InputDevice.getDevice(deviceId));
            }
            try {
                result = cb.onSearchRequested(searchEvent);
            } catch (AbstractMethodError e) {
                Log.e(TAG, "WindowCallback " + cb.getClass().getName() + " does not implement" + " method onSearchRequested(SearchEvent); fa", e);
                result = cb.onSearchRequested();
            }
        }
        if (result || (getContext().getResources().getConfiguration().uiMode & 15) != 4) {
            return result;
        }
        Bundle args = new Bundle();
        args.putInt("android.intent.extra.ASSIST_INPUT_DEVICE_ID", event.getDeviceId());
        return ((SearchManager) getContext().getSystemService("search")).launchLegacyAssist(null, UserHandle.myUserId(), args);
    }

    public void setVolumeControlStream(int streamType) {
        this.mVolumeControlStreamType = streamType;
    }

    public int getVolumeControlStream() {
        return this.mVolumeControlStreamType;
    }

    public void setMediaController(MediaController controller) {
        this.mMediaController = controller;
    }

    public MediaController getMediaController() {
        return this.mMediaController;
    }

    public void setEnterTransition(Transition enterTransition) {
        this.mEnterTransition = enterTransition;
    }

    public void setReturnTransition(Transition transition) {
        this.mReturnTransition = transition;
    }

    public void setExitTransition(Transition exitTransition) {
        this.mExitTransition = exitTransition;
    }

    public void setReenterTransition(Transition transition) {
        this.mReenterTransition = transition;
    }

    public void setSharedElementEnterTransition(Transition sharedElementEnterTransition) {
        this.mSharedElementEnterTransition = sharedElementEnterTransition;
    }

    public void setSharedElementReturnTransition(Transition transition) {
        this.mSharedElementReturnTransition = transition;
    }

    public void setSharedElementExitTransition(Transition sharedElementExitTransition) {
        this.mSharedElementExitTransition = sharedElementExitTransition;
    }

    public void setSharedElementReenterTransition(Transition transition) {
        this.mSharedElementReenterTransition = transition;
    }

    public Transition getEnterTransition() {
        return this.mEnterTransition;
    }

    public Transition getReturnTransition() {
        if (this.mReturnTransition == USE_DEFAULT_TRANSITION) {
            return getEnterTransition();
        }
        return this.mReturnTransition;
    }

    public Transition getExitTransition() {
        return this.mExitTransition;
    }

    public Transition getReenterTransition() {
        if (this.mReenterTransition == USE_DEFAULT_TRANSITION) {
            return getExitTransition();
        }
        return this.mReenterTransition;
    }

    public Transition getSharedElementEnterTransition() {
        return this.mSharedElementEnterTransition;
    }

    public Transition getSharedElementReturnTransition() {
        return this.mSharedElementReturnTransition == USE_DEFAULT_TRANSITION ? getSharedElementEnterTransition() : this.mSharedElementReturnTransition;
    }

    public Transition getSharedElementExitTransition() {
        return this.mSharedElementExitTransition;
    }

    public Transition getSharedElementReenterTransition() {
        return this.mSharedElementReenterTransition == USE_DEFAULT_TRANSITION ? getSharedElementExitTransition() : this.mSharedElementReenterTransition;
    }

    public void setAllowEnterTransitionOverlap(boolean allow) {
        this.mAllowEnterTransitionOverlap = Boolean.valueOf(allow);
    }

    public boolean getAllowEnterTransitionOverlap() {
        return this.mAllowEnterTransitionOverlap == null ? true : this.mAllowEnterTransitionOverlap.booleanValue();
    }

    public void setAllowReturnTransitionOverlap(boolean allowExitTransitionOverlap) {
        this.mAllowReturnTransitionOverlap = Boolean.valueOf(allowExitTransitionOverlap);
    }

    public boolean getAllowReturnTransitionOverlap() {
        return this.mAllowReturnTransitionOverlap == null ? true : this.mAllowReturnTransitionOverlap.booleanValue();
    }

    public long getTransitionBackgroundFadeDuration() {
        if (this.mBackgroundFadeDurationMillis < 0) {
            return 300;
        }
        return this.mBackgroundFadeDurationMillis;
    }

    public void setTransitionBackgroundFadeDuration(long fadeDurationMillis) {
        if (fadeDurationMillis < 0) {
            throw new IllegalArgumentException("negative durations are not allowed");
        }
        this.mBackgroundFadeDurationMillis = fadeDurationMillis;
    }

    public void setSharedElementsUseOverlay(boolean sharedElementsUseOverlay) {
        this.mSharedElementsUseOverlay = Boolean.valueOf(sharedElementsUseOverlay);
    }

    public boolean getSharedElementsUseOverlay() {
        return this.mSharedElementsUseOverlay == null ? true : this.mSharedElementsUseOverlay.booleanValue();
    }

    int getLocalFeaturesPrivate() {
        return super.getLocalFeatures();
    }

    protected void setDefaultWindowFormat(int format) {
        super.setDefaultWindowFormat(format);
    }

    void sendCloseSystemWindows() {
        sendCloseSystemWindows(getContext(), null);
    }

    void sendCloseSystemWindows(String reason) {
        sendCloseSystemWindows(getContext(), reason);
    }

    public static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManager.isSystemReady()) {
            try {
                ActivityManager.getService().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    public int getStatusBarColor() {
        return this.mStatusBarColor;
    }

    public void setStatusBarColor(int color) {
        if (this.mAdapationStatusBarColor == 0) {
            this.mStatusBarColor = color;
            this.mForcedStatusBarColor = true;
            if (DEBUG_OPPO_SYSTEMBAR) {
                Log.d(TAG, "setStatusBarColor mStatusBarColor:" + Integer.toHexString(this.mStatusBarColor) + ", window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
            }
            if (this.mDecor != null) {
                this.mDecor.updateColorViews(null, false);
            }
        }
    }

    public int getNavigationBarColor() {
        return this.mNavigationBarColor;
    }

    public void setNavigationBarColor(int color) {
        this.mNavigationBarColor = color;
        this.mForcedNavigationBarColor = true;
        String pkg = getContext().getPackageName();
        if ("com.google.android.setupwizard".equals(pkg) || "com.google.android.apps.gsa.staticplugins.opa.errorui.OpaErrorActivity".equals(this.mActivityName) || "com.google.android.gms".equals(pkg)) {
            this.mNavigationBarColor = NAVIGATION_BAR_COLOR_GRAY;
        } else {
            this.mNavColorIsAppCustom = true;
        }
        if (DEBUG_OPPO_SYSTEMBAR) {
            Log.d(TAG, "setNavigationBarColor mNavigationBarColor:" + Integer.toHexString(this.mNavigationBarColor) + ", window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
        }
        updateNavigationBarLightFlag();
        if (this.mDecor != null) {
            this.mDecor.updateColorViews(null, false);
            this.mDecor.updateNavigationGuardColor();
        }
    }

    public void setIsStartingWindow(boolean isStartingWindow) {
        this.mIsStartingWindow = isStartingWindow;
    }

    public void setTheme(int resid) {
        this.mTheme = resid;
        if (this.mDecor != null) {
            Context context = this.mDecor.getContext();
            if (context instanceof DecorContext) {
                context.setTheme(resid);
            }
        }
    }

    public void setResizingCaptionDrawable(Drawable drawable) {
        this.mDecor.setUserCaptionBackgroundDrawable(drawable);
    }

    public void setDecorCaptionShade(int decorCaptionShade) {
        this.mDecorCaptionShade = decorCaptionShade;
        if (this.mDecor != null) {
            this.mDecor.updateDecorCaptionShade();
        }
    }

    int getDecorCaptionShade() {
        return this.mDecorCaptionShade;
    }

    public void setAttributes(WindowManager.LayoutParams params) {
        super.setAttributes(params);
        if (this.mDecor != null) {
            this.mDecor.updateLogTag(params);
        }
    }

    void setDrawNavigatinBarBackground(boolean isDrawsNavBarBackground) {
        if (!this.mRunningCts) {
            this.mDrawsNavBarBackground = isDrawsNavBarBackground;
            updateNavigationBarLightFlag();
        }
    }

    void updateNavigationBarLightFlag() {
        WindowManager.LayoutParams params = getAttributes();
        if (isColorDark(this.mNavigationBarColor)) {
            if (DEBUG_OPPO_SYSTEMBAR) {
                Log.d(TAG, "set navigation bar color icon dark, window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
            }
            params.navigationBarVisibility |= Integer.MIN_VALUE;
        } else {
            if (DEBUG_OPPO_SYSTEMBAR) {
                Log.d(TAG, "set navigation bar color icon light, window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
            }
            params.navigationBarVisibility &= Integer.MAX_VALUE;
        }
        if (!this.mDrawsNavBarBackground && DEBUG_OPPO_SYSTEMBAR) {
            Log.d(TAG, "we do not draw navigation bar background in decor view, window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + "  @" + this);
        }
        if (this.mHasAddFullScreenView) {
            if (DEBUG_OPPO_SYSTEMBAR) {
                Log.d(TAG, "window has add full screen view, window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + "  @" + this);
            }
            params.navigationBarVisibility &= Integer.MAX_VALUE;
        }
        if (this.mNavColorIsAppCustom) {
            params.navigationBarVisibility |= 33554432;
        } else {
            params.navigationBarVisibility &= -33554433;
        }
        boolean isFullScreen = (params.x == 0 && params.y == 0 && params.width == -1) ? params.height == -1 : false;
        if (isFullScreen) {
            if ((params.flags & Integer.MIN_VALUE) != 0) {
                if (DEBUG_OPPO_SYSTEMBAR) {
                    Log.d(TAG, "set navigation bar color in decor window :" + Integer.toHexString(this.mNavigationBarColor) + ", window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
                }
                params.navigationBarVisibility &= -1073741825;
            } else {
                if (DEBUG_OPPO_SYSTEMBAR) {
                    Log.d(TAG, "set navigation bar color in navbar window :" + Integer.toHexString(this.mNavigationBarColor) + ", window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
                }
                if (this.mNavigationBarColor == -16777216 || this.mNavigationBarColor == 0) {
                    params.navigationBarVisibility &= -1073741825;
                } else {
                    params.navigationBarVisibility |= 1073741824;
                }
            }
            if (this.mDrawsNavBarBackground) {
                params.navigationBarVisibility |= 268435456;
            } else {
                params.navigationBarVisibility &= -268435457;
            }
            if ("com.pplive.androidphone.ui.MainFragmentActivity".equals(this.mActivityName)) {
                params.navigationBarVisibility |= 1073741824;
            }
        } else {
            if (DEBUG_OPPO_SYSTEMBAR) {
                Log.d(TAG, "the window is not fullscreen, x:" + params.x + " y:" + params.y + " width:" + params.width + " height:" + params.height + ", window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
            }
            params.navigationBarVisibility &= -1073741825;
        }
        params.navigationBarColor = this.mNavigationBarColor;
        if (this.mDecor != null) {
            this.mDecor.updateNavigationBarParams(params);
        }
    }

    public void setWindowPreNavigationBarColor(int color) {
        if (DEBUG_OPPO_SYSTEMBAR) {
            Log.d(TAG, "setWindowPreNavigationBarColor color:" + Integer.toHexString(color) + ", mNavigationBarColor:" + Integer.toHexString(this.mNavigationBarColor) + ", mNeedPaletteColor:" + this.mNeedPaletteColor + ", mForcedNavigationBarColor:" + this.mForcedNavigationBarColor + ", window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
        }
        if (this.mNeedPaletteColor && color != 0 && (this.mForcedNavigationBarColor ^ 1) != 0) {
            this.mNavigationBarColor = color;
            this.mForcedNavigationBarColor = true;
            updateNavigationBarLightFlag();
            if (this.mDecor != null) {
                this.mDecor.updateColorViews(null, false);
                this.mDecor.updateNavigationGuardColor();
            }
        }
    }

    private boolean isColorDark(int color) {
        if (color == 0) {
            return false;
        }
        int alpha = (-16777216 & color) >>> 24;
        if (((int) (((((double) ((Spanned.SPAN_PRIORITY & color) >>> 16)) * 0.299d) + (((double) ((65280 & color) >>> 8)) * 0.587d)) + (((double) (color & 255)) * 0.114d))) <= 150 || alpha <= 156) {
            return false;
        }
        return true;
    }

    private void setNavigationBarPaletteColor(int color) {
        if (isColorDark(this.mNavigationBarColor) == isColorDark(color)) {
            if (DEBUG_OPPO_SYSTEMBAR) {
                Log.d(TAG, "The palette color(0x" + Integer.toHexString(color) + ") is similar to mNavigationBarColor." + " window:" + getContext().getPackageName() + "/" + getContext().getClass().getName());
            }
            return;
        }
        this.mNavigationBarColor = color;
        this.mForcedNavigationBarColor = true;
        if (DEBUG_OPPO_SYSTEMBAR) {
            Log.d(TAG, "setNavigationBarPaletteColor color:" + Integer.toHexString(this.mNavigationBarColor) + ", window:" + getContext().getPackageName() + "/" + getContext().getClass().getName() + ", mIsStartingWindow:" + this.mIsStartingWindow + " @" + this);
        }
        updateNavigationBarLightFlag();
        if (this.mDecor != null) {
            this.mDecor.updateColorViews(null, false);
            this.mDecor.updateNavigationGuardColor();
        }
    }

    private int getNavBarColorFromAdaptation() {
        String pkg = getContext().getPackageName();
        int defaultColor = 0;
        try {
            WindowManagerGlobal.getInstance();
            return WindowManagerGlobal.getWindowManagerService().getNavBarColorFromAdaptation(pkg, this.mActivityName);
        } catch (RemoteException e) {
            Log.w(TAG, "getNavBarColorFromAdaptation " + e);
            return defaultColor;
        }
    }

    private boolean needPaletteColorForNavBar() {
        String pkg = getContext().getPackageName();
        boolean needPalette = false;
        try {
            WindowManagerGlobal.getInstance();
            needPalette = WindowManagerGlobal.getWindowManagerService().isActivityNeedPalette(pkg, this.mActivityName);
        } catch (RemoteException e) {
            Log.w(TAG, "isActivityNeedPalette " + e);
        }
        WindowManager.LayoutParams params = getAttributes();
        boolean isFullScreen = (params.x == 0 && params.y == 0 && params.width == -1) ? params.height == -1 : false;
        if (DEBUG_OPPO_SYSTEMBAR) {
            boolean z;
            String str = TAG;
            StringBuilder append = new StringBuilder().append("needPaletteColorForNavBar window:").append(getContext().getPackageName()).append("/").append(getContext().getClass().getName()).append(" ");
            if (needPalette && isFullScreen) {
                z = this.mIsStartingWindow ^ 1;
            } else {
                z = false;
            }
            Log.d(str, append.append(z).toString());
        }
        if (needPalette && isFullScreen) {
            return this.mIsStartingWindow ^ 1;
        }
        return false;
    }

    private int getStatusBarColorFromAdaptation() {
        String pkg = getContext().getPackageName();
        int defaultColor = 0;
        try {
            WindowManagerGlobal.getInstance();
            return WindowManagerGlobal.getWindowManagerService().getStatusBarColorFromAdaptation(pkg, this.mActivityName);
        } catch (RemoteException e) {
            Log.w(TAG, "getStatusBarColorFromAdaptation " + e);
            return defaultColor;
        }
    }
}
