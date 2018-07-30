package com.android.internal.content;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.Preconditions;
import java.util.HashSet;

public abstract class PackageMonitor extends BroadcastReceiver {
    public static final int PACKAGE_PERMANENT_CHANGE = 3;
    public static final int PACKAGE_TEMPORARY_CHANGE = 2;
    public static final int PACKAGE_UNCHANGED = 0;
    public static final int PACKAGE_UPDATING = 1;
    static final IntentFilter sExternalFilt = new IntentFilter();
    static final IntentFilter sNonDataFilt = new IntentFilter();
    static final IntentFilter sPackageFilt = new IntentFilter();
    String[] mAppearingPackages;
    int mChangeType;
    int mChangeUserId = -10000;
    String[] mDisappearingPackages;
    String[] mModifiedComponents;
    String[] mModifiedPackages;
    Context mRegisteredContext;
    Handler mRegisteredHandler;
    boolean mSomePackagesChanged;
    String[] mTempArray = new String[1];
    final HashSet<String> mUpdatingPackages = new HashSet();

    static {
        sPackageFilt.addAction("android.intent.action.PACKAGE_ADDED");
        sPackageFilt.addAction("android.intent.action.PACKAGE_REMOVED");
        sPackageFilt.addAction("android.intent.action.PACKAGE_CHANGED");
        sPackageFilt.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
        sPackageFilt.addAction("android.intent.action.PACKAGE_RESTARTED");
        sPackageFilt.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        sPackageFilt.addDataScheme("package");
        sNonDataFilt.addAction("android.intent.action.UID_REMOVED");
        sNonDataFilt.addAction("android.intent.action.USER_STOPPED");
        sNonDataFilt.addAction("android.intent.action.PACKAGES_SUSPENDED");
        sNonDataFilt.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        sExternalFilt.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sExternalFilt.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
    }

    public void register(Context context, Looper thread, boolean externalStorage) {
        register(context, thread, null, externalStorage);
    }

    public void register(Context context, Looper thread, UserHandle user, boolean externalStorage) {
        register(context, user, externalStorage, thread == null ? BackgroundThread.getHandler() : new Handler(thread));
    }

    public void register(Context context, UserHandle user, boolean externalStorage, Handler handler) {
        if (this.mRegisteredContext != null) {
            throw new IllegalStateException("Already registered");
        }
        this.mRegisteredContext = context;
        this.mRegisteredHandler = (Handler) Preconditions.checkNotNull(handler);
        if (user != null) {
            context.registerReceiverAsUser(this, user, sPackageFilt, null, this.mRegisteredHandler);
            context.registerReceiverAsUser(this, user, sNonDataFilt, null, this.mRegisteredHandler);
            if (externalStorage) {
                context.registerReceiverAsUser(this, user, sExternalFilt, null, this.mRegisteredHandler);
                return;
            }
            return;
        }
        context.registerReceiver(this, sPackageFilt, null, this.mRegisteredHandler);
        context.registerReceiver(this, sNonDataFilt, null, this.mRegisteredHandler);
        if (externalStorage) {
            context.registerReceiver(this, sExternalFilt, null, this.mRegisteredHandler);
        }
    }

    public Handler getRegisteredHandler() {
        return this.mRegisteredHandler;
    }

    public void unregister() {
        if (this.mRegisteredContext == null) {
            throw new IllegalStateException("Not registered");
        }
        this.mRegisteredContext.unregisterReceiver(this);
        this.mRegisteredContext = null;
    }

    boolean isPackageUpdating(String packageName) {
        boolean contains;
        synchronized (this.mUpdatingPackages) {
            contains = this.mUpdatingPackages.contains(packageName);
        }
        return contains;
    }

    public void onBeginPackageChanges() {
    }

    public void onPackageAdded(String packageName, int uid) {
    }

    public void onPackageRemoved(String packageName, int uid) {
    }

    public void onPackageRemovedAllUsers(String packageName, int uid) {
    }

    public void onPackageUpdateStarted(String packageName, int uid) {
    }

    public void onPackageUpdateFinished(String packageName, int uid) {
    }

    public boolean onPackageChanged(String packageName, int uid, String[] components) {
        if (components != null) {
            for (String name : components) {
                if (packageName.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
        return false;
    }

    public void onHandleUserStop(Intent intent, int userHandle) {
    }

    public void onUidRemoved(int uid) {
    }

    public void onPackagesAvailable(String[] packages) {
    }

    public void onPackagesUnavailable(String[] packages) {
    }

    public void onPackagesSuspended(String[] packages) {
    }

    public void onPackagesUnsuspended(String[] packages) {
    }

    public void onPackageDisappeared(String packageName, int reason) {
    }

    public void onPackageAppeared(String packageName, int reason) {
    }

    public void onPackageModified(String packageName) {
    }

    public boolean didSomePackagesChange() {
        return this.mSomePackagesChanged;
    }

    public int isPackageAppearing(String packageName) {
        if (this.mAppearingPackages != null) {
            for (int i = this.mAppearingPackages.length - 1; i >= 0; i--) {
                if (packageName.equals(this.mAppearingPackages[i])) {
                    return this.mChangeType;
                }
            }
        }
        return 0;
    }

    public boolean anyPackagesAppearing() {
        return this.mAppearingPackages != null;
    }

    public int isPackageDisappearing(String packageName) {
        if (this.mDisappearingPackages != null) {
            for (int i = this.mDisappearingPackages.length - 1; i >= 0; i--) {
                if (packageName.equals(this.mDisappearingPackages[i])) {
                    return this.mChangeType;
                }
            }
        }
        return 0;
    }

    public boolean anyPackagesDisappearing() {
        return this.mDisappearingPackages != null;
    }

    public boolean isReplacing() {
        return this.mChangeType == 1;
    }

    public boolean isPackageModified(String packageName) {
        if (this.mModifiedPackages != null) {
            for (int i = this.mModifiedPackages.length - 1; i >= 0; i--) {
                if (packageName.equals(this.mModifiedPackages[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isComponentModified(String className) {
        if (className == null || this.mModifiedComponents == null) {
            return false;
        }
        for (int i = this.mModifiedComponents.length - 1; i >= 0; i--) {
            if (className.equals(this.mModifiedComponents[i])) {
                return true;
            }
        }
        return false;
    }

    public void onSomePackagesChanged() {
    }

    public void onFinishPackageChanges() {
    }

    public void onPackageDataCleared(String packageName, int uid) {
    }

    public int getChangingUserId() {
        return this.mChangeUserId;
    }

    String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        return uri != null ? uri.getSchemeSpecificPart() : null;
    }

    public void onReceive(Context context, Intent intent) {
        int i = 2;
        this.mChangeUserId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
        if (this.mChangeUserId == -10000) {
            Slog.w("PackageMonitor", "Intent broadcast does not contain user handle: " + intent);
            return;
        }
        onBeginPackageChanges();
        this.mAppearingPackages = null;
        this.mDisappearingPackages = null;
        this.mSomePackagesChanged = false;
        this.mModifiedComponents = null;
        String action = intent.getAction();
        String pkg;
        int uid;
        String[] pkgList;
        if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
            pkg = getPackageName(intent);
            uid = intent.getIntExtra("android.intent.extra.UID", 0);
            this.mSomePackagesChanged = true;
            if (pkg != null) {
                this.mAppearingPackages = this.mTempArray;
                this.mTempArray[0] = pkg;
                if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    this.mModifiedPackages = this.mTempArray;
                    this.mChangeType = 1;
                    onPackageUpdateFinished(pkg, uid);
                    onPackageModified(pkg);
                } else {
                    this.mChangeType = 3;
                    onPackageAdded(pkg, uid);
                }
                onPackageAppeared(pkg, this.mChangeType);
                if (this.mChangeType == 1) {
                    synchronized (this.mUpdatingPackages) {
                        this.mUpdatingPackages.remove(pkg);
                    }
                }
            }
        } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
            pkg = getPackageName(intent);
            uid = intent.getIntExtra("android.intent.extra.UID", 0);
            if (pkg != null) {
                this.mDisappearingPackages = this.mTempArray;
                this.mTempArray[0] = pkg;
                if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    this.mChangeType = 1;
                    synchronized (this.mUpdatingPackages) {
                    }
                    onPackageUpdateStarted(pkg, uid);
                } else {
                    this.mChangeType = 3;
                    this.mSomePackagesChanged = true;
                    onPackageRemoved(pkg, uid);
                    if (intent.getBooleanExtra("android.intent.extra.REMOVED_FOR_ALL_USERS", false)) {
                        onPackageRemovedAllUsers(pkg, uid);
                    }
                }
                onPackageDisappeared(pkg, this.mChangeType);
            }
        } else if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
            pkg = getPackageName(intent);
            uid = intent.getIntExtra("android.intent.extra.UID", 0);
            this.mModifiedComponents = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
            if (pkg != null) {
                this.mModifiedPackages = this.mTempArray;
                this.mTempArray[0] = pkg;
                this.mChangeType = 3;
                if (onPackageChanged(pkg, uid, this.mModifiedComponents)) {
                    this.mSomePackagesChanged = true;
                }
                onPackageModified(pkg);
            }
        } else if ("android.intent.action.PACKAGE_DATA_CLEARED".equals(action)) {
            pkg = getPackageName(intent);
            uid = intent.getIntExtra("android.intent.extra.UID", 0);
            if (pkg != null) {
                onPackageDataCleared(pkg, uid);
            }
        } else if ("android.intent.action.QUERY_PACKAGE_RESTART".equals(action)) {
            this.mDisappearingPackages = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
            this.mChangeType = 2;
            if (onHandleForceStop(intent, this.mDisappearingPackages, intent.getIntExtra("android.intent.extra.UID", 0), false)) {
                setResultCode(-1);
            }
        } else if ("android.intent.action.PACKAGE_RESTARTED".equals(action)) {
            this.mDisappearingPackages = new String[]{getPackageName(intent)};
            this.mChangeType = 2;
            onHandleForceStop(intent, this.mDisappearingPackages, intent.getIntExtra("android.intent.extra.UID", 0), true);
        } else if ("android.intent.action.UID_REMOVED".equals(action)) {
            onUidRemoved(intent.getIntExtra("android.intent.extra.UID", 0));
        } else if ("android.intent.action.USER_STOPPED".equals(action)) {
            if (intent.hasExtra("android.intent.extra.user_handle")) {
                onHandleUserStop(intent, intent.getIntExtra("android.intent.extra.user_handle", 0));
            }
        } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
            pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            this.mAppearingPackages = pkgList;
            if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                i = 1;
            }
            this.mChangeType = i;
            this.mSomePackagesChanged = true;
            if (pkgList != null) {
                onPackagesAvailable(pkgList);
                for (String onPackageAppeared : pkgList) {
                    onPackageAppeared(onPackageAppeared, this.mChangeType);
                }
            }
        } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
            pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            this.mDisappearingPackages = pkgList;
            if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                i = 1;
            }
            this.mChangeType = i;
            this.mSomePackagesChanged = true;
            if (pkgList != null) {
                onPackagesUnavailable(pkgList);
                for (String onPackageAppeared2 : pkgList) {
                    onPackageDisappeared(onPackageAppeared2, this.mChangeType);
                }
            }
        } else if ("android.intent.action.PACKAGES_SUSPENDED".equals(action)) {
            pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            this.mSomePackagesChanged = true;
            onPackagesSuspended(pkgList);
        } else if ("android.intent.action.PACKAGES_UNSUSPENDED".equals(action)) {
            pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            this.mSomePackagesChanged = true;
            onPackagesUnsuspended(pkgList);
        }
        if (this.mSomePackagesChanged) {
            onSomePackagesChanged();
        }
        onFinishPackageChanges();
        this.mChangeUserId = -10000;
    }
}
