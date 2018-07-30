package com.android.server;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Slog;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.SpellCheckerSubtype;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.textservice.ISpellCheckerService;
import com.android.internal.textservice.ISpellCheckerServiceCallback;
import com.android.internal.textservice.ISpellCheckerSession;
import com.android.internal.textservice.ISpellCheckerSessionListener;
import com.android.internal.textservice.ITextServicesManager.Stub;
import com.android.internal.textservice.ITextServicesSessionListener;
import com.android.internal.util.DumpUtils;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlPullParserException;

public class TextServicesManagerService extends Stub {
    private static final boolean DBG = false;
    private static final String TAG = TextServicesManagerService.class.getSimpleName();
    private final Context mContext;
    private final Object mLock = new Object();
    private final TextServicesMonitor mMonitor;
    private final TextServicesSettings mSettings;
    private final HashMap<String, SpellCheckerBindGroup> mSpellCheckerBindGroups = new HashMap();
    private final ArrayList<SpellCheckerInfo> mSpellCheckerList = new ArrayList();
    private final HashMap<String, SpellCheckerInfo> mSpellCheckerMap = new HashMap();
    private boolean mSystemReady = false;
    private final UserManager mUserManager;

    private static final class ISpellCheckerServiceCallbackBinder extends ISpellCheckerServiceCallback.Stub {
        private final SpellCheckerBindGroup mBindGroup;
        private final SessionRequest mRequest;

        ISpellCheckerServiceCallbackBinder(SpellCheckerBindGroup bindGroup, SessionRequest request) {
            this.mBindGroup = bindGroup;
            this.mRequest = request;
        }

        public void onSessionCreated(ISpellCheckerSession newSession) {
            this.mBindGroup.onSessionCreated(newSession, this.mRequest);
        }
    }

    private static final class InternalDeathRecipients extends RemoteCallbackList<ISpellCheckerSessionListener> {
        private final SpellCheckerBindGroup mGroup;

        public InternalDeathRecipients(SpellCheckerBindGroup group) {
            this.mGroup = group;
        }

        public void onCallbackDied(ISpellCheckerSessionListener listener) {
            this.mGroup.removeListener(listener);
        }
    }

    private final class InternalServiceConnection implements ServiceConnection {
        private final String mSciId;

        public InternalServiceConnection(String id) {
            this.mSciId = id;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (TextServicesManagerService.this.mLock) {
                onServiceConnectedInnerLocked(name, service);
            }
        }

        private void onServiceConnectedInnerLocked(ComponentName name, IBinder service) {
            ISpellCheckerService spellChecker = ISpellCheckerService.Stub.asInterface(service);
            SpellCheckerBindGroup group = (SpellCheckerBindGroup) TextServicesManagerService.this.mSpellCheckerBindGroups.get(this.mSciId);
            if (group != null && this == group.mInternalConnection) {
                group.onServiceConnectedLocked(spellChecker);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (TextServicesManagerService.this.mLock) {
                onServiceDisconnectedInnerLocked(name);
            }
        }

        private void onServiceDisconnectedInnerLocked(ComponentName name) {
            SpellCheckerBindGroup group = (SpellCheckerBindGroup) TextServicesManagerService.this.mSpellCheckerBindGroups.get(this.mSciId);
            if (group != null && this == group.mInternalConnection) {
                group.onServiceDisconnectedLocked();
            }
        }
    }

    public static final class Lifecycle extends SystemService {
        private TextServicesManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new TextServicesManagerService(context);
        }

        public void onStart() {
            publishBinderService("textservices", this.mService);
        }

        public void onSwitchUser(int userHandle) {
            this.mService.onSwitchUser(userHandle);
        }

        public void onBootPhase(int phase) {
            if (phase == SystemService.PHASE_ACTIVITY_MANAGER_READY) {
                this.mService.systemRunning();
            }
        }

        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }
    }

    private static final class SessionRequest {
        public final Bundle mBundle;
        public final String mLocale;
        public final ISpellCheckerSessionListener mScListener;
        public final ITextServicesSessionListener mTsListener;
        public final int mUserId;

        SessionRequest(int userId, String locale, ITextServicesSessionListener tsListener, ISpellCheckerSessionListener scListener, Bundle bundle) {
            this.mUserId = userId;
            this.mLocale = locale;
            this.mTsListener = tsListener;
            this.mScListener = scListener;
            this.mBundle = bundle;
        }
    }

    private final class SpellCheckerBindGroup {
        private final String TAG = SpellCheckerBindGroup.class.getSimpleName();
        private boolean mConnected;
        private final InternalServiceConnection mInternalConnection;
        private final InternalDeathRecipients mListeners;
        private final ArrayList<SessionRequest> mOnGoingSessionRequests = new ArrayList();
        private final ArrayList<SessionRequest> mPendingSessionRequests = new ArrayList();
        private ISpellCheckerService mSpellChecker;
        private boolean mUnbindCalled;

        public SpellCheckerBindGroup(InternalServiceConnection connection) {
            this.mInternalConnection = connection;
            this.mListeners = new InternalDeathRecipients(this);
        }

        public void onServiceConnectedLocked(ISpellCheckerService spellChecker) {
            if (!this.mUnbindCalled) {
                this.mSpellChecker = spellChecker;
                this.mConnected = true;
                try {
                    int size = this.mPendingSessionRequests.size();
                    for (int i = 0; i < size; i++) {
                        SessionRequest request = (SessionRequest) this.mPendingSessionRequests.get(i);
                        this.mSpellChecker.getISpellCheckerSession(request.mLocale, request.mScListener, request.mBundle, new ISpellCheckerServiceCallbackBinder(this, request));
                        this.mOnGoingSessionRequests.add(request);
                    }
                    this.mPendingSessionRequests.clear();
                } catch (RemoteException e) {
                    removeAllLocked();
                }
                cleanLocked();
            }
        }

        public void onServiceDisconnectedLocked() {
            this.mSpellChecker = null;
            this.mConnected = false;
        }

        public void removeListener(ISpellCheckerSessionListener listener) {
            synchronized (TextServicesManagerService.this.mLock) {
                this.mListeners.unregister(listener);
                cleanLocked();
            }
        }

        private void cleanLocked() {
            if (!this.mUnbindCalled && this.mListeners.getRegisteredCallbackCount() <= 0 && this.mPendingSessionRequests.isEmpty() && this.mOnGoingSessionRequests.isEmpty()) {
                String sciId = this.mInternalConnection.mSciId;
                if (((SpellCheckerBindGroup) TextServicesManagerService.this.mSpellCheckerBindGroups.get(sciId)) == this) {
                    TextServicesManagerService.this.mSpellCheckerBindGroups.remove(sciId);
                }
                TextServicesManagerService.this.mContext.unbindService(this.mInternalConnection);
                this.mUnbindCalled = true;
            }
        }

        public void removeAllLocked() {
            Slog.e(this.TAG, "Remove the spell checker bind unexpectedly.");
            for (int i = this.mListeners.getRegisteredCallbackCount() - 1; i >= 0; i--) {
                this.mListeners.unregister((ISpellCheckerSessionListener) this.mListeners.getRegisteredCallbackItem(i));
            }
            this.mPendingSessionRequests.clear();
            this.mOnGoingSessionRequests.clear();
            cleanLocked();
        }

        public void getISpellCheckerSessionOrQueueLocked(SessionRequest request) {
            if (!this.mUnbindCalled) {
                if (this.mConnected) {
                    try {
                        this.mSpellChecker.getISpellCheckerSession(request.mLocale, request.mScListener, request.mBundle, new ISpellCheckerServiceCallbackBinder(this, request));
                        this.mOnGoingSessionRequests.add(request);
                    } catch (RemoteException e) {
                        removeAllLocked();
                    }
                    cleanLocked();
                    return;
                }
                this.mPendingSessionRequests.add(request);
            }
        }

        void onSessionCreated(ISpellCheckerSession newSession, SessionRequest request) {
            synchronized (TextServicesManagerService.this.mLock) {
                if (this.mUnbindCalled) {
                    return;
                }
                if (this.mOnGoingSessionRequests.remove(request)) {
                    try {
                        request.mTsListener.onServiceConnected(newSession);
                        this.mListeners.register(request.mScListener);
                    } catch (RemoteException e) {
                    }
                }
                cleanLocked();
            }
        }
    }

    private final class TextServicesBroadcastReceiver extends BroadcastReceiver {
        /* synthetic */ TextServicesBroadcastReceiver(TextServicesManagerService this$0, TextServicesBroadcastReceiver -this1) {
            this();
        }

        private TextServicesBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_ADDED".equals(action) || "android.intent.action.USER_REMOVED".equals(action)) {
                TextServicesManagerService.this.updateCurrentProfileIds();
            } else {
                Slog.w(TextServicesManagerService.TAG, "Unexpected intent " + intent);
            }
        }
    }

    private final class TextServicesMonitor extends PackageMonitor {
        /* synthetic */ TextServicesMonitor(TextServicesManagerService this$0, TextServicesMonitor -this1) {
            this();
        }

        private TextServicesMonitor() {
        }

        private boolean isChangingPackagesOfCurrentUser() {
            return getChangingUserId() == TextServicesManagerService.this.mSettings.getCurrentUserId();
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onSomePackagesChanged() {
            if (isChangingPackagesOfCurrentUser()) {
                synchronized (TextServicesManagerService.this.mLock) {
                    SpellCheckerInfo sci = TextServicesManagerService.this.getCurrentSpellChecker(null);
                    TextServicesManagerService.buildSpellCheckerMapLocked(TextServicesManagerService.this.mContext, TextServicesManagerService.this.mSpellCheckerList, TextServicesManagerService.this.mSpellCheckerMap, TextServicesManagerService.this.mSettings);
                    if (!TextServicesManagerService.this.isSpellCheckerEnabledLocked()) {
                    } else if (sci == null) {
                        TextServicesManagerService.this.setCurrentSpellCheckerLocked(TextServicesManagerService.this.findAvailSystemSpellCheckerLocked(null));
                    } else {
                        String packageName = sci.getPackageName();
                        int change = isPackageDisappearing(packageName);
                        if (!(change == 3 || change == 2)) {
                        }
                        SpellCheckerInfo availSci = TextServicesManagerService.this.findAvailSystemSpellCheckerLocked(packageName);
                        if (!(availSci == null || (availSci.getId().equals(sci.getId()) ^ 1) == 0)) {
                            TextServicesManagerService.this.setCurrentSpellCheckerLocked(availSci);
                        }
                    }
                }
            }
        }
    }

    private static final class TextServicesSettings {
        private boolean mCopyOnWrite = false;
        private final HashMap<String, String> mCopyOnWriteDataStore = new HashMap();
        @GuardedBy("mLock")
        private int[] mCurrentProfileIds = new int[0];
        private int mCurrentUserId;
        private Object mLock = new Object();
        private final ContentResolver mResolver;

        public TextServicesSettings(ContentResolver resolver, int userId, boolean copyOnWrite) {
            this.mResolver = resolver;
            switchCurrentUser(userId, copyOnWrite);
        }

        public void switchCurrentUser(int userId, boolean copyOnWrite) {
            if (!(this.mCurrentUserId == userId && this.mCopyOnWrite == copyOnWrite)) {
                this.mCopyOnWriteDataStore.clear();
            }
            this.mCurrentUserId = userId;
            this.mCopyOnWrite = copyOnWrite;
        }

        private void putString(String key, String str) {
            if (this.mCopyOnWrite) {
                this.mCopyOnWriteDataStore.put(key, str);
            } else {
                Secure.putStringForUser(this.mResolver, key, str, this.mCurrentUserId);
            }
        }

        private String getString(String key, String defaultValue) {
            String result;
            if (this.mCopyOnWrite && this.mCopyOnWriteDataStore.containsKey(key)) {
                result = (String) this.mCopyOnWriteDataStore.get(key);
            } else {
                result = Secure.getStringForUser(this.mResolver, key, this.mCurrentUserId);
            }
            if (result != null) {
                return result;
            }
            return defaultValue;
        }

        private void putInt(String key, int value) {
            if (this.mCopyOnWrite) {
                this.mCopyOnWriteDataStore.put(key, String.valueOf(value));
            } else {
                Secure.putIntForUser(this.mResolver, key, value, this.mCurrentUserId);
            }
        }

        private int getInt(String key, int defaultValue) {
            if (!this.mCopyOnWrite || !this.mCopyOnWriteDataStore.containsKey(key)) {
                return Secure.getIntForUser(this.mResolver, key, defaultValue, this.mCurrentUserId);
            }
            String result = (String) this.mCopyOnWriteDataStore.get(key);
            return result != null ? Integer.parseInt(result) : 0;
        }

        private boolean getBoolean(String key, boolean defaultValue) {
            return getInt(key, defaultValue ? 1 : 0) == 1;
        }

        public void setCurrentProfileIds(int[] currentProfileIds) {
            synchronized (this.mLock) {
                this.mCurrentProfileIds = currentProfileIds;
            }
        }

        public boolean isCurrentProfile(int userId) {
            synchronized (this.mLock) {
                if (userId == this.mCurrentUserId) {
                    return true;
                }
                for (int i : this.mCurrentProfileIds) {
                    if (userId == i) {
                        return true;
                    }
                }
                return false;
            }
        }

        public int getCurrentUserId() {
            return this.mCurrentUserId;
        }

        public void putSelectedSpellChecker(String sciId) {
            putString("selected_spell_checker", sciId);
        }

        public void putSelectedSpellCheckerSubtype(int hashCode) {
            putInt("selected_spell_checker_subtype", hashCode);
        }

        public String getSelectedSpellChecker() {
            return getString("selected_spell_checker", "");
        }

        public int getSelectedSpellCheckerSubtype(int defaultValue) {
            return getInt("selected_spell_checker_subtype", defaultValue);
        }

        public boolean isSpellCheckerEnabled() {
            return getBoolean("spell_checker_enabled", true);
        }

        public void dumpLocked(PrintWriter pw, String prefix) {
            pw.println(prefix + "mCurrentUserId=" + this.mCurrentUserId);
            pw.println(prefix + "mCurrentProfileIds=" + Arrays.toString(this.mCurrentProfileIds));
            pw.println(prefix + "mCopyOnWrite=" + this.mCopyOnWrite);
        }
    }

    void systemRunning() {
        synchronized (this.mLock) {
            if (!this.mSystemReady) {
                this.mSystemReady = true;
                resetInternalState(this.mSettings.getCurrentUserId());
            }
        }
    }

    void onSwitchUser(int userId) {
        synchronized (this.mLock) {
            resetInternalState(userId);
        }
    }

    void onUnlockUser(int userId) {
        synchronized (this.mLock) {
            int currentUserId = this.mSettings.getCurrentUserId();
            if (userId != currentUserId) {
                return;
            }
            resetInternalState(currentUserId);
        }
    }

    public TextServicesManagerService(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction("android.intent.action.USER_ADDED");
        broadcastFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new TextServicesBroadcastReceiver(), broadcastFilter);
        int userId = 0;
        try {
            userId = ActivityManager.getService().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
        }
        this.mMonitor = new TextServicesMonitor();
        this.mMonitor.register(context, null, true);
        this.mSettings = new TextServicesSettings(context.getContentResolver(), userId, this.mSystemReady ? this.mUserManager.isUserUnlockingOrUnlocked(userId) ^ 1 : true);
        resetInternalState(userId);
    }

    private void resetInternalState(int userId) {
        this.mSettings.switchCurrentUser(userId, this.mSystemReady ? this.mUserManager.isUserUnlockingOrUnlocked(userId) ^ 1 : true);
        updateCurrentProfileIds();
        unbindServiceLocked();
        buildSpellCheckerMapLocked(this.mContext, this.mSpellCheckerList, this.mSpellCheckerMap, this.mSettings);
        if (getCurrentSpellChecker(null) == null) {
            setCurrentSpellCheckerLocked(findAvailSystemSpellCheckerLocked(null));
        }
    }

    void updateCurrentProfileIds() {
        this.mSettings.setCurrentProfileIds(this.mUserManager.getProfileIdsWithDisabled(this.mSettings.getCurrentUserId()));
    }

    private static void buildSpellCheckerMapLocked(Context context, ArrayList<SpellCheckerInfo> list, HashMap<String, SpellCheckerInfo> map, TextServicesSettings settings) {
        list.clear();
        map.clear();
        List<ResolveInfo> services = context.getPackageManager().queryIntentServicesAsUser(new Intent("android.service.textservice.SpellCheckerService"), 128, settings.getCurrentUserId());
        int N = services.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo ri = (ResolveInfo) services.get(i);
            ServiceInfo si = ri.serviceInfo;
            ComponentName compName = new ComponentName(si.packageName, si.name);
            if ("android.permission.BIND_TEXT_SERVICE".equals(si.permission)) {
                try {
                    SpellCheckerInfo sci = new SpellCheckerInfo(context, ri);
                    if (sci.getSubtypeCount() <= 0) {
                        Slog.w(TAG, "Skipping text service " + compName + ": it does not contain subtypes.");
                    } else {
                        list.add(sci);
                        map.put(sci.getId(), sci);
                    }
                } catch (XmlPullParserException e) {
                    Slog.w(TAG, "Unable to load the spell checker " + compName, e);
                } catch (IOException e2) {
                    Slog.w(TAG, "Unable to load the spell checker " + compName, e2);
                }
            } else {
                Slog.w(TAG, "Skipping text service " + compName + ": it does not require the permission " + "android.permission.BIND_TEXT_SERVICE");
            }
        }
    }

    private boolean calledFromValidUser() {
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (uid == 1000 || userId == this.mSettings.getCurrentUserId()) {
            return true;
        }
        boolean isCurrentProfile = this.mSettings.isCurrentProfile(userId);
        if (this.mSettings.isCurrentProfile(userId)) {
            SpellCheckerInfo spellCheckerInfo = getCurrentSpellCheckerWithoutVerification();
            if (spellCheckerInfo != null) {
                if ((spellCheckerInfo.getServiceInfo().applicationInfo.flags & 1) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean bindCurrentSpellCheckerService(Intent service, ServiceConnection conn, int flags) {
        if (service != null && conn != null) {
            return this.mContext.bindServiceAsUser(service, conn, flags, new UserHandle(this.mSettings.getCurrentUserId()));
        }
        Slog.e(TAG, "--- bind failed: service = " + service + ", conn = " + conn);
        return false;
    }

    private void unbindServiceLocked() {
        for (SpellCheckerBindGroup scbg : this.mSpellCheckerBindGroups.values()) {
            scbg.removeAllLocked();
        }
        this.mSpellCheckerBindGroups.clear();
    }

    private SpellCheckerInfo findAvailSystemSpellCheckerLocked(String prefPackage) {
        SpellCheckerInfo sci;
        ArrayList<SpellCheckerInfo> spellCheckerList = new ArrayList();
        for (SpellCheckerInfo sci2 : this.mSpellCheckerList) {
            if ((sci2.getServiceInfo().applicationInfo.flags & 1) != 0) {
                spellCheckerList.add(sci2);
            }
        }
        int spellCheckersCount = spellCheckerList.size();
        if (spellCheckersCount == 0) {
            Slog.w(TAG, "no available spell checker services found");
            return null;
        }
        if (prefPackage != null) {
            for (int i = 0; i < spellCheckersCount; i++) {
                sci2 = (SpellCheckerInfo) spellCheckerList.get(i);
                if (prefPackage.equals(sci2.getPackageName())) {
                    return sci2;
                }
            }
        }
        ArrayList<Locale> suitableLocales = InputMethodUtils.getSuitableLocalesForSpellChecker(this.mContext.getResources().getConfiguration().locale);
        int localeCount = suitableLocales.size();
        for (int localeIndex = 0; localeIndex < localeCount; localeIndex++) {
            Locale locale = (Locale) suitableLocales.get(localeIndex);
            for (int spellCheckersIndex = 0; spellCheckersIndex < spellCheckersCount; spellCheckersIndex++) {
                SpellCheckerInfo info = (SpellCheckerInfo) spellCheckerList.get(spellCheckersIndex);
                int subtypeCount = info.getSubtypeCount();
                for (int subtypeIndex = 0; subtypeIndex < subtypeCount; subtypeIndex++) {
                    if (locale.equals(InputMethodUtils.constructLocaleFromString(info.getSubtypeAt(subtypeIndex).getLocale()))) {
                        return info;
                    }
                }
            }
        }
        if (spellCheckersCount > 1) {
            Slog.w(TAG, "more than one spell checker service found, picking first");
        }
        return (SpellCheckerInfo) spellCheckerList.get(0);
    }

    public SpellCheckerInfo getCurrentSpellChecker(String locale) {
        if (calledFromValidUser()) {
            return getCurrentSpellCheckerWithoutVerification();
        }
        return null;
    }

    private SpellCheckerInfo getCurrentSpellCheckerWithoutVerification() {
        synchronized (this.mLock) {
            String curSpellCheckerId = this.mSettings.getSelectedSpellChecker();
            if (TextUtils.isEmpty(curSpellCheckerId)) {
                return null;
            }
            SpellCheckerInfo spellCheckerInfo = (SpellCheckerInfo) this.mSpellCheckerMap.get(curSpellCheckerId);
            return spellCheckerInfo;
        }
    }

    public SpellCheckerSubtype getCurrentSpellCheckerSubtype(String locale, boolean allowImplicitlySelectedSubtype) {
        if (!calledFromValidUser()) {
            return null;
        }
        int subtypeHashCode;
        SpellCheckerInfo sci;
        Locale systemLocale;
        synchronized (this.mLock) {
            subtypeHashCode = this.mSettings.getSelectedSpellCheckerSubtype(0);
            sci = getCurrentSpellChecker(null);
            systemLocale = this.mContext.getResources().getConfiguration().locale;
        }
        if (sci == null || sci.getSubtypeCount() == 0) {
            return null;
        }
        if (subtypeHashCode == 0 && (allowImplicitlySelectedSubtype ^ 1) != 0) {
            return null;
        }
        String candidateLocale = null;
        if (subtypeHashCode == 0) {
            InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService(InputMethodManager.class);
            if (imm != null) {
                InputMethodSubtype currentInputMethodSubtype = imm.getCurrentInputMethodSubtype();
                if (currentInputMethodSubtype != null) {
                    String localeString = currentInputMethodSubtype.getLocale();
                    if (!TextUtils.isEmpty(localeString)) {
                        candidateLocale = localeString;
                    }
                }
            }
            if (candidateLocale == null) {
                candidateLocale = systemLocale.toString();
            }
        }
        SpellCheckerSubtype candidate = null;
        for (int i = 0; i < sci.getSubtypeCount(); i++) {
            SpellCheckerSubtype scs = sci.getSubtypeAt(i);
            if (subtypeHashCode == 0) {
                String scsLocale = scs.getLocale();
                if (candidateLocale.equals(scsLocale)) {
                    return scs;
                }
                if (candidate == null && candidateLocale.length() >= 2 && scsLocale.length() >= 2 && candidateLocale.startsWith(scsLocale)) {
                    candidate = scs;
                }
            } else if (scs.hashCode() == subtypeHashCode) {
                return scs;
            }
        }
        return candidate;
    }

    public void getSpellCheckerService(String sciId, String locale, ITextServicesSessionListener tsListener, ISpellCheckerSessionListener scListener, Bundle bundle) {
        if (!calledFromValidUser() || !this.mSystemReady) {
            return;
        }
        if (TextUtils.isEmpty(sciId) || tsListener == null || scListener == null) {
            Slog.e(TAG, "getSpellCheckerService: Invalid input.");
            return;
        }
        synchronized (this.mLock) {
            if (this.mSpellCheckerMap.containsKey(sciId)) {
                SpellCheckerInfo sci = (SpellCheckerInfo) this.mSpellCheckerMap.get(sciId);
                SpellCheckerBindGroup bindGroup = (SpellCheckerBindGroup) this.mSpellCheckerBindGroups.get(sciId);
                int uid = Binder.getCallingUid();
                if (bindGroup == null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        bindGroup = startSpellCheckerServiceInnerLocked(sci);
                        Binder.restoreCallingIdentity(ident);
                        if (bindGroup == null) {
                            return;
                        }
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
                bindGroup.getISpellCheckerSessionOrQueueLocked(new SessionRequest(uid, locale, tsListener, scListener, bundle));
                return;
            }
        }
    }

    public boolean isSpellCheckerEnabled() {
        if (!calledFromValidUser()) {
            return false;
        }
        boolean isSpellCheckerEnabledLocked;
        synchronized (this.mLock) {
            isSpellCheckerEnabledLocked = isSpellCheckerEnabledLocked();
        }
        return isSpellCheckerEnabledLocked;
    }

    private SpellCheckerBindGroup startSpellCheckerServiceInnerLocked(SpellCheckerInfo info) {
        String sciId = info.getId();
        InternalServiceConnection connection = new InternalServiceConnection(sciId);
        Intent serviceIntent = new Intent("android.service.textservice.SpellCheckerService");
        serviceIntent.setComponent(info.getComponent());
        if (bindCurrentSpellCheckerService(serviceIntent, connection, 8388609)) {
            SpellCheckerBindGroup group = new SpellCheckerBindGroup(connection);
            this.mSpellCheckerBindGroups.put(sciId, group);
            return group;
        }
        Slog.e(TAG, "Failed to get a spell checker service.");
        return null;
    }

    public SpellCheckerInfo[] getEnabledSpellCheckers() {
        if (calledFromValidUser()) {
            return (SpellCheckerInfo[]) this.mSpellCheckerList.toArray(new SpellCheckerInfo[this.mSpellCheckerList.size()]);
        }
        return null;
    }

    public void finishSpellCheckerService(ISpellCheckerSessionListener listener) {
        if (calledFromValidUser()) {
            synchronized (this.mLock) {
                ArrayList<SpellCheckerBindGroup> removeList = new ArrayList();
                for (SpellCheckerBindGroup group : this.mSpellCheckerBindGroups.values()) {
                    if (group != null) {
                        removeList.add(group);
                    }
                }
                int removeSize = removeList.size();
                for (int i = 0; i < removeSize; i++) {
                    ((SpellCheckerBindGroup) removeList.get(i)).removeListener(listener);
                }
            }
        }
    }

    private void setCurrentSpellCheckerLocked(SpellCheckerInfo sci) {
        String sciId = sci != null ? sci.getId() : "";
        long ident = Binder.clearCallingIdentity();
        try {
            this.mSettings.putSelectedSpellChecker(sciId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void setCurrentSpellCheckerSubtypeLocked(int hashCode) {
        SpellCheckerInfo sci = getCurrentSpellChecker(null);
        int tempHashCode = 0;
        int i = 0;
        while (sci != null && i < sci.getSubtypeCount()) {
            if (sci.getSubtypeAt(i).hashCode() == hashCode) {
                tempHashCode = hashCode;
                break;
            }
            i++;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mSettings.putSelectedSpellCheckerSubtype(tempHashCode);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isSpellCheckerEnabledLocked() {
        long ident = Binder.clearCallingIdentity();
        try {
            boolean retval = this.mSettings.isSpellCheckerEnabled();
            return retval;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLock) {
                pw.println("Current Text Services Manager state:");
                pw.println("  Spell Checkers:");
                int spellCheckerIndex = 0;
                for (SpellCheckerInfo info : this.mSpellCheckerMap.values()) {
                    pw.println("  Spell Checker #" + spellCheckerIndex);
                    info.dump(pw, "    ");
                    spellCheckerIndex++;
                }
                pw.println("");
                pw.println("  Spell Checker Bind Groups:");
                for (Entry<String, SpellCheckerBindGroup> ent : this.mSpellCheckerBindGroups.entrySet()) {
                    int i;
                    SessionRequest req;
                    SpellCheckerBindGroup grp = (SpellCheckerBindGroup) ent.getValue();
                    pw.println("    " + ((String) ent.getKey()) + " " + grp + ":");
                    pw.println("      mInternalConnection=" + grp.mInternalConnection);
                    pw.println("      mSpellChecker=" + grp.mSpellChecker);
                    pw.println("      mUnbindCalled=" + grp.mUnbindCalled);
                    pw.println("      mConnected=" + grp.mConnected);
                    int numPendingSessionRequests = grp.mPendingSessionRequests.size();
                    for (i = 0; i < numPendingSessionRequests; i++) {
                        req = (SessionRequest) grp.mPendingSessionRequests.get(i);
                        pw.println("      Pending Request #" + i + ":");
                        pw.println("        mTsListener=" + req.mTsListener);
                        pw.println("        mScListener=" + req.mScListener);
                        pw.println("        mScLocale=" + req.mLocale + " mUid=" + req.mUserId);
                    }
                    int numOnGoingSessionRequests = grp.mOnGoingSessionRequests.size();
                    i = 0;
                    while (i < numOnGoingSessionRequests) {
                        req = (SessionRequest) grp.mOnGoingSessionRequests.get(i);
                        pw.println("      On going Request #" + i + ":");
                        i++;
                        pw.println("        mTsListener=" + req.mTsListener);
                        pw.println("        mScListener=" + req.mScListener);
                        pw.println("        mScLocale=" + req.mLocale + " mUid=" + req.mUserId);
                        i++;
                    }
                    int N = grp.mListeners.getRegisteredCallbackCount();
                    for (i = 0; i < N; i++) {
                        ISpellCheckerSessionListener mScListener = (ISpellCheckerSessionListener) grp.mListeners.getRegisteredCallbackItem(i);
                        pw.println("      Listener #" + i + ":");
                        pw.println("        mScListener=" + mScListener);
                        pw.println("        mGroup=" + grp);
                    }
                }
                pw.println("");
                pw.println("  mSettings:");
                this.mSettings.dumpLocked(pw, "    ");
            }
        }
    }

    private static String getStackTrace() {
        StringBuilder sb = new StringBuilder();
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            StackTraceElement[] frames = e.getStackTrace();
            for (int j = 1; j < frames.length; j++) {
                sb.append(frames[j].toString()).append("\n");
            }
            return sb.toString();
        }
    }
}
