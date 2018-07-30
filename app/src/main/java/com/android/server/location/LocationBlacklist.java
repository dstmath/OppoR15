package com.android.server.location;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocationManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public final class LocationBlacklist extends ContentObserver {
    private static final String BLACKLIST_CONFIG_NAME = "locationPackagePrefixBlacklist";
    private static final boolean D = LocationManagerService.D;
    private static final String TAG = "LocationBlacklist";
    private static final String WHITELIST_CONFIG_NAME = "locationPackagePrefixWhitelist";
    private String[] mBlacklist = new String[0];
    private final Context mContext;
    private int mCurrentUserId = 0;
    private final Object mLock = new Object();
    private String[] mWhitelist = new String[0];

    public LocationBlacklist(Context context, Handler handler) {
        super(handler);
        this.mContext = context;
    }

    public void init() {
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor(BLACKLIST_CONFIG_NAME), false, this, -1);
        reloadBlacklist();
    }

    private void reloadBlacklistLocked() {
        this.mWhitelist = getStringArrayLocked(WHITELIST_CONFIG_NAME);
        if (D) {
            Slog.d(TAG, "whitelist: " + Arrays.toString(this.mWhitelist));
        }
        this.mBlacklist = getStringArrayLocked(BLACKLIST_CONFIG_NAME);
        if (D) {
            Slog.d(TAG, "blacklist: " + Arrays.toString(this.mBlacklist));
        }
    }

    private void reloadBlacklist() {
        synchronized (this.mLock) {
            reloadBlacklistLocked();
        }
    }

    public boolean isBlacklisted(String packageName) {
        synchronized (this.mLock) {
            String[] strArr = this.mBlacklist;
            int length = strArr.length;
            int i = 0;
            while (i < length) {
                String black = strArr[i];
                if (!packageName.startsWith(black) || inWhitelist(packageName)) {
                    i++;
                } else {
                    if (D) {
                        Log.d(TAG, "dropping location (blacklisted): " + packageName + " matches " + black);
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private boolean inWhitelist(String pkg) {
        synchronized (this.mLock) {
            for (String white : this.mWhitelist) {
                if (pkg.startsWith(white)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void onChange(boolean selfChange) {
        reloadBlacklist();
    }

    public void switchUser(int userId) {
        synchronized (this.mLock) {
            this.mCurrentUserId = userId;
            reloadBlacklistLocked();
        }
    }

    private String[] getStringArrayLocked(String key) {
        String flatString;
        int i = 0;
        synchronized (this.mLock) {
            flatString = Secure.getStringForUser(this.mContext.getContentResolver(), key, this.mCurrentUserId);
        }
        if (flatString == null) {
            return new String[0];
        }
        String[] splitStrings = flatString.split(",");
        ArrayList<String> result = new ArrayList();
        int length = splitStrings.length;
        while (i < length) {
            String pkg = splitStrings[i].trim();
            if (!pkg.isEmpty()) {
                result.add(pkg);
            }
            i++;
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    public void dump(PrintWriter pw) {
        pw.println("mWhitelist=" + Arrays.toString(this.mWhitelist) + " mBlacklist=" + Arrays.toString(this.mBlacklist));
    }
}
