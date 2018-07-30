package android.service.wallpaper;

import android.app.Service;
import android.app.WallpaperColors;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.SettingsStringUtil;
import android.service.wallpaper.IWallpaperEngine.Stub;
import android.util.Log;
import android.util.MergedConfiguration;
import android.view.Display;
import android.view.IWindowSession;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceHolder.Callback2;
import android.view.WindowInsets;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import com.android.internal.R;
import com.android.internal.os.HandlerCaller;
import com.android.internal.view.BaseIWindow;
import com.android.internal.view.BaseSurfaceHolder;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Supplier;

public abstract class WallpaperService extends Service {
    static final boolean DEBUG = false;
    private static final int DO_ATTACH = 10;
    private static final int DO_DETACH = 20;
    private static final int DO_SET_DESIRED_SIZE = 30;
    private static final int DO_SET_DISPLAY_PADDING = 40;
    private static final int MSG_REQUEST_WALLPAPER_COLORS = 10050;
    private static final int MSG_TOUCH_EVENT = 10040;
    private static final int MSG_UPDATE_SURFACE = 10000;
    private static final int MSG_VISIBILITY_CHANGED = 10010;
    private static final int MSG_WALLPAPER_COMMAND = 10025;
    private static final int MSG_WALLPAPER_OFFSETS = 10020;
    private static final int MSG_WINDOW_MOVED = 10035;
    private static final int MSG_WINDOW_RESIZED = 10030;
    private static final int NOTIFY_COLORS_RATE_LIMIT_MS = 1000;
    public static final String SERVICE_INTERFACE = "android.service.wallpaper.WallpaperService";
    public static final String SERVICE_META_DATA = "android.service.wallpaper";
    static final String TAG = "WallpaperService";
    private final ArrayList<Engine> mActiveEngines = new ArrayList();

    public class Engine {
        final Rect mBackdropFrame;
        HandlerCaller mCaller;
        private final Supplier<Long> mClockFunction;
        IWallpaperConnection mConnection;
        final Rect mContentInsets;
        boolean mCreated;
        int mCurHeight;
        int mCurWidth;
        int mCurWindowFlags;
        int mCurWindowPrivateFlags;
        boolean mDestroyed;
        final Rect mDispatchedContentInsets;
        final Rect mDispatchedOutsets;
        final Rect mDispatchedOverscanInsets;
        final Rect mDispatchedStableInsets;
        Display mDisplay;
        private final DisplayListener mDisplayListener;
        DisplayManager mDisplayManager;
        private int mDisplayState;
        boolean mDrawingAllowed;
        final Rect mFinalStableInsets;
        final Rect mFinalSystemInsets;
        boolean mFixedSizeAllowed;
        int mFormat;
        private final Handler mHandler;
        int mHeight;
        IWallpaperEngineWrapper mIWallpaperEngine;
        boolean mInitializing;
        InputChannel mInputChannel;
        WallpaperInputEventReceiver mInputEventReceiver;
        boolean mIsCreating;
        private long mLastColorInvalidation;
        final LayoutParams mLayout;
        final Object mLock;
        final MergedConfiguration mMergedConfiguration;
        private final Runnable mNotifyColorsChanged;
        boolean mOffsetMessageEnqueued;
        boolean mOffsetsChanged;
        final Rect mOutsets;
        final Rect mOverscanInsets;
        MotionEvent mPendingMove;
        boolean mPendingSync;
        float mPendingXOffset;
        float mPendingXOffsetStep;
        float mPendingYOffset;
        float mPendingYOffsetStep;
        boolean mReportedVisible;
        IWindowSession mSession;
        final Rect mStableInsets;
        boolean mSurfaceCreated;
        final BaseSurfaceHolder mSurfaceHolder;
        int mType;
        boolean mVisible;
        final Rect mVisibleInsets;
        int mWidth;
        final Rect mWinFrame;
        final BaseIWindow mWindow;
        int mWindowFlags;
        int mWindowPrivateFlags;
        IBinder mWindowToken;

        final class WallpaperInputEventReceiver extends InputEventReceiver {
            public WallpaperInputEventReceiver(InputChannel inputChannel, Looper looper) {
                super(inputChannel, looper);
            }

            public void onInputEvent(InputEvent event, int displayId) {
                boolean handled = false;
                try {
                    if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0) {
                        Engine.this.dispatchPointer(MotionEvent.obtainNoHistory((MotionEvent) event));
                        handled = true;
                    }
                    finishInputEvent(event, handled);
                } catch (Throwable th) {
                    finishInputEvent(event, false);
                }
            }
        }

        /* synthetic */ void -android_service_wallpaper_WallpaperService$Engine-mthref-0() {
            notifyColorsChanged();
        }

        public Engine(WallpaperService this$0) {
            this(-$Lambda$htiXs5zQinBXs3seMVLgh3fgmis.$INST$0, Handler.getMain());
        }

        public Engine(Supplier<Long> clockFunction, Handler handler) {
            this.mInitializing = true;
            this.mWindowFlags = 16;
            this.mWindowPrivateFlags = 4;
            this.mCurWindowFlags = this.mWindowFlags;
            this.mCurWindowPrivateFlags = this.mWindowPrivateFlags;
            this.mVisibleInsets = new Rect();
            this.mWinFrame = new Rect();
            this.mOverscanInsets = new Rect();
            this.mContentInsets = new Rect();
            this.mStableInsets = new Rect();
            this.mOutsets = new Rect();
            this.mDispatchedOverscanInsets = new Rect();
            this.mDispatchedContentInsets = new Rect();
            this.mDispatchedStableInsets = new Rect();
            this.mDispatchedOutsets = new Rect();
            this.mFinalSystemInsets = new Rect();
            this.mFinalStableInsets = new Rect();
            this.mBackdropFrame = new Rect();
            this.mMergedConfiguration = new MergedConfiguration();
            this.mLayout = new LayoutParams();
            this.mLock = new Object();
            this.mNotifyColorsChanged = new android.service.wallpaper.-$Lambda$htiXs5zQinBXs3seMVLgh3fgmis.AnonymousClass1(this);
            this.mSurfaceHolder = new BaseSurfaceHolder() {
                {
                    this.mRequestedFormat = 2;
                }

                public boolean onAllowLockCanvas() {
                    return Engine.this.mDrawingAllowed;
                }

                public void onRelayoutContainer() {
                    Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessage(10000));
                }

                public void onUpdateSurface() {
                    Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessage(10000));
                }

                public boolean isCreating() {
                    return Engine.this.mIsCreating;
                }

                public void setFixedSize(int width, int height) {
                    if (Engine.this.mFixedSizeAllowed) {
                        super.setFixedSize(width, height);
                        return;
                    }
                    throw new UnsupportedOperationException("Wallpapers currently only support sizing from layout");
                }

                public void setKeepScreenOn(boolean screenOn) {
                    throw new UnsupportedOperationException("Wallpapers do not support keep screen on");
                }

                private void prepareToDraw() {
                    if (Engine.this.mDisplayState == 3 || Engine.this.mDisplayState == 4) {
                        try {
                            Engine.this.mSession.pokeDrawLock(Engine.this.mWindow);
                        } catch (RemoteException e) {
                        }
                    }
                }

                public Canvas lockCanvas() {
                    prepareToDraw();
                    return super.lockCanvas();
                }

                public Canvas lockCanvas(Rect dirty) {
                    prepareToDraw();
                    return super.lockCanvas(dirty);
                }

                public Canvas lockHardwareCanvas() {
                    prepareToDraw();
                    return super.lockHardwareCanvas();
                }
            };
            this.mWindow = new BaseIWindow() {
                public void resized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, Rect outsets, boolean reportDraw, MergedConfiguration mergedConfiguration, Rect backDropRect, boolean forceLayout, boolean alwaysConsumeNavBar, int displayId) {
                    Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessageIO(10030, reportDraw ? 1 : 0, outsets));
                }

                public void moved(int newX, int newY) {
                    Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessageII(10035, newX, newY));
                }

                public void dispatchAppVisibility(boolean visible) {
                    if (!Engine.this.mIWallpaperEngine.mIsPreview) {
                        Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessageI(10010, visible ? 1 : 0));
                    }
                }

                public void dispatchWallpaperOffsets(float x, float y, float xStep, float yStep, boolean sync) {
                    synchronized (Engine.this.mLock) {
                        Engine.this.mPendingXOffset = x;
                        Engine.this.mPendingYOffset = y;
                        Engine.this.mPendingXOffsetStep = xStep;
                        Engine.this.mPendingYOffsetStep = yStep;
                        if (sync) {
                            Engine.this.mPendingSync = true;
                        }
                        if (!Engine.this.mOffsetMessageEnqueued) {
                            Engine.this.mOffsetMessageEnqueued = true;
                            Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessage(10020));
                        }
                    }
                }

                public void dispatchWallpaperCommand(String action, int x, int y, int z, Bundle extras, boolean sync) {
                    synchronized (Engine.this.mLock) {
                        WallpaperCommand cmd = new WallpaperCommand();
                        cmd.action = action;
                        cmd.x = x;
                        cmd.y = y;
                        cmd.z = z;
                        cmd.extras = extras;
                        cmd.sync = sync;
                        Message msg = Engine.this.mCaller.obtainMessage(10025);
                        msg.obj = cmd;
                        Engine.this.mCaller.sendMessage(msg);
                    }
                }
            };
            this.mDisplayListener = new DisplayListener() {
                public void onDisplayChanged(int displayId) {
                    if (Engine.this.mDisplay.getDisplayId() == displayId) {
                        Engine.this.reportVisibility();
                    }
                }

                public void onDisplayRemoved(int displayId) {
                }

                public void onDisplayAdded(int displayId) {
                }
            };
            this.mClockFunction = clockFunction;
            this.mHandler = handler;
        }

        public SurfaceHolder getSurfaceHolder() {
            return this.mSurfaceHolder;
        }

        public int getDesiredMinimumWidth() {
            return this.mIWallpaperEngine.mReqWidth;
        }

        public int getDesiredMinimumHeight() {
            return this.mIWallpaperEngine.mReqHeight;
        }

        public boolean isVisible() {
            return this.mReportedVisible;
        }

        public boolean isPreview() {
            return this.mIWallpaperEngine.mIsPreview;
        }

        public void setTouchEventsEnabled(boolean enabled) {
            int i;
            if (enabled) {
                i = this.mWindowFlags & -17;
            } else {
                i = this.mWindowFlags | 16;
            }
            this.mWindowFlags = i;
            if (this.mCreated) {
                updateSurface(false, false, false);
            }
        }

        public void setOffsetNotificationsEnabled(boolean enabled) {
            int i;
            if (enabled) {
                i = this.mWindowPrivateFlags | 4;
            } else {
                i = this.mWindowPrivateFlags & -5;
            }
            this.mWindowPrivateFlags = i;
            if (this.mCreated) {
                updateSurface(false, false, false);
            }
        }

        public void setFixedSizeAllowed(boolean allowed) {
            this.mFixedSizeAllowed = allowed;
        }

        public void onCreate(SurfaceHolder surfaceHolder) {
        }

        public void onDestroy() {
        }

        public void onVisibilityChanged(boolean visible) {
        }

        public void onApplyWindowInsets(WindowInsets insets) {
        }

        public void onTouchEvent(MotionEvent event) {
        }

        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
        }

        public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
            return null;
        }

        public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
        }

        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
        }

        public void onSurfaceCreated(SurfaceHolder holder) {
        }

        public void onSurfaceDestroyed(SurfaceHolder holder) {
        }

        public void notifyColorsChanged() {
            long now = ((Long) this.mClockFunction.get()).longValue();
            if (now - this.mLastColorInvalidation < 1000) {
                Log.w(WallpaperService.TAG, "This call has been deferred. You should only call notifyColorsChanged() once every 1.0 seconds.");
                if (!this.mHandler.hasCallbacks(this.mNotifyColorsChanged)) {
                    this.mHandler.postDelayed(this.mNotifyColorsChanged, 1000);
                }
                return;
            }
            this.mLastColorInvalidation = now;
            this.mHandler.removeCallbacks(this.mNotifyColorsChanged);
            try {
                WallpaperColors newColors = onComputeColors();
                if (this.mConnection != null) {
                    this.mConnection.onWallpaperColorsChanged(newColors);
                } else {
                    Log.w(WallpaperService.TAG, "Can't notify system because wallpaper connection was not established.");
                }
            } catch (RemoteException e) {
                Log.w(WallpaperService.TAG, "Can't notify system because wallpaper connection was lost.", e);
            }
        }

        public WallpaperColors onComputeColors() {
            return null;
        }

        protected void dump(String prefix, FileDescriptor fd, PrintWriter out, String[] args) {
            out.print(prefix);
            out.print("mInitializing=");
            out.print(this.mInitializing);
            out.print(" mDestroyed=");
            out.println(this.mDestroyed);
            out.print(prefix);
            out.print("mVisible=");
            out.print(this.mVisible);
            out.print(" mReportedVisible=");
            out.println(this.mReportedVisible);
            out.print(prefix);
            out.print("mDisplay=");
            out.println(this.mDisplay);
            out.print(prefix);
            out.print("mCreated=");
            out.print(this.mCreated);
            out.print(" mSurfaceCreated=");
            out.print(this.mSurfaceCreated);
            out.print(" mIsCreating=");
            out.print(this.mIsCreating);
            out.print(" mDrawingAllowed=");
            out.println(this.mDrawingAllowed);
            out.print(prefix);
            out.print("mWidth=");
            out.print(this.mWidth);
            out.print(" mCurWidth=");
            out.print(this.mCurWidth);
            out.print(" mHeight=");
            out.print(this.mHeight);
            out.print(" mCurHeight=");
            out.println(this.mCurHeight);
            out.print(prefix);
            out.print("mType=");
            out.print(this.mType);
            out.print(" mWindowFlags=");
            out.print(this.mWindowFlags);
            out.print(" mCurWindowFlags=");
            out.println(this.mCurWindowFlags);
            out.print(prefix);
            out.print("mWindowPrivateFlags=");
            out.print(this.mWindowPrivateFlags);
            out.print(" mCurWindowPrivateFlags=");
            out.println(this.mCurWindowPrivateFlags);
            out.print(prefix);
            out.print("mVisibleInsets=");
            out.print(this.mVisibleInsets.toShortString());
            out.print(" mWinFrame=");
            out.print(this.mWinFrame.toShortString());
            out.print(" mContentInsets=");
            out.println(this.mContentInsets.toShortString());
            out.print(prefix);
            out.print("mConfiguration=");
            out.println(this.mMergedConfiguration.getMergedConfiguration());
            out.print(prefix);
            out.print("mLayout=");
            out.println(this.mLayout);
            synchronized (this.mLock) {
                out.print(prefix);
                out.print("mPendingXOffset=");
                out.print(this.mPendingXOffset);
                out.print(" mPendingXOffset=");
                out.println(this.mPendingXOffset);
                out.print(prefix);
                out.print("mPendingXOffsetStep=");
                out.print(this.mPendingXOffsetStep);
                out.print(" mPendingXOffsetStep=");
                out.println(this.mPendingXOffsetStep);
                out.print(prefix);
                out.print("mOffsetMessageEnqueued=");
                out.print(this.mOffsetMessageEnqueued);
                out.print(" mPendingSync=");
                out.println(this.mPendingSync);
                if (this.mPendingMove != null) {
                    out.print(prefix);
                    out.print("mPendingMove=");
                    out.println(this.mPendingMove);
                }
            }
        }

        private void dispatchPointer(MotionEvent event) {
            if (event.isTouchEvent()) {
                synchronized (this.mLock) {
                    if (event.getAction() == 2) {
                        this.mPendingMove = event;
                    } else {
                        this.mPendingMove = null;
                    }
                }
                this.mCaller.sendMessage(this.mCaller.obtainMessageO(10040, event));
                return;
            }
            event.recycle();
        }

        void updateSurface(boolean forceRelayout, boolean forceReport, boolean redrawNeeded) {
            if (this.mDestroyed) {
                Log.w(WallpaperService.TAG, "Ignoring updateSurface: destroyed");
            }
            boolean fixedSize = false;
            int myWidth = this.mSurfaceHolder.getRequestedWidth();
            if (myWidth <= 0) {
                myWidth = -1;
            } else {
                fixedSize = true;
            }
            int myHeight = this.mSurfaceHolder.getRequestedHeight();
            if (myHeight <= 0) {
                myHeight = -1;
            } else {
                fixedSize = true;
            }
            boolean creating = this.mCreated ^ 1;
            boolean surfaceCreating = this.mSurfaceCreated ^ 1;
            boolean formatChanged = this.mFormat != this.mSurfaceHolder.getRequestedFormat();
            boolean sizeChanged = (this.mWidth == myWidth && this.mHeight == myHeight) ? false : true;
            boolean insetsChanged = this.mCreated ^ 1;
            boolean typeChanged = this.mType != this.mSurfaceHolder.getRequestedType();
            boolean flagsChanged = this.mCurWindowFlags == this.mWindowFlags ? this.mCurWindowPrivateFlags != this.mWindowPrivateFlags : true;
            if (forceRelayout || creating || surfaceCreating || formatChanged || sizeChanged || typeChanged || flagsChanged || redrawNeeded || (this.mIWallpaperEngine.mShownReported ^ 1) != 0) {
                try {
                    Rect rect;
                    this.mWidth = myWidth;
                    this.mHeight = myHeight;
                    this.mFormat = this.mSurfaceHolder.getRequestedFormat();
                    this.mType = this.mSurfaceHolder.getRequestedType();
                    this.mLayout.x = 0;
                    this.mLayout.y = 0;
                    this.mLayout.width = myWidth;
                    this.mLayout.height = myHeight;
                    this.mLayout.format = this.mFormat;
                    this.mCurWindowFlags = this.mWindowFlags;
                    this.mLayout.flags = (((this.mWindowFlags | 512) | 65536) | 256) | 8;
                    this.mCurWindowPrivateFlags = this.mWindowPrivateFlags;
                    this.mLayout.privateFlags = this.mWindowPrivateFlags;
                    this.mLayout.memoryType = this.mType;
                    this.mLayout.token = this.mWindowToken;
                    if (!this.mCreated) {
                        WallpaperService.this.obtainStyledAttributes(R.styleable.Window).recycle();
                        this.mLayout.type = this.mIWallpaperEngine.mWindowType;
                        this.mLayout.gravity = 8388659;
                        this.mLayout.setTitle(WallpaperService.this.getClass().getName());
                        this.mLayout.windowAnimations = R.style.Animation_Wallpaper;
                        this.mInputChannel = new InputChannel();
                        if (this.mSession.addToDisplay(this.mWindow, this.mWindow.mSeq, this.mLayout, 0, 0, this.mContentInsets, this.mStableInsets, this.mOutsets, this.mInputChannel) < 0) {
                            Log.w(WallpaperService.TAG, "Failed to add window while updating wallpaper surface.");
                            return;
                        } else {
                            this.mCreated = true;
                            this.mInputEventReceiver = new WallpaperInputEventReceiver(this.mInputChannel, Looper.myLooper());
                        }
                    }
                    this.mSurfaceHolder.mSurfaceLock.lock();
                    this.mDrawingAllowed = true;
                    if (fixedSize) {
                        this.mLayout.surfaceInsets.set(0, 0, 0, 0);
                    } else {
                        this.mLayout.surfaceInsets.set(this.mIWallpaperEngine.mDisplayPadding);
                        rect = this.mLayout.surfaceInsets;
                        rect.left += this.mOutsets.left;
                        rect = this.mLayout.surfaceInsets;
                        rect.top += this.mOutsets.top;
                        rect = this.mLayout.surfaceInsets;
                        rect.right += this.mOutsets.right;
                        rect = this.mLayout.surfaceInsets;
                        rect.bottom += this.mOutsets.bottom;
                    }
                    int relayoutResult = this.mSession.relayout(this.mWindow, this.mWindow.mSeq, this.mLayout, this.mWidth, this.mHeight, 0, 0, this.mWinFrame, this.mOverscanInsets, this.mContentInsets, this.mVisibleInsets, this.mStableInsets, this.mOutsets, this.mBackdropFrame, this.mMergedConfiguration, this.mSurfaceHolder.mSurface);
                    int w = this.mWinFrame.width();
                    int h = this.mWinFrame.height();
                    if (!fixedSize) {
                        Rect padding = this.mIWallpaperEngine.mDisplayPadding;
                        w += ((padding.left + padding.right) + this.mOutsets.left) + this.mOutsets.right;
                        h += ((padding.top + padding.bottom) + this.mOutsets.top) + this.mOutsets.bottom;
                        rect = this.mOverscanInsets;
                        rect.left += padding.left;
                        rect = this.mOverscanInsets;
                        rect.top += padding.top;
                        rect = this.mOverscanInsets;
                        rect.right += padding.right;
                        rect = this.mOverscanInsets;
                        rect.bottom += padding.bottom;
                        rect = this.mContentInsets;
                        rect.left += padding.left;
                        rect = this.mContentInsets;
                        rect.top += padding.top;
                        rect = this.mContentInsets;
                        rect.right += padding.right;
                        rect = this.mContentInsets;
                        rect.bottom += padding.bottom;
                        rect = this.mStableInsets;
                        rect.left += padding.left;
                        rect = this.mStableInsets;
                        rect.top += padding.top;
                        rect = this.mStableInsets;
                        rect.right += padding.right;
                        rect = this.mStableInsets;
                        rect.bottom += padding.bottom;
                    }
                    if (this.mCurWidth != w) {
                        sizeChanged = true;
                        this.mCurWidth = w;
                    }
                    if (this.mCurHeight != h) {
                        sizeChanged = true;
                        this.mCurHeight = h;
                    }
                    insetsChanged = (((insetsChanged | (this.mDispatchedOverscanInsets.equals(this.mOverscanInsets) ^ 1)) | (this.mDispatchedContentInsets.equals(this.mContentInsets) ^ 1)) | (this.mDispatchedStableInsets.equals(this.mStableInsets) ^ 1)) | (this.mDispatchedOutsets.equals(this.mOutsets) ^ 1);
                    this.mSurfaceHolder.setSurfaceFrameSize(w, h);
                    this.mSurfaceHolder.mSurfaceLock.unlock();
                    if (this.mSurfaceHolder.mSurface.isValid()) {
                        Callback[] callbacks;
                        boolean didSurface = false;
                        this.mSurfaceHolder.ungetCallbacks();
                        if (surfaceCreating) {
                            this.mIsCreating = true;
                            didSurface = true;
                            onSurfaceCreated(this.mSurfaceHolder);
                            callbacks = this.mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (Callback c : callbacks) {
                                    c.surfaceCreated(this.mSurfaceHolder);
                                }
                            }
                        }
                        int i = (creating || (relayoutResult & 2) != 0) ? 1 : 0;
                        redrawNeeded |= i;
                        if (forceReport || creating || surfaceCreating || formatChanged || sizeChanged) {
                            didSurface = true;
                            onSurfaceChanged(this.mSurfaceHolder, this.mFormat, this.mCurWidth, this.mCurHeight);
                            callbacks = this.mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (Callback c2 : callbacks) {
                                    c2.surfaceChanged(this.mSurfaceHolder, this.mFormat, this.mCurWidth, this.mCurHeight);
                                }
                            }
                        }
                        if (insetsChanged) {
                            this.mDispatchedOverscanInsets.set(this.mOverscanInsets);
                            rect = this.mDispatchedOverscanInsets;
                            rect.left += this.mOutsets.left;
                            rect = this.mDispatchedOverscanInsets;
                            rect.top += this.mOutsets.top;
                            rect = this.mDispatchedOverscanInsets;
                            rect.right += this.mOutsets.right;
                            rect = this.mDispatchedOverscanInsets;
                            rect.bottom += this.mOutsets.bottom;
                            this.mDispatchedContentInsets.set(this.mContentInsets);
                            this.mDispatchedStableInsets.set(this.mStableInsets);
                            this.mDispatchedOutsets.set(this.mOutsets);
                            this.mFinalSystemInsets.set(this.mDispatchedOverscanInsets);
                            this.mFinalStableInsets.set(this.mDispatchedStableInsets);
                            onApplyWindowInsets(new WindowInsets(this.mFinalSystemInsets, null, this.mFinalStableInsets, WallpaperService.this.getResources().getConfiguration().isScreenRound(), false));
                        }
                        if (redrawNeeded) {
                            onSurfaceRedrawNeeded(this.mSurfaceHolder);
                            callbacks = this.mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (Callback c22 : callbacks) {
                                    if (c22 instanceof Callback2) {
                                        ((Callback2) c22).surfaceRedrawNeeded(this.mSurfaceHolder);
                                    }
                                }
                            }
                        }
                        if (didSurface && (this.mReportedVisible ^ 1) != 0) {
                            if (this.mIsCreating) {
                                onVisibilityChanged(true);
                            }
                            onVisibilityChanged(false);
                        }
                        this.mIsCreating = false;
                        this.mSurfaceCreated = true;
                        if (redrawNeeded) {
                            this.mSession.finishDrawing(this.mWindow);
                        }
                        this.mIWallpaperEngine.reportShown();
                    } else {
                        reportSurfaceDestroyed();
                    }
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    this.mIsCreating = false;
                    this.mSurfaceCreated = true;
                    if (redrawNeeded) {
                        this.mSession.finishDrawing(this.mWindow);
                    }
                    this.mIWallpaperEngine.reportShown();
                }
            }
        }

        void attach(IWallpaperEngineWrapper wrapper) {
            if (!this.mDestroyed) {
                this.mIWallpaperEngine = wrapper;
                this.mCaller = wrapper.mCaller;
                this.mConnection = wrapper.mConnection;
                this.mWindowToken = wrapper.mWindowToken;
                this.mSurfaceHolder.setSizeFromLayout();
                this.mInitializing = true;
                this.mSession = WindowManagerGlobal.getWindowSession();
                this.mWindow.setSession(this.mSession);
                this.mLayout.packageName = WallpaperService.this.getPackageName();
                this.mDisplayManager = (DisplayManager) WallpaperService.this.getSystemService("display");
                this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mCaller.getHandler());
                this.mDisplay = this.mDisplayManager.getDisplay(0);
                this.mDisplayState = this.mDisplay.getState();
                onCreate(this.mSurfaceHolder);
                this.mInitializing = false;
                this.mReportedVisible = false;
                updateSurface(false, false, false);
            }
        }

        void doDesiredSizeChanged(int desiredWidth, int desiredHeight) {
            if (!this.mDestroyed) {
                this.mIWallpaperEngine.mReqWidth = desiredWidth;
                this.mIWallpaperEngine.mReqHeight = desiredHeight;
                onDesiredSizeChanged(desiredWidth, desiredHeight);
                doOffsetsChanged(true);
            }
        }

        void doDisplayPaddingChanged(Rect padding) {
            if (!this.mDestroyed && !this.mIWallpaperEngine.mDisplayPadding.equals(padding)) {
                this.mIWallpaperEngine.mDisplayPadding.set(padding);
                updateSurface(true, false, false);
            }
        }

        void doVisibilityChanged(boolean visible) {
            if (!this.mDestroyed) {
                this.mVisible = visible;
                reportVisibility();
            }
        }

        void reportVisibility() {
            if (!this.mDestroyed) {
                this.mDisplayState = this.mDisplay == null ? 0 : this.mDisplay.getState();
                boolean visible = (!this.mVisible || this.mDisplayState == 1 || this.mDisplayState == 3 || this.mDisplayState == 4) ? false : true;
                if (this.mReportedVisible != visible) {
                    this.mReportedVisible = visible;
                    if (visible) {
                        doOffsetsChanged(false);
                        updateSurface(false, false, false);
                    }
                    onVisibilityChanged(visible);
                }
            }
        }

        void doOffsetsChanged(boolean always) {
            if (!this.mDestroyed) {
                if (always || (this.mOffsetsChanged ^ 1) == 0) {
                    float xOffset;
                    float yOffset;
                    float xOffsetStep;
                    float yOffsetStep;
                    boolean sync;
                    synchronized (this.mLock) {
                        xOffset = this.mPendingXOffset;
                        yOffset = this.mPendingYOffset;
                        xOffsetStep = this.mPendingXOffsetStep;
                        yOffsetStep = this.mPendingYOffsetStep;
                        sync = this.mPendingSync;
                        this.mPendingSync = false;
                        this.mOffsetMessageEnqueued = false;
                    }
                    if (this.mSurfaceCreated) {
                        if (this.mReportedVisible) {
                            int availw = this.mIWallpaperEngine.mReqWidth - this.mCurWidth;
                            int availh = this.mIWallpaperEngine.mReqHeight - this.mCurHeight;
                            onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, availw > 0 ? -((int) ((((float) availw) * xOffset) + 0.5f)) : 0, availh > 0 ? -((int) ((((float) availh) * yOffset) + 0.5f)) : 0);
                        } else {
                            this.mOffsetsChanged = true;
                        }
                    }
                    if (sync) {
                        try {
                            this.mSession.wallpaperOffsetsComplete(this.mWindow.asBinder());
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
        }

        void doCommand(WallpaperCommand cmd) {
            Bundle result;
            if (this.mDestroyed) {
                result = null;
            } else {
                result = onCommand(cmd.action, cmd.x, cmd.y, cmd.z, cmd.extras, cmd.sync);
            }
            if (cmd.sync) {
                try {
                    this.mSession.wallpaperCommandComplete(this.mWindow.asBinder(), result);
                } catch (RemoteException e) {
                }
            }
        }

        void reportSurfaceDestroyed() {
            int i = 0;
            if (this.mSurfaceCreated) {
                this.mSurfaceCreated = false;
                this.mSurfaceHolder.ungetCallbacks();
                Callback[] callbacks = this.mSurfaceHolder.getCallbacks();
                if (callbacks != null) {
                    int length = callbacks.length;
                    while (i < length) {
                        callbacks[i].surfaceDestroyed(this.mSurfaceHolder);
                        i++;
                    }
                }
                onSurfaceDestroyed(this.mSurfaceHolder);
            }
        }

        void detach() {
            if (!this.mDestroyed) {
                this.mDestroyed = true;
                if (this.mDisplayManager != null) {
                    this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
                }
                if (this.mVisible) {
                    this.mVisible = false;
                    onVisibilityChanged(false);
                }
                reportSurfaceDestroyed();
                onDestroy();
                if (this.mCreated) {
                    try {
                        if (this.mInputEventReceiver != null) {
                            this.mInputEventReceiver.dispose();
                            this.mInputEventReceiver = null;
                        }
                        this.mSession.remove(this.mWindow);
                    } catch (RemoteException e) {
                    }
                    this.mSurfaceHolder.mSurface.release();
                    this.mCreated = false;
                    if (this.mInputChannel != null) {
                        this.mInputChannel.dispose();
                        this.mInputChannel = null;
                    }
                }
            }
        }
    }

    class IWallpaperEngineWrapper extends Stub implements HandlerCaller.Callback {
        private final HandlerCaller mCaller;
        final IWallpaperConnection mConnection;
        final Rect mDisplayPadding = new Rect();
        Engine mEngine;
        final boolean mIsPreview;
        int mReqHeight;
        int mReqWidth;
        boolean mShownReported;
        final IBinder mWindowToken;
        final int mWindowType;

        IWallpaperEngineWrapper(WallpaperService context, IWallpaperConnection conn, IBinder windowToken, int windowType, boolean isPreview, int reqWidth, int reqHeight, Rect padding) {
            this.mCaller = new HandlerCaller(context, context.getMainLooper(), this, true);
            this.mConnection = conn;
            this.mWindowToken = windowToken;
            this.mWindowType = windowType;
            this.mIsPreview = isPreview;
            this.mReqWidth = reqWidth;
            this.mReqHeight = reqHeight;
            this.mDisplayPadding.set(padding);
            this.mCaller.sendMessage(this.mCaller.obtainMessage(10));
        }

        public void setDesiredSize(int width, int height) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageII(30, width, height));
        }

        public void setDisplayPadding(Rect padding) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageO(40, padding));
        }

        public void setVisibility(boolean visible) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageI(10010, visible ? 1 : 0));
        }

        public void dispatchPointer(MotionEvent event) {
            if (this.mEngine != null) {
                this.mEngine.dispatchPointer(event);
            } else {
                event.recycle();
            }
        }

        public void dispatchWallpaperCommand(String action, int x, int y, int z, Bundle extras) {
            if (this.mEngine != null) {
                this.mEngine.mWindow.dispatchWallpaperCommand(action, x, y, z, extras, false);
            }
        }

        public void reportShown() {
            if (!this.mShownReported) {
                this.mShownReported = true;
                try {
                    this.mConnection.engineShown(this);
                } catch (RemoteException e) {
                    Log.w(WallpaperService.TAG, "Wallpaper host disappeared", e);
                }
            }
        }

        public void requestWallpaperColors() {
            this.mCaller.sendMessage(this.mCaller.obtainMessage(10050));
        }

        public void destroy() {
            this.mCaller.sendMessage(this.mCaller.obtainMessage(20));
        }

        public void executeMessage(Message message) {
            switch (message.what) {
                case 10:
                    try {
                        this.mConnection.attachEngine(this);
                        Engine engine = WallpaperService.this.onCreateEngine();
                        this.mEngine = engine;
                        WallpaperService.this.mActiveEngines.add(engine);
                        engine.attach(this);
                        return;
                    } catch (RemoteException e) {
                        Log.w(WallpaperService.TAG, "Wallpaper host disappeared", e);
                        return;
                    }
                case 20:
                    WallpaperService.this.mActiveEngines.remove(this.mEngine);
                    this.mEngine.detach();
                    return;
                case 30:
                    this.mEngine.doDesiredSizeChanged(message.arg1, message.arg2);
                    return;
                case 40:
                    this.mEngine.doDisplayPaddingChanged((Rect) message.obj);
                    break;
                case 10000:
                    break;
                case 10010:
                    this.mEngine.doVisibilityChanged(message.arg1 != 0);
                    break;
                case 10020:
                    this.mEngine.doOffsetsChanged(true);
                    break;
                case 10025:
                    this.mEngine.doCommand(message.obj);
                    break;
                case 10030:
                    boolean reportDraw = message.arg1 != 0;
                    this.mEngine.mOutsets.set((Rect) message.obj);
                    this.mEngine.updateSurface(true, false, reportDraw);
                    this.mEngine.doOffsetsChanged(true);
                    break;
                case 10035:
                    break;
                case 10040:
                    boolean skip = false;
                    MotionEvent ev = message.obj;
                    if (ev.getAction() == 2) {
                        synchronized (this.mEngine.mLock) {
                            if (this.mEngine.mPendingMove == ev) {
                                this.mEngine.mPendingMove = null;
                            } else {
                                skip = true;
                            }
                        }
                    }
                    if (!skip) {
                        this.mEngine.onTouchEvent(ev);
                    }
                    ev.recycle();
                    break;
                case 10050:
                    if (this.mConnection != null) {
                        try {
                            this.mConnection.onWallpaperColorsChanged(this.mEngine.onComputeColors());
                            break;
                        } catch (RemoteException e2) {
                            break;
                        }
                    }
                    break;
                default:
                    Log.w(WallpaperService.TAG, "Unknown message type " + message.what);
                    break;
            }
            this.mEngine.updateSurface(true, false, false);
        }
    }

    class IWallpaperServiceWrapper extends IWallpaperService.Stub {
        private final WallpaperService mTarget;

        public IWallpaperServiceWrapper(WallpaperService context) {
            this.mTarget = context;
        }

        public void attach(IWallpaperConnection conn, IBinder windowToken, int windowType, boolean isPreview, int reqWidth, int reqHeight, Rect padding) {
            IWallpaperEngineWrapper iWallpaperEngineWrapper = new IWallpaperEngineWrapper(this.mTarget, conn, windowToken, windowType, isPreview, reqWidth, reqHeight, padding);
        }
    }

    static final class WallpaperCommand {
        String action;
        Bundle extras;
        boolean sync;
        int x;
        int y;
        int z;

        WallpaperCommand() {
        }
    }

    public abstract Engine onCreateEngine();

    public void onCreate() {
        super.onCreate();
    }

    public void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < this.mActiveEngines.size(); i++) {
            ((Engine) this.mActiveEngines.get(i)).detach();
        }
        this.mActiveEngines.clear();
    }

    public final IBinder onBind(Intent intent) {
        return new IWallpaperServiceWrapper(this);
    }

    protected void dump(FileDescriptor fd, PrintWriter out, String[] args) {
        out.print("State of wallpaper ");
        out.print(this);
        out.println(SettingsStringUtil.DELIMITER);
        for (int i = 0; i < this.mActiveEngines.size(); i++) {
            Engine engine = (Engine) this.mActiveEngines.get(i);
            out.print("  Engine ");
            out.print(engine);
            out.println(SettingsStringUtil.DELIMITER);
            engine.dump("    ", fd, out, args);
        }
    }
}
