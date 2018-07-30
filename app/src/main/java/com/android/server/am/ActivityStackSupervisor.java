package com.android.server.am;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.StackId;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityManagerInternal.SleepToken;
import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.app.WaitResult;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.input.InputManagerInternal;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.BoostFramework;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.view.Display;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.TransferPipe;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.IStatusBarService.Stub;
import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.LocationManagerService;
import com.android.server.coloros.OppoListManager;
import com.android.server.display.OppoBrightUtils;
import com.android.server.face.FaceDaemonWrapper;
import com.android.server.pm.OppoPackageManagerHelper;
import com.android.server.wm.PinnedStackWindowController;
import com.android.server.wm.WindowManagerService;
import com.oppo.hypnus.Hypnus;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ActivityStackSupervisor extends ConfigurationContainer implements DisplayListener {
    private static final ArrayMap<String, String> ACTION_TO_RUNTIME_PERMISSION = new ArrayMap();
    private static final int ACTIVITY_RESTRICTION_APPOP = 2;
    private static final int ACTIVITY_RESTRICTION_NONE = 0;
    private static final int ACTIVITY_RESTRICTION_PERMISSION = 1;
    static final boolean CREATE_IF_NEEDED = true;
    static final boolean DEFER_RESUME = true;
    static final boolean FORCE_FOCUS = true;
    static final int HANDLE_DISPLAY_ADDED = 105;
    static final int HANDLE_DISPLAY_CHANGED = 106;
    static final int HANDLE_DISPLAY_REMOVED = 107;
    static final int IDLE_NOW_MSG = 101;
    static final int IDLE_TIMEOUT = 10000;
    static final int IDLE_TIMEOUT_MSG = 100;
    static final int LAUNCH_TASK_BEHIND_COMPLETE = 112;
    static final int LAUNCH_TIMEOUT = 10000;
    static final int LAUNCH_TIMEOUT_MSG = 104;
    static final int LOCK_TASK_END_MSG = 110;
    static final int LOCK_TASK_START_MSG = 109;
    private static final String LOCK_TASK_TAG = "Lock-to-App";
    static final int MATCH_TASK_IN_STACKS_ONLY = 0;
    static final int MATCH_TASK_IN_STACKS_OR_RECENT_TASKS = 1;
    static final int MATCH_TASK_IN_STACKS_OR_RECENT_TASKS_AND_RESTORE = 2;
    private static final int MAX_TASK_IDS_PER_USER = 100000;
    static final boolean MOVING = true;
    static final boolean ON_TOP = true;
    protected static final String OPPO_SAFECENTER_PASSWORD = "com.coloros.safecenter/.privacy.view.password";
    static final boolean PAUSE_IMMEDIATELY = true;
    static final boolean PRESERVE_WINDOWS = true;
    static final boolean REMOVE_FROM_RECENTS = true;
    static final int REPORT_MULTI_WINDOW_MODE_CHANGED_MSG = 114;
    static final int REPORT_PIP_MODE_CHANGED_MSG = 115;
    static final int RESUME_TOP_ACTIVITY_MSG = 102;
    static final int SHOW_LOCK_TASK_ESCAPE_MESSAGE_MSG = 113;
    static final int SLEEP_TIMEOUT = 5000;
    static final int SLEEP_TIMEOUT_MSG = 103;
    private static final String TAG = "ActivityManager";
    private static final String TAG_FOCUS = (TAG + ActivityManagerDebugConfig.POSTFIX_FOCUS);
    private static final String TAG_IDLE = (TAG + ActivityManagerDebugConfig.POSTFIX_IDLE);
    private static final String TAG_LOCKTASK = (TAG + ActivityManagerDebugConfig.POSTFIX_LOCKTASK);
    private static final String TAG_PAUSE = (TAG + ActivityManagerDebugConfig.POSTFIX_PAUSE);
    private static final String TAG_RECENTS = (TAG + ActivityManagerDebugConfig.POSTFIX_RECENTS);
    private static final String TAG_RELEASE = (TAG + ActivityManagerDebugConfig.POSTFIX_RELEASE);
    private static final String TAG_STACK = (TAG + ActivityManagerDebugConfig.POSTFIX_STACK);
    private static final String TAG_STATES = (TAG + ActivityManagerDebugConfig.POSTFIX_STATES);
    private static final String TAG_SWITCH = (TAG + ActivityManagerDebugConfig.POSTFIX_SWITCH);
    static final String TAG_TASKS = (TAG + ActivityManagerDebugConfig.POSTFIX_TASKS);
    static final boolean VALIDATE_WAKE_LOCK_CALLER = false;
    private static final String VIRTUAL_DISPLAY_BASE_NAME = "ActivityViewVirtualDisplay";
    public static Hypnus mHyp = null;
    final String ACTION_OPPO_SAFE_COUNT_START_URL = "oppo.intent.action.OPPO_SAFE_COUNT_START_URL";
    boolean inResumeTopActivity;
    final ArrayList<ActivityRecord> mActivitiesWaitingForVisibleActivity = new ArrayList();
    private final SparseArray<ActivityDisplay> mActivityDisplays = new SparseArray();
    final ActivityMetricsLogger mActivityMetricsLogger;
    private boolean mAllowDockedStackResize = true;
    boolean mAppVisibilitiesChangedSinceLastPause;
    private final SparseIntArray mCurTaskIdForUser = new SparseIntArray(20);
    int mCurrentUser;
    int mDefaultMinSizeOfResizeableTask = -1;
    private int mDeferResumeCount;
    private IDevicePolicyManager mDevicePolicyManager;
    private final SparseArray<IntArray> mDisplayAccessUIDs = new SparseArray();
    DisplayManager mDisplayManager;
    private DisplayManagerInternal mDisplayManagerInternal;
    final ArrayList<ActivityRecord> mFinishingActivities = new ArrayList();
    ActivityStack mFocusedStack;
    WakeLock mGoingToSleep;
    final ArrayList<ActivityRecord> mGoingToSleepActivities = new ArrayList();
    final ActivityStackSupervisorHandler mHandler;
    ActivityStack mHomeStack;
    private InputManagerInternal mInputManagerInternal;
    boolean mIsDockMinimized;
    final KeyguardController mKeyguardController;
    private ActivityStack mLastFocusedStack;
    WakeLock mLaunchingActivity;
    private int mLockTaskModeState;
    ArrayList<TaskRecord> mLockTaskModeTasks = new ArrayList();
    private LockTaskNotify mLockTaskNotify;
    final ArrayList<ActivityRecord> mMultiWindowModeChangedActivities = new ArrayList();
    private int mNextFreeStackId = 8;
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "ZhiYong.Lin@Plf.Framework, modify for secure protect", property = OppoRomType.ROM)
    OppoSecureProtectUtils mOppoSecureProtectUtils = new OppoSecureProtectUtils();
    public BoostFramework mPerfBoost = null;
    public BoostFramework mPerfPack = null;
    final ArrayList<ActivityRecord> mPipModeChangedActivities = new ArrayList();
    Rect mPipModeChangedTargetStackBounds;
    private PowerManager mPowerManager;
    private RecentTasks mRecentTasks;
    private final ArraySet<Integer> mResizingTasksDuringAnimation = new ArraySet();
    final ActivityManagerService mService;
    final ArrayList<SleepToken> mSleepTokens = new ArrayList();
    SparseArray<ActivityStack> mStacks = new SparseArray();
    final ArrayList<UserState> mStartingUsers = new ArrayList();
    private IStatusBarService mStatusBarService;
    final ArrayList<ActivityRecord> mStoppingActivities = new ArrayList();
    private boolean mTaskLayersChanged = true;
    private final ArrayList<ActivityRecord> mTmpActivityList = new ArrayList();
    private final FindTaskResult mTmpFindTaskResult = new FindTaskResult();
    private SparseIntArray mTmpOrderedDisplayIds = new SparseIntArray();
    private IBinder mToken = new Binder();
    boolean mUserLeaving = false;
    SparseIntArray mUserStackInFront = new SparseIntArray(2);
    final ArrayList<WaitResult> mWaitingActivityLaunched = new ArrayList();
    private final ArrayList<WaitInfo> mWaitingForActivityVisible = new ArrayList();
    WindowManagerService mWindowManager;
    private final Rect tempRect = new Rect();

    private final class ActivityStackSupervisorHandler extends Handler {
        public ActivityStackSupervisorHandler(Looper looper) {
            super(looper);
        }

        void activityIdleInternal(ActivityRecord r, boolean processPausingActivities) {
            IBinder iBinder = null;
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityStackSupervisor activityStackSupervisor = ActivityStackSupervisor.this;
                    if (r != null) {
                        iBinder = r.appToken;
                    }
                    activityStackSupervisor.activityIdleInternalLocked(iBinder, true, processPausingActivities, null);
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void handleMessage(Message msg) {
            int i;
            switch (msg.what) {
                case 100:
                    if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                        Slog.d(ActivityStackSupervisor.TAG_IDLE, "handleMessage: IDLE_TIMEOUT_MSG: r=" + msg.obj);
                    }
                    activityIdleInternal((ActivityRecord) msg.obj, true);
                    return;
                case 101:
                    if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                        Slog.d(ActivityStackSupervisor.TAG_IDLE, "handleMessage: IDLE_NOW_MSG: r=" + msg.obj);
                    }
                    activityIdleInternal((ActivityRecord) msg.obj, false);
                    return;
                case 102:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStackSupervisor.this.resumeFocusedStackTopActivityLocked();
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    return;
                case 103:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (ActivityStackSupervisor.this.mService.isSleepingOrShuttingDownLocked()) {
                                Slog.w(ActivityStackSupervisor.TAG, "Sleep timeout!  Sleeping now.");
                                ActivityStackSupervisor.this.checkReadyForSleepLocked(false);
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    return;
                case 104:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (ActivityStackSupervisor.this.mLaunchingActivity.isHeld()) {
                                Slog.w(ActivityStackSupervisor.TAG, "Launch timeout has expired, giving up wake lock!");
                                ActivityStackSupervisor.this.mLaunchingActivity.release();
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    return;
                case 105:
                    ActivityStackSupervisor.this.handleDisplayAdded(msg.arg1);
                    return;
                case 106:
                    ActivityStackSupervisor.this.handleDisplayChanged(msg.arg1);
                    return;
                case 107:
                    ActivityStackSupervisor.this.handleDisplayRemoved(msg.arg1);
                    return;
                case 109:
                    try {
                        if (ActivityStackSupervisor.this.mLockTaskNotify == null) {
                            ActivityStackSupervisor.this.mLockTaskNotify = new LockTaskNotify(ActivityStackSupervisor.this.mService.mContext);
                        }
                        ActivityStackSupervisor.this.mLockTaskNotify.show(true);
                        ActivityStackSupervisor.this.mLockTaskModeState = msg.arg2;
                        if (ActivityStackSupervisor.this.getStatusBarService() != null) {
                            int flags = 0;
                            if (ActivityStackSupervisor.this.mLockTaskModeState == 1) {
                                flags = 62849024;
                            } else if (ActivityStackSupervisor.this.mLockTaskModeState == 2) {
                                flags = 43974656;
                            }
                            ActivityStackSupervisor.this.getStatusBarService().disable(flags ^ DumpState.DUMP_VOLUMES, ActivityStackSupervisor.this.mToken, ActivityStackSupervisor.this.mService.mContext.getPackageName());
                        }
                        ActivityStackSupervisor.this.mWindowManager.disableKeyguard(ActivityStackSupervisor.this.mToken, ActivityStackSupervisor.LOCK_TASK_TAG);
                        if (ActivityStackSupervisor.this.getDevicePolicyManager() != null) {
                            ActivityStackSupervisor.this.getDevicePolicyManager().notifyLockTaskModeChanged(true, (String) msg.obj, msg.arg1);
                            return;
                        }
                        return;
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                case 110:
                    try {
                        if (ActivityStackSupervisor.this.getStatusBarService() != null) {
                            ActivityStackSupervisor.this.getStatusBarService().disable(0, ActivityStackSupervisor.this.mToken, ActivityStackSupervisor.this.mService.mContext.getPackageName());
                        }
                        ActivityStackSupervisor.this.mWindowManager.reenableKeyguard(ActivityStackSupervisor.this.mToken);
                        if (ActivityStackSupervisor.this.getDevicePolicyManager() != null) {
                            ActivityStackSupervisor.this.getDevicePolicyManager().notifyLockTaskModeChanged(false, null, msg.arg1);
                        }
                        if (ActivityStackSupervisor.this.mLockTaskNotify == null) {
                            ActivityStackSupervisor.this.mLockTaskNotify = new LockTaskNotify(ActivityStackSupervisor.this.mService.mContext);
                        }
                        ActivityStackSupervisor.this.mLockTaskNotify.show(false);
                        try {
                            boolean shouldLockKeyguard = Secure.getIntForUser(ActivityStackSupervisor.this.mService.mContext.getContentResolver(), "lock_to_app_exit_locked", -2) != 0;
                            if (ActivityStackSupervisor.this.mLockTaskModeState == 2 && shouldLockKeyguard) {
                                ActivityStackSupervisor.this.mWindowManager.lockNow(null);
                                ActivityStackSupervisor.this.mWindowManager.dismissKeyguard(null);
                                new LockPatternUtils(ActivityStackSupervisor.this.mService.mContext).requireCredentialEntry(-1);
                            }
                        } catch (SettingNotFoundException e) {
                        }
                        ActivityStackSupervisor.this.mLockTaskModeState = 0;
                        return;
                    } catch (RemoteException ex2) {
                        throw new RuntimeException(ex2);
                    } catch (Throwable th) {
                        ActivityStackSupervisor.this.mLockTaskModeState = 0;
                    }
                case 112:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityRecord r = ActivityRecord.forTokenLocked((IBinder) msg.obj);
                            if (r != null) {
                                ActivityStackSupervisor.this.handleLaunchTaskBehindCompleteLocked(r);
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    return;
                case 113:
                    if (ActivityStackSupervisor.this.mLockTaskNotify == null) {
                        ActivityStackSupervisor.this.mLockTaskNotify = new LockTaskNotify(ActivityStackSupervisor.this.mService.mContext);
                    }
                    ActivityStackSupervisor.this.mLockTaskNotify.showToast(2);
                    return;
                case 114:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (i = ActivityStackSupervisor.this.mMultiWindowModeChangedActivities.size() - 1; i >= 0; i--) {
                                ((ActivityRecord) ActivityStackSupervisor.this.mMultiWindowModeChangedActivities.remove(i)).updateMultiWindowMode();
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    return;
                case 115:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (i = ActivityStackSupervisor.this.mPipModeChangedActivities.size() - 1; i >= 0; i--) {
                                ((ActivityRecord) ActivityStackSupervisor.this.mPipModeChangedActivities.remove(i)).updatePictureInPictureMode(ActivityStackSupervisor.this.mPipModeChangedTargetStackBounds, false);
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    static class FindTaskResult {
        boolean matchedByRootAffinity;
        ActivityRecord r;

        FindTaskResult() {
        }
    }

    class ActivityDisplay extends ConfigurationContainer {
        final ArrayList<SleepTokenImpl> mAllSleepTokens = new ArrayList();
        Display mDisplay;
        private IntArray mDisplayAccessUIDs = new IntArray();
        int mDisplayId;
        SleepToken mOffToken;
        private boolean mSleeping;
        final ArrayList<ActivityStack> mStacks = new ArrayList();

        ActivityDisplay() {
            ActivityStackSupervisor.this.mActivityDisplays.put(this.mDisplayId, this);
        }

        ActivityDisplay(int displayId) {
            Display display = ActivityStackSupervisor.this.mDisplayManager.getDisplay(displayId);
            if (display != null) {
                init(display);
            }
        }

        void init(Display display) {
            this.mDisplay = display;
            this.mDisplayId = display.getDisplayId();
        }

        void attachStack(ActivityStack stack, int position) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.v(ActivityStackSupervisor.TAG_STACK, "attachStack: attaching " + stack + " to displayId=" + this.mDisplayId + " position=" + position);
            }
            this.mStacks.add(position, stack);
            ActivityStackSupervisor.this.mService.updateSleepIfNeededLocked();
        }

        void detachStack(ActivityStack stack) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.v(ActivityStackSupervisor.TAG_STACK, "detachStack: detaching " + stack + " from displayId=" + this.mDisplayId);
            }
            this.mStacks.remove(stack);
            if (stack != null && stack.mStackId == 2) {
                OppoFreeFormManagerService.getInstance().resetParentInfo();
            }
            ActivityStackSupervisor.this.mService.updateSleepIfNeededLocked();
        }

        public String toString() {
            return "ActivityDisplay={" + this.mDisplayId + " numStacks=" + this.mStacks.size() + "}";
        }

        protected int getChildCount() {
            return this.mStacks.size();
        }

        protected ConfigurationContainer getChildAt(int index) {
            return (ConfigurationContainer) this.mStacks.get(index);
        }

        protected ConfigurationContainer getParent() {
            return ActivityStackSupervisor.this;
        }

        boolean isPrivate() {
            return (this.mDisplay.getFlags() & 4) != 0;
        }

        boolean isUidPresent(int uid) {
            for (ActivityStack stack : this.mStacks) {
                if (stack.isUidPresent(uid)) {
                    return true;
                }
            }
            return false;
        }

        private IntArray getPresentUIDs() {
            this.mDisplayAccessUIDs.clear();
            for (ActivityStack stack : this.mStacks) {
                stack.getPresentUIDs(this.mDisplayAccessUIDs);
            }
            return this.mDisplayAccessUIDs;
        }

        boolean shouldDestroyContentOnRemove() {
            return this.mDisplay.getRemoveMode() == 1;
        }

        boolean shouldSleep() {
            if ((this.mStacks.isEmpty() || (this.mAllSleepTokens.isEmpty() ^ 1) != 0) && ActivityStackSupervisor.this.mService.mRunningVoice == null) {
                return true;
            }
            return false;
        }

        boolean isSleeping() {
            return this.mSleeping;
        }

        void setIsSleeping(boolean asleep) {
            this.mSleeping = asleep;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface AnyTaskForIdMatchTaskMode {
    }

    static class PendingActivityLaunch {
        final ProcessRecord callerApp;
        final ActivityRecord r;
        final ActivityRecord sourceRecord;
        final ActivityStack stack;
        final int startFlags;

        PendingActivityLaunch(ActivityRecord _r, ActivityRecord _sourceRecord, int _startFlags, ActivityStack _stack, ProcessRecord _callerApp) {
            this.r = _r;
            this.sourceRecord = _sourceRecord;
            this.startFlags = _startFlags;
            this.stack = _stack;
            this.callerApp = _callerApp;
        }

        void sendErrorResult(String message) {
            try {
                if (this.callerApp.thread != null) {
                    this.callerApp.thread.scheduleCrash(message);
                }
            } catch (RemoteException e) {
                Slog.e(ActivityStackSupervisor.TAG, "Exception scheduling crash of failed activity launcher sourceRecord=" + this.sourceRecord, e);
            }
        }
    }

    private final class SleepTokenImpl extends SleepToken {
        private final long mAcquireTime = SystemClock.uptimeMillis();
        private final int mDisplayId;
        private final String mTag;

        public SleepTokenImpl(String tag, int displayId) {
            this.mTag = tag;
            this.mDisplayId = displayId;
        }

        public void release() {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityStackSupervisor.this.removeSleepTokenLocked(this);
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public String toString() {
            return "{\"" + this.mTag + "\", display " + this.mDisplayId + ", acquire at " + TimeUtils.formatUptime(this.mAcquireTime) + "}";
        }
    }

    static class WaitInfo {
        private final WaitResult mResult;
        private final ComponentName mTargetComponent;

        public WaitInfo(ComponentName targetComponent, WaitResult result) {
            this.mTargetComponent = targetComponent;
            this.mResult = result;
        }

        public boolean matches(ComponentName targetComponent) {
            return this.mTargetComponent != null ? this.mTargetComponent.equals(targetComponent) : true;
        }

        public WaitResult getResult() {
            return this.mResult;
        }

        public ComponentName getComponent() {
            return this.mTargetComponent;
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.println(prefix + "WaitInfo:");
            pw.println(prefix + "  mTargetComponent=" + this.mTargetComponent);
            pw.println(prefix + "  mResult=");
            this.mResult.dump(pw, prefix);
        }
    }

    static {
        ACTION_TO_RUNTIME_PERMISSION.put("android.media.action.IMAGE_CAPTURE", OppoPermissionConstants.PERMISSION_CAMERA);
        ACTION_TO_RUNTIME_PERMISSION.put("android.media.action.VIDEO_CAPTURE", OppoPermissionConstants.PERMISSION_CAMERA);
        ACTION_TO_RUNTIME_PERMISSION.put("android.intent.action.CALL", OppoPermissionConstants.PERMISSION_CALL_PHONE);
    }

    protected int getChildCount() {
        return this.mActivityDisplays.size();
    }

    protected ActivityDisplay getChildAt(int index) {
        return (ActivityDisplay) this.mActivityDisplays.valueAt(index);
    }

    protected ConfigurationContainer getParent() {
        return null;
    }

    Configuration getDisplayOverrideConfiguration(int displayId) {
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (activityDisplay != null) {
            return activityDisplay.getOverrideConfiguration();
        }
        throw new IllegalArgumentException("No display found with id: " + displayId);
    }

    void setDisplayOverrideConfiguration(Configuration overrideConfiguration, int displayId) {
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (activityDisplay == null) {
            throw new IllegalArgumentException("No display found with id: " + displayId);
        }
        activityDisplay.onOverrideConfigurationChanged(overrideConfiguration);
    }

    boolean canPlaceEntityOnDisplay(int displayId, boolean resizeable, int callingPid, int callingUid, ActivityInfo activityInfo) {
        if (displayId == 0) {
            return true;
        }
        if (this.mService.mSupportsMultiDisplay) {
            return (resizeable || (displayConfigMatchesGlobal(displayId) ^ 1) == 0) && isCallerAllowedToLaunchOnDisplay(callingPid, callingUid, displayId, activityInfo);
        } else {
            return false;
        }
    }

    private boolean displayConfigMatchesGlobal(int displayId) {
        if (displayId == 0) {
            return true;
        }
        if (displayId == -1) {
            return false;
        }
        ActivityDisplay targetDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (targetDisplay != null) {
            return getConfiguration().equals(targetDisplay.getConfiguration());
        }
        throw new IllegalArgumentException("No display found with id: " + displayId);
    }

    public ActivityStackSupervisor(ActivityManagerService service, Looper looper) {
        this.mService = service;
        this.mHandler = new ActivityStackSupervisorHandler(looper);
        this.mActivityMetricsLogger = new ActivityMetricsLogger(this, this.mService.mContext);
        this.mKeyguardController = new KeyguardController(service, this);
    }

    void setRecentTasks(RecentTasks recentTasks) {
        this.mRecentTasks = recentTasks;
    }

    void initPowerManagement() {
        this.mPowerManager = (PowerManager) this.mService.mContext.getSystemService("power");
        this.mGoingToSleep = this.mPowerManager.newWakeLock(1, "ActivityManager-Sleep");
        this.mLaunchingActivity = this.mPowerManager.newWakeLock(1, "*launch*");
        this.mLaunchingActivity.setReferenceCounted(false);
    }

    private IStatusBarService getStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mStatusBarService == null) {
                    this.mStatusBarService = Stub.asInterface(ServiceManager.checkService("statusbar"));
                    if (this.mStatusBarService == null) {
                        Slog.w("StatusBarManager", "warning: no STATUS_BAR_SERVICE");
                    }
                }
                iStatusBarService = this.mStatusBarService;
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return iStatusBarService;
    }

    private IDevicePolicyManager getDevicePolicyManager() {
        IDevicePolicyManager iDevicePolicyManager;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mDevicePolicyManager == null) {
                    this.mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.checkService("device_policy"));
                    if (this.mDevicePolicyManager == null) {
                        Slog.w(TAG, "warning: no DEVICE_POLICY_SERVICE");
                    }
                }
                iDevicePolicyManager = this.mDevicePolicyManager;
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return iDevicePolicyManager;
    }

    void setWindowManager(WindowManagerService wm) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mWindowManager = wm;
                this.mKeyguardController.setWindowManager(wm);
                this.mDisplayManager = (DisplayManager) this.mService.mContext.getSystemService("display");
                this.mDisplayManager.registerDisplayListener(this, null);
                this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
                Display[] displays = this.mDisplayManager.getDisplays();
                for (int displayNdx = displays.length - 1; displayNdx >= 0; displayNdx--) {
                    int displayId = displays[displayNdx].getDisplayId();
                    ActivityDisplay activityDisplay = new ActivityDisplay(displayId);
                    if (activityDisplay.mDisplay == null) {
                        throw new IllegalStateException("Default Display does not exist");
                    }
                    this.mActivityDisplays.put(displayId, activityDisplay);
                    calculateDefaultMinimalSizeOfResizeableTasks(activityDisplay);
                }
                ActivityStack stack = getStack(0, true, true);
                this.mLastFocusedStack = stack;
                this.mFocusedStack = stack;
                this.mHomeStack = stack;
                this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    ActivityStack getFocusedStack() {
        return this.mFocusedStack;
    }

    ActivityStack getLastStack() {
        return this.mLastFocusedStack;
    }

    boolean isFocusedStack(ActivityStack stack) {
        return stack != null && stack == this.mFocusedStack;
    }

    boolean isFrontStackOnDisplay(ActivityStack stack) {
        return isFrontOfStackList(stack, stack.getDisplay().mStacks);
    }

    private boolean isFrontOfStackList(ActivityStack stack, List<ActivityStack> stackList) {
        return stack == stackList.get(stackList.size() + -1);
    }

    void setFocusStackUnchecked(String reason, ActivityStack focusCandidate) {
        int i = -1;
        if (!focusCandidate.isFocusable()) {
            focusCandidate = getNextFocusableStackLocked(focusCandidate);
        }
        if (focusCandidate != this.mFocusedStack) {
            this.mLastFocusedStack = this.mFocusedStack;
            this.mFocusedStack = focusCandidate;
            int i2 = this.mCurrentUser;
            int stackId = this.mFocusedStack == null ? -1 : this.mFocusedStack.getStackId();
            if (this.mLastFocusedStack != null) {
                i = this.mLastFocusedStack.getStackId();
            }
            EventLogTags.writeAmFocusedStack(i2, stackId, i, reason);
        }
        ActivityRecord r = topRunningActivityLocked();
        if ((this.mService.mBooting || (this.mService.mBooted ^ 1) != 0) && r != null && r.idle) {
            checkFinishBootingLocked();
        }
    }

    void moveHomeStackToFront(String reason) {
        this.mHomeStack.moveToFront(reason);
    }

    void moveRecentsStackToFront(String reason) {
        ActivityStack recentsStack = getStack(5);
        if (recentsStack != null) {
            recentsStack.moveToFront(reason);
        }
    }

    boolean moveHomeStackTaskToTop(String reason) {
        this.mHomeStack.moveHomeStackTaskToTop();
        ActivityRecord top = getHomeActivity();
        if (top == null) {
            return false;
        }
        moveFocusableActivityStackToFrontLocked(top, reason);
        return true;
    }

    boolean resumeHomeStackTask(ActivityRecord prev, String reason) {
        if (!this.mService.mBooting && (this.mService.mBooted ^ 1) != 0) {
            return false;
        }
        if (prev != null) {
            prev.getTask().setTaskToReturnTo(0);
        }
        this.mHomeStack.moveHomeStackTaskToTop();
        ActivityRecord r = getHomeActivity();
        String myReason = reason + " resumeHomeStackTask";
        if (r == null || (r.finishing ^ 1) == 0) {
            return this.mService.startHomeActivityLocked(this.mCurrentUser, myReason);
        }
        moveFocusableActivityStackToFrontLocked(r, myReason);
        return resumeFocusedStackTopActivityLocked(this.mHomeStack, prev, null);
    }

    TaskRecord anyTaskForIdLocked(int id) {
        return anyTaskForIdLocked(id, 2, -1);
    }

    TaskRecord anyTaskForIdLocked(int id, int matchMode, int stackId) {
        if (matchMode == 2 || stackId == -1) {
            TaskRecord task;
            int numDisplays = this.mActivityDisplays.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
                for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                    task = ((ActivityStack) stacks.get(stackNdx)).taskForIdLocked(id);
                    if (task != null) {
                        return task;
                    }
                }
            }
            if (matchMode == 0) {
                return null;
            }
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.v(TAG_RECENTS, "Looking for task id=" + id + " in recents");
            }
            task = this.mRecentTasks.taskForIdLocked(id);
            if (task == null) {
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "\tDidn't find task id=" + id + " in recents");
                }
                return null;
            } else if (matchMode == 1) {
                return task;
            } else {
                if (restoreRecentTaskLocked(task, stackId)) {
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.w(TAG_RECENTS, "Restored task id=" + id + " from in recents");
                    }
                    return task;
                }
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.w(TAG_RECENTS, "Couldn't restore task id=" + id + " found in recents");
                }
                return null;
            }
        }
        throw new IllegalArgumentException("Should not specify stackId for non-restore lookup");
    }

    ActivityRecord isInAnyStackLocked(IBinder token) {
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord r = ((ActivityStack) stacks.get(stackNdx)).isInStackLocked(token);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    private boolean taskTopActivityIsUser(TaskRecord task, int userId) {
        ActivityRecord activityRecord = task.getTopActivity();
        ActivityRecord resultTo = activityRecord != null ? activityRecord.resultTo : null;
        if (activityRecord != null && activityRecord.userId == userId) {
            return true;
        }
        if (resultTo == null || resultTo.userId != userId) {
            return false;
        }
        return true;
    }

    void lockAllProfileTasks(int userId) {
        this.mWindowManager.deferSurfaceLayout();
        try {
            List<ActivityStack> stacks = getStacks();
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                List<TaskRecord> tasks = ((ActivityStack) stacks.get(stackNdx)).getAllTasks();
                for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                    TaskRecord task = (TaskRecord) tasks.get(taskNdx);
                    if (taskTopActivityIsUser(task, userId)) {
                        this.mService.mTaskChangeNotificationController.notifyTaskProfileLocked(task.taskId, userId);
                    }
                }
            }
        } finally {
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    void setNextTaskIdForUserLocked(int taskId, int userId) {
        if (taskId > this.mCurTaskIdForUser.get(userId, -1)) {
            this.mCurTaskIdForUser.put(userId, taskId);
        }
    }

    static int nextTaskIdForUser(int taskId, int userId) {
        int nextTaskId = taskId + 1;
        if (nextTaskId == (userId + 1) * MAX_TASK_IDS_PER_USER) {
            return nextTaskId - MAX_TASK_IDS_PER_USER;
        }
        return nextTaskId;
    }

    int getNextTaskIdForUserLocked(int userId) {
        int currentTaskId = this.mCurTaskIdForUser.get(userId, MAX_TASK_IDS_PER_USER * userId);
        int candidateTaskId = nextTaskIdForUser(currentTaskId, userId);
        do {
            if (this.mRecentTasks.taskIdTakenForUserLocked(candidateTaskId, userId) || anyTaskForIdLocked(candidateTaskId, 1, -1) != null) {
                candidateTaskId = nextTaskIdForUser(candidateTaskId, userId);
            } else {
                this.mCurTaskIdForUser.put(userId, candidateTaskId);
                return candidateTaskId;
            }
        } while (candidateTaskId != currentTaskId);
        throw new IllegalStateException("Cannot get an available task id. Reached limit of 100000 running tasks per user.");
    }

    ActivityRecord getResumedActivityLocked() {
        ActivityStack stack = this.mFocusedStack;
        if (stack == null) {
            return null;
        }
        ActivityRecord resumedActivity = stack.mResumedActivity;
        if (resumedActivity == null || resumedActivity.app == null) {
            resumedActivity = stack.mPausingActivity;
            if (resumedActivity == null || resumedActivity.app == null) {
                resumedActivity = stack.topRunningActivityLocked();
            }
        }
        return resumedActivity;
    }

    boolean attachApplicationLocked(ProcessRecord app) throws RemoteException {
        String processName = app.processName;
        boolean didSomething = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                if (isFocusedStack(stack)) {
                    stack.getAllRunningVisibleActivitiesLocked(this.mTmpActivityList);
                    ActivityRecord top = stack.topRunningActivityLocked();
                    int size = this.mTmpActivityList.size();
                    for (int i = 0; i < size; i++) {
                        ActivityRecord activity = (ActivityRecord) this.mTmpActivityList.get(i);
                        if (activity.app == null && app.uid == activity.info.applicationInfo.uid && processName.equals(activity.processName)) {
                            try {
                                if (realStartActivityLocked(activity, app, top == activity, true)) {
                                    didSomething = true;
                                }
                            } catch (RemoteException e) {
                                Slog.w(TAG, "Exception in new application when starting activity " + top.intent.getComponent().flattenToShortString(), e);
                                throw e;
                            }
                        }
                    }
                    continue;
                }
            }
        }
        if (!didSomething) {
            ensureActivitiesVisibleLocked(null, 0, false);
        }
        return didSomething;
    }

    boolean allResumedActivitiesIdle() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                if (isFocusedStack(stack) && stack.numActivities() != 0) {
                    ActivityRecord resumedActivity = stack.mResumedActivity;
                    if (resumedActivity == null || (resumedActivity.idle ^ 1) != 0) {
                        if (ActivityManagerDebugConfig.DEBUG_STATES) {
                            Slog.d(TAG_STATES, "allResumedActivitiesIdle: stack=" + stack.mStackId + " " + resumedActivity + " not idle");
                        }
                        return false;
                    }
                }
            }
        }
        this.mService.mActivityStarter.sendPowerHintForLaunchEndIfNeeded();
        return true;
    }

    boolean allResumedActivitiesComplete() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                if (isFocusedStack(stack)) {
                    ActivityRecord r = stack.mResumedActivity;
                    if (!(r == null || r.state == ActivityState.RESUMED)) {
                        return false;
                    }
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.d(TAG_STACK, "allResumedActivitiesComplete: mLastFocusedStack changing from=" + this.mLastFocusedStack + " to=" + this.mFocusedStack);
        }
        int lastStackId = -1;
        if (this.mLastFocusedStack != null) {
            lastStackId = this.mLastFocusedStack.mStackId;
        }
        this.mLastFocusedStack = this.mFocusedStack;
        if (!(this.mFocusedStack == null || this.mFocusedStack.mStackId != 2 || lastStackId == 2 || lastStackId == -1)) {
            ActivityStack focusCandidate = getNextFocusableStackLocked(this.mFocusedStack);
            if (focusCandidate != null && focusCandidate.mStackId == 1) {
                ActivityState parentState = OppoFreeFormManagerService.getInstance().getParentState();
                if (!(parentState == null || parentState == ActivityState.RESUMED)) {
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        Slog.d(TAG_STACK, "allResumedActivitiesComplete: focusCandidate =" + focusCandidate);
                    }
                    focusCandidate.moveToFront("updateFullScreenIfNeededLocked");
                }
            }
        }
        return true;
    }

    boolean allResumedActivitiesVisible() {
        boolean foundResumed = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord r = ((ActivityStack) stacks.get(stackNdx)).mResumedActivity;
                if (r != null) {
                    if (!r.nowVisible || this.mActivitiesWaitingForVisibleActivity.contains(r)) {
                        return false;
                    }
                    foundResumed = true;
                }
            }
        }
        return foundResumed;
    }

    boolean pauseBackStacks(boolean userLeaving, ActivityRecord resuming, boolean dontWait) {
        boolean someActivityPaused = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                if (!(isFocusedStack(stack) || stack.mResumedActivity == null)) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.d(TAG_STATES, "pauseBackStacks: stack=" + stack + " mResumedActivity=" + stack.mResumedActivity);
                    }
                    if (resuming != null) {
                        int resumeId = resuming.getStackId();
                        if (stack.mStackId != resumeId) {
                            if (stack.mStackId != -1) {
                                if (stack.mStackId == 1) {
                                    if (resumeId == 2) {
                                    }
                                }
                                if (stack.mStackId == 2 && resumeId == 1) {
                                }
                            }
                        }
                    }
                    someActivityPaused |= stack.startPausingLocked(userLeaving, false, resuming, dontWait, "pause-stack");
                }
            }
        }
        return someActivityPaused;
    }

    boolean allPausedActivitiesComplete() {
        boolean pausing = true;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord r = ((ActivityStack) stacks.get(stackNdx)).mPausingActivity;
                if (!(r == null || r.state == ActivityState.PAUSED || r.state == ActivityState.STOPPED || r.state == ActivityState.STOPPING)) {
                    if (!ActivityManagerDebugConfig.DEBUG_STATES) {
                        return false;
                    }
                    Slog.d(TAG_STATES, "allPausedActivitiesComplete: r=" + r + " state=" + r.state);
                    pausing = false;
                }
            }
        }
        return pausing;
    }

    void cancelInitializingActivities() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ((ActivityStack) stacks.get(stackNdx)).cancelInitializingActivities();
            }
        }
    }

    void waitActivityVisible(ComponentName name, WaitResult result) {
        this.mWaitingForActivityVisible.add(new WaitInfo(name, result));
    }

    void cleanupActivity(ActivityRecord r) {
        this.mFinishingActivities.remove(r);
        this.mActivitiesWaitingForVisibleActivity.remove(r);
        for (int i = this.mWaitingForActivityVisible.size() - 1; i >= 0; i--) {
            if (((WaitInfo) this.mWaitingForActivityVisible.get(i)).matches(r.realActivity)) {
                this.mWaitingForActivityVisible.remove(i);
            }
        }
    }

    void reportActivityVisibleLocked(ActivityRecord r) {
        sendWaitingVisibleReportLocked(r);
    }

    void sendWaitingVisibleReportLocked(ActivityRecord r) {
        boolean changed = false;
        for (int i = this.mWaitingForActivityVisible.size() - 1; i >= 0; i--) {
            WaitInfo w = (WaitInfo) this.mWaitingForActivityVisible.get(i);
            if (w.matches(r.realActivity)) {
                WaitResult result = w.getResult();
                changed = true;
                result.timeout = false;
                result.who = w.getComponent();
                result.totalTime = SystemClock.uptimeMillis() - result.thisTime;
                result.thisTime = result.totalTime;
                this.mWaitingForActivityVisible.remove(w);
            }
        }
        if (changed) {
            this.mService.notifyAll();
        }
    }

    void reportTaskToFrontNoLaunch(ActivityRecord r) {
        boolean changed = false;
        for (int i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
            WaitResult w = (WaitResult) this.mWaitingActivityLaunched.remove(i);
            if (w.who == null) {
                changed = true;
                w.result = 2;
            }
        }
        if (changed) {
            this.mService.notifyAll();
        }
    }

    void reportActivityLaunchedLocked(boolean timeout, ActivityRecord r, long thisTime, long totalTime) {
        boolean changed = false;
        for (int i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
            WaitResult w = (WaitResult) this.mWaitingActivityLaunched.remove(i);
            if (w.who == null) {
                changed = true;
                w.timeout = timeout;
                if (r != null) {
                    w.who = new ComponentName(r.info.packageName, r.info.name);
                }
                w.thisTime = thisTime;
                w.totalTime = totalTime;
            }
        }
        if (changed) {
            this.mService.notifyAll();
        }
    }

    ActivityRecord topRunningActivityLocked() {
        ActivityStack focusedStack = this.mFocusedStack;
        ActivityRecord r = focusedStack.topRunningActivityLocked();
        if (r != null) {
            return r;
        }
        this.mWindowManager.getDisplaysInFocusOrder(this.mTmpOrderedDisplayIds);
        for (int i = this.mTmpOrderedDisplayIds.size() - 1; i >= 0; i--) {
            List<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.get(this.mTmpOrderedDisplayIds.get(i))).mStacks;
            if (stacks != null) {
                for (int j = stacks.size() - 1; j >= 0; j--) {
                    ActivityStack stack = (ActivityStack) stacks.get(j);
                    if (stack != focusedStack && isFrontStackOnDisplay(stack) && stack.isFocusable()) {
                        r = stack.topRunningActivityLocked();
                        if (r != null) {
                            return r;
                        }
                    }
                }
                continue;
            }
        }
        return null;
    }

    void getTasksLocked(int maxNum, List<RunningTaskInfo> list, int callingUid, boolean allowed) {
        int stackNdx;
        ArrayList<RunningTaskInfo> stackTaskList;
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.v(TAG, "getTasksLocked maxNum " + maxNum + " callingUid " + callingUid + " allowed " + allowed);
        }
        ArrayList<ArrayList<RunningTaskInfo>> runningTaskLists = new ArrayList();
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.v(TAG, "stack " + stack);
                }
                stackTaskList = new ArrayList();
                runningTaskLists.add(stackTaskList);
                stack.getTasksLocked(stackTaskList, callingUid, allowed);
            }
        }
        while (maxNum > 0) {
            long mostRecentActiveTime = Long.MIN_VALUE;
            ArrayList selectedStackList = null;
            int numTaskLists = runningTaskLists.size();
            for (stackNdx = 0; stackNdx < numTaskLists; stackNdx++) {
                stackTaskList = (ArrayList) runningTaskLists.get(stackNdx);
                if (!stackTaskList.isEmpty()) {
                    long lastActiveTime = ((RunningTaskInfo) stackTaskList.get(0)).lastActiveTime;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        String str = TAG;
                        String str2 = str;
                        Slog.v(str2, "topActivity " + ((RunningTaskInfo) stackTaskList.get(0)).topActivity + " lastActiveTime " + lastActiveTime);
                    }
                    if (lastActiveTime > mostRecentActiveTime) {
                        mostRecentActiveTime = lastActiveTime;
                        selectedStackList = stackTaskList;
                    }
                }
            }
            if (selectedStackList != null) {
                list.add((RunningTaskInfo) selectedStackList.remove(0));
                maxNum--;
            } else {
                return;
            }
        }
    }

    ActivityInfo resolveActivity(Intent intent, ResolveInfo rInfo, int startFlags, ProfilerInfo profilerInfo) {
        ActivityInfo aInfo = rInfo != null ? rInfo.activityInfo : null;
        if (aInfo != null) {
            intent.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
            if (!aInfo.processName.equals("system")) {
                if ((startFlags & 2) != 0) {
                    this.mService.setDebugApp(aInfo.processName, true, false);
                }
                if ((startFlags & 8) != 0) {
                    this.mService.setNativeDebuggingAppLocked(aInfo.applicationInfo, aInfo.processName);
                }
                if ((startFlags & 4) != 0) {
                    this.mService.setTrackAllocationApp(aInfo.applicationInfo, aInfo.processName);
                }
                if (profilerInfo != null) {
                    this.mService.setProfileApp(aInfo.applicationInfo, aInfo.processName, profilerInfo);
                }
            }
            String intentLaunchToken = intent.getLaunchToken();
            if (aInfo.launchToken == null && intentLaunchToken != null) {
                aInfo.launchToken = intentLaunchToken;
            }
        }
        return aInfo;
    }

    ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId) {
        return resolveIntent(intent, resolvedType, userId, 0);
    }

    ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId, int flags) {
        ResolveInfo resolveIntent;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                resolveIntent = this.mService.getPackageManagerInternalLocked().resolveIntent(intent, resolvedType, (8454144 | flags) | 1024, userId);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return resolveIntent;
    }

    ActivityInfo resolveActivity(Intent intent, String resolvedType, int startFlags, ProfilerInfo profilerInfo, int userId) {
        return resolveActivity(intent, resolveIntent(intent, resolvedType, userId), startFlags, profilerInfo);
    }

    final boolean realStartActivityLocked(com.android.server.am.ActivityRecord r35, com.android.server.am.ProcessRecord r36, boolean r37, boolean r38) throws android.os.RemoteException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r21_1 android.app.ProfilerInfo) in PHI: PHI: (r21_2 android.app.ProfilerInfo) = (r21_0 android.app.ProfilerInfo), (r21_0 android.app.ProfilerInfo), (r21_0 android.app.ProfilerInfo), (r21_0 android.app.ProfilerInfo), (r21_0 android.app.ProfilerInfo), (r21_1 android.app.ProfilerInfo) binds: {(r21_0 android.app.ProfilerInfo)=B:65:0x02d5, (r21_0 android.app.ProfilerInfo)=B:67:0x02e5, (r21_0 android.app.ProfilerInfo)=B:71:0x02f7, (r21_0 android.app.ProfilerInfo)=B:73:0x0309, (r21_0 android.app.ProfilerInfo)=B:75:0x030f, (r21_1 android.app.ProfilerInfo)=B:81:?}
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
        r34 = this;
        r4 = r34.allPausedActivitiesComplete();
        if (r4 != 0) goto L_0x0036;
    L_0x0006:
        r4 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_SWITCH;
        if (r4 != 0) goto L_0x0012;
    L_0x000a:
        r4 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_PAUSE;
        if (r4 != 0) goto L_0x0012;
    L_0x000e:
        r4 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_STATES;
        if (r4 == 0) goto L_0x0034;
    L_0x0012:
        r4 = TAG_PAUSE;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "realStartActivityLocked: Skipping start of r=";
        r5 = r5.append(r6);
        r0 = r35;
        r5 = r5.append(r0);
        r6 = " some activities pausing...";
        r5 = r5.append(r6);
        r5 = r5.toString();
        android.util.Slog.v(r4, r5);
    L_0x0034:
        r4 = 0;
        return r4;
    L_0x0036:
        r33 = r35.getTask();
        r32 = r33.getStack();
        r34.beginDeferResume();
        r4 = 0;
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r1 = r36;	 Catch:{ all -> 0x04bf }
        r0.startFreezingScreenLocked(r1, r4);	 Catch:{ all -> 0x04bf }
        r35.startLaunchTickingLocked();	 Catch:{ all -> 0x04bf }
        r0 = r36;	 Catch:{ all -> 0x04bf }
        r1 = r35;	 Catch:{ all -> 0x04bf }
        r1.app = r0;	 Catch:{ all -> 0x04bf }
        r0 = r34;	 Catch:{ all -> 0x04bf }
        r4 = r0.mKeyguardController;	 Catch:{ all -> 0x04bf }
        r4 = r4.isKeyguardLocked();	 Catch:{ all -> 0x04bf }
        if (r4 == 0) goto L_0x005f;	 Catch:{ all -> 0x04bf }
    L_0x005c:
        r35.notifyUnknownVisibilityLaunched();	 Catch:{ all -> 0x04bf }
    L_0x005f:
        if (r38 == 0) goto L_0x008f;	 Catch:{ all -> 0x04bf }
    L_0x0061:
        r24 = r35.getDisplayId();	 Catch:{ all -> 0x04bf }
        r0 = r34;	 Catch:{ all -> 0x04bf }
        r5 = r0.mWindowManager;	 Catch:{ all -> 0x04bf }
        r0 = r34;	 Catch:{ all -> 0x04bf }
        r1 = r24;	 Catch:{ all -> 0x04bf }
        r6 = r0.getDisplayOverrideConfiguration(r1);	 Catch:{ all -> 0x04bf }
        r4 = r35.mayFreezeScreenLocked(r36);	 Catch:{ all -> 0x04bf }
        if (r4 == 0) goto L_0x01d4;	 Catch:{ all -> 0x04bf }
    L_0x0077:
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4 = r0.appToken;	 Catch:{ all -> 0x04bf }
    L_0x007b:
        r0 = r24;	 Catch:{ all -> 0x04bf }
        r23 = r5.updateOrientationFromAppTokens(r6, r4, r0);	 Catch:{ all -> 0x04bf }
        r0 = r34;	 Catch:{ all -> 0x04bf }
        r4 = r0.mService;	 Catch:{ all -> 0x04bf }
        r5 = 1;	 Catch:{ all -> 0x04bf }
        r0 = r23;	 Catch:{ all -> 0x04bf }
        r1 = r35;	 Catch:{ all -> 0x04bf }
        r2 = r24;	 Catch:{ all -> 0x04bf }
        r4.updateDisplayOverrideConfigurationLocked(r0, r1, r5, r2);	 Catch:{ all -> 0x04bf }
    L_0x008f:
        r4 = r35.getStack();	 Catch:{ all -> 0x04bf }
        r5 = 1;	 Catch:{ all -> 0x04bf }
        r6 = 1;	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4 = r4.checkKeyguardVisibility(r0, r5, r6);	 Catch:{ all -> 0x04bf }
        if (r4 == 0) goto L_0x00a3;	 Catch:{ all -> 0x04bf }
    L_0x009d:
        r4 = 1;	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r0.setVisibility(r4);	 Catch:{ all -> 0x04bf }
    L_0x00a3:
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4 = r0.info;	 Catch:{ all -> 0x04bf }
        r4 = r4.applicationInfo;	 Catch:{ all -> 0x04bf }
        if (r4 == 0) goto L_0x01d7;	 Catch:{ all -> 0x04bf }
    L_0x00ab:
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4 = r0.info;	 Catch:{ all -> 0x04bf }
        r4 = r4.applicationInfo;	 Catch:{ all -> 0x04bf }
        r0 = r4.uid;	 Catch:{ all -> 0x04bf }
        r22 = r0;	 Catch:{ all -> 0x04bf }
    L_0x00b5:
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4 = r0.userId;	 Catch:{ all -> 0x04bf }
        r0 = r36;	 Catch:{ all -> 0x04bf }
        r5 = r0.userId;	 Catch:{ all -> 0x04bf }
        if (r4 != r5) goto L_0x00c9;	 Catch:{ all -> 0x04bf }
    L_0x00bf:
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4 = r0.appInfo;	 Catch:{ all -> 0x04bf }
        r4 = r4.uid;	 Catch:{ all -> 0x04bf }
        r0 = r22;	 Catch:{ all -> 0x04bf }
        if (r4 == r0) goto L_0x011e;	 Catch:{ all -> 0x04bf }
    L_0x00c9:
        r4 = TAG;	 Catch:{ all -> 0x04bf }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x04bf }
        r5.<init>();	 Catch:{ all -> 0x04bf }
        r6 = "User ID for activity changing for ";	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r0);	 Catch:{ all -> 0x04bf }
        r6 = " appInfo.uid=";	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r6 = r0.appInfo;	 Catch:{ all -> 0x04bf }
        r6 = r6.uid;	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r6 = " info.ai.uid=";	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r0 = r22;	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r0);	 Catch:{ all -> 0x04bf }
        r6 = " old=";	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r6 = r0.app;	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r6 = " new=";	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r0 = r36;	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r0);	 Catch:{ all -> 0x04bf }
        r5 = r5.toString();	 Catch:{ all -> 0x04bf }
        android.util.Slog.wtf(r4, r5);	 Catch:{ all -> 0x04bf }
    L_0x011e:
        r4 = 0;	 Catch:{ all -> 0x04bf }
        r0 = r36;	 Catch:{ all -> 0x04bf }
        r0.waitingToKill = r4;	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4 = r0.launchCount;	 Catch:{ all -> 0x04bf }
        r4 = r4 + 1;	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r0.launchCount = r4;	 Catch:{ all -> 0x04bf }
        r4 = android.os.SystemClock.uptimeMillis();	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r0.lastLaunchTime = r4;	 Catch:{ all -> 0x04bf }
        r0 = r36;	 Catch:{ all -> 0x04bf }
        r4 = r0.activities;	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r28 = r4.indexOf(r0);	 Catch:{ all -> 0x04bf }
        if (r28 >= 0) goto L_0x014a;	 Catch:{ all -> 0x04bf }
    L_0x0141:
        r0 = r36;	 Catch:{ all -> 0x04bf }
        r4 = r0.activities;	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4.add(r0);	 Catch:{ all -> 0x04bf }
    L_0x014a:
        r0 = r34;	 Catch:{ all -> 0x04bf }
        r4 = r0.mService;	 Catch:{ all -> 0x04bf }
        r5 = 1;	 Catch:{ all -> 0x04bf }
        r6 = 0;	 Catch:{ all -> 0x04bf }
        r0 = r36;	 Catch:{ all -> 0x04bf }
        r4.updateLruProcessLocked(r0, r5, r6);	 Catch:{ all -> 0x04bf }
        r0 = r34;	 Catch:{ all -> 0x04bf }
        r4 = r0.mService;	 Catch:{ all -> 0x04bf }
        r4.updateOomAdjLocked();	 Catch:{ all -> 0x04bf }
        r0 = r33;	 Catch:{ all -> 0x04bf }
        r4 = r0.mLockTaskAuth;	 Catch:{ all -> 0x04bf }
        r5 = 2;	 Catch:{ all -> 0x04bf }
        if (r4 == r5) goto L_0x016a;	 Catch:{ all -> 0x04bf }
    L_0x0163:
        r0 = r33;	 Catch:{ all -> 0x04bf }
        r4 = r0.mLockTaskAuth;	 Catch:{ all -> 0x04bf }
        r5 = 4;	 Catch:{ all -> 0x04bf }
        if (r4 != r5) goto L_0x0176;	 Catch:{ all -> 0x04bf }
    L_0x016a:
        r4 = "mLockTaskAuth==LAUNCHABLE";	 Catch:{ all -> 0x04bf }
        r5 = 1;	 Catch:{ all -> 0x04bf }
        r6 = 0;	 Catch:{ all -> 0x04bf }
        r0 = r34;	 Catch:{ all -> 0x04bf }
        r1 = r33;	 Catch:{ all -> 0x04bf }
        r0.setLockTaskModeLocked(r1, r5, r4, r6);	 Catch:{ all -> 0x04bf }
    L_0x0176:
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.thread;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 != 0) goto L_0x01db;	 Catch:{ RemoteException -> 0x0182 }
    L_0x017c:
        r4 = new android.os.RemoteException;	 Catch:{ RemoteException -> 0x0182 }
        r4.<init>();	 Catch:{ RemoteException -> 0x0182 }
        throw r4;	 Catch:{ RemoteException -> 0x0182 }
    L_0x0182:
        r25 = move-exception;
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4 = r0.launchFailed;	 Catch:{ all -> 0x04bf }
        if (r4 == 0) goto L_0x04c4;	 Catch:{ all -> 0x04bf }
    L_0x0189:
        r4 = TAG;	 Catch:{ all -> 0x04bf }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x04bf }
        r5.<init>();	 Catch:{ all -> 0x04bf }
        r6 = "Second failure launching ";	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r6 = r0.intent;	 Catch:{ all -> 0x04bf }
        r6 = r6.getComponent();	 Catch:{ all -> 0x04bf }
        r6 = r6.flattenToShortString();	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r6 = ", giving up";	 Catch:{ all -> 0x04bf }
        r5 = r5.append(r6);	 Catch:{ all -> 0x04bf }
        r5 = r5.toString();	 Catch:{ all -> 0x04bf }
        r0 = r25;	 Catch:{ all -> 0x04bf }
        android.util.Slog.e(r4, r5, r0);	 Catch:{ all -> 0x04bf }
        r0 = r34;	 Catch:{ all -> 0x04bf }
        r4 = r0.mService;	 Catch:{ all -> 0x04bf }
        r0 = r36;	 Catch:{ all -> 0x04bf }
        r4.appDiedLocked(r0);	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r5 = r0.appToken;	 Catch:{ all -> 0x04bf }
        r8 = "2nd-crash";	 Catch:{ all -> 0x04bf }
        r6 = 0;	 Catch:{ all -> 0x04bf }
        r7 = 0;	 Catch:{ all -> 0x04bf }
        r9 = 0;	 Catch:{ all -> 0x04bf }
        r4 = r32;	 Catch:{ all -> 0x04bf }
        r4.requestFinishActivityLocked(r5, r6, r7, r8, r9);	 Catch:{ all -> 0x04bf }
        r4 = 0;
        r34.endDeferResume();
        return r4;
    L_0x01d4:
        r4 = 0;
        goto L_0x007b;
    L_0x01d7:
        r22 = -1;
        goto L_0x00b5;
    L_0x01db:
        r17 = 0;
        r18 = 0;
        if (r37 == 0) goto L_0x01ed;
    L_0x01e1:
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r0.results;	 Catch:{ RemoteException -> 0x0182 }
        r17 = r0;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r0.newIntents;	 Catch:{ RemoteException -> 0x0182 }
        r18 = r0;	 Catch:{ RemoteException -> 0x0182 }
    L_0x01ed:
        r4 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_SWITCH;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x0242;	 Catch:{ RemoteException -> 0x0182 }
    L_0x01f1:
        r4 = TAG_SWITCH;	 Catch:{ RemoteException -> 0x0182 }
        r5 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0182 }
        r5.<init>();	 Catch:{ RemoteException -> 0x0182 }
        r6 = "Launching: ";	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r6);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r0);	 Catch:{ RemoteException -> 0x0182 }
        r6 = " icicle=";	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r6);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r6 = r0.icicle;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r6);	 Catch:{ RemoteException -> 0x0182 }
        r6 = " with results=";	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r6);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r17;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r0);	 Catch:{ RemoteException -> 0x0182 }
        r6 = " newIntents=";	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r6);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r18;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r0);	 Catch:{ RemoteException -> 0x0182 }
        r6 = " andResume=";	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r6);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r37;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r0);	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.toString();	 Catch:{ RemoteException -> 0x0182 }
        android.util.Slog.v(r4, r5);	 Catch:{ RemoteException -> 0x0182 }
    L_0x0242:
        r4 = 4;	 Catch:{ RemoteException -> 0x0182 }
        r4 = new java.lang.Object[r4];	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r0.userId;	 Catch:{ RemoteException -> 0x0182 }
        r5 = java.lang.Integer.valueOf(r5);	 Catch:{ RemoteException -> 0x0182 }
        r6 = 0;	 Catch:{ RemoteException -> 0x0182 }
        r4[r6] = r5;	 Catch:{ RemoteException -> 0x0182 }
        r5 = java.lang.System.identityHashCode(r35);	 Catch:{ RemoteException -> 0x0182 }
        r5 = java.lang.Integer.valueOf(r5);	 Catch:{ RemoteException -> 0x0182 }
        r6 = 1;	 Catch:{ RemoteException -> 0x0182 }
        r4[r6] = r5;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r33;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r0.taskId;	 Catch:{ RemoteException -> 0x0182 }
        r5 = java.lang.Integer.valueOf(r5);	 Catch:{ RemoteException -> 0x0182 }
        r6 = 2;	 Catch:{ RemoteException -> 0x0182 }
        r4[r6] = r5;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r0.shortComponentName;	 Catch:{ RemoteException -> 0x0182 }
        r6 = 3;	 Catch:{ RemoteException -> 0x0182 }
        r4[r6] = r5;	 Catch:{ RemoteException -> 0x0182 }
        r5 = 30006; // 0x7536 float:4.2047E-41 double:1.4825E-319;	 Catch:{ RemoteException -> 0x0182 }
        android.util.EventLog.writeEvent(r5, r4);	 Catch:{ RemoteException -> 0x0182 }
        r4 = r35.isHomeActivity();	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x028b;	 Catch:{ RemoteException -> 0x0182 }
    L_0x0278:
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r33;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mActivities;	 Catch:{ RemoteException -> 0x0182 }
        r6 = 0;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.get(r6);	 Catch:{ RemoteException -> 0x0182 }
        r4 = (com.android.server.am.ActivityRecord) r4;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.app;	 Catch:{ RemoteException -> 0x0182 }
        r5.mHomeProcess = r4;	 Catch:{ RemoteException -> 0x0182 }
    L_0x028b:
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r0.intent;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.getComponent();	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.getPackageName();	 Catch:{ RemoteException -> 0x0182 }
        r6 = 0;	 Catch:{ RemoteException -> 0x0182 }
        r4.notifyPackageUse(r5, r6);	 Catch:{ RemoteException -> 0x0182 }
        r4 = 0;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r0.sleeping = r4;	 Catch:{ RemoteException -> 0x0182 }
        r4 = 0;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r0.forceNewConfig = r4;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r4.showUnsupportedZoomDialogIfNeededLocked(r0);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r4.showAskCompatModeDialogLocked(r0);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r0.info;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.applicationInfo;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.compatibilityInfoForPackageLocked(r5);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r0.compat = r4;	 Catch:{ RemoteException -> 0x0182 }
        r21 = 0;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.mProfileApp;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x032c;	 Catch:{ RemoteException -> 0x0182 }
    L_0x02d7:
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.mProfileApp;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r0.processName;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.equals(r5);	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x032c;	 Catch:{ RemoteException -> 0x0182 }
    L_0x02e7:
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.mProfileProc;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x02f9;	 Catch:{ RemoteException -> 0x0182 }
    L_0x02ef:
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.mProfileProc;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 != r0) goto L_0x032c;	 Catch:{ RemoteException -> 0x0182 }
    L_0x02f9:
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r4.mProfileProc = r0;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r4.mProfilerInfo;	 Catch:{ RemoteException -> 0x0182 }
        r31 = r0;	 Catch:{ RemoteException -> 0x0182 }
        if (r31 == 0) goto L_0x032c;	 Catch:{ RemoteException -> 0x0182 }
    L_0x030b:
        r0 = r31;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.profileFile;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x032c;	 Catch:{ RemoteException -> 0x0182 }
    L_0x0311:
        r0 = r31;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.profileFd;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x0323;
    L_0x0317:
        r0 = r31;	 Catch:{ IOException -> 0x04b9 }
        r4 = r0.profileFd;	 Catch:{ IOException -> 0x04b9 }
        r4 = r4.dup();	 Catch:{ IOException -> 0x04b9 }
        r0 = r31;	 Catch:{ IOException -> 0x04b9 }
        r0.profileFd = r4;	 Catch:{ IOException -> 0x04b9 }
    L_0x0323:
        r21 = new android.app.ProfilerInfo;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r21;	 Catch:{ RemoteException -> 0x0182 }
        r1 = r31;	 Catch:{ RemoteException -> 0x0182 }
        r0.<init>(r1);	 Catch:{ RemoteException -> 0x0182 }
    L_0x032c:
        r4 = 1;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r0.hasShownUi = r4;	 Catch:{ RemoteException -> 0x0182 }
        r4 = 1;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r0.pendingUiClean = r4;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.mTopProcessState;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r0.forceProcessStateUpTo(r4);	 Catch:{ RemoteException -> 0x0182 }
        r29 = new android.util.MergedConfiguration;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.getGlobalConfiguration();	 Catch:{ RemoteException -> 0x0182 }
        r5 = r35.getMergedOverrideConfiguration();	 Catch:{ RemoteException -> 0x0182 }
        r0 = r29;	 Catch:{ RemoteException -> 0x0182 }
        r0.<init>(r4, r5);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r1 = r29;	 Catch:{ RemoteException -> 0x0182 }
        r0.setLastReportedConfiguration(r1);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.intent;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x037c;	 Catch:{ RemoteException -> 0x0182 }
    L_0x0361:
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.intent;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.getComponent();	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x037c;	 Catch:{ RemoteException -> 0x0182 }
    L_0x036b:
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.intent;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.getComponent();	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.getClassName();	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r0.startActivityBoost(r4);	 Catch:{ RemoteException -> 0x0182 }
    L_0x037c:
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.intent;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r0.icicle;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r0.logIfTransactionTooLarge(r4, r5);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.thread;	 Catch:{ RemoteException -> 0x0182 }
        r5 = new android.content.Intent;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r6 = r0.intent;	 Catch:{ RemoteException -> 0x0182 }
        r5.<init>(r6);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r6 = r0.appToken;	 Catch:{ RemoteException -> 0x0182 }
        r7 = java.lang.System.identityHashCode(r35);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r8 = r0.info;	 Catch:{ RemoteException -> 0x0182 }
        r9 = r29.getGlobalConfiguration();	 Catch:{ RemoteException -> 0x0182 }
        r10 = r29.getOverrideConfiguration();	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r11 = r0.compat;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r12 = r0.launchedFromPackage;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r33;	 Catch:{ RemoteException -> 0x0182 }
        r13 = r0.voiceInteractor;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r14 = r0.repProcState;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r15 = r0.icicle;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r0.persistentState;	 Catch:{ RemoteException -> 0x0182 }
        r16 = r0;	 Catch:{ RemoteException -> 0x0182 }
        r19 = r37 ^ 1;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r20 = r0;	 Catch:{ RemoteException -> 0x0182 }
        r20 = r20.isNextTransitionForward();	 Catch:{ RemoteException -> 0x0182 }
        r4.scheduleLaunchActivity(r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15, r16, r17, r18, r19, r20, r21);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.info;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.privateFlags;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4 & 2;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x0450;	 Catch:{ RemoteException -> 0x0182 }
    L_0x03dd:
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.processName;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r0.info;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.packageName;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.equals(r5);	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x0450;	 Catch:{ RemoteException -> 0x0182 }
    L_0x03ed:
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.mHeavyWeightProcess;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == 0) goto L_0x042b;	 Catch:{ RemoteException -> 0x0182 }
    L_0x03f5:
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.mHeavyWeightProcess;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        if (r4 == r0) goto L_0x042b;	 Catch:{ RemoteException -> 0x0182 }
    L_0x03ff:
        r4 = TAG;	 Catch:{ RemoteException -> 0x0182 }
        r5 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0182 }
        r5.<init>();	 Catch:{ RemoteException -> 0x0182 }
        r6 = "Starting new heavy weight process ";	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r6);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r0);	 Catch:{ RemoteException -> 0x0182 }
        r6 = " when already running ";	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r6);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r6 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r6 = r6.mHeavyWeightProcess;	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.append(r6);	 Catch:{ RemoteException -> 0x0182 }
        r5 = r5.toString();	 Catch:{ RemoteException -> 0x0182 }
        android.util.Slog.w(r4, r5);	 Catch:{ RemoteException -> 0x0182 }
    L_0x042b:
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r36;	 Catch:{ RemoteException -> 0x0182 }
        r4.mHeavyWeightProcess = r0;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.mHandler;	 Catch:{ RemoteException -> 0x0182 }
        r5 = 24;	 Catch:{ RemoteException -> 0x0182 }
        r30 = r4.obtainMessage(r5);	 Catch:{ RemoteException -> 0x0182 }
        r0 = r35;	 Catch:{ RemoteException -> 0x0182 }
        r1 = r30;	 Catch:{ RemoteException -> 0x0182 }
        r1.obj = r0;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r34;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r0.mService;	 Catch:{ RemoteException -> 0x0182 }
        r4 = r4.mHandler;	 Catch:{ RemoteException -> 0x0182 }
        r0 = r30;	 Catch:{ RemoteException -> 0x0182 }
        r4.sendMessage(r0);	 Catch:{ RemoteException -> 0x0182 }
    L_0x0450:
        r34.endDeferResume();
        r4 = 0;
        r0 = r35;
        r0.launchFailed = r4;
        r0 = r32;
        r1 = r35;
        r4 = r0.updateLRUListLocked(r1);
        if (r4 == 0) goto L_0x0484;
    L_0x0462:
        r4 = TAG;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "Activity ";
        r5 = r5.append(r6);
        r0 = r35;
        r5 = r5.append(r0);
        r6 = " being launched, but already in LRU list";
        r5 = r5.append(r6);
        r5 = r5.toString();
        android.util.Slog.w(r4, r5);
    L_0x0484:
        if (r37 == 0) goto L_0x04d3;
    L_0x0486:
        r4 = r34.readyToResume();
        if (r4 == 0) goto L_0x04d3;
    L_0x048c:
        r0 = r32;
        r1 = r35;
        r0.minimalResumeActivityLocked(r1);
    L_0x0493:
        r0 = r34;
        r1 = r32;
        r4 = r0.isFocusedStack(r1);
        if (r4 == 0) goto L_0x04a4;
    L_0x049d:
        r0 = r34;
        r4 = r0.mService;
        r4.startSetupActivityLocked();
    L_0x04a4:
        r0 = r35;	 Catch:{ NullPointerException -> 0x0500 }
        r4 = r0.app;	 Catch:{ NullPointerException -> 0x0500 }
        if (r4 == 0) goto L_0x04b7;	 Catch:{ NullPointerException -> 0x0500 }
    L_0x04aa:
        r0 = r34;	 Catch:{ NullPointerException -> 0x0500 }
        r4 = r0.mService;	 Catch:{ NullPointerException -> 0x0500 }
        r4 = r4.mServices;	 Catch:{ NullPointerException -> 0x0500 }
        r0 = r35;	 Catch:{ NullPointerException -> 0x0500 }
        r5 = r0.app;	 Catch:{ NullPointerException -> 0x0500 }
        r4.updateServiceConnectionActivitiesLocked(r5);	 Catch:{ NullPointerException -> 0x0500 }
    L_0x04b7:
        r4 = 1;
        return r4;
    L_0x04b9:
        r26 = move-exception;
        r31.closeFd();	 Catch:{ RemoteException -> 0x0182 }
        goto L_0x0323;
    L_0x04bf:
        r4 = move-exception;
        r34.endDeferResume();
        throw r4;
    L_0x04c4:
        r4 = 1;
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r0.launchFailed = r4;	 Catch:{ all -> 0x04bf }
        r0 = r36;	 Catch:{ all -> 0x04bf }
        r4 = r0.activities;	 Catch:{ all -> 0x04bf }
        r0 = r35;	 Catch:{ all -> 0x04bf }
        r4.remove(r0);	 Catch:{ all -> 0x04bf }
        throw r25;	 Catch:{ all -> 0x04bf }
    L_0x04d3:
        r4 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_STATES;
        if (r4 == 0) goto L_0x04f9;
    L_0x04d7:
        r4 = TAG_STATES;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "Moving to PAUSED: ";
        r5 = r5.append(r6);
        r0 = r35;
        r5 = r5.append(r0);
        r6 = " (starting in paused state)";
        r5 = r5.append(r6);
        r5 = r5.toString();
        android.util.Slog.v(r4, r5);
    L_0x04f9:
        r4 = com.android.server.am.ActivityStack.ActivityState.PAUSED;
        r0 = r35;
        r0.state = r4;
        goto L_0x0493;
    L_0x0500:
        r27 = move-exception;
        r4 = TAG;
        r5 = "updateServiceConnectionActivitiesLocked catch NullPointerException";
        r0 = r27;
        android.util.Slog.w(r4, r5, r0);
        goto L_0x04b7;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ActivityStackSupervisor.realStartActivityLocked(com.android.server.am.ActivityRecord, com.android.server.am.ProcessRecord, boolean, boolean):boolean");
    }

    private void logIfTransactionTooLarge(Intent intent, Bundle icicle) {
        int extrasSize = 0;
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                extrasSize = extras.getSize();
            }
        }
        int icicleSize = icicle == null ? 0 : icicle.getSize();
        if (extrasSize + icicleSize > 200000) {
            Slog.e(TAG, "Transaction too large, intent: " + intent + ", extras size: " + extrasSize + ", icicle size: " + icicleSize);
        }
    }

    void startSpecificActivityLocked(ActivityRecord r, boolean andResume, boolean checkConfig) {
        ProcessRecord app = this.mService.getProcessRecordLocked(r.processName, r.info.applicationInfo.uid, true);
        r.getStack().setLaunchTime(r);
        if (!(app == null || app.thread == null)) {
            try {
                if ((r.info.flags & 1) == 0 || ("android".equals(r.info.packageName) ^ 1) != 0) {
                    app.addPackage(r.info.packageName, r.info.applicationInfo.versionCode, this.mService.mProcessStats);
                }
                realStartActivityLocked(r, app, andResume, checkConfig);
                return;
            } catch (Throwable e) {
                Slog.w(TAG, "Exception when starting activity " + r.intent.getComponent().flattenToShortString(), e);
            }
        }
        this.mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0, OppoAppStartupManager.TYPE_ACTIVITY, r.intent.getComponent(), false, false, true, r);
    }

    boolean checkStartAnyActivityPermission(Intent intent, ActivityInfo aInfo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, boolean ignoreTargetSecurity, ProcessRecord callerApp, ActivityRecord resultRecord, ActivityStack resultStack, ActivityOptions options) {
        if (this.mService.checkPermission("android.permission.START_ANY_ACTIVITY", callingPid, callingUid) == 0) {
            return true;
        }
        int componentRestriction = getComponentRestrictionForCallingPackage(aInfo, callingPackage, callingPid, callingUid, ignoreTargetSecurity);
        int actionRestriction = getActionRestrictionForCallingPackage(intent.getAction(), callingPackage, callingPid, callingUid);
        String msg;
        if (componentRestriction == 1 || actionRestriction == 1) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, 0, null);
            }
            if (actionRestriction == 1) {
                msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ")" + " with revoked permission " + ((String) ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction()));
            } else if (aInfo.exported) {
                msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ")" + " requires " + aInfo.permission;
            } else {
                msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ")" + " not exported from uid " + aInfo.applicationInfo.uid;
            }
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        } else if (actionRestriction == 2) {
            Slog.w(TAG, "Appop Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ")" + " requires " + AppOpsManager.permissionToOp((String) ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction())));
            return false;
        } else if (componentRestriction == 2) {
            Slog.w(TAG, "Appop Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ")" + " requires appop " + AppOpsManager.permissionToOp(aInfo.permission));
            return false;
        } else {
            if (options != null) {
                if (options.getLaunchTaskId() == -1 || this.mService.checkPermission("android.permission.START_TASKS_FROM_RECENTS", callingPid, callingUid) != -1) {
                    int launchDisplayId = options.getLaunchDisplayId();
                    if (!(launchDisplayId == -1 || (isCallerAllowedToLaunchOnDisplay(callingPid, callingUid, launchDisplayId, aInfo) ^ 1) == 0)) {
                        msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with launchDisplayId=" + launchDisplayId;
                        Slog.w(TAG, msg);
                        throw new SecurityException(msg);
                    }
                }
                msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with launchTaskId=" + options.getLaunchTaskId();
                Slog.w(TAG, msg);
                throw new SecurityException(msg);
            }
            return true;
        }
    }

    boolean isCallerAllowedToLaunchOnDisplay(int callingPid, int callingUid, int launchDisplayId, ActivityInfo aInfo) {
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG, "Launch on display check: displayId=" + launchDisplayId + " callingPid=" + callingPid + " callingUid=" + callingUid);
        }
        if (callingPid == -1 && callingUid == -1) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "Launch on display check: no caller info, skip check");
            }
            return true;
        }
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(launchDisplayId);
        if (activityDisplay == null) {
            Slog.w(TAG, "Launch on display check: display not found");
            return false;
        } else if (this.mService.checkPermission("android.permission.INTERNAL_SYSTEM_WINDOW", callingPid, callingUid) == 0) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "Launch on display check: allow launch any on display");
            }
            return true;
        } else {
            boolean uidPresentOnDisplay = activityDisplay.isUidPresent(callingUid);
            int displayOwnerUid = activityDisplay.mDisplay.getOwnerUid();
            if (!(activityDisplay.mDisplay.getType() != 5 || displayOwnerUid == 1000 || displayOwnerUid == aInfo.applicationInfo.uid)) {
                if ((aInfo.flags & Integer.MIN_VALUE) == 0) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(TAG, "Launch on display check: disallow launch on virtual display for not-embedded activity.");
                    }
                    return false;
                } else if (this.mService.checkPermission("android.permission.ACTIVITY_EMBEDDING", callingPid, callingUid) == -1 && (uidPresentOnDisplay ^ 1) != 0) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(TAG, "Launch on display check: disallow activity embedding without permission.");
                    }
                    return false;
                }
            }
            if (!activityDisplay.isPrivate()) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "Launch on display check: allow launch on public display");
                }
                return true;
            } else if (displayOwnerUid == callingUid) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "Launch on display check: allow launch for owner of the display");
                }
                return true;
            } else if (uidPresentOnDisplay) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "Launch on display check: allow launch for caller present on the display");
                }
                return true;
            } else {
                Slog.w(TAG, "Launch on display check: denied");
                return false;
            }
        }
    }

    void updateUIDsPresentOnDisplay() {
        this.mDisplayAccessUIDs.clear();
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            if (activityDisplay.isPrivate()) {
                this.mDisplayAccessUIDs.append(activityDisplay.mDisplayId, activityDisplay.getPresentUIDs());
            }
        }
        this.mDisplayManagerInternal.setDisplayAccessUIDs(this.mDisplayAccessUIDs);
    }

    UserInfo getUserInfo(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = UserManager.get(this.mService.mContext).getUserInfo(userId);
            return userInfo;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private int getComponentRestrictionForCallingPackage(ActivityInfo activityInfo, String callingPackage, int callingPid, int callingUid, boolean ignoreTargetSecurity) {
        if (!ignoreTargetSecurity) {
            if (this.mService.checkComponentPermission(activityInfo.permission, callingPid, callingUid, activityInfo.applicationInfo.uid, activityInfo.exported) == -1) {
                return 1;
            }
        }
        if (activityInfo.permission == null) {
            return 0;
        }
        int opCode = AppOpsManager.permissionToOpCode(activityInfo.permission);
        if (opCode == -1 || this.mService.mAppOpsService.noteOperation(opCode, callingUid, callingPackage) == 0 || ignoreTargetSecurity) {
            return 0;
        }
        return 2;
    }

    void dataCollectionInfo(IApplicationThread caller, ProcessRecord callerApp, int pid, int uid, String callingPackage, Intent intent) {
        if (intent != null && intent.getComponent() != null) {
            ComponentName realActivity = intent.getComponent();
            String packageInstall = "com.android.packageinstaller.PackageInstallerActivity";
            String packageUninstall = "com.android.packageinstaller.UninstallerActivity";
            if (realActivity == null) {
                return;
            }
            if (packageInstall.equals(realActivity.getClassName()) || packageUninstall.equals(realActivity.getClassName())) {
                int i;
                String str = OppoPackageManagerHelper.OPPO_EXTRA_PID;
                if (callerApp != null) {
                    i = callerApp.pid;
                } else {
                    i = pid;
                }
                intent.putExtra(str, i);
                str = OppoPackageManagerHelper.OPPO_EXTRA_UID;
                if (callerApp != null) {
                    i = callerApp.info.uid;
                } else {
                    i = uid;
                }
                intent.putExtra(str, i);
                intent.putExtra("oppo_extra_pkg_name", callingPackage != null ? callingPackage : "");
                StringBuffer sb = new StringBuffer("dataCollection debug info ");
                if (caller == null) {
                    sb.append(" caller is null,");
                }
                if (callerApp == null) {
                    sb.append(" callerApp is null");
                }
                sb.append(" pid ");
                sb.append(pid);
                sb.append(" uid ");
                sb.append(uid);
                sb.append(" callingPackage ");
                sb.append(callingPackage);
                intent.putExtra(OppoPackageManagerHelper.OPPO_EXTRA_DEBUG_INFO, sb.toString());
            }
        }
    }

    private int getActionRestrictionForCallingPackage(String action, String callingPackage, int callingPid, int callingUid) {
        if (action == null) {
            return 0;
        }
        String permission = (String) ACTION_TO_RUNTIME_PERMISSION.get(action);
        if (permission == null) {
            return 0;
        }
        try {
            if (!ArrayUtils.contains(this.mService.mContext.getPackageManager().getPackageInfo(callingPackage, 4096).requestedPermissions, permission)) {
                return 0;
            }
            if (this.mService.checkPermission(permission, callingPid, callingUid) == -1) {
                return 1;
            }
            int opCode = AppOpsManager.permissionToOpCode(permission);
            if (opCode == -1 || this.mService.mAppOpsService.noteOperation(opCode, callingUid, callingPackage) == 0) {
                return 0;
            }
            return 2;
        } catch (NameNotFoundException e) {
            Slog.i(TAG, "Cannot find package info for " + callingPackage);
            return 0;
        }
    }

    void setLaunchSource(int uid) {
        this.mLaunchingActivity.setWorkSource(new WorkSource(uid));
    }

    void acquireLaunchWakelock() {
        this.mLaunchingActivity.acquire();
        if (!this.mHandler.hasMessages(104)) {
            this.mHandler.sendEmptyMessageDelayed(104, 10000);
        }
    }

    private boolean checkFinishBootingLocked() {
        boolean booting = this.mService.mBooting;
        boolean enableScreen = false;
        this.mService.mBooting = false;
        if (!this.mService.mBooted) {
            this.mService.mBooted = true;
            enableScreen = true;
        }
        if (booting || enableScreen) {
            this.mService.postFinishBooting(booting, enableScreen);
        }
        return booting;
    }

    final com.android.server.am.ActivityRecord activityIdleInternalLocked(android.os.IBinder r19, boolean r20, boolean r21, android.content.res.Configuration r22) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r16_1 'startingUsers' java.util.ArrayList) in PHI: PHI: (r16_2 'startingUsers' java.util.ArrayList) = (r16_0 'startingUsers' java.util.ArrayList), (r16_1 'startingUsers' java.util.ArrayList) binds: {(r16_0 'startingUsers' java.util.ArrayList)=B:29:0x00c6, (r16_1 'startingUsers' java.util.ArrayList)=B:30:0x00c8}
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
        r18 = this;
        r13 = 0;
        r16 = 0;
        r10 = 0;
        r2 = 0;
        r12 = 0;
        r11 = 0;
        r5 = com.android.server.am.ActivityRecord.forTokenLocked(r19);
        if (r5 == 0) goto L_0x0064;
    L_0x000d:
        r3 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_IDLE;
        if (r3 == 0) goto L_0x002f;
    L_0x0011:
        r3 = TAG_IDLE;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r6 = "activityIdleInternalLocked: Callers=";
        r4 = r4.append(r6);
        r6 = 4;
        r6 = android.os.Debug.getCallers(r6);
        r4 = r4.append(r6);
        r4 = r4.toString();
        android.util.Slog.d(r3, r4);
    L_0x002f:
        r0 = r18;
        r3 = r0.mHandler;
        r4 = 100;
        r3.removeMessages(r4, r5);
        r5.finishLaunchTickingLocked();
        if (r20 == 0) goto L_0x0048;
    L_0x003d:
        r6 = -1;
        r8 = -1;
        r3 = r18;
        r4 = r20;
        r3.reportActivityLaunchedLocked(r4, r5, r6, r8);
    L_0x0048:
        if (r22 == 0) goto L_0x004f;
    L_0x004a:
        r0 = r22;
        r5.setLastReportedGlobalConfiguration(r0);
    L_0x004f:
        r3 = 1;
        r5.idle = r3;
        r3 = r5.getStack();
        r0 = r18;
        r3 = r0.isFocusedStack(r3);
        if (r3 != 0) goto L_0x0060;
    L_0x005e:
        if (r20 == 0) goto L_0x0064;
    L_0x0060:
        r12 = r18.checkFinishBootingLocked();
    L_0x0064:
        r3 = r18.allResumedActivitiesIdle();
        if (r3 == 0) goto L_0x0095;
    L_0x006a:
        if (r5 == 0) goto L_0x0073;
    L_0x006c:
        r0 = r18;
        r3 = r0.mService;
        r3.scheduleAppGcsLocked();
    L_0x0073:
        r0 = r18;
        r3 = r0.mLaunchingActivity;
        r3 = r3.isHeld();
        if (r3 == 0) goto L_0x008d;
    L_0x007d:
        r0 = r18;
        r3 = r0.mHandler;
        r4 = 104; // 0x68 float:1.46E-43 double:5.14E-322;
        r3.removeMessages(r4);
        r0 = r18;
        r3 = r0.mLaunchingActivity;
        r3.release();
    L_0x008d:
        r3 = 0;
        r4 = 0;
        r6 = 0;
        r0 = r18;
        r0.ensureActivitiesVisibleLocked(r3, r4, r6);
    L_0x0095:
        r3 = 1;
        r0 = r18;
        r1 = r21;
        r17 = r0.processStoppingActivitiesLocked(r5, r3, r1);
        if (r17 == 0) goto L_0x00f7;
    L_0x00a0:
        r10 = r17.size();
    L_0x00a4:
        r0 = r18;
        r3 = r0.mFinishingActivities;
        r2 = r3.size();
        if (r2 <= 0) goto L_0x00be;
    L_0x00ae:
        r13 = new java.util.ArrayList;
        r0 = r18;
        r3 = r0.mFinishingActivities;
        r13.<init>(r3);
        r0 = r18;
        r3 = r0.mFinishingActivities;
        r3.clear();
    L_0x00be:
        r0 = r18;
        r3 = r0.mStartingUsers;
        r3 = r3.size();
        if (r3 <= 0) goto L_0x00da;
    L_0x00c8:
        r16 = new java.util.ArrayList;
        r0 = r18;
        r3 = r0.mStartingUsers;
        r0 = r16;
        r0.<init>(r3);
        r0 = r18;
        r3 = r0.mStartingUsers;
        r3.clear();
    L_0x00da:
        r14 = 0;
    L_0x00db:
        if (r14 >= r10) goto L_0x00fd;
    L_0x00dd:
        r0 = r17;
        r5 = r0.get(r14);
        r5 = (com.android.server.am.ActivityRecord) r5;
        r15 = r5.getStack();
        if (r15 == 0) goto L_0x00f4;
    L_0x00eb:
        r3 = r5.finishing;
        if (r3 == 0) goto L_0x00f9;
    L_0x00ef:
        r3 = 0;
        r4 = 0;
        r15.finishCurrentActivityLocked(r5, r3, r4);
    L_0x00f4:
        r14 = r14 + 1;
        goto L_0x00db;
    L_0x00f7:
        r10 = 0;
        goto L_0x00a4;
    L_0x00f9:
        r15.stopActivityLocked(r5);
        goto L_0x00f4;
    L_0x00fd:
        r14 = 0;
    L_0x00fe:
        if (r14 >= r2) goto L_0x0118;
    L_0x0100:
        r5 = r13.get(r14);
        r5 = (com.android.server.am.ActivityRecord) r5;
        r15 = r5.getStack();
        if (r15 == 0) goto L_0x0115;
    L_0x010c:
        r3 = "finish-idle";
        r4 = 1;
        r3 = r15.destroyActivityLocked(r5, r4, r3);
        r11 = r11 | r3;
    L_0x0115:
        r14 = r14 + 1;
        goto L_0x00fe;
    L_0x0118:
        if (r12 != 0) goto L_0x0137;
    L_0x011a:
        if (r16 == 0) goto L_0x0137;
    L_0x011c:
        r14 = 0;
    L_0x011d:
        r3 = r16.size();
        if (r14 >= r3) goto L_0x0137;
    L_0x0123:
        r0 = r18;
        r3 = r0.mService;
        r4 = r3.mUserController;
        r0 = r16;
        r3 = r0.get(r14);
        r3 = (com.android.server.am.UserState) r3;
        r4.finishUserSwitch(r3);
        r14 = r14 + 1;
        goto L_0x011d;
    L_0x0137:
        r0 = r18;
        r3 = r0.mService;
        r3.trimApplications();
        if (r11 == 0) goto L_0x0143;
    L_0x0140:
        r18.resumeFocusedStackTopActivityLocked();
    L_0x0143:
        return r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ActivityStackSupervisor.activityIdleInternalLocked(android.os.IBinder, boolean, boolean, android.content.res.Configuration):com.android.server.am.ActivityRecord");
    }

    boolean handleAppDiedLocked(ProcessRecord app) {
        boolean hasVisibleActivities = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                hasVisibleActivities |= ((ActivityStack) stacks.get(stackNdx)).handleAppDiedLocked(app);
            }
        }
        return hasVisibleActivities;
    }

    void closeSystemDialogsLocked() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ((ActivityStack) stacks.get(stackNdx)).closeSystemDialogsLocked();
            }
        }
    }

    void removeUserLocked(int userId) {
        this.mUserStackInFront.delete(userId);
    }

    void updateUserStackLocked(int userId, ActivityStack stack) {
        if (userId != this.mCurrentUser) {
            this.mUserStackInFront.put(userId, stack != null ? stack.getStackId() : 0);
        }
    }

    boolean finishDisabledPackageActivitiesLocked(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId) {
        boolean didSomething = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                if (((ActivityStack) stacks.get(stackNdx)).finishDisabledPackageActivitiesLocked(packageName, filterByClasses, doit, evenPersistent, userId)) {
                    didSomething = true;
                }
            }
        }
        return didSomething;
    }

    void updatePreviousProcessLocked(ActivityRecord r) {
        ProcessRecord fgApp = null;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            int stackNdx = stacks.size() - 1;
            while (stackNdx >= 0) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                if (isFocusedStack(stack)) {
                    if (stack.mResumedActivity != null) {
                        fgApp = stack.mResumedActivity.app;
                    } else if (stack.mPausingActivity != null) {
                        fgApp = stack.mPausingActivity.app;
                    }
                } else {
                    stackNdx--;
                }
            }
        }
        if (r.app != null && fgApp != null && r.app != fgApp && r.lastVisibleTime > this.mService.mPreviousProcessVisibleTime && r.app != this.mService.mHomeProcess) {
            this.mService.mPreviousProcess = r.app;
            this.mService.mPreviousProcessVisibleTime = r.lastVisibleTime;
        }
    }

    boolean resumeFocusedStackTopActivityLocked() {
        return resumeFocusedStackTopActivityLocked(null, null, null);
    }

    boolean resumeFocusedStackTopActivityLocked(ActivityStack targetStack, ActivityRecord target, ActivityOptions targetOptions) {
        if (!readyToResume()) {
            return false;
        }
        if (targetStack != null && (((targetStack.mStackId == 1 && isFocusedStack(targetStack)) || (targetStack.mStackId == 2 && (isFocusedStack(targetStack) ^ 1) != 0)) && target != null)) {
            ActivityStack freeformStack = target.getStack();
            if (freeformStack != null && freeformStack.mStackId == 2) {
                return freeformStack.resumeTopActivityUncheckedLocked(target, targetOptions);
            }
        }
        if (targetStack != null && isFocusedStack(targetStack)) {
            return targetStack.resumeTopActivityUncheckedLocked(target, targetOptions);
        }
        ActivityRecord r = this.mFocusedStack.topRunningActivityLocked();
        if (r == null || r.state != ActivityState.RESUMED) {
            return this.mFocusedStack.resumeTopActivityUncheckedLocked(null, null);
        }
        if (r.state == ActivityState.RESUMED) {
            this.mFocusedStack.executeAppTransition(targetOptions);
        }
        return false;
    }

    void updateActivityApplicationInfoLocked(ApplicationInfo aInfo) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ((ActivityStack) stacks.get(stackNdx)).updateActivityApplicationInfoLocked(aInfo);
            }
        }
    }

    TaskRecord finishTopRunningActivityLocked(ProcessRecord app, String reason) {
        TaskRecord finishedTask = null;
        ActivityStack focusedStack = getFocusedStack();
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            int numStacks = stacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                TaskRecord t = stack.finishTopRunningActivityLocked(app, reason);
                if (stack == focusedStack || finishedTask == null) {
                    finishedTask = t;
                }
            }
        }
        return finishedTask;
    }

    void finishVoiceTask(IVoiceInteractionSession session) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            int numStacks = stacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ((ActivityStack) stacks.get(stackNdx)).finishVoiceTask(session);
            }
        }
    }

    void findTaskToMoveToFrontLocked(TaskRecord task, int flags, ActivityOptions options, String reason, boolean forceNonResizeable) {
        ActivityStack focusedStack = getFocusedStack();
        ActivityRecord top_activity = focusedStack != null ? focusedStack.topActivity() : null;
        if (top_activity != null && top_activity.state == ActivityState.DESTROYED) {
            acquireAppLaunchPerfLock(top_activity.packageName);
        }
        if (top_activity != null && top_activity.state == ActivityState.DESTROYED) {
            acquireAppLaunch();
        }
        if ((flags & 2) == 0) {
            this.mUserLeaving = true;
        }
        if ((flags & 1) != 0) {
            task.setTaskToReturnTo(1);
        }
        ActivityStack currentStack = task.getStack();
        if (currentStack == null) {
            Slog.e(TAG, "findTaskToMoveToFrontLocked: can't move task=" + task + " to front. Stack is null");
            return;
        }
        if (task.isResizeable() && options != null) {
            int stackId = options.getLaunchStackId();
            if (canUseActivityOptionsLaunchBounds(options, stackId)) {
                Rect bounds = TaskRecord.validateBounds(options.getLaunchBounds());
                task.updateOverrideConfiguration(bounds);
                if (stackId == -1) {
                    stackId = task.getLaunchStackId();
                }
                if (stackId != currentStack.mStackId) {
                    task.reparent(stackId, true, 1, false, true, "findTaskToMoveToFrontLocked");
                    stackId = currentStack.mStackId;
                }
                if (StackId.resizeStackWithLaunchBounds(stackId)) {
                    resizeStackLocked(stackId, bounds, null, null, false, true, false);
                } else {
                    task.resizeWindowContainer();
                }
            }
        }
        if (currentStack.mStackId == 1) {
            ActivityStack stack = getStack(2);
            if (stack != null) {
                stack.moveToFront("moveFreeFormStack");
            }
        }
        ActivityRecord r = task.getTopActivity();
        currentStack.moveTaskToFrontLocked(task, false, options, r == null ? null : r.appTimeTracker, reason);
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.d(TAG_STACK, "findTaskToMoveToFront: moved to front of stack=" + currentStack);
        }
        handleNonResizableTaskIfNeeded(task, -1, 0, currentStack.mStackId, forceNonResizeable);
    }

    boolean canUseActivityOptionsLaunchBounds(ActivityOptions options, int launchStackId) {
        if (options.getLaunchBounds() == null) {
            return false;
        }
        boolean z;
        if (this.mService.mSupportsPictureInPicture && launchStackId == 4) {
            z = true;
        } else {
            z = this.mService.mSupportsFreeformWindowManagement;
        }
        return z;
    }

    void createPreloadStack() {
        ActivityStack preloadStack = getStack(7, true, false);
        Point displayBounds = new Point(0, 0);
        int height = 0;
        this.mService.mWindowManager.getInitialDisplaySize(0, displayBounds);
        if (!displayBounds.equals(new Point(0, 0))) {
            int width = displayBounds.x;
            height = displayBounds.y;
            if (width > height) {
                int temp = width;
                width = height;
                height = temp;
            }
        }
        Rect preloadBounds = new Rect(height, 0, height + height, height);
        if (preloadStack != null) {
            if (!preloadBounds.equals(preloadStack.mBounds)) {
                preloadStack.mFullscreen = false;
                preloadStack.setBounds(preloadBounds);
            }
            Slog.e("hsg", "preloadStack.bounds is " + preloadStack.mBounds);
            return;
        }
        Slog.e("hsg", "preloadStack is null");
    }

    protected <T extends ActivityStack> T getStack(int stackId) {
        return getStack(stackId, false, false);
    }

    protected <T extends ActivityStack> T getStack(int stackId, boolean createStaticStackIfNeeded, boolean createOnTop) {
        ActivityStack stack = (ActivityStack) this.mStacks.get(stackId);
        if (stack != null) {
            return stack;
        }
        if ((!createStaticStackIfNeeded || (StackId.isStaticStack(stackId) ^ 1) != 0) && stackId != 7) {
            return null;
        }
        if (stackId == 3) {
            getStack(5, true, createOnTop);
        }
        return createStackOnDisplay(stackId, 0, createOnTop);
    }

    ActivityStack getValidLaunchStackOnDisplay(int displayId, ActivityRecord r) {
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (activityDisplay == null) {
            throw new IllegalArgumentException("Display with displayId=" + displayId + " not found.");
        }
        for (int i = activityDisplay.mStacks.size() - 1; i >= 0; i--) {
            ActivityStack stack = (ActivityStack) activityDisplay.mStacks.get(i);
            if (this.mService.mActivityStarter.isValidLaunchStackId(stack.mStackId, displayId, r)) {
                return stack;
            }
        }
        if (displayId != 0) {
            int newDynamicStackId = getNextStackId();
            if (this.mService.mActivityStarter.isValidLaunchStackId(newDynamicStackId, displayId, r)) {
                return createStackOnDisplay(newDynamicStackId, displayId, true);
            }
        }
        Slog.w(TAG, "getValidLaunchStackOnDisplay: can't launch on displayId " + displayId);
        return null;
    }

    ArrayList<ActivityStack> getStacks() {
        ArrayList<ActivityStack> allStacks = new ArrayList();
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            allStacks.addAll(((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks);
        }
        return allStacks;
    }

    ArrayList<ActivityStack> getStacksOnDefaultDisplay() {
        return ((ActivityDisplay) this.mActivityDisplays.valueAt(0)).mStacks;
    }

    ActivityStack getNextFocusableStackLocked(ActivityStack currentFocus) {
        this.mWindowManager.getDisplaysInFocusOrder(this.mTmpOrderedDisplayIds);
        for (int i = this.mTmpOrderedDisplayIds.size() - 1; i >= 0; i--) {
            List<ActivityStack> stacks = getActivityDisplayOrCreateLocked(this.mTmpOrderedDisplayIds.get(i)).mStacks;
            for (int j = stacks.size() - 1; j >= 0; j--) {
                ActivityStack stack = (ActivityStack) stacks.get(j);
                if (stack != currentFocus && stack.isFocusable() && stack.shouldBeVisible(null) != 0) {
                    return stack;
                }
            }
        }
        return null;
    }

    ActivityStack getNextValidLaunchStackLocked(ActivityRecord r, int currentFocus) {
        this.mWindowManager.getDisplaysInFocusOrder(this.mTmpOrderedDisplayIds);
        for (int i = this.mTmpOrderedDisplayIds.size() - 1; i >= 0; i--) {
            int displayId = this.mTmpOrderedDisplayIds.get(i);
            if (displayId != currentFocus) {
                ActivityStack stack = getValidLaunchStackOnDisplay(displayId, r);
                if (stack != null) {
                    return stack;
                }
            }
        }
        return null;
    }

    ActivityRecord getHomeActivity() {
        return getHomeActivityForUser(this.mCurrentUser);
    }

    ActivityRecord getHomeActivityForUser(int userId) {
        ArrayList<TaskRecord> tasks = this.mHomeStack.getAllTasks();
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = (TaskRecord) tasks.get(taskNdx);
            if (task.isHomeTask()) {
                ArrayList<ActivityRecord> activities = task.mActivities;
                for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                    ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                    if (r.isHomeActivity() && (userId == -1 || r.userId == userId)) {
                        return r;
                    }
                }
                continue;
            }
        }
        return null;
    }

    boolean isStackDockedInEffect(int stackId) {
        if (stackId != 3) {
            return StackId.isResizeableByDockedStack(stackId) && getStack(3) != null;
        } else {
            return true;
        }
    }

    void resizeStackLocked(int stackId, Rect bounds, Rect tempTaskBounds, Rect tempTaskInsetBounds, boolean preserveWindows, boolean allowResizeInDockedMode, boolean deferResume) {
        if (stackId == 3) {
            resizeDockedStackLocked(bounds, tempTaskBounds, tempTaskInsetBounds, null, null, preserveWindows, deferResume);
            return;
        }
        ActivityStack stack = getStack(stackId);
        if (stack == null) {
            Slog.w(TAG, "resizeStack: stackId " + stackId + " not found.");
        } else if (allowResizeInDockedMode || (StackId.tasksAreFloating(stackId) ^ 1) == 0 || getStack(3) == null) {
            Trace.traceBegin(64, "am.resizeStack_" + stackId);
            this.mWindowManager.deferSurfaceLayout();
            try {
                stack.resize(bounds, tempTaskBounds, tempTaskInsetBounds);
                if (!deferResume) {
                    stack.ensureVisibleActivitiesConfigurationLocked(stack.topRunningActivityLocked(), preserveWindows);
                }
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
            } catch (Throwable th) {
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
            }
        }
    }

    void deferUpdateBounds(int stackId) {
        ActivityStack stack = getStack(stackId);
        if (stack != null) {
            stack.deferUpdateBounds();
        }
    }

    void continueUpdateBounds(int stackId) {
        ActivityStack stack = getStack(stackId);
        if (stack != null) {
            stack.continueUpdateBounds();
        }
    }

    void notifyAppTransitionDone() {
        continueUpdateBounds(5);
        for (int i = this.mResizingTasksDuringAnimation.size() - 1; i >= 0; i--) {
            TaskRecord task = anyTaskForIdLocked(((Integer) this.mResizingTasksDuringAnimation.valueAt(i)).intValue(), 0, -1);
            if (task != null) {
                task.setTaskDockedResizing(false);
            }
        }
        this.mResizingTasksDuringAnimation.clear();
    }

    private void moveTasksToFullscreenStackInSurfaceTransaction(int fromStackId, boolean onTop) {
        ActivityStack stack = getStack(fromStackId);
        if (stack != null) {
            int i;
            this.mWindowManager.deferSurfaceLayout();
            if (fromStackId == 3) {
                i = 0;
                while (i <= 7) {
                    try {
                        if (StackId.isResizeableByDockedStack(i) && getStack(i) != null) {
                            resizeStackLocked(i, null, null, null, true, true, true);
                        }
                        i++;
                    } catch (Throwable th) {
                        this.mAllowDockedStackResize = true;
                        this.mWindowManager.continueSurfaceLayout();
                    }
                }
                this.mAllowDockedStackResize = false;
            } else if (fromStackId == 4 && onTop) {
                MetricsLogger.action(this.mService.mContext, 820);
            }
            ActivityStack fullscreenStack = getStack(1);
            boolean isFullscreenStackVisible = fullscreenStack != null ? fullscreenStack.shouldBeVisible(null) == 1 : false;
            boolean schedulePictureInPictureModeChange = fromStackId == 4;
            ArrayList<TaskRecord> tasks = stack.getAllTasks();
            int size = tasks.size();
            if (onTop) {
                i = 0;
                while (i < size) {
                    TaskRecord task = (TaskRecord) tasks.get(i);
                    boolean isTopTask = i == size + -1;
                    if (fromStackId == 4) {
                        int i2 = (isFullscreenStackVisible && onTop) ? 0 : 1;
                        task.setTaskToReturnTo(i2);
                    }
                    task.reparent(1, true, 0, isTopTask, true, schedulePictureInPictureModeChange, "moveTasksToFullscreenStack - onTop");
                    i++;
                }
            } else {
                for (i = 0; i < size; i++) {
                    ((TaskRecord) tasks.get(i)).reparent(1, i, 2, false, true, schedulePictureInPictureModeChange, "moveTasksToFullscreenStack - NOT_onTop");
                }
            }
            ensureActivitiesVisibleLocked(null, 0, true);
            resumeFocusedStackTopActivityLocked();
            this.mAllowDockedStackResize = true;
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    void moveTasksToFullscreenStackLocked(int fromStackId, boolean onTop) {
        this.mWindowManager.inSurfaceTransaction(new -$Lambda$5yQSwWrsRDcxoFuTXgyaBIqPvDw((byte) 0, onTop, fromStackId, this));
    }

    /* synthetic */ void lambda$-com_android_server_am_ActivityStackSupervisor_126527(int fromStackId, boolean onTop) {
        moveTasksToFullscreenStackInSurfaceTransaction(fromStackId, onTop);
    }

    void resizeDockedStackLocked(Rect dockedBounds, Rect tempDockedTaskBounds, Rect tempDockedTaskInsetBounds, Rect tempOtherTaskBounds, Rect tempOtherTaskInsetBounds, boolean preserveWindows) {
        resizeDockedStackLocked(dockedBounds, tempDockedTaskBounds, tempDockedTaskInsetBounds, tempOtherTaskBounds, tempOtherTaskInsetBounds, preserveWindows, false);
    }

    void resizeDockedStackLocked(Rect dockedBounds, Rect tempDockedTaskBounds, Rect tempDockedTaskInsetBounds, Rect tempOtherTaskBounds, Rect tempOtherTaskInsetBounds, boolean preserveWindows, boolean deferResume) {
        if (this.mAllowDockedStackResize) {
            ActivityStack stack = getStack(3);
            if (stack == null) {
                Slog.w(TAG, "resizeDockedStackLocked: docked stack not found");
                return;
            }
            Trace.traceBegin(64, "am.resizeDockedStack");
            this.mWindowManager.deferSurfaceLayout();
            try {
                this.mAllowDockedStackResize = false;
                ActivityRecord r = stack.topRunningActivityLocked();
                stack.resize(dockedBounds, tempDockedTaskBounds, tempDockedTaskInsetBounds);
                if (stack.mFullscreen || (dockedBounds == null && (stack.isAttached() ^ 1) != 0)) {
                    moveTasksToFullscreenStackLocked(3, true);
                    r = null;
                } else {
                    Rect otherTaskRect = new Rect();
                    int i = 0;
                    while (i <= 7) {
                        ActivityStack current = getStack(i);
                        if (current != null && StackId.isResizeableByDockedStack(i)) {
                            current.getStackDockedModeBounds(tempOtherTaskBounds, this.tempRect, otherTaskRect, true);
                            if (current.mStackId == 1 && i == 1 && current.mFullscreen && current.mResumedActivity != null && current.mResumedActivity.shortComponentName != null && current.mResumedActivity.shortComponentName.contains(OPPO_SAFECENTER_PASSWORD)) {
                                resizeStackLocked(i, null, null, null, true, true, true);
                            } else {
                                Rect rect;
                                Rect rect2 = !this.tempRect.isEmpty() ? this.tempRect : null;
                                if (otherTaskRect.isEmpty()) {
                                    rect = tempOtherTaskBounds;
                                } else {
                                    rect = otherTaskRect;
                                }
                                resizeStackLocked(i, rect2, rect, tempOtherTaskInsetBounds, preserveWindows, true, deferResume);
                            }
                        }
                        i++;
                    }
                }
                if (!deferResume) {
                    stack.ensureVisibleActivitiesConfigurationLocked(r, preserveWindows);
                }
                this.mAllowDockedStackResize = true;
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
            } catch (Throwable th) {
                this.mAllowDockedStackResize = true;
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
            }
        }
    }

    void resizePinnedStackLocked(Rect pinnedBounds, Rect tempPinnedTaskBounds) {
        PinnedActivityStack stack = (PinnedActivityStack) getStack(4);
        if (stack == null) {
            Slog.w(TAG, "resizePinnedStackLocked: pinned stack not found");
        } else if (!((PinnedStackWindowController) stack.getWindowContainerController()).pinnedStackResizeDisallowed()) {
            Trace.traceBegin(64, "am.resizePinnedStack");
            this.mWindowManager.deferSurfaceLayout();
            try {
                ActivityRecord r = stack.topRunningActivityLocked();
                Rect insetBounds = null;
                if (tempPinnedTaskBounds != null) {
                    insetBounds = this.tempRect;
                    insetBounds.top = 0;
                    insetBounds.left = 0;
                    insetBounds.right = tempPinnedTaskBounds.width();
                    insetBounds.bottom = tempPinnedTaskBounds.height();
                }
                stack.resize(pinnedBounds, tempPinnedTaskBounds, insetBounds);
                stack.ensureVisibleActivitiesConfigurationLocked(r, false);
            } finally {
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
            }
        }
    }

    ActivityStack createStackOnDisplay(int stackId, int displayId, boolean onTop) {
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (activityDisplay == null) {
            return null;
        }
        return createStack(stackId, activityDisplay, onTop);
    }

    ActivityStack createStack(int stackId, ActivityDisplay display, boolean onTop) {
        switch (stackId) {
            case 4:
                return new PinnedActivityStack(display, stackId, this, this.mRecentTasks, onTop);
            default:
                if (stackId != 3 || getFocusedStack() == null) {
                    return new ActivityStack(display, stackId, this, this.mRecentTasks, onTop);
                }
                ActivityStack tmpStack = new ActivityStack(display, stackId, this, this.mRecentTasks, onTop);
                tmpStack.mDockComponentName = getFocusedStack().mComponentName;
                return tmpStack;
        }
    }

    void removeStackInSurfaceTransaction(int stackId) {
        ActivityStack stack = getStack(stackId);
        if (stack != null) {
            ArrayList<TaskRecord> tasks = stack.getAllTasks();
            if (stack.getStackId() == 4) {
                PinnedActivityStack pinnedStack = (PinnedActivityStack) stack;
                pinnedStack.mForceHidden = true;
                pinnedStack.ensureActivitiesVisibleLocked(null, 0, true);
                pinnedStack.mForceHidden = false;
                activityIdleInternalLocked(null, false, true, null);
                moveTasksToFullscreenStackLocked(4, false);
            } else {
                for (int i = tasks.size() - 1; i >= 0; i--) {
                    removeTaskByIdLocked(((TaskRecord) tasks.get(i)).taskId, true, true);
                }
            }
        }
    }

    void removeStackLocked(int stackId) {
        this.mWindowManager.inSurfaceTransaction(new -$Lambda$wXoCvN1vCS9Im-C0Hwk121gFGr0(stackId, this));
    }

    /* synthetic */ void lambda$-com_android_server_am_ActivityStackSupervisor_136475(int stackId) {
        removeStackInSurfaceTransaction(stackId);
    }

    boolean removeTaskByIdLocked(int taskId, boolean killProcess, boolean removeFromRecents) {
        return removeTaskByIdLocked(taskId, killProcess, removeFromRecents, false);
    }

    boolean removeTaskByIdLocked(int taskId, boolean killProcess, boolean removeFromRecents, boolean pauseImmediately) {
        TaskRecord tr = anyTaskForIdLocked(taskId, 1, -1);
        if (tr != null) {
            tr.removeTaskActivitiesLocked(pauseImmediately);
            if (ActivityManagerDebugConfig.DEBUG_TASKS && killProcess) {
                int callerPid = Binder.getCallingPid();
                Slog.d(TAG, "kill proc from removeTask, callerPid:" + callerPid + ", callerUid:" + Binder.getCallingUid() + Debug.getCallers(5));
            }
            cleanUpRemovedTaskLocked(tr, killProcess, removeFromRecents);
            if (tr.isPersistable) {
                this.mService.notifyTaskPersisterLocked(null, true);
            }
            return true;
        }
        Slog.w(TAG, "Request to remove task ignored for non-existent task " + taskId);
        return false;
    }

    void cleanUpRemovedTaskLocked(TaskRecord tr, boolean killProcess, boolean removeFromRecents) {
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.i(TAG, "cleanUpRemovedTaskLocked " + tr + " killProcess" + killProcess);
        }
        if (removeFromRecents) {
            this.mRecentTasks.remove(tr);
            tr.removedFromRecents();
        }
        ComponentName component = tr.getBaseIntent().getComponent();
        if (component == null) {
            Slog.w(TAG, "No component for base intent of task: " + tr);
            return;
        }
        this.mService.mServices.cleanUpRemovedTaskLocked(tr, component, new Intent(tr.getBaseIntent()));
        if (killProcess) {
            int i;
            String pkg = component.getPackageName();
            ArrayList<ProcessRecord> procsToKill = new ArrayList();
            ArrayMap<String, SparseArray<ProcessRecord>> pmap = this.mService.mProcessNames.getMap();
            for (i = 0; i < pmap.size(); i++) {
                SparseArray<ProcessRecord> uids = (SparseArray) pmap.valueAt(i);
                for (int j = 0; j < uids.size(); j++) {
                    ProcessRecord proc = (ProcessRecord) uids.valueAt(j);
                    if (proc.userId == tr.userId && proc != this.mService.mHomeProcess && proc.pkgList.containsKey(pkg)) {
                        int k = 0;
                        while (k < proc.activities.size()) {
                            TaskRecord otherTask = ((ActivityRecord) proc.activities.get(k)).getTask();
                            if (tr.taskId == otherTask.taskId || !otherTask.inRecents) {
                                k++;
                            } else {
                                return;
                            }
                        }
                        if (!proc.foregroundServices) {
                            if (!(proc.processName == null || proc.info == null || proc.info.packageName == null)) {
                                ArrayList<String> filterList = OppoListManager.getInstance().getRemoveTaskFilterPkgList(this.mService.mContext);
                                if (!(filterList == null || (filterList.isEmpty() ^ 1) == 0)) {
                                    for (String str : filterList) {
                                        if (str != null && proc.info.packageName.equals(str)) {
                                            Slog.d(TAG, "remove task filter pkg process " + proc.processName);
                                            return;
                                        }
                                    }
                                }
                                ArrayList<String> stageProtectList = OppoListManager.getInstance().getStageProtectList();
                                if (!(stageProtectList == null || (stageProtectList.isEmpty() ^ 1) == 0)) {
                                    for (String str2 : stageProtectList) {
                                        if (str2 != null && proc.info.packageName.equals(str2)) {
                                            Slog.d(TAG, "remove task filter process for stage protect " + proc.processName);
                                            return;
                                        }
                                    }
                                }
                                ArrayList<String> proFilterList = OppoListManager.getInstance().getRemoveTaskFilterProcessList(this.mService.mContext);
                                if (proFilterList != null && proFilterList.contains(proc.processName)) {
                                    Slog.d(TAG, "remove task filter process " + proc.processName);
                                }
                            }
                            procsToKill.add(proc);
                        } else {
                            return;
                        }
                    }
                }
            }
            for (i = 0; i < procsToKill.size(); i++) {
                ProcessRecord pr = (ProcessRecord) procsToKill.get(i);
                if (pr.setSchedGroup != 0 || !pr.curReceivers.isEmpty()) {
                    pr.waitingToKill = "remove task";
                } else if (LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON.equals(SystemProperties.get("oppo.clear.running", "0"))) {
                    pr.oppoClearProcess("remove task", true);
                } else {
                    pr.kill("remove task", true);
                }
            }
        }
    }

    int getNextStackId() {
        while (true) {
            if (this.mNextFreeStackId >= 8 && getStack(this.mNextFreeStackId) == null) {
                return this.mNextFreeStackId;
            }
            this.mNextFreeStackId++;
        }
    }

    boolean restoreRecentTaskLocked(TaskRecord task, int stackId) {
        if (!StackId.isStaticStack(stackId)) {
            stackId = task.getLaunchStackId();
        } else if (stackId == 3 && (task.supportsSplitScreen() ^ 1) != 0) {
            stackId = 1;
        }
        ActivityStack currentStack = task.getStack();
        if (currentStack != null) {
            if (currentStack.mStackId == stackId) {
                return true;
            }
            currentStack.removeTask(task, "restoreRecentTaskLocked", 1);
        }
        ActivityStack stack = getStack(stackId, true, false);
        if (stack == null) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.v(TAG_RECENTS, "Unable to find/create stack to restore recent task=" + task);
            }
            return false;
        }
        stack.addTask(task, false, "restoreRecentTask");
        task.createWindowContainer(false, true);
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.v(TAG_RECENTS, "Added restored task=" + task + " to stack=" + stack);
        }
        ArrayList<ActivityRecord> activities = task.mActivities;
        for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
            ((ActivityRecord) activities.get(activityNdx)).createWindowContainer();
        }
        return true;
    }

    void moveStackToDisplayLocked(int stackId, int displayId, boolean onTop) {
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (activityDisplay == null) {
            throw new IllegalArgumentException("moveStackToDisplayLocked: Unknown displayId=" + displayId);
        }
        ActivityStack stack = (ActivityStack) this.mStacks.get(stackId);
        if (stack == null) {
            throw new IllegalArgumentException("moveStackToDisplayLocked: Unknown stackId=" + stackId);
        }
        ActivityDisplay currentDisplay = stack.getDisplay();
        if (currentDisplay == null) {
            throw new IllegalStateException("moveStackToDisplayLocked: Stack with stack=" + stack + " is not attached to any display.");
        } else if (currentDisplay.mDisplayId == displayId) {
            throw new IllegalArgumentException("Trying to move stack=" + stack + " to its current displayId=" + displayId);
        } else {
            stack.reparent(activityDisplay, onTop);
        }
    }

    ActivityStack getReparentTargetStack(TaskRecord task, int stackId, boolean toTop) {
        ActivityStack prevStack = task.getStack();
        if (prevStack != null && prevStack.mStackId == stackId) {
            Slog.w(TAG, "Can not reparent to same stack, task=" + task + " already in stackId=" + stackId);
            return prevStack;
        } else if (StackId.isMultiWindowStack(stackId) && (this.mService.mSupportsMultiWindow ^ 1) != 0) {
            throw new IllegalArgumentException("Device doesn't support multi-window, can not reparent task=" + task + " to stackId=" + stackId);
        } else if (StackId.isDynamicStack(stackId) && (this.mService.mSupportsMultiDisplay ^ 1) != 0) {
            throw new IllegalArgumentException("Device doesn't support multi-display, can not reparent task=" + task + " to stackId=" + stackId);
        } else if (stackId != 2 || (this.mService.mSupportsFreeformWindowManagement ^ 1) == 0) {
            if (stackId == 3 && (task.isResizeable() ^ 1) != 0) {
                stackId = prevStack != null ? prevStack.mStackId : 1;
                Slog.w(TAG, "Can not move unresizeable task=" + task + " to docked stack." + " Moving to stackId=" + stackId + " instead.");
            }
            try {
                task.mTemporarilyUnresizable = true;
                ActivityStack stack = getStack(stackId, true, toTop);
                return stack;
            } finally {
                task.mTemporarilyUnresizable = false;
            }
        } else {
            throw new IllegalArgumentException("Device doesn't support freeform, can not reparent task=" + task);
        }
    }

    boolean moveTopStackActivityToPinnedStackLocked(int stackId, Rect destBounds) {
        ActivityStack stack = getStack(stackId, false, false);
        if (stack == null) {
            throw new IllegalArgumentException("moveTopStackActivityToPinnedStackLocked: Unknown stackId=" + stackId);
        }
        ActivityRecord r = stack.topRunningActivityLocked();
        if (r == null) {
            Slog.w(TAG, "moveTopStackActivityToPinnedStackLocked: No top running activity in stack=" + stack);
            return false;
        } else if (this.mService.mForceResizableActivities || (r.supportsPictureInPicture() ^ 1) == 0) {
            moveActivityToPinnedStackLocked(r, null, OppoBrightUtils.MIN_LUX_LIMITI, true, "moveTopActivityToPinnedStack");
            return true;
        } else {
            Slog.w(TAG, "moveTopStackActivityToPinnedStackLocked: Picture-In-Picture not supported for  r=" + r);
            return false;
        }
    }

    void moveActivityToPinnedStackLocked(ActivityRecord r, Rect sourceHintBounds, float aspectRatio, boolean moveHomeStackToFront, String reason) {
        this.mWindowManager.deferSurfaceLayout();
        moveTasksToFullscreenStackLocked(4, false);
        PinnedActivityStack stack = (PinnedActivityStack) getStack(4, true, true);
        try {
            TaskRecord task = r.getTask();
            resizeStackLocked(4, task.mBounds, null, null, false, true, false);
            if (task.mActivities.size() == 1) {
                if (moveHomeStackToFront && task.getTaskToReturnTo() == 1 && (r.state == ActivityState.RESUMED || (r.supportsEnterPipOnTaskSwitch ^ 1) != 0)) {
                    moveHomeStackToFront(reason);
                }
                task.reparent(4, true, 0, false, true, false, reason);
            } else {
                TaskRecord newTask = task.getStack().createTaskRecord(getNextTaskIdForUserLocked(r.userId), r.info, r.intent, null, null, true, r.mActivityType);
                r.reparent(newTask, Integer.MAX_VALUE, "moveActivityToStack");
                newTask.reparent(4, true, 0, false, true, false, reason);
            }
            r.supportsEnterPipOnTaskSwitch = false;
            stack.animateResizePinnedStack(sourceHintBounds, stack.getDefaultPictureInPictureBounds(aspectRatio), -1, true);
            ensureActivitiesVisibleLocked(null, 0, false);
            resumeFocusedStackTopActivityLocked();
            this.mService.mTaskChangeNotificationController.notifyActivityPinned(r.packageName, r.userId, r.getTask().taskId);
        } finally {
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    boolean moveFocusableActivityStackToFrontLocked(ActivityRecord r, String reason) {
        if (r == null || (r.isFocusable() ^ 1) != 0) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                Slog.d(TAG_FOCUS, "moveActivityStackToFront: unfocusable r=" + r);
            }
            return false;
        }
        TaskRecord task = r.getTask();
        ActivityStack stack = r.getStack();
        if (stack == null) {
            Slog.w(TAG, "moveActivityStackToFront: invalid task or stack: r=" + r + " task=" + task);
            return false;
        } else if (stack == this.mFocusedStack && stack.topRunningActivityLocked() == r) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                Slog.d(TAG_FOCUS, "moveActivityStackToFront: already on top, r=" + r);
            }
            return false;
        } else {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                Slog.d(TAG_FOCUS, "moveActivityStackToFront: r=" + r);
            }
            stack.moveToFront(reason, task);
            if (r.state == ActivityState.RESUMED && reason != null && reason.contains("setFocusedTask")) {
                this.mWindowManager.setFocusedApp(r.appToken, true);
                Slog.v(TAG, "oppo freeform setFocusedApp: " + r);
            }
            return true;
        }
    }

    void acquireAppLaunchPerfLock(String packageName) {
        if (this.mPerfPack == null) {
            this.mPerfPack = new BoostFramework();
        }
        if (this.mPerfPack != null) {
            this.mPerfPack.perfHint(4225, packageName, -1, 2);
        }
        if (this.mPerfBoost == null) {
            this.mPerfBoost = new BoostFramework();
        }
        if (this.mPerfBoost != null) {
            this.mPerfBoost.perfHint(4225, packageName, -1, 1);
        }
    }

    void acquireAppLaunch() {
        if (mHyp == null) {
            mHyp = Hypnus.getHypnus();
        }
        if (mHyp != null) {
            mHyp.hypnusSetAction(13, 5000);
        }
    }

    void startActivityBoost(String className) {
        if (className != null) {
            if (className.equals("com.tencent.mm.plugin.voip.ui.VideoActivity") || className.equals("com.tencent.av.ui.AVLoadingDialogActivity") || className.equals("com.tencent.mm.plugin.exdevice.ui.ExdeviceRankInfoUI") || className.equals("com.tencent.mm.plugin.webview.ui.tools.WebViewUI") || className.equals("com.jingdong.app.mall.pay.CashierDeskActivity") || className.equals("com.tmall.wireless.pay.TMPayActivity") || className.equals("com.alipay.android.app.pay.MiniLaucherActivity") || className.equals("com.alipay.android.app.ui.quickpay.window.MiniPayActivity") || className.equals("com.taobao.tao.alipay.cashdesk.CashDeskActivity")) {
                if (mHyp == null) {
                    mHyp = Hypnus.getHypnus();
                }
                if (mHyp != null) {
                    mHyp.hypnusSetAction(15, OppoBrightUtils.HIGH_BRIGHTNESS_LUX_STEP);
                }
            }
        }
    }

    ActivityRecord findTaskLocked(ActivityRecord r, int displayId) {
        this.mTmpFindTaskResult.r = null;
        this.mTmpFindTaskResult.matchedByRootAffinity = false;
        ActivityRecord affinityMatch = null;
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG_TASKS, "Looking for task of " + r);
        }
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                if (checkActivityBelongsInStack(r, stack)) {
                    stack.findTaskLocked(r, this.mTmpFindTaskResult);
                    if (this.mTmpFindTaskResult.r == null) {
                        continue;
                    } else if (!this.mTmpFindTaskResult.matchedByRootAffinity) {
                        if (this.mTmpFindTaskResult.r.state == ActivityState.DESTROYED) {
                            acquireAppLaunchPerfLock(r.packageName);
                        }
                        if (this.mTmpFindTaskResult.r.state == ActivityState.DESTROYED) {
                            acquireAppLaunch();
                        }
                        return this.mTmpFindTaskResult.r;
                    } else if (this.mTmpFindTaskResult.r.getDisplayId() == displayId) {
                        affinityMatch = this.mTmpFindTaskResult.r;
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG_TASKS, "Skipping stack: (mismatch activity/stack) " + stack);
                }
            }
        }
        if (this.mTmpFindTaskResult.r == null || this.mTmpFindTaskResult.r.state == ActivityState.DESTROYED) {
            acquireAppLaunchPerfLock(r.packageName);
        }
        acquireAppLaunch();
        if (ActivityManagerDebugConfig.DEBUG_TASKS && affinityMatch == null) {
            Slog.d(TAG_TASKS, "No task found");
        }
        return affinityMatch;
    }

    private boolean checkActivityBelongsInStack(ActivityRecord r, ActivityStack stack) {
        if (r.isHomeActivity()) {
            return stack.isHomeStack();
        }
        if (r.isRecentsActivity()) {
            return stack.isRecentsStack();
        }
        if (r.isAssistantActivity()) {
            return stack.isAssistantStack();
        }
        return true;
    }

    ActivityRecord findActivityLocked(Intent intent, ActivityInfo info, boolean compareIntentFilters) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord ar = ((ActivityStack) stacks.get(stackNdx)).findActivityLocked(intent, info, compareIntentFilters);
                if (ar != null) {
                    return ar;
                }
            }
        }
        return null;
    }

    ActivityRecord findActivityForFreeformLocked(Intent intent, int userId, boolean compareIntentFilters) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                int stackId = stack.mStackId;
                if (stackId == 1 || stackId == 2) {
                    ActivityRecord ar = stack.findActivityForFreeformLocked(intent, userId, compareIntentFilters);
                    if (ar != null) {
                        return ar;
                    }
                }
            }
        }
        return null;
    }

    TaskRecord findTaskForFreeformLocked(Intent intent, int userId) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                int stackId = stack.mStackId;
                Slog.v(TAG, "oppo freeform findTaskForFreeformLocked: stack " + stack);
                if (stackId == 1 || stackId == 2) {
                    Slog.v(TAG, "oppo freeform findTaskForFreeformLocked: stack test");
                    TaskRecord task = stack.findTaskForFreeformLocked(intent, userId);
                    if (task != null) {
                        return task;
                    }
                }
            }
        }
        return null;
    }

    boolean hasAwakeDisplay() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            if (!((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).shouldSleep()) {
                return true;
            }
        }
        return false;
    }

    void goingToSleepLocked() {
        scheduleSleepTimeout();
        if (!this.mGoingToSleep.isHeld()) {
            this.mGoingToSleep.acquire();
            if (this.mLaunchingActivity.isHeld()) {
                this.mLaunchingActivity.release();
                this.mService.mHandler.removeMessages(104);
            }
        }
        applySleepTokensLocked(false);
        checkReadyForSleepLocked(true);
    }

    void prepareForShutdownLocked() {
        for (int i = 0; i < this.mActivityDisplays.size(); i++) {
            createSleepTokenLocked("shutdown", this.mActivityDisplays.keyAt(i));
        }
    }

    boolean shutdownLocked(int timeout) {
        goingToSleepLocked();
        boolean timedout = false;
        long endTime = System.currentTimeMillis() + ((long) timeout);
        while (!putStacksToSleepLocked(true, true)) {
            long timeRemaining = endTime - System.currentTimeMillis();
            if (timeRemaining <= 0) {
                Slog.w(TAG, "Activity manager shutdown timed out");
                timedout = true;
                break;
            }
            try {
                this.mService.wait(timeRemaining);
            } catch (InterruptedException e) {
            }
        }
        checkReadyForSleepLocked(false);
        return timedout;
    }

    void comeOutOfSleepIfNeededLocked() {
        removeSleepTimeouts();
        if (this.mGoingToSleep.isHeld()) {
            this.mGoingToSleep.release();
        }
    }

    void applySleepTokensLocked(boolean applyToStacks) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            boolean displayShouldSleep = display.shouldSleep();
            if (displayShouldSleep != display.isSleeping()) {
                display.setIsSleeping(displayShouldSleep);
                if (applyToStacks) {
                    ArrayList<ActivityStack> stacks = display.mStacks;
                    for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                        ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                        if (displayShouldSleep) {
                            stack.goToSleepIfPossible(false);
                        } else {
                            stack.awakeFromSleepingLocked();
                            if (isFocusedStack(stack)) {
                                resumeFocusedStackTopActivityLocked();
                            }
                        }
                    }
                    if (!(displayShouldSleep || this.mGoingToSleepActivities.isEmpty())) {
                        Iterator<ActivityRecord> it = this.mGoingToSleepActivities.iterator();
                        while (it.hasNext()) {
                            if (((ActivityRecord) it.next()).getDisplayId() == display.mDisplayId) {
                                it.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    void activitySleptLocked(ActivityRecord r) {
        this.mGoingToSleepActivities.remove(r);
        if (this.mService.mIgnoreSleepCheckLater) {
            Slog.d(TAG, "activitySleptLocked, mIgnoreSleepCheckLater, do not care.");
            return;
        }
        ActivityStack s = r.getStack();
        if (s != null) {
            s.checkReadyForSleep();
        } else {
            checkReadyForSleepLocked(true);
        }
    }

    void checkReadyForSleepWhenResumeLocked(ComponentName currResumeActCompName) {
        if (this.mService.mIgnoreSleepCheckLater) {
            Slog.d(TAG, "checkReadyForSleepWhenResumeLocked, mIgnoreSleepCheckLater, do not care. " + Debug.getCallers(4));
        } else {
            checkReadyForSleepLocked(true);
        }
    }

    void checkReadyForSleepLocked(boolean allowDelay) {
        if (this.mService.isSleepingOrShuttingDownLocked() && putStacksToSleepLocked(allowDelay, false)) {
            this.mService.mActivityStarter.sendPowerHintForLaunchEndIfNeeded();
            removeSleepTimeouts();
            if (this.mGoingToSleep.isHeld()) {
                this.mGoingToSleep.release();
            }
            if (this.mService.mShuttingDown) {
                this.mService.notifyAll();
            }
        }
    }

    private boolean putStacksToSleepLocked(boolean allowDelay, boolean shuttingDown) {
        boolean allSleep = true;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                if (allowDelay) {
                    allSleep &= ((ActivityStack) stacks.get(stackNdx)).goToSleepIfPossible(shuttingDown);
                } else {
                    ((ActivityStack) stacks.get(stackNdx)).goToSleep();
                }
            }
        }
        return allSleep;
    }

    boolean reportResumedActivityLocked(ActivityRecord r) {
        this.mStoppingActivities.remove(r);
        if (isFocusedStack(r.getStack())) {
            this.mService.updateUsageStats(r, true);
        }
        if (!allResumedActivitiesComplete()) {
            return false;
        }
        ensureActivitiesVisibleLocked(null, 0, false);
        this.mWindowManager.executeAppTransition();
        return true;
    }

    void handleAppCrashLocked(ProcessRecord app) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ((ActivityStack) stacks.get(stackNdx)).handleAppCrashLocked(app);
            }
        }
    }

    private void handleLaunchTaskBehindCompleteLocked(ActivityRecord r) {
        TaskRecord task = r.getTask();
        ActivityStack stack = task.getStack();
        r.mLaunchTaskBehind = false;
        task.setLastThumbnailLocked(r.screenshotActivityLocked());
        this.mRecentTasks.addLocked(task);
        this.mService.mTaskChangeNotificationController.notifyTaskStackChanged();
        r.setVisibility(false);
        ActivityRecord top = stack.topActivity();
        if (top != null) {
            top.getTask().touchActiveTime();
        }
    }

    void scheduleLaunchTaskBehindComplete(IBinder token) {
        this.mHandler.obtainMessage(112, token).sendToTarget();
    }

    void ensureActivitiesVisibleLocked(ActivityRecord starting, int configChanges, boolean preserveWindows) {
        this.mKeyguardController.beginActivityVisibilityUpdate();
        try {
            for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
                ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
                for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                    ((ActivityStack) stacks.get(stackNdx)).ensureActivitiesVisibleLocked(starting, configChanges, preserveWindows);
                }
            }
        } finally {
            this.mKeyguardController.endActivityVisibilityUpdate();
        }
    }

    void addStartingWindowsForVisibleActivities(boolean taskSwitch) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ((ActivityStack) stacks.get(stackNdx)).addStartingWindowsForVisibleActivities(taskSwitch);
            }
        }
    }

    void invalidateTaskLayers() {
        this.mTaskLayersChanged = true;
    }

    void rankTaskLayersIfNeeded() {
        if (this.mTaskLayersChanged) {
            this.mTaskLayersChanged = false;
            for (int displayNdx = 0; displayNdx < this.mActivityDisplays.size(); displayNdx++) {
                ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
                int baseLayer = 0;
                for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                    baseLayer += ((ActivityStack) stacks.get(stackNdx)).rankTaskLayers(baseLayer);
                }
            }
        }
    }

    void clearOtherAppTimeTrackers(AppTimeTracker except) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ((ActivityStack) stacks.get(stackNdx)).clearOtherAppTimeTrackers(except);
            }
        }
    }

    void scheduleDestroyAllActivities(ProcessRecord app, String reason) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            int numStacks = stacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ((ActivityStack) stacks.get(stackNdx)).scheduleDestroyActivities(app, reason);
            }
        }
    }

    void releaseSomeActivitiesLocked(ProcessRecord app, String reason) {
        TaskRecord firstTask = null;
        ArraySet tasks = null;
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            Slog.d(TAG_RELEASE, "Trying to release some activities in " + app);
        }
        for (int i = 0; i < app.activities.size(); i++) {
            ActivityRecord r = (ActivityRecord) app.activities.get(i);
            if (r.finishing || r.state == ActivityState.DESTROYING || r.state == ActivityState.DESTROYED) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Abort release; already destroying: " + r);
                }
                return;
            }
            if (!r.visible && (r.stopped ^ 1) == 0 && (r.haveState ^ 1) == 0 && r.state != ActivityState.RESUMED && r.state != ActivityState.PAUSING && r.state != ActivityState.PAUSED && r.state != ActivityState.STOPPING) {
                TaskRecord task = r.getTask();
                if (task != null) {
                    if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                        Slog.d(TAG_RELEASE, "Collecting release task " + task + " from " + r);
                    }
                    if (firstTask == null) {
                        firstTask = task;
                    } else if (firstTask != task) {
                        if (tasks == null) {
                            tasks = new ArraySet();
                            tasks.add(firstTask);
                        }
                        tasks.add(task);
                    }
                }
            } else if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                Slog.d(TAG_RELEASE, "Not releasing in-use activity: " + r);
            }
        }
        if (tasks == null) {
            if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                Slog.d(TAG_RELEASE, "Didn't find two or more tasks to release");
            }
            return;
        }
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            int stackNdx = 0;
            while (stackNdx < stacks.size()) {
                if (((ActivityStack) stacks.get(stackNdx)).releaseSomeActivitiesLocked(app, tasks, reason) <= 0) {
                    stackNdx++;
                } else {
                    return;
                }
            }
        }
    }

    boolean switchUserLocked(int userId, UserState uss) {
        boolean z;
        ActivityStack stack;
        int focusStackId = this.mFocusedStack.getStackId();
        if (focusStackId == 3) {
            z = true;
        } else {
            z = false;
        }
        moveTasksToFullscreenStackLocked(3, z);
        removeStackLocked(4);
        this.mUserStackInFront.put(this.mCurrentUser, focusStackId);
        int restoreStackId = this.mUserStackInFront.get(userId, 0);
        this.mCurrentUser = userId;
        this.mStartingUsers.add(uss);
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                stack = (ActivityStack) stacks.get(stackNdx);
                stack.switchUserLocked(userId);
                TaskRecord task = stack.topTask();
                if (task != null) {
                    stack.positionChildWindowContainerAtTop(task);
                }
            }
        }
        stack = getStack(restoreStackId);
        if (stack == null) {
            stack = this.mHomeStack;
        }
        boolean homeInFront = stack.isHomeStack();
        if (stack.isOnHomeDisplay()) {
            stack.moveToFront("switchUserOnHomeDisplay");
        } else {
            resumeHomeStackTask(null, "switchUserOnOtherDisplay");
        }
        return homeInFront;
    }

    boolean isCurrentProfileLocked(int userId) {
        if (userId == this.mCurrentUser || OppoMultiAppManager.getInstance().isCurrentProfile(userId)) {
            return true;
        }
        return this.mService.mUserController.isCurrentProfileLocked(userId);
    }

    boolean isStoppingNoHistoryActivity() {
        for (ActivityRecord record : this.mStoppingActivities) {
            if (record.isNoHistory()) {
                return true;
            }
        }
        return false;
    }

    final ArrayList<ActivityRecord> processStoppingActivitiesLocked(ActivityRecord idleActivity, boolean remove, boolean processPausingActivities) {
        ArrayList stops = null;
        boolean nowVisible = allResumedActivitiesVisible();
        for (int activityNdx = this.mStoppingActivities.size() - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord s = (ActivityRecord) this.mStoppingActivities.get(activityNdx);
            boolean waitingVisible = this.mActivitiesWaitingForVisibleActivity.contains(s);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG, "Stopping " + s + ": nowVisible=" + nowVisible + " waitingVisible=" + waitingVisible + " finishing=" + s.finishing);
            }
            if (waitingVisible && nowVisible) {
                this.mActivitiesWaitingForVisibleActivity.remove(s);
                waitingVisible = false;
                if (s.finishing) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.v(TAG, "Before stopping, can hide: " + s);
                    }
                    s.setVisibility(false);
                }
            }
            if (remove && (s.mShouldPause ^ 1) != 0) {
                ActivityStack stack = s.getStack();
                boolean shouldSleepOrShutDown;
                if (stack != null) {
                    shouldSleepOrShutDown = stack.shouldSleepOrShutDownActivities();
                } else {
                    shouldSleepOrShutDown = this.mService.isSleepingOrShuttingDownLocked();
                }
                if (!waitingVisible || shouldSleepOrShutDown) {
                    if (processPausingActivities || s.state != ActivityState.PAUSING) {
                        if (ActivityManagerDebugConfig.DEBUG_STATES) {
                            Slog.v(TAG, "Ready to stop: " + s);
                        }
                        if (stops == null) {
                            stops = new ArrayList();
                        }
                        stops.add(s);
                        this.mStoppingActivities.remove(activityNdx);
                    } else {
                        removeTimeoutsForActivityLocked(idleActivity);
                        scheduleIdleTimeoutLocked(idleActivity);
                    }
                }
            }
            if (s != null) {
                s.mShouldPause = false;
            }
        }
        return stops;
    }

    void validateTopActivitiesLocked() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                ActivityRecord r = stack.topRunningActivityLocked();
                ActivityState state = r == null ? ActivityState.DESTROYED : r.state;
                if (!isFocusedStack(stack)) {
                    ActivityRecord resumed = stack.mResumedActivity;
                    if (resumed != null && resumed == r) {
                        Slog.e(TAG, "validateTop...: back stack has resumed activity r=" + r + " state=" + state);
                    }
                    if (r != null && (state == ActivityState.INITIALIZING || state == ActivityState.RESUMED)) {
                        Slog.e(TAG, "validateTop...: activity in back resumed r=" + r + " state=" + state);
                    }
                } else if (r == null) {
                    Slog.e(TAG, "validateTop...: null top activity, stack=" + stack);
                } else {
                    ActivityRecord pausing = stack.mPausingActivity;
                    if (pausing != null && pausing == r) {
                        Slog.e(TAG, "validateTop...: top stack has pausing activity r=" + r + " state=" + state);
                    }
                    if (!(state == ActivityState.INITIALIZING || state == ActivityState.RESUMED)) {
                        Slog.e(TAG, "validateTop...: activity in front not resumed r=" + r + " state=" + state);
                    }
                }
            }
        }
    }

    private String lockTaskModeToString() {
        switch (this.mLockTaskModeState) {
            case 0:
                return "NONE";
            case 1:
                return "LOCKED";
            case 2:
                return "PINNED";
            default:
                return "unknown=" + this.mLockTaskModeState;
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        int i;
        pw.print(prefix);
        pw.print("mFocusedStack=" + this.mFocusedStack);
        pw.print(" mLastFocusedStack=");
        pw.println(this.mLastFocusedStack);
        pw.print(prefix);
        pw.println("mCurTaskIdForUser=" + this.mCurTaskIdForUser);
        pw.print(prefix);
        pw.println("mUserStackInFront=" + this.mUserStackInFront);
        pw.print(prefix);
        pw.println("mStacks=" + this.mStacks);
        pw.print(prefix);
        pw.print("mLockTaskModeState=" + lockTaskModeToString());
        SparseArray<String[]> packages = this.mService.mLockTaskPackages;
        if (packages.size() > 0) {
            pw.print(prefix);
            pw.println("mLockTaskPackages (userId:packages)=");
            for (i = 0; i < packages.size(); i++) {
                pw.print(prefix);
                pw.print(prefix);
                pw.print(packages.keyAt(i));
                pw.print(":");
                pw.println(Arrays.toString((Object[]) packages.valueAt(i)));
            }
        }
        if (!this.mWaitingForActivityVisible.isEmpty()) {
            pw.print(prefix);
            pw.println("mWaitingForActivityVisible=");
            for (i = 0; i < this.mWaitingForActivityVisible.size(); i++) {
                pw.print(prefix);
                pw.print(prefix);
                ((WaitInfo) this.mWaitingForActivityVisible.get(i)).dump(pw, prefix);
            }
        }
        pw.println(" mLockTaskModeTasks" + this.mLockTaskModeTasks);
        this.mKeyguardController.dump(pw, prefix);
    }

    void dumpDisplayConfigs(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("Display override configurations:");
        int displayCount = this.mActivityDisplays.size();
        for (int i = 0; i < displayCount; i++) {
            ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.valueAt(i);
            pw.print(prefix);
            pw.print("  ");
            pw.print(activityDisplay.mDisplayId);
            pw.print(": ");
            pw.println(activityDisplay.getOverrideConfiguration());
        }
    }

    ArrayList<ActivityRecord> getDumpActivitiesLocked(String name, boolean dumpVisibleStacksOnly, boolean dumpFocusedStackOnly) {
        if (dumpFocusedStackOnly) {
            return this.mFocusedStack.getDumpActivitiesLocked(name);
        }
        ArrayList<ActivityRecord> activities = new ArrayList();
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                if (!dumpVisibleStacksOnly || stack.shouldBeVisible(null) == 1) {
                    activities.addAll(stack.getDumpActivitiesLocked(name));
                }
            }
        }
        return activities;
    }

    static boolean printThisActivity(PrintWriter pw, ActivityRecord activity, String dumpPackage, boolean needSep, String prefix) {
        if (activity == null || (dumpPackage != null && !dumpPackage.equals(activity.packageName))) {
            return false;
        }
        if (needSep) {
            pw.println();
        }
        pw.print(prefix);
        pw.println(activity);
        return true;
    }

    boolean dumpActivitiesLocked(FileDescriptor fd, PrintWriter pw, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        boolean printed = false;
        boolean needSep = false;
        for (int displayNdx = 0; displayNdx < this.mActivityDisplays.size(); displayNdx++) {
            ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            pw.print("Display #");
            pw.print(activityDisplay.mDisplayId);
            pw.println(" (activities from top to bottom):");
            ArrayList<ActivityStack> stacks = activityDisplay.mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
                StringBuilder stringBuilder = new StringBuilder(128);
                stringBuilder.append("  Stack #");
                stringBuilder.append(stack.mStackId);
                stringBuilder.append(":");
                stringBuilder.append("\n");
                stringBuilder.append("  mFullscreen=").append(stack.mFullscreen);
                stringBuilder.append("\n");
                stringBuilder.append("  isSleeping=").append(stack.shouldSleepActivities());
                stringBuilder.append("\n");
                stringBuilder.append("  mBounds=").append(stack.mBounds);
                boolean printedStackHeader = stack.dumpActivitiesLocked(fd, pw, dumpAll, dumpClient, dumpPackage, needSep, stringBuilder.toString());
                printed |= printedStackHeader;
                if (!printedStackHeader) {
                    pw.println();
                    pw.println(stringBuilder);
                }
                printed |= dumpHistoryList(fd, pw, stack.mLRUActivities, "    ", "Run", false, dumpAll ^ 1, false, dumpPackage, true, "    Running activities (most recent first):", null);
                needSep = printed;
                if (printThisActivity(pw, stack.mPausingActivity, dumpPackage, printed, "    mPausingActivity: ")) {
                    printed = true;
                    needSep = false;
                }
                if (printThisActivity(pw, stack.mResumedActivity, dumpPackage, needSep, "    mResumedActivity: ")) {
                    printed = true;
                    needSep = false;
                }
                if (dumpAll) {
                    int printed2;
                    if (printThisActivity(pw, stack.mLastPausedActivity, dumpPackage, needSep, "    mLastPausedActivity: ")) {
                        printed2 = 1;
                        needSep = true;
                    }
                    printed = printed2 | printThisActivity(pw, stack.mLastNoHistoryActivity, dumpPackage, needSep, "    mLastNoHistoryActivity: ");
                }
                needSep = printed;
            }
        }
        return (((printed | dumpHistoryList(fd, pw, this.mFinishingActivities, "  ", "Fin", false, dumpAll ^ 1, false, dumpPackage, true, "  Activities waiting to finish:", null)) | dumpHistoryList(fd, pw, this.mStoppingActivities, "  ", "Stop", false, dumpAll ^ 1, false, dumpPackage, true, "  Activities waiting to stop:", null)) | dumpHistoryList(fd, pw, this.mActivitiesWaitingForVisibleActivity, "  ", "Wait", false, dumpAll ^ 1, false, dumpPackage, true, "  Activities waiting for another to become visible:", null)) | dumpHistoryList(fd, pw, this.mGoingToSleepActivities, "  ", "Sleep", false, dumpAll ^ 1, false, dumpPackage, true, "  Activities waiting to sleep:", null);
    }

    static boolean dumpHistoryList(FileDescriptor fd, PrintWriter pw, List<ActivityRecord> list, String prefix, String label, boolean complete, boolean brief, boolean client, String dumpPackage, boolean needNL, String header1, String header2) {
        TaskRecord lastTask = null;
        String innerPrefix = null;
        String[] args = null;
        boolean printed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            ActivityRecord r = (ActivityRecord) list.get(i);
            if (dumpPackage != null) {
                if ((dumpPackage.equals(r.packageName) ^ 1) != 0) {
                    continue;
                }
            }
            if (innerPrefix == null) {
                innerPrefix = prefix + "      ";
                args = new String[0];
            }
            printed = true;
            int full = !brief ? !complete ? r.isInHistory() ^ 1 : 1 : 0;
            if (needNL) {
                pw.println("");
                needNL = false;
            }
            if (header1 != null) {
                pw.println(header1);
                header1 = null;
            }
            if (header2 != null) {
                pw.println(header2);
                header2 = null;
            }
            if (lastTask != r.getTask()) {
                lastTask = r.getTask();
                pw.print(prefix);
                pw.print(full != 0 ? "* " : "  ");
                pw.println(lastTask);
                if (full != 0) {
                    lastTask.dump(pw, prefix + "  ");
                } else if (complete && lastTask.intent != null) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.println(lastTask.intent.toInsecureStringWithClip());
                }
            }
            pw.print(prefix);
            pw.print(full != 0 ? "  * " : "    ");
            pw.print(label);
            pw.print(" #");
            pw.print(i);
            pw.print(": ");
            pw.println(r);
            if (full != 0) {
                r.dump(pw, innerPrefix);
            } else if (complete) {
                pw.print(innerPrefix);
                pw.println(r.intent.toInsecureString());
                if (r.app != null) {
                    pw.print(innerPrefix);
                    pw.println(r.app);
                }
            }
            if (!(!client || r.app == null || r.app.thread == null)) {
                pw.flush();
                TransferPipe tp;
                try {
                    tp = new TransferPipe();
                    r.app.thread.dumpActivity(tp.getWriteFd(), r.appToken, innerPrefix, args);
                    tp.go(fd, 2000);
                    tp.kill();
                } catch (IOException e) {
                    pw.println(innerPrefix + "Failure while dumping the activity: " + e);
                } catch (RemoteException e2) {
                    pw.println(innerPrefix + "Got a RemoteException while dumping the activity");
                } catch (Throwable th) {
                    tp.kill();
                }
                needNL = true;
            }
        }
        return printed;
    }

    void scheduleIdleTimeoutLocked(ActivityRecord next) {
        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
            Slog.d(TAG_IDLE, "scheduleIdleTimeoutLocked: Callers=" + Debug.getCallers(4));
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100, next), 10000);
    }

    final void scheduleIdleLocked() {
        this.mHandler.sendEmptyMessage(101);
    }

    void removeTimeoutsForActivityLocked(ActivityRecord r) {
        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
            Slog.d(TAG_IDLE, "removeTimeoutsForActivity: Callers=" + Debug.getCallers(4));
        }
        this.mHandler.removeMessages(100, r);
    }

    final void scheduleResumeTopActivities() {
        if (!this.mHandler.hasMessages(102)) {
            this.mHandler.sendEmptyMessage(102);
        }
    }

    void removeSleepTimeouts() {
        this.mHandler.removeMessages(103);
    }

    final void scheduleSleepTimeout() {
        removeSleepTimeouts();
        this.mHandler.sendEmptyMessageDelayed(103, FaceDaemonWrapper.TIMEOUT_FACED_BINDERCALL_CHECK);
    }

    public void onDisplayAdded(int displayId) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG, "Display added displayId=" + displayId);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(105, displayId, 0));
    }

    public void onDisplayRemoved(int displayId) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG, "Display removed displayId=" + displayId);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(107, displayId, 0));
    }

    public void onDisplayChanged(int displayId) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG, "Display changed displayId=" + displayId);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(106, displayId, 0));
    }

    private void handleDisplayAdded(int displayId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                getActivityDisplayOrCreateLocked(displayId);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    boolean isDisplayAdded(int displayId) {
        return getActivityDisplayOrCreateLocked(displayId) != null;
    }

    ActivityDisplay getActivityDisplay(int displayId) {
        return (ActivityDisplay) this.mActivityDisplays.get(displayId);
    }

    private ActivityDisplay getActivityDisplayOrCreateLocked(int displayId) {
        ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.get(displayId);
        if (activityDisplay != null) {
            return activityDisplay;
        }
        if (this.mDisplayManager == null || this.mDisplayManager.getDisplay(displayId) == null) {
            return null;
        }
        activityDisplay = new ActivityDisplay(displayId);
        if (activityDisplay.mDisplay == null) {
            Slog.w(TAG, "Display " + displayId + " gone before initialization complete");
            return null;
        }
        this.mActivityDisplays.put(displayId, activityDisplay);
        calculateDefaultMinimalSizeOfResizeableTasks(activityDisplay);
        this.mWindowManager.onDisplayAdded(displayId);
        return activityDisplay;
    }

    private void calculateDefaultMinimalSizeOfResizeableTasks(ActivityDisplay display) {
        this.mDefaultMinSizeOfResizeableTask = this.mService.mContext.getResources().getDimensionPixelSize(17105003);
    }

    private void handleDisplayRemoved(int displayId) {
        if (displayId == 0) {
            throw new IllegalArgumentException("Can't remove the primary display.");
        }
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.get(displayId);
                if (activityDisplay != null) {
                    boolean destroyContentOnRemoval = activityDisplay.shouldDestroyContentOnRemove();
                    ArrayList<ActivityStack> stacks = activityDisplay.mStacks;
                    while (!stacks.isEmpty()) {
                        ActivityStack stack = (ActivityStack) stacks.get(0);
                        if (destroyContentOnRemoval) {
                            moveStackToDisplayLocked(stack.mStackId, 0, false);
                            stack.finishAllActivitiesLocked(true);
                        } else {
                            moveTasksToFullscreenStackLocked(stack.getStackId(), true);
                        }
                    }
                    releaseSleepTokens(activityDisplay);
                    this.mActivityDisplays.remove(displayId);
                    this.mWindowManager.onDisplayRemoved(displayId);
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private void handleDisplayChanged(int displayId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.get(displayId);
                if (!(activityDisplay == null || displayId == 0)) {
                    int displayState = activityDisplay.mDisplay.getState();
                    if (displayState == 1 && activityDisplay.mOffToken == null) {
                        activityDisplay.mOffToken = this.mService.acquireSleepToken("Display-off", displayId);
                    } else if (displayState == 2) {
                        if (activityDisplay.mOffToken != null) {
                            activityDisplay.mOffToken.release();
                            activityDisplay.mOffToken = null;
                        }
                    }
                }
                this.mWindowManager.onDisplayChanged(displayId);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    SleepToken createSleepTokenLocked(String tag, int displayId) {
        ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.get(displayId);
        if (display == null) {
            throw new IllegalArgumentException("Invalid display: " + displayId);
        }
        SleepTokenImpl token = new SleepTokenImpl(tag, displayId);
        this.mSleepTokens.add(token);
        display.mAllSleepTokens.add(token);
        return token;
    }

    private void removeSleepTokenLocked(SleepTokenImpl token) {
        this.mSleepTokens.remove(token);
        ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.get(token.mDisplayId);
        if (display != null) {
            display.mAllSleepTokens.remove(token);
            if (display.mAllSleepTokens.isEmpty()) {
                this.mService.updateSleepIfNeededLocked();
            }
        }
    }

    private void releaseSleepTokens(ActivityDisplay display) {
        if (!display.mAllSleepTokens.isEmpty()) {
            for (SleepTokenImpl token : display.mAllSleepTokens) {
                this.mSleepTokens.remove(token);
            }
            display.mAllSleepTokens.clear();
            this.mService.updateSleepIfNeededLocked();
        }
    }

    private StackInfo getStackInfoLocked(ActivityStack stack) {
        int indexOf;
        int displayId = stack.mDisplayId;
        ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.get(displayId);
        StackInfo info = new StackInfo();
        stack.getWindowContainerBounds(info.bounds);
        info.displayId = displayId;
        info.stackId = stack.mStackId;
        info.userId = stack.mCurrentUser;
        info.visible = stack.shouldBeVisible(null) == 1;
        if (display != null) {
            indexOf = display.mStacks.indexOf(stack);
        } else {
            indexOf = 0;
        }
        info.position = indexOf;
        ArrayList<TaskRecord> tasks = stack.getAllTasks();
        int numTasks = tasks.size();
        int[] taskIds = new int[numTasks];
        String[] taskNames = new String[numTasks];
        Rect[] taskBounds = new Rect[numTasks];
        int[] taskUserIds = new int[numTasks];
        for (int i = 0; i < numTasks; i++) {
            String flattenToString;
            TaskRecord task = (TaskRecord) tasks.get(i);
            taskIds[i] = task.taskId;
            if (task.origActivity != null) {
                flattenToString = task.origActivity.flattenToString();
            } else if (task.realActivity != null) {
                flattenToString = task.realActivity.flattenToString();
            } else if (task.getTopActivity() != null) {
                flattenToString = task.getTopActivity().packageName;
            } else {
                flattenToString = Shell.NIGHT_MODE_STR_UNKNOWN;
            }
            taskNames[i] = flattenToString;
            taskBounds[i] = new Rect();
            task.getWindowContainerBounds(taskBounds[i]);
            taskUserIds[i] = task.userId;
        }
        info.taskIds = taskIds;
        info.taskNames = taskNames;
        info.taskBounds = taskBounds;
        info.taskUserIds = taskUserIds;
        ActivityRecord top = stack.topRunningActivityLocked();
        info.topActivity = top != null ? top.intent.getComponent() : null;
        return info;
    }

    StackInfo getStackInfoLocked(int stackId) {
        ActivityStack stack = getStack(stackId);
        if (stack != null) {
            return getStackInfoLocked(stack);
        }
        return null;
    }

    ArrayList<StackInfo> getAllStackInfosLocked() {
        ArrayList<StackInfo> list = new ArrayList();
        for (int displayNdx = 0; displayNdx < this.mActivityDisplays.size(); displayNdx++) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int ndx = stacks.size() - 1; ndx >= 0; ndx--) {
                list.add(getStackInfoLocked((ActivityStack) stacks.get(ndx)));
            }
        }
        return list;
    }

    TaskRecord getLockedTaskLocked() {
        int top = this.mLockTaskModeTasks.size() - 1;
        if (top >= 0) {
            return (TaskRecord) this.mLockTaskModeTasks.get(top);
        }
        return null;
    }

    boolean isLockedTask(TaskRecord task) {
        return this.mLockTaskModeTasks.contains(task);
    }

    boolean isLastLockedTask(TaskRecord task) {
        return this.mLockTaskModeTasks.size() == 1 ? this.mLockTaskModeTasks.contains(task) : false;
    }

    void removeLockedTaskLocked(TaskRecord task) {
        if (this.mLockTaskModeTasks.remove(task)) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.w(TAG_LOCKTASK, "removeLockedTaskLocked: removed " + task);
            }
            if (this.mLockTaskModeTasks.isEmpty()) {
                if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                    Slog.d(TAG_LOCKTASK, "removeLockedTask: task=" + task + " last task, reverting locktask mode. Callers=" + Debug.getCallers(3));
                }
                Message lockTaskMsg = Message.obtain();
                lockTaskMsg.arg1 = task.userId;
                lockTaskMsg.what = 110;
                this.mHandler.sendMessage(lockTaskMsg);
            }
        }
    }

    void handleNonResizableTaskIfNeeded(TaskRecord task, int preferredStackId, int preferredDisplayId, int actualStackId) {
        handleNonResizableTaskIfNeeded(task, preferredStackId, preferredDisplayId, actualStackId, false);
    }

    private void handleNonResizableTaskIfNeeded(TaskRecord task, int preferredStackId, int preferredDisplayId, int actualStackId, boolean forceNonResizable) {
        int isSecondaryDisplayPreferred;
        if (preferredDisplayId == 0 || preferredDisplayId == -1) {
            isSecondaryDisplayPreferred = StackId.isDynamicStack(preferredStackId);
        } else {
            isSecondaryDisplayPreferred = 1;
        }
        if ((isStackDockedInEffect(actualStackId) || preferredStackId == 3 || (isSecondaryDisplayPreferred ^ 1) == 0) && !task.isHomeTask()) {
            boolean launchOnSecondaryDisplayFailed;
            if (isSecondaryDisplayPreferred != 0) {
                int actualDisplayId = task.getStack().mDisplayId;
                if (task.canBeLaunchedOnDisplay(actualDisplayId)) {
                    launchOnSecondaryDisplayFailed = actualDisplayId != 0 ? preferredDisplayId != -1 ? preferredDisplayId != actualDisplayId : false : true;
                } else {
                    this.mService.moveTaskToStack(task.taskId, 1, true);
                    launchOnSecondaryDisplayFailed = true;
                }
            } else {
                launchOnSecondaryDisplayFailed = false;
            }
            ActivityRecord topActivity = task.getTopActivity();
            if (!(topActivity == null || topActivity.shortComponentName == null || !topActivity.shortComponentName.contains(OPPO_SAFECENTER_PASSWORD) || getStack(3) == null)) {
                if (actualStackId == 1) {
                    resizeStackLocked(actualStackId, null, null, null, true, true, true);
                    return;
                } else if (actualStackId == 3) {
                    resizeDockedStackLocked(null, null, null, null, null, true, true);
                    return;
                }
            }
            if (task.supportsSplitScreen() && actualStackId == 1 && task != null) {
                ActivityStack stack = getStack(3, false, false);
                if (!(stack == null || (stack.mFullscreen ^ 1) == 0 || !task.mFullscreen)) {
                    Rect resetRect = new Rect();
                    Rect tmpRect = new Rect();
                    ActivityStack homeStack = getStack(0, false, false);
                    if (homeStack != null) {
                        homeStack.getStackDockedModeBounds(null, resetRect, tmpRect, true);
                        resizeDockedStackLocked(stack.mBounds, null, null, null, null, true, true);
                        return;
                    }
                }
            }
            if (launchOnSecondaryDisplayFailed || (task.supportsSplitScreen() ^ 1) != 0 || forceNonResizable) {
                boolean z;
                if (launchOnSecondaryDisplayFailed) {
                    this.mService.mTaskChangeNotificationController.notifyActivityLaunchOnSecondaryDisplayFailed();
                } else {
                    this.mService.mTaskChangeNotificationController.notifyActivityDismissingDockedStack();
                }
                if (actualStackId == 3) {
                    z = true;
                } else {
                    z = false;
                }
                moveTasksToFullscreenStackLocked(3, z);
            } else if (!(topActivity == null || !topActivity.isNonResizableOrForcedResizable() || (topActivity.noDisplay ^ 1) == 0 || task.affinity == null || !task.affinity.equals(topActivity.taskAffinity))) {
                int reason;
                String packageName = topActivity.appInfo.packageName;
                if (isSecondaryDisplayPreferred != 0) {
                    reason = 2;
                } else {
                    reason = 1;
                }
                this.mService.mTaskChangeNotificationController.notifyActivityForcedResizable(task.taskId, reason, packageName);
            }
        }
    }

    void showLockTaskToast() {
        if (this.mLockTaskNotify != null) {
            this.mLockTaskNotify.showToast(this.mLockTaskModeState);
        }
    }

    void showLockTaskEscapeMessageLocked(TaskRecord task) {
        if (this.mLockTaskModeTasks.contains(task)) {
            this.mHandler.sendEmptyMessage(113);
        }
    }

    void setLockTaskModeLocked(TaskRecord task, int lockTaskModeState, String reason, boolean andResume) {
        if (task == null) {
            TaskRecord lockedTask = getLockedTaskLocked();
            if (lockedTask != null) {
                removeLockedTaskLocked(lockedTask);
                if (!this.mLockTaskModeTasks.isEmpty()) {
                    if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                        Slog.w(TAG_LOCKTASK, "setLockTaskModeLocked: Tasks remaining, can't unlock");
                    }
                    lockedTask.performClearTaskLocked();
                    resumeFocusedStackTopActivityLocked();
                    return;
                }
            }
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.w(TAG_LOCKTASK, "setLockTaskModeLocked: No tasks to unlock. Callers=" + Debug.getCallers(4));
            }
        } else if (task.mLockTaskAuth == 0) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.w(TAG_LOCKTASK, "setLockTaskModeLocked: Can't lock due to auth");
            }
        } else if (isLockTaskModeViolation(task)) {
            Slog.e(TAG_LOCKTASK, "setLockTaskMode: Attempt to start an unauthorized lock task.");
        } else {
            if (this.mLockTaskModeTasks.isEmpty()) {
                Message lockTaskMsg = Message.obtain();
                lockTaskMsg.obj = task.intent.getComponent().getPackageName();
                lockTaskMsg.arg1 = task.userId;
                lockTaskMsg.what = 109;
                lockTaskMsg.arg2 = lockTaskModeState;
                this.mHandler.sendMessage(lockTaskMsg);
            }
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.w(TAG_LOCKTASK, "setLockTaskModeLocked: Locking to " + task + " Callers=" + Debug.getCallers(4));
            }
            this.mLockTaskModeTasks.remove(task);
            this.mLockTaskModeTasks.add(task);
            if (task.mLockTaskUid == -1) {
                task.mLockTaskUid = task.effectiveUid;
            }
            if (andResume) {
                findTaskToMoveToFrontLocked(task, 0, null, reason, lockTaskModeState != 0);
                resumeFocusedStackTopActivityLocked();
                this.mWindowManager.executeAppTransition();
            } else if (lockTaskModeState != 0) {
                handleNonResizableTaskIfNeeded(task, -1, 0, task.getStackId(), true);
            }
        }
    }

    boolean isLockTaskModeViolation(TaskRecord task) {
        return isLockTaskModeViolation(task, false);
    }

    boolean isLockTaskModeViolation(TaskRecord task, boolean isNewClearTask) {
        if (getLockedTaskLocked() == task && (isNewClearTask ^ 1) != 0) {
            return false;
        }
        int lockTaskAuth = task.mLockTaskAuth;
        if (lockTaskAuth == 1 && task.intent != null && "android.intent.action.CHOOSER".equals(task.intent.getAction()) && "coloros_multiapp_chooser".equals(task.affinity)) {
            return false;
        }
        switch (lockTaskAuth) {
            case 0:
                return this.mLockTaskModeTasks.isEmpty() ^ 1;
            case 1:
                return this.mLockTaskModeTasks.isEmpty() ^ 1;
            case 2:
            case 3:
            case 4:
                return false;
            default:
                Slog.w(TAG, "isLockTaskModeViolation: invalid lockTaskAuth value=" + lockTaskAuth);
                return true;
        }
    }

    void onLockTaskPackagesUpdatedLocked() {
        boolean didSomething = false;
        for (int taskNdx = this.mLockTaskModeTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord lockedTask = (TaskRecord) this.mLockTaskModeTasks.get(taskNdx);
            boolean wasWhitelisted = lockedTask.mLockTaskAuth != 2 ? lockedTask.mLockTaskAuth == 3 : true;
            lockedTask.setLockTaskAuth();
            boolean isWhitelisted = lockedTask.mLockTaskAuth != 2 ? lockedTask.mLockTaskAuth == 3 : true;
            if (wasWhitelisted && (isWhitelisted ^ 1) != 0) {
                if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                    Slog.d(TAG_LOCKTASK, "onLockTaskPackagesUpdated: removing " + lockedTask + " mLockTaskAuth=" + lockedTask.lockTaskAuthToString());
                }
                removeLockedTaskLocked(lockedTask);
                lockedTask.performClearTaskLocked();
                didSomething = true;
            }
        }
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ((ActivityStack) stacks.get(stackNdx)).onLockTaskPackagesUpdatedLocked();
            }
        }
        ActivityRecord r = topRunningActivityLocked();
        TaskRecord task = r != null ? r.getTask() : null;
        if (this.mLockTaskModeTasks.isEmpty() && task != null && task.mLockTaskAuth == 2) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.d(TAG_LOCKTASK, "onLockTaskPackagesUpdated: starting new locktask task=" + task);
            }
            setLockTaskModeLocked(task, 1, "package updated", false);
            didSomething = true;
        }
        if (didSomething) {
            resumeFocusedStackTopActivityLocked();
        }
    }

    int getLockTaskModeState() {
        return this.mLockTaskModeState;
    }

    void activityRelaunchedLocked(IBinder token) {
        this.mWindowManager.notifyAppRelaunchingFinished(token);
        ActivityRecord r = ActivityRecord.isInStackLocked(token);
        if (r != null && r.getStack().shouldSleepOrShutDownActivities()) {
            r.setSleeping(true, true);
        }
    }

    void activityRelaunchingLocked(ActivityRecord r) {
        this.mWindowManager.notifyAppRelaunching(r.appToken, r.mFromFreeform);
    }

    void logStackState() {
        this.mActivityMetricsLogger.logWindowState();
    }

    void scheduleUpdateMultiWindowMode(TaskRecord task) {
        if (!task.getStack().deferScheduleMultiWindowModeChanged()) {
            for (int i = task.mActivities.size() - 1; i >= 0; i--) {
                ActivityRecord r = (ActivityRecord) task.mActivities.get(i);
                if (!(r.app == null || r.app.thread == null)) {
                    this.mMultiWindowModeChangedActivities.add(r);
                }
            }
            if (!this.mHandler.hasMessages(114)) {
                this.mHandler.sendEmptyMessage(114);
            }
        }
    }

    void scheduleUpdatePictureInPictureModeIfNeeded(TaskRecord task, ActivityStack prevStack) {
        ActivityStack stack = task.getStack();
        if (prevStack != null && prevStack != stack && (prevStack.mStackId == 4 || stack.mStackId == 4)) {
            scheduleUpdatePictureInPictureModeIfNeeded(task, stack.mBounds);
        }
    }

    void scheduleUpdatePictureInPictureModeIfNeeded(TaskRecord task, Rect targetStackBounds) {
        for (int i = task.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = (ActivityRecord) task.mActivities.get(i);
            if (!(r.app == null || r.app.thread == null)) {
                this.mPipModeChangedActivities.add(r);
            }
        }
        this.mPipModeChangedTargetStackBounds = targetStackBounds;
        if (!this.mHandler.hasMessages(115)) {
            this.mHandler.sendEmptyMessage(115);
        }
    }

    void updatePictureInPictureMode(TaskRecord task, Rect targetStackBounds, boolean forceUpdate) {
        this.mHandler.removeMessages(115);
        for (int i = task.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = (ActivityRecord) task.mActivities.get(i);
            if (!(r.app == null || r.app.thread == null)) {
                r.updatePictureInPictureMode(targetStackBounds, forceUpdate);
            }
        }
    }

    void setDockedStackMinimized(boolean minimized) {
        this.mIsDockMinimized = minimized;
    }

    void wakeUp(String reason) {
        this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server.am:TURN_ON:" + reason);
    }

    private void beginDeferResume() {
        this.mDeferResumeCount++;
    }

    private void endDeferResume() {
        this.mDeferResumeCount--;
    }

    private boolean readyToResume() {
        return this.mDeferResumeCount == 0;
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "ZhiYong.Lin@Plf.Framework add for BPM", property = OppoRomType.ROM)
    final ActivityRecord getTopRunningActivityLocked() {
        ActivityRecord topRunningActivityLocked;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                topRunningActivityLocked = topRunningActivityLocked();
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return topRunningActivityLocked;
    }

    ActivityStack findStackBehind(ActivityStack stack) {
        ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.get(0);
        if (display == null) {
            return null;
        }
        ArrayList<ActivityStack> stacks = display.mStacks;
        int i = stacks.size() - 1;
        while (i >= 0) {
            if (stacks.get(i) == stack && i > 0) {
                return (ActivityStack) stacks.get(i - 1);
            }
            i--;
        }
        throw new IllegalStateException("Failed to find a stack behind stack=" + stack + " in=" + stacks);
    }

    private void setResizingDuringAnimation(TaskRecord task) {
        this.mResizingTasksDuringAnimation.add(Integer.valueOf(task.taskId));
        task.setTaskDockedResizing(true);
    }

    final int startActivityFromRecentsInner(int r23, android.os.Bundle r24) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r17_0 'activityOptions' android.app.ActivityOptions) in PHI: PHI: (r17_1 'activityOptions' android.app.ActivityOptions) = (r17_0 'activityOptions' android.app.ActivityOptions), (r17_2 'activityOptions' android.app.ActivityOptions) binds: {(r17_0 'activityOptions' android.app.ActivityOptions)=B:1:0x0002, (r17_2 'activityOptions' android.app.ActivityOptions)=B:8:0x003a}
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
        r22 = this;
        if (r24 == 0) goto L_0x003a;
    L_0x0002:
        r17 = new android.app.ActivityOptions;
        r0 = r17;
        r1 = r24;
        r0.<init>(r1);
    L_0x000b:
        if (r17 == 0) goto L_0x003d;
    L_0x000d:
        r3 = r17.getLaunchStackId();
    L_0x0011:
        r4 = android.app.ActivityManager.StackId.isHomeOrRecentsStack(r3);
        if (r4 == 0) goto L_0x003f;
    L_0x0017:
        r4 = new java.lang.IllegalArgumentException;
        r8 = new java.lang.StringBuilder;
        r8.<init>();
        r9 = "startActivityFromRecentsInner: Task ";
        r8 = r8.append(r9);
        r0 = r23;
        r8 = r8.append(r0);
        r9 = " can't be launch in the home/recents stack.";
        r8 = r8.append(r9);
        r8 = r8.toString();
        r4.<init>(r8);
        throw r4;
    L_0x003a:
        r17 = 0;
        goto L_0x000b;
    L_0x003d:
        r3 = -1;
        goto L_0x0011;
    L_0x003f:
        r0 = r22;
        r4 = r0.mWindowManager;
        r4.deferSurfaceLayout();
        r4 = 3;
        if (r3 != r4) goto L_0x0065;
    L_0x0049:
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mWindowManager;	 Catch:{ all -> 0x00a0 }
        r8 = r17.getDockCreateMode();	 Catch:{ all -> 0x00a0 }
        r9 = 0;	 Catch:{ all -> 0x00a0 }
        r4.setDockedStackCreateState(r8, r9);	 Catch:{ all -> 0x00a0 }
        r4 = 5;	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r0.deferUpdateBounds(r4);	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mWindowManager;	 Catch:{ all -> 0x00a0 }
        r8 = 19;	 Catch:{ all -> 0x00a0 }
        r9 = 0;	 Catch:{ all -> 0x00a0 }
        r4.prepareAppTransition(r8, r9);	 Catch:{ all -> 0x00a0 }
    L_0x0065:
        r4 = 2;	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r1 = r23;	 Catch:{ all -> 0x00a0 }
        r2 = r0.anyTaskForIdLocked(r1, r4, r3);	 Catch:{ all -> 0x00a0 }
        if (r2 != 0) goto L_0x00a9;	 Catch:{ all -> 0x00a0 }
    L_0x0070:
        r4 = 5;	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r0.continueUpdateBounds(r4);	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mWindowManager;	 Catch:{ all -> 0x00a0 }
        r4.executeAppTransition();	 Catch:{ all -> 0x00a0 }
        r4 = new java.lang.IllegalArgumentException;	 Catch:{ all -> 0x00a0 }
        r8 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00a0 }
        r8.<init>();	 Catch:{ all -> 0x00a0 }
        r9 = "startActivityFromRecentsInner: Task ";	 Catch:{ all -> 0x00a0 }
        r8 = r8.append(r9);	 Catch:{ all -> 0x00a0 }
        r0 = r23;	 Catch:{ all -> 0x00a0 }
        r8 = r8.append(r0);	 Catch:{ all -> 0x00a0 }
        r9 = " not found.";	 Catch:{ all -> 0x00a0 }
        r8 = r8.append(r9);	 Catch:{ all -> 0x00a0 }
        r8 = r8.toString();	 Catch:{ all -> 0x00a0 }
        r4.<init>(r8);	 Catch:{ all -> 0x00a0 }
        throw r4;	 Catch:{ all -> 0x00a0 }
    L_0x00a0:
        r4 = move-exception;
        r0 = r22;
        r8 = r0.mWindowManager;
        r8.continueSurfaceLayout();
        throw r4;
    L_0x00a9:
        r18 = r22.getFocusedStack();	 Catch:{ all -> 0x00a0 }
        if (r18 == 0) goto L_0x0137;	 Catch:{ all -> 0x00a0 }
    L_0x00af:
        r20 = r18.topActivity();	 Catch:{ all -> 0x00a0 }
    L_0x00b3:
        r4 = -1;	 Catch:{ all -> 0x00a0 }
        if (r3 == r4) goto L_0x00c6;	 Catch:{ all -> 0x00a0 }
    L_0x00b6:
        r4 = r2.getStackId();	 Catch:{ all -> 0x00a0 }
        if (r4 == r3) goto L_0x00c6;	 Catch:{ all -> 0x00a0 }
    L_0x00bc:
        r8 = "startActivityFromRecents";	 Catch:{ all -> 0x00a0 }
        r4 = 1;	 Catch:{ all -> 0x00a0 }
        r5 = 0;	 Catch:{ all -> 0x00a0 }
        r6 = 1;	 Catch:{ all -> 0x00a0 }
        r7 = 1;	 Catch:{ all -> 0x00a0 }
        r2.reparent(r3, r4, r5, r6, r7, r8);	 Catch:{ all -> 0x00a0 }
    L_0x00c6:
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mService;	 Catch:{ all -> 0x00a0 }
        r4 = r4.mUserController;	 Catch:{ all -> 0x00a0 }
        r8 = r2.userId;	 Catch:{ all -> 0x00a0 }
        r4 = r4.shouldConfirmCredentials(r8);	 Catch:{ all -> 0x00a0 }
        if (r4 != 0) goto L_0x013d;	 Catch:{ all -> 0x00a0 }
    L_0x00d4:
        r4 = r2.getRootActivity();	 Catch:{ all -> 0x00a0 }
        if (r4 == 0) goto L_0x013d;	 Catch:{ all -> 0x00a0 }
    L_0x00da:
        r21 = r2.getTopActivity();	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mService;	 Catch:{ all -> 0x00a0 }
        r4 = r4.mActivityStarter;	 Catch:{ all -> 0x00a0 }
        r8 = 1;	 Catch:{ all -> 0x00a0 }
        r0 = r21;	 Catch:{ all -> 0x00a0 }
        r4.sendPowerHintForLaunchStartIfNeeded(r8, r0);	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mActivityMetricsLogger;	 Catch:{ all -> 0x00a0 }
        r4.notifyActivityLaunching();	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mService;	 Catch:{ all -> 0x00a0 }
        r8 = r2.taskId;	 Catch:{ all -> 0x00a0 }
        r9 = 0;	 Catch:{ all -> 0x00a0 }
        r10 = 1;	 Catch:{ all -> 0x00a0 }
        r0 = r24;	 Catch:{ all -> 0x00a0 }
        r4.moveTaskToFrontLocked(r8, r9, r0, r10);	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mActivityMetricsLogger;	 Catch:{ all -> 0x00a0 }
        r8 = 2;	 Catch:{ all -> 0x00a0 }
        r0 = r21;	 Catch:{ all -> 0x00a0 }
        r4.notifyActivityLaunched(r8, r0);	 Catch:{ all -> 0x00a0 }
        r4 = 3;	 Catch:{ all -> 0x00a0 }
        if (r3 != r4) goto L_0x0110;	 Catch:{ all -> 0x00a0 }
    L_0x010b:
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r0.setResizingDuringAnimation(r2);	 Catch:{ all -> 0x00a0 }
    L_0x0110:
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mService;	 Catch:{ all -> 0x00a0 }
        r4 = r4.mActivityStarter;	 Catch:{ all -> 0x00a0 }
        r5 = r2.getTopActivity();	 Catch:{ all -> 0x00a0 }
        if (r20 == 0) goto L_0x013b;	 Catch:{ all -> 0x00a0 }
    L_0x011c:
        r8 = r20.getTask();	 Catch:{ all -> 0x00a0 }
        r7 = r8.getStackId();	 Catch:{ all -> 0x00a0 }
    L_0x0124:
        r9 = r2.getStack();	 Catch:{ all -> 0x00a0 }
        r6 = 2;	 Catch:{ all -> 0x00a0 }
        r8 = r20;	 Catch:{ all -> 0x00a0 }
        r4.postStartActivityProcessing(r5, r6, r7, r8, r9);	 Catch:{ all -> 0x00a0 }
        r4 = 2;
        r0 = r22;
        r8 = r0.mWindowManager;
        r8.continueSurfaceLayout();
        return r4;
    L_0x0137:
        r20 = 0;
        goto L_0x00b3;
    L_0x013b:
        r7 = -1;
        goto L_0x0124;
    L_0x013d:
        r5 = r2.mCallingUid;	 Catch:{ all -> 0x00a0 }
        r6 = r2.mCallingPackage;	 Catch:{ all -> 0x00a0 }
        r7 = r2.intent;	 Catch:{ all -> 0x00a0 }
        r4 = 1048576; // 0x100000 float:1.469368E-39 double:5.180654E-318;	 Catch:{ all -> 0x00a0 }
        r7.addFlags(r4);	 Catch:{ all -> 0x00a0 }
        r14 = r2.userId;	 Catch:{ all -> 0x00a0 }
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r4 = r0.mService;	 Catch:{ all -> 0x00a0 }
        r16 = "startActivityFromRecents";	 Catch:{ all -> 0x00a0 }
        r8 = 0;	 Catch:{ all -> 0x00a0 }
        r9 = 0;	 Catch:{ all -> 0x00a0 }
        r10 = 0;	 Catch:{ all -> 0x00a0 }
        r11 = 0;	 Catch:{ all -> 0x00a0 }
        r12 = 0;	 Catch:{ all -> 0x00a0 }
        r13 = r24;	 Catch:{ all -> 0x00a0 }
        r15 = r2;	 Catch:{ all -> 0x00a0 }
        r19 = r4.startActivityInPackage(r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15, r16);	 Catch:{ all -> 0x00a0 }
        r4 = 3;	 Catch:{ all -> 0x00a0 }
        if (r3 != r4) goto L_0x0165;	 Catch:{ all -> 0x00a0 }
    L_0x0160:
        r0 = r22;	 Catch:{ all -> 0x00a0 }
        r0.setResizingDuringAnimation(r2);	 Catch:{ all -> 0x00a0 }
    L_0x0165:
        r0 = r22;
        r4 = r0.mWindowManager;
        r4.continueSurfaceLayout();
        return r19;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ActivityStackSupervisor.startActivityFromRecentsInner(int, android.os.Bundle):int");
    }

    final int startActivityForFreeform(android.content.Intent r23, android.os.Bundle r24, int r25, int r26, java.lang.String r27) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r16_0 'activityOptions' android.app.ActivityOptions) in PHI: PHI: (r16_1 'activityOptions' android.app.ActivityOptions) = (r16_0 'activityOptions' android.app.ActivityOptions), (r16_2 'activityOptions' android.app.ActivityOptions) binds: {(r16_0 'activityOptions' android.app.ActivityOptions)=B:2:0x0003, (r16_2 'activityOptions' android.app.ActivityOptions)=B:10:0x0041}
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
        r22 = this;
        r14 = 0;
        if (r24 == 0) goto L_0x0041;
    L_0x0003:
        r16 = new android.app.ActivityOptions;
        r0 = r16;
        r1 = r24;
        r0.<init>(r1);
    L_0x000c:
        if (r16 == 0) goto L_0x0044;
    L_0x000e:
        r18 = r16.getLaunchStackId();
    L_0x0012:
        r4 = r26;
        r5 = r27;
        r3 = android.app.ActivityManager.StackId.isHomeOrRecentsStack(r18);
        if (r3 != 0) goto L_0x001e;
    L_0x001c:
        if (r23 != 0) goto L_0x0047;
    L_0x001e:
        r3 = new java.lang.IllegalArgumentException;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "startActivityFromRecentsInner: Task ";
        r6 = r6.append(r7);
        r0 = r23;
        r6 = r6.append(r0);
        r7 = " can't be launch in the home/recents stack.";
        r6 = r6.append(r7);
        r6 = r6.toString();
        r3.<init>(r6);
        throw r3;
    L_0x0041:
        r16 = 0;
        goto L_0x000c;
    L_0x0044:
        r18 = -1;
        goto L_0x0012;
    L_0x0047:
        r0 = r22;
        r3 = r0.mWindowManager;
        r3.deferSurfaceLayout();
        r0 = r22;	 Catch:{ all -> 0x018c }
        r1 = r23;	 Catch:{ all -> 0x018c }
        r2 = r25;	 Catch:{ all -> 0x018c }
        r14 = r0.findTaskForFreeformLocked(r1, r2);	 Catch:{ all -> 0x018c }
        r3 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_STACK;	 Catch:{ all -> 0x018c }
        if (r3 == 0) goto L_0x0075;	 Catch:{ all -> 0x018c }
    L_0x005c:
        r3 = TAG;	 Catch:{ all -> 0x018c }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x018c }
        r6.<init>();	 Catch:{ all -> 0x018c }
        r7 = "oppo freeform findTaskForFreeformLocked: task0 ";	 Catch:{ all -> 0x018c }
        r6 = r6.append(r7);	 Catch:{ all -> 0x018c }
        r6 = r6.append(r14);	 Catch:{ all -> 0x018c }
        r6 = r6.toString();	 Catch:{ all -> 0x018c }
        android.util.Slog.v(r3, r6);	 Catch:{ all -> 0x018c }
    L_0x0075:
        if (r14 != 0) goto L_0x013d;	 Catch:{ all -> 0x018c }
    L_0x0077:
        r3 = 0;	 Catch:{ all -> 0x018c }
        r0 = r22;	 Catch:{ all -> 0x018c }
        r1 = r23;	 Catch:{ all -> 0x018c }
        r2 = r25;	 Catch:{ all -> 0x018c }
        r17 = r0.findActivityForFreeformLocked(r1, r2, r3);	 Catch:{ all -> 0x018c }
        r3 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_STACK;	 Catch:{ all -> 0x018c }
        if (r3 == 0) goto L_0x00a1;	 Catch:{ all -> 0x018c }
    L_0x0086:
        r3 = TAG;	 Catch:{ all -> 0x018c }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x018c }
        r6.<init>();	 Catch:{ all -> 0x018c }
        r7 = "oppo freeform startActivityForFreeform: intentActivity ";	 Catch:{ all -> 0x018c }
        r6 = r6.append(r7);	 Catch:{ all -> 0x018c }
        r0 = r17;	 Catch:{ all -> 0x018c }
        r6 = r6.append(r0);	 Catch:{ all -> 0x018c }
        r6 = r6.toString();	 Catch:{ all -> 0x018c }
        android.util.Slog.v(r3, r6);	 Catch:{ all -> 0x018c }
    L_0x00a1:
        r21 = -1;	 Catch:{ all -> 0x018c }
        if (r17 == 0) goto L_0x00d4;	 Catch:{ all -> 0x018c }
    L_0x00a5:
        r0 = r17;	 Catch:{ all -> 0x018c }
        r3 = r0.task;	 Catch:{ all -> 0x018c }
        if (r3 == 0) goto L_0x00d4;	 Catch:{ all -> 0x018c }
    L_0x00ab:
        r0 = r17;	 Catch:{ all -> 0x018c }
        r3 = r0.task;	 Catch:{ all -> 0x018c }
        r0 = r3.taskId;	 Catch:{ all -> 0x018c }
        r21 = r0;	 Catch:{ all -> 0x018c }
        r3 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_STACK;	 Catch:{ all -> 0x018c }
        if (r3 == 0) goto L_0x00d4;	 Catch:{ all -> 0x018c }
    L_0x00b7:
        r3 = TAG;	 Catch:{ all -> 0x018c }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x018c }
        r6.<init>();	 Catch:{ all -> 0x018c }
        r7 = "oppo freeform startActivityForFreeform: task ";	 Catch:{ all -> 0x018c }
        r6 = r6.append(r7);	 Catch:{ all -> 0x018c }
        r0 = r17;	 Catch:{ all -> 0x018c }
        r7 = r0.task;	 Catch:{ all -> 0x018c }
        r6 = r6.append(r7);	 Catch:{ all -> 0x018c }
        r6 = r6.toString();	 Catch:{ all -> 0x018c }
        android.util.Slog.v(r3, r6);	 Catch:{ all -> 0x018c }
    L_0x00d4:
        r3 = 2;	 Catch:{ all -> 0x018c }
        r0 = r22;	 Catch:{ all -> 0x018c }
        r1 = r21;	 Catch:{ all -> 0x018c }
        r2 = r18;	 Catch:{ all -> 0x018c }
        r14 = r0.anyTaskForIdLocked(r1, r3, r2);	 Catch:{ all -> 0x018c }
        r3 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_STACK;	 Catch:{ all -> 0x018c }
        if (r3 == 0) goto L_0x00fc;	 Catch:{ all -> 0x018c }
    L_0x00e3:
        r3 = TAG;	 Catch:{ all -> 0x018c }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x018c }
        r6.<init>();	 Catch:{ all -> 0x018c }
        r7 = "oppo freeform startActivityForFreeform: task2 ";	 Catch:{ all -> 0x018c }
        r6 = r6.append(r7);	 Catch:{ all -> 0x018c }
        r6 = r6.append(r14);	 Catch:{ all -> 0x018c }
        r6 = r6.toString();	 Catch:{ all -> 0x018c }
        android.util.Slog.v(r3, r6);	 Catch:{ all -> 0x018c }
    L_0x00fc:
        if (r14 == 0) goto L_0x013d;	 Catch:{ all -> 0x018c }
    L_0x00fe:
        r3 = r14.intent;	 Catch:{ all -> 0x018c }
        if (r3 == 0) goto L_0x013d;	 Catch:{ all -> 0x018c }
    L_0x0102:
        r3 = r14.intent;	 Catch:{ all -> 0x018c }
        r20 = r3.getAction();	 Catch:{ all -> 0x018c }
        if (r20 == 0) goto L_0x013d;	 Catch:{ all -> 0x018c }
    L_0x010a:
        r3 = "android.intent.action.VIEW";	 Catch:{ all -> 0x018c }
        r0 = r20;	 Catch:{ all -> 0x018c }
        r3 = r3.equals(r0);	 Catch:{ all -> 0x018c }
        r3 = r3 ^ 1;	 Catch:{ all -> 0x018c }
        if (r3 == 0) goto L_0x013d;	 Catch:{ all -> 0x018c }
    L_0x0117:
        r3 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_STACK;	 Catch:{ all -> 0x018c }
        if (r3 == 0) goto L_0x0134;	 Catch:{ all -> 0x018c }
    L_0x011b:
        r3 = TAG;	 Catch:{ all -> 0x018c }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x018c }
        r6.<init>();	 Catch:{ all -> 0x018c }
        r7 = "oppo freeform startActivityForFreeform: use task ";	 Catch:{ all -> 0x018c }
        r6 = r6.append(r7);	 Catch:{ all -> 0x018c }
        r6 = r6.append(r14);	 Catch:{ all -> 0x018c }
        r6 = r6.toString();	 Catch:{ all -> 0x018c }
        android.util.Slog.v(r3, r6);	 Catch:{ all -> 0x018c }
    L_0x0134:
        r4 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;	 Catch:{ all -> 0x018c }
        r5 = "com.android.systemui";	 Catch:{ all -> 0x018c }
        r0 = r14.intent;	 Catch:{ all -> 0x018c }
        r23 = r0;	 Catch:{ all -> 0x018c }
    L_0x013d:
        r3 = com.android.server.am.OppoFreeFormManagerService.getInstance();	 Catch:{ all -> 0x018c }
        r3.handleActivityFromFreeformFullscreen(r14);	 Catch:{ all -> 0x018c }
        r3 = com.android.server.am.OppoFreeFormManagerService.getInstance();	 Catch:{ all -> 0x018c }
        r0 = r22;	 Catch:{ all -> 0x018c }
        r6 = r0.mFocusedStack;	 Catch:{ all -> 0x018c }
        r3.setParentInfo(r6);	 Catch:{ all -> 0x018c }
        r3 = TAG;	 Catch:{ all -> 0x018c }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x018c }
        r6.<init>();	 Catch:{ all -> 0x018c }
        r7 = "oppo freeform startActivityForFreeform: ";	 Catch:{ all -> 0x018c }
        r6 = r6.append(r7);	 Catch:{ all -> 0x018c }
        r6 = r6.append(r14);	 Catch:{ all -> 0x018c }
        r6 = r6.toString();	 Catch:{ all -> 0x018c }
        android.util.Slog.v(r3, r6);	 Catch:{ all -> 0x018c }
        r3 = 1;	 Catch:{ all -> 0x018c }
        r0 = r23;	 Catch:{ all -> 0x018c }
        r0.setLaunchStackId(r3);	 Catch:{ all -> 0x018c }
        r0 = r22;	 Catch:{ all -> 0x018c }
        r3 = r0.mService;	 Catch:{ all -> 0x018c }
        r15 = "startActivityForFreeform";	 Catch:{ all -> 0x018c }
        r7 = 0;	 Catch:{ all -> 0x018c }
        r8 = 0;	 Catch:{ all -> 0x018c }
        r9 = 0;	 Catch:{ all -> 0x018c }
        r10 = 0;	 Catch:{ all -> 0x018c }
        r11 = 0;	 Catch:{ all -> 0x018c }
        r6 = r23;	 Catch:{ all -> 0x018c }
        r12 = r24;	 Catch:{ all -> 0x018c }
        r13 = r25;	 Catch:{ all -> 0x018c }
        r19 = r3.startActivityInPackage(r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15);	 Catch:{ all -> 0x018c }
        r0 = r22;
        r3 = r0.mWindowManager;
        r3.continueSurfaceLayout();
        return r19;
    L_0x018c:
        r3 = move-exception;
        r0 = r22;
        r6 = r0.mWindowManager;
        r6.continueSurfaceLayout();
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ActivityStackSupervisor.startActivityForFreeform(android.content.Intent, android.os.Bundle, int, int, java.lang.String):int");
    }

    List<IBinder> getTopVisibleActivities() {
        ArrayList<IBinder> topActivityTokens = new ArrayList();
        for (int i = this.mActivityDisplays.size() - 1; i >= 0; i--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(i);
            for (int j = display.mStacks.size() - 1; j >= 0; j--) {
                ActivityStack stack = (ActivityStack) display.mStacks.get(j);
                if (stack.shouldBeVisible(null) == 1) {
                    ActivityRecord top = stack.topActivity();
                    if (top != null) {
                        if (stack == this.mFocusedStack) {
                            topActivityTokens.add(0, top.appToken);
                        } else {
                            topActivityTokens.add(top.appToken);
                        }
                    }
                }
            }
        }
        return topActivityTokens;
    }

    public ComponentName getTopAppName() {
        return getFocusedStack().getTopAppName();
    }

    void collectionStartUrlInfo(IApplicationThread caller, ProcessRecord callerApp, int pid, int uid, String callingPackage, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            String url = intent.getDataString();
            if (action != null && action.equals("android.intent.action.VIEW") && url != null && url.contains("http")) {
                boolean isFore = false;
                String cpnPkgName = "";
                String cpnClassName = "";
                ComponentName cpn = intent.getComponent();
                if (cpn != null) {
                    cpnPkgName = cpn.getPackageName();
                    cpnClassName = cpn.getClassName();
                }
                if (callingPackage != null && callingPackage.equals(getTopPackageName())) {
                    isFore = true;
                }
                if (callingPackage != null && (callingPackage.equals(cpnPkgName) ^ 1) != 0) {
                    sendBroadcastForStartUrlInfo(callingPackage, url, cpnPkgName, cpnClassName, isFore);
                }
            }
        }
    }

    private void sendBroadcastForStartUrlInfo(String callingPkg, String url, String pkgName, String className, boolean isFore) {
        Slog.d(TAG, "sendBroadcastForStartUrlInfo callingPkg = " + callingPkg + "  url = " + url + "  pkgName = " + pkgName + "  className= " + className + "  isFore = " + isFore);
        Intent intent = new Intent("oppo.intent.action.OPPO_SAFE_COUNT_START_URL");
        intent.putExtra("caller", callingPkg);
        intent.putExtra("url", url);
        intent.putExtra("pkgName", pkgName);
        intent.putExtra("className", className);
        intent.putExtra("isFore", isFore);
        this.mService.mContext.sendBroadcast(intent);
    }

    private String getTopPackageName() {
        String topPkgName = "";
        ComponentName topCpn = getFocusedStack().mComponentName;
        if (topCpn != null) {
            return topCpn.getPackageName();
        }
        return topPkgName;
    }

    void dataCollectionInfoExp(ProcessRecord callerApp, String callingPackage, Intent intent) {
        String pkgInstallerExp = "com.google.android.packageinstaller";
        String pkgInstallerActExp = "com.android.packageinstaller.PackageInstallerActivity";
        if (intent != null && intent.getComponent() != null) {
            ComponentName realActivity = intent.getComponent();
            if (realActivity != null && pkgInstallerActExp.equals(realActivity.getClassName()) && callerApp != null && callerApp.info.processName != null) {
                String callAppName = callerApp.info.processName;
                if (!pkgInstallerExp.equals(callAppName)) {
                    Slog.d(TAG, "dataCollectionInfoExp: callAppName" + callAppName + " callingPackage " + callingPackage);
                    intent.putExtra("android.intent.extra.INSTALLER_PACKAGE_NAME", callAppName);
                    try {
                        SystemProperties.set("oppo.exp.install.collect", callAppName);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public ComponentName getDockTopAppName() {
        ActivityStack stack = getStack(3);
        if (stack != null) {
            return stack.getDockTopAppName();
        }
        return null;
    }

    public List<String> getAllTopPkgName() {
        List<String> list = new ArrayList();
        ActivityStack appStack = getStack(1);
        if (appStack == null || appStack.getResumedCpn() == null || appStack.getResumedCpn().getPackageName() == null) {
            ActivityStack homeStack = getStack(0);
            if (homeStack == null || homeStack.getResumedCpn() == null || homeStack.getResumedCpn().getPackageName() == null) {
                list.add("");
            } else {
                try {
                    Slog.v(TAG, "getAllTopPkgName 0 " + homeStack.getResumedCpn().getPackageName());
                    list.add(homeStack.getResumedCpn().getPackageName());
                } catch (Exception e) {
                    Slog.v(TAG, "getAllTopPkgName 0 exception!");
                    list.add("");
                }
            }
        } else {
            try {
                Slog.v(TAG, "getAllTopPkgName 1 " + appStack.getResumedCpn().getPackageName());
                list.add(appStack.getResumedCpn().getPackageName());
            } catch (Exception e2) {
                Slog.v(TAG, "getAllTopPkgName 1 exception!");
                list.add("");
            }
        }
        ActivityStack freeformStack = getStack(2);
        if (freeformStack == null || freeformStack.getResumedCpn() == null || freeformStack.getResumedCpn().getPackageName() == null) {
            list.add("");
        } else {
            try {
                Slog.v(TAG, "getAllTopPkgName 2 " + freeformStack.getResumedCpn().getPackageName());
                list.add(freeformStack.getResumedCpn().getPackageName());
            } catch (Exception e3) {
                Slog.v(TAG, "getAllTopPkgName 2 exception!");
                list.add("");
            }
        }
        ActivityStack dockedStack = getStack(3);
        if (dockedStack == null || dockedStack.getDockTopAppName() == null || dockedStack.getDockTopAppName().getPackageName() == null) {
            list.add("");
        } else {
            try {
                Slog.v(TAG, "getAllTopPkgName 3 " + dockedStack.getDockTopAppName().getPackageName());
                list.add(dockedStack.getDockTopAppName().getPackageName());
            } catch (Exception e4) {
                Slog.v(TAG, "getAllTopPkgName 3 exception!");
                list.add("");
            }
        }
        return list;
    }

    public ApplicationInfo getFreeFormAppInfo() {
        ActivityStack appStack = getStack(2);
        if (appStack == null) {
            return null;
        }
        ApplicationInfo appInfo = appStack.getResumedAppInfo();
        Slog.v(TAG, "getFreeFormAppInfo " + appInfo);
        return appInfo;
    }

    public void pauseStack(int stackId, boolean dontWait) {
        ActivityStack stack = getStack(stackId, true, false);
        if (stack != null) {
            ActivityRecord pausingActivity;
            if (stack.mResumedActivity != null) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.d(TAG_STATES, "pauseStack: stack=" + stack + " mResumedActivity=" + stack.mResumedActivity);
                }
                pausingActivity = stack.mResumedActivity;
            } else {
                pausingActivity = stack.topActivity();
                if (pausingActivity == null && stack.topTask() != null) {
                    pausingActivity = stack.topTask().getTopActivity();
                }
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.d(TAG_STATES, "pauseStack: stack=" + stack + " topActivity=" + pausingActivity);
                }
            }
            if (pausingActivity != null) {
                if (stack.topTask() != null) {
                    stack.topTask().setForceInvibleInPreload(true);
                }
                stack.mResumedActivity = pausingActivity;
                stack.startPausingLocked(false, false, null, dontWait, "pause-stack");
            }
        }
    }
}
