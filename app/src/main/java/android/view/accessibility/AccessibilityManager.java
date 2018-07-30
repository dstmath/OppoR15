package android.view.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.view.IWindow;
import android.view.accessibility.IAccessibilityManagerClient.Stub;
import com.android.internal.R;
import com.android.internal.util.IntPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AccessibilityManager {
    public static final String ACTION_CHOOSE_ACCESSIBILITY_BUTTON = "com.android.internal.intent.action.CHOOSE_ACCESSIBILITY_BUTTON";
    public static final int AUTOCLICK_DELAY_DEFAULT = 600;
    public static final int COLOR_STATE_DIRECT_ENABLED = 16;
    public static final int DALTONIZER_CORRECT_DEUTERANOMALY = 12;
    public static final int DALTONIZER_DISABLED = -1;
    public static final int DALTONIZER_SIMULATE_MONOCHROMACY = 0;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AccessibilityManager";
    public static final int STATE_FLAG_ACCESSIBILITY_ENABLED = 1;
    public static final int STATE_FLAG_HIGH_TEXT_CONTRAST_ENABLED = 4;
    public static final int STATE_FLAG_TOUCH_EXPLORATION_ENABLED = 2;
    private static final ArrayList<String> mAllowsPackage = new ArrayList(Arrays.asList(new String[]{"com.tencent.mm"}));
    private static boolean sExplorationEnabled = false;
    private static AccessibilityManager sInstance;
    static final Object sInstanceSync = new Object();
    private final ArrayMap<AccessibilityStateChangeListener, Handler> mAccessibilityStateChangeListeners = new ArrayMap();
    final Callback mCallback = new MyCallback();
    private final Stub mClient = new Stub() {
        public void setState(int state) {
            AccessibilityManager.this.mHandler.obtainMessage(1, state, 0).sendToTarget();
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void notifyServicesStateChanged() {
            synchronized (AccessibilityManager.this.mLock) {
                if (AccessibilityManager.this.mServicesStateChangeListeners.isEmpty()) {
                    return;
                }
                ArrayMap<AccessibilityServicesStateChangeListener, Handler> listeners = new ArrayMap(AccessibilityManager.this.mServicesStateChangeListeners);
            }
        }

        /* synthetic */ void lambda$-android_view_accessibility_AccessibilityManager$1_9166(AccessibilityServicesStateChangeListener listener) {
            listener.onAccessibilityServicesStateChanged(AccessibilityManager.this);
        }

        public void setRelevantEventTypes(int eventTypes) {
            AccessibilityManager.this.mRelevantEventTypes = eventTypes;
        }
    };
    final Handler mHandler;
    private final ArrayMap<HighTextContrastChangeListener, Handler> mHighTextContrastStateChangeListeners = new ArrayMap();
    boolean mIsDirectEnabled;
    boolean mIsEnabled;
    boolean mIsHighTextContrastEnabled;
    private boolean mIsSystemserver;
    boolean mIsTouchExplorationEnabled;
    private final Object mLock = new Object();
    int mRelevantEventTypes = -1;
    private SparseArray<List<AccessibilityRequestPreparer>> mRequestPreparerLists;
    private IAccessibilityManager mService;
    private final ArrayMap<AccessibilityServicesStateChangeListener, Handler> mServicesStateChangeListeners = new ArrayMap();
    private final ArrayMap<TouchExplorationStateChangeListener, Handler> mTouchExplorationStateChangeListeners = new ArrayMap();
    final int mUserId;

    public interface AccessibilityStateChangeListener {
        void onAccessibilityStateChanged(boolean z);
    }

    public interface HighTextContrastChangeListener {
        void onHighTextContrastStateChanged(boolean z);
    }

    public interface AccessibilityServicesStateChangeListener {
        void onAccessibilityServicesStateChanged(AccessibilityManager accessibilityManager);
    }

    private final class MyCallback implements Callback {
        public static final int MSG_SET_STATE = 1;

        /* synthetic */ MyCallback(AccessibilityManager this$0, MyCallback -this1) {
            this();
        }

        private MyCallback() {
        }

        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    int state = message.arg1;
                    synchronized (AccessibilityManager.this.mLock) {
                        AccessibilityManager.this.setStateLocked(state);
                    }
            }
            return true;
        }
    }

    public interface TouchExplorationStateChangeListener {
        void onTouchExplorationStateChanged(boolean z);
    }

    public static AccessibilityManager getInstance(Context context) {
        synchronized (sInstanceSync) {
            if (sInstance == null) {
                int userId;
                if (!(Binder.getCallingUid() == 1000 || context.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") == 0)) {
                    if (context.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
                        userId = UserHandle.myUserId();
                        sInstance = new AccessibilityManager(context, null, userId);
                        if (context.getPackageName() == null && mAllowsPackage.contains(context.getPackageName())) {
                            sExplorationEnabled = true;
                        } else {
                            sExplorationEnabled = false;
                        }
                    }
                }
                userId = -2;
                sInstance = new AccessibilityManager(context, null, userId);
                if (context.getPackageName() == null) {
                }
                sExplorationEnabled = false;
            }
        }
        return sInstance;
    }

    private boolean isSpecialProcess(String strProcName) {
        return false;
    }

    private boolean isSystemserverProcess() {
        return isSpecialProcess("system_server");
    }

    public AccessibilityManager(Context context, IAccessibilityManager service, int userId) {
        this.mHandler = new Handler(context.getMainLooper(), this.mCallback);
        this.mUserId = userId;
        synchronized (this.mLock) {
            tryConnectToServiceLocked(service);
        }
    }

    public AccessibilityManager(Handler handler, IAccessibilityManager service, int userId) {
        this.mHandler = handler;
        this.mUserId = userId;
        this.mIsSystemserver = isSystemserverProcess();
        synchronized (this.mLock) {
            tryConnectToServiceLocked(service);
        }
    }

    public IAccessibilityManagerClient getClient() {
        return this.mClient;
    }

    public Callback getCallback() {
        return this.mCallback;
    }

    public boolean isEnabled() {
        synchronized (this.mLock) {
            if (getServiceLocked() == null) {
                return false;
            }
            boolean z = this.mIsEnabled;
            return z;
        }
    }

    public boolean isTouchExplorationEnabled() {
        synchronized (this.mLock) {
            if (getServiceLocked() == null) {
                return false;
            }
            boolean z = this.mIsTouchExplorationEnabled;
            return z;
        }
    }

    public boolean isHighTextContrastEnabled() {
        synchronized (this.mLock) {
            if (getServiceLocked() == null) {
                return false;
            }
            boolean z = this.mIsHighTextContrastEnabled;
            return z;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void sendAccessibilityEvent(AccessibilityEvent event) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
            } else if (this.mIsEnabled) {
                if ((event.getEventType() & this.mRelevantEventTypes) == 0) {
                    return;
                }
                int userId = this.mUserId;
            } else if (Looper.myLooper() != Looper.getMainLooper()) {
                Log.e(LOG_TAG, "AccessibilityEvent sent with accessibility disabled");
            } else if (this.mIsSystemserver) {
                Log.w(LOG_TAG, "sendAccessibilityEvent, Accessibility off. Did you forget to check that? Becasue systemserver process, we skip throw exception");
            } else {
                throw new IllegalStateException("Accessibility off. Did you forget to check that?");
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void interrupt() {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
            } else if (this.mIsEnabled) {
                int userId = this.mUserId;
            } else if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("Accessibility off. Did you forget to check that?");
            } else {
                Log.e(LOG_TAG, "Interrupt called with accessibility disabled");
            }
        }
    }

    @Deprecated
    public List<ServiceInfo> getAccessibilityServiceList() {
        List<AccessibilityServiceInfo> infos = getInstalledAccessibilityServiceList();
        List<ServiceInfo> services = new ArrayList();
        int infoCount = infos.size();
        for (int i = 0; i < infoCount; i++) {
            services.add(((AccessibilityServiceInfo) infos.get(i)).getResolveInfo().serviceInfo);
        }
        return Collections.unmodifiableList(services);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList() {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
                List<AccessibilityServiceInfo> emptyList = Collections.emptyList();
                return emptyList;
            }
            int userId = this.mUserId;
        }
        if (services != null) {
            return Collections.unmodifiableList(services);
        }
        return Collections.emptyList();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackTypeFlags) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
                List<AccessibilityServiceInfo> emptyList = Collections.emptyList();
                return emptyList;
            }
            int userId = this.mUserId;
        }
        if (services != null) {
            return Collections.unmodifiableList(services);
        }
        return Collections.emptyList();
    }

    public boolean addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
        addAccessibilityStateChangeListener(listener, null);
        return true;
    }

    public void addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener, Handler handler) {
        synchronized (this.mLock) {
            ArrayMap arrayMap = this.mAccessibilityStateChangeListeners;
            if (handler == null) {
                handler = this.mHandler;
            }
            arrayMap.put(listener, handler);
        }
    }

    public boolean removeAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
        boolean z = false;
        synchronized (this.mLock) {
            int index = this.mAccessibilityStateChangeListeners.indexOfKey(listener);
            this.mAccessibilityStateChangeListeners.remove(listener);
            if (index >= 0) {
                z = true;
            }
        }
        return z;
    }

    public boolean addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
        addTouchExplorationStateChangeListener(listener, null);
        return true;
    }

    public void addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener, Handler handler) {
        synchronized (this.mLock) {
            ArrayMap arrayMap = this.mTouchExplorationStateChangeListeners;
            if (handler == null) {
                handler = this.mHandler;
            }
            arrayMap.put(listener, handler);
        }
    }

    public boolean removeTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
        boolean z = false;
        synchronized (this.mLock) {
            int index = this.mTouchExplorationStateChangeListeners.indexOfKey(listener);
            this.mTouchExplorationStateChangeListeners.remove(listener);
            if (index >= 0) {
                z = true;
            }
        }
        return z;
    }

    public void addAccessibilityServicesStateChangeListener(AccessibilityServicesStateChangeListener listener, Handler handler) {
        synchronized (this.mLock) {
            ArrayMap arrayMap = this.mServicesStateChangeListeners;
            if (handler == null) {
                handler = this.mHandler;
            }
            arrayMap.put(listener, handler);
        }
    }

    public void removeAccessibilityServicesStateChangeListener(AccessibilityServicesStateChangeListener listener) {
        this.mServicesStateChangeListeners.remove(listener);
    }

    public void addAccessibilityRequestPreparer(AccessibilityRequestPreparer preparer) {
        if (this.mRequestPreparerLists == null) {
            this.mRequestPreparerLists = new SparseArray(1);
        }
        int id = preparer.getView().getAccessibilityViewId();
        List<AccessibilityRequestPreparer> requestPreparerList = (List) this.mRequestPreparerLists.get(id);
        if (requestPreparerList == null) {
            requestPreparerList = new ArrayList(1);
            this.mRequestPreparerLists.put(id, requestPreparerList);
        }
        requestPreparerList.add(preparer);
    }

    public void removeAccessibilityRequestPreparer(AccessibilityRequestPreparer preparer) {
        if (this.mRequestPreparerLists != null) {
            int viewId = preparer.getView().getAccessibilityViewId();
            List<AccessibilityRequestPreparer> requestPreparerList = (List) this.mRequestPreparerLists.get(viewId);
            if (requestPreparerList != null) {
                requestPreparerList.remove(preparer);
                if (requestPreparerList.isEmpty()) {
                    this.mRequestPreparerLists.remove(viewId);
                }
            }
        }
    }

    public List<AccessibilityRequestPreparer> getRequestPreparersForAccessibilityId(int id) {
        if (this.mRequestPreparerLists == null) {
            return null;
        }
        return (List) this.mRequestPreparerLists.get(id);
    }

    public void addHighTextContrastStateChangeListener(HighTextContrastChangeListener listener, Handler handler) {
        synchronized (this.mLock) {
            ArrayMap arrayMap = this.mHighTextContrastStateChangeListeners;
            if (handler == null) {
                handler = this.mHandler;
            }
            arrayMap.put(listener, handler);
        }
    }

    public void removeHighTextContrastStateChangeListener(HighTextContrastChangeListener listener) {
        synchronized (this.mLock) {
            this.mHighTextContrastStateChangeListeners.remove(listener);
        }
    }

    public boolean isAccessibilityVolumeStreamActive() {
        List<AccessibilityServiceInfo> serviceInfos = getEnabledAccessibilityServiceList(-1);
        for (int i = 0; i < serviceInfos.size(); i++) {
            if ((((AccessibilityServiceInfo) serviceInfos.get(i)).flags & 128) != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean sendFingerprintGesture(int keyCode) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
                return false;
            }
            try {
                return service.sendFingerprintGesture(keyCode);
            } catch (RemoteException e) {
                return false;
            }
        }
    }

    private void setStateLocked(int stateFlags) {
        boolean enabled = (stateFlags & 1) != 0;
        boolean touchExplorationEnabled = (stateFlags & 2) != 0;
        boolean highTextContrastEnabled = (stateFlags & 4) != 0;
        boolean wasEnabled = this.mIsEnabled;
        boolean wasTouchExplorationEnabled = this.mIsTouchExplorationEnabled;
        boolean wasHighTextContrastEnabled = this.mIsHighTextContrastEnabled;
        this.mIsDirectEnabled = (stateFlags & 16) != 0;
        this.mIsEnabled = enabled;
        this.mIsTouchExplorationEnabled = touchExplorationEnabled;
        this.mIsHighTextContrastEnabled = highTextContrastEnabled;
        if (wasEnabled != enabled) {
            notifyAccessibilityStateChanged();
        }
        if (wasTouchExplorationEnabled != touchExplorationEnabled) {
            notifyTouchExplorationStateChanged();
        }
        if (wasHighTextContrastEnabled != highTextContrastEnabled) {
            notifyHighTextContrastStateChanged();
        }
    }

    public AccessibilityServiceInfo getInstalledServiceInfoWithComponentName(ComponentName componentName) {
        List<AccessibilityServiceInfo> installedServiceInfos = getInstalledAccessibilityServiceList();
        if (installedServiceInfos == null || componentName == null) {
            return null;
        }
        for (int i = 0; i < installedServiceInfos.size(); i++) {
            if (componentName.equals(((AccessibilityServiceInfo) installedServiceInfos.get(i)).getComponentName())) {
                return (AccessibilityServiceInfo) installedServiceInfos.get(i);
            }
        }
        return null;
    }

    public int addAccessibilityInteractionConnection(IWindow windowToken, IAccessibilityInteractionConnection connection) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
                return -1;
            }
            int userId = this.mUserId;
            try {
                return service.addAccessibilityInteractionConnection(windowToken, connection, userId);
            } catch (RemoteException re) {
                Log.e(LOG_TAG, "Error while adding an accessibility interaction connection. ", re);
                return -1;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeAccessibilityInteractionConnection(IWindow windowToken) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void performAccessibilityShortcut() {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyAccessibilityButtonClicked() {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyAccessibilityButtonVisibilityChanged(boolean shown) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setPictureInPictureActionReplacingConnection(IAccessibilityInteractionConnection connection) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
            }
        }
    }

    private IAccessibilityManager getServiceLocked() {
        if (this.mService == null) {
            tryConnectToServiceLocked(null);
        }
        return this.mService;
    }

    private void tryConnectToServiceLocked(IAccessibilityManager service) {
        if (service == null) {
            IBinder iBinder = ServiceManager.getService("accessibility");
            if (iBinder != null) {
                service = IAccessibilityManager.Stub.asInterface(iBinder);
            } else {
                return;
            }
        }
        try {
            long userStateAndRelevantEvents = service.addClient(this.mClient, this.mUserId);
            setStateLocked(IntPair.first(userStateAndRelevantEvents));
            this.mRelevantEventTypes = IntPair.second(userStateAndRelevantEvents);
            this.mService = service;
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "AccessibilityManagerService is dead", re);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyAccessibilityStateChanged() {
        synchronized (this.mLock) {
            if (this.mAccessibilityStateChangeListeners.isEmpty()) {
            } else {
                boolean isEnabled = this.mIsEnabled;
                ArrayMap<AccessibilityStateChangeListener, Handler> listeners = new ArrayMap(this.mAccessibilityStateChangeListeners);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyTouchExplorationStateChanged() {
        synchronized (this.mLock) {
            if (this.mTouchExplorationStateChangeListeners.isEmpty()) {
            } else {
                boolean isTouchExplorationEnabled = this.mIsTouchExplorationEnabled;
                ArrayMap<TouchExplorationStateChangeListener, Handler> listeners = new ArrayMap(this.mTouchExplorationStateChangeListeners);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyHighTextContrastStateChanged() {
        synchronized (this.mLock) {
            if (this.mHighTextContrastStateChangeListeners.isEmpty()) {
            } else {
                boolean isHighTextContrastEnabled = this.mIsHighTextContrastEnabled;
                ArrayMap<HighTextContrastChangeListener, Handler> listeners = new ArrayMap(this.mHighTextContrastStateChangeListeners);
            }
        }
    }

    public static boolean isAccessibilityButtonSupported() {
        return Resources.getSystem().getBoolean(R.bool.config_showNavigationBar);
    }

    public boolean isOppoTouchExplorationEnabled() {
        synchronized (this.mLock) {
            if (getServiceLocked() == null) {
                return false;
            }
            boolean z = this.mIsTouchExplorationEnabled;
            return z;
        }
    }

    public boolean isColorDirectEnabled() {
        synchronized (this.mLock) {
            if (getServiceLocked() == null) {
                return false;
            }
            boolean z = this.mIsDirectEnabled;
            return z;
        }
    }
}
