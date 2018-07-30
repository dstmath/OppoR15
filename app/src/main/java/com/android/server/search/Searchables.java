package com.android.server.search;

import android.app.AppGlobals;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Searchables {
    public static String ENHANCED_GOOGLE_SEARCH_COMPONENT_NAME = "com.google.android.providers.enhancedgooglesearch/.Launcher";
    private static final Comparator<ResolveInfo> GLOBAL_SEARCH_RANKER = new Comparator<ResolveInfo>() {
        public int compare(ResolveInfo lhs, ResolveInfo rhs) {
            if (lhs == rhs) {
                return 0;
            }
            boolean lhsSystem = Searchables.isSystemApp(lhs);
            boolean rhsSystem = Searchables.isSystemApp(rhs);
            if (lhsSystem && (rhsSystem ^ 1) != 0) {
                return -1;
            }
            if (!rhsSystem || (lhsSystem ^ 1) == 0) {
                return rhs.priority - lhs.priority;
            }
            return 1;
        }
    };
    public static String GOOGLE_SEARCH_COMPONENT_NAME = "com.android.googlesearch/.GoogleSearch";
    private static final String LOG_TAG = "Searchables";
    private static final String MD_LABEL_DEFAULT_SEARCHABLE = "android.app.default_searchable";
    private static final String MD_SEARCHABLE_SYSTEM_SEARCH = "*";
    private Context mContext;
    private ComponentName mCurrentGlobalSearchActivity = null;
    private List<ResolveInfo> mGlobalSearchActivities;
    private final IPackageManager mPm;
    private ArrayList<SearchableInfo> mSearchablesInGlobalSearchList = null;
    private ArrayList<SearchableInfo> mSearchablesList = null;
    private HashMap<ComponentName, SearchableInfo> mSearchablesMap = null;
    private int mUserId;
    private ComponentName mWebSearchActivity = null;

    public Searchables(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
        this.mPm = AppGlobals.getPackageManager();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SearchableInfo getSearchableInfo(ComponentName activity) {
        synchronized (this) {
            SearchableInfo result = (SearchableInfo) this.mSearchablesMap.get(activity);
            if (result != null) {
                return result;
            }
        }
    }

    public void updateSearchableList() {
        HashMap<ComponentName, SearchableInfo> newSearchablesMap = new HashMap();
        ArrayList<SearchableInfo> newSearchablesList = new ArrayList();
        ArrayList<SearchableInfo> newSearchablesInGlobalSearchList = new ArrayList();
        Intent intent = new Intent("android.intent.action.SEARCH");
        long ident = Binder.clearCallingIdentity();
        try {
            List<ResolveInfo> searchList = queryIntentActivities(intent, 268435584);
            List<ResolveInfo> webSearchInfoList = queryIntentActivities(new Intent("android.intent.action.WEB_SEARCH"), 268435584);
            if (!(searchList == null && webSearchInfoList == null)) {
                int search_count = searchList == null ? 0 : searchList.size();
                int count = search_count + (webSearchInfoList == null ? 0 : webSearchInfoList.size());
                for (int ii = 0; ii < count; ii++) {
                    ResolveInfo info;
                    if (ii < search_count) {
                        info = (ResolveInfo) searchList.get(ii);
                    } else {
                        info = (ResolveInfo) webSearchInfoList.get(ii - search_count);
                    }
                    ActivityInfo ai = info.activityInfo;
                    if (newSearchablesMap.get(new ComponentName(ai.packageName, ai.name)) == null) {
                        SearchableInfo searchable = SearchableInfo.getActivityMetaData(this.mContext, ai, this.mUserId);
                        if (searchable != null) {
                            newSearchablesList.add(searchable);
                            newSearchablesMap.put(searchable.getSearchActivity(), searchable);
                            if (searchable.shouldIncludeInGlobalSearch()) {
                                newSearchablesInGlobalSearchList.add(searchable);
                            }
                        }
                    }
                }
            }
            List<ResolveInfo> newGlobalSearchActivities = findGlobalSearchActivities();
            ComponentName newGlobalSearchActivity = findGlobalSearchActivity(newGlobalSearchActivities);
            ComponentName newWebSearchActivity = findWebSearchActivity(newGlobalSearchActivity);
            synchronized (this) {
                this.mSearchablesMap = newSearchablesMap;
                this.mSearchablesList = newSearchablesList;
                this.mSearchablesInGlobalSearchList = newSearchablesInGlobalSearchList;
                this.mGlobalSearchActivities = newGlobalSearchActivities;
                this.mCurrentGlobalSearchActivity = newGlobalSearchActivity;
                this.mWebSearchActivity = newWebSearchActivity;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private List<ResolveInfo> findGlobalSearchActivities() {
        List<ResolveInfo> activities = queryIntentActivities(new Intent("android.search.action.GLOBAL_SEARCH"), 268500992);
        if (!(activities == null || (activities.isEmpty() ^ 1) == 0)) {
            Collections.sort(activities, GLOBAL_SEARCH_RANKER);
        }
        return activities;
    }

    private ComponentName findGlobalSearchActivity(List<ResolveInfo> installed) {
        String searchProviderSetting = getGlobalSearchProviderSetting();
        if (!TextUtils.isEmpty(searchProviderSetting)) {
            ComponentName globalSearchComponent = ComponentName.unflattenFromString(searchProviderSetting);
            if (globalSearchComponent != null && isInstalled(globalSearchComponent)) {
                return globalSearchComponent;
            }
        }
        return getDefaultGlobalSearchProvider(installed);
    }

    private boolean isInstalled(ComponentName globalSearch) {
        Intent intent = new Intent("android.search.action.GLOBAL_SEARCH");
        intent.setComponent(globalSearch);
        List<ResolveInfo> activities = queryIntentActivities(intent, 65536);
        if (activities == null || (activities.isEmpty() ^ 1) == 0) {
            return false;
        }
        return true;
    }

    private static final boolean isSystemApp(ResolveInfo res) {
        return (res.activityInfo.applicationInfo.flags & 1) != 0;
    }

    private ComponentName getDefaultGlobalSearchProvider(List<ResolveInfo> providerList) {
        if (providerList == null || (providerList.isEmpty() ^ 1) == 0) {
            Log.w(LOG_TAG, "No global search activity found");
            return null;
        }
        ActivityInfo ai = ((ResolveInfo) providerList.get(0)).activityInfo;
        return new ComponentName(ai.packageName, ai.name);
    }

    private String getGlobalSearchProviderSetting() {
        return Secure.getString(this.mContext.getContentResolver(), "search_global_search_activity");
    }

    private ComponentName findWebSearchActivity(ComponentName globalSearchActivity) {
        if (globalSearchActivity == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.WEB_SEARCH");
        intent.setPackage(globalSearchActivity.getPackageName());
        List<ResolveInfo> activities = queryIntentActivities(intent, 65536);
        if (activities == null || (activities.isEmpty() ^ 1) == 0) {
            Log.w(LOG_TAG, "No web search activity found");
            return null;
        }
        ActivityInfo ai = ((ResolveInfo) activities.get(0)).activityInfo;
        return new ComponentName(ai.packageName, ai.name);
    }

    private List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        List<ResolveInfo> activities = null;
        try {
            return this.mPm.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), flags, this.mUserId).getList();
        } catch (RemoteException e) {
            return activities;
        }
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesList() {
        return new ArrayList(this.mSearchablesList);
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesInGlobalSearchList() {
        return new ArrayList(this.mSearchablesInGlobalSearchList);
    }

    public synchronized ArrayList<ResolveInfo> getGlobalSearchActivities() {
        return new ArrayList(this.mGlobalSearchActivities);
    }

    public synchronized ComponentName getGlobalSearchActivity() {
        return this.mCurrentGlobalSearchActivity;
    }

    public synchronized ComponentName getWebSearchActivity() {
        return this.mWebSearchActivity;
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Searchable authorities:");
        synchronized (this) {
            if (this.mSearchablesList != null) {
                for (SearchableInfo info : this.mSearchablesList) {
                    pw.print("  ");
                    pw.println(info.getSuggestAuthority());
                }
            }
        }
    }
}
