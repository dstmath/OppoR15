package com.android.server.notification;

import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class GroupHelper {
    protected static final int AUTOGROUP_AT_COUNT = 4;
    protected static final String AUTOGROUP_KEY = "ranker_group";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "GroupHelper";
    private final Callback mCallback;
    Map<Integer, Map<String, LinkedHashSet<String>>> mUngroupedNotifications = new HashMap();

    protected interface Callback {
        void addAutoGroup(String str);

        void addAutoGroupSummary(int i, String str, String str2);

        void removeAutoGroup(String str);

        void removeAutoGroupSummary(int i, String str);
    }

    public GroupHelper(Callback callback) {
        this.mCallback = callback;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onNotificationPosted(StatusBarNotification sbn, boolean autogroupSummaryExists) {
        if (DEBUG) {
            Log.i(TAG, "POSTED " + sbn.getKey());
        }
        try {
            List<String> notificationsToGroup = new ArrayList();
            if (sbn.isAppGroup()) {
                maybeUngroup(sbn, false, sbn.getUserId());
                return;
            }
            synchronized (this.mUngroupedNotifications) {
                if ("android".equals(sbn.getPackageName())) {
                    return;
                }
                Map<String, LinkedHashSet<String>> ungroupedNotificationsByUser = (Map) this.mUngroupedNotifications.get(Integer.valueOf(sbn.getUserId()));
                if (ungroupedNotificationsByUser == null) {
                    ungroupedNotificationsByUser = new HashMap();
                }
                this.mUngroupedNotifications.put(Integer.valueOf(sbn.getUserId()), ungroupedNotificationsByUser);
                LinkedHashSet<String> notificationsForPackage = (LinkedHashSet) ungroupedNotificationsByUser.get(sbn.getPackageName());
                if (notificationsForPackage == null) {
                    notificationsForPackage = new LinkedHashSet();
                }
                notificationsForPackage.add(sbn.getKey());
                ungroupedNotificationsByUser.put(sbn.getPackageName(), notificationsForPackage);
                if (notificationsForPackage.size() >= 4 || autogroupSummaryExists) {
                    notificationsToGroup.addAll(notificationsForPackage);
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failure processing new notification", e);
        }
    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
        try {
            maybeUngroup(sbn, true, sbn.getUserId());
        } catch (Exception e) {
            Slog.e(TAG, "Error processing canceled notification", e);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void maybeUngroup(StatusBarNotification sbn, boolean notificationGone, int userId) {
        List<String> notificationsToUnAutogroup = new ArrayList();
        boolean removeSummary = false;
        synchronized (this.mUngroupedNotifications) {
            Map<String, LinkedHashSet<String>> ungroupedNotificationsByUser = (Map) this.mUngroupedNotifications.get(Integer.valueOf(sbn.getUserId()));
            if (ungroupedNotificationsByUser == null || ungroupedNotificationsByUser.size() == 0) {
            } else {
                LinkedHashSet<String> notificationsForPackage = (LinkedHashSet) ungroupedNotificationsByUser.get(sbn.getPackageName());
                if (notificationsForPackage == null || notificationsForPackage.size() == 0) {
                } else {
                    if (notificationsForPackage.remove(sbn.getKey()) && !notificationGone) {
                        notificationsToUnAutogroup.add(sbn.getKey());
                    }
                    if (notificationsForPackage.size() == 0) {
                        ungroupedNotificationsByUser.remove(sbn.getPackageName());
                        removeSummary = true;
                    }
                }
            }
        }
    }

    private void adjustAutogroupingSummary(int userId, String packageName, String triggeringKey, boolean summaryNeeded) {
        if (summaryNeeded) {
            this.mCallback.addAutoGroupSummary(userId, packageName, triggeringKey);
        } else {
            this.mCallback.removeAutoGroupSummary(userId, packageName);
        }
    }

    private void adjustNotificationBundling(List<String> keys, boolean group) {
        for (String key : keys) {
            if (DEBUG) {
                Log.i(TAG, "Sending grouping adjustment for: " + key + " group? " + group);
            }
            if (group) {
                this.mCallback.addAutoGroup(key);
            } else {
                this.mCallback.removeAutoGroup(key);
            }
        }
    }
}
