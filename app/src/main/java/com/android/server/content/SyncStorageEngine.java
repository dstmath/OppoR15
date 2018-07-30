package com.android.server.content;

import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountManager;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncStatusObserver;
import android.content.PeriodicSync;
import android.content.SyncInfo;
import android.content.SyncRequest.Builder;
import android.content.SyncStatusInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.audio.AudioService;
import com.android.server.secrecy.policy.DecryptTool;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SyncStorageEngine extends Handler {
    private static final int ACCOUNTS_VERSION = 3;
    private static final double DEFAULT_FLEX_PERCENT_SYNC = 0.04d;
    private static final long DEFAULT_MIN_FLEX_ALLOWED_SECS = 5;
    private static final long DEFAULT_POLL_FREQUENCY_SECONDS = 86400;
    public static final int EVENT_START = 0;
    public static final int EVENT_STOP = 1;
    public static final int MAX_HISTORY = 100;
    public static final String MESG_CANCELED = "canceled";
    public static final String MESG_SUCCESS = "success";
    static final long MILLIS_IN_4WEEKS = 2419200000L;
    private static final int MSG_WRITE_STATISTICS = 2;
    private static final int MSG_WRITE_STATUS = 1;
    public static final long NOT_IN_BACKOFF_MODE = -1;
    public static final String[] SOURCES = new String[]{"SERVER", "LOCAL", "POLL", "USER", "PERIODIC", "SERVICE"};
    public static final int SOURCE_LOCAL = 1;
    public static final int SOURCE_PERIODIC = 4;
    public static final int SOURCE_POLL = 2;
    public static final int SOURCE_SERVER = 0;
    public static final int SOURCE_USER = 3;
    public static final int STATISTICS_FILE_END = 0;
    public static final int STATISTICS_FILE_ITEM = 101;
    public static final int STATISTICS_FILE_ITEM_OLD = 100;
    public static final int STATUS_FILE_END = 0;
    public static final int STATUS_FILE_ITEM = 100;
    private static final boolean SYNC_ENABLED_DEFAULT = false;
    private static final String TAG = "SyncManager";
    private static final String TAG_FILE = "SyncManagerFile";
    private static final long WRITE_STATISTICS_DELAY = 1800000;
    private static final long WRITE_STATUS_DELAY = 600000;
    private static final String XML_ATTR_ENABLED = "enabled";
    private static final String XML_ATTR_LISTEN_FOR_TICKLES = "listen-for-tickles";
    private static final String XML_ATTR_NEXT_AUTHORITY_ID = "nextAuthorityId";
    private static final String XML_ATTR_SYNC_RANDOM_OFFSET = "offsetInSeconds";
    private static final String XML_ATTR_USER = "user";
    private static final String XML_TAG_LISTEN_FOR_TICKLES = "listenForTickles";
    private static PeriodicSyncAddedListener mPeriodicSyncAddedListener;
    private static HashMap<String, String> sAuthorityRenames = new HashMap();
    private static volatile SyncStorageEngine sSyncStorageEngine = null;
    private final AtomicFile mAccountInfoFile;
    private final HashMap<AccountAndUser, AccountInfo> mAccounts = new HashMap();
    private final SparseArray<AuthorityInfo> mAuthorities = new SparseArray();
    private OnAuthorityRemovedListener mAuthorityRemovedListener;
    private final Calendar mCal;
    private final RemoteCallbackList<ISyncStatusObserver> mChangeListeners = new RemoteCallbackList();
    private final Context mContext;
    private final SparseArray<ArrayList<SyncInfo>> mCurrentSyncs = new SparseArray();
    private final DayStats[] mDayStats = new DayStats[28];
    private boolean mDefaultMasterSyncAutomatically;
    private boolean mGrantSyncAdaptersAccountAccess;
    private SparseArray<Boolean> mMasterSyncAutomatically = new SparseArray();
    private int mNextAuthorityId = 0;
    private int mNextHistoryId = 0;
    private final ArrayMap<ComponentName, SparseArray<AuthorityInfo>> mServices = new ArrayMap();
    private final AtomicFile mStatisticsFile;
    private final AtomicFile mStatusFile;
    private final ArrayList<SyncHistoryItem> mSyncHistory = new ArrayList();
    private int mSyncRandomOffset;
    private OnSyncRequestListener mSyncRequestListener;
    private final SparseArray<SyncStatusInfo> mSyncStatus = new SparseArray();
    private int mYear;
    private int mYearInDays;

    interface PeriodicSyncAddedListener {
        void onPeriodicSyncAdded(EndPoint endPoint, Bundle bundle, long j, long j2);
    }

    interface OnAuthorityRemovedListener {
        void onAuthorityRemoved(EndPoint endPoint);
    }

    interface OnSyncRequestListener {
        void onSyncRequest(EndPoint endPoint, int i, Bundle bundle);
    }

    private static class AccountAuthorityValidator {
        private final AccountManager mAccountManager;
        private final SparseArray<Account[]> mAccountsCache = new SparseArray();
        private final PackageManager mPackageManager;
        private final SparseArray<ArrayMap<String, Boolean>> mProvidersPerUserCache = new SparseArray();

        AccountAuthorityValidator(Context context) {
            this.mAccountManager = (AccountManager) context.getSystemService(AccountManager.class);
            this.mPackageManager = context.getPackageManager();
        }

        boolean isAccountValid(Account account, int userId) {
            Account[] accountsForUser = (Account[]) this.mAccountsCache.get(userId);
            if (accountsForUser == null) {
                accountsForUser = this.mAccountManager.getAccountsAsUser(userId);
                this.mAccountsCache.put(userId, accountsForUser);
            }
            return ArrayUtils.contains(accountsForUser, account);
        }

        boolean isAuthorityValid(String authority, int userId) {
            ArrayMap<String, Boolean> authorityMap = (ArrayMap) this.mProvidersPerUserCache.get(userId);
            if (authorityMap == null) {
                authorityMap = new ArrayMap();
                this.mProvidersPerUserCache.put(userId, authorityMap);
            }
            if (!authorityMap.containsKey(authority)) {
                authorityMap.put(authority, Boolean.valueOf(this.mPackageManager.resolveContentProviderAsUser(authority, 786432, userId) != null));
            }
            return ((Boolean) authorityMap.get(authority)).booleanValue();
        }
    }

    static class AccountInfo {
        final AccountAndUser accountAndUser;
        final HashMap<String, AuthorityInfo> authorities = new HashMap();

        AccountInfo(AccountAndUser accountAndUser) {
            this.accountAndUser = accountAndUser;
        }
    }

    public static class AuthorityInfo {
        public static final int NOT_INITIALIZED = -1;
        public static final int NOT_SYNCABLE = 0;
        public static final int SYNCABLE = 1;
        public static final int SYNCABLE_NOT_INITIALIZED = 2;
        public static final int SYNCABLE_NO_ACCOUNT_ACCESS = 3;
        public static final int UNDEFINED = -2;
        long backoffDelay;
        long backoffTime;
        long delayUntil;
        boolean enabled;
        final int ident;
        final ArrayList<PeriodicSync> periodicSyncs;
        int syncable;
        final EndPoint target;

        AuthorityInfo(AuthorityInfo toCopy) {
            this.target = toCopy.target;
            this.ident = toCopy.ident;
            this.enabled = toCopy.enabled;
            this.syncable = toCopy.syncable;
            this.backoffTime = toCopy.backoffTime;
            this.backoffDelay = toCopy.backoffDelay;
            this.delayUntil = toCopy.delayUntil;
            this.periodicSyncs = new ArrayList();
            for (PeriodicSync sync : toCopy.periodicSyncs) {
                this.periodicSyncs.add(new PeriodicSync(sync));
            }
        }

        AuthorityInfo(EndPoint info, int id) {
            this.target = info;
            this.ident = id;
            this.enabled = false;
            this.periodicSyncs = new ArrayList();
            defaultInitialisation();
        }

        private void defaultInitialisation() {
            this.syncable = -1;
            this.backoffTime = -1;
            this.backoffDelay = -1;
            if (SyncStorageEngine.mPeriodicSyncAddedListener != null) {
                SyncStorageEngine.mPeriodicSyncAddedListener.onPeriodicSyncAdded(this.target, new Bundle(), SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS, SyncStorageEngine.calculateDefaultFlexTime(SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS));
            }
        }

        public String toString() {
            return this.target + ", enabled=" + this.enabled + ", syncable=" + this.syncable + ", backoff=" + this.backoffTime + ", delay=" + this.delayUntil;
        }
    }

    public static class DayStats {
        public final int day;
        public int failureCount;
        public long failureTime;
        public int successCount;
        public long successTime;

        public DayStats(int day) {
            this.day = day;
        }
    }

    public static class EndPoint {
        public static final EndPoint USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL = new EndPoint(null, null, -1);
        final Account account;
        final String provider;
        final int userId;

        public EndPoint(Account account, String provider, int userId) {
            this.account = account;
            this.provider = provider;
            this.userId = userId;
        }

        public boolean matchesSpec(EndPoint spec) {
            if (this.userId != spec.userId && this.userId != -1 && spec.userId != -1) {
                return false;
            }
            boolean accountsMatch;
            boolean providersMatch;
            if (spec.account == null) {
                accountsMatch = true;
            } else {
                accountsMatch = this.account.equals(spec.account);
            }
            if (spec.provider == null) {
                providersMatch = true;
            } else {
                providersMatch = this.provider.equals(spec.provider);
            }
            if (!accountsMatch) {
                providersMatch = false;
            }
            return providersMatch;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.account == null ? "ALL ACCS" : this.account.name).append("/").append(this.provider == null ? "ALL PDRS" : this.provider);
            sb.append(":u").append(this.userId);
            return sb.toString();
        }
    }

    public static class SyncHistoryItem {
        int authorityId;
        long downstreamActivity;
        long elapsedTime;
        int event;
        long eventTime;
        Bundle extras;
        int historyId;
        boolean initialization;
        String mesg;
        int reason;
        int source;
        long upstreamActivity;
    }

    static {
        sAuthorityRenames.put("contacts", "com.android.contacts");
        sAuthorityRenames.put("calendar", "com.android.calendar");
    }

    private SyncStorageEngine(Context context, File dataDir) {
        this.mContext = context;
        sSyncStorageEngine = this;
        this.mCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
        this.mDefaultMasterSyncAutomatically = this.mContext.getResources().getBoolean(17957041);
        File syncDir = new File(new File(dataDir, "system"), "sync");
        syncDir.mkdirs();
        maybeDeleteLegacyPendingInfoLocked(syncDir);
        this.mAccountInfoFile = new AtomicFile(new File(syncDir, "accounts.xml"));
        this.mStatusFile = new AtomicFile(new File(syncDir, "status.bin"));
        this.mStatisticsFile = new AtomicFile(new File(syncDir, "stats.bin"));
        readAccountInfoLocked();
        readStatusLocked();
        readStatisticsLocked();
        readAndDeleteLegacyAccountInfoLocked();
        writeAccountInfoLocked();
        writeStatusLocked();
        writeStatisticsLocked();
    }

    public static SyncStorageEngine newTestInstance(Context context) {
        return new SyncStorageEngine(context, context.getFilesDir());
    }

    public static void init(Context context) {
        if (sSyncStorageEngine == null) {
            sSyncStorageEngine = new SyncStorageEngine(context, Environment.getDataDirectory());
        }
    }

    public static SyncStorageEngine getSingleton() {
        if (sSyncStorageEngine != null) {
            return sSyncStorageEngine;
        }
        throw new IllegalStateException("not initialized");
    }

    protected void setOnSyncRequestListener(OnSyncRequestListener listener) {
        if (this.mSyncRequestListener == null) {
            this.mSyncRequestListener = listener;
        }
    }

    protected void setOnAuthorityRemovedListener(OnAuthorityRemovedListener listener) {
        if (this.mAuthorityRemovedListener == null) {
            this.mAuthorityRemovedListener = listener;
        }
    }

    protected void setPeriodicSyncAddedListener(PeriodicSyncAddedListener listener) {
        if (mPeriodicSyncAddedListener == null) {
            mPeriodicSyncAddedListener = listener;
        }
    }

    public void handleMessage(Message msg) {
        SparseArray sparseArray;
        if (msg.what == 1) {
            sparseArray = this.mAuthorities;
            synchronized (sparseArray) {
                writeStatusLocked();
            }
        } else if (msg.what == 2) {
            sparseArray = this.mAuthorities;
            synchronized (sparseArray) {
                writeStatisticsLocked();
            }
        } else {
            return;
        }
    }

    public int getSyncRandomOffset() {
        return this.mSyncRandomOffset;
    }

    public void addStatusChangeListener(int mask, ISyncStatusObserver callback) {
        synchronized (this.mAuthorities) {
            this.mChangeListeners.register(callback, Integer.valueOf(mask));
        }
    }

    public void removeStatusChangeListener(ISyncStatusObserver callback) {
        synchronized (this.mAuthorities) {
            this.mChangeListeners.unregister(callback);
        }
    }

    public static long calculateDefaultFlexTime(long syncTimeSeconds) {
        if (syncTimeSeconds < DEFAULT_MIN_FLEX_ALLOWED_SECS) {
            return 0;
        }
        if (syncTimeSeconds < DEFAULT_POLL_FREQUENCY_SECONDS) {
            return (long) (((double) syncTimeSeconds) * DEFAULT_FLEX_PERCENT_SYNC);
        }
        return 3456;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void reportChange(int which) {
        Throwable th;
        synchronized (this.mAuthorities) {
            try {
                int i = this.mChangeListeners.beginBroadcast();
                ArrayList<ISyncStatusObserver> reports = null;
                while (i > 0) {
                    i--;
                    ArrayList<ISyncStatusObserver> reports2;
                    try {
                        if ((((Integer) this.mChangeListeners.getBroadcastCookie(i)).intValue() & which) != 0) {
                            if (reports == null) {
                                reports2 = new ArrayList(i);
                            } else {
                                reports2 = reports;
                            }
                            reports2.add((ISyncStatusObserver) this.mChangeListeners.getBroadcastItem(i));
                            reports = reports2;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        reports2 = reports;
                        throw th;
                    }
                }
                this.mChangeListeners.finishBroadcast();
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getSyncAutomatically(Account account, int userId, String providerName) {
        boolean z = false;
        synchronized (this.mAuthorities) {
            if (account != null) {
                AuthorityInfo authority = getAuthorityLocked(new EndPoint(account, providerName, userId), "getSyncAutomatically");
                if (authority != null) {
                    z = authority.enabled;
                }
            } else {
                int i = this.mAuthorities.size();
                while (i > 0) {
                    i--;
                    AuthorityInfo authorityInfo = (AuthorityInfo) this.mAuthorities.valueAt(i);
                    if (authorityInfo.target.matchesSpec(new EndPoint(account, providerName, userId)) && authorityInfo.enabled) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setSyncAutomatically(Account account, int userId, String providerName, boolean sync) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.d("SyncManager", "setSyncAutomatically:  provider " + providerName + ", user " + userId + " -> " + sync);
        }
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(new EndPoint(account, providerName, userId), -1, false);
            if (authority.enabled != sync) {
                if (sync) {
                    if (authority.syncable == 2) {
                        authority.syncable = -1;
                    }
                }
                authority.enabled = sync;
                writeAccountInfoLocked();
            } else if (Log.isLoggable("SyncManager", 2)) {
                Slog.d("SyncManager", "setSyncAutomatically: already set to " + sync + ", doing nothing");
            }
        }
    }

    public int getIsSyncable(Account account, int userId, String providerName) {
        synchronized (this.mAuthorities) {
            int i;
            if (account != null) {
                AuthorityInfo authority = getAuthorityLocked(new EndPoint(account, providerName, userId), "get authority syncable");
                if (authority == null) {
                    return -1;
                }
                i = authority.syncable;
                return i;
            }
            int i2 = this.mAuthorities.size();
            while (i2 > 0) {
                i2--;
                AuthorityInfo authorityInfo = (AuthorityInfo) this.mAuthorities.valueAt(i2);
                if (authorityInfo.target != null && authorityInfo.target.provider.equals(providerName)) {
                    i = authorityInfo.syncable;
                    return i;
                }
            }
            return -1;
        }
    }

    public void setIsSyncable(Account account, int userId, String providerName, int syncable) {
        setSyncableStateForEndPoint(new EndPoint(account, providerName, userId), syncable);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setSyncableStateForEndPoint(EndPoint target, int syncable) {
        synchronized (this.mAuthorities) {
            AuthorityInfo aInfo = getOrCreateAuthorityLocked(target, -1, false);
            if (syncable < -1) {
                syncable = -1;
            }
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.d("SyncManager", "setIsSyncable: " + aInfo.toString() + " -> " + syncable);
            }
            if (aInfo.syncable != syncable) {
                aInfo.syncable = syncable;
                writeAccountInfoLocked();
            } else if (Log.isLoggable("SyncManager", 2)) {
                Slog.d("SyncManager", "setIsSyncable: already set to " + syncable + ", doing nothing");
            }
        }
    }

    public Pair<Long, Long> getBackoff(EndPoint info) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getBackoff");
            if (authority != null) {
                Pair<Long, Long> create = Pair.create(Long.valueOf(authority.backoffTime), Long.valueOf(authority.backoffDelay));
                return create;
            }
            return null;
        }
    }

    public void setBackoff(EndPoint info, long nextSyncTime, long nextDelay) {
        boolean changed;
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "setBackoff: " + info + " -> nextSyncTime " + nextSyncTime + ", nextDelay " + nextDelay);
        }
        synchronized (this.mAuthorities) {
            if (info.account == null || info.provider == null) {
                changed = setBackoffLocked(info.account, info.userId, info.provider, nextSyncTime, nextDelay);
            } else {
                AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(info, -1, true);
                if (authorityInfo.backoffTime == nextSyncTime && authorityInfo.backoffDelay == nextDelay) {
                    changed = false;
                } else {
                    authorityInfo.backoffTime = nextSyncTime;
                    authorityInfo.backoffDelay = nextDelay;
                    changed = true;
                }
            }
        }
        if (changed) {
            reportChange(1);
        }
    }

    private boolean setBackoffLocked(Account account, int userId, String providerName, long nextSyncTime, long nextDelay) {
        boolean changed = false;
        for (AccountInfo accountInfo : this.mAccounts.values()) {
            if (account == null || (account.equals(accountInfo.accountAndUser.account) ^ 1) == 0 || userId == accountInfo.accountAndUser.userId) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if ((providerName == null || (providerName.equals(authorityInfo.target.provider) ^ 1) == 0) && !(authorityInfo.backoffTime == nextSyncTime && authorityInfo.backoffDelay == nextDelay)) {
                        authorityInfo.backoffTime = nextSyncTime;
                        authorityInfo.backoffDelay = nextDelay;
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    public void clearAllBackoffsLocked() {
        boolean changed = false;
        synchronized (this.mAuthorities) {
            for (AccountInfo accountInfo : this.mAccounts.values()) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (authorityInfo.backoffTime != -1 || authorityInfo.backoffDelay != -1) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Slog.v("SyncManager", "clearAllBackoffsLocked: authority:" + authorityInfo.target + " account:" + accountInfo.accountAndUser.account.name + " user:" + accountInfo.accountAndUser.userId + " backoffTime was: " + authorityInfo.backoffTime + " backoffDelay was: " + authorityInfo.backoffDelay);
                        }
                        authorityInfo.backoffTime = -1;
                        authorityInfo.backoffDelay = -1;
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            reportChange(1);
        }
    }

    public long getDelayUntilTime(EndPoint info) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getDelayUntil");
            if (authority == null) {
                return 0;
            }
            long j = authority.delayUntil;
            return j;
        }
    }

    public void setDelayUntilTime(EndPoint info, long delayUntil) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "setDelayUntil: " + info + " -> delayUntil " + delayUntil);
        }
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority.delayUntil == delayUntil) {
                return;
            }
            authority.delayUntil = delayUntil;
            reportChange(1);
        }
    }

    boolean restoreAllPeriodicSyncs() {
        if (mPeriodicSyncAddedListener == null) {
            return false;
        }
        synchronized (this.mAuthorities) {
            for (int i = 0; i < this.mAuthorities.size(); i++) {
                AuthorityInfo authority = (AuthorityInfo) this.mAuthorities.valueAt(i);
                for (PeriodicSync periodicSync : authority.periodicSyncs) {
                    mPeriodicSyncAddedListener.onPeriodicSyncAdded(authority.target, periodicSync.extras, periodicSync.period, periodicSync.flexTime);
                }
                authority.periodicSyncs.clear();
            }
            writeAccountInfoLocked();
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setMasterSyncAutomatically(boolean flag, int userId) {
        synchronized (this.mAuthorities) {
            Boolean auto = (Boolean) this.mMasterSyncAutomatically.get(userId);
            if (auto == null || !auto.equals(Boolean.valueOf(flag))) {
                this.mMasterSyncAutomatically.put(userId, Boolean.valueOf(flag));
                writeAccountInfoLocked();
            }
        }
    }

    public boolean getMasterSyncAutomatically(int userId) {
        boolean booleanValue;
        synchronized (this.mAuthorities) {
            Boolean auto = (Boolean) this.mMasterSyncAutomatically.get(userId);
            booleanValue = auto == null ? this.mDefaultMasterSyncAutomatically : auto.booleanValue();
        }
        return booleanValue;
    }

    public int getAuthorityCount() {
        int size;
        synchronized (this.mAuthorities) {
            size = this.mAuthorities.size();
        }
        return size;
    }

    public AuthorityInfo getAuthority(int authorityId) {
        AuthorityInfo authorityInfo;
        synchronized (this.mAuthorities) {
            authorityInfo = (AuthorityInfo) this.mAuthorities.get(authorityId);
        }
        return authorityInfo;
    }

    public boolean isSyncActive(EndPoint info) {
        synchronized (this.mAuthorities) {
            for (SyncInfo syncInfo : getCurrentSyncs(info.userId)) {
                AuthorityInfo ainfo = getAuthority(syncInfo.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void markPending(EndPoint info, boolean pendingValue) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority == null) {
                return;
            }
            getOrCreateSyncStatusLocked(authority.ident).pending = pendingValue;
            reportChange(2);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void doDatabaseCleanup(Account[] accounts, int userId) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Updating for new accounts...");
            }
            SparseArray<AuthorityInfo> removing = new SparseArray();
            Iterator<AccountInfo> accIt = this.mAccounts.values().iterator();
            while (accIt.hasNext()) {
                AccountInfo acc = (AccountInfo) accIt.next();
                if (!ArrayUtils.contains(accounts, acc.accountAndUser.account) && acc.accountAndUser.userId == userId) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Slog.v("SyncManager", "Account removed: " + acc.accountAndUser);
                    }
                    for (AuthorityInfo auth : acc.authorities.values()) {
                        removing.put(auth.ident, auth);
                    }
                    accIt.remove();
                }
            }
            int i = removing.size();
        }
    }

    public SyncInfo addActiveSync(ActiveSyncContext activeSyncContext) {
        SyncInfo syncInfo;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "setActiveSync: account= auth=" + activeSyncContext.mSyncOperation.target + " src=" + activeSyncContext.mSyncOperation.syncSource + " extras=" + activeSyncContext.mSyncOperation.extras);
            }
            AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(activeSyncContext.mSyncOperation.target, -1, true);
            syncInfo = new SyncInfo(authorityInfo.ident, authorityInfo.target.account, authorityInfo.target.provider, activeSyncContext.mStartTime);
            getCurrentSyncs(authorityInfo.target.userId).add(syncInfo);
        }
        reportActiveChange();
        return syncInfo;
    }

    public void removeActiveSync(SyncInfo syncInfo, int userId) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "removeActiveSync: account=" + syncInfo.account + " user=" + userId + " auth=" + syncInfo.authority);
            }
            getCurrentSyncs(userId).remove(syncInfo);
        }
        reportActiveChange();
    }

    public void reportActiveChange() {
        reportChange(4);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long insertStartSyncEvent(SyncOperation op, long now) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "insertStartSyncEvent: " + op);
            }
            AuthorityInfo authority = getAuthorityLocked(op.target, "insertStartSyncEvent");
            if (authority == null) {
                return -1;
            }
            SyncHistoryItem item = new SyncHistoryItem();
            item.initialization = op.isInitialization();
            item.authorityId = authority.ident;
            int i = this.mNextHistoryId;
            this.mNextHistoryId = i + 1;
            item.historyId = i;
            if (this.mNextHistoryId < 0) {
                this.mNextHistoryId = 0;
            }
            item.eventTime = now;
            item.source = op.syncSource;
            item.reason = op.reason;
            item.extras = op.extras;
            item.event = 0;
            this.mSyncHistory.add(0, item);
            while (this.mSyncHistory.size() > 100) {
                this.mSyncHistory.remove(this.mSyncHistory.size() - 1);
            }
            long id = (long) item.historyId;
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "returning historyId " + id);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void stopSyncEvent(long historyId, long elapsedTime, String resultMessage, long downstreamActivity, long upstreamActivity) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "stopSyncEvent: historyId=" + historyId);
            }
            SyncHistoryItem item = null;
            int i = this.mSyncHistory.size();
            while (i > 0) {
                i--;
                item = (SyncHistoryItem) this.mSyncHistory.get(i);
                if (((long) item.historyId) == historyId) {
                    break;
                }
                item = null;
            }
            if (item == null) {
                Slog.w("SyncManager", "stopSyncEvent: no history for id " + historyId);
                return;
            }
            item.elapsedTime = elapsedTime;
            item.event = 1;
            item.mesg = resultMessage;
            item.downstreamActivity = downstreamActivity;
            item.upstreamActivity = upstreamActivity;
            SyncStatusInfo status = getOrCreateSyncStatusLocked(item.authorityId);
            status.numSyncs++;
            status.totalElapsedTime += elapsedTime;
            switch (item.source) {
                case 0:
                    status.numSourceServer++;
                    break;
                case 1:
                    status.numSourceLocal++;
                    break;
                case 2:
                    status.numSourcePoll++;
                    break;
                case 3:
                    status.numSourceUser++;
                    break;
                case 4:
                    status.numSourcePeriodic++;
                    break;
            }
            boolean writeStatisticsNow = false;
            int day = getCurrentDayLocked();
            if (this.mDayStats[0] == null) {
                this.mDayStats[0] = new DayStats(day);
            } else if (day != this.mDayStats[0].day) {
                System.arraycopy(this.mDayStats, 0, this.mDayStats, 1, this.mDayStats.length - 1);
                this.mDayStats[0] = new DayStats(day);
                writeStatisticsNow = true;
            } else {
                DayStats dayStats = this.mDayStats[0];
            }
            DayStats ds = this.mDayStats[0];
            long lastSyncTime = item.eventTime + elapsedTime;
            boolean writeStatusNow = false;
            if (MESG_SUCCESS.equals(resultMessage)) {
                if (status.lastSuccessTime == 0 || status.lastFailureTime != 0) {
                    writeStatusNow = true;
                }
                status.lastSuccessTime = lastSyncTime;
                status.lastSuccessSource = item.source;
                status.lastFailureTime = 0;
                status.lastFailureSource = -1;
                status.lastFailureMesg = null;
                status.initialFailureTime = 0;
                ds.successCount++;
                ds.successTime += elapsedTime;
            } else if (!MESG_CANCELED.equals(resultMessage)) {
                if (status.lastFailureTime == 0) {
                    writeStatusNow = true;
                }
                status.lastFailureTime = lastSyncTime;
                status.lastFailureSource = item.source;
                status.lastFailureMesg = resultMessage;
                if (status.initialFailureTime == 0) {
                    status.initialFailureTime = lastSyncTime;
                }
                ds.failureCount++;
                ds.failureTime += elapsedTime;
            }
            StringBuilder event = new StringBuilder();
            event.append("").append(resultMessage).append(" Source=").append(SOURCES[item.source]).append(" Elapsed=");
            SyncManager.formatDurationHMS(event, elapsedTime);
            event.append(" Reason=");
            event.append(SyncOperation.reasonToString(null, item.reason));
            event.append(" Extras=");
            SyncOperation.extrasToStringBuilder(item.extras, event);
            status.addEvent(event.toString());
            if (writeStatusNow) {
                writeStatusLocked();
            } else if (!hasMessages(1)) {
                sendMessageDelayed(obtainMessage(1), 600000);
            }
            if (writeStatisticsNow) {
                writeStatisticsLocked();
            } else if (!hasMessages(2)) {
                sendMessageDelayed(obtainMessage(2), 1800000);
            }
        }
    }

    private List<SyncInfo> getCurrentSyncs(int userId) {
        List<SyncInfo> currentSyncsLocked;
        synchronized (this.mAuthorities) {
            currentSyncsLocked = getCurrentSyncsLocked(userId);
        }
        return currentSyncsLocked;
    }

    public List<SyncInfo> getCurrentSyncsCopy(int userId, boolean canAccessAccounts) {
        List<SyncInfo> syncsCopy;
        synchronized (this.mAuthorities) {
            List<SyncInfo> syncs = getCurrentSyncsLocked(userId);
            syncsCopy = new ArrayList();
            for (SyncInfo sync : syncs) {
                SyncInfo copy;
                if (canAccessAccounts) {
                    copy = new SyncInfo(sync);
                } else {
                    copy = SyncInfo.createAccountRedacted(sync.authorityId, sync.authority, sync.startTime);
                }
                syncsCopy.add(copy);
            }
        }
        return syncsCopy;
    }

    private List<SyncInfo> getCurrentSyncsLocked(int userId) {
        ArrayList<SyncInfo> syncs = (ArrayList) this.mCurrentSyncs.get(userId);
        if (syncs != null) {
            return syncs;
        }
        syncs = new ArrayList();
        this.mCurrentSyncs.put(userId, syncs);
        return syncs;
    }

    public Pair<AuthorityInfo, SyncStatusInfo> getCopyOfAuthorityWithSyncStatus(EndPoint info) {
        Pair<AuthorityInfo, SyncStatusInfo> createCopyPairOfAuthorityWithSyncStatusLocked;
        synchronized (this.mAuthorities) {
            createCopyPairOfAuthorityWithSyncStatusLocked = createCopyPairOfAuthorityWithSyncStatusLocked(getOrCreateAuthorityLocked(info, -1, true));
        }
        return createCopyPairOfAuthorityWithSyncStatusLocked;
    }

    public SyncStatusInfo getStatusByAuthority(EndPoint info) {
        if (info.account == null || info.provider == null) {
            return null;
        }
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            int i = 0;
            while (i < N) {
                SyncStatusInfo cur = (SyncStatusInfo) this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = (AuthorityInfo) this.mAuthorities.get(cur.authorityId);
                if (ainfo == null || !ainfo.target.matchesSpec(info)) {
                    i++;
                } else {
                    return cur;
                }
            }
            return null;
        }
    }

    public boolean isSyncPending(EndPoint info) {
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo cur = (SyncStatusInfo) this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = (AuthorityInfo) this.mAuthorities.get(cur.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info) && cur.pending) {
                    return true;
                }
            }
            return false;
        }
    }

    public ArrayList<SyncHistoryItem> getSyncHistory() {
        ArrayList<SyncHistoryItem> items;
        synchronized (this.mAuthorities) {
            int N = this.mSyncHistory.size();
            items = new ArrayList(N);
            for (int i = 0; i < N; i++) {
                items.add((SyncHistoryItem) this.mSyncHistory.get(i));
            }
        }
        return items;
    }

    public DayStats[] getDayStatistics() {
        DayStats[] ds;
        synchronized (this.mAuthorities) {
            ds = new DayStats[this.mDayStats.length];
            System.arraycopy(this.mDayStats, 0, ds, 0, ds.length);
        }
        return ds;
    }

    private Pair<AuthorityInfo, SyncStatusInfo> createCopyPairOfAuthorityWithSyncStatusLocked(AuthorityInfo authorityInfo) {
        return Pair.create(new AuthorityInfo(authorityInfo), new SyncStatusInfo(getOrCreateSyncStatusLocked(authorityInfo.ident)));
    }

    private int getCurrentDayLocked() {
        this.mCal.setTimeInMillis(System.currentTimeMillis());
        int dayOfYear = this.mCal.get(6);
        if (this.mYear != this.mCal.get(1)) {
            this.mYear = this.mCal.get(1);
            this.mCal.clear();
            this.mCal.set(1, this.mYear);
            this.mYearInDays = (int) (this.mCal.getTimeInMillis() / 86400000);
        }
        return this.mYearInDays + dayOfYear;
    }

    private AuthorityInfo getAuthorityLocked(EndPoint info, String tag) {
        AccountAndUser au = new AccountAndUser(info.account, info.userId);
        AccountInfo accountInfo = (AccountInfo) this.mAccounts.get(au);
        if (accountInfo == null) {
            if (tag != null && Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", tag + ": unknown account " + au);
            }
            return null;
        }
        AuthorityInfo authority = (AuthorityInfo) accountInfo.authorities.get(info.provider);
        if (authority != null) {
            return authority;
        }
        if (tag != null && Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", tag + ": unknown provider " + info.provider);
        }
        return null;
    }

    private AuthorityInfo getOrCreateAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        AccountAndUser au = new AccountAndUser(info.account, info.userId);
        AccountInfo account = (AccountInfo) this.mAccounts.get(au);
        if (account == null) {
            account = new AccountInfo(au);
            this.mAccounts.put(au, account);
        }
        AuthorityInfo authority = (AuthorityInfo) account.authorities.get(info.provider);
        if (authority != null) {
            return authority;
        }
        authority = createAuthorityLocked(info, ident, doWrite);
        account.authorities.put(info.provider, authority);
        return authority;
    }

    private AuthorityInfo createAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        if (ident < 0) {
            ident = this.mNextAuthorityId;
            this.mNextAuthorityId++;
            doWrite = true;
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "created a new AuthorityInfo for " + info);
        }
        AuthorityInfo authority = new AuthorityInfo(info, ident);
        this.mAuthorities.put(ident, authority);
        if (doWrite) {
            writeAccountInfoLocked();
        }
        return authority;
    }

    public void removeAuthority(EndPoint info) {
        synchronized (this.mAuthorities) {
            removeAuthorityLocked(info.account, info.userId, info.provider, true);
        }
    }

    private void removeAuthorityLocked(Account account, int userId, String authorityName, boolean doWrite) {
        AccountInfo accountInfo = (AccountInfo) this.mAccounts.get(new AccountAndUser(account, userId));
        if (accountInfo != null) {
            AuthorityInfo authorityInfo = (AuthorityInfo) accountInfo.authorities.remove(authorityName);
            if (authorityInfo != null) {
                if (this.mAuthorityRemovedListener != null) {
                    this.mAuthorityRemovedListener.onAuthorityRemoved(authorityInfo.target);
                }
                this.mAuthorities.remove(authorityInfo.ident);
                if (doWrite) {
                    writeAccountInfoLocked();
                }
            }
        }
    }

    private SyncStatusInfo getOrCreateSyncStatusLocked(int authorityId) {
        SyncStatusInfo status = (SyncStatusInfo) this.mSyncStatus.get(authorityId);
        if (status != null) {
            return status;
        }
        status = new SyncStatusInfo(authorityId);
        this.mSyncStatus.put(authorityId, status);
        return status;
    }

    public void writeAllState() {
        synchronized (this.mAuthorities) {
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    public boolean shouldGrantSyncAdaptersAccountAccess() {
        return this.mGrantSyncAdaptersAccountAccess;
    }

    public void clearAndReadState() {
        synchronized (this.mAuthorities) {
            this.mAuthorities.clear();
            this.mAccounts.clear();
            this.mServices.clear();
            this.mSyncStatus.clear();
            this.mSyncHistory.clear();
            readAccountInfoLocked();
            readStatusLocked();
            readStatisticsLocked();
            readAndDeleteLegacyAccountInfoLocked();
            writeAccountInfoLocked();
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    private void readAccountInfoLocked() {
        int highestAuthorityId = -1;
        FileInputStream fis = null;
        try {
            fis = this.mAccountInfoFile.openRead();
            if (Log.isLoggable(TAG_FILE, 2)) {
                Slog.v(TAG_FILE, "Reading " + this.mAccountInfoFile.getBaseFile());
            }
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            int eventType = parser.getEventType();
            while (eventType != 2 && eventType != 1) {
                eventType = parser.next();
            }
            if (eventType == 1) {
                Slog.i("SyncManager", "No initial accounts");
                this.mNextAuthorityId = Math.max(0, this.mNextAuthorityId);
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
                return;
            }
            if ("accounts".equals(parser.getName())) {
                int version;
                String listen = parser.getAttributeValue(null, XML_ATTR_LISTEN_FOR_TICKLES);
                String versionString = parser.getAttributeValue(null, "version");
                if (versionString == null) {
                    version = 0;
                } else {
                    try {
                        version = Integer.parseInt(versionString);
                    } catch (NumberFormatException e2) {
                        version = 0;
                    }
                }
                if (version < 3) {
                    this.mGrantSyncAdaptersAccountAccess = true;
                }
                String nextIdString = parser.getAttributeValue(null, XML_ATTR_NEXT_AUTHORITY_ID);
                try {
                    this.mNextAuthorityId = Math.max(this.mNextAuthorityId, nextIdString == null ? 0 : Integer.parseInt(nextIdString));
                } catch (NumberFormatException e3) {
                }
                String offsetString = parser.getAttributeValue(null, XML_ATTR_SYNC_RANDOM_OFFSET);
                try {
                    this.mSyncRandomOffset = offsetString == null ? 0 : Integer.parseInt(offsetString);
                } catch (NumberFormatException e4) {
                    this.mSyncRandomOffset = 0;
                }
                if (this.mSyncRandomOffset == 0) {
                    this.mSyncRandomOffset = new Random(System.currentTimeMillis()).nextInt(86400);
                }
                this.mMasterSyncAutomatically.put(0, Boolean.valueOf(listen != null ? Boolean.parseBoolean(listen) : true));
                eventType = parser.next();
                AuthorityInfo authority = null;
                PeriodicSync periodicSync = null;
                AccountAuthorityValidator accountAuthorityValidator = new AccountAuthorityValidator(this.mContext);
                do {
                    if (eventType == 2) {
                        String tagName = parser.getName();
                        if (parser.getDepth() == 2) {
                            if ("authority".equals(tagName)) {
                                authority = parseAuthority(parser, version, accountAuthorityValidator);
                                periodicSync = null;
                                if (authority == null) {
                                    EventLog.writeEvent(1397638484, new Object[]{"26513719", Integer.valueOf(-1), "Malformed authority"});
                                } else if (authority.ident > highestAuthorityId) {
                                    highestAuthorityId = authority.ident;
                                }
                            } else if (XML_TAG_LISTEN_FOR_TICKLES.equals(tagName)) {
                                parseListenForTickles(parser);
                            }
                        } else if (parser.getDepth() == 3) {
                            if ("periodicSync".equals(tagName) && authority != null) {
                                periodicSync = parsePeriodicSync(parser, authority);
                            }
                        } else if (parser.getDepth() == 4 && periodicSync != null && "extra".equals(tagName)) {
                            parseExtra(parser, periodicSync.extras);
                        }
                    }
                    eventType = parser.next();
                } while (eventType != 1);
            }
            this.mNextAuthorityId = Math.max(highestAuthorityId + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e5) {
                }
            }
            maybeMigrateSettingsForRenamedAuthorities();
        } catch (XmlPullParserException e6) {
            Slog.w("SyncManager", "Error reading accounts", e6);
            this.mNextAuthorityId = Math.max(highestAuthorityId + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e7) {
                }
            }
        } catch (IOException e8) {
            if (fis == null) {
                Slog.i("SyncManager", "No initial accounts");
            } else {
                Slog.w("SyncManager", "Error reading accounts", e8);
            }
            this.mNextAuthorityId = Math.max(highestAuthorityId + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e9) {
                }
            }
        } catch (Throwable th) {
            this.mNextAuthorityId = Math.max(highestAuthorityId + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e10) {
                }
            }
        }
    }

    private void maybeDeleteLegacyPendingInfoLocked(File syncDir) {
        File file = new File(syncDir, "pending.bin");
        if (file.exists()) {
            file.delete();
        }
    }

    private boolean maybeMigrateSettingsForRenamedAuthorities() {
        boolean writeNeeded = false;
        ArrayList<AuthorityInfo> authoritiesToRemove = new ArrayList();
        int N = this.mAuthorities.size();
        for (int i = 0; i < N; i++) {
            AuthorityInfo authority = (AuthorityInfo) this.mAuthorities.valueAt(i);
            String newAuthorityName = (String) sAuthorityRenames.get(authority.target.provider);
            if (newAuthorityName != null) {
                authoritiesToRemove.add(authority);
                if (authority.enabled) {
                    EndPoint newInfo = new EndPoint(authority.target.account, newAuthorityName, authority.target.userId);
                    if (getAuthorityLocked(newInfo, "cleanup") == null) {
                        getOrCreateAuthorityLocked(newInfo, -1, false).enabled = true;
                        writeNeeded = true;
                    }
                }
            }
        }
        for (AuthorityInfo authorityInfo : authoritiesToRemove) {
            removeAuthorityLocked(authorityInfo.target.account, authorityInfo.target.userId, authorityInfo.target.provider, false);
            writeNeeded = true;
        }
        return writeNeeded;
    }

    private void parseListenForTickles(XmlPullParser parser) {
        int userId = 0;
        try {
            userId = Integer.parseInt(parser.getAttributeValue(null, XML_ATTR_USER));
        } catch (NumberFormatException e) {
            Slog.e("SyncManager", "error parsing the user for listen-for-tickles", e);
        } catch (NullPointerException e2) {
            Slog.e("SyncManager", "the user in listen-for-tickles is null", e2);
        }
        String enabled = parser.getAttributeValue(null, XML_ATTR_ENABLED);
        this.mMasterSyncAutomatically.put(userId, Boolean.valueOf(enabled != null ? Boolean.parseBoolean(enabled) : true));
    }

    private AuthorityInfo parseAuthority(XmlPullParser parser, int version, AccountAuthorityValidator validator) {
        AuthorityInfo authority = null;
        int id = -1;
        try {
            id = Integer.parseInt(parser.getAttributeValue(null, DecryptTool.UNLOCK_TYPE_ID));
        } catch (NumberFormatException e) {
            Slog.e("SyncManager", "error parsing the id of the authority", e);
        } catch (NullPointerException e2) {
            Slog.e("SyncManager", "the id of the authority is null", e2);
        }
        if (id >= 0) {
            String authorityName = parser.getAttributeValue(null, "authority");
            String enabled = parser.getAttributeValue(null, XML_ATTR_ENABLED);
            String syncable = parser.getAttributeValue(null, "syncable");
            String accountName = parser.getAttributeValue(null, "account");
            String accountType = parser.getAttributeValue(null, SoundModelContract.KEY_TYPE);
            String user = parser.getAttributeValue(null, XML_ATTR_USER);
            String packageName = parser.getAttributeValue(null, "package");
            String className = parser.getAttributeValue(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS);
            int userId = user == null ? 0 : Integer.parseInt(user);
            if (accountType == null && packageName == null) {
                accountType = "com.google";
                syncable = String.valueOf(-1);
            }
            authority = (AuthorityInfo) this.mAuthorities.get(id);
            if (Log.isLoggable(TAG_FILE, 2)) {
                Slog.v(TAG_FILE, "Adding authority: account=" + accountName + " accountType=" + accountType + " auth=" + authorityName + " package=" + packageName + " class=" + className + " user=" + userId + " enabled=" + enabled + " syncable=" + syncable);
            }
            if (authority == null) {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Slog.v(TAG_FILE, "Creating authority entry");
                }
                if (!(accountName == null || authorityName == null)) {
                    EndPoint info = new EndPoint(new Account(accountName, accountType), authorityName, userId);
                    if (validator.isAccountValid(info.account, userId) && validator.isAuthorityValid(authorityName, userId)) {
                        authority = getOrCreateAuthorityLocked(info, id, false);
                        if (version > 0) {
                            authority.periodicSyncs.clear();
                        }
                    } else {
                        EventLog.writeEvent(1397638484, new Object[]{"35028827", Integer.valueOf(-1), "account:" + info.account + " provider:" + authorityName + " user:" + userId});
                    }
                }
            }
            if (authority != null) {
                authority.enabled = enabled != null ? Boolean.parseBoolean(enabled) : true;
                try {
                    authority.syncable = syncable == null ? -1 : Integer.parseInt(syncable);
                } catch (NumberFormatException e3) {
                    if (Shell.NIGHT_MODE_STR_UNKNOWN.equals(syncable)) {
                        authority.syncable = -1;
                    } else {
                        authority.syncable = Boolean.parseBoolean(syncable) ? 1 : 0;
                    }
                }
            } else {
                Slog.w("SyncManager", "Failure adding authority: account=" + accountName + " auth=" + authorityName + " enabled=" + enabled + " syncable=" + syncable);
            }
        }
        return authority;
    }

    private PeriodicSync parsePeriodicSync(XmlPullParser parser, AuthorityInfo authorityInfo) {
        long flextime;
        Bundle extras = new Bundle();
        String periodValue = parser.getAttributeValue(null, "period");
        String flexValue = parser.getAttributeValue(null, "flex");
        try {
            long period = Long.parseLong(periodValue);
            try {
                flextime = Long.parseLong(flexValue);
            } catch (NumberFormatException e) {
                flextime = calculateDefaultFlexTime(period);
                Slog.e("SyncManager", "Error formatting value parsed for periodic sync flex: " + flexValue + ", using default: " + flextime);
            } catch (NullPointerException e2) {
                flextime = calculateDefaultFlexTime(period);
                Slog.d("SyncManager", "No flex time specified for this sync, using a default. period: " + period + " flex: " + flextime);
            }
            PeriodicSync periodicSync = new PeriodicSync(authorityInfo.target.account, authorityInfo.target.provider, extras, period, flextime);
            authorityInfo.periodicSyncs.add(periodicSync);
            return periodicSync;
        } catch (NumberFormatException e3) {
            Slog.e("SyncManager", "error parsing the period of a periodic sync", e3);
            return null;
        } catch (NullPointerException e4) {
            Slog.e("SyncManager", "the period of a periodic sync is null", e4);
            return null;
        }
    }

    private void parseExtra(XmlPullParser parser, Bundle extras) {
        String name = parser.getAttributeValue(null, "name");
        String type = parser.getAttributeValue(null, SoundModelContract.KEY_TYPE);
        String value1 = parser.getAttributeValue(null, "value1");
        String value2 = parser.getAttributeValue(null, "value2");
        try {
            if ("long".equals(type)) {
                extras.putLong(name, Long.parseLong(value1));
            } else if ("integer".equals(type)) {
                extras.putInt(name, Integer.parseInt(value1));
            } else if ("double".equals(type)) {
                extras.putDouble(name, Double.parseDouble(value1));
            } else if ("float".equals(type)) {
                extras.putFloat(name, Float.parseFloat(value1));
            } else if ("boolean".equals(type)) {
                extras.putBoolean(name, Boolean.parseBoolean(value1));
            } else if ("string".equals(type)) {
                extras.putString(name, value1);
            } else if ("account".equals(type)) {
                extras.putParcelable(name, new Account(value1, value2));
            }
        } catch (NumberFormatException e) {
            Slog.e("SyncManager", "error parsing bundle value", e);
        } catch (NullPointerException e2) {
            Slog.e("SyncManager", "error parsing bundle value", e2);
        }
    }

    private void writeAccountInfoLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + this.mAccountInfoFile.getBaseFile());
        }
        try {
            FileOutputStream fos = this.mAccountInfoFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag(null, "accounts");
            out.attribute(null, "version", Integer.toString(3));
            out.attribute(null, XML_ATTR_NEXT_AUTHORITY_ID, Integer.toString(this.mNextAuthorityId));
            out.attribute(null, XML_ATTR_SYNC_RANDOM_OFFSET, Integer.toString(this.mSyncRandomOffset));
            int M = this.mMasterSyncAutomatically.size();
            for (int m = 0; m < M; m++) {
                int userId = this.mMasterSyncAutomatically.keyAt(m);
                Boolean listen = (Boolean) this.mMasterSyncAutomatically.valueAt(m);
                out.startTag(null, XML_TAG_LISTEN_FOR_TICKLES);
                out.attribute(null, XML_ATTR_USER, Integer.toString(userId));
                out.attribute(null, XML_ATTR_ENABLED, Boolean.toString(listen.booleanValue()));
                out.endTag(null, XML_TAG_LISTEN_FOR_TICKLES);
            }
            int N = this.mAuthorities.size();
            for (int i = 0; i < N; i++) {
                AuthorityInfo authority = (AuthorityInfo) this.mAuthorities.valueAt(i);
                EndPoint info = authority.target;
                out.startTag(null, "authority");
                out.attribute(null, DecryptTool.UNLOCK_TYPE_ID, Integer.toString(authority.ident));
                out.attribute(null, XML_ATTR_USER, Integer.toString(info.userId));
                out.attribute(null, XML_ATTR_ENABLED, Boolean.toString(authority.enabled));
                out.attribute(null, "account", info.account.name);
                out.attribute(null, SoundModelContract.KEY_TYPE, info.account.type);
                out.attribute(null, "authority", info.provider);
                out.attribute(null, "syncable", Integer.toString(authority.syncable));
                out.endTag(null, "authority");
            }
            out.endTag(null, "accounts");
            out.endDocument();
            this.mAccountInfoFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w("SyncManager", "Error writing accounts", e1);
            if (null != null) {
                this.mAccountInfoFile.failWrite(null);
            }
        }
    }

    static int getIntColumn(Cursor c, String name) {
        return c.getInt(c.getColumnIndex(name));
    }

    static long getLongColumn(Cursor c, String name) {
        return c.getLong(c.getColumnIndex(name));
    }

    private void readAndDeleteLegacyAccountInfoLocked() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r24_5 'st' android.content.SyncStatusInfo) in PHI: PHI: (r24_6 'st' android.content.SyncStatusInfo) = (r24_4 'st' android.content.SyncStatusInfo), (r24_5 'st' android.content.SyncStatusInfo) binds: {(r24_4 'st' android.content.SyncStatusInfo)=B:31:0x017e, (r24_5 'st' android.content.SyncStatusInfo)=B:32:0x0180}
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
        r26 = this;
        r0 = r26;
        r4 = r0.mContext;
        r5 = "syncmanager.db";
        r16 = r4.getDatabasePath(r5);
        r4 = r16.exists();
        if (r4 != 0) goto L_0x0012;
    L_0x0011:
        return;
    L_0x0012:
        r22 = r16.getPath();
        r3 = 0;
        r4 = 0;
        r5 = 1;
        r0 = r22;	 Catch:{ SQLiteException -> 0x0226 }
        r3 = android.database.sqlite.SQLiteDatabase.openDatabase(r0, r4, r5);	 Catch:{ SQLiteException -> 0x0226 }
    L_0x001f:
        if (r3 == 0) goto L_0x02e0;
    L_0x0021:
        r4 = r3.getVersion();
        r5 = 11;
        if (r4 < r5) goto L_0x0229;
    L_0x0029:
        r18 = 1;
    L_0x002b:
        r4 = "SyncManagerFile";
        r5 = 2;
        r4 = android.util.Log.isLoggable(r4, r5);
        if (r4 == 0) goto L_0x003e;
    L_0x0035:
        r4 = "SyncManagerFile";
        r5 = "Reading legacy sync accounts db";
        android.util.Slog.v(r4, r5);
    L_0x003e:
        r2 = new android.database.sqlite.SQLiteQueryBuilder;
        r2.<init>();
        r4 = "stats, status";
        r2.setTables(r4);
        r20 = new java.util.HashMap;
        r20.<init>();
        r4 = "_id";
        r5 = "status._id as _id";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "account";
        r5 = "stats.account as account";
        r0 = r20;
        r0.put(r4, r5);
        if (r18 == 0) goto L_0x0071;
    L_0x0066:
        r4 = "account_type";
        r5 = "stats.account_type as account_type";
        r0 = r20;
        r0.put(r4, r5);
    L_0x0071:
        r4 = "authority";
        r5 = "stats.authority as authority";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "totalElapsedTime";
        r5 = "totalElapsedTime";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "numSyncs";
        r5 = "numSyncs";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "numSourceLocal";
        r5 = "numSourceLocal";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "numSourcePoll";
        r5 = "numSourcePoll";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "numSourceServer";
        r5 = "numSourceServer";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "numSourceUser";
        r5 = "numSourceUser";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "lastSuccessSource";
        r5 = "lastSuccessSource";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "lastSuccessTime";
        r5 = "lastSuccessTime";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "lastFailureSource";
        r5 = "lastFailureSource";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "lastFailureTime";
        r5 = "lastFailureTime";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "lastFailureMesg";
        r5 = "lastFailureMesg";
        r0 = r20;
        r0.put(r4, r5);
        r4 = "pending";
        r5 = "pending";
        r0 = r20;
        r0.put(r4, r5);
        r0 = r20;
        r2.setProjectionMap(r0);
        r4 = "stats._id = status.stats_id";
        r2.appendWhere(r4);
        r4 = 0;
        r5 = 0;
        r6 = 0;
        r7 = 0;
        r8 = 0;
        r9 = 0;
        r14 = r2.query(r3, r4, r5, r6, r7, r8, r9);
    L_0x0115:
        r4 = r14.moveToNext();
        if (r4 == 0) goto L_0x0232;
    L_0x011b:
        r4 = "account";
        r4 = r14.getColumnIndex(r4);
        r10 = r14.getString(r4);
        if (r18 == 0) goto L_0x022d;
    L_0x0128:
        r4 = "account_type";
        r4 = r14.getColumnIndex(r4);
        r11 = r14.getString(r4);
    L_0x0133:
        if (r11 != 0) goto L_0x0138;
    L_0x0135:
        r11 = "com.google";
    L_0x0138:
        r4 = "authority";
        r4 = r14.getColumnIndex(r4);
        r13 = r14.getString(r4);
        r4 = new com.android.server.content.SyncStorageEngine$EndPoint;
        r5 = new android.accounts.Account;
        r5.<init>(r10, r11);
        r6 = 0;
        r4.<init>(r5, r13, r6);
        r5 = -1;
        r6 = 0;
        r0 = r26;
        r12 = r0.getOrCreateAuthorityLocked(r4, r5, r6);
        if (r12 == 0) goto L_0x0115;
    L_0x0158:
        r0 = r26;
        r4 = r0.mSyncStatus;
        r19 = r4.size();
        r17 = 0;
        r24 = 0;
    L_0x0164:
        if (r19 <= 0) goto L_0x017e;
    L_0x0166:
        r19 = r19 + -1;
        r0 = r26;
        r4 = r0.mSyncStatus;
        r0 = r19;
        r24 = r4.valueAt(r0);
        r24 = (android.content.SyncStatusInfo) r24;
        r0 = r24;
        r4 = r0.authorityId;
        r5 = r12.ident;
        if (r4 != r5) goto L_0x0164;
    L_0x017c:
        r17 = 1;
    L_0x017e:
        if (r17 != 0) goto L_0x0194;
    L_0x0180:
        r24 = new android.content.SyncStatusInfo;
        r4 = r12.ident;
        r0 = r24;
        r0.<init>(r4);
        r0 = r26;
        r4 = r0.mSyncStatus;
        r5 = r12.ident;
        r0 = r24;
        r4.put(r5, r0);
    L_0x0194:
        r4 = "totalElapsedTime";
        r4 = getLongColumn(r14, r4);
        r0 = r24;
        r0.totalElapsedTime = r4;
        r4 = "numSyncs";
        r4 = getIntColumn(r14, r4);
        r0 = r24;
        r0.numSyncs = r4;
        r4 = "numSourceLocal";
        r4 = getIntColumn(r14, r4);
        r0 = r24;
        r0.numSourceLocal = r4;
        r4 = "numSourcePoll";
        r4 = getIntColumn(r14, r4);
        r0 = r24;
        r0.numSourcePoll = r4;
        r4 = "numSourceServer";
        r4 = getIntColumn(r14, r4);
        r0 = r24;
        r0.numSourceServer = r4;
        r4 = "numSourceUser";
        r4 = getIntColumn(r14, r4);
        r0 = r24;
        r0.numSourceUser = r4;
        r4 = 0;
        r0 = r24;
        r0.numSourcePeriodic = r4;
        r4 = "lastSuccessSource";
        r4 = getIntColumn(r14, r4);
        r0 = r24;
        r0.lastSuccessSource = r4;
        r4 = "lastSuccessTime";
        r4 = getLongColumn(r14, r4);
        r0 = r24;
        r0.lastSuccessTime = r4;
        r4 = "lastFailureSource";
        r4 = getIntColumn(r14, r4);
        r0 = r24;
        r0.lastFailureSource = r4;
        r4 = "lastFailureTime";
        r4 = getLongColumn(r14, r4);
        r0 = r24;
        r0.lastFailureTime = r4;
        r4 = "lastFailureMesg";
        r4 = r14.getColumnIndex(r4);
        r4 = r14.getString(r4);
        r0 = r24;
        r0.lastFailureMesg = r4;
        r4 = "pending";
        r4 = getIntColumn(r14, r4);
        if (r4 == 0) goto L_0x0230;
    L_0x021f:
        r4 = 1;
    L_0x0220:
        r0 = r24;
        r0.pending = r4;
        goto L_0x0115;
    L_0x0226:
        r15 = move-exception;
        goto L_0x001f;
    L_0x0229:
        r18 = 0;
        goto L_0x002b;
    L_0x022d:
        r11 = 0;
        goto L_0x0133;
    L_0x0230:
        r4 = 0;
        goto L_0x0220;
    L_0x0232:
        r14.close();
        r2 = new android.database.sqlite.SQLiteQueryBuilder;
        r2.<init>();
        r4 = "settings";
        r2.setTables(r4);
        r4 = 0;
        r5 = 0;
        r6 = 0;
        r7 = 0;
        r8 = 0;
        r9 = 0;
        r14 = r2.query(r3, r4, r5, r6, r7, r8, r9);
    L_0x024a:
        r4 = r14.moveToNext();
        if (r4 == 0) goto L_0x02d0;
    L_0x0250:
        r4 = "name";
        r4 = r14.getColumnIndex(r4);
        r21 = r14.getString(r4);
        r4 = "value";
        r4 = r14.getColumnIndex(r4);
        r25 = r14.getString(r4);
        if (r21 == 0) goto L_0x024a;
    L_0x0268:
        r4 = "listen_for_tickles";
        r0 = r21;
        r4 = r0.equals(r4);
        if (r4 == 0) goto L_0x0282;
    L_0x0273:
        if (r25 == 0) goto L_0x0280;
    L_0x0275:
        r4 = java.lang.Boolean.parseBoolean(r25);
    L_0x0279:
        r5 = 0;
        r0 = r26;
        r0.setMasterSyncAutomatically(r4, r5);
        goto L_0x024a;
    L_0x0280:
        r4 = 1;
        goto L_0x0279;
    L_0x0282:
        r4 = "sync_provider_";
        r0 = r21;
        r4 = r0.startsWith(r4);
        if (r4 == 0) goto L_0x024a;
    L_0x028d:
        r4 = "sync_provider_";
        r4 = r4.length();
        r5 = r21.length();
        r0 = r21;
        r23 = r0.substring(r4, r5);
        r0 = r26;
        r4 = r0.mAuthorities;
        r19 = r4.size();
    L_0x02a6:
        if (r19 <= 0) goto L_0x024a;
    L_0x02a8:
        r19 = r19 + -1;
        r0 = r26;
        r4 = r0.mAuthorities;
        r0 = r19;
        r12 = r4.valueAt(r0);
        r12 = (com.android.server.content.SyncStorageEngine.AuthorityInfo) r12;
        r4 = r12.target;
        r4 = r4.provider;
        r0 = r23;
        r4 = r4.equals(r0);
        if (r4 == 0) goto L_0x02a6;
    L_0x02c2:
        if (r25 == 0) goto L_0x02ce;
    L_0x02c4:
        r4 = java.lang.Boolean.parseBoolean(r25);
    L_0x02c8:
        r12.enabled = r4;
        r4 = 1;
        r12.syncable = r4;
        goto L_0x02a6;
    L_0x02ce:
        r4 = 1;
        goto L_0x02c8;
    L_0x02d0:
        r14.close();
        r3.close();
        r4 = new java.io.File;
        r0 = r22;
        r4.<init>(r0);
        r4.delete();
    L_0x02e0:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.content.SyncStorageEngine.readAndDeleteLegacyAccountInfoLocked():void");
    }

    private void readStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Reading " + this.mStatusFile.getBaseFile());
        }
        try {
            byte[] data = this.mStatusFile.readFully();
            Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            while (true) {
                int token = in.readInt();
                if (token == 0) {
                    return;
                }
                if (token == 100) {
                    SyncStatusInfo status = new SyncStatusInfo(in);
                    if (this.mAuthorities.indexOfKey(status.authorityId) >= 0) {
                        status.pending = false;
                        if (Log.isLoggable(TAG_FILE, 2)) {
                            Slog.v(TAG_FILE, "Adding status for id " + status.authorityId);
                        }
                        this.mSyncStatus.put(status.authorityId, status);
                    }
                } else {
                    Slog.w("SyncManager", "Unknown status token: " + token);
                    return;
                }
            }
        } catch (IOException e) {
            Slog.i("SyncManager", "No initial status");
        }
    }

    private void writeStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + this.mStatusFile.getBaseFile());
        }
        removeMessages(1);
        try {
            FileOutputStream fos = this.mStatusFile.startWrite();
            Parcel out = Parcel.obtain();
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo status = (SyncStatusInfo) this.mSyncStatus.valueAt(i);
                out.writeInt(100);
                status.writeToParcel(out, 0);
            }
            out.writeInt(0);
            fos.write(out.marshall());
            out.recycle();
            this.mStatusFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w("SyncManager", "Error writing status", e1);
            if (null != null) {
                this.mStatusFile.failWrite(null);
            }
        }
    }

    private void requestSync(AuthorityInfo authorityInfo, int reason, Bundle extras) {
        if (Process.myUid() != 1000 || this.mSyncRequestListener == null) {
            Builder req = new Builder().syncOnce().setExtras(extras);
            req.setSyncAdapter(authorityInfo.target.account, authorityInfo.target.provider);
            ContentResolver.requestSync(req.build());
            return;
        }
        this.mSyncRequestListener.onSyncRequest(authorityInfo.target, reason, extras);
    }

    private void requestSync(Account account, int userId, int reason, String authority, Bundle extras) {
        if (Process.myUid() != 1000 || this.mSyncRequestListener == null) {
            ContentResolver.requestSync(account, authority, extras);
        } else {
            this.mSyncRequestListener.onSyncRequest(new EndPoint(account, authority, userId), reason, extras);
        }
    }

    private void readStatisticsLocked() {
        try {
            byte[] data = this.mStatisticsFile.readFully();
            Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            int index = 0;
            while (true) {
                int token = in.readInt();
                if (token == 0) {
                    return;
                }
                if (token == 101 || token == 100) {
                    int day = in.readInt();
                    if (token == 100) {
                        day = (day - 2009) + 14245;
                    }
                    DayStats ds = new DayStats(day);
                    ds.successCount = in.readInt();
                    ds.successTime = in.readLong();
                    ds.failureCount = in.readInt();
                    ds.failureTime = in.readLong();
                    if (index < this.mDayStats.length) {
                        this.mDayStats[index] = ds;
                        index++;
                    }
                } else {
                    Slog.w("SyncManager", "Unknown stats token: " + token);
                    return;
                }
            }
        } catch (IOException e) {
            Slog.i("SyncManager", "No initial statistics");
        }
    }

    private void writeStatisticsLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v("SyncManager", "Writing new " + this.mStatisticsFile.getBaseFile());
        }
        removeMessages(2);
        try {
            FileOutputStream fos = this.mStatisticsFile.startWrite();
            Parcel out = Parcel.obtain();
            for (DayStats ds : this.mDayStats) {
                if (ds == null) {
                    break;
                }
                out.writeInt(101);
                out.writeInt(ds.day);
                out.writeInt(ds.successCount);
                out.writeLong(ds.successTime);
                out.writeInt(ds.failureCount);
                out.writeLong(ds.failureTime);
            }
            out.writeInt(0);
            fos.write(out.marshall());
            out.recycle();
            this.mStatisticsFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w("SyncManager", "Error writing stats", e1);
            if (null != null) {
                this.mStatisticsFile.failWrite(null);
            }
        }
    }

    public void queueBackup() {
        BackupManager.dataChanged("android");
    }
}
