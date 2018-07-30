package com.android.server.notification;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.LruCache;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ValidateNotificationPeople implements NotificationSignalExtractor {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final boolean ENABLE_PEOPLE_VALIDATOR = true;
    private static final String[] LOOKUP_PROJECTION = new String[]{"_id", "starred"};
    private static final int MAX_PEOPLE = 10;
    static final float NONE = 0.0f;
    private static final int PEOPLE_CACHE_SIZE = 200;
    private static final String SETTING_ENABLE_PEOPLE_VALIDATOR = "validate_notification_people_enabled";
    static final float STARRED_CONTACT = 1.0f;
    private static final String TAG = "ValidateNoPeople";
    static final float VALID_CONTACT = 0.5f;
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private Context mBaseContext;
    protected boolean mEnabled;
    private int mEvictionCount;
    private Handler mHandler;
    private ContentObserver mObserver;
    private LruCache<String, LookupResult> mPeopleCache;
    private NotificationUsageStats mUsageStats;
    private Map<Integer, Context> mUserToContextMap;

    private static class LookupResult {
        private static final long CONTACT_REFRESH_MILLIS = 3600000;
        private float mAffinity = 0.0f;
        private final long mExpireMillis = (System.currentTimeMillis() + CONTACT_REFRESH_MILLIS);

        public void mergeContact(Cursor cursor) {
            this.mAffinity = Math.max(this.mAffinity, 0.5f);
            int idIdx = cursor.getColumnIndex("_id");
            if (idIdx >= 0) {
                int id = cursor.getInt(idIdx);
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "contact _ID is: " + id);
                }
            } else {
                Slog.i(ValidateNotificationPeople.TAG, "invalid cursor: no _ID");
            }
            int starIdx = cursor.getColumnIndex("starred");
            if (starIdx >= 0) {
                boolean isStarred = cursor.getInt(starIdx) != 0;
                if (isStarred) {
                    this.mAffinity = Math.max(this.mAffinity, 1.0f);
                }
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "contact STARRED is: " + isStarred);
                }
            } else if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "invalid cursor: no STARRED");
            }
        }

        private boolean isExpired() {
            return this.mExpireMillis < System.currentTimeMillis();
        }

        private boolean isInvalid() {
            return this.mAffinity != 0.0f ? isExpired() : true;
        }

        public float getAffinity() {
            if (isInvalid()) {
                return 0.0f;
            }
            return this.mAffinity;
        }
    }

    private class PeopleRankingReconsideration extends RankingReconsideration {
        private float mContactAffinity;
        private final Context mContext;
        private final LinkedList<String> mPendingLookups;
        private NotificationRecord mRecord;

        /* synthetic */ PeopleRankingReconsideration(ValidateNotificationPeople this$0, Context context, String key, LinkedList pendingLookups, PeopleRankingReconsideration -this4) {
            this(context, key, pendingLookups);
        }

        private PeopleRankingReconsideration(Context context, String key, LinkedList<String> pendingLookups) {
            super(key);
            this.mContactAffinity = 0.0f;
            this.mContext = context;
            this.mPendingLookups = pendingLookups;
        }

        public void work() {
            long start = SystemClock.elapsedRealtime();
            if (ValidateNotificationPeople.VERBOSE) {
                Slog.i(ValidateNotificationPeople.TAG, "Executing: validation for: " + this.mKey);
            }
            long timeStartMs = System.currentTimeMillis();
            for (String handle : this.mPendingLookups) {
                LookupResult lookupResult;
                Uri uri = Uri.parse(handle);
                if ("tel".equals(uri.getScheme())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "checking telephone URI: " + handle);
                    }
                    lookupResult = ValidateNotificationPeople.this.resolvePhoneContact(this.mContext, uri.getSchemeSpecificPart());
                } else if ("mailto".equals(uri.getScheme())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "checking mailto URI: " + handle);
                    }
                    lookupResult = ValidateNotificationPeople.this.resolveEmailContact(this.mContext, uri.getSchemeSpecificPart());
                } else if (handle.startsWith(Contacts.CONTENT_LOOKUP_URI.toString())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "checking lookup URI: " + handle);
                    }
                    lookupResult = ValidateNotificationPeople.this.searchContacts(this.mContext, uri);
                } else {
                    lookupResult = new LookupResult();
                    Slog.w(ValidateNotificationPeople.TAG, "unsupported URI " + handle);
                }
                if (lookupResult != null) {
                    synchronized (ValidateNotificationPeople.this.mPeopleCache) {
                        ValidateNotificationPeople.this.mPeopleCache.put(ValidateNotificationPeople.this.getCacheKey(this.mContext.getUserId(), handle), lookupResult);
                    }
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "lookup contactAffinity is " + lookupResult.getAffinity());
                    }
                    this.mContactAffinity = Math.max(this.mContactAffinity, lookupResult.getAffinity());
                } else if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "lookupResult is null");
                }
            }
            if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "Validation finished in " + (System.currentTimeMillis() - timeStartMs) + "ms");
            }
            if (this.mRecord != null) {
                ValidateNotificationPeople.this.mUsageStats.registerPeopleAffinity(this.mRecord, this.mContactAffinity > 0.0f, this.mContactAffinity == 1.0f, false);
            }
        }

        public void applyChangesLocked(NotificationRecord operand) {
            operand.setContactAffinity(Math.max(this.mContactAffinity, operand.getContactAffinity()));
            if (ValidateNotificationPeople.VERBOSE) {
                Slog.i(ValidateNotificationPeople.TAG, "final affinity: " + operand.getContactAffinity());
            }
        }

        public float getContactAffinity() {
            return this.mContactAffinity;
        }

        public void setRecord(NotificationRecord record) {
            this.mRecord = record;
        }
    }

    public void initialize(Context context, NotificationUsageStats usageStats) {
        if (DEBUG) {
            Slog.d(TAG, "Initializing  " + getClass().getSimpleName() + ".");
        }
        this.mUserToContextMap = new ArrayMap();
        this.mBaseContext = context;
        this.mUsageStats = usageStats;
        this.mPeopleCache = new LruCache(200);
        this.mEnabled = 1 == Global.getInt(this.mBaseContext.getContentResolver(), SETTING_ENABLE_PEOPLE_VALIDATOR, 1);
        if (this.mEnabled) {
            this.mHandler = new Handler();
            this.mObserver = new ContentObserver(this.mHandler) {
                public void onChange(boolean selfChange, Uri uri, int userId) {
                    super.onChange(selfChange, uri, userId);
                    if ((ValidateNotificationPeople.DEBUG || ValidateNotificationPeople.this.mEvictionCount % 100 == 0) && ValidateNotificationPeople.VERBOSE) {
                        Slog.i(ValidateNotificationPeople.TAG, "mEvictionCount: " + ValidateNotificationPeople.this.mEvictionCount);
                    }
                    ValidateNotificationPeople.this.mPeopleCache.evictAll();
                    ValidateNotificationPeople validateNotificationPeople = ValidateNotificationPeople.this;
                    validateNotificationPeople.mEvictionCount = validateNotificationPeople.mEvictionCount + 1;
                }
            };
            this.mBaseContext.getContentResolver().registerContentObserver(Contacts.CONTENT_URI, true, this.mObserver, -1);
        }
    }

    public RankingReconsideration process(NotificationRecord record) {
        if (!this.mEnabled) {
            if (VERBOSE) {
                Slog.i(TAG, "disabled");
            }
            return null;
        } else if (record == null || record.getNotification() == null) {
            if (VERBOSE) {
                Slog.i(TAG, "skipping empty notification");
            }
            return null;
        } else if (record.getUserId() == -1) {
            if (VERBOSE) {
                Slog.i(TAG, "skipping global notification");
            }
            return null;
        } else {
            Context context = getContextAsUser(record.getUser());
            if (context != null) {
                return validatePeople(context, record);
            }
            if (VERBOSE) {
                Slog.i(TAG, "skipping notification that lacks a context");
            }
            return null;
        }
    }

    public void setConfig(RankingConfig config) {
    }

    public float getContactAffinity(UserHandle userHandle, Bundle extras, int timeoutMs, float timeoutAffinity) {
        if (DEBUG) {
            Slog.d(TAG, "checking affinity for " + userHandle);
        }
        if (extras == null) {
            return 0.0f;
        }
        String key = Long.toString(System.nanoTime());
        float[] affinityOut = new float[1];
        Context context = getContextAsUser(userHandle);
        if (context == null) {
            return 0.0f;
        }
        final PeopleRankingReconsideration prr = validatePeople(context, key, extras, null, affinityOut);
        float affinity = affinityOut[0];
        if (prr != null) {
            final Semaphore s = new Semaphore(0);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    prr.work();
                    s.release();
                }
            });
            try {
                if (s.tryAcquire((long) timeoutMs, TimeUnit.MILLISECONDS)) {
                    affinity = Math.max(prr.getContactAffinity(), affinity);
                } else {
                    Slog.w(TAG, "Timeout while waiting for affinity: " + key + ". " + "Returning timeoutAffinity=" + timeoutAffinity);
                    return timeoutAffinity;
                }
            } catch (InterruptedException e) {
                Slog.w(TAG, "InterruptedException while waiting for affinity: " + key + ". " + "Returning affinity=" + affinity, e);
                return affinity;
            }
        }
        return affinity;
    }

    private Context getContextAsUser(UserHandle userHandle) {
        Context context = (Context) this.mUserToContextMap.get(Integer.valueOf(userHandle.getIdentifier()));
        if (context != null) {
            return context;
        }
        try {
            context = this.mBaseContext.createPackageContextAsUser("android", 0, userHandle);
            this.mUserToContextMap.put(Integer.valueOf(userHandle.getIdentifier()), context);
            return context;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "failed to create package context for lookups", e);
            return context;
        }
    }

    private RankingReconsideration validatePeople(Context context, NotificationRecord record) {
        float[] affinityOut = new float[1];
        PeopleRankingReconsideration rr = validatePeople(context, record.getKey(), record.getNotification().extras, record.getPeopleOverride(), affinityOut);
        float affinity = affinityOut[0];
        record.setContactAffinity(affinity);
        if (rr == null) {
            this.mUsageStats.registerPeopleAffinity(record, affinity > 0.0f, affinity == 1.0f, true);
        } else {
            rr.setRecord(record);
        }
        return rr;
    }

    private PeopleRankingReconsideration validatePeople(Context context, String key, Bundle extras, List<String> peopleOverride, float[] affinityOut) {
        long start = SystemClock.elapsedRealtime();
        float affinity = 0.0f;
        if (extras == null) {
            return null;
        }
        Set<String> people = new ArraySet(peopleOverride);
        if (getExtraPeople(extras) != null) {
            people.addAll(Arrays.asList(getExtraPeople(extras)));
        }
        if (VERBOSE) {
            Slog.i(TAG, "Validating: " + key + " for " + context.getUserId());
        }
        LinkedList<String> pendingLookups = new LinkedList();
        int personIdx = 0;
        for (String handle : people) {
            if (!TextUtils.isEmpty(handle)) {
                synchronized (this.mPeopleCache) {
                    LookupResult lookupResult = (LookupResult) this.mPeopleCache.get(getCacheKey(context.getUserId(), handle));
                    if (lookupResult == null || lookupResult.isExpired()) {
                        pendingLookups.add(handle);
                    } else if (DEBUG) {
                        Slog.d(TAG, "using cached lookupResult");
                    }
                    if (lookupResult != null) {
                        affinity = Math.max(affinity, lookupResult.getAffinity());
                    }
                }
                personIdx++;
                if (personIdx == 10) {
                    break;
                }
            }
        }
        affinityOut[0] = affinity;
        if (pendingLookups.isEmpty()) {
            if (VERBOSE) {
                Slog.i(TAG, "final affinity: " + affinity);
            }
            return null;
        }
        if (DEBUG) {
            Slog.d(TAG, "Pending: future work scheduled for: " + key);
        }
        return new PeopleRankingReconsideration(this, context, key, pendingLookups, null);
    }

    private String getCacheKey(int userId, String handle) {
        return Integer.toString(userId) + ":" + handle;
    }

    public static String[] getExtraPeople(Bundle extras) {
        ArrayList people = extras.get("android.people");
        if (people instanceof String[]) {
            return (String[]) people;
        }
        int N;
        String[] array;
        int i;
        if (people instanceof ArrayList) {
            ArrayList<String> arrayList = people;
            if (arrayList.isEmpty()) {
                return null;
            }
            if (arrayList.get(0) instanceof String) {
                ArrayList<String> stringArray = arrayList;
                return (String[]) arrayList.toArray(new String[arrayList.size()]);
            } else if (!(arrayList.get(0) instanceof CharSequence)) {
                return null;
            } else {
                ArrayList<CharSequence> charSeqList = arrayList;
                N = arrayList.size();
                array = new String[N];
                for (i = 0; i < N; i++) {
                    array[i] = ((CharSequence) arrayList.get(i)).toString();
                }
                return array;
            }
        } else if (people instanceof String) {
            return new String[]{(String) people};
        } else if (people instanceof char[]) {
            return new String[]{new String((char[]) people)};
        } else if (people instanceof CharSequence) {
            return new String[]{((CharSequence) people).toString()};
        } else if (!(people instanceof CharSequence[])) {
            return null;
        } else {
            CharSequence[] charSeqArray = (CharSequence[]) people;
            N = charSeqArray.length;
            array = new String[N];
            for (i = 0; i < N; i++) {
                array[i] = charSeqArray[i].toString();
            }
            return array;
        }
    }

    private LookupResult resolvePhoneContact(Context context, String number) {
        return searchContacts(context, Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)));
    }

    private LookupResult resolveEmailContact(Context context, String email) {
        return searchContacts(context, Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(email)));
    }

    private LookupResult searchContacts(Context context, Uri lookupUri) {
        LookupResult lookupResult = new LookupResult();
        Cursor c = null;
        try {
            c = context.getContentResolver().query(lookupUri, LOOKUP_PROJECTION, null, null, null);
            if (c == null) {
                Slog.w(TAG, "Null cursor from contacts query.");
                if (c != null) {
                    c.close();
                }
                return lookupResult;
            }
            while (c.moveToNext()) {
                lookupResult.mergeContact(c);
            }
            if (c != null) {
                c.close();
            }
            return lookupResult;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }
}
