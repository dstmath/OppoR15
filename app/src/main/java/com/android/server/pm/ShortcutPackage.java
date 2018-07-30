package com.android.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutPackage extends ShortcutPackageItem {
    private static final String ATTR_ACTIVITY = "activity";
    private static final String ATTR_BITMAP_PATH = "bitmap-path";
    private static final String ATTR_CALL_COUNT = "call-count";
    private static final String ATTR_DISABLED_MESSAGE = "dmessage";
    private static final String ATTR_DISABLED_MESSAGE_RES_ID = "dmessageid";
    private static final String ATTR_DISABLED_MESSAGE_RES_NAME = "dmessagename";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_ICON_RES_ID = "icon-res";
    private static final String ATTR_ICON_RES_NAME = "icon-resname";
    private static final String ATTR_ID = "id";
    private static final String ATTR_INTENT_LEGACY = "intent";
    private static final String ATTR_INTENT_NO_EXTRA = "intent-base";
    private static final String ATTR_LAST_RESET = "last-reset";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_NAME_XMLUTILS = "name";
    private static final String ATTR_RANK = "rank";
    private static final String ATTR_TEXT = "text";
    private static final String ATTR_TEXT_RES_ID = "textid";
    private static final String ATTR_TEXT_RES_NAME = "textname";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_TITLE_RES_ID = "titleid";
    private static final String ATTR_TITLE_RES_NAME = "titlename";
    private static final String KEY_BITMAPS = "bitmaps";
    private static final String KEY_BITMAP_BYTES = "bitmapBytes";
    private static final String KEY_DYNAMIC = "dynamic";
    private static final String KEY_MANIFEST = "manifest";
    private static final String KEY_PINNED = "pinned";
    private static final String NAME_CATEGORIES = "categories";
    private static final String TAG = "ShortcutService";
    private static final String TAG_CATEGORIES = "categories";
    private static final String TAG_EXTRAS = "extras";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_INTENT_EXTRAS_LEGACY = "intent-extras";
    static final String TAG_ROOT = "package";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_STRING_ARRAY_XMLUTILS = "string-array";
    private static final String TAG_VERIFY = "ShortcutService.verify";
    private int mApiCallCount;
    private long mLastKnownForegroundElapsedTime;
    private long mLastResetTime;
    private final int mPackageUid;
    final Comparator<ShortcutInfo> mShortcutRankComparator;
    final Comparator<ShortcutInfo> mShortcutTypeAndRankComparator;
    private final ArrayMap<String, ShortcutInfo> mShortcuts;

    private ShortcutPackage(ShortcutUser shortcutUser, int packageUserId, String packageName, ShortcutPackageInfo spi) {
        if (spi == null) {
            spi = ShortcutPackageInfo.newEmpty();
        }
        super(shortcutUser, packageUserId, packageName, spi);
        this.mShortcuts = new ArrayMap();
        this.mShortcutTypeAndRankComparator = -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w.$INST$2;
        this.mShortcutRankComparator = -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w.$INST$3;
        this.mPackageUid = shortcutUser.mService.injectGetPackageUid(packageName, packageUserId);
    }

    public ShortcutPackage(ShortcutUser shortcutUser, int packageUserId, String packageName) {
        this(shortcutUser, packageUserId, packageName, null);
    }

    public int getOwnerUserId() {
        return getPackageUserId();
    }

    public int getPackageUid() {
        return this.mPackageUid;
    }

    public Resources getPackageResources() {
        return this.mShortcutUser.mService.injectGetResourcesForApplicationAsUser(getPackageName(), getPackageUserId());
    }

    public int getShortcutCount() {
        return this.mShortcuts.size();
    }

    protected void onRestoreBlocked() {
        this.mShortcuts.clear();
    }

    protected void onRestored() {
        refreshPinnedFlags();
    }

    public ShortcutInfo findShortcutById(String id) {
        return (ShortcutInfo) this.mShortcuts.get(id);
    }

    private void ensureNotImmutable(ShortcutInfo shortcut) {
        if (shortcut != null && shortcut.isImmutable()) {
            throw new IllegalArgumentException("Manifest shortcut ID=" + shortcut.getId() + " may not be manipulated via APIs");
        }
    }

    public void ensureNotImmutable(String id) {
        ensureNotImmutable((ShortcutInfo) this.mShortcuts.get(id));
    }

    public void ensureImmutableShortcutsNotIncludedWithIds(List<String> shortcutIds) {
        for (int i = shortcutIds.size() - 1; i >= 0; i--) {
            ensureNotImmutable((String) shortcutIds.get(i));
        }
    }

    public void ensureImmutableShortcutsNotIncluded(List<ShortcutInfo> shortcuts) {
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            ensureNotImmutable(((ShortcutInfo) shortcuts.get(i)).getId());
        }
    }

    private ShortcutInfo deleteShortcutInner(String id) {
        ShortcutInfo shortcut = (ShortcutInfo) this.mShortcuts.remove(id);
        if (shortcut != null) {
            this.mShortcutUser.mService.removeIconLocked(shortcut);
            shortcut.clearFlags(35);
        }
        return shortcut;
    }

    private void addShortcutInner(ShortcutInfo newShortcut) {
        ShortcutService s = this.mShortcutUser.mService;
        deleteShortcutInner(newShortcut.getId());
        s.saveIconAndFixUpShortcutLocked(newShortcut);
        s.fixUpShortcutResourceNamesAndValues(newShortcut);
        this.mShortcuts.put(newShortcut.getId(), newShortcut);
    }

    public void addOrUpdateDynamicShortcut(ShortcutInfo newShortcut) {
        boolean wasPinned;
        Preconditions.checkArgument(newShortcut.isEnabled(), "add/setDynamicShortcuts() cannot publish disabled shortcuts");
        newShortcut.addFlags(1);
        ShortcutInfo oldShortcut = (ShortcutInfo) this.mShortcuts.get(newShortcut.getId());
        if (oldShortcut == null) {
            wasPinned = false;
        } else {
            oldShortcut.ensureUpdatableWith(newShortcut);
            wasPinned = oldShortcut.isPinned();
        }
        if (wasPinned) {
            newShortcut.addFlags(2);
        }
        addShortcutInner(newShortcut);
    }

    private void removeOrphans() {
        int i;
        ArrayList removeList = null;
        for (i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (!si.isAlive()) {
                if (removeList == null) {
                    removeList = new ArrayList();
                }
                removeList.add(si.getId());
            }
        }
        if (removeList != null) {
            for (i = removeList.size() - 1; i >= 0; i--) {
                deleteShortcutInner((String) removeList.get(i));
            }
        }
    }

    public void deleteAllDynamicShortcuts() {
        long now = this.mShortcutUser.mService.injectCurrentTimeMillis();
        boolean changed = false;
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.isDynamic()) {
                changed = true;
                si.setTimestamp(now);
                si.clearFlags(1);
                si.setRank(0);
            }
        }
        if (changed) {
            removeOrphans();
        }
    }

    public boolean deleteDynamicWithId(String shortcutId) {
        if (deleteOrDisableWithId(shortcutId, false, false) == null) {
            return true;
        }
        return false;
    }

    private boolean disableDynamicWithId(String shortcutId) {
        if (deleteOrDisableWithId(shortcutId, true, false) == null) {
            return true;
        }
        return false;
    }

    public void disableWithId(String shortcutId, String disabledMessage, int disabledMessageResId, boolean overrideImmutable) {
        ShortcutInfo disabled = deleteOrDisableWithId(shortcutId, true, overrideImmutable);
        if (disabled == null) {
            return;
        }
        if (disabledMessage != null) {
            disabled.setDisabledMessage(disabledMessage);
        } else if (disabledMessageResId != 0) {
            disabled.setDisabledMessageResId(disabledMessageResId);
            this.mShortcutUser.mService.fixUpShortcutResourceNamesAndValues(disabled);
        }
    }

    private ShortcutInfo deleteOrDisableWithId(String shortcutId, boolean disable, boolean overrideImmutable) {
        ShortcutInfo oldShortcut = (ShortcutInfo) this.mShortcuts.get(shortcutId);
        if (oldShortcut == null || (oldShortcut.isEnabled() ^ 1) != 0) {
            return null;
        }
        if (!overrideImmutable) {
            ensureNotImmutable(oldShortcut);
        }
        if (oldShortcut.isPinned()) {
            oldShortcut.setRank(0);
            oldShortcut.clearFlags(33);
            if (disable) {
                oldShortcut.addFlags(64);
            }
            oldShortcut.setTimestamp(this.mShortcutUser.mService.injectCurrentTimeMillis());
            if (this.mShortcutUser.mService.isDummyMainActivity(oldShortcut.getActivity())) {
                oldShortcut.setActivity(null);
            }
            return oldShortcut;
        }
        deleteShortcutInner(shortcutId);
        return null;
    }

    public void enableWithId(String shortcutId) {
        ShortcutInfo shortcut = (ShortcutInfo) this.mShortcuts.get(shortcutId);
        if (shortcut != null) {
            ensureNotImmutable(shortcut);
            shortcut.clearFlags(64);
        }
    }

    public void refreshPinnedFlags() {
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ((ShortcutInfo) this.mShortcuts.valueAt(i)).clearFlags(2);
        }
        this.mShortcutUser.mService.getUserShortcutsLocked(getPackageUserId()).forAllLaunchers(new -$Lambda$4qJi2sHY5X4ys3rtlAQIsVPSn60((byte) 1, this));
        removeOrphans();
    }

    /* synthetic */ void lambda$-com_android_server_pm_ShortcutPackage_14748(ShortcutLauncher launcherShortcuts) {
        ArraySet<String> pinned = launcherShortcuts.getPinnedShortcutIds(getPackageName(), getPackageUserId());
        if (pinned != null && pinned.size() != 0) {
            for (int i = pinned.size() - 1; i >= 0; i--) {
                ShortcutInfo si = (ShortcutInfo) this.mShortcuts.get((String) pinned.valueAt(i));
                if (si != null) {
                    si.addFlags(2);
                }
            }
        }
    }

    public int getApiCallCount() {
        ShortcutService s = this.mShortcutUser.mService;
        if (s.isUidForegroundLocked(this.mPackageUid) || this.mLastKnownForegroundElapsedTime < s.getUidLastForegroundElapsedTimeLocked(this.mPackageUid)) {
            this.mLastKnownForegroundElapsedTime = s.injectElapsedRealtime();
            resetRateLimiting();
        }
        long last = s.getLastResetTimeLocked();
        long now = s.injectCurrentTimeMillis();
        if (!ShortcutService.isClockValid(now) || this.mLastResetTime <= now) {
            if (this.mLastResetTime < last) {
                this.mApiCallCount = 0;
                this.mLastResetTime = last;
            }
            return this.mApiCallCount;
        }
        Slog.w(TAG, "Clock rewound");
        this.mLastResetTime = now;
        this.mApiCallCount = 0;
        return this.mApiCallCount;
    }

    public boolean tryApiCall() {
        ShortcutService s = this.mShortcutUser.mService;
        if (getApiCallCount() >= s.mMaxUpdatesPerInterval) {
            return false;
        }
        this.mApiCallCount++;
        s.scheduleSaveUser(getOwnerUserId());
        return true;
    }

    public void resetRateLimiting() {
        if (this.mApiCallCount > 0) {
            this.mApiCallCount = 0;
            this.mShortcutUser.mService.scheduleSaveUser(getOwnerUserId());
        }
    }

    public void resetRateLimitingForCommandLineNoSaving() {
        this.mApiCallCount = 0;
        this.mLastResetTime = 0;
    }

    public void findAll(List<ShortcutInfo> result, Predicate<ShortcutInfo> query, int cloneFlag) {
        findAll(result, query, cloneFlag, null, 0);
    }

    public void findAll(List<ShortcutInfo> result, Predicate<ShortcutInfo> query, int cloneFlag, String callingLauncher, int launcherUserId) {
        if (!getPackageInfo().isShadow()) {
            ArraySet pinnedByCallerSet;
            ShortcutService s = this.mShortcutUser.mService;
            if (callingLauncher == null) {
                pinnedByCallerSet = null;
            } else {
                pinnedByCallerSet = s.getLauncherShortcutsLocked(callingLauncher, getPackageUserId(), launcherUserId).getPinnedShortcutIds(getPackageName(), getPackageUserId());
            }
            for (int i = 0; i < this.mShortcuts.size(); i++) {
                ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
                boolean isPinnedByCaller = callingLauncher != null ? pinnedByCallerSet != null ? pinnedByCallerSet.contains(si.getId()) : false : true;
                if (!si.isFloating() || isPinnedByCaller) {
                    ShortcutInfo clone = si.clone(cloneFlag);
                    if (!isPinnedByCaller) {
                        clone.clearFlags(2);
                    }
                    if (query == null || query.test(clone)) {
                        result.add(clone);
                    }
                }
            }
        }
    }

    public void resetThrottling() {
        this.mApiCallCount = 0;
    }

    public ArraySet<String> getUsedBitmapFiles() {
        ArraySet<String> usedFiles = new ArraySet(this.mShortcuts.size());
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.getBitmapPath() != null) {
                usedFiles.add(getFileName(si.getBitmapPath()));
            }
        }
        return usedFiles;
    }

    private static String getFileName(String path) {
        int sep = path.lastIndexOf(File.separatorChar);
        if (sep == -1) {
            return path;
        }
        return path.substring(sep + 1);
    }

    private boolean areAllActivitiesStillEnabled() {
        if (this.mShortcuts.size() == 0) {
            return true;
        }
        ShortcutService s = this.mShortcutUser.mService;
        ArrayList<ComponentName> checked = new ArrayList(4);
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ComponentName activity = ((ShortcutInfo) this.mShortcuts.valueAt(i)).getActivity();
            if (!checked.contains(activity)) {
                checked.add(activity);
                if (!(activity == null || (s.injectIsActivityEnabledAndExported(activity, getOwnerUserId()) ^ 1) == 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean rescanPackageIfNeeded(boolean isNewApp, boolean forceRescan) {
        ShortcutService s = this.mShortcutUser.mService;
        long start = s.injectElapsedRealtime();
        try {
            PackageInfo pi = this.mShortcutUser.mService.getPackageInfo(getPackageName(), getPackageUserId());
            if (pi == null) {
                return false;
            }
            if (!(isNewApp || (forceRescan ^ 1) == 0)) {
                if (getPackageInfo().getVersionCode() == pi.versionCode && getPackageInfo().getLastUpdateTime() == pi.lastUpdateTime && areAllActivitiesStillEnabled()) {
                    s.logDurationStat(14, start);
                    return false;
                }
            }
            s.logDurationStat(14, start);
            List newManifestShortcutList = null;
            try {
                newManifestShortcutList = ShortcutParser.parseShortcuts(this.mShortcutUser.mService, getPackageName(), getPackageUserId());
            } catch (Exception e) {
                Slog.e(TAG, "Failed to load shortcuts from AndroidManifest.xml.", e);
            }
            int manifestShortcutSize;
            if (newManifestShortcutList == null) {
                manifestShortcutSize = 0;
            } else {
                manifestShortcutSize = newManifestShortcutList.size();
            }
            if (isNewApp && manifestShortcutSize == 0) {
                return false;
            }
            getPackageInfo().updateVersionInfo(pi);
            if (!isNewApp) {
                Resources publisherRes = null;
                for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
                    ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
                    if (si.isDynamic()) {
                        if (si.getActivity() == null) {
                            s.wtf("null activity detected.");
                        } else if (!s.injectIsMainActivity(si.getActivity(), getPackageUserId())) {
                            Slog.w(TAG, String.format("%s is no longer main activity. Disabling shorcut %s.", new Object[]{getPackageName(), si.getId()}));
                            if (disableDynamicWithId(si.getId())) {
                                continue;
                            }
                        }
                    }
                    if (si.hasAnyResources()) {
                        if (!si.isOriginallyFromManifest()) {
                            if (publisherRes == null) {
                                publisherRes = getPackageResources();
                                if (publisherRes == null) {
                                    break;
                                }
                            }
                            si.lookupAndFillInResourceIds(publisherRes);
                        }
                        si.setTimestamp(s.injectCurrentTimeMillis());
                    } else {
                        continue;
                    }
                }
            }
            publishManifestShortcuts(newManifestShortcutList);
            if (newManifestShortcutList != null) {
                pushOutExcessShortcuts();
            }
            s.verifyStates();
            s.packageShortcutsChanged(getPackageName(), getPackageUserId());
            return true;
        } finally {
            s.logDurationStat(14, start);
        }
    }

    private boolean publishManifestShortcuts(List<ShortcutInfo> newManifestShortcutList) {
        int i;
        boolean changed = false;
        ArraySet toDisableList = null;
        for (i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.isManifestShortcut()) {
                if (toDisableList == null) {
                    toDisableList = new ArraySet();
                }
                toDisableList.add(si.getId());
            }
        }
        if (newManifestShortcutList != null) {
            int newListSize = newManifestShortcutList.size();
            i = 0;
            while (i < newListSize) {
                changed = true;
                ShortcutInfo newShortcut = (ShortcutInfo) newManifestShortcutList.get(i);
                boolean newDisabled = newShortcut.isEnabled() ^ 1;
                String id = newShortcut.getId();
                ShortcutInfo oldShortcut = (ShortcutInfo) this.mShortcuts.get(id);
                boolean wasPinned = false;
                if (oldShortcut != null) {
                    if (!oldShortcut.isOriginallyFromManifest()) {
                        Slog.e(TAG, "Shortcut with ID=" + newShortcut.getId() + " exists but is not from AndroidManifest.xml, not updating.");
                        i++;
                    } else if (oldShortcut.isPinned()) {
                        wasPinned = true;
                        newShortcut.addFlags(2);
                    }
                }
                if (!newDisabled || (wasPinned ^ 1) == 0) {
                    addShortcutInner(newShortcut);
                    if (!(newDisabled || toDisableList == null)) {
                        toDisableList.remove(id);
                    }
                    i++;
                } else {
                    i++;
                }
            }
        }
        if (toDisableList != null) {
            for (i = toDisableList.size() - 1; i >= 0; i--) {
                changed = true;
                disableWithId((String) toDisableList.valueAt(i), null, 0, true);
            }
            removeOrphans();
        }
        adjustRanks();
        return changed;
    }

    private boolean pushOutExcessShortcuts() {
        ShortcutService service = this.mShortcutUser.mService;
        int maxShortcuts = service.getMaxActivityShortcuts();
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
        for (int outer = all.size() - 1; outer >= 0; outer--) {
            ArrayList<ShortcutInfo> list = (ArrayList) all.valueAt(outer);
            if (list.size() > maxShortcuts) {
                Collections.sort(list, this.mShortcutTypeAndRankComparator);
                for (int inner = list.size() - 1; inner >= maxShortcuts; inner--) {
                    ShortcutInfo shortcut = (ShortcutInfo) list.get(inner);
                    if (shortcut.isManifestShortcut()) {
                        service.wtf("Found manifest shortcuts in excess list.");
                    } else {
                        deleteDynamicWithId(shortcut.getId());
                    }
                }
            }
        }
        return false;
    }

    static /* synthetic */ int lambda$-com_android_server_pm_ShortcutPackage_34437(ShortcutInfo a, ShortcutInfo b) {
        if (a.isManifestShortcut() && (b.isManifestShortcut() ^ 1) != 0) {
            return -1;
        }
        if (a.isManifestShortcut() || !b.isManifestShortcut()) {
            return Integer.compare(a.getRank(), b.getRank());
        }
        return 1;
    }

    private ArrayMap<ComponentName, ArrayList<ShortcutInfo>> sortShortcutsToActivities() {
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> activitiesToShortcuts = new ArrayMap();
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (!si.isFloating()) {
                ComponentName activity = si.getActivity();
                if (activity == null) {
                    this.mShortcutUser.mService.wtf("null activity detected.");
                } else {
                    ArrayList<ShortcutInfo> list = (ArrayList) activitiesToShortcuts.get(activity);
                    if (list == null) {
                        list = new ArrayList();
                        activitiesToShortcuts.put(activity, list);
                    }
                    list.add(si);
                }
            }
        }
        return activitiesToShortcuts;
    }

    private void incrementCountForActivity(ArrayMap<ComponentName, Integer> counts, ComponentName cn, int increment) {
        Integer oldValue = (Integer) counts.get(cn);
        if (oldValue == null) {
            oldValue = Integer.valueOf(0);
        }
        counts.put(cn, Integer.valueOf(oldValue.intValue() + increment));
    }

    public void enforceShortcutCountsBeforeOperation(List<ShortcutInfo> newList, int operation) {
        int i;
        ShortcutService service = this.mShortcutUser.mService;
        ArrayMap<ComponentName, Integer> counts = new ArrayMap(4);
        for (i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo shortcut = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (shortcut.isManifestShortcut()) {
                incrementCountForActivity(counts, shortcut.getActivity(), 1);
            } else if (shortcut.isDynamic() && operation != 0) {
                incrementCountForActivity(counts, shortcut.getActivity(), 1);
            }
        }
        for (i = newList.size() - 1; i >= 0; i--) {
            ShortcutInfo newShortcut = (ShortcutInfo) newList.get(i);
            ComponentName newActivity = newShortcut.getActivity();
            if (newActivity != null) {
                ShortcutInfo original = (ShortcutInfo) this.mShortcuts.get(newShortcut.getId());
                if (original == null) {
                    if (operation != 2) {
                        incrementCountForActivity(counts, newActivity, 1);
                    }
                } else if (!original.isFloating() || operation != 2) {
                    if (operation != 0) {
                        ComponentName oldActivity = original.getActivity();
                        if (!original.isFloating()) {
                            incrementCountForActivity(counts, oldActivity, -1);
                        }
                    }
                    incrementCountForActivity(counts, newActivity, 1);
                }
            } else if (operation != 2) {
                service.wtf("Activity must not be null at this point");
            }
        }
        for (i = counts.size() - 1; i >= 0; i--) {
            service.enforceMaxActivityShortcuts(((Integer) counts.valueAt(i)).intValue());
        }
    }

    public void resolveResourceStrings() {
        ShortcutService s = this.mShortcutUser.mService;
        boolean changed = false;
        Resources publisherRes = null;
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.hasStringResources()) {
                changed = true;
                if (publisherRes == null) {
                    publisherRes = getPackageResources();
                    if (publisherRes == null) {
                        break;
                    }
                }
                si.resolveResourceStrings(publisherRes);
                si.setTimestamp(s.injectCurrentTimeMillis());
            }
        }
        if (changed) {
            s.packageShortcutsChanged(getPackageName(), getPackageUserId());
        }
    }

    public void clearAllImplicitRanks() {
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ((ShortcutInfo) this.mShortcuts.valueAt(i)).clearImplicitRankAndRankChangedFlag();
        }
    }

    static /* synthetic */ int lambda$-com_android_server_pm_ShortcutPackage_41485(ShortcutInfo a, ShortcutInfo b) {
        int ret = Integer.compare(a.getRank(), b.getRank());
        if (ret != 0) {
            return ret;
        }
        if (a.isRankChanged() != b.isRankChanged()) {
            return a.isRankChanged() ? -1 : 1;
        }
        ret = Integer.compare(a.getImplicitRank(), b.getImplicitRank());
        if (ret != 0) {
            return ret;
        }
        return a.getId().compareTo(b.getId());
    }

    public void adjustRanks() {
        int i;
        ShortcutInfo si;
        ShortcutService s = this.mShortcutUser.mService;
        long now = s.injectCurrentTimeMillis();
        for (i = this.mShortcuts.size() - 1; i >= 0; i--) {
            si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.isFloating() && si.getRank() != 0) {
                si.setTimestamp(now);
                si.setRank(0);
            }
        }
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
        for (int outer = all.size() - 1; outer >= 0; outer--) {
            ArrayList<ShortcutInfo> list = (ArrayList) all.valueAt(outer);
            Collections.sort(list, this.mShortcutRankComparator);
            int size = list.size();
            i = 0;
            int rank = 0;
            while (i < size) {
                int rank2;
                si = (ShortcutInfo) list.get(i);
                if (si.isManifestShortcut()) {
                    rank2 = rank;
                } else if (si.isDynamic()) {
                    rank2 = rank + 1;
                    int thisRank = rank;
                    if (si.getRank() != rank) {
                        si.setTimestamp(now);
                        si.setRank(rank);
                    }
                } else {
                    s.wtf("Non-dynamic shortcut found.");
                    rank2 = rank;
                }
                i++;
                rank = rank2;
            }
        }
    }

    public boolean hasNonManifestShortcuts() {
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            if (!((ShortcutInfo) this.mShortcuts.valueAt(i)).isDeclaredInManifest()) {
                return true;
            }
        }
        return false;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println();
        pw.print(prefix);
        pw.print("Package: ");
        pw.print(getPackageName());
        pw.print("  UID: ");
        pw.print(this.mPackageUid);
        pw.println();
        pw.print(prefix);
        pw.print("  ");
        pw.print("Calls: ");
        pw.print(getApiCallCount());
        pw.println();
        pw.print(prefix);
        pw.print("  ");
        pw.print("Last known FG: ");
        pw.print(this.mLastKnownForegroundElapsedTime);
        pw.println();
        pw.print(prefix);
        pw.print("  ");
        pw.print("Last reset: [");
        pw.print(this.mLastResetTime);
        pw.print("] ");
        pw.print(ShortcutService.formatTime(this.mLastResetTime));
        pw.println();
        getPackageInfo().dump(pw, prefix + "  ");
        pw.println();
        pw.print(prefix);
        pw.println("  Shortcuts:");
        long totalBitmapSize = 0;
        ArrayMap<String, ShortcutInfo> shortcuts = this.mShortcuts;
        int size = shortcuts.size();
        for (int i = 0; i < size; i++) {
            ShortcutInfo si = (ShortcutInfo) shortcuts.valueAt(i);
            pw.print(prefix);
            pw.print("    ");
            pw.println(si.toInsecureString());
            if (si.getBitmapPath() != null) {
                long len = new File(si.getBitmapPath()).length();
                pw.print(prefix);
                pw.print("      ");
                pw.print("bitmap size=");
                pw.println(len);
                totalBitmapSize += len;
            }
        }
        pw.print(prefix);
        pw.print("  ");
        pw.print("Total bitmap size: ");
        pw.print(totalBitmapSize);
        pw.print(" (");
        pw.print(Formatter.formatFileSize(this.mShortcutUser.mService.mContext, totalBitmapSize));
        pw.println(")");
    }

    public JSONObject dumpCheckin(boolean clear) throws JSONException {
        JSONObject result = super.dumpCheckin(clear);
        int numDynamic = 0;
        int numPinned = 0;
        int numManifest = 0;
        int numBitmaps = 0;
        long totalBitmapSize = 0;
        ArrayMap<String, ShortcutInfo> shortcuts = this.mShortcuts;
        int size = shortcuts.size();
        for (int i = 0; i < size; i++) {
            ShortcutInfo si = (ShortcutInfo) shortcuts.valueAt(i);
            if (si.isDynamic()) {
                numDynamic++;
            }
            if (si.isDeclaredInManifest()) {
                numManifest++;
            }
            if (si.isPinned()) {
                numPinned++;
            }
            if (si.getBitmapPath() != null) {
                numBitmaps++;
                totalBitmapSize += new File(si.getBitmapPath()).length();
            }
        }
        result.put(KEY_DYNAMIC, numDynamic);
        result.put(KEY_MANIFEST, numManifest);
        result.put(KEY_PINNED, numPinned);
        result.put(KEY_BITMAPS, numBitmaps);
        result.put(KEY_BITMAP_BYTES, totalBitmapSize);
        return result;
    }

    public void saveToXml(XmlSerializer out, boolean forBackup) throws IOException, XmlPullParserException {
        int size = this.mShortcuts.size();
        if (size != 0 || this.mApiCallCount != 0) {
            out.startTag(null, TAG_ROOT);
            ShortcutService.writeAttr(out, "name", getPackageName());
            ShortcutService.writeAttr(out, ATTR_CALL_COUNT, (long) this.mApiCallCount);
            ShortcutService.writeAttr(out, ATTR_LAST_RESET, this.mLastResetTime);
            getPackageInfo().saveToXml(out);
            for (int j = 0; j < size; j++) {
                saveShortcut(out, (ShortcutInfo) this.mShortcuts.valueAt(j), forBackup);
            }
            out.endTag(null, TAG_ROOT);
        }
    }

    private void saveShortcut(XmlSerializer out, ShortcutInfo si, boolean forBackup) throws IOException, XmlPullParserException {
        boolean z = false;
        ShortcutService s = this.mShortcutUser.mService;
        if (forBackup) {
            if (si.isPinned()) {
                z = si.isEnabled();
            }
            if (!z) {
                return;
            }
        }
        if (si.isIconPendingSave()) {
            s.removeIconLocked(si);
        }
        out.startTag(null, TAG_SHORTCUT);
        ShortcutService.writeAttr(out, "id", si.getId());
        ShortcutService.writeAttr(out, "activity", si.getActivity());
        ShortcutService.writeAttr(out, ATTR_TITLE, si.getTitle());
        ShortcutService.writeAttr(out, ATTR_TITLE_RES_ID, (long) si.getTitleResId());
        ShortcutService.writeAttr(out, ATTR_TITLE_RES_NAME, si.getTitleResName());
        ShortcutService.writeAttr(out, ATTR_TEXT, si.getText());
        ShortcutService.writeAttr(out, ATTR_TEXT_RES_ID, (long) si.getTextResId());
        ShortcutService.writeAttr(out, ATTR_TEXT_RES_NAME, si.getTextResName());
        ShortcutService.writeAttr(out, ATTR_DISABLED_MESSAGE, si.getDisabledMessage());
        ShortcutService.writeAttr(out, ATTR_DISABLED_MESSAGE_RES_ID, (long) si.getDisabledMessageResourceId());
        ShortcutService.writeAttr(out, ATTR_DISABLED_MESSAGE_RES_NAME, si.getDisabledMessageResName());
        ShortcutService.writeAttr(out, ATTR_TIMESTAMP, si.getLastChangedTimestamp());
        if (forBackup) {
            ShortcutService.writeAttr(out, ATTR_FLAGS, (long) (si.getFlags() & -2062));
        } else {
            ShortcutService.writeAttr(out, ATTR_RANK, (long) si.getRank());
            ShortcutService.writeAttr(out, ATTR_FLAGS, (long) si.getFlags());
            ShortcutService.writeAttr(out, ATTR_ICON_RES_ID, (long) si.getIconResourceId());
            ShortcutService.writeAttr(out, ATTR_ICON_RES_NAME, si.getIconResName());
            ShortcutService.writeAttr(out, ATTR_BITMAP_PATH, si.getBitmapPath());
        }
        Set<String> cat = si.getCategories();
        if (cat != null && cat.size() > 0) {
            out.startTag(null, "categories");
            XmlUtils.writeStringArrayXml((String[]) cat.toArray(new String[cat.size()]), "categories", out);
            out.endTag(null, "categories");
        }
        Intent[] intentsNoExtras = si.getIntentsNoExtras();
        PersistableBundle[] intentsExtras = si.getIntentPersistableExtrases();
        int numIntents = intentsNoExtras.length;
        for (int i = 0; i < numIntents; i++) {
            out.startTag(null, "intent");
            ShortcutService.writeAttr(out, ATTR_INTENT_NO_EXTRA, intentsNoExtras[i]);
            ShortcutService.writeTagExtra(out, TAG_EXTRAS, intentsExtras[i]);
            out.endTag(null, "intent");
        }
        ShortcutService.writeTagExtra(out, TAG_EXTRAS, si.getExtras());
        out.endTag(null, TAG_SHORTCUT);
    }

    public static ShortcutPackage loadFromXml(ShortcutService s, ShortcutUser shortcutUser, XmlPullParser parser, boolean fromBackup) throws IOException, XmlPullParserException {
        String packageName = ShortcutService.parseStringAttribute(parser, "name");
        ShortcutPackage ret = new ShortcutPackage(shortcutUser, shortcutUser.getUserId(), packageName);
        ret.mApiCallCount = ShortcutService.parseIntAttribute(parser, ATTR_CALL_COUNT);
        ret.mLastResetTime = ShortcutService.parseLongAttribute(parser, ATTR_LAST_RESET);
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return ret;
            }
            if (type == 2) {
                int depth = parser.getDepth();
                String tag = parser.getName();
                if (depth == outerDepth + 1) {
                    if (tag.equals("package-info")) {
                        ret.getPackageInfo().loadFromXml(parser, fromBackup);
                    } else if (tag.equals(TAG_SHORTCUT)) {
                        ShortcutInfo si = parseShortcut(parser, packageName, shortcutUser.getUserId());
                        ret.mShortcuts.put(si.getId(), si);
                    }
                }
                ShortcutService.warnForInvalidTag(depth, tag);
            }
        }
        return ret;
    }

    private static android.content.pm.ShortcutInfo parseShortcut(org.xmlpull.v1.XmlPullParser r38, java.lang.String r39, int r40) throws java.io.IOException, org.xmlpull.v1.XmlPullParserException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r18_2 'categories' java.util.Set) in PHI: PHI: (r18_3 'categories' java.util.Set) = (r18_1 'categories' java.util.Set), (r18_1 'categories' java.util.Set), (r18_1 'categories' java.util.Set), (r18_2 'categories' java.util.Set), (r18_1 'categories' java.util.Set), (r18_1 'categories' java.util.Set), (r18_1 'categories' java.util.Set) binds: {(r18_1 'categories' java.util.Set)=B:39:0x00c0, (r18_1 'categories' java.util.Set)=B:40:0x00c0, (r18_1 'categories' java.util.Set)=B:41:0x00c0, (r18_2 'categories' java.util.Set)=B:44:0x00c0, (r18_1 'categories' java.util.Set)=B:43:0x00c0, (r18_1 'categories' java.util.Set)=B:42:0x00c0, (r18_1 'categories' java.util.Set)=B:38:0x00c0}
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
        r31 = 0;
        r32 = new java.util.ArrayList;
        r32.<init>();
        r21 = 0;
        r18 = 0;
        r3 = "id";
        r0 = r38;
        r5 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r3);
        r3 = "activity";
        r0 = r38;
        r7 = com.android.server.pm.ShortcutService.parseComponentNameAttribute(r0, r3);
        r3 = "title";
        r0 = r38;
        r9 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r3);
        r3 = "titleid";
        r0 = r38;
        r10 = com.android.server.pm.ShortcutService.parseIntAttribute(r0, r3);
        r3 = "titlename";
        r0 = r38;
        r11 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r3);
        r3 = "text";
        r0 = r38;
        r12 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r3);
        r3 = "textid";
        r0 = r38;
        r13 = com.android.server.pm.ShortcutService.parseIntAttribute(r0, r3);
        r3 = "textname";
        r0 = r38;
        r14 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r3);
        r3 = "dmessage";
        r0 = r38;
        r15 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r3);
        r3 = "dmessageid";
        r0 = r38;
        r16 = com.android.server.pm.ShortcutService.parseIntAttribute(r0, r3);
        r3 = "dmessagename";
        r0 = r38;
        r17 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r3);
        r3 = "intent";
        r0 = r38;
        r30 = com.android.server.pm.ShortcutService.parseIntentAttributeNoDefault(r0, r3);
        r3 = "rank";
        r0 = r38;
        r36 = com.android.server.pm.ShortcutService.parseLongAttribute(r0, r3);
        r0 = r36;
        r0 = (int) r0;
        r20 = r0;
        r3 = "timestamp";
        r0 = r38;
        r22 = com.android.server.pm.ShortcutService.parseLongAttribute(r0, r3);
        r3 = "flags";
        r0 = r38;
        r36 = com.android.server.pm.ShortcutService.parseLongAttribute(r0, r3);
        r0 = r36;
        r0 = (int) r0;
        r24 = r0;
        r3 = "icon-res";
        r0 = r38;
        r36 = com.android.server.pm.ShortcutService.parseLongAttribute(r0, r3);
        r0 = r36;
        r0 = (int) r0;
        r25 = r0;
        r3 = "icon-resname";
        r0 = r38;
        r26 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r3);
        r3 = "bitmap-path";
        r0 = r38;
        r27 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r3);
        r33 = r38.getDepth();
    L_0x00c0:
        r35 = r38.next();
        r3 = 1;
        r0 = r35;
        if (r0 == r3) goto L_0x016c;
    L_0x00c9:
        r3 = 3;
        r0 = r35;
        if (r0 != r3) goto L_0x00d6;
    L_0x00ce:
        r3 = r38.getDepth();
        r0 = r33;
        if (r3 <= r0) goto L_0x016c;
    L_0x00d6:
        r3 = 2;
        r0 = r35;
        if (r0 != r3) goto L_0x00c0;
    L_0x00db:
        r28 = r38.getDepth();
        r34 = r38.getName();
        r3 = "intent-extras";
        r0 = r34;
        r3 = r0.equals(r3);
        if (r3 == 0) goto L_0x00f3;
    L_0x00ee:
        r31 = android.os.PersistableBundle.restoreFromXml(r38);
        goto L_0x00c0;
    L_0x00f3:
        r3 = "intent";
        r0 = r34;
        r3 = r0.equals(r3);
        if (r3 == 0) goto L_0x0108;
    L_0x00fe:
        r3 = parseIntent(r38);
        r0 = r32;
        r0.add(r3);
        goto L_0x00c0;
    L_0x0108:
        r3 = "extras";
        r0 = r34;
        r3 = r0.equals(r3);
        if (r3 == 0) goto L_0x0118;
    L_0x0113:
        r21 = android.os.PersistableBundle.restoreFromXml(r38);
        goto L_0x00c0;
    L_0x0118:
        r3 = "categories";
        r0 = r34;
        r3 = r0.equals(r3);
        if (r3 != 0) goto L_0x00c0;
    L_0x0123:
        r3 = "string-array";
        r0 = r34;
        r3 = r0.equals(r3);
        if (r3 == 0) goto L_0x0163;
    L_0x012e:
        r3 = "categories";
        r4 = "name";
        r0 = r38;
        r4 = com.android.server.pm.ShortcutService.parseStringAttribute(r0, r4);
        r3 = r3.equals(r4);
        if (r3 == 0) goto L_0x00c0;
    L_0x0140:
        r3 = "string-array";
        r4 = 0;
        r0 = r38;
        r2 = com.android.internal.util.XmlUtils.readThisStringArrayXml(r0, r3, r4);
        r18 = new android.util.ArraySet;
        r3 = r2.length;
        r0 = r18;
        r0.<init>(r3);
        r29 = 0;
    L_0x0154:
        r3 = r2.length;
        r0 = r29;
        if (r0 >= r3) goto L_0x00c0;
    L_0x0159:
        r3 = r2[r29];
        r0 = r18;
        r0.add(r3);
        r29 = r29 + 1;
        goto L_0x0154;
    L_0x0163:
        r0 = r28;
        r1 = r34;
        r3 = com.android.server.pm.ShortcutService.throwForInvalidTag(r0, r1);
        throw r3;
    L_0x016c:
        if (r30 == 0) goto L_0x017b;
    L_0x016e:
        android.content.pm.ShortcutInfo.setIntentExtras(r30, r31);
        r32.clear();
        r0 = r32;
        r1 = r30;
        r0.add(r1);
    L_0x017b:
        r3 = new android.content.pm.ShortcutInfo;
        r4 = r32.size();
        r4 = new android.content.Intent[r4];
        r0 = r32;
        r19 = r0.toArray(r4);
        r19 = (android.content.Intent[]) r19;
        r8 = 0;
        r4 = r40;
        r6 = r39;
        r3.<init>(r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15, r16, r17, r18, r19, r20, r21, r22, r24, r25, r26, r27);
        return r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.ShortcutPackage.parseShortcut(org.xmlpull.v1.XmlPullParser, java.lang.String, int):android.content.pm.ShortcutInfo");
    }

    private static Intent parseIntent(XmlPullParser parser) throws IOException, XmlPullParserException {
        Intent intent = ShortcutService.parseIntentAttribute(parser, ATTR_INTENT_NO_EXTRA);
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return intent;
            }
            if (type == 2) {
                int depth = parser.getDepth();
                String tag = parser.getName();
                if (tag.equals(TAG_EXTRAS)) {
                    ShortcutInfo.setIntentExtras(intent, PersistableBundle.restoreFromXml(parser));
                } else {
                    throw ShortcutService.throwForInvalidTag(depth, tag);
                }
            }
        }
        return intent;
    }

    List<ShortcutInfo> getAllShortcutsForTest() {
        return new ArrayList(this.mShortcuts.values());
    }

    public void verifyStates() {
        super.verifyStates();
        boolean failed = false;
        ShortcutService s = this.mShortcutUser.mService;
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
        for (int outer = all.size() - 1; outer >= 0; outer--) {
            ArrayList<ShortcutInfo> list = (ArrayList) all.valueAt(outer);
            if (list.size() > this.mShortcutUser.mService.getMaxActivityShortcuts()) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": activity " + all.keyAt(outer) + " has " + ((ArrayList) all.valueAt(outer)).size() + " shortcuts.");
            }
            Collections.sort(list, -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w.$INST$4);
            ArrayList<ShortcutInfo> dynamicList = new ArrayList(list);
            dynamicList.removeIf(-$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw.$INST$3);
            ArrayList<ShortcutInfo> manifestList = new ArrayList(list);
            dynamicList.removeIf(-$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw.$INST$4);
            verifyRanksSequential(dynamicList);
            verifyRanksSequential(manifestList);
        }
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            boolean isPinned = (si.isDeclaredInManifest() || si.isDynamic()) ? true : si.isPinned();
            if (!isPinned) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " is not manifest, dynamic or pinned.");
            }
            if (si.isDeclaredInManifest() && si.isDynamic()) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " is both dynamic and manifest at the same time.");
            }
            if (si.getActivity() == null && (si.isFloating() ^ 1) != 0) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has null activity, but not floating.");
            }
            if ((si.isDynamic() || si.isManifestShortcut()) && (si.isEnabled() ^ 1) != 0) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " is not floating, but is disabled.");
            }
            if (si.isFloating() && si.getRank() != 0) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " is floating, but has rank=" + si.getRank());
            }
            if (si.getIcon() != null) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " still has an icon");
            }
            if (si.hasAdaptiveBitmap() && (si.hasIconFile() ^ 1) != 0) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has adaptive bitmap but was not saved to a file.");
            }
            if (si.hasIconFile() && si.hasIconResource()) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has both resource and bitmap icons");
            }
            if (s.isDummyMainActivity(si.getActivity())) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has a dummy target activity");
            }
        }
        if (failed) {
            throw new IllegalStateException("See logcat for errors");
        }
    }

    private boolean verifyRanksSequential(List<ShortcutInfo> list) {
        boolean failed = false;
        for (int i = 0; i < list.size(); i++) {
            ShortcutInfo si = (ShortcutInfo) list.get(i);
            if (si.getRank() != i) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " rank=" + si.getRank() + " but expected to be " + i);
            }
        }
        return failed;
    }
}
